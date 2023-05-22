/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.generalconfig;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import net.java.sip.communicator.plugin.desktoputil.ConfigSectionPanel;
import net.java.sip.communicator.plugin.desktoputil.CreateConferenceMenu;
import net.java.sip.communicator.plugin.desktoputil.ErrorDialog;
import net.java.sip.communicator.plugin.desktoputil.OptionListConfigurationPanel;
import net.java.sip.communicator.plugin.desktoputil.SIPCommBasicTextButton;
import net.java.sip.communicator.plugin.desktoputil.SIPCommCheckBox;
import net.java.sip.communicator.plugin.desktoputil.ScaleUtils;
import net.java.sip.communicator.plugin.desktoputil.TransparentPanel;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.AccountManager;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.ProtocolNames;
import net.java.sip.communicator.service.reset.ResetService;
import net.java.sip.communicator.service.systray.PopupMessageHandler;
import net.java.sip.communicator.util.AccessibilityUtils;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.FileUtils;
import net.java.sip.communicator.util.Logger;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.neomedia.Recorder;
import org.jitsi.service.resources.ImageIconFuture;
import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.util.OSUtils;

/**
 * The general configuration form.
 *
 * @author Yana Stamcheva
 */
public class GeneralConfigurationPanel
    extends OptionListConfigurationPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>Logger</tt> used by this <tt>GeneralConfigurationPanel</tt> for
     * logging output.
     */
    private static final Logger logger
        = Logger.getLogger(GeneralConfigurationPanel.class);

    /**
     * Indicates if the Ringtones configuration panel should be disabled, i.e.
     * not visible to the user.
     */
    private static final String RINGTONES_CONFIG_DISABLED_PROP =
      "net.java.sip.communicator.plugin.generalconfig.ringtonesconfig.DISABLED";

    /**
     * Prefix for ringtone resource keys.
     * For backwards-compatibility reasons, the first ringtone will be called
     * 'INCOMING_CALL', and subsequent ringtones will be called
     * 'INCOMING_CALL_[index]'.
     */
    private static final String RINGTONE_PATH_RESOURCE_PREFIX = "INCOMING_CALL";

    /**
     * Prefix for ringtone display name resource keys. These are used to provide
     * a user-visible name for the default packaged ringtones.
     */
    private static final String RINGTONE_NAME_RESOURCE_PREFIX =
                        "service.gui.RINGTONE_NAME_";

    /**
     * The button within the ringtones panel which is used to preview ringtones.
     * We hold a reference to it so that we can halt any playing audio when we
     * are closed or are no longer the active tab in the configuration panel.
     */
    private RingtonePreviewButton mPreviewButton;

    /**
     * Indicates if the device type configuration panel should be disabled, i.e.
     * not visible to the user.
     */
    private static final String DEVICE_TYPE_CONFIG_DISABLED_PROP
        =
        "net.java.sip.communicator.plugin.generalconfig.devicetype.DISABLED";

    /**
     * Indicates if the contact store configuration panel should be disabled,
     * i.e. not visible to the user.
     */
    private static final String CONTACT_STORE_CONFIG_ENABLED_PROP =
        "net.java.sip.communicator.DEFAULT_CONTACT_SOURCE_" +
        (OSUtils.IS_WINDOWS ? "WIN" : "MAC") +
        "_USER_OVERRIDE";

    /**
     * Indicates the name of the saved personal contact store.
     */
    private static final String PERSONAL_CONTACT_STORE_KEY =
        "net.java.sip.communicator.PERSONAL_CONTACT_STORE";

    /**
     * The root of resources keys for the contact store selector.
     */
    private static final String CONTACT_STORE_NAME_ROOT =
        "plugin.generalconfig.CONTACT_STORE_";

    /**
     * Indicates if the Notification configuration panel should be disabled,
     * i.e.  not visible to the user.
     */
    private static final String NOTIFICATION_CONFIG_DISABLED_PROP
        =
        "net.java.sip.communicator.plugin.generalconfig.notificationconfig.DISABLED";

    /**
     * Indicates if the Locale configuration panel should be disabled, i.e.
     * not visible to the user.
     */
    private static final String LOCALE_CONFIG_DISABLED_PROP
        =
        "net.java.sip.communicator.plugin.generalconfig.localeconfig.DISABLED";

    /**
     * Indicates if the privacy panel should be disabled, i.e. not visible to
     * the user.
     */
    private static final String PRIVACY_PROP =
        "net.java.sip.communicator.plugin.generalconfig.privacy.DISABLED";

    /**
     * Indicates whether we should be the default application for Outlook
     * integration (for calls, IM and presence).
     */
    private static final String DEFAULT_OUTLOOK_INTEGRATION_APP_PROP =
        "plugin.msofficecomm.DEFAULT_OUTLOOK_INTEGRATION_APP";

    /**
     * Indicates whether we should automatically log in when the application
     * starts (assuming we have valid log username and password).
     */
    private static final String DEFAULT_AUTO_LOG_IN =
        "net.java.sip.communicator.plugin.generalconfig.autologin.ENABLED";

    /**
     * The resource key for the loading spinner GIF.
     */
    private static final String LOADING_SPINNER_KEY =
                                                  "service.gui.LOADING_SPINNER";

    /**
     * Property that determines whether we display recordings UI or not.
     */
    private static final String CALL_RECORDING_DISABLED_PROP =
                   "net.java.sip.communicator.impl.gui.CALL_RECORDING_DISABLED";

    /**
     * The maximum width, in pixels, of the dropdown used to select the folder
     * to save recordings to.
     */
    private static final int CALL_RECORDING_SELECTOR_MAX_WIDTH =
                                                       ScaleUtils.scaleInt(150);

    /**
     * The configuration service
     */
    private static final ConfigurationService configService =
        GeneralConfigPluginActivator.getConfigurationService();

    /**
     * The account manager
     */
    private static final AccountManager accountManager =
        GeneralConfigPluginActivator.getAccountManager();

    /**
     * Holds the thread that is currently switching contact stores, or else the
     * last thread to do so.
     */
    private static Thread sContactStoreSwitchThread;

    /**
     * A panel that allows the user to configure the folder that recorded calls
     * and/or conferences are saved to.
     */
    private Component recordingsPanel;

    /**
     * A panel that allows the user to select the ringtone they want, or choose
     * a custom one.
     */
    private Component mRingtoneConfigPanel;

    /**
     * The panel with all of the general config displayed on it. This includes
     * ringtones, integration with Outlook and whether to launch the client on
     * startup, but not for example Contact settings or recording settings.
     */
    private Component mGeneralConfigPanel;

    /**
     * This tracks whether a user has just clicked on the dropdown ringtone
     * selector menu, and has not refocused on the config panel since. If this
     * is false, when we click on the general config panel, all of the data
     * contained inside is updated if necessary. If this is true, it is not.
     * This avoids the following scenario: we select a custom ringtone, which
     * automatically focuses on the config panel. This in turn checks, among
     * other things, if the custom ringtone is still valid. However it is
     * possible that not everything has been updated since choosing our custom
     * ringtone yet, so it would switch to the default ringtone, thinking that
     * the custom one was not valid. To avoid this race condition, we use this
     * atomic boolean.
     */
    static final AtomicBoolean mJustUpdatedRingtone = new AtomicBoolean(false);

    /**
     * Creates the general configuration panel.
     */
    public GeneralConfigurationPanel()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Add a new GeneralConfigPanel to the top.
        createOrUpdateGeneralConfigPanel();

        if (!configService.global().getBoolean(DEVICE_TYPE_CONFIG_DISABLED_PROP, false))
        {
            Component deviceTypeConfigPanel = createDeviceTypeConfigPanel();
            if (deviceTypeConfigPanel != null)
            {
                add(deviceTypeConfigPanel);
            }
        }

        if (!configService.user().getBoolean(NOTIFICATION_CONFIG_DISABLED_PROP, true))
        {
            Component notifConfigPanel = createNotificationConfigPanel();
            if (notifConfigPanel != null)
            {
                add(notifConfigPanel);
            }
        }

        if (!configService.user().getBoolean(LOCALE_CONFIG_DISABLED_PROP, true))
        {
            add(createLocaleConfigPanel());
        }

        Component contactStoreConfigPanel = createContactConfigPanel();
        if ((contactStoreConfigPanel != null))
        {
            add(contactStoreConfigPanel);
        }

        recordingsPanel = createRecordingsPanel();
        add(recordingsPanel);
        setRecordingsPanelVisibility();

        if (!configService.global().getBoolean(PRIVACY_PROP, false))
        {
            add(createPrivacyPanel());
        }

        add(createResetConfigPanel());
    }

    /**
     * Creates or updates the panel for the general section of the general
     * config panel
     */
    private synchronized void createOrUpdateGeneralConfigPanel()
    {
        if (mGeneralConfigPanel != null)
        {
            // We remove the old panel, then add the updated one at the end of
            // this function.
            remove(mGeneralConfigPanel);
        }
        JPanel generalConfigJPanel = new ConfigSectionPanel("service.gui.GENERAL");

        // No options available unless on Windows - so return null in that case
        if (OSUtils.IS_WINDOWS)
        {
            generalConfigJPanel.add(createAutoStartCheckBox());
            generalConfigJPanel.add(createAutomaticLogInCheckBox());

            // This option relates to either calls, or IM or both.
            if (ConfigurationUtils.isCallingOrImEnabled())
            {
                generalConfigJPanel.add(createDefaultOutlookIntegrationAppCheckBox());
            }

            // This option only makes sense if we actually have call function.
            if (ConfigurationUtils.isCallingEnabled())
            {
                generalConfigJPanel.add(createUrlProtocolHandlerAppCheckBox());
            }
        }

        // Ringtones are only used for incoming calls or incoming meeting invites (which
        // are IMs).
        boolean meetingInvitesEnabled =
                GeneralConfigPluginActivator.getConferenceService().isFullServiceEnabled() &&
                ConfigurationUtils.isImEnabled();

        if (!configService.global().getBoolean(RINGTONES_CONFIG_DISABLED_PROP, false) &&
            (ConfigurationUtils.isCallingEnabled() || meetingInvitesEnabled))
        {
            createOrUpdateRingtonesConfigPanel();

            // Then we add the ringtones panel if it is not null.
            if (mRingtoneConfigPanel != null)
            {
                generalConfigJPanel.add(mRingtoneConfigPanel);
            }
        }

        // Note, that an 'empty' ConfigSectionPanel still contains a title separator
        int numberOfComponents = generalConfigJPanel.getComponentCount();
        if (numberOfComponents <= 1)
        {
            logger.debug("Hiding empty general config panel");
            generalConfigJPanel = null;
        }

        mGeneralConfigPanel = generalConfigJPanel;

        if (mGeneralConfigPanel != null)
        {
            // We add the config panel at the top of the page.
            add(mGeneralConfigPanel, 0);
        }
    }

    /**
     * Creates or updates the panel used to select the current ringtone (which
     * is used both for incoming calls and meeting invites).
     */
    private void createOrUpdateRingtonesConfigPanel()
    {
        // The label text, dropdown selector, and preview button should all
        // appear on the same row, so we place them in a FlowLayout.
        JPanel selectorPanel = new TransparentPanel();
        selectorPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        // Create the label text for the row.
        JLabel ringtoneLabel = new JLabel();
        ringtoneLabel.setText(Resources.getResources().getI18NString(
                "service.gui.RINGTONE_SELECTOR_LABEL"));
        ringtoneLabel.setForeground(TEXT_COLOR);
        ScaleUtils.scaleFontAsDefault(ringtoneLabel);

        // We don't want to create a new preview button when we update the panel
        // if one already exists. If we did this, we would not stop playing the
        // audio when the window lost focus.
        if (mPreviewButton == null)
        {
            // Create the button used to preview audio.
            mPreviewButton = new RingtonePreviewButton();
        }

        // Create and populate the dropdown selector from which ringtones may be
        // selected. We need only provide the default ringtones here, as the
        // selector will handle custom ringtones itself.
        Map<String, String> defaultRingtones = getDefaultRingtones();
        RingtoneSelectorComboBox ringtoneSelector =
                new RingtoneSelectorComboBox(defaultRingtones,
                                             mPreviewButton);

        // The preview button needs to hold a reference to the ringtone selector,
        // so we pass this now.
        mPreviewButton.registerRingtoneSelector(ringtoneSelector);

        selectorPanel.add(ringtoneLabel);
        selectorPanel.add(ringtoneSelector);
        selectorPanel.add(mPreviewButton);

        mRingtoneConfigPanel=selectorPanel;
    }

    /**
     * Obtains from config all ringtones that are packaged with the client,
     * and returns them.
     *
     * @return The default ringtones.
     */
    private Map<String, String> getDefaultRingtones()
    {
        // Use a sorted map (TreeMap) so that ringtones are listed
        // alphabetically by name.
        Map<String, String> results = new TreeMap<>();
        String defaultString = "Default";
        int idx = 0;
        ResourceManagementService resources = Resources.getResources();
        while (true)
        {
            idx++;

            String pathKey = (idx==1) ? RINGTONE_PATH_RESOURCE_PREFIX
                                    : RINGTONE_PATH_RESOURCE_PREFIX + "_" + idx;
            String path = resources.getSoundPath(pathKey);

            if (path == null)
            {
                // We don't have a resource at this index, so we've enumerated
                // all the ringtones.
                break;
            }

            String ringtoneNameProp = RINGTONE_NAME_RESOURCE_PREFIX + idx;
            String defaultName = (idx==1) ? defaultString :
                                                      defaultString + " " + idx;
            String ringtoneName = resources.getI18NString(ringtoneNameProp);
            ringtoneName = (ringtoneName == null) ? defaultName : ringtoneName;

            results.put(ringtoneName, path);
        }

        return results;
    }

    /**
     * Creates the checkbox for whether we should be the default application
     * for Outlook integration (for calls, IM and presence).
     *
     * @return the default Outlook integration application check box.
     */
    private Component createDefaultOutlookIntegrationAppCheckBox()
    {
        // Note, if neither IM nor calls are available, we will not show this
        // checkbox at all, so we only handle 3/4 cases here.
        String checkboxLabel = ConfigurationUtils.isImEnabled() ?
            "plugin.generalconfig.DEFAULT_OUTLOOK_INTEGRATION_APP" :
            "plugin.generalconfig.DEFAULT_OUTLOOK_INTEGRATION_APP_NO_IM";
        checkboxLabel = ConfigurationUtils.isCallingEnabled() ?
            checkboxLabel :
            "plugin.generalconfig.DEFAULT_OUTLOOK_INTEGRATION_APP_NO_CALLS";

        final JCheckBox defaultOutlookIntegrationAppCheckBox =
            new SIPCommCheckBox(
                Resources.getString(checkboxLabel),
                configService.user().getBoolean(DEFAULT_OUTLOOK_INTEGRATION_APP_PROP, true));
        defaultOutlookIntegrationAppCheckBox.setForeground(TEXT_COLOR);

        defaultOutlookIntegrationAppCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                logger.user("Default Outlook integration checkbox toggled to: " +
                    defaultOutlookIntegrationAppCheckBox.isSelected());
                configService.user().setProperty(DEFAULT_OUTLOOK_INTEGRATION_APP_PROP,
                    defaultOutlookIntegrationAppCheckBox.isSelected());
            }
        });

        return defaultOutlookIntegrationAppCheckBox;
    }

    /**
     * Creates the checkbox for whether we should allow auto-log in when the
     * application starts.
     *
     * @return the Automatic log-in checkbox.
     */
    private Component createAutomaticLogInCheckBox()
    {
        final JCheckBox createAutomaticLogInCheckBox =
            new SIPCommCheckBox(
                Resources.getString("plugin.generalconfig.DEFAULT_AUTO_LOG_IN"),
                configService.user().getBoolean(DEFAULT_AUTO_LOG_IN, true));
        createAutomaticLogInCheckBox.setForeground(TEXT_COLOR);

        createAutomaticLogInCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                logger.user("Auto Login checkbox toggled to: " + createAutomaticLogInCheckBox.isSelected());
                configService.user().setProperty(DEFAULT_AUTO_LOG_IN,
                    createAutomaticLogInCheckBox.isSelected());
            }
        });

        return createAutomaticLogInCheckBox;
    }

    /**
     * Creates the checkbox for whether we should be used to launch SIP and TEL
     * URI links.
     *
     * @return the URL protocol handler check box.
     */
    private Component createUrlProtocolHandlerAppCheckBox()
    {
        final JCheckBox urlProtocolHandlerCheckBox =
            new SIPCommCheckBox(
                Resources.getString("plugin.generalconfig.URL_PROTOCOL_HANDLER_APP"),
                ConfigurationUtils.isProtocolURLHandler());
        urlProtocolHandlerCheckBox.setForeground(TEXT_COLOR);

        urlProtocolHandlerCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                boolean isSelected = urlProtocolHandlerCheckBox.isSelected();
                logger.user("URL protocol handler checkbox toggled to: " +
                                    isSelected);
                ConfigurationUtils.setProtocolURLHandler(isSelected);
            }
        });

        return urlProtocolHandlerCheckBox;
    }

    /**
     * Returns the application name.
     * @return the application name
     */
    private String getApplicationName()
    {
        return Resources.getSettingsString("service.gui.APPLICATION_NAME");
    }

    /**
     * Initializes the personal contact configuration panel.
     *
     * @return the created component
     */
    private Component createContactConfigPanel()
    {
        ConfigSectionPanel contactConfigPanel = null;

        if (ConfigurationUtils.isCallingOrImEnabled())
        {
            contactConfigPanel =
                new ConfigSectionPanel("plugin.generalconfig.CONTACT_CONFIG");

            // Create and add the contact store panel:
            TransparentPanel contactsStorePanel = createContactStorePanel();
            if (contactsStorePanel != null)
            {
                contactConfigPanel.addPanel(contactsStorePanel);

                JTextArea helpLabel = createHintPanel("plugin.generalconfig.CONTACT_STORE_HINT");
                ScaleUtils.scaleFontAsDefault(helpLabel);
                contactConfigPanel.add(helpLabel);
            } else
            {
                contactConfigPanel = null;
            }
        }

        return contactConfigPanel;
    }

    /**
     * Updates the valid double-click actions for a contact. VIEW is
     * always valid but CALL, IM and CONF are only valid if they are enabled.
     */
    private void updateContactClickOptions()
    {
        boolean callEnabled = ConfigurationUtils.isCallingEnabled();
        boolean imEnabled = ConfigurationUtils.isImEnabled();
        boolean inviteToConfEnabled =
            CreateConferenceMenu.isConferenceInviteByImEnabled();

        // Build up a list of all of the valid double click options.
        List<String> opts = new ArrayList<>();

        if (callEnabled)
        {
            opts.add("CALL");
        }

        if (imEnabled)
        {
            opts.add("IM");
        }

        opts.add("VIEW");

        if (inviteToConfEnabled)
        {
            opts.add("CONF");
        }

        // If the selected action is no longer enabled, then
        // reset the action to one that is enabled in this order: IM/CONF > CALL > VIEW.
        // VIEW is always enabled.
        String contactDoubleClickAction = ConfigurationUtils.getContactDoubleClickAction();
        String newContactDoubleClickAction = contactDoubleClickAction;

        if (!imEnabled && "IM".equals(contactDoubleClickAction))
        {
            contactDoubleClickAction = "CALL";
        }

        if (!inviteToConfEnabled && "CONF".equals(contactDoubleClickAction))
        {
            contactDoubleClickAction = "CALL";
        }

        if (!callEnabled && "CALL".equals(newContactDoubleClickAction))
        {
            contactDoubleClickAction = "VIEW";
        }

        if (!newContactDoubleClickAction.equals(contactDoubleClickAction))
        {
            logger.debug("Changing double-click from " + contactDoubleClickAction +
                         " to " + newContactDoubleClickAction);
            ConfigurationUtils.setContactDoubleClickAction("CALL");
        }

    }

    /**
    * Sets the visibility of the recordings panel
    */
   private void setRecordingsPanelVisibility()
   {
       // The recordings panel allows the user to configure the folder that
       // recorded calls and/or conferences are saved to and includes a button
       // to open that folder. Therefore, it is visible if at least one of the
       // following is true.
       //  - Call Recording is enabled in the user's branding/CoS.
       //  - The conference service is enabled.
       boolean callRecordingEnabled =
           !configService.user().getBoolean(CALL_RECORDING_DISABLED_PROP, false);
       boolean confEnabled =
           GeneralConfigPluginActivator.getConferenceService().isFullServiceEnabled();

       logger.debug("Setting recordings panel visibility. Call recording enabled? " +
                       callRecordingEnabled + ", conf enabled? " + confEnabled);
       recordingsPanel.setVisible(callRecordingEnabled || confEnabled);
   }

    /**
     * @return The panel which controls the contact store or null if this is not
     *         configurable for some reason.
     */
    private TransparentPanel createContactStorePanel()
    {
        TransparentPanel saveContactsPanel = null;
        List<AccountID> accounts =
                                GeneralConfigPluginActivator.getContactStores();

        // Show a panel for the user to select a contact store, only if the
        // user is allowed to override the default store (set by provisioning).
        if (!configService.user().getBoolean(CONTACT_STORE_CONFIG_ENABLED_PROP, true))
            return null;

        // Only show the panel if there is more than one contact store
        // available.
        if (accounts.isEmpty())
            return null;

        // Loading spinner to show while the contact source is switching.
        ImageIconFuture spinnerIcon = Resources.getImage(LOADING_SPINNER_KEY);
        final JLabel loadingSpinner = spinnerIcon.addToLabel(new JLabel());
        spinnerIcon.setImageObserver(loadingSpinner);
        loadingSpinner.setVisible(false);

        // Create a combo box containing all the providers.
        final JComboBox<AccountID> contactStoreComboBox = new JComboBox<>();
        ScaleUtils.scaleFontAsDefault(contactStoreComboBox);

        // Load the configured store, so we can select it in the combo box.
        String configuredStore =
            (String)configService.user().getProperty(PERSONAL_CONTACT_STORE_KEY);

        for (AccountID account : accounts)
        {
            contactStoreComboBox.addItem(account);

            if (account.getProtocolName().equalsIgnoreCase(configuredStore))
            {
                // This is the chosen store - select it then continue to
                // add stores to the combo box.
                contactStoreComboBox.setSelectedItem(account);
            }
        }

        // Give the combo box a custom renderer that shows a suitable
        // string for the protocol provider names (e.g. 'Outlook').
        DefaultListCellRenderer renderer = new DefaultListCellRenderer()
        {
            private static final long serialVersionUID = 0L;

            @Override
            public Component getListCellRendererComponent(JList<? extends Object> list,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean hasFocus)
            {
                String protocolName = ((AccountID)value).getProtocolName();

                // If the account protocol is the Contact Source Protocol,
                // contacts are coming from Outlook or Mac Address Book.
                if (protocolName.equalsIgnoreCase(ProtocolNames.ADDRESS_BOOK))
                {
                    String keySuffix =
                                OSUtils.IS_WINDOWS ? "OUTLOOK" : "ADDRESS_BOOK";
                    protocolName = Resources.getString(
                                           CONTACT_STORE_NAME_ROOT + keySuffix);
                }
                else if (protocolName.equalsIgnoreCase("CommPortal"))
                {
                    protocolName = Resources.getString(
                                        CONTACT_STORE_NAME_ROOT + "COMMPORTAL");
                }

                Component lcrComponent = super.getListCellRendererComponent(list,
                                                          protocolName,
                                                          index,
                                                          isSelected,
                                                          hasFocus);
                ScaleUtils.scaleFontAsDefault(lcrComponent);
                return lcrComponent;
            }
        };

        contactStoreComboBox.setRenderer(renderer);

        // When an option is selected in the combo box, save the new
        // contact store to config and load it.
        contactStoreComboBox.addItemListener(new ItemListener()
        {
            public void itemStateChanged(ItemEvent evt)
            {
                // We're only interested in items being selected (not the
                // effectively duplicate unselected events) so that we
                // don't do processing twice.
                if (evt.getStateChange() == ItemEvent.SELECTED)
                {
                    final AccountID account =
                               (AccountID)contactStoreComboBox.getSelectedItem();
                    logger.user("New contact source selected: " +
                                (account != null ? account.getLoggableAccountID(): null));

                    // Load the selected protocol provider and unload all
                    // others. Disable the contact store selector in the
                    // meantime, and show a 'loading' spinner.
                    contactStoreComboBox.setEnabled(false);
                    loadingSpinner.setVisible(true);
                    sContactStoreSwitchThread =
                                          new Thread("ContactStoreSwitchThread")
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                loadContactStore(account);
                            }
                            finally
                            {
                                loadingSpinner.setVisible(false);
                                contactStoreComboBox.setEnabled(true);
                            }
                        }
                    };
                    sContactStoreSwitchThread.start();

                    // Save the protocol provider name in config.
                    configService.user().setProperty(PERSONAL_CONTACT_STORE_KEY,
                                              account.getProtocolName());
                }
            }
        });

        saveContactsPanel =
                    new TransparentPanel(new FlowLayout(FlowLayout.LEFT,
                                                        ScaleUtils.scaleInt(4),
                                                        ScaleUtils.scaleInt(3)));

        JLabel saveContactsLabel = new JLabel();
        saveContactsLabel.setText(Resources.getString(
                                   "plugin.generalconfig.CONTACT_STORE_LABEL"));
        saveContactsLabel.setForeground(TEXT_COLOR);
        ScaleUtils.scaleFontAsDefault(saveContactsLabel);
        saveContactsPanel.add(saveContactsLabel);
        saveContactsPanel.add(contactStoreComboBox);
        saveContactsPanel.add(loadingSpinner);

        AccessibilityUtils.setNameAndDescription(
            contactStoreComboBox,
            Resources.getResources().getI18NString("plugin.generalconfig.CONTACT_STORE_LABEL_NAME"),
            Resources.getResources().getI18NString("plugin.generalconfig.CONTACT_STORE_LABEL_DESCRIPTION"));

        if (sContactStoreSwitchThread != null &&
            sContactStoreSwitchThread.isAlive())
        {
            // The contact store was changed in an earlier instance of the
            // GeneralConfigurationPanel. Disable the contact store selector
            // and show a loading indication until the switch has finished.
            new Thread("ContactStoreUIEnabler")
            {
                public void run()
                {
                    logger.info("Contact store switch in progress. " +
                                " Hiding store selector UI until done.");
                    loadingSpinner.setVisible(true);
                    contactStoreComboBox.setEnabled(false);

                    while (true)
                    {
                        try
                        {
                            sContactStoreSwitchThread.join();
                            break;
                        }
                        catch (InterruptedException e)
                        {
                            // Don't wake up until we've managed to join the
                            // switcher thread.
                            logger.error("ContactStoreUIEnabled ignored " +
                                         " an InterruptedException", e);
                        }
                    }

                    loadingSpinner.setVisible(false);
                    contactStoreComboBox.setEnabled(true);
                    logger.info("Contact store selector UI re-enabled.");
                }
            }.start();
        }

        return saveContactsPanel;
    }

    /**
     * Load the specified personal contact store and unload all others, so that
     * the contacts showing in the main UI are only from the specified store.
     *
     * @param store The saved account info of the personal contact store to
     *              load.
     */
    private void loadContactStore(AccountID store)
    {
        List<AccountID> accounts =
            GeneralConfigPluginActivator.getContactStores();

        if (accounts.size() > 0)
        {
            try
            {
                // Loop through the account stores, loading the selected
                // account and unloading the others.
                for (AccountID account : accounts)
                {
                    if (account.equals(store))
                    {
                        logger.info("Attempt to load account: " +
                                     account.getLoggableAccountID());
                        accountManager.loadAccount(account);
                        logger.debug("Account loaded: " +
                                     account.getLoggableAccountID());
                    }
                    else
                    {
                        logger.debug("Attempt to unload account: " +
                                     account.getLoggableAccountID());
                        accountManager.unloadAccount(account);
                        logger.debug("Account unloaded: " +
                                     account.getLoggableAccountID());
                    }
                }
            }
            catch (OperationFailedException e)
            {
                logger.error("Exception selecting personal contact store", e);
            }
        }
    }

    /**
     * Initializes the auto start checkbox. Used only on windows.
     * @return the created auto start check box
     */
    private Component createAutoStartCheckBox()
    {
        final JCheckBox autoStartCheckBox = new SIPCommCheckBox();

        String label = Resources.getString(
                "plugin.generalconfig.AUTO_START");
        autoStartCheckBox.setText(label);
        autoStartCheckBox.setForeground(TEXT_COLOR);
        autoStartCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    logger.user("Auto start checkbox toggled to: " +
                                        autoStartCheckBox.isSelected());
                    setAutostart(autoStartCheckBox.isSelected());
                }
                catch (Exception ex)
                {
                    logger.error("Cannot create/delete startup shortcut", ex);
                }
            }
        });

        try
        {
            autoStartCheckBox.setSelected(
                WindowsStartup.isStartupEnabled(getApplicationName()));
        }
        catch (Exception e)
        {
            logger.error(e);
        }

        return autoStartCheckBox;
    }

    /**
     * Initializes the notification configuration panel.
     * @return the created panel
     */
    private Component createNotificationConfigPanel()
    {
        ServiceReference<?>[] handlerRefs = null;
        BundleContext bc = GeneralConfigPluginActivator.bundleContext;
        try
        {
            handlerRefs = bc.getServiceReferences(
                PopupMessageHandler.class.getName(),
                null);
        }
        catch (InvalidSyntaxException ex)
        {
            logger.warn("Error while retrieving service refs", ex);
        }

        if (handlerRefs == null)
            return null;

        JPanel notifConfigPanel = new ConfigSectionPanel("plugin.notificationconfig.POPUP_NOTIF_HANDLER");

        final JComboBox<Object> notifConfigComboBox = new JComboBox<>();

        String configuredHandler =
            (String) configService.user().getProperty("systray.POPUP_HANDLER");

        for (ServiceReference<?> ref : handlerRefs)
        {
            PopupMessageHandler handler =
                (PopupMessageHandler) bc.getService(ref);

            notifConfigComboBox.addItem(handler);

            if (configuredHandler != null &&
                configuredHandler.equals(handler.getClass().getName()))
            {
                notifConfigComboBox.setSelectedItem(handler);
            }
        }

        // We need an entry in combo box that represents automatic
        // popup handler selection in systray service. It is selected
        // only if there is no user preference regarding which popup
        // handler to use.
        String auto = "Auto";
        notifConfigComboBox.addItem(auto);
        if (configuredHandler == null)
        {
            notifConfigComboBox.setSelectedItem(auto);
        }

        notifConfigComboBox.addItemListener(new ItemListener()
        {
            public void itemStateChanged(ItemEvent evt)
            {
                if (notifConfigComboBox.getSelectedItem() instanceof String)
                {
                    // "Auto" selected. Delete the user's preference and
                    // select the best available handler.
                    ConfigurationUtils.setPopupHandlerConfig(null);
                    GeneralConfigPluginActivator.getSystrayService()
                        .selectBestPopupMessageHandler();
                }
                else
                {
                    PopupMessageHandler handler =
                        (PopupMessageHandler)
                        notifConfigComboBox.getSelectedItem();

                    ConfigurationUtils.setPopupHandlerConfig(
                        handler.getClass().getName());

                    GeneralConfigPluginActivator.getSystrayService()
                        .setActivePopupMessageHandler(handler);
                }
            }
        });
        notifConfigPanel.add(notifConfigComboBox);

        return notifConfigPanel;
    }

    /**
     * Initializes the local configuration panel.
     * @return the created component
     */
    private Component createLocaleConfigPanel()
    {
        JPanel localeConfigPanel = new ConfigSectionPanel("plugin.generalconfig.DEFAULT_LANGUAGE");

        TransparentPanel localeComboBoxPanel =
            new TransparentPanel(new FlowLayout(FlowLayout.LEFT,
                                                ScaleUtils.scaleInt(4),
                                                ScaleUtils.scaleInt(3)));

        final JComboBox<String> localesConfigComboBox = new JComboBox<>();

        Iterator<Locale> iter =
                Resources.getResources().getAvailableLocales();
        while (iter.hasNext())
        {
            Locale locale = iter.next();
            localesConfigComboBox.addItem(
                locale.getDisplayLanguage(locale));
        }
        Locale currLocale =
            ConfigurationUtils.getCurrentLanguage();
        localesConfigComboBox.setSelectedItem(currLocale
            .getDisplayLanguage(currLocale));

        localesConfigComboBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                GeneralConfigPluginActivator.getUIService().getPopupDialog().
                    showMessagePopupDialog(Resources.getString(
                    "plugin.generalconfig.DEFAULT_LANGUAGE_RESTART_WARN"));

                String language =
                        (String)localesConfigComboBox.getSelectedItem();
                Iterator<Locale> iter =
                    Resources.getResources().getAvailableLocales();
                while (iter.hasNext())
                {
                    Locale locale = iter.next();
                    if (locale.getDisplayLanguage(locale)
                        .equals(language))
                    {
                        ConfigurationUtils.setLanguage(locale);
                        break;
                    }
                }
            }
        });
        localeComboBoxPanel.add(localesConfigComboBox);
        localeConfigPanel.add(localeComboBoxPanel);

        String label =
                Resources.getString(
                        "plugin.generalconfig.DEFAULT_LANGUAGE_RESTART_WARN");
        JLabel warnLabel = new JLabel(label);
        warnLabel.setForeground(Color.GRAY);
        warnLabel.setFont(warnLabel.getFont().
            deriveFont(ScaleUtils.getScaledFontSize(8f)));
        warnLabel.setBorder(ScaleUtils.createEmptyBorder(3, 4, 0, 0));
        warnLabel.setHorizontalAlignment(JLabel.LEADING);
        localeConfigPanel.add(warnLabel);

        return localeConfigPanel;
    }

    /**
     * Creates the panel containing the combo box used to select the folder
     * where recorded calls/conferences are saved and the button to open that
     * folder.
     *
     * @return The recordings panel
     */
    private Component createRecordingsPanel()
    {
        ConfigSectionPanel recordingsPanel =
            new ConfigSectionPanel("service.gui.RECORDINGS");

        // We want a label to appear to the left of the combo box, so we use
        // an intermediary panel to store both the label and the combo box.
        TransparentPanel panel =
            new TransparentPanel(new FlowLayout(FlowLayout.LEFT,
                                                ScaleUtils.scaleInt(4),
                                                ScaleUtils.scaleInt(3)));

        // Create the label.
        JLabel saveCallsLabel = new JLabel(
            Resources.getString("plugin.generalconfig.SAVE_CALLS"));
        ScaleUtils.scaleFontAsDefault(saveCallsLabel);
        saveCallsLabel.setForeground(TEXT_COLOR);
        panel.add(saveCallsLabel);

        // Create the combo box, and restrict its width.
        CallRecordingDirSelectorComboBox recordingDirSelector =
                                         new CallRecordingDirSelectorComboBox();
        recordingDirSelector.setMaximumSize(
            new Dimension(CALL_RECORDING_SELECTOR_MAX_WIDTH,
                          recordingDirSelector.getMaximumSize().height));

        AccessibilityUtils.setNameAndDescription(
            recordingDirSelector,
            Resources.getResources().getI18NString("plugin.generalconfig.SAVE_CALLS"),
            Resources.getResources().getI18NString("plugin.generalconfig.SAVE_CALLS_DESCRIPTION"));

        panel.add(recordingDirSelector);
        panel.add(createShowRecordingsFolderButton());

        recordingsPanel.addPanel(panel);

        return recordingsPanel;
    }

    /**
     * Creates the button for the recordings panel which opens the folder
     * where recorded calls/conferences are currently saved.
     *
     * @return The newly-created recordings button.
     */
    private Component createShowRecordingsFolderButton()
    {
        JButton button = new SIPCommBasicTextButton(Resources.getString(
                             "service.gui.CALL_RECORDING_SHOW_RECORDED_CALLS"));

        AccessibilityUtils.setNameAndDescription(button,
            Resources.getString("service.gui.CALL_RECORDING_SHOW_RECORDED_CALLS"),
            Resources.getString("service.gui.CALL_RECORDING_SHOW_RECORDED_CALLS_DESCRIPTION"));

        button.addActionListener(new ActionListener()
        {
           public void actionPerformed(ActionEvent evt)
           {
               logger.user("Show recordings folder clicked");
               String savedCallsPath =
                   configService.user().getString(Recorder.SAVED_CALLS_PATH,
                                   ConfigurationUtils.DEFAULT_SAVED_CALLS_PATH);
               File recordingsFolder = new File(savedCallsPath);

               if (!recordingsFolder.exists())
               {
                   logger.debug("Recordings folder " +
                                " does not exist, so it will now be created.");
                   try
                   {
                       boolean success = recordingsFolder.mkdir();
                       if (!success)
                       {
                           logger.error("Unspecified failure when creating " +
                                        "recordings folder.");
                           showFailedToOpenRecordingFolderDialog();
                       }
                   }
                   catch (SecurityException e)
                   {
                       logger.error("Failed to create recordings folder.",
                                    e);
                       showFailedToOpenRecordingFolderDialog();
                   }
               }

               try
               {
                   FileUtils.openFileOrFolder(recordingsFolder);
                   logger.debug("Opened recordings folder");
               }
               catch (IOException e)
               {
                   logger.error("Failed to show recordings folder");
                   showFailedToOpenRecordingFolderDialog();
               }
           }
        });

        JPanel buttonPanel =
            new TransparentPanel(new FlowLayout((FlowLayout.LEFT), 0, 0));
        buttonPanel.add(button);
        buttonPanel.setBorder(ScaleUtils.createEmptyBorder(0, 18, 0, 0));
        return buttonPanel;
    }

    /**
     * Shows the error dialog which informs the user that we failed to open
     * the recordings folder.
     */
    private void showFailedToOpenRecordingFolderDialog()
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                new ErrorDialog(Resources.getString("service.gui.ERROR"),
                    Resources.getString("service.gui.FOLDER_OPEN_FAILED"))
                    .showDialog();
            }
        });
    }

    /**
     * Initializes the privacy panel
     *
     * @return the created component
     */
    private Component createPrivacyPanel()
    {
        JPanel privacyPanel = new ConfigSectionPanel("plugin.generalconfig.PRIVACY");

        privacyPanel.add(createAnalyticsCheckBox());
        if (!ConfigurationUtils.isQaMode() &&
            ConfigurationUtils.isDataSendingEnabled())
        {
            privacyPanel.add(createEraseErrorsButton());
        }

        return privacyPanel;
    }

    /**
     * Initializes the analytics opt-out checkbox.
     *
     * @return the created auto start check box
     */
    private Component createAnalyticsCheckBox()
    {
        final JCheckBox analyticsCheckBox = new SIPCommCheckBox();
        final String name = "net.java.sip.communicator.impl.analytics.ENABLED";

        String label = Resources.getString("plugin.generalconfig.ANALYTICS_OPT_OUT");
        analyticsCheckBox.setText(label);
        analyticsCheckBox.setForeground(TEXT_COLOR);
        analyticsCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                logger.user("Analytics opt-out checkbox toggled to: " +
                                    analyticsCheckBox.isSelected());
                configService.user().setProperty(name, analyticsCheckBox.isSelected());
            }
        });

        analyticsCheckBox.setSelected(configService.user().getBoolean(name, true));

        return analyticsCheckBox;
    }

    /**
     * Adds a button to erase the list of stored errors, enabling the user to be
     * shown them again.
     *
     * @return A button to reset errors
     */
    private Component createEraseErrorsButton()
    {
        final JButton eraseErrorsButton = new SIPCommBasicTextButton(
                Resources.getString("plugin.errorreport.RESET_ERRORS_TEXT"));

        logger.info("GeneralConfigurationPanel::createEraseErrorsButton()");
        eraseErrorsButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        eraseErrorsButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                logger.info("User selected to clear errors");
                GeneralConfigPluginActivator.getDiagnosticsService()
                        .clearEncounteredErrors();
            }
        });

        JPanel eraseErrorsPanel = new TransparentPanel();
        eraseErrorsPanel.setLayout(new BoxLayout(eraseErrorsPanel, BoxLayout.Y_AXIS));
        eraseErrorsPanel.add(eraseErrorsButton);

        return eraseErrorsPanel;
    }

    /**
     * Initializes the device type config panel.
     * @return the created component
     */
    private Component createDeviceTypeConfigPanel()
    {
        JPanel updateConfigPanel = new ConfigSectionPanel("plugin.generalconfig.DEVICE_TYPE_CONFIG");

        JRadioButton desktopRadioButton = createDesktopRadioButton();
        JRadioButton laptopRadioButton = createLaptopRadioButton();

        ButtonGroup deviceTypeGroup = new ButtonGroup();
        deviceTypeGroup.add(desktopRadioButton);
        deviceTypeGroup.add(laptopRadioButton);
        updateConfigPanel.add(desktopRadioButton);
        updateConfigPanel.add(laptopRadioButton);

        return updateConfigPanel;
    }

    /**
     * Initializes the desktop device-type radio button.
     * @return the created component
     */
    private JRadioButton createDesktopRadioButton()
    {
        final String desktop = "desktop";
        JRadioButton desktopButton = new JRadioButton();
        desktopButton.setText(
            Resources.getString("plugin.generalconfig.DEVICE_TYPE_DESKTOP"));
        desktopButton.setOpaque(false);

        desktopButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                setDeviceIsDesktop(desktop);
            }
        });

        String deviceType =
            configService.user().getString(
                "net.java.sip.communicator.plugin.devicetypeprompter.DEVICE_TYPE");
        desktopButton.setSelected(deviceType == null ||
                                  desktop.equalsIgnoreCase(deviceType));

        return desktopButton;
    }

    /**
     * Initializes the laptop device-type radio button.
     * @return the created component
     */
    private JRadioButton createLaptopRadioButton()
    {
        final String laptop = "laptop";
        JRadioButton laptopButton = new JRadioButton();
        laptopButton.setText(
            Resources.getString("plugin.generalconfig.DEVICE_TYPE_LAPTOP"));
        laptopButton.setOpaque(false);

        laptopButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                setDeviceIsDesktop(laptop);
            }
        });

        String deviceType =
            configService.user().getString(
                "net.java.sip.communicator.plugin.devicetypeprompter.DEVICE_TYPE");
        laptopButton.setSelected(laptop.equalsIgnoreCase(deviceType));

        return laptopButton;
    }

    /**
     * Saves the device type and consequent codec order to config.
     * @param deviceType the type of computer e.g. "desktop" or "laptop"
     */
    private void setDeviceIsDesktop(String deviceType)
    {
        configService.user().setProperty(
            "net.java.sip.communicator.plugin.devicetypeprompter.DEVICE_TYPE",
            deviceType);

        // We only want to ever use G722 on desktops as they can be expected
        // to have wired connections.  To put the priority below SILK 16000
        // and above SILK 8000, use a value of 15.
        int codecOrder = "desktop".equalsIgnoreCase(deviceType) ? 15 : 0;
        configService.user().setProperty(
            "net.java.sip.communicator.impl.neomedia.codec.EncodingConfiguration.G722/8000",
            codecOrder);
    }

    /**
     * Creates the panel for the reset section of the general config
     * panel
     *
     * @return the reset panel
     */
    private Component createResetConfigPanel()
    {
        JPanel resetConfigPanel = new ConfigSectionPanel("service.gui.RESET_HEADER");

        resetConfigPanel.add(createResetButton(resetConfigPanel));

        return resetConfigPanel;
    }

    /**
     * Initializes the reset check box.
     * @return the created check box
     */
    private Component createResetButton(JPanel parentPanel)
    {
        logger.entry();
        final JButton resetButton = new SIPCommBasicTextButton(
                                      Resources.getString("service.gui.RESET_BUTTON"));

        resetButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        resetButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                logger.user("Clear data button clicked");
                ClearDataDialog dialog = new ClearDataDialog();
                ResetService resetService =
                                 GeneralConfigPluginActivator.getResetService();

                if (dialog.showDialog())
                {
                    logger.user("Reset config confirmed, clear data for " +
                                        (dialog.isClearAll() ?
                                                "all users on computer" :
                                                "this user only") +
                                        " selected");
                    resetService.setDeleteUserConfig(true);
                    resetService.setDeleteGlobalConfig(dialog.isClearAll());
                    resetService.setRestart(true);
                    GeneralConfigPluginActivator.getShutdownService().beginShutdown(true);
                }
                else
                {
                    logger.user("Reset config dialog quit");
                    resetService.setDeleteUserConfig(false);
                    resetService.setDeleteGlobalConfig(false);
                    resetService.setRestart(false);
                }
            }
        });

        JPanel resetPanel = new TransparentPanel();
        resetPanel.setLayout(new BoxLayout(resetPanel, BoxLayout.Y_AXIS));
        resetPanel.add(resetButton);

        return resetPanel;
    }

    /**
     * Sets the auto start.
     * @param isAutoStart indicates if the auto start property is set to true or
     * false
     * @throws Exception if something goes wrong when obtaining the canonical
     * path or when creating or saving the shortcut
     */
    private void setAutostart(boolean isAutoStart)
        throws Exception
    {
        String workingDir = new File(".").getCanonicalPath();

        WindowsStartup.setAutostart(
                getApplicationName(), workingDir, isAutoStart);
    }

    @Override
    public void onRefresh()
    {
        // The panel has been refreshed - check whether we need to update
        // - the valid contact double click options
        // - the visibility of the recordings selector
        updateContactClickOptions();
        setRecordingsPanelVisibility();
        // If we are refocusing on the window, and have not just updated the
        // ringtone, we want to refresh the generalConfigPanel to catch any
        // recent changes.
        if (!mJustUpdatedRingtone.getAndSet(false))
        {
            createOrUpdateGeneralConfigPanel();
        }
    }

    @Override
    public void onDismissed()
    {
        // We don't want audio to keep playing after the config panel is no
        // longer visible, so force-stop it.
        if (mPreviewButton != null)
        {
            mPreviewButton.stopAudio();
        }
        // Since we have left the window, we must have refocused on it after any
        // changes to the ringtone.
        mJustUpdatedRingtone.set(false);
    }
}
