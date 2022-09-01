// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main;

import java.awt.Desktop;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;

import com.drew.lang.annotations.Nullable;
import com.google.common.annotations.VisibleForTesting;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.impl.gui.main.UcServicesConfigPanel.UrlServiceConfigPanel;
import net.java.sip.communicator.impl.gui.main.UrlServiceTools.UrlService;
import net.java.sip.communicator.plugin.desktoputil.ErrorDialog;
import net.java.sip.communicator.plugin.desktoputil.ScaleUtils;
import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.analytics.AnalyticsParameter;
import net.java.sip.communicator.service.analytics.AnalyticsParameterSimple;
import net.java.sip.communicator.util.Logger;

/**
 * Which service type is being requested - Web conferencing, Filesharing and
 * Collaboration, and CRM.
 */
public enum ServiceType
{
    WEBCONFERENCING
    {
        public String getConfigName() { return "webconf"; }
        public String getShortConfigName() { return "WEBCONF"; }

        @Override
        public String getMenuText(String name)
        {
            return GuiActivator.getResources().getI18NString(
              "service.gui.UC_SERVICES.SERVICE_LAUNCH", new String[]{name});
        }
    },

    FILESHARE_COLLABORATION
    {
        public String getConfigName() { return "collab"; }
        public String getShortConfigName() { return "FILESHARE"; }
        @Override
        public String getMenuText(String name)
        {
            return GuiActivator.getResources().getI18NString(
              "service.gui.UC_SERVICES.SERVICE_LAUNCH", new String[]{name});
        }
    },

    CRM
    {
        public String getConfigName() { return "crm"; }
        public String getShortConfigName() { return "CRM"; }

        @Override
        public ArrayList<JMenuItem> getMenuItems()
        {
            // Use createMenuItem() instead (to get a unique instance).
            return null;
        }

        @Override
        protected void refreshAppearance()
        {
            // CRM doesn't have a single menu item, so this method is not
            // valid.
        }

        @Override
        public String getMenuText(String name)
        {
            return GuiActivator.getResources().getI18NString(
              "service.gui.UC_SERVICES.SEARCH_SERVICE", new String[]{name});
        }

        @Override
        public String getAutoLaunchType()
        {
            String autoLaunch = GuiActivator.getConfigurationService().user().getString(
                UrlServiceTools.CONFIG_PREFIX + "." + getConfigName() + "." +
                          AUTO_LAUNCH_TYPE, AUTO_LAUNCH_NEVER);

            logger.debug("Got auto launch:" + autoLaunch);

            return autoLaunch;
        }

        @Override
        public void setAutoLaunchType(String value)
        {
            logger.debug("Setting auto launch:" + value);

            GuiActivator.getConfigurationService().user().setProperty(UrlServiceTools.CONFIG_PREFIX +
                "." + getConfigName() + "." + AUTO_LAUNCH_TYPE, value);
        }
    },

