// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main;

import java.awt.event.*;
import java.beans.*;
import java.util.*;

import net.java.sip.communicator.plugin.desktoputil.SIPCommSnakeButton;
import net.java.sip.communicator.plugin.desktoputil.ScaleUtils;
import org.jitsi.service.configuration.*;
import org.jitsi.util.*;

import org.jitsi.util.CustomAnnotations.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.configforms.*;
import net.java.sip.communicator.plugin.provisioning.*;
import net.java.sip.communicator.util.Logger;

public class UrlServiceTools
{
    /**
     * Constants used in config strings.
     */
    protected static final String CONFIG_PREFIX =
                        "net.java.sip.communicator.impl.gui.main.urlservices";

    /**
     * The end of a config property name string which maps to a single int
     * representing a selected service. From V2.27, services are stored as a
     * list, but we need to be able to determine which service was selected
     * prior to upgrade
     */
    private static final String SELECTED = "selected";

    /**
     * The end of a config property name string which maps to a list of
     * selected services
     */
    private static final String SELECTED_SERVICES = "selectedservices";
    private static final String ENABLED = "enabled";
    private static final String NAME = "name";
    private static final String URL = "url";
    private static final int NONE_SELECTED = -1;

    protected static final int N_SERVICES_PER_TYPE = 5;

    // This class is a singleton.
    private static UrlServiceTools sUrlServiceTools;
    private static final Logger logger =
                                        Logger.getLogger(UrlServiceTools.class);

    /**
     * Map to get the 5 different services configured for each service type.
     */
    protected EnumMap<ServiceType, UrlService[]> urlServices;

    /**
    * Map to find out whether a particular service type is enabled.
    */
    private EnumMap<ServiceType, Boolean> enabledServices;

    /**
     * Maps to find out which underlying service is selected for a particular
     * service type.
     */
    private EnumMap<ServiceType, List<Integer>> selectedServices;

    /**
     * True if at least one service type which should be displayed in the config
     * panel is enabled, and has at least one URL enabled under it.
     */
    private boolean enabledServicesInConfigPanel;

    /**
     * Handle to the config service.
     */
    private static ConfigurationService configService;

    /**
     * The frame containing the main config panel in the options menu.  Kept
     * here as we are responsible for controlling its visibility - the whole
     * panel should be hidden if no Web Apps are enabled.
     */
    private static ConfigurationFrame ucServicesConfigFrame;

    /**
     * The main config panel itself.  Kept here so we can control the visibility
     * of individual Web Apps within the main frame.
     */
    private static UcServicesConfigPanel ucServicesConfigPanel;

    /**
     * Update the necessary state when the config changes.
     */
    private final PropertyChangeListener configChangeListener =
         new PropertyChangeListener()
         {
             @Override
             public void propertyChange(PropertyChangeEvent evt)
             {
                 logger.debug("Property changed: " + evt);
                 refreshConfig();
             }
         };

    /**
     * Holds information about a particular service.
     */
    public class UrlService
    {
        public String name;
        private final String url;

        private ServiceType serviceType;
        protected int serviceIndex;
        private boolean enabled;

        public UrlService(ServiceType serviceType, int serviceIndex,
                                       boolean enabled, String name, String url)
        {
            this.serviceType = serviceType;
            this.serviceIndex = serviceIndex;
            this.enabled = enabled;
            this.name = name;
            this.url = url;
        }

        /**
         * @return the URL
         */
        public String getURL()
        {
            return url;
        }

        /**
         * Set this service as the enabled service for this service type.
         */
        public void setEnabled()
        {
            List<Integer> serviceIndexAsArray = new ArrayList<>();
            serviceIndexAsArray.add(serviceIndex);
            setSelectedServices(serviceType, serviceIndexAsArray);
        }

        /**
         * Return the name as the string representation.
         */
        public String toString()
        {
            return name;
        }
    }

