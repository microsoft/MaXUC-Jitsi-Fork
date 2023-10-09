// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.calljump;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * A service which allows the user to pull and push calls between Accession
 * clients.
 */
public interface CallJumpService
{
    /**
     * X-MSW-client-jumpto alert-info header.
     */
    String JUMPTO_HEADER_PREFIX = "X-MSW-client-jumpto;id=";

    /**
     * Registers the given CallJumpComponent to be notified when there is a
     * change to whether or not there is a call available to pull.
     *
     * @param callPullComponent the CallJumpComponent to be notified
     */
    void registerForCallPullUpdates(CallJumpComponent callPullComponent);

    /**
     * Registers the given CallJumpComponent to be notified when there is a
     * change to whether or not the given call is available to push.
     * @param callPushComponent the CallJumpComponent to be notified
     * @param call the Call
     */
    void registerForCallPushUpdates(CallJumpComponent callPushComponent,
                                    Call call);

    /**
     * Unregisters the given CallJumpComponent from being notified when there
     * is a change to whether or not the call with the given call ID is
     * available to push.
     * @param callPushComponent the CallJumpComponent
     */
    void unRegisterForCallPushUpdates(CallJumpComponent callPushComponent);

    /**
     * Pulls the given call onto this phone
     *
     * @param call The call to pull
     * @param callJumpComponent The component to notify in case of an error
     */
    void pull(final CallData call, CallJumpComponent callJumpComponent);

    /**
     * Pushes the given call from this phone
     * @param call The call to push
     * @param callJumpComponent The component to notify in case of an error
     */
    void push(final CallData call, CallJumpComponent callJumpComponent);

    /**
     * Pushes the given call from this phone
     * @param remoteParty The remote party the call to push is with
     */
    void push(String remoteParty);

    /**
     * Returns the alert-info header to use when requesting a call jump.
     *
     * @param isPush if true, a call push header will be returned, otherwise a
     * call pull header will be returned
     * @param includeId if true, this client's id will be included in the
     * header, otherwise it will be omitted.
     *
     * @return the call jump alert-info header, optionally including this
     * client's id.
     */
    static String getCallJumpAlertInfoHeader(boolean isPush, boolean includeId)
    {
        return "info=" + JUMPTO_HEADER_PREFIX + (isPush ? "!" : "") +
                (includeId ? ConfigurationUtils.getUuid().replaceAll("\\-", "") : "");
    }

    /**
     * An enum to represent the different possible results of a call jump
     * attempt.
     */
    enum CallJumpResult
    {
        SUCCESS,
        NO_CALL,
        FAILED,
        INVALID_TARGET,
        FEATURE_ALREADY_SET,
        SERVICE_NOT_SUPPORTED,
        NETWORK,
        CP_DATA_ERROR
    }
}
