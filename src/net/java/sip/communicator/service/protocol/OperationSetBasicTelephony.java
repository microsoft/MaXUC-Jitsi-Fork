/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.text.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.emergencylocation.EmergencyCallContext;
import net.java.sip.communicator.service.protocol.event.*;

import org.jitsi.service.neomedia.*;

/**
 * An Operation Set defining all basic telephony operations such as conducting
 * simple calls etc. Note that video is not considered as a part of a
 * supplementary operation set and if included in the service should be available
 * behind the basic telephony set.
 *
 * @param <T> the provider extension class like for example
 * <tt>ProtocolProviderServiceSipImpl</tt> or
 * <tt>ProtocolProviderServiceJabberImpl</tt>
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public interface OperationSetBasicTelephony<T extends ProtocolProviderService>
    extends OperationSet
{
    /**
     * The name of the property that contains the maximum port number that we'd
     * like our RTP managers to bind upon.
     */
    String MAX_MEDIA_PORT_NUMBER_PROPERTY_NAME
        = "net.java.sip.communicator.service.protocol.MAX_MEDIA_PORT_NUMBER";

    /**
     * The name of the property that contains the minimum port number that we'd
     * like our RTP managers to bind upon.
     */
    String MIN_MEDIA_PORT_NUMBER_PROPERTY_NAME
        = "net.java.sip.communicator.service.protocol.MIN_MEDIA_PORT_NUMBER";

    /**
     * The name of the property that contains the minimum port number that we'd
     * like our Video RTP managers to bind upon.
     */
    String MIN_VIDEO_PORT_NUMBER_PROPERTY_NAME
        = "net.java.sip.communicator.service.protocol.MIN_VIDEO_PORT_NUMBER";

    /**
     * The name of the property that contains the maximum port number that we'd
     * like our Video RTP managers to bind upon.
     */
    String MAX_VIDEO_PORT_NUMBER_PROPERTY_NAME
        = "net.java.sip.communicator.service.protocol.MAX_VIDEO_PORT_NUMBER";

    /**
     * The name of the property that contains the minimum port number that we'd
     * like our Audio RTP managers to bind upon.
     */
    String MIN_AUDIO_PORT_NUMBER_PROPERTY_NAME
        = "net.java.sip.communicator.service.protocol.MIN_AUDIO_PORT_NUMBER";

    /**
     * The name of the property that contains the maximum port number that we'd
     * like our Audio RTP managers to bind upon.
     */
    String MAX_AUDIO_PORT_NUMBER_PROPERTY_NAME
        = "net.java.sip.communicator.service.protocol.MAX_AUDIO_PORT_NUMBER";

    /**
     * Reason code used to hangup peer, indicates normal hangup.
     */
    int HANGUP_REASON_NORMAL_CLEARING = 200;

    /**
     * Reason code used to hangup peer when we wait for some event
     * and it timeouted.
     */
    int HANGUP_REASON_TIMEOUT = 408;

    /**
     * Reason code used to hangup peer if call was not encrypted.
     */
    int HANGUP_REASON_ENCRYPTION_REQUIRED = 609;

    /**
     * Reason code used to hangup peer when we receive an unexpected 4XX, 5XX
     * or 6XX error
     */
    int HANGUP_REASON_UNEXPECTED_RESPONSE = 456;

    /**
     * Reason code used to hangup peer when we receive an unsupported 2XX or
     * 3XX response
     */
    int HANGUP_REASON_UNSUPPORTED_RESPONSE = 230;

    /**
     * Reason code used to hangup peer, indicates busy here.
     */
    int HANGUP_REASON_BUSY_HERE = 486;

    /**
     * Reason code used to hangup peer, indicates internal error.
     */
    int HANGUP_REASON_INTERNAL_ERROR = 500;

    /**
     * The property prefix for SIP accounts which holds the user's directory
     * number.
     */
    String SIP_DIRECTORY_NUMBER_PROP = "DIRECTORY_NUMBER";

    /**
     * Registers the specified CallListener with this provider so that it could
     * be notified when incoming calls are received. This method is called
     * by the implementation of the PhoneUI service.
     * @param listener the listener to register with this provider.
     *
     */
    void addCallListener(CallListener listener);

    /**
     * Removes the specified listener from the list of call listeners.
     * @param listener the listener to unregister.
     */
    void removeCallListener(CallListener listener);

    /**
     * Creates a new <tt>Call</tt> and invites a specific <tt>CallPeer</tt> to
     * it given by her <tt>String</tt> URI.
     *
     * @param uri the address of the callee who we should invite to a new
     * <tt>Call</tt>
     * @return a newly created <tt>Call</tt>. The specified <tt>callee</tt> is
     * available in the <tt>Call</tt> as a <tt>CallPeer</tt>
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call
     * @throws ParseException if <tt>callee</tt> is not a valid SIP address
     * <tt>String</tt>
     */
    Call createCall(String uri)
        throws OperationFailedException,
               ParseException;

    /**
     * Creates a new <tt>Call</tt> and invites a specific <tt>CallPeer</tt> to
     * it given by her <tt>String</tt> URI.
     *
     * @param uri the address of the callee who we should invite to a new
     * <tt>Call</tt>
     * @param displayName the user-visible name of the callee (optional).
     * @return a newly created <tt>Call</tt>. The specified <tt>callee</tt> is
     * available in the <tt>Call</tt> as a <tt>CallPeer</tt>
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call
     * @throws ParseException if <tt>callee</tt> is not a valid SIP address
     * <tt>String</tt>
     */
    Call createCall(String uri, String displayName)
        throws OperationFailedException,
               ParseException;

    /**
     * Creates a new <tt>Call</tt> and invites a specific <tt>CallPeer</tt>
     * to it given by her <tt>Contact</tt>.
     *
     * @param callee the address of the callee who we should invite to a new
     * call
     * @return a newly created <tt>Call</tt>. The specified <tt>callee</tt> is
     * available in the <tt>Call</tt> as a <tt>CallPeer</tt>
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call
     */
    Call createCall(Contact callee)
        throws OperationFailedException;

    /**
     * Creates a new <tt>Call</tt> and invites a specific <tt>CallPeer</tt> to
     * it given by her <tt>String</tt> URI.
     *
     * @param uri the address of the callee who we should invite to a new
     * <tt>Call</tt>
     * @param conference the <tt>CallConference</tt> in which the newly-created
     * <tt>Call</tt> is to participate
     * @param emergency an object providing context for an emergency call.  This
     * is set to null for non-emergency calls.
     * @return a newly created <tt>Call</tt>. The specified <tt>callee</tt> is
     * available in the <tt>Call</tt> as a <tt>CallPeer</tt>
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call
     * @throws ParseException if <tt>callee</tt> is not a valid SIP address
     * <tt>String</tt>
     */
    Call createCall(String uri,
                    CallConference conference,
                    EmergencyCallContext emergency)
        throws OperationFailedException,
               ParseException;

    /**
     * Creates a new <tt>Call</tt> and invites a specific <tt>CallPeer</tt> to
     * it given by her <tt>String</tt> URI.
     *
     * @param uri the address of the callee who we should invite to a new
     * <tt>Call</tt>
     * @param displayName the user-visible display name of the callee.
     * @param conference the <tt>CallConference</tt> in which the newly-created
     * <tt>Call</tt> is to participate
     * @param emergency an object providing context for an emergency call.  This
     * is set to null for non-emergency calls.
     * @return a newly created <tt>Call</tt>. The specified <tt>callee</tt> is
     * available in the <tt>Call</tt> as a <tt>CallPeer</tt>
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call
     * @throws ParseException if <tt>callee</tt> is not a valid SIP address
     * <tt>String</tt>
     */
    Call createCall(String uri,
                    String displayName,
                    CallConference conference,
                    EmergencyCallContext emergency)
        throws OperationFailedException,
               ParseException;

    /**
     * Creates a new <tt>Call</tt> and invites a specific <tt>CallPeer</tt>
     * to it given by her <tt>Contact</tt>.
     *
     * @param callee the address of the callee who we should invite to a new
     * call
     * @param conference the <tt>CallConference</tt> in which the newly-created
     * <tt>Call</tt> is to participate
     * @param emergency an object providing context for an emergency call.  This
     * is set to null for non-emergency calls.
     * @return a newly created <tt>Call</tt>. The specified <tt>callee</tt> is
     * available in the <tt>Call</tt> as a <tt>CallPeer</tt>
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call
     */
    Call createCall(Contact callee,
                    CallConference conference,
                    EmergencyCallContext emergency)
        throws OperationFailedException;

    /**
     * Indicates a user request to answer an incoming call from the specified
     * CallPeer.
     * @param peer the call peer that we'd like to answer.
     * @throws OperationFailedException with the corresponding code if we
     * encounter an error while performing this operation.
     */
    void answerCallPeer(CallPeer peer)
        throws OperationFailedException;

    /**
     * Puts the specified CallPeer "on hold". In other words incoming
     * media flows are not played and outgoing media flows are either muted or
     * stopped, without actually interrupting the session.
     * @param peer the peer that we'd like to put on hold.
     * @throws OperationFailedException with the corresponding code if we
     * encounter an error while performing this operation.
     */
    void putOnHold(CallPeer peer)
        throws OperationFailedException;

    /**
     * Resumes communication with a call peer previously put on hold. If
     * the specified peer is not "On Hold" at the time putOffHold is
     * called, the method has no effect.
     *
     * @param peer the call peer to put on hold.
     * @throws OperationFailedException with the corresponding code if we
     *             encounter an error while performing this operation
     */
    void putOffHold(CallPeer peer)
        throws OperationFailedException;

    /**
     * Indicates a user request to end a call with the specified call
     * peer.
     * @param peer the peer that we'd like to hang up on.
     * @throws OperationFailedException with the corresponding code if we
     * encounter an error while performing this operation.
     */
    void hangupCallPeer(CallPeer peer)
        throws OperationFailedException;

    /**
     * Ends the call with the specified <tt>peer</tt>.
     *
     * @param peer the peer that we'd like to hang up on.
     * @param reasonCode indicates if the hangup is following to a call failure or
     * simply a disconnect indicate by the reason.
     * @param reason the reason of the hangup. If the hangup is due to a call
     * failure, then this string could indicate the reason of the failure
     *
     * @throws OperationFailedException if we fail to terminate the call.
     */
    void hangupCallPeer(CallPeer peer,
                        int reasonCode,
                        String reason)
        throws OperationFailedException;

    /**
     * Returns an iterator over all currently active calls.
     * @return Iterator
     */
    Iterator<? extends Call> getActiveCalls();

    /**
     * @param  The ID of the call to get.
     * @return The matching call.
     */
    Call getCall(String callID);

    /**
     * Sets the mute state of the <tt>Call</tt>.
     * <p>
     * Muting audio streams sent from the call is implementation specific
     * and one of the possible approaches to it is sending silence.
     * </p>
     *
     * @param call the <tt>Call</tt> who's mute state is set
     * @param mute <tt>true</tt> to mute the call streams being sent to
     *            <tt>peers</tt>; otherwise, <tt>false</tt>
     */
    void setMute(Call call, boolean mute);

    /**
     * Returns the protocol provider that this operation set belongs to.
     *
     * @return a reference to the <tt>ProtocolProviderService</tt> that created
     * this operation set.
     */
    T getProtocolProvider();

    /**
     * Creates a new <tt>Recorder</tt> which is to record the specified
     * <tt>Call</tt> (into a file which is to be specified when starting the
     * returned <tt>Recorder</tt>).
     *
     * @param call the <tt>Call</tt> which is to be recorded by the returned
     * <tt>Recorder</tt> when the latter is started
     * @return a new <tt>Recorder</tt> which is to record the specified
     * <tt>call</tt> (into a file which is to be specified when starting the
     * returned <tt>Recorder</tt>)
     * @throws OperationFailedException if anything goes wrong while creating
     * the new <tt>Recorder</tt> for the specified <tt>call</tt>
     */
    Recorder createRecorder(Call call)
        throws OperationFailedException;

    /**
     * Determine whether this call will require media on the user's PC.
     * Typically true unless this is a CTD call.
     * @return <tt>true</tt> if this call will require media on the user's PC,
     * <tt>false</tt> otherwise.
     */
    boolean requiresLocalMedia();

    /**
     * Whether this operation set passes on location information.
     * @return true if location information is used
     */
    boolean usesLocationInformation();
}
