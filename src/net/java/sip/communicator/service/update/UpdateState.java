// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.update;

/**
 * Enum to represent all the possible states that the update dialog could be in.
 * Used to conditionally render different contents of the update dialog on the Electron side
 */
public enum UpdateState
{
    UP_TO_DATE,
    UPDATE_FORCED,
    UPDATE_OPTIONAL,
    ERROR_FORCED,
    ERROR_OPTIONAL,
}
