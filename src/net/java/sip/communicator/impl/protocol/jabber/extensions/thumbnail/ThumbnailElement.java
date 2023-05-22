/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber.extensions.thumbnail;

import org.jivesoftware.smack.util.SHA1;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smackx.bob.ContentId;

/**
 * The <tt>ThumbnailElement</tt> represents a "thumbnail" XML element, that is
 * contained in the file element, we're sending to notify for a file transfer.
 * The <tt>ThumbnailElement</tt>'s role is to advertise a thumbnail.
 *
 * @author Yana Stamcheva
 */
public class ThumbnailElement
{
    /**
     * The name of the XML element used for transport of thumbnail parameters.
     */
    public static final String ELEMENT_NAME = "thumbnail";

    /**
     * The names XMPP space that the thumbnail elements belong to.
     */
    public static final String NAMESPACE = "urn:xmpp:thumbs:0";

    /**
     * The name of the thumbnail attribute "cid".
     */
    public static final String CID = "cid";

    /**
     * The name of the thumbnail attribute "mime-type".
     */
    public static final String MIME_TYPE = "mime-type";

    /**
     * The name of the thumbnail attribute "width".
     */
    public static final String WIDTH = "width";

    /**
     * The name of the thumbnail attribute "height".
     */
    public static final String HEIGHT = "height";

    private ContentId cid;

    private String mimeType;

    private int width;

    private int height;

    /**
     * Creates a <tt>ThumbnailExtensionElement</tt> by specifying all extension
     * attributes.
     *
     * @param thumbnailData the byte array containing the thumbnail data
     * @param mimeType the mime type attribute
     * @param width the width of the thumbnail
     * @param height the height of the thumbnail
     */
    public ThumbnailElement(byte[] thumbnailData,
                            String mimeType,
                            int width,
                            int height)
    {
        this.cid = createCid(thumbnailData);
        this.mimeType = mimeType;
        this.width = width;
        this.height = height;
    }

    /**
     * Creates a new empty <tt>ThumbnailElement</tt>.
     * @param parser
     */
    public ThumbnailElement(XmlPullParser parser)
    {
        cid = parseCid(parser.getAttributeValue("", CID));
        mimeType = parser.getAttributeValue("", MIME_TYPE);
        String parserWidth = parser.getAttributeValue("", WIDTH);
        String parserHeight = parser.getAttributeValue("", HEIGHT);

        try
        {
            width = Integer.parseInt(parserWidth);
            height = Integer.parseInt(parserHeight);
        }
        catch (NumberFormatException nfe)
        {
            // ignore, width and height are optional
        }
    }

    private ContentId parseCid(String cid)
    {
        // previous Jitsi versions used to send <hashType>-<hash>@<server>
        if (!cid.endsWith("@bob.xmpp.org"))
        {
            cid = cid.substring(0, cid.indexOf('@')) + "@bob.xmpp.org";
        }

        return ContentId.fromCid(cid);
    }

    /**
     * Returns the XML representation of this ExtensionElement.
     *
     * @return the packet extension as XML.
     */
    public String toXML()
    {
        StringBuffer buf = new StringBuffer();

        // open element
        buf.append("<").append(ELEMENT_NAME).
            append(" xmlns=\"").append(NAMESPACE).append("\"");

        // adding thumbnail parameters
        buf = addXmlAttribute(buf, CID, this.getContentId().getCid());
        buf = addXmlAttribute(buf, MIME_TYPE, this.getMimeType());
        buf = addXmlIntAttribute(buf, WIDTH, this.getWidth());
        buf = addXmlIntAttribute(buf, HEIGHT, this.getWidth());

        // close element
        buf.append("/>");

        return buf.toString();
    }

    /**
     * Returns the Content-ID, corresponding to this <tt>ThumbnailElement</tt>.
     * @return the Content-ID, corresponding to this <tt>ThumbnailElement</tt>
     */
    public ContentId getContentId()
    {
        return cid;
    }

    /**
     * Returns the mime type of this <tt>ThumbnailElement</tt>.
     * @return the mime type of this <tt>ThumbnailElement</tt>
     */
    public String getMimeType()
    {
        return mimeType;
    }

    /**
     * Returns the width of this <tt>ThumbnailElement</tt>.
     * @return the width of this <tt>ThumbnailElement</tt>
     */
    public int getWidth()
    {
        return width;
    }

    /**
     * Returns the height of this <tt>ThumbnailElement</tt>.
     * @return the height of this <tt>ThumbnailElement</tt>
     */
    public int getHeight()
    {
        return height;
    }

    /**
     * Sets the content-ID of this <tt>ThumbnailElement</tt>.
     * @param cid the content-ID to set
     */
    public void setCid(ContentId cid)
    {
        this.cid = cid;
    }

    /**
     * Sets the mime type of the thumbnail.
     * @param mimeType the mime type of the thumbnail
     */
    public void setMimeType(String mimeType)
    {
        this.mimeType = mimeType;
    }

    /**
     * Sets the width of the thumbnail
     * @param width the width of the thumbnail
     */
    public void setWidth(int width)
    {
        this.width = width;
    }

    /**
     * Sets the height of the thumbnail
     * @param height the height of the thumbnail
     */
    public void setHeight(int height)
    {
        this.height = height;
    }

    /**
     * Creates the XML <tt>String</tt> corresponding to the specified attribute
     * and value and adds them to the <tt>buff</tt> StringBuffer.
     *
     * @param buff the <tt>StringBuffer</tt> to add the attribute and value to.
     * @param attrName the name of the thumbnail attribute that we're adding.
     * @param attrValue the value of the attribute we're adding to the XML
     * buffer.
     * @return the <tt>StringBuffer</tt> that we've added the attribute and its
     * value to.
     */
    private StringBuffer addXmlAttribute(   StringBuffer buff,
                                            String attrName,
                                            String attrValue)
    {
        buff.append(" " + attrName + "=\"").append(attrValue).append("\"");

        return buff;
    }

    /**
     * Creates the XML <tt>String</tt> corresponding to the specified attribute
     * and value and adds them to the <tt>buff</tt> StringBuffer.
     *
     * @param buff the <tt>StringBuffer</tt> to add the attribute and value to.
     * @param attrName the name of the thumbnail attribute that we're adding.
     * @param attrValue the value of the attribute we're adding to the XML
     * buffer.
     * @return the <tt>StringBuffer</tt> that we've added the attribute and its
     * value to.
     */
    private StringBuffer addXmlIntAttribute(StringBuffer buff,
                                            String attrName,
                                            int attrValue)
    {
        return addXmlAttribute(buff, attrName, String.valueOf(attrValue));
    }

    /**
     * Creates the cid attribute value for the given <tt>thumbnailData</tt>.
     *
     * @param thumbnailData the byte array containing the data
     * @return the cid attribute value for the thumbnail extension
     */
    private ContentId createCid(byte[] thumbnailData)
    {
        return new ContentId(SHA1.hex(thumbnailData), "sha1");
    }
}
