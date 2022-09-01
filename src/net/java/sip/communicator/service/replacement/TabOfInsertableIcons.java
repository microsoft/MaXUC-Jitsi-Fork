// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.replacement;

import java.util.List;

import javax.swing.ImageIcon;

/**
 * A tab of icons to be displayed by a selector box
 */
public class TabOfInsertableIcons
{
    /**
     * The list of InsertableIcons to be displayed in this tab, in the order they
     * need to appear on the tab.
     */
    List<InsertableIcon> iconList;

    /**
     * The InsertableIcon to be displayed on the tab-selector (e.g. a picture
     * of a cat for the animal emojis tab)
     */
    ImageIcon iconToDisplayOnTab;

    /**
     * The constructor for this object.
     * @param iconList
     * @param iconToDisplayOnTab
     */
    public TabOfInsertableIcons(List<InsertableIcon> iconList, ImageIcon iconToDisplayOnTab)
    {
        this.iconList = iconList;
        this.iconToDisplayOnTab = iconToDisplayOnTab;
    }

    /**
     * Returns the iconList of a TabofInsertableIcons object.
     * @return iconList
     */
    public List<InsertableIcon> getIconList()
    {
        return this.iconList;
    }

    /**
     * Returns the iconToDisplayOnTab of a TabOfInsertableIcons object (e.g.
     * picture of a cat for the animal emojis tab).
     * @return iconToDisplayOnTab
     */
    public ImageIcon getIconToDisplayOnTab()
    {
        return this.iconToDisplayOnTab;
    }
}
