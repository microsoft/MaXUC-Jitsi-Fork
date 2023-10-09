/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import net.java.sip.communicator.service.protocol.*;

/**
 * An <tt>OperationSet</tt> that allows us to receive DMF tones through
 * this protocol provider.
 *
 * @author Damian Minkov
 */
public class OperationSetIncomingDTMFSipImpl
    implements OperationSetIncomingDTMF
{
    /**
     * The send DTMF operation set holding dtmf implementations.
     */
    private OperationSetDTMFSipImpl opsetDTMFSip;

    /**
     * Creates operation set.
     * @param provider the parent provider
     * @param opsetDTMFSip the dtmf implementation.
     */
    OperationSetIncomingDTMFSipImpl(ProtocolProviderServiceSipImpl provider,
                                    OperationSetDTMFSipImpl opsetDTMFSip)
    {
        this.opsetDTMFSip = opsetDTMFSip;
    }

}
