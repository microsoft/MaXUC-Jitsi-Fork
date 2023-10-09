/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil.plaf;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalTextFieldUI;
import javax.swing.text.JTextComponent;

import org.jitsi.service.resources.BufferedImageFuture;

import net.java.sip.communicator.plugin.desktoputil.AntialiasingManager;
import net.java.sip.communicator.plugin.desktoputil.DesktopUtilActivator;
import net.java.sip.communicator.plugin.desktoputil.SIPCommButton;
import net.java.sip.communicator.plugin.desktoputil.ScaleUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.skin.Skinnable;

/**
 * SIPCommTextFieldUI implementation.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class SIPCommTextFieldUI
    extends MetalTextFieldUI
    implements Skinnable,
               MouseMotionListener,
               MouseListener
{
    /**
     * The logger.
     */
    private static final Logger logger = Logger.getLogger(SIPCommTextFieldUI.class);

    /**
     * Indicates if the mouse is currently over the delete button.
     */
    protected boolean isDeleteMouseOver = false;

    /**
     * Indicates if the mouse is currently pressed on the delete button.
     */
    protected boolean isDeleteMousePressed = false;

    /**
     * The gap between the delete button and the text in the field.
     */
    protected static int BUTTON_GAP = ScaleUtils.scaleInt(5);

    /**
     * The image of the delete text button.
     */
    private BufferedImageFuture deleteButtonImg;

    /**
     * The rollover image of the delete text button.
     */
    private BufferedImageFuture deleteButtonRolloverImg;

    /**
     * The image for the pressed state of the delete text button.
     */
    private BufferedImageFuture deleteButtonPressedImg;

    /**
     * Indicates if the text field contains a
     * delete button allowing to delete all the content at once.
     */
    private boolean isDeleteButtonEnabled = false;

    /**
     * The delete text button shown on the right of the field.
     */
    protected SIPCommButton deleteButton;

    /**
     * Indicates if the delete icon is visible.
     */
    private boolean isDeleteIconVisible = false;

    /**
     * The start background gradient color.
     */
    private Color bgStartColor
        = new Color(DesktopUtilActivator.getResources().getColor(
            "service.gui.LIGHT_BACKGROUND"));

    /**
     * The end background gradient color.
     */
    private Color bgEndColor
        = new Color(DesktopUtilActivator.getResources().getColor(
            "service.gui.LIGHT_BACKGROUND"));

    /**
     * The start background gradient color.
     */
    private Color bgBorderStartColor
        = new Color(DesktopUtilActivator.getResources().getColor(
            "service.gui.BORDER_SHADOW"));

    /**
     * The end background gradient color.
     */
    private Color bgBorderEndColor
        = new Color(DesktopUtilActivator.getResources().getColor(
            "service.gui.BORDER_SHADOW"));

    /**
     * Creates a <tt>SIPCommTextFieldUI</tt>.
     */
    public SIPCommTextFieldUI()
    {
        loadSkin();
    }

    /**
     * Updates the isDeleteButtonEnabled field.
     *
     * @param isDeleteButtonEnabled indicates if the delete buttons is enabled
     * or not
     */
    public void setDeleteButtonEnabled(boolean isDeleteButtonEnabled)
    {
        this.isDeleteButtonEnabled = isDeleteButtonEnabled;
    }

    /**
     * Adds the custom mouse listeners defined in this class to the installed
     * listeners.
     */
    protected void installListeners()
    {
        super.installListeners();

        getComponent().addMouseListener(this);

        getComponent().addMouseMotionListener(this);
    }

    /**
     * Uninstalls listeners for the UI.
     */
    protected void uninstallListeners()
    {
        super.uninstallListeners();

        getComponent().removeMouseListener(this);
        getComponent().removeMouseMotionListener(this);
    }

    /**
     * Paints the background of the associated component.
     * @param g the <tt>Graphics</tt> object used for painting
     */
    protected void customPaintBackground(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g.create();

        try
        {
            String roundedSet = DesktopUtilActivator.getResources().
            getSettingsString(
                    "impl.gui.IS_SIP_COMM_TEXT_FIELD_ROUNDED");

            boolean isRounded = true;

            if(roundedSet != null)
            {
                isRounded = Boolean.valueOf(roundedSet);
            }

            AntialiasingManager.activateAntialiasing(g2);
            JTextComponent c = this.getComponent();

            GradientPaint bgGradientColor =
                new GradientPaint(  c.getWidth() / 2, 0,
                                    bgStartColor,
                                    c.getWidth() / 2, c.getHeight(),
                                    bgEndColor);

            g2.setPaint(bgGradientColor);

            if(isRounded)
            {
                g2.fillRoundRect(1, 1, c.getWidth() - 1, c.getHeight() - 1,
                        8, 8);
            }
            else
            {
                g2.fillRect(1, 1, c.getWidth() - 1, c.getHeight() - 1);
            }

            drawDeleteIcon(g2, c);

            g2.setStroke(new BasicStroke(1f));
            GradientPaint bgBorderGradientColor
                = new GradientPaint(  c.getWidth() / 2, 0,
                                    bgBorderStartColor,
                                    c.getWidth() / 2, c.getHeight(),
                                    bgBorderEndColor);

            g2.setPaint(bgBorderGradientColor);

            if(isRounded)
            {
                g2.drawRoundRect(
                    0, 0, c.getWidth() - 1, c.getHeight() - 1, 8, 8);
            }
            else
            {
                g2.drawRect(0, 0, c.getWidth() - 1, c.getHeight() - 1);
            }
        }
        finally
        {
            g2.dispose();
        }
    }

    /**
     * Draw the delete icon if it is required
     *
     * @param g the <tt>Graphics</tt> object used for painting
     * @param c the text component
     */
    protected void drawDeleteIcon(Graphics2D g, JTextComponent c)
    {
        Rectangle deleteButtonRect = getDeleteButtonRect();

        if(deleteButtonRect != null)
        {
            int dx = deleteButtonRect.x;
            int dy = deleteButtonRect.y;

            if (c.getText() != null &&
                c.getText().length() > 0 &&
                isDeleteButtonEnabled)
            {
                if (isDeleteMousePressed)
                    g.drawImage(deleteButtonPressedImg.resolve(), dx, dy, null);
                if (isDeleteMouseOver)
                    g.drawImage(deleteButtonRolloverImg.resolve(), dx, dy, null);
                else
                    g.drawImage(deleteButtonImg.resolve(), dx, dy, null);

                isDeleteIconVisible = true;
            }
            else
                isDeleteIconVisible = false;
        }
        else
        {
            isDeleteIconVisible = false;
            intervalLogNullObject("drawDeleteIcon");
        }
    }

    /**
     * Updates the delete icon, changes the cursor and deletes the content of
     * the associated text component when the mouse is pressed over the delete
     * icon.
     *
     * @param evt the mouse event that has prompted us to update the delete
     * icon.
     */
    protected void updateDeleteIcon(MouseEvent evt)
    {
        // If the component is null we have nothing more to do here. Fixes a
        // NullPointerException in getDeleteButtonRectangle().
        JTextComponent component = getComponent();
        if (component == null)
        {
            intervalLogNullObject("updateDeleteIcon");
            return;
        }

        int x = evt.getX();
        int y = evt.getY();

        if (!isDeleteButtonEnabled)
            return;

        Rectangle deleteRect = getDeleteButtonRect();

        if (isDeleteIconVisible && deleteRect.contains(x, y))
        {
            if (evt.getID() == MouseEvent.MOUSE_PRESSED)
            {
                isDeleteMouseOver = false;
                isDeleteMousePressed = true;
            }
            else
            {
                isDeleteMouseOver = true;
                isDeleteMousePressed = false;
            }

            component.setCursor(Cursor.getDefaultCursor());

            if (evt.getID() == MouseEvent.MOUSE_CLICKED)
                component.setText("");
        }
        else
        {
            isDeleteMouseOver = false;
            isDeleteMousePressed = false;
        }

        component.repaint();
    }

    /**
     * Calculates the delete button rectangle.
     *
     * @return the delete button rectangle
     */
    protected Rectangle getDeleteButtonRect()
    {
        JTextComponent c = getComponent();

        if (c == null)
        {
            intervalLogNullObject("getDeleteButtonRect");
            return null;
        }

        Rectangle rect = c.getBounds();

        int dx = rect.width - deleteButton.getWidth() - BUTTON_GAP;
        int dy = rect.height / 2 - deleteButton.getHeight()/2;

        return new Rectangle(   dx,
                                dy,
                                deleteButton.getWidth(),
                                deleteButton.getHeight());
    }

    /**
     * If we are in the case of disabled delete button, we simply call the
     * parent implementation of this method, otherwise we recalculate the editor
     * rectangle in order to leave place for the delete button.
     * @return the visible editor rectangle
     */
    protected Rectangle getVisibleEditorRect()
    {
        if (!isDeleteIconVisible)
        {
            return super.getVisibleEditorRect();
        }

        JTextComponent c = getComponent();

        if (c == null)
        {
            intervalLogNullObject("getVisibleEditorRect");
            return null;
        }

        Rectangle alloc = c.getBounds();

        if ((alloc.width > 0) && (alloc.height > 0))
        {
            alloc.x = alloc.y = 0;
            Insets insets = c.getInsets();
            alloc.x += insets.left;
            alloc.y += insets.top;
            alloc.width -= insets.left + insets.right
                + getDeleteButtonRect().getWidth();
            alloc.height -= insets.top + insets.bottom;
            return alloc;
        }

        return null;
    }

    /**
     * Reloads skin information.
     */
    public void loadSkin()
    {
        deleteButtonImg = DesktopUtilActivator.getResources()
            .getBufferedImage("service.gui.lookandfeel.DELETE_TEXT_ICON");

        deleteButtonRolloverImg = DesktopUtilActivator.getResources()
            .getBufferedImage("service.gui.lookandfeel.DELETE_TEXT_ROLLOVER_ICON");

        deleteButtonPressedImg = DesktopUtilActivator.getResources()
            .getBufferedImage("service.gui.lookandfeel.DELETE_TEXT_PRESSED_ICON");

        if(deleteButton != null)
        {
            deleteButton.setBackgroundImage(deleteButtonImg);
            deleteButton.setRolloverImage(deleteButtonRolloverImg);
            deleteButton.setPressedImage(deleteButtonPressedImg);
        }
        else
        {
            deleteButton = new SIPCommButton(
                    deleteButtonImg,
                    deleteButtonRolloverImg,
                    deleteButtonPressedImg,
                    null, null, null);
        }

        deleteButton.setSize (  deleteButtonImg.resolve().getWidth(null),
                                deleteButtonImg.resolve().getHeight(null));
    }

    /**
     * Updates the delete icon when the mouse was clicked.
     * @param e the <tt>MouseEvent</tt> that notified us of the click
     */
    @Override
    public void mouseClicked(MouseEvent e)
    {
        updateDeleteIcon(e);
        updateCursor(e);
    }

    /**
     * Updates the delete icon when the mouse is enters the component area.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    @Override
    public void mouseEntered(MouseEvent e)
    {
        updateDeleteIcon(e);
        updateCursor(e);
    }

    /**
     * Updates the delete icon when the mouse exits the component area.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    @Override
    public void mouseExited(MouseEvent e)
    {
        updateDeleteIcon(e);
        updateCursor(e);
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        updateDeleteIcon(e);
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        updateDeleteIcon(e);
    }

    /**
     * Updates the delete icon when the mouse is dragged over.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    @Override
    public void mouseDragged(MouseEvent e)
    {
        updateDeleteIcon(e);
        updateCursor(e);
    }

    /**
     * Updates the delete icon when the mouse is moved over.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    @Override
    public void mouseMoved(MouseEvent e)
    {
        updateDeleteIcon(e);
        updateCursor(e);
    }

    /**
     * Updates the cursor type depending on a given <tt>MouseEvent</tt>.
     *
     * @param mouseEvent the <tt>MouseEvent</tt> on which the cursor depends
     */
    private void updateCursor(MouseEvent mouseEvent)
    {
        Rectangle rect = getVisibleEditorRect();
        if (rect != null && rect.contains(mouseEvent.getPoint()))
        {
            getComponent().setCursor(
                Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        }
    }

    /**
     * Creates a UI for a SIPCommTextFieldUI.
     *
     * @param c the text field
     * @return the UI
     */
    public static ComponentUI createUI(JComponent c)
    {
        return new SIPCommTextFieldUI();
    }

    /**
     * The delete button sometimes is not painted when it should be in the
     * search field (see SFR 501286).  We don't have a reliable repro for this
     * and the current diags are not useful, as there was no logging in this
     * class or SearchFieldUI.  There are a lot of places in these classes
     * where we used to silently do nothing when an object is unexpectedly
     * null, which may be the cause of this SFR.  This method has been added
     * to write an interval log when that happens.
     *
     * @param instanceHit A string identifying where this bug was hit.  Used
     * as the tag for the interval log to ensure that that we write a log for
     * each place in which we find a null object, but we don't write multiple,
     * spammy logs for the same null instance.
     */
    protected synchronized void intervalLogNullObject(String instanceHit)
    {
        logger.interval(instanceHit,
                        "An object is unexpectedly null in " + instanceHit);
    }
}
