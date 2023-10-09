/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.*;
import java.util.*;
import java.util.Timer;

import javax.swing.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.packetlogging.PacketLoggingService;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.wispaservice.WISPAAction;
import net.java.sip.communicator.service.wispaservice.WISPANamespace;
import net.java.sip.communicator.service.wispaservice.WISPANotion;
import net.java.sip.communicator.service.wispaservice.WISPANotionType;
import net.java.sip.communicator.service.wispaservice.WISPAService;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.FileUtils;
import net.java.sip.communicator.util.Logger;
import static net.java.sip.communicator.util.PrivacyUtils.sanitiseFilePath;

/**
 * The button that starts/stops the call recording.
 *
 * @author Dmitri Melnikov
 * @author Lubomir Marinov
 */
public class RecordButton extends InCallButton
    implements PropertyChangeListener
{
    private static final long serialVersionUID = 0L;

    /**
     * The logger used by the <tt>RecordButton</tt> class and its instances for
     * logging output.
     */
    private static final Logger logger =
                                     Logger.getLogger(RecordButton.class);

    /**
     * Configuration service.
     */
    private static final ConfigurationService configuration
                                       = GuiActivator.getConfigurationService();

    /**
     * Resource service.
     */
    private static final ResourceManagementService resources
        = GuiActivator.getResources();

    private static final WISPAService wispaService = GuiActivator.getWISPAService();

    /**
     * The string used to label this menu item in the dropdown when recording
     * is not in progress.
     */
    private static final String RECORD_CALL =
                             resources.getI18NString("service.gui.RECORD_CALL");

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
     * The title of the error dialog shown if the 'Recording saved' toast is
     * clicked, but we fail to open the call recording folder.
     */
    private static final String FAILED_TO_OPEN_FOLDER_TITLE =
                                   resources.getI18NString("service.gui.ERROR");

    /**
     * The text of the error dialog shown if the 'Recording saved' toast is
     * clicked, but we fail to open the call recording folder.
     */
    private static final String FAILED_TO_OPEN_FOLDER_TEXT =
                      resources.getI18NString("service.gui.FOLDER_OPEN_FAILED");

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
     * The date format used in file names.
     */
    private static final SimpleDateFormat FORMAT
        = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

    /**
     * The characters we forbid for the telephone number part of the recorded
     * call filename.
     */
    private static final String FORBIDDEN_CHARACTERS = "[^0-9a-zA-Z]";

    /**
     * The timer used to schedule timeout of recording if it goes on for too
     * long.
     */
    private Timer timeoutTimer = new Timer("Recording timeout timer");

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
     * The <tt>Recorder</tt> which is depicted by this <tt>RecordButton</tt> and
     * which is to record or records {@link #mCall} into {@link #callFilename}.
     */
    private Recorder recorder;

    /**
     * The call associated with this menu item.
     */
    private final Call mCall;

    /**
     * The string to be used in the filename to identify the peer(s) in the call
     * (e.g. the phone number, the string 'MultiPartyCall', or 'Anonymous'.)
     */
    private String mPeerIdentifier;

    /**
     * The conference underlying this call at the time recording began.
     * This can change if the call is transferred to another conference, so
     * we track the original value here.
     */
    private CallConference mCallConference;

    /**
     * Whether the recorder is currently active for this call.
     */
    private boolean mIsRecording = false;

    /**
     * Initializes a new <tt>RecordButton</tt> instance which is to record the
     * audio stream.
     *
     * @param call the <tt>Call</tt> to be associated with the new instance and
     * to have the audio stream recorded
     */
    public RecordButton(Call call)
    {
        super(ImageLoaderService.RECORD_BUTTON,
              ImageLoaderService.RECORD_BUTTON_ROLLOVER,
              ImageLoaderService.RECORD_BUTTON_FOCUS,
              ImageLoaderService.RECORD_BUTTON_PRESSED,
              ImageLoaderService.RECORD_BUTTON_ON,
              ImageLoaderService.RECORD_BUTTON_ON_ROLLOVER,
              ImageLoaderService.RECORD_BUTTON_ON_FOCUS,
              null,
              RECORD_CALL);

        mCall = call;

        addActionListener(new ActionListener()
        {
           public void actionPerformed(ActionEvent evt)
           {
               logger.user("Record button clicked in call " +
                            mCall.getCallID());
               toggleRecording();
           }
        });
    }

    /**
     * Updates the UI and behaviour of this component to reflect the current
     * status of recording for this call.
     *
     * @param isRecording <tt>true</tt> if this call is currently being
     * recorded, and <tt>false</tt> otherwise.
     */
    private void setIsRecording(final boolean isRecording)
    {
        mIsRecording = isRecording;
        setSelected(isRecording);
    }

    /**
     * Starts/stops the recording of the call when this button is pressed.
     */
    public void toggleRecording()
    {
        logger.debug("Toggle recording");
        if (mCall != null)
        {
            logger.debug("mCall not null");
            // start recording
            if (!mIsRecording)
            {
                try
                {
                    logger.debug("Starting recording");
                    setIsRecording(startRecording());
                }
                finally
                {
                    if (!mIsRecording && (recorder != null))
                    {
                        logger.debug("Stopping recording");
                        stopRecording();
                    }
                }
            }
            // stop recording
            else
            {
                logger.debug("Stopping recording");
                stopRecording();
            }
        }
    }

    /**
     * Stops the current recording session.
     * This method should be used in preference to calling
     * <tt>recorder.stop()</tt>, since the recorder may be null if it was
     * stopped in another thread.
     */
    private void stopRecording()
    {
        logger.debug("Stop recording");
        if (recorder != null)
        {
            logger.debug("Still recording- stopping");
            stopRecordingOffEDTThread();
        }
    }

    /**
     * Stop recording off the EDT thread.  This is a good idea because calls
     * to stop have been seen to take a long time, or hang outright.
     */
    private void stopRecordingOffEDTThread()
    {
      logger.debug("Stopping recording for call " + mCall.getCallID());

      // Get off the EDT thread
      new Thread("Recorder stopper")
      {
          public void run()
          {
              logger.debug("Stopping recording off thread");

              // Check recorder gain- it might be null.  In fact, we can still
              // hit NPEs in the really small edge condition.  But there is
              // probably no impact as this thread can die with no big impact.
              if (recorder != null)
              {
                recorder.stop();
                handleStop();
              }
          }
      }.start();

      // Tell the packet logging service that any media buffering for diagnostic
      // purposes should now be stopped.
      PacketLoggingService packetLoggingService = GuiActivator.getPacketLoggingService();
      packetLoggingService.stopRecording();

    }

    public void recorderStarted(Recorder recorder)
    {
        // No action required; nothing else should be kicking off recording
        // except us!
    }

    public void recorderStopped(Recorder recorder)
    {
        logger.info("RecordButton notified that recording has stopped for " +
                    "call " + mCall.getCallID());
        handleStop();
    }

    /**
     * Handles the event that call recording is stopped by either ourselves or
     * a third party.
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
        setIsRecording(false);

        if (recorder != null)
        {
            recorder = null;
        }

        mCall.removePropertyChangeListener(this);
        mCallConference.removePropertyChangeListener(this);

        logger.info("Call recording stopped for call " + mCall.getCallID());

        publishRecording();
    }

    /**
     * Clean up before disposal of this object.
     */
    public void dispose()
    {
        logger.debug("Cleaning up RecordButton.");

        if ((recorder != null) && (mIsRecording))
        {
            logger.info("Still recording- stopping");
            stopRecordingOffEDTThread();
        }

        logger.debug("Canceling timer");
        timeoutTimer.cancel();
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
                recorder.stop();
            }
        }
        else if (CONFERENCE_PROPERTY.equals(evt.getPropertyName()))
        {
            // The conference underlying this call has changed (i.e. this call
            // has been merged into another) - so we stop recording.
            logger.info("Call " + mCall.getCallID() + " has changed " +
                         "conference, so stopping recording.");
            recorder.stop();
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
            Files.move(mTempFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            showSavedToast(filename, new File(savedCallsFolder.getPath()));
            logger.info("Recording for call " + mCall.getCallID() +
                        " saved.");
        }
        catch (IOException e)
        {
            logger.debug("Failed to export call recording file for call " +
                         mCall.getCallID(), e);
            showPublishingFailedDialog();
        }
    }

    /**
     * Show the 'failed to begin recording' error dialog.
     */
    private void showRecordingStartFailedDialog()
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                new ErrorDialog(CALL_RECORDING_START_FAILED_TITLE,
                    CALL_RECORDING_START_FAILED_TEXT).showDialog();
           }
        });
    }

    /**
     * Show the 'failed to save recording' error dialog.
     */
    private void showPublishingFailedDialog()
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                new ErrorDialog(CALL_RECORDING_STOP_FAILED_TITLE,
                    CALL_RECORDING_STOP_FAILED_TEXT).showDialog();
           }
        });
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

        String filename = peerIdentifier + "_" + date + ".wav";
        return  filename;
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

                peerIdentifier = peerIdentifier.replaceAll(FORBIDDEN_CHARACTERS,
                                                           "");

                // Fallback if the peer identifier was *all* forbidden chars.
                if (peerIdentifier.equals(""))
                    peerIdentifier = UNKNOWN;
            }
        }

        return peerIdentifier;
    }

    /**
     * Gets the <tt>Recorder</tt> represented by this <tt>RecordButton</tt>
     * creating it first if it does not exist.
     *
     * @return the <tt>Recorder</tt> represented by this <tt>RecordButton</tt>
     * created first if it does not exist
     * @throws OperationFailedException if anything goes wrong while creating
     * the <tt>Recorder</tt> to be represented by this <tt>RecordButton</tt>
     */
    private Recorder getRecorder()
        throws OperationFailedException
    {
        if (recorder == null)
        {
            logger.debug("RecordButton has no existing recorder attached; " +
                         "creating a recorder for call " + mCall.getCallID());
            OperationSetBasicTelephony<?> telephony
                = mCall.getProtocolProvider().getOperationSet(
                        OperationSetBasicTelephony.class);

            recorder = telephony.createRecorder(mCall);
        }

        return recorder;
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
            logger.debug("Writing call recording for call " + mCall.getCallID()
                + " to temporary file " + sanitiseFilePath(mTempFile.getAbsolutePath()));

            Recorder recorder = getRecorder();

            if (recorder != null)
            {
                recorder.start(callFormat, mTempFile.getAbsolutePath());
            }

            this.recorder = recorder;
        }
        catch (IOException | OperationFailedException | MediaException ioex)
        {
            exception = ioex;
        }

        boolean isRecording = true;

        if ((recorder == null) || (exception != null))
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
            PacketLoggingService packetLoggingService = GuiActivator.getPacketLoggingService();
            packetLoggingService.startRecording();
        }

        return isRecording;
    }

    /**
     * Shows the error dialog shown when recording times out.
     */
    private void showRecordingTimeoutDialog()
    {
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                    new ErrorDialog(RECORDING_TIMEOUT_DIALOG_TITLE,
                        RECORDING_TIMEOUT_DIALOG_TEXT).showDialog();
               }
            });
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
        ConfigurationService cfg = GuiActivator.getConfigurationService();

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
    private void showSavedToast(String filename, final File folderPath)
    {
        String message = resources.getI18NString(
               "service.gui.CALL_RECORDING_SAVE_TEXT", new String[] {filename});

        Runnable toastClickedAction = new Runnable()
        {
            public void run()
            {
               try
               {
                   logger.user("'Finished recording' toast clicked on" +
                                "for call " + mCall.getCallID());
                   FileUtils.openFileOrFolder(folderPath);
               }
               catch (IOException e)
               {
                    logger.debug("Failed to open call recording folder at "
                        + sanitiseFilePath(mTempFile.getAbsolutePath()) + " for call "
                        + mCall.getCallID(), e);
                   SwingUtilities.invokeLater(new Runnable()
                   {
                       public void run()
                       {
                            new ErrorDialog(FAILED_TO_OPEN_FOLDER_TITLE,
                                FAILED_TO_OPEN_FOLDER_TEXT).showDialog();
                       }
                   });
               }
            }
        };

        if (wispaService != null)
        {
            Object actionData = new WISPANotion(WISPANotionType.NOTIFICATION,
                new NotificationInfo(CALL_RECORDING_SAVED_SUCCESSFULLY_TITLE,
                    message, toastClickedAction));

            wispaService.notify(WISPANamespace.EVENTS, WISPAAction.NOTION,
                actionData);
        }
    }
}
