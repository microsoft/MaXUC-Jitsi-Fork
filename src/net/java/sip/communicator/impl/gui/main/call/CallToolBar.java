/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;

import javax.swing.*;

import org.jitsi.service.resources.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.util.*;

/**
 * The tool bar container shown in the call window.
 *
 * @author Lyubomir Marinov
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class CallToolBar
    extends OrderedTransparentPanel
{
    private static final long serialVersionUID = 0L;
    private static final int TOOL_BAR_BORDER = 2;

    private static final int TOOL_BAR_X_GAP = 3;

    private final BufferedImageFuture buttonDarkSeparatorImage
        = GuiActivator.getImageLoaderService().getImage(
                                ImageLoaderService.CALL_TOOLBAR_DARK_SEPARATOR);

    private final BufferedImageFuture buttonSeparatorImage
        = GuiActivator.getImageLoaderService().getImage(
                                     ImageLoaderService.CALL_TOOLBAR_SEPARATOR);

    /**
     * The indicator which determines whether this <tt>CallToolBar</tt> is
     * displayed in full-screen or windowed mode.
     */
    private boolean fullScreen;

    private final boolean incomingCall;

    private final Color toolbarColor;

    private final Color toolbarFullScreenColor;

    private final Color toolbarInCallBorderColor;

    private final Color toolbarInCallShadowColor;

    public CallToolBar(boolean fullScreen, boolean incomingCall)
    {
        this.fullScreen = fullScreen;
        this.incomingCall = incomingCall;

        ResourceManagementService res = GuiActivator.getResources();

        toolbarColor = new Color(res.getColor("service.gui.CALL_TOOL_BAR"));
        toolbarFullScreenColor
            = new Color(
                    res.getColor("service.gui.CALL_TOOL_BAR_FULL_SCREEN"));
        toolbarInCallBorderColor
            = new Color(
                    res.getColor("service.gui.IN_CALL_TOOL_BAR_BORDER"));
        toolbarInCallShadowColor
            = new Color(
                    res.getColor(
                            "service.gui.IN_CALL_TOOL_BAR_BORDER_SHADOW"));

        setBorder(
                BorderFactory.createEmptyBorder(
                        TOOL_BAR_BORDER,
                        TOOL_BAR_BORDER,
                        TOOL_BAR_BORDER,
                        TOOL_BAR_BORDER));
        setLayout(new FlowLayout(FlowLayout.CENTER, TOOL_BAR_X_GAP, 0));
    }

    /**
     * Determines whether this <tt>CallToolBar</tt> is displayed in full-screen
     * or windowed mode.
     *
     * @return <tt>true</tt> if this <tt>CallToolBar</tt> is displayed in
     * full-screen mode or <tt>false</tt> for windowed mode
     */
    public boolean isFullScreen()
    {
        return fullScreen;
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        boolean isAltCallPopup = ConfigurationUtils.isAltCallPopupStyle();

        g = g.create();
        try
        {
            AntialiasingManager.activateAntialiasing(g);

            int width = getWidth();
            int height = getHeight();

            if (incomingCall)
            {
                if (!isAltCallPopup)
                {
                    g.setColor(toolbarInCallShadowColor);
                    g.drawRoundRect(0, 0, width - 1, height - 2, 10, 10);

                    g.setColor(toolbarInCallBorderColor);
                    g.drawRoundRect(0, 0, width - 1, height - 3, 10, 10);
                }
            }
            else
            {
                g.setColor(fullScreen ? toolbarFullScreenColor : toolbarColor);
                g.fillRoundRect(0, 0, width, height, 10, 10);
            }

            if (!fullScreen)
            {
                // We add the border.
                int x
                    = CallToolBarButton.DEFAULT_WIDTH
                        + TOOL_BAR_BORDER
                        + TOOL_BAR_X_GAP;
                int endX = width - TOOL_BAR_BORDER - TOOL_BAR_X_GAP;
                Image separatorImage
                    = ((incomingCall && !isAltCallPopup)
                                          ? buttonDarkSeparatorImage
                                          : buttonSeparatorImage).resolve();

                while (x < endX)
                {
                    g.drawImage(
                            separatorImage,
                            x + 1,
                            (height - separatorImage.getHeight(this)) / 2,
                            this);

                    x += CallToolBarButton.DEFAULT_WIDTH + TOOL_BAR_X_GAP;
                }
            }
        }
        finally
        {
            g.dispose();
        }
    }

    /**
     * Sets the display of this <tt>CallToolBar</tt> to full-screen or windowed
     * mode.
     *
     * @param fullScreen <tt>true</tt> to set the display of this
     * <tt>CallToolBar</tt> to full-screen mode or <tt>false</tt> for windowed
     * mode
     */
    public void setFullScreen(boolean fullScreen)
    {
        if (this.fullScreen != fullScreen)
        {
            this.fullScreen = fullScreen;

            /*
             * The value of the fullScreen state of this CallToolBar affects its
             * painting.
             */
            if (isDisplayable())
                repaint();
        }
    }
}
