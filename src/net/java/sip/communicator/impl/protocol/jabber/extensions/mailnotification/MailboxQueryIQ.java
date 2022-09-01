/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.mailnotification;

import org.jivesoftware.smack.packet.IQ;

import net.java.sip.communicator.util.Logger;

/**
 * A straightforward <tt>IQ</tt> extension. The <tt>QueryNotify</tt> object is
 * used to create queries for the Gmail mail server. It creates a simple
 * <tt>IQ</tt> packet which represents the query.
 *
 * @author Matthieu Helleringer
 * @author Alain Knaebel
 * @author Emil Ivov
 */
public class MailboxQueryIQ extends IQ
{
    /**
     * Logger for this class
     */
    private static final Logger logger =
        Logger.getLogger(MailboxQueryIQ.class);

    /**
     * The name space for new mail notification packets.
     */
    public static final String NAMESPACE = "google:mail:notify";

    /**
     * The name of the element that contains new mail notification packets.
     */
    public static final String ELEMENT_NAME = "query";

    /**
     * The value of the newer-than-time attribute in this query;
     */
    private long newerThanTime = -1;

    /**
     * The value of the newer-than-tid attribute in this query;
     */
    private long newerThanTid = -1;

    /**
     * Constructor.
     */
    public MailboxQueryIQ()
    {
        super(ELEMENT_NAME, NAMESPACE);
    }

    /**
     * Returns the sub-element XML section of the IQ packet.
     *
     * @return the child element section of the IQ XML.
     */
    @Override
    public IQChildElementXmlStringBuilder getIQChildElementBuilder(
        IQChildElementXmlStringBuilder xml)
    {
        logger.debug("QueryNotify.getChildElementXML usage");

        if(getNewerThanTime() != -1)
            xml.attribute("newer-than-time", String.valueOf(getNewerThanTime()));

        if(getNewerThanTid() != -1)
            xml.attribute("newer-than-tid", String.valueOf(getNewerThanTid()));

        xml.setEmptyElement();

        return xml;
    }

    /**
     * Sets the value of the "newer-than-time" attribute. The value indicates
     * the time of the oldest unread email to retrieve, in milliseconds since
     * the UNIX epoch (00:00:00 UTC, January 1 1970). When querying for the
     * first time, you should omit this attribute (i.e. not call this method or
     * call it with a <tt>-1</tt> value) to return a set of the most recent
     * unread mail. The sever will return only unread mail received after this
     * time. If using this attribute, you should also use newer-than-tid for
     * best results.
     *
     * @param newerThanTime the time of the oldest unread email to retrieve or
     * <tt>-1</tt> if the newer-than-time attribute should be omitted.
     */
    public void setNewerThanTime(long newerThanTime)
    {
        this.newerThanTime = newerThanTime;
    }

    /**
     * Returns the value of the "newer-than-time" attribute. The value indicates
     * the time of the oldest unread email to retrieve, in milliseconds since
     * the UNIX epoch (00:00:00 UTC, January 1 1970). When querying for the
     * first time, you should omit this attribute (i.e. not call this method or
     * call it with a <tt>-1</tt> value) to return a set of the most recent
     * unread mail. The sever will return only unread mail received after this
     * time. If using this attribute, you should also use newer-than-tid for
     * best results.
     *
     * @return the time of the oldest unread email to retrieve or <tt>-1</tt> if
     * the attribute is to be omitted.
     */
    public long getNewerThanTime()
    {
        return this.newerThanTime;
    }

    /**
     * Sets the value of the "newer-than-tid" attribute. The value indicates
     * the highest thread number of messages to return, where higher numbers are
     * more recent email threads. The server will return only threads newer than
     * that specified by this attribute. If using this attribute, you should
     * also use newer-than-time for best results. When querying for the first
     * time, you should omit this value.
     *
     * @param newerThanTid the time of the oldest unread email to retrieve or
     * <tt>-1</tt> if the newer-than-time attribute should be omitted.
     */
    public void setNewerThanTid(long newerThanTid)
    {
        this.newerThanTid = newerThanTid;
    }

    /**
     * Returns the value of the "newer-than-tid" attribute. The value indicates
     * the highest thread number of messages to return, where higher numbers are
     * more recent email threads. The server will return only threads newer than
     * that specified by this attribute. If using this attribute, you should
     * also use newer-than-time for best results. When querying for the first
     * time, you should omit this value.
     *
     * @return the time of the oldest unread email to retrieve or <tt>-1</tt> if
     * the newer-than-time attribute is to be omitted.
     */
    public long getNewerThanTid()
    {
        return this.newerThanTid;
    }
}