    CLOUD_HOSTED
    {
        public String getConfigName() { return "cloudhosted"; }
        public String getShortConfigName() { return "CLOUDHOSTED"; }

        @Override
        public String getMenuText(String name)
        {
            return GuiActivator.getResources().getI18NString(
              "service.gui.UC_SERVICES.SERVICE_LAUNCH", new String[]{name});
        }

        @Override
        public void launchServiceFromName(String serviceName)
        {
            UrlService service = findServiceToLaunch(serviceName);

            if (service == null)
            {
                logger.error("service could not be found for " + serviceName);
                return;
            }

            service.getDisplayer().setVisible(true);

            logger.debug("Launching browser panel for " + service.name);
        }

        @Override
        public int numberOfSelectedServicesAllowed()
        {
            return UrlServiceTools.N_SERVICES_PER_TYPE;
        }

        @Override
        public boolean shouldAppearInConfigPanel()
        {
            // The config panel allows users to select a single service out of
            // all the enabled services for that service type. For the cloud
            // hosted service type all enabled services are set as selected, so it
            // is not necessary for a user to have the option to select a service.
            return false;
        }

        @Override
        public void updateWindowSize(UrlServiceTools ust)
        {
            ArrayList<UrlService> selectedServices = ust.getSelectedServices(this);

            for (UrlService service : selectedServices)
            {
                //set default as full screen.
                String windowSize = GuiActivator.getConfigurationService().user().getString(getPrefix(
                    service.serviceIndex) + "." + WINDOW_SIZE, FULL_SCREEN);

                if (windowSize.equals(SMALL) || windowSize.equals(MEDIUM))
                {
                    int height;
                    int width;

                    if (windowSize.equals(SMALL))
                    {
                        height = SMALL_HEIGHT;
                        width = SMALL_WIDTH;
                    }
                    else
                    {
                        height = MEDIUM_HEIGHT;
                        width = MEDIUM_WIDTH;
                    }

                    GuiActivator.getConfigurationService().user().setProperty(getPrefix(
                                service.serviceIndex) + ".HEIGHT", height);
                    GuiActivator.getConfigurationService().user().setProperty(getPrefix(
                                service.serviceIndex) + ".WIDTH", width);
                    GuiActivator.getConfigurationService().user().setProperty(getPrefix(
                                service.serviceIndex) + ".fullscreen", false);
                }
                else
                {
                    // If the window size is set to anything other than small
                    // or medium, we default to fullscreen.
                    GuiActivator.getConfigurationService().user().setProperty(getPrefix(
                                service.serviceIndex) + ".fullscreen", true);
                }
            }
        }

        @Override
        public String getNameOfSelectedService()
        {
            logger.error("There is no concept of a selected service for the Cloud Hosted category because all services are visible to user");
            return null;
        }
    };

    private static final String CALLER_VALUE_TOKEN = "\\{SEARCH_VALUE\\}";
    private static final String CALLER_NUMBER_TOKEN = "\\{SEARCH_NUMBER_ONLY\\}";

    private static final String AUTO_LAUNCH_TYPE = "autolaunch";

    private static final String WINDOW_SIZE = "windowsize";
    private static final String SMALL = "small";
    private static final String MEDIUM = "medium";
    private static final String FULL_SCREEN = "fullscreen";

    // The dimensions of the small window have been chosen to match the default
    // size of the CommPortal windows. Consider both sets of dimensions before
    //making any changes.
    protected final int SMALL_HEIGHT = 385;
    protected final int SMALL_WIDTH = 575;
    protected final int MEDIUM_HEIGHT = 770;
    protected final int MEDIUM_WIDTH = 1285;

    private static final Logger logger = Logger.getLogger(ServiceType.class);

    /**
     * The menu items for this service. This is only relevant for
     * Webconferencing, Fileshare/collaboration and Cloud Hosted services
     * (for CRM, each contact has a new menu item).
     */
    protected ArrayList<JMenuItem> menuItems;

    /**
     * @return The short string that represents this service type used in
     * the sip-communicator.properties file.
     */
    public abstract String getConfigName();

    /**
     * @return The short string that represents this service type used in
     * the resources.properties file.
     */
    public abstract String getShortConfigName();

    /**
     * @param name The name of the enabled service (e.g. "Dropbox")
     * @return Text for use in a menu.
     */
    public abstract String getMenuText(String name);

    /**
     *  The maximum number of selected services allowed for this service type.
     *  Most services only allow a single selected service.
     *
     *  @return The maximum number of selected services
     */
    public int numberOfSelectedServicesAllowed()
    {
        return 1;
    }

    /**
     *  Indicates whether the service type should appear in the configuration
     *  panel. The config panel allows users to select a single service out of
     *  all the enabled services for that service type. If the selected services
     *  of a service type are configured to be the same as the enabled services,
     *  and no additional config needs to be set by the user, the service type
     *  does not need to appear in the config panel.
     *
     * @return True if the service type should appear in the configuration
     * panel. False otherwise
     */
    public boolean shouldAppearInConfigPanel()
    {
        return true;
    }

    /**
     * The config panel created by {@link #getUrlServiceConfigPanel}.  Used
     * to control visibility if the config changes.
     */
    private UrlServiceConfigPanel urlServiceConfigPanel;

