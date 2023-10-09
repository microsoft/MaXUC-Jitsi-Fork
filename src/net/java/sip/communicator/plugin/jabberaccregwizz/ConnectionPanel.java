/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.certificate.*;

import org.jitsi.util.*;

/**
 *
 * @author Yana Stamcheva
 */
public class ConnectionPanel
    extends TransparentPanel
    implements ValidatingPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private final TransparentPanel mainPanel = new TransparentPanel();

    private final JPanel advancedOpPanel
        = new TransparentPanel(new BorderLayout(10, 10));

    private final JPanel serverOpPanel
            = new TransparentPanel(new BorderLayout(10, 10));

    private final JPanel labelsAdvOpPanel
        = new TransparentPanel(new GridLayout(0, 1, 10, 10));

    private final JPanel valuesAdvOpPanel
        = new TransparentPanel(new GridLayout(0, 1, 10, 10));

    private final JCheckBox sendKeepAliveBox = new SIPCommCheckBox(
        Resources.getString("plugin.jabberaccregwizz.ENABLE_KEEP_ALIVE"));

    private final JLabel resourceLabel
        = new JLabel(Resources.getString("plugin.jabberaccregwizz.RESOURCE"));

    private final JTextField resourceField
        = new JTextField(JabberAccountRegistration.DEFAULT_RESOURCE);

    private final JLabel priorityLabel = new JLabel(
        Resources.getString("plugin.jabberaccregwizz.PRIORITY"));

    private final JTextField priorityField
        = new JTextField(JabberAccountRegistration.DEFAULT_PRIORITY);

    private final JCheckBox serverAutoCheckBox = new SIPCommCheckBox(
            Resources.getString(
                "plugin.jabberaccregwizz.OVERRIDE_SERVER_DEFAULT_OPTIONS"),
                JabberAccountRegistration.DEFAULT_RESOURCE_AUTOGEN);

    private final JLabel serverLabel
        = new JLabel(Resources.getString("plugin.jabberaccregwizz.SERVER"));

    private final JTextField serverField = new JTextField();

    private final JLabel portLabel
        = new JLabel(Resources.getString("service.gui.PORT"));

    private final JTextField portField
        = new JTextField(JabberAccountRegistration.DEFAULT_PORT);

    private final JCheckBox autoGenerateResource = new SIPCommCheckBox(
            Resources.getString("plugin.jabberaccregwizz.AUTORESOURCE"),
                JabberAccountRegistration.DEFAULT_RESOURCE_AUTOGEN);

    private JComboBox<String> dtmfMethodBox = new JComboBox<>(new String[]
                                                                      {
                                                                              Resources.getString(
                                                                                      "plugin.jabberaccregwizz.DTMF_AUTO"),
                                                                              Resources.getString(
                                                                                      "plugin.sipaccregwizz.DTMF_RTP"),
                                                                              Resources.getString(
                                                                                      "plugin.sipaccregwizz.DTMF_INBAND")
                                                                      });

    /**
     * The text field used to change the DTMF minimal tone duration.
     */
    private JTextField dtmfMinimalToneDurationValue = new JTextField();

    private final JLabel certificateLabel = new JLabel(
            Resources.getString("plugin.sipaccregwizz.CLIENT_CERTIFICATE"));

    private final JComboBox<Object> certificate = new JComboBox<>();

    private final JabberAccountRegistrationForm parentForm;

    /**
     * Creates an instance of <tt>ConnectionPanel</tt> by specifying the parent
     * wizard, where it's contained.
     * @param parentForm the parent form
     */
    public ConnectionPanel(JabberAccountRegistrationForm parentForm)
    {
        super(new BorderLayout());

        this.parentForm = parentForm;
        parentForm.addValidatingPanel(this);

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        portField.getDocument().addDocumentListener(new DocumentListener()
        {
            public void changedUpdate(DocumentEvent evt)
            {
            }

            public void insertUpdate(DocumentEvent evt)
            {
                setNextButtonAccordingToPortAndPriority();
            }

            public void removeUpdate(DocumentEvent evt)
            {
                setNextButtonAccordingToPortAndPriority();
            }
        });

        priorityField.getDocument().addDocumentListener(new DocumentListener()
        {
            public void changedUpdate(DocumentEvent evt)
            {
            }

            public void insertUpdate(DocumentEvent evt)
            {
                setNextButtonAccordingToPortAndPriority();
            }

            public void removeUpdate(DocumentEvent evt)
            {
                setNextButtonAccordingToPortAndPriority();
            }
        });

        serverAutoCheckBox.addActionListener(new ActionListener()
        {
            /**
             * Invoked when an action occurs.
             */
            public void actionPerformed(ActionEvent e)
            {
                enablesServerAutoConfigure(serverAutoCheckBox.isSelected());
            }
        });
        serverAutoCheckBox.setSelected(
            parentForm.getRegistration().isServerOverridden());
        enablesServerAutoConfigure(serverAutoCheckBox.isSelected());

        labelsAdvOpPanel.add(serverLabel);
        labelsAdvOpPanel.add(portLabel);
        labelsAdvOpPanel.add(certificateLabel);

        valuesAdvOpPanel.add(serverField);
        valuesAdvOpPanel.add(portField);
        valuesAdvOpPanel.add(certificate);
        initCertificateAliases(null);

        serverOpPanel.add(serverAutoCheckBox, BorderLayout.NORTH);
        serverOpPanel.add(labelsAdvOpPanel, BorderLayout.WEST);
        serverOpPanel.add(valuesAdvOpPanel, BorderLayout.CENTER);
        serverOpPanel.setBorder(BorderFactory.createTitledBorder(
                Resources.getString("plugin.jabberaccregwizz.SERVER_OPTIONS")));

        JPanel checkBoxesPanel
            = new TransparentPanel(new GridLayout(0, 1, 10, 10));
        //checkBoxesPanel.add(sendKeepAliveBox);

        final JPanel resourcePanel
                = new TransparentPanel(new BorderLayout(10, 10));
        resourcePanel.setBorder(BorderFactory.createTitledBorder(
            Resources.getString("plugin.jabberaccregwizz.RESOURCE")));
        resourcePanel.add(autoGenerateResource, BorderLayout.NORTH);
        JPanel resSubPanelLabel =
            new TransparentPanel(new GridLayout(0, 1, 10, 10));
        JPanel resSubPanelValue =
            new TransparentPanel(new GridLayout(0, 1, 10, 10));
        resSubPanelLabel.add(resourceLabel);
        resSubPanelLabel.add(priorityLabel);
        resSubPanelValue.add(resourceField);
        resSubPanelValue.add(priorityField);
        resourcePanel.add(resSubPanelLabel, BorderLayout.WEST);
        resourcePanel.add(resSubPanelValue, BorderLayout.CENTER);

        // default for new account
        advancedOpPanel.add(checkBoxesPanel, BorderLayout.NORTH);
        advancedOpPanel.add(serverOpPanel, BorderLayout.CENTER);
        advancedOpPanel.add(resourcePanel, BorderLayout.SOUTH);

        if(autoGenerateResource.isSelected())
        {
            resourceField.setEnabled(false);
        }
        autoGenerateResource.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                resourceField.setEnabled(!autoGenerateResource.isSelected());
            }
        });

        mainPanel.add(advancedOpPanel);

        mainPanel.add(createDTMFPanel());

        String serverAddress = parentForm.getServerAddress();
        if (!StringUtils.isNullOrEmpty(serverAddress))
            serverField.setText(serverAddress);

        add(mainPanel, BorderLayout.NORTH);
    }

    /**
     * Creates the DTMF panel.
     * @return the created DTMF panel
     */
    private Component createDTMFPanel()
    {
        JPanel emptyLabelPanel = new TransparentPanel();

        // Labels.
        JPanel dtmfLabels = new TransparentPanel(new GridLayout(0, 1, 5, 5));
        JLabel dtmfMethodLabel = new JLabel(
            Resources.getString("plugin.sipaccregwizz.DTMF_METHOD"));
        JLabel minimalDTMFToneDurationLabel = new JLabel(
            Resources.getString(
                "plugin.sipaccregwizz.DTMF_MINIMAL_TONE_DURATION"));
        dtmfLabels.add(dtmfMethodLabel);
        dtmfLabels.add(minimalDTMFToneDurationLabel);
        dtmfLabels.add(emptyLabelPanel);

        // Values
        JPanel dtmfValues = new TransparentPanel(new GridLayout(0, 1, 5, 5));
        dtmfMethodBox.addItemListener(new ItemListener()
                {
                    public void itemStateChanged(ItemEvent e)
                    {
                        boolean isEnabled = false;
                        String selectedItem
                            = (String) dtmfMethodBox.getSelectedItem();
                        if(selectedItem != null
                            && (selectedItem.equals(Resources.getString(
                                    "plugin.sipaccregwizz.DTMF_AUTO"))
                                || selectedItem.equals(Resources.getString(
                                        "plugin.sipaccregwizz.DTMF_RTP")))
                          )
                        {
                            isEnabled = true;
                        }
                        dtmfMinimalToneDurationValue.setEnabled(isEnabled);
                    }
                });
        dtmfMethodBox.setSelectedItem(
                parentForm.getRegistration().getDefaultDTMFMethod());
        dtmfMinimalToneDurationValue.setText(
                JabberAccountRegistration.DEFAULT_MINIMAL_DTMF_TONE_DURATION);
        JLabel dtmfMinimalToneDurationExampleLabel = new JLabel(
                Resources.getString(
                    "plugin.sipaccregwizz.DTMF_MINIMAL_TONE_DURATION_INFO"));
        dtmfMinimalToneDurationExampleLabel.setForeground(Color.GRAY);
        dtmfMinimalToneDurationExampleLabel.setFont(
                dtmfMinimalToneDurationExampleLabel.getFont().deriveFont(
                    ScaleUtils.getSmallFontSize()));
        dtmfMinimalToneDurationExampleLabel.setMaximumSize(
                new ScaledDimension(40, 35));
        dtmfMinimalToneDurationExampleLabel.setBorder(
                ScaleUtils.createEmptyBorder(0, 0, 8, 0));
        dtmfValues.add(dtmfMethodBox);
        dtmfValues.add(dtmfMinimalToneDurationValue);
        dtmfValues.add(dtmfMinimalToneDurationExampleLabel);

        // DTMF panel
        JPanel dtmfPanel = new TransparentPanel(new BorderLayout(10, 10));
        dtmfPanel.setBorder(BorderFactory.createTitledBorder(
            Resources.getString("plugin.sipaccregwizz.DTMF")));
        dtmfPanel.add(dtmfLabels, BorderLayout.WEST);
        dtmfPanel.add(dtmfValues, BorderLayout.CENTER);

        return dtmfPanel;
    }

    /**
     * Returns the server address.
     * @return the server address
     */
    String getServerAddress()
    {
        return serverField.getText();
    }

    /**
     * Sets the server address.
     * @param serverAddress the server address
     */
    void setServerAddress(String serverAddress)
    {
        serverField.setText(serverAddress);
    }

    /**
     * Returns the server port.
     * @return the server port
     */
    String getServerPort()
    {
        return portField.getText();
    }

    /**
     * Returns the resource.
     * @return the resource
     */
    String getResource()
    {
        return resourceField.getText();
    }

    /**
     * Returns the priority field value.
     * @return the priority field value
     */
    String getPriority()
    {
        return priorityField.getText();
    }

    /**
     * Returns <tt>true</tt> if the "send keep alive" check box is selected,
     * otherwise returns <tt>false</tt>.
     * @return <tt>true</tt> if the "send keep alive" check box is selected,
     * otherwise returns <tt>false</tt>
     */
    boolean isSendKeepAlive()
    {
        return sendKeepAliveBox.isSelected();
    }

    /**
     * Disables Next Button if Port field value is incorrect
     */
    private void setNextButtonAccordingToPortAndPriority()
    {
        try
        {
            Integer.parseInt(getServerPort());
            Integer.parseInt(getPriority());
            parentForm.reValidateInput();
        }
        catch (NumberFormatException ex)
        {
            parentForm.reValidateInput();
        }
    }

    /**
     * Whether current inserted values into the panel are valid and enough
     * to continue with account creation/modification.
     * Checks whether proxy field values are ok to continue with
     * account creating.
     *
     * @return whether the input values are ok to continue with account
     * creation/modification.
     */
    public boolean isValidated()
    {
        return true;
    }

    /**
     * Is resource auto generate enabled. Returns checkbox state.
     * @return is resource auto generate enabled.
     */
    boolean isAutogenerateResourceEnabled()
    {
        return autoGenerateResource.isSelected();
    }

    /**
     * Returns the DTMF method.
     * @return the DTMF method
     */
    String getDTMFMethod()
    {
        Object selItem = dtmfMethodBox.getSelectedItem();

        // By default sets DTMF mezthod to auto.
        if(selItem == null)
        {
            return null;
        }

        String selString = selItem.toString();
        if(selString.equals(
                    Resources.getString("plugin.sipaccregwizz.DTMF_RTP")))
        {
            return "RTP_DTMF";
        }
        else if(selString.equals(
                    Resources.getString("plugin.sipaccregwizz.DTMF_INBAND")))
        {
            return "INBAND_DTMF";
        }
        else
        {
            return "AUTO_DTMF";
        }
    }

    /**
     * Returns the minimal DTMF tone duration.
     *
     * @return The minimal DTMF tone duration.
     */
    String getDtmfMinimalToneDuration()
    {
        return dtmfMinimalToneDurationValue.getText();
    }

    /**
     * Return <tt>isServerOverridden</tt> property.
     * @return <tt>isServerOverridden</tt> property.
     */
    boolean isServerOverridden()
    {
        return this.serverAutoCheckBox.isSelected();
    }

    /**
     * Enables/disables the proxy auto-configuration.
     * @param isEnable <tt>true</tt> to enable proxy auto-configuration,
     * <tt>false</tt> - otherwise
     */
    void enablesServerAutoConfigure(boolean isEnable)
    {
        serverAutoCheckBox.setSelected(isEnable);
        serverField.setEnabled(isEnable);
        portField.setEnabled(isEnable);
        parentForm.reValidateInput();
    }

    /**
     * Gets the ID of the selected client TLS certificate or <tt>null</tt> if no
     * certificate is selected.
     *
     * @return the ID of the selected client TLS certificate or <tt>null</tt> if
     *         no certificate is selected.
     */
    String getClientTlsCertificateId()
    {
        if(certificate.getSelectedItem() != null
            && certificate.getSelectedItem() instanceof CertificateConfigEntry)
            return ((CertificateConfigEntry)certificate.getSelectedItem())
                    .getId();
        return null;
    }

    /**
     * Initializes the certificate combo box with certificate names
     *
     * @param id ID of the selected certificate
     *           (can be null if no certificate is selected).
     */
    private void initCertificateAliases(String id)
    {
        certificate.removeAllItems();
        certificate.insertItemAt(
                Resources.getString("plugin.sipaccregwizz.NO_CERTIFICATE"), 0);
        certificate.setSelectedIndex(0);
        for(CertificateConfigEntry e : JabberAccRegWizzActivator
                .getCertificateService().getClientAuthCertificateConfigs())
        {
            certificate.addItem(e);
            if(e.getId().equals(id))
                certificate.setSelectedItem(e);
        }
    }
}
