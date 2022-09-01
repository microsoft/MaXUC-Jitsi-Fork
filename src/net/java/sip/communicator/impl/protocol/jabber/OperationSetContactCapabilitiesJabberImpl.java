/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.caps.EntityCapsManager;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import net.java.sip.communicator.impl.protocol.jabber.extensions.caps.PropertyEntityCapsPersistentCache;
import net.java.sip.communicator.impl.protocol.jabber.extensions.messagecorrection.MessageCorrectionExtension;
import net.java.sip.communicator.service.protocol.AbstractOperationSetContactCapabilities;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.OperationSet;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
import net.java.sip.communicator.service.protocol.OperationSetMessageCorrection;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
import net.java.sip.communicator.service.protocol.event.ContactCapabilitiesEvent;
import net.java.sip.communicator.util.JitsiStringUtils;
import net.java.sip.communicator.util.Logger;

/**
 * Represents an <tt>OperationSet</tt> to query the <tt>OperationSet</tt>s
 * supported for a specific Jabber <tt>Contact</tt>. The <tt>OperationSet</tt>s
 * reported as supported for a specific Jabber <tt>Contact</tt> are considered
 * by the associated protocol provider to be capabilities possessed by the
 * Jabber <tt>Contact</tt> in question.
 *
 * @author Lyubomir Marinov
 * @author Yana Stamcheva
 */
