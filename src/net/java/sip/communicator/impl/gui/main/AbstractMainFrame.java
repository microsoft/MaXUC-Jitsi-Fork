// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.util.OSUtils;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.impl.gui.customcontrols.MessageDialog;
import net.java.sip.communicator.impl.gui.main.contactlist.ContactListPane;
import net.java.sip.communicator.impl.gui.main.menus.MainMenu;
import net.java.sip.communicator.impl.gui.main.presence.AccountStatusPanel;
import net.java.sip.communicator.plugin.desktoputil.SIPCommFrame;
import net.java.sip.communicator.plugin.desktoputil.lookandfeel.LookAndFeelManager;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.gui.ExportedWindow;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.gui.WindowID;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.OperationSetMultiUserChat;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.ServiceUtils;

public abstract class AbstractMainFrame extends SIPCommFrame implements ExportedWindow
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The delay after the last resize or drag before we save size and location
     * to config. We delay doing this so as to avoid saving the values every
     * time we receive a drag or resize event.
     */
    protected static final int SAVE_SIZE_AND_LOCATION_DELAY_MS = 500;

    /**
     * The word 'Close', or the translated equivalent.
     */
    protected static final String CLOSE =
                 GuiActivator.getResources().getI18NString("service.gui.CLOSE");

    /**
     * The message shown to inform the user that closing the main window will
     * only hide it, rather than quitting the application.
     */
    protected static final String HIDE_MAIN_WINDOW =
        GuiActivator.getResources().getI18NString(
                                                "service.gui.HIDE_MAIN_WINDOW");

    /**
     * The logger.
     */
    private final Logger logger = Logger.getLogger(AbstractMainFrame.class);

    /**
     * The main menu.
     */
    protected MainMenu menu;

    /**
     * The keyboard focus manager.
     */
    protected KeyboardFocusManager keyManager;

    /**
     * The timer which schedules when next to save size and location to config.
     */
    protected ResettableTimer saveSizeAndLocationTimer;

    /**
     * The dialog that warns the user that the app is only hidden, rather than
     * closed, when the main frame is closed.
     */
    protected MessageDialog windowHiddenDialog = null;

    /**
     * The configuration service
     */
    protected static final ConfigurationService configService =
                                         GuiActivator.getConfigurationService();

    /**
     * Returns the panel containing the ContactList.
     * @return ContactListPanel the panel containing the ContactList
     */
    public abstract ContactListPane getContactListPanel();

    /**
     * Enters or exits the "unknown contact" view. This view will propose to
     * the user some specific operations if the current filter doesn't match
     * any contacts.
     *
     * This view will vary depending on the currently active filter.
     *
     * @param isEnabled <tt>true</tt> to enable the "unknown contact" view,
     * <tt>false</tt> - otherwise.
     */
    public abstract void enableUnknownContactView(boolean isEnabled);

    /**
     * Select the tab with the given tab name
     *
     * @param tabName the name of the tab to select
     */
    public abstract void selectTab(String tabName);

    /**
     * Select the sub-tab with the given tab name
     *
     * @param tabName the name of the sub-tab to select
     */
    public abstract void selectSubTab(String tabName);

    /**
     * Sets or unsets the notification icon that indicates whether there are
     * new events in this sub tab that the user has not acknowledged.
     *
     * @param tabName the name of the sub tab to set or clear the notification
     * icon on.
     * @param notificationsActive if true, there are active notifications on
     * this tab so the icon should be visible.
     */
    public abstract void setSubTabNotification(String tabName, boolean notificationsActive);

    /**
     * Adds a protocol provider.
     * @param protocolProvider The protocol provider to add.
     */
    public abstract void addProtocolProvider(ProtocolProviderService protocolProvider);

    /**
     * Removes a protocol provider.
     *
     * @param protocolProvider The protocol provider of the account.
     */
    public abstract void removeProtocolProvider(ProtocolProviderService protocolProvider);

    /**
     * Removes a protocol provider of a given account.
     *
     * @param accountID The unique account ID.
     */
    public abstract void removeProtocolProvider(AccountID accountID);

    /**
     * Returns the account status panel.
     * @return the account status panel.
     */
    public abstract AccountStatusPanel getAccountStatusPanel();

    /**
     * Returns the multi user chat operation set for the given protocol provider.
     *
     * @param protocolProvider The protocol provider for which the multi user
     * chat operation set is about.
     * @return OperationSetMultiUserChat The telephony operation
     * set for the given protocol provider.
     */
    public abstract OperationSetMultiUserChat getMultiUserChatOpSet(
        ProtocolProviderService protocolProvider);

    /**
     * Checks whether we have already loaded the protocol provider.
     * @param protocolProvider the provider to check.
     * @return whether we have already loaded the specified provider.
     */
    public abstract boolean hasProtocolProvider(
        ProtocolProviderService protocolProvider);

    /**
     * Initializes the contact list panel.
     *
     * @param contactListService The <tt>MetaContactListService</tt> containing
     * the contact list data.
     */
    public abstract void setContactList(MetaContactListService contactListService);

    /**
     * Returns the index of the given protocol provider.
     * @param protocolProvider the protocol provider to search for
     * @return the index of the given protocol provider
     */
    public abstract int getProviderIndex(ProtocolProviderService protocolProvider);

    /**
     * Adds all native plugins to this container.
     */
    public abstract void addNativePlugins();

    /**
     * Gets the frame's minimum bounds size
     * @return the frame's minimum bounds size
     */
    protected abstract Dimension getMinimumBoundsSize();

    /**
     * Gets the frame's preferred bounds size
     * @return the frame's preferred bounds size
     */
    protected abstract Dimension getPreferredBoundsSize();

    /**
     * Sets the frame's default font.
     * @param font is the Font to set as the frame's default.
     */
    public abstract void setDefaultFont(Font font);

    /**
     * @param bgStartOverrideColor if not null, this Color will be used as the
     * start Color for this dialog, instead of the Color saved in config.
     * @param bgEndOverrideColor if not null, this Color will be used as the
     * end Color for this dialog, instead of the Color saved in config.
     * @param useBgOverrideColor whether to use the SIP Comm Frame override
     * colour or not. If not, the native background colour is used.
     */
    public AbstractMainFrame(
        Color bgStartOverrideColor,
        Color bgEndOverrideColor,
        boolean useBgOverrideColor)
    {
        super(bgStartOverrideColor, bgEndOverrideColor, useBgOverrideColor);

        if (!ConfigurationUtils.isWindowDecorated())
        {
            this.setUndecorated(true);
        }

        menu = new MainMenu(this);

        this.initTitleFont();

        ResourceManagementService resources = GuiActivator.getResources();
        String applicationName
            = resources.getSettingsString("service.gui.APPLICATION_NAME");

        this.setTitle(applicationName);

        // sets the title to application name
        // fix for some window managers(gnome3)
        try
        {
            Toolkit xToolkit = Toolkit.getDefaultToolkit();
            java.lang.reflect.Field awtAppClassNameField =
            xToolkit.getClass().getDeclaredField("awtAppClassName");
            awtAppClassNameField.setAccessible(true);
            awtAppClassNameField.set(xToolkit, applicationName);
        }
        catch(Throwable t)
        {
            logger.error("Unable to set the application name: " + t);
        }

        keyManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();

        Runnable saveSizeAndLocationTask = new Runnable()
        {
            public void run()
            {
                SIPCommFrame.saveSizeAndLocation(
                    AbstractMainFrame.this, this.getClass().getName());
            }
        };

        saveSizeAndLocationTimer = new ResettableTimer(saveSizeAndLocationTask,
                                               SAVE_SIZE_AND_LOCATION_DELAY_MS);

        /*
         * If the application is configured to quit when this frame is closed,
         * do so.
         */
        this.addWindowListener(new WindowAdapter()
        {
            /**
             * Invoked when a window has been closed.
             */
            public void windowClosed(WindowEvent event)
            {
                AbstractMainFrame.this.windowClosed(event);
            }

            /**
             * Invoked when a window has been opened.
             */
            public void windowOpened(WindowEvent e)
            {

            }
        });

        /*
         * Ensure that we update the stored config each time the window is
         * moved or resized. There are times where we reset the window to the
         * location stored in the config so it must stay up to date.
         *
         */
        this.addComponentListener(new ComponentAdapter()
        {
            public void componentResized(ComponentEvent e)
            {
                saveSizeAndLocationTimer.reset();
            }

            public void componentMoved(ComponentEvent e)
            {
                saveSizeAndLocationTimer.reset();
            }
        });

        this.setJMenuBar(menu);
    }

    public AbstractMainFrame()
    {
        this(null, null, true);
    }

    /**
     * Sets frame size and position.
     */
    public void initBounds()
    {
        Dimension minSize = getMinimumBoundsSize();
        Dimension preferredSize = getPreferredBoundsSize();

        // Different operating systems border windows with differing
        // thicknesses. Therefore set the size of the content pane rather than
        // the window.
        getContentPane().setMinimumSize(minSize);
        getContentPane().setPreferredSize(preferredSize);
        pack();

        // Set the minimum size of the window.
        int borderWidth = getWidth() - preferredSize.width;
        int borderHeight = getHeight() - preferredSize.height;
        setMinimumSize(new Dimension(minSize.width + borderWidth,
                                     minSize.height + borderHeight));
        setSizeAndLocation();
    }

    /**
     * Returns the main menu in the application window.
     * @return the main menu in the application window
     */
    public MainMenu getMainMenu()
    {
        return menu;
    }

    /**
     * Brings this window to front.
     */
    public void bringToFront()
    {
        logger.debug("bringToFront");
        this.toFront();
    }

    @Override
    public void bringToBack()
    {
        logger.debug("bringToBack");
        toBack();
    }

    /**
     * Returns the identifier of this window.
     * @return the identifier of this window
     */
    public WindowID getIdentifier()
    {
        return ExportedWindow.MAIN_WINDOW;
    }

    /**
     * Returns this window.
     * @return this window
     */
    public Object getSource()
    {
        return this;
    }

    /**
     * Maximizes this window.
     */
    public void maximize()
    {
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    /**
     * Minimizes this window.
     */
    public void minimize()
    {
        this.setExtendedState(JFrame.ICONIFIED);
    }

    /**
     * Implements <code>isVisible</code> in the UIService interface. Checks if
     * the main application window is visible.
     *
     * @return <code>true</code> if main application window is visible,
     *         <code>false</code> otherwise
     * @see UIService#isVisible()
     */
    public boolean isFrameVisible()
    {
        return super.isVisible()
            && (super.getExtendedState() != JFrame.ICONIFIED);
    }

    /**
     * Implements <code>setVisible</code> in the UIService interface. Shows or
     * hides the main application window depending on the parameter
     * <code>makeVisible</code> and the config.
     *
     * @param makeVisible true to request to show the main application and false
     * to hide. A request to show the frame might get ignored, depending on the
     * config.
     *
     * @see UIService#setVisible(boolean)
     */
    public void setFrameVisible(final boolean makeVisible)
    {
        logger.debug("Request to set main frame visibility to: " + makeVisible);

        boolean showMainFrameConfig = configService.global()
            .getBoolean("plugin.wispa.SHOW_MAIN_UI", false);

        logger.debug("Config allows showing main frame:" + showMainFrameConfig);

        boolean isVisible = makeVisible && showMainFrameConfig;

        ConfigurationUtils.setApplicationVisible(isVisible);

        SwingUtilities.invokeLater(new Runnable(){
            public void run()
            {
                if(isVisible)
                {
                    AbstractMainFrame.this.addNativePlugins();

                    Window focusedWindow = keyManager.getFocusedWindow();

                    // If there's another currently focused window we prevent
                    // this frame from stealing the focus. This happens for
                    // example in the case of a Master Password window which is
                    // opened before the contact list window.
                    if (focusedWindow != null)
                        setFocusableWindowState(false);

                    AbstractMainFrame.super.setVisible(isVisible);

                    if (focusedWindow != null)
                        setFocusableWindowState(true);

                    AbstractMainFrame.super.setExtendedState(MainFrame.NORMAL);
                    AbstractMainFrame.super.toFront();
                }
                else
                {
                    AbstractMainFrame.super.setVisible(isVisible);
                }
            }
        });
    }

    /**
     * @param event Currently not used
     */
    protected void windowClosed(WindowEvent event)
    {
        if(GuiActivator.getUIService().getExitOnMainWindowClose())
        {
            logger.info("Shutting down on Window Closed");
            ServiceUtils.shutdownAll(GuiActivator.bundleContext);
        }
    }

    /**
     * Overrides SIPCommFrame#windowClosing(WindowEvent). Reflects the closed
     * state of this MainFrame in the configuration in order to make it
     * accessible to interested parties, displays the warning that the
     * application will not quit.
     * @param event the <tt>WindowEvent</tt> that notified us
     */
    protected void windowClosing(WindowEvent event)
    {
        super.windowClosing(event);

        // On Mac systems the application is not quited on window close, so we
        // don't need to warn the user.
        if (!GuiActivator.getUIService().getExitOnMainWindowClose()
            && !OSUtils.IS_MAC)
        {
            if (ConfigurationUtils.isQuitWarningShown())
            {
                showWindowHiddenDialog();
            }

            ConfigurationUtils.setApplicationVisible(false);
        }
    }

    /**
     * Display the dialog which warns the user that the app is hiding but not
     * closing, and if they choose to not show the dialog in future, store the
     * decision in config.
     */
    private void showWindowHiddenDialog()
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    showWindowHiddenDialog();
                }
            });
        }

        if (windowHiddenDialog == null)
        {
            windowHiddenDialog =
                         new MessageDialog(null, CLOSE, HIDE_MAIN_WINDOW, false)
            {
                private static final long serialVersionUID = 0L;

                public void actionPerformed(ActionEvent evt)
                {
                    super.actionPerformed(evt);
                    if (returnCode == MessageDialog.OK_DONT_ASK_CODE)
                        ConfigurationUtils.setQuitWarningShown(false);
                }
            };

            windowHiddenDialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        }

        windowHiddenDialog.setVisible(true);
    }

    /**
     * Initialize main window font.
     */
    private void initTitleFont()
    {
        JComponent layeredPane = this.getLayeredPane();
        Font font = LookAndFeelManager.getScaledDefaultFont(Font.BOLD);
        logger.info("Setting title font to " + font);

        for (int i = 0, componentCount = layeredPane.getComponentCount();
                i < componentCount;
                i++)
            layeredPane.getComponent(i).setFont(font);
    }

    /**
     * Implementation of Timer which maintains a single task with a fixed delay,
     * and can be kicked to reset the delay countdown.
     */
    private static class ResettableTimer extends Timer
    {
        /**
         * The task to be executed whenever the delay interval has passed
         * without a reset() call.
         */
        private final Runnable task;

        /**
         * The delay between the task being scheduled and the task being
         * executed (assuming reset() is not re-called during that time).
         */
        private final long delay;

        /**
         * The <tt>TimerTask</tt> tracking when next to execute 'task'.
         */
        private TimerTask timerTask;

        /**
         * Construct a ResettableTimer that executes the given task after
         * the specified delay. The timer does not start until 'reset' is
         * called.
         *
         * @param task The task which this timer will execute.
         * @param delay The delay for which 'task' will be scheduled after
         * calls to reset(), in ms.
         */
        public ResettableTimer(Runnable task, long delay)
        {
            this.task = task;
            this.delay = delay;
        }

        /**
         * Reschedule the task to be executed after 'delay' milliseconds.
         */
        public void reset()
        {
            // Abort any pending task.
            if (timerTask != null)
                timerTask.cancel();

            // Reschedule the task for 'delay' ms.
            timerTask = new TimerTask()
            {
                public void run()
                {
                    task.run();
                }
            };

            schedule(timerTask, delay);
        }
    }

    /**
     * Implementation of {@link ExportedWindow#setParams(Object[])}.
     */
    public void setParams(Object[] windowParams) {}
}
