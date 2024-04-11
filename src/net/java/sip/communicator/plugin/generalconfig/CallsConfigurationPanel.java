/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.generalconfig;

import static net.java.sip.communicator.util.ConfigurationUtils.CALL_RATING_SHOW_PROP;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

import org.json.simple.JSONObject;

import net.java.sip.communicator.plugin.desktoputil.ConfigSectionPanel;
import net.java.sip.communicator.plugin.desktoputil.OptionListConfigurationPanel;
import net.java.sip.communicator.plugin.desktoputil.SIPCommBasicTextButton;
import net.java.sip.communicator.plugin.desktoputil.SIPCommCheckBox;
import net.java.sip.communicator.plugin.desktoputil.SIPCommConfirmDialog;
import net.java.sip.communicator.plugin.desktoputil.ScaleUtils;
import net.java.sip.communicator.plugin.desktoputil.TransparentPanel;
import net.java.sip.communicator.plugin.provisioning.ProvisioningServiceImpl;
import net.java.sip.communicator.service.commportal.CPCos;
import net.java.sip.communicator.service.commportal.CPDataError;
import net.java.sip.communicator.service.commportal.CPDataSenderCallback;
import net.java.sip.communicator.service.commportal.ClassOfServiceService;
import net.java.sip.communicator.service.commportal.CommPortalService;
import net.java.sip.communicator.service.commportal.OutgoingCallerIDType;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.insights.InsightsEventHint;
import net.java.sip.communicator.service.insights.parameters.CommonParameterInfo;
import net.java.sip.communicator.service.insights.parameters.GeneralConfigPluginParameterInfo;
import net.java.sip.communicator.service.phonenumberutils.PhoneNumberUtilsService;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.OperationSetCallPark;
import net.java.sip.communicator.service.protocol.OperationSetCallPark.CallParkListener;
import net.java.sip.communicator.service.protocol.OperationSetCallPark.CallParkOrbit;
import net.java.sip.communicator.service.protocol.OperationSetCallPark.CallParkOrbitState;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import org.jitsi.service.configuration.ConfigurationService;

/**
 * The Calls configuration form.
 */
