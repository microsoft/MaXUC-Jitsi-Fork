/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil.plaf;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import javax.swing.tree.*;

import net.java.sip.communicator.util.Logger;

/**
 * SIPCommTreeUI implementation.
 *
 * @author Yana Stamcheva
 */
public class SIPCommTreeUI
    extends BasicTreeUI
    implements HierarchyListener,
               TreeSelectionListener
{
    /**
     * Our class logger.
     */
    private static final Logger sLog = Logger.getLogger(SIPCommTreeUI.class);

    private JViewport mParentViewport;

    private VariableLayoutCache mLayoutCache;

    /**
     * Last selected index.
     */
    private int mLastSelectedIndex;

    /**
     * Creates the UI for the given component.
     * @param component the component for which we're create an UI
     * @return this UI implementation
     */
    public static ComponentUI createUI(JComponent component)
    {
        return new SIPCommTreeUI();
    }

    /**
     * Installs this UI to the given component.
     * @param component the component to which to install this UI
     */
    public void installUI(JComponent component)
    {
        if (component == null)
            throw new NullPointerException(
                "null component passed to BasicTreeUI.installUI()" );

        // set 'our' tree, then get a viewport or set this as listener
        // super method then sets super tree object and  other listeners, etc.
        tree = (JTree)component;

        JViewport viewport = getFirstParentViewport(tree);
        if(viewport != null)
        {
            mParentViewport = viewport;
        }
        else
        {
            tree.addHierarchyListener(this);
        }

        tree.getSelectionModel().addTreeSelectionListener(this);

        super.installUI(component);
    }

    /**
     * Returns the first parent view port found.
     * @param container the component parents we search
     * @return the first parent view port found.
     */
    private JViewport getFirstParentViewport(Container container)
    {
        if(container == null)
            return null;
        else
            if(container instanceof JViewport)
                return (JViewport)container;
            else
                return getFirstParentViewport(container.getParent());
    }

    /**
     * On uninstalling the ui remove the listeners.
     * @param component
     */
    public void uninstallUI(JComponent component)
    {
        sLog.trace("uninstall UI and null tree");
        tree.getSelectionModel().clearSelection();
        tree.getSelectionModel().removeTreeSelectionListener(this);
        tree.removeHierarchyListener(this);

        super.uninstallUI(component);
    }

    /**
     * HierarchyListener's method.
     * @param event the event.
     */
    public void hierarchyChanged(HierarchyEvent event)
    {
        if (event.getID() == HierarchyEvent.HIERARCHY_CHANGED
            && (event.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0
            && event.getChangedParent() instanceof JViewport)
        {
            mParentViewport = (JViewport) event.getChangedParent();
        }
    }

    /**
     * The TreeSelectionListener's method.
     * @param event the event.
     */
    public void valueChanged(TreeSelectionEvent event)
    {
        // Update cell size.
        selectionChanged(   event.getOldLeadSelectionPath(),
                            event.getNewLeadSelectionPath());
    }

    /**
     * Installs the defaults of this UI.
     */
    protected void installDefaults()
    {
        if(tree.getBackground() == null ||
           tree.getBackground() instanceof UIResource) {
            tree.setBackground(UIManager.getColor("Tree.background"));
        }
        if(getHashColor() == null || getHashColor() instanceof UIResource) {
            setHashColor(UIManager.getColor("Tree.hash"));
        }
        if (tree.getFont() == null || tree.getFont() instanceof UIResource)
            tree.setFont( UIManager.getFont("Tree.font") );
            // JTree's original row height is 16.  To correctly display the
            // contents on Linux we should have set it to 18, Windows 19 and
            // Solaris 20.  As these values vary so much it's too hard to
            // be backward compatable and try to update the row height, we're
            // therefor NOT going to adjust the row height based on font. If the
            // developer changes the font, it's there responsibility to update
            // the row height.

        setExpandedIcon(null);
        setCollapsedIcon(null);

        setLeftChildIndent(0);
        setRightChildIndent(0);

        LookAndFeel.installProperty(tree, "rowHeight",
                        UIManager.get("Tree.rowHeight"));

        largeModel = (tree.isLargeModel() && tree.getRowHeight() > 0);

        Object scrollsOnExpand = UIManager.get("Tree.scrollsOnExpand");
        if (scrollsOnExpand != null) {
            LookAndFeel.installProperty(
                tree, "scrollsOnExpand", scrollsOnExpand);
        }

        UIManager.getDefaults().put("Tree.paintLines", false);
        UIManager.getDefaults().put("Tree.lineTypeDashed", false);
    }

    /**
     * Creates the object responsible for managing what is expanded, as
     * well as the size of nodes.
     * @return the created layout cache
     */
    protected AbstractLayoutCache createLayoutCache()
    {
        mLayoutCache = new VariableLayoutCache();
        return mLayoutCache;
    }

    /**
     * Do not select the <tt>ShowMoreContact</tt>.
     *
     * @param path the <tt>TreePath</tt> to select
     * @param event the <tt>MouseEvent</tt> that provoked the select
     */
    protected void selectPathForEvent(TreePath path, MouseEvent event)
    {
        super.selectPathForEvent(path, event);
    }

    /**
     * A custom layout cache that recalculates the width of the cell the match
     * the width of the tree (i.e. expands the cell to the right).
     */
    private class VariableLayoutCache extends VariableHeightLayoutCache
    {
        /**
         * Returns the preferred width of the receiver.
         * @param path the path, which bounds we obtain
         * @param placeIn the initial rectangle of the path
         * @return the bounds of the path
         */
        public Rectangle getBounds(TreePath path, Rectangle placeIn)
        {
            Rectangle rect =  super.getBounds(path, placeIn);

            if (rect != null && mParentViewport != null)
            {
                rect.width = mParentViewport.getWidth() - 2;
            }

            return rect;
        }
    }

    /**
     * Ensures the tree size.
     */
    private void ensureTreeSize()
    {
        // If we have already been uninstalled then this is null, but there
        // is also nothing to resize or repaint.
        if (tree != null)
        {
            // Update tree height.
            updateSize();

            // Finally repaint in order the change to take place.
            tree.repaint();
        }
        else
        {
            sLog.warn("tree is unexpectedly null");
        }
    }

    /**
     * Refreshes row sizes corresponding to the given paths.
     *
     * @param oldPath the old selection path
     * @param newPath the new selection path
     */
    public void selectionChanged(TreePath oldPath, TreePath newPath)
    {
        if (oldPath != null)
        {
            mLayoutCache.invalidatePathBounds(oldPath);
        }

        if (newPath != null)
        {
            mLayoutCache.invalidatePathBounds(newPath);
            mLastSelectedIndex = tree != null ? tree.getRowForPath(newPath) : 0;
        }
        // If the selection has disappeared, for example when the selected row
        // has been removed, refresh the previously selected row.
        else
        {
            if (tree != null)
            {
                int nextRow = (tree.getRowCount() > mLastSelectedIndex)
                    ? mLastSelectedIndex : tree.getRowCount() - 1;

                mLayoutCache.invalidatePathBounds(
                    tree.getPathForRow(nextRow));
            }
        }

        ensureTreeSize();
    }

    /**
     * Override this method to allow multiple cell selection without having
     * to hold down the ctrl key
     *
     * @param event the Mouse event
     * @return whether this is a toggle selection event.
     */
    protected boolean isToggleSelectionEvent(MouseEvent event)
    {
        return (event.getButton() == MouseEvent.BUTTON1);
    }
}
