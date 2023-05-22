/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import org.apache.commons.lang3.*;
import org.jitsi.service.protocol.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

/**
 * Handles DTMF sending and playing sound notifications for that.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class DTMFHandler
    implements KeyEventDispatcher,
                Runnable
{
    /**
     * The <tt>Logger</tt> used by the <tt>DTMFHandler</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(DTMFHandler.class);

    /**
     * The call dialog, where this handler is registered.
     */
    private final CallFrame callFrame;

    /**
     * The <tt>KeyboadFocusManager</tt> to which this instance is added as a
     * <tt>KeyEventDispatcher</tt>.
     */
    private KeyboardFocusManager keyboardFocusManager;

    /**
     * The <tt>Window</tt>s which this instance listens to for key presses and
     * releases.
     */
    private final List<Window> parents = new ArrayList<>();

    /**
     * If we are currently playing an audio for a DTMF tone. Used
     * to play in Loop and stop it if forced to do or new tone has come.
     */
    NotificationData currentlyPlayingTone;

    /**
     * Default event type for DTMF tone.
     */
    public static final String DTMF_TONE_PREFIX = "DTMFTone.";

    /**
     * The list of audio DTMF tones to play.
     */
    private Vector<DTMFToneInfo> dmtfToneNotifications
        = new Vector<>(1, 1);

    /**
     * The thread which plays the audio DTMF notifications.
     */
    private Thread dtmfToneNotificationThread;

    /**
     * The standard consumer DTMF tones, along with their associated properties
     * such as button images and sound resources.
     */
    public static final DTMFToneInfo[] STANDARD_TONES
        = new DTMFToneInfo[]
                {
                    new DTMFToneInfo(
                        DTMFTone.DTMF_1,
                        KeyEvent.VK_1,
                        '1',
                        ImageLoaderService.ONE_DIAL_BUTTON,
                        ImageLoaderService.ONE_DIAL_BUTTON_MAC,
                        ImageLoaderService.ONE_DIAL_BUTTON_MAC_ROLLOVER,
                        ImageLoaderService.ONE_DIAL_BUTTON_MAC_PRESSED,
                        SoundProperties.DIAL_ONE),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_2,
                        KeyEvent.VK_2,
                        '2',
                        ImageLoaderService.TWO_DIAL_BUTTON,
                        ImageLoaderService.TWO_DIAL_BUTTON_MAC,
                        ImageLoaderService.TWO_DIAL_BUTTON_MAC_ROLLOVER,
                        ImageLoaderService.TWO_DIAL_BUTTON_MAC_PRESSED,
                        SoundProperties.DIAL_TWO),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_3,
                        KeyEvent.VK_3,
                        '3',
                        ImageLoaderService.THREE_DIAL_BUTTON,
                        ImageLoaderService.THREE_DIAL_BUTTON_MAC,
                        ImageLoaderService.THREE_DIAL_BUTTON_MAC_ROLLOVER,
                        ImageLoaderService.THREE_DIAL_BUTTON_MAC_PRESSED,
                        SoundProperties.DIAL_THREE),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_4,
                        KeyEvent.VK_4,
                        '4',
                        ImageLoaderService.FOUR_DIAL_BUTTON,
                        ImageLoaderService.FOUR_DIAL_BUTTON_MAC,
                        ImageLoaderService.FOUR_DIAL_BUTTON_MAC_ROLLOVER,
                        ImageLoaderService.FOUR_DIAL_BUTTON_MAC_PRESSED,
                        SoundProperties.DIAL_FOUR),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_5,
                        KeyEvent.VK_5,
                        '5',
                        ImageLoaderService.FIVE_DIAL_BUTTON,
                        ImageLoaderService.FIVE_DIAL_BUTTON_MAC,
                        ImageLoaderService.FIVE_DIAL_BUTTON_MAC_ROLLOVER,
                        ImageLoaderService.FIVE_DIAL_BUTTON_MAC_PRESSED,
                        SoundProperties.DIAL_FIVE),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_6,
                        KeyEvent.VK_6,
                        '6',
                        ImageLoaderService.SIX_DIAL_BUTTON,
                        ImageLoaderService.SIX_DIAL_BUTTON_MAC,
                        ImageLoaderService.SIX_DIAL_BUTTON_MAC_ROLLOVER,
                        ImageLoaderService.SIX_DIAL_BUTTON_MAC_PRESSED,
                        SoundProperties.DIAL_SIX),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_7,
                        KeyEvent.VK_7,
                        '7',
                        ImageLoaderService.SEVEN_DIAL_BUTTON,
                        ImageLoaderService.SEVEN_DIAL_BUTTON_MAC,
                        ImageLoaderService.SEVEN_DIAL_BUTTON_MAC_ROLLOVER,
                        ImageLoaderService.SEVEN_DIAL_BUTTON_MAC_PRESSED,
                        SoundProperties.DIAL_SEVEN),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_8,
                        KeyEvent.VK_8,
                        '8',
                        ImageLoaderService.EIGHT_DIAL_BUTTON,
                        ImageLoaderService.EIGHT_DIAL_BUTTON_MAC,
                        ImageLoaderService.EIGHT_DIAL_BUTTON_MAC_ROLLOVER,
                        ImageLoaderService.EIGHT_DIAL_BUTTON_MAC_PRESSED,
                        SoundProperties.DIAL_EIGHT),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_9,
                        KeyEvent.VK_9,
                        '9',
                        ImageLoaderService.NINE_DIAL_BUTTON,
                        ImageLoaderService.NINE_DIAL_BUTTON_MAC,
                        ImageLoaderService.NINE_DIAL_BUTTON_MAC_ROLLOVER,
                        ImageLoaderService.NINE_DIAL_BUTTON_MAC_PRESSED,
                        SoundProperties.DIAL_NINE),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_STAR,
                        KeyEvent.VK_ASTERISK,
                        '*',
                        ImageLoaderService.STAR_DIAL_BUTTON,
                        ImageLoaderService.STAR_DIAL_BUTTON_MAC,
                        ImageLoaderService.STAR_DIAL_BUTTON_MAC_ROLLOVER,
                        ImageLoaderService.STAR_DIAL_BUTTON_MAC_PRESSED,
                        SoundProperties.DIAL_STAR),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_0,
                        KeyEvent.VK_0,
                        '0',
                        ImageLoaderService.ZERO_DIAL_BUTTON,
                        ImageLoaderService.ZERO_DIAL_BUTTON_MAC,
                        ImageLoaderService.ZERO_DIAL_BUTTON_MAC_ROLLOVER,
                        ImageLoaderService.ZERO_DIAL_BUTTON_MAC_PRESSED,
                        SoundProperties.DIAL_ZERO),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_SHARP,
                        KeyEvent.VK_NUMBER_SIGN,
                        '#',
                        ImageLoaderService.DIEZ_DIAL_BUTTON,
                        ImageLoaderService.DIEZ_DIAL_BUTTON_MAC,
                        ImageLoaderService.DIEZ_DIAL_BUTTON_MAC_ROLLOVER,
                        ImageLoaderService.DIEZ_DIAL_BUTTON_MAC_PRESSED,
                        SoundProperties.DIAL_DIEZ)
                };

    /**
     * DTMF tones A, B, C and D; along with their associated images and sound
     * resources.
     * These tones are generally reserved for military applications and for
     * trunk signalling, so are not usually used by consumers.
     */
    public static final DTMFToneInfo[] SPECIAL_TONES
        = new DTMFToneInfo[]
            {
                  new DTMFToneInfo(
                  DTMFTone.DTMF_A,
                  KeyEvent.VK_A,
                  'a',
                  null,
                  null,
                  null,
                  null,
                  null),
              new DTMFToneInfo(
                  DTMFTone.DTMF_B,
                  KeyEvent.VK_B,
                  'b',
                  null,
                  null,
                  null,
                  null,
                  null),
              new DTMFToneInfo(
                  DTMFTone.DTMF_C,
                  KeyEvent.VK_C,
                  'c',
                  null,
                  null,
                  null,
                  null,
                  null),
              new DTMFToneInfo(
                  DTMFTone.DTMF_D,
                  KeyEvent.VK_D,
                  'd',
                  null,
                  null,
                  null,
                  null,
                  null),
            };

    /**
     * Whether or not the user is permitted to send the special DTMF tones
     * A, B, C and D during calls.
     */
    public static final boolean ALLOW_SPECIAL_TONES =
        GuiActivator.getConfigurationService().user().getBoolean(
         "net.java.sip.communicator.impl.gui.main.call.ENABLE_SPECIAL_DTMF_TONES",
         true);

    /**
     * The DTMF tones the user is able to send when in calls.
     */
    public static final DTMFToneInfo[] AVAILABLE_TONES = ALLOW_SPECIAL_TONES ?
        ArrayUtils.addAll(STANDARD_TONES, SPECIAL_TONES) :
        STANDARD_TONES;

    /**
     * Whether we have already loaded the defaults for dtmf tones.
     */
    private static boolean defaultsLoaded = false;

    /**
     * Creates DTMF handler for a call.
     */
    public DTMFHandler()
    {
        this(null);
    }

    /**
     * Creates DTMF handler for a call.
     *
     * @param callContainer the <tt>CallFrame</tt> where this handler is
     * registered
     */
    public DTMFHandler(CallFrame callContainer)
    {
        loadDefaults();

        callFrame = callContainer;

        if (callFrame != null)
        {
            callFrame.addWindowListener(
                    new WindowAdapter()
                    {
                        @Override
                        public void windowClosed(WindowEvent e)
                        {
                            removeParent(callFrame);
                        }

                        @Override
                        public void windowOpened(WindowEvent e)
                        {
                            addParent(callFrame);
                        }
                    });
            if (callFrame.isVisible())
                addParent(callFrame);
        }
    }

    /**
     * Load the defaults for dtmf tones.
     */
    public static synchronized void loadDefaults()
    {
        if(defaultsLoaded)
            return;

        // init the
        NotificationService notificationService =
            GuiActivator.getNotificationService();

        for(DTMFToneInfo info : AVAILABLE_TONES)
        {
            notificationService.registerDefaultNotificationForEvent(
                DTMF_TONE_PREFIX + info.tone.getValue(),
                new SoundNotificationAction(
                    info.sound, 0, false, true, false));
        }

        logger.info("Loaded " + AVAILABLE_TONES.length + " DTMF tones");
        defaultsLoaded = true;
    }

    /**
     * Adds a <tt>Window</tt> on which key presses and releases are to be
     * monitored for the purposes of this <tt>DTMFHandler</tt>.
     *
     * @param parent the <tt>Window</tt> on which key presses and releases are
     * to be monitored for the purposes of this <tt>DTMFHandler</tt>
     */
    public void addParent(Window parent)
    {
        synchronized (parents)
        {
            if (!parents.contains(parent)
                    && parents.add(parent)
                    && (keyboardFocusManager == null))
            {
                keyboardFocusManager
                    = KeyboardFocusManager.getCurrentKeyboardFocusManager();
                keyboardFocusManager.addKeyEventDispatcher(this);
            }
        }
    }

    /**
     * Removes a <tt>Window</tt> on which key presses and releases are to no
     * longer be monitored for the purposes of this <tt>DTMFHandler</tt>.
     *
     * @param parent the <tt>Window</tt> on which key presses and releases are
     * to no longer be monitored for the purposes of this <tt>DTMFHandler</tt>
     */
    public void removeParent(Window parent)
    {
        synchronized (parents)
        {
            if (parents.remove(parent)
                    && parents.isEmpty()
                    && (keyboardFocusManager != null))
            {
                keyboardFocusManager.removeKeyEventDispatcher(this);
                keyboardFocusManager = null;
            }
        }
    }

    /**
     * Dispatches a specific <tt>KeyEvent</tt>. If one of the <tt>parents</tt>
     * registered with this <tt>DTMFHandler</tt> is focused, starts or stops
     * sending a respective DTMF tone.
     *
     * @param e the <tt>KeyEvent</tt> to be dispatched
     * @return <tt>true</tt> to stop dispatching the event or <tt>false</tt> to
     * continue dispatching it. <tt>DTMFHandler</tt> always returns
     * <tt>false</tt>
     */
    public boolean dispatchKeyEvent(KeyEvent e)
    {
        if (e.getID() == KeyEvent.KEY_TYPED)
            return false;

        /*
         * When the UI uses a single window and we do not have a callContainer,
         * we do not seem to be able to deal with the situation.
         */
        if ((callFrame == null))
            return false;

        boolean dispatch = false;

        synchronized (parents)
        {
            for (int i = 0, count = parents.size(); i < count; i++)
            {
                if (parents.get(i).isFocused())
                {
                    dispatch = true;
                    break;
                }
            }
        }

        // If we are not focused, the KeyEvent was not meant for us.
        if (dispatch)
        {
            for (int i = 0; i < AVAILABLE_TONES.length; i++)
            {
                DTMFToneInfo info = AVAILABLE_TONES[i];

                if (info.keyChar == e.getKeyChar())
                {
                    switch (e.getID())
                    {
                    case KeyEvent.KEY_PRESSED:
                        logger.debug("User pressed DTMF");
                        startSendingDtmfTone(info);
                        break;
                    case KeyEvent.KEY_RELEASED:
                        logger.debug("User released DTMF");
                        stopSendingDtmfTone();
                        break;
                    }
                    break;
                }
            }
        }

        return false;
    }

    /**
     * Sends a DTMF tone to the current DTMF operation set.
     *
     * @param toneValue the value of the DTMF tone to send.
     */
    public void startSendingDtmfTone(String toneValue)
    {
        for (int i = 0; i < AVAILABLE_TONES.length; i++)
        {
            DTMFToneInfo info = AVAILABLE_TONES[i];

            if (info.tone.getValue().equals(toneValue))
            {
                startSendingDtmfTone(info);
                return;
            }
        }
    }

    /**
     * Sends a DTMF tone to the current DTMF operation set.
     *
     * @param info The DTMF tone to send.
     */
    private synchronized void startSendingDtmfTone(DTMFToneInfo info)
    {
        if(info.sound != null)
        {
            synchronized(dmtfToneNotifications)
            {
                boolean startThread = (dmtfToneNotifications.size() == 0);
                dmtfToneNotifications.add(info);
                if(startThread)
                {
                    dtmfToneNotificationThread
                        = new Thread(
                                this,
                                "DTMFHandler: DTMF tone notification player");
                    dtmfToneNotificationThread.start();
                }
            }
        }

        if (callFrame != null)
        {
            callFrame.dtmfToneSent(info);
            for (Call call : callFrame.getCallConference().getCalls())
            {
                startSendingDtmfTone(
                    call,
                    info);
            }
        }
        else
        {
            Collection<Call> activeCalls = CallManager.getInProgressCalls();

            if (activeCalls != null)
            {
                for (Call activeCall : activeCalls)
                    startSendingDtmfTone(activeCall, info);
            }
        }
    }

    /**
     * Sends a DTMF tone to the current DTMF operation set of the given call.
     *
     * @param call The call to which we send DTMF-s.
     * @param info The DTMF tone to send.
     */
    private void startSendingDtmfTone(Call call, DTMFToneInfo info)
    {
        Iterator<? extends CallPeer> callPeers = call.getCallPeers();

        try
        {
            while (callPeers.hasNext())
            {
                CallPeer peer = callPeers.next();
                OperationSetDTMF dtmfOpSet
                    = peer.getProtocolProvider().getOperationSet(
                            OperationSetDTMF.class);

                if (dtmfOpSet != null)
                {
                    dtmfOpSet.startSendingDTMF(peer, info.tone);
                }
            }
        }
        catch (Throwable t)
        {
            logger.error("Failed to send a DTMF tone.", t);
        }
    }

    /**
     * Stop sending DTMF tone.
     */
    public synchronized void stopSendingDtmfTone()
    {
        if (callFrame != null)
        {
            for (Call call : callFrame.getCallConference().getCalls())
            {
                stopSendingDtmfTone(call);
            }
        }
        else
        {
            Collection<Call> activeCalls = CallManager.getInProgressCalls();

            if (activeCalls != null)
            {
                for (Call activeCall : activeCalls)
                    stopSendingDtmfTone(activeCall);
            }
        }
    }

    /**
     * Stops sending DTMF tone to the given call.
     *
     * @param call The call to which we send DTMF-s.
     */
    private void stopSendingDtmfTone(Call call)
    {
        Iterator<? extends CallPeer> callPeers = call.getCallPeers();

        try
        {
            while (callPeers.hasNext())
            {
                CallPeer peer = callPeers.next();
                OperationSetDTMF dtmfOpSet
                    = peer.getProtocolProvider().getOperationSet(
                            OperationSetDTMF.class);

                if (dtmfOpSet != null)
                    dtmfOpSet.stopSendingDTMF(peer);
            }
        }
        catch (Throwable t)
        {
            logger.error("Failed to send a DTMF tone.", t);
        }
    }

    /**
     * DTMF extended information.
     */
    public static class DTMFToneInfo
    {
        /**
         * The tone itself
         */
        public final DTMFTone tone;

        /**
         * The key code when entered from keyboard.
         */
        public final int keyCode;

        /**
         * The char associated with this DTMF tone.
         */
        public final char keyChar;

        /**
         * The image to display in buttons sending DTMFs.
         */
        public final ImageID imageID;

        /**
         * The image to display on Mac buttons.
         */
        public final ImageID macImageID;

        /**
         * The id of the image to display on Mac buttons on rollover.
         */
        public final ImageID macImageRolloverID;

        /**
         * The id of the image to display on Mac buttons when pressed.
         */
        public final ImageID macImagePressedID;

        /**
         * The sound to play during send of this tone.
         */
        public final String sound;

        /**
         * Creates DTMF extended info.
         * @param tone the tone.
         * @param keyCode its key code.
         * @param keyChar the char associated with the DTMF
         * @param imageID the image if any.
         * @param macImageID the Mac OS X-specific image if any.
         * @param macImageRolloverID the Mac OS X-specific rollover image if any
         * @param macImagePressedID the Mac OS X-specific pressed image if any
         * @param sound the sound if any.
         */
        public DTMFToneInfo(
            DTMFTone tone,
            int keyCode, char keyChar,
            ImageID imageID, ImageID macImageID, ImageID macImageRolloverID,
            ImageID macImagePressedID,
            String sound)
        {
            this.tone = tone;
            this.keyCode = keyCode;
            this.keyChar = keyChar;
            this.imageID = imageID;
            this.macImageID = macImageID;
            this.macImageRolloverID = macImageRolloverID;
            this.macImagePressedID = macImagePressedID;
            this.sound = sound;
        }
    }

    /**
     * Runnable used to read all waiting DTMF tone notification.
     */
    public void run()
    {
        boolean moreToPlay = (dmtfToneNotifications.size() > 0);
        DTMFToneInfo toneToPlay = null;
        while(moreToPlay)
        {
            toneToPlay = dmtfToneNotifications.get(0);
            if(toneToPlay.sound != null)
            {
                // Plays the next DTMF sond notification.
                currentlyPlayingTone
                    = GuiActivator.getNotificationService().fireNotification(
                            DTMF_TONE_PREFIX + toneToPlay.tone.getValue());

                // Waits for the current notification to end.
                while(GuiActivator.getNotificationService()
                        .isPlayingNotification(currentlyPlayingTone))
                {
                    Thread.yield();
                }
                // Removes the ended notification from the DTMF list.
                GuiActivator.getNotificationService()
                    .stopNotification(currentlyPlayingTone);
                synchronized(dmtfToneNotifications)
                {
                    dmtfToneNotifications.remove(0);
                    moreToPlay = (dmtfToneNotifications.size() > 0);
                }
            }
        }
    }
}
