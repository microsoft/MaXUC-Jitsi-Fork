/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.certificate;

import static java.nio.file.StandardWatchEventKinds.*;
import static java.util.stream.Collectors.toList;

import java.beans.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.security.*;
import java.security.KeyStore.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.*;
import javax.security.auth.callback.*;

import com.google.common.annotations.VisibleForTesting;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.jcajce.*;
import org.jitsi.service.configuration.*;
import org.jitsi.util.*;

import net.java.sip.communicator.launcher.SIPCommunicator;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.AuthenticationWindow.*;
import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.httputil.*;
import net.java.sip.communicator.service.threading.ThreadFactoryBuilder;
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

    /**
     * Caches retrievals of AIA information (downloaded certs or failures).
     */
    private Map<URI, AiaCacheEntry> aiaCache =
            new HashMap<>();

    /**
     * Caches retrievals of TrustManagers based on the identities they were
     * initialised with.
     */
    private final ConcurrentMap<List<String>, X509TrustManager> trustManagerCache
            = new ConcurrentHashMap<>();

    /**
     * Monitors the macOS keychain for changes to the trusted root certificate
     * store, and performs actions when a change is detected to ensure all
     * subsequent secure connections use the most recent root CAs.
     */
    private final KeychainChangeMonitor monitor;

    /**
     * Auxiliary executor service for the keychain monitor task.
     */
    private final ExecutorService monitorExecutor =
            Executors.newSingleThreadExecutor(
                    new ThreadFactoryBuilder()
                            .setName("keychain-monitor-thread")
                            .build());
    /**
     * Path and filename of the system root certificate keychain on macOS.
     */
    private static String MAC_KEYCHAIN_PATH = "/System/Library/Keychains";
    private static String MAC_KEYCHAIN_FILE =
            "SystemRootCertificates.keychain";

    /**
     * Used to get hold of the client/server keys/certificates for use on the
     * WISPA interface, when using WSS.  Will be null when using WS (as part of
     * a local dev client).
     */
    private WISPACertificateInfo wispaCertificateInfo = null;

    /**
     * Email and hostname matchers for certificates.
     */
    private static final EMailAddressMatcher EmailMatcher =
            new EMailAddressMatcher();

    private static final BrowserLikeHostnameMatcher BrowserMatcher =
            new BrowserLikeHostnameMatcher();

    // ------------------------------------------------------------------------
    // Map access helpers
    // ------------------------------------------------------------------------
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

    /**
     * Monitors the macOS keychain for changes to the trusted root certificate
     * store, and performs actions when a change is detected to ensure all
     * subsequent secure connections use the most recent root CAs.
     */
    private class KeychainChangeMonitor
    {
        private final WatchService monitor;
        /**
         * This eventTrigger is used by the KeychainChangeMonitor to signal when
         * a change to the keychain has occurred.
         */
        private CompletableFuture<Void> eventTrigger;

        KeychainChangeMonitor(Path dir) throws IOException {
            Objects.requireNonNull(dir);
            this.monitor = FileSystems.getDefault().newWatchService();
            dir.register(monitor, ENTRY_MODIFY, ENTRY_CREATE);
            this.eventTrigger = new CompletableFuture<>();
        }

        /**
         * Looping task that monitors the macOS keychain of system root
         * certificates for changes. Once a change is detected, we empty the
         * TrustManager cache and set the eventTrigger for any other tasks
         * listening for updates based on it.
         */
        void startMonitoringKeychain()
        {
            logger.debug("Keychain monitor thread started!");
            while (true)
            {
                WatchKey key;
                try
                {
                    key = monitor.take();
                }
                catch (InterruptedException ex)
                {
                    logger.debug("Keychain monitor thread interrupted!");
                    return;
                }

                if (keychainFileModified(key))
                {
                    logger.debug("macOS keychain updated! " +
                                 "Emptying TrustManager cache.");
                    trustManagerCache.clear();
                    eventTrigger.complete(null);
                }

                boolean isKeyValid = key.reset();
                if (!isKeyValid)
                {
                    break;
                }
            }
        }

        /**
         * Provides the eventTrigger that other tasks can use to monitor whether
         * a keychain update has been detected.
         * <p>
         * Supplies a new trigger for future keychain updates
         * once the old one has expired (i.e. been completed).
         */
        synchronized CompletableFuture<Void> getEventTrigger()
        {
            if (eventTrigger.isDone())
            {
                eventTrigger = new CompletableFuture<>();
            }
            return eventTrigger;
        }

        /**
         * Returns true if a change to the macOS system root certificate
         * keychain file has been detected.
         */
        private boolean keychainFileModified(WatchKey key)
        {
            return key.pollEvents().stream()
                    .map(this::castToPath)
                    .map(WatchEvent::context)
                    .map(path -> path.getFileName().toString())
                    .anyMatch(getMacOSKeychainFileName()::contains);
        }

        /**
         * Utility method to convert WatchEvents to the correct type.
         */
        @SuppressWarnings("unchecked")
        private WatchEvent<Path> castToPath(WatchEvent<?> watchEvent)
        {
            return (WatchEvent<Path>) watchEvent;
        }
    }

    // ------------------------------------------------------------------------
    // Truststore configuration
    // ------------------------------------------------------------------------
    /**
     * Initializes a new <tt>CertificateServiceImpl</tt> instance.
     */
    public CertificateServiceImpl() throws GeneralSecurityException
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

        if(OSUtils.isMac())
        {
            try
            {
                this.monitor = new KeychainChangeMonitor(
                        Path.of(getMacOSKeychainPath()));
                monitorExecutor.submit(monitor::startMonitoringKeychain);
            }
            catch (IOException e)
            {
                throw new GeneralSecurityException(
                        "Failed to create macOS keychain monitor", e);
            }
        }
        else
        {
            this.monitor = null;
        }
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        setTrustStore();
    }

    private void setTrustStore()
    {
        System.getProperties().remove("javax.net.ssl.trustStoreType");
        if (OSUtils.isWindows())
        {
            System.getProperties().remove("javax.net.ssl.trustStore");
            System.getProperties().remove("javax.net.ssl.trustStorePassword");
        }
        else
        {
            String tsFile = (String)config.global().getProperty(PNAME_TRUSTSTORE_FILE);
            String tsPassword = credService.global().loadPassword(PNAME_TRUSTSTORE_PASSWORD);
            if(tsFile != null)
                System.setProperty("javax.net.ssl.trustStore", tsFile);
            else
                System.getProperties().remove("javax.net.ssl.trustStore");

            if(tsPassword != null)
                System.setProperty("javax.net.ssl.trustStorePassword", tsPassword);
            else
                System.getProperties().remove("javax.net.ssl.trustStorePassword");
        }
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

    /**
     * @see CertificateService#getSSLContext()
     */
    public SSLContext getSSLContext() throws GeneralSecurityException
    {
        return getSSLContext(getTrustManager(Collections.emptyList()));
    }

    /**
     * @see CertificateService#getSSLContext(javax.net.ssl.X509TrustManager)
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

    /**
     * @see CertificateService#getSSLContext(java.lang.String,
     * javax.net.ssl.X509TrustManager)
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

    /**
     * @see CertificateService#getSSLContext(javax.net.ssl.KeyManager[],
     * javax.net.ssl.X509TrustManager)
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

    /**
     * @see CertificateService#getTrustManager()
     */
    @Override
    public X509TrustManager getTrustManager() throws GeneralSecurityException
    {
        return getTrustManager(
                new ArrayList<>(),
                EmailMatcher,
                BrowserMatcher
        );
    }

    /**
     * @see CertificateService#getTrustManager(java.util.List)
     */
    public X509TrustManager getTrustManager(List<String> identitiesToTest)
        throws GeneralSecurityException
    {
        return getTrustManager(
            identitiesToTest,
            EmailMatcher,
            BrowserMatcher
        );
    }

    /**
     * (non-Javadoc)
     *
     * @see CertificateService#getTrustManager(java.util.List,
     * net.java.sip.communicator.service.certificate.CertificateMatcher,
     * net.java.sip.communicator.service.certificate.CertificateMatcher)
     */
    public X509TrustManager getTrustManager(
        final List<String> identitiesToTest,
        final CertificateMatcher clientVerifier,
        final CertificateMatcher serverVerifier)
        throws GeneralSecurityException
    {
        Objects.requireNonNull(identitiesToTest);
        X509TrustManager cachedTm = queryTrustManagerCache(identitiesToTest);
        if (cachedTm != null)
        {
            logger.debug("Cached TrustManager found with identities: " +
                         identitiesToTest);
            return cachedTm;
        }

        logger.debug("Creating new TrustManager with identities: " +
                     identitiesToTest);
        // obtain the default X509 trust manager
        X509TrustManager defaultTm = null;
        TrustManagerFactory tmFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory
                .getDefaultAlgorithm());

        tmFactory.init(initialiseKeyStore());
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

        final X509TrustManager tmToReturn = new X509TrustManager()
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
                       if(identitiesToTest.isEmpty())
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
                        List<String> storedCerts = new LinkedList<>();

                        if (identitiesToTest.isEmpty())
                        {
                            String propName =
                                PNAME_CERT_TRUST_PREFIX + ".server." + thumbprint;

                            // get the thumbprints from the permanent allowances
                            String hashes = config.global().getString(propName);
                            if (hashes != null)
                                storedCerts.addAll(Arrays.asList(hashes.split(",")));
                        }
                        else
                        {
                            for (String identity : identitiesToTest)
                            {
                                String propName =
                                    PNAME_CERT_TRUST_PREFIX + ".param." + identity;

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

        trustManagerCache.put(identitiesToTest, tmToReturn);
        return tmToReturn;
    }

    /**
     * Checks if there is a cached TrustManager associated with a given set of
     * identities.
     */
    private X509TrustManager queryTrustManagerCache(List<String> identitiesToTest)
    {
        List<String> sorted = identitiesToTest.stream()
                .sorted()
                .collect(toList());
        return trustManagerCache.get(sorted);
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
        return ks;
    }

    /**
     * Returns the path to the directory of the file containing
     * the system root certificates on macOS.
     */
    public static String getMacOSKeychainPath()
    {
        return MAC_KEYCHAIN_PATH;
    }

    /**
     * Returns the name of the file containing the system root certificates on
     * macOS.
     */
    public static String getMacOSKeychainFileName()
    {
        return MAC_KEYCHAIN_FILE;
    }

    @VisibleForTesting
    public static void setMacOSKeychainFileName(String filename)
    {
        MAC_KEYCHAIN_FILE = filename;
    }

    @VisibleForTesting
    public static void setMacOSKeychainPath(String path)
    {
        MAC_KEYCHAIN_PATH = path;
    }

    /**
     * Returns the event trigger from the KeychainChangeMonitor that signals
     * when an update to the keychain has occurred. macOS only.
     */
    @Override
    public CompletableFuture<Void> getMacOSKeychainUpdateTrigger()
    {
        if (OSUtils.isMac())
        {
            return monitor.getEventTrigger().copy();
        }
        else
        {
            return CompletableFuture.completedFuture(null);
        }
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

    protected static class BrowserLikeHostnameMatcher
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

    protected static class EMailAddressMatcher
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
