/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import org.osgi.framework.ServiceReference;

import net.java.sip.communicator.plugin.provisioning.ProvisioningServiceImpl;
import net.java.sip.communicator.service.commportal.ClassOfServiceService;
import net.java.sip.communicator.service.commportal.CommPortalService;
import net.java.sip.communicator.service.conference.ConferenceService;
import net.java.sip.communicator.service.headsetmanager.HeadsetManagerService.HeadsetResponseState;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.OperationSet;
import net.java.sip.communicator.service.protocol.OperationSetMultiUserChat;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.resources.ResourceManagementServiceUtils;
import net.java.sip.communicator.util.account.AccountUtils;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.neomedia.MediaType;
import org.jitsi.service.neomedia.codec.EncodingConfiguration;
import org.jitsi.service.resources.ResourceManagementService;
import org.jitsi.util.OSUtils;
import org.jitsi.util.StringUtils;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

/**
 * Cares about all common configurations. Storing and retrieving configuration
 * values.
 *
 * @author Yana Stamcheva
 * @author Damian Minkov
 */
public class ConfigurationUtils
{
    private static final Logger logger = Logger.getLogger(ConfigurationUtils.class);
    private static final ResourceManagementService resources = UtilActivator.getResources();
    public static final String ENTER_COMMAND = resources.getI18NString("service.gui.ENTER_KEY");
    public static final String CTRL_ENTER_COMMAND =
        resources.getI18NString("service.gui.ENTER_KEY") + " + " + resources.getI18NString("service.gui.CTRL_KEY");

    /**
     * Indicates whether the video call button should be added to the chat window.
     */
    private static boolean isVideoCallButtonInChatEnabled;

    /**
     * Indicates whether the history navigation buttons should be added to the chat window.
     */
    private static boolean isNavigationButtonsEnabled;

    /**
     * Indicates if the chat window should show contact photos
     */
    private static boolean isChatShowContact;

    /**
     * Indicates whether the video call button is disabled (from the contact right-click menu).
     */
    private static boolean isVideoCallButtonDisabled;

    private static int windowTransparency;
    private static boolean isTransparentWindowEnabled;
    private static boolean isWindowDecorated;
    private static boolean isShowSmileys;
    private static boolean isGroupRemoveDisabled;
    private static boolean isPresetStatusMessagesEnabled;

    private static final ConfigurationService configService = UtilActivator.getConfigurationService();

    /**
     * The default location to which recorded calls should be saved.
     */
    public static final String DEFAULT_SAVED_CALLS_PATH =
        System.getProperty("user.home") + File.separator + resources.getSettingsString("service.gui.APPLICATION_NAME");
    private static ProtocolProviderService lastCallConferenceProvider = null;

    /**
     * Hide accounts from accounts status list.
     */
    private static boolean hideAccountStatusSelectors = false;

    /**
     * Whether to disable creation of auto answer submenu.
     */
    private static boolean autoAnswerDisableSubmenu = false;
    private static boolean isSingleWindowInterfaceEnabled = false;

    /**
     * Property to indicate if in call security indication is hidden.
     */
    private static final String SECURITY_STATUS_HIDDEN_PROP = "net.java.sip.communicator.impl.gui.main.call.SECURITY_STATUS_HIDDEN";

    /**
     * Property to set whether login using an email is enabled. This is true by default
     */
    private static final String PNAME_EMAIL_LOGIN_ENABLED = "net.java.sip.communicator.plugin.login.EMAIL_LOGIN_ENABLED";
    private static final String IM_ENABLED_PROP = "net.java.sip.communicator.im.IM_ENABLED";
    private static final String SMS_ENABLED_PROP = "net.java.sip.communicator.im.SMS_ENABLED";
    private static final String IM_CORRECTION_ENABLED_PROP = "net.java.sip.communicator.im.IM_CORRECTION_ENABLED";
    private static final String OPEN_WINDOW_ON_NEW_CHAT_PROP = "net.java.sip.communicator.im.OPEN_WINDOW_ON_NEW_CHAT";
    private static final String IM_PROVISION_SOURCE_PROP = "net.java.sip.communicator.im.IM_PROVISION_SOURCE";
    private static final String IM_AUTOPOPULATE_PROP = "net.java.sip.communicator.impl.protocol.jabber.BRAND_IM_AUTOPOPULATE_FROM_BG_CONTACTS";
    private static final String SHOW_SMILEYS_PROPERTY = "net.java.sip.communicator.service.replacement.SMILEY.enable";
    private static final String CHAT_SHOW_CONTACT_PHOTO = "net.java.sip.communicator.service.gui.CHAT_SHOW_CONTACT_PHOTO";
    private static final String SINGLE_WINDOW_INTERFACE_ENABLED = "net.java.sip.communicator.service.gui.SINGLE_WINDOW_INTERFACE_ENABLED";
    private static final String MAIN_UI_USES_INLINE_DIAL_PAD = "net.java.sip.communicator.service.gui.USE_INLINE_DIAL_PAD";
    private static final String CONTACT_FAVORITES_ENABLED_PROP = "net.java.sip.communicator.service.gui.CONTACT_FAVORITES_ENABLED";
    public static final String SELECTED_TAB = "net.java.sip.communicator.service.gui.SELECTED_TAB";
    public static final String SELECTED_HISTORY_TAB = "net.java.sip.communicator.service.gui.SELECTED_HISTORY_TAB";
    private static final String SMS_NUMBER_MESSAGABLE_REGEX = "net.java.sip.communicator.impl.protocol.sip.SMS_NUMBER_MESSAGABLE_REGEX";
    private static final String PHONE_NUMBER_CALLABLE_REGEX_PROP = "net.java.sip.communicator.impl.protocol.sip.PHONE_NUMBER_CALLABLE_REGEX";
    private static final String PHONE_NUMBER_DIALOUT_REGEX_PROP = "plugin.conference.accessionmeeting.PHONE_NUMBER_DIALOUT_REGEX";
    private static final String PHONE_NUMBER_IGNORE_REGEX_PROP = "net.java.sip.communicator.impl.protocol.sip.PHONE_NUMBER_IGNORE_REGEX";
    private static final String ALT_INCOMING_CALL_POPUP = "net.java.sip.communicator.impl.gui.main.call.ALT_INCOMING_CALL_POPUP";
    private static final String ALLOW_GROUP_CONTACT_PROPERTY = "net.java.sip.communicator.impl.protocol.groupcontacts.SUPPORT_GROUP_CONTACTS";
    private static boolean isNormalizePhoneNumber;
    private static boolean acceptPhoneNumberWithAlphaChars;
    public static final String CALL_ON_TOP_PROP = "net.java.sip.communicator.impl.gui.main.call.CALL_ALWAYS_ON_TOP";

    /**
     * The name of the ignore regex for removing odd formatting from login.
     */
    public static final String PROVISIONING_IGNORE_REGEX = "net.java.sip.communicator.plugin.provisioning.PHONE_NUMBER_IGNORE_REGEX";

    /**
     * The name of the property for controlling the close action
     */
    public static final String ACCESSIBILITY_MODE = "net.java.sip.communicator.impl.gui.main.ACCESSIBILITY_MODE";
    public static final String NETWORK_CALL_HISTORY_ENABLED = "net.java.sip.communicator.impl.gui.main.NETWORK_CALL_HISTORY_ENABLED";
    private static boolean securityStatusHidden;
    private static String GUI_MENU_ICONS_DISABLED = "net.java.sip.communicator.impl.gui.MENU_ITEMS_DISABLED";

    /**
     * Property storing a string representation of the Locale used for Meeting email invitations.
     */
    private static final String EMAIL_INVITE_LANGUAGE = "net.java.sip.communicator.plugin.conference.impls.EMAIL_INVITE_LANGUAGE";
    private static final String CALL_RATING_SHOW_PROP = "net.java.sip.communicator.impl.callrating.CALL_RATING_SHOW";
    private static final String CALL_RATING_SEND_ERROR_REPORT = "net.java.sip.communicator.impl.callrating.CALL_RATING_SEND_ERROR_REPORT";
    private static final String HEADSET_RESPONSE_SETTING = "net.java.sip.communicator.impl.neomedia.HEADSET_RESPONSE";
    private static final String HEADSET_LOGGING_ENABLED_SETTING = "plugin.headsetmanager.HEADSET_API_LOGGING_ENABLED";
    private static final String UUID_PROP = "net.java.sip.communicator.UUID";

    /** The config option containing name of the OS e.g. "Windows 10" */
    public static final String OS_NAME_PROP = "plugin.errorreport.OS_NAME";

    /**
     * The config option containing version number of the OS e.g. "10.0.19044".
     */
    public static final String OS_VERSION_PROP = "plugin.errorreport.OS_VERSION";

    /** The build number of the first version on Windows 11. */
    private static final int MIN_WINDOWS_11_BUILD_NUMBER = 22000;

    /**
     * Property which controls whether or not we should show a dialog asking
     * the user to confirm that they would like to remove a user from a group
     * chat.
     */
    private static final String DONT_ASK_REMOVE_FROM_CHAT_PROP = "net.java.sip.communicator.impl.gui.main.chat.DONT_ASK_REMOVE_FROM_CHAT";

    /**
     * Configuration property which indicates whether automatic update
     * checking has been disabled for this user. This property is part of the user
     * configuration and is set in SIP PS.
     */
    private static final String DISABLE_UPDATE_CHECKING_PROP = "net.java.sip.communicator.plugin.update.DISABLE_AUTO_UPDATE_CHECKING";
    public static final String WEBSOCKET_SERVER_ENABLED = "plugin.websocketserver.WEBSOCKET_SERVER_ENABLED";

    /**
     * The name of the property indicating the name(s) of any applications
     * connected to Accession via the WebSocket.
     */
    private static final String WEBSOCKET_CONNECTED_APPLICATIONS = "plugin.websocketserver.WEBSOCKET_CONNECTED_APPLICATIONS";

    /**
     * Indicates whether a notification should be raised for a new WebSocket connection.
     */
    private static final String WEBSOCKET_NOTIFICATION_ENABLED = "plugin.websocketserver.WEBSOCKET_NOTIFICATION_ENABLED";

    /**
     * The name of the property for controlling whether we are the protocol
     * handler for sip/tel/callto URIs
     */
    public static final String URL_PROTOCOL_HANDLER_APP_PROP = "plugin.urlprotocolhandler.URL_PROTOCOL_HANDLER_APP";

    /**
     * The name of the property for controlling whether we should use the native
     * MacOS theme for the UI
     */
    public static final String USE_NATIVE_MAC_THEME_PROP = "net.java.sip.communicator.impl.gui.USE_NATIVE_THEME_ON_MACOS";
    private static final String PROPERTY_QA_MODE = "net.java.sip.communicator.QA_MODE";
    private static final boolean isQaMode = configService.global().getBoolean(PROPERTY_QA_MODE, false);

