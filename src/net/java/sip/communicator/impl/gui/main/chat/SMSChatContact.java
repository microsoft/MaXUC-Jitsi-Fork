/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat;

import org.jitsi.service.resources.*;

/**
 * The <tt>SMSChatContact</tt> represents a <tt>ChatContact</tt> in an SMS-only
 * chat.
 */
public class SMSChatContact extends ChatContact<String>
{
    /**
     * Creates an instance of <tt>ChatContact</tt> by passing to it the
     * corresponding SMS number.
     *
     * @param smsNumber the SMS number of the remote party in the chat.
     */
    public SMSChatContact(String smsNumber)
    {
        super(smsNumber);
    }

    /**
     * @return null as there is no contact associated with an SMS chat session,
     *              therefore no avatar.
     */
    public BufferedImageFuture getAvatarBytes()
    {
        return null;
    }

    /**
     * @return the SMS number
     */
    public String getName()
    {
        return descriptor;
    }

    /**
     * @return the SMS number
     */
    public String getUID()
    {
        return descriptor;
    }
}
