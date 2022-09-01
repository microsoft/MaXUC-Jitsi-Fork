// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui;

import java.awt.*;
import java.awt.desktop.QuitResponse;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.swing.*;

import com.drew.lang.annotations.Nullable;
import com.sun.jna.platform.WindowUtils;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;

import net.java.sip.communicator.impl.gui.event.PluginComponentEvent;
import net.java.sip.communicator.impl.gui.event.PluginComponentListener;
import net.java.sip.communicator.impl.gui.main.AbstractMainFrame;
import net.java.sip.communicator.impl.gui.main.account.NewAccountDialog;
import net.java.sip.communicator.impl.gui.main.chat.ChatWindowManager;
import net.java.sip.communicator.impl.gui.main.chat.conference.ConferenceChatManager;
import net.java.sip.communicator.impl.gui.main.chat.history.HistoryWindowManager;
import net.java.sip.communicator.impl.gui.main.configforms.ConfigurationFrame;
import net.java.sip.communicator.impl.gui.main.contactlist.AddContactDialog;
import net.java.sip.communicator.impl.gui.main.contactlist.TreeContactList;
import net.java.sip.communicator.impl.gui.main.login.DefaultSecurityAuthority;
import net.java.sip.communicator.impl.gui.main.login.LoginRendererSwingImpl;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.plugin.desktoputil.ComponentUtils;
import net.java.sip.communicator.plugin.desktoputil.ErrorDialog;
import net.java.sip.communicator.plugin.desktoputil.SIPCommSnakeButton;
import net.java.sip.communicator.plugin.desktoputil.UIAction;
import net.java.sip.communicator.plugin.desktoputil.WindowStacker;
import net.java.sip.communicator.plugin.desktoputil.lookandfeel.LookAndFeelManager;
import net.java.sip.communicator.plugin.desktoputil.lookandfeel.SIPCommLookAndFeel;
import net.java.sip.communicator.service.conference.ConferenceService;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactsource.SourceContact;
import net.java.sip.communicator.service.diagnostics.ReportReason;
import net.java.sip.communicator.service.gui.Chat;
import net.java.sip.communicator.service.gui.ConfigurationContainer;
import net.java.sip.communicator.service.gui.ContactListContainer;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.gui.CreateAccountWindow;
import net.java.sip.communicator.service.gui.ExportedWindow;
import net.java.sip.communicator.service.gui.PluginComponent;
import net.java.sip.communicator.service.gui.PopupDialog;
import net.java.sip.communicator.service.gui.UIContact;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.gui.UrlCreatorEnum;
import net.java.sip.communicator.service.gui.WindowID;
import net.java.sip.communicator.service.gui.WizardContainer;
import net.java.sip.communicator.service.gui.event.ChatListener;
import net.java.sip.communicator.service.protocol.Call;
import net.java.sip.communicator.service.protocol.ChatRoom;
import net.java.sip.communicator.service.protocol.ChatRoomMember;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.SecurityAuthority;
import net.java.sip.communicator.service.shutdown.ShutdownService;
import net.java.sip.communicator.service.wispaservice.WISPAAction;
import net.java.sip.communicator.service.wispaservice.WISPAMotion;
import net.java.sip.communicator.service.wispaservice.WISPANamespace;
import net.java.sip.communicator.service.wispaservice.WISPAService;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.account.AccountUtils;
import net.java.sip.communicator.util.account.LoginManager;
import net.java.sip.communicator.util.skin.Skinnable;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.util.OSUtils;

public abstract class AbstractUIServiceImpl implements UIService, PropertyChangeListener, ServiceListener, ShutdownService
{
    /**
     * The <tt>Logger</tt> used by the <tt>UIServiceImpl</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(AbstractUIServiceImpl.class);

    /**
     * The configuration service.
     */
    public static final ConfigurationService sConfigService =
            GuiActivator.getConfigurationService();

    /**
     * Name of the provisioning username in the configuration service.
     */
    private static final String PROPERTY_PROVISIONING_USERNAME
            = "net.java.sip.communicator.plugin.provisioning.auth.USERNAME";

    /**
     * Name of the provisioning password in the configuration service (HTTP
     * authentication).
     */
    private static final String PROPERTY_PROVISIONING_PASSWORD
            = "net.java.sip.communicator.plugin.provisioning.auth";

    /**
     * Name of the active user being used by configuration.
     */
    private static final String PROPERTY_ACTIVE_USER
            = "net.java.sip.communicator.plugin.provisioning.auth.ACTIVE_USER";

    /**
     * Suffix for several properties that shouldn't be left lying around after logout.
     */
    private static final String ENCRYPTED_PASSWORD = "ENCRYPTED_PASSWORD";
    private static final String CUSTOMSTATUS = "CUSTOM_STATUS";
    private static final String CHATSUBJECT = "chatRoomSubject";

    protected AbstractMainFrame mainFrame;

    protected LoginManager loginManager;

    protected ConfigurationFrame configurationFrame;

    protected static NewAccountDialog accountDialog = null;

    protected PopupDialogImpl popupDialog;

    /**
     * Used to track and stack the stackable alert windows that are currently
     * visible to the user.
     */
    protected WindowStacker windowStacker = new WindowStacker();

    /**
     * The <tt>PluginComponentListener</tt>s interested into when
     * <tt>PluginComponent</tt>s gets added and removed.
     * <p>
     * Because <tt>UIServiceImpl</tt> is global with respect to the lifetime of
     * the application and, consequently, <tt>PluginComponentListener</tt>s get
     * leaked, the listeners are referenced by <tt>WeakReference</tt>s.
     * </p>
     */
    protected final List<WeakReference<PluginComponentListener>>
        pluginComponentListeners
            = new ArrayList<>();

    protected static final Hashtable<WindowID, ExportedWindow> exportedWindows
        = new Hashtable<>();

    /**
     * Creates the main frame to be used.
     */
    public abstract void createMainFrame();

    /**
     * Implements UIService#useMacOSXScreenMenuBar(). Indicates that the Mac OS
     * X screen menu bar is to be used on Mac OS X and the Windows-like
     * per-window menu bars are to be used on non-Mac OS X operating systems.
     *
     * @return <tt>true</tt> to indicate that MacOSX screen menu bar should be
     * used, <tt>false</tt> - otherwise
     */
    @Override
    public boolean useMacOSXScreenMenuBar()
    {
        return (OSUtils.IS_MAC);
    }

    /**
     * Implements ShutdownService#beginShutdown(). Disposes of the mainFrame
     * (if it exists) and then instructs Felix to start shutting down the
     * bundles so that the application can gracefully quit.
     */
    @Override
    public void beginShutdown()
    {
        beginShutdown(false);
    }

