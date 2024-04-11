/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.notificationwiring;

import java.beans.*;
import java.lang.ref.*;
import java.util.*;
import java.util.concurrent.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.protocol.event.*;
import org.jitsi.service.resources.*;

import org.osgi.framework.*;

import net.java.sip.communicator.service.conference.ConferenceService;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.CallConference.*;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications.TypingState;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

/**
 * Listens to various events which are related to the display and/or playback of
 * notifications and shows/starts or hides/stops the notifications in question.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class NotificationManager
    implements CallChangeListener,
               CallListener,
               CallPeerConferenceListener,
               CallPeerListener,
               CallPeerSecurityListener,
               FileTransferListener,
               ServiceListener,
               TypingNotificationsListener,
               MessageWaitingListener
{
    /**
     * Default event type for a busy call.
     */
    public static final String BUSY_CALL = "BusyCall";

    /**
     * Default event type for call been saved using a recorder.
     */
    public static final String CALL_SAVED = "CallSaved";

    /**
     * Default event type for security error on a call.
     */
    public static final String CALL_SECURITY_ERROR = "CallSecurityError";

    /**
     * Default event type for activated security on a call.
     */
    public static final String CALL_SECURITY_ON = "CallSecurityOn";

    /**
     * The image used, when a contact has no photo specified.
     */
    public static final ImageID DEFAULT_USER_PHOTO
        = new ImageID("service.gui.DEFAULT_USER_PHOTO");

    /**
     * Default event type for dialing.
     */
    public static final String DIALING = "Dialing";

    /**
     * Default event type for hanging up calls.
     */
    public static final String HANG_UP = "HangUp";

    /**
     * The cache of <tt>BufferedImage</tt> instances which we have already
     * loaded by <tt>ImageID</tt> and which we store so that we do not have to
     * load them again.
     */
    private static final Map<ImageID, BufferedImageFuture> images
        = new Hashtable<>();

    private static final String BG_CONTACTS_ENABLED_PROP =
            "net.java.sip.communicator.BG_CONTACTS_ENABLED";

    private static final String CONFIG_DISTINCT_RINGTONES_ENABLED_PROP =
            "net.java.sip.communicator.plugin.generalconfig.ringtonesconfig.DISTINCT_ENABLED";

    /**
     * Default event type for receiving calls (incoming calls).
     */
    public static final String INCOMING_CALL = "IncomingCall";

    /**
     * Default event type for missed calls
     */
    public static final String MISSED_CALL = "MissedCall";

    /**
     * Default event type for a change in the number of messages waiting
     * (voicemail and fax)
     */
    public static final String MESSAGE_WAITING = "MessageWaiting";

    /**
     * Default event type for call waiting (incoming calls when existing
     * calls are active).
     */
    public static final String CALL_WAITING = "CallWaiting";

    /**
     * Default event type for incoming file transfers.
     */
    public static final String INCOMING_FILE = "IncomingFile";

    /**
     * Default event type for receiving messages.
     */
    public static final String INCOMING_MESSAGE = "IncomingMessage";

    /**
     * Default event type for receiving conference invitations.
     */
    public static final String INCOMING_CONFERENCE = "IncomingConference";

    /**
     * Default event type for receiving chat room member status updates
     */
    public static final String CHAT_ROOM_MEMBER_CHANGED = "ChatRoomMemberChanged";

    /**
     * The <tt>Logger</tt> used by the <tt>NotificationManager</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(NotificationManager.class);

    private static final ConfigurationService configService = NotificationWiringActivator.getConfigurationService();

    /**
     * Default event type for outgoing calls.
     */
    public static final String OUTGOING_CALL = "OutgoingCall";

    /**
     * Default event type for
     * proactive notifications (typing notifications when chatting).
     */
    public static final String PROACTIVE_NOTIFICATION = "ProactiveNotification";

    /**
     * Default event type when a secure message received.
     */
    public static final String SECURITY_MESSAGE = "SecurityMessage";

    /**
     * Key in the config service for the country code
     */
    private static final String COUNTRY_CODE_KEY =
              "net.java.sip.communicator.impl.protocol.commportal.COUNTRY_CODE";

    /**
     * Fires a chat message notification for the given event type through the
     * <tt>NotificationService</tt>.
     *
     * @param chatContact the chat contact to which the chat message corresponds;
     * the chat contact could be a Contact or a ChatRoom.
     * @param smsNumber The SMS number (will be null if this isn't an SMS)
     * @param eventType the event type for which we fire a notification
     * @param messageTitle the title of the message
     * @param message the content of the message
     * @param isError whether this message is an error
     */
    public static void fireChatNotification(Object chatContact,
                                            String smsNumber,
                                            String eventType,
                                            String messageTitle,
                                            String message,
                                            boolean isError)
    {
        logger.debug("entry on method fireChatNotification");
        NotificationService notificationService
            = NotificationWiringActivator.getNotificationService();

        if (notificationService == null)
        {
            logger.debug("NotificationService is null, returning");
            return;
        }

        UIService uiService = NotificationWiringActivator.getUIService();

        Chat chatPanel = null;
        BufferedImageFuture contactIcon = null;
        if (chatContact instanceof Contact)
        {
            Contact contact = (Contact) chatContact;

            if (uiService != null)
            {
                chatPanel = uiService.getChat(contact, smsNumber);
            }

            BufferedImageFuture cachedContactAvatar = AvatarCacheUtils.getCachedAvatar(contact);

            if (cachedContactAvatar != null)
            {
                contactIcon = cachedContactAvatar;
            }
            else
            {
                contactIcon = contact.getImage();

                if (contactIcon != null)
                {
                    logger.info("Found new avatar for contact " + contact + " - add it to the cache");
                    AvatarCacheUtils.cacheAvatar(contact, contactIcon);
                }
            }

            if (contactIcon == null)
            {
                contactIcon = getImage(DEFAULT_USER_PHOTO);
            }
        }
        else if (chatContact instanceof ChatRoom)
        {
            ChatRoom chatRoom = (ChatRoom) chatContact;

            // For muted rooms we don't want to send notification events.
            if (chatRoom.isMuted())
            {
                return;
            }

            if (uiService != null)
            {
                chatPanel = uiService.getChat(chatRoom, true);
            }
        }
        else if (chatContact instanceof String)
        {
            String contact = (String) chatContact;

            if (uiService != null)
            {
                chatPanel = uiService.getChat(contact);
            }

             contactIcon = getImage(DEFAULT_USER_PHOTO);
        }

        // We only fire the notification if the chat panel is not
        // already in focus (otherwise it's not necessary and too annoying!)
        if ((chatPanel == null) ||
            !(INCOMING_MESSAGE.equals(eventType)) ||
            !(chatPanel.isChatFocused()))
        {
            Object tag = (smsNumber == null) ? chatContact : smsNumber;
            Map<String,Object> extras = new HashMap<>();

            extras.put(
                NotificationData.MESSAGE_NOTIFICATION_TAG_EXTRA,
                tag);
            extras.put(
                NotificationData.MESSAGE_NOTIFICATION_ERROR_EXTRA,
                isError);
            notificationService.fireNotification(
                eventType,
                messageTitle,
                message,
                contactIcon,
                extras);
        }
    }

    /**
     * Fires a notification for the given event type through the
     * <tt>NotificationService</tt>. The event type is one of the static
     * constants defined in the <tt>NotificationManager</tt> class.
     * <p>
     * <b>Note</b>: The uses of the method at the time of this writing do not
     * take measures to stop looping sounds if the respective notifications use
     * them i.e. there is implicit agreement that the notifications fired
     * through the method do not loop sounds. Consequently, the method passes
     * arguments to <tt>NotificationService</tt> so that sounds are played once
     * only.
     * </p>
     *
     * @param eventType the event type for which we want to fire a notification
     */
    private static void fireNotification(String eventType)
    {
        NotificationService notificationService
            = NotificationWiringActivator.getNotificationService();

        if (notificationService != null)
            notificationService.fireNotification(eventType);
    }

    /**
     * Fires a notification for the given event type through the
     * <tt>NotificationService</tt>. The event type is one of the static
     * constants defined in the <tt>NotificationManager</tt> class.
     *
     * @param eventType the event type for which we want to fire a notification
     * @param loopCondition the method which will determine whether any sounds
     * played as part of the specified notification will continue looping
     * @return a reference to the fired notification to stop it.
     */
    private static NotificationData fireNotification(
            String eventType,
            Callable<Boolean> loopCondition)
    {
        return fireNotification(eventType, null, null, null, null, loopCondition);
    }

    /**
     * Fires a notification through the <tt>NotificationService</tt> with a
     * specific event type, a specific message title and a specific message.
     * <p>
     * <b>Note</b>: The uses of the method at the time of this writing do not
     * take measures to stop looping sounds if the respective notifications use
     * them i.e. there is implicit agreement that the notifications fired
     * through the method do not loop sounds. Consequently, the method passes
     * arguments to <tt>NotificationService</tt> so that sounds are played once
     * only.
     * </p>
     *
     * @param eventType the event type of the notification to be fired
     * @param messageTitle the title of the message to be displayed by the
     * notification to be fired if such a display is supported
     * @param message the message to be displayed by the notification to be
     * fired if such a display is supported
     */
    private static void fireNotification(
            String eventType,
            String messageTitle,
            String message)
    {
        NotificationService notificationService
            = NotificationWiringActivator.getNotificationService();

        if (notificationService != null)
        {
            notificationService.fireNotification(
                        eventType,
                        messageTitle,
                        message,
                        null);
        }
    }

    /**
     * Fires a message notification for the given event type through the
     * <tt>NotificationService</tt>.
     *
     * @param eventType     the event type for which we fire a notification
     * @param messageTitle  the title of the message
     * @param message       the content of the message
     * @param bgTag         business group tag, or null depending on context
     * @param cmdargs       the value to be provided to
     *                      {@link CommandNotificationHandler#execute(CommandNotificationAction, Map)} as the
     *                      <tt>cmdargs</tt> argument
     * @param loopCondition the method which will determine whether any sounds played as part of the specified
     *                      notification will continue looping
     * @return a reference to the fired notification to stop it.
     */
    private static NotificationData fireNotification(
            String eventType,
            String messageTitle,
            String message,
            SoundNotificationAction.BgTag bgTag,
            Map<String,String> cmdargs,
            Callable<Boolean> loopCondition)
    {
        NotificationService notificationService
            = NotificationWiringActivator.getNotificationService();

        if (notificationService == null)
            return null;
        else
        {
            Map<String,Object> extras = new HashMap<>();

            if (cmdargs != null)
            {
                extras.put(NotificationData.COMMAND_NOTIFICATION_HANDLER_CMDARGS_EXTRA, cmdargs);
            }

            if (loopCondition != null)
            {
                extras.put(NotificationData.SOUND_NOTIFICATION_HANDLER_LOOP_CONDITION_EXTRA, loopCondition);
            }

            if (bgTag != null)
            {
                extras.put(NotificationData.NOTIFICATION_SERVICE_SOUND_BG_TAG_EXTRA, bgTag);
            }

            return
                notificationService.fireNotification(
                        eventType,
                        messageTitle,
                        message,
                        null,
                        extras);
        }
    }

    /**
     * Fires a message notification for the given event type through the
     * <tt>NotificationService</tt>.
     *
     * @param eventType the event type for which we fire a notification
     * @param messageTitle the title of the message
     * @param message the content of the message
     * @param extras additional/extra NotificationHandler-specific data to be
     * provided to the firing of the specified notification(s). The well-known
     * keys are defined by the NotificationData XXX_EXTRA constants.
     * @return a reference to the fired notification to stop it.
     */
    private static NotificationData fireNotification(
        String eventType,
        String messageTitle,
        String message,
        Map<String,Object> extras)
    {
        NotificationService notificationService
            = NotificationWiringActivator.getNotificationService();

        NotificationData notificationData = null;

        if (notificationService != null)
        {
            notificationData = notificationService.fireNotification(
                        eventType,
                        messageTitle,
                        message,
                        null,
                        extras);
        }

        return notificationData;
    }

    /**
     * Loads an image from a given image identifier.
     *
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public static BufferedImageFuture getImage(ImageID imageID)
    {
        /*
         * If we were mapping ImageID to null, we would be using the method
         * Map.containsKey. However, that does not seem to be the case.
         */
        BufferedImageFuture image = images.get(imageID);

        if (image == null)
        {
            image = NotificationWiringActivator.getResources().getBufferedImage(imageID.getId());
            images.put(imageID, image);
        }

        return image;
    }

    /**
     * Returns all <tt>ProtocolProviderFactory</tt>s obtained from the bundle
     * context.
     *
     * @return all <tt>ProtocolProviderFactory</tt>s obtained from the bundle
     *         context
     */
    public static Map<Object, ProtocolProviderFactory>
        getProtocolProviderFactories()
    {
        ServiceReference<?>[] serRefs = null;
        try
        {
            // get all registered provider factories
            serRefs
                = NotificationWiringActivator.bundleContext.getServiceReferences(
                        ProtocolProviderFactory.class.getName(),
                        null);
        }
        catch (InvalidSyntaxException e)
        {
            logger.error("NotificationManager : " + e);
        }

        Map<Object, ProtocolProviderFactory>
            providerFactoriesMap = new Hashtable<>();

        if (serRefs != null)
        {
            for (ServiceReference<?> serRef : serRefs)
            {
                ProtocolProviderFactory providerFactory
                    = (ProtocolProviderFactory)
                        NotificationWiringActivator.bundleContext.getService(serRef);

                providerFactoriesMap.put(
                        serRef.getProperty(ProtocolProviderFactory.PROTOCOL),
                        providerFactory);
            }
        }
        return providerFactoriesMap;
    }

    /**
     * Returns all protocol providers currently registered.
     * @return all protocol providers currently registered.
     */
    public static List<ProtocolProviderService>
        getProtocolProviders()
    {
        ServiceReference<?>[] serRefs = null;
        try
        {
            // get all registered provider factories
            serRefs
                = NotificationWiringActivator.bundleContext.getServiceReferences(
                        ProtocolProviderService.class.getName(),
                        null);
        }
        catch (InvalidSyntaxException e)
        {
            logger.error("NotificationManager : " + e);
        }

        List<ProtocolProviderService>
            providersList = new ArrayList<>();

        if (serRefs != null)
        {
            for (ServiceReference<?> serRef : serRefs)
            {
                ProtocolProviderService pp
                    = (ProtocolProviderService)
                        NotificationWiringActivator.bundleContext.getService(serRef);

                providersList.add(pp);
            }
        }
        return providersList;
    }

    /**
     * Determines whether the <tt>DIALING</tt> sound notification should be
     * played for a specific <tt>CallPeer</tt>.
     *
     * @param weakPeer the <tt>CallPeer</tt> for which it is to be determined
     * whether the <tt>DIALING</tt> sound notification is to be played
     * @return <tt>true</tt> if the <tt>DIALING</tt> sound notification should
     * be played for the specified <tt>callPeer</tt>; otherwise, <tt>false</tt>
     */
    private static boolean shouldPlayDialingSound(
            WeakReference<CallPeer> weakPeer)
    {
        CallPeer peer = weakPeer.get();

        if (peer == null)
            return false;

        Call call = peer.getCall();

        if (call == null)
            return false;

        CallConference conference = call.getConference();

        if (conference == null)
            return false;

        boolean play = false;

        for (Call aCall : conference.getCalls())
        {
            Iterator<? extends CallPeer> peerIter = aCall.getCallPeers();

            while (peerIter.hasNext())
            {
                CallPeer aPeer = peerIter.next();

                /*
                 * The peer is still in a call/telephony conference so the
                 * DIALING sound may need to be played.
                 */
                if (peer == aPeer)
                    play = true;

                CallPeerState state = peer.getState();

                if (CallPeerState.INITIATING_CALL.equals(state)
                        || CallPeerState.CONNECTING.equals(state))
                {
                    /*
                     * The DIALING sound should be played for the first CallPeer
                     * only.
                     */
                    if (peer != aPeer)
                        return false;
                }
                else
                {
                    /*
                     * The DIALING sound should not be played if there is a
                     * CallPeer which does not require the DIALING sound to be
                     * played.
                     */
                    return false;
                }
            }
        }

        return play;
    }

    /**
     * Stores notification references to stop them if a notification has expired
     * (e.g. to stop the dialing sound).
     */
    private final Map<Call, NotificationData> callNotifications
        = new WeakHashMap<>();

    /**
     * The pseudo timer which is used to delay multiple typing notifications
     * before receiving the message.
     */
    private final Map<Contact, Long> proactiveTimer
        = new HashMap<>();

    /**
     * Implements CallListener.callEnded. Stops sounds that are playing at
     * the moment if there're any.
     *
     * @param ev the <tt>CallEvent</tt>
     */
    public void callEnded(CallEvent ev)
    {
        try
        {
            NotificationData notification
                = callNotifications.get(ev.getSourceCall());

            if (notification != null)
                stopSound(notification);

            CallConference callConf = ev.getCallConference();
            CallConferenceStateEnum callState = callConf.getCallState();

            logger.debug(
                "CallConf: " + callConf.hashCode() + " is in state = " + callState);
            if (!CallConferenceStateEnum.UPLIFTING.equals(callState))
            {
                // Play the hangup sound.
                fireNotification(HANG_UP);
            }
        }
        catch(Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            else
            {
                logger.error(
                        "An error occurred while trying to notify"
                            + " about the end of a call.",
                        t);
            }
        }
    }

    /**
     * Implements the <tt>CallChangeListener.callPeerAdded</tt> method.
     * @param evt the <tt>CallPeerEvent</tt> that notifies us for the change
     */
    public void callPeerAdded(CallPeerEvent evt)
    {
        CallPeer peer = evt.getSourceCallPeer();

        if(peer == null)
            return;

        peer.addCallPeerListener(this);
        peer.addCallPeerSecurityListener(this);
        peer.addCallPeerConferenceListener(this);
    }

    /**
     * Implements the <tt>CallChangeListener.callPeerRemoved</tt> method.
     * @param evt the <tt>CallPeerEvent</tt> that has been triggered
     */
    public void callPeerRemoved(CallPeerEvent evt)
    {
        Call call = evt.getSourceCall();
        if (call != null && call.getCallPeerCount() == 0)
        {
            // Stop any call notifications.
            NotificationData notification
              = callNotifications.get(call);

            logger.debug("Stopping notification as call has no peers");
            if (notification != null)
            {
                stopSound(notification);
            }
        }

        CallPeer peer = evt.getSourceCallPeer();

        if(peer == null)
            return;

        peer.removeCallPeerListener(this);
        peer.removeCallPeerSecurityListener(this);
        peer.addCallPeerConferenceListener(this);
    }

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void callStateChanged(CallChangeEvent ev) {}

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void conferenceFocusChanged(CallPeerConferenceEvent ev) {}

    /**
     * Indicates that the given conference member has been added to the given
     * peer.
     *
     * @param conferenceEvent the event
     */
    public void conferenceMemberAdded(CallPeerConferenceEvent conferenceEvent)
    {
        try
        {
            CallPeer peer
                = conferenceEvent
                    .getConferenceMember()
                        .getConferenceFocusCallPeer();

            if(peer.getConferenceMemberCount() > 0)
            {
                CallPeerSecurityStatusEvent securityEvent
                    = peer.getCurrentSecuritySettings();

                if (securityEvent instanceof CallPeerSecurityOnEvent)
                    fireNotification(CALL_SECURITY_ON);
            }
        }
        catch(Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            else
                logger.error("Error notifying for secured call member", t);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void conferenceMemberRemoved(CallPeerConferenceEvent ev) {}

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void fileTransferCreated(FileTransferCreatedEvent ev) {}

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void fileTransferRequestCanceled(FileTransferRequestEvent ev) {}

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void fileTransferRequestReceived(FileTransferRequestEvent ev) {}

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void fileTransferRequestThumbnailUpdate(FileTransferRequestEvent ev)
    {}

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void fileTransferRequestRejected(FileTransferRequestEvent ev) {}

    /**
     * Adds all listeners related to the given protocol provider.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>
     */
    private void handleProviderAdded(ProtocolProviderService protocolProvider)
    {
        logger.info("Got provider " + protocolProvider.getProtocolDisplayName());

        if (!protocolProvider.getAccountID().isEnabled())
        {
            // Jabber and SIP will validly be disabled here when signing back
            // into chat/SIP (SIP will sign out and in when it's having
            // connection difficulties), so don't return here if so.  For
            // all other protocols (e.g. CSProtocol when Oulook contacts
            // are being used) we should return and do nothing here.
            String displayName = protocolProvider.getProtocolDisplayName();

            if (!ProtocolNames.JABBER.equals(displayName) &&
                !ProtocolNames.SIP.equals(displayName))
            {
                logger.info("Ignoring disabled provider " + displayName);
                return;
            }
        }

        Map<String, OperationSet> supportedOperationSets
            = protocolProvider.getSupportedOperationSets();

        // Obtain the typing notifications operation set.
        String tnOpSetClassName = OperationSetTypingNotifications
                                    .class.getName();

        if (supportedOperationSets.containsKey(tnOpSetClassName))
        {
            logger.info("Registering as typing listener");
            OperationSetTypingNotifications tn
                = (OperationSetTypingNotifications)
                    supportedOperationSets.get(tnOpSetClassName);

            //Add to all typing notification operation sets the Message
            //listener implemented in the ContactListPanel, which handles
            //all received messages.
            tn.addTypingNotificationsListener(this);
        }

        // Obtain the Message Waiting Indicator operation set
        String opSetMWIClassName = OperationSetMessageWaiting.class.getName();

        if (supportedOperationSets.containsKey(opSetMWIClassName))
        {
            logger.info("Registering as MWI listener");
            OperationSetMessageWaiting opSetMWI
                    = (OperationSetMessageWaiting)
                        supportedOperationSets.get(opSetMWIClassName);
            opSetMWI.addMessageWaitingNotificationListener(this);

            // Check if a message was received before we registered as a
            // listener.
            MessageWaitingEvent lastMessageWaitingEvent =
                    opSetMWI.getLastMessageWaitingNotification();

            if (lastMessageWaitingEvent != null)
            {
                messageWaitingNotify(lastMessageWaitingEvent);
            }
        }

        // Obtain file transfer operation set.
        OperationSetFileTransfer fileTransferOpSet
            = protocolProvider.getOperationSet(OperationSetFileTransfer.class);

        if (fileTransferOpSet != null)
        {
            logger.info("Registering as file transfer listener");
            fileTransferOpSet.addFileTransferListener(this);
        }

        OperationSetBasicTelephony<?> basicTelephonyOpSet
            = protocolProvider.getOperationSet(OperationSetBasicTelephony.class);

        if (basicTelephonyOpSet != null)
        {
            logger.info("Registering as telephony listener");
            basicTelephonyOpSet.addCallListener(this);
        }
    }

    /**
     * Removes all listeners related to the given protocol provider.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>
     */
    private void handleProviderRemoved(ProtocolProviderService protocolProvider)
    {
        logger.info("Removing provider " + protocolProvider.getProtocolDisplayName());
        Map<String, OperationSet> supportedOperationSets
            = protocolProvider.getSupportedOperationSets();

        // Obtain the typing notifications operation set.
        String tnOpSetClassName = OperationSetTypingNotifications
                                    .class.getName();

        if (supportedOperationSets.containsKey(tnOpSetClassName))
        {
            logger.info("Unregistering for typing");
            OperationSetTypingNotifications tn
                = (OperationSetTypingNotifications)
                    supportedOperationSets.get(tnOpSetClassName);

            //Add to all typing notification operation sets the Message
            //listener implemented in the ContactListPanel, which handles
            //all received messages.
            tn.removeTypingNotificationsListener(this);
        }

        // Obtain file transfer operation set.
        OperationSetFileTransfer fileTransferOpSet
            = protocolProvider.getOperationSet(OperationSetFileTransfer.class);

        if (fileTransferOpSet != null)
        {
            logger.info("Unregistering for file transfer");
            fileTransferOpSet.removeFileTransferListener(this);
        }

        OperationSetBasicTelephony<?> basicTelephonyOpSet
            = protocolProvider.getOperationSet(OperationSetBasicTelephony.class);

        if (basicTelephonyOpSet != null)
        {
            logger.info("Unregistering for telephony");
            basicTelephonyOpSet.removeCallListener(this);
        }
    }

    private boolean isCallOrConferenceInProgress()
    {
        UIService uiService = NotificationWiringActivator.getUIService();
        ConferenceService confService =
            NotificationWiringActivator.getConfService();
        return !uiService.getInProgressCalls().isEmpty()
            || (confService != null && confService.isConferenceJoined());
    }

    /**
     * Implements CallListener.incomingCallReceived. When a call is received
     * plays the ring phone sound to the user and gathers caller information
     * that may be used by a user-specified command (incomingCall event
     * trigger).
     *
     * @param ev the <tt>CallEvent</tt>
     */
    public void incomingCallReceived(CallEvent ev)
    {
        try
        {
            Call call = ev.getSourceCall();
            CallPeer peer = call.getCallPeers().next();
            Map<String,String> peerInfo = new HashMap<>();
            String peerName = peer.getDisplayName();

            peerInfo.put("caller.uri", peer.getURI());
            peerInfo.put("caller.address", peer.getAddress());
            peerInfo.put("caller.name", peerName);
            peerInfo.put("caller.id", peer.getPeerID());

            // Only play a notification sound if the call hasn't been
            // auto-answered.
            if (!ev.isAutoAnswered())
            {
                /*
                 * The sound we play for incoming calls depends on whether
                 * there is an existing active call (or meeting).  If there is,
                 * we use the call waiting notification type.
                 */
                String notificationType;

                if (!isCallOrConferenceInProgress())
                {
                    logger.debug("No existing call - use incoming_call sound");
                    notificationType = INCOMING_CALL;
                }
                else
                {
                    logger.debug("Already in call - use call_waiting sound");
                    notificationType = CALL_WAITING;
                }

                /*
                 * The loopCondition will stay with the notification sound
                 * until the latter is stopped. If by any chance the sound
                 * fails to stop by the time the call is no longer referenced,
                 * do try to stop it then. That's why the loopCondition will
                 * weakly reference the call.
                 */
                final WeakReference<Call> weakCall = new WeakReference<>(call);

                boolean isInSameBg = false;

                // Anonymous users and abstract peers have no meta contact
                if (peer.getMetaContact() != null)
                {
                    isInSameBg = peer.getMetaContact().getBGContact() != null;
                }
                boolean bgContactsEnabled = configService.user().getBoolean(BG_CONTACTS_ENABLED_PROP, false);

                boolean isDistinctRingingEnabled = configService.user()
                        .getBoolean(CONFIG_DISTINCT_RINGTONES_ENABLED_PROP, false);

                SoundNotificationAction.BgTag businessGroupTag;
                if (bgContactsEnabled && isDistinctRingingEnabled && isInSameBg)
                {
                    businessGroupTag = SoundNotificationAction.BgTag.BG_TAG_INTERNAL;
                }
                else
                {
                    businessGroupTag = SoundNotificationAction.BgTag.BG_TAG_GENERIC;
                }

                NotificationData notification
                    = fireNotification(
                            notificationType,
                            "",
                            NotificationWiringActivator.getResources()
                                    .getI18NString(
                                            "service.gui.INCOMING_CALL",
                                            new String[] { peerName }),
                            businessGroupTag,
                            peerInfo,
                            new Callable<Boolean>()
                            {
                                public Boolean call()
                                {
                                    Call call = weakCall.get();

                                    if (call == null)
                                        return false;

                                    /*
                                     * INCOMING_CALL should be played for a Call
                                     * only while there is a CallPeer in the
                                     * INCOMING_CALL state.
                                     */
                                    Iterator<? extends CallPeer> peerIter
                                        = call.getCallPeers();
                                    boolean loop = false;

                                    while (peerIter.hasNext())
                                    {
                                        CallPeer peer = peerIter.next();

                                        if (CallPeerState.INCOMING_CALL.equals(
                                                peer.getState()))
                                        {
                                            loop = true;
                                            break;
                                        }
                                    }
                                    return loop;
                                }
                            });

                if (notification != null)
                {
                    callNotifications.put(call, notification);
                }
            }
            else
            {
                logger.debug(
                    "Not playing notification as call has been auto-answered: " + call);
            }

            call.addCallChangeListener(this);

            peer.addCallPeerListener(this);
            peer.addCallPeerSecurityListener(this);
            peer.addCallPeerConferenceListener(this);
        }
        catch(Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            else
            {
                logger.error(
                        "An error occurred while trying to notify"
                            + " about an incoming call",
                        t);
            }
        }
    }

    public void messageWaitingNotify(MessageWaitingEvent evt)
    {
        NotificationService notificationService
                = NotificationWiringActivator.getNotificationService();

        if (notificationService == null)
        {
            return;
        }

        HashMap<String, Object> extras = new HashMap<>();
        extras.put(NotificationData.MESSAGE_WAITING_COUNT_EXTRA,
                   (Integer) evt.getUnreadMessages());

        notificationService.fireNotification(
                MESSAGE_WAITING,
                null,
                null,
                null,
                extras);
    }

    /**
     * Initialize, register default notifications and start listening for
     * new protocols or removed one and find any that are already registered.
     */
    void init()
    {
        registerDefaultNotifications();

        // listens for new protocols
        NotificationWiringActivator.bundleContext.addServiceListener(this);

        // enumerate currently registered protocols
        for(ProtocolProviderService pp : getProtocolProviders())
        {
            handleProviderAdded(pp);
        }
    }

    /**
     * Checks if the contained call is a conference call.
     *
     * @param call the call to check
     * @return <code>true</code> if the contained <tt>Call</tt> is a conference
     * call, otherwise - returns <code>false</code>.
     */
    public boolean isConference(Call call)
    {
        // If we're the focus of the conference.
        if (call.isConferenceFocus())
            return true;

        // If one of our peers is a conference focus, we're in a
        // conference call.
        Iterator<? extends CallPeer> callPeers = call.getCallPeers();

        while (callPeers.hasNext())
        {
            CallPeer callPeer = callPeers.next();

            if (callPeer.isConferenceFocus())
                return true;
        }

        // the call can have two peers at the same time and there is no one
        // is conference focus. This is situation when some one has made an
        // attended transfer and has transfered us. We have one call with two
        // peers the one we are talking to and the one we have been transfered
        // to. And the first one is been hanguped and so the call passes through
        // conference call fo a moment and than go again to one to one call.
        return call.getCallPeerCount() > 1;
    }

    /**
     * Do nothing. Implements CallListener.outGoingCallCreated.
     * @param event the <tt>CallEvent</tt>
     */
    public void outgoingCallCreated(CallEvent event)
    {
        Call call = event.getSourceCall();
        call.addCallChangeListener(this);

        if(call.getCallPeers().hasNext())
        {
            CallPeer peer = call.getCallPeers().next();
            peer.addCallPeerListener(this);
            peer.addCallPeerSecurityListener(this);
            peer.addCallPeerConferenceListener(this);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void peerAddressChanged(CallPeerChangeEvent ev) {}

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void peerDisplayNameChanged(CallPeerChangeEvent ev) {}

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void peerImageChanged(CallPeerChangeEvent ev) {}

    /**
     * Fired when peer's state is changed
     *
     * @param ev fired CallPeerEvent
     */
    public void peerStateChanged(CallPeerChangeEvent ev)
    {
        try
        {
            CallPeer peer = ev.getSourceCallPeer();
            Call call = peer.getCall();
            CallPeerState newState = (CallPeerState) ev.getNewValue();
            CallPeerState oldState = (CallPeerState) ev.getOldValue();

            // Play the dialing audio when in connecting and initiating call state.
            // Stop the dialing audio when we enter any other state.
            if ((newState == CallPeerState.INITIATING_CALL)
                    || (newState == CallPeerState.CONNECTING))
            {
                /*
                 * The loopCondition will stay with the notification sound until
                 * the latter is stopped. If by any chance the sound fails to
                 * stop by the time the peer is no longer referenced, do try to
                 * stop it then. That's why the loopCondition will weakly
                 * reference the peer.
                 */
                final WeakReference<CallPeer> weakPeer
                    = new WeakReference<>(peer);

                /* We want to play the dialing once for multiple CallPeers. */
                if (shouldPlayDialingSound(weakPeer))
                {
                    NotificationData notification
                        = fireNotification(
                                DIALING,
                                new Callable<Boolean>()
                                {
                                    public Boolean call()
                                    {
                                        return shouldPlayDialingSound(weakPeer);
                                    }
                                });

                    if (notification != null)
                        callNotifications.put(call, notification);
                }
            }
            else
            {
                NotificationData notification = callNotifications.get(call);

                if (notification != null)
                    stopSound(notification);

                // If there is a call/conference in progress, then need to
                // convert any incoming call ringing notifications to call
                // waiting ones.
                if (isCallOrConferenceInProgress())
                {
                    logger.debug("There is a call in progress. Converting all"
                        + " the incoming call ring tones to call waiting "
                        + "tones.");

                    // List to store the notifications to stop. This will
                    // contain the notification data for the incoming call ring
                    List<NotificationData> incomingCallNotificationsToStop =
                            new ArrayList<>();

                    // Map to store the new notifications that we want to add.
                    // This will include the notification data for all the newly
                    // converted call waiting notifications.
                    Map<Call, NotificationData> callWaitingNotificationsToAdd =
                            new HashMap<>();

                    // Iterate through all the current notifications and add to
                    // notificationsToStop any where the event type is incoming
                    // call and create a new call waiting one and add to the
                    // notificationsToAdd map
                    for (Map.Entry<Call, NotificationData> entry:
                        callNotifications.entrySet())
                    {
                        Call searchCall = entry.getKey();
                        NotificationData currentData = entry.getValue();

                        // Only need to convert to call waiting tone if the
                        // type is incoming call and the current call is not the
                        // call that just had a call peer change.
                        if (!call.equals(searchCall) &&
                            currentData.getEventType().equals(INCOMING_CALL))
                        {
                            logger.debug("Converting Call " +
                                entry.getKey().getCallID() + " to use call "
                                + "waiting tone.");

                            // Fire notification with the same data but with
                            // the call waiting event type
                            NotificationData newNotification
                                = fireNotification(
                                    CALL_WAITING,
                                    currentData.getTitle(),
                                    currentData.getMessage(),
                                    currentData.getExtras()
                                  );

                            incomingCallNotificationsToStop.add(currentData);

                            if (notification != null)
                            {
                                callWaitingNotificationsToAdd.put(searchCall,
                                    newNotification);
                            }
                        }
                    }

                    // Stop all the incoming Call Notifications
                    for (NotificationData notificationData:
                        incomingCallNotificationsToStop)
                    {
                        stopSound(notificationData);
                    }

                    // Update the callNotifications map
                    callNotifications.putAll(callWaitingNotificationsToAdd);
                }
            }

            if (newState == CallPeerState.ALERTING_REMOTE_SIDE
                //if we were already in state CONNECTING_WITH_EARLY_MEDIA the server
                //is already taking care of playing the notifications so we don't
                //need to fire a notification here.
                && oldState != CallPeerState.CONNECTING_WITH_EARLY_MEDIA)
            {
                final WeakReference<CallPeer> weakPeer
                    = new WeakReference<>(peer);
                NotificationData notification
                    = fireNotification(
                            OUTGOING_CALL,
                            new Callable<Boolean>()
                            {
                                public Boolean call()
                                {
                                    CallPeer peer = weakPeer.get();

                                    return
                                        (peer != null)
                                            && CallPeerState
                                                .ALERTING_REMOTE_SIDE
                                                    .equals(peer.getState());
                                }
                            });

                if (notification != null)
                    callNotifications.put(call, notification);
            }
            else if (newState == CallPeerState.BUSY)
            {
                // We start the busy sound only if we're in a simple call.
                if (!isConference(call))
                {
                    final WeakReference<CallPeer> weakPeer
                        = new WeakReference<>(peer);
                    NotificationData notification
                        = fireNotification(
                                BUSY_CALL,
                                new Callable<Boolean>()
                                {
                                    public Boolean call()
                                    {
                                        CallPeer peer = weakPeer.get();

                                        return
                                            (peer != null)
                                                && CallPeerState.BUSY.equals(
                                                        peer.getState());
                                    }
                                });

                    if (notification != null)
                        callNotifications.put(call, notification);
                }
            }
        }
        catch(Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            else
            {
                logger.error(
                        "An error occurred while trying to notify"
                            + " about a change in the state of a call peer.",
                        t);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void peerTransportAddressChanged(CallPeerChangeEvent ev) {}

    /**
     * Register all default notifications.
     */
    private void registerDefaultNotifications()
    {
        final NotificationService notificationService
            = NotificationWiringActivator.getNotificationService();

        if(notificationService == null)
            return;

        if (configService == null)
        {
            logger.fatal("Can't access config service in NotificationWiringActivator");
            return;
        }

        // Register incoming message notifications.
        notificationService.registerDefaultNotificationForEvent(
                INCOMING_MESSAGE,
                NotificationAction.ACTION_POPUP_MESSAGE,
                null,
                null);

        notificationService.registerDefaultNotificationForEvent(
                INCOMING_MESSAGE,
                new SoundNotificationAction(
                    SoundProperties.INCOMING_MESSAGE, -1, true, false, false));

        notificationService.registerDefaultNotificationForEvent(
                INCOMING_MESSAGE,
                new UINotificationAction()
        );

        notificationService.registerDefaultNotificationForEvent(
                MISSED_CALL,
                new UINotificationAction()
        );

        notificationService.registerDefaultNotificationForEvent(
                MESSAGE_WAITING,
                new UINotificationAction()
        );

        notificationService.registerDefaultNotificationForEvent(
            INCOMING_CONFERENCE,
            new SoundNotificationAction(
                SoundProperties.INCOMING_CONFERENCE, 2000, true, true, true));

        // Register incoming call notifications.
        notificationService.registerDefaultNotificationForEvent(
                INCOMING_CALL,
                NotificationAction.ACTION_POPUP_MESSAGE,
                null,
                null);

        SoundNotificationAction inCallSoundHandler
            = new SoundNotificationAction(
                    SoundProperties.INCOMING_CALL, 2000, true, true, true);

        notificationService.registerDefaultNotificationForEvent(
                INCOMING_CALL,
                inCallSoundHandler);

        // Register outgoing call notifications.
        final int loopInterval =
            configService.user().getInt(
                "net.java.sip.communicator.plugin.notificationwiring.RINGBACK_LOOP_INTERVAL",
                3000);

        // We set a sensible default now, but also wait to be informed when we
        // have the international dial prefix from EAS - because that's a better
        // thing to base this off than the machine locale.  We may not yet have
        // got the info from EAS though.
        configService.user().addPropertyChangeListener(COUNTRY_CODE_KEY,
            new PropertyChangeListener()
            {
                @Override
                public void propertyChange(PropertyChangeEvent evt)
                {
                    logger.debug("Property changed: " + evt);
                    SoundProperties.updateOutgoingCallSound((Integer) evt.getNewValue());
                    notificationService.registerDefaultNotificationForEvent(
                        OUTGOING_CALL,
                        new SoundNotificationAction(
                                SoundProperties.getOutgoingCall(),
                                loopInterval,
                                false, true, false));
                }
            });

        // Check in case the property has already been set (in which case we
        // won't get a property change callback).  It's safe to set the sound
        // twice (in the race where we find the event here and also get a
        // callback).
        int countryCode = configService.user().getInt(COUNTRY_CODE_KEY, -1);
        if (countryCode != -1)
        {
            SoundProperties.updateOutgoingCallSound(countryCode);
        }

        logger.debug("Use ringing file: " + SoundProperties.getOutgoingCall());
        notificationService.registerDefaultNotificationForEvent(
                OUTGOING_CALL,
                new SoundNotificationAction(
                        SoundProperties.getOutgoingCall(),
                        loopInterval,
                        false, true, false));

        // Register incoming call notifications.
        notificationService.registerDefaultNotificationForEvent(
                CALL_WAITING,
                NotificationAction.ACTION_POPUP_MESSAGE,
                null,
                null);

        SoundNotificationAction callWaitSoundHandler
            = new SoundNotificationAction(
                    SoundProperties.CALL_WAITING, 2000, true, true, true);

        notificationService.registerDefaultNotificationForEvent(
                CALL_WAITING,
                callWaitSoundHandler);

        // Register busy call notifications.
        notificationService.registerDefaultNotificationForEvent(
                BUSY_CALL,
                new SoundNotificationAction(
                        SoundProperties.BUSY,
                        1,
                        false, true, false));

        // Register dial notifications.
        notificationService.registerDefaultNotificationForEvent(
                DIALING,
                new SoundNotificationAction(
                        SoundProperties.DIALING,
                        -1,
                        false, true, false));

        // Register the hangup sound notification.
        notificationService.registerDefaultNotificationForEvent(
                HANG_UP,
                new SoundNotificationAction(
                        SoundProperties.HANG_UP,
                        -1,
                        false, true, false));

        // Register proactive notifications.
        notificationService.registerDefaultNotificationForEvent(
                PROACTIVE_NOTIFICATION,
                NotificationAction.ACTION_POPUP_MESSAGE,
                null,
                null);

        // Register warning message notifications.
        notificationService.registerDefaultNotificationForEvent(
                SECURITY_MESSAGE,
                NotificationAction.ACTION_POPUP_MESSAGE,
                null,
                null);

        // Register sound notification for security state on during a call.
        notificationService.registerDefaultNotificationForEvent(
                CALL_SECURITY_ON,
                new SoundNotificationAction(
                        SoundProperties.CALL_SECURITY_ON, -1,
                        false, true, false));

        // Register sound notification for security state off during a call.
        notificationService.registerDefaultNotificationForEvent(
                CALL_SECURITY_ERROR,
                new SoundNotificationAction(
                        SoundProperties.CALL_SECURITY_ERROR, -1,
                        false, true, false));

        // Register sound notification for incoming files.
        notificationService.registerDefaultNotificationForEvent(
                INCOMING_FILE,
                NotificationAction.ACTION_POPUP_MESSAGE,
                null,
                null);

        notificationService.registerDefaultNotificationForEvent(
                INCOMING_FILE,
                new SoundNotificationAction(
                        SoundProperties.INCOMING_FILE, -1,
                        true, false, false));

        // Register notification for saved calls.
        notificationService.registerDefaultNotificationForEvent(
            CALL_SAVED,
            NotificationAction.ACTION_POPUP_MESSAGE,
            null,
            null);
    }

    /**
     * Processes the received security message.
     * @param ev the event we received
     */
    public void securityMessageRecieved(CallPeerSecurityMessageEvent ev)
    {
        try
        {
            String messageTitleKey;

            switch (ev.getEventSeverity())
            {
            // Don't play alert sound for Info or warning.
            case CallPeerSecurityMessageEvent.INFORMATION:
                messageTitleKey = "service.gui.SECURITY_INFO";
                break;

            case CallPeerSecurityMessageEvent.WARNING:
                messageTitleKey = "service.gui.SECURITY_WARNING";
                break;

            // Security cannot be established! Play an alert sound.
            case CallPeerSecurityMessageEvent.SEVERE:
            case CallPeerSecurityMessageEvent.ERROR:
                messageTitleKey = "service.gui.SECURITY_ERROR";
                fireNotification(CALL_SECURITY_ERROR);
                break;

            default:
                /*
                 * Whatever other severity there is or will be, we do not how to
                 * react to it yet.
                 */
                messageTitleKey = null;
            }

            if (messageTitleKey != null)
            {
                fireNotification(
                        SECURITY_MESSAGE,
                        NotificationWiringActivator.getResources()
                                .getI18NString(messageTitleKey),
                        ev.getI18nMessage());
            }
        }
        catch(Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            else
            {
                logger.error(
                        "An error occurred while trying to notify"
                            + " about a security message",
                        t);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void securityNegotiationStarted(
            CallPeerSecurityNegotiationStartedEvent ev) {}

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void securityOff(CallPeerSecurityOffEvent ev) {}

    /**
     * When a <tt>securityOnEvent</tt> is received.
     * @param ev the event we received
     */
    public void securityOn(CallPeerSecurityOnEvent ev)
    {
        try
        {
            SrtpControl securityController = ev.getSecurityController();
            CallPeer peer = (CallPeer) ev.getSource();

            if(!securityController.requiresSecureSignalingTransport()
                    || peer.getProtocolProvider().isSignalingTransportSecure())
            {
                fireNotification(CALL_SECURITY_ON);
            }
        }
        catch(Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            else
            {
                logger.error(
                        "An error occurred while trying to notify"
                            + " about a security-related event",
                        t);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * Not used.
     */
    public void securityTimeout(CallPeerSecurityTimeoutEvent ev) {}

    /**
     * Implements the <tt>ServiceListener</tt> method. Verifies whether the
     * passed event concerns a <tt>ProtocolProviderService</tt> and adds the
     * corresponding listeners.
     *
     * @param event The <tt>ServiceEvent</tt> object.
     */
    public void serviceChanged(ServiceEvent event)
    {
        ServiceReference<?> serviceRef = event.getServiceReference();

        // if the event is caused by a bundle being stopped, we don't want to
        // know
        if (serviceRef.getBundle().getState() == Bundle.STOPPING)
            return;

        Object service
            = NotificationWiringActivator.bundleContext.getService(serviceRef);
        logger.debug("Notified of service change for " + service.toString());

        // we don't care if the source service is not a protocol provider
        if (service instanceof ProtocolProviderService)
        {
            switch (event.getType())
            {
                case ServiceEvent.REGISTERED:
                    handleProviderAdded((ProtocolProviderService) service);
                    break;
                case ServiceEvent.UNREGISTERING:
                    handleProviderRemoved((ProtocolProviderService) service);
                    break;
            }
        }
    }

    /**
     * Stops all sounds for the given event type.
     *
     * @param data the event type for which we should stop sounds. One of
     * the static event types defined in this class.
     */
    private void stopSound(NotificationData data)
    {
        if (data == null)
            return;

        try
        {
            NotificationService notificationService
                = NotificationWiringActivator.getNotificationService();

            if(notificationService != null)
                notificationService.stopNotification(data);
        }
        finally
        {
            /*
             * The field callNotifications associates a Call with a
             * NotificationData for the purposes of the stopSound method so the
             * stopSound method should dissociate them upon stopping a specific
             * NotificationData.
             */
            Iterator<Map.Entry<Call, NotificationData>> i
                = callNotifications.entrySet().iterator();

            while (i.hasNext())
            {
                Map.Entry<Call, NotificationData> e = i.next();

                if (data.equals(e.getValue()))
                    i.remove();
            }
        }
    }

     /**
     * Informs the user what is the typing state of his chat contacts.
     *
     * @param ev the event containing details on the typing notification
     */
    public void typingNotificationReceived(TypingNotificationEvent ev)
    {
        logger.debug("entry on method typingNotificationReceived");
        try
        {
            Contact contact = ev.getSourceContact();

            // we don't care for proactive notifications, different than typing
            // sometimes after closing chat we can see someone is typing us
            // its just server sanding that the chat is inactive (STATE_STOPPED)
            if (ev.getTypingState() != TypingState.TYPING)
            {
                logger.debug("Typing state different from TYPING, returning...");
                return;
            }

            // check whether the current chat window shows the
            // chat we received a typing info for and in such case don't show
            // notifications
            UIService uiService = NotificationWiringActivator.getUIService();

            if(uiService != null)
            {
                Chat chat = uiService.getCurrentChat();

                if(chat != null)
                {
                    MetaContact metaContact = uiService.getChatContact(chat);

                    if((metaContact != null)
                            && metaContact.containsContact(contact)
                            && chat.isChatFocused())
                    {
                        return;
                    }
                }
            }

            long currentTime = System.currentTimeMillis();

            if (proactiveTimer.size() > 0)
            {
                logger.debug("proactiveTimer Size: " + proactiveTimer.size());
                // first remove contacts that have been here longer than the
                // timeout to avoid memory leaks
                Iterator<Map.Entry<Contact, Long>> entries
                    = proactiveTimer.entrySet().iterator();

                while (entries.hasNext())
                {
                    Map.Entry<Contact, Long> entry = entries.next();
                    Long lastNotificationDate = entry.getValue();

                    if (lastNotificationDate + 30000 <  currentTime)
                    {
                        // The entry is outdated
                        entries.remove();
                    }
                }

                // Now, check if the contact is still in the map
                if (proactiveTimer.containsKey(contact))
                {
                    logger.debug("Already notified");
                    // We already notified the others about this
                    return;
                }
            }

            proactiveTimer.put(contact, currentTime);
            logger.debug("firing proactive chat notification");
            fireChatNotification(
                    contact,
                    null,
                    PROACTIVE_NOTIFICATION,
                    contact.getDisplayName(),
                    NotificationWiringActivator.getResources().getI18NString(
                            "service.gui.PROACTIVE_NOTIFICATION"),
                    false);
        }
        catch(Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            else
            {
                logger.error(
                        "An error occurred while handling"
                            + " a typing notification.",
                        t);
            }
        }
    }
}
