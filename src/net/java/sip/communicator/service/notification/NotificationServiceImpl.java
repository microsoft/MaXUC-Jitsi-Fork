/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.notification;

import static net.java.sip.communicator.service.notification.NotificationAction.*;
import static net.java.sip.communicator.service.notification.event.NotificationActionTypeEvent.*;
import static net.java.sip.communicator.service.notification.event.NotificationEventTypeEvent.*;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import net.java.sip.communicator.plugin.notificationwiring.NotificationManager;
import net.java.sip.communicator.service.notification.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;

/**
 * The implementation of the <tt>NotificationService</tt>.
 *
 * @author Yana Stamcheva
 * @author Ingo Bauersachs
 */
class NotificationServiceImpl
    implements NotificationService
{
    private static final String NOTIFICATIONS_PREFIX
        = "net.java.sip.communicator.impl.notifications";

    /**
     * If true, we won't play audio notifications to the user.
     */
    private static final String AUDIO_CONFIG_DISABLED =
        "net.java.sip.communicator.impl.neomedia.AUDIO_CONFIG_DISABLED";

    /**
     * A list of all registered <tt>NotificationChangeListener</tt>s.
     */
    private final List<NotificationChangeListener> changeListeners
        = new Vector<>();

    private final ConfigurationService configService =
        NotificationServiceActivator.getConfigurationService();

    /**
     * A set of all registered event notifications.
     */
    private final Map<String, Notification> defaultNotifications
        = new HashMap<>();

    /**
     * Contains the notification handler per action type.
     */
    private final Map<String, NotificationHandler> handlers
        = new HashMap<>();

    private static final Logger logger
        = Logger.getLogger(NotificationServiceImpl.class);

    /**
     * Queue to cache fired notifications before all handlers are registered.
     */
    private Queue<NotificationData> notificationCache
        = new LinkedList<>();

    /**
     * A set of all registered event notifications.
     */
    private final Map<String, Notification> notifications
        = new HashMap<>();

    /**
     * The set of objects that want to be informed of changes to the mute state
     */
    private final Set<HandlerAddedListener> handlerAddedListeners
                                          = new HashSet<>();

    /**
     * The name of the property holding the URI of the user's custom ringtone,
     * if any.
     */
    private String CUSTOM_RINGTONE_URI_PROPNAME =
            "net.java.sip.communicator.plugin.generalconfig.CUSTOM_RINGTONE_URI";

    /**
     * The name of the property holding the path of the currently active ringtone.
     */
    private String CURRENT_RINGTONE_PATH_PROPNAME =
            "net.java.sip.communicator.plugin.generalconfig.CURRENT_RINGTONE_PATH";

    /**
     * Creates an instance of <tt>NotificationServiceImpl</tt> by loading all
     * previously saved notifications.
     */
    NotificationServiceImpl()
    {
        // Load all previously saved notifications.
        this.loadNotifications();
    }

    /**
     * Adds an object that executes the actual action of a notification action.
     * If the same action type is added twice, the last added wins.
     *
     * @param handler The handler that executes the action.
     */
    public void addActionHandler(NotificationHandler handler)
    {
        if(handler == null)
            throw new IllegalArgumentException("handler cannot be null");

        synchronized(handlers)
        {
            handlers.put(handler.getActionType(), handler);
            if((handlers.size() == NUM_ACTIONS) && (notificationCache != null))
            {
                for(NotificationData event : notificationCache)
                    fireNotification(event);

                notificationCache.clear();
                notificationCache = null;
            }
        }

        // Publish the new handler
        Set<HandlerAddedListener> listeners;
        synchronized (handlerAddedListeners)
        {
            listeners = new HashSet<>(handlerAddedListeners);
        }

        for (HandlerAddedListener listener : listeners)
        {
            listener.onHandlerAdded(handler);
        }
    }

    /**
     * Adds the given <tt>listener</tt> to the list of change listeners.
     *
     * @param listener the listener that we'd like to register to listen for
     * changes in the event notifications stored by this service.
     */
    public void addNotificationChangeListener(
        NotificationChangeListener listener)
    {
        synchronized (changeListeners)
        {
            changeListeners.add(listener);
        }
    }

    /**
     * Checking an action when it is edited (property .default=false).
     * Checking for older versions of the property. If it is older one
     * we migrate it to new configuration using the default values.
     *
     * @param eventType the event type.
     * @param defaultAction the default action which values we will use.
     */
    private void checkDefaultAgainstLoadedNotification
        (String eventType, NotificationAction defaultAction)
    {
        // checking for new sound action properties
        if(defaultAction instanceof SoundNotificationAction)
        {
            SoundNotificationAction soundDefaultAction
                = (SoundNotificationAction)defaultAction;
            SoundNotificationAction soundAction = (SoundNotificationAction)
                getEventNotificationAction(eventType, ACTION_SOUND);

            if (soundAction == null)
            {
                logger.warn("No sound action for event type " + eventType);
                return;
            }

            boolean isSoundNotificationEnabledPropExist
                = getNotificationActionProperty(
                    eventType,
                    defaultAction,
                    "isSoundNotificationEnabled") != null;

            if(!isSoundNotificationEnabledPropExist)
            {
                soundAction.setSoundNotificationEnabled(
                    soundDefaultAction.isSoundNotificationEnabled());
            }

            boolean isSoundPlaybackEnabledPropExist
                = getNotificationActionProperty(
                    eventType,
                    defaultAction,
                    "isSoundPlaybackEnabled") != null;

            if(!isSoundPlaybackEnabledPropExist)
            {
                soundAction.setSoundPlaybackEnabled(
                    soundDefaultAction.isSoundPlaybackEnabled());
            }

            boolean isSoundPCSpeakerEnabledPropExist
                = getNotificationActionProperty(
                    eventType,
                    defaultAction,
                    "isSoundPCSpeakerEnabled") != null;

            if(!isSoundPCSpeakerEnabledPropExist)
            {
                soundAction.setSoundPCSpeakerEnabled(
                    soundDefaultAction.isSoundPCSpeakerEnabled());
            }

            boolean fixDialingLoop = false;

            // hack to fix wrong value:just check whether loop for outgoing call
            // (dialing) has gone into config as 0, should be -1
            if(eventType.equals("Dialing")
               && soundAction.getLoopInterval() == 0)
            {
                soundAction.setLoopInterval(
                    soundDefaultAction.getLoopInterval());
                fixDialingLoop = true;
            }

            if(!(isSoundNotificationEnabledPropExist
                && isSoundPCSpeakerEnabledPropExist
                && isSoundPlaybackEnabledPropExist)
                || fixDialingLoop)
            {
                // this check is done only when the notification
                // is edited and is not default
                saveNotification(
                    eventType,
                    soundAction,
                    soundAction.isEnabled(),
                    false);
            }
        }
    }

    /**
     * Executes a notification data object on the handlers.
     * @param data The notification data to act upon.
     */
    private void fireNotification(NotificationData data)
    {
        logger.entry(data);
        Notification notification = notifications.get(data.getEventType());

        if((notification == null) || !notification.isActive())
            return;

        for(NotificationAction action : notification.getActions().values())
        {
            String actionType = action.getActionType();

            logger.debug("fireNotification with action " + actionType +
                                           ", enabled? " + action.isEnabled());

            if(!action.isEnabled())
                continue;

            NotificationHandler handler = handlers.get(actionType);

            if (handler == null)
            {
                logger.debug("No handler for this action");
                continue;
            }

            switch (actionType)
            {
                case ACTION_POPUP_MESSAGE:
                    Object errorExtra = data.getExtra(
                            NotificationData.MESSAGE_NOTIFICATION_ERROR_EXTRA);
                    boolean isError =
                            (errorExtra == null) ? false : (Boolean) errorExtra;
                    logger.debug("About to request popup message.  Is error? " +
                                         isError);
                    ((PopupMessageNotificationHandler) handler).popupMessage(
                            (PopupMessageNotificationAction) action,
                            data.getTitle(),
                            data.getMessage(),
                            data.getIcon(),
                            data.getExtra(
                                    NotificationData
                                            .MESSAGE_NOTIFICATION_TAG_EXTRA),
                            isError);
                    break;
                case ACTION_LOG_MESSAGE:
                    ((LogMessageNotificationHandler) handler).logMessage(
                            (LogMessageNotificationAction) action,
                            data.getMessage());
                    break;
                case ACTION_SOUND:
                    if (!configService.user().getBoolean(AUDIO_CONFIG_DISABLED,
                                                         false))
                    {
                        SoundNotificationAction soundNotificationAction =
                                (SoundNotificationAction) action;

                        if (soundNotificationAction.isSoundNotificationEnabled() ||
                                soundNotificationAction.isSoundPlaybackEnabled() ||
                                soundNotificationAction.isSoundPCSpeakerEnabled())
                        {
                            ((SoundNotificationHandler) handler).start(
                                    (SoundNotificationAction) action,
                                    data);
                        }
                    }
                    break;
                case ACTION_COMMAND:
                    @SuppressWarnings("unchecked")
                    Map<String, String> cmdargs
                            = (Map<String, String>)
                            data.getExtra(
                                    NotificationData
                                            .COMMAND_NOTIFICATION_HANDLER_CMDARGS_EXTRA);

                    ((CommandNotificationHandler) handler).execute(
                            (CommandNotificationAction) action,
                            cmdargs);
                    break;
                case ACTION_DISPLAY_UI_NOTIFICATIONS:
                    ((UINotificationHandler) handler).handleUINotification(data);
                    break;
            }
        }
        logger.exit();
    }

    /**
     * If there is a registered event notification of the given
     * <tt>eventType</tt> and the event notification is currently activated, we
     * go through the list of registered actions and execute them.
     *
     * @param eventType the type of the event that we'd like to fire a
     *            notification for.
     *
     * @return An object referencing the notification. It may be used to stop a
     *         still running notification. Can be null if the eventType is
     *         unknown or the notification is not active.
     */
    public NotificationData fireNotification(String eventType)
    {
        return fireNotification(eventType, null, null, null);
    }

    /**
     * If there is a registered event notification of the given
     * <tt>eventType</tt> and the event notification is currently activated, the
     * list of registered actions is executed.
     *
     * @param eventType the type of the event that we'd like to fire a
     *            notification for.
     * @param title the title of the given message
     * @param message the message to use if and where appropriate (e.g. with
     *            systray or log notification.)
     * @param icon the icon to show in the notification if and where appropriate
     *
     * @return An object referencing the notification. It may be used to stop a
     *         still running notification. Can be null if the eventType is
     *         unknown or the notification is not active.
     */
    public NotificationData fireNotification(
        String eventType,
        String title,
        String message,
        BufferedImageFuture icon)
    {
        return fireNotification(eventType, title, message, icon, null);
    }

    /**
     * If there is a registered event notification of the given
     * <tt>eventType</tt> and the event notification is currently activated, the
     * list of registered actions is executed.
     *
     * @param eventType the type of the event that we'd like to fire a
     *            notification for.
     * @param message the message to use if and where appropriate (e.g. with
     *            systray or log notification.)
     * @param icon the icon to show in the notification if and where appropriate
     * @param extras additiona/extra {@link NotificationHandler}-specific data
     * to be provided to the firing of the specified notification(s). The
     * well-known keys are defined by the <tt>NotificationData</tt>
     * <tt>XXX_EXTRA</tt> constants.
     * @param title the title of the given message
     * @return An object referencing the notification. It may be used to stop a
     *         still running notification. Can be null if the eventType is
     *         unknown or the notification is not active.
     */
    public NotificationData fireNotification(
            String eventType,
            String title,
            String message,
            BufferedImageFuture icon,
            Map<String,Object> extras)
    {
        logger
            .debug("Entry on method fireNotification. EventType: " + eventType);
        Notification notification = notifications.get(eventType);

        if((notification == null) || !notification.isActive())
            return null;

        NotificationData data
            = new NotificationData(eventType, title, message, icon, extras);

        //cache the notification when the handlers are not yet ready
        if (notificationCache != null)
        {
            logger.debug("caching notification");
            notificationCache.add(data);
        }
        else
        {
            logger.debug("calling fireNotification(data)...");
            fireNotification(data);
        }

        return data;
    }

    /**
     * Notifies all registered <tt>NotificationChangeListener</tt>s that a
     * <tt>NotificationActionTypeEvent</tt> has occurred.
     *
     * @param eventType the type of the event, which is one of ACTION_XXX
     * constants declared in the <tt>NotificationActionTypeEvent</tt> class.
     * @param sourceEventType the <tt>eventType</tt>, which is the parent of the
     * action
     * @param action the notification action
     */
    private void fireNotificationActionTypeEvent(
                                        String eventType,
                                        String sourceEventType,
                                        NotificationAction action)
    {
        NotificationActionTypeEvent event
            = new NotificationActionTypeEvent(  this,
                                                eventType,
                                                sourceEventType,
                                                action);

        for(NotificationChangeListener listener : changeListeners)
        {
            switch (eventType)
            {
                case ACTION_ADDED:
                    listener.actionAdded(event);
                    break;
                case ACTION_REMOVED:
                    listener.actionRemoved(event);
                    break;
                case ACTION_CHANGED:
                    listener.actionChanged(event);
                    break;
            }
        }
    }

    /**
     * Notifies all registered <tt>NotificationChangeListener</tt>s that a
     * <tt>NotificationEventTypeEvent</tt> has occurred.
     *
     * @param eventType the type of the event, which is one of EVENT_TYPE_XXX
     * constants declared in the <tt>NotificationEventTypeEvent</tt> class.
     * @param sourceEventType the <tt>eventType</tt>, for which this event is
     * about
     */
    private void fireNotificationEventTypeEvent(String eventType,
                                                String sourceEventType)
    {
        logger.debug("Dispatching NotificationEventType Change. Listeners="
                 + changeListeners.size()
                 + " evt=" + eventType);

        NotificationEventTypeEvent event
            = new NotificationEventTypeEvent(this, eventType, sourceEventType);

        for (NotificationChangeListener listener : changeListeners)
        {
            if (eventType.equals(EVENT_TYPE_ADDED))
            {
                listener.eventTypeAdded(event);
            }
            else if (eventType.equals(EVENT_TYPE_REMOVED))
            {
                listener.eventTypeRemoved(event);
            }
        }
    }

    /**
     * Gets a list of handler for the specified action type.
     *
     * @param actionType the type for which the list of handlers should be
     *            retrieved or <tt>null</tt> if all handlers shall be returned.
     */
    public Iterable<NotificationHandler> getActionHandlers(String actionType)
    {
        if (actionType != null)
        {
            NotificationHandler handler = handlers.get(actionType);
            Set<NotificationHandler> ret;

            if (handler == null)
                ret = Collections.emptySet();
            else
                ret = Collections.singleton(handler);
            return ret;
        }
        else
            return handlers.values();
    }

    /**
     * Returns the notification action corresponding to the given
     * <tt>eventType</tt> and <tt>actionType</tt>.
     *
     * @param eventType the type of the event that we'd like to retrieve.
     * @param actionType the type of the action that we'd like to retrieve a
     * descriptor for.
     * @return the notification action of the action to be executed
     * when an event of the specified type has occurred.
     */
    public NotificationAction getEventNotificationAction(
                                                            String eventType,
                                                            String actionType)
    {
        Notification notification = notifications.get(eventType);

        return
            (notification == null) ? null : notification.getAction(actionType);
    }

    /**
     * Getting a notification property directly from configuration service.
     * Used to check do we have an updated version of already saved/edited
     * notification configurations. Detects old configurations.
     *
     * @param eventType the event type
     * @param action the action which property to check.
     * @param property the property name without the action prefix.
     * @return the property value or null if missing.
     * @throws IllegalArgumentException when the event ot action is not
     * found.
     */
    private String getNotificationActionProperty(
        String eventType,
        NotificationAction action,
        String property)
            throws IllegalArgumentException
    {
        String eventTypeNodeName = null;
        String actionTypeNodeName = null;

        List<String> eventTypes = configService.user()
                .getPropertyNamesByPrefix(NOTIFICATIONS_PREFIX, true);

        for (String eventTypeRootPropName : eventTypes)
        {
            String eType = configService.user().getString(eventTypeRootPropName);
            if(eType.equals(eventType))
                eventTypeNodeName = eventTypeRootPropName;
        }

        // If we didn't find the given event type in the configuration
        // there is not need to further check
        if(eventTypeNodeName == null)
        {
            throw new IllegalArgumentException("Missing event type node");
        }

        // Go through contained actions.
        String actionPrefix = eventTypeNodeName + ".actions";

        List<String> actionTypes = configService.user()
                .getPropertyNamesByPrefix(actionPrefix, true);

        for (String actionTypeRootPropName : actionTypes)
        {
            String aType = configService.user().getString(actionTypeRootPropName);
            if(aType.equals(action.getActionType()))
                actionTypeNodeName = actionTypeRootPropName;
        }

        // If we didn't find the given actionType in the configuration
        // there is no need to further check
        if(actionTypeNodeName == null)
            throw new IllegalArgumentException("Missing action type node");

        return
            (String)
                configService.user().getProperty(actionTypeNodeName + "." + property);
    }

    /**
     * Returns an iterator over a list of all events registered in this
     * notification service. Each line in the returned list consists of
     * a String, representing the name of the event (as defined by the plugin
     * that registered it).
     *
     * @return an iterator over a list of all events registered in this
     * notifications service
     */
    public Iterable<String> getRegisteredEvents()
    {
        return Collections.unmodifiableSet(
            notifications.keySet());
    }

    /**
     * Finds the <tt>EventNotification</tt> corresponding to the given
     * <tt>eventType</tt> and returns its isActive status.
     *
     * @param eventType the name of the event (as defined by the plugin that's
     * registered it) that we are checking.
     * @return <code>true</code> if actions for the specified <tt>eventType</tt>
     * are activated, <code>false</code> - otherwise. If the given
     * <tt>eventType</tt> is not contained in the list of registered event
     * types - returns <code>false</code>.
     */
    public boolean isActive(String eventType)
    {
        Notification eventNotification
            = notifications.get(eventType);

        if(eventNotification == null)
            return false;

        return eventNotification.isActive();
    }

    private boolean isDefault(String eventType, String actionType)
    {
        List<String> eventTypes = configService.user()
                .getPropertyNamesByPrefix(NOTIFICATIONS_PREFIX, true);

        for (String eventTypeRootPropName : eventTypes)
        {
            String eType
                = configService.user().getString(eventTypeRootPropName);

            if(!eType.equals(eventType))
                continue;

            List<String> actions = configService.user()
                .getPropertyNamesByPrefix(
                    eventTypeRootPropName + ".actions", true);

            for (String actionPropName : actions)
            {
                String aType
                    = configService.user().getString(actionPropName);

                if(!aType.equals(actionType))
                    continue;

                Object isDefaultdObj =
                    configService.user().getProperty(actionPropName + ".default");

                // if setting is missing we accept it is true
                // this way we override old saved settings
                if(isDefaultdObj == null)
                    return true;
                else
                    return Boolean.parseBoolean((String)isDefaultdObj);
            }
        }
        return true;
    }

    private boolean isEnabled(String configProperty)
    {
        Object isEnabledObj = configService.user().getProperty(configProperty);

        // if setting is missing we accept it is true
        // this way we not affect old saved settings
        if(isEnabledObj == null)
            return true;
        else
            return Boolean.parseBoolean((String)isEnabledObj);
    }

    /**
     * Loads all previously saved event notifications.
     */
    private void loadNotifications()
    {
        List<String> eventTypes = configService.user()
                .getPropertyNamesByPrefix(NOTIFICATIONS_PREFIX, true);

        for (String eventTypeRootPropName : eventTypes)
        {
            boolean isEventActive =
                isEnabled(eventTypeRootPropName + ".active");

            String eventType
                = configService.user().getString(eventTypeRootPropName);

            List<String> actions = configService.user()
                .getPropertyNamesByPrefix(
                    eventTypeRootPropName + ".actions", true);

            for (String actionPropName : actions)
            {
                String actionType = configService.user().getString(actionPropName);

                NotificationAction action = null;

                switch (actionType)
                {
                    case ACTION_SOUND:
                        String soundFileDescriptor
                                = configService.user().getString(
                                actionPropName + ".soundFileDescriptor");

                        String loopInterval
                                = configService.user().getString(
                                actionPropName + ".loopInterval");

                        boolean isSoundNotificationEnabled
                                = configService.user().getBoolean(
                                actionPropName + ".isSoundNotificationEnabled",
                                (soundFileDescriptor != null));

                        boolean isSoundPlaybackEnabled
                                = configService.user().getBoolean(
                                actionPropName + ".isSoundPlaybackEnabled",
                                false);

                        boolean isSoundPCSpeakerEnabled
                                = configService.user().getBoolean(
                                actionPropName + ".isSoundPCSpeakerEnabled",
                                false);

                        action = new SoundNotificationAction(
                                soundFileDescriptor,
                                Integer.parseInt(loopInterval),
                                isSoundNotificationEnabled,
                                isSoundPlaybackEnabled,
                                isSoundPCSpeakerEnabled);
                        break;
                    case ACTION_POPUP_MESSAGE:
                        String defaultMessage
                                = configService.user().getString(
                                actionPropName + ".defaultMessage");

                        action = new PopupMessageNotificationAction(
                                defaultMessage);
                        break;
                    case ACTION_LOG_MESSAGE:
                        String logType
                                = configService.user().getString(
                                actionPropName + ".logType");

                        action = new LogMessageNotificationAction(logType);
                        break;
                    case ACTION_COMMAND:
                        String commandDescriptor
                                = configService.user().getString(
                                actionPropName + ".commandDescriptor");

                        action = new CommandNotificationAction(commandDescriptor);
                        break;

                    case ACTION_DISPLAY_UI_NOTIFICATIONS:
                        action = new UINotificationAction();
                        break;
                    // Ignore unknown actions when loading notifications from
                    // disk. This prevents the Notification Service from
                    // crashing, thus allowing downgrade to an older client
                    // version that supports fewer notification types
                    default:
                        logger.info("Ignoring unknown action type " + actionType);
                        continue;
                }

                action.setEnabled(isEnabled(actionPropName + ".enabled"));

                // Load the data in the notifications table.
                Notification notification = notifications.get(eventType);
                if(notification == null)
                {
                    notification = new Notification(eventType);
                    notifications.put(eventType, notification);
                }
                notification.setActive(isEventActive);
                notification.addAction(action);
            }
        }
    }

    /**
     * Creates a new default <tt>EventNotification</tt> or obtains the
     * corresponding existing one and registers a new action in it.
     *
     * @param eventType the name of the event (as defined by the plugin that's
     * registering it) that we are setting an action for.
     * @param action the <tt>NotificationAction</tt> to register
     */
    public void registerDefaultNotificationForEvent(
        String eventType,
        NotificationAction action)
    {
        if(isDefault(eventType, action.getActionType()))
        {
            NotificationAction h =
                getEventNotificationAction(eventType,
                    action.getActionType());

            boolean isNew = false;
            if(h == null)
            {
                isNew = true;
                h = action;
            }

            this.saveNotification(  eventType,
                                    action,
                                    h.isEnabled(),
                                    true);

            Notification notification = null;

            if(notifications.containsKey(eventType))
                notification = notifications.get(eventType);
            else
            {
                notification = new Notification(eventType);
                notifications.put(eventType, notification);
            }

            notification.addAction(action);

            // We fire the appropriate event depending on whether this is an
            // already existing actionType or a new one.
            fireNotificationActionTypeEvent(
                isNew
                    ? ACTION_ADDED
                    : ACTION_CHANGED,
                eventType,
                action);
        }
        else
            checkDefaultAgainstLoadedNotification(eventType, action);

        // now store this default events if we want to restore them
        Notification notification = null;

        if(defaultNotifications.containsKey(eventType))
            notification = defaultNotifications.get(eventType);
        else
        {
            notification = new Notification(eventType);

            defaultNotifications.put(eventType, notification);
        }

        notification.addAction(action);
    }

    /**
     * Creates a new default <tt>EventNotification</tt> or obtains the corresponding
     * existing one and registers a new action in it.
     *
     * @param eventType the name of the event (as defined by the plugin that's
     * registering it) that we are setting an action for.
     * @param actionType the type of the action that is to be executed when the
     * specified event occurs (could be one of the ACTION_XXX fields).
     * @param actionDescriptor a String containing a description of the action
     * (a URI to the sound file for audio notifications or a command line for
     * exec action types) that should be executed when the action occurs.
     * @param defaultMessage the default message to use if no specific message
     * has been provided when firing the notification.
     */
    public void registerDefaultNotificationForEvent(    String eventType,
                                                        String actionType,
                                                        String actionDescriptor,
                                                        String defaultMessage)
    {
        logger.debug("Registering default event " + eventType + "/" +
            actionType + "/" + actionDescriptor + "/" + defaultMessage);

        if(isDefault(eventType, actionType))
        {
            NotificationAction action =
                getEventNotificationAction(eventType, actionType);
            boolean isNew = false;

            if(action == null)
            {
                isNew = true;

                switch (actionType)
                {
                    case ACTION_SOUND:
                        action = new SoundNotificationAction(actionDescriptor,
                                                             -1);
                        break;
                    case ACTION_LOG_MESSAGE:
                        action = new LogMessageNotificationAction(
                                LogMessageNotificationAction.INFO_LOG_TYPE);
                        break;
                    case ACTION_POPUP_MESSAGE:
                        action = new PopupMessageNotificationAction(
                                defaultMessage);
                        break;
                    case ACTION_COMMAND:
                        action = new CommandNotificationAction(actionDescriptor);
                        break;
                }
            }

            this.saveNotification(  eventType,
                                    action,
                                    action.isEnabled(),
                                    true);

            Notification notification = null;

            if(notifications.containsKey(eventType))
                notification = notifications.get(eventType);
            else
            {
                notification = new Notification(eventType);
                notifications.put(eventType, notification);
            }

            notification.addAction(action);

            // We fire the appropriate event depending on whether this is an
            // already existing actionType or a new one.
            fireNotificationActionTypeEvent(
                isNew
                    ? ACTION_ADDED
                    : ACTION_CHANGED,
                eventType,
                action);
        }

        // now store this default events if we want to restore them
        Notification notification = null;

        if(defaultNotifications.containsKey(eventType))
            notification = defaultNotifications.get(eventType);
        else
        {
            notification = new Notification(eventType);
            defaultNotifications.put(eventType, notification);
        }

        NotificationAction action = null;
        switch (actionType)
        {
            case ACTION_SOUND:
                action = new SoundNotificationAction(actionDescriptor, -1);
                break;
            case ACTION_LOG_MESSAGE:
                action = new LogMessageNotificationAction(
                        LogMessageNotificationAction.INFO_LOG_TYPE);
                break;
            case ACTION_POPUP_MESSAGE:
                action = new PopupMessageNotificationAction(defaultMessage);
                break;
            case ACTION_COMMAND:
                action = new CommandNotificationAction(actionDescriptor);
                break;
        }

        notification.addAction(action);
    }

    /**
     * Creates a new <tt>EventNotification</tt> or obtains the corresponding
     * existing one and registers a new action in it.
     *
     * @param eventType the name of the event (as defined by the plugin that's
     * registering it) that we are setting an action for.
     * @param action the <tt>NotificationAction</tt> responsible for
     * handling the given <tt>actionType</tt>
     */
    public void registerNotificationForEvent(   String eventType,
                                                NotificationAction action)
    {
        Notification notification = null;

        if(notifications.containsKey(eventType))
            notification = notifications.get(eventType);
        else
        {
            notification = new Notification(eventType);
            notifications.put(eventType, notification);

            this.fireNotificationEventTypeEvent(
                EVENT_TYPE_ADDED, eventType);
        }

        Object existingAction = notification.addAction(action);

        // We fire the appropriate event depending on whether this is an
        // already existing actionType or a new one.
        if (existingAction != null)
        {
            fireNotificationActionTypeEvent(
                ACTION_CHANGED,
                eventType,
                action);
        }
        else
        {
            fireNotificationActionTypeEvent(
                ACTION_ADDED,
                eventType,
                action);
        }

        // Save the notification through the ConfigurationService.
        this.saveNotification(eventType,
            action,
            true,
            false);
    }

    /**
     * Creates a new <tt>EventNotification</tt> or obtains the corresponding
     * existing one and registers a new action in it.
     *
     * @param eventType the name of the event (as defined by the plugin that's
     * registering it) that we are setting an action for.
     * @param actionType the type of the action that is to be executed when the
     * specified event occurs (could be one of the ACTION_XXX fields).
     * @param actionDescriptor a String containing a description of the action
     * (a URI to the sound file for audio notifications or a command line for
     * exec action types) that should be executed when the action occurs.
     * @param defaultMessage the default message to use if no specific message
     * has been provided when firing the notification.
     */
    public void registerNotificationForEvent(   String eventType,
                                                String actionType,
                                                String actionDescriptor,
                                                String defaultMessage)
    {
        logger.debug("Registering event " + eventType + "/" +
            actionType + "/" + actionDescriptor + "/" + defaultMessage);

        switch (actionType)
        {
            case ACTION_SOUND:
                Notification notification = defaultNotifications.get(eventType);
                SoundNotificationAction action =
                        (SoundNotificationAction) notification.getAction(
                                ACTION_SOUND);
                registerNotificationForEvent(
                        eventType,
                        new SoundNotificationAction(
                                actionDescriptor,
                                action.getLoopInterval()));
                break;
            case ACTION_LOG_MESSAGE:
                registerNotificationForEvent(eventType,
                                             new LogMessageNotificationAction(
                                                     LogMessageNotificationAction.INFO_LOG_TYPE));
                break;
            case ACTION_POPUP_MESSAGE:
                registerNotificationForEvent(eventType,
                                             new PopupMessageNotificationAction(
                                                     defaultMessage));
                break;
            case ACTION_COMMAND:
                registerNotificationForEvent(eventType,
                                             new CommandNotificationAction(
                                                     actionDescriptor));
                break;
        }
    }

    /**
     * Removes an object that executes the actual action of notification action.
     * @param actionType The handler type to remove.
     */
    public void removeActionHandler(String actionType)
    {
        if(actionType == null)
            throw new IllegalArgumentException("actionType cannot be null");

        synchronized(handlers)
        {
            handlers.remove(actionType);
        }
    }

    /**
     * Removes the <tt>EventNotification</tt> corresponding to the given
     * <tt>eventType</tt> from the table of registered event notifications.
     *
     * @param eventType the name of the event (as defined by the plugin that's
     * registering it) to be removed.
     */
    public void removeEventNotification(String eventType)
    {
        notifications.remove(eventType);

        this.fireNotificationEventTypeEvent(
            EVENT_TYPE_REMOVED, eventType);
    }

    /**
     * Removes the given actionType from the list of actions registered for the
     * given <tt>eventType</tt>.
     *
     * @param eventType the name of the event (as defined by the plugin that's
     * registering it) for which we'll remove the notification.
     * @param actionType the type of the action that is to be executed when the
     * specified event occurs (could be one of the ACTION_XXX fields).
     */
    public void removeEventNotificationAction(  String eventType,
                                                String actionType)
    {
        Notification notification
            = notifications.get(eventType);

        if(notification == null)
            return;

        NotificationAction action = notification.getAction(actionType);

        if(action == null)
            return;

        notification.removeAction(actionType);

        saveNotification(
            eventType,
            action,
            false,
            false);

        fireNotificationActionTypeEvent(
            ACTION_REMOVED,
            eventType,
            action);
    }

    /**
     * Removes the given <tt>listener</tt> from the list of change listeners.
     *
     * @param listener the listener that we'd like to remove
     */
    public void removeNotificationChangeListener(
        NotificationChangeListener listener)
    {
        synchronized (changeListeners)
        {
            changeListeners.remove(listener);
        }
    }

    /**
     * Deletes all registered events and actions
     * and registers and saves the default events as current.
     */
    public void restoreDefaults()
    {
        for (String eventType : new Vector<>(notifications.keySet()))
        {
            Notification notification = notifications.get(eventType);

            for (String actionType
                    : new Vector<>(notification.getActions().keySet()))
                removeEventNotificationAction(eventType, actionType);

            removeEventNotification(eventType);
        }

        for (Map.Entry<String, Notification> entry
                : defaultNotifications.entrySet())
        {
            String eventType = entry.getKey();
            Notification notification = entry.getValue();

            for (NotificationAction action : notification.getActions().values())
                registerNotificationForEvent(eventType, action);
        }
    }

    /**
     * Saves the event notification given by these parameters through the
     * <tt>ConfigurationService</tt>.
     *
     * @param eventType the name of the event
     * @param action the notification action to change
     * @param isActive is the event active
     * @param isDefault is it a default one
     */
    private void saveNotification(  String eventType,
                                    NotificationAction action,
                                    boolean isActive,
                                    boolean isDefault)
    {
        String eventTypeNodeName = null;
        String actionTypeNodeName = null;

        List<String> eventTypes = configService.user()
                .getPropertyNamesByPrefix(NOTIFICATIONS_PREFIX, true);

        for (String eventTypeRootPropName : eventTypes)
        {
            String eType = configService.user().getString(eventTypeRootPropName);
            if(eType.equals(eventType))
                eventTypeNodeName = eventTypeRootPropName;
        }

        // If we didn't find the given event type in the configuration we save
        // it here.
        if(eventTypeNodeName == null)
        {
            eventTypeNodeName = NOTIFICATIONS_PREFIX
                                + ".eventType"
                                + Long.toString(System.currentTimeMillis());

            configService.user().setProperty(eventTypeNodeName, eventType);
        }

        // if we set active/inactive for the whole event notification
        if(action == null)
        {
            configService.user().setProperty(
                eventTypeNodeName + ".active",
                Boolean.toString(isActive));
            return;
        }

        // Go through contained actions.
        String actionPrefix = eventTypeNodeName + ".actions";

        List<String> actionTypes = configService.user()
                .getPropertyNamesByPrefix(actionPrefix, true);

        for (String actionTypeRootPropName : actionTypes)
        {
            String aType = configService.user().getString(actionTypeRootPropName);
            if(aType.equals(action.getActionType()))
                actionTypeNodeName = actionTypeRootPropName;
        }

        Map<String, Object> configProperties = new HashMap<>();

        // If we didn't find the given actionType in the configuration we save
        // it here.
        if(actionTypeNodeName == null)
        {
            actionTypeNodeName = actionPrefix
                                    + ".actionType"
                                    + Long.toString(System.currentTimeMillis());

            configProperties.put(actionTypeNodeName, action.getActionType());
        }

        if(action instanceof SoundNotificationAction)
        {
            SoundNotificationAction soundAction
                = (SoundNotificationAction) action;

            configProperties.put(
                actionTypeNodeName + ".soundFileDescriptor",
                soundAction.getDescriptor());

            configProperties.put(
                actionTypeNodeName + ".loopInterval",
                soundAction.getLoopInterval());

            configProperties.put(
                actionTypeNodeName + ".isSoundNotificationEnabled",
                soundAction.isSoundNotificationEnabled());

            configProperties.put(
                actionTypeNodeName + ".isSoundPlaybackEnabled",
                soundAction.isSoundPlaybackEnabled());

            configProperties.put(
                actionTypeNodeName + ".isSoundPCSpeakerEnabled",
                soundAction.isSoundPCSpeakerEnabled());
        }
        else if(action instanceof PopupMessageNotificationAction)
        {
            PopupMessageNotificationAction messageAction
                = (PopupMessageNotificationAction) action;

            configProperties.put(
                actionTypeNodeName + ".defaultMessage",
                messageAction.getDefaultMessage());
        }
        else if(action instanceof LogMessageNotificationAction)
        {
            LogMessageNotificationAction logMessageAction
                = (LogMessageNotificationAction) action;

            configProperties.put(
                actionTypeNodeName + ".logType",
                logMessageAction.getLogType());
        }
        else if(action instanceof CommandNotificationAction)
        {
            CommandNotificationAction commandAction
                = (CommandNotificationAction) action;

            configProperties.put(
                actionTypeNodeName + ".commandDescriptor",
                commandAction.getDescriptor());
        }

        configProperties.put(
            actionTypeNodeName + ".enabled",
            Boolean.toString(isActive));

        configProperties.put(
            actionTypeNodeName + ".default",
            Boolean.toString(isDefault));

        configService.user().setProperties(configProperties);
    }

    /**
     * Finds the <tt>EventNotification</tt> corresponding to the given
     * <tt>eventType</tt> and marks it as activated/deactivated.
     *
     * @param eventType the name of the event, which actions should be activated
     * /deactivated.
     * @param isActive indicates whether to activate or deactivate the actions
     * related to the specified <tt>eventType</tt>.
     */
    public void setActive(String eventType, boolean isActive)
    {
        Notification eventNotification
            = notifications.get(eventType);

        if(eventNotification == null)
            return;

        eventNotification.setActive(isActive);
        saveNotification(eventType, null, isActive, false);
    }

    /**
     * Stops a notification if notification is continuous, like playing sounds
     * in loop. Do nothing if there are no such events currently processing.
     *
     * @param data the data that has been returned when firing the event..
     */
    public void stopNotification(NotificationData data)
    {
        Notification notification = notifications.get(data.getEventType());

        if (notification == null || !notification.isActive())
        {
            return;
        }

        for(NotificationAction action : notification.getActions().values())
        {
            String actionType = action.getActionType();

            logger.debug("stopNotification with action " + actionType +
                                 ", enabled? " + action.isEnabled());

            if (!action.isEnabled())
                continue;

            NotificationHandler handler = handlers.get(actionType);

            if (handler == null)
            {
                logger.debug("No handler for this action");
                continue;
            }

            switch(actionType)
            {
                case ACTION_SOUND:
                    Iterable<NotificationHandler> soundHandlers
                            = getActionHandlers(NotificationAction.ACTION_SOUND);

                    // There could be no sound action handler for this event type
                    if (soundHandlers != null)
                    {
                        for (NotificationHandler soundHandler : soundHandlers)
                        {
                            if (soundHandler instanceof SoundNotificationHandler)
                                ((SoundNotificationHandler) soundHandler).stop(data);
                        }
                    }
                    break;
                case ACTION_DISPLAY_UI_NOTIFICATIONS:
                    if (NotificationManager.MISSED_CALL.equals(data.getEventType()))
                    {
                        ((UINotificationHandler) handler).clearCallNotifications();
                    }
                    else if (NotificationManager.INCOMING_MESSAGE.equals(data.getEventType()))
                    {
                        ((UINotificationHandler) handler).clearChatNotifications(data);
                    }
                    break;
            }
        }
    }

    /**
     * Tells if the given sound notification is currently played.
     *
     * @param data Additional data for the event.
     */
    public boolean isPlayingNotification(NotificationData data)
    {
        boolean isPlaying = false;

        Iterable<NotificationHandler> soundHandlers
            = getActionHandlers(NotificationAction.ACTION_SOUND);

        // There could be no sound action handler for this event type
        if (soundHandlers != null)
        {
            for (NotificationHandler handler : soundHandlers)
            {
                if (handler instanceof SoundNotificationHandler)
                {
                    isPlaying
                        |= ((SoundNotificationHandler) handler).isPlaying(data);
                }
            }
        }

        return isPlaying;
    }

    @Override
    public void addHandlerAddedListener(HandlerAddedListener listener)
    {
        synchronized (handlerAddedListeners)
        {
            handlerAddedListeners.add(listener);
        }
    }

    @Override
    public void removeHandlerAddedListener(HandlerAddedListener listener)
    {
        synchronized (handlerAddedListeners)
        {
            handlerAddedListeners.remove(listener);
        }
    }

    @Override
    public void checkAndSetCustomRingtone()
    {
        String ringtoneURI =
                configService.user().getString(CUSTOM_RINGTONE_URI_PROPNAME, "");

        if (ringtoneURI.equals(""))
        {
            logger.debug("No custom ringtone has been set.");
            return;
        }

        // Remove the 'file:' from the front of the URI to give the actual file
        // location, and escape characters like spaces in the filepath.
        File ringtoneFile = new File(URLDecoder.decode(ringtoneURI.substring(5),
                                                       StandardCharsets.UTF_8));

        if (ringtoneFile.exists())
        {
            logger.info("Custom ringtone was valid. The URI was: " + ringtoneURI);
            return;
        }

        logger.info("Custom ringtone has been deleted.");
        String path = "resources/sounds/incomingCall.wav";
        configService.user().setProperty(CURRENT_RINGTONE_PATH_PROPNAME, path);

        registerNewRingtoneNotification(path);
    }

    /**
     * Register a new sound as the notification for incoming calls and meetings.
     *
     * @param ringtonePath The file path of the new ringtone
     */
    public void registerNewRingtoneNotification(String ringtonePath)
    {
        logger.debug("Registering new ringtone for NotificationService");

        // Get the existing notification action for incoming calls and
        // conferences, so that we can keep all the settings the same
        // except for the sound file used.
        String[] actions = {NotificationManager.INCOMING_CALL,
                NotificationManager.INCOMING_CONFERENCE};

        for (String action : actions)
        {
            SoundNotificationAction oldAction = (SoundNotificationAction)(
                            getEventNotificationAction(
                            action,
                            NotificationAction.ACTION_SOUND));

            SoundNotificationAction newAction = new SoundNotificationAction(
                    ringtonePath,
                    oldAction.getLoopInterval(),
                    oldAction.isSoundNotificationEnabled(),
                    oldAction.isSoundPlaybackEnabled(),
                    oldAction.isSoundPCSpeakerEnabled());

            registerNotificationForEvent(action, newAction);
        }
    }
}
