// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat;

import java.util.Objects;

/**
 * A PictureMessagePart contains the information to display an image as part of
 * a message in a chat conversation. So for example, emojis and emoticons will
 * be converted to PictureMessageParts, so that they are displayed by their
 * corresponding images.
 */
public class PicturePartOfMessage extends DisplayablePartOfMessage
{
    /**
     * The URL of image to be displayed in the message.
     */
    private final String imageURL;
    private final String imagePath;

    /**
     * The 'alt' is the part of the HTML displaying the image, and is the value
     * which gets copied if the user copies the image from the chat.
     */
    private final String alt;

    /**
     * The constructor for a PictureMessagePart.
     * @param imagePath
     * @param alt
     */
    public PicturePartOfMessage(String imageURL, String alt, String imagePath)
    {
        this.alt = alt;
        this.imagePath = imagePath;
        this.imageURL = imageURL;
    }

    /**
     * Convenience constructor for PictureMessagePart where
     * imageURL and imagePath are the same
     * @param imageURL
     * @param alt
     */
    public PicturePartOfMessage(String imageURL, String alt)
    {
        this.alt = alt;
        this.imagePath = imageURL;
        this.imageURL = imageURL;
    }

    public String getImagePath() { return imagePath; }

    public String getAlt() { return alt; }

    /**
     * Returns the HTML which causes the picture corresponding to this
     * PictureMessagePart to display in the chat area.
     */
    public String toHTML()
    {
        StringBuilder outputStringBuilder = new StringBuilder();

        outputStringBuilder.append("<IMG SRC=\"");
        outputStringBuilder.append(imageURL);
        outputStringBuilder.append("\" BORDER=\"0\" ALT=\"");
        outputStringBuilder.append(alt);
        outputStringBuilder.append("\"></IMG>");

        String outputHtml = outputStringBuilder.toString();
        return outputHtml;
    }

    /**
     * Compares this PicturePartOfMessage to specified object. The result is
     * true iff the argument is not null and is a PicturePartOfMessage object
     * containing the same alt, imagePath and imageURL as this object.
     *
     * @param anObject the object to compare against
     * @return true if given object represents an equivalent
     *         PicturePartOfMessage, false otherwise
     */
    @Override
    public boolean equals(Object anObject)
    {
        if (this == anObject) {
            return true;
        }
        if (anObject instanceof PicturePartOfMessage) {
            PicturePartOfMessage ppom = (PicturePartOfMessage) anObject;
            return (Objects.equals(alt, ppom.alt) &&
                    Objects.equals(imagePath, ppom.imagePath) &&
                    Objects.equals(imageURL, ppom.imageURL));
        }
        return false;
    }

    /**
     * Return a hash code value for this object.
     * @return an integer hash code, formed by combining the hash codes for alt,
     *         imagePath and imageURL strings.
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(alt, imagePath, imageURL);
    }
}
