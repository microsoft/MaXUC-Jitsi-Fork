// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.diagnostics;

/**
 * An interface for objects that dump state to error reports
 */
public interface StateDumper
{
    /**
     * @return the name to associated with this state
     */
    String getStateDumpName();

    /**
     * @return the state to dump to file
     */
    String getState();
}
