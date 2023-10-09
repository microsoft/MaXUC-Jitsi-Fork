package net.java.sip.communicator.service.protocol;

import static net.java.sip.communicator.util.PrivacyUtils.sanitiseFilePath;

import java.beans.*;
import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.neomedia.MediaException;
import org.jitsi.service.neomedia.Recorder;
import org.jitsi.service.packetlogging.PacketLoggingService;
import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.util.SoundFileUtils;

import net.java.sip.communicator.plugin.desktoputil.ErrorDialog;
import net.java.sip.communicator.plugin.desktoputil.NotificationInfo;
import net.java.sip.communicator.service.wispaservice.*;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.FileUtils;
import net.java.sip.communicator.util.Logger;

public class CallRecording implements PropertyChangeListener, Recorder.Listener
{
    /**
     * The logger used by the <tt>RecordButton</tt> class and its instances for
     * logging output.
     */
    private static final Logger logger =
                                     Logger.getLogger(CallRecording.class);

    /**
     * WISPA service.
     */
    private static final WISPAService mWispaService = ProtocolProviderActivator.getWispaService();

    /**
     * Configuration service.
     */
    private static final ConfigurationService configuration = ProtocolProviderActivator.getConfigurationService();

    /**
    * Resource service.
    */
    private static final ResourceManagementService resources = ProtocolProviderActivator.getResourceService();

    /** Call that is being recorded */
    private final Call mCall;

    /** Recording state */
    private boolean mIsRecording;

    /**
     * The string to be used in the filename to identify the peer(s) in the call
     * (e.g. the phone number, the string 'MultiPartyCall', or 'Anonymous'.)
     */
    private String mPeerIdentifier;

    /**
     * The <tt>Recorder</tt> whose purpose is to record {@link #mCall} into {@link #callFilename}.
     */
    private Recorder mRecorder;

    /**
     * The full filename of the saved call on the file system.
     */
    private String callFilename;

    /**
     * The temporary file used for writing call audio before we know where
     * recordings are to be saved (the user could change the target folder in
     * the options menu before recording completed).
     */
    private File mTempFile;

    /**
     * The timer used to schedule timeout of recording if it goes on for too
     * long.
     */
    private Timer timeoutTimer = new Timer("Recording timeout timer");

    /**
     * The conference underlying this call at the time recording began.
     * This can change if the call is transferred to another conference, so
     * we track the original value here.
     */
    private CallConference mCallConference;

    /**
     * The name of the <tt>CallConference</tt> property which specifies the list
     * of <tt>Call</tt>s participating in a telephony conference. A change in
     * the value of the property is delivered in the form of a
     * <tt>PropertyChangeEvent</tt> which has its <tt>oldValue</tt> or
     * <tt>newValue</tt> set to the <tt>Call</tt> which has been removed or
     * added to the list of <tt>Call</tt>s participating in the telephony
     * conference.
     */
    public static final String CALLS_PROPERTY = "calls";

    /**
     * The name of the <tt>Call</tt> property which represents its telephony
     * conference-related state.
     */
    public static final String CONFERENCE_PROPERTY = "conference";

    /**
     * The text of the error dialog shown when we fail to start call recording.
     */
    private static final String CALL_RECORDING_START_FAILED_TEXT =
        resources.getI18NString("service.gui.CALL_RECORDING_START_FAILED_TEXT");

    /**
     * The title of the error dialog shown when we fail to start call recording.
     */
    private static final String CALL_RECORDING_START_FAILED_TITLE =
        resources.getI18NString("service.gui.CALL_RECORDING_START_FAILED_TITLE");

    /**
     * The text of the error dialog shown when we fail to end call recording.
     */
    private static final String CALL_RECORDING_STOP_FAILED_TEXT =
        resources.getI18NString("service.gui.CALL_RECORDING_STOP_FAILED_TEXT");

    /**
     * The title of the error dialog shown when we fail to end call recording.
     */
    private static final String CALL_RECORDING_STOP_FAILED_TITLE =
        resources.getI18NString("service.gui.CALL_RECORDING_STOP_FAILED_TITLE");

