// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber.extensions.caps;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.provider.IqProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smackx.caps.cache.EntityCapsPersistentCache;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;

import net.java.sip.communicator.impl.protocol.jabber.JabberActivator;
import net.java.sip.communicator.util.Logger;
import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.util.StringUtils;

/**
 * Implementation of a persistent cache for Smack's EntityCapsManager
 * using <tt>ConfigurationService</tt> properties as a storage.
 */
public class PropertyEntityCapsPersistentCache
        implements EntityCapsPersistentCache
{
    private static final Logger sLog
            = Logger.getLogger(PropertyEntityCapsPersistentCache.class);

    /**
     * The prefix of the <tt>ConfigurationService</tt> properties
     * which persist {@link DiscoverInfo}.
     */
    private static final String CAPS_PROPERTY_NAME_PREFIX =
        "net.java.sip.communicator.impl.protocol.jabber.extensions.caps."
        + "EntityCapsManager.CAPS";

    private final IqProvider<IQ> mDiscoverInfoProvider;
    private final IqData mCommonIqData;

    /**
     * Creates a new PropertyEntityCapsPersistentCache instance.
     */
    public PropertyEntityCapsPersistentCache()
    {
        mDiscoverInfoProvider = ProviderManager.getIQProvider(
                "query",
                "http://jabber.org/protocol/disco#info");
        mCommonIqData = IqData.buildIqData(null);
    }

    @Override
    public void addDiscoverInfoByNodePersistent(String nodeVer,
                                                DiscoverInfo discoverInfo)
    {
        ConfigurationService configurationService =
                JabberActivator.getConfigurationService();

        if (configurationService != null)
        {
            configurationService.user()
                    .setProperty(getCapsPropertyName(nodeVer),
                                 discoverInfo.toXML());
        }
    }

    @Override
    public DiscoverInfo lookup(String nodeVer)
    {
        ConfigurationService configurationService =
                JabberActivator.getConfigurationService();

        if (configurationService != null)
        {
            String capsPropertyName = getCapsPropertyName(nodeVer);
            String xml = configurationService.user()
                    .getString(capsPropertyName);

            if (!StringUtils.isNullOrEmpty(xml))
            {
                try
                {
                    XmlPullParser parser = PacketParserUtils.getParserFor(xml);
                    parser.next();

                    return (DiscoverInfo) mDiscoverInfoProvider
                            .parse(parser, mCommonIqData);
                }
                catch (Exception ex)
                {
                    sLog.error(
                            "Failed to parse DiscoveryInfo from property "
                            + capsPropertyName, ex);
                }
            }
        }

        return null;
    }

    @Override
    public void emptyCache()
    {
        ConfigurationService configurationService =
                JabberActivator.getConfigurationService();

        if (configurationService != null)
        {
            configurationService.user()
                    .removeProperty(CAPS_PROPERTY_NAME_PREFIX);
        }
    }

    /**
     * Gets the name of the property in the <tt>ConfigurationService</tt>
     * which is or is to be associated with a specific <tt>nodeVer</tt> value.
     *
     * @param nodeVer the value for which the associated
     *                property name is to be returned
     * @return the name of the property in the <tt>ConfigurationService</tt>
     * which is or is to be associated with a specific <tt>nodeVer</tt> value
     */
    private static String getCapsPropertyName(String nodeVer)
    {
        return CAPS_PROPERTY_NAME_PREFIX + "." + nodeVer;
    }
}
