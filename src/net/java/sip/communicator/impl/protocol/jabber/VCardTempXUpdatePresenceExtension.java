/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.PresenceBuilder;
import org.jivesoftware.smack.util.Consumer;
import org.jivesoftware.smack.util.StringUtils;

import net.java.sip.communicator.util.Logger;

/**
 * Implements the presence extension corresponding to element name  "x" and
 * namespace "vcard-temp:x:update" (cf. XEP-0153). This ables to include the
 * SHA-1 hash of the avatar image in the "photo" tag of this presence extension.
 *
 * @author Vincent Lucas
 */
public class VCardTempXUpdatePresenceExtension
    implements ExtensionElement, Consumer<PresenceBuilder>
{
    /**
     * The <tt>Logger</tt> used by the
     * <tt>VCardTempXUpdatePresenceExtension</tt> class and its instances for
     * logging output.
     */
    private static final Logger logger
        = Logger.getLogger(VCardTempXUpdatePresenceExtension.class);

    /**
     * This presence extension element name.
     */
    public static final String ELEMENT_NAME =  "x";

    /**
     * This presence extension namespace.
     */
    public static final String NAMESPACE = "vcard-temp:x:update";

    /**
     * The SHA-1 hash in hexadecimal representation of the avatar image.
     */
    private String imageSha1 = null;

    /**
     * The whole XML string generated by this presence extension.
     */
    private String xmlString = null;

    /**
     * Creates a new instance of this presence extension with the avatar image
     * given in parameter.
     *
     * @param imageBytes The avatar image.
     */
    public VCardTempXUpdatePresenceExtension(byte[] imageBytes)
    {
        // Computes the default XML with an empty image avatar (necessary to
        // manage the case when "imageBytes" is "null").
        this.computeXML();
        // Updates this presence extension with a new avatar image.
        this.updateImage(imageBytes);
    }

    /**
     * Updates the avatar image manged by this presence extension.
     *
     * @param imageBytes The avatar image.
     *
     * @return "false" if the new avatar image is the same as the current one.
     * "true" if this presence extension has been updated with the  new avatar
     * image.
     */
    public boolean updateImage(byte[] imageBytes)
    {
        boolean isImageUpdated = false;

        // Computes the SHA-1 hash in hexadecimal representation.
        String tmpImageSha1 = (imageBytes == null || imageBytes.length == 0) ? null :
            VCardTempXUpdatePresenceExtension.getImageSha1(imageBytes);

        // If the image has changed, then recomputes the XML string.
        if (!Objects.equals(tmpImageSha1, imageSha1))
        {
            imageSha1 = tmpImageSha1;
            this.computeXML();
            isImageUpdated = true;
        }

        return isImageUpdated;
    }

    /**
     * Gets the String representation in hexadecimal of the SHA-1 hash of the
     * image given in parameter.
     *
     * @param image The image to get the hexadecimal representation of the SHA-1
     * hash.
     *
     * @return The SHA-A hash hexadecimal representation of the image. Null if
     * the image is null or if the SHA1 is not recognized as a valid algorithm.
     */
    public static String getImageSha1(byte[] image)
    {
        String imageSha1 = null;

        try
        {
            if (image != null)
            {
                MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
                imageSha1 = StringUtils.encodeHex(messageDigest.digest(image));
            }
        }
        catch (NoSuchAlgorithmException ex)
        {
            logger.error("SHA1 algorithm unavailable!", ex);
        }

        return imageSha1;
    }

    /**
     * Computes the XML string used by this presence extension.
     */
    private void computeXML()
    {
        StringBuilder stringBuilder = new StringBuilder(
                "<" + this.getElementName() +
                " xmlns='" + this.getNamespace() + "'>");
        if (imageSha1 == null)
        {
            stringBuilder.append("<photo/>");
        }
        else
        {
            stringBuilder.append("<photo>" + imageSha1 + "</photo>");
        }
        stringBuilder.append("</" + this.getElementName() + ">");

        this.xmlString = stringBuilder.toString();
    }

    /**
     * Returns the root element name.
     *
     * @return the element name.
     */
    public String getElementName()
    {
        return ELEMENT_NAME;
    }

    /**
     * Returns the root element XML namespace.
     *
     * @return the namespace.
     */
    public String getNamespace()
    {
        return NAMESPACE;
    }

    /**
     * Returns the XML representation of the ExtensionElement.
     *
     * @return the packet extension as XML.
     */
    public String toXML()
    {
        return this.xmlString;
    }

    @Override
    public String toXML(XmlEnvironment enclosingNamespace)
    {
        return toXML();
    }

    /**
     * Intercepts sent presence packets in order to add this extension.
     *
     * @param packet The sent presence packet.
     */
    public void accept(PresenceBuilder presenceBuilder)
    {
        presenceBuilder.addExtension(this);
    }
}
