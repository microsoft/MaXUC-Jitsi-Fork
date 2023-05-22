/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber.debugger;

import java.nio.charset.StandardCharsets;

import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.sasl.packet.SaslNonza.AuthMechanism;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smackx.archive.ArchivedMessageExtensionElement;
import org.jivesoftware.smackx.carbons.packet.Carbon;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.jiveproperties.JivePropertiesManager;

import net.java.sip.communicator.impl.protocol.jabber.JabberActivator;
import org.jitsi.service.packetlogging.PacketLoggingService;

// TODO https://dev.azure.com/msazuredev/AzureForOperators/_workitems/edit/59674
// Make use of Smack Debugger interface
/**
 * The jabber packet interceptor and listener that logs outoing and incoming packets to the
 * packet logging service. In XMPP the packets are called stanzas and the terms are used
 * interchangeably.
 * @author Damian Minkov
 */
public class SmackPacketDebugger
{
    /**
     * The current jabber connection.
     */
    private XMPPConnection mConnection = null;

    /**
     * Local address for the connection.
     */
    private byte[] mLocalAddress;

    /**
     * Remote address for the connection.
     */
    private byte[] mRemoteAddress;

    /**
     * Instance for the packet logging service.
     */
    private final PacketLoggingService mPacketLogging;

    /**
     * Creates the SmackPacketDebugger instance.
     */
    public SmackPacketDebugger()
    {
        mPacketLogging = JabberActivator.getPacketLogging();
    }

    /**
     * Sets current connection.
     * @param connection the connection.
     */
    public void setConnection(XMPPConnection connection)
    {
        mConnection = connection;
    }

    /**
     * Interceptor to print out outgoing stanzas.
     *
     */
    public class SmackDebuggerInterceptor implements StanzaListener {
        /**
         * Process the stanza that is about to be sent to the server. The intercepted
         * stanza can be modified by the interceptor.<p>
         * <p/>
         * Interceptors are invoked using the same thread that requested the stanza
         * to be sent, so it's very important that implementations of this method
         * not block for any extended period of time.
         *
         * @param stanza the stanza that is going to be sent to the server.
         */
        @Override
        public void processStanza(Stanza stanza)
        {
            try
            {
                if (mPacketLogging.isLoggingEnabled(
                        PacketLoggingService.ProtocolName.JABBER)
                    && stanza != null && mConnection.getSocket() != null)
                {
                    if (mRemoteAddress == null)
                    {
                        mRemoteAddress = mConnection.getSocket().getInetAddress().getAddress();
                        mLocalAddress = mConnection.getLocalAddress().getAddress();
                    }

                    byte[] packetBytes = anonymizeStanza(stanza)
                        .toXML().toString().getBytes(StandardCharsets.UTF_8);

                    mPacketLogging.logPacket(
                            PacketLoggingService.ProtocolName.JABBER,
                            mLocalAddress,
                            mConnection.getSocket().getLocalPort(),
                            mRemoteAddress,
                            mConnection.getPort(),
                            PacketLoggingService.TransportName.TCP,
                            true,
                            packetBytes
                        );
                }
            }
            catch (Throwable t)
            {
                t.printStackTrace();
            }
        }
    }

    /**
     * Listener to print out incoming stanzas.
     */
    public class SmackDebuggerListener implements StanzaListener
    {
        /**
         * Process the next stanza sent to this stanza listener.<p>
         * <p/>
         * A single thread is responsible for invoking all listeners, so
         * it's very important that implementations of this method not block
         * for any extended period of time.
         *
         * @param stanza the stanza to process.
         */
        @Override
        public void processStanza(Stanza stanza)
        {
            try
            {
                if (mPacketLogging.isLoggingEnabled(
                        PacketLoggingService.ProtocolName.JABBER)
                   && stanza != null && mConnection.getSocket() != null)
                {
                    byte[] packetBytes = anonymizeStanza(stanza)
                            .toXML().toString().getBytes(StandardCharsets.UTF_8);

                    mPacketLogging.logPacket(
                            PacketLoggingService.ProtocolName.JABBER,
                            mRemoteAddress,
                            mConnection.getPort(),
                            mLocalAddress,
                            mConnection.getSocket().getLocalPort(),
                            PacketLoggingService.TransportName.TCP,
                            false,
                            packetBytes
                    );
                }
            }
            catch (Throwable t)
            {
                t.printStackTrace();
            }
        }
    }

