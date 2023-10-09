/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.xdata.form.FillableForm;
import org.jivesoftware.smackx.xdata.form.Form;

import net.java.sip.communicator.service.protocol.ChatRoomConfigurationForm;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.util.Logger;

/**
 * The Jabber implementation of the <tt>ChatRoomConfigurationForm</tt>
 * interface.
 *
 * @author Yana Stamcheva
 */
public class ChatRoomConfigurationFormJabberImpl
    implements ChatRoomConfigurationForm
{
    /**
     * The logger of this class.
     */
    private Logger logger
        = Logger.getLogger(ChatRoomConfigurationFormJabberImpl.class);

    /**
     * The smack chat room configuration form.
     */
    private Form smackConfigForm;

    /**
     * The form that will be filled out and submitted by user.
     */
    private FillableForm smackSubmitForm;

    /**
     * The smack multi user chat is the one to which we'll send the form once
     * filled out.
     */
    private MultiUserChat smackMultiUserChat;

    /**
     * Creates an instance of <tt>ChatRoomConfigurationFormJabberImpl</tt> by
     * specifying the corresponding smack multi user chat and smack
     * configuration form.
     *
     * @param multiUserChat the smack multi user chat, to which we'll send the
     * configuration form once filled out
     * @param smackConfigForm the smack configuration form
     */
    public ChatRoomConfigurationFormJabberImpl(
        MultiUserChat multiUserChat, Form smackConfigForm)
    {
        this.smackMultiUserChat = multiUserChat;
        this.smackConfigForm = smackConfigForm;
        this.smackSubmitForm = smackConfigForm.getFillableForm();
    }

    /**
     * Sends the ready smack configuration form to the multi user chat.
     */
    public void submit()
        throws OperationFailedException
    {
        logger.trace("Sends chat room configuration form to the server.");

        try
        {
            smackMultiUserChat.sendConfigurationForm(smackSubmitForm);
        }
        catch (NoResponseException | InterruptedException |
            NotConnectedException | XMPPErrorException e)
        {
            logger.error("Failed to submit the configuration form.", e);

            throw new OperationFailedException(
                "Failed to submit the configuration form.",
                OperationFailedException.GENERAL_ERROR);
        }
    }
}
