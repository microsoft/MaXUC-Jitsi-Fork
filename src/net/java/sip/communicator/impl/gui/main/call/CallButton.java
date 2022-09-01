/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.event.*;
import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.event.*;
import net.java.sip.communicator.service.analytics.*;
import net.java.sip.communicator.service.gui.UIService.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.skin.*;

import org.jitsi.service.resources.*;

/**
 *
 * @author Yana Stamcheva
 */
public class CallButton
    extends SIPCommButton
    implements  Skinnable,
                TextFieldChangeListener
{
    private static final long serialVersionUID = 0L;

    /**
     * The history icon.
     */
    private BufferedImageFuture image;

    /**
     * The history pressed icon.
     */
    private BufferedImageFuture pressedImage;

    /**
     * The rollover image.
     */
    private BufferedImageFuture rolloverImage;

    /**
     * The main application window.
     */
    private final MainFrame mainFrame;

    private final String defaultTooltip
        = GuiActivator.getResources().getI18NString(
            "service.gui.CALL_NAME_OR_NUMBER");

    /**
     * Creates the contact list call button.
     *
     * @param mainFrame the main window
     */
    public CallButton(final MainFrame mainFrame)
    {
        this.mainFrame = mainFrame;

        loadSkin();

        mainFrame.addSearchFieldListener(this);

        setToolTipText(defaultTooltip);

        addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                GuiActivator.getAnalyticsService().onEvent(AnalyticsEventType.OUTBOUND_CALL,
                                                           "Calling from",
                                                           "Dialer");
                List<ProtocolProviderService> telephonyProviders
                    = CallManager.getTelephonyProviders();

                String searchText = mainFrame.getCurrentSearchText();
                if (telephonyProviders.size() == 1)
                {
                    CallManager.createCall(
                        telephonyProviders.get(0),
                        searchText,
                        Reformatting.NOT_NEEDED);
                }
                else if (telephonyProviders.size() > 1)
                {
                    ChooseCallAccountPopupMenu chooseAccountDialog
                        = new ChooseCallAccountPopupMenu(
                            CallButton.this,
                            searchText,
                            telephonyProviders);

                    chooseAccountDialog
                        .setLocation(CallButton.this.getLocation());
                    chooseAccountDialog.showPopupMenu();
                }
            }
        });
    }

    /**
     * Loads button images.
     */
    public void loadSkin()
    {
        image
            = GuiActivator.getResources()
                .getBufferedImage("service.gui.buttons.CALL_NUMBER_BUTTON");

        pressedImage
            = GuiActivator.getResources()
                .getBufferedImage("service.gui.buttons.CALL_NUMBER_BUTTON_PRESSED");

        rolloverImage
            = GuiActivator.getResources()
                .getBufferedImage("service.gui.buttons.CALL_NUMBER_BUTTON_ROLLOVER");

        setBackgroundImage(image);
        setPressedImage(pressedImage);
        setRolloverImage(rolloverImage);
    }

    /**
     * Invoked when any text is inserted in the search field.
     */
    public void textInserted()
    {
        updateTooltip();
    }

    /**
     * Invoked when any text is removed from the search field.
     */
    public void textRemoved()
    {
        updateTooltip();
    }

    /**
     * Updates the tooltip of this button.
     */
    private void updateTooltip()
    {
        String searchText = mainFrame.getCurrentSearchText();

        if (searchText != null && searchText.length() > 0)
            setToolTipText(
                GuiActivator.getResources().getI18NString("service.gui.CALL")
                    + " " + searchText);
        else
            setToolTipText(defaultTooltip);
    }
}
