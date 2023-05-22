// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.callpark;

import static org.jitsi.util.Hasher.logHasher;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.tree.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetCallPark.CallParkListener;
import net.java.sip.communicator.service.protocol.OperationSetCallPark.CallParkOrbit;
import net.java.sip.communicator.service.protocol.OperationSetCallPark.CallParkOrbitState;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;

/**
 * Class which displays the Call Park window.
 */
public class CallParkWindow extends SIPCommFrame implements CallParkListener,
                                                            ExportedWindow,
                                                            ContactListContainer
{
    private static final long serialVersionUID = 1L;
    private static final Logger sLog = Logger.getLogger(CallParkWindow.class);

    /**
     * The instance of the Call Park
     */
    private static CallParkWindow sWindow;

    /**
     * The colour at the top of the background gradient
     */
    private static final Color BACKGROUND_GRADIENT_TOP =
                    new Color(GuiActivator.getResources().getColor(
                                        "service.gui.LIGHT_BACKGROUND"));

    /**
     * The colour at the bottom of the background gradient
     */
    private static final Color BACKGROUND_GRADIENT_BOTTOM =
                    new Color(GuiActivator.getResources().getColor(
                                     "service.gui.LIGHT_BACKGROUND"));

    /**
     * The colour of the border around the header
     */
    private static final Color BORDER_HEADER =
                    new Color(GuiActivator.getResources().getColor(
                                         "service.gui.BORDER_SHADOW"));

    /**
     * The contact list component that contains all the call park orbits
     */
    private final TreeContactList mContactList = new TreeContactList(this)
    {
        private static final long serialVersionUID = 1L;

        protected void registerForTooltips() {}

        protected boolean isFlattenGroupsEnabled()
        {
            return false;
        }
    };

    /**
     * The operation set for call parks that we are displaying
     */
    private final OperationSetCallPark mParkOpSet;

    /**
     * The operation set for contacts that corresponds to the provider we display
     */
    private final OperationSetPersistentPresence mContactsOpset;

    /**
     * A map from the call park orbit to the contact that it is represented by
     */
    private final HashMap<CallParkOrbit, CallParkUIContact> mOrbitContactMap =
            new HashMap<>();

    /**
     * A map from ContactGroups to the UIGroup that represents it
     */
    private final HashMap<ContactGroup, CallParkUIGroup> mGroupMap =
            new HashMap<>();

    /**
     * Resources used to get strings and images
     */
    private final ResourceManagementService mResources;

    /**
     * True if this window is focused
     */
    private boolean mFocused;

    /**
     * The cell editor for the contact list
     */
    private CallParkCellEditor mCellEditor = new CallParkCellEditor();

    /**
     * The currently selected row
     */
    private int mSelectedRow;

    private CallParkWindow()
    {
        List<ProtocolProviderService> provs =
                          GuiActivator.getProviders(OperationSetCallPark.class);
        ProtocolProviderService prov = provs.get(0);
        mParkOpSet     = prov.getOperationSet(OperationSetCallPark.class);
        mContactsOpset = prov.getOperationSet(OperationSetPersistentPresence.class);
        mResources = GuiActivator.getResources();

        // Listen for changes to the call park orbits
        mParkOpSet.registerListener(this);

        // Listen for if this window is focused
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowActivated(WindowEvent e)
            {
                mFocused = true;

                SwingUtilities.invokeLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (mContactList.getSelectionPath() == null)
                        {
                            mContactList.setSelectionRow(mSelectedRow);
                        }

                        if (mContactList.getEditingPath() == null)
                        {
                            TreeUI ui = mContactList.getUI();
                            TreePath path = ui.getPathForRow(mContactList,
                                                             mSelectedRow);

                            if (path == null)
                                path = ui.getPathForRow(mContactList, 0);

                            mContactList.startEditingAtPath(path);
                        }
                    }
                });
            }

            @Override
            public void windowDeactivated(WindowEvent e)
            {
                mFocused = false;
                mContactList.clearSelection();
            }
        });

        // Set up the window
        setTitle(mResources.getI18NString("service.gui.CALL_PARK_TITLE"));
        setMinimumSize(new ScaledDimension(375, 475));
        setPreferredSize(new ScaledDimension(375, 475));

        // Add the constituent components of the window:
        JPanel mainPanel = new JPanel(new BorderLayout(5, 0));
        mainPanel.setBorder(BorderFactory.createEmptyBorder());

        JPanel headerBar = createHeaderBar();
        JPanel contactList = createContactList();

        mainPanel.add(headerBar, BorderLayout.PAGE_START);
        mainPanel.add(contactList, BorderLayout.CENTER);

        getContentPane().add(mainPanel);

        addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
                {
                    sLog.user("Escape pressed in call window");
                    setVisible(false);
                }
                else if (mContactList.getEditingPath() != null)
                {
                    // User pressed a key, while the window is focused.
                    // Let the cell editor handle it
                    mCellEditor.handleKeyPress(e);
                }
            }
        });
    }

    /**
     * @return the header bar at the top of this window
     */
    private JPanel createHeaderBar()
    {
        // Create the header bar - it has a gradient which is achieved by over-
        // riding the paintComponent method.
        JPanel headerBar = new JPanel()
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g)
            {
                super.paintComponent(g);

                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                                     RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gradient =
                                  new GradientPaint(0,
                                                    0,
                                                    BACKGROUND_GRADIENT_TOP,
                                                    0,
                                                    getHeight(),
                                                    BACKGROUND_GRADIENT_BOTTOM);

                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                g.finalize();
            }
        };

        headerBar.setLayout(new BorderLayout());
        headerBar.setBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_HEADER));

        // Text
        JLabel title = new JLabel();
        title.setText(mResources.getI18NString("service.gui.CALL_PARK_SUBTITLE"));
        title.setBorder(ScaleUtils.createEmptyBorder(10, 10, 10, 10));
        title.setFont(title.getFont().deriveFont(
            Font.PLAIN, ScaleUtils.getScaledFontSize(14f)));
        title.setFocusable(true);
        headerBar.add(title, BorderLayout.LINE_START);

        // Icon at right hand side
        JLabel icon = new JLabel();
        mResources.getImage("service.gui.PARK_ICON").addToLabel(icon);
        icon.setBorder(ScaleUtils.createEmptyBorder(5, 5, 5, 5));
        headerBar.add(icon, BorderLayout.LINE_END);

        return headerBar;
    }

    /**
     * @return the component that contains the contact list
     */
    private TransparentPanel createContactList()
    {
        // Set the contact list to be as we want
        mContactList.setEditable(true);
        mContactList.setRightButtonMenuEnabled(false);
        mContactList.setCellRenderer(new CallParkCellRenderer(this));
        mContactList.setCellEditor(mCellEditor);
        mContactList.setFocusable(true);

        // Add a mouse listener so that we can select a cell on mouse over
        mContactList.addMouseMotionListener(new MouseMotionListener()
        {
            private int mLastSelected;

            @Override
            public void mouseMoved(MouseEvent e)
            {
                handleMouseEvent(e);
            }

            @Override
            public void mouseDragged(MouseEvent e)
            {
                handleMouseEvent(e);
            }

            /**
             * Method for handling mouse events
             *
             * @param e the mouse event to handle
             */
            private void handleMouseEvent(MouseEvent e)
            {
                // Nothing to do if not focused, or if the menu is showing
                if (!mFocused || mCellEditor.isMenuShowing())
                    return;

                JTree tree = (JTree)e.getSource();
                int selectedRow = tree.getRowForLocation(e.getX(), e.getY());

                if (selectedRow == -1)
                {
                    // Not selected any row
                    tree.clearSelection();
                    mLastSelected = -1;
                }
                else if (selectedRow != mLastSelected)
                {
                    // Select the currently selected row
                    tree.setSelectionRow(selectedRow);
                    mLastSelected = selectedRow;
                }
            }
        });

        // Finally, set the data of the contact list
        initContactListData();

        // Make sure that the first element in the list is selected
        mContactList.setSelectionRow(mSelectedRow);

        // And add the contact list to a scroll pane
        JScrollPane contactListScrollPane = new JScrollPane();
        contactListScrollPane.setBorder(BorderFactory.createEmptyBorder());
        contactListScrollPane.getViewport().add(mContactList.getComponent());

        TransparentPanel listPanel = new TransparentPanel(new BorderLayout());
        listPanel.setBorder(BorderFactory.createEmptyBorder());
        listPanel.add(contactListScrollPane);

        return listPanel;
    }

    /**
     * Initializes the contact list with the list of call park orbits to display
     */
    private synchronized void initContactListData()
    {
        // First, remove all existing contacts and contact sources
        mContactList.removeAllContactSources();
        mContactList.removeAll();
        mOrbitContactMap.clear();
        mGroupMap.clear();

        ContactGroup group = getContactGroup();

        if (group != null)
        {
            addGroup(group, null);

            // Finally, work out how many of the orbits are busy
            countOrbits();
        }
        else
        {
            sLog.error("Null group when trying to populate Call Park Window");
        }
    }

    /**
     * Adds a contact group (including all contacts and sub-groups) to the
     * contact list
     *
     * @param contactGroup The contact group to add
     * @param parentUIGroup The UI group that should contain the contact group
     * @return true if we added a contact in this group
     */
    private boolean addGroup(ContactGroup contactGroup,
                             CallParkUIGroup parentUIGroup)
    {
        boolean addedSomething = false;
        CallParkUIGroup uiGroup = new CallParkUIGroup(contactGroup, parentUIGroup);
        mGroupMap.put(contactGroup, uiGroup);

        // Only add the group to the list if it has a parent.  This is required
        // so that we don't have a root group entry in the list.
        if (parentUIGroup != null)
            mContactList.addGroup(uiGroup, false);

        // Add all contacts
        Iterator<Contact> childContacts = contactGroup.contacts();
        while (childContacts.hasNext())
        {
            Contact child = childContacts.next();
            CallParkOrbit orbit = mParkOpSet.getOrbitForContact(child);

            if (orbit != null)
            {
                CallParkUIContact contact = new CallParkUIContact(orbit, uiGroup);
                mOrbitContactMap.put(orbit, contact);

                addedSomething = true;

                if (parentUIGroup == null)
                    mContactList.addContact(contact, null, true, false);
                else
                    mContactList.addContact(contact, uiGroup, true, false);
            }
        }

        // Add all child groups
        Iterator<ContactGroup> childGroups = contactGroup.subgroups();
        while (childGroups.hasNext())
        {
            addedSomething |= addGroup(childGroups.next(), uiGroup);
        }

        // If there aren't any contacts, then we don't actually want to show this
        // group - it's empty
        if (!addedSomething)
        {
            mContactList.removeGroup(uiGroup);
        }

        return addedSomething;
    }

    /**
     * Counts all the orbits in the entire group and work out how many are busy
     * and how many there are.
     */
    private void countOrbits()
    {
        CallParkUIGroup uiGroup = mGroupMap.get(getContactGroup());

        // Count the orbits, first all, then the busy ones
        countOrbits(uiGroup, false);
        countOrbits(uiGroup, true);
    }

    /**
     * Counts how many orbits are in a group and all its subgroups.
     *
     * @param uiGroup The group to count
     * @param checkBusy If true, then the count will only include busy orbits
     * @return The number of orbits
     */
    private int countOrbits(CallParkUIGroup uiGroup, boolean checkBusy)
    {
        int nOrbits = 0;
        ContactGroup contactGroup = uiGroup.getDescriptor();

        Iterator<Contact> contacts = contactGroup.contacts();
        while (contacts.hasNext())
        {
            Contact contact = contacts.next();
            CallParkOrbit orbit = mParkOpSet.getOrbitForContact(contact);

            if (orbit != null && (!checkBusy || orbit.getState().isBusy()))
                nOrbits++;
        }

        Iterator<ContactGroup> subGroups = contactGroup.subgroups();
        while (subGroups.hasNext())
        {
            nOrbits += countOrbits(mGroupMap.get(subGroups.next()), checkBusy);
        }

        if (checkBusy)
            uiGroup.setBusyOrbits(nOrbits);
        else
            uiGroup.setAllOrbits(nOrbits);

        return nOrbits;
    }

    /**
     * @return the group of call park orbits (which are contacts)
     */
    private ContactGroup getContactGroup()
    {
        // Note that we don't want the root group, but the subgroup of that
        // which contains the Call Park info
        ContactGroup root = mContactsOpset.getServerStoredContactListRoot();
        ContactGroup group = root.getGroup(OperationSetCallPark.CALL_PARK_GROUP);

        return group;
    }

    @Override
    public void onOrbitListChanged()
    {
        sLog.info("Orbit list changed");
        initContactListData();
    }

    @Override
    public void onOrbitStateChanged(CallParkOrbit orbit, CallParkOrbitState oldState)
    {
        CallParkOrbitState newState = orbit.getState();
        CallParkUIContact uiContact = mOrbitContactMap.get(orbit);
        sLog.debug("Orbit changed " + orbit.getOrbitCode() + ", " + newState);

        countOrbits();

        // Redraw the node for this contact and all its parents
        TreeNode node = uiContact.getContactNode();
        while (node != null)
        {
            mContactList.nodeChanged(node);
            node = node.getParent();
        }
    }

    @Override
    public void onCallParkAvailabilityChanged()
    {
        if (!mParkOpSet.isCallParkAvailable() || !mParkOpSet.isEnabled())
        {
            sLog.debug("Availability changed, hiding window");
            removeWindow();
        }
    }

    @Override
    public void onCallParkEnabledChanged()
    {
        if (!mParkOpSet.isCallParkAvailable() || !mParkOpSet.isEnabled())
        {
            sLog.debug("Enabledness changed, hiding window");
            removeWindow();
        }
    }

    /**
     * Remove the cached window, including unregistering from all listeners
     */
    private void removeWindow()
    {
        mParkOpSet.unregisterListener(this);
        setVisible(false);
        sWindow = null;
        GuiActivator.getUIService().unregisterExportedWindow(this);
    }

    /**
     * Update the cached currently selected row
     *
     * @param row The new value of the selected row
     */
    public void setSelectedRow(int row)
    {
        mSelectedRow = row;
    }

    @Override
    public WindowID getIdentifier()
    {
        return ExportedWindow.CALL_PARK_WINDOW;
    }

    @Override
    public void bringToFront()
    {
        requestFocus();
    }

    @Override
    public void bringToBack()
    {
        toBack();
    }

    @Override
    public void minimize()
    {
        setState(Frame.ICONIFIED);
    }

    @Override
    public void maximize()
    {
        setState(Frame.NORMAL);
    }

    @Override
    public Object getSource()
    {
        return null;
    }

    @Override
    public void setParams(Object[] windowParams) {}

    /**
     * Display a call park window.  This will either create the window if one is
     * not yet showing or redisplay the existing one
     */
    public static synchronized void showCallParkWindow()
    {
        showCallParkWindow(null);
    }

    /**
     * Show the call park window, creating it if required
     *
     * @param window The owner window
     */
    private static void showCallParkWindow(Window window)
    {
        // This function must be run on EDT.
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(() -> showCallParkWindow(window));
            return;
        }

        sLog.info("Show call park window");

        if (sWindow == null)
        {
            sWindow = new CallParkWindow();
            GuiActivator.getUIService().registerExportedWindow(sWindow);
        }

        if (window != null && window.isAlwaysOnTop())
        {
            sWindow.setAlwaysOnTop(true);
            sWindow.toFront();
        }

        sWindow.setState(Frame.NORMAL);

        if (!sWindow.isVisible())
        {
            sWindow.setVisible(true);
        }
        else
        {
            // Already visible, just request the focus.
            sWindow.requestFocus();
        }

        // Make sure that an element is selected.
        if (sWindow.mContactList.getSelectionPath() == null)
        {
            sWindow.mContactList.setSelectionRow(sWindow.mSelectedRow);
        }

        sWindow.setAlwaysOnTop(true);
        sWindow.toFront();
        sWindow.setAlwaysOnTop(false);
    }

    /**
     * Creates or shows the call park window.  If the Call Park window was not
     * yet showing, then it will be hidden once the user has parked the call
     * associated with the call peer.
     *
     * @param peer The peer the user is trying to park
     * @param window The window that should own the call park dialog
     */
    public static synchronized void parkCall(CallPeer peer, Window window)
    {
        sLog.info("Open call park window for call " + logHasher(peer.getDisplayName()));
        final boolean windowWasHidden = sWindow == null || !sWindow.isVisible();

        // Add a listener for the  call to be dismissed.
        peer.getCall().addCallChangeListener(new CallChangeAdapter()
        {
            @Override
            public void callStateChanged(CallChangeEvent evt)
            {
                sLog.debug("Call state change on peer: " + evt.getNewValue());

                if (CallState.CALL_ENDED.equals(evt.getNewValue()))
                {
                    if (windowWasHidden)
                    {
                        sLog.debug("Call ended so hiding window");
                        hideWindow();
                    }
                }
            }
        });

        showCallParkWindow(window);
    }

    /**
     * Hide the CallParkWindow, if it exists.
     */
    static void hideWindow()
    {
        sLog.debug("Hide Call Park Window");

        if (sWindow != null)
        {
            // Window was hidden before the call, so hide it again
            sWindow.setVisible(false);

            // Make sure that we reset the always on top flag, or it will irritate users.
            sWindow.setAlwaysOnTop(false);
        }
    }

    @Override
    public void repaintWindow()
    {
        repaint();
    }

    /*
     * None of the ContactListContainer methods need to be implemented, as
     * CallParkWindow has no search field.  However, it must still implement
     * ContactListContainer to be able to link it to its implementation of
     * TreeContactList.
     */
    @Override
    public void enterKeyTyped(KeyEvent evt)
    {
    }

    @Override
    public void ctrlEnterKeyTyped(){}
    {
    }

    @Override
    public void clearCurrentSearchText(){}
    {
    }

    @Override
    public String getCurrentSearchText()
    {
        return null;
    }
}
