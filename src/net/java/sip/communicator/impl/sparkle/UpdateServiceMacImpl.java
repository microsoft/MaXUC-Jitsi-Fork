// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.sparkle;

import net.java.sip.communicator.service.update.UpdateService;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import org.jitsi.service.version.VersionService;

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
    /**
     * Name of property to force update.
     */
    private static final String FORCE_UPDATE = "net.java.sip.communicator.plugin.update.FORCE_UPDATE";
    private static final Logger logger = Logger.getLogger(UpdateServiceMacImpl.class);

    @Override
    public void checkForUpdates(boolean isUserTriggered)
    {
        if (!isUserTriggered &&
            (ConfigurationUtils.isAutoUpdateCheckingDisabledForUser() ||
             ConfigurationUtils.isAutoUpdateCheckingDisabledGlobally()))
        {
            logger.info("Automatic update checking is disabled.");
            return;
        }

        blockUserInteractionIfBelowMinimumVersion();
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
     * Prevents the user from interacting with the app if the current app version
     * is below the minimum version allowed by SIP-PS config.
     */
    private static void blockUserInteractionIfBelowMinimumVersion()
    {
        final VersionService versionService = SparkleActivator.getVersionService();

        if (versionService.isOutOfDate())
        {
            logger.debug("Version too low - forcing update on macOS");
            SparkleActivator.getConfigurationService().user().setProperty(FORCE_UPDATE, true);
        }
        else
        {
            logger.debug("Version is fine - removing FORCE_UPDATE property " +
                         "if it exists from a previously out-of-date client");
            SparkleActivator.getConfigurationService().user().removeProperty(FORCE_UPDATE);
        }
    }

    /**
     * WARNING: Forced updates are implemented differently for macOS due to the
     * Sparkle API.
     * Calling this method has no effect.
     */
    @Override
    public void forceUpdate()
    {
        logger.info("forceUpdate() is unused on macOS. Call checkForUpdates() with" +
                    " a subscriber below the minimum allowed version in SIP-PS config for" +
                    " forced update behaviour on macOS.");
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

    @Override
    public void forceQuitApplication()
    {
        logger.info("forceQuitApplication not supported on Mac, so do nothing");
    }

    @Override
    public void cancelUpdateDownload()
    {
        logger.info("cancelUpdateDownload not supported on Mac, so do nothing");
    }
}