    /**
     * As beginShutdown() but doesn't wait for the meeting client to close first.
     * This is designed to be called when the client is being killed by e.g. PC shutdown,
     * where we don't have the time to afford waiting.
     */
    @Override
    public void shutdownWithoutWaitingForMeeting()
    {
        beginShutdown(false, false, null, false);
    }

    /**
     * Implements ShutdownService#beginShutdown(). Disposes of the mainFrame
     * (if it exists) and then instructs Felix to start shutting down the
     * bundles so that the application can gracefully quit.
     *
     * @param logOut if true, log out the current user and restart showing an
     *               empty login dialog.  Otherwise, simply quit leaving the
     *               current user logged in.
     */
    @Override
    public void beginShutdown(final boolean logOut)
    {
        beginShutdown(logOut, false);
    }

    /**
     * Implements ShutdownService#beginShutdown(). Disposes of the mainFrame
     * (if it exists) and then instructs Felix to start shutting down the
     * bundles so that the application can gracefully quit.
     * @param logOut if true, log out the current user and restart showing an
     *               empty login dialog.  Otherwise, simply quit leaving the
     *               current user logged in.
     * @param electronTriggered if true, this shut down was triggered by a
     *                          request from the Electron UI.
     */
    @Override
    public void beginShutdown(final boolean logOut,
                              final boolean electronTriggered)
    {
        beginShutdown(logOut, electronTriggered, null, true);
    }

    /**
     * Implements ShutdownService#beginShutdown(). Disposes of the mainFrame
     * (if it exists) and then instructs Felix to start shutting down the
     * bundles so that the application can gracefully quit.
     *
     * @param logOut if true, log out the current user and restart showing an
     *               empty login dialog.  Otherwise, simply quit leaving the
     *               current user logged in.
     * @param electronTriggered if true, this shut down was triggered by a
     *                          request from the Electron UI.
     * @param macQuitResponse will be null unless the request to quit came from
     *                        the Mac OS.  In that case, we will call back to the
     *                        macQuitResponse once this method has finished executing.
     */
    @Override
    public void beginShutdown(final boolean logOut,
                              final boolean electronTriggered,
                              @Nullable final QuitResponse macQuitResponse,
                              boolean waitForMeetingToQuit)
    {
        WISPAService wispaService = GuiActivator.getWISPAService();

        // When we're shutting down, we don't want to restart Electron when we
        // lose connection
        if (wispaService != null)
        {
            wispaService.allowElectronRelaunch(false);
        }

        if (!SwingUtilities.isEventDispatchThread())
        {
            try
            {
                SwingUtilities.invokeAndWait(() -> beginShutdown(logOut, electronTriggered, macQuitResponse, waitForMeetingToQuit));
            }
            catch (Exception e)
            {
                logger.error("Failed to shutdown: ", e);
                if (wispaService != null)
                {
                    wispaService.allowElectronRelaunch(true);
                }
            }

            return;
        }

        // Note that it is possible that the client may need to shut down before
        // the ShutdownService is registered (e.g cancelling login or CoS check
        // failure on startup).  In that case the shutdownAll method in
        // ServiceUtils will be used.  It first tries to use the ShutdownService,
        // but if it isn't registered, it asks the Electron UI to shut down then
        // shuts down all of the OSGI bundles cleanly, one by one.  Unlike the
        // ShutdownService, it doesn't also ask the Meeting client to quit, as if
        // the ShutdownService isn't registered, neither will the ConferenceService
        // be registered.  However, if you are adding new actions that the
        // ShutdownService may perform, you MUST also consider whether they need
        // to run by ServiceUtils.shutdownAll in case of shutdown before the
        // ShutdownService is available and, if so, add them to that method.

        logger.info("Begin shutdown: Logout? " + logOut +
                    ", Triggered by Electron? " + electronTriggered +
                    ", Mac QuitResponse: " + macQuitResponse);

        //  We need the Java backend and the Electron UI to start up and shut
        //  down together, so when either app shuts down it must ask the
        //  other to do the same.  Therefore, unless we're shutting down now
        //  because Electron UI has asked us to, we now ask the Electron UI to
        //  shut down as well.  We do this before asking the Meeting client to
        //  quit, as sometimes it can take a few seconds for the Meeting client
        //  to shut down and we don't want the user to think their request to
        //  quit the app has been ignored if the UI doesn't close promptly.
        if (!electronTriggered)
        {
            if (wispaService != null)
            {
                wispaService.notify(WISPANamespace.CORE,
                                    WISPAAction.MOTION,
                                    WISPAMotion.SHUTDOWN);
            }
            else
            {
                logger.error("Cannot quit Electron - WISPAService is null.");
            }
        }

        // Check whether we need to quit the Meeting client before shutting
        // this client down.
        ConferenceService confService = GuiActivator.getConferenceService();
        if ((confService != null) && waitForMeetingToQuit)
        {
            logger.info("Quit meeting client and wait for it");
            confService.quitMeetingClient(
                new ConferenceService.MeetingClientQuitCallback()
                {
                    @Override
                    public void onMeetingClientQuitComplete(boolean cancelled)
                    {
                        logger.info("Meeting client quit complete.  Cancelled ? " + cancelled);

                        if (cancelled)
                        {
                            if (wispaService != null)
                            {
                                wispaService.allowElectronRelaunch(true);

                                // Ensure Electron is running when we cancel quitting Java
                                wispaService.checkAndRestartElectronUI();
                            }

                            if (macQuitResponse != null)
                            {
                                logger.info("About to call handleMacOSXQuitRequest with " + macQuitResponse);
                                handleMacOSXQuitRequest(macQuitResponse, true);
                            }
                        }
                        else
                        {
                            AbstractUIServiceImpl.this.onMeetingClientQuitComplete(logOut, macQuitResponse);
                        }
                    }
                }, logOut);
        }
        else
        {
            if (confService != null)
            {
                logger.info("Quit meeting client but don't wait for it");
                confService.quitMeetingClient(null, logOut);
            }

            onMeetingClientQuitComplete(logOut, macQuitResponse);
        }
    }

