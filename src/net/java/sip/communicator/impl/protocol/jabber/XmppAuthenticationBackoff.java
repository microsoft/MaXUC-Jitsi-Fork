// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.util.ServerBackoff;
import org.jitsi.service.configuration.ConfigurationService;

/**
 * Class for backing off from XMPP login retries in the event of authentication
 * errors. These can occur if the auth procedure on AMS times out due to
 * e.g. it is being restarted and the client needs to switch nodes.
 */
public class XmppAuthenticationBackoff extends ServerBackoff
{
    private static final String CONFIG_INITIAL_FAIL_BACKOFF =
            "net.java.sip.communicator.impl.protocol.jabber.intialfailbackoff";
    private static final String CONFIG_MAX_NUMBER_FAILURE_DOUBLES =
            "net.java.sip.communicator.impl.protocol.jabber.maxnumberfailuredoubles";

    /** Default initial backoff interval is 2000 ms. */
    private static final long DEFAULT_INITIAL_FAIL_BACKOFF = 2000;

    /** Double the backoff interval 8 times by default (~ 9 minutes). */
    private static final int DEFAULT_MAX_NUMBER_FAILURE_DOUBLES = 8;

    private static final ConfigurationService configService = JabberActivator.getConfigurationService();

    @Override
    public int getMaxNumberFailureDoubles()
    {
        return configService.global().getInt(CONFIG_MAX_NUMBER_FAILURE_DOUBLES, DEFAULT_MAX_NUMBER_FAILURE_DOUBLES);
    }

    @Override
    public long getInitialFailBackoff()
    {
        return configService.global().getLong(CONFIG_INITIAL_FAIL_BACKOFF, DEFAULT_INITIAL_FAIL_BACKOFF);
    }

    /**
     * Once the backoff time hits its maximum limit, give up.
     */
    @Override
    public synchronized boolean shouldRetry()
    {
        return !mHasHitMaxDoubles;
    }
}
