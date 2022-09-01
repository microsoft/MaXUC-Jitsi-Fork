/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.service.contactsource.*;

/**
 * The user interface representation of a contact source.
 *
 * @author Yana Stamcheva
 */
public interface UIContactSource
{
    /**
     * Returns the UI group for this contact source. There's only one group
     * descriptor per external source.
     *
     * @return the group descriptor
     */
    UIGroup getUIGroup();

    /**
     * Returns the <tt>UIContact</tt> corresponding to the given
     * <tt>sourceContact</tt>.
     *
     * @param sourceContact the <tt>SourceContact</tt>, for which we search a
     * corresponding <tt>UIContact</tt>
     * @param mclSource the <tt>MetaContactListSource</tt>
     * @return the <tt>UIContact</tt> corresponding to the given
     * <tt>sourceContact</tt>
     */
    UIContact createUIContact(SourceContact sourceContact,
                              MetaContactListSource mclSource);

    /**
     * Removes the <tt>UIContact</tt> from the given <tt>sourceContact</tt>.
     * @param sourceContact the <tt>SourceContact</tt>, which corresponding UI
     * contact we would like to remove
     */
    void removeUIContact(SourceContact sourceContact);

    /**
     * Returns the <tt>UIContact</tt> corresponding to the given
     * <tt>SourceContact</tt>.
     * @param sourceContact the <tt>SourceContact</tt>, which corresponding UI
     * contact we're looking for
     * @return the <tt>UIContact</tt> corresponding to the given
     * <tt>MetaContact</tt>
     */
    UIContact getUIContact(SourceContact sourceContact);

    /**
     * Returns the corresponding <tt>ContactSourceService</tt>.
     *
     * @return the corresponding <tt>ContactSourceService</tt>
     */
    ContactSourceService getContactSourceService();
}
