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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.configforms.ConfigurationFrame;
import net.java.sip.communicator.plugin.desktoputil.ConfigSectionPanel;
import net.java.sip.communicator.plugin.desktoputil.CreateConferenceMenu;
import net.java.sip.communicator.plugin.desktoputil.ErrorDialog;
import net.java.sip.communicator.plugin.desktoputil.OptionListConfigurationPanel;
import net.java.sip.communicator.plugin.desktoputil.SIPCommBasicTextButton;
import net.java.sip.communicator.plugin.desktoputil.SIPCommCheckBox;
import net.java.sip.communicator.plugin.desktoputil.ScaleUtils;
import net.java.sip.communicator.plugin.desktoputil.TransparentPanel;
import net.java.sip.communicator.service.conference.ConferenceService;

import net.java.sip.communicator.service.insights.InsightsEventHint;
import net.java.sip.communicator.service.insights.parameters.CommonParameterInfo;
import net.java.sip.communicator.service.insights.parameters.GeneralConfigPluginParameterInfo;
import net.java.sip.communicator.service.notification.SoundNotificationAction;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.AccountManager;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.ProtocolNames;
import net.java.sip.communicator.service.reset.ResetService;
import net.java.sip.communicator.util.AccessibilityUtils;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.FileUtils;
import net.java.sip.communicator.util.Logger;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.neomedia.Recorder;
import org.jitsi.service.resources.ImageIconFuture;
import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.util.OSUtils;
import org.jitsi.util.StringUtils;

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
     * The map of buttons within the ringtones panel which is used to preview ringtones.
     * We hold a reference to it so that we can halt any playing audio when we
     * are closed or are no longer the active tab in the configuration panel.
     */
    private static final Map<SoundNotificationAction.BgTag, RingtonePreviewButton> mPreviewButtonMap = new HashMap<>();

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
     * Indicates if the privacy panel should be disabled, i.e. not visible to
     * the user.
     */
    private static final String PRIVACY_PROP =
        "net.java.sip.communicator.plugin.generalconfig.privacy.DISABLED";

    private static final String BG_CONTACTS_ENABLED_PROP =
                                "net.java.sip.communicator.BG_CONTACTS_ENABLED";

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
     * The name of the Locale not corresponding to any real language, must not
     * appear in Locale dropdown.
     */
    private static final String INVALID_LOCALE_NAME = "plain";

    /**
     * The resource key for the system language option text.
     */
    private static final String SYSTEM_LANGUAGE =
        "plugin.generalconfig.SYSTEM_LANGUAGE";

    /**
     * The resource key for the language and country option text.
     */
    private static final String LANGUAGE_AND_COUNTRY =
        "service.gui.LANGUAGE_AND_COUNTRY";

    /**
     * The maximum width, in pixels, of the dropdown used to select the folder
     * to save recordings to.
     */
    private static final int CALL_RECORDING_SELECTOR_MAX_WIDTH =
                                                       ScaleUtils.scaleInt(150);

    private static final String CONFIG_DISTINCT_RINGTONES_ENABLED_PROP =
            "net.java.sip.communicator.plugin.generalconfig.ringtonesconfig.DISTINCT_ENABLED";

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
     * The panel with all of the general config displayed on it. This includes
     * ringtones, integration with Outlook and whether to launch the client on
     * startup, but not for example Contact settings or recording settings.
     */
    private JPanel mGeneralConfigPanel;

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
     * Distinct ringtone selector changes visibility according to the distinct ringing checkbox
     * action event
     */
    private TransparentPanel distinctRingtoneSelector;

    /**
     * Creates the general configuration panel.
     */
    public GeneralConfigurationPanel()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Add a new GeneralConfigPanel to the top.
        createGeneralConfigPanel();

        if (!configService.global().getBoolean(DEVICE_TYPE_CONFIG_DISABLED_PROP, false))
        {
            Component deviceTypeConfigPanel = createDeviceTypeConfigPanel();
            if (deviceTypeConfigPanel != null)
            {
                add(deviceTypeConfigPanel);
            }
        }

        add(createLocaleConfigPanel());

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
     * Creates the panel for the general section of the general config panel
     */
    private synchronized void createGeneralConfigPanel()
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

            // This option only makes sense if we actually have call function.
            if (ConfigurationUtils.isCallingEnabled())
            {
                generalConfigJPanel.add(createUrlProtocolHandlerAppCheckBox());
            }
        }

        // Ringtones are only used for incoming calls or incoming meeting invites (which
        // are IMs).
        ConferenceService mConferenceService =
            GeneralConfigPluginActivator.getConferenceService();
        boolean meetingInvitesEnabled = mConferenceService != null
            && mConferenceService.isFullServiceEnabled()
            && ConfigurationUtils.isImEnabled();

        if (!configService.global().getBoolean(RINGTONES_CONFIG_DISABLED_PROP, false) &&
            (ConfigurationUtils.isCallingEnabled() || meetingInvitesEnabled))
        {
            TransparentPanel genericRingtoneSelector = createRingtoneSelector(
                    Resources.getResources().getI18NString("service.gui.RINGTONE_SELECTOR_LABEL"),
                    Resources.getResources().getI18NString("service.gui.RINGTONE_SELECTOR_LABEL_DESCRIPTION"),
                    SoundNotificationAction.BgTag.BG_TAG_GENERIC);
            generalConfigJPanel.add(genericRingtoneSelector);

            boolean bgContactsEnabled = configService.user().getBoolean(BG_CONTACTS_ENABLED_PROP, false);
            if (bgContactsEnabled)
            {
                JCheckBox checkBox = createDistinctRingingCheckbox();
                generalConfigJPanel.add(checkBox);
                distinctRingtoneSelector = createRingtoneSelector(
                        Resources.getResources().getI18NString("service.gui.RINGTONE_SELECTOR_LABEL_BG"),
                        Resources.getResources().getI18NString("service.gui.RINGTONE_SELECTOR_LABEL_BG_DESCRIPTION"),
                        SoundNotificationAction.BgTag.BG_TAG_INTERNAL);
                generalConfigJPanel.add(distinctRingtoneSelector);
                distinctRingtoneSelector.setVisible(checkBox.isSelected());
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
     * Creates the panel used to select the current ringtone (which
     * is used both for incoming calls and meeting invites).
     */
    private TransparentPanel createRingtoneSelector(String labelText,
                                                    String description,
                                                    SoundNotificationAction.BgTag bgTag)
    {
        // The label text, dropdown selector, and preview button should all
        // appear on the same row, so we place them in a FlowLayout.
        TransparentPanel selectorPanel = new TransparentPanel();
        selectorPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        // Create the label text for the row.
        JLabel ringtoneLabel = new JLabel();
        ringtoneLabel.setText(labelText);
        ringtoneLabel.setForeground(TEXT_COLOR);
        ScaleUtils.scaleFontAsDefault(ringtoneLabel);

        // We don't want to create a new preview button when we update the panel
        // if one already exists. If we did this, we would not stop playing the
        // audio when the window lost focus.
        if (!mPreviewButtonMap.containsKey(bgTag))
        {
            // Create the button used to preview audio.
            RingtonePreviewButton button = new RingtonePreviewButton();
            mPreviewButtonMap.put(bgTag, button);
            button.addActionListener(l -> {
                logger.debug("Preview button clicked, stopping audio from other buttons");

                mPreviewButtonMap.values().stream()
                        .filter(previewButton -> previewButton.isSelected() && previewButton != button)
                        .forEach(RingtonePreviewButton::stopAudio);
            });
        }

        // Create and populate the dropdown selector from which ringtones may be
        // selected. We need only provide the default ringtones here, as the
        // selector will handle custom ringtones itself.
        Map<String, String> defaultRingtones = getDefaultRingtones();
        RingtoneSelectorComboBox ringtoneSelector = new RingtoneSelectorComboBox(bgTag,
                                                                                 defaultRingtones,
                                                                                 mPreviewButtonMap.get(bgTag));

        ringtoneSelector.setAccessibilityNameAndDescription(labelText, description);

        // The preview button needs to hold a reference to the ringtone selector,
        // so we pass this now.
        mPreviewButtonMap.get(bgTag).registerRingtoneSelector(ringtoneSelector);

        selectorPanel.add(ringtoneLabel);
        selectorPanel.add(ringtoneSelector);
        selectorPanel.add(mPreviewButtonMap.get(bgTag));

        return selectorPanel;
    }

    private JCheckBox createDistinctRingingCheckbox()
    {
        final JCheckBox distinctRingingCheckBox = new SIPCommCheckBox();

        String label = Resources.getString("service.gui.DISTINCT_RINGING_CHECKBOX_LABEL");
        distinctRingingCheckBox.setText(label);
        distinctRingingCheckBox.setForeground(TEXT_COLOR);
        distinctRingingCheckBox.addActionListener(actionEvent -> {
            logger.user("Distinct ringing enabled prop set to: " +
                        distinctRingingCheckBox.isSelected());

            configService.user().setProperty(CONFIG_DISTINCT_RINGTONES_ENABLED_PROP, distinctRingingCheckBox.isSelected());

            GeneralConfigPluginActivator
                    .getInsightsService()
                    .logEvent(InsightsEventHint.GENERAL_CONFIG_PLUGIN_RINGTONE_IS_DISTINCTIVE.name(),
                              Map.of(GeneralConfigPluginParameterInfo.RINGTONE_IS_DISTINCTIVE.name(),
                                     distinctRingingCheckBox.isSelected()));

            // Stop audio when toggling the button.
            mPreviewButtonMap.values().forEach(RingtonePreviewButton::stopAudio);

            distinctRingtoneSelector.setVisible(distinctRingingCheckBox.isSelected());
            mGeneralConfigPanel.updateUI();

            findAndUpdateConfigurationFrame(this);
        });

        distinctRingingCheckBox.setSelected(configService.user().getBoolean(CONFIG_DISTINCT_RINGTONES_ENABLED_PROP, false));

        return distinctRingingCheckBox;
    }

    /**
     * Utility method for digging to the ConfigurationFrame holding the components. Once the parent ConfigurationFrame
     * is found, the frame is resized accordingly, taking into account newly added or removed components. In case the
     * component doesn't have ConfigurationFrame as a parent, the loop terminates naturally once the MainFrame, or
     * the root frame, or first null parent is reached.
     */
    public static void findAndUpdateConfigurationFrame(Component component)
    {
        while (component != null)
        {
            if (component instanceof ConfigurationFrame)
            {
                ((ConfigurationFrame) component).updateCentralPanelSize();
                logger.info("Resized central panel");
                break;
            }
            component = component.getParent();
        }
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

       ConferenceService mConferenceService =
           GeneralConfigPluginActivator.getConferenceService();
       boolean confEnabled = mConferenceService != null
           && mConferenceService.isFullServiceEnabled();

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

        localesConfigComboBox.addItem(Resources.getString(SYSTEM_LANGUAGE));

        Iterator<Locale> iter =
                Resources.getResources().getAvailableLocales();
        while (iter.hasNext())
        {
            Locale locale = iter.next();

            if (!INVALID_LOCALE_NAME.equals(localeToDisplayString(locale)))
            {
                localesConfigComboBox.addItem(localeToDisplayString(locale));
            }
            else
            {
                logger.info("Found an invalid locale.");
            }
        }
        Locale currLocale =
            ConfigurationUtils.getCurrentLanguage();

        if (currLocale != null) {
            localesConfigComboBox.setSelectedItem(localeToDisplayString(currLocale));
        }
        else
        {
            localesConfigComboBox.setSelectedItem(Resources.getString("plugin.generalconfig.SYSTEM_LANGUAGE"));
        }

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
                    if (localeToDisplayString(locale).equals(language))
                    {
                        ConfigurationUtils.setLanguage(locale);
                        return;
                    }
                }

                ConfigurationUtils.setLanguage(null);
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
     * Should be aligned with
     * LocaleSelectorComboBox.localeToDisplayString(Locale locale)
     *
     * @param locale from which the display language will be obtained
     * @return display language
     */
    private String localeToDisplayString(Locale locale)
    {
        String localeDisplayLanguage = locale.getDisplayLanguage(locale);
        String localeDisplayCountry = locale.getDisplayCountry(locale);
        if (!StringUtils.isNullOrEmpty(localeDisplayCountry))
        {
            return Resources.getString(
                    LANGUAGE_AND_COUNTRY,
                    new String[] {localeDisplayLanguage, localeDisplayCountry}
                );
        }
        else
        {
            return localeDisplayLanguage;
        }
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
                boolean isSelected = analyticsCheckBox.isSelected();
                if(!isSelected)
                {
                    // If analytics is being disabled, send the event before actually disabling the feature
                    sendAnalyticForAnalyticsEnabled(false);
                }
                configService.user().setProperty(name, analyticsCheckBox.isSelected());
                if (isSelected)
                {
                    // If analytics is being enabled, send the event after actually enabling the feature
                    sendAnalyticForAnalyticsEnabled(true);
                }
            }
        });

        analyticsCheckBox.setSelected(configService.user().getBoolean(name, true));

        return analyticsCheckBox;
    }

    /**
     * Sends an analytic event for enabling analytics
     *
     * @param value Whether the analytics is enabled
     */
    private void sendAnalyticForAnalyticsEnabled(boolean value)
    {
        GeneralConfigPluginActivator.getInsightsService().logEvent(
                InsightsEventHint.GENERAL_CONFIG_PLUGIN_ANALYTICS_ENABLED.name(),
                Map.of(
                        CommonParameterInfo.NEW_VALUE.name(), value
                ));
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
    }

    @Override
    public void onDismissed()
    {
        // We don't want audio to keep playing after the config panel is no
        // longer visible, so force-stop it.
        mPreviewButtonMap.values().forEach(RingtonePreviewButton::stopAudio);
    }
}
