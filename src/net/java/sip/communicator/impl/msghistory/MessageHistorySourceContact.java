/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.msghistory;

import static org.jitsi.util.Hasher.logHasher;

import java.util.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;

/**
 * The <tt>MessageHistorySourceContact</tt> is an implementation of the
 * <tt>SourceContact</tt> interface based on a <tt>MessageEvent</tt>.
 */
public class MessageHistorySourceContact
    extends DataObject
    implements SourceContact
{
    /**
     * The logger for this class.
     */
    private static final Logger sLog =
        Logger.getLogger(MessageHistorySourceContact.class);

    /**
     * The resources service.
     */
    private static final ResourceManagementService mResources =
        MessageHistoryActivator.getResources();

    /**
     * The parent <tt>MessageHistoryContactSource</tt>, where this contact is
     * contained.
     */
    private final MessageHistoryContactSource mContactSource;

    /**
     * The corresponding message event.
     */
    private final MessageEvent mMessageEvent;

    /**
     * The read message icon.
     */
    private static BufferedImageFuture mReadMessageIcon = mResources.getBufferedImage(
                                                      "service.gui.icons.CHAT");

    /**
     * The unread message icon.
     */
    private static BufferedImageFuture mUnreadMessageIcon = mResources.getBufferedImage(
                                               "service.gui.icons.CHAT_UNREAD");

    /**
     * The group message icon
     */
    private static BufferedImageFuture mGroupMessageReadIcon = mResources.getBufferedImage(
                                                "service.gui.icons.GROUP_CHAT");

    /**
     * The group message unread icon
     */
    private static BufferedImageFuture mGroupMessageUnreadIcon = mResources.getBufferedImage(
                                         "service.gui.icons.GROUP_CHAT_UNREAD");

    /**
     * The group message closed icon
     */
    private static BufferedImageFuture mGroupMessageClosedIcon = mResources.getBufferedImage(
                                         "service.gui.icons.GROUP_CHAT_CLOSED");

    /**
     * A list of all contact details.
     */
    private final List<ContactDetail> mContactDetails
        = new LinkedList<>();

    /**
     * The MetaContact associated with this SourceContact.
     */
    MetaContact mMetaContact;

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
     * message. In this case it is their JID (IM), phone number(SMS) or group
     * chat subject (Group Chat).
     */
    private String mToolTipPeerAddress = null;

    /**
     * The timestamp when the message was sent/received.
     */
    private final Date mTimestamp;

    /**
     * Whether the message has been read.
     */
    private boolean mIsMessageRead;

    /**
     * Whether this is a group chat contact
     */
    private boolean mIsGroupChat;

    /**
     * Whether this chat has been closed
     */
    private boolean mIsClosed;

    /**
     * The string to use for unread messages
     */
    private static final String MESSAGE_HISTORY_UNREAD = mResources.
                            getI18NString("service.gui.MESSAGE_HISTORY_UNREAD");

    /**
     * The string to use for IMs
     */
    private static final String MESSAGE_HISTORY_IM = mResources.
                                getI18NString("service.gui.MESSAGE_HISTORY_IM");

    /**
     * The string to use for SMSs
     */
    private static final String MESSAGE_HISTORY_SMS = mResources.
                                getI18NString("service.gui.MESSAGE_HISTORY_SMS");

    /**
     * The string to use for Group IMs
     */
    private static final String MESSAGE_HISTORY_GROUP = mResources.
                                getI18NString("service.gui.MESSAGE_HISTORY_GROUP");

    /**
     * Creates an instance of <tt>MessageHistorySourceContact</tt>
     * @param contactSource the contact source
     * @param messageEvent the message event
     */
    public MessageHistorySourceContact(MessageHistoryContactSource contactSource,
                                       MessageEvent messageEvent)
    {
        mContactSource = contactSource;
        mMessageEvent = messageEvent;

        mIsMessageRead = messageEvent.isMessageRead();

        initDetails();

        String tooltipText = "";

        if (!mIsMessageRead)
        {
            tooltipText = MESSAGE_HISTORY_UNREAD + " ";
        }

        String messageType;
        switch (messageEvent.getEventType())
        {
            case (MessageEvent.SMS_MESSAGE):
            {
                messageType = MESSAGE_HISTORY_SMS;
                break;
            }
            case (MessageEvent.GROUP_MESSAGE):
            {
                messageType = MESSAGE_HISTORY_GROUP;
                break;
            }
            case (MessageEvent.STATUS_MESSAGE):
            {
                messageType = MESSAGE_HISTORY_GROUP;
                break;
            }
            default:
            {
                messageType = MESSAGE_HISTORY_IM;
            }
        }

        tooltipText += messageType;

        mToolTipDisplayDetails = "<em style=\"font-size:" +
                                 ScaleUtils.scaleInt(9) +
                                 "px;color:grey;\">" +
                                 tooltipText + "</em>";

        mTimestamp = messageEvent.getTimestamp();
        mDisplayDetails = getDateString(mTimestamp.getTime());
    }

    /**
     * Initializes peer details.
     */
    private void initDetails()
    {
        MetaContactListService contactListService =
        MessageHistoryActivator.getContactListService();
        String peerIdentifier = mMessageEvent.getPeerIdentifier();
        String peerAddress = null;

        Contact peerContact = null;
        if (mMessageEvent instanceof OneToOneMessageEvent)
        {
            peerContact = ((OneToOneMessageEvent)mMessageEvent).getPeerContact();
        }

        // Try to find a MetaContact for the message event, searching by SMS
        // number if there is one, otherwise by contact.
        if (mMessageEvent instanceof ChatRoomMessageEvent)
        {
            setData(SourceContact.DATA_TYPE, SourceContact.Type.GROUP_MESSAGE_HISTORY);
            initChatRoomDetails(peerIdentifier,
                                ((ChatRoomMessageEvent)mMessageEvent).getSubject());
        }
        else if (mMessageEvent.getEventType() == MessageEvent.SMS_MESSAGE)
        {
            mMetaContact =
                contactListService.findMetaContactForSmsNumber(peerIdentifier);
            peerAddress = peerIdentifier;
            setData(SourceContact.DATA_TYPE, SourceContact.Type.SMS_MESSAGE_HISTORY);
            initPeerDetails(peerAddress);
        }
        else if (peerContact != null)
        {
            mMetaContact = contactListService.findMetaContactByContact(peerContact);
            peerAddress = peerContact.getAddress();
            setData(SourceContact.DATA_TYPE, SourceContact.Type.IM_MESSAGE_HISTORY);
            initPeerDetails(peerAddress);
        }
        else
        {
            sLog.error("No SMS number or contact found in MessageEvent");
        }
    }

    /**
     * Initializes the details for the chat room contained by this source
     * contact
     *
     * @param chatRoomUid the chat room Uid
     */
    private void initChatRoomDetails(String chatRoomUid, String subject)
    {
        mIsGroupChat = true;
        mIsClosed = ((ChatRoomMessageEvent)mMessageEvent).isConversationClosed();

        // Create a new chat room detail
        ContactDetail contactDetail = new ContactDetail(chatRoomUid,
                                              ContactDetail.Category.GroupChat);

        mToolTipPeerAddress = subject;
        mDisplayName = subject;
        setData("isClosed", mIsClosed);

        // Initialize the preferred protocols and supported operation sets
        Map<Class<? extends OperationSet>, String> preferredProtocols
                       = new Hashtable<>();

        LinkedList<Class<? extends OperationSet>> supportedOpSets =
                new LinkedList<>();

        // Add IM to the supported operation sets with Jabber as the preferred
        // protocol.
        supportedOpSets.add(OperationSetMultiUserChat.class);
        preferredProtocols.put(OperationSetMultiUserChat.class,
                               ProtocolNames.JABBER);

        contactDetail.setPreferredProtocols(preferredProtocols);
        contactDetail.setSupportedOpSets(supportedOpSets);
        mContactDetails.add(contactDetail);
    }

    /**
     * Initializes the details for the peer contained by this source contact
     *
     * @param peerAddress the address of the peer
     */
    private void initPeerDetails(String peerAddress)
    {
        mIsGroupChat = false;

        // Create a new IM contact detail.
        ContactDetail contactDetail = new ContactDetail(
            peerAddress, ContactDetail.Category.InstantMessaging, null);

        mToolTipPeerAddress = peerAddress;

        LinkedList<Class<? extends OperationSet>> supportedOpSets =
                new LinkedList<>();

        if (mMetaContact != null)
        {
            // We have a MetaContact, so use its display name as the display name
            mDisplayName = mMetaContact.getDisplayName();
        }
        else
        {
            // We don't have a MetaContact so just use the peer address as the
            // display name and add OperationSetPersistentPresence to supported
            // operation sets so that the user can choose to add them as a
            // contact.
            supportedOpSets.add(OperationSetPersistentPresence.class);
            mDisplayName = peerAddress;
        }

        Map<Class<? extends OperationSet>, String> preferredProtocols
                       = new Hashtable<>();

        // Add IM to the supported operation sets with Jabber as the preferred
        // protocol.
        supportedOpSets.add(OperationSetBasicInstantMessaging.class);
        preferredProtocols.put(OperationSetBasicInstantMessaging.class,
                               ProtocolNames.JABBER);

        contactDetail.setPreferredProtocols(preferredProtocols);
        contactDetail.setSupportedOpSets(supportedOpSets);
        mContactDetails.add(contactDetail);
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
     * Returns the MetaContact associated with this SourceContact (may be null).
     *
     * @return MetaContact associated with this SourceContact
     */
    public MetaContact getMetaContact()
    {
        return mMetaContact;
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
        BufferedImageFuture image;
        if (mIsGroupChat)
        {
            if (mIsClosed)
            {
                image = mGroupMessageClosedIcon;
            }
            else
            {
                if (mIsMessageRead)
                {
                    image = mGroupMessageReadIcon;
                }
                else
                {
                    image = mGroupMessageUnreadIcon;
                }
            }
        }
        else
        {
            if (mIsMessageRead)
            {
                image = mReadMessageIcon;
            }
            else
            {
                image = mUnreadMessageIcon;
            }
        }
        return image;
    }

    public String getImageDescription()
    {
        BufferedImageFuture image = getImage();

        return mResources.getI18NString(
                image == mGroupMessageClosedIcon ? "service.gui.accessibility.CLOSED_GROUP" :
                image == mGroupMessageReadIcon ? "service.gui.accessibility.READ_GROUP" :
                image == mGroupMessageUnreadIcon ? "service.gui.accessibility.UNREAD_GROUP" :
                image == mReadMessageIcon ? "service.gui.accessibility.READ_MESSAGE" :
                image == mUnreadMessageIcon ? "service.gui.accessibility.UNREAD_MESSAGE" :
                null);
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
        // We support only message details
        // or persistence presence so we can add contacts.
        if (!(operationSet.equals(OperationSetBasicInstantMessaging.class)
                || operationSet.equals(OperationSetPersistentPresence.class)
                || operationSet.equals(OperationSetMultiUserChat.class)))
            return null;

        List<ContactDetail> details = new LinkedList<>();

        for (ContactDetail contactDetail : getContactDetails())
        {
            List<Class<? extends OperationSet>> supportedOperationSets
                = contactDetail.getSupportedOperationSets();

            if ((supportedOperationSets != null)
                    && supportedOperationSets.contains(operationSet))
                details.add(contactDetail);
        }
        return details;
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
    {
        List<ContactDetail> details = new LinkedList<>();

        for (ContactDetail contactDetail : getContactDetails())
        {
            if(contactDetail != null)
            {
                ContactDetail.Category detailCategory
                    = contactDetail.getCategory();
                if (detailCategory != null && detailCategory.equals(category))
                    details.add(contactDetail);
            }
        }
        return details;
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
        // We support only message details
        // or persistence presence so we can add contacts.
        if (!(operationSet.equals(OperationSetBasicInstantMessaging.class)
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
        return mIsGroupChat ? mToolTipPeerAddress :
            MessageHistoryActivator.getPhoneNumberUtilsService().
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
        // A MessageHistorySourceContact can be messaged if:
        // <li> Group IM is enabled and it is a group IM history entry
        // <li> SMS is enabled and the number of the contact is a valid SMS number.
        // <li> IM is enabled, it is an IM (i.e. not SMS or group IM) history
        // entry and the entry has an IM contact that can still be messaged.
        boolean isValidGroupIm =
            ConfigurationUtils.isMultiUserChatEnabled() && mIsGroupChat;

        boolean isValidSmsNumber = MessageHistoryActivator.
            getPhoneNumberUtilsService().isValidSmsNumber(getEmphasizedNumber());
        boolean isValidSms =
            ConfigurationUtils.isSmsEnabled() && isValidSmsNumber;

        boolean isValidIm = false;
        if (ConfigurationUtils.isImEnabled() &&
            !isValidSmsNumber &&
            !mIsGroupChat &&
            mMetaContact != null)
        {
            isValidIm = mMetaContact.isImCapable();
        }

        return (isValidGroupIm || isValidIm || isValidSms);
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
