/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.sip.net;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;

import javax.net.ssl.*;
import javax.swing.*;

import net.java.sip.communicator.impl.netaddr.NetworkAddressManagerServiceImpl;
import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.ResourceManagementService;

import gov.nist.core.net.*;
import gov.nist.javax.sip.*;
import net.java.sip.communicator.impl.protocol.sip.*;
import net.java.sip.communicator.plugin.desktoputil.ErrorDialog;
import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Manages jain-sip socket creation. When dealing with ssl sockets we interact
 * with the user when the certificate for some reason is not trusted.
 *
 * @author Damian Minkov
 * @author Ingo Bauersachs
 * @author Sebastien Vincent
 */
public class SslNetworkLayer
    implements NetworkLayer
{
     private static final Logger logger = Logger.getLogger(SslNetworkLayer.class);
     private static final String SIP_DSCP_PROPERTY = "net.java.sip.communicator.impl.protocol.SIP_DSCP";

     /** Don't need to synchronize access because it's only used on the EDT. */
     private static boolean sRefreshNetworkConfigDialogShowing = false;

    /**
     * The service we use to interact with user.
     */
    private CertificateService certificateVerification = null;

    /**
     * Creates the network layer.
     */
    public SslNetworkLayer()
    {
        certificateVerification = SipActivator.getCertificateService();
    }

    /**
     * Creates a server with the specified port, listen backlog, and local IP
     * address to bind to. Comparable to
     * "new java.net.ServerSocket(port,backlog,bindAddress);"
     *
     * @param port the port
     * @param backlog backlog
     * @param bindAddress local address to use
     * @return the newly created server socket.
     * @throws IOException problem creating socket.
     */
    public ServerSocket createServerSocket(int port, int backlog,
            InetAddress bindAddress)
            throws IOException
    {
        ServerSocket sock = new ServerSocket(port, backlog, bindAddress);
        // XXX apparently traffic class cannot be set on ServerSocket
        //setTrafficClass(sock);
        return sock;
    }

    /**
     * Creates a stream socket and connects it to the specified port number at
     * the specified IP address.
     *
     * @param address the address to connect.
     * @param port the port to connect.
     * @return the socket
     * @throws IOException problem creating socket.
     */
    public Socket createSocket(InetAddress address, int port)
        throws IOException
    {
        Socket sock = new Socket(address, port);
        setTrafficClass(sock);
        return sock;
    }

    /**
     * Constructs a datagram socket and binds it to any available port on the
     * local host machine. Comparable to "new java.net.DatagramSocket();"
     *
     * @return the datagram socket
     * @throws SocketException problem creating socket.
     */
    public DatagramSocket createDatagramSocket()
        throws SocketException
    {
        DatagramSocket sock = new DatagramSocket();
        setTrafficClass(sock);
        return sock;
    }

    /**
     * Creates a datagram socket, bound to the specified local address.
     * Comparable to "new java.net.DatagramSocket(port,laddr);"
     *
     * @param port local port to use
     * @param laddr local address to bind
     * @return the datagram socket
     * @throws SocketException problem creating socket.
     */
    public DatagramSocket createDatagramSocket(int port, InetAddress laddr)
        throws SocketException
    {
        DatagramSocket sock = new DatagramSocket(port, laddr);
        setTrafficClass(sock);
        return sock;
    }

    /**
     * Creates an SSL server with the specified port, listen backlog, and local
     * IP address to bind to.
     *
     * @param port the port to listen to
     * @param backlog backlog
     * @param bindAddress the address to listen to
     * @return the server socket.
     * @throws IOException problem creating socket.
     */
    public SSLServerSocket createSSLServerSocket(int port, int backlog,
            InetAddress bindAddress)
        throws IOException
    {
        SSLServerSocket sock = (SSLServerSocket) getSSLServerSocketFactory()
            .createServerSocket(port, backlog, bindAddress);
        // XXX apparently traffic class cannot be set on ServerSocket
        // setTrafficClass(sock);
        return sock;
    }

    /**
     * Creates a ssl server socket factory.
     *
     * @return the server socket factory.
     * @throws IOException problem creating factory.
     */
    protected SSLServerSocketFactory getSSLServerSocketFactory()
        throws IOException
    {
        try
        {
            return certificateVerification.getSSLContext()
                .getServerSocketFactory();
        }
        catch (GeneralSecurityException e)
        {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Creates ssl socket factory.
     *
     * @return the socket factory.
     * @throws IOException problem creating ssl socket factory.
     */
    private SSLSocketFactory getSSLSocketFactory(InetAddress address)
        throws IOException
    {
        ProtocolProviderServiceSipImpl provider = null;
        for (ProtocolProviderServiceSipImpl pps : ProtocolProviderServiceSipImpl
            .getAllInstances())
        {
            ProxyConnection conn = pps.getConnection();
            if (conn != null && conn.isSameInetAddress(address))
            {
                provider = pps;
                break;
            }
            else if (conn == null)
            {
                logger.warn("Provider " + pps + " has null connection.");
            }
            else
            {
                InetAddress connAddr = conn.getAddress().getAddress();
                logger.warn("Provider address " +
                            connAddr + " (" + System.identityHashCode(connAddr) +
                            ") does not match requested address " +
                            address + " (" + System.identityHashCode(address) +
                            ")");

                // The IP address that the SIP domain resolves to has changed.
                // This can result from DNS hijacking so do not continue, to be
                // safe. This can also be hit if the domain resolves to
                // different IPs depending on location and the user turned
                // on/off a VPN. So advise the user to restart their client,
                // which will refresh their network config.
                showRefreshNetworkConfigDialog();

                // Shutdown calls to avoid a hanging call frame
                OperationSetBasicTelephonySipImpl telephony
                        = (OperationSetBasicTelephonySipImpl) pps.getOperationSet(OperationSetBasicTelephony.class);
                if (telephony != null)
                {
                    telephony.shutdown();
                }
            }
        }
        if (provider == null)
            throw new IOException(
                "The provider that requested "
                + "the SSL Socket could not be found");
        try
        {
            ArrayList<String> identities = new ArrayList<>(2);
            SipAccountID id = (SipAccountID) provider.getAccountID();
            // if the proxy is configured manually, the entered name is valid
            // for the X.509 certificate
            if(!id.getAccountPropertyBoolean(
                ProtocolProviderFactory.PROXY_AUTO_CONFIG, false))
            {
                String proxy = id.getAccountPropertyString(
                    ProtocolProviderFactory.PROXY_ADDRESS);
                String parsedProxy = parseOneHostnameFromProxy(proxy);

                if (parsedProxy != null)
                    identities.add(parsedProxy);

                logger.debug("Added <" + parsedProxy
                        + "> to list of valid SIP TLS server identities.");
            }
            // the domain part of the user id is always valid
            String userID =
                id.getAccountPropertyString(ProtocolProviderFactory.USER_ID);
            int index = userID.indexOf('@');
            if (index > -1)
            {
                identities.add(userID.substring(index + 1));
                logger.debug("Added <" + userID.substring(index + 1)
                        + "> to list of valid SIP TLS server identities.");
            }

            return certificateVerification.getSSLContext(
                (String)id.getAccountProperty(
                    ProtocolProviderFactory.CLIENT_TLS_CERTIFICATE),
                certificateVerification.getTrustManager(
                    identities,
                    null,
                    new RFC5922Matcher(provider)
                )).getSocketFactory();
        }
        catch (GeneralSecurityException e)
        {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Display a dialog telling the user to restart their client. Do this on the
     * EDT.
     */
    private void showRefreshNetworkConfigDialog()
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(this::showRefreshNetworkConfigDialog);
            return;
        }

        if (!sRefreshNetworkConfigDialogShowing)
        {
            sRefreshNetworkConfigDialogShowing = true;
            logger.info("Showing refresh network config dialog");
            ResourceManagementService res = SipActivator.getResources();
            String title = res.getI18NString("service.gui.ERROR");
            String message = res.getI18NString("service.gui.REFRESH_NETWORK_CONFIG_DIALOG_TEXT");
            // Show the error popup once and never again
            new ErrorDialog(title, message).showDialog();
        }
    }

    /**
     * The proxy string might contain multiple proxies with different ports.
     * This method just grabs the first one.
     *
     * @param proxy The proxy string to parse.
     * @return A hostname, or null if one couldn't be found.
     */
    private String parseOneHostnameFromProxy(String proxy)
    {
        String hostname = null;

        if (proxy != null && proxy.length() > 0)
        {
            // Remove all whitespace from the proxy string
            proxy = proxy.replaceAll("\\s+","");
            for (String aProxy : proxy.split(";"))
            {
                String address = aProxy.split(":")[0];

                if (address.matches("[\\d\\.]+"))
                {
                    logger.debug("Ignoring proxy as it appears to be an " +
                                 "IP address: " + address);
                    continue;
                }
                else
                {
                    // We've got a hostname. It will do.
                    logger.debug("Got hostname: " + address);
                    hostname = address;
                    break;
                }
            }
        }

        return hostname;
    }

    /**
     * Creates a stream SSL socket and connects it to the specified port number
     * at the specified IP address.
     *
     * @param address the address we are connecting to.
     * @param port the port we use.
     * @return the socket.
     * @throws IOException problem creating socket.
     */
    public SSLSocket createSSLSocket(InetAddress address, int port)
        throws IOException
    {
        SSLSocket sock = (SSLSocket) getSSLSocketFactory(address).createSocket(
            address, port);
        setTrafficClass(sock);
        return sock;
    }

    /**
     * Creates a stream SSL socket and connects it to the specified port number
     * at the specified IP address.
     *
     * @param address the address we are connecting to.
     * @param port the port we use.
     * @param myAddress the local address to use
     * @return the socket.
     * @throws IOException problem creating socket.
     */
    public SSLSocket createSSLSocket(InetAddress address, int port,
            InetAddress myAddress)
        throws IOException
    {
        if (isValidLocalAddress(myAddress))
        {
            SSLSocket sock = (SSLSocket) getSSLSocketFactory(address).createSocket(
                address, port, myAddress, 0);
            setTrafficClass(sock);
            return sock;
        }

        return createSSLSocket(address, port);
    }

    /**
     * Creates a stream socket and connects it to the specified port number at
     * the specified IP address. Comparable to
     * "new java.net.Socket(address, port,localaddress);"
     *
     * @param address the address to connect to.
     * @param port the port we use.
     * @param myAddress the local address to use.
     * @return the created socket.
     * @throws IOException problem creating socket.
     */
    public Socket createSocket(InetAddress address, int port,
            InetAddress myAddress)
        throws IOException
    {
        Socket sock = null;

        if (isValidLocalAddress(myAddress))
        {
            sock = new Socket(address, port, myAddress, 0);
        }
        else
            sock = new Socket(address, port);

        setTrafficClass(sock);

        return sock;
    }

    /**
     * Creates a new Socket, binds it to myAddress:myPort and connects it to
     * address:port.
     *
     * @param address the InetAddress that we'd like to connect to.
     * @param port the port that we'd like to connect to
     * @param myAddress the address that we are supposed to bind on or null for
     *            the "any" address.
     * @param myPort the port that we are supposed to bind on or 0 for a random
     *            one.
     *
     * @return a new Socket, bound on myAddress:myPort and connected to
     *         address:port.
     * @throws IOException if binding or connecting the socket fail for a reason
     *             (exception relayed from the corresponding Socket methods)
     */
    public Socket createSocket(InetAddress address, int port,
                    InetAddress myAddress, int myPort)
        throws IOException
    {
        Socket sock = null;

        if (isValidLocalAddress(myAddress))
        {
            sock = new Socket(address, port, myAddress, myPort);
        }
        else if (port != 0)
        {
            // myAddress is null (i.e. any) but we have a port number
            sock = new Socket();
            sock.bind(new InetSocketAddress(myPort));
            sock.connect(new InetSocketAddress(address, port));
        }
        else
        {
            sock = new Socket(address, port);
        }
        setTrafficClass(sock);
        return sock;
    }

    /**
     * Determine if we can create a socket on a given local address.
     * We can do this if:
     * - The given address is non-null
     * - It is not the "any" local address (0.0.0.0 on IPv4)
     * - It is not the dummy local address we use in signalling if we fail to
     *   get the local IP address
     * @param address the address to check if we can create a socket on
     * @return true if we can create a socket on this address
     */
    private boolean isValidLocalAddress(InetAddress address)
    {
        return ((address != null) && !address.isAnyLocalAddress() &&
                !NetworkAddressManagerServiceImpl.DUMMY_ADDRESS_STRING
                        .equals(address.getHostAddress()));
    }

    /**
     * Sets the traffic class for the <tt>Socket</tt>.
     *
     * @param s <tt>Socket</tt>
     */
    protected void setTrafficClass(Socket s)
    {
        int tc = getDSCP();

        try
        {
            s.setTrafficClass(tc);
        }
        catch (SocketException e)
        {
            logger.warn("Failed to set traffic class on Socket", e);
        }
    }

    /**
     * Sets the traffic class for the <tt>DatagramSocket</tt>.
     *
     * @param s <tt>DatagramSocket</tt>
     */
    protected void setTrafficClass(DatagramSocket s)
    {
        int tc = getDSCP();

        try
        {
            s.setTrafficClass(tc);
        }
        catch (SocketException e)
        {
            logger.warn("Failed to set traffic class on DatagramSocket", e);
        }
    }

    /**
     * Get the SIP traffic class from configuration.
     *
     * @return SIP traffic class or 0 if not configured
     */
    private int getDSCP()
    {
        ConfigurationService configService = SipActivator.getConfigurationService();

        String dscp = (String) configService.user().getProperty(SIP_DSCP_PROPERTY);

        if (dscp != null && !dscp.equals(""))
        {
            // The phone profile divides the configured value by 4 before sending to the client.
            // That's possibly related to what's a standard value to use for DSCP headers, but all
            // even values should be allowed, so just attempt to use whatever is configured.
            return (int) (Double.parseDouble(dscp) * 4);
        }

        return 0;
    }

    public void setSipStack(SipStackImpl arg0)
    {
    }
}
