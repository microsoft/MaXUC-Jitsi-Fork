// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.callpark;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetCallPark.CallParkOrbit;
import net.java.sip.communicator.service.protocol.OperationSetCallPark.CallParkOrbitState;
import net.java.sip.communicator.util.Logger;

import org.jitsi.service.resources.*;

/**
 * Class responsible for the layout of each cell in the tree of Call Park orbits
 */
public class CallParkComponent extends JPanel
{
    private static final long serialVersionUID = 1L;
    private static final Logger sLog = Logger.getLogger(CallParkComponent.class);

    // Indentations used in the display
    private static final int GROUP_INDENT = ScaleUtils.scaleInt(20);
    private static final int INITIAL_INDENT = ScaleUtils.scaleInt(10);

    // Icons used in the display
    private static final ImageIconFuture COLLAPSED_ICON =
             GuiActivator.getResources().getImage("service.gui.COLLAPSED_ICON");
    private static final ImageIconFuture EXPANDED_ICON =
              GuiActivator.getResources().getImage("service.gui.EXPANDED_ICON");

    // Colors used in the display:
    private static final Color BACKGROUND_GRADIENT_TOP = new Color(0xffffff);
    private static final Color BACKGROUND_GRADIENT_BOTTOM = new Color(0xf8f8f8);
    private static final Color GROUP_BORDER = new Color(0xc2c2c2);

    private final ResourceManagementService mRes = GuiActivator.getResources();

    /**
     * The left hand label that displays either the group name or the orbit code
     */
    private final JLabel mLeftLabel = new JLabel();

    /**
     * The right hand label that displays either the groups orbit state, or the
     * orbit code friendly name
     */
    private final JLabel mRightLabel = new JLabel();

    /**
     * The pick up call button
     */
    private final JButton mPickUpButton;

    /**
     * The park call button
     */
    private final JButton mParkButton;

    /**
     * If true then paint a gradient on the background of this component
     */
    private boolean mPaintGradient;

    /**
     * The string used for picking up a call
     */
    private final String pickUp;

    /**
     * The string used for parking a call
     */
    private final String park;

    /**
     * The string used when no actions are available
     */
    private final String noActions;

    public CallParkComponent()
    {
        super(new BorderLayout());

        ResourceManagementService res = GuiActivator.getResources();
        pickUp = res.getI18NString("service.gui.CALL_PARK_PICK_UP");
        park = res.getI18NString("service.gui.CALL_PARK_PARK");
        noActions = res.getI18NString("service.gui.CALL_PARK_NO_ACTIONS");

        // Create the buttons - don't bother adding action listeners as they
        // will be lost when the UI is rendered
        mPickUpButton = new SIPCommSnakeButton(pickUp, "service.gui.button.retrieve");
        mParkButton = new SIPCommSnakeButton(park, "service.gui.button.park");
        mPickUpButton.setFocusable(false);
        mParkButton.setFocusable(false);

        JPanel buttonsPanel = new TransparentPanel();
        buttonsPanel.add(mParkButton);
        buttonsPanel.add(mPickUpButton);
        setMinimumSize(new Dimension(1, mParkButton.getPreferredSize().height));

        add(mLeftLabel, BorderLayout.LINE_START);
        add(mRightLabel, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.LINE_END);
    }

    /**
     * @return the button used to pick up a call
     */
    public JButton getPickUpButton()
    {
        return mPickUpButton;
    }

    /**
     * @return the button used to park a call
     */
    public JButton getParkButton()
    {
        return mParkButton;
    }

    /**
     * @return the list of all active calls, suitable for parking
     */
    public static List<Call> getActiveCalls()
    {
        Collection<Call> allCalls = CallManager.getInProgressCalls();

        // We only allow parking of calls with a single peer:
        List<Call> calls = new ArrayList<>();

        for (Call call : allCalls)
        {
            if (call.getCallPeerCount() == 1)
            {
                calls.add(call);
            }
        }

        return calls;
    }