    /**
     * Boolean indicating whether the client is allowed to send diags data for
     * error reports, accessible throughout the code.
     */
    private static boolean isDataSendingEnabled;

    // Loads all cached configuration.
    static
    {
        configService.global().addPropertyChangeListener(new ConfigurationChangeListener());

        String isTransparentWindowEnabledProperty = "impl.gui.IS_TRANSPARENT_WINDOW_ENABLED";
        String isTransparentWindowEnabledString = configService.global().getString(isTransparentWindowEnabledProperty);

        if (isTransparentWindowEnabledString == null)
        {
            isTransparentWindowEnabledString = resources.getSettingsString(isTransparentWindowEnabledProperty);
        }

        if (isTransparentWindowEnabledString != null && isTransparentWindowEnabledString.length() > 0)
        {
            isTransparentWindowEnabled = Boolean.parseBoolean(isTransparentWindowEnabledString);
        }

        String windowTransparencyProperty = "impl.gui.WINDOW_TRANSPARENCY";
        String windowTransparencyString = configService.global().getString(windowTransparencyProperty);

        if (windowTransparencyString == null)
        {
            windowTransparencyString = resources.getSettingsString(windowTransparencyProperty);
        }

        if (windowTransparencyString != null && windowTransparencyString.length() > 0)
        {
            windowTransparency = Integer.parseInt(windowTransparencyString);
        }

        String isWindowDecoratedProperty = "impl.gui.IS_WINDOW_DECORATED";
        String isWindowDecoratedString = configService.global().getString(isWindowDecoratedProperty);

        if (isWindowDecoratedString == null)
        {
            isWindowDecoratedString = resources.getSettingsString(isWindowDecoratedProperty);
        }

        if (isWindowDecoratedString != null && isWindowDecoratedString.length() > 0)
        {
            isWindowDecorated = Boolean.parseBoolean(isWindowDecoratedString);
        }

        isShowSmileys = configService.global().getBoolean(SHOW_SMILEYS_PROPERTY, true);
        isChatShowContact = configService.global().getBoolean(CHAT_SHOW_CONTACT_PHOTO, true);
        isGroupRemoveDisabled = configService.global().getBoolean(
            "net.java.sip.communicator.impl.gui.main.contactlist.GROUP_REMOVE_DISABLED", false);
        isVideoCallButtonDisabled = configService.global().getBoolean(
            "net.java.sip.communicator.impl.gui.main.contactlist.VIDEO_CALL_BUTTON_DISABLED", false);
        isPresetStatusMessagesEnabled = configService.global().getBoolean(
            "net.java.sip.communicator.impl.gui.main.presence.PRESET_STATUS_MESSAGES", true);

        String singleInterfaceEnabledProp = resources.getSettingsString(SINGLE_WINDOW_INTERFACE_ENABLED);

        boolean isEnabled = false;

        if (singleInterfaceEnabledProp != null)
        {
            isEnabled = Boolean.parseBoolean(singleInterfaceEnabledProp);
        }
        else
        {
            isEnabled = Boolean.parseBoolean(resources.getSettingsString("impl.gui.SINGLE_WINDOW_INTERFACE"));
        }

        isSingleWindowInterfaceEnabled = configService.global().getBoolean( SINGLE_WINDOW_INTERFACE_ENABLED, isEnabled);

        String hideAccountStatusSelectorsProperty = "impl.gui.HIDE_ACCOUNT_STATUS_SELECTORS";
        String hideAccountsStatusDefaultValue = resources.getSettingsString(hideAccountStatusSelectorsProperty);

        if (hideAccountsStatusDefaultValue != null)
        {
            hideAccountStatusSelectors = Boolean.parseBoolean(hideAccountsStatusDefaultValue);
        }

        hideAccountStatusSelectors = configService.global().getBoolean(hideAccountStatusSelectorsProperty, hideAccountStatusSelectors);

        String autoAnswerDisableSubmenuProperty = "impl.gui.AUTO_ANSWER_DISABLE_SUBMENU";
        String autoAnswerDisableSubmenuDefaultValue = resources.getSettingsString(autoAnswerDisableSubmenuProperty);

        if (autoAnswerDisableSubmenuDefaultValue != null)
        {
            autoAnswerDisableSubmenu = Boolean.parseBoolean(autoAnswerDisableSubmenuDefaultValue);
        }

        autoAnswerDisableSubmenu = configService.global().getBoolean(autoAnswerDisableSubmenuProperty, autoAnswerDisableSubmenu);
        isNormalizePhoneNumber = configService.global().getBoolean("impl.gui.NORMALIZE_PHONE_NUMBER", true);
        securityStatusHidden = configService.global().getBoolean(SECURITY_STATUS_HIDDEN_PROP, false);
        acceptPhoneNumberWithAlphaChars = configService.global().getBoolean("impl.gui.ACCEPT_PHONE_NUMBER_WITH_ALPHA_CHARS", true);
    }

    /**
     * Globally accessible method for returning whether or not the client is in
     * QA mode, required for significant functionality.
     * @return Whether the client is in QA mode
     */
    public static boolean isQaMode()
    {
        return isQaMode;
    }

    /**
     * Globally accessible method for setting whether this branding allows the
     * sending of diagnostics data for manual error reports.
     *
     * @param enabled Whether data sending is enabled
     */
    public static void setDataSendingEnabled(boolean enabled)
    {
        isDataSendingEnabled = enabled;
    }

    /**
     * Globally accessible method for returning whether or not the client has
     * data sending enabled, required to open and send error reports.
     * @return Whether the provider for this user has data sending enabled
     */
    public static boolean isDataSendingEnabled()
    {
        return isDataSendingEnabled;
    }

    /**
     * Globally accessible method for deciding whether to show error frames for
     * generic (non-categorised) uncaught exceptions.
     *
     * @return Whether this branding allows uncaught exceptions to be shown
     */
    public static boolean isShowingUncaughtExceptions()
    {
        return configService.user().getBoolean("plugin.errorreport.SEND_ON_UNCAUGHT_EXCEPTION", false);
    }

    /**
     * Return TRUE if "autoPopupNewMessage" property is true, otherwise - return
     * FALSE. Indicates to the user interface whether new messages should be
     * opened and bring to front.
     * @return TRUE if "autoPopupNewMessage" property is true, otherwise
     * - return FALSE.
     */
    public static boolean isAutoPopupNewMessage()
    {
     // This has been hard-coded to false as the current option is confusing
     // and poorly defined. However the code is left in case this is
     // implemented correctly in future
     return false;
    }

    /**
     * Return TRUE if "showApplication" property is true, otherwise - return
     * FALSE. Indicates to the user interface whether the main application
     * window should shown or hidden on startup.
     * @return TRUE if "showApplication" property is true, otherwise - return
     * FALSE.
     */
    public static boolean isApplicationVisible()
    {
        return configService.user().getBoolean("net.java.sip.communicator.impl.systray.showApplication", true);
    }

    /**
     * Return TRUE if "quitWarningShown" property is true, otherwise -
     * return FALSE. Indicates to the user interface whether the quit warning
     * dialog should be shown when user clicks on the X button.
     * @return TRUE if "quitWarningShown" property is true, otherwise -
     * return FALSE. Indicates to the user interface whether the quit warning
     * dialog should be shown when user clicks on the X button.
     */
    public static boolean isQuitWarningShown()
    {
        return configService.user().getBoolean("net.java.sip.communicator.impl.gui.quitWarningShown", true);
    }

    /**
     * Return TRUE if "sendTypingNotifications" property is true, otherwise -
     * return FALSE. Indicates to the user interface whether typing
     * notifications are enabled or disabled.
     * @return TRUE if "sendTypingNotifications" property is true, otherwise -
     * return FALSE.
     */
    public static boolean isSendTypingNotifications()
    {
        return configService.user().getBoolean("service.gui.SEND_TYPING_NOTIFICATIONS_ENABLED", true);
    }

    /**
     * Updates the "sendTypingNotifications" property through the
     * <tt>ConfigurationService</tt>.
     *
     * @param isSendTypingNotif <code>true</code> to indicate that typing
     * notifications are enabled, <code>false</code> otherwise.
     */
    public static void setSendTypingNotifications(boolean isSendTypingNotif)
    {
        configService.user().setProperty("service.gui.SEND_TYPING_NOTIFICATIONS_ENABLED", Boolean.toString(isSendTypingNotif));
    }

    /**
     * Returns TRUE if the "isMoveContactConfirmationRequested" property is true,
     * otherwise - returns FALSE. Indicates to the user interface whether the
     * confirmation window during the move contact process is enabled or not.
     * @return TRUE if the "isMoveContactConfirmationRequested" property is true,
     * otherwise - returns FALSE
     */
    public static boolean isMoveContactConfirmationRequested()
    {
        return configService.user().getBoolean("net.java.sip.communicator.impl.gui.isMoveContactConfirmationRequested", true);
    }

    /**
     * @return false if email login is explicitly disabled, otherwise returns
     * true.
     */
    public static boolean isEmailLoginEnabled()
    {
        return configService.global().getBoolean(PNAME_EMAIL_LOGIN_ENABLED, true);
    }

    /**
     * Some function, e.g. contacts is hidden in a meeting only client.
     *
     * @return TRUE if contacts are enabled
     */
    public static boolean isCallingOrImEnabled()
    {
        return isCallingEnabled() || isImEnabled();
    }

    /**
     * Whether the current user is a standalone meeting user. A user is a
     * standalone meeting user if they don't have calling or IM enabled.
     *
     * @return true if the user is a standalone meeting user and false otherwise.
     */
    public static boolean isStandaloneMeetingUser()
    {
        return !isCallingOrImEnabled();
    }

    /**
     * Returns TRUE if either CTD or VoIP calling is active.  Used to determine
     * whether to show UI components related to calling.
     */
    public static boolean isCallingEnabled()
    {
        boolean ctdActive =  configService.user().getBoolean(ClassOfServiceService.CONFIG_COS_CTD_ALLOWED, true) &&
                             configService.user().getBoolean(ClassOfServiceService.CONFIG_COS_CTD_ENABLED, true);
        return (isVoIPEnabled() || ctdActive);
    }

    /**
     * @return true if VoIP calling is enabled by both the CoS and user
     */
    public static boolean isVoIPEnabled()
    {
        return isVoIPEnabledInCoS() && isVoIPEnabledByUser();
    }

