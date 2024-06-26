/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * Operation set used for type of contacts, retrieving changing and
 * creating contacts with types.
 *
 * @author Damian Minkov
 */
public interface OperationSetContactTypeInfo
    extends OperationSet
{

    /**
     * Persistently adds a subscription for the presence status of the contact
     * corresponding to the specified contactIdentifier to the top level group.
     * Note that this method, unlike the subscribe method in
     * OperationSetPresence, is going the subscribe the specified contact in a
     * persistent manner or in other words, it will add it to a server stored
     * contact list and thus making the subscription for its presence status
     * last along multiple registrations/logins/signons.
     * <p>
     * Apart from an exception in the case of an immediate failure, the method
     * won't return any indication of success or failure. That would happen
     * later on through a SubscriptionEvent generated by one of the methods of
     * the SubscriptionListener.
     * <p>
     *
     * @param contactIdentifier the contact whose status updates we are
     *            subscribing for.
     *            <p>
     * @param contactType the type of the newly created contact.
     * @throws OperationFailedException with code NETWORK_FAILURE if subscribing
     *             fails due to errors experienced during network communication
     * @throws IllegalArgumentException if <tt>contact</tt> is not a contact
     *             known to the underlying protocol provider
     * @throws IllegalStateException if the underlying protocol provider is not
     *             registered/signed on a public service.
     */
    void subscribe(String contactIdentifier, String contactType)
        throws IllegalArgumentException,
               IllegalStateException,
               OperationFailedException;

    /**
     * Persistently adds a subscription for the presence status of the contact
     * corresponding to the specified contactIdentifier and indicates that it
     * should be added to the specified group of the server stored contact list.
     * Note that apart from an exception in the case of an immediate failure,
     * the method won't return any indication of success or failure. That would
     * happen later on through a SubscriptionEvent generated by one of the
     * methods of the SubscriptionListener.
     * <p>
     *
     * @param contactIdentifier the contact whose status updates we are
     *            subscribing for.
     * @param parent the parent group of the server stored contact list where
     *            the contact should be added.
     *            <p>
     * @param contactType the type of the newly created contact.
     * @throws OperationFailedException with code NETWORK_FAILURE if subscribing
     *             fails due to errors experienced during network communication
     * @throws IllegalArgumentException if <tt>contact</tt> or <tt>parent</tt>
     *             are not a contact known to the underlying protocol provider.
     * @throws IllegalStateException if the underlying protocol provider is not
     *             registered/signed on a public service.
     */
    void subscribe(ContactGroup parent, String contactIdentifier,
                   String contactType)
        throws IllegalArgumentException,
               IllegalStateException,
               OperationFailedException;
}
