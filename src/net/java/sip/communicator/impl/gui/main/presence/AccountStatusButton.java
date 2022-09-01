// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.presence;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.service.protocol.globalstatus.GlobalStatusService.*;
import net.java.sip.communicator.util.AccessibilityUtils;
import net.java.sip.communicator.util.account.*;

/**
 * The account status button is a button that allows the user to update the top
 * level status of their account. I.e. they can use it to set themself as "busy"
 *
 * It also displays an icon representing the current status thus it must be
 * registered as a global status change listener
 */
public class AccountStatusButton extends SIPCommSnakeButton
                           implements ActionListener, GlobalStatusChangeListener
{
    private static final Logger sLog = Logger.getLogger(AccountStatusButton.class);

    // Text color on a Mac must always be black as the background color
    // is not brandable.
    private static final Color TEXT_COLOR =
        Constants.MAIN_FRAME_HEADER_TEXT_COLOR;

    private static final long serialVersionUID = 1L;
    private static final ResourceManagementService sRes = GuiActivator.getResources();

    // Strings we use
    private static final String PRESENCE_TITLE = sRes.getI18NString("service.protocol.status.gui.PRESENCE_MENU");

    private static final String ONLINE     = sRes.getI18NString("service.protocol.status.AVAILABLE");
    private static final String MEETING    = sRes.getI18NString("service.protocol.status.gui.MEETING");
    private static final String PHONE      = sRes.getI18NString("service.protocol.status.gui.PHONE");
    private static final String CONFERENCE = sRes.getI18NString("service.protocol.status.gui.CONFERENCE");
    private static final String BUSY       = sRes.getI18NString("service.protocol.status.BUSY");

    private static final String NO_PRESENCE_DND        = sRes.getI18NString("service.protocol.status.gui.NO_PRESENCE_DND");
    private static final String NO_PRESENCE_OFFLINE    = sRes.getI18NString("service.protocol.status.gui.NO_PRESENCE_OFFLINE");
    private static final String NO_PRESENCE_SIGNED_OUT = sRes.getI18NString("service.protocol.status.gui.NO_PRESENCE_SIGNED_OUT");

    private static final String CALL_MANAGER = sRes.getI18NString("plugin.toolsmenuoptions.menu.ICM");

    /**
     * Reference to the global status service
     */
    private final GlobalStatusService mStatusService;

    /**
     * The image representing the global status
     */
    private BufferedImageFuture mImage;

    /**
     * True if we have an IM account
     */
    private boolean mGotImAccount;

    public AccountStatusButton(GlobalStatusService statusService)
    {
        super(GuiActivator.getResources().getI18NString("service.gui.ACCOUNT_ME"),
              "service.gui.button.status");

        mStatusService = statusService;
        mGotImAccount = AccountUtils.getImAccount() != null;
        setAccessibilityText(GlobalStatusEnum.ONLINE);
        mImage = getImageForStatus(GlobalStatusEnum.ONLINE);
        addActionListener(this);
        this.setForeground(TEXT_COLOR);

        // And listen for changes to the status
        statusService.addStatusChangeListener(this);
    }

    /**
     * Set the accessibility text for this button.
     *
     * @param status the current status
     */
    private void setAccessibilityText(GlobalStatusEnum status)
    {
        String statusText = GlobalStatusEnum.getI18NStatusName(status);

        AccessibilityUtils.setName(this, sRes.getI18NString("service.gui.accessibility.STATUS_BUTTON",
                                                            new String[] {this.getText(), statusText}));
    }

    /**
     * Convenience method for converting a top level status into an image
     *
     * @param status the current status
     * @return the image that represents the status
     */
    private BufferedImageFuture getImageForStatus(GlobalStatusEnum status)
    {
        return status.getStatusIcon();
    }

    @Override
    public void onStatusChanged()
    {
        // Get the new status from the StatusService
        GlobalStatusEnum status = mStatusService.getGlobalStatus();

        sLog.debug("Status changed to " + status);
        mImage = getImageForStatus(status);
        repaint();

        setAccessibilityText(status);
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        // Draw the icon (if we've got one)
        if (!isEnabled())
        {
            // The button is disabled, so add transparency to the presence
            // icon.
            ((Graphics2D)g).setComposite(AlphaComposite.getInstance(
                                            AlphaComposite.SRC_OVER, 0.6f));
        }

        g.drawImage(mImage.resolve(),
                    ScaleUtils.scaleInt(4),
                    ScaleUtils.scaleInt(4),
                    null);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        JPopupMenu menu = new JPopupMenu();

        // Need a border on non-mac clients
        if (!OSUtils.IS_MAC)
        {
            Border outside = BorderFactory.createLineBorder(Color.DARK_GRAY);
            Border inside = BorderFactory.createLineBorder(Color.WHITE, 2);
            Border border = BorderFactory.createCompoundBorder(outside, inside);
            menu.setBorder(border);
        }

        boolean imAccountExists;
        boolean imAccountSignedIn;
        boolean imAccountRegistered;

        // See if there are any IM providers and, if so, see if they are signed
        // in, or if the user has disabled it.
        ProtocolProviderService imProvider = AccountUtils.getImProvider();

        if (imProvider != null)
        {
            // There is a provider, so we are signed in.
            imAccountExists = true;
            imAccountSignedIn = true;
            imAccountRegistered = imProvider.isRegistered();
        }
        else
        {
            // No provider - so possibly the user has signed out of chat. Look
            // for an account to see.
            AccountID account = AccountUtils.getImAccount();

            imAccountRegistered = false;
            imAccountExists = (account != null);
            imAccountSignedIn = imAccountExists && account.isEnabled();
        }

        if (imAccountExists)
        {
            menu.add(SIPCommMenuItem.createHeading(PRESENCE_TITLE));
            menu.add(SIPCommMenuItem.createSeparator());

            if (!mStatusService.isDoNotDisturb() &&
                imAccountSignedIn &&
                imAccountRegistered)
            {
                // The IM account exists and we can change the status of it. Add
                // the items that allow the user to swap between online and busy
                SIPCommMenuItem online = new SIPCommMenuItem(getOnlineText(), (BufferedImageFuture) null);
                SIPCommMenuItem busy   = new SIPCommMenuItem(BUSY, (BufferedImageFuture) null);

                online.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        sLog.user("Status changed to online");
                        mStatusService.setBusy(false);
                    }
                });

                busy.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        sLog.user("Status changed to busy");
                        mStatusService.setBusy(true);
                    }
                });

                boolean isBusy = mStatusService.isBusy();
                online.setSelected(!isBusy);
                busy.setSelected(isBusy);

                AccessibilityUtils.reflectSelectionState(online);
                AccessibilityUtils.reflectSelectionState(busy);

                menu.add(online);
                menu.add(busy);
            }
            else
            {
                // Either IM account is signed out, or DND is enabled.  Either
                // way we shouldn't be able to change the presence.
                String text;

                if (!imAccountSignedIn)
                {
                    // Signed out of chat
                    text = NO_PRESENCE_SIGNED_OUT;
                }
                else if (!imAccountRegistered)
                {
                    // Not registered
                    text = NO_PRESENCE_OFFLINE;
                }
                else
                {
                    // DND must be enabled
                    text = NO_PRESENCE_DND;
                }

                JLabel comp = new JLabel(text);
                comp.setFont(comp.getFont().deriveFont(Font.ITALIC, ScaleUtils.getDefaultFontSize()));
                comp.setHorizontalAlignment(SwingConstants.CENTER);
                comp.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
                Dimension size = ComponentUtils.getStringSize(comp, text);

                // We want the text to be split onto two lines
                int width  = (size.width / 2) + 20;
                int height = size.height * 2;
                comp.setPreferredSize(new Dimension(width, height));

                comp.setBackground(Color.WHITE);
                comp.setOpaque(true);
                comp.setEnabled(false);
                menu.add(comp);
            }
        }

        // See if there are any Call Manager items to add
        if (GuiActivator.getCallManagerService() != null)
        {
            // If the IM account status has changed, we are going to need to
            // recreate the call manager menu item.
            JMenu callManagerMenu = (mGotImAccount == imAccountExists) ?
                 GuiActivator.getCallManagerService().getCallManagerMenu() :
                 GuiActivator.getCallManagerService().recreateCallManagerMenu();

            if (callManagerMenu != null)
            {
                if (!imAccountExists)
                {
                    // No IM account so call manager menu should take entire menu
                    menu = callManagerMenu.getPopupMenu();
                }
                else
                {
                    menu.add(SIPCommMenuItem.createSeparator());
                    menu.add(SIPCommMenuItem.createHeading(CALL_MANAGER));
                    menu.add(SIPCommMenuItem.createSeparator());
                    menu.add(callManagerMenu);
                }
            }
        }

        menu.setInvoker(this);

        Point locationOnScreen = getLocationOnScreen();
        menu.setLocation(locationOnScreen.x,
                         locationOnScreen.y + getHeight());
        menu.setVisible(true);

        mGotImAccount = imAccountExists;
    }

    /**
     * @return the appropriate text to display on the online menu item
     */
    private String getOnlineText()
    {
        if (mStatusService.isInConference())
            return CONFERENCE;

        if (mStatusService.isOnThePhone())
            return PHONE;

        if (mStatusService.isInMeeting())
            return MEETING;

        return ONLINE;
    }
}
