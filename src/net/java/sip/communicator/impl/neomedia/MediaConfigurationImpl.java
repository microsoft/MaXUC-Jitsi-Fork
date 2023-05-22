/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.neomedia;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.media.Buffer;
import javax.media.CaptureDeviceInfo;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.swing.*;
import javax.swing.border.Border;

import net.java.sip.communicator.plugin.desktoputil.ConfigSectionPanel;
import net.java.sip.communicator.plugin.desktoputil.ScaleUtils;
import net.java.sip.communicator.plugin.desktoputil.ScaledDimension;
import net.java.sip.communicator.plugin.desktoputil.SoundLevelIndicator;
import net.java.sip.communicator.plugin.desktoputil.StyledHTMLEditorPane;
import net.java.sip.communicator.plugin.desktoputil.TransparentPanel;
import net.java.sip.communicator.plugin.desktoputil.volumecontrol.VolumeSlider;
import net.java.sip.communicator.service.gui.ConfigurationContainer;
import net.java.sip.communicator.service.headsetmanager.HeadsetManagerService;
import net.java.sip.communicator.service.headsetmanager.HeadsetManagerService.HeadsetResponseState;
import net.java.sip.communicator.util.AccessibilityUtils;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import org.jitsi.impl.neomedia.HardwareVolumeControl;
import org.jitsi.impl.neomedia.MediaServiceImpl;
import org.jitsi.impl.neomedia.device.AudioMediaDeviceImpl;
import org.jitsi.impl.neomedia.device.AudioMediaDeviceSession;
import org.jitsi.impl.neomedia.device.AudioSystem;
import org.jitsi.impl.neomedia.device.CaptureDeviceListManager;
import org.jitsi.impl.neomedia.device.DataFlow;
import org.jitsi.impl.neomedia.device.MediaDeviceImpl;
import org.jitsi.impl.neomedia.device.MediaDeviceSession;
import org.jitsi.impl.neomedia.device.NoneAudioSystem;
import org.jitsi.impl.neomedia.device.NotifyDeviceListManager;
import org.jitsi.impl.neomedia.device.PlaybackDeviceListManager;
import org.jitsi.impl.neomedia.device.VideoDeviceListManager;
import org.jitsi.service.audionotifier.SCAudioClip;
import org.jitsi.service.neomedia.BasicVolumeControl;
import org.jitsi.service.neomedia.MediaConfigurationService;
import org.jitsi.service.neomedia.MediaDirection;
import org.jitsi.service.neomedia.MediaService;
import org.jitsi.service.neomedia.MediaType;
import org.jitsi.service.neomedia.MediaUseCase;
import org.jitsi.service.neomedia.device.MediaDevice;
import org.jitsi.service.neomedia.event.SimpleAudioLevelListener;
import org.jitsi.service.neomedia.event.VolumeChangeEvent;
import org.jitsi.service.neomedia.event.VolumeChangeListener;
import org.jitsi.util.OSUtils;
import org.jitsi.util.swing.VideoContainer;

/**
 * Implements <tt>MediaConfigurationService</tt> i.e. represents a factory of
 * user interface which allows the user to configure the media-related
 * functionality of the application.
 *
 * @author Lyubomir Marinov
 * @author Damian Minkov
 * @author Yana Stamcheva
 * @author Boris Grozev
 * @author Vincent Lucas
 */
