// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.callpark;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.analytics.AnalyticsService;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetCallPark.CallParkOrbit;
import net.java.sip.communicator.service.protocol.OperationSetCallPark.CallParkOrbitState;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;

/**
 * Class responsible for the user's interaction with each entry in the Tree of
 * Call Park Orbits. Note that there is only one editor per list of call park
 * orbits thus the editor has a field "mValue" which contains the object that
 * the currently selected cell represents
 */
public class CallParkCellEditor extends AbstractCellEditor implements TreeCellEditor
{
    private static final long serialVersionUID = 1L;
    private static final Logger sLog = Logger.getLogger(CallParkCellEditor.class);

    private final ResourceManagementService mRes = GuiActivator.getResources();

    /**
     * The object responsible for actually rendering each orbit.
     */
    private final CallParkComponent mComponent = new CallParkComponent();

    /**
     * The Object associated with this editor.
     */
    private Object mValue;

    /**
     * The menu that can be created to allow the user to choose which call to
     * park
     */
    private JPopupMenu mMenu;

    /**
     * The object that currently handles key presses.
     */
    private KeyAdapter mKeyPressHandler;

    private AnalyticsService mAnalytics = GuiActivator.getAnalyticsService();

    public CallParkCellEditor()
    {
        // Add park button listener
        mComponent.getParkButton().addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                mAnalytics.onEventWithIncrementingCount(AnalyticsEventType.CALL_PARKED, new ArrayList<>());

                sLog.user("Park button clicked");
                parkAction();
            }
        });

        // And pick up button listener
        mComponent.getPickUpButton().addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                mAnalytics.onEventWithIncrementingCount(AnalyticsEventType.CALL_PARK_PICKED_UP, new ArrayList<>());
                sLog.user("Pick up button clicked");
                pickupAction();
            }
        });
    }

    /**
     * The action of the park button
     */
    private void parkAction()
    {
        if (mValue instanceof CallParkUIContact)
        {
            CallParkUIContact uiContact = (CallParkUIContact) mValue;
            sLog.user("Call being parked: " + uiContact);
            List<Call> calls = CallParkComponent.getActiveCalls();

            if (calls.isEmpty())
            {
                // Shouldn't really happen, as the UI should prevent this.
                sLog.warn("No calls to park");
                showError("service.gui.ERROR",
                          "service.gui.CALL_PARK_NO_CALLS");
            }
            else if (calls.size() == 1)
            {
                sLog.info("Single call to park");
                parkCall(uiContact, calls.get(0));
            }
            else
            {
                sLog.info("Multiple calls to park");
                parkCalls(uiContact, calls);
            }
        }

        stopCellEditing();
    }

    /**
     * The action of the pick up button
     */
    private void pickupAction()
    {
        if (mValue instanceof CallParkUIContact)
        {
            CallParkOrbit orbit = ((CallParkUIContact)mValue).getDescriptor();
            CallParkOrbitState state = orbit.getState();

            sLog.user("Call being picked up on " + orbit.getOrbitCode() +
                              ", " + state);

            switch (state)
            {
            case BUSY:
                orbit.pickUpCall();
                break;

            case DISABLED_BUSY:
                showError("impl.protocol.sip.CALL_PARK_FAILED_BUSY_TITLE",
                          "impl.protocol.sip.CALL_PARK_FAILED_BUSY_TEXT",
                          orbit.getFriendlyName());
                break;

            case DISABLED_FREE:
                sLog.debug("Pick up button clicked consecutive times. State is still " + state);
                break;

            default:
                // Shouldn't happen, just log
                sLog.error("Attempt to retrieve call when state is " + state);
            }
        }

        stopCellEditing();
    }

    /**
     * Method for parking a call on the selected contact when there are multiple
     * calls
     *
     * @param uiContact The contact to park the call on
     * @param calls The calls to park
     */
    private void parkCalls(final CallParkUIContact uiContact,
                           List<Call> calls)
    {
        mMenu = new JPopupMenu();

        // Create a title for the menu
        ResourceManagementService res = GuiActivator.getResources();
        JLabel comp = new JLabel(res.getI18NString("service.gui.CALL_PARK_CHOOSE"));
        ScaleUtils.scaleFontAsDefault(comp);
        comp.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        mMenu.add(comp);
        mMenu.addSeparator();

        // For each call add an entry
        for (final Call call : calls)
        {
            CallPeer peer = call.getCallPeers().next();
            JMenuItem menuItem = new JMenuItem(peer.getDisplayName());
            ScaleUtils.scaleFontAsDefault(menuItem);

            ActionListener actionListener = e -> {
                parkCall(uiContact, call);
                mMenu.setVisible(false);
            };

            // Adding an action listener handles being clicked on with the mouse
            menuItem.addActionListener(actionListener);

            // Handling keyboard input is slightly more complicated
            ActionMap actionMap = menuItem.getActionMap();
            InputMap inputMap = menuItem.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

            String vkEnter = "VK_ENTER";
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), vkEnter);

            actionMap.put(vkEnter, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    actionListener.actionPerformed(e);
                }
            });

            mMenu.add(menuItem);
        }

        // Set the menu to appear in the right place
        try
        {
            JButton parkButton = mComponent.getParkButton();
            Point parkPoint = parkButton.getLocationOnScreen();
            mMenu.setLocation(parkPoint.x + 5, parkPoint.y + parkButton.getHeight());
            mMenu.setInvoker(parkButton);
            mMenu.requestFocusInWindow();

            mMenu.pack();
            mMenu.setVisible(true);
        }
        catch (Exception e)
        {
            mMenu = null;
        }
    }

    /**
     * Park a call
     *
     * @param contact The contact to use to park the call
     * @param call The call to park
     */
    private void parkCall(CallParkUIContact contact, Call call)
    {
        sLog.user("Call being parked on contact: " + contact);
        CallPeer peer = call.getCallPeers().next();
        contact.getDescriptor().parkCall(peer);
        mComponent.requestFocusInWindow();
    }

    @Override
    public Object getCellEditorValue()
    {
        return mValue;
    }

    @Override
    public Component getTreeCellEditorComponent(final JTree tree,
                                                final Object value,
                                                boolean selected,
                                                final boolean expanded,
                                                final boolean leaf,
                                                final int row)
    {
        Object newValue = mComponent.updateDisplay(value, selected, expanded, row);

        // Remove any key or mouse listeners that we once added
        for (KeyListener l : mComponent.getKeyListeners())
            mComponent.removeKeyListener(l);
        for (MouseListener l : mComponent.getMouseListeners())
            mComponent.removeMouseListener(l);

        if (newValue != null)
            mValue = newValue;

        if (mMenu != null)
        {
            mMenu.setVisible(false);
            mMenu = null;
        }

        // Add a key listener to enable keyboard navigation, escape closing the
        // window and enter performing the action
        mComponent.requestFocus();

        mKeyPressHandler = new KeyAdapter()
        {
            private boolean ignore = false;

            @Override
            public void keyPressed(KeyEvent event)
            {
                if (ignore || (mMenu != null && mMenu.isVisible()))
                    return;

                int keyCode = event.getKeyCode();

                switch (keyCode)
                {
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_UP:
                    // Up or down pressed, select the appropriate node
                    int maxRow = tree.getUI().getRowCount(tree);
                    int nextRow = keyCode == KeyEvent.VK_DOWN ?
                                                          (row + 1) : (row - 1);

                    if (nextRow >= 0 && nextRow < maxRow)
                    {
                        // Make sure that we ignore any subsequent events on
                        // this component as we are about to move off it.
                        ignore = true;

                        tree.stopEditing();

                        TreePath path = tree.getPathForRow(nextRow);
                        tree.setSelectionRow(nextRow);
                        tree.getUI().startEditingAtPath(tree, path);
                    }

                    break;

                case KeyEvent.VK_RIGHT:
                    // Right - expand the group
                    tree.expandRow(row);
                    break;

                case KeyEvent.VK_LEFT:
                    // Left, collapse the group or containing group
                    if (leaf || !expanded)
                    {
                        // Either this is a leaf (i.e. a contact) or a collapsed
                        // group.  In which case we should be collapsing the
                        // parent, not this.
                        TreePath parentPath = tree.getEditingPath().getParentPath();

                        // Only collapse the path if there is a visible parent
                        if (parentPath.getPathCount() > 1)
                            tree.collapsePath(parentPath);
                    }
                    else
                    {
                        tree.collapseRow(row);
                    }

                    break;

                case KeyEvent.VK_ESCAPE:
                    sLog.user("Escape pressed in call park window");
                    CallParkWindow.hideWindow();
                    break;

                case KeyEvent.VK_ENTER:
                    sLog.user("Enter pressed in call park window");
                    performButtonAction();
                    break;
                }
            }
        };

        mComponent.addKeyListener(mKeyPressHandler);
        mComponent.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (!leaf)
                {
                    if (expanded)
                    {
                        tree.collapseRow(row);
                    }
                    else
                    {
                        tree.expandRow(row);
                    }
                }
                else if (e.getClickCount() == 2)
                {
                    sLog.user("Double click in call park window");
                    performButtonAction();
                }
            }
        });

        return mComponent;
    }

    /**
     * Performs the action of the currently visible button.  Does nothing if
     * there is no such button.
     */
    private void performButtonAction()
    {
        // Note that only one button can ever be visible
        if (mComponent.getParkButton().isVisible() &&
            mComponent.getParkButton().isEnabled())
        {
            sLog.info("So parking call");
            parkAction();
        }
        else if (mComponent.getPickUpButton().isVisible() &&
                 mComponent.getPickUpButton().isEnabled())
        {
            sLog.info("So picking up call");
            pickupAction();
        }
    }

    /**
     * Show an error message
     *
     * @param titleRes the resource of the title of the error
     * @param msgRes the resource of the message of the error
     * @param msgArgs optional arguments to add to the message
     */
    private void showError(String titleRes, String msgRes, String... msgArgs)
    {
        String title = mRes.getI18NString(titleRes);
        String msg = mRes.getI18NString(msgRes, msgArgs);

        new ErrorDialog(title, msg).showDialog();
    }

    /**
     * @return true if the menu for choosing a call to park is showing
     */
    public boolean isMenuShowing()
    {
        return mMenu != null && mMenu.isVisible();
    }

    /**
     * Ask the currently selected component to handle a key press
     *
     * @param event the key event to handle
     */
    public void handleKeyPress(KeyEvent event)
    {
        if (mKeyPressHandler != null)
            mKeyPressHandler.keyPressed(event);
    }
}
