// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.Map.*;
import java.util.concurrent.*;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;
import org.jitsi.util.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.DTMFHandler.*;
import net.java.sip.communicator.impl.gui.main.call.volumecontrol.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.diagnostics.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.phonenumberutils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.CallConference.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.AccessibilityUtils;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.GuiUtils;
import net.java.sip.communicator.util.Logger;

/**
 * The Call Frame is the UI representation of a call or conference
 */
public class CallFrame extends SIPCommFrame implements ActionListener
{
    private static final long serialVersionUID = 1L;

    /**
     * The logger for this class
     */
    private static final Logger logger = Logger.getLogger(CallFrame.class);

    /**
     * The minimum length (in milliseconds) a call should last before we show
     * the call rating frame. Publicly visible for UT.
     */
    public static final long MIN_RATABLE_CALL_LENGTH = 5000;

    /**
     * The resource management service
     */
    private static final ResourceManagementService resources =
                                                    GuiActivator.getResources();

    /**
     * Phone number utils, used for format phone numbers for display in the UI.
     */
    private static final PhoneNumberUtilsService phoneNumberUtils =
                                             GuiActivator.getPhoneNumberUtils();

    /**
     * The size of the call frame when the call does not contain video
     */
    public static final Dimension DEFAULT_AUDIO_SIZE = new ScaledDimension(390, 160);

    /**
     * Property that determines whether we display video UI elements or not.
     */
    private static final String DISABLE_VIDEO_UI_PROP =
        "net.java.sip.communicator.impl.gui.DISABLE_VIDEO_UI";

    /**
     * Property that determines whether we display call recording UI or not.
     */
    private static final String CALL_RECORDING_DISABLED_PROP =
                   "net.java.sip.communicator.impl.gui.CALL_RECORDING_DISABLED";

    /**
     * The size of the call frame when the call contains video
     */
    private static final Dimension VIDEO_SIZE = new ScaledDimension(600, 480);

    /**
     * The size of the vanity feed
     */
    private static final Dimension VANITY_FEED_SIZE = new ScaledDimension(120, 75);

    private static final Color PANE_COLOR = new Color(resources.getColor(
            "refresh.gui.MAIN_COLOR"));

    /**
     * The padding to use at the bottom of the peer panel so the peer name is
     * not touching the buttons pane
     */
    private static final int PEER_PANEL_BOTTOM_PADDING = ScaleUtils.scaleInt(4);

    /**
     * The amount of padding around the vanity feed
     */
    private static final int VANITY_FEED_PADDING = ScaleUtils.scaleInt(10);

    /**
     * The amount of time in ms to show DTMF for
     */
    public static final int DTMF_SHOW_DURATION = 2000;

    /**
     * The image loader service
     */
    private static final ImageLoaderService imageLoaderService =
                                           GuiActivator.getImageLoaderService();

    /**
     * The icon to use for the add peer button
     */
    private static final ImageIconFuture ADD_PEER_IMAGE_ICON =
        imageLoaderService.getImage(ImageLoaderService.ADD_PARTICIPANT)
        .getImageIcon();

    /**
     * The icon to use for the add peer button when the mouse is over the peer
     * panel
     */
    private static final ImageIconFuture ADD_PEER_IMAGE_ICON_ROLLOVER =
        imageLoaderService.getImage(ImageLoaderService.ADD_PARTICIPANT_ROLLOVER)
        .getImageIcon();

    /**
     * The icon to use for the add peer button when it is pressed
     */
    private static final ImageIconFuture ADD_PEER_IMAGE_ICON_PRESSED =
        imageLoaderService.getImage(ImageLoaderService.ADD_PARTICIPANT_PRESSED)
        .getImageIcon();

    /**
     * The icon to use for the add peer button in a multi-peer call
     */
    private static final ImageIconFuture ADD_PEER_IMAGE_ICON_MULTI =
        imageLoaderService.getImage(ImageLoaderService.ADD_PARTICIPANT_MULTI)
        .getImageIcon();

    /**
     * The icon to use for the add peer button when the mouse is over the peer
     * panel and this is a multi-peer call
     */
    private static final ImageIconFuture ADD_PEER_IMAGE_ICON_MULTI_ROLLOVER =
        imageLoaderService.getImage(ImageLoaderService.ADD_PARTICIPANT_MULTI_ROLLOVER)
        .getImageIcon();

    /**
     * The icon to use for the add peer button when it is pressed and this is a
     * multi-peer call
     */
    private static final ImageIconFuture ADD_PEER_IMAGE_ICON_MULTI_PRESSED =
        imageLoaderService.getImage(ImageLoaderService.ADD_PARTICIPANT_MULTI_PRESSED)
        .getImageIcon();

    /**
     * The diameter of the add peer image label
     */
    public static final int ADD_PEER_IMAGE_LABEL_DIAMETER = ScaleUtils.scaleInt(46);

    /**
     * The timer used to show/hide the DTMF label
     */
    private Timer dtmfTimer = new Timer(CallFrame.DTMF_SHOW_DURATION, this);

    /**
     * The size of the call frame to actually use. This is used if the
     * components included in the call frame do not fit (e.g. when Windows is
     * displaying 125% or 150% text size)
     */
    private Dimension audioSize = DEFAULT_AUDIO_SIZE;

    /**
     * This JLayeredPane contains all content displayed in the call frame
     * above the buttons tool bar
     */
    private JLayeredPane layeredCallPeersPane;

    /**
     * The background panel is the panel used to display the gradient
     * background, it is on the background layer of the layeredCallPeersPane
     */
    private JPanel backgroundPanel;

    /**
     * A call frame can contain multiple peers. This map links CallPeer
     * objects to their PeerPanel elements.
     */
    protected final ConcurrentHashMap<CallPeer, PeerPanel> callPeers =
            new ConcurrentHashMap<>();

    /**
     * A list used to preserve the order of call peer panels when peers are
     * added or removed
     */
    private LinkedList<CallPeer> callPeersOrder = new LinkedList<>();

    /**
     * The buttons pane runs across the bottom of the call frame, it comprises
     * the actionButtonsPane and the endCallPane
     */
    private JPanel buttonsPane;

    /**
     * The actionButtonsPane contains all the buttons pertaining to a call
     * (e.g. turn on local video). It is positioned to the left hand side of
     * the buttons pane.
     */
    private JPanel actionButtonsPane;

    /**
     * Keep a reference to the record button so that we can dispose it when this
     * window is closed.
     */
    private RecordButton recordButton;

    /**
     * Reference to the hold button so that we can dispose of it when the window
     * is closed.
     */
    private HoldButton holdButton;

    /**
     * Reference to the call push button so that we can dispose of it when the window
     * is closed.
     */
    private CallPushButton pushButton;

    /**
     * The endCallPane contains only the hangup button. It is positioned to
     * the right hand side of the buttonsPane
     */
    private JPanel endCallPane;

    /**
     * The audioWarningComponent is the triangle audio warning in the top
     * right of the call frame. It is added to the layeredCallPeersPane.
     */
    private MediaStateComponent audioWarningComponent;

    /**
     * The Call Conference instance depicted by this Call Frame
     */
    private CallConference callConference;

    /**
     * The Call Conference Listener listens for events such as peer added and
     * peer removed
     */
    private CallConferenceListener callConferenceListener =
                                                   new CallConferenceListener();

    /**
     * The timer that is responsible for updating the call frame title to show
     * the call duration
     */
    protected Timer callDurationTimer = new Timer(1000, this);

    /**
     * The time in milliseconds at which this call became connected. Visibility
     * not private so that it can be faked in UTs.
     */
    protected long callStartTime = 0;

    /**
     * The string representation of the display names of each peer in this call
     */
    private String callTitlePeers = "";

    /**
     * The state of this call, e.g. "connected", "ringing"
     */
    private String callTitleState =
                       resources.getI18NString("service.gui.CONNECTING_STATUS");

    /**
     * The most recent CallPeerState passed to this call frame, e.g.
     * "connected", "ringing"
     */
    private String latestCallPeerStateString = callTitleState;

