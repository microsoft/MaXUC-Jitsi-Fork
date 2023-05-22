/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.callhistory;

import static org.jitsi.util.Hasher.logHasher;

import java.util.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.callhistory.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;

/**
 * The <tt>CallHistorySourceContact</tt> is an implementation of the
 * <tt>SourceContact</tt> interface based on a <tt>CallRecord</tt>.
 *
 * @author Yana Stamcheva
 */
public class CallHistorySourceContact
    extends DataObject
    implements SourceContact
{
    /** The config service. */
    private static final ConfigurationService sConfigService =
        CallHistoryActivator.getConfigurationService();

    /**
     * The resources service.
     */
    private static final ResourceManagementService mResources =
        CallHistoryActivator.getResources();

    /**
     * The parent <tt>CallHistoryContactSource</tt>, where this contact is
     * contained.
     */
    private final CallHistoryContactSource mContactSource;

    /**
     * The corresponding call record.
     */
    private final CallRecord mCallRecord;

    /**
     * Property to show detailed or compact dates and times.
     */
    private static final String USE_COMPACT_DATETIME_PROP =
        "net.java.sip.communicator.impl.gui.USE_COMPACT_DATETIME";

    /**
     * The incoming call icon.
     */
    private static final BufferedImageFuture mIncomingIcon
        = mResources.getBufferedImage("service.gui.icons.INCOMING_CALL");

    /**
     * The outgoing call icon.
     */
    private static BufferedImageFuture mOutgoingIcon
        = mResources.getBufferedImage("service.gui.icons.OUTGOING_CALL");

    /**
     * The missed call icon.
     */
    private static BufferedImageFuture mMissedCallIcon
        = mResources.getBufferedImage("service.gui.icons.MISSED_CALL");

    /**
     * A list of all contact details.
     */
    private final List<ContactDetail> mContactDetails
        = new LinkedList<>();

    /**
     * The display name of this contact.
     */
    private String mDisplayName = "";

    /**
     * A custom display name to show instead of the normal display name
     */
    private String mCustomDisplayName;

    /**
     * The display details of this contact.
     */
    private final String mDisplayDetails;

    /**
     * The toolTip display details of this contact.
     */
    private final String mToolTipDisplayDetails;

    /**
     * The toolTip peer address is used if there was a single recipient on this
     * call. In this case it is their phone number
     */
    private String mToolTipPeerAddress = null;

    /**
     * The timestamp when the call was started.
     */
    private final Date mTimestamp;

    /**
     * The string to use for incoming calls
     */
    private static final String HISTORY_INCOMING = mResources.
                                  getI18NString("service.gui.HISTORY_INCOMING");

    /**
     * The string to use for outgoin calls
     */
    private static final String HISTORY_OUTGOING = mResources.
                                  getI18NString("service.gui.HISTORY_OUTGOING");

    /**
     * The string to use for missed calls
     */
    private static final String HISTORY_MISSED = mResources.
                                  getI18NString("service.gui.HISTORY_MISSED");

    /**
     * The string to use for conference calls
     */
    private static final String HISTORY_CONFERENCE = mResources.
                                  getI18NString("service.gui.CONFERENCE");

    /**
     * Creates an instance of <tt>CallHistorySourceContact</tt>
     * @param contactSource the contact source
     * @param callRecord the call record
     */
    public CallHistorySourceContact(CallHistoryContactSource contactSource,
                                    CallRecord callRecord)
    {
        mContactSource = contactSource;
        mCallRecord = callRecord;
        mTimestamp = callRecord.getStartTime();

        initPeerDetails();

        String duration = "";

        // Get the call duration in the correct format, depending on whether
        // we're using compact date and time format.
        if (sConfigService.user().getBoolean(USE_COMPACT_DATETIME_PROP, false))
        {
            duration = GuiUtils.formatCompactDuration(
                mTimestamp, callRecord.getEndTime());
        }
        else
        {
            duration = " " + CallHistoryActivator.getResources()
                .getI18NString("service.gui.DURATION") + ": "
                    + GuiUtils.formatTime(mTimestamp, callRecord.getEndTime());
        }

        if (callRecord.getDirection().equals(CallRecord.IN))
        {
            // if the call record has reason for normal call clearing
            // means it was answered somewhere else and we don't
            // mark it as missed
            if (mTimestamp.equals(callRecord.getEndTime())
                && (callRecord.getEndReason() !=
                                      CallPeerChangeEvent.NORMAL_CALL_CLEARING))
            {
                // This was a missed call
                mToolTipDisplayDetails = "<em style=\"font-size:" +
                                         ScaleUtils.scaleInt(9) +
                                         "px;color:red;\">" +
                                         HISTORY_MISSED + "</em>";
            }
            else
            {
                // This was an answered incoming call
                mToolTipDisplayDetails = "<em style=\"font-size:" +
                                         ScaleUtils.scaleInt(9) +
                                         "px;\"><em style=\"color:green;\">" +
                                         HISTORY_INCOMING +
                                         "</em>" +
                                         duration +
                                         "</em>";
            }
        }
        else if (callRecord.getDirection().equals(CallRecord.OUT))
        {
            // This was an outgoing call
            mToolTipDisplayDetails = "<em style=\"font-size:" +
                                     ScaleUtils.scaleInt(9) +
                                     "px;\"><em style=\"color:blue;\">" +
            HISTORY_OUTGOING + "</em>" + duration + "</em>";
        }
        else
        {
            mToolTipDisplayDetails = "";
        }

        mDisplayDetails = getDateString(mTimestamp.getTime());

        setData("MetaContactUID", callRecord.getCallPeerContactUID());
        setData(SourceContact.DATA_TYPE, SourceContact.Type.CALL_HISTORY);
    }

    /**
     * Initializes peer details.
     */
    private void initPeerDetails()
    {
        Iterator<CallPeerRecord> recordsIter
            = mCallRecord.getPeerRecords().iterator();

        setData(CallHistoryService.CALL_HISTORY_DIRECTION_KEY,
                mCallRecord.getDirection());

        while (recordsIter.hasNext())
        {
            CallPeerRecord peerRecord = recordsIter.next();

            String peerAddress = peerRecord.getPeerAddress();

            // SIP addresses should always appear as phone numbers, without
            // an account registrar e.g. 0123456789 not
            // 0123456789@sbc.metaswitch.com
            //
            // If the address starts with '@' (which would be strange) treat
            // it as a null address.
            if ((peerAddress != null) && (!peerAddress.startsWith("@")))
            {
                // Strip everything after and including the first occurrence of
                // '@'.  Given the string doesn't start with '@', there will
                // always be something remaining.  If '@' doesn't appear in
                // the string, the whole string is used.
                peerAddress = peerAddress.split("@")[0];

                // If this is the only CallPeerRecord within this CallRecord
                // then save it for use by the tooltip
                mToolTipPeerAddress = peerAddress;

                ContactDetail contactDetail = new ContactDetail(
                    peerAddress, ContactDetail.Category.Phone, null);

                Map<Class<? extends OperationSet>, ProtocolProviderService>
                    preferredProviders = null;
                Map<Class<? extends OperationSet>, String>
                    preferredProtocols = null;

                ProtocolProviderService preferredProvider
                    = mCallRecord.getProtocolProvider();

                if (preferredProvider != null)
                {
                    preferredProviders
                        = new Hashtable<>();

                    OperationSetPresence opSetPres =
                        preferredProvider.getOperationSet(
                                OperationSetPresence.class);

                    Contact contact = null;
                    if(opSetPres != null)
                        contact = opSetPres.findContactByID(peerAddress);

                    OperationSetContactCapabilities opSetCaps =
                        preferredProvider.getOperationSet(
                                OperationSetContactCapabilities.class);

                    if(opSetCaps != null && opSetPres != null)
                    {
                        if(contact != null && opSetCaps.getOperationSet(
                                contact,
                                OperationSetBasicTelephony.class) != null)
                        {
                            preferredProviders.put(
                                    OperationSetBasicTelephony.class,
                                    preferredProvider);
                        }
                    }
                    else
                    {
                        preferredProviders.put(OperationSetBasicTelephony.class,
                                            preferredProvider);
                    }

                    contactDetail.setPreferredProviders(preferredProviders);
                }
                // If there's no preferred provider set we just specify that
                // the SIP protocol should be used for the telephony operation
                // set. This is needed for all history records stored before
                // the protocol provider property had been introduced.
                else
                {
                    preferredProtocols
                        = new Hashtable<>();

                    preferredProtocols.put(OperationSetBasicTelephony.class,
                                           ProtocolNames.SIP);

                    contactDetail.setPreferredProtocols(preferredProtocols);
                }

                LinkedList<Class<? extends OperationSet>> supportedOpSets
                    = new LinkedList<>();

                // if the contact supports call
                if((preferredProviders != null &&
                        preferredProviders.containsKey(
                                OperationSetBasicTelephony.class)) ||
                                (preferredProtocols != null))
                {
                    supportedOpSets.add(OperationSetBasicTelephony.class);
                }

                // can be added as contacts
                supportedOpSets.add(OperationSetPersistentPresence.class);

                contactDetail.setSupportedOpSets(supportedOpSets);

                mContactDetails.add(contactDetail);

                // Set the displayName.
                String name = peerRecord.getDisplayName();

                if (name == null || name.length() <= 0)
                {
                    name = peerAddress;
                }

                if (mDisplayName == null || mDisplayName.length() <= 0)
                {
                    if (mCallRecord.getPeerRecords().size() > 1)
                    {
                        mDisplayName = HISTORY_CONFERENCE;
                    }
                    else
                    {
                        mDisplayName = name;
                    }
                }
            }
        }
    }

    /**
     * Returns a list of available contact details.
     * @return a list of available contact details
     */
    public List<ContactDetail> getContactDetails()
    {
        return new LinkedList<>(mContactDetails);
    }

    /**
     * Returns the parent <tt>ContactSourceService</tt> from which this contact
     * came from.
     * @return the parent <tt>ContactSourceService</tt> from which this contact
     * came from
     */
    public ContactSourceService getContactSource()
    {
        return mContactSource;
    }

    /**
     * Returns the display details of this search contact. This could be any
     * important information that should be shown to the user.
     *
     * @return the display details of the search contact
     */
    public String getDisplayDetails()
    {
        return mDisplayDetails;
    }

    /**
     * Returns the details to be displayed in the tooltip of this search
     * contact. This could be any important information that should be shown
     * to the user.
     *
     * @return the tooltip display details of the search contact
     */
    public String getTooltipDisplayDetails()
    {
        return mToolTipDisplayDetails;
    }

    /**
     * Returns the display name of this search contact. This is a user-friendly
     * name that could be shown in the user interface.
     *
     * @return the display name of this search contact
     */
    public String getDisplayName()
    {
        return mCustomDisplayName == null ? mDisplayName : mCustomDisplayName;
    }

    public void setDisplayName(String displayName)
    {
        mCustomDisplayName = displayName;
    }

    /**
     * An image (or avatar) corresponding to this search contact. If such is
     * not available this method will return null.
     *
     * @return the byte array of the image or null if no image is available
     */
    public BufferedImageFuture getImage()
    {
        if (mCallRecord.getDirection().equals(CallRecord.IN))
        {
            // if the call record has reason for normal call clearing
            // means it was answered somewhere else and we don't
            // mark it as missed
            if (mCallRecord.getStartTime().equals(mCallRecord.getEndTime())
                && (mCallRecord.getEndReason()
                        != CallPeerChangeEvent.NORMAL_CALL_CLEARING))
                return mMissedCallIcon;
            else
                return mIncomingIcon;
        }
        else if (mCallRecord.getDirection().equals(CallRecord.OUT))
            return mOutgoingIcon;

        return null;
    }

    public String getImageDescription()
    {
        BufferedImageFuture image = getImage();

        return mResources.getI18NString(
               image == mMissedCallIcon ? "service.gui.accessibility.MISSED_CALL" :
               image == mIncomingIcon ? "service.gui.accessibility.INCOMING_CALL" :
               image == mOutgoingIcon ? "service.gui.accessibility.OUTGOING_CALL" :
               "service.gui.accessibility.UNKNOWN_CALL");
    }

    /**
     * Returns a list of all <tt>ContactDetail</tt>s supporting the given
     * <tt>OperationSet</tt> class.
     * @param operationSet the <tt>OperationSet</tt> class we're looking for
     * @return a list of all <tt>ContactDetail</tt>s supporting the given
     * <tt>OperationSet</tt> class
     */
    public List<ContactDetail> getContactDetails(
                                    Class<? extends OperationSet> operationSet)
    {
        // We support only call details
        // or persistence presence so we can add contacts.
        if (!(operationSet.equals(OperationSetBasicTelephony.class)
                || operationSet.equals(OperationSetPersistentPresence.class)))
            return null;

        return new LinkedList<>(mContactDetails);
    }

    /**
     * Returns a list of all <tt>ContactDetail</tt>s corresponding to the given
     * category.
     * @param category the <tt>OperationSet</tt> class we're looking for
     * @return a list of all <tt>ContactDetail</tt>s corresponding to the given
     * category
     */
    public List<ContactDetail> getContactDetails(
        ContactDetail.Category category)
        throws OperationNotSupportedException
    {
        // We don't support category for call history details, so we return null.
        throw new OperationNotSupportedException(
            "Categories are not supported for call history records.");
    }

    /**
     * Returns the preferred <tt>ContactDetail</tt> for a given
     * <tt>OperationSet</tt> class.
     * @param operationSet the <tt>OperationSet</tt> class, for which we would
     * like to obtain a <tt>ContactDetail</tt>
     * @return the preferred <tt>ContactDetail</tt> for a given
     * <tt>OperationSet</tt> class
     */
    public ContactDetail getPreferredContactDetail(
        Class<? extends OperationSet> operationSet)
    {
        // We support only call details
        // or persistence presence so we can add contacts.
        if (!(operationSet.equals(OperationSetBasicTelephony.class)
                || operationSet.equals(OperationSetPersistentPresence.class)))
            return null;

        return mContactDetails.get(0);
    }

    /**
     * Returns the date string to show for the given date.
     *
     * @param date the date to format
     * @return the date string to show for the given date
     */
    public static String getDateString(long date)
    {
        // Get the time.
        String time = GuiUtils.formatTime(date);

        // Get the date, unless it's today.
        if (GuiUtils.compareDatesOnly(date, System.currentTimeMillis()) < 0)
        {
            StringBuilder dateStrBuilder = new StringBuilder();
            GuiUtils.formatDate(date, dateStrBuilder);
            return dateStrBuilder.append(" ").append(time).toString();
        }

        return time;
    }

    /**
     * Returns the status of the source contact. And null if such information
     * is not available.
     * @return the PresenceStatus representing the state of this source contact.
     */
    public PresenceStatus getPresenceStatus()
    {
        return null;
    }

    /**
     * Returns the index of this source contact in its parent.
     *
     * @return the index of this source contact in its parent
     */
    public int getIndex()
    {
        return -1;
    }

    public boolean isEnabled()
    {
        // Is always enabled
        return true;
    }

    public void setEnabled(boolean enabled)
    {
        // Does nothing
    }

    @Override
    public String getEmphasizedNumber()
    {
        return CallHistoryActivator.getPhoneNumberUtilsService().
                                   formatNumberToNational(mToolTipPeerAddress);
    }

    @Override
    public Date getTimestamp()
    {
        return mTimestamp;
    }

    @Override
    public boolean canBeMessaged()
    {
        // A CallHistorySourceContact can be messaged if SMS is enabled and the
        // number of the contact is a valid SMS number.
        return (ConfigurationUtils.isSmsEnabled() &&
                CallHistoryActivator.getPhoneNumberUtilsService().
                    isValidSmsNumber(getEmphasizedNumber()));
    }

    /**
     * Returns a hash of the contact's display name so that we can log the
     * SourceContact without logging any PII.
     *
     * @return  the hashed display name of the SourceContact.
     */
    @Override
    public String toString()
    {
        return logHasher(getDisplayName());
    }
}
