// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.neomedia;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.swing.*;
import javax.swing.border.*;

import org.jitsi.impl.neomedia.*;
import org.jitsi.service.audionotifier.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.device.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.*;

// N.B. This class should not begin with the word Test - we reserve that for
// test files that we do not ship with the product.
public class CheckMicrophoneContainer
    extends JPanel
    implements ActionListener
{
    /**
     * Serial version UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * The name of the directory in which to store the recorded audio sample.
     */
    private static final String RECORDED_SAMPLE_DIRECTORY =
        MediaService.RECORDED_SAMPLE_DIRECTORY;

    /**
     * The filename of the recorded sample.
     */
    private static final String RECORDED_SAMPLE_FILENAME =
        MediaService.RECORDED_SAMPLE_FILENAME;

    /**
     * The filename of the sample that is played back. It is a copy of the
     * original recorded file, necessary because files cannot be stopped and
     * then re-recorded in quick succession.
     */
    private static final String PLAYBACK_SAMPLE_FILENAME =
        RECORDED_SAMPLE_FILENAME + "_playback";

    /**
     * The file format of the recorded sample.
     */
    private static final String RECORDED_SAMPLE_EXTENSION =
        MediaService.RECORDED_SAMPLE_EXTENSION;

    /**
     * The <tt>MediaService</tt> implementation used by
     * CheckMicrophoneContainer.
     */
    private static final MediaServiceImpl sMediaService
        = NeomediaActivator.getMediaServiceImpl();

    /**
     * The <tt>Logger</tt> used by the <tt>CheckMicrophoneContainer</tt>
     * class for logging output.
     */
    private static final Logger sLogger
        = Logger.getLogger(CheckMicrophoneContainer.class);

    /**
     * The duration of the recording timer, in units of 50 milliseconds.
     */
    private static final int SAMPLE_RECORDING_LENGTH = 100;

    //Scaled UI dimensions for the recording and playback controls.
    private static final int PLAY_BUTTON_WIDTH = ScaleUtils.scaleInt(25);
    private static final int TEST_MIC_BUTTON_WIDTH = ScaleUtils.scaleInt(107);
    private static final int BUTTON_BORDER_WIDTH = ScaleUtils.scaleInt(14);

    private static final Dimension PLAY_BUTTON_DIMENSION =
        new Dimension(PLAY_BUTTON_WIDTH, PLAY_BUTTON_WIDTH);
    private static final Dimension TEST_MIC_BUTTON_DIMENSION =
        new Dimension(TEST_MIC_BUTTON_WIDTH, PLAY_BUTTON_WIDTH);
    private static final Dimension TEST_MIC_CONTAINER_DIMENSION =
        new Dimension(TEST_MIC_BUTTON_WIDTH + PLAY_BUTTON_WIDTH +
            3*BUTTON_BORDER_WIDTH, PLAY_BUTTON_WIDTH);
    private static final Border BUTTON_BORDER =
        BorderFactory.createEmptyBorder(0, BUTTON_BORDER_WIDTH, 0, 0);

    /**
     * Indicates whether audio is currently being recorded from the microphone.
     */
    private boolean mRecording = false;

    /**
     * Indicates whether the first playback of the recorded sample has started.
     */
    private boolean mInitialPlaybackStarted = false;

    /**
     * The RecorderImpl instance used to record audio from the microphone.
     */
    private RecorderImpl mRecorder;

    /**
     * The recorded audio sample.
     */
    private SCAudioClip mRecordedSample;

    /**
     * The listener registered to the audio sample.
     */
    private AudioPlaybackListener mRecordedSampleListener;

    /**
     * The button used to start recording in order to test the microphone.
     */
    private JButton mTestMicButton;

    /**
     * The progress bar used to count down when recording audio.
     */
    private RecordingTimer mRecordingTimer;

    /**
     * The button used to stop recording of the microphone.
     */
    private JButton mStopRecordingButton;

    /**
     * The button used to play back the recorded sample from the microphone.
     */
    private JButton mPlayRecordedSampleButton;

    /**
     * The full path to a copy of the recorded sample used for playback.
     */
    private String mPlaybackSamplePath;

    /**
     * The full path of the recorded sample, the file written to by the recorder.
     */
    private String mRecordedSamplePath;

    private final Object mSampleLock = new Object();

    /**
     * Constructs (but does not paint) all UI elements of the Check Microphone container.
     */
    CheckMicrophoneContainer()
    {
        mTestMicButton = new JButton();
        ScaleUtils.scaleFontAsDefault(mTestMicButton);
        mTestMicButton.addActionListener(this);
        mTestMicButton.setMaximumSize(TEST_MIC_BUTTON_DIMENSION);
        mTestMicButton.setOpaque(false);

        mRecordingTimer = new RecordingTimer();

        mStopRecordingButton = NeomediaActivator.getResources().getBufferedImage(
                "plugin.mediaconfig.STOP_ICON")
            .getImageIcon()
            .addToButton(new JButton());
        mStopRecordingButton.setMinimumSize(PLAY_BUTTON_DIMENSION);
        mStopRecordingButton.setPreferredSize(PLAY_BUTTON_DIMENSION);
        mStopRecordingButton.setOpaque(false);
        mStopRecordingButton.addActionListener(this);
        AccessibilityUtils.setName(
            mStopRecordingButton,
            NeomediaActivator.getResources().getI18NString(
                     "plugin.notificationconfig.STOP_BUTTON_NAME"));

        mPlayRecordedSampleButton = new JButton();
        mPlayRecordedSampleButton.setMinimumSize(PLAY_BUTTON_DIMENSION);
        mPlayRecordedSampleButton.setPreferredSize(PLAY_BUTTON_DIMENSION);
        mPlayRecordedSampleButton.setOpaque(false);
        mPlayRecordedSampleButton.addActionListener(this);
        AccessibilityUtils.setName(
            mPlayRecordedSampleButton,
            NeomediaActivator.getResources().getI18NString(
                     "plugin.notificationconfig.SAMPLE_PLAY_BUTTON_NAME"));

        setOpaque(false);
        setLayout(new BorderLayout());
        setMaximumSize(TEST_MIC_CONTAINER_DIMENSION);
        setPreferredSize(TEST_MIC_CONTAINER_DIMENSION);
    }

    public void actionPerformed(ActionEvent e)
    {
        // If the test Mic button is pressed.
        sLogger.user("Test mic button clicked");
        if (e.getSource() == mTestMicButton)
        {
            synchronized (mSampleLock)
            {
                if (mRecordedSample != null)
                {
                    mRecordedSample.stop();
                }
            }

            try
            {
                mRecordingTimer.start();
                paintAndValidate();
            }
            catch (MediaException | IOException ex)
            {
                sLogger.error("Could not start recording to " +
                    mRecordedSamplePath);
            }
        }

        // If the stop recording button is pressed.
        else if (e.getSource() == mStopRecordingButton)
        {
            mRecordingTimer.stop();
        }

        // If the 'play recorded sample button' is pressed.
        else if (e.getSource() == mPlayRecordedSampleButton)
        {
            synchronized (mSampleLock)
            {
                if (mRecordedSample.isStarted())
                {
                    mRecordedSample.stop();
                }
                else
                {
                    mRecordedSample.playEvenIfMuted();
                }
            }
        }
    }

    /**
     * Repaints the Check Mic Container to correspond to current recording state.
     */
    void paintAndValidate()
    {
        removeAll();

        JPanel smallButtonPanel = new JPanel(new BorderLayout());
        smallButtonPanel.setOpaque(false);
        smallButtonPanel.setBorder(BUTTON_BORDER);

        JPanel bigButtonPanel = new JPanel(new BorderLayout());
        bigButtonPanel.setOpaque(false);
        bigButtonPanel.setBorder(BUTTON_BORDER);

        synchronized (mSampleLock)
        {
            if (mRecording)
            {
                if (mInitialPlaybackStarted)
                {
                    smallButtonPanel.add(mPlayRecordedSampleButton, BorderLayout.CENTER);
                }
                else
                {
                    smallButtonPanel.add(mStopRecordingButton, BorderLayout.CENTER);
                }

                bigButtonPanel.add(mRecordingTimer, BorderLayout.CENTER);
                add(smallButtonPanel, BorderLayout.LINE_START);
                add(bigButtonPanel, BorderLayout.CENTER);
            }
            else if (mRecordedSample != null)
            {
                mTestMicButton.setText(
                    NeomediaActivator.getResources().
                    getI18NString("impl.neomedia.configform.RETEST_MIC"));
                mTestMicButton.setEnabled(true);
                smallButtonPanel.add(mPlayRecordedSampleButton, BorderLayout.CENTER);
                bigButtonPanel.add(mTestMicButton, BorderLayout.CENTER);
                add(smallButtonPanel, BorderLayout.LINE_START);
                add(bigButtonPanel, BorderLayout.CENTER);
            }
            else
            {
                mTestMicButton.setText(
                    NeomediaActivator.getResources().
                    getI18NString("impl.neomedia.configform.TEST_MIC"));
                bigButtonPanel.add(mTestMicButton, BorderLayout.CENTER);
                add(bigButtonPanel, BorderLayout.LINE_START);
            }
        }

        repaint();
        revalidate();
    }

    /**
     * Returns a copy of recorded sample as an SCAudioClip.
     *
     * @return the recorded sample audio clip.
     * @throws IOException if sample copy cannot be created.
     */
    private SCAudioClip getRecordedSample()
        throws IOException
    {
        // If a sound clip is stopped prematurely, it cannot be immediately
        // overwritten. Therefore to allow playback and recording in quick
        // succession, we make a copy of the recorded sample to be used for playback.

        File playbackFile = new File(mPlaybackSamplePath);
        File recordedSample = new File(mRecordedSamplePath);
        Files.copy(recordedSample.toPath(), playbackFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        String playbackPath = playbackFile.toURI().toString();
        SCAudioClip audio = NeomediaActivator.getAudioNotifierService()
            .createAudio(playbackPath, true);

        mRecordedSampleListener = new AudioPlaybackListener(mPlayRecordedSampleButton)
        {
            @Override
            public void onClipEnded()
            {
                super.onClipEnded();

                // If the initial playback has ended, change to Playback UI.
                if (mInitialPlaybackStarted)
                {
                    mInitialPlaybackStarted = false;
                    mRecording = false;
                    paintAndValidate();
                }
            }
        };

        audio.registerAudioListener(mRecordedSampleListener);
        return audio;
    }

    /**
     * Sets the Test Mic functionality to either enabled or disabled.
     *
     * @param recorderSuccess True to enable Test Mic button, otherwise false.
     */
    public void enableTestMic(Boolean recorderSuccess)
    {
        mTestMicButton.setEnabled(recorderSuccess);
    }

    /**
     * Called when the audio settings panel is exited. Cancels all recording and playback.
     */
    public void onDismissed()
    {
        if (mRecording)
        {
            mRecordingTimer.abort();
        }
        if (mRecordedSample != null)
        {
            mRecordedSample.stop();
            mRecordedSample.removeAudioListener(mRecordedSampleListener);
        }
    }

    /**
     * Creates the microphone recorder.
     *
     * @return success flag. Returns true if microphone and directory created
     * successfully, false otherwise.
     */
    public boolean createRecorder()
    {
        // Break out of recording and clear previous recorded sample:
        if (mRecording)
        {
            mRecordingTimer.abort();
            mRecording = false;
        }

        synchronized (mSampleLock)
        {
            if (mRecordedSample != null)
            {
                mRecordedSample.stop();
            }

            mRecordedSample = null;
        }

        // Attempt to find directory for storing recording:
        try
        {
            File sample = UtilActivator.getFileAccessService()
                .getPrivatePersistentDirectory(RECORDED_SAMPLE_DIRECTORY);
            mRecordedSamplePath =
                sample.toString() + File.separator + RECORDED_SAMPLE_FILENAME +
                "." + RECORDED_SAMPLE_EXTENSION;
            mPlaybackSamplePath =
                sample.toString() + File.separator + PLAYBACK_SAMPLE_FILENAME +
                "." + RECORDED_SAMPLE_EXTENSION;
        }
        catch (Exception ex)
        {
            sLogger.error("Unable to find suitable directory for storing recorded sample.");
            return false;
        }

        MediaDevice device = sMediaService.getDefaultDevice(
            MediaType.AUDIO, MediaUseCase.CALL);
        if (device != null)
        {
            try
            {
                MediaDevice mixer = sMediaService.createMixer(device);
                mRecorder = (RecorderImpl) sMediaService.createRecorder(mixer);
            }
            catch (IllegalArgumentException ex)
            {
                // Thrown by createMixer if device cannot record Audio.
                sLogger.error("Input device is unable to capture audio: " + device);
                mRecorder = null;
            }
        }
        else
        {
            mRecorder = null;
        }

        //If mixer or recorder are unsuccessfully created, recorder will be null.
        if (mRecorder == null)
        {
            sLogger.error("Unable to find create recorder from microphone: " + device);
            return false;
        }

        return true;
    }

    /**
     * The timer which starts and stops the recorder, and displays a progress
     * bar which fills up during recording.
     */
    private class RecordingTimer
        extends JProgressBar
    {
        /**
         * Serial version UID
         */
        private static final long serialVersionUID = 1L;

        /**
         * Counter which is incremented from 0 to SAMPLE_RECORDING_LENGTH
         */
        private int counter;

        /**
         * Timer which increments counter every 50 milliseconds.
         */
        private Timer timer = new Timer(50, new ActionListener()
        {
            public void actionPerformed(ActionEvent ev)
            {
                setValue(++counter);
                if (counter > SAMPLE_RECORDING_LENGTH)
                {
                    stop();
                }
            }
        });

        /**
         * Creates a timer and progress bar with horizontal orientation,
         * minimum = 0 and maximum = SAMPLE_RECORDING_LENGTH.
         */
        private RecordingTimer()
        {
            super(JProgressBar.HORIZONTAL, 0, SAMPLE_RECORDING_LENGTH);
            setStringPainted(true);
        }

        /**
         * Starts the timer, progress bar and recorder.
         *
         * @throws org.jitsi.service.neomedia.MediaException
         * @throws IOException
         */
        private synchronized void start() throws IOException, org.jitsi.service.neomedia.MediaException
        {
            counter = 0;
            setStringPainted(true);
            setString( NeomediaActivator.getResources()
                .getI18NString("impl.neomedia.configform.SAMPLE_RECORDING"));
            repaint();
            timer.start();
            mRecorder.start(RECORDED_SAMPLE_EXTENSION, mRecordedSamplePath);
            mRecording = true;
        }

        /**
         * Stops the timer, progress bar and recorder; starts playing recorded sample
         * and repaints progress bar to indicate audio playback has started.
         */
        private synchronized void stop()
        {
            timer.stop();
            mRecorder.stop();
            try
            {
                mRecordedSample = getRecordedSample();
                if (!mRecordedSample.testRender())
                {
                    throw new IOException("Recorded sample cannot be rendered.");
                }
                mRecordedSample.playEvenIfMuted();
                mInitialPlaybackStarted = true;
                setValue(SAMPLE_RECORDING_LENGTH);
                setString( NeomediaActivator.getResources()
                    .getI18NString("impl.neomedia.configform.SAMPLE_PLAYING"));
                repaint();
                paintAndValidate();
            }
            catch (IOException ex)
            {
                sLogger.error("Cannot play recorded sample.");
                mRecording = false;
                NeomediaActivator.getResources().getBufferedImage(
                    "plugin.mediaconfig.PLAY_ICON").getImageIcon().
                    addToButton(mPlayRecordedSampleButton);
                paintAndValidate();
            }
        }

        /**
         * Cancels the timer without triggering playback of recorded sample.
         */
        private synchronized void abort()
        {
            timer.stop();
            mRecorder.stop();
        }
    }
}
