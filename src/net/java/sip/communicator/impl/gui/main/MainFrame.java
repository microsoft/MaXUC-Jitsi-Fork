/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import net.java.sip.communicator.impl.gui.DefaultContactEventHandler;
import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.impl.gui.event.PluginComponentEvent;
import net.java.sip.communicator.impl.gui.event.PluginComponentListener;
import net.java.sip.communicator.impl.gui.main.call.CallManager;
import net.java.sip.communicator.impl.gui.main.chat.conference.ConferenceChatManager;
import net.java.sip.communicator.impl.gui.main.contactlist.ContactListPane;
import net.java.sip.communicator.impl.gui.main.contactlist.TreeContactList;
import net.java.sip.communicator.plugin.desktoputil.LegacyErrorDialog;
import net.java.sip.communicator.plugin.desktoputil.lookandfeel.SearchField;
import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.analytics.AnalyticsParameter;
import net.java.sip.communicator.service.browserlauncher.BrowserLauncherService;
import net.java.sip.communicator.service.commportal.CPCos;
import net.java.sip.communicator.service.commportal.CPCosGetterCallback;
import net.java.sip.communicator.service.commportal.ClassOfServiceService;
import net.java.sip.communicator.service.contacteventhandler.ContactEventHandler;
import net.java.sip.communicator.service.gui.ContactListContainer;
import net.java.sip.communicator.service.gui.UIService.Reformatting;
import net.java.sip.communicator.service.netaddr.NetworkAddressManagerService;
import net.java.sip.communicator.service.netaddr.NetworkAddressManagerService.BSSIDAvailability;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.OperationSet;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
import net.java.sip.communicator.service.protocol.OperationSetBasicTelephony;
import net.java.sip.communicator.service.protocol.OperationSetFileTransfer;
import net.java.sip.communicator.service.protocol.OperationSetMultiUserChat;
import net.java.sip.communicator.service.protocol.OperationSetPersistentPresence;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.CallListener;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.PrivacyUtils;
import net.java.sip.communicator.util.ServiceUtils;
import net.java.sip.communicator.util.skin.Skinnable;
import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.util.OSUtils;
import org.jitsi.util.SanitiseUtils;

/**
 * The main application window. This class is the core of this UI
 * implementation. It stores all available protocol providers and their
 * operation sets, as well as all registered accounts, the
 * <tt>MetaContactListService</tt> and all sent messages that aren't
 * delivered yet.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Adam Netocny
 */
