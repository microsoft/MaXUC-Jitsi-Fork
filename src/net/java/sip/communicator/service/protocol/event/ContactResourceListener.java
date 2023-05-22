/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol.event;

/**
 * The <tt>ContactResourceListener</tt> listens for events related to
 * <tt>ContactResource</tt>-s. It is notified each time a
 * <tt>ContactResource</tt> has been added, removed or modified.
 *
 * @author Yana Stamcheva
 */
public interface ContactResourceListener
{
    /**
     * Called when a new <tt>ContactResource</tt> has been added to the list
     * of available <tt>Contact</tt> resources.
     *
     * @param event the <tt>ContactResourceEvent</tt> that notified us
     */
    void contactResourceAdded(ContactResourceEvent event);

    /**
     * Called when a <tt>ContactResource</tt> has been removed to the list
     * of available <tt>Contact</tt> resources.
     *
     * @param event the <tt>ContactResourceEvent</tt> that notified us
     */
    void contactResourceRemoved(ContactResourceEvent event);

    /**
     * Called when a <tt>ContactResource</tt> in the list of available
     * <tt>Contact</tt> resources has been modified.
     *
     * @param event the <tt>ContactResourceEvent</tt> that notified us
     */
    void contactResourceModified(ContactResourceEvent event);
}
