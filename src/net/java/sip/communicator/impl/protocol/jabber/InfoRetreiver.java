/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import static net.java.sip.communicator.util.PrivacyUtils.sanitiseChatAddress;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.*;

import com.google.common.annotations.VisibleForTesting;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;

import net.java.sip.communicator.service.imageloader.BufferedImageAvailableFromBytes;
import net.java.sip.communicator.service.protocol.ServerStoredDetails;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.AddressDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.CityDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.DisplayNameDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.EmailAddressDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.FaxDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.FirstNameDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.ImageDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.LastNameDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.MiddleNameDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.MobilePhoneDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.NameDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.NicknameDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.PagerDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.PhoneNumberDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.PostalCodeDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.ProvinceDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.WorkAddressDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.WorkCityDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.WorkOrganizationNameDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.WorkPostalCodeDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.WorkProvinceDetail;
import net.java.sip.communicator.util.Logger;
import org.jitsi.service.resources.BufferedImageFuture;

/**
 * Handles and retrieves all info of our contacts or our account info
 *
 * @author Damian Minkov
 */
public class InfoRetreiver
{
    private static final Logger sLog =
        Logger.getLogger(InfoRetreiver.class);

    /**
     * A callback to the Jabber provider that created us.
     */
    @VisibleForTesting
    public ProtocolProviderServiceJabberImpl jabberProvider;

    /**
     * Map to hold all the details retrieved so far
     */
    @VisibleForTesting
    public Map<String, List<GenericDetail>> retrievedDetails
        = new ConcurrentHashMap<>();

    private static final String TAG_FN_OPEN = "<FN>";
    private static final String TAG_FN_CLOSE = "</FN>";

    /**
     * The name of the property which indicates whether detailed server stored
     * info is supported for Jabber e.g. organization, phone number
     */
    @VisibleForTesting
    public boolean extraInfoDisabled =
        JabberActivator.getConfigurationService().global().getBoolean(
            "net.java.sip.communicator.impl.protocol.jabber." +
            "EXTRA_VCARD_INFO_DISABLED",
            false);

    /**
     * The timeout to wait before considering vcard has time outed.
     */
    @VisibleForTesting
    public long vcardTimeoutReply;

    protected InfoRetreiver(ProtocolProviderServiceJabberImpl jabberProvider)
    {
        this.jabberProvider = jabberProvider;

        vcardTimeoutReply
            = JabberActivator.getConfigurationService().global().getLong(
                    ProtocolProviderServiceJabberImpl
                        .VCARD_REPLY_TIMEOUT_PROPERTY,
                    -1);
    }

    /**
     * returns the user details from the specified class or its descendants
     * the class is one from the
     * net.java.sip.communicator.service.protocol.ServerStoredDetails
     * or implemented one in the operation set for the user info
     *
     * @param uin String
     * @param detailClass Class
     * @return Iterator
     */
    <T extends GenericDetail> Iterator<T> getDetailsAndDescendants(
        String uin,
        Class<T> detailClass)
    {
        List<GenericDetail> details = getContactDetails(uin);
        List<T> result = new LinkedList<>();

        for (GenericDetail item : details)
            if(detailClass.isInstance(item))
            {
                @SuppressWarnings("unchecked")
                T t = (T) item;

                result.add(t);
            }

        return result.iterator();
    }

    /**
     * returns the user details from the specified class
     * exactly that class not its descendants
     *
     * @param uin String
     * @param detailClass Class
     * @return Iterator
     */
    Iterator<GenericDetail> getDetails(
        String uin,
        Class<? extends GenericDetail> detailClass)
    {
        List<GenericDetail> details = getContactDetails(uin);
        List<GenericDetail> result = new LinkedList<>();

        if (details != null)
        {
            for (GenericDetail item : details)
                if(detailClass.equals(item.getClass()))
                    result.add(item);
        }

        return result.iterator();
    }

    /**
     * request the full info for the given contactAddress
     * waits and return this details
     *
     * @param contactAddress String
     * @return Vector the details
     */
    List<GenericDetail> getContactDetails(String contactAddress)
    {
        List<GenericDetail> result = getCachedContactDetails(contactAddress);

        if (result == null)
        {
            result = retrieveDetails(contactAddress);
        }

        return result;
    }

