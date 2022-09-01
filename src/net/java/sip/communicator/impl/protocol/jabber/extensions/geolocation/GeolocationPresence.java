/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.geolocation;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.PresenceBuilder;

import net.java.sip.communicator.impl.protocol.jabber.OperationSetPersistentPresenceJabberImpl;
import net.java.sip.communicator.service.protocol.OperationSetPresence;

/**
 * This class represents a Jabber presence message including a Geolocation
 * Extension.
 *
 * @author Guillaume Schreiner
 */
public class GeolocationPresence
{
    /**
     * the presence message to send via a XMPPConnection
     */
    private Presence prez = null;

    /**
     *
     * @param persistentPresence OperationSetPresence
     * @param connection XMPPConnection
     */
    public GeolocationPresence(OperationSetPresence persistentPresence,
                               XMPPConnection connection)
    {
        PresenceBuilder presenceBuilder = connection.getStanzaFactory()
                                                    .buildPresenceStanza()
                                                    .ofType(Presence.Type.available);

        // set the custom status message
        presenceBuilder.setStatus(persistentPresence.getCurrentStatusMessage());

        // set the presence mode (available, NA, free for chat)
        presenceBuilder.setMode(
            OperationSetPersistentPresenceJabberImpl.presenceStatusToJabberMode(
                persistentPresence
                .getPresenceStatus()));

        this.prez = presenceBuilder.build();
    }

    /**
     * Set the Geolocation extension packet.
     *
     * @param ext the <tt>GeolocationPacketExtension</tt> to set
     */
    public void setGeolocationExtension(GeolocationExtensionElement ext)
    {
        this.prez.addExtension(ext);
    }

    /**
     * Get the Geolocation presence message.
     *
     * @return the Geolocation presence message.
     */
    public Presence getGeolocPresence()
    {
        return this.prez;
    }
}