    /**
     * The title of the toast shown when a call recording is successfully saved.
     */
    private static final String CALL_RECORDING_SAVED_SUCCESSFULLY_TITLE =
               resources.getI18NString("service.gui.CALL_RECORDING_SAVE_TITLE");

    /**
     * The maximum length of time we permit a recording to last.
     * Default is 2 hours.
     */
    private static final int MAX_RECORDING_TIME_MILLIS =
        configuration.global().getInt("net.java.sip.communicator.impl.gui.main.call." +
                                 "MAX_RECORDING_TIME_MILLIS", 2 * 3600 * 1000);

    /**
     * The title of the dialog that shows when the recording times out.
     */
    private static final String RECORDING_TIMEOUT_DIALOG_TITLE =
            resources.getI18NString("service.gui.CALL_RECORDING_TIMEOUT_TITLE");

    /**
     * The text of the dialog that shows when the recording times out.
     */
    private static final String RECORDING_TIMEOUT_DIALOG_TEXT =
             resources.getI18NString("service.gui.CALL_RECORDING_TIMEOUT_TEXT");

    /**
     * The string used in the filename of the saved audio to indicate that the
     * call was with multiple parties.
     */
    private static final String MULTI_PARTY_CALL =
                        resources.getI18NString("service.gui.MULTI_PARTY_CALL");

    /**
     * The string used in the filename of the saved audio to indicate that the
     * call was with a party whose identity could not be determined.
     */
    private static final String UNKNOWN =
                            resources.getI18NString("service.gui.UNKNOWN_PEER");

    /**
     * The string used in the filename of the saved audio to indicate that the
     * call was with a party whose identity was withheld.
     */
    private static final String ANONYMOUS =
                               resources.getI18NString("service.gui.ANONYMOUS");

    /**
     * The date format used in file names.
     */
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

    // TODO REFRESH-CALL-UI: Rather than a static map in this class, with instances of
    // this class exposed to other services, it would be cleaner if we just expose an
    // interface to start/stop recording and isRecording to other services and have a
    // "CallRecordingManager" or "CallRecordingService" that handles that for us.
    private static HashMap<String, CallRecording> sCalls = new HashMap<>();

    public static CallRecording getOrCreateCallRecording(Call call)
    {
        String callId = call.getCallID();
        if (sCalls.containsKey(callId))
        {
            return sCalls.get(callId);
        }
        CallRecording callRecording = new CallRecording(call);
        sCalls.put(callId, callRecording);
        return callRecording;
    }

    private CallRecording(Call call)
    {
        mCall = call;
        mIsRecording = false;
    }

    /** Returns whether the call is being recorded */
    public boolean isRecording()
    {
        return mIsRecording;
    }

    /**
     * Starts/stops the recording of the call according to a request
     */
    public void toggleRecording(boolean requestedIsRecording)
    {
        logger.debug("Toggle recording");
        if (mCall != null)
        {
            logger.debug("mCall not null");
            // Start recording if we have been asked to, and we aren't already recording
            if (requestedIsRecording && !mIsRecording)
            {
                logger.debug("Not currently recording");
                try
                {
                    // Attempt to start recording
                    logger.debug("Starting recording");
                    mIsRecording = startRecording();
                    mWispaService.notify(WISPANamespace.ACTIVE_CALLS, WISPAAction.UPDATE, mCall);
                }
                finally
                {
                    // If attempt to start recording failed, call stopRecording to tidy up
                    if (!mIsRecording && (mRecorder != null))
                    {
                        logger.debug("Stopping recording");
                        stopRecording();
                    }
                }
            }
            // Stop recording if we have been asked to, and we are already recording
            else if (!requestedIsRecording && mIsRecording)
            {
                logger.debug("Currently recording; stopping recording");
                stopRecording();
            }
            else
            {
                logger.debug("Already in requested recording state, do nothing");
            }
        }
    }

