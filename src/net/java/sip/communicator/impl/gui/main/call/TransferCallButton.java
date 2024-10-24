/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.call;

import java.util.*;

import com.google.common.annotations.VisibleForTesting;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Represents an UI means to transfer (the <tt>Call</tt> of) an associated
 * <tt>CallPariticant</tt>.
 *
 */
public class TransferCallButton extends InCallButton
                                implements CallChangeListener
{
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>Logger</tt> used by the <tt>TransferCallButton</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(TransferCallButton.class);

    /**
     * The call conference associated with this button
     */
    private CallConference callConference;

    /**
     * Initializes a new <tt>TransferCallButton</tt> instance which is to
     * transfer (the <tt>Call</tt> of) a specific
     * <tt>CallPeer</tt>.
     *
     * @param conference the <tt>CallConference</tt> to be associated with the new instance and
     * to be transfered
     */
    public TransferCallButton(CallConference conference)
    {
        super(ImageLoaderService.TRANSFER_CALL_BUTTON,
              ImageLoaderService.TRANSFER_CALL_BUTTON_ROLLOVER,
              ImageLoaderService.TRANSFER_CALL_BUTTON_FOCUS,
              ImageLoaderService.TRANSFER_CALL_BUTTON_PRESSED,
              null,
              null,
              ImageLoaderService.TRANSFER_CALL_BUTTON_FOCUS,
              ImageLoaderService.TRANSFER_CALL_BUTTON_DISABLED,
              GuiActivator.getResources().getI18NString("service.gui.TRANSFER_BUTTON_TOOL_TIP"));

        callConference = conference;

        Call call = null;
        for (Call c : conference.getCalls())
        {
            if (c.getCallPeers().hasNext())
            {
                call = c;
            }
        }

        if (call == null)
        {
            setEnabled(false);
            return;
        }

        conference.addCallChangeListener(this);

        // Initialize as disabled unless the call is already connected, it
        // will become enabled when the call connects. Call can be connected
        // but on hold, in which case we allow transfers.
        for (CallPeer peer : conference.getCallPeers())
        {
            if (!CallPeerState.CONNECTED.equals(peer.getState())
                && !CallPeerState.isOnHold(peer.getState()))
            {
                this.setEnabled(false);
            }
        }

        addActionListener(evt -> {
            logger.user("Call transfer button clicked in call window");
            transferCall();
        });
    }

    /**
     * Transfers the given <tt>callPeer</tt>.
     */
    @VisibleForTesting
    public void transferCall()
    {
        GuiActivator.getAnalyticsService().onEvent(AnalyticsEventType.START_TRANSFER);

        Call call = null;
        for (Call c : callConference.getCalls())
        {
            if (c.getCallPeers().hasNext())
            {
                call = c;
            }
        }

        if (call == null)
        {
            return;
        }

        OperationSetAdvancedTelephony<?> telephony
            = call.getProtocolProvider()
                .getOperationSet(OperationSetAdvancedTelephony.class);

        // If the telephony operation set is null we have nothing more to
        // do here.
        if (telephony == null)
            return;

        Collection<CallPeer> transferCalls = getTransferCallPeers();

        // We support transfer for one-to-one calls only.
        CallPeer initialPeer = call.getCallPeers().next();

        if (transferCalls.isEmpty())
        {
            // No active calls available for the transfer - display
            // TransferCallMenu which provides attended and unattended call
            // transfer options to choose from.
            TransferCallMenu transferCallMenu
                    = new TransferCallMenu(
                    TransferCallButton.this, initialPeer);

            transferCallMenu.showPopupMenu();
        }
        else
        {
            // At least one active call in progress - display
            // TransferActiveCallsMenu that allows the user to choose between
            // attended call transfer (to the peers from the active call list)
            // or unattended transfer to another number.
            TransferActiveCallsMenu activeCallsMenu
                = new TransferActiveCallsMenu(
                    TransferCallButton.this, initialPeer, transferCalls);

            activeCallsMenu.showPopupMenu();
        }
    }

    /**
     * Returns the list of transfer call peers.
     *
     * @return the list of transfer call peers
     */
    private Collection<CallPeer> getTransferCallPeers()
    {
        Collection<CallPeer> transferCalls = new LinkedList<>();
        Iterator<Call> activeCalls
            = CallManager.getInProgressCalls().iterator();

        while (activeCalls.hasNext())
        {
            Call activeCall = activeCalls.next();
            CallState activeCallState = activeCall.getCallState();
            if (!callConference.containsCall(activeCall)
                // We're only interested in one to one calls
                && activeCall.getCallPeerCount() == 1
                // we are interested only in connected calls
                && CallState.CALL_IN_PROGRESS.equals(activeCallState))
            {
                CallPeer peer = activeCall.getCallPeers().next();

                // We can't transfer calls to anonymous calls, so don't include them
                if (peer.getAddress() != null &&
                    !peer.getAddress().startsWith("anonymous"))
                {
                    transferCalls.add(peer);
                }
            }
        }
        return transferCalls;
    }

    public void callPeerAdded(CallPeerEvent evt)
    {
        // Nothing to do
    }

    public void callPeerRemoved(CallPeerEvent evt)
    {
        // Nothing to do
    }

    public void callStateChanged(CallChangeEvent evt)
    {
        // We only want the transfer call button to be enabled if this call is
        // connected
        if (callConference.containsCall(evt.getSourceCall()))
        {
            if (CallState.CALL_IN_PROGRESS.equals(evt.getSourceCall().getCallState()))
            {
                logger.debug("Call is connected so enable the transfer call button");
                this.setEnabled(true);
            }
            else
            {
                logger.debug("Call is not connected so disable the transfer call button");
                this.setEnabled(false);
            }
        }
    }
}
