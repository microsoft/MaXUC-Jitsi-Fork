/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import static org.jitsi.util.Hasher.logHasher;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;
import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.main.chat.menus.*;
import net.java.sip.communicator.impl.gui.main.chat.toolBars.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.skin.*;

/**
 * The chat window is the place, where users can write and send messages, view
 * received messages.
 * The <tt>ChatPanel</tt> is added like a "write message", "send button", etc.
 * It corresponds to a <tt>MetaContact</tt> or to a conference.
 * <p>
 * Note that the conference case is not yet implemented.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 * @author Adam Netocny
 */
public class ChatWindow
    extends SIPCommFrame
    implements  ChatContainer,
                ExportedWindow,
                PluginComponentListener,
                WindowFocusListener
{
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>Logger</tt> used by the <tt>ChatWindow</tt> class and its
     * instances for logging output.
     */
    private static final Logger sLog = Logger.getLogger(ChatWindow.class);

    private final List<ChatChangeListener> mChatChangeListeners
        = new Vector<>();

    private final JPanel mMainPanel
        = new TransparentPanel(new BorderLayout());

    private final JPanel mStatusBarPanel
        = new TransparentPanel(new BorderLayout());

    private final JPanel mPluginPanelSouth = new JPanel();
    private final JPanel mPluginPanelWest = new JPanel();
    private final JPanel mPluginPanelEast = new JPanel();

    private final ContactPhotoPanel mContactPhotoPanel;

    private ChatToolbar mChatToolbar;

    private final JPanel mToolbarPanel;

    /**
     * The chat panel that has been opened in this chat window.
     */
    private ChatPanel mChatPanel;

    /**
     * A keyboard manager, where we register our own key dispatcher.
     */
    private KeyboardFocusManager mKeyManager;

    /**
     * A key dispatcher that redirects all key events to call field.
     */
    private KeyEventDispatcher mKeyDispatcher;

    /**
     * The manager for this chat window.
     */
    private ChatWindowManager mManager;

    /**
     * Whether this window is showing a conference chat session or not
     */
    private boolean mIsConference;

    /**
     * The split pane that is used to divide the toolbar and main content panes
     * when in a group chat
     */
    private JSplitPane mToolbarSplitPane;

    /**
     * The content pane of this window
     */
    private java.awt.Container mContentPane;

    /**
     * Creates an instance of <tt>ChatWindow</tt> by passing to it an instance
     * of the main application window.
     */
    public ChatWindow(ChatWindowManager manager)
    {
        mManager = manager;
        if (!ConfigurationUtils.isWindowDecorated())
        {
            setUndecorated(true);
        }

        addWindowFocusListener(this);
        setMinimumSize(new ScaledDimension(400, 200));

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        mContactPhotoPanel = new ContactPhotoPanel();
        mContactPhotoPanel.setVisible(!ConfigurationUtils.isChatShowContact());

        mToolbarPanel = createToolBar();

        mToolbarSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mToolbarSplitPane.setBorder(null);
        mToolbarSplitPane.setOpaque(false);
        mToolbarSplitPane.setDividerSize(3);

        mContentPane = getContentPane();
        mContentPane.add(mToolbarPanel, BorderLayout.NORTH);
        mContentPane.add(mMainPanel, BorderLayout.CENTER);
        mContentPane.add(mStatusBarPanel, BorderLayout.SOUTH);

        initPluginComponents();

        setKeybindingInput(KeybindingSet.Category.CHAT);
        addKeybindingAction("chat-copy", new CopyAction());
        addKeybindingAction("chat-paste", new PasteAction());
        addKeybindingAction("chat-openSmileys", new OpenSmileyAction());
        addKeybindingAction("chat-openHistory", new OpenHistoryAction());
        addKeybindingAction("chat-close", new CloseAction());

        addWindowListener(new ChatWindowAdapter());

        int width = GuiActivator.getResources()
            .getSettingsInt("impl.gui.CHAT_WINDOW_WIDTH");
        int height = GuiActivator.getResources()
            .getSettingsInt("impl.gui.CHAT_WINDOW_HEIGHT");

        setSize(width, height);

        // Set initially invisible to allow the location of the window to be
        // adjusted before display.
        setVisible(false);
    }

    /**
     * @see SIPCommFrame#dispose()
     */
    public void dispose()
    {
        try
        {
            AbstractUIServiceImpl uiService = GuiActivator.getUIService();

            /*
             * The ChatWindow should cease to exist so we don't want any strong
             * references to it i.e. it cannot be exported anymore.
             */
            uiService.unregisterExportedWindow(this);

            uiService.removePluginComponentListener(this);

            removeWindowFocusListener(this);

            mChatToolbar.dispose();
        }
        finally
        {
            super.dispose();
        }
    }

    /**
     * @return the main toolbar in this chat window
     */
    public ChatToolbar getChatToolbar()
    {
        return mChatToolbar;
    }

    /**
     * Adds a given <tt>ChatPanel</tt> to this chat window.
     *
     * @param chatPanel The <tt>ChatPanel</tt> to add.
     */
    public void addChat(final ChatPanel chatPanel)
    {
        mChatPanel = chatPanel;

        mMainPanel.add(chatPanel, BorderLayout.CENTER);

        chatPanel.setShown(true);

        notifyChatChanged(chatPanel);
    }

    /**
     * Send a notification to all registered listeners that the chat has changed
     *
     * @param chatPanel the panel that has changed
     */
    private void notifyChatChanged(ChatPanel chatPanel)
    {
        List<ChatChangeListener> chatListeners;

        synchronized (mChatChangeListeners)
        {
            chatListeners = new ArrayList<>(mChatChangeListeners);
        }

        for (ChatChangeListener l : chatListeners)
            l.chatChanged(chatPanel);
    }

    /**
     * Creates the toolbar panel for this chat window, depending on the current
     * operating system.
     *
     * @return the created toolbar
     */
    private JPanel createToolBar()
    {
        JPanel toolbarPanel = null;

        mChatToolbar = new ChatToolbar(this);

        ToolbarPanel panel = new ToolbarPanel(new BorderLayout());

        panel.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        panel.add(mChatToolbar, BorderLayout.CENTER);
        panel.add(mContactPhotoPanel, BorderLayout.EAST);
        panel.setVisible(true);

        toolbarPanel = panel;

        return toolbarPanel;
    }

    /**
     * Removes a given <tt>ChatPanel</tt> from this chat window.
     *
     * @param chatPanel The <tt>ChatPanel</tt> to remove.
     */
    public void removeChat(ChatPanel chatPanel)
    {
        sLog.debug("Removes chat for contact: " +
                logHasher(chatPanel.getChatSession().getChatName()));

        // Remove the chat panel directly from the content pane.
        mMainPanel.remove(chatPanel);

        dispose();
    }

    /**
     * Selects the chat tab which corresponds to the given <tt>MetaContact</tt>.
     *
     * @param chatPanel The <tt>ChatPanel</tt> to select.
     */
    public void setCurrentChat(final ChatPanel chatPanel)
    {
        ChatSession chatSession = chatPanel.getChatSession();

        setConferenceUI(chatSession instanceof ConferenceChatSession);

        sLog.debug(
                "Set current chat panel to: " + chatSession.getChatName());

        chatPanel.requestFocusInWriteArea();

        notifyChatChanged(chatPanel);
    }

    /**
     * Sets up the UI depending on whether this window is showing a conference
     * or not.
     *
     * @param isNowConference
     */
    private void setConferenceUI(boolean isNowConference)
    {
        if (isNowConference && !mIsConference)
        {
            sLog.debug("Converting chat window to conference UI");
            mContentPane.remove(mToolbarPanel);
            mContentPane.remove(mMainPanel);
            mToolbarSplitPane.setTopComponent(mToolbarPanel);
            mToolbarSplitPane.setBottomComponent(mMainPanel);
            mContentPane.add(mToolbarSplitPane, BorderLayout.CENTER);
            mToolbarPanel.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));

            mContentPane.revalidate();
            mContentPane.repaint();
        }

        mIsConference = isNowConference;
    }

    /**
     * Returns the <tt>ChatPanel</tt> for this <tt>ChatContainer</tt>.
     *
     * @return a <tt>ChatPanel</tt>.
     */
    public ChatPanel getCurrentChat()
    {
        return mChatPanel;
    }

    /**
     * The <tt>CopyAction</tt> is an <tt>AbstractAction</tt> that copies the
     * text currently selected.
     */
    private class CopyAction
        extends UIAction
    {
        private static final long serialVersionUID = 0L;

        public void actionPerformed(ActionEvent e)
        {
            getCurrentChat().copy();
        }
    }

    /**
     * The <tt>PasteAction</tt> is an <tt>AbstractAction</tt> that pastes
     * the text contained in the clipboard in the current <tt>ChatPanel</tt>.
     */
    class PasteAction
        extends UIAction
    {
        private static final long serialVersionUID = 0L;

        public void actionPerformed(ActionEvent e)
        {
            getCurrentChat().paste();
        }
    }

    /**
     * The <tt>OpenSmileyAction</tt> is an <tt>AbstractAction</tt> that
     * opens the menu, containing all available smileys' icons.
     */
    private class OpenSmileyAction
        extends UIAction
    {
        private static final long serialVersionUID = 0L;

        public void actionPerformed(ActionEvent e)
        {
            IconSelectorBox smileyBox =
                mChatPanel.getChatWritePanel().getIconSelectorBox();
            if (smileyBox != null)
            {
                smileyBox.open();
            }
        }
    }

    /**
     * The <tt>OpenHistoryAction</tt> is an <tt>AbstractAction</tt> that
     * opens the history window for the currently selected contact.
     */
    private class OpenHistoryAction
        extends UIAction
    {
        private static final long serialVersionUID = 0L;

        public void actionPerformed(ActionEvent e)
        {
            mChatToolbar.getHistoryButton().doClick();
        }
    }

    /**
     * The <tt>CloseAction</tt> is an <tt>AbstractAction</tt> that closes
     * something in the chat window, or the chat window itself if appropriate.
     */
    private class CloseAction
        extends UIAction
    {
        private static final long serialVersionUID = 0L;

        public void actionPerformed(ActionEvent e)
        {
            close(true);
        }
    }

    /**
     * Before closing the chat window saves the current size and position
     * through the <tt>ConfigurationService</tt>.
     */
    public class ChatWindowAdapter
        extends WindowAdapter
    {
        public void windowDeiconified(WindowEvent e)
        {
            String title = getTitle();

            if (title.startsWith("*"))
                setTitle(title.substring(1, title.length()));
        }

        public void windowOpened(WindowEvent e)
        {
            if (mKeyManager == null)
            {
                mKeyManager
                    = KeyboardFocusManager.getCurrentKeyboardFocusManager();
                if (mKeyDispatcher == null)
                    mKeyDispatcher = new MainKeyDispatcher(mKeyManager);
                mKeyManager.addKeyEventDispatcher(mKeyDispatcher);
            }
        }

        @Override
        public void windowClosed(WindowEvent e)
        {
            if (mKeyManager != null)
            {
                mKeyManager.removeKeyEventDispatcher(mKeyDispatcher);
            }

            mKeyManager = null;
            mKeyDispatcher = null;
        }
    }

    /**
     * Implements the <tt>SIPCommFrame</tt> close method. We check for an open
     * menu and if there's one we close it, otherwise we close the current chat.
     * @param isEscaped indicates if this window was closed by pressing the esc
     * button
     */
    protected void close(boolean isEscaped)
    {
        sLog.user(
            "Asked to close chat window: " + this + ", escaped = " + isEscaped);

        if (isEscaped)
        {
            ChatPanel chatPanel = getCurrentChat();

            ChatRightButtonMenu chatRightMenu
                = chatPanel.getChatConversationPanel().getRightButtonMenu();

            ChatWritePanel chatWritePanel = chatPanel.getChatWritePanel();
            WritePanelRightButtonMenu writePanelRightMenu
                = chatWritePanel.getRightButtonMenu();

            if (chatRightMenu.isVisible())
            {
                sLog.info("Closing chat right menu");
                chatRightMenu.setVisible(false);
            }
            else if (writePanelRightMenu.isVisible())
            {
                sLog.info("Closing write panel right menu");
                writePanelRightMenu.setVisible(false);
            }
            else if (chatWritePanel.getIconSelectorBox() != null &&
                     chatWritePanel.getIconSelectorBox().getPopupMenu().isVisible())
            {
                sLog.info("Closing menu bar menu");
                MenuSelectionManager.defaultManager().clearSelectedPath();
            }
            else if (mChatToolbar.isConferenceMenuVisible())
            {
                sLog.info("Closing conference menu");
                mChatToolbar.closeConferenceMenu();
            }
            else
            {
                sLog.info("Closing chat panel");
                GuiActivator
                    .getUIService().getChatWindowManager().closeChat(chatPanel);
            }
        }
        else
        {
            sLog.info("Closing all chats");
            GuiActivator
                .getUIService().getChatWindowManager().closeChat(this, true);
        }
    }

    /**
     * Implements the <tt>ExportedWindow.getIdentifier()</tt> method.
     * @return the identifier of this window, used as plugin container
     * identifier.
     */
    public WindowID getIdentifier()
    {
        return ExportedWindow.CHAT_WINDOW;
    }

    /**
     * Implements the <tt>ExportedWindow.minimize()</tt> method. Minimizes this
     * window.
     */
    public void minimize()
    {
        setExtendedState(JFrame.ICONIFIED);
    }

    /**
     * Implements the <tt>ExportedWindow.maximize()</tt> method. Maximizes this
     * window.
     */
    public void maximize()
    {
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    /**
     * Implements the <tt>ExportedWindow.bringToFront()</tt> method. Brings
     * this window to front.
     */
    public void bringToFront()
    {
        if (getExtendedState() == JFrame.ICONIFIED)
            setExtendedState(JFrame.NORMAL);

        toFront();
    }

    @Override
    public void bringToBack()
    {
        toBack();
    }

    /**
     * Initialize plugin components already registered for this container.
     */
    private void initPluginComponents()
    {
        // Make sure that we don't miss any event.
        GuiActivator.getUIService().addPluginComponentListener(this);

        mPluginPanelEast.setLayout(
            new BoxLayout(mPluginPanelEast, BoxLayout.Y_AXIS));
        mPluginPanelSouth.setLayout(
            new BoxLayout(mPluginPanelSouth, BoxLayout.Y_AXIS));
        mPluginPanelWest.setLayout(
            new BoxLayout(mPluginPanelWest, BoxLayout.Y_AXIS));

        mContentPane.add(mPluginPanelEast, BorderLayout.EAST);
        mContentPane.add(mPluginPanelWest, BorderLayout.WEST);
        mMainPanel.add(mPluginPanelSouth, BorderLayout.SOUTH);

        // Search for plugin components registered through the OSGI bundle
        // context.
        ServiceReference<?>[] serRefs = null;

        String osgiFilter = "(|("
            + Container.CONTAINER_ID
            + "="+Container.CONTAINER_CHAT_WINDOW.getID()+")"
            + "(" + Container.CONTAINER_ID
            + "="+Container.CONTAINER_CHAT_STATUS_BAR.getID()+"))";

        try
        {
            serRefs = GuiActivator.bundleContext.getServiceReferences(
                PluginComponent.class.getName(),
                osgiFilter);
        }
        catch (InvalidSyntaxException exc)
        {
            sLog.error("Could not obtain plugin component reference.", exc);
        }

        if (serRefs != null)
        {
            for (ServiceReference<?> serRef : serRefs)
            {
                PluginComponent c
                    = (PluginComponent)
                        GuiActivator .bundleContext.getService(serRef);

                Component comp = (Component) c.getComponent();

                // If this component has been already added, we have nothing
                // more to do here.
                if (comp.getParent() != null)
                    return;

                Object borderLayoutConstraint = StandardUIServiceImpl
                    .getBorderLayoutConstraintsFromContainer(c.getConstraints());

                addPluginComponent(comp,
                                   c.getContainer(),
                                   borderLayoutConstraint);
            }
        }
    }

    /**
     * Adds a plugin component to this container.
     * @param event the <tt>PluginComponentEvent</tt> that notified us of the
     * add
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        Component comp = (Component) c.getComponent();

        // If this component has been already added, we have nothing more to do
        // here.
        if (comp.getParent() != null)
            return;

        if (c.getContainer().equals(Container.CONTAINER_CHAT_WINDOW)
            || c.getContainer().equals(Container.CONTAINER_CHAT_STATUS_BAR))
        {
            Object borderLayoutConstraints = StandardUIServiceImpl
                .getBorderLayoutConstraintsFromContainer(c.getConstraints());

            addPluginComponent((Component) c.getComponent(),
                               c.getContainer(),
                               borderLayoutConstraints);
        }
    }

    /**
     * Removes a plugin component from this container.
     * @param event the <tt>PluginComponentEvent</tt> that notified us of the
     * remove
     */
    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if (c.getContainer().equals(Container.CONTAINER_CHAT_WINDOW)
            || c.getContainer().equals(Container.CONTAINER_CHAT_STATUS_BAR))
        {
            Object borderLayoutConstraint = StandardUIServiceImpl
                .getBorderLayoutConstraintsFromContainer(c.getConstraints());

            removePluginComponent((Component) c.getComponent(),
                                  c.getContainer(),
                                  borderLayoutConstraint);

            pack();
        }
    }

    /**
     * The source of the window
     * @return the source of the window
     */
    public Object getSource()
    {
        return this;
    }

    /**
     * Adds the given <tt>ChatChangeListener</tt>.
     * @param listener the listener to add
     */
    public void addChatChangeListener(ChatChangeListener listener)
    {
        synchronized (mChatChangeListeners)
        {
            if (!mChatChangeListeners.contains(listener))
                mChatChangeListeners.add(listener);
        }
    }

    /**
     * Removes the given <tt>ChatChangeListener</tt>.
     * @param listener the listener to remove
     */
    public void removeChatChangeListener(ChatChangeListener listener)
    {
        synchronized (mChatChangeListeners)
        {
            mChatChangeListeners.remove(listener);
        }
    }

    /**
     * Adds the given component with to the container corresponding to the
     * given constraints.
     *
     * @param c the component to add
     * @param container the plugin container
     * @param constraints the constraints determining the container
     */
    private void addPluginComponent(Component c,
                                    Container container,
                                    Object constraints)
    {
        if (container.equals(Container.CONTAINER_CHAT_WINDOW))
        {
            if (constraints.equals(BorderLayout.SOUTH))
            {
                mPluginPanelSouth.add(c);
                mPluginPanelSouth.repaint();
            }
            else if (constraints.equals(BorderLayout.WEST))
            {
                mPluginPanelWest.add(c);
                mPluginPanelSouth.repaint();
            }
            else if (constraints.equals(BorderLayout.EAST))
            {
                mPluginPanelEast.add(c);
                mPluginPanelSouth.repaint();
            }
        }
        else if (container.equals(Container.CONTAINER_CHAT_STATUS_BAR))
        {
            mStatusBarPanel.add(c);
        }

        mContentPane.repaint();
    }

    /**
     * Removes the given component from the container corresponding to the given
     * constraints.
     *
     * @param c the component to remove
     * @param container the plugin container
     * @param constraints the constraints determining the container
     */
    private void removePluginComponent(Component c,
                                       Container container,
                                       Object constraints)
    {
        if (container.equals(Container.CONTAINER_CHAT_WINDOW))
        {
            if (constraints.equals(BorderLayout.SOUTH))
                mPluginPanelSouth.remove(c);
            else if (constraints.equals(BorderLayout.WEST))
                mPluginPanelWest.remove(c);
            else if (constraints.equals(BorderLayout.EAST))
                mPluginPanelEast.remove(c);
        }
        else if (container.equals(Container.CONTAINER_CHAT_STATUS_BAR))
        {
            mStatusBarPanel.remove(c);
        }
    }

    /**
     * Sets the chat panel contact photo to this window.
     *
     * @param chatSession
     */
    private void setChatContactPhoto(ChatSession chatSession)
    {
        mContactPhotoPanel.setChatSession(chatSession);
    }

    /**
     * Implementation of {@link ExportedWindow#setParams(Object[])}.
     */
    public void setParams(Object[] windowParams) {}

    /**
     * Handles <tt>WindowEvent</tt>s triggered when the window has gained focus.
     * @param evt the <tt>WindowEvent</tt>
     */
    public void windowGainedFocus(WindowEvent evt)
    {
        ChatPanel currentChat = getCurrentChat();

        if (currentChat != null)
        {
            sLog.debug(
                "Window focused - clear new message notifications for '" +
                                                              getTitle() + "'");
            // We haven't received a new message in this chat - we've just
            // brought the window into focus, so make sure we just update the
            // 'Recent' tab entry in place, rather than moving it to the top of
            // the list.
            currentChat.clearNewMessageNotifications(true);
            GuiActivator.getUIService().getChatWindowManager()
                .removeNonReadChatState(currentChat);
        }
    }

    public void windowLostFocus(WindowEvent arg0) {}

    private static class ToolbarPanel
        extends TransparentPanel
        implements Skinnable
    {
        private static final long serialVersionUID = 0L;

        private BufferedImageFuture logoBgImage;

        public ToolbarPanel(LayoutManager layoutManager)
        {
            super(layoutManager);

            loadSkin();
        }

        @Override
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            Image logoBgImage = this.logoBgImage.resolve();

            if (logoBgImage != null)
            {
                g.drawImage(
                        logoBgImage,
                        (this.getWidth() - logoBgImage.getWidth(null))/2,
                        0,
                        null);
            }
        }

        /**
         * Reloads bg image.
         */
        public void loadSkin()
        {
            BufferedImageFuture logoBgImage = GuiActivator.getImageLoaderService().getImage(
                ImageLoaderService.WINDOW_TITLE_BAR);
            if (logoBgImage != null)
                logoBgImage
                    = logoBgImage.scaleImageWithinBounds(80, 35);

            this.logoBgImage = logoBgImage;
        }
    }

    /**
     * Sends the given file when dropped to the chat window.
     * @param file the file to send
     * @param point the point, where the file was dropped
     */
    public void fileDropped(File file, Point point)
    {
        getCurrentChat().sendFile(file);
    }

    /**
     * Opens the specified <tt>ChatPanel</tt> and optionally brings it to the
     * front.
     *
     * @param chatPanel the <tt>ChatPanel</tt> to be opened
     * @param setSelected <tt>true</tt> if <tt>chatPanel</tt> (and respectively
     * this <tt>ChatContainer</tt>) should be brought to the front; otherwise,
     * <tt>false</tt>
     * @param alertWindow if true, the window will be alerted when it is opened.
     */
    public void openChat(ChatPanel chatPanel, boolean setSelected, boolean alertWindow)
    {
        boolean isWindowHidden = !isVisible();

        ChatWritePanel chatWritePanel = chatPanel.getChatWritePanel();
        ChatSession chatSession = chatPanel.getChatSession();
        String chatWindowTitle = (chatSession instanceof ConferenceChatSession) ?
                                    ((ConferenceChatSession) chatSession).getChatSubject() :
                                    chatSession.getChatName();
        // Only set the title if the new one is not null
        if (!StringUtils.isNullOrEmpty(chatWindowTitle))
        {
            setTitle(chatWindowTitle);
        }

        setChatContactPhoto(chatSession);
        IconSelectorBox smileyBox = chatWritePanel.getIconSelectorBox();
        if (smileyBox != null)
        {
            smileyBox.setChat(chatPanel);
        }

        if (getExtendedState() != JFrame.ICONIFIED)
        {
            if (GuiActivator.getGlobalStatusService().isDoNotDisturb() &&
                !setSelected)
            {
                // The user is on DND, and this is an incoming chat. Therefore
                // request the focus and set the state to be iconified so that
                // the user is not disturbed.  In particular, requesting focus
                // ensures that the icon does not flash in the task bar (so is
                // only required on windows).
                if (isWindowHidden)
                {
                    // Only actually hide the window if it isn't already showing
                    // we don't want to hide the window while the user is looking
                    // at it!
                    if (OSUtils.IS_WINDOWS)
                        requestFocus();

                    setState(Frame.ICONIFIED);
                    setVisible(true);
                }
            }
            else if (ConfigurationUtils.isAutoPopupNewMessage() || setSelected)
            {
                if (isWindowHidden)
                    setVisible(true);

                toFront();
            }
            else if (isWindowHidden)
            {
                setFocusableWindowState(false);
                setVisible(true);
                setFocusableWindowState(true);

                // Make the chat window visible but, unless we're running on
                // Mac make sure it stays at the back so it doesn't get in the
                // way of any other windows that the user might be using.
                if (!OSUtils.IS_MAC)
                    toBack();
            }
        }
        else
        {
            if (setSelected)
            {
                setExtendedState(JFrame.NORMAL);
                toFront();
            }

            setTitle("*" + chatWindowTitle);
        }

        if (setSelected)
        {
            setCurrentChat(chatPanel);
        }
    }

    /**
     * Returns the frame to which this container belongs.
     *
     * @return the frame to which this container belongs
     */
    public Frame getFrame()
    {
        return this;
    }

    /**
     * Indicates if the parent frame is currently the active window.
     *
     * @return <tt>true</tt> if the parent window is currently the active
     * window, <tt>false</tt> otherwise
     */
    public boolean isFrameActive()
    {
        return isActive();
    }

    /**
     * The <tt>MainKeyDispatcher</tt> is added to pre-listen KeyEvents before
     * they're delivered to the current focus owner in order to introduce a
     * specific behavior for the <tt>CallField</tt> on top of the dial pad.
     */
    private class MainKeyDispatcher implements KeyEventDispatcher
    {
        private final KeyboardFocusManager mKeyManager;

        /**
         * Creates an instance of <tt>MainKeyDispatcher</tt>.
         * @param keyManager the parent <tt>KeyboardFocusManager</tt>
         */
        public MainKeyDispatcher(KeyboardFocusManager keyManager)
        {
            mKeyManager = keyManager;
        }

        /**
         * Dispatches the given <tt>KeyEvent</tt>.
         * @param e the <tt>KeyEvent</tt> to dispatch
         * @return <tt>true</tt> if the KeyboardFocusManager should take no
         * further action with regard to the KeyEvent; <tt>false</tt>
         * otherwise
         */
        public boolean dispatchKeyEvent(KeyEvent e)
        {
            // If this window is not the focus window or if the event is not
            // of type PRESSED we have nothing more to do here.
            if (!isFocused() || (e.getID() != KeyEvent.KEY_TYPED))
                return false;

            // There are a number of keys that are used to interact with the
            // ChatToolbar, which must not result in us grabbing the focus
            // away from the toolbar
            char keyChar = e.getKeyChar();
            if (keyChar == KeyEvent.CHAR_UNDEFINED
                || keyChar == KeyEvent.VK_ENTER
                || keyChar == KeyEvent.VK_UP
                || keyChar == KeyEvent.VK_DOWN
                || keyChar == KeyEvent.VK_ESCAPE
                || e.getKeyCode() == KeyEvent.VK_ENTER)
            {
                if (mChatToolbar.isConferenceMenuVisible())
                {
                    // Send the key to the toolbar
                    mKeyManager.redispatchEvent(mChatToolbar, e);
                }

                return false;
            }

            if (getCurrentChat() == null)
                return false;

            ChatWritePanel chatWritePanel
                = getCurrentChat().getChatWritePanel();
            JEditorPane chatWriteEditor = chatWritePanel.getEditorPane();

            if (mKeyManager.getFocusOwner() != null
                && !chatWritePanel.isFocusOwner())
            {
                // Request the focus in the chat write panel if a letter is
                // typed.

                // Since we are grabbing the focus, we want to hide any menu
                // that previously may have had the focus
                if (mChatToolbar.isConferenceMenuVisible())
                {
                    sLog.info("Closing conference menu as we grab focus");
                    mChatToolbar.closeConferenceMenu();
                }

                chatWriteEditor.requestFocusInWindow();

                // We re-dispatch the event to the chat write panel.
                mKeyManager.redispatchEvent(chatWriteEditor, e);

                // We don't want to dispatch further this event.
                return true;
            }

            return false;
        }
    }

    @Override
    public void updateContainer(ChatPanel chatPanel)
    {
        ChatSession chatSession = chatPanel.getChatSession();
        setConferenceUI(chatSession instanceof ConferenceChatSession);
        notifyChatChanged(chatPanel);
    }

    @Override
    public void repaintWindow()
    {
        repaint();
    }

    @Override
    public void setVisible(final boolean isVisible)
    {
        // Hidden for DUIR - prevent showing group chat window
        // if (isVisible)
        // {
        //     mManager.mWindowStacker.addWindow(ChatWindow.this);
        // }

        // ChatWindow.super.setVisible(isVisible);

        // if (mChatPanel != null)
        // {
        //     mChatPanel.setVisible(isVisible);
        // }
    }
}
