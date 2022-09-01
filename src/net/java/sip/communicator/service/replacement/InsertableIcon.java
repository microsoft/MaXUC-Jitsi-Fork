// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.replacement;

import org.jitsi.service.resources.ImageIconFuture;

/**
 * Represents an icon to be displayed in a selector box, such as the emoji
 * selector box, that can be inserted into the chat panel.
 */
public class InsertableIcon
{
    /**
     * The filepath of the image to be inserted.
     */
    private final String filepath;

    /**
     * The text to be added to the chatPanel
     */
    private final String textToAdd;

    /**
     * The imageIcon of the image to be inserted
     */
    private final ImageIconFuture imageIcon;

    /**
     * Constructor
     */
    public InsertableIcon(String textToAdd, String filepath, ImageIconFuture image)
    {
        this.textToAdd = textToAdd;
        this.filepath = filepath;
        this.imageIcon = image;
    }

    /**
     * @return the filepath
     */
    public String getFilepath()
    {
        return filepath;
    }

    /**
     * @return the text to add
     */
    public String getTextToAdd()
    {
        return textToAdd;
    }

    /**
     * @return the image icon
     */
    public ImageIconFuture getImageIcon()
    {
        return imageIcon;
    }
}
