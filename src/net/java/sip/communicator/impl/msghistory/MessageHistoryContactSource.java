/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.msghistory;

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>MessageHistoryContactSource</tt> is the contact source for the
 * message history.
 */
public class MessageHistoryContactSource implements ContactSourceService
{
    /**
     * The logger.
     */
    private static final Logger sLog =
        Logger.getLogger(MessageHistoryContactSource.class);

    /**
     * The query string most recently applied to this contact source.
     */
    private String mQueryString = null;

    /**
     * The query most recently applied to this contact source.
     */
    private MessageHistoryContactQuery mHistoryQuery = null;

    /**
     * The data key of the SourceContactDescriptor object used to store a
     * reference to the SourceContact's UIContact in the SourceContact.
     */
    public static final String UI_CONTACT_DATA_KEY =
                                          "SourceUIContact.uiContactDescriptor";

    public int getType()
    {
        return MESSAGE_HISTORY_TYPE;
    }

    public String getDisplayName()
    {
        return MessageHistoryActivator.getResources().
                                       getI18NString("service.gui.MESSAGE_HISTORY");
    }

    public ContactQuery queryContactSource(String queryString)
    {
        return queryContactSource(queryString,
                                  ContactSourceService.DEFAULT_QUERY_SIZE);
    }

    /**
     * UI REFRESH DELETION CANDIDATE
     * REFACTORED TO: MessagingNamespaceHandler.handleList()
     *
     */
    public ContactQuery queryContactSource(String queryString, int contactCount)
    {
        mQueryString = queryString;

        // Get a list of 'contactCount' events that match the queryString.
        Collection<MessageEvent> matchingEvents =
             MessageHistoryActivator.getMessageHistoryService()
                                    .findLastForAll(queryString, contactCount);

        mHistoryQuery = new MessageHistoryContactQuery(matchingEvents);

        return mHistoryQuery;
    }

    public int getIndex()
    {
        return -1;
    }

    /**
     * Called when a new MessageEvent is received that may change the results
     * that should be returned by the latest query to the
     * MessageHistoryContactSource.
     * @param isRefresh If true, the existing history entry should be refreshed
     * in its place in the 'Recent' tab, rather than moved to the top of this
     * list.
     */
    protected void messageHistoryChanged(MessageEvent evt, boolean isRefresh)
    {
        if (mHistoryQuery != null)
        {
            mHistoryQuery.updateSourceContacts(evt, isRefresh);
        }
    }

    /**
     * The <tt>MessageHistoryContactQuery</tt> contains information about a
     * current query to the contact source.
     */
    private class MessageHistoryContactQuery implements ContactQuery
    {
        /**
         * A list of all registered query listeners.
         */
        private final List<ContactQueryListener> mQueryListeners =
                new LinkedList<>();

        /**
         * A sorted set of all the source contact results.
         */
        private final TreeSet<MessageHistorySourceContact> mSourceContacts =
                new TreeSet<>(
                        new Comparator<MessageHistorySourceContact>()
                        {
                            @Override
                            public int compare(MessageHistorySourceContact o1,
                                               MessageHistorySourceContact o2)
                            {
                                return o1.getTimestamp().compareTo(o2.getTimestamp());
                            }
                        });

        /**
         * Indicates the status of this query. When created this query is in
         * progress.
         */
        private int mStatus = QUERY_IN_PROGRESS;

        /**
         * Creates an instance of <tt>MessageHistoryContactQuery</tt> by
         * specifying the list of message event results.
         *
         * @param messageEvents the list of message events, which are the
         * result of this query
         */
        public MessageHistoryContactQuery(Collection<MessageEvent> messageEvents)
        {
            for (MessageEvent messageEvent : messageEvents)
            {
                if (mStatus != QUERY_CANCELED)
                {
                    MessageHistorySourceContact messageHistorySourceContact =
                        new MessageHistorySourceContact(
                                               MessageHistoryContactSource.this,
                                               messageEvent);
                    mSourceContacts.add(messageHistorySourceContact);
                }
            }

            if (mStatus != QUERY_CANCELED)
            {
                mStatus = QUERY_COMPLETED;
            }
        }

        public void addContactQueryListener(ContactQueryListener l)
        {
            synchronized (mQueryListeners)
            {
                mQueryListeners.add(l);
            }
        }

        public String getQueryString()
        {
            return mQueryString;
        }

        public void cancel()
        {
            mStatus = QUERY_CANCELED;
        }

        public int getStatus()
        {
            return mStatus;
        }

        public void removeContactQueryListener(ContactQueryListener l)
        {
            synchronized (mQueryListeners)
            {
                mQueryListeners.remove(l);
            }
        }

        public List<SourceContact> getQueryResults()
        {
            List<SourceContact> sourceContactsCopy =
                    new LinkedList<>();
            synchronized (mSourceContacts)
            {
                sourceContactsCopy.addAll(mSourceContacts);
            }

            return sourceContactsCopy;
        }

        public ContactSourceService getContactSource()
        {
            return MessageHistoryContactSource.this;
        }

