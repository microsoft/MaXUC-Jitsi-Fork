// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicButtonUI;

import org.jitsi.service.resources.ResourceManagementService;

public class SIPCommColoredTextButton extends JButton
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Class id key used in UIDefaults.
     */
    private static final String UIClassID = "BasicButtonUI";

    /**
     * Adds the ui class to UIDefaults.
     */
    static
    {
        UIManager.getDefaults().put(UIClassID,
            BasicButtonUI.class.getName());
    }

    /**
     * The ResourceManagementService
     */
    private static final ResourceManagementService sResources =
        DesktopUtilActivator.getResources();

    /**
     * The border radius to use for painting the component
     */
    private static final int sBorderRadius =
        DesktopUtilActivator.getConfigurationService().global().getInt(
            "net.java.sip.communicator.plugin.desktoputil.SIP_COMM_COLORED_TEXT_BUTTON_BORDER_RADIUS",
            10);

    private static final float[] sBorderColor
        = Color.DARK_GRAY.getRGBComponents(null);

    private Color mBgColor;
    private Color mRollOverColor;
    private Color mPressedColor;

    /**
     * Creates a <tt>SIPCommColoredTextButton</tt> with the text and color
     * specified by the property.
     *
     * @param text the text of the button
     * @param colorProperty the property to read from to get the colors of the
     *      button. The properties colorProperty, colorProperty_ROLLOVER, and
     *      colorProperty_PRESSED are used and must exist.
     */
    public SIPCommColoredTextButton(String text, String colorProperty)
    {
        this(
            text,
            new Color(sResources.getColor(colorProperty)),
            new Color(sResources.getColor(colorProperty + "_ROLLOVER")),
            new Color(sResources.getColor(colorProperty + "_PRESSED")));
    }

    /**
     * Creates a <tt>SIPCommColoredTextButton</tt> with the text and color
     * specified for the background when in default state, rollOver state and
     * pressed state.
     *
     * @param text the text of the button
     * @param bgColor the color to use for the default background color
     * @param rollOverColor the color to use for the background when rollover
     * @param pressedColor the color to use for the background when pressed
     */
    private SIPCommColoredTextButton(String text,
                                     Color bgColor,
                                     Color rollOverColor,
                                     Color pressedColor)
    {
        super(text);

        mBgColor = bgColor;
        mPressedColor = pressedColor;
        mRollOverColor = rollOverColor;

        MouseRolloverHandler mouseHandler = new MouseRolloverHandler();

        this.addMouseListener(mouseHandler);
        this.addMouseMotionListener(mouseHandler);

        this.setIcon(null);
        this.setIconTextGap(0);

        /*
         * Explicitly remove all borders that may be set from the current look
         * and feel.
         */
        this.setContentAreaFilled(false);
    }

    /**
     * Overrides the <code>paintComponent</code> method of <tt>JButton</tt> to
     * paint the button background and icon, and all additional effects of this
     * configurable button.
     *
     * @param g The Graphics object.
     */
    @Override
    protected void paintComponent(Graphics g)
    {
        Graphics2D g1 = (Graphics2D) g.create();
        try
        {
            internalPaintComponent(g1);
        }
        finally
        {
            g1.dispose();
        }

        super.paintComponent(g);
    }

    /**
     * Paints this button.
     * @param g the <tt>Graphics</tt> object used for painting
     */
    private void internalPaintComponent(Graphics2D g)
    {
        AntialiasingManager.activateAntialiasing(g);

        float visibility = this.getModel().isRollover() ? 0.5f : 0.0f;

        if (visibility != 0.0f)
        {
            g.setColor(new Color(sBorderColor[0], sBorderColor[1],
                    sBorderColor[2], visibility));

            g.fillRoundRect(0, 0,
                            this.getWidth(), this.getHeight(),
                            sBorderRadius, sBorderRadius);
        }

        Color colorToPaint = getBackground();

        if (this.getModel().isPressed() && mPressedColor != null)
        {
            colorToPaint = mPressedColor;
        }
        else if (this.getModel().isRollover() && mRollOverColor != null)
        {
            colorToPaint = mRollOverColor;
        }
        else if (mBgColor != null)
        {
            colorToPaint = mBgColor;
        }

        g.setColor(colorToPaint);
        g.fillRoundRect(1, 1,
                        this.getWidth() - 2, this.getHeight() - 2,
                        sBorderRadius, sBorderRadius);
    }

    /**
    * Returns the name of the L&F class that renders this component.
    *
    * @return the string "TreeUI"
    * @see JComponent#getUIClassID
    * @see UIDefaults#getUI
    */
   public String getUIClassID()
   {
       return UIClassID;
   }

    /**
     * Perform a rollover on mouse over.
     */
    private class MouseRolloverHandler
        implements  MouseListener,
                    MouseMotionListener
    {
        @Override
        public void mouseMoved(MouseEvent e)
        {
        }

        @Override
        public void mouseExited(MouseEvent e)
        {
            if (isEnabled())
            {
                getModel().setRollover(false);
            }
        }

        @Override
        public void mouseClicked(MouseEvent e)
        {
            if (isEnabled())
            {
                getModel().setRollover(false);
            }
        }

        @Override
        public void mouseEntered(MouseEvent e)
        {
            if (isEnabled())
            {
                getModel().setRollover(true);
            }
        }

        @Override
        public void mousePressed(MouseEvent e)
        {
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
        }

        @Override
        public void mouseDragged(MouseEvent e)
        {
        }
    }
}
