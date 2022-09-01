// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.utils;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * An implementation of the Contact Chooser Service, which displays a dialog
 * with a list of contacts, allowing the user to choose to display one of them.
 */
public class ContactChooserServiceImpl implements ContactChooserService
{
    private static final Logger sLog = Logger.getLogger(ContactChooserServiceImpl.class);

    public void displayListOfContacts(String title,
                                      String message,
                                      String positiveButton,
                                      ImageIconFuture image,
                                      ContactChosenCallback callback,
                                      Class<? extends OperationSet> opSet)
    {
        // Just create a dialog and display it:
        new ContactsDialog(title,
                           message,
                           positiveButton,
                           image,
                           callback,
                           opSet).setVisible(true);
    }

    /**
     * A class representing a dialog which displays and allows the user to
     * choose from a list of contacts.
     */
    private static class ContactsDialog extends ContactInviteDialog
    {
        private static final long serialVersionUID = 1L;

        /**
         * Call back for the result of the dialog
         */
        private final ContactChosenCallback mCallback;

        /**
         * Constructor
         *
         * @param title The title to give the dialog
         * @param message An optional hint to display in the dialog
         * @param button The label on the "ok" button of the dialog
         * @param image An optional image to display in the dialog
         * @param callback Callback for the result of the dialog
         * @param opSetClass Optional opset indicating which contacts we are
         *                   interested in.
         */
        public ContactsDialog(String title,
                              String message,
                              String button,
                              ImageIconFuture image,
                              ContactChosenCallback callback,
                              Class<? extends OperationSet> opSetClass)
        {
            super(null, title);
            mCallback = callback;
            initContactListData(opSetClass);
            setMinimumSize(new Dimension(250, 350));

            addCancelButtonListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    sLog.debug("User cancelled contact selection");
                    setVisible(false);
                    dispose();

                    mCallback.contactChoiceCancelled();
                }
            });

            addOkButtonListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    sLog.debug("User selected a contact");

                    UIContact selectedContact = mContactList.getSelectedContact();
                    if (selectedContact == null)
                    {
                        mContactList.selectFirstContact();
                    }

                    // Selecting the first contact happens on the UI thread,
                    // thus we must return the result on the UI thread to
                    // ensure that the selection succeeds:
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        public void run()
                        {
                            UIContact contact = mContactList.getSelectedContact();

                            if (contact == null)
                            {
                                // just ignore
                                sLog.debug("User selected a null contact");
                                return;
                            }

                            // Need to ignore disabled contacts
                            if (contact.getDescriptor() instanceof SourceContact &&
                                !((SourceContact)contact.getDescriptor()).isEnabled())
                            {
                                sLog.debug("Ignore selection of disabled contact");
                                return;
                            }

                            mCallback.contactChosen(contact);
                            setVisible(false);
                            dispose();
                        }
                    });
                }
            });

            if (message != null)
                setInfoText(message);

            if (image != null)
                setIcon(image);

            if (button != null)
                setOkButtonText(button);
        }

        /**
         * Initialise the contact list with the contact sources that we wish to
         * display
         *
         * @param opSetClass Optional opset indicating which contacts we are
         *                   interested in.
         */
        private void initContactListData(Class<? extends OperationSet> opSetClass)
        {
            mContactList.removeAllContactSources();

            Map<Object, ProtocolProviderFactory> factories =
                GuiActivator.getProtocolProviderFactories();

            for (Object key : factories.keySet())
            {
                // For each factory, get all the accounts
                ProtocolProviderFactory factory = factories.get(key);
                ArrayList<AccountID> accounts = factory.getRegisteredAccounts();

                for (AccountID account : accounts)
                {
                    // For each account, look at the provider then add the account
                    // if it is suitable
                    ServiceReference<?> serRef = factory.getProviderForAccount(account);
                    ProtocolProviderService provider = (ProtocolProviderService)
                    GuiActivator.bundleContext.getService(serRef);

                    if (provider.isRegistered() &&
                         (opSetClass == null ||
                          provider.getOperationSet(opSetClass) != null))
                    {
                        mContactList.addContactSource(
                                     createContactSource(opSetClass, provider));
                    }
                }
            }

            mContactList.applyCurrentFilter();
        }

        /**
         * Create a contact source for a particular protocol provider
         *
         * @param opSetClass The class of the provider
         * @param provider The provider itself
         * @return The contact source for that provider
         */
        private ProtocolContactSourceServiceImpl createContactSource(
                                       Class<? extends OperationSet> opSetClass,
                                       ProtocolProviderService provider)
        {
            // Over-ride the default Protocol Contact Source as we wish to
            // insert a check to see if some contacts should be added.
            return new ProtocolContactSourceServiceImpl(provider, opSetClass)
            {
                @Override
                protected ProtocolCQuery createQuery(String queryString,
                                                     int contactCount)
                {
                    ProtocolCQuery query =
                                   new ProtocolCQuery(queryString, contactCount)
                    {
                        @Override
                        protected void addContact(
                                       SortedGenericSourceContact sourceContact,
                                       MetaContact metaContact)
                        {
                            mCallback.prepareContact(metaContact, sourceContact);

                            super.addContact(sourceContact, metaContact);
                        }
                    };

                    return query;
                }

                @Override
                public String getDisplayName()
                {
                    return null;
                }
            };
        }
    }
}
