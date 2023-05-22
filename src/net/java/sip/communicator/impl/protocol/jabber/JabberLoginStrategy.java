/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import java.io.IOException;
import java.security.GeneralSecurityException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import net.java.sip.communicator.service.certificate.CertificateService;
import net.java.sip.communicator.service.protocol.SecurityAuthority;
import net.java.sip.communicator.service.protocol.UserCredentials;

/**
 * Is responsible to configure the login mechanism for smack
 * and later login to the XMPP server.
 *
 * @author Stefan Sieber
 */
public interface JabberLoginStrategy
{
    /**
     * Prepare the login by e.g. asking the user for his password.
     *
     * @param authority SecurityAuthority to obtain the password
     * @param reasonCode reason why we're preparing for login
     * @return UserCredentials in case they need to be cached for this session
     *         (i.e. password is not persistent)
     *
     * @see SecurityAuthority
     */
    UserCredentials prepareLogin(SecurityAuthority authority,
                                 int reasonCode);

    /**
     * Determines whether the login preparation was successful and the strategy
     * is ready to start connecting.
     *
     * @return true if prepareLogin was successful.
     */
    boolean loginPreparationSuccessful();

    /**
     * Performs the login for the specified connection.
     *
     * @param connection Connection  to login
     * @param userName userName to be used for the login.
     * @param resource the XMPP resource
     * @return true to continue connecting, false to abort
     * @throws IOException
     * @throws InterruptedException
     * @throws SmackException
     * @throws XMPPException
     */
    boolean login(AbstractXMPPConnection connection, String userName,
                  String resource)
            throws IOException, InterruptedException, SmackException,
                   XMPPException;

    /**
     * Creates an SSLContext to use for the login strategy.
     * @param certificateService  certificate service to retrieve the
     *                            ssl context
     * @param trustManager Trust manager to use for the context
     *
     * @return the SSLContext
     */
    SSLContext createSslContext(CertificateService certificateService,
                                X509TrustManager trustManager)
            throws GeneralSecurityException;
}