    /**
     * Update the display of this component according to the state of the row
     * that we are being set to display
     *
     * @param value The object to display
     * @param isSelected True if this row is selected
     * @param expanded True if this is a group node that is expanded
     * @param row The row number of this entry
     * @return the object that this component represents
     */
    public Object updateDisplay(Object value,
                                boolean isSelected,
                                boolean expanded,
                                int row)
    {
        Object newValue = null;

        if (value instanceof ContactNode)
        {
            ContactNode node = (ContactNode)value;

            UIContactImpl contactDescriptor = node.getContactDescriptor();

            // The call park window contains an instance of TreeContactList to
            // display the call park orbits, which are represented by
            // instances of CallParkUIContact.  Whenever any instance of
            // TreeContactList is updated, the Call Park window is notified,
            // however we only care about updates to CallParkUIContacts, so
            // we can safely ignore all others.
            if (contactDescriptor instanceof CallParkUIContact)
            {
                CallParkUIContact uiContact =
                                         (CallParkUIContact) contactDescriptor;
                CallParkOrbit orbit = uiContact.getDescriptor();

                TreeNode parent = node.getParent();
                int positionInGroup =
                    parent == null ? row : parent.getIndex(node);

                setOrbit(orbit, isSelected, positionInGroup, node);
                newValue = uiContact;
            }
        }
        else if (value instanceof GroupNode)
        {
            GroupNode node = (GroupNode) value;

            if (node.isRoot())
            {
                setVisible(false);
            }

            UIGroupImpl descriptor = node.getGroupDescriptor();

            if (descriptor.getDescriptor() instanceof ContactGroup)
            {
                ContactGroup group = (ContactGroup) descriptor.getDescriptor();
                CallParkUIGroup uiGroup = (CallParkUIGroup) descriptor;
                setGroup(group, expanded, isSelected, node, uiGroup);
            }
            else
            {
                setVisible(false);
            }

            newValue = descriptor;
        }

        return newValue;
    }

    /**
     * Set up this component so that it displays an orbit
     *
     * @param orbit The orbit to display
     * @param selected True if this entry is selected
     * @param position The position of this element in its group
     * @param node The node that we are displaying
     */
    private void setOrbit(CallParkOrbit orbit,
                          boolean selected,
                          int position,
                          DefaultMutableTreeNode node)
    {
        mPaintGradient = false;

        // Set up the label text
        mLeftLabel.setIcon(null);
        mLeftLabel.setText(orbit.getOrbitCode());
        mRightLabel.setText(orbit.getFriendlyName());
        mRightLabel.setForeground(Color.BLACK);
        mRightLabel.setVisible(true);

        CallParkOrbitState state = orbit.getState();
        int style = state.isBusy() ? Font.BOLD : Font.PLAIN;
        Font font = mLeftLabel.getFont().deriveFont(
            style, ScaleUtils.getDefaultFontSize());
        mLeftLabel.setFont(font);
        mRightLabel.setFont(font);

        // Button visibility - pick up is always shown if appropriate, park only
        // if the row is selected.
        sLog.debug("Set orbit position: " + position + " state: " + state + " selected: " + selected);
        mPickUpButton.setVisible(state.isBusy());
        mParkButton.setVisible(selected && state == CallParkOrbitState.FREE);

        if (mParkButton.isVisible())
            mParkButton.setEnabled(!getActiveCalls().isEmpty());

        Color bgColor = selected ? Constants.SELECTED_COLOR :
                        (position %2 == 0) ? Constants.CALL_HISTORY_EVEN_ROW_COLOR :
                        Color.WHITE;

        setPreferredSize(new ScaledDimension(300, 35));
        setBackground(bgColor);

        // If this is the first contact in a group, then add the lower border of
        // the group above it.
        setBorder((position == 0 && node.getLevel() > 1) ?
                     BorderFactory.createMatteBorder(1, 0, 0, 0, GROUP_BORDER) :
                     BorderFactory.createEmptyBorder());
        setOpaque(true);

        int indentation = calculateGroupIndentation(node);
        mLeftLabel.setBorder(BorderFactory.createEmptyBorder(0, indentation, 0, INITIAL_INDENT));
    }

