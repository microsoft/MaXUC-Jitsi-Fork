/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.protocol.OperationSetChangePassword;
import net.java.sip.communicator.util.Logger;

/**
 * A jabber implementation of the password change operation set.
 *
 * @author Boris Grozev
 */
public class OperationSetChangePasswordJabberImpl
        implements OperationSetChangePassword
{
    /**
     * The <tt>ProtocolProviderService</tt> whose password we'll change.
     */
    private ProtocolProviderServiceJabberImpl protocolProvider;

    /**
     * The logger used by <tt>OperationSetChangePasswordJabberImpl</tt>.
     */
    private static final Logger logger =
        Logger.getLogger(OperationSetChangePasswordJabberImpl.class);

    /**
     * Sets the object protocolProvider to the one given.
     * @param protocolProvider the protocolProvider to use.
     */
    OperationSetChangePasswordJabberImpl (
                            ProtocolProviderServiceJabberImpl protocolProvider)
    {
        this.protocolProvider = protocolProvider;
    }

}
