/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat;

import static org.jitsi.util.Hasher.logHasher;

import java.util.*;

import org.jitsi.service.resources.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * An implementation of the <tt>ChatSession</tt> interface that represents an
 * SMS-only chat session.
 */
public class SMSChatSession extends ChatSession
{
    private static final ContactLogger contactLogger = ContactLogger.getLogger();

    /**
     * The chat transport used by this chat session.
     */
    private ChatTransport mChatTransport;

    /**
     * The SMS number of the remote part in this chat session.
     */
    private String mSmsNumber;

    /**
     * The ChatSessionRenderer that links the chat session to its ChatPanel.
     */
    private final ChatSessionRenderer mSessionRenderer;

    /**
     * The MetaContactListService.
     */
    private final MetaContactListService mMetaContactListService;

    /**
     * A list of listeners registered to receive events when the chat
     * transports associated with this chat session change.
     */
    private final List<ChatSessionChangeListener>
        mChatTransportChangeListeners = new Vector<>();

    /**
     * Creates an instance of <tt>SMSChatSession</tt> by specifying the
     * renderer, which gives the connection with the UI and the SMS number
     * corresponding to the session.
     *
     * @param sessionRenderer the renderer, which gives the connection with the UI.
     * @param smsNumber the SMS number corresponding to the session.
     */
    public SMSChatSession(ChatSessionRenderer sessionRenderer, String smsNumber)
    {
        mSessionRenderer = sessionRenderer;
        mSmsNumber = smsNumber;

        contactLogger.info("Creating SMS chat session with " + smsNumber);

        // Obtain the MetaContactListService and add this class to it as a
        // listener of all events concerning the contact list.
        mMetaContactListService = GuiActivator.getContactListService();

        if (mMetaContactListService != null)
            mMetaContactListService.addMetaContactListListener(this);

        synchronized (chatParticipants)
        {
            chatParticipants.add(new SMSChatContact(smsNumber));
        }

        addSmsListeners();
    }

    @Override
    public String getChatName()
    {
        return mSmsNumber;
    }

    @Override
    public Collection<Object> getHistory(int count)
    {
        final MessageHistoryService messageHistory
            = GuiActivator.getMessageHistoryService();

        // If the MessageHistoryService is not registered we have nothing to do
        // here. The history could be "disabled" by the user via one of the
        // configuration forms.
        if (messageHistory == null)
            return null;

        return new ArrayList<>(messageHistory.findLast(mSmsNumber, count));
    }

    /**
     * Returns the start date of the history of this chat session.
     *
     * @return the start date of the history of this chat session.
     */
    @Override
    public Date getHistoryStartDate()
    {
        Date startHistoryDate = new Date(0);

        MessageHistoryService messageHistory
            = GuiActivator.getMessageHistoryService();

        // If the MessageHistoryService is not registered we have nothing to do
        // here. The history could be "disabled" from the user
        // through one of the configuration forms.
        if (messageHistory == null)
            return startHistoryDate;

        Collection<MessageEvent> firstMessage =
            messageHistory.findFirstMessagesAfter(mSmsNumber, new Date(0), 1);

        if (firstMessage.size() > 0)
        {
            Iterator<MessageEvent> i = firstMessage.iterator();

            MessageEvent evt = i.next();

            startHistoryDate = evt.getTimestamp();
        }

        return startHistoryDate;
    }

    /**
     * Returns the end date of the history of this chat session.
     *
     * @return the end date of the history of this chat session.
     */
    @Override
    public Date getHistoryEndDate()
    {
        Date endHistoryDate = new Date(0);

        MessageHistoryService messageHistory
            = GuiActivator.getMessageHistoryService();

        // If the MessageHistoryService is not registered we have nothing to do
        // here. The history could be "disabled" by the user via one of the
        // configuration forms.
        if (messageHistory == null)
            return endHistoryDate;

        Collection<MessageEvent> lastMessage =
            messageHistory.findLastMessagesBefore(
                                       mSmsNumber, new Date(Long.MAX_VALUE), 1);

        if (lastMessage.size() > 0)
        {
            Iterator<MessageEvent> i = lastMessage.iterator();

            MessageEvent evt = i.next();

            endHistoryDate = evt.getTimestamp();
        }

        return endHistoryDate;
    }

