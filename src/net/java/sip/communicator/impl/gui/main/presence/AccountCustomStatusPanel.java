// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.presence;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.notification.NotificationService.HandlerAddedListener;
import net.java.sip.communicator.service.notification.SoundNotificationHandler.MuteListener;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.service.protocol.globalstatus.GlobalStatusService.GlobalStatusChangeListener;
import net.java.sip.communicator.util.account.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

/**
 * Panel used to display and edit the custom status. It toggles between edit
 * mode, where the custom status can be updated, and view mode which displays
 * the full status
 */
public class AccountCustomStatusPanel extends TransparentPanel
{
    private static final long serialVersionUID = 0L;

    private static final Logger logger =
                               Logger.getLogger(AccountCustomStatusPanel.class);

    /**
     * The icon displayed on the status label if the client is muted
     */
    private final ImageIconFuture muteIcon =
        GuiActivator.getImageLoaderService().getImage(
                                       ImageLoaderService.NOTIFICATIONS_MUTED).getImageIcon();

    /**
     * The label displaying the muteIcon
     */
    private final JLabel muteLabel = new JLabel("");

    /**
     * The label displaying the current full status (Global - Custom) when not
     * editing
     */
    private final JLabel statusLabel = new JLabel("");

    /**
     * The text entry panel displayed when in edit mode
     */
    private SIPCommSnakeTextEntry editPanel;

    private static final Color TEXT_COLOR =
        Constants.MAIN_FRAME_HEADER_TEXT_COLOR;
    /**
     * The GlobalStatusService so we can listen to changes to status
     */
    private final GlobalStatusService globalStatusService =
                                          GuiActivator.getGlobalStatusService();

    /**
     * A global mouse listener to see if we have clicked out side the editPanel
     *
     * This is required because the editPanel doesn't lose focus if something is
     * clicked on which doesn't gain focus
     *
     * It is added/removed when the editPanel is shown/hidden
     */
    private final AWTEventListener globalMouseListener = new AWTEventListener()
    {
        @Override
        public void eventDispatched(AWTEvent event)
        {
            if(event instanceof MouseEvent)
            {
                MouseEvent evt = (MouseEvent)event;
                if(evt.getID() == MouseEvent.MOUSE_CLICKED)
                {
                    // If the mouse click is within the edit panel, we shouldn't
                    // close it
                    if(!editPanel.getBounds().contains(evt.getPoint()))
                    {
                        logger.user("Clicked outside custom edit panel");
                        hideEditPanel();
                    }
                }
            }
        }
    };

    /**
     * The action listener to be added to the editPanel. This is used to save
     * and exit edit mode when Enter is pressed
     */
    private final ActionListener editPanelActionListener = new ActionListener()
    {
        @Override
        public void actionPerformed(ActionEvent event)
        {
            logger.debug("editPanel action event received");
            hideEditPanel();
        }
    };

    /**
     * The focus listener to be added to the editPanel. This is used to save
     * and exit edit mode when it loses focus
     */
    private final FocusListener editPanelFocusListener = new FocusAdapter()
    {
        @Override
        public void focusLost(FocusEvent e)
        {
            logger.debug("editPanel lost focus");
            save();
            hideEditPanel();
        }
    };

    /**
     * The mouse listener to be added to the editPanel. This is used to save
     * and exit edit mode when the mouse exits the panel
     */
    private final MouseListener editPanelMouseListener = new MouseAdapter()
    {
        @Override
        public void mouseExited(MouseEvent event)
        {
            logger.debug("Mouse exited editPanel");
            handleMouseExit(event);
        }
    };

    /**
     * The key listener to be added to the editPanel. This is used to undo what
     * was entered and exit edit mode
     */
    private final KeyListener editPanelKeyListener = new KeyAdapter()
    {
        @Override
        public void keyReleased(KeyEvent e)
        {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
            {
                logger.user("Custom status change cancelled");
                editPanel.setText(globalStatusService.getCustomStatus());
                hideEditPanel();
            }
        }
    };

