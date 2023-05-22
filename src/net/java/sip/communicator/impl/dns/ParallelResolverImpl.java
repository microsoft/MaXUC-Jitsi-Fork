/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.dns;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xbill.DNS.EDNSOption;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Message;
import org.xbill.DNS.OPTRecord;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.ResolverConfig;
import org.xbill.DNS.ResolverListener;
import org.xbill.DNS.Section;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TSIG;
import org.xbill.DNS.Type;

import net.java.sip.communicator.service.dns.CustomResolver;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.NetworkUtils;

/**
 * The purpose of this class is to help avoid the significant delays that occur
 * in networks where DNS servers would ignore SRV, NAPTR, and sometimes even
 * A/AAAA queries (i.e. without even sending an error response). We also try to
 * handle cases where DNS servers may return empty responses to some records.
 * <p>
 * We achieve this by entering a redundant mode whenever we detect an abnormal
 * delay (longer than <tt>DNS_PATIENCE</tt>)  while waiting for a DNS resonse,
 * or when that response is not considered satisfying.
 * <p>
 * Once we enter redundant mode, we start duplicating all queries and sending
 * them to both our primary and backup resolvers (in case we have any). We then
 * always return the first response we get, regardless of who sent it.
 * <p>
 * We exit redundant mode after receiving <tt>DNS_REDEMPTION</tt> consecutive
 * timely and correct responses from our primary resolver.
 *
 * @author Emil Ivov
 */
