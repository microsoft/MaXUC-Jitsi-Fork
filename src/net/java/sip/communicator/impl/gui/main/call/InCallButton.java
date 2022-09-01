// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;

/**
 * In Call Buttons are placed in the Call Frame, they all have a similar
 * styled UI
 */
public class InCallButton extends JButton
                          implements FocusListener,
                                     ChangeListener
{
    private static final long serialVersionUID = 0L;

    /**
     * The normal image
     */
    private ImageIconFuture imageNormal;

    /**
     * The focus image
     */
    private ImageIconFuture imageFocus;

    /**
     * The active image
     */
    private ImageIconFuture imageActive;

    /**
     * The active focus image
     */
    private ImageIconFuture imageActiveFocus;

    /**
     * Creates an in call button that is displayed in the in-call window
     *
     * @param imageNormal the normal button image
     * @param imageRollover the image to use on mouse over
     * @param imageFocus the image to use when this button has focus
     * @param imagePressed the image to use when the button is pressed
     * @param imageActive the image to use when this button is toggled on
     * @param imageActiveRollover the image to use when the button is toggled
     * on and has the mouse over
     * @param imageActiveFocus the image to use when the button is toggled on
     * and has focus
     * @param imageDisabled the image to use when the button is disabled
     * @param toolTipText the tooltip for this button
     */
    public InCallButton(ImageIconFuture imageNormal,
                        ImageIconFuture imageRollover,
                        ImageIconFuture imageFocus,
                        ImageIconFuture imagePressed,
                        ImageIconFuture imageActive,
                        ImageIconFuture imageActiveRollover,
                        ImageIconFuture imageActiveFocus,
                        ImageIconFuture imageDisabled,
                        String toolTipText)
    {
        this.imageNormal = imageNormal;
        this.imageFocus = imageFocus;
        this.imageActive = imageActive;
        this.imageActiveFocus = imageActiveFocus;

        addFocusListener(this);
        addChangeListener(this);

        // Set up the UI for this button
        setContentAreaFilled(false);
        setBackground(null);
        setFocusPainted(false);
        setBorder(null);
        setBorderPainted(false);
        setToolTipText(toolTipText);

        if (imageNormal != null)
        {
            setPreferredSize(new Dimension(imageNormal.resolve().getIconWidth(),
                                           imageNormal.resolve().getIconHeight()));
            imageNormal.addToButton(this);
        }

        if (imageRollover != null)
        {
            imageRollover.addToButtonAsRollover(this);
        }

        if (imagePressed != null)
        {
            imagePressed.addToButtonAsPressed(this);
        }

        if (imageActive != null)
        {
            imageActive.addToButtonAsSelected(this);
        }

        if (imageActiveRollover != null)
        {
            imageActiveRollover.addToButtonAsRolloverSelected(this);
        }

        if (imageDisabled != null)
        {
            imageDisabled.addToButtonAsDisabled(this);
            imageDisabled.addToButtonAsDisabledSelected(this);
        }
    }

    /**
     * Creates an in call button that is displayed in the in-call window
     *
     * @param imageNormal the normal button image ID
     * @param imageRollover the image ID to use on mouse over
     * @param imageFocus the image ID to use when this button has focus
     * @param imagePressed the image ID to use when the button is pressed
     * @param imageActive the image ID to use when this button is toggled on
     * @param imageActiveRollover the image ID to use when the button is toggled
     * on and has the mouse over
     * @param imageActiveFocus the image ID to use when the button is toggled on
     * and has focus
     * @param imageDisabled the image ID to use when the button is disabled
     * @param toolTipText the tooltip for this button
     */
    public InCallButton(ImageID imageNormal,
                        ImageID imageRollover,
                        ImageID imageFocus,
                        ImageID imagePressed,
                        ImageID imageActive,
                        ImageID imageActiveRollover,
                        ImageID imageActiveFocus,
                        ImageID imageDisabled,
                        String toolTipText)
    {
        this(getImageIcon(imageNormal),
             getImageIcon(imageRollover),
             getImageIcon(imageFocus),
             getImageIcon(imagePressed),
             getImageIcon(imageActive),
             getImageIcon(imageActiveRollover),
             getImageIcon(imageActiveFocus),
             getImageIcon(imageDisabled),
             toolTipText);
    }

    /**
     * Gets an Image Icon from the Image Loader
     *
     * @return the image icon
     */
    private static ImageIconFuture getImageIcon(ImageID imageID)
    {
        if (imageID == null)
        {
            return null;
        }

        return GuiActivator.getImageLoaderService().getImage(imageID).getImageIcon();
    }

    @Override
    public void focusGained(FocusEvent evt)
    {
        updateState();
    }

    @Override
    public void focusLost(FocusEvent evt)
    {
        updateState();
    }

    @Override
    public void stateChanged(ChangeEvent evt)
    {
        if (this.equals(evt.getSource()))
        {
            updateState();
        }
    }

    private void updateState()
    {
        if (hasFocus())
        {
            if (isSelected() && imageActiveFocus != null)
            {
                imageActiveFocus.addToButtonAsSelected(this);
            }
            else if (imageFocus != null)
            {
                imageFocus.addToButton(this);
            }
        }
        else
        {
            if (imageActive != null)
            {
                imageActive.addToButtonAsSelected(this);
            }
            else
            {
                imageNormal.addToButton(this);
            }
        }
    }

    @Override
    public void setToolTipText(String tooltip)
    {
        super.setToolTipText(tooltip);
        AccessibilityUtils.setName(this, tooltip);
    }
}
