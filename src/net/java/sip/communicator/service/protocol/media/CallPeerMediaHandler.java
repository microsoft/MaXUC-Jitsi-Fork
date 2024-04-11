/* Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol.media;

import static net.java.sip.communicator.service.insights.parameters.ProtocolMediaParameterInfo.*;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import net.sf.fmj.media.rtp.RTCPFeedback;
import net.sf.fmj.media.rtp.RTCPReport;
import net.sf.fmj.media.rtp.RTCPSenderReport;
import org.json.simple.JSONObject;

import net.java.sip.communicator.impl.netaddr.NetworkAddressManagerServiceImpl;
import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.analytics.AnalyticsParameter;
import net.java.sip.communicator.service.analytics.AnalyticsParameterComplex;
import net.java.sip.communicator.service.analytics.AnalyticsParameterSimple;
import net.java.sip.communicator.service.analytics.AnalyticsService;
import net.java.sip.communicator.service.insights.InsightsEventHint;
import net.java.sip.communicator.service.protocol.CallPeerState;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationSetVideoTelephony;
import net.java.sip.communicator.service.protocol.event.CallChangeEvent;
import net.java.sip.communicator.service.protocol.event.SoundLevelListener;
import net.java.sip.communicator.util.Logger;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.neomedia.AudioMediaStream;
import org.jitsi.service.neomedia.MediaDirection;
import org.jitsi.service.neomedia.MediaStream;
import org.jitsi.service.neomedia.MediaStreamStats;
import org.jitsi.service.neomedia.MediaStreamTarget;
import org.jitsi.service.neomedia.MediaType;
import org.jitsi.service.neomedia.MediaTypeSrtpControl;
import org.jitsi.service.neomedia.QualityPreset;
import org.jitsi.service.neomedia.RTPExtension;
import org.jitsi.service.neomedia.SrtpControl;
import org.jitsi.service.neomedia.SrtpControlType;
import org.jitsi.service.neomedia.StreamConnector;
import org.jitsi.service.neomedia.VideoMediaStream;
import org.jitsi.service.neomedia.codec.Constants;
import org.jitsi.service.neomedia.codec.EncodingConfiguration;
import org.jitsi.service.neomedia.control.KeyFrameControl.KeyFrameRequester;
import org.jitsi.service.neomedia.device.MediaDevice;
import org.jitsi.service.neomedia.event.CsrcAudioLevelListener;
import org.jitsi.service.neomedia.event.SimpleAudioLevelListener;
import org.jitsi.service.neomedia.event.SrtpListener;
import org.jitsi.service.neomedia.format.MediaFormat;
import org.jitsi.service.neomedia.rtp.RTCPExtendedReport.VoIPMetricsReportBlock;
import org.jitsi.util.Hasher;
import org.jitsi.util.OSUtils;
import org.jitsi.util.event.PropertyChangeNotifier;
import org.jitsi.util.event.VideoEvent;
import org.jitsi.util.event.VideoListener;
import org.jitsi.util.event.VideoNotifierSupport;

/**
 * A utility class implementing media control code shared between current
 * telephony implementations. This class is only meant for use by protocol
 * implementations and should not be accessed by bundles that are simply using
 * the telephony functionalities.
 *
 * @param <T> the peer extension class like for example <tt>CallPeerSipImpl</tt>
 * or <tt>CallPeerJabberImpl</tt>
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public abstract class CallPeerMediaHandler<T extends MediaAwareCallPeer<?,?,?>>
    extends PropertyChangeNotifier
{
    private enum ConnectionType {
        WIRED(AnalyticsParameter.VALUE_WIRED, false),
        WIRELESS(AnalyticsParameter.VALUE_WIFI, true),
        UNKNOWN(AnalyticsParameter.VALUE_WIRED, true),
        ;

        private final String analyticsParam;
        private final boolean useWirelessCodecs;

        ConnectionType(String analyticsParam, boolean useWirelessCodecs) {
            this.analyticsParam = analyticsParam;
            this.useWirelessCodecs = useWirelessCodecs;
        }
    }

    private static final Logger logger =
                                   Logger.getLogger(CallPeerMediaHandler.class);

    /**
     * The name of the <tt>CallPeerMediaHandler</tt> property which specifies
     * the local SSRC of its audio <tt>MediaStream</tt>.
     */
    public static final String AUDIO_LOCAL_SSRC = "AUDIO_LOCAL_SSRC";

    /**
     * The name of the <tt>CallPeerMediaHandler</tt> property which specifies
     * the remote SSRC of its audio <tt>MediaStream</tt>.
     */
    public static final String AUDIO_REMOTE_SSRC = "AUDIO_REMOTE_SSRC";

    /**
     * The name of the property indicating the interval in ms. between
     * printing stats to logs.
     */
    protected static final String PROPERTY_NAME_STATS_INTERVAL
        = "net.java.sip.communicator.impl.neomedia.STATS_INTERVAL";

    /**
     * The prefix that is used to store configuration for encodings preference
     * for WIRELESS devices.
     */
    private static final String ENCODING_CONFIG_PROP_WIRELESS_PREFIX
        = "net.java.sip.communicator.impl.neomedia.codec.EncodingConfiguration.WIRELESS";

    /**
     * The prefix that is used to store configuration for encodings preference
     * for WIRED devices.
     */
    public static final String ENCODING_CONFIG_PROP_WIRED_PREFIX
        = "net.java.sip.communicator.impl.neomedia.codec.EncodingConfiguration.WIRED";

    /**
     * The constant which signals that a SSRC value is unknown.
     */
    public static final long SSRC_UNKNOWN = -1;

    /**
     * The name of the <tt>CallPeerMediaHandler</tt> property which specifies
     * the local SSRC of its video <tt>MediaStream</tt>.
     */
    public static final String VIDEO_LOCAL_SSRC = "VIDEO_LOCAL_SSRC";

    /**
     * The name of the <tt>CallPeerMediaHandler</tt> property which specifies
     * the remote SSRC of its video <tt>MediaStream</tt>.
     */
    public static final String VIDEO_REMOTE_SSRC = "VIDEO_REMOTE_SSRC";

    /**
     * List of advertised encryption methods. Indicated before establishing the
     * call.
     */
    private List<SrtpControlType> advertisedEncryptionMethods =
            new ArrayList<>();

    /**
     * Determines whether or not streaming local audio is currently enabled.
     */
    private MediaDirection audioDirectionUserPreference
        = MediaDirection.SENDRECV;

    /**
     * The <tt>AudioMediaStream</tt> which this instance uses to send and
     * receive audio.
     */
    private AudioMediaStream audioStream;

    /**
     * The <tt>PropertyChangeListener</tt> which listens to changes in the
     * values of the properties of the <tt>Call</tt> of {@link #peer}.
     */
    private final CallPropertyChangeListener callPropertyChangeListener;

    /**
     * The listener that our <tt>CallPeer</tt> registers for CSRC audio level
     * events.
     */
    private CsrcAudioLevelListener csrcAudioLevelListener;

    /**
     * The object that we are using to sync operations on
     * <tt>csrcAudioLevelListener</tt>.
     */
    private final Object csrcAudioLevelListenerLock = new Object();

    /**
     * Contains all dynamic payload type mappings that have been made for this
     * call.
     */
    private final DynamicPayloadTypeRegistry dynamicPayloadTypes
        = new DynamicPayloadTypeRegistry();

    /**
     * The <tt>KeyFrameRequester</tt> implemented by this
     * <tt>CallPeerMediaHandler</tt>.
     */
    private final KeyFrameRequester keyFrameRequester
        = new KeyFrameRequester()
        {
            public boolean requestKeyFrame()
            {
                return CallPeerMediaHandler.this.requestKeyFrame();
            }
        };

    /**
     * Determines whether we have placed the call on hold locally.
     */
    private boolean locallyOnHold = false;

    /**
     * Indicates that the start method has been called
     */
    private boolean startCalled = false;

    /**
     * The listener that the <tt>CallPeer</tt> registered for local user audio
     * level events.
     */
    private SimpleAudioLevelListener localUserAudioLevelListener;

    /**
     * The object that we are using to sync operations on
     * <tt>localAudioLevelListener</tt>.
     */
    private final Object localUserAudioLevelListenerLock = new Object();

    /**
     * The state of this instance which may be shared with multiple other
     * <tt>CallPeerMediaHandler</tt>s.
     */
    private MediaHandler mediaHandler;

    /**
     * The <tt>PropertyChangeListener</tt> which listens to changes in the
     * values of the properties of the <tt>MediaStream</tt>s of this instance.
     * Since <tt>CallPeerMediaHandler</tt> wraps around/shares a
     * <tt>MediaHandler</tt>, <tt>mediaHandlerPropertyChangeListener</tt>
     * actually listens to <tt>PropertyChangeEvent</tt>s fired by the
     * <tt>MediaHandler</tt> in question and forwards them as its own.
     */
    private final PropertyChangeListener mediaHandlerPropertyChangeListener
        = new PropertyChangeListener()
        {
            /**
             * Notifies this <tt>PropertyChangeListener</tt> that the value of
             * a specific property of the notifier it is registered with has
             * changed.
             *
             * @param ev a <tt>PropertyChangeEvent</tt> which describes the
             * source of the event, the name of the property which has changed
             * its value and the old and new values of the property
             * @see PropertyChangeListener#propertyChange(PropertyChangeEvent)
             */
            public void propertyChange(PropertyChangeEvent ev)
            {
                mediaHandlerPropertyChange(ev);
            }
        };

    /**
     * A reference to the CallPeer instance that this handler is managing media
     * streams for.
     */
    private final T peer;

    /**
     * Contains all RTP extension mappings (those made through the extmap
     * attribute) that have been bound during this call.
     */
    private final DynamicRTPExtensionsRegistry rtpExtensionsRegistry
        = new DynamicRTPExtensionsRegistry();

    /**
     * The <tt>SrtpListener</tt> which is responsible for the SRTP control. Most
     * often than not, it is the <tt>peer</tt> itself.
     */
    private final SrtpListener srtpListener;

    /**
     * The listener that our <tt>CallPeer</tt> registered for stream audio
     * level events.
     */
    private SimpleAudioLevelListener streamAudioLevelListener;

    /**
     * The object that we are using to sync operations on
     * <tt>streamAudioLevelListener</tt>.
     */
    private final Object streamAudioLevelListenerLock = new Object();

    /**
     * Determines whether or not streaming local video is currently enabled.
     * Default is RECVONLY. We tried to have INACTIVE at one point but it was
     * breaking incoming reINVITEs for video calls..
     */
    private MediaDirection videoDirectionUserPreference
        = MediaDirection.RECVONLY;

    /**
     * The aid which implements the boilerplate related to adding and removing
     * <tt>VideoListener</tt>s and firing <tt>VideoEvent</tt>s to them on behalf
     * of this instance.
     */
    private final VideoNotifierSupport videoNotifierSupport
        = new VideoNotifierSupport(this, true);

    /**
     * The <tt>VideoMediaStream</tt> which this instance uses to send and
     * receive video.
     */
    private VideoMediaStream videoStream;

    /**
     * The <tt>VideoListener</tt> which listens to the video
     * <tt>MediaStream</tt> of this instance for changes in the availability of
     * visual <tt>Component</tt>s displaying remote video and re-fires them as
     * originating from this instance.
     */
    private final VideoListener videoStreamVideoListener
        = new VideoListener()
        {
            /**
             * Notifies this <tt>VideoListener</tt> about a specific
             * <tt>VideoEvent</tt>. Fires a new <tt>VideoEvent</tt> which has
             * this <tt>CallPeerMediaHandler</tt> as its source and carries the
             * same information as the specified <tt>ev</tt> i.e. translates the
             * specified <tt>ev</tt> into a <tt>VideoEvent</tt> fired by this
             * <tt>CallPeerMediaHandler</tt>.
             *
             * @param ev the <tt>VideoEvent</tt> to notify this
             * <tt>VideoListener</tt> about
             */
            private void onVideoEvent(VideoEvent ev)
            {
                VideoEvent clone = ev.clone(CallPeerMediaHandler.this);

                fireVideoEvent(clone);
                if (clone.isConsumed())
                    ev.consume();
            }

            public void videoAdded(VideoEvent ev)
            {
                onVideoEvent(ev);
            }

            public void videoRemoved(VideoEvent ev)
            {
                onVideoEvent(ev);
            }

            public void videoUpdate(VideoEvent ev)
            {
                onVideoEvent(ev);
            }
        };

    /**
     * Media energy listener for received media which is reset every time the
     * stats are printed.
     */
    private MediaEnergyListener receivedResettableMediaListener;

    /**
     * Media energy listener for sent media which is reset every time the
     * stats are printed.
     */
    private MediaEnergyListener sentResettableMediaListener;

    /**
     * Media energy listener for received media
     */
    private MediaEnergyListener receivedMediaListener;

    /**
     * Media energy listener for sent media
     */
    private MediaEnergyListener sentMediaListener;

    /**
     * Set to true when we have started to stream media. False before that and
     * afterwards when we have stopped streaming
     */
    private boolean started;

    /**
     * Whether this media stream is set up on a WiFi connection.  This is used
     * to determine which codec(s) to use for the media and raising analytics.
     * !!!NOTE!!! this isn't 100% accurate, but is accurate enough for choosing codecs and raising analytics.
     * <p>
     * (Ways in which it can be inaccurate - we can hit an exception while
     * calculating it, or timeout waiting for localhost info [on Mac], or possibly
     * miscategorise network interfaces)
     */
    private ConnectionType mProbableConnectionType = ConnectionType.UNKNOWN;

    /**
     * If using Wireless, the SSID of the connection, if known.
     */
    private String mWirelessSSID = "";

    /**
     * The time that start() was first called on this media handler.
     */
    private long audioStartTime;

    /**
     * The (SIP) call ID.
     */
    private String mSIPCallID = "";

    /**
     * The call direction ("in" or "out")
     */
    private String mCallDirection = AnalyticsParameter.VALUE_INBOUND;

    /**
     * The start time as perceived by the user.
     */
    private long mUserDefinedCallStartTime = 0;

    /**
     * Whether we have sent a cut through analytics event.
     */
    private boolean mSentMediaCutThrough = false;

    /**
     * The threshold at which we think there is significant network loss.
     * This is a fraction of 256 (so 50%=128).
     */
    private static final int NETWORK_LOSS_ERROR_THRESHOLD = 256 * 30 / 100;

    /**
     * Creates a new handler that will be managing media streams for
     * <tt>peer</tt>.
     *
     * @param peer the <tt>CallPeer</tt> instance that we will be managing
     * media for.
     * @param srtpListener the object that receives SRTP security events.
     */
    public CallPeerMediaHandler(T            peer,
                                SrtpListener srtpListener)
    {
        this.peer = peer;
        this.srtpListener = srtpListener;

        setMediaHandler(new MediaHandler());

        /*
         * Listen to the call of peer in order to track the user's choice with
         * respect to the default audio device.
         */
        MediaAwareCall<?, ?, ?> call = this.peer.getCall();

        if (call == null)
            callPropertyChangeListener = null;
        else
        {
            callPropertyChangeListener = new CallPropertyChangeListener(call);
            call.addPropertyChangeListener(callPropertyChangeListener);
        }

        // We check what type of network we're on at the start of each media
        // stream to determine which codecs we should use for this stream.  We
        // don't do this later on because this processing can take a while on
        // some machines so we don't want to hold up processing of answering a
        // call to do it.
        try
        {
            logger.debug("Performing wired/wireless check");
            InetAddress localAddress =
                getTransportManager().getLastUsedLocalHost();

            // On Mac we fail to reliably calculate (for all network setups) a
            // local IP address to use for connecting to a given remote address
            // - see NetworkAddressManagerServiceImpl.getLocalHost().  So
            // use a dummy local IP address. However, that means that all
            // analytics appear to be from wired calls.  Until we properly fix
            // the logic to calculate the local address on Mac, we use this
            // temporary solution to make a more educated guess at what our
            // local IP address is, for the purpose of raising analytics.
            //
            // However, separately there is an issue where InetAddress#getLocalHost()
            // can take 5s+ to execute, thus blocking call setup. Hence,
            // timeout if this is taking too long and take some guesses:
            // 1) For analytics, guess 'wired' to preserve existing behaviour
            // 2) For codec selection, guess 'wireless' as this list should be
            //    suitable in a wider range of situations then wired codecs.
            if ((OSUtils.IS_MAC) &&
                (localAddress.getHostAddress().equals(NetworkAddressManagerServiceImpl.DUMMY_ADDRESS_STRING)))
            {
                // N.B. returns null if this times out
                localAddress = ProtocolMediaActivator.getNetworkAddressManagerService().getOsLocalHostWithTimeout(1,
                                                                                                                  TimeUnit.SECONDS);
            }

            if (localAddress == null) {
                // We timed out above
                mProbableConnectionType = ConnectionType.UNKNOWN;
            } else if (ProtocolMediaActivator.getNetworkAddressManagerService()
                .isAddressWifi(localAddress)) {
                mProbableConnectionType = ConnectionType.WIRELESS;
            } else {
                mProbableConnectionType = ConnectionType.WIRED;
            }

            if (mProbableConnectionType == ConnectionType.WIRELESS)
            {
                mWirelessSSID =
                    ProtocolMediaActivator.getNetworkAddressManagerService()
                    .getSSID(localAddress);
            }

            logger.info("Connection type = " + mProbableConnectionType + ", using " +
                        (mProbableConnectionType.useWirelessCodecs ? "WIRELESS": "WIRED") + " codecs for interface");
        }
        catch (Exception e)
        {
            logger.error("Failed wired/wireless check, connection type = " + mProbableConnectionType, e);
        }
    }

    /**
     * Adds encryption method to the list of advertised secure methods.
     * @param encryptionMethod the method to add.
     */
    public void addAdvertisedEncryptionMethod(SrtpControlType encryptionMethod)
    {
        if(!advertisedEncryptionMethods.contains(encryptionMethod))
            advertisedEncryptionMethods.add(encryptionMethod);
    }

    /**
     * Registers a specific <tt>VideoListener</tt> with this instance so that it
     * starts receiving notifications from it about changes in the availability
     * of visual <tt>Component</tt>s displaying video.
     *
     * @param listener the <tt>VideoListener</tt> to be registered with this
     * instance and to start receiving notifications from it about changes in
     * the availability of visual <tt>Component</tt>s displaying video
     */
    public void addVideoListener(VideoListener listener)
    {
        videoNotifierSupport.addVideoListener(listener);
    }

    /**
     * Notifies this instance that a value of a specific property of the
     * <tt>Call</tt> of {@link #peer} has changed from a specific old value to a
     * specific new value.
     *
     * @param event a <tt>PropertyChangeEvent</tt> which specified the property
     * which had its value changed and the old and new values of that property
     */
    private void callPropertyChange(PropertyChangeEvent event)
    {
        String propertyName = event.getPropertyName();

        if (MediaAwareCall.CONFERENCE.equals(propertyName)
                || MediaAwareCall.DEFAULT_DEVICE.equals(propertyName))
        {
            MediaAwareCall<?,?,?> call = getPeer().getCall();

            if (call == null)
                return;

            for (MediaType mediaType : MediaType.values())
            {
                MediaStream stream = getStream(mediaType);

                if (stream == null)
                    continue;

                MediaDevice oldDevice = stream.getDevice();

                if (oldDevice != null)
                {
                    MediaDevice newDevice = getDefaultDevice(mediaType);

                    if (oldDevice != newDevice)
                        stream.setDevice(newDevice);
                }

                stream.setRTPTranslator(call.getRTPTranslator(mediaType));
            }
        }
    }

    /**
     * Just logs the failure to SAS and analytics.  Caller is expected to additionally
     * call close().
     */
    public synchronized void callFailed(String reason, int reasonCode)
    {
        AnalyticsService analytics = ProtocolMediaActivator.getAnalyticsService();

        // This parameter list may contain PII as it will be sent to the Service Provider's
        // SAS (via CP).
        ArrayList<AnalyticsParameter> paramsForCP = new ArrayList<>();
        paramsForCP.add(new AnalyticsParameterSimple(AnalyticsParameter.NAME_FAILED_ERR,
                                                AnalyticsParameter.VALUE_FAILED_SIP));
        paramsForCP.add(new AnalyticsParameterSimple(AnalyticsParameter.NAME_DIRECTION,
                                                AnalyticsParameter.VALUE_OUTBOUND));
        paramsForCP.add(new AnalyticsParameterSimple(AnalyticsParameter.NAME_FAILED_SIP,
                                                "" + reasonCode));

        @SuppressWarnings("unchecked")
        // This list MUST NOT contain PII as it is sent to Msw.
        ArrayList<AnalyticsParameter> paramsForMSW = (ArrayList<AnalyticsParameter>)paramsForCP.clone();

        // Add the network info.
        AnalyticsParameterComplex networkAnalytic = new AnalyticsParameterComplex(AnalyticsParameter.NAME_NETWORK_INFO,
                                                                                  getNetworkParams());
        paramsForCP.add(networkAnalytic);
        paramsForMSW.add(networkAnalytic);

        // And send the event.
        ConfigurationService configService = ProtocolMediaActivator.getConfigurationService();
        String userName = "net.java.sip.communicator.plugin.provisioning.auth.USERNAME";
        String dn = configService.global().getString(userName, "Unknown");

        analytics.onEvent(AnalyticsEventType.CALL_FAILED,
                          paramsForCP,
                          paramsForMSW,
                          new int[]{AnalyticsParameter.SAS_MARKER_SUBSCRIBER_NUM},
                          new String[]{dn});

        sendTelemetryOutgoingCallFailed(reasonCode);
    }

    private void sendTelemetryOutgoingCallFailed(int reasonCode)
    {
        if (peer == null)
        {
            return;
        }

        String networkType = mProbableConnectionType.analyticsParam;
        boolean isConnected = peer.getState().equals(CallPeerState.CONNECTED);

        ProtocolMediaActivator.getInsightsService().logEvent(InsightsEventHint.COMMON_OUTGOING_CALL_FAILED.name(),
                                                             Map.of(NETWORK_TYPE.name(), networkType,
                                                                    CALL_FAILED_REASON_CODE.name(), reasonCode,
                                                                    CONNECTED.name(), isConnected));
    }

    /**
     * Closes and null-ifies all streams and connectors and readies this media
     * handler for garbage collection (or reuse). Synchronized if any other
     * stream operations are in process we won't interrupt them.
     */
    public synchronized void close()
    {
        logger.debug("Closing CallPeerMediaHandler: " + this.hashCode());

        sendMediaCutThroughAnalytics();

        sendEndOfCallAnalytics();

        closeStream(MediaType.AUDIO);
        closeStream(MediaType.VIDEO);

        locallyOnHold = false;
        started = false;

        if (callPropertyChangeListener != null)
            callPropertyChangeListener.removePropertyChangeListener();

        if (peer != null)
        {
            peer.removeStreamSoundLevelListener(receivedMediaListener);
            peer.removeStreamSoundLevelListener(receivedResettableMediaListener);

            if (peer.getCall() != null)
            {
                peer.getCall().removeLocalUserSoundLevelListener(
                    sentMediaListener);
                peer.getCall().removeLocalUserSoundLevelListener(
                    sentResettableMediaListener);
            }
        }

        setMediaHandler(null);
    }

    /**
     * Send an analytics event for the end of the call.
     */
    public void sendMediaCutThroughAnalytics()
    {
        logger.debug("Sending cut thru analytics");

        // Don't send if we have already sent.  No need for
        // synchronization here, it's a very small window, which will
        // never be hit, and even if we ever did, no big deal.
        if (mSentMediaCutThrough)
        {
            logger.debug("Already sent cut through analytics");
            return;
        }

        AnalyticsService analytics = ProtocolMediaActivator.getAnalyticsService();
        long startOfCall = mUserDefinedCallStartTime;

        logger.debug("Start of call:" + startOfCall);

        if ((audioStream == null) || (startOfCall == 0))
            // No call - nothing to do.
            return;

        MediaStreamStats audioStats = audioStream.getMediaStreamStats();

        // Find the TX and RX SSRCs.
        long txSSRC = audioStream.getLocalSourceID();
        long rxSSRC = audioStream.getRemoteSourceID();

        long firstSentPacketTime = audioStats.getFirstSentPacketTime(txSSRC);
        long firstReceivedPacketTime = audioStats.getFirstReceivedPacketTime(rxSSRC);
        logger.debug("Sent:" + "(" + txSSRC + ")" + firstSentPacketTime);
        logger.debug("Received:" + "(" + rxSSRC + ")" + firstReceivedPacketTime);

        if ((firstReceivedPacketTime == 0) || (firstSentPacketTime == 0))
            // Call not set up yet
            return;

        List<AnalyticsParameter> params = new ArrayList<>();
        params.add(new AnalyticsParameterSimple(AnalyticsParameter.NAME_TIME_TO_FIRST_SENT_PACKET,
                                                "" + (firstSentPacketTime - startOfCall)));
        logger.debug("Sent delta=" + (firstSentPacketTime - startOfCall));

        params.add(new AnalyticsParameterSimple(AnalyticsParameter.NAME_TIME_TO_FIRST_REC_PACKET,
                                                "" + (firstReceivedPacketTime - startOfCall)));
        logger.debug("Rec delta=" + (firstReceivedPacketTime - startOfCall));

        params.add(new AnalyticsParameterSimple(AnalyticsParameter.NAME_DIRECTION,
                                                mCallDirection));

        // And send the event.
        mSentMediaCutThrough = true;
        analytics.onEvent(AnalyticsEventType.CUT_THRU,
                          params);
    }

    /*
     * Send the marker to SAS.  That'll get us a trail ID to use at the end of the call.
     */
    private void sendAnalyticsMarker()
    {
        // And send the event.
        ConfigurationService configService = ProtocolMediaActivator.getConfigurationService();
        String userName =
            "net.java.sip.communicator.plugin.provisioning.auth.USERNAME";
        String dn = configService.global().getString(userName, "Unknown");

        AnalyticsService analytics = ProtocolMediaActivator.getAnalyticsService();
        analytics.onEvent(AnalyticsEventType.SAS_MARKER_ONLY,
                          null,
                          null,
                          new int[]{AnalyticsParameter.SAS_MARKER_SIP_CALL_ID,
                                    AnalyticsParameter.SAS_MARKER_CALLING_DN},
                          new String[]{mSIPCallID,
                                       dn});
    }

    /**
     * Send an analytics event for the end of the call.
     */
    private void sendEndOfCallAnalytics()
    {
        logger.debug("Sending end of call analytics start");
        AnalyticsService analytics = ProtocolMediaActivator.getAnalyticsService();

        if (audioStream == null)
            // No call stream - nothing to do.
            return;

        // Get the audio stats and basic call stats
        MediaStreamStats audioStats = audioStream.getMediaStreamStats();
        audioStats.updateStats();

        //
        //  We want this event to end up looking like this JSON.  We are going
        //  to achieve that by using the nested AnalyticsParameterComplex
        //  structure.
        //
        //  parameters:{
        //    l:14344
        //    d:out
        //    cdc:{
        //      n:SILK
        //      khz:8
        //    }
        //    nwi:{
        //      nw:wired
        //    }
        //    nwr:{
        //      rr:{
        //        ssrc:4452345BC
        //        l:3
        //        j:21
        //      }
        //      xr:{
        //        l:0
        //        d:43
        //        bdn:0
        //        bdr:0
        //        gdn:1
        //        gdr:8860
        //        esd:997
        //        rtt:1574
        //        jb:{
        //          nm:180
        //          mx:260
        //          am:260
        //        }
        //        mcq:9
        //      }
        //    }
        //    nwt:{
        //      // Repeat of mwr, with different values
        //    }
        //    dropped:1
        //  }
        //}
        // This parameter list may contain PII as it will be sent to the Service Provider's
        // SAS (via CP).
        ArrayList<AnalyticsParameter> paramsForCP = new ArrayList<>();

        //
        //  First the flat "easy" bit.
        //    l:14344
        //    d:out
        //
        // Call ID not added to prevent correlation of events with data from SAS
        // allowing determination of the user and leaking PII.

        // It's possible for the audioStart time to be 0, in which case
        // we call the call length 0.
        long callLength = 0;
        if (audioStartTime > 0)
          callLength = System.currentTimeMillis() - audioStartTime;

        paramsForCP.add(new AnalyticsParameterSimple(AnalyticsParameter.NAME_CALL_LENGTH,
                                                "" + callLength));

        String direction = mCallDirection;
        paramsForCP.add(new AnalyticsParameterSimple(AnalyticsParameter.NAME_DIRECTION,
                                                direction));

        //
        // Now the codec bit
        //    cdc:{
        //      n:SILK
        //      khz:8
        //    }
        String codecName = audioStats.getEncoding();
        String codecHz = audioStats.getEncodingClockRate();

        // Sadly, we have the codec freq in Hz, and need it in KHz, so need to do
        // some parsing, etc.
        int codecKHz = 0 ;
        try
        {
            codecKHz = Integer.valueOf(codecHz) / 1000;
        }
        catch(NumberFormatException e)
        {
            logger.error("Unexpected codec clock rate: " + codecHz);
        }
        AnalyticsParameter[] codecParams =  new AnalyticsParameter[2];
        codecParams[0]= new AnalyticsParameterSimple(AnalyticsParameter.NAME_CODEC_NAME,
                                                     codecName);
        codecParams[1]= new AnalyticsParameterSimple(AnalyticsParameter.NAME_CODEC_FREQ,
                                                     "" + codecKHz);
        paramsForCP.add(new AnalyticsParameterComplex(AnalyticsParameter.NAME_CODEC_STRUCT,
                                                 codecParams));

        // Find the TX and RX SSRCs.
        long txSSRC = audioStream.getLocalSourceID();
        long rxSSRC = audioStream.getRemoteSourceID();

        // TX and RX reports are virtually the same, so call out to private method
        // to handle them.
        boolean isBye1 = addNetworkStatsToCallEnded(AnalyticsParameter.NAME_NETWORK_TX,
                                                    paramsForCP,
                                                    txSSRC);
        boolean isBye2 = addNetworkStatsToCallEnded(AnalyticsParameter.NAME_NETWORK_RX,
                                                    paramsForCP,
                                                    rxSSRC);
        logger.debug("Bye1=" + isBye1 + ": Bye2=" + isBye2);

        // The "dropped" part, if the call was dropped.
        // If the call end was clean then both ends should have supplied a BYE.
        //
        // I can't get this to work reliably, so forget the dropped parameter.
        //boolean dropped = !(isBye1 && isBye2);
        //if (dropped)
        //{
        //    params.add(new AnalyticsParameterSimple(AnalyticsParameter.NAME_DROPPED,
        //               AnalyticsParameter.VALUE_DROPPED_YES));
        //}

        //
        // Now the network bit
        //    nwi:{
        //      nw:wired
        //    }
        //
        @SuppressWarnings("unchecked")
        // This list MUST NOT contain PII as it is sent to Msw.
        ArrayList<AnalyticsParameter> paramsForMSW = (ArrayList<AnalyticsParameter>)paramsForCP.clone();

        // Add the network info.
        AnalyticsParameterComplex networkAnalytic = new AnalyticsParameterComplex(AnalyticsParameter.NAME_NETWORK_INFO,
                                                                                  getNetworkParams());
        paramsForCP.add(networkAnalytic);
        paramsForMSW.add(networkAnalytic);

        logger.debug("Sending end of call analytics send");

        // And send the event.
        ConfigurationService configService = ProtocolMediaActivator.getConfigurationService();
        String userName =
            "net.java.sip.communicator.plugin.provisioning.auth.USERNAME";
        String dn = configService.global().getString(userName, "Unknown");

        analytics.onEvent(AnalyticsEventType.CALL_ENDED,
                          paramsForCP,
                          paramsForMSW,
                          new int[]{AnalyticsParameter.SAS_MARKER_SIP_CALL_ID,
                                    AnalyticsParameter.SAS_MARKER_CALLING_DN},
                          new String[]{mSIPCallID,
                                       dn});

        JSONObject paramsJSON = new JSONObject();
        for (AnalyticsParameter param : paramsForCP)
        {
            param.addToJSON(paramsJSON, 0);
        }

        ProtocolMediaActivator.getInsightsService().logEvent(InsightsEventHint.PROTOCOL_MEDIA_CALL_ENDED.name(),
                                                             Map.of(NETWORK_AND_CALL_STATS_JSON.name(), paramsJSON));
        logger.debug("Sending end of call analytics end");
    }

    /*
     * Generates the network parameters part of an analytics event.
     */
    private List<AnalyticsParameter> getNetworkParams()
    {
        //
        // Now the network bit
        //    nwi:{
        //          nw:wired
        //    }
        List<AnalyticsParameter> networkParams = new ArrayList<>();
        String network = mProbableConnectionType.analyticsParam;

        networkParams.add(new AnalyticsParameterSimple(AnalyticsParameter.NAME_NETWORK_TYPE,
                                                       network));
        if (!mWirelessSSID.equals(""))
        {
            // Network SSID counts as Personal Data, but it may be useful to spot multiple issues on the
            // same network, so hash rather than remove it.
            String ssidToUse = Hasher.logHasher(mWirelessSSID);

            networkParams.add(new AnalyticsParameterSimple(AnalyticsParameter.NAME_NETWORK_SSID,
                              ssidToUse));
        }

        return networkParams;
    }

    /**
     * @param statName - Stat to add to the Call Ended event
     *                   Must be NWR or NWT
     * @param params - Params to add to
     *
     * @return Whether this was a "bye" report
     */
    private boolean addNetworkStatsToCallEnded(String statName,
                                               List<AnalyticsParameter> params,
                                               long ssrc)
    {
        // Get hold of the RR and XR reports.
        boolean isBye = false;
        MediaStreamStats audioStats = audioStream.getMediaStreamStats();
        RTCPReport rtcpRR = null;
        VoIPMetricsReportBlock voipBlock = null;
        RTCPFeedback feedback = null;

        // OK, pay attention, this is tricky.
        //
        //     TX:
        //     ------------ SSRC 100 --------->
        // A                                     B
        //     RX:
        //     <----------- SSRC 200 ----------
        //
        // We want to group the stats by SSRC (TX/RX)
        //
        // A Sends:
        //    - An RR report (labelled 100) that includes:
        //       -- Sent data for SSRC 100
        //       -- A feedback on SSRC 200
        //    - An XR on SSRC 200
        //
        // A Receives:
        //    - An RR report (labelled 200) that includes
        //       -- Sent data for SSRC 200
        //       -- A feedback on SSRC 100
        //    - An XR on SSRC 100
        //
        logger.debug("Looking at " + statName);
        if (statName.equals(AnalyticsParameter.NAME_NETWORK_TX))
        {
            // TX = SSRC 100
            logger.debug("Getting TX RR");
            rtcpRR = audioStats.getSentRTCPRR(ssrc);
            voipBlock = audioStats.getReceivedRTCPVoIPMetrics(ssrc);
            feedback = audioStats.getReceivedFeedback(ssrc);
        }
        else
        {
            // RX = SSRC 200
            logger.debug("Getting RX RR");

            rtcpRR = audioStats.getReceivedRTCPRR(ssrc);
            voipBlock = audioStats.getSentRTCPVoIPMetrics(ssrc);
            feedback = audioStats.getSentFeedback(ssrc);
        }

        if ((voipBlock == null) || (feedback == null))
        {
            logger.debug("Unexpectedly could not get data for " + statName +
                         ": voipBlock=" + voipBlock +
                         ", feedback=" + feedback);
        }

        // Did we close down cleanly?
        if (rtcpRR != null)
        {
            isBye = rtcpRR.isByePacket();
        }

        // Now construct the RR part
        //      rr:{
        //        ssrc:4452345BC
        //        l:3
        //        j:21
        //        bs:223424
        //        lst:2342
        //      }
        List<AnalyticsParameter> rrParams = new ArrayList<>();

        if (rtcpRR != null)
        {
            rrParams.add(new AnalyticsParameterSimple(AnalyticsParameter.NAME_RR_SSRC,
                                                      Long.toString(ssrc & 0xffffffffL, 16)));

            if (feedback != null)
            {
                long numLost = feedback.getNumLost();
                long jitter = feedback.getJitter();

                rrParams.add(new AnalyticsParameterSimple(AnalyticsParameter.NAME_RR_PACKETS_LOST,
                                                          "" + numLost));
                rrParams.add(new AnalyticsParameterSimple(AnalyticsParameter.NAME_RR_JITTER,
                                                          "" + jitter));
            }

            if (rtcpRR instanceof RTCPSenderReport)
            {
                RTCPSenderReport senderReport = (RTCPSenderReport) rtcpRR;
                long bytes = senderReport.getSenderByteCount();
                long packets = senderReport.getSenderPacketCount();

                rrParams.add(new AnalyticsParameterSimple(AnalyticsParameter.NAME_RR_BYTES_SENT,
                    "" + bytes));
                rrParams.add(new AnalyticsParameterSimple(AnalyticsParameter.NAME_RR_PACKETS_SENT,
                    "" + packets));
            }
        }

        // And now the XR part
        //      xr:{
        //        l:0
        //        d:43
        //        bdn:0
        //        bdr:0
        //        gdn:1
        //        gdr:8860
        //        esd:997
        //        rtt:1574
        //        jb:{
        //          nm:180
        //          mx:260
        //          am:260
        //        }
        //        mcq:9
        //      }
        List<AnalyticsParameter> xrParams = new ArrayList<>();

        if (voipBlock != null)
        {
            short lossRate = voipBlock.getLossRate();
            short discardRate = voipBlock.getDiscardRate();
            short burstDensity = voipBlock.getBurstDensity();
            int burstDuration = voipBlock.getBurstDuration();
            short gapDensity = voipBlock.getGapDensity();
            int gapDuration = voipBlock.getGapDuration();
            int endSystemDelay = voipBlock.getEndSystemDelay();
            int roundTripDelay = voipBlock.getRoundTripDelay();

            int bufferMaxDelay = voipBlock.getJitterBufferMaximumDelay();
            int bufferNominalDelay = voipBlock.getJitterBufferNominalDelay();
            int bufferAbsoluteMaximumDelay = voipBlock.getJitterBufferAbsoluteMaximumDelay();

            byte mosCq = voipBlock.getMosCq();

            xrParams.add(new AnalyticsParameterSimple(AnalyticsParameter.NAME_XR_LOSS_RATE,
                "" + lossRate));
            xrParams.add(new AnalyticsParameterSimple(AnalyticsParameter.NAME_XR_DISCARD_RATE,
                "" + discardRate));
            xrParams.add(new AnalyticsParameterSimple(AnalyticsParameter.NAME_XR_BURST_DENSITY,
                "" + burstDensity));
            xrParams.add(new AnalyticsParameterSimple(AnalyticsParameter.NAME_XR_BURST_DURATION,
                "" + burstDuration));
            xrParams.add(new AnalyticsParameterSimple(AnalyticsParameter.NAME_XR_GAP_DENSITY,
                "" + gapDensity));
            xrParams.add(new AnalyticsParameterSimple(AnalyticsParameter.NAME_XR_GAP_DURATION,
               "" + gapDuration));
            xrParams.add(new AnalyticsParameterSimple(AnalyticsParameter.NAME_XR_END_SYSTEM_DELAY,
                "" + endSystemDelay));
            xrParams.add(new AnalyticsParameterSimple(AnalyticsParameter.NAME_XR_ROUND_TRIP,
                "" + roundTripDelay));

            AnalyticsParameter[] jitterParams = new AnalyticsParameter[3];
            jitterParams[0] =
                new AnalyticsParameterSimple(AnalyticsParameter.NAME_JITTER_NOMINAL,
                                             "" + bufferNominalDelay);
            jitterParams[1] =
                new AnalyticsParameterSimple(AnalyticsParameter.NAME_JITTER_MAX_SIZE,
                                             "" + bufferMaxDelay);
            jitterParams[2] =
                new AnalyticsParameterSimple(AnalyticsParameter.NAME_JITTER_ABS_MAX_SIZE,
                                             "" + bufferAbsoluteMaximumDelay);

            xrParams.add(new AnalyticsParameterComplex(AnalyticsParameter.NAME_JITTER_BUFFER,
                                                       jitterParams));

            xrParams.add(new AnalyticsParameterSimple(AnalyticsParameter.NAME_XR_MCQ,
                "" + mosCq));
        }

        // And put the RR and XR parts together in the network params.
        List<AnalyticsParameter> networkParams = new ArrayList<>();
        networkParams.add(
            new AnalyticsParameterComplex(AnalyticsParameter.NAME_RTCP_RR,
                                          rrParams));
        networkParams.add(
            new AnalyticsParameterComplex(AnalyticsParameter.NAME_RTCP_XR,
                                          xrParams));

        if (statName.equals(AnalyticsParameter.NAME_NETWORK_TX))
        {
            networkParams.add(
                new AnalyticsParameterSimple(AnalyticsParameter.NAME_RTT_SEQ,
                                             "" + audioStats.getRTCPReports().getRTTViaSeq((int)ssrc)));
        }

        params.add(new AnalyticsParameterComplex(statName,
                                                 networkParams));

        return isBye;
    }

    /**
     * Closes the <tt>MediaStream</tt> that this instance uses for a specific
     * <tt>MediaType</tt> and prepares it for garbage collection.
     *
     * @param mediaType the <tt>MediaType</tt> that we'd like to stop a stream
     * for.
     */
    protected void closeStream(MediaType mediaType)
    {
        /*
         * This CallPeerMediaHandler releases its reference to the MediaStream
         * it has initialized via #initStream().
         */
        boolean mediaHandlerCloseStream = false;

        switch (mediaType)
        {
        case AUDIO:
            if (audioStream != null)
            {
                audioStream = null;
                mediaHandlerCloseStream = true;
            }
            break;
        case VIDEO:
            if (videoStream != null)
            {
                videoStream = null;
                mediaHandlerCloseStream = true;
            }
            break;
        }
        if (mediaHandlerCloseStream)
        {
            logger.debug("closeStream " + mediaHandler + " (" + this + ")");
            mediaHandler.closeStream(this, mediaType);
        }

        getTransportManager().closeStreamConnector(mediaType);
    }

    /**
     * Returns the first <tt>RTPExtension</tt> in <tt>extList</tt> that uses
     * the specified <tt>extensionURN</tt> or <tt>null</tt> if <tt>extList</tt>
     * did not contain such an extension.
     *
     * @param extList the <tt>List</tt> that we will be looking through.
     * @param extensionURN the URN of the <tt>RTPExtension</tt> that we are
     * looking for.
     *
     * @return the first <tt>RTPExtension</tt> in <tt>extList</tt> that uses
     * the specified <tt>extensionURN</tt> or <tt>null</tt> if <tt>extList</tt>
     * did not contain such an extension.
     */
    private RTPExtension findExtension(List<RTPExtension> extList,
                                       String extensionURN)
    {
        for(RTPExtension rtpExt : extList)
            if (rtpExt.getURI().toASCIIString().equals(extensionURN))
                return rtpExt;
        return null;
    }

    /**
     * Finds a <tt>MediaFormat</tt> in a specific list of <tt>MediaFormat</tt>s
     * which matches a specific <tt>MediaFormat</tt>.
     *
     * @param formats the list of <tt>MediaFormat</tt>s to find the specified
     * matching <tt>MediaFormat</tt> into
     * @param format encoding of the <tt>MediaFormat</tt> to find
     * @return the <tt>MediaFormat</tt> from <tt>formats</tt> which matches
     * <tt>format</tt> if such a match exists in <tt>formats</tt>; otherwise,
     * <tt>null</tt>
     */
    protected MediaFormat findMediaFormat(
            List<MediaFormat> formats, MediaFormat format)
    {
        for(MediaFormat match : formats)
        {
            if (match.matches(format))
                return match;
        }
        return null;
    }

    /**
     * Notifies the <tt>VideoListener</tt>s registered with this
     * <tt>CallPeerMediaHandler</tt> about a specific type of change in the
     * availability of a specific visual <tt>Component</tt> depicting video.
     *
     * @param type the type of change as defined by <tt>VideoEvent</tt> in the
     * availability of the specified visual <tt>Component</tt> depicting video
     * @param visualComponent the visual <tt>Component</tt> depicting video
     * which has been added or removed in this <tt>CallPeerMediaHandler</tt>
     * @param origin {@link VideoEvent#LOCAL} if the origin of the video is
     * local (e.g. it is being locally captured); {@link VideoEvent#REMOTE} if
     * the origin of the video is remote (e.g. a remote peer is streaming it)
     * @return <tt>true</tt> if this event and, more specifically, the visual
     * <tt>Component</tt> it describes have been consumed and should be
     * considered owned, referenced (which is important because
     * <tt>Component</tt>s belong to a single <tt>Container</tt> at a time);
     * otherwise, <tt>false</tt>
     */
    protected boolean fireVideoEvent(
            int type,
            Component visualComponent,
            int origin)
    {
        return
            videoNotifierSupport.fireVideoEvent(
                    type, visualComponent, origin,
                    true);
    }

    /**
     * Notifies the <tt>VideoListener</tt>s registered with this
     * <tt>CallPeerMediaHandler</tt> about a specific <tt>VideoEvent</tt>.
     *
     * @param event the <tt>VideoEvent</tt> to fire to the
     * <tt>VideoListener</tt>s registered with this
     * <tt>CallPeerMediaHandler</tt>
     */
    public void fireVideoEvent(VideoEvent event)
    {
        videoNotifierSupport.fireVideoEvent(event, true);
    }

    /**
     * Returns the advertised methods for securing the call,
     * this are the methods like SDES, ZRTP that are
     * indicated in the initial session initialization. Missing here doesn't
     * mean the other party don't support it.
     * @return the advertised encryption methods.
     */
    public SrtpControlType[] getAdvertisedEncryptionMethods()
    {
        return
            advertisedEncryptionMethods.toArray(
                    new SrtpControlType[advertisedEncryptionMethods.size()]);
    }

    /**
     * Gets a <tt>MediaDevice</tt> which is capable of capture and/or playback
     * of media of the specified <tt>MediaType</tt>, is the default choice of
     * the user for a <tt>MediaDevice</tt> with the specified <tt>MediaType</tt>
     * and is appropriate for the current states of the associated
     * <tt>CallPeer</tt> and <tt>Call</tt>.
     * <p>
     * For example, when the local peer is acting as a conference focus in the
     * <tt>Call</tt> of the associated <tt>CallPeer</tt>, the audio device must
     * be a mixer.
     * </p>
     *
     * @param mediaType the <tt>MediaType</tt> in which the retrieved
     * <tt>MediaDevice</tt> is to capture and/or play back media
     * @return a <tt>MediaDevice</tt> which is capable of capture and/or
     * playback of media of the specified <tt>mediaType</tt>, is the default
     * choice of the user for a <tt>MediaDevice</tt> with the specified
     * <tt>mediaType</tt> and is appropriate for the current states of the
     * associated <tt>CallPeer</tt> and <tt>Call</tt>
     */
    protected MediaDevice getDefaultDevice(MediaType mediaType)
    {
        return getPeer().getCall().getDefaultDevice(mediaType);
    }

    /**
     * Gets the <tt>MediaDirection</tt> value which represents the preference of
     * the user with respect to streaming media of the specified
     * <tt>MediaType</tt>.
     *
     * @param mediaType the <tt>MediaType</tt> to retrieve the user preference
     * for
     * @return a <tt>MediaDirection</tt> value which represents the preference
     * of the user with respect to streaming media of the specified
     * <tt>mediaType</tt>
     */
    protected MediaDirection getDirectionUserPreference(MediaType mediaType)
    {
        switch (mediaType)
        {
        case AUDIO:
            return audioDirectionUserPreference;
        case VIDEO:
            return videoDirectionUserPreference;
        default:
            throw new IllegalArgumentException("mediaType");
        }
    }

    /**
     * Returns the {@link DynamicPayloadTypeRegistry} instance we are currently
     * using.
     *
     * @return the {@link DynamicPayloadTypeRegistry} instance we are currently
     * using.
     */
    protected DynamicPayloadTypeRegistry getDynamicPayloadTypes()
    {
        return this.dynamicPayloadTypes;
    }

    /**
     * Returns a (possibly empty) <tt>List</tt> of <tt>RTPExtension</tt>s
     * supported by the device that this media handler uses to handle media of
     * the specified <tt>type</tt>.
     *
     * @param type the <tt>MediaType</tt> of the device whose
     * <tt>RTPExtension</tt>s we are interested in.
     *
     * @return a (possibly empty) <tt>List</tt> of <tt>RTPExtension</tt>s
     * supported by the device that this media handler uses to handle media of
     * the specified <tt>type</tt>.
     */
    protected List<RTPExtension> getExtensionsForType(MediaType type)
    {
        return getDefaultDevice(type).getSupportedExtensions();
    }

    /**
     * Returns a list of locally supported <tt>MediaFormat</tt>s for the
     * given <tt>MediaDevice</tt>, ordered in descending priority. Takes into
     * account the configuration obtained from the <tt>ProtocolProvider</tt>
     * instance associated this media handler -- if its set up to override the
     * global encoding settings, uses that configuration, otherwise uses the
     * global configuration.
     *
     * @param mediaDevice the <tt>MediaDevice</tt>.
     *
     * @return a non-null list of locally supported <tt>MediaFormat</tt>s for
     * <tt>mediaDevice</tt>, in decreasing order of priority.
     *
     * @see CallPeerMediaHandler#getLocallySupportedFormats(MediaDevice,
     * QualityPreset, QualityPreset)
     */
    public List<MediaFormat> getLocallySupportedFormats(MediaDevice mediaDevice)
    {
        return getLocallySupportedFormats(mediaDevice, null, null);
    }

    /**
     * Returns a list of locally supported <tt>MediaFormat</tt>s for the
     * given <tt>MediaDevice</tt>, ordered in descending priority. Takes into
     * account the configuration obtained from the <tt>ProtocolProvider</tt>
     * instance associated this media handler -- if its set up to override the
     * global encoding settings, uses that configuration, otherwise uses the
     * global configuration.
     *
     * @param mediaDevice the <tt>MediaDevice</tt>.
     * @param sendPreset the preset used to set some of the format parameters,
     * used for video and settings.
     * @param receivePreset the preset used to set the receive format
     * parameters, used for video and settings.
     *
     * @return a non-null list of locally supported <tt>MediaFormat</tt>s for
     * <tt>mediaDevice</tt>, in decreasing order of priority.
     */
    public List<MediaFormat> getLocallySupportedFormats(
            MediaDevice mediaDevice,
            QualityPreset sendPreset, QualityPreset receivePreset)
    {
        if(mediaDevice == null)
            return Collections.emptyList();

        // Check the address we're using to see if it corresponds to a wired or
        // wireless interface and select the codec configuration accordingly
        EncodingConfiguration encodingConfiguration = mProbableConnectionType.useWirelessCodecs ?
            EncodingConfiguration.getInstanceForPrefix(
                ENCODING_CONFIG_PROP_WIRELESS_PREFIX) :
            EncodingConfiguration.getInstanceForPrefix(
                ENCODING_CONFIG_PROP_WIRED_PREFIX);

        List<MediaFormat> supportedFormats = mediaDevice.getSupportedFormats(
            sendPreset, receivePreset,
            encodingConfiguration);

        return supportedFormats;
    }

    /**
     * Gets the visual <tt>Component</tt>, if any, depicting the video streamed
     * from the local peer to the remote peer.
     *
     * @return the visual <tt>Component</tt> depicting the local video if local
     * video is actually being streamed from the local peer to the remote peer;
     * otherwise, <tt>null</tt>
     */
    public Component getLocalVisualComponent()
    {
        MediaStream videoStream = getStream(MediaType.VIDEO);

        return
            ((videoStream == null) || !isLocalVideoTransmissionEnabled())
                ? null
                : ((VideoMediaStream) videoStream).getLocalVisualComponent();
    }

    public MediaHandler getMediaHandler()
    {
        return mediaHandler;
    }

    /**
     * Returns the peer that is this media handler's "raison d'etre".
     *
     * @return the {@link MediaAwareCallPeer} that this handler is servicing.
     */
    public T getPeer()
    {
        return peer;
    }

    /**
     * Gets the last-known SSRC of an RTP stream with a specific
     * <tt>MediaType</tt> received by a <tt>MediaStream</tt> of this instance.
     *
     * @return the last-known SSRC of an RTP stream with a specific
     * <tt>MediaType</tt> received by a <tt>MediaStream</tt> of this instance
     */
    public long getRemoteSSRC(MediaType mediaType)
    {
        return mediaHandler.getRemoteSSRC(this, mediaType);
    }

    /**
     * Returns the {@link DynamicRTPExtensionsRegistry} instance we are
     * currently using.
     *
     * @return the {@link DynamicRTPExtensionsRegistry} instance we are
     * currently using.
     */
    protected DynamicRTPExtensionsRegistry getRtpExtensionsRegistry()
    {
        return this.rtpExtensionsRegistry;
    }

    /**
     * Gets the <tt>SrtpControl</tt>s of the <tt>MediaStream</tt>s of this
     * instance.
     *
     * @return the <tt>SrtpControl</tt>s of the <tt>MediaStream</tt>s of this
     * instance
     */
    protected Map<MediaTypeSrtpControl, SrtpControl> getSrtpControls()
    {
        return mediaHandler.getSrtpControls(this);
    }

    /**
     * Gets the <tt>MediaStream</tt> of this <tt>CallPeerMediaHandler</tt> which
     * is of a specific <tt>MediaType</tt>. If this instance doesn't have such a
     * <tt>MediaStream</tt>, returns <tt>null</tt>
     *
     * @param mediaType the <tt>MediaType</tt> of the <tt>MediaStream</tt> to
     * retrieve
     * @return the <tt>MediaStream</tt> of this <tt>CallPeerMediaHandler</tt>
     * which is of the specified <tt>mediaType</tt> if this instance has such a
     * <tt>MediaStream</tt>; otherwise, <tt>null</tt>
     */
    public MediaStream getStream(MediaType mediaType)
    {
        switch (mediaType)
        {
        case AUDIO:
            return audioStream;
        case VIDEO:
            return videoStream;
        default:
            throw new IllegalArgumentException("mediaType");
        }
    }

    /**
     * Gets the <tt>TransportManager</tt> implementation handling our )address
     * management.
     *
     * @return the <tt>TransportManager</tt> implementation handling our address
     * management
     */
    protected abstract TransportManager<T> getTransportManager();

    /**
     * Gets the visual <tt>Component</tt> in which video from the remote peer is
     * currently being rendered or <tt>null</tt> if there is currently no video
     * streaming from the remote peer.
     *
     * @return the visual <tt>Component</tt> in which video from the remote peer
     * is currently being rendered or <tt>null</tt> if there is currently no
     * video streaming from the remote peer
     */
    @Deprecated
    public Component getVisualComponent()
    {
        List<Component> visualComponents = getVisualComponents();

        return visualComponents.isEmpty() ? null : visualComponents.get(0);
    }

    /**
     * Gets the visual <tt>Component</tt>s in which videos from the remote peer
     * are currently being rendered.
     *
     * @return the visual <tt>Component</tt>s in which videos from the remote
     * peer are currently being rendered
     */
    public List<Component> getVisualComponents()
    {
        MediaStream videoStream = getStream(MediaType.VIDEO);
        List<Component> visualComponents;

        if (videoStream == null)
            visualComponents = Collections.emptyList();
        else
        {
            visualComponents
                = ((VideoMediaStream) videoStream).getVisualComponents();
        }
        return visualComponents;
    }

    /**
     * Creates if necessary, and configures the stream that this
     * <tt>MediaHandler</tt> is using for the <tt>MediaType</tt> matching the
     * one of the <tt>MediaDevice</tt>.
     *
     * @param connector the <tt>MediaConnector</tt> that we'd like to bind the
     * newly created stream to.
     * @param device the <tt>MediaDevice</tt> that we'd like to attach the newly
     * created <tt>MediaStream</tt> to.
     * @param format the <tt>MediaFormat</tt> that we'd like the new
     * <tt>MediaStream</tt> to be set to transmit in.
     * @param target the <tt>MediaStreamTarget</tt> containing the RTP and RTCP
     * address:port couples that the new stream would be sending packets to.
     * @param direction the <tt>MediaDirection</tt> that we'd like the new
     * stream to use (i.e. sendonly, sendrecv, recvonly, or inactive).
     * @param rtpExtensions the list of <tt>RTPExtension</tt>s that should be
     * enabled for this stream.
     * @param masterStream whether the stream to be used as master if secured
     *
     * @return the newly created <tt>MediaStream</tt>.
     *
     * @throws OperationFailedException if creating the stream fails for any
     * reason (like, for example, accessing the device or setting the format).
     */
    protected MediaStream initStream(StreamConnector    connector,
                                     MediaDevice        device,
                                     MediaFormat        format,
                                     MediaStreamTarget  target,
                                     MediaDirection     direction,
                                     List<RTPExtension> rtpExtensions,
                                     boolean            masterStream)
        throws OperationFailedException
    {
        MediaType mediaType = device.getMediaType();

        /*
         * Do make sure that no unintentional streaming of media generated by
         * the user without prior consent will happen.
         */
        direction = direction.and(getDirectionUserPreference(mediaType));
        /*
         * If the device does not support a direction, there is really nothing
         * to be done at this point to make it use it.
         */
        direction = direction.and(device.getDirection());

        MediaStream stream
            = mediaHandler.initStream(
                    this,
                    connector,
                    device,
                    format,
                    target,
                    direction,
                    rtpExtensions,
                    masterStream);

        setStream(mediaType, stream);

        return stream;
    }

    /**
     * Store the media stream
     *
     * @param mediaType The type of media this stream represents
     * @param stream The stream to store
     */
    protected void setStream(MediaType mediaType, MediaStream stream)
    {
        switch (mediaType)
        {
        case AUDIO:
            audioStream = (AudioMediaStream) stream;
            break;
        case VIDEO:
            videoStream = (VideoMediaStream) stream;
            break;
        }
    }

    /**
     * Create a media stream before we know whether or not it will be used.
     *
     * @param connector the <tt>MediaConnector</tt> that we'd like to bind the
     * newly created stream to.
     * @param device the <tt>MediaDevice</tt> that we'd like to attach the newly
     * created <tt>MediaStream</tt> to.
     * @param format the <tt>MediaFormat</tt> that we'd like the new
     * <tt>MediaStream</tt> to be set to transmit in.
     * @param target the <tt>MediaStreamTarget</tt> containing the RTP and RTCP
     * address:port couples that the new stream would be sending packets to.
     * @param direction the <tt>MediaDirection</tt> that we'd like the new
     * stream to use (i.e. sendonly, sendrecv, recvonly, or inactive).
     * @param rtpExtensions the list of <tt>RTPExtension</tt>s that should be
     * enabled for this stream.
     * @param masterStream whether the stream to be used as master if secured
     *
     * @return the newly created <tt>MediaStream</tt>.
     */
    protected void preCreateMediaStream(StreamConnector    connector,
                                        MediaDevice        device,
                                        MediaFormat        format,
                                        MediaStreamTarget  target,
                                        MediaDirection     direction,
                                        List<RTPExtension> rtpExtensions,
                                        boolean            masterStream)
    {
        // Don't create media stream for telephony events. They're handled elsewhere
        if ("telephone-event".equals(format.getEncoding()))
            return;

        mediaHandler.precreateStream(
                    this,
                    connector,
                    device,
                    format,
                    target,
                    direction,
                    rtpExtensions,
                    masterStream);
    }

    /**
     * Fetch a pre-created stream for a particular format. This will ensure that
     * the media handler is set to use this stream hereafter.  Any other streams
     * that have been pre-created will be cleared up.
     *
     * @param format The format of the stream that is required
     * @return the corresponding stream (or null if it doesn't exist)
     */
    protected MediaStream fetchPreCreatedStream(MediaFormat format)
    {
        return mediaHandler.fetchPreCreatedStream(this, format);
    }

    /**
     * Compares a list of <tt>MediaFormat</tt>s offered by a remote party to the list of locally
     * supported <tt>RTPExtension</tt>s in preferred order (as returned by one of our local
     *  <tt>MediaDevice</tt>s) and returns a list of formats to include in an SDP response.
     *
     * The returned list will contain our most preferred codec supported by the remote, and also the
     * telephone-event format if supported by us and remote.
     *
     * @param remoteFormats remote <tt>MediaFormat</tt> found in the
     * SDP message
     * @param localFormats local supported <tt>MediaFormat</tt> of our device in order of
     * preference
     * @return Formats to include in SDP response.
     */
    protected List<MediaFormat> getFormatsForSdpResponse(
                                            List<MediaFormat> remoteFormats,
                                            List<MediaFormat> localFormats)
    {
        logger.info("Getting formats from SDP Response:\nRemote Formats: " +
                             remoteFormats + "\nLocal  Formats: " + localFormats);

        List<MediaFormat> ret = new ArrayList<>();
        MediaFormat telephoneEventFormat = null;

        for (MediaFormat localFormat : localFormats)
        {
            MediaFormat matchingRemoteFormat
                = findMediaFormat(remoteFormats, localFormat);

            if (matchingRemoteFormat != null)
            {
                // We ignore telephone-event here as it's not a real media
                // format.  Therefore we don't want to decide to use it as
                // our preferred format.  We'll add it back later if we find
                // a suitable format.
                //
                // Note if there are multiple telephone-event formats, we'll
                // lose all but the last one.  That's fine because it's
                // meaningless to have multiple repeated formats.
                if (Constants.TELEPHONE_EVENT.equals(matchingRemoteFormat.getEncoding()))
                {
                    logger.debug(
                            "Matched remote telephone event so continuing: [" +
                                         matchingRemoteFormat + "]");
                    telephoneEventFormat = matchingRemoteFormat;
                    continue;
                }

                // We only want to return 1 matching codec.  Thus only add the
                // match if we haven't added any already.
                if (ret.isEmpty())
                {
                    logger.debug(
                        "Found highest priority matching format: [" +
                                     matchingRemoteFormat + "]");
                    ret.add(matchingRemoteFormat);
                }
            }
        }

        // If we've found some compatible formats, add telephone-event back
        // in to the end of the list if we removed it above.  If we didn't
        // find any compatible formats, we don't want to add telephone-event
        // as the only entry in the list because there'd be no media.
        if ((!ret.isEmpty()) && (telephoneEventFormat != null))
        {
            ret.add(telephoneEventFormat);
        }

        logger.info("Returning formats: " + ret);
        return ret;
    }

    /**
     * Compares a list of <tt>RTPExtension</tt>s offered by a remote party
     * to the list of locally supported <tt>RTPExtension</tt>s as returned
     * by one of our local <tt>MediaDevice</tt>s and returns a third
     * <tt>List</tt> that contains their intersection. The returned
     * <tt>List</tt> contains extensions supported by both the remote party and
     * the local device that we are dealing with. Direction attributes of both
     * lists are also intersected and the returned <tt>RTPExtension</tt>s have
     * directions valid from a local perspective. In other words, if
     * <tt>remoteExtensions</tt> contains an extension that the remote party
     * supports in a <tt>SENDONLY</tt> mode, and we support that extension in a
     * <tt>SENDRECV</tt> mode, the corresponding entry in the returned list will
     * have a <tt>RECVONLY</tt> direction.
     *
     * @param remoteExtensions the <tt>List</tt> of <tt>RTPExtension</tt>s as
     * advertised by the remote party.
     * @param supportedExtensions the <tt>List</tt> of <tt>RTPExtension</tt>s
     * that a local <tt>MediaDevice</tt> returned as supported.
     *
     * @return the (possibly empty) intersection of both of the extensions lists
     * in a form that can be used for generating an SDP media description or
     * for configuring a stream.
     */
    protected List<RTPExtension> intersectRTPExtensions(
                                    List<RTPExtension> remoteExtensions,
                                    List<RTPExtension> supportedExtensions)
    {
        if(remoteExtensions == null || supportedExtensions == null)
            return new ArrayList<>();

        List<RTPExtension> intersection = new ArrayList<>(
                Math.min(remoteExtensions.size(), supportedExtensions.size()));

        //loop through the list that the remote party sent
        for(RTPExtension remoteExtension : remoteExtensions)
        {
            RTPExtension localExtension = findExtension(
                    supportedExtensions, remoteExtension.getURI().toString());

            if(localExtension == null)
                continue;

            MediaDirection localDir  = localExtension.getDirection();
            MediaDirection remoteDir = remoteExtension.getDirection();

            RTPExtension intersected = new RTPExtension(
                            localExtension.getURI(),
                            localDir.getDirectionForAnswer(remoteDir),
                            remoteExtension.getExtensionAttributes());

            intersection.add(intersected);
        }

        return intersection;
    }

    /**
     * Checks whether <tt>dev</tt> can be used for a call.
     *
     * @return <tt>true</tt> if the device is not null, and it has at least
     * one enabled format. Otherwise <tt>false</tt>
     */
    public boolean isDeviceActive(MediaDevice dev)
    {
        return (dev != null) && !getLocallySupportedFormats(dev).isEmpty();
    }

    /**
     * Checks whether <tt>dev</tt> can be used for a call, using
     * <tt>sendPreset</tt> and <tt>reveicePreset</tt>
     *
     * @return <tt>true</tt> if the device is not null, and it has at least
     * one enabled format. Otherwise <tt>false</tt>
     */
    public boolean isDeviceActive(
            MediaDevice dev,
            QualityPreset sendPreset, QualityPreset receivePreset)
    {
        return
            (dev != null)
                && !getLocallySupportedFormats(dev, sendPreset, receivePreset)
                        .isEmpty();
    }

    /**
     * Determines whether this media handler is currently set to transmit local
     * audio.
     *
     * @return <tt>true</tt> if the media handler is set to transmit local audio
     * and <tt>false</tt> otherwise.
     */
    public boolean isLocalAudioTransmissionEnabled()
    {
        return audioDirectionUserPreference.allowsSending();
    }

    /**
     * Determines whether this handler's streams have been placed on hold.
     *
     * @return <tt>true</tt> if this handler's streams have been placed on hold
     * and <tt>false</tt> otherwise.
     */
    public boolean isLocallyOnHold()
    {
        return locallyOnHold;
        //no need to actually check stream directions because we only update
        //them through the setLocallyOnHold() method so if the value of the
        //locallyOnHold field has changed, so have stream directions.
    }

    /**
     * Determines whether this media handler is currently set to transmit local
     * video.
     *
     * @return <tt>true</tt> if the media handler is set to transmit local video
     * and false otherwise.
     */
    public boolean isLocalVideoTransmissionEnabled()
    {
        return videoDirectionUserPreference.allowsSending();
    }

    /**
     * Determines whether this media handler is currently set to allow remote
     * video streams
     *
     * @return <tt>true</tt> if the media handler is set to allow remote video
     * and false otherwise.
     */
    public boolean isRemoteVideoTransmissionEnabled()
    {
        return videoDirectionUserPreference.allowsReceiving();
    }

    /**
     * Dump call statistics to logs.
     */
    private void dumpStatsToLog()
    {
        if (audioStream != null)
        {
            MediaStreamStats mediaStreamStats = audioStream.getMediaStreamStats();
            mediaStreamStats.updateStats();
            logger.debug(mediaStreamStats.toString());
        }

        logger.debug(receivedResettableMediaListener.toString());
        receivedResettableMediaListener.reset();

        logger.debug(sentResettableMediaListener.toString());
        sentResettableMediaListener.reset();

//        printFlowStatistics(rtpManager); // Uncomment if we want these extra stats
    }

    /**
     * Get network loss stats and put up a warning if the value exceeds the
     * threshold.
     */
    private void checkNetworkLossStats()
    {
        // Don't do any of this if there is no audio stream
        if (audioStream == null) return;

        long txSSRC = audioStream.getLocalSourceID();
        long rxSSRC = audioStream.getRemoteSourceID();

        MediaStreamStats audioStats = audioStream.getMediaStreamStats();
        MediaAwareCall<?, ?, ?> call = peer.getCall();

        if ((audioStats != null) && (call != null))
        {
            RTCPFeedback outFeedback = audioStats.getReceivedFeedback(txSSRC);
            RTCPFeedback inFeedback = audioStats.getSentFeedback(rxSSRC);

            if (outFeedback != null)
            {
                int outLoss = outFeedback.getFractionLost();
                logger.debug(String.format("Outbound loss=%d (%.1f%%)",
                                               outLoss, ((100.0*outLoss)/255)));

                call.updateCallState(CallChangeEvent.OUTBOUND_PACKET_LOSS_CHANGE,
                                     outLoss > NETWORK_LOSS_ERROR_THRESHOLD);
            }

            if (inFeedback != null)
            {
                int inLoss = inFeedback.getFractionLost();
                logger.debug(String.format("Outbound loss=%d (%.1f%%)",
                                                 inLoss, ((100.0*inLoss)/255)));

                call.updateCallState(CallChangeEvent.INBOUND_PACKET_LOSS_CHANGE,
                                     inLoss > NETWORK_LOSS_ERROR_THRESHOLD);
            }
        }
    }

    /**
     * Determines whether the audio stream of this media handler is currently
     * on mute.
     *
     * @return <tt>true</tt> if local audio transmission is currently on mute
     * and <tt>false</tt> otherwise.
     */
    public boolean isMute()
    {
        MediaStream audioStream = getStream(MediaType.AUDIO);

        return (audioStream != null) && audioStream.isMute();
    }

    /**
     * Determines whether the remote party has placed all our streams on hold.
     *
     * @return <tt>true</tt> if all our streams have been placed on hold (i.e.
     * if none of them is currently sending and <tt>false</tt> otherwise.
     */
    public boolean isRemotelyOnHold()
    {
        for (MediaType mediaType : MediaType.values())
        {
            MediaStream stream = getStream(mediaType);

            if ((stream != null) && stream.getDirection().allowsSending())
                return false;
        }
        return true;
    }

    /**
     * Returns the secure state of the call. If both audio and video is secured.
     *
     * @return the call secure state
     */
    public boolean isSecure()
    {
        for (MediaType mediaType : MediaType.values())
        {
            MediaStream stream = getStream(mediaType);

            /*
             * If a stream for a specific MediaType does not exist, it's
             * considered secure.
             */
            if ((stream != null)
                    && !stream.getSrtpControl().getSecureCommunicationStatus())
                return false;
        }
        return true;
    }

    /**
     * Notifies this instance about a <tt>PropertyChangeEvent</tt> fired by the
     * associated {@link MediaHandler}. Since this instance wraps around the
     * associated <tt>MediaHandler</tt>, it forwards the property changes as its
     * own. Allows extenders to override.
     *
     * @param ev the <tt>PropertyChangeEvent</tt> fired by the associated
     * <tt>MediaHandler</tt>
     */
    protected void mediaHandlerPropertyChange(PropertyChangeEvent ev)
    {
        firePropertyChange(
                ev.getPropertyName(),
                ev.getOldValue(), ev.getNewValue());
    }

    /**
     * Processes a request for a (video) key frame from the remote peer to the
     * local peer.
     *
     * @return <tt>true</tt> if the request for a (video) key frame has been
     * honored by the local peer; otherwise, <tt>false</tt>
     */
    public boolean processKeyFrameRequest()
    {
        return mediaHandler.processKeyFrameRequest(this);
    }

    /**
     * Unregisters a specific <tt>VideoListener</tt> from this instance so that
     * it stops receiving notifications from it about changes in the
     * availability of visual <tt>Component</tt>s displaying video.
     *
     * @param listener the <tt>VideoListener</tt> to be unregistered from this
     * instance and to stop receiving notifications from it about changes in the
     * availability of visual <tt>Component</tt>s displaying video
     */
    public void removeVideoListener(VideoListener listener)
    {
        videoNotifierSupport.removeVideoListener(listener);
    }

    /**
     * Requests a key frame from the remote peer of the associated
     * <tt>VideoMediaStream</tt> of this <tt>CallPeerMediaHandler</tt>. The
     * default implementation provided by <tt>CallPeerMediaHandler</tt> always
     * returns <tt>false</tt>.
     *
     * @return <tt>true</tt> if this <tt>CallPeerMediaHandler</tt> has indeed
     * requested a key frame from the remote peer of its associated
     * <tt>VideoMediaStream</tt> in response to the call; otherwise,
     * <tt>false</tt>
     */
    protected boolean requestKeyFrame()
    {
        return false;
    }

    /**
     * Sets <tt>csrcAudioLevelListener</tt> as the listener that will be
     * receiving notifications for changes in the audio levels of the remote
     * participants that our peer is mixing.
     *
     * @param listener the <tt>CsrcAudioLevelListener</tt> to set to our audio
     * stream.
     */
    public void setCsrcAudioLevelListener(CsrcAudioLevelListener listener)
    {
        synchronized (csrcAudioLevelListenerLock)
        {
            if (this.csrcAudioLevelListener != listener)
            {
                MediaHandler mediaHandler = getMediaHandler();

                if ((mediaHandler != null)
                        && (this.csrcAudioLevelListener != null))
                {
                    mediaHandler.removeCsrcAudioLevelListener(
                            this.csrcAudioLevelListener);
                }

                this.csrcAudioLevelListener = listener;

                if ((mediaHandler != null)
                        && (this.csrcAudioLevelListener != null))
                {
                    mediaHandler.addCsrcAudioLevelListener(
                            this.csrcAudioLevelListener);
                }
            }
        }
    }

    /**
     * Puts all <tt>MediaStream</tt>s in this handler locally on or off hold
     * (according to the value of <tt>locallyOnHold</tt>). This would also be
     * taken into account when the next update offer is generated.
     *
     * @param locallyOnHold <tt>true</tt> if we are to make our audio stream
     * stop transmitting and <tt>false</tt> if we are to start transmitting
     * again.
     */
    public void setLocallyOnHold(boolean locallyOnHold)
    {
        this.locallyOnHold = locallyOnHold;

        // On hold.
        if(locallyOnHold)
        {
            MediaStream audioStream = getStream(MediaType.AUDIO);

            if(audioStream != null)
            {
                audioStream.setDirection(
                        audioStream.getDirection().and(
                                MediaDirection.SENDONLY));
                audioStream.setMute(locallyOnHold);
            }

            MediaStream videoStream = getStream(MediaType.VIDEO);

            if (videoStream != null)
            {
                videoStream.stop();
            }
        }
        /*
         * Off hold. Make sure that we re-enable sending only if other party is
         * not on hold.
         */
        else if (!CallPeerState.ON_HOLD_MUTUALLY.equals(getPeer().getState()))
        {
            MediaStream audioStream = getStream(MediaType.AUDIO);

            if(audioStream != null)
            {
                audioStream.setDirection(
                        audioStream.getDirection().or(MediaDirection.SENDONLY));
                audioStream.setMute(locallyOnHold);
            }

            MediaStream videoStream = getStream(MediaType.VIDEO);

            if (videoStream != null)
            {
                videoStream.stop();
            }
        }
    }

    /**
     * If the local <tt>AudioMediaStream</tt> has already been created, sets
     * <tt>listener</tt> as the <tt>SimpleAudioLevelListener</tt> that it should
     * notify for local user level events. Otherwise stores a reference to
     * <tt>listener</tt> so that we could add it once we create the stream.
     *
     * @param listener the <tt>SimpleAudioLevelListener</tt> to add or
     * <tt>null</tt> if we are trying to remove it.
     */
    public void setLocalUserAudioLevelListener(
            SimpleAudioLevelListener listener)
    {
        synchronized (localUserAudioLevelListenerLock)
        {
            if (this.localUserAudioLevelListener != listener)
            {
                MediaHandler mediaHandler = getMediaHandler();

                if ((mediaHandler != null)
                        && (this.localUserAudioLevelListener != null))
                {
                    mediaHandler.removeLocalUserAudioLevelListener(
                            this.localUserAudioLevelListener);
                }

                this.localUserAudioLevelListener = listener;

                if ((mediaHandler != null)
                        && (this.localUserAudioLevelListener != null))
                {
                    mediaHandler.addLocalUserAudioLevelListener(
                            this.localUserAudioLevelListener);
                }
            }
        }
    }

    /**
     * Specifies whether this media handler should be allowed to transmit
     * local video.
     *
     * @param enabled  <tt>true</tt> if the media handler should transmit local
     * video and <tt>false</tt> otherwise.
     */
    public void setLocalVideoTransmissionEnabled(boolean enabled)
    {
        MediaDirection oldValue = videoDirectionUserPreference;

        videoDirectionUserPreference
            = enabled ? MediaDirection.SENDRECV : MediaDirection.RECVONLY;

        MediaDirection newValue = videoDirectionUserPreference;

        /*
         * Do not send an event here if the local video is enabled because the
         * video stream needs to start before the correct MediaDevice is set in
         * VideoMediaDeviceSession.
         */
        if (!enabled)
        {
            firePropertyChange(
                    OperationSetVideoTelephony.LOCAL_VIDEO_STREAMING,
                    oldValue, newValue);
        }
    }

    /**
     * Specifies whether this media handler should be allowed to receive
     * remote video.
     *
     * @param enabled whether the media handler can receive remote video.
     */
    public void setRemoteVideoTransmissionEnabled(boolean enabled)
    {
        if (enabled)
        {
            // If remote transmission is enabled then we also need to check
            // whether local transmission is enabled
            if (isLocalVideoTransmissionEnabled())
            {
                videoDirectionUserPreference = MediaDirection.SENDRECV;
            }
            else
            {
                videoDirectionUserPreference = MediaDirection.RECVONLY;
            }
        }
        else
        {
            videoDirectionUserPreference = MediaDirection.INACTIVE;
        }
    }

    public void setMediaHandler(MediaHandler mediaHandler)
    {
        if (this.mediaHandler != mediaHandler)
        {
            if (this.mediaHandler != null)
            {
                synchronized (csrcAudioLevelListenerLock)
                {
                    if (csrcAudioLevelListener != null)
                    {
                        this.mediaHandler.removeCsrcAudioLevelListener(
                                csrcAudioLevelListener);
                    }
                }
                synchronized (localUserAudioLevelListenerLock)
                {
                    if (localUserAudioLevelListener != null)
                    {
                        this.mediaHandler.removeLocalUserAudioLevelListener(
                                localUserAudioLevelListener);
                    }
                }
                synchronized (streamAudioLevelListenerLock)
                {
                    if (streamAudioLevelListener != null)
                    {
                        this.mediaHandler.removeStreamAudioLevelListener(
                                streamAudioLevelListener);
                    }
                }

                this.mediaHandler.removeKeyFrameRequester(keyFrameRequester);
                this.mediaHandler.removePropertyChangeListener(
                        mediaHandlerPropertyChangeListener);
                if (srtpListener != null)
                    this.mediaHandler.removeSrtpListener(srtpListener);
                this.mediaHandler.removeVideoListener(videoStreamVideoListener);

                // Make sure that the prefetched streams have been cleared
                this.mediaHandler.clearPrefetchedStreams();
            }

            this.mediaHandler = mediaHandler;

            if (this.mediaHandler != null)
            {
                synchronized (csrcAudioLevelListenerLock)
                {
                    if (csrcAudioLevelListener != null)
                    {
                        this.mediaHandler.addCsrcAudioLevelListener(
                                csrcAudioLevelListener);
                    }
                }
                synchronized (localUserAudioLevelListenerLock)
                {
                    if (localUserAudioLevelListener != null)
                    {
                        this.mediaHandler.addLocalUserAudioLevelListener(
                                localUserAudioLevelListener);
                    }
                }
                synchronized (streamAudioLevelListenerLock)
                {
                    if (streamAudioLevelListener != null)
                    {
                        this.mediaHandler.addStreamAudioLevelListener(
                                streamAudioLevelListener);
                    }
                }

                this.mediaHandler.addKeyFrameRequester(-1, keyFrameRequester);
                this.mediaHandler.addPropertyChangeListener(
                        mediaHandlerPropertyChangeListener);
                if (srtpListener != null)
                    this.mediaHandler.addSrtpListener(srtpListener);
                this.mediaHandler.addVideoListener(videoStreamVideoListener);
            }
        }
    }

    /**
     * Causes this handler's <tt>AudioMediaStream</tt> to stop transmitting the
     * audio being fed from this stream's <tt>MediaDevice</tt> and transmit
     * silence instead.
     *
     * @param mute <tt>true</tt> if we are to make our audio stream start
     * transmitting silence and <tt>false</tt> if we are to end the transmission
     * of silence and use our stream's <tt>MediaDevice</tt> again.
     */
    public void setMute(boolean mute)
    {
        MediaStream audioStream = getStream(MediaType.AUDIO);

        if (audioStream != null)
            audioStream.setMute(mute);
    }

    /**
     * If the local <tt>AudioMediaStream</tt> has already been created, sets
     * <tt>listener</tt> as the <tt>SimpleAudioLevelListener</tt> that it should
     * notify for stream user level events. Otherwise stores a reference to
     * <tt>listener</tt> so that we could add it once we create the stream.
     *
     * @param listener the <tt>SimpleAudioLevelListener</tt> to add or
     * <tt>null</tt> if we are trying to remove it.
     */
    public void setStreamAudioLevelListener(SimpleAudioLevelListener listener)
    {
        synchronized (streamAudioLevelListenerLock)
        {
            if (this.streamAudioLevelListener != listener)
            {
                MediaHandler mediaHandler = getMediaHandler();

                if ((mediaHandler != null)
                        && (this.streamAudioLevelListener != null))
                {
                    mediaHandler.removeStreamAudioLevelListener(
                            this.streamAudioLevelListener);
                }

                this.streamAudioLevelListener = listener;

                if ((mediaHandler != null)
                        && (this.streamAudioLevelListener != null))
                {
                    mediaHandler.addStreamAudioLevelListener(
                            this.streamAudioLevelListener);
                }
            }
        }
    }

    /**
     * Starts this <tt>CallPeerMediaHandler</tt>. If it has already been
     * started, does nothing.
     *
     * @throws IllegalStateException if this method is called without this
     * handler having first seen a media description or having generated an
     * offer.
     */
    public void start()
        throws IllegalStateException
    {
        logger.debug("Starting CallPeerMediaHandler: " + this.hashCode());
        MediaStream stream;

        stream = getStream(MediaType.AUDIO);

        if (stream != null)
        {
            if (!startCalled)
            {
                logger.debug("Start not called, setting start time");
                audioStartTime = System.currentTimeMillis();
                sendTelemetryCallConnected();

                sendHolePunchPackets(stream, MediaType.AUDIO);

                if (isLocalAudioTransmissionEnabled())
                {
                    asyncSetTrafficClass(MediaType.AUDIO);
                }
            }

            if(!stream.isStarted() &&
                isLocalAudioTransmissionEnabled())
            {
                logger.info("Audio stream present but not yet started.");
                stream.start();
                started = true;
            }
        }

        stream = getStream(MediaType.VIDEO);
        if (stream != null)
        {
            /*
             * Inform listener of LOCAL_VIDEO_STREAMING only once the video
             * starts so that VideoMediaDeviceSession has correct MediaDevice
             * set (switch from desktop streaming to webcam video or vice-versa
             * issue)
             */
            firePropertyChange(
                    OperationSetVideoTelephony.LOCAL_VIDEO_STREAMING,
                    null, videoDirectionUserPreference);

            if(!stream.isStarted())
            {
                logger.info("Video stream present but not yet started.");
                asyncSetTrafficClass(MediaType.VIDEO);
                stream.start();

                if (stream instanceof VideoMediaStream)
                {
                    sendHolePunchPackets(stream, MediaType.VIDEO);
                }
            }
        }

        // Set up all our stats listeners, but only if the stream isn't already
        // started - we only want one of these listeners per stream.
        if (!startCalled)
        {
            receivedResettableMediaListener =
                new MediaEnergyListener("Received - Resettable");
            sentResettableMediaListener =
                new MediaEnergyListener("Sent - Resettable");

            receivedMediaListener = new MediaEnergyListener("Received");
            sentMediaListener = new MediaEnergyListener("Sent");

            peer.addStreamSoundLevelListener(receivedMediaListener);
            peer.getCall().addLocalUserSoundLevelListener(sentMediaListener);

            peer.addStreamSoundLevelListener(receivedResettableMediaListener);
            peer.getCall().addLocalUserSoundLevelListener(
                sentResettableMediaListener);

            // Create a timer thread to periodically dump stats, and update the
            // "no network" warning if necessary.
            final int interval =
                ProtocolMediaActivator.getConfigurationService().global().getInt(
                                            PROPERTY_NAME_STATS_INTERVAL, 2500);
            new Timer("Stats dump timer").scheduleAtFixedRate(new TimerTask()
            {
                @Override
                public void run()
                {
                    if (started)
                    {
                        dumpStatsToLog();
                        checkNetworkLossStats();
                    }
                    else
                    {
                        this.cancel();
                    }
                }
            }, interval, interval);

            // Add a new sound listener to listen for low media
            SoundLevelListener listener = new LowMediaListener();
            peer.getCall().addLocalUserSoundLevelListener(listener);

            // Store the call ID off for later.  We'll also need to
            // send it to SAS to get a trail ID.
            mSIPCallID = peer.getDialogCallID();
            sendAnalyticsMarker();

            mCallDirection = peer.dialogIsServer() ?
                                 AnalyticsParameter.VALUE_INBOUND :
                                 AnalyticsParameter.VALUE_OUTBOUND;
            if (peer.getCall() != null)
            {
                mUserDefinedCallStartTime = peer.getCall().getUserPerceivedCallStartTime();
                logger.debug("Storing start time=" + mUserDefinedCallStartTime);
            }
        }

        startCalled = true;
    }

    private void sendTelemetryCallConnected()
    {
        if (audioStream == null || peer == null) {
            return;
        }

        MediaStreamStats audioStats = audioStream.getMediaStreamStats();

        String codecName = audioStats.getEncoding();
        String codecHz = audioStats.getEncodingClockRate();
        int clockRate = 0;
        try
        {
            clockRate = Integer.valueOf(codecHz) / 1000;
        }
        catch(NumberFormatException e)
        {
            logger.error("Unexpected codec clock rate: " + codecHz);
        }

        String networkType = mProbableConnectionType.analyticsParam;
        boolean isConnected = peer.getState().equals(CallPeerState.CONNECTED);

        ProtocolMediaActivator.getInsightsService().logEvent(InsightsEventHint.PROTOCOL_MEDIA_CALL_CONNECTED.name(),
                                                             Map.of(NETWORK_TYPE.name(), networkType,
                                                                    CONNECTED.name(), isConnected,
                                                                    CODEC_NAME.name(), codecName,
                                                                    CLOCK_RATE.name(), clockRate));
    }

    /**
     * Sends an empty hole punch packet for the given media stream to unblock
     * some kinds of RTP proxies. This allows one-way video streams to reach the
     * us, and aids with audio cut-through time.
     *
     * @param stream the media stream for which to send hole punch packets
     * @param mediaType the type of media stream (audio or video).
     */
    private void sendHolePunchPackets(MediaStream stream, MediaType mediaType)
    {
        StreamConnector connector;
        try
        {
            connector = getTransportManager().
                                getStreamConnector(mediaType);

            // Don't send hole punch packets if this is a TCP stream.
            if (connector.getProtocol() != StreamConnector.Protocol.TCP)
            {
                stream.sendHolePunchPackets(stream.getTarget());
            }
        }
        catch (OperationFailedException ex)
        {
            logger.error("Failed to get stream connector for " + mediaType, ex);
        }
    }

    /**
     * Sets the QoS traffic class for the given stream, according to its media
     * type.
     * We do this in a new thread so as to not block the SIP stack.
     */
    private void asyncSetTrafficClass(final MediaType mediaType)
    {
        new Thread("TrafficClassSetter")
        {
            @Override
            public void run()
            {
                logger.debug("Setting traffic class for " + mediaType);
                getTransportManager().setTrafficClass(mediaType);
                logger.debug("Successfully set traffic class for " + mediaType);
            }
        }.start();
    }

    /**
     * Passes <tt>multiStreamData</tt> to the video stream that we are using
     * in this media handler (if any) so that the underlying SRTP lib could
     * properly handle stream security.
     *
     * @param master the data that we are supposed to pass to our
     * video stream.
     */
    public void startSrtpMultistream(SrtpControl master)
    {
        MediaStream videoStream = getStream(MediaType.VIDEO);

        if (videoStream != null)
            videoStream.getSrtpControl().setMultistream(master);
    }

    /**
     * Lets the underlying implementation take note of this error and only
     * then throws it to the using bundles.
     *
     * @param message the message to be logged and then wrapped in a new
     * <tt>OperationFailedException</tt>
     * @param errorCode the error code to be assigned to the new
     * <tt>OperationFailedException</tt>
     * @param cause the <tt>Throwable</tt> that has caused the necessity to log
     * an error and have a new <tt>OperationFailedException</tt> thrown
     *
     * @throws OperationFailedException the exception that we wanted this method
     * to throw.
     */
    protected abstract void throwOperationFailedException(
            String message,
            int errorCode,
            Throwable cause)
        throws OperationFailedException;

    /**
     * A Sound Level Listener which listens for, and broadcasts about, low
     * media at the start of the call.
     *
     * Low media is determined by whether a number of consecutive packets is
     * below a certain threshold
     */
    private final class LowMediaListener implements SoundLevelListener
    {
        private final ConfigurationService cfg =
                 ProtocolMediaActivator.getConfigurationService();

        /**
         * The number of events to ignore at the start of the call
         */
        private final int NB_EVENTS_IGNORE =
            cfg.global().getInt("net.java.sip.communicator.service.protocol.media." +
                "NB_EVENTS_IGNORE", 125);

        /**
         * The number of consecutive events we need before sending a broadcast
         */
        private final int NB_CONSECUTIVE_EVENTS_THRESHOLD =
            cfg.global().getInt("net.java.sip.communicator.service.protocol.media." +
                    "NB_CONSECUTIVE_EVENTS", 15);

        // Thresholds of the sound levels for the media states
        private final int THRESHOLD_LOW_MEDIA =
            cfg.global().getInt("net.java.sip.communicator.service.protocol.media." +
                    "THRESHOLD_LOW_MEDIA", 6);
        private final int THRESHOLD_MED_MEDIA =
            cfg.global().getInt("net.java.sip.communicator.service.protocol.media." +
                    "THRESHOLD_MED_MEDIA", 20);
        private final int THRESHOLD_HIGH_MEDIA =
            cfg.global().getInt("net.java.sip.communicator.service.protocol.media." +
                    "THRESHOLD_HIGH_MEDIA", 30);

        /**
         * The number of times the sound level has changed
         */
        private int mCount = 0;

        /**
         * The current media state that we are tracking
         */
        private int mState = -1;

        /**
         * The number of events we have been told about for our current
         * state.
         */
        private int nbConsecutivePacketsAtLevel = 0;

        /**
         * The state that we last broadcast
         */
        private int mBroadcastState = -1;

        public void soundLevelChanged(Object source, int level)
        {
            mCount++;

            int state = -1;

            // Work out what state the passed level represents
            if (level >= THRESHOLD_HIGH_MEDIA)
            {
                state = CallChangeEvent.HIGH_MEDIA;
            }
            else if (mCount > NB_EVENTS_IGNORE)
            {
                // The first events are an unreliable indicator of low
                // media as the echo canceller tunes so wait until the
                // count is higher before checking the state
                if (level < THRESHOLD_LOW_MEDIA)
                    state = CallChangeEvent.NO_MEDIA;
                else if (level < THRESHOLD_MED_MEDIA)
                    state = CallChangeEvent.LOW_MEDIA;
                else
                    state = CallChangeEvent.MED_MEDIA;
            }

            if (state == mState)
            {
                // State hasn't changed
                nbConsecutivePacketsAtLevel++;
            }
            else
            {
                // State has changed
                mState = state;
                nbConsecutivePacketsAtLevel = 0;
            }

            MediaAwareCall<?, ?, ?> call = peer.getCall();

            if (nbConsecutivePacketsAtLevel > NB_CONSECUTIVE_EVENTS_THRESHOLD &&
                mBroadcastState < mState)
            {
                // We've got enough events to broadcast, and haven't yet
                // broadcast it - do so now.
                mBroadcastState = mState;
                if (call != null)
                {
                    call.updateCallState(CallChangeEvent.CALL_MEDIA_STATE_CHANGE, mState);
                }
            }

            if (!started && call != null)
            {
                // The call has ended so the job of this listener is done.
                call.removeLocalUserSoundLevelListener(this);
            }
        }
    }

    /**
     * Represents the <tt>PropertyChangeListener</tt> which listens to changes
     * in the values of the properties of the <tt>Call</tt> of {@link #peer}.
     * Remembers the <tt>Call</tt> it has been added to because <tt>peer</tt>
     * does not have a <tt>call</tt> anymore at the time {@link #close()} is
     * called.
     */
    private class CallPropertyChangeListener
        implements PropertyChangeListener
    {
        /**
         * The <tt>Call</tt> this <tt>PropertyChangeListener</tt> will be or is
         * already added to.
         */
        private final MediaAwareCall<?, ?, ?> call;

        /**
         * Initializes a new <tt>CallPropertyChangeListener</tt> which is to be
         * added to a specific <tt>Call</tt>.
         *
         * @param call the <tt>Call</tt> the new instance is to be added to
         */
        public CallPropertyChangeListener(MediaAwareCall<?, ?, ?> call)
        {
            this.call = call;
        }

        /**
         * Notifies this instance that the value of a specific property of
         * {@link #call} has changed from a specific old value to a specific
         * new value.
         *
         * @param event a <tt>PropertyChangeEvent</tt> which specifies the name
         * of the property which had its value changed and the old and new
         * values
         */
        public void propertyChange(PropertyChangeEvent event)
        {
            callPropertyChange(event);
        }

        /**
         * Removes this <tt>PropertyChangeListener</tt> from its associated
         * <tt>Call</tt>.
         */
        public void removePropertyChangeListener()
        {
            call.removePropertyChangeListener(this);
        }
    }
}
