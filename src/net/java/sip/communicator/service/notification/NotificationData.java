/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.notification;

import java.util.*;

import org.jitsi.service.resources.*;

/**
 * Object to cache fired notifications before all handler implementations are
 * ready registered.
 *
 * @author Ingo Bauersachs
 */
public class NotificationData
{
    /**
     * The name/key of the <tt>NotificationData</tt> extra which is provided to
     * {@link CommandNotificationHandler#execute(CommandNotificationAction,
     * Map)} i.e. a <tt>Map&lt;String,String&gt;</tt> which is known by the
     * (argument) name <tt>cmdargs</tt>.
     */
    public static final String COMMAND_NOTIFICATION_HANDLER_CMDARGS_EXTRA
        = "CommandNotificationHandler.cmdargs";

    /**
     * The name/key of the <tt>NotificationData</tt> extra which is provided to
     * {@link PopupMessageNotificationHandler#popupMessage(PopupMessageNotificationAction, String, String, BufferedImageFuture, Object, boolean)} and
     * {@link UINotificationHandler#handleUINotification(NotificationData)} i.e. an
     * <tt>Object</tt> which is known by the (argument) name <tt>tag</tt>.
     * The value associated with this key contains additional information about
     * the chat which caused the notification to be generated.
     */
    public static final String MESSAGE_NOTIFICATION_TAG_EXTRA
        = "MessageNotification.tag";

    /**
     * The name/key of the <tt>NotificationData</tt> extra which is provided to
     * {@link PopupMessageNotificationHandler#popupMessage(PopupMessageNotificationAction, String, String, BufferedImageFuture, Object, boolean)} i.e. an
     * <tt>Object</tt> which is known by the (argument) name <tt>error</tt>.
     */
    public static final String MESSAGE_NOTIFICATION_ERROR_EXTRA
        = "MessageNotification.error";

    /**
     * The name/key of the <tt>NotificationData</tt> extra which is provided to
     * {@link SoundNotificationHandler} i.e. a <tt>Callable&lt;Boolean&gt;</tt>
     * which is known as the condition which determines whether looping sounds
     * are to continue playing.
     */
    public static final String SOUND_NOTIFICATION_HANDLER_LOOP_CONDITION_EXTRA
        = "SoundNotificationHandler.loopCondition";

    /**
     * The name/key of the <tt>NotificationData</tt> extra provided to
     * {@link UINotificationHandler#handleUINotification(NotificationData data)}. The value
     * associated with this key contains the number of messages waiting.
     */
    public static final String MESSAGE_WAITING_COUNT_EXTRA
            = "MessageWaitingCount.Extra";

    private final String eventType;

    /**
     * The {@link NotificationHandler}-specific extras provided to this
     * instance. The keys are among the <tt>XXX_EXTRA</tt> constants defined by
     * the <tt>NotificationData</tt> class.
     */
    private final Map<String, Object> extras;

    private final BufferedImageFuture icon;
    private final String message;
    private final String title;

    /**
     * Creates a new instance of this class.
     *
     * @param eventType the type of the event that we'd like to fire a
     * notification for.
     * @param title the title of the given message
     * @param message the message to use if and where appropriate (e.g. with
     * systray or log notification.)
     * @param icon the icon to show in the notification if and where appropriate
     * @param extras additional/extra {@link NotificationHandler}-specific data
     * to be provided by the new instance to the various
     * <tt>NotificationHandler</tt>s
     */
    public NotificationData(
            String eventType,
            String title,
            String message,
            BufferedImageFuture icon,
            Map<String, Object> extras)
    {
        this.eventType = eventType;
        this.title = title;
        this.message = message;
        this.icon = icon;
        this.extras = extras;
    }

    /**
     * Gets the type of the event that we'd like to fire a notification for
     *
     * @return the eventType
     */
    public String getEventType()
    {
        return eventType;
    }

    /**
     * Gets the {@link NotificationHandler}-specific extras provided to this
     * instance.
     *
     * @return the <tt>NotificationHandler</tt>-specific extras provided to this
     * instance. The keys are among the <tt>XXX_EXTRA</tt> constants defined by
     * the <tt>NotificationData</tt> class
     */
    public Map<String, Object> getExtras()
    {
        return Collections.unmodifiableMap(extras);
    }

    /**
     * Gets the {@link NotificationHandler}-specific extra provided to this
     * instance associated with a specific key.
     *
     * @param key the key whose associated <tt>NotificationHandler</tt>-specific
     * extra is to be returned. Well known keys are defined by the
     * <tt>NotificationData</tt> class as the <tt>XXX_EXTRA</tt> constants.
     * @return the <tt>NotificationHandler</tt>-specific extra provided to this
     * instance associated with the specified <tt>key</tt>
     */
    public Object getExtra(String key)
    {
        return (extras == null) ? null : extras.get(key);
    }

    /**
     * Gets the icon to show in the notification if and where appropriate.
     *
     * @return the icon
     */
    public BufferedImageFuture getIcon()
    {
        return icon;
    }

    /**
     * Gets the message to use if and where appropriate (e.g. with systray or
     * log notification).
     *
     * @return the message
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * Gets the title of the given message.
     *
     * @return the title
     */
    public String getTitle()
    {
        return title;
    }
}