    /*
     * Values for the auto launch.
     */
    public static final String AUTO_LAUNCH_NEVER = "never";
    public static final String AUTO_LAUNCH_ALWAYS = "always";
    public static final String AUTO_LAUNCH_EXTERNAL = "external";

    /**
     * Create the relevant config panel for the Tools > Options > Web Apps
     * menu pane.
     * @return The panel for this object.
     */
    public UrlServiceConfigPanel getUrlServiceConfigPanel(UcServicesConfigPanel enclosingPanel)
    {
        urlServiceConfigPanel = enclosingPanel.new UrlServiceConfigPanel(this);
        return urlServiceConfigPanel;
    }

    @VisibleForTesting
    public String getAnalyticName()
    {
        switch (this)
        {
            case CRM:
                return "CRM";
            case WEBCONFERENCING:
                return "Web Conferencing";
            case CLOUD_HOSTED:
                return "Cloud Hosted";
            case FILESHARE_COLLABORATION:
                return "File Sharing";
            default:
                throw new IllegalStateException("Analytic name must be implemented for given service type");
        }
    }

    public void raiseUserInteractionAnalytic()
    {
        List<AnalyticsParameter> params = new ArrayList<>();
        params.add(new AnalyticsParameterSimple(AnalyticsParameter.SERVICE_CATEGORY_LAUNCHED, getAnalyticName()));
        GuiActivator.getAnalyticsService().onEventWithIncrementingCount(
                AnalyticsEventType.THIRD_PARTY_INTEGRATION_LAUNCHED, params);
    }

    /**
     * We need to present the current selected service for each category in Electron, therefore we have to send the
     * current selected names over WISPA. This method finds the current selected service for each category and returns
     * the corresponding name of the service, returning null if there no selected service.
     *
     * This method is called on all categories except "Cloud hosted", this is because the cloud hosted services are all
     * visible to the user unlike the other categories.
     *
     * @return The name of the current selected service for this category, null if no selected service.
     */
    public String getNameOfSelectedService()
    {
        boolean isEnabled = UrlServiceTools.getUrlServiceTools().isServiceTypeEnabled(this);
        ArrayList<UrlService> selectedUrlServices = UrlServiceTools.getUrlServiceTools().getSelectedServices(this);

        if (isEnabled && !selectedUrlServices.isEmpty())
        {
            // There is only ever one selected service for the categories this method can be called on (all except cloud
            // hosted). Therefore, if the list is non-empty, then we know the selected service is the one in the first
            // (and only) index.
            return selectedUrlServices.get(0).name;
        }
        else
        {
            return null;
        }
    }

    /**
     * Determine which service has been launched via the Tools menu
     * @param serviceName the menu text for the service
     *
     * @return the service to launch
     */
    protected UrlService findServiceToLaunch(String serviceName)
    {
        UrlService serviceToLaunch = null;
        ArrayList<UrlService> selectedServices = UrlServiceTools.getUrlServiceTools().getSelectedServices(this);
        for (UrlService service : selectedServices)
        {
            if (serviceName.equals(getMenuText(service.name)))
            {
                serviceToLaunch = service;
                break;
            }
        }
        return serviceToLaunch;
    }

    /**
     * Launch the service corresponding to the given service name.
     *
     * Only used for the cloud hosted category.
     */
    public void launchServiceFromName(String serviceName)
    {
        String url = findServiceToLaunch(serviceName).getURL();
        launchURLInternal(url);
    }

    /**
     * Launch the selected service for the current category.
     *
     * Only used for File Sharing and Web Conferencing category.
     */
    public void launchSelectedService()
    {
        String url = UrlServiceTools.getUrlServiceTools().getSelectedServices(this).get(0).getURL();
        launchURLInternal(url);
    }

