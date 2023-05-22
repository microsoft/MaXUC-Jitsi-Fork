// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.wispaservice;

/**
 * Interface enabling locations throughout the codebase to specify which
 * namespace they wish to use when sending/receiving a WISPA message.
 */
public enum WISPANamespace
{
    /**
     * All the in-client protocol types set as a WISPA namespace. Each should
     * have an associated NamespaceHandler implementation and an associated
     * .proto file in the shared Protobuf repository
     */
    ACTIVE_CALLS("/activecalls"),
    ANALYTICS("/analytics"),
    CONTACTS("/contacts"),
    CORE("/core"),
    CALL_HISTORY("/callhistory"),
    LDAP_SEARCHING("/ldapsearching"),
    MEETINGS("/meetings"),
    MESSAGING("/messaging"),
    EVENTS("/events"),
    PRESENCE("/presence"),
    TELEPHONY("/telephony"),
    SETTINGS("/settings"),
    VOICEMAILS("/voicemails"),
    USER("/user");
    // Add more here as new protocols are re-implemented.

    /**
     * The string representation of this namespace, i.e. the subdomain of the
     * port we send and receive this namespace's WISPA messages with.
     */
    private final String mName;

    /**
     * WISPA works by registering several namespaces against the main port, each
     * for handling a particular operation or protocol. This separates the
     * message flow for different responsibilities. WISPANamespace is an enum
     * containing all possible namespaces we can handle.
     *
     * @param name The name of this namespace, as appended to the port
     */
    WISPANamespace(String name)
    {
        mName = name;
    }

    /**
     * Get the name for appending to the port.
     *
     * @return The name of this namespace
     */
    public String toString()
    {
        return mName;
    }

    /**
     * The string sent on an ack to a register for this namespace, notifying the
     * connected interface that we're ready to send/receive other messages.
     *
     * @return The message to send on a register ack for this namespace
     */
    public String getRegisterMessage()
    {
        return "REGISTERED " + toString().split("/")[1].toUpperCase();
    }
}
