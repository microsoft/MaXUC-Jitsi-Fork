/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.packet.RosterPacket;

import net.java.sip.communicator.service.protocol.AuthorizationRequest;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.OperationSetExtendedAuthorizations;
import net.java.sip.communicator.util.Logger;

/**
 * Extended authorization implementation for jabber provider.
 *
 * @author Damian Minkov
 */
public class OperationSetExtendedAuthorizationsJabberImpl
    implements OperationSetExtendedAuthorizations
{
    private static final Logger logger =
        Logger.getLogger(OperationSetExtendedAuthorizationsJabberImpl.class);

    /**
     * A reference to the persistent presence operation set that we use
     * to match incoming messages to <tt>Contact</tt>s and vice versa.
     */
    private OperationSetPersistentPresenceJabberImpl opSetPersPresence = null;

    /**
     * The parent provider.
     */
    private ProtocolProviderServiceJabberImpl parentProvider;

    /**
     * Creates OperationSetExtendedAuthorizations.
     * @param opSetPersPresence the presence opset.
     * @param provider the parent provider
     */
    OperationSetExtendedAuthorizationsJabberImpl(
        ProtocolProviderServiceJabberImpl provider,
        OperationSetPersistentPresenceJabberImpl opSetPersPresence)
    {
        this.opSetPersPresence = opSetPersPresence;
        this.parentProvider = provider;
    }

    /**
     * Send a positive authorization to <tt>contact</tt> thus allowing them to
     * add us to their contact list without needing to first request an
     * authorization.
     * @param contact the <tt>Contact</tt> whom we're granting authorization
     * prior to receiving a request.
     */
    public void explicitAuthorize(Contact contact)
    {
        opSetPersPresence.assertConnected();

        if( !(contact instanceof ContactJabberImpl) )
            throw new IllegalArgumentException(
                "The specified contact is not an jabber contact." +
                    contact);

        Presence responsePacket =
            parentProvider.getConnection()
                          .getStanzaFactory()
                          .buildPresenceStanza()
                          .ofType(Presence.Type.subscribed).build();

        try
        {
            responsePacket.setTo(((ContactJabberImpl)contact).getAddressAsJid());
            parentProvider.getConnection().sendStanza(responsePacket);
        }
        catch (NotConnectedException | InterruptedException e)
        {
            logger.error("Failed to send positive authorization for contact: "
                + contact + ".", e);
        }
   }

    /**
     * Send an authorization request, requesting <tt>contact</tt> to add them
     * to our contact list?
     *
     * @param request the <tt>AuthorizationRequest</tt> that we'd like the
     * protocol provider to send to <tt>contact</tt>.
     * @param contact the <tt>Contact</tt> who we'd be asking for an
     * authorization.
     */
    public void reRequestAuthorization(AuthorizationRequest request,
                                       Contact contact)
    {
        opSetPersPresence.assertConnected();

        if( !(contact instanceof ContactJabberImpl) )
            throw new IllegalArgumentException(
                "The specified contact is not an jabber contact." +
                    contact);

        Presence responsePacket =
            parentProvider.getConnection()
                          .getStanzaFactory()
                          .buildPresenceStanza()
                          .ofType(Presence.Type.subscribed).build();

        try
        {
            responsePacket.setTo(((ContactJabberImpl)contact).getAddressAsJid());
            parentProvider.getConnection().sendStanza(responsePacket);
        }
        catch (NotConnectedException | InterruptedException e)
        {
            logger.error("Failed to send authorization request for contact: "
                + contact, e);
        }
    }

    /**
     * Returns the subscription status for the <tt>contact</tt> or
     * if not available returns null.
     * @param contact the contact to query for subscription status.
     * @return the subscription status for the <tt>contact</tt> or
     *         if not available returns null.
     */
    public SubscriptionStatus getSubscriptionStatus(Contact contact)
    {
        if( !(contact instanceof ContactJabberImpl) )
            throw new IllegalArgumentException(
                "The specified contact is not an jabber contact." +
                    contact);

        RosterEntry entry = ((ContactJabberImpl) contact).getSourceEntry();

        if(entry != null)
        {
            if((entry.getType() == RosterPacket.ItemType.none
                    || entry.getType() == RosterPacket.ItemType.from)
               && entry.isSubscriptionPending())
            {
                return SubscriptionStatus.SubscriptionPending;
            }
            else if(entry.getType() == RosterPacket.ItemType.to
                    || entry.getType() == RosterPacket.ItemType.both)
                return SubscriptionStatus.Subscribed;
            else
                return SubscriptionStatus.NotSubscribed;
        }

        return null;
    }
}
