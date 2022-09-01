// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.contactsource;

/**
 * The <tt>SourceContactChangeListener</tt> is notified whenever a
 * SourceContact is added or updated.
 */
public interface SourceContactChangeListener
{
    /**
     * Indicates a <tt>SourceContact</tt> has been updated.
     *
     * @param sourceContact the <tt>SourceContact</tt> that has been updated.
     * @param isRefresh If true, the existing SourceContact should be refreshed
     * rather than replaced
     */
    void sourceContactUpdated(SourceContact sourceContact, boolean isRefresh);

    /**
     * Indicates a <tt>SourceContact</tt> has been added.
     *
     * @param sourceContact the <tt>SourceContact</tt> that has been added.
     */
    void sourceContactAdded(SourceContact sourceContact);
 }
