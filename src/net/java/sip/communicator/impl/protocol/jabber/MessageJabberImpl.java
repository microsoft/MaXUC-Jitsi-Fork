/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.protocol.AbstractMessage;

/**
 * A simple implementation of the <tt>Message</tt> interface. Right now the
 * message only supports test contents and no binary data.
 *
 * @author Damian Minkov
 * @author Lubomir Marinov
 */
public class MessageJabberImpl extends AbstractMessage
{
    /**
     * True if this message is for an archive message
     */
    private final boolean isArchive;

    /**
     * True if this message is an offline message
     */
    private final boolean isOffline;

    /**
     * True if this message is a carbon message (XEP-0280)
     */
    private final boolean isCarbon;

    /**
     * Creates an instance of this Message with the specified parameters.
     *
     * @param content the text content of the message.
     * @param contentType a MIME string indicating the content type of the
     *            <tt>content</tt> String.
     * @param contentEncoding a MIME String indicating the content encoding of
     *            the <tt>content</tt> String.
     * @param subject the subject of the message or null for empty.
     * @param isArchive true if this is an archive message
     * @param isOffline true if this is an offline message
     * @param isCarbon  true if this is a carbon message (XEP-0280)
     */
    public MessageJabberImpl(String content,
                             String contentType,
                             String contentEncoding,
                             String subject,
                             boolean isArchive,
                             boolean isOffline,
                             boolean isCarbon)
    {
        super(content, contentType, contentEncoding, subject);
        this.isArchive = isArchive;
        this.isOffline = isOffline;
        this.isCarbon = isCarbon;
    }

    /**
     * Creates an instance of this Message with the specified parameters.
     *
     * @param content the text content of the message.
     * @param contentType a MIME string indicating the content type of the
     *            <tt>content</tt> String.
     * @param contentEncoding a MIME String indicating the content encoding of
     *            the <tt>content</tt> String.
     * @param subject the subject of the message or null for empty.
     * @param messageUID the UID of the message.
     * @param isArchive true if this is an archive message
     * @param isOffline true if this is an offline message
     * @param isCarbon  true if this is a carbon message (XEP-0280)
     */
    public MessageJabberImpl(String content,
                             String contentType,
                             String contentEncoding,
                             String subject,
                             String messageUID,
                             boolean isArchive,
                             boolean isOffline,
                             boolean isCarbon)
    {
        super(content, contentType, contentEncoding, subject, messageUID);
        this.isArchive = isArchive;
        this.isOffline = isOffline;
        this.isCarbon = isCarbon;
    }

    @Override
    public boolean isArchive()
    {
        return isArchive;
    }

    @Override
    public boolean isOffline()
    {
        return isOffline;
    }

    @Override
    public boolean isCarbon()
    {
        return isCarbon;
    }

    /**
     * Workaround for problems with ASCII control characters: remove them
     * (except 0009-000D, namely horizontal tab, line feed, vertical tab, form
     * feed and carriage return).
     *
     * Without this fix, we see two problems:
     * - The XMPP connection drops and restarts.
     * - Exceptions in the XML parsing in the HistoryWriter.
     *
     * This is a pragmatic fix - an ideal solution would XML-escape the
     * content before storing and before passing to the protocol layer to
     * send the message.
     *
     * @param content the text content of the message.
     */
    @Override
    protected void setContent(String content)
    {
        super.setContent(content != null ?
            content.replaceAll("[\\x{0000}-\\x{0008}]|[\\x{000e}-\\x{001f}]|\\x{007f}", "") :
            null);
    }
}
