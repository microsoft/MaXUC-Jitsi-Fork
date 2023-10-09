/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.msghistory;

import static java.util.stream.Collectors.*;
import static net.java.sip.communicator.util.PrivacyUtils.*;
import static org.jitsi.util.Hasher.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.resources.BufferedImageFuture;
import org.jitsi.service.resources.ResourceManagementService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.analytics.AnalyticsService;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactsource.SourceContact;
import net.java.sip.communicator.service.contactsource.SourceContactChangeListener;
import net.java.sip.communicator.service.database.DatabaseConnection;
import net.java.sip.communicator.service.database.DatabaseService;
import net.java.sip.communicator.service.database.schema.GroupMessageHistoryTable;
import net.java.sip.communicator.service.database.schema.MessageHistoryTable;
import net.java.sip.communicator.service.database.util.DatabaseUtils;
import net.java.sip.communicator.service.gui.ChatRoomWrapper;
import net.java.sip.communicator.service.msghistory.MessageHistoryService;
import net.java.sip.communicator.service.protocol.AbstractMessage;
import net.java.sip.communicator.service.protocol.ChatRoom;
import net.java.sip.communicator.service.protocol.ChatRoomMember;
import net.java.sip.communicator.service.protocol.ChatRoomMemberRole;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.ImMessage;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
import net.java.sip.communicator.service.protocol.OperationSetMultiUserChat;
import net.java.sip.communicator.service.protocol.OperationSetPersistentPresence;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.ChatRoomCreatedEvent;
import net.java.sip.communicator.service.protocol.event.ChatRoomCreatedListener;
import net.java.sip.communicator.service.protocol.event.ChatRoomMemberPresenceChangeEvent;
import net.java.sip.communicator.service.protocol.event.ChatRoomMemberPresenceListener;
import net.java.sip.communicator.service.protocol.event.ChatRoomMessageDeliveredEvent;
import net.java.sip.communicator.service.protocol.event.ChatRoomMessageDeliveryFailedEvent;
import net.java.sip.communicator.service.protocol.event.ChatRoomMessageEvent;
import net.java.sip.communicator.service.protocol.event.ChatRoomMessageListener;
import net.java.sip.communicator.service.protocol.event.ChatRoomMessageReceivedEvent;
import net.java.sip.communicator.service.protocol.event.ChatRoomPropertyChangeEvent;
import net.java.sip.communicator.service.protocol.event.ChatRoomPropertyChangeFailedEvent;
import net.java.sip.communicator.service.protocol.event.ChatRoomPropertyChangeListener;
import net.java.sip.communicator.service.protocol.event.LocalUserChatRoomPresenceChangeEvent;
import net.java.sip.communicator.service.protocol.event.LocalUserChatRoomPresenceListener;
import net.java.sip.communicator.service.protocol.event.MessageDeliveredEvent;
import net.java.sip.communicator.service.protocol.event.MessageDeliveryFailedEvent;
import net.java.sip.communicator.service.protocol.event.MessageEvent;
import net.java.sip.communicator.service.protocol.event.MessageListener;
import net.java.sip.communicator.service.protocol.event.MessageReceivedEvent;
import net.java.sip.communicator.service.protocol.event.OneToOneMessageEvent;
import net.java.sip.communicator.service.wispaservice.WISPAAction;
import net.java.sip.communicator.service.wispaservice.WISPANamespace;
import net.java.sip.communicator.service.wispaservice.WISPAService;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.PrivacyUtils;
import net.java.sip.communicator.util.account.AccountUtils;

/**
 * The Message History Service stores messages exchanged through the various
 * protocols Logs messages for all protocol providers that support basic instant
 * messaging (i.e. those that implement OperationSetBasicInstantMessaging).
 *
 * @author Alexander Pelov
 * @author Damian Minkov
 * @author Lubomir Marinov
 * @author Valentin Martinet
 */
