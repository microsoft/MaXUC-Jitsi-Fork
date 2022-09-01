/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.osdependent.jdic;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.impl.osdependent.*;
import net.java.sip.communicator.impl.osdependent.SystemTray;
import net.java.sip.communicator.impl.osdependent.TrayIcon;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.service.systray.event.*;
import net.java.sip.communicator.util.Logger;

/**
 * The <tt>Systray</tt> provides a Icon and the associated <tt>TrayMenu</tt>
 * in the system tray using the Jdic library.
 *
 * @author Nicolas Chamouard
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 * @author Symphorien Wanko
 */
public class SystrayServiceJdicImpl implements SystrayService
{
    private final SystemTray systray;

    /**
     * The icon in the system tray.
     */
    private TrayIcon trayIcon;

    /**
     * The menu that spring with a right click.
     */
    private Object menu;

    /**
     * The popup handler currently used to show popup messages
     */
    private PopupMessageHandler activePopupHandler;

    /**
     * A set of usable <tt>PopupMessageHandler</tt>
     */
    private final Hashtable<String, PopupMessageHandler> popupHandlerSet
        = new Hashtable<>();

    /**
     * The <tt>ConfigurationService</tt> obtained from the associated
     * <tt>BundleActivator</tt>.
     */
    private final ConfigurationService cfg
        = OsDependentActivator.getConfigurationService();

    /**
     * The <tt>Logger</tt> used by the <tt>SystrayServiceJdicImpl</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(SystrayServiceJdicImpl.class);

    /**
     * Lock to restrict access to the system tray and the icon displayed by it.
     */
    private final Object trayLock = new Object();

    /**
     * The various icons used on the systray
     */
    private ImageIconFuture currentIcon = null;

    private ImageIconFuture logoIcon;

    private ImageIconFuture logoIconOffline;

    private ImageIconFuture logoIconWhite;

    private ImageIconFuture envelopeIcon;

    private ImageIconFuture envelopeIconWhite;

    private boolean initialized = false;

    /**
     * The listener which gets notified about pop-up message events (e.g. clicks
     * on the pop-up).
     */
    private final SystrayPopupMessageListener popupMessageListener
        = new SystrayPopupMessageListenerImpl();

    /**
     * List of listeners from early received calls to addPopupMessageListener.
     * Calls to addPopupMessageListener before the UIService is registered.
     */
    private List<SystrayPopupMessageListener> earlyAddedListeners = null;

    /**
     * Initializes a new <tt>SystrayServiceJdicImpl</tt> instance.
     */
    public SystrayServiceJdicImpl()
    {
        SystemTray systray;

        try
        {
            systray = SystemTray.getDefaultSystemTray();
        }
        catch (Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            else
            {
                systray = null;
                if (!GraphicsEnvironment.isHeadless())
                {
                    logger.error("Failed to create a systray!", t);
                }
                else
                {
                    // We are running headless, so this is probably expected,
                    // but since we've caught Throwable, we must log what
                    // went wrong.
                    logger.warn("Failed to create a systray!", t);
                }
            }
        }
        this.systray = systray;

        if (this.systray != null)
            initSystray();
    }

