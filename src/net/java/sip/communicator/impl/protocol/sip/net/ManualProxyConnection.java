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

    private int defaultPort;

    private String[] proxyAddresses;
    private int proxyIndex;
    private InetSocketAddress[] currentLookups;
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
                     account.getLoggableAccountID());
        reset();
    }

    /*
     * Returns the next address to try connecting to.
     * Iterates over all configured proxies. Before trying each proxy,
     * its domain name is resolved using DNS query.
     *
     * @see net.java.sip.communicator.impl.protocol.sip.net.ProxyConnection#
     * getNextAddress()
     */
    @Override
    protected boolean getNextAddressFromDns()
    {
        logger.debug("Getting next address from DNS - lookups: " + Arrays.toString(currentLookups));

        // find next possible address
        if (!findNextHostOrProxy())
        {
            logger.debug("No more addresses for " + account.getLoggableAccountID());
            return false;
        }

        //assign the next address and return lookup success
        socketAddress = currentLookups[lookupIndex];
        logger.debug("Returning <" + socketAddress + "> as next address for " +
                     account.getLoggableAccountID());
        return true;
    }

    private boolean findNextHostOrProxy()
    {
        // iterate over all proxies till the end of the list
        for (; proxyIndex < proxyAddresses.length; proxyIndex++)
        {
            // lazy DNS resolving, if lookup is null, fill it with resolved addresses.
            if (currentLookups == null)
            {
                if (!initializeAddressesForCurrentProxy())
                {
                    // nothing resolved, try the next proxy
                    currentLookups = null;
                    continue;
                }

                // start trying resolved addresses from the beginning.
                lookupIndex = 0;
            }
            else
            {
                // try next address in current array of resolved addresses
                lookupIndex++;
            }

            // check if the available lookups are exhausted
            if (lookupIndex < currentLookups.length)
            {
                // address to try is found, exit
                return true;
            }

            // no more addresses to try - move to the next proxy
            currentLookups = null;
        }

        // next time start over from the first proxy
        proxyIndex = 0;

        // all proxies tried, no address to try
        return false;
    }

    /**
     * Convert the current proxy address into an array of the resolved addresses.
     *
     * @return true if the array of address is not empty
     */
    private boolean initializeAddressesForCurrentProxy()
    {
        final String proxyAddress = proxyAddresses[proxyIndex];
        logger.debug("Initializing addresses for " + proxyAddress);

        final List<InetSocketAddress> addresses = new LinkedList<>();

        try
        {
            // split(":")[0] will always return just the address whether there is a port or not.
            final String domain = proxyAddress.split(":")[0];
            final int port = extractPort(proxyAddress, this.defaultPort);
            logger.debug("Doing A/AAAA lookup for " + domain + ":" + port);

            final InetSocketAddress[] records = NetworkUtils.getAandAAAARecords(domain, port);
            if (records != null)
            {
                addresses.addAll(Arrays.asList(records));
            }
        }
        catch (ParseException e)
        {
            logger.error("Invalid address <" + proxyAddress + ">", e);
        }

        currentLookups = addresses.toArray(new InetSocketAddress[0]);
        logger.debug("Found results: " + addresses);

        // There were no valid proxy addresses.
        return currentLookups.length != 0;
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
            return Integer.parseInt(parsed[1]);
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
        String address = account.getAccountPropertyString(PROXY_ADDRESS).replaceAll("\\s+", "");
        // Config can contain a semi-colon separated list of addresses.
        proxyAddresses = address.split(";");
        proxyIndex = 0;
        currentLookups = null;

        defaultPort = account.getAccountPropertyInt(PROXY_PORT, PORT_5060);
        transport = account.getAccountPropertyString(PREFERRED_TRANSPORT);

        // check property sanity
        if (!ProtocolProviderServiceSipImpl.isValidTransport(transport))
            throw new IllegalArgumentException(transport + " is not a valid SIP transport");
    }
}