    /**
     * Must only be used on the CRM category!
     *
     * Launch the configured URL for this service, first substituting the
     * string {@link #CALLER_VALUE_TOKEN} and {@link #CALLER_NUMBER_TOKEN}
     * in the configured URL for the passed-in parameter.
     * @param name The name of the contact to use as a parameter, should
     *      default to DN if no name found.
     * @param number The DN of the contact to use as a parameter
     */
    public void launchSelectedCrmService(@Nullable String name, @Nullable String number)
    {
        String url = UrlServiceTools.getUrlServiceTools().getSelectedServices(this).get(0).getURL();

        // Process the input name and number. If both of these end up being
        // null, the URL will be launched with an empty string, though this
        // should never be hit.
        name = processNameForCrm(name);
        number = processNumberForCrm(number);

        String safeName = "";
        String safeNumber = "";

        try
        {
            // Name falls back to the number if not present.
            safeNumber = (number == null) ? "" : URLEncoder.encode(number, "UTF-8");
            safeName = (name == null) ? safeNumber : URLEncoder.encode(name, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            // If the system doesn't support UTF-8 then tough luck.
            logger.fatal("UTF-8 not supported", e);
        }

        if (url != null && !url.isEmpty())
        {
            url = url.replaceAll(CALLER_VALUE_TOKEN, safeName);
            url = url.replaceAll(CALLER_NUMBER_TOKEN, safeNumber);
        }
        else
        {
            url = "";
        }

        launchURLInternal(url);
    }

    private void launchURLInternal(String url)
    {
        logger.info("Launch a " + getConfigName() + " session");
        boolean error = false;
        String errorTitle = "";
        String errorText = "";

        if (Desktop.isDesktopSupported())
        {
            logger.debug("Desktop supported");
            try
            {
                Desktop.getDesktop().browse(new URI(url));
            }
            catch (IOException e)
            {
                logger.warn("Exception using URL: " + url);
                error = true;
                errorTitle = GuiActivator.getResources().getI18NString(
                    "service.gui.UC_SERVICES.URL_ERROR.FAILED.TITLE");
                errorText = GuiActivator.getResources().getI18NString(
                    "service.gui.UC_SERVICES.URL_ERROR.FAILED.TEXT",
                    new String[]{url});
            }
            catch (URISyntaxException e)
            {
                logger.warn("Syntax error in URL: " + url, e);
                error = true;
                errorTitle = GuiActivator.getResources().getI18NString(
                    "service.gui.UC_SERVICES.URL_ERROR.FAILED.TITLE");
                errorText = GuiActivator.getResources().getI18NString(
                    "service.gui.UC_SERVICES.URL_ERROR.FAILED.TEXT",
                    new String[]{url});
            }
            catch (RuntimeException e)
            {
                // Seriously! We hit this on Mac when trying to load a URL
                // containing invalid characters. Use the "Invalid URL"
                // error text.
                logger.warn("Exception loading URL: " + url, e);
                error = true;
                errorTitle = GuiActivator.getResources().getI18NString(
                    "service.gui.UC_SERVICES.URL_ERROR.FAILED.TITLE");
                errorText = GuiActivator.getResources().getI18NString(
                    "service.gui.UC_SERVICES.URL_ERROR.FAILED.TEXT",
                    new String[]{url});
            }
        }
        else
        {
            logger.debug("Desktop not supported");
            error = true;
            errorTitle =  "Unable to access browser";
            errorText = "It was not possible to access a browser to launch this URL";
        }

        if (error)
        {
            ErrorDialog popup = new ErrorDialog(null, errorTitle, errorText);
            popup.setVisible(true);
        }
    }

    /**
     * Get (or create as necessary) the JMenuItem for the tools menu.
     * @return A JMenuItem, or null if this service shouldn't appear in the
     * Tools menu.
     */
    public ArrayList<JMenuItem> getMenuItems()
    {
        if (menuItems == null)
        {
            menuItems = createMenuItems();
        }

        return menuItems;
    }

    /**
     * Refresh visibility of this service's JMenuItem.
     */
    protected void refreshAppearance()
    {
        refreshAppearance(menuItems);
    }

    /**
     * Refresh visibility of the passed-in JMenuItem.
     * @param items a list of menu items for the service type.
     */
    private void refreshAppearance(ArrayList<JMenuItem> items)
    {
        if (items == null || items.isEmpty())

        {
            logger.debug(this + ": No item to refresh");
            return;
        }

        if (UrlServiceTools.getUrlServiceTools().isServiceTypeEnabled(this))
        {
            logger.debug("Enable the " + getConfigName() + " menu");

            ArrayList<UrlService> selectedServices = UrlServiceTools.getUrlServiceTools().getSelectedServices(this);

            int selectedServicesIndex = 0;
            for (JMenuItem item : items)
            {
                if (selectedServicesIndex < selectedServices.size())
                {
                    String menuName = selectedServices.get(selectedServicesIndex).name;
                    item.setText(getMenuText(menuName));

                    if (this != CRM)
                    {
                        // Don't have a tool-tip on Contacts (the CRM item)
                        item.setToolTipText(selectedServices.get(selectedServicesIndex).getURL());
                    }

                    item.setVisible(true);
                }
                else
                {
                    item.setVisible(false);
                }
                selectedServicesIndex++;
            }
        }
        else
        {
            logger.debug("Hide the " + getConfigName() + " menu");
            for (JMenuItem item : items)
            {
                item.setVisible(false);
            }
        }
    }
        /**
         * Create a menu item to appear in a menu.
         */
    public ArrayList<JMenuItem> createMenuItems()
    {
        logger.debug("Creating " + numberOfSelectedServicesAllowed() +
             " menu items: " + getConfigName());
        ArrayList<JMenuItem> menuItemArray = new ArrayList<>();

        for (int i = 0; i < numberOfSelectedServicesAllowed(); i++)
        {
            JMenuItem newMenuItem = new JMenuItem(getConfigName());
            newMenuItem.setName(getConfigName());
            ScaleUtils.scaleFontAsDefault(newMenuItem);
            menuItemArray.add(newMenuItem);
        }
        refreshAppearance(menuItemArray);

        return menuItemArray;
    }

    /**
     * Whether this service should auto launch.  Defaults to off.
     */
    public String getAutoLaunchType()
    {
        return AUTO_LAUNCH_NEVER;
    }

    /**
     * Sets whether this service should auto launch.  Defaults to noop,
     * and is overridden by subclasses.
     */
    public void setAutoLaunchType(String value)
    {
    }

    /**
     * Updates the height and width of the window if the Url service is to
     * be launched in an embedded browser. Defaults to noop as most
     * services are launched in the native browser. It should be
     * overridden if the service is to be launched in an embedded browser.
     */
    public void updateWindowSize(UrlServiceTools ust)
    {
    }

    /**
     * Returns a prefix to the string used in the config to identify the
     * different services for each service type
     *
     * @param serviceIndex The index identifying the service.
     *
     * @return a string in the form:
     * net.java.sip.communicator.impl.gui.main.urlservices.ServiceType.serviceIndex
     */
    public String getPrefix(int serviceIndex)
    {
        return UrlServiceTools.CONFIG_PREFIX + "." + getConfigName() + "." + serviceIndex;
    }

    /**
     * Process the number to use in the CRM lookup - a simple check that it
     * matches a proper DN is all that is required at present.
     *
     * @param number The number to process
     * @return A plain DN or null
     */
    protected static String processNumberForCrm(@Nullable String number)
    {
        return GuiActivator.getPhoneNumberUtils().extractPlainDnFromAddress(number);
    }

    /**
     * This method processes in the input name for the CRM lookup and
     * specifically sets the string "Anonymous" (or variations of this
     * capitalization) to null.
     *
     * This is because it seems that at various points when a CRM request
     * is required, the calling party's name could be set to that string for
     * anonymous calls.
     *
     * Technically, this means that if users call their native contacts
     * "Anonymous", this method will ignore that, but that should be fine
     * because in those cases, the number of the contact should be parsable and
     * used for the lookup instead.
     *
     * @param name The input name to check
     * @return The input name unchanged or null
     */
    protected static String processNameForCrm(@Nullable String name)
    {
        // Consider the name to correspond to an anonymous caller if it matches
        // the string "anonymous", barring any capitalization differences
        String anonymous = "anonymous";

        if (name != null)
        {
            if (name.toLowerCase().equals(anonymous))
            {
                name = null;
            }
        }

        return name;
    }
}
