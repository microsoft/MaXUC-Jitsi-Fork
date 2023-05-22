/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

public class ContactListUtils
{
    private static final Logger sLog
        = Logger.getLogger(ContactListUtils.class);

    public static void addContact(
                                final ProtocolProviderService protocolProvider,
                                final MetaContactGroup group,
                                final String contactAddress)
    {
        new Thread("ContactListUtils.addContact")
        {
            @Override
            public void run()
            {
                try
                {
                    GuiActivator.getContactListService()
                                .createMetaContact(protocolProvider,
                                                   group,
                                                   contactAddress);
                }
                catch (MetaContactListException ex)
                {
                    sLog.error(ex);
                    ex.printStackTrace();
                    int errorCode = ex.getErrorCode();

                    if (errorCode
                            == MetaContactListException
                                .CODE_CONTACT_ALREADY_EXISTS_ERROR)
                    {
                        new ErrorDialog(
                            GuiActivator.getResources().getI18NString(
                                "service.gui.ADD_CONTACT_ERROR_TITLE"),
                            GuiActivator.getResources().getI18NString(
                                "service.gui.ADD_CONTACT_EXIST_ERROR",
                                new String[]
                        { contactAddress })).showDialog();
                    }
                    else if (errorCode
                            == MetaContactListException
                                .CODE_NETWORK_ERROR)
                    {
                        sLog.error("Could not add contact: server did not " +
                            "respond.", ex);
                        new ErrorDialog(
                            GuiActivator.getResources().getI18NString(
                                "service.gui.ADD_CONTACT_ERROR_TITLE"),
                            GuiActivator.getResources().getI18NString(
                                "service.gui.ADD_CONTACT_NETWORK_ERROR",
                                new String[]
                        { contactAddress })).showDialog();
                    }
                    else if (errorCode
                            == MetaContactListException
                                .CODE_NOT_SUPPORTED_OPERATION)
                    {
                        sLog.error("The user tried to add a contact to a " +
                            "protocol which did not support the " +
                            "operation.", ex);
                        new ErrorDialog(
                            GuiActivator.getResources().getI18NString(
                                "service.gui.ADD_CONTACT_ERROR_TITLE"),
                            GuiActivator.getResources().getI18NString(
                                "service.gui.ADD_CONTACT_NOT_SUPPORTED",
                                new String[]
                        { contactAddress })).showDialog();
                    }
                    else
                    {
                        sLog.error("Failed to add a contact.", ex);
                        new ErrorDialog(
                            GuiActivator.getResources().getI18NString(
                                "service.gui.ADD_CONTACT_ERROR_TITLE"),
                            GuiActivator.getResources().getI18NString(
                                "service.gui.ADD_CONTACT_ERROR", new String[]
                        { contactAddress })).showDialog();
                    }
                }
            }
        }.start();
    }
}
