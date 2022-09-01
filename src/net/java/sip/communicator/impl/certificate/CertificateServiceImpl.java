/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.certificate;

import java.beans.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.security.*;
import java.security.KeyStore.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.*;

import javax.net.ssl.*;
import javax.security.auth.callback.*;
import javax.swing.*;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.jcajce.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import net.java.sip.communicator.launcher.SIPCommunicator;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.AuthenticationWindow.*;
import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.httputil.*;
import net.java.sip.communicator.util.Logger;

/**
 * Implementation of the CertificateService. It asks the user to trust a
 * certificate when the automatic verification fails.
 *
 * @author Ingo Bauersachs
 */
public class CertificateServiceImpl
    implements CertificateService, PropertyChangeListener
{
    // ------------------------------------------------------------------------
    // static data
    // ------------------------------------------------------------------------
    private final List<KeyStoreType> supportedTypes = new LinkedList<>()
            {
                /**
                 * Serial version UID.
                 */
                private static final long serialVersionUID = 0L;

                {
                    if (!OSUtils.IS_WINDOWS64)
                    {
                        add(new KeyStoreType("PKCS11", new String[]
                                {".dll", ".so"}, false));
                    }
                    add(new KeyStoreType("PKCS12", new String[]
                            {".p12", ".pfx"}, true));
                    add(new KeyStoreType(KeyStore.getDefaultType(), new String[]
                            {".ks", ".jks"}, true));
                }
            };

    // ------------------------------------------------------------------------
    // services
    // ------------------------------------------------------------------------
    private static final Logger logger =
        Logger.getLogger(CertificateServiceImpl.class);

    private final ResourceManagementService R =
        CertificateVerificationActivator.getResources();

    private final ConfigurationService config =
        CertificateVerificationActivator.getConfigurationService();

    private final CredentialsStorageService credService =
        CertificateVerificationActivator.getCredService();

    // ------------------------------------------------------------------------
    // properties
    // ------------------------------------------------------------------------
    /**
     * Base property name for the storage of certificate user preferences.
     */
    private static final String PNAME_CERT_TRUST_PREFIX =
        "net.java.sip.communicator.impl.certservice";

    /** Hash algorithm for the cert thumbprint*/
    private static final String THUMBPRINT_HASH_ALGORITHM = "SHA1";

    // ------------------------------------------------------------------------
    // fields
    // ------------------------------------------------------------------------
    /**
     * Stores the certificates that are trusted as long as this service lives.
     */
    private Map<String, List<String>> sessionAllowedCertificates =
            new HashMap<>();

    /**
     * Caches retrievals of AIA information (downloaded certs or failures).
     */
    private Map<URI, AiaCacheEntry> aiaCache =
            new HashMap<>();

    /**
     * Store object reference in order to prevent duplicate dialogs.
     */
    private static VerifyCertificateDialog sCertificateDialog = null;

    /**
     * Used to get hold of the client/server keys/certificates for use on the
     * WISPA interface, when using WSS.  Will be null when using WS (as part of
     * a local dev client).
     */
    private WISPACertificateInfo wispaCertificateInfo = null;

    // ------------------------------------------------------------------------
    // Map access helpers
    // ------------------------------------------------------------------------
    /**
     * Helper method to avoid accessing null-lists in the session allowed
     * certificate map
     *
     * @param propName the key to access
     * @return the list for the given list or a new, empty list put in place for
     *         the key
     */
    private List<String> getSessionCertEntry(String propName)
    {
        return sessionAllowedCertificates.computeIfAbsent(propName,
                                                          k -> new LinkedList<>());
    }

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

    // ------------------------------------------------------------------------
    // Truststore configuration
    // ------------------------------------------------------------------------
    /**
     * Initializes a new <tt>CertificateServiceImpl</tt> instance.
     */
    public CertificateServiceImpl()
    {
        setTrustStore();
        config.global().addPropertyChangeListener(PNAME_TRUSTSTORE_TYPE, this);

        System.setProperty("com.sun.security.enableCRLDP",
            config.global().getString(PNAME_REVOCATION_CHECK_ENABLED, "false"));
        System.setProperty("com.sun.net.ssl.checkRevocation",
            config.global().getString(PNAME_REVOCATION_CHECK_ENABLED, "false"));
        Security.setProperty("ocsp.enable",
            config.global().getString(PNAME_OCSP_ENABLED, "false"));

        if (System.getProperty(SIPCommunicator.WISPA_KEYS_DIR_PROPERTY) != null)
        {
            // Generate the certificates required for the WISPA interface.
            wispaCertificateInfo = new WISPACertificateInfo();
        }
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        setTrustStore();
    }

    private void setTrustStore()
    {
//        String tsType = (String)config.global().getProperty(PNAME_TRUSTSTORE_TYPE);
        String tsFile = (String)config.global().getProperty(PNAME_TRUSTSTORE_FILE);
        String tsPassword = credService.global().loadPassword(PNAME_TRUSTSTORE_PASSWORD);

//        // use the OS store as default store on Windows
//        if (tsType == null
//            && !"meta:default".equals(tsType)
//            && OSUtils.IS_WINDOWS)
//        {
//            config.global().setProperty(PNAME_TRUSTSTORE_TYPE, "Windows-ROOT");
//        }

//        if(tsType != null && !"meta:default".equals(tsType))
//            System.setProperty("javax.net.ssl.trustStoreType", tsType);
//        else
//        Revert fix from jitsi core. We never want to use the Windows store
// (for now). By removing the system property, we always use the default store (jks)
            System.getProperties().remove("javax.net.ssl.trustStoreType");

        if(tsFile != null)
            System.setProperty("javax.net.ssl.trustStore", tsFile);
        else
            System.getProperties().remove("javax.net.ssl.trustStore");

        if(tsPassword != null)
            System.setProperty("javax.net.ssl.trustStorePassword", tsPassword);
        else
            System.getProperties().remove("javax.net.ssl.trustStorePassword");
    }

    // ------------------------------------------------------------------------
    // Client authentication configuration
    // ------------------------------------------------------------------------
    /*
     * (non-Javadoc)
     *
     * @see net.java.sip.communicator.service.certificate.CertificateService#
     * getSupportedKeyStoreTypes()
     */
    public List<KeyStoreType> getSupportedKeyStoreTypes()
    {
        return supportedTypes;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.java.sip.communicator.service.certificate.CertificateService#
     * getClientAuthCertificateConfigs()
     */
    public List<CertificateConfigEntry> getClientAuthCertificateConfigs()
    {
        List<CertificateConfigEntry> map =
                new LinkedList<>();
        for (String propName : config.global().getPropertyNamesByPrefix(
            PNAME_CLIENTAUTH_CERTCONFIG_BASE, false))
        {
            String propValue = config.global().getString(propName);
            if(propValue == null || !propName.endsWith(propValue))
                continue;

            String pnBase = PNAME_CLIENTAUTH_CERTCONFIG_BASE
                + "." + propValue;
            CertificateConfigEntry e = new CertificateConfigEntry();
            e.setId(propValue);
            e.setAlias(config.global().getString(pnBase + ".alias"));
            e.setDisplayName(config.global().getString(pnBase + ".displayName"));
            e.setKeyStore(config.global().getString(pnBase + ".keyStore"));
            e.setSavePassword(config.global().getBoolean(pnBase + ".savePassword", false));
            if(e.isSavePassword())
            {
                e.setKeyStorePassword(credService.global().loadPassword(pnBase));
            }
            String type = config.global().getString(pnBase + ".keyStoreType");
            for(KeyStoreType kt : getSupportedKeyStoreTypes())
            {
                if(kt.getName().equals(type))
                {
                    e.setKeyStoreType(kt);
                    break;
                }
            }
            map.add(e);
        }
        return map;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.java.sip.communicator.service.certificate.CertificateService#
     * setClientAuthCertificateConfig
     * (net.java.sip.communicator.service.certificate.CertificateConfigEntry)
     */
    public void setClientAuthCertificateConfig(CertificateConfigEntry e)
    {
        if (e.getId() == null)
            e.setId("conf" + Math.abs(new SecureRandom().nextInt()));
        String pn = PNAME_CLIENTAUTH_CERTCONFIG_BASE + "." + e.getId();
        config.global().setProperty(pn, e.getId());
        config.global().setProperty(pn + ".alias", e.getAlias());
        config.global().setProperty(pn + ".displayName", e.getDisplayName());
        config.global().setProperty(pn + ".keyStore", e.getKeyStore());
        config.global().setProperty(pn + ".savePassword", e.isSavePassword());
        if (e.isSavePassword())
            credService.global().storePassword(pn, e.getKeyStorePassword());
        else
            credService.global().removePassword(pn);
        config.global().setProperty(pn + ".keyStoreType", e.getKeyStoreType());
    }

    /*
     * (non-Javadoc)
     *
     * @see net.java.sip.communicator.service.certificate.CertificateService#
     * removeClientAuthCertificateConfig(java.lang.String)
     */
    public void removeClientAuthCertificateConfig(String id)
    {
        for (String p : config.global().getPropertyNamesByPrefix(
            PNAME_CLIENTAUTH_CERTCONFIG_BASE + "." + id, true))
        {
            config.global().removeProperty(p);
        }
        config.global().removeProperty(PNAME_CLIENTAUTH_CERTCONFIG_BASE + "." + id);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.java.sip.communicator.service.certificate.CertificateService#
     * getSSLContext()
     */
    public SSLContext getSSLContext() throws GeneralSecurityException
    {
        return getSSLContext(getTrustManager((Iterable<String>)null));
    }

    /*
     * (non-Javadoc)
     *
     * @see net.java.sip.communicator.service.certificate.CertificateService#
     * getSSLContext(javax.net.ssl.X509TrustManager)
     */
    public SSLContext getSSLContext(X509TrustManager trustManager)
        throws GeneralSecurityException
    {
        try
        {
            KeyStore ks =
                KeyStore.getInstance(System.getProperty(
                    "javax.net.ssl.keyStoreType", KeyStore.getDefaultType()));
            KeyManagerFactory kmFactory =
                KeyManagerFactory.getInstance(KeyManagerFactory
                    .getDefaultAlgorithm());

            String keyStorePassword =
                System.getProperty("javax.net.ssl.keyStorePassword");
            if (System.getProperty("javax.net.ssl.keyStore") != null)
            {
                ks.load(
                    new FileInputStream(System
                        .getProperty("javax.net.ssl.keyStore")), null);
            }
            else
            {
                ks.load(null, null);
            }

            kmFactory.init(ks, keyStorePassword == null ? null
                : keyStorePassword.toCharArray());
            return getSSLContext(kmFactory.getKeyManagers(), trustManager);
        }
        catch (Exception e)
        {
            throw new GeneralSecurityException("Cannot init SSLContext", e);
        }
    }

    private Builder loadKeyStore(final CertificateConfigEntry entry)
    {
        final File f = new File(entry.getKeyStore());
        final KeyStoreType kt = entry.getKeyStoreType();
        if ("PKCS11".equals(kt.getName()))
        {
            String config =
                "name=" + f.getName() + "\nlibrary=" + f.getAbsoluteFile();
            try
            {
                Class<?> pkcs11c =
                    Class.forName("sun.security.pkcs11.SunPKCS11");
                Constructor<?> c = pkcs11c.getConstructor(InputStream.class);
                Provider p =
                    (Provider) c.newInstance(new ByteArrayInputStream(config
                        .getBytes()));
                Security.insertProviderAt(p, 0);
            }
            catch (Exception e)
            {
                logger.error("Tried to access the PKCS11 provider on an "
                    + "unsupported platform or the load failed", e);
            }
        }
        KeyStore.Builder ksBuilder =
            KeyStore.Builder.newInstance(kt.getName(), null, f,
                new KeyStore.CallbackHandlerProtection(new CallbackHandler()
                {
                    public void handle(Callback[] callbacks)
                        throws IOException,
                        UnsupportedCallbackException
                    {
                        for (Callback cb : callbacks)
                        {
                            if (!(cb instanceof PasswordCallback))
                                throw new UnsupportedCallbackException(cb);

                            PasswordCallback pwcb = (PasswordCallback) cb;
                            if (entry.isSavePassword())
                            {
                                pwcb.setPassword(entry.getKeyStorePassword()
                                    .toCharArray());
                                return;
                            }
                            else
                            {
                                AuthenticationWindowResult result =
                                    AuthenticationWindow.getAuthenticationResult(
                                        f.getName(),
                                        null,
                                        kt.getName(),
                                        false,
                                        false);

                                if (!result.isCanceled())
                                    pwcb.setPassword(result.getPassword());
                                else
                                    throw new IOException("User cancel");
                            }
                        }
                    }
                }));
        return ksBuilder;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.java.sip.communicator.service.certificate.CertificateService#
     * getSSLContext(java.lang.String, javax.net.ssl.X509TrustManager)
     */
    public SSLContext getSSLContext(String clientCertConfig,
        X509TrustManager trustManager)
        throws GeneralSecurityException
    {
        try
        {
            if(clientCertConfig == null)
                return getSSLContext(trustManager);

            CertificateConfigEntry entry = null;
            for (CertificateConfigEntry e : getClientAuthCertificateConfigs())
            {
                if (e.getId().equals(clientCertConfig))
                {
                    entry = e;
                    break;
                }
            }
            if (entry == null)
                throw new GeneralSecurityException(
                    "Client certificate config with id <"
                    + clientCertConfig
                    + "> not found."
                );

            final KeyManagerFactory kmf =
                KeyManagerFactory.getInstance("NewSunX509");
            kmf.init(new KeyStoreBuilderParameters(loadKeyStore(entry)));

            return getSSLContext(kmf.getKeyManagers(), trustManager);
        }
        catch (Exception e)
        {
            throw new GeneralSecurityException("Cannot init SSLContext", e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see net.java.sip.communicator.service.certificate.CertificateService#
     * getSSLContext(javax.net.ssl.KeyManager[], javax.net.ssl.X509TrustManager)
     */
    public SSLContext getSSLContext(KeyManager[] keyManagers,
        X509TrustManager trustManager)
        throws GeneralSecurityException
    {
        try
        {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(
                keyManagers,
                new TrustManager[] { trustManager },
                null
            );

            return sslContext;
        }
        catch (Exception e)
        {
            throw new GeneralSecurityException("Cannot init SSLContext", e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.java.sip.communicator.service.certificate
     * .CertificateService#getTrustManager()
     */
    @Override
    public X509TrustManager getTrustManager() throws GeneralSecurityException
    {
        return getTrustManager(
                new ArrayList<>(),
            new EMailAddressMatcher(),
            new BrowserLikeHostnameMatcher()
        );
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.java.sip.communicator.service.certificate
     * .CertificateService#getTrustManager(java.lang.Iterable)
     */
    public X509TrustManager getTrustManager(Iterable<String> identitiesToTest)
        throws GeneralSecurityException
    {
        return getTrustManager(
            identitiesToTest,
            new EMailAddressMatcher(),
            new BrowserLikeHostnameMatcher()
        );
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.java.sip.communicator.service.certificate.CertificateService
     * #getTrustManager(java.lang.String)
     */
    public X509TrustManager getTrustManager(String identityToTest)
        throws GeneralSecurityException
    {
        return getTrustManager(
            Arrays.asList(identityToTest),
            new EMailAddressMatcher(),
            new BrowserLikeHostnameMatcher()
        );
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.java.sip.communicator.service.certificate.CertificateService
     * #getTrustManager(java.lang.String,
     * net.java.sip.communicator.service.certificate.CertificateMatcher,
     * net.java.sip.communicator.service.certificate.CertificateMatcher)
     */
    public X509TrustManager getTrustManager(
        String identityToTest,
        CertificateMatcher clientVerifier,
        CertificateMatcher serverVerifier)
        throws GeneralSecurityException
    {
        return getTrustManager(
            Arrays.asList(identityToTest),
            clientVerifier,
            serverVerifier
        );
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.java.sip.communicator.service.certificate.CertificateService
     * #getTrustManager(java.lang.Iterable,
     * net.java.sip.communicator.service.certificate.CertificateMatcher,
     * net.java.sip.communicator.service.certificate.CertificateMatcher)
     */
    public X509TrustManager getTrustManager(
        final Iterable<String> identitiesToTest,
        final CertificateMatcher clientVerifier,
        final CertificateMatcher serverVerifier)
        throws GeneralSecurityException
    {
        // obtain the default X509 trust manager
        X509TrustManager defaultTm = null;
        TrustManagerFactory tmFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory
                .getDefaultAlgorithm());
        tmFactory.init((KeyStore) null);
        for (TrustManager m : tmFactory.getTrustManagers())
        {
            if (m instanceof X509TrustManager)
            {
                defaultTm = (X509TrustManager) m;
                break;
            }
        }
        if (defaultTm == null)
            throw new GeneralSecurityException(
                "No default X509 trust manager found");

        final X509TrustManager tm = defaultTm;

        return new X509TrustManager()
        {
            private boolean serverCheck;

            public X509Certificate[] getAcceptedIssuers()
            {
                return tm.getAcceptedIssuers();
            }

            public void checkServerTrusted(X509Certificate[] chain,
                String authType) throws CertificateException
            {
                serverCheck = true;
                checkCertTrusted(chain, authType);
            }

            public void checkClientTrusted(X509Certificate[] chain,
                String authType) throws CertificateException
            {
                serverCheck = false;
                checkCertTrusted(chain, authType);
            }

            private void checkCertTrusted(X509Certificate[] chain,
                String authType) throws CertificateException
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
                        alwaysTrustMode = config.user().getBoolean(PNAME_ALWAYS_TRUST,
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
                            " tm: " + tm);

                        if(serverCheck)
                        {
                            tm.checkServerTrusted(chain, authType);
                        }
                        else
                        {
                            tm.checkClientTrusted(chain, authType);
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
                       if(identitiesToTest == null
                            || !identitiesToTest.iterator().hasNext())
                       {
                           return;
                       }
                       else
                       {
                           try
                           {
                               if(serverCheck)
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
                        String thumbprint = getThumbprint(
                            chain[0], THUMBPRINT_HASH_ALGORITHM);
                        String message = null;
                        List<String> propNames = new LinkedList<>();
                        List<String> storedCerts = new LinkedList<>();
                        String appName =
                            R.getSettingsString("service.gui.APPLICATION_NAME");

                        if (identitiesToTest == null
                            || !identitiesToTest.iterator().hasNext())
                        {
                            String propName =
                                PNAME_CERT_TRUST_PREFIX + ".server." + thumbprint;
                            propNames.add(propName);

                            message =
                                R.getI18NString("service.gui."
                                    + "CERT_DIALOG_DESCRIPTION_TXT_NOHOST",
                                    new String[] {
                                        appName
                                    }
                                );

                            // get the thumbprints from the permanent allowances
                            String hashes = config.global().getString(propName);
                            if (hashes != null)
                                for(String h : hashes.split(","))
                                    storedCerts.add(h);

                            // get the thumbprints from the session allowances
                            List<String> sessionCerts =
                                sessionAllowedCertificates.get(propName);
                            if (sessionCerts != null)
                                storedCerts.addAll(sessionCerts);
                        }
                        else
                        {
                            if (serverCheck)
                            {
                                message =
                                    R.getI18NString(
                                        "service.gui."
                                        + "CERT_DIALOG_DESCRIPTION_TXT",
                                        new String[] {
                                            appName,
                                            identitiesToTest.toString()
                                        }
                                    );
                            }
                            else
                            {
                                message =
                                    R.getI18NString(
                                        "service.gui."
                                        + "CERT_DIALOG_PEER_DESCRIPTION_TXT",
                                        new String[] {
                                            appName,
                                            identitiesToTest.toString()
                                        }
                                    );
                            }
                            for (String identity : identitiesToTest)
                            {
                                String propName =
                                    PNAME_CERT_TRUST_PREFIX + ".param." + identity;
                                propNames.add(propName);

                                // get the thumbprints from the permanent allowances
                                String hashes = config.global().getString(propName);
                                if (hashes != null)
                                    for(String h : hashes.split(","))
                                        storedCerts.add(h);

                                // get the thumbprints from the session allowances
                                List<String> sessionCerts =
                                    sessionAllowedCertificates.get(propName);
                                if (sessionCerts != null)
                                    storedCerts.addAll(sessionCerts);
                            }
                        }

                        if (!storedCerts.contains(thumbprint))
                        {
                            logger.debug("Certificate not stored:" +
                                " stored: " + storedCerts +
                                " thumbprint: " + thumbprint);

                            switch (verify(chain, message))
                            {
                            case DO_NOT_TRUST:
                                throw new CertificateException(
                                    "The peer provided certificate with Subject <"
                                        + chain[0].getSubjectDN()
                                        + "> is not trusted");
                            case TRUST_ALWAYS:
                                for (String propName : propNames)
                                {
                                    String current = config.global().getString(propName);
                                    String newValue = thumbprint;
                                    if (current != null)
                                        newValue += "," + current;
                                    config.global().setProperty(propName, newValue);
                                }
                                break;
                            case TRUST_THIS_SESSION_ONLY:
                                for(String propName : propNames)
                                    getSessionCertEntry(propName).add(thumbprint);
                                break;
                            }
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

            private X509Certificate[] tryBuildChain(X509Certificate[] chain)
                throws IOException,
                URISyntaxException,
                CertificateException
            {
                // Only try to build chains for servers that send only their
                // own cert, but no issuer. This also matches self signed (will
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
                for (X509Certificate cert : chain)
                {
                    newChain.add(cert);
                }

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
                                logger.debug("Parent is self-signed, ignoring");
                        }
                    }
                    chainLookupCount++;
                }
                while (foundParent && chainLookupCount < 10);
                chain = newChain.toArray(chain);
                return chain;
            }
        };
    }

    @Override
    public boolean useSecureWispa()
    {
        return wispaCertificateInfo != null;
    }

    @Override
    public InputStream getWispaKeyStore()
    {
        return useSecureWispa() ? wispaCertificateInfo.getKeyStoreStream() : null;
    }

    @Override
    public InputStream getWispaTrustStore()
    {
        return useSecureWispa() ? wispaCertificateInfo.getTrustStoreStream() : null;
    }

    @Override
    public String getWispaStorePassword()
    {
        return useSecureWispa() ? wispaCertificateInfo.getStorePassword() : null;
    }

    @Override
    public String getWispaStoreFormat()
    {
        return useSecureWispa() ? wispaCertificateInfo.getStoreFormat() : null;
    }

    protected class BrowserLikeHostnameMatcher
        implements CertificateMatcher
    {
        public void verify(Iterable<String> identitiesToTest,
            X509Certificate cert) throws CertificateException
        {
            // check whether one of the hostname is present in the
            // certificate
            boolean oneMatched = false;
            for(String identity : identitiesToTest)
            {
                try
                {
                    org.apache.http.conn.ssl.SSLSocketFactory
                        .BROWSER_COMPATIBLE_HOSTNAME_VERIFIER
                        .verify(identity, cert);
                    oneMatched = true;
                    break;
                }
                catch (SSLException e)
                {}
            }

            if (!oneMatched)
                throw new CertificateException("None of <"
                    + identitiesToTest
                    + "> matched the cert with CN="
                    + cert.getSubjectDN());
        }
    }

    protected class EMailAddressMatcher
        implements CertificateMatcher
    {
        public void verify(Iterable<String> identitiesToTest,
            X509Certificate cert) throws CertificateException
        {
            // check if the certificate contains the E-Mail address(es)
            // in the SAN(s)
            //TODO: extract address from DN (E-field) too?
            boolean oneMatched = false;
            Iterable<String> emails = getSubjectAltNames(cert, 6);
            for(String identity : identitiesToTest)
            {
                for(String email : emails)
                {
                    if(identity.equalsIgnoreCase(email))
                    {
                        oneMatched = true;
                        break;
                    }
                }
            }
            if(!oneMatched)
                throw new CertificateException(
                    "The peer provided certificate with Subject <"
                    + cert.getSubjectDN()
                    + "> contains no SAN for <"
                    + identitiesToTest + ">");
        }
    }

    /**
     * chainToString
     *
     * @param chain An array of <tt>X509Certificate</tt>s representing a certificate chain
     * @return A String representing the certificates in the chain suitable for logging.
     */
    private String chainToString(X509Certificate[] chain)
    {
        StringBuffer result = new StringBuffer("X509 Chain: [");

        for (X509Certificate cert : chain)
        {
            StringBuffer certName = new StringBuffer();

            if (cert == null)
            {
                certName.append("null");
            }
            else
            {
                try
                {
                    certName.append("SN=");
                    certName.append(cert.getSerialNumber());
                    certName.append("\nIssuer=");
                    certName.append(cert.getIssuerX500Principal());
                    certName.append("\nSubject=");
                    certName.append(cert.getSubjectX500Principal());

                    Collection<List<?>> altNames = cert.getSubjectAlternativeNames();

                    if (altNames != null)
                    {
                        for (List<?> altName : altNames)
                        {
                            certName.append("\n");
                            certName.append(altName.get(0));
                            certName.append("=");
                            certName.append(altName.get(1));
                        }
                    }
                }
                catch (CertificateParsingException e)
                {
                    logger.info("Failed to parse certificate: " + certName, e);
                    certName = new StringBuffer("ParseError");
                }
            }

            result.append("{");
            certName.append("\n");
            result.append(certName);
            certName.append("\n");
            result.append("}\n");
        }

        result.append("]");
        return result.toString();
    }

    /**
     * Asks the user whether they trust the supplied chain of certificates.
     *
     * @param chain The chain of the certificates to check with user.
     * @param message A text that describes why the verification failed.
     * @return The result of the user interaction. One of
     *         {@link CertificateService#DO_NOT_TRUST},
     *         {@link CertificateService#TRUST_THIS_SESSION_ONLY},
     *         {@link CertificateService#TRUST_ALWAYS}
     */
    protected int verify(final X509Certificate[] chain, final String message)
    {
        logger.warn("Asking user to verify certificate. " +
                    " chain: " + chainToString(chain) +
                    " with message: " + message);

        if (config.user() != null && config.user().getBoolean(PNAME_NO_USER_INTERACTION, false))
            return DO_NOT_TRUST;

        final VerifyCertificateDialog dialog = sCertificateDialog == null?
            new VerifyCertificateDialog(chain, null, message) : sCertificateDialog;

        sCertificateDialog = dialog;

        try
        {
            // show the dialog in the swing thread and wait for the user
            // choice
            SwingUtilities.invokeAndWait(new Runnable()
            {
                public void run()
                {
                    dialog.setVisible(true);
                }
            });
        }
        catch (Exception e)
        {
            logger.error("Cannot show certificate verification dialog", e);
            return DO_NOT_TRUST;
        }

        if(!dialog.isTrusted)
        {
            logger.warn("User said do not trust");
            logger.user("Do not trust selected");
            return DO_NOT_TRUST;
        }
        else if(dialog.alwaysTrustCheckBox.isSelected())
        {
            logger.user("Always trust selected");
            return TRUST_ALWAYS;
        }
        else
        {
            logger.user("Trust only for this session selected");
            return TRUST_THIS_SESSION_ONLY;
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

    /**
     * Gets the SAN (Subject Alternative Name) of the specified type.
     *
     * @param cert the certificate to extract from
     * @param altNameType The type to be returned
     * @return SAN of the type
     *
     * <PRE>
     * GeneralName ::= CHOICE {
     *                 otherName                   [0]   OtherName,
     *                 rfc822Name                  [1]   IA5String,
     *                 dNSName                     [2]   IA5String,
     *                 x400Address                 [3]   ORAddress,
     *                 directoryName               [4]   Name,
     *                 ediPartyName                [5]   EDIPartyName,
     *                 uniformResourceIdentifier   [6]   IA5String,
     *                 iPAddress                   [7]   OCTET STRING,
     *                 registeredID                [8]   OBJECT IDENTIFIER
     *              }
     * <PRE>
     */
    private static Iterable<String> getSubjectAltNames(X509Certificate cert,
        int altNameType)
    {
        Collection<List<?>> altNames = null;
        try
        {
            altNames = cert.getSubjectAlternativeNames();
        }
        catch (CertificateParsingException e)
        {
            return Collections.emptyList();
        }

        List<String> matchedAltNames = new LinkedList<>();
        for (List<?> item : altNames)
        {
            if (item.contains(altNameType))
            {
                Integer type = (Integer) item.get(0);
                if (type.intValue() == altNameType)
                    matchedAltNames.add((String) item.get(1));
            }
        }
        return matchedAltNames;
    }
}
