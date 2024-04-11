/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.sip.net;

import static javax.sip.ListeningPoint.TCP;
import static javax.sip.ListeningPoint.TLS;
import static javax.sip.ListeningPoint.UDP;
import static net.java.sip.communicator.service.protocol.ProtocolProviderFactory.SERVER_ADDRESS;
import static net.java.sip.communicator.service.protocol.ProtocolProviderFactory.USER_ID;

import java.net.*;
import java.text.*;

import javax.sip.*;

import com.google.common.annotations.VisibleForTesting;

import net.java.sip.communicator.impl.protocol.sip.*;
import net.java.sip.communicator.util.*;

/**
 * Implementation of the autodetect proxy connection. Tries to resolve a SIP-
 * server by querying DNS in this order: NAPTR-SRV-A; SRV-A; A.
 *
 * @author Ingo Bauersachs
 */
public class AutoProxyConnection extends ProxyConnection
{
    private enum State
    {
        New,
        Naptr,
        NaptrSrv,
        NaptrSrvHosts,
        NaptrSrvHostIPs,
        Srv,
        SrvHosts,
        SrvHostIPs,
        Hosts,
        IP
    }

    private static final Logger logger = Logger.getLogger(AutoProxyConnection.class);

    @VisibleForTesting
    public State state;
    @VisibleForTesting
    public String address;
    private final String defaultTransport;

    private static final String[] transports = new String[]
    {
        ListeningPoint.TLS,
        ListeningPoint.TCP,
        ListeningPoint.UDP
    };
    private boolean hadSrvResults;
    private String[][] naptrRecords;
    private int naptrIndex;
    private SRVRecord[] srvRecords;
    private int srvRecordsIndex;
    private int srvTransportIndex;
    private InetSocketAddress socketAddresses[];
    private int socketAddressIndex;

    /**
     * Creates a new instance of this class. Uses the server from the account.
     *
     * @param account the account of this SIP protocol instance
     * @param defaultTransport the default transport to use when DNS does not
     *            provide a protocol through NAPTR or SRV
     */
    public AutoProxyConnection(SipAccountID account, String defaultTransport)
    {
        super(account);
        logger.debug("Creating new AutoProxyConnection for " +
                     account + ":" + defaultTransport);
        this.defaultTransport = defaultTransport;
        reset();
    }

