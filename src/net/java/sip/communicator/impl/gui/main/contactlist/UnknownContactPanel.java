/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;
import java.util.List;

import javax.swing.*;
import javax.swing.text.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.event.*;
import net.java.sip.communicator.service.gui.UIService.Reformatting;
import net.java.sip.communicator.service.managecontact.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>UnknownContactPanel</tt> replaces the contact list, when a
 * <tt>SearchFilter</tt> founds no matches. It is meant to propose the user
 * some alternatives if she's looking for a contact, which is not contained in
 * the contact list.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class UnknownContactPanel
    extends TransparentPanel
    implements  TextFieldChangeListener,
                Skinnable
{
    /**
     * The logger for this class.
     */
    private static final Logger logger
        = Logger.getLogger(UnknownContactPanel.class);

    private static final long serialVersionUID = 0L;

    private static final boolean sUseColoredButtons =
        GuiActivator.getConfigurationService().global().getBoolean(
            "net.java.sip.communicator.impl.gui.main.USE_COLORED_TEXT_BUTTONS_IN_MAIN_FRAME",
            false);

    /**
     * The 'Add to contacts' button
     */
    private JButton mAddButton;

    /**
     * The 'Call' button to call the number/contact entered
     */
    private JButton mCallButton;

    /**
     * The 'Chat' button to chat with the number/contact entered
     */
    private JButton mChatButton;

    private final JTextPane mTextArea = new JTextPane();

    private final TransparentPanel mButtonPanel
        = new TransparentPanel(new GridLayout(0, 1));

    /**
     * The main application window.
     */
    private MainFrame mParentWindow;

    /**
     * An empty constructor allowing to extend this class.
     */
    public UnknownContactPanel() {}

    /**
     * Creates the <tt>UnknownContactPanel</tt> by specifying the parent window.
     * @param window the parent window
     */
    public UnknownContactPanel(MainFrame window)
    {
        super(new BorderLayout());

        mParentWindow = window;

        TransparentPanel mainPanel = new TransparentPanel(new BorderLayout());

        if (sUseColoredButtons)
        {
            mCallButton = new SIPCommColoredTextButton(
                GuiActivator.getResources().getI18NString("service.gui.CALL_CONTACT"),
                "service.gui.MAIN_WINDOW_TEXT_BUTTON_COLOR");
            mAddButton = new SIPCommColoredTextButton(
                GuiActivator.getResources().getI18NString("service.gui.ADD_CONTACT"),
                "service.gui.MAIN_WINDOW_TEXT_BUTTON_COLOR");
            mChatButton = new SIPCommColoredTextButton(
                GuiActivator.getResources().getI18NString("service.gui.CHAT"),
                "service.gui.MAIN_WINDOW_TEXT_BUTTON_COLOR");

            Color textColor = new Color(
                GuiActivator.getResources().getColor(
                    "service.gui.MAIN_WINDOW_TEXT_BUTTON_TEXT_COLOR",
                    0x000000));

            // Set the color of the text
            mCallButton.setForeground(textColor);
            mAddButton.setForeground(textColor);
            mChatButton.setForeground(textColor);

        }
        else
        {
            mCallButton = new JButton(
                GuiActivator.getResources().getI18NString("service.gui.CALL_CONTACT"));
            mAddButton = new JButton(
                GuiActivator.getResources().getI18NString("service.gui.ADD_CONTACT"));
            mChatButton = new JButton(
                GuiActivator.getResources().getI18NString("service.gui.CHAT"));
        }

        add(mainPanel, BorderLayout.NORTH);

        initAddContactButton();
        initCallButton();
        initTextArea();
        mainPanel.add(mTextArea, BorderLayout.CENTER);

        if (mCallButton.getParent() != null)
        {
            mTextArea.setText(GuiActivator.getResources()
                .getI18NString("service.gui.NO_CONTACTS_FOUND",
                    new String[]{'"'
                               + mParentWindow.getCurrentSearchText() + '"'}));
        }
        else
        {
            // There is no call button, which means we must already have
            // determined we shouldn't let the user call the entered text.
            mTextArea.setText(GuiActivator.getResources()
                .getI18NString("service.gui.NO_CONTACTS_FOUND_SHORT"));
        }

        if (mButtonPanel.getComponentCount() > 0)
        {
            TransparentPanel southPanel
                = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));
            southPanel.add(mButtonPanel);

            mainPanel.add(southPanel, BorderLayout.SOUTH);
        }

        loadSkin();
    }

    private void initAddContactButton()
    {
        mAddButton.setAlignmentX(JButton.CENTER_ALIGNMENT);

        mAddButton.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.ADD_CONTACT"));

        mButtonPanel.add(mAddButton);

        mAddButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                logger.user("Add contact button clicked");
                ManageContactWindow addContactWindow =
                    GuiActivator.getAddContactWindow(null);

                if (addContactWindow != null)
                {
                    addContactWindow.setContactAddress(
                        mParentWindow.getCurrentSearchText());
                    addContactWindow.setVisible(true);
                }
            }
        });
    }

    /**
     * Initializes the call button.
     */
    private void initCallButton()
    {
        List<ProtocolProviderService> telephonyProviders
            = CallManager.getTelephonyProviders();

        // Only show the call button if:
        // - there is at least one protocol provider to make calls with
        // - the search text can be called
        // - The user is configured to make calls
        if ((telephonyProviders!= null) && (telephonyProviders.size() > 0) &&
            (mParentWindow.getCurrentSearchPhoneNumber() != null) &&
            ConfigurationUtils.isCallingEnabled())
        {
            if (mCallButton.getParent() != null)
            {
                logger.debug("Already have parent, so button already set-up");
                return;
            }

            mCallButton.setAlignmentX(JButton.CENTER_ALIGNMENT);

            mCallButton.setMnemonic(GuiActivator.getResources()
                .getI18nMnemonic("service.gui.CALL_CONTACT"));

            mButtonPanel.add(mCallButton);

            mCallButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    logger.user("init call button clicked");
                    String searchText = mParentWindow
                        .getCurrentSearchPhoneNumber();

                    if ((searchText == null) ||
                        !ConfigurationUtils.isCallingEnabled())
                    {
                        logger.debug("Don't call: either no number or no calling");
                        return;
                    }

                    CallManager.createCall(searchText, Reformatting.NOT_NEEDED);
                    mParentWindow.clearCurrentSearchText();
                }
            });
        }
        else
        {
            mButtonPanel.remove(mCallButton);
        }
    }

    /**
     * Initializes the chat button.
     */
    private void initChatButton()
    {
        // Only show the chat button if there is at least one protocol
        // provider to use to send chats, if SMS is enabled and if the search
        // text is a number.
        if (AccountUtils.getImProvider() != null &&
            ConfigurationUtils.isSmsEnabled() &&
            (mParentWindow.getCurrentSearchPhoneNumber() != null))
        {
            if (mChatButton.getParent() != null)
                return;

            mChatButton.setAlignmentX(JButton.CENTER_ALIGNMENT);

            mChatButton.setMnemonic(GuiActivator.getResources()
                .getI18nMnemonic("service.gui.CHAT"));

            mButtonPanel.add(mChatButton);

            mChatButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    logger.user("Chat button clicked");
                    String searchText = mParentWindow
                        .getCurrentSearchPhoneNumber();

                    if (searchText == null)
                        return;

                    String smsNumber = GuiActivator.
                        getPhoneNumberUtils().formatNumberToNational(searchText);

                    GuiActivator.getUIService().startChat(new String[]{smsNumber});
                    mParentWindow.clearCurrentSearchText();
                }
            });
        }
        else
        {
            mButtonPanel.remove(mChatButton);
        }
    }

    /**
     * Clicks the call contact button in order to call the unknown contact.
     */
    public void startCall()
    {
        mCallButton.doClick();
    }

    /**
     * Clicks the add contact button in order to add the unknown contact
     * to the contact list.
     */
    public void addUnknownContact()
    {
        mAddButton.doClick();
    }

    /**
     * Invoked when any text is inserted in the search field.
     */
    public void textInserted()
    {
        updateTextArea(mParentWindow.getCurrentSearchText());
    }

    /**
     * Invoked when any text is removed from the search field.
     */
    public void textRemoved()
    {
        updateTextArea(mParentWindow.getCurrentSearchText());
    }

    /**
     * Creates the text area.
     */
    private void initTextArea()
    {
        mTextArea.setOpaque(false);
        mTextArea.setEditable(false);
        StyledDocument doc = mTextArea.getStyledDocument();

        MutableAttributeSet standard = new SimpleAttributeSet();
        StyleConstants.setAlignment(standard, StyleConstants.ALIGN_CENTER);
        StyleConstants.setFontFamily(standard, mTextArea.getFont().getFamily());
        StyleConstants.setFontSize(standard, ScaleUtils.scaleInt(12));
        doc.setParagraphAttributes(0, 0, standard, true);

        mParentWindow.addSearchFieldListener(this);
    }

    /**
     * Updates the text area to take into account the new search text.
     * @param searchText the search text to update
     */
    private void updateTextArea(String searchText)
    {
        if (mButtonPanel != null)
        {
            // If the text in the search field cannot be called (i.e. the
            // phone number is null) then the call button won't be shown and
            // the text displayed should reflect this (the short version
            // doesn't mention calling).
            String key = ((mParentWindow.getCurrentSearchPhoneNumber() == null) ||
                !ConfigurationUtils.isCallingEnabled()) ?
                "service.gui.NO_CONTACTS_FOUND_SHORT" :
                "service.gui.NO_CONTACTS_FOUND";

            mTextArea.setText(GuiActivator.getResources()
                .getI18NString(key, new String[]{'"' + searchText + '"'}));

            revalidate();
            repaint();
        }
    }

    /**
     * Reloads button resources.
     */
    public void loadSkin()
    {
        if (ConfigurationUtils.isMenuIconsDisabled())
        {
            return;
        }

        GuiActivator.getResources()
        .getImage("service.gui.icons.ADD_CONTACT_16x16_ICON")
        .addToButton(mAddButton);

        GuiActivator.getResources()
        .getImage("service.gui.icons.CALL_16x16_ICON")
        .addToButton(mCallButton);

        GuiActivator.getResources()
        .getImage("service.gui.icons.CHAT_16x16_ICON")
        .addToButton(mChatButton);
    }

    /**
     * Updates the call button appearance and shows/hides this panel.
     *
     * @param isVisible indicates if this panel should be shown or hidden
     */
    public void setVisible(boolean isVisible)
    {
        if (isVisible)
        {
            initCallButton();
            initChatButton();
        }

        super.setVisible(isVisible);
    }
}