    /**
     * Stops the current recording session.
     */
    private void stopRecording()
    {
        logger.debug("Stopping recording for call " + mCall.getCallID());
        if (mRecorder == null)
        {
            logger.debug("Recorder alerady null, do nothing");
            return;
        }

        // Create a new thread, as we could hit NPEs
        new Thread("Recorder stopper")
        {
            public void run()
            {
                logger.debug("Stopping recording in new thread");

                // Check recorder gain as it might be null. In fact, we can still
                // hit NPEs in the really small edge condition. But there is
                // probably no impact as this thread can die with no big impact.
                if (mRecorder != null)
                {
                    mRecorder.stop();
                    handleStop();
                }
            }
        }.start();

        // Tell the packet logging service that any media buffering for diagnostic
        // purposes should now be stopped.
        PacketLoggingService packetLoggingService = ProtocolProviderActivator.getPacketLoggingService();
        packetLoggingService.stopRecording();
    }

    /**
     * Handles the event that call recording is stopped
     */
    private void handleStop()
    {
        logger.info("Call recording requested to stop for call " +
                    mCall.getCallID());

        if (!mIsRecording)
        {
            // Ensure that this method isn't called twice for a single recording
            // session (e.g. if the user presses 'stop' just as the recording
            // was being ended by a call merge).
            logger.debug("Call recording for call " + mCall.getCallID() +
                         "asked to stop when had already finished.");
            return;
        }

        timeoutTimer.cancel();
        timeoutTimer = new Timer("Recording timeout timer");
        mIsRecording = false;
        mWispaService.notify(WISPANamespace.ACTIVE_CALLS, WISPAAction.UPDATE, mCall);

        if (mRecorder != null)
        {
            mRecorder = null;
        }

        mCall.removePropertyChangeListener(this);
        mCallConference.removePropertyChangeListener(this);

        logger.info("Call recording stopped for call " + mCall.getCallID());

        publishRecording();
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        if (CALLS_PROPERTY.equals(evt.getPropertyName()))
        {
            if (evt.getOldValue() == null)
            {
                // A call has been added to the conference (i.e. a call has
                // been merged into this one) - so we stop recording.
                logger.info("New call merged; stopping recording of call " +
                            mCall.getCallID());
                mRecorder.stop();
            }
        }
        else if (CONFERENCE_PROPERTY.equals(evt.getPropertyName()))
        {
            // The conference underlying this call has changed (i.e. this call
            // has been merged into another) - so we stop recording.
            logger.info("Call " + mCall.getCallID() + " has changed " +
                         "conference, so stopping recording.");
                         mRecorder.stop();
        }
    }

    /**
     * Move the temporary file to the recorded calls folder, and give it the
     * correct name.
     */
    private void publishRecording()
    {
        // Recorded calls saved in e.g. <SAVED_CALLS_PATH>/Recorded Calls/ to
        // separate them from recorded meetings
        String recordingsPath = configuration.user().getString(
                                   Recorder.SAVED_CALLS_PATH,
                                   ConfigurationUtils.DEFAULT_SAVED_CALLS_PATH);
        String callsDirectory = resources.getI18NString("service.gui.RECORDED_CALLS");
        File savedCallsFolder = new File(recordingsPath + File.separator + callsDirectory);

        if (!savedCallsFolder.exists())
        {
            logger.debug("Call recording folder does " +
                         "not exist, so will now be created.");
            try
            {
                boolean success = savedCallsFolder.mkdirs();
                if (!success)
                {
                    logger.error("Unspecified failure when creating call " +
                                 "recording folder.");
                    showPublishingFailedDialog();
                }
            }
            catch (SecurityException e)
            {
                logger.error("Failed to create call recording folder.", e);
                showPublishingFailedDialog();
            }
        }

        String filename = generateCallFilename();
        File target = new File(savedCallsFolder.getPath() + File.separator + filename);

        try
        {
            Files.move(mTempFile.toPath(), target.toPath());
            showSavedNotification(filename, new File(savedCallsFolder.getPath()));
            logger.info("Recording for call " + mCall.getCallID() +
                        " saved to " + sanitiseFilePath(target.getAbsolutePath()));
        }
        catch (IOException e)
        {
            logger.debug("Failed to export call recording file "
                + sanitiseFilePath(mTempFile.getAbsolutePath())
                + " to destination "
                + sanitiseFilePath(target.getAbsolutePath()) + " for call "
                + mCall.getCallID(), e);
            showPublishingFailedDialog();
        }
    }

