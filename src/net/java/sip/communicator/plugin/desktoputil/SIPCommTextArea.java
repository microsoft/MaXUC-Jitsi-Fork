/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.desktoputil.event.*;

/**
 * The <tt>SIPCommTextArea</tt> is a <tt>JTextArea</tt> that offers the
 * possibility to specify a default (tip) text that explains what is the
 * required data.
 * @author Yana Stamcheva
 */
public class SIPCommTextArea
    extends JTextArea
    implements  MouseListener,
                FocusListener,
                KeyListener,
                DocumentListener,
                ColoredDefaultText
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The default text.
     */
    private String defaultText;

    /**
     * A list of all listeners registered for text field change events.
     */
    private final Collection<TextFieldChangeListener> changeListeners
        = new LinkedList<>();

    /**
     * Indicates if the default text is currently visible.
     */
    private boolean isDefaultTextVisible;

    /**
     * The color of the foreground.
     */
    private Color foregroundColor = Color.BLACK;

    /**
     * The foreground color of the default text.
     */
    private Color defaultTextColor = Color.GRAY;

    /**
     * Creates an instance of <tt>SIPCommTextArea</tt> by specifying the text
     * we would like to show by default in it.
     * @param text the text we would like to enter by default
     * @param rows the number of rows >= 0
     * @param columns the number of columns >= 0
     */
    public SIPCommTextArea(String text, int rows, int columns)
    {
        super(text, rows, columns);

        if (text != null && text.length() > 0)
        {
            this.defaultText = text;
            isDefaultTextVisible = true;
        }

        // The "normal" font size is 12, we want our font to be slightly smaller
        // However, the font size varies depending on the zoom level of the
        // screen.  Thus we can't just set the font size to be 10, but the
        // current font size * (10 / 12).
        this.setFont(getFont().deriveFont(ScaleUtils.getMediumFontSize()));

        this.setForeground(defaultTextColor);

        this.addMouseListener(this);
        this.addFocusListener(this);

        this.addKeyListener(this);
        this.getDocument().addDocumentListener(this);
    }

    /**
     * Indicates that the mouse button was pressed on this component. Hides
     * the default text when user clicks on the text field.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    @Override
    public void mousePressed(MouseEvent e)
    {
        if (getText() == null)
        {
            clearDefaultText();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    /**
     * Selects the user text when this text field gains the focus.
     * @param e the <tt>FocusEvent</tt> that notified us
     */
    public void focusGained(FocusEvent e)
    {
        clearDefaultText();
    }

    /**
     * Sets the default text when the field looses focus.
     * @param e the <tt>FocusEvent</tt> that notified us
     */
    public void focusLost(FocusEvent e)
    {
        if (getText() == null || getText().length() == 0)
        {
            setDefaultText();
        }
    }

    /**
     * Returns the text contained in this field.
     * @return the text contained in this field
     */
    public String getText()
    {
        if (!super.getText().equals(defaultText))
            return super.getText();

        return null;
    }

    /**
     * Sets the text of this text field.
     * @param text the text to show in this text field
     */
    public void setText(String text)
    {
        if ((text == null || text.length() == 0) && !isFocusOwner())
            setDefaultText();
        else
        {
            this.setForeground(foregroundColor);
            super.setText(text);
        }
    }

    /**
     * Sets the default text.
     */
    private void setDefaultText()
    {
        super.setText(defaultText);
        this.setForeground(defaultTextColor);
        this.setCaretPosition(0);
    }

    /**
     * Clears the default text.
     */
    private void clearDefaultText()
    {
        if (super.getText().equals(defaultText))
        {
            super.setText("");
            this.setForeground(foregroundColor);
        }
    }

    /**
     * Clears the default text when a key pressed event is received.
     * @param e the <tt>KeyEvent</tt> that notified us
     */
    public void keyPressed(KeyEvent e)
    {
        clearDefaultText();
    }

    /**
     * Clears the default text when a key typed event is received.
     * @param e the <tt>KeyEvent</tt> that notified us
     */
    public void keyTyped(KeyEvent e)
    {
        clearDefaultText();
    }

    public void keyReleased(KeyEvent e){}

    /**
     * Adds the given <tt>TextFieldChangeListener</tt> to the list of listeners
     * notified on changes of the text contained in this field.
     * @param l the <tt>TextFieldChangeListener</tt> to add
     */
    public void addTextChangeListener(TextFieldChangeListener l)
    {
        synchronized (changeListeners)
        {
            changeListeners.add(l);
        }
    }

    /**
     * Removes the given <tt>TextFieldChangeListener</tt> from the list of
     * listeners notified on changes of the text contained in this field.
     * @param l the <tt>TextFieldChangeListener</tt> to add
     */
    public void removeTextChangeListener(TextFieldChangeListener l)
    {
        synchronized (changeListeners)
        {
            changeListeners.remove(l);
        }
    }

    public void changedUpdate(DocumentEvent e) {}

    /**
     * Handles the change when a char has been inserted in the field.
     * @param e the <tt>DocumentEvent</tt> that notified us
     */
    public void insertUpdate(DocumentEvent e)
    {
        if(!super.getText().equals(defaultText))
            fireTextFieldChangeListener(0);
        else
            isDefaultTextVisible = true;
    }

    /**
     * Handles the change when a char has been removed from the field.
     * @param e the <tt>DocumentEvent</tt> that notified us
     */
    public void removeUpdate(DocumentEvent e)
    {
        if (!isDefaultTextVisible)
            fireTextFieldChangeListener(1);
        else
            isDefaultTextVisible = false;
    }

    /**
     * Sets the foreground color.
     *
     * @param c the color to set for the text field foreground
     */
    public void setForegroundColor(Color c)
    {
        foregroundColor = c;
    }

    /**
     * Gets the foreground color.
     *
     * @return the color of the text
     */
    public Color getForegroundColor()
    {
        return foregroundColor;
    }

    /**
     * Sets the foreground color of the default text shown in this text field.
     *
     * @param c the color to set
     */
    public void setDefaultTextColor(Color c)
    {
        defaultTextColor = c;

        if (isDefaultTextVisible)
            setForeground(defaultTextColor);
    }

    /**
     * Gets the foreground color of the default text shown in this text field.
     *
     * @return the color of the default text
     */
    public Color getDefaultTextColor()
    {
        return defaultTextColor;
    }

    /**
     * Notifies all registered <tt>TextFieldChangeListener</tt>s that a change
     * has occurred in the text contained in this field.
     * @param eventType the type of the event to transfer
     */
    private void fireTextFieldChangeListener(int eventType)
    {
        for (TextFieldChangeListener l : changeListeners)
            switch (eventType)
            {
                case 0: l.textInserted(); break;
                case 1: l.textRemoved(); break;
            }
    }
}
