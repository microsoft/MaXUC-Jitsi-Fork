// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.generalconfig;

import java.net.URI;
import java.net.URISyntaxException;

import net.java.sip.communicator.service.notification.SoundNotificationAction;
import net.java.sip.communicator.util.Logger;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.util.StringUtils;

/**
 * Class for updating stored config properties pointing to custom ringtones over
 * events such as name change over upgrade.
 */
public class RingtonePathUpdater
{
    /**
     * The logger.
     */
    private static final Logger logger =
            Logger.getLogger(RingtonePathUpdater.class);

    /**
     * The configuration service.
     */
    private ConfigurationService configService =
            GeneralConfigPluginActivator.getConfigurationService();

    /**
     * The properties to the current paths of the user's custom ringtone, stored in app data.
     */
    private static final String CURRENT_RINGTONE_PATH_PROPNAME =
            "net.java.sip.communicator.plugin.generalconfig.CURRENT_RINGTONE_PATH";
    private static final String CURRENT_BG_RINGTONE_PATH_PROPNAME =
            "net.java.sip.communicator.plugin.generalconfig.CURRENT_BG_RINGTONE_PATH";

    /**
     * The URI paths for the custom ringtones.
     */
    private static final String CUSTOM_RINGTONE_URI_PROPNAME =
            "net.java.sip.communicator.plugin.generalconfig.CUSTOM_RINGTONE_URI";
    private static final String CUSTOM_BG_RINGTONE_URI_PROPNAME =
            "net.java.sip.communicator.plugin.generalconfig.CUSTOM_BG_RINGTONE_URI";

    /**
     * Change config properties pointing to custom ringtones if they have moved.
     *
     * @param oldName The old app name
     * @param newName The new app name
     * @param bgTag   Business group tag, depending on the context
     */
    public String updateRingtonePaths(String oldName, String newName, SoundNotificationAction.BgTag bgTag)
    {
        String ringtonePath;
        String ringtoneUri;
        String currentRingtonePathProp;
        String customRingtoneUriProp;

        if (bgTag == SoundNotificationAction.BgTag.BG_TAG_GENERIC)
        {
            customRingtoneUriProp = CUSTOM_RINGTONE_URI_PROPNAME;
            currentRingtonePathProp = CURRENT_RINGTONE_PATH_PROPNAME;
            ringtonePath = configService.user().getString(CURRENT_RINGTONE_PATH_PROPNAME);
            ringtoneUri = configService.user().getString(CUSTOM_RINGTONE_URI_PROPNAME);
        }
        else
        {
            customRingtoneUriProp = CUSTOM_BG_RINGTONE_URI_PROPNAME;
            currentRingtonePathProp = CURRENT_BG_RINGTONE_PATH_PROPNAME;
            ringtonePath = configService.user().getString(CURRENT_BG_RINGTONE_PATH_PROPNAME);
            ringtoneUri = configService.user().getString(CUSTOM_BG_RINGTONE_URI_PROPNAME);
        }

        // Only make changes if the user has previously set a custom ringtone,
        // in which case the custom URI will exist.
        if (!StringUtils.isNullOrEmpty(ringtoneUri))
        {
            String parsedOldName;
            String parsedNewName;
            try
            {
                // Parse the old and new name as URI so we can replace them in
                // the stored config addresses.
                parsedOldName = new URI("file", oldName, null)
                        .getRawSchemeSpecificPart();
                parsedNewName = new URI("file", newName, null)
                        .getRawSchemeSpecificPart();
            }
            catch (URISyntaxException e)
            {
                logger.error("Caught error while parsing old and new " +
                             "names, so not changing ringtone property: " +
                             e.getMessage());
                return ringtonePath;
            }

            // Only make changes if the user's custom ringtone path contains the
            // old app name, i.e. is pointing to the old, non-existent folder.
            if (ringtoneUri.contains(parsedOldName))
            {
                logger.debug("Updating URI to custom ringtone");
                ringtoneUri = ringtoneUri.replaceAll(parsedOldName,
                                                     parsedNewName);
                configService.user().setProperty(customRingtoneUriProp, ringtoneUri);

                // Now update the path if the user is still using the custom
                // ringtone. If not, they may just have it stored but selected
                // something else afterwards.
                if (!StringUtils.isNullOrEmpty(ringtonePath) &&
                    ringtonePath.contains(parsedOldName))
                {
                    logger.debug("Updating path to custom ringtone");
                    ringtonePath = ringtonePath.replaceAll(parsedOldName,
                                                           parsedNewName);
                    configService.user().setProperty(currentRingtonePathProp,
                                                     ringtonePath);
                }
            }
        }
        return ringtonePath;
    }
}
