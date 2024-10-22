// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.certificate;

import static net.java.sip.communicator.impl.certificate.CertificateServiceImpl.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import net.java.sip.communicator.service.certificate.CertificateMatcher;
import net.java.sip.communicator.service.certificate.CertificateService;
import net.java.sip.communicator.service.httputil.HttpUtils;
import net.java.sip.communicator.util.Logger;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.util.OSUtils;

/**
 * A wrapper to X509TrustManager that allows reloading of the inner trust manager.
 * The reloading is currently used only if the OS is Windows because the
 * Windows Trust Store could potentially not have a root CA certificate,
 * so after forcing an update to the Windows Trust Store, the inner trust manager
 * must be reloaded to pick up those changes
 */
class ReloadableX509TrustManager implements X509TrustManager
{
    private static final Logger logger = Logger.getLogger(ReloadableX509TrustManager.class);
    /**
     * Base property name for the storage of certificate user preferences.
     */
    private static final String PNAME_CERT_TRUST_PREFIX = "net.java.sip.communicator.impl.certservice";
    /** Hash algorithm for the cert thumbprint **/
    private static final String THUMBPRINT_HASH_ALGORITHM = "SHA1";
    private final ConfigurationService config = CertificateVerificationActivator.getConfigurationService();
    private final List<String> identitiesToTest;
    private final CertificateMatcher serverVerifier;
    private final CertificateMatcher clientVerifier;
    private X509TrustManager innerTrustManager;

    /**
     * Caches retrievals of AIA information (downloaded certs or failures).
     */
    private static final Map<URI, AiaCacheEntry> aiaCache = new ConcurrentHashMap<>();

    /**
     * AIA cache retrieval entry.
     */
    private static class AiaCacheEntry
    {
        Date cacheDate;
        X509Certificate cert;
        AiaCacheEntry(Date cacheDate, X509Certificate cert)
        {
            this.cacheDate = cacheDate;
            this.cert = cert;
        }
    }

    public ReloadableX509TrustManager(List<String> identitiesToTest,
                                      CertificateMatcher serverVerifier,
                                      CertificateMatcher clientVerifier) throws GeneralSecurityException
    {
        this.innerTrustManager = loadTrustManager(identitiesToTest);
        this.identitiesToTest = identitiesToTest;
        this.serverVerifier = serverVerifier;
        this.clientVerifier = clientVerifier;
    }

    public X509Certificate[] getAcceptedIssuers()
    {
        return this.innerTrustManager.getAcceptedIssuers();
    }

    public void checkServerTrusted(X509Certificate[] chain,
                                   String authType) throws CertificateException
    {
        checkCertTrusted(chain, authType, true);
    }

    public void checkClientTrusted(X509Certificate[] chain,
                                   String authType) throws CertificateException
    {
        checkCertTrusted(chain, authType, false);
    }

    private void checkCertTrusted(X509Certificate[] chain,
                                  String authType,
                                  boolean serverCheck) throws CertificateException
    {
        try
        {
            // check and default configurations for property
            // if missing default is null - false
            String defaultAlwaysTrustMode =
                    CertificateVerificationActivator.getResources()
                            .getSettingsString(
                                    CertificateService.PNAME_ALWAYS_TRUST);

            boolean alwaysTrustMode = Boolean.parseBoolean(defaultAlwaysTrustMode);

            if (config.user() != null)
            {
                alwaysTrustMode = config.user().getBoolean(CertificateService.PNAME_ALWAYS_TRUST,
                                                                              alwaysTrustMode);
            }

            if (alwaysTrustMode)
            {
                return;
            }

            boolean failedToVerify = false;

            try
            {
                // check the certificate itself (issuer, validity)
                try
                {
                    chain = tryBuildChain(chain);
                }
                catch (Exception e)
                {
                    // Log the exception and take the chain as is
                    logger.info(e);
                }

                logger.debug("Checking server certificate" +
                                                    " authType: " + authType +
                                                    " tm: " + this.innerTrustManager);

                if (serverCheck)
                {
                    try
                    {
                        this.innerTrustManager.checkServerTrusted(chain, authType);
                    }
                    catch (CertificateException exception)
                    {
                        if (OSUtils.isWindows())
                        {
                            handleWindowsCertificateFailure(chain, authType);
                        }
                        else
                        {
                            throw exception;
                        }
                    }
                }
                else
                {
                    this.innerTrustManager.checkClientTrusted(chain, authType);
                }
            }
            catch (CertificateException e)
            {
                // Failed to validate the certificate chain using the
                // default Trust Manager.
                // We used to check against a custom trust store here
                // in this case - removed under BUG-4242
                failedToVerify = true;

                logger.warn("Failed to check certificate " +
                                                   "using default trust manager: ", e);
            }

            if (!failedToVerify)
            {
                if (identitiesToTest.isEmpty())
                {
                    return;
                }
                else
                {
                    try
                    {
                        if (serverCheck)
                        {
                            serverVerifier.verify(identitiesToTest, chain[0]);
                        }
                        else
                        {
                            clientVerifier.verify(identitiesToTest, chain[0]);
                        }
                    }
                    catch (CertificateException e)
                    {
                        failedToVerify = true;
                        logger.warn("Failed to verify certificate: ", e);
                    }

                    // OK, the certificate is valid.
                }
            }

            if (failedToVerify)
            {
                String thumbprint = getThumbprint(chain[0], THUMBPRINT_HASH_ALGORITHM);
                List<String> storedCerts = new LinkedList<>();

                if (identitiesToTest.isEmpty())
                {
                    String propName = PNAME_CERT_TRUST_PREFIX + ".server." + thumbprint;

                    // get the thumbprints from the permanent allowances
                    String hashes = config.global().getString(propName);
                    if (hashes != null)
                        storedCerts.addAll(Arrays.asList(hashes.split(",")));
                }
                else
                {
                    for (String identity : identitiesToTest)
                    {
                        String propName = PNAME_CERT_TRUST_PREFIX + ".param." + identity;

                        // get the thumbprints from the permanent allowances
                        String hashes = config.global().getString(propName);
                        if (hashes != null)
                            Collections.addAll(storedCerts, hashes.split(","));
                    }
                }

                if (!storedCerts.contains(thumbprint))
                {
                    logger.debug("Certificate not stored:" +
                                                        " stored: " + storedCerts +
                                                        " thumbprint: " + thumbprint);

                    throw new CertificateException(
                            "The peer provided certificate with Subject <"
                            + chain[0].getSubjectDN()
                            + "> is not trusted");
                }
                // ok, we've seen this certificate before
            }
        }
        catch (Exception e)
        {
            logger.error("Error checking certificate: " + e);
            throw e;
        }
    }

