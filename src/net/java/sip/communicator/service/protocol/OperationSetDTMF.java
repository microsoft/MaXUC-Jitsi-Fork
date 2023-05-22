/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol;

import org.jitsi.service.protocol.*;

/**
 * An <tt>OperationSet</tt> that allows other modules to send DMF tones through
 * this protocol provider.
 *
 * @author JM HEITZ
 */
public interface OperationSetDTMF
    extends OperationSet
{
    /**
     * The minimal tone duration value for RFC4733 is 100 ms.
     */
    int DEFAULT_DTMF_MINIMAL_TONE_DURATION = 100;

    /**
     * The maximal tone duration value is -1 in order to stop sending tone only
     * when user requests to stop it.
     */
    int DEFAULT_DTMF_MAXIMAL_TONE_DURATION = -1;

    /**
     * The default tone volume value.
     */
    int DEFAULT_DTMF_TONE_VOLUME = 10;

    /**
     * The name of the <tt>ConfigurationService</tt> <tt>int</tt> property
     * which indicates the minimal duration for a DTMF tone.
     * The default value is 70 ms.
     */
    String PROP_MINIMAL_RTP_DTMF_TONE_DURATION =
        "net.java.sip.communicator.service.protocol.minimalRtpDtmfToneDuration";

    /**
     * The name of the <tt>ConfigurationService</tt> <tt>int</tt> property
     * which indicates the maximal duration for a DTMF tone (in ms).
     * The default value is -1 to tell to stop DTMF tones only following user
     * will.
     */
    String PROP_MAXIMAL_RTP_DTMF_TONE_DURATION =
        "net.java.sip.communicator.service.protocol.maximalRtpDtmfToneDuration";

    /**
     * Sends the <tt>DTMFTone</tt> <tt>tone</tt> to <tt>callPeer</tt>.
     *
     * @param callPeer the  call peer to send <tt>tone</tt> to.
     * @param tone the DTMF tone to send to <tt>callPeer</tt>.
     *
     * @throws OperationFailedException with code OPERATION_NOT_SUPPORTED if
     * DTMF tones are not supported for <tt>callPeer</tt>.
     *
     * @throws IllegalArgumentException in case the call peer does not
     * belong to the underlying implementation.
     */
    void startSendingDTMF(CallPeer callPeer, DTMFTone tone)
        throws OperationFailedException;

    /**
     * Stop sending of the currently transmitting DTMF tone.
     *
     * @param callPeer the  call peer to stop send <tt>tone</tt> to.
     */
    void stopSendingDTMF(CallPeer callPeer);
}
