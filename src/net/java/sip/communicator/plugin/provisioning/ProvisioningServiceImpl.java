/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.provisioning;

import static net.java.sip.communicator.service.insights.parameters.ProvisioningParameterInfo.*;
import static net.java.sip.communicator.util.PrivacyUtils.*;
import static org.jitsi.util.Hasher.logHasher;
import static org.jitsi.util.SanitiseUtils.sanitiseFirstPatternMatch;

import java.awt.*;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.HttpStatus;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import net.java.sip.communicator.plugin.desktoputil.ErrorDialog;
import net.java.sip.communicator.plugin.desktoputil.PreLoginUtils;
import net.java.sip.communicator.plugin.desktoputil.ScreenInformation;
import net.java.sip.communicator.plugin.desktoputil.ErrorDialog.OnDismiss;
import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.analytics.AnalyticsParameter;
import net.java.sip.communicator.service.analytics.AnalyticsService;
import net.java.sip.communicator.service.credentialsstorage.CredentialsStorageService;
import net.java.sip.communicator.service.diagnostics.DiagnosticsService;
import net.java.sip.communicator.service.diagnostics.ReportReason;
import net.java.sip.communicator.service.httputil.HTTPResponseResult;
import net.java.sip.communicator.service.httputil.HttpUtils;
import net.java.sip.communicator.service.httputil.HttpUtils.HttpMethod;
import net.java.sip.communicator.service.httputil.HttpUrlParams;
import net.java.sip.communicator.service.httputil.SSOCredentials;
import net.java.sip.communicator.service.insights.InsightsService;

import net.java.sip.communicator.service.insights.InsightsEventHint;
import net.java.sip.communicator.service.insights.parameters.ProvisioningParameterInfo;
import net.java.sip.communicator.service.protocol.OperationSetCallPark;
import net.java.sip.communicator.service.provisioning.ProvisioningService;
import net.java.sip.communicator.service.threading.CancellableRunnable;
import net.java.sip.communicator.service.threading.ThreadingService;
import net.java.sip.communicator.service.wispaservice.WISPAAction;
import net.java.sip.communicator.service.wispaservice.WISPANamespace;
import net.java.sip.communicator.service.wispaservice.WISPANotion;
import net.java.sip.communicator.service.wispaservice.WISPANotionType;
import net.java.sip.communicator.service.wispaservice.WISPAService;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.OrderedProperties;
import net.java.sip.communicator.util.ServiceUtils;
import net.java.sip.communicator.util.launchutils.ProvisioningParams;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.util.Hasher;
import org.jitsi.util.OSUtils;
import org.jitsi.util.StringUtils;

/**
 * Provisioning service.
 *
 * @author Sebastien Vincent
 */
public class ProvisioningServiceImpl implements ProvisioningService
{
    /**
     * Logger of this class
     */
    private static final Logger logger
        = Logger.getLogger(ProvisioningServiceImpl.class);

    private static final Pattern SIP_PS_ERROR_PATTERN = Pattern.compile("^Error=(.*)$" , Pattern.MULTILINE | Pattern.DOTALL);

    /**
     * The time (in millis) that the config was last refreshed from the
     * provisioning server.  Other services can register to be informed when
     * this value changes, rather than having to monitor many individual
     * properties that come from the provisioning server.
     */
    public static final String LAST_PROVISIONING_UPDATE_TIME
        = "net.java.sip.communicator.LAST_UPDATE_TIME";

    /**
     * Name of the config update URL in the configuration service.
     */
    private static final String PROPERTY_UPDATE_URL
        = "net.java.sip.communicator.plugin.provisioning.CONFIG_UPDATE_URL";

    /**
     * Name of the provisioning URL in the configuration service.
     */
    private static final String PROPERTY_PROVISIONING_URL
        = "net.java.sip.communicator.plugin.provisioning.URL";

    /**
     * Name of the CDAP service provider ID in the configuration service.
     */
    static final String PROPERTY_CDAP_SP_ID
        = "net.java.sip.communicator.plugin.cdap.service_provider_id";

    /**
     * Name of the CDAP URL in the config.
     */
    static final String CDAP_URL = "net.java.sip.communicator.plugin.cdap.URL";

    /**
     * Name of the active user being used by configuration.
     */
    static final String PROPERTY_ACTIVE_USER =
            "net.java.sip.communicator.plugin.provisioning.auth.ACTIVE_USER";

    /**
     * Name of the provisioning username in the configuration service.
     */
    static final String PROPERTY_PROVISIONING_USERNAME
    = "net.java.sip.communicator.plugin.provisioning.auth.USERNAME";

    /**
     * Name of the provisioning password in the configuration service (HTTP
     * authentication).
     */
    static final String PROPERTY_PROVISIONING_PASSWORD
        = "net.java.sip.communicator.plugin.provisioning.auth";

    /**
     * Name of the property that contains the provisioning method (i.e. DHCP,
     * DNS, manual, ...).
     */
    private static final String PROVISIONING_METHOD_PROP
        = "net.java.sip.communicator.plugin.provisioning.METHOD";

    /**
     * Name of the property that contains enforce prefix list (separated by
     * pipe) for the provisioning. The retrieved configuration properties will
     * be checked against these prefixes to avoid having incorrect content in
     * the configuration file (such as HTML content resulting of HTTP error).
     */
    private static final String PROVISIONING_ALLOW_PREFIX_PROP
        = "provisioning.ALLOW_PREFIX";

    /**
     * Name of the enforce prefix property.
     */
    private static final String PROVISIONING_ENFORCE_PREFIX_PROP = "provisioning.ENFORCE_PREFIX";

    /**
     * Name of the BG Contacts enabled property
     */
    private static final String PROVISIONING__BG_CONTACTS = "net.java.sip.communicator.BG_CONTACTS_ENABLED";

    /**
     * The name of the property which specifies the update link in the
     * configuration file. Once getAndStoreConfig() has run, this will be
     * set correctly in the configuration service.
     */
    static final String PROP_UPDATE_LINK = "net.java.sip.communicator.UPDATE_LINK";

    /**
     * String representing the IM provisioning method in the configuration
     * service.
     */
    private static final String IM_PROVISION_SOURCE_PROP = "net.java.sip.communicator.im.IM_PROVISION_SOURCE";

    /**
     * String representing the name of the IM domain in the configuration
     * service.
     */
    private static final String IM_DOMAIN_PROP = "net.java.sip.communicator.im.IM_DOMAIN";

    /**
     * Property to indicate whether IM is enabled in the client.
     */
    private static final String IM_ENABLED_PROP = "net.java.sip.communicator.im.IM_ENABLED";

    /**
     * A config prefix that covers both WIRELESS and WIRED codecs.
     */
    private static final String ENCODING_CONFIG_PROP_WIRED_WIRELESS_SHARED
        = "net.java.sip.communicator.impl.neomedia.codec.EncodingConfiguration.WIRE";

    /**
     * A config value that tracks the last logged in CDAP provider.
     */
    static final String PROPERTY_LAST_CDAP_SP_ID =
            "net.java.sip.communicator.plugin.provisioning.LAST_CDAP_SP_ID";

    /**
     * Config prefix for WIRED codecs.
     */
    private static final String ENCODING_CONFIG_PROP_WIRED
        = ENCODING_CONFIG_PROP_WIRED_WIRELESS_SHARED + "D";

    /**
     * Config prefix for WIRED codecs.
     */
    private static final String ENCODING_CONFIG_PROP_WIRELESS
        = ENCODING_CONFIG_PROP_WIRED_WIRELESS_SHARED + "LESS";

    /**
     * Config prefix for default contact source
     */
    private static final String DEFAULT_CONTACT_SOURCE_PROP_PREFIX
        = "net.java.sip.communicator.DEFAULT_CONTACT_SOURCE_";

    /**
     * A config prefix for WIRED codecs.
     */
    private static final String DISABLE_VIDEO_UI
        = "net.java.sip.communicator.impl.gui.DISABLE_VIDEO_UI";

    /** Name of the service provider name in the config. */
    static final String PROPERTY_SERVICE_PROVIDER_NAME
        = "net.java.sip.communicator.plugin.provisioning.SERVICE_PROVIDER_NAME";

    /**
     * Indicates if VoIP is enabled by the user (this only has any effect if
     * VoIP is allowed in the CoS).
     */
    public static final String VOIP_ENABLED_PROP =
                  "net.java.sip.communicator.plugin.generalconfig.VOIP_ENABLED";

    /**
     * Name of the config tracking whether user is logged in with SSO.
     */
    static final String PROPERTY_IS_SSO_ACTIVE =
            "net.java.sip.communicator.plugin.provisioning.auth.SSO_ACTIVE";

    /**
     * Flag for checking if password was remembered.
     */
    private static final String REMEMBERED_PASSWORD = "net.java.sip.communicator.plugin.desktoputil.credentials";

    /**
     * List of config properties whose values expose personal data.
     *
     * The URL_SERVICES regex is to address lines like these:
     * net.java.sip.communicator.impl.gui.main.urlservices.crm.2.url.
     *
     * This differs from <code>SANITISE_CONFIG_PATTERN_LIST</code> in that this
     * is used to check property names as independent strings, whereas the former
     * is meant for full server config lines (i.e. "property=value").
     */
    private static final List<Pattern> SANITISE_CONFIG_PROPERTY_PATTERN_LIST = List.of(
            Pattern.compile(ACCOUNT_UID),
            Pattern.compile(AUTHORIZATION_NAME),
            Pattern.compile(DISPLAY_NAME),
            Pattern.compile(GLOBAL_DISPLAY_NAME),
            Pattern.compile(NUMBER),
            Pattern.compile(URL_SERVICES + "\\.[^.]+\\.[0-9]+\\.name"),
            Pattern.compile(URL_SERVICES + "\\.[^.]+\\.[0-9]+\\.url"),
            Pattern.compile(USER_ID),
            Pattern.compile(VOICEMAIL_CHECK_URI),
            Pattern.compile(VOICEMAIL_URI),
            Pattern.compile(SERVER_ADDRESS)
    );

    /**
     * List of config entries that expose personal data as their values (the part following
     * the "=" sign); these regex statements allow us to pick out the values associated with
     * these entries and apply sanitisation methods on them.
     *
     * The URL_SERVICES regex is to address lines like these:
     * net.java.sip.communicator.impl.gui.main.urlservices.crm.2.url=https
     *
     * In this case, only the part after the "=" sign (the "https") would be picked out for hashing.
     */
    private static final List<Pattern> SANITISE_CONFIG_PATTERN_LIST = SANITISE_CONFIG_PROPERTY_PATTERN_LIST
            .stream()
            .map(entry -> Pattern.compile(entry.pattern() + "=(.*)"))
            .collect(Collectors.toList());

    /**
     * List of allowed configuration prefixes.
     */
    private List<String> allowedPrefixes = new ArrayList<>();

    /**
     * Runnable for getting config. Synchronisation policy - must use the
     * runnableLock before accessing this variable.
     */
    private CancellableRunnable mConfigSyncRunnable;

    /**
     * Lock for accessing the runnable.
     */
    private static final Object sConfigSyncRunnableLock = new Object();

    /**
     * Access to the config service
     */
    private final ConfigurationService mConfig;

    /**
     * Access to the credentials service
     */
    private final CredentialsStorageService mCredsService;

    /**
     * Access to the analytics service
     */
    private final AnalyticsService mAnalyticsService;

    private final InsightsService mInsightsService;

    /**
     * Access to the threading service
     */
    private final ThreadingService mThreadingService;

    /**
     * Access to the resource management service
     */
    private final ResourceManagementService mResourceService =
        ProvisioningActivator.getResourceService();

    /**
     * If true, then we have got some config stored and valid:
     */
    private boolean mStoredConfig = false;

    /**
     * If true, the user is not permitted to log in when retrieving
     * config fails, even if they have valid stored config.
     */
    private boolean mTerminalError = true;

    /**
     * Track when we hit an authFailedError so the first time this happens we can infer that the user's
     * password has been changed and show them an appropriately worded dialog to re-enter their password.
     */
    private boolean hitAuthFailedError = false;

    /**
     * We start off trying a POST request to SIP-PS (more secure than a GET, as the password will be in the content, not
     * the URL). Down-level EAS doesn't support this; if we fail with the POST we try with a GET.
     */
    private boolean fallBackToGetRequest = false;

    /**
     * We sometimes want to save credentials from previous failed requests, as sometimes we can re-use the previously
     * entered username and/or password and save the user from having to unnecessarily re-enter these.
     */
    private Credentials savedCredentials;

     /**
      * The default audio system (wasapi for Windows 7 or 8 and portaudio for
      * all other OSs).
      */
    private static String mAudioSystem = null;

