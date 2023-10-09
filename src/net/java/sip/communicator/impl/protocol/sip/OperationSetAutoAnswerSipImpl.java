/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

import javax.sdp.*;
import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import org.jitsi.service.neomedia.MediaDirection;
import org.jitsi.service.neomedia.MediaType;
import org.jitsi.util.*;

import gov.nist.javax.sip.header.*;
import net.java.sip.communicator.impl.protocol.sip.sdp.*;
import net.java.sip.communicator.service.calljump.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.Logger;
// Disambiguation.

/**
 * An Operation Set defining options to auto answer/forward incoming calls.
 * Forward calls to specified number using same provider.
 * Auto answering calls unconditional, on existence of certain header name, or
 * on existence of specified header name and value.
 *
 * @author Damian Minkov
 * @author Vincent Lucas
 */
public class OperationSetAutoAnswerSipImpl
    extends AbstractOperationSetBasicAutoAnswer
    implements OperationSetAdvancedAutoAnswer
{
    /**
     * Our class logger.
     */
    private static final Logger logger =
        Logger.getLogger(OperationSetBasicTelephonySipImpl.class);

    /**
     * Should we answer on existence of some header and/or name.
     */
    private boolean answerConditional = false;

    /**
     * The header name to look for.
     */
    private String headerName = null;

    /**
     * The header value to look for, if specified.
     */
    private String headerValue = null;

    /**
     * The call number to use for forwarding calls.
     */
    private String callFwdTo = null;

    /**
     * Creates this operation set, loads stored values, populating
     * local variable settings.
     *
     * @param protocolProvider the parent Protocol Provider.
     */
    public OperationSetAutoAnswerSipImpl(
            ProtocolProviderServiceSipImpl protocolProvider)
    {
        super(protocolProvider);
    }

    /**
     * Load values from account properties.
     */
    protected void load()
    {
        super.load();

        AccountID acc = protocolProvider.getAccountID();

        headerName =
            acc.getAccountPropertyString(AUTO_ANSWER_COND_NAME_PROP);
        headerValue =
            acc.getAccountPropertyString(AUTO_ANSWER_COND_VALUE_PROP);
        if(!StringUtils.isNullOrEmpty(headerName))
            answerConditional = true;

        callFwdTo =
            acc.getAccountPropertyString(AUTO_ANSWER_FWD_NUM_PROP);
    }

    /**
     * Saves values to account properties.
     */
    protected void save()
    {
        AccountID acc = protocolProvider.getAccountID();
        Map<String, String> accProps = acc.getAccountProperties();

        // lets clear anything before saving :)
        accProps.put(AUTO_ANSWER_UNCOND_PROP, null);
        accProps.put(AUTO_ANSWER_COND_NAME_PROP, null);
        accProps.put(AUTO_ANSWER_COND_VALUE_PROP, null);
        accProps.put(AUTO_ANSWER_FWD_NUM_PROP, null);

        if(answerUnconditional)
        {
            accProps.put(AUTO_ANSWER_UNCOND_PROP, Boolean.TRUE.toString());
        }
        else if(answerConditional)
        {
            accProps.put(AUTO_ANSWER_COND_NAME_PROP, headerName);

            if(!StringUtils.isNullOrEmpty(headerValue))
                accProps.put(AUTO_ANSWER_COND_VALUE_PROP, headerValue);
        }
        else if(!StringUtils.isNullOrEmpty(callFwdTo))
        {
            accProps.put(AUTO_ANSWER_FWD_NUM_PROP, callFwdTo);
        }
        accProps.put(
                AUTO_ANSWER_WITH_VIDEO_PROP,
                Boolean.toString(answerWithVideo));

        acc.setAccountProperties(accProps);
        SipActivator.getProtocolProviderFactory().storeAccount(acc);
    }

    /**
     * Sets a specified header and its value if they exist in the incoming
     * call packet this will activate auto answer.
     * If value is empty or null it will be considered as any (will search
     * only for a header with that name and ignore the value)
     *
     * @param headerName the name of the header to search
     * @param value the value for the header, can be null.
     */
    public void setAutoAnswerCondition(String headerName, String value)
    {
        clearLocal();

        this.answerConditional = true;
        this.headerName = headerName;
        this.headerValue = value;

        save();
    }

    /**
     * Get the value for automatically forward all calls to the specified
     * number using the same provider.
     *
     * @return numberTo number to use for forwarding
     */
    public String getCallForward()
    {
        return this.callFwdTo;
    }

    /**
     * Clear local settings.
     */
    protected void clearLocal()
    {
        super.clearLocal();

        this.answerConditional = false;
        this.headerName = null;
        this.headerValue = null;
        this.callFwdTo = null;
    }

    /**
     * Makes a check before locally creating call, should we just forward it.
     *
     * @param invite the current invite to check.
     * @param serverTransaction the transaction.
     *
     * @return <tt>true</tt> if we have processed and no further processing is
     *          needed, <tt>false</tt> otherwise.
     */
    public boolean forwardCall(
            Request invite,
            ServerTransaction serverTransaction)
    {
        if(StringUtils.isNullOrEmpty(callFwdTo))
            return false;

        Response response;
        try
        {
            logger.trace("will send moved temporally response: ");

            response = ((ProtocolProviderServiceSipImpl) protocolProvider)
                .getMessageFactory()
                .createResponse(Response.MOVED_TEMPORARILY, invite);

            ContactHeader contactHeader =
                (ContactHeader)response.getHeader(ContactHeader.NAME);
            AddressFactory addressFactory =
                ((ProtocolProviderServiceSipImpl) protocolProvider)
                .getAddressFactory();

            String destination = getCallForward();
            if(!destination.startsWith("sip"))
                destination = "sip:" + destination;

            contactHeader.setAddress(addressFactory.createAddress(
                addressFactory.createURI(destination)));

            serverTransaction.sendResponse(response);
            logger.debug("sent a moved temporally response: " + response);
        }
        catch (Throwable ex)
        {
            logger.error("Error while trying to send a request", ex);
            return false;
        }

        return true;
    }

    /**
     * Checks if the call satisfy the auto answer conditions.
     *
     * @param call The new incoming call to auto-answer if needed.
     *
     * @return <tt>true</tt> if the call satisfy the auto answer conditions.
     * <tt>False</tt> otherwise.
     */
    protected boolean satisfyAutoAnswerConditions(Call call)
    {
        return satisfyAutoAnswerConditions(call, null);
    }

    /**
     * Checks if the call satisfies the auto answer conditions.
     *
     * @param call The new incoming call to auto-answer if needed.
     * @param invite The invite request on which we need to check.  If none is
     * provided, or this invite does not satisfy the auto answer conditions,
     * this method will check the invite from the latest invite transaction for
     * each call peer.
     * @return <tt>true</tt> if the call satisfies the auto answer conditions.
     * <tt>False</tt> otherwise.
     */
    private boolean satisfyAutoAnswerConditions(Call call, Request invite)
    {
        boolean satisfiesConditions = false;

        if(answerConditional)
        {
            if (invite != null)
            {
                satisfiesConditions = inviteContainsAutoAnswerHeader(invite);
            }

            Iterator<? extends CallPeer> peers = call.getCallPeers();
            while(!satisfiesConditions && peers.hasNext())
            {
                Transaction transaction =
                    ((CallPeerSipImpl) peers.next()).getLatestInviteTransaction();

                if(transaction != null)
                {
                    satisfiesConditions =
                        inviteContainsAutoAnswerHeader(transaction.getRequest());
                }
            }
        }

        return satisfiesConditions;
    }

    /**
     * Checks whether the given invite contains the auto-answer header.
     *
     * @param invite the invite to check
     *
     * @return true if the invite does contain the auto-answer header, false
     * otherwise.
     */
    private boolean inviteContainsAutoAnswerHeader(Request invite)
    {
        // lets check for headers
        boolean containsHeader = false;

        SIPHeader callAnswerHeader = (SIPHeader) invite.getHeader(headerName);

        if(callAnswerHeader != null)
        {
            if(!StringUtils.isNullOrEmpty(headerValue))
            {
                String value = callAnswerHeader.getHeaderValue();

                if(value != null && headerValue.equals(value))
                {
                    containsHeader = true;
                }
            }
        }

        return containsHeader;
    }

    /**
     * Makes a check after creating call locally, should we answer it.
     *
     * @param call The new incoming call to auto-answer if needed.
     * @param invite The invite request on which we need to check. If none is
     * provided, or this invite does not satisfy the auto answer conditions,
     * this method will check the invite from the latest invite transaction for
     * each call peer.
     * @param serverTransaction the server transaction on which the call is
     * happening.
     * @param sipProvider the SIP provider of the server transaction.
     * @return <tt>true</tt> if we have processed and no further processing is
     *          needed, <tt>false</tt> otherwise.
     */
    public boolean autoAnswer(CallSipImpl call,
                              Request invite,
                              ServerTransaction serverTransaction,
                              SipProvider sipProvider)
    {
        if(answerUnconditional || satisfyAutoAnswerConditions(call, invite))
        {
            call.processInvite(sipProvider, serverTransaction, true);
            boolean isVideoCall = doesRequestContainsActiveVideoMediaType(call);
            answerCall(call, isVideoCall);
            return true;
        }
        return false;
    }

    /**
     * Checks if an incoming call should be automatically rejected.  This
     * happens when we're already in a call and pull it from another device to
     * a different other device.
     *
     * @param call The new incoming call to auto-reject if needed.
     * @param invite The invite request on which we need to check.
     * @param serverTransaction the server transaction on which the call is
     *                          happening.
     *
     * @return <tt>true</tt> if we have processed and no further processing is
     *          needed, <tt>false</tt> otherwise.
     */
    public boolean autoReject(CallSipImpl call,
                              Request invite,
                              ServerTransaction serverTransaction)
    {
        // The Alert info header contains the information necessary to decide
        // if we reject the call or not:
        // If there is a X-MSW-client-jumpto Alert Info Header then the call
        // should be:
        //   - allowed to ring if this is a call push (i.e. the field is of the
        // form ...=!<some id>), unless the call pull was initiated by this client
        // (i.e. <some id> equals this client's id).
        //   - rejected if this is a call pull (i.e. the field is of the form
        // ...;id=<some id>) UNLESS the call pull was initiated by this client
        // (i.e. <some id> equals this client's id).
        boolean autoRejecting = false;
        Header alertInfoHeader =
                          invite.getHeader(AlertInfoHeader.NAME.toLowerCase());
        String alertInfoHeaderString = null;
        String callPushHeader = null;
        String callPushByUsHeader = null;
        String callPullHeader = null;
        String callPullByUsHeader = null;

        if (alertInfoHeader != null)
        {
            alertInfoHeaderString = alertInfoHeader.toString();
            callPushHeader = CallJumpService.getCallJumpAlertInfoHeader(true, false);
            callPushByUsHeader = CallJumpService.getCallJumpAlertInfoHeader(true, true);
            callPullHeader = CallJumpService.getCallJumpAlertInfoHeader(false, false);
            callPullByUsHeader = CallJumpService.getCallJumpAlertInfoHeader(false, true);

            if (alertInfoHeaderString.contains(callPushByUsHeader) ||
                (!alertInfoHeaderString.contains(callPushHeader) &&
                alertInfoHeaderString.contains(callPullHeader) &&
                !alertInfoHeaderString.contains(callPullByUsHeader)))
            {
                call.autoReject(serverTransaction, invite);
                autoRejecting = true;
            }
        }

        logger.debug("Autorejecting call? " + autoRejecting +
                     "[alertInfoHeader = " + alertInfoHeaderString +
                      ", callPushHeader: " + callPushHeader +
                      ", callPushByUsHeader: " + callPushByUsHeader +
                      ", callPullHeader: " + callPullHeader +
                      ", callPullByUsHeader: " + callPullByUsHeader + "]");
        return autoRejecting;
    }

    /**
     * Detects if the incoming call is a video call. Parses the SDP from the SIP
     * request to determine if the video is active.
     *
     * @param call The new incoming call to auto-answer if needed.
     *
     * @return True if the incoming call is a video call. False, otherwise.
     */
    private boolean doesRequestContainsActiveVideoMediaType(Call call)
    {
        Iterator<? extends CallPeer> peers = call.getCallPeers();

        while(peers.hasNext())
        {
            Transaction transaction = ((CallPeerSipImpl) peers.next())
                .getLatestInviteTransaction();
            if(transaction != null)
            {
                Request inviteReq = transaction.getRequest();

                if(inviteReq != null && inviteReq.getRawContent() != null)
                {
                    String sdpStr = SdpUtils.getContentAsString(inviteReq);
                    SessionDescription sesDescr
                        = SdpUtils.parseSdpString(sdpStr);
                    List<MediaDescription> remoteDescriptions
                        = SdpUtils.extractMediaDescriptions(sesDescr);

                    for (MediaDescription mediaDescription :
                            remoteDescriptions)
                    {
                        if(SdpUtils.getMediaType(mediaDescription)
                                == MediaType.VIDEO)
                        {
                            if(SdpUtils.getDirection(mediaDescription)
                                    == MediaDirection.SENDRECV)
                            {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }
}
