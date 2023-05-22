/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.text.*;
import java.util.*;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.tree.*;

import org.jitsi.util.*;

import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.plugin.desktoputil.plaf.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>GroupNode</tt> is a <tt>ContactListNode</tt> corresponding to a
 * given <tt>UIGroup</tt>.
 *
 * @author Yana Stamcheva
 */
public class GroupNode
    extends DefaultMutableTreeNode
    implements  ContactListNode
{
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>ContactLogger</tt> used by the <tt>GroupNode</tt> class and its
     * instances for logging output.
     */
    private static final ContactLogger contactLogger = ContactLogger.getLogger();

    /**
     * The parent contact list model.
     */
    private final ContactListTreeModel treeModel;

    /**
     * The corresponding <tt>UIGroup</tt>.
     */
    private final UIGroup group;

    /**
     * The <tt>ContactListNode</tt> <tt>Comparator</tt> used to sort the list of
     * children.
     * <p>
     * Since the <tt>NodeComparator</tt> class is static, it makes sense to not
     * have it instantiated per <tt>GroupNode</tt> instance but rather share one
     * and the same between all of them.
     * </p>
     */
    private static final NodeComparator nodeComparator = new NodeComparator();

    /**
     * Indicates if this group node is collapsed or expanded.
     */
    private boolean isCollapsed = false;

    /**
     * The task used to sort the child contacts within this GroupNode according
     * to their presence availability.
     */
    private CancellableRunnable sorter;
    private final Object sorterLock = new Object();

    /**
     * Creates a <tt>GroupNode</tt> by specifying the parent <tt>treeModel</tt>
     * and the corresponding <tt>uiGroup</tt>.
     *
     * @param treeModel the parent tree model containing this group
     * @param uiGroup the corresponding <tt>UIGroupImpl</tt>
     */
    public GroupNode(ContactListTreeModel treeModel, UIGroupImpl uiGroup)
    {
        super(uiGroup, true);

        this.treeModel = treeModel;
        this.group = uiGroup;

        isCollapsed = group.isGroupCollapsed();
    }

    /**
     * Creates a <tt>ContactNode</tt> for the given <tt>uiContact</tt>
     * and adds it to this group.
     * @param uiContact the <tt>UIContactImpl</tt> to add
     * @param index the index at which to insert the contact
     * @return the created <tt>ContactNode</tt>
     */
    public synchronized ContactNode addContact(UIContactImpl uiContact, int index)
    {
        if (contactLogger.isDebugEnabled())
            contactLogger.debug("Group node add contact: " + uiContact +
                                " at index " + index);

        int selectedIndex = getLeadSelectionRow();

        // There may already be a contact node, if the contact has been added
        // to the tree already, so check the UIContact and do not create a
        // duplicate.
        ContactNode contactNode = uiContact.getContactNode();

        if (contactNode == null)
        {
            contactNode = new ContactNode(uiContact);
            uiContact.setContactNode(contactNode);

            // If an index of -1 is specified, just add the contact to the end
            // of the tree, otherwise insert it at the specified index.
            if (index == -1)
            {
                add(contactNode);
                // Since contactNode is added to the back of the list, don't go looking
                // for it, just calculate which index is for the last node in the list.
                fireNodeInserted(children.size() - 1);
            }
            else
            {
                insert(contactNode, index);
                fireNodeInserted(index);
            }

            refreshSelection(selectedIndex, getLeadSelectionRow());
        }

        return contactNode;
    }

    /**
     * Creates a <tt>ContactNode</tt> for the given <tt>uiContact</tt>,
     * adds it to this group and performs a sort at the end.
     * @param uiContact the <tt>UIContactImpl</tt> to add
     * @return the created <tt>ContactNode</tt>
     */
    public synchronized ContactNode sortedAddContact(UIContactImpl uiContact)
    {
        // There may already be a contact node, if the contact has been added
        // to the tree already, so check the UIContact and do not create a
        // duplicate.
        ContactNode contactNode = uiContact.getContactNode();

        // If the underlying contact object is a MetaContact, grab it so we
        // can put it in log messages.
        MetaContact metaContact = null;
        if (uiContact instanceof MetaUIContact)
        {
            metaContact = ((MetaUIContact) uiContact).getMetaContact();
        }

        if (contactNode == null)
        {
            contactNode = new ContactNode(uiContact);
            uiContact.setContactNode(contactNode);

            if (children == null)
            {
                // Initially, children will be null.
                contactLogger.note(metaContact, "Added first UI contact to tree: "
                                                + uiContact);
                add(contactNode);
                fireNodeInserted(0);
            }
            else
            {
                // Instead of sorting after every addition, find the spot where we
                // should insert the node such that it is inserted in order.
                final int insertionPoint = Collections.binarySearch(getAllChildren(),
                        contactNode, nodeComparator);
                if (insertionPoint < 0)
                {
                    // index < 0 indicates that the node is not currently in the
                    // list and suggests an insertion point.
                    final int index = (insertionPoint + 1) * -1;
                    insert(contactNode, index);
                    contactLogger.note(metaContact, "Added UI contact to tree: "
                                                    + uiContact);
                    fireNodeInserted(index);
                }
                else
                {
                    contactLogger.warn(metaContact,
                              "Added UI contact " + uiContact + " not found!");
                }
            }
        }
        else
        {
            contactLogger.note(metaContact,
                                "UI Contact " + uiContact + " already exists!");
        }

        return contactNode;
    }

    /**
     * Removes the node corresponding to the given <tt>uiContact</tt> from this
     * group.
     * @param uiContact the <tt>UIContactImpl</tt> to remove
     */
    public synchronized void removeContact(UIContactImpl uiContact)
    {
        final ContactNode contactNode = uiContact.getContactNode();

        if (contactNode != null)
        {
            int index = getIndex(contactNode);
            if (index == -1)
            {
                contactLogger.debug(
                    "Asked to remove uiContact that is not a child of this node: " +
                                                        uiContact);
                return;
            }

            int selectedIndex = getLeadSelectionRow();

            // We remove the node directly from the list, thus skipping all
            // the checks verifying if the node belongs to this parent.
            children.removeElementAt(index);

            contactNode.setParent(null);
            uiContact.setContactNode(null);
            uiContact = null;

            fireNodeRemoved(contactNode, index);

            refreshSelection(selectedIndex, getLeadSelectionRow());
        }
    }

    /**
     * Creates a <tt>GroupNode</tt> for the given <tt>uiGroup</tt> and
     * adds it to this group.
     * @param uiGroup the <tt>UIGroupImpl</tt> to add
     * @return the created <tt>GroupNode</tt>
     */
    public synchronized GroupNode addContactGroup(UIGroupImpl uiGroup)
    {
        int selectedIndex = getLeadSelectionRow();

        GroupNode groupNode = new GroupNode(treeModel, uiGroup);
        uiGroup.setGroupNode(groupNode);

        add(groupNode);

        // Since groupNode is added to the back of the list, don't go looking
        // for it, just calculate which index is for the last node in the list.
        fireNodeInserted(children.size() - 1);

        refreshSelection(selectedIndex, getLeadSelectionRow());

        return groupNode;
    }

    /**
     * Removes the node corresponding to the given <tt>uiGroup</tt> from this
     * group node.
     * @param uiGroup the <tt>UIGroupImpl</tt> to remove
     */
    public synchronized void removeContactGroup(UIGroupImpl uiGroup)
    {
        GroupNode groupNode = uiGroup.getGroupNode();

        if (groupNode != null)
        {
            int index = getIndex(groupNode);

            if (index == -1)
            {
                // Not found.
                contactLogger.debug(
                    "Asked to remove uiGroup that is not a child of this node: " +
                                                        uiGroup);
                return;
            }

            int selectedIndex = getLeadSelectionRow();

            // We remove the node directly from the list, thus skipping all the
            // checks verifying if the node belongs to this parent.
            children.removeElementAt(index);

            groupNode.setParent(null);
            uiGroup.setGroupNode(null);

            fireNodeRemoved(groupNode, index);

            refreshSelection(selectedIndex, getLeadSelectionRow());
        }
    }

    /**
     * Creates a <tt>GroupNode</tt> for the given <tt>uiGroup</tt>,
     * adds it to this group node and performs a sort at the end.
     * @param uiGroup the <tt>UIGroupImpl</tt> to add
     * @return the created <tt>GroupNode</tt>
     */
    public synchronized GroupNode sortedAddContactGroup(UIGroupImpl uiGroup)
    {
        GroupNode groupNode = new GroupNode(treeModel, uiGroup);

        uiGroup.setGroupNode(groupNode);

        add(groupNode);

        if (children == null)
        {
            // Initially, children will be null.
            add(groupNode);
            fireNodeInserted(0);
        }
        else
        {
            // Instead of sorting after every addition, find the spot where we
            // should insert the node such that it is inserted in order.
            int insertionPoint = Collections.binarySearch(getAllChildren(),
                    groupNode, nodeComparator);
            if (insertionPoint < 0)
            {
                // index < 0 indicates that the node is not currently in the
                // list and suggests an insertion point.
                insertionPoint = (insertionPoint + 1) * -1;
            }
            else
            {
                // A node with this index was already found. As the index
                // is not guaranteed to be unique, add this group after the
                // one just found.
                ++insertionPoint;
            }

            // You can't insert AFTER the last entry, so use add() for that case.
            // Note insertionPoint is zero-based.
            if (insertionPoint < getChildCount())
            {
                insert(groupNode, insertionPoint);
                fireNodeInserted(insertionPoint);
            }
            else
            {
                add(groupNode);

                // Since groupNode is added to the back of the list, don't go looking
                // for it, just calculate which index is for the last node in the list.
                fireNodeInserted(children.size() - 1);
            }
        }

        return groupNode;
    }

    /**
     * Returns a collection of all direct children of this <tt>GroupNode</tt>.
     *
     * @return a collection of all direct children of this <tt>GroupNode</tt>
     */
    public Collection<ContactNode> getContacts()
    {
        if (children != null)
            return Collections.unmodifiableCollection(getDirectChildren());

        return null;
    }

    /**
     * Returns the <tt>UIGroup</tt> corresponding to this <tt>GroupNode</tt>.
     * @return the <tt>UIGroup</tt> corresponding to this <tt>GroupNode</tt>
     */
    public UIGroupImpl getGroupDescriptor()
    {
        return (UIGroupImpl) getUserObject();
    }

    /**
     * Returns the index of this node in its parent group.
     * @return the index of this node in its parent group
     */
    public int getSourceIndex()
    {
        return group.getSourceIndex();
    }

    /**
     * Sorts the children of this node.
     * Note we only synchronize the actual processing on <tt>this</tt>, not the logic to schedule
     * that processing on the EDT. This prevents deadlocks when a non-UI thread owns a lock that
     * the EDT needs, whilst the EDT has the <tt>this</tt> lock.
     *
     * @param treeModel the <tt>ContactListTreeModel</tt>, which should be refreshed
     */
    public void sort(final ContactListTreeModel treeModel)
    {
        synchronized(sorterLock)
        {
            if (sorter != null)
            {
                // There's an existing sort task that has been added to the EDT queue at some point.
                // Cancel it in case it's yet to run - there's no need to sort twice.
                sorter.cancel();
            }

            sorter = new CancellableRunnable()
            {
                public void performTask()
                {
                    synchronized(GroupNode.this)
                    {
                        TreePath selectionPath = getLeadSelectionPath();
                        int oldSelectionIndex = getLeadSelectionRow();

                        Collections.sort(getAllChildren(), nodeComparator);

                        fireNodesChanged();

                        treeModel.getParentTree().setSelectionPath(selectionPath);

                        refreshSelection(oldSelectionIndex, getLeadSelectionRow());
                    }
                }
            };

            if (children != null)
            {
                // Sort the children of this node on the EDT.
                SwingUtilities.invokeLater(sorter);
            }
        }
    }

    /**
     * Returns <tt>true</tt> if the group is collapsed or <tt>false</tt>
     * otherwise.
     * @return <tt>true</tt> if the group is collapsed or <tt>false</tt>
     * otherwise.
     */
    public boolean isCollapsed()
    {
        return isCollapsed;
    }

    /**
     * Clears all dependencies for all children in the given <tt>groupNode</tt>
     * (i.e. GroupNode - UIGroup - MetaContactGroup or ContactNode - UIContact
     * - SourceContact).
     */
    public synchronized void clear()
    {
        int childCount = getChildCount();

        for (int i = 0; i < childCount; i ++)
        {
            TreeNode treeNode = getChildAt(i);

            if (treeNode instanceof ContactNode)
            {
                ((ContactNode) treeNode).getContactDescriptor()
                    .setContactNode(null);
            }
            else if (treeNode instanceof GroupNode)
            {
                ((GroupNode) treeNode).getGroupDescriptor()
                    .setGroupNode(null);

                ((GroupNode) treeNode).clear();
            }
        }
        if (children != null)
            children.removeAllElements();
    }

    /**
     * Notifies all interested listeners that a node has been inserted at the
     * given <tt>index</tt>.
     * @param index the index of the newly inserted node
     */
    private void fireNodeInserted(int index)
    {
        treeModel.nodesWereInserted(this, new int[]{index});
    }

    /**
     * Notifies all interested listeners that <tt>node</tt> has been removed
     * from the given <tt>index</tt>.
     * @param node the node that has been removed
     * @param index the index of the removed node
     */
    private void fireNodeRemoved(ContactListNode node, int index)
    {
        treeModel.nodesWereRemoved(this, new int[]{index}, new Object[]{node});
    }

    /**
     * Notifies all interested listeners that all nodes have changed.
     */
    private void fireNodesChanged()
    {
        int childCount = getChildCount();
        int[] changedIndexes = new int[childCount];

        for (int i = 0; i < childCount; i++)
            changedIndexes[i] = i;

        treeModel.nodesChanged(this, changedIndexes);
    }

    /**
     * Note: this comparator imposes orderings that are inconsistent with
     * equals.
     */
    private static class NodeComparator
        implements Comparator<ContactListNode>
    {
        /**
         * Collator to use when comparing two strings.  This is better than a
         * naive string comparison as the collator will take into account
         * accented characters
         */
        private final Collator collator;

        public NodeComparator()
        {
            // Create the collator we use to compare the node names.  Strength
            // primary so that we consider accents appropriately
            collator = Collator.getInstance();
            collator.setStrength(Collator.PRIMARY);
        }

        /**
         * Compares its two arguments for order.  Returns a negative integer,
         * zero, or a positive integer as the first argument is less than, equal
         * to, or greater than the second.
         * @param node1 the first <tt>ContactListNode</tt> to compare
         * @param node2 the second <tt>ContactListNode</tt> to compare
         * @return -1 if the first node should be positioned before the second
         * one, 1 if the first argument should be positioned after the second
         * one, 0 if there's no matter
         */
        public int compare(ContactListNode node1, ContactListNode node2)
        {
            // Child groups are shown after child contacts.
            if (node1 instanceof GroupNode)
            {
                if (node2 instanceof ContactNode)
                    return 1;
            }
            else if (node1 instanceof ContactNode)
            {
                if (node2 instanceof GroupNode)
                {
                    return -1;
                }
                else if (node2 instanceof ContactNode)
                {
                    // If both nodes are ContactNodes then we can get to the
                    // underlying MetaUIContact. Only if we have have two
                    // MetaUIContacts do we compare their display names
                    // otherwise we flow through to just compare source indices.
                    UIContactImpl node1Descriptor =
                        ((ContactNode) node1).getContactDescriptor();
                    UIContactImpl node2Descriptor =
                        ((ContactNode) node2).getContactDescriptor();

                    if (node1Descriptor instanceof MetaUIContact
                        && node2Descriptor instanceof MetaUIContact)
                    {
                        String node1Name =
                            ((UIContact) node1Descriptor).getDisplayName();
                        String node2Name =
                            ((UIContact) node2Descriptor).getDisplayName();

                        if (StringUtils.isNullOrEmpty(node1Name))
                        {
                            return 1;
                        }
                        else if (StringUtils.isNullOrEmpty(node2Name))
                        {
                            return -1;
                        }

                        if (Character.isDigit(node1Name.charAt(0)) &&
                            !Character.isDigit(node2Name.charAt(0)))
                        {
                            // 1st name starts with a number, 2nd does not.
                            // Position 1st after 2nd
                            return 1;
                        }
                        else if (Character.isDigit(node2Name.charAt(0)) &&
                                 !Character.isDigit(node1Name.charAt(0)))
                        {
                            // 2nd name starts with a number, 1st does not.
                            // Position 1st before 2nd
                            return -1;
                        }

                        // Use the collator to compare the names:
                        int rc = collator.compare(node1Name, node2Name);
                        if (rc != 0)
                        {
                            return rc;
                        }

                        // Both nodes have the same display name.  They are
                        // therefore either the same MetaContact, or two
                        // distinct MetaContacts which need to be ordered
                        // consistently, so we used the Meta UID for that
                        // because it is guaranteed to be unique.
                        // We already know that node1/2Descriptor is a
                        // MetaUIContact from the test above.
                        MetaContact metaContact1 = (MetaContact)
                            ((MetaUIContact) node1Descriptor).getDescriptor();
                        MetaContact metaContact2 = (MetaContact)
                            ((MetaUIContact) node2Descriptor).getDescriptor();
                        String metaUid1 = metaContact1.getMetaUID();
                        String metaUid2 = metaContact2.getMetaUID();

                        return collator.compare(metaUid1, metaUid2);
                    }
                }
            }

            int index1 = node1.getSourceIndex();
            int index2 = node2.getSourceIndex();

            // If the first index is unknown then we position it at the end.
            if (index1 < 0)
            {
                return 1;
            }
            // If the second index is unknown then we position it at the end.
            if (index2 < 0)
            {
                return -1;
            }

            return Integer.compare(index1, index2);
        }
    }

    /**
     * Returns the current lead selection row.
     *
     * @return the current lead selection row
     */
    private int getLeadSelectionRow()
    {
        JTree tree = treeModel.getParentTree();
        int[] rows = tree.getSelectionRows();
        int selectedRow = -1;

        if ((rows != null) && (rows.length != 0))
            selectedRow = rows[0];

        return selectedRow;
    }

    /**
     * Returns the current lead selection path.
     *
     * @return the current lead selection path
     */
    private TreePath getLeadSelectionPath()
    {
        return treeModel.getParentTree().getSelectionPath();
    }

    /**
     * Refreshes the selection paths.
     *
     * @param lastSelectedIndex the last selected index
     * @param newSelectedIndex the newly selected index
     */
    private void refreshSelection(int lastSelectedIndex, int newSelectedIndex)
    {
        JTree tree = treeModel.getParentTree();
        TreeUI treeUI = tree.getUI();

        if (treeUI instanceof SIPCommTreeUI)
        {
            SIPCommTreeUI sipCommTreeUI = (SIPCommTreeUI) treeUI;
            TreePath oldSelectionPath = tree.getPathForRow(lastSelectedIndex);
            TreePath newSelectionPath = tree.getPathForRow(newSelectedIndex);

            sipCommTreeUI.selectionChanged(oldSelectionPath, newSelectionPath);
        }
    }

    /**
     * children may be a mix of ContactNode and GroupNode objects, return only the
     * ContactNode objects - these are the direct children.
     * @return children as a Vector of ContactNode objects
     */
    private Vector<ContactNode> getDirectChildren()
    {
        Vector<ContactNode> contactNodeChildren = new Vector<>();
        children.forEach( (child) ->
        {
            if (child.getClass().isInstance(ContactNode.class))
            {
                contactNodeChildren.add((ContactNode)child);
            }
            else
            {
                // GroupNodes only contain indirect children, so just log and ignore.
                contactLogger.debug(this + "Contains indirect children in: " + child);
            }
        });
        return contactNodeChildren;
    }

    /**
     * children may be a mix of ContactNode and GroupNode objects, return all of them.
     * @return children as a Vector of ContactListNode objects
     */
    private Vector<ContactListNode> getAllChildren()
    {
        Vector<ContactListNode> contactListNodeChildren = new Vector<>();
        children.forEach( (child) ->
        {
            contactListNodeChildren.add((ContactListNode)child);
        });
        return contactListNodeChildren;
    }

    /**
     * Implementation of Runnable that can be subsequently cancelled if it has
     * not yet been started.
     */
    abstract class CancellableRunnable implements Runnable
    {
        private boolean cancelled = false;

        public void run()
        {
            synchronized(this)
            {
                if (cancelled)
                {
                    // The task has been cancelled, so abort without running
                    // it.
                    return;
                }
            }

            // The task has not been cancelled, so go ahead and execute it.
            performTask();
        }

        /**
         * Perform the task associated with this <tt>CancellableRunnable</tt>.
         */
        public abstract void performTask();

        /**
         * Cancel this runnable, so that if it hasn't yet been executed yet, it
         * will never be.
         */
        public synchronized void cancel()
        {
            cancelled = true;
        }
    }
}
