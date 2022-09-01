// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import org.jitsi.service.resources.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.*;

/**
 * An abstract class to contain methods shared by more than one
 * RightButtonMenu class.
 */
public abstract class AbstractContactRightButtonMenu extends SIPCommPopupMenu
{
    private static final long serialVersionUID = 0L;

    private static final Logger sLog =
                         Logger.getLogger(AbstractContactRightButtonMenu.class);

    private static final ResourceManagementService sResources =
                                                    GuiActivator.getResources();

    /**
     * Text to display on the "Send Email" menu item.
     */
    private static final String SEND_EMAIL_MENU_TEXT =
                             sResources.getI18NString("service.gui.SEND_EMAIL");

    /**
     * Creates the "Send Email" menu and adds it to this right button menu.
     *
     * @param addIfEmpty if true, the menu item will be added (but disabled)
     * even if the contact has no email addresses, otherwise the menu item will
     * only be added if the contact has at least one email address.
     */
    protected void initEmailMenu(boolean addIfEmpty)
    {
        // Get a set of all email addresses associated with this contact.
        Set<String> emailAddresses = getEmailAddresses();

        if (emailAddresses.size() > 1)
        {
            // If we have more than one email address, we need to use a
            // SIPCommMenu so we can display a choice of individual email
            // addresses in a side menu.
            SIPCommMenu emailMenu = new SIPCommMenu(SEND_EMAIL_MENU_TEXT);
            for (String emailAddress : emailAddresses)
            {
                emailMenu.add(createEmailMenuItem(emailAddress));
            }

            add(emailMenu);
        }
        else
        {
            JMenuItem emailItem = null;

            if (emailAddresses.isEmpty())
            {
                // If we have no email addresses, only add the menu item if
                // we've been asked to.  If so, make sure we disable it.
                if (addIfEmpty)
                {
                    emailItem = new JMenuItem();
                    emailItem.setEnabled(false);
                }
            }
            else
            {
                // If we have exactly one email address, set it to open an
                // email to the first (and only) email address in the set.
                emailItem = createEmailMenuItem(emailAddresses.iterator().next());
            }

            if (emailItem != null)
            {
                ScaleUtils.scaleFontAsDefault(emailItem);
                emailItem.setText(SEND_EMAIL_MENU_TEXT);
                add(emailItem);
            }
        }
    }

    /**
     * Creates a menu item that, when clicked, will open a new email to the
     * given email address.
     *
     * @param emailAddress the email address to open the email to.
     *
     * @return the menu item.
     */
    protected JMenuItem createEmailMenuItem(String emailAddress)
    {
        JMenuItem emailMenuItem = new JMenuItem(emailAddress, null);
        emailMenuItem.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                sLog.user("Right-click, 'Send Email' option selected");
                EmailUtils.createEmail(emailAddress, null, null);
            }
        });

        return emailMenuItem;
    }

    /**
     * @return a set of all email addresses belonging to the contact associated
     * with this right button menu.
     */
    protected abstract Set<String> getEmailAddresses();
}