     /**
      * A map of the contact source strings returned by provisioning server to
      * the strings that the client expects in the config.
      */
     private static final Map<String, String> contactSources =
             new HashMap<>(4);
     static
     {
         contactSources.put("Outlook", "Microsoft Outlook");
         contactSources.put("CommPortal", "CommPortal");
         contactSources.put("MacAddressBook", "Address Book");
     }

    /**
     * Time constant used for scheduled tasks.
     */
    private static final long ONE_DAY_IN_MILLISECONDS = 24*60*60*1000L;

    /**
     * PropertyChangeListener for username updates.
     */
    private PropertyChangeListener mUsernameChangeListener;

    /**
      * An enum mapping error strings returned by the provisioning server to
      * the client, to the following:
      *  - error strings to display to the user.
      *  - whether or not the error cases are terminal (i.e. whether the
      *    client should try to start using saved config, if any exists).
      *  - whether or not the errors relate to the subscriber (i.e. something
      *    that could have been caused by the user entering the wrong details).
      *
      * This enum currently contains all of the possible strings that SIP-PS
      * might return.  However, in case they change, these can be found in the
      * following places.
      *
      *  - SIP-PS V1 (built-in):
      *    http://enfieldsvn/repos/ucp/trunk/commp/code/dcl/mwp/servlet/pps/TextResources.java
      *  - SIP-OS V2 (endpoint packs):
      *    http://edinsvn/repos/ucp/trunk/sipps/src/dcl/sipps/prh/userdata/DataStore.java
      */
     public enum ErrorResponse
     {
         AuthFailed("plugin.provisioning.ERROR_AUTH_FAILED", true, true),
         SubNotFound("plugin.provisioning.ERROR_AUTH_FAILED", true, true),
         BadSPID("plugin.provisioning.ERROR_AUTH_FAILED", true, true),
         BadCos("plugin.provisioning.ERROR_BAD_SUBSCRIBER", true, true),
         BadSubscriberType("plugin.provisioning.ERROR_BAD_SUBSCRIBER", true, true),
         ClientOutOfDate("plugin.provisioning.ERROR_CLIENT_OUT_OF_DATE", true, false),
         FailedGet("plugin.provisioning.ERROR_SERVER_ERROR", false, false),
         InternalError("plugin.provisioning.ERROR_SERVER_ERROR", false, false),
         MissingParameter("plugin.provisioning.ERROR_MISSING_PARAMETER", true, true),
         NoPasswordSupplied("plugin.provisioning.ERROR_MISSING_PARAMETER", true, true),
         MissingUsername("plugin.provisioning.ERROR_MISSING_USERNAME", true, true),
         MissingUsernameEmail("plugin.provisioning.ERROR_MISSING_USERNAME_EMAIL", true, true),
         RetryExceeded("plugin.provisioning.ERROR_SUBSCRIBER_BLOCKED", true, true),
         SubscriberBlocked("plugin.provisioning.ERROR_SUBSCRIBER_BLOCKED", true, true);

         /**
          * The resource string used to retrieve the error string to display
          * to the user.
          */
         private final String mResourceString;

         /**
          * True if this error is terminal (i.e. the client should not try to
          * start using saved config, even if some exists).
          */
         private final boolean mTerminal;

         /**
          * Whether this is a subscriber or non-subscriber error (i.e.
          * something that could have been caused by the user entering the
          * wrong details, e.g. account locked out vs. client version
          * incorrect or server internal error).
          */
         private final boolean mSubscriberError;

         /**
          * Constructor
          *
          * @param resourceString  The resource string used to retrieve the
          *                        string to display to the user.
          * @param terminal        Whether or not this error is terminal.
          * @param subscriberError Whether or not this is a subscriber-related
          *                        error.
          */
         ErrorResponse(String resourceString,
                       boolean terminal,
                       boolean subscriberError)
         {
             mResourceString = resourceString;
             mTerminal = terminal;
             mSubscriberError = subscriberError;
         }

         /**
          * Get an ErrorResponse from the error string returned by the server.
          *
          * @param serverError The error string
          *
          * @return The ErrorResponse
          */
         public static ErrorResponse fromServerError(String serverError)
         {
             try
             {
                 logger.info("Trying to get ErrorResponse object for " +
                             serverError);
                 return ErrorResponse.valueOf(serverError);
             }
             catch (IllegalArgumentException e)
             {
                 logger.error("Failed to get ErrorResponse object for " +
                              serverError, e);
                 return null;
             }
         }

         /**
          * @return The resource string that maps to the error string to
          *         display to the user.
          */
         public String getResourceString()
         {
             return mResourceString;
         }

         /**
          * @return true when the error is terminal
          */
         public boolean isTerminal()
         {
             return mTerminal;
         }

         /**
          * @return true when this is a subscriber error
          */
         public boolean isSubscriberError()
         {
             return mSubscriberError;
         }
     }

    /**
     * Adds a listener for PROPERTY_PROVISIONING_USERNAME,
     * which should be called when we first get some user config after
     * a user logs in.
     */
    private void registerConfigListener()
    {
        mUsernameChangeListener = onUsernameUpdate();
        mConfig.global()
                .addPropertyChangeListener(
                        PROPERTY_PROVISIONING_USERNAME,
                        mUsernameChangeListener);
    }

    /**
     * PropertyChangeListener that schedules the SIP-PS polling task
     * once the username property has been populated following a
     * successful login.
     */
    private PropertyChangeListener onUsernameUpdate()
    {
        return (evt) ->
        {
            if (evt.getNewValue() != null)
            {
                logger.debug("Username now written to config. " +
                             "Begin scheduler to check for config updates via SIP-PS");
                scheduleTaskToPollPPSConfig();
            }
        };
    }

    /**
      * Constructor.
      */
     ProvisioningServiceImpl()
     {
         // check if UUID is already configured
         mConfig = ProvisioningActivator.getConfigurationService();

         String cdapUrl = mConfig.global().getString(CDAP_URL);
         if (mConfig.global().getString(PROPERTY_CDAP_SP_ID) == null && !StringUtils.isNullOrEmpty(cdapUrl)) {
             // Wait for user to choose service provider if it is not saved from last run
             PreLoginUtils.awaitEvent(PreLoginUtils.EventType.SERVICE_PROVIDER);
         }
         mAnalyticsService = ProvisioningActivator.getAnalyticsService();
         mInsightsService = ProvisioningActivator.getInsightsService();
         mCredsService = ProvisioningActivator.getCredentialsStorageService();
         mThreadingService = ProvisioningActivator.getThreadingService();

         String uuid = ConfigurationUtils.getUuid();

         if(StringUtils.isNullOrEmpty(uuid))
         {
             uuid = UUID.randomUUID().toString();
             ConfigurationUtils.setUuid(uuid);
         }

         ConfigurationUtils.calculateAndStoreOsVersion();

         mStoredConfig =
                    mConfig.global().getProperty(PROPERTY_PROVISIONING_USERNAME) != null;

         if (OSUtils.IS_MAC)
         {
             // Temporary change to not override any existing audio system
             // config on Mac so it is possible for us to switch to core
             // audio for testing without the config being overwritten on
             // start-up.
//             String currentAudioSystem = mConfig.global().getString(
//                         "net.java.sip.communicator.impl.neomedia.audioSystem");
             mAudioSystem = "coreaudio";
         }
         else
         {
             // Set the default audio system to wasapi for Windows.
             mAudioSystem = "wasapi";
         }

         registerConfigListener();

         if (StringUtils.isNullOrEmpty(cdapUrl))
         {
             logger.info("Tailored branding - fetching applicationID");
             // This is a tailored branding - so initiate fetching applicationID
             // as we have provisioning URL already - unlike with CDAP where we
             // first need user to select a CDAP provider.
             PreLoginUtils.fetchApplicationID();
         }
     }

     /**
      * Starts provisioning.
      */
     void start()
     {
         // If we already have a user then 'give it a go' at using this
         // user until we're told otherwise.
         String username = getProvisioningNumber();
         logger.info("Starting with username " + logHasher(username));

         if (!StringUtils.isNullOrEmpty(username))
         {
             mConfig.setActiveUser(username);
             // Store the active user's DN as a salt to protect Personally Identifiable
             // Information.  If we change active user, then it is right that this will
             // be updated.
             Hasher.setSalt(username);
             notifyCoreNotion();

             mCredsService.setActiveUser();
         }

         getAndStoreFreshConfig();

         /// Now we (probably) have the config, it's reasonable to start the
         // timer that sends the analytic that reports we are running
         mAnalyticsService.startSysRunningTimer();
         mInsightsService.startCollection();

         if (!mTerminalError &&
             mConfig.global().getProperty(PROPERTY_PROVISIONING_USERNAME) != null)
         {
             logger.debug("Username config exists! " +
                          "Begin scheduler to check for config updates " +
                          "via SIP-PS");
             scheduleTaskToPollPPSConfig();
         }
     }

    /**
     * Sets up a task to poll for config via SIP-PS. This method does nothing
     * if a scheduled PPS task has already been submitted.
     */
    private void scheduleTaskToPollPPSConfig()
    {
        synchronized (sConfigSyncRunnableLock)
        {
            if (hasActiveScheduler())
            {
                logger.debug("Returning as we already have a " +
                             "scheduled update task running.");
                return;
            }

            mConfigSyncRunnable = new CancellableRunnable()
            {
                public void run()
                {
                    logger.info("Running a scheduled SIP-PS config request");
                    getAndStoreConfig();
                }
            };

            // By this point, we will already have made our 1st PPS config request, so
            // request again in 24 hours.
            mThreadingService.scheduleAtFixedRate("SIP-PS config request",
                                                  mConfigSyncRunnable,
                                                  ONE_DAY_IN_MILLISECONDS,
                                                  ONE_DAY_IN_MILLISECONDS);
        }
    }

    /**
      * Get and store a fresh set of config using the provisioning URL from the branding.
      */
     public void getAndStoreFreshConfig()
     {
         getAndStoreConfig(getProvisioningUrl(), false);
     }

     /**
      * Get and store config from the update URL if that exists from a previous SIP-PS response, but
      * if not then falling back to the provisioning URL from the branding.
      */
     public void getAndStoreConfig()
     {
         boolean isConfigRefresh = true;
         String configUrl = mConfig.user().getString(PROPERTY_UPDATE_URL);

         if (StringUtils.isNullOrEmpty(configUrl))
         {
             configUrl = getProvisioningUrl();
             isConfigRefresh = false;
         }

         getAndStoreConfig(configUrl, isConfigRefresh);
     }

     /**
      * Get the config from the presented URL and store it in the config file
      *
      * @param url The URL whence to get config.
      * @param isConfigRefresh Is this a refresh of the config using the update URL, or getting
      *        fresh config using the provisioning URL.
      */
     private void getAndStoreConfig(String url, boolean isConfigRefresh)
     {
         if (StringUtils.isNullOrEmpty(url))
         {
             logger.warn("No config URL supplied");
             return;
         }

         String config = retrieveConfigurationFile(url, isConfigRefresh);

         if (config != null)
         {
             logger.info("New config from URL: " + url + " (length " + config.length() + ") refresh? " + isConfigRefresh);
             /*
              * For unclear historic reasons, the provisioning URL is stored in the global config.
              * This poses a problem when a tailored branding (which has the provisioning URL written to
              * the client's default config store in the build process) wants to change a provisioning URL
              * - any existing users will have the old value stored in the global config, which takes
              * precedence over the default config.  In lieu of removing the field from the global
              * config, the build process is amended to *-prefix the field in the default config store iff
              * the branding has a value for the provisioning URL, thereby making the default config store
              * value take precedence over the value in the global config. In short - all users have
              * the provisioning URL in their global config store, but tailored branded clients have a
              * *-prefixed field in their default config, populated by the build process, taking precedence.
              */
             String urlProperty = isConfigRefresh ? PROPERTY_UPDATE_URL : PROPERTY_PROVISIONING_URL;

             mConfig.global().setProperty(urlProperty, url);

             updateConfiguration(config);
             sendAnalyticsEvent(url);
         }

         // Update any accounts that may or may not have been
         // created but only if this is a config update - otherwise the
         // normal start up procedures will handle this for us
         //
         if (isConfigRefresh)
         {
             ProvisioningActivator.getAccountManager().accountsChanged();
         }

         // Finally, notify Electron that we've received some config as it may
         // need to change the UI that is displayed to the user accordingly.
         WISPAService wispaService = ProvisioningActivator.getWISPAService();
         if (wispaService != null)
         {
             wispaService.notify(WISPANamespace.SETTINGS, WISPAAction.UPDATE);
             notifyCoreNotion();
         }
         else
         {
             logger.warn("Could not notify WISPA about configuration update.");
         }
     }