public class MainFrame
    extends AbstractMainFrame
    implements  ContactListContainer,
                PluginComponentListener,
                Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The logger.
     */
    private final Logger logger = Logger.getLogger(MainFrame.class);

    /**
     * The search field shown above the contact list.
     */
    private SearchField searchField;

    /**
     * A mapping of <tt>ProtocolProviderService</tt>s and their indexes.
     */
    private final HashMap<ProtocolProviderService, Integer> protocolProviders
        = new LinkedHashMap<>();

    /**
     * A mapping of <tt>ProtocolProviderService</tt>s and corresponding
     * <tt>ContactEventHandler</tt>s.
     */
    private final Map<ProtocolProviderService, ContactEventHandler>
        providerContactHandlers =
            new Hashtable<>();

    /**
     * The container containing the contact list.
     */
    private ContactListPane contactListPanel;

    /**
     * The contact list
     */
    private TreeContactList contactList;

    /**
     * The user interface call listener.
     */
    private CallListener uiCallListener;

    /**
     * The start colour used for the background of the main frame header
     */
    public static final Color HEADER_BACKGROUND = new Color(
        GuiActivator.getResources().getColor(
            "service.gui.MAIN_FRAME_HEADER_BACKGROUND_OVERRIDE",
            GuiActivator.getResources().getColor("service.gui.LIGHT_BACKGROUND"),
            false)
        );

    /**
     * The end colour used for the background of the main frame header
     */
    public static final Color HEADER_BACKGROUND_GRADIENT = new Color(
        GuiActivator.getResources().getColor(
            "service.gui.MAIN_FRAME_HEADER_BACKGROUND_GRADIENT_OVERRIDE",
            GuiActivator.getResources().getColor(
                "service.gui.LIGHT_BACKGROUND"),
            false)
        );

    // We record the last values of these so we spot when they change
    private BSSIDAvailability mLastBssidAvailability = BSSIDAvailability.UNKNOWN;
    private boolean lastUseLocation = false;

    // We want to show the permissions dialog the first time it is needed
    private boolean showPermissionsDialog = true;

    /**
     * The prefix of all keys in the config that relate to jabber accounts.
     */
    private static final String JABBER_ACC_CONFIG_PREFIX =
        "net.java.sip.communicator.impl.protocol.jabber";

    /**
     * The suffix to account config strings that indicate whether the
     * configured account is disabled.
     */
    private static final String ACCOUNT_DISABLED_SUFFIX = ".IS_ACCOUNT_DISABLED";

    /**
     * The prefix of the config string used by the reconnect plugin to track
     * whether accounts are online.
     */
    private static final String RECONNECTPLUGIN_PREFIX =
        "net.java.sip.communicator.plugin.reconnectplugin." +
            "ATLEAST_ONE_SUCCESSFUL_CONNECTION.";

    /**
     * Creates an instance of <tt>MainFrame</tt>.
     */
    public MainFrame()
    {
        super(HEADER_BACKGROUND, HEADER_BACKGROUND_GRADIENT, true);

        contactListPanel = new ContactListPane(this);
        contactList = contactListPanel.getContactList();

        // Listen for changes to whether the chat account has gone
        // online/offline or has been enabled/disabled by the user.
        configService.user().addPropertyChangeListener(new PropertyChangeListener()
        {
            public void propertyChange(PropertyChangeEvent e)
            {
                String propertyName = e.getPropertyName();

                boolean connectionChanged =
                    propertyName.startsWith(RECONNECTPLUGIN_PREFIX + "Jabber");

                boolean userChanged =
                    propertyName.startsWith(JABBER_ACC_CONFIG_PREFIX) &&
                    propertyName.endsWith(ACCOUNT_DISABLED_SUFFIX);

                if (connectionChanged || userChanged)
                {
                    logger.debug("IM account connectivity has changed");

                    String oldValue = (String) e.getOldValue();
                    String newValue = (String) e.getNewValue();

                    // If the value has changed or the returned values are
                    // null so we don't know whether they have changed, update
                    // the chat offline icon.
                    if (oldValue == null || !oldValue.equals(newValue))
                    {
                        String loggablePropertyName = connectionChanged ?
                                                      SanitiseUtils.sanitise(propertyName, PrivacyUtils.SUCCESSFUL_JABBER_CONNECTION) :
                                                      propertyName;
                        logger.debug(loggablePropertyName + " has changed from " +
                            oldValue + " to " + newValue +
                            ". Updating chat offline icon.");

                        // Send an analytics event to log the change.
                        AnalyticsEventType event = connectionChanged ?
                            AnalyticsEventType.IM_CONNECTED :
                            AnalyticsEventType.IM_SIGNED_IN;

                        GuiActivator.getAnalyticsService().onEvent(
                            event , "newValue", newValue);
                        GuiActivator.getAccountManager().refreshAccountInfo();
                    }
                }
            }
        });

        this.watchCos();
    }

    /**
     * Monitors the Class of Service to trigger events from CoS changes.
     */
    private void watchCos()
    {
        ClassOfServiceService cos = GuiActivator.getCosService();
        cos.getClassOfService(new CPCosGetterCallback()
        {
            @Override
            public void onCosReceived(CPCos cos)
            {
                // Must be run on the EDT thread
                if (!SwingUtilities.isEventDispatchThread())
                {
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            onCosReceived(cos);
                        }
                    });
                    return;
                }

                // Check if we need to ask the user for location permissions
                getAnyRequiredLocationPermission();
            }
        });
    }

    /**
     * Trigger any currently required location permission checks.
     */
    private void getAnyRequiredLocationPermission()
    {
        if (!OSUtils.IS_MAC)
        {
            return;
        }

        logger.info("Starting timer to check location permissions");
        TimerTask timerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                checkCurrentPermissionsState();
            }
        };

        Timer timer = new Timer("Permissions polling");
        long scheduleRate = 5000;
        timer.scheduleAtFixedRate(timerTask, scheduleRate, scheduleRate);
    }

    /**
     * This is called at regular intervals, so should do minimal logging to
     * avoid spam.
     * Both the isEmergencyLocationSupportNeeded can change (because we
     * previously did not have the data, but it has now arrived), and the
     * user may update the location permission via system preferences.
     */
    private void checkCurrentPermissionsState()
    {
        boolean useLocation =
                GuiActivator.getCommPortalService().isEmergencyLocationSupportNeeded();

        // We only want to log changes, not every time we check
        if (useLocation != lastUseLocation)
        {
            logger.info("Emergency Location support " + (useLocation ?  "" : "not ") + "in use");
            lastUseLocation = useLocation;
        }

        if (!useLocation)
        {
            return;
        }

        // We do need to support the emergency location feature, so we will
        // be accessing location information
        NetworkAddressManagerService networkAddressService =
                ServiceUtils.getService(GuiActivator.bundleContext,
                                        NetworkAddressManagerService.class);

        // See if we can already get hold of the BSSID information
        BSSIDAvailability bssidAvailability = networkAddressService != null ?
            networkAddressService.getBSSIDAvailability() : BSSIDAvailability.UNKNOWN;

        // Report any change to the permissions
        if (bssidAvailability != mLastBssidAvailability)
        {
            logger.info("BSSID availability changed to " + bssidAvailability.name());

            String reported =
                    bssidAvailability == BSSIDAvailability.AVAILABLE ? AnalyticsParameter.VALUE_PERMISSION_ACTION_TYPE_GRANTED :
                            AnalyticsParameter.VALUE_PERMISSION_ACTION_TYPE_DENIED;
            GuiActivator
                    .getAnalyticsService()
                    .onEvent(AnalyticsEventType.PERMISSION_ACTION,
                                AnalyticsParameter.PARAM_PERMISSION_ACTION_PERMISSION_NAME,
                                AnalyticsParameter.VALUE_PERMISSION_LOCATION,
                                AnalyticsParameter.PARAM_PERMISSION_ACTION_TYPE,
                                reported);

        }

        ResourceManagementService resources = GuiActivator.getResources();

        String textRes  = null;
        String actionTextRes = null;
        if (bssidAvailability == BSSIDAvailability.UNKNOWN)
        {
            // We have not yet asked for permissions, but need to
            textRes = "plugin.sipregistrationnotifier.REQUEST_PERMISSION_DESCRIPTION";
            actionTextRes = "plugin.sipregistrationnotifier.REQUEST_PERMISSION";
        }
        else if (bssidAvailability != BSSIDAvailability.AVAILABLE)
        {
            // Permissions have been blocked
            textRes = "plugin.sipregistrationnotifier.GO_TO_PRIVACY_SETTINGS_DESCRIPTION";
            actionTextRes = "plugin.sipregistrationnotifier.GO_TO_PRIVACY_SETTINGS";
        }

        if (showPermissionsDialog && textRes != null)
        {
            PermissionsDialog dialog = new PermissionsDialog(
                    resources.getI18NString("plugin.sipregistrationnotifier.LOCATION_TITLE"),
                    resources.getI18NString(textRes),
                    resources.getI18NString(actionTextRes));

            if (bssidAvailability == BSSIDAvailability.UNKNOWN)
            {
                dialog.hideOKButton();

                dialog.addActionListener(e -> {
                    logger.user("Request location permission");
                    networkAddressService.requestLocationPermission();

                    // Send an analytic
                    GuiActivator
                            .getAnalyticsService()
                            .onEvent(AnalyticsEventType.PERMISSION_ACTION,
                                        AnalyticsParameter.PARAM_PERMISSION_ACTION_PERMISSION_NAME,
                                        AnalyticsParameter.VALUE_PERMISSION_LOCATION,
                                        AnalyticsParameter.PARAM_PERMISSION_ACTION_TYPE,
                                        AnalyticsParameter.VALUE_PERMISSION_ACTION_TYPE_REQUESTED);
                });
            }
            else
            {
                dialog.addActionListener(e -> {
                    logger.user("Open Privacy location services");

                    BrowserLauncherService browserLauncherService = ServiceUtils.getService(
                            GuiActivator.bundleContext, BrowserLauncherService.class);

                    browserLauncherService.openURL(
                            "x-apple.systempreferences:com.apple.preference.security?Privacy_LocationServices");
                });
            }

            // We only want to show a dialog one time
            showPermissionsDialog = false;
            mLastBssidAvailability = bssidAvailability;

            dialog.showDialog();
        }
        else
        {
            mLastBssidAvailability = bssidAvailability;
        }
    }

    private class PermissionsDialog extends LegacyErrorDialog
    {
        private static final long serialVersionUID = 1L;

        /**
         * @param title   Title for the pop-up
         * @param message Main body of text for the pop-up
         * @param actionText text for the action button
         */
        public PermissionsDialog(String title, String message, String actionText)
        {
            super(GuiActivator.getUIService().getMainFrame(),
                  title, message, ErrorType.LOCATION);

            getRootPane().setDefaultButton(actionButton);
            actionButton.setText(actionText);
            actionButton.setVisible(true);
            actionButton.getParent().setLayout(new FlowLayout(FlowLayout.RIGHT));
            actionButton.addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            JButton button = (JButton) e.getSource();
            if (button.equals(okButton))
            {
                super.actionPerformed(e);
            }
            else
            {
                logger.user("Clicked action button " + button.getText());
                this.dispose();
            }
        }

        public void hideOKButton()
        {
            okButton.setVisible(false);
        }

        public void addActionListener(ActionListener listener)
        {
            actionButton.addActionListener(listener);
        }
    }

    /**
     * Adds all protocol supported operation sets.
     *
     * @param protocolProvider The protocol provider.
     */
    public void addProtocolSupportedOperationSets(
            ProtocolProviderService protocolProvider)
    {
        Map<String, OperationSet> supportedOperationSets
            = protocolProvider.getSupportedOperationSets();

        String ppOpSetClassName = OperationSetPersistentPresence
                                    .class.getName();
        String pOpSetClassName = OperationSetPresence.class.getName();

        // Obtain the presence operation set.
        if (supportedOperationSets.containsKey(ppOpSetClassName)
                || supportedOperationSets.containsKey(pOpSetClassName))
        {
            OperationSetPresence presence = (OperationSetPresence)
                supportedOperationSets.get(ppOpSetClassName);

            if(presence == null) {
                presence = (OperationSetPresence)
                    supportedOperationSets.get(pOpSetClassName);
            }

            presence.addContactPresenceStatusListener(
                contactList.getMetaContactListSource());
            presence.addContactPresenceStatusListener(
                GuiActivator.getGlobalStatusService());
        }

        // Obtain the basic instant messaging operation set.
        String imOpSetClassName = OperationSetBasicInstantMessaging
                                    .class.getName();

        if (supportedOperationSets.containsKey(imOpSetClassName))
        {
            OperationSetBasicInstantMessaging im
                = (OperationSetBasicInstantMessaging)
                    supportedOperationSets.get(imOpSetClassName);

            //Add to all instant messaging operation sets the Message
            //listener implemented in the ContactListPanel, which handles
            //all received messages.
            im.addMessageListener(getContactListPanel());
        }

        // Obtain the typing notifications operation set.
        String tnOpSetClassName = OperationSetTypingNotifications
                                    .class.getName();

        if (supportedOperationSets.containsKey(tnOpSetClassName))
        {
            OperationSetTypingNotifications tn
                = (OperationSetTypingNotifications)
                    supportedOperationSets.get(tnOpSetClassName);

            //Add to all typing notification operation sets the Message
            //listener implemented in the ContactListPanel, which handles
            //all received messages.
            tn.addTypingNotificationsListener(this.getContactListPanel());
        }

        // Obtain the basic telephony operation set.
        String telOpSetClassName = OperationSetBasicTelephony.class.getName();

        if (supportedOperationSets.containsKey(telOpSetClassName))
        {
            OperationSetBasicTelephony<?> telephony
                = (OperationSetBasicTelephony<?>)
                    supportedOperationSets.get(telOpSetClassName);

            uiCallListener = new CallManager.GuiCallListener();

            telephony.addCallListener(uiCallListener);
        }

        // Obtain the multi user chat operation set.
        String multiChatClassName = OperationSetMultiUserChat.class.getName();

        if (supportedOperationSets.containsKey(multiChatClassName))
        {
            OperationSetMultiUserChat multiUserChat
                = (OperationSetMultiUserChat)
                    supportedOperationSets.get(multiChatClassName);

            ConferenceChatManager conferenceManager
                = GuiActivator.getUIService().getConferenceChatManager();

            multiUserChat.addInvitationListener(conferenceManager);
            multiUserChat.addInvitationRejectionListener(conferenceManager);
            multiUserChat.addPresenceListener(conferenceManager);
        }

        // Obtain file transfer operation set.
        OperationSetFileTransfer fileTransferOpSet
            = protocolProvider.getOperationSet(OperationSetFileTransfer.class);

        if (fileTransferOpSet != null)
        {
            fileTransferOpSet.addFileTransferListener(getContactListPanel());
        }
    }

    /**
     * Removes all protocol supported operation sets.
     *
     * @param protocolProvider The protocol provider.
     */
    public void removeProtocolSupportedOperationSets(
            ProtocolProviderService protocolProvider)
    {
        Map<String, OperationSet> supportedOperationSets
            = protocolProvider.getSupportedOperationSets();

        String ppOpSetClassName = OperationSetPersistentPresence
                                    .class.getName();
        String pOpSetClassName = OperationSetPresence.class.getName();

        // Obtain the presence operation set.
        if (supportedOperationSets.containsKey(ppOpSetClassName)
                || supportedOperationSets.containsKey(pOpSetClassName))
        {
            OperationSetPresence presence = (OperationSetPresence)
                supportedOperationSets.get(ppOpSetClassName);

            if(presence == null)
            {
                presence = (OperationSetPresence)
                    supportedOperationSets.get(pOpSetClassName);
            }

            presence.removeContactPresenceStatusListener(
                contactList.getMetaContactListSource());
        }

        // Obtain the basic instant messaging operation set.
        String imOpSetClassName = OperationSetBasicInstantMessaging
                                    .class.getName();

        if (supportedOperationSets.containsKey(imOpSetClassName))
        {
            OperationSetBasicInstantMessaging im
                = (OperationSetBasicInstantMessaging)
                    supportedOperationSets.get(imOpSetClassName);

            im.removeMessageListener(getContactListPanel());
        }

        // Obtain the typing notifications operation set.
        String tnOpSetClassName = OperationSetTypingNotifications
                                    .class.getName();

        if (supportedOperationSets.containsKey(tnOpSetClassName))
        {
            OperationSetTypingNotifications tn
                = (OperationSetTypingNotifications)
                    supportedOperationSets.get(tnOpSetClassName);

            //Add to all typing notification operation sets the Message
            //listener implemented in the ContactListPanel, which handles
            //all received messages.
            tn.removeTypingNotificationsListener(this.getContactListPanel());
        }

        // Obtain the basic telephony operation set.
        String telOpSetClassName = OperationSetBasicTelephony.class.getName();

        if (supportedOperationSets.containsKey(telOpSetClassName))
        {
            OperationSetBasicTelephony<?> telephony
                = (OperationSetBasicTelephony<?>)
                    supportedOperationSets.get(telOpSetClassName);

            if (uiCallListener != null)
                telephony.removeCallListener(uiCallListener);
        }

        // Obtain the multi user chat operation set.
        String multiChatClassName = OperationSetMultiUserChat.class.getName();

        if (supportedOperationSets.containsKey(multiChatClassName))
        {
            OperationSetMultiUserChat multiUserChat
                = (OperationSetMultiUserChat)
                    supportedOperationSets.get(multiChatClassName);

            ConferenceChatManager conferenceManager
                = GuiActivator.getUIService().getConferenceChatManager();

            multiUserChat.removeInvitationListener(conferenceManager);
            multiUserChat.removeInvitationRejectionListener(conferenceManager);
            multiUserChat.removePresenceListener(conferenceManager);
        }

        // Obtain file transfer operation set.
        OperationSetFileTransfer fileTransferOpSet
            = protocolProvider.getOperationSet(OperationSetFileTransfer.class);

        if (fileTransferOpSet != null)
        {
            fileTransferOpSet.removeFileTransferListener(getContactListPanel());
        }
    }

    /**
     * Returns a set of all protocol providers.
     *
     * @return a set of all protocol providers.
     */
    public Iterator<ProtocolProviderService> getProtocolProviders()
    {
        return new LinkedList<>(
                protocolProviders.keySet())
                        .iterator();
    }

    /**
     * Adds a protocol provider.
     * @param protocolProvider The protocol provider to add.
     */
    public void addProtocolProvider(ProtocolProviderService protocolProvider)
    {
        synchronized(this.protocolProviders)
        {
            if(this.protocolProviders.containsKey(protocolProvider))
                return;

            this.protocolProviders.put(protocolProvider,
                    initiateProviderIndex(protocolProvider));
        }

        logger.info("Add the following protocol provider to the gui: "
                     + protocolProvider.getAccountID().getLoggableAccountID()
                     + " , hashcode: " + Integer.toHexString(hashCode()));

        this.addProtocolSupportedOperationSets(protocolProvider);

        ContactEventHandler contactHandler
            = this.getContactHandlerForProvider(protocolProvider);

        if (contactHandler == null)
            contactHandler = new DefaultContactEventHandler();

        this.addProviderContactHandler(protocolProvider, contactHandler);
        GuiActivator.getGlobalStatusService().addProvider(protocolProvider);
    }

    /**
     * Checks whether we have already loaded the protocol provider.
     * @param protocolProvider the provider to check.
     * @return whether we have already loaded the specified provider.
     */
    public boolean hasProtocolProvider(
        ProtocolProviderService protocolProvider)
    {
        synchronized(this.protocolProviders)
        {
            return this.protocolProviders.containsKey(protocolProvider);
        }
    }

    /**
     * Removes an account from the application.
     *
     * @param protocolProvider The protocol provider of the account.
     */
    public void removeProtocolProvider(ProtocolProviderService protocolProvider)
    {
        logger.info("Remove the following protocol provider from the gui: "
                    + protocolProvider.getAccountID().getLoggableAccountID()
                    + " , hashcode: " + protocolProvider.hashCode());

        synchronized(this.protocolProviders)
        {
            this.protocolProviders.remove(protocolProvider);
        }

        this.removeProtocolSupportedOperationSets(protocolProvider);

        removeProviderContactHandler(protocolProvider);

        this.updateProvidersIndexes(protocolProvider);

        GuiActivator.getGlobalStatusService().removeProvider(protocolProvider);
    }

    /**
     * Removes a protocol provider of a given account.
     *
     * @param accountID The unique account ID.
     */
    public void removeProtocolProvider(AccountID accountID)
    {
        logger.info("Attempt to remove protocol provider with account ID "
                    + (accountID != null ? accountID.getLoggableAccountID() : "null")
                    + " from the gui");

        ProtocolProviderService providerToRemove = null;

        for (ProtocolProviderService pp : this.protocolProviders.keySet())
        {
            // Find protocol provider for the account with the given ID
            if (pp.getAccountID() == accountID)
            {
                providerToRemove = pp;
                break;
            }
        }

        if (providerToRemove != null)
        {
            removeProtocolProvider(providerToRemove);
        }
    }

    /**
     * Returns the index of the given protocol provider.
     * @param protocolProvider the protocol provider to search for
     * @return the index of the given protocol provider
     */
    public int getProviderIndex(ProtocolProviderService protocolProvider)
    {
        Integer o = protocolProviders.get(protocolProvider);

        return (o != null) ? o : 0;
    }

    /**
     * Returns the account user id for the given protocol provider.
     * @param protocolProvider the protocol provider corresponding to the
     * account to add
     * @return The account user id for the given protocol provider.
     */
    public String getAccountAddress(ProtocolProviderService protocolProvider)
    {
        return protocolProvider.getAccountID().getAccountAddress();
    }

    /**
     * Returns the multi user chat operation set for the given protocol provider.
     *
     * @param protocolProvider The protocol provider for which the multi user
     * chat operation set is about.
     * @return OperationSetMultiUserChat The telephony operation
     * set for the given protocol provider.
     */
    public OperationSetMultiUserChat getMultiUserChatOpSet(
            ProtocolProviderService protocolProvider)
    {
        OperationSet opSet
            = protocolProvider.getOperationSet(OperationSetMultiUserChat.class);

        return (opSet instanceof OperationSetMultiUserChat)
            ? (OperationSetMultiUserChat) opSet
            : null;
    }

    /**
     * Returns the panel containing the ContactList.
     * @return ContactListPanel the panel containing the ContactList
     */
    public ContactListPane getContactListPanel()
    {
        return this.contactListPanel;
    }

    /**
     * Returns the valid phone number currently shown in the search field.
     * @return the valid phone number currently shown in the search field, or
     * null if there isn't one
     */
    public String getCurrentSearchPhoneNumber()
    {
        return searchField.getPhoneNumber();
    }

    /**
     * Returns the text currently shown in the search field.
     * @return the text currently shown in the search field
     */
    public String getCurrentSearchText()
    {
        return searchField.getText();
    }

    /**
     * Clears the current text in the search field.
     */
    public void clearCurrentSearchText()
    {
        searchField.setText("");
    }

    /**
     * Checks in the configuration xml if there is already stored index for
     * this provider and if yes, returns it, otherwise creates a new account
     * index and stores it.
     *
     * @param protocolProvider the protocol provider
     * @return the protocol provider index
     */
    private int initiateProviderIndex(
            ProtocolProviderService protocolProvider)
    {
        String prefix = "net.java.sip.communicator.impl.gui.accounts";

        List<String> accounts = configService.user()
                .getPropertyNamesByPrefix(prefix, true);

        for (String accountRootPropName : accounts) {
            String accountUID
                = configService.user().getString(accountRootPropName);

            if(accountUID.equals(protocolProvider
                    .getAccountID().getAccountUniqueID()))
            {
                String  index = configService.user().getString(
                        accountRootPropName + ".accountIndex");

                if(index != null) {
                    //if we have found the accountIndex for this protocol provider
                    //return this index
                    return Integer.parseInt(index);
                }
                else
                {
                    //if there's no stored accountIndex for this protocol
                    //provider, calculate the index, set it in the configuration
                    //service and return it.

                    return createAccountIndex(protocolProvider,
                            accountRootPropName);
                }
            }
        }

        String accNodeName = "acc" + System.currentTimeMillis();

        String accountPackage
            = "net.java.sip.communicator.impl.gui.accounts."
                    + accNodeName;

        configService.user().setProperty(accountPackage,
                protocolProvider.getAccountID().getAccountUniqueID());

        return createAccountIndex(protocolProvider,
                accountPackage);
    }

    /**
     * Creates and calculates the account index for the given protocol
     * provider.
     * @param protocolProvider the protocol provider
     * @param accountRootPropName the path to where the index should be saved
     * in the configuration xml
     * @return the created index
     */
    private int createAccountIndex(ProtocolProviderService protocolProvider,
            String accountRootPropName)
    {
        int accountIndex = -1;

        for (ProtocolProviderService pps : protocolProviders.keySet())
        {
            if (pps.getProtocolDisplayName().equals(
                protocolProvider.getProtocolDisplayName())
                && !pps.equals(protocolProvider))
            {
                int index = protocolProviders.get(pps);

                if (accountIndex < index)
                    accountIndex = index;
            }
        }
        accountIndex++;
        configService.user().setProperty(
                accountRootPropName + ".accountIndex",
                accountIndex);

        return accountIndex;
    }

    /**
     * Updates the indexes in the configuration xml, when a provider has been
     * removed.
     * @param removedProvider the removed protocol provider
     */
    private void updateProvidersIndexes(ProtocolProviderService removedProvider)
    {
        String prefix = "net.java.sip.communicator.impl.gui.accounts";

        ProtocolProviderService currentProvider = null;
        int sameProtocolProvidersCount = 0;

        for (ProtocolProviderService pps : protocolProviders.keySet()) {
            if(pps.getProtocolDisplayName().equals(
                    removedProvider.getProtocolDisplayName())) {

                sameProtocolProvidersCount++;
                if(sameProtocolProvidersCount > 1) {
                    break;
                }
                currentProvider = pps;
            }
        }

        if(sameProtocolProvidersCount < 2 && currentProvider != null) {
            protocolProviders.put(currentProvider, 0);

            List<String> accounts = configService.user()
                .getPropertyNamesByPrefix(prefix, true);

            for (String rootPropName : accounts) {
                String accountUID
                    = configService.user().getString(rootPropName);

                if(accountUID.equals(currentProvider
                        .getAccountID().getAccountUniqueID())) {

                    configService.user().setProperty(
                            rootPropName + ".accountIndex",
                            0);
                }
            }
        }
    }

    /**
     * Overwrites the <tt>SIPCommFrame</tt> close method. This method is
     * invoked when user presses the Escape key.
     * @param isEscaped indicates if this window has been closed by pressing
     * the escape key
     */
    @Override
    protected void close(boolean isEscaped)
    {
        Component contactListRightMenu = contactList.getRightButtonMenu();

        if (contactListRightMenu != null && contactListRightMenu.isVisible())
        {
            contactListRightMenu.setVisible(false);
        }
    }

    /**
     * Adds the given <tt>contactHandler</tt> to handle contact events for the
     * given <tt>protocolProvider</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, which
     * contacts should be handled by the given <tt>contactHandler</tt>
     * @param contactHandler the <tt>ContactEventHandler</tt> that would handle
     * events coming from the UI for any contacts belonging to the given
     * provider
     */
    public void addProviderContactHandler(
        ProtocolProviderService protocolProvider,
        ContactEventHandler contactHandler)
    {
        providerContactHandlers.put(protocolProvider, contactHandler);
    }

    /**
     * Removes the <tt>ContactEventHandler</tt> corresponding to the given
     * <tt>protocolProvider</tt>.
     *
     * @param protocolProvider the protocol provider, which contact handler
     * we would like to remove
     */
    public void removeProviderContactHandler(
        ProtocolProviderService protocolProvider)
    {
        providerContactHandlers.remove(protocolProvider);
    }

    /**
     * Returns the <tt>ContactEventHandler</tt> for contacts given by the
     * <tt>protocolProvider</tt>. The <tt>ContactEventHandler</tt> is meant to
     * be used from other bundles in order to change the default behavior of
     * events generated when clicking a contact.
     * @param protocolProvider the protocol provider for which we want to obtain
     * a contact event handler
     * @return the <tt>ContactEventHandler</tt> for contacts given by the
     * <tt>protocolProvider</tt>
     */
    private ContactEventHandler getContactHandlerForProvider(
        ProtocolProviderService protocolProvider)
    {
        ServiceReference<?>[] serRefs = null;

        String osgiFilter = "("
            + ProtocolProviderFactory.PROTOCOL
            + "=" + protocolProvider.getProtocolName()+")";

        try
        {
            serRefs = GuiActivator.bundleContext.getServiceReferences(
                ContactEventHandler.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex){
            logger.error("GuiActivator : " + ex);
        }

        if(serRefs == null)
            return null;

        return (ContactEventHandler) GuiActivator.bundleContext
            .getService(serRefs[0]);
    }

    /**
     * Adds the associated with this <tt>PluginComponentEvent</tt> component to
     * the appropriate container.
     * @param event the <tt>PluginComponentEvent</tt> that has notified us of
     * the add of a plugin component
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
    }

    /**
     * Removes the associated with this <tt>PluginComponentEvent</tt> component
     * from this container.
     * @param event the <tt>PluginComponentEvent</tt> that notified us of the
     * remove of a plugin component
     */
    public void pluginComponentRemoved(PluginComponentEvent event)
    {
    }

    /**
     * Called when the ENTER key was typed when this window was the focused
     * window. Performs the appropriate actions depending on what item had the focus,
     * and the current state of the contact list.
     * @param evt The KeyEvent of the ENTER key
     */
    public void enterKeyTyped(KeyEvent evt)
    {
        Component focus = getFocusOwner();
        logger.user("ENTER pressed in main window, with focus on " + focus.getClass().getSimpleName());

        if (focus instanceof JButton)
        {
            // For accessibility, enter on a button should activate that button
            ((JButton)focus).doClick();
        }
        else if (focus instanceof JLabel)
        {
            // Dispatch the key to the label if it has a listener
            KeyListener[] listeners = focus.getKeyListeners();
            if (listeners.length > 0)
            {
                listeners[0].keyTyped(evt);
            }
        }
        // Check whether pressing 'enter' is meant to start a call.
        else if (GuiActivator.enterDialsNumber())
        {
            // Get the phone number from the search box.
            String number = getCurrentSearchPhoneNumber();

            if ((contactListPanel.isVisible()) &&
                (contactList.getSelectedContact() != null))
            {
                // Make the contact list handle the key press.
                contactList.enterKeyTyped();
            }
            else if ((number != null) &&
                (ConfigurationUtils.isCallingEnabled()))
            {
                // The search box contains a valid phone number, so call it
                // (and make an analytics event).
                GuiActivator.getAnalyticsService().onEvent(AnalyticsEventType.OUTBOUND_CALL,
                                                           "Calling from",
                                                           "Search field");
                CallManager.createCall(number, Reformatting.NOT_NEEDED);
                clearCurrentSearchText();
            }
            else if (contactListPanel.isVisible() &&
                     (contactList.getSelectedGroup() == null))
            {
                // No contact or group is selected, and the number cannot be
                // called (this is probably because the user has entered a
                // name or IM address) so select the first contact and give
                // focus to the contact list.
                contactList.selectFirstContact();
                contactList.getComponent().requestFocus();
            }
        }
        else if (contactListPanel.isVisible())
        {
            // Make the contact list handle the key press.
            contactList.enterKeyTyped();
        }
    }

    /**
     * Called when the CTRL-ENTER or CMD-ENTER keys were typed when this window
     * was the focused window. Performs the appropriate actions depending on the
     * current state of the contact list.
     */
    public void ctrlEnterKeyTyped()
    {
        logger.user("CTRL-ENTER or CMD-ENTER pressed in main window");

        if (contactListPanel.isVisible())
        {
            // Make the contact list handle the key press.
            contactList.ctrlEnterKeyTyped();
        }
    }

    /**
     * Reloads skin information
     */
    public void loadSkin()
    {
    }

    @Override
    protected Dimension getMinimumBoundsSize()
    {
        int width = GuiActivator.getResources()
            .getScaledSize("impl.gui.MAIN_WINDOW_MIN_WIDTH");

        int height = GuiActivator.getResources()
            .getScaledSize("impl.gui.MAIN_WINDOW_MIN_HEIGHT");

        return new Dimension(width, height);

    }

    @Override
    protected Dimension getPreferredBoundsSize()
    {
        int width = GuiActivator.getResources()
            .getScaledSize("impl.gui.MAIN_WINDOW_WIDTH");

        int height = GuiActivator.getResources()
            .getScaledSize("impl.gui.MAIN_WINDOW_HEIGHT");

        return new Dimension(width, height);
    }

    @Override
    public void setDefaultFont(Font font)
    {
        JComponent layeredPane = getLayeredPane();

        for (int i = 0; i < layeredPane.getComponentCount(); i++)
        {
            layeredPane.getComponent(i).setFont(font);
        }
    }
}
