// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.util.OSUtils;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.impl.gui.main.contactlist.ContactListPane;
import net.java.sip.communicator.plugin.desktoputil.SIPCommFrame;
import net.java.sip.communicator.service.gui.ExportedWindow;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.gui.WindowID;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.OperationSetMultiUserChat;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.ServiceUtils;
import net.java.sip.communicator.impl.gui.main.callpark.CallParkAvailabilityObserver;

public abstract class AbstractMainFrame extends SIPCommFrame implements ExportedWindow
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

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
    public void enableUnknownContactView(boolean isEnabled) {};

    /**
     * Select the tab with the given tab name
     *
     * @param tabName the name of the tab to select
     */
    public void selectTab(String tabName) {};

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
     * Returns the index of the given protocol provider.
     * @param protocolProvider the protocol provider to search for
     * @return the index of the given protocol provider
     */
    public abstract int getProviderIndex(ProtocolProviderService protocolProvider);

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

        new CallParkAvailabilityObserver().observeCallParkAvailability();

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
        logger.debug("Request to set main frame visibility to: " + makeVisible + " (ignored)");

        ConfigurationUtils.setApplicationVisible(false);

        SwingUtilities
            .invokeLater(() -> AbstractMainFrame.super.setVisible(false));
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
            ConfigurationUtils.setApplicationVisible(false);
        }
    }

    /**
     * Implementation of {@link ExportedWindow#setParams(Object[])}.
     */
    public void setParams(Object[] windowParams) {}
}
