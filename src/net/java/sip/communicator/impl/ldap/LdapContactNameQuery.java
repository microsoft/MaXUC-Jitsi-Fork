/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.ldap;

import java.util.regex.*;

import net.java.sip.communicator.service.ldap.*;
import net.java.sip.communicator.service.ldap.event.*;

/**
 * Implementation of <tt>ContactQuery</tt> which queries the LDAP directory by
 * name.
 */
public class LdapContactNameQuery
    extends LdapContactQuery
{
    /**
     * {@inheritDoc}
     */
    protected LdapContactNameQuery(LdapContactSourceService contactSource,
                                   Pattern query,
                                   int count)
    {
        super(contactSource, query, count);
    }

    /**
     * Performs an LDAP search for the given query on the given directory,
     * informing the given listener as matches are found.
     *
     * @param ldapDir The directory to query against.
     * @param query The query to perform.
     * @param caller The listener that should be informed of search status
     * updates.
     * @param settings Optional - custom search settings.
     */
    protected void performSearch(LdapDirectory ldapDir,
                                 LdapQuery query,
                                 LdapListener caller,
                                 LdapSearchSettings settings)
    {
        ldapDir.searchPerson(query, caller, settings);
    }
}
