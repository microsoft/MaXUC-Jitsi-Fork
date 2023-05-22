// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.dns;

import static org.jitsi.util.Hasher.*;

import java.io.IOException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.xbill.DNS.DClass;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.ResolverConfig;

import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeListener;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.NetworkUtils;
import net.java.sip.communicator.util.account.AccountUtils;

/**
 * Implementation of the {@link Resolver} interface, extending
 * {@link ParallelResolverImpl}.  We have found that the default DNS servers
 * provided by ResolverConfig tend to contain entries that don't necessarily
 * work.  Instead we should preference the OS provided servers as we already
 * know they are working for general lookups.
 */
public class NativeResolver
    extends ParallelResolverImpl
    implements RegistrationStateChangeListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>NativeResolver</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger = Logger
                    .getLogger(NativeResolver.class.getName());

    /**
     * The list of DNS servers we use.
     */
    private String[] dnsServers;

    /**
     * A two-second timeout for our resolvers.
     */
    private static final Duration TIMEOUT = Duration.ofSeconds(2L);

    /**
     * Sends a message and waits for a response.
     *
     * @param query The query to send.
     * @return The response
     *
     * @throws IOException An error occurred while sending or receiving.
     * @throws IllegalStateException We received a nonsensical response.
     */
    public Message send(Message query) throws IOException
    {
        long lookupStartTime = System.currentTimeMillis();
        Message response = null;

        // Our default resolver now has the correct servers so just pass on the
        // request.
        try
        {
            response = defaultResolver.send(query);

            logger.debug("NativeResolver DNS lookup took " +
                         (System.currentTimeMillis() - lookupStartTime) + "ms");
        }
        catch (IOException ex)
        {
            // The primary lookup could fail via exception for various reasons
            // (e.g. a timeout) that aren't necessarily fatal for our backup
            // resolver so just catch them and continue.
            logger.warn("Hit exception on send to primary resolver", ex);
        }

        // We have seen some DNS servers don't like our standard message and so
        // respond with a server error.  We can check for this and tweak the
        // flags in the hope of getting a better response.
        if ((response != null) &&
            (response.getRcode() == Rcode.SERVFAIL))
        {
            logger.info("Resending DNS query due to server failure");
            response = resendOnServerFailure(query, response);
        }

        // To match previous (ParallelResolverImpl) behaviour we now have to
        // try a fallback server if we don't yet have a valid response.
        if (!isResponseSatisfactory(response))
        {
            logger.warn("Failed to get valid DNS lookup from primary, " +
                        "falling back");
            response = sendToFallback(query);
        }

        if (response != null)
        {
            return response;
        }
        else
        {
            // The underlying resolver code doesn't actually return null so
            // this is a bit arbitrary.  However just in case it changes in
            // the future, let's throw an IOException to prevent bringing
            // everything down just because of DNS issues (we may still have
            // some level of service).
            throw new IOException("No response from resolver");
        }
    }

    /**
     * Tweaks a query to make it more likely to be accepted by a DNS server
     * and resends it.
     *
     * @param query The query to send.
     * @param response The original response
     * @return The new response
     *
     * @throws IOException An error occurred while sending or receiving.
     */
    private Message resendOnServerFailure(Message query, Message response) throws IOException
    {
        // There are two flags on our query which could cause offence -
        // "Recursion desired" and "Non-authenticated data".  If the response
        // doesn't match either of those flags on our initial request, tweak
        // and resend.
        if ((query.getHeader().getFlag(Flags.RD) != response.getHeader().getFlag(Flags.RD)) ||
            (query.getHeader().getFlag(Flags.AD) != response.getHeader().getFlag(Flags.AD)))
        {
            if (response.getHeader().getFlag(Flags.RD))
            {
                query.getHeader().setFlag(Flags.RD);
            }
            else
            {
                query.getHeader().unsetFlag(Flags.RD);
            }

            if (response.getHeader().getFlag(Flags.AD))
            {
                query.getHeader().setFlag(Flags.AD);
            }
            else
            {
                query.getHeader().unsetFlag(Flags.AD);
            }

            logger.debug("Resent tweaked DNS query");
            response = defaultResolver.send(query);
        }

        return response;
    }

    /**
     * Sends a query to our fallback server.
     *
     * @param query The query to send.
     * @return The response
     *
     * @throws IOException An error occurred while sending or receiving.
     */
    private Message sendToFallback(Message query) throws IOException
    {
        return backupResolver.send(query);
    }

    /**
     * Resets resolver configuration and populates our default resolver with
     * the OS configured servers.
     */
    @Override
    public final void reset()
    {
        // Reset the default config.
        Lookup.refreshDefault();

        try
        {
            // Lookup the DNS server list we should use to create an
            // ExtendedResolver.
            // We refresh DNSJava's ResolverConfig in order to prevent issues arising from cached DNS server lists.
            // E.g. SFR541571 and SFR537948.
            DNSJavaUtils.refreshResolverConfig();
            dnsServers = lookupDnsServers();

            ExtendedResolver temp;

            if (dnsServers.length > 0)
            {
                logger.info("Have DNS servers " + logCollectionHasher(List.of(dnsServers)));
                temp = new ExtendedResolver(dnsServers);
            }
            else
            {
                // If we failed to find any servers fall back to the standard
                // ResolverConfig (default).
                logger.warn("Failed to find any DNS servers, using default");
                temp = new ExtendedResolver();

                ResolverConfig resolverConfig =
                                             ResolverConfig.getCurrentConfig();

                if ((resolverConfig != null) &&
                    (resolverConfig.servers() != null))
                {
                    dnsServers =
                        NetworkUtils.convertServerListToStrings(resolverConfig.servers());
                }
            }

            // Setup the resolver config. We're much better off fast-failing -
            // any problems will be picked up by the higher layers failing to
            // login and they'll retry.  Note that by default ExtendedResolver
            // tries each of the servers one by one so we don't want to sit
            // around if one of them isn't contactable.  We could potentially
            // get around this by using sendAsync (untested) but that would
            // make the code more complicated.
            temp.setTimeout(TIMEOUT);
            temp.setRetries(1);
            temp.setLoadBalance(false);
            defaultResolver = temp;

            // Also, configure the backup resolver with sensible timeout
            // behaviour.  We do this here, in reset(), even though we only
            // need to do it once at start of day - because reset() is already
            // overridden by this class, where as the initProperties() method
            // isn't - and we want to avoid changing the startup behaviour
            // inherited from the parent class ParallelResolverImpl.
            backupResolver.setTimeout(TIMEOUT);
            backupResolver.setRetries(1);

            // Setup the default Lookup behaviour to use us and to use sensible
            // caching behaviour (i.e. not forever as default).
            Lookup.setDefaultResolver(this);
            Lookup.getDefaultCache(DClass.IN).clearCache();
            Lookup.getDefaultCache(DClass.IN).setMaxCache(86400);
            Lookup.getDefaultCache(DClass.IN).setMaxNCache(300);
        }
        catch (UnknownHostException e)
        {
            // Should never happen.
            throw new RuntimeException("Failed to initialize resolver");
        }
    }

    /**
     * Add a listener to any protocol providers to spot when they fail
     * to connect.
     */
    @Override
    public final void accountLoaded()
    {
        for (ProtocolProviderService provider : AccountUtils.getRegisteredProviders())
        {
            provider.addRegistrationStateChangeListener(this);
        }
    }

    /**
     * Lookup the DNS servers we should use.
     *
     * @return A list of DNS servers (empty list if there are none).
     */
    private String[] lookupDnsServers()
    {
        // Get the current list of servers.
        String[] servers = NetworkUtils.convertServerListToStrings(ResolverConfig.getCurrentConfig().servers());

        // We don't currently support DNS over IPv6 (SFR 537948)
        // so do not add IPv6 addresses to the list.
        // If we add IPv6 support, this will need to change.
        servers = extractIpv4Addresses(servers);

        logger.debug("Returning DNS servers from Java: " + logCollectionHasher(List.of(servers)));

        // Return the list of servers, or an empty array if we failed to
        // retrieve any.
        if (servers.length > 0)
        {
            return servers;
        }
        else
        {
            return new String[0];
        }
    }

    /**
     * Returns a list of all valid IPv4 addresses from the given list.
     */
    private String[] extractIpv4Addresses(String[] ipAddresses)
    {
        List<String> ipv4Addresses = new ArrayList<>();

        for (String ipAddress : ipAddresses)
        {
            if (NetworkUtils.isIPv4Address(ipAddress))
            {
                if (!ipv4Addresses.contains(ipAddress))
                {
                    ipv4Addresses.add(ipAddress);
                }
            }
            else
            {
                logger.debug("Ignoring non-IPv4 address " + logHasher(ipAddress));
            }
        }

        logger.debug("Returning IPv4 addresses: " + logCollectionHasher(ipv4Addresses));
        return ipv4Addresses.toArray(new String[0]);
    }

    /**
     * Return the array of servers used for DNS lookups.
     *
     * @return the server list.
     *
     */
    @Override
    public String[] getDnsServers()
    {
        return dnsServers;
    }

    /**
     * Handle notification of registration state change.  If the connection
     * fails, it may be that the DNS result we returned wasn't valid (e.g.
     * the fallback resolution wasn't suitable).  In that case we want to
     * ensure we clear the cache so we try again next time we are asked.  We
     * also use unregistering as a trigger in case we're unregistering due to
     * a network change.
     *
     * @param evt The event
     *
     */
    @Override
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        if (evt.getNewState().equals(RegistrationState.CONNECTION_FAILED) ||
            evt.getNewState().equals(RegistrationState.UNREGISTERING))
        {
            logger.debug("Clearing DNS cache due to connection failure");
            Lookup.getDefaultCache(DClass.IN).clearCache();
        }
    }
}
