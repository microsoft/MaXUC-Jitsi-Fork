// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.conference;

import java.util.Set;

import net.java.sip.communicator.plugin.desktoputil.ConfigurationPanel;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.protocol.CallConference;
import net.java.sip.communicator.service.protocol.ChatRoom;
import org.jitsi.util.CustomAnnotations.Nullable;

/**
 * The interface over which conferences should be handled
 */
public interface ConferenceService
{
    /**
     * Returns true if the conference invite is ringing
     */
    boolean isRinging();

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
     * Returns true if the user is in a conference or in the process
     * of starting a conference.
     *
     * @return true if a conference is currently in progress or the user is
     * starting a conference.
     */
    boolean isConferenceStarted();

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
    void addConferencesChangedListener(ConferencesChangedListener listener);

    /**
     * @param listener The listener that no longer wants to be notified
     *                 when the user enters or leaves a conference.
     */
    void removeConferencesChangedListener(ConferencesChangedListener listener);

    /**
     * @param listener The listener that wants to be notified when the
     *                 ringing state occurs.
     */
    void addConferenceRingingStateListener(ConferenceRingingStateListener listener);

    /**
     * @param listener The listener that no longer wants to be notified
     *                 when the ringing state occurs.
     */
    void removeConferenceRingingStateListener(ConferenceRingingStateListener listener);

    /**
     * @param listener The listener that wants to be notified when the
     *                 failure occurs.
     */
    void addConferenceFailureListener(ConferenceFailureListener listener);

    /**
     * @param listener The listener that no longer wants to be notified
     *                 when the user enters or leaves a conference.
     */
    void removeConferenceFailureListener(ConferenceFailureListener listener);

    /**
     * Get the ID of the conference we are currently either in or in the process,
     * of creating, if there is one, otherwise returns an empty string.
     */
    String getConfId();

    /**
     * Resend the ringing state to the UI.
     */
    void notifyListenersRingingStateChanged();

    void cancelMeeting();

    /** Interface for notifications whether we are in a conference. */
    interface ConferencesChangedListener
    {
        /**
         * Called when the overall state of conferences changes.
         */
        void onConferencesChanged();
    }

    interface ConferenceRingingStateListener
    {
        /**
         * Called when the ringing state changes.
         */
        void onRingingStateChanged();
    }

    interface ConferenceFailureListener
    {
        /**
         * Called when the meeting invite is rejected
         * @param chatAddress is the other party
         */
        void onInviteRejection(String chatAddress);

        /**
         * Called when the meeting invite fails
         * @param chatAddress is the other party
         */
        void onInviteFailure(String chatAddress);
    }

    /**
     * Interface used by callers of the quitMeetingClient method to indicate
     * that we have finished executing the code to quit the meeting client.
     */
    interface MeetingClientQuitCallback
    {
        /**
         * Called when we have finished executing the code to quit the meeting
         * Called when we have finished executing the code to quit the meeting
         * client.
         *
         * @param cancelled if true, the user chose to cancelling quitting the
         *                  meeting client (e.g. because a meeting is in progress).
         */
        void onMeetingClientQuitComplete(boolean cancelled);
    }
}
