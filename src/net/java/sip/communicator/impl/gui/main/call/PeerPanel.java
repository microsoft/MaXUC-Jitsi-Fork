// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import org.jitsi.service.resources.*;
import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.DTMFHandler.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.phonenumberutils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.wispaservice.WISPAAction;
import net.java.sip.communicator.service.wispaservice.WISPANamespace;
import net.java.sip.communicator.service.wispaservice.WISPAService;
import net.java.sip.communicator.util.*;

/**
 * The Peer Panel is the UI representation of one peer in this call. It
 * is responsible for passing any necessary call peer events to the main
 * call frame, e.g. display name change
 */
class PeerPanel extends JPanel implements CallPeerListener, ActionListener
{
    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(PeerPanel.class);

    private static final ImageLoaderService imageLoaderService =
                                           GuiActivator.getImageLoaderService();

    private static final PhoneNumberUtilsService phoneNumberUtils =
                                             GuiActivator.getPhoneNumberUtils();

    /**
     * The icon to use for the remove peer button
     */
    private static final ImageIconFuture CLOSE_PEER_IMAGE_ICON =
                            imageLoaderService.getImage(ImageLoaderService.CLOSE_TAB_BUTTON)
                            .getImageIcon();

    /**
     * The icon to use for the remove peer button when the mouse is over it
     */
    private static final ImageIconFuture CLOSE_PEER_IMAGE_ICON_ROLLOVER =
                   imageLoaderService.getImage(ImageLoaderService.CLOSE_TAB_BUTTON_ROLLOVER)
                   .getImageIcon();

    /**
     * The icon to use for the remove peer button when it is pressed
     */
    private static final ImageIconFuture CLOSE_PEER_IMAGE_ICON_PRESSED =
                    imageLoaderService.getImage(ImageLoaderService.CLOSE_TAB_BUTTON_PRESSED)
                    .getImageIcon();

    /**
     * The image to use as the photo frame for the peer avatar
     */
    private static final ImageIconFuture PEER_IMAGE_SURROUND =
                            imageLoaderService.getImage(ImageLoaderService.USER_PHOTO_FRAME)
                            .getImageIcon();

    /**
     * The default avatar if we don't have one for a call peer
     */
    private static final BufferedImageFuture DEFAULT_AVATAR =
                          imageLoaderService.getImage(ImageLoaderService.DEFAULT_USER_PHOTO);

    /**
     * The distance from the centre of the avatar to the centre of the peer
     * buttons
     */
    private static final int BUTTON_POSITION_RADIUS = ScaleUtils.scaleInt(35);

    /**
     * The angle of the peer chat button from the vertical
     */
    private static final int CHAT_BUTTON_ANGLE = 127;

    /**
     * The angle of the transfer call button from the vertical
     */
    private static final int TRANSFER_BUTTON_ANGLE = 83;

    /**
     * The diameter of the peer buttons
     */
    private static final int BUTTON_DIAMETER = ScaleUtils.scaleInt(24);

    /**
     * The radius of the peer buttons
     */
    private static final int BUTTON_RADIUS = BUTTON_DIAMETER / 2;

    /**
     * The diameter of the peer's avatar
     */
    public static final int PEER_DISPLAY_IMAGE_DIAMETER = ScaleUtils.scaleInt(60);

    /**
     * The radius of the peer's avatar
     */
    private static final int PEER_DISPLAY_IMAGE_RADIUS =
                                                PEER_DISPLAY_IMAGE_DIAMETER / 2;

    /**
     * The padding used above the peer's avatar
     */
    public static final int PEER_DISPLAY_IMAGE_PADDING_TOP = ScaleUtils.scaleInt(7);

    /**
     * The padding used to the left side of the peer's avatar for a
     * single-party call
     */
    private static final int PEER_DISPLAY_IMAGE_PADDING_SIDE = ScaleUtils.scaleInt(66);

