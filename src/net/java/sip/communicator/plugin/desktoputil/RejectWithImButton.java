// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.wispaservice.WISPAAction;
import net.java.sip.communicator.service.wispaservice.WISPANamespace;
import net.java.sip.communicator.service.wispaservice.WISPAService;
import net.java.sip.communicator.util.AccessibilityUtils;

/**
 * A "reject" button that gives the user the option of sending an IM to the
 * user who triggered the component containing the "reject" button.
 */
public class RejectWithImButton extends TransparentPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;

    private static final Logger sLog = Logger.getLogger(RejectWithImButton.class);

    /**
     * The resources management service.
     */
    private static final ResourceManagementService sRes = DesktopUtilActivator.getResources();

    // Resources used for the reject with IM option:
    private static final String REJECT_HINT_RES = "service.gui.REJECT_CALL_HINT";
    private static final String REJECT_REASON_STUB = "service.gui.REJECT_CALL_";

    /**
     * The MetaContact who caused this button to be created.
     */
    private final MetaContact mMetaContact;

    /**
     * The action listener to call back to if the user chooses to reject
     * without IM.
     */
    private final ActionListener mActionListener;

    /**
     * The button used to reject without sending an IM.
     */
    private JButton mRejectButton;

    /**
     * The button used to reject and send an IM.
     */
    private SIPCommButton mRejectImButton;

    /**
     * The pop-up menu containing options for rejecting with IM.
     */
    private JPopupMenu mRejectImMenu;

    private static final WISPAService sWISPAService = DesktopUtilActivator.getWISPAService();

    /**
     * Creates a RejectWithImButton
     *
     * @param metaContact the MetaContact to send the IM to.
     * @param rejectResourcePrefix the prefix of the resource strings to use
     * for the button.
     * @param actionListener the action listener to call back to if the user
     * chooses to reject without IM
     */
    public RejectWithImButton(MetaContact metaContact,
                              String rejectResourcePrefix,
                              ActionListener actionListener)
    {
        super(new FlowLayout(0, 0, 0));

        sLog.debug("Creating reject with IM button with resource prefix: " +
            rejectResourcePrefix);

        String reject =
            sRes.getI18NString("service.gui." + rejectResourcePrefix + ".REJECT");
        String imageResourcePrefix = "service.gui.button." + rejectResourcePrefix;
        mMetaContact = metaContact;
        mActionListener = actionListener;

        boolean imAvailable = mMetaContact != null &&
                              mMetaContact.canBeMessaged() &&
                              mMetaContact.getIMContact() != null;

        if (imAvailable)
        {
            // It doesn't make sense to allow the user to reject offline contacts
            // as they won't get the message immediately.
            PresenceStatus status = mMetaContact.getIMContact().getPresenceStatus();
            imAvailable = status.isOnline();
        }

        if (imAvailable)
        {
            sLog.debug("IM Contact present, create reject with IM button");

            // There is an IM contact, thus we need to add two buttons.  One
            // just to reject the call, and one to reject with an IM message.
            // We don't use OS-specific resources for the reject button, as we
            // don't want the rounded edge of the Mac graphics in the middle of
            // the button.
            mRejectButton = new SIPCommSnakeButton(
                reject, imageResourcePrefix + ".IM", true);
            mRejectButton.addActionListener(mActionListener);
            mRejectButton.setForeground(Color.WHITE);
            add(mRejectButton);

            mRejectImButton = new SIPCommButton();
            mRejectImButton.setBackgroundImage(sRes.getBufferedImage(
                    imageResourcePrefix + ".IM.NORMAL"));
            mRejectImButton.setRolloverImage(sRes.getBufferedImage(
                    imageResourcePrefix + ".IM.ROLLOVER"));
            mRejectImButton.setPressedImage(sRes.getBufferedImage(
                    imageResourcePrefix + ".IM.PRESSED"));
            mRejectImButton.addActionListener(this);
            mRejectImButton.setForeground(Color.WHITE);

            AccessibilityUtils.setName(mRejectImButton, sRes.getI18NString(REJECT_HINT_RES));
            add(mRejectImButton);
        }
        else
        {
            sLog.debug("No IM Contact present");

            // No IM, just a simple button required
            mRejectButton = new SIPCommSnakeButton(
                reject, imageResourcePrefix + ".REJECT", true);
            mRejectButton.addActionListener(mActionListener);
            mRejectButton.setForeground(Color.WHITE);
            add(mRejectButton);
        }
    }

    /**
     * Reject a call with an IM message
     *
     * @param container The component to align the menu with
     */
    private void rejectWithImPressed(Container container)
    {
        sLog.user("Reject with IM selected");

        if (mRejectImMenu != null)
            mRejectImMenu.setVisible(false);

        mRejectImMenu = new JPopupMenu();

        // Need a border on non-mac clients
        if (!OSUtils.IS_MAC)
        {
            Border outside = BorderFactory.createLineBorder(Color.DARK_GRAY);
            Border inside = BorderFactory.createLineBorder(Color.WHITE, 2);
            Border border = BorderFactory.createCompoundBorder(outside, inside);
            mRejectImMenu.setBorder(border);
        }

        // Set up the menu items
        JLabel menuTitle = SIPCommMenuItem.createHeading(sRes.getI18NString(REJECT_HINT_RES));

        if (OSUtils.IS_WINDOWS)
        {
            // In windows, stop vertical separator from going through text
            menuTitle.setOpaque(true);
        }

        menuTitle.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 0));
        menuTitle.setFont(menuTitle.getFont().deriveFont(Font.BOLD, ScaleUtils.getDefaultFontSize()));

        // Add title for the menu
        mRejectImMenu.add(menuTitle);
        mRejectImMenu.add(new JSeparator(SwingConstants.HORIZONTAL));

        // Add a menu item for each string in resources.  There are an unknown
        // number of strings, but they are stored in <res>_0, <res>_1 ... thus
        // we can find them all using while res_i != null.
        int i = 0;
        while (sRes.getI18NString(REJECT_REASON_STUB + i) != null)
        {
            String displayString = sRes.getI18NString(REJECT_REASON_STUB + i);
            SIPCommMenuItem menuItem = new SIPCommMenuItem(displayString, (BufferedImageFuture) null);
            final String imMessage;

            i++;
            if (sRes.getI18NString(REJECT_REASON_STUB + i) == null)
            {
                // This is the last menu item, so make the menu item italics.
                // Further, the last string should be the "custom message", so
                // make the imMessage null
                menuItem.setFont(menuItem.getFont().deriveFont(Font.ITALIC));
                imMessage = null;
            }
            else
            {
                imMessage = displayString;
            }

            menuItem.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    sLog.user("Call rejected with message: " + imMessage);
                    sendIM(imMessage);
                    if (mRejectImMenu != null)
                        mRejectImMenu.setVisible(false);
                    // After sending the IM, call into the action performed
                    // method of the listener that was passed in when creating
                    // this button.
                    mActionListener.actionPerformed(e);
                    sWISPAService.notify(WISPANamespace.CONTACTS, WISPAAction.MOTION, mMetaContact);
                }
            });

            mRejectImMenu.add(menuItem);
        }

        mRejectImMenu.show(container, 0, ScaleUtils.scaleInt(30));
    }

    /**
     * Open a chat window and send a message to the contact associated with this
     * incoming call.
     *
     * @param message The message to send.  Can be null in which case no message
     *                is sent
     */
    private void sendIM(final String message)
    {
        Contact imContact = mMetaContact.getIMContact();

        // If we've gone offline since creating the pop-up, the im
        // contact will not exist any more
        if (imContact == null)
            return;

        // Send the message that the user has selected - if not null
        // Run later to allow the chat window to be created
        if (message != null)
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    Contact imContact = mMetaContact.getIMContact();

                    // If we've gone offline since creating the pop-up, the im
                    // contact will not exist any more
                    if (imContact == null)
                        return;

                    OperationSetBasicInstantMessaging opSet = imContact
                             .getProtocolProvider()
                             .getOperationSet(
                                       OperationSetBasicInstantMessaging.class);

                    // The string that is displayed to the user has quotation
                    // marks at start and end - remove them:
                    ImMessage imMessage = opSet.createMessage(
                                    message.substring(1, message.length() - 1));
                    opSet.sendInstantMessage(imContact, imMessage);
                }
            });
        }
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton) e.getSource();
        rejectWithImPressed(button.getParent());
    }

    @Override
    public void setPreferredSize(Dimension dim)
    {
        super.setPreferredSize(dim);

        // As well as setting the preferred size of this panel, we need to make
        // sure the buttons in this panel respect that.  The size of the reject
        // IM button is fixed to the width of its graphic, therefore the
        // component that we need to update the preferred size of is the reject
        // button. If we don't have a reject IM button, we can just set the
        // preferred size of the reject button to be the requested preferred
        // size.  If we have a reject IM button, we need to subtract the width
        // of the reject IM button from the requested preferred size before
        // setting that on the reject button.
        Dimension prefRejectDim = new Dimension(dim);
        if (mRejectImButton != null)
        {
            Dimension imDim = mRejectImButton.getPreferredSize();
            double prefRejectWidth = dim.getWidth() - imDim.getWidth();

            sLog.debug(
                "Setting preferred width of reject button to " + prefRejectWidth);
            prefRejectDim.setSize(prefRejectWidth, dim.getHeight());
        }

        mRejectButton.setPreferredSize(prefRejectDim);
    }
}
