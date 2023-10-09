/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.packet.id.StandardStanzaIdSource;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;

import net.java.sip.communicator.service.protocol.AbstractOperationSetServerStoredAccountInfo;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationSetPersistentPresence;
import net.java.sip.communicator.service.protocol.ServerStoredDetails;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.ImageDetail;
import net.java.sip.communicator.service.protocol.event.ServerStoredDetailsChangeEvent;
import net.java.sip.communicator.util.Logger;

/**
 * The Account Info Operation set is a means of accessing and modifying detailed
 * information on the user/account that is currently logged in through this
 * provider.
 *
 * @author Damian Minkov
 */
public class OperationSetServerStoredAccountInfoJabberImpl
    extends AbstractOperationSetServerStoredAccountInfo
{
    /**
     * The logger.
     */
    private static final Logger logger =
        Logger.getLogger(OperationSetServerStoredAccountInfoJabberImpl.class);

    private static final int CUSTOM_SAVE_VCARD_REPLY_TIMEOUT_IN_MILLIS = 30000;

    /**
     * The info retriever.
     */
    private InfoRetreiver infoRetreiver = null;

    /**
     * The jabber provider that created us.
     */
    private ProtocolProviderServiceJabberImpl jabberProvider = null;

    /**
     * Our account UIN.
     */
    private String uin = null;

    protected OperationSetServerStoredAccountInfoJabberImpl(
        ProtocolProviderServiceJabberImpl jabberProvider,
        InfoRetreiver infoRetreiver,
        String uin)
    {
        this.infoRetreiver = infoRetreiver;
        this.jabberProvider = jabberProvider;
        this.uin = uin;
    }

    /**
     * Returns an iterator over all details that are instances or descendants of
     * the specified class. If for example an our account has a work address
     * and an address detail, a call to this method with AddressDetail.class
     * would return both of them.
     * <p>
     * @param detailClass one of the detail classes defined in the
     * ServerStoredDetails class, indicating the kind of details we're
     * interested in.
     * <p>
     * @return a java.util.Iterator over all details that are instances or
     * descendants of the specified class.
     */
    public <T extends GenericDetail> Iterator<T> getDetailsAndDescendants(
        Class<T> detailClass)
    {
        assertConnected();

        return infoRetreiver.getDetailsAndDescendants(uin, detailClass);
    }

    /**
     * Returns an iterator over all details that are instances of exactly the
     * same class as the one specified. Not that, contrary to the
     * getDetailsAndDescendants() method this one would only return details
     * that are instances of the specified class and not only its descendants.
     * If for example our account has both a work address and an address detail,
     * a call to this method with AddressDetail.class would return only the
     * AddressDetail instance and not the WorkAddressDetail instance.
     * <p>
     * @param detailClass one of the detail classes defined in the
     * ServerStoredDetails class, indicating the kind of details we're
     * interested in.
     * <p>
     * @return a java.util.Iterator over all details of specified class.
     */
    public Iterator<GenericDetail> getDetails(
        Class<? extends GenericDetail> detailClass)
    {
        assertConnected();

        return infoRetreiver.getDetails(uin, detailClass);
    }

    /**
     * Returns all details currently available and set for our account.
     * <p>
     * @return a java.util.Iterator over all details currently set our account.
     */
    public Iterator<GenericDetail> getAllAvailableDetails()
    {
        assertConnected();

        List<GenericDetail> results = new LinkedList<>();

        List<GenericDetail> contactDetails = infoRetreiver.getContactDetails(uin);

        if (contactDetails != null)
        {
            results.addAll(contactDetails);
        }

        return results.iterator();
    }

    /**
     * Determines whether a detail class represents a detail supported by the
     * underlying implementation or not. Note that if you call one of the
     * modification methods (add remove or replace) with a detail that this
     * method has determined to be unsupported (returned false) this would lead
     * to an IllegalArgumentException being thrown.
     * <p>
     * @param detailClass the class the support for which we'd like to
     * determine.
     * <p>
     * @return true if the underlying implementation supports setting details of
     * this type and false otherwise.
     */
    public boolean isDetailClassSupported(
        Class<? extends GenericDetail> detailClass)
    {
        List<GenericDetail> details = infoRetreiver.getContactDetails(uin);

        for (GenericDetail obj : details)
            if(detailClass.isAssignableFrom(obj.getClass()))
                return true;
        return false;
    }

    /**
     * The method returns the number of instances supported for a particular
     * detail type. Some protocols offer storing multiple values for a
     * particular detail type. Spoken languages are a good example.
     * @param detailClass the class whose max instance number we'd like to find
     * out.
     * <p>
     * @return int the maximum number of detail instances.
     */
    public int getMaxDetailInstances(Class<? extends GenericDetail> detailClass)
    {
        return 1;
    }

    /**
     * Adds the specified detail to the list of details registered on-line
     * for this account. If such a detail already exists its max instance number
     * is consulted and if it allows it - a second instance is added or otherwise
     * and illegal argument exception is thrown. An IllegalArgumentException is
     * also thrown in case the class of the specified detail is not supported by
     * the underlying implementation, i.e. its class name was not returned by the
     * getSupportedDetailTypes() method.
     * <p>
     * @param detail the detail that we'd like registered on the server.
     * <p>
     * @throws IllegalArgumentException if such a detail already exists and its
     * max instances number has been attained or if the underlying
     * implementation does not support setting details of the corresponding
     * class.
     * @throws OperationFailedException with if putting the new value online has
     * failed. Note that if we get an error response back from the server
     * we store that error value in the exception code - which isn't quite what
     * that code is intended for, but is a convenient way to pass up that
     * information.
     * @throws java.lang.ArrayIndexOutOfBoundsException if the number of
     * instances currently registered by the application is already equal to the
     * maximum number of supported instances (@see getMaxDetailInstances())
     */
    public void addDetail(ServerStoredDetails.GenericDetail detail)
        throws IllegalArgumentException,
               OperationFailedException,
               ArrayIndexOutOfBoundsException
    {
        assertConnected();

        /*
        Currently as the function only provided the list of classes that
        currently have data associated with them
         in Jabber InfoRetreiver we have to skip this check*/
        //if (!isDetailClassSupported(detail.getClass())) {
        //    throw new IllegalArgumentException(
        //            "implementation does not support such details " +
        //            detail.getClass());
        //}

        Iterator<GenericDetail> iter = getDetails(detail.getClass());
        int currentDetailsSize = 0;
        while (iter.hasNext())
        {
            currentDetailsSize++;
        }

        if (currentDetailsSize >= getMaxDetailInstances(detail.getClass()))
        {
            throw new ArrayIndexOutOfBoundsException(
                    "Max count for this detail is already reached");
        }

        if(detail instanceof ImageDetail)
        {
            // Push the avatar photo to the server.
            try
            {
                this.uploadImageDetail(
                        ServerStoredDetailsChangeEvent.DETAIL_ADDED,
                        null,
                        detail);
            }
            catch (XMPPErrorException e)
            {
                XMPPErrorConvertor.reThrowAsOperationFailedException(
                    "Failed to set avatar.", e);
            }
            catch (InterruptedException | NotConnectedException |
                NoResponseException e)
            {
                throw new OperationFailedException(
                    "Failed to set avatar.",
                    OperationFailedException.GENERAL_ERROR,
                    e);
            }
        }
    }

    /**
     * Replaces the currentDetailValue detail with newDetailValue and returns
     * true if the operation was a success or false if currentDetailValue did
     * not previously exist (in this case an additional call to addDetail is
     * required).
     * <p>
     * @param currentDetailValue the detail value we'd like to replace.
     * @param newDetailValue the value of the detail that we'd like to replace
     * currentDetailValue with.
     * @return true if the operation was a success or false if
     * currentDetailValue did not previously exist (in this case an additional
     * call to addDetail is required).
     * @throws ClassCastException if newDetailValue is not an instance of the
     * same class as currentDetailValue.
     * @throws OperationFailedException if putting the new value back online has
     * failed. Note that if we get an error response back from the server
     * we store that error value in the exception code - which isn't quite what
     * that code is intended for, but is a convenient way to pass up that
     * information.
     */
    public boolean replaceDetail(
                    ServerStoredDetails.GenericDetail currentDetailValue,
                    ServerStoredDetails.GenericDetail newDetailValue)
        throws ClassCastException, OperationFailedException
    {
        assertConnected();

        if (!newDetailValue.getClass().equals(currentDetailValue.getClass()))
        {
            throw new ClassCastException(
                    "New value to be replaced is not as the current one");
        }
        // if values are the same no change
        if (currentDetailValue.equals(newDetailValue))
        {
            return true;
        }

        boolean isFound = false;
        Iterator<GenericDetail> iter =
                infoRetreiver.getDetails(uin, currentDetailValue.getClass());

        while (iter.hasNext())
        {
            GenericDetail item = iter.next();
            if (item.equals(currentDetailValue))
            {
                isFound = true;
                break;
            }
        }
        // current detail value does not exist
        if (!isFound)
        {
            return false;
        }

        if(newDetailValue instanceof ImageDetail)
        {
            // Push the new avatar photo to the server.
            try
            {
                uploadImageDetail(
                        ServerStoredDetailsChangeEvent.DETAIL_REPLACED,
                        currentDetailValue,
                        newDetailValue);
                return true;
            }
            catch (XMPPErrorException e)
            {
                XMPPErrorConvertor.reThrowAsOperationFailedException(
                    "Failed to set avatar.", e);
            }
            catch (InterruptedException | NotConnectedException |
                NoResponseException e)
            {
                throw new OperationFailedException(
                    "Failed to set avatar.",
                    OperationFailedException.GENERAL_ERROR,
                    e);
            }
        }

        return false;
    }

    /**
     * Utility method throwing an exception if the icq stack is not properly
     * initialized.
     * @throws java.lang.IllegalStateException if the underlying ICQ stack is
     * not registered and initialized.
     */
    private void assertConnected() throws IllegalStateException
    {
        if (jabberProvider == null)
            throw new IllegalStateException(
                "The jabber provider must be non-null and signed on "
                +"before being able to communicate.");
        if (!jabberProvider.isRegistered())
            throw new IllegalStateException(
                "The jabber provider must be signed on before "
                +"being able to communicate.");
    }

    /**
     * Uploads the new avatar image to the server via the vCard mechanism
     * (XEP-0153).
     *
     * @param changeEventID the int ID of the event to dispatch
     * @param currentDetailValue the detail value we'd like to replace.
     * @param newDetailValue the value of the detail that we'd like to replace
     * currentDetailValue with. If ((ImageDetail) newDetailValue).getBytes() is
     * null, then this function removes the current avatar from the server by
     * sending a vCard with a "photo" tag without any content.
     */
    private void uploadImageDetail(
            int changeEventID,
            ServerStoredDetails.GenericDetail currentDetailValue,
            ServerStoredDetails.GenericDetail newDetailValue)
        throws XMPPErrorException, InterruptedException, NotConnectedException,
            NoResponseException
    {
        try
        {
            byte[] newAvatar = ((ImageDetail) newDetailValue).getImage().getBytes();

            // Get the user's current vCard, so we can modify the stored image
            // and re-upload it to the server.
            VCard v1 = getUserVCard();

            // Checks if the new avatar photo is different from the server one.
            // If yes, then upload the new avatar photo.
            if (!Arrays.equals(v1.getAvatar(), newAvatar))
            {
                v1.setAvatar(newAvatar);

                // Update the ID of the vCard; because vCard extends Packet,
                // simply re-saving the vCard will cause us to send an IQ(set)
                // with the same ID as the IQ(get) we performed on v1.load.
                // This doesn't have any functional impact, but it makes it
                // easier to understand the packet trace, and is good practice.
                v1.setStanzaId(StandardStanzaIdSource.DEFAULT.getNewStanzaId());

                // Saves the new vCard.
                VCardManager.getInstanceFor(jabberProvider.getConnection())
                    .saveVCard(v1, CUSTOM_SAVE_VCARD_REPLY_TIMEOUT_IN_MILLIS);
            }

            // Sets the new avatar photo advertised in all presence messages,
            // and send one presence message immediately.
            ((OperationSetPersistentPresenceJabberImpl)
             this.jabberProvider.getOperationSet(
                 OperationSetPersistentPresence.class))
                .updateAccountPhotoPresenceExtension(newAvatar);

            // Advertises all detail change listeners, that the server stored
            // details have changed.
            fireServerStoredDetailsChangeEvent(
                    jabberProvider,
                    changeEventID,
                    currentDetailValue,
                    newDetailValue);
        }
        catch (XMPPErrorException | InterruptedException |
            NotConnectedException | NoResponseException e)
        {
            logger.error("Error loading/saving vcard: ", e);
            throw e;
        }
    }

    /**
     * Retrieve the user's vCard from the IM server and return it.
     * If no vCard is currently set, return an empty vCard.
     * @return The user's current vCard.
     * @throws XMPPException If the vCard could not be obtained.
     */
    private VCard getUserVCard()
        throws XMPPErrorException, NoResponseException, NotConnectedException,
            InterruptedException
    {
        VCard vCard = new VCard();

        // Retrieve the old vCard.
        try
        {
            vCard = VCardManager.getInstanceFor(jabberProvider.getConnection())
                .loadVCard();
        }
        catch (XMPPErrorException e)
        {
            // Suppress 503 errors, as some XMPP servers (such as mongooseim)
            // use 503 to indicate that no vCard is currently set.
            // In this case, we should just return an empty vCard to the caller.
            StanzaError.Condition condition;
            condition = ((XMPPErrorException)e).getStanzaError().getCondition();

            if (condition != StanzaError.Condition.service_unavailable)
            {
                throw e;
            }

        }
        catch (NoResponseException | NotConnectedException |
            InterruptedException e)
        {
            throw e;
        }

        return vCard;
    }
}