    /**
     * Initializes the systray icon and related listeners.
     */
    private void initSystray()
    {
        UIService uiService = OsDependentActivator.getUIService();

        logger.debug("Initialising systray");

        if (uiService == null)
        {
            /*
             * Delay the call to the #initSystray() method until the UIService
             * implementation becomes available.
             */
            try
            {
                logger.debug("Waiting for UI Service to initialise the systray");
                OsDependentActivator.bundleContext.addServiceListener(
                        new DelayedInitSystrayServiceListener(),
                        '('
                            + Constants.OBJECTCLASS
                            + '='
                            + UIService.class.getName()
                            + ')');
            }
            catch (InvalidSyntaxException ise)
            {
                /*
                 * Oh, it should not really happen. Besides, it is not clear at
                 * the time of this writing what is supposed to happen in the
                 * case of such an exception here.
                 */
                logger.error("Failed to listen for UI Service initialisation", ise);
            }
            return;
        }

        logger.debug("UI Service available, continue initialising systray");

        menu = TrayMenuFactory.createTrayMenu(this, systray.isSwing());

        boolean isMac = OSUtils.IS_MAC;

        // If we're running under Windows, we use a special icon without
        // background.
        if (OSUtils.IS_WINDOWS)
        {
            logoIcon = Resources.getImage("service.systray.TRAY_ICON_WINDOWS");
            logoIconOffline = Resources.getImage(
                "service.systray.TRAY_ICON_WINDOWS_OFFLINE");
            envelopeIcon = Resources.getImage(
                "service.systray.MESSAGE_ICON_WINDOWS");
        }
        /*
         * If we're running under Mac OS X, we use special black and white icons
         * without background.
         */
        else if (isMac)
        {
            logoIcon = Resources.getImage("service.systray.TRAY_ICON_MACOSX");
            logoIconWhite = Resources.getImage(
                "service.systray.TRAY_ICON_MACOSX_WHITE");
            envelopeIcon = Resources.getImage(
                "service.systray.MESSAGE_ICON_MACOSX");
            envelopeIconWhite = Resources.getImage(
                "service.systray.MESSAGE_ICON_MACOSX_WHITE");
        }
        else
        {
            logoIcon = Resources.getImage("service.systray.TRAY_ICON");
            logoIconOffline = Resources.getImage(
                "service.systray.TRAY_ICON_OFFLINE");
            envelopeIcon = Resources.getImage("service.systray.MESSAGE_ICON");
        }

        // Default to set offline , if any protocols become online will set it
        // to online.  Synchronize on the tray lock to ensure that changes to
        // the currentIcon happen consecutively.
        synchronized (trayLock)
        {
            if (currentIcon == null)
            {
                currentIcon = isMac ? logoIcon : logoIconOffline;
            }

            String appName = Resources.getApplicationString(
                                                "service.gui.APPLICATION_NAME");
            trayIcon = new TrayIcon(currentIcon.resolve(), appName, menu);
        }

        trayIcon.setIconAutoSize(true);

        //Show/hide the contact list when user clicks on the systray.
        trayIcon.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        UIService uiService
                            = OsDependentActivator.getUIService();
                        uiService.setVisible(true);
                    }
                });

        trayIcon.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (SwingUtilities.isLeftMouseButton(e))
                {
                    UIService uiService = OsDependentActivator.getUIService();
                    uiService.setVisible(true);
                }
            }
        });

        /*
         * Change the Mac OS X icon with the white one when the pop-up menu
         * appears.
         */
        if (isMac)
        {
            TrayMenuFactory.addPopupMenuListener(
                    menu,
                    new PopupMenuListener()
                    {
                        public void popupMenuWillBecomeVisible(PopupMenuEvent e)
                        {
                            ImageIconFuture newIcon
                                = (currentIcon == envelopeIcon)
                                    ? envelopeIconWhite
                                    : logoIconWhite;

                            synchronized (trayLock)
                            {
                                trayIcon.setIcon(newIcon.resolve());
                                currentIcon = newIcon;
                            }
                        }

                        public void popupMenuWillBecomeInvisible(
                                PopupMenuEvent e)
                        {
                            ImageIconFuture newIcon
                                = (currentIcon == envelopeIconWhite)
                                    ? envelopeIcon
                                    : logoIcon;

                            trayIcon.setIcon(newIcon.resolve());
                            currentIcon = newIcon;
                        }

                        public void popupMenuCanceled(PopupMenuEvent e)
                        {
                            popupMenuWillBecomeInvisible(e);
                        }
                    });
        }

        PopupMessageHandler pmh = null;

        if (!isMac)
        {
            pmh = new PopupMessageHandlerTrayIconImpl(trayIcon);
            popupHandlerSet.put(pmh.getClass().getName(), pmh);
            OsDependentActivator.bundleContext.registerService(
                    PopupMessageHandler.class.getName(),
                    pmh,
                    null);
        }
        try
        {
            OsDependentActivator.bundleContext.addServiceListener(
                    new ServiceListenerImpl(),
                    "(objectclass="
                        + PopupMessageHandler.class.getName()
                        + ")");
        }
        catch (Exception e)
        {
            logger.warn(e);
        }

        // now we look if some handler has been registered before we start
        // to listen
        ServiceReference<?>[] handlerRefs = null;

        try
        {
            handlerRefs
                = OsDependentActivator.bundleContext.getServiceReferences(
                        PopupMessageHandler.class.getName(),
                        null);
        }
        catch (InvalidSyntaxException ise)
        {
            logger.error("Error while retrieving service refs", ise);
        }
        if (handlerRefs != null)
        {
            String configuredHandler
                = (String) cfg.user().getProperty("systray.POPUP_HANDLER");

            for (ServiceReference<?> handlerRef : handlerRefs)
            {
                PopupMessageHandler handler
                    = (PopupMessageHandler)
                        OsDependentActivator.bundleContext.getService(
                                handlerRef);
                String handlerName = handler.getClass().getName();

                if (!popupHandlerSet.containsKey(handlerName))
                {
                    popupHandlerSet.put(handlerName, handler);
                    logger.info("added the following popup handler : "
                                + handler);
                    if ((configuredHandler != null)
                            && configuredHandler.equals(
                                    handler.getClass().getName()))
                    {
                        setActivePopupMessageHandler(handler);
                    }
                }
            }

            if (configuredHandler == null)
                selectBestPopupMessageHandler();
        }

        /*
         * Either we have an incorrect configuration value or the default pop-up
         * handler is not available yet. We will use the available pop-up
         * handler and will automatically switch to the configured one when it
         * becomes available. We will be aware of it since we listen for new
         * registered services in the BundleContext.
         */
        if ((activePopupHandler == null) && (pmh != null))
            setActivePopupMessageHandler(pmh);

        boolean showSystrayIcon = cfg.global()
            .getBoolean("plugin.wispa.SHOW_MAIN_UI", false);

        if (!isMac && showSystrayIcon)
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    logger.info("Showing systray icon");
                    systray.addTrayIcon(trayIcon);
                }
            });
        }

        logger.info("systray service initialized");
        initialized = true;
    }

    /**
     * Implements <tt>SystraService#showPopupMessage()</tt>
     *
     * @param popupMessage the message we will show
     */
    public void showPopupMessage(PopupMessage popupMessage)
    {
        // since popup handler could be loaded and unloader on the fly,
        // we have to check if we currently have a valid one.
        if (activePopupHandler != null)
        {
            logger.debug("Showing pop-up message");
            activePopupHandler.showPopupMessage(popupMessage);
        }
        else
        {
            logger.warn("Cannot show popup as no active popup handler");
        }
    }

    /**
     * Implements the <tt>SystrayService.addPopupMessageListener</tt> method.
     * If <tt>activePopupHandler</tt> is still not available record the listener
     * so we can add him later.
     *
     * @param listener the listener to add
     */
    public void addPopupMessageListener(SystrayPopupMessageListener listener)
    {
        if (activePopupHandler != null)
            activePopupHandler.addPopupMessageListener(listener);
        else
        {
            if(earlyAddedListeners == null)
                earlyAddedListeners =
                        new ArrayList<>();

            earlyAddedListeners.add(listener);
        }
    }

    /**
     * Implements the <tt>SystrayService.removePopupMessageListener</tt> method.
     *
     * @param listener the listener to remove
     */
    public void removePopupMessageListener(SystrayPopupMessageListener listener)
    {
        if (activePopupHandler != null)
            activePopupHandler.removePopupMessageListener(listener);
    }

    /**
     * Set the handler which will be used for popup message
     * @param newHandler the handler to set. providing a null handler is like
     * disabling popup.
     * @return the previously used popup handler
     */
    public PopupMessageHandler setActivePopupMessageHandler(
            PopupMessageHandler newHandler)
    {
        PopupMessageHandler oldHandler = activePopupHandler;

        if (oldHandler != null)
            oldHandler.removePopupMessageListener(popupMessageListener);
        if (newHandler != null)
            newHandler.addPopupMessageListener(popupMessageListener);

        logger.info("setting the following popup handler as active: "
                    + newHandler);
        activePopupHandler = newHandler;
        // if we have received calls to addPopupMessageListener before
        // the UIService is registered we should add those listeners
        if(earlyAddedListeners != null)
        {
            for(SystrayPopupMessageListener l : earlyAddedListeners)
                activePopupHandler.addPopupMessageListener(l);

            earlyAddedListeners.clear();
            earlyAddedListeners = null;
        }

        return oldHandler;
    }

    /**
     * Sets activePopupHandler to be the one with the highest preference index.
     */
    public void selectBestPopupMessageHandler()
    {
        PopupMessageHandler preferedHandler = null;
        int highestPrefIndex = 0;

        if (!popupHandlerSet.isEmpty())
        {
            Enumeration<String> keys = popupHandlerSet.keys();

            while (keys.hasMoreElements())
            {
                String handlerName = keys.nextElement();
                PopupMessageHandler h = popupHandlerSet.get(handlerName);

                if (h.getPreferenceIndex() > highestPrefIndex)
                {
                    highestPrefIndex = h.getPreferenceIndex();
                    preferedHandler = h;
                }
            }
            setActivePopupMessageHandler(preferedHandler);
        }
    }

    /** our listener for popup message click */
    private static class SystrayPopupMessageListenerImpl
        implements SystrayPopupMessageListener
    {
        /**
         * Handles a user click on a systray popup message. If the popup
         * notification was the result of an incoming message from a contact,
         * the chat window with that contact will be opened, if not already, and
         * brought to front.
         *
         * @param evt the event triggered when user clicks on a systray popup
         * message
         */
        public void popupMessageClicked(SystrayPopupMessageEvent evt)
        {
            final Object o = evt.getTag();

            if (o instanceof Contact || o instanceof String || o instanceof ChatRoom)
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        UIService uiService = OsDependentActivator.getUIService();
                        Chat chat = null;
                        if (o instanceof Contact)
                        {
                            chat = uiService.getChat((Contact) o);
                        }
                        else if (o instanceof String)
                        {
                            chat = uiService.getChat((String) o);
                        }
                        else if (o instanceof ChatRoom)
                        {
                            chat = uiService.getChat((ChatRoom) o, true, true);
                        }

                        if (chat != null)
                            chat.setChatVisible();
                    }
                });
            }
        }
    }

    /** An implementation of <tt>ServiceListener</tt> we will use */
    private class ServiceListenerImpl
        implements ServiceListener
    {
        public void serviceChanged(ServiceEvent serviceEvent)
        {
            try
            {
                PopupMessageHandler handler =
                    (PopupMessageHandler) OsDependentActivator.bundleContext.
                    getService(serviceEvent.getServiceReference());

                if (serviceEvent.getType() == ServiceEvent.REGISTERED)
                {
                    if (!popupHandlerSet.containsKey(
                        handler.getClass().getName()))
                    {
                        logger.info(
                            "adding the following popup handler : " + handler);
                        popupHandlerSet.put(
                            handler.getClass().getName(), handler);
                    }
                    else
                        logger.warn("the following popup handler has not " +
                            "been added since it is already known : " + handler);

                    String configuredHandler
                        = (String) cfg.user().getProperty("systray.POPUP_HANDLER");

                    if ((configuredHandler == null)
                            && ((activePopupHandler == null)
                                || (handler.getPreferenceIndex()
                                    > activePopupHandler.getPreferenceIndex())))
                    {
                        // The user doesn't have a preferred handler set and new
                        // handler with better preference index has arrived,
                        // thus setting it as active.
                        setActivePopupMessageHandler(handler);
                    }
                    if ((configuredHandler != null)
                            && configuredHandler.equals(
                                    handler.getClass().getName()))
                    {
                        // The user has a preferred handler set and it just
                        // became available, thus setting it as active
                        setActivePopupMessageHandler(handler);
                    }
                }
                else if (serviceEvent.getType() == ServiceEvent.UNREGISTERING)
                {
                    logger.info(
                        "removing the following popup handler : " + handler);
                    popupHandlerSet.remove(handler.getClass().getName());
                    if (activePopupHandler == handler)
                    {
                        activePopupHandler.removePopupMessageListener(
                            popupMessageListener);
                        activePopupHandler = null;

                        // We just lost our default handler, so we replace it
                        // with the one that has the highest preference index.
                        selectBestPopupMessageHandler();
                    }
                }
            }
            catch (IllegalStateException e)
            {
                logger.debug(e);
            }
        }
    }

    /**
     * Implements a <tt>ServiceListener</tt> which waits for an
     * <tt>UIService</tt> implementation to become available, invokes
     * {@link #initSystray()} and unregisters itself.
     */
    private class DelayedInitSystrayServiceListener
        implements ServiceListener
    {
        public void serviceChanged(ServiceEvent serviceEvent)
        {
            if (serviceEvent.getType() == ServiceEvent.REGISTERED)
            {
                Object service = OsDependentActivator.bundleContext.
                                 getService(serviceEvent.getServiceReference());

                if (service instanceof UIService)
                {
                    /*
                     * This ServiceListener has successfully waited for an
                     * UIService implementation to become available so it no
                     * longer need to listen.
                     */
                    OsDependentActivator.bundleContext.removeServiceListener(
                            this);

                    if (!initialized)
                        initSystray();
                }
            }
        }
    }

    public void uninitializeService()
    {
        // Disable the service by setting it as uninitialized
        initialized = false;
    }

    @Override
    public void updateTaskbarOverlay(int notificationCount)
    {
        // Do nothing
    }
}