    /**
     * Called when we have finished executing the code that may quit the meeting client.
     *
     * @param logOut if true, log out the current user and restart showing an empty login dialog.
     * Otherwise, simply quit leaving the current user logged in.
     * @param macQuitResponse will be null unless the request to quit came from the Mac OS. In
     * that case, we will call back to the macQuitResponse once this method has finished executing.
     */
    private void onMeetingClientQuitComplete(boolean logOut, QuitResponse macQuitResponse)
    {
        logger.info("Meeting client quit - beginning shutdown.  Logout and restart? " + logOut);

        WISPAService wispaService = GuiActivator.getWISPAService();

        // When we're shutting down, we don't want to restart Electron when we
        // lose connection
        if (wispaService != null)
        {
            wispaService.allowElectronRelaunch(false);
        }

        if (logOut)
        {
            logger.debug("Logging out - remove active user");
            sConfigService.global().removeProperty(PROPERTY_ACTIVE_USER);
            sConfigService.global().removeProperty(PROPERTY_PROVISIONING_USERNAME);
            sConfigService.user().removeProperty(PROPERTY_PROVISIONING_PASSWORD);

            // Remove the PAT, encrypted passwords and PII.
            sConfigService.user().removePropertyBySuffix(ENCRYPTED_PASSWORD);
            sConfigService.user().removePropertyBySuffix(CUSTOMSTATUS);
            sConfigService.user().removePropertyBySuffix(CHATSUBJECT);

            // Flush the config so that the .bak file doesn't contain a copy of the now
            // deleted credentials.
            sConfigService.user().storePendingConfigurationNow(true);
        }

        // If the user has asked to log out, restart the client to load the login screen.  If not, simply shut down.
        // Note that not logging out doesn't mean we want to turn off restart - it could be marked to restart for other
        // reasons, e.g. changing CTD config.
        if (logOut)
        {
            GuiActivator.getResetService().setRestart(true);
        }
        quitMaXUCClient();

        // Note we don't expect this chunk of code to run, as the client will be killed in quitMaXUCClient()
        // just above.  It's left in in case it either does run somehow, or we need to reinstate it in the
        // future. If the request to quit came from the Mac OS, respond to that now.
        if (macQuitResponse != null)
        {
            logger.info("About to call handleMacOSXQuitRequest with " + macQuitResponse);
            handleMacOSXQuitRequest(macQuitResponse, false);
        }
    }

    /**
     * Actually shut down this client.
     */
    private void quitMaXUCClient()
    {
        String adVersion = System.getProperty("sip-communicator.version");
        logger.error("*******************************************************");
        logger.error("*           Shutting down Accession " + adVersion + "         *");
        logger.error("*******************************************************");

        try
        {
            // Disable the system tray service as it can cause a hang on
            // shutdown.
            GuiActivator.getSystrayService().uninitializeService();

            if (mainFrame != null)
            {
                if (!SwingUtilities.isEventDispatchThread())
                {
                    SwingUtilities.invokeLater(() -> mainFrame.dispose());
                }
                else
                {
                mainFrame.dispose();
                }
            }

            // Log off all currently registered providers before we shut down
            // to allow them to do any clean-up processing that may be required.
            Collection<ProtocolProviderService> registeredProviders =
                AccountUtils.getRegisteredProviders();

            for (ProtocolProviderService registeredProvider : registeredProviders)
            {
                logger.debug(
                    "Logging off " + registeredProvider.getProtocolDisplayName());
                loginManager.logoff(registeredProvider);
            }

            // Make sure any pending config writes are written immediately,
            // rather than waiting for the scheduled delay, as we're about to
            // kill the application.
            sConfigService.user().storePendingConfigurationNow(false);
            sConfigService.global().storePendingConfigurationNow(false);

            // If this is a mac application, then we need to kill it, rather
            // than shutting down normally - the normal approach causes a
            // crash report (see SFR 446860)
            if (OSUtils.isMac())
            {
                killApplication();
            }
        }
        finally
        {
            // Just exit. The shutdown hook in felix ensures that we stop
            // everything nicely.
            logger.info("Calling system exit now");
            System.exit(0);
        }
    }

    /**
     * Calls back to Mac OS to either to complete or cancel the shutdown.
     *
     * @param quitResponse the QuitResponse to call back to.
     * @param cancelled whether or not the quit is cancelled.
     */
    private void handleMacOSXQuitRequest(final QuitResponse quitResponse, boolean cancelled)
    {
        if (!cancelled)
        {
            logger.info("Continuing with Mac shut down");
            /*
             * Tell Mac OS X that it shouldn't terminate the
             * application. We've already initiated the quit and we'll
             * eventually complete it i.e. we'll honor the request of
             * Mac OS X to quit.
             *
             * (2011-06-10) Changed to true, we tell that quit is handled
             * as otherwise will stop OS from logout or shutdown and
             * a notification will be shown to user to inform about it.
             *
             * (2011-07-12) Wait before answering to the OS or we will
             * end too quickly. 15sec is the time our shutdown timer
             * waits before force the shutdown.
             */

            synchronized(this)
            {
                try
                {
                    wait(15000);
                }catch (InterruptedException ex){}
            }

            // Free the event dispatch thread before performing the
            // quit (System.exit), shutdown timer may also have started
            // the quit and is waiting to free the threads which
            // we may be blocking.
            new Thread(new Runnable()
            {
                public void run()
                {
                    logger.info("Actually performing quit.");
                    quitResponse.performQuit();
                }
            }).start();
        }
        else
        {
            logger.info("Cancelling Mac shut down");
            quitResponse.cancelQuit();
        }
    }

    /**
     * Kill the application in a horrible way.
     *
     * Ensures that the user doesn't see an error pop-up on shutdown
     */
    private void killApplication()
    {
        try
        {
            // Get the process id and execute a kill.  This prevents the
            // user from seeing a crash report.
            String id = ManagementFactory.getRuntimeMXBean()
                .getName().split("@")[0];
            logger.info("Killing application with id " + id);

            // If we are killing the app, we need to ensure we perform
            // the application reset now if the user requested it.
            // Normally this would be done by the Reset Service bundle
            // stopping, but this won't happen in this case.
            GuiActivator.getResetService().shutdown();

            new ProcessBuilder("kill", "-9", id).start();
        }
        catch (Exception e)
        {
            logger.error("Error killing application", e);
        }
    }

    @Override
    public Font getDefaultFont()
    {
        return Constants.FONT;
    }

    /**
     * Returns the create account window.
     *
     * @return the create account window
     */
    @Override
    public CreateAccountWindow getCreateAccountWindow()
    {
        accountDialog = new NewAccountDialog();
        return accountDialog;
    }

    /**
     * Creates the corresponding PluginComponentEvent and notifies all
     * <tt>ContainerPluginListener</tt>s that a plugin component is added or
     * removed from the container.
     *
     * @param pluginComponent the plugin component that is added to the
     *            container.
     * @param eventID one of the PLUGIN_COMPONENT_XXX static fields indicating
     *            the nature of the event.
     */
    protected void firePluginEvent(PluginComponent pluginComponent, int eventID)
    {
        PluginComponentEvent evt =
            new PluginComponentEvent(pluginComponent, eventID);

        logger.debug("Will dispatch the following plugin component event: "
            + evt);

        synchronized (pluginComponentListeners)
        {
            Iterator<WeakReference<PluginComponentListener>> i
                = pluginComponentListeners.iterator();

            while (i.hasNext())
            {
                PluginComponentListener l = i.next().get();

                if (l == null)
                    i.remove();
                else
                {
                    switch (evt.getEventID())
                    {
                    case PluginComponentEvent.PLUGIN_COMPONENT_ADDED:
                        l.pluginComponentAdded(evt);
                        break;
                    case PluginComponentEvent.PLUGIN_COMPONENT_REMOVED:
                        l.pluginComponentRemoved(evt);
                        break;
                    default:
                        logger.error("Unknown event type " + evt.getEventID());
                        break;
                    }
                }
            }
        }
    }

