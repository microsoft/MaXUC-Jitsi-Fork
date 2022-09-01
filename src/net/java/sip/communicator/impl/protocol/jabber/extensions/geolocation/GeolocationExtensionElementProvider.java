/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.geolocation;

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import net.java.sip.communicator.util.Logger;

/**
 * This class parses incoming messages and extracts the geolocation parameters
 * from the raw XML messages.
 *
 * @author Guillaume Schreiner
 */
public class GeolocationExtensionElementProvider
    extends ExtensionElementProvider<GeolocationExtensionElement>
{
    /**
     * The logger of this class.
     */
    private static final Logger logger =
        Logger.getLogger(GeolocationExtensionElementProvider.class);

    /**
     * The name of the XML element used for transport of geolocation parameters.
     */
    public static final String ELEMENT_NAME = "geoloc";

    /**
     * The names XMPP space that the geolocation elements belong to.
     */
    public static final String NAMESPACE = "http://jabber.org/protocol/geoloc";

    /**
     * Creates a new GeolocationPacketExtensionProvider.
     * ProviderManager requires that every PacketExtensionProvider has a public,
     * no-argument constructor
     */
    public GeolocationExtensionElementProvider()
    {}

    /**
     * Parses a GeolocationPacketExtension packet (extension sub-packet).
     *
     * @param parser an XML parser.
     * @return a new GeolocationPacketExtension instance.
     * @throws Exception if an error occurs parsing the XML.
     * @todo Implement this
     *   org.jivesoftware.smack.provider.PacketExtensionProvider method
     */
    public GeolocationExtensionElement parse(XmlPullParser parser,
                                            int initialDepth,
                                            XmlEnvironment xmlEnvironment)
            throws XmlPullParserException, IOException, SmackParsingException
    {
        GeolocationExtensionElement result = new GeolocationExtensionElement();

        logger.trace("Trying to map XML Geolocation Extension");

        boolean done = false;
        while (!done)
        {
            try
            {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT)
                {
                    if (parser.getName().equals(GeolocationExtensionElement.ALT))
                    {
                        result.setAlt(Float.parseFloat(parser.nextText()));
                    }
                    if (parser.getName()
                            .equals(GeolocationExtensionElement.AREA))
                    {
                        result.setArea(parser.nextText());
                    }
                    if (parser.getName()
                            .equals(GeolocationExtensionElement.BEARING))
                    {
                        result.setBearing(Float.parseFloat(parser.nextText()));
                    }
                    if (parser.getName()
                            .equals(GeolocationExtensionElement.BUILDING))
                    {
                        result.setBuilding(parser.nextText());
                    }
                    if (parser.getName()
                            .equals(GeolocationExtensionElement.COUNTRY))
                    {
                        result.setCountry(parser.nextText());
                    }
                    if (parser.getName()
                            .equals(GeolocationExtensionElement.DATUM))
                    {
                        result.setDatum(parser.nextText());
                    }
                    if (parser.getName().equals(GeolocationExtensionElement.
                                                DESCRIPTION))
                    {
                        result.setDescription(parser.nextText());
                    }
                    if (parser.getName()
                            .equals(GeolocationExtensionElement.ERROR))
                    {
                        result.setError(Float.parseFloat(parser.nextText()));
                    }
                    if (parser.getName()
                            .equals(GeolocationExtensionElement.FLOOR))
                    {
                        result.setFloor(parser.nextText());
                    }
                    if (parser.getName().equals(GeolocationExtensionElement.LAT))
                    {
                        result.setLat(Float.parseFloat(parser.nextText()));
                    }
                    if (parser.getName()
                            .equals(GeolocationExtensionElement.LOCALITY))
                    {
                        result.setLocality(parser.nextText());
                    }
                    if (parser.getName().equals(GeolocationExtensionElement.LON))
                    {
                        result.setLon(Float.parseFloat(parser.nextText()));
                    }
                    if (parser.getName()
                            .equals(GeolocationExtensionElement.POSTALCODE))
                    {
                        result.setPostalCode(parser.nextText());
                    }
                    if (parser.getName()
                            .equals(GeolocationExtensionElement.REGION))
                    {
                        result.setRegion(parser.nextText());
                    }
                    if (parser.getName()
                            .equals(GeolocationExtensionElement.ROOM))
                    {
                        result.setRoom(parser.nextText());
                    }
                    if (parser.getName()
                            .equals(GeolocationExtensionElement.STREET))
                    {
                        result.setStreet(parser.nextText());
                    }
                    if (parser.getName()
                            .equals(GeolocationExtensionElement.TEXT))
                    {
                        result.setText(parser.nextText());
                    }
                    if (parser.getName()
                            .equals(GeolocationExtensionElement.TIMESTAMP))
                    {
                        result.setText(parser.nextText());
                    }
                }
                else if (eventType == XmlPullParser.Event.END_ELEMENT)
                {
                    if (parser.getName().equals(
                            GeolocationExtensionElementProvider.ELEMENT_NAME))
                    {
                        done = true;
                        logger.trace("Parsing finish");
                    }
                }
            }
            catch (NumberFormatException ex)
            {
                ex.printStackTrace();
            }
        }

        return result;
    }
}
