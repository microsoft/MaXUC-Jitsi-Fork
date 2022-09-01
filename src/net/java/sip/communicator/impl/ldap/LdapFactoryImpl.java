/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.ldap;

import net.java.sip.communicator.service.ldap.*;

/**
 * Implementation of The LdapFactory, used to
 * create LdapDirectory-s, LdapDirectorySettings, LdapQuery, ...
 *
 * @author Sebastien Mazy
 */
public class LdapFactoryImpl
    implements LdapFactory
{
    /**
     * Required by LdapFactory interface.
     * Creates an LdapDirectory based on the provided settings.
     * This method will not modify the <tt>settings</tt>
     * or save a reference to it, but may save a clone.
     *
     * @param settings settings for this new server
     *
     * @return a reference to the created LdapDirectory
     *
     * @see LdapDirectorySettings
     * @see LdapFactory#createServer
     */
    public LdapDirectory createServer(LdapDirectorySettings settings)
        throws IllegalArgumentException
    {
        return new LdapDirectoryImpl(settings);
    }

    /**
     * Required by LdapFactory interface.
     * Return a new instance of LdapDirectorySettings,
     * a wrapper around a directory settings
     *
     * @return a new instance of LdapDirectorySettings
     *
     * @see LdapDirectorySettings
     * @see LdapFactory#createServerSettings
     */
    public LdapDirectorySettings createServerSettings()
    {
        return new LdapDirectorySettingsImpl();
    }

    /**
     * Required by LdapFactory interface.
     * Returns an LDAP query, ready to be sent to an LdapDirectory
     *
     * @param query the query string, e.g. "John Doe"
     *
     * @return an LDAP query, ready to be sent to an LdapDirectory
     *
     * @see LdapQuery
     * @see LdapFactory#createQuery
     */
    public LdapQuery createQuery(String query)
    {
        return new LdapQueryImpl(query);
    }

    /**
     * Returns an LdapSearchSettings, to use when performing a search
     *
     * @return an LdapSearchSettings, to use when performing a search
     */
    public LdapSearchSettings createSearchSettings()
    {
        return new LdapSearchSettingsImpl();
    }
}
