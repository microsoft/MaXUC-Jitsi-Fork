// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main;

import static org.jitsi.util.Hasher.logHasher;
import static org.jitsi.util.SanitiseUtils.sanitiseNullable;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import org.jitsi.service.configuration.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.UrlServiceTools.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.commportal.*;
import net.java.sip.communicator.service.conference.ConferenceService;
import net.java.sip.communicator.util.*;

/**
 * The configuration panel for UC services (including URL triggers).
 * Consists of a list of services on the left, and the configuration for each
 * on the right.
 */
public class UcServicesConfigPanel
    extends ConfigurationPanel
    implements ListSelectionListener
{
    /**
     * Colors of the different items in the left-hand menu.
     * Selected: white text on a blue background.
     * Unselected: black text on a white background.
     * Unselectable: grey text on white background.
     */
    private static final Color SELECTED_FOREGROUND = new Color(0xFFFFFF);
    private static final Color SELECTED_BACGROUND = new Color(0x60A0FF);
    private static final Color UNSELECTED_FOREGROUND = new Color(0x000000);
    private static final Color UNSELECTED_BACKGROUND = new Color(0xFFFFFF);
    private static final Color UNSELECTABLE_FOREGROUND = new Color(0x808080);
    private static final Color UNSELECTABLE_BACKGROUND = new Color(0xFFFFFF);

    /**
     * Serial version UID
     */
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>Logger</tt> used by this <tt>UcServicesConfigPanel</tt> for
     * logging output.
     */
    private final Logger logger = Logger.getLogger(UcServicesConfigPanel.class);

    /**
     * The CoS service
     */
    private static final ClassOfServiceService cosService =
                                                   GuiActivator.getCosService();

    /**
     * The configuration list. A JList of UI elements.
     */
    private final JList<UrlServiceLeftMenuElement> configList = new JList<>();

    /**
     * The config panels (one for each service).
     */
    private EnumMap<ServiceType, UrlServiceConfigPanel> configPanels;

    /**
     * The center panel.
     */
    private final JPanel centerPanel = new TransparentPanel(new BorderLayout());

    /**
     * The <tt>UrlServiceTools</tt> handle.
     */
    private final UrlServiceTools urlServiceTools;

    /**
     * The configuration service
     */
    private static final ConfigurationService configService =
                                         GuiActivator.getConfigurationService();
    /*
     * Config options for the auto launch
     */
    private static final String AUTO_LAUNCH_ALWAYS = GuiActivator.getResources().getI18NString("service.gui.UC_SERVICES.CRM.AUTO_LAUNCH_ALWAYS");
    private static final String AUTO_LAUNCH_NEVER = GuiActivator.getResources().getI18NString("service.gui.UC_SERVICES.CRM.AUTO_LAUNCH_NEVER");
    private static final String AUTO_LAUNCH_EXTERNAL = GuiActivator.getResources().getI18NString("service.gui.UC_SERVICES.CRM.AUTO_LAUNCH_EXTERNAL");
    private static final String AUTO_LAUNCH_ALWAYS_CONF = GuiActivator.getResources().getI18NString("service.gui.UC_SERVICES.CRM.AUTO_LAUNCH_ALWAYS_CONF");
    private static final String AUTO_LAUNCH_EXTERNAL_CONF = GuiActivator.getResources().getI18NString("service.gui.UC_SERVICES.CRM.AUTO_LAUNCH_EXTERNAL_CONF");

    /**
     *  Last time a service was selected, or zero if first time through
     *  (if test will always succeed first time).
     *  Used to ensure logs are not unnecessarily duplicated.
     */
    private long selectionTime;

    /**
     * Creates an instance of the <tt>UcServicesConfigPanel</tt>.
     */
    public UcServicesConfigPanel()
    {
        super(new BorderLayout(10, 0));

        urlServiceTools = UrlServiceTools.getUrlServiceTools();

        initList();

        centerPanel.setPreferredSize(new ScaledDimension(500, 250));

        add(centerPanel, BorderLayout.CENTER);
    }

    /**
     * Initializes the config list.
     */
    private void initList()
    {
        configList.setModel(new DefaultListModel<>());
        ListCellRenderer<UrlServiceLeftMenuElement> listCellRenderer =
            new ListCellRenderer<UrlServiceLeftMenuElement>()
            {
                @Override
                public Component getListCellRendererComponent(
                    javax.swing.JList<? extends UrlServiceLeftMenuElement> list,
                    UrlServiceLeftMenuElement value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus)
                {
                    Component component =
                                 value.getListCellRendererComponent(isSelected);

                    if (component.isVisible())
                    {
                        return component;
                    }
                    else
                    {
                        return null;
                    }
                }
            };

        configList.setCellRenderer(listCellRenderer);
        configList.addListSelectionListener(this);
        configList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane configScrollList = new JScrollPane();

        configScrollList.getVerticalScrollBar().setUnitIncrement(30);
        configScrollList.getViewport().add(configList);
        add(configScrollList, BorderLayout.WEST);

        DefaultListModel<UrlServiceLeftMenuElement> listModel =
            (DefaultListModel<UrlServiceLeftMenuElement>) configList.getModel();

        // Add the different forms.  Start with the heading.
        UrlServiceHeading heading = new UrlServiceHeading(GuiActivator.
               getResources().getI18NString("service.gui.UC_SERVICES.HEADING"));
        listModel.addElement(heading);

        configPanels =
                new EnumMap<>(ServiceType.class);
        for (ServiceType serviceType : ServiceType.values())
        {
            if (serviceType.shouldAppearInConfigPanel())
            {
                UrlServiceConfigPanel configPanel =
                                     serviceType.getUrlServiceConfigPanel(this);
                configPanels.put(serviceType, configPanel);
            }
        }

        updateConfigPanelVisibility();
        UrlServiceTools.getUrlServiceTools().registerConfigPanel(this);
    }

    /**
     * Show or hide the various different config panel options, depending on
     * which services are enabled.
     */
    public void updateConfigPanelVisibility()
    {
        DefaultListModel<UrlServiceLeftMenuElement> listModel =
            (DefaultListModel<UrlServiceLeftMenuElement>) configList.getModel();

        // First remove all items from the list, apart from the heading. Start
        // at the end so we don't get confused as items are removed.
        for (int ix = listModel.getSize() - 1; ix >= 1; ix--)
        {
            listModel.remove(ix);
        }

        // Add in all the services which should appear in the config panel and
        // are currently enabled. Set the first enabled service as selected.
        boolean itemAdded = false;
        UrlServiceTools ust = UrlServiceTools.getUrlServiceTools();
        for (ServiceType serviceType : ServiceType.values())
        {
            if (ust.isServiceTypeEnabled(serviceType) &&
                serviceType.shouldAppearInConfigPanel())
            {
                logger.debug("Adding enabled serviceType" + serviceType);
                UrlServiceConfigPanel panel = configPanels.get(serviceType);
                listModel.addElement(panel);

                if (!itemAdded)
                {
                    configList.setSelectedValue(panel, true);
                    itemAdded = true;
                }

                // While we're here, update the URL chooser drop-down too.
                panel.addUrlChooser(serviceType);
            }
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        @SuppressWarnings("unchecked")
        JList<UrlServiceLeftMenuElement> confList =
                                (JList<UrlServiceLeftMenuElement>)e.getSource();
        UrlServiceLeftMenuElement element = confList.getSelectedValue();
        logger.debug("Selected element: " + element);

        if (element != null) // Can be null while we're removing items
        {
            element.elementSelected();
        }
    }

    /**
     * Interface implemented by selectable and unselectable elements in the left
     * hand menu.
     */
    private interface UrlServiceLeftMenuElement
    {
        /**
         * Get the correctly rendered component from the left hand menu.
         * @param isSelected Whether the element has been selected.
         * @return the element.
         */
        Component getListCellRendererComponent(boolean isSelected);

        /**
         * Called when this element has been selected.  The element should then
         * draw the appropriate right-hand menu.
         */
        void elementSelected();
    }

    /**
     * Left-hand menu element representing a heading - i.e. unselectable and has
     * no corresponding right-hand menu.
     */
    private class UrlServiceHeading implements UrlServiceLeftMenuElement
    {
        private JLabel serviceLabel;

        public UrlServiceHeading(String text)
        {
            serviceLabel = new JLabel();
            serviceLabel.setFocusable(false);
            serviceLabel.setEnabled(false);
            serviceLabel.setOpaque(true);
            serviceLabel.setText(text);

            // Make the heading text smaller to distinguish it further from the
            // regular headings.
            serviceLabel.setFont(getFont().deriveFont(
                ScaleUtils.getMediumFontSize()));

            // Unselectable element always looks the same so set the colors now.
            Color fcolor = UNSELECTABLE_FOREGROUND;
            Color bcolor = UNSELECTABLE_BACKGROUND;

            serviceLabel.setForeground(fcolor);
            serviceLabel.setBackground(bcolor);
            serviceLabel.setBorder(BorderFactory.createLineBorder(bcolor, 8));
        }

        @Override
        public Component getListCellRendererComponent(boolean isSelected)
        {
            // Unselectable element always looks the same.
            return serviceLabel;
        }

        @Override
        public void elementSelected()
        {
            // Heading has no content on right - just ensure right is blank
            centerPanel.removeAll();
        }
    }

    /**
     * Left-hand menu element representing a selectable URL service.
     */
    public class UrlServiceConfigPanel
        extends ConfigurationPanel
        implements UrlServiceLeftMenuElement, ActionListener
    {
        /**
         * Serial version UID
         */
        private static final long serialVersionUID = 1L;

        private String serviceHeading;

        private JLabel serviceLabel;

        private GridBagConstraints cnstrnts;

        private TransparentPanel contentPanel;

        private JComboBox<UrlService> urlChooser;

        private String textAreaText;

        private boolean isCreated;

        public UrlServiceConfigPanel(ServiceType serviceType)
        {
            super(new BorderLayout());

            // Get the relevant texts from the resources file.
            String shortConfigName = serviceType.getShortConfigName();
            serviceHeading = GuiActivator.getResources().getI18NString(
                "service.gui.UC_SERVICES." + shortConfigName + ".HEADING");

            // Create the left-hand menu item
            serviceLabel = new JLabel();
            serviceLabel.setOpaque(true);
            serviceLabel.setText(serviceHeading);
            ScaleUtils.scaleFontAsDefault(serviceLabel);

            // Create the UI components.  We shouldn't need a new transparent
            // panel here but it makes it much easier to get the components to
            // appear in the right place...
            contentPanel = new TransparentPanel(new GridBagLayout());
            add(contentPanel, BorderLayout.NORTH);

            cnstrnts = new GridBagConstraints();

            cnstrnts.anchor = GridBagConstraints.FIRST_LINE_START;
            cnstrnts.fill = GridBagConstraints.HORIZONTAL;

            // Add the description text
            textAreaText = GuiActivator.getResources().getI18NString(
                 "service.gui.UC_SERVICES." + shortConfigName + ".DESCRIPTION");
            JTextArea textArea = new JTextArea();
            textArea.setEditable(false);
            textArea.setFocusable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setOpaque(false);
            textArea.setText(textAreaText);
            ScaleUtils.scaleFontAsDefault(textArea);
            textArea.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

            cnstrnts.gridx = 0;
            cnstrnts.gridy = 0;
            cnstrnts.gridwidth = 2;
            cnstrnts.weightx = 1;
            contentPanel.add(textArea, cnstrnts);

            cnstrnts.gridy += 1;

            JTextArea dropLabel = new JTextArea();
            dropLabel.setEditable(false);
            dropLabel.setFocusable(false);
            dropLabel.setWrapStyleWord(true);
            dropLabel.setOpaque(false);
            ScaleUtils.scaleFontAsDefault(dropLabel);
            dropLabel.setText(GuiActivator.getResources().getI18NString(
                  "service.gui.UC_SERVICES." + shortConfigName + ".SELECTION"));

            cnstrnts.gridx = 0;
            cnstrnts.gridwidth = 1;
            contentPanel.add(dropLabel, cnstrnts);

            // .. and the drop-down itself
            cnstrnts.gridx = 1;
            addUrlChooser(serviceType);

            // Clone the constraints, because the one we are currently using
            // gets used elsewhere in the class to refresh the page.
            GridBagConstraints constraints = (GridBagConstraints) cnstrnts.clone();

            if (serviceType == ServiceType.CRM)
            {
                // Move onto the Auto-launch line.
                constraints.gridy += 1;
                constraints.insets = new Insets(5, 0, 0, 0);

                final JTextArea autoCRMLabel = new JTextArea();
                ScaleUtils.scaleFontAsDefault(autoCRMLabel);
                autoCRMLabel.setEditable(false);
                autoCRMLabel.setFocusable(false);
                autoCRMLabel.setWrapStyleWord(true);
                autoCRMLabel.setOpaque(false);
                autoCRMLabel.setText(GuiActivator.getResources().getI18NString(
                  "service.gui.UC_SERVICES." + shortConfigName + ".AUTO_LAUNCH"));

                constraints.gridx = 0;
                constraints.gridwidth = 1;
                contentPanel.add(autoCRMLabel, constraints);

                constraints.gridx = 1;
                constraints.gridwidth = 1;

                // The available options in the auto-launch combo box vary
                // depending on the following.
                // 1) If it is possible for the user to be invited to
                // conferences, auto-launch applies to both incoming calls and
                // incoming meetings.  Otherwise it applies just to incoming
                // calls.  Therefore, we have to choose the actual string to
                // use to match.
                // 2) If BG contacts are not enabled, the concept of "external"
                // calls doesn't make sense so we don't include that option in
                // the combo box in that case.
                String[] autoLaunchValues;

                ConferenceService mConferenceService =
                                            GuiActivator.getConferenceService();
                boolean confEnabled =
                        mConferenceService.isFullServiceEnabled() ||
                        (mConferenceService.isJoinEnabled() &&
                                       mConferenceService.isConfAppInstalled());

                String autoLaunchExternal =
                    confEnabled ? AUTO_LAUNCH_EXTERNAL_CONF : AUTO_LAUNCH_EXTERNAL;
                String autoLaunchAlways =
                    confEnabled ? AUTO_LAUNCH_ALWAYS_CONF : AUTO_LAUNCH_ALWAYS;

                boolean bgContactsEnabled =
                    configService.user().getBoolean("net.java.sip.communicator.BG_CONTACTS_ENABLED",
                                             false);

                if (bgContactsEnabled)
                {
                    autoLaunchValues = new String[] {AUTO_LAUNCH_NEVER,
                                                     autoLaunchExternal,
                                                     autoLaunchAlways};
                }
                else
                {
                    autoLaunchValues = new String[] {AUTO_LAUNCH_NEVER,
                                                     autoLaunchAlways};
                }

                final JComboBox<String> autoCRMValue = new JComboBox<>(
                        autoLaunchValues);
                ScaleUtils.scaleFontAsDefault(autoCRMValue);

                String currentValue = ServiceType.CRM.getAutoLaunchType();
                if (currentValue.equals(ServiceType.AUTO_LAUNCH_ALWAYS))
                {
                    autoCRMValue.setSelectedItem(autoLaunchAlways);
                }
                else if (currentValue.equals(ServiceType.AUTO_LAUNCH_EXTERNAL))
                {
                    if (bgContactsEnabled)
                    {
                        autoCRMValue.setSelectedItem(autoLaunchExternal);
                    }
                    else
                    {
                        // Kind of an odd case as this external
                        // item should be hidden, but you can hit this
                        // by turning off BG contacts.  In that case
                        // all calls will appear external, so it'll
                        // behave like ALWAYS, so show that to the user.
                        autoCRMValue.setSelectedItem(autoLaunchAlways);
                    }
                }
                else
                {
                    autoCRMValue.setSelectedItem(AUTO_LAUNCH_NEVER);
                }

                autoCRMValue.addItemListener(new ItemListener()
                {
                    @Override
                    public void itemStateChanged(ItemEvent e)
                    {
                        if (e.getStateChange() == ItemEvent.SELECTED)
                        {
                            logger.user("Auto-search set to: " + e.getItem());
                            Object newItem = e.getItem();
                            if (autoLaunchAlways.equals(newItem))
                            {
                                ServiceType.CRM.setAutoLaunchType(ServiceType.AUTO_LAUNCH_ALWAYS);
                            }
                            else if (autoLaunchExternal.equals(newItem))
                            {
                                ServiceType.CRM.setAutoLaunchType(ServiceType.AUTO_LAUNCH_EXTERNAL);
                            }
                            else
                            {
                                ServiceType.CRM.setAutoLaunchType(ServiceType.AUTO_LAUNCH_NEVER);
                            }
                        }
                    }
                });

                contentPanel.add(autoCRMValue, constraints);

                // Move onto the button help text line.
                constraints.gridy += 1;

                // For CRM, also show text indicating how to use the in-call
                // button.
                final TransparentPanel buttonHelpPanel =
                                      new TransparentPanel(new GridBagLayout());
                buttonHelpPanel.setAlignmentY(JLabel.CENTER_ALIGNMENT);
                buttonHelpPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
                GridBagConstraints cons = new GridBagConstraints();
                cons.anchor = GridBagConstraints.CENTER;
                cons.fill = GridBagConstraints.HORIZONTAL;
                cons.weightx = 1;
                cons.gridy = 0;
                cons.gridx = 0;

                // The text
                JTextArea helpText = new JTextArea();
                ScaleUtils.scaleFontAsDefault(helpText);
                helpText.setEditable(false);
                helpText.setFocusable(false);
                helpText.setLineWrap(true);
                helpText.setWrapStyleWord(true);
                helpText.setOpaque(false);
                helpText.setText(GuiActivator.getResources().getI18NString(
                  "service.gui.UC_SERVICES." + shortConfigName + ".BUTTON_HELP_TEXT"));

                cons.gridx = 1;
                buttonHelpPanel.add(helpText, cons);

                constraints.gridx = 0;
                constraints.gridwidth = 2;
                constraints.weightx = 1;

                // The part needs to be slightly further away from the last option
                // so it stands out.
                constraints.insets = new Insets(20, 0, 0, 0);
                contentPanel.add(buttonHelpPanel, constraints);

                // Set whether the button help text panel is visible based on
                // whether VoIP is enabled in the CoS.
                cosService.getClassOfService(new CPCosGetterCallback()
                {
                    @Override
                    public void onCosReceived(CPCos classOfService)
                    {
                        // We could use ConfigurationUtils.isVoIPEnabledInCoS() for
                        // the VoIP enabled by CoS setting, but there is a race
                        // condition between the CoS storeCosFieldsInConfig() method
                        // saving this setting to the config file, and this callback
                        // being called (where isVoIPEnabledInCoS() would then read
                        // that same config file).
                        boolean voipEnabledInCoS =
                            classOfService.getSubscribedMashups().isVoipAllowed();
                        boolean voipEnabledByUser = ConfigurationUtils.isVoIPEnabledByUser();
                        boolean voipEnabled = voipEnabledInCoS && voipEnabledByUser;
                        logger.info("CoS received, display button " +
                                            "help text panel based on " +
                                            "voip enabled in CoS = " + voipEnabledInCoS +
                                            ", and voip enabled by user = " +
                                            voipEnabledByUser);

                        autoCRMLabel.setVisible(voipEnabled);
                        autoCRMValue.setVisible(voipEnabled);
                        buttonHelpPanel.setVisible(voipEnabled);
                    }
                });
            }
        }

        /**
         * Remove the old URL chooser (if it exists) and add a new one.  Allows
         * the chooser to be updated if config has changed.
         * @param serviceType
         */
        protected void addUrlChooser(ServiceType serviceType)
        {
            if (urlChooser != null)
            {
                logger.debug("Remove old URL Chooser for " + serviceType);
                contentPanel.remove(urlChooser);
            }

            Vector<UrlService> services =
                urlServiceTools.getAllEnabledServices(serviceType);
            urlChooser = new JComboBox<>(services);
            ScaleUtils.scaleFontAsDefault(urlChooser);
            urlChooser.addActionListener(this);
            ArrayList<UrlService> selectedServices = urlServiceTools.
                getSelectedServices(serviceType);
            UrlService service = null;
            if (!selectedServices.isEmpty())
            {
                service = selectedServices.get(0);
            }
            urlChooser.setSelectedItem(service);

            contentPanel.add(urlChooser, cnstrnts);
        }

        @Override
        public Component getListCellRendererComponent(boolean isSelected)
        {
            Color fcolor = (isSelected ? SELECTED_FOREGROUND : UNSELECTED_FOREGROUND);
            Color bcolor = (isSelected ? SELECTED_BACGROUND : UNSELECTED_BACKGROUND);

            serviceLabel.setForeground(fcolor);
            serviceLabel.setBackground(bcolor);
            serviceLabel.setBorder(BorderFactory.createLineBorder(bcolor, 8));

            return serviceLabel;
        }

        @Override
        public void elementSelected()
        {
            // Prevent duplicate logging for one combo box change
            if (System.currentTimeMillis() - selectionTime > 200)
            {
                selectionTime = System.currentTimeMillis();
                logger.user(serviceLabel.getText() + " web service selected");
            }

            // Display the right-hand menu.
            centerPanel.removeAll();

            this.setOpaque(false);

            centerPanel.add(this, BorderLayout.CENTER);

            centerPanel.revalidate();
            centerPanel.repaint();

            AccessibilityUtils.setNameAndDescription(configList, serviceLabel.getText(), textAreaText);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            @SuppressWarnings("unchecked")
            JComboBox<UrlService> comboBox = (JComboBox<UrlService>)e.getSource();
            UrlService urlService = (UrlService)comboBox.getSelectedItem();
            if (isCreated)
            {
                logger.user("Combo-box selection set to: " +
                            sanitiseNullable(urlService, srv -> logHasher(srv.name)) +
                            " (Note: May also log on creation of panel)");
            }
            isCreated = true;
            if (urlService != null)
            {
                urlService.setEnabled();
            }
        }
    }
}
