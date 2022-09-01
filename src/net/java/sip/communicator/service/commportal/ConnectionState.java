// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.commportal;

// Information about any errors we have connecting to the solution.
public class ConnectionState
{
    public boolean chatDisconnected = false;
    public boolean callsDisconnected = false;

    public String toString()
    {
        return "chatDisconnected: " + chatDisconnected + ", callsDisconnected: " + callsDisconnected;
    }
}
