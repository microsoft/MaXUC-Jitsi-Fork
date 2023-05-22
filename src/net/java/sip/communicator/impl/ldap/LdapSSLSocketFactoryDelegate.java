/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.ldap;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.List;

import javax.net.*;

import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.util.*;

/**
 * Utility class to delegate the creation of sockets to LDAP servers to our
 * {@link CertificateService}.
 * <p>
 * @author Ingo Bauersachs
 */
public class LdapSSLSocketFactoryDelegate extends SocketFactory
{
    private static final Logger logger = Logger.getLogger(LdapSSLSocketFactoryDelegate.class);

    private final SocketFactory socketFactory;

    public LdapSSLSocketFactoryDelegate()
    {
        // Looking up LDAP config from the config service directly (as opposed to using the
        // LdapDirectorySettings object) contravenes the intended LDAP design, but we have no
        // choice:
        // - we are forced (by Java LDAP code, see com.sun.jndi.ldap.Connection.createSocket()) to
        //   support unconnected sockets (i.e. creating a socket before connecting to a given
        //   host/port with it)
        // - thus, if we want to verify the SSL certificate we need to know the host name to check
        //   against without it being passed in as a parameter to createSocket
        // - this class is instantiated by Java LDAP code using reflection (see createSocket again),
        //   so we can't pass in the LdapDirectorySettings object
        // - thus we must look it up from config directly
        String host = LdapActivator.getLdapHostnameFromConfig();

        try
        {
            logger.debug("Instantiating LdapSSLSocketFactoryDelegate");
            socketFactory =  LdapActivator.getCertificateService().getSSLContext(
                    LdapActivator.getCertificateService().getTrustManager(List.of(host))).
                getSocketFactory();
        }
        catch (GeneralSecurityException e)
        {
            logger.error(
                "unable to create socket through the certificate service", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Get default SSL socket factory delegate.
     *
     * @return default SSL socket factory delegate.
     */
    public static SocketFactory getDefault()
    {
        logger.debug("LDAP getDefault() called");
        return new LdapSSLSocketFactoryDelegate();
    }

    @Override
    public Socket createSocket() throws IOException
    {
        logger.debug("LDAP createSocket() called");
        return socketFactory.createSocket();
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException
    {
        logger.debug("LDAP createSocket(host, port) called");
        return socketFactory.createSocket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException
    {
        logger.debug("LDAP createSocket(host, port, localHost, localPort) called");
        return socketFactory.createSocket(host, port, localHost, localPort);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException
    {
        logger.debug("LDAP createSocket(iNetHost, port) called");
        return socketFactory.createSocket(host, port);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException
    {
        logger.debug("LDAP createSocket(address, port, localAddress, localPort) called");
        return socketFactory.createSocket(address, port, localAddress, localPort);
    }
}
