// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.calljump;

import net.java.sip.communicator.service.calljump.CallJumpService.*;

/**
 * A component that will be enabled/disabled when a call becomes
 * available/unavailable to pull/push.
 */
public interface CallJumpComponent
{
    /**
     * Sets the enabled state of the component.  If it should be
     * enabled, callData will contain details of the call that is available to
     * be pulled/pushed, otherwise callData will be null.
     *
     * @param enabled if true, the component should be enabled.
     * @param callData the CallData object representing the call that is
     * available to be pulled/pushed, or null if there is none.
     */
    void setEnabled(boolean enabled, CallData callData);

    /**
     * Called if there is a problem with the CommPortal request to pull/push
     * the call
     *
     * @param errorStatus The CallJumpStatus that represents the error that was
     * returned by CommPortal
     */
    void onDataError(CallJumpResult errorStatus);
}
