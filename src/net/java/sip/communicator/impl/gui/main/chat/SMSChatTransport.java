/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import static org.jitsi.util.Hasher.logHasher;

import java.io.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications.TypingState;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

/**
 * An implementation of the <tt>ChatTransport</tt> interface that allows
 * sending of SMS without requiring a contact.
 */
public class SMSChatTransport implements ChatTransport
{
    /**
     * The logger.
     */
    private static final Logger sLog = Logger.getLogger(SMSChatTransport.class);

    /**
     * The parent <tt>ChatSession</tt>, where this transport is available.
     */
    private final ChatSession mParentChatSession;

    /**
     * The SMS number.
     */
    private final String mSmsNumber;

    /**
     * The <tt>ProtocolProviderService</tt> that supports
     * <tt>OperationSetBasicInstantMessaging</tt>, which is required to send SMS.
     */
    private ProtocolProviderService mImProvider;

    /**
     * The IM operation set.
     */
    private OperationSetBasicInstantMessaging mImOpSet;

    /**
     * Creates an instance of <tt>SMSChatTransport</tt> by specifying
     * the parent <tt>chatSession</tt> and the SMS number associated with
     * the transport.
     *
     * @param chatSession the parent <tt>ChatSession</tt>
     * @param smsNumber the SMS number associated with this transport
     */
    public SMSChatTransport(ChatSession chatSession, String smsNumber)
    {
        sLog.info(
            "Creating new SMSChatTransport for " + smsNumber + " in " + chatSession);
        mParentChatSession = chatSession;
        mSmsNumber = smsNumber;

        mImProvider = AccountUtils.getImProvider();

        if (mImProvider != null)
        {
            mImOpSet = mImProvider.getOperationSet(
                                       OperationSetBasicInstantMessaging.class);
        }
    }

    public String getName()
    {
        return mSmsNumber;
    }

    public String getDisplayName()
    {
        return mSmsNumber;
    }

    /**
     * @return unknown
     */
    public PresenceStatus getStatus()
    {
        return GlobalStatusEnum.UNKNOWN;
    }

    /**
     * Returns the <tt>ProtocolProviderService</tt> that supports
     * <tt>OperationSetBasicInstantMessaging</tt>, which is required to send SMS.
     *
     * @return the <tt>ProtocolProviderService</tt> that supports
     * <tt>OperationSetBasicInstantMessaging</tt>, which is required to send SMS.
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return mImProvider;
    }

    public boolean allowsInstantMessage()
    {
        // If we have an IM provider, we allow IM.
        return true;
    }

    /**
     * Sends the given instant message through this chat transport,
     * by specifying the mime type (html or plain text).
     *
     * @param message The message to send.
     * @param mimeType The mime type of the message to send: text/html or
     * text/plain.
     */
    public void sendInstantMessage(String message, String mimeType)
    {
        if (mImOpSet == null)
        {
            mImProvider = AccountUtils.getImProvider();
            mImOpSet = (mImProvider != null) ?
                            mImProvider.getOperationSet(
                                    OperationSetBasicInstantMessaging.class) :
                            null;

            if (mImOpSet == null)
            {
                sLog.error("Failed to send IM as could not find op set");
                return;
            }
        }

        // If this chat transport does not support instant messaging we do
        // nothing here.
        if (!allowsInstantMessage())
            return;

        ImMessage msg;
        if (mimeType.equals(OperationSetBasicInstantMessaging.HTML_MIME_TYPE)
            && mImOpSet.isContentTypeSupported(
                OperationSetBasicInstantMessaging.HTML_MIME_TYPE))
        {
            msg = mImOpSet.createMessage(message,
                OperationSetBasicInstantMessaging.HTML_MIME_TYPE, "utf-8", "");
        }
        else
        {
            msg = mImOpSet.createMessage(message);
        }

        sLog.debug("Sending SMS to " + mSmsNumber);
        mImOpSet.sendInstantMessage(mSmsNumber, msg);
    }

    public boolean isContentTypeSupported(String contentType)
    {
        boolean isSupported = false;

        if (mImOpSet != null)
        {
            isSupported = mImOpSet.isContentTypeSupported(contentType);
        }

        return isSupported;
    }

    public ChatSession getParentChatSession()
    {
        return mParentChatSession;
    }

    public void addInstantMessageListener(MessageListener l)
    {
        if (mImOpSet != null)
        {
            sLog.debug("Adding IM listener: " + l);
            mImOpSet.addMessageListener(l);
        }
    }

    public void removeInstantMessageListener(MessageListener l)
    {
        if (mImOpSet != null)
        {
            sLog.debug("Removing IM listener: " + l);
            mImOpSet.removeMessageListener(l);
        }
    }

    public Object getDescriptor()
    {
        return mSmsNumber;
    }

    public boolean equals(Object obj)
    {
        boolean isEqual = false;

        if ((obj != null) && (obj.getClass() == this.getClass()))
        {
            isEqual = ((SMSChatTransport) obj).getDescriptor().equals(getDescriptor());
        }

        return isEqual;
    }

    /**
     * @return null as SMS transport has no resource
     */
    public String getResourceName()
    {
        return null;
    }

    /**
     * @return false
     */
    public boolean isDisplayResourceOnly()
    {
        return false;
    }

    /**
     * @return false as SMS doesn't allow correction.
     */
    public boolean allowsMessageCorrections()
    {
        return false;
    }

    /**
     * @return false as we cannot do SMS using the SMS Op Set.
     */
    public boolean allowsSmsMessage()
    {
        return false;
    }

    /**
     * @return false as SMS doesn't allow typing notifications.
     */
    public boolean allowsTypingNotifications()
    {
        return false;
    }

    /**
     * @return false as SMS doesn't allow file transfer
     */
    public boolean allowsFileTransfer()
    {
        return false;
    }

    /**
     * Just returns as SMS doesn't allow message correction.
     */
    public void correctInstantMessage(String message, String mimeType,
            String correctedMessageUID)
    {
    }

    /**
     * Just returns as SMS doesn't support this.
     */
    public void inviteChatContact(String contactAddress, String reason)
    {
    }

    /**
     * Just returns as SMS doesn't allow SMS via the SMS Op Set.
     */
    public void sendSmsMessage(String phoneNumber, String messageText)
    {
    }

    /**
     * Just returns as SMS doesn't allow SMS via the SMS Op Set.
     */
    public void sendSmsMessage(Contact contact, String message)
    {
    }

    /**
     * SMS doesn't support typing notifications.
     */
    public void sendTypingNotification(TypingState typingState)
    {
    }

    /**
     * Just returns null as SMS doesn't support file transfer.
     */
    public FileTransfer sendFile(File file, String id)
    {
        return null;
    }

    /**
     * Just returns -1 as SMS doesn't support file transfer.
     */
    public long getMaximumFileLength()
    {
        return -1;
    }

    public void dispose()
    {
    }

    /**
     * Just returns as SMS doesn't allow SMS via the SMS Op Set.
     * @param l The message listener to add.
     */
    public void addSmsMessageListener(MessageListener l)
    {
    }

    /**
     * Just returns as SMS doesn't allow SMS via the SMS Op Set.
     *
     * @param l The message listener to remove.
     */
    public void removeSmsMessageListener(MessageListener l)
    {
    }

    @Override
    public void join() throws OperationFailedException
    {
        throw new OperationFailedException(
            "SMSChatTransport does not support joining",
            OperationFailedException.NOT_SUPPORTED_OPERATION);
    }

    @Override
    public String toString()
    {
        return "<" + super.toString() + ", SMS Number = " +
               logHasher(mSmsNumber) + ">";
    }
}
