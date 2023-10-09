/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat;

import static net.java.sip.communicator.util.PrivacyUtils.sanitiseDirectoryNumber;
import static org.jitsi.util.Hasher.logHasher;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications.TypingState;
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
     * @param smsNumber the SMS number associated with this transport
     */
    public SMSChatTransport(String smsNumber)
    {
        sLog.info("Creating new SMSChatTransport for " + sanitiseDirectoryNumber(smsNumber));
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
     * Just returns as SMS doesn't support this.
     */
    public void inviteChatContact(String contactAddress, String reason)
    {
    }

    /**
     * SMS doesn't support typing notifications.
     */
    public void sendTypingNotification(TypingState typingState)
    {
    }

    public void dispose()
    {
    }

    @Override
    public String toString()
    {
        return "<" + super.toString() + ", SMS Number = " +
               logHasher(mSmsNumber) + ">";
    }
}
