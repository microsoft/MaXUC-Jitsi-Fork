/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.replacement;

import java.util.List;

import net.java.sip.communicator.impl.gui.main.chat.DisplayablePartOfMessage;
import net.java.sip.communicator.impl.gui.main.chat.TextPartOfMessage;

/**
 * A service used to provide substitution for parts of text in chat messages
 * into something non-text like smileys, video and image previews, etc.
 *
 * @author Purvesh Sahoo
 */
public interface ChatPartReplacementService
{
    /**
     * The source name property name.
     */
    String SOURCE_NAME = "SOURCE";

    /**
     * Returns the name of the replacement source.
     *
     * @return the replacement source name
     */
    String getServiceName();

    /**
     * Performs the replacements (e.g. an image for ':-)', or an image for some emoji's
     * unicode value) and returns the corresponding list.
     * @param stringMessagePart, a wrapper for the bit of string in the message on which to perform replacements
     * @return the list corresponding to the message after replacements have been performed
     */
    List<DisplayablePartOfMessage> replaceText(TextPartOfMessage stringMessagePart);
}
