/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import org.jitsi.util.*;

import net.java.sip.communicator.plugin.desktoputil.event.*;
import net.java.sip.communicator.util.Logger;

/**
 * The <tt>SIPCommTextField</tt> is a <tt>JTextField</tt> that offers the
 * possibility to specify hint text that explains what is the required data.
 * @author Yana Stamcheva
 */
public class SIPCommTextField
    extends JTextField
    implements  MouseListener,
                FocusListener,
                KeyListener,
                DocumentListener,
                ColoredDefaultText
{
    private static final Logger sLog = Logger.getLogger(SIPCommTextField.class);

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The hint text. This is displayed in the field when it is empty to
     * indicate to the user what type of data they should enter into this
     * field. This text disappears when the user clicks in the field to
     * type into it and will not be submitted if the field is submitted without
     * the user replacing the hint text.
     */
    private String mHintText;

    /**
     * The maximum length of the text that can be submitted into this field.
     */
    private final int mMaxLength;

    /**
     * A list of all listeners registered for text field change events.
     */
    private final Collection<TextFieldChangeListener> changeListeners
        = new LinkedList<>();

    /**
     * Indicates if the hint text is currently visible.
     */
    private boolean isHintTextVisible;

    /**
     * The color of the foreground.
     */
    private Color foregroundColor = Color.BLACK;

    /**
     * The foreground color of the hint text.
     */
    private Color hintTextColor = Color.GRAY;

    /**
     * Creates an instance of <tt>SIPCommTextField</tt> by specifying the hint
     * text that we would like to show in it.
     * @param hintText the hint text. This is displayed in the field when it is
     * empty to indicate to the user what type of data they should enter into
     * this field. This text disappears when the user clicks in the field to
     * type into it and will not be submitted if the field is submitted without
     * the user replacing the hint text. This can be an empty string to
     * indicate that there should be no hint text.
     */
    public SIPCommTextField(String hintText)
    {
        this(hintText, -1);
    }

    /**
     * Creates an instance of <tt>SIPCommTextField</tt> by specifying the hint
     * text that we would like to show in it and the maximum permitted number
     * of characters.
     * @param hintText the hint text. This is displayed in the field when it is
     * empty to indicate to the user what type of data they should enter into
     * this field. This text disappears when the user clicks in the field to
     * type into it and will not be submitted if the field is submitted without
     * the user replacing the hint text. This can be an empty string to
     * indicate that there should be no hint text. If this exceeds maxLength,
     * it will be truncated.
     * @param maxLength the maximum number of characters permitted in the
     * field.  If the number supplied is < 1, no maximum will be set.
     */
    public SIPCommTextField(String hintText, int maxLength)
    {
        super();

        mMaxLength = maxLength;

        // If a maximum length has been provided, limit the number of
        // characters that the user can type into the field to that length.
        if (mMaxLength > 0)
        {
            AbstractDocument document = (AbstractDocument)getDocument();
            document.setDocumentFilter(new LimitLengthFilter(mMaxLength));
        }

        if (hintText != null && hintText.length() > 0)
        {
            // Hint text has been provided, so truncate it to the maximum
            // length then set it.
            if (mMaxLength > 0)
            {
                hintText = StringUtils.truncate(hintText, mMaxLength);
            }

            // Prepend and append a space to the hint text to make it unlikely
            // that the user will type text into the field that exactly matches
            // the hint text, as doing so would clear the field.  We both
            // prepend and append to ensure the text remains centered in the
            // field in case the field is sized to be just big enough for the
            // text.
            mHintText = " " + hintText + " ";
            isHintTextVisible = true;
        }
        else
        {
            // No hint text has been provided, so just set it to null.
            mHintText = null;
        }

        // We've finished processing the hint text, so set it.
        setToHintText();

        // The "normal" font size is 12, we want our font to be slightly smaller
        // However, the font size varies depending on the zoom level of the
        // screen.  Thus we can't just set the font size to be 10, but the
        // current font size * (10 / 12).
        this.setFont(getFont().deriveFont(ScaleUtils.getMediumFontSize()));

        this.addMouseListener(this);
        this.addFocusListener(this);

        this.addKeyListener(this);
        this.getDocument().addDocumentListener(this);

        // Set the transfer handler so that we can control which sorts of data
        // this component will accept
        final TransferHandler oldTransferHandler = getTransferHandler();
        if (oldTransferHandler == null)
        {
            sLog.warn("Unable to get reference to old transfer handler");
        }

        setTransferHandler(new TransferHandler()
        {
            private static final long serialVersionUID = 0L;

            @Override
            public boolean canImport(TransferSupport support)
            {
                // Only support string data types
                for (DataFlavor dataFlavor : support.getDataFlavors())
                {
                    if (dataFlavor.equals(DataFlavor.stringFlavor))
                        return true;
                }

                return false;
            }

            @Override
            public boolean importData(TransferSupport support)
            {
                // Override the default import data behaviour as otherwise the
                // imported text will be stuck in the middle of the default.
                DataFlavor[] dataFlavors = support.getDataFlavors();

                for (DataFlavor dataFlavor : dataFlavors)
                {
                    if (!dataFlavor.equals(DataFlavor.stringFlavor))
                        continue;

                    try
                    {
                        Object data = support.getTransferable()
                                             .getTransferData(dataFlavor);

                        // If we are showing the hint text (i.e. getText()
                        // returns null), then we need to replace it entirely.
                        // Otherwise we can just fall back on the default handler
                        if (getText() == null || oldTransferHandler == null)
                        {
                            setText(data.toString());
                            return true;
                        }
                        else
                        {
                            return oldTransferHandler.importData(support);
                        }
                    }
                    catch (UnsupportedFlavorException | IOException e)
                    {
                        // Nothing required
                    }
                }

                return false;
            }

            @Override
            public void exportAsDrag(JComponent comp, InputEvent e, int action)
            {
                if (oldTransferHandler != null)
                    oldTransferHandler.exportAsDrag(comp, e, action);
            }

            @Override
            public void exportToClipboard(JComponent comp, Clipboard clip,
                int action) throws IllegalStateException
            {
                if (oldTransferHandler != null)
                    oldTransferHandler.exportToClipboard(comp, clip, action);
            }
        });
    }

    /**
     * Indicates that the mouse button was pressed on this component. Hides
     * the hint text when user clicks on the text field.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    @Override
    public void mousePressed(MouseEvent e)
    {
        if (getText() == null && isEnabled())
        {
            clearHintText();
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
        clearHintText();
    }

    /**
     * Sets the hint text when the field looses focus.
     * @param e the <tt>FocusEvent</tt> that notified us
     */
    public void focusLost(FocusEvent e)
    {
        if (getText() == null || getText().length() == 0)
        {
            setToHintText();
        }
    }

    /**
     * Returns the text contained in this field.
     * @return the text contained in this field
     */
    public String getText()
    {
        if (!super.getText().equals(mHintText))
            return super.getText();

        return null;
    }

    /**
     * Sets the text of this text field.  If the provided text is longer than
     * the maximum length specified in the constructor, it will be truncated.
     * @param text the text to show in this text field
     */
    public void setText(String text)
    {
        if ((text == null || text.length() == 0) && !isFocusOwner())
            setToHintText();
        else if (text != null)
        {
            if (mMaxLength > 0)
            {
                text = StringUtils.truncate(text, mMaxLength);
            }

            this.setForeground(foregroundColor);
            super.setText(text);
        }
    }

    /**
     * Sets contents to the hint text.
     */
    protected void setToHintText()
    {
        super.setText(mHintText);
        this.setForeground(hintTextColor);
        this.setCaretPosition(0);
        ScaleUtils.scaleFontAsDefault(this);
    }

    /**
     * Clears the hint text.
     */
    protected void clearHintText()
    {
        if (super.getText().equals(mHintText))
        {
            super.setText("");
            this.setForeground(foregroundColor);
        }
    }

    /**
     * Clears the hint text when a key pressed event is received.
     * @param e the <tt>KeyEvent</tt> that notified us
     */
    public void keyPressed(KeyEvent e)
    {
        clearHintText();
    }

    /**
     * Clears the hint text when a key typed event is received.
     * @param e the <tt>KeyEvent</tt> that notified us
     */
    public void keyTyped(KeyEvent e)
    {
        clearHintText();
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

    public void changedUpdate(DocumentEvent e) {}

    /**
     * Handles the change when a char has been inserted in the field.
     * @param e the <tt>DocumentEvent</tt> that notified us
     */
    public void insertUpdate(DocumentEvent e)
    {
        if(!super.getText().equals(mHintText))
            fireTextFieldChangeListener(0);
        else
            isHintTextVisible = true;
    }

    /**
     * Handles the change when a char has been removed from the field.
     * @param e the <tt>DocumentEvent</tt> that notified us
     */
    public void removeUpdate(DocumentEvent e)
    {
        if (!isHintTextVisible)
            fireTextFieldChangeListener(1);
        else
            isHintTextVisible = false;
    }

    @Override
    public Color getForegroundColor()
    {
        return foregroundColor;
    }

    @Override
    public void setForegroundColor(Color c)
    {
        foregroundColor = c;
    }

    @Override
    public void setDefaultTextColor(Color c)
    {
        hintTextColor = c;

        if (isHintTextVisible)
            setForeground(hintTextColor);
    }

    @Override
    public Color getDefaultTextColor()
    {
        return hintTextColor;
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
