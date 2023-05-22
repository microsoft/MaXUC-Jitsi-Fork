/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import net.java.sip.communicator.util.*;

/**
 * Represents a default implementation of {@link ImMessage} in order to make it
 * easier for implementers to provide complete solutions while focusing on
 * implementation-specific details.
 *
 * @author Lubomir Marinov
 */
public abstract class AbstractMessage
    implements ImMessage
{
    /**
     * The <tt>Logger</tt> used by the <tt>AbstractMessage</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(AbstractMessage.class);

    private static boolean equals(String a, String b)
    {
        return Objects.equals(a, b);
    }

    private String content;

    private final String contentType;

    private String encoding;

    private final String messageUID;

    /**
     * The content of this message, in raw bytes according to the encoding.
     */
    private byte[] rawData;

    private final String subject;

    protected AbstractMessage(String content, String contentType,
        String encoding, String subject, String messageUID)
    {
        this.contentType = contentType;
        this.subject = subject;

        setEncoding(encoding);
        setContent(content);

        this.messageUID = messageUID == null ? createMessageUID() : messageUID;
    }

    protected AbstractMessage(String content, String contentType,
        String encoding, String subject)
    {
        this(content, contentType, encoding, subject, null);
    }

    protected String createMessageUID()
    {
        return UUID.randomUUID().toString();
    }

    /**
     * Returns the content of this message if representable in text form or null
     * if this message does not contain text data.
     * <p>
     * The implementation is final because it caches the raw data of the
     * content.
     * </p>
     *
     * @return a String containing the content of this message or null if the
     *         message does not contain data representable in text form.
     */
    public final String getContent()
    {
        return content;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.java.sip.communicator.service.protocol.Message#getContentType()
     */
    public String getContentType()
    {
        return contentType;
    }

    /**
     * Returns the MIME content encoding of this message.
     * <p>
     * The implementation is final because of the presumption it can set the
     * encoding.
     * </p>
     *
     * @return a String indicating the MIME encoding of this message.
     */
    public final String getEncoding()
    {
        return encoding;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.java.sip.communicator.service.protocol.Message#getMessageUID()
     */
    public String getMessageUID()
    {
        return messageUID;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.java.sip.communicator.service.protocol.Message#getRawData()
     */
    public byte[] getRawData()
    {
        if (rawData == null)
        {
            String content = getContent();
            String encoding = getEncoding();
            boolean useDefaultEncoding = true;
            if (encoding != null)
            {
                try
                {
                    rawData = content.getBytes(encoding);
                    useDefaultEncoding = false;
                }
                catch (UnsupportedEncodingException ex)
                {
                    logger.warn(
                        "Failed to get raw data from content using encoding "
                            + encoding, ex);

                    // We'll use the default encoding
                }
            }
            if (useDefaultEncoding)
            {
                setEncoding(Charset.defaultCharset().name());
                rawData = content.getBytes();
            }
        }
        return rawData;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.java.sip.communicator.service.protocol.Message#getSize()
     */
    public int getSize()
    {
        return getRawData().length;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.java.sip.communicator.service.protocol.Message#getSubject()
     */
    public String getSubject()
    {
        return subject;
    }

    @Override
    public boolean isArchive()
    {
        // Most protocols don't support archive messages, thus this is false in
        // the default implementation
        return false;
    }

    @Override
    public boolean isOffline()
    {
        return false;
    }

    @Override
    public boolean isCarbon()
    {
        // Most protocols don't support carbon messages, so this is false for the
        // default implementation.
        return false;
    }

    protected void setContent(String content)
    {
        if (!equals(this.content, content))
        {
            this.content = content;
            this.rawData = null;
        }
    }

    private void setEncoding(String encoding)
    {
        if (!equals(this.encoding, encoding))
        {
            this.encoding = encoding;
            this.rawData = null;
        }
    }
}
