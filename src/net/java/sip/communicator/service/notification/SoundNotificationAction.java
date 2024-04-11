/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.notification;

import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of the <tt>SoundNotificationHandlerImpl</tt> interface.
 *
 * @author Yana Stamcheva
 */
public class SoundNotificationAction
    extends NotificationAction
{
    /**
     * Interval of milliseconds to wait before repeating the sound. -1 means no
     * repetition.
     */
    private int loopInterval;

    /**
     * the descriptor pointing to the sound to be played.
     */
    private String soundFileDescriptor;

    /**
     * Descriptor map that may hold multiple ringtones. Only used for incoming conference and calls.
     * The Format is "tagKey = descriptorValue". The descriptor is a relative path to the sound file.
     */
    private final Map<BgTag, String> soundFileDescriptorsMap = new HashMap<>();

    /**
     * The boolean telling if this sound is to be played on notification device.
     */
    private boolean isSoundNotificationEnabled;

    /**
     * Is sound to be played on playback device.
     */
    private boolean isSoundPlaybackEnabled;

    /**
     * Is sound to be played on pc speaker device.
     */
    private boolean isSoundPCSpeakerEnabled;

    /**
     * Tags depicting whether an incoming call is internal or external. Used in
     * selecting the ringtone of the incoming call.
     */
    public enum BgTag
    {
        BG_TAG_INTERNAL("Internal"),
        BG_TAG_GENERIC("External"),
        ;

        private final String tag;

        BgTag(String tag)
        {
            this.tag = tag;
        }

        @Override
        public String toString()
        {
            return tag;
        }
    }

    /**
     * Creates an instance of <tt>SoundNotification</tt> by
     * specifying the sound file descriptor and the loop interval.
     * By default is enabling simulation output to notification and
     * playback device.
     *
     * @param soundDescriptor the sound file descriptor
     * @param loopInterval the loop interval
     */
    public SoundNotificationAction( String soundDescriptor,
                                    int loopInterval)
    {
        this(soundDescriptor, loopInterval,
            false,
            false,
            false);
    }

    /**
     * Creates an instance of <tt>SoundNotification</tt> by specifying the sound file descriptor and the loop interval.
     *
     * @param taggedSoundDescriptors map of tagged sound file descriptors. Preferably tagged with BG_TAG_INTERNAL or
     *                               BG_TAG_GENERIC
     * @param loopInterval the loop interval
     * @param isSoundNotificationEnabled True if this sound is activated. False Otherwise.
     * @param isSoundPlaybackEnabled True if this sound is activated. False Otherwise.
     * @param isSoundPCSpeakerEnabled True if this sound is activated. False Otherwise.
     */
    public SoundNotificationAction(Map<BgTag, String> taggedSoundDescriptors,
                                   int loopInterval,
                                   boolean isSoundNotificationEnabled,
                                   boolean isSoundPlaybackEnabled,
                                   boolean isSoundPCSpeakerEnabled)
    {
        this("", loopInterval, isSoundNotificationEnabled, isSoundPlaybackEnabled, isSoundPCSpeakerEnabled);
        soundFileDescriptorsMap.putAll(taggedSoundDescriptors);

        // Take the first entry as default
        soundFileDescriptor = (String) soundFileDescriptorsMap.values().toArray()[0];
    }

    /**
     * Creates an instance of <tt>SoundNotification</tt> by
     * specifying the sound file descriptor and the loop interval.
     *
     * @param soundDescriptor the sound file descriptor
     * @param loopInterval the loop interval
     * @param isSoundNotificationEnabled True if this sound is activated. False Otherwise.
     * @param isSoundPlaybackEnabled True if this sound is activated. False Otherwise.
     * @param isSoundPCSpeakerEnabled True if this sound is activated. False Otherwise.
     */
    public SoundNotificationAction( String soundDescriptor,
                                    int loopInterval,
                                    boolean isSoundNotificationEnabled,
                                    boolean isSoundPlaybackEnabled,
                                    boolean isSoundPCSpeakerEnabled)
    {
        super(NotificationAction.ACTION_SOUND);
        this.soundFileDescriptor = soundDescriptor;
        this.loopInterval = loopInterval;
        this.isSoundNotificationEnabled = isSoundNotificationEnabled;
        this.isSoundPlaybackEnabled = isSoundPlaybackEnabled;
        this.isSoundPCSpeakerEnabled = isSoundPCSpeakerEnabled;
    }

    /**
     * Returns the loop interval. This is the interval of milliseconds to wait
     * before repeating the sound, when playing a sound in loop. By default this
     * method returns -1.
     *
     * @return the loop interval
     */
    public int getLoopInterval()
    {
        return loopInterval;
    }

    /**
     * Changes the loop interval. This is the interval of milliseconds to wait
     * before repeating the sound, when playing a sound in loop.
     *
     * @return the loop interval
     */
    public void setLoopInterval(int loopInterval)
    {
        this.loopInterval = loopInterval;
    }

    /**
     * Returns the descriptor pointing to the sound to be played.
     *
     * @return the descriptor pointing to the sound to be played.
     */
    public String getDescriptor()
    {
        return soundFileDescriptor;
    }

    public Map<BgTag, String> getSoundFileDescriptorsMap()
    {
        return soundFileDescriptorsMap;
    }

    public void setSoundFileDescriptorsMapEntry(BgTag key, String soundFileDescriptor)
    {
        soundFileDescriptorsMap.put(key, soundFileDescriptor);
    }

    /**
     * Sets the soundFileDescriptor for the selected tag. If the tag doesn't exist then the soundFileDescriptor remains
     * unchanged.
     */
    public void selectSoundFileDescriptorByGroup(BgTag key)
    {
        soundFileDescriptor = soundFileDescriptorsMap.getOrDefault(key, soundFileDescriptor);
    }

    /**
     * Returns if this sound is to be played on notification device.
     *
     * @return True if this sound is played on notification device.
     * False Otherwise.
     */
    public boolean isSoundNotificationEnabled()
    {
        return isSoundNotificationEnabled;
    }

    /**
     * Returns if this sound is to be played on playback device.
     *
     * @return True if this sound is played on playback device.
     * False Otherwise.
     */
    public boolean isSoundPlaybackEnabled()
    {
        return isSoundPlaybackEnabled;
    }

    /**
     * Returns if this sound is to be played on pc speaker device.
     *
     * @return True if this sound is played on pc speaker device.
     * False Otherwise.
     */
    public boolean isSoundPCSpeakerEnabled()
    {
        return isSoundPCSpeakerEnabled;
    }

    /**
     * Enables or disables this sound for notification device.
     *
     * @param isSoundEnabled True if this sound is played on notification
     *                       device. False Otherwise.
     */
    public void setSoundNotificationEnabled(boolean isSoundEnabled)
    {
        this.isSoundNotificationEnabled = isSoundEnabled;
    }

    /**
     * Enables or disables this sound for playback device.
     *
     * @param isSoundEnabled True if this sound is played on playback
     *                       device. False Otherwise.
     */
    public void setSoundPlaybackEnabled(boolean isSoundEnabled)
    {
        this.isSoundPlaybackEnabled = isSoundEnabled;
    }

    /**
     * Enables or disables this sound for pc speaker device.
     *
     * @param isSoundEnabled True if this sound is played on speaker
     *                       device. False Otherwise.
     */
    public void setSoundPCSpeakerEnabled(boolean isSoundEnabled)
    {
        this.isSoundPCSpeakerEnabled = isSoundEnabled;
    }
}
