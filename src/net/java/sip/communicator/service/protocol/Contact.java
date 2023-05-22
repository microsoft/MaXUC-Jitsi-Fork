/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol;

import java.awt.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;

import org.jitsi.service.resources.*;

/**
 * This class represents the notion of a Contact or Buddy, that is widely used
 * in instant messaging today. From a protocol point of view, a contact is
 * generally considered to be another user of the service that proposes the
 * protocol. Instances of Contact could be used for delivery of presence
 * notifications or when addressing instant messages.
 *
 * @author Emil Ivov
 */
public interface Contact
{
    /**
     * Classes implementing Contact must override the toString method and use
     * LogHasher to hash any PII that needs to be returned.
     *
     * @return a string representation of the Contact with any PII hashed.
     */
    public String toString();

    /**
     * Returns a String that can be used for identifying the contact. The
     * exact contents of the string depends on the protocol. In the case of
     * SIP, for example, that would be the SIP uri (e.g. sip:alice@biloxi.com)
     * in the case of icq - a UIN (12345653) and for AIM a screenname (mysname).
     * Jabber (and hence Google) would be having e-mail like addresses.
     * @return a String id representing and uniquely identifying the contact.
     */
    String getAddress();

    /**
     * Returns a String that could be used by any user interacting modules for
     * referring to this contact. An alias is not necessarily unique but is
     * often more human readable than an address (or id).
     * @return a String that can be used for referring to this contact when
     * interacting with the user.
     */
    String getDisplayName();

    /**
     * Returns a byte array containing an image (most often a photo or an avatar)
     * that the contact uses as a representation.
     * @return byte[] an image representing the contact.
     */
    BufferedImageFuture getImage();

    /**
     * Returns an image that can be overlaid on the image if required
     *
     * @param size Size of the image that the overlay is going to be added to
     * @return an image that can be overlaid on the image if required
     */
    ImageIconFuture getOverlay(Dimension size);

    /**
     * Returns the status of the contact as per the last status update we've
     * received for it. Note that this method is not to perform any network
     * operations and will simply return the status received in the last
     * status update message. If you want a reliable way of retrieving someone's
     * status, you should use the <tt>queryContactStatus()</tt> method in
     * <tt>OperationSetPresence</tt>.
     * @return the PresenceStatus that we've received in the last status update
     * pertaining to this contact.
     */
    PresenceStatus getPresenceStatus();

    /**
     * Returns a reference to the contact group that this contact is currently
     * a child of or null if the underlying protocol does not support persistent
     * presence.
     * @return a reference to the contact group that this contact is currently
     * a child of or null if the underlying protocol does not support persistent
     * presence.
     */
    ContactGroup getParentContactGroup();

    /**
     * Returns a reference to the protocol provider that created the contact.
     * @return a reference to an instance of the ProtocolProviderService
     */
    ProtocolProviderService getProtocolProvider();

    /**
     * Determines whether or not this contact is being stored by the server.
     * Non persistent contacts are common in the case of simple, non-persistent
     * presence operation sets. They could however also be seen in persistent
     * presence operation sets when for example we have received an event
     * from someone not on our contact list. Non persistent contacts are
     * volatile even when coming from a persistent presence op. set. They would
     * only exist until the application is closed and will not be there next
     * time it is loaded.
     * @return true if the contact is persistent and false otherwise.
     */
    boolean isPersistent();

    /**
     * Determines whether this contact may be merged with others.
     * This is generally determined by its state of persistence.
     */
    default boolean canBeMerged()
    {
        return isPersistent();
    }

    /**
     * Determines whether or not this contact has been resolved against the
     * server. Unresolved contacts are used when initially loading a contact
     * list that has been stored in a local file until the presence operation
     * set has managed to retrieve all the contact list from the server and has
     * properly mapped contacts to their on-line buddies.
     * @return true if the contact has been resolved (mapped against a buddy)
     * and false otherwise.
     */
    boolean isResolved();

    /**
     * Returns a String that can be used to create a unresolved instance of
     * this contact. Unresolved contacts are created through the
     * createUnresolvedContact() method in the persistent presence operation
     * set. The method may also return null if no such data is required and
     * the contact address is sufficient for restoring the contact.
     * <p>
     * @return A <tt>String</tt> that could be used to create a unresolved
     * instance of this contact during a next run of the application, before
     * establishing network connectivity or null if no such data is required.
     */
    String getPersistentData();

    /**
     * Return the current status message of this contact.
     *
     * @return the current status message
     */
    String getStatusMessage();

    /**
     * Indicates if this contact supports resources.
     *
     * @return <tt>true</tt> if this contact supports resources, <tt>false</tt>
     * otherwise
     */
    boolean supportResources();

    /**
     * @return true if we support automatically adding an IM contact corresponding
     * to this Contact, false otherwise (currently only true for BGContacts).
     */
    boolean supportsIMAutoPopulation();

    /**
     * Returns a collection of resources supported by this contact or null
     * if it doesn't support resources.
     *
     * @return a collection of resources supported by this contact or null
     * if it doesn't support resources
     */
    Collection<ContactResource> getResources();

    /**
     * Adds the given <tt>ContactResourceListener</tt> to listen for events
     * related to contact resources changes.
     *
     * @param l the <tt>ContactResourceListener</tt> to add
     */
    void addResourceListener(ContactResourceListener l);

    /**
     * Removes the given <tt>ContactResourceListener</tt> listening for events
     * related to contact resources changes.
     *
     * @param l the <tt>ContactResourceListener</tt> to rmove
     */
    void removeResourceListener(ContactResourceListener l);
}
