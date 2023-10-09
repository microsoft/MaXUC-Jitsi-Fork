/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model;

/**
 * Exceptions of this class get thrown whenever an error occurs while operating
 * with XCAP server.
 *
 * @author Grigorii Balutsel
 */
public class ParsingException extends Exception
{
    /**
     * Serial versionUID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Creates a new <code>XCapException</code> instance whith human-readable
     * explanation.
     *
     * @param message the detailed message explaining any particular details as
     *                to why is not the specified operation supported or null if
     *                no particular details exist.
     */
    public ParsingException(String message)
    {
        super(message);
    }

    /**
     * Creates a new <code>XCapException</code> instance with the original cause
     * of the problem.
     *
     * @param cause the original cause of the problem.
     */
    public ParsingException(Throwable cause)
    {
        super(cause);
    }
}
