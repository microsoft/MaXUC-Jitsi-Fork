/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.sip.net;

import static net.java.sip.communicator.service.protocol.ProtocolProviderFactory.*;

import java.net.*;
import java.util.*;

import net.java.sip.communicator.impl.protocol.sip.*;
import net.java.sip.communicator.service.dns.*;

/**
 * Abstract class for the determining the address for the SIP proxy.
 *
 * @author Ingo Bauersachs
 */
public abstract class ProxyConnection
{
    private List<String> returnedAddresses = new LinkedList<>();

    protected String transport;
    protected InetSocketAddress socketAddress;
    protected final SipAccountID account;

    /**
     * Creates a new instance of this class.
     * @param account the account of this SIP protocol instance
     */
    protected ProxyConnection(SipAccountID account)
    {
        this.account = account;
    }

    /**
     * Gets the address to use for the next connection attempt.
     * @return the address of the last lookup.
     */
    public final InetSocketAddress getAddress()
    {
        return socketAddress;
    }

    /**
     * Gets the transport to use for the next connection attempt.
     * @return the transport of the last lookup.
     */
    public final String getTransport()
    {
        return transport;
    }

    /**
     * In case we are using an outbound proxy this method returns
     * a suitable string for use with Router.
     * The method returns <tt>null</tt> otherwise.
     *
     * @return the string of our outbound proxy if we are using one and
     * <tt>null</tt> otherwise.
     */
    public final String getOutboundProxyString()
    {
        if(socketAddress == null)
            return null;

        InetAddress proxyAddress = socketAddress.getAddress();
        StringBuilder proxyStringBuffer
            = new StringBuilder(proxyAddress.getHostAddress());

        if(proxyAddress instanceof Inet6Address)
        {
            proxyStringBuffer.insert(0, '[');
            proxyStringBuffer.append(']');
        }

        proxyStringBuffer.append(':');
        proxyStringBuffer.append(socketAddress.getPort());
        proxyStringBuffer.append('/');
        proxyStringBuffer.append(transport);

        return proxyStringBuffer.toString();
    }

    /**
     * Compares an InetAddress against the active outbound proxy. The comparison
     * is by reference, not equals.
     *
     * @param addressToTest The address to test.
     * @return True when the InetAddress is the same as the outbound proxy.
     */
    public final boolean isSameInetAddress(InetAddress addressToTest)
    {
        // if the proxy is not yet initialized then this is not the provider
        // that caused this comparison
        if(socketAddress == null)
            return addressToTest == null;

        InetAddress existingAddr = socketAddress.getAddress();
        if (existingAddr == addressToTest)
            return true;

        // If either is null, then they are unequal as the test above found that
        // they aren't reference-equal.
        if (existingAddr == null || addressToTest == null)
            return false;

        return existingAddr.equals(addressToTest);
    }

    /**
     * Retrieves the next address to use from DNS. Duplicate results are
     * suppressed.
     *
     * @return True if a new address is available through {@link #getAddress()},
     *         false if the last address was reached. A new lookup from scratch
     *         can be started by calling {@link #reset()}.
     */
    public final boolean getNextAddress()
    {
        boolean result;
        String key = null;
        do
        {
            result = getNextAddressFromDns();
            if(result && socketAddress != null)
            {
                key = getOutboundProxyString();
                if(!returnedAddresses.contains(key))
                {
                    returnedAddresses.add(key);
                    break;
                }
            }
        }
        while(result && returnedAddresses.contains(key));
        return result;
    }

    /**
     * Implementations must use this method to get the next address, but do not
     * have to care about duplicate addresses.
     *
     * @return True when a further address was available.
     */
    protected abstract boolean getNextAddressFromDns();

    /**
     * Resets the lookup to its initial state. Overriders methods have to call
     * this method through a super-call.
     */
    public void reset()
    {
        returnedAddresses.clear();
    }

    /**
     * Factory method to create a proxy connection based on the account settings
     * of the protocol provider.
     *
     * @param pps the protocol provider that needs a SIP server connection.
     * @return An instance of a derived class.
     */
    public static ProxyConnection create(ProtocolProviderServiceSipImpl pps)
    {
        if (pps.getAccountID().getAccountPropertyBoolean(PROXY_AUTO_CONFIG,
            true))
            return new AutoProxyConnection((SipAccountID) pps.getAccountID(),
                pps.getDefaultTransport());
        else
            return new ManualProxyConnection((SipAccountID) pps.getAccountID());
    }
}
