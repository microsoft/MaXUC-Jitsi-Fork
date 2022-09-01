// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat;

/**
 * A DisplayablePartOfMessage represents anything which might be displayed in
 * the chat area. This could include things like plain text, or pictures.
 */
public abstract class DisplayablePartOfMessage
{
    /**
     * Returns the bit of HTML text which will display the
     * displayablePartOfMessage correctly in the chat area. For example, for
     * plain text, this method will return the text surrounded by <PLAINTEXT>
     * tags; for an image, it will return the HTML including file path, image
     * size etc.
     * @return HTML representation of this message part
     */
    public abstract String toHTML();
}
