// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import static net.java.sip.communicator.service.insights.parameters.DesktopUtilParameterInfo.SSO_TOKEN_REFRESHED;
import static org.jitsi.util.Hasher.logHasher;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.annotations.VisibleForTesting;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import net.java.sip.communicator.service.cdap.CDAPService;
import net.java.sip.communicator.service.cdap.ServiceProviderDetails;
import net.java.sip.communicator.service.credentialsstorage.ScopedCredentialsStorageService;
import net.java.sip.communicator.service.httputil.HttpUtils;
import net.java.sip.communicator.service.insights.InsightsEventHint;
import net.java.sip.communicator.service.provisioning.RefreshSSOToken;
import net.java.sip.communicator.service.provisioning.SsoError;
import net.java.sip.communicator.service.wispaservice.WISPAAction;
import net.java.sip.communicator.service.wispaservice.WISPAMotion;
import net.java.sip.communicator.service.wispaservice.WISPAMotionType;
import net.java.sip.communicator.service.wispaservice.WISPANamespace;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.launchutils.ProvisioningParams;
import org.jitsi.service.configuration.ScopedConfigurationService;
import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.util.StringUtils;
import org.jitsi.util.ThreadUtils;

/**
 * A helper class for the pre login flow which exposes API to freeze/unfreeze
 * the thread which is calling it. It also exposes API to fill/clear the login
 * data retrieved from Electron.
 */
public class PreLoginUtils
{
    /**
     * Enum indicating a lock type. Every type corresponds to specific lock object.
     * <p>
     * LOGIN - Stop executing Java until login credentials are passed from Electron
     * NAMESPACES_REGISTERED -  Stop executing Java until required namespaces are registered
     * SERVICE_PROVIDER - Stop executing Java until service provider is selected from service provider list
     */
    public enum EventType {
        LOGIN,
        SERVICE_PROVIDER
    }
    private static final Logger log = Logger.getLogger(PreLoginUtils.class);
    public static boolean isServiceProviderChosen = true;
    public static boolean isBrandingChecked = false;
    public static String currentUsername = "";
    public static String currentSSOToken = "";
    public static SsoError currentSSOError = null;
    public static String currentGraphAPIToken = "";
    public static String currentPassword = "";
    public static boolean didSSOLoginFail = false;
    @VisibleForTesting public static boolean isShutdownInitiated = false;
    private static final Object shutdownLock = new Object();
    @VisibleForTesting static Object loginLock = new Object();
    @VisibleForTesting static Object serviceProviderLock = new Object();
    static final String REMEMBERED_USERNAME =
            "net.java.sip.communicator.plugin.desktoputil.credentials.REMEMBERED_USERNAME";
    static final String REMEMBERED_PASSWORD = "net.java.sip.communicator.plugin.desktoputil.credentials";
    static final String PNAME_SAVE_PASSWORD_TICKED =
            "net.java.sip.communicator.util.swing.auth.SAVE_PASSWORD_TICKED";
    static final String PROPERTY_SERVICE_PROVIDER_ID =
            "net.java.sip.communicator.plugin.cdap.service_provider_id";
    static final String PROPERTY_SERVICE_PROVIDER_NAME =
            "net.java.sip.communicator.plugin.provisioning.SERVICE_PROVIDER_NAME";
    static final String PROPERTY_PROVISIONING_URL_PREFIX =
            "net.java.sip.communicator.plugin.provisioning.URL_PREFIX";
    static final String PROPERTY_PROVISIONING_URL =
            "net.java.sip.communicator.plugin.provisioning.URL";
    static final String PROPERTY_CDAP_VERSION_NUMBER =
            "net.java.sip.communicator.plugin.cdap.version_number";
    static final String PROPERTY_CDAP_EULA_VERSION_NUMBER =
            "net.java.sip.communicator.plugin.eula.EXTRA_EULA_VER";
    static final String PROPERTY_APPLICATION_ID =
            "net.java.sip.communicator.plugin.provisioning.APPLICATION_ID";
    static final String PROPERTY_IS_SSO_ACTIVE =
            "net.java.sip.communicator.plugin.provisioning.auth.SSO_ACTIVE";
    static final String PROPERTY_ACTIVE_USER =
            "net.java.sip.communicator.plugin.provisioning.auth.ACTIVE_USER";
    static final String PROPERTY_ENCRYPTED_MSAL_CACHE
            = "net.java.sip.communicator.plugin.provisioning.token_cache";
    static final String PROPERTY_ENCRYPTED_SSO_TOKEN
            = "net.java.sip.communicator.plugin.provisioning.sso_token";