     /**
      * Send an analytics event for the retrieval of config.
      */
     @VisibleForTesting
     void sendAnalyticsEvent(String url)
     {
         List<String> parameters = new ArrayList<>();

         parameters.add(AnalyticsParameter.PARAM_USING_HTTPS);
         parameters.add(Boolean.toString(url.startsWith("https://")));

         // Find out details of the Jabber accounts
         List<String> jabberAccs = getJabberAccounts();

         parameters.add("Nb IM Acc");
         parameters.add(String.valueOf(jabberAccs.size()));

         for (int i = 0; i < jabberAccs.size(); i++)
         {
             String jabberAcc = jabberAccs.get(i);
             parameters.add("IM domain " + i);
             parameters.add(mConfig.user().getString(jabberAcc + ".SERVER_ADDRESS"));
         }

         // Get the audio device details:
         String audioDevicePrefix =
             "net.java.sip.communicator.impl.neomedia.audioSystem." +
                 mAudioSystem + ".";
         String[] deviceTypes = {"capture", "notify", "playback"};

         for (String deviceType : deviceTypes)
         {
             String devices = mConfig.user()
                     .getString(audioDevicePrefix + deviceType + "Device_list");

             if (devices != null)
             {
                 // The device list looks like
                 // ["<device>", "<device2>,...] or ["<device>"] if only one device.
                 // We are only interested in the first of these devices...
                 String firstDevice = devices.split("\", \"")[0];
                 firstDevice = firstDevice.substring(2).replaceAll("\"\\]", "");

                 parameters.add(deviceType + " device");
                 parameters.add(firstDevice);
             }
         }

         parameters.add("Contact source");
         parameters.add(mConfig.user().getString("net.java.sip.communicator.PERSONAL_CONTACT_STORE"));

         // Add an analytic to say we're using SIP media security headers if we are (see PRD 14452).
         if (mConfig.user().getBoolean("net.java.sip.communicator.ENABLE_3GPP_MEDIA_HEADERS", false))
         {
             logger.debug("Adding analytics parameter for using 3GPP headers");
             parameters.add("3GPP media headers");
             parameters.add("enabled");
         }

         mAnalyticsService.onEvent(AnalyticsEventType.CONFIG_RETRIEVED, parameters.toArray(new String[parameters.size()]));
     }

    private void notifyCoreNotion() {
        WISPAService wispaService = ProvisioningActivator.getWISPAService();
        if (wispaService != null)
        {
            String activeUser = mConfig.global().getString(PROPERTY_ACTIVE_USER);
            WISPANotion wispaNotion = new WISPANotion(WISPANotionType.SEND_SALT, activeUser);
            wispaService.notify(WISPANamespace.CORE, WISPAAction.NOTION, wispaNotion);
        }
    }

    /**
     * @return A list of the account IDs of all jabber accounts in the current
     *         config.
     */
    private List<String> getJabberAccounts()
    {
        logger.debug("Getting a list of jabber accounts");

        String jabberPrefix = "net.java.sip.communicator.impl.protocol.jabber";
        List<String> potentialJabberAccs = mConfig.user().getPropertyNamesByPrefix(
                                                            jabberPrefix, true);

        List<String> jabberAccs = new ArrayList<>();

        for (String potentialAcc : potentialJabberAccs)
        {
            if (mConfig.user().getString(potentialAcc).startsWith("acc"))
            {
                jabberAccs.add(potentialAcc);
            }
        }

        return jabberAccs;
    }

     /**
      * Indicates if the provisioning has been enabled.
      *
      * @return <tt>true</tt> if the provisioning is enabled, <tt>false</tt> -
      * otherwise
      */
     public String getProvisioningMethod()
     {
         String provMethod = mConfig.global().getString(PROVISIONING_METHOD_PROP);

         if (provMethod == null || provMethod.length() <= 0)
         {
             provMethod = mResourceService.getSettingsString(
                             "plugin.provisioning.DEFAULT_PROVISIONING_METHOD");

             if (provMethod != null && provMethod.length() > 0)
                 setProvisioningMethod(provMethod);
         }

         return provMethod;
     }

     /**
      * Enables the provisioning with the given method. If the provisioningMethod
      * is null disables the provisioning.
      *
      * @param provisioningMethod the provisioning method
      */
     public void setProvisioningMethod(String provisioningMethod)
     {
         mConfig.global().setProperty(PROVISIONING_METHOD_PROP, provisioningMethod);
     }

     /**
      * Returns the provisioning URL.
      *
      * @return the provisioning URL
      */
     public String getProvisioningUrl()
     {
         return mConfig.global().getString(PROPERTY_PROVISIONING_URL);
     }

    /**
     * Returns the provisioning DN if any.
     *
     * @return provisioning DN or null if the DN is not set
     */
    public String getProvisioningNumber()
    {
        return mConfig.global().getString(PROPERTY_PROVISIONING_USERNAME);
    }

    /**
     * Handle failure to retrieve the configuration file, producing
     * sensible error pop up.
     *
     * @param networkError True if this was (probably) the result of a
     *                     network error.
     */
    private void retrieveConfigurationFileFailed(boolean networkError)
    {
        if (PreLoginUtils.isShutdownInitiated()) {
            // Avoid handing error as we had to unblock Java to allow Felix to
            // close the bundle - which caused login to execute - and fail.
            logger.info("Shutdown initiated, avoid handling error");
            return;
        }

        // Retrieving config failed. The user either submitted blank
        // credentials, cancelled the login, or selected a CDAP server with
        // no valid SIP PS config.
        String[] params = {"Stored", String.valueOf(mStoredConfig)};
        mAnalyticsService.onEvent(AnalyticsEventType.GETTING_CONFIG_FAILED,
                                  params);

        if (!mStoredConfig)
        {
            logger.warn("No stored config - shut down and forget the credentials");

            // We should forget the user to avoid leaving the app in a bad state
            // where the global config says there is a user, but there is no user
            // config as we failed to get it! If we don't next time the app starts
            // up it will think there is a user that exists and certain bundles
            // will attempt to start up as if the user config exists and fails.
            forgetCredentials(true);

            String errorMessage;

            if (networkError)
            {
                errorMessage = mResourceService.getI18NString(
                    "service.gui.LOGIN_NETWORK_ERROR");
            }
            else
            {
                errorMessage = mResourceService.getI18NString(
                    "service.gui.LOGIN_INTERNAL_ERROR");
            }

            errorMessage += " " + mResourceService.getI18NString(
                        "plugin.cdap.APPLICATION_CLOSING");

            // Send an error to Electron, which quits the app when dismissed
            displayLoginError(errorMessage, true);
        }
        else
        {
            handleConfigError();
        }
    }

    /**
     * Retrieve configuration file from provisioning URL.
     * This method is blocking until configuration file is retrieved from the
     * network or we hit an exception.
     *
     * @param url provisioning URL
     * @boolean isConfigRefresh i.e. do we already have SIP-PS config (or are we getting it for the
     * first time after login)
     * @return provisioning config downloaded
     */
    private String retrieveConfigurationFile(String url, boolean isConfigRefresh)
    {
        try
        {
            // Reset the terminal error state, as we're about to send a
            // new provisioning request.
            setTerminalError(false);

            String arg = null;
            String[] args = null;

            // Process the URL, replacing any parameters that we know and that
            // will not change
            url = resolveParametersInUrl(url);

            if(url.contains("?"))
            {
                /* do not handle URL of type http://domain/index.php? (no
                 * parameters)
                 */
                if((url.indexOf('?') + 1) != url.length())
                {
                    arg = url.substring(url.indexOf('?') + 1);
                    args = arg.split("&");
                }
                url = url.substring(0, url.indexOf('?'));
            }

            HttpUrlParams urlParams = HttpUrlParams.getHttpUrlParamsFromArgs(args);

            // Clear any saved credentials from previous spins.
            savedCredentials = null;
            hitAuthFailedError = false;
            mStoredConfig = (mConfig.global().getProperty(PROPERTY_PROVISIONING_USERNAME) != null);

            while (true)
            {
                if (PreLoginUtils.isShutdownInitiated()) {
                    logger.info("Shutdown initiated, break the loop to" +
                                "allow Felix to close all the bundles");
                    return null;
                }

                if (ProvisioningParams.getSubscriber() != null && !PreLoginUtils.isLoggedIn()) {
                    logger.info("Logging in with a link, password provided:" +
                                (ProvisioningParams.getPassword() != null ? "true" : "false"));
                    savedCredentials = new UsernamePasswordCredentials(
                            ProvisioningParams.getSubscriber(),
                            ProvisioningParams.getPassword()
                    );
                }

                // Try posting a config request to the Accession URL.
                HTTPResponseResult res = makeHttpRequestForConfig(url,
                                                                  (fallBackToGetRequest ? HttpMethod.GET : HttpMethod.POST),
                                                                  urlParams);

                // Store off the credentials from this request - if we need to try again, the username and/or
                // password will be useful to remember.
                if (res != null && !(res.getCredentials() instanceof SSOCredentials))
                {
                    savedCredentials = res.getCredentials();
                }

                if ((res == null) || (res.getStatusCode() != HttpStatus.SC_OK) || (res.getContentLength() == 0))
                {
                    // Retrieving config failed.
                    logger.error("Config retrieval failed." +
                        " content length " + (res == null ? "null" : res.getContentLength()) +
                        " status code " + (res == null ? "null" : res.getStatusCode()));

                    if (!fallBackToGetRequest)
                    {
                        logger.info("Fallback to GET requests and try again");
                        fallBackToGetRequest = true;
                        continue;
                    }

                    logLoginAnalytics(res);

                    retrieveConfigurationFileFailed(false);
                    return null;
                }

                // We have config from the server.  Parse the result into a string
                String configFromServer = convertResultToString(res);

                // Remove Personal Data from the string (i.e. subscriber DNs)
                String loggableConfigFromServer = sanitiseServerConfigForLogging(configFromServer);
                logger.info("Config from server\n" + loggableConfigFromServer);

                try
                {
                    Matcher errorMatcher = SIP_PS_ERROR_PATTERN.matcher(configFromServer);

                    if (errorMatcher.matches())
                    {
                        if (handleErrorFromServer(errorMatcher.group(1).trim()))
                        {
                            continue;
                        }
                        else
                        {
                            return null;
                        }
                    }
                    else if (!configFromServer.contains("net.java.sip.communicator"))
                    {
                        // The response content doesn't contain any valid config strings, possibly
                        // because we're connected to a public network that requires the user to
                        // log in, or SIP-PS is behind a WAF that has returned some html. If we have
                        // any saved config then start the client with that, else show an error and
                        // close the client.
                        logger.warn("Unrecognized config returned");

                        if (!fallBackToGetRequest)
                        {
                            logger.info("Fallback to GET requests and try again");
                            fallBackToGetRequest = true;
                            continue;
                        }
                        logLoginAnalytics(new Exception("Unrecognized configuration"));

                        retrieveConfigurationFileFailed(false);
                        return null;
                    }

                    // The returned config from SIP PS is not just an error response so process it now.
                    logger.debug("Successfully retrieved config from server");
                    hitAuthFailedError = false;
                    String lastCdapSpId = mConfig.global().getString(PROPERTY_LAST_CDAP_SP_ID, "");
                    String processedConfig = processConfigFromServer(configFromServer);

                    String cdapSpID = mConfig.global().getString(PROPERTY_CDAP_SP_ID, "");
                    if (!cdapSpID.equals(lastCdapSpId) && !cdapSpID.isEmpty())
                    {
                        // User has logged into CDAP for the first time (or after changing the
                        // service provider) - log the event.
                        String providerName = mConfig.global().getString(PROPERTY_SERVICE_PROVIDER_NAME);
                        mInsightsService.logEvent(
                                InsightsEventHint.USER_CDAP_CONFIRM_PROVIDER.name(),
                                Map.of(ProvisioningParameterInfo.CDAP_NAME.name(), providerName)
                        );

                        // We are now successfully logged into CDAP version of the app, save the provider id
                        // so we can don't send a analytic EVENT_USER_CDAP_CONFIRM_PROVIDER next time
                        // user logs out and then logs in without changing the service provider.
                        mConfig.global().setProperty(PROPERTY_LAST_CDAP_SP_ID, cdapSpID);
                    }

                    if (mConfig.user() != null &&
                        mConfig.user().getBoolean(IM_ENABLED_PROP, true))
                    {
                        updateImConfiguration(isConfigRefresh, processedConfig);
                    }

                    if (PreLoginUtils.hasSSOToken())
                    {
                        // If this was an SSO login, and it was successful - save the flag.
                        logger.info("Login with SSO successful");
                        ProvisioningActivator.getConfigurationService().global()
                                .setProperty(PROPERTY_IS_SSO_ACTIVE, true);
                    }

                    // Successful login - clear login data.
                    PreLoginUtils.clearManualLoginData();
                    ProvisioningParams.clear();

                    logLoginAnalytics(res);

                    return processedConfig;
                }
                catch (Exception e)
                {
                    logLoginAnalytics(e);
                    logger.error("Error updating config", e);
                    return null;
                }
            }
        }
        catch (IOException e)
        {
            // Almost certainly a network error.
            logLoginAnalytics(e);
            logger.warn("Network error: ", e);
            retrieveConfigurationFileFailed(true);
            return null;
        }
        catch (Exception e)
        {
            // Some other error; probably not network.
            logLoginAnalytics(e);
            logger.warn("Config retrieval failed: ", e);
            retrieveConfigurationFileFailed(false);
            return null;
        }
    }

