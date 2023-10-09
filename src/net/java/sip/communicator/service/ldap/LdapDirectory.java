/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.ldap;

import net.java.sip.communicator.service.ldap.event.*;

/**
 * LdapDirectory is the "core" object of the service, which
 * should be used to perform LDAP queries.
 * It is comparable in order to display LdapDirectory(s)
 * in alphabetic order in the UI.
 *
 * @author Sebastien Mazy
 */
public interface LdapDirectory extends LdapConstants
{
    /**
     * Returns an LdapDirectorySettings object containing
     * a copy of the settings of this server
     *
     * @return a copy of this server settings
     *
     * @see LdapDirectorySettings
     */
    LdapDirectorySettings getSettings();

    /**
     * Searches a person in the directory, based on a search string.
     * Since that method might take time to process, it should be
     * implemented asynchronously and send the results (LdapPersonFound)
     * with an LdapEvent to its listeners
     *
     * @param query assumed name (can be partial) of the person searched
     * e.g. "john", "doe", "john doe"
     * @param caller the LdapListener which called the method and will
     * receive results.
     * @param searchSettings custom settings for this search, null if you
     * want to stick with the defaults
     *
     * @see LdapDirectory#searchPerson
     * @see LdapPersonFound
     * @see LdapEvent
     */
    void searchPerson(final LdapQuery query, final LdapListener caller,
                      LdapSearchSettings searchSettings);

    /**
     * Searches a number in the directory, based on a search string.
     * Since that method might take time to process, it should be
     * implemented asynchronously and send the results (LdapPersonFound)
     * with an LdapEvent to its listeners
     *
     * @param query number to search - can be partial.
     * @param caller the LdapListener which called the method and will
     * receive results.
     * @param searchSettings custom settings for this search, null if you
     * want to stick with the defaults
     *
     * @see LdapDirectory#searchPerson
     * @see LdapPersonFound
     * @see LdapEvent
     */
    void searchNumber(final LdapQuery query, final LdapListener caller,
                      LdapSearchSettings searchSettings);
}