    private void handleWindowsCertificateFailure(X509Certificate[] chain, String authType) throws CertificateException
    {
        // This scenario could be happening because the root certificate is not yet present
        // in the Windows Certificate Trust Store.
        // (Note: this has not been detected as an issue on Mac so far)
        //
        // On Windows 11 the behaviour is that once the app tries to reach a certain url,
        // the OS will install the certificate in its trust store, but possibly not in time
        // so that the app will be able to verify it on start up. In this case we can just retry
        // verifying the certificate once we reload the trust store.
        //
        // On Windows 10 the behaviour is that whatever url the app will try to reach, the
        // Windows Certificate Trust Store will not update itself with the certificate that
        // is needed, so we would always get an exception that the certificate is not valid.
        // The process of using CertUtil to verify the certificate through the OS is needed
        // because that way the Windows Trust Store will download the certificate,
        // so we can just reload and retry after that
        logger.error("Certificate verification failed");

        // Trigger the Windows Trust Store to update itself
        forceUpdateOfWindowsTrustStore(chain);

        // Reload the trust store to pick up any changes in the list of OS roots that
        // were triggered by the forced update of the Windows Trust Store
        reloadInnerTrustManager(identitiesToTest);

        // Now re-check the trust of the chain
        this.innerTrustManager.checkServerTrusted(chain, authType);
    }

    private void forceUpdateOfWindowsTrustStore(X509Certificate[] chain)
    {
        String certLocation = null;
        try
        {
            certLocation = createPemFileFromX509CertificateAndGetLocation(chain[0]);
            verifyCertificateFileThroughWindowsStore(certLocation);
        }
        catch (Exception e)
        {
            // Even though we have failed to either create the certificate file
            // or verify it, we can still retry to reload the trust store
            // and verify the certificate, so no exception will be thrown here
            logger.error("Unable to create certificate file or verify it " + e);
        }
        finally
        {
            if (certLocation != null)
            {
                cleanUpTempCertificate(certLocation);
            }
        }
    }

    private void reloadInnerTrustManager(List<String> identitiesToTest) throws CertificateException
    {
        try
        {
            this.innerTrustManager = loadTrustManager(identitiesToTest);
        }
        catch (GeneralSecurityException ex)
        {
            logger.error("Unable to reload trust manager " + ex);
            throw new CertificateException(ex);
        }
    }

