/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.BodyElementProvider;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.chatstates.provider.ChatStateExtensionProvider;
import org.jivesoftware.smackx.delay.provider.DelayInformationProvider;
import org.jivesoftware.smackx.delay.provider.LegacyDelayInformationProvider;
import org.jivesoftware.smackx.iqversion.provider.VersionProvider;
import org.jivesoftware.smackx.muc.packet.GroupChatInvitation;
import org.jivesoftware.smackx.archive.ArchiveCompleteIQ;
import org.jivesoftware.smackx.archive.ArchiveCompleteProvider;
import org.jivesoftware.smackx.archive.ArchivedMessageExtensionElement;
import org.jivesoftware.smackx.archive.ArchivedMessageExtensionElementProvider;
import org.jivesoftware.smackx.archive.MessageArchivedExtensionElement;
import org.jivesoftware.smackx.archive.MessageArchivedExtensionElementProvider;
import org.jivesoftware.smackx.bytestreams.ibb.provider.CloseIQProvider;
import org.jivesoftware.smackx.bytestreams.ibb.provider.DataPacketProvider;
import org.jivesoftware.smackx.bytestreams.ibb.provider.OpenIQProvider;
import org.jivesoftware.smackx.bytestreams.socks5.provider.BytestreamsProvider;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension;
import org.jivesoftware.smackx.carbons.provider.CarbonManagerProvider;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.forward.provider.ForwardedProvider;
import org.jivesoftware.smackx.iqlast.packet.LastActivity;
import org.jivesoftware.smackx.offline.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.offline.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.disco.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.disco.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.muc.provider.MUCAdminProvider;
import org.jivesoftware.smackx.muc.provider.MUCOwnerProvider;
import org.jivesoftware.smackx.muc.provider.MUCUserProvider;
import org.jivesoftware.smackx.xevent.provider.MessageEventProvider;
import org.jivesoftware.smackx.xroster.provider.RosterExchangeProvider;
import org.jivesoftware.smackx.si.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.vcardtemp.provider.VCardProvider;
import org.jivesoftware.smackx.xhtmlim.provider.XHTMLExtensionProvider;

import net.java.sip.communicator.util.Logger;

/**
 * Our Provider Manager that loads providers and extensions we use.
 * If we receive query and packets for unknown providers
 * service-unavailable is sent according RFC 6120.
 *
 * @author Damian Minkov
 */
public class ProviderManagerExt
{
    /**
     * Logger of this class
     */
    private static final Logger logger =
        Logger.getLogger(ProviderManagerExt.class);

