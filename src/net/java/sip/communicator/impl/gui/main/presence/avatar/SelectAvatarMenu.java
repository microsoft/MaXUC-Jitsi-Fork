/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.presence.avatar;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.*;

import org.jitsi.service.resources.BufferedImageFuture;
import org.jitsi.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.presence.avatar.imagepicker.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.analytics.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.account.*;

/**
 * The dialog used as menu.
 *
 * @author Damian Minkov
 */
public class SelectAvatarMenu
    extends SIPCommPopupMenu
    implements ActionListener, SelectAvatarService
{
    private static final long serialVersionUID = 0L;

    /**
     * Logger for this class.
     */
    private static final Logger logger =
            Logger.getLogger(SelectAvatarMenu.class);

    /**
     * Images shown as history.
     */
    private static final int MAX_STORED_IMAGES = 8;

    /**
     * Thumbnail width.
     */
    private static final int THUMB_WIDTH = 48;

    /**
     * Thumbnail height.
     */
    private static final int THUMB_HEIGHT = 48;

    /**
     * Buttons corresponding to images.
     * TODO: Although this field is declared, there is no code that actually initializes it, so it
     * would appear to be entirely redundant.
     * There is code that reads from it (in refreshRecentImages), but I have to assume this is never
     * called, otherwise it would be hitting NPE when accessing this uninitialized array.
     */
    private SIPCommButton[] recentImagesButtons =
        new SIPCommButton[MAX_STORED_IMAGES];

    /**
     * Next free image index number.
     */
    private int nextImageIndex = 0;

    /**
     * The parent button using us.
     */
    private FramedImageWithMenu avatarImage;

    /**
     * Creates the dialog.
     * @param avatarImage the button that will trigger this menu.
     */
    public SelectAvatarMenu(FramedImageWithMenu avatarImage)
    {
        this.avatarImage = avatarImage;

        init();

        this.pack();
    }

    /**
     * Init visible components.
     */
    private void init()
    {
        // Construct bold title
        JLabel titleLabel = new JLabel(GuiActivator.getResources().
            getI18NString("service.gui.avatar.AVATAR_MENU_NAME"));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, ScaleUtils.getDefaultFontSize()));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        setFont(titleLabel.getFont().deriveFont(
            Font.PLAIN, ScaleUtils.getScaledFontSize(11f)));

        Color oldSeparatorDefault = (Color) UIManager.getDefaults().
            get("Separator.foreground");

        if (OSUtils.IS_MAC && ConfigurationUtils.useNativeTheme())
        {
            // If using native theme for mac we need to be transparent so that the background can be
            // seen through it.
            // This is because the background is a gradient on mac.
            setOpaque(false);
            titleLabel.setOpaque(false);
        }
        else
        {
            // stop vertical separator from going through text
            titleLabel.setOpaque(true);

            // Change colour of custom separator to match look and feel
            UIManager.getDefaults().put(
                "Separator.foreground", new Color(226, 227, 227));
        }

        SIPCommMenuItem chooseButton = new SIPCommMenuItem(
            GuiActivator.getResources().getI18NString(
            "service.gui.avatar.CHOOSE_ICON"), (BufferedImageFuture) null);
        chooseButton.addActionListener(this);
        chooseButton.setName("chooseButton");

        SIPCommMenuItem removeButton = new SIPCommMenuItem(
            GuiActivator.getResources().getI18NString(
            "service.gui.avatar.REMOVE_ICON"), (BufferedImageFuture) null);
        removeButton.addActionListener(this);
        removeButton.setName("removeButton");

        // Add elements to menu
        this.add(titleLabel);
        this.add(new JSeparator(SwingConstants.HORIZONTAL));
        this.add(chooseButton);
        this.add(removeButton);

        UIManager.getDefaults().put(
            "Separator.foreground", oldSeparatorDefault);
    }

    /**
     * Refresh images with those stored locally.
     * TODO: This code is never called!
     */
    public void refreshRecentImages()
    {
        int i;

        for (i = 0; i < MAX_STORED_IMAGES; i++)
        {
            BufferedImage image = AvatarStackManager.loadImage(i);
            if (image == null)
                break;

            this.recentImagesButtons[i].setImage(new BufferedImageAvailable(createThumbnail(image)));
            this.recentImagesButtons[i].setEnabled(true);
        }

        if (i < MAX_STORED_IMAGES)
        {
            this.nextImageIndex = i;

            for (; i < MAX_STORED_IMAGES; i++)
            {
                this.recentImagesButtons[i].setImage(null);
                this.recentImagesButtons[i].setEnabled(false);
            }
        }
        else
            this.nextImageIndex = MAX_STORED_IMAGES;
    }

    /**
     * Create thumbnail for the image.
     * @param image to scale.
     * @return the thumbnail image.
     */
    private static BufferedImage createThumbnail(BufferedImage image)
    {
        int width = image.getWidth();
        int height = image.getHeight();

        // Image smaller than the thumbnail size
        if (width < THUMB_WIDTH && height < THUMB_HEIGHT)
            return image;

        Image i;

        if (width > height)
            i = image.getScaledInstance(THUMB_WIDTH, -1, Image.SCALE_SMOOTH);
        else
            i = image.getScaledInstance(-1, THUMB_HEIGHT, Image.SCALE_SMOOTH);

        return ImageUtils.getBufferedImage(i);
    }

    /**
     * Here is all the action. Stores the selected image into protocols and if
     * needed updates it in an AccountStatusPanel.
     *
     * @param image the new image.
     */
    private static void setNewImage(final BufferedImage image)
    {
        // Use separate thread to be sure we don't block UI thread.
        new Thread("SelectAvatarMenu.setNewImage")
        {
            public void run()
            {
                boolean success = false;
                boolean changeRejected = false;

                AccountManager accountManager
                        = GuiActivator.getAccountManager();

                for(AccountID accountID : accountManager.getStoredAccounts())
                {
                    if(accountManager.isAccountLoaded(accountID))
                    {
                        ProtocolProviderService protocolProvider
                            = AccountUtils.getRegisteredProviderForAccount(
                                accountID);

                        if(protocolProvider != null
                           && protocolProvider.isRegistered())
                        {
                            OperationSetAvatar opSetAvatar
                                = protocolProvider
                                    .getOperationSet(OperationSetAvatar.class);

                            if(opSetAvatar != null)
                            {
                                try
                                {
                                    logger.info("Setting avatar for account " +
                                                                     accountID);
                                    success = opSetAvatar.setAvatar(new BufferedImageAvailable(image));
                                    if (!success)
                                    {
                                        logger.error("Unspecified failure " +
                                            "setting avatar for account " +
                                            accountID);
                                    }
                                }
                                catch(Exception e)
                                {
                                    logger.error("Error setting image", e);
                                    if (e instanceof OperationFailedException &&
                                       ((OperationFailedException) e).getErrorCode() == 405)
                                    {
                                        // Change rejected by server.
                                        changeRejected = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                if (success)
                {
                    logger.debug("Sending analytic event for successful avatar change");

                    // Avatar change was accepted, so send analytic.
                    // Value varies based on whether this is a removal or just
                    // an update.
                    String paramValue = image == null?
                        AnalyticsParameter.VALUE_CHANGE_AVATAR_REMOVE :
                        AnalyticsParameter.VALUE_CHANGE_AVATAR_UPDATE;

                    AnalyticsService analyticsService = GuiActivator.getAnalyticsService();
                    analyticsService.onEvent(AnalyticsEventType.USER_CHANGE_AVATAR,
                                             AnalyticsParameter.NAME_CHANGE_AVATAR,
                                             paramValue);
                }
                else
                {
                    logger.error("Failed to change user avatar: " +
                                 "changeRejected=" + changeRejected);
                    showAvatarChangeError(changeRejected);
                }
            }
        }.start();
    }

    /**
     * Action performed on various action links(buttons).
     *
     * @param e the action.
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem source = (JMenuItem)(e.getSource());

        if (source.getName().equals("chooseButton"))
        {
            logger.user("Choose new avatar selected in Avatar menu");

            // Open the image picker
            if (innerHandleChooseAvatar())
            {
                return;
            }
        }
        else if (source.getName().equals("removeButton"))
        {
            logger.user("Remove current avatar selected in Avatar menu");
            // Removes the current photo.
            setNewImage(null);
        }

        setVisible(false);
    }

    @Override
    public void handleChooseAvatar()
    {
        logger.debug("Handling choose avatar - must switch from WISPA thread to UI thread");

        // This is called on a non UI thread, so switch to do the work on the UI one
        SwingUtilities.invokeLater(this::innerHandleChooseAvatar);
    }

    private boolean innerHandleChooseAvatar()
    {
        logger.debug("Handling choose avatar on UI thread");

        Image currentImage = this.avatarImage.getAvatar();

        ImagePickerDialog dialog = new ImagePickerDialog(96, 96);

        byte[] newImage = dialog.showDialog(currentImage);

        if (newImage == null)
        {
            logger.debug("Avatar selection was cancelled");
            return true;
        }

        logger.debug("A new avatar was chosen");

        // New image
        BufferedImage image = ImageUtils.getBufferedImage(
                new ImageIcon(newImage).getImage());

        // Store image
        if (this.nextImageIndex == MAX_STORED_IMAGES)
        {
            // No more place to store images
            // Pop the first element (index 0)
            AvatarStackManager.popFirstImage(MAX_STORED_IMAGES);

            this.nextImageIndex = MAX_STORED_IMAGES - 1;
        }

        // Store the new image on hard drive
        AvatarStackManager.storeImage(image, this.nextImageIndex);

        // Inform protocols about the new image
        setNewImage(image);
        return false;
    }

    /**
     * Display an error showing that the avatar could not be modified.
     *
     * @param changeRejected Whether the failure was due to the remote server
     * not supporting avatar changes (true if so).
     */
    private static void showAvatarChangeError(boolean changeRejected)
    {
        // The title of the error dialog shown when we fail to update the avatar.
        final String AVATAR_SET_ERROR_TITLE =
                GuiActivator.getResources().getI18NString(
                        "service.gui.avatar.AVATAR_CHANGE_ERROR_TITLE");

        // The error message shown when we fail to update the avatar for an unknown reason.
        final String AVATAR_SET_ERROR_MSG_GENERIC =
                GuiActivator.getResources().getI18NString(
                        "service.gui.avatar.AVATAR_CHANGE_ERROR_MSG_GENERIC");

        // The error message shown when we fail to update the avatar because the
        // server did not allow the change (405 not-allowed).
        final String AVATAR_SET_ERROR_MSG_REJECTED =
                GuiActivator.getResources().getI18NString(
                        "service.gui.avatar.AVATAR_CHANGE_ERROR_MSG_REJECTED");

        final String errorText = changeRejected ?
            AVATAR_SET_ERROR_MSG_REJECTED : AVATAR_SET_ERROR_MSG_GENERIC;

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                new ErrorDialog(
                      null, AVATAR_SET_ERROR_TITLE, errorText).setVisible(true);
            }
        });
    }
}