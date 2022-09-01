/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.service.commportal.ClassOfServiceService;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.util.*;

/**
 * Message Waiting Indication Event rfc3842.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class OperationSetMessageWaitingSipImpl
    implements OperationSetMessageWaiting,
                RegistrationStateChangeListener
{
    /**
     * Our class logger.
     */
    private static final Logger logger
        = Logger.getLogger(OperationSetMessageWaitingSipImpl.class);

    /**
     * The provider that created us.
     */
    private final ProtocolProviderServiceSipImpl provider;

    /**
     * The timer which will handle all the scheduled tasks
     */
    private final TimerScheduler timer = new TimerScheduler();

    /**
     * The configuration service.
     */
    private static final ConfigurationService configService =
                                         SipActivator.getConfigurationService();

    /**
     * The name of the event package supported by
     * <tt>OperationSetMessageWaitingSipImpl</tt> in SUBSCRIBE
     * and NOTIFY requests.
     */
    static final String EVENT_PACKAGE = "message-summary";

    /**
     * The content sub-type of the content supported in NOTIFY requests handled
     * by <tt>OperationSetMessageWaitingSipImpl</tt>.
     */
    private static final String CONTENT_SUB_TYPE = "simple-message-summary";

    /**
     * The time in seconds after which a <tt>Subscription</tt> should be expired
     * by the <tt>OperationSetMessageWaitingSipImpl</tt> instance
     * which manages it.
     */
    private static final int SUBSCRIPTION_DURATION = 3600;

    /**
     * The <code>EventPackageSubscriber</code> which provides the ability of
     * this instance to act as a subscriber
     * for the message-summary event package.
     */
    private EventPackageSubscriber messageWaitingSubscriber = null;

    /**
     * How many seconds before a timeout should we refresh our state
     */
    private static final int REFRESH_MARGIN = 60;

    /**
     * Listeners that would receive event notifications for new messages.
     */
    private List<MessageWaitingListener> messageWaitingNotificationListeners =
        new ArrayList<>();

    /**
     * The account associated with the last SIP NOTIFY received.
     */
    private String mAccount;

    /**
     * The total number of unread messages we currently know about. Reset to 0
     * each time a new SIP NOTIFY is received.
     */
    private int mCurrentUnreadMessages = 0;

    /**
     * The last notification event fired.
     */
    private MessageWaitingEvent mLastNotification;

    /**
     * Creates this operation set.
     *
     * @param provider
     */
    OperationSetMessageWaitingSipImpl(
            ProtocolProviderServiceSipImpl provider)
    {
        this.provider = provider;
        this.provider.addRegistrationStateChangeListener(this);

        /*
         * Answer with NOT_IMPLEMENTED to message-summary SUBSCRIBEs in order to
         * not have its ServerTransaction remaining in the SIP stack forever .
         */
        this.provider.registerMethodProcessor(
                Request.SUBSCRIBE,
                new MethodProcessorAdapter()
                        {
                            @Override
                            public boolean processRequest(
                                    RequestEvent requestEvent)
                            {
                                return
                                    OperationSetMessageWaitingSipImpl.this
                                            .processRequest(requestEvent);
                            }
                        });
    }

    /**
     * Registers a <tt>MessageWaitingListener</tt> with this operation set so
     * that it gets notifications of new messages waiting.
     *
     * @param listener the <tt>MessageWaitingListener</tt> to register.
     */
    public void addMessageWaitingNotificationListener(
            MessageWaitingListener listener)
    {
        synchronized (messageWaitingNotificationListeners)
        {
            if (!messageWaitingNotificationListeners.contains(listener))
            {
                messageWaitingNotificationListeners.add(listener);
            }
        }
    }

    /**
     * Unregisters <tt>listener</tt> so that it won't receive any further
     * notifications upon new messages waiting.
     *
     * @param listener the <tt>MessageWaitingListener</tt> to unregister.
     */
    public void removeMessageWaitingNotificationListener(
            MessageWaitingListener listener)
    {
        synchronized (messageWaitingNotificationListeners)
        {
            messageWaitingNotificationListeners.remove(listener);
        }
    }

    /**
     * The method is called by a <code>ProtocolProviderService</code>
     * implementation whenever a change in the registration state of the
     * corresponding provider had occurred.
     *
     * @param evt the event describing the status change.
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        if (evt.getNewState().equals(RegistrationState.REGISTERED))
        {
            messageWaitingSubscriber =
                new EventPackageSubscriber(
                        provider,
                        EVENT_PACKAGE,
                        SUBSCRIPTION_DURATION,
                        CONTENT_SUB_TYPE,
                        timer,
                        REFRESH_MARGIN)
                {
                    /**
                     * We may receive some message-waiting notifications
                     * out of dialog but we still want to process them, as
                     * the server is just not rfc compliant.
                     * This happens with asterisk when using qualify option
                     * for configured user(user is behind nat and we * ping it),
                     * as the sent packet pings delete our subscription dialog.
                     *
                     * @param callId the CallId associated with the
                     * <tt>Subscription</tt> to be retrieved
                     * @return the Subscription.
                     */
                    @Override
                    protected Subscription getSubscription(String callId)
                    {
                        Subscription resultSub = super.getSubscription(callId);

                        if(resultSub != null)
                            return resultSub;

                        // lets find our subscription and return it
                        // as we cannot find it by callid
                        Object[] subs = getSubscriptions();

                        for(Object s : subs)
                            if(s instanceof MessageSummarySubscriber)
                                return (MessageSummarySubscriber)s;

                        return null;
                    }
                };

            try
            {
                final Address subscribeAddress = getSubscribeAddress();

                if (subscribeAddress != null)
                {
                    messageWaitingSubscriber.subscribe(
                        new MessageSummarySubscriber(subscribeAddress,
                            messageWaitingSubscriber));
                }
            }
            catch(Throwable e)
            {
                logger.error("Error subscribing for mailbox", e);
            }
        }
        else if (evt.getNewState().equals(RegistrationState.UNREGISTERING))
        {
            if(messageWaitingSubscriber != null)
            {
                try
                {
                    messageWaitingSubscriber.unsubscribe(
                        getSubscribeAddress(), false);
                }
                catch(Throwable t)
                {
                    logger.error("Error unsubscribing mailbox", t);
                }
            }
        }
    }

    /**
     * Returns the subscribe address for current account mailbox, default
     * or configured.
     * @return the subscribe address for current account mailbox.
     * @throws ParseException
     */
    private Address getSubscribeAddress()
        throws ParseException
    {
        String vmAddressURI = (String)provider.getAccountID()
            .getAccountProperty(
                    ProtocolProviderFactory.VOICEMAIL_URI);

        if (StringUtils.isNullOrEmpty(vmAddressURI))
            return provider.getRegistrarConnection()
                    .getAddressOfRecord();
        else
            return provider.parseAddressString(
                    vmAddressURI);
    }

    /**
     * Fires new event to update message waiting indicators.
     *
     * @param account the account to reach the messages.
     * @param unreadMessages number of unread messages.
     */
    private void fireMessageWaitingEvent(
        String account,
        int unreadMessages)
    {
        synchronized (this)
        {
            if (mLastNotification != null &&
                mLastNotification.getUnreadMessages() == unreadMessages)
            {
                // no new information, skip event;
                logger.debug("No change from current state - skipping");
                return;
            }
        }

        MessageWaitingEvent event =
            new MessageWaitingEvent(provider, account, unreadMessages);

        // Save this notification as the lastNotification.
        mLastNotification = event;

        ArrayList<MessageWaitingListener> listeners;

        synchronized (messageWaitingNotificationListeners)
        {
            listeners = new ArrayList<>(messageWaitingNotificationListeners);
        }

        logger.debug("Sending MWI notifications to " + listeners.size() +
                         " listeners: " + event);
        for (MessageWaitingListener listener : listeners)
            listener.messageWaitingNotify(event);
    }

    /**
     * Informs the operation set of the number of unread messages found in the
     * inbox, from a particular source. Fires a message waiting notification
     * containing the total number of unread messages from all sources.
     *
     * @param unreadMessages The number of unread messages found.
     */
    public void totalMessagesAndFireNotification(int unreadMessages)
    {
        int totalUnreadMessages = unreadMessages + mCurrentUnreadMessages;
        mCurrentUnreadMessages = totalUnreadMessages;
        fireMessageWaitingEvent(mAccount, totalUnreadMessages);
    }

    /**
     * Sends a {@link Response#NOT_IMPLEMENTED} <tt>Response</tt> to a specific
     * {@link Request#SUBSCRIBE} <tt>Request</tt> with <tt>message-summary</tt>
     * event type.
     *
     * @param requestEvent the <tt>Request</tt> to process
     * @return <tt>true</tt> if the specified <tt>Request</tt> has been
     * successfully processed; otherwise, <tt>false</tt>
     */
    private boolean processRequest(RequestEvent requestEvent)
    {
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

        if (!EVENT_PACKAGE.equalsIgnoreCase(eventType))
            return false;

        boolean processed = false;

        if (Request.SUBSCRIBE.equals(request.getMethod()))
        {
            processed
                = EventPackageSupport.sendNotImplementedResponse(
                        provider,
                        requestEvent);
        }

        return processed;
    }

    /**
     * Subscribes and receive result for message-summary event package.
     */
    private class MessageSummarySubscriber
        extends EventPackageSubscriber.Subscription
    {
        /**
         * Matching messages count
         * group 1 - new messages count.
         * group 2 - old messages count.
         * group 3 - new urgent messages count.
         * group 4 - old urgent messages count.
         *
         * Note we currently only care about the number of new messages.
         */
        private Pattern messageWaitingCountPattern = Pattern.compile(
                "(\\d+)/(\\d+) \\((\\d+)/(\\d+)\\)");

        /**
         * Back reference to the parent EventPackageSubscriber.
         */
        private EventPackageSubscriber eventPackageSubscriber;

        /**
         * Initializes a new <tt>Subscription</tt> instance with a specific
         * subscription <tt>Address</tt>/Request URI and an id tag of the
         * associated Event headers of value <tt>null</tt>.
         *
         * @param toAddress the subscription <tt>Address</tt>/Request URI which
         *            is to be the target of the SUBSCRIBE requests associated
         *            with the new instance
         */
        public MessageSummarySubscriber(Address toAddress,
            EventPackageSubscriber eventPackageSubscriber)
        {
            super(toAddress);
            this.eventPackageSubscriber = eventPackageSubscriber;
        }

        /**
         * Notifies this <tt>Subscription</tt> that an active NOTIFY
         * <tt>Request</tt> has been received and it may process the
         * specified raw content carried in it.
         *
         * @param requestEvent the <tt>RequestEvent</tt> carrying the full details of
         *                     the received NOTIFY <tt>Request</tt> including the raw
         *                     content which may be processed by this
         *                     <tt>Subscription</tt>
         * @param rawContent   an array of bytes which represents the raw content carried
         *                     in the body of the received NOTIFY <tt>Request</tt>
         *                     and extracted from the specified <tt>RequestEvent</tt>
         *                     for the convenience of the implementers
         */
        @Override
        protected void processActiveRequest(
                RequestEvent requestEvent, byte[] rawContent)
        {
            // If the message body is missing we have nothing more to do here.
            if (rawContent == null || rawContent.length <= 0)
            {
                logger.debug("No content in MWI NOTIFY - ignoring");
                return;
            }

            // We have new data, so reset the current total.
            mCurrentUnreadMessages = 0;

            try
            {
                String messageAccount =
                    provider.getAccountID().getAccountPropertyString(
                                ProtocolProviderFactory.VOICEMAIL_CHECK_URI);

                BufferedReader input = new BufferedReader(new InputStreamReader(
                        new ByteArrayInputStream(rawContent)));
                String line;

                boolean gotMessageCounts = false;
                int unreadMessages = 0;

                while((line = input.readLine()) != null)
                {
                    String lcaseLine = line.toLowerCase();
                    if (lcaseLine.startsWith("messages-waiting"))
                    {
                        String messageWaitingStr  =
                            line.substring(line.indexOf(":") + 1).trim();
                        if (messageWaitingStr.equalsIgnoreCase("yes"))
                        {
                            logger.debug("Messages waiting");
                        }
                        else
                        {
                            // No messages waiting. No point reading the rest of
                            // the NOTIFY content, just fire an empty event.
                            logger.debug("No messages waiting");
                            mAccount = messageAccount;
                            totalMessagesAndFireNotification(0);
                            return;
                        }
                    }
                    else if (lcaseLine.startsWith("voice-message")
                            || lcaseLine.startsWith("fax-message")
                            || lcaseLine.startsWith("multimedia-message")
                            || lcaseLine.startsWith("pager-message")
                            || lcaseLine.startsWith("text-message")
                            || lcaseLine.startsWith("none"))
                    {
                        String messagesCountValue =
                                line.substring(line.indexOf(":") + 1).trim();

                        Matcher matcher =
                            messageWaitingCountPattern.matcher(messagesCountValue);

                        if(matcher.find())
                        {
                            // We only care about the number of unread messages.
                            unreadMessages += Integer.valueOf(matcher.group(1));
                            gotMessageCounts = true;
                        }
                    }
                }

                // as defined in rfc3842
                //'In some cases, detailed message summaries are not available.'
                // this is a simple workaround that will trigger a notification
                // for one message so we can inform the user that there are
                // messages waiting
                if (!gotMessageCounts)
                {
                    unreadMessages = 1;
                }

                mAccount = messageAccount;

                // If fax is enabled in the Class Of Service, we must ask
                // the CommPortal service to fetch the unread fax counts,
                // because this information is not delivered in the SIP NOTIFY.
                boolean faxEnabled = configService.user()
                    .getBoolean(ClassOfServiceService.CONFIG_FAX_ENABLED, false);

                if (faxEnabled)
                {
                    // Don't fire a notification yet. Store the number of
                    // messages we've found, get the fax counts, then let the
                    // callback fire a notification.
                    mCurrentUnreadMessages = unreadMessages;
                    SipActivator.getCommPortalService().getFaxMessageCounts();
                }
                else
                {
                    // We have all the information we need - fire a
                    // notification.
                    totalMessagesAndFireNotification(unreadMessages);
                }

            }
            catch(IOException ex)
            {
                logger.error("Error processing message waiting info");
            }
        }

        /**
         * Notifies this <tt>Subscription</tt> that a <tt>Response</tt>
         * to a previous SUBSCRIBE <tt>Request</tt> has been received with a
         * status code in the failure range and it may process the status code
         * carried in it.
         *
         * @param responseEvent the <tt>ResponseEvent</tt> carrying the full details
         *                      of the received <tt>Response</tt> including the status
         *                      code which may be processed by this
         *                      <tt>Subscription</tt>
         * @param statusCode    the status code carried in the <tt>Response</tt> and
         *                      extracted from the specified <tt>ResponseEvent</tt>
         *                      for the convenience of the implementers
         */
        @Override
        protected void processFailureResponse(
                ResponseEvent responseEvent, int statusCode)
        {
            logger.debug("Processing failed: " + statusCode);
        }

        /**
         * Notifies this <tt>Subscription</tt> that a <tt>Response</tt>
         * to a previous SUBSCRIBE <tt>Request</tt> has been received with a
         * status code in the success range and it may process the status code
         * carried in it.
         *
         * @param responseEvent the <tt>ResponseEvent</tt> carrying the full details
         *                      of the received <tt>Response</tt> including the status
         *                      code which may be processed by this
         *                      <tt>Subscription</tt>
         * @param statusCode    the status code carried in the <tt>Response</tt> and
         *                      extracted from the specified <tt>ResponseEvent</tt>
         *                      for the convenience of the implementers
         */
        @Override
        protected void processSuccessResponse(
                ResponseEvent responseEvent, int statusCode)
        {
            logger.debug("Processing success: " + statusCode);
        }

        /**
         * Notifies this <tt>Subscription</tt> that a terminating NOTIFY
         * <tt>Request</tt> has been received and it may process the reason
         * code carried in it.
         *
         * @param requestEvent the <tt>RequestEvent</tt> carrying the full details of
         *                     the received NOTIFY <tt>Request</tt> including the
         *                     reason code which may be processed by this
         *                     <tt>Subscription</tt>
         * @param reasonCode   the code of the reason for the termination carried in the
         *                     NOTIFY <tt>Request</tt> and extracted from the
         *                     specified <tt>RequestEvent</tt> for the convenience of
         *                     the implementers
         */
        @Override
        protected void processTerminatedRequest(
                RequestEvent requestEvent, String reasonCode)
        {
            logger.info("Processing terminated (retrying): " + reasonCode);

            // The SIP registrar has terminated our subscription early.  Not
            // sure why it would do that but let's make sure we retry the
            // subscription later.
            eventPackageSubscriber.retrySubscribe(this,
                EventPackageSubscriber.retryMargin);
        }
    }

    /**
     * Returns the last fired message waiting event or null if no such event
     * has been fired.
     *
     * @return the last message waiting event.
     */
    public MessageWaitingEvent getLastMessageWaitingNotification()
    {
        return mLastNotification;
    }
}
