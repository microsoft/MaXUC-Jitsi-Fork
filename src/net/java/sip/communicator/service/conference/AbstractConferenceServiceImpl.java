// Copyright (c) Microsoft Corporation. All rights reserved.
// Highly Confidential Material
package net.java.sip.communicator.service.conference;

import java.util.Set;

import net.java.sip.communicator.plugin.desktoputil.ConfigurationPanel;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.protocol.CallConference;
import net.java.sip.communicator.service.protocol.ChatRoom;
import org.jitsi.util.CustomAnnotations.Nullable;

/**
 * An shell abstract implementation of the conference service.
 *
 * DO NOT REMOVE ANYTHING FROM THIS IMPLEMENTATION, YOU CAN ONLY ADD!
 * We have hidden plugin implementations which rely on this class, which
 * you may not have access to.
 */
public abstract class AbstractConferenceServiceImpl
        implements ConferenceService
{
    @Override
    public boolean isFullServiceEnabled()
    {
        // Default to false in the abstract case, as we need to be sure we have
        // a conference server that we can talk to for the specific
        // implementation.
        return false;
    }

    @Override
    public boolean isJoinEnabled()
    {
        // Default to false
        return false;
    }

    @Override
    public boolean isConfAppInstalled()
    {
        // Default to false
        return false;
    }

    @Override
    public void createOrAdd(final boolean openOtherTab)
    {
        // Do nothing
    }

    @Override
    public void createOrAdd(CallConference callConference, boolean createImmediately)
    {
        // Do nothing
    }

    @Override
    public void createOrAdd(ChatRoom chatRoom, boolean createImmediately)
    {
        // Do nothing
    }

    @Override
    public void createOrAdd(MetaContact contact, boolean createImmediately)
    {
        // Do nothing
    }

    @Override
    public void createOrAdd(Set<MetaContact> contacts, CallConference call)
    {
        // Do nothing
    }

    @Override
    public boolean canCallBeUplifted(CallConference callConference)
    {
        // Default to false
        return false;
    }

    @Override
    public boolean canChatRoomBeUplifted(ChatRoom chatRoom)
    {
        // Default to false
        return false;
    }

    @Override
    public boolean canContactBeUplifted(MetaContact contact)
    {
        // Default to false
        return false;
    }

    @Override
    public boolean isConferenceCreated()
    {
        // Default to false
        return false;
    }

    @Override
    public boolean isConferenceStarted()
    {
        // Default to false
        return false;
    }

    @Override
    public boolean isConferenceJoined()
    {
        // Default to false
        return false;
    }

    @Override
    public void setRecordingsDirectory(String recordingsPath)
    {
        // Do nothing
    }

    @Override
    public void showScheduledConferences()
    {
        // Do nothing
    }

    @Override
    public void showRecordedConferences()
    {
        // Do nothing
    }

    @Override
    public void showJoinConferenceDialog()
    {
        // Do nothing
    }

    @Override
    public void showScheduler()
    {
        // Do nothing
    }

    @Override
    public void bringConferenceToForeground()
    {
        // Do nothing
    }

    @Override
    public void showAdvancedSettings()
    {
        // Do nothing
    }

    @Override
    public void showWebinarManagement()
    {
        // Do nothing
    }

    @Override
    public ConfigurationPanel createConfigPanel()
    {
        // Default to null
        return null;
    }

    @Override
    public void quitMeetingClient(@Nullable MeetingClientQuitCallback callback, boolean clearMeetingData)
    {
        // Do nothing
    }

    @Override
    public String getConfId()
    {
        // Default to empty string
        return "";
    }

    @Override
    public boolean isRinging()
    {
        return false;
    }

    @Override
    public void addConferencesChangedListener(ConferencesChangedListener listener)
    {
        // Do nothing
    }

    @Override
    public void removeConferencesChangedListener(ConferencesChangedListener listener)
    {
        // Do nothing
    }

    @Override
    public void addConferenceRingingStateListener(ConferenceRingingStateListener listener)
    {
        // Do nothing
    }

    @Override
    public void removeConferenceRingingStateListener(ConferenceRingingStateListener listener)
    {
        // Do nothing
    }

    @Override
    public void addConferenceFailureListener(ConferenceFailureListener listener)
    {
        // Do nothing
    }

    @Override
    public void removeConferenceFailureListener(ConferenceFailureListener listener)
    {
        // Do nothing
    }

    @Override
    public void notifyListenersRingingStateChanged()
    {
        // Do nothing
    }

    @Override
    public void cancelMeeting()
    {
        // Do nothing
    }
}