    private void logLoginAnalytics(Exception exception)
    {
        logLoginAnalytics(null, exception, null);
    }

    private void logLoginAnalytics(ErrorResponse errorResponse)
    {
        logLoginAnalytics(null, null, errorResponse);
    }

    private void logLoginAnalytics(HTTPResponseResult responseResult)
    {
        logLoginAnalytics(responseResult, null, null);
    }

    private void logLoginAnalytics(
            HTTPResponseResult responseResult,
            Exception exception,
            ErrorResponse errorResponse
    )
    {
        String rememberedPassword = mConfig.global().getString(REMEMBERED_PASSWORD);
        Principal userPrincipal = savedCredentials != null ? savedCredentials.getUserPrincipal() : null;
        String username = userPrincipal != null ? userPrincipal.getName() : "";

        Map<String, Object> parameterInfoObjectMap = new HashMap<>();
        parameterInfoObjectMap.put(USER_NAME.name(), username);
        parameterInfoObjectMap.put(USER_REMEMBERED_PASSWORD.name(), rememberedPassword);

        if (responseResult != null)
        {
            parameterInfoObjectMap.put(ProvisioningParameterInfo.LOG_IN_RESPONSE.name(), responseResult);
        }

        if (exception != null)
        {
            parameterInfoObjectMap.put(ProvisioningParameterInfo.LOG_IN_EXCEPTION.name(), exception);
        }

        if (errorResponse != null)
        {
            parameterInfoObjectMap.put(ProvisioningParameterInfo.LOG_IN_ERROR_RESPONSE.name(), errorResponse);
        }

        mInsightsService.logEvent(InsightsEventHint.PROVISIONING_LOG_IN.name(),
                                parameterInfoObjectMap);
    }

    /**
     * Process an error response from the server
     * @param errorMessage The message from the server.
     * @return true when we should continue trying to get config, false if we should stop trying.
     */
    private boolean handleErrorFromServer(String errorMessage)
    {
        logger.error("Server returned provisioning error: " + errorMessage);

        // The server has returned an error.  Assume to start with that this is a terminal error and we should
        // delete user credentials, as that is safest and should give the best default UX.
        boolean forgetCreds = true;
        setTerminalError(true);

        // If there is a mapping between the error message returned by the server and a string to display to
        // the user, use that string as the error message.
        ErrorResponse errorResponse = ErrorResponse.fromServerError(errorMessage);

        logLoginAnalytics(errorResponse);

        if (errorResponse == ErrorResponse.ClientOutOfDate &&
            ProvisioningActivator.getUpdateService() != null)
        {
            logger.warn("Client is out of date");
            // N.B. Client-triggered forced updates are only supported on Windows. This method has no effect on Mac.
            ProvisioningActivator.getUpdateService().forceUpdate();
            return false;
        }
        else if (errorResponse == ErrorResponse.AuthFailed &&
                 mStoredConfig &&
                 !hitAuthFailedError)
        {
            // Authentication failed, even though we have stored config and there has been no previous
            // failure to authenticate in this session. This probably means the remote password has
            // changed. Warn the user before reprompting for their credentials.
            logger.error("Failed to authenticate using stored config. Prompting user.");
            hitAuthFailedError = true;
            errorMessage = mResourceService.getI18NString(
                "plugin.provisioning.PASSWORD_CHANGED_REMOTELY",
                 new String[]
                 {
                    mResourceService.getI18NString("service.gui.APPLICATION_NAME")
                 });
            setTerminalError(false);

            // If we have any saved credentials, replace them with new ones that just contain the
            // username so we can fill that in for the dialog requesting the new password.
            if (savedCredentials != null && !(savedCredentials instanceof SSOCredentials))
            {
                savedCredentials = new UsernamePasswordCredentials(savedCredentials.getUserPrincipal().getName(), null);
            }
        }
        else if (PreLoginUtils.hasSSOToken())
        {
            errorMessage = mResourceService.getI18NString(
                    "service.gui.LOGIN_MICROSOFT_ERROR");
            logger.warn("Failed to authenticate using SSO");
        }
        else if (errorResponse != null)
        {
            logger.error("Error from server " + errorResponse);
            errorMessage = mResourceService.getI18NString(errorResponse.getResourceString());
            setTerminalError(errorResponse.isTerminal());

            // If this is a subscriber error, the user might just have entered the wrong username or
            // password, so make sure we delete their credentials.
            forgetCreds = errorResponse.isSubscriberError();

            if (forgetCreds)
            {
                savedCredentials = null;
            }
        }

        displayLoginError(errorMessage, false);

        if (forgetCreds)
        {
            // This is a subscriber error, so forget the entered username and password (but not CDAP
            // selection) and continue to the top of the loop, so the user will be re-shown the login dialog.
            logger.warn("Subscriber error - prompt for login");
            forgetCredentials(false);

            // Remove any account details - the account details may have changed and we don't want the old data
            // to hang around.  If it's not changed then there will be no overall effect as the new config will
            // contain what we need.
            removeProvisionedAccounts();
            return true;
        }
        else
        {
            // This isn't a subscriber error, so there's no point deleting the user's credentials and
            // re-displaying the login prompt.  Call handleConfigError to shut down the client cleanly
            // if the error is terminal or there's no saved config. Otherwise, returning null will cause
            // us to start up with saved config.
            logger.warn("Config error. Terminal? " + mTerminalError + ", saved config? " + mStoredConfig);
            handleConfigError();
            return false;
        }
    }

    /**
     * Make any necessary amendments to the config we've received from the server.
     * @param configFromServer what we've received from the server
     * @return the Config we've received from the server with any required
     * adjustments made
     */
    @VisibleForTesting
    protected String processConfigFromServer(String configFromServer)
    {
        String webSize = "net.java.sip.communicator.impl.browserpanel.size.WIDTH=${null}\n" +
                         "net.java.sip.communicator.impl.browserpanel.size.HEIGHT=${null}\n" +
                         "net.java.sip.communicator.impl.browserpanel.BrowserPanelServiceImpl_BrowserFrame.height=${null}\n" +
                         "net.java.sip.communicator.impl.browserpanel.BrowserPanelServiceImpl_BrowserFrame.width=${null}\n";

        String configTemplate =
            webSize +
            "net.java.sip.communicator.impl.neomedia.audioSystem=" + mAudioSystem + "\n";

        // Edit this to false to allow changing the notification mechanism in settings.
        String restrictedConfig = "net.java.sip.communicator.plugin.generalconfig.notificationconfig.DISABLED=true\n";

        String sipProxyAutoConfig = extractText("(net\\.java\\.sip\\.communicator\\.impl\\.protocol\\.sip\\.acc\\d+\\.PROXY_AUTO_CONFIG=\\w+)", configFromServer);
        String[] sipAutoConfigSubStrings = sipProxyAutoConfig.split("PROXY_AUTO_CONFIG=");

        try
        {
            restrictedConfig = restrictedConfig.concat(sipAutoConfigSubStrings[0] + "IS_PROTOCOL_HIDDEN=true\n");

            if ("false".equalsIgnoreCase(sipAutoConfigSubStrings[1]))
            {
                logger.info("Getting SIP Proxy config from server config");
                // The proxy isn't auto configured so get the SIP
                // proxy config from the server config and save it
                // in the template
                String sipProxyConfig = extractText("(net\\.java\\.sip\\.communicator\\.impl\\.protocol\\.sip\\.acc\\d+\\.PROXY_CONFIG=.*)", configFromServer);
                String[] sipConfigSubStrings = sipProxyConfig.split("PROXY_CONFIG=");
                logger.debug("Proxy address=" + sipConfigSubStrings[1]);

                configTemplate = configTemplate.concat(sipConfigSubStrings[0] + "PROXY_ADDRESS=" + sipConfigSubStrings[1] + "\n");
            }
        }
        catch (java.lang.ArrayIndexOutOfBoundsException e)
        {
            // If the proxy config is invalid or missing, we
            // will hit an ArrayIndexOutOfBoundsException
            // here.
            logger.error("Error retrieving proxy configuration", e);
        }

        String sipAccConfig = extractText("(net\\.java\\.sip\\.communicator\\.impl\\.protocol\\.sip\\.acc\\d+)", configFromServer);

        // Extract the directory number of the sip account from config.
        String sipDn = extractText(sipAccConfig + "DIRECTORY_NUMBER=(.*)", configFromServer);
        if (sipDn.length() == 0)
        {
            logger.debug("No directory number in SIP PS config.");
            sipDn = extractText(sipDn + "=acc(\\d+)", configFromServer);
            if (sipDn.length() > 0)
            {
                logger.debug("Scraped directory number " + logHasher(sipDn) + " successfully.");
                configFromServer = configFromServer.concat(sipAccConfig + ".DIRECTORY_NUMBER=" + sipDn + "\n");
                // Use the DN to create the user configuration if it does not already exist.
                mConfig.createUser(sipDn);
                // The user may have logged in using an email address. Overwrite these
                // values with the DN now that it is known.
                mConfig.global().setProperty(PROPERTY_ACTIVE_USER, sipDn);
                // setActiveUser calls storePassword which securely stores password if there were any store locally.
                // Master password depends on ACTIVE_USER property, so we save it before calling setActiveUser.
                mCredsService.setActiveUser();
                // Store the active user's DN as a salt to protect Personally Identifiable
                // Information.  If we change active user, then it is right that this will
                // be updated.
                Hasher.setSalt(sipDn);
                ProvisioningParams.clear();
                ProvisioningParams.unsubscribe();
                mConfig.global().setProperty(PROPERTY_PROVISIONING_USERNAME, sipDn);
            }
            else
            {
                logger.error("Failed to scrape SIP directory number." +
                        "Cannot create user configuration service");
            }
        }

        // Turn on SIP Presence
        configFromServer = configFromServer.replace("IS_PRESENCE_ENABLED=false", "IS_PRESENCE_ENABLED=true");

        // Turn on BGContacts if need be.  Unfortunately, the PPS
        // doesn't pass us the info required to create the BG
        // contact provider thus we must do so ourselves.

        // The branding might have overridden the BG contacts option, so check what's stored in the config too.
        // At present, this is only used for the refresh-beta branding, to enable BG contacts for beta users without
        // having to modify their SIP-PS profiles
        boolean bgContactsEnabled = configFromServer.contains(
                               PROVISIONING__BG_CONTACTS + "=true") || mConfig.global().getBoolean(PROVISIONING__BG_CONTACTS, false);
        configFromServer += createBgContactInfo(bgContactsEnabled);

        // Enable the CTD protocol providers in config
        configFromServer += createCtdAccountInfo();

        // Escape any backslashes in SIP passwords.  The processed config will
        // later be turned into java properties via Properties.load() in
        // updateConfiguration(), which will remove any backslashes (believing
        // them to be 'line continuations' - see https://stackoverflow.com/que
        // stions/5784895/java-properties-backslash).  We don't expect any line
        // continuations in the config received from the server - however, to
        // avoid the risk of breaking any existing customer configs we don't
        // escape all backslashes in the config.  Instead, we only escape
        // those in SIP passwords which we know can legitimately contain them.
        Pattern pattern = Pattern.compile("net\\.java\\.sip\\.communicator\\.impl\\.protocol\\.sip\\.acc\\d+\\.PASSWORD=\\S+");
        Matcher sipPasswordMatcher = pattern.matcher(configFromServer);
        String password;

        while (sipPasswordMatcher.find())
        {
            password = sipPasswordMatcher.group(0);
            configFromServer = configFromServer.replace(password, password.replace("\\", "\\\\"));
        }

        try
        {
            String sipTransportConfig = extractText("(net\\.java\\.sip\\.communicator\\.impl\\.protocol\\.sip\\.acc\\d+\\.PREFERRED_TRANSPORT=\\w+)", configFromServer);
            String[] sipTransportSubStrings = sipTransportConfig.split("PREFERRED_TRANSPORT=");

            if ("TLS_SECURE".equals(sipTransportSubStrings[1]))
            {
                logger.debug("Setting preferred transport to TLS and enabling default encryption");

                // TLS_SECURE has been selected on provisioning
                // server, so set PREFERRED_TRANSPORT to TLS
                // and DEFAULT_ENCRYPTION to true.  This is
                // added to the end of the config from the
                // server, so that it overrides the default
                // values returned by the server.
                configFromServer = configFromServer.concat(sipTransportSubStrings[0] + "PREFERRED_TRANSPORT=TLS\n");
                configFromServer = configFromServer.concat(sipTransportSubStrings[0] + "DEFAULT_ENCRYPTION=true\n");
            }

            String audioSrtpConfig = extractText("(net\\.java\\.sip\\.communicator\\.ENABLE_AUDIO_SRTP=\\w+)", configFromServer);

            if (Boolean.parseBoolean(audioSrtpConfig.split("ENABLE_AUDIO_SRTP=")[1]))
            {
                logger.debug("Enabling secure media and default encryption as SRTP for audio is enabled");

                // Secure media has been selected on
                // provisioning server, so enable SAVP, SDES
                // and DEFAULT_ENCRYPTION to override the
                // defaults from the server.
                configFromServer = configFromServer.concat(sipTransportSubStrings[0] + "DEFAULT_ENCRYPTION=true\n");
                configFromServer = configFromServer.concat(sipTransportSubStrings[0] + "SAVP_OPTION=1\n");
                configFromServer = configFromServer.concat(sipTransportSubStrings[0] + "SDES_ENABLED=true\n");
                configFromServer = configFromServer.concat(sipTransportSubStrings[0] + "ENCRYPTION_PROTOCOL_STATUS.SDES=true\n");
                configFromServer = configFromServer.concat(sipTransportSubStrings[0] + "ENCRYPTION_PROTOCOL.SDES=0\n");
            }
            else
            {
                // We need to manually set the SDES config
                // options to false, as they are not defaulted
                // by provisioning server.
                configFromServer = configFromServer.concat(sipTransportSubStrings[0] + "ENCRYPTION_PROTOCOL_STATUS.SDES=false\n");
                configFromServer = configFromServer.concat(sipTransportSubStrings[0] + "ENCRYPTION_PROTOCOL.SDES=-1\n");
            }
        }
        catch (java.lang.ArrayIndexOutOfBoundsException e)
        {
            // We would only hit this if the server returned
            // invalid config.
            logger.error("Error retrieving encryption configuration", e);
        }

        // Override the name of the ldap server received from the server.
        String ldapDir = extractText("(net\\.java\\.sip\\.communicator\\.impl\\.ldap\\.directories\\.dir1=.*)", configFromServer);
        if (!StringUtils.isNullOrEmpty(ldapDir))
        {
            logger.info("Got LDAP config from server config");
            String ldapDirectoryName = mResourceService.getI18NString("plugin.provisioning.LDAP_NAME");
            configFromServer = configFromServer.replace(ldapDir, "net.java.sip.communicator.impl.ldap.directories.dir1=" + ldapDirectoryName);
        }

        // Add the config from the server to the config string.
        configFromServer = configTemplate.concat(configFromServer);
        configFromServer = configFromServer.concat(restrictedConfig);

        // processedConfig may already contain an UPDATE_LINK returned from
        // SIP PS. For CDAP clients, we may need to replace it with a different URL.
        CDAPUpgradeUrl cdapUpgradeUrl = new CDAPUpgradeUrl(determineCdapUrl(), configFromServer);

        if (cdapUpgradeUrl.getUrl() != null)
        {
            // Override by appending the newly determined link to processedConfig,
            // even though the existing update link is still present.
            // This works as the config is applied in order, so last update wins.
            configFromServer = configFromServer.concat(PROP_UPDATE_LINK + "=" + cdapUpgradeUrl + "\n");
            logger.info("Overriding UPDATE_LINK to be " + cdapUpgradeUrl);
        }

        return configFromServer;
    }

