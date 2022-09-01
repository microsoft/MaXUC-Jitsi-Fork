/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.ldap;

/**
 * LdapSearchSettings is a wrapper around all settings a directory search.
 * Each setting has a getter, a setter, and isEnabled method so that the
 * LdapServer can choose defaults when a value isn't set.
 *
 * @author Sebastien Mazy
 */
public interface LdapSearchSettings
    extends LdapConstants
{
    /**
     * If results set has reach maximum number.
     * @return true if results set has reach maximum number, false otherwise
     */
    boolean isMaxResultsSet();

    /**
     * Sets the maximum number of results to fetch from the
     * directory when performing the search query.
     *
     * @param maxResults the maximum number of results
     */
    void setMaxResults(int maxResults);

    /**
     * Returns the maximum number of results to fetch from the
     * directory when performing the search query. The returned
     * value makes sense only if isMaxResultsSet is true.
     *
     * @return the maximum number of results
     */
    int getMaxResults();

    /**
     * If scope has been set.
     *
     * @return true if scope has been set, false otherwise
     */
    boolean isScopeSet();

    /**
     * Sets a custom search scope.
     *
     * @param scope a custom search scope
     */
    void setScope(Scope scope);

    /**
     * Returns the custom search scope. The
     * returned value makes sense only if
     * isScopeSet is true.
     *
     * @return the custom search scope
     */
    Scope getScope();

    /**
     * If delay has been configured.
     *
     * @return true if delay has been configured, false otherwise
     */
    boolean isDelaySet();

    /**
     * Sets a delay before performing the "real" search.
     * (to give a chance to cancel the search using LdapQuery.setState()
     * before the search has actually been started)
     *
     * @param delay a delay before the actual search
     */
    void setDelay(int delay);

    /**
     * Returns the delay before the search. This value
     * makes sense only if isDelaySet() is true.
     *
     * @return the delay before the search
     */
    int getDelay();
}
