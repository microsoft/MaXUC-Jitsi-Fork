// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.contactlist;

import javax.swing.event.ChangeListener;
import net.java.sip.communicator.service.protocol.Contact;

/**
 * Service which tracks the presence status of IM contacts that we wish to be
 * notified of, when they become available for calls. When they become
 * available, we alert the user with a dialog.
 */
public interface NotifyWhenAvailableService
{
    /**
     * Start watching the presence of a contact, if they are not already
     * available.
     */
    void startWatchingContact(Contact contact);

    /**
     * Stop watching a contact, if we are tracking their presence.
     */
    void stopWatchingContact(Contact contact);

    /**
     * Return true if contact's presence is being watched, otherwise false.
     */
    boolean isContactBeingWatched(Contact contact);

    /**
     * Add a listener, which will be notified when we start/stop watching a
     * contact.
     */
    void addWatchingStateListener(ChangeListener listener);

    /**
     * Remove a listener so that they are no longer notified when we start/stop
     * watching a contact.
     */
    void removeWatchingStateListener(ChangeListener listener);
}
