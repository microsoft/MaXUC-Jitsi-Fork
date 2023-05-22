/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.call;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.UIService.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import static org.jitsi.util.Hasher.logHasher;

/**
 * Represents a <tt>Dialog</tt> which allows specifying the target contact
 * address of a transfer-call operation.
 *
 * @author Yana Stamcheva
 */
public class TransferCallDialog extends AbstractCallContactInviteDialog
{
    private static final long serialVersionUID = 0L;

    private static final Logger sLog = Logger.getLogger(TransferCallDialog.class);
    /**
     * The peer to transfer.
     */
    private final CallPeer transferPeer;

    /**
     * Creates a <tt>TransferCallDialog</tt> by specifying the peer to transfer
     * @param peer the peer to transfer
     */
    public TransferCallDialog(final CallPeer peer)
    {
        super(null,
              GuiActivator.getResources().getI18NString("service.gui.TRANSFER"),
              GuiActivator.getResources().getI18NString("service.gui.TRANSFER_CALL_TITLE"));

        this.transferPeer = peer;
    }

    protected  void okPressed(UIContact uiContact,
                              String enteredText,
                              String phoneNumber)
    {
        if (uiContact != null)
        {
            // A contact was selected - transfer to it.
            // No need to hash contact as UIContact's toString() already does so.
            sLog.info("Ok pressed on contact " + uiContact);
            String number = getNumberFromContact(uiContact);

            if (number != null)
            {
                sLog.info("Got a number from the contact " + logHasher(number));
                CallManager.transferCall(transferPeer, number, Reformatting.NEEDED);
                setVisible(false);
                dispose();
            }
        }
        else
        {
            // No contact was selected so just try to transfer to the
            // entered number
            if (enteredText != null && enteredText.length() > 0)
            {
                if (phoneNumber != null && phoneNumber.length() > 0)
                {
                    // We should reformat the number if the user has entered
                    // the number in E164 format.
                    sLog.info("Transferring to a number " + logHasher(phoneNumber));

                    // Valid phone number - transfer it
                    CallManager.transferCall(transferPeer,
                                             enteredText,
                                             Reformatting.NOT_NEEDED);

                    setVisible(false);
                    dispose();
                }
                else
                {
                    // Invalid number entered - show error
                    sLog.info("Invalid number entered");
                    showError("service.gui.transfer.ERROR",
                              "service.gui.ERROR_INVALID_NUMBER");
                }
            }
            else
            {
                // No number at all - show error
                sLog.info("No number entered");
                showError("service.gui.transfer.ERROR",
                          "service.gui.ERROR_NO_NUMBER");
            }
        }
    }
}
