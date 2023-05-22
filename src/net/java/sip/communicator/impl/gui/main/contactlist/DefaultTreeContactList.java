/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.lookandfeel.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

/**
 * DeafultContactlist used to display <code>JList</code>s with contacts.
 *
 * @author Damian Minkov
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class DefaultTreeContactList
    extends JTree
    implements Skinnable
{
    private static final long serialVersionUID = 0L;

    /**
     * Class id key used in UIDefaults.
     */
    private static final String sUiClassID =
        DefaultTreeContactList.class.getName() +  "TreeUI";

    /**
     * The name of the contact drag and drop enabled property
     */
    public static final String CONTACTLIST_DND_ENABLED
                            = "impl.gui.dnd.MERGE_ENABLED";

    /**
     * Adds the ui class to UIDefaults.
     */
    static
    {
        UIManager.getDefaults().put(sUiClassID,
            ExtendedTreeUI.class.getName());
    }

    /**
     * The cached selection event.
     */
    private TreeSelectionEvent mMyCachedSelectionEvent;

    /**
     * The tree cell renderer.
     */
    protected ContactListTreeCellRenderer mRenderer;

    /**
     * Creates an instance of <tt>DefaultContactList</tt>.
     */
    public DefaultTreeContactList()
    {
        setBackground(Color.WHITE);
        setDragEnabled(true);
        String mergeEnabledStr = GuiActivator.getResources().getSettingsString(
                                                       CONTACTLIST_DND_ENABLED);

        if (Boolean.parseBoolean(mergeEnabledStr))
        {
            setTransferHandler(new ContactListTransferHandler(this));
        }

        getSelectionModel().
            setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        if (ConfigurationUtils.isAccessibilityMode())
        {
            mRenderer = new AccessibleContactListTreeCellRenderer();
        }
        else
        {
            mRenderer = new CompactContactListTreeCellRenderer();
        }

        setCellRenderer(mRenderer);

        registerForTooltips();

        // By default 2 successive clicks are need to begin dragging.
        // Workaround provided by simon@tardell.se on 29-DEC-2002 for bug 4521075
        // http://bugs.sun.com/bugdatabase/view_bug.do;jsessionid=a13e98ab2364524506eb91505565?bug_id=4521075
        // "Drag gesture in JAVA different from Windows". The bug is also noticed
        // on Mac Leopard.
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseReleased(MouseEvent e)
            {
                if (mMyCachedSelectionEvent == null)
                    return;

                DefaultTreeContactList.super
                    .fireValueChanged(mMyCachedSelectionEvent);

                mMyCachedSelectionEvent = null;
            }
        });
    }

    /**
     * Registers this component with the tooltip manager
     */
    protected void registerForTooltips()
    {
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    /**
     * Returns the currently selected object in the contact list. If there's
     * no selection, returns null.
     *
     * @return the currently selected object
     */
    public Object getSelectedValue()
    {
        TreePath selectionPath = getSelectionPath();

        if (selectionPath != null)
            return selectionPath.getLastPathComponent();

        return null;
    }

    /**
     * Checks if the given contact is currently active.
     * Dummy method used and overridden from classes extending this
     * functionality such as ContactList.
     *
     * @param contact the <tt>MetaContact</tt> to verify
     * @return TRUE if the given <tt>MetaContact</tt> is active, FALSE -
     * otherwise
     */
    public boolean isContactActive(UIContact contact)
    {
        return false;
    }

    /**
     * Checks if the given contact is currently active.
     * Dummy method used and overridden from classes extending this
     * functionality such as ContactList.
     *
     * @param metaContact the <tt>MetaContact</tt> to verify
     * @return TRUE if the given <tt>MetaContact</tt> is active, FALSE -
     * otherwise
     */
    public boolean isContactActive(MetaContact metaContact)
    {
        return false;
    }

    /**
     * Reloads renderer resources for this tree.
     */
    public void loadSkin()
    {
        mRenderer.loadSkin();
    }

    /**
     * Returns the name of the L&F class that renders this component.
     *
     * @return the string "TreeUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    public String getUIClassID()
    {
        return sUiClassID;
    }
}
