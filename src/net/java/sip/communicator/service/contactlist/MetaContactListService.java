/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.contactlist;

import java.util.*;

import org.jitsi.util.CustomAnnotations.*;

import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;

/**
 * The <tt>MetaContactListService</tt> handles the global project contact
 * list including contacts from all implemented protocols.
 * <p>
 * An implementation of the <tt>MetaContactListService</tt> would take care
 * of synchronizing the local copy of the contact list with the  versions stored
 * on the various server protocols.
 * <p>
 * All modules that would for some reason like to query or modify the contact
 * list should use this service rather than accessing protocol providers
 * directly.
 * <p>
 * The point of <tt>MetaContact</tt>s is being able to merge different
 * protocol specific contacts so that they represent a single person or identity.
 * Every protocol specific <tt>Contact</tt> would therefore automatically
 * be assigned to a corresponding <tt>MetaContact</tt>. A single
 * MetaContact may containing multiple contacts (e.g. a single person often
 * has accounts in different protocols) while a single protocol specific
 * Contact may only be assigned to a exactly one MetaContact.
 * <p>
 * Once created a MetaContact may be updated to contain multiple protocol
 * specific contacts. These protocol specific contacts may also be removed
 * away from a MetaContact. Whenever a MetaContact remains empty (i.e. all of
 * its protocol specific contacts are removed) it is automatically deleted.
 * <p>
 * Note that for most of the methods defined by this interface, it is likely
 * that implementations require one or more network operations to complete
 * before returning. It is therefore strongly advised not to call these methods
 * in event dispatching threads (watch out UI implementors ;) ) as this may lead
 * to unpleasant user experience.
 * <p>
 * The MetaContactListService also defines a property named:<br>
 * <tt>net.java.sip.communicator.service.contactlist.PROVIDER_MASK</tt><br>
 * When this property is set, implementations of the MetaContactListService
 * would only interact with protocol providers that same property set to the
 * same value. This feature is mostly used during unit testing so that testing
 * bundles could make sure that a tested meta contact list implementation would
 * only load their mocking protocol provider implementations during the test
 * run.
 * <p>
 * @author Emil Ivov
 */
public interface MetaContactListService
{
    /**
     * This property is used to tell implementations of the
     * MetaContactListService that they are to only interact with providers
     * that have the same property set to the same value as the system one.
     * This feature is mostly used during unit testing so that testing bundles
     * could make sure that a tested meta contact list implementation would only
     * load their mocking protocol provider implementations during the test run.
     */
    String PROVIDER_MASK_PROPERTY =
        "net.java.sip.communicator.service.contactlist.PROVIDER_MASK";

    /**
     * Returns a MetaContact list featuring all contacts supplied by this
     * service which should be displayed by the UI. For use in UI processing,
     * which only cares which contacts are in the top-level list. We also
     * exclude contacts which are just used for registering presence updates
     * (e.g. SIP contacts).
     *
     * @return All displayable MetaContact objects in this service
     */
    List<MetaContact> getDisplayableContactList();

    /**
     * Returns the root <tt>MetaContactGroup</tt> in this contact list.
     * All meta contacts and subgroups are children of the root meta contact
     * and references to them can only be obtained through it.
     *
     * @return the root <tt>MetaContactGroup</tt> for this contact list.
     */
    MetaContactGroup getRoot();

    /**
     * Returns the meta contact group that is a direct parent of the specified
     * <tt>child</tt>.
     * @param child the <tt>MetaContactGroup</tt> whose parent group we're
     * looking for. If no parent is found <tt>null</tt> is returned.
     *
     * @return the <tt>MetaContactGroup</tt> that contains <tt>child</tt> or
     * null if no parent couldn't be found.
     */
    MetaContactGroup findParentMetaContactGroup(MetaContactGroup child);

    /**
     * Returns the meta contact group that is a direct parent of the specified
     * <tt>child</tt>. If no parent is found <tt>null</tt> is returned.
     * @param child the <tt>MetaContact</tt> whose parent group we're looking
     * for.
     *
     * @return the <tt>MetaContactGroup</tt> that contains <tt>child</tt> or
     * null if no such group could be found.
     */
    MetaContactGroup findParentMetaContactGroup(MetaContact child);