    /**
     * Locks the calling thread until continueJava is called. We need this in
     * order to stop some modules from finishing and others starting before it's
     * time - for example, we need to stop and wait in ProvisioningServiceImpl
     * for user to enter their credentials, before we can continue to log in
     * using those credentials - and only then we can allow other modules to
     * start - otherwise the app would not run properly as it depends on user
     * being logged in.
     *
     * @param lockType enum value indicating which object to lock
     */
    public static void awaitEvent(EventType lockType)
    {
        Object lock = null;

        synchronized (shutdownLock)
        {
            if (isShutdownInitiated)
            {
                log.info("Shutdown initiated. Avoid blocking Java to enable" +
                         "Felix to close all the bundles");
                return;
            }
        }

        switch (lockType)
        {
            case LOGIN:
                // If user is asked to select service provider don't freeze the login
                if (isServiceProviderChosen)
                {
                    lock = loginLock;
                }
                else
                {
                    log.debug("Await event with type: " + lockType + " ignored as service provider is not selected");
                }
                break;
            case SERVICE_PROVIDER:
                lock = serviceProviderLock;
                // User has been asked to choose service provider
                isServiceProviderChosen = false;
                break;
        }

        if (lock != null)
        {
            synchronized (lock)
            {
                try
                {
                    log.info("Stopping Java execution with event type: " + lockType);
                    lock.wait();
                }
                catch (InterruptedException e) {}
            }
        }
    }

    /**
     * Unlocks the thead which called the awaitEvent - which is once we retrieve
     * the required data from Electron.
     *
     * @param lockType enum value indicating which object to unlock
     */
    public static void notifyEvent(EventType lockType)
    {
        Object lock = null;

        switch (lockType)
        {
            case LOGIN:
                lock = loginLock;
                break;
            case SERVICE_PROVIDER:
                // Service provider is already chosen, so Java wasn't freezed by this lock
                if (!isServiceProviderChosen)
                {
                    lock = serviceProviderLock;
                }
                else
                {
                    log.debug("Notify event with type: " + lockType + " ignored as service provider is already chosen");
                }
                break;
        }

        // If lock is assigned, notify Java to continue
        if (lock != null)
        {
            synchronized (lock)
            {
                log.info("Continue Java execution with event type: " + lockType);
                ThreadUtils.notifyAll(lock);
            }
        }
    }

    /**
     * Notifies all EventTypes, effectively unblocking Java in each place it was
     * blocked. On shutdown, Felix needs to close all the bundles, and it cannot
     * close them if the thread was blocked in the start() method of an
     * activator.
     */
    public static void invokeShutdown() {
        log.info("Invoking shutdown");
        synchronized (shutdownLock) {
            isShutdownInitiated = true;
        }

        // If the user isn't logged in, make sure we clear any branding info
        // we've already saved in config so that, when they next start the
        // client, they will be returned to the start of the login process
        // and given the option to select a different service provider.
        if (!isLoggedIn())
        {
            log.info("Clearing branding info as user not logged in.");
            clearBrandingInfo(false);
        }

        for (EventType event : EventType.values())
        {
            notifyEvent(event);
        }
    }

    /**
     * Returns true if ServiceUtils.shutdown was called which kills the Java
     * process. To do so, Felix first needs to close all the bundles, but it
     * cannot stop the bundle if the thread was blocked in the start() method
     * of an activator.
     */
    public static boolean isShutdownInitiated() {
        synchronized (shutdownLock) {
            return isShutdownInitiated;
        }
    }