    /**
     * Convert the contents of the result to a string
     *
     * @param res the result to convert
     * @return The contents of the result
     */
    public static String convertResultToString(HTTPResponseResult res)
                                       throws IllegalStateException, IOException
    {
        InputStream in = res.getContent();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try
        {
            int read = -1;
            byte[] buff = new byte[1024];
            while ((read = in.read(buff)) != -1 )
            {
                out.write(buff, 0, read);
            }
        }
        finally
        {
            // Make sure we close our input and output streams
            if (in != null)
            {
                in.close();
            }
            if (out != null)
            {
                out.close();
            }
        }

        return out == null ? null : out.toString(StandardCharsets.UTF_8);
    }

    /**
     * Get the CDAP URL from global config and maybe update it (occasionally
     * providers have been migrated between CDAP servers).
     * @return the base CDAP URL, e.g. https://[indeterminate link]/
     */
    private String determineCdapUrl()
    {
        String cdapUrl = mConfig.global().getString(CDAP_URL);

        if (!StringUtils.isNullOrEmpty(cdapUrl))
        {
            String sp = mConfig.global().getString(PROPERTY_SERVICE_PROVIDER_NAME);

            logger.debug("CDAP URL: " + cdapUrl +
                        " Service Provider: " + sp);

            final String CDAP_CUST = mConfig.global().getString(
                    "net.java.sip.communicator.plugin.provisioning.CDAP_CUST", "");
            final String CDAP_SERVER_LIVE = mConfig.global().getString(
                    "net.java.sip.communicator.plugin.provisioning.CDAP_SERVER_LIVE", "");

            if (isCDAPCustomer(CDAP_CUST, sp) &&
                !cdapUrl.contains(CDAP_SERVER_LIVE))
            {
                // Use the live CDAP server instead.
                logger.info("Updating CDAP URL" +
                          " from: " + cdapUrl +
                          " to: https://[indeterminate link]/");

                cdapUrl = "https://[indeterminate link]/";
            }

            // In V2.7, some users got shifted from Mac OS X Live to Internal
            // We can fix this by forcing the base URL to be Live if the user
            // shouldn't be on the internal system
            final String CDAP_CUST_MACOS = mConfig.global().getString(
                    "net.java.sip.communicator.plugin.provisioning.CDAP_CUST_MACOS", "");
            final String CDAP_SERVER_INTERNAL = mConfig.global().getString(
                    "net.java.sip.communicator.plugin.provisioning.CDAP_SERVER_INTERNAL", "");

            if (OSUtils.IS_MAC &&
                cdapUrl.contains(CDAP_SERVER_INTERNAL) &&
                isCDAPCustomer(CDAP_CUST_MACOS, sp))
            {
                // Use the live CDAP server instead.
                logger.info("Updating CDAP URL" +
                          " from: " + cdapUrl +
                          " to: https://[indeterminate link]/");

                cdapUrl = "https://[indeterminate link]/";
            }
        }

        return cdapUrl;
    }

    /**
     * Returns true if the current service provider is found in the list of
     * customers in the config file.
     */
    private boolean isCDAPCustomer(String customerList, String sp)
    {
        return Arrays.stream(customerList.split("/"))
                .anyMatch(sp::contains);
    }

    /**
     * Handle updated provisioned IM configuration
     *
     * @param update Is this an update, as opposed to an initial provisioning
     * attempt?
     * @param configFromServer Config retrieved from SIP PS.
     */
    private void updateImConfiguration(boolean update, String configFromServer)
    {
        logger.debug("IM is enabled - " +
                             "checking IM provisioning source");

        // Check what the value of the IM provisioning source
        // currently is in the config
        String oldProvSource =
                    mConfig.user().getString(IM_PROVISION_SOURCE_PROP);

        String newProvSource = null;

        // Search the config that we retrieved from the server
        // for a new value for the IM provisioning source.
        String returnedProvConfig = extractText(
            "(net\\.java\\.sip\\.communicator\\.im\\.IM_PROVISION_SOURCE=\\w+)",
            configFromServer);

        // If we found the IM provisioning source config
        // string in the config from the server, strip out its
        // value.
        if (!StringUtils.isNullOrEmpty(returnedProvConfig))
        {
            logger.debug(
                "Getting value of IM prov source from server config");
            newProvSource =
                returnedProvConfig.split("IM_PROVISION_SOURCE=")[1];
        }

        // If the server has returned a value for the IM
        // provisioning source, check whether this has changed
        // from our previous config.
        if (!StringUtils.isNullOrEmpty(newProvSource))
        {
            logger.debug(
                "Value for IM prov source found in config from server: " +
                 newProvSource );

            // Start by assuming the config hasn't changed.
            boolean imConfigUpdated = false;

            if (!newProvSource.equals(oldProvSource) &&
                !(StringUtils.isNullOrEmpty(oldProvSource) &&
                    newProvSource.equalsIgnoreCase("Manual")))
            {
                logger.debug("IM prov source has changed.");
                imConfigUpdated = true;
            }
            else
            {
                // If the IM prov source hasn't changed, we
                // might still need to update the configured
                // IM account if the IM domain has changed and
                // either we're provisioning via CommPortal or
                // we're provisioning manually with an
                // enforced IM domain.
                String newImDomain = null;

                // Search the config that we retrieved from
                // the server for a new value for the IM
                // domain.
                String returnedImDomain =
                    extractText("(net\\.java\\.sip\\.communicator\\.im\\.IM_DOMAIN=\\S+)",
                                configFromServer);

                // If we found the IM domain config string in
                // the config from the server, strip out its
                // value.
                if (!StringUtils.isNullOrEmpty(returnedImDomain))
                {
                    logger.debug(
                        "Getting value of IM domain from server config");
                    newImDomain =
                        returnedImDomain.split("IM_DOMAIN=")[1];
                }

                // If the server has returned a value for the
                // IM domain, check whether this has changed
                // from our previous config.
                if (!StringUtils.isNullOrEmpty(newImDomain))
                {
                    String oldImDomain = null;

                    if (newProvSource.equals("CommPortal"))
                    {
                        logger.debug("Using CommPortal to provision IM " +
                                     "- checking old IM domain in config");

                        // We're using CommPortal provisioning
                        // so the existing IM account will be
                        // using the IM domain in the current
                        // config, so we can just retrieve it
                        // from there.
                        oldImDomain =
                            mConfig.user().getString(IM_DOMAIN_PROP);

                        // If we don't have any configured IM accounts,
                        // the IM configuration needs updating
                        if (getJabberAccounts().size() != 1)
                        {
                            imConfigUpdated = true;
                        }
                    }
                    else if (newProvSource.equals("Manual"))
                    {
                        logger.debug("Using manual IM provisioning " +
                                     "- checking whether IM domain is enforced");

                        // We're using manual provisioning, so
                        // we need to check whether the IM
                        // domain is enforced so we know
                        // whether we need to check if the
                        // configured domain has changed.
                        boolean newEnforceImDomain = false;

                        // Search the config that we retrieved from the server
                        // for a new value for the IM domain.
                        String returnedEnforceImDomain =
                            extractText("(net\\.java\\.sip\\.communicator\\.im\\.ENFORCE_IM_DOMAIN=\\w+)",
                                        configFromServer);

                        // If we found the enforce IM domain
                        // config string in the config from
                        // the server, strip out its value.
                        if (!StringUtils.isNullOrEmpty(returnedEnforceImDomain))
                        {
                            logger.debug(
                                "Getting value of enforce IM domain from server config");
                            newEnforceImDomain = Boolean.parseBoolean(
                                returnedEnforceImDomain.split("ENFORCE_IM_DOMAIN=")[1]);
                        }

                        if (newEnforceImDomain)
                        {
                            logger.debug("IM domain is enforced - " +
                                         "checking domain of existing Jabber account");

                            // The IM domain is enforced so we
                            // need to check what the domain
                            // of the current jabber account
                            // in the config is.
                            List<String> jabberAccs = getJabberAccounts();

                            if (jabberAccs.size() > 0)
                            {
                                oldImDomain =
                                    mConfig.user().getString(jabberAccs.get(0) + ".SERVER_ADDRESS");
                            }
                        }
                    }

                    if (!StringUtils.isNullOrEmpty(oldImDomain) &&
                        !newImDomain.equals(oldImDomain))
                    {
                        logger.debug("IM domain has changed from " +
                                     oldImDomain + " to " + newImDomain);
                        imConfigUpdated = true;
                    }
                }
            }

            if (imConfigUpdated)
            {
                updatedImAccount(update);
            }
        }
    }

    /**
     * Handle updated provisioned IM configuration
     *
     * @param update Is this an update, as opposed to an initial provisioning
     * attempt?
     */
    private void updatedImAccount(boolean update)
    {
        logger.debug("IM config has been updated");

        // As IM config has changed, make sure the XMPP account prompter plugin is enabled.
        mConfig.user().setProperty(
            "net.java.sip.communicator.plugin.xmppaccountprompter.DISABLED",
            "false");

        // If any IM accounts already exist, we'll need to delete them then display a warning
        // to the user.  First check for any existing accounts.
        List<String> jabberAccs = getJabberAccounts();

        if (jabberAccs.size() > 0)
        {
            logger.info("Existing IM accounts found - deleting them.");
            mConfig.user().removeAccountConfigForProtocol("jabber", true);

            // If this is a scheduled update, the client is already running so we need to
            // prompt the user to restart the client to provision their updated IM account.
            if (update)
            {
                logger.info("Prompting user to restart due to IM config update");
                String title = mResourceService.getI18NString("service.gui.INFORMATION");
                String applicationName = mResourceService.getI18NString("service.gui.APPLICATION_NAME");
                String restartString = mResourceService.getI18NString("service.gui.PLEASE_RESTART",
                    new String[] {applicationName});
                String message = mResourceService.getI18NString("service.gui.CHAT_REPLACED") + " " + restartString;

                new ErrorDialog(title, message).showDialog();
            }
        }
    }