    /**
     * Retrieve details and return them or if missing return an empty list.
     * @param contactAddress the address to search for.
     * @return the details or empty list.
     */
    protected List<GenericDetail> retrieveDetails(String contactAddress)
    {
        // Don't run this method on the EDT - it queries the server for details
        // thus can block it if there is a poor network.
        if (SwingUtilities.isEventDispatchThread())
            return null;

        List<GenericDetail> result = new LinkedList<>();

        Throwable throwable = null;
        try
        {
            XMPPConnection connection = jabberProvider.getConnection();
            EntityBareJid contactAddressAsJid =
                JidCreate.entityBareFrom(contactAddress);

            if(connection == null || !connection.isAuthenticated())
                return null;

            VCard card = load(connection, contactAddressAsJid);

            String tmp;

            tmp = checkForFullName(card);
            if(tmp != null)
                result.add(new DisplayNameDetail(tmp));

            tmp = card.getFirstName();
            if(tmp != null)
                result.add(new FirstNameDetail(tmp));

            tmp = card.getMiddleName();
            if(tmp != null)
                result.add(new MiddleNameDetail(tmp));

            tmp = card.getLastName();
            if(tmp != null)
                result.add(new LastNameDetail(tmp));

            tmp = card.getNickName();
            if(tmp != null)
                result.add(new NicknameDetail(tmp));

            if (!extraInfoDisabled)
            {
                // Home Details
                // addrField one of
                // POSTAL, PARCEL, (DOM | INTL), PREF, POBOX, EXTADR, STREET,
                // LOCALITY, REGION, PCODE, CTRY
                tmp = card.getAddressFieldHome("STREET");
                if(tmp != null)
                    result.add(new AddressDetail(tmp));

                tmp = card.getAddressFieldHome("LOCALITY");
                if(tmp != null)
                    result.add(new CityDetail(tmp));

                tmp = card.getAddressFieldHome("REGION");
                if(tmp != null)
                    result.add(new ProvinceDetail(tmp));

                tmp = card.getAddressFieldHome("PCODE");
                if(tmp != null)
                    result.add(new PostalCodeDetail(tmp));

//                tmp = card.getAddressFieldHome("CTRY");
//                if(tmp != null)
//                    result.add(new CountryDetail(tmp);

            // phoneType one of
            //VOICE, FAX, PAGER, MSG, CELL, VIDEO, BBS, MODEM, ISDN, PCS, PREF

                tmp = card.getPhoneHome("VOICE");
                if(tmp != null)
                    result.add(new PhoneNumberDetail(tmp));

                tmp = card.getPhoneHome("FAX");
                if(tmp != null)
                    result.add(new FaxDetail(tmp));

                tmp = card.getPhoneHome("PAGER");
                if(tmp != null)
                    result.add(new PagerDetail(tmp));

                tmp = card.getPhoneHome("CELL");
                if(tmp != null)
                    result.add(new MobilePhoneDetail(tmp));

                tmp = card.getEmailHome();
                if(tmp != null)
                    result.add(new EmailAddressDetail(tmp));

                // Work Details
                // addrField one of
                // POSTAL, PARCEL, (DOM | INTL), PREF, POBOX, EXTADR, STREET,
                // LOCALITY, REGION, PCODE, CTRY
                tmp = card.getAddressFieldWork("STREET");
                if(tmp != null)
                    result.add(new WorkAddressDetail(tmp));

                tmp = card.getAddressFieldWork("LOCALITY");
                if(tmp != null)
                    result.add(new WorkCityDetail(tmp));

                tmp = card.getAddressFieldWork("REGION");
                if(tmp != null)
                    result.add(new WorkProvinceDetail(tmp));

                tmp = card.getAddressFieldWork("PCODE");
                if(tmp != null)
                    result.add(new WorkPostalCodeDetail(tmp));

//                tmp = card.getAddressFieldWork("CTRY");
//                if(tmp != null)
//                    result.add(new WorkCountryDetail(tmp);

            // phoneType one of
            //VOICE, FAX, PAGER, MSG, CELL, VIDEO, BBS, MODEM, ISDN, PCS, PREF

                tmp = card.getPhoneWork("VOICE");
                if(tmp != null)
                    result.add(new ServerStoredDetails.WorkPhoneDetail(tmp));

                tmp = card.getPhoneWork("FAX");
                if(tmp != null)
                    result.add(new WorkFaxDetail(tmp));

                tmp = card.getPhoneWork("PAGER");
                if(tmp != null)
                    result.add(new WorkPagerDetail(tmp));

                tmp = card.getPhoneWork("CELL");
                if(tmp != null)
                    result.add(
                        new ServerStoredDetails.WorkMobilePhoneDetail(tmp));

                tmp = card.getEmailWork();
                if(tmp != null)
                    result.add(new EmailAddressDetail(tmp));

                tmp = card.getOrganization();
                if(tmp != null)
                    result.add(new WorkOrganizationNameDetail(tmp));

                tmp = card.getOrganizationUnit();
                if(tmp != null)
                    result.add(new WorkDepartmentNameDetail(tmp));
            }

            BufferedImageFuture imageBytes = BufferedImageAvailableFromBytes.fromBytes(card.getAvatar());
            if(imageBytes != null)
                result.add(new ImageDetail("Image", imageBytes));
        }
        catch (XMPPErrorException ex)
        {
            StanzaError error = ex.getStanzaError();

            // Ignore code 503 (service-unavailable), as it is used on some IM
            // servers (e.g. mongooseim) to indicate that the user has no vcard.
            // Therefore 503 is not an error, and should just result in an empty
            // vCard.
            if (error.getCondition() != StanzaError.Condition.service_unavailable)
                throwable = ex;
        }
        catch (Throwable t)
        {
            throwable = t;
        }

        // Handle any errors thrown while getting or processing the vCard.
        if (throwable != null)
        {
            sLog.error("Cannot load details for contact " + sanitiseChatAddress(contactAddress));
        }

        retrievedDetails.put(contactAddress, result);

        return result;
    }