    /**
     * Pushes new user entered manual login data passed from Electron.
     */
    public static void setManualLoginData(
            String username,
            String password,
            Boolean rememberMe,
            Boolean useRememberedLogin) {
        currentUsername = username;
        currentPassword = password;
        didSSOLoginFail = false;
        if (useRememberedLogin)
        {
            currentPassword = DesktopUtilActivator.getCredentialsService().global().loadPassword(REMEMBERED_PASSWORD);
        }
        DesktopUtilActivator.getConfigurationService().global().setProperty(PNAME_SAVE_PASSWORD_TICKED, rememberMe);
        if (rememberMe)
        {
            DesktopUtilActivator.getConfigurationService().global().setProperty(REMEMBERED_USERNAME, currentUsername);
            DesktopUtilActivator.getCredentialsService().global().storePassword(REMEMBERED_PASSWORD, currentPassword);
        } else
        {
            DesktopUtilActivator.getConfigurationService().global().removeProperty(REMEMBERED_USERNAME);
            DesktopUtilActivator.getCredentialsService().global().removePassword(REMEMBERED_PASSWORD);
        }
        formatUserName();
    }

    /**
     * Clears Manual login data retrieved from Electron.
     */
    public static void clearManualLoginData() {
        currentUsername = "";
        currentPassword = "";
    }

    /**
     * Pushes sso token and username retrieved from the MSAL lib upon user login.
     */
    public static void setSSOData(String username, String ssoToken, String graphAPIToken, SsoError ssoError) {
        currentUsername = username;
        currentSSOToken = ssoToken;
        currentGraphAPIToken = graphAPIToken;
        currentSSOError = ssoError;
        didSSOLoginFail = StringUtils.isNullOrEmpty(ssoToken);
    }

    /**
     * Clears SSO data retrieved from the MSAL lib.
     */
    public static void clearSSOData() {
        currentSSOToken = "";
        currentGraphAPIToken = "";
        currentSSOError = null;
        didSSOLoginFail = false;
    }

    /**
     * Returns true if SSO token was provided from Electron.
     */
    public static boolean hasSSOToken() {
        return !StringUtils.isNullOrEmpty(currentSSOToken);
    }

    /**
     * Returns true if user is logged in via SSO, false otherwise.
     */
    public static boolean isLoggedInViaSSO() {
        return DesktopUtilActivator.getConfigurationService().global().getBoolean(PROPERTY_IS_SSO_ACTIVE, false);
    }

    /**
     * Returns true if PROPERTY_ACTIVE_USER is set, false otherwise.
     */
    public static boolean isLoggedIn()
    {
        return DesktopUtilActivator.getConfigurationService().global().getProperty(PROPERTY_ACTIVE_USER) != null;
    }

    /**
     * Fetches the branding info for service provider ID provided via login link.
     * After fetching it, it pushes the branding data to Electron, so it knows
     * it should proceed to show the login screen.
     */
    public static void fetchBrandingAndPushToElectron() {
        if (ProvisioningParams.getCdapID() != null) {
            try
            {
                CDAPService cdapService = DesktopUtilActivator.getCdapService();
                if (cdapService == null)
                {
                    // In case login link is clicked before CDAPService is up,
                    // we just skip doing anything here, as CDAPService will
                    // check for cdapID provided via link upon starting.
                    log.info("Clicked on a login link but CDAPService " +
                             "is not up yet.");
                    return;
                }

                if (isLoggedIn())
                {
                    log.info("Login link clicked but there is user already logged in - ignore the link");
                    return;
                }

                if (ProvisioningParams.getCdapID() == null)
                {
                    log.info("Login link clicked but cdapID was not provided - ignore it");
                    return;
                }

                if (loginLinkMismatchesSelectedProvider())
                {
                    showCDAPIdNotMatchingError();
                    return;
                }

                HashMap<String, ServiceProviderDetails> serviceProviders = cdapService.getServiceProviders();
                if (serviceProviders == null)
                {
                    log.warn("No service providers, cannot login with link");
                    return;
                }

                String cdapID = cdapService.getServiceProviderIdIfProvidedViaLink();
                cdapService.fetchAndSaveBrandingInfo(cdapID, false);
                DesktopUtilActivator.getWISPAService().notify(WISPANamespace.BRANDING, WISPAAction.DATA);

                if (ProvisioningParams.shouldAutomaticallyLoginViaLink())
                {
                    log.info("Login with link provided with username and password - " +
                             "proceed to login immediately");
                    WISPAMotion wispaMotion = new WISPAMotion(WISPAMotionType.SHOW_LOGGING_IN);
                    DesktopUtilActivator.getWISPAService().notify(WISPANamespace.CORE, WISPAAction.MOTION, wispaMotion);

                    notifyEvent(EventType.SERVICE_PROVIDER);
                    notifyEvent(EventType.LOGIN);
                }
            }
            catch (Exception e)
            {
                log.warn("Failed fetching branding info for cdap ID: " +
                         ProvisioningParams.getCdapID() + "provided via login link", e);
                ProvisioningParams.clear();
                showError("plugin.cdap.PROBLEM_RETRIEVING", null);
            }
        }
    }