    /**
     * Create the config required to enable or disable BG contacts in a format
     * suitable to be tacked onto the end of the existing config
     *
     * @param enabled true if BG contacts should be enabled
     * @return the config snippet
     */
    private String createBgContactInfo(boolean enabled)
    {
        String snippet = "";
        String userName = getProvisioningNumber();
        String prefix = "net.java.sip.communicator.impl.protocol.commportal.bg.acc" + userName;

        if (enabled)
        {
            logger.debug("BG Contacts enabled");

            snippet = "\n" + prefix + "=acc" + userName +
                      "\n" + prefix + ".ACCOUNT_UID=bg" + userName +
                      "\n" + prefix + ".IS_PROTOCOL_HIDDEN=true" +
                      "\n" + prefix + ".NUMBER=" + userName +
                      "\n" + prefix + ".PROTOCOL_NAME=BG_CommPortal" +
                      "\n" + prefix + ".USER_ID=" + userName + "\n";
        }
        else
        {
            logger.debug("BG Contacts disabled");

            // No need to create config specially to disable BG contacts - code
            // later will ensure that all BG contact info is removed
        }

        return snippet;
    }

    /**
     * Create the config required to create the Click to Dial account
     *
     * @return the config snippet
     */
    private String createCtdAccountInfo()
    {
        String userName = getProvisioningNumber();
        String prefix = "net.java.sip.communicator.impl.protocol.commportal.ctd.acc" + userName;

        String snippet = "\n" + prefix + "=acc" + userName +
                         "\n" + prefix + ".ACCOUNT_UID=ctd" + userName +
                         "\n" + prefix + ".IS_PROTOCOL_HIDDEN=true" +
                         "\n" + prefix + ".NUMBER=" + userName +
                         "\n" + prefix + ".PROTOCOL_NAME=CTD_CommPortal" +
                         "\n" + prefix + ".USER_ID=" + userName + "\n";

        return snippet;
    }

    /**
     * Removes config for all accounts that are provisioned by provisioning
     * server to make sure out of date config does not persist over config
     * updates.
     */
    private void removeProvisionedAccounts()
    {
        if (mConfig.user() != null)
        {
            logger.debug("Removing provisioned accounts");
            List<String> protocolNames = new ArrayList<>();
            protocolNames.add("commportal.bg");
            protocolNames.add("commportal");
            protocolNames.add("sip");

            for (String protocolName : protocolNames)
            {
                mConfig.user().removeAccountConfigForProtocol(protocolName, false);
            }

            // LDAP config is not a standard protocol account, so delete its
            // config separately.
            mConfig.user().removeProperty(
                    "net.java.sip.communicator.impl.ldap.directories.dir");
        }
        else
        {
            logger.debug("User account does not exist yet. No action required");
        }
    }

    /**
     * Check for duplicate IM accounts and removes one if necessary
     */
    private void removeDuplicateIMAccounts()
    {
        List<String> jabberAccounts = getJabberAccounts();
        Set<String> jabberAccountIds =
                new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (String jabberAcc : jabberAccounts)
        {
            // Get the user ID for this account and add to case insensitive set.
            String jabberAccId = mConfig.user().getString(jabberAcc + ".USER_ID");

            if (jabberAccId != null)
            {
                if (jabberAccountIds.contains(jabberAccId))
                {
                    mConfig.user().removeProperty(jabberAcc);
                    logger.error("Found duplicate IM account, removing account " +
                        sanitiseChatAddress(jabberAccId) +
                        " with AccountID " + jabberAcc);
                }

                jabberAccountIds.add(jabberAccId);
            }
        }
    }

    /**
     * Determine any values we can for parameters in the supplied URL/
     *
     * @param url The URL to process
     * @return The processed URL
     */
    private String resolveParametersInUrl(String url) throws UnknownHostException,
                                                 SocketException,
                                                 MalformedURLException
    {
        // Get any system environment identified by ${env.xyz}
        Pattern p = Pattern.compile("\\$\\{env\\.([^\\}]*)\\}");
        Matcher m = p.matcher(url);
        StringBuffer sb = new StringBuffer();
        while(m.find())
        {
            String value = System.getenv(m.group(1));
            if(value != null)
            {
                m.appendReplacement(sb, Matcher.quoteReplacement(value));
            }
        }
        m.appendTail(sb);
        url = sb.toString();

        // Get any system property variable identified by ${system.xyz}
        p = Pattern.compile("\\$\\{system\\.([^\\}]*)\\}");
        m = p.matcher(url);
        sb = new StringBuffer();
        while(m.find())
        {
            String value = System.getProperty(m.group(1));
            if(value != null)
            {
                m.appendReplacement(sb, Matcher.quoteReplacement(value));
            }
        }
        m.appendTail(sb);
        url = sb.toString();

        if(url.contains("${home.location}"))
        {
            url = urlReplace(url, "${home.location}",
                              mConfig.user().getScHomeDirLocation());
        }

        if(url.contains("${home.name}"))
        {
            url = urlReplace(url, "${home.name}", mConfig.user().getScHomeDirName());
        }

        if(url.contains("${uuid}"))
        {
            url = urlReplace(url, "${uuid}", ConfigurationUtils.getUuid());
        }

        if(url.contains("${computerid}"))
        {
            url = urlReplace(url, "${computerid}", ConfigurationUtils.getUuid());
        }

        if(url.contains("${applicationid}"))
        {
            url = urlReplace(url, "${applicationid}", "MS_Desktop_Acc");
        }

        if(url.contains("${osname}"))
        {
            url = urlReplace(url, "${osname}", System.getProperty("os.name"));
        }

        if(url.contains("${device}"))
        {
            String osname = System.getProperty("os.name");
            url = urlReplace(url, "${device}", osname.split("\\s+")[0]);
        }

        if(url.contains("${deviceos}"))
        {
            url = urlReplace(url, "${deviceos}", System.getProperty("os.name") +
                                    " " + System.getProperty("os.version"));
        }

        if(url.contains("${arch}"))
        {
            url = urlReplace(url, "${arch}", System.getProperty("os.arch"));
        }

        if(url.contains("${resx}") || url.contains("${resy}"))
        {
            Rectangle screen = ScreenInformation.getScreenBounds();

            if(url.contains("${resx}"))
            {
                url = urlReplace(url, "${resx}", String.valueOf(screen.width));
            }

            if(url.contains("${resy}"))
            {
                url = urlReplace(url, "${resy}", String.valueOf(screen.height));
            }
        }

        if(url.contains("${build}"))
        {
            url = urlReplace(url, "${build}",
                    System.getProperty("sip-communicator.version"));
        }

        if (url.contains("${applicationversion}"))
        {
            // Setting the 'ApplicationVersion' parameter means that when we poll for a config
            // update, SIP-PS can determine if we no longer meeting the minimum version it has
            // configured for the user, and will reply with a ClientOutOfDate error if so.
            url = urlReplace(url, "${applicationversion}", System.getProperty("sip-communicator.version"));
        }

        if(url.contains("${locale}"))
        {
            String locale =
                ProvisioningActivator.getConfigurationService().global().getString(
                    ResourceManagementService.DEFAULT_LOCALE_CONFIG);
            if(locale == null)
                locale = "";

            logger.info("Setting locale in url to " + locale);
            url = urlReplace(url, "${locale}", locale);
        }

        if (url.contains("${ipaddr}")  ||
            url.contains("${hostname}") ||
            url.contains("${hwaddr}"))
        {
            InetAddress ipaddr =
                ProvisioningActivator.getNetworkAddressManagerService().
                    getLocalHost(InetAddress.getByName(new URL(url).getHost()));
            if(url.contains("${ipaddr}"))
            {
                url = urlReplace(url, "${ipaddr}", ipaddr.getHostAddress());
            }

            if(url.contains("${hostname}"))
            {
                String name;
                if(OSUtils.IS_WINDOWS)
                {
                    // avoid reverse DNS lookup
                    name = System.getenv("COMPUTERNAME");
                }
                else
                {
                    name = ipaddr.getHostName();
                }
                url = urlReplace(url, "${hostname}", name);
            }

            if(url.contains("${hwaddr}"))
            {
                if(ipaddr != null)
                {
                    /* find the hardware address of the interface
                     * that has this IP address
                     */
                    List<NetworkInterface> en =
                            ProvisioningActivator.getNetworkAddressManagerService().
                                    getNetworkInterfaces(false);

                    for (NetworkInterface iface : en)
                    {
                        Enumeration<InetAddress> enInet =
                            iface.getInetAddresses();

                        while(enInet.hasMoreElements())
                        {
                            InetAddress inet = enInet.nextElement();

                            if(inet.equals(ipaddr))
                            {
                                byte[] hw =
                                    ProvisioningActivator.
                                        getNetworkAddressManagerService().
                                            getHardwareAddress(iface);

                                if(hw == null)
                                    continue;

                                StringBuffer buf =
                                    new StringBuffer();

                                for(byte h : hw)
                                {
                                    int hi = h >= 0 ? h : h + 256;
                                    String t = (hi <= 0xf) ? "0" : "";
                                    t += Integer.toHexString(hi);
                                    buf.append(t);
                                    buf.append(":");
                                }

                                buf.deleteCharAt(buf.length() - 1);

                                url = urlReplace(url, "${hwaddr}",
                                        buf.toString());

                                break;
                            }
                        }
                    }
                }
            }
        }
        return url;
    }

