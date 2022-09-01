// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.presence;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.jitsi.service.configuration.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.presence.avatar.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.imageloader.*;

public class AvatarImageButton
    extends FramedImageWithMenu
{
    private static final long serialVersionUID = 0L;

    /**
     * The desired height of the avatar.
     */
    private static final int AVATAR_ICON_HEIGHT = ScaleUtils.scaleInt(45);

    /**
     * The desired width of the avatar.
     */
    private static final int AVATAR_ICON_WIDTH = ScaleUtils.scaleInt(45);

    /**
     * The suffix to account config strings that indicate whether the
     * configured account is disabled.
     */
    private static final String ACCOUNT_DISABLED_SUFFIX = ".IS_ACCOUNT_DISABLED";

    /**
     * The prefix of the config string used by the reconnect plugin to track
     * whether accounts are online.
     */
    private static final String RECONNECTPLUGIN_PREFIX =
        "net.java.sip.communicator.plugin.reconnectplugin." +
            "ATLEAST_ONE_SUCCESSFUL_CONNECTION.";

    /**
     * The title of the error dialog shown when the button is clicked but the
     * user is not signed into chat.
     */
    private static final String NOT_SIGNED_IN_TITLE =
        GuiActivator.getResources().getI18NString(
                                      "service.gui.avatar.NOT_SIGNED_IN_TITLE");

    /**
     * The error message shown when the button is clicked but the user is not
     * signed into chat.
     */
    private static final String NOT_SIGNED_IN_MSG =
        GuiActivator.getResources().getI18NString(
                                        "service.gui.avatar.NOT_SIGNED_IN_MSG");

    /**
     * The prefix of all keys in the config that relate to jabber accounts.
     */
    private static final String JABBER_ACC_CONFIG_PREFIX =
        "net.java.sip.communicator.impl.protocol.jabber";

    /**
     * The configuration service.
     */
    private final ConfigurationService configService =
        GuiActivator.getConfigurationService();

    public AvatarImageButton(JFrame parent)
    {
        super(parent,
              GuiActivator.getImageLoaderService()
              .getImage(ImageLoaderService.DEFAULT_USER_PHOTO)
              .getImageIcon()
              .resolve(),
              AVATAR_ICON_WIDTH,
              AVATAR_ICON_HEIGHT);

        SelectAvatarMenu sam = new SelectAvatarMenu(this);
        setPopupMenu(sam);

        GuiActivator.bundleContext.registerService(SelectAvatarService.class.getName(),
                                                   sam,
                                                   null);
    }

    public void updateImage(ImageIcon img)
    {
        setImageIcon(img.getImage());
        setMaximumSize(
            new Dimension(AVATAR_ICON_WIDTH, AVATAR_ICON_HEIGHT));
        revalidate();
        repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        if (hasOnlineChatAccount())
        {
            super.mouseReleased(e);
        }
        else
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    new ErrorDialog(null,
                                    NOT_SIGNED_IN_TITLE,
                                    NOT_SIGNED_IN_MSG).setVisible(true);
                }
            });
        }
    }

    /**
     * Determine whether the user has any IM accounts that currently have
     * connectivity.
     *
     * @return True if the user has at least one IM account that is currently
     * connected and signed in.
     */
    private boolean hasOnlineChatAccount()
    {
        // First build a list of all configured Jabber accounts.
        List<String> potentialJabberAccs =
            configService.user().getPropertyNamesByPrefix(JABBER_ACC_CONFIG_PREFIX ,
                                                   true);

        List<String> jabberAccs = new ArrayList<>();

        for (String potentialAcc : potentialJabberAccs)
        {
            String value = configService.user().getString(potentialAcc);

            if (value != null && value.startsWith("acc"))
            {
                jabberAccs.add(potentialAcc);
            }
        }

        for (String account : jabberAccs)
        {
            String disabledProperty = account + ACCOUNT_DISABLED_SUFFIX;
            String disabled = configService.user().getString(disabledProperty);

            if (disabled != null && Boolean.valueOf(disabled))
            {
                // This account is disabled in config, so it can't be connected.
                // Skip it.
                continue;
            }

            String propertyName = account + ".ACCOUNT_UID";
            String accountUID = configService.user().getString(propertyName);

            String accountConnectedConfig =
                RECONNECTPLUGIN_PREFIX + accountUID;

            if (configService.user().getBoolean(accountConnectedConfig, true))
            {
                // The account is connected, so we know at least 1 IM account is
                // online, and there's nothing else we need to check.
                return true;
            }
        }

        return false;
    }
}
