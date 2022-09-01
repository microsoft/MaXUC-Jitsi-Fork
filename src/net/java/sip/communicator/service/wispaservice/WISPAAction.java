// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.wispaservice;

/**
 * Enum containing all action types we wish to send or receive using the WISPA
 * API. Having this here makes it accessible both in the WISPA plugin package
 * and wider codebase.
 */
public enum WISPAAction
{
    /**
     * Actions a NamespaceHandler may be set up to receive.
     */
    REGISTER("register"),
    LIST("list"),
    GET("get"),
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    ACTION("action"),

    /**
     * Actions a NamespaceHandler may be set up to send.
     */
    DATA("data"),
    DATA_LIST("datalist"),
    DELETED("deleted"),
    MOTION("motion"),

    /**
     * Dummy event for testing and ending the list.
     */
    DUMMY_ACTION("dummy");

    /**
     * The string representation of this event, used to label the protobuf
     * messages we send/receive.
     */
    private final String mName;

    /**
     * A WISPAAction is the name attached to messages sent by Java as the server
     * to a client, so that the client may act accordingly. We also rely on
     * receiving events with specific names. We store all event names in this
     * enum, which is accessible throughout the codebase.
     *
     * @param name The name of this event as sent via the websocket connection
     */
    WISPAAction(String name)
    {
        mName = name;
    }

    /**
     * Parse as a string for use in websocket connections.
     *
     * @return The name of this event
     */
    public String toString()
    {
        return mName;
    }
}
