// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import java.awt.*;

import javax.accessibility.*;
import javax.swing.*;

import org.jitsi.service.resources.ResourceManagementService;

/**
 * A collection of useful utilities for dealing with Accessibility.
 */
public class AccessibilityUtils
{
    /**
     * Set the accessible name of a component
     *
     * @param component the component for which to set the accessible
     *        name
     * @param name the name to set for this component
     */
    public static void setName(Component component, String name)
    {
        // In normal use, we don't expect the component to be null, but when UTs
        // are being run, it sometimes is due to incomplete mocking, so the simplest
        // workaround is to handle that here.
        if (component != null)
        {
            AccessibleContext accessibleContext = component.getAccessibleContext();
            if (accessibleContext != null)
            {
                accessibleContext.setAccessibleName(name);
            }
        }
    }

    /**
     * Set the accessible description of a component
     *
     * @param component the component for which to set the accessible
     *        description
     * @param description the description to set for this component
     */
    public static void setDescription(Component component,
                                      String description)
    {
        if (component != null)
        {
            AccessibleContext accessibleContext = component.getAccessibleContext();
            if (accessibleContext != null)
            {
                accessibleContext.setAccessibleDescription(description);
            }
        }
    }

    /**
     * Set the accessible name and description of a component
     *
     * @param component the component for which to set the accessible
     *        name and description
     * @param name the name to set for this component
     * @param description the description to set for this component
     */
    public static void setNameAndDescription(Component component,
                                             String name,
                                             String description)
    {
        setName(component, name);
        setDescription(component, description);
    }

    public static void reflectSelectionState(JMenuItem menuItem)
    {
        // For a menu item, JAWS will read out the text of the menu item, or
        // we can override that by setting an accessible name
        String name = menuItem.getText();
        if (menuItem.isSelected())
        {
            final ResourceManagementService resources = UtilActivator.getResources();
            name = resources.getI18NString("service.gui.accessibility.MENU_ITEM_SELECTED",
                                                  new String[] {name});
        }

        setName(menuItem, name);
    }
}
