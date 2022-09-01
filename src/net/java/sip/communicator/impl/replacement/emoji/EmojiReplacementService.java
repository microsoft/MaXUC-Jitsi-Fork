// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.replacement.emoji;

import java.util.*;

import org.jitsi.service.resources.ResourceManagementService;

import net.java.sip.communicator.impl.gui.main.chat.DisplayablePartOfMessage;
import net.java.sip.communicator.impl.gui.main.chat.PicturePartOfMessage;
import net.java.sip.communicator.impl.gui.main.chat.TextPartOfMessage;
import net.java.sip.communicator.service.replacement.ChatPartReplacementService;
import net.java.sip.communicator.util.*;

public class EmojiReplacementService
    implements ChatPartReplacementService
{
    /**
     * The <tt>Logger</tt> used by the <tt>EmoticonReplacementService</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(EmojiReplacementService.class);

    /**
     * Configuration label shown in the config form.
     */
    public static final String EMOJI_REPLACEMENT_SERVICE = "EMOJI_REPLACEMENT";

    private static final Set<Integer> setOfIgnorableCodepoints = new HashSet<>();

    static
    {
        // Usually sent along with 4 character long codepoints
        setOfIgnorableCodepoints.add(new Integer(65039));
    }

  /**
    * Returns the source name
    */
   public String getServiceName()
   {
       return EMOJI_REPLACEMENT_SERVICE;
   }

   /**
    * Checks if a particular codepoint corresponds to an ASCII character
    */
   public boolean isASCII(int codepoint)
   {
       return (codepoint < 256);
   }

   /**
    * Checks if a particular codepoint corresponds to an emoji for which we
    * have an image and is not the first codepoint in a sequence.
    *
    * @param codepoint The codepoint to check as decimal integer
    */
    public boolean isSupportedSingleCodepointEmojiNotStartingSequence(int codepoint)
    {
        boolean isSupportedSingleCodepointNotStartingSequence = false;
        // So we don't bother checking any of the 256 ASCII characters
        if (!isASCII(codepoint))
        {
            Set<Integer> setOfEmojis = EmojiResources.getSetOfEmojiCodepointsNotStartingSequence();
            if (setOfEmojis.contains(codepoint))
            {
                isSupportedSingleCodepointNotStartingSequence = true;
            }
        }
        return isSupportedSingleCodepointNotStartingSequence;
    }

    /**
     * Some emoji are automatically sent with a second unicode value which are
     * meant to act as instructions for how to display them. For example, the
     * emoji with 4-character Unicode (e.g. football, +26bd) are usually sent
     * with the codepoint +fe0f following it. However, these additional codepoints
     * serve no purpose in the current emoji display implementation, and cause
     * the chat panel to display a blank box just next to the sent emoji.
     * isIgnorableUnicode checks if a given codepoint is one of these ignorable codepoints.
     */
    private boolean isIgnorableCodepoint(int codepoint)
    {
        return setOfIgnorableCodepoints.contains(codepoint);
    }

    /**
     * Create a PicturePartOfMessage for an emoji that matches the greatest
     * sequence of codepoints at the beginning of the supplied list.
     * Remove the codepoints from the list if we find a match.
     *
     * @param listOfCodepoints
     * @return
     */
    private PicturePartOfMessage createLongestMatchEmoji(List<Integer> listOfCodepoints)
    {
        // Create a emoji out of the longest matching sequence
        PicturePartOfMessage emoji = null;

        Set<List<Integer>> sequences = EmojiResources.getCodepointSequences();

        List<Integer> bestMatch = new ArrayList<>();

        for (int i = 1; i <= listOfCodepoints.size(); i++)
        {
          // We don't want to create an emoji from (single-codepoint) ASCII
          // but do want to continue looping to find any emojis that start
          // with an ASCII codepoint
          if ((i>1) || !(isASCII(listOfCodepoints.get(0))))
          {
              if (sequences.contains(listOfCodepoints.subList(0, i)))
              {
                  // If sequence is a match, append the remaining portion to bestMatch
                  bestMatch.addAll(listOfCodepoints.subList(bestMatch.size(), i));
              }
          }
        }

        if (!bestMatch.isEmpty())
        {
            // Remove what we've matched
            StringBuilder alt = new StringBuilder();

            for (Integer r : bestMatch)
            {
                listOfCodepoints.remove(0);
                alt.append(Character.toChars(r));
            }

            ResourceManagementService res = EmojiActivator.getResources();
            String path = EmojiResources.getFilepathFromCodepoints(bestMatch);
            String url = res.getImageURLForPath(path).toString();

            emoji = new PicturePartOfMessage(url,
                                             alt.toString(),
                                             EmojiResources.getFilepathFromCodepoints(bestMatch));
        }

        return emoji;
    }

    /**
     * We're supplied with a set of integer lists in possibleEmojiCodepoints.  Remove any of them
     * that don't have the supplied codepoint at the supplied position.
     * @param possibleEmojiCodepoints
     * @param codepoint
     * @param position
     */
    private void filterPrefixes(Set<List<Integer>> possibleEmojiCodepoints, int codepoint, int position)
    {
        for (Iterator<List<Integer>> iterator = possibleEmojiCodepoints.iterator(); iterator.hasNext();)
        {
            List<Integer> codepointSequence = iterator.next();

            if ((codepointSequence.size() <= position) ||
                (codepointSequence.get(position) != codepoint))
            {
                iterator.remove();
            }
        }
    }

    /*
     * Get a set of emojis starting with the supplied codepoint (note the emoji could be
     * _just_ the supplied codepoint, i.e. sequence of length 1)
     */
    private Set<List<Integer>> getEmojiMatches(int codepoint, Set<List<Integer>> possibleEmojiCodepoints)
    {
        // Get a list of possible sequences of emoji characters beginning with this one.
        Set<List<Integer>> result = new HashSet<>();
        for (List<Integer> codepointSequence : EmojiResources.getCodepointSequences())
        {
            if (codepointSequence.get(0) == codepoint)
            {
                if (isASCII(codepointSequence.get(0)) && codepointSequence.size() == 0)
                {
                    //We don't want to consider single codepoint ASCII
                    continue;
                }
                result.add(codepointSequence);
            }
        }
        return result;
    }

    /**
     * Returns a list corresponding to the message with unicode characters
     * representing emojis replaced by the corresponding PictureMessageParts
     * @param textPartOfMessage
     * @return list of replaced message (e.g. pictures where emoji unicode was sent)
     */
    @Override
    public List<DisplayablePartOfMessage> replaceText(TextPartOfMessage textPartOfMessage)
    {
        String messageText = textPartOfMessage.getText();

        List<DisplayablePartOfMessage> replacedMessageList = new ArrayList<>();
        StringBuilder collectedNonEmojis = new StringBuilder();

        List<Integer> collectedPossibleEmojiCharacters = new ArrayList<>();
        Set<List<Integer>> possibleEmojiCodepointSequences = new HashSet<>();

        int[] codePoints = messageText.codePoints().toArray();

        int currentCodepointIndex = 0;

        // Loop over the codepoints in this message. If we match an emoji,
        // continue processing from the codepoint after the emoji.
        while (currentCodepointIndex < codePoints.length)
        {
            int codepoint = codePoints[currentCodepointIndex];

            if (isIgnorableCodepoint(codepoint))
            {
                currentCodepointIndex++;
                continue;
            }

            if (collectedPossibleEmojiCharacters.size() > 0)
            {
                // Already collected some characters that might make up an emoji
                // Narrow down possibilities using current character
                filterPrefixes(possibleEmojiCodepointSequences, codepoint, collectedPossibleEmojiCharacters.size());
                if (possibleEmojiCodepointSequences.size() > 0)
                {
                    // There are supported sequences that begin with the
                    // codepoints in collectedPossibleEmojiCharacters
                    // We need to continue collecting characters
                    collectedPossibleEmojiCharacters.add(codepoint);
                    currentCodepointIndex++;
                }
                else
                {
                    // No more possibilities - create the longest match with what we have
                    PicturePartOfMessage emoji = createLongestMatchEmoji(collectedPossibleEmojiCharacters);
                    //This call also removes the codepoints of emoji from collectedPossibleEmojiCharacters

                    if (emoji == null)
                    {
                        // The first character of
                        // collectedPossibleEmojiCharacters is clearly text
                        collectedNonEmojis.append(Character.toChars(collectedPossibleEmojiCharacters.get(0)));
                        //Continue processing from the next character
                        currentCodepointIndex -= (collectedPossibleEmojiCharacters.size() - 1);
                        collectedPossibleEmojiCharacters.clear();
                    }
                    else
                    {
                        if (collectedNonEmojis.length() > 0)
                        {
                            // Must add any collected text to the replacedMessageList to maintain ordering
                            addTextToReplacedMessageList(replacedMessageList, collectedNonEmojis.toString());
                            collectedNonEmojis.delete(0, collectedNonEmojis.length()); // This is the fastest way to clear a stringBuilder
                        }

                        replacedMessageList.add(emoji);

                        // Continue processing codePoints where the leftovers begin
                        currentCodepointIndex -= collectedPossibleEmojiCharacters.size();
                        collectedPossibleEmojiCharacters.clear();
                    }
                }
            }
            else
            {
                //Not currently collecting emoji characters
                possibleEmojiCodepointSequences = getEmojiMatches(codepoint, possibleEmojiCodepointSequences);
                if (possibleEmojiCodepointSequences.size() > 0)
                {
                    //Could be part of an emoji
                    //
                    if (collectedNonEmojis.length() > 0)
                    {
                        // Must add any collected text to the replacedMessageList to maintain ordering
                        String stringBeforeCurrentEmoji = collectedNonEmojis.toString();
                        addTextToReplacedMessageList(replacedMessageList, stringBeforeCurrentEmoji);
                        collectedNonEmojis.delete(0, collectedNonEmojis.length()); // This is the fastest way to clear a stringBuilder
                    }

                    if (isSupportedSingleCodepointEmojiNotStartingSequence(codepoint))
                    {
                        // Single codepoint emoji only, don't bother collecting - just add it
                        replacedMessageList.add(new PicturePartOfMessage
                                                             (EmojiResources.getURLFromCodepoint(codepoint),
                                                             String.valueOf(Character.toChars(codepoint)),
                                                             EmojiResources.getFilepathFromCodepoint(codepoint)));
                    }
                    else
                    {
                        collectedPossibleEmojiCharacters.add(codepoint);
                    }
                }
                else
                {
                    //This codepoint is definitely text
                    collectedNonEmojis.append(Character.toChars(codepoint));
                }
                currentCodepointIndex++;
            }
        }

        // Now we've finished inspecting the stream of characters, do we have
        // anything 'partially collected' to add?
        processLeftovers(collectedPossibleEmojiCharacters, collectedNonEmojis, replacedMessageList);

        return replacedMessageList;
    }

    /**
     * Finds (longest) emojis in the codepoint sequence
     * collectedEmojiCharacters (recursively) and
     * adds them to the replacedMessageList.
     * Adds any text characters to the replacedMessageList to maintain ordering.
     *
     * When this method returns, collectedPossibleEmojiCharacters and collectedNonEmojis
     * will be empty
     */
    private void processLeftovers(List<Integer> collectedPossibleEmojiCharacters,
                                  StringBuilder collectedNonEmojis,
                                  List<DisplayablePartOfMessage> replacedMessageList)
    {
        while (collectedPossibleEmojiCharacters.size() > 0)
        {
            PicturePartOfMessage emoji = createLongestMatchEmoji(collectedPossibleEmojiCharacters);
            //This call also removes the characters in emoji from collectedEmojiCharacters

            if (emoji != null)
            {
                // Emoji matched
                if (collectedNonEmojis.length() > 0)
                {
                    // Must add any collected text to the replacedMessageList to maintain ordering
                    addTextToReplacedMessageList(replacedMessageList, collectedNonEmojis.toString());
                    collectedNonEmojis.delete(0, collectedNonEmojis.length()); // This is the fastest way to clear a stringBuilder
                }
                replacedMessageList.add(emoji);
            }
            else
            {
                // This codepoint sequence does not match any emojis, however
                // its tail might, so we pop the first codepoint, add it to
                // nonEmojiCodepoints (since it must be text), and try
                // to match on the remainder of the sequence
                collectedNonEmojis.append(Character.toChars(collectedPossibleEmojiCharacters.get(0)));
                collectedPossibleEmojiCharacters = collectedPossibleEmojiCharacters.subList(1,
                                                                                            collectedPossibleEmojiCharacters.size());
            }
        }
        //Save remaining text characters, if any
        if (collectedNonEmojis.length() > 0)
        {
            addTextToReplacedMessageList(replacedMessageList, collectedNonEmojis.toString());
        }
    }

    /*
     * Necessary because if we see a character that could be the start of an
     * emoji (e.g. #), but turns out not be, we don't want to separate it
     * from the previous text characters with plaintext tags and split up the
     * HTML character references for / and '.
     */
    private void addTextToReplacedMessageList(List<DisplayablePartOfMessage> replacedMessageList, String newText)
    {
        if (replacedMessageList.size() > 0){
            DisplayablePartOfMessage lastPart = replacedMessageList.get(replacedMessageList.size()-1);
            if (lastPart instanceof TextPartOfMessage)
            {
                //Should continue on from previous TextPartOfMessage
                TextPartOfMessage lastTextPart = (TextPartOfMessage) lastPart;
                lastTextPart.setText(lastTextPart.getText() + newText);
            }
            else {
                //Previous element was a picture - safe to begin a new TextPartOfMessage
                replacedMessageList.add(new TextPartOfMessage(newText));
            }
        }
        else
        {
            //This will be the first element of replacedMessageList
            replacedMessageList.add(new TextPartOfMessage(newText));
        }
    }
}