    private X509Certificate[] tryBuildChain(X509Certificate[] chain)
            throws IOException,
            URISyntaxException,
            CertificateException
    {
        // Only try to build chains for servers that send only their
        // own cert, but no issuer. This also matches self-signed (will
        // be ignored later) and Root-CA signed certs. In this case we
        // throw the Root-CA away after the lookup
        if (chain.length != 1)
            return chain;

        // ignore self signed certs
        if (chain[0].getIssuerDN().equals(chain[0].getSubjectDN()))
            return chain;

        // prepare for the newly created chain
        List<X509Certificate> newChain =
                new ArrayList<>(chain.length + 4);
        newChain.addAll(Arrays.asList(chain));

        // search from the topmost certificate upwards
        CertificateFactory certFactory =
                CertificateFactory.getInstance("X.509");
        X509Certificate current = chain[chain.length - 1];
        boolean foundParent;
        int chainLookupCount = 0;
        do
        {
            foundParent = false;
            // extract the url(s) where the parent certificate can be
            // found
            byte[] aiaBytes =
                    current.getExtensionValue(
                            Extension.authorityInfoAccess.getId());
            if (aiaBytes == null)
                break;

            AuthorityInformationAccess aia
                    = AuthorityInformationAccess.getInstance(
                    JcaX509ExtensionUtils.parseExtensionValue(aiaBytes));

            // the AIA may contain different URLs and types, try all
            // of them
            for (AccessDescription ad : aia.getAccessDescriptions())
            {
                // we are only interested in the issuer certificate,
                // not in OCSP urls the like
                if (!ad.getAccessMethod().equals(
                        AccessDescription.id_ad_caIssuers))
                    continue;

                GeneralName gn = ad.getAccessLocation();
                if (!(gn.getTagNo() ==
                      GeneralName.uniformResourceIdentifier
                      && gn.getName() instanceof DERIA5String))
                    continue;

                URI uri =
                        new URI(((DERIA5String) gn.getName()).getString());
                // only http(s) urls; LDAP is taken care of in the
                // default implementation
                if (!(uri.getScheme().equalsIgnoreCase("http") || uri
                        .getScheme().equals("https")))
                    continue;

                X509Certificate cert = null;

                // try to get cert from cache first to avoid consecutive
                // (slow) http lookups
                AiaCacheEntry cache = aiaCache.get(uri);
                if (cache != null && cache.cacheDate.after(new Date()))
                {
                    cert = cache.cert;
                }
                else
                {
                    // download if no cache entry or if it is expired
                    logger
                            .debug("Downloading parent certificate for <"
                                   + current.getSubjectDN()
                                   + "> from <"
                                   + uri + ">");
                    try
                    {
                        InputStream is =
                                HttpUtils.openURLConnection(uri.toString())
                                        .getContent();
                        cert =
                                (X509Certificate) certFactory
                                        .generateCertificate(is);
                    }
                    catch (Exception e)
                    {
                        logger.debug("Could not download from <" + uri
                                                            + ">");
                    }
                    // cache for 10mins
                    aiaCache.put(uri, new AiaCacheEntry(new Date(
                            new Date().getTime() + 10 * 60 * 1000), cert));
                }
                if (cert != null)
                {
                    if (!cert.getIssuerDN().equals(cert.getSubjectDN()))
                    {
                        newChain.add(cert);
                        foundParent = true;
                        current = cert;
                        break; // an AD was valid, ignore others
                    }
                    else
                    {
                        logger.debug("Parent is self-signed, ignoring");
                    }
                }
            }
            chainLookupCount++;
        }
        while (foundParent && chainLookupCount < 10);
        chain = newChain.toArray(chain);
        return chain;
    }

    private X509TrustManager loadTrustManager(List<String> identitiesToTest) throws GeneralSecurityException
    {
        logger.debug("Creating new TrustManager with identities: " + identitiesToTest);

        // obtain the default X509 trust manager
        TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

        tmFactory.init(initialiseKeyStore());
        for (TrustManager m : tmFactory.getTrustManagers())
        {
            if (m instanceof X509TrustManager)
            {
                return (X509TrustManager) m;
            }
        }

        throw new GeneralSecurityException("No default X509 trust manager found");
    }