    /**
     * @return whether direct (VoIP) calling is enabled by the user. If emergency calling locations are required
     * (Ray Baum), then direct calling will always be enabled.
     */
    public static boolean isVoIPEnabledByUser()
    {
        boolean voipEnabledByUser = configService.user().getBoolean(ProvisioningServiceImpl.VOIP_ENABLED_PROP, true);
        // If the BG needs emergency calling support, then we cannot
        // allow the user to actually disable all VoIP support
        boolean bgNeedsEmergencyCalling = false;
        CommPortalService commPortalService = UtilActivator.getCommPortalService();
        if (commPortalService != null)
        {
            bgNeedsEmergencyCalling = commPortalService.isEmergencyLocationSupportNeeded();
        }
        else
        {
            logger.debug("Asked whether BG needs emergency calling before we have data from CommPortal.");
        }

        voipEnabledByUser |= bgNeedsEmergencyCalling;
        return voipEnabledByUser;
    }

    /**
     * @return whether VoIP calling is enabled by the CoS only
     */
    public static boolean isVoIPEnabledInCoS()
    {
        return configService.user().getBoolean(ClassOfServiceService.CONFIG_COS_VOIPALLOWED, true);
    }

    /**
     * Returns TRUE if phone service is enabled.  Currently this is only false for 'meeting + IM only'
     * subscribers. The setting roughly translates to whether SIP is enabled/required.
     * This is a looser requirement than isVoIPEnabled, because some users have SIP enabled but no VoIP
     * e.g. if they are CTD only (so not VoIP) but use SIP for LSM or VM MWI.
     */
    public static boolean isPhoneServiceEnabled()
    {
        return configService.user().getBoolean(ClassOfServiceService.CONFIG_COS_HAS_PHONE_SERVICE, true);
    }

    /**
     * We enable audio and video if the user has either VoIP or meeting enabled.
     * When either of those is updated, this method should be called to update
     * the relevant config properties.  Note that we can't simply have a method
     * in ConfigurationUtils that checks whether VoIP/Meeting is enabled instead
     * of reading these config properties, as this config needs to be read by
     * WASAPISystem in libjitsi, which cannot access ConfigurationUtils.
     */
    public static void updateAudioVideoEnabled()
    {
        ConferenceService confService = UtilActivator.getConferenceService();

        // This method will first be called when we receive the CoS, before the
        // ConferenceService has registered.  Therefore, if ConferenceService is
        // null, assume meeting will be enabled, as this gives better UX and
        // will be updated as soon as ConferenceService registers.
        boolean joinMeetingAllowed = (confService == null) ? true : confService.isJoinEnabled();
        boolean voipEnabled = isVoIPEnabled();

        logger.info("Updating whether audio/video is enabled  based on voipEnabled = " + voipEnabled +
                    " and joinMeetingAllowed = " + joinMeetingAllowed + " (confService = " + confService + ")");

        boolean audioVideoDisabled = !voipEnabled && !joinMeetingAllowed;
        configService.user().setProperty("net.java.sip.communicator.impl.neomedia.AUDIO_CONFIG_DISABLED", audioVideoDisabled);
        configService.user().setProperty("net.java.sip.communicator.impl.neomedia.VIDEO_CONFIG_DISABLED", audioVideoDisabled);
    }

    /**
     * Returns <code>true</code> if the "isImEnabled" property is true,
     * otherwise - returns <code>false</code>. Indicates to the user interface
     * whether IM is enabled.
     *
     * @return <code>true</code> if the "isImEnabled" property is true,
     * otherwise - returns <code>false</code>.
     */
    public static boolean isImEnabled()
    {
        return configService.user().getBoolean(IM_ENABLED_PROP, true);
    }

    /**
     * @return <code>true</code> if IM message correction is enabled, otherwise
     * returns <code>false</code>.
     */
    public static boolean isImCorrectionEnabled()
    {
        return configService.user().getBoolean(IM_CORRECTION_ENABLED_PROP, true);
    }

    /**
     * Returns <code>true</code> if the "isSmsEnabled" property is true,
     * otherwise - returns <code>false</code>. Indicates to the user interface
     * whether SMS is enabled.
     *
     * @return <code>true</code> if the "isSmsEnabled" property is true,
     * otherwise - returns <code>false</code>.
     */
    public static boolean isSmsEnabled()
    {
        return configService.user().getBoolean(SMS_ENABLED_PROP, true);
    }

    /**
     * Returns the IM provisioning source.
     *
     * @return The IM provisioning source.
     */
    public static String getImProvSource()
    {
        return configService.user().getString(IM_PROVISION_SOURCE_PROP, "Manual");
    }

    /**
     * Returns whether the branding has autopopulation of IM addresses for BGContacts enabled.
     *
     * Note that IM autopopulation requires both BG Contacts and CommPortal-provisioned
     * IM to be enabled.  If they are not, this branding option has no effect.
     */
    public static boolean autoPopulateIMEnabled()
    {
        return configService.global().getBoolean(IM_AUTOPOPULATE_PROP, true);
    }

    /**
    * Returns <code>true</code> if the "isVideoCallButtonInChatEnabled"
    * property is true, otherwise - returns <code>false</code>. Indicates to
    * the user interface whether the video call button should be added to the
    * chat window.
    * @return <code>true</code> if the "isVideoCallButtonInChatEnabled"
    * property is true, otherwise - returns <code>false</code>.
    */
    public static boolean isVideoCallButtonInChatEnabled()
    {
        return isVideoCallButtonInChatEnabled;
    }

    /**
    * Returns <code>true</code> if the "isNavigationButtonsEnabled"
    * property is true, otherwise - returns <code>false</code>. Indicates to
    * the user interface whether the history navigation buttons should be
    * added to the chat window.
    * @return <code>true</code> if the "isNavigationButtonsEnabled"
    * property is true, otherwise - returns <code>false</code>.
    */
    public static boolean isNavigationButtonsEnabled()
    {
        return isNavigationButtonsEnabled;
    }

    /**
     * Returns the value of the configured double-click-on-contact-action.
     * Defaults to CALL.  The value can be one of
     * <li>CALL
     * <li>IM
     * <li>VIEW
     *
     * @return The double click on contact action
     */
    public static String getContactDoubleClickAction()
    {
        String defaultAction = isCallingEnabled() ? "CALL" : isImEnabled() ? "IM" : "VIEW";
        return configService.user().getString("service.gui.CONTACT_DOUBLE_CLICK_ACTION", defaultAction);
    }

    public static void setContactDoubleClickAction(String newItemValue)
    {
        configService.user().setProperty("service.gui.CONTACT_DOUBLE_CLICK_ACTION", newItemValue);
    }

    /**
     * Returns <code>true</code> if the "isOpenWindowOnNewChatEnabled" property
     * is true, otherwise - returns <code>false</code>. Indicates to the user
     * interface whether to open a chat window when a new chat is received.
     * @return <code>true</code> if the "isOpenWindowOnNewChatEnabled" property
     * is true, otherwise - returns <code>false</code>.
     */
    public static boolean isOpenWindowOnNewChatEnabled()
    {
        return configService.user().getBoolean(OPEN_WINDOW_ON_NEW_CHAT_PROP, true);
    }

    /**
     * Updates the "isOpenWindowOnNewChatEnabled" property through the
     * <tt>ConfigurationService</tt>.
     *
     * @param isEnabled indicates whether to open a chat window when a new chat
     * is received.
     */
    public static void setOpenWindowOnNewChatEnabled(boolean isEnabled)
    {
        configService.user().setProperty(OPEN_WINDOW_ON_NEW_CHAT_PROP, Boolean.toString(isEnabled));
    }

    /**
     * Returns <code>true</code> if the "isHistoryShown" property is
     * true, otherwise - returns <code>false</code>. Indicates to the user
     * whether the history is shown in the chat window.
     * @return <code>true</code> if the "isHistoryShown" property is
     * true, otherwise - returns <code>false</code>.
     */
    public static boolean isHistoryShown()
    {
        return configService.user().getBoolean("service.gui.IS_MESSAGE_HISTORY_SHOWN", true);
    }

    /**
     * Updates the "isHistoryShown" property through the
     * <tt>ConfigurationService</tt>.
     *
     * @param isShown indicates if the message history is shown
     */
    public static void setHistoryShown(boolean isShown)
    {
        configService.user().setProperty("service.gui.IS_MESSAGE_HISTORY_SHOWN", Boolean.toString(isShown));
    }

    /**
     * Returns <code>true</code> if the "isWindowDecorated" property is
     * true, otherwise - returns <code>false</code>..
     * @return <code>true</code> if the "isWindowDecorated" property is
     * true, otherwise - returns <code>false</code>.
     */
    public static boolean isWindowDecorated()
    {
        return isWindowDecorated;
    }

    /**
     * Returns <code>true</code> if the "isShowSmileys" property is
     * true, otherwise - returns <code>false</code>..
     * @return <code>true</code> if the "isShowSmileys" property is
     * true, otherwise - returns <code>false</code>.
     */
    public static boolean isShowSmileys()
    {
        return isShowSmileys;
    }

    /**
     * Returns <code>true</code> if the "isChatShowContact" property is true,
     * otherwise - returns <code>false</code>.
     * @return <code>true</code> if the "isChatShowContact" property is true,
     * otherwise - returns <code>false</code>.
     */
    public static boolean isChatShowContact()
    {
        return isChatShowContact;
    }

    /**
     * Returns <code>true</code> if the "GROUP_REMOVE_DISABLED" property is
     * true, otherwise - returns <code>false</code>.
     * @return <code>true</code> if the "GROUP_REMOVE_DISABLED" property is
     * true, otherwise - returns <code>false</code>.
     */
    public static boolean isGroupRemoveDisabled()
    {
        return isGroupRemoveDisabled;
    }

    /**
     * Returns whether the "VIDEO_CALL_BUTTON_DISABLED" property is set.
     *
     * @return Whether the "VIDEO_CALL_BUTTON_DISABLED" property is set.
     */
    public static boolean isVideoCallButtonDisabled()
    {
        return isVideoCallButtonDisabled;
    }

    /**
     * Returns <code>true</code> if the "PRESET_STATUS_MESSAGES" property is
     * true, otherwise - returns <code>false</code>.
     * @return <code>true</code> if the "PRESET_STATUS_MESSAGES" property is
     * true, otherwise - returns <code>false</code>.
     */
    public static boolean isPresetStatusMessagesEnabled()
    {
        return isPresetStatusMessagesEnabled;
    }

    /**
     * Return the "sendMessageCommand" property that was saved previously
     * through the <tt>ConfigurationService</tt>. Indicates to the user
     * interface whether the default send message command is Enter or CTRL-Enter.
     * @return "Enter" or "CTRL-Enter" message commands.
     */
    public static String getSendMessageCommand()
    {
        // Load the "sendMessageCommand" property.
        String messageCommandProperty = "service.gui.SEND_MESSAGE_COMMAND";
        String messageCommand = configService.user().getString(messageCommandProperty);

        if (messageCommand == null)
        {
            messageCommand = resources.getSettingsString(messageCommandProperty);
        }

        return messageCommand;
    }

