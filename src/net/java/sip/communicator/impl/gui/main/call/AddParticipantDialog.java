// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.call;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.UIService.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import static org.jitsi.util.Hasher.logHasher;

/**
 * A class which allows the user to choose a participant to add to a call
 */
public class AddParticipantDialog extends AbstractCallContactInviteDialog
{
    private static final long serialVersionUID = 0L;
    private static final Logger sLog = Logger.getLogger(AddParticipantDialog.class);

    private final Call mCall;

    public AddParticipantDialog(Call call)
    {
        super(null,
              GuiActivator.getResources().getI18NString("service.gui.chat.ADD_PARTICIPANTS_TITLE"),
              GuiActivator.getResources().getI18NString("service.gui.addparticipant.OK_BUTTON"));
        mCall = call;
    }

    @Override
    protected void okPressed(UIContact uiContact,
                             String enteredText,
                             String phoneNumber)
    {
        if (uiContact != null)
        {
            // A contact was selected - invite it
            sLog.info("Adding contact to call " + uiContact);
            String number = getNumberFromContact(uiContact);

            if (number != null)
            {
                sLog.info("Number selected " + logHasher(number));
                CallManager.inviteToConferenceCall(mCall,
                                                   Reformatting.NEEDED,
                                                   number);
                setVisible(false);
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
                    // Valid phone number - invite it
                    sLog.info("Phone number entered: " + logHasher(phoneNumber));
                    CallManager.inviteToConferenceCall(mCall,
                                                       Reformatting.NOT_NEEDED,
                                                       phoneNumber);

                    setVisible(false);
                    dispose();
                }
                else
                {
                    // Invalid number entered - show error
                    sLog.info("Invalid number entered " + logHasher(enteredText));
                    showError("service.gui.add.ERROR",
                              "service.gui.ERROR_INVALID_NUMBER");
                }
            }
            else
            {
                // No number at all - show error
                sLog.info("No number at all entered");
                showError("service.gui.add.ERROR",
                          "service.gui.ERROR_NO_NUMBER");
            }
        }
    }
}
