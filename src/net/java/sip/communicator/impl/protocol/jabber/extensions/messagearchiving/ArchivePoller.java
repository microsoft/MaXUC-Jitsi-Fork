// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber.extensions.messagearchiving;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.archive.ArchiveQueryPacket;
import org.jxmpp.jid.Jid;

import net.java.sip.communicator.impl.gui.main.MainFrameTabComponent;
import net.java.sip.communicator.impl.protocol.jabber.JabberActivator;
import net.java.sip.communicator.impl.protocol.jabber.ProtocolProviderServiceJabberImpl;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeListener;
import net.java.sip.communicator.service.threading.CancellableRunnable;
import net.java.sip.communicator.service.threading.ThreadingService;
import net.java.sip.communicator.util.Logger;
import org.jitsi.service.configuration.ConfigurationService;

/**
 * The archive poller - requests archive messages from the server at certain
 * times.
 *
 * The times are as follows:
 * 1. When the Jabber account becomes unregistered - happens automatically as a
 *    new Poller is created when this happens
 * 2. When the user opens the recents tab - via a config listener
 * 3. Every 5 minutes, but only if the user is logged in on multiple accounts
 */
public class ArchivePoller implements RegistrationStateChangeListener,
                                      PropertyChangeListener,
                                      RosterListener
{
    private static final Logger sLog = Logger.getLogger(ArchivePoller.class);

    /**
     * Maximum number of results to query for.
     */
    public static final int MAX_RESULTS = 10;

    /**
     * The place in config where the currently selected tab is stored
     */
    private static final String SELECTED_TAB_NAME =
                           "net.java.sip.communicator.service.gui.SELECTED_TAB";

    /**
     * How often to request the archive - 5 minutes
     */
    private static final int ARCHIVE_REQUEST_TIME = 5 * 60 * 1000;

    /**
     * Config service
     */
    private final ConfigurationService mConfigurationService;

    /**
     * Threading service
     */
    private final ThreadingService mThreadingService;

    /**
     * A set of JIDs from other clients logged in with the same account. Used to
     * see if the user is logged in on multiple places.
     */
    private final HashSet<Jid> mClientJids = new HashSet<>();

    /**
     * Jabber provider that this object queries the archive for
     */
    private final ProtocolProviderServiceJabberImpl mJabberProvider;

    /**
     * Archive receiver used to receive archive messages
     */
    private final ArchiveReceiver mArchiveReceiver;

    /**
     * Boolean indicating whether or not we are currently requesting the archive
     * Used to prevent multiple concurrent requests to get the archive
     */
    private final AtomicBoolean mRequestingArchive = new AtomicBoolean();

    /**
     * Boolean indicating whether or not our first archive request has
     * completed.
     */
    private final AtomicBoolean mFirstRequestComplete = new AtomicBoolean();

    /**
     * Task run to request the archive every ARCHIVE_REQUEST_TIME when multiple
     * clients are logged in
     */
    private CancellableRunnable mArchiveGetterTask;

    /**
     * Recovery task run to reset the "requesting archive" flag in case a request
     * to get the archive fails for some reason
     */
    private CancellableRunnable mGettingArchiveResetTask;

    /**
     * Jabber roster for the provider
     */
    private Roster mRoster;

    public ArchivePoller(ProtocolProviderServiceJabberImpl jabberProvider)
    {
        sLog.debug("Created archive poller " + this);
        mArchiveReceiver = new ArchiveReceiver(jabberProvider, this);
        mConfigurationService = JabberActivator.getConfigurationService();
        mThreadingService = JabberActivator.getThreadingService();
        mJabberProvider = jabberProvider;

        // Listen for registration state changes to the Jabber provider.  We can
        // only send a request if the account is registered.
        mJabberProvider.addRegistrationStateChangeListener(this);
        if (mJabberProvider.isRegistered())
        {
            requestFirstArchive();

            // Listen for roster changes so that we can see if the user logs
            // in on another device
            listenForOtherClients();
        }

        // Listen for the recents tab being selected
        mConfigurationService.user().addPropertyChangeListener(SELECTED_TAB_NAME, this);
    }

    /**
     * Called in order to request the archive for the first time
     */
    private void requestFirstArchive()
    {
        // The first request should be delayed by 5 seconds, in order to allow
        // any offline messages to be processed first.
        // Set requesting as true, so that no other request sneaks in
        mRequestingArchive.set(true);
        mFirstRequestComplete.set(false);
        mThreadingService.schedule("ArchivePoller.initialRequest",
                                   new CancellableRunnable()
        {
            @Override
            public void run()
            {
                // Unset the requesting archive flag, as otherwise the request
                // will fail
                mRequestingArchive.set(false);
                requestArchive();
            }
        }, 5000);
    }

    /**
     * Get the XMPP roster and use it to see if the user is logged in on multiple
     * clients. Add a listener to the roster so that this information is updated.
     */
    private void listenForOtherClients()
    {
        // No need to process the roster if we already have.
        if (mRoster != null)
            return;

        XMPPConnection connection = mJabberProvider.getConnection();
        mRoster = Roster.getInstanceFor(connection);

        // Make sure that the list of JIDs includes our account - we won't be
        // reliably told of our own presence
        Jid ourJID = mJabberProvider.getOurJid();

        if (ourJID != null)
        {
            synchronized (mClientJids)
            {
                mClientJids.add(ourJID);
                sLog.info("Adding our JID to client JIDs " + ourJID + ", JIDs now " + mClientJids);
            }
        }

        // Add listener so that we can see if the user logs in on another device
        // The listener will be informed of all existing presences.
        mRoster.addRosterListener(this);
    }

    /**
     * Called when part of a query has completed.  I.e. a request for part of the
     * archive has completed, but there are still more parts to request
     */
    public void onQueryPartComplete()
    {
        // This request succeeded, thus cancel the reset task
        if (mGettingArchiveResetTask != null)
            mGettingArchiveResetTask.cancel();

        // Request the next part of the archive
        sLog.debug("Request next part of archive");
        doRequestArchive();
    }

    /**
     * Called when a query is complete
     */
    public void onQueryComplete()
    {
        sLog.debug("Query complete");
        mRequestingArchive.set(false);

        if (mGettingArchiveResetTask != null)
            mGettingArchiveResetTask.cancel();

        // If this query is the first request we've done since connecting, we
        // need to let the chat room manager know that it is complete so that
        // it knows it has finished receiving chat room join and leave
        // timestamps from join and leave messages in the archive.  If this is
        // not the first request, the chat room manager will already have
        // finished this processing so we don't need to do anything.
        if (!mFirstRequestComplete.get())
        {
            sLog.debug("Setting chat room manager archive query complete to true");
            mFirstRequestComplete.set(true);
            mJabberProvider.getChatRoomManager().setArchiveQueryComplete(true);
        }
    }

    /**
     * This is the most important method of this class.  It requests the archive
     * since the last archive ID.
     */
    private void requestArchive()
    {
        if (mRequestingArchive.get())
        {
            sLog.debug("Ignoring request to get archive - "
                                            + "request is already in progress");
        }
        else
        {
            sLog.debug("Requesting archive");
            mRequestingArchive.set(true);
            doRequestArchive();
        }
    }

    /**
     * Actually request the archive, regardless of whether or not a query is in
     * progress
     */
    private void doRequestArchive()
    {
        // If (for some reason) we fail to complete a request, then we will be
        // in a situation where all further requests to get the archive will
        // fail (due to the requesting archive flag).  Thus schedule a task to
        // reset the flag in a little while.
        mGettingArchiveResetTask = new CancellableRunnable()
        {
            @Override
            public void run()
            {
                sLog.error("Running archive poll recovery task");
                mRequestingArchive.set(false);
            }
        };

        mThreadingService.schedule("ArchivePoller.RecoveryTask",
                                   mGettingArchiveResetTask,
                                   60 * 1000);

        // Request using the threading service since the request can take a while
        mThreadingService.submit("ArchiveRequest", new Runnable()
        {
            @Override
            public void run()
            {
                AccountID account = mJabberProvider.getAccountID();
                String lastArchiveId = (String)
                                    account.getAccountProperty("lastArchiveId");
                sLog.debug("Requesting archive since " + lastArchiveId);

                ArchiveQueryPacket archiveRequest =
                    new ArchiveQueryPacket(lastArchiveId,MAX_RESULTS);

                try
                {
                    mJabberProvider.getConnection().sendStanza(archiveRequest);
                }
                catch (NotConnectedException | InterruptedException ex)
                {
                    sLog.error("Error sending Archive Query for acc:" +
                        account.getLoggableAccountID());
                }
            }
        });
    }

    @Override
    public void entriesAdded(Collection<Jid> addresses)
    {
        // Nothing required
    }

    @Override
    public void entriesUpdated(Collection<Jid> addresses)
    {
        // Nothing required
    }

    @Override
    public void entriesDeleted(Collection<Jid> addresses)
    {
        // Nothing required
    }

    @Override
    public void presenceChanged(Presence pres)
    {
        Jid from = pres.getFrom();
        String accountAddress = mJabberProvider.getAccountID().getAccountAddress();

        if (from != null && from.toString().startsWith(accountAddress))
        {
            boolean isOffline = pres.getMode() == null && !pres.isAvailable();
            sLog.debug("Got presence from " + from + ", is offline " + isOffline);

            boolean stopPolling = false;
            boolean startPolling = false;

            synchronized (mClientJids)
            {
                if (isOffline)
                {
                    boolean removed = mClientJids.remove(from);
                    sLog.info("Removing client JID " + from +
                                                   ", JIDs now " + mClientJids);

                    // If another client has gone offline leaving only this one,
                    // then we should stop polling for the archive
                    stopPolling = removed && mClientJids.size() == 1;
                }
                else
                {
                    boolean added = mClientJids.add(from);
                    sLog.info("Adding client JID " + from +
                                                   ", JIDs now " + mClientJids);

                    // Another client has gone online, therefore this is no
                    // longer the only active client - start polling.
                    startPolling = (added && mClientJids.size() == 2);
                }
            }

            if (stopPolling)
            {
                stopPollingForArchive();
            }
            else if (startPolling)
            {
                startPollingForArchive();
            }
        }
    }

    /**
     * Start a 5 minute poll for changes to the archive.
     */
    private synchronized void startPollingForArchive()
    {
        sLog.info("Starting poll for archive");
        final String name = "ArchivePoller.ArchiveGetterTask";
        mArchiveGetterTask = new CancellableRunnable()
        {
            @Override
            public void run()
            {
                sLog.debug("Getting the archive from the timer");
                requestArchive();
            }
        };

        mThreadingService.scheduleAtFixedRate(name,
                                              mArchiveGetterTask,
                                              ARCHIVE_REQUEST_TIME,
                                              ARCHIVE_REQUEST_TIME);
    }

    /**
     * Stop the 5 minute poll for changes to the archive
     */
    private synchronized void stopPollingForArchive()
    {
        sLog.info("Stop poll for archive");

        // Stop the poll
        if (mArchiveGetterTask != null)
        {
            mArchiveGetterTask.cancel();
            mArchiveGetterTask = null;
        }

        // Request the archive one last time
        requestArchive();
    }

    @Override
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        RegistrationState newState = evt.getNewState();
        sLog.debug("Registration state changed " + newState);

        if (RegistrationState.REGISTERED.equals(newState))
        {
            requestFirstArchive();
            listenForOtherClients();
        }
        else if (RegistrationState.UNREGISTERED.equals(newState))
        {
            stopPollingForArchive();

            // Not registered thus none of the JIDs are valid any more.
            synchronized (mClientJids)
            {
                mClientJids.clear();
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (SELECTED_TAB_NAME.equals(evt.getPropertyName()) &&
            MainFrameTabComponent.HISTORY_TAB_NAME.equals(evt.getNewValue()))
        {
            sLog.debug("User selected recents tab");
            requestArchive();
        }
    }

    /**
     * Check to see if the current user is logged in in only 1 place.
     *
     * @return true if the user is logged in on only one device - this one.
     */
    public boolean isOnlyUser()
    {
        synchronized (mClientJids)
        {
            return mClientJids.size() == 1;
        }
    }

    /**
     * Check to see if we have already completed the first archive request
     * since connecting.
     *
     * @return true if we have completed the first archive request.
     */
    protected boolean isFirstRequestComplete()
    {
        return mFirstRequestComplete.get();
    }

    /**
     * Stop this poller, shutting down all listeners and receivers
     */
    public synchronized void stop()
    {
        sLog.info("Stop called on ArchivePoller " + this);
        mJabberProvider.removeRegistrationStateChangeListener(this);
        mConfigurationService.user().removePropertyChangeListener(this);

        mArchiveReceiver.stop();
        mRequestingArchive.set(false);

        if (mArchiveGetterTask != null)
            mArchiveGetterTask.cancel();

        if (mGettingArchiveResetTask != null)
            mGettingArchiveResetTask.cancel();

        if (mRoster != null)
            mRoster.removeRosterListener(this);
    }
}
