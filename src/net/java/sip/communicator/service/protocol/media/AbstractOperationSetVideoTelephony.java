/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol.media;

import java.awt.*;
import java.beans.*;
import java.text.*;
import java.util.List;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.util.event.*;

/**
 * Represents a default implementation of <tt>OperationSetVideoTelephony</tt> in
 * order to make it easier for implementers to provide complete solutions while
 * focusing on implementation-specific details.
 *
 * @param <T> the implementation specific telephony operation set class like for
 * example <tt>OperationSetBasicTelephonySipImpl</tt>.
 * @param <U> the implementation specific provider class like for example
 * <tt>ProtocolProviderServiceSipImpl</tt>.
 * @param <V> the <tt>MediaAwareCall</tt> implementation like
 * <tt>CallSipImpl</tt> or <tt>CallJabberImpl</tt>.
 * @param <W> the <tt>MediaAwarePeerCall</tt> implementation like
 * <tt>CallPeerSipImpl</tt> or <tt>CallPeerJabberImpl</tt>.
 *
 * @author Emil Ivov
 * @author Sebastien Vincent
 */
public abstract class AbstractOperationSetVideoTelephony<
                                    T extends OperationSetBasicTelephony<U>,
                                    U extends ProtocolProviderService,
                                    V extends MediaAwareCall<W, T, U>,
                                    W extends MediaAwareCallPeer<V, ?, U> >
    implements OperationSetVideoTelephony
{
    /**
     * The SIP <tt>ProtocolProviderService</tt> implementation which created
     * this instance and for which telephony conferencing services are being
     * provided by this instance.
     */
    protected final U parentProvider;

    /**
     * The telephony-related functionality this extension builds upon.
     */
    protected final T basicTelephony;

    /**
     * The <tt>Logger</tt> used by this class and its instances for logging
     * output.
     */
    private static final Logger logger = Logger.getLogger(
                                      AbstractOperationSetVideoTelephony.class);

    /**
     * Initializes a new <tt>AbstractOperationSetVideoTelephony</tt> instance
     * which builds upon the telephony-related functionality of a specific
     * <tt>OperationSetBasicTelephony</tt> implementation.
     *
     * @param basicTelephony the <tt>OperationSetBasicTelephony</tt>
     * the new extension should build upon
     */
    public AbstractOperationSetVideoTelephony(T basicTelephony)
    {
        this.basicTelephony = basicTelephony;
        this.parentProvider = basicTelephony.getProtocolProvider();
    }

    /**
     * Delegates to the <tt>CallPeerMediaHandler</tt> of the specified
     * <tt>CallPeer</tt> because the video is provided by it. Because other
     * <tt>OperationSetVideoTelephony</tt> implementations may not provide their
     * video through the <tt>CallPeerMediaHandler</tt>, this implementation
     * promotes itself as the provider of the video by replacing the
     * <tt>CallPeerMediaHandler</tt> in the <tt>VideoEvents</tt> it fires.
     *
     * @param peer the <tt>CallPeer</tt> that we will be registering
     * <tt>listener</tt> with.
     * @param listener the <tt>VideoListener</tt> that we'd like to register.
     */
    @SuppressWarnings("unchecked") // work with MediaAware* in media package
    public void addVideoListener(CallPeer peer, VideoListener listener)
    {
        if (listener == null)
            throw new NullPointerException("listener");

        ((W)peer).getMediaHandler().addVideoListener(listener);
    }

    /**
     * Implements
     * {@link OperationSetVideoTelephony#getLocalVisualComponent(CallPeer)}.
     *
     * @param peer the <tt>CallPeer</tt> that we are sending our local video to.
     * @return the <tt>Component</tt> containing the local video.
     */
    @SuppressWarnings("unchecked") // work with MediaAware* in media package
    public Component getLocalVisualComponent(CallPeer peer)
    {
        return ((W)peer).getMediaHandler().getLocalVisualComponent();
    }

    /**
     * Gets the visual/video <tt>Component</tt> available in this telephony for
     * a specific <tt>CallPeer</tt>.
     *
     * @param peer the <tt>CallPeer</tt> whose video is to be retrieved
     * @return the visual/video <tt>Component</tt> available in this telephony
     * for the specified <tt>peer</tt> if any; otherwise, <tt>null</tt>
     */
    @Deprecated
    public Component getVisualComponent(CallPeer peer)
    {
        List<Component> visualComponents = getVisualComponents(peer);

        return visualComponents.isEmpty() ? null : visualComponents.get(0);
    }

    /**
     * Gets the visual/video <tt>Component</tt>s available in this telephony for
     * a specific <tt>CallPeer</tt>.
     *
     * @param peer the <tt>CallPeer</tt> whose videos are to be retrieved
     * @return the visual/video <tt>Component</tt>s available in this telephony
     * for the specified <tt>peer</tt>
     */
    @SuppressWarnings("unchecked") // work with MediaAware* in media package
    public List<Component> getVisualComponents(CallPeer peer)
    {
        return ((W)peer).getMediaHandler().getVisualComponents();
    }

    /**
     * Delegates to the <tt>CallPeerMediaHandler</tt> of the specified
     * <tt>CallPeer</tt> because the video is provided by it.
     *
     * @param peer the <tt>CallPeer</tt> that we'd like to unregister our
     * <tt>VideoListener</tt> from.
     * @param listener the <tt>VideoListener</tt> that we'd like to unregister.
     */
    @SuppressWarnings("unchecked") // work with MediaAware* in media package
    public void removeVideoListener(CallPeer peer, VideoListener listener)
    {
        if (listener != null)
            ((W)peer).getMediaHandler().removeVideoListener(listener);
    }

    /**
     * Implements OperationSetVideoTelephony#setLocalVideoAllowed(Call,
     * boolean). Modifies the local media setup to reflect the requested setting
     * for the streaming of the local video and then re-invites all
     * CallPeers to re-negotiate the modified media setup.
     *
     * @param call    the call where we'd like to allow sending local video.
     * @param allowed <tt>true</tt> if local video transmission is allowed and
     * <tt>false</tt> otherwise.
     *
     * @throws OperationFailedException if video initialization fails.
     */
    public void setLocalVideoAllowed(Call call, boolean allowed)
        throws OperationFailedException
    {
        MediaAwareCall<?, ?, ?> mediaAwareCall = (MediaAwareCall<?, ?, ?>) call;

        mediaAwareCall.setVideoDevice(null);
        mediaAwareCall.setLocalVideoAllowed(allowed, MediaUseCase.CALL);
    }

    /**
     * Determines whether the streaming of local video in a specific
     * <tt>Call</tt> is currently allowed. The setting does not reflect
     * the availability of actual video capture devices, it just expresses the
     * desire of the user to have the local video streamed in the case the
     * system is actually able to do so.
     *
     * @param call the <tt>Call</tt> whose video transmission properties we are
     * interested in.
     *
     * @return <tt>true</tt> if the streaming of local video for the specified
     * <tt>Call</tt> is allowed; otherwise, <tt>false</tt>
     */
    @SuppressWarnings("unchecked") // work with MediaAware* in media package
    public boolean isLocalVideoAllowed(Call call)
    {
        return ((V)call).isLocalVideoAllowed();
    }

    /**
     * Determines whether a specific <tt>Call</tt> is currently streaming the
     * local video (to a remote destination).
     *
     * @param call the <tt>Call</tt> whose video transmission we are interested
     * in.
     *
     * @return <tt>true</tt> if the specified <tt>Call</tt> is currently
     * streaming the local video (to a remote destination); otherwise,
     * <tt>false</tt>
     */
    @SuppressWarnings("unchecked") // work with MediaAware* in media package
    public boolean isLocalVideoStreaming(Call call)
    {
        return ((V)call).isLocalVideoStreaming();
    }

    /**
     * Implements OperationSetVideoTelephony#setRemoteVideoAllowed(Call,
     * boolean). Modifies the local media setup to reflect the requested setting
     * for the streaming of the local video and then re-invites all
     * CallPeers to re-negotiate the modified media setup.
     *
     * If remote video is not supported then this implies that local video
     * transmission is also disabled.
     *
     * @param call    the call where we'd like to allow sending local video.
     * @param allowed <tt>true</tt> if local video transmission is allowed and
     * <tt>false</tt> otherwise.
     *
     * @throws OperationFailedException if video initialization fails.
     */
    public void setRemoteVideoAllowed(Call call, boolean allowed)
        throws OperationFailedException
    {
        MediaAwareCall<?, ?, ?> mediaAwareCall = (MediaAwareCall<?, ?, ?>) call;

        logger.info("Setting remote video allowed to " + allowed + " for call "
                                                            + call.getCallID());

        // If disabling remote video video, then also disable local video.
        if (!allowed)
        {
            mediaAwareCall.setLocalVideoAllowed(allowed, MediaUseCase.CALL);
        }

        mediaAwareCall.setRemoteVideoAllowed(allowed, MediaUseCase.CALL);
    }

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
    @SuppressWarnings("unchecked") // work with MediaAware* in media package
    public void addPropertyChangeListener(
            Call call,
            PropertyChangeListener listener)
    {
        ((V)call).addVideoPropertyChangeListener(listener);
    }

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
    @SuppressWarnings("unchecked") // work with MediaAware* in media package
    public void removePropertyChangeListener(
            Call call,
            PropertyChangeListener listener)
    {
        ((V)call).removeVideoPropertyChangeListener(listener);
    }

    /**
     * Get the <tt>MediaUseCase</tt> of a video telephony operation set.
     *
     * @return <tt>MediaUseCase.CALL</tt>
     */
    public MediaUseCase getMediaUseCase()
    {
        return MediaUseCase.CALL;
    }

    /**
     * Create a new video call and invite the specified CallPeer to it with
     * initial video setting.
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
     * @throws java.text.ParseException if <tt>callee</tt> is not a valid sip address
     * string.
     */
    public Call createVideoCall(String uri, QualityPreset qualityPreferences)
        throws OperationFailedException,
            ParseException
    {
        return createVideoCall(uri);
    }

    /**
     * Create a new video call and invite the specified CallPeer to it with
     * initial video setting.
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
    public Call createVideoCall(
            Contact callee, QualityPreset qualityPreferences)
        throws OperationFailedException
    {
        return createVideoCall(callee);
    }
}
