// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import org.jitsi.service.resources.*;

import net.java.sip.communicator.util.*;

/**
 *  Class representing a "snake" text entry panel.
 *
 * A snake text entry panel is a text field which has a left hand side
 * (the head), a repeating middle, then a tail.
 *
 * All snake text entry panels should have their component images defined in the
 * same way:
 * "panel_name"."position"
 * Where
 * "position" is one of "LEFT", "RIGHT" or "MIDDLE"
 *
 * The middle image should be very thin - ideally 1 pixel so that it tiles
 * correctly.
 */
public class SIPCommSnakeTextEntry extends JLayeredPane
{
    private static final long serialVersionUID = 0L;
    private static final Logger logger = Logger.getLogger(SIPCommSnakeTextEntry.class);

    /**
     * The resource management service for access to the background images
     */
    private static final ResourceManagementService sRes =
                                                   UtilActivator.getResources();

    // Background images
    private final BufferedImageFuture mLeft;
    private final BufferedImageFuture mMiddle;
    private final BufferedImageFuture mRight;
    private final BufferedImageFuture mLeftDisabled;
    private final BufferedImageFuture mMiddleDisabled;
    private final BufferedImageFuture mRightDisabled;

    /**
     * The label shown on the left of the text box. This could be used to
     * display a hint. It is also used to display placeholder text as a hint
     * when nothing has been entered into the text field
     */
    private final JLabel mLeftLabel = new JLabel("");

    /**
     * The text field displayed for text entry
     */
    private final JTextField mTextField;

    /**
     * The text to displayed in the LeftLabel. This will be appended by the
     * placeholderText
     */
    private String labelText = null;

    /**
     * The placeholder to be displayed when the textField is empty and not
     * focused
     */
    private String placeholderText = null;

    private Color TEXT_COLOR =
        ConfigurationUtils.useNativeTheme() ? Color.BLACK :
            new Color(UtilActivator.getResources().getColor("service.gui.DARK_TEXT"));

    private Color HINT_TEXT_COLOR =
        new Color(
            DesktopUtilActivator.getResources().getColor(
                "service.gui.MID_TEXT"));