    /**
     * Creates a new instance of this class. Uses the supplied address instead
     * of the server address from the account.
     *
     * @param account the account of this SIP protocol instance
     * @param address the domain on which to perform autodetection
     * @param defaultTransport the default transport to use when DNS does not
     *            provide a protocol through NAPTR or SRV
     */
    public AutoProxyConnection(SipAccountID account, String address,
        String defaultTransport)
    {
        super(account);
        logger.debug("Creating new AutoProxyConnection for " +
                     account + "/" + address + ":" + defaultTransport);
        this.defaultTransport = defaultTransport;
        reset();
        this.address = address;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.java.sip.communicator.impl.protocol.sip.net.ProxyConnection#
     * getNextAddressFromDns()
     */
    protected boolean getNextAddressFromDns()
    {
        logger.debug("Getting next address from DNS");

        try
        {
            return getNextAddressInternal();
        }
        catch(ParseException ex)
        {
            logger.error("Unable to get DNS data for <" + address
                + "> in state" + state, ex);
        }
        return false;
    }

    /**
     * Gets the next address from DNS.
     *
     * @throws ParseException When a domain name (possibly returned from DNS
     *             itself) is invalid.
     */
    private boolean getNextAddressInternal()
        throws ParseException
    {
        logger.info("Getting address - state = " + state);
        switch(state)
        {
            case New:
                state = State.Naptr;
                return getNextAddressFromDns();
            case IP:
                if(socketAddressIndex == 0)
                {
                    socketAddressIndex++;
                    try
                    {
                        socketAddress = new InetSocketAddress(
                            NetworkUtils.getInetAddress(address),
                            ListeningPoint.TLS.equalsIgnoreCase(transport)
                                ? ListeningPoint.PORT_5061
                                : ListeningPoint.PORT_5060
                        );
                    }
                    catch (UnknownHostException e)
                    {
                        //this is not supposed to happen
                        logger.error("invalid IP address: " + address, e);
                        return false;
                    }
                    transport = defaultTransport;
                    return true;
                }
                return false;
            case Naptr:
                naptrRecords = NetworkUtils.getNAPTRRecords(address);
                if(naptrRecords != null && naptrRecords.length > 0)
                {
                    state = State.NaptrSrv;
                    naptrIndex = 0;
                }
                else
                {
                    hadSrvResults = false;
                    state = State.Srv;
                    srvTransportIndex = 0;
                }

                return getNextAddressFromDns();
            case NaptrSrv:
                for(; naptrIndex < naptrRecords.length; naptrIndex++)
                {
                    srvRecords = NetworkUtils.getSRVRecords(
                        naptrRecords[naptrIndex][2]);
                    if(srvRecords != null && srvRecords.length > 0)
                    {
                        state = State.NaptrSrvHosts;
                        if(TLS.equalsIgnoreCase(naptrRecords[naptrIndex][1]))
                            transport = TLS;
                        else if(TCP.equalsIgnoreCase(naptrRecords[naptrIndex][1]))
                            transport = TCP;
                        else
                            transport = UDP;
                        srvRecordsIndex = 0;
                        if(getNextAddressFromDns())
                        {
                            naptrIndex++;
                            return true;
                        }
                    }
                }
                return false; //no more naptr's
            case NaptrSrvHosts:
                for(; srvRecordsIndex < srvRecords.length; srvRecordsIndex++)
                {
                    socketAddresses = NetworkUtils.getAandAAAARecords(
                        srvRecords[srvRecordsIndex].getTarget(),
                        srvRecords[srvRecordsIndex].getPort());
                    if(socketAddresses != null && socketAddresses.length > 0)
                    {
                        state = State.NaptrSrvHostIPs;
                        socketAddressIndex = 0;
                        if(getNextAddressFromDns())
                        {
                            srvRecordsIndex++;
                            return true;
                        }
                    }
                }
                state = State.NaptrSrv;
                return getNextAddressFromDns(); //backtrack to next naptr
            case NaptrSrvHostIPs:
                if(socketAddressIndex >= socketAddresses.length)
                {
                    state = State.NaptrSrvHosts;
                    return getNextAddressFromDns(); //backtrack to next srv
                }
                socketAddress = socketAddresses[socketAddressIndex];
                socketAddressIndex++;
                return true;
            case Srv:
                for(;srvTransportIndex < transports.length; srvTransportIndex++)
                {
                    srvRecords = NetworkUtils.getSRVRecords(
                        (TLS.equals(transports[srvTransportIndex])
                            ? "sips"
                            : "sip"),
                        (UDP.equalsIgnoreCase(transports[srvTransportIndex])
                            ? UDP
                            : TCP),
                        address);
                    if(srvRecords != null && srvRecords.length > 0)
                    {
                        hadSrvResults = true;
                        state = State.SrvHosts;
                        srvRecordsIndex = 0;
                        transport = transports[srvTransportIndex];
                        if(getNextAddressFromDns())
                        {
                            srvTransportIndex++;
                            return true;
                        }
                    }
                }
                if(!hadSrvResults)
                {
                    state = State.Hosts;
                    socketAddressIndex = 0;
                    return getNextAddressFromDns();
                }
                return false;
            case SrvHosts:
                if(srvRecordsIndex >= srvRecords.length)
                {
                    state = State.Srv;
                    return getNextAddressFromDns(); //backtrack to next srv record
                }
                for(; srvRecordsIndex < srvRecords.length; srvRecordsIndex++)
                {
                    socketAddresses = NetworkUtils.getAandAAAARecords(
                        srvRecords[srvRecordsIndex].getTarget(),
                        srvRecords[srvRecordsIndex].getPort());
                    if(socketAddresses != null && socketAddresses.length > 0)
                    {
                        state = State.SrvHostIPs;
                        socketAddressIndex = 0;
                        if(getNextAddressFromDns())
                        {
                            srvRecordsIndex++;
                            return true;
                        }
                    }
                }
                return false;
            case SrvHostIPs:
                if(socketAddressIndex >= socketAddresses.length)
                {
                    state = State.SrvHosts;
                    return getNextAddressFromDns();
                }
                socketAddress = socketAddresses[socketAddressIndex];
                socketAddressIndex++;
                return true;
            case Hosts:
                transport = defaultTransport;

                if(socketAddresses == null)
                {
                    socketAddresses = NetworkUtils.getAandAAAARecords(
                        address,
                        ListeningPoint.PORT_5060);
                }

                if(socketAddresses != null && socketAddresses.length > 0
                    && socketAddressIndex < socketAddresses.length)
                {
                    socketAddress = socketAddresses[socketAddressIndex++];
                    return true;
                }
                return false;
        }
        return false;
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
        state = State.New;

        //determine the hostname of the proxy for autodetection:
        //1) server part of the user ID
        //2) name of the registrar when the user ID contains no domain
        String userID =  account.getAccountPropertyString(USER_ID);
        int domainIx = userID.indexOf("@");
        if(domainIx > 0)
        {
            address = userID.substring(domainIx + 1);
        }
        else
        {
            address = account.getAccountPropertyString(SERVER_ADDRESS);
            if(address == null || address.trim().length() == 0)
            {
                //registrarless account
                return;
            }
        }
        if(NetworkUtils.isValidIPAddress(address))
        {
            state = State.IP;
            socketAddressIndex = 0;
        }
    }
}
