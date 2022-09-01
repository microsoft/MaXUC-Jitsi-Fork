/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.net.URI;
import java.text.*;
import java.util.*;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.xml.XMLUtils;
import org.w3c.dom.*;

/**
 * Sip presence implementation (SIMPLE).
 */
public class OperationSetPresenceSipSimpleImpl
    extends OperationSetPresenceSipImpl
{
    /**
     * Our class logger.
     */
    private static final Logger logger
        = Logger.getLogger(OperationSetPresenceSipSimpleImpl.class);

    /**
     * List of all the CallIds to wait before unregister
     * Content : String
     */
    private final List<String> waitedCallIds = new Vector<>();

    /**
     * Do we have to use a distant presence agent (default initial value)
     */
    private boolean useDistantPA;

    /**
     * Entity tag associated with the current communication with the distant PA
     */
    private String distantPAET = null;

    /**
     * The current CSeq used in the PUBLISH requests
     */
    private static long publish_cseq = 1;

    /**
     * The re-PUBLISH task if any
     */
    private RePublishTask republishTask = null;

    /**
     * The interval between two execution of the polling task (in ms.)
     */
    private final int pollingTaskPeriod;

    /**
     * The task in charge of polling offline contacts
     */
    private PollOfflineContactsTask pollingTask = null;

    /**
     * XML documents types.
     * The notify body content as said in rfc3856.
     */
    private static final String PIDF_XML        = "pidf+xml";
    private static final String WATCHERINFO_XML = "watcherinfo+xml";

    private static final String PIDF_PKG    = "presence";

    // pidf elements and attributes
    private static final String PRIORITY_ATTRIBUTE  = "priority";

    private static final String PERSON_ELEMENT  = "person";
    private static final String ACTIVITY_ELEMENT= "activities";
    private static final String AWAY_ELEMENT    = "away";
    private static final String BUSY_ELEMENT    = "busy";
    private static final String OTP_ELEMENT     = "on-the-phone";
    private static final String STATUS_ICON_ELEMENT = "status-icon";

    private static final String WATCHERINFO_NS_VALUE
            = "urn:ietf:params:xml:ns:watcherinfo";
    private static final String WATCHERINFO_ELEMENT = "watcherinfo";
    private static final String STATE_ATTRIBUTE     = "state";
    private static final String VERSION_ATTRIBUTE   = "version";
    private static final String WATCHERLIST_ELEMENT = "watcher-list";
    private static final String RESOURCE_ATTRIBUTE  = "resource";
    private static final String PACKAGE_ATTRIBUTE   = "package";
    private static final String WATCHER_ELEMENT     = "watcher";

    /**
     * The <code>EventPackageSubscriber</code> which provides the ability of
     * this instance to act as a subscriber for the presence.winfo event package.
     */
    private final EventPackageSubscriber watcherInfoSubscriber;

    /**
     * Creates an instance of this operation set keeping a reference to the
     * specified parent <tt>provider</tt>.
     * @param provider the ProtocolProviderServiceSipImpl instance that
     * created us.
     * @param presenceEnabled if we are activated or if we don't have to
     * handle the presence informations for contacts
     * @param forceP2PMode if we should start in the p2p mode directly
     * @param pollingPeriod the period between two poll for offline contacts
     * @param subscriptionExpiration the default subscription expiration value
     * to use
     */
    public OperationSetPresenceSipSimpleImpl(
        ProtocolProviderServiceSipImpl provider,
        boolean presenceEnabled,
        boolean forceP2PMode,
        int pollingPeriod,
        int subscriptionExpiration)
    {
        super(provider,
            presenceEnabled,
            forceP2PMode,
            pollingPeriod,
            subscriptionExpiration,
            PIDF_PKG,
            PIDF_XML);

        if (this.presenceEnabled)
        {
            this.notifier = new EventPackageNotifier(
                this.parentProvider,
                PIDF_PKG,
                PRESENCE_DEFAULT_EXPIRE,
                PIDF_XML,
                this.timer)
            {
                /**
                 * Creates a new <tt>PresenceNotificationSubscription</tt>
                 * instance.
                 * @param fromAddress our AOR
                 * @param eventId the event id to use.
                 */
                protected Subscription createSubscription(
                    Address fromAddress, String eventId)
                {
                    setUseDistantPA(false);
                    return new PresenceNotifierSubscription(
                        fromAddress, eventId);
                }
            };

            this.watcherInfoSubscriber
                = new EventPackageSubscriber(
                        this.parentProvider,
                        "presence.winfo",
                        this.subscriptionDuration,
                        WATCHERINFO_XML,
                        this.timer,
                        REFRESH_MARGIN);
        }
        else
        {
            this.notifier = null;
            this.watcherInfoSubscriber = null;
        }

        // retrieve the options for this account
        this.pollingTaskPeriod
        = (pollingPeriod > 0) ? (pollingPeriod * 1000) : 30000;

        // if we force the p2p mode, we start by not using a distant PA
        this.useDistantPA = !forceP2PMode;
    }

    /**
     * Sets if we should use a distant presence agent.
     *
     * @param useDistantPA
     *            <tt>true</tt> if we should use a distant presence agent
     */
    private void setUseDistantPA(boolean useDistantPA)
    {
        this.useDistantPA = useDistantPA;

        if (!this.useDistantPA && (this.republishTask != null))
        {
            this.republishTask.cancel();
            this.republishTask = null;
        }
    }

    /**
     * Requests the provider to enter into a status corresponding to the
     * specified parameters.
     *
     * @param status the PresenceStatus as returned by
     *   getRequestableStatusSet
     * @param statusMsg the message that should be set as the reason to
     *   enter that status
     * @throws IllegalArgumentException if the status requested is not a
     *   valid PresenceStatus supported by this provider.
     * @throws IllegalStateException if the provider is not currently
     *   registered.
     * @throws OperationFailedException with code NETWORK_FAILURE if
     *   publishing the status fails due to a network error.
     */
    public void publishPresenceStatus(
            PresenceStatus status,
            String statusMsg)
        throws IllegalArgumentException,
               IllegalStateException,
               OperationFailedException
    {
        PresenceStatus oldStatus = this.presenceStatus;
        this.presenceStatus = status;
        String oldMessage = this.statusMessage;
        this.statusMessage = statusMsg;

        if (!this.presenceEnabled
            || parentProvider.getRegistrarConnection()
                instanceof SipRegistrarlessConnection)//no registrar-no publish
        {
            // inform the listeners of these changes in order to reflect
            // to GUI
            this.fireProviderStatusChangeEvent(oldStatus);
            this.fireProviderMsgStatusChangeEvent(oldMessage);

            return;
        }

        // in the offline status, the protocol provider is already unregistered
        if (!status.equals(sipStatusEnum.getStatus(SipStatusEnum.OFFLINE)))
            assertConnected();

        // now inform our distant presence agent if we have one
        if (this.useDistantPA)
        {
            Request req = null;
            if (status.equals(sipStatusEnum.getStatus(SipStatusEnum.OFFLINE)))
            {
                // unpublish our state
                req = createPublish(0, false);

                // remember the callid to be sure that the publish arrived
                // before unregister
                synchronized (this.waitedCallIds)
                {
                    this.waitedCallIds.add(((CallIdHeader)
                        req.getHeader(CallIdHeader.NAME)).getCallId());
                }
            }
            else
            {
                req = createPublish(this.subscriptionDuration, true);
            }

            ClientTransaction transac = null;
            try
            {
                transac = this.parentProvider
                    .getDefaultJainSipProvider().getNewClientTransaction(req);
            }
            catch (TransactionUnavailableException e)
            {
                logger.error("can't create the client transaction", e);
                throw new OperationFailedException(
                        "can't create the client transaction",
                        OperationFailedException.NETWORK_FAILURE);
            }

            try
            {
                transac.sendRequest();
            }
            catch (SipException e)
            {
                logger.error("can't send the PUBLISH request", e);
                throw new OperationFailedException(
                        "can't send the PUBLISH request",
                        OperationFailedException.NETWORK_FAILURE);
            }
        }
        // no distant presence agent, send notify to everyone
        else
        {
            String subscriptionState;
            String reason;

            if (status.equals(sipStatusEnum.getStatus(SipStatusEnum.OFFLINE)))
            {
                subscriptionState = SubscriptionStateHeader.TERMINATED;
                reason = SubscriptionStateHeader.PROBATION;
            }
            else
            {
                subscriptionState = SubscriptionStateHeader.ACTIVE;
                reason = null;
            }
            notifier.notifyAll(subscriptionState, reason);
        }

        // must be done in last to avoid some problem when terminating a
        // subscription of a contact who is also one of our watchers
        if (status.equals(sipStatusEnum.getStatus(SipStatusEnum.OFFLINE)))
            unsubscribeToAllContact();

        // inform the listeners of these changes
        this.fireProviderStatusChangeEvent(oldStatus);
        this.fireProviderMsgStatusChangeEvent(oldMessage);
    }

    /**
     * Create a valid PUBLISH request corresponding to the current presence
     * state. The request is forged to be send to the current distant presence
     * agent.
     *
     * @param expires the expires value to send
     * @param insertPresDoc if a presence document has to be added (typically
     * = false when refreshing a publication)
     *
     * @return a valid <tt>Request</tt> containing the PUBLISH
     *
     * @throws OperationFailedException if something goes wrong
     */
    private Request createPublish(int expires, boolean insertPresDoc)
        throws OperationFailedException
    {
        // Call ID
        CallIdHeader callIdHeader = this.parentProvider
            .getDefaultJainSipProvider().getNewCallId();

        // FromHeader and ToHeader
        String localTag = SipMessageFactory.generateLocalTag();
        FromHeader fromHeader = null;
        ToHeader toHeader = null;
        try
        {
            //the publish method can only be used if we have a presence agent
            //so we deliberately use our AOR and do not use the
            //getOurSipAddress() method.
            Address ourAOR = parentProvider.getRegistrarConnection()
                                                    .getAddressOfRecord();
            //FromHeader
            fromHeader = this.parentProvider.getHeaderFactory()
                .createFromHeader(ourAOR,
                                  localTag);

            //ToHeader (it's ourselves)
            toHeader = this.parentProvider.getHeaderFactory()
                .createToHeader(ourAOR, null);
        }
        catch (ParseException ex)
        {
            //these two should never happen.
            logger.error(
                "An unexpected error occurred while"
                + "constructing the FromHeader or ToHeader", ex);
            throw new OperationFailedException(
                "An unexpected error occurred while"
                + "constructing the FromHeader or ToHeader",
                OperationFailedException.INTERNAL_ERROR,
                ex);
        }

        //ViaHeaders
        List<ViaHeader> viaHeaders = parentProvider.getLocalViaHeaders(
            toHeader.getAddress());

        //MaxForwards
        MaxForwardsHeader maxForwards = this.parentProvider
            .getMaxForwardsHeader();

        // Content params
        byte[] doc = null;

        if (insertPresDoc)
        {
            //this is a publish request so we would use the default
            //getLocalContact that would return a method based on the registrar
            //address
            doc
                = getPidfPresenceStatus(
                    getLocalContactForDst(toHeader.getAddress()));
        }
        else
        {
            doc = new byte[0];
        }

        ContentTypeHeader contTypeHeader;
        try
        {
            contTypeHeader = this.parentProvider.getHeaderFactory()
                .createContentTypeHeader("application",
                                         PIDF_XML);
        }
        catch (ParseException ex)
        {
            //these two should never happen.
            logger.error(
                "An unexpected error occurred while"
                + "constructing the content headers", ex);
            throw new OperationFailedException(
                "An unexpected error occurred while"
                + "constructing the content headers"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        // eventually add the entity tag
        SIPIfMatchHeader ifmHeader = null;
        try
        {
            if (this.distantPAET != null)
            {
                ifmHeader = this.parentProvider.getHeaderFactory()
                    .createSIPIfMatchHeader(this.distantPAET);
            }
        }
        catch (ParseException e)
        {
            logger.error(
                "An unexpected error occurred while"
                + "constructing the SIPIfMatch header", e);
            throw new OperationFailedException(
                "An unexpected error occurred while"
                + "constructing the SIPIfMatch header",
                OperationFailedException.INTERNAL_ERROR,
                e);
        }

        //CSeq
        CSeqHeader cSeqHeader = null;
        try
        {
            cSeqHeader = this.parentProvider.getHeaderFactory()
                .createCSeqHeader(publish_cseq++, Request.PUBLISH);
        }
        catch (InvalidArgumentException | ParseException ex)
        {
            //Shouldn't happen
            logger.error(
                "An unexpected error occurred while"
                + "constructing the CSeqHeader", ex);
            throw new OperationFailedException(
                "An unexpected error occurred while"
                + "constructing the CSeqHeader"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        // expires
        ExpiresHeader expHeader = null;
        try
        {
            expHeader = this.parentProvider.getHeaderFactory()
                .createExpiresHeader(expires);
        }
        catch (InvalidArgumentException e)
        {
            // will never happen
            logger.error(
                    "An unexpected error occurred while"
                    + "constructing the Expires header", e);
            throw new OperationFailedException(
                    "An unexpected error occurred while"
                    + "constructing the Expires header"
                    , OperationFailedException.INTERNAL_ERROR
                    , e);
        }

        // event
        EventHeader evtHeader = null;
        try
        {
            evtHeader = this.parentProvider.getHeaderFactory()
                .createEventHeader("presence");
        }
        catch (ParseException e)
        {
            // will never happen
            logger.error(
                    "An unexpected error occurred while"
                    + "constructing the Event header", e);
            throw new OperationFailedException(
                    "An unexpected error occurred while"
                    + "constructing the Event header"
                    , OperationFailedException.INTERNAL_ERROR
                    , e);
        }

        Request req = null;
        try
        {
            req = this.parentProvider.getMessageFactory().createRequest(
                toHeader.getAddress().getURI(), Request.PUBLISH, callIdHeader,
                cSeqHeader, fromHeader, toHeader, viaHeaders, maxForwards,
                contTypeHeader, doc);
        }
        catch (ParseException ex)
        {
            //shouldn't happen
            logger.error(
                "Failed to create message Request!", ex);
            throw new OperationFailedException(
                "Failed to create message Request!"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        req.setHeader(expHeader);
        req.setHeader(evtHeader);

        if (ifmHeader != null)
        {
            req.setHeader(ifmHeader);
        }

        return req;
    }

    /**
     * Analyzes the incoming <tt>responseEvent</tt> and then forwards it to the
     * proper event handler.
     *
     * @param responseEvent the responseEvent that we received
     *            ProtocolProviderService.
     * @return <tt>true</tt> if the specified event has been handled by this
     *         processor and shouldn't be offered to other processors registered
     *         for the same method; <tt>false</tt>, otherwise
     */
    public boolean processResponse(ResponseEvent responseEvent)
    {
        if (!this.presenceEnabled)
            return false;

        ClientTransaction clientTransaction = responseEvent
            .getClientTransaction();
        Response response = responseEvent.getResponse();

        CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
        if (cseq == null)
        {
            logger.error("An incoming response did not contain a CSeq header");
            return false;
        }
        String method = cseq.getMethod();

        boolean processed = false;

        // PUBLISH
        if (method.equals(Request.PUBLISH))
        {
            // if it's a final response to a PUBLISH, we try to remove it from
            // the list of waited PUBLISH end
            if (response.getStatusCode() != Response.UNAUTHORIZED
                && response.getStatusCode() != Response
                    .PROXY_AUTHENTICATION_REQUIRED
                && response.getStatusCode() != Response.INTERVAL_TOO_BRIEF)
            {
                synchronized (this.waitedCallIds)
                {
                    this.waitedCallIds.remove(((CallIdHeader) response
                        .getHeader(CallIdHeader.NAME)).getCallId());
                }
            }

            // OK (200)
            if (response.getStatusCode() == Response.OK)
            {
                // remember the entity tag
                SIPETagHeader etHeader = (SIPETagHeader)
                    response.getHeader(SIPETagHeader.NAME);

                // must be one (rfc3903)
                if (etHeader == null)
                {
                    logger.debug("can't find the ETag header");
                    return false;
                }

                this.distantPAET = etHeader.getETag();

                // schedule a re-publish task
                ExpiresHeader expires = (ExpiresHeader)
                    response.getHeader(ExpiresHeader.NAME);

                if (expires == null)
                {
                    logger.error("no Expires header in the response");
                    return false;
                }

                // if it's a response to an unpublish request (Expires: 0),
                // invalidate the etag and don't schedule a republish
                if (expires.getExpires() == 0)
                {
                    this.distantPAET = null;
                    return true;
                }

                // just to be sure to not have two refreshing task
                if (this.republishTask != null)
                    this.republishTask.cancel();

                this.republishTask = new RePublishTask();

                int republishDelay = expires.getExpires();
                // try to keep a margin if the refresh delay allows it
                if (republishDelay >= (2*REFRESH_MARGIN))
                    republishDelay -= REFRESH_MARGIN;
                timer.schedule(this.republishTask, republishDelay * 1000);

            // UNAUTHORIZED (401/407)
            }
            else if (response.getStatusCode() == Response.UNAUTHORIZED
                    || response.getStatusCode() == Response
                        .PROXY_AUTHENTICATION_REQUIRED)
            {
                try
                {
                    processAuthenticationChallenge(
                        clientTransaction,
                        response,
                        (SipProvider) responseEvent.getSource());
                }
                catch (OperationFailedException e)
                {
                    logger.error("can't handle the challenge", e);
                    return false;
                }
            // INTERVAL TOO BRIEF (423)
            }
            else if (response.getStatusCode() == Response.INTERVAL_TOO_BRIEF)
            {
                // we get the Min expires and we use it as the interval
                MinExpiresHeader min = (MinExpiresHeader)
                    response.getHeader(MinExpiresHeader.NAME);

                if (min == null)
                {
                    logger.error("can't find a min expires header in the 423" +
                            " error message");
                    return false;
                }

                // send a new publish with the new expires value
                Request req = null;
                try
                {
                    req = createPublish(min.getExpires(), true);
                }
                catch (OperationFailedException e)
                {
                    logger.error("can't create the new publish request", e);
                    return false;
                }

                ClientTransaction transac = null;
                try
                {
                    transac = this.parentProvider
                        .getDefaultJainSipProvider()
                        .getNewClientTransaction(req);
                }
                catch (TransactionUnavailableException e)
                {
                    logger.error("can't create the client transaction", e);
                    return false;
                }

                try
                {
                    transac.sendRequest();
                }
                catch (SipException e)
                {
                    logger.error("can't send the PUBLISH request", e);
                    return false;
                }

            // CONDITIONAL REQUEST FAILED (412)
            }
            else if (response.getStatusCode() == Response
                                                .CONDITIONAL_REQUEST_FAILED)
            {
                // as recommanded in rfc3903#5, we start a totally new
                // publication
                this.distantPAET = null;
                Request req = null;
                try
                {
                    req = createPublish(this.subscriptionDuration, true);
                }
                catch (OperationFailedException e)
                {
                    logger.error("can't create the new publish request", e);
                    return false;
                }

                ClientTransaction transac = null;
                try
                {
                    transac = this.parentProvider
                        .getDefaultJainSipProvider()
                        .getNewClientTransaction(req);
                }
                catch (TransactionUnavailableException e)
                {
                    logger.error("can't create the client transaction", e);
                    return false;
                }

                try
                {
                    transac.sendRequest();
                }
                catch (SipException e)
                {
                    logger.error("can't send the PUBLISH request", e);
                    return false;
                }
            }
            // PROVISIONAL RESPONSE (1XX)
            else if (response.getStatusCode() >= 100
                    && response.getStatusCode() < 200)
            {
                // Ignore provisional response: simply wait for a next response
                // with a SUCCESS (2XX) code.
            }
            // with every other error, we consider that we have to start a new
            // communication.
            // Enter p2p mode if the distant PA mode fails
            else
            {
                logger.debug("error received from the network" + response);

                this.distantPAET = null;

                if (this.useDistantPA)
                {
                    logger.debug(
                                "we enter into the peer-to-peer mode"
                                + " as the distant PA mode fails");

                    setUseDistantPA(false);

                    // if we are here, we don't have any watcher so no need to
                    // republish our presence state
                }
            }

            processed = true;
        }

        return processed;
    }

    /**
     * Process a request from a distant contact
     *
     * @param requestEvent the <tt>RequestEvent</tt> containing the newly
     *            received request.
     * @return <tt>true</tt> if the specified event has been handled by this
     *         processor and shouldn't be offered to other processors registered
     *         for the same method; <tt>false</tt>, otherwise
     */
    public boolean processRequest(RequestEvent requestEvent)
    {
        if (!this.presenceEnabled)
            return false;

        Request request = requestEvent.getRequest();
        EventHeader eventHeader
            = (EventHeader) request.getHeader(EventHeader.NAME);

        if (eventHeader == null)
        {
            /*
             * We are not concerned by this request, perhaps another listener
             * is. So don't send a 489 / Bad event response here.
             */
            return false;
        }

        String eventType = eventHeader.getEventType();

        if (!"presence".equalsIgnoreCase(eventType)
                && !"presence.winfo".equalsIgnoreCase(eventType))
            return false;

        String requestMethod = request.getMethod();
        boolean processed = false;

        // presence PUBLISH and presence.winfo SUBSCRIBE
        if (("presence".equalsIgnoreCase(eventType)
                        && Request.PUBLISH.equals(requestMethod))
                || ("presence.winfo".equalsIgnoreCase(eventType)
                        && Request.SUBSCRIBE.equals(requestMethod)))
        {
            /*
             * We aren't supposed to receive a PUBLISH so just say "not
             * implemented". This behavior is useful for SC to SC communication
             * with the PA auto detection feature and a server which proxy the
             * PUBLISH requests.
             *
             * We support presence.winfo only as a subscriber, not as a
             * notifier. So say "not implemented" in order to not have its
             * ServerTransaction remaining in the SIP stack forever.
             */
            processed
                = EventPackageSupport.sendNotImplementedResponse(
                        parentProvider,
                        requestEvent);
        }

        return processed;
    }

    /**
     * Attempts to re-generate the corresponding request with the proper
     * credentials.
     *
     * @param clientTransaction
     *            the corresponding transaction
     * @param response
     *            the challenge
     * @param jainSipProvider
     *            the provider that received the challenge
     * @throws OperationFailedException
     *             if processing the authentication challenge fails.
     */
    private void processAuthenticationChallenge(
            ClientTransaction clientTransaction,
            Response response,
            SipProvider jainSipProvider)
        throws OperationFailedException
    {
        EventPackageSupport.processAuthenticationChallenge(
            parentProvider,
            clientTransaction,
            response,
            jainSipProvider);
    }

     /**
      * Sets the contact's presence status using the PIDF document provided.
      * In case of conflict (more than one status per contact) the last valid
      * status in the document is used.
      * This implementation is very tolerant to be more compatible with bad
      * implementations of SIMPLE. The limit of the tolerance is defined by
      * the CPU cost: as far as the tolerance costs nothing more in well
      * structured documents, we do it.
      *
      * @param presenceDoc the pidf document to use
      */
     public void setPidfPresenceStatus(String presenceDoc)
     {
         Document doc = convertDocument(presenceDoc);

         if (doc == null)
             return;

         logger.debug("parsing:\n" + presenceDoc);

         // <presence>
         NodeList presList = doc.getElementsByTagNameNS(PIDF_NS_VALUE,
                 PRESENCE_ELEMENT);

         if (presList.getLength() == 0)
         {
             presList = doc.getElementsByTagNameNS(ANY_NS, PRESENCE_ELEMENT);

             if (presList.getLength() == 0)
             {
                 logger.error("no presence element in this document");
                 return;
             }
         }
         if (presList.getLength() > 1)
         {
             logger.warn("more than one presence element in this document");
         }
         Node presNode = presList.item(0);
         if (presNode.getNodeType() != Node.ELEMENT_NODE)
         {
             logger.error("the presence node is not an element");
             return;
         }
         Element presence = (Element) presNode;

         // RPID area

         // due to a lot of changes in the past years to this functionality,
         // the namespace used by servers and clients are often wrong so we just
         // ignore namespaces here

         PresenceStatus personStatus = null;
         URI personStatusIcon = null;
         NodeList personList = presence.getElementsByTagNameNS(ANY_NS,
                 PERSON_ELEMENT);

         //if (personList.getLength() > 1) {
         //    logger.error("more than one person in this document");
         //    return;
         //}

         if (personList.getLength() > 0)
         {
             Node personNode = personList.item(0);
             if (personNode.getNodeType() != Node.ELEMENT_NODE)
             {
                 logger.error("the person node is not an element");
                 return;
             }
             Element person = (Element) personNode;

             NodeList activityList =
                 person.getElementsByTagNameNS(ANY_NS, ACTIVITY_ELEMENT);
             if (activityList.getLength() > 0)
             {
                 Element activity = null;
                 // find the first correct activity
                 for (int i = 0; i < activityList.getLength(); i++)
                 {
                     Node activityNode = activityList.item(i);

                     if (activityNode.getNodeType() != Node.ELEMENT_NODE)
                         continue;

                     activity = (Element) activityNode;

                     NodeList statusList = activity.getChildNodes();
                     label:
                     for (int j = 0; j < statusList.getLength(); j++)
                     {
                         Node statusNode = statusList.item(j);
                         if (statusNode.getNodeType() == Node.ELEMENT_NODE)
                         {
                             String statusname = statusNode.getLocalName();
                             switch (statusname)
                             {
                                 case AWAY_ELEMENT:
                                     personStatus = sipStatusEnum
                                             .getStatus(SipStatusEnum.AWAY);
                                     break label;
                                 case BUSY_ELEMENT:
                                     personStatus = sipStatusEnum
                                             .getStatus(SipStatusEnum.BUSY);
                                     break label;
                                 case OTP_ELEMENT:
                                     personStatus = sipStatusEnum
                                             .getStatus(SipStatusEnum.ON_THE_PHONE);
                                     break label;
                             }
                         }
                     }
                     if (personStatus != null)
                         break;
                 }
             }
             NodeList statusIconList = person.getElementsByTagNameNS(ANY_NS,
                     STATUS_ICON_ELEMENT);
             if (statusIconList.getLength() > 0)
             {
                 Element statusIcon;
                 Node statusIconNode = statusIconList.item(0);
                 if (statusIconNode.getNodeType() == Node.ELEMENT_NODE)
                 {
                     statusIcon = (Element) statusIconNode;
                     String content = getTextContent(statusIcon);
                     if (content != null && content.trim().length() != 0)
                     {
                         try
                         {
                             personStatusIcon = URI.create(content);
                         }
                         catch (IllegalArgumentException ex)
                         {
                             logger.error("Person's status icon uri: " +
                                     content + " is invalid");
                         }
                     }
                 }
             }
         }

          if(personStatusIcon != null)
          {
              String contactID =
                  XMLUtils.getAttribute(presNode, ENTITY_ATTRIBUTE);

              if (contactID.startsWith("pres:"))
              {
                  contactID = contactID.substring("pres:".length());
              }
              Contact contact = resolveContactID(contactID);
              updateContactIcon((ContactSipImpl) contact, personStatusIcon);
         }

         // Vector containing the list of status to set for each contact in
         // the presence document ordered by priority (highest first).
         // <SipContact, Float (priority), SipStatusEnum>
         List<Object[]> newPresenceStates = new Vector<>(3, 2);

         // <tuple>
         NodeList tupleList = getPidfChilds(presence, TUPLE_ELEMENT);
         for (int i = 0; i < tupleList.getLength(); i++)
         {
             Node tupleNode = tupleList.item(i);

             if (tupleNode.getNodeType() != Node.ELEMENT_NODE)
                 continue;

             Element tuple = (Element) tupleNode;

             // <contact>
             NodeList contactList = getPidfChilds(tuple, CONTACT_ELEMENT);

             // we use a vector here and not an unique contact to handle an
             // error case where many contacts are associated with a status
             // Vector<ContactSipImpl>
             List<Object[]> sipcontact = new Vector<>(1, 3);
             String contactID = null;
             if (contactList.getLength() == 0)
             {
                 // use the entity attribute of the presence node
                 contactID = XMLUtils.getAttribute(
                         presNode, ENTITY_ATTRIBUTE);
                 // also accept entity URIs starting with pres: instead of sip:
                 if (contactID.startsWith("pres:"))
                 {
                     contactID = contactID.substring("pres:".length());
                 }
                 Contact tmpContact = resolveContactID(contactID);

                 if (tmpContact != null)
                 {
                     sipcontact.add(new Object[] { tmpContact, new Float(0f) });
                 }
             }
             else
             {
                 // this is normally not permitted by RFC3863
                 for (int j = 0; j < contactList.getLength(); j++)
                 {
                     Node contactNode = contactList.item(j);

                     if (contactNode.getNodeType() != Node.ELEMENT_NODE)
                         continue;

                     Element contact = (Element) contactNode;

                     contactID = getTextContent(contact);
                     // also accept entity URIs starting with pres: instead
                     // of sip:
                     if (contactID.startsWith("pres:"))
                     {
                         contactID = contactID.substring("pres:".length());
                     }
                     Contact tmpContact = resolveContactID(contactID);
                     if (tmpContact == null)
                         continue;

                     // defines an array containing the contact and its
                     // priority
                     Object tab[] = new Object[2];

                     // search if the contact has a priority
                     String prioStr = contact.getAttribute(PRIORITY_ATTRIBUTE);
                     Float prio = null;
                     try
                     {
                         if (prioStr == null || prioStr.length() == 0)
                         {
                             prio = 0f;
                         }
                         else
                         {
                             prio = Float.valueOf(prioStr);
                         }
                     }
                     catch (NumberFormatException e)
                     {
                         logger.debug("contact priority is not a valid float",
                                     e);
                         prio = 0f;
                     }

                     // 0 <= priority <= 1 according to rfc
                     if (prio < 0)
                     {
                         prio = 0f;
                     }

                     if (prio > 1)
                     {
                         prio = 1f;
                     }

                     tab[0] = tmpContact;
                     tab[1] = prio;

                     // search if the contact hasn't already been added
                     boolean contactAlreadyListed = false;
                     for (int k = 0; k < sipcontact.size(); k++)
                     {
                         Object[] tmp = sipcontact.get(k);

                         if (tmp[0].equals(tmpContact))
                         {
                             contactAlreadyListed = true;

                             // take the highest priority
                             if ((Float) tmp[1] <
                                     prio)
                             {
                                 sipcontact.remove(k);
                                 sipcontact.add(tab);
                             }
                             break;
                         }
                     }

                     // add the contact and its priority to the list
                     if (!contactAlreadyListed)
                     {
                         sipcontact.add(tab);
                     }
                 }
             }

             if (sipcontact.isEmpty())
             {
                 logger.debug("no contact found for id: " + contactID);
                 continue;
             }

             // <status>
             NodeList statusList = getPidfChilds(tuple, STATUS_ELEMENT);

             // in case of many status, just consider the last one
             // this is normally not permitted by RFC3863
             int index = statusList.getLength() - 1;
             Node statusNode = null;
             do
             {
                 Node temp = statusList.item(index);
                 if (temp.getNodeType() == Node.ELEMENT_NODE)
                 {
                     statusNode = temp;
                     break;
                 }
                 index--;
             }
             while (index >= 0);

             Element basic = null;

             if (statusNode == null)
             {
                 logger.debug("no valid status in this tuple");
             }
             else
             {
                 Element status = (Element) statusNode;

                 // <basic>
                 NodeList basicList = getPidfChilds(status, BASIC_ELEMENT);

                 // in case of many basic, just consider the last one
                 // this is normally not permitted by RFC3863
                 index = basicList.getLength() - 1;
                 Node basicNode = null;
                 do
                 {
                     Node temp = basicList.item(index);
                     if (temp.getNodeType() == Node.ELEMENT_NODE)
                     {
                         basicNode = temp;
                         break;
                     }
                     index--;
                 }
                 while (index >= 0);

                 if (basicNode == null)
                 {
                     logger.debug("no valid <basic> in this status");
                 }
                 else
                 {
                     basic = (Element) basicNode;
                 }
             }

             // search for a <note> that can define a more precise
             // status this is not recommended by RFC3863 but some im
             // clients use this.
             NodeList noteList = getPidfChilds(tuple, NOTE_ELEMENT);

             boolean changed = false;
             for (int k = 0; k < noteList.getLength() && !changed; k++)
             {
                 Node noteNode = noteList.item(k);

                 if (noteNode.getNodeType() != Node.ELEMENT_NODE)
                     continue;

                 Element note = (Element) noteNode;

                 String state = getTextContent(note);

                 Iterator<PresenceStatus> states
                     = sipStatusEnum.getSupportedStatusSet();
                 while (states.hasNext())
                 {
                     PresenceStatus current = states.next();

                     if (current.getStatusName().equalsIgnoreCase(state))
                     {
                         changed = true;
                         newPresenceStates = setStatusForContacts(current,
                                 sipcontact,
                                 newPresenceStates);
                         break;
                     }
                 }
             }

             if (!changed && basic != null)
             {
                 if (getTextContent(basic).equalsIgnoreCase(ONLINE_STATUS))
                 {
                     // if its online(open) we use the person status
                     // if any, otherwise just mark as online
                     if(personStatus != null)
                     {
                         newPresenceStates = setStatusForContacts(
                                 personStatus,
                                 sipcontact,
                                 newPresenceStates);
                     }
                     else
                     {
                         newPresenceStates = setStatusForContacts(
                                 sipStatusEnum.getStatus(SipStatusEnum.ONLINE),
                                 sipcontact,
                                 newPresenceStates);
                     }
                 }
                 else if (getTextContent(basic).equalsIgnoreCase(
                         OFFLINE_STATUS))
                 {
                     // if its offline we ignore person status
                     newPresenceStates = setStatusForContacts(
                             sipStatusEnum.getStatus(SipStatusEnum.OFFLINE),
                             sipcontact,
                             newPresenceStates);
                 }
             }
             else
             {
                 if (!changed)
                 {
                     logger.debug("no suitable presence state found in this "
                                 + "tuple");
                 }
             }
         } // for each <tuple>

         // Now really set the new presence status for the listed contacts
         // newPresenceStates is ordered so priority order is respected
         for (Object[] tab : newPresenceStates)
         {
             ContactSipImpl contact = (ContactSipImpl) tab[0];
             PresenceStatus status = (PresenceStatus) tab[2];

             changePresenceStatusForContact(contact, status);
         }
     }

     /**
      * Parses watchers info document rfc3858.
      * @param watcherInfoDoc the doc.
      * @param subscriber the subscriber which receives lists.
      */
     private void setWatcherInfoStatus(
             WatcherInfoSubscriberSubscription subscriber,
             String watcherInfoDoc)
     {
         if(this.authorizationHandler == null)
         {
             logger.warn("AuthorizationHandler missing!");
             return;
         }

         Document doc = convertDocument(watcherInfoDoc);

          if (doc == null)
              return;

          logger.debug("parsing:\n" + watcherInfoDoc);

         // <watcherinfo>
         NodeList watchList = doc.getElementsByTagNameNS(
                 WATCHERINFO_NS_VALUE, WATCHERINFO_ELEMENT);
         if (watchList.getLength() == 0)
         {
             watchList = doc.getElementsByTagNameNS(
                      ANY_NS, WATCHERINFO_ELEMENT);

             if (watchList.getLength() == 0)
             {
                 logger.error("no watcherinfo element in this document");
                 return;
             }
         }
         if (watchList.getLength() > 1)
         {
             logger.warn("more than one watcherinfo element in this document");
         }
         Node watcherInfoNode = watchList.item(0);
         if (watcherInfoNode.getNodeType() != Node.ELEMENT_NODE)
         {
             logger.error("the watcherinfo node is not an element");
             return;
         }

         Element watcherInfo = (Element)watcherInfoNode;

         // we don't take in account whether the state is full or partial.
         logger.debug("Watcherinfo is with state: "
                     + watcherInfo.getAttribute(STATE_ATTRIBUTE));

         int currentVersion = -1;
         try
         {
             currentVersion =
                 Integer.parseInt(watcherInfo.getAttribute(VERSION_ATTRIBUTE));
         }
         catch(Throwable t)
         {
             logger.error("Cannot parse version!", t);
         }

         if(currentVersion != -1 && currentVersion <= subscriber.version)
         {
             logger.warn("Document version is old, ignore it.");
             return;
         }
         else
         subscriber.version = currentVersion;

         // we need watcher list only for our resource
         Element wlist = XMLUtils.locateElement(
                 watcherInfo, WATCHERLIST_ELEMENT, RESOURCE_ATTRIBUTE,
                 parentProvider.getRegistrarConnection()
                     .getAddressOfRecord().getURI().toString());

         if(wlist == null ||
             !wlist.getAttribute(PACKAGE_ATTRIBUTE).equals(PRESENCE_ELEMENT))
         {
             logger.error("Watcher list for us is missing in this document!");
             return;
         }

         NodeList watcherList = wlist.getElementsByTagNameNS(ANY_NS,
                  WATCHER_ELEMENT);
         for(int i = 0; i < watcherList.getLength(); i++)
         {
             Node watcherNode = watcherList.item(i);
             if (watcherNode.getNodeType() != Node.ELEMENT_NODE)
             {
                 logger.error("the watcher node is not an element");
                 return;
             }

             Element watcher = (Element)watcherNode;

             String status = watcher.getAttribute(STATUS_ELEMENT);
             String contactID = getTextContent(watcher);

             //String event - subscribe, approved, deactivated, probation,
             //rejected, timeout, giveup, noresource

             if(status == null || contactID == null)
             {
                 logger.warn("Status or contactID missing for watcher!");
                 continue;
             }

             if(status.equals("waiting") || status.equals("pending"))
             {
                 ContactSipImpl contact = resolveContactID(contactID);

                 if(contact != null)
                 {
                     logger.warn("We are not supposed to have this contact in our " +
                             "list or its just rerequest of authorization!");

                     // if we have this contact in the list
                     // means we have this request already and have shown
                     // dialog to user so skip further processing
                     return;
                 }
                 else
                 {
                 contact = createVolatileContact(contactID);
                 }

                 AuthorizationRequest req = new AuthorizationRequest();
                 AuthorizationResponse response = authorizationHandler
                         .processAuthorisationRequest(req, contact);

                 if(response.getResponseCode() == AuthorizationResponse.ACCEPT)
                 {
                     ssContactList.authorizationAccepted(contact);
                 }
                 else if(response.getResponseCode()
                             == AuthorizationResponse.REJECT)
                 {
                     ssContactList.authorizationRejected(contact);
                 }
                 else if(response.getResponseCode()
                             == AuthorizationResponse.IGNORE)
                 {
                     ssContactList.authorizationIgnored(contact);
                 }
             }
         }
     }

     /**
      * Checks whether to URIs are equal with safe null check.
      * @param uri1 to be compared.
      * @param uri2 to be compared.
      * @return if uri1 is equal to uri2.
      */
    public static boolean isEquals(URI uri1, URI uri2) {
        return (uri1 == null && uri2 == null)
            || (uri1 != null && uri1.equals(uri2));
    }

    /**
     * Changes the Contact image
     * @param contact
     * @param imageUri
     */
    private void updateContactIcon(ContactSipImpl contact, URI imageUri)
    {
        if(isEquals(contact.getImageUri(), imageUri) || imageUri == null)
        {
            return;
        }
        BufferedImageFuture oldImage = contact.getImage();
        BufferedImageFuture newImage = ssContactList.getImage(imageUri);

        if(oldImage == null && newImage == null)
            return;

        contact.setImageUri(imageUri);
        contact.setImage(newImage);
        fireContactPropertyChangeEvent(
                ContactPropertyChangeEvent.PROPERTY_IMAGE,
                contact,
                oldImage,
                newImage);
    }

    /**
     * Unsubscribe to every contact.
     */
    private void unsubscribeToAllContact()
    {
        logger.debug("Trying to unsubscribe to every contact");

        // Send event notifications saying that all our buddies are offline.
        for (ContactSipImpl contact : ssContactList
                .getUniqueContacts(ssContactList.getRootGroup()))
        {
            try
            {
                unsubscribe(contact, false);
            }
            catch (Throwable ex)
            {
                // No need to hash Contact, as its toString() method does that.
                logger.error("Failed to unsubscribe to contact " + contact, ex);
            }
        }
    }

    /**
     * Gets the list of the descendant of an element in the pidf namespace.
     * If the list is empty, we try to get this list in any namespace.
     * This method is useful for being able to read pidf document without any
     * namespace or with a wrong namespace.
     *
     * @param element the base element concerned.
     * @param childName the name of the descendants to match on.
     *
     * @return The list of all the descendant node.
     */
    private NodeList getPidfChilds(Element element, String childName)
    {
        NodeList res;

        res = element.getElementsByTagNameNS(PIDF_NS_VALUE, childName);

        if (res.getLength() == 0)
        {
            res = element.getElementsByTagNameNS(ANY_NS, childName);
        }

        return res;
    }

    /**
     * Cancels the timer which handles all scheduled tasks and disposes of the
     * currently existing tasks scheduled with it.
     */
    protected void cancelTimer()
    {
        /*
         * The timer is being cancelled so the tasks schedules with it are being
         * made obsolete.
         */
        if (republishTask != null)
            republishTask = null;
        if (pollingTask != null)
            pollingTask = null;

        super.cancelTimer();
    }

    /**
     * A <tt>TimerTask</tt> handling refresh of PUBLISH requests.
     */
    private class RePublishTask extends TimerTask
    {
        /**
         * Send a new PUBLISH request to refresh the publication
         */
        public void run()
        {
            Request req = null;
            try
            {
                if (distantPAET != null)
                {
                    req = createPublish(subscriptionDuration, false);
                }
                else
                {
                    // if the last publication failed for any reason, send a
                    // new publication, not a refresh
                    req = createPublish(subscriptionDuration, true);
                }
            }
            catch (OperationFailedException e)
            {
                logger.error("can't create a new PUBLISH message", e);
                return;
            }

            ClientTransaction transac = null;
            try
            {
                transac = parentProvider
                    .getDefaultJainSipProvider().getNewClientTransaction(req);
            }
            catch (TransactionUnavailableException e)
            {
                logger.error("can't create the client transaction", e);
                return;
            }

            try
            {
                transac.sendRequest();
            }
            catch (SipException e)
            {
                logger.error("can't send the PUBLISH request", e);
                return;
            }
        }
    }

    /**
     * A task handling polling of offline contacts.
     */
    private class PollOfflineContactsTask extends TimerTask
    {
        /**
         * Check if we can't subscribe to this contact now
         */
        public void run()
        {
            // send a subscription for every contact
            Iterator<Contact> rootContactsIter
            = getServerStoredContactListRoot().contacts();

            while (rootContactsIter.hasNext())
            {
                ContactSipImpl contact =
                    (ContactSipImpl) rootContactsIter.next();

                // poll this contact
                forcePollContact(contact);
            }

            Iterator<ContactGroup> groupsIter
            = getServerStoredContactListRoot().subgroups();

            while (groupsIter.hasNext())
            {
                ContactGroup group = groupsIter.next();
                Iterator<Contact> contactsIter = group.contacts();

                while (contactsIter.hasNext())
                {
                    ContactSipImpl contact
                    = (ContactSipImpl) contactsIter.next();

                    // poll this contact
                    forcePollContact(contact);
                }
            }
        }
    }

    /**
     * Will wait for every SUBSCRIBE, NOTIFY and PUBLISH transaction
     * to finish before continuing the unsubscription
     */
    private void stopEvents()
    {
        for (byte i = 0; i < 10; i++)
        {
            synchronized (waitedCallIds)
            {
                if (waitedCallIds.size() == 0)
                {
                    break;
                }
            }
            synchronized (this)
            {
                try
                {
                    // Wait 5 s. max (10 loops * 500ms)
                    wait(500);
                }
                catch (InterruptedException e)
                {
                    logger.debug("abnormal behavior, may cause unnecessary CPU use", e);
                }
            }
        }
    }

    protected void registrationStateUnregistering()
    {
        super.registrationStateUnregistering();
        stopEvents();
    }

    protected void registrationStateRegistered()
    {
        super.registrationStateRegistered();

        /*
        * If presence support is enabled and the keep-alive method
        * is REGISTER, we'll get RegistrationState.REGISTERED more
        * than one though we're already registered. If we're
        * receiving such subsequent REGISTERED, we don't have to do
        * anything because we've already set it up in response to
        * the first REGISTERED.
        */
        if ((!presenceEnabled) || (pollingTask != null))
        {
            return;
        }

        forcePollAllContacts();

        // create the new polling task
        pollingTask = new PollOfflineContactsTask();

        // start polling the offline contacts
        timer.schedule(pollingTask, pollingTaskPeriod, pollingTaskPeriod);

        if(this.useDistantPA)
        {
            try
            {
                watcherInfoSubscriber.subscribe(
                    new WatcherInfoSubscriberSubscription(
                        parentProvider.getRegistrarConnection()
                            .getAddressOfRecord()));
            }
            catch (OperationFailedException ex)
            {
                logger.error("Failed to create and send the subcription " +
                        "for watcher info.", ex);
            }
        }
    }

    protected void registrationStateConnectionFailed()
    {
        super.registrationStateConnectionFailed();
        waitedCallIds.clear();
    }

    /**
     * Represents a subscription to the presence.winfo event package.
     *
     * @author Damian Minkov
     */
    private class WatcherInfoSubscriberSubscription
        extends EventPackageSubscriber.Subscription
    {
        private int version = -1;

        /**
         * Initializes a new <tt>Subscription</tt> instance with a specific
         * subscription <tt>Address</tt>/Request URI and an id tag of the
         * associated Event headers of value <tt>null</tt>.
         *
         * @param toAddress the subscription <tt>Address</tt>/Request URI which
         * is to be the target of the SUBSCRIBE requests associated with
         * the new instance
         */
        public WatcherInfoSubscriberSubscription(Address toAddress)
        {
            super(toAddress);
        }

        /**
         * Notifies this <tt>Subscription</tt> that an active NOTIFY
         * <tt>Request</tt> has been received and it may process the
         * specified raw content carried in it.
         *
         * @param requestEvent the <tt>RequestEvent</tt> carrying the full
         * details of the received NOTIFY <tt>Request</tt> including the raw
         * content which may be processed by this <tt>Subscription</tt>
         * @param rawContent   an array of bytes which represents the raw
         * content carried in the body of the received NOTIFY <tt>Request</tt>
         * and extracted from the specified <tt>RequestEvent</tt>
         * for the convenience of the implementers
         */
        @Override
        protected void processActiveRequest(
                RequestEvent requestEvent, byte[] rawContent)
        {
            if (rawContent != null)
                setWatcherInfoStatus(this, new String(rawContent));
        }

        /**
         * Notifies this <tt>Subscription</tt> that a <tt>Response</tt>
         * to a previous SUBSCRIBE <tt>Request</tt> has been received with a
         * status code in the failure range and it may process the status code
         * carried in it.
         *
         * @param responseEvent the <tt>ResponseEvent</tt> carrying the
         * full details of the received <tt>Response</tt> including the status
         * code which may be processed by this <tt>Subscription</tt>
         * @param statusCode the status code carried in the <tt>Response</tt>
         * and extracted from the specified <tt>ResponseEvent</tt>
         * for the convenience of the implementers
         */
        @Override
        protected void processFailureResponse(
                ResponseEvent responseEvent, int statusCode)
        {
            logger.debug("Cannot subscripe to presence watcher info!");
        }

        /**
         * Notifies this <tt>Subscription</tt> that a <tt>Response</tt>
         * to a previous SUBSCRIBE <tt>Request</tt> has been received with a
         * status code in the success range and it may process the status code
         * carried in it.
         *
         * @param responseEvent the <tt>ResponseEvent</tt> carrying the
         * full details of the received <tt>Response</tt> including the status
         * code which may be processed by this <tt>Subscription</tt>
         * @param statusCode the status code carried in the <tt>Response</tt>
         * and extracted from the specified <tt>ResponseEvent</tt>
         * for the convenience of the implementers
         */
        @Override
        protected void processSuccessResponse(
                ResponseEvent responseEvent, int statusCode)
        {
            logger.debug("Subscriped to presence watcher info! status:"
                        + statusCode);
        }

        /**
         * Notifies this <tt>Subscription</tt> that a terminating NOTIFY
         * <tt>Request</tt> has been received and it may process the reason
         * code carried in it.
         *
         * @param requestEvent the <tt>RequestEvent</tt> carrying the
         * full details of the received NOTIFY <tt>Request</tt> including the
         * reason code which may be processed by this <tt>Subscription</tt>
         * @param reasonCode the code of the reason for the termination carried
         * in the NOTIFY <tt>Request</tt> and extracted from the specified
         * <tt>RequestEvent</tt> for the convenience of the implementers.
         */
        @Override
        protected void processTerminatedRequest(
                RequestEvent requestEvent, String reasonCode)
        {
            logger.error("Subscription to presence watcher info terminated!");
        }
    }
}
