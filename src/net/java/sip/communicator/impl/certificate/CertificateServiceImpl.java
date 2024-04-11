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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.Builder;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyStoreBuilderParameters;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import com.google.common.annotations.VisibleForTesting;

import net.java.sip.communicator.launcher.ElectronUILauncher;
import net.java.sip.communicator.plugin.desktoputil.AuthenticationWindow;
import net.java.sip.communicator.plugin.desktoputil.AuthenticationWindow.AuthenticationWindowResult;
import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.analytics.AnalyticsParameter;
import net.java.sip.communicator.service.analytics.AnalyticsService;
import net.java.sip.communicator.service.certificate.CertificateConfigEntry;
import net.java.sip.communicator.service.certificate.CertificateMatcher;
import net.java.sip.communicator.service.certificate.CertificateService;
import net.java.sip.communicator.service.certificate.KeyStoreType;
import net.java.sip.communicator.service.credentialsstorage.CredentialsStorageService;
import net.java.sip.communicator.service.threading.ThreadFactoryBuilder;
import net.java.sip.communicator.util.Logger;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.util.OSUtils;

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

    private final AnalyticsService analyticsService =
            CertificateVerificationActivator.getAnalyticsService();

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

        if (System.getProperty(ElectronUILauncher.WISPA_KEYS_DIR_PROPERTY) != null)
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

        return getConfiguredTrustManager(identitiesToTest, clientVerifier, serverVerifier);
    }

    private X509TrustManager getConfiguredTrustManager(final List<String> identitiesToTest,
                                                       final CertificateMatcher clientVerifier,
                                                       final CertificateMatcher serverVerifier)
            throws GeneralSecurityException
    {
        X509TrustManager trustManager = queryTrustManagerCache(identitiesToTest);

        if (trustManager == null)
        {
            trustManager = new ReloadableX509TrustManager(identitiesToTest, serverVerifier, clientVerifier);
            trustManagerCache.put(identitiesToTest, trustManager);
        }

        return trustManager;
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

    @Override
    public void notifySecureConnectionEstablished(String connection, SSLSession session)
    {
        analyticsService.onEvent(AnalyticsEventType.SECURE_CONNECTION_ESTABLISHED,
                                 AnalyticsParameter.PARAM_TLS_CONNECTION, connection,
                                 AnalyticsParameter.PARAM_TLS_PROTOCOL_VERSION, session.getProtocol(),
                                 AnalyticsParameter.PARAM_TLS_CIPHER, session.getCipherSuite());
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