    /**
     * Returns the MetaContact containing the specified contact or null if no
     * such MetaContact was found. The method can be used when for example
     * we need to find the MetaContact that is the author of an incoming message
     * and the corresponding ProtocolProviderService has only provided a
     * <tt>Contact</tt> as its author.
     *
     * @param contact the contact whose encapsulating meta contact we're looking
     * for.
     * @return the MetaContact containing the specified contact or <tt>null</tt>
     * if no such contact is present in this contact list.
     */
    MetaContact findMetaContactByContact(Contact contact);

    /**
     * Returns the MetaContact containing a contact with an address equal to
     * <tt>contactAddress</tt> and with a source provider matching
     * <tt>accountID</tt>, or null if no such MetaContact was found. The method
     * can be used when for example we
     * need to find the MetaContact that is the author of an incoming message
     * and the corresponding ProtocolProviderService has only provided a
     * <tt>Contact</tt> as its author.
     *
     * @param contactAddress the address of the  protocol specific
     * <tt>contact</tt> that we're looking for.
     * @param accountID the ID of the account that the contact we're looking for
     * must belong to.
     *
     * @return the MetaContact containing the specified contact or null if no
     *         such contact is present in this contact list.
     */
    MetaContact findMetaContactByContact(String contactAddress,
                                         String accountID);

    /**
     * Returns the MetaContactGroup encapsulating the specified protocol contact
     * group or null if no such MetaContactGroup was found.
     *
     * @param group the group whose encapsulating meta group we're looking for.
     * @return the MetaContact containing the specified contact or <tt>null</tt>
     * if no such contact is present in this contact list.
     */
    MetaContactGroup findMetaContactGroupByContactGroup(
            ContactGroup group);

    /**
     * Returns the MetaContact that corresponds to the specified metaContactID.
     *
     * @param metaContactID a String identifier of a meta contact.
     * @return the MetaContact with the specified string identifier or
     * <tt>null</tt> if no such meta contact was found.
     */
    MetaContact findMetaContactByMetaUID(String metaContactID);

    /**
     * Returns a list of MetaContacts containing all contacts whose phone number
     * equals the given <tt>phoneNumber</tt>, or empty list if no such MetaContacts
     * could be found.
     *
     * @param phoneNumber the phone number of the protocol specific
     * <tt>contact</tt> that we're looking for.
     * @return a list of MetaContacts whose phone number equals the given
     * <tt>phoneNumber</tt>
     */
    @NotNull
    List<MetaContact> findMetaContactByNumber(String phoneNumber);

    /**
     * Returns a list of MetaContacts containing all contacts whose details
     * match those given.
     *
     * @param details the details that we're looking for.
     * @return a list of MetaContacts, or null if none are found.
     */
    List<MetaContact> findMetaContactsByDetails(ArrayList<GenericDetail> details);

    /**
     * Returns a list of MetaContacts containing all contacts whose email address
     * or IM address equals the given <tt>emailAddress</tt>.
     *
     * @param emailAddress the email address of the  protocol specific
     * <tt>contact</tt> that we're looking for.
     * @return a list of MetaContacts whose phone number equals the given
     * <tt>emailAddress</tt>.
     */
    List<MetaContact> findMetaContactByEmail(String emailAddress);

    /**
     * Returns the MetaContactGroup that corresponds to the specified
     * metaGroupID.
     *
     * @param metaGroupID
     *            a String identifier of a meta contact group.
     * @return the MetaContactGroup with the specified string identifier or null
     *          if no such meta contact was found.
     */
    MetaContactGroup findMetaContactGroupByMetaUID(String metaGroupID);

    /**
     * Returns a list of all <tt>MetaContact</tt>s containing a protocol contact
     * from the given <tt>ProtocolProviderService</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> whose
     * contacts we're looking for.
     * @return a list of all <tt>MetaContact</tt>s containing a protocol contact
     * from the given <tt>ProtocolProviderService</tt> or all contacts if the
     * protocol provider was null.
     */
    Iterator<MetaContact> findAllMetaContactsForProvider(
            ProtocolProviderService protocolProvider);

