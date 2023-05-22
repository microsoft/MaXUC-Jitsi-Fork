/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.callhistory.event;

import java.util.*;
import net.java.sip.communicator.service.callhistory.CallRecord;

/**
 * A call history change listener receives events indicating that the call
 * history has changed
 *
 */
public interface CallHistoryChangeListener
    extends EventListener
{
    /**
     * Indicates that a change has occurred in the call history
     * @param transactions the changes that are being notified of.
     */
    void callHistoryChanged(HashMap<CallRecord, Boolean> transactions);
}
