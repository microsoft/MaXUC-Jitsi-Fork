// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.call;

import static org.jitsi.util.Hasher.logHasher;

import net.java.sip.communicator.impl.gui.utils.AbstractCallContactInviteDialog;
import net.java.sip.communicator.service.gui.UIContact;
import net.java.sip.communicator.service.gui.UIService.Reformatting;
import net.java.sip.communicator.service.protocol.CallPeer;
import net.java.sip.communicator.util.Logger;
import org.jitsi.util.StringUtils;

/**
 * A class which allows the user to choose a number either from a contact or by
 * entering it themselves to invite them to a call (attended call transfer) or
 * transfer to another call (unattended).
 */
public class TransferCallDialog extends AbstractCallContactInviteDialog
{
    private static final long serialVersionUID = 0L;
    private static final Logger sLog = Logger.getLogger(TransferCallDialog.class);

    /**
     * The peer to transfer in the case of an unattended call transfer.
     */
    private final CallPeer transferPeer;

    /**
     * A flag that indicates the type of the dialog (attended/unattended).
     */
    private final CallTransferType callTransferType;

    /**
     * Creates a dialog by specifying the peer to transfer and the dialog data.
     *
     * @param peer             the peer to transfer
     * @param callTransferType call transfer type that determines dialog text
     */
    TransferCallDialog(final CallPeer peer, CallTransferType callTransferType)
    {
        super(null, callTransferType.getTitle(), callTransferType.getOkButtonText());
        this.transferPeer = peer;
        this.callTransferType = callTransferType;
    }

    protected void okPressed(UIContact uiContact, String enteredText, String phoneNumber)
    {
        if (uiContact != null)
        {
            // A contact was selected - call/transfer to it.
            // No need to hash contact as UIContact's toString() already does so.
            sLog.info("Ok pressed on contact " + uiContact);
            String number = getNumberFromContact(uiContact);

            if (number != null)
            {
                sLog.info("Got a number from the contact " + logHasher(number));

                callTransfer(number, transferPeer, Reformatting.NEEDED);

                setVisible(false);
                dispose();
            }
        }
        else
        {
            String errorTitle = CallTransferType.ATTENDED == callTransferType ?
                    "service.gui.CALL_FAILED" : "service.gui.transfer.ERROR";

            // No contact was selected so just try to call/transfer to the
            // entered number
            if (!StringUtils.isNullOrEmpty(enteredText))
            {
                if (!StringUtils.isNullOrEmpty(phoneNumber))
                {
                    sLog.info("Transferring to a number " + logHasher(phoneNumber));

                    callTransfer(enteredText, transferPeer, Reformatting.NOT_NEEDED);

                    setVisible(false);
                    dispose();
                }
                else
                {
                    // Invalid number entered - show error
                    sLog.info("Invalid number entered");
                    showError(errorTitle,
                              "service.gui.ERROR_INVALID_NUMBER");
                }
            }
            else
            {
                // No number at all - show error
                sLog.info("No number entered");
                showError(errorTitle,
                          "service.gui.ERROR_NO_NUMBER");
            }
        }
    }

    private void callTransfer(String number, CallPeer transferPeer, Reformatting reformatNeeded)
    {
        if (CallTransferType.ATTENDED == callTransferType)
        {
            // Create a call in the case of an attended call transfer dialog
            CallManager.createCall(number, reformatNeeded);
        }
        else
        {
            // Transfer the call in the case of unattended call transfer dialog
            CallManager.transferCall(transferPeer, number, reformatNeeded);
        }
    }
}