    /**
     * request the full info for the given contactAddress if available
     * in cache.
     *
     * @param contactAddress to search for
     * @return list of the details if any.
     */
    List<GenericDetail> getCachedContactDetails(String contactAddress)
    {
        return retrievedDetails.get(contactAddress);
    }

    /**
     * Checks for full name tag in the <tt>card</tt>.
     * @param card the card to check.
     * @return the Full name if existing, null otherwise.
     */
    private String checkForFullName(VCard card)
    {
        String vcardXml = card.toXML().toString();

        int indexOpen = vcardXml.indexOf(TAG_FN_OPEN);

        if(indexOpen == -1)
            return null;

        int indexClose = vcardXml.indexOf(TAG_FN_CLOSE, indexOpen);

        // something is wrong!
        if(indexClose == -1)
            return null;

        return vcardXml.substring(indexOpen + TAG_FN_OPEN.length(), indexClose);
    }

    /**
     * Load VCard for the given contact.
     *
     * @param connection XMPP connection
     * @param contactAddressAsJid the contact
     * @return loaded VCard
     * @throws XMPPErrorException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     * @throws SmackException.NotConnectedException
     */
    VCard load(
        XMPPConnection connection,
        EntityBareJid contactAddressAsJid)
            throws SmackException.NoResponseException, XMPPErrorException,
            SmackException.NotConnectedException, InterruptedException
    {
        VCard card;

        // if there is no value load vcard using default load method
        if (vcardTimeoutReply <= 0)
        {
            card = VCardManager.getInstanceFor(connection).loadVCard(contactAddressAsJid);
        }
        else
        {
            card = VCardManager.getInstanceFor(connection).loadVCard(contactAddressAsJid, vcardTimeoutReply);
        }

        return card;
    }

    /**
     * Work department
     */
    public static class WorkDepartmentNameDetail
        extends NameDetail
    {
        /**
         * Constructor.
         *
         * @param workDepartmentName name of the work department
         */
        public WorkDepartmentNameDetail(String workDepartmentName)
        {
            super("Work Department Name", workDepartmentName);
        }
    }

    /**
     * Fax at work
     */
    public static class WorkFaxDetail
        extends FaxDetail
    {
        /**
         * Constructor.
         *
         * @param number work fax number
         */
        public WorkFaxDetail(String number)
        {
            super(number);
            super.detailDisplayName = "WorkFax";
        }
    }

    /**
     * Pager at work
     */
    public static class WorkPagerDetail
        extends PhoneNumberDetail
    {
        /**
         * Constructor.
         *
         * @param number work pager number
         */
        public WorkPagerDetail(String number)
        {
            super(number);
            super.detailDisplayName = "WorkPager";
        }
    }
}
