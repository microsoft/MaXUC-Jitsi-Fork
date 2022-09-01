/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.analytics.AnalyticsService;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications.TypingState;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * The single chat implementation of the <tt>ChatTransport</tt> interface that
 * provides abstraction to protocol provider access.
 *
 * @author Yana Stamcheva
 */
public class MetaContactChatTransport
    implements  ChatTransport,
                ContactPresenceStatusListener
{
    /**
     * The logger.
     */
    private static final Logger sLog =
        Logger.getLogger(MetaContactChatTransport.class);

    /**
     * The parent <tt>ChatSession</tt>, where this transport is available.
     */
    private final MetaContactChatSession mParentChatSession;

    /**
     * The associated protocol <tt>Contact</tt>.
     */
    private final Contact mContact;

    /**
     * The resource associated with this contact.
     */
    private ContactResource mContactResource;

    /**
     * The protocol presence operation set associated with this transport.
     */
    private final OperationSetPresence mPresenceOpSet;

    /**
     * The thumbnail default width.
     */
    private static final int THUMBNAIL_WIDTH = 64;

    /**
     * The thumbnail default height.
     */
    private static final int THUMBNAIL_HEIGHT = 64;

    /**
     * Indicates if only the resource name should be displayed.
     */
    private boolean mIsDisplayResourceOnly = false;

    /**
     * Creates an instance of <tt>MetaContactChatTransport</tt> by specifying
     * the parent <tt>chatSession</tt> and the <tt>contact</tt> associated with
     * the transport.
     *
     * @param chatSession the parent <tt>ChatSession</tt>
     * @param contact the <tt>Contact</tt> associated with this transport
     */
    public MetaContactChatTransport(MetaContactChatSession chatSession,
                                    Contact contact)
    {
        this(chatSession, contact, null, false);
    }

    /**
     * Creates an instance of <tt>MetaContactChatTransport</tt> by specifying
     * the parent <tt>chatSession</tt> and the <tt>contact</tt> associated with
     * the transport.
     *
     * @param chatSession the parent <tt>ChatSession</tt>
     * @param contact the <tt>Contact</tt> associated with this transport
     * @param contactResource the <tt>ContactResource</tt> associated with the
     * contact
     * @param isDisplayResourceOnly indicates if only the resource name should
     * be displayed
     */
    public MetaContactChatTransport(MetaContactChatSession chatSession,
                                    Contact contact,
                                    ContactResource contactResource,
                                    boolean isDisplayResourceOnly)
    {
        mParentChatSession = chatSession;
        mContact = contact;
        mContactResource = contactResource;
        mIsDisplayResourceOnly = isDisplayResourceOnly;

        mPresenceOpSet = mContact.getProtocolProvider()
            .getOperationSet(OperationSetPresence.class);

        if (mPresenceOpSet != null)
            mPresenceOpSet.addContactPresenceStatusListener(this);
    }

    /**
     * Returns the contact associated with this transport.
     *
     * @return the contact associated with this transport
     */
    public Contact getContact()
    {
        return mContact;
    }

    /**
     * Returns the contact address corresponding to this chat transport.
     *
     * @return The contact address corresponding to this chat transport.
     */
    public String getName()
    {
        return mContact.getAddress();
    }

    /**
     * Returns the display name corresponding to this chat transport.
     *
     * @return The display name corresponding to this chat transport.
     */
    public String getDisplayName()
    {
        return mContact.getDisplayName();
    }

    /**
     * Returns the resource name of this chat transport. This is for example the
     * name of the user agent from which the contact is logged.
     *
     * @return The display name of this chat transport resource.
     */
    public String getResourceName()
    {
        if (mContactResource != null)
            return mContactResource.getResourceName();

        return null;
    }

    public boolean isDisplayResourceOnly()
    {
        return mIsDisplayResourceOnly;
    }

    /**
     * Returns the presence status of this transport.
     *
     * @return the presence status of this transport.
     */
    public PresenceStatus getStatus()
    {
        if (mContactResource != null)
            return mContactResource.getPresenceStatus();
        else
            return mContact.getPresenceStatus();
    }

    /**
     * Returns the <tt>ProtocolProviderService</tt>, corresponding to this chat
     * transport.
     *
     * @return the <tt>ProtocolProviderService</tt>, corresponding to this chat
     * transport.
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return mContact.getProtocolProvider();
    }

    /**
     * Returns <code>true</code> if this chat transport supports instant
     * messaging, otherwise returns <code>false</code>.
     *
     * @return <code>true</code> if this chat transport supports instant
     * messaging, otherwise returns <code>false</code>.
     */
    public boolean allowsInstantMessage()
    {
        // First try to ask the capabilities operation set if such is
        // available.
        OperationSetContactCapabilities capOpSet = getProtocolProvider()
            .getOperationSet(OperationSetContactCapabilities.class);

        if (capOpSet != null)
        {
            if (capOpSet.getOperationSet(
                mContact, OperationSetBasicInstantMessaging.class) != null)
            {
                return true;
            }
        }
        else if (mContact.getProtocolProvider()
            .getOperationSet(OperationSetBasicInstantMessaging.class) != null)
            return true;

        return false;
    }

    /**
     * Returns <code>true</code> if this chat transport supports message
     * corrections and false otherwise.
     *
     * @return <code>true</code> if this chat transport supports message
     * corrections and false otherwise.
     */
    public boolean allowsMessageCorrections()
    {
        OperationSetContactCapabilities capOpSet = getProtocolProvider()
                .getOperationSet(OperationSetContactCapabilities.class);

        if (capOpSet != null)
        {
            return capOpSet.getOperationSet(
                    mContact, OperationSetMessageCorrection.class) != null;
        }
        else
        {
            return mContact.getProtocolProvider().getOperationSet(
                    OperationSetMessageCorrection.class) != null;
        }
    }

    /**
     * Returns <code>true</code> if this chat transport supports sms
     * messaging, otherwise returns <code>false</code>.
     *
     * @return <code>true</code> if this chat transport supports sms
     * messaging, otherwise returns <code>false</code>.
     */
    public boolean allowsSmsMessage()
    {
        // First try to ask the capabilities operation set if such is
        // available.
        OperationSetContactCapabilities capOpSet = getProtocolProvider()
            .getOperationSet(OperationSetContactCapabilities.class);

        if (capOpSet != null)
        {
            if (capOpSet.getOperationSet(
                mContact, OperationSetSmsMessaging.class) != null)
            {
                return true;
            }
        }
        else if (mContact.getProtocolProvider()
            .getOperationSet(OperationSetSmsMessaging.class) != null)
            return true;

        return false;
    }

    /**
     * Returns <code>true</code> if this chat transport supports typing
     * notifications, otherwise returns <code>false</code>.
     *
     * @return <code>true</code> if this chat transport supports typing
     * notifications, otherwise returns <code>false</code>.
     */
    public boolean allowsTypingNotifications()
    {
        return mContact.getProtocolProvider().getOperationSet(OperationSetTypingNotifications.class) != null;
    }

    /**
     * Returns <code>true</code> if this chat transport supports file transfer,
     * otherwise returns <code>false</code>.
     *
     * @return <code>true</code> if this chat transport supports file transfer,
     * otherwise returns <code>false</code>.
     */
    public boolean allowsFileTransfer()
    {
        Object ftOpSet = mContact.getProtocolProvider()
            .getOperationSet(OperationSetFileTransfer.class);

        if (ftOpSet != null)
            return true;
        else
            return false;
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
        // If this chat transport does not support instant messaging we do
        // nothing here.
        if (!allowsInstantMessage())
            return;

        OperationSetBasicInstantMessaging imOpSet
            = mContact.getProtocolProvider()
                .getOperationSet(OperationSetBasicInstantMessaging.class);

        ImMessage msg;
        if (mimeType.equals(OperationSetBasicInstantMessaging.HTML_MIME_TYPE)
            && imOpSet.isContentTypeSupported(
                OperationSetBasicInstantMessaging.HTML_MIME_TYPE))
        {
            msg = imOpSet.createMessage(message,
                OperationSetBasicInstantMessaging.HTML_MIME_TYPE, "utf-8", "");
        }
        else
        {
            msg = imOpSet.createMessage(message);
        }

        if (mContactResource != null)
        {
            sLog.debug("Sending IM to specific resource: " +
                                            mContactResource);
            imOpSet.sendInstantMessage(mContact, mContactResource, msg);
        }
        else
        {
            sLog.debug("Sending IM to the all registered resources for this contact");
            imOpSet.sendInstantMessage(mContact,
                    ContactResource.BASE_RESOURCE, msg);
        }
    }

    /**
     * Sends <tt>message</tt> as a message correction through this transport,
     * specifying the mime type (html or plain text) and the id of the
     * message to replace.
     *
     * @param message The message to send.
     * @param mimeType The mime type of the message to send: text/html or
     * text/plain.
     * @param correctedMessageUID The ID of the message being corrected by
     * this message.
     */
    public void correctInstantMessage(String message, String mimeType,
            String correctedMessageUID)
    {
        if (!allowsMessageCorrections())
        {
            return;
        }

        AnalyticsService analytics = GuiActivator.getAnalyticsService();

        analytics.onEventWithIncrementingCount(AnalyticsEventType.EDIT_IM, new ArrayList<>());

        OperationSetMessageCorrection mcOpSet = mContact.getProtocolProvider()
                .getOperationSet(OperationSetMessageCorrection.class);

        ImMessage msg;
        if (mimeType.equals(OperationSetBasicInstantMessaging.HTML_MIME_TYPE)
                && mcOpSet.isContentTypeSupported(
                OperationSetBasicInstantMessaging.HTML_MIME_TYPE))
        {
            msg = mcOpSet.createMessage(message,
                    OperationSetBasicInstantMessaging.HTML_MIME_TYPE,
                    "utf-8", "");
        }
        else
        {
            msg = mcOpSet.createMessage(message);
        }

        mcOpSet.correctMessage(mContact, mContactResource, msg, correctedMessageUID);
    }

    /**
     * Determines whether this chat transport supports the supplied content type
     *
     * @param contentType the type we want to check
     * @return <tt>true</tt> if the chat transport supports it and
     * <tt>false</tt> otherwise.
     */
    public boolean isContentTypeSupported(String contentType)
    {
        OperationSetBasicInstantMessaging imOpSet
            = mContact.getProtocolProvider()
                .getOperationSet(OperationSetBasicInstantMessaging.class);

        if (imOpSet != null)
            return imOpSet.isContentTypeSupported(contentType);
        else
            return false;
    }

    /**
     * Sends the given sms message trough this chat transport.
     *
     * @param phoneNumber phone number of the destination
     * @param messageText The message to send.
     */
    public void sendSmsMessage(String phoneNumber, String messageText)
    {
        // If this chat transport does not support sms messaging we do
        // nothing here.
        if (!allowsSmsMessage())
            return;

        OperationSetSmsMessaging smsOpSet
            = mContact.getProtocolProvider()
                .getOperationSet(OperationSetSmsMessaging.class);

        ImMessage smsMessage = smsOpSet.createMessage(messageText);

        smsOpSet.sendSmsMessage(phoneNumber, smsMessage);
    }

    /**
     * Sends the given sms message trough this chat transport.
     *
     * @param contact the destination contact
     * @param message the message to send
     */
    public void sendSmsMessage(Contact contact, String message)
    {
        // If this chat transport does not support sms messaging we do
        // nothing here.
        if (!allowsSmsMessage())
            return;

        OperationSetSmsMessaging smsOpSet
            = contact.getProtocolProvider()
                .getOperationSet(OperationSetSmsMessaging.class);

        ImMessage smsMessage = smsOpSet.createMessage(message);

        smsOpSet.sendSmsMessage(contact, smsMessage);
    }

    /**
     * Sends a typing notification state.
     *
     * @param typingState the typing notification state to send
     */
    public void sendTypingNotification(TypingState typingState)
    {
        // If this chat transport does not support sms messaging we do
        // nothing here.
        if (!allowsTypingNotifications())
        {
            return;
        }

        ProtocolProviderService protocolProvider = mContact.getProtocolProvider();
        OperationSetTypingNotifications tnOperationSet
            = protocolProvider.getOperationSet(OperationSetTypingNotifications.class);

        // if protocol is not registered or contact is offline don't
        // try to send typing notifications
        if (protocolProvider.isRegistered() &&
            mContact.getPresenceStatus().getStatus() >= PresenceStatus.ONLINE_THRESHOLD)
        {
            try
            {
                tnOperationSet.sendTypingNotification(mContact, mContactResource, typingState);
            }
            catch (Exception ex)
            {
                sLog.error("Failed to send typing notifications.", ex);
            }
        }
    }

    @Override
    public FileTransfer sendFile(File file, String transferId)
        throws Exception
    {
        // If this chat transport does not support instant messaging we do
        // nothing here.
        if (!allowsFileTransfer())
        {
            return null;
        }

        OperationSetFileTransfer ftOpSet =
            mContact.getProtocolProvider().getOperationSet(OperationSetFileTransfer.class);

        if (FileUtils.isImage(file.getName()))
        {
            // Create a thumbnailed file if possible.
            OperationSetThumbnailedFileFactory tfOpSet =
                mContact.getProtocolProvider()
                    .getOperationSet(OperationSetThumbnailedFileFactory.class);

            if (tfOpSet != null)
            {
                byte[] thumbnail = getFileThumbnail(file);

                if (thumbnail != null && thumbnail.length > 0)
                {
                    file = tfOpSet.createFileWithThumbnail(
                        file, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT,
                        "image/png", thumbnail);
                }
            }
        }
        return ftOpSet.sendFile(mContactResource, mContact, file, transferId);
    }

    /**
     * Returns the maximum file length supported by the protocol in bytes.
     * @return the file length that is supported.
     */
    public long getMaximumFileLength()
    {
        OperationSetFileTransfer ftOpSet =
            mContact.getProtocolProvider().getOperationSet(OperationSetFileTransfer.class);

        return ftOpSet.getMaximumFileLength();
    }

    public void inviteChatContact(String contactAddress, String reason) {}

    /**
     * Returns the parent session of this chat transport. A <tt>ChatSession</tt>
     * could contain more than one transports.
     *
     * @return the parent session of this chat transport
     */
    public ChatSession getParentChatSession()
    {
        return mParentChatSession;
    }

    /**
     * Adds an SMS message listener to this chat transport.
     * @param l The message listener to add.
     */
    public void addSmsMessageListener(MessageListener l)
    {
        // If this chat transport does not support sms messaging we do
        // nothing here.
        if (!allowsSmsMessage())
        {
            return;
        }

        OperationSetSmsMessaging smsOpSet =
            mContact.getProtocolProvider().getOperationSet(OperationSetSmsMessaging.class);
        smsOpSet.addMessageListener(l);
    }

    /**
     * Adds an instant message listener to this chat transport.
     * @param l The message listener to add.
     */
    public void addInstantMessageListener(MessageListener l)
    {
        // If this chat transport does not support instant messaging we do
        // nothing here.
        if (!allowsInstantMessage())
            return;

        OperationSetBasicInstantMessaging imOpSet =
            mContact.getProtocolProvider().getOperationSet(OperationSetBasicInstantMessaging.class);
        imOpSet.addMessageListener(l);
    }

    /**
     * Removes the given sms message listener from this chat transport.
     * @param l The message listener to remove.
     */
    public void removeSmsMessageListener(MessageListener l)
    {
        // If this chat transport does not support sms messaging we do
        // nothing here.
        if (!allowsSmsMessage())
            return;

        OperationSetSmsMessaging smsOpSet
            = mContact.getProtocolProvider()
                .getOperationSet(OperationSetSmsMessaging.class);

        smsOpSet.removeMessageListener(l);
    }

    /**
     * Removes the instant message listener from this chat transport.
     * @param l The message listener to remove.
     */
    public void removeInstantMessageListener(MessageListener l)
    {
        // If this chat transport does not support instant messaging we do
        // nothing here.
        if (!allowsInstantMessage())
            return;

        OperationSetBasicInstantMessaging imOpSet
            = mContact.getProtocolProvider()
                .getOperationSet(OperationSetBasicInstantMessaging.class);

        imOpSet.removeMessageListener(l);
    }

    /**
     * Indicates that a contact has changed its status.
     * @param evt The presence event containing information about the
     * contact status change.
     */
    public void contactPresenceStatusChanged(
                                        ContactPresenceStatusChangeEvent evt)
    {
        if (evt.getSourceContact().equals(mContact))
        {
            updateContactStatus();
        }
    }

    /**
     * Updates the status of this contact with the new given status.
     */
    private void updateContactStatus()
    {
        // Update the status of the given contact in the "send via" selector
        // box.
        mParentChatSession.getChatSessionRenderer()
            .updateChatTransportStatus(this);
    }

    /**
     * Removes all previously added listeners.
     */
    public void dispose()
    {
        if (mPresenceOpSet != null)
            mPresenceOpSet.removeContactPresenceStatusListener(this);
    }

    /**
     * Returns the descriptor of this chat transport.
     * @return the descriptor of this chat transport
     */
    public Object getDescriptor()
    {
        return mContact;
    }

    /**
     * Sets the icon for the given file.
     *
     * @param file the file to set an icon for
     * @return the byte array containing the thumbnail
     */
    private byte[] getFileThumbnail(File file)
    {
        byte[] bytes = null;
        if (FileUtils.isImage(file.getName()))
        {
            try
            {
                ImageIcon image = new ImageIcon(file.toURI().toURL());
                int width = image.getIconWidth();
                int height = image.getIconHeight();

                if (width > THUMBNAIL_WIDTH)
                    width = THUMBNAIL_WIDTH;
                if (height > THUMBNAIL_HEIGHT)
                    height = THUMBNAIL_HEIGHT;

                bytes
                    = ImageUtils
                        .getScaledInstanceInBytes(
                            image.getImage(),
                            width,
                            height);
            }
            catch (MalformedURLException e)
            {
                sLog.debug("Could not locate image.", e);
            }
        }
        return bytes;
    }

    @Override
    public void join() throws OperationFailedException
    {
        throw new OperationFailedException(
            "MetaContactChatTransport does not support joining",
            OperationFailedException.NOT_SUPPORTED_OPERATION);
    }

    @Override
    public String toString()
    {
        return "<" + super.toString() + ", Contact = " + mContact +
               ", Resource = " + mContactResource + ">";
    }
}
