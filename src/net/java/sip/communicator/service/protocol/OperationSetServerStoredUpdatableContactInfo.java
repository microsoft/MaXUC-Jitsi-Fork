/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;

/**
 * The Updatable Contact Info Operation set is a means of setting detailed
 * information of Contacts that have made it available on-line (on a protocol
 * server, p2p net or others). Examples of such details are your picture, postal
 * or e-mail addresses, work, hobbies, interests, and many many others.
 * <p>
 * Various types of details have been defined in the ServerStoredDetails class
 * and can be used with the get methods of this interface. Implementors may also
 * define their own details by extending or instantiating the
 * ServerStoredDetails.GenericDetail class.
 * <p>
 */
public interface OperationSetServerStoredUpdatableContactInfo
    extends OperationSet
{
    /**
     * Sets the specified list of details on a contact.
     *
     * @param contact The contact to update
     * @param details The details to set on the subscriber. After this operation
     *                the contact will have exactly these details.  Any details
     *                that are not in this list will be deleted
     * @param isCreateOp Whether the contact is being created or edited.
     * @param listener Listener to listen for results to the change to the
     *                 contact
     */
    void setDetailsForContact(Contact contact,
                              ArrayList<GenericDetail> details,
                              boolean isCreateOp,
                              ContactUpdateResultListener listener);

    /**
     * An interface for listening for the result of the contact change
     */
    interface ContactUpdateResultListener
    {
        /**
         * Called if the update to the contact failed
         * <p/>
         * One of updateFailed or updateSucceeded will be called
         *
         * @param badNetwork If true then the update failed because there is no
         *        network to make the change.  If false then it is probably
         *        because the contact data was bad.
         */
        void updateFailed(boolean badNetwork);

        /**
         * Called if the update to the contact failed due to a CommPortal Data
         * Error
         * <p/>
         * One of updateFailed or updateSucceeded will be called
         *
         * @param badNetwork If true then the update failed because there is no
         *        network to make the change.  If false then it is probably
         *        because the contact data was bad.
         * @param errorMsg The error message describing the failure
         */
        void updateFailed(boolean badNetwork,
                          String errorMsg);

        /**
         * Called if the update to the contact succeeds
         * <p/>
         * One of updateFailed or updateSucceeded will be called
         */
        void updateSucceeded();
    }
}