    /**
     * Loads the providers and extensions used by us.
     */
    public static void load()
    {
        //<!-- Private Data Storage -->
        //addProvider("query", "jabber:iq:private",
        //    org.jivesoftware.smackx.PrivateDataManager.PrivateDataIQProvider.class);

        //<!-- Time -->
        //addProvider("query", "jabber:iq:time",
        //    org.jivesoftware.smackx.packet.Time.class);

        //<!-- Roster Exchange -->
        addExtProvider("x", "jabber:x:roster",
            RosterExchangeProvider.class);
        //<!-- Message Events -->
        addExtProvider("x", "jabber:x:event",
            MessageEventProvider.class);

        //<!-- Chat State -->
        addExtProvider(
            "active",
            "http://jabber.org/protocol/chatstates",
            ChatStateExtensionProvider.class);
        addExtProvider(
            "composing",
            "http://jabber.org/protocol/chatstates",
            ChatStateExtensionProvider.class);
        addExtProvider(
            "paused",
            "http://jabber.org/protocol/chatstates",
            ChatStateExtensionProvider.class);
        addExtProvider(
            "inactive",
            "http://jabber.org/protocol/chatstates",
            ChatStateExtensionProvider.class);
        addExtProvider(
            "gone",
            "http://jabber.org/protocol/chatstates",
            ChatStateExtensionProvider.class);

        //<!-- XHTML -->
        addExtProvider("html", "http://jabber.org/protocol/xhtml-im",
            XHTMLExtensionProvider.class);

        //<!-- Group Chat Invitations -->
        addExtProvider("x", "jabber:x:conference",
            GroupChatInvitation.Provider.class);

        //<!-- Service Discovery # Items -->
        addProvider("query", "http://jabber.org/protocol/disco#items",
            DiscoverItemsProvider.class);
        //<!-- Service Discovery # Info -->
        addProvider("query", "http://jabber.org/protocol/disco#info",
            DiscoverInfoProvider.class);

        //<!-- Data Forms-->
        //addExtProvider("x", "jabber:x:data",
        //    org.jivesoftware.smackx.provider.DataFormProvider.class);

        //<!-- MUC User -->
        addExtProvider("x", "http://jabber.org/protocol/muc#user",
            MUCUserProvider.class);
        //<!-- MUC Admin -->
        addProvider("query", "http://jabber.org/protocol/muc#admin",
            MUCAdminProvider.class);
        //<!-- MUC Owner -->
        addProvider("query", "http://jabber.org/protocol/muc#owner",
            MUCOwnerProvider.class);

        //<!-- Delayed Delivery -->
        addExtProvider("x", "jabber:x:delay",
           LegacyDelayInformationProvider.class);
        addExtProvider("delay", "urn:xmpp:delay",
            DelayInformationProvider.class);

        //<!-- Version -->
        addProvider("query", "jabber:iq:version",
            VersionProvider.class);

        //<!-- VCard -->
        addProvider("vCard", "vcard-temp",
            VCardProvider.class);

        //<!-- Offline Message Requests -->
        addProvider("offline", "http://jabber.org/protocol/offline",
            OfflineMessageRequest.Provider.class);

        //<!-- Offline Message Indicator -->
        addExtProvider("offline", "http://jabber.org/protocol/offline",
            OfflineMessageInfo.Provider.class);

        //<!-- Last Activity -->
        addProvider("query", "jabber:iq:last",
            LastActivity.Provider.class);

        //<!-- User Search -->
        //addProvider("query", "jabber:iq:search",
        //    org.jivesoftware.smackx.search.UserSearch.Provider.class);

        //<!-- SharedGroupsInfo -->
        //addProvider("sharedgroup", "http://www.jivesoftware.org/protocol/sharedgroup",
        //    org.jivesoftware.smackx.packet.SharedGroupsInfo.Provider.class);

        //<!-- JEP-33: Extended Stanza Addressing -->
        //addProvider("addresses", "http://jabber.org/protocol/address",
        //    org.jivesoftware.smackx.provider.MultipleAddressesProvider.class);

        //<!-- FileTransfer -->
        addProvider("si", "http://jabber.org/protocol/si",
            StreamInitiationProvider.class);
        addProvider("query", "http://jabber.org/protocol/bytestreams",
            BytestreamsProvider.class);
        addProvider("open", "http://jabber.org/protocol/ibb",
            OpenIQProvider.class);
        addProvider("data", "http://jabber.org/protocol/ibb",
            DataPacketProvider.class);
        addProvider("close", "http://jabber.org/protocol/ibb",
            CloseIQProvider.class);
        addExtProvider("data", "http://jabber.org/protocol/ibb",
            DataPacketProvider.class);

        // XEP-013 Message archiving
         addProvider(ArchiveCompleteIQ.ELEMENT_NAME,
             ArchiveCompleteIQ.NAMESPACE,
             ArchiveCompleteProvider.class);
         addExtProvider(ArchivedMessageExtensionElement.ELEMENT_NAME,
             ArchivedMessageExtensionElement.NAMESPACE,
             ArchivedMessageExtensionElementProvider.class);
         addExtProvider(MessageArchivedExtensionElement.ELEMENT_NAME,
             MessageArchivedExtensionElement.NAMESPACE,
             MessageArchivedExtensionElementProvider.class);
         // Messages from the archive could have the forwarded namespace
         // so set up the BodyElementProvider to parse them
         addExtProvider(Message.Body.ELEMENT,
             Forwarded.NAMESPACE,
             BodyElementProvider.class);

        // XEP-280 Carbons
        addExtProvider(CarbonExtension.Direction.received.toString(),
                       CarbonExtension.NAMESPACE,
                       CarbonManagerProvider.class);
        addExtProvider(CarbonExtension.Direction.sent.toString(),
                       CarbonExtension.NAMESPACE,
                       CarbonManagerProvider.class);
        addExtProvider(Forwarded.ELEMENT,
                       Forwarded.NAMESPACE,
                       ForwardedProvider.class);

        // XEP-0198 support must be added by the owner of ProviderManagerExt,
        // as using the static method to add the provider causes a new instance
        // of Smack's basic ProviderManager to be created and used instead of
        // this class.

        //<!-- Privacy -->
        //addProvider("query", "jabber:iq:privacy",
        //    org.jivesoftware.smack.provider.PrivacyProvider.class);

        //<!-- Fastpath providers -->
        //addProvider("offer", "http://jabber.org/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.OfferRequestProvider.class);

        //addProvider("offer-revoke", "http://jabber.org/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.OfferRevokeProvider.class);

        //addProvider("agent-status-request", "http://jabber.org/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.AgentStatusRequest.Provider.class);

        //addProvider("transcripts", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.TranscriptsProvider.class);

        //addProvider("transcript", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.TranscriptProvider.class);

        //addProvider("workgroups", "http://jabber.org/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.AgentWorkgroups.Provider.class);

        //addProvider("agent-info", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.AgentInfo.Provider.class);

                //addProvider("transcript-search", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.TranscriptSearch.Provider.class);

        //addProvider("occupants-info", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.OccupantsInfo.Provider.class);

        //addProvider("chat-settings", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.settings.ChatSettings.InternalProvider.class);

        //addProvider("chat-notes", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.ext.notes.ChatNotes.Provider.class);

        //addProvider("chat-sessions", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.ext.history.AgentChatHistory.InternalProvider.class);

        //addProvider("offline-settings", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.settings.OfflineSettings.InternalProvider.class);

        //addProvider("sound-settings", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.settings.SoundSettings.InternalProvider.class);

         //addProvider("workgroup-properties", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.settings.WorkgroupProperties.InternalProvider.class);

        //addProvider("search-settings", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.settings.SearchSettings.InternalProvider.class);

        //addProvider("workgroup-form", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.ext.forms.WorkgroupForm.InternalProvider.class);

        //addProvider("macros", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.ext.macros.Macros.InternalProvider.class);

        //addProvider("chat-metadata", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.ext.history.ChatMetadata.Provider.class);

        //<!--
        //org.jivesoftware.smackx.workgroup.site is missing ...

        //addProvider("site-user", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.site.SiteUser.Provider.class);

        //addProvider("site-invite", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.site.SiteInvitation.Provider.class);

        //addProvider("site-user-history", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.site.SiteUserHistory.Provider.class);
        //-->
        //addProvider("generic-metadata", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.settings.GenericSettings.InternalProvider.class);

        //addProvider("monitor", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.MonitorPacket.InternalProvider.class);

        //<!-- Packet Extension Providers -->
        //addExtProvider("queue-status", "http://jabber.org/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.QueueUpdate.Provider.class);

        //addExtProvider("workgroup", "http://jabber.org/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.WorkgroupInformation.Provider.class);

        //addExtProvider("metadata", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.MetaDataProvider.class);

        //addExtProvider("session", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.SessionID.Provider.class);

        //addExtProvider("user", "http://jivesoftware.com/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.UserID.Provider.class);

        //addExtProvider("agent-status", "http://jabber.org/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.AgentStatus.Provider.class);

        //addExtProvider("notify-queue-details", "http://jabber.org/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.QueueDetails.Provider.class);

        //addExtProvider("notify-queue", "http://jabber.org/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.QueueOverview.Provider.class);

        //addExtProvider("invite", "http://jabber.org/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.RoomInvitation.Provider.class);

        //addExtProvider("transfer", "http://jabber.org/protocol/workgroup",
        //    org.jivesoftware.smackx.workgroup.packet.RoomTransfer.Provider.class);

        //<!-- SHIM -->
        //addExtProvider("headers", "http://jabber.org/protocol/shim",
        //    org.jivesoftware.smackx.provider.HeadersProvider.class);

        //addExtProvider("header", "http://jabber.org/protocol/shim",
        //    org.jivesoftware.smackx.provider.HeaderProvider.class);

        //<!-- XEP-0060 pubsub -->
        //addProvider("pubsub", "http://jabber.org/protocol/pubsub",
        //    org.jivesoftware.smackx.pubsub.provider.PubSubProvider.class);

        //addExtProvider("create", "http://jabber.org/protocol/pubsub",
        //    org.jivesoftware.smackx.pubsub.provider.SimpleNodeProvider.class);

        //addExtProvider("items", "http://jabber.org/protocol/pubsub",
        //    org.jivesoftware.smackx.pubsub.provider.ItemsProvider.class);

        //addExtProvider("item", "http://jabber.org/protocol/pubsub",
        //    org.jivesoftware.smackx.pubsub.provider.ItemProvider.class);

        //addExtProvider("subscriptions", "http://jabber.org/protocol/pubsub",
        //    org.jivesoftware.smackx.pubsub.provider.SubscriptionsProvider.class);

        //addExtProvider("subscription", "http://jabber.org/protocol/pubsub",
        //    org.jivesoftware.smackx.pubsub.provider.SubscriptionProvider.class);

        //addExtProvider("affiliations", "http://jabber.org/protocol/pubsub",
        //    org.jivesoftware.smackx.pubsub.provider.AffiliationsProvider.class);

        //addExtProvider("affiliation", "http://jabber.org/protocol/pubsub",
        //    org.jivesoftware.smackx.pubsub.provider.AffiliationProvider.class);

        //addExtProvider("options", "http://jabber.org/protocol/pubsub",
        //    org.jivesoftware.smackx.pubsub.provider.FormNodeProvider.class);

        //<!-- XEP-0060 pubsub#owner -->
        //addProvider("pubsub", "http://jabber.org/protocol/pubsub#owner",
        //    org.jivesoftware.smackx.pubsub.provider.PubSubProvider.class);

        //addExtProvider("configure", "http://jabber.org/protocol/pubsub#owner",
        //    org.jivesoftware.smackx.pubsub.provider.FormNodeProvider.class);

        //addExtProvider("default", "http://jabber.org/protocol/pubsub#owner",
        //    org.jivesoftware.smackx.pubsub.provider.FormNodeProvider.class);

        //<!-- XEP-0060 pubsub#event -->
        //addExtProvider("event", "http://jabber.org/protocol/pubsub#event",
        //    org.jivesoftware.smackx.pubsub.provider.EventProvider.class);

        //addExtProvider("configuration", "http://jabber.org/protocol/pubsub#event",
        //    org.jivesoftware.smackx.pubsub.provider.ConfigEventProvider.class);

        //addExtProvider("delete", "http://jabber.org/protocol/pubsub#event",
        //    org.jivesoftware.smackx.pubsub.provider.SimpleNodeProvider.class);

        //addExtProvider("options", "http://jabber.org/protocol/pubsub#event",
        //    org.jivesoftware.smackx.pubsub.provider.FormNodeProvider.class);

        //addExtProvider("items", "http://jabber.org/protocol/pubsub#event",
        //    org.jivesoftware.smackx.pubsub.provider.ItemsProvider.class);

        //addExtProvider("item", "http://jabber.org/protocol/pubsub#event",
        //    org.jivesoftware.smackx.pubsub.provider.ItemProvider.class);

        //addExtProvider("retract", "http://jabber.org/protocol/pubsub#event",
        //    org.jivesoftware.smackx.pubsub.provider.RetractEventProvider.class);

        //addExtProvider("purge", "http://jabber.org/protocol/pubsub#event",
        //    org.jivesoftware.smackx.pubsub.provider.SimpleNodeProvider.class);

        //<!-- Nick Exchange -->
        //addExtProvider("nick", "http://jabber.org/protocol/nick",
        //    org.jivesoftware.smackx.packet.Nick.Provider.class);

        //<!-- Attention -->
        //addExtProvider("attention", "urn:xmpp:attention:0",
        //    org.jivesoftware.smackx.packet.AttentionExtension.Provider.class);
    }

