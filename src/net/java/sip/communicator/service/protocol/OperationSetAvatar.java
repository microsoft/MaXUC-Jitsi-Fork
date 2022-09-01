/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.service.protocol.event.*;

import org.jitsi.service.resources.*;

/**
 * This interface is an extension of the operation set, meant to be implemented
 * by protocols that support user avatar.
 *
 * @author Damien Roth
 */
public interface OperationSetAvatar
    extends OperationSet
{
    /**
     * Returns the maximum width of the avatar. This method should return 0
     * (zero) if there is no maximum width.
     *
     * @return the maximum width of the avatar
     */
    int getMaxWidth();

    /**
     * Returns the maximum height of the avatar. This method should return 0
     * (zero) if there is no maximum height.
     *
     * @return the maximum height of the avatar
     */
    int getMaxHeight();

    /**
     * Returns the maximum size of the avatar. This method should return 0
     * (zero) if there is no maximum size.
     *
     * @return the maximum size of the avatar
     */
    int getMaxSize();

    /**
     * Defines a new avatar for this protocol
     *
     * @param avatar
     *            the new avatar
     * @return true if the operation succeeded
     * @throws OperationFailedException
     */
    boolean setAvatar(BufferedImageFuture avatar) throws OperationFailedException;

    /**
     * Returns the current avatar of this protocol. May return null if the
     * account has no avatar
     *
     * @return avatar's bytes or null if no avatar set
     */
    BufferedImageFuture getAvatar();

    /**
     * Registers a listener that would receive events upon avatar changes.
     *
     * @param listener
     *            a AvatarListener that would receive events upon avatar
     *            changes.
     */
    void addAvatarListener(AvatarListener listener);

    /**
     * Removes the specified group change listener so that it won't receive any
     * further events.
     *
     * @param listener
     *            the AvatarListener to remove
     */
    void removeAvatarListener(AvatarListener listener);
}