public class MediaConfigurationImpl
    implements ActionListener,
               MediaConfigurationService,
               VolumeChangeListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>MediaConfigurationServiceImpl</tt>
     * class for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(MediaConfigurationImpl.class);

    /**
     * The <tt>MediaService</tt> implementation used by
     * <tt>MediaConfigurationImpl</tt>.
     */
    private static final MediaServiceImpl mediaService
        = NeomediaActivator.getMediaServiceImpl();

    /**
     * The name of the property for the sound file used to test the playback and
     * the notification devices.
     */
    private static final String TEST_SOUND_FILENAME_PROP
        = "net.java.sip.communicator.impl.neomedia.TestSoundFilename";

    /**
     * The name of the sound file used to test the playback and the notification
     * devices.
     */
    private static final String testSoundFilename =
        NeomediaActivator.getConfigurationService().global().getString(
            TEST_SOUND_FILENAME_PROP,
            NeomediaActivator.getResources().getSoundPath("TEST_SOUND"));

    /**
     * The color used for text in hint panels.
     */
    private static final Color HINT_TEXT_COLOR =
        new Color(NeomediaActivator.getResources().getColor(
             "service.gui.MID_TEXT"));

    private static final Color TEXT_COLOR =
        ConfigurationUtils.useNativeTheme() ? Color.BLACK :
            new Color(NeomediaActivator.getResources().getColor(
                "service.gui.DARK_TEXT"));

    /**
     * The preferred width of all panels.
     */
    private static final int WIDTH = 350;

    // Scaled UI dimensions for the audio system controls.
    private static final int PLAY_BUTTON_WIDTH = ScaleUtils.scaleInt(25);
    private static final int BUTTON_BORDER_WIDTH = ScaleUtils.scaleInt(14);
    private static final int AUDIO_CONTROLS_BORDER_WIDTH = ScaleUtils.scaleInt(30);
    private static final int AUDIO_COMBOBOX_WIDTH = ScaleUtils.scaleInt(300);
    private static final int VIDEO_COMBOBOX_WIDTH = ScaleUtils.scaleInt(280);
    private static final int COMBOBOX_HEIGHT = ScaleUtils.scaleInt(23);
    private static final int HEADSET_RESPONSE_COMBOBOX_WIDTH = ScaleUtils.scaleInt(200);
    private static final int HEADSET_RESPONSE_TITLE_BORDER_WIDTH = ScaleUtils.scaleInt(14);

    // Spacing between the audio panel sections.  Without this they run into
    // each other.
    private static final int SECTION_BORDER_HEIGHT = ScaleUtils.scaleInt(15);

    private static final Dimension PLAY_BUTTON_DIMENSION =
        new Dimension(PLAY_BUTTON_WIDTH, PLAY_BUTTON_WIDTH);
    private static final Dimension AUDIO_COMBOBOX_DIMENSION =
        new Dimension(AUDIO_COMBOBOX_WIDTH, COMBOBOX_HEIGHT);
    private static final Dimension VIDEO_COMBOBOX_DIMENSION =
        new Dimension(VIDEO_COMBOBOX_WIDTH, COMBOBOX_HEIGHT);
    private static final Dimension HEADSET_RESPONSE_COMBOBOX_DIMENSION =
        new Dimension(HEADSET_RESPONSE_COMBOBOX_WIDTH, COMBOBOX_HEIGHT);

    private static final Border AUDIO_CONTROLS_BORDER =
        BorderFactory.createEmptyBorder(
            0, AUDIO_CONTROLS_BORDER_WIDTH, 0, AUDIO_CONTROLS_BORDER_WIDTH);
    private static final Border PLAY_BUTTON_BORDER =
        BorderFactory.createEmptyBorder(0, BUTTON_BORDER_WIDTH, 0, 0);
    private static final Border HEADSET_RESPONSE_TITLE_BORDER =
        BorderFactory.createEmptyBorder(0, 0, 0, HEADSET_RESPONSE_TITLE_BORDER_WIDTH);
    private static final Border SECTION_BORDER =
        BorderFactory.createEmptyBorder(0, 0, SECTION_BORDER_HEIGHT, 0);

    private ConfigSectionPanel mMicrophonePanel;
    private ConfigSectionPanel mInCallAudioPanel;
    private ConfigSectionPanel mNotificationsPanel;
    private ConfigSectionPanel mHeadsetResponsePanel;

    /**
     * Creates the video container.
     * @param noVideoComponent the container component.
     * @return the video container.
     */
    private static JComponent createVideoContainer(Component noVideoComponent)
    {
        return new VideoContainer(noVideoComponent);
    }

    /**
     * Creates preview for the (video) device in the video container.
     *
     * @param device the device
     * @param videoContainer the video container
     */
    private static void createVideoPreviewInContainer(
            CaptureDeviceInfo device,
            JComponent videoContainer)
    {
        logger.debug("Device: " + device);

        videoContainer.removeAll();
        videoContainer.revalidate();
        videoContainer.repaint();

        if (device == null)
        {
            return;
        }

        for (MediaDevice mediaDevice
                : mediaService.getDevices(MediaType.VIDEO, MediaUseCase.ANY))
        {
            if (((MediaDeviceImpl) mediaDevice).getCaptureDeviceInfo().equals(
                    device))
            {
                Dimension videoContainerSize =
                        videoContainer.getPreferredSize();
                Component preview =
                    (Component) mediaService.getVideoPreviewComponent(
                        mediaDevice,
                        videoContainerSize.width,
                        videoContainerSize.height);

                if (preview != null)
                {
                    videoContainer.add(preview);
                }

                break;
            }
        }
    }

    /**
     * The thread which updates the capture device as selected by the user. This
     * prevent the UI to lock while changing the device.
     */
    private AudioLevelListenerThread audioLevelListenerThread = null;

    /**
     * The slider used to control the volume of the audio playback device.
     */
    private VolumeSlider playbackSlider;

    /**
     * The slider used to control the volume of the audio notify device.
     */
    private VolumeSlider notifySlider;

    /**
     * The combo box used to selected the microphone device.
     */
    private JComboBox<Object> captureCombo;

    /**
     * The combo box used to selected the notification device.
     */
    private JComboBox<Object> playbackCombo;

    /**
     * The combo box used to selected the notification device.
     */
    private JComboBox<Object> notifyCombo;

    /**
     * The combo box used to select when we respond to heasdset button presses
     */
    private JComboBox<String> headsetResponseCombo;

    /**
     * The button used to play a sound in order to test playback device.
     */
    private JButton playbackPlaySoundButton;

    /**
     * The button used to play a sound in order to test notification devices.
     */
    private JButton notifyPlaySoundButton;

    /**
     * The container holding all UI elements for testing the microphone.
     */
    private CheckMicrophoneContainer testMicContainer;

    // The audio clips played when testing the playback and notify devices.
    private SCAudioClip mPlaybackSound;
    private SCAudioClip mNotifySound;

    // The listeners registered to the audio clips.
    private AudioPlaybackListener mPlaybackListener;
    private AudioPlaybackListener mNotifyListener;

    /**
     * The currently selected Audio Capture device.
     */
    private MediaDevice mSelectedCaptureDevice;

    /**
     * Indicates that one of the contained in this panel buttons has been
     * clicked.
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        // If the user clicked on the playback play sound button.
        if (e.getSource() == playbackPlaySoundButton)
        {
            logger.user("Playback play sound button clicked");
            synchronized (mPlaybackSound)
            {
                if (mPlaybackSound.isStarted())
                {
                    mPlaybackSound.stop();
                }
                else
                {
                    mPlaybackSound.playEvenIfMuted();
                }
            }
        }

        // If the user clicked on the notify play sound button.
        else if (e.getSource() == notifyPlaySoundButton)
        {
            logger.user("Notify play sound button clicked");
            synchronized (mNotifySound)
            {
                if (mNotifySound.isStarted())
                {
                    mNotifySound.stop();
                }
                else
                {
                    mNotifySound.playEvenIfMuted();
                }
            }
        }

        // If the selected item of the playback or notify combobox has changed.
        else if (e.getSource() == playbackCombo
                || e.getSource() == notifyCombo)
        {
            @SuppressWarnings("unchecked")
            DeviceConfigurationComboBoxModel.CaptureDevice device
                = (DeviceConfigurationComboBoxModel.CaptureDevice)
                    ((JComboBox<Object>) e.getSource()).getSelectedItem();

            boolean isEnabled = (device != null) && (device.getInfo() != null);
            if (e.getSource() == playbackCombo)
            {
                playbackPlaySoundButton.setEnabled(isEnabled);
            }
            else
            {
                notifyPlaySoundButton.setEnabled(isEnabled);
            }
        }
    }

    /**
     * Returns the audio configuration panel.
     *
     * @return the audio configuration panel
     */
    public Component createAudioConfigPanel()
    {
        return createControls(MediaType.AUDIO);
    }

    /**
     * Creates the UI controls which are to control the details of a specific
     * <tt>AudioSystem</tt>.
     *
     * @param audioSystem the <tt>AudioSystem</tt> for which the UI controls to
     * control its details are to be created
     * @param container the <tt>JComponent</tt> into which the UI controls which
     * are to control the details of the specified <tt>AudioSystem</tt> are to
     * be added
     */
    private void createAudioSystemControls(
            AudioSystem audioSystem,
            JComponent container)
    {
        logger.debug("Create audio system controls for " + audioSystem);
        container.setBorder(AUDIO_CONTROLS_BORDER);

        int audioSystemFeatures = audioSystem.getFeatures();
        boolean featureNotifyAndPlaybackDevices
            = ((audioSystemFeatures
                    & AudioSystem.FEATURE_NOTIFY_AND_PLAYBACK_DEVICES)
                != 0);

        mMicrophonePanel = new ConfigSectionPanel("impl.media.configform.AUDIO_IN");
        container.add(mMicrophonePanel);

        if (featureNotifyAndPlaybackDevices)
        {
            // Set the section header to match the available function (where 'calls' is
            // considered to cover both telephony and meetings.
            String audioOutSectionHeading = "impl.media.configform.AUDIO_OUT_MEETINGS";
            if (ConfigurationUtils.isCallingEnabled())
            {
                audioOutSectionHeading = "impl.media.configform.AUDIO_OUT";
            }

            mInCallAudioPanel = new ConfigSectionPanel(audioOutSectionHeading);
            container.add(mInCallAudioPanel);

            if (ConfigurationUtils.isCallingOrImEnabled())
            {
                logger.debug("Add Notifications panel");
                mNotificationsPanel = new ConfigSectionPanel("impl.media.configform.AUDIO_NOTIFY");
                container.add(mNotificationsPanel);
            }

            mHeadsetResponsePanel = new ConfigSectionPanel(null);
            container.add(mHeadsetResponsePanel);
        }

        // Microphone components:
        SoundLevelIndicator capturePreview = new SoundLevelIndicator(
                SimpleAudioLevelListener.MIN_LEVEL,
                SimpleAudioLevelListener.MAX_LEVEL);

        BasicVolumeControl captureVolumeControl =
            (BasicVolumeControl) mediaService.getCaptureVolumeControl();
        final VolumeSlider captureSlider = new VolumeSlider(captureVolumeControl);
        mMicrophonePanel.add(captureSlider);

        mMicrophonePanel.add(capturePreview);

        if (featureNotifyAndPlaybackDevices)
        {
            addCaptureComponents(captureSlider);
            addPlaybackComponents();
            addNotifyComponents();
            addHeadsetResponseComponents();
        }

        if (audioLevelListenerThread == null)
        {
            audioLevelListenerThread
                = new AudioLevelListenerThread(
                        audioSystem,
                        captureCombo,
                        capturePreview);
        }
        else
        {
            audioLevelListenerThread.init(
                    audioSystem,
                    captureCombo,
                    capturePreview);
        }
    }

    /**
     * Add the components to the microphone panel
     * @param captureSlider the volume slider
     */
    private void addCaptureComponents(VolumeSlider captureSlider)
    {
        mSelectedCaptureDevice = mediaService.getDefaultDevice(MediaType.AUDIO, MediaUseCase.CALL);

        captureCombo = new JComboBox<>();
        ScaleUtils.scaleFontAsDefault(captureCombo);
        captureCombo.setEditable(false);
        DeviceConfigurationComboBoxModel captureModel =
                new DeviceConfigurationComboBoxModel(
                        mediaService.getDeviceConfiguration(),
                        DeviceConfigurationComboBoxModel.AUDIO_CAPTURE,
                        CaptureDeviceListManager.PROP_DEVICE,
                        captureCombo)
                {
                    @Override
                    protected void fireContentsChanged(int index0, int index1)
                    {
                        super.fireContentsChanged(index0, index1);

                        MediaDevice newDevice = mediaService.
                            getDefaultDevice(MediaType.AUDIO, MediaUseCase.CALL);

                        if (!newDevice.equals(mSelectedCaptureDevice))
                        {
                            mSelectedCaptureDevice = newDevice;

                            logger.info("Device changed... refreshing volume slider "
                                + "and remaking recorder.");
                            captureSlider.refresh();

                            boolean recorderSuccess;
                            if (testMicContainer != null)
                            {
                                recorderSuccess = testMicContainer.createRecorder();
                            }
                            else
                            {
                                recorderSuccess = false;
                            }

                            testMicContainer.enableTestMic(recorderSuccess);
                            testMicContainer.paintAndValidate();
                        }
                    }
                };

        captureCombo.setModel(captureModel);
        captureCombo.setRenderer(new DeviceConfigListCellRenderer(
                captureCombo.getRenderer(), captureModel));
        captureCombo.addKeyListener(captureModel);
        captureCombo.setPreferredSize(AUDIO_COMBOBOX_DIMENSION);
        captureCombo.setMaximumSize(AUDIO_COMBOBOX_DIMENSION);
        captureCombo.setOpaque(false);

        testMicContainer = new CheckMicrophoneContainer();
        ScaleUtils.scaleFontAsDefault(testMicContainer);
        Boolean recorderSuccess = testMicContainer.createRecorder();
        testMicContainer.enableTestMic(recorderSuccess);
        testMicContainer.paintAndValidate();

        JPanel captureComboPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        captureComboPanel.setOpaque(false);
        captureComboPanel.add(captureCombo);
        captureComboPanel.add(testMicContainer);
        captureComboPanel.setBorder(SECTION_BORDER);
        mMicrophonePanel.add(captureComboPanel);

    }

    /**
     * Add the components to the playback panel
     */
    private void addPlaybackComponents()
    {
        HardwareVolumeControl playbackVolumeControl =
            (HardwareVolumeControl) mediaService.getPlaybackVolumeControl();
        playbackSlider = new VolumeSlider(playbackVolumeControl);
        playbackVolumeControl.addVolumeChangeListener(this);
        mInCallAudioPanel.add(playbackSlider);

        playbackCombo = new JComboBox<>();
        ScaleUtils.scaleFontAsDefault(playbackCombo);
        playbackCombo.setEditable(false);
        DeviceConfigurationComboBoxModel playbackModel =
                new DeviceConfigurationComboBoxModel(
                        mediaService.getDeviceConfiguration(),
                        DeviceConfigurationComboBoxModel.AUDIO_PLAYBACK,
                        PlaybackDeviceListManager.PROP_DEVICE,
                        playbackCombo)
                {
                    @Override
                    public void setSelectedItem(Object item)
                    {
                        logger.info("Device changed... refreshing volume slider.");
                        super.setSelectedItem(item);
                        playbackSlider.refresh();
                    }
                };
        playbackCombo.setModel(playbackModel);
        playbackCombo.setRenderer(new DeviceConfigListCellRenderer(
                playbackCombo.getRenderer(), playbackModel));
        playbackCombo.addActionListener(this);
        playbackCombo.addKeyListener(playbackModel);
        playbackCombo.setPreferredSize(AUDIO_COMBOBOX_DIMENSION);
        playbackCombo.setMaximumSize(AUDIO_COMBOBOX_DIMENSION);
        AccessibilityUtils.setDescription(
                playbackCombo,
                NeomediaActivator.getResources().getI18NString("impl.neomedia.configform.PLAYBACK_DESCRIPTION"));

        // Playback play sound button.
        playbackPlaySoundButton = NeomediaActivator.getResources()
                .getBufferedImage(
                        "plugin.mediaconfig.PLAY_ICON")
                .getImageIcon()
                .addToButton(new JButton());
        playbackPlaySoundButton.setMinimumSize(PLAY_BUTTON_DIMENSION);
        playbackPlaySoundButton.setPreferredSize(PLAY_BUTTON_DIMENSION);

        mPlaybackSound = NeomediaActivator.getAudioNotifierService()
            .createAudio(testSoundFilename,true);
        mPlaybackListener = new AudioPlaybackListener(playbackPlaySoundButton);
        mPlaybackSound.registerAudioListener(mPlaybackListener);

        DeviceConfigurationComboBoxModel.CaptureDevice playbackCaptureDevice =
                (DeviceConfigurationComboBoxModel.CaptureDevice) playbackCombo.getSelectedItem();

        if (playbackCaptureDevice == null || playbackCaptureDevice.getInfo() == null)
        {
            logger.debug("No playback device: " + playbackCaptureDevice);
            playbackPlaySoundButton.setEnabled(false);
        }

        playbackPlaySoundButton.setOpaque(false);
        playbackPlaySoundButton.addActionListener(this);
        AccessibilityUtils.setName(
                 playbackPlaySoundButton,
                 NeomediaActivator.getResources().getI18NString(
                             "plugin.notificationconfig.PLAY_BUTTON_NAME"));

        JPanel playbackComboPanel = new TransparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        playbackComboPanel.setOpaque(false);
        playbackComboPanel.add(playbackCombo);
        JPanel playbackPlayButtonPanel = new JPanel(new BorderLayout());
        playbackPlayButtonPanel.setOpaque(false);
        playbackPlayButtonPanel.setBorder(PLAY_BUTTON_BORDER);
        playbackPlayButtonPanel.add(playbackPlaySoundButton, BorderLayout.CENTER);
        playbackComboPanel.add(playbackPlayButtonPanel);
        playbackComboPanel.setBorder(SECTION_BORDER);
        mInCallAudioPanel.add(playbackComboPanel);
    }

    /**
     * Add the components to the notify panel if it exists
     */
    private void addNotifyComponents()
    {
        if (mNotificationsPanel != null)
        {
            HardwareVolumeControl notifyVolumeControl =
                (HardwareVolumeControl) mediaService.getNotifyVolumeControl();
            notifySlider = new VolumeSlider(notifyVolumeControl);
            notifyVolumeControl.addVolumeChangeListener(this);
            mNotificationsPanel.add(notifySlider);

            notifyCombo = new JComboBox<>();
            ScaleUtils.scaleFontAsDefault(notifyCombo);
            notifyCombo.setEditable(false);
            DeviceConfigurationComboBoxModel notifyModel =
                new DeviceConfigurationComboBoxModel(
                        mediaService.getDeviceConfiguration(),
                        DeviceConfigurationComboBoxModel.AUDIO_NOTIFY,
                        NotifyDeviceListManager.PROP_DEVICE,
                        notifyCombo)
                {
                    @Override
                    public void setSelectedItem(Object item)
                    {
                        logger.info("Device changed... refreshing volume slider.");
                        super.setSelectedItem(item);
                        notifySlider.refresh();
                    }
                };
            notifyCombo.setModel(notifyModel);
            notifyCombo.addKeyListener(notifyModel);
            notifyCombo.setRenderer(new DeviceConfigListCellRenderer(
                notifyCombo.getRenderer(), notifyModel));
            notifyCombo.addActionListener(this);
            notifyCombo.setPreferredSize(AUDIO_COMBOBOX_DIMENSION);
            notifyCombo.setMaximumSize(AUDIO_COMBOBOX_DIMENSION);
            AccessibilityUtils.setDescription(
                notifyCombo,
                NeomediaActivator.getResources().getI18NString("impl.neomedia.configform.NOTIFY_DESCRIPTION"));

            // Notification play sound button.
            notifyPlaySoundButton
                = NeomediaActivator.getResources()
                    .getBufferedImage(
                        "plugin.mediaconfig.PLAY_ICON")
                        .getImageIcon()
                        .addToButton(new JButton());
            notifyPlaySoundButton.setMinimumSize(PLAY_BUTTON_DIMENSION);
            notifyPlaySoundButton.setPreferredSize(PLAY_BUTTON_DIMENSION);

            mNotifySound = NeomediaActivator.getAudioNotifierService()
                .createAudio(testSoundFilename,false);
            mNotifyListener = new AudioPlaybackListener(notifyPlaySoundButton);
            mNotifySound.registerAudioListener(mNotifyListener);

            DeviceConfigurationComboBoxModel.CaptureDevice notifyCaptureDevice =
                (DeviceConfigurationComboBoxModel.CaptureDevice) notifyCombo.getSelectedItem();

            if(notifyCaptureDevice == null || notifyCaptureDevice.getInfo() == null)
            {
                logger.debug("No notify device: " + notifyCaptureDevice);
                notifyPlaySoundButton.setEnabled(false);
            }

            notifyPlaySoundButton.setOpaque(false);
            notifyPlaySoundButton.addActionListener(this);
            AccessibilityUtils.setName(
                        notifyPlaySoundButton,
                        NeomediaActivator.getResources().getI18NString(
                                 "plugin.notificationconfig.PLAY_BUTTON_NAME"));

            JPanel notifyComboPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            notifyComboPanel.setOpaque(false);
            notifyComboPanel.add(notifyCombo);
            JPanel notifyPlayButtonPanel = new JPanel(new BorderLayout());
            notifyPlayButtonPanel.setOpaque(false);
            notifyPlayButtonPanel.setBorder(PLAY_BUTTON_BORDER);
            notifyPlayButtonPanel.add(notifyPlaySoundButton);
            notifyComboPanel.add(notifyPlayButtonPanel, BorderLayout.LINE_END);
            mNotificationsPanel.add(notifyComboPanel);
        }
    }

    /**
     * Add the components to the headset response panel
     */
    private void addHeadsetResponseComponents()
    {
        boolean unsupportedMacVersion = OSUtils.IS_MAC &&
            !NeomediaActivator.getHeadsetManager().headsetIntegrationSupported();

        JPanel headsetResponseComboPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        headsetResponseComboPanel.setOpaque(false);
        JTextArea headsetResponseTitle = new JTextArea();
        headsetResponseTitle.setText(NeomediaActivator.getResources().
            getI18NString("impl.media.configform.RESPOND_TO_BUTTON_PRESSES"));
        headsetResponseTitle.setForeground(TEXT_COLOR);
        ScaleUtils.scaleFontAsDefault(headsetResponseTitle);
        headsetResponseTitle.setEditable(false);
        headsetResponseTitle.setOpaque(false);
        headsetResponseTitle.setBorder(HEADSET_RESPONSE_TITLE_BORDER);

        headsetResponseCombo = new JComboBox<>();
        ScaleUtils.scaleFontAsDefault(headsetResponseCombo);
        headsetResponseCombo.setEditable(false);

        ComboBoxModel<String> headsetModel = new DefaultComboBoxModel<>(
                new String[]{HeadsetResponseState.ALWAYS.toString(),
                        HeadsetResponseState.NEVER.toString(),
                        HeadsetResponseState.WHILE_UNLOCKED.toString()});

        headsetResponseCombo.setModel(headsetModel);

        headsetResponseCombo.setRenderer(new DefaultListCellRenderer()
        {
            private static final long serialVersionUID = 0L;

            @Override
            public Component getListCellRendererComponent(JList<?> list,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus)
            {
                // Override the default renderer so that we display a
                // sensible message rather than the option that is stored
                String displayString = NeomediaActivator.getResources().
                    getI18NString("impl.media.configform." + value);
                return super.getListCellRendererComponent(list,
                                                          displayString,
                                                          index,
                                                          isSelected,
                                                          cellHasFocus);
            }
        });

        headsetResponseCombo.setMaximumSize(HEADSET_RESPONSE_COMBOBOX_DIMENSION);

        if (!unsupportedMacVersion)
        {
            String selectedItem = ConfigurationUtils.getHeadsetResponse();
            headsetResponseCombo.setSelectedItem(selectedItem);
            headsetResponseCombo.addItemListener(new HeadsetResponseComboListener());
        }
        else
        {
            // If the user is using a Mac that is an unsupported version:
            // - headset response combo should be disabled
            // - headset response combo should be set to the response state to
            //   use when an unsupported OS is used.
            headsetResponseCombo.setEnabled(false);
            headsetResponseCombo.setSelectedItem(HeadsetManagerService.
                UNSUPPORTED_OS_HEADSET_RESPONSE.toString());
        }

        headsetResponseComboPanel.add(headsetResponseTitle);
        headsetResponseComboPanel.add(headsetResponseCombo);
        mHeadsetResponsePanel.add(headsetResponseComboPanel);

        StyledHTMLEditorPane headsetHintText = new StyledHTMLEditorPane();

        // If the user is using a Mac that is an unsupported version,
        // hint text should show explanation that the Mac version is not
        // supported
        String headsetHintResource = unsupportedMacVersion ?
                "impl.media.configform.RESPOND_TO_BUTTON_PRESSES_HINT_UNSUPPORTED_MAC" :
                "impl.media.configform.RESPOND_TO_BUTTON_PRESSES_HINT";
        headsetHintText.setText(NeomediaActivator.getResources().getI18NString(headsetHintResource));

        headsetHintText.setForeground(HINT_TEXT_COLOR);
        ScaleUtils.scaleFontAsDefault(headsetHintText);
        headsetHintText.setContentType("text/html");
        headsetHintText.setOpaque(false);
        headsetHintText.setEditable(false);
        headsetHintText.setBorder(SECTION_BORDER);

        mHeadsetResponsePanel.add(headsetHintText);
    }

    /**
     * Listens to changes to the headset response combo box
     */
    public static class HeadsetResponseComboListener
        implements ItemListener
    {
        @Override
        public void itemStateChanged(ItemEvent e)
        {
            if (e.getStateChange() == ItemEvent.SELECTED)
            {
                String newItemValue = (String)e.getItem();
                logger.user("Headset response selection changed to: " +
                                    newItemValue);
                ConfigurationUtils.setHeadsetResponse(newItemValue);
                NeomediaActivator.getHeadsetManager().headsetResponseStateChanged(HeadsetResponseState.valueOf(newItemValue));
            }
        }
    }

    /**
     * Creates the UI controls which are to control the details of a specific
     * <tt>VideoSystem</tt>.
     *
     * @param deviceAndPreviewPanel the <tt>JComponent</tt> into which the UI controls which
     * are to control the details of the specified <tt>VideoSystem</tt> are to
     * be added
     * @param devicePanel
     * @return The JComboBox for selecting a video device.
     */
    private JComboBox<Object> createVideoSystemControls(
            JPanel deviceAndPreviewPanel, Container devicePanel)
    {
        logger.debug("Create video system controls");

        JComboBox<Object> deviceComboBox = new JComboBox<>();
        deviceComboBox.setEditable(false);
        DeviceConfigurationComboBoxModel comboBoxModel =
                new DeviceConfigurationComboBoxModel(
                        mediaService.getDeviceConfiguration(),
                        DeviceConfigurationComboBoxModel.VIDEO,
                        VideoDeviceListManager.PROP_DEVICE,
                        deviceComboBox);
        deviceComboBox.setModel(comboBoxModel);

        deviceComboBox.addKeyListener(comboBoxModel);
        deviceComboBox.setRenderer(new DeviceConfigListCellRenderer(
                deviceComboBox.getRenderer(), comboBoxModel));
        ScaleUtils.scaleFontAsDefault(deviceComboBox);
        deviceComboBox.setPreferredSize(VIDEO_COMBOBOX_DIMENSION);
        deviceComboBox.setMaximumSize(VIDEO_COMBOBOX_DIMENSION);
        deviceComboBox.setOpaque(false);

        String description =
            NeomediaActivator.getResources().getI18NString("impl.neomedia.configform.VIDEO_DESCRIPTION");

        AccessibilityUtils.setDescription(deviceComboBox, description);

        JLabel deviceLabel = new JLabel(
            NeomediaActivator.getResources().getI18NString("impl.media.configform.VIDEO"));
        ScaleUtils.scaleFontAsDefault(deviceLabel);

        deviceLabel.setDisplayedMnemonic(
            NeomediaActivator.getResources().getI18nMnemonic(
                "impl.media.configform.VIDEO"));
        deviceLabel.setLabelFor(deviceComboBox);

        devicePanel.setMaximumSize(new Dimension(WIDTH, 25));
        devicePanel.add(deviceLabel);
        devicePanel.add(deviceComboBox);

        deviceAndPreviewPanel.add(devicePanel, BorderLayout.SOUTH);

        return deviceComboBox;
    }

    /**
     * Called when the playback or capture volume controls are changed. This is
     * to enable two sliders to move in tandem if one is dragged.
     */
    @Override
    public void volumeChange(VolumeChangeEvent e)
    {
        if (playbackSlider != null)
        {
            playbackSlider.refresh();
        }

        if (notifySlider != null)
        {
            notifySlider.refresh();
        }
    }

    /**
     * Creates basic controls for a type (AUDIO or VIDEO).
     *
     * @param type the type.
     * @return the build Component.
     */
    private Component createBasicControls(final MediaType type)
    {
        final JPanel deviceAndPreviewPanel =
                new TransparentPanel(new BorderLayout());

        deviceAndPreviewPanel.setPreferredSize(new ScaledDimension(WIDTH, 405));

        final Container videoDevicePanel = (type == MediaType.VIDEO) ?
                new TransparentPanel(new FlowLayout(FlowLayout.CENTER)) : null;

        final JComboBox<Object> videoDeviceComboBox = (type == MediaType.VIDEO) ?
                createVideoSystemControls(deviceAndPreviewPanel, videoDevicePanel) : null;

        final ActionListener deviceComboBoxActionListener
            = new ActionListener()
            {
                Object mLastSelectedVideoItem = null;

                public void actionPerformed(ActionEvent ev)
                {
                    logger.info("deviceComboBoxActionListener - action performed");

                    if (tryingToOpenParallelConnectionToVideoDevice(ev))
                    {
                        logger.info("Same device selected, drop out: " +
                                    mLastSelectedVideoItem);
                        return;
                    }

                    if (ev != null &&
                        OSUtils.isMac() &&
                        type == MediaType.VIDEO &&
                        videoDeviceComboBox.isShowing())
                    {
                        // The user has changed device.  Due to bugs in the
                        // MAC JVM (see SFR 443366) hide the current config
                        // window and recreate it
                        logger.info("Mac changed video device - recreate config container");
                        SwingUtilities.getWindowAncestor(videoDevicePanel).setVisible(false);
                        ConfigurationContainer cfgContainer =
                             NeomediaActivator.getUIService().getConfigurationContainer();
                        cfgContainer.setSelected(NeomediaActivator.getVideoConfigurationForm());
                        cfgContainer.setVisible(true);
                    }

                    boolean revalidateAndRepaint = false;

                    for (int i = deviceAndPreviewPanel.getComponentCount() - 1;
                            i >= 0;
                            i--)
                    {
                        Component c = deviceAndPreviewPanel.getComponent(i);

                        if (c != videoDevicePanel)
                        {
                            deviceAndPreviewPanel.remove(i);
                        }
                    }

                    Component preview = null;

                    if (type == MediaType.AUDIO)
                    {
                        revalidateAndRepaint = true;

                        preview = createAudioPreview(
                            deviceAndPreviewPanel.getPreferredSize());
                    }
                    else if (videoDeviceComboBox.isShowing())
                    {
                        revalidateAndRepaint = true;
                        mLastSelectedVideoItem = videoDeviceComboBox.getSelectedItem();

                        if (mLastSelectedVideoItem != null)
                        {
                            preview = createVideoPreview(
                                    videoDeviceComboBox,
                                    deviceAndPreviewPanel,
                                    deviceAndPreviewPanel.getPreferredSize());
                        }
                    }

                    if (preview != null)
                    {
                        deviceAndPreviewPanel.add(preview, BorderLayout.CENTER);
                    }

                    if (revalidateAndRepaint)
                    {
                        deviceAndPreviewPanel.revalidate();
                        deviceAndPreviewPanel.repaint();
                    }
                }

                /**
                 * Returns true if the client attempts to create a second, parallel
                 * connection to the same video device (which will fail and
                 * cause an error).
                 */
                private boolean tryingToOpenParallelConnectionToVideoDevice(ActionEvent ev)
                {
                    return (type == MediaType.VIDEO) &&
                           (ev != null) &&
                           ("comboBoxChanged".equals(ev.getActionCommand())) &&
                           (mLastSelectedVideoItem != null) &&
                           mLastSelectedVideoItem.equals(videoDeviceComboBox.getSelectedItem());
                }
            };

        if (type == MediaType.VIDEO)
        {
            videoDeviceComboBox.addActionListener(deviceComboBoxActionListener);
        }

        /*
         * We have to initialize the controls to reflect the configuration at
         * the time of creating this instance. Additionally, because the
         * preview will stop when it and its associated controls become
         * unnecessary, we have to restart it when the mentioned controls become
         * necessary again. We'll address the two goals described by pretending
         * there's a selection in the combo box when user interface becomes
         * displayable.
         */
        deviceAndPreviewPanel.addHierarchyListener(
        new HierarchyListener()
        {
            public void hierarchyChanged(HierarchyEvent event)
            {
                if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED)
                        != 0)
                {
                    SwingUtilities.invokeLater(
                    new Runnable()
                    {
                        public void run()
                        {
                            deviceComboBoxActionListener.actionPerformed(null);
                        }
                    });
                }
            }
        });

        if (OSUtils.IS_MAC)
        {
            // Resize the window in order to force the video to display - required
            // due to a bug in the MacOSX JRE.  See SFR 443366 for details. Run in
            // a timer task to allow the video UI to be created first
            TimerTask task = new TimerTask()
            {
                @Override
                public void run()
                {
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        public void run()
                        {
                            JDialog window = (JDialog)SwingUtilities.
                                           getWindowAncestor(deviceAndPreviewPanel);

                            if (window == null)
                                return;

                            Dimension windowSize = window.getSize();
                            Dimension newSize =
                                          new Dimension(windowSize.width + 5,
                                                        windowSize.height + 5);
                            window.setSize(newSize);
                            window.setResizable(false);
                        }
                    });
                }
            };
            Timer timer = new Timer("Mac video window resize timer (neomedia)");
            timer.schedule(task, 1000);
        }

        return deviceAndPreviewPanel;
    }

    /**
     * Creates all the controls for a type (AUDIO or VIDEO).
     *
     * @param type the type.
     * @return the build Component.
     */
    private Component createControls(MediaType type)
    {
        Component devicesComponent = createBasicControls(type);

        Container container = new TransparentPanel(new BorderLayout());

        container.add(devicesComponent);

        return container;
    }

    /**
     * Initializes a new <tt>Component</tt> which.is to preview and/or allow
     * detailed configuration of an audio  <tt>DeviceSystem</tt>.
     *
     * @param prefSize the preferred size to be applied to the preview
     * @return a new <tt>Component</tt> which is to preview and/or allow
     * detailed configuration of the <tt>DeviceSystem</tt> identified by
     * <tt>type</tt> and <tt>comboBox</tt>
     */
    private Component createAudioPreview(Dimension prefSize)
    {
        JComponent preview = null;

        AudioSystem audioSystem =
                mediaService.getDeviceConfiguration().getAudioSystem();

        if (audioSystem != null
            && !NoneAudioSystem.LOCATOR_PROTOCOL.equalsIgnoreCase(
                audioSystem.getLocatorProtocol()))
        {
            preview = new TransparentPanel();
            preview.setLayout(new BoxLayout(preview, BoxLayout.Y_AXIS));

            createAudioSystemControls(audioSystem, preview);
        }
        else
        {
            String noAvailableAudioDevice
                = NeomediaActivator.getResources().getI18NString(
                    "impl.media.configform.NO_AVAILABLE_AUDIO_DEVICE");
            preview = new TransparentPanel(new GridBagLayout());
            preview.add(new JLabel(noAvailableAudioDevice));
        }

        preview.setPreferredSize(prefSize);

        return preview;
    }

    /**
     * Initializes a new <tt>Component</tt> which is to preview and/or allow
     * detailed configuration of a video <tt>DeviceSystem</tt>.
     *
     * @param deviceComboBox The combobox UI component.
     * @param deviceAndPreviewPanel Parent UI component.
     * @param prefSize the preferred size to be applied to the preview
     * @return a new <tt>Component</tt> which is to preview and/or allow
     * detailed configuration of the <tt>DeviceSystem</tt> identified by
     * <tt>type</tt> and <tt>comboBox</tt>
     */
    private Component createVideoPreview(
            JComboBox<Object> deviceComboBox,
            JPanel deviceAndPreviewPanel,
            Dimension prefSize)
    {
        JLabel noPreview
            = new JLabel(
                    NeomediaActivator.getResources().getI18NString(
                            "impl.media.configform.NO_PREVIEW"));

        noPreview.setHorizontalAlignment(SwingConstants.CENTER);
        noPreview.setVerticalAlignment(SwingConstants.CENTER);

        JComponent preview = createVideoContainer(noPreview);
        preview.setPreferredSize(prefSize);

        // If we are in a conference, do not show video preview so as not
        // to block the conference app from accessing the video device.
        // If we are in a video call, do not attempt to show the video
        // preview because it will fail while we are already displaying
        // video for the call.
        if ((!NeomediaActivator.getConferenceService().isConferenceJoined()) &&
            (NeomediaActivator.getUIService().getInProgressVideoCalls().size() == 0))
        {
            Object selectedItem = deviceComboBox.getSelectedItem();
            CaptureDeviceInfo device = null;

            if (selectedItem instanceof
                            DeviceConfigurationComboBoxModel.CaptureDevice)
            {
                device =
                    ((DeviceConfigurationComboBoxModel.CaptureDevice) selectedItem)
                    .getInfo();
            }

            createVideoPreviewInContainer(device, preview);
        }

        return preview;
    }

    /**
     * Returns the video configuration panel.
     *
     * @return the video configuration panel
     */
    public Component createVideoConfigPanel()
    {
        return createControls(MediaType.VIDEO);
    }

    /**
     * Returns the <tt>MediaService</tt> instance.
     *
     * @return the <tt>MediaService</tt> instance
     */
    public MediaService getMediaService()
    {
        return mediaService;
    }

    public void onDismissed()
    {
        if (testMicContainer != null)
        {
            testMicContainer.onDismissed();
        }

        if (mPlaybackSound != null && mPlaybackListener != null)
        {
            mPlaybackSound.removeAudioListener(mPlaybackListener);
        }

        if (mNotifySound != null && mNotifyListener != null)
        {
            mNotifySound.removeAudioListener(mNotifyListener);
        }
    }

    /**
     * Creates a new listener to combo box and affect changes to the audio level
     * indicator. The level indicator is updated via a thread in order to avoid
     * deadlock of the user interface.
     */
    private class AudioLevelListenerThread
        implements ActionListener,
                   HierarchyListener
    {
        /**
         * Listener to update the audio level indicator.
         */
        private final SimpleAudioLevelListener audioLevelListener
            = new SimpleAudioLevelListener()
            {
                public void audioLevelChanged(int level)
                {
                    soundLevelIndicator.updateSoundLevel(level);
                }
            };

        /**
         * The audio system used to get and set the sound devices.
         */
        private AudioSystem audioSystem;

        /**
         * The combo box used to select the device the user wants to use.
         */
        private JComboBox<Object> comboBox;

        /**
         * The current capture device.
         */
        private AudioMediaDeviceSession deviceSession;

        /**
         * The new device chosen by the user and that we need to initialize as
         * the new capture device.
         */
        private AudioMediaDeviceSession deviceSessionToSet;

        /**
         * The indicator which determines whether
         * {@link #setDeviceSession(AudioMediaDeviceSession)} is to be invoked
         * when {@link #deviceSessionToSet} is <tt>null</tt>.
         */
        private boolean deviceSessionToSetIsNull;

        /**
         * The <tt>ExecutorService</tt> which is to asynchronously invoke
         * {@link #setDeviceSession(AudioMediaDeviceSession)} with
         * {@link #deviceSessionToSet}.
         */
        private final ExecutorService setDeviceSessionExecutor
            = Executors.newSingleThreadExecutor();

        private final Runnable setDeviceSessionTask
            = new Runnable()
            {
                public void run()
                {
                    AudioMediaDeviceSession deviceSession = null;
                    boolean deviceSessionIsNull = false;

                    synchronized (AudioLevelListenerThread.this)
                    {
                        if ((deviceSessionToSet != null)
                                || deviceSessionToSetIsNull)
                        {
                            /*
                             * Invoke #setDeviceSession(AudioMediaDeviceSession)
                             * outside the synchronized block to avoid a GUI
                             * deadlock.
                             */
                            deviceSession = deviceSessionToSet;
                            deviceSessionIsNull = deviceSessionToSetIsNull;
                            deviceSessionToSet = null;
                            deviceSessionToSetIsNull = false;
                        }
                    }

                    if ((deviceSession != null) || deviceSessionIsNull)
                    {
                        /*
                         * XXX The method blocks on Mac OS X for Bluetooth
                         * devices which are paired but disconnected.
                         */
                        setDeviceSession(deviceSession);
                    }
                }
            };

        /**
         * The sound level indicator used to show the effectiveness of the
         * capture device.
         */
        private SoundLevelIndicator soundLevelIndicator;

        /**
         *  Provides an handler which reads the stream into the
         *  "transferHandlerBuffer".
         */
        private final BufferTransferHandler transferHandler
            = new BufferTransferHandler()
            {
                public void transferData(PushBufferStream stream)
                {
                    try
                    {
                        stream.read(transferHandlerBuffer);
                    }
                    catch (IOException ioe)
                    {
                    }
                }
            };

        /**
         * The buffer used for reading the capture device.
         */
        private final Buffer transferHandlerBuffer = new Buffer();

        /**
         * Creates a new listener to combo box and affect changes to the audio
         * level indicator.
         *
         * @param audioSystem The audio system used to get and set the sound
         * devices.
         * @param comboBox The combo box used to select the device the user
         * wants to use.
         * @param soundLevelIndicator The sound level indicator used to show the
         * effectiveness of the capture device.
         */
        public AudioLevelListenerThread(
                AudioSystem audioSystem,
                JComboBox<Object> comboBox,
                SoundLevelIndicator soundLevelIndicator)
        {
            init(audioSystem, comboBox, soundLevelIndicator);
        }

        /**
         * Refresh combo box when the user click on it.
         *
         * @param ev The click on the combo box.
         */
        public void actionPerformed(ActionEvent ev)
        {
            synchronized (this)
            {
                deviceSessionToSet = null;
                deviceSessionToSetIsNull = true;
                setDeviceSessionExecutor.execute(setDeviceSessionTask);
            }

            CaptureDeviceInfo cdi;

            if (comboBox == null)
            {
                cdi
                    = soundLevelIndicator.isShowing()
                        ? audioSystem.getAndRefreshSelectedDevice(
                                DataFlow.CAPTURE)
                        : null;
            }
            else
            {
                Object selectedItem
                    = soundLevelIndicator.isShowing()
                        ? comboBox.getSelectedItem()
                        : null;

                cdi
                    = (selectedItem
                            instanceof
                                DeviceConfigurationComboBoxModel.CaptureDevice)
                        ? ((DeviceConfigurationComboBoxModel.CaptureDevice)
                                selectedItem)
                            .getInfo()
                        : null;
            }

            if (cdi != null)
            {
                for (MediaDevice md: mediaService.getDevices(
                            MediaType.AUDIO,
                            MediaUseCase.ANY))
                {
                    if (md instanceof AudioMediaDeviceImpl)
                    {
                        AudioMediaDeviceImpl amd = (AudioMediaDeviceImpl) md;

                        if (cdi.equals(amd.getCaptureDeviceInfo()))
                        {
                            try
                            {
                                MediaDeviceSession deviceSession
                                    = amd.createSession();
                                boolean deviceSessionIsSet = false;

                                try
                                {
                                    if (deviceSession instanceof
                                            AudioMediaDeviceSession)
                                    {
                                        synchronized (this)
                                        {
                                            deviceSessionToSet
                                                = (AudioMediaDeviceSession)
                                                    deviceSession;
                                            deviceSessionToSetIsNull
                                                = (deviceSessionToSet == null);
                                            setDeviceSessionExecutor.execute(
                                                    setDeviceSessionTask);
                                        }
                                        deviceSessionIsSet = true;
                                    }
                                }
                                finally
                                {
                                    if (!deviceSessionIsSet)
                                        deviceSession.close();
                                }
                            }
                            catch (Throwable t)
                            {
                                if (t instanceof ThreadDeath)
                                    throw (ThreadDeath) t;
                            }
                            break;
                        }
                    }
                }
            }
        }

        public void hierarchyChanged(HierarchyEvent ev)
        {
            if ((ev.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0)
            {
                SwingUtilities.invokeLater(
                        new Runnable()
                        {
                            public void run()
                            {
                                actionPerformed(null);
                            }
                        });
            }
        }

        /**
         * Creates a new listener to combo box and affect changes to the audio
         * level indicator.
         *
         * @param audioSystem The audio system used to get and set the sound
         * devices.
         * @param comboBox The combo box used to select the device the user
         * wants to use.
         * @param soundLevelIndicator The sound level indicator used to show the
         * effectiveness of the capture device.
         */
        public void init(
                AudioSystem audioSystem,
                JComboBox<Object> comboBox,
                SoundLevelIndicator soundLevelIndicator)
        {
            this.audioSystem = audioSystem;

            if (this.comboBox != comboBox)
            {
                if (this.comboBox != null)
                    this.comboBox.removeActionListener(this);
                this.comboBox = comboBox;
                if (comboBox != null)
                    comboBox.addActionListener(this);
            }

            if (this.soundLevelIndicator != soundLevelIndicator)
            {
                if (this.soundLevelIndicator != null)
                    this.soundLevelIndicator.removeHierarchyListener(this);
                this.soundLevelIndicator = soundLevelIndicator;
                if (soundLevelIndicator != null)
                    soundLevelIndicator.addHierarchyListener(this);
            }
        }

        /**
         * Sets the new capture device used by the audio level indicator.
         *
         * @param deviceSession The new capture device used by the audio level
         * indicator.
         */
        private void setDeviceSession(AudioMediaDeviceSession deviceSession)
        {
            if (this.deviceSession == deviceSession)
                return;

            if (this.deviceSession != null)
            {
                try
                {
                    this.deviceSession.close();
                }
                finally
                {
                    this.deviceSession.setLocalUserAudioLevelListener(null);
                    soundLevelIndicator.resetSoundLevel();
                }
            }

            this.deviceSession = deviceSession;

            if (deviceSession != null)
            {
                deviceSession.setContentDescriptor(
                        new ContentDescriptor(ContentDescriptor.RAW));
                deviceSession.setLocalUserAudioLevelListener(
                        audioLevelListener);

                deviceSession.start(MediaDirection.SENDONLY);

                try
                {
                    DataSource dataSource = deviceSession.getOutputDataSource();

                    dataSource.connect();

                    PushBufferStream[] streams
                        = ((PushBufferDataSource) dataSource).getStreams();

                    for (PushBufferStream stream : streams)
                        stream.setTransferHandler(transferHandler);

                    dataSource.start();
                }
                catch (Throwable t)
                {
                    if (t instanceof ThreadDeath)
                        throw (ThreadDeath) t;
                }
            }
        }
    }
}
