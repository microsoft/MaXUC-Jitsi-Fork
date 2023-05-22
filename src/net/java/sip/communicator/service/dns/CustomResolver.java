/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.dns;

import org.xbill.DNS.*;

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
public interface CustomResolver
    extends Resolver
{
    /**
     * The default number of milliseconds it takes us to get into redundant
     * mode while waiting for a DNS query response.
     */
    int DNS_PATIENCE = 1500;

    /**
     * The name of the property that allows us to override the default
     * <tt>DNS_PATIENCE</tt> value.
     */
    String PNAME_DNS_PATIENCE
        = "net.java.sip.communicator.util.dns.DNS_PATIENCE";

    /**
     * The default number of milliseconds we wait for the primary DNS server(s)
     * to respond while in redundant mode.
     */
    int DNS_PRIM_WAIT = 75;

    /**
     * The name of the property that allows us to override the default
     * <tt>DNS_PRIM_WAIT</tt> value.
     */
    String PNAME_DNS_PRIM_WAIT
        = "net.java.sip.communicator.util.dns.DNS_PRIM_WAIT";

    /**
     * The default number of times that the primary DNS would have to provide a
     * faster response than the backup resolver before we consider it safe
     * enough to exit redundant mode.
     */
    int DNS_REDEMPTION = 3;

    /**
     * The name of the property that allows us to override the default
     * <tt>DNS_REDEMPTION</tt> value.
     */
    String PNAME_DNS_REDEMPTION
        = "net.java.sip.communicator.util.dns.DNS_REDEMPTION";

    /**
     * The currently configured number of times that the primary DNS would have
     * to provide a faster response than the backup resolver before we consider
     * it safe enough to exit redundant mode.
     */
    int currentDnsRedemption = DNS_REDEMPTION;

    /**
     * Resets resolver configuration and populate our default resolver
     * with the newly configured servers.
     */
    void reset();

    /**
     * Called when an account has been loaded, allowing for account change
     * notification registration.
     */
    void accountLoaded();

    /**
     * Returns a list of DNS servers in use by this Resolver.
     */
    String[] getDnsServers();
}