public class ParallelResolverImpl
    implements CustomResolver, PropertyChangeListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>ParallelResolver</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger = Logger
                    .getLogger(ParallelResolverImpl.class.getName());

    /**
     * Indicates whether we are currently in a mode where all DNS queries are
     * sent to both the primary and the backup DNS servers.
     */
    private static boolean redundantMode = false;

    /**
     * The currently configured number of milliseconds that we need to wait
     * before entering redundant mode.
     */
    private static long currentDnsPatience = DNS_PATIENCE;

    /**
     * The currently configured number of milliseconds that we will wait on
     * every request in redundant mode for the primary DNS server(s) to
     * respond.
     */
    private static long currentDnsPrimWait = DNS_PRIM_WAIT;

    /**
     * The currently configured number of times that the primary DNS would have
     * to provide a faster response than the backup resolver before we consider
     * it safe enough to exit redundant mode.
     */
    public static int currentDnsRedemption = DNS_REDEMPTION;

    /**
     * The number of fast responses that we need to get from the primary
     * resolver before we exit redundant mode. <tt>0</tt> indicates that we are
     * no longer in redundant mode
     */
    private static int redemptionStatus = 0;

    /**
     * A lock that we use while determining whether we've completed redemption
     * and can exit redundant mode.
     */
    private static final Object redemptionLock = new Object();

    /**
     * The default resolver that we use if everything works properly.
     */
    protected Resolver defaultResolver;

    /**
     * An extended resolver that would be encapsulating all backup resolvers.
     */
    protected ExtendedResolver backupResolver;

    /**
     * Creates a new instance of this class.
     */
    ParallelResolverImpl()
    {
        DnsUtilActivator.getConfigurationService().global()
            .addPropertyChangeListener(this);
        initProperties();
        reset();
    }

    protected void initProperties()
    {
        String rslvrAddrStr
            = DnsUtilActivator.getConfigurationService().global().getString(
                DnsUtilActivator.PNAME_BACKUP_RESOLVER,
                DnsUtilActivator.DEFAULT_BACKUP_RESOLVER);
        String customResolverIP
            = DnsUtilActivator.getConfigurationService().global().getString(
                DnsUtilActivator.PNAME_BACKUP_RESOLVER_FALLBACK_IP,
                DnsUtilActivator.getResources().getSettingsString(
                    DnsUtilActivator.PNAME_BACKUP_RESOLVER_FALLBACK_IP));

        InetAddress resolverAddress = null;
        try
        {
            resolverAddress = NetworkUtils.getInetAddress(rslvrAddrStr);
        }
        catch(UnknownHostException exc)
        {
            logger.warn("Oh! Seems like our primary DNS is down! "
                        + "Don't panic! We'll try to fall back to "
                        + customResolverIP, exc);
        }

        if(resolverAddress == null)
        {
            // name resolution failed for backup DNS resolver,
            // try with the IP address of the default backup resolver
            try
            {
                resolverAddress = NetworkUtils.getInetAddress(customResolverIP);
            }
            catch (UnknownHostException e)
            {
                // this shouldn't happen, but log anyway
                logger.error(e);
            }
        }

        int resolverPort = DnsUtilActivator.getConfigurationService().global().getInt(
            DnsUtilActivator.PNAME_BACKUP_RESOLVER_PORT,
            SimpleResolver.DEFAULT_PORT);

        InetSocketAddress resolverSockAddr
            = new InetSocketAddress(resolverAddress, resolverPort);

        setBackupServers(new InetSocketAddress[]{ resolverSockAddr });

        currentDnsPatience = DnsUtilActivator.getConfigurationService()
            .global().getLong(PNAME_DNS_PATIENCE, DNS_PATIENCE);

        currentDnsPrimWait = DnsUtilActivator.getConfigurationService()
            .global().getLong(PNAME_DNS_PRIM_WAIT, DNS_PRIM_WAIT);

        currentDnsRedemption
            = DnsUtilActivator.getConfigurationService().global()
                .getInt(PNAME_DNS_REDEMPTION, DNS_REDEMPTION);
    }

    /**
     * Sets the specified array of <tt>backupServers</tt> used if the default
     * DNS doesn't seem to be doing that well.
     *
     * @param backupServers the list of backup DNS servers that we should use
     * if, and only if, the default servers don't seem to work that well.
     */
    private void setBackupServers(InetSocketAddress[] backupServers)
    {
        try
        {
            backupResolver = new ExtendedResolver(new SimpleResolver[]{});
            for(InetSocketAddress backupServer : backupServers )
            {
                SimpleResolver sr = new SimpleResolver();
                sr.setAddress(backupServer);
                backupResolver.addResolver(sr);
            }
        }
        catch (UnknownHostException e)
        {
            //this shouldn't be thrown since we don't do any DNS querying
            //in here. this is why we take an InetSocketAddress as a param.
            throw new IllegalStateException("The impossible just happened: "
                        +"we could not initialize our backup DNS resolver", e);
        }
    }

    /**
     * Sends a message and waits for a response.
     *
     * @param query The query to send.
     * @return The response
     *
     * @throws IOException An error occurred while sending or receiving.
     */
    @Override
    public Message send(Message query)
        throws IOException
    {
        ParallelResolution resolution = new ParallelResolution(query);

        long primaryStartTime = System.currentTimeMillis();
        resolution.sendFirstQuery();

        //make a copy of the redundant mode variable in case we are currently
        //completed a redemption that started earlier.
        boolean redundantModeCopy;

        synchronized(redemptionLock)
        {
            redundantModeCopy = redundantMode;
        }

        //if we are not in redundant mode we should wait a bit and see how this
        //goes. if we get a reply we could return bravely.
        if(!redundantModeCopy)
        {
            if(resolution.waitForResponse(currentDnsPatience))
            {
                //we are done.
                logger.debug("Got response from primary DNS in " +
                        (System.currentTimeMillis() - primaryStartTime) + "ms");
                return resolution.returnResponseOrThrowUp();
            }
            else
            {
                synchronized(redemptionLock)
                {
                    redundantMode = true;
                    redemptionStatus = currentDnsRedemption;
                    logger.info("Primary DNS seems laggy as we got no "
                                +"response for " + currentDnsPatience + "ms. "
                                + "Enabling redundant mode.");
                }
            }
        }
        else
        {
            // Even in redundant mode we need to wait a bit.  We have seen
            // instances where the backup DNS resolver responds first every
            // time but only because the router doesn't allow external DNS
            // servers and immediately responds with an apparent resolution
            // that is just an error page.
            if (resolution.waitForResponse(currentDnsPrimWait))
            {
                // Primary looks like it's back.
                logger.debug("Got quick response from primary DNS in " +
                       (System.currentTimeMillis() - primaryStartTime) +
                       "ms (" + redemptionStatus + " responses before we " +
                       "leave redundant mode");

                redemptionStatus --;

                // Yes, it's now time to end DNS redundant mode;
                if(redemptionStatus <= 0)
                {
                    redundantMode = false;
                    logger.info("Primary DNS is back to health. " +
                                "Disabling redundant mode.");
                }

                return resolution.returnResponseOrThrowUp();
            }
        }

        //we are definitely in redundant mode now
        long backupStartTime = System.currentTimeMillis();
        resolution.sendBackupQueries();

        resolution.waitForResponse(0);

        //check if it is time to end redundant mode.
        synchronized(redemptionLock)
        {
            if(!resolution.primaryResolverRespondedFirst)
            {
                //primary DNS is still feeling shaky. we reinit redemption
                //status in case we were about to cut the server some slack
                logger.debug("Backup DNS resolver responded first after " +
                         (System.currentTimeMillis() - backupStartTime) + "ms");
                redemptionStatus = currentDnsRedemption;
            }
            else
            {
                //primary server replied first. we let him redeem some dignity
                redemptionStatus --;
                logger.debug("Primary DNS resolver responded first after " +
                             (System.currentTimeMillis() - primaryStartTime) +
                             "ms (" + redemptionStatus +
                             " ticks left before we stop using the backups)");

                //yup, it's now time to end DNS redundant mode;
                if(redemptionStatus <= 0)
                {
                    redundantMode = false;
                    logger.info("Primary DNS seems back in biz. "
                                    + "Disabling redundant mode.");
                }
            }
        }

        return resolution.returnResponseOrThrowUp();
    }

    /**
     * Supposed to asynchronously send messages but not currently implemented.
     *
     * Overrides Resolver.sendAsync().
     *
     * @param query The query to send
     * @param listener The object containing the callbacks.
     * @return An identifier, which is also a parameter in the callback
     */
    @Override
    public Object sendAsync(final Message query, final ResolverListener listener)
    {
        return null;
    }

    /**
     * Sets the port to communicate on with the default servers.
     *
     * @param port The port to send messages to
     */
    @Override
    public void setPort(int port)
    {
        defaultResolver.setPort(port);
    }

    /**
     * Sets whether TCP connections will be sent by default with the default
     * resolver. Backup servers would always be contacted the same way.
     *
     * @param flag Indicates whether TCP connections are made
     */
    @Override
    public void setTCP(boolean flag)
    {
        defaultResolver.setTCP(flag);
    }

    /**
     * Sets whether truncated responses will be ignored.  If not, a truncated
     * response over UDP will cause a retransmission over TCP. Backup servers
     * would always be contacted the same way.
     *
     * @param flag Indicates whether truncated responses should be ignored.
     */
    @Override
    public void setIgnoreTruncation(boolean flag)
    {
        defaultResolver.setIgnoreTruncation(flag);
    }

    /**
     * Sets the EDNS version used on outgoing messages.
     *
     * @param level The EDNS level to use.  0 indicates EDNS0 and -1 indicates no
     * EDNS.
     * @throws IllegalArgumentException An invalid level was indicated.
     */
    @Override
    public void setEDNS(int level)
    {
        defaultResolver.setEDNS(level);
    }

    /**
     * Sets the EDNS information on outgoing messages.
     *
     * @param level The EDNS level to use.  0 indicates EDNS0 and -1 indicates no
     * EDNS.
     * @param payloadSize The maximum DNS packet size that this host is capable
     * of receiving over UDP.  If 0 is specified, the default (1280) is used.
     * @param flags EDNS extended flags to be set in the OPT record.
     * @param options EDNS options to be set in the OPT record, specified as a
     * List of OPTRecord.Option elements.
     *
     * @throws IllegalArgumentException An invalid field was specified.
     * @see OPTRecord
     */
    @Override
    public void setEDNS(int level, int payloadSize, int flags, List<EDNSOption> options)
    {
        defaultResolver.setEDNS(level, payloadSize, flags, options);
    }

    /**
     * Specifies the TSIG key that messages will be signed with
     * @param key The key
     */
    @Override
    public void setTSIGKey(TSIG key)
    {
        defaultResolver.setTSIGKey(key);
    }

    /**
     * Sets the amount of time to wait for a response before giving up.
     *
     * @param secs The number of seconds to wait.
     * @param msecs The number of milliseconds to wait.
     */
    @Override
    public void setTimeout(int secs, int msecs)
    {
        setTimeout(Duration.ofSeconds(secs).plus(Duration.ofMillis(msecs)));
    }

    /**
     * Sets the amount of time to wait for a response before giving up.
     *
     * @param secs The number of seconds to wait.
     */
    @Override
    public void setTimeout(int secs)
    {
        setTimeout(Duration.ofSeconds(secs));
    }

    /**
     * Sets the amount of time to wait for a response before giving up.
     *
     * @param timeout The amount of time to wait.
     */
    @Override
    public void setTimeout(Duration timeout)
    {
        defaultResolver.setTimeout(timeout);
    }

    /**
     * Resets resolver configuration and populate our default resolver
     * with the newly configured servers.
     */
    @Override
    public void reset()
    {
        Lookup.refreshDefault();

        // populate with new servers after refreshing configuration
        Lookup.setDefaultResolver(this);
        ExtendedResolver temp = new ExtendedResolver();
        temp.setTimeout(Duration.ofSeconds(10L));
        defaultResolver = temp;
    }

    /**
     * Determines if <tt>response</tt> can be considered a satisfactory DNS
     * response and returns accordingly.
     * <p>
     * We consider non-satisfactory responses that may indicate that the local
     * DNS does not work properly and that we may hence need to fall back to
     * the backup resolver.
     * <p>
     * Basically the goal here is to be able to go into redundant mode when we
     * come across DNS servers that send empty responses to SRV and NAPTR
     * requests.
     *
     * @param response the dnsjava {@link Message} that we'd like to inspect.
     *
     * @return <tt>true</tt> if <tt>response</tt> appears as a satisfactory
     * response and <tt>false</tt> otherwise.
     */
    protected boolean isResponseSatisfactory(Message response)
    {
        if ( response == null )
            return false;

        // NXDOMAIN should indicate that the result does not exist anywhere.
        // However, some resolvers claim that the answer is NXDOMAIN as they
        // only have a partial view of the DNS space. As such, return
        // unsatisfactory. This should be fine as we don't expect NXDOMAIN
        // to occur, except in an error case anyway, at which point, falling
        // back is reasonable.
        if (response.getRcode() == Rcode.NXDOMAIN)
            return false;

        Record[] answerRR = response.getSectionArray(Section.ANSWER);
        Record[] authorityRR = response.getSectionArray(Section.AUTHORITY);
        Record[] additionalRR = response.getSectionArray(Section.ADDITIONAL);

        if (    (answerRR     != null && answerRR.length > 0)
             || (authorityRR  != null && authorityRR.length > 0)
             || (additionalRR != null && additionalRR.length > 0))
        {
            return true;
        }

        // We didn't find any responses

        // If we received NODATA (same as NOERROR and no response records) for
        // an AAAA or a NAPTR query then it makes sense since many existing
        // domains come without those two.
        if ( response.getRcode() == Rcode.NOERROR
            && (response.getQuestion().getType() == Type.AAAA
                || response.getQuestion().getType() == Type.NAPTR))
        {
            return true;
        }

        //nope .. this doesn't make sense ...
        return false;
    }

    /**
     * The class that listens for responses to any of the queries we send to
     * our default and backup servers and returns as soon as we get one or until
     * our default resolver fails.
     */
    private class ParallelResolution extends Thread
    {
        /**
         * The query that we have sent to the default and backup DNS servers.
         */
        private final Message query;

        /**
         * The field where we would store the first incoming response to our
         * query.
         */
        public Message response;

        /**
         * The field where we would store the first error we receive from a DNS
         * or a backup resolver.
         */
        private Throwable exception;

        /**
         * Indicates whether we are still waiting for an answer from someone
         */
        private boolean done = false;

        /**
         * Indicates that a response was received from the primary resolver.
         */
        private boolean primaryResolverRespondedFirst = true;

        /**
         * Creates a {@link ParallelResolution} for the specified <tt>query</tt>
         *
         * @param query the DNS query that we'd like to send to our primary
         * and backup resolvers.
         */
        public ParallelResolution(final Message query)
        {
            super("ParallelResolution Primary Resolver");
            this.query = query;
        }

        /**
         * Starts this collector which would cause it to send its query to the
         * default resolver.
         */
        public void sendFirstQuery()
        {
            start();
        }

        /**
         * Sends this collector's query to the default resolver.
         */
        @Override
        public void run()
        {
            Message localResponse = null;
            try
            {
                localResponse = defaultResolver.send(query);
            }
            catch (Throwable exc)
            {
                logger.info("Exception occurred during parallel DNS resolving" +
                        exc, exc);

                this.exception = exc;
            }
            synchronized(this)
            {
                //if the backup resolvers had already replied we ignore the
                //reply of the primary one whatever it was.
                if(done)
                    return;

                //if there was a response we're only done if it is satisfactory
                if(    localResponse != null
                    && isResponseSatisfactory(localResponse))
                {
                    response = localResponse;
                    done = true;
                }
                notify();
            }
        }

        /**
         * Asynchronously sends this collector's query to all backup resolvers.
         */
        public void sendBackupQueries()
        {
            logger.info("Send DNS queries to backup resolvers");

            //yes. a second thread in the thread ... it's ugly but it works
            //and i do want to keep code simple to read ... this whole parallel
            //resolving is complicated enough as it is.
            new Thread("ParallelResolution Backup Resolver")
            {
                @Override
                public void run()
                {
                    synchronized(ParallelResolution.this)
                    {
                        if (done)
                            return;
                    }

                    Message localResponse = null;
                    try
                    {
                        localResponse = backupResolver.send(query);
                    }
                    catch (Throwable exc)
                    {
                        logger.info("Exception occurred during backup "
                                    +"DNS resolving", exc);

                        synchronized (ParallelResolution.this)
                        {
                            // keep this so that we can rethrow it
                            if (exception == null)
                            {
                                exception = exc;
                            }
                        }
                    }

                    synchronized (ParallelResolution.this)
                    {
                        // If the default resolver has already replied we
                        // ignore the reply of the backup ones.
                        if(done)
                        {
                            return;
                        }

                        // If we've got a response and it's satisfactory
                        // return it
                        if(localResponse != null
                           && isResponseSatisfactory(localResponse))
                        {
                            response = localResponse;
                            done = true;
                            primaryResolverRespondedFirst = false;
                            ParallelResolution.this.notify();
                            return;
                        }
                    }

                    // We received a result from the backup resolver
                    // which wasn't satisfactory. Try waiting for the primary
                    // resolver to return a satisfactory response for a short
                    // period
                    synchronized (ParallelResolution.this)
                    {
                        if (waitForResponse(DNS_PATIENCE))
                        {
                            return;
                        }

                        // The primary resolver isn't replying - just use
                        // whatever response (however unsatisfactory) we got
                        // from the backup resolver
                        response = localResponse;
                        done = true;
                        primaryResolverRespondedFirst = false;
                        ParallelResolution.this.notify();
                        return;
                    }
                }
            }.start();
        }

        /**
         * Waits for a response or an error to occur during <tt>waitFor</tt>
         * milliseconds.If neither happens, we return false.
         *
         * @param waitFor the number of milliseconds to wait for a response or
         * an error or <tt>0</tt> if we'd like to wait until either of these
         * happen.
         *
         * @return <tt>true</tt> if we returned because we received a response
         * from a resolver or errors from everywhere, and <tt>false</tt> that
         * didn't happen.
         */
        public boolean waitForResponse(long waitFor)
        {
            // Cache the time outside of the synchronized block to avoid
            // taking into account the time it takes to get the lock.
            long l = System.currentTimeMillis();

            synchronized(this)
            {
                while(! done)
                {
                    long left = 0;

                    // If we aren't waiting for ever, work out how long we've
                    // got to wait for.
                    if (waitFor != 0)
                    {
                         left = (l + waitFor) - System.currentTimeMillis();

                         if (left <= 0)
                         {
                             // We've run out of time. Return
                             break;
                         }
                    }

                    try
                    {
                        wait(left);
                    }
                    catch (InterruptedException e)
                    {
                        // We don't care about being interrupted.
                    }
                }

                return done;
            }
        }

        /**
         * Waits for resolution to complete (if necessary) and then either
         * returns the response we received or throws whatever exception we
         * saw.
         *
         * @return the response {@link Message} we received from the DNS.
         *
         * @throws IOException if this resolution ended badly because of a
         * network IO error
         * @throws RuntimeException if something unexpected happened
         * during resolution.
         * @throws IllegalArgumentException if something unexpected happened
         * during resolution or if there was no response.
         */
        public synchronized Message returnResponseOrThrowUp()
            throws IOException, RuntimeException, IllegalArgumentException
        {
            if(!done)
                waitForResponse(0);

            if(response != null)
            {
                return response;
            }
            else if (exception == null)
            {
                throw new IllegalStateException("No response from resolver");
            }
            else if (exception instanceof IOException)
            {
                logger.warn("IO exception while using DNS resolver", exception);
                throw (IOException) exception;
            }
            else if (exception instanceof RuntimeException)
            {
                logger.warn("RunTimeException while using DNS resolver",
                        exception);
                throw (RuntimeException) exception;
            }
            else if (exception instanceof Error)
            {
                logger.warn("Error while using DNS resolver", exception);
                throw (Error) exception;
            }
            else
            {
                logger.warn("Received a bad response from DNS resolver",
                        exception);
                throw new IllegalStateException("ExtendedResolver failure", exception);
            }
        }
    }

    @SuppressWarnings("serial")
    private final Set<String> configNames = new HashSet<String>(5)
    {{
        add(DnsUtilActivator.PNAME_BACKUP_RESOLVER);
        add(DnsUtilActivator.PNAME_BACKUP_RESOLVER_FALLBACK_IP);
        add(DnsUtilActivator.PNAME_BACKUP_RESOLVER_PORT);
        add(CustomResolver.PNAME_DNS_PATIENCE);
        add(CustomResolver.PNAME_DNS_REDEMPTION);
    }};

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (!configNames.contains(evt.getPropertyName()))
        {
            return;
        }

        initProperties();
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
        // We just use the default ResolverConfig ones so get them.
        ResolverConfig resolverConfig = ResolverConfig.getCurrentConfig();
        ArrayList<String> servers = new ArrayList<>();

        if ((resolverConfig != null) && (resolverConfig.servers() != null))
        {
            for (String s : NetworkUtils.convertServerListToStrings(resolverConfig.servers()))
            {
                servers.add(s);
            }
        }

        // If running redundant mode we will also be using the backup servers
        // so don't forget to add them to the list.
        if (redundantMode)
        {
            String customResolverIP
              = DnsUtilActivator.getConfigurationService().global().getString(
                  DnsUtilActivator.PNAME_BACKUP_RESOLVER_FALLBACK_IP,
                  DnsUtilActivator.getResources().getSettingsString(
                      DnsUtilActivator.PNAME_BACKUP_RESOLVER_FALLBACK_IP));

            servers.add(customResolverIP);
        }

        return servers.toArray(new String[0]);
    }

    public void setDefaultResolver(Resolver resolver)
    {
        defaultResolver = resolver;
    }

    public void setBackupResolver(ExtendedResolver resolver)
    {
        backupResolver = resolver;
    }

    @Override
    public void accountLoaded()
    {
    }
}