    /**
     * If the CDAP ID has changed (the user may have clicked a CDAP URI for a
     * different service provider whilst on the login screen) from the service
     * provider they selected previously, we show an error message, force quit
     * the app and clear the branding data so users can click the link again and
     * login.
     */
    public static void showCDAPIdNotMatchingError()
    {
        log.warn("User selected CDAP ID and login link CDAP ID" +
                 " do not match. Forcing user to close the app.");

        clearBrandingInfo(false);
        showError(
                "plugin.cdap.selection.CDAP_ID_NOT_MATCHING",
                "plugin.cdap.selection.CLOSE"
        );
    }

    /**
     * @return Whether the CDAP ID on the most recently received URI is
     *         different to the CDAP ID selected by the user in the CDAP
     *         selection window.
     */
    public static boolean loginLinkMismatchesSelectedProvider()
    {
        // If the user didn't manually select a service provider the stored
        // service provider ID will either be empty or set to -1. Therefore,
        // assume a value of -1 if no stored config is found.
        String selectedId = DesktopUtilActivator.getConfigurationService()
                .global().getString(PROPERTY_SERVICE_PROVIDER_ID, "-1");

        return ProvisioningParams.getCdapID() != null &&
               !selectedId.equals("-1") &&
               !selectedId.equals(ProvisioningParams.getCdapID());
    }

    /**
     * Clear previously saved CDAP branding information.
     * @param notifyElectron true if Branding DATA should be sent to notify
     *                       Electron about the change.
     */
    public static void clearBrandingInfo(boolean notifyElectron)
    {
        log.entry();

        log.info("Clearing CDAP branding information");

        ScopedConfigurationService configurationService = DesktopUtilActivator.getConfigurationService().global();
        configurationService.removeProperty(PROPERTY_SERVICE_PROVIDER_ID);
        configurationService.removeProperty(PROPERTY_SERVICE_PROVIDER_NAME);
        configurationService.removeProperty(PROPERTY_PROVISIONING_URL_PREFIX);
        configurationService.removeProperty(PROPERTY_PROVISIONING_URL);
        configurationService.removeProperty(PROPERTY_CDAP_VERSION_NUMBER);
        configurationService.removeProperty(PROPERTY_CDAP_EULA_VERSION_NUMBER);
        configurationService.removeProperty(PROPERTY_APPLICATION_ID);

        if (notifyElectron) {
            DesktopUtilActivator.getWISPAService().notify(WISPANamespace.BRANDING, WISPAAction.DATA);
        }

        log.exit();
    }

    private static void showError(String errorResId, String buttonResId)
    {
        ResourceManagementService resources = DesktopUtilActivator.getResources();
        ErrorDialog errorDialog = new ErrorDialog(
                resources.getI18NString("service.gui.ERROR"),
                resources.getI18NString(errorResId),
                ErrorDialog.OnDismiss.FORCE_EXIT
        );
        if (buttonResId != null)
        {
            // It defaults to "OK" by default.
            errorDialog.setButtonText(resources.getI18NString(buttonResId));
        }
        errorDialog.setModal(true);
        errorDialog.showDialog();
    }

    /**
     * Request a new SSO access token, and then replace DirectoryNumber and
     * Password params in the updateLink with a new AccessToken param - in case
     * there are both 'DirectoryNumber' and 'Password' params present in the link.
     */
    public static String prepareUpdateLinkForSSO(String updateLink) {
        log.info("Preparing update link for SSO");
        String ssoToken = PreLoginUtils.refreshSSOToken();

        // First split the link to host and query params.
        String[] splitUrl = updateLink.split("\\?", 2);
        if (splitUrl.length < 2)
        {
           log.info("Update link has no query params - using the original link");
           return updateLink;
        }

        String host = splitUrl[0];
        String query = splitUrl[1];

        // Now split the params based on & char.
        Map<String, String> params = new HashMap<>();
        Pattern pattern = Pattern.compile("([^&=]+)=([^&]*)");
        Matcher matcher = pattern.matcher(query);

        while (matcher.find())
        {
            String name = matcher.group(1);
            String value = matcher.group(2);
            params.put(name, value);
        }

        if (params.containsKey("DirectoryNumber") && params.containsKey("Password"))
        {
            params.remove("DirectoryNumber");
            params.remove("Password");
            params.put("AccessToken", ssoToken);

            StringBuilder newParams = new StringBuilder("?");
            for (String key : params.keySet())
            {
                newParams.append(key).append("=").append(params.get(key)).append("&");
            }

            // Remove the last '&' sign.
            String fixedParams = newParams.substring(0, newParams.length() - 1);

            log.info("Found DirectoryNumber and Password in update link - replacing with AccessToken");
            return host + fixedParams;
        }

        log.info("DirectoryNumber and Password not found in update link - using the original link");
        return updateLink;
    }

