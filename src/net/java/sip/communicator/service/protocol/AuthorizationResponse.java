/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

public enum AuthorizationResponse
{
    /**
     * Indicates that the source authorization request which this response is
     * about has been accepted and that the requestor may now proceed to adding
     * the user to their contact list.
     */
    ACCEPT,

    /**
     * Indicates that source authorization request which this response is about
     * has been rejected. A reason may also have been specified.
     */
    REJECT,

    /**
     * Indicates that source authorization request which this response is about
     * has been ignored and that no other indication will be sent to the
     * requestor.
     */
    IGNORE;
}
