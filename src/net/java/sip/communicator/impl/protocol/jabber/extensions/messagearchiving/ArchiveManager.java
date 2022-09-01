// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber.extensions.messagearchiving;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.archive.ArchivedMessageExtensionElement;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;

import net.java.sip.communicator.impl.protocol.jabber.JabberActivator;
import net.java.sip.communicator.impl.protocol.jabber.ProtocolProviderServiceJabberImpl;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeListener;
import net.java.sip.communicator.service.threading.CancellableRunnable;
import net.java.sip.communicator.service.threading.ThreadingService;
import net.java.sip.communicator.util.Logger;

/**
 * The ArchiveManager is responsible for polling to see if XEP-0313 (which is
 * the XEP that adds message archiving) is supported by the server.  If it is,
 * then it creates the objects that handle querying for the archive messages.
 */
public class ArchiveManager implements RegistrationStateChangeListener
{
    private static final Logger sLog = Logger.getLogger(ArchiveManager.class);

    /**
     * The protocol provider that makes and handles XMPP requests
     */
    private final ProtocolProviderServiceJabberImpl mJabberProvider;

    /**
     * Archive Poller - requests the archive from the server periodically.  May
     * be null if archiving is not supported
     */
    private ArchivePoller mArchivePoller = null;

    /**
     * Task used to schedule the checking of whether or not XEP-0313 is supported
     */
    private CancellableRunnable mGetXepTask;

    public ArchiveManager(ProtocolProviderServiceJabberImpl jabberProvider)
    {
        sLog.info("Create ArchiveManager");
        mJabberProvider = jabberProvider;

        // Add a registration state change listener - in order for the availability
        // of XEP-0313 to change, the server has to be stopped.  Therefore, in order
        // to see if XEP support has changed we only need to check when the account
        // goes back online.
        mJabberProvider.addRegistrationStateChangeListener(this);
        boolean registered = mJabberProvider.isRegistered();
        sLog.debug("Jabber registered? " + registered);

        if (registered)
        {
            mJabberProvider.removeRegistrationStateChangeListener(this);
            maybeStartArchivePoll();
        }
    }

    @Override
    public synchronized void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        RegistrationState newState = evt.getNewState();
        sLog.trace("Registration state changed " + newState);

        if (RegistrationState.REGISTERED.equals(newState))
        {
            maybeStartArchivePoll();
            mJabberProvider.removeRegistrationStateChangeListener(this);
        }
    }

    /**
     * Check to see if XEP-0313 is supported.  If it is (and it wasn't before)
     * then this creates the message archive objects.  Vice-versa (it isn't
     * supported but was before) the archive objects are removed.
     */
    private void maybeStartArchivePoll()
    {
        sLog.debug("maybeStartArchivePoll");

        // Run on a different thread since this request involves a network request
        ThreadingService threadingService = JabberActivator.getThreadingService();
        mGetXepTask = new CancellableRunnable()
        {
            @Override
            public void run()
            {
                boolean supported = isXepSupported();

                // If the XEP is supported and we haven't already done an
                // archive query since connecting, we need to let the chat room
                // manager know that it needs to wait for chat room join and
                // leave timestamps from join and leave messages in the
                // archive.  If this is not the first request, the chat room
                // manager will already have finished this processing so we
                // don't need to do anything.
                if (mArchivePoller == null ||
                    !mArchivePoller.isFirstRequestComplete())
                {
                    mJabberProvider.getChatRoomManager().
                                            setArchiveQueryComplete(!supported);
                }

                if (!supported)
                {
                    sLog.info("Message archiving not supported");
                    if (mArchivePoller != null)
                    {
                        mArchivePoller.stop();
                    }
                }
                else
                {
                    sLog.info("Message archiving now supported");
                    mArchivePoller = new ArchivePoller(mJabberProvider);
                }
            }
        };

        String name = "ArchiveManager.XEP0313 supported check";
        threadingService.submit(name, mGetXepTask);
    }

    /**
     * @return true if the server supports XEP-0313 (required for archiving)
     */
    private boolean isXepSupported()
    {
        XMPPConnection connection = mJabberProvider.getConnection();
        ServiceDiscoveryManager discoManager =
                             ServiceDiscoveryManager.getInstanceFor(connection);
        boolean supported = false;

        try
        {
            DiscoverInfo infoSet =
                         discoManager.discoverInfo(connection.getXMPPServiceDomain());

            if (infoSet.containsFeature(ArchivedMessageExtensionElement.NAMESPACE))
            {
                sLog.debug("Info set has MAM " + infoSet);
                supported = true;
            }
        }
        catch (XMPPErrorException | NoResponseException | NotConnectedException |
            InterruptedException e)
        {
            // Indicates that something went wrong with the connection - most
            // likely we went offline.  Not fatal, but means we will check again
            // when the manager is re-created.
            sLog.debug("Error checking discovery info " + e);
        }

        return supported;
    }

    /**
     * Stops this archive manager and any associated archive objects
     */
    public void stop()
    {
        sLog.info("Stop");

        CancellableRunnable getXepTask = mGetXepTask;
        if (getXepTask != null)
        {
            getXepTask.cancel();
            mGetXepTask = null;
        }

        mJabberProvider.removeRegistrationStateChangeListener(this);
        ArchivePoller archivePoller = mArchivePoller;

        if (archivePoller != null)
        {
            archivePoller.stop();
            mArchivePoller = null;
        }
    }
}
