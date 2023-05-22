// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.update;

import java.util.Objects;

import org.jitsi.util.CustomAnnotations.*;

/**
 * This class contains the information that an update contains. The properties are serialized via protobuf
 * to be passed on to the Electron application. The properties determine how our dialog will be rendered.
 * The fields in this class must not be null.
 */
public class UpdateInfo
{
    private String latestVersion;
    private String currentVersion;
    private UpdateState updateState;
    /**
     * @param latestVersion string representation of latest available version of app
     * @param currentVersion string representation of current version of app
     * @param updateState state of the update can be UP_TO_DATE | UPDATE_FORCED | UPDATE_OPTIONAL | ERROR
     */
    public UpdateInfo(@NotNull String latestVersion,
                      @NotNull String currentVersion,
                      @NotNull UpdateState updateState)
    {
        Objects.requireNonNull(latestVersion);
        Objects.requireNonNull(currentVersion);
        Objects.requireNonNull(updateState);

        this.latestVersion = latestVersion;
        this.currentVersion = currentVersion;
        this.updateState = updateState;
    }

    public String getLatestVersion()
    {
        return latestVersion;
    }

    public String getCurrentVersion()
    {
        return currentVersion;
    }

    public UpdateState getUpdateState()
    {
        return updateState;
    }

    public String toString()
    {
        return latestVersion + " " + currentVersion + " " + updateState;
    }
}
