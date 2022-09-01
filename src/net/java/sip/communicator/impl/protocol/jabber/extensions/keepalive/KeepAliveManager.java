/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.keepalive;

import java.util.Timer;
import java.util.TimerTask;

import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jxmpp.jid.impl.JidCreate;

import net.java.sip.communicator.impl.protocol.jabber.ProtocolProviderServiceJabberImpl;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeListener;
import net.java.sip.communicator.util.Logger;

/**
 * XEP-0199: XMPP Ping. Tracks received stanzas and if for some interval there
 * is nothing received sends a <tt>KeepAliveEvent</tt>.
 *
 * @author Damian Minkov
 */
public class KeepAliveManager
    implements RegistrationStateChangeListener, StanzaListener
{
    /**
     * Our class logger
     */
    private static final Logger logger =
        Logger.getLogger(KeepAliveManager.class);

    /**
     * The task sending stanzas
     */
    private KeepAliveSendTask keepAliveSendTask = null;

    /**
     * The timer executing tasks on specified intervals
     */
    private Timer keepAliveTimer;

    /**
     * The last received stanza from server.
     */
    private long lastReceiveActivity = 0;

    /**
     * The interval between checks in ms.
     */
    private static final int KEEP_ALIVE_CHECK_INTERVAL = 30000;

    /**
     * If we didn't receive a stanza between two checks, we send a stanza, so we
     * can receive something error or reply.
     */
    private String waitingForStanzaWithID = null;

    /**
     * Our parent provider.
     */
    private ProtocolProviderServiceJabberImpl parentProvider = null;

    /**
     * Creates manager.
     *
     * @param parentProvider the parent provider.
     */
    public KeepAliveManager(ProtocolProviderServiceJabberImpl parentProvider)
    {
        this.parentProvider = parentProvider;

        this.parentProvider.addRegistrationStateChangeListener(this);

        // register the KeepAlive Extension in the smack library
        // used only if somebody ping us
        ProviderManager.addIQProvider(KeepAliveEvent.ELEMENT_NAME,
                                      KeepAliveEvent.NAMESPACE,
                                      new KeepAliveEventProvider());
    }

    /**
     * The method is called by a ProtocolProvider implementation whenever a
     * change in the registration state of the corresponding provider had
     * occurred.
     *
     * @param evt ProviderStatusChangeEvent the event describing the status
     *            change.
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        logger.debug("The provider changed state from: " + evt.getOldState()
            + " to: " + evt.getNewState());

        if (evt.getNewState() == RegistrationState.REGISTERED)
        {
            parentProvider.getConnection().removeStanzaListener(this);
            parentProvider.getConnection().addStanzaListener(this, null);

            // if for some unknown reason we got two registered events
            // and we have already created those tasks make sure we cancel them
            if (keepAliveSendTask != null)
            {
                logger.error("Those task is not supposed to be available for "
                    + parentProvider.getAccountID().getDisplayName());
                keepAliveSendTask.cancel();
                keepAliveSendTask = null;
            }
            if (keepAliveTimer != null)
            {
                logger.error("Those timer is not supposed to be available for "
                    + parentProvider.getAccountID().getDisplayName());
                keepAliveTimer.cancel();
                keepAliveTimer = null;
            }

            keepAliveSendTask = new KeepAliveSendTask();
            waitingForStanzaWithID = null;

            keepAliveTimer = new Timer("Jabber keepalive timer for <"
                + parentProvider.getAccountID() + ">", true);
            keepAliveTimer.scheduleAtFixedRate(keepAliveSendTask,
                                               KEEP_ALIVE_CHECK_INTERVAL,
                                               KEEP_ALIVE_CHECK_INTERVAL);
        }
        else if (evt.getNewState() == RegistrationState.UNREGISTERED ||
                 evt.getNewState() == RegistrationState.CONNECTION_FAILED ||
                 evt.getNewState() == RegistrationState.AUTHENTICATION_FAILED)
        {
            waitingForStanzaWithID = null;

            if (parentProvider.getConnection() != null)
                parentProvider.getConnection().removeStanzaListener(this);

            if (keepAliveSendTask != null)
            {
                keepAliveSendTask.cancel();
                keepAliveSendTask = null;
            }
            if (keepAliveTimer != null)
            {
                keepAliveTimer.cancel();
                keepAliveTimer = null;
            }
        }
    }

    /**
     * A stanza Listener for all incoming stanzas.
     *
     * @param stanza an incoming stanza
     */
    public void processStanza(Stanza stanza)
    {
        // Store that we have received a stanza from the server
        lastReceiveActivity = System.currentTimeMillis();

        if (waitingForStanzaWithID != null && waitingForStanzaWithID.equals(stanza.getStanzaId()))
        {
            // We are no longer waiting for this stanza
            waitingForStanzaWithID = null;
        }

        if (stanza instanceof KeepAliveEvent)
        {
            // Reply only to server pings, to avoid leak of presence
            KeepAliveEvent evt = (KeepAliveEvent) stanza;

            if (evt.getError() != null)
            {
                logger.error("Error (" + evt.getError()
                    + ") received processing stanza " + stanza);
            }
            else if (evt.getFrom() != null &&
                evt.getFrom().equals(parentProvider.getAccountID().getService()))
            {
                try
                {
                    parentProvider.getConnection()
                        .sendStanza(IQ.createResultIQ(evt));
                }
                catch (NotConnectedException | InterruptedException ex)
                {
                    logger.error("Error sending ping reponse!", ex);
                }
            }
        }
    }

    /**
     * Task sending stanzas on intervals. The task runs at specified
     * intervals by the keepAliveTimer
     */
    private class KeepAliveSendTask
        extends TimerTask
    {
        /**
         * Sends a single <tt>KeepAliveEvent</tt>.
         */
        public void run()
        {
            // If we are not registered do nothing
            if (!parentProvider.isRegistered())
            {
                logger.trace("provider not registered. Won't send keep alive for "
                        + parentProvider.getAccountID().getDisplayName());

                parentProvider.unregister(false);

                parentProvider.fireRegistrationStateChanged(
                    parentProvider.getRegistrationState(),
                    RegistrationState.CONNECTION_FAILED,
                    RegistrationStateChangeEvent.REASON_SERVER_NOT_FOUND,
                    null);

                return;
            }

            if (System.currentTimeMillis() - lastReceiveActivity >
                KEEP_ALIVE_CHECK_INTERVAL)
            {
                if (waitingForStanzaWithID != null)
                {
                    logger.error("un-registering not received ping stanza " +
                        "for: " + parentProvider.getAccountID().getDisplayName());

                    parentProvider.unregister(false);

                    parentProvider.fireRegistrationStateChanged(
                        parentProvider.getRegistrationState(),
                        RegistrationState.CONNECTION_FAILED,
                        RegistrationStateChangeEvent.REASON_SERVER_NOT_FOUND,
                        null);

                    return;
                }

                try
                {
                    // Let's send a ping
                    KeepAliveEvent ping = new KeepAliveEvent(
                        parentProvider.getOurJid(),
                        JidCreate.from(parentProvider.getAccountID().getService()));

                    logger.trace("send keepalive for acc: "
                        + parentProvider.getAccountID().getDisplayName());

                    waitingForStanzaWithID = ping.getStanzaId();
                    parentProvider.getConnection().sendStanza(ping);
                }
                catch (Throwable ex)
                {
                    logger.error("Error sending ping request!", ex);
                    waitingForStanzaWithID = null;
                }
            }
        }
    }
}