    /**
     * Updates the "sendMessageCommand" property through the
     * <tt>ConfigurationService</tt>.
     *
     * @param newMessageCommand the command used to send a message ( it could be
     * ENTER_COMMAND or CTRL_ENTER_COMMAND)
     */
    public static void setSendMessageCommand(String newMessageCommand)
    {
        configService.user().setProperty("service.gui.SEND_MESSAGE_COMMAND", newMessageCommand);
    }

    /**
     * Return the "lastContactParent" property that was saved previously
     * through the <tt>ConfigurationService</tt>. Indicates
     * the last selected group on adding new contact
     * @return group name of the last selected group when adding contact.
     */
    public static String getLastContactParent()
    {
        return configService.user().getString("net.java.sip.communicator.impl.gui.addcontact.lastContactParent");
    }

    /**
     * Returns the call conference provider used for the last conference call.
     * @return the call conference provider used for the last conference call
     */
    public static ProtocolProviderService getLastCallConferenceProvider()
    {
        if (lastCallConferenceProvider != null)
        {
            return lastCallConferenceProvider;
        }

        // Obtain the "lastCallConferenceAccount" property from the
        // configuration service
        return findProviderFromAccountId(configService.user().getString(
            "net.java.sip.communicator.impl.gui.call.lastCallConferenceProvider"));
    }

    /**
     * Returns the protocol provider associated with the given
     * <tt>accountId</tt>.
     * @param savedAccountId the identifier of the account
     * @return the protocol provider associated with the given
     * <tt>accountId</tt>
     */
    private static ProtocolProviderService findProviderFromAccountId(String savedAccountId)
    {
        ProtocolProviderService protocolProvider = null;
        for (ProtocolProviderFactory providerFactory : UtilActivator.getProtocolProviderFactories().values())
        {
            ServiceReference<?> serRef;

            for (AccountID accountId : providerFactory.getRegisteredAccounts())
            {
                // We're interested only in the savedAccountId
                if (!accountId.getAccountUniqueID().equals(savedAccountId))
                {
                    continue;
                }

                serRef = providerFactory.getProviderForAccount(accountId);
                protocolProvider = (ProtocolProviderService) UtilActivator.bundleContext.getService(serRef);
            }
        }

        return protocolProvider;
    }

    /**
     * Returns a list of all known providers that support the given
     * OperationSet.
     *
     * @param operationSet    The operation set for which we're looking for
     * providers
     * @return a list of all known providers that support the given operation
     * set
     */
    public static List<ProtocolProviderService> getOperationSetProviders(Class<? extends OperationSet> operationSet)
    {
        List<ProtocolProviderService> opSetProviders = new ArrayList<>();
        ProtocolProviderService protocolProvider;

        for (ProtocolProviderFactory factory : UtilActivator.getProtocolProviderFactories().values())
        {
            ServiceReference<?> serRef;

            for (AccountID accountID : factory.getRegisteredAccounts())
            {
                serRef = factory.getProviderForAccount(accountID);
                protocolProvider = (ProtocolProviderService) UtilActivator.bundleContext.getService(serRef);

                if (protocolProvider.getOperationSet(operationSet) != null)
                {
                    opSetProviders.add(protocolProvider);
                }
            }
        }

        return opSetProviders;
    }

    /**
     * Returns the number of messages from chat history that would be shown in
     * the chat window.
     * @return the number of messages from chat history that would be shown in
     * the chat window.
     */
    public static int getChatHistorySize()
    {
        return configService.user().getInt("service.gui.MESSAGE_HISTORY_SIZE", 10);
    }

    /**
     * Updates the "chatHistorySize" property through the
     * <tt>ConfigurationService</tt>.
     *
     * @param historySize indicates if the history logging is
     * enabled.
     */
    public static void setChatHistorySize(int historySize)
    {
        configService.user().setProperty("service.gui.MESSAGE_HISTORY_SIZE", Integer.toString(historySize));
    }

    /**
     * Returns the preferred height of the chat write area.
     *
     * @return the preferred height of the chat write area.
     */
    public static int getChatWriteAreaSize()
    {
        return configService.user().getInt("net.java.sip.communicator.impl.gui.CHAT_WRITE_AREA_SIZE", 0);
    }

    /**
     * Returns <code>true</code> if transparent windows are enabled,
     * <code>false</code> otherwise.
     *
     * @return <code>true</code> if transparent windows are enabled,
     * <code>false</code> otherwise.
     */
    public static boolean isTransparentWindowEnabled()
    {
        return isTransparentWindowEnabled;
    }

    /**
     * Returns the transparency value for all transparent windows.
     *
     * @return the transparency value for all transparent windows.
     */
    public static int getWindowTransparency()
    {
        return windowTransparency;
    }

    /**
     * Returns the last opened directory of the send file file chooser.
     *
     * @return the last opened directory of the send file file chooser
     */
    public static String getSendFileLastDir()
    {
        return configService.user().getString("net.java.sip.communicator.impl.gui.chat.filetransfer.SEND_FILE_LAST_DIR");
    }

    /**
     * Returns <code>true</code> if phone numbers should be normalized,
     * <code>false</code> otherwise.
     *
     * @return <code>true</code> if phone numbers should be normalized,
     * <code>false</code> otherwise.
     */
    public static boolean isNormalizePhoneNumber()
    {
        return isNormalizePhoneNumber;
    }

    /**
     * Updates the "NORMALIZE_PHONE_NUMBER" property.
     *
     * @param isNormalize indicates to the user interface whether all dialed
     * phone numbers should be normalized
     */
    public static void setNormalizePhoneNumber(boolean isNormalize)
    {
        isNormalizePhoneNumber = isNormalize;
        configService.global().setProperty("impl.gui.NORMALIZE_PHONE_NUMBER", Boolean.toString(isNormalize));
    }

    /**
     * Indicates if in call security indication is hidden.
     *
     * @return whether call security indication is hidden.
     */
    public static boolean isSecurityStatusHidden()
    {
        return securityStatusHidden;
    }

    /**
     * Whether call windows are configured to appear always on top.
     *
     * @return whether call windows are always on top
     */
    public static boolean isCallAlwaysOnTop()
    {
        // Always on top is not permitted in Accessibility mode
        boolean isCallAlwaysOnTop = configService.user().getBoolean(
            "net.java.sip.communicator.impl.gui.main.call.CALL_ALWAYS_ON_TOP", false);
        return isCallAlwaysOnTop && !isAccessibilityMode();
    }

    /**
     * Updates the "CALL_ALWAYS_ON_TOP" property.
     *
     * @param isCallOnTop indicates to the user interface whether call
     * windows should always be on top
     */
    public static void setCallAlwaysOnTop(boolean isCallOnTop)
    {
        configService.user().setProperty(CALL_ON_TOP_PROP, Boolean.toString(isCallOnTop));
    }

    /**
     * Returns <code>true</code> if a string with an alphabetical character might
     * be considered as a phone number.  <code>false</code> otherwise.
     *
     * @return <code>true</code> if a string with an alphabetical character might
     * be considered as a phone number.  <code>false</code> otherwise.
     */
    public static boolean acceptPhoneNumberWithAlphaChars()
    {
        return acceptPhoneNumberWithAlphaChars;
    }

    /**
     * Updates the "ACCEPT_PHONE_NUMBER_WITH_CHARS" property.
     *
     * @param accept indicates to the user interface whether a string with
     * alphabetical characters might be accepted as a phone number.
     */
    public static void setAcceptPhoneNumberWithAlphaChars(boolean accept)
    {
        acceptPhoneNumberWithAlphaChars = accept;
        configService.global().setProperty(
            "impl.gui.ACCEPT_PHONE_NUMBER_WITH_ALPHA_CHARS", Boolean.toString(acceptPhoneNumberWithAlphaChars));
    }

    /**
     * Whether to hide account statuses from global menu.
     * @return whether to hide account statuses.
     */
    public static boolean isHideAccountStatusSelectorsEnabled()
    {
        return hideAccountStatusSelectors;
    }

    /**
     * Whether creation of separate submenu for auto answer is disabled.
     * @return whether creation of separate submenu for auto answer
     * is disabled.
     */
    public static boolean isAutoAnswerDisableSubmenu()
    {
        return autoAnswerDisableSubmenu;
    }

    /**
     * Indicates if the single interface is enabled.
     *
     * @return <tt>true</tt> if the single window interface is enabled,
     * <tt>false</tt> - otherwise
     */
    public static boolean isSingleWindowInterfaceEnabled()
    {
        return isSingleWindowInterfaceEnabled;
    }

    /**
     * Updates the "singleWindowInterface" property through the
     * <tt>ConfigurationService</tt>.
     *
     * @param isEnabled <code>true</code> to indicate that the
     * single window interface is enabled, <tt>false</tt> - otherwise
     */
    public static void setSingleWindowInterfaceEnabled(boolean isEnabled)
    {
        isSingleWindowInterfaceEnabled = isEnabled;
        configService.global().setProperty(SINGLE_WINDOW_INTERFACE_ENABLED, isEnabled);
    }

    /**
     * Sets the transparency value for all transparent windows.
     *
     * @param transparency the transparency value for all transparent windows.
     */
    public static void setWindowTransparency(int transparency)
    {
        windowTransparency = transparency;
    }

    /**
     * Updates the "showApplication" property through the
     * <tt>ConfigurationService</tt>.
     *
     * @param isVisible <code>true</code> to indicate that the
     * application should be shown, <code>false</code> otherwise.
     */
    public static void setApplicationVisible(boolean isVisible)
    {
        configService.user().setProperty(
            "net.java.sip.communicator.impl.systray.showApplication", Boolean.toString(isVisible));
    }

    /**
     * Updates the "showAppQuitWarning" property through the
     * <tt>ConfigurationService</tt>.
     *
     * @param isWarningShown indicates if the message warning the user that the
     * application would not be closed if she clicks the X button would be
     * shown again.
     */
    public static void setQuitWarningShown(boolean isWarningShown)
    {
        configService.user().setProperty(
            "net.java.sip.communicator.impl.gui.quitWarningShown", Boolean.toString(isWarningShown));
    }

    /**
     * Saves the popup handler choice made by the user.
     *
     * @param handler the handler which will be used
     */
    public static void setPopupHandlerConfig(String handler)
    {
        configService.user().setProperty("systray.POPUP_HANDLER", handler);
    }

