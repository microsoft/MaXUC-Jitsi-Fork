// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.StanzaError;

import net.java.sip.communicator.service.protocol.OperationFailedException;

/**
 * Utility class to rethrow Smack XMPPErrorExceptions as
 * OperationFailedException
 */
public class XMPPErrorConvertor
{
    /**
     * Rethrows an XMPPErrorException as an appropriate OperationFailedException
     *
     * @param errorText the error text
     * @param ex the source exception
     * @throws OperationFailedException
     */
    public static void reThrowAsOperationFailedException(
        String errorText,
        XMPPErrorException ex)
    throws OperationFailedException
    {
        int errorCode = OperationFailedException.INTERNAL_ERROR;
        StanzaError err = ex.getStanzaError();

        if (err != null)
        {
            switch (err.getCondition())
            {
                case forbidden:
                case not_allowed:
                case not_authorized:
                    errorCode = OperationFailedException.FORBIDDEN;
                    break;
                default:
                    errorCode = OperationFailedException.INTERNAL_SERVER_ERROR;
            }

            errorText = err.getConditionText();
        }

        throw new OperationFailedException(errorText, errorCode, ex);
    }

}
