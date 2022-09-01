// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.util.Timer;

import javax.swing.*;
import javax.swing.border.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * A component for displaying the state of media in a call.
 */
class MediaStateComponent extends TransparentPanel
                          implements FocusListener
{
    private static final Logger sLog = Logger.getLogger(MediaStateComponent.class);
    private static final long serialVersionUID = 1L;

    /**
     * The frame rate (milliseconds per frame) of the pulse animation
     */
    private static final int FRAME_LENGTH = 50;

    /**
     * Resource service
     */
    private static final ResourceManagementService sResources =
                                                GuiActivator.getResources();

    /**
     * Image tool tip when there is low audio
     */
    private static final String TOOLTIP_LOW =
                              sResources.getI18NString("service.gui.LOW_AUDIO");

    /**
     * Image tool tip when the mic is muted
     */
    private static final String TOOLTIP_MUTED =
                        sResources.getI18NString("service.gui.LOW_AUDIO_MUTED");

    /**
     * Image tool tip when outbound network is bad
     */
    private static final String TOOLTIP_OUTBOUND_PACKET_LOSS =
        sResources.getI18NString("service.gui.OUTBOUND_PACKET_LOSS");

    /**
     * Image tool tip when inbound network is bad
     */
    private static final String TOOLTIP_INBOUND_PACKET_LOSS =
        sResources.getI18NString("service.gui.INBOUND_PACKET_LOSS");

    // Images used by this component:
    private static final BufferedImageFuture LOW          = sResources.getBufferedImage("service.gui.icons.audiowarning.LOW");
    private static final BufferedImageFuture LOW_ROLLOVER = sResources.getBufferedImage("service.gui.icons.audiowarning.LOW_ROLLOVER");
    private static final BufferedImageFuture LOW_PRESSED  = sResources.getBufferedImage("service.gui.icons.audiowarning.LOW_PRESSED");
    private static final BufferedImageFuture LOW_PULSE    = sResources.getBufferedImage("service.gui.icons.audiowarning.LOW_PULSE");

    private static final BufferedImageFuture MUTED          = sResources.getBufferedImage("service.gui.icons.audiowarning.MUTED");
    private static final BufferedImageFuture MUTED_ROLLOVER = sResources.getBufferedImage("service.gui.icons.audiowarning.MUTED_ROLLOVER");
    private static final BufferedImageFuture MUTED_PRESSED  = sResources.getBufferedImage("service.gui.icons.audiowarning.MUTED_PRESSED");
    private static final BufferedImageFuture MUTED_PULSE    = sResources.getBufferedImage("service.gui.icons.audiowarning.MUTED_PULSE");

    private static final BufferedImageFuture BAD_NETWORK          = sResources.getBufferedImage("service.gui.icons.audiowarning.BAD_NETWORK");
    private static final BufferedImageFuture BAD_NETWORK_ROLLOVER = sResources.getBufferedImage("service.gui.icons.audiowarning.BAD_NETWORK_ROLLOVER");
    private static final BufferedImageFuture BAD_NETWORK_PRESSED  = sResources.getBufferedImage("service.gui.icons.audiowarning.BAD_NETWORK_PRESSED");
    private static final BufferedImageFuture BAD_NETWORK_PULSE    = sResources.getBufferedImage("service.gui.icons.audiowarning.BAD_NETWORK_PULSE");

    /**
     * The opacity of the overlay
     */
    private float mOpacityOverlay;

    /**
     * The timer controlling the animation of the overlay
     */
    private final Timer mTimer = new Timer("Overlay animation timer");

    /**
     * The tooltip replacement shown by this window.  We use a replacement as
     * it gives better control over placement, styling and when it appears.
     */
    private JWindow mToolTip;

    /*
     * The tooltip text
     */
    private String mTooltipText = "";

    /*
     * The current icon.
     */
    private BufferedImageFuture mImage;

    /*
     * The current pulse icon
     */
    private BufferedImageFuture mPulseImage;

    /**
     * The media state that this component is currently representing
     */
    private int mMediaState;

    /*
     * Whether we have breached the threshold of outbound packet loss
     */
    private boolean mOutboundPacketLoss = false;

    /*
     * Whether we have breached the threshold of inbound packet loss
     */
    private boolean mInboundPacketLoss = false;

    /*
     * Whether or not we will show the bad network icon (we don't for
     * external customers- just for internals).
     */
    private boolean mBadNetworkWarningEnabled =
        GuiActivator.getConfigurationService().global().getBoolean(
            "net.java.sip.communicator.plugin.errorreport.diagnostics.BAD_NETWORK_WARNING", false);

    /**
     * True if the mouse is pressed
     */
    private boolean mMousePressed = false;

    /**
     * True if the mouse is over this component
     */
    private boolean mMouseOver = false;

    /**
     * Create an instance of the MediaStateComponent
     */
    public MediaStateComponent()
    {
        // Assume good media to start with - set state to high media.
        setMediaState(CallChangeEvent.HIGH_MEDIA);

        // Add a focus listener so we show the correct graphic when this
        // component has focus
        addFocusListener(this);

        Dimension size = new Dimension(LOW.resolve().getWidth(null), LOW.resolve().getHeight(null));
        setPreferredSize(size);
        setMinimumSize(size);

        // Add a click listener:
        addMouseListener(new MouseListener()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                doAction();
            }

            @Override
            public void mouseEntered(MouseEvent e)
            {
                // Force the replacement tooltip to show - we use a replacement
                // to give us better control of the look and display
                sLog.trace("Mouse entering");
                getToolTip().setVisible(true);
                mMouseOver = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                // Force the replacement tooltip to hide.
                sLog.trace("Mouse exit");
                getToolTip().setVisible(false);
                mMouseOver = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e)
            {
                mMousePressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                mMousePressed = false;
                repaint();
            }
        });

        addKeyListener(new KeyListener()
        {
            @Override
            public void keyPressed(KeyEvent evt)
            {
                int keyCode = evt.getKeyCode();
                if (keyCode == KeyEvent.VK_SPACE ||
                    keyCode == KeyEvent.VK_ENTER)
                {
                    doAction();
                }
            }

            @Override
            public void keyReleased(KeyEvent e){}

            @Override
            public void keyTyped(KeyEvent e){}
        });

        // Create a timer to make the icon flash
        mTimer.schedule(new TimerTask()
        {
            private boolean opacityIncreasing;

            @Override
            public void run()
            {
                if (mOpacityOverlay >= 0.95)
                    opacityIncreasing = false;
                if (mOpacityOverlay <= 0.05)
                    opacityIncreasing = true;

                if (opacityIncreasing)
                    mOpacityOverlay += 0.05;
                else
                    mOpacityOverlay -= 0.05;

                repaint();
            }
        }, FRAME_LENGTH, FRAME_LENGTH);
    }

    /**
     * Performs the action when this component is clicked or activated with the
     * keyboard
     */
    protected void doAction()
    {
        sLog.user("Warning in call window clicked on");

        // Only activate the click when there is a media volume issue.
        if (mMediaState != CallChangeEvent.HIGH_MEDIA)
        {
            // Display the audio config panel:
            ConfigurationContainer container =
                    GuiActivator.getUIService().getConfigurationContainer();
            container.setVisible(true);
            container.setSelected(
                sResources.getI18NString("impl.neomedia.configform.AUDIO"));
        }
    }

    /**
     * Update the media state and redraw the UI if the new state has caused a
     * change.
     *
     * @param mediaState The new media state
     */
    public void setMediaState(int mediaState)
    {
        sLog.info("Updating call media state to " + mediaState);

        if (mediaState != mMediaState)
        {
            mMediaState = mediaState;
            recalculateUI();
        }
    }

    /**
     * Update the outbound packet loss and redraw the UI if the new state has caused a
     * change.
     */
    public void setOutboundPacketLoss(boolean outboundPacketLoss)
    {
        sLog.info("Updating outbound packet loss to " + outboundPacketLoss);

        if ((outboundPacketLoss != mOutboundPacketLoss) &&
            (mBadNetworkWarningEnabled))
        {
            mOutboundPacketLoss = outboundPacketLoss;
            recalculateUI();
        }
    }

    /**
     * Update the inbound packet loss and redraw the UI if the new state has caused a
     * change.
     */
    public void setInboundPacketLoss(boolean inboundPacketLoss)
    {
        sLog.info("Updating inbound packet loss to " + inboundPacketLoss);

        if ((inboundPacketLoss != mInboundPacketLoss) &&
            (mBadNetworkWarningEnabled))
        {
            mInboundPacketLoss = inboundPacketLoss;
            recalculateUI();
        }
    }

    /*
     * Following a state change update the UI to match the new state.
     */
    public void recalculateUI()
    {
        // We aren't actually expecting to have network issues (which we will
        // only spot after a quite a few seconds), and muted mic (which we
        // will only spot at the start of a call) at the same time.  However,
        // we'll say Muted Mic beats the Network issues.
        boolean noError = false;
        if (mMediaState != CallChangeEvent.HIGH_MEDIA)
        {
            mTooltipText = (mMediaState == CallChangeEvent.NO_MEDIA) ?
                TOOLTIP_MUTED : TOOLTIP_LOW;
            // Determine the image to draw
            if (mMediaState == CallChangeEvent.NO_MEDIA)
            {
                mImage = mMousePressed ? MUTED_PRESSED :
                        mMouseOver ? MUTED_ROLLOVER :
                        MUTED;
                mPulseImage = MUTED_PULSE;
            }
            else
            {
                mImage = mMousePressed ? LOW_PRESSED :
                        mMouseOver ? LOW_ROLLOVER :
                        LOW;
                mPulseImage = LOW_PULSE;
            }
        }
        else if (mOutboundPacketLoss || mInboundPacketLoss)
        {
            mTooltipText = mInboundPacketLoss ?
                TOOLTIP_INBOUND_PACKET_LOSS : TOOLTIP_OUTBOUND_PACKET_LOSS;
            mImage = mMousePressed ? BAD_NETWORK_PRESSED :
                mMouseOver ? BAD_NETWORK_ROLLOVER : BAD_NETWORK;
            mPulseImage = BAD_NETWORK_PULSE;
        }
        else
        {
            noError = true;
        }

        if (noError)
        {
            setVisible(false);
            if (mToolTip != null)
            {
                mToolTip.setVisible(false);
            }
        }
        else
        {
            // Show the icon, and briefly show the tooltip so the user
            // understands what this UI represents
            setVisible(true);
            removeToolTip();
            final JWindow toolTip = getToolTip();

            if (toolTip != null)
            {
                toolTip.setVisible(true);
                mTimer.schedule(new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        toolTip.setVisible(false);
                    }
                }, 3000);
            }
        }
    }

    /**
     * Dispose of this window when it is no longer required
     */
    public void dispose()
    {
        mTimer.cancel();

        if (mToolTip != null)
            mToolTip.setVisible(false);
    }

    /**
     * @return the tooltip, creating it if necessary
     */
    private JWindow getToolTip()
    {
        if (mToolTip == null)
        {
            // Create the tool tip with a some padding around it, and a 1 pixel
            // black border round the edge of that
            mToolTip = new JWindow();
            JTextArea text = new JTextArea(mTooltipText);
            Border border = BorderFactory.createCompoundBorder(
                                 BorderFactory.createLineBorder(Color.BLACK, 1),
                                 BorderFactory.createEmptyBorder(2, 2, 2, 2));
            text.setBorder(border);
            mToolTip.add(text);
            mToolTip.setAlwaysOnTop(true);

            // Position the tooltip according to the location of the call window
            // Add some padding to make it appears above the icon
            final Window window = SwingUtilities.getWindowAncestor(this);
            if ((window == null) || (!window.isVisible()))
            {
                // Nothing to do if the owner window isn't showing.
                mToolTip = null;
                return null;
            }

            Point point = window.getLocationOnScreen();
            int x = point.x + window.getWidth() - 60;
            int y = point.y + 5;
            int width = ComponentUtils.getStringWidth(this, mTooltipText) + 8;
            mToolTip.setBounds(x, y, width, 23);

            // If the window moves, then dismiss the tooltip
            window.addComponentListener(new ComponentAdapter()
            {
                public void componentMoved(ComponentEvent e)
                {
                    removeToolTip();
                    window.removeComponentListener(this);
                }
            });

            // Make sure the tooltip is hidden when the window closes:
            window.addWindowListener(new WindowAdapter()
            {
                @Override
                public void windowClosed(WindowEvent e)
                {
                    removeToolTip();
                    window.removeWindowListener(this);
                }
            });
        }

        return mToolTip;
    }

    @Override
    public void paint(Graphics g)
    {
        // Draw the image:
        g.drawImage(mImage.resolve(), 0, 0, null);

        BufferedImage bufferedImage =
                             new BufferedImage(getSize().width,
                                               getSize().height,
                                               BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bufferedImage.createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                    mOpacityOverlay));
        g2d.drawImage(mPulseImage.resolve(), 0, 0, null);
        g.drawImage(bufferedImage, 0, 0, null);
    }

    @Override
    public void setVisible(boolean visible)
    {
        if (!isEnabled())
        {
            return;
        }

        super.setVisible(visible);

        if (!visible && mToolTip != null)
            mToolTip.setVisible(false);
    }

    /**
     * Remove the currently showing tool tip - dismissing it if it is already
     * showing
     */
    private void removeToolTip()
    {
        if (mToolTip != null)
            mToolTip.setVisible(false);

        mToolTip = null;
    }

    @Override
    public void focusGained(FocusEvent e)
    {
        mMouseOver = true;
        repaint();
    }

    @Override
    public void focusLost(FocusEvent e)
    {
        mMouseOver = false;
        repaint();
    }
}