    /**
     * If it can match the given SMS number to a single <tt>MetaContact</tt>,
     * this method returns that <tt>MetaContact</tt>. Otherwise it returns null.
     *
     * @param smsNumber The SMS number to try to match to a <tt>MetaContact</tt>.
     * @return the matched <tt>MetaContact</tt>, or null if not exactly one
     *         <tt>MetaContact</tt> is found.
     */
    MetaContact findMetaContactForSmsNumber(String smsNumber);

    /**
     * Adds a listener for <tt>MetaContactListChangeEvent</tt>s posted after
     * the tree changes.
     *
     * @param l the listener to add
     */
    void addMetaContactListListener(MetaContactListListener l);

    /**
     * Removes a listener previously added with
     * <tt>addContactListListener</tt>.
     *
     * @param l the listener to remove
     */
    void removeMetaContactListListener(MetaContactListListener l);

    /**
     * Makes the specified <tt>contact</tt> a child of the
     * <tt>newParent</tt> MetaContact. If <tt>contact</tt> was
     * previously a child of another meta contact, it will be removed from its
     * old parent before being added to the new one. If the specified contact
     * was the only child of its previous parent, then it (the previous parent)
     * will be removed.
     *
     * @param contact the <tt>Contact</tt> to move to the
     * @param newParent the MetaContact where we'd like contact to be moved.
     *
     * @throws MetaContactListException with an appropriate code if the
     * operation fails for some reason.
     */
    void moveContact(Contact contact, MetaContact newParent)
        throws MetaContactListException;

    /**
     * Makes the specified <tt>contact</tt> a child of the
     * <tt>newParent</tt> MetaContactGroup. If <tt>contact</tt> was
     * previously a child of a meta contact, it will be removed from its
     * old parent and to a newly created one even if they both are in the same
     * group. If the specified contact was the only child of its previous
     * parent, then the meta contact will also be moved.
     *
     * @param contact the <tt>Contact</tt> to move to the
     * @param newParent the MetaContactGroup where we'd like contact to be moved.
     *
     * @throws MetaContactListException with an appropriate code if the
     * operation fails for some reason.
     */
    void moveContact(Contact contact, MetaContactGroup newParent)
        throws MetaContactListException;

    /**
     * Deletes the specified contact from both the local contact list and (if
     * applicable) the server stored contact list if supported by the
     * corresponding protocol. If the <tt>MetaContact</tt> that contained
     * the given contact had no other children, it will be removed.
     * <p>
     * @param contact the contact to remove.
     * @throws MetaContactListException with an appropriate code if the
     * operation fails for some reason.
     */
    void removeContact(Contact contact)
        throws MetaContactListException;

    /**
     * First makes the specified protocol provider create the contact as
     * indicated by <tt>contactID</tt>, and then associates it to the
     * _existing_ <tt>metaContact</tt> given as an argument.
     * <p>
     * @param provider the ProtocolProviderService that should create the
     * contact indicated by <tt>contactID</tt>.
     * @param metaContact the meta contact where that the newly created contact
     * should be associated to.
     * @param contactID the identifier of the contact that the specified provider
     *
     * @throws MetaContactListException with an appropriate code if the
     * operation fails for some reason.
     */
    void addNewContactToMetaContact(ProtocolProviderService provider,
                                    MetaContact metaContact,
                                    String contactID)
        throws MetaContactListException;

    /**
     * First makes the specified protocol provider create a contact
     * corresponding to the specified <tt>contactID</tt>, then creates a new
     * MetaContact which will encapsulate the newly-created protocol specific
     * contact. Depending on implementations the method may sometimes need
     * time to complete as it may be necessary for an underlying protocol to
     * wait for a server to acknowledge addition of the contact.
     * <p>
     * If the specified parent MetaContactGroup did not have a corresponding
     * group on the protocol server, it will be created before the contact
     * itself.
     * <p>
     * @param provider a ref to <tt>ProtocolProviderService</tt> instance
     * which will create the actual protocol specific contact.
     * @param contactGroup the MetaContactGroup where the newly created meta
     * contact should be stored.
     * @param contactID a protocol specific string identifier indicating the
     * contact the protocol provider should create.
     * @return the newly created <tt>MetaContact</tt>
     *
     * @throws MetaContactListException with an appropriate code if the
     * operation fails for some reason.
     */
    MetaContact createMetaContact(ProtocolProviderService provider,
                                  MetaContactGroup contactGroup,
                                  String contactID)
        throws MetaContactListException;