    /**
     * Fetch the application ID from a sessionless provisioning endpoint. In case we cannot retrieve
     * it, whether this request is to an older version of EAS that doesn't support SSO, or the
     * provider just doesn't support SSO - we determine SSO availability by the presence of
     * applicationID in the config - so we won't show the SSO button if applicationID
     * retrieval failed.
     * Application ID is required for SSO login.
     */
    public static void fetchApplicationID() {
        ScopedConfigurationService configurationService = DesktopUtilActivator.getConfigurationService().global();
        String provisioningUrlPrefix = configurationService.getString(PROPERTY_PROVISIONING_URL_PREFIX);
        String provisioningLoginUrl = configurationService.getString(PROPERTY_PROVISIONING_URL);

        String pgcUrl = provisioningUrlPrefix + "/psi/publicglobalconfig";

        try
        {
            if (StringUtils.isNullOrEmpty(provisioningUrlPrefix)) {
                log.info("Provisioning URL prefix empty, using Provisioning Login URL");
                // Login URL looks like: https://dixie.datcon.co.uk/sip-ps/accession/login?DirectoryNumber=${number} or
                // https://dixie.datcon.co.uk/pps/accession/login?DirectoryNumber=${number} so we need to take the host
                // only and append 'sip-ps/psi/publicglobalconfig'.
                URL url = new URL(provisioningLoginUrl);
                pgcUrl = url.getProtocol() + "://" + url.getHost() + "/sip-ps/psi/publicglobalconfig";
            }

            String responseString = HttpUtils.executeGet(pgcUrl);

            Object objectData = null;
            if (!StringUtils.isNullOrEmpty(responseString))
            {
                JSONParser parser = new JSONParser();
                JSONObject jsonObject = (JSONObject) parser.parse(responseString);
                objectData = jsonObject.get("AADApplicationId");
            }

            String applicationID = "";
            if (objectData != null)
            {
                applicationID = objectData.toString();
            }

            if (StringUtils.isNullOrEmpty(applicationID))
            {
                log.info("Retrieved an empty AADApplicationID");
            }
            else
            {
                log.info("Retrieved AADApplicationID");
                configurationService.setProperty(PROPERTY_APPLICATION_ID, applicationID);
            }
        }
        catch (IOException | ParseException | ClassCastException e)
        {
            log.error("Failed retrieving AADApplicationID", e);
        }
    }

