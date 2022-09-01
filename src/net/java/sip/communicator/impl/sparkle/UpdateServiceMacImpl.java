// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.sparkle;

import net.java.sip.communicator.service.update.UpdateService;
import net.java.sip.communicator.util.Logger;

/**
 * Implements checking for updates on Mac.
 *
 * Checking for, downloading and applying updates (and creating appropriate UI)
 * is handled by Sparkle, a third-party library.
 * The sole function of this service is to prompt Sparkle to begin the 'Check
 * for updates' flow.
 */
public class UpdateServiceMacImpl implements UpdateService
{
    private static final Logger logger = Logger.getLogger(UpdateServiceMacImpl.class);

    @Override
    public void checkForUpdates(boolean isUserTriggered)
    {
        logger.debug("Calling checkForUpdates() with isUserTriggered = " + isUserTriggered);
        if (isUserTriggered)
        {
            logger.info("Checking for updates (user-requested)");
            SparkleActivator.checkForUpdates();
        }
        else
        {
            logger.debug("Client requested a background update check - ",
                         "ignoring, as these are entirely handled by Sparkle on Mac");
        }
    }

    /**
     * WARNING: Forced updates are not supported by the API Sparkle exposes
     * to the client.
     * Calling this method has no effect.
     */
    @Override
    public void forceUpdate()
    {
        logger.info("Client-triggered forced update not supported on Mac, so doing nothing");
    }

    @Override
    public void updateIfAvailable()
    {
        // TODO
        logger.info("updateIfAvailable not supported on Mac, so doing nothing");
    }

    @Override
    public boolean isLatestVersion()
    {
        // TODO
        logger.info("isLatestVersion not supported on Mac, returning true");
        return true;
    }
}
