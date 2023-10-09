/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.xcaperror;

/**
 * The XCAP error interface.
 *
 * @author Grigorii Balutsel
 */
public interface XCapError
{
    /**
     * Gets the phrase attribute.
     *
     * @return User readable error description.
     */
    String getPhrase();
}
