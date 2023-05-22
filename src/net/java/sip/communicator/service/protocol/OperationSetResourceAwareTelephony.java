/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.service.protocol.emergencylocation.EmergencyCallContext;

/**
 * The <tt>OperationSetResourceAwareTelephony</tt> defines methods for creating
 * a call toward a specific resource, from which a callee is connected.
 *
 * @author Yana Stamcheva
 */
public interface OperationSetResourceAwareTelephony
    extends OperationSet
{
    /**
     * Creates a new <tt>Call</tt> and invites a specific <tt>CallPeer</tt>
     * given by her <tt>Contact</tt> on a specific <tt>ContactResource</tt> to
     * it.
     *
     * @param callee the address of the callee who we should invite to a new
     * call
     * @param calleeResource the specific resource to which the invite should be
     * sent
     * @return a newly created <tt>Call</tt>. The specified <tt>callee</tt> is
     * available in the <tt>Call</tt> as a <tt>CallPeer</tt>
     */
    Call createCall(Contact callee, ContactResource calleeResource)
    ;

    /**
     * Creates a new <tt>Call</tt> and invites a specific <tt>CallPeer</tt>
     * given by her <tt>Contact</tt> on a specific <tt>ContactResource</tt> to
     * it.
     *
     * @param callee the address of the callee who we should invite to a new
     * call
     * @param calleeResource the specific resource to which the invite should be
     * sent
     * @return a newly created <tt>Call</tt>. The specified <tt>callee</tt> is
     * available in the <tt>Call</tt> as a <tt>CallPeer</tt>
     */
    Call createCall(String callee, String calleeResource)
    ;

    /**
     * Creates a new <tt>Call</tt> and invites a specific <tt>CallPeer</tt> to
     * it given by her <tt>String</tt> URI.
     *
     * @param uri the address of the callee who we should invite to a new
     * <tt>Call</tt>
     * @param calleeResource the specific resource to which the invite should be
     * sent
     * @param conference the <tt>CallConference</tt> in which the newly-created
     * <tt>Call</tt> is to participate
     * @param emergency an object providing context for an emergency call.  This
     * is set to null for non-emergency calls.
     * @return a newly created <tt>Call</tt>. The specified <tt>callee</tt> is
     * available in the <tt>Call</tt> as a <tt>CallPeer</tt>
     */
    Call createCall(String uri,
                    String calleeResource,
                    CallConference conference,
                    EmergencyCallContext emergency)
    ;

    /**
     * Creates a new <tt>Call</tt> and invites a specific <tt>CallPeer</tt>
     * given by her <tt>Contact</tt> to it.
     *
     * @param callee the address of the callee who we should invite to a new
     * call
     * @param calleeResource the specific resource to which the invite should be
     * sent
     * @param conference the <tt>CallConference</tt> in which the newly-created
     * <tt>Call</tt> is to participate
     * @param emergency an object providing context for an emergency call.  This
     * is set to null for non-emergency calls.
     * @return a newly created <tt>Call</tt>. The specified <tt>callee</tt> is
     * available in the <tt>Call</tt> as a <tt>CallPeer</tt>
     */
    Call createCall(Contact callee,
                    ContactResource calleeResource,
                    CallConference conference,
                    EmergencyCallContext emergency)
    ;
}
