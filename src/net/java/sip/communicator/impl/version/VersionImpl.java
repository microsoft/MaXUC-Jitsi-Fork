/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.version;

import org.jitsi.service.resources.*;
import org.jitsi.service.version.*;

import net.java.sip.communicator.util.*;

/**
 * A static implementation of the Version interface.
 */
public class VersionImpl
    implements Version
{
    /**
     * The version major of the current MaX UC version. In an
     * example 2.3.1 version string 2 is the version major. The version major
     * number changes when a relatively extensive set of new features and
     * possibly rearchitecturing have been applied.
     */
    public static final int VERSION_MAJOR = 3;

    /**
     * The version major field. Default value is VERSION_MAJOR.
     */
    private int versionMajor = VERSION_MAJOR;

    /**
     * The version major of the current Jitsi version. In an
     * example 2.3.1 version string 2 is the version major. The version major
     * number changes when a relatively extensive set of new features and
     * possibly rearchitecturing have been applied to the Jitsi.
     *
     * N.B. We always want a 2-digit minor version format! However, to avoid
     * making changes to the structure of the code (i.e. changing this variable
     * to a String), we just tweak this in the toString method below,
     * where it matters as that is what affects what the user sees, what the
     * logs say and what analytics are sent. Otherwise, for comparison purposes,
     * X.0 and X.00 are treated as the same version - see compareTo() method.
     *
     * See DUIR-6751 for more details, and consider reworking this code in the
     * future!
     */
    public static final int VERSION_MINOR = 10;

    /**
     * The version minor field. Default value is VERSION_MINOR.
     */
    private int versionMinor = VERSION_MINOR;

    /**
     * Indicates whether this version represents a prerelease (i.e. a
     * non-complete release like an alpha, beta or release candidate version).
     */
    public static final boolean IS_PRE_RELEASE_VERSION  = false;

    /**
     * Returns the version prerelease ID of the current Jitsi version
     * and null if this version is not a prerelease.
     */
    public static final String PRE_RELEASE_ID = "beta1";

    /**
     * Indicates if this Jitsi version corresponds to a nightly build
     * of a repository snapshot or to an official Jitsi release.
     */
    public static final boolean IS_NIGHTLY_BUILD = true;

    /**
     * The nightly release field. Default value is NightlyBuildID.RELEASE.
     */
    private String nightlyRelease = NightlyBuildID.RELEASE;

    /**
     * The nightly build id field. Default value is NightlyBuildID.BUILD_ID.
     */
    private String nightlyBuildID = NightlyBuildID.BUILD_ID;

    /**
     * The default name of this application.
     */
    public static final String DEFAULT_APPLICATION_NAME = "Jitsi";

    /**
     * The name of this application.
     */
    private static String applicationName = null;

    /**
     * Returns the VersionImpl instance describing the current version of
     * Jitsi.
     */
    public static final VersionImpl CURRENT_VERSION = new VersionImpl();

    /**
     * Creates version object with default (current) values.
     */
    private VersionImpl()
    {}

    /**
     * Creates version object with custom major, minor and nightly build id.
     *
     * @param majorVersion the major version to use.
     * @param minorVersion the minor version to use.
     * @param nightlyBuildID the nightly build id value for new version object.
     */
    private VersionImpl(int majorVersion,
                       int minorVersion,
                       String nightlyRelease,
                       String nightlyBuildID)
    {
        this.versionMajor = majorVersion;
        this.versionMinor = minorVersion;
        this.nightlyRelease = nightlyRelease;
        this.nightlyBuildID = nightlyBuildID;
    }

    /**
     * Returns the version major of the current Jitsi version. In an
     * example 2.3.1 version string 2 is the version major. The version major
     * number changes when a relatively extensive set of new features and
     * possibly rearchitecturing have been applied to the Jitsi.
     *
     * @return the version major String.
     */
    public int getVersionMajor()
    {
        return versionMajor;
    }

    /**
     * Returns the version minor of the current Jitsi version. In an
     * example 2.3.1 version string 3 is the version minor. The version minor
     * number changes after adding enhancements and possibly new features to a
     * given Jitsi version.
     *
     * @return the version minor integer.
     */
    public int getVersionMinor()
    {
        return versionMinor;
    }

    /**
     * Indicates if this Jitsi version corresponds to a nightly build
     * of a repository snapshot or to an official Jitsi release.
     *
     * @return true if this is a build of a nightly repository snapshot and
     * false if this is an official Jitsi release.
     */
    public boolean isNightly()
    {
        return IS_NIGHTLY_BUILD;
    }

    /**
     * If this is a nightly build, returns the build's release.
     * If this is not a nightly build Jitsi version, the method returns null.
     *
     * @return a String containing a nightly build identifier or null if this is
     * a release version and therefore not a nightly build
     */
    public String getNightlyRelease()
    {
        if(!isNightly())
            return null;

        return nightlyRelease;
    }

    /**
     * If this is a nightly build, returns the build identifies (e.g.
     * nightly-2007.12.07-06.45.17). If this is not a nightly build Jitsi
     * version, the method returns null.
     *
     * @return a String containing a nightly build identifier or null if this is
     * a release version and therefore not a nightly build
     */
    public String getNightlyBuildID()
    {
        if(!isNightly())
            return null;

        return nightlyBuildID;
    }

    /**
     * Indicates whether this version represents a prerelease (i.e. a
     * non-complete release like an alpha, beta or release candidate version).
     * @return true if this version represents a prerelease and false otherwise.
     */
    public boolean isPreRelease()
    {
        return IS_PRE_RELEASE_VERSION;
    }

    /**
     * Returns the version prerelease ID of the current Jitsi version
     * and null if this version is not a prerelease.
     *
     * @return a String containing the version prerelease ID.
     */
    public String getPreReleaseID()
    {
        if(!isPreRelease())
            return null;

        return PRE_RELEASE_ID;
    }

    /**
     * Compares another <tt>Version</tt> object to this one and returns a
     * negative, zero or a positive integer if this version instance represents
     * respectively an earlier, same, or later version as the one indicated
     * by the <tt>version</tt> parameter.
     *
     * @param version the <tt>Version</tt> instance that we'd like to compare
     * to this one.
     *
     * @return a negative integer, zero, or a positive integer as this object
     * represents a version that is earlier, same, or more recent than the one
     * referenced by the <tt>version</tt> parameter.
     */
    public int compareTo(Version version)
    {
        if(version == null)
        {
            return -1;
        }

        if(getVersionMajor() != version.getVersionMajor())
        {
            return getVersionMajor() - version.getVersionMajor();
        }

        if(getVersionMinor() != version.getVersionMinor())
        {
            return getVersionMinor() - version.getVersionMinor();
        }

        String ourRelease = getNightlyRelease();
        String ourID = getNightlyBuildID();

        if (ourRelease != null && !ourRelease.equals(""))
        {
            if (ourID != null && !ourID.equals(""))
            {
                ourID = ourRelease + "." + ourID;
            }
            else
            {
                ourID = ourRelease;
            }
        }
        else if (ourID == null)
        {
            ourID = "";
        }

        String theirRelease = version.getNightlyRelease();
        String theirID = version.getNightlyBuildID();

        if (theirRelease != null && !theirRelease.equals(""))
        {
            if (theirID != null && !theirID.equals(""))
            {
                theirID = theirRelease + "." + theirID;
            }
            else
            {
                theirID = theirRelease;
            }
        }
        else if (theirID == null)
        {
            theirID = "";
        }

        String[] ours = ourID.split("\\.");
        String[] theirs = theirID.split("\\.");

        for(int i = 0; i < Math.min(ours.length, theirs.length); ++i)
        {
            try
            {
                int a = Integer.parseInt(ours[i]);
                int b = Integer.parseInt(theirs[i]);

                if (a != b)
                {
                    return a - b;
                }
            }
            catch (NumberFormatException e)
            {
                int result = ours[i].compareTo(theirs[i]);

                if (result != 0)
                {
                    return result;
                }
            }
        }

        return 0;
    }

    /**
     * Compares the <tt>version</tt> parameter to this version and returns true
     * if and only if both reference the same Jitsi version and
     * false otherwise.
     *
     * @param version the version instance that we'd like to compare with this
     * one.
     * @return true if and only the version param references the same
     * Jitsi version as this Version instance and false otherwise.
     */
    public boolean equals(Object version)
    {
        //simply compare the version strings
        return toString().equals( (version == null)
                                        ? "null"
                                        : version.toString());
    }

    /**
     * Returns a String representation of this Version instance in the generic
     * form of major.minor[.release.build]. If you'd just like to obtain the
     * version of Jitsi so that you could display it (e.g. in a Help->About
     * dialog) then all you need is calling this method.
     *
     * @return a major.minor[.release.build] String containing the complete
     * Jitsi version.
     */
    public String toString()
    {
        return toString(true);
    }

    /**
     * Returns a String representation of this Version instance in the generic
     * form of major.minor[.release.build]. If you'd just like to obtain the
     * version of Jitsi so that you could display it (e.g. in a Help->About
     * dialog) then all you need is calling this method.
     *
     * @param includeNightlyBuildID whether to include the build ID.
     * @return a major.minor[.release[.build]] String containing the complete
     * Jitsi version.
     */
    public String toString(boolean includeNightlyBuildID)
    {
        StringBuffer versionStringBuff = new StringBuffer();

        versionStringBuff.append(Integer.toString(getVersionMajor()));
        versionStringBuff.append(".");

        // For various reasons to do with alphabetic ordering, we want to have
        // a 2-digit format for the minor version, so pad the number if it's
        // less than 10. See DUIR-6751 for more details.
        versionStringBuff.append(String.format("%02d", getVersionMinor()));

        if(isPreRelease())
        {
            versionStringBuff.append("-");
            versionStringBuff.append(getPreReleaseID());
        }

        if(isNightly())
        {
            String release = getNightlyRelease();
            if (release != null && !release.equals(""))
            {
                versionStringBuff.append(".");
                versionStringBuff.append(release);
            }

            if (includeNightlyBuildID)
            {
                versionStringBuff.append(".");
                versionStringBuff.append(getNightlyBuildID());
            }
        }

        return versionStringBuff.toString();
    }

    /**
     * Returns the VersionImpl instance describing the current version of
     * Jitsi.
     *
     * @return the VersionImpl instance describing the current version of
     * Jitsi.
     */
    public static VersionImpl currentVersion()
    {
        return CURRENT_VERSION;
    }

    /**
     * Returns the VersionImpl instance describing the version with the
     * parameters supplied.
     *
     * @param majorVersion the major version to use.
     * @param minorVersion the minor version to use.
     * @param nightlyBuildID the nightly build id value for new version object.
     * @return the VersionImpl instance describing the version with parameters
     * supplied.
     */
    public static VersionImpl customVersion(
        int majorVersion,
        int minorVersion,
        String nightlyRelease,
        String nightlyBuildID)
    {
        return new VersionImpl(majorVersion, minorVersion, nightlyRelease, nightlyBuildID);
    }

    /**
     * Returns the name of the application that we're currently running. Default
     * MUST be Jitsi.
     *
     * @return the name of the application that we're currently running. Default
     * MUST be Jitsi.
     */
    public String getApplicationName()
    {
        if (applicationName == null)
        {
            try
            {
                /*
                 * XXX There is no need to have the ResourceManagementService
                 * instance as a static field of the VersionImpl class because
                 * it will be used once only anyway.
                 */
                ResourceManagementService resources
                    = ServiceUtils.getService(
                            VersionActivator.getBundleContext(),
                            ResourceManagementService.class);

                if (resources != null)
                {
                    applicationName
                        = resources.getSettingsString(
                                "service.gui.APPLICATION_NAME");
                }
            }
            catch (Exception e)
            {
                // if resource bundle is not found or the key is missing
                // return the default name
            }
            finally
            {
                if (applicationName == null)
                    applicationName = DEFAULT_APPLICATION_NAME;
            }
        }
        return applicationName;
    }
}
