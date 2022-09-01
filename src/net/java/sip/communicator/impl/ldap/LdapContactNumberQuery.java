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
 * Implementation of <tt>ContactQuery</tt> which queries LDAP for a telephone
 * number.
 */
public class LdapContactNumberQuery extends LdapContactQuery
{
    /**
     * {@inheritDoc}
     */
    protected LdapContactNumberQuery(LdapContactSourceService contactSource,
                                     Pattern query,
                                     int count)
    {
        super(contactSource, query, count);
    }

    @Override
    protected void performSearch(LdapDirectory ldapDir,
                                 LdapQuery query,
                                 LdapListener caller,
                                 LdapSearchSettings settings)
    {
        ldapDir.searchNumber(query, caller, settings);
    }
}
