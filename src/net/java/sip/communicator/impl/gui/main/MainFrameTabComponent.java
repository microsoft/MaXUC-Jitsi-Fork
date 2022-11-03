// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.accessibility.AccessibleRole;
import javax.swing.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.MainFrame.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.plugin.desktoputil.ScaleUtils;
import net.java.sip.communicator.plugin.desktoputil.event.*;
import net.java.sip.communicator.plugin.desktoputil.lookandfeel.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.notification.NotificationAction;
import net.java.sip.communicator.service.notification.NotificationData;
import net.java.sip.communicator.service.notification.NotificationHandler;
import net.java.sip.communicator.service.notification.NotificationService;
import net.java.sip.communicator.service.notification.UINotificationHandler;
import net.java.sip.communicator.service.notification.UINotificationListener;
import net.java.sip.communicator.util.AccessibilityUtils;
import net.java.sip.communicator.util.AccessibleWrappedJTextField;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;

/**
 * An object representing the UI of a tab on the main frame. It implements
 * <tt>MouseListener</tt> in order to activate the tab when it is clicked. It
 * implements <tt>UINotificationListener</tt> as the tab is able to display a
 * notification counter for notifications (currently missed calls and unread chats)
 */
public class MainFrameTabComponent extends JPanel
                                   implements MouseListener,
                                              ActionListener,
                                              UINotificationListener
{
    private static final Logger sLog = Logger.getLogger(MainFrameTabComponent.class);
    private static final long serialVersionUID = 1L;

    // Colors used by the tab
    private Color selectedColor =
        new Color(GuiActivator.getResources()
                               .getColor("service.gui.LIGHT_BACKGROUND"));
    private Color unselectedColor = ConfigurationUtils.useNativeTheme() ?
        new Color(GuiActivator.getResources()
                                       .getColor("service.gui.CALL_TOOL_BAR")) :
        MainFrame.HEADER_BACKGROUND_GRADIENT;
    private static final Color BORDER_COLOR = Color.GRAY;

    /**
    * Text color for the title of the selected tab. Text color on a mac is
    * not brandable.
    */
    private static final Color TEXT_COLOR =
        ConfigurationUtils.useNativeTheme() ? Color.BLACK :
            MainFrame.HEADER_TEXT_COLOUR;

    /**
     * The string for the Favourites tab
     */
    public static final String FAVOURITES_TAB_NAME = "service.gui.TAB_FAVORITES";

    /**
     * The string for the Contacts tab
     */
    public static final String CONTACTS_TAB_NAME = "service.gui.TAB_CONTACTS";

    /**
     * The string for the History tab
     */
    public static final String HISTORY_TAB_NAME = "service.gui.TAB_HISTORY";

    /**
     * The amount of padding to give the notification image on the right hand
     * side.
     */
    private static final int mNotificationPaddingRight = 2;

    /**
     * The currently selected tab, or the last selected tab if none is currently
     * selected
     */
    private static MainFrameTabComponent sLastSelectedTab;

    /**
     * The resource of the title of this tab
     */
    private final String mTitleRes;

    /**
     * The contact list that is displayed by this tab.
     */
    private TreeContactList mContactList;

    /**
     * The filter to apply to the contact list when this tab is selected
     */
    private ContactListFilter mFilterType;

    /**
     * The panel to which this tab is added
     */
    private final JPanel mOwner;

    /**
     * The text field displaying the title of this tab
     */
    private final JTextField mTextField;

    /**
     * The label displaying notifications
     */
    private final JLabel mNotifyField;

    /**
     * A list of the event types that the tab will notify for
     */
    private final Set<String> mNotifyFor;

    /**
     * true if this tab is selected
     */
    private boolean mSelected = false;

    /**
     * true if this tab is the first tab to be added
     */
    private boolean mIsFirst = false;

    /**
     * true if this tab is the last tab added
     */
    private boolean mIsLast = false;

    /**
     * The number of unacknowledged missed call notifications.
     */
    private int mCallsNotificationCount = 0;

    /**
     * The number of unacknowledged unread chat notifications
     */
    private int mChatsNotificationCount = 0;

    /**
     * An object to synchronize changes to the unacknowledged calls and
     * messages.
     */
    private final Object mNotificationsLock = new Object();

    private JPanel mButtonPanel;
    private JPanel mCenterPanel;

    /**
     * Constructor
     *
     * @param titleRes The resource key of the title of this tab
     * @param contactList The contact list that is displayed by this tab.
     * @param filterType The filter to apply to the contact list when this tab
     *                   is clicked
     * @param panel The panel to which this tab is to be added
     * @param notifyFor The list of events that the tab will notify for. This
     * is optional and can be null
     * @param searchField The search field that users enter text into to search
     * for a contact.
     * @param buttonPanel The panel containing the buttons that allow the user
     * to filter the history tab.
     * @param centerPanel The panel to contain the tabs.
     */
    public MainFrameTabComponent(String titleRes,
                                 TreeContactList contactList,
                                 ContactListFilter filterType,
                                 JPanel panel,
                                 String[] notifyFor,
                                 final SearchField searchField,
                                 JPanel buttonPanel,
                                 JPanel centerPanel)
    {
        mTitleRes = titleRes;
        mContactList = contactList;
        mFilterType = filterType;
        mOwner = panel;
        mNotifyFor = (notifyFor == null) ?
            null : new HashSet<>(Arrays.asList(notifyFor));
        mButtonPanel = buttonPanel;
        mCenterPanel = centerPanel;

        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        ResourceManagementService res = GuiActivator.getResources();

        // Create a text field to show the tab title
        mTextField = new AccessibleWrappedJTextField(AccessibleRole.LABEL);
        mTextField.setHighlighter(null);
        mTextField.setText(res.getI18NString(titleRes));
        mTextField.addActionListener(this);
        mTextField.addMouseListener(this);
        mTextField.setEditable(false);
        mTextField.setHorizontalAlignment(SwingConstants.CENTER);
        add(mTextField);
        mTextField.setForeground(TEXT_COLOR);
        ScaleUtils.scaleFontAsDefault(mTextField);

        mTextField.getAccessibleContext().setAccessibleParent(panel);

        // Create a label to show any notifications for this tab
        mNotifyField = new JLabel();
        mNotifyField.addMouseListener(this);
        mNotifyField.setVisible(false);
        add(mNotifyField);

        // Initialise the borders
        updateAppearance();

        // Add this tab as a notification listener. We do not explicitly remove
        // these listeners as they are only added once and should persist until
        // the client shuts down.
        if (notifyFor != null)
        {
            GuiActivator.getNotificationService().addHandlerAddedListener(
                    new NotificationService.HandlerAddedListener() {
                        @Override
                        public void onHandlerAdded(NotificationHandler handler)
                        {
                            if (handler instanceof UINotificationHandler)
                            {
                                sLog.debug("Registered as UINotificationListener");
                                ((UINotificationHandler) handler)
                                        .addNotificationListener(MainFrameTabComponent.this);
                            }
                        }
                    }
            );
            for (NotificationHandler handler :
                    GuiActivator.getNotificationService().getActionHandlers(
                            NotificationAction.ACTION_DISPLAY_UI_NOTIFICATIONS))
            {
                ((UINotificationHandler) handler)
                        .addNotificationListener(MainFrameTabComponent.this);
            }
        }

        // Add a listener for text being added or removed to the search field.
        // We want to un-select all tabs when the search field is active and
        // re-select it afterwards
        searchField.addTextChangeListener(new TextFieldChangeListener()
        {
            /**
             * Flag that is true if the "select" runnable needs to be removed
             */
            private boolean mSelectRunnableCancelled = false;

            /**
             * Runnable to enable the selected UI as long as it has not been
             * cancelled in the mean time.
             *
             * We have to do this on a runnable that can be run "later" as
             * altering the search text will first clear, then insert text.
             * Thus without this runnable, adding text can result in text not
             * appearing at all
             */
            private Runnable mSelectRunnable = new Runnable()
            {
                @Override
                public void run()
                {
                    if (!mSelectRunnableCancelled)
                    {
                        setSelected(true);
                    }
                }
            };

            @Override
            public void textRemoved()
            {
                String text = searchField.getText();
                if ((text == null || text.length() == 0) &&
                    sLastSelectedTab == MainFrameTabComponent.this)
                {
                    mSelectRunnableCancelled = false;
                    SwingUtilities.invokeLater(mSelectRunnable);
                }
            }

            @Override
            public void textInserted()
            {
                String text = searchField.getText();
                if (text != null && text.length() > 0)
                {
                    // Text has been added thus cancel the select runnable
                    mSelectRunnableCancelled = true;
                    setSelected(false);
                }
            }
        });
    }

    /**
     * Style the whole appearance of this tab so that it appears correctly
     * depending on whether it is selected or not.
     * Also make sure the accessibility info reflects the current info.
     */
    private void updateAppearance()
    {
        // If using modern tabs, we don't want any borders
        if (mSelected && !ConfigurationUtils.useModernTabsInMainFrame())
        {
            // Create a 1 pixel border to wrap round the tab except at the
            // bottom and where the tab touches the side of the main window
            setBorder(BorderFactory.createMatteBorder(1,
                                                      mIsFirst ? 0 : 1,
                                                      0,
                                                      mIsLast ? 0 : 1,
                                                      BORDER_COLOR));

            // If a notification image is visible then pad the text field on
            // the left hand side so the text remains central on the tab.
            int leftPadding = 0;
            if (mNotifyField.isVisible())
            {
                int notificationIconWidth = mNotifyField.getIcon()
                                                            .getIconWidth();
                leftPadding = notificationIconWidth +
                                             mNotificationPaddingRight;
            }

            // Make sure that the text field has the right colour.  Add some
            // padding above and below so that it looks nice.  Note that the
            // padding above has to be slightly smaller otherwise the text will
            // jump up and down as the tab is selected or  not
            mTextField.setBackground(selectedColor);
            mNotifyField.setBackground(selectedColor);
            mTextField.setBorder(
                    BorderFactory.createEmptyBorder(
                        4,
                        leftPadding + (mIsFirst ? 1 : 0),
                        5,
                        mIsLast ? 1 : 0));

            mNotifyField.setBorder(BorderFactory
                        .createEmptyBorder(0, 0, 0, mNotificationPaddingRight));
            setBackground(selectedColor);
        }
        else
        {
            // If a notification image is visible then pad the text field on
            // the left hand side so the text remains central on the tab.
            int leftPadding = 0;
            if (mNotifyField.isVisible())
            {
                int notificationIconWidth = mNotifyField.getIcon()
                                                            .getIconWidth();
                leftPadding = notificationIconWidth +
                                             mNotificationPaddingRight;
            }

            mTextField.setBorder(BorderFactory
                                    .createEmptyBorder(5, leftPadding, 5, 0));

            // Create a border just at the bottom of the tab
            int topBorder = ConfigurationUtils.useNativeTheme() ? 1 : 0;
            setBorder(BorderFactory
                          .createMatteBorder(topBorder, 0, 1, 0, BORDER_COLOR));

            // Reset the colours and borders to the unselected value
            mTextField.setBackground(unselectedColor);
            setBackground(unselectedColor);
            mNotifyField.setBackground(unselectedColor);
            mNotifyField.setBorder(BorderFactory
                      .createEmptyBorder(0, 0, 0, mNotificationPaddingRight));
        }

        final int notificationCount = mChatsNotificationCount + mCallsNotificationCount;
        AccessibilityUtils.setName(mTextField,
                                   GuiActivator.getResources().getI18NQuantityString(
                                           mNotifyField.isVisible() ?
                                                   (isSelected() ? "service.gui.accessibility.MAIN_TAB_SELECTED_WITH_NOTIFICATION" :
                                                                   "service.gui.accessibility.MAIN_TAB_NOT_SELECTED_WITH_NOTIFICATION") :
                                           isSelected() ? "service.gui.accessibility.MAIN_TAB_SELECTED" :
                                                          "service.gui.accessibility.MAIN_TAB_NOT_SELECTED",
                                           notificationCount,
                                           new String[] { mTextField.getText(), Integer.toString(notificationCount)}));
    }

    /**
     * Mark this tab as selected or not.
     *
     * @param selected if true then this tab will be marked as selected and the
     *        UI of this tab updated to show that
     */
    public synchronized void setSelected(boolean selected)
    {
        mSelected = selected;
        updateAppearance();

        mCenterPanel.getComponents();

        if (mButtonPanel != null)
        {
            if (mSelected && mButtonPanel.isEnabled())
            {
                mCenterPanel.add(mButtonPanel, BorderLayout.CENTER);
                mButtonPanel.setVisible(true);
            }
            else
            {
                mCenterPanel.remove(mButtonPanel);
                mButtonPanel.setVisible(false);
            }
        }

        if (mSelected)
        {
            Color textColor = ConfigurationUtils.useModernTabsInMainFrame() ?
                Constants.SELECTED_COLOR : Color.BLACK;
            mTextField.setForeground(textColor);

            sLastSelectedTab = this;
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    applyFilterToContacts();
                }
            });
        }
        else
        {
            mTextField.setForeground(TEXT_COLOR);
        }
    }

    /**
     * Returns whether this tab is currently selected.
     *
     * @return whether this tab is currently selected
     */
    public boolean isSelected()
    {
        return mSelected;
    }

    /**
     * Apply the filter of this tab to the contacts list.  Should be run on the
     * EDT thread
     */
    private void applyFilterToContacts()
    {
        mContactList.setCurrentFilter(mFilterType);
        mContactList.applyCurrentFilter();

        // Finally set that this tab was selected in the config.  This
        // allows us to open the client on the last tab that the user was
        // on.  Use the title resource as that should be unique across all
        // tabs.
        ConfigurationUtils.setSelectedTab(mTitleRes);

        if (mFilterType.equals(mContactList.getCallHistoryFilter()) ||
            mFilterType.equals(mContactList.getAllHistoryFilter()))
        {
            // Reset the notification count for this tab
            if (mNotifyFor != null)
            {
                synchronized (mNotificationsLock)
                {
                    mCallsNotificationCount = 0;
                    GuiActivator.getNotificationService().stopNotification(
                            new NotificationData("MissedCall",
                                                 null,
                                                 null,
                                                 null,
                                                 null)
                    );
                }

                updateNewEventNotifications();
            }
        }
    }

    /**
     * Sets the current filter for this tab and applies it to the contact list
     * if this tab is selected.
     *
     * @param filter the filter to set as the current filter
     */
    public void setFilter(ContactListFilter filter)
    {
        mFilterType = filter;

        if (mSelected)
        {
            applyFilterToContacts();
        }
    }

    /**
     * Set whether this tab is the first tab and restyle the tab appropriately.
     *
     * @param isFirst True if this is the first tab
     */
    public void setIsFirst(boolean isFirst)
    {
        if (mIsFirst != isFirst)
        {
            mIsFirst = isFirst;
            updateAppearance();
        }
    }

    /**
     * Set whether this tab is the last tab and restyle the tab appropriately.
     *
     * @param isLast True if this is the last tab
     */
    public void setIsLast(boolean isLast)
    {
        if (mIsLast != isLast)
        {
            mIsLast = isLast;
            updateAppearance();
        }
    }

    @Override
    public void updateUI(int callNotifications,
                         int chatNotifications,
                         int messageWaitingNotifications)
    {
        sLog.debug("Received UI notification");
        // There's a window at startup where we could come in here before we've even finished our
        // constructor, and we'll hit exceptions if we try to update the UI.
        if (GuiActivator.getUIService().getMainFrame() == null)
        {
            sLog.warn("Can't update UI before finished creating " + this);
            return;
        }

        synchronized (mNotificationsLock)
        {
            mCallsNotificationCount = callNotifications;
            mChatsNotificationCount = chatNotifications;
        }

        updateNewEventNotifications();
    }

    /**
     * Updates the count of new events on the 'Recent' tab and sets or unsets
     * the notification icons on the 'Calls' and 'Chats' sub tabs.
     */
    private void updateNewEventNotifications()
    {
        // Start by assuming we don't have any active notifications.
        boolean activeNotifications = false;
        boolean callsNotifications = false;
        boolean chatsNotifications = false;

        synchronized (mNotificationsLock)
        {
            // The total number of new events is the total number of missed
            // calls plus the total number of IM addresses/SMS numbers that
            // we have received new messages from.
            int messageNotificationCount = mChatsNotificationCount;
            int notificationsTotal =
                mCallsNotificationCount + messageNotificationCount;

            if (notificationsTotal > 0)
            {
                sLog.debug(
                    "Updating Notifications: Calls = " + mCallsNotificationCount +
                    ", Chats = " + messageNotificationCount);
                activeNotifications = true;

                // For notifications greater than 9 we display a '9+' image
                String imgString;
                if (notificationsTotal > 9)
                {
                    imgString = "service.gui.icons.TAB_NOTIFICATION_9PLUS";
                }
                else
                {
                    imgString = "service.gui.icons.TAB_NOTIFICATION_" +
                                                             notificationsTotal;
                }

                ImageIconFuture imgIcon = GuiActivator.getResources().getImage(imgString);
                // We need to resolve the image icon as we get it later, and rely on it
                // being set already.
                mNotifyField.setIcon(imgIcon.resolve());

                // Check whether we need to set individual notifications on
                // the calls and chats tabs
                callsNotifications = mCallsNotificationCount > 0;
                chatsNotifications = messageNotificationCount > 0;
            }
        }

        // Set or unset the notifications on the calls and chats tabs
        AbstractMainFrame mainFrame = GuiActivator.getUIService().getMainFrame();
        mainFrame.setSubTabNotification(HistorySubTab.callsName, callsNotifications);
        mainFrame.setSubTabNotification(HistorySubTab.chatsName, chatsNotifications);

        // Set the visibility of the notification image and restyle the borders
        // so the tab text is in the correct position
        mNotifyField.setVisible(activeNotifications);
        updateAppearance();
    }

    @Override
    public void actionPerformed(ActionEvent evt)
    {
        select();
    }

    /**
     * Selects this tab
     */
    public void select()
    {
        sLog.user(mTitleRes + " tab clicked");

        // If we are already selected then there is no action to take
        if (mSelected)
            return;

        // Make sure that we aren't showing any unknown contact views
        GuiActivator.getUIService().getMainFrame().
                                                enableUnknownContactView(false);

        // We only want to change the selected state of at most 2 tabs
        // - if one was previously selected then unselect it
        // - select the new one
        MainFrameTabComponent previousSelection = null;
        MainFrameTabComponent newSelection = null;
        for (Component component : mOwner.getComponents())
        {
            if (component instanceof MainFrameTabComponent)
            {
                MainFrameTabComponent tabComponent = (MainFrameTabComponent) component;

                if (tabComponent.isSelected())
                {
                    previousSelection = tabComponent;
                }
                if (Objects.equals(tabComponent.mTitleRes, mTitleRes))
                {
                    newSelection = tabComponent;
                }
            }
        }
        if (previousSelection != null && previousSelection != newSelection)
        {
            previousSelection.setSelected(false);
        }
        newSelection.setSelected(true);
    }

    /**
     * Reloads the colour resources and repaints this component
     */
    public void restyle()
    {
        selectedColor =
            new Color(GuiActivator.getResources()
                                   .getColor("service.gui.LIGHT_BACKGROUND"));
        unselectedColor = OSUtils.IS_MAC ?
            new Color(GuiActivator.getResources()
                                           .getColor("service.gui.CALL_TOOL_BAR")) :
            new Color(GuiActivator.getResources()
                                 .getColor("service.gui.LIGHT_BACKGROUND"));

        updateAppearance();
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent evt)
    {
        select();
        evt.consume();
    }

    @Override
    public void mouseClicked(MouseEvent evt)
    {
        // Nothing to do
        evt.consume();
    }

    @Override
    public void mouseEntered(MouseEvent evt)
    {
        // Nothing to do
    }

    @Override
    public void mouseExited(MouseEvent evt)
    {
        // Nothing to do
    }

    @Override
    public void mouseReleased(MouseEvent evt)
    {
        // Nothing to do
        evt.consume();
    }
}