    /**
     * Constructor
     */
    private UrlServiceTools()
    {
        urlServices = new EnumMap<>(ServiceType.class);
        urlServices.put(ServiceType.WEBCONFERENCING, new UrlService[5]);
        urlServices.put(ServiceType.FILESHARE_COLLABORATION, new UrlService[5]);
        urlServices.put(ServiceType.CRM, new UrlService[5]);
        urlServices.put(ServiceType.CLOUD_HOSTED, new UrlService[5]);

        enabledServices = new EnumMap<>(ServiceType.class);
        enabledServices.put(ServiceType.WEBCONFERENCING, false);
        enabledServices.put(ServiceType.FILESHARE_COLLABORATION, false);
        enabledServices.put(ServiceType.CRM, false);
        enabledServices.put(ServiceType.CLOUD_HOSTED, false);

        selectedServices = new EnumMap<>(ServiceType.class);

        selectedServices.put(ServiceType.WEBCONFERENCING, null);
        selectedServices.put(ServiceType.FILESHARE_COLLABORATION, null);
        selectedServices.put(ServiceType.CRM, null);
        selectedServices.put(ServiceType.CLOUD_HOSTED, null);

        // Add a listener so that when the config changes we can update the
        // Tools menu as appropriate (show/hide items, display the correct
        // name).  Rather than registering for all 48 properties we care about,
        // just listen for whenever we get new config from SIP PS.
        configService = GuiActivator.getConfigurationService();
        configService.user().addPropertyChangeListener(
            ProvisioningServiceImpl.LAST_PROVISIONING_UPDATE_TIME,
            configChangeListener);

        // Read from the config store
        refreshConfig();
    }

    /**
     * Get hold of this class (it's a singleton).
     * @return
     */
    public static UrlServiceTools getUrlServiceTools()
    {
        if (sUrlServiceTools == null)
        {
            logger.info("Create URL service tools");
            sUrlServiceTools = new UrlServiceTools();
        }

        return sUrlServiceTools;
    }

    /**
     * Find out if a particular service is enabled.
     * @return true if the service is enabled with at least one active URL.
     */
    public boolean isServiceTypeEnabled(ServiceType serviceType)
    {
        return enabledServices.get(serviceType);
    }

    /**
     * Get all the UrlService objects that are enabled for this service type.
     * @return a vector containing all the enabled UrlService objects.
     */
    public Vector<UrlService> getAllEnabledServices(ServiceType serviceType)
    {
        UrlService[] allServices = urlServices.get(serviceType);
        Vector<UrlService> allEnabledServices = new Vector<>();

        for (UrlService thisService : allServices)
        {
            if (thisService.enabled &&
                (thisService.name != null) &&
                (thisService.url != null))
            {
                allEnabledServices.addElement(thisService);
            }
        }

        return allEnabledServices;
    }

    /**
     * Write back the selected service to the config service.
     * @param service the service type
     * @param serviceIndices the indices representing the selected services.
     * This is null if there are no selected services
     *
     * @throws IllegalArgumentException
     */
    public void setSelectedServices(ServiceType service, List<Integer> serviceIndices)
        throws IllegalArgumentException
    {
        logger.debug("Selected services for " + service + ": " + serviceIndices);

        selectedServices.put(service, serviceIndices);

        String key = CONFIG_PREFIX + "." + service.getConfigName() + "." + SELECTED_SERVICES;
        configService.user().setProperty(key, serviceIndices);
    }

    /**
     * @return The selected services for the given service type, or an empty list if no services are selected.
     */
    public ArrayList<UrlService> getSelectedServices(ServiceType serviceType)
    {
        // The selected services are stored as a list of numbers which
        // are used to identify which of the five URL services for each service
        // type are currently selected.
        List<Integer> selectedServicesNumbers = selectedServices.get(serviceType);

        ArrayList<UrlService> selectedServices = new ArrayList<>();

        if (selectedServicesNumbers != null)
        {
            // Use the list of numbers to identify which services we should be
            // returning
            for (int serviceInt : selectedServicesNumbers)
            {
                // Selected services 1-5 stored in an array (index 0-4)
                UrlService serviceToAdd = urlServices.get(serviceType)[serviceInt-1];
                selectedServices.add(serviceToAdd);
            }
        }
        else
        {
            logger.debug("No selected service for " + serviceType);
        }

        return selectedServices;
    }

