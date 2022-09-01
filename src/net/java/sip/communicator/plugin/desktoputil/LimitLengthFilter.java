// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import javax.swing.text.*;

/**
 * Filter which limits the amount of characters the user can type in the
 * text field to a specified amount.
 */
class LimitLengthFilter extends DocumentFilter
{
    private final int mMaxChars;

    public LimitLengthFilter(int limit)
    {
        mMaxChars = limit;
    }

    @Override
    public void replace(FilterBypass fb, int offset, int charsRemoved,
                        String newText, AttributeSet attrs)
                            throws BadLocationException
    {
        // 'replace' is called every time the user makes an atomic text
        // operation, e.g. typing a single character, deleting one or more
        // characters, pasting text into the field, or pasting new text
        // over the top of existing characters.
        // In the latter case, a single update might both add new text
        // and remove existing text at the same time.

        if (newText == null)
        {
            // The update doesn't add any new characters. Therefore it is
            // a deletion of one or more characters. This will never make us
            // exceed the character limit, so just allow it to happen.
            super.replace(fb, offset, charsRemoved, newText, attrs);
            return;
        }

        int currentLength = fb.getDocument().getLength();
        int lengthChange = newText.length() - charsRemoved;
        int newLength = currentLength + lengthChange;

        int excess = newLength - mMaxChars;
        if (excess > 0)
        {
            // The update would cause us to exceed the character limit,
            // so only allow the first few characters of the update to
            // be applied.
            newText = newText.substring(0, newText.length() - excess);
        }

        super.replace(fb,  offset, charsRemoved, newText, attrs);
    }
}
