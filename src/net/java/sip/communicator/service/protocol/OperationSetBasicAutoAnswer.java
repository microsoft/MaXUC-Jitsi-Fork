/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * An Operation Set defining option
 * to unconditional auto answer incoming calls.
 *
 * @author Damian Minkov
 */
public interface OperationSetBasicAutoAnswer
    extends OperationSet
{
    /**
     * Auto answer unconditional account property.
     */
    String AUTO_ANSWER_UNCOND_PROP =
        "AUTO_ANSWER_UNCONDITIONAL";

    /**
     * Auto answer video calls with video account property.
     */
    String AUTO_ANSWER_WITH_VIDEO_PROP =
        "AUTO_ANSWER_WITH_VIDEO";

    /**
     * Clear any previous settings.
     */
    void clear();

}