    /**
     * Moves the specified <tt>MetaContact</tt> to <tt>newGroup</tt>.
     * <p>
     * @param metaContact the <tt>MetaContact</tt> to move.
     * @param newGroup the <tt>MetaContactGroup</tt> that should be the
     * new parent of <tt>contact</tt>.
     *
     * @throws MetaContactListException with an appropriate code if the
     * operation fails for some reason.
     */
    void moveMetaContact(MetaContact metaContact,
                         MetaContactGroup newGroup)
        throws MetaContactListException;

    /**
     * Sets the display name for <tt>metaContact</tt> to be <tt>newName</tt>.
     * <p>
     * @param metaContact the <tt>MetaContact</tt> that we are renaming
     * @param newName a <tt>String</tt> containing the new display name for
     * <tt>metaContact</tt>.
     * @throws IllegalArgumentException if <tt>metaContact</tt> is not an
     * instance that belongs to the underlying implementation.
     */
    void renameMetaContact(MetaContact metaContact, String newName)
        throws IllegalArgumentException;

    /**
     * Resets display name of the MetaContact to show the value from
     * the underlying contacts.
     * @param metaContact the <tt>MetaContact</tt> that we are operating on
     * @throws IllegalArgumentException if <tt>metaContact</tt> is not an
     * instance that belongs to the underlying implementation.
     */
    void clearUserDefinedDisplayName(MetaContact metaContact)
        throws IllegalArgumentException;

    /**
     * Removes the specified <tt>metaContact</tt> as well as all of its
     * underlying contacts.
     * <p>
     * @param metaContact the metaContact to remove.
     * @throws MetaContactListException with an appropriate code if the
     * operation fails for some reason.
     */
    void removeMetaContact(MetaContact metaContact)
        throws MetaContactListException;

    /**
     * Creates a <tt>MetaContactGroup</tt> with the specified group name.
     * Initially, the group would only be created locally. Corresponding
     * server stored groups will be created on the fly, whenever real protocol
     * specific contacts are added to the group if the protocol lying behind
     * them supports that.
     * <p>
     * @param parentGroup the <tt>MetaContactGroup</tt> that should be the
     * parent of the newly created group.
     * @param groupName the name of the <tt>MetaContactGroup</tt> to create.
     * @return the newly created <tt>MetaContactGroup</tt>
     *
     * @throws MetaContactListException with an appropriate code if the
     * operation fails for some reason.
     */
    MetaContactGroup createMetaContactGroup(MetaContactGroup parentGroup,
                                            String groupName)
        throws MetaContactListException;

    /**
     * Renames the specified <tt>MetaContactGroup</tt> as indicated by the
     * <tt>newName</tt> param.
     * The operation would only affect the local meta group and would not
     * "touch" any encapsulated protocol specific group.
     * <p>
     * @param group the <tt>MetaContactGroup</tt> to rename.
     * @param newGroupName the new name of the <tt>MetaContactGroup</tt> to
     * rename.
     *
     * @throws MetaContactListException with an appropriate code if the
     * operation fails for some reason.
     */
    void renameMetaContactGroup(MetaContactGroup group,
                                String newGroupName)
        throws MetaContactListException;

    /**
     * Removes the specified meta contact group, all its corresponding protocol
     * specific groups and all their children. If some of the children belong to
     * server stored contact lists, they will be updated to not include the
     * child contacts any more.
     * @param groupToRemove the <tt>MetaContactGroup</tt> to have removed.
     *
     * @throws MetaContactListException with an appropriate code if the
     * operation fails for some reason.
     */
    void removeMetaContactGroup(MetaContactGroup groupToRemove)
        throws MetaContactListException;
}
