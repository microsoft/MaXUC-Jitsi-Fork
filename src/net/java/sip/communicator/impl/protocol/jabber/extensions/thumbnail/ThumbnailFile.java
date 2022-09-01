/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.thumbnail;

import org.jxmpp.util.XmppDateTime;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.si.packet.StreamInitiation;

/**
 * The <tt>ThumbnailFile</tt> extends the smackx <tt>StreamInitiation.File</tt>
 * in order to provide a file that supports thumbnails.
 *
 * @author Yana Stamcheva
 */
public class ThumbnailFile
    extends StreamInitiation.File
    {
        private ThumbnailElement thumbnail;

        /**
         * Creates a <tt>ThumbnailFile</tt> by specifying a base file and a thumbnail
         * to extend it with.
         *
         * @param baseFile the file used as a base
         * @param thumbnail the thumbnail to add
         */
        public ThumbnailFile(StreamInitiation.File baseFile, ThumbnailElement thumbnail)
        {
            this(baseFile.getName(), baseFile.getSize());

            this.thumbnail = thumbnail;
        }

        /**
         * Creates a <tt>ThumbnailFile</tt> by specifying the name and the size of the
         * file.
         *
         * @param name the name of the file
         * @param size the size of the file
         */
        public ThumbnailFile(String name, long size)
        {
            super(name, size);
        }

        /**
         * Represents this <tt>ThumbnailFile</tt> in an XML.
         */
        @Override
        public String toXML(XmlEnvironment xmlEnvironment)
        {
            XmlStringBuilder xml = new XmlStringBuilder();

            xml.halfOpenElement(getElementName());
            xml.xmlnsAttribute(getNamespace());

            if (getName() != null)
            {
                xml.attribute("name", getName());
            }

            if (getSize() > 0)
            {
                xml.attribute("size", String.valueOf(getSize()));
            }

            if (getDate() != null)
            {
                xml.attribute(
                    "date",
                    XmppDateTime.formatXEP0082Date(this.getDate()));
            }

            xml.optAttribute("hash", getHash());
            xml.rightAngleBracket();

            String desc = this.getDesc();
            if (StringUtils.isNotEmpty(desc)
                    || isRanged()
                    || thumbnail != null)
            {
                if (StringUtils.isNotEmpty(desc))
                {
                    xml.element("desc", desc);
                }

                if (isRanged())
                {
                    xml.emptyElement("range");
                }

                if (thumbnail != null)
                {
                    xml.append(thumbnail.toXML());
                }

                xml.closeElement(getElementName());
            }
            else
            {
                xml.closeEmptyElement();
            }

            return xml.toString();
        }

        /**
         * Represents this <tt>ThumbnailFile</tt> in an XML.
         */
        @Override
        public String toXML()
        {
            return this.toXML(XmlEnvironment.EMPTY);
        }

        /**
         * Returns the <tt>ThumbnailElement</tt> contained in this <tt>ThumbnailFile</tt>.
         * @return the <tt>ThumbnailElement</tt> contained in this <tt>ThumbnailFile</tt>
         */
        public ThumbnailElement getThumbnailElement()
        {
            return thumbnail;
        }

        /**
         * Sets the given <tt>ThumbnailElement</tt> to this <tt>File</tt>.
         * @param thumbnail the <tt>ThumbnailElement</tt> to set
         */
        void setThumbnailElement(ThumbnailElement thumbnail)
        {
            this.thumbnail = thumbnail;
        }
    }
