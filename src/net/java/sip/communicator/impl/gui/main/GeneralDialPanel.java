/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>DialPanel</tt> is the panel that contains the buttons to dial a
 * phone number.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class GeneralDialPanel
    extends TransparentPanel
    implements  MouseListener,
                Skinnable
{
    private static final long serialVersionUID = 0L;
    private static final Logger sLogger = Logger.getLogger(
        GeneralDialPanel.class);

    /**
     * The dial panel.
     */
    private final JPanel dialPadPanel =
        new TransparentPanel(new GridLayout(4, 3,
            GuiActivator.getResources()
                .getSettingsInt("impl.gui.DIAL_PAD_HORIZONTAL_GAP"),
            GuiActivator.getResources()
                .getSettingsInt("impl.gui.DIAL_PAD_VERTICAL_GAP")));

    /**
     * Handles DTMFs.
     */
    private DTMFHandler dtmfHandler;

    /**
     * The listener for button presses
     */
    private final DialPadButtonListener dialButtonListener;

    /**
     * Creates an instance of <tt>DialPanel</tt> for a specific call, by
     * specifying the parent <tt>CallManager</tt> and the
     * <tt>CallPeer</tt>.
     *
     * @param dtmfHandler handles DTMFs.
     */
    public GeneralDialPanel(DialPadButtonListener dialButtonListener,
                            DTMFHandler dtmfHandler)
    {
        this.dialButtonListener = dialButtonListener;
        this.dtmfHandler = dtmfHandler;

        this.init();
    }

    /**
     * Initializes this panel by adding all dial buttons to it.
     */
    public void init()
    {
        this.dialPadPanel.setOpaque(false);

        this.setBorder(BorderFactory.createEmptyBorder());

        loadSkin();

        this.add(dialPadPanel, BorderLayout.CENTER);
    }

    /**
     * Creates DTMF button.
     *
     * @param imageID
     * @param rolloverImageID
     * @param pressedImageID
     * @param name
     * @return the created dial button
     */
    private JButton createMacOSXDialButton( ImageID imageID,
                                            ImageID rolloverImageID,
                                            ImageID pressedImageID,
                                            String name)
    {
        ImageLoaderService imageLoaderService = GuiActivator.getImageLoaderService();
        JButton button = new SIPCommButton(
            imageLoaderService.getImage(imageID),
            imageLoaderService.getImage(rolloverImageID),
            imageLoaderService.getImage(pressedImageID),
            null,
            null,
            null);

        button.setName(name);
        button.addMouseListener(this);

        return button;
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    /**
     * Handles the <tt>MouseEvent</tt> triggered when user presses one of the
     * dial buttons.
     * @param e the event
     */
    @Override
    public void mousePressed(MouseEvent e)
    {
        JButton button = (JButton) e.getSource();
        sLogger.user("Main dial-pad button " + button.getName() + " clicked");

        dialButtonListener.dialButtonPressed(button.getName());
        dtmfHandler.startSendingDtmfTone(button.getName());
    }

    /**
     * Handles the <tt>MouseEvent</tt> triggered when user releases one of the
     * dial buttons.
     * @param e the event
     */
    @Override
    public void mouseReleased(MouseEvent e)
    {
        sLogger.debug("User released main dialpad button");
        dtmfHandler.stopSendingDtmfTone();
    }

    /**
     * Paints the main background image to the background of this dial panel.
     *
     * @param g the <tt>Graphics</tt> object used for painting
     */
    @Override
    public void paintComponent(Graphics g)
    {
     // do the superclass behavior first
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        boolean isTextureBackground
            = Boolean.parseBoolean(GuiActivator.getResources()
            .getSettingsString("impl.gui.IS_CONTACT_LIST_TEXTURE_BG_ENABLED"));

        BufferedImage bgImage
            = GuiActivator.getImageLoaderService().getImage(
                                     ImageLoaderService.MAIN_WINDOW_BACKGROUND).resolve();

        // paint the image
        if (bgImage != null)
        {
            if (isTextureBackground)
            {
                Rectangle rect
                    = new Rectangle(0, 0,
                            bgImage.getWidth(null),
                            bgImage.getHeight(null));

                TexturePaint texture = new TexturePaint(bgImage, rect);

                g2.setPaint(texture);

                g2.fillRect(0, 0, this.getWidth(), this.getHeight());
            }
            else
            {
                g.setColor(new Color(
                    GuiActivator.getResources()
                        .getColor("contactListBackground")));

                // paint the background with the choosen color
                g.fillRect(0, 0, getWidth(), getHeight());

                g2.drawImage(bgImage,
                        this.getWidth() - bgImage.getWidth(),
                        this.getHeight() - bgImage.getHeight(),
                        this);
            }
        }
    }

    /**
     * Reloads dial buttons.
     */
    public void loadSkin()
    {
        dialPadPanel.removeAll();

        DTMFHandler.DTMFToneInfo[] availableTones = DTMFHandler.AVAILABLE_TONES;

        for (int i = 0; i < availableTones.length; i++)
        {
            DTMFHandler.DTMFToneInfo info = availableTones[i];

            // we add only buttons having image
            if(info.imageID == null)
                continue;

            JComponent c = createMacOSXDialButton(info.macImageID,
                                                  info.macImageRolloverID,
                                                  info.macImagePressedID,
                                                  info.tone.getValue());

            dialPadPanel.add(c);
        }
    }
}