    /**
     * Create a snake text entry panel
     * @param text The text to put in the text entry box
     * @param textColor The color to use for both the text entry and label
     * @param dimension The size of the panel. Height is calculated from
     * background images
     * @param imageRes The resource prefix for the background images
     */
    public SIPCommSnakeTextEntry(String text,
                                 Color textColor,
                                 Dimension dimension,
                                 String imageRes)
    {
        if (textColor != null)
        {
            TEXT_COLOR = textColor;
            HINT_TEXT_COLOR = textColor;
        }
        // Load the background images
        mLeft   = loadImage(imageRes + ".LEFT"  );
        mMiddle = loadImage(imageRes + ".MIDDLE");
        mRight  = loadImage(imageRes + ".RIGHT" );
        mLeftDisabled   = loadImage(imageRes + ".LEFT" + ".DISABLED");
        mMiddleDisabled = loadImage(imageRes + ".MIDDLE" + ".DISABLED");
        mRightDisabled  = loadImage(imageRes + ".RIGHT" + ".DISABLED");

        // Setup the pane
        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        setPreferredSize(dimension);

        // By default the label shouldn't be shown
        mLeftLabel.setVisible(false);
        mLeftLabel.setForeground(HINT_TEXT_COLOR);

        // When the label is clicked, focus the text box
        mLeftLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                mTextField.requestFocus();
            }
        });

        // Create the text field, with no background and no border
        mTextField = new JTextField(text);
        ScaleUtils.scaleFontAsDefault(mTextField);
        mTextField.setOpaque(false);
        mTextField.setBorder(BorderFactory.createEmptyBorder());
        mTextField.setForeground(TEXT_COLOR);

        mTextField.addFocusListener(new FocusListener()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                // When the textField loses focus, we may need to display the
                // placeholder again
                mTextField.select(0, 0);
                displayLabel();
            }

            @Override
            public void focusGained(FocusEvent e)
            {
                // When the textField gains focus, we need to hide the
                // placeholder and select any text inside the textField to allow
                // for quick deletion
                logger.user("textField being edited");
                displayLabel();
                mTextField.selectAll();
            }
        });

        // Create a transparent panel to hold the label and text field.
        // This is then put on a layer above the background
        TransparentPanel textPanel = new TransparentPanel(new BorderLayout());
        textPanel.setBorder(BorderFactory.createEmptyBorder(
            0,
            mLeft.resolve().getWidth(null),
            0,
            mRight.resolve().getWidth(null)));
        textPanel.add(mLeftLabel, BorderLayout.WEST);
        textPanel.add(mTextField, BorderLayout.CENTER);

        // Create a separate component for the background
        JPanel background = new JPanel()
        {
            private static final long serialVersionUID = 0L;

            public void paint(Graphics g)
            {
                paintBackground(g);
            }
        };

        // Add the background, then add the text panel on top
        add(background, new Integer(1), 0);
        add(textPanel, new Integer(2), 0);
    }

    /**
     * Create a snake text entry panel
     * @param text The text to put in the text entry box
     * @param dimension The size of the panel. Height is calculated from
     * background images
     * @param imageRes The resource prefix for the background images
     */
    public SIPCommSnakeTextEntry(String text,
                                 Dimension dimension,
                                 String imageRes)
    {
        this(text, null, dimension, imageRes);
    }

    @Override
    public boolean requestFocusInWindow()
    {
        // When we focus on the panel, we really want to focus on the text field
        return mTextField.requestFocusInWindow();
    }

    @Override
    public boolean isFocusOwner()
    {
        return mTextField.isFocusOwner();
    }

    @Override
    public void setFont(Font font)
    {
        mLeftLabel.setFont(font);
        mTextField.setFont(font);
    }

    /**
     * Add a placeholder to be displayed when no text has been entered and the
     * text field doesn't have focus
     * @param placeholder The text to display as a placeholder
     */
    public void setPlaceholder(String placeholder)
    {
        placeholderText = placeholder;
        displayLabel();
    }

    /**
     * Set a maximum input length for the text entry field.
     * @param maxLength the maximum number of characters allowed
     */
    public void setLimit(int maxLength)
    {
        AbstractDocument document = (AbstractDocument)mTextField.getDocument();
        document.setDocumentFilter(new LimitLengthFilter(maxLength));
    }

    /**
     * Update the displayed label text
     */
    private void displayLabel()
    {
        String text = "";

        if (labelText != null)
        {
            text += labelText;
        }

        // We shouldn't add the placeholder text if the textField isn't empty,
        // or it has focus
        if (placeholderText != null &&
            mTextField.getText().isEmpty() &&
            !mTextField.isFocusOwner())
        {
            text += placeholderText;
        }

        mLeftLabel.setText(text);
        mLeftLabel.setVisible(true);
    }

    /**
     * Returns the text contained in the text field
     * @return The text in the text box
     */
    public String getText()
    {
        return mTextField.getText();
    }

    public void addDocumentListener(DocumentListener listener)
    {
        mTextField.getDocument().addDocumentListener(listener);
    }

    @Override
    public void setEnabled(boolean isEnabled)
    {
        super.setEnabled(isEnabled);
        setCursor(Cursor.getPredefinedCursor(isEnabled ? Cursor.TEXT_CURSOR :
                                                         Cursor.DEFAULT_CURSOR));
        mTextField.setEnabled(isEnabled);
        mTextField.setFocusable(isEnabled);
        repaint();
    }

    /**
     * Sets the text of the text field to the specified text
     * @return The text in the text box
     */
    public void setText(String text)
    {
        mTextField.setText(text);
        displayLabel();
    }

    /**
     * Check to see if a Point is within the panel
     * @param p The point to check for
     * @return true if the point is within the panel
     */
    @Override
    public boolean contains(Point p)
    {
        return getBounds().contains(p) ||
               mTextField.getBounds().contains(p) ||
               mLeftLabel.getBounds().contains(p);
    }

    /**
     * Adds an action listener to the text field
     *
     * @param listener the listener to be added
     */
    public void addActionListener(ActionListener listener)
    {
        logger.debug("Adding action listener to text field: " + listener);
        mTextField.addActionListener(listener);
    }

    /**
     * Adds a mouse listener to the text field and label
     *
     * @param listener the listener to be added
     */
    public void addMouseListener(MouseListener listener)
    {
        logger.debug("Adding mouse listener to SnakeTextEntry: " + listener);
        mTextField.addMouseListener(listener);
        mLeftLabel.addMouseListener(listener);
    }

    /**
     * Adds a key listener to the text field
     *
     * @param listener the listener to be added
     */
    public void addKeyListener(KeyListener listener)
    {
        logger.debug("Adding key listener to text field: " + listener);
        mTextField.addKeyListener(listener);
    }

    /**
     * Adds a focus listener to the text field
     *
     * @param listener the listener to be added
     */
    public void addFocusListener(FocusListener listener)
    {
        logger.debug("Adding focus listener to text field: " + listener);
        mTextField.addFocusListener(listener);
    }

    /**
     * Load an image from resources and return it
     *
     * @param res The resource of the image
     * @return The image or null if it was not found
     */
    private BufferedImageFuture loadImage(String res)
    {
        return sRes.getBufferedImage(res);
    }

    /**
     * Set the preferred size.
     *
     * This overrides the height as the pane should only be as tall as the
     * provided background image
     */
    @Override
    public void setPreferredSize(Dimension preferredSize)
    {
        super.setPreferredSize(
            new Dimension(preferredSize.width, mRight.resolve().getHeight(null)));
    }

    /**
     * Override doLayout so that each time this JLayeredPane is redrawn
     * e.g. from resize, we resize all its components so that they all fill
     * the entire extents of the pane
     * i.e. From (0,0) to (getWidth(), getHeight())
     */
    @Override
    public void doLayout()
    {
        super.doLayout();
        // Synchronizing on getTreeLock, because other layouts do that.
        // see BorderLayout::layoutContainer(Container)
        synchronized (getTreeLock())
        {
            // Update the dimensions of all components to the dimensions of the
            // pane
            int w = getWidth();
            int h = getHeight();
            for (Component c : getComponents())
            {
                c.setBounds(0, 0, w, h);
            }
        }
    }

    /**
     * To draw the snake background we need a custom background painter
     * The background looks like:
     *      left image - repeated central image - right image
     * @param g
     */
    private void paintBackground(Graphics g)
    {
        int x = 0;

        boolean isEnabled = isEnabled();
        Image left = (isEnabled ? mLeft : mLeftDisabled).resolve();
        Image middle = (isEnabled ? mMiddle : mMiddleDisabled).resolve();
        Image right = (isEnabled ? mRight : mRightDisabled).resolve();

        g.drawImage(left, x, 0, null);
        x += left.getWidth(null);

        int width = getWidth() - right.getWidth(null);
        while (x < width)
        {
            g.drawImage(middle, x, 0, null);
            x += middle.getWidth(null);
        }

        g.drawImage(right, x, 0, null);

        // Clip the graphics and ask the super class to draw the text and
        // cursor.  The clip is required to make the text be drawn in the
        // right place
        g = g.create(left.getWidth(null),
                     1,
                     getWidth() - 2 * right.getWidth(null),
                     getHeight() - 2);
    }
}
