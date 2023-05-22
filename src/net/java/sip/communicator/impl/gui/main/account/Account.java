/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.account;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;

import org.jitsi.service.resources.*;

/**
 * Represents an account in the account list.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class Account
{
    /**
     * The corresponding protocol provider.
     */
    private ProtocolProviderService protocolProvider;

    /**
     * The identifier of the account.
     */
    private final AccountID accountID;

    /**
     * The display name of the account
     */
    private final String name;

    /**
     * The icon of the image.
     */
    private ImageIconFuture icon;

    /**
     * Indicates if the account is enabled.
     */
    private boolean isEnabled;

    /**
     * Creates an <tt>Account</tt> instance from the given
     * <tt>protocolProvider</tt>.
     * @param protocolProvider the protocol provider on which this account is
     * based
     */
    public Account(ProtocolProviderService protocolProvider)
    {
        this.protocolProvider = protocolProvider;

        this.accountID = protocolProvider.getAccountID();

        this.name = accountID.getDisplayName();

        this.icon = getProtocolIcon(protocolProvider);

        this.isEnabled = accountID.isEnabled();
    }

    /**
     * Creates an account object with the given <tt>accountName</tt> and
     * <tt>icon</tt>.
     * @param accountID the identifier of the account
     */
    public Account(AccountID accountID)
    {
        this.accountID = accountID;

        this.name = accountID.getDisplayName();

        String iconPath = accountID.getAccountPropertyString(
            ProtocolProviderFactory.ACCOUNT_ICON_PATH);

        if (iconPath != null)
            this.icon = GuiActivator.getImageLoaderService().getIconFromPath(iconPath);

        this.isEnabled = accountID.isEnabled();
    }

    /**
     * Returns the protocol provider, on which this account is based.
     * @return the protocol provider, on which this account is based
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return protocolProvider;
    }

    /**
     * Returns the account identifier.
     * @return the account identifier
     */
    public AccountID getAccountID()
    {
        return accountID;
    }

    /**
     * Returns the account name.
     * @return the account name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns the protocol name of the account.
     * @return the protocol name of the account
     */
    public String getProtocolName()
    {
        return accountID.getProtocolDisplayName();
    }

    /**
     * The icon of the account.
     * @return the icon of the account
     */
    public ImageIconFuture getIcon()
    {
        return icon;
    }

    /**
     * Returns the status name.
     * @return the status name
     */
    public String getStatusName()
    {
        if (protocolProvider != null)
            return getAccountStatus(protocolProvider);
        else
            return GlobalStatusEnum.OFFLINE_STATUS;
    }

    /**
     * Returns the status icon of this account.
     * @return the status icon of this account
     */
    public ImageIconFuture getStatusIcon()
    {
        if (protocolProvider != null)
            return GuiActivator.getImageLoaderService().getAccountStatusImage(
                                                               protocolProvider);
        else if (icon != null)
        {
            icon = GuiActivator.getImageLoaderService().getIconFromPath(
                accountID.getAccountPropertyString(
                    ProtocolProviderFactory.ACCOUNT_ICON_PATH));

            BufferedImageFuture scaledImage
                = icon.getImage().scaleImageWithinBounds(16, 16);

            if (scaledImage != null)
                return new ImageIconAvailable(
                    new ImageIcon(GrayFilter.createDisabledImage(scaledImage.resolve())));
        }
        return null;
    }

    /**
     * Returns <tt>true</tt> to indicate that this account is enabled,
     * <tt>false</tt> - otherwise.
     * @return <tt>true</tt> to indicate that this account is enabled,
     * <tt>false</tt> - otherwise
     */
    public boolean isEnabled()
    {
        return isEnabled;
    }

    /**
     * Returns the current presence status of the given protocol provider.
     *
     * @param protocolProvider the protocol provider which status we're looking
     * for.
     * @return the current presence status of the given protocol provider.
     */
    private String getAccountStatus(ProtocolProviderService protocolProvider)
    {
        String status;

        // If our account doesn't have a registered protocol provider we return
        // offline.
        if (protocolProvider == null)
            return GuiActivator.getResources()
            .getI18NString("service.gui.OFFLINE");

        OperationSetPresence presence
            = protocolProvider.getOperationSet(OperationSetPresence.class);

        if (presence != null)
        {
            status = presence.getPresenceStatus().getStatusName();
        }
        else
        {
            status = GuiActivator.getResources()
                        .getI18NString( protocolProvider.isRegistered()
                                        ? "service.gui.ONLINE"
                                        : "service.gui.OFFLINE");
        }

        return status;
    }

    /**
     * Returns the protocol icon. If an icon 32x32 is available, returns it,
     * otherwise tries to scale a bigger icon if available. If we didn't find
     * a bigger icon to scale, we return null.
     *
     * @param protocolProvider the protocol provider, which icon we're looking
     * for
     * @return the protocol icon
     */
    private ImageIconFuture getProtocolIcon(ProtocolProviderService protocolProvider)
    {
        ProtocolIcon protocolIcon = protocolProvider.getProtocolIcon();
        BufferedImageFuture protocolImage
            = protocolIcon.getIcon(ProtocolIcon.ICON_SIZE_32x32);

        if (protocolImage != null)
        {
            return protocolImage.getImageIcon();
        }
        else
        {
            protocolImage
                = protocolIcon.getIcon(ProtocolIcon.ICON_SIZE_48x48);

            if (protocolImage == null)
                protocolImage
                    = protocolIcon.getIcon(ProtocolIcon.ICON_SIZE_64x64);

            if (protocolImage != null)
                return protocolImage.scaleIconWithinBounds(32, 32);
        }

        return null;
    }

    /**
     * Sets the given <tt>protocolProvider</tt> to this account.
     * @param protocolProvider the <tt>ProtocolProviderService</tt>
     * corresponding to this account
     */
    public void setProtocolProvider(ProtocolProviderService protocolProvider)
    {
        this.protocolProvider = protocolProvider;
    }

    /**
     * Sets the <tt>isDisabled</tt> property.
     * @param isEnabled indicates if this account is currently
     * <tt>disabled</tt>
     */
    public void setEnabled(boolean isEnabled)
    {
        this.isEnabled = isEnabled;
    }

    /**
     * Returns the string representation of this account.
     *
     * @return the string representation of this account
     */
    public String toString()
    {
        return protocolProvider.getAccountID().getDisplayName();
    }
}