    /**
     * The padding used to the left side of the peer's avatar for a
     * multi-party call
     */
    public static final int PEER_DISPLAY_IMAGE_PADDING_SIDE_MULTI_PEER = ScaleUtils.scaleInt(36);

    /**
     * The padding used to the bottom of the peer's avatar
     */
    private static final int PEER_DISPLAY_IMAGE_PADDING_BOTTOM = ScaleUtils.scaleInt(4);

    /**
     * The label used as a button to remove a call peer from a multi-party call
     */
    private static final Rectangle REMOVE_PEER_LABEL_BOUNDS =
                          new Rectangle(
                              ScaleUtils.scaleInt(26),
                              ScaleUtils.scaleInt(5),
                              ScaleUtils.scaleInt(14),
                              ScaleUtils.scaleInt(13));

    /**
     * The width of a peer panel when this is a multi-party call
     */
    public static final int PEER_PANEL_MULTI_PEER_WIDTH =
                                     PEER_DISPLAY_IMAGE_DIAMETER +
                                     2 * PEER_DISPLAY_IMAGE_PADDING_SIDE_MULTI_PEER;

    /**
     * The internal name to use for the 'other button' if it is a transfer
     * button
     */
    private static final String TRANSFER_BUTTON_NAME = "transfer";

    /**
     * The internal name to use for the 'other button' if it is a local hold
     * button
     */
    private static final String HOLD_BUTTON_NAME = "hold";

    /**
     * The resource management service
     */
    private static final ResourceManagementService resources = GuiActivator.getResources();

    private static final Color PEER_DISPLAY_FOREGROUND_COLOR = new Color(resources.getColor(
            "LIGHT_TEXT"));

    /**
     * The padding used to the left side of the peer's avatar for this panel
     */
    private final int peerDisplayImagePaddingSide;

    /**
     * The call frame that this peer panel belongs to
     */
    private final CallFrame callFrame;

    /**
     * The second button around the avatar that displays either a transfer
     * button or a local hold button
     */
    private InCallButton btnPeerOther;

    /**
     * How long to disable the hold button for after it has been clicked.
     * Disabling the button prevents us sending overlapping or wrong INVITEs and
     * then being rejected by the server.  (We could fix this properly in JSIP
     * but disabling the button is a lot easier.)
     */
    private static final long TOGGLE_DISABLE_TIME_MS = 300;

    /**
     * The left hand padding for the add peer image label
     */
    private static final int ADD_PEER_IMAGE_LABEL_PADDING_LEFT = ScaleUtils.scaleInt(26);

    /**
     * Timer for the {@link #disableTimerTask}.
     */
    private java.util.Timer disableTimer;

    /**
     * <tt>TimerTask</tt> that tracks when the hold button has been clicked,
     * and disables it for {@link #TOGGLE_DISABLE_TIME_MS}.
     */
    private TimerTask disableTimerTask;

    /**
     * When <tt>true</tt>, the hold button has just been clicked, and is
     * temporarily disabled for a short while to prevent the user from spamming
     * the server with overlapping INVITEs.
     * <p>
     * We use this flag rather than disabling the component for two reasons.
     * <li>it means the button can keep focus (otherwise focus moves to the next
     * element when this once is disabled)
     * <li>we don't want to change the icon when we're temporarily disabled -
     * the disable period is short enough that changing the icon just results in
     * a weird flickery effect.
     */
    private boolean togglingPeerHoldButton;

    /**
     * The call peer associated with this peer panel
     */
    private final CallPeer callPeer;

    /**
     * The <tt>Observer</tt> which listens to {@link #uiVideoHandler} about
     * changes in the video-related information.
     */
    private final Observer uiVideoHandlerObserver = new Observer()
        {
            @Override
            public void update(Observable o, Object arg)
            {
                callFrame.onVideoEvent(PeerPanel.this, arg);
            }
        };

    /**
     * The UI Video Handler used to listen for video events
     */
    private final UIVideoHandler2 uiVideoHandler;

    /**
     * The label that displays the peer's name, or DTMF if it is being sent
     */
    private JLabel peerDisplayNameLabel;

