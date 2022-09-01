// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.conference;

import java.util.*;

import com.drew.lang.annotations.Nullable;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The interface over which conferences should be handled
 */
public interface ConferenceService
{
    /**
     * Returns true if this implementation of the conference service is
     * enabled, false otherwise.
     */
    boolean isFullServiceEnabled();

    /**
     * Returns true if the branding allows users to join meetings by ID.
     */
    boolean isJoinEnabled();

    /**
     * Returns true if the conference client is installed.
     */
    boolean isConfAppInstalled();

    /**
     * Create a conference from scratch
     * @param openOtherTab Decides which tab we should open on.
     */
    void createOrAdd(final boolean openOtherTab);

    /**
     * Uplift an existing call into a conference
     *
     * @param callConference represents the existing call
     * @param createImmediately If true, the conference will be created
     * immediately without prompting the user to select additional
     * participants.
     */
    void createOrAdd(CallConference callConference, boolean createImmediately);

    /**
     * Create a conference for a chat room
     *
     * @param chatRoom, the chat room to create the conference from.
     * @param createImmediately If true, the conference will be created
     * immediately without prompting the user to select additional
     * participants.
     */
    void createOrAdd(ChatRoom chatRoom, boolean createImmediately);

    /**
     * Create a conference with a single IM contact
     *
     * @param contact the contact to create the conference with
     * @param createImmediately If true, the conference will be created
     * immediately without prompting the user to select additional
     * participants.
     */
    void createOrAdd(MetaContact contact, boolean createImmediately);

    /**
     * Create a conference with a set of IM contacts.
     *
     * @param contacts The contact to create the conference with.
     */
    void createOrAdd(Set<MetaContact> contacts);

    /**
     * Create a conference with a set of IM contacts, transitioning audio from
     * an existing call.
     *
     * @param contacts The contact to create the conference with.
     * @param call Call being uplifted, that audio will transition from.
     */
    void createOrAdd(Set<MetaContact> contacts, CallConference call);

    /**
     * Returns true if the given call can be uplifted to a conference.
     *
     * @param callConference the call to be uplifted
     * @return true if the given call can be uplifted to a conference.
     */
    boolean canCallBeUplifted(CallConference callConference);

    /**
     * Returns true if the given chat room can be uplifted to a conference.
     *
     * @param chatRoom the chat room to be uplifted
     * @return true if the given chat room can be uplifted to a conference.
     */
    boolean canChatRoomBeUplifted(ChatRoom chatRoom);

    /**
     * Returns true if the given contact can be invited to a conference.
     *
     * @param contact the contact to be invited
     * @return true if the given contact can be invited to a conference.
     */
    boolean canContactBeUplifted(MetaContact contact);

    /**
     * Returns true if the user is currently in a conference or in the process
     * of creating a conference.
     *
     * @return true if a conference is currently in progress or the user is
     * creating a conference.
     */
    boolean isConferenceCreated();

    /**
     * Returns true if the user has started a conference, even if they haven't
     * yet joined
     *
     * @return true if the user has started a conference
     */
    boolean isConferenceStarted();

    /**
     * @return true if the user is in a conference, or is connecting to a
     * conference
     */
    //boolean isConferenceConnectingOrConnected();

    /**
     * Returns true if the user is in a conference.
     *
     * @return true if the user is in a conference.
     */
    boolean isConferenceJoined();

    /**
     * Sets where meeting recordings are saved.
     *
     * @param recordingsPath The parent directory of the recorded meetings directory.
     */
    void setRecordingsDirectory(String recordingsPath);

    /**
     * Shows the scheduled conferences page for this conference service.
     */
    void showScheduledConferences();

    /**
     * Shows the recorded conferences page for this conference service.
     */
    void showRecordedConferences();

    /**
     * Shows the join conference dialog box to allow user to join conference by
     * conference ID.
     */
    void showJoinConferenceDialog();

    /**
     * Shows the scheduler for this conference service.
     */
    void showScheduler();

    /**
     * Brings the conference back into the foreground if it is
     * minimised/in the background.
     */
    void bringConferenceToForeground();

    /**
     * If there is a meeting client, then exit it.
     */
    void exitMeetingClient();

    /**
     * Open the advanced settings webpage.
     */
    void showAdvancedSettings();

    /**
     * Open the webinar management webpage
     */
    void showWebinarManagement();

    /**
     * Returns config panel for the conference service.
     */
    ConfigurationPanel createConfigPanel();

    /**
     * Starts a request to quit the meeting client.
     *
     * @param callback Callback used when we have finished executing the code
     *                 to quit the meeting client.
     * @param clearMeetingData  true if we should also clear the meeting client data
     *                          when quitting (i.e. the user has asked to log out).
     */
    void quitMeetingClient(@Nullable MeetingClientQuitCallback callback, boolean clearMeetingData);

    /**
     * @param listener The listener that wants to be notified when the
     *                 user enters or leaves a conference.
     */
    void addConferencesListener(ConferencesListener listener);

    /**
     * @param listener The listener that no longer wants to be notified
     *                 when the user enters or leaves a conference.
     */
    void removeConferencesListener(ConferencesListener listener);

    /**
     * Get the ID of the current conference if there is one, otherwise
     * returns an empty string.
     */
    String getCurrentConferenceId();

    /** Interface for notifications whether we are in a conference. */
    interface ConferencesListener
    {
        /**
         * Called when the overall state of conferences changes.
         * @param newId New ID, empty indicates no conference.
         * @param oldId Old ID, empty indicates no conference.
         */
        void onConferencesChanged(String newId, String oldId);
    }

    /**
     * Interface used by callers of the quitMeetingClient method to indicate
     * that we have finished executing the code to quit the meeting client.
     */
    interface MeetingClientQuitCallback
    {
        /**
         * Called when we have finished executing the code to quit the meeting
         * client.
         *
         * @param cancelled if true, the user chose to cancelling quitting the
         *                  meeting client (e.g. because a meeting is in progress).
         */
        void onMeetingClientQuitComplete(boolean cancelled);
    }

    /**
     * Enum representing the different types of room system that can be invited
     * using room call-out
     */
    enum RoomSystemType
    {
        H323,
        SIP,
        BOTH
    }
}
