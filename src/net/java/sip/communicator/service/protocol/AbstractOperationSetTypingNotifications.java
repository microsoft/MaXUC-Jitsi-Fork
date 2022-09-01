/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Represents a default implementation of
 * <tt>OperationSetTypingNotifications</tt> in order to make it easier for
 * implementers to provide complete solutions while focusing on
 * implementation-specific details.
 *
 * @param <T> the type of the <tt>ProtocolProviderService</tt> implementation
 * providing the <tt>AbstractOperationSetTypingNotifications</tt> implementation
 *
 * @author Lubomir Marinov
 */
public abstract class AbstractOperationSetTypingNotifications<T extends ProtocolProviderService>
    implements OperationSetTypingNotifications
{
    /**
     * The <tt>Logger</tt> used by the
     * <tt>AbstractOperationSetTypingNotifications</tt> class and its instances
     * for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(AbstractOperationSetTypingNotifications.class);

    private final Map<String, ScheduledExecutorService> mScheduleExecutorServices;
    /**
     * The provider that created us.
     */
    protected final T parentProvider;

    /**
     * The list of currently registered <tt>TypingNotificationsListener</tt>s.
     */
    private final List<TypingNotificationsListener> typingNotificationsListeners
        = new ArrayList<>();

    /**
     * Initializes a new <tt>AbstractOperationSetTypingNotifications</tt>
     * instance created by a specific <tt>ProtocolProviderService</tt> instance.
     *
     * @param parentProvider the <tt>ProtocolProviderService</tt> which creates
     * the new instance
     */
    protected AbstractOperationSetTypingNotifications(T parentProvider)
    {
        this.parentProvider = parentProvider;
        this.mScheduleExecutorServices = new HashMap<String, ScheduledExecutorService>();
    }

    /**
     * Adds <tt>listener</tt> to the list of listeners registered for receiving
     * <tt>TypingNotificationEvent</tt>s.
     *
     * @param listener the <tt>TypingNotificationsListener</tt> listener that
     * we'd like to add
     * @see OperationSetTypingNotifications#addTypingNotificationsListener(
     * TypingNotificationsListener)
     */
    public void addTypingNotificationsListener(
        TypingNotificationsListener listener)
    {
        synchronized (typingNotificationsListeners)
        {
            if (!typingNotificationsListeners.contains(listener))
                typingNotificationsListeners.add(listener);
        }
    }

    /**
     * Utility method throwing an exception if the stack is not properly
     * initialized.
     *
     * @throws IllegalStateException if the underlying stack is not registered
     * and initialized
     */
    protected void assertConnected()
        throws IllegalStateException
    {
        if (parentProvider == null)
            throw
                new IllegalStateException(
                        "The provider must be non-null"
                            + " before being able to communicate.");
        if (!parentProvider.isRegistered())
            throw
                new IllegalStateException(
                        "The provider must be signed on the service"
                            + " before being able to communicate.");
    }

    /**
     * Delivers a <tt>TypingNotificationEvent</tt> to all registered listeners.
     *
     * @param sourceContact the contact who has sent the notification
     * @param evtCode the code of the event to deliver
     */
    public void fireTypingNotificationsEvent(
        Contact sourceContact,
        OperationSetTypingNotifications.TypingState evtCode)
    {
        logger.entry(sourceContact);

        TypingNotificationsListener[] listeners;
        synchronized (typingNotificationsListeners)
        {
            listeners
                = typingNotificationsListeners
                    .toArray(
                        new TypingNotificationsListener[
                                typingNotificationsListeners.size()]);
        }

        // No need to hash Contact, as its toString() method does that.
        logger.debug(
            "Dispatching a TypingNotificationEvent to "
                + listeners.length
                + " listeners. Contact "
                + sourceContact
                + " has now a typing status of "
                + evtCode);

        TypingNotificationEvent evt
            = new TypingNotificationEvent(sourceContact, evtCode);

        for (TypingNotificationsListener listener : listeners)
            listener.typingNotificationReceived(evt);
        // This code snippet schedule a task to send a NOT_TYPING notification in
        // the case of a TYPING notification is stucked acting as a back-stop
        //in case we miss a NOT_TYPING notification. The schedule time is a trade-off
        //between accuracy and probability of missing an event.
        //NOTE: If the sender continues to typing for longer then 2 minutes uninterruptedly,
        //which means without any pause greater then 5 seconds, the recipient will have a
        //false negative on typing notification (the recipient will not see "{sender} is typing...").
        ScheduledExecutorService executor = null;
        if (evt.getTypingState() == TypingState.TYPING)
        {
            logger.info(
                "TYPING notification sent, scheduling a NOT_TYPING notification");

            TypingNotificationEvent scheduleEvt = new TypingNotificationEvent(
                sourceContact, TypingState.NOT_TYPING);
            Runnable sendNotTypingNotificationTask = () -> {
                logger.info(
                    "Probably stuck TYPING notification, sending a NOT_TYPING notification");
                for (TypingNotificationsListener listener : listeners)
                    listener.typingNotificationReceived(scheduleEvt);
            };
            executor = Executors.newScheduledThreadPool(1);
            executor.schedule(sendNotTypingNotificationTask, 120,
                TimeUnit.SECONDS);
            mScheduleExecutorServices.put(sourceContact.getAddress(), executor);
        }
        else
        {
            executor =
                mScheduleExecutorServices.get(sourceContact.getAddress());
            if (executor != null && !executor.isShutdown())
            {
                executor.shutdownNow();
                logger.info("Executor shutdown");
            }
        }
        logger.exit();
    }

     /**
     * Removes <tt>listener</tt> from the list of listeners registered for
     * receiving <tt>TypingNotificationEvent</tt>s.
     *
     * @param listener the <tt>TypingNotificationsListener</tt> listener that
     * we'd like to remove
     * @see OperationSetTypingNotifications#removeTypingNotificationsListener(
     * TypingNotificationsListener)
     */
    public void removeTypingNotificationsListener(
        TypingNotificationsListener listener)
    {
        synchronized (typingNotificationsListeners)
        {
            typingNotificationsListeners.remove(listener);
        }
    }

    public void sendTypingNotification(Contact notifiedContact,
                                       ContactResource resource,
                                       TypingState typingState)
        throws IllegalStateException, IllegalArgumentException
    {
        // This method does not use the specified resource by default, instead
        // it sends the notification to all registered resources for the
        // contact. A protocol-specific implementation must be written to send
        // the notification just to the specified resource.
        sendTypingNotification(notifiedContact, typingState);
    }
}