    /**
     * Adds an IQ provider (must be an instance of IQProvider or Class object that is an IQ)
     * with the specified element name and name space. The provider will override any providers
     * loaded through the classpath.
     *
     * @param elementName the XML element name.
     * @param namespace the XML namespace.
     * @param provider the IQ provider class.
     */
    private static void addProvider(
            String elementName,
            String namespace,
            Class<?> provider)
    {
        // Attempt to load the provider class and then create
        // a new instance if it's an IQProvider. Otherwise, if it's
        // an IQ class, add the class object itself, then we'll use
        // reflection later to create instances of the class.
        try
        {
            // Add the provider to the map.
            if (IQProvider.class.isAssignableFrom(provider))
            {
                ProviderManager.addIQProvider(
                        elementName,
                        namespace,
                        provider.getDeclaredConstructor().newInstance());
            }
            else if (IQ.class.isAssignableFrom(provider))
            {
                ProviderManager.addIQProvider(elementName, namespace, provider);
            }
        }
        catch (Throwable t)
        {
            logger.error("Error adding iq provider.", t);
        }
    }

    /**
     * Adds an extension provider with the specified element name and name space. The provider
     * will override any providers loaded through the classpath. The provider must be either
     * a ExtensionElementProvider instance, or a Class object of a Javabean.
     *
     * @param elementName the XML element name.
     * @param namespace the XML namespace.
     * @param provider the extension provider class.
     */
    public static void addExtProvider(
            String elementName,
            String namespace,
            Class<?> provider)
    {
        // Attempt to load the provider class and then create
        // a new instance if it's a Provider. Otherwise, if it's
        // a ExtensionElement, add the class object itself and
        // then we'll use reflection later to create instances
        // of the class.
        try
        {
            // Add the provider to the map.
            if (ExtensionElementProvider.class.isAssignableFrom(provider))
            {
                ProviderManager.addExtensionProvider(
                        elementName,
                        namespace,
                        provider.getDeclaredConstructor().newInstance());
            }
            else if (ExtensionElement.class.isAssignableFrom(
                    provider))
            {
                ProviderManager.addExtensionProvider(elementName, namespace, provider);
            }
        }
        catch (Throwable t)
        {
            logger.error("Error adding extension provider.", t);
        }
    }
}