     /**
     * Updates the "lastContactParent" property through the
     * <tt>ConfigurationService</tt>.
     *
     * @param groupName the group name of the selected group when adding
     * last contact
     */
    public static void setLastContactParent(String groupName)
    {
        configService.user().setProperty("net.java.sip.communicator.impl.gui.addcontact.lastContactParent", groupName);
    }

    /**
     * Updates the "isMoveContactQuestionEnabled" property through the
     * <tt>ConfigurationService</tt>.
     *
     * @param isRequested indicates if a confirmation would be requested
     * from user during the move contact process.
     */
    public static void setMoveContactConfirmationRequested(boolean isRequested)
    {
        configService.user().setProperty(
            "net.java.sip.communicator.impl.gui.isMoveContactConfirmationRequested", Boolean.toString(isRequested));
    }

    /**
     * Updates the "isTransparentWindowEnabled" property through the
     * <tt>ConfigurationService</tt>.
     *
     * @param isTransparent indicates if the transparency is enabled in the
     * application.
     */
    public static void setTransparentWindowEnabled(boolean isTransparent)
    {
        isTransparentWindowEnabled = isTransparent;
        configService.global().setProperty(
            "impl.gui.IS_TRANSPARENT_WINDOW_ENABLED", Boolean.toString(isTransparentWindowEnabled));
    }

    /**
     * Updates the "isShowSmileys" property through the
     * <tt>ConfigurationService</tt>.
     *
     * @param isVisible indicates if the smileys are visible
     */
    public static void setShowSmileys(boolean isVisible)
    {
        isShowSmileys = isVisible;
        configService.global().setProperty(SHOW_SMILEYS_PROPERTY, Boolean.toString(isShowSmileys));
    }

    /**
     * Updates the "net.java.sip.communicator.impl.gui.CHAT_WRITE_AREA_SIZE"
     * property through the <tt>ConfigurationService</tt>.
     *
     * @param size the new size to set
     */
    public static void setChatWriteAreaSize(int size)
    {
        configService.user().setProperty("net.java.sip.communicator.impl.gui.CHAT_WRITE_AREA_SIZE", Integer.toString(size));
    }

    /**
     * Updates the "SEND_FILE_LAST_DIR"
     * property through the <tt>ConfigurationService</tt>.
     *
     * @param lastDir last download directory
     */
    public static void setSendFileLastDir(String lastDir)
    {
        configService.user().setProperty(
            "net.java.sip.communicator.impl.gui.chat.filetransfer.SEND_FILE_LAST_DIR", lastDir);
    }

    /**
     * Sets the call conference provider used for the last conference call.
     * @param protocolProvider the call conference provider used for the last
     * conference call
     */
    public static void setLastCallConferenceProvider(
        ProtocolProviderService protocolProvider)
    {
        lastCallConferenceProvider = protocolProvider;
        configService.user().setProperty(
            "net.java.sip.communicator.impl.gui.call.lastCallConferenceProvider",
            protocolProvider.getAccountID().getAccountUniqueID());
    }

    /**
     * Returns the current language configuration.
     *
     * @return the current locale
     */
    public static Locale getCurrentLanguage()
    {
        String localeId = configService.global().getString(ResourceManagementService.DEFAULT_LOCALE_CONFIG);
        return (localeId != null) ? ResourceManagementServiceUtils.getLocale(localeId) : Locale.getDefault();
    }

    /**
     * Sets the current language configuration.
     *
     * @param locale the locale to set
     */
    public static void setLanguage(Locale locale)
    {
        String language = locale.getLanguage();
        String country = locale.getCountry();

        configService.global().setProperty(
            ResourceManagementService.DEFAULT_LOCALE_CONFIG,
            (country.length() > 0) ? (language + '_' + country) : language);
    }

    /**
     * Saves a chat room through the <tt>ConfigurationService</tt>.
     *
     * @param protocolProvider the protocol provider to which the chat room
     * belongs
     * @param oldChatRoomId the old identifier of the chat room
     * @param newChatRoomId the new identifier of the chat room
     * @param newChatRoomName the new chat room name
     * @param newChatRoomSubject the new chat room subject
     */
    public static void saveChatRoom(ProtocolProviderService protocolProvider,
                                    String oldChatRoomId,
                                    String newChatRoomId,
                                    String newChatRoomName,
                                    String newChatRoomSubject)
    {
        String prefix = "net.java.sip.communicator.impl.gui.accounts";
        List<String> accounts = configService.user().getPropertyNamesByPrefix(prefix, true);

        for (String accountRootPropName : accounts)
        {
            String accountUID = configService.user().getString(accountRootPropName);

            if(accountUID.equals(protocolProvider.getAccountID().getAccountUniqueID()))
            {
                List<String> chatRooms = configService.user().getPropertyNamesByPrefix(accountRootPropName + ".chatRooms", true);

                boolean isExistingChatRoom = false;

                for (String chatRoomPropName : chatRooms)
                {
                    String chatRoomID = configService.user().getString(chatRoomPropName);

                    if (!oldChatRoomId.equals(chatRoomID))
                    {
                        continue;
                    }

                    isExistingChatRoom = true;
                    configService.user().setProperty(chatRoomPropName, newChatRoomId);
                    configService.user().setProperty(chatRoomPropName + ".chatRoomName", newChatRoomName);

                    // Sometimes the server returns null for the subject when
                    // one is set.  Therefore, don't save null subjects as we
                    // might overwrite the valid subject name that we
                    // previously stored.
                    if (newChatRoomSubject != null)
                    {
                        configService.user().setProperty(chatRoomPropName + ".chatRoomSubject", newChatRoomSubject);
                    }

                    break;
                }

                if (!isExistingChatRoom)
                {
                String chatRoomNodeName = "chatRoom" + System.currentTimeMillis();
                    String chatRoomPackage = accountRootPropName + ".chatRooms." + chatRoomNodeName;
                    configService.user().setProperty(chatRoomPackage, newChatRoomId);
                    configService.user().setProperty(chatRoomPackage + ".chatRoomName", newChatRoomName);

                    // Sometimes the server returns null for the subject when
                    // one is set.  Therefore, don't save null subjects as we
                    // might overwrite the valid subject name that we
                    // previously stored.
                    if (newChatRoomSubject != null)
                    {
                        configService.user().setProperty(chatRoomPackage + ".chatRoomSubject", newChatRoomSubject);
                    }
                }

                return;
            }
        }
    }

    /**
     * Updates the status of the chat room through the
     * <tt>ConfigurationService</tt>.
     *
     * @param protocolProvider the protocol provider to which the chat room
     * belongs
     * @param chatRoomId the identifier of the chat room to update
     * @param chatRoomStatus the new status of the chat room
     */
    public static void updateChatRoomStatus(
            ProtocolProviderService protocolProvider,
            String chatRoomId,
            String chatRoomStatus)
    {
        String prefix = "net.java.sip.communicator.impl.gui.accounts";
        List<String> accounts = configService.user().getPropertyNamesByPrefix(prefix, true);

        for (String accountRootPropName : accounts)
        {
            String accountUID = configService.user().getString(accountRootPropName);

            if (accountUID.equals(protocolProvider.getAccountID().getAccountUniqueID()))
            {
                List<String> chatRooms = configService.user()
                    .getPropertyNamesByPrefix(accountRootPropName + ".chatRooms", true);

                for (String chatRoomPropName : chatRooms)
                {
                    String chatRoomID = configService.user().getString(chatRoomPropName);

                    if (!chatRoomId.equals(chatRoomID))
                    {
                        continue;
                    }

                    configService.user().setProperty(chatRoomPropName + ".lastChatRoomStatus", chatRoomStatus);

                    return;
                }
            }
        }
    }

    /**
     * Deletes the chat room through the <tt>ConfigurationService</tt>.
     *
     * @param protocolProvider the protocol provider to which the chat room
     * belongs
     * @param chatRoomId the identifier of the chat room to delete
     */
    public static void deleteChatRoom(
            ProtocolProviderService protocolProvider,
            String chatRoomId)
    {
        String prefix = "net.java.sip.communicator.impl.gui.accounts";
        List<String> accounts = configService.user().getPropertyNamesByPrefix(prefix, true);

        for (String accountRootPropName : accounts)
        {
            String accountUID = configService.user().getString(accountRootPropName);

            if (accountUID.equals(protocolProvider.getAccountID().getAccountUniqueID()))
            {
                List<String> chatRooms = configService.user()
                    .getPropertyNamesByPrefix(accountRootPropName + ".chatRooms", true);

                for (String chatRoomPropName : chatRooms)
                {
                    String chatRoomID = configService.user().getString(chatRoomPropName);

                    if (!chatRoomId.equals(chatRoomID))
                    {
                        continue;
                    }

                    configService.user().removeProperty(chatRoomPropName);
                    return;
                }
            }
        }
    }

    /**
     * Updates the value of a chat room property through the
     * <tt>ConfigurationService</tt>.
     *
     * @param protocolProvider the protocol provider to which the chat room
     * belongs
     * @param chatRoomId the identifier of the chat room to update
     * @param property the name of the property of the chat room
     * @param value the value of the property if null, property will be removed
     */
    public static void updateChatRoomProperty(
            ProtocolProviderService protocolProvider,
            String chatRoomId,
            String property,
            String value)
    {
        String prefix = "net.java.sip.communicator.impl.gui.accounts";
        List<String> accounts = configService.user().getPropertyNamesByPrefix(prefix, true);

        for (String accountRootPropName : accounts)
        {
            String accountUID = configService.user().getString(accountRootPropName);

            if (accountUID.equals(protocolProvider.getAccountID().getAccountUniqueID()))
            {
                List<String> chatRooms = configService.user()
                    .getPropertyNamesByPrefix(accountRootPropName + ".chatRooms", true);

                for (String chatRoomPropName : chatRooms)
                {
                    String chatRoomID = configService.user().getString(chatRoomPropName);

                    if (!chatRoomId.equals(chatRoomID))
                    {
                        continue;
                    }

                    if (value != null)
                    {
                        configService.user().setProperty(chatRoomPropName + "." + property, value);
                    }
                    else
                    {
                        configService.user().removeProperty(chatRoomPropName + "." + property);
                    }
                    return;
                }
            }
        }
    }

