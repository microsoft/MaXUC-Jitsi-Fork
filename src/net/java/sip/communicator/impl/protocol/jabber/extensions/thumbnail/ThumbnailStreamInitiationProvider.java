/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.thumbnail;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.si.packet.StreamInitiation;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jivesoftware.smackx.xdata.provider.DataFormProvider;
import org.jxmpp.util.XmppDateTime;

import net.java.sip.communicator.util.Logger;

/**
 * A class implementing <tt>IQProvider</tt> to allow parsing of ThumbnailStreamInitiationProvider
 * IQs.
 *
 */
public class ThumbnailStreamInitiationProvider
    extends IQProvider<StreamInitiation>
{
    private static final Logger logger
        = Logger.getLogger(ThumbnailStreamInitiationProvider.class);

    /**
     * Parses the given <tt>parser</tt> in order to create a
     * <tt>FileElement</tt> from it.
     * @param parser the parser to parse
     */
    public StreamInitiation parse(XmlPullParser parser,
                                  int initialDepth,
                                  XmlEnvironment xmlEnvironment)
        throws XmlPullParserException, IOException, SmackParsingException
    {
        boolean done = false;

        // si
        String id = parser.getAttributeValue("", "id");
        String mimeType = parser.getAttributeValue("", "mime-type");
        StreamInitiation initiation = new StreamInitiation();

        // file
        String name = null;
        String size = null;
        String hash = null;
        String date = null;
        String desc = null;
        ThumbnailElement thumbnail = null;
        boolean isRanged = false;

        // feature
        DataForm form = null;
        DataFormProvider dataFormProvider = new DataFormProvider();

        XmlPullParser.Event eventType;
        String elementName;
        String namespace;

        while (!done)
        {
            eventType = parser.next();
            elementName = parser.getName();
            namespace = parser.getNamespace();

            if (eventType == XmlPullParser.Event.START_ELEMENT)
            {
                if (elementName.equals("file"))
                {
                    name = parser.getAttributeValue("", "name");
                    size = parser.getAttributeValue("", "size");
                    hash = parser.getAttributeValue("", "hash");
                    date = parser.getAttributeValue("", "date");
                }
                else if (elementName.equals("desc"))
                {
                    desc = parser.nextText();
                }
                else if (elementName.equals("range"))
                {
                    isRanged = true;
                }
                else if (elementName.equals("x")
                    && namespace.equals("jabber:x:data"))
                {
                    form = dataFormProvider.parse(parser);
                }
                else if (elementName.equals("thumbnail"))
                {
                    thumbnail = new ThumbnailElement(parser);
                }
            }
            else if (eventType == XmlPullParser.Event.END_ELEMENT)
            {
                if (elementName.equals("si"))
                {
                    done = true;
                }
                // The name-attribute is required per XEP-0096, so ignore the
                // IQ if the name is not set to avoid exceptions. Particularly,
                // the SI response of Empathy contains an invalid, empty
                // file-tag.
                else if (elementName.equals("file") && name != null)
                {
                    long fileSize = 0;

                    if(size != null && size.trim().length() !=0)
                    {
                        try
                        {
                            fileSize = Long.parseLong(size);
                        }
                        catch (NumberFormatException e)
                        {
                            logger.warn("Received an invalid file size,"
                                + " continuing with fileSize set to 0", e);
                        }
                    }

                    ThumbnailFile file = new ThumbnailFile(name, fileSize);
                    file.setHash(hash);

                    if (date != null)
                    {
                        try
                        {
                            file.setDate(XmppDateTime.parseDate(date));
                        }
                        catch (ParseException ex)
                        {
                            logger.warn(
                                "Unknown dateformat on incoming file transfer: "
                                    + date);
                        }
                    }
                    else
                    {
                        file.setDate(new Date());
                    }

                    if (thumbnail != null)
                        file.setThumbnailElement(thumbnail);

                    file.setDesc(desc);
                    file.setRanged(isRanged);
                    initiation.setFile(file);
                }
            }
        }

        initiation.setSessionID(id);
        initiation.setMimeType(mimeType);
        initiation.setFeatureNegotiationForm(form);

        return initiation;
    }
}
