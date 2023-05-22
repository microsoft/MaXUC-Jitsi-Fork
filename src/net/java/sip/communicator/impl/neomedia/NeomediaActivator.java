/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.neomedia;

import java.util.*;

import org.jitsi.impl.neomedia.*;
import org.jitsi.service.audionotifier.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.fileaccess.*;
import org.jitsi.service.libjitsi.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.packetlogging.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

import net.java.sip.communicator.service.browserlauncher.BrowserLauncherService;
import net.java.sip.communicator.service.commportal.*;
import net.java.sip.communicator.service.conference.ConferenceService;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.headsetmanager.HeadsetManagerService;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

/**
 * Implements <tt>BundleActivator</tt> for the neomedia bundle.
 *
 * @author Martin Andre
 * @author Emil Ivov
 * @author Lyubomir Marinov
 * @author Boris Grozev
 */
public class NeomediaActivator
    implements BundleActivator
{
    /**
     * Indicates if the audio configuration form should be disabled, i.e.
     * not visible to the user.
     */
    protected static final String AUDIO_CONFIG_DISABLED_PROP
        = "net.java.sip.communicator.impl.neomedia.AUDIO_CONFIG_DISABLED";

    /**
     *  The audio configuration form used to define the capture/notify/playback
     *  audio devices.
     */
    private static ConfigurationForm audioConfigurationForm;

    /**
     * The audio notifier service.
     */
    private static AudioNotifierService audioNotifierService;

    /**
     * The context in which the one and only <tt>NeomediaActivator</tt> instance
     * has started executing.
     */
    private static BundleContext bundleContext;

    /**
     * The current instance of the <tt>ConferenceService</tt> to suppress camera
     * previews if video is in use by the conference service.
     */
    private static ConferenceService conferenceService;

    /**
     * The <tt>ConfigurationService</tt> registered in {@link #bundleContext}
     * and used by the <tt>NeomediaActivator</tt> instance to read and write
     * configuration properties.
     */
    private static ConfigurationService configurationService;

    /**
     * The name of the notification pop-up event displayed when the device
     * configration has changed.
     */
    public static final String DEVICE_CONFIGURATION_HAS_CHANGED
        = "DeviceConfigurationChanged";

    /**
     * The <tt>FileAccessService</tt> registered in {@link #bundleContext} and
     * used by the <tt>NeomediaActivator</tt> instance to safely access files.
     */
    private static FileAccessService fileAccessService;

    /**
     * A {@link MediaConfigurationService} instance.
     */
    private static MediaConfigurationImpl mediaConfiguration;

    /**
     * The one and only <tt>MediaServiceImpl</tt> instance registered in
     * {@link #bundleContext} by the <tt>NeomediaActivator</tt> instance.
     */
    private static MediaServiceImpl mediaServiceImpl;

    /**
     * The name of the notification pop-up event displayed when a new device
     * is selected (for audio in, audio out or notifications).
     */
    public static final String NEW_SELECTED_DEVICE
        = "NewSelectedDevice";

    /**
     * The notifcation service to pop-up messages.
     */
    private static NotificationService notificationService;

    /**
     * The OSGi <tt>PacketLoggingService</tt> of {@link #mediaServiceImpl} in
     * {@link #bundleContext} and used for debugging.
     */
    private static PacketLoggingService packetLoggingService  = null;

    /**
     * The <tt>ResourceManagementService</tt> registered in
     * {@link #bundleContext} and representing the resources such as
     * internationalized and localized text and images used by the neomedia
     * bundle.
     */
    private static ResourceManagementService resources;

    /**
     * The UI service
     */
    private static UIService uiService;

    /**
     * The CoS service
     */
    private static ClassOfServiceService cosService;

    /**
     * The browser launcher service
     */
    private static BrowserLauncherService browserLauncherService;

    /**
     * The headset manager service
     */
    private static HeadsetManagerService headsetManagerService;

    /**
     * Indicates if the video configuration form should be disabled, i.e.
     * not visible to the user.
     */
    protected static final String VIDEO_CONFIG_DISABLED_PROP
        = "net.java.sip.communicator.impl.neomedia.VIDEO_CONFIG_DISABLED";

    /**
     *  A listener to the click on the popup message concerning video device
     *  configuration changes.
     */
    private VideoDeviceConfigurationListener
        videoDeviceConfigurationPropertyChangeListener;

    /**
     *  The video configuration form.
     */
    private static ConfigurationForm videoConfigurationForm;

    /**
     * Returns the audio configuration form used to define the
     * capture/notify/playback audio devices.
     *
     * @return The audio configuration form used to define the
     * capture/notify/playback audio devices.
     */
    public static ConfigurationForm getAudioConfigurationForm()
    {
        return audioConfigurationForm;
    }

    /**
     * Returns the <tt>AudioService</tt> obtained from the bundle
     * context.
     * @return the <tt>AudioService</tt> obtained from the bundle
     * context
     */
    public static AudioNotifierService getAudioNotifierService()
    {
        if(audioNotifierService == null)
        {
            audioNotifierService
                = ServiceUtils.getService(
                        bundleContext,
                        AudioNotifierService.class);
        }
        return audioNotifierService;
    }

    /**
     * Returns the context in which the one and only <tt>NeomediaActivator</tt>
     * instance has started executing.
     *
     * @return The context in which the one and only <tt>NeomediaActivator</tt>
     * instance has started executing.
     */
    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    /**
     * @return a reference to the registered conference service
     */
    public static ConferenceService getConferenceService()
    {
        if (conferenceService == null)
            conferenceService = ServiceUtils.getService(bundleContext,
                                                        ConferenceService.class);

        return conferenceService;
    }

    /**
     * Returns a reference to a ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the ConfigurationService.
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configurationService == null)
        {
            configurationService
                = ServiceUtils.getService(
                        bundleContext,
                        ConfigurationService.class);
        }
        return configurationService;
    }

    /**
     * Returns a reference to a FileAccessService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the
     * FileAccessService .
     */
    public static FileAccessService getFileAccessService()
    {
        if (fileAccessService == null)
        {
            fileAccessService
                = ServiceUtils.getService(
                        bundleContext,
                        FileAccessService.class);
        }
        return fileAccessService;
    }

    public static MediaConfigurationService getMediaConfiguration()
    {
        return mediaConfiguration;
    }

    /**
     * Gets the <tt>MediaService</tt> implementation instance registered by the
     * neomedia bundle.
     *
     * @return the <tt>MediaService</tt> implementation instance registered by
     * the neomedia bundle
     */
    public static MediaServiceImpl getMediaServiceImpl()
    {
        return mediaServiceImpl;
    }

    /**
     * Returns the <tt>NotificationService</tt> obtained from the bundle
     * context.
     *
     * @return The <tt>NotificationService</tt> obtained from the bundle
     * context.
     */
    public static NotificationService getNotificationService()
    {
        if(notificationService == null)
        {
            // Get the notification service implementation
            ServiceReference<?> notifReference = bundleContext
                .getServiceReference(NotificationService.class.getName());

            notificationService = (NotificationService) bundleContext
                .getService(notifReference);

            if(notificationService != null)
            {
                // Register a popup message for a device configuration changed
                // notification.
                notificationService.registerDefaultNotificationForEvent(
                        DEVICE_CONFIGURATION_HAS_CHANGED,
                        net.java.sip.communicator.service.notification.NotificationAction.ACTION_POPUP_MESSAGE,
                        "Device configuration has changed",
                        null);

                // Register a popup message for a new device selected for audio
                // in, audio out or notifications.
                notificationService.registerDefaultNotificationForEvent(
                        NEW_SELECTED_DEVICE,
                        net.java.sip.communicator.service.notification.NotificationAction.ACTION_POPUP_MESSAGE,
                        "New selected device",
                        null);
            }
        }

        return notificationService;
    }

    /**
     * Returns a reference to the <tt>PacketLoggingService</tt> implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a reference to a <tt>PacketLoggingService</tt> implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     */
    public static PacketLoggingService getPacketLogging()
    {
        if (packetLoggingService == null)
        {
            packetLoggingService
                = ServiceUtils.getService(
                        bundleContext,
                        PacketLoggingService.class);
        }
        return packetLoggingService;
    }

    /**
     * Gets the <tt>ResourceManagementService</tt> instance which represents the
     * resources such as internationalized and localized text and images used by
     * the neomedia bundle.
     *
     * @return the <tt>ResourceManagementService</tt> instance which represents
     * the resources such as internationalized and localized text and images
     * used by the neomedia bundle
     */
    public static ResourceManagementService getResources()
    {
        if (resources == null)
        {
            resources
                = ResourceManagementServiceUtils.getService(bundleContext);
        }
        return resources;
    }

    /**
     * Returns the UI service
     *
     * @return the UI service
     */
    public static UIService getUIService()
    {
        if (uiService == null)
        {
            uiService = ServiceUtils.getService(bundleContext, UIService.class);
        }

        return uiService;
    }

    /**
     * Returns the CoS service
     *
     * @return the CoS service
     */
    public static ClassOfServiceService getCosService()
    {
        if (cosService == null)
        {
            cosService = ServiceUtils.getService(
                bundleContext, ClassOfServiceService.class);
        }

        return cosService;
    }

    /**
     * Returns the Browser Launcher service
     *
     * @return the Browser Launcher service
     */
    public static BrowserLauncherService getBrowserLauncher()
    {
        if (browserLauncherService == null)
        {
            browserLauncherService = ServiceUtils.getService(
                bundleContext, BrowserLauncherService.class);
        }

        return browserLauncherService;
    }

    /**
     * Returns the Headset Manager Service
     *
     * @return the Headset Manager Service
     */
    public static HeadsetManagerService getHeadsetManager()
    {
        if (headsetManagerService == null)
        {
            headsetManagerService = ServiceUtils.getService(
                bundleContext, HeadsetManagerService.class);
        }

        return headsetManagerService;
    }

    /**
     * Returns the video configuration form.
     *
     * @return The video configuration form.
     */
    public static ConfigurationForm getVideoConfigurationForm()
    {
        return videoConfigurationForm;
    }

    /**
     * A listener to the click on the popup message concerning audio device
     * configuration changes.
     */
    private AudioDeviceConfigurationListener
        audioDeviceConfigurationPropertyChangeListener;

    /**
     * The <tt>Logger</tt> used by the <tt>NeomediaActivator</tt> class and its
     * instances for logging output.
     */
    private final Logger logger = Logger.getLogger(NeomediaActivator.class);

    /**
     * Starts the execution of the neomedia bundle in the specified context.
     *
     * @param bundleContext the context in which the neomedia bundle is to start
     * executing
     */
    public void start(BundleContext bundleContext)
    {
        logger.debug("Started.");

        NeomediaActivator.bundleContext = bundleContext;

        // MediaService
        mediaServiceImpl = (MediaServiceImpl) LibJitsi.getMediaService();

        bundleContext.registerService(
                MediaService.class.getName(),
                mediaServiceImpl,
                null);
        logger.debug("Media Service ... [REGISTERED]");

        mediaConfiguration = new MediaConfigurationImpl();
        bundleContext.registerService(
                MediaConfigurationService.class.getName(),
                getMediaConfiguration(),
                null);
        logger.debug("Media Configuration ... [REGISTERED]");

        final ConfigurationService cfg = NeomediaActivator.getConfigurationService();
        Dictionary<String, String> mediaProps = new Hashtable<>();

        mediaProps.put( ConfigurationForm.FORM_TYPE,
                        ConfigurationForm.GENERAL_TYPE);

        audioConfigurationForm
            = new LazyConfigurationForm(
                    AudioConfigurationPanel.class.getName(),
                    getClass().getClassLoader(),
                    "plugin.mediaconfig.AUDIO_ICON",
                    "impl.neomedia.configform.AUDIO",
                    20);

        bundleContext.registerService(
                ConfigurationForm.class.getName(),
                audioConfigurationForm,
                mediaProps);

        // Initializes and registers the changed audio device configuration
        // event at the notification service.
        if (audioDeviceConfigurationPropertyChangeListener == null)
        {
            getNotificationService();

            audioDeviceConfigurationPropertyChangeListener
                = new AudioDeviceConfigurationListener(
                        audioConfigurationForm);
            mediaServiceImpl
                .getDeviceConfiguration()
                .addPropertyChangeListener(
                        audioDeviceConfigurationPropertyChangeListener);
        }

        logger.info("Registering video configuration form.");
        videoConfigurationForm
            = new LazyConfigurationForm(
                    VideoConfigurationPanel.class.getName(),
                    getClass().getClassLoader(),
                    "plugin.mediaconfig.VIDEO_ICON",
                    "impl.neomedia.configform.VIDEO",
                    21);

        bundleContext.registerService(
                ConfigurationForm.class.getName(),
                videoConfigurationForm,
                mediaProps);

        // Initializes and registers the changed video device configuration
        // event at the notification service.
        if (videoDeviceConfigurationPropertyChangeListener == null)
        {
            getNotificationService();

            videoDeviceConfigurationPropertyChangeListener
                = new VideoDeviceConfigurationListener(
                        videoConfigurationForm);
            mediaServiceImpl
                .getDeviceConfiguration()
                .addPropertyChangeListener(
                        videoDeviceConfigurationPropertyChangeListener);
        }

        //we use the nist-sdp stack to make parse sdp and we need to set the
        //following property to make sure that it would accept java generated
        //IPv6 addresses that contain address scope zones.
        System.setProperty("gov.nist.core.STRIP_ADDR_SCOPES", "true");

        // AudioNotifierService
        AudioNotifierService audioNotifierService
            = LibJitsi.getAudioNotifierService();

        bundleContext.registerService(
                AudioNotifierService.class.getName(),
                audioNotifierService,
                null);

        logger.info("Audio Notifier Service ...[REGISTERED]");
    }

    /**
     * Stops the execution of the neomedia bundle in the specified context.
     *
     * @param bundleContext the context in which the neomedia bundle is to stop
     * executing
     */
    public void stop(BundleContext bundleContext)
    {
        try
        {
            if(audioDeviceConfigurationPropertyChangeListener != null)
            {
                mediaServiceImpl
                    .getDeviceConfiguration()
                        .removePropertyChangeListener(
                                audioDeviceConfigurationPropertyChangeListener);
                audioDeviceConfigurationPropertyChangeListener.dispose();
                audioDeviceConfigurationPropertyChangeListener = null;
            }

            if(videoDeviceConfigurationPropertyChangeListener != null)
            {
                mediaServiceImpl
                    .getDeviceConfiguration()
                        .removePropertyChangeListener(
                                videoDeviceConfigurationPropertyChangeListener);
                videoDeviceConfigurationPropertyChangeListener.dispose();
                videoDeviceConfigurationPropertyChangeListener = null;
            }
        }
        finally
        {
            configurationService = null;
            fileAccessService = null;
            mediaServiceImpl = null;
            resources = null;
        }
    }
}
