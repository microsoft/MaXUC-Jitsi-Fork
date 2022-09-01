/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

import org.jitsi.service.resources.*;

/**
 * The <tt>UIContactDetail</tt> implementation
 *
 * @author Yana Stamcheva
 */
public class UIContactDetailImpl
    extends UIContactDetail
{
    /**
     * The status icon of this contact detail.
     */
    private ImageIconFuture mStatusIcon;

    /**
     * Creates a <tt>UIContactDetailImpl</tt> by specifying the contact
     * <tt>address</tt>, the <tt>displayName</tt> and <tt>preferredProvider</tt>.
     *
     * @param address the contact address
     * @param displayName the contact display name
     * @param statusIcon the status icon of this contact detail
     * @param descriptor the underlying object that this class is wrapping
     */
    public UIContactDetailImpl(
        String address,
        String displayName,
        ImageIconFuture statusIcon,
        Object descriptor)
    {
        super(address,
              displayName,
              null,
              null,
              null,
              null,
              descriptor);

        setStatusIcon(statusIcon);
    }

    /**
     * Creates a <tt>UIContactDetailImpl</tt> by specifying the contact
     * <tt>address</tt>, the <tt>displayName</tt> and <tt>preferredProvider</tt>.
     * @param address the contact address
     * @param displayName the contact display name
     * @param category the category of the underlying contact detail
     * @param labels the collection of labels associated with this detail
     * @param statusIcon the status icon of this contact detail
     * @param preferredProviders the preferred protocol providers
     * @param preferredProtocols the preferred protocols if no protocol provider
     * is set
     * @param descriptor the underlying object that this class is wrapping
     */
    public UIContactDetailImpl(
        String address,
        String displayName,
        String category,
        Collection<String> labels,
        ImageIconFuture statusIcon,
        Map<Class<? extends OperationSet>, ProtocolProviderService>
                                                        preferredProviders,
        Map<Class<? extends OperationSet>, String> preferredProtocols,
        Object descriptor)
    {
        super(address, displayName, category, labels, preferredProviders,
            preferredProtocols, descriptor);

        setStatusIcon(statusIcon);
    }

    /**
     * Sets the given status icon.
     *
     * @param statusIcon the status icon to set
     */
    public void setStatusIcon(ImageIconFuture statusIcon)
    {
        mStatusIcon = statusIcon;
    }

    /**
     * Returns the status icon of this contact detail.
     *
     * @return the status icon of this contact detail
     */
    public ImageIconFuture getStatusIcon()
    {
        return mStatusIcon;
    }

    @Override
    public PresenceStatus getPresenceStatus()
    {
        return null;
    }
}