    /**
     * Create a button that will launch an Accession CRM search when clicked,
     * provided that at least one of the number and DN provided are not null
     * and can be processed correctly (e.g. are not anonymous).
     *
     * @param searchName The peer name to search for.
     * @param searchNumber The peer number to search for.
     * @return A SIPCommButton to put into the UI.
     */
    public SIPCommSnakeButton createCrmLaunchButton(
            @Nullable final String searchName,
            @Nullable final String searchNumber)
    {
        // Process the incoming name and number
        String name = ServiceType.processNameForCrm(searchName);
        String number = ServiceType.processNumberForCrm(searchNumber);

        // One of the phone number or name to search for can be null, but if
        // both are, then don't create the button.
        if (name == null && number == null)
        {
            logger.info("Number and name both null, don't create CRM " +
                                "button");
            return null;
        }

        String crmLaunchButtonText = GuiActivator.getResources().getI18NString(
                "service.gui.UC_SERVICES.CRM_LAUNCH_BUTTON_TEXT_ACCESSION");

        SIPCommSnakeButton crmLaunchButton = new SIPCommSnakeButton(
                crmLaunchButtonText,
                "service.gui.button.crm",
                false,
                true);

        crmLaunchButton.setFont(crmLaunchButton.getFont().deriveFont(
                ScaleUtils.getMediumLargeFontSize()));

        // The tooltip will depend on the specific search that is configured.
        String tooltip = ServiceType.CRM.getMenuText(
                getSelectedServices(ServiceType.CRM).get(0).name);
        crmLaunchButton.setToolTipText(tooltip);

        crmLaunchButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                logger.user("CRM button clicked to launch an accession " +
                                    "CRM search");
                ServiceType.CRM.launchSelectedCrmService(name, number);
            }
        });

        return crmLaunchButton;
    }

    /**
     * Register the main config frame with the URL Services class.  This allows
     * URL Service Tools to control the visibility of the UC Services Config
     * panel if the underlying config changes.
     * @param frame The main configuration frame (which owns the Web Apps icon)
     */
    public void registerMainConfigFrame(ConfigurationFrame frame)
    {
        logger.debug("Register main config frame: " + frame);
        UrlServiceTools.ucServicesConfigFrame = frame;

        updateConfigPanelVisibility();
    }

    private void updateConfigPanelVisibility()
    {
        if (ucServicesConfigFrame != null)
        {
            boolean visible = enabledServicesInConfigPanel;
            logger.debug("Set visibility of main config panel: " + visible);
            ucServicesConfigFrame.setUcServicesPanelVisible(visible);
        }
    }

    /**
     * Register the main config panel with the URL Services class.  This allows
     * URL Service Tools to control the visibility of individual Web Apps as
     * their availability changes.
     * @param configPanel The config panel (which owns the left hand menu and
     * right-hand config options)
     */
    public void registerConfigPanel(UcServicesConfigPanel configPanel)
    {
        ucServicesConfigPanel = configPanel;
    }

    private void updateUcServicesConfigPanelVisibility()
    {
        if (ucServicesConfigPanel != null)
        {
            ucServicesConfigPanel.updateConfigPanelVisibility();
        }
    }

    /**
     * Connect to the config service to read all the info we care about.
     */
    protected synchronized void refreshConfig()
    {
        boolean atLeastOneServiceInConfigPanelActive = false;

        for (ServiceType serviceType : ServiceType.values())
        {
            // Each service type (webconf, collab, CRM, cloud hosted), get &
            //store the data
            List<Integer> currentEnabledServices = new ArrayList<>();
            UrlService[] thisService = urlServices.get(serviceType);

            String prefix =
                        CONFIG_PREFIX + "." + serviceType.getConfigName() + ".";
            boolean serviceTypeEnabled = configService.user().getBoolean(
                                                       prefix + ENABLED, false);

            List<Integer> origSelectedServices = getOriginalSelectedServices(prefix);

            logger.debug(serviceType + "; enabled: " + serviceTypeEnabled +
                                          ", selected: " + origSelectedServices);

            boolean hasActiveUrl = false;

            // create an array of all five services to determine which services
            // are currently enabled
            for (int ix = 0; ix < N_SERVICES_PER_TYPE; ix++)
            {
                // Each individual service within the type (1-5)
                int serviceIndex = ix + 1;
                String servicePrefix = prefix + serviceIndex + ".";
                boolean enabled = configService.user().getBoolean(
                                                servicePrefix + ENABLED, false);
                String name = configService.user().getString(servicePrefix + NAME);
                String url = configService.user().getString(servicePrefix + URL);

                thisService[ix] = new UrlService(serviceType,
                                              serviceIndex, enabled, name, url);

                boolean thisHasActiveUrl =
                                   (enabled && !StringUtils.isNullOrEmpty(url)
                                       && !StringUtils.isNullOrEmpty(name));

                if (thisHasActiveUrl)
                {
                    currentEnabledServices.add(serviceIndex);
                    hasActiveUrl |= thisHasActiveUrl;
                }
            }

            List<Integer> selectedServices = null;

            if (!currentEnabledServices.isEmpty() &&
                origSelectedServices != null &&
                currentEnabledServices.containsAll(origSelectedServices) &&
                serviceType.numberOfSelectedServicesAllowed()==1)
            {
                // If only one selected service is allowed and an enabled
                // service is already selected, keep that as the selected service
                // If multiple selected services are allowed, we cannot simply
                // keep the current selected services in case further services
                // have been set as enabled, and need to be set as selected.
                selectedServices = origSelectedServices;
            }
            else if (!currentEnabledServices.isEmpty())
            {
                // If only one selected service is allowed and there is
                // currently no selected service, set the first
                // enabled service as selected.
                // If multiple selected services are allowed, all enabled
                // services must be set as selected.
                int maxNumberOfSelectedServices = Integer.min(
                    serviceType.numberOfSelectedServicesAllowed(),
                    currentEnabledServices.size());

                selectedServices = new ArrayList<>();

                for (int i = 0; i < maxNumberOfSelectedServices ; i++)
                {
                    selectedServices.add(currentEnabledServices.get(i));
                }
            }

            setSelectedServices(serviceType, selectedServices);

            boolean enabledAndActive = (serviceTypeEnabled && hasActiveUrl);
            enabledServices.put(serviceType, enabledAndActive);

            if (serviceType.shouldAppearInConfigPanel())
            {
                // Determine whether any service types which should appear
                // in the config panel are enabled and active.
                atLeastOneServiceInConfigPanelActive |= enabledAndActive;
            }

            logger.debug(serviceType + " ActiveURL? " + hasActiveUrl +
                " serviceTypeEnabled: " + serviceTypeEnabled +
                " selectedServices: " + selectedServices);
        }

        enabledServicesInConfigPanel = atLeastOneServiceInConfigPanelActive;

        // Services may have changed, so refresh visibility of various UI
        // elements.
        updateUIElementVisibility();
    }

    /**
     * Get the selected services which are currently stored in the config as a
     * string in the form [a,b,c,...] where a,b,c,... are a set of one to five
     * integers, each identifying a currently selected service
     * @param prefix the config prefix for the service
     * @return the selected services currently stored in the config, or null if
     * there are no selected services
     */
    private List<Integer> getOriginalSelectedServices(String prefix)
    {
        List<Integer> origSelectedServices = null;

        String selectedServices = configService.user().getString(
                                             prefix + SELECTED_SERVICES, null);
        if (selectedServices != null)
        {
            origSelectedServices = new ArrayList<>();

            // The string is currently in the form [a,b,c,...]. We convert
            // this string into a list by first removing the square brackets
            String string = selectedServices.substring(1, selectedServices.length()-1);

            // Then the numbers, which are currently separated by commas, are
            // added to the list as integers.
            String str[] = string.split(", ");
            for(int i=0 ; i < str.length; i++)
            {
                if (!str[i].isEmpty())
                {
                    origSelectedServices.add(Integer.parseInt(str[i]));
                }
            }
        }
        else
        {
            // From V2.27, selected services is a list of numbers, rather than
            // a single int. If selectedServices is null, this may be because a
            // user has just upgraded. We should check to see if they previously
            // had a selected service enabled, and if so, add this to the
            // selected services list.
            int selected = configService.user().getInt(prefix + SELECTED,
                                                                NONE_SELECTED);

            if (selected != NONE_SELECTED)
            {
                origSelectedServices = new ArrayList<>();
                origSelectedServices.add(selected);
            }
        }
        return origSelectedServices;
    }

    /**
     * Update the visibility of any UI element registered with us.
     */
    private void updateUIElementVisibility()
    {
        updateConfigPanelVisibility();
        updateUcServicesConfigPanelVisibility();
    }
}
