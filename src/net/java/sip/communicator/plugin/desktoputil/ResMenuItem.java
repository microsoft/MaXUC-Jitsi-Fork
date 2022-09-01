// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import javax.swing.*;

import org.jitsi.service.resources.ResourceManagementService;

/**
 * A ResMenuItem is a JMenuItem that we create from a resource string.
 * The resource string provides both the text of the item, and also any
 * mnemonic used for the item.
 */
public class ResMenuItem extends JMenuItem
{
    private static final ResourceManagementService resources =
            DesktopUtilActivator.getResources();

    public ResMenuItem(String key)
    {
        super(resources.getI18NString(key));
        this.setMnemonic(resources.getI18nMnemonic(key));

        // We want all our menu items to be scaled appropriately
        ScaleUtils.scaleFontAsDefault(this);
    }
}