    public AccountCustomStatusPanel()
    {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        // Add set the muteLabel icon and set invisible to begin with
        muteIcon.addToLabel(muteLabel);
        muteLabel.setVisible(false);

        // Create our text entry for the custom text and hide it
        editPanel = new SIPCommSnakeTextEntry(
            globalStatusService.getCustomStatus(),
            TEXT_COLOR,
            new Dimension(getWidth(), 10),
            "service.gui.presence.CUSTOM_PRESENCE");

        editPanel.setVisible(false);
        editPanel.setForeground(TEXT_COLOR);

        // Use a slightly smaller font
        Font font = getFont().deriveFont(ScaleUtils.getScaledFontSize(11f));

        // Position the status label so it is displayed precisely under the
        // text entry field
        Border paddingBorder = ScaleUtils.createEmptyBorder(3, 6, 3, 0);
        if (OSUtils.IS_MAC)
        {
            paddingBorder = ScaleUtils.createEmptyBorder(3, 6, 4, 0);
        }
        statusLabel.setBorder(paddingBorder);
        statusLabel.setFont(font);
        statusLabel.setForeground(TEXT_COLOR);

        // Add a listener for the global status to update the displayed text
        globalStatusService.addStatusChangeListener(
            new GlobalStatusChangeListener()
        {
            @Override
            public void onStatusChanged()
            {
                // Get the string to display
                statusLabel.setText(globalStatusService.getStatusMessage());
                editPanel.setText(globalStatusService.getCustomStatus());

                GlobalStatusEnum status = globalStatusService.getGlobalStatus();
                editPanel.setLabelText(
                    GlobalStatusEnum.getI18NStatusName(status) + " - ");
            }
        });

        // Add Mute Listeners to allow us to change the mute icon when the
        // mute state changes
        GuiActivator.getNotificationService().addHandlerAddedListener(
            new HandlerAddedListener()
            {
                @Override
                public void onHandlerAdded(NotificationHandler handler)
                {
                    logger.debug("New sound handler added");
                    addMuteListener(handler);
                }
            }
        );

        for (NotificationHandler handler :
                        GuiActivator.getNotificationService()
                            .getActionHandlers(NotificationAction.ACTION_SOUND))
        {
            addMuteListener(handler);
        }

        // Set up the editPanel
        editPanel.setFont(font);
        editPanel.setPlaceholder(GuiActivator.getResources().
            getI18NString("service.protocol.status.CUSTOM_STATUS_PLACEHOLDER"));

        resizeEditPanel();

        // Add the various listeners to the editPanel
        editPanel.addActionListener(editPanelActionListener);
        editPanel.addFocusListener(editPanelFocusListener);
        editPanel.addMouseListener(editPanelMouseListener);
        editPanel.addKeyListener(editPanelKeyListener);

        // Add a mouse listener to handle showing/hiding edit mode when mouse
        // enters or exits this panel
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseExited(MouseEvent event)
            {
                handleMouseExit(event);
            }

            @Override
            public void mouseEntered(MouseEvent event)
            {
                logger.debug("Mouse entered custom status panel");
                showEditPanel();
            }
        });

        // Add a component listener so that we can resize the editPanel when
        // the window resizes. We have to do this as the editPanel is a
        // JLayeredPane which has no layout manager
        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                resizeEditPanel();
            }
        });

        // Finally add all the components to the panel
        add(muteLabel);
        add(statusLabel);
        add(editPanel);
    }

    /**
     * Show the edit panel
     */
    private void showEditPanel()
    {
        // If we don't have a IM account or we are Offline, don't show the edit
        // panel
        AccountID imAccount = AccountUtils.getImAccount();
        GlobalStatusEnum status = globalStatusService.getGlobalStatus();
        if (imAccount == null || globalStatusService.isOffline())
            return;

        logger.debug("Show the custom status edit panel");
        // Add the global mouse listener to listen for clicks
        Toolkit.getDefaultToolkit().addAWTEventListener(
            globalMouseListener,
            AWTEvent.MOUSE_EVENT_MASK);

        // Hide the statusLabel and show the editPanel
        statusLabel.setVisible(false);
        editPanel.setVisible(true);
    }

    /**
     * Hide the edit panel
     */
    private void hideEditPanel()
    {
        logger.debug("Hide the custom status edit panel");
        // Remove the global mouse listener
        Toolkit.getDefaultToolkit().removeAWTEventListener(globalMouseListener);

        // Hide the statusLabel and show the editPanel
        editPanel.setVisible(false);
        statusLabel.setVisible(true);
    }

    /**
     * Save the current value in the editPanel
     */
    private void save()
    {
        String status = editPanel.getText();
        globalStatusService.setCustomStatus(status);
        logger.user("Custom status saved");
    }

    /**
     * Handle a mouse exiting event
     * @param me the MouseEvent of the mouse exiting
     */
    private void handleMouseExit(MouseEvent me)
    {
        if (!editPanel.contains(me.getPoint()) &&
            !editPanel.isFocusOwner())
        {
            logger.info("Mouse exited the editPanel, hide the panel");
            hideEditPanel();
        }
    }

    /**
     * Resize the edit panel based of this panels width
     */
    private void resizeEditPanel()
    {
        int width = getWidth();

        // If the muteLabel is visible we need to account for it
        width -= muteLabel.isVisible() ? muteLabel.getWidth() : 0;
        editPanel.setPreferredSize(new Dimension(width, 0));
    }

    /**
     * Check the current mute state and listen to mute state changes if provided
     * a SoundNotificationHandler
     * @param handler
     */
    private void addMuteListener(NotificationHandler handler)
    {
        if(handler instanceof SoundNotificationHandler)
        {
            logger.debug("Adding mute listeners for File Menu");

            SoundNotificationHandler soundHandler
                = (SoundNotificationHandler) handler;

            muteLabel.setVisible(soundHandler.isMute());
            resizeEditPanel();

            // Also need to add a listener on the handler itself so we update
            // when the mute state changes
            soundHandler.addMuteListener(new MuteListener()
            {
                @Override
                public void onMuteChanged(boolean newMute)
                {
                    logger.debug(
                          (newMute ? "Displaying" : "Removing") + " mute icon");
                    muteLabel.setVisible(newMute);
                    resizeEditPanel();
                }
            });
        }
    }
}
