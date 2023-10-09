/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.replacement.emoticon;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.sip.communicator.impl.gui.main.chat.DisplayablePartOfMessage;
import net.java.sip.communicator.impl.gui.main.chat.PicturePartOfMessage;
import net.java.sip.communicator.impl.gui.main.chat.TextPartOfMessage;
import net.java.sip.communicator.service.replacement.ChatPartReplacementService;
import net.java.sip.communicator.util.*;

/**
 * Implements the {@link ChatPartReplacementService} to provide emoticon as replacement
 * source.
 *
 * @author Yana Stamcheva
 * @author Purvesh Sahoo
 * @author Adam Netocny
 */
public class EmoticonReplacementService implements ChatPartReplacementService
{
    /**
     * The <tt>Logger</tt> used by the <tt>EmoticonReplacementService</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(EmoticonReplacementService.class);

    /**
     * The <tt>List</tt> of emoticon strings which are matched by
     * {@link #emoticonRegex}.
     */
    private static final List<String> emoticonStrings = new ArrayList<>();

    /**
     * Configuration label shown in the config form.
     */
    public static final String EMOTICON_SERVICE = "EMOTICON_REPLACAMENT";

    /**
     * The regex used to match the emoticons in the message.
     */
    private static String emoticonRegex;

    /**
     * Replaces the emoticon strings with their corresponding emoticon image.
     *
     * @param sourceString the original emoticon string.
     * @return the emoticon image replaced for the emoticon string; the original
     *         emoticon string in case of no match.
     */
    private String getReplacement(final String sourceString)
    {
        try
        {
            Emoticon emoticon = EmoticonResources.getEmoticon(sourceString.trim());

            if (emoticon != null)
                return emoticon.getImageURL();
        }
        catch (Exception e)
        {
            logger.error(
                    "Failed to get emoticon replacement for " + sourceString,
                    e);
        }
        return sourceString;
    }

    /**
     * Gets a regex string which matches the emoticon strings of the specified
     * <tt>Collection</tt> of <tt>Emoticon</tt>s.
     *
     * @param emoticons the <tt>Collection</tt> of <tt>Emoticon</tt>s for which to
     *            get a compiled <tt>Pattern</tt> which matches its emoticon
     *            strings
     * @return a regex string which matches the emoticon strings of the specified
     *         <tt>Collection</tt> of <tt>Emoticon</tt>s
     */
    private static String getPatternForEmoticonReplacement(Collection<Emoticon> emoticons)
    {
        synchronized (emoticonStrings)
        {
            boolean emoticonStringsIsEqual;

            if (emoticonRegex == null)
                emoticonStringsIsEqual = false;
            else
            {
                emoticonStringsIsEqual = true;

                int emoticonStringIndex = 0;
                int emoticonStringCount = emoticonStrings.size();

                emoticonLoop: for (Emoticon emoticon : emoticons)
                    for (String emoticonString : emoticon.getSmileyStrings())
                        if ((emoticonStringIndex < emoticonStringCount)
                            && emoticonString.equals(emoticonStrings
                                .get(emoticonStringIndex)))
                            emoticonStringIndex++;
                        else
                        {
                            emoticonStringsIsEqual = false;
                            break emoticonLoop;
                        }
                if (emoticonStringsIsEqual
                    && (emoticonStringIndex != emoticonStringCount))
                    emoticonStringsIsEqual = false;
            }

            if (!emoticonStringsIsEqual)
            {
                emoticonStrings.clear();

                StringBuffer regex = new StringBuffer();

                // Should not make the replacement unless the emoticon is at the
                // start of the message or after a whitespace character (to
                // prevent interference with HTML markup or unwanted
                // substitution in pasted code, etc.). Note that \u00A0 is the
                // unicode character corresponding to the HTML no-break space
                // &nbsp;.
                regex.append("(?<=(\\u00A0|^|\\s))(");
                for (Emoticon emoticon : emoticons)
                    for (String emoticonString : emoticon.getSmileyStrings())
                    {
                        emoticonStrings.add(emoticonString);

                        GuiUtils.replaceSpecialRegExpChars(emoticonString);
                        regex.append(
                            GuiUtils.replaceSpecialRegExpChars(emoticonString))
                            .append("|");
                    }
                regex = regex.deleteCharAt(regex.length() - 1);
                regex.append(')');

                emoticonRegex = regex.toString();
            }
            return emoticonRegex;
        }
    }

    /**
     * Returns the source name
     *
     * @return the source name
     */
    public String getServiceName()
    {
        return EMOTICON_SERVICE;
    }

    /**
     * Returns an array corresponding to the message with emoticon strings
     * (e.g. :\ ) replaced by the corresponding PictureMessageParts
     *
     * @return array of replaced message (e.g. pictures instead of emoticons strings)
     * @param stringMessagePart corresponding to string on which to perform replacements
     */
    public ArrayList<DisplayablePartOfMessage> replaceText(TextPartOfMessage stringMessagePart)
    {
        String messageString = stringMessagePart.getText();

        // Creating the matcher to search for emoticon strings [e.g. :-) ]
        Collection<Emoticon> emoticons = EmoticonResources.getDefaultEmoticonPack();
        String stringForPattern = getPatternForEmoticonReplacement(emoticons);
        // getDefaultEmoticonPack returns all variants of case-insensitive
        // emoticons, so matching should be case-sensitive
        Pattern pattern = Pattern.compile(stringForPattern, Pattern.DOTALL);
        Matcher m = pattern.matcher(messageString);

        ArrayList<DisplayablePartOfMessage> replacedMessageArray = new ArrayList<>(
                0);
        int startPos = 0;

        while (m.find())
        {
            // Adds the segment of the string before the current emoji to the
            // message array
            String stringSegment = messageString.substring(startPos, m.start());
            startPos = m.end();
            TextPartOfMessage stringSegmentStringMessageObject = new TextPartOfMessage(stringSegment);
            replacedMessageArray.add(stringSegmentStringMessageObject);

            // Adds the picture message part corresponding to the current emoji
            // to the message array
            String emoticonString = m.group();

            String imageFilePath = getReplacement(emoticonString);
            PicturePartOfMessage pictureMessagePart = new PicturePartOfMessage(imageFilePath, emoticonString);
            replacedMessageArray.add(pictureMessagePart);

            logger.debug("Replaced the string " + emoticonString + " with its image");
        }

        // Adds to the replaced array the section of the string after the last emoji
        TextPartOfMessage stringRemnantPart = new TextPartOfMessage(messageString.substring(startPos));
        replacedMessageArray.add(stringRemnantPart);

        return replacedMessageArray;
    }
}