    /**
     * @return null as this session doesn't support sending SMS via the SMS OpSet.
     */
    @Override
    public String getDefaultSmsNumber()
    {
        return null;
    }

    /**
     * Just returns without doing anything as this session doesn't support
     * sending SMS via the SMS OpSet.
     */
    @Override
    public void setDefaultSmsNumber(String smsPhoneNumber)
    {
    }

    @Override
    public ChatTransport getCurrentChatTransport()
    {
        return mChatTransport;
    }

    @Override
    public void setCurrentChatTransport(ChatTransport chatTransport)
    {
        this.mChatTransport = chatTransport;
        synchronized (mChatTransportChangeListeners)
        {
            for (ChatSessionChangeListener l : mChatTransportChangeListeners)
                l.currentChatTransportChanged(this);
        }
    }

    @Override
    public void dispose()
    {
        removeSmsListeners();
    }

    @Override
    public ChatSessionRenderer getChatSessionRenderer()
    {
        return mSessionRenderer;
    }

    @Override
    public Object getDescriptor()
    {
        return mSmsNumber;
    }

    /**
     * @return false as SMSChatSessions are only used for SMS conversations
     *               with non-contacts.
     */
    @Override
    public boolean isDescriptorPersistent()
    {
        return false;
    }

    /**
     * @return null as there is no contact so no avatar.
     */
    @Override
    public BufferedImageFuture getChatAvatar()
    {
        return null;
    }

    @Override
    public boolean isContactListSupported()
    {
        return false;
    }

    @Override
    public void addChatTransportChangeListener(ChatSessionChangeListener l)
    {
        synchronized (mChatTransportChangeListeners)
        {
            if (!mChatTransportChangeListeners.contains(l))
                mChatTransportChangeListeners.add(l);
        }
    }

    @Override
    public void removeChatTransportChangeListener(ChatSessionChangeListener l)
    {
        synchronized (mChatTransportChangeListeners)
        {
            mChatTransportChangeListeners.remove(l);
        }
    }

    @Override
    public void metaContactAdded(MetaContactEvent evt)
    {
        // A new MetaContact has been added.  If it contains the phone number
        // of this SMS chat session, replace this chat session with a
        // MetaContactChatSession.
        MetaContact newMetaContact = evt.getSourceMetaContact();
        Set<String> smsNumbers = newMetaContact.getSmsNumbers();

        for (String smsNumber : smsNumbers)
        {
            if (mSmsNumber.equalsIgnoreCase(smsNumber) &&
                (mSessionRenderer instanceof ChatPanel))
            {
                contactLogger.debug("Replacing SMS chat session for " + logHasher(mSmsNumber) +
                                    " with MetaContactChatSession for " + newMetaContact);
                Contact imContact = newMetaContact.getIMContact();
                MetaContactChatSession chatSession =
                    new MetaContactChatSession(mSessionRenderer,
                                               newMetaContact,
                                               imContact,
                                               null);

                ChatPanel chatPanel = (ChatPanel) mSessionRenderer;
                chatPanel.removeChatTransport(mChatTransport);
                chatPanel.setChatSession(chatSession);

                ChatContainer chatWindow = chatPanel.getChatContainer();
                if (chatWindow != null)
                {
                    chatWindow.updateContainer(chatPanel);
                }
            }
        }
    }

    @Override
    protected void addSMSChatTransports()
    {
        contactLogger.info("Adding SMSChatTransport for " + mSmsNumber);
        mChatTransport = new SMSChatTransport(mSmsNumber);

        synchronized (chatTransports)
        {
            chatTransports.add(mChatTransport);
        }

        mSessionRenderer.addChatTransport(mChatTransport);
        mSessionRenderer.setSelectedChatTransport(mChatTransport);
    }

    @Override
    protected void removeSMSChatTransports()
    {
        contactLogger.info("Removing SMSChatTransport for " + mSmsNumber);
        if (mChatTransport != null)
        {
            synchronized (chatTransports)
            {
                chatTransports.remove(mChatTransport);
            }

            mSessionRenderer.removeChatTransport(mChatTransport);
            mChatTransport = null;
        }
    }
}
