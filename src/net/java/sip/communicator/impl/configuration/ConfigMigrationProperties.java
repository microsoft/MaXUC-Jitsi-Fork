// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.configuration;

import java.util.*;

interface ConfigMigrationProperties
{
    // Properties that store whether or not we have migrated individual pieces
    // of config.
    String PROP_MIGRATION_PREFIX = "net.java.sip.communicator.impl.configuration";
    String PROP_HAS_MIGRATED =
        PROP_MIGRATION_PREFIX + ".HAS_MIGRATED";
    String PROP_HAS_MIGRATED_CONTACTS =
        PROP_MIGRATION_PREFIX + ".HAS_MIGRATED_CONTACTS";
    String PROP_HAS_MIGRATED_RINGTONE =
        PROP_MIGRATION_PREFIX + ".HAS_MIGRATED_RINGTONE";
    String PROP_HAS_MIGRATED_HISTORY =
        PROP_MIGRATION_PREFIX + ".HAS_MIGRATED_HISTORY";
    String PROP_HAS_MIGRATED_CONFIG =
        PROP_MIGRATION_PREFIX + ".HAS_MIGRATED_CONFIG";
    String PROP_HAS_MIGRATED_AVATAR_CACHE =
        PROP_MIGRATION_PREFIX + ".HAS_MIGRATED_AVATAR_CACHE";

    /** The property where the user name is stored */
    String PROP_USER =
                  "net.java.sip.communicator.plugin.provisioning.auth.USERNAME";

    /** The properties that should not be moved from global to user config. */
    List<String> PROPS_TO_NOT_MOVE = Arrays.asList(
        "net.java.sip.communicator.APPLICATION_PATH_NAME",
        "net.java.sip.communicator.UUID",
        "net.java.sip.communicator.ALT_ANALYTICS_UID",
        "net.java.sip.communicator.plugin.cdap.BRANDING_INFO",
        "net.java.sip.communicator.plugin.cdap.service_provider_id",
        "net.java.sip.communicator.plugin.cdap.version_number",
        "net.java.sip.communicator.plugin.eula.EULA_ACCEPTED_VER",
        "net.java.sip.communicator.plugin.eula.EXTRA_EULA_ACCEPTED_VER",
        "net.java.sip.communicator.plugin.eula.EXTRA_EULA_VER",
        "net.java.sip.communicator.plugin.provisioning.SERVICE_PROVIDER_NAME",
        "net.java.sip.communicator.plugin.provisioning.METHOD",
        "net.java.sip.communicator.plugin.provisioning.auth.ACTIVE_USER",
        "net.java.sip.communicator.plugin.contactdetails.DIAL_AS_E164",
        "net.java.sip.communicator.plugin.provisioning.URL",
        "net.java.sip.communicator.plugin.provisioning.URL_PREFIX",
        "net.java.sip.communicator.impl.protocol.SIP_DSCP",
        "net.java.sip.communicator.impl.protocol.RTP_AUDIO_DSCP",
        "net.java.sip.communicator.impl.protocol.RTP_VIDEO_DSCP",
        "net.java.sip.communicator.impl.protocol.jabber.EXTRA_VCARD_INFO_DISABLED",
        "net.java.sip.communicator.service.resources.DefaultLocale",
        "plugin.conference.accessionmeeting.DIAL_IN_INFO_1",
        "plugin.conference.accessionmeeting.DIAL_IN_INFO_2",
        "plugin.conference.accessionmeeting.VANITY_URL",
        PROP_USER,
        PROP_HAS_MIGRATED,
        PROP_HAS_MIGRATED_CONTACTS,
        PROP_HAS_MIGRATED_RINGTONE,
        PROP_HAS_MIGRATED_HISTORY,
        PROP_HAS_MIGRATED_CONFIG
    );
}
