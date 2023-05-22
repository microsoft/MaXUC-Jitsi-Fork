/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.analytics.*;

/**
 * The <tt>DialpadDialog</tt> is a popup dialog containing a dialpad.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 */
public class DialpadDialog
    extends JDialog
    implements WindowFocusListener
{
    private static final long serialVersionUID = 0L;

    /**
     * The component for which we are created
     */
    private Component mOwner = null;

    /**
     * Component listener for listening to window moved events
     */
    private final ComponentListener mComponentListener = new ComponentListener()
    {
        public void componentShown(ComponentEvent e)
        {
            setVisible(true);
        }

        public void componentResized(ComponentEvent e)
        {
            setVisible(true);
        }

        public void componentMoved(ComponentEvent e)
        {
            setVisible(true);
        }

        public void componentHidden(ComponentEvent e)
        {
            setVisible(false);
        }
    };

    /**
     * Window listener for listening to window hidden events
     */
    private final WindowAdapter mWindowListener = new WindowAdapter()
    {
        @Override
        public void windowDeactivated(WindowEvent e)
        {
            setVisible(false);
        }
    };

    /**
     * Creates a new instance of this class using the specified
     * <tt>dialPanel</tt>.
     *
     * @param dialPanel the <tt>DialPanel</tt> that we'd like to wrap.
     */
    private DialpadDialog(DialPanel dialPanel)
    {
        dialPanel.setOpaque(false);

        BackgroundPanel bgPanel = new BackgroundPanel();

        bgPanel.setLayout(new BorderLayout());
        bgPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bgPanel.add(dialPanel, BorderLayout.CENTER);

        Container contentPane = getContentPane();

        contentPane.setLayout(new BorderLayout());
        contentPane.add(bgPanel, BorderLayout.CENTER);

        this.setUndecorated(true);
        this.setFocusableWindowState(false);

        // Prevent the window from being blocked by modal dialogs - the parent
        // CallDialog is, and so all child UI should be as well.
        this.setModalExclusionType(
                                 Dialog.ModalExclusionType.APPLICATION_EXCLUDE);

        this.pack();
    }

    /**
     * Creates an instance of the <tt>DialpadDialog</tt>.
     *
     * @param dtmfHandler handles DTMFs.
     * @param owner The component that created us
     */
    public DialpadDialog(final DTMFHandler dtmfHandler, Component owner)
    {
        this(new DialPanel(dtmfHandler));
        mOwner = owner;

        this.setModal(false);

        addWindowListener(
                new WindowAdapter()
                {
                    @Override
                    public void windowClosed(WindowEvent e)
                    {
                        GuiActivator.getAnalyticsService()
                                   .onEvent(AnalyticsEventType.DIALPAD_CLOSED);
                        dtmfHandler.removeParent(DialpadDialog.this);
                    }

                    @Override
                    public void windowOpened(WindowEvent e)
                    {
                        GuiActivator.getAnalyticsService()
                                   .onEvent(AnalyticsEventType.DIALPAD_OPENED);
                        dtmfHandler.addParent(DialpadDialog.this);
                    }
                });
    }

    /**
     * New panel used as background for the dialpad which would be painted with
     * round corners and a gradient background.
     */
    private static class BackgroundPanel extends JPanel
    {
        private static final long serialVersionUID = 0L;

        /**
         * Calls <tt>super</tt>'s <tt>paintComponent</tt> method and then adds
         * background with gradient.
         *
         * @param g a reference to the currently valid <tt>Graphics</tt> object
         */
        @Override
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            int width = getWidth();
            int height = getHeight();

            GradientPaint p = new GradientPaint(width / 2, 0,
                    Constants.GRADIENT_DARK_COLOR, width / 2,
                    height,
                    Constants.GRADIENT_LIGHT_COLOR);

            g2.setPaint(p);

            g2.fillRoundRect(0, 0, width, height, 10, 10);

            g2.setColor(Constants.GRADIENT_DARK_COLOR);

            g2.drawRoundRect(0, 0, width - 1, height - 1, 10, 10);
        }
    }

    /**
     * Dummy implementation.
     *
     * @param e unused
     */
    public void windowGainedFocus(WindowEvent e)
    {
    }

    /**
     * Dummy implementation.
     *
     * @param e unused
     */
    public void windowLostFocus(WindowEvent e)
    {
        this.removeWindowFocusListener(this);
        this.setVisible(false);
    }

    @Override
    public void setVisible(boolean visible)
    {
        if (mOwner != null)
        {
            // We have an owner thus make sure that we are positioned sensibly
            // relative to it
            Window parentWindow = SwingUtilities.getWindowAncestor(mOwner);

            if (visible)
            {
                pack();
                Point location = new Point(mOwner.getX(),
                                           mOwner.getY() + mOwner.getHeight());
                SwingUtilities.convertPointToScreen(location,
                                                    mOwner.getParent());

                setLocation((int) location.getX() + 2,
                            (int) location.getY() + 2);

                if (!isVisible())
                {
                    // We are being made visible, add some listeners to ensure
                    // that we behave sensibly when the parent is dismissed or
                    // moves.
                    parentWindow.addComponentListener(mComponentListener);
                    parentWindow.addWindowListener(mWindowListener);
                    addWindowFocusListener(this);
                }
            }
            else if (isVisible())
            {
                // We are being made invisible, remove the listeners as they are
                // not required.
                parentWindow.removeComponentListener(mComponentListener);
                parentWindow.removeWindowListener(mWindowListener);
                removeWindowFocusListener(this);
            }
        }

        super.setVisible(visible);
    }
}
