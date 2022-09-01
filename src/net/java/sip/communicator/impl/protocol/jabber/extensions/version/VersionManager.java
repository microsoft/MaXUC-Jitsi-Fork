/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.version;

import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.IQTypeFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.StringUtils;

import net.java.sip.communicator.impl.protocol.jabber.JabberActivator;
import net.java.sip.communicator.impl.protocol.jabber.ProtocolProviderServiceJabberImpl;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeListener;
import org.jitsi.service.version.Version;
import net.java.sip.communicator.util.Logger;

/**
 * XEP-0092: Software Version.
 * Provider that fills the version IQ using our Version Service.
 *
 * @author Damian Minkov
 */
public class VersionManager
    implements RegistrationStateChangeListener,
               StanzaListener
{
    /**
     * Our class logger
     */
    private static final Logger sLog =
        Logger.getLogger(VersionManager.class);

    /**
     * Our parent provider.
     */
    private ProtocolProviderServiceJabberImpl parentProvider = null;

    /**
     * Creates and registers the provider.
     * @param parentProvider
     */
    public VersionManager(ProtocolProviderServiceJabberImpl parentProvider)
    {
        this.parentProvider = parentProvider;

        this.parentProvider.addRegistrationStateChangeListener(this);
    }

    /**
     * The method is called by a ProtocolProvider implementation whenever
     * a change in the registration state of the corresponding provider had
     * occurred.
     * @param evt ProviderStatusChangeEvent the event describing the status
     * change.
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        if (evt.getNewState() == RegistrationState.REGISTERED)
        {
            parentProvider.getConnection().removeStanzaListener(this);
            parentProvider.getConnection().addStanzaListener(this,
                new AndFilter(IQTypeFilter.GET,
                    new StanzaTypeFilter(
                            org.jivesoftware.smackx.iqversion.packet.Version.class)));
        }
        else if(evt.getNewState() == RegistrationState.UNREGISTERED
            || evt.getNewState() == RegistrationState.CONNECTION_FAILED
            || evt.getNewState() == RegistrationState.AUTHENTICATION_FAILED)
        {
            if(parentProvider.getConnection() != null)
            {
                parentProvider.getConnection().removeStanzaListener(this);
            }
        }
    }

    /**
     * A packet Listener for incoming Version packets.
     * @param stanza an incoming packet
     */
    @Override
    public void processStanza(Stanza stanza)
    {
        Version ver = JabberActivator.getVersionService().getCurrentVersion();
        String appName = (StringUtils.escapeForXml(ver.getApplicationName())).toString();

        if (!appName.toLowerCase().contains("jitsi"))
        {
            appName += "-Jitsi";
        }

        // send packet
        org.jivesoftware.smackx.iqversion.packet.Version versionIQ =
            new org.jivesoftware.smackx.iqversion.packet.Version(
                appName,
                ver.toString(),
                System.getProperty("os.name"));

        versionIQ.setType(IQ.Type.result);
        versionIQ.setTo(stanza.getFrom());
        versionIQ.setFrom(stanza.getTo());
        versionIQ.setStanzaId(stanza.getStanzaId());

        try
        {
            parentProvider.getConnection().sendStanza(versionIQ);
        }
        catch (NotConnectedException | InterruptedException e)
        {
            sLog.error("Could not send version IQ packet", e);
        }
    }
}