    /** Used to create and refresh the CRM button for the peer. */
    private final CrmButtonSetter mCrmButtonSetter;

    /**
     * The timer used to show/hide the DTMF label
     */
    private javax.swing.Timer dtmfTimer = new javax.swing.Timer(CallFrame.DTMF_SHOW_DURATION, this);

    /**
     * The width of this peer display panel
     */
    private final int peerDisplayPanelWidth;

    /** The height of the peer Image panel. */
    private static final int PEER_DISPLAY_IMAGE_PANEL_HEIGHT =
            PEER_DISPLAY_IMAGE_DIAMETER +
            PEER_DISPLAY_IMAGE_PADDING_TOP +
            PEER_DISPLAY_IMAGE_PADDING_BOTTOM;

    private static final WISPAService sWISPAService = GuiActivator.getWISPAService();

    /**
     * Creates a new peer panel for the given call frame and peer
     *
     * @param callFrame the call frame that created us
     * @param peer the peer that this panel represents
     * @param isMultiPeerCall whether this is a multi-party call
     */
    public PeerPanel(final CallFrame callFrame,
                     CallPeer peer,
                     boolean isMultiPeerCall)
    {
        logger.info("Adding peer panel to call frame: " + peer);

        this.callFrame = callFrame;
        callPeer = peer;
        callPeer.addCallPeerListener(this);
        handleStateChange(callPeer.getState());

        uiVideoHandler = new UIVideoHandler2(this.callFrame.getCallConference());
        uiVideoHandler.addObserver(uiVideoHandlerObserver);

        // Define all measurements for this peer panel
        // The peer display image panel
        peerDisplayImagePaddingSide = (isMultiPeerCall) ?
                                    PEER_DISPLAY_IMAGE_PADDING_SIDE_MULTI_PEER :
                                    PEER_DISPLAY_IMAGE_PADDING_SIDE;

        peerDisplayPanelWidth = (isMultiPeerCall) ?
                 PEER_PANEL_MULTI_PEER_WIDTH:
                 (2 * CallFrame.ADD_PEER_IMAGE_LABEL_DIAMETER) + PEER_DISPLAY_IMAGE_DIAMETER + 40;

        setOpaque(false);
        setLayout(new BorderLayout(0, 0));

        // Add the name frame - it may contain text and buttons
        JPanel peerDisplayNamePanel = new JPanel();
        peerDisplayNamePanel.setOpaque(false);
        add(peerDisplayNamePanel, BorderLayout.SOUTH);
        peerDisplayNamePanel.setLayout(new BorderLayout(0, 0));

        // Get the peer's display name.  This may be a phone number so format
        // it nicely before displaying it. If it isn't a valid phone number,
        // this method will just return the string unchanged.
        String displayName =
            phoneNumberUtils.formatNumberForDisplay(callPeer.getDisplayName());

        // Create and add the text label to the name frame
        // Disable HTML to prevent HTML injection
        peerDisplayNameLabel = new JLabel();
        ScaleUtils.scaleFontAsDefault(peerDisplayNameLabel);
        peerDisplayNameLabel.putClientProperty("html.disable", Boolean.TRUE);
        peerDisplayNameLabel.setText(displayName);
        peerDisplayNamePanel.add(peerDisplayNameLabel, BorderLayout.NORTH);
        peerDisplayNamePanel.validate();
        peerDisplayNameLabel.setForeground(PEER_DISPLAY_FOREGROUND_COLOR);

        // Add the section for the CRM button/CRM progress indication - it will
        // be empty if neither type of CRM service is available.
        JPanel crmButtonPanel = new JPanel();
        crmButtonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0 , 0));
        crmButtonPanel.setOpaque(false);
        peerDisplayNamePanel.add(crmButtonPanel, BorderLayout.CENTER);

        // Get the peer address for CRM phone number search.
        String peerAddress = callPeer.getAddress();

        // Create the CRM button setter and try to create the CRM button.
        // If a button can be created, it will be inserted into the
        // crmButtonPanel.
        //
        // N.B. - this will always create a CRM button (barring any further
        // lower level restrictions).
        mCrmButtonSetter = new CrmButtonSetter(callPeer.getDisplayName(),
                                               peerAddress,
                                               crmButtonPanel);
        mCrmButtonSetter.createButton();

        setCrmButtonStyle();

        validateDisplayNameSize();

        // Add the peer image frame
        JLayeredPane peerDisplayImagePanel = new JLayeredPane();
        peerDisplayImagePanel.setOpaque(false);
        add(peerDisplayImagePanel, BorderLayout.CENTER);

        peerDisplayImagePanel.setPreferredSize(new Dimension(
                peerDisplayPanelWidth,
                PEER_DISPLAY_IMAGE_PANEL_HEIGHT));

        if (isMultiPeerCall)
        {
            initializeMultiPeerUI(peerDisplayImagePanel);
        }

        // Get the peer's avatar
        BufferedImageFuture peerAvatar = callPeer.getImage();
        if (peerAvatar == null)
        {
            if (callPeer.getMetaContact() != null)
            {
                peerAvatar = callPeer.getMetaContact().getAvatar();
            }

            if (peerAvatar == null)
            {
                peerAvatar = DEFAULT_AVATAR;
            }
        }

        // Add the peer's avatar
        ImageIconFuture scaledPeerImage =
                ImageUtils.getScaledCircularIcon(peerAvatar,
                                                 PEER_DISPLAY_IMAGE_DIAMETER,
                                                 PEER_DISPLAY_IMAGE_DIAMETER);

        JLabel peerDisplayImage = scaledPeerImage.addToLabel(new JLabel());
        peerDisplayImagePanel.add(peerDisplayImage, JLayeredPane.DEFAULT_LAYER);
        peerDisplayImage.setBounds(peerDisplayImagePaddingSide,
                                   PEER_DISPLAY_IMAGE_PADDING_TOP,
                                   PEER_DISPLAY_IMAGE_DIAMETER,
                                   PEER_DISPLAY_IMAGE_DIAMETER);

        // Add the photo surround to the palette layer so it is drawn on top
        // of the avatar
        JLabel peerDisplaySurround = PEER_IMAGE_SURROUND.addToLabel(new JLabel());
        peerDisplayImagePanel.add(peerDisplaySurround, JLayeredPane.PALETTE_LAYER);
        peerDisplaySurround.setBounds(peerDisplayImagePaddingSide,
                                      PEER_DISPLAY_IMAGE_PADDING_TOP,
                                      PEER_DISPLAY_IMAGE_DIAMETER,
                                      PEER_DISPLAY_IMAGE_DIAMETER);

        // Create the two buttons that appear around the avatar
        createChatButton(peerDisplayImagePanel);
        createOtherButton(peerDisplayImagePanel, isMultiPeerCall);

        // Create the add participant button
        if (!isMultiPeerCall)
        {
            JLabel addPeerImageLabel = callFrame.createAddParticipantLabel(true);

            addPeerImageLabel.setBounds(
                                    ADD_PEER_IMAGE_LABEL_PADDING_LEFT,
                                    PeerPanel.PEER_DISPLAY_IMAGE_PADDING_TOP,
                                    PeerPanel.PEER_DISPLAY_IMAGE_DIAMETER,
                                    PeerPanel.PEER_DISPLAY_IMAGE_DIAMETER);

            peerDisplayImagePanel.add(addPeerImageLabel,
                                      JLayeredPane.DEFAULT_LAYER);
        }

        // Ensure that the call frame is tall enough to accommodate this Peer
        // Panel.
        callFrame.ensureSize(getPanelHeight());

        disableTimer = new java.util.Timer("Peer panel hold button disable timer");
        togglingPeerHoldButton = false;
    }

    /**
     * Ensures that the peer display name label is not too large.
     */
    private void validateDisplayNameSize()
    {
        String displayName = peerDisplayNameLabel.getText();

        // Set a maximum size of the peer display name label
        int peerDisplayNameMaxWidth = (int) Math.floor(peerDisplayPanelWidth * 0.9);
        int peerDisplayNameWidth = (int) ComponentUtils.getStringSize(
                                 peerDisplayNameLabel, displayName).getWidth();
        int peerDisplayNameHeight = (int) ComponentUtils.getStringSize(
                                peerDisplayNameLabel, displayName).getHeight();

        Dimension peerDisplayNameSize = new Dimension(
                        Math.min(peerDisplayNameMaxWidth, peerDisplayNameWidth),
                        peerDisplayNameHeight);

        // Set the size of the peer display name label
        peerDisplayNameLabel.setPreferredSize(peerDisplayNameSize);
        peerDisplayNameLabel.setMaximumSize(peerDisplayNameSize);
        peerDisplayNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
    }

    /**
     * @return The required height of this panel, accounting for the CRM button
     * (or progress indicator string) if appropriate.
     */
    private int getPanelHeight()
    {
        int peerNameHeight = (int) ComponentUtils.getStringSize(
            peerDisplayNameLabel, peerDisplayNameLabel.getText()).getHeight();

        // Always count the height of the image panel showing the user's avatar,
        // and the height of the user's DN/name display.
        int panelHeight = peerNameHeight + PEER_DISPLAY_IMAGE_PANEL_HEIGHT;

        // Include the height of the CRM button panel.
        panelHeight += mCrmButtonSetter.getButtonHeight();

        return panelHeight;
    }

    /**
     * Configures the UI components required for a multi-peer call
     *
     * @param peerDisplayImagePanel The parent panel for all the multi-peer UI
     * components
     */
    private void initializeMultiPeerUI(JLayeredPane peerDisplayImagePanel)
    {
        // Add the 'x' if this is a multi-peer call
        final JLabel closePeerLabel = new JLabel();
        CLOSE_PEER_IMAGE_ICON.addToLabel(closePeerLabel);
        closePeerLabel.setFocusable(true);

        closePeerLabel.addMouseListener(new MouseListener(){
            @Override
            public void mouseClicked(MouseEvent arg0)
            {
                CallManager.hangupCallPeer(callPeer);
                PeerPanel.this.callFrame.removePeer(callPeer);
            }

            @Override
            public void mouseEntered(MouseEvent arg0)
            {
                CLOSE_PEER_IMAGE_ICON_ROLLOVER.addToLabel(closePeerLabel);
            }

            @Override
            public void mouseExited(MouseEvent arg0)
            {
                CLOSE_PEER_IMAGE_ICON.addToLabel(closePeerLabel);
            }

            @Override
            public void mousePressed(MouseEvent arg0)
            {
                CLOSE_PEER_IMAGE_ICON_PRESSED.addToLabel(closePeerLabel);
            }

            @Override
            public void mouseReleased(MouseEvent arg0)
            {
                CLOSE_PEER_IMAGE_ICON.addToLabel(closePeerLabel);
            }
        });

        closePeerLabel.addKeyListener(new KeyAdapter(){
            @Override
            public void keyPressed(KeyEvent evt)
            {
                if (evt.getKeyCode() == KeyEvent.VK_SPACE ||
                    evt.getKeyCode() == KeyEvent.VK_ENTER)
                {
                    CallManager.hangupCallPeer(callPeer);
                    PeerPanel.this.callFrame.removePeer(callPeer);
                }
            }
        });

        closePeerLabel.addFocusListener(new FocusListener(){
            @Override
            public void focusGained(FocusEvent evt)
            {
                CLOSE_PEER_IMAGE_ICON_ROLLOVER.addToLabel(closePeerLabel);
            }

            @Override
            public void focusLost(FocusEvent evt)
            {
                CLOSE_PEER_IMAGE_ICON.addToLabel(closePeerLabel);
            }
        });

        peerDisplayImagePanel.add(closePeerLabel, JLayeredPane.PALETTE_LAYER);
        closePeerLabel.setBounds(REMOVE_PEER_LABEL_BOUNDS);
    }

    /**
     * Determines whether this call is in a suitable state for video. This
     * means that there can only be one peer.
     *
     * @return whether this call can be used for video
     */
    public boolean canDisplayVideo()
    {
        if (callFrame.getCallConference().getCallPeers().size() == 1)
        {
            return true;
        }

        return false;
    }

    /**
     * Creates the chat button that appears on the edge of the avatar
     *
     * @param peerDisplayImagePanel the panel to add this button to
     */
    private void createChatButton(JLayeredPane peerDisplayImagePanel)
    {
        if (callPeer.getMetaContact() == null ||
            !callPeer.getMetaContact().canBeMessaged())
        {
            // Cannot chat if there is no MetaContact, or if the MetaContact
            // does not support messaging so do not create the button
            return;
        }

        // Create a chat button
        InCallButton btnPeerChat = new InCallButton(
            ImageLoaderService.CHAT_AVATAR_BUTTON,
            ImageLoaderService.CHAT_AVATAR_BUTTON_ROLLOVER,
            ImageLoaderService.CHAT_AVATAR_BUTTON_FOCUS,
            ImageLoaderService.CHAT_AVATAR_BUTTON_PRESSED,
            null,
            null,
            ImageLoaderService.CHAT_AVATAR_BUTTON_FOCUS,
            ImageLoaderService.CHAT_AVATAR_BUTTON_DISABLED,
            resources.getI18NString("service.gui.CHAT"));

        initializeAvatarButton(btnPeerChat, CHAT_BUTTON_ANGLE);

        btnPeerChat.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                logger.user("Chat selected in call window");
                MetaContact metaContact = callPeer.getMetaContact();
                sWISPAService.notify(WISPANamespace.CONTACTS, WISPAAction.MOTION, metaContact);
            }
        });

        peerDisplayImagePanel.add(btnPeerChat, JLayeredPane.MODAL_LAYER);
    }

    /**
     * Configures the given avatar button by positioning and styling it
     *
     * @param btnPeerChat the button to initialise
     * @parm btnAngle the angle at which this button is from the centre of the
     * avatar.
     */
    private void initializeAvatarButton(InCallButton btnPeerChat, int btnAngle)
    {
        // Determine the x and y co-ordinates of the button relative to the
        // centre of the avatar
        int buttonX = (int)Math.floor(BUTTON_POSITION_RADIUS *
            Math.sin((btnAngle * Math.PI) / 180));
        int buttonY = (int)Math.floor(BUTTON_POSITION_RADIUS *
            Math.cos((btnAngle * Math.PI) / 180));

        // Now translate the button co-ordinates to the panel we are adding to
        int buttonLocX = peerDisplayImagePaddingSide +
            PEER_DISPLAY_IMAGE_RADIUS + buttonX - BUTTON_RADIUS;
        int buttonLocY = PEER_DISPLAY_IMAGE_PADDING_TOP +
            PEER_DISPLAY_IMAGE_RADIUS - buttonY - BUTTON_RADIUS;

        btnPeerChat.setBounds(new Rectangle(buttonLocX, buttonLocY, BUTTON_DIAMETER, BUTTON_DIAMETER));
        btnPeerChat.setOpaque(false);
        btnPeerChat.setBorderPainted(false);
        btnPeerChat.setPreferredSize(new Dimension(BUTTON_DIAMETER, BUTTON_DIAMETER));
        btnPeerChat.setBackground(new Color(0, 0, 0, 0));
    }

    /**
     * Creates the second button that appears on the edge of the avatar. This
     * button changes depending on whether this is a single or multi party call
     *
     * @param peerDisplayImagePanel the panel to add this button to
     * @param isMultiPeerCall whether this call contains more than 1 peer
     */
    private void createOtherButton(JLayeredPane peerDisplayImagePanel,
                                   boolean isMultiPeerCall)
    {
        if (!isMultiPeerCall)
        {
            // The call frame call conference may have once been a multi-party
            // call, so we need to determine the correct call to use for the
            // transfer call button. This is the call that has a peer
            btnPeerOther = new TransferCallButton(callFrame.getCallConference());
            btnPeerOther.setName(TRANSFER_BUTTON_NAME);
        }
        else
        {
            // Create a hold peer button
            btnPeerOther = new InCallButton(
                ImageLoaderService.HOLD_AVATAR_BUTTON,
                ImageLoaderService.HOLD_AVATAR_BUTTON_ROLLOVER,
                ImageLoaderService.HOLD_AVATAR_BUTTON_FOCUS,
                ImageLoaderService.HOLD_AVATAR_BUTTON_PRESSED,
                ImageLoaderService.HOLD_AVATAR_BUTTON_ON,
                ImageLoaderService.HOLD_AVATAR_BUTTON_ON_ROLLOVER,
                ImageLoaderService.HOLD_AVATAR_BUTTON_ON_FOCUS,
                ImageLoaderService.HOLD_AVATAR_BUTTON_DISABLED,
                resources.getI18NString("service.gui.HOLD_BUTTON_TOOL_TIP"));

            btnPeerOther.setName(HOLD_BUTTON_NAME);

            btnPeerOther.addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent arg0)
                {
                    // If the button has been recently clicked, ignore this
                    // click.
                    if (togglingPeerHoldButton)
                    {
                        logger.debug("Ignore peer hold button click while recently clicked");
                        return;
                    }

                    // Button has been clicked; disable it for a short while to
                    // prevent double-clicks / spamming.
                    togglingPeerHoldButton = true;
                    if (disableTimerTask != null)
                    {
                        logger.debug("Re-enable peer hold button");
                        disableTimerTask.cancel();
                    }
                    disableTimerTask = new TimerTask() {
                        @Override
                        public void run()
                        {
                            togglingPeerHoldButton = false;
                        }};
                    disableTimer.schedule(disableTimerTask, TOGGLE_DISABLE_TIME_MS);

                    // Now toggle the state
                    logger.debug("Toggle peer hold state");
                    boolean setOnHold = !btnPeerOther.isSelected();
                    setOnHold(setOnHold);
                    btnPeerOther.setSelected(setOnHold);
                }
            });

            // When the hold button is created, it defaults to off (not being on
            // hold). The peer may actually be on hold, so update the hold
            // button to reflect peer's actual hold state.
            updateLocalHoldButton();
        }

        initializeAvatarButton(btnPeerOther, TRANSFER_BUTTON_ANGLE);

        peerDisplayImagePanel.add(btnPeerOther, JLayeredPane.MODAL_LAYER);
    }

    @Override
    public void peerStateChanged(CallPeerChangeEvent evt)
    {
        logger.debug("Peer state changed " + evt);

        // The peer that this event concerns
        CallPeerState newState = (CallPeerState) evt.getNewValue();
        handleStateChange(newState);
    }

    /**
     * Handle the state change of the call peer
     *
     * @param newState the new state of the peer.
     */
    private void handleStateChange(CallPeerState newState)
    {
        callFrame.updateCallTitleState(callPeer, newState);

        // The peer's hold state may have changed, so update the local hold
        // button to make sure it's up to date.
        updateLocalHoldButton();

        // Alert all other call panels in case they need to update their UI
        CallManager.forwardCallEventToCallPanels(null);
    }

    /**
     * Updates the local hold button to reflect the hold state of the call peer.
     */
    private void updateLocalHoldButton()
    {
        logger.debug("Updating peer's local hold button");
        boolean onHold = false;

        // Work out whether the call peer is on hold
        if (callPeer.getState() == CallPeerState.ON_HOLD_LOCALLY ||
            callPeer.getState() == CallPeerState.ON_HOLD_MUTUALLY)
        {
            onHold = true;
        }

        // If the peer's button is a local hold button, update it with the hold
        // state determined from the peer.
        if (btnPeerOther != null &&
            btnPeerOther.getName().equals(HOLD_BUTTON_NAME))
        {
            // This method handles the case where we set to the same value as
            // the current state, so don't need to check here.
            btnPeerOther.setSelected(onHold);
            logger.debug("Setting local hold button " + (onHold ? "on": "off"));
        }
    }

    @Override
    public void peerDisplayNameChanged(CallPeerChangeEvent evt)
    {
        // Update the call peer display name label if it is not currently
        // showing DTMF
        if (!callFrame.isDTMFLabelVisible())
        {
            peerDisplayNameLabel.setText(
                phoneNumberUtils.formatNumberForDisplay(callPeer.getDisplayName()));
            validateDisplayNameSize();
        }

        callFrame.updateCallTitlePeers();

        setCrmButtonStyle();

        // Update the call frame.
        callFrame.ensureSize(getPanelHeight());
        callFrame.revalidate();
        callFrame.repaint();
    }

    @Override
    public void peerAddressChanged(CallPeerChangeEvent evt){}

    @Override
    public void peerTransportAddressChanged(CallPeerChangeEvent evt){}

    @Override
    public void peerImageChanged(CallPeerChangeEvent evt){}

    /**
     * Sets this peer on hold
     *
     * @param isOnHold whether to set this peer on hold or take them off hold
     */
    void setOnHold(boolean isOnHold)
    {
        logger.info("Set " + callPeer + (isOnHold ? " on" : " off") + " hold");
        Call call = callPeer.getCall();
        OperationSetBasicTelephony<?> telephony
            = call.getProtocolProvider().getOperationSet(
                    OperationSetBasicTelephony.class);

        try
        {
            if (isOnHold)
            {
                telephony.putOnHold(callPeer);
            }
            else
            {
                telephony.putOffHold(callPeer);
            }
        }
        catch (OperationFailedException ex)
        {
            logger.error("Failed to put " + callPeer.getAddress() +
                         (isOnHold ? "on" : "off") + " hold.", ex);
        }
    }

    /**
     * Disposes of any resources or listeners
     */
    public void dispose()
    {
        logger.info("Disposing of peer panel: " + callPeer);

        // Need to remove the PeerPanel as a listener to this peer
        callPeer.removeCallPeerListener(this);

        // Cancel the timers too.
        disableTimer.cancel();
        dtmfTimer.stop();
    }

    /**
     * Gets the local hold button if it is showing
     *
     * @return the local hold button if it showing
     */
    public InCallButton getLocalHoldButton()
    {
        // Check whether the hold button is showing
        if (btnPeerOther.getName().equals(HOLD_BUTTON_NAME))
        {
            return btnPeerOther;
        }

        return null;
    }

    /**
     * Called when a DTMF tone has been dispatched. This allows the UI to be
     * updated accordingly.
     *
     * @param info The DTMF Tone Info that was sent
     */
    public void dtmfToneSent(DTMFToneInfo info)
    {
        char dtmfTone = info.keyChar;

        if (dtmfTimer.isRunning())
        {
            peerDisplayNameLabel.setText(peerDisplayNameLabel.getText() + dtmfTone);
            dtmfTimer.restart();
        }
        else
        {
            peerDisplayNameLabel.setText(Character.toString(dtmfTone));
            dtmfTimer.start();
        }

        // We have changed the contents of the peer display name label so the
        // size must be validated to ensure it is not too large
        validateDisplayNameSize();
    }

    @Override
    public void actionPerformed(ActionEvent evt)
    {
        if (dtmfTimer.equals(evt.getSource()))
        {
            peerDisplayNameLabel.setText(
                phoneNumberUtils.formatNumberForDisplay(callPeer.getDisplayName()));
            validateDisplayNameSize();
            dtmfTimer.stop();
        }
    }

    // Set Crm button text color to match style of in-call UI
    private void setCrmButtonStyle() {
        if (mCrmButtonSetter.getButton() != null) {
            mCrmButtonSetter.getButton().setForeground(PEER_DISPLAY_FOREGROUND_COLOR);
        }
    }
}
