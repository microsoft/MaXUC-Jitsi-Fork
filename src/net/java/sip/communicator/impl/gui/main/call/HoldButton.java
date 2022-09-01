// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

import org.jitsi.util.*;

/**
 * A hold button that pertains to a call. It is displayed in the action
 * buttons bar at the bottom of a call frame
 */
public class HoldButton
    extends InCallDisableButton
    implements ActionListener,
               CallPeerListener,
               CallChangeListener
{
    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(HoldButton.class);

    /**
     * The call frame that this button belongs to
     */
    private CallFrame parentFrame;

    /**
     * Create a new call hold button
     *
     * @param callFrame the call frame that creates us
     */
    public HoldButton(CallFrame callFrame)
    {
        // The hold button
        super(ImageLoaderService.HOLD_BUTTON,
              ImageLoaderService.HOLD_BUTTON_ROLLOVER,
              ImageLoaderService.HOLD_BUTTON_FOCUS,
              ImageLoaderService.HOLD_BUTTON_PRESSED,
              ImageLoaderService.HOLD_BUTTON_ON,
              ImageLoaderService.HOLD_BUTTON_ON_ROLLOVER,
              ImageLoaderService.HOLD_BUTTON_ON_FOCUS,
              null,
              GuiActivator.getResources().getI18NString("service.gui.HOLD_BUTTON_TOOL_TIP"));

        addActionListener(this);
        callFrame.getCallConference().addCallChangeListener(this);
        parentFrame = callFrame;

        for (CallPeer peer : callFrame.getCallConference().getCallPeers())
        {
            peer.addCallPeerListener(this);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        logger.user("Hold button clicked in call window");
        // If the button has been recently clicked, ignore this click.
        if (!shouldHandleAction())
        {
            logger.debug("Ignore hold button click while recently clicked");
            return;
        }

        // This hold button changes the hold state of all call peers
        // unless they were already locally on hold
        logger.debug("Toggle hold state");
        boolean putOnHold = (!isSelected());
        synchronized (parentFrame.callPeers)
        {
            for (PeerPanel peerPanel : parentFrame.callPeers.values())
            {
                InCallButton btnLocalHold = peerPanel.getLocalHoldButton();
                if (btnLocalHold != null)
                {
                    btnLocalHold.setSelected(putOnHold);
                }

                peerPanel.setOnHold(putOnHold);
            }

            setSelected(putOnHold);
        }
    }

    @Override
    public void peerStateChanged(CallPeerChangeEvent evt)
    {
        boolean allPeersOnHold = true;

        // Whether all the peers in the call are still connecting. Assume that
        // all peers are trying to connect until we find one that isn't.
        boolean allPeersConnecting = true;

        for (CallPeer peer : parentFrame.getCallConference().getCallPeers())
        {
            if (!(peer.getState() == CallPeerState.ON_HOLD_LOCALLY ||
                  peer.getState() == CallPeerState.ON_HOLD_MUTUALLY))
            {
                allPeersOnHold = false;
            }

            if (!CallPeerState.isConnecting(peer.getState()) &&
                !CallPeerState.isRinging(peer.getState()))
            {
                // If the peer is not connecting and not ringing, this means we
                // have found a peer that's no longer trying to be connected.
                allPeersConnecting = false;
            }

            if (allPeersOnHold == false && allPeersConnecting == false)
            {
                // No longer have to proceed with the search
                break;
            }
        }

        logger.debug("Update hold button to selected: " + allPeersOnHold + " enabled: " + !allPeersConnecting);
        setSelected(allPeersOnHold);

        // Hold Button should not be enabled if the peers are still connecting.
        setEnabled(!allPeersConnecting);
    }

    @Override
    public void peerDisplayNameChanged(CallPeerChangeEvent evt){}

    @Override
    public void peerAddressChanged(CallPeerChangeEvent evt){}

    @Override
    public void peerTransportAddressChanged(CallPeerChangeEvent evt){}

    @Override
    public void peerImageChanged(CallPeerChangeEvent evt){}

    @Override
    public void callPeerAdded(CallPeerEvent evt)
    {
        evt.getSourceCallPeer().addCallPeerListener(this);
    }

    @Override
    public void callPeerRemoved(CallPeerEvent evt)
    {
        evt.getSourceCallPeer().removeCallPeerListener(this);
    }
    @Override
    public void callStateChanged(CallChangeEvent evt){}
}