    /**
     * Returns the chat room property, saved through the
     * <tt>ConfigurationService</tt>.
     *
     * @param protocolProvider the protocol provider, to which the chat room
     * belongs
     * @param chatRoomId the identifier of the chat room
     * @param property the property name, saved through the
     * <tt>ConfigurationService</tt>.
     * @return the value of the property, saved through the
     * <tt>ConfigurationService</tt>.
     */
    public static String getChatRoomProperty(
        ProtocolProviderService protocolProvider,
        String chatRoomId,
        String property)
    {
        String prefix = "net.java.sip.communicator.impl.gui.accounts";
        List<String> accounts = configService.user().getPropertyNamesByPrefix(prefix, true);

        for (String accountRootPropName : accounts)
        {
            String accountUID = configService.user().getString(accountRootPropName);

            if (accountUID.equals(protocolProvider.getAccountID().getAccountUniqueID()))
            {
                List<String> chatRooms = configService.user()
                    .getPropertyNamesByPrefix(accountRootPropName + ".chatRooms", true);

                for (String chatRoomPropName : chatRooms)
                {
                    String chatRoomID = configService.user().getString(chatRoomPropName);

                    if (!chatRoomId.equals(chatRoomID))
                    {
                        continue;
                    }

                    return configService.user().getString(chatRoomPropName + "." + property);
                }
            }
        }

        return null;
    }

    /**
     * Returns the last chat room status, saved through the
     * <tt>ConfigurationService</tt>.
     *
     * @param protocolProvider the protocol provider, to which the chat room
     * belongs
     * @param chatRoomId the identifier of the chat room
     * @return the last chat room status, saved through the
     * <tt>ConfigurationService</tt>.
     */
    public static String getChatRoomStatus(
        ProtocolProviderService protocolProvider,
        String chatRoomId)
    {
        String prefix = "net.java.sip.communicator.impl.gui.accounts";
        List<String> accounts = configService.user().getPropertyNamesByPrefix(prefix, true);

        for (String accountRootPropName : accounts)
        {
            String accountUID = configService.user().getString(accountRootPropName);

            if (accountUID.equals(protocolProvider.getAccountID().getAccountUniqueID()))
            {
                List<String> chatRooms = configService.user()
                    .getPropertyNamesByPrefix(accountRootPropName + ".chatRooms", true);

                for (String chatRoomPropName : chatRooms)
                {
                    String chatRoomID = configService.user().getString(chatRoomPropName);

                    if (!chatRoomId.equals(chatRoomID))
                    {
                        continue;
                    }

                    return configService.user().getString(chatRoomPropName + ".lastChatRoomStatus");
                }
            }
        }

        return null;
    }

    /**
     * Returns a list of all chat room IDs for the given protocol provider,
     * saved through the <tt>ConfigurationService</tt>.
     *
     * @param protocolProvider the protocol provider, to which the chat rooms
     * belong
     * @return a list of chat room IDs.
     */
    public static List<Jid> getAllChatRoomIds(
        ProtocolProviderService protocolProvider)
    {
        List<Jid> chatRoomIds = new ArrayList<>();
        String prefix = "net.java.sip.communicator.impl.gui.accounts";
        List<String> accounts = configService.user().getPropertyNamesByPrefix(prefix, true);
        String accountUniqueID = protocolProvider.getAccountID().getAccountUniqueID();

        for (String accountRootPropName : accounts)
        {
            String accountUID = configService.user().getString(accountRootPropName);

            if (accountUID.equals(accountUniqueID))
            {
                List<String> chatRooms = configService.user()
                    .getPropertyNamesByPrefix(accountRootPropName + ".chatRooms", true);

                for (String chatRoomPropName : chatRooms)
                {
                    String chatRoomIdString = configService.user().getString(chatRoomPropName);

                    if (chatRoomIdString != null)
                    {
                        Jid chatRoomId = JidCreate.fromOrNull(chatRoomIdString);

                        if (chatRoomId != null)
                        {
                            chatRoomIds.add(chatRoomId);
                        }
                        else
                        {
                            logger.warn("Not returning non JID chat room ID: " + chatRoomIdString);
                        }
                    }
                }
            }
        }

        return chatRoomIds;
    }

    /**
     * Stores the last group <tt>status</tt> for the given <tt>groupID</tt>.
     * @param groupID the identifier of the group
     * @param isCollapsed indicates if the group is collapsed or expanded
     */
    public static void setContactListGroupCollapsed(String groupID, boolean isCollapsed)
    {
        String prefix = "net.java.sip.communicator.impl.gui.contactlist.groups";
        List<String> groups = configService.user().getPropertyNamesByPrefix(prefix, true);

        boolean isExistingGroup = false;
        for (String groupRootPropName : groups)
        {
            String storedID = configService.user().getString(groupRootPropName);

            if (storedID.equals(groupID))
            {
                configService.user().setProperty(groupRootPropName + ".isClosed", Boolean.toString(isCollapsed));
                isExistingGroup = true;
                break;
            }
        }

        if (!isExistingGroup)
        {
        String groupNodeName = "group" + System.currentTimeMillis();
            String groupPackage = prefix + "." + groupNodeName;
            configService.user().setProperty(groupPackage, groupID);
            configService.user().setProperty(groupPackage + ".isClosed", Boolean.toString(isCollapsed));
        }
    }

    /**
     * Returns <tt>true</tt> if the group given by <tt>groupID</tt> is collapsed
     * or <tt>false</tt> otherwise.
     * @param groupID the identifier of the group
     * @return <tt>true</tt> if the group given by <tt>groupID</tt> is collapsed
     * or <tt>false</tt> otherwise
     */
    public static boolean isContactListGroupCollapsed(String groupID)
    {
        String prefix = "net.java.sip.communicator.impl.gui.contactlist.groups";
        List<String> groups = configService.user().getPropertyNamesByPrefix(prefix, true);

        for (String groupRootPropName : groups)
        {
            String storedID = configService.user().getString(groupRootPropName);

            if(storedID.equals(groupID))
            {
                String status = (String) configService.global().getProperty(  groupRootPropName + ".isClosed");
                return Boolean.parseBoolean(status);
            }
        }

        return false;
    }

    /**
     * Indicates if the account configuration is disabled.
     *
     * @return <tt>true</tt> if the account manual configuration and creation
     * is disabled, otherwise return <tt>false</tt>
     */
    public static boolean isShowAccountConfig()
    {
        final String SHOW_ACCOUNT_CONFIG_PROP = "net.java.sip.communicator.impl.gui.main.configforms.SHOW_ACCOUNT_CONFIG";
        boolean defaultValue = !Boolean.parseBoolean(resources.getSettingsString("impl.gui.main.account.ACCOUNT_CONFIG_DISABLED"));
        return configService.global().getBoolean(SHOW_ACCOUNT_CONFIG_PROP, defaultValue);
    }

    /**
     * Listens for changes of the properties.
     */
    private static class ConfigurationChangeListener implements PropertyChangeListener
    {
        public void propertyChange(PropertyChangeEvent evt)
        {
            // All properties we're interested in here are Strings.
            if (!(evt.getNewValue() instanceof String))
            {
                return;
            }

            String newValue = (String) evt.getNewValue();
            String propertyName = evt.getPropertyName();

            switch (propertyName)
            {
                case "impl.gui.IS_TRANSPARENT_WINDOW_ENABLED":
                    isTransparentWindowEnabled = Boolean.parseBoolean(newValue);
                    break;
                case "impl.gui.WINDOW_TRANSPARENCY":
                    windowTransparency = Integer.parseInt(newValue);
                    break;
                case "net.java.sip.communicator.impl.gui.call.lastCallConferenceProvider":
                    lastCallConferenceProvider = findProviderFromAccountId(newValue);
                    break;
            }
        }
    }

    /**
     * Returns the package name under which we would store information for the
     * given factory.
     * @param factory the <tt>ProtocolProviderFactory</tt>, which package name
     * we're looking for
     * @return the package name under which we would store information for the
     * given factory
     */
    public static String getFactoryImplPackageName(ProtocolProviderFactory factory)
    {
        String className = factory.getClass().getName();
        return className.substring(0, className.lastIndexOf('.'));
    }

    /**
     * Returns the configured client port.
     *
     * @return the client port
     */
    public static int getClientPort()
    {
        return configService.user().getInt(ProtocolProviderFactory.PREFERRED_CLEAR_PORT_PROPERTY_NAME, 5060);
    }

    /**
     * Sets the client port.
     *
     * @param port the port to set
     */
    public static void setClientPort(int port)
    {
        configService.user().setProperty(ProtocolProviderFactory.PREFERRED_CLEAR_PORT_PROPERTY_NAME, port);
    }

    /**
     * Returns the client secure port.
     *
     * @return the client secure port
     */
    public static int getClientSecurePort()
    {
        return configService.user().getInt(ProtocolProviderFactory.PREFERRED_SECURE_PORT_PROPERTY_NAME, 5061);
    }

    /**
     * Sets the client secure port.
     *
     * @param port the port to set
     */
    public static void setClientSecurePort(int port)
    {
        configService.user().setProperty(ProtocolProviderFactory.PREFERRED_SECURE_PORT_PROPERTY_NAME, port);
    }

    /**
     * Returns the list of enabled SSL protocols.
     *
     * @return the list of enabled SSL protocols
     */
    public static String[] getEnabledSslProtocols()
    {
        String enabledSslProtocols = configService.global().getString("gov.nist.javax.sip.TLS_CLIENT_PROTOCOLS");

        if (StringUtils.isNullOrEmpty(enabledSslProtocols, true))
        {
            SSLSocket temp;
            try
            {
                temp = (SSLSocket) SSLSocketFactory.getDefault().createSocket();
                return temp.getEnabledProtocols();
            }
            catch (IOException e)
            {
                logger.error(e);
                return getAvailableSslProtocols();
            }
        }
        return enabledSslProtocols.split("(,)|(,\\s)");
    }

    /**
     * Returns the list of available SSL protocols.
     *
     * @return the list of available SSL protocols
     */
    public static String[] getAvailableSslProtocols()
    {
        SSLSocket temp;
        try
        {
            temp = (SSLSocket) SSLSocketFactory.getDefault().createSocket();
            return temp.getSupportedProtocols();
        }
        catch (IOException e)
        {
            logger.error(e);
            return new String[]{};
        }
    }

    /**
     * Sets the enables SSL protocols list.
     *
     * @param enabledProtocols the list of enabled SSL protocols to set
     */
    public static void setEnabledSslProtocols(String[] enabledProtocols)
    {
        if (enabledProtocols == null || enabledProtocols.length == 0)
        {
            configService.global().removeProperty("gov.nist.javax.sip.TLS_CLIENT_PROTOCOLS");
        }
        else
        {
            String protocols = Arrays.toString(enabledProtocols);
            configService.user().setProperty("gov.nist.javax.sip.TLS_CLIENT_PROTOCOLS", protocols.substring(1, protocols.length() - 1));
        }
    }

    /**
     * Returns <tt>true</tt> if the account associated with
     * <tt>protocolProvider</tt> has at least one video format enabled in it's
     * configuration, <tt>false</tt> otherwise.
     *
     * @return <tt>true</tt> if the account associated with
     * <tt>protocolProvider</tt> has at least one video format enabled in it's
     * configuration, <tt>false</tt> otherwise.
     */
    public static boolean hasEnabledVideoFormat(ProtocolProviderService protocolProvider)
    {
        EncodingConfiguration encodingConfiguration = UtilActivator.getMediaService().getCurrentEncodingConfiguration();
        return encodingConfiguration.hasEnabledFormat(MediaType.VIDEO);
    }

