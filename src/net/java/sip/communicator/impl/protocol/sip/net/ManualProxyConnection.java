/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.sip.net;

import static javax.sip.ListeningPoint.*;
import static net.java.sip.communicator.service.protocol.ProtocolProviderFactory.*;

import java.net.*;
import java.text.*;
import java.util.*;

import net.java.sip.communicator.impl.protocol.sip.*;
import net.java.sip.communicator.util.*;

/**
 * Implementation of the manually configured SIP proxy connection. IP Address
 * lookups are performed using the account's proxy address.
 *
 * @author Ingo Bauersachs
 */
public class ManualProxyConnection
    extends ProxyConnection
{
    private static final Logger logger
        = Logger.getLogger(ManualProxyConnection.class);

    private String address;
    private int port;

    private InetSocketAddress[] lookups;
    private int lookupIndex;

    /**
     * Creates a new instance of this class. Uses the server from the account.
     *
     * @param account the account of this SIP protocol instance
     */
    public ManualProxyConnection(SipAccountID account)
    {
        super(account);
        logger.debug("Creating new ManualProxyConnection for " +
                    account != null ? account.getLoggableAccountID() : account);
        reset();
    }

    /*
     * (non-Javadoc)
     *
     * @see net.java.sip.communicator.impl.protocol.sip.net.ProxyConnection#
     * getNextAddress()
     */
    @Override
    protected boolean getNextAddressFromDns()
    {
        logger.debug("Getting next address from DNS - lookups: " + Arrays.toString(lookups));
        if(lookups == null)
        {
            if (!initializeAddresses())
            {
                return false;
            }
        }

        //check if the available addresses are exhausted
        if(lookupIndex >= lookups.length)
        {
            logger.debug("No more addresses for " + account.getLoggableAccountID());
            lookups = null;
            return false;
        }

        //assign the next address and return lookup success
        socketAddress = lookups[lookupIndex];
        logger.debug("Returning <" + socketAddress + "> as next address for " +
                     account.getLoggableAccountID());
        lookupIndex++;
        return true;
    }

    /**
     * Convert the config (which might contain multiple hostnames) into a array
     * of concrete addresses.
     *
     * @return an array of InetSocketAddress
     */
    private boolean initializeAddresses()
    {
        logger.debug("Initializing addresses for " + address);

        lookupIndex = 0;
        List<InetSocketAddress> addresses = new LinkedList<>();

        // Config can contain a semi-colon separated list of addresses. Iterate
        // through them all.
        for (String anAddress : address.split(";"))
        {
            try
            {
                // split(":")[0] will always return just the address whether
                // there is a port or not.
                final String domain = anAddress.split(":")[0];
                final int port = extractPort(anAddress, this.port);
                logger.debug("Doing A/AAAA lookup for " + domain + ":" + port);
                addresses.addAll(Arrays.asList(
                        NetworkUtils.getAandAAAARecords(domain, port)));
            }
            catch (ParseException e)
            {
                logger.error("Invalid address <" + anAddress + ">", e);
            }
        }

        lookups = addresses.toArray(new InetSocketAddress[0]);
        logger.debug("Found results: " + addresses);

        // There were no valid proxy addresses. Return false to indicate "out
        // of addresses" and reset state.
        if(lookups.length == 0)
        {
            lookups = null;
            return false;
        }

        return true;
    }

    /**
     * Extract a port from an address.
     *
     * @param address The address to parse
     * @param defaultPort A default port to use
     * @return The port extracted from address or defaultPort if one couldn't
     * be found.
     */
    private int extractPort(String address, int defaultPort)
    {
        String[] parsed = address.split(":");

        if (parsed.length == 2 && parsed[1].matches("\\d+"))
        {
            return Integer.valueOf(parsed[1]);
        }
        else
        {
            return defaultPort;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.java.sip.communicator.impl.protocol.sip.net.ProxyConnection#reset()
     */
    @Override
    public void reset()
    {
        super.reset();
        // Strip all whitespace from the address list
        address = account.getAccountPropertyString(PROXY_ADDRESS).replaceAll("\\s+","");
        port = account.getAccountPropertyInt(PROXY_PORT, PORT_5060);
        transport = account.getAccountPropertyString(PREFERRED_TRANSPORT);

        //check property sanity
        if(!ProtocolProviderServiceSipImpl.isValidTransport(transport))
            throw new IllegalArgumentException(
                transport + " is not a valid SIP transport");
    }
}
