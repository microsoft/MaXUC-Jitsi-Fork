/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.contactsource;

import java.util.regex.*;

/**
 * The <tt>ExtendedContactSourceService</tt> as its name states it, extends the
 * <tt>ContactSourceService</tt> in order to provide the possibility to query
 * a contact source by specifying a given <tt>Pattern</tt>. This interface is
 * meant to be implemented by contact sources, where it is possible to match
 * source results directly through a given <tt>Pattern</tt>, i.e. by using:<br>
 * <code>Matcher matcher = pattern.matcher(myString);
 *       if(matcher.find())
 *          ....
 * </code>
 * The advantages of passing a <tt>Pattern</tt> over a <tt>String</tt> are that
 * a <tt>Pattern</tt> can use a predefined regular expression and
 * can define certain properties like Pattern.CASE_INSENSITIVE,
 * Pattern.UNICODE_CASE, etc., which could be important for the search.
 *
 * @author Yana Stamcheva
 */
public interface ExtendedContactSourceService
    extends ContactSourceService
{
    /**
     * Queries this search source for the given <tt>searchPattern</tt>,
     * returning those contacts with names matching the pattern.
     * @param queryPattern the pattern to search for
     * @return the created query
     */
    ContactQuery queryContactSource(Pattern queryPattern);

    /**
     * Queries this search source for the given <tt>queryPattern</tt>,
     * returning those contacts who have a phone number matching the pattern.
     * @param queryPattern The pattern to search for.
     * @return The created query.
     */
    ContactQuery querySourceForNumber(Pattern queryPattern);

    /**
     * Returns the global phone number prefix to be used when calling contacts
     * from this contact source.
     *
     * @return the global phone number prefix
     */
    String getPhoneNumberPrefix();
}
