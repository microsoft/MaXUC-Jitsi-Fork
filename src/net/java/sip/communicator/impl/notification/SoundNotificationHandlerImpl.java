/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.notification;

import java.beans.*;
import java.util.*;
import java.util.concurrent.*;

import org.jitsi.service.audionotifier.*;
import org.jitsi.service.configuration.*;
import org.jitsi.util.*;

import net.java.sip.communicator.plugin.notificationwiring.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.service.protocol.globalstatus.GlobalStatusService.*;
import net.java.sip.communicator.util.Logger;

/**
 * An implementation of the <tt>SoundNotificationHandler</tt> interface.
 *
 * @author Yana Stamcheva
 */
public class SoundNotificationHandlerImpl
    implements SoundNotificationHandler
{
    /**
     * The logger that will be used to log messages.
     */
    private static Logger logger
        = Logger.getLogger(SoundNotificationHandlerImpl.class);

    /**
     * Property to disable sound notification during an on-going call.
     */
    private static final String PROP_DISABLE_NOTIFICATION_DURING_CALL =
    "net.java.sip.communicator.impl.notification.disableNotificationDuringCall";

    /**
     * Property to mute notifications when busy of in a meeting.
     */
    private static final String PROP_MUTE_WHEN_BUSY =
                     "net.java.sip.communicator.impl.notification.muteWhenBusy";

    /**
     * The indicator which determines whether this
     * <tt>SoundNotificationHandler</tt> is currently muted i.e. the sounds are
     * off.
     */
    private boolean mute;

    /**
     * Indicates whether the client is currently busy (where busy means either
     * Busy or In a Meeting)
     */
    private boolean busy;

    private final Map<SCAudioClip, NotificationData> playedClips
        = new WeakHashMap<>();

    /**
     * Configuration Service
     */
    private static ConfigurationService configService
                              = NotificationActivator.getConfigurationService();

    /**
     * Global Status Service
     */
    private static GlobalStatusService statusService
                               = NotificationActivator.getGlobalStatusService();

    /**
     * The set of objects that want to be informed of changes to the Global mute
     * status
     */
    private final Set<MuteListener> muteListeners = new HashSet<>();

    public SoundNotificationHandlerImpl()
    {
        // Publish mute when 'Mute when busy' changes
        configService.user().addPropertyChangeListener(PROP_MUTE_WHEN_BUSY,
                                                new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                logger.debug("Mute when Busy property changed - updating mute accordingly");
                publishMuteState();
            }
        });

        // Publish mute when the global status changes
        statusService.addStatusChangeListener(new GlobalStatusChangeListener()
        {
            @Override
            public void onStatusChanged()
            {
                logger.debug("Status changed - updating mute accordingly");

                busy = isBusyStatus(statusService.getGlobalStatus());
                publishMuteState();
            }
        });
    }

    /**
     * Decides if the provided status should be deemed as busy in the context of
     * mute. This means either busy or in a meeting.
     * @param status
     * @return true if deemed to be busy
     */
    private boolean isBusyStatus(GlobalStatusEnum status)
    {
        return  status == GlobalStatusEnum.BUSY ||
                status == GlobalStatusEnum.IN_A_MEETING;
    }

    /**
     * {@inheritDoc}
     */
    public String getActionType()
    {
        return NotificationAction.ACTION_SOUND;
    }

    /**
     * Specifies if currently the sound is off.
     *
     * @return TRUE if currently the sound is off, FALSE otherwise
     */
    public boolean isMute()
    {
        return mute ||
               (busy &&
                configService.user().getBoolean(PROP_MUTE_WHEN_BUSY, false));
    }

    /**
     * Plays the sound given by the containing <tt>soundFileDescriptor</tt>. The
     * sound is played in loop if the loopInterval is defined.
     * @param action The action to act upon.
     * @param data Additional data for the event.
     * @param device
     */
    private void play(
            SoundNotificationAction action,
            NotificationData data,
            SCAudioClipDevice device)
    {
        AudioNotifierService audioNotifService
            = NotificationActivator.getAudioNotifier();

        if((audioNotifService == null)
                || StringUtils.isNullOrEmpty(action.getDescriptor(), true))
        {
            logger.debug("Unable to play sound as no service or descriptor");
            return;
        }

        logger.debug(
            new StringBuilder().
                append("Playing ").
                append(action.getDescriptor()).
                append(" on ").
                append(device).
                append(" device").
                toString());

        // this is hack, seen on some os (particularly seen on macosx with
        // external devices).
        // when playing notification in the call, can break the call and
        // no further communicating can be done after the notification.
        // So we skip playing notification if we have a call running
        ConfigurationService cfg
            = NotificationActivator.getConfigurationService();
        if(cfg != null
                && cfg.user().getBoolean(
                    PROP_DISABLE_NOTIFICATION_DURING_CALL,
                    false)
                && SCAudioClipDevice.NOTIFICATION.equals(device))
        {
            UIService uiService = NotificationActivator.getUIService();

            if(!uiService.getInProgressCalls().isEmpty())
            {
                logger.debug("Skipping playing as in a call already");
                return;
            }
        }

        SCAudioClip audio = null;

        switch (device)
        {
        case NOTIFICATION:
        case PLAYBACK:
            audio
                = audioNotifService.createAudio(
                        action.getDescriptor(),
                        SCAudioClipDevice.PLAYBACK.equals(device));
            break;

        case PC_SPEAKER:
            audio = new PCSpeakerClip();
            break;
        }

        // it is possible that audio cannot be created
        if(audio == null)
            return;

        synchronized(playedClips)
        {
            playedClips.put(audio, data);
        }

        boolean played = false;

        try
        {
            @SuppressWarnings("unchecked")
            Callable<Boolean> loopCondition
                = (Callable<Boolean>)
                    data.getExtra(
                            NotificationData
                                .SOUND_NOTIFICATION_HANDLER_LOOP_CONDITION_EXTRA);

            audio.play(action.getLoopInterval(), loopCondition);
            played = true;
        }
        finally
        {
            synchronized(playedClips)
            {
                if (!played)
                    playedClips.remove(audio);
            }
        }
    }

    /**
     * Stops/Restores all currently playing sounds.
     *
     * @param mute mute or not currently playing sounds
     */
    public void setMute(boolean mute)
    {
        this.mute = mute;

        // To allow unmuting whilst in a busy state, if we are setting mute to
        // false, also set busy to false.
        busy = busy && mute;

        publishMuteState();
    }

    /**
     * Plays the sound given by the containing <tt>soundFileDescriptor</tt>. The
     * sound is played in loop if the loopInterval is defined.
     * @param action The action to act upon.
     * @param data Additional data for the event.
     */
    public void start(SoundNotificationAction action, NotificationData data)
    {
        String actionData = action.getDescriptor() +
            " Event Type: " + data.getEventType() +
            " Notification Enabled: " + action.isSoundNotificationEnabled() +
            " Sound playback enabled: " + action.isSoundPlaybackEnabled() +
            " PC Sound playback enabled: " + action.isSoundPCSpeakerEnabled();
        logger.debug("Asked to play a sound: " + actionData);

        boolean playOnlyOnPlayback = true;

        AudioNotifierService audioNotifService
            = NotificationActivator.getAudioNotifier();

        if(audioNotifService != null)
        {
            playOnlyOnPlayback
                = audioNotifService.audioOutAndNotificationsShareSameDevice();
        }

        logger.debug("playOnlyOnPlayback set to " + playOnlyOnPlayback);

        // We should mute Notification sounds if we are muted or DnD
        boolean muted = false;
        if (statusService.getGlobalStatus() == GlobalStatusEnum.DO_NOT_DISTURB)
        {
            logger.debug("Currently DnD, may not play audio notification");
            muted = true;
        }
        else if (isMute())
        {
            logger.debug("Currently muted, may not play audio notification");
            muted = true;
        }

        // If we are muted, we shouldn't play the incoming call/conference
        // ringing over the Playback device
        boolean isMutedRing =
            (NotificationManager.INCOMING_CALL.equals(data.getEventType()) ||
             NotificationManager.CALL_WAITING.equals(data.getEventType()) ||
             NotificationManager.INCOMING_CONFERENCE.equals(data.getEventType())) &&
                                                                           muted;

        // If we aren't muted and are supposed to play a notification but our
        // Notification Device is the same as our Playback device, then use the
        // Playback device.
        boolean playNotificationAsPlayback =
            action.isSoundNotificationEnabled() && !muted && playOnlyOnPlayback;

        if ((action.isSoundPlaybackEnabled() && !isMutedRing) ||
            playNotificationAsPlayback)
        {
            play(action, data, SCAudioClipDevice.PLAYBACK);
        }

        // Only play a notification if we aren't muted and we shouldn't play it
        // through the playback device
        if (action.isSoundNotificationEnabled() &&
            !playOnlyOnPlayback &&
            !muted)
        {
            play(action, data, SCAudioClipDevice.NOTIFICATION);
        }

        if(action.isSoundPCSpeakerEnabled())
            play(action, data, SCAudioClipDevice.PC_SPEAKER);
    }

    /**
     * Stops the sound.
     * @param data Additional data for the event.
     */
    public void stop(NotificationData data)
    {
        AudioNotifierService audioNotifService
            = NotificationActivator.getAudioNotifier();

        if (audioNotifService != null)
        {
            List<SCAudioClip> clipsToStop = new ArrayList<>();

            synchronized(playedClips)
            {
                Iterator<Map.Entry<SCAudioClip, NotificationData>> i
                    = playedClips.entrySet().iterator();

                while (i.hasNext())
                {
                    Map.Entry<SCAudioClip, NotificationData> e = i.next();

                    if (e.getValue() == data)
                    {
                        clipsToStop.add(e.getKey());
                        i.remove();
                    }
                }
            }

            for(SCAudioClip clip : clipsToStop)
            {
                try
                {
                    clip.stop();
                }
                catch(Throwable t)
                {
                    logger.error("Error stopping audio clip", t);
                }
            }
        }
    }

    /**
     * Tells if the given notification sound is currently played.
     *
     * @param data Additional data for the event.
     */
    public boolean isPlaying(NotificationData data)
    {
        AudioNotifierService audioNotifService
            = NotificationActivator.getAudioNotifier();

        if (audioNotifService != null)
        {
            synchronized(playedClips)
            {
                Iterator<Map.Entry<SCAudioClip, NotificationData>> i
                    = playedClips.entrySet().iterator();

                while (i.hasNext())
                {
                    Map.Entry<SCAudioClip, NotificationData> e = i.next();

                    if (e.getValue() == data)
                    {
                        return e.getKey().isStarted();
                    }
                }
            }
        }
        return false;
    }

    /**
     * Beeps the PC speaker.
     */
    private static class PCSpeakerClip
        extends AbstractSCAudioClip
    {
        /**
         * Initializes a new <tt>PCSpeakerClip</tt> instance.
         */
        public PCSpeakerClip()
        {
            super(null, NotificationActivator.getAudioNotifier());
        }

        /**
         * Beeps the PC speaker.
         *
         * @return <tt>true</tt> if the playback was successful; otherwise,
         * <tt>false</tt>
         */
        protected boolean runOnceInPlayThread()
        {
            try
            {
                java.awt.Toolkit.getDefaultToolkit().beep();
                return true;
            }
            catch (Throwable t)
            {
                if (t instanceof ThreadDeath)
                    throw (ThreadDeath) t;
                else
                    return false;
            }
        }
    }

    /**
     * Enumerates the types of devices on which <tt>SCAudioClip</tt>s may be
     * played back.
     */
    private enum SCAudioClipDevice
    {
        NOTIFICATION,
        PC_SPEAKER,
        PLAYBACK
    }

    @Override
    public void addMuteListener(MuteListener listener)
    {
        synchronized (muteListeners)
        {
            muteListeners.add(listener);
        }
    }

    @Override
    public void removeMuteListener(MuteListener listener)
    {
        synchronized (muteListeners)
        {
            muteListeners.remove(listener);
        }
    }

    /**
     * Publish the current mute state to all registered listeners
     */
    private synchronized void publishMuteState()
    {
        boolean muted = isMute();

        Set<MuteListener> listeners;
        synchronized (muteListeners)
        {
            listeners = new HashSet<>(muteListeners);
        }

        for (MuteListener listener : listeners)
        {
            listener.onMuteChanged(muted);
        }
    }
}