    /**
     * Replace a string in the URL provided with a url encoded version of the
     * replacement.
     *
     * @param url The URL in which the replacement should happen
     * @param oldString The string to replace
     * @param newString The string to replace it with
     * @return The updated URL
     */
    private String urlReplace(String url, String oldString, String newString)
    {
        String safeString = newString;
        try
        {
            safeString = URLEncoder.encode(newString, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            // Nothing we can do - just log and use the original
            logger.error("Unsupported encoding?", e);
        }

        return url.replace(oldString, safeString);
    }

    /**
     * Display an error dialog to the user after failing to retrieve config.
     *
     * @param errorMessage  The error message to include in the dialog.
     */
    private void displayLoginError(String errorMessage, boolean forceExit)
    {
        logger.debug("Displaying error to user: " + errorMessage);

        OnDismiss dismissAction =
            forceExit ? OnDismiss.FORCE_EXIT : OnDismiss.DO_NOTHING;

        ErrorDialog dialog = new ErrorDialog(mResourceService.getI18NString("service.gui.ERROR"),
            errorMessage, dismissAction);
        dialog.setModal(true);
        dialog.showDialog();
    }

    /**
     * Forget the credentials that the user has entered so they are prompted
     * to re-enter them after a failure. Also clears any credentials we may
     * have received on a login URI.
     *
     * @param resetCdap If true then also reset the CDAP provider selection.
     */
    private void forgetCredentials(boolean resetCdap)
    {
        logger.info("Removing username from config");
        ConfigurationUtils.clearCredentialsAndLogout(false);

        // If we're forgetting the credentials then we should forget the ones
        // we may have just received on a login URI, if we didn't clear them
        // and they were incorrect then we would keep trying to use them and
        // failing which could result in the user getting locked out of their
        // account which is bad.
        ProvisioningParams.clear();
        PreLoginUtils.clearSSOData();
        PreLoginUtils.clearManualLoginData();

        if (resetCdap)
        {
            logger.info("Resetting CDAP SP ID in stored config");
            mConfig.global().setProperty(PROPERTY_CDAP_SP_ID, -1);
        }
    }

    static String extractText(String xiRegex, String xiConfigFromServer)
    {
        Pattern pattern = Pattern.compile(xiRegex);

        Matcher m = pattern.matcher(xiConfigFromServer);

        if (m.find())
        {
            return (m.group(1));
        }

        return "";
    }

    /**
     * Update configuration with properties retrieved from provisioning URL.
     *
     * @param config provisioning file
     */
    private void updateConfiguration(String config)
    {
        Properties fileProps = new OrderedProperties();

        try
        {
            StringReader reader = new StringReader(config);
            fileProps.load(reader);
            reader.close();

            // Add a value indicating that we have new config.  Other services
            // can register for notifications if this value changes (i.e. when
            // we get new config from the provisioning URL).
            fileProps.put(LAST_PROVISIONING_UPDATE_TIME,
                                                    System.currentTimeMillis());

            Iterator<Map.Entry<Object, Object> > it =
                fileProps.entrySet().iterator();

            // We want to disable any codecs that aren't explicitly enabled
            // by the provisioning server, so delete all codec config so that
            // we only load those in the server config.
            purgeExistingCodecs();

            // Remove all accounts that are provisioned by provisioning server
            // so that we don't end up with old or invalid config hanging
            // around if any account IDs have changed.
            removeProvisionedAccounts();

            // In rare cases the user can end up with two IM accounts that are
            // the same except for some capitalization. Check for this to
            // protect against this case.
            removeDuplicateIMAccounts();

            // Remove any Call Park Orbit details, as otherwise we won't be able
            // to tell when old orbits are deleted.
            purgeExistingParkOrbits();

            // Remove any contact source config, otherwise the user might not
            // be able to override this config if the default contact source is
            // removed in SIP PS.
            removeExistingContactSourceConfig();

            try
            {
                /* save and reload the "new" configuration */
                mConfig.user().setProperties(processProperties(it));
                mConfig.user().storeConfiguration();
                mConfig.user().reloadConfiguration();
            }
            catch(Exception e)
            {
                logger.error("Cannot reload configuration", e);
            }
        }
        catch(IOException e)
        {
            logger.warn("Error during load of provisioning file");
        }
    }

    /**
     * Check if a property name belongs to the allowed prefixes.
     *
     * @param key property key name
     * @return true if key is allowed, false otherwise
     */
    private boolean isPrefixAllowed(String key)
    {
        if(allowedPrefixes.size() > 0)
        {
            for(String s : allowedPrefixes)
            {
                if(key.startsWith(s))
                {
                    return true;
                }
            }

            /* current property prefix is not allowed */
            return false;
        }
        else
        {
            /* no allowed prefixes configured so key is valid by default */
            return true;
        }
    }

    /**
     * Process new properties received from the provisioning server.
     *
     * - If a property's value equals "${null}", remove the property from
     *   the configuration service.
     * - IF the key equals "net.java.sip.communicator.impl.gui.main.VMAddress",
     *   remove the (unused) property from the configuration service as it
     *   leaks security info (the PAT).
     * - If the key name ends with "PASSWORD", its value is encrypted
     *   through credentials storage service.
     * - Otherwise the property is added to the configProperties map, ready
     *   to be updated in the configuration service.
     *
     * @param properties an iterator containing the properties to be processed.
     *
     * @return a map of key strings to their values ready to be saved in the
     *         configuration service.
     */
    private Map<String, Object> processProperties(
                                Iterator<Map.Entry<Object, Object> > properties)
    {
        Map<String, Object> configProperties = new HashMap<>();

        while(properties.hasNext())
        {
            Map.Entry<Object, Object> entry = properties.next();
            String key = (String)entry.getKey();
            Object value = entry.getValue();

            // skip empty keys, prevent them going into the configuration
            if(key.trim().length() == 0)
                continue;

            if(key.equals(PROVISIONING_ALLOW_PREFIX_PROP))
            {
                String[] prefixes = ((String)value).split("\\|");

                /* updates allowed prefixes list */
                for(String s : prefixes)
                {
                    logger.info("Adding new value to allowed prefixes list: " + s);
                    allowedPrefixes.add(s);
                }
                continue;
            }
            else if(key.equals(PROVISIONING_ENFORCE_PREFIX_PROP))
            {
                logger.info("Found enforce prefix property");
                checkEnforcePrefix((String)value);
                continue;
            }

            /* check that properties is allowed */
            if(!isPrefixAllowed(key))
            {
                logger.info("Found disallowed prefix: " + key);
                continue;
            }

            // Remove properties with a null value.
            // Also a special case to remove value that is a security issue as the
            // value written to sip-communicator.properties contains the PAT which
            // can easily be used to login to CPWeb.
            // See https://[indeterminate link]
            if (value instanceof String &&
                (value.equals("${null}") ||
                 key.equals("net.java.sip.communicator.impl.gui.main.VMAddress")))
            {
                logger.info("Removing property: " + key);
                mConfig.user().removeProperty(key);
            }
            else if(key.endsWith(".PASSWORD"))
            {
                /* password => credentials storage service */
                ProvisioningActivator.getCredentialsStorageService().user().storePassword(
                        key.substring(0, key.lastIndexOf(".")),
                        (String)value);

                logger.info("Saving password for property: " +
                            sanitiseFirstPatternMatch(key, PRIVACY_PATTERNS, str -> "_" + logHasher(str)));
            }
            else if (key.equals(PROP_UPDATE_LINK))
            {
                // This is the upgrade URL.  Before filling in the parameters with
                // resolveParametersInUrl(), remove the 'ApplicationVersion' parameter if it's
                // present, as the version check is done client-side for upgrades so that the server
                // will always return upgrade information (even if we are out of date). We do not
                // want ClientOutOfDate errors returned when polling for new software.
                String  processedUrl = ((String) value).replaceAll(
                    "(&ApplicationVersion=\\$\\{applicationversion\\})|" +
                        "(ApplicationVersion=\\$\\{applicationversion\\}&)", "");

                try
                {
                    processedUrl = resolveParametersInUrl(processedUrl);
                }
                catch (IOException e)
                {
                    logger.error("Exception processing URL " + value, e);
                    processedUrl = (String)value;
                }

                mConfig.user().setProperty(key, processedUrl);

                logger.info("Updating update URL for key: " + key);
            }
            else
            {
                configProperties.put(migrateKey(key), migrateValue(key,value));

                // Remove account (subscriber) number from the logs. Returns {key, value}.
                String loggableKey = sanitiseDirectoryNumberWithAccPrefix(key);
                String loggableValue = "";
                if (SANITISE_CONFIG_PROPERTY_PATTERN_LIST
                        .stream()
                        .map(pattern -> pattern.matcher(key))
                        .anyMatch(Matcher::find))
                {
                    loggableValue = logHasher(value.toString());
                }
                else
                {
                    loggableValue = sanitiseDirectoryNumberWithAccPrefix(value.toString());
                }

                logger.info("Saving to config: " + loggableKey +
                            "=" + loggableValue);
            }
        }

        // Fix for a bug on old provisioning servers where the wrong config
        // property name is provided for the H264 codec.
        ensureH264CodecCorrectlySet(configProperties);

        // If we have no H264 config from the server, disable or hide all
        // video-related UI elements.
        setEnableVideoUIFlag(configProperties);

        return configProperties;
    }

    /**
     * Perform any required config migrations of the keys that have come back
     * from the provisioning server.
     *
     * This migration happens before the config makes it into the config service.
     *
     * @param key The key.
     * @return The migrated key.
     */
    private String migrateKey(String key)
    {
        // Pre-release versions of SIP PS misses the frequency of the g722 codec.
        // This method corrects this.
        if (key.startsWith(ENCODING_CONFIG_PROP_WIRED_WIRELESS_SHARED) &&
            key.endsWith("G722"))
        {
            return key + "/8000";
        }

        return key;
    }

    /**
     * Perform any required config migrations of the values that have come back
     * from the provisioning server.
     *
     * This migration happens before the config makes it into the config service.
     *
     * @param key The key.
     * @param value The current value.
     * @return The migrated value.
     */
    private Object migrateValue(String key, Object value)
    {
        // Reverse the priorities of codecs that have come back from SIP PS,
        // unless the value is zero, which is a special value meaning not set.
        try
        {
            if (key.startsWith(ENCODING_CONFIG_PROP_WIRED_WIRELESS_SHARED) &&
                !(value.toString().equals("0")))
            {
                // Subtract each codec priority from Integer.MAX_VALUE to reverse the order.
                return Integer.MAX_VALUE - Integer.parseInt((String) value);
            }
        }
        catch (NumberFormatException e)
        {
            logger.warn("Expected int for config with key: " + key +
                        " found: " + value, e);
        }

        return value;
    }

    /**
     * Walk through all properties and make sure all properties keys match
     * a specific set of prefixes defined in configuration.
     *
     * @param enforcePrefix list of enforce prefix.
     */
    private void checkEnforcePrefix(String enforcePrefix)
    {
        if(enforcePrefix == null)
        {
            return;
        }

        /* must escape the | character */
        String[] prefixes = enforcePrefix.split("\\|");

        /* get all properties */
        for (String key : mConfig.user().getAllPropertyNames())
        {
            boolean isValid = false;

            for(String k : prefixes)
            {
                if(key.startsWith(k))
                {
                    isValid = true;
                    break;
                }
            }

            /* property name does is not in the enforce prefix list
             * so remove it
             */
            if(!isValid)
            {
                mConfig.user().removeProperty(key);
            }
        }
    }

    /**
     * If the config retrieved from the provisioning server does not contain
     * information about a codec, that codec should be disabled.
     * Therefore we clean out existing codec config before applying the config
     * from the provisioning server.
     */
    private void purgeExistingCodecs()
    {
        logger.entry();

        // Get both wired and wireless config; can't use
        // getPropertyNamesByPrefix as it only works at '.'s -
        // "ENCODING.W" wouldn't match anything.
        List<String> codecs =
            mConfig.user().getPropertyNamesByPrefix(ENCODING_CONFIG_PROP_WIRED, false);
        codecs.addAll(mConfig.user().getPropertyNamesByPrefix(
                                         ENCODING_CONFIG_PROP_WIRELESS, false));
        for (String codec : codecs)
        {
            mConfig.user().removeProperty(codec);
        }

        logger.exit();
    }

    private void purgeExistingParkOrbits()
    {
        logger.entry();
        List<String> callParkOrbitProperties = mConfig.user().getPropertyNamesByPrefix(
            OperationSetCallPark.CALLPARK_ORBIT_CONFIG_PREFIX, false);

        for (String property : callParkOrbitProperties)
        {
            mConfig.user().removeProperty(property);
        }

        logger.exit();
    }

    /**
     * If a default contact source is not set in SIP PS, the config returned by
     * the server will not contain a default contact source 'user_override'
     * setting.  Therefore, we delete any existing default contact source
     * config before applying the new config to make sure out of date
     * 'user_override' settings are removed.
     */
    private void removeExistingContactSourceConfig()
    {
        logger.entry();
        mConfig.user().removeProperty(DEFAULT_CONTACT_SOURCE_PROP_PREFIX + "MAC");
        mConfig.user().removeProperty(DEFAULT_CONTACT_SOURCE_PROP_PREFIX + "MAC_USER_OVERRIDE");
        mConfig.user().removeProperty(DEFAULT_CONTACT_SOURCE_PROP_PREFIX + "WIN");
        mConfig.user().removeProperty(DEFAULT_CONTACT_SOURCE_PROP_PREFIX + "WIN_USER_OVERRIDE");
        logger.exit();
    }

    /*
     * Old provisioning server customisations use the wrong name for the H264
     * codec property - they set "H264_unified" rather than "H264/90000".
     * This method changes the name of the codec property if it is wrongly set.
     * @param properties
     */
    private void ensureH264CodecCorrectlySet(Map<String, Object> properties)
    {
        String h264Unified = ".h264_unified";
        String h264_90000 = ".H264/90000";

        String[] prefixes =
        {
            ENCODING_CONFIG_PROP_WIRELESS,
            ENCODING_CONFIG_PROP_WIRED
        };

        for (String prefix : prefixes)
        {
            if (!properties.containsKey(prefix + h264_90000) &&
                properties.containsKey(prefix + h264Unified))
            {
                logger.info("Provisioning server gave H264_unified " +
                            "config line; changed to H264/90000 locally.");
                properties.put(prefix + h264_90000,
                               properties.get(prefix + h264Unified));
                properties.remove(prefix + h264Unified);
            }
        }
    }

    /**
     * Check to see if we have any H.264 codecs set (either for wireless or
     * wired mode) in the config from the provisioning server.
     * If not, we disable all video options in the UI by setting a config flag.
     * @param properties The configuration received from the prov server.
     */
    private void setEnableVideoUIFlag(Map<String, Object> properties)
    {
        String wiredH264Prop = ENCODING_CONFIG_PROP_WIRED + ".H264/90000";
        String wirelessH264Prop = ENCODING_CONFIG_PROP_WIRELESS + ".H264/90000";

        boolean hasWiredCodec = (properties.containsKey(wiredH264Prop) &&
                                 !properties.get(wiredH264Prop).equals("0"));
        boolean hasWirelessCodec = (properties.containsKey(wirelessH264Prop) &&
                                 !properties.get(wirelessH264Prop).equals("0"));

        boolean isUIDisabled = !(hasWiredCodec || hasWirelessCodec);
        properties.put(DISABLE_VIDEO_UI, isUIDisabled);
        logger.info("Setting DISABLE_VIDEO_UI flag to " + isUIDisabled);
    }

    /**
     * Make an HTTP request to the server for config.
     *
     * @param url         The config server's URL
     * @param httpMethod GET or POST.
     * @param urlParams A ProvisioningUrlParams object encapsulating the names and values of the URL params to use.
     *
     * @return The HTTPResponseResult from the server
     */
    private HTTPResponseResult makeHttpRequestForConfig(String url,
                                                        HttpMethod httpMethod,
                                                        HttpUrlParams urlParams)
        throws IOException
    {
        HTTPResponseResult res = null;

        try
        {
            logger.info("Attempting to log in to " + url + " with a " + httpMethod);
            res = HttpUtils.sendHttpRequest(url,
                                            httpMethod,
                                            PROPERTY_PROVISIONING_USERNAME,
                                            PROPERTY_PROVISIONING_PASSWORD,
                                            urlParams,
                                            savedCredentials);
        }
        catch(IOException e)
        {
            // Pass network errors onwards so that the caller can put up a
            // sensible message instead of a generic 'it failed' message.
            logger.error("IOException " + e);
            throw e;
        }
        catch(Throwable t)
        {
            logger.error("Error posting form", t);
        }

        return res;
    }

    /**
     * Handle a failure to retrieve config from the provisioning server.
     * Should be private but needed for unit tests.
     */
    @VisibleForTesting
    void handleConfigError()
    {
        logger.warn("Error retrieving data");

        if (!mStoredConfig || mTerminalError)
        {
            // Provisioning failed and there is either no stored config or
            // we have hit a terminal error (e.g. their account is locked
            // out), therefore we need to exit.
            logger.error("There is no stored config or the client is not " +
                                       "permitted to start from stored config");
            ServiceUtils.shutdownAll(ProvisioningActivator.bundleContext);
        }
        else
        {
            logger.warn("Continuing to start the client with stored config");
            boolean qaMode = ConfigurationUtils.isQaMode();
            if (qaMode)
            {
                logger.info("In QA mode so opening error report frame " +
                                    "to inform the user of stored config use.");
                ServiceUtils.getService(ProvisioningActivator.bundleContext,
                                        DiagnosticsService.class,
                                        new ServiceUtils.ServiceCallback<DiagnosticsService>()
                                        {
                                            @Override
                                            public void onServiceRegistered(
                                                    DiagnosticsService diagsService)
                                            {
                                                logger.info(
                                                        "Got reference to the diagnostics service");
                                                diagsService.openErrorReportFrame(
                                                        ReportReason.UNACKNOWLEDGED_USE_OF_STORED_CONFIG);
                                            }
                                        });
            }
        }
    }

    /**
     *  Set the terminal error member variable
     */
    @VisibleForTesting
    void setTerminalError(boolean terminalError)
    {
        mTerminalError = terminalError;
    }

    /**
     * Returns true if we already have a scheduler.
     */
    @VisibleForTesting
    boolean hasActiveScheduler()
    {
        synchronized (sConfigSyncRunnableLock)
        {
            return (mConfigSyncRunnable != null) && !mConfigSyncRunnable.isCancelled();
        }
    }

    /**
     * Stop the provisioning service
     */
    void stop()
    {
        synchronized (sConfigSyncRunnableLock)
        {
            if (mConfigSyncRunnable != null)
            {
                mConfigSyncRunnable.cancel();
            }
        }

        mConfig.global()
                .removePropertyChangeListener(
                        PROPERTY_PROVISIONING_USERNAME,
                        mUsernameChangeListener);
    }

    /**
     * Upgrade URL for CDAP client, to use instead of the URL returned by
     * the provisioning server.
     *
     * The default for CDAP clients is to determine the update location from the CDAP URL.
     * However, there are 2 ways this can be overridden:
     *     - The client branding may contain Dev specific values in
     *       net.java.sip.communicator.UPDATE_LINK
     *     - The CDAP branding may contain upgrade-override-location
     * If both are present, the Dev client branding takes precedence.
     */
    @VisibleForTesting
    static class CDAPUpgradeUrl
    {
        /**
         * True if this is a CDAP client which gets its updates using the CDAP mechanism
         * <p>
         * Set to false for reselling service providers that use CDAP to separate the customer enterprises (when CDAP is
         * used only for branding, not for the update location - the tailored branding mechanism is used for updates).
         * <p>
         * Not relevant if we are not using CDAP at all (i.e. tailored branding client)
         */
        private static final String UPDATE_FROM_CDAP
                = "net.java.sip.communicator.plugin.cdap.UPDATE_FROM";

        // Duplicated from CDAPService - cannot directly access that definition
        // due to the jitsi vs plugin split.
        private static final String PROPERTY_UPGRADE_OVERRIDE_LOCATION_URL =
                "net.java.sip.communicator.plugin.upgrade.OVERRIDE_LOCATION";

        private static final String PROPERTY_USE_UPGRADE_LOCATION_OVERRIDE =
                "net.java.sip.communicator.plugin.provisioning.OVERRIDE_CDAP_UPGRADE_URL";

        // Update links point at version files, which contain the latest client version
        // and its location. They have different formats and extensions on Windows and Mac.
        private static final String WIN_VERSION_FILE_NAME = "ucdVersion.txt";
        private static final String MAC_VERSION_FILE_NAME = "ucdVersion.xml";

        // Update links for dev clients (specified in the branding) will contain one of these two domains
        private static final String DEV_DESKTOP_ANALYTICS_DOMAIN = "[indeterminate link]";
        private static final String DEV_ARTIFACTORY_DOMAIN = "[indeterminate link]";

        // URL to check for updates
        private final String upgradeUrl;

        // URL of the CDAP server this client gets config from.
        // This is set to a valid URL for all CDAP clients
        private final String cdapConfigUrl;

        // Partially processed config from SIP PS, which has not yet been
        // fully processed and written to the configuration service.
        // Properties are separated by newline characters.
        private final String sippsConfig;

        // URL from the CDAP server which specifies an alternative location of updates
        // If set, the client can check for updates here if configured to,
        // instead of checking on the main CDAP server
        // Normally points to a different CDAP server
        private final String overrideUrl;

        private final ConfigurationService mConfig = ProvisioningActivator.getConfigurationService();

        CDAPUpgradeUrl(String cdapConfigUrl, String sippsConfig)
        {
            this.cdapConfigUrl = cdapConfigUrl;
            this.sippsConfig = sippsConfig;
            this.overrideUrl = mConfig.global().getString(PROPERTY_UPGRADE_OVERRIDE_LOCATION_URL);
            this.upgradeUrl = determineCDAPUpgradeUrl();
       }

        /**
         * @return Upgrade URL if this client upgrades via CDAP, otherwise null
         */
        public String getUrl()
        {
            return upgradeUrl;
        }

        @Override
        public String toString()
        {
            return getUrl();
        }

        private String determineCDAPUpgradeUrl()
        {
            boolean usingCdap = !StringUtils.isNullOrEmpty(cdapConfigUrl);
            logger.debug("CDAP client? : " + usingCdap);

            if (!usingCdap)
            {
                logger.debug("NOT using CDAP at all.");
                return null;
            }

            boolean updateFromCdap = mConfig.global().getBoolean(UPDATE_FROM_CDAP, true);
            logger.debug("Update via CDAP? : " + updateFromCdap);

            if (!updateFromCdap)
            {
                logger.debug("Using CDAP, but updating using tailored mechanism (reseller).");
                return null;
            }

            // This is the current string in config - not the one provided
            // by the server (they can be different as it may have an immutable
            // default from branding, or be an old stored value).
            String currentUpdateLink = mConfig.user().getString(ProvisioningServiceImpl.PROP_UPDATE_LINK);
            logger.debug("Current update link: " + currentUpdateLink);

            final String CDAP_SERVER_BETA = mConfig.global().getString(
                    "net.java.sip.communicator.plugin.provisioning.CDAP_SERVER_BETA", "");
            final String CDAP_SERVER_INTERNAL = mConfig.global().getString(
                    "net.java.sip.communicator.plugin.provisioning.CDAP_SERVER_INTERNAL", "");
            // Handle that Metaswitch users often switch between Dev, alpha, beta and
            // internal CDAP clients by pasting in their config from the old client.
            // This can mean the stored UPDATE_LINK is wrong, so get it from scratch.
            if (currentUpdateLink != null && (
                    currentUpdateLink.contains(DEV_DESKTOP_ANALYTICS_DOMAIN) ||
                    currentUpdateLink.contains(DEV_ARTIFACTORY_DOMAIN) ||
                    currentUpdateLink.contains(CDAP_SERVER_INTERNAL) ||
                    currentUpdateLink.contains(CDAP_SERVER_BETA)))
            {
                // This removes the field from the config, so that...
                mConfig.user().removeProperty(ProvisioningServiceImpl.PROP_UPDATE_LINK);
                // ...when we try to get the value it will get it from the branded defaults.
                currentUpdateLink = mConfig.user().getString(ProvisioningServiceImpl.PROP_UPDATE_LINK);
                logger.debug("Dev, alpha, beta or internal build. " +
                             "Removed stored update link and reloaded from branding, new value: " +
                             currentUpdateLink);
            }

            String updateLink;

            // Dev and alpha builds are hosted on desktopanalytics or artifactory,
            // and will always just use the branding defaults.
            // Otherwise, only tailored brandings would have non-null currentUpdateLink,
            // but this code only runs for CDAP brandings only.
            if ((currentUpdateLink != null) &&
                (currentUpdateLink.contains(DEV_DESKTOP_ANALYTICS_DOMAIN) ||
                 currentUpdateLink.contains(DEV_ARTIFACTORY_DOMAIN)))
            {
                logger.debug("Dev or alpha client, use the branded value");
                updateLink = currentUpdateLink;
            }
            else
            {
                // For all other CDAP clients, an update link is not in the branding,
                // so is determined using the CDAP server.
                logger.debug("Live build (which includes beta and internal)");

                updateLink = determineProductionCDAPUpdateUrl();
            }

            // The update link is now for the Windows "ucdVersion.txt" file.
            // If we're actually running on a Mac, change this to
            // point to the sparkle update file ("ucdVersion.xml") instead.
            if (OSUtils.IS_MAC)
            {
                updateLink = updateLink.replace(WIN_VERSION_FILE_NAME, MAC_VERSION_FILE_NAME);
            }

            logger.debug("Returning update link as " + updateLink);
            return updateLink;
        }

        /** Returns a Windows update URL (ending ucdVersion.txt) for consistency with
         * any branded update links - must convert to correct format for OS before use. */
        private String determineProductionCDAPUpdateUrl()
        {
            String updateLink;

            // A specific upgrade location override may be provided by the CDAP branding
            if (shouldUseOverrideUrl())
            {
                logger.debug("Use override update link");
                updateLink = overrideUrl;
            }
            else
            {
                logger.debug("Use CDAP based update link");
                updateLink = cdapConfigUrl;
            }

            // Now set the rest of the URL, allowing that they may or may not have a trailing /
            if (!updateLink.endsWith("/"))
            {
                updateLink = updateLink + "/";
            }

            return updateLink + WIN_VERSION_FILE_NAME;
        }

        private boolean shouldUseOverrideUrl()
        {
            if (StringUtils.isNullOrEmpty(overrideUrl))
            {
                logger.debug("No override URL specified in CDAP config");
                return false;
            }

            // Regex used to check the SIP PS config for the relevant property and extract its value
            // Must be in XSI format to use as an argument to extractText
            String useOverridePropertyRegexXSI =
                    StringEscapeUtils.escapeXSI(PROPERTY_USE_UPGRADE_LOCATION_OVERRIDE) + "=(\\w+)";

            boolean useOverrideSIPPS = Boolean.parseBoolean(extractText(useOverridePropertyRegexXSI, sippsConfig));
            boolean useOverrideCDAP = mConfig.global().getBoolean(PROPERTY_USE_UPGRADE_LOCATION_OVERRIDE, false);

            boolean useOverride = useOverrideCDAP || useOverrideSIPPS;
            logger.debug("Using CDAP upgrade URL override? " + useOverride +
                         " based on CDAP config: " + useOverrideCDAP +
                         " and SIP PS config: " + useOverrideSIPPS);

            return useOverride;
        }
    }

    /**
     * Sanitise server config strings that contain Personal Data.
     *
     * @param configFromServer The server config.
     * @return The sanitised output.
     */
    private static String sanitiseServerConfigForLogging(String configFromServer)
    {
        final List<String> result = new ArrayList<>();
        for (String line : configFromServer.split("\\R"))
        {
            String redactedAttrValues = sanitiseFirstPatternMatch(line,
                                                 SANITISE_CONFIG_PATTERN_LIST,
                                                 Hasher::logHasher);

            String redactedDN = sanitiseDirectoryNumberWithAccPrefix(redactedAttrValues);
            result.add(redactedDN);
        }

        return String.join(System.lineSeparator(), result);
    }
}
