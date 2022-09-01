// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat;

import java.util.Objects;

/**
 * A TextPartOfMessages contains the information to display an section of text
 * as part of a message in a chat conversation.
 */
public class TextPartOfMessage extends DisplayablePartOfMessage
{
    /**
     * The text that the TextPartOfMessage is wrapping
     */
    private String text;

    /**
     * The constructor for a TextPartOfMessage
     * @param text
     */
    public TextPartOfMessage(String text)
    {
        this.text = text;
    }

    /**
     * Returns the text value of this TextPartOfMessage
     * @return the text value of this TextPartOfMessage
     */
    public String getText()
    {
        return text;
    }

    public void setText(String newText)
    {
        this.text = newText;
    }

    /**
     * Returns the text value of this TextPartOfMessage surrounded by Plaintext
     * tags, so that it displays as text when inserted amongst other HTML.
     * @return the text surrounded in Plaintext tags
     */
    public String toHTML()
    {
        return "<PLAINTEXT>" + this.text + "</PLAINTEXT>";
    }

    /**
     * Compares this TextPartOfMessage to specified object. The result is true
     * iff the argument is not null and is a TextPartOfMessage object that
     * contains the same text string as this object.
     *
     * @param anObject the object to compare against
     * @return true if given object represents an equivalent TextPartOfMessage,
     *         false otherwise
     */
    @Override
    public boolean equals(Object anObject)
    {
        if (this == anObject) {
            return true;
        }
        if (anObject instanceof TextPartOfMessage) {
            TextPartOfMessage tpom = (TextPartOfMessage) anObject;
            return Objects.equals(text, tpom.text);
        }
        return false;
    }

    /**
     * Return a hash code value for this object.
     * @return hash code for text string contained in object, or 0 if null
     */
    @Override
    public int hashCode()
    {
        return Objects.hashCode(text);
    }
}