    /**
     * Return the regular expression used to validate SMS numbers.  The
     * defaults matches any string.
     *
     * @return the regex string for validating SMS numbers
     */
    public static String getSmsNumberMessagableRegex()
    {
        return configService.global().getString(SMS_NUMBER_MESSAGABLE_REGEX, "^.*$");
    }

    /**
     * Return the regular expression used to validate phone numbers.  The
     * default matches any combination of dialable digits (and matches
     * the value set in the default jitsi-overrides.properties file).
     *
     * @return the regex string for validating phone numbers
     */
    public static String getPhoneNumberCallableRegex()
    {
        return configService.global().getString(PHONE_NUMBER_CALLABLE_REGEX_PROP, "^[+#*\\d]*$");
    }

    /**
     * Return the regular expression used to validate phone numbers for
     * dial-out from a conference.
     *
     * @return the regex string for validating phone numbers for dial out
     */
    public static String getPhoneNumberDialOutRegex()
    {
        return configService.global().getString(PHONE_NUMBER_DIALOUT_REGEX_PROP, "^.*$");
    }

    /**
     * Return the regular expression of characters that should be stripped
     * from phone numbers before validation and starting a call.  The default
     * matches no characters i.e. will mean no characters are stripped.
     *
     * @return the regex string for validating phone numbers
     */
    public static String getPhoneNumberIgnoreRegex()
    {
        return configService.global().getString(PHONE_NUMBER_IGNORE_REGEX_PROP, "");
    }

    /**
     * Return true if the main UI should use an inline dial pad
     *
     * @return true if the main UI should use an inline dial pad
     */
    public static boolean getMainUiUsesInlineDialPad()
    {
        return configService.global().getBoolean(MAIN_UI_USES_INLINE_DIAL_PAD, false);
    }

    /**
     * Return true if contact favorites is supported
     *
     * @return true if contact favorites is supported
     */
    public static boolean getContactFavoritesEnabled()
    {
        return configService.global().getBoolean(CONTACT_FAVORITES_ENABLED_PROP, false);
    }

    /**
     * Store the currently selected tab in config
     *
     * @param tabIdentifier An identifier for the currently selected tab
     */
    public static void setSelectedTab(String tabIdentifier)
    {
        configService.user().setProperty(SELECTED_TAB, tabIdentifier);
    }

    /**
     * Returns the identifier of the selected tab
     *
     * @return the identifier of the selected tab
     */
    public static String getSelectedTab()
    {
        return configService.user().getString(SELECTED_TAB);
    }

    /**
     * Store the currently selected history tab in config
     *
     * @param tabIdentifier An identifier for the currently history selected tab
     */
    public static void setSelectedHistoryTab(String tabIdentifier)
    {
        configService.user().setProperty(SELECTED_HISTORY_TAB, tabIdentifier);
    }

    /**
     * Returns the identifier of the selected history tab
     *
     * @return the identifier of the selected history tab
     */
    public static String getSelectedHistoryTab()
    {
        return configService.user().getString(SELECTED_HISTORY_TAB);
    }

    /**
     * Whether the incoming call popup should show an alternative display
     * where the popup is bordered and there are no button borders
     *
     * @return Whether the alternative incoming call popup display is enabled
     */
    public static boolean isAltCallPopupStyle()
    {
        return configService.user().getBoolean(ALT_INCOMING_CALL_POPUP, false);
    }

    /**
     * Whether the incoming call popup should show an alternative display
     * where the popup is bordered and there are no button borders
     *
     * @return Whether the alternative incoming call popup display is enabled
     */
    public static boolean isMenuIconsDisabled()
    {
        return configService.global().getBoolean(GUI_MENU_ICONS_DISABLED, false);
    }

    /**
     * Returns the regex string to replace odd formatting and
     * characters in login number
     *
     * @return the regex string to replace odd formatting and
     * characters in login number
     */
    public static String getPhoneNumberLoginRegex()
    {
        return configService.global().getString(PROVISIONING_IGNORE_REGEX, "");
    }

    /**
     * @return true if the app should run in accessibility mode
     */
    public static boolean isAccessibilityMode()
    {
        // A specific accessibility mode is no longer supported, so in case
        // some existing users have turned it on, we now explicitly ignore
        // that flag
        return false;
    }

    /**
     * Set the value of the accessibility property
     *
     * @param accessibilityMode True to turn on accessibility mode
     */
    public static void setAccessibilityMode(boolean accessibilityMode)
    {
        //Accessibility mode will always be turned off while the new electron UI doesn't supported it.
        configService.user().setProperty(ACCESSIBILITY_MODE, false);
    }

    /**
     * @return true if the user is allowed to use network call history
     */
    public static boolean isNetworkCallHistoryEnabled()
    {
        return configService.user().getBoolean(NETWORK_CALL_HISTORY_ENABLED, false);
    }

    /**
     * Set the value of whether or not the user is allowed to use network call
     * history
     *
     * @param enabled true to enable network call history.
     */
    public static void setNetworkCallHistoryEnabled(boolean enabled)
    {
        configService.user().setProperty(NETWORK_CALL_HISTORY_ENABLED, enabled);
    }

    /**
     * Determines whether multi user chat is enabled. For this service to be
     * enabled, it must be permitted in the user's CoS and enabled on the IM
     * server.
     *
     * @return whether multi user chat is enabled
     */
    public static boolean isMultiUserChatEnabled()
    {
        boolean cosEnabled = configService.user().getBoolean(ClassOfServiceService.CONFIG_COS_GROUP_IM_ALLOWED, false);

        if (!cosEnabled)
        {
            return false;
        }

        // Assume the server supports multi user chat unless we are told
        // otherwise, as we may receive multi user chat packets from the server
        // immediately on start-up and we don't want to reject them just
        // because we haven't yet been able to check whether the server
        // supports multi user chat yet.  The packets will still be ignored if
        // group IM is disabled in the user's CoS.
        boolean serverEnabled = true;

        ProtocolProviderService imProvider = AccountUtils.getImProvider();
        if (imProvider != null)
        {
            OperationSetMultiUserChat opSetMuc = imProvider.getOperationSet(OperationSetMultiUserChat.class);

            if (opSetMuc != null)
            {
                serverEnabled = opSetMuc.isMultiChatSupported();
            }
            else
            {
                logger.debug("MUC enabled in CoS but Op Set is null");
            }
        }
        else
        {
            logger.debug("MUC enabled in CoS but IM provider is null");
        }

        return cosEnabled && serverEnabled;
    }

    /**
     * @return true if group contacts are supported
     */
    public static boolean groupContactsSupported()
    {
        // Currently the only actions that can be triggered from a group
        // contact are launching a new group chat and sending a direct meeting
        // invite.  Therefore, only support group contacts if at least one of
        // those actions is enabled.  Both of those also require IM, so make
        // sure IM is enabled too.
        return configService.global().getBoolean(ALLOW_GROUP_CONTACT_PROPERTY, false) &&
               isImEnabled() &&
               (isMultiUserChatEnabled() ||
                UtilActivator.getConferenceService().isFullServiceEnabled());
    }

    /**
     * @return true if the user should not be prompted to confirm leaving a
     * group chat.
     */
    public static boolean getDontAskLeaveGroupChat()
    {
        return configService.user().getBoolean("net.java.sip.communicator.impl.gui.DONT_ASK_LEAVE_GROUP_CHAT", false);
    }

    /**
     * Set that the user should not be prompted to confirm leaving a group chat
     *
     * @param dontAskLeaveGroupChat whether the user should not be prompted to
     * confirm leaving a group chat
     */
    public static void setDontAskLeaveGroupChat(boolean dontAskLeaveGroupChat)
    {
        configService.user().setProperty("net.java.sip.communicator.impl.gui.DONT_ASK_LEAVE_GROUP_CHAT", dontAskLeaveGroupChat);
    }

    /**
     * @return true if the user should not be prompted to confirm removing a
     * user from a group chat, false otherwise.
     */
    public static boolean getDontAskRemoveFromChat()
    {
        return configService.user().getBoolean(DONT_ASK_REMOVE_FROM_CHAT_PROP, false);
    }

    /**
     * Set whether the user should not be prompted to confirm removing a
     * user from a group chat.
     *
     * @param dontAskRemoveFromChat if true, set that the user should not be
     * prompted to confirm removing a user from a group chat.
     */
    public static void setDontAskRemoveFromChat(boolean dontAskRemoveFromChat)
    {
        configService.user().setProperty(DONT_ASK_REMOVE_FROM_CHAT_PROP, dontAskRemoveFromChat);
    }

    /**
     * @return the Locale for meeting invitation emails stored in
     * config, or the default system Locale if none is stored in config.
     */
    public static Locale getEmailInviteLocale()
    {
        String configString = configService.user().getString(EMAIL_INVITE_LANGUAGE);
        return (configString == null) ? Locale.getDefault() : Locale.forLanguageTag(configString);
    }

    /**
     * Store a string representation of the Locale used for meeting invitation
     * emails in user config.
     *
     * @param localeString The string to store.
     */
    public static void setEmailInviteLocale(String localeString)
    {
        configService.user().setProperty(EMAIL_INVITE_LANGUAGE, localeString);
    }

    /**
     * @return true if we should show the call rating screen at the end of a
     * call. Defaults to true if the user hasn't yet chosen.
     */
    public static boolean showCallRating()
    {
        return configService.user().getBoolean(CALL_RATING_SHOW_PROP, true);
    }

    /**
     * @return true if an error report should be sent when a poor call quality
     * rating is submitted by the user.
     */
    public static boolean sendCallRatingErrorReport()
    {
        return configService.global().getBoolean(CALL_RATING_SEND_ERROR_REPORT, false);
    }

    /**
     * @return true if Accession Meeting is allowed by the branding options.
     *         Accession Meeting is considered enabled if each of the URI
     *         schemes, vanity URL, app name and installer versions are branded
     *         - there is no single boolean branding flag for Accession Meeting.
     */
    public static boolean isAccessionMeetingEnabled()
    {
        return (getMeetingUriScheme() != null) &&
            (getDesktopUriScheme() != null) &&
            (getVanityUrl() != null) &&
            (getApplicationName() != null) &&
            (getWindowsInstallerVersion() != null) &&
            (getMacInstallerVersion() != null);
    }

