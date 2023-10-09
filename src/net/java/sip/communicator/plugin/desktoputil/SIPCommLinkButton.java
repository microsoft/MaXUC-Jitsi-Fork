/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.net.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.plaf.*;

/**
 * A button which text is a link. The button looks like a link.
 */
public class SIPCommLinkButton
    extends JButton
{
    private static final long serialVersionUID = 1L;

    /**
     * Class id key used in UIDefaults.
     */
    private static final String UIClassID = "LinkButtonUI";

    /**
     * Adds the ui class to UIDefaults.
     */
    static
    {
        UIManager.getDefaults().put(UIClassID,
            SIPCommLinkButtonUI.class.getName());
    }

    public static final int ALWAYS_UNDERLINE = 0;

    public static final int HOVER_UNDERLINE = 1;

    private int linkBehavior;

    private Color linkColor;

    private Color colorPressed;

    private Color visitedLinkColor;

    private Color disabledLinkColor;

    private URL buttonURL;

    private boolean isLinkVisited;

    /**
     * Created Link Button with text and url.
     * @param text
     * @param url
     */
    public SIPCommLinkButton(String text, URL url)
    {
        super(text);

        linkBehavior = SIPCommLinkButton.HOVER_UNDERLINE;

        linkColor = Color.blue;
        colorPressed = Color.red;
        visitedLinkColor = new Color(128, 0, 128);

        if (text == null && url != null)
          this.setText(url.toExternalForm());
        setLinkURL(url);

        this.setBorderPainted(false);
        this.setContentAreaFilled(false);
        this.setRolloverEnabled(true);
        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public String getUIClassID()
    {
        return SIPCommLinkButton.UIClassID;
    }

    /**
     * Setup the tooltip.
     */
    protected void setupToolTipText()
    {
        String tip = null;
        if (buttonURL != null)
            tip = buttonURL.toExternalForm();
        setToolTipText(tip);
    }

    /**
     * Returns the link behaviour.
     * @return the link behaviour.
     */
    public int getLinkBehavior()
    {
        return linkBehavior;
    }

    /**
     * Return the link color.
     * @return link color.
     */
    public Color getLinkColor()
    {
        return linkColor;
    }

    /**
     * Returns the active link color.
     * @return the active link color.
     */
    public Color getActiveLinkColor()
    {
        return colorPressed;
    }

    /**
     * Returns the disabled link color.
     * @return the disabled link color.
     */
    public Color getDisabledLinkColor()
    {
        return disabledLinkColor;
    }

    /**
     * Returns visited link color.
     * @return visited link color.
     */
    public Color getVisitedLinkColor()
    {
        return visitedLinkColor;
    }

    /**
     * Set a link.
     * @param url the url.
     */
    public void setLinkURL(URL url)
    {
        URL urlOld = buttonURL;
        buttonURL = url;
        setupToolTipText();
        firePropertyChange("linkURL", urlOld, url);
        revalidate();
        repaint();
    }

    /**
     * Returns is link visited.
     * @return is link visited.
     */
    public boolean isLinkVisited()
    {
        return isLinkVisited;
    }
}
