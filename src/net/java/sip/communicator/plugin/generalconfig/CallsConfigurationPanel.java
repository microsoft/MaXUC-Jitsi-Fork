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
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.ConfigSectionPanel;
import net.java.sip.communicator.plugin.desktoputil.OptionListConfigurationPanel;
import net.java.sip.communicator.plugin.desktoputil.SIPCommBasicTextButton;
import net.java.sip.communicator.plugin.desktoputil.SIPCommCheckBox;
import net.java.sip.communicator.plugin.desktoputil.SIPCommConfirmDialog;
import net.java.sip.communicator.plugin.desktoputil.TransparentPanel;
import net.java.sip.communicator.plugin.provisioning.ProvisioningServiceImpl;
import net.java.sip.communicator.service.commportal.CPCos;
import net.java.sip.communicator.service.commportal.CPCosGetterCallback;
import net.java.sip.communicator.service.commportal.ClassOfServiceService;
import net.java.sip.communicator.service.commportal.CommPortalService;
import net.java.sip.communicator.service.protocol.OperationSetCallPark;
import net.java.sip.communicator.service.protocol.OperationSetCallPark.CallParkListener;
import net.java.sip.communicator.service.protocol.OperationSetCallPark.CallParkOrbit;
import net.java.sip.communicator.service.protocol.OperationSetCallPark.CallParkOrbitState;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import org.jitsi.service.configuration.ConfigurationService;

/**
 * The general configuration form.
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

    /**
     * The configuration service
     */
    private static final ConfigurationService configService =
        GeneralConfigPluginActivator.getConfigurationService();

    /**
     * The CoS service
     */
    private static final ClassOfServiceService cosService =
        GeneralConfigPluginActivator.getCosService();

    /**
     * The CommPortal service
     */
    private static final CommPortalService commPortalService =
            GeneralConfigPluginActivator.getCommPortalService();

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

        // Set whether the call config panel is visible based on whether VoIP
        // is enabled in the CoS.
        cosService.getClassOfService(new CPCosGetterCallback()
        {
            @Override
            public void onCosReceived(CPCos cos)
            {
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
            }
        });
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

        checkBox.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                boolean selected = checkBox.isSelected();
                logger.user("Call rating component " + selected + " selected");
                ConfigurationUtils.setShowCallRating(selected);
            }
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

        callParkCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                boolean enabled = !callParkOpSet.isEnabled();

                logger.user("Call park checkbox toggled to: " + enabled);

                // Disable the call park check box so that the user can't
                // spam the option.
                callParkCheckBox.setEnabled(false);
                callParkOpSet.setEnabled(enabled);
            }
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
     * @return the created check box
     */
    private Component createDirectCallingPanel()
    {
        final boolean voipEnabledByUser = ConfigurationUtils.isVoIPEnabledByUser();

        String buttonResource = voipEnabledByUser ?
            "service.gui.VOIP_DISABLE_BUTTON" : "service.gui.VOIP_ENABLE_BUTTON";

        final JButton noVoipButton =
                new SIPCommBasicTextButton(Resources.getString(buttonResource));

        noVoipButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        noVoipButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
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

                SIPCommConfirmDialog dialog =
                    new SIPCommConfirmDialog("service.gui.VOIP_DIALOG_TITLE",
                                             dialogTextRes,
                                             dialogButtonRes,
                                             "service.gui.CANCEL");
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
            }
        });

        ConfigSectionPanel noVoipPanel = new ConfigSectionPanel(true);
        noVoipPanel.addPanel(noVoipButton);
        noVoipPanel.addPanel(createHintPanel("service.gui.VOIP_HINT"));

        return noVoipPanel;
    }
}
