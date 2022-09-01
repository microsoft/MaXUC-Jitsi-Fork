/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.text.ParseException;
import java.util.ArrayList;
import javax.sip.ListeningPoint;
import javax.sip.address.Hop;

import gov.nist.core.net.AddressResolver;
import gov.nist.javax.sip.stack.HopImpl;
import gov.nist.javax.sip.stack.MessageProcessor;

import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.NetworkUtils;
import net.java.sip.communicator.util.SRVRecord;

/**
 * Lookup for SRV records for given host. If nothing found
 * the original host is returned this way when a Socket
 * is constructed another dns lookup will be made for the A record.
 *
 * @author Damian Minkov
 * @author Alan Kelly
 * @author Emil Ivov
 */
public class AddressResolverImpl
    implements AddressResolver
{
    /**
     * Our class logger
     */
    private static final Logger logger
        = Logger.getLogger(AddressResolverImpl.class);

    /**
     * Implements the actual resolving. This is where we do the DNS queries.
     *
     * @param inputAddress the unresolved <tt>Hop</tt> that we'd need to find
     * an address for.
     *
     * @return the newly created <tt>Hop</tt> containing the resolved
     * destination.
     */
    public Hop resolveAddress(Hop inputAddress)
    {
        Hop returnHop = null;

        // To avoid spamming the log files, we only write each log from this
        // method if at least 60 seconds has passed since the previous log was
        // written, unless the details of the log have changed.
        int logIntervalSecs = 60;

        try
        {
            String transport = inputAddress.getTransport();
            String hostAddress = inputAddress.getHost();

            if (transport == null)
            {
                transport = ListeningPoint.UDP;
            }

            String host = null;
            int port = 0;

            if (NetworkUtils.isValidIPAddress(hostAddress))
            {
                // The hostAddress provided already appears to be an IP address
                // so we shouldn't need to do a DNS lookup to resolve it.
                byte[] addr = NetworkUtils.strToIPv4(hostAddress);

                // not an IPv4, try IPv6
                if (addr == null)
                {
                    addr = NetworkUtils.strToIPv6(hostAddress);
                }

                if (addr != null)
                {
                    InetSocketAddress hostSocketAddress = new InetSocketAddress(
                            InetAddress.getByAddress(hostAddress, addr),
                            inputAddress.getPort());
                    returnHop = new HopImpl(hostSocketAddress.getHostName(),
                                            inputAddress.getPort(),
                                            transport);
                    logger.interval(logIntervalSecs,
                                    "IP Provided:",
                                    "IP address provided so no need for DNS lookup.",
                                    "ReturnHop: " + returnHop);
                }
                else
                {
                    logger.warn("Unable to convert " + hostAddress +
                                " to a valid IPv4/6 address. Trying DNS lookup.");
                }
            }

            if (returnHop == null)
            {
                // The hostAddress provided isn't an IP address, so try a DNS
                // lookup.  It is important to try to get an A/AAAA record first,
                // as SRV records are not always supported by SIP registrars.
                logger.interval(logIntervalSecs,
                                "Trying A/AAAA:",
                                "Doing A/AAAA lookup.",
                                "hostAddress: " + hostAddress);

                InetSocketAddress[] addrs = null;
                port = inputAddress.getPort();

                try
                {
                    addrs = NetworkUtils.getAandAAAARecords(hostAddress, port);
                }
                catch (ParseException e)
                {
                    logger.error("Failed to parse domain for: " +
                                 hostAddress, e);
                }

                int addrCount = (addrs != null) ? addrs.length : 0;
                if (addrCount != 0)
                {
                    if (addrCount > 1)
                    {
                        String tag = "Multiple A/AAAA:";
                        String message = "Found " + addrCount + " A/AAAA records for " +
                                         hostAddress + ". Using the first one.";
                        ArrayList <String> params = new ArrayList<>();
                        for (InetSocketAddress addr : addrs)
                        {
                            params.add("\n< hostName: " + addr.getHostName() +
                                       ", hostString: " + addr.getHostString() +
                                       ", address: " + addr.getAddress() +
                                       ", port: " + addr.getPort() + " >");
                        }

                        logger.interval(logIntervalSecs, tag, message, params.toArray());
                    }

                    // A/AAAA records are returned in order of importance,
                    // so use the first one.
                    InetSocketAddress record = addrs[0];
                    host = record.getAddress().getHostAddress();
                    port = record.getPort();
                }
                else
                {
                    // We failed to find an A/AAAA record, so try SRV instead.
                    logger.interval(logIntervalSecs,
                                    "No A/AAAA:",
                                    "No A/AAAA addresses found. Trying SRV lookup.",
                                    "hostAddress: " + hostAddress);

                    if (transport.equalsIgnoreCase(ListeningPoint.TLS))
                    {
                        SRVRecord srvRecord = NetworkUtils.getSRVRecord(
                                "sips", ListeningPoint.TCP, hostAddress);
                        if (srvRecord != null)
                        {
                            host = srvRecord.getTarget();
                            port = srvRecord.getPort();
                        }
                    }
                    else
                    {
                        SRVRecord srvRecord = NetworkUtils.getSRVRecord(
                                "sip", transport, hostAddress);
                        if (srvRecord != null)
                        {
                            host = srvRecord.getTarget();
                            port = srvRecord.getPort();
                        }
                    }
                }
            }

            if (host != null)
            {
                logger.interval(logIntervalSecs,
                                "DNS Resolved:",
                                "Returning hop as follows.",
                                " host = " + host + " port = " +
                                port + " transport = " + transport);

                returnHop = new HopImpl(host, port, transport);
            }
        }
        catch (Exception ex)
        {
            // It would be preferable not to catch a general Exception, but this
            // is original Jitsi code so we do not know the history of it.  This
            // means that trying to figure out what this was originally trying
            // to handle and replace it with more specific Exception handling
            // could be dangerous and not worth the possible destabilisation.

            //could mean there was no SRV record
            logger.debug("Domain "+ inputAddress
                                +" could not be resolved " + ex.getMessage());
            //show who called us
            logger.trace("Printing SRV resolution stack trace", ex);
        }

        if (returnHop == null)
        {
            if (inputAddress.getPort() != -1)
            {
                returnHop = inputAddress;
                logger.interval(logIntervalSecs,
                                "inputAddress:",
                                "Setting returnHop as inputAddress.",
                                "inputAddress: " + inputAddress);
            }
            else
            {
                String transport = inputAddress.getTransport();

                returnHop
                        = new HopImpl(
                        inputAddress.getHost(),
                        MessageProcessor.getDefaultPort(transport),
                        transport);

                logger.interval(logIntervalSecs,
                                "inputAddress:",
                                "Creating returnHop from " +
                                "inputAddress using its transport's default port ",
                                "inputAddress: " + inputAddress);
            }
        }

        return returnHop;
    }
}