    /**
     * Set up this component so that it displays a group
     *
     * @param group The group to display
     * @param expanded True if this group has been expanded.
     * @param selected True if this group is selected
     * @param node The node that we are displaying
     * @param uiGroup The uiGroup that we are displaying
     */
    private void setGroup(ContactGroup group,
                          boolean expanded,
                          boolean selected,
                          DefaultMutableTreeNode node,
                          CallParkUIGroup uiGroup)
    {
        mLeftLabel.setText(group.getGroupName());
        (expanded ? EXPANDED_ICON : COLLAPSED_ICON).addToLabel(mLeftLabel);
        mLeftLabel.setFont(mLeftLabel.getFont().deriveFont(
            Font.PLAIN, ScaleUtils.getDefaultFontSize()));

        int busyOrbits = uiGroup.getCountBusyOrbits();
        if (busyOrbits != 0 && !expanded)
        {
            // We've got some busy orbits, and the group is not expanded - add a
            // label saying how many there are
            mRightLabel.setVisible(true);
            mRightLabel.setFont(mRightLabel.getFont().deriveFont(
                Font.ITALIC, ScaleUtils.getDefaultFontSize()));
            mRightLabel.setForeground(Color.GRAY);

            int allOrbits = uiGroup.getCountAllOrbits();
            String text;

            if (busyOrbits == allOrbits)
            {
                text = mRes.getI18NString("service.gui.CALL_PARK_ALL_USED");
            }
            else if (busyOrbits == 1)
            {
                text = mRes.getI18NString("service.gui.CALL_PARK_ONE_USED");
            }
            else
            {
                text = mRes.getI18NString("service.gui.CALL_PARK_SOME_USED",
                                      new String[]{String.valueOf(busyOrbits)});
            }

            mRightLabel.setText(" " + text);
        }
        else
        {
            mRightLabel.setVisible(false);
        }

        mParkButton.setVisible(false);
        mPickUpButton.setVisible(false);

        if (selected)
        {
            setBackground(Constants.SELECTED_COLOR);
            setOpaque(true);
        }
        else
        {
            mPaintGradient = true;
            setOpaque(false);
        }

        setPreferredSize(new ScaledDimension(300, 35));
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, GROUP_BORDER));

        int indentation = calculateGroupIndentation(node);
        mLeftLabel.setBorder(BorderFactory.createEmptyBorder(0, indentation, 0, 0));
    }

    /**
     * Calculate the correct indentation for a node
     *
     * @param node the node to calculate the indentation for
     * @return the amount to indent the node by
     */
    private int calculateGroupIndentation(DefaultMutableTreeNode node)
    {
        return INITIAL_INDENT + (node.getLevel() - 1) * GROUP_INDENT;
    }

    @Override
    public void paint(Graphics g)
    {
        if (mPaintGradient)
        {
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

        super.paint(g);
    }

    /**
     * Gets the text used for the orbit or group name
     *
     * @return the text used for the orbit or group name
     */
    public String getLeftLabelText()
    {
        return mLeftLabel.getText();
    }

    /**
     * Gets the text used for the orbit friendly name or group state
     *
     * @return the text used for the orbit friendly name or group state
     */
    public String getRightLabelText()
    {
        return mRightLabel.isVisible() ? mRightLabel.getText() : "";
    }

    /**
     * Gets a string representation of the action available to the user
     *
     * @return a string representation of the action available to the user
     */
    public String getActionState()
    {
        String actionState;

        if (mParkButton.isEnabled() && mParkButton.isVisible())
        {
            actionState = park;
        }
        else if (mPickUpButton.isEnabled() && mPickUpButton.isVisible())
        {
            actionState = pickUp;
        }
        else
        {
            actionState = noActions;
        }

        return actionState;
    }
}
