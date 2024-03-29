/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.certificate;

import java.io.InputStream;
import java.security.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.*;

/**
 * A service which implementors will ask the user for permission for the
 * certificates which are for some reason not valid and not globally trusted.
 *
 * @author Damian Minkov
 * @author Ingo Bauersachs
 */
public interface CertificateService
{
    // ------------------------------------------------------------------------
    // Configuration property names
    // ------------------------------------------------------------------------
    /**
     * Property for always trust mode. When enabled certificate check is
     * skipped.
     */
    String PNAME_ALWAYS_TRUST =
        "net.java.sip.communicator.service.gui.ALWAYS_TRUST_MODE_ENABLED";

    /**
     * The property name prefix of all client authentication configurations.
     */
    String PNAME_CLIENTAUTH_CERTCONFIG_BASE =
        "net.java.sip.communicator.service.cert.clientauth";

    /**
     * Property that is being applied to the system property
     * <tt>javax.net.ssl.trustStoreType</tt>
     */
    String PNAME_TRUSTSTORE_TYPE =
        "net.java.sip.communicator.service.cert.truststore.type";

    /**
     * Property that is being applied to the system property
     * <tt>javax.net.ssl.trustStore</tt>
     */
    String PNAME_TRUSTSTORE_FILE =
        "net.java.sip.communicator.service.cert.truststore.file";

    /**
     * Property that is being applied to the system property
     * <tt>javax.net.ssl.trustStorePassword</tt>
     */
    String PNAME_TRUSTSTORE_PASSWORD =
        "net.java.sip.communicator.service.cert.truststore.password";

    /**
     * Property that is being applied to the system properties
     * <tt>com.sun.net.ssl.checkRevocation</tt> and
     * <tt>com.sun.security.enableCRLDP</tt>
     */
    String PNAME_REVOCATION_CHECK_ENABLED =
        "net.java.sip.communicator.service.cert.revocation.enabled";

    /**
     * Property that is being applied to the Security property
     * <tt>ocsp.enable</tt>
     */
    String PNAME_OCSP_ENABLED =
        "net.java.sip.communicator.service.cert.ocsp.enabled";

    // ------------------------------------------------------------------------
    // constants
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Client authentication configuration
    // ------------------------------------------------------------------------
    /**
     * Returns all saved {@link CertificateConfigEntry}s.
     *
     * @return List of the saved authentication configurations.
     */
    List<CertificateConfigEntry> getClientAuthCertificateConfigs();

    /**
     * Gets a list of all supported KeyStore types.
     *
     * @return a list of all supported KeyStore types.
     */
    List<KeyStoreType> getSupportedKeyStoreTypes();

    // ------------------------------------------------------------------------
    // Certificate trust handling
    // ------------------------------------------------------------------------
    /**
     * Get an SSL Context that validates certificates based on the JRE default
     * check and asks the user when the JRE check fails.
     *
     * CAUTION: Only the certificate itself is validated, no check is performed
     * whether it is valid for a specific server or client.
     *
     * @return An SSL context based on a user confirming trust manager.
     * @throws GeneralSecurityException
     */
    SSLContext getSSLContext() throws GeneralSecurityException;

    /**
     * Get an SSL Context with the specified trustmanager.
     *
     * @param trustManager The trustmanager that will be used by the created
     *            SSLContext
     * @return An SSL context based on the supplied trust manager.
     * @throws GeneralSecurityException
     */
    SSLContext getSSLContext(X509TrustManager trustManager)
        throws GeneralSecurityException;

    /**
     * Get an SSL Context with the specified trustmanager.
     *
     * @param clientCertConfig The ID of a client certificate configuration
     *            entry that is to be used when the server asks for a client TLS
     *            certificate
     * @param trustManager The trustmanager that will be used by the created
     *            SSLContext
     * @return An SSL context based on the supplied trust manager.
     * @throws GeneralSecurityException
     */
    SSLContext getSSLContext(String clientCertConfig,
                             X509TrustManager trustManager)
        throws GeneralSecurityException;