    /**
     * Initialises a new KeyStore instance.
     * <p>
     * For Windows, we rely on the native OS implementation
     * (obtained via the SunMSCAPI provider),
     * so we simply return null here.
     * <p>
     * For macOS, we must load the certificates from the system root
     * into a KeyStore.
     */
    private static KeyStore initialiseKeyStore() throws GeneralSecurityException
    {
        KeyStore ks = null;
        if (!OSUtils.isUT())
        {
            if (OSUtils.isWindows())
            {
                try
                {
                    ks = KeyStore.getInstance("Windows-ROOT", "SunMSCAPI");
                    ks.load(null, null);
                }
                catch (Exception ex)
                {
                    throw new GeneralSecurityException("Cannot init KeyStore for Windows", ex);
                }
            }
            else
            {
                ks = KeyStore.getInstance(KeyStore.getDefaultType());
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                try
                {
                    ks.load(null, null);
                    ProcessBuilder pb = new ProcessBuilder("security", "find-certificate", "-a", "-p",
                                                           getMacOSKeychainPath() + "/" + getMacOSKeychainFileName());
                    Process proc = pb.start();
                    proc.waitFor(100, TimeUnit.MILLISECONDS);
                    try (BufferedInputStream procInputStream = new BufferedInputStream(proc.getInputStream()))
                    {
                        int certCount = 0;
                        while (procInputStream.available() > 0)
                        {
                            Certificate cert = cf.generateCertificate(procInputStream);
                            ks.setCertificateEntry(cert.toString(), cert);
                            certCount++;
                        }
                        logger.debug("Loaded " + certCount +
                                     " certificates from the macOS system root");
                    }
                }
                catch (IOException e)
                {
                    throw new GeneralSecurityException(
                            "Unable to load certificates from macOS system root", e);
                }
                catch (InterruptedException ex)
                {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return ks;
    }

    /**
     *  This method creates a pem certificate file and returns the location where this was created
     */
    private String createPemFileFromX509CertificateAndGetLocation(X509Certificate certificate) throws IOException
    {
        ConfigurationService cfg = CertificateVerificationActivator.getConfigurationService();
        String certDirectoryLocation = cfg.global().getScHomeDirLocation() + File.separator + cfg.global().getScHomeDirName();
        String fileName = "tempCert.pem";
        String filePath = certDirectoryLocation + File.separator + fileName;

        try (JcaPEMWriter pemWriter = new JcaPEMWriter(new FileWriter(filePath))) // CodeQL [SM00697] Not Exploitable. The file/path is not user provided.
        {
            pemWriter.writeObject(certificate);
        }
        catch (IOException e)
        {
            logger.error("The file " + fileName + " failed to be created with ex: " + e);
            throw e;
        }

        logger.debug("Temporary certificate file successfully " + fileName);
        return filePath;
    }

    /**
     *  This method invokes a process that asks the OS to verify a certificate.
     *  After this process completes, if the certificate has been verified its root certificate
     *  will be added to the Windows Trust Store.
     *  The output of the process is ignored, as we are still doing the verification in the checkServerTrusted method.
     *  This is just used to prompt the Windows Trust Store to update itself.
     *
     *  Using the isValidFilePath method makes sure the path we are providing to the ProcessBuilder
     *  is valid and does not contain any invalid characters that could make the process vulnerable to an attack,
     *  such as shell injection
     */
    private void verifyCertificateFileThroughWindowsStore(String filePath) throws IOException, InterruptedException
    {
        boolean isValid = isValidFilePath(filePath);

        if(!isValid)
        {
            logger.error("The file provided for the temporary certificate is not valid, so the process to verify it cannot be invoked");
            throw new IOException();
        }

        ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", "CertUtil", "-verify", filePath);
        Process process = processBuilder.start();

        // The output of the process should be read because that will allow the process to terminate.
        // The result of the output is not needed.
        InputStream inputStream = process.getInputStream();
        inputStream.readAllBytes();

        // Give some time for the process to terminate
        process.waitFor(1000, TimeUnit.MILLISECONDS);

        // Make sure the process has been destroyed
        process.destroy();
    }

    /**
     * Validates the provided filePath
     *
     * Paths.get() provides validation for file paths
     * and checks for invalid characters such as ", ?, <, >, \, *
     *
     * This method also does an additional check whether the file exists
     * on the provided file path
     *
     * @param filePath - the file path to be validated
     * @return whether the file path is valid
     */
    private static boolean isValidFilePath(String filePath)
    {
        if (filePath == null || filePath.isEmpty())
        {
            return false;
        }

        Path path;

        try
        {
            path = Paths.get(filePath);
        }
        catch (InvalidPathException | NullPointerException ex)
        {
            return false;
        }

        return Files.exists(path) && !Files.isDirectory(path); // CodeQL [SM00697] Not Exploitable. The file/path is not user provided. // CodeQL [SM00679] Not Exploitable. The command is not user provided.
    }

    private void cleanUpTempCertificate(String filePath)
    {
        try
        {
            File tempCert = new File(filePath);
            tempCert.delete();
        }
        catch (Exception e)
        {
            logger.error("Deletion of temporary certificate file failed " + e);
        }
    }

    /**
     * Calculates the hash of the certificate known as the "thumbprint"
     * and returns it as a string representation.
     *
     * @param cert The certificate to hash.
     * @param algorithm The hash algorithm to use.
     * @return The SHA-1 hash of the certificate.
     * @throws CertificateException
     */
    private static String getThumbprint(Certificate cert, String algorithm)
            throws CertificateException
    {
        MessageDigest digest;
        try
        {
            digest = MessageDigest.getInstance(algorithm);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new CertificateException(e);
        }
        byte[] encodedCert = cert.getEncoded();
        StringBuilder sb = new StringBuilder(encodedCert.length * 2);
        try (Formatter f = new Formatter(sb))
        {
            for (byte b : digest.digest(encodedCert))
                f.format("%02x", b);
        }
        return sb.toString();
    }
}
