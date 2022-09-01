// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;

/**
 * The purpose of this interface is to allow UI components with a default
 * text value to give the default text its own colour, set independently of
 * the normal text colour.
 */
public interface ColoredDefaultText
{
    /**
     * Sets the foreground color.
     *
     * @param c the color to set for the text field foreground
     */
    void setForegroundColor(Color c);

    /**
     * Gets the foreground color.
     *
     * @return the color of the text
     */
    Color getForegroundColor();

    /**
     * Sets the foreground color of the default text shown in this text field.
     *
     * @param c the color to set
     */
    void setDefaultTextColor(Color c);

    /**
     * Gets the foreground color of the default text shown in this text field.
     *
     * @return the color of the default text
     */
    Color getDefaultTextColor();
}