public class CallsConfigurationPanel
    extends OptionListConfigurationPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>Logger</tt> used by this <tt>CallsConfigurationPanel</tt> for
     * logging output.
     */
    private static final Logger logger
        = Logger.getLogger(CallsConfigurationPanel.class);

    private static final ConfigurationService configService =
        GeneralConfigPluginActivator.getConfigurationService();

    private static final ClassOfServiceService cosService =
        GeneralConfigPluginActivator.getCosService();

    private static final CommPortalService commPortalService =
            GeneralConfigPluginActivator.getCommPortalService();

    private static final PhoneNumberUtilsService phoneNumberUtilsService =
            GeneralConfigPluginActivator.getPhoneNumberUtils();

    private static final MetaContactListService metaContactListService =
            GeneralConfigPluginActivator.getMetaContactListService();

    private static final String META_SUBSCRIBER_BASE_INFORMATION = "Meta_Subscriber_BaseInformation";

    private static final String CALLER_ID_PROPERTY_NAME = "CallerIDToUseOnOutboundCalls";

    /**
     * The comboBox that displays which caller ID will be used on outgoing calls
     */
    private JComboBox<OutgoingCallerIDType> mOutgoingCallerIDComboBox;

    /**
     * The comboBox panel containing comboBox label and the comboBox itself.
     */
    private final JPanel mOutgoingCallerIDPanel;

    /**
     * The comboBox model for the outgoing caller ID comboBox.
     */
    private OutgoingCallerIDComboBoxModel mOutgoingCallerIDComboBoxModel;

    /**
     * Creates the calls configuration panel.
     */
    public CallsConfigurationPanel()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        final ConfigSectionPanel callConfigPanel =
            new ConfigSectionPanel("service.gui.CALLS");

        // Create the Click to Dial panel regardless of whether we need it or not
        final ClickToDialPhonesPanel ctdPhonesPanel = new ClickToDialPhonesPanel();
        callConfigPanel.addPanel(ctdPhonesPanel);

        // Create the Outgoing caller ID panel regardless of whether we need it or not
        mOutgoingCallerIDPanel = createOutgoingCallerIdPanel();
        callConfigPanel.addPanel(mOutgoingCallerIDPanel);

        final ConfigSectionPanel voipContainer = new ConfigSectionPanel(false);

        // Call Park option.  First, get the CallPark operation set.
        ProtocolProviderService[] providers =
                            GeneralConfigPluginActivator.getProtocolProviders();
        List<OperationSetCallPark> callParkOpSets =
                new ArrayList<>();
        for (ProtocolProviderService protocol : providers)
        {
            OperationSetCallPark opSet =
                           protocol.getOperationSet(OperationSetCallPark.class);

            if (opSet != null)
                callParkOpSets.add(opSet);
        }

        // Should have exactly one call park operation set
        if (callParkOpSets.size() == 1)
        {
            voipContainer.addPanel(createCallParkConfigPanel(callParkOpSets.get(0)));
        }
        else
        {
            logger.warn("Unexpected number of call park op. sets " + callParkOpSets);
        }

        voipContainer.addPanel(createCallRatingComponent());

        callConfigPanel.addPanel(voipContainer);
        add(callConfigPanel);

        final Component directCallingPanel = createDirectCallingPanel();
        add(directCallingPanel);

        cosService.getClassOfService((CPCos cos) -> {
            // Clearly, the no VoIP button should only be shown if VoIP is
            // enabled.  But also, it should only be shown if Click to Dial
            // is enabled. This is because it doesn't make sense to disable
            // VoIP if it is the only calling option.
            boolean voipEnabledByUser = ConfigurationUtils.isVoIPEnabledByUser();
            // We could use ConfigurationUtils.isVoIPEnabledInCoS() for
            // the VoIP enabled by CoS setting, but there is a race
            // condition between the CoS storeCosFieldsInConfig() method
            // saving this setting to the config file, and this callback
            // being called (where isVoIPEnabledInCoS() would then read
            // that same config file).
            boolean voipEnabledInCoS = cos.getSubscribedMashups().isVoipAllowed();
            // If the BG needs emergency calling support, then we cannot
            // allow the user to actually disable all VoIP support.
            boolean bgNeedsEmergencyCalling =
                    commPortalService.isEmergencyLocationSupportNeeded();
            boolean ctdRemoteAllowed = cos.getClickToDialRemoteEnabled();
            boolean ctdAllowed = cos.getSubscribedMashups().isCtdAllowed() &&
                                 cos.getClickToDialEnabled();

            // Set the visibility of the VoIP calls section depending on
            // whether voip is allowed and enabled or not
            logger.info("CoS received, display call options panel based " +
                        "on voip enabled in CoS = " + voipEnabledInCoS);
            voipContainer.setVisible(voipEnabledInCoS && voipEnabledByUser);
            directCallingPanel.setVisible(voipEnabledInCoS &&
                                          ctdAllowed &&
                                          !bgNeedsEmergencyCalling);

            // If both VoIP and remote CtD are not allowed, then the call
            // config panel won't have any options thus hide the entire thing.
            logger.info("Changing visibility of section based on voip enabled in CoS " +
                        voipEnabledInCoS + " and ctd remote " + ctdRemoteAllowed);
            callConfigPanel.setVisible(voipEnabledInCoS || ctdRemoteAllowed);

            updateCallerIDComboBox(cos);
        });
    }

    private void updateCallerIDComboBox(CPCos cos)
    {
        mOutgoingCallerIDComboBox.removeAllItems();

        boolean isAllowedToOverrideOutgoingCallerID = cos.isAllowedToOverrideOutgoingCallerID();
        String callingPartyNumber = cos.getOverridableCPN();
        String phoneNumber = cos.getPhoneNumber();
        OutgoingCallerIDType callerIDForOutboundCalls = OutgoingCallerIDType.findByValue(
                (String) configService.user().getProperty(ClassOfServiceService.CONFIG_CALLER_ID_FOR_OUTBOUND_CALLS));

        if (!isAllowedToOverrideOutgoingCallerID || callingPartyNumber == null || phoneNumber == null)
        {
            mOutgoingCallerIDPanel.setVisible(false);
            return;
        }

        String phoneNumberDisplayText = Resources.getResources().getI18NString(
                "service.gui.OUTGOING_CALLER_ID_YOUR_NUMBER",
                new String[]{phoneNumberUtilsService.formatNumberForDisplay(phoneNumber)});

        Contact CPNContact = getBGContactByPhoneNumber(callingPartyNumber);
        String CPNDisplayText = phoneNumberUtilsService.formatNumberForDisplay(callingPartyNumber);
        if (CPNContact != null && CPNContact.getDisplayName() != null && !CPNContact.getDisplayName().isEmpty())
        {
            CPNDisplayText += " - " + CPNContact.getDisplayName();
        }

        Map<OutgoingCallerIDType, String> outgoingCallerIDComboBoxOptions = new HashMap<>();
        outgoingCallerIDComboBoxOptions.put(OutgoingCallerIDType.SUBSCRIBER_DN, phoneNumberDisplayText);
        outgoingCallerIDComboBoxOptions.put(OutgoingCallerIDType.CONFIGURED_CALLING_PARTY_NUMBER, CPNDisplayText);
        mOutgoingCallerIDComboBoxModel = new OutgoingCallerIDComboBoxModel(outgoingCallerIDComboBoxOptions);

        mOutgoingCallerIDComboBox.setModel(mOutgoingCallerIDComboBoxModel);

        mOutgoingCallerIDComboBox.setSelectedItem(callerIDForOutboundCalls);

        CPDataSenderCallback patchComboBoxValueCallback = generateOutgoingCallerIDPatchRequestCallback();
        mOutgoingCallerIDComboBox.addItemListener((ItemEvent ev) -> {
            if (ev.getStateChange() == ItemEvent.SELECTED)
            {
                commPortalService.postServiceIndication(patchComboBoxValueCallback, logger::error, false);
            }
        });

        mOutgoingCallerIDPanel.setVisible(true);
    }

    /**
     * Gets the business group contact by phone number if it exists. This method is used
     * in the Outgoing Caller ID dropdown to retrieve and display the name of the contact
     * associated with the provided phone number.
     *
     * @param number The phone number for which the associated business group contact is sought.
     * @return The Contact object representing the business group contact associated with the
     *         provided phone number, or null if no such contact is found.
     */
    private Contact getBGContactByPhoneNumber(String number)
    {
        Contact CPNContact = null;

        // Retrieve the list of meta contacts eligible for display
        List<MetaContact> metaContacts = metaContactListService.getDisplayableContactList();

        if (metaContacts != null)
        {
            // Filter meta contacts to include only those associated with business groups
            List<MetaContact> BGMetaContacts = new ArrayList<>();
            for (MetaContact metaContact : metaContacts)
            {
                if (metaContact != null && metaContact.getBGContact() != null)
                {
                    BGMetaContacts.add(metaContact);
                }
            }

            for (MetaContact metaContact : BGMetaContacts)
            {
                // Retrieve contacts associated with the provided phone number
                List<Contact> CPNContacts = metaContact.getContactByPhoneNumber(number);
                if (CPNContacts != null)
                {
                    for (Contact contact : CPNContacts)
                    {
                        if (contact != null)
                        {
                            CPNContact = contact;
                            break;
                        }
                    }
                }
            }
        }
        return CPNContact;
    }

    private CPDataSenderCallback generateOutgoingCallerIDPatchRequestCallback()
    {
        return new CPDataSenderCallback()
        {
            @Override
            public String getData(String siName)
            {
                OutgoingCallerIDType selectedItem = mOutgoingCallerIDComboBoxModel.getSelectedItem();
                Map<String, String> requestBody = Map.of(CALLER_ID_PROPERTY_NAME, selectedItem.getValue());
                return new JSONObject(requestBody).toJSONString();
            }

            @Override
            public String getCommPortalVersion()
            {
                return commPortalService.getLatestVersion().toString();
            }

            @Override
            public boolean onDataSent(String result)
            {
                OutgoingCallerIDType previousOutgoingCallerIDType = mOutgoingCallerIDComboBoxModel.getPreviousItem();
                OutgoingCallerIDType selectedOutgoingCallerIDType = mOutgoingCallerIDComboBoxModel.getSelectedItem();
                String selectedValue = selectedOutgoingCallerIDType.getValue();
                String previousValue = previousOutgoingCallerIDType.getValue();
                logger.info("Post request to update outgoing caller ID was successful. " +
                            "Updated value is: " + selectedValue + ". Storing to config.");
                configService.user().setProperty(
                        ClassOfServiceService.CONFIG_CALLER_ID_FOR_OUTBOUND_CALLS,
                        selectedValue);

                GeneralConfigPluginActivator
                        .getInsightsService()
                        .logEvent(InsightsEventHint.GENERAL_CONFIG_PLUGIN_CALLER_ID_SETTING.name(),
                                  Map.of(GeneralConfigPluginParameterInfo.OUTGOING_CALLER_ID_OLD.name(),
                                         previousValue,
                                         GeneralConfigPluginParameterInfo.OUTGOING_CALLER_ID_NEW.name(),
                                         selectedValue));
                return true;
            }

            @Override
            public DataFormat getDataFormat()
            {
                return DataFormat.DATA_JS;
            }

            @Override
            public String getSIName()
            {
                return META_SUBSCRIBER_BASE_INFORMATION;
            }

            @Override
            public void onDataError(CPDataError error)
            {
                GeneralConfigPluginActivator
                        .getInsightsService()
                        .logEvent(InsightsEventHint.GENERAL_CONFIG_PLUGIN_CALLER_ID_SETTING.name(),
                                  Map.of(GeneralConfigPluginParameterInfo.OUTGOING_CALLER_ID_ERROR.name(),
                                         error));
                logger.error("Failed to patch caller ID value. Error: " + error);
            }
        };
    }

    private JPanel createOutgoingCallerIdPanel()
    {
        TransparentPanel outgoingCallerIDPanel = new TransparentPanel();
        outgoingCallerIDPanel.setLayout(new GridLayout(2, 1));

        TransparentPanel outgoingCallerIDComboBoxPanel = new TransparentPanel();
        FlowLayout outgoingCallerIDComboBoxPanelLayout = new FlowLayout(FlowLayout.LEFT);
        outgoingCallerIDComboBoxPanelLayout.setAlignOnBaseline(true);
        outgoingCallerIDComboBoxPanel.setLayout(outgoingCallerIDComboBoxPanelLayout);
        outgoingCallerIDComboBoxPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        JLabel outgoingCallerIDLabel = new JLabel(Resources.getResources().getI18NString(
                "service.gui.OUTGOING_CALLER_ID"));
        outgoingCallerIDLabel.setForeground(TEXT_COLOR);
        ScaleUtils.scaleFontAsDefault(outgoingCallerIDLabel);
        mOutgoingCallerIDComboBox = new JComboBox<>();
        mOutgoingCallerIDComboBox.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        mOutgoingCallerIDComboBox.setRenderer(createOutgoingCallerIDComboBoxRenderer());

        outgoingCallerIDComboBoxPanel.add(outgoingCallerIDLabel);
        outgoingCallerIDComboBoxPanel.add(mOutgoingCallerIDComboBox);

        outgoingCallerIDPanel.add(outgoingCallerIDComboBoxPanel);
        JTextArea outgoingCallerIDHint = createHintPanel("service.gui.OUTGOING_CALLER_ID_HINT");
        outgoingCallerIDPanel.add(outgoingCallerIDHint);

        return outgoingCallerIDPanel;
    }

    private DefaultListCellRenderer createOutgoingCallerIDComboBoxRenderer()
    {
        return new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
            {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof OutgoingCallerIDType)
                {
                    OutgoingCallerIDType key = (OutgoingCallerIDType) value;
                    setText(mOutgoingCallerIDComboBoxModel.getStringValue(key));
                }
                return this;
            }
        };
    }

    /**
     * @return the call rating control component
     */
    private Component createCallRatingComponent()
    {
        String text = Resources.getResources()
                               .getI18NString("service.gui.SHOW_CALL_RATING");
        final JCheckBox checkBox = new SIPCommCheckBox(text);
        checkBox.setForeground(TEXT_COLOR);
        checkBox.setSelected(ConfigurationUtils.showCallRating());

        // Listen for changes to the config that controls whether call rating dialog
        // should be shown. If settings was opened at the moment call ended, and user
        // selected "Don't ask again" option, we want to update it here right away.
        configService.user().addPropertyChangeListener(CALL_RATING_SHOW_PROP, event -> {
            logger.debug("Show call rating config updated");
            checkBox.setSelected(ConfigurationUtils.showCallRating());
        });

        checkBox.addActionListener((ActionEvent e) ->
        {
            boolean selected = checkBox.isSelected();
            logger.user("Call rating component " + selected + " selected");
            ConfigurationUtils.setShowCallRating(selected);
            GeneralConfigPluginActivator
                    .getInsightsService()
                    .logEvent(InsightsEventHint.GENERAL_CONFIG_PLUGIN_ENABLE_CALL_RATING.name(),
                              Map.of(
                                      CommonParameterInfo.NEW_VALUE.name(), selected
                              ));
        });

        return checkBox;
    }

    /**
     * Create the config panel used to control the Call Park Op Set
     *
     * @param callParkOpSet The operation set that this panel controls
     * @return the created config panel
     */
    private Component createCallParkConfigPanel(final OperationSetCallPark callParkOpSet)
    {
        final JCheckBox callParkCheckBox = new SIPCommCheckBox();
        callParkCheckBox.setText(
                     Resources.getString("service.gui.CALL_PARK_OPTIONS_TEXT"));
        callParkCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        callParkCheckBox.setSelected(callParkOpSet.isEnabled());
        callParkCheckBox.setForeground(TEXT_COLOR);

        callParkCheckBox.addActionListener((ActionEvent e) ->
        {
            boolean enabled = !callParkOpSet.isEnabled();

            logger.user("Call park checkbox toggled to: " + enabled);

            // Disable the call park check box so that the user can't
            // spam the option.
            callParkCheckBox.setEnabled(false);
            callParkOpSet.setEnabled(enabled);
        });

        // Add the components to a container
        final TransparentPanel container = new TransparentPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        container.add(callParkCheckBox);
        container.add(createHintPanel("service.gui.CALL_PARK_OPTIONS_HINT"));
        container.setVisible(callParkOpSet.isCallParkAvailable());

        // Add a listener for changes to the config
        callParkOpSet.registerListener(new CallParkListener()
        {
            @Override
            public void onOrbitStateChanged(CallParkOrbit orbit, CallParkOrbitState oldState) {}

            @Override
            public void onOrbitListChanged() {}

            @Override
            public void onCallParkEnabledChanged()
            {
                // Re-enable the check-box
                callParkCheckBox.setEnabled(true);
                callParkCheckBox.setSelected(callParkOpSet.isEnabled());
            }

            @Override
            public void onCallParkAvailabilityChanged()
            {
                container.setVisible(callParkOpSet.isCallParkAvailable());
            }
        });

        return container;
    }

    /**
     * Initializes the direct calling panel, that enables the user to turn direct calling on/off.
     * @return the created checkbox
     */
    private Component createDirectCallingPanel()
    {
        final boolean voipEnabledByUser = ConfigurationUtils.isVoIPEnabledByUser();

        String buttonResource = voipEnabledByUser ?
            "service.gui.VOIP_DISABLE_BUTTON" : "service.gui.VOIP_ENABLE_BUTTON";

        final JButton noVoipButton =
                new SIPCommBasicTextButton(Resources.getString(buttonResource));

        noVoipButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        noVoipButton.addActionListener((ActionEvent e) ->
        {
            SIPCommConfirmDialog dialog = getSipCommConfirmDialog(voipEnabledByUser);
            if (dialog.showDialog())
            {
                logger.user("Direct calling enabled set to " + !voipEnabledByUser);
                configService.user().setProperty(
                    ProvisioningServiceImpl.VOIP_ENABLED_PROP, !voipEnabledByUser);
                GeneralConfigPluginActivator.getResetService().setRestart(true);
                GeneralConfigPluginActivator.getShutdownService().beginShutdown();
            }
            else
            {
                logger.user("'Direct calling enabled' dialog quit");
                GeneralConfigPluginActivator.getResetService().setRestart(false);
            }
        });

        ConfigSectionPanel noVoipPanel = new ConfigSectionPanel(true);
        noVoipPanel.addPanel(noVoipButton);
        noVoipPanel.addPanel(createHintPanel("service.gui.VOIP_HINT"));

        return noVoipPanel;
    }

    private SIPCommConfirmDialog getSipCommConfirmDialog(boolean voipEnabledByUser)
    {
        String dialogTextRes;
        String dialogButtonRes;

        if (voipEnabledByUser)
        {
            dialogTextRes = "service.gui.VOIP_DISABLE_DIALOG_TEXT";
            dialogButtonRes = "service.gui.VOIP_DISABLE_DIALOG_CONFIRM";
        }
        else
        {
            dialogTextRes = "service.gui.VOIP_ENABLE_DIALOG_TEXT";
            dialogButtonRes = "service.gui.VOIP_ENABLE_DIALOG_CONFIRM";
        }

        return new SIPCommConfirmDialog("service.gui.VOIP_DIALOG_TITLE",
                                        dialogTextRes,
                                        dialogButtonRes,
                                        "service.gui.CANCEL");
    }

    private static class OutgoingCallerIDComboBoxModel extends DefaultComboBoxModel<OutgoingCallerIDType>
    {
        /**
         * Serial version UID
         */
        private static final long serialVersionUID = 1L;
        private final Map<OutgoingCallerIDType, String> comboBoxOptions;
        private OutgoingCallerIDType previousItem = null;

        public OutgoingCallerIDComboBoxModel(Map<OutgoingCallerIDType, String> comboBoxOptions)
        {
            this.comboBoxOptions = comboBoxOptions;
            for (OutgoingCallerIDType key : comboBoxOptions.keySet())
            {
                addElement(key);
            }
        }

        @Override
        public void setSelectedItem(Object outgoingCallerIDType)
        {
            this.previousItem = getSelectedItem();
            super.setSelectedItem(outgoingCallerIDType);
        }

        @Override
        public OutgoingCallerIDType getSelectedItem()
        {
            return (OutgoingCallerIDType) super.getSelectedItem();
        }

        public String getStringValue(OutgoingCallerIDType key)
        {
            return comboBoxOptions.get(key);
        }

        public OutgoingCallerIDType getPreviousItem()
        {
            return previousItem;
        }
    }
}
