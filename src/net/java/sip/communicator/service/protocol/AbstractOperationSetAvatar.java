/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.ImageDetail;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;

/**
 * Represents a default implementation of {@link OperationSetAvatar} in order to
 * make it easier for implementers to provide complete solutions while focusing
 * on implementation-specific details.
 *
 * @author Damien Roth
 */
public abstract class AbstractOperationSetAvatar<T extends ProtocolProviderService>
    implements OperationSetAvatar
{
    /**
     * The <tt>Logger</tt> used by the <tt>AbstractOperationSetAvatar</tt> class
     * and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(AbstractOperationSetAvatar.class);

    /**
     * The maximum avatar width. Zero mean no maximum
     */
    private final int maxWidth;

    /**
     * The maximum avatar height. Zero mean no maximum
     */
    private final int maxHeight;

    /**
     * The maximum avatar size. Zero mean no maximum
     */
    private final int maxSize;

    /**
     * The provider that created us.
     */
    private final T parentProvider;

    private final OperationSetServerStoredAccountInfo accountInfoOpSet;

    /**
     * The list of listeners interested in <tt>AvatarEvent</tt>s.
     */
    private final List<AvatarListener> avatarListeners
        = new ArrayList<>();

    protected AbstractOperationSetAvatar(T parentProvider,
            OperationSetServerStoredAccountInfo accountInfoOpSet, int maxWidth,
            int maxHeight, int maxSize)
    {
        this.parentProvider = parentProvider;
        this.accountInfoOpSet = accountInfoOpSet;

        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.maxSize = maxSize;
    }

    public int getMaxWidth()
    {
        return this.maxWidth;
    }

    public int getMaxHeight()
    {
        return this.maxHeight;
    }

    public int getMaxSize()
    {
        return this.maxSize;
    }

    public BufferedImageFuture getAvatar()
    {
        return AccountInfoUtils.getImage(this.accountInfoOpSet);
    }

    public boolean setAvatar(BufferedImageFuture avatar) throws OperationFailedException
    {
        ImageDetail oldDetail = null;
        ImageDetail newDetail = new ImageDetail("avatar", avatar);

        Iterator<GenericDetail> imageDetails = this.accountInfoOpSet
                .getDetails(ServerStoredDetails.ImageDetail.class);
        if (imageDetails.hasNext())
        {
            oldDetail = (ImageDetail) imageDetails.next();
        }

        boolean result = false;
        try
        {
            if (oldDetail == null)
            {
                this.accountInfoOpSet.addDetail(newDetail);
                result = true;
            }
            else
            {
                result = this.accountInfoOpSet.replaceDetail(oldDetail, newDetail);
            }
        }
        catch (OperationFailedException e)
        {
            logger.warn("Unable to set new avatar", e);
            throw e;
        }

        if (result)
        {
            fireAvatarChanged(avatar);
        }
        else
        {
            logger.warn("Unable to set new avatar; detail change returned false");
        }

        return result;
    }

    public void addAvatarListener(AvatarListener listener)
    {
        synchronized (this.avatarListeners)
        {
            if (!this.avatarListeners.contains(listener))
                this.avatarListeners.add(listener);
        }
    }

    public void removeAvatarListener(AvatarListener listener)
    {
        synchronized (this.avatarListeners)
        {
            if (this.avatarListeners.contains(listener))
                this.avatarListeners.remove(listener);
        }
    }

    /**
     * Notifies all registered listeners of the new event.
     *
     * @param newAvatar
     *            the new avatar
     */
    protected void fireAvatarChanged(BufferedImageFuture newAvatar)
    {
        Collection<AvatarListener> listeners;
        synchronized (this.avatarListeners)
        {
            listeners = new ArrayList<>(this.avatarListeners);
        }

        if (!listeners.isEmpty())
        {
            AvatarEvent event
                = new AvatarEvent(this, this.parentProvider, newAvatar);

            for (AvatarListener l : listeners)
                l.avatarChanged(event);
        }
    }
}
