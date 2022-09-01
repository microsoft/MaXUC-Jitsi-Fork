// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.*;

/**
 * A basic tree cell renderer that is more accessible to screen reading software
 * than the default tree cell renderer
 */
public class AccessibleContactListTreeCellRenderer
    implements ContactListTreeCellRenderer
{
    /**
     * The label component that displays the information for this cell
     */
    private JLabel component;

    @Override
    public synchronized Component getTreeCellRendererComponent(JTree tree, Object value,
        boolean selected, boolean expanded, boolean leaf, int row,
        boolean hasFocus)
    {
        String displayName = "";
        component = new JLabel();

        // Colour each row in alternate colours
        if (value instanceof ContactNode)
        {
            if (row % 2 == 0)
            {
                component.setBackground(Constants.CALL_HISTORY_EVEN_ROW_COLOR);
            }
            else
            {
                component.setBackground(Color.WHITE);
            }

            UIContactImpl contact
                = ((ContactNode) value).getContactDescriptor();

            displayName = contact.getDisplayName();
            component.setFont(component.getFont().deriveFont(Font.PLAIN, ScaleUtils.getDefaultFontSize()));
        }
        else if (value instanceof GroupNode)
        {
            displayName = ((GroupNode) value).getGroupDescriptor().getDisplayName();
            if (displayName == null)
                displayName = "";
            component.setFont(component.getFont().deriveFont(Font.BOLD, ScaleUtils.getDefaultFontSize()));
        }

        component.setFont(component.getFont().deriveFont(
            ScaleUtils.getScaledFontSize(20f)));

        component.setText(displayName);
        component.setOpaque(true);
        int componentHeight =
            (int) ComponentUtils.getStringSize(component, displayName).getHeight() +
                                                             ScaleUtils.scaleInt(8);

        component.setPreferredSize(new Dimension(component.getWidth(), componentHeight));

        // Colour the selected cell blue
        if (selected)
        {
            component.setBackground(Constants.SELECTED_COLOR);
            AccessibilityUtils.setNameAndDescription(
                tree,
                displayName,
                GuiActivator.getResources().getI18NString("service.gui.CONTACT_LIST_DESCRIPTION"));
        }

        return component;
    }

    @Override
    public void loadSkin()
    {
        // Nothing to do
    }

    @Override
    public void setMouseOverContact(Object element)
    {
        // Nothing to do
    }
}
