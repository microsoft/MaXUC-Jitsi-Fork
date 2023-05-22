/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.presence;

import static org.jitsi.util.Hasher.logHasher;

import java.beans.*;
import java.util.*;

import org.jitsi.service.configuration.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.analytics.AnalyticsService;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.diagnostics.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.service.threading.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

/**
 * Global statuses service impl. Gives access to the outside for some
 * methods to change the global status and individual protocol provider
 * status. Used to implement global status menu with list of all account
 * statuses.
 * @author Damian Minkov
 */
public class GlobalStatusServiceImpl
    implements GlobalStatusService, RegistrationStateChangeListener, StateDumper
{
    private static final Logger logger = Logger.getLogger(GlobalStatusServiceImpl.class);

    /**
     * Initial period of time in which our status is always 'online' regardless
     * of the state of our statusProviders. This allows the providers a chance
     * to log in without showing connection warnings to the user.
     */
    private static final int STARTUP_GRACE_PERIOD_MS = 20000;

    /**
     * Place in config where we store if we should be using Outlook for the meeting status
     */
    private static final String USE_OUTLOOK_FOR_PRESENCE =
                      "net.java.sip.communicator.protocol.presence.USE_OUTLOOK";

    /**
     * Place in config where we store the busy flag
     */
    private static final String CFG_IS_BUSY =
                          "net.java.sip.communicator.protocol.presence.IS_BUSY";

    /**
     * Place in config where we store the Custom Status
     */
    private static final String CFG_CUSTOM_STATUS =
                    "net.java.sip.communicator.protocol.presence.CUSTOM_STATUS";

    /**
     * Configuration service
     */
    private final ConfigurationService configService =
                                         GuiActivator.getConfigurationService();

    /**
     * The set of objects that want to be informed of changes to the global
     * status
     */
    private final Set<GlobalStatusChangeListener> statusChangeListeners =
            new HashSet<>();

    /**
     * Set to true if the user has enabled 'do not disturb'.
     */
    private boolean mIsDnd = false;

    /**
     * Set to true if the user is on the phone in this client.
     */
    private boolean isLocallyOnThePhone = false;

    /**
     * Set to true if the user is on the phone elsewhere.
     */
    private boolean isRemotelyOnThePhone = false;

    /**
     * Set to true if the user is on the phone in a conference.
     */
    private boolean isInConference = false;

    /**
     * Set to true if the user is forwarding calls.
     */
    private boolean isForwarding = false;

    /**
     * Set to true if the user is in a meeting
     */
    private boolean isInMeeting = false;

    /**
     * Set to true if the user has selected a state of "Busy"
     */
    private boolean isBusy = configService.user().getBoolean(CFG_IS_BUSY, false);

    /**
     * Set to true if the user is away
     */
    private boolean isAway = false;

    /**
     * The current global status
     */
    private GlobalStatusEnum mGlobalStatus;

    /**
     * The current custom status
     */
    private String mCustomStatus = "";

    /**
     * True if we are using Outlook for meeting state
     */
    private boolean meetingStateEnabled = GuiActivator.getConfigurationService()
                             .user().getBoolean(USE_OUTLOOK_FOR_PRESENCE, true);

    /**
     * The time at which this service was started; used to determine whether we
     * are in the initial grace period where we appear 'online' regardless of
     * the state of the statusProviders.
     */
    private final long mStartTime;

    /**
     * The Unknown status. Indicate that we don't know if the user is present
     * or not.
     */
    private static final String UNKNOWN =
        GuiActivator.getResources().getI18NString("service.protocol.status.UNKNOWN");

    /**
     * Set of the providers that have been registered
     */
    private final Set<ProtocolProviderService> statusProviders =
            new HashSet<>();

    public GlobalStatusServiceImpl()
    {
        configService.user().addPropertyChangeListener(
            USE_OUTLOOK_FOR_PRESENCE,
            new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                meetingStateEnabled = Boolean.parseBoolean(evt.getNewValue().toString());
                publishStatus();
            }
        });

        initialiseCustomStatusOnStartup();

        // Calculates and publishes the status
        publishStatus();

        logger.debug("Start up global status is: " + mGlobalStatus);
        logger.debug("Start up custom status is: " + mCustomStatus);

        DiagnosticsServiceRegistrar.registerStateDumper(this, GuiActivator.bundleContext);

        // Once the initial grace period has expired, we need to re-check our
        // status in case one or more of our providers are still offline.
        mStartTime = System.currentTimeMillis();
        GuiActivator.getThreadingService().schedule("SetInitialStatus",
            new CancellableRunnable()
            {
                @Override
                public void run()
                {
                    // When we set custom status on start-up, if the IM account is null, we'll not
                    // set the custom status. There's currently no listening to see if that changes
                    // later on, so we just try again here. The "proper" fix is to listen for
                    // changes to the protocol provider for IM coming online, then setting custom
                    // status, but that was judged too risky at the point in the release that we
                    // made this fix.
                    initialiseCustomStatusOnStartup();

                    publishStatus();
                }
            },
            STARTUP_GRACE_PERIOD_MS);
    }

    /**
     * Stop this component
     */
    public void stop()
    {
        synchronized (statusChangeListeners)
        {
            statusChangeListeners.clear();
        }

        synchronized (statusProviders)
        {
            statusProviders.clear();
        }
    }

    /**
     * Returns the last status that was stored in the configuration for the
     * given protocol provider.
     *
     * @param protocolProvider the protocol provider
     * @return the last status that was stored in the configuration for the
     *         given protocol provider
     */
    public PresenceStatus getLastPresenceStatus(
        ProtocolProviderService protocolProvider)
    {
        String lastStatus = getLastStatusString(protocolProvider);

        if (lastStatus != null)
        {
            OperationSetPresence presence
                = protocolProvider.getOperationSet(OperationSetPresence.class);

            if (presence == null)
                return null;

            Iterator<PresenceStatus> i = presence.getSupportedStatusSet();
            PresenceStatus status;

            while (i.hasNext())
            {
                status = i.next();
                if (status.getStatusName().equals(lastStatus))
                    return status;
            }
        }
        return null;
    }

    /**
     * Returns the last contact status saved in the configuration.
     *
     * @param protocolProvider the protocol provider to which the status
     *            corresponds
     * @return the last contact status saved in the configuration.
     */
    public String getLastStatusString(ProtocolProviderService protocolProvider)
    {
        // find the last contact status saved in the configuration.
        String lastStatus = null;

        ConfigurationService configService
            = GuiActivator.getConfigurationService();
        String prefix = "net.java.sip.communicator.impl.gui.accounts";
        List<String> accounts
            = configService.user().getPropertyNamesByPrefix(prefix, true);
        String protocolProviderAccountUID
            = protocolProvider.getAccountID().getAccountUniqueID();

        for (String accountRootPropName : accounts)
        {
            String accountUID = configService.user().getString(accountRootPropName);

            if ((accountUID != null) && accountUID.equals(protocolProviderAccountUID))
            {
                lastStatus =
                    configService.global().getString(accountRootPropName
                        + ".lastAccountStatus");

                if (lastStatus != null)
                    break;
            }
        }

        return lastStatus;
    }

    /**
     * Publish the global status.  Both to the listeners inside the app, and to
     * providers which inform outside the app
     */
    private synchronized void publishStatus()
    {
        mGlobalStatus = calculateStatus();

        logger.info("Publishing status " + mGlobalStatus);

        // Update the providers that publish our status:
        publishStatusToProviders();

        // Inform all the listeners that the status has changed:
        Set<GlobalStatusChangeListener> listeners;
        synchronized (statusChangeListeners)
        {
            listeners = new HashSet<>(statusChangeListeners);
        }

        for (GlobalStatusChangeListener listener : listeners)
        {
            listener.onStatusChanged();
        }
    }

    /**
     * Publish the global status to the providers that export their status.
     */
    private void publishStatusToProviders()
    {
        ProtocolProviderService imProvider = null;

        // Only need to publish to Jabber
        synchronized (statusProviders)
        {
            for (ProtocolProviderService pps : statusProviders)
            {
                if (pps.getProtocolName().equals("Jabber"))
                {
                    imProvider = pps;
                    break;
                }
            }
        }

        if (imProvider == null || !imProvider.isRegistered())
        {
            return;
        }

        GlobalStatusEnum globalStatus = getGlobalStatus();

        if (globalStatus == GlobalStatusEnum.DO_NOT_DISTURB)
        {
            publishStatus(imProvider,
                          PresenceStatus.ONLINE_THRESHOLD,
                          PresenceStatus.DND_THRESHOLD);
        }
        else if (globalStatus == GlobalStatusEnum.BUSY)
        {
            publishStatus(imProvider,
                          PresenceStatus.DND_THRESHOLD,
                          PresenceStatus.BUSY_THRESHOLD);
        }
        else if (globalStatus == GlobalStatusEnum.ON_THE_PHONE)
        {
            publishStatus(imProvider,
                          PresenceStatus.BUSY_THRESHOLD,
                          PresenceStatus.ON_THE_PHONE_THRESHOLD);
        }
        else if (globalStatus == GlobalStatusEnum.IN_A_MEETING)
        {
            publishStatus(imProvider,
                          PresenceStatus.ON_THE_PHONE_THRESHOLD,
                          PresenceStatus.MEETING_THRESHOLD);
        }
        else if (globalStatus == GlobalStatusEnum.AWAY)
        {
            publishStatus(imProvider,
                          PresenceStatus.MEETING_THRESHOLD,
                          PresenceStatus.AWAY_THRESHOLD);
        }
        else
        {
            publishStatus(imProvider,
                          PresenceStatus.AVAILABLE_THRESHOLD,
                          PresenceStatus.EAGER_TO_COMMUNICATE_THRESHOLD);
        }
    }

    /**
     * Add a provider to the list of providers that affect the status
     *
     * @param protocolProvider the provider to add
     */
    public void addProvider(ProtocolProviderService protocolProvider)
    {
        String name = protocolProvider.getProtocolDisplayName();
        if (!"Jabber".equals(name) && !"SIP".equals(name))
        {
            // We only care about the SIP and Jabber providers
            return;
        }

        if (!ConfigurationUtils.isPhoneServiceEnabled() && "SIP".equals(name))
        {
            logger.info("We only care about the SIP provider if phone service is enabled.");
            return;
        }

        logger.info("Adding provider " + protocolProvider);
        boolean changed;

        synchronized (statusProviders)
        {
            changed = statusProviders.add(protocolProvider);
            protocolProvider.addRegistrationStateChangeListener(this);
        }

        if (changed)
        {
            publishStatus();
        }
    }

    /**
     * Remove a provider from the list of providers that affect the status
     *
     * @param protocolProvider the provider to remove
     */
    public void removeProvider(ProtocolProviderService protocolProvider)
    {
        logger.info("Removing provider " + protocolProvider);
        boolean changed;

        synchronized (statusProviders)
        {
            changed = statusProviders.remove(protocolProvider);
            protocolProvider.removeRegistrationStateChangeListener(this);
        }

        if (changed)
        {
            publishStatus();
        }
    }

    @Override
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        // One of our providers has changed - probably means we need to update
        // the status:
        publishStatus();
    }

    public void setDoNotDisturbEnabled(boolean enabled)
    {
        if (mIsDnd != enabled)
        {
            logger.debug("Setting DND enabled to " + enabled);
            mIsDnd = enabled;

            publishStatus();
        }
    }

    @Override
    public boolean isDoNotDisturb()
    {
        return mIsDnd;
    }

    @Override
    public boolean isOffline() {
        return getGlobalStatus() == GlobalStatusEnum.OFFLINE;
    }

    @Override
    public void setIsForwarding(boolean isForwarding)
    {
        if (this.isForwarding != isForwarding)
        {
            logger.debug("Setting 'Forwarding' to " + isForwarding);

            this.isForwarding = isForwarding;

            publishStatus();
        }
    }

    @Override
    public boolean isForwarding()
    {
        return isForwarding;
    }

    public void setInMeeting(boolean inMeeting)
    {
        if (isInMeeting != inMeeting)
        {
            logger.debug("Setting in meeting to " + inMeeting);
            isInMeeting = inMeeting;
            publishStatus();
        }
    }

    public boolean isInMeeting()
    {
        return isInMeeting && meetingStateEnabled;
    }

    public void setIsInConference(boolean isInConference)
    {
        if (this.isInConference != isInConference)
        {
            logger.debug("Setting 'isInConference' to " + isInConference);

            this.isInConference = isInConference;

            publishStatus();
        }
    }

    public boolean isInConference()
    {
        return this.isInConference;
    }

    public void setIsOnThePhone(boolean onThePhone, boolean isLocal)
    {
        if ((isLocallyOnThePhone != onThePhone) ||
            (isRemotelyOnThePhone != onThePhone))
        {
            logger.debug("Setting 'isOnThePhone' to " + onThePhone +
                         " Is local = " + isLocal);

            if (isLocal)
            {
                isLocallyOnThePhone = onThePhone;
            }
            else
            {
                isRemotelyOnThePhone = onThePhone;
            }

            publishStatus();
        }
    }

    @Override
    public boolean isOnThePhone()
    {
        return isLocallyOnThePhone ||
               isRemotelyOnThePhone ||
               isInConference;
    }

    @Override
    public boolean isLocallyOnThePhone()
    {
        return isLocallyOnThePhone;
    }

    @Override
    public void setBusy(boolean busy)
    {
        if (isBusy != busy)
        {
            logger.debug("Setting busy to " + busy);
            isBusy = busy;
            configService.user().setProperty(CFG_IS_BUSY, isBusy);

            publishStatus();
        }
    }

    @Override
    public boolean isBusy()
    {
        return isBusy;
    }

    @Override
    public void setAway(boolean away)
    {
        if (isAway != away)
        {
            logger.debug("Setting away to " + away);
            isAway = away;
            publishStatus();
        }
    }

    private synchronized GlobalStatusEnum calculateStatus()
    {
        GlobalStatusEnum newStatus = GlobalStatusEnum.ONLINE;

        // If we don't have an IM account, we can't have a custom status
        if (AccountUtils.getImAccount() == null)
        {
            logger.info("No IM account currently available - set custom status to empty string.");
            mCustomStatus = "";
        }

        // Priority order is
        // OFFLINE
        // DND
        // BUSY
        // MEETING
        // ON THE PHONE
        // AWAY
        // ONLINE

        boolean haveImAccount = false;

        // So, first look at the providers to see if one of those is offline.
        // If one is then we should return offline immediately since that takes
        // priority.
        synchronized (statusProviders)
        {
            for (ProtocolProviderService provider : statusProviders)
            {
                if (!provider.isRegistered())
                {
                    logger.info("Provider is offline " + provider.getProtocolName());
                    newStatus = GlobalStatusEnum.OFFLINE;
                    break;
                }

                if (provider.getProtocolName().equals("Jabber"))
                    haveImAccount = true;
            }
        }

        // Appear as 'online' in the first few seconds of startup regardless of
        // the state of our statusProviders; this allows the app time to settle
        // without displaying connectivity warnings to the user.
        long graceTimeLeft = STARTUP_GRACE_PERIOD_MS -
                                      (System.currentTimeMillis() - mStartTime);
        if (isOffline() && graceTimeLeft > 0)
        {
            logger.debug("Ignore offline status during initial startup; " +
                         " return ONLINE. Grace period ends in " +
                         graceTimeLeft + "ms");
            newStatus = GlobalStatusEnum.ONLINE;
        }

        // Next see if there is a disabled IM account.  (A disabled account
        // doesn't have a protocol provider associated with it).
        AccountID account = AccountUtils.getImAccount();
        if (account != null && !account.isEnabled())
        {
            newStatus = GlobalStatusEnum.OFFLINE;
        }

        // Next up, DND, BUSY, IN_A_MEETING, ON_THE_PHONE and AWAY
        // Note that in a meeting requires us to be listening to meetings state
        // from Outlook.
        // Note also that if we don't have an IM account, we can only be DND or
        // Available
        if (newStatus != GlobalStatusEnum.OFFLINE)
        {
            if (mIsDnd)
                newStatus = GlobalStatusEnum.DO_NOT_DISTURB;
            else if (!haveImAccount)
                newStatus = GlobalStatusEnum.ONLINE;
            else if (isBusy)
                newStatus = GlobalStatusEnum.BUSY;
            else if (isOnThePhone())
                newStatus = GlobalStatusEnum.ON_THE_PHONE;
            else if (isInMeeting())
                newStatus = GlobalStatusEnum.IN_A_MEETING;
            else if (isAway)
                newStatus = GlobalStatusEnum.AWAY;
        }

        return newStatus;
    }

    /**
     * Publish present status. We search for the highest value in the
     * given interval.
     *
     * @param protocolProvider the protocol provider to which we
     * change the status.
     * @param floorStatusValue the min status value.
     * @param ceilStatusValue the max status value.
     */
    private void publishStatus(
            ProtocolProviderService protocolProvider,
            int floorStatusValue, int ceilStatusValue)
    {
        if (!protocolProvider.isRegistered())
            return;

        OperationSetPresence presence
            = protocolProvider
                .getOperationSet(OperationSetPresence.class);

        Iterator<PresenceStatus> statusSet;

        if (presence == null)
        {
            // This protocol provider doesn't support presence, but might still
            // affect the global status so check against the GlobalStatus
            // supported status set.
            statusSet = GlobalStatusEnum.supportedStatusSet.iterator();
        }
        else
        {
            // This protocol provider supports presence, determine the global
            // status based on its supported status set.
            statusSet = presence.getSupportedStatusSet();
        }

        PresenceStatus status = null;

        while (statusSet.hasNext())
        {
            PresenceStatus currentStatus = statusSet.next();

            // If we don't have a matching status yet, check to see if this
            // value from the status set fits in the range.
            if (status == null
                && currentStatus.getStatus() < ceilStatusValue
                && currentStatus.getStatus() >= floorStatusValue)
            {
                status = currentStatus;
            }

            // If we do have a status already found, but this one still fits and
            // has a higher priority then use it instead.
            if (status != null)
            {
                if (currentStatus.getStatus() < ceilStatusValue
                    && currentStatus.getStatus() >= floorStatusValue
                    && currentStatus.getStatus() > status.getStatus())
                {
                    status = currentStatus;
                }
            }
        }

        if (status != null)
        {
            if (presence != null)
            {
                //Only publish the presence if the protocol provider supports it
                new PublishPresenceStatusThread(
                    protocolProvider,
                    presence,
                    status,
                    mCustomStatus).start();

                this.saveStatusInformation(protocolProvider,
                                           status.getStatusName());
            }
        }
    }

    /**
     * Saves the last status for all accounts. This information is used
     * on login. Each time user logs in he's logged with the same status
     * as he was the last time before closing the application.
     * @param protocolProvider the protocol provider to save status information
     * for
     * @param statusName the name of the status to save
     */
    private void saveStatusInformation(
            ProtocolProviderService protocolProvider,
            String statusName)
    {
        logger.debug("Saving status of " + logHasher(statusName));
        ConfigurationService configService
            = GuiActivator.getConfigurationService();

        String prefix = "net.java.sip.communicator.impl.gui.accounts";

        List<String> accounts = configService.user()
                .getPropertyNamesByPrefix(prefix, true);

        boolean savedAccount = false;

        for (String accountRootPropName : accounts) {
            String accountUID
                = configService.user().getString(accountRootPropName);

            if(accountUID.equals(protocolProvider
                    .getAccountID().getAccountUniqueID())) {

                configService.user().setProperty(
                        accountRootPropName + ".lastAccountStatus",
                        statusName);

                savedAccount = true;
            }
        }

        if(!savedAccount)
        {
            String accNodeName
                = "acc" + System.currentTimeMillis();

            String accountPackage
                = "net.java.sip.communicator.impl.gui.accounts."
                        + accNodeName;

            configService.global().setProperty(accountPackage,
                protocolProvider.getAccountID().getAccountUniqueID());

            configService.global().setProperty(
                accountPackage + ".lastAccountStatus",
                statusName);
        }
    }

    /**
     * Publishes the given status to the given presence operation set.
     */
    private static class PublishPresenceStatusThread
        extends Thread
    {
        private ProtocolProviderService protocolProvider;

        private PresenceStatus status;

        private OperationSetPresence presence;

        private String customMood;

        /**
         * Publishes the given <tt>status</tt> through the given
         * <tt>presence</tt> operation set.
         * @param protocolProvider the protocol provider
         * @param presence the operation set through which we publish the status
         * @param status the status to publish
         * @param customMood a custom mood string, as specified by the user
         * (may be null or empty).
         */
        public PublishPresenceStatusThread(
                                    ProtocolProviderService protocolProvider,
                                    OperationSetPresence presence,
                                    PresenceStatus status,
                                    String customMood)
        {
            this.protocolProvider = protocolProvider;
            this.presence = presence;
            this.status = status;
            this.customMood = customMood;
        }

        @Override
        public void run()
        {
            try
            {
                boolean customMoodSet = customMood != null && !customMood.isEmpty();
                logger.debug("Publishing presence as: " + status +
                             ", custom mood set: " + customMoodSet);
                presence.publishPresenceStatus(status, customMood);
            }
            catch (IllegalArgumentException | IllegalStateException e1)
            {
                logger.error("Error - changing status", e1);
            }
            catch (OperationFailedException e1)
            {
                if (e1.getErrorCode()
                    == OperationFailedException.GENERAL_ERROR)
                {
                    String msgText =
                        GuiActivator.getResources().getI18NString(
                            "service.gui.STATUS_CHANGE_GENERAL_ERROR",
                            new String[]{
                                protocolProvider.getAccountID().getUserID(),
                                protocolProvider.getAccountID().getService()});

                    new ErrorDialog(GuiActivator.getResources().getI18NString(
                        "service.gui.GENERAL_ERROR"), msgText).showDialog();
                }
                else if (e1
                    .getErrorCode() == OperationFailedException.NETWORK_FAILURE
                    || e1
                        .getErrorCode() == OperationFailedException.PROVIDER_NOT_REGISTERED)
                {
                    String msgText =
                        GuiActivator.getResources().getI18NString(
                            "service.gui.STATUS_CHANGE_NETWORK_FAILURE",
                            new String[]{
                                protocolProvider.getAccountID().getUserID(),
                                protocolProvider.getAccountID().getService()});

                    new ErrorDialog(GuiActivator.getResources().getI18NString(
                        "service.gui.NETWORK_FAILURE"), msgText)
                        .showDialog();
                }
                logger.error("Error - changing status", e1);
            }
        }
    }

    /**
     * Upon each status notification this method checks if the contact is our
     * own line, and updates the global status if so.
     *
     * @param event the ContactPresenceStatusChangeEvent describing the status
     * change.
     */
    public void contactPresenceStatusChanged(
        ContactPresenceStatusChangeEvent event)
    {
        ProtocolProviderService pps = event.getSourceProvider();

        if (event.getNewStatus().getStatusName().equals(UNKNOWN))
        {
            return;
        }

        // If this is not a SIP contact then we don't care
        if (!pps.getProtocolName().equals(ProtocolNames.SIP))
        {
            return;
        }

        Contact sourceContact = event.getSourceContact();

        String persistentData = sourceContact.getPersistentData();
        if (persistentData == null)
        {
            // No persistent data so can't be our own contact
            return;
        }

        // Check whether this contact is our own line by looking in the
        // persistent data.
        if (persistentData.contains(MetaContact.IS_OWN_LINE + "=" + true))
        {
            boolean setOnThePhone = (event.getNewStatus().getStatus() ==
                                     PresenceStatus.ON_THE_PHONE_THRESHOLD);

            logger.debug("Received LSM notification for our own line. OnThePhone=" + setOnThePhone);

            setIsOnThePhone(setOnThePhone, false);
        }
    }

    @Override
    public void addStatusChangeListener(GlobalStatusChangeListener listener)
    {
        synchronized (statusChangeListeners)
        {
            statusChangeListeners.add(listener);
        }
    }

    @Override
    public void removeStatusChangeListener(GlobalStatusChangeListener listener)
    {
        synchronized (statusChangeListeners)
        {
            statusChangeListeners.remove(listener);
        }
    }

    @Override
    public String getStatusMessage()
    {
        String status = GlobalStatusEnum.getI18NStatusName(mGlobalStatus);

        // Only combine with the custom status if there is one
        if (!getCustomStatus().isEmpty())
        {
            status = GuiActivator.getResources().getI18NString("service.gui.CUSTOM_STATUS",
                                                               new String[] {status, mCustomStatus});
        }

        return status;
    }

    @Override
    public GlobalStatusEnum getGlobalStatus()
    {
        return mGlobalStatus;
    }

    @Override
    public String getCustomStatus()
    {
        // If we are offline then we shouldn't display the custom status
        return isOffline() ? "" : mCustomStatus;
    }

    @Override
    public synchronized void setCustomStatus(String customStatus)
    {
        // Truncate the status to avoid super long statuses
        customStatus = customStatus.substring(0, Math.min(100, customStatus.length()));

        logger.debug("Custom status changed");

        if (!mCustomStatus.equals(customStatus))
        {
            // If we're setting the custom status to a new non-empty string, log an analytic
            if (!customStatus.equals(""))
            {
                AnalyticsService analytics = GuiActivator.getAnalyticsService();
                analytics.onEventWithIncrementingCount(AnalyticsEventType.SET_CUSTOM_STATUS, new ArrayList<>());
            }

            mCustomStatus = customStatus;

            // Update the value in config
            configService.user().setProperty(CFG_CUSTOM_STATUS, mCustomStatus);

            publishStatus();
        }
    }

    /**
     *  Pull the custom status value from config on startup
     */
    private void initialiseCustomStatusOnStartup()
    {
        String customStatus = configService.user().getString(CFG_CUSTOM_STATUS, "");

        // Only set it if our mCustomStatus is not yet initialised, and we have a value to set it to.
        // There's no point setting it if it's already been set, and it prevents an edge condition
        // where the user has already set a new custom status during the 20s grace period, but it
        // hasn't yet synced to the config service.
        if (mCustomStatus.equals("") && !customStatus.equals(""))
        {
            logger.info("Setting custom status on startup to " + customStatus);
            mCustomStatus = customStatus;

            publishStatus();
        }
    }

    @Override
    public String getStateDumpName()
    {
        return "GlobalStatusService";
    }

    @Override
    public String getState()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Status           ").append(logHasher(getStatusMessage())).append("\n")
               .append("Started          ").append(mStartTime).append("\n")
               .append("Global Status    ").append(getGlobalStatus().getStatusName()).append("\n")
               .append("Custom Status    ").append(logHasher(getCustomStatus())).append("\n")
               .append("dnd?             ").append(mIsDnd).append("\n")
               .append("local on phone?  ").append(isLocallyOnThePhone).append("\n")
               .append("remote on phone? ").append(isRemotelyOnThePhone).append("\n")
               .append("conf on phone?   ").append(isInConference).append("\n")
               .append("forwarding?      ").append(isForwarding).append("\n")
               .append("in meeting?      ").append(isInMeeting).append("\n")
               .append("meeting enabled? ").append(meetingStateEnabled).append("\n")
               .append("is busy?         ").append(isBusy).append("\n")
               .append("is away?         ").append(isAway);

        return builder.toString();
    }
}