    /**
     * @return true if the Accession Meeting dial-out feature is allowed by the
     *         branding options.
     */
    public static boolean isDialOutEnabled()
    {
        return isAccessionMeetingEnabled() &&
            configService.global().getBoolean("plugin.conference.accessionmeeting.SUPPORT_MEETING_DIALOUT", false);
    }

    /**
     * @return true if webinars are enabled for the user. This requires that
     * accession meeting is installed, and the user has webinars assigned.
     */
    public static boolean isWebinarsEnabled()
    {
        return isAccessionMeetingEnabled() && configService.user().getBoolean(ClassOfServiceService.CONFIG_WEBINARS_ENABLED, false);
    }

    /**
     * @return The URI scheme for URIs sent to the Accession Meeting client
     */
    public static String getMeetingUriScheme()
    {
        return configService.global().getString("plugin.conference.accessionmeeting.MEETING_URI_SCHEME");
    }

    /**
     * @return The URI scheme for URIs sent to us by the Accession Meeting
     * client
     */
    public static String getDesktopUriScheme()
    {
        return configService.global().getString("plugin.conference.accessionmeeting.DESKTOP_URI_SCHEME");
    }

    /**
     * @return The branded Accession Meeting vanity URL
     */
    public static String getVanityUrl()
    {
        return configService.global().getString("plugin.conference.accessionmeeting.VANITY_URL");
    }

    /**
     * @return The non-user-visible name of the installed Meeting client.
     * By default this is "Accession Meeting" so should not be used in
     * user-visible strings. It is instead used to find the directory where
     * the Meeting client is installed and where it writes its logs.
     */
    public static String getApplicationName()
    {
        return configService.global().getString("plugin.conference.accessionmeeting.APPLICATION_NAME");
    }

    /**
     * @return The version of the installer for the Accession Meeting client
     * for Windows
     */
    public static String getWindowsInstallerVersion()
    {
        return configService.global().getString("plugin.conference.accessionmeeting.WINDOWS_INSTALLER_VERSION");
    }

    /**
     * @return The inner installer name, required to install the Meeting client.
     */
    public static String getWindowsInnerInstallerName()
    {
        return configService.global().getString("plugin.conference.accessionmeeting.WINDOWS_INNER_INSTALLER_NAME");
    }

    /**
     * @return The name of the Accession Meeting executable for Windows
     */
    public static String getWindowsExeName()
    {
        return configService.global().getString("plugin.conference.accessionmeeting.WINDOWS_EXECUTABLE_NAME");
    }

    /**
     * @return The version of the installer for the Accession Meeting client
     * for Mac
     */
    public static String getMacInstallerVersion()
    {
        return configService.global().getString("plugin.conference.accessionmeeting.MAC_INSTALLER_VERSION");
    }

    /**
     * Update the value of the show call rating option
     *
     * @param showCallRating the new value of the call rating flag.
     */
    public static void setShowCallRating(boolean showCallRating)
    {
        configService.user().setProperty(CALL_RATING_SHOW_PROP, showCallRating);
    }

    /**
     * Sets the UUID property.
     *
     * @param uuid The UUID.
     */
    public static void setUuid(String uuid)
    {
        configService.global().setProperty(UUID_PROP, uuid);
    }

    /**
     * @return The UUID.
     */
    public static String getUuid()
    {
        return configService.global().getString(UUID_PROP);
    }

    /**
     * @return The maximum allowed edge length for an image file opened in the
     * avatar picker.
     */
    public static int getMaximumAvatarSideLength()
    {
        return configService.global().getInt("service.gui.avatar.imagepicker.MAX_AVATAR_EDGE_LENGTH", Integer.MAX_VALUE);
    }

    /**
     * @return The subscriber type in config e.g. BusinessGroupLine,
     * IndividualLine, etc. If the relevant property is missing from config,
     * returns <tt>null</tt>.
     */
    public static String getSubscriberType()
    {
        return configService.user().getString(ClassOfServiceService.CONFIG_COS_SUBSCRIBER_TYPE, null);
    }

    /**
     * @return The number of groups the subscriber is a member of (will be 0 for
     * non-BG lines). If the relevant property is missing from config, returns 0.
     */
    public static int getGroupListLength()
    {
        return configService.user().getInt(ClassOfServiceService.CONFIG_COS_GROUP_LIST_LENGTH, 0);
    }

    /**
     * @return The EAS business group name in config. If the relevant property
     * is missing from config, returns <tt>null</tt>.
     */
    public static String getEASBusinessGroup()
    {
        return configService.user().getString(ClassOfServiceService.CONFIG_COS_EAS_BG, null);
    }

    /**
     * Sets the headset response state
     * @param newItemValue The headset response state which identifies when we
     * should respond to headset button presses
     */
    public static void setHeadsetResponse(String newItemValue)
    {
        configService.user().setProperty(HEADSET_RESPONSE_SETTING, newItemValue);
    }

    /**
     * @return The headset response state, identifying when we should respond
     * to headset button presses. If the property is missing from the config,
     * returns 'ALWAYS'
     */
    public static String getHeadsetResponse()
    {
        return configService.user().getString(HEADSET_RESPONSE_SETTING, HeadsetResponseState.ALWAYS.toString());
    }

    /**
     * @return whether headset API logging is enabled. If the property is
     * missing from the config, returns false.
     */
    public static boolean getHeadsetAPILoggingEnabled()
    {
        return configService.global().getBoolean(HEADSET_LOGGING_ENABLED_SETTING, false);
    }

    /**
     * @return whether automatic update checking is disabled
     */
    public static boolean isUpdateCheckingDisabled()
    {
        return configService.user().getBoolean(DISABLE_UPDATE_CHECKING_PROP, false);
    }

    /**
     * @return whether the WebSocket server is enabled. If the property is
     * missing from the config, returns false.
     */
    public static boolean isWebSocketServerEnabled()
    {
        return configService.user().getBoolean(WEBSOCKET_SERVER_ENABLED, false);
    }

    /**
     * Sets the WebSocket server state.
     * @param webSocketServerEnabled true to enable the WebSocket server, false
     *                               to disable it.
     */
    public static void setWebsocketServerEnabled(boolean webSocketServerEnabled)
    {
        configService.user().setProperty(WEBSOCKET_SERVER_ENABLED, webSocketServerEnabled);
    }

    /**
     * @return returns the names of any connected applications or null if there
     * aren't any
     */
    public static String getWebSocketConnectedApplications()
    {
        return configService.user().getString(WEBSOCKET_CONNECTED_APPLICATIONS, null);
    }

    /**
     * Sets the name of applications currently connected via the WebSocket.
     * @param webSocketConnectedApplications whether there are any available
     *                                      WebSocket connections.
     */
    public static void setWebSocketConnectedApplications(
            String webSocketConnectedApplications)
    {
        configService.user().setProperty(WEBSOCKET_CONNECTED_APPLICATIONS, webSocketConnectedApplications);
    }

    /**
     * @return whether notifications for new WebSocket connections are enabled.
     */
    public static Boolean getWebSocketNotificationEnabled()
    {
        return configService.user().getBoolean(WEBSOCKET_NOTIFICATION_ENABLED, true);
    }

    /**
     * Set whether notifications for new WebSocket connections are enabled.
     * @param notificationEnabled whether notifications should be enabled.
     */
    public static void setWebSocketNotificationEnabled(
            boolean notificationEnabled)
    {
        configService.user().setProperty(WEBSOCKET_NOTIFICATION_ENABLED, notificationEnabled);
    }

    /**
     * @return whether the app is registered as the URL handler for the
     * sip/tel/callto protocols
     */
    public static boolean isProtocolURLHandler()
    {
        return configService.user().getBoolean(URL_PROTOCOL_HANDLER_APP_PROP, true);
    }

    /**
     * Sets whether the app is registered as the URL handler for the
     * sip/tel/callto protocols.
     * @param enabled true if the client is registered, false otherwise.
     */
    public static void setProtocolURLHandler(boolean enabled)
    {
        configService.user().setProperty(URL_PROTOCOL_HANDLER_APP_PROP, enabled);
    }

    /**
     * @return whether the app should use the native theme on Mac OS
     */
    private static boolean useNativeMacOSTheme()
    {
        return configService.global().getBoolean(USE_NATIVE_MAC_THEME_PROP, true);
    }

    /**
     * @return whether the app should use the native theme
     */
    public static boolean useNativeTheme()
    {
        // We never use native theme for Windows
        return OSUtils.IS_MAC && ConfigurationUtils.useNativeMacOSTheme();
    }

    /**
     * @return whether we should use modern tabs in the main frame
     */
    public static boolean useModernTabsInMainFrame()
    {
        return configService.global().getBoolean("net.java.sip.communicator.impl.gui.main.USE_MODERN_TABS_IN_MAIN_FRAME", false);
    }

    /**
     * @return whether we highlight the list row text, if not, then should
     * highlight the whole row
     */
    public static boolean highlightListTextWhenSelected()
    {
        return configService.global().getBoolean("net.java.sip.communicator.impl.gui.main.HIGHLIGHT_LIST_ROW_TEXT_WHEN_SELECTED", false);
    }

    /**
     * @return the user config string giving the (regexes of) numbers that require location information
     */
    public static String numbersRequiringLocationInfo()
    {
        return configService.user().getString(
            "net.java.sip.communicator.service.commportal.emergencylocation.NUMBERS_REQUIRING_LOCATION_INFO",
            "");
    }

    /**
     * Get the OS version info from the system and save in global config. We
     * include special logic for Windows here since the version reported by the
     * OS can be misleading.
     */
    public static void calculateAndStoreOsVersion()
    {
        String osName = null;
        String osVersion = null;

        if (OSUtils.IS_WINDOWS)
        {
            Kernel32 kernel = Kernel32.INSTANCE;
            WinNT.OSVERSIONINFOEX osvi = new WinNT.OSVERSIONINFOEX();
            if (kernel.GetVersionEx(osvi))
            {
                int majorVersion = osvi.getMajor();
                int minorVersion = osvi.getMinor();
                int buildNumber = osvi.getBuildNumber();

                if (majorVersion == 10 && buildNumber >= MIN_WINDOWS_11_BUILD_NUMBER)
                {
                    // This is Windows 11- the OS inaccurately reports a major
                    // version of 10. GetVersionEx doesn't work for Windows 10
                    // however, so only use these values for Windows 11.
                    osName = "Windows 11";
                    osVersion = String.format("%d.%d.%d", majorVersion, minorVersion, buildNumber);
                }
            }
        }

        if (osVersion == null)
        {
            // If we are not on Windows 11 (including if we are on Mac)
            osName = System.getProperty("os.name");
            osVersion = System.getProperty("os.version");
        }

        configService.global().setProperty(OS_NAME_PROP, osName);
        configService.global().setProperty(OS_VERSION_PROP, osVersion);
    }
}
