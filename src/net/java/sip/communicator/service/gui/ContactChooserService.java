// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.gui;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;

import org.jitsi.service.resources.*;

/**
 * A service which allows plug-ins access to a dialog which displays a list
 * of all contacts, allowing the user to pick one
 */
public interface ContactChooserService
{
    /**
     * Display a dialog containing a list of all contacts allowing the user to
     * pick one
     *
     * @param title The title of the dialog
     * @param message An optional hint to be displayed in the top of the dialog
     * @param positiveButton The text of the positive button
     * @param image An optional image to be displayed in the top of the dialog
     * @param callback Callback for returning the result of this dialog
     * @param opSet The operation set of the contacts that we are interested in.
     *              Can be null in which case all contacts are displayed
     */
    void displayListOfContacts(String title,
                               String message,
                               String positiveButton,
                               ImageIconFuture image,
                               ContactChosenCallback callback,
                               Class<? extends OperationSet> opSet);

    /**
     * A callback used by the contact chooser service to inform the calling code
     * of the users choice.  Also allows more control over what contacts are
     * displayed in the first place
     */
    interface ContactChosenCallback
    {
        /**
         * Called when the user has selected a contact
         *
         * @param contact The selected contact
         */
        void contactChosen(UIContact contact);

        /**
         * Called when the user cancels the selection
         */
        void contactChoiceCancelled();

        /**
         * Prepare a source contact before displaying it - this allows any
         * specific changes required to be made
         *
         * @param sourceContact The contact being added
         * @param metaContact The meta contact that is the source of the contact
         *        that is being added
         */
        void prepareContact(MetaContact metaContact,
                            SourceContact sourceContact);
    }
}