        /**
         * Checks whether the SourceContacts returned by this query
         * should be modified as a result of the MessageEvent that has been
         * received.  If so, fires a SourceContactChanged event.
         *
         * @param evt The MessageEvent
         * @param isRefresh If true, the existing SourceContact should be
         * refreshed in its place in the 'Recent' tab, rather than moved to the
         * top of the list.
         */
        private void updateSourceContacts(final MessageEvent evt, final boolean isRefresh)
        {
            // This method results in updates to the UI, so it is called on the EDT
            if (!SwingUtilities.isEventDispatchThread())
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        updateSourceContacts(evt, isRefresh);
                    }
                });

                return;
            }

            sLog.debug("Updating source contacts.");

            MetaContactListService contactListService =
                                MessageHistoryActivator.getContactListService();
            MessageHistoryServiceImpl messageHistoryService =
                             MessageHistoryActivator.getMessageHistoryService();

            SourceContact matchedContact = null;
            String peerIdentifier = evt.getPeerIdentifier();

            // We only show the most recent entry per MetaContact in the
            // 'Recent' tab, even if the MetaContact has exchanged messages via
            // more than one SMS number/IM address. Therefore, the SMS number
            // or IM address in this MessageEvent might not match the address
            // (emphasized number) of the SourceContact that needs to be
            // updated. This means we need to check whether the MessageEvent
            // matches a MetaContact and, if so, use that MetaContact to search
            // for a SourceContact. If it doesn't match a MetaContact, this
            // must be an event for an SMS with a non-contact number, so we can
            // just compare the event's SMS number with the SourceContacts'
            // emphasized numbers.
            MetaContact metaContact = null;
            Contact contact = null;
            if (evt instanceof OneToOneMessageEvent)
            {
               contact = ((OneToOneMessageEvent)evt).getPeerContact();

               if (evt.getEventType() == MessageEvent.SMS_MESSAGE)
               {
                   metaContact = contactListService.findMetaContactForSmsNumber(peerIdentifier);
               }
               else if (contact != null)
               {
                   metaContact = contactListService.findMetaContactByContact(contact);
               }
            }

            synchronized (mSourceContacts)
            {
                // Search through the sourceContacts returned by this query to
                // find a match to this event and break when we find a match.
                for (MessageHistorySourceContact sourceContact : mSourceContacts)
                {
                    if (evt instanceof OneToOneMessageEvent)
                    {
                        if (metaContact != null &&
                            metaContact.equals(sourceContact.getMetaContact()))
                        {
                            matchedContact = sourceContact;
                            break;
                        }
                        else if (sourceContact.getEmphasizedNumber() != null &&
                                 sourceContact.getEmphasizedNumber().
                                               equalsIgnoreCase(peerIdentifier))
                        {
                            matchedContact = sourceContact;
                            break;
                        }
                    }
                    else
                    {
                        // This is a group chat so we match the group chat id
                        // of the source contact to the peer identifier
                        List<ContactDetail> details = sourceContact.
                            getContactDetails(ContactDetail.Category.GroupChat);

                        if (details != null && details.size() > 0)
                        {
                            String detail = details.get(0).getDetail();
                            if (detail.equalsIgnoreCase(peerIdentifier))
                            {
                                matchedContact = sourceContact;
                                break;
                            }
                        }
                    }
                }

                if (matchedContact != null &&
                    !evt.getTimestamp().before(matchedContact.getTimestamp()))
                {
                    // We've found a matching SourceContact, and the event is
                    // newer than it. Therefore, we need to replace the source
                    // contact with one containing updated details from this
                    // event.
                    // Note that we do !before rather than after to catch events
                    // with the same timestamp - e.g. when a group chat is created.
                    // First get the UI contact from the existing SourceContact,
                    // so that we can move it to the new SourceContact.
                    Object uiContact = matchedContact.getData(UI_CONTACT_DATA_KEY);

                    // Remove the UI contact from the existing SourceContact
                    // then remove the SourceContact from the list of query
                    // results.
                    matchedContact.setData(UI_CONTACT_DATA_KEY, null);
                    mSourceContacts.remove(matchedContact);

                    // Create a new SourceContact based on this MessageEvent
                    // and set its UI contact to be the old SourceContact's UI
                    // contact.  Then add the new SourceContact to the list of
                    // query results.
                    MessageHistorySourceContact newContact =
                        new MessageHistorySourceContact(
                            MessageHistoryContactSource.this, evt);
                    newContact.setData(UI_CONTACT_DATA_KEY, uiContact);
                    mSourceContacts.add(newContact);
                    messageHistoryService.fireSourceContactUpdated(newContact,
                                                                   isRefresh);
                }
                else if (matchedContact == null)
                {
                    // There is no matching SourceContact so we need to create
                    // a new SourceContact, but only if
                    // 1. the MessageEvent matches the current query string (if any).
                    // 2. the MessageEvent is not too old
                    boolean eventMatches = true;
                    if ((mQueryString != null) && mQueryString.length() > 0)
                    {
                        eventMatches =
                            messageHistoryService.eventMatchesQuery(
                                evt, mQueryString.toLowerCase());
                    }

                    boolean eventTooOld =
                        !mSourceContacts.isEmpty() &&
                        mSourceContacts.last().getTimestamp().after(evt.getTimestamp());

                    if (eventMatches && !eventTooOld)
                    {
                        MessageHistorySourceContact newContact =
                            new MessageHistorySourceContact(
                                MessageHistoryContactSource.this, evt);
                        mSourceContacts.add(newContact);
                        messageHistoryService.fireSourceContactAdded(newContact);
                    }
                }
            }
        }
    }
}