public class MessageHistoryServiceImpl
    implements MessageHistoryService, MessageListener, ChatRoomMessageListener,
    ServiceListener,
    LocalUserChatRoomPresenceListener,
    ChatRoomCreatedListener, ChatRoomMemberPresenceListener, ChatRoomPropertyChangeListener
{
    /**
     * The logger for this class.  We can log PII in this class when it is
     * logging jids used in messages sent or received by the user.  We must
     * still hash contact display names, though this is already done by
     * MetaContact's toString() method.
     */
    private static Logger sLog =
        Logger.getLogger(MessageHistoryServiceImpl.class);

    private static final AnalyticsService sAnalyticsService = MessageHistoryActivator.getAnalyticsService();

    /**
     * The resource management service.
     */
    private static final ResourceManagementService sResources =
                                        MessageHistoryActivator.getResources();

    /**
     * The configuration service.
     */
    private static final ConfigurationService sCfg =
                             MessageHistoryActivator.getConfigurationService();

    private static final WISPAService sWISPA =
                             MessageHistoryActivator.getWISPAService();

    /**
     * If we're running in QA mode, we can log details of what the user types
     * into the search box, otherwise we can't as that may contain PII.
     */
    private static final boolean mQAMode =
            sCfg.global().getBoolean("net.java.sip.communicator.QA_MODE", false);

    /**
     * The string used to identify a default ID in message history records.
     */
    private static final String DEFAULT_ID = "default";

    /**
     * String used for the group chat subject if the chat room returns null
     * for the subject.
     */
    private static final String DEFAULT_GROUP_CHAT_SUBJECT =
        sResources.getI18NString("service.gui.DEFAULT_GROUP_CHAT_SUBJECT");

    /**
     * String used to display as a status message to the user if they were
     * removed from a group chat while they were offline so we cannot find out
     * who removed them.
     */
    private static final String DEFAULT_REMOVED_FROM_GROUP_CHAT_STATUS_STRING =
        sResources.getI18NString("service.gui.CHAT_ROOM_UNKNOWN_REMOVED_YOU");

    /**
     * This is the only content type we support for IM messages.
     */
    private static final String DEFAULT_CONTENT_TYPE = "text/plain";

    /**
     * This is the only content encoding we support for IM messages.
     */
    private static final String DEFAULT_CONTENT_ENCODING = "UTF-8";

    /**
     * A simple enum for the one-to-one message types that this class handles.
     */
    private enum MessageType
    {
        IM_MESSAGE("im"),
        SMS_MESSAGE("sms");

        /**
         * A string describing the type of message.
         */
        private final String mMessageType;

        MessageType(String messageType)
        {
            mMessageType = messageType;
        }

        /**
         * @return a string describing the type of message.
         */
        public String getMessageType()
        {
            return mMessageType;
        }

        /**
         * Returns the value of the MessageEvent event type constant that maps
         * to the given MessageType messageType string.
         *
         * @return the MessageEvent event type
         */
        public static int getMessageEventTypeFromMessageType(String messageType)
        {
            return (MessageType.SMS_MESSAGE.getMessageType().equals(messageType)) ?
                MessageEvent.SMS_MESSAGE :
                MessageEvent.CHAT_MESSAGE;
        }

        /**
         * Returns the value of the MessageType messageType string that maps
         * to the given MessageEvent event type constant.
         *
         * @return the MessageType message type
         */
        public static MessageType getMessageTypeFromMessageEventType(int messageEventType)
        {
            return (MessageEvent.SMS_MESSAGE == messageEventType) ?
                MessageType.SMS_MESSAGE :
                MessageType.IM_MESSAGE;
        }
    }

    /**
     * A simple enum for the group message types that this class handles.
     */
    private enum GroupMessageType
    {
        GROUP_MESSAGE("groupchat"),
        STATUS_MESSAGE("status");

        /**
         * A string describing the type of message.
         */
        private final String mMessageType;

        GroupMessageType(String messageType)
        {
            mMessageType = messageType;
        }

        /**
         * @return a string describing the type of message.
         */
        public String getMessageType()
        {
            return mMessageType;
        }

        /**
         * Returns the value of the MessageEvent event type constant that maps
         * to the given MessageType messageType string.
         *
         * @return the MessageEvent event type
         */
        public static int getMessageEventTypeFromMessageType(String messageType)
        {
            return (GroupMessageType.STATUS_MESSAGE.getMessageType().equals(messageType)) ?
                MessageEvent.STATUS_MESSAGE :
                MessageEvent.GROUP_MESSAGE;
        }
    }

    /**
     * The <tt>MessageHistoryContactSource</tt> reference.
     */
    private final MessageHistoryContactSource mMsgHistoryContactSource;

    /**
     * A list of listeners registered for SourceContactChange events.
     */
    private final List<SourceContactChangeListener> mSourceContactChangeListeners =
            new LinkedList<>();

    /**
     * The BundleContext that we got from the OSGI bus.
     */
    private BundleContext mBundleContext = null;

    private DatabaseService mDatabaseService = MessageHistoryActivator.getDatabaseService();

    /**
     * The IM ProtocolProviderService.
     */
    private ProtocolProviderService mImProvider;

    /**
     * Creates a new instance of the MessageHistoryServiceImpl.
     *
     * @param contactSource The MessageHistoryContactSource associated with
     *                      this instance of the MessageHistoryServiceImpl.
     */
    public MessageHistoryServiceImpl(MessageHistoryContactSource contactSource)
    {
        mMsgHistoryContactSource = contactSource;
    }

    /**
     * Starts the service.
     * Check the current registered protocol providers which
     * supports BasicIM and adds message listener to them
     */
    public void start(BundleContext bundleContext)
    {
        sLog.info("Starting the MessageHistoryService.");
        mBundleContext = bundleContext;
        loadMessageHistoryService();
    }

    /**
     * Loads the History and MessageHistoryService. Registers the service in the
     * bundle context.
     */
    private void loadMessageHistoryService()
    {
        // start listening for newly register or removed protocol providers
        mBundleContext.addServiceListener(this);

        ServiceReference<?>[] protocolProviderRefs = null;
        try
        {
            protocolProviderRefs =
                mBundleContext.getServiceReferences(
                    ProtocolProviderService.class.getName(), null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            sLog.error("Error while retrieving service refs", ex);
            return;
        }

        // in case we found any
        if (protocolProviderRefs != null)
        {
            sLog.debug("Found " + protocolProviderRefs.length
                + " already installed providers.");
            for (int i = 0; i < protocolProviderRefs.length; i++)
            {
                ProtocolProviderService provider =
                    (ProtocolProviderService) mBundleContext
                        .getService(protocolProviderRefs[i]);

                handleProviderAdded(provider);
            }
        }
    }

    /**
     * Stops the MessageHistoryService.
     */
    public void stop(BundleContext bundleContext)
    {
        sLog.info("Stopping the MessageHistoryService.");

        // Start listening for newly register or removed protocol providers.
        mBundleContext.removeServiceListener(this);

        ServiceReference<?>[] protocolProviderRefs = null;
        try
        {
            protocolProviderRefs =
                mBundleContext.getServiceReferences(
                    ProtocolProviderService.class.getName(), null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            sLog.error("Error while retrieving service refs", ex);
            return;
        }

        // in case we found any
        if (protocolProviderRefs != null)
        {
            for (int i = 0; i < protocolProviderRefs.length; i++)
            {
                ProtocolProviderService provider =
                    (ProtocolProviderService) mBundleContext
                        .getService(protocolProviderRefs[i]);

                handleProviderRemoved(provider);
            }
        }
    }

    @Override
    public Collection<MessageEvent> findByEndDate(MetaContact metaContact,
        Date endDate)
    {
        return findByEndDate(metaContact,
            remoteJidsFromMetaContact(metaContact), endDate);
    }

    @Override
    public Collection<MessageEvent> findByEndDate(String smsNumber, Date endDate)
    {
        MetaContact metaContact = smsNumberToMetaContact(smsNumber);
        return findByEndDate(metaContact,
            remoteJidsFromSmsNumber(metaContact, smsNumber), endDate);
    }

    /**
     * Returns all the messages exchanged with the given remoteJids before the
     * given date.
     * @param metaContact The MetaContact.
     * @param remoteJids the jids to check
     * @param endDate the date to check
     * @return the event objects for the messages
     */
    private Collection <MessageEvent> findByEndDate(MetaContact metaContact,
        List<String> remoteJids,
        Date endDate)
    {
        sLog.debug("endDate: " + endDate.getTime() + ", jids: " + sanitiseRemoteJids(remoteJids));
        DatabaseConnection connection = null;
        ResultSet rs = null;
        List<MessageEvent> result = new ArrayList<>();
        try
        {
            connection = mDatabaseService.connect();
            rs = connection.findBeforeDate(MessageHistoryTable.NAME,
                                           MessageHistoryTable.COL_LOCAL_JID,
                                           getImAccountJid(),
                                           MessageHistoryTable.COL_REMOTE_JID,
                                           remoteJids,
                                           MessageHistoryTable.COL_RECEIVED_TIMESTAMP,
                                           MessageHistoryTable.COL_MSG_ID,
                                           endDate);

            while (rs.next())
            {
                MessageEvent evt =
                    convertDatabaseRecordToMessageEvent(metaContact, rs);
                if (evt != null)
                {
                    result.add(evt);
                }
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Message History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, rs);
        }

        sLog.debug("found " + result.size() + " records");
        return result;
    }

    @Override
    public Collection<MessageEvent> findByPeriod(String smsNumber,
        Date startDate, Date endDate)
    {
        MetaContact metaContact = smsNumberToMetaContact(smsNumber);
        return findByPeriod(metaContact,
            remoteJidsFromSmsNumber(metaContact, smsNumber),
            startDate,
            endDate);
    }

    @Override
    public Collection<MessageEvent> findByPeriod(MetaContact metaContact,
        Date startDate, Date endDate)
    {
        return findByPeriod(metaContact,
            remoteJidsFromMetaContact(metaContact), startDate, endDate);
    }

    /**
     * Returns all the messages exchanged with the given remoteJids between the
     * given dates.
     * @param metaContact The MetaContact.
     * @param remoteJids the jids to check
     * @param startDate the start date
     * @param endDate the end date
     * @return the event objects for the messages
     */
    private Collection <MessageEvent> findByPeriod(MetaContact metaContact,
        List<String> remoteJids,
        Date startDate,
        Date endDate)
    {
        sLog.debug("startDate: " + startDate.getTime() + ", endDate: "
                   + endDate.getTime() + ", jids: " + sanitiseRemoteJids(remoteJids));
        DatabaseConnection connection = null;
        ResultSet rs = null;
        List<MessageEvent> result = new ArrayList<>();

        try
        {
            connection = mDatabaseService.connect();
            rs = connection.findByPeriod(MessageHistoryTable.NAME,
                                     MessageHistoryTable.COL_LOCAL_JID,
                                     getImAccountJid(),
                                     MessageHistoryTable.COL_REMOTE_JID,
                                     remoteJids,
                                     MessageHistoryTable.COL_RECEIVED_TIMESTAMP,
                                     MessageHistoryTable.COL_MSG_ID,
                                     startDate,
                                     endDate);

            while (rs.next())
            {
                MessageEvent evt =
                    convertDatabaseRecordToMessageEvent(metaContact, rs);
                if (evt != null)
                {
                    result.add(evt);
                }
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Message History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, rs);
        }

        sLog.debug("found " + result.size() + " records");
        return result;
    }

    @Override
    public MessageEvent findById(String uid)
    {
        MessageEvent result = null;
        DatabaseConnection connection = null;
        ResultSet resultSet = null;

        try
        {
            connection = mDatabaseService.connect();
            String [] searchColumnNames = {MessageHistoryTable.COL_MSG_ID};
            resultSet = connection.findByKeyword(
                MessageHistoryTable.NAME,
                MessageHistoryTable.COL_RECEIVED_TIMESTAMP,
                uid,
                searchColumnNames,
                1);

            if (resultSet.next())
            {
                result = convertDatabaseRecordToMessageEvent(null, resultSet);
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed in findByID: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, resultSet);
        }

        return result;
    }

    @Override
    public List<MessageEvent> findLast(MetaContact metaContact, int count)
    {
        return findLast(metaContact,
            remoteJidsFromMetaContact(metaContact),
            count);
    }

    @Override
    public List<MessageEvent> findLastForThread(String peerId, int count)
    {
        sLog.debug("Finding last " + count + " messages for thread remote JID: " + sanitisePeerId(peerId));

        // We don't know what type of chat the peerId relates to - try looking for
        // one-to-one chats first, then try group.
        List<MessageEvent> oneToOne = findLast(null, Arrays.asList(peerId), count);
        if (oneToOne.size() != 0)
        {
            return oneToOne;
        }
        List<MessageEvent> group = new ArrayList<>(findLast(null, peerId, count));
        return group;
    }

    @Override
    public List<MessageEvent> findLast(String smsNumber, int count)
    {
        MetaContact metaContact = smsNumberToMetaContact(smsNumber);
        return findLast(metaContact,
            remoteJidsFromSmsNumber(metaContact, smsNumber),
            count);
    }

    /**
     * Returns the supplied number of recent messages exchanged with the given
     * remote JIDs.
     * @param metaContact The MetaContact.
     * @param remoteJids the jids to check
     * @param count the number of records to return, or -1 for all records
     * @return a list of Event Objects for the messages
     */
    private List<MessageEvent> findLast(MetaContact metaContact,
        List<String> remoteJids, int count)
    {
        // Check Chat Address format, chat Room format, else hash value
        sLog.debug("count: " + count + ", jids: " + sanitiseRemoteJids(remoteJids));
        DatabaseConnection connection = null;
        ResultSet rs = null;
        List<MessageEvent> result = new ArrayList<>();

        try
        {
            connection = mDatabaseService.connect();
            rs = connection.findLast(MessageHistoryTable.NAME,
                                     MessageHistoryTable.COL_LOCAL_JID,
                                     getImAccountJid(),
                                     MessageHistoryTable.COL_REMOTE_JID,
                                     remoteJids,
                                     MessageHistoryTable.COL_RECEIVED_TIMESTAMP,
                                     MessageHistoryTable.COL_MSG_ID,
                                     count);

            while (rs.next())
            {
                MessageEvent evt =
                    convertDatabaseRecordToMessageEvent(metaContact, rs);
                if (evt != null)
                {
                    result.add(evt);
                }
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Message History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, rs);
        }

        LinkedList<MessageEvent> resultAsList = new LinkedList<>(result);
        int startIndex = resultAsList.size() - count;

        if (startIndex < 0)
            startIndex = 0;

        List<MessageEvent> subList =
            resultAsList.subList(startIndex, resultAsList.size());

        sLog.debug("found " + resultAsList.size() + " records, return " +
            subList.size());

        return subList;
    }

    @Override
    public Collection<MessageEvent> findFirstMessagesAfter(String smsNumber,
        Date date, int count)
    {
        MetaContact metaContact = smsNumberToMetaContact(smsNumber);
        return findFirstMessagesAfter(metaContact,
            remoteJidsFromSmsNumber(metaContact, smsNumber),
            date,
            count);
    }

    @Override
    public Collection<MessageEvent> findFirstMessagesAfter(MetaContact metaContact,
        Date date, int count)
    {
        return findFirstMessagesAfter(metaContact,
            remoteJidsFromMetaContact(metaContact),
            date,
            count);
    }

    /**
     * Returns the supplied number of recent messages after the given date
     * exchanged with all the given remote JIDs.
     *
     * @param metaContact The MetaContact.
     * @param remoteJids the JIDs to check
     * @param date the date after which to check
     * @param count The maximum number of results to return
     * @return the event objects for the messages
     */
    private Collection<MessageEvent> findFirstMessagesAfter(
        MetaContact metaContact,
        List<String> remoteJids,
        Date date,
        int count)
    {
        sLog.debug("count: " + count + ", date: " + date.getTime() +
            ", jids: " + sanitiseRemoteJids(remoteJids));
        DatabaseConnection connection = null;
        ResultSet rs = null;
        List<MessageEvent> result = new ArrayList<>();

        try
        {
            connection = mDatabaseService.connect();
            rs = connection.findFirstRecordsAfter(MessageHistoryTable.NAME,
                                     MessageHistoryTable.COL_LOCAL_JID,
                                     getImAccountJid(),
                                     MessageHistoryTable.COL_REMOTE_JID,
                                     remoteJids,
                                     MessageHistoryTable.COL_RECEIVED_TIMESTAMP,
                                     MessageHistoryTable.COL_MSG_ID,
                                     date,
                                     count);

            while (rs.next())
            {
                MessageEvent evt =
                    convertDatabaseRecordToMessageEvent(metaContact, rs);
                if (evt != null)
                {
                    result.add(evt);
                }
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Message History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, rs);
        }

        sLog.debug("found " + result.size() + " records");
        return result;
    }

    @Override
    public Collection<MessageEvent> findLastMessagesBefore(MetaContact metaContact,
        Date date, int count)
    {
        return findLastMessagesBefore(metaContact,
            remoteJidsFromMetaContact(metaContact),
            date,
            count);
    }

    @Override
    public Collection<MessageEvent> findLastMessagesBefore(String smsNumber,
        Date date, int count)
    {
        MetaContact metaContact = smsNumberToMetaContact(smsNumber);
        return findLastMessagesBefore(metaContact,
            remoteJidsFromSmsNumber(metaContact, smsNumber),
            date,
            count);
    }

    /**
     * Return all the messages exchanged with the given remote JIDs after the
     * given date.
     * @param metaContact The MetaContact.
     * @param remoteJids JIDs to check
     * @param date Date to search before.
     * @return count The maximum number of records to return.
     */
    private Collection<MessageEvent> findLastMessagesBefore(
        MetaContact metaContact,
        List<String> remoteJids,
        Date date,
        int count)
    {
        sLog.debug("date: " + date.getTime() + ", count: " + count +
            ", jids: " + sanitiseRemoteJids(remoteJids));
        DatabaseConnection connection = null;
        ResultSet rs = null;
        List<MessageEvent> result = new ArrayList<>();

        try
        {
            connection = mDatabaseService.connect();
            rs = connection.findLastRecordsBefore(MessageHistoryTable.NAME,
                                     MessageHistoryTable.COL_LOCAL_JID,
                                     getImAccountJid(),
                                     MessageHistoryTable.COL_REMOTE_JID,
                                     remoteJids,
                                     MessageHistoryTable.COL_RECEIVED_TIMESTAMP,
                                     MessageHistoryTable.COL_MSG_ID,
                                     date,
                                     count);

            while (rs.next())
            {
                MessageEvent evt =
                    convertDatabaseRecordToMessageEvent(metaContact, rs);
                if (evt != null)
                {
                    result.add(evt);
                }
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Message History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, rs);
        }

        sLog.debug("found " + result.size() + " records");
        return result;
    }

    /**
     * Gets a list of all the JIDs for the given metacontact, and any
     * sms numbers associated with the contact
     * @param metaContact MetaContact to search
     * @return list of associated JIDs and sms numbers
     */
    private List<String> remoteJidsFromMetaContact(MetaContact metaContact)
    {
        List<String> remoteJids = new ArrayList<>();

        Contact contact = metaContact.getIMContact();

        if (contact != null)
        {
            remoteJids.add(contact.getAddress().toLowerCase());
        }

        // Add any sms numbers to the list
        if (ConfigurationUtils.isSmsEnabled())
        {
            remoteJids.addAll(metaContact.getSmsNumbers());
        }

        // Do not return an empty list as the SQL queries can't cope.  This is
        // an edge case when contacts are deleted.  Instead return a dummy
        // value that can't match in the SQL query.
        if (remoteJids.isEmpty())
        {
            sLog.info("No remote JIDs for: " + metaContact);
            remoteJids.add("NOREMOTEJIDSFORMETACONTACT");
        }

        sLog.debug("Return: " + sanitiseRemoteJids(remoteJids));
        return remoteJids;
    }

    /**
     * Takes a list of all the JIDs for a given metacontact
     * and hashes any contained Personal Data.
     */
    private String sanitiseRemoteJids(List<String> remoteJids)
    {
        return remoteJids.stream()
                .map(PrivacyUtils::sanitisePeerId)
                .collect(joining(", "));
    }

    /**
     * Gets a list of the given sms number, plus all the JIDs for any contacts
     * associated with the sms number
     * @param smsNumber sms number to use
     * @return list of associated JIDs and the sms number
     */
    private List<String> remoteJidsFromSmsNumber(MetaContact metaContact,
        String smsNumber)
    {
        List<String> remoteJids = new ArrayList<>();

        if (ConfigurationUtils.isSmsEnabled())
        {
            remoteJids.add(formatToNationalNumber(smsNumber));
        }

        if (metaContact != null)
        {
            // Get the remote JID for this MetaContact, if any.
            Contact contact = metaContact.getIMContact();

            if (contact != null)
            {
                remoteJids.add(contact.getAddress().toLowerCase());
            }
        }

        // Do not return an empty list as the SQL queries can't cope.  This is
        // an edge case when contacts are deleted.  Instead return a dummy
        // value that can't match in the SQL query.
        if (remoteJids.isEmpty())
        {
            sLog.info("No remote JIDs for: " + metaContact + ", " + logHasher(smsNumber));
            remoteJids.add("NOREMOTEJIDSFORSMSNUMBER");
        }

        sLog.debug("Return: " + sanitiseRemoteJids(remoteJids));
        return remoteJids;
    }

    /**
     * Used to convert a row of a ResultSet to an EventObject which is returned
     * by the finder methods. For one-to-one IM and SMS.
     *
     * @param metaContact The contact the message was to/from.
     * @param rs ResultSet containing the record
     * @return EventObject The event created from the ResultSet, or null.
     * @throws SQLException
     */
    private MessageEvent convertDatabaseRecordToMessageEvent(
        MetaContact metaContact, ResultSet rs)
        throws SQLException
    {
        MessageEvent messageEvent = null;
        MessageImpl msg = null;
        Contact contact = null;

        msg = createMessageFromDatabaseRecord(rs);
        Date timestamp = msg.getMessageReceivedDate();
        int type = MessageType.getMessageEventTypeFromMessageType(msg.type);
        String remoteJid = rs.getString(MessageHistoryTable.COL_REMOTE_JID);
        String smsNumber = null;

        if (type == MessageEvent.SMS_MESSAGE)
        {
            smsNumber = remoteJid;

            if (metaContact == null)
            {
                metaContact = MessageHistoryActivator.getContactListService().
                                        findMetaContactForSmsNumber(smsNumber);
            }

            if (metaContact != null)
            {
                contact = metaContact.getContactForSmsNumber(smsNumber);
            }
        }
        else
        {
            // Not SMS, so must be IM.
            if (metaContact != null)
            {
                contact = metaContact.getIMContact();
            }
            else
            {
                ProtocolProviderService provider = getImProvider();

                if (provider != null)
                {
                    OperationSetPersistentPresence opSetPersPresence =
                        provider.getOperationSet(
                                         OperationSetPersistentPresence.class);
                    contact = opSetPersPresence.findContactByID(remoteJid);
                }
            }
        }

        if ((contact != null) || (smsNumber != null))
        {
            messageEvent = (msg.isOutgoing) ?
                new MessageDeliveredEvent(
                    msg, contact, smsNumber, timestamp, null, type, msg.isFailed) :
                new MessageReceivedEvent(
                    msg, smsNumber, contact, timestamp, msg.isRead, type);
        }
        else
        {
            // This will happen if the contact that this conversation was
            // with has subsequently been deleted.  In that case, we just
            // ignore their history.
            // No need to hash MetaContact, as its toString() method does that.
            sLog.info("No contact found for JID: " + logHasher(remoteJid) +
                ", metaContact: " + metaContact);
        }

        return messageEvent;
    }

    /**
     * Used to convert a row of a ResultSet for a group message to an
     * EventObject which is returned by the finder methods.
     *
     * @param rs ResultSet containing the record
     * @param room the chat room involved in the message.  May be null.
     * @return EventObject The event created from the ResultSet
     */
    private ChatRoomMessageEvent convertDatabaseRecordToGroupMessageEvent(
        ResultSet rs,
        ChatRoom room)
    {
        return convertDatabaseRecordToGroupMessageEvent(
            rs, room.getIdentifier().toString(), room);
    }

    /**
     * Used to convert a row of a ResultSet for a group message to an
     * EventObject which is returned by the finder methods.
     *
     * One or
     *
     * @param rs ResultSet containing the record
     * @param roomJid the room JID of the room.  Must be valid.
     * @param room the chat room involved in the message.  May be null.
     * @return EventObject The event created from the ResultSet
     */
    private ChatRoomMessageEvent convertDatabaseRecordToGroupMessageEvent(
        ResultSet rs,
        String roomJid,
        ChatRoom room)
    {
        MessageImpl msg = createGroupMessageFromDatabaseRecord(rs);
        Date timestamp = msg.getMessageReceivedDate();
        int type = GroupMessageType.getMessageEventTypeFromMessageType(msg.type);
        ChatRoomMember from = new ChatRoomMemberImpl(msg.peerAddress, room, null);
        ChatRoomMessageEvent msgReturn = (msg.isOutgoing) ? new ChatRoomMessageDeliveredEvent(
                    msg,
                    room,
                    roomJid,
                    msg.getSubject(),
                    timestamp,
                    msg.isClosed,
                    type)
                    : new ChatRoomMessageReceivedEvent(
                        msg,
                        room,
                        roomJid,
                        msg.getSubject(),
                        from,
                        timestamp,
                        msg.isRead,
                        msg.isClosed,
                        type);
        // Making an specific change to the event that is returned, so it will display the correct status of the message
        msgReturn.setFailed(msg.isFailed);
        return msgReturn;
    }

    /**
     * Used to convert a row of a ResultSet to a MessageImpl for a one-to-one
     * message.
     *
     * @param rs ResultSet containing the record to convert
     */
    private static MessageImpl createMessageFromDatabaseRecord(ResultSet rs)
    {
        boolean isOutgoing = false;
        String textContent = null;
        String messageUID = null;
        Date messageReceivedDate = new Date(0);
        boolean isRead = true;
        // Default type to IM, as older records won't contain a type but will
        // all be IMs.
        MessageHistoryTable.TYPE type = MessageHistoryTable.TYPE.IM;
        String msgType;
        boolean isFailed = false;
        String smppID = null;

        try
        {
            isOutgoing =
                MessageHistoryTable.DIRECTION.values()[rs.getInt(MessageHistoryTable.COL_DIR)] ==
                MessageHistoryTable.DIRECTION.OUT;
            textContent = rs.getString(MessageHistoryTable.COL_TEXT);
            messageUID = rs.getString(MessageHistoryTable.COL_MSG_ID);
            messageReceivedDate = new Date(rs.getLong(MessageHistoryTable.COL_RECEIVED_TIMESTAMP));
            isRead = rs.getBoolean(MessageHistoryTable.COL_READ);
            type = MessageHistoryTable.TYPE.values()[rs.getInt(MessageHistoryTable.COL_TYPE)];
            isFailed = rs.getBoolean(MessageHistoryTable.COL_FAILED);
            smppID = rs.getString(MessageHistoryTable.COL_SMPP_ID);
        }
        catch (SQLException ex)
        {
            sLog.error("Couldn't parse message history record: ", ex);
        }

        if (type == MessageHistoryTable.TYPE.SMS)
        {
            msgType = MessageType.SMS_MESSAGE.getMessageType();
        }
        else
        {
            msgType = MessageType.IM_MESSAGE.getMessageType();
        }

        return new MessageImpl(textContent, DEFAULT_CONTENT_TYPE,
                               DEFAULT_CONTENT_ENCODING,
                               null, null, messageUID, isOutgoing,
                               messageReceivedDate, isRead, msgType, isFailed,
                               false, smppID);
    }

    /**
     * Used to convert a row of a ResultSet to a MessageImpl for a group message
     *
     * @param rs ResultSet containing the record to convert
     */
    private static MessageImpl createGroupMessageFromDatabaseRecord(ResultSet rs)
    {
        boolean isOutgoing = false;
        String textContent = null;
        String sender = null;
        String subject = null;
        String messageUID = null;
        Date messageReceivedDate = new Date(0);
        boolean isRead = true;
        GroupMessageHistoryTable.TYPE type = GroupMessageHistoryTable.TYPE.GROUP_IM;
        boolean isFailed = false;
        boolean isLeft = false;
        String msgType = null;

        try
        {
            isOutgoing =
                GroupMessageHistoryTable.DIRECTION.values()[rs.getInt(GroupMessageHistoryTable.COL_DIR)] ==
                GroupMessageHistoryTable.DIRECTION.OUT;
            textContent = rs.getString(GroupMessageHistoryTable.COL_TEXT);
            sender = rs.getString(GroupMessageHistoryTable.COL_SENDER_JID);
            subject = rs.getString(GroupMessageHistoryTable.COL_SUBJECT);
            messageUID = rs.getString(GroupMessageHistoryTable.COL_MSG_ID);
            messageReceivedDate = new Date(rs.getLong(GroupMessageHistoryTable.COL_RECEIVED_TIMESTAMP));
            isRead = rs.getBoolean(GroupMessageHistoryTable.COL_READ);
            type = GroupMessageHistoryTable.TYPE.values()[rs.getInt(GroupMessageHistoryTable.COL_TYPE)];
            isFailed = rs.getBoolean(GroupMessageHistoryTable.COL_FAILED);
            isLeft = rs.getBoolean(GroupMessageHistoryTable.COL_LEFT);
        }
        catch (SQLException ex)
        {
            sLog.error("Couldn't parse message history record: ", ex);
        }

        msgType = (type == GroupMessageHistoryTable.TYPE.GROUP_IM) ?
            GroupMessageType.GROUP_MESSAGE.getMessageType() :
            GroupMessageType.STATUS_MESSAGE.getMessageType();

        return new MessageImpl(textContent, DEFAULT_CONTENT_TYPE,
                               DEFAULT_CONTENT_ENCODING,
                               sender, subject, messageUID, isOutgoing,
                               messageReceivedDate, isRead, msgType, isFailed,
                               isLeft, null);
    }

    // //////////////////////////////////////////////////////////////////////////
    // MessageListener implementation methods

    @Override
    public void messageReceived(MessageReceivedEvent evt)
    {
        processMessageEvent(evt, MessageHistoryTable.DIRECTION.IN);
    }

    @Override
    public void messageDelivered(MessageDeliveredEvent evt)
    {
        processMessageEvent(evt, MessageHistoryTable.DIRECTION.OUT);
    }

    @Override
    public void messageDeliveryFailed(MessageDeliveryFailedEvent evt)
    {
        Contact peerContact = evt.getPeerContact();
        String peerId = evt.getPeerIdentifier();
        int eventType = evt.getEventType();

        if (eventType == MessageEvent.SMS_MESSAGE)
        {
            markMessageFailed(peerId, evt.getFailedMessageUID());
        }
        else if (peerContact != null)
        {
            if (eventType == MessageEvent.GROUP_MESSAGE)
            {
                markGroupMessageFailed(peerContact, evt.getFailedMessageUID());
            }
            else
            {
                markMessageFailed(peerContact, evt.getFailedMessageUID());
            }
        }
        else
        {
            sLog.error("Unable to mark message as failed - no contact, "
                + "IM address or SMS number specified.");
        }
    }

    /**
     * Processes a MessageReceivedEvent or a MessageDeliveredEvent then calls a
     * method to write the message to history.
     *
     * @param evt The MessageReceivedEvent or MessageDeliveredEvent
     * @param direction "incoming" or "outgoing"
     */
    private void processMessageEvent(OneToOneMessageEvent evt,
                                     MessageHistoryTable.DIRECTION direction)
    {
        MessageType type =
            MessageType.getMessageTypeFromMessageEventType(evt.getEventType());

        Contact peerContact = evt.getPeerContact();
        String peerId = evt.getPeerIdentifier();

        // Determine the type of message then write it to history.  Also, if
        // this is an outgoing message, call a method to tell the contact
        // source that message history has changed.  This is required to ensure
        // the change is reflected in the 'Recent' tab.  This isn't necessary
        // for incoming messages, as the 'Recent' tab is refreshed when they
        // are marked as unread and read.
        if (type.equals(MessageType.SMS_MESSAGE))
        {
            writeOneToOneMessage(peerId, direction, evt.getSourceMessage(),
                evt.getTimestamp(), evt.isMessageRead(),
                MessageHistoryTable.TYPE.SMS, evt.isFailed());

            if (MessageHistoryTable.DIRECTION.OUT.equals(direction))
            {
                sendMessageHistoryChanged(peerId, false);
            }
        }
        else if (peerContact != null)
        {
            writeOneToOneMessage(peerContact, direction,
                evt.getSourceMessage(), evt.getTimestamp(),
                evt.isMessageRead(),
                MessageHistoryTable.TYPE.IM,
                                 evt.isFailed());

            if (MessageHistoryTable.DIRECTION.OUT.equals(direction))
            {
                MetaContact metaContact =
                    MessageHistoryActivator.getContactListService()
                                     .findMetaContactByContact(peerContact);

                if (metaContact != null)
                {
                    sendMessageHistoryChanged(metaContact, false);
                }
                else
                {
                    sLog.warn("No MetaContact found for " + peerContact +
                              " - unable to send message history changed " +
                              "for IM to address: " + logHasher(peerId));
                }
            }
        }
        else
        {
            sLog.error("Unable to write message - no contact, IM address "
                + "or SMS number specified.");
        }
    }

    // //////////////////////////////////////////////////////////////////////////
    // ChatRoomMessageListener implementation methods

    @Override
    public void messageReceived(ChatRoomMessageReceivedEvent evt)
    {
        // ignore non conversation messages or status
        int eventType = evt.getEventType();
        if (eventType == MessageEvent.GROUP_MESSAGE ||
            eventType == MessageEvent.STATUS_MESSAGE)
        {
            writeGroupMessage(evt.getChatRoom(),
                              evt.getChatRoomMember().getContactAddressAsString(),
                              GroupMessageHistoryTable.DIRECTION.IN,
                              evt.getSourceMessage().getContent(),
                              evt.getSourceMessage().getMessageUID(),
                              evt.getTimestamp(),
                              evt.getSubject(),
                              evt.isMessageRead(),
                              evt.isFailed(),
                              evt.isConversationClosed(),
                              eventType == MessageEvent.GROUP_MESSAGE ?
                                  GroupMessageHistoryTable.TYPE.GROUP_IM :
                                  GroupMessageHistoryTable.TYPE.STATUS);
        }
    }

    @Override
    public void messageDelivered(ChatRoomMessageDeliveredEvent evt)
    {
        int eventType = evt.getEventType();

        ChatRoom chatRoom = evt.getChatRoom();
        writeGroupMessage(chatRoom,
                          getImAccountJid(),
                          GroupMessageHistoryTable.DIRECTION.OUT,
                          evt.getSourceMessage().getContent(),
                          evt.getSourceMessage().getMessageUID(),
                          evt.getTimestamp(),
                          evt.getSubject(),
                          true,
                          evt.isFailed(),
                          evt.isConversationClosed(),
                          eventType == MessageEvent.GROUP_MESSAGE ?
                              GroupMessageHistoryTable.TYPE.GROUP_IM :
                              GroupMessageHistoryTable.TYPE.STATUS);

        // We need to tell the contact source that message history has changed
        // to ensure the change is reflected in the 'Recent' tab. Note, this
        // isn't necessary for incoming messages, as the 'Recent' tab is
        // refreshed when they are marked as unread and read.
        sendChatRoomMessageHistoryChanged(chatRoom, false);
    }

    @Override
    public void messageDeliveryFailed(ChatRoomMessageDeliveryFailedEvent evt)
    {
    }

    /**
     * Writes message to the history database.
     *
     * @param peer The peer contact
     * @param direction Incoming or outgoing
     * @param message Message message to be written
     * @param messageTimestamp Date this is the timestamp when was message
     *            received that came from the protocol provider
     * @param isRead If true, the message has been read
     * @param type IM or SMS.
     */
    private void writeOneToOneMessage(Contact peer,
                                      MessageHistoryTable.DIRECTION direction,
                                      ImMessage message,
                                      Date messageTimestamp,
                                      boolean isRead,
                                      MessageHistoryTable.TYPE type,
                                      boolean isFailed)
    {
        writeOneToOneMessage(peer == null ? DEFAULT_ID : peer.getAddress(),
            direction,
            message,
            messageTimestamp,
            isRead,
            type,
                             isFailed);
    }

    /**
     * Writes message to the history database.
     *
     * @param remoteJid the remote sender/receiver of the message
     * @param direction Incoming or outgoing
     * @param message Message
     * @param messageTimestamp Date this is the timestamp when was message
     *                              received that came from the protocol provider
     * @param isRead If true, the message has been read
     * @param type IM or SMS.
     */
    private void writeOneToOneMessage(String remoteJid,
                                      MessageHistoryTable.DIRECTION direction,
                                      ImMessage message,
                                      Date messageTimestamp,
                                      boolean isRead,
                                      MessageHistoryTable.TYPE type,
                                      boolean isFailed)
    {
        DatabaseConnection connection = null;
        PreparedStatement preparedStatement = null;

        // If this is an SMS, the number should be formatted according to the
        // Locale.
        String formattedRemoteJid = (type == MessageHistoryTable.TYPE.SMS) ?
            formatToNationalNumber(remoteJid) :
            remoteJid.toLowerCase();

        try
        {
            connection = mDatabaseService.connect();

            preparedStatement = connection.prepare("INSERT INTO " +
                MessageHistoryTable.NAME + "(" +
                MessageHistoryTable.COL_LOCAL_JID + "," +
                MessageHistoryTable.COL_REMOTE_JID + "," +
                MessageHistoryTable.COL_DIR + "," +
                MessageHistoryTable.COL_TEXT + "," +
                MessageHistoryTable.COL_MSG_ID + "," +
                MessageHistoryTable.COL_RECEIVED_TIMESTAMP + "," +
                MessageHistoryTable.COL_TYPE + "," +
                MessageHistoryTable.COL_READ + "," +
                MessageHistoryTable.COL_FAILED +
                ") VALUES (?,?,?,?,?,?,?,?,?)");

            preparedStatement.setString(1, getImAccountJid());
            preparedStatement.setString(2, formattedRemoteJid);
            preparedStatement.setInt(3, direction.ordinal());
            preparedStatement.setString(4, message.getContent());
            preparedStatement.setString(5, message.getMessageUID());
            preparedStatement.setLong(6, messageTimestamp.getTime());
            preparedStatement.setInt(7, type.ordinal());
            preparedStatement.setBoolean(8, isRead);
            preparedStatement.setBoolean(9, isFailed);

            // We can't log the actual statement as it contains the full text
            // of the message. Instead, log a copy of the statement with the
            // text removed.
            connection.log(DatabaseUtils.createInsertMessageLogString(
                getImAccountJid(),
                remoteJid,
                direction.ordinal(),
                message.getMessageUID(),
                messageTimestamp.getTime(),
                type.ordinal(),
                isRead));

            connection.executeNoLog(preparedStatement);
        }
        catch (SQLException e)
        {
            sLog.error("Could not add message to history: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection);
        }
    }

    /**
     * Writes a chatroom message to the history.
     *
     * @param chatRoom Chatroom object.
     * @param senderJid Sender JID.
     * @param direction the direction of the message.
     * @param messageText
     * @param messageUid
     * @param messageTimestamp Date this is the timestamp when was message
     *            received that came from the protocol provider
     * @param subject the subject of the chat room (used as the chat room
     *                display name)
     * @param isRead If true, the message has been read
     * @param isFailed If true, the message has failed to been sent
     * @param isClosed If true, the conversation is closed
     * @param type 'groupchat' or 'status'
     */
    private void writeGroupMessage(ChatRoom chatRoom,
        String senderJid,
        GroupMessageHistoryTable.DIRECTION direction,
        String messageText,
        String messageUid,
        Date messageTimestamp,
        String subject,
        boolean isRead,
        boolean isFailed,
        boolean isClosed,
        GroupMessageHistoryTable.TYPE type)
    {
        // Missing from, strange messages, most probably a history
        // coming from server and probably already written
        if (senderJid == null)
        {
            sLog.warn("Received a chatroom message with no sender");
            return;
        }

        String roomJid = chatRoom.getIdentifier().toString();

        if (subject == null)
            subject = DEFAULT_GROUP_CHAT_SUBJECT;

        DatabaseConnection connection = null;
        PreparedStatement preparedStatement = null;

        try
        {
            connection = mDatabaseService.connect();

            preparedStatement = connection.prepare("INSERT INTO " +
                GroupMessageHistoryTable.NAME + " (" +
                GroupMessageHistoryTable.COL_LOCAL_JID + "," +
                GroupMessageHistoryTable.COL_SENDER_JID + "," +
                GroupMessageHistoryTable.COL_ROOM_JID + "," +
                GroupMessageHistoryTable.COL_DIR + "," +
                GroupMessageHistoryTable.COL_TEXT + "," +
                GroupMessageHistoryTable.COL_MSG_ID + "," +
                GroupMessageHistoryTable.COL_RECEIVED_TIMESTAMP + "," +
                GroupMessageHistoryTable.COL_TYPE + "," +
                GroupMessageHistoryTable.COL_READ + "," +
                GroupMessageHistoryTable.COL_FAILED + "," +
                GroupMessageHistoryTable.COL_LEFT + "," +
                GroupMessageHistoryTable.COL_SUBJECT +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");

            preparedStatement.setString(1, getImAccountJid());
            preparedStatement.setString(2, senderJid.toLowerCase());
            preparedStatement.setString(3, roomJid.toLowerCase());
            preparedStatement.setInt(4, direction.ordinal());
            preparedStatement.setString(5, messageText);
            preparedStatement.setString(6, messageUid);
            preparedStatement.setLong(7, messageTimestamp.getTime());
            preparedStatement.setInt(8, type.ordinal());
            preparedStatement.setBoolean(9, isRead);
            preparedStatement.setBoolean(10, isFailed);
            preparedStatement.setBoolean(11, isClosed);
            preparedStatement.setString(12, subject);

            // We can't log the actual statement as it contains the full text
            // of the message. Instead, log a copy of the statement with the
            // text removed.
            connection.log(DatabaseUtils.createInsertGroupMessageLogString(
                getImAccountJid(),
                senderJid,
                roomJid,
                direction.ordinal(),
                messageUid,
                messageTimestamp.getTime(),
                type.ordinal(),
                isRead,
                isClosed,
                subject));

            connection.executeNoLog(preparedStatement);
        }
        catch (SQLException e)
        {
            sLog.error("Could not add message to history: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection);
        }
    }

    @Override
    public void serviceChanged(ServiceEvent serviceEvent)
    {
        Object sService =
            mBundleContext.getService(serviceEvent.getServiceReference());

        sLog.trace("Received a service event for: "
                + sService.getClass().getName());

        // we don't care if the source service is not a protocol provider
        if (!(sService instanceof ProtocolProviderService))
        {
            return;
        }

        sLog.debug("Service is a protocol provider.");
        if (serviceEvent.getType() == ServiceEvent.REGISTERED)
        {
            sLog.debug("Handling registration of a new Protocol Provider.");

            handleProviderAdded((ProtocolProviderService) sService);
        }
        else if (serviceEvent.getType() == ServiceEvent.UNREGISTERING)
        {
            handleProviderRemoved((ProtocolProviderService) sService);
        }
    }

    /**
     * Used to attach the Message History Service to existing or just registered
     * protocol provider. Checks if the provider has implementation of
     * OperationSetBasicInstantMessaging
     *
     * @param provider ProtocolProviderService
     */
    private void handleProviderAdded(ProtocolProviderService provider)
    {
        sLog.debug("Adding protocol provider "
            + provider.getProtocolDisplayName());

        // check whether the provider has a basic im operation set
        OperationSetBasicInstantMessaging opSetIm =
            provider.getOperationSet(OperationSetBasicInstantMessaging.class);

        if (opSetIm != null)
        {
            opSetIm.addMessageListener(this);
        }
        else
        {
            sLog.trace("Service did not have a im op. set.");
        }

        OperationSetMultiUserChat opSetMultiUChat =
            provider.getOperationSet(OperationSetMultiUserChat.class);

        if (opSetMultiUChat != null)
        {
            // Make sure we add the local user presence change listener and
            // chat room created listener before we iterate through the
            // currently joined chat rooms to make sure we don't miss us
            // joining any chat rooms.
            opSetMultiUChat.addPresenceListener(this);
            opSetMultiUChat.addChatRoomCreatedListener(this);

            Iterator<ChatRoom> iter =
                opSetMultiUChat.getCurrentlyJoinedChatRooms().iterator();

            while (iter.hasNext())
            {
                ChatRoom room = iter.next();
                room.addMessageListener(this);
            }
        }
        else
        {
            sLog.trace("Service did not have a multi im op. set.");
        }
    }

    /**
     * Removes the specified provider from the list of currently known providers
     * and ignores all the messages exchanged by it
     *
     * @param provider the ProtocolProviderService that has been unregistered.
     */
    private void handleProviderRemoved(ProtocolProviderService provider)
    {
        OperationSetBasicInstantMessaging opSetIm =
            provider.getOperationSet(OperationSetBasicInstantMessaging.class);

        if (opSetIm != null)
        {
            opSetIm.removeMessageListener(this);
        }

        OperationSetMultiUserChat opSetMultiUChat =
            provider.getOperationSet(OperationSetMultiUserChat.class);

        if (opSetMultiUChat != null)
        {
            Iterator<ChatRoom> iter =
                opSetMultiUChat.getCurrentlyJoinedChatRooms().iterator();

            while (iter.hasNext())
            {
                ChatRoom room = iter.next();
                room.removeMessageListener(this);
            }
        }
    }

    @Override
    public void localUserPresenceChanged(
        LocalUserChatRoomPresenceChangeEvent evt)
    {
        if (evt.isLeavingEvent())
        {
            evt.getChatRoom().removeMessageListener(this);
        }
        else
        {
            evt.getChatRoom().addMessageListener(this);
        }
    }

    @Override
    public Collection<MessageEvent> findByKeyword(MetaContact metaContact,
        String keyword)
    {
        return findByKeyword(metaContact,
            remoteJidsFromMetaContact(metaContact),
            keyword);
    }

    @Override
    public Collection<MessageEvent> findByKeyword(String smsNumber,
        String keyword)
    {
        MetaContact metaContact = smsNumberToMetaContact(smsNumber);
        return findByKeyword(metaContact,
            remoteJidsFromSmsNumber(metaContact, smsNumber),
            keyword);
    }

    /**
     * Return all the messages exchanged with the given remote JIDs and
     * matching a keyword search.
     * @param metaContact The MetaContact.
     * @param remoteJids JIDs to check
     * @param keyword Keyword to search on
     * @return the event objects for the messages
     */
    private Collection<MessageEvent> findByKeyword(MetaContact metaContact,
        List<String> remoteJids,
        String keyword)
    {
        String loggableKeyword = mQAMode ? keyword : REDACTED;
        sLog.debug("keyword: " + loggableKeyword + ", jids: " + remoteJids);
        DatabaseConnection connection = null;
        ResultSet rs = null;
        List<MessageEvent> result = new ArrayList<>();

        try
        {
            connection = mDatabaseService.connect();
            rs = connection.findByKeyword(MessageHistoryTable.NAME,
                                     MessageHistoryTable.COL_LOCAL_JID,
                                     getImAccountJid(),
                                     MessageHistoryTable.COL_REMOTE_JID,
                                     remoteJids,
                                     MessageHistoryTable.COL_RECEIVED_TIMESTAMP,
                                     MessageHistoryTable.COL_MSG_ID,
                                     MessageHistoryTable.COL_TEXT,
                                     keyword);

            while (rs.next())
            {
                MessageEvent evt =
                    convertDatabaseRecordToMessageEvent(metaContact, rs);
                if (evt != null)
                {
                    result.add(evt);
                }
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Message History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, rs);
        }

        sLog.debug("found " + result.size() + " records");
        return result;
    }

    @Override
    public Collection<MessageEvent> findByEndDate(ChatRoomWrapper chatRoomWrapper,
                                                  Date endDate)
    {
        ChatRoom chatRoom = chatRoomWrapper.getChatRoom();
        String chatRoomID = chatRoomWrapper.getChatRoomID();
        sLog.debug("endDate: " + endDate.getTime() + ", room: " + sanitiseChatRoom(chatRoomID));
        DatabaseConnection connection = null;
        ResultSet rs = null;
        List<MessageEvent> result = new ArrayList<>();

        try
        {
            connection = mDatabaseService.connect();
            rs = connection.findBeforeDate(GroupMessageHistoryTable.NAME,
                                GroupMessageHistoryTable.COL_LOCAL_JID,
                                getImAccountJid(),
                                GroupMessageHistoryTable.COL_ROOM_JID,
                                chatRoomID,
                                GroupMessageHistoryTable.COL_RECEIVED_TIMESTAMP,
                                GroupMessageHistoryTable.COL_MSG_ID,
                                endDate);

            while (rs.next())
            {
                result.add(convertDatabaseRecordToGroupMessageEvent(rs,
                                                                    chatRoomID,
                                                                    chatRoom));
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Message History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, rs);
        }

        sLog.debug("found " + result.size() + " records");
        return result;
    }

    @Override
    public Collection<MessageEvent> findByPeriod(ChatRoomWrapper chatRoomWrapper,
                                                 Date startDate,
                                                 Date endDate)
    {
        String chatRoomId = chatRoomWrapper.getChatRoomID();
        sLog.debug("startDate: " + startDate.getTime() + ", endDate: " +
            endDate.getTime() + ", room: " + sanitiseChatRoom(chatRoomId));
        DatabaseConnection connection = null;
        ResultSet rs = null;
        List<MessageEvent> result = new ArrayList<>();

        try
        {
            connection = mDatabaseService.connect();
            rs = connection.findByPeriod(GroupMessageHistoryTable.NAME,
                                GroupMessageHistoryTable.COL_LOCAL_JID,
                                getImAccountJid(),
                                GroupMessageHistoryTable.COL_ROOM_JID,
                                chatRoomId,
                                GroupMessageHistoryTable.COL_RECEIVED_TIMESTAMP,
                                GroupMessageHistoryTable.COL_MSG_ID,
                                startDate,
                                endDate);

            while (rs.next())
            {
                result.add(convertDatabaseRecordToGroupMessageEvent(
                               rs, chatRoomId, chatRoomWrapper.getChatRoom()));
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Message History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, rs);
        }

        sLog.debug("found " + result.size() + " records");
        return result;
    }

    @Override
    public ChatRoomMessageEvent findOldestStatus(ChatRoom room)
    {
        sLog.debug("room: " + sanitiseChatRoom(room.getIdentifier()));
        DatabaseConnection connection = null;
        ResultSet rs = null;
        ChatRoomMessageEvent result = null;

        try
        {
            connection = mDatabaseService.connect();

            rs = connection.findFirstByIntValue(GroupMessageHistoryTable.NAME,
                                GroupMessageHistoryTable.COL_LOCAL_JID,
                                getImAccountJid(),
                                GroupMessageHistoryTable.COL_ROOM_JID,
                                room.getIdentifier().toString(),
                                GroupMessageHistoryTable.COL_RECEIVED_TIMESTAMP,
                                GroupMessageHistoryTable.COL_MSG_ID,
                                GroupMessageHistoryTable.COL_TYPE,
                                GroupMessageHistoryTable.TYPE.STATUS.ordinal());

            // Take the first returned record
            if (rs.next())
            {
                result = convertDatabaseRecordToGroupMessageEvent(rs, room);
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Message History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, rs);
        }

        return result;
    }

    @Override
    public ChatRoomMessageEvent findLatestChatMessage(ChatRoom room)
    {
        sLog.debug("room: " + sanitiseChatRoom(room.getIdentifier()));
        DatabaseConnection connection = null;
        ResultSet rs = null;
        ChatRoomMessageEvent result = null;

        try
        {
            connection = mDatabaseService.connect();

            rs = connection.findLastByIntValue(GroupMessageHistoryTable.NAME,
                                GroupMessageHistoryTable.COL_LOCAL_JID,
                                getImAccountJid(),
                                GroupMessageHistoryTable.COL_ROOM_JID,
                                room.getIdentifier().toString(),
                                GroupMessageHistoryTable.COL_RECEIVED_TIMESTAMP,
                                GroupMessageHistoryTable.COL_MSG_ID,
                                GroupMessageHistoryTable.COL_TYPE,
                                GroupMessageHistoryTable.TYPE.GROUP_IM.ordinal());

            // Take the first returned record
            if (rs.next())
            {
                result = convertDatabaseRecordToGroupMessageEvent(rs, room);
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Message History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, rs);
        }

        return result;
    }

    @Override
    public Collection<MessageEvent> findLast(ChatRoomWrapper chatRoomWrapper,
                                             int count)
    {
        return findLast(chatRoomWrapper.getChatRoom(),
                        chatRoomWrapper.getChatRoomID(),
                        count);
    }

    @Override
    public Collection<MessageEvent> findLast(ChatRoom room, int count)
    {
        return findLast(room, room.getIdentifier().toString(), count);
    }

    private Collection<MessageEvent> findLast(ChatRoom room,
                                              String chatRoomId,
                                              int count)
    {
        if (CHATROOM_PATTERN.matcher(chatRoomId).find())
        {
            sLog.debug("count: " + count + ", room: " + sanitiseChatRoom(chatRoomId));
        }
        else
        {
            sLog.debug("count: " + count + ", room: <redacted as doesn't match chat room pattern>");
        }
        DatabaseConnection connection = null;
        ResultSet rs = null;
        List<MessageEvent> result = new ArrayList<>();

        try
        {
            connection = mDatabaseService.connect();
            rs = connection.findLast(GroupMessageHistoryTable.NAME,
                                GroupMessageHistoryTable.COL_LOCAL_JID,
                                getImAccountJid(),
                                GroupMessageHistoryTable.COL_ROOM_JID,
                                chatRoomId,
                                GroupMessageHistoryTable.COL_RECEIVED_TIMESTAMP,
                                GroupMessageHistoryTable.COL_MSG_ID,
                                count);

            while (rs.next())
            {
                result.add(convertDatabaseRecordToGroupMessageEvent(rs, chatRoomId, room));
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Message History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, rs);
        }

        sLog.debug("found " + result.size() + " records");
        return result;
    }

    @Override
    public Collection<MessageEvent> findByKeyword(ChatRoom chatRoom,
                                                   String keyword)
    {
        String chatRoomId = chatRoom.getIdentifier().toString();

        String loggableKeyword = mQAMode ? keyword : REDACTED;
        sLog.debug("findByKeyword: " + loggableKeyword + ", room: " + sanitiseChatRoom(chatRoomId));
        DatabaseConnection connection = null;
        ResultSet rs = null;
        List<MessageEvent> result = new ArrayList<>();

        try
        {
            connection = mDatabaseService.connect();
            rs = connection.findByKeyword(GroupMessageHistoryTable.NAME,
                                GroupMessageHistoryTable.COL_LOCAL_JID,
                                getImAccountJid(),
                                GroupMessageHistoryTable.COL_ROOM_JID,
                                Arrays.asList(chatRoomId),
                                GroupMessageHistoryTable.COL_RECEIVED_TIMESTAMP,
                                GroupMessageHistoryTable.COL_MSG_ID,
                                GroupMessageHistoryTable.COL_TEXT,
                                keyword);

            while (rs.next())
            {
                result.add(convertDatabaseRecordToGroupMessageEvent(
                                rs, chatRoomId, chatRoom));
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Message History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, rs);
        }

        sLog.debug("found " + result.size() + " records");
        return result;
    }

    @Override
    public Collection<MessageEvent> findByKeyword(ChatRoomWrapper chatRoomWrapper,
                                                  String keyword)
    {
        return findByKeyword(chatRoomWrapper.getChatRoom(), keyword);
    }

    @Override
    public Collection<MessageEvent> findFirstMessagesAfter(ChatRoom room,
        Date date, int count)
    {
        sLog.debug("date: " + date.getTime() + ", count: " + count +
            ", room: " + sanitiseChatRoom(room.getIdentifier()));
        DatabaseConnection connection = null;
        ResultSet rs = null;
        List<MessageEvent> result = new ArrayList<>();

        try
        {
            connection = mDatabaseService.connect();
            rs = connection.findFirstRecordsAfter(GroupMessageHistoryTable.NAME,
                                GroupMessageHistoryTable.COL_LOCAL_JID,
                                getImAccountJid(),
                                GroupMessageHistoryTable.COL_ROOM_JID,
                                room.getIdentifier().toString(),
                                GroupMessageHistoryTable.COL_RECEIVED_TIMESTAMP,
                                GroupMessageHistoryTable.COL_MSG_ID,
                                date,
                                count);

            while (rs.next())
            {
                result.add(convertDatabaseRecordToGroupMessageEvent(rs, room));
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Message History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, rs);
        }

        sLog.debug("found " + result.size() + " records");
        return result;
    }

    @Override
    public Collection<MessageEvent> findLastMessagesBefore(ChatRoom room,
        Date date, int count)
    {
        sLog.debug("date: " + date.getTime() + ", count: " + count +
                   ", room: " + sanitiseChatRoom(room.getIdentifier().toString()));
        DatabaseConnection connection = null;
        ResultSet rs = null;
        List<MessageEvent> result = new ArrayList<>();

        try
        {
            connection = mDatabaseService.connect();
            rs = connection.findLastRecordsBefore(GroupMessageHistoryTable.NAME,
                                GroupMessageHistoryTable.COL_LOCAL_JID,
                                getImAccountJid(),
                                GroupMessageHistoryTable.COL_ROOM_JID,
                                Arrays.asList(room.getIdentifier().toString()),
                                GroupMessageHistoryTable.COL_RECEIVED_TIMESTAMP,
                                GroupMessageHistoryTable.COL_MSG_ID,
                                date,
                                count);

            while (rs.next())
            {
                result.add(convertDatabaseRecordToGroupMessageEvent(rs, room));
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Message History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, rs);
        }

        sLog.debug("found " + result.size() + " records");
        return result;
    }

    @Override
    public Collection<String> findMatchingGroupChatMsgUids(String roomJid, List<String> msgUids)
    {
        List<String> matches = new ArrayList<>();

        sLog.debug("room: " + roomJid + " msgUids: " + msgUids);
        DatabaseConnection connection = null;
        ResultSet rs = null;

        try
        {
            connection = mDatabaseService.connect();
            rs = connection.findMatchingGroupChatMsgUids(GroupMessageHistoryTable.NAME,
                                                         GroupMessageHistoryTable.COL_ROOM_JID,
                                                         roomJid,
                                                         GroupMessageHistoryTable.COL_MSG_ID,
                                                         msgUids);

            while (rs.next())
            {
                matches.add(rs.getString(GroupMessageHistoryTable.COL_MSG_ID));
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Message History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, rs);
        }

        sLog.debug("found " + matches.size() + " matches");
        return matches;
    }

    @Override
    public Set<String> getRoomParticipants(String roomJid)
    {
        Set<String> participants = new HashSet<>();
        DatabaseConnection connection = null;
        ResultSet rs = null;

        try
        {
            connection = mDatabaseService.connect();

            rs = connection.findLast(GroupMessageHistoryTable.NAME,
                                GroupMessageHistoryTable.COL_LOCAL_JID,
                                getImAccountJid(),
                                GroupMessageHistoryTable.COL_ROOM_JID,
                                roomJid,
                                GroupMessageHistoryTable.COL_RECEIVED_TIMESTAMP,
                                GroupMessageHistoryTable.COL_MSG_ID,
                                -1);
            while (rs.next())
            {
                String participant = rs.getString(GroupMessageHistoryTable.COL_SENDER_JID);
                if ((participant != null) &&
                    (participant.length() != 0) &&
                    (!participant.startsWith(OperationSetMultiUserChat.CHATROOM_ID_PREFIX)))
                {
                    participants.add(participant);
                }
            }
        }
        catch (SQLException e)
        {
            sLog.error("Could not read message history", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, rs);
        }

        sLog.debug("found " + participants.size() + " participants");
        return participants;
    }

    @Override
    public MessageEvent findByXmppId(String remoteJid, String xmppId)
    {
        // We don't know if we're looking for a group message or a one-to-one
        // IM/SMS, so check both tables until we find a result.
        MessageEvent msgEvt = findById(
            remoteJid, xmppId, MessageHistoryTable.COL_MSG_ID);

        if (msgEvt == null)
        {
            msgEvt = findByIdRoom(
                remoteJid, xmppId, GroupMessageHistoryTable.COL_MSG_ID);
        }

        return msgEvt;
     }

    @Override
    public MessageEvent findBySmppId(String remoteJid, String smppId)
    {
        return findById(remoteJid, smppId, MessageHistoryTable.COL_SMPP_ID);
    }

    /**
     * Returns the message to/from the given peerId that has the given
     * messageId.
     *
     * @param remoteJid The remote JID.
     * @param messageId the message ID (either XMPP ID or SMPP ID)
     * @param idColumn the index of the column to search (either XMPP ID or
     * SMPP ID)
     *
     * @return the MessageEvent representing the matching message
     */
    private MessageEvent findById(String remoteJid,
        String messageId, String idColumn)
    {
        sLog.debug("Finding message with ID " + messageId +
                     " in column " + idColumn + " for " +
                   sanitiseChatAddress(remoteJid));
        DatabaseConnection connection = null;
        ResultSet rs = null;
        MessageEvent result = null;

        try
        {
            connection = mDatabaseService.connect();
            rs = connection.findByStringValue(MessageHistoryTable.NAME,
                                        MessageHistoryTable.COL_LOCAL_JID,
                                        getImAccountJid(),
                                        MessageHistoryTable.COL_REMOTE_JID,
                                        formatToNationalNumber(remoteJid),
                                        idColumn,
                                        messageId);
            if (rs.next())
            {
                result = convertDatabaseRecordToMessageEvent(null, rs);

                if (rs.next())
                {
                    // The query shouldn't return more than one record
                    sLog.warn("Multiple messages found with id " + messageId);
                }
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Message History", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, rs);
        }

        return result;
     }

    /**
     * Returns the message to/from the given chatroom that has the given
     * messageId.
     *
     * @param roomJid The chatroom JID.
     * @param messageId the message ID (either XMPP ID or SMPP ID)
     * @param idColumn the index of the column to search (either XMPP ID or
     * SMPP ID)
     *
     * @return the MessageEvent representing the matching message
     */
    private MessageEvent findByIdRoom(
        String roomJid, String messageId, String idColumn)
    {
        sLog.debug("Finding group message with ID " + messageId +
                     " in column " + idColumn + " for " +
                   sanitiseChatAddress(roomJid));
        DatabaseConnection connection = null;
        ResultSet rs = null;
        MessageEvent result = null;

        try
        {
            connection = mDatabaseService.connect();
            rs = connection.findByStringValue(GroupMessageHistoryTable.NAME,
                                        GroupMessageHistoryTable.COL_LOCAL_JID,
                                        getImAccountJid(),
                                        GroupMessageHistoryTable.COL_ROOM_JID,
                                        roomJid,
                                        idColumn,
                                        messageId);
            if (rs.next())
            {
                result = convertDatabaseRecordToGroupMessageEvent(
                    rs, roomJid, null);

                if (rs.next())
                {
                    // The query shouldn't return more than one record
                    sLog.warn("Multiple messages found with id " + messageId);
                }
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Message History", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, rs);
        }

        return result;
     }

    @Override
    public boolean updateSmppId(String remoteJid, String xmppId, String smppId)
    {
        sLog.debug("Setting message with XMPP ID " + xmppId +
                     " to have SMPP ID " + smppId + " for " + sanitisePeerId(remoteJid));

        boolean updated = false;

        DatabaseConnection connection = null;
        PreparedStatement preparedStatement = null;

        try
        {
            connection = mDatabaseService.connect();

            preparedStatement = connection.prepare("UPDATE " +
                MessageHistoryTable.NAME + " SET " +
                MessageHistoryTable.COL_SMPP_ID + "=? WHERE " +
                MessageHistoryTable.COL_LOCAL_JID + "=? AND " +
                MessageHistoryTable.COL_REMOTE_JID + "=? AND " +
                MessageHistoryTable.COL_MSG_ID + "=?");

            preparedStatement.setString(1, smppId);
            preparedStatement.setString(2, getImAccountJid());
            preparedStatement.setString(3, formatToNationalNumber(remoteJid));
            preparedStatement.setString(4, xmppId);

            connection.execute(preparedStatement);
            updated = true;
        }
        catch (SQLException e)
        {
            sLog.error("Could not update smpp ID", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection);
        }
        return updated;
    }

    /**
     * Marks the message with the given XMPP ID for the given peerId
     *
     * @param remoteJid The address that the failed message was sent to.
     * @param xmppId the XMPP ID (not SMPP ID) of the failed message
     */
    private void markMessageFailed(String remoteJid, String xmppId)
    {
        sLog.debug("Marking message " + xmppId + " as failed for " + sanitisePeerId(remoteJid));

        DatabaseConnection connection = null;
        PreparedStatement preparedStatement = null;

        try
        {
            connection = mDatabaseService.connect();

            preparedStatement = connection.prepare("UPDATE " +
                MessageHistoryTable.NAME + " SET " +
                MessageHistoryTable.COL_FAILED + "=? WHERE " +
                MessageHistoryTable.COL_LOCAL_JID + "=? AND " +
                MessageHistoryTable.COL_REMOTE_JID + "=? AND " +
                MessageHistoryTable.COL_MSG_ID + "=?");

            preparedStatement.setBoolean(1, true);
            preparedStatement.setString(2, getImAccountJid());
            preparedStatement.setString(3, formatToNationalNumber(remoteJid));
            preparedStatement.setString(4, xmppId);

            connection.execute(preparedStatement);
        }
        catch (SQLException e)
        {
            sLog.error("Could not read/write message history", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection);
        }
    }

    /**
     * Marks the message with the given XMPP ID for the given contact
     *
     * @param contact   the contact that the failed message was sent to
     * @param xmppId the XMPP ID (not SMPP ID) of the failed message
     */
    private void markMessageFailed(Contact contact, String xmppId)
    {
        sLog.debug("Marking message " + xmppId + " as failed for " + contact);

        DatabaseConnection connection = null;
        PreparedStatement preparedStatement = null;

        try
        {
            connection = mDatabaseService.connect();

            preparedStatement = connection.prepare("UPDATE " +
                MessageHistoryTable.NAME + " SET " +
                MessageHistoryTable.COL_FAILED + "=? WHERE " +
                MessageHistoryTable.COL_LOCAL_JID + "=? AND " +
                MessageHistoryTable.COL_REMOTE_JID + "=? AND " +
                MessageHistoryTable.COL_MSG_ID + "=?");

            preparedStatement.setBoolean(1, true);
            preparedStatement.setString(2, getImAccountJid());
            preparedStatement.setString(3, contact.getAddress());
            preparedStatement.setString(4, xmppId);

            connection.execute(preparedStatement);
        }
        catch (SQLException e)
        {
            sLog.error("Could not read/write message history", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection);
        }
    }

    /**
     * Marks the message with the given XMPP ID for the given contact
     *
     * @param contact   the contact that the failed message was sent to
     * @param xmppId the XMPP ID (not SMPP ID) of the failed message
     */
    private void markGroupMessageFailed(Contact contact, String xmppId)
    {
        sLog.debug("Marking message " + xmppId + " as failed for " + contact);

        DatabaseConnection connection = null;
        PreparedStatement preparedStatement = null;

        try
        {
            connection = mDatabaseService.connect();

            preparedStatement = connection.prepare("UPDATE " +
                GroupMessageHistoryTable.NAME + " SET " +
                GroupMessageHistoryTable.COL_FAILED + "=? WHERE " +
                GroupMessageHistoryTable.COL_LOCAL_JID + "=? AND " +
                GroupMessageHistoryTable.COL_ROOM_JID + "=? AND " +
                GroupMessageHistoryTable.COL_MSG_ID + "=?");

            preparedStatement.setBoolean(1, true);
            preparedStatement.setString(2, getImAccountJid());
            preparedStatement.setString(3, contact.getAddress());
            preparedStatement.setString(4, xmppId);

            connection.execute(preparedStatement);
        }
        catch (SQLException e)
        {
            sLog.error("Could not read/write message history", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection);
        }
    }

    @Override
    public Collection<MessageEvent> findLastForAll(String queryString, int count)
    {
        String loggableQueryString = mQAMode ? queryString : REDACTED;
        sLog.debug("Max " + count + ", query: " + loggableQueryString);
        String accountJid = getImAccountJid();
        DatabaseConnection conn = null;
        ResultSet rs = null;

        // Use a TreeSet rather than a List, so that we can combine the IM/SMS
        // and group message histories in date order.
        TreeSet<MessageEvent> results =
                new TreeSet<>(new MessageHistoryComparator<>(true));

        // Get the last one-to-one message for each conversation
        try
        {
            conn = mDatabaseService.connect();
            rs = conn.findLast(MessageHistoryTable.NAME,
                               MessageHistoryTable.COL_LOCAL_JID,
                               accountJid,
                               MessageHistoryTable.COL_REMOTE_JID,
                               MessageHistoryTable.COL_RECEIVED_TIMESTAMP);
            while (rs.next())
            {
                MessageEvent evt = convertDatabaseRecordToMessageEvent(null, rs);
                if (evt != null)
                {
                    results.add(evt);
                }
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Message History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(conn, rs);
        }

        // We'll be querying for both the most recent group message, and the
        // oldest status message.  That's because some groups won't have any
        // messages in them - and we sort those by creation date.  Therefore
        // create a set to store the group chats that we have already seen.
        Set<String> seenGroupChats = new HashSet<>();

        // Get the last group message for each group chat.
        try
        {
            conn = mDatabaseService.connect();
            rs = conn.findLastByType(GroupMessageHistoryTable.NAME,
                                     GroupMessageHistoryTable.COL_LOCAL_JID,
                                     accountJid,
                                     GroupMessageHistoryTable.COL_ROOM_JID,
                                     GroupMessageHistoryTable.COL_RECEIVED_TIMESTAMP,
                                     GroupMessageHistoryTable.COL_TYPE,
                                     GroupMessageHistoryTable.TYPE.GROUP_IM.ordinal());

            while (rs.next())
            {
                String room = rs.getString(GroupMessageHistoryTable.COL_ROOM_JID);
                MessageEvent evt = convertDatabaseRecordToGroupMessageEvent(rs, room, null);
                if (evt != null)
                {
                    results.add(evt);
                    seenGroupChats.add(room);
                }
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Group Message History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(conn, rs);
        }

        // A group chat may not contain any messages. In that case, we want to
        // get the _first_ status message - which will indicate when the group
        // was created.  We don't want the last as that will put a group which
        // has "x left" messages on top of an 'active' group.
        try
        {
            conn = mDatabaseService.connect();

            rs = conn.findFirstByType(GroupMessageHistoryTable.NAME,
                                      GroupMessageHistoryTable.COL_LOCAL_JID,
                                      accountJid,
                                      GroupMessageHistoryTable.COL_ROOM_JID,
                                      GroupMessageHistoryTable.COL_RECEIVED_TIMESTAMP,
                                      GroupMessageHistoryTable.COL_TYPE,
                                      GroupMessageHistoryTable.TYPE.STATUS.ordinal());

            while (rs.next())
            {
                String room = rs.getString(GroupMessageHistoryTable.COL_ROOM_JID);
                if (!seenGroupChats.contains(room))
                {
                    MessageEvent evt = convertDatabaseRecordToGroupMessageEvent(rs, room, null);
                    if (evt != null)
                    {
                        sLog.debug("Group chat with no messages, use first status message for room " + room +
                            " with timestamp " + evt.getTimestamp());
                        results.add(evt);
                    }
                }
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Group Status Message History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(conn, rs);
        }

        // Finally filter the results before returning them
        return filterResults(results, queryString, count);
    }

    /**
     * Once a complete list of results have been found, filter it to remove
     * 1. any results that don't match the query string (if one was provided)
     * 2. the older of any results that have the same source contact
     * 3. the older of any results that have the same source group chat
     *
     * @param searchResults The list of results to filter
     * @param queryString The query string that the results should match
     * @param count The maximum number of results.
     * @return the filtered results
     */
    private Collection<MessageEvent> filterResults(Set<MessageEvent> searchResults,
                                                   String queryString,
                                                   int count)
    {
        Collection<MessageEvent> filteredResults;
        Iterator<MessageEvent> resultsIterator = searchResults.iterator();

        // Sets to store the MetaContacts/ChatRooms that we've already found
        // events for, so we can check this set and discard any duplicates.
        Set<MetaContact> eventContacts = new HashSet<>();
        Set<String> eventChatRoomIds = new HashSet<>();

        // Check whether the user submitted a query string.
        boolean querySubmitted = false;
        if (queryString != null && queryString.length() > 0)
        {
            querySubmitted = true;
            queryString = queryString.toLowerCase();
        }

        sLog.debug("Found " + searchResults.size() +
            " conversations prior to filter/combine");

        while (resultsIterator.hasNext())
        {
            MessageEvent evt = resultsIterator.next();

            // If a query was submitted, check whether this event matches the
            // query string. If no query was submitted, all events match.
            if (querySubmitted && !eventMatchesQuery(evt, queryString))
            {
                // Result doesn't match, remove it.
                resultsIterator.remove();
            }
            else if (evt instanceof OneToOneMessageEvent)
            {
                Contact peerContact =
                                  ((OneToOneMessageEvent) evt).getPeerContact();
                if (peerContact != null)
                {
                    MetaContact metaContact =
                        MessageHistoryActivator.getContactListService()
                                         .findMetaContactByContact(peerContact);

                    if (metaContact != null)
                    {
                        // We only want to return one result per MetaContact so,
                        // if we've already seen an event for this MetaContact,
                        // remove it from the results. Otherwise, add this
                        // MetaContact to this list of contacts that we've
                        // already seen.
                        if (eventContacts.contains(metaContact))
                        {
                            resultsIterator.remove();
                        }
                        else
                        {
                            eventContacts.add(metaContact);
                        }
                    }
                }
            }
            else if (evt instanceof ChatRoomMessageEvent)
            {
                ChatRoomMessageEvent chatRoomEvent = (ChatRoomMessageEvent) evt;
                String chatRoomId = chatRoomEvent.getPeerIdentifier();

                if (chatRoomId != null)
                {
                    if (eventChatRoomIds.contains(chatRoomId))
                    {
                        // We've seen an event for this chat room already.  This
                        // one therefore is after the old one.
                        resultsIterator.remove();
                    }
                    else
                    {
                        eventChatRoomIds.add(chatRoomId);
                    }
                }
            }
        }

        // Now that we've removed duplicate and non-matching items from the
        // results, discard older entries so we don't return more histories
        // than specified by 'count'
        int resultsSize = searchResults.size();
        if (resultsSize < count)
        {
            filteredResults = searchResults;
        }
        else
        {
            filteredResults =
                    new ArrayList<>(searchResults).subList(0, count);
        }

        sLog.debug("Found " + resultsSize + " conversations, return " +
            filteredResults.size());
        return filteredResults;
    }

    @Override
    public List<List<MessageEvent>> findLastForAllThreads(int count)
    {
        sLog.debug("Finding last " + count + " messages for each thread");
        String accountJid = getImAccountJid();

        /**
         * Analytic to track how much we hit
         * https://[indeterminate link]
         *
         * A proper fix is tracked under https://[indeterminate link]
         */
        if (accountJid == "unknown")
        {
            sLog.warn("Failed to provide chat list because IM provider not ready");
            sAnalyticsService.onEvent(AnalyticsEventType.CHAT_LIST_REQUEST_BEFORE_IM_PROVIDER_READY);
        }

        DatabaseConnection conn = null;
        ResultSet rs = null;

        // Get a set of all the thread IDs
        List<String> threadIds = new ArrayList<>();
        try
        {
            conn = mDatabaseService.connect();
            rs = conn.findUniqueColumnValues(
                MessageHistoryTable.NAME,
                MessageHistoryTable.COL_LOCAL_JID,
                accountJid,
                MessageHistoryTable.COL_REMOTE_JID);

            while (rs.next())
            {
                threadIds.add(rs.getString(MessageHistoryTable.COL_REMOTE_JID));
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Message History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(conn, rs);
        }
        sLog.debug(threadIds.size() + " one to one threads found");
        // For each thread, get the most recent n messages
        List<List<MessageEvent>> events = new ArrayList<List<MessageEvent>>();
        Iterator<String> threadIter = threadIds.iterator();
        while (threadIter.hasNext())
        {
            events.add(findLastForThread(threadIter.next(), count));
        }

        // Group chat
        List<String> roomIds = new ArrayList<>();
        try
        {
            conn = mDatabaseService.connect();
            rs = conn.findUniqueColumnValues(
                GroupMessageHistoryTable.NAME,
                GroupMessageHistoryTable.COL_LOCAL_JID,
                accountJid,
                GroupMessageHistoryTable.COL_ROOM_JID);

            while (rs.next())
            {
                roomIds.add(rs.getString(GroupMessageHistoryTable.COL_ROOM_JID));
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Group Message History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(conn, rs);
        }
        sLog.debug(roomIds.size() + " chat room threads found");
        // For each room, get the most recent n messages
        Iterator<String> roomIter = roomIds.iterator();
        while (roomIter.hasNext())
        {
            events.add(findLastForThread(roomIter.next(), count));
        }

        return events;
    }

    @Override
    public List<String> findLeastActiveChatrooms(int count)
    {
        sLog.debug("Find " + count + " least active group chats");

        DatabaseConnection connection = null;
        ResultSet rs = null;
        String accountJid = getImAccountJid();

        // ArrayList to store the IDs of the chat rooms with the least active
        // first.
        List<String> results = new ArrayList<>();

        // Treeset to store the messages sorted in reverse date order.
        TreeSet<MessageEvent> messages = new TreeSet<>(
                new MessageHistoryComparator<>(false));

        // Get a set of all the room IDs involved in group messages
        // with the current IM account.
        List<String> roomJids = new ArrayList<>();
        try
        {
            connection = mDatabaseService.connect();
            rs = connection.findUniqueColumnValues(
                GroupMessageHistoryTable.NAME,
                GroupMessageHistoryTable.COL_LOCAL_JID,
                accountJid,
                GroupMessageHistoryTable.COL_ROOM_JID);

            while (rs.next())
            {
                roomJids.add(rs.getString(GroupMessageHistoryTable.COL_ROOM_JID));
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read Message History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, rs);
        }

        // For each room ID find the last activity in the chatroom.
        for (String roomJid : roomJids)
        {
            try
            {
                connection = mDatabaseService.connect();
                rs = connection.findLastByIntValue(GroupMessageHistoryTable.NAME,
                              GroupMessageHistoryTable.COL_LOCAL_JID,
                              accountJid,
                              GroupMessageHistoryTable.COL_ROOM_JID,
                              roomJid,
                              GroupMessageHistoryTable.COL_RECEIVED_TIMESTAMP,
                              GroupMessageHistoryTable.COL_MSG_ID,
                              GroupMessageHistoryTable.COL_TYPE,
                              GroupMessageHistoryTable.TYPE.GROUP_IM.ordinal());

                // If the chatroom is already closed, ignore this message
                if (rs.next() && !rs.getBoolean(GroupMessageHistoryTable.COL_LEFT))
                {
                    messages.add(convertDatabaseRecordToGroupMessageEvent(
                        rs, roomJid, null));
                }
                else
                {
                    // We didn't find any messages in this history, so this
                    // might be a history for a chat room that has not yet
                    // received any messages. Look for the least recent
                    // status message, which indicates when the chat room was
                    // created.
                    rs = connection.findFirstByIntValue(
                             GroupMessageHistoryTable.NAME,
                             GroupMessageHistoryTable.COL_LOCAL_JID, accountJid,
                             GroupMessageHistoryTable.COL_ROOM_JID, roomJid,
                             GroupMessageHistoryTable.COL_RECEIVED_TIMESTAMP,
                             GroupMessageHistoryTable.COL_MSG_ID,
                             GroupMessageHistoryTable.COL_TYPE,
                             GroupMessageHistoryTable.TYPE.STATUS.ordinal());

                    // If the chatroom is already closed, ignore this message
                    if (rs.next() && !rs.getBoolean(GroupMessageHistoryTable.COL_LEFT))
                    {
                        messages.add(convertDatabaseRecordToGroupMessageEvent(
                            rs, roomJid, null));
                    }
                }
            }
            catch (SQLException e)
            {
                sLog.error("Failed to read Message History", e);
            }
            finally
            {
                DatabaseUtils.safeClose(connection, rs);
            }
        }

        List<MessageEvent> messagesAsList = new ArrayList<>(messages);

        int resultsSize = messagesAsList.size();
        if (resultsSize < count)
        {
            count = resultsSize;
        }

        for (int i = 0; i < count; i++)
        {
            MessageEvent event = messagesAsList.get(i);
            String id = event.getPeerIdentifier();
            sLog.debug("Chat room to be deleted: " + id +
                         ", last active " + event.getTimestamp());
            results.add(id);
        }

        sLog.debug("Found " + results.size() + " least active group chats");
        return results;
    }

    /**
     * Returns whether the given MessageEvent matches the given query string.
     *
     * @param evt The MessageEvent
     * @param queryString The query string
     *
     * @return Whether the MessageEvent matches the query string
     */
    protected boolean eventMatchesQuery(MessageEvent evt, String queryString)
    {
        String peerIdentifier = null;
        Contact peerContact = null;

        // For normal IMs or SMSs we want to try and match the query to the
        // contact or identifier (IM address or SMS number).  All other
        // messages are chat room messages and we only want to try and match
        // these to the chat room name (saved as the subject).
        if (evt instanceof OneToOneMessageEvent)
        {
            peerContact = ((OneToOneMessageEvent)evt).getPeerContact();
            peerIdentifier = evt.getPeerIdentifier();
        }
        else
        {
            peerIdentifier = ((ChatRoomMessageEvent)evt).getSubject();
        }

        if (peerIdentifier != null)
        {
            peerIdentifier = peerIdentifier.toLowerCase();
        }

        // Check whether the identifier matches the query string
        boolean identifierMatches =
            (peerIdentifier != null) && peerIdentifier.contains(queryString);

        // Check whether the event's contact (if any) matches the query string
        boolean contactMatches = false;
        if (peerContact != null)
        {
            String contactAddress = peerContact.getAddress();
            if (contactAddress != null)
            {
                contactAddress = contactAddress.toLowerCase();
            }
            String contactDisplayName = peerContact.getDisplayName();
            if (contactDisplayName != null)
            {
                contactDisplayName = contactDisplayName.toLowerCase();
            }

            MetaContact metaContact =
                MessageHistoryActivator.getContactListService().
                findMetaContactByContact(peerContact);

            String metaContactDisplayName = (metaContact == null) ?
                "" : metaContact.getDisplayName().toLowerCase();

            // Check whether the event's contact's address or display name
            // matches the query string.
            contactMatches = contactAddress.contains(queryString) ||
                             contactDisplayName.contains(queryString) ||
                             metaContactDisplayName.contains(queryString);
        }

        // The event matches the query if either the contact or the SMS
        // address matches.
        return contactMatches || identifierMatches;
    }

    @Override
    public void setLastMessageReadStatus(Object descriptor,
                                         boolean isRead,
                                         boolean isRefresh)
    {
        if (descriptor instanceof MetaContact)
        {
            setLastMessageReadStatus((MetaContact) descriptor, isRead, isRefresh);
        }
        else if (descriptor instanceof String)
        {
            setLastMessageReadStatus((String) descriptor, isRead, isRefresh);
        }
        else if (descriptor instanceof ChatRoom)
        {
            setLastMessageReadStatus((ChatRoom) descriptor, isRead, isRefresh);
        }
        else
        {
            sLog.warn("Cannot set message read status - unknown descriptor type " +
                                                                         descriptor);
        }
    }

    /**
     * Sets the read status of the entry for the most recent message in the
     * supplied MetaContact's history and refreshes the SourceContact that
     * represents this message in the 'Recent' tab.
     *
     * @param metaContact The MetaContact whose message is to be set as read/unread
     * @param isRead If true, mark the message as read, otherwise mark it as
     * unread.
     * @param isRefresh If true, the existing history entry for this message
     * should be refreshed in its place in the 'Recent' tab, rather than moved
     * to the top of the list.
     */
    private void setLastMessageReadStatus(MetaContact metaContact,
                                          boolean isRead,
                                          boolean isRefresh)
    {
        // Update for all contacts in the Metacontact
        Iterator<Contact> iter = metaContact.getContacts();
        while (iter.hasNext())
        {
            Contact contact = iter.next();
            updateLastMessageReadStatus(contact.getAddress(), isRead);
        }

        // Update for any sms numbers associated with the Metacontact
        if (ConfigurationUtils.isSmsEnabled())
        {
            for (String smsNumber : metaContact.getSmsNumbers())
            {
                updateLastMessageReadStatus(smsNumber, isRead);
            }
        }

        // Update the 'Recent' tab.
        sLog.debug("Setting messages from " + metaContact +
                           " as read status " + isRead + " in the recent tab");
        sendMessageHistoryChanged(metaContact, isRefresh);
    }

    /**
     * Sets the read status of the entry for the most recent message in the
     * supplied SMS number's history.
     *
     * @param smsNumber The SMS number whose message is to be set as
     * read/unread.
     * @param isRead If true, mark the message as read, otherwise mark it as
     * unread.
     * @param isRefresh If true, the existing history entry for this message
     * should be refreshed in its place in the 'Recent' tab, rather than moved
     * to the top of the list.
     */
    private void setLastMessageReadStatus(String smsNumber,
                                          boolean isRead,
                                          boolean isRefresh)
    {
        smsNumber = formatToNationalNumber(smsNumber);

        updateLastMessageReadStatus(smsNumber, isRead);

        // Update the 'Recent' tab.
        sLog.debug("Setting messages from " + logHasher(smsNumber) +
                           " as read status " + isRead + " in the recent tab");
        sendMessageHistoryChanged(smsNumber, isRefresh);
    }

    /**
     * Sets the read status of the most recent message in the database for the
     * supplied remote address.
     *
     * @param remoteJid The remote JID / SMS number to update for.
     * @param isRead If true, mark the message as read, otherwise mark it as
     * unread.
     */
    private void updateLastMessageReadStatus(String remoteJid, boolean isRead)
    {
        DatabaseConnection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;

        try
        {
            connection = mDatabaseService.connect();

            preparedStatement = connection.prepare(
                "UPDATE " + MessageHistoryTable.NAME +
                " SET " + MessageHistoryTable.COL_READ + "=?" +
                " WHERE " + MessageHistoryTable.COL_ID + " IN" +
                " (SELECT TOP 1 " + MessageHistoryTable.COL_ID +
                " FROM " + MessageHistoryTable.NAME +
                " WHERE " + MessageHistoryTable.COL_LOCAL_JID +
                " =? AND " + MessageHistoryTable.COL_REMOTE_JID +
                " =? ORDER BY " +
                MessageHistoryTable.COL_RECEIVED_TIMESTAMP + " DESC)");

            preparedStatement.setBoolean(1, isRead);
            preparedStatement.setString(2, getImAccountJid());
            preparedStatement.setString(3, remoteJid);

            int rowsUpdated = connection.execute(preparedStatement);
            sLog.debug("Rows updated: " + rowsUpdated);
        }
        catch (SQLException e)
        {
            sLog.error("Can't read/write message history for " + remoteJid, e);
        }
        finally
        {
            DatabaseUtils.safeClose(rs);
            DatabaseUtils.safeClose(connection, preparedStatement);
        }
    }

    /**
     * Sets the read status of the entry for the most recent message in the
     * supplied chat room's history.
     *
     * @param chatRoom The chat room whose message is to be set as read/unread.
     * @param isRead If true, mark the message as read, otherwise mark it as
     * unread.
     * @param isRefresh If true, the existing history entry for this message
     * should be refreshed in its place in the 'Recent' tab, rather than moved
     * to the top of the list.
     */
    private void setLastMessageReadStatus(ChatRoom chatRoom,
                                          boolean isRead,
                                          boolean isRefresh)
    {
        synchronized (chatRoom)
        {
            // Update the 'isRead' flag in history.
            updateChatroomHistoryFieldBoolean(chatRoom.getIdentifier().toString(),
                                       GroupMessageHistoryTable.COL_READ,
                                       isRead);

            // Update the 'Recent' tab.
            sLog.debug("Setting messages from " + sanitiseChatRoom(chatRoom.getIdentifier()) +
                       " as read status " + isRead + " in the recent tab");
            sendChatRoomMessageHistoryChanged(chatRoom, isRefresh);
        }
    }

    @Override
    public void setChatroomClosedStatus(ChatRoom chatRoom, boolean closed)
    {
        synchronized (chatRoom)
        {
            // Update the 'isLeft' flag in history
            if (updateChatroomHistoryFieldBoolean(chatRoom.getIdentifier().toString(),
                                           GroupMessageHistoryTable.COL_LEFT,
                                           closed))
            {
                // If we actually updated something, update the 'Recent' tab
                sLog.debug("Setting chat room " + sanitiseChatRoom(chatRoom.getIdentifier()) +
                           " as closed = " + closed + " in the recent tab");
                sendChatRoomMessageHistoryChanged(chatRoom, true);
            }
        }
    }

    @Override
    public void setRemovedFromChatroom(ChatRoom chatRoom)
    {
        synchronized (chatRoom)
        {
            sLog.info("Adding removed status message for: " + sanitiseChatRoom(chatRoom.getIdentifier()));

            writeGroupMessage(chatRoom,
                              getImAccountJid(),
                              GroupMessageHistoryTable.DIRECTION.OUT,
                              DEFAULT_REMOVED_FROM_GROUP_CHAT_STATUS_STRING,
                              UUID.randomUUID().toString(),
                              new Date(),
                              chatRoom.getSubject(),
                              true,
                              true,
                              true,
                              GroupMessageHistoryTable.TYPE.STATUS);
        }
    }

    @Override
    public void updateReadStatus(String remoteJid, String xmppId, boolean isRead)
    {
        DatabaseConnection connection = null;
        PreparedStatement preparedStatement = null;

        try
        {
            connection = mDatabaseService.connect();

            preparedStatement = connection.prepare("UPDATE " +
                MessageHistoryTable.NAME + " SET " +
                MessageHistoryTable.COL_READ + "=? WHERE " +
                MessageHistoryTable.COL_LOCAL_JID + "=? AND " +
                MessageHistoryTable.COL_REMOTE_JID + "=? AND " +
                MessageHistoryTable.COL_MSG_ID + "=?");

            preparedStatement.setBoolean(1, isRead);
            preparedStatement.setString(2, getImAccountJid());
            preparedStatement.setString(3, formatToNationalNumber(remoteJid));
            preparedStatement.setString(4, xmppId);

            connection.execute(preparedStatement);
        }
        catch (SQLException e)
        {
            sLog.error("Could not update read flag", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection);
        }
    }

    @Override
    public void updateReadStatusToReadForAllUnread(String remoteJid)
    {
        // We don't know what type of chat the remoteJid relates to - try
        // looking for one-to-one chats first, then try group.
        DatabaseConnection connection = null;
        PreparedStatement preparedStatement = null;

        // One-to-one
        try
        {
            connection = mDatabaseService.connect();

            preparedStatement = connection.prepare("UPDATE " +
                MessageHistoryTable.NAME + " SET " +
                MessageHistoryTable.COL_READ + "=? WHERE " +
                MessageHistoryTable.COL_LOCAL_JID + "=? AND " +
                MessageHistoryTable.COL_REMOTE_JID + "=? AND " +
                MessageHistoryTable.COL_READ + "=?");

            preparedStatement.setBoolean(1, true);
            preparedStatement.setString(2, getImAccountJid());
            preparedStatement.setString(3, formatToNationalNumber(remoteJid));
            preparedStatement.setBoolean(4, false);

            connection.execute(preparedStatement);
        }
        catch (SQLException e)
        {
            sLog.error("Could not update read flag", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection);
        }

        // Group
        try
        {
            connection = mDatabaseService.connect();

            preparedStatement = connection.prepare("UPDATE " +
                GroupMessageHistoryTable.NAME + " SET " +
                GroupMessageHistoryTable.COL_READ + "=? WHERE " +
                GroupMessageHistoryTable.COL_LOCAL_JID + "=? AND " +
                GroupMessageHistoryTable.COL_ROOM_JID + "=? AND " +
                GroupMessageHistoryTable.COL_READ + "=?");

            preparedStatement.setBoolean(1, true);
            preparedStatement.setString(2, getImAccountJid());
            preparedStatement.setString(3, remoteJid);
            preparedStatement.setBoolean(4, false);

            connection.execute(preparedStatement);
        }
        catch (SQLException e)
        {
            sLog.error("Could not update read flag", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection);
        }
    }

    /**
     * Updates the given column to the given value for the latest message
     * and status message which match the current account ID and the given
     * chatroom ID. Only for boolean columns.
     *
     * @param roomJid The ID of the chat room.
     * @param colName The column to update.
     * @param newValue The new value.
     *
     * @return true if we updated at least one field, false otherwise
     */
    private boolean updateChatroomHistoryFieldBoolean(String roomJid,
                                                      String colName,
                                                      boolean newValue)
    {
        int rowsUpdated = 0;
        DatabaseConnection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet rs1 = null;
        ResultSet rs2 = null;

        try
        {
            String accountJid = getImAccountJid();
            connection = mDatabaseService.connect();

            // Update the value for the latest group IM message
            preparedStatement = connection.prepare(
                "UPDATE " + GroupMessageHistoryTable.NAME +
                " SET " + colName + "=?" +
                " WHERE " + GroupMessageHistoryTable.COL_ID + " IN" +
                " (SELECT TOP 1 " + GroupMessageHistoryTable.COL_ID +
                " FROM " + GroupMessageHistoryTable.NAME +
                " WHERE " + GroupMessageHistoryTable.COL_LOCAL_JID +
                " =? AND " + GroupMessageHistoryTable.COL_ROOM_JID +
                " =? AND " + GroupMessageHistoryTable.COL_TYPE + " =? ORDER BY " +
                GroupMessageHistoryTable.COL_RECEIVED_TIMESTAMP + " DESC)");

            preparedStatement.setBoolean(1, newValue);
            preparedStatement.setString(2, accountJid);
            preparedStatement.setString(3, roomJid);
            preparedStatement.setInt(4, GroupMessageHistoryTable.TYPE.GROUP_IM.ordinal());
            rowsUpdated = connection.execute(preparedStatement);

            // Update the value for the latest group chat status message
            preparedStatement = connection.prepare(
                "UPDATE " + GroupMessageHistoryTable.NAME +
                " SET " + colName + "=?" +
                " WHERE " + GroupMessageHistoryTable.COL_ID + " IN" +
                " (SELECT TOP 1 " + GroupMessageHistoryTable.COL_ID +
                " FROM " + GroupMessageHistoryTable.NAME +
                " WHERE " + GroupMessageHistoryTable.COL_LOCAL_JID +
                " =? AND " + GroupMessageHistoryTable.COL_ROOM_JID +
                " =? AND " + GroupMessageHistoryTable.COL_TYPE + " =? ORDER BY " +
                GroupMessageHistoryTable.COL_RECEIVED_TIMESTAMP + " DESC)");

            preparedStatement.setBoolean(1, newValue);
            preparedStatement.setString(2, accountJid);
            preparedStatement.setString(3, roomJid);
            preparedStatement.setInt(4, GroupMessageHistoryTable.TYPE.STATUS.ordinal());
            rowsUpdated += connection.execute(preparedStatement);
            sLog.debug("Rows updated: " + rowsUpdated);
        }
        catch (SQLException e)
        {
            sLog.error("Error setting group chat " + colName +
                                                          " to " + newValue, e);
        }
        finally
        {
            DatabaseUtils.safeClose(rs1);
            DatabaseUtils.safeClose(rs2);
            DatabaseUtils.safeClose(connection, preparedStatement);
        }

        return rowsUpdated > 0;
    }

    /**
     * Updates the given column to the given value for all records which match
     * the current account ID and the given chatroom ID. Only for String columns.
     *
     * @param roomJid The ID of the chat room.
     * @param colName The column to update.
     * @param newValue The new value.
     *
     * @return true if we updated the field, false otherwise
     */
    private boolean updateChatroomHistoryFieldString(String roomJid,
                                                     String colName,
                                                     String newValue)
    {
        int rowsUpdated = 0;
        DatabaseConnection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet rs1 = null;
        ResultSet rs2 = null;

        try
        {
            String accountJid = getImAccountJid();
            connection = mDatabaseService.connect();
            preparedStatement = connection.prepare(
                "UPDATE " + GroupMessageHistoryTable.NAME +
                " SET " + colName + "=?" +
                " WHERE " + GroupMessageHistoryTable.COL_ID + " IN" +
                " (SELECT TOP 1 " + GroupMessageHistoryTable.COL_ID +
                " FROM " + GroupMessageHistoryTable.NAME +
                " WHERE " + GroupMessageHistoryTable.COL_LOCAL_JID +
                " =? AND " + GroupMessageHistoryTable.COL_ROOM_JID +
                " =? AND " + GroupMessageHistoryTable.COL_TYPE + " =? ORDER BY " +
                GroupMessageHistoryTable.COL_RECEIVED_TIMESTAMP + " DESC)");

            preparedStatement.setString(1, newValue);
            preparedStatement.setString(2, accountJid);
            preparedStatement.setString(3, roomJid);
            preparedStatement.setInt(4, GroupMessageHistoryTable.TYPE.GROUP_IM.ordinal());

            rowsUpdated = connection.execute(preparedStatement);
            sLog.debug("Rows updated: " + rowsUpdated);

            if (rowsUpdated == 0)
            {
                // Haven't found any group messages for this chatroom. Use the
                // earliest status message instead, as this contains the
                // timestamp of when the chatroom was created.
                preparedStatement = connection.prepare(
                    "UPDATE " + GroupMessageHistoryTable.NAME +
                    " SET " + colName + "=?" +
                    " WHERE " + GroupMessageHistoryTable.COL_ID + " IN" +
                    " (SELECT TOP 1 " + GroupMessageHistoryTable.COL_ID +
                    " FROM " + GroupMessageHistoryTable.NAME +
                    " WHERE " + GroupMessageHistoryTable.COL_LOCAL_JID +
                    " =? AND " + GroupMessageHistoryTable.COL_ROOM_JID +
                    " =? AND " + GroupMessageHistoryTable.COL_TYPE + " =? ORDER BY " +
                    GroupMessageHistoryTable.COL_RECEIVED_TIMESTAMP + " ASC)");

                preparedStatement.setString(1, newValue);
                preparedStatement.setString(2, accountJid);
                preparedStatement.setString(3, roomJid);
                preparedStatement.setInt(4, GroupMessageHistoryTable.TYPE.STATUS.ordinal());

                rowsUpdated = connection.execute(preparedStatement);
                sLog.debug("Rows updated: " + rowsUpdated);
            }
        }
        catch (SQLException e)
        {
            sLog.error("Error setting group chat " + colName +
                                                          " to " + newValue, e);
        }
        finally
        {
            DatabaseUtils.safeClose(rs1);
            DatabaseUtils.safeClose(rs2);
            DatabaseUtils.safeClose(connection, preparedStatement);
        }

        return (rowsUpdated == 1);
    }

    /**
     * Removes the message with the provided id from the database
     *
     * @param uid id of the message
     */
    public void removeByID(String uid)
    {
        DatabaseConnection connection = null;

        try
        {
            connection = mDatabaseService.connect();

            PreparedStatement preparedStatement = connection.prepare("DELETE FROM " +
            MessageHistoryTable.NAME + " WHERE " +
            MessageHistoryTable.COL_MSG_ID + " = ?");

            preparedStatement.setString(1, uid);
            connection.execute(preparedStatement);
        }
        catch (SQLException e)
        {
            sLog.error("Failed to remove message ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection);
        }
    }

    /**
     * Returns the IM ProtocolProviderService.
     *
     * @return the IM ProtocolProviderService.
     */
    private ProtocolProviderService getImProvider()
    {
        // This can't be initialized when this service starts, as the IM
        // provider won't be available then.
        if (mImProvider == null)
        {
            mImProvider = AccountUtils.getImProvider();
        }

        return mImProvider;
    }

    /**
     * Returns the account ID of the IM account, or unknown if no IM provider found.
     */
    private String getImAccountJid()
    {
        ProtocolProviderService provider = getImProvider();
        if (provider == null)
        {
            sLog.error("No IM provider found, likely a race condition between accessing " +
                       "and registering IM provider. Database queries using Jid 'unknown' " +
                       "will return unexpected results. ");
            // See latest comments on DUIR-5717 for more information on this race condition
            return "unknown";
        }
        return provider.getAccountID().getUserID().toLowerCase();
    }

    @Override
    public void addSourceContactChangeListener(SourceContactChangeListener listener)
    {
        synchronized (mSourceContactChangeListeners)
        {
            if (!mSourceContactChangeListeners.contains(listener))
            {
                mSourceContactChangeListeners.add(listener);
            }
        }
    }

    /**
     * Notifies all registered SourceContactChangedListeners that the given
     * SourceContact has been updated.
     *
     * @param sourceContact the SourceContact that has been updated.
     * @param isRefresh If true, the existing SourceContact should be refreshed
     *                  rather than replaced.
     */
    protected void fireSourceContactUpdated(SourceContact sourceContact,
                                            boolean isRefresh)
    {
        synchronized (mSourceContactChangeListeners)
        {
            sLog.debug("Dispatching update event to " +
                         mSourceContactChangeListeners.size() +
                         " SourceContactChangeListeners.");

            for (SourceContactChangeListener listener : mSourceContactChangeListeners)
            {
                listener.sourceContactUpdated(sourceContact, isRefresh);
            }
        }
    }

    /**
     * Notifies all registered SourceContactChangedListeners that the given
     * SourceContact has been added.
     *
     * @param sourceContact the SourceContact that has been added.
     */
    protected void fireSourceContactAdded(SourceContact sourceContact)
    {
        synchronized (mSourceContactChangeListeners)
        {
            sLog.debug("Dispatching add event to " +
                         mSourceContactChangeListeners.size() +
                         " SourceContactChangeListeners.");

            for (SourceContactChangeListener listener : mSourceContactChangeListeners)
            {
                listener.sourceContactAdded(sourceContact);
            }
        }
    }

    @Override
    public void removeSourceContactChangeListener(SourceContactChangeListener listener)
    {
        synchronized (mSourceContactChangeListeners)
        {
            mSourceContactChangeListeners.remove(listener);
        }
    }

    /**
     * Simple message implementation.
     */
    private static class MessageImpl
        extends AbstractMessage
    {
        private final String peerAddress;

        private final boolean isOutgoing;

        private final Date messageReceivedDate;

        private final boolean isRead;

        private final String type;

        private final boolean isFailed;

        private final boolean isClosed;

        private final String smppId;

        MessageImpl(String content, String contentType, String encoding,
            String peer, String subject, String messageUID,
            boolean isOutgoing, Date messageReceivedDate, boolean isRead,
            String type, boolean isFailed, boolean isClosed, String smppId)
        {
            super(content, contentType, encoding, subject, messageUID);

            this.peerAddress = peer;
            this.isOutgoing = isOutgoing;
            this.messageReceivedDate = messageReceivedDate;
            this.isRead = isRead;
            this.type = type;
            this.isFailed = isFailed;
            this.isClosed = isClosed;
            this.smppId = smppId;
        }

        public Date getMessageReceivedDate()
        {
            return messageReceivedDate;
        }

        @Override
        public String toString()
        {
            return super.toString() +
                   ": type = " + type + ", peerAddress = " + peerAddress +
                   ", subject = " + getSubject() +
                   ", isRead = " + isRead + ", isOutgoing = " + isOutgoing +
                   ", isFailed = " + isFailed + ", isClosed = " + isClosed +
                   ", messageUID = " + getMessageUID() + ", smppId = " + smppId +
                   ", messageReceivedDate = " + messageReceivedDate;
        }
    }

    /**
     * Used to compare history items to be ordered in TreeSet according their
     * timestamp.  If the timestamps are identical, they will be ordered
     * according to their ID.
     */
    private static class MessageHistoryComparator<T> implements Comparator<T>
    {
        /**
         * If true, compare the items in reverse order
         */
        private boolean reverse;

        /**
         * Creates a comparator of message history items.
         *
         * @param reverse If true, compare the events in reverse order.
         */
        public MessageHistoryComparator(boolean reverse)
        {
            this.reverse = reverse;
        }

        private Date getDate(Object o)
        {
            Date date = new Date(0);
            if(o instanceof MessageEvent)
                date = ((MessageEvent)o).getTimestamp();
            else
                sLog.debug(
                    "Asked to compare objects of unknown type: " + o.getClass());

            return date;
        }

        private String getId(Object o)
        {
            String id = "";
            if(o instanceof MessageEvent)
                id = ((MessageEvent)o).getSourceMessage().getMessageUID();
            else
                sLog.debug(
                    "Asked to compare objects of unknown type: " + o.getClass());

            return id;
        }

        @Override
        public int compare(T o1, T o2)
        {
            Date date1 = getDate(o1);
            Date date2 = getDate(o2);

            int result = date1.compareTo(date2);

            // If we return 0 from this comparator, the message history items
            // disappear from the results, as they are stored in a TreeSet.
            // To avoid this, compare their IDs instead.
            if (result == 0)
            {
                String id1 = getId(o1);
                String id2 = getId(o2);

                result = id1.compareTo(id2);
            }

            if (reverse)
            {
                result = result * -1;
            }

            return result;
        }
    }

    /**
     * Simple ChatRoomMember implementation.
     */
    static class ChatRoomMemberImpl
        implements ChatRoomMember
    {
        private final ChatRoom chatRoom;

        private final String name;

        private ChatRoomMemberRole role;

        public ChatRoomMemberImpl(String name, ChatRoom chatRoom,
            ChatRoomMemberRole role)
        {
            this.chatRoom = chatRoom;
            this.name = name;
            this.role = role;
        }

        @Override
        public ChatRoom getChatRoom()
        {
            return chatRoom;
        }

        @Override
        public ProtocolProviderService getProtocolProvider()
        {
            return chatRoom.getParentProvider();
        }

        @Override
        public String getContactAddressAsString()
        {
            return name;
        }

        @Override
        public String getName()
        {
            return name;
        }

        @Override
        public ChatRoomMemberRole getRole()
        {
            return role;
        }

        @Override
        public BufferedImageFuture getAvatar()
        {
            return null;
        }

        @Override
        public void setRole(ChatRoomMemberRole newRole)
        {
            this.role = newRole;
        }

        @Override
        public Contact getContact()
        {
            return null;
        }

        @Override
        public String toString()
        {
            return logHasher(getContactAddressAsString());
        }
    }

    @Override
    public void chatRoomCreated(ChatRoomCreatedEvent evt)
    {
        ChatRoom chatRoom = evt.getChatRoom();
        sLog.info("Adding member presence and property change listeners to chat room "
                  + sanitiseChatRoom(chatRoom.getIdentifier()));

        chatRoom.addPropertyChangeListener(this);

        // If this chat room doesn't already have any history entries, create
        // a dummy entry so that we can display it in the 'Recent' tab.
        Collection<MessageEvent> messageEvents = findLast(chatRoom, 1);
        if (messageEvents == null || messageEvents.isEmpty())
        {
            writeChatRoomStatusMessage(evt);
        }
    }

    @Override
    public void memberPresenceChanged(ChatRoomMemberPresenceChangeEvent evt)
    {
        // ignore events that are not to do with room membership
        if ((evt.getEventType() != ChatRoomMemberPresenceChangeEvent.MEMBER_JOINED) &&
            (evt.getEventType() != ChatRoomMemberPresenceChangeEvent.MEMBER_LEFT))
            return;

        writeChatRoomStatusMessage(evt);
    }

    /**
     * Writes a status message to history for a chat room based on the
     * information in the given event.
     *
     * @param evt The event containing the information about the chat room
     * status
     */
    private void writeChatRoomStatusMessage(EventObject evt)
    {
        ChatRoom chatRoom = null;
        String contactAddress = null;
        Date timestamp = null;
        String textContent = null;
        String uid = null;
        boolean isRead = true;
        boolean isFailed = false;
        boolean isClosed = false;
        boolean sendChatData = false;
        boolean sendDisplayChatMotion = false;

        if (evt instanceof ChatRoomMemberPresenceChangeEvent)
        {
            // This is a member presence change event, so we need to write
            // a status message containing all details of the contact who
            // changed.
            ChatRoomMemberPresenceChangeEvent presenceEvt =
                (ChatRoomMemberPresenceChangeEvent) evt;
            chatRoom = presenceEvt.getChatRoom();
            ChatRoomMember chatRoomMember = presenceEvt.getChatRoomMember();
            contactAddress = chatRoomMember.getContactAddressAsString();
            timestamp = presenceEvt.getTimestamp();
            textContent = presenceEvt.getReason();
            uid = presenceEvt.getUID();
        }
        else if (evt instanceof LocalUserChatRoomPresenceChangeEvent)
        {
            LocalUserChatRoomPresenceChangeEvent presenceEvt =
                (LocalUserChatRoomPresenceChangeEvent) evt;
            chatRoom = presenceEvt.getChatRoom();
            contactAddress = presenceEvt.getLocalUserId().toString();
            timestamp = presenceEvt.getTimestamp();
            textContent = presenceEvt.getReason();
            uid = presenceEvt.getUID();

            // If this is a notification that we have left the chatroom,
            // mark the chatroom as closed when we write this message.
            if (!(presenceEvt.getEventType() ==
                LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED))
            {
                isClosed = true;
            }
        }
        else if (evt instanceof ChatRoomCreatedEvent)
        {
            // This is a chat room created event so we're only writing a
            // dummy event that can be used to create a placeholder entry
            // in the 'Recent' tab until some real messages or presence
            // change events have been received.  Therefore, there's no
            // need for real message content.
            ChatRoomCreatedEvent createdEvt = (ChatRoomCreatedEvent) evt;
            chatRoom = createdEvt.getChatRoom();
            contactAddress = chatRoom.getIdentifier().toString();
            timestamp = new Date();
            // Empty string means we won't display the message.
            textContent = "";
            uid = UUID.randomUUID().toString();
        }
        else if (evt instanceof ChatRoomPropertyChangeEvent)
        {
            ChatRoomPropertyChangeEvent subjectEvt = (ChatRoomPropertyChangeEvent) evt;
            chatRoom = subjectEvt.getSourceChatRoom();
            contactAddress = subjectEvt.getFromAddress();
            timestamp = new Date();
            textContent = SUBJECT + "," + contactAddress + "," + subjectEvt.getNewValue();
            uid = UUID.randomUUID().toString();

            if (findByKeyword(chatRoom, textContent).size() != 0)
            {
                return;
            }

            sendChatData = true;

            // Tell WISPA to display the group chat if it's new and you've created it. Note
            // limitation here that this also fires if you've created a new group chat on
            // a different device logged in to the same account.
            if (subjectEvt.getOldValue() == null && (getImAccountJid().equals(contactAddress)))
            {
                sendDisplayChatMotion = true;
            }

        }

        writeGroupMessage(chatRoom,
            contactAddress,
            GroupMessageHistoryTable.DIRECTION.IN,
            textContent,
            uid,
            timestamp,
            chatRoom.getSubject(),
            isRead,
            isFailed,
            isClosed,
            GroupMessageHistoryTable.TYPE.STATUS);

        if (sendChatData)
        {
            // Notify WISPA of the subject update now that the update
            // has been stored in the database.
            sWISPA.notify(WISPANamespace.MESSAGING, WISPAAction.DATA, evt);

            if (sendDisplayChatMotion)
            {
                // Tell WISPA to display the group chat if you've just created it.
                // Must be done after sending DATA containing the group chat.
                sWISPA.notify(WISPANamespace.MESSAGING, WISPAAction.MOTION, chatRoom);
            }
        }
    }

    @Override
    public void chatRoomPropertyChanged(ChatRoomPropertyChangeEvent evt)
    {
        if (evt.getPropertyName().equals(
            ChatRoomPropertyChangeEvent.CHAT_ROOM_SUBJECT))
        {
            ChatRoom sourceChatRoom = evt.getSourceChatRoom();
            synchronized (sourceChatRoom)
            {
                String roomJid = sourceChatRoom.getIdentifier().toString();
                String newSubject = (String) evt.getNewValue();

                if (!newSubject.equals(evt.getOldValue()))
                {
                    writeChatRoomStatusMessage(evt);
                }

                sLog.debug("Updating subject of " + sanitiseChatRoom(roomJid));

                // Update the subject in history
                if (updateChatroomHistoryFieldString(roomJid,
                                                     GroupMessageHistoryTable.COL_SUBJECT,
                                                     newSubject))
                {
                    // If we actually updated something, update the 'Recent' tab
                    sendChatRoomMessageHistoryChanged(sourceChatRoom, true);
                }
            }
        }
    }

    @Override
    public void chatRoomPropertyChangeFailed(ChatRoomPropertyChangeFailedEvent evt) {}

    /**
     * Notifies the MessageHistoryContactSource that the message history has
     * changed for the given chat room so it can update the results of the
     * latest history query.
     *
     * @param chatRoom The chat room
     * @param isRefresh If true, the existing history entry should be refreshed
     * in its place in the 'Recent' tab, rather than moved to the top of this
     * list.
     */
    private void sendChatRoomMessageHistoryChanged(ChatRoom chatRoom,
                                                   boolean isRefresh)
    {
        // We need to send the MessageHistoryContactSource a MessageEvent
        // representing the latest state of this chat room that we want to
        // display in the 'Recent' tab.  If the chatroom has ever received a
        // message, we want to display the timestamp of that, so first we look
        // for the most recent message in the chat room.
        MessageEvent latestMessage = findLatestChatMessage(chatRoom);

        // If the chat room hasn't received any messages, we look for the
        // oldest status message in the chat room, as we want to just display
        // the timestamp of when the chat room was created.
        if (latestMessage == null)
        {
            latestMessage = findOldestStatus(chatRoom);
        }

        if (latestMessage != null)
        {
            sLog.debug("Chat room history changed for " + sanitiseChatRoom(chatRoom.getIdentifier()));
            mMsgHistoryContactSource.messageHistoryChanged(
                latestMessage, isRefresh);
        }
        else
        {
            sLog.warn("No history found for chatRoom " + sanitiseChatRoom(chatRoom.getIdentifier()));
        }
    }

    /**
     * Notifies the MessageHistoryContactSource that the message history has
     * changed for the given SMS number so it can update the results of the
     * latest history query.
     *
     * @param smsNumber The SMS number
     * @param isRefresh If true, the existing history entry should be refreshed
     * in its place in the 'Recent' tab, rather than moved to the top of this
     * list.
     */
    private void sendMessageHistoryChanged(String smsNumber, boolean isRefresh)
    {
        // We need to send the MessageHistoryContactSource a MessageEvent
        // representing the latest state of this SMS conversation that we want
        // to display in the 'Recent' tab, so look for the most recent message
        // in the conversation.
        String loggableSmsNumber = logHasher(smsNumber);
        List<MessageEvent> messages = findLast(smsNumber, 1);

        if (!messages.isEmpty())
        {
            sLog.debug("History changed for " + loggableSmsNumber);
            mMsgHistoryContactSource.messageHistoryChanged(messages.iterator().next(), isRefresh);
        }
        else
        {
            sLog.warn("No history found for SMS number " + loggableSmsNumber);
        }
    }

    /**
     * Notifies the MessageHistoryContactSource that the message history has
     * changed for the given MetaContact so it can update the results of the
     * latest history query.
     *
     * @param metaContact The MetaContact
     * @param isRefresh If true, the existing history entry should be refreshed
     * in its place in the 'Recent' tab, rather than moved to the top of this
     * list.
     */
    private void sendMessageHistoryChanged(MetaContact metaContact,
        boolean isRefresh)
    {
        // We need to send the MessageHistoryContactSource a MessageEvent
        // representing the latest state of the conversation with this contact
        // that we want to display in the 'Recent' tab, so look for the most
        // recent message in the conversation.
        Collection<MessageEvent> messages = findLast(metaContact, 1);

        if (!messages.isEmpty())
        {
            sLog.debug("History changed for " + metaContact);
            mMsgHistoryContactSource.messageHistoryChanged(
                    messages.iterator().next(), isRefresh);
        }
        else
        {
            sLog.warn("No history found for MetaContact " + metaContact);
        }
    }

    /**
     * In case the JID is an SMS number, update it to national format
     * before getting the history so that the number is displayed
     * consistently in the chat window and message history.
     * @param numberOrJid Remote JID that might be an SMS number.
     * @return The formatted version, unchanged if an IM address was supplied.
     */
    private String formatToNationalNumber(String numberOrJid)
    {
        return MessageHistoryActivator.getPhoneNumberUtilsService()
                                          .formatNumberToNational(numberOrJid);
    }

    /**
     * Exists to keep calling code simpler.
     * @param smsNumber Number to do lookup on.
     * @return The MetaContact for the SMS number, if found.
     */
    private MetaContact smsNumberToMetaContact(String smsNumber)
    {
        return MessageHistoryActivator.getContactListService()
                                           .findMetaContactForSmsNumber(smsNumber);
    }
}