    /**
     * Implements <code>isVisible</code> in the UIService interface. Checks if
     * the main application window is visible.
     *
     * @return <code>true</code> if main application window is visible,
     *         <code>false</code> otherwise
     * @see UIService#isVisible()
     */
    @Override
    public boolean isVisible()
    {
        boolean isVisible = false;
        if (mainFrame != null)
        {
            isVisible = mainFrame.isFrameVisible();
        }
        return isVisible;
    }

    /**
     * Implements <code>setVisible</code> in the UIService interface. Shows or
     * hides the main application window depending on the parameter
     * <code>visible</code>.
     *
     * @param isVisible true if we are to show the main application frame and
     * false otherwise.
     *
     * @see UIService#setVisible(boolean)
     */
    @Override
    public void setVisible(final boolean isVisible)
    {
        logger.debug("setVisible " + isVisible);
        this.mainFrame.setFrameVisible(isVisible);
    }

    /**
     * Enable/disable the waiting cursor on the main frame.
     */
    @Override
    public void enableWaitingCursor(boolean enable)
    {
        logger.info("Enable waiting cursor " + enable);
        this.mainFrame.setCursor(enable ?
            Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) :
                Cursor.getDefaultCursor());
    }

    /**
     * Locates the main application window to the new x and y coordinates.
     *
     * @param x The new x coordinate.
     * @param y The new y coordinate.
     */
    @Override
    public void setLocation(int x, int y)
    {
        mainFrame.setLocation(x, y);
    }

    /**
     * Returns the current location of the main application window. The returned
     * point is the top left corner of the window.
     *
     * @return The top left corner coordinates of the main application window.
     */
    @Override
    public Point getLocation()
    {
        return mainFrame.getLocation();
    }

    /**
     * Returns the size of the main application window.
     *
     * @return the size of the main application window.
     */
    @Override
    public Dimension getSize()
    {
        return mainFrame.getSize();
    }

    /**
     * Sets the size of the main application window.
     *
     * @param width The width of the window.
     * @param height The height of the window.
     */
    @Override
    public void setSize(int width, int height)
    {
        mainFrame.setSize(width, height);
    }

    /**
     * Implements <code>minimize</code> in the UIService interface. Minimizes
     * the main application window.
     *
     * @see UIService#minimize()
     */
    @Override
    public void minimize()
    {
        this.mainFrame.minimize();
    }

    /**
     * Implements <code>maximize</code> in the UIService interface. Maximizes
     * the main application window.
     *
     * @see UIService#maximize()
     */
    @Override
    public void maximize()
    {
        this.mainFrame.maximize();
    }

    /**
     * Implements <code>restore</code> in the UIService interface. Restores
     * the main application window.
     *
     * @see UIService#restore()
     */
    @Override
    public void restore()
    {
        if (mainFrame.isFrameVisible())
        {
            if (mainFrame.getState() == JFrame.ICONIFIED)
                mainFrame.setState(JFrame.NORMAL);

            mainFrame.toFront();
        }
        else
            mainFrame.setFrameVisible(true);
    }

    /**
     * Implements <code>resize</code> in the UIService interface. Resizes the
     * main application window.
     *
     * @param height the new height of tha main application frame.
     * @param width the new width of the main application window.
     *
     * @see UIService#resize(int, int)
     */
    @Override
    public void resize(int width, int height)
    {
        this.mainFrame.setSize(width, height);
    }

    /**
     * Implements <code>move</code> in the UIService interface. Moves the main
     * application window to the point with coordinates - x, y.
     *
     * @param x the value of X where the main application frame is to be placed.
     * @param y the value of Y where the main application frame is to be placed.
     *
     * @see UIService#move(int, int)
     */
    @Override
    public void move(int x, int y)
    {
        this.mainFrame.setLocation(x, y);
    }

    /**
     * Brings the focus to the main application window.
     */
    @Override
    public void bringToFront()
    {
        if (mainFrame.getState() == Frame.ICONIFIED)
            mainFrame.setState(Frame.NORMAL);
        // Because toFront() method gives us no guarantee that our frame would
        // go on top we'll try to also first request the focus and set our
        // window always on top to put all the chances on our side.
        mainFrame.requestFocus();
        mainFrame.setAlwaysOnTop(true);
        mainFrame.toFront();
        mainFrame.setAlwaysOnTop(false);
    }

    /**
     * Implements {@link UIService#getExitOnMainWindowClose()}. Gets the boolean
     * property which indicates whether the application should be exited when
     * the main application window is closed.
     *
     * @return determines whether the UI impl would exit the application when
     * the main application window is closed.
     */
    @Override
    public boolean getExitOnMainWindowClose()
    {
        return
            (mainFrame != null)
                && (mainFrame.getDefaultCloseOperation()
                        == JFrame.DISPOSE_ON_CLOSE);
    }

    /**
     * Registers the given <tt>ExportedWindow</tt> to the list of windows that
     * could be accessed from other bundles.
     *
     * @param window the window to be exported
     */
    public void registerExportedWindow(ExportedWindow window)
    {
        exportedWindows.put(window.getIdentifier(), window);
    }

    /**
     * Unregisters the given <tt>ExportedWindow</tt> from the list of windows
     * that could be accessed from other bundles.
     *
     * @param window the window to no longer be exported
     */
    public void unregisterExportedWindow(ExportedWindow window)
    {
        WindowID identifier = window.getIdentifier();
        ExportedWindow removed = exportedWindows.remove(identifier);

        /*
         * In case the unexpected happens and we happen to have the same
         * WindowID for multiple ExportedWindows going through
         * #registerExportedWindow(), we have to make sure we're not
         * unregistering some other ExportedWindow which has overwritten the
         * registration of the specified window.
         */
        if ((removed != null) && !removed.equals(window))
        {
            /*
             * We accidentally unregistered another window so bring back its
             * registration.
             */
            exportedWindows.put(identifier, removed);

            /* Now unregister the right window. */
            exportedWindows.entrySet().removeIf(entry -> window.equals(entry.getValue()));
        }
    }

    /**
     * Adds the given <tt>PluginComponentListener</tt> to the list of component
     * listeners registered in this <tt>UIService</tt> implementation.
     *
     * @param listener the <tt>PluginComponentListener</tt> to add
     */
    public void addPluginComponentListener(PluginComponentListener listener)
    {
        synchronized (pluginComponentListeners)
        {
            Iterator<WeakReference<PluginComponentListener>> i
                = pluginComponentListeners.iterator();
            boolean contains = false;

            while (i.hasNext())
            {
                PluginComponentListener l = i.next().get();

                if (l == null)
                    i.remove();
                else if (l.equals(listener))
                    contains = true;
            }
            if (!contains)
                pluginComponentListeners.add(
                        new WeakReference<>(listener));
        }
    }

    /**
     * Removes the given <tt>PluginComponentListener</tt> from the list of
     * component listeners registered in this <tt>UIService</tt> implementation.
     *
     * @param listener the <tt>PluginComponentListener</tt> to remove
     */
    public void removePluginComponentListener(PluginComponentListener listener)
    {
        synchronized (pluginComponentListeners)
        {
            Iterator<WeakReference<PluginComponentListener>> i
                = pluginComponentListeners.iterator();

            while (i.hasNext())
            {
                PluginComponentListener l = i.next().get();

                if ((l == null) || l.equals(listener))
                    i.remove();
            }
        }
    }

    /**
     * Implements <code>getSupportedExportedWindows</code> in the UIService
     * interface. Returns an iterator over a set of all windows exported by
     * this implementation.
     *
     * @return an Iterator over all windows exported by this implementation of
     * the UI service.
     *
     * @see UIService#getSupportedExportedWindows()
     */
    @Override
    public Iterator<WindowID> getSupportedExportedWindows()
    {
        return Collections.unmodifiableMap(exportedWindows).keySet().iterator();
    }

    /**
     * Implements the <code>getExportedWindow</code> in the UIService interface.
     * Returns the window corresponding to the given <tt>WindowID</tt>.
     *
     * @param windowID the id of the window we'd like to retrieve.
     * @param params the params to be passed to the returned window.
     * @return a reference to the <tt>ExportedWindow</tt> instance corresponding
     *         to <tt>windowID</tt>.
     * @see UIService#getExportedWindow(WindowID)
     */
    @Override
    public ExportedWindow getExportedWindow(WindowID windowID, Object[] params)
    {
        ExportedWindow win = exportedWindows.get(windowID);

        if (win != null)
            win.setParams(params);

        return win;
    }

    /**
     * Implements the <code>getExportedWindow</code> in the UIService
     * interface. Returns the window corresponding to the given
     * <tt>WindowID</tt>.
     *
     * @param windowID the id of the window we'd like to retrieve.
     *
     * @return a reference to the <tt>ExportedWindow</tt> instance corresponding
     * to <tt>windowID</tt>.
     * @see UIService#getExportedWindow(WindowID)
     */
    @Override
    public ExportedWindow getExportedWindow(WindowID windowID)
    {
        return getExportedWindow(windowID, null);
    }

    /**
     * Implements the <code> hideExportedWindow</code> in the UIService
     * interface.
     *
     * @param windowID The ID of the exported window to hide.
     */
    @Override
    public void hideExportedWindow(WindowID windowID)
    {
        ExportedWindow win = getExportedWindow(windowID);

        if (win != null)
            win.bringToBack();
    }

    /**
     * Implements the <code>UIService.isExportedWindowSupported</code> method.
     * Checks if there's an exported component for the given
     * <tt>WindowID</tt>.
     *
     * @param windowID the id of the window that we're making the query for.
     *
     * @return true if a window with the corresponding windowID is exported by
     * the UI service implementation and false otherwise.
     *
     * @see UIService#isExportedWindowSupported(WindowID)
     */
    @Override
    public boolean isExportedWindowSupported(WindowID windowID)
    {
        return exportedWindows.containsKey(windowID);
    }

    /**
     * Implements <code>getPopupDialog</code> in the UIService interface.
     * Returns a <tt>PopupDialog</tt> that could be used to show simple
     * messages, warnings, errors, etc.
     *
     * @return a <tt>PopupDialog</tt> that could be used to show simple
     * messages, warnings, errors, etc.
     *
     * @see UIService#getPopupDialog()
     */
    @Override
    public PopupDialog getPopupDialog()
    {
        return this.popupDialog;
    }

    /**
     * Returns the LoginManager.
     * @return the LoginManager
     */
    @Override
    public LoginManager getLoginManager()
    {
        return loginManager;
    }

    /**
     * Returns the <tt>MainFrame</tt>. This is the class defining the main
     * application window.
     *
     * @return the <tt>MainFrame</tt>
     */
    public AbstractMainFrame getMainFrame()
    {
        return mainFrame;
    }

    /**
     * The <tt>RunLogin</tt> implements the Runnable interface and is used to
     * show the login windows in a separate thread.
     */
    protected class RunLoginGui implements Runnable {
        @Override
        public void run() {
            loginManager.runLogin();
        }
    }

    /**
     * Notifies all plugin containers of a <tt>PluginComponent</tt>
     * registration.
     * @param event the <tt>ServiceEvent</tt> that notified us
     */
    @Override
    public void serviceChanged(ServiceEvent event)
    {
        Object sService = GuiActivator.bundleContext.getService(
            event.getServiceReference());

        // we don't care if the source service is not a plugin component
        if (! (sService instanceof PluginComponent))
            return;

        PluginComponent pluginComponent = (PluginComponent) sService;

        switch (event.getType())
        {
        case ServiceEvent.REGISTERED:
            logger.info("Handling registration of a new Plugin Component.");

            /*
            // call getComponent for the first time when the component is
            // needed to be created and add to a container
            // skip early creating of components
            Object component = pluginComponent.getComponent();
            if (!(component instanceof Component))
            {
                logger.error("Plugin Component type is not supported."
                    + "Should provide a plugin in AWT, SWT or Swing.");
                if (logger.isDebugEnabled())
                    logger.debug("Logging exception to show the calling plugin",
                    new Exception(""));
                return;
            }*/

            this.firePluginEvent(pluginComponent,
                PluginComponentEvent.PLUGIN_COMPONENT_ADDED);
            break;

        case ServiceEvent.UNREGISTERING:
            this.firePluginEvent(pluginComponent,
                PluginComponentEvent.PLUGIN_COMPONENT_REMOVED);
            break;
        }
    }

    /**
     * Returns the corresponding <tt>BorderLayout</tt> constraint from the given
     * <tt>Container</tt> constraint.
     *
     * @param containerConstraints constraints defined in the <tt>Container</tt>
     * @return the corresponding <tt>BorderLayout</tt> constraint from the given
     * <tt>Container</tt> constraint.
     */
    public static Object getBorderLayoutConstraintsFromContainer(
        Object containerConstraints)
    {
        Object layoutConstraint = null;
        if (containerConstraints == null)
            return null;

        if (containerConstraints.equals(Container.START))
            layoutConstraint = BorderLayout.LINE_START;
        else if (containerConstraints.equals(Container.END))
            layoutConstraint = BorderLayout.LINE_END;
        else if (containerConstraints.equals(Container.TOP))
            layoutConstraint = BorderLayout.NORTH;
        else if (containerConstraints.equals(Container.BOTTOM))
            layoutConstraint = BorderLayout.SOUTH;
        else if (containerConstraints.equals(Container.LEFT))
            layoutConstraint = BorderLayout.WEST;
        else if (containerConstraints.equals(Container.RIGHT))
            layoutConstraint = BorderLayout.EAST;

        return layoutConstraint;
    }

    /**
     * Indicates that a <tt>PropertyChangeEvent</tt> has occurred.
     *
     * @param evt the <tt>PropertyChangeEvent</tt> that notified us
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        String propertyName = evt.getPropertyName();

        if (propertyName.equals(
            "impl.gui.IS_TRANSPARENT_WINDOW_ENABLED"))
        {
            String isTransparentString = (String) evt.getNewValue();

            boolean isTransparentWindowEnabled
                = Boolean.parseBoolean(isTransparentString);

            try
            {
                WindowUtils.setWindowTransparent(   mainFrame,
                    isTransparentWindowEnabled);
            }
            catch (UnsupportedOperationException ex)
            {
                logger.error(ex.getMessage(), ex);

                if (isTransparentWindowEnabled)
                {
                    ResourceManagementService resources
                        = GuiActivator.getResources();

                    new ErrorDialog(
                            mainFrame,
                            resources.getI18NString("service.gui.ERROR"),
                            resources.getI18NString(
                                    "service.gui.TRANSPARENCY_NOT_ENABLED"))
                        .showDialog();
                }

                ConfigurationUtils.setTransparentWindowEnabled(false);
            }
        }
        else if (propertyName.equals(
            "impl.gui.WINDOW_TRANSPARENCY"))
        {
            mainFrame.repaint();
        }
    }

    /**
     * Initialize main window font.
     */
    protected void initCustomFonts()
    {
        Font font = LookAndFeelManager.getScaledDefaultFont(Font.BOLD);
        logger.info("Setting MainFrame default font to " + font);
        mainFrame.setDefaultFont(font);
    }

    /**
     * Returns the <tt>ConfigurationContainer</tt> associated with this
     * <tt>UIService</tt>.
     *
     * @return the <tt>ConfigurationContainer</tt> associated with this
     * <tt>UIService</tt>
     */
    @Override
    public ConfigurationContainer getConfigurationContainer()
    {
        // If the configuration form doesn't exist then we create it.  Also
        // create it if this is a Mac and the existing frame has been hidden.
        // This is required to work around a bug where the video panel will
        // sometimes display on other frames.
        if (configurationFrame == null ||
            (OSUtils.IS_MAC && !configurationFrame.isVisible()))
        {
            if(!SwingUtilities.isEventDispatchThread())
            {
                logger.info("Ask EDT to open configuration frame");
                try
                {
                    // This has the potential to cause deadlocks if the current
                    // thread holds a lock that this code path (when continued
                    // on the EDT) will later try to obtain e.g. the AppKit lock (see SFR 540377).
                    //
                    // Ideally we would make this method private and make operations on the config window
                    // asynchronous, but that's a bit risky ATM.
                    SwingUtilities.invokeAndWait(this::getConfigurationContainer);
                }
                catch (Throwable e)
                {
                    logger.error("Error creating config frame in Swing thread");
                    // if still no frame create it outside event dispatch thread
                    if (configurationFrame == null)
                        configurationFrame = new ConfigurationFrame(mainFrame);
                }

                return configurationFrame;
            }

            logger.debug("Creating new config frame; do " +
                         (configurationFrame == null ? "not " : "") +
                         "have existing frame");
            configurationFrame = new ConfigurationFrame(mainFrame);
        }

        return configurationFrame;
    }

    /**
     * Repaints and revalidates the whole UI Tree.
     *
     * Calls {@link SwingUtilities#updateComponentTreeUI(Component c)}
     * for every window owned by the application which cause UI skin and
     * layout repaint.
     */
    @Override
    public void repaintUI()
    {
        logger.info("Repainting UI");

        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(this::repaintUI);
            return;
        }

        if (UIManager.getLookAndFeel() instanceof SIPCommLookAndFeel)
            ((SIPCommLookAndFeel) UIManager.getLookAndFeel()).loadSkin();

        Constants.reload();
        GuiActivator.getImageLoaderService().clearCache();

        Window[] windows
            = net.java.sip.communicator.plugin.desktoputil.WindowUtils
                .getWindows();

        for(Window win : windows)
        {
            reloadComponents(win);
            ComponentUtils.updateComponentTreeUI(win);
        }
    }

    /**
     * Reloads reload-able children of the given <tt>window</tt>.
     *
     * @param window the window, which components to reload
     */
    private void reloadComponents(Window window)
    {
        if (window instanceof Skinnable)
            ((Skinnable) window).loadSkin();

        reloadComponents((java.awt.Container) window);
    }

    /**
     * Reloads all children components of the given <tt>container</tt> in depth.
     *
     * @param container the container, which children to reload
     */
    private void reloadComponents(java.awt.Container container)
    {
        for (int i = 0; i < container.getComponentCount(); i++)
        {
            Component c = container.getComponent(i);

            if (c instanceof Skinnable)
                ((Skinnable) c).loadSkin();

            if (c instanceof JComponent)
            {
                JPopupMenu jpm = ((JComponent) c).getComponentPopupMenu();
                if(jpm != null && jpm.isVisible()
                        && jpm.getInvoker() == c)
                {
                    if (jpm instanceof Skinnable)
                        ((Skinnable) jpm).loadSkin();

                    reloadComponents(jpm);
                }
            }

            if (c instanceof JMenu)
            {
                Component[] children = ((JMenu)c).getMenuComponents();
                for (Component child : children)
                {
                    if (child instanceof Skinnable)
                        ((Skinnable) child).loadSkin();

                    if (child instanceof java.awt.Container)
                        reloadComponents((java.awt.Container) child);
                }
            }
            else if (c instanceof java.awt.Container)
                reloadComponents((java.awt.Container) c);
        }
    }

    /**
     * Alerts that an event has occurred (e.g. a message has been received or
     * a call has been missed) in the exported window specified by the
     * parameter, using a platform-dependent visual clue such as flashing it
     * in the task bar on Windows and Linux.
     *
     * @param windowID The ID of the exported window to alert.
     */
    @Override
    public void alertExportedWindow(WindowID windowID)
    {
        ExportedWindow win = getExportedWindow(windowID);
        if (win == null)
            return;

        alertWindow((Window) win.getSource());
    }

    /**
     * Alerts that an event has occurred (e.g. a message has been received or
     * a call has been missed) in the window specified by the parameter, using
     * a platform-dependent visual clue such as flashing it in the task bar on
     * Windows and Linux.
     *
     * @param window the window to alert.
     */
    @Override
    public void alertWindow(Window window)
    {
        if (!(window instanceof JFrame))
            return;

        JFrame fr = (JFrame) window;

        if(OSUtils.IS_MAC)
            Taskbar.getTaskbar()
                .requestUserAttention(true, false);
        else
        {
            // First check to see if the frame is in the system tray.
            // If the frame is not visible, and has a close operation of
            // HIDE, then it is either in the taskbar, or the system tray.
            // Task bar is represented by a state of ICONIFIED and the sys
            // tray (surprisingly) by NORMAL
            if (!fr.isVisible() &&
                fr.getDefaultCloseOperation() == JFrame.HIDE_ON_CLOSE &&
                fr.getState() == JFrame.NORMAL)
            {
                // Frame is in system tray, needs to be made visible
                if (fr == mainFrame)
                    setVisible(true);
                else
                    fr.setVisible(true);
            }

            if (!fr.isFocused())
            {
                flashWindow(fr, 1);
            }
        }
    }

    /**
     * Map containing the mapping between window being flashed and its respective TimerTask.
     */
    private final Map<JFrame, TimerTask> flashingFrames = new ConcurrentHashMap<>();

    /**
     * Since java awt does not have a direct way of flashing/blinking a window (i.e. without dll
     * or any native code support), mimic flashing effect behaviour by hide/show the frame
     * for given period in a Task that can be scheduled for repeated execution by a Timer.
     *
     * @param window JFrame which needs to be alerted / flashed
     * @param durationInSeconds amount of time in seconds flashing should occur
     */
    private void flashWindow(final JFrame window, final int durationInSeconds)
    {
        // The main frame is only displayed for test clients, so we don't care
        // about notifying the user.
        if (window == mainFrame)
        {
            return;
        }

        final Timer timer = new Timer("Flashing Window Timer");
        final int numberOfWindowFlashesPerSecond = 2;
        TimerTask newTask = new TimerTask()
        {
            private int remainingBlinks = durationInSeconds * numberOfWindowFlashesPerSecond;

            @Override
            public void run()
            {
                if (remainingBlinks > 0)
                {
                    remainingBlinks--;
                    // Toggle window visibility to mimic a flashing effect
                    window.setVisible(!window.isVisible());
                }
                else
                {
                    // Stop flashing the window
                    window.setVisible(true);
                    cancel();
                }
            }

            @Override
            public boolean cancel()
            {
                // Remove from the map and cancel the TimerTask.
                flashingFrames.remove(this);
                return super.cancel();
            }
        };
        TimerTask oldTask = flashingFrames.put(window, newTask);

        /*
         * While associating a TimerTask to a given window, stop already associated TimeTasks blinking
         * the same window (if any).
         */
        if (oldTask != null)
        {
            oldTask.cancel();
        }
        timer.schedule(newTask, 0, (int) (1000 * (0.5 / numberOfWindowFlashesPerSecond)));
    }

    @Override
    public int getProviderUIIndex(ProtocolProviderService pps)
    {
        return getMainFrame().getProviderIndex(pps);
    }

    /**
     * Creates a contact list component.
     *
     * @param clContainer the parent contact list container
     * @return the created <tt>ContactList</tt>
     */
    @Override
    public TreeContactList createContactListComponent(
        ContactListContainer clContainer)
    {
        return new TreeContactList(clContainer);
    }

    /**
     * Initializes all frames and panels and shows the GUI.
     */
    public void loadApplicationGui()
    {
        // Create the main frame first
        this.createMainFrame();

        if (UIManager.getLookAndFeel() instanceof SIPCommLookAndFeel)
            initCustomFonts();

        // Initialize main window bounds.
        this.mainFrame.initBounds();

        // Register the main window as an exported window, so that other bundles
        // could access it through the UIService.
        GuiActivator.getUIService().registerExportedWindow(mainFrame);

        // Initialize the login manager.
        this.loginManager
            = new LoginManager(new LoginRendererSwingImpl());

        this.popupDialog = new PopupDialogImpl();

        if (ConfigurationUtils.isTransparentWindowEnabled())
        {
            try
            {
                WindowUtils.setWindowTransparent(mainFrame, true);
            }
            catch (UnsupportedOperationException ex)
            {
                logger.error(ex.getMessage(), ex);
                ConfigurationUtils.setTransparentWindowEnabled(false);
            }
        }

        if(ConfigurationUtils.isApplicationVisible())
            mainFrame.setFrameVisible(true);

        SwingUtilities.invokeLater(new RunLoginGui());

        this.initExportedWindows();

        KeyboardFocusManager focusManager =
            KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.
            addKeyEventDispatcher(new KeyBindingsDispatching(focusManager));
    }

    /**
     * Adds all <tt>ExportedWindow</tt>s to the list of application windows,
     * which could be used from other bundles. Once registered in the
     * <tt>UIService</tt> this window could be obtained through the
     * <tt>getExportedWindow(WindowID)</tt> method and could be shown,
     * hidden, resized, moved, etc.
     */
    public void initExportedWindows()
    {
        registerExportedWindow(new AddContactDialog(mainFrame));
    }

    /**
     * Returns a default implementation of the <tt>SecurityAuthority</tt>
     * interface that can be used by non-UI components that would like to launch
     * the registration process for a protocol provider. Initially this method
     * was meant for use by the systray bundle and the protocol URI handlers.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> for which
     * the authentication window is about.
     *
     * @return a default implementation of the <tt>SecurityAuthority</tt>
     * interface that can be used by non-UI components that would like to launch
     * the registration process for a protocol provider.
     */
    @Override
    public SecurityAuthority getDefaultSecurityAuthority(
                    ProtocolProviderService protocolProvider)
    {
        SecurityAuthority secAuthority = GuiActivator.getSecurityAuthority(
            protocolProvider.getProtocolName());

        if (secAuthority == null)
            secAuthority = GuiActivator.getSecurityAuthority();

        if (secAuthority == null)
            secAuthority = new DefaultSecurityAuthority(protocolProvider);

        return secAuthority;
    }

    /**
     * Adds the given <tt>WindowListener</tt> to the main application window.
     * @param l the <tt>WindowListener</tt> to add
     */
    @Override
    public void addWindowListener(WindowListener l)
    {
        if (mainFrame != null)
        {
            mainFrame.addWindowListener(l);
        }
    }

    /**
     * Removes the given <tt>WindowListener</tt> from the main application
     * window.
     * @param l the <tt>WindowListener</tt> to remove
     */
    @Override
    public void removeWindowListener(WindowListener l)
    {
        mainFrame.removeWindowListener(l);
    }

    @Override
    public void showStackableAlertWindow(final Window window)
    {
        if (window.isVisible())
            return;

        // Set the visibility before adding the window, which will set its location. This is the
        // only reliable way to ensure that the location is respected.
        //
        // We also need a workaround for a problem where windows steal focus from the user when they
        // appear.  We might need the window to be focusable for accessibility reasons (e.g. so the
        // user can tab to it and interact with the keyboard), but the only way to stop such a
        // window from stealing focus seems to be to only make it focusable after it's made visible.
        boolean focusable = window.getFocusableWindowState();

        if (focusable)
        {
            window.setFocusableWindowState(false);
        }
        window.setVisible(true);

        if (focusable)
        {
            window.setFocusableWindowState(true);
        }

        // Alert windows are displayed centered unless they need to be stacked.
        // Set the location now, which will be overridden by the windowStacker
        // if necessary.
        window.pack();
        window.setLocationRelativeTo(null);

        windowStacker.addWindow(window);
    }

    /**
     * Dispatcher which ensures that our custom keybindings will
     * be executed before any other focused(or not focused) component
     * will consume our key event. This way we override some components
     * keybindings.
     */
    private static class KeyBindingsDispatching
        implements KeyEventDispatcher
    {
        private final KeyboardFocusManager focusManager;

        KeyBindingsDispatching(KeyboardFocusManager focusManager)
        {
            this.focusManager = focusManager;
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent e)
        {
            if(e.getID() == KeyEvent.KEY_PRESSED)
            {
                Window w = focusManager.getActiveWindow();
                JRootPane rpane = null;

                if(w instanceof JFrame)
                    rpane = ((JFrame)w).getRootPane();

                if(w instanceof JDialog)
                    rpane = ((JDialog)w).getRootPane();

                if(rpane == null)
                    return false;

                Object binding = rpane.
                    getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).
                        get(KeyStroke.getKeyStrokeForEvent(e));

                if(binding == null)
                    return false;

                Object actObj = rpane.getActionMap().get(binding);

                if(actObj instanceof UIAction)
                {
                    ((UIAction)actObj).actionPerformed(
                        new ActionEvent(w, -1, (String)binding));
                    return true;
                }
            }

            return false;
        }
    }

    // Provide empty implementations of the chat and call related methods, since
    // not all concrete UIServices implement chat and calls

    // These first 2 methods are only in this class, not in the interface
    public ConferenceChatManager getConferenceChatManager()
    {
        return null;
    }

    public ChatWindowManager getChatWindowManager()
    {
        return null;
    }

    // The rest of the methods are defined in the interface
    @Override
    public Chat getChat(String smsNumber)
    {
        return null;
    }

    @Override
    public Chat getChat(Contact contact)
    {
        return null;
    }

    @Override
    public Chat getChat(Contact contact, String smsNumber)
    {
        return null;
    }

    @Override
    public Chat getChat(ChatRoom chatRoom, boolean create,
                        boolean requestHistory)
    {
        return null;
    }

    @Override
    public MetaContact getChatContact(Chat chat)
    {
        return null;
    }

    @Override
    public Chat getCurrentChat()
    {
        return null;
    }

    @Override
    public String getCurrentPhoneNumber()
    {
        return null;
    }

    @Override
    public void setCurrentPhoneNumber(String phoneNumber)
    {
    }

    @Override
    public WizardContainer getAccountRegWizardContainer()
    {
        return null;
    }

    @Override
    public Iterator<Container> getSupportedContainers()
    {
        return null;
    }

    @Override
    public boolean isContainerSupported(Container containerID)
    {
        return false;
    }

    @Override
    public void addChatListener(ChatListener listener)
    {
    }

    @Override
    public void removeChatListener(ChatListener listener)
    {
    }

    @Override
    public void createCall(Reformatting reformatNeeded, String... participants)
    {
    }

    @Override
    public void hangupCall(Call call)
    {
    }

    @Override
    public void startChat(String[] participants)
    {
    }

    @Override
    public void startGroupChat(String chatRoomUid, boolean isClosed,
                               String chatRoomSubject)
    {
    }

    @Override
    public Collection<Call> getInProgressCalls()
    {
        return new ArrayList<>();
    }

    @Override
    public Collection<Call> getInProgressVideoCalls()
    {
        return new ArrayList<>();
    }

    @Override
    public void updateVideoButtonsEnabledState()
    {
    }

    @Override
    public void reloadContactList()
    {
    }

    @Override
    public JDialog createViewGroupContactDialog(Contact groupContact)
    {
        return null;
    }

    @Override
    public JDialog createEditGroupContactDialog(Contact groupContact,
                                                String defaultDisplayName, Set<MetaContact> preSelectedContacts,
                                                boolean showViewOnCancel)
    {
        return null;
    }

    @Override
    public JDialog createContactWithNumberSelectDialog(Window parent,
                                                       String title, Consumer<UIContact> callback)
    {
        return null;
    }

    @Override
    public void showNewGroupChatWindow()
    {
    }

    @Override
    public void showUpdateGroupChatSubjectWindow(ChatRoom chatRoom)
    {
    }

    @Override
    public void showLeaveDialog(ChatRoom chatRoom)
    {
    }

    @Override
    public void showRemoveDialog(MetaContact metaContact, ChatRoomMember member)
    {
    }

    @Override
    public void showAddChatParticipantsDialog(ChatRoom chatRoom)
    {
    }

    @Override
    public SIPCommSnakeButton getAccessionCrmLaunchButton(String searchName,
                                                          String searchNumber)
    {
        return null;
    }

    @Override
    public SIPCommSnakeButton getWebSocketCrmLaunchButton(
            @Nullable String searchNumber)
    {
        return null;
    }

    @Override
    public JLabel getCrmLookupInProgressIndicator()
    {
        return null;
    }

    @Override
    public JLabel getCrmLookupFailedIndicator()
    {
        return null;
    }

    @Override
    public boolean isCrmAutoLaunchAlways()
    {
        return false;
    }

    @Override
    public boolean isCrmAutoLaunchExternal()
    {
        return false;
    }

    @Override
    public HistoryWindowManager getHistoryWindowManager()
    {
        return null;
    }

    @Override
    public void showChatHistory(Object descriptor)
    {
    }

    @Override
    public void showGroupChatHistory(ChatRoom room)
    {
    }

    @Override
    public void showAddUserWindow(SourceContact contact)
    {
    }

    @Override
    public void showEditUserWindow(MetaContact metaContact)
    {
    }

    @Override
    public void showCallParkWindow()
    {
    }

    @Override
    public void showBrowserService(UrlCreatorEnum mOption)
    {
    }

    @Override
    public void showBugReport()
    {
        logger.info("Open the bug report window.");
        GuiActivator.getDiagsService().openErrorReportFrame(ReportReason.USER);
    }
}