    /**
     * The microphone button, sometimes not used
     */
    private AbstractVolumeControlButton microphoneButton;

    /**
     * The volume button, sometimes not used
     */
    private AbstractVolumeControlButton volumeButton;

    /**
     * The dialog that displays the numeric dial pad
     */
    private DialpadDialog dialpadDialog;

    /**
     * The DTMF handler for this call frame
     */
    private DTMFHandler dtmfHandler;

    /**
     * The dialpad button
     */
    private InCallButton btnDialpad;

    /**
     * The merge button
     */
    private InCallButton btnMerge;

    /**
     * The Web Conference button
     */
    private InCallButton webConfButton;

    /**
     * The local video button used to toggle video on this call
     */
    private LocalVideoButton videoButton;

    /**
     * The park button used to park a call
     */
    private ParkButton parkButton;

    /**
     * A timer used to invalidate the call frame every second when there is video
     * showing.  A historic comment says that this is needed to deal with a video
     * bug on Mac, but it also causes intermittent white flashes on the video call
     * display on Windows, so we use it on Mac only.
     */
    private Timer validateTimer = null;

    /**
     * The dialog which controls adding a new participant to a call
     */
    private AddParticipantDialog addParticipantDialog;

    /**
     * Boolean indicating that we've already sent an end of call report.
     */
    private boolean callReportSent = false;

    /**
     * A listener for changes in the state of the call, used to update the
     * audioWarningComponent
     */
    private final CallChangeAdapter callChangeListener
        = new CallChangeAdapter()
        {
            public void callStateChanged(CallChangeEvent evt)
            {
                if (audioWarningComponent == null)
                    return;

                if (evt.getPropertyName().equals(
                                       CallChangeEvent.CALL_MEDIA_STATE_CHANGE))
                {
                    audioWarningComponent.setMediaState((Integer) evt.getNewValue());
                }

                if (evt.getPropertyName().equals(
                    CallChangeEvent.OUTBOUND_PACKET_LOSS_CHANGE))
                {
                    audioWarningComponent.setOutboundPacketLoss((Boolean) evt.getNewValue());
                }

                if (evt.getPropertyName().equals(
                    CallChangeEvent.INBOUND_PACKET_LOSS_CHANGE))
                {
                    audioWarningComponent.setInboundPacketLoss((Boolean) evt.getNewValue());
                }
            }
        };

    /**
     * The local video component for this call. This is the component that
     * displays the vanity feed when the user is sending video
     */
    private Component localVideoComponent;

    /**
     * The remote video component for this call. This is the component that
     * displays the remote peer's video stream when they are sending video
     */
    private Component remoteVideoComponent;

    /**
     * If <tt>true</tt>, there is a pending dispose operation, and subsequent
     * calls to <tt>dispose</tt> will be ignored.<br>
     * To close the call frame immediately, call {@link #disposeNow}.
     */
    private boolean closing;

    /**
     * The panel used to display DTMF
     */
    private static final JLabel dtmfLabel = new JLabel();

    /**
     * The panel that contains the add participant label
     */
    private JPanel addParticipantPanel;

    /**
     * The add peer image label
     */
    private JLabel callFrameAddPeerImageLabel = null;

    /**
     * The timer that is responsible for performing some action to prevent the
     * OS from entering the screensaver mode (turn off display, sleep, etc)
     */
    private Timer screensaverBlockTimer = new Timer(60000, this);

