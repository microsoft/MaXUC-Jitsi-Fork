/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol;

import java.awt.*;
import java.beans.*;
import java.text.*;
import java.util.List;

import org.jitsi.service.neomedia.*;
import org.jitsi.util.event.*;

/**
 * Represents an <tt>OperationSet</tt> giving access to video-specific
 * functionality in telephony such as visual <tt>Component</tt>s displaying
 * video and listening to dynamic availability of such <tt>Component</tt>s.
 *
 * @author Lyubomir Marinov
 * @author Sebastien Vincent
 */
public interface OperationSetVideoTelephony
    extends OperationSet
{
    /**
     * Adds a specific <tt>VideoListener</tt> to this telephony in order to
     * receive notifications when visual/video <tt>Component</tt>s are being
     * added and removed for a specific <tt>CallPeer</tt>.
     *
     * @param peer the <tt>CallPeer</tt> whose video the specified listener
     * is to be notified about
     * @param listener the <tt>VideoListener</tt> to be notified when
     * visual/video <tt>Component</tt>s are being added or removed for
     * <tt>peer</tt>
     */
    void addVideoListener(CallPeer peer, VideoListener listener);

    /**
     * Gets the visual <tt>Component</tt> which depicts the local video
     * being streamed to a specific <tt>CallPeer</tt>.
     *
     * @param peer the <tt>CallPeer</tt> to whom the local video which is to be
     * depicted by the returned visual <tt>Component</tt> is being streamed
     * @return a visual <tt>Component</tt> which depicts the local video being
     * streamed to the specified <tt>CallPeer</tt> if this telephony chooses to
     * carry out the creation synchronously; <tt>null</tt> if this telephony
     * chooses to create the requested visual <tt>Component</tt> asynchronously
     */
    Component getLocalVisualComponent(CallPeer peer)
    ;

    /**
     * Gets the visual/video <tt>Component</tt> available in this telephony for
     * a specific <tt>CallPeer</tt>.
     *
     * @param peer the <tt>CallPeer</tt> whose video is to be retrieved
     * @return the visual/video <tt>Component</tt> available in this telephony
     * for the specified <tt>peer</tt> if any; otherwise, <tt>null</tt>
     */
    @Deprecated
    Component getVisualComponent(CallPeer peer);

    /**
     * Gets the visual/video <tt>Component</tt>s available in this telephony for
     * a specific <tt>CallPeer</tt>.
     *
     * @param peer the <tt>CallPeer</tt> whose videos are to be retrieved
     * @return the visual/video <tt>Component</tt>s available in this telephony
     * for the specified <tt>peer</tt>
     */
    List<Component> getVisualComponents(CallPeer peer);

    /**
     * Removes a specific <tt>VideoListener</tt> from this telephony in
     * order to no longer have it receive notifications when visual/video
     * <tt>Component</tt>s are being added and removed for a specific
     * <tt>CallPeer</tt>.
     *
     * @param peer the <tt>CallPeer</tt> whose video the specified listener is
     * to no longer be notified about
     * @param listener the <tt>VideoListener</tt> to no longer be notified
     * when visual/video <tt>Component</tt>s are being added or removed for
     * <tt>peer</tt>
     */
    void removeVideoListener(CallPeer peer, VideoListener listener);

    /**
     * Sets the indicator which determines whether the streaming of local video
     * in a specific <tt>Call</tt> is allowed. The setting does not reflect
     * the availability of actual video capture devices, it just expresses the
     * desire of the user to have the local video streamed in the case the
     * system is actually able to do so.
     *
     * @param call the <tt>Call</tt> to allow/disallow the streaming of local
     * video for
     * @param allowed <tt>true</tt> to allow the streaming of local video for
     * the specified <tt>Call</tt>; <tt>false</tt> to disallow it
     *
     * @throws OperationFailedException if initializing local video fails.
     */
    void setLocalVideoAllowed(Call call, boolean allowed)
        throws OperationFailedException;

    /**
     * Gets the indicator which determines whether the streaming of local video
     * in a specific <tt>Call</tt> is allowed. The setting does not reflect
     * the availability of actual video capture devices, it just expresses the
     * desire of the user to have the local video streamed in the case the
     * system is actually able to do so.
     *
     * @param call the <tt>Call</tt> to get the indicator of
     * @return <tt>true</tt> if the streaming of local video for the specified
     * <tt>Call</tt> is allowed; otherwise, <tt>false</tt>
     */
    boolean isLocalVideoAllowed(Call call);

    /**
     * The property which indicates whether a specific <tt>Call</tt> is
     * currently streaming the local video (to a remote destination).
     */
    String LOCAL_VIDEO_STREAMING = "LOCAL_VIDEO_STREAMING";

    /**
     * Gets the indicator which determines whether a specific <tt>Call</tt>
     * is currently streaming the local video (to a remote destination).
     *
     * @param call the <tt>Call</tt> to get the indicator of
     * @return <tt>true</tt> if the specified <tt>Call</tt> is currently
     * streaming the local video (to a remote destination); otherwise,
     * <tt>false</tt>
     */
    boolean isLocalVideoStreaming(Call call);

    /**
     * Sets the indicator which determines whether the receiving of remote video
     * in a specific <tt>Call</tt> is allowed. The setting does not reflect
     * the availability of actual video capture devices, it just expresses the
     * desire of the user to receive the remote video stream in the case the
     * system is actually able to do so.
     *
     * @param call the <tt>Call</tt> to allow/disallow the receiving of remote
     * video for
     * @param allowed <tt>true</tt> to allow the receiving of remote video for
     * the specified <tt>Call</tt>; <tt>false</tt> to disallow it
     *
     * @throws OperationFailedException if initializing local video fails.
     */
    void setRemoteVideoAllowed(Call call, boolean allowed)
        throws OperationFailedException;

    /**
     * Adds a specific <tt>PropertyChangeListener</tt> to the list of
     * listeners which get notified when the properties (e.g.
     * {@link #LOCAL_VIDEO_STREAMING}) associated with a specific
     * <tt>Call</tt> change their values.
     *
     * @param call the <tt>Call</tt> to start listening to the changes of
     * the property values of
     * @param listener the <tt>PropertyChangeListener</tt> to be notified
     * when the properties associated with the specified <tt>Call</tt> change
     * their values
     */
    void addPropertyChangeListener(Call call,
                                   PropertyChangeListener listener);

    /**
     * Removes a specific <tt>PropertyChangeListener</tt> from the list of
     * listeners which get notified when the properties (e.g.
     * {@link #LOCAL_VIDEO_STREAMING}) associated with a specific
     * <tt>Call</tt> change their values.
     *
     * @param call the <tt>Call</tt> to stop listening to the changes of the
     * property values of
     * @param listener the <tt>PropertyChangeListener</tt> to no longer be
     * notified when the properties associated with the specified <tt>Call</tt>
     * change their values
     */
    void removePropertyChangeListener(Call call,
                                      PropertyChangeListener listener);

    /**
     * Create a new video call and invite the specified CallPeer to it.
     *
     * @param uri the address of the callee that we should invite to a new
     * call.
     * @return CallPeer the CallPeer that will represented by the
     * specified uri. All following state change events will be delivered
     * through that call peer. The Call that this peer is a member
     * of could be retrieved from the CallParticipant instance with the use
     * of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the video call.
     * @throws ParseException if <tt>callee</tt> is not a valid sip address
     * string.
     */
    Call createVideoCall(String uri)
        throws OperationFailedException, ParseException;

    /**
     * Create a new video call and invite the specified CallPeer to it.
     *
     * @param callee the address of the callee that we should invite to a new
     * call.
     * @return CallPeer the CallPeer that will represented by the
     * specified uri. All following state change events will be delivered
     * through that call peer. The Call that this peer is a member
     * of could be retrieved from the CallParticipant instance with the use
     * of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the video call.
     */
    Call createVideoCall(Contact callee)
        throws OperationFailedException;

    /**
     * Create a new video call and invite the specified CallPeer to it.
     *
     * @param uri the address of the callee that we should invite to a new
     * call.
     * @param qualityPreferences the quality preset we will use establishing
     * the video call, and we will expect from the other side. When establishing
     * call we don't have any indications whether remote part supports quality
     * presets, so this setting can be ignored.
     * @return CallPeer the CallPeer that will represented by the
     * specified uri. All following state change events will be delivered
     * through that call peer. The Call that this peer is a member
     * of could be retrieved from the CallParticipant instance with the use
     * of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the video call.
     * @throws ParseException if <tt>callee</tt> is not a valid sip address
     * string.
     */
    Call createVideoCall(String uri, QualityPreset qualityPreferences)
        throws OperationFailedException, ParseException;

    /**
     * Create a new video call and invite the specified CallPeer to it.
     *
     * @param callee the address of the callee that we should invite to a new
     * call.
     * @param qualityPreferences the quality preset we will use establishing
     * the video call, and we will expect from the other side. When establishing
     * call we don't have any indications whether remote part supports quality
     * presets, so this setting can be ignored.
     * @return CallPeer the CallPeer that will represented by the
     * specified uri. All following state change events will be delivered
     * through that call peer. The Call that this peer is a member
     * of could be retrieved from the CallParticipant instance with the use
     * of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the video call.
     */
    Call createVideoCall(Contact callee,
                         QualityPreset qualityPreferences)
        throws OperationFailedException;

    /**
     * Indicates a user request to answer an incoming call with video enabled
     * from the specified CallPeer.
     * @param peer the call peer that we'd like to answer.
     * @throws OperationFailedException with the corresponding code if we
     * encounter an error while performing this operation.
     */
    void answerVideoCallPeer(CallPeer peer)
        throws OperationFailedException;
}