public class OperationSetContactCapabilitiesJabberImpl
    extends AbstractOperationSetContactCapabilities<ProtocolProviderServiceJabberImpl>
    implements StanzaListener
{
    /**
     * The <tt>Logger</tt> used by the
     * <tt>OperationSetContactCapabilitiesJabberImpl</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(OperationSetContactCapabilitiesJabberImpl.class);

    /**
     * The list of <tt>OperationSet</tt> capabilities presumed to be supported
     * by a <tt>Contact</tt> when it is offline.
     */
    private static final Set<Class<? extends OperationSet>>
        OFFLINE_OPERATION_SETS
            = new HashSet<>();

    /**
     * The <tt>Map</tt> which associates specific <tt>OperationSet</tt> classes
     * with the features to be supported by a <tt>Contact</tt> in order to
     * consider the <tt>Contact</tt> to possess the respective
     * <tt>OperationSet</tt> capability.
     */
    private static final Map<Class<? extends OperationSet>, String[]>
        OPERATION_SETS_TO_FEATURES
            = new HashMap<>();

    /**
     * Contact capabilities update event will be sent when
     * either a presence with capabilities or a discovery info is received.
     */
    private static final StanzaFilter CAPS_UPDATE_FILTER =
        new AndFilter(
            new StanzaTypeFilter(Presence.class),
            new StanzaExtensionFilter(
                EntityCapsManager.ELEMENT, EntityCapsManager.NAMESPACE));

    static
    {
        OFFLINE_OPERATION_SETS.add(OperationSetBasicInstantMessaging.class);
        OFFLINE_OPERATION_SETS.add(OperationSetMessageCorrection.class);

        OPERATION_SETS_TO_FEATURES.put(
                OperationSetMessageCorrection.class,
                new String[]
                {
                    MessageCorrectionExtension.NAMESPACE
                });
    }

    /**
     * Initializes a new <tt>OperationSetContactCapabilitiesJabberImpl</tt>
     * instance which is to be provided by a specific
     * <tt>ProtocolProviderServiceJabberImpl</tt>.
     *
     * @param parentProvider the <tt>ProtocolProviderServiceJabberImpl</tt>
     * which will provide the new instance
     */
    public OperationSetContactCapabilitiesJabberImpl(
            ProtocolProviderServiceJabberImpl parentProvider)
    {
        super(parentProvider);

        EntityCapsManager.setPersistentCache(
                new PropertyEntityCapsPersistentCache());
    }

    /**
     * Gets the <tt>OperationSet</tt> corresponding to the specified
     * <tt>Class</tt> and supported by the specified <tt>Contact</tt>. If the
     * returned value is non-<tt>null</tt>, it indicates that the
     * <tt>Contact</tt> is considered by the associated protocol provider to
     * possess the <tt>opsetClass</tt> capability. Otherwise, the associated
     * protocol provider considers <tt>contact</tt> to not have the
     * <tt>opsetClass</tt> capability.
     *
     * @param <U> the type extending <tt>OperationSet</tt> for which the
     * specified <tt>contact</tt> is to be checked whether it possesses it as a
     * capability
     * @param contact the <tt>Contact</tt> for which the <tt>opsetClass</tt>
     * capability is to be queried
     * @param opsetClass the <tt>OperationSet</tt> <tt>Class</tt> for which the
     * specified <tt>contact</tt> is to be checked whether it possesses it as a
     * capability
     * @param online <tt>true</tt> if <tt>contact</tt> is online; otherwise,
     * <tt>false</tt>
     * @return the <tt>OperationSet</tt> corresponding to the specified
     * <tt>opsetClass</tt> which is considered by the associated protocol
     * provider to be possessed as a capability by the specified
     * <tt>contact</tt>; otherwise, <tt>null</tt>
     * @see AbstractOperationSetContactCapabilities#getOperationSet(Contact,
     * Class)
     */
    @Override
    protected <U extends OperationSet> U getOperationSet(
            Contact contact,
            Class<U> opsetClass,
            boolean online)
    {
        Jid jid = jidFromContact(contact);

        if (jid == null)
        {
            return null;
        }

        return getOperationSet(jid, opsetClass, online);
    }

    /**
     * Gets the <tt>OperationSet</tt>s supported by a specific <tt>Contact</tt>.
     * The returned <tt>OperationSet</tt>s are considered by the associated
     * protocol provider to capabilities possessed by the specified
     * <tt>contact</tt>.
     *
     * @param contact the <tt>Contact</tt> for which the supported
     * <tt>OperationSet</tt> capabilities are to be retrieved
     * @param online <tt>true</tt> if <tt>contact</tt> is online; otherwise,
     * <tt>false</tt>
     * @return a <tt>Map</tt> listing the <tt>OperationSet</tt>s considered by
     * the associated protocol provider to be supported by the specified
     * <tt>contact</tt> (i.e. to be possessed as capabilities). Each supported
     * <tt>OperationSet</tt> capability is represented by a <tt>Map.Entry</tt>
     * with key equal to the <tt>OperationSet</tt> class name and value equal to
     * the respective <tt>OperationSet</tt> instance
     * @see AbstractOperationSetContactCapabilities#getSupportedOperationSets(
     * Contact)
     */
    @Override
    protected Map<String, OperationSet> getSupportedOperationSets(
        Contact contact,
        boolean online)
    {
        Jid jid = jidFromContact(contact);

        if (jid == null)
        {
            return null;
        }

        return getSupportedOperationSets(jid, online);
    }

    private Jid jidFromContact(Contact contact)
    {
        try
        {
            Jid jid = parentProvider.getFullJid(contact);

            if (jid != null)
            {
                return jid;
            }

            return JidCreate.from(contact.getAddress());
        }
        catch (XmppStringprepException e)
        {
            return null;
        }
    }

    /**
     * Gets the <tt>OperationSet</tt>s supported by a specific <tt>Contact</tt>.
     * The returned <tt>OperationSet</tt>s are considered by the associated
     * protocol provider to capabilities possessed by the specified
     * <tt>contact</tt>.
     *
     * @param jid the <tt>Contact</tt> for which the supported
     * <tt>OperationSet</tt> capabilities are to be retrieved
     * @param online <tt>true</tt> if <tt>contact</tt> is online; otherwise,
     * <tt>false</tt>
     * @return a <tt>Map</tt> listing the <tt>OperationSet</tt>s considered by
     * the associated protocol provider to be supported by the specified
     * <tt>contact</tt> (i.e. to be possessed as capabilities). Each supported
     * <tt>OperationSet</tt> capability is represented by a <tt>Map.Entry</tt>
     * with key equal to the <tt>OperationSet</tt> class name and value equal to
     * the respective <tt>OperationSet</tt> instance
     * @see AbstractOperationSetContactCapabilities#getSupportedOperationSets(
     * Contact)
     */
    @SuppressWarnings("unchecked")
    private Map<String, OperationSet> getSupportedOperationSets(Jid jid,
                                                                boolean online)
    {
        Map<String, OperationSet> supportedOperationSets
            = parentProvider.getSupportedOperationSets();

        int supportedOperationSetCount = supportedOperationSets.size();
        Map<String, OperationSet> contactSupportedOperationSets
            = new HashMap<>(supportedOperationSetCount);

        if (supportedOperationSetCount != 0)
        {
            for (Map.Entry<String, OperationSet> supportedOperationSetEntry
                    : supportedOperationSets.entrySet())
            {
                String opsetClassName = supportedOperationSetEntry.getKey();
                Class<? extends OperationSet> opsetClass;

                try
                {
                    opsetClass
                        = (Class<? extends OperationSet>)
                            Class.forName(opsetClassName);
                }
                catch (ClassNotFoundException cnfex)
                {
                    opsetClass = null;
                    logger.error(
                            "Failed to get OperationSet class for name: "
                                + opsetClassName,
                            cnfex);
                }

                if (opsetClass != null)
                {
                    OperationSet opset
                        = getOperationSet(jid, opsetClass, online);

                    if (opset != null)
                    {
                        contactSupportedOperationSets.put(
                                opsetClassName,
                                opset);
                    }
                }
            }
        }
        return contactSupportedOperationSets;
    }

    /**
     * Gets the <tt>OperationSet</tt> corresponding to the specified
     * <tt>Class</tt> and supported by the specified <tt>Contact</tt>. If the
     * returned value is non-<tt>null</tt>, it indicates that the
     * <tt>Contact</tt> is considered by the associated protocol provider to
     * possess the <tt>opsetClass</tt> capability. Otherwise, the associated
     * protocol provider considers <tt>contact</tt> to not have the
     * <tt>opsetClass</tt> capability.
     *
     * @param <U> the type extending <tt>OperationSet</tt> for which the
     * specified <tt>contact</tt> is to be checked whether it possesses it as a
     * capability
     * @param jid the Jabber id for which we're checking supported operation
     * sets
     * @param opsetClass the <tt>OperationSet</tt> <tt>Class</tt> for which the
     * specified <tt>contact</tt> is to be checked whether it possesses it as a
     * capability
     * @param online <tt>true</tt> if <tt>contact</tt> is online; otherwise,
     * <tt>false</tt>
     * @return the <tt>OperationSet</tt> corresponding to the specified
     * <tt>opsetClass</tt> which is considered by the associated protocol
     * provider to be possessed as a capability by the specified
     * <tt>contact</tt>; otherwise, <tt>null</tt>
     * @see AbstractOperationSetContactCapabilities#getOperationSet(Contact,
     * Class)
     */
    private <U extends OperationSet> U getOperationSet(Jid jid,
                                                       Class<U> opsetClass,
                                                       boolean online)
    {
        U opset = parentProvider.getOperationSet(opsetClass);

        if (opset == null)
            return null;

        /*
         * If the specified contact is offline, don't query its features (they
         * should fail anyway).
         */
        if (!online)
            return OFFLINE_OPERATION_SETS.contains(opsetClass) ? opset : null;

        /*
         * If we know the features required for the support of opsetClass, check
         * whether the contact supports them. Otherwise, presume the contact
         * possesses the opsetClass capability in light of the fact that we miss
         * any knowledge of the opsetClass whatsoever.
         */
        if (OPERATION_SETS_TO_FEATURES.containsKey(opsetClass))
        {
            String[] features = OPERATION_SETS_TO_FEATURES.get(opsetClass);

            /*
             * Either we've completely disabled the opsetClass capability by
             * mapping it to the null list of features or we've mapped it to an
             * actual list of features which are to be checked whether the
             * contact supports them.
             */
            if ((features == null)
                    || ((features.length != 0)
                            && !parentProvider.isFeatureListSupported(
                                    jid,
                                    features)))
            {
                opset = null;
            }
        }

        return opset;
    }

    public void addCapabilitiesUpdateListener()
    {
        parentProvider.getConnection().addStanzaListener(this,
            CAPS_UPDATE_FILTER);
    }

    public void removeCapabilitiesUpdateListener()
    {
        parentProvider.getConnection().removeStanzaListener(this);
    }

    @Override
    public void processStanza(Stanza stanza)
    {
        if (stanza instanceof Presence)
        {
            Presence presence = (Presence) stanza;
            fireContactCapabilitiesChangedForPresence(
                    presence.getFrom(), presence.isAvailable());
        }
    }

    /**
     * Fires an event that contact capabilities has changed
     * when a presence with capabilities received.
     * @param user the user to search for its contact.
     * @param online true if user is online
     */
    private void fireContactCapabilitiesChangedForPresence(Jid user, boolean online)
    {
        OperationSetPresence opsetPresence
            = parentProvider.getOperationSet(OperationSetPresence.class);

        if (opsetPresence != null)
        {
            String userID = JitsiStringUtils.parseBareAddress(user.toString());
            Contact contact = opsetPresence.findContactByID(userID);

            // If the contact isn't null and is online we try to discover the
            // new set of operation sets and to notify interested parties.
            // Otherwise, we ignore the event.
            if (contact != null)
            {
                if (online)
                {
                    // when going online we have received a presence
                    // and make sure we discover this particular jid
                    // for getSupportedOperationSets
                    fireContactCapabilitiesEvent(
                        contact,
                        ContactCapabilitiesEvent.SUPPORTED_OPERATION_SETS_CHANGED,
                        getSupportedOperationSets(user, online));
                }
                else
                {
                    // when offline, we use the contact, and selecting
                    // the most connected jid
                    // for getSupportedOperationSets
                    fireContactCapabilitiesEvent(
                        contact,
                        ContactCapabilitiesEvent.SUPPORTED_OPERATION_SETS_CHANGED,
                        getSupportedOperationSets(contact));
                }
            }
        }
    }
}