    /**
     * Anonymise an XMPP packet
     *
     * Removes sensitive details from a packet if enabled.
     *
     * @param element Element to log - should never be null
     * @return Anonymous packet.
     */
    private NamedElement anonymizeStanza(NamedElement element)
    {
        NamedElement anonStanza;

        if (element instanceof Message)
        {
            anonStanza = cloneAndAnonymizeMessage((Message)element);
        }
        else if (element instanceof AuthMechanism)
        {
            AuthMechanism auth = (AuthMechanism)element;
            anonStanza = new AuthMechanism(auth.getMechanism(), "REMOVED AUTHENTICATION DETAILS");
        }
        else
        {
            anonStanza = element;
        }

        return anonStanza;
    }

    /**
     * Clones and anonymises messages and redacts subject and bodies (including
     * the content of archived or forwarded messages).
     *
     * @param message Message packet to anonymise
     */
    private Message cloneAndAnonymizeMessage(Message message)
    {
        MessageBuilder newMsgBldr = mConnection.getStanzaFactory()
                                    .buildMessageStanza();
        newMsgBldr.ofType(message.getType());
        newMsgBldr.to(message.getTo());
        newMsgBldr.from(message.getFrom());
        newMsgBldr.setLanguage(message.getLanguage());
        if (message.getThread() != null)
        {
            newMsgBldr.setThread(message.getThread());
        }

        for (ExtensionElement extElement : message.getExtensions())
        {
            if (ArchivedMessageExtensionElement.ELEMENT_NAME.equals(extElement.getElementName()) &&
                ArchivedMessageExtensionElement.NAMESPACE.equals(extElement.getNamespace()) &&
                extElement instanceof ArchivedMessageExtensionElement)
            {
                ArchivedMessageExtensionElement oldArchive = (ArchivedMessageExtensionElement)extElement;
                ArchivedMessageExtensionElement newArchive = new ArchivedMessageExtensionElement();

                newArchive.setArchiveId(oldArchive.getArchiveId());
                newArchive.setDate(oldArchive.getDate());
                newArchive.setMessage(cloneAndAnonymizeMessage(oldArchive.getMessage()));

                newMsgBldr.addExtension(newArchive);
            }
            else if (Carbon.NAMESPACE.equals(extElement.getNamespace()) &&
                extElement instanceof CarbonExtension)
            {
                CarbonExtension oldCarbon = (CarbonExtension) extElement;

                Forwarded<Message> oldForwarded = oldCarbon.getForwarded();
                Forwarded<Message> newForwarded =new Forwarded<Message>(
                    cloneAndAnonymizeMessage(oldForwarded.getForwardedStanza()),
                    oldForwarded.getDelayInformation());

                CarbonExtension newCarbon = new CarbonExtension(
                    oldCarbon.getDirection(),
                    newForwarded);

                newMsgBldr.addExtension(newCarbon);
            }
            else if (extElement instanceof Message.Subject)
            {
                Message.Subject sub = (Message.Subject) extElement;
                newMsgBldr.addSubject(sub.getLanguage(),
                        new String(new char[sub.getSubject().length()])
                                .replace('\0', '.'));
            }
            else if (extElement instanceof Message.Body)
            {
                Message.Body body = (Message.Body) extElement;
                newMsgBldr.addBody(body.getLanguage(),
                        new String(new char[body.getMessage().length()])
                                .replace('\0', '.'));
            }
            else
            {
                newMsgBldr.addExtension(extElement);
            }
        }

        for (String propName : JivePropertiesManager.getPropertiesNames(message))
        {
            JivePropertiesManager.addProperty(
                newMsgBldr,
                propName,
                JivePropertiesManager.getProperty(message, propName));
        }

        Message newMsg = newMsgBldr.build();
        newMsg.setStanzaId(message.getStanzaId());

        return newMsg;
    }
}
