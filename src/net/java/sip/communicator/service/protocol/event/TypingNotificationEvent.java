/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications.TypingState;

/**
 * <tt>TypingNotificationEvent</tt>s are delivered upon reception of a
 * corresponding message from a remote contact.
 * <tt>TypingNotificationEvent</tt>s contain a state id, identifying the exact
 * typing event that has occurred (a user has started or stopped typing at us),
 * the source <tt>Contact</tt> that generated the event and others.
 * @author Emil Ivov
 */
public class TypingNotificationEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private TypingState typingState = TypingState.UNKNOWN;

    /**
     * Creates a TypingNotificationEvent with the specified parameters.
     * <p>
     * @param sourceContact the <tt>Contact</tt> that has sent the notification.
     * @param state
     */
    public TypingNotificationEvent(Contact sourceContact, TypingState state)
    {
        super(sourceContact);
        this.typingState = state;
    }

    /**
     * Returns the typing state that this <tt>TypingNotificationEvent</tt> is
     * carrying.
     * @return one of the <tt>STATE_XXX int</tt>s indicating the typing state
     * that this notification is about.
     */
    public TypingState getTypingState()
    {
        return typingState;
    }

    /**
     * Returns a reference to the <tt>Contact</tt> that has sent this event.
     * @return a reference to the <tt>Contact</tt> whose typing state we're
     * being notified about.
     */
    public Contact getSourceContact()
    {
        return (Contact)getSource();
    }

    /**
     * Returns a String representation of this EventObject.
     *
     * @return A String representation of this EventObject.
     */
    public String toString()
    {
        return new StringBuffer("TypingNotificationEvent[from=")
            .append(getSourceContact().getDisplayName())
                .append(" state="+getTypingState()).toString();
    }
}