    /**
     * Show the 'failed to begin recording' error dialog.
     */
    private void showRecordingStartFailedDialog()
    {
        new ErrorDialog(CALL_RECORDING_START_FAILED_TITLE,
                        CALL_RECORDING_START_FAILED_TEXT).showDialog();
    }

    /**
     * Show the 'failed to save recording' error dialog.
     */
    private void showPublishingFailedDialog()
    {
        new ErrorDialog(CALL_RECORDING_STOP_FAILED_TITLE,
                        CALL_RECORDING_STOP_FAILED_TEXT).showDialog();
    }

    /**
     * Generates a file name for the call based on the current date and
     * the names of the peers in the call.
     *
     * @return the file name for the call
     */
    private String generateCallFilename()
    {
        String peerIdentifier = mPeerIdentifier;
        String date = FORMAT.format(new Date());

        return peerIdentifier + "_" + date + ".wav";
    }

    /**
     * Gets and formats the names of the peers in the call.
     *
     * @return the name of the peer in the call formated
     */
    private String getCallPeerIdentifier()
    {
        List<CallPeer> callPeers = mCall.getConference().getCallPeers();

        String peerIdentifier;
        if (mCall.getConference().getCallPeerCount() > 1)
        {
            peerIdentifier = MULTI_PARTY_CALL;
        }
        else if (callPeers.isEmpty())
        {
            peerIdentifier = UNKNOWN;
        }
        else
        {
            CallPeer callPeer = callPeers.get(0);

            if ("Anonymous".equalsIgnoreCase(callPeer.getDisplayName()))
            {
                peerIdentifier = ANONYMOUS;
            }
            else
            {
                peerIdentifier = callPeer.getAddress();

                if (peerIdentifier.indexOf('@') != -1)
                {
                    // If the address has the form usr@domain, we are only
                    // interested in 'usr'.
                    peerIdentifier =
                       peerIdentifier.substring(0, peerIdentifier.indexOf('@'));
                }

                peerIdentifier = peerIdentifier.replaceAll("[^0-9a-zA-Z]",
                                                           "");

                // Fallback if the peer identifier was *all* forbidden chars.
                if (peerIdentifier.equals(""))
                    peerIdentifier = UNKNOWN;
            }
        }

        return peerIdentifier;
    }

    /**
     * Gets the <tt>Recorder</tt>, creating it first if it does not exist.
     *
     * @return the <tt>Recorder</tt>
     * @throws OperationFailedException if anything goes wrong while creating
     * the <tt>Recorder</tt>
     */
    private Recorder getRecorder()
        throws OperationFailedException
    {
        if (mRecorder == null)
        {
            logger.debug("No existing recorder; " +
                         "creating a recorder for call " + mCall.getCallID());
            OperationSetBasicTelephony<?> telephony
                = mCall.getProtocolProvider().getOperationSet(
                        OperationSetBasicTelephony.class);

            mRecorder = telephony.createRecorder(mCall);
        }
        mRecorder.addListener(this);

        return mRecorder;
    }

    @Override
    public void recorderStarted(Recorder recorder)
    {
        // No action required
    }

    @Override
    public void recorderStopped(Recorder recorder)
    {
        logger.info("Notified that recording has stopped for " +
                    "call " + mCall.getCallID());
        handleStop();
    }

