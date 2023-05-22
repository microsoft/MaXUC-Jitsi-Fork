/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.globaldisplaydetails.event;

import java.util.*;

import org.jitsi.service.resources.*;

/**
 * The event that contains information about global avatar change.
 *
 * @author Yana Stamcheva
 */
public class GlobalAvatarChangeEvent
    extends EventObject
{
    /**
     * A default serial version id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The avatar this event is about.
     */
    private BufferedImageFuture avatar;

    /**
     * Creates an instance of <tt>GlobalDisplayDetailsEvent</tt>
     *
     * @param source the source of this event
     * @param newAvatar the new avatar
     */
    public GlobalAvatarChangeEvent( Object source,
        BufferedImageFuture newAvatar)
    {
        super(source);

        this.avatar = newAvatar;
    }

    /**
     * Returns the new global avatar.
     *
     * @return a byte array representing the new global avatar
     */
    public BufferedImageFuture getNewAvatar()
    {
        return avatar;
    }
}
