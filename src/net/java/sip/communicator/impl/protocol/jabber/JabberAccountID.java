/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.Map;

import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.ProtocolNames;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;

/**
 * The Jabber implementation of a sip-communicator AccountID
 *
 * @author Damian Minkov
 * @author Sebastien Vincent
 */
public class JabberAccountID
    extends AccountID
{
    /**
     * Creates an account id from the specified id and account properties.
     * @param id the id identifying this account
     * @param accountProperties any other properties necessary for the account.
     */
    JabberAccountID(String id, Map<String, String> accountProperties )
    {
        super(  id, accountProperties,
                ProtocolNames.JABBER,
                getServiceName(accountProperties));
    }

    /**
     * Returns the service name - the server we are logging to
     * if it is null which is not supposed to be - we return for compatibility
     * the string we used in the first release for creating AccountID
     * (Using this string is wrong, but used for compatibility for now)
     * @param accountProperties Map
     * @return String
     */
    private static String getServiceName(Map<String, String> accountProperties)
    {
        return accountProperties.get(ProtocolProviderFactory.SERVER_ADDRESS);
    }

    /**
     * Determines whether this account's provider is supposed to auto discover
     * STUN and TURN servers.
     *
     * @return <tt>true</tt> if this provider would need to discover STUN/TURN
     * servers and false otherwise.
     */
    public boolean isStunServerDiscoveryEnabled()
    {
        return
            getAccountPropertyBoolean(
                    ProtocolProviderFactory.AUTO_DISCOVER_STUN,
                    true);
    }

    /**
     * Determines whether this account's provider uses the default STUN server
     * provided by SIP Communicator if there is no other STUN/TURN server
     * discovered/configured.
     *
     * @return <tt>true</tt> if this provider would use the default STUN server,
     * <tt>false</tt> otherwise
     */
    public boolean isUseDefaultStunServer()
    {
        return
            getAccountPropertyBoolean(
                    ProtocolProviderFactory.USE_DEFAULT_STUN_SERVER,
                    true);
    }

    /**
     * Determines whether this account's provider uses UPnP (if available).
     *
     * @return <tt>true</tt> if this provider would use UPnP (if available),
     * <tt>false</tt> otherwise
     */
    public boolean isUPNPEnabled()
    {
        return getAccountPropertyBoolean(
                ProtocolProviderFactory.IS_USE_UPNP,
                true);
    }

    /**
     * Determines whether this account's provider allow non-secure connection
     *
     * @return <tt>true</tt> if this provider would allow non-secure connection,
     * <tt>false</tt> otherwise
     */
    public boolean allowNonSecureConnection()
    {
        return getAccountPropertyBoolean(
                ProtocolProviderFactory.IS_ALLOW_NON_SECURE,
                false);
    }
}
