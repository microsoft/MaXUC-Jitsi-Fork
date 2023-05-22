/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.version;

import java.util.regex.*;

import net.java.sip.communicator.util.Logger;
import org.jitsi.service.version.*;

/**
 * The version service keeps track of the Jitsi version that we are
 * currently running. Other modules (such as a Help->About dialog) query and
 * use this service in order to show the current application version.
 * <p>
 * This version service implementation is based around the VersionImpl class
 * where all details of the version are statically defined.
 *
 * @author Emil Ivov
 */
public class VersionServiceImpl
    implements VersionService
{
    private static final Logger logger
            = Logger.getLogger(VersionServiceImpl.class);
    /**
     * The pattern that will parse strings to version object.
     */
    private static final Pattern PARSE_VERSION_STRING_PATTERN =
        Pattern.compile("[Vv]?(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:\\.([\\d\\.]+))?");

    /**
     * Returns a <tt>Version</tt> object containing version details of the
     * Jitsi version that we're currently running.
     *
     * @return a <tt>Version</tt> object containing version details of the
     *   Jitsi version that we're currently running.
     */
    public Version getCurrentVersion()
    {
        return VersionImpl.currentVersion();
    }

    /**
     * Returns a Version instance corresponding to the <tt>version</tt>
     * string.
     *
     * @param version a version String that we have obtained by calling a
     *   <tt>Version.toString()</tt> method.
     * @return the <tt>Version</tt> object corresponding to the
     *   <tt>version</tt> string. Or null if we cannot parse the string.
     */
    public Version parseVersionString(String version)
    {
        Matcher matcher = PARSE_VERSION_STRING_PATTERN.matcher(version);

        if(matcher.matches() && matcher.groupCount() == 4)
        {
            return VersionImpl.customVersion(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                matcher.group(3),
                matcher.group(4));
        }

        return null;
    }

    /**
     * Returns true if the client is running a version below the minimum
     * allowed version specified in the current subscriber's config (false if
     * no such minimum version is provided in the config).
     */
    public boolean isOutOfDate()
    {
        final String minimumVersionString = VersionActivator
                .getConfigurationService()
                .user()
                .getString("net.java.sip.communicator.MIN_VERSION");
        Version currentVersion = getCurrentVersion();

        Version minimumVersion = minimumVersionString == null ? null :
                parseVersionString(minimumVersionString);

        logger.debug("Comparing current and minimum versions: " +
                    currentVersion + ", " + minimumVersion);

        return minimumVersion != null &&
               currentVersion.compareTo(minimumVersion) < 0;
    }
}
