/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Notifies interested parties in <tt>ConferenceMember</tt>s sound level changes.
 * When a <tt>CallPeer</tt> is participating in the conference also as a
 * <tt>ConferenceMember</tt> its sound level would be included in the map of
 * received levels.
 *
 * @author Yana Stamcheva
 */
public class ConferenceMembersSoundLevelEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The mapping of <tt>ConferenceMember</tt>s to sound levels. It is
     * presumed that all <tt>ConferenceMember</tt>s not contained in the map has
     * a 0 sound level.
     */
    private final Map<ConferenceMember, Integer> levels;

    /**
     * Creates an instance of <tt>ConferenceMembersSoundLevelEvent</tt> for the
     * given <tt>callPeer</tt> by indicating the mapping of
     * <tt>ConferenceMember</tt>s and sound levels.
     *
     * @param callPeer the <tt>CallPeer</tt> for which this event occurred
     * @param levels the mapping of <tt>ConferenceMember</tt>s to sound levels
     */
    public ConferenceMembersSoundLevelEvent(
        CallPeer callPeer,
        Map<ConferenceMember, Integer> levels)
    {
        super(callPeer);

        this.levels = levels;
    }

    /**
     * Returns the mapping of <tt>ConferenceMember</tt>s to sound levels. It is
     * presumed that all <tt>ConferenceMember</tt>s not contained in the map has
     * a 0 sound level.
     * @return the mapping of <tt>ConferenceMember</tt>s to sound levels
     */
    public Map<ConferenceMember, Integer> getLevels()
    {
        return levels;
    }
}
