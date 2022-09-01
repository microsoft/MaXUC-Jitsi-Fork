// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.callpark;

import java.awt.*;

import javax.swing.*;
import javax.swing.tree.*;

import net.java.sip.communicator.util.*;

/**
 * Class responsible for the display of each entry in the Tree of Call Park
 * Orbits. Note that there is only one renderer per list of call park orbits.
 */
public class CallParkCellRenderer implements TreeCellRenderer
{
    /**
     * The call park component that actually does the rendering
     */
    private final CallParkComponent mComponent = new CallParkComponent();

    /**
     * The object that is currently being edited
     */
    private Object mEditingObject = null;

    /**
     * True if the object we are editing is expanded
     */
    private boolean mEditExpanded = false;

    /**
     * The call park window that contains the tree that this is the renderer for
     */
    private final CallParkWindow mCallParkWindow;

    public CallParkCellRenderer(CallParkWindow callParkWindow)
    {
        mCallParkWindow = callParkWindow;
    }

    @Override
    public synchronized Component getTreeCellRendererComponent(final JTree tree,
                                                  Object value,
                                                  boolean selected,
                                                  boolean expanded,
                                                  boolean leaf,
                                                  final int row,
                                                  boolean hasFocus)
    {
        // Simply tell the call park component to update itself
        mComponent.updateDisplay(value, selected, expanded, row);

        boolean valueChanged     = mEditingObject != value;
        boolean expansionChanged = !leaf && (mEditExpanded != expanded);

        if (selected && (valueChanged || expansionChanged))
        {
            // This object is selected and either the value has changed, or the
            // expanded flag has changed.  Make sure that we edit it.
            mEditExpanded = expanded;
            mEditingObject = value;

            // Make sure we edit on the EDT, as otherwise can get display bugs
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    tree.startEditingAtPath(tree.getUI().getPathForRow(tree, row));
                }
            });
        }

        if (selected)
        {
            String accessibleDescription = mComponent.getLeftLabelText() + " " +
                                           mComponent.getRightLabelText() + " " +
                                           mComponent.getActionState();
            AccessibilityUtils.setName(
                tree,
                accessibleDescription);
            mCallParkWindow.setSelectedRow(row);
        }

        return mComponent;
    }
}
