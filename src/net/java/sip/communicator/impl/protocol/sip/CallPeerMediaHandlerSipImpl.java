/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.sip;

import java.net.*;
import java.text.ParseException;
import java.util.*;

import javax.sdp.*;
import javax.sip.header.ContentTypeHeader;

import net.java.sip.communicator.impl.protocol.sip.sdp.*;
import net.java.sip.communicator.service.analytics.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.device.*;
import org.jitsi.service.neomedia.format.*;
import org.jitsi.service.neomedia.rtp.*;
import org.jitsi.service.resources.*;

import ch.imvs.sdes4j.srtp.*;
import gov.nist.javax.sip.message.Content;
import gov.nist.javax.sip.message.ContentImpl;

/**
 * The media handler class handles all media management for a single
 * <tt>CallPeer</tt>. This includes initializing and configuring streams,
 * generating SDP, handling ICE, etc. One instance of <tt>CallPeer</tt> always
 * corresponds to exactly one instance of <tt>CallPeerMediaHandler</tt> and
 * both classes are only separated for reasons of readability.
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public class CallPeerMediaHandlerSipImpl
    extends CallPeerMediaHandler<CallPeerSipImpl>
{
    /**
     * Our class logger.
     */
    private static final Logger logger
        = Logger.getLogger(CallPeerMediaHandlerSipImpl.class);

    /**
     * The error string in resources used when no audio or video devices have
     * been found to use in a call.
     */
    private static final String NO_AUDIO_VIDEO_DEVICES =
        "service.protocol.media.NO_AUDIO_VIDEO_DEVICES";

    /**
     * The error string in resources used when no valid media descriptions
     * have been found to use in a call.
     */
    private static final String NO_VALID_MEDIA_DESCS =
        "service.protocol.media.NO_VALID_MEDIA_DESCS";

    /**
     * The error string in resources used when the call was rejected due to
     * insecure media.
     */
    private static final String INSECURE_MEDIA_REJECTED =
        "service.protocol.media.INSECURE_MEDIA_REJECTED";

    /**
     * The resources service.
     */
    private ResourceManagementService mResources = SipActivator.getResources();

    /**
     * The last ( and maybe only ) session description that we generated for
     * our own media.
     */
    private SessionDescription localSess = null;

    /**
     * A <tt>URL</tt> pointing to a location with call information or a call
     * control web interface related to the <tt>CallPeer</tt> that we are
     * associated with.
     */
    private URL callInfoURL = null;

    /**
     * A temporarily single transport manager that we use for generating
     * addresses until we properly implement both ICE and Raw UDP managers.
     */
    private TransportManagerSipImpl transportManager = null;

    /**
     * Whether other party is able to change video quality settings.
     * Normally its whether we have detected existence of imageattr in sdp.
     */
    boolean supportQualityControls;

    /**
     * The current quality controls for this peer media handler if any.
     */
    private QualityControlWrapper qualityControls;

    /**
     * The lock we use to make sure that we won't be processing a second
     * offer/answer exchange
     */
    private final Object offerAnswerLock = new Object();

    /**
     * Creates a new handler that will be managing media streams for
     * <tt>peer</tt>.
     *
     * @param peer that <tt>CallPeerSipImpl</tt> instance that we will be
     * managing media for.
     */
    public CallPeerMediaHandlerSipImpl(CallPeerSipImpl peer)
    {
        super(peer, peer);

        if (transportManager == null)
        {
            transportManager = new TransportManagerSipImpl(peer);
        }

        qualityControls = new QualityControlWrapper(peer);
    }

    /**
     * Creates a session description <tt>String</tt> representing the
     * <tt>MediaStream</tt>s that this <tt>MediaHandler</tt> is prepare to
     * exchange. The offer takes into account user preferences such as whether
     * or not local user would be transmitting video, whether any or all streams
     * are put on hold, etc. The method is also taking into account any previous
     * offers that this handler may have previously issues hence making the
     * newly generated <tt>String</tt> an session creation or a session update
     * offer accordingly.
     *
     * @return an SDP description <tt>String</tt> representing the streams that
     * this handler is prepared to initiate.
     *
     * @throws OperationFailedException if creating the SDP fails for some
     * reason.
     */
    public String createOffer()
        throws OperationFailedException
    {
        if (localSess == null)
            return createFirstOffer().toString();
        else
            return createUpdateOffer(localSess).toString();
    }

    public Content createOfferContent() throws OperationFailedException, ParseException
    {
        String sdpBody = createOffer();
        ContentImpl sdpOfferContent = new ContentImpl(sdpBody);
        ContentTypeHeader sdpContentType = getPeer().getProtocolProvider()
                .getHeaderFactory().createContentTypeHeader("application", "sdp");
        sdpOfferContent.setContentTypeHeader(sdpContentType);

        return sdpOfferContent;
    }

    /**
     * Allocates ports, retrieves supported formats and creates a
     * <tt>SessionDescription</tt>.
     *
     * @return the <tt>String</tt> representation of the newly created
     * <tt>SessionDescription</tt>.
     *
     * @throws OperationFailedException if generating the SDP fails for whatever
     * reason.
     */
    private SessionDescription createFirstOffer()
        throws OperationFailedException
    {
        //Audio Media Description
        Vector<MediaDescription> mediaDescs = createMediaDescriptions();

        // Create the media streams for each possible media description so that
        // when the call is answered, we can send and receive media immediately.
        preCreateMediaStreams(mediaDescs);

        //wrap everything up in a session description
        String userName
            = getPeer().getProtocolProvider().getAccountID().getUserID();

        SessionDescription sDes = SdpUtils.createSessionDescription(
            getTransportManager().getLastUsedLocalHost(), userName, mediaDescs);

        this.localSess = sDes;
        return localSess;
    }

    /**
     * Takes a list of media descriptions and uses it to pre-create the media
     * streams that this call peer may need during the call.  This reduces
     * media cut through since the media stack will have been created before
     * it is required.
     *
     * @param mediaDescriptions the descriptions to consider
     */
    private void preCreateMediaStreams(List<MediaDescription> mediaDescriptions)
    {
        logger.debug("preCreateMediateStreams");
        long startTime = System.currentTimeMillis();
        List<AnalyticsParameter> parameters = new ArrayList<>();

        MediaDescription mediaDescription = null;
        MediaType mediaType = null;

        // Only care about the pre-creating the audio streams.
        for (MediaDescription description : mediaDescriptions)
        {
            mediaType = SdpUtils.getMediaType(description);
            if (mediaType.equals(MediaType.AUDIO))
            {
                mediaDescription = description;
                break;
            }
        }

        if (mediaDescription == null)
        {
            // Nothing to do
            logger.info("No audio media description");
        }
        else
        {
            MediaDevice dev = getDefaultDevice(mediaType);

            List<RTPExtension> remoteRTPExtensions = SdpUtils.extractRTPExtensions(
                                  mediaDescription, getRtpExtensionsRegistry());
            List<RTPExtension> supportedExtensions = getExtensionsForType(mediaType);
            List<RTPExtension> rtpExtensions = intersectRTPExtensions(
                                      remoteRTPExtensions, supportedExtensions);

            List<MediaFormat> supportedFormats = SdpUtils.extractFormats(
                                    mediaDescription, getDynamicPayloadTypes());

            try
            {
                StreamConnector connector =
                            getTransportManager().getStreamConnector(mediaType);

                for (MediaFormat fmt : supportedFormats)
                {
                    logger.debug("Precreating for format " + fmt);
                    long fmtStartTime = System.currentTimeMillis();
                    preCreateMediaStream(connector,
                                         dev,
                                         fmt,
                                         null,
                                         MediaDirection.SENDRECV,
                                         rtpExtensions,
                                         true);

                    long duration = System.currentTimeMillis() - fmtStartTime;
                    logger.debug("Format " + fmt + " took " + duration + "ms");

                    String fmtName = fmt.getEncoding() + "/" +
                                     fmt.getClockRateString();
                    parameters.add(new AnalyticsParameterSimple(fmtName,
                                                     String.valueOf(duration)));
                }
            }
            catch (OperationFailedException e)
            {
                // Not the end of the world - we'll create the streams later when
                // we need them.
                logger.info("Exception precreating connector.", e);
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.debug("preCreateMediateStreams, took " + duration + "ms");

        if (mediaDescription != null)
        {
            AnalyticsService analytics = ProtocolMediaActivator.getAnalyticsService();
            parameters.add(
                new AnalyticsParameterSimple("total", String.valueOf(duration)));
            analytics.onEvent(AnalyticsEventType.MEDIA_PRECREATE, parameters);
        }
    }

    /**
     * Creates a <tt>Vector</tt> containing the <tt>MediaDescription</tt>s of
     * streams that this handler is prepared to initiate depending on available
     * <tt>MediaDevice</tt>s and local on-hold and video transmission
     * preferences.
     *
     * @return a <tt>Vector</tt> containing the <tt>MediaDescription</tt>s of
     * streams that this handler is prepared to initiate.
     *
     * @throws OperationFailedException if we fail to create the descriptions
     * for reasons like - problems with device interaction, allocating ports,
     * etc.
     */
    private Vector<MediaDescription> createMediaDescriptions()
        throws OperationFailedException
    {
        logger.debug("Creating media descriptions");
        //Audio Media Description
        Vector<MediaDescription> mediaDescs = new Vector<>();

        QualityPreset sendQualityPreset = null;
        QualityPreset receiveQualityPreset = null;

        if(qualityControls != null)
        {
            // the one we will send is the one the other part has announced
            // as receive
            sendQualityPreset = qualityControls.getRemoteReceivePreset();
            // the one we want to receive is the setting that remote
            // can send
            receiveQualityPreset = qualityControls.getRemoteSendMaxPreset();
        }

        for (MediaType mediaType : MediaType.values())
        {
            logger.debug("Looking at " + mediaType);

            if (mediaType == MediaType.VIDEO &&
                !isLocalVideoTransmissionEnabled() &&
                getStream(MediaType.VIDEO) == null)
            {
                // Media type is video but video transmission is not enabled -
                // skip adding the codec info.
                logger.debug("Skip video codecs");
                continue;
            }

            MediaDevice dev = getDefaultDevice(mediaType);

            if (!isDeviceActive(dev, sendQualityPreset, receiveQualityPreset))
                continue;

            MediaDirection direction = dev.getDirection().and(
                            getDirectionUserPreference(mediaType));

            if(isLocallyOnHold())
                direction = direction.and(MediaDirection.SENDONLY);

            // For video interop, do not send a=recvonly/sendonly/inactive SDP
            if (mediaType == MediaType.VIDEO)
                direction = MediaDirection.SENDRECV;

            if(direction != MediaDirection.INACTIVE)
            {
                boolean hadSavp = false;
                for (String profileName : getRtpTransports())
                {
                    logger.debug("Profile name= " + profileName);

                    List<MediaFormat> supportedFormats =
                        getLocallySupportedFormats(dev,
                                                   sendQualityPreset,
                                                   receiveQualityPreset);

                    // For video, we only send a single format on our offer as
                    // we only want to offer our preferred video resolution,
                    // which implicitly means we support lower resolutions.
                    //
                    // Note: CFS will strip any additional ones so it just
                    // confuses things.
                    if ((mediaType == MediaType.VIDEO) &&
                        (supportedFormats.size() > 1))
                    {
                        supportedFormats = supportedFormats.subList(0, 1);
                    }

                    logger.debug("Creating media desc" +
                        ": supportedFormats=" + supportedFormats +
                        ", extensions=" + dev.getSupportedExtensions());

                    logger.debug("Offering formats: " + supportedFormats);
                    MediaDescription md =
                        createMediaDescription(
                            profileName,
                            supportedFormats,
                            getTransportManager().getStreamConnector(mediaType),
                            direction,
                            dev.getSupportedExtensions());

                    try
                    {
                        switch (mediaType)
                        {
                        case AUDIO:
                            /*
                             * Let the remote peer know that we support RTCP XR
                             * in general and VoIP Metrics Report Block in
                             * particular.
                             */
                             logger.debug("Setting SDP Attribute to " +
                                   RTCPExtendedReport.VoIPMetricsReportBlock.SDP_PARAMETER);

                             md.setAttribute(
                                        RTCPExtendedReport.SDP_ATTRIBUTE,
                                        RTCPExtendedReport.VoIPMetricsReportBlock.SDP_PARAMETER);

                            break;
                        case VIDEO:
                            // If we have a video preset, let's send info about
                            // the desired frame rate.
                            if (receiveQualityPreset != null)
                            {
                                // doing only int frame rate for now
                                int frameRate
                                    = (int) receiveQualityPreset.getFameRate();

                                if (frameRate > 0)
                                {
                                    md.setAttribute(
                                            "framerate",
                                            String.valueOf(frameRate));
                                }
                            }
                            break;
                        default:
                            break;
                        }
                    }
                    catch(SdpException e)
                    {
                        // do nothing in case of error.
                        logger.debug("sdp error", e);
                    }

                    if(!hadSavp)
                    {
                        updateMediaDescriptionForSDes(mediaType, md, null);
                    }

                    mediaDescs.add(md);

                    if(!hadSavp && profileName.contains("SAVP"))
                        hadSavp = true;
                }
            }
        }

        //fail if all devices were inactive
        if(mediaDescs.isEmpty())
        {
            ProtocolProviderServiceSipImpl
                .throwOperationFailedException(
                    mResources.getI18NString(NO_AUDIO_VIDEO_DEVICES),
                    OperationFailedException.GENERAL_ERROR,
                    null,
                    logger);
        }

        return mediaDescs;
    }

    /**
     * Creates a <tt>SessionDescription</tt> meant to update a previous offer
     * (<tt>sdescToUpdate</tt>) so that it would reflect the current state (e.g.
     * on-hold and local video transmission preferences) of this
     * <tt>MediaHandler</tt>.
     *
     * @param sdescToUpdate the <tt>SessionDescription</tt> that we are going to
     * update.
     *
     * @return the newly created <tt>SessionDescription</tt> meant to update
     * <tt>sdescToUpdate</tt>.
     *
     * @throws OperationFailedException in case creating the new description
     * fails for some reason.
     */
    private SessionDescription createUpdateOffer(
                                        SessionDescription sdescToUpdate)
        throws OperationFailedException

    {
        //create the media descriptions reflecting our current state.
        Vector<MediaDescription> newMediaDescs = createMediaDescriptions();

        SessionDescription newOffer = SdpUtils.createSessionUpdateDescription(
            sdescToUpdate, getTransportManager().getLastUsedLocalHost(),
            newMediaDescs);

        this.localSess = newOffer;
        return newOffer;
    }

    /**
     * Parses <tt>offerString</tt>, creates the <tt>MediaStream</tt>s that it
     * describes and constructs a response representing the state of this
     * <tt>MediaHandler</tt>. The method takes into account the presence or
     * absence of previous negotiations and interprets the <tt>offerString</tt>
     * as an initial offer or a session update accordingly.
     *
     * @param offerString The SDP offer that we'd like to parse, handle and get
     * a response for.
     *
     * @return A <tt>String</tt> containing the SDP response representing the
     * current state of this <tt>MediaHandler</tt>.
     *
     * @throws OperationFailedException if parsing or handling
     * <tt>offerString</tt> fails or we have a problem while creating the
     * response.
     * @throws IllegalArgumentException if there's a problem with the format
     * or semantics of the <tt>offerString</tt>.
     */
    public String processOffer(String offerString)
        throws OperationFailedException,
               IllegalArgumentException
    {
        SessionDescription offer = SdpUtils.parseSdpString(offerString);

        synchronized (offerAnswerLock)
        {
            if (localSess == null)
                return processFirstOffer(offer).toString();
            else
                return processUpdateOffer(offer, localSess).toString();
        }
    }

    /**
     * Called when an INVITE is first received.  This method creates the media
     * streams that will be required if the end user accepts the call.  This
     * reduces the media cut through time since the media streams are available
     * as soon as they are required.
     *
     * @param offerString The SDP offer that we've received.
     */
    public void preProcessOffer(String offerString)
    {
        SessionDescription offer = SdpUtils.parseSdpString(offerString);

        try
        {
            Vector<MediaDescription> answerDescriptions =
                                 createMediaDescriptionsForAnswer(offer, false);
            preCreateMediaStreams(answerDescriptions);
        }
        catch (IllegalArgumentException | OperationFailedException e)
        {
            // Not important - we'll try again when (if) the user answers the call
            logger.info("Exception trying to preProcessOffer", e);
        }
    }

    /**
     * Parses and handles the specified <tt>SessionDescription offer</tt> and
     * returns and SDP answer representing the current state of this media
     * handler. This method MUST only be called when <tt>offer</tt> is the
     * first session description that this <tt>MediaHandler</tt> is seeing.
     *
     * @param offer the offer that we'd like to parse, handle and get an SDP
     * answer for.
     * @return the SDP answer reflecting the current state of this
     * <tt>MediaHandler</tt>
     *
     * @throws OperationFailedException if we have a problem satisfying the
     * description received in <tt>offer</tt> (e.g. failed to open a device or
     * initialize a stream ...).
     * @throws IllegalArgumentException if there's a problem with
     * <tt>offer</tt>'s format or semantics.
     */
    private SessionDescription processFirstOffer(SessionDescription offer)
        throws OperationFailedException,
               IllegalArgumentException
    {
        logger.entry();
        Vector<MediaDescription> answerDescriptions
            = createMediaDescriptionsForAnswer(offer, true);

        //wrap everything up in a session description
        SessionDescription answer = SdpUtils.createSessionDescription(
            getTransportManager().getLastUsedLocalHost(), getUserName(),
            answerDescriptions);

        this.localSess = answer;
        logger.exit();

        return localSess;
    }

    /**
     * Parses, handles <tt>newOffer</tt>, and produces an update answer
     * representing the current state of this <tt>MediaHandler</tt>.
     *
     * @param newOffer the new SDP description that we are receiving as an
     * update to our current state.
     * @param previousAnswer the <tt>SessionDescripiton</tt> that we last sent
     * as an answer to the previous offer currently updated by
     * <tt>newOffer</tt> is updating.
     *
     * @return an answer that updates <tt>previousAnswer</tt>.
     *
     * @throws OperationFailedException if we have a problem initializing the
     * <tt>MediaStream</tt>s as suggested by <tt>newOffer</tt>
     * @throws IllegalArgumentException if there's a problem with the syntax
     * or semantics of <tt>newOffer</tt>.
     */
    private SessionDescription processUpdateOffer(
                                           SessionDescription newOffer,
                                           SessionDescription previousAnswer)
        throws OperationFailedException,
               IllegalArgumentException
    {
        logger.entry();
        Vector<MediaDescription> answerDescriptions
            = createMediaDescriptionsForAnswer(newOffer, true);

        // wrap everything up in a session description
        SessionDescription newAnswer = SdpUtils.createSessionUpdateDescription(
                previousAnswer, getTransportManager().getLastUsedLocalHost(),
                answerDescriptions);

        this.localSess = newAnswer;
        logger.exit();

        return localSess;
    }

    /**
     * Creates a number of <tt>MediaDescription</tt>s answering the descriptions
     * offered by the specified <tt>offer</tt> and reflecting the state of this
     * <tt>MediaHandler</tt>.
     *
     * @param offer the offer that we'd like the newly generated session
     * descriptions to answer.
     * @param createMedia if true then the media streams required will be created
     *
     * @return a <tt>Vector</tt> containing the <tt>MediaDescription</tt>s
     * answering those provided in the <tt>offer</tt>.
     * @throws OperationFailedException if there's a problem handling the
     * <tt>offer</tt>
     * @throws IllegalArgumentException if there's a problem with the syntax
     * or semantics of <tt>newOffer</tt>.
     */
    private Vector<MediaDescription> createMediaDescriptionsForAnswer(
                    SessionDescription offer,
                    boolean createMedia)
        throws OperationFailedException,
               IllegalArgumentException
    {
        List<MediaDescription> remoteDescriptions = SdpUtils
                        .extractMediaDescriptions(offer);

        // prepare to generate answers to all the incoming descriptions
        Vector<MediaDescription> answerDescriptions
            = new Vector<>(remoteDescriptions.size());

        this.setCallInfoURL(SdpUtils.getCallInfoURL(offer));

        boolean atLeastOneValidDescription = false;
        boolean rejectedAvpOfferDueToSavpRequired = false;

        // Flag to track whether we have found any valid media devices.
        boolean mediaDeviceFound = false;

        boolean encryptionEnabled = getPeer()
            .getProtocolProvider()
            .getAccountID()
            .getAccountPropertyBoolean(
                ProtocolProviderFactory.DEFAULT_ENCRYPTION, true);
        int savpOption = getPeer()
            .getProtocolProvider()
            .getAccountID()
            .getAccountPropertyInt(ProtocolProviderFactory.SAVP_OPTION,
                ProtocolProviderFactory.SAVP_OFF);

        boolean masterStreamSet = false;
        List<MediaType> seenMediaTypes = new ArrayList<>();
        for (MediaDescription mediaDescription : remoteDescriptions)
        {
            String transportProtocol;
            try
            {
                transportProtocol = mediaDescription.getMedia().getProtocol();
            }
            catch (SdpParseException e)
            {
                throw new OperationFailedException(
                    "unable to create the media description",
                    OperationFailedException.ILLEGAL_ARGUMENT, e);
            }

            //ignore RTP/AVP(F) stream when RTP/SAVP(F) is mandatory
            if (savpOption == ProtocolProviderFactory.SAVP_MANDATORY
                && !(transportProtocol.equals("RTP/SAVP")
                    || transportProtocol.equals("RTP/SAVPF"))
                && encryptionEnabled)
            {
                rejectedAvpOfferDueToSavpRequired = true;
                continue;
            }

            MediaType mediaType = null;
            try
            {
                mediaType = SdpUtils.getMediaType(mediaDescription);
                //don't process a second media of the same type
                if(seenMediaTypes.contains(mediaType))
                    continue;
                seenMediaTypes.add(mediaType);
            }
            catch (IllegalArgumentException iae)
            {
                //remote party offers a stream of a type that we don't support.
                //we'll disable it and move on.
                answerDescriptions.add(
                        SdpUtils.createDisablingAnswer(mediaDescription));
                continue;
            }

            List<MediaFormat> remoteFormats = SdpUtils.extractFormats(
                mediaDescription, getDynamicPayloadTypes());

            MediaDevice dev = getDefaultDevice(mediaType);
            MediaDirection effectiveDirection;

            // If the default media device is not null and its media format
            // is also not null, we know we have found a valid media device,
            // so set the flag. If it is null, just continue - the alternative
            // is hitting a NullPointerException
            //
            // We don't currently check the media format.
            // - The implementation is dumb in that it creates and then destroys
            //   a capture device, just to get the supported formats
            // - Though in theory that's fine, in practice we've seen this cause
            //   problems with hi-def video (SFR452618)
            // - The only purpose of mediaDeviceFound is to produce a different
            //   user-visible error string - and we don't want the user to see
            //   that anyway.
            // - Checking the media format could cause an exception, which we were
            //   just ignoring anyway.
            if (dev == null)
            {
                logger.warn("Default device for " + mediaType + " is null");
                continue;
            }

            MediaDirection devDirection = dev.getDirection();

            MediaDirection userDirection = getDirectionUserPreference(mediaType);

            // Take the preference of the user with respect to streaming
            // mediaType into account.
            effectiveDirection = devDirection.and(userDirection);

            if (effectiveDirection == MediaDirection.INACTIVE)
            {
                logger.warn(" Media is inactive for device: " + dev +
                            " and media type: " + mediaType +
                            " with user: " + userDirection +
                            " and device: " + devDirection);
                continue;
            }

            mediaDeviceFound = true;

            // determine the direction that we need to announce.
            MediaDirection remoteDirection
                = SdpUtils.getDirection(mediaDescription);
            MediaDirection direction
                = effectiveDirection.getDirectionForAnswer(remoteDirection);

            // take into account the direction previously set/sent
            // to change directions properly, this is in case
            // where we set a direction and the other side don't agree with us
            // we need to be in the state we have offered
            if (isLocallyOnHold())
                direction = direction.and(MediaDirection.SENDONLY);

            List<MediaFormat> mutuallySupportedFormats;
            if (mediaType.equals(MediaType.VIDEO) &&
                    qualityControls != null)
            {
                mutuallySupportedFormats = intersectFormats(
                        remoteFormats,
                        getLocallySupportedFormats(
                                dev,
                                qualityControls.getRemoteReceivePreset(),
                                qualityControls.getRemoteSendMaxPreset()));
            }
            else
            {
                mutuallySupportedFormats = intersectFormats(
                        remoteFormats,
                        getLocallySupportedFormats(dev));
            }

            // stream target
            MediaStreamTarget target
                = SdpUtils.extractDefaultTarget(mediaDescription, offer);
            int targetDataPort = target.getDataAddress().getPort();

            // If this call is not allowed to receive remote video then set
            // the port to 0 so the offer is refused.
            if (mediaType.equals(MediaType.VIDEO) &&
                !isRemoteVideoTransmissionEnabled())
            {
                targetDataPort = 0;
            }

            if ((mutuallySupportedFormats == null)
                || mutuallySupportedFormats.isEmpty()
                || (targetDataPort == 0))
            {
                logger.warn(" Formats: " + mutuallySupportedFormats +
                            " Target data port: " + targetDataPort);

                // mark stream as dead and go on bravely
                answerDescriptions.add(
                        SdpUtils.createDisablingAnswer(mediaDescription));

                //close the stream in case it already exists
                closeStream(mediaType);
                continue;
            }

            // check whether we will be exchanging any RTP extensions.
            List<RTPExtension> offeredRTPExtensions
                    = SdpUtils.extractRTPExtensions(
                            mediaDescription, this.getRtpExtensionsRegistry());
            List<RTPExtension> supportedExtensions
                    = getExtensionsForType(mediaType);
            List<RTPExtension> rtpExtensions
                = intersectRTPExtensions(
                        offeredRTPExtensions,
                        supportedExtensions);

            StreamConnector connector
                = getTransportManager().getStreamConnector(mediaType);

            // check for options from remote party and set them locally
            if (mediaType.equals(MediaType.VIDEO))
            {
                // update stream
                MediaStream stream = getStream(MediaType.VIDEO);

                if(stream != null && dev != null)
                {
                    List<MediaFormat> fmts = intersectFormats(
                        getLocallySupportedFormats(dev),
                        remoteFormats);

                    if(fmts.size() > 0)
                    {
                        MediaFormat fmt = fmts.get(0);

                        ((VideoMediaStream)stream).updateQualityControl(
                            fmt.getAdvancedAttributes());
                    }
                }

                supportQualityControls =
                    SdpUtils.containsAttribute(mediaDescription, "imageattr");

                float frameRate = -1;
                // check for frame rate setting
                try
                {
                    String frStr = mediaDescription.getAttribute("framerate");
                    if(frStr != null)
                        frameRate = Integer.parseInt(frStr);
                }
                catch (SdpParseException e)
                {
                    // do nothing
                }

                if (frameRate > 0)
                {
                    qualityControls.setMaxFrameRate(frameRate);
                }
            }

            // For video, although we want to set our media stream to the
            // remote format (so we send the requested video resolution), the
            // format we want to advertise is our preferred video format so the
            // remote end knows what we support receiving.  Get our preferred
            // format here.
            List<MediaFormat> formatsForMD = mutuallySupportedFormats;
            if (mediaType.equals(MediaType.VIDEO))
            {
                formatsForMD = getLocallySupportedFormats(dev);
                if (formatsForMD.size() > 0)
                {
                    logger.debug("Set local video format to best available: " +
                        formatsForMD.get(0));
                    formatsForMD = formatsForMD.subList(0, 1);
                }
            }

            // Build our SDP media description
            MediaDescription md
                = createMediaDescription(
                        transportProtocol,
                        mutuallySupportedFormats,
                        connector,
                        direction,
                        rtpExtensions);

            // Sets ZRTP or SDES, depending on the preferences for this account.
            setAndAddPreferredEncryptionProtocol(
                    mediaType,
                    md,
                    mediaDescription);

            /*
             * We support the receiving of RTCP XR so we will answer the
             * offer of the remote peer.
             */
            try
            {
                logger.debug("Setting SDP Attribute to " +
                             RTCPExtendedReport.VoIPMetricsReportBlock.SDP_PARAMETER);
                md.setAttribute(RTCPExtendedReport.SDP_ATTRIBUTE,
                                RTCPExtendedReport.VoIPMetricsReportBlock.SDP_PARAMETER);
            }
            catch(SdpException e)
            {
                // do nothing in case of error.
                logger.debug("sdp error", e);
            }

            // create the corresponding stream...
            MediaFormat fmt = findMediaFormat(remoteFormats,
                    mutuallySupportedFormats.get(0));

            boolean masterStream = false;
            // if we have more than one stream, lets the audio be the master
            if (!masterStreamSet)
            {
                if (remoteDescriptions.size() > 1)
                {
                    if(mediaType.equals(MediaType.AUDIO))
                    {
                        masterStream = true;
                        masterStreamSet = true;
                    }
                }
                else
                {
                    masterStream = true;
                    masterStreamSet = true;
                }
            }

            if (createMedia)
            {
                logger.debug("Creating media stream");
                MediaStream stream = fetchPreCreatedStream(fmt);

                if (stream == null)
                {
                    stream = initStream(connector,
                                        dev,
                                        fmt,
                                        target,
                                        direction,
                                        rtpExtensions,
                                        masterStream);
                }
                else
                {
                    // Update the stream with the information that was lacking
                    // when it was pre-created
                    stream.setTarget(target);
                    setStream(mediaType, stream);
                }

                // RTCP XR
                if (stream != null)
                {
                    logger.debug("Setting SDP Attribute to " +
                                 RTCPExtendedReport.VoIPMetricsReportBlock.SDP_PARAMETER);
                    stream.setProperty(RTCPExtendedReport.SDP_ATTRIBUTE,
                                       RTCPExtendedReport.VoIPMetricsReportBlock.SDP_PARAMETER);
                }
            }

            // create the answer description
            answerDescriptions.add(md);

            atLeastOneValidDescription = true;
        }

        if (!atLeastOneValidDescription)
        {
            String errorMessage;

            if (rejectedAvpOfferDueToSavpRequired)
            {
                // The offer was rejected due to insecure media, so set the
                // error message to reflect that.
                errorMessage =
                    mResources.getI18NString(INSECURE_MEDIA_REJECTED);
            }
            else if (!mediaDeviceFound)
            {
                // The call was rejected because we didn't find any media
                // devices, so set the error message to reflect that.
                errorMessage =
                    mResources.getI18NString(NO_AUDIO_VIDEO_DEVICES);
            }
            else
            {
                // In all other cases, the call was rejected because no valid
                // media descriptions were found, so set the error message to
                // reflect that.
                errorMessage =
                    mResources.getI18NString(NO_VALID_MEDIA_DESCS);
            }

            ProtocolProviderServiceSipImpl.throwOperationFailedException(
                errorMessage,
                OperationFailedException.ILLEGAL_ARGUMENT,
                null,
                logger);
        }

        return answerDescriptions;
    }

    /**
     * Updates the supplied description with SDES attributes if necessary.
     *
     * @param mediaType the media type.
     * @param localMd the description of the local peer.
     * @param remoteMd the description of the remote peer.
     *
     * @return True if SDES is added tp the media description. False, otherwise.
     */
    private boolean updateMediaDescriptionForSDes(
            MediaType mediaType,
            MediaDescription localMd,
            MediaDescription remoteMd)
    {
        AccountID accountID = getPeer().getProtocolProvider().getAccountID();

        // check if SDES and encryption is enabled at all
        if(!accountID.isEncryptionProtocolEnabled("SDES")
                || !accountID.getAccountPropertyBoolean(
                        ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                        true))
        {
            return false;
        }

        // get or create the control
        Map<MediaTypeSrtpControl, SrtpControl> srtpControls = getSrtpControls();
        MediaTypeSrtpControl key
            = new MediaTypeSrtpControl(mediaType, SrtpControlType.SDES);
        SrtpControl scontrol = srtpControls.get(key);

        if (scontrol == null)
        {
            scontrol = SipActivator.getMediaService().createSDesControl();
            srtpControls.put(key, scontrol);
        }

        // set the enabled ciphers suites
        SDesControl sdcontrol = (SDesControl) scontrol;
        String ciphers
            = accountID.getAccountPropertyString(
                    ProtocolProviderFactory.SDES_CIPHER_SUITES);

        if (ciphers == null)
        {
            ciphers =
                SipActivator.getResources().getSettingsString(
                    SDesControl.SDES_CIPHER_SUITES);
        }
        sdcontrol.setEnabledCiphers(Arrays.asList(ciphers.split(",")));

        // act as initiator
        if (remoteMd == null)
        {
            Vector<Attribute> atts = localMd.getAttributes(true);

            for(SrtpCryptoAttribute ca:
                    sdcontrol.getInitiatorCryptoAttributes())
                atts.add(SdpUtils.createAttribute("crypto", ca.encode()));

            return true;
        }
        // act as responder
        else
        {
            SrtpCryptoAttribute localAttr
                = selectSdesCryptoSuite(false, sdcontrol, remoteMd);

            if (localAttr != null)
            {
                try
                {
                    localMd.setAttribute("crypto", localAttr.encode());
                    return true;
                }
                catch (SdpException e)
                {
                    logger.error("unable to add crypto to answer", e);
                }
            }
            else
            {
                // none of the offered suites match, destroy the sdes
                // control
                sdcontrol.cleanup();
                srtpControls.remove(key);
                logger.warn("Received unsupported sdes crypto attribute.");
            }
            return false;
        }
    }

    private List<String> getRtpTransports() throws OperationFailedException
    {
        List<String> result = new ArrayList<>(2);
        int savpOption = ProtocolProviderFactory.SAVP_OFF;
        if(getPeer()
            .getProtocolProvider()
            .getAccountID()
            .getAccountPropertyBoolean(
                ProtocolProviderFactory.DEFAULT_ENCRYPTION, true))
        {
            savpOption = getPeer()
                .getProtocolProvider()
                .getAccountID()
                .getAccountPropertyInt(
                    ProtocolProviderFactory.SAVP_OPTION,
                    ProtocolProviderFactory.SAVP_OFF);
        }
        if(savpOption == ProtocolProviderFactory.SAVP_MANDATORY)
            result.add("RTP/SAVP");
        else if(savpOption == ProtocolProviderFactory.SAVP_OFF)
            result.add(SdpConstants.RTP_AVP);
        else if(savpOption == ProtocolProviderFactory.SAVP_OPTIONAL)
        {
            result.add("RTP/SAVP");
            result.add(SdpConstants.RTP_AVP);
        }
        else
            throw new OperationFailedException("invalid value for SAVP_OPTION",
                OperationFailedException.GENERAL_ERROR);
        return result;
    }

    /**
     * Handles the specified <tt>answer</tt> by creating and initializing the
     * corresponding <tt>MediaStream</tt>s.
     *
     * @param answer the SDP answer that we'd like to handle.
     *
     * @throws OperationFailedException if we fail to handle <tt>answer</tt> for
     * reasons like failing to initialize media devices or streams.
     * @throws IllegalArgumentException if there's a problem with the syntax or
     * the semantics of <tt>answer</tt>.
     */
    public void processAnswer(String answer)
        throws OperationFailedException,
               IllegalArgumentException
    {
        logger.entry();
        processAnswer(SdpUtils.parseSdpString(answer));
        logger.exit();
    }

    /**
     * Handles the specified <tt>answer</tt> by creating and initializing the
     * corresponding <tt>MediaStream</tt>s. This method basically just adds
     * synchronisation on top of {@link #doNonSynchronisedProcessAnswer(
     * SessionDescription)}
     *
     * @param answer the SDP <tt>SessionDescription</tt>.
     *
     * @throws OperationFailedException if we fail to handle <tt>answer</tt> for
     * reasons like failing to initialize media devices or streams.
     * @throws IllegalArgumentException if there's a problem with the syntax or
     * the semantics of <tt>answer</tt>. Method is synchronized in order to
     * avoid closing mediaHandler when we are currently in process of
     * initializing, configuring and starting streams and anybody interested
     * in this operation can synchronize to the mediaHandler instance to wait
     * processing to stop (method setState in CallPeer).
     */
    private void processAnswer(SessionDescription answer)
        throws OperationFailedException,
               IllegalArgumentException
    {
        logger.debug("Waiting for offerAnswerLock to process SDP answer.");
        synchronized (offerAnswerLock)
        {
            logger.debug("Obtained offerAnswerLock. Processing SDP answer.");
            doNonSynchronisedProcessAnswer(answer);
        }

        logger.debug("Released offerAnswerLock after processing SDP answer.");
    }

    /**
     * Handles the specified <tt>answer</tt> by creating and initializing the
     * corresponding <tt>MediaStream</tt>s.
     *
     * @param answer the SDP <tt>SessionDescription</tt>.
     *
     * @throws OperationFailedException if we fail to handle <tt>answer</tt> for
     * reasons like failing to initialize media devices or streams.
     * @throws IllegalArgumentException if there's a problem with the syntax or
     * the semantics of <tt>answer</tt>. Method is synchronized in order to
     * avoid closing mediaHandler when we are currently in process of
     * initializing, configuring and starting streams and anybody interested
     * in this operation can synchronize to the mediaHandler instance to wait
     * processing to stop (method setState in CallPeer).
     */
    private void doNonSynchronisedProcessAnswer(SessionDescription answer)
            throws OperationFailedException,
                   IllegalArgumentException
    {
        List<MediaDescription> remoteDescriptions
            = SdpUtils.extractMediaDescriptions(answer);

        this.setCallInfoURL(SdpUtils.getCallInfoURL(answer));

        boolean masterStreamSet = false;
        List<MediaType> seenMediaTypes = new ArrayList<>();
        for (MediaDescription mediaDescription : remoteDescriptions)
        {
            logger.debug("Looking at " + mediaDescription);
            MediaType mediaType;
            try
            {
                mediaType = SdpUtils.getMediaType(mediaDescription);
                //don't process a second media of the same type
                if(seenMediaTypes.contains(mediaType))
                    continue;
                seenMediaTypes.add(mediaType);
            }
            catch(IllegalArgumentException iae)
            {
                logger.info("Remote party added to answer a media type that " +
                        "we don't understand. Ignoring stream.");
                continue;
            }

            //stream target
            MediaStreamTarget target
                = SdpUtils.extractDefaultTarget(mediaDescription, answer);

            // not target port - try next media description
            if(target.getDataAddress().getPort() == 0)
            {
                closeStream(mediaType);
                continue;
            }

            List<MediaFormat> supportedFormats = SdpUtils.extractFormats(
                mediaDescription, getDynamicPayloadTypes());

            MediaDevice dev = getDefaultDevice(mediaType);

            if(!isDeviceActive(dev))
            {
                closeStream(mediaType);
                continue;
            }

            MediaDirection devDirection
                = (dev == null) ? MediaDirection.INACTIVE : dev.getDirection();

            // Take the preference of the user with respect to streaming
            // mediaType into account.
            devDirection
                = devDirection.and(getDirectionUserPreference(mediaType));

            if (supportedFormats.isEmpty())
            {
                //remote party must have messed up our SDP. throw an exception.
                ProtocolProviderServiceSipImpl.throwOperationFailedException(
                    "Remote party sent an invalid SDP answer. The codecs in " +
                            "the answer are either not present or not " +
                            "supported",
                     OperationFailedException.ILLEGAL_ARGUMENT, null, logger);
            }

            StreamConnector connector
                = getTransportManager().getStreamConnector(mediaType);

            //determine the direction that we need to announce.
            MediaDirection remoteDirection
                = SdpUtils.getDirection(mediaDescription);

            MediaDirection direction
                = devDirection.getDirectionForAnswer(remoteDirection);

            // take into account and the direction previously set/sent
            // to change directions properly, this is in case
            // where we set a direction and the other side don't agree with us
            // we need to be in the state we have offered
            if(isLocallyOnHold())
                direction = direction.and(MediaDirection.SENDONLY);

            // update the RTP extensions that we will be exchanging.
            List<RTPExtension> remoteRTPExtensions
                    = SdpUtils.extractRTPExtensions(
                            mediaDescription, getRtpExtensionsRegistry());

            List<RTPExtension> supportedExtensions
                    = getExtensionsForType(mediaType);

            List<RTPExtension> rtpExtensions = intersectRTPExtensions(
                            remoteRTPExtensions, supportedExtensions);

            // check for options from remote party and set
            // is quality controls supported
            if(mediaType.equals(MediaType.VIDEO))
            {
                supportQualityControls =
                    SdpUtils.containsAttribute(mediaDescription, "imageattr");
            }

            // select the crypto key the peer has chosen from our proposal
            Map<MediaTypeSrtpControl, SrtpControl> srtpControls
                = getSrtpControls();
            MediaTypeSrtpControl key =
                new MediaTypeSrtpControl(mediaType, SrtpControlType.SDES);
            SrtpControl scontrol = srtpControls.get(key);

            if(scontrol != null)
            {
                if(selectSdesCryptoSuite(
                            true,
                            (SDesControl) scontrol,
                            mediaDescription) == null)
                {
                    scontrol.cleanup();
                    srtpControls.remove(key);
                    logger.warn("Received unsupported sdes crypto attribute.");
                }
                else
                {
                    //found an SDES answer, remove all other controls
                    Iterator<Map.Entry<MediaTypeSrtpControl, SrtpControl>> iter
                        = srtpControls.entrySet().iterator();

                    while (iter.hasNext())
                    {
                        Map.Entry<MediaTypeSrtpControl, SrtpControl> entry
                            = iter.next();
                        MediaTypeSrtpControl mtsc = entry.getKey();

                        if ((mtsc.mediaType == mediaType)
                                && (mtsc.srtpControlType
                                        != SrtpControlType.SDES))
                        {
                            entry.getValue().cleanup();
                            iter.remove();
                        }
                    }

                    addAdvertisedEncryptionMethod(SrtpControlType.SDES);
                }
            }

            boolean masterStream = false;
            // if we have more than one stream, lets the audio be the master
            if(!masterStreamSet)
            {
                if(remoteDescriptions.size() > 1)
                {
                    if(mediaType.equals(MediaType.AUDIO))
                    {
                        masterStream = true;
                        masterStreamSet = true;
                    }
                }
                else
                {
                    masterStream = true;
                    masterStreamSet = true;
                }
            }

            try
            {
                if(mediaDescription.getAttribute(SdpUtils.ZRTP_HASH_ATTR)
                        != null)
                {
                    addAdvertisedEncryptionMethod(SrtpControlType.ZRTP);
                }
            }
            catch (SdpParseException e)
            {
                logger.error("received an unparsable sdp attribute", e);
            }

            // Create the corresponding stream, or use the pre-created one
            MediaFormat format = supportedFormats.get(0);
            MediaStream stream = fetchPreCreatedStream(format);

            if (stream == null)
            {
                logger.debug("No pre-created stream for " + format);
                stream = initStream(
                        connector,
                        dev,
                        supportedFormats.get(0),
                        target,
                        direction,
                        rtpExtensions,
                        masterStream);
            }
            else
            {
                logger.debug("Using precreated stream for " + format);

                // Update the stream with the information that was lacking when
                // the stream was created
                stream.setTarget(target);
                setStream(mediaType, stream);
            }

            // RTCP XR
            if (stream != null)
            {
                logger.debug("Setting SDP Attribute to " +
                             RTCPExtendedReport.VoIPMetricsReportBlock.SDP_PARAMETER);
                stream.setProperty(RTCPExtendedReport.SDP_ATTRIBUTE,
                                   RTCPExtendedReport.VoIPMetricsReportBlock.SDP_PARAMETER);
            }
        }
    }

    /**
     * Returns our own user name so that we could use it when generating SDP o=
     * fields.
     *
     * @return our own user name so that we could use it when generating SDP o=
     * fields.
     */
    private String getUserName()
    {
        return getPeer().getProtocolProvider().getAccountID().getUserID();
    }

    /**
     * Generates an SDP <tt>MediaDescription</tt> for <tt>MediaDevice</tt>
     * taking account the local streaming preference for the corresponding
     * media type.
     *
     * @param transport the profile name (RTP/SAVP or RTP/AVP)
     * @param formats the list of <tt>MediaFormats</tt> that we'd like to
     * advertise.
     * @param connector the <tt>StreamConnector</tt> that we will be using
     * for the stream represented by the description we are creating.
     * @param direction the <tt>MediaDirection</tt> that we'd like to establish
     * the stream in.
     * @param extensions the list of <tt>RTPExtension</tt>s that we'd like to
     * advertise in the <tt>MediaDescription</tt>.
     *
     * @return a newly created <tt>MediaDescription</tt> representing streams
     * that we'd be able to handle.
     *
     * @throws OperationFailedException if generating the
     * <tt>MediaDescription</tt> fails for some reason.
     */
    private MediaDescription createMediaDescription(
                                             String             transport,
                                             List<MediaFormat>  formats,
                                             StreamConnector    connector,
                                             MediaDirection     direction,
                                             List<RTPExtension> extensions )
        throws OperationFailedException
    {
        return SdpUtils.createMediaDescription(transport, formats, connector,
           direction, extensions,
           getDynamicPayloadTypes(), getRtpExtensionsRegistry());
    }

    /**
     * Returns a <tt>URL</tt> pointing ta a location with call control
     * information for this peer or <tt>null</tt> if no such <tt>URL</tt> is
     * available for the <tt>CallPeer</tt> associated with this handler.
     *
     * @return a <tt>URL</tt> link to a location with call information or a
     * call control web interface related to our <tt>CallPeer</tt> or
     * <tt>null</tt> if no such <tt>URL</tt>.
     */
    public URL getCallInfoURL()
    {
        return callInfoURL;
    }

    /**
     * Specifies a <tt>URL</tt> pointing to a location with call control
     * information for this peer.
     *
     * @param callInfoURL a <tt>URL</tt> link to a location with call
     * information or a call control web interface related to the
     * <tt>CallPeer</tt> that we are associated with.
     */
    private void setCallInfoURL(URL callInfoURL)
    {
        this.callInfoURL = callInfoURL;
    }

    /**
     * Returns a reference to the currently valid network address manager
     * service for use by this handler's generic ancestor.
     *
     * @return a reference to the currently valid {@link
     * NetworkAddressManagerService}
     */
    protected NetworkAddressManagerService getNetworkAddressManagerService()
    {
        return SipActivator.getNetworkAddressManagerService();
    }

    /**
     * Returns a reference to the currently valid media service for use by this
     * handler's generic ancestor.
     *
     * @return a reference to the currently valid {@link MediaService}
     */
    protected ConfigurationService getConfigurationService()
    {
        return SipActivator.getConfigurationService();
    }

    /**
     * Returns a reference to the currently valid media service for use by this
     * handler's generic ancestor.
     *
     * @return a reference to the currently valid {@link MediaService}
     */
    protected MediaService getMediaService()
    {
        return SipActivator.getMediaService();
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
    protected void throwOperationFailedException( String    message,
                                                  int       errorCode,
                                                  Throwable cause)
        throws OperationFailedException
    {
        ProtocolProviderServiceSipImpl.throwOperationFailedException(
                        message, errorCode, cause, logger);
    }

    /**
     * Returns the transport manager that is handling our address management.
     *
     * @return the transport manager that is handling our address management.
     */
    protected TransportManagerSipImpl getTransportManager()
    {
        if (transportManager == null)
        {
            transportManager = new TransportManagerSipImpl(this.getPeer());
        }

        return transportManager;
    }

    /**
     * Returns the quality control for video calls if any.
     * @return the implemented quality control.
     */
    public QualityControl getQualityControl()
    {
        if(supportQualityControls)
        {
            return qualityControls;
        }
        else
        {
            // we have detected that its not supported and return null
            // and control ui won't be visible
            return null;
        }
    }

    /**
     * Sometimes as initing a call with custom preset can set and we force
     * that quality controls is supported.
     * @param value whether quality controls is supported.
     */
    public void setSupportQualityControls(boolean value)
    {
        this.supportQualityControls = value;
    }

    /**
     * Returns the selected crypto suite selected.
     *
     * @param isInitiator True if the local call instance is the initiator of
     * the call. False otherwise.
     * @param sDesControl The SDES based SRTP MediaStream encryption control.
     * @param mediaDescription The description received from the
     * remote peer. This contains the SDES crypto suites available for the
     * remote peer.
     *
     * @return The selected SDES crypto suite supported by both the local and
     * the remote peer. Or null, if there is no crypto suite supported by both
     * of the peers.
     */
    protected SrtpCryptoAttribute selectSdesCryptoSuite(
            boolean isInitiator,
            SDesControl sDesControl,
            MediaDescription mediaDescription)
    {
        Vector<Attribute> attrs = mediaDescription.getAttributes(true);
        Vector<SrtpCryptoAttribute> peerAttributes
            = new Vector<>(attrs.size());

        Attribute a;
        for(int i = 0; i < attrs.size(); ++i)
        {
            try
            {
                a = attrs.get(i);
                if (a.getName().equals("crypto"))
                {
                    try
                    {
                        peerAttributes.add(
                            SrtpCryptoAttribute.create(a.getValue()));
                    }
                    catch (IllegalArgumentException e)
                    {
                        // Ignore an invalid cipher suite.
                        //
                        // This branch was added to ignore AES_CM_256_HMAC_SHA1_80,
                        // which is a common typo of AES_256_CM_HMAC_SHA1_80
                        // (CM and 256 inverted).  Perimeta deliberately offers
                        // both to better interwork with bugged clients.
                        // Ironically we need to silently ignore the typo here
                        // because SrtpCryptoAttribute not unreasonably throws
                        // IllegalArgumentException.
                        logger.warn("Ignore unrecognised attr " + i + ", value: " +
                            a.getValue());
                    }
                }
            }
            catch (SdpParseException e)
            {
                logger.error("received an unparsable sdp attribute", e);
            }
        }

        if(isInitiator)
        {
            return sDesControl.initiatorSelectAttribute(peerAttributes);
        }
        else
        {
            return sDesControl.responderSelectAttribute(peerAttributes);
        }
    }

    /**
     * Selects the preferred encryption protocol (only used by the callee).
     *
     * @param mediaType The type of media (AUDIO or VIDEO).
     * @param localMd the description of the local peer.
     * @param remoteMd the description of the remote peer.
     */
    protected void setAndAddPreferredEncryptionProtocol(
            MediaType mediaType,
            MediaDescription localMd,
            MediaDescription remoteMd)
    {
        // Sets ZRTP or SDES, depending on the preferences for this account.
        List<String> preferredEncryptionProtocols = getPeer()
            .getProtocolProvider()
            .getAccountID()
            .getSortedEnabledEncryptionProtocolList();

        if (preferredEncryptionProtocols != null && preferredEncryptionProtocols.size() == 0) {
            logger.error("Preferred encryption protocol(s) were not set for this account!");
        }

        for(int i = 0; i < preferredEncryptionProtocols.size(); ++i)
        {
            if(preferredEncryptionProtocols.get(i).equals(
                        ProtocolProviderFactory.ENCRYPTION_PROTOCOL + ".SDES"))
            {
                if(updateMediaDescriptionForSDes(
                            mediaType,
                            localMd,
                            remoteMd))
                {
                    // Stops once an encryption advertisement has been chosen.
                    return;
                }
            } else {
                String encrProtocol = preferredEncryptionProtocols.get(i);
                logger.error("Unhandled Encryption Protocol was provided: " + encrProtocol);
            }
        }
    }

    /**
     * Starts this <tt>CallPeerMediaHandler</tt>. If it has already been
     * started, does nothing. This method just adds synchronization on top of
     * the one already implemented by {@link CallPeerMediaHandler#start()}.
     *
     * @throws IllegalStateException if this method is called without this
     * handler having first seen a media description or having generated an
     * offer.
     */
    public void start()
        throws IllegalStateException
    {
        logger.debug("Waiting for offerAnswerLock to start CallPeerMediaHandler.");
        synchronized (offerAnswerLock)
        {
            logger.debug("Obtained offerAnswerLock. Starting CallPeerMediaHandler");
            super.start();
        }

        logger.debug("Released offerAnswerLock after starting CallPeerMediaHandler.");
    }
}
