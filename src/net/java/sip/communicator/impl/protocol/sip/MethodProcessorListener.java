/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import javax.sip.message.*;

/**
 * Represents a listener which gets notified by the <tt>CallPeer</tt> it is
 * registered with about the processing of SIP signaling that the
 * <tt>CallPeer</tt> performs.
 *
 * @author Lubomir Marinov
 */
public interface MethodProcessorListener
{
    /**
     * Notifies this <tt>MethodProcessorListener</tt> that a specific
     * <tt>CallPeer</tt> has processed a specific SIP <tt>Request</tt> and has
     * replied to it with a specific SIP <tt>Response</tt>.
     *
     * @param sourceCallPeer the <tt>CallPeer</tt> which has processed the
     * specified SIP <tt>Request</tt>
     * @param request the SIP <tt>Request</tt> which has been processed by
     * <tt>sourceCallPeer</tt>
     * @param response the SIP <tt>Response</tt> sent by <tt>sourceCallPeer</tt>
     * as a reply to the specified SIP <tt>request</tt>
     */
    void requestProcessed(
            CallPeerSipImpl sourceCallPeer,
            Request request,
            Response response);

    /**
     * Notifies this <tt>MethodProcessorListener</tt> that a specific
     * <tt>CallPeer</tt> has processed a specific SIP <tt>Response</tt> and has
     * replied to it with a specific SIP <tt>Request</tt>.
     *
     * @param sourceCallPeer the <tt>CallPeer</tt> which has processed the
     * specified SIP <tt>Response</tt>
     * @param response the SIP <tt>Response</tt> which has been processed by
     * <tt>sourceCallPeer</tt>
     * @param request the SIP <tt>Request</tt> sent by <tt>sourceCallPeer</tt>
     * as a reply to the specified SIP <tt>response</tt>
     */
    void responseProcessed(
            CallPeerSipImpl sourceCallPeer,
            Response response,
            Request request);
}
