/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.provider.ProviderManager;

import net.java.sip.communicator.impl.protocol.jabber.extensions.geolocation.GeolocationJabberUtils;
import net.java.sip.communicator.impl.protocol.jabber.extensions.geolocation.GeolocationExtensionElement;
import net.java.sip.communicator.impl.protocol.jabber.extensions.geolocation.GeolocationExtensionElementProvider;
import net.java.sip.communicator.impl.protocol.jabber.extensions.geolocation.GeolocationPresence;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.OperationSetGeolocation;
import net.java.sip.communicator.service.protocol.OperationSetPersistentPresence;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.event.GeolocationEvent;
import net.java.sip.communicator.service.protocol.event.GeolocationListener;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeListener;
import net.java.sip.communicator.util.Logger;

/**
 * The Jabber implementation of an OperationSetGeolocation done with the
 * XEP-0080: User Geolocation. This class broadcast our own geolocation and
 * manage the geolocation status of our buddies.
 *
 * Currently, we send geolocation message in presence. We passively listen
 * to buddies geolocation when their presence are updated.
 *
 * @author Guillaume Schreiner
 */
public class OperationSetGeolocationJabberImpl
    implements OperationSetGeolocation
{
    /**
     * Our logger.
     */
    private static final Logger logger =
        Logger.getLogger(OperationSetGeolocationJabberImpl.class);

    /**
     * The list of Geolocation status listeners interested in receiving presence
     * notifications of changes in geolocation of contacts in our contact list.
     */
    private final List<GeolocationListener> geolocationContactsListeners
        = new Vector<>();

    /**
     * A callback to the provider
     */
    private final ProtocolProviderServiceJabberImpl jabberProvider;

    /**
     * A callback to the persistent presence operation set.
     */
    private final OperationSetPersistentPresence opsetprez;

    /**
     * Constuctor
     *
     * @param provider <tt>ProtocolProviderServiceJabberImpl</tt>
     */
    public OperationSetGeolocationJabberImpl(
        ProtocolProviderServiceJabberImpl provider)
    {
        this.jabberProvider = provider;

        this.opsetprez
                = provider
                    .getOperationSet(OperationSetPersistentPresence.class);

        this.jabberProvider.addRegistrationStateChangeListener(
            new RegistrationStateListener());

        // Add the custom GeolocationExtension to the Smack library
        ProviderManager.addExtensionProvider(
            GeolocationExtensionElementProvider.ELEMENT_NAME
            , GeolocationExtensionElementProvider.NAMESPACE
            , new GeolocationExtensionElementProvider());
    }

    /**
     * Broadcast our current Geolocation trough this provider using a Jabber
     * presence message.
     *
     * @param geolocation our current Geolocation ready to be sent
     */
    public void publishGeolocation(Map<String, String> geolocation)
    {
        GeolocationPresence myGeolocPrez =
            new GeolocationPresence(opsetprez, jabberProvider.getConnection());

        GeolocationExtensionElement geolocExt = GeolocationJabberUtils
            .convertMapToExtension(geolocation);

        myGeolocPrez.setGeolocationExtension(geolocExt);

        try
        {
            this.jabberProvider.getConnection()
                .sendStanza(myGeolocPrez.getGeolocPresence());
        }
        catch (NotConnectedException | InterruptedException e)
        {
            logger.error("Error publishing geolocation!", e);
        }
    }

    /**
     * Retrieve the geolocation of the given contact.
     * <p>
     * Note: Currently not implemented because we can not actively poll the
     * server for the presence of a given contact ?
     * <p>
     * @param contactIdentifier the <tt>Contact</tt> we want to retrieve its
     * geolocation by its identifier.
     * @return the <tt>Geolocation</tt> of the contact.
     */
    public Map<String, String> queryContactGeolocation(String contactIdentifier)
    {
        /** @todo implement queryContactGeolocation() */
        return null;
    }

    /**
     * Registers a listener that would get notifications any time a contact
     * refreshed its geolocation via Presence.
     *
     * @param listener the <tt>ContactGeolocationPresenceListener</tt> to
     * register
     */
    public void addGeolocationListener(GeolocationListener listener)
    {
        synchronized (geolocationContactsListeners)
        {
            geolocationContactsListeners.add(listener);
        }
    }

    /**
     * Remove a listener that would get notifications any time a contact
     * refreshed its geolocation via Presence.
     *
     * @param listener the <tt>ContactGeolocationPresenceListener</tt> to
     * register
     */
    public void removeGeolocationListener(GeolocationListener listener)
    {
        synchronized (geolocationContactsListeners)
        {
            geolocationContactsListeners.remove(listener);
        }
    }

    /**
     * Our listener that will tell us when we're registered to server
     * and we are ready to launch the listener for GeolocationExtensionElement
     * packets
     */
    private class RegistrationStateListener
        implements RegistrationStateChangeListener
    {
        /**
         * The method is called by a ProtocolProvider implementation whenever
         * a change in the registration state of the corresponding provider had
         * occurred.
         * @param evt ProviderStatusChangeEvent the event describing the status
         * change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            logger.debug("The Jabber provider changed state from: "
                         + evt.getOldState()
                         + " to: " + evt.getNewState());

            if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                StanzaExtensionFilter filterGeoloc =
                    new StanzaExtensionFilter(
                        GeolocationExtensionElementProvider.ELEMENT_NAME,
                        GeolocationExtensionElementProvider.NAMESPACE
                    );

                // launch the listener
                try
                {
                    jabberProvider.getConnection().addStanzaListener(
                        new GeolocationPresenceStanzaListener()
                                , filterGeoloc
                        );
                }
                catch (Exception e)
                {
                    logger.error(e);
                }
            }
            else if (evt.getNewState() == RegistrationState.UNREGISTERED
                     || evt.getNewState()
                                == RegistrationState.AUTHENTICATION_FAILED
                     || evt.getNewState()
                                == RegistrationState.CONNECTION_FAILED)
            {
            }
        }
    }

    /**
     * This class listen to GeolocationExtension into Presence Packet.
     * If GeolocationExtension is found, an event is sent.
     *
     * @author Guillaume Schreiner
     */
    private class GeolocationPresenceStanzaListener
        implements StanzaListener
    {
        /**
         * Match incoming packets with geolocation Extension tags for
         * dispatching a new event.
         *
         * @param packet matching Geolocation Extension tags.
         */
        public void processStanza(Stanza stanza)
        {
            String from = stanza.getFrom().asBareJid().toString();

            GeolocationExtensionElement geolocExt =
                (GeolocationExtensionElement) stanza.getExtensionElement(
                    GeolocationExtensionElementProvider.ELEMENT_NAME,
                    GeolocationExtensionElementProvider.NAMESPACE);

            if (geolocExt != null)
            {
                logger.debug("GeolocationExtension found from " + from + ":" +
                             geolocExt.toXML());

                Map<String, String> newGeolocation
                    = GeolocationJabberUtils.convertExtensionToMap(geolocExt);

                this.fireGeolocationContactChangeEvent(
                    from,
                    newGeolocation);
            }
        }

        /**
         * Notify registred listeners for a new incoming GeolocationExtension.
         *
         * @param sourceContact which send a new Geolocation.
         * @param newGeolocation the new given Geolocation.
         */
        public void fireGeolocationContactChangeEvent(
                String sourceContact,
                Map<String, String> newGeolocation)
        {
            logger.debug("Trying to dispatch geolocation contact update for "
                         + sourceContact);

            Contact source = opsetprez.findContactByID(sourceContact);

            GeolocationEvent evt =
                new GeolocationEvent(
                    source
                    , jabberProvider
                    , newGeolocation
                    , OperationSetGeolocationJabberImpl.this);

            logger.debug("Dispatching  geolocation contact update. Listeners="
                         + geolocationContactsListeners.size()
                         + " evt=" + evt);

            GeolocationListener[] listeners;

            synchronized (geolocationContactsListeners)
            {
                listeners
                    = geolocationContactsListeners.toArray(
                            new GeolocationListener[
                                    geolocationContactsListeners.size()]);
            }

            for (GeolocationListener listener : listeners)
                listener.contactGeolocationChanged(evt);
        }
    }
}