    /**
     * Create a new call frame.
     *
     * @param callPeer the remote peer associated with this call frame. All
     * call frames must be initialized with one call peer
     * @param conference the call conference that this call frame is
     * depicting
     */
    public CallFrame(CallPeer callPeer, CallConference conference)
    {
        super(true);

        callConference = conference;
        closing = false;

        logger.info("Creating call frame");

        // Set up the call duration timer
        callDurationTimer.setRepeats(true);

        dtmfLabel.setHorizontalAlignment(SwingConstants.CENTER);

        if (callConference.getCallCount() == 1)
        {
            Call call = callConference.getCalls().get(0);
            call.addCallChangeListener(callChangeListener);
            logger.info("Adding call to call frame " + call.getCallID());
        }

        // Ensure we dispose of all resources and hang up peers when the
        // window is closed
        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent we)
            {
                CallManager.hangupCalls(callConference);
                disposeNow();
            }
        });

        // Set up the listeners for the call or the call peers changing
        callConference.addCallChangeListener(callConferenceListener);
        callConference.addCallPeerConferenceListener(callConferenceListener);

        // Initialise the DTMF handler
        dtmfHandler = new DTMFHandler(this);

        // Create a new call frame
        initializeCallFrame();

        // Creates a Peer Panel for this call and put it in the call peers map
        PeerPanel peerPanel = new PeerPanel(this, callPeer, false);
        synchronized (callPeers)
        {
            logger.debug("Adding call peer: " + callPeer);
            callPeers.put(callPeer, peerPanel);
            callPeersOrder.add(callPeer);
            updateCallTitleState(callPeer, callPeer.getState());
        }

        backgroundPanel.add(peerPanel);

        // Set the title of this window
        updateCallTitlePeers();

        if (OSUtils.IS_MAC) {
            /*
            * A timer used to invalidate the call frame every second when there is video
            * showing.  A historic comment says that this is needed to deal with a video
            * bug on Mac, but it also causes intermittent white flashes on the video call
            * display on Windows, so we use it on Mac only.
            */
            this.validateTimer = new Timer(1000, this);
            this.validateTimer.start();
        }

        // Initialize the screensaver blocker timer
        screensaverBlockTimer.setRepeats(true);
        screensaverBlockTimer.start();

        actionButtonsPane.requestFocus();
    }

    /**
     * Set up the window UI by setting the icon, window size and inner frames
     */
    private void initializeCallFrame()
    {
        logger.debug("Initialize Call Frame: " + System.currentTimeMillis());
        if (ConfigurationUtils.isCallAlwaysOnTop())
            setAlwaysOnTop(true);

        setIconImage(GuiActivator.getImageLoaderService().getImage(ImageLoaderService.SIP_COMMUNICATOR_LOGO).resolve());

        setBounds(0, 0, audioSize.width, audioSize.height);

        // Configure the content pane for this call frame
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);

        // The ButtonsPane is located in the bottom of the call frame, it
        // contains the ActionButtonsPane and the EndCallPane
        buttonsPane = new JPanel();
        buttonsPane.setBackground(PANE_COLOR);
        buttonsPane.setLayout(new BorderLayout(0, 0));
        buttonsPane.setBorder(BorderFactory.createCompoundBorder(
                           new MatteBorder(1,0,0,0,PANE_COLOR),
                           new MatteBorder(2,0,0,0,PANE_COLOR)));
        contentPane.add(buttonsPane, BorderLayout.SOUTH);

        // The ActionButtonsPane contains buttons that affect all call peers
        // in the call or the call itself
        actionButtonsPane = new JPanel();

        // The action buttons pane contains a 5 pixel border in order to space
        // the buttons correctly
        actionButtonsPane.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 3));
        actionButtonsPane.setBackground(PANE_COLOR);
        buttonsPane.add(actionButtonsPane, BorderLayout.WEST);

        // The EndCallPane contains the hang up button only
        endCallPane = new JPanel();
        endCallPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        endCallPane.setBackground(PANE_COLOR);
        buttonsPane.add(endCallPane, BorderLayout.EAST);

        // Create all buttons to display in the buttons pane that is at the
        // bottom of the call frame.
        initializeButtonsPane();

        // Configure the Layered Pane that is the main UI within the Call
        // Frame. The Layered Pane contains each Call Peer Frame on the
        // background layer, and any overlays such as the audio warning button
        // on the Palette and Modal layers
        layeredCallPeersPane = new JLayeredPane();

        // Configure the size of this call frame
        setPreferredSize(new Dimension(getWidth(), getHeight()));
        setResizable(false);

        // Determine the bounds for the layered pane, it is set to the same
        // size as the call frame minus the buttons pane. The call frame must
        // be packed first so the insets can be determined
        pack();
        int layeredPaneWidth = getWidth() - getInsets().right - getInsets().left;
        int layeredPaneHeight = getHeight() - getInsets().top -
                                getInsets().bottom - buttonsPane.getHeight();

        layeredCallPeersPane.setBounds(new Rectangle(0, 0, layeredPaneWidth, layeredPaneHeight));
        contentPane.add(layeredCallPeersPane, BorderLayout.CENTER);

        // Add a mouse listener so the Add Participant button becomes more
        // visible when the mouse is anywhere over this pane.
        layeredCallPeersPane.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseEntered(MouseEvent e)
            {
                setMouseOver(true);
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                // This mouse listener is set for the Layered Pane, but any
                // components placed on this pane will steal focus when the
                // mouse is over them. Check whether the mouse co-ordinates
                // are within this frame's space before deciding the mouse has
                // exited.
                Point p = e.getLocationOnScreen();

                SwingUtilities.convertPointFromScreen(p, e.getComponent());
                if (!e.getComponent().contains(p))
                {
                    setMouseOver(false);
                }
            }
        });

        initializeLayeredCallPeersPane();
    }

    /**
     * Sets the add participant graphic accordingly if the mouse is over this
     * peer
     * @param isMouseOver whether the mouse is over this component
     */
    private void setMouseOver(boolean isMouseOver)
    {
        if (callFrameAddPeerImageLabel == null)
        {
            return;
        }

        if (isMouseOver)
        {
            if (callPeers.size() == 1)
            {
                ADD_PEER_IMAGE_ICON_ROLLOVER.addToLabel(callFrameAddPeerImageLabel);
            }
            else
            {
                ADD_PEER_IMAGE_ICON_MULTI_ROLLOVER.addToLabel(callFrameAddPeerImageLabel);
            }
        }
        else
        {
            if (callPeers.size() == 1)
            {
                ADD_PEER_IMAGE_ICON.addToLabel(callFrameAddPeerImageLabel);
            }
            else
            {
                ADD_PEER_IMAGE_ICON_MULTI.addToLabel(callFrameAddPeerImageLabel);
            }
        }
    }

    /**
     * Creates and configures all buttons in the ButtonsPane that is located
     * at the bottom of the CallFrame
     */
    private void initializeButtonsPane()
    {
        createWebConferenceButton();
        createHoldButton();
        createVideoButton();
        createVolumeSliderButtons();
        createDialpadButton();
        createPushButton();
        createRecordCallButton();
        createMergeButton();
        createParkButton();
        createHangupButton();
    }

    private void createVideoButton()
    {
        videoButton = new LocalVideoButton(callConference, this);

        // Set the video button as disabled until the call is connected
        videoButton.setEnabled(false);

        // Video UI can be disabled, in which case don't show the video button
        // We still create it, in order to defend against NPEs if people assume
        // it's non-null.
        boolean videoUIDisabled = GuiActivator.getConfigurationService().user().
                                       getBoolean(DISABLE_VIDEO_UI_PROP, false);
        if (videoUIDisabled)
        {
            logger.info("Hiding video button as video UI is disabled");
            videoButton.setVisible(false);
        }

        actionButtonsPane.add(videoButton);
    }

    private void createMergeButton()
    {
        btnMerge = new InCallButton(
            ImageLoaderService.MERGE_CALL_BUTTON,
            ImageLoaderService.MERGE_CALL_BUTTON_ROLLOVER,
            ImageLoaderService.MERGE_CALL_BUTTON_FOCUS,
            ImageLoaderService.MERGE_CALL_BUTTON_PRESSED,
            null,
            null,
            ImageLoaderService.MERGE_CALL_BUTTON_FOCUS,
            null,
            resources.getI18NString("service.gui.MERGE_TO_CALL"));

        updateMergeButtonState();

        btnMerge.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent evt)
            {
                logger.user("Merge button clicked in call: " +
                                    callConference);
                CallManager.mergeExistingCalls(
                        callConference,
                        CallManager.getInProgressCalls());

                // The button has been pressed so set it to not visible as the
                // user cannot merge more than one call
                btnMerge.setVisible(false);
            }
        });

        actionButtonsPane.add(btnMerge);
    }

    private void createVolumeSliderButtons()
    {
        // Create the in-line volume controls
        microphoneButton = new InputVolumeControlButton(callConference);

        // Add the microphone button as a PropertyChangeListener so it
        // responds to external mute events.
        for (CallPeer callPeer : callConference.getCallPeers())
        {
            callPeer.addPropertyChangeListener(
                     (InputVolumeControlButton) microphoneButton);
        }

        volumeButton = new OutputVolumeControlButton(callConference);
        actionButtonsPane.add(microphoneButton);
        actionButtonsPane.add(volumeButton);
    }

    private void createDialpadButton()
    {
        // The dialpad button
        btnDialpad = new InCallButton(
            ImageLoaderService.DIAL_BUTTON,
            ImageLoaderService.DIAL_BUTTON_ROLLOVER,
            ImageLoaderService.DIAL_BUTTON_FOCUS,
            ImageLoaderService.DIAL_BUTTON_PRESSED,
            null,
            null,
            ImageLoaderService.DIAL_BUTTON_FOCUS,
            ImageLoaderService.DIAL_BUTTON_DISABLED,
            resources.getI18NString("service.gui.SHOW_HIDE_DIALPAD"));

        actionButtonsPane.add(btnDialpad);

        btnDialpad.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                logger.user("Dialpad button clicked in call: " +
                                    callConference);

                if (dialpadDialog == null)
                {
                    dialpadDialog = getDialpadDialog();
                }

                btnDialpad.setSelected(!btnDialpad.isSelected());

                dialpadDialog.setVisible(!dialpadDialog.isVisible());
            }
        });
    }

    private void createHangupButton()
    {
        // The hang up button
        JButton btnHangup = new InCallButton(
            ImageLoaderService.END_CALL,
            ImageLoaderService.END_CALL_ROLLOVER,
            ImageLoaderService.END_CALL_FOCUS,
            ImageLoaderService.END_CALL_PRESSED,
            null,
            null,
            ImageLoaderService.END_CALL_FOCUS,
            null,
            resources.getI18NString("service.gui.HANG_UP"));

        btnHangup.setBackground(null);
        btnHangup.setBorderPainted(false);
        btnHangup.setPreferredSize(new ScaledDimension(60, 32));

        endCallPane.add(btnHangup);
        btnHangup.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent evt)
            {
                logger.user("Hang-up button pressed in call: " +
                                    callConference);
                CallManager.hangupCalls(callConference);
                dispose();
            }
        });
    }

    private void createHoldButton()
    {
        logger.info("Creating new hold button for call " + callConference);
        holdButton = new HoldButton(this);
        actionButtonsPane.add(holdButton);
    }

    private void createPushButton()
    {
        GuiActivator.getCosService().getClassOfService((cos) ->
        {
            if (cos.getSubscribedMashups().isCallJumpAllowed())
            {
                pushButton = new CallPushButton(this);
                actionButtonsPane.add(pushButton);
            }
        });
    }

    private void createRecordCallButton()
    {
        if (callConference.getCallCount() == 1)
        {
            Call call = callConference.getCalls().get(0);
            recordButton = new RecordButton(call);
            actionButtonsPane.add(recordButton);

            // The record button can be disabled in config; in which case we
            // hide it from the UI.
            // We still create it, in order to defend against NPEs if people
            // assume it's non-null.
            boolean recordBtnDisabled = GuiActivator.getConfigurationService().user().
                                getBoolean(CALL_RECORDING_DISABLED_PROP, false);
            if (recordBtnDisabled)
            {
                logger.info("Hiding record button as it is disabled in config");
                recordButton.setVisible(false);
            }
        }
    }

    private void createParkButton()
    {
        List<ProtocolProviderService> providers =
                          GuiActivator.getProviders(OperationSetCallPark.class);

        if (providers.size() == 1)
        {
            logger.debug("Got 1 call park provider");
            OperationSetCallPark opSet =
                   providers.get(0).getOperationSet(OperationSetCallPark.class);

            if (opSet.isCallParkAvailable() && opSet.isEnabled())
            {
                logger.debug("Call park is enabled");
                parkButton = new ParkButton(this, callConference);
                actionButtonsPane.add(parkButton);
            }
        }
    }

    /**
     * Create the Start Web Conference button.
     */
    private void createWebConferenceButton()
    {
        if (!CreateConferenceMenu.isConferenceInviteByImEnabled())
        {
            return;
        }

        logger.debug("Web Conferencing enabled. Displaying in call button");
        webConfButton = new InCallButton(
            ImageLoaderService.WEB_CONFERENCE_BUTTON,
            ImageLoaderService.WEB_CONFERENCE_BUTTON_ROLLOVER,
            null,
            ImageLoaderService.WEB_CONFERENCE_BUTTON_PRESSED,
            null,
            null,
            null,
            ImageLoaderService.WEB_CONFERENCE_BUTTON_DISABLED,
            resources.getI18NString("service.gui.conf.START_CONFERENCE"));

        actionButtonsPane.add(webConfButton);

        webConfButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent evt)
            {
                logger.user("Web conference button clicked in call: " +
                                    callConference);
                CreateConferenceMenu createMenu =
                    new CreateConferenceMenu(getCallConference());
                createMenu.getJPopupMenu(webConfButton).setVisible(true);
            }
        });
    }

    private void initializeLayeredCallPeersPane()
    {
        // The AudioWarningPanel contains the audio warning label
        audioWarningComponent = new MediaStateComponent();
        audioWarningComponent.setOpaque(false);
        audioWarningComponent.setLayout(null);
        audioWarningComponent.setFocusable(true);

        // The audio warning component is added on the palette layer so it is
        // always drawn on top of other components
        layeredCallPeersPane.add(audioWarningComponent,
                                 JLayeredPane.PALETTE_LAYER);

        // The background panel has a gradient background, this is achieved by
        // overriding the paintComponent method
        backgroundPanel = new JPanel()
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g)
            {
                super.paintComponent(g);

                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                                     RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp =
                    new GradientPaint(0,
                                      0,
                                      PANE_COLOR,
                                      0,
                                      getHeight(),
                                      PANE_COLOR);

                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                g.finalize();
            }
        };

        // Configure the Layered Pane with the background layer and the
        // palette layer. A layered pane does not use a layout manager so each
        // panel's bounds must be configured
        backgroundPanel.setBounds(layeredCallPeersPane.getBounds());
        backgroundPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        layeredCallPeersPane.add(backgroundPanel, JLayeredPane.DEFAULT_LAYER);
    }

    /**
     * Returns the <tt>DialpadDialog</tt> corresponding to this CallDialog.
     *
     * @return the <tt>DialpadDialog</tt> corresponding to this CallDialog.
     */
    private DialpadDialog getDialpadDialog()
    {
        return new DialpadDialog(dtmfHandler, btnDialpad);
    }

    /**
     * Adds a new peer to this call if allowed to do so. A maximum of 2 remote
     * peers is permitted.
     *
     * @param newCallPeer the peer to add to this call
     */
    public void addPeer(CallPeer newCallPeer)
    {
        synchronized (callPeers)
        {
            logger.info("Adding new call peer to call frame: " + newCallPeer);

            // If we are going from a one-to-one call to a multi-party call then
            // we need to add a new add participant button
            boolean addAddParticipantLabel = (callPeers.size() == 1);

            // We need to add the new call to the call conference
            if (newCallPeer.getCall() != null)
            {
                newCallPeer.getCall().setConference(callConference);
            }

            // Ensure neither peer has video
            for (Call call : callConference.getCalls())
            {
                CallManager.enableLocalVideo(call, false);
                removeVideoComponent(true);
                removeVideoComponent(false);
            }

            // We keep track of each peer we remove so they can be re-added later
            LinkedList<CallPeer> removedCallPeers = new LinkedList<>();

            for (CallPeer callPeer : callPeersOrder)
            {
                PeerPanel peerPanel = callPeers.get(callPeer);

                // Remove existing PeerPanels and separators as they must be
                // recreated
                peerPanel.dispose();
                backgroundPanel.remove(peerPanel);

                callPeers.remove(callPeer);

                removedCallPeers.add(callPeer);
            }

            if (addAddParticipantLabel)
            {
                addParticipantPanel = new JPanel();
                addParticipantPanel.setLayout(new BoxLayout(addParticipantPanel,
                                                     BoxLayout.Y_AXIS));

                addParticipantPanel.setBackground(new Color(0, 0, 0, 0));
                addParticipantPanel.setOpaque(false);

                createAddParticipantLabel(false);
                callFrameAddPeerImageLabel.setBounds(
                                          0,
                                          0,
                                          PeerPanel.PEER_DISPLAY_IMAGE_DIAMETER,
                                          PeerPanel.PEER_DISPLAY_IMAGE_DIAMETER);
                addParticipantPanel.add(callFrameAddPeerImageLabel);

                addParticipantPanel.add(Box.createVerticalStrut(30));
                backgroundPanel.add(addParticipantPanel);
            }

            // Add the new peer and create the new peer panel
            PeerPanel newPeerPanel = new PeerPanel(this, newCallPeer, true);
            backgroundPanel.add(newPeerPanel);
            callPeers.put(newCallPeer, newPeerPanel);
            callPeersOrder.add(0, newCallPeer);

            // Add the existing call peers back
            for (CallPeer callPeer : removedCallPeers)
            {
                // Recreate the Peer Panel for this peer
                PeerPanel oldPeerPanel = new PeerPanel(this, callPeer, true);
                backgroundPanel.add(oldPeerPanel);
                callPeers.put(callPeer, oldPeerPanel);
            }

            // Add some padding to the right hand side of the call frame if the
            // add participant label is showing
            if (callPeers.size() > 1)
            {
                backgroundPanel.setBorder(new EmptyBorder(0, 0, 0, ADD_PEER_IMAGE_LABEL_DIAMETER));
            }

            // Disable the video button as this is not allowed for a 3-way call
            videoButton.setEnabled(false);

            if (parkButton != null)
                parkButton.callPeerAdded(null);

            // Update the title of the call frame
            updateCallTitlePeers();

            // Update the merge button state
            updateMergeButtonState();

            // We must validate and repaint as the contents of the Call Frame have
            // changed
            validate();
            repaint();
        }
    }

    /**
     * Removes a peer from this call
     *
     * Must be called on the UI thread
     *
     * @param callPeer the peer to remove from this call
     */
    public void removePeer(CallPeer callPeer)
    {
        synchronized (callPeers)
        {
            // Remove the call peer panel
            PeerPanel peerPanelToRemove = callPeers.remove(callPeer);
            if (peerPanelToRemove == null)
            {
                // We have already removed this peer so nothing to do
                return;
            }

            backgroundPanel.remove(peerPanelToRemove);
            peerPanelToRemove.dispose();
            callPeersOrder.remove(callPeer);

            if (callPeers.isEmpty())
            {
                // We have been told to remove the final peer in this call, so
                // dispose of the call frame.
                logger.info("Removed last peer from call frame: " + callPeer);

                // If the reason for the call peer being removed is that it has
                // failed, inform the user by keeping the window open for a couple
                // of seconds with an updated title.
                if (callPeer.getState() == CallPeerState.FAILED)
                {
                    logger.debug("Call failed");
                    closing = true;
                    callConference.setCallState(CallConferenceStateEnum.CALL_FAILED);
                    updateCallTitleState();

                    new Thread("Delayed call frame dispose"){
                        public void run() {
                            try
                            {
                                Thread.sleep(3 * 1000); // wait for 3 seconds
                            }
                            catch (InterruptedException e)
                            {
                                logger.error("Unexpectedly interrupted", e);
                            }

                            logger.debug("Now dispose of call frame");
                            disposeNow();
                        }
                    }.start();
                }
                else
                {
                    dispose();
                }
                return;
            }

            logger.info("Removing call peer from call frame: " + callPeer);

            if (callPeers.size() == 1)
            {
                // Remove the padding from the background panel
                backgroundPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

                // At this point we know that the map contains one peer and one
                // peerPanel. Get them both
                Entry<CallPeer, PeerPanel> currentPeer =
                                                 callPeers.entrySet().iterator().next();

                // Update the call state
                updateCallTitleState(
                    currentPeer.getKey(), currentPeer.getKey().getState());

                // Dispose of the add participant label
                if (callFrameAddPeerImageLabel != null)
                {
                    backgroundPanel.remove(addParticipantPanel);
                    callFrameAddPeerImageLabel = null;
                    addParticipantPanel = null;
                }

                // Dispose of and recreate the peer panel for the remaining peer
                backgroundPanel.remove(currentPeer.getValue());
                callPeers.remove(currentPeer.getKey());
                currentPeer.getValue().dispose();

                PeerPanel oldPeerPanel = new PeerPanel(this, currentPeer.getKey(), false);
                backgroundPanel.add(oldPeerPanel);
                callPeers.put(currentPeer.getKey(), oldPeerPanel);

                // Update the local video button as it is now permitted to be used
                videoButton.setEnabled(true);

                if (parkButton != null)
                    parkButton.callPeerRemoved(null);
            }

            // Update the title
            updateCallTitlePeers();

            // Update the merge button state
            updateMergeButtonState();

            // We must validate and repaint as the contents of the Call Frame have
            // changed
            validate();
            repaint();
        }
    }

    /**
     * Returns the call conference that is represented by this call frame
     *
     * @return the call conference represented by this call frame
     */
    public CallConference getCallConference()
    {
        return callConference;
    }

    /**
     * Sets the local video button selected state
     *
     * @param isSelected whether selected
     */
    public void setVideoButtonSelected(boolean isSelected)
    {
        videoButton.setSelected(isSelected);
    }

    /**
     * Updates the callTitleState using the CallConferenceState, if it isn't
     * the default, otherwise using the most recent CallPeerState.
     */
    public void updateCallTitleState()
    {
        updateCallTitleState(null, null);
    }

    /**
     * Updates the callTitleState using the CallConferenceState, if it isn't
     * the default, otherwise using the given CallPeer and CallPeerState,
     * unless they are null or the CallPeer is no longer in the call, when the
     * most recent CallPeerState will be used.
     *
     * @param callPeer the CallPeer
     * @param callPeerState the CallPeerState
     */
    public synchronized void updateCallTitleState(CallPeer callPeer,
                                                  CallPeerState callPeerState)
    {
        logger.interval("updateCallTitleState1:" + getCallConference().hashCode(),
                        "Updating call title state",
                        "callPeer = " + callPeer,
                        "callPeerState = " + callPeerState);

        // If the CallConference associated with this CallFrame has a state
        // string, that will be displayed in preference to a call peer state
        // string. However, we will also use the CallPeerState to decide
        // whether to start the call timer and enable/disable the video button,
        // so we need to get both for now.
        String callConfStateString =
            getCallConference().getCallState().getLocalizedStateString();

        String callPeerStateString =
            (callPeerState == null) ? null : callPeerState.getLocalizedStateString();

        if (CallPeerState.CONNECTED.equals(callPeerState))
        {
            if (!callDurationTimer.isRunning())
            {
                // The peer is now connected so start the call duration
                // timer
                callDurationTimer.start();
                callStartTime = System.currentTimeMillis();
            }

            // If this is a single-peer call then set the video button to
            // enabled.
            if (getCallConference().getCallPeerCount() == 1)
            {
                if (videoButton != null)
                {
                    videoButton.setEnabled(true);
                }
            }

            // If the call is connected then don't display a state
            callPeerStateString = "";
        }

        // If we have got a CallPeerStateString we need to save it in case we
        // need to revert to the latest CallPeerState after the CallConference
        // reverts to having no call state, unless the callPeer is no longer in
        // the call. When a peer disconnects from a call, we receive events in
        // an undefined order. We must therefore check whether we have already
        // removed this peer from the call frame before using its the call
        // state.
        if (callPeerStateString != null)
        {
            if ((callPeer != null) && callPeers.containsKey(callPeer))
            {
                logger.interval(
                    "updateCallTitleState2:" + getCallConference().hashCode(),
                    "Saving latestCallPeerStateString",
                    "latestCallPeerStateString = " + callPeerStateString);
                latestCallPeerStateString = callPeerStateString;
            }
            else
            {
                logger.interval(
                    "updateCallTitleState3:" + getCallConference().hashCode(),
                    "CallPeer no longer in call, ignoring CallPeerState. " +
                    "CallPeers = " + callPeers,
                    "callPeerStateString = " + callPeerStateString);
                callPeerStateString = null;
            }
        }

        if (callConfStateString != null)
        {
            // If we have a callConfStateString, always use that.
            logger.interval(
                "updateCallTitleState4:" + getCallConference().hashCode(),
                "Using callConfStateString as callTitleState",
                "callConfStateString = " + callConfStateString);
            callTitleState = callConfStateString;
        }
        else if (callPeerStateString != null)
        {
            // If we don't have a callConfStateString, use the
            // callPeerStateString if we have one.
            logger.interval(
                "updateCallTitleState5:" + getCallConference().hashCode(),
                "Using callPeerStateString as callTitleState",
                "callPeerStateString = " + callPeerStateString);
            callTitleState = callPeerStateString;
        }
        else
        {
            // We don't have a callConfStateString or a callPeerStateString
            // a from valid CallPeer so fall back to using the previous
            // callPeerStateString.
            logger.interval(
                "updateCallTitleState6:" + getCallConference().hashCode(),
                "Using latestCallPeerStateString as callTitleState",
                "latestCallPeerStateString = " + latestCallPeerStateString);
            callTitleState = latestCallPeerStateString;
        }

        updateCallFrameTitle();
    }

    /**
     * Update the title of the call frame window. The call duration or the
     * peer state changing causes this method to be called
     */
    public synchronized void updateCallFrameTitle()
    {
        String duration = (callStartTime != 0) ?
                              GuiUtils.formatTime(callStartTime,
                                                  System.currentTimeMillis()) :
                              "00:00:00";

        String callFrameTitle = callTitlePeers + " - " + duration;

        if (!StringUtils.isNullOrEmpty(callTitleState))
        {
            callFrameTitle = callFrameTitle + " " + callTitleState;
        }

        setTitle(callFrameTitle);
    }

    /**
     * Update the text used in the call window title for the call peers
     */
    protected void updateCallTitlePeers()
    {
        synchronized (callPeers)
        {
            String peerDisplayNames = "";

            if (callPeers.size() != 0)
            {
                if (callPeers.size() < 3)
                {
                    for (CallPeer callPeer : callPeers.keySet())
                    {
                        // We just take the first part of the display name up
                        // to the first space.
                        String firstPart = callPeer.getDisplayName().split(" ")[0];

                        // In case the display name is a phone number, format
                        // it nicely for display. If it isn't a valid phone
                        // number, this method will just return the display
                        // name unchanged.
                        String peerDisplayName =
                            phoneNumberUtils.formatNumberForDisplay(firstPart);

                        if (peerDisplayNames.isEmpty())
                        {
                            peerDisplayNames = peerDisplayName;
                        }
                        else
                        {
                            peerDisplayNames = resources.getI18NString(
                                    "service.gui.MULTIPLE_PEER_DISPLAY_NAMES",
                                    new String[]{peerDisplayNames, peerDisplayName});
                        }
                    }
                }
                else
                {
                    peerDisplayNames =
                        resources.getI18NString("service.gui.CONFERENCE");
                }
            }

        callTitlePeers = peerDisplayNames;
        }

        updateCallFrameTitle();
    }

    /**
     * Updates the enabled state of the video button in this call frame. If
     * any video devices are available, the button will be enabled, otherwise
     * it will be disabled.
     */
    protected void updateVideoButtonEnabledState()
    {
        logger.info(
            "Asked to update video button enabled state for CallFrame: " +
                                                              this.getTitle());
        videoButton.maybeEnableVideoButton();
    }

    /**
     * Called by the Call Manager when a call event requires us to make a UI
     * change.
     *
     * @param ev the CallEvent
     */
    public void onCallEvent(CallEvent ev)
    {
        updateMergeButtonState();
    }

    @Override
    public void dispose()
    {
        if (closing)
        {
            logger.debug("Ignore dispose as there is a dispose pending");
        }
        else
        {
            disposeNow();
        }
    }

    /**
     * Dispose of the window immediately.  Should be used only for direct user
     * actions (e.g. the user closes the window); otherwise, call {@link #dispose}
     * instead.
     */
    private void disposeNow()
    {
        // Dispose of the window on the EDT thread to ensure we don't deadlock
        if (!SwingUtilities.isEventDispatchThread())
        {
            logger.debug("dispose() must be called on the EDT");
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    disposeNow();
                }
            });

            return;
        }

        // Dispose of all the peer panels
        synchronized (callPeers)
        {
            for (PeerPanel peerPanel : callPeers.values())
            {
                peerPanel.dispose();
            }
        }

        // Dispose of the buttons and UI elements that need disposing of
        // Protect against NPEs (even though these generally should not be null)
        if (recordButton != null) recordButton.dispose();
        if (holdButton != null) holdButton.dispose();
        if (audioWarningComponent != null) audioWarningComponent.dispose();
        if (microphoneButton != null) microphoneButton.dispose();
        if (volumeButton != null) volumeButton.dispose();
        if (videoButton != null) videoButton.dispose();
        if (pushButton != null) pushButton.dispose();

        for (Call call : callConference.getCalls())
        {
            call.removeCallChangeListener(callChangeListener);
        }

        // Ensure the call manager knows this call frame has been disposed of
        CallManager.callFrameDisposed(callConference);

        CallFrame.super.dispose();

        closing = false;

        callDurationTimer.stop();
        if (this.validateTimer != null) {
            this.validateTimer.stop();
            this.validateTimer = null;
        }
        dtmfTimer.stop();

        if (screensaverBlockTimer != null)
        {
            screensaverBlockTimer.stop();
        }

        // Don't show the rating window for really short calls, or if there is
        // still a call in progress (e.g. if the call was merged)
        if (!callReportSent &&
            CallManager.getInProgressCalls().isEmpty() &&
            ConfigurationUtils.showCallRating() &&
            callStartTime > 0 &&
            System.currentTimeMillis() - callStartTime > MIN_RATABLE_CALL_LENGTH)
        {
            logger.info("Show call rating since there are no other call frames open and the call was connected" +
                        "for >" + MIN_RATABLE_CALL_LENGTH + " ms");
            String title =
                resources.getI18NString("plugin.errorreport.CALL_QUALITY_TITLE",
                                        new String[]
                                        {
                                            callTitlePeers,
                                            GuiUtils.formatTime(callStartTime)
                                        });

            callReportSent = true;
            GuiActivator.getDiagsService().openErrorReportFrame(ReportReason.CALL_END, title);
        }
    }

    /**
     * The merge button will only show if there are exactly two calls in
     * progress and neither is a multi-party call. This method checks these
     * conditions and sets the merge button state accordingly
     */
    private void updateMergeButtonState()
    {
        boolean showMergeButton = true;
        String reason = "There are exactly two calls in progress";

        if (callConference.getCallPeerCount() > 1)
        {
            showMergeButton = false;
            reason = "This is a multi-peer call";
        }
        else
        {
            int peerCount = 0;

            // Show the merge button if there are precisely two calls in
            // progress and none of them are multi-party calls
            for (Call call : CallManager.getInProgressCalls())
            {
                peerCount += call.getCallPeerCount();
            }

            if (peerCount != 2)
            {
                showMergeButton = false;
                reason = "There are " + peerCount + " peers in calls";
            }
        }

        logger.debug("Setting merge button visible: " + showMergeButton + " because: " + reason);
        btnMerge.setVisible(showMergeButton);
    }

    /**
     * Adds a local video stream to this call
     *
     * @param component the component that displays the local video stream
     */
    protected void addLocalVideoComponent(final Component component)
    {
        if (component == null)
        {
            return;
        }

        logger.info("Adding local video component to call frame");

        localVideoComponent = component;

        // For some reason Mac's appear to reverse the order of the JLayered
        // pane. This is the workaround
        if (OSUtils.IS_MAC)
        {
            layeredCallPeersPane.add(component, JLayeredPane.PALETTE_LAYER);
        }
        else
        {
            layeredCallPeersPane.add(component, JLayeredPane.MODAL_LAYER);
        }

        localVideoComponent.setVisible(true);
        localVideoComponent.validate();
        layeredCallPeersPane.validate();
        validate();
    }

    /**
     * Adds a remote video stream to this call
     *
     * @param component the component that displays the remote video stream
     */
    protected void addRemoteVideoComponent(final Component component)
    {
        if (component == null)
        {
            return;
        }

        logger.info("Adding remote video component to call frame");

        remoteVideoComponent = component;

        // For some reason Mac's appear to reverse the order of the JLayered
        // pane. This is the workaround
        if (OSUtils.IS_MAC)
        {
            layeredCallPeersPane.add(component, JLayeredPane.MODAL_LAYER);
        }
        else
        {
            layeredCallPeersPane.add(component, JLayeredPane.PALETTE_LAYER);
        }

        remoteVideoComponent.setVisible(true);
        remoteVideoComponent.validate();
        layeredCallPeersPane.validate();
        validate();
    }

    /**
     * Removes a local or remote video stream to this call
     *
     * @param isLocal whether to remove the local or remote video component
     */
    public void removeVideoComponent(boolean isLocal)
    {
        Component videoComponent;
        if (isLocal && localVideoComponent != null)
        {
            logger.info("Removing local video component from call frame");
            videoComponent = localVideoComponent;
            localVideoComponent = null;
        }
        else if (!isLocal && remoteVideoComponent != null)
        {
            logger.info("Removing remote video component from call frame");
            videoComponent = remoteVideoComponent;
            remoteVideoComponent = null;
        }
        else
        {
            return;
        }

        videoComponent.setVisible(false);

        // On Mac the video component resize is animated, therefore make
        // set the video component position to the centre of itself, and
        // the width and height 0. This makes the component disappear into
        // itself
        final Rectangle bounds = new Rectangle(
            videoComponent.getX() + videoComponent.getWidth() / 2,
            videoComponent.getY() + videoComponent.getHeight() / 2,
            0, 0
        );
        videoComponent.setBounds(bounds);
        layeredCallPeersPane.remove(videoComponent);
        videoComponent.validate();

        layeredCallPeersPane.validate();
        layeredCallPeersPane.repaint();
        backgroundPanel.repaint();

        if (remoteVideoComponent == null && localVideoComponent == null)
        {
            // We have reverted to an audio only call so we need to reinstate
            // the Peer Panel and set the video button as unselected.
            callPeers.values().iterator().next().setVisible(true);
            audioWarningComponent.setEnabled(true);
            setMinimumSize(audioSize);
            setSize(audioSize);
            setResizable(false);
            resizeCallFrame();
            videoButton.setSelected(false);
            validate();
        }
    }

    /**
     * Determines the size of the given video component such that it will
     * fit within the given container dimensions without changing aspect ratio
     *
     * @param videoComponent the video component for which to determine bounds
     * @param containerWidth the width of the container for the video component
     * @param containerHeight the height of the container for the video
     * component
     *
     * @return the size of the video component
     */
    private Dimension getVideoComponentSize(Component videoComponent,
                                            int containerWidth,
                                            int containerHeight)
    {
        // Calculate the aspect ratio of the video component
        double videoAspectRatio =
                    ((double) videoComponent.getPreferredSize().width) /
                    ((double) videoComponent.getPreferredSize().height);

        // Calculate the aspect ratio of the container
        double containerAspectRatio = ((double) containerWidth) /
                                      ((double) containerHeight);

        int newWidth;
        int newHeight;
        if (videoAspectRatio > containerAspectRatio)
        {
            // Limiting dimension is width, so set the video component's width
            // to match the container and calculate the height
            newWidth = containerWidth;
            newHeight = (int) Math.floor(newWidth / videoAspectRatio);
        }
        else
        {
            // Limiting dimension is height, so set the video component's height
            // to match the container and calculate the width
            newHeight = containerHeight;
            newWidth = (int) Math.floor(newHeight * videoAspectRatio);
        }

        return new Dimension(newWidth, newHeight);
    }

    @Override
    public void validate()
    {
        // This method is called when the call frame is resized, if this is a
        // video call then we must recalculate frame positions
        if (audioSize != null && audioSize.height > 0)
        {
            resizeCallFrame(audioSize.height);
        }
        else
        {
            resizeCallFrame();
        }

        super.validate();
    }

    private void resizeCallFrame()
    {
        resizeCallFrame(DEFAULT_AUDIO_SIZE.height);
    }

    /**
     * @return the width of the audio call window according to the current
     * number of call participants.
     */
    private int getAudioCallWidth()
    {
        if (callPeers.size() <= 2)
        {
            return DEFAULT_AUDIO_SIZE.width;
        }
        else
        {
            return callPeers.size() * PeerPanel.PEER_PANEL_MULTI_PEER_WIDTH +
                   2 * ADD_PEER_IMAGE_LABEL_DIAMETER +
                   callPeers.size() * 2 + 2 +  // The borders for each component
                   callPeers.size() * 20; // SFR 541395 fix
        }
    }

    /**
     * Re-sizes all necessary components in the Call Frame to the given new
     * height.
     */
    private void resizeCallFrame(int newHeight)
    {
        if (layeredCallPeersPane == null || backgroundPanel == null)
        {
            return;
        }

        boolean isVideoCall = localVideoComponent != null ||
                              remoteVideoComponent != null;

        if (!isVideoCall)
        {
            setSize(new Dimension(getAudioCallWidth(), newHeight));
        }

        int layeredPaneWidth = getWidth() - getInsets().right - getInsets().left;
        int layeredPaneHeight = getHeight() - getInsets().top -
                                getInsets().bottom - buttonsPane.getHeight();

        Rectangle newPanelBounds = new Rectangle(0,
                                                 0,
                                                 layeredPaneWidth,
                                                 layeredPaneHeight);

        layeredCallPeersPane.setBounds(newPanelBounds);
        backgroundPanel.setBounds(newPanelBounds);

        // Set the audio warning component to be positioned in the top right
        // hand corner
        Dimension audioPanelSize = audioWarningComponent.getPreferredSize();
        audioWarningComponent.setBounds(
            new Rectangle(layeredCallPeersPane.getWidth() - audioPanelSize.width,
                          0,
                          audioPanelSize.width,
                          audioPanelSize.height));

        // The dtmf label could contain an empty string, determine the
        // height by using a '1' as a test
        int dtmfLabelHeight = (int) ComponentUtils.getStringSize(
                                dtmfLabel, "1").getHeight();
        int dtmfLabelWidth = layeredCallPeersPane.getWidth();

        if (isVideoCall)
        {
            dtmfLabel.setBounds(
               new Rectangle(0,
                             layeredCallPeersPane.getHeight() - dtmfLabelHeight,
                             dtmfLabelWidth,
                             dtmfLabelHeight));
            dtmfLabel.setVisible(true);
        }

        // Position the local video component
        if (localVideoComponent != null)
        {
            Dimension videoSize = getVideoComponentSize(localVideoComponent,
                                                        VANITY_FEED_SIZE.width,
                                                        VANITY_FEED_SIZE.height);

            int xPosition = (layeredCallPeersPane.getWidth() -
                             videoSize.width -
                             VANITY_FEED_PADDING);

            int yPosition = (layeredCallPeersPane.getHeight() -
                             videoSize.height -
                             VANITY_FEED_PADDING);

            // Due to a Mac bug we need to resize the video component for it to
            // show video. Briefly resize to 1px bigger in each direction
            localVideoComponent.setBounds(new Rectangle(xPosition,
                                                        yPosition,
                                                        videoSize.width+1,
                                                        videoSize.height+1));

            localVideoComponent.setBounds(new Rectangle(xPosition,
                                                        yPosition,
                                                        videoSize.width,
                                                        videoSize.height));
        }

        // Position the remote video component
        if (remoteVideoComponent != null)
        {
            Dimension videoSize = getVideoComponentSize(remoteVideoComponent,
                layeredCallPeersPane.getWidth(),
                layeredCallPeersPane.getHeight() - (dtmfLabelHeight*2));

            int xPosition = (layeredCallPeersPane.getWidth() - videoSize.width) / 2;
            int yPosition = (layeredCallPeersPane.getHeight() - videoSize.height) / 2;

            // Due to a Mac bug we need to resize the video component for it to
            // show video. Briefly resize to 1px bigger in each direction
            remoteVideoComponent.setBounds(new Rectangle(xPosition,
                                                         yPosition,
                                                         videoSize.width+1,
                                                         videoSize.height+1));

            remoteVideoComponent.setBounds(new Rectangle(xPosition,
                                                         yPosition,
                                                         videoSize.width,
                                                         videoSize.height));
        }
    }

    /**
     * Called when a video event has been received
     *
     * @param peerPanel the peer panel that originally received the event
     * @param evt the video event
     */
    protected void onVideoEvent(PeerPanel peerPanel, Object evt)
    {
        VideoEvent videoEvent;

        // Check this is a Video Event
        if (evt instanceof VideoEvent)
        {
            videoEvent = (VideoEvent) evt;
        }
        else
        {
            return;
        }

        int eventType = videoEvent.getType();
        int eventOrigin = videoEvent.getOrigin();

        if (eventType == VideoEvent.VIDEO_ADDED)
        {
            if (!peerPanel.canDisplayVideo())
            {
                // If the peer panel cannot display video (e.g. if the peer
                // does not support video telephony) then do nothing
                return;
            }

            if (localVideoComponent == null && remoteVideoComponent == null)
            {
                // This is an uplift to video, resize the frame
                setMinimumSize(CallFrame.VIDEO_SIZE);
                setSize(CallFrame.VIDEO_SIZE);
                layeredCallPeersPane.setBounds(getBounds());

                // We also hide the low audio warning as the transparency
                // does not obey the video components and therefore looks ugly
                audioWarningComponent.setVisible(false);
                audioWarningComponent.setEnabled(false);

                layeredCallPeersPane.add(dtmfLabel, JLayeredPane.PALETTE_LAYER);
            }

            // Dispose of the peer panel and replace it with the video panel
            callPeers.values().iterator().next().setVisible(false);
            setResizable(true);

            // Add the video component
            Component videoComponent = videoEvent.getVisualComponent();
            if (eventOrigin == VideoEvent.LOCAL)
            {
                addLocalVideoComponent(videoComponent);
            }
            else
            {
                addRemoteVideoComponent(videoComponent);
            }
        }
        else if (eventType == VideoEvent.VIDEO_REMOVED)
        {
            logger.debug("Got video removed event for origin: " + eventOrigin);
            removeVideoComponent(eventOrigin == VideoEvent.LOCAL);
        }
        else
        {
            // We might receive a Video Event for a change of resolution, in
            // which case we need to ensure the size of the call frame is
            // correct.
            resizeCallFrame();
        }
    }

    /**
     * Implements the listener which listens to events fired by the
     * CallConference depicted by this instance. Responds to peer added and
     * removed events
     */
    private class CallConferenceListener
        extends CallPeerConferenceAdapter
        implements CallChangeListener
    {
        public void callPeerAdded(CallPeerEvent ev)
        {
            addPeer(ev.getSourceCallPeer());
        }

        public void callPeerRemoved(final CallPeerEvent ev)
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    removePeer(ev.getSourceCallPeer());
                }
            });
        }

        public void callStateChanged(CallChangeEvent ev){}
    }

    @Override
    public void actionPerformed(ActionEvent evt)
    {
        Object source = evt.getSource();

        // Due to a video bug on Mac we need to validate the window when there
        // is a video component on screen
        if ((this.validateTimer != null && this.validateTimer.equals(source)) &&
            (localVideoComponent != null || remoteVideoComponent != null))
        {
            validate();
        }
        else if (dtmfTimer.equals(source))
        {
            dtmfLabel.setText("");
            dtmfTimer.stop();
        }
        else if (callDurationTimer.equals(source))
        {
            updateCallTitleState();
        }
        else if (screensaverBlockTimer != null &&
                 screensaverBlockTimer.equals(source))
        {
            try
            {
                // Move the mouse 1 pixel to prevent the screensaver
                // from activating.
                Robot robot = new Robot();
                Point point = MouseInfo.getPointerInfo().getLocation();
                robot.mouseMove(point.x + 1, point.y);
                robot.mouseMove(point.x, point.y);
            }
            catch (AWTException ex)
            {
                // Platform configuration does not allow low-level
                // input control, so we won't be able to prevent the
                // screensaver from activating.
                // Just ignore, to avoid spamming the logs with an
                // exception every minute.
                logger.info("Screen saver couldn't be blocked as"
                 + " platform doesn't allow low-level input control: "
                 + ex.getMessage());
                screensaverBlockTimer.stop();
                screensaverBlockTimer = null;
            }
        }
    }

    /**
     * Ensures that the Call Frame components fit in the Call Frame. Resizes
     * the Call Frame if necessary
     *
     * @param peerPanelHeight the height of the peer panel
     */
    public void ensureSize(int peerPanelHeight)
    {
        int newHeight = peerPanelHeight + getInsets().top +
                        getInsets().bottom + buttonsPane.getHeight() +
                        PEER_PANEL_BOTTOM_PADDING;
        logger.debug("New call frame height is: " + newHeight);

        if (newHeight > DEFAULT_AUDIO_SIZE.height)
        {
            audioSize = new Dimension(DEFAULT_AUDIO_SIZE.width, newHeight);
            setSize(audioSize);
            resizeCallFrame((int) audioSize.getHeight());
        }
    }

    /**
     * Called when DTMF has been sent from this call frame. Used to update the
     * UI elements to display the DTMF.
     *
     * @param info The DTMF info that has been sent
     */
    public void dtmfToneSent(DTMFToneInfo info)
    {
        if (localVideoComponent == null && remoteVideoComponent == null)
        {
            // This is an audio-only call so it is the responsibility of the
            // Peer Panels to display DTMF
            synchronized (callPeers)
            {
                Iterator<PeerPanel> peerPanels = callPeers.values().iterator();
                while (peerPanels.hasNext())
                {
                    PeerPanel peerPanel = peerPanels.next();
                    peerPanel.dtmfToneSent(info);
                }
            }
        }
        else
        {
            // This is a video call so the Call Frame must display the DTMF
            dtmfLabel.setText(dtmfLabel.getText() + info.keyChar);

            if (dtmfTimer.isRunning())
            {
                dtmfTimer.restart();
            }
            else
            {
                dtmfTimer.start();
            }
        }
    }

    /**
     * Whether the DTMF label is visible.
     *
     * @return whether the DTMF label is visible
     */
    public boolean isDTMFLabelVisible()
    {
        return dtmfTimer.isRunning();
    }

    /**
     * Create a new add participant label
     *
     * @param isSinglePeer whether this call is a one-to-one call
     * @return a new add participant label
     */
    protected JLabel createAddParticipantLabel(boolean isSinglePeer)
    {
        final ImageIconFuture addPeerImage;
        final ImageIconFuture addPeerImageRollover;
        final ImageIconFuture addPeerImagePressed;

        if (isSinglePeer)
        {
            addPeerImage = ADD_PEER_IMAGE_ICON;
            addPeerImageRollover = ADD_PEER_IMAGE_ICON_ROLLOVER;
            addPeerImagePressed = ADD_PEER_IMAGE_ICON_PRESSED;
        }
        else
        {
            addPeerImage = ADD_PEER_IMAGE_ICON_MULTI;
            addPeerImageRollover = ADD_PEER_IMAGE_ICON_MULTI_ROLLOVER;
            addPeerImagePressed = ADD_PEER_IMAGE_ICON_MULTI_PRESSED;
        }

        // We are using a label as a pretend button, so to keep the accessibility
        // tools happy, we need to override the role of our label
        callFrameAddPeerImageLabel = new JLabelWithOverriddenRole(AccessibleRole.PUSH_BUTTON);
        callFrameAddPeerImageLabel.setFocusable(true);
        addPeerImage.addToLabel(callFrameAddPeerImageLabel);

        // The button is an image, but we also need text for the tooltip and
        // accessibility
        String buttonText = resources.getI18NString("service.gui.CREATE_CONFERENCE_CALL");
        callFrameAddPeerImageLabel.setToolTipText(buttonText);
        AccessibilityUtils.setName(callFrameAddPeerImageLabel, buttonText);

        callFrameAddPeerImageLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                doAddParticipantAction();
            }

            @Override
            public void mousePressed(MouseEvent e)
            {
                addPeerImagePressed.addToLabel(callFrameAddPeerImageLabel);
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                addPeerImage.addToLabel(callFrameAddPeerImageLabel);
            }
        });

        callFrameAddPeerImageLabel.addFocusListener(new FocusListener()
        {
            @Override
            public void focusGained(FocusEvent evt)
            {
                addPeerImageRollover.addToLabel(callFrameAddPeerImageLabel);
            }

            @Override
            public void focusLost(FocusEvent evt)
            {
                addPeerImage.addToLabel(callFrameAddPeerImageLabel);
            }
        });

        callFrameAddPeerImageLabel.addKeyListener(new KeyListener()
        {
            @Override
            public void keyPressed(KeyEvent evt)
            {
                int keyCode = evt.getKeyCode();
                if (keyCode == KeyEvent.VK_SPACE ||
                    keyCode == KeyEvent.VK_ENTER)
                {
                    doAddParticipantAction();
                }
            }

            @Override
            public void keyReleased(KeyEvent evt){}

            @Override
            public void keyTyped(KeyEvent evt){}
        });

        return callFrameAddPeerImageLabel;
    }

    /**
     * Performs the action when the add participant button is clicked or
     * activated with the keyboard
     */
    protected void doAddParticipantAction()
    {
        logger.user("Add participant button clicked in call: " +
                            callConference);
        if (addParticipantDialog == null)
        {
            // We know there will only be one call in the call
            // conference at this point
            addParticipantDialog = new AddParticipantDialog(
                                getCallConference().getCalls().get(0));
        }

        addParticipantDialog.setVisible(true);
    }

    /**
     * Inner class that allows us to set the role of our JLabel which we are
     * pretending is a button.
     */
    private class JLabelWithOverriddenRole extends JLabel
    {
        private final AccessibleRole role;
        private AccessibleContext accessibleContext;

        public JLabelWithOverriddenRole(AccessibleRole role)
        {
            this.role = role;
        }

        @Override
        public AccessibleContext getAccessibleContext()
        {
            if (accessibleContext == null)
            {
                accessibleContext = new WrappedAccessibleJLabel();
            }
            return accessibleContext;
        }

        // This must be an inner class, since AccessibleJLabel is protected
        class WrappedAccessibleJLabel extends AccessibleJLabel
        {
            @Override
            public AccessibleRole getAccessibleRole()
            {
                return JLabelWithOverriddenRole.this.role;
            }
        }
    }
}