    /**
     * Get an SSL Context with the specified trustmanager.
     *
     * @param keyManagers The key manager(s) to be used for client
     *            authentication
     * @param trustManager The trustmanager that will be used by the created
     *            SSLContext
     * @return An SSL context based on the supplied trust manager.
     * @throws GeneralSecurityException
     */
    SSLContext getSSLContext(KeyManager[] keyManagers,
                             X509TrustManager trustManager)
        throws GeneralSecurityException;

    /**
     * Creates a trustmanager that validates the certificate based on the JRE
     * default check and asks the user when the JRE check fails. No check is
     * performed as to whether the certificate is valid for a specific server
     * or client.
     *
     * @return TrustManager to use in an SSLContext
     * @throws GeneralSecurityException
     */
    X509TrustManager getTrustManager()
        throws GeneralSecurityException;

    /**
     * Creates a trustmanager that validates the certificate based on the JRE
     * default check and asks the user when the JRE check fails. When
     * <tt>null</tt> is passed as the <tt>identityToTest</tt> then no check is
     * performed whether the certificate is valid for a specific server or
     * client. The passed identities are checked by applying a behavior similar
     * to the on regular browsers use.
     *
     * @param identitiesToTest when not <tt>null</tt>, the values are assumed
     *            to be hostnames for invocations of checkServerTrusted and
     *            e-mail addresses for invocations of checkClientTrusted
     * @return TrustManager to use in an SSLContext
     * @throws GeneralSecurityException
     */
    X509TrustManager getTrustManager(List<String> identitiesToTest)
        throws GeneralSecurityException;

    /**
     * Creates a trustmanager that validates the certificate based on the JRE
     * default check and asks the user when the JRE check fails. When
     * <tt>null</tt> is passed as the <tt>identityToTest</tt> then no check is
     * performed whether the certificate is valid for a specific server or
     * client.
     *
     * @param identitiesToTest The identities to match against the supplied
     *            verifiers.
     * @param clientVerifier The verifier to use in calls to checkClientTrusted
     * @param serverVerifier The verifier to use in calls to checkServerTrusted
     * @return TrustManager to use in an SSLContext
     * @throws GeneralSecurityException
     */
    X509TrustManager getTrustManager(
            final List<String> identitiesToTest,
            final CertificateMatcher clientVerifier,
            final CertificateMatcher serverVerifier)
        throws GeneralSecurityException;

    /**
     * Allow access to the event trigger that signals
     * when an update to the keychain has occurred. macOS only.
     *
     * @return the event trigger for macOS keychain updates.
     */
    CompletableFuture<Void> getMacOSKeychainUpdateTrigger();

    /**
     * Whether to use WSS (as opposed to WS) for the WISPA connection.  We will
     * use WSS only for packaged applications.  Dev builds use WS.
     *
     * @return True if using WSS, false if using WS
     */
    boolean useSecureWispa();

    /**
     * Read the WISPA server key store for the current invocation of the
     * application.  This contains a single, self-signed certificate that the
     * server will present to connecting clients.
     *
     * @return An open input stream from which the key store information can be
     * read.
     */
    InputStream getWispaKeyStore();

    /**
     * Read the WISPA server trust store for the current invocation of the
     * application.  This contains a single trusted certificate - specifically,
     * the self-signed client certificate that was generated at the same time
     * as the server certificate.
     *
     * @return An open input stream from which the trust store information can
     * be read.
     */
    InputStream getWispaTrustStore();

    /**
     * Get the password required to access the WISPA server key store and trust
     * store.
     *
     * @return The WISPA server store password
     */
    String getWispaStorePassword();

    /**
     * Get the format of the WISPA server key store and trust store
     *
     * @return The WISPA server store format
     */
    String getWispaStoreFormat();

    /**
     * Notifies about a new established TLS connection for the specified protocol.
     *
     * @param connection the connection for e.g. STRP or XMPP
     * @param session secure session containing parameters of the connection
     */
    void notifySecureConnectionEstablished(String connection, SSLSession session);
}