    /**
     * Notice: This is a blocking method.
     * Sends a Core action REFRESH_SSO_TOKEN event to Electron to trigger it to retrieve a new SSO access token. It
     * then freezes the calling thread and waits for Electron to push the token back. Once the token is retrieved it's
     * returned.
     * <p>
     * There is no issue with concurrency since even if it gets called from 2 different threads, we block both threads
     * and wait for a response from Electron, and then unblock and return the token - but there is a slim change of
     * that happening anyway.
     * <p>
     * If Electron fails to retrieve the token it will return an error, which can either indicate network issue or
     * authentication issue. In case of an authentication issue we show an error and force logout, so user can
     * log back in when the app restarts - into the same account or a different one. But if it's a network issue we just
     * use the locally saved token instead.
     */
    public static String refreshSSOToken() {
        ScopedConfigurationService globalConfigService = DesktopUtilActivator.getConfigurationService().global();
        ScopedCredentialsStorageService credentialsService = DesktopUtilActivator.getCredentialsService().user();

        String applicationID = globalConfigService.getString(PROPERTY_APPLICATION_ID, "");
        String tokenCache = credentialsService.loadPassword(PROPERTY_ENCRYPTED_MSAL_CACHE);
        RefreshSSOToken refreshSSOToken = new RefreshSSOToken(applicationID, tokenCache);
        WISPAMotion wispaMotion = new WISPAMotion(WISPAMotionType.REFRESH_SSO_TOKEN, refreshSSOToken);
        DesktopUtilActivator.getWISPAService().notify(WISPANamespace.CORE, WISPAAction.MOTION, wispaMotion);

        log.info("SSO token refresh requested");

        PreLoginUtils.awaitEvent(EventType.LOGIN);

        if (SsoError.NETWORK_ERROR.equals(currentSSOError))
        {
            log.error("Network error while refreshing SSO token, use cached token");
            currentSSOToken = credentialsService.loadPassword(PROPERTY_ENCRYPTED_SSO_TOKEN);
        }
        else if (!hasSSOToken())
        {
            DesktopUtilActivator.getInsightsService().logEvent(InsightsEventHint.DESKTOP_UTIL_SSO_TOKEN.name(),
                                                              Map.of(SSO_TOKEN_REFRESHED.name(),
                                                                     false));
            // We failed to refresh SSO token, display an error message and force logout.
            log.error("Failed to refresh SSO token, clearing saved values");
            displaySSORefreshError();
        }
        else
        {
            DesktopUtilActivator.getInsightsService().logEvent(InsightsEventHint.DESKTOP_UTIL_SSO_TOKEN.name(),
                                                              Map.of(SSO_TOKEN_REFRESHED.name(),
                                                                     true));
        }

        return currentSSOToken;
    }

    /**
     * Display an error dialog to the user after failing to retrieve SSO token.
     */
    private static void displaySSORefreshError()
    {
        ResourceManagementService resources = DesktopUtilActivator.getResources();
        ErrorDialog dialog;
        String errorTitle = resources.getI18NString("service.gui.ERROR");
        String loginFailedMessage = resources.getI18NString("service.gui.LOGIN_MICROSOFT_ERROR");
        String refreshFailedMessage = resources.getI18NString("service.gui.REFRESH_TOKEN_MICROSOFT_ERROR");
        if (DesktopUtilActivator.getCommPortalService() == null) {
            // If we fail to log in to SIPPS, we don't want to force close the app as we can just clear the credentials
            // and show the Login screen. That's why we check for CommPortalService here, since it's started after
            // ProvisioningService - where we log in to SIPPS.
            log.warn("SSO login failed, displaying error");
            ConfigurationUtils.clearCredentialsAndLogout(false);
            dialog = new ErrorDialog(errorTitle, loginFailedMessage);
        }
        else
        {
            // But if user was already logged in, and we failed to refresh the token - force logout and close the app
            // since we cannot recover from it. Upon the next start user will be presented with a Login screen where
            // they can log in with SSO again, or even chose to log in with username and password - we don't care.
            log.warn("SSO token refresh failed, displaying error and forcing logout");
            dialog = new ErrorDialog(
                    errorTitle,
                    refreshFailedMessage,
                    ErrorDialog.OnDismiss.FORCE_LOGOUT_RESTART
            );
        }
        dialog.setModal(true);
        dialog.showDialog();
    }

    /**
     * Formats the string entered into the username field. If email login
     * is enabled, and the string contains a "@", we assume the subscriber
     * has entered an email and simply strip out any whitespace
     * Otherwise, we assume the subscriber has entered a phone number,
     * and remove any odd formatting and characters
     */
    private static void formatUserName()
    {
        String originalUserName = currentUsername;
        if (currentUsername.contains("@"))
        {
            // This looks like an email address. Removing an invalid character
            // in an email address probably isn't going to result in a valid
            // email so just remove any spaces as these may not be visible to the user.
            currentUsername = currentUsername.replaceAll(" ", "");
        }
        else
        {
            //  This is a phone number. Replace odd formatting and characters
            currentUsername = currentUsername.replaceAll(ConfigurationUtils.getPhoneNumberLoginRegex(), "");
        }

        if (!currentUsername.equals(originalUserName))
        {
            log.user("Log in attempted with username: "
                     + logHasher(originalUserName) + ". reformatted to: " +
                     logHasher(currentUsername));
        }
    }
}
