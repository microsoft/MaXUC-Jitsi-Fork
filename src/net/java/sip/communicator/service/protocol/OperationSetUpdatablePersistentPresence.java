/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * Interface used to create temporary contact before calling the subscribe of
 * the corresponding OperationSetPersistentPresence. This is used for database
 * which generates the contact identifier only once the contact has been
 * created. Oce done this identitifer is getting back and used to feed the
 * subscribe method of the OperationSetPersistentPresence to finish the creation
 * of this contact.
 *
 * @author Vincent Lucas
 */
public interface OperationSetUpdatablePersistentPresence
    extends OperationSet
{
    /**
     * Creates a new temporary and returns its identifer.
     *
     * @return The identifer of the created contact.
     */
    String createContact();

    /**
     * Returns the root group of the server stored contact list.
     *
     * @return the root ContactGroup for the ContactList stored by this
     *   service.
     */
    ContactGroup getServerStoredContactListRoot();
}
