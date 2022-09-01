// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.call;

import java.util.*;
import java.util.Timer;

import javax.swing.*;

import org.jitsi.service.resources.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.service.analytics.*;
import net.java.sip.communicator.service.calljump.*;
import net.java.sip.communicator.service.calljump.CallJumpService.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * A button which appears in the call frame to enable a user to switch their call to a different device/client.
 */
class CallPushButton extends InCallButton implements CallJumpComponent
{
    private static final Logger sLog = Logger.getLogger(CallPushButton.class);

    private static final ResourceManagementService sResources = GuiActivator.getResources();
    private static final CallJumpService sCallJumpService = GuiActivator.getCallJumpService();

    private static final int FAILED_TIMEOUT = 10000;
    private static final int DISABLED_TIMEOUT = 15000;

    private CallFrame mCallFrame;
    private CallData mCallData;

    CallPushButton(CallFrame callFrame)
    {
        super(ImageLoaderService.CALL_PUSH_BUTTON,
              ImageLoaderService.CALL_PUSH_BUTTON_ROLLOVER,
              ImageLoaderService.CALL_PUSH_BUTTON_FOCUS,
              ImageLoaderService.CALL_PUSH_BUTTON_PRESSED,
              null,
              null,
              null,
              ImageLoaderService.CALL_PUSH_BUTTON_DISABLED,
              sResources.getI18NString("service.calljump.PUSH_CALL_TOOL_TIP"));

        mCallFrame = callFrame;

        addActionListener((e) -> onClick());
        setEnabled(false);

        List<Call> calls = mCallFrame.getCallConference().getCalls();

        if (calls.size() == 1)
        {
            sCallJumpService.registerForCallPushUpdates(this, calls.get(0));
        }
        else
        {
            sLog.info("Conference does not correspond to unique call - call push will remain disabled");
        }
    }

    /**
     * Called when the user clicks this button.
     */
    private void onClick()
    {
        sLog.user("'Push Call' clicked in call window");
        GuiActivator.getAnalyticsService().onEvent(AnalyticsEventType.CALL_PUSHED);

        sCallJumpService.push(mCallData, this);
        sCallJumpService.unRegisterForCallPushUpdates(this);
        setEnabled(false);
        mCallFrame.getCallConference()
                  .setCallStateTransient(CallConference.CallConferenceStateEnum.SWITCHING, DISABLED_TIMEOUT);
        mCallFrame.updateCallFrameTitle();

        // After a delay, re-enable the button and reset the call frame title text
        new Timer().schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                setEnabled(true);
            }
        }, DISABLED_TIMEOUT);
    }

    @Override
    public void setEnabled(boolean enabled, CallData callData)
    {
        sLog.debug((enabled ? "Enabling" : "Disabling") + " call push button");
        mCallData = callData;
        SwingUtilities.invokeLater(() -> setEnabled(enabled));
    }

    @Override
    public void onDataError(CallJumpResult errorStatus)
    {
        // An error occurred in trying to push the call - change the title text of the call frame and re-enable
        // the button
        setEnabled(true);
        mCallFrame.getCallConference()
                  .setCallStateTransient(CallConference.CallConferenceStateEnum.SWITCH_FAILED, FAILED_TIMEOUT);
        mCallFrame.updateCallFrameTitle();
    }

    /**
     * Should be called when this button's container is disposed of.
     */
    public void dispose()
    {
        sLog.debug("Disposing of call push button");
        sCallJumpService.unRegisterForCallPushUpdates(this);
    }
}
