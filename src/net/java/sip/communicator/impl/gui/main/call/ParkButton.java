// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.callpark.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * In call button which opens (or brings to the front if already open) the call
 * park window to allow the user to park the call.
 */
public class ParkButton extends InCallButton
        implements CallChangeListener, ActionListener, PropertyChangeListener,
                                                              KeyEventDispatcher
{
    private static final long serialVersionUID = 1L;
    private static final Logger sLog = Logger.getLogger(ParkButton.class);

    /**
     * The call we are created for
     */
    private final Call mCall;

    public ParkButton(CallFrame callFrame, CallConference callConference)
    {
        super(ImageLoaderService.PARK_BUTTON,
              ImageLoaderService.PARK_BUTTON_ROLLOVER,
              ImageLoaderService.PARK_BUTTON_FOCUS,
              ImageLoaderService.PARK_BUTTON_PRESSED,
              null,
              null,
              ImageLoaderService.PARK_BUTTON_FOCUS,
              null,
              GuiActivator.getResources().getI18NString("service.gui.PARK_BUTTON_TOOL_TIP"));

        addActionListener(this);
        callConference.addCallChangeListener(this);
        callConference.addPropertyChangeListener(this);

        // Add a window listener so we can stop listening for the park call
        // keyboard shortcut when the call is over.
        callFrame.addWindowListener(
                new WindowAdapter()
                {
                    @Override
                    public void windowClosed(WindowEvent e)
                    {
                        KeyboardFocusManager.getCurrentKeyboardFocusManager().
                                      removeKeyEventDispatcher(ParkButton.this);
                    }
                });

        // There should be just one call - otherwise it's an error.
        List<Call> calls = callConference.getCalls();
        if (calls.size() == 1)
        {
            mCall = calls.get(0);
            setEnabled(mCall.getCallState().equals(CallState.CALL_IN_PROGRESS));
        }
        else
        {
            // Not a lot we can do if there is no call, or if there are multiple calls
            sLog.warn("Creating a park button with bad number of calls " + calls);
            callConference.removeCallChangeListener(this);
            callConference.removePropertyChangeListener(this);
            setVisible(false);
            mCall = null;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        sLog.user("Park call button pressed in call window");

        // This button is only visible when there is only one peer.  Thus it is
        // safe to call next(). Having said that if there is more than one peer
        // something has gone wrong, so log:
        if (mCall.getCallPeerCount() != 1)
        {
            sLog.error("Parking call with multiple peers " + mCall);
        }

        CallParkWindow.parkCall(mCall.getCallPeers().next(),
                                SwingUtilities.getWindowAncestor(this));
    }

    @Override
    public void callPeerAdded(CallPeerEvent evt)
    {
        sLog.debug("Call Peer added, setting button invisible");
        setVisible(false);
    }

    @Override
    public void callPeerRemoved(CallPeerEvent evt)
    {
        sLog.debug("Call peer removed, peer count: " + mCall.getCallPeerCount());
        setVisible(mCall.getCallPeerCount() <= 1);
    }

    @Override
    public void callStateChanged(CallChangeEvent evt)
    {
        if (mCall.getCallState().equals(CallState.CALL_IN_PROGRESS))
            setEnabled(true);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        sLog.debug("Property change, call peer count now " + mCall.getCallPeerCount());
        setVisible(mCall.getCallPeerCount() <= 1);
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        // We need to capture keyboard events to listen for the Park Call
        // keyboard shortcut, when the park button is enabled.
        KeyboardFocusManager keyboardFocusManager =
                          KeyboardFocusManager.getCurrentKeyboardFocusManager();
        if (!this.isEnabled() && enabled)
        {
            sLog.debug("Add keyboard listener for call park shortcut");
            keyboardFocusManager.addKeyEventDispatcher(this);
        }
        else if (this.isEnabled() && !enabled)
        {
            sLog.debug("Remove keyboard listener for call park shortcut");
            keyboardFocusManager.removeKeyEventDispatcher(this);
        }

        super.setEnabled(enabled);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e)
    {
        boolean handled = false;

        int key = e.getKeyCode();
        int modifiers = e.getModifiers();

        if ((key == OperationSetCallPark.CALL_PARK_SHORTCUT_KEY) &&
            (modifiers == OperationSetCallPark.CALL_PARK_SHORTCUT_KEY_MODIFIERS))
        {
            sLog.info("Park call key combo pressed");
            actionPerformed(null);
            handled = true;
        }

        return handled;
    }
}
