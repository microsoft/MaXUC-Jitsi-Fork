/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.globaldisplaydetails.event;

import java.util.*;

/**
 * The event that contains information about global display details change.
 *
 * @author Yana Stamcheva
 */
public class GlobalDisplayNameChangeEvent
    extends EventObject
{
    /**
     * A default serial version id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates an instance of <tt>GlobalDisplayDetailsEvent</tt>
     *
     * @param source the source of this event
     * @param newDisplayName the new display name
     */
    public GlobalDisplayNameChangeEvent(   Object source,
                                        String newDisplayName)
    {
        super(source);

    }
}
