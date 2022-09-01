// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.commportal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a version of the CommPortal API
 */
public final class CommPortalVersion implements Comparable<CommPortalVersion>
{
    /**
     * Major release number - 8 for 8.1.04
     */
    private int mMajor;

    /**
     * Minor release number - 1 for 8.1.04
     */
    private int mMinor;

    /**
     * Release number - 4 for 8.1.04
     */
    private int mRelease;

    /**
     * Maintenance release number - 16 for 8.1.04.16
     */
    private int mMaintenance;

    /**
     * Suffix - w for 7.4w
     */
    private String mSuffix; // w for 7.4w

    /**
     * String as provided - "8.1.04" for 8.1.04
     */
    private String mString;

    private static final Pattern COMMPORTAL_VERSION_REGEX =
        Pattern.compile("[Vv]?(\\d+)\\.(\\d+)(?:[.\\-_](\\d+))?(?:[.\\-_](\\d+))?([A-Za-z].*)?");

    public CommPortalVersion(String version)
    {
        Matcher match = COMMPORTAL_VERSION_REGEX.matcher(version);

        if (! match.matches())
        {
            throw new IllegalArgumentException("Unknown CommPortal version string format: " + version);
        }

        try
        {
            mMajor = Integer.parseInt(match.group(1));
            mMinor = Integer.parseInt(match.group(2));

            if (match.group(3) != null)
            {
                mRelease = Integer.parseInt(match.group(3));
            }
            else
            {
                mRelease = 0;
            }

            if (match.group(4) != null)
            {
                mMaintenance = Integer.parseInt(match.group(4));
            }
            else
            {
                mMaintenance = 0;
            }
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("Invalid CommPortal version string format: " + version, e);
        }

        mString = version;

        mSuffix = match.group(5);

        if (mSuffix == null)
        {
            mSuffix = "";
        }
    }

    @Override
    public int compareTo(CommPortalVersion other)
    {
        if (mMajor != other.mMajor)
        {
            return Integer.compare(mMajor, other.mMajor);
        }

        if (mMinor != other.mMinor)
        {
            return Integer.compare(mMinor, other.mMinor);
        }

        if (mRelease != other.mRelease)
        {
            return Integer.compare(mRelease, other.mRelease);
        }

        if (mMaintenance != other.mMaintenance)
        {
            return Integer.compare(mMaintenance, other.mMaintenance);
        }

        return mSuffix.compareTo(other.mSuffix);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if ((!(other instanceof CommPortalVersion)))
        {
            return false;
        }

        return (this.compareTo((CommPortalVersion) other) == 0);
    }

    @Override
    public int hashCode()
    {
        int hash = mMajor;
        hash = hash * 7 + mMinor;
        hash = hash * 29 + mRelease;
        hash = hash * 37 + mMaintenance;
        hash = hash * 17 + mSuffix.hashCode();
        return hash;
    }

    @Override
    public String toString()
    {
        return mString;
    }
}