    /**
     * Starts recording {@link #mCall} creating {@link #recorder} first and
     * asking the user for the recording format and file if they are not
     * configured in the "Call Recording" configuration form.
     *
     * @return <tt>true</tt> if the recording has been started successfully;
     * otherwise, <tt>false</tt>
     */
    private boolean startRecording()
    {
        logger.info("Started recording call " + mCall.getCallID());

        mCallConference = mCall.getConference();

        mPeerIdentifier = getCallPeerIdentifier();

        mCall.addPropertyChangeListener(this);
        mCallConference.addPropertyChangeListener(this);

        Exception exception = null;

        String callFormat = SoundFileUtils.wav;

        try
        {
            mTempFile = getTempFile();
            logger.debug("Writing call recording for call " +
                         mCall.getCallID() +
                         " to temporary file " + mTempFile.getName());

            Recorder recorder = getRecorder();

            if (recorder != null)
            {
                recorder.start(callFormat, mTempFile.getAbsolutePath());
            }

            this.mRecorder = recorder;
        }
        catch (IOException | OperationFailedException | MediaException ioex)
        {
            exception = ioex;
        }

        boolean isRecording = true;

        if ((mRecorder == null) || (exception != null))
        {
            logger.error(
                    "Failed to start recording call " + mCall
                        + " into file " + callFilename,
                    exception);
            isRecording = false;
            showRecordingStartFailedDialog();
        }

        if (isRecording)
        {
            // End recording after a certain amount of time, to prevent the user
            // leaving recording turned on and using up all their disc space.

            TimerTask timeoutTask = new TimerTask()
            {
                public void run()
                {
                    logger.info("Recording for call " + mCall.getCallID() +
                                " timed out.");
                    showRecordingTimeoutDialog();
                    stopRecording();
                }
            };

            logger.debug("Recording for call " + mCall.getCallID() +
                         " will terminate in " + MAX_RECORDING_TIME_MILLIS +
                         "ms.");
            timeoutTimer.schedule(timeoutTask, MAX_RECORDING_TIME_MILLIS);

            // Tell the packet logging service that the call is being recorded so that
            // media packet capture can commence if the client's branding settings allow it.
            PacketLoggingService packetLoggingService = ProtocolProviderActivator.getPacketLoggingService();
            packetLoggingService.startRecording();
        }

        return isRecording;
    }

    /**
     * Shows the error dialog shown when recording times out.
     */
    private void showRecordingTimeoutDialog()
    {
        new ErrorDialog(RECORDING_TIMEOUT_DIALOG_TITLE,
                        RECORDING_TIMEOUT_DIALOG_TEXT).showDialog();
    }

    /**
     * Creates a temporary file to write call audio to, so that we can put off
     * moving it to the recorded calls folder until the last minute (the user
     * might change the recorded calls location during recording).
     *
     * @return A reference to the temporary file.
     * @throws IOException
     */
    private File getTempFile() throws IOException
    {
        ConfigurationService cfg = ProtocolProviderActivator.getConfigurationService();

        File path = new File(cfg.user().getScHomeDirLocation()
            + File.separator + cfg.user().getScHomeDirName()
            + File.separator);

        return File.createTempFile("call", ".wav", path);
    }

    /**
     * Show a toast to inform the user that the recording has been saved
     * successfully.
     *
     * @param filename The name of the file, to be displayed in the toast.
     * @param folderPath The path to the containing folder, so that it can be
     * opened when the toast is clicked.
     */
    private void showSavedNotification(String filename, final File folderPath)
    {
        String message = resources
            .getI18NString("service.gui.CALL_RECORDING_SAVE_TEXT", new String[]
            { filename });

        Runnable toastClickedAction = () -> {
            try
            {
                logger.user("'Finished recording' toast clicked on"
                    + "for call " + mCall.getCallID());
                FileUtils.openFileOrFolder(folderPath);
            }
            catch (IOException e)
            {
                logger.debug("Failed to open call recording folder at "
                    + sanitiseFilePath(folderPath.getAbsolutePath()) + " for call "
                    + mCall.getCallID(), e);
                new ErrorDialog(resources.getI18NString("service.gui.ERROR"),
                    resources.getI18NString("service.gui.FOLDER_OPEN_FAILED"))
                    .showDialog();
            }
        };

        if (mWispaService != null)
        {
            Object actionData = new WISPANotion(WISPANotionType.NOTIFICATION,
                new NotificationInfo(CALL_RECORDING_SAVED_SUCCESSFULLY_TITLE,
                    message, toastClickedAction));

            mWispaService.notify(WISPANamespace.EVENTS, WISPAAction.NOTION,
                actionData);
        }
    }
}