/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat;

import java.text.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.html.HTML.*;

import org.jitsi.service.configuration.ConfigurationService;

import com.google.common.annotations.VisibleForTesting;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.replacement.ChatPartReplacementService;
import net.java.sip.communicator.service.replacement.ReplacementProperty;
import net.java.sip.communicator.util.*;

/**
 *
 * @author Yana Stamcheva
 */
public class ChatHtmlUtils
{
    /**
     * The name attribute.
     */
    public static final String NAME_ATTRIBUTE = "name";

    /**
     * The date attribute.
     */
    public static final String DATE_ATTRIBUTE = "date";

    /**
     * The name of the attribute containing the original chat message before
     * processing replacements.
     */
    public static final String ORIGINAL_MESSAGE_ATTRIBUTE = "original_message";

    /**
     * The message header identifier attribute.
     */
    public static final String MESSAGE_HEADER_ID = "messageHeader";

    /**
     * The message identifier attribute.
     */
    public static final String MESSAGE_TEXT_ID = "message";

    /**
     * The error message identifier attribute.
     */
    public static final String ERROR_MESSAGE_ID = "errorMessage";

    /**
     * The closing tag of the <code>PLAINTEXT</code> HTML element.
     */
    static final String END_PLAINTEXT_TAG = "</PLAINTEXT>";

    /**
     * The opening tag of the <code>PLAINTEXT</code> HTML element.
     */
    static final String START_PLAINTEXT_TAG = "<PLAINTEXT>";

    /**
     * The html text content type.
     */
    public static final String HTML_CONTENT_TYPE = "text/html";

    /**
     * The plain text content type.
     */
    public static final String TEXT_CONTENT_TYPE = "text/plain";

    /**
     * Date format used to display message dates in the UI.
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    /**
     * A regular expression that matches a <div> tag and its contents.
     * The opening tag is group 1, and the tag contents is group 2 when
     * a match is found.
     */
    static final Pattern DIV_PATTERN =
            Pattern.compile("(<div[^>]*>)(.*)(</div>)", Pattern.DOTALL);

    /**
     * The <tt>Logger</tt> used by the <tt>ChatHtmlUtils</tt> class for
     * logging output.
     */
    private static final Logger sLog
        = Logger.getLogger(ChatHtmlUtils.class);

    /**
     * The regular expression which matches URLs for the purpose of turning them
     * into links.
     *
     * First half obtained from:
     * http://daringfireball.net/2010/07/improved_regex_for_matching_urls
     *
     * Second half obtained from:
     * http://blog.codinghorror.com/the-problem-with-urls/
     *
     * This regex should match all valid URLs (excluding unicode chars),
     * but doesn't attempt to exclude all invalid URLs - as that test is O(2^n)
     * in the worst case: see
     * https://stackoverflow.com/questions/5011672/java-regular-expression-running-very-slow
     * (the accepted answer causes the regex to fail on some inputs, so isn't a
     * solution to the problem).
     *
     * We try to make sure that the matcher doesn't grab surrounding characters
     * which shouldn't be in the URL, however we do always grab terminal ')'
     * characters. This is usually the wrong behaviour - e.g.
     * "(here's a link: www.foo.com)" so we fix this by post-processing in Java.
     *
     */
    static final Pattern URL_PATTERN
        = Pattern.compile(
            "\\b" +
            "(" +                                            // Capture 1: entire matched URL
                "(?:" +
                    "[a-z][\\w-]+:" +                        // URL protocol and colon
                    "(?:" +
                        "/{1,3}" +                           // 1-3 slashes
                    "|" +                                    // or
                        "[a-z0-9%]" +                        // Single letter or digit or '%'
                                                             // (Trying not to match e.g. "URI::Escape")
                    ")" +
                "|" +                                        // or
                    "www\\d{0,3}[.]" +                       // "www.", "www1.", "www2." ... "www999."
                "|" +                                        // or
                    "[a-z0-9.\\-]+[.][a-z]{2,4}/" +          // looks like domain name followed by a slash
                ")" +                                        // -- END of daringfireball section --
             "[-A-Za-z0-9+&@#/%?=~_()\\[\\]|!:,.;\\{\\}]*" + // Some number of valid characters
             "[-A-Za-z0-9+&@#/%=~_()\\[\\]|\\{\\}]" +        // A final character (not all valid chars are valid at the end of the url)
            ")",
           Pattern.CASE_INSENSITIVE);

    /**
     * Creates an incoming message tag.
     *
     * @param messageID the identifier
     * @param contactName the name of the contact sending the message
     * @param contactDisplayName the display name of the contact sending the
     * message
     * @param avatarPath the path to the avatar file
     * @param date the date, when the message was sent
     * @param message the message content
     * @param contentType the content type HTML or PLAIN_TEXT
     * @param messageType The type of message (IM or SMS)
     * @param isGroupChat indicates if this is a message coming from a group chat
     * @return the created incoming message tag
     */
    public static String createIncomingMessageTag(
        String messageID,
        String contactName,
        String contactDisplayName,
        String avatarPath,
        Date date,
        String message,
        String contentType,
        String messageType,
        boolean isGroupChat)
    {
        StringBuilder messageBuilder = new StringBuilder();

        messageBuilder.append("<table width=\"100%\" ");
        messageBuilder.append(NAME_ATTRIBUTE + "=\""
            +  Tag.TABLE.toString() + "\">");

        messageBuilder.append("<tr><td valign=\"top\">");
        messageBuilder.append(
            "<table " + IncomingMessageStyle.createTableBubbleStyle()
            + " cellspacing=\"0\" cellpadding=\"0\" style=\"width:80%;margin-right:20%;\">");

        // Add the peer's name in the third cell of a new table row
        if (isGroupChat)
        {
            messageBuilder.append("<tr><td></td><td></td><td><p style=\"color:#7c8082;font-size:");
            messageBuilder.append(ScaleUtils.scaleInt(8));
            messageBuilder.append("px;font-weight:bold;margin-left:");
            messageBuilder.append(ScaleUtils.scaleInt(6));
            messageBuilder.append("px;margin-bottom:");
            messageBuilder.append(ScaleUtils.scaleInt(3));
            messageBuilder.append("px;\">");
            messageBuilder.append(contactDisplayName);
            messageBuilder.append("</p></td></tr>");
        }

        messageBuilder.append("<tr><td valign=\"top\" style=\"width:");
        messageBuilder.append(ScaleUtils.scaleInt(26));
        messageBuilder.append("px;\"><img src=\"");
        messageBuilder.append(avatarPath);
        messageBuilder.append("\" width=\"");
        messageBuilder.append(ScaleUtils.scaleInt(26));
        messageBuilder.append("px\" height=\"");
        messageBuilder.append(ScaleUtils.scaleInt(26));
        messageBuilder.append("px\"/></td>");
        messageBuilder.append("<td "
            + IncomingMessageStyle.createIndicatorStyle(messageType) +"></td>");
        messageBuilder.append("<td "
            + IncomingMessageStyle.createTableBubbleMessageStyle(messageType) + ">");
        messageBuilder.append("<div>");
        messageBuilder.append(
            createMessageTag(messageID,
                             contactName,
                             message,
                             contentType,
                             date,
                             false));
        messageBuilder.append("</div>");
        messageBuilder.append(createMessageHeaderTag(messageID, date, null));
        messageBuilder.append("</td></tr></table></td></tr></table>");

        return messageBuilder.toString();
    }

    /**
     * Create an outgoing message tag.
     *
     * @param messageID the identifier of the message
     * @param contactName the name of the account sending the message
     * @param date the date, when the message was sent
     * @param message the content of the message
     * @param contentType the content type HTML or PLAIN_TEXT
     * @param messageType The type of message (IM or SMS)
     * @param errorMessage the error message to display (will be null if the
     *                     message hasn't failed)
     * @return the created outgoing message tag
     */
    public static String createOutgoingMessageTag(String messageID,
                                                  String contactName,
                                                  Date date,
                                                  String message,
                                                  String contentType,
                                                  String messageType,
                                                  String errorMessage)
    {
        StringBuilder messageBuilder = new StringBuilder();

        messageBuilder.append("<table width=\"100%\" ");
        messageBuilder.append(NAME_ATTRIBUTE + "=\""
            +  Tag.TABLE.toString() + "\">");
        messageBuilder.append("<tr><td valign=\"top\">");
        messageBuilder.append(
            "<table " + OutgoingMessageStyle.createTableBubbleStyle()
            + " cellspacing=\"0\" cellpadding=\"0\" style=\"width:100%;\">");
        messageBuilder.append("<tr><td style=\"width:20%;\"></td>");
        messageBuilder.append("<td "
            + OutgoingMessageStyle.createTableBubbleMessageStyle(messageType) + ">");
        messageBuilder.append("<div>");
        messageBuilder.append(
            createMessageTag(messageID,
                             contactName,
                             message,
                             contentType,
                             date,
                             false));
        messageBuilder.append("</div>");
        messageBuilder.append(
            createMessageHeaderTag(messageID, date, errorMessage));
        messageBuilder.append("</td>");
        messageBuilder.append("<td "
            + OutgoingMessageStyle.createIndicatorStyle(messageType) +"></td>");
        messageBuilder.append("</tr></table></td></tr></table>");

        return messageBuilder.toString();
    }

    /**
     * Creates a message table tag, representing the message header.
     * @param messageID the identifier of the message
     * @param date the date, when the message was sent or received
     * @param errorMessage the error message to display (will be null if the
     *                     message hasn't failed)
     * @return the message header tag
     */
    private static String createMessageHeaderTag(String messageID,
                                                 Date date,
                                                 String errorMessage)
    {
        StringBuilder messageHeader = new StringBuilder();

        messageHeader.append("<div "
                + OutgoingMessageStyle.createDateStyle() + ">");
        messageHeader.append(createMessageErrorTag(messageID, errorMessage));
        messageHeader.append(getDateAndTimeString(date));
        messageHeader.append("</div>");

        return messageHeader.toString();
    }

    public static String getDateAndTimeString(Date date)
    {
        // This is hopelessly not localized, but that applies to most if not all
        // of the date handling in the client
        return getDateString(date) + GuiUtils.formatTime(date);
    }

    /**
     * Creates the start tag, which indicates that the next text would be plain
     * text.
     *
     * @param contentType the current content type
     * @return the start plaintext tag
     */
    public static String createStartPlainTextTag(String contentType)
    {
        if (HTML_CONTENT_TYPE.equals(contentType))
        {
            return "";
        }
        else
        {
            return START_PLAINTEXT_TAG;
        }
    }

    /**
     * Creates the end tag, which indicates that the next text would be plain
     * text.
     *
     * @param contentType the current content type
     * @return the end plaintext tag
     */
    public static String createEndPlainTextTag(String contentType)
    {
        if (HTML_CONTENT_TYPE.equals(contentType))
        {
            return "";
        }
        else
        {
            return END_PLAINTEXT_TAG;
        }
    }

    /**
     * Creates the message tag.
     *
     * @param messageID the identifier of the message
     * @param contactName the name of the sender
     * @param message the message content
     * @param contentType the content type (html or plain text)
     * @param date the date on which the message was sent
     * @param isEdited indicates if the given message has been edited
     * @return the newly constructed message tag
     */
    public static String createMessageTag(String messageID,
                                          String contactName,
                                          String message,
                                          String contentType,
                                          Date date,
                                          boolean isEdited)
    {
        StringBuilder messageTag = new StringBuilder();

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        messageTag.append(String.format("<div id='%s' %s='%s' ",
                MESSAGE_TEXT_ID + messageID, NAME_ATTRIBUTE,
                contactName));
        messageTag.append(DATE_ATTRIBUTE + "=\"" + sdf.format(date) + "\" ");
        messageTag.append(String.format("%s='%s' ",
                ORIGINAL_MESSAGE_ATTRIBUTE, GuiUtils.escapeHTMLChars(message)));
        messageTag.append(IncomingMessageStyle
            .createSingleMessageStyle(isEdited));
        messageTag.append(">");
        messageTag.append(createStartPlainTextTag(contentType));
        messageTag.append(message);
        if (isEdited)
            messageTag.append("    ");
        messageTag.append(createEndPlainTextTag(contentType));
        if (isEdited)
            messageTag.append(createEditedAt(date));
        messageTag.append("</div>");

        return messageTag.toString();
    }

    /**
     * Creates an error message tag for the message with the given ID containing
     * the given error message.
     *
     * @param messageID the ID of the message to create an error message tag for
     * @param errorMessage the error message to display (will be null if the
     *                     message hasn't failed)
     *
     * @return The error tag to display
     */
    public static String createMessageErrorTag(String messageID, String errorMessage)
    {
        StringBuilder errorMessageTag = new StringBuilder();
        errorMessageTag.append("<b id=\"" + ERROR_MESSAGE_ID + messageID);
        // If the error message is null, set it to a whitespace character
        // rather than an empty string so that the error tag won't be stripped
        // out in case this message needs to have an error message added later.
        if (errorMessage == null)
        {
            errorMessage = "&nbsp;";
        }
        else
        {
            errorMessageTag.append("\" style=\"color:red;");
            errorMessage = errorMessage + " - ";
        }

        errorMessageTag.append("\">" + errorMessage + "</b>");
        return errorMessageTag.toString();
    }

    /**
     * Returns the date string to show for the given date.
     *
     * @param date the date to format
     * @return the date string to show for the given date
     */
    public static String getDateString(Date date)
    {
        if (GuiUtils.compareDatesOnly(date, new Date()) <= 0)
        {
            StringBuilder dateStrBuilder = new StringBuilder();

            GuiUtils.formatDate(date, dateStrBuilder);
            return dateStrBuilder.append(" ").toString();
        }

        return "";
    }

    /**
     * Creates the edited at string.
     *
     * @param date the date of the re-edition
     * @return the newly constructed string
     */
    private static String createEditedAt(Date date)
    {
        return "<font color=\"#b7b7b7\">" + GuiActivator.getResources()
                    .getI18NString("service.gui.EDITED_AT",
                                   new String[]{GuiUtils.formatTime(date)})
                + "</font>";
    }

    /**
     * Formats the given message. Processes all smiley chars, new lines and
     * links.
     *
     * @param message the message to be formatted
     * @param contentType the content type of the message to be formatted
     * @param keyword the word to be highlighted
     * @return the formatted message
     */
    public static String formatMessage(String message,
                                 String contentType,
                                 String keyword)
    {
        // If the message content type is HTML we won't process links and
        // new lines, but only the smileys.
        if (!ChatHtmlUtils.HTML_CONTENT_TYPE.equals(contentType))
        {
            /*
             * We disallow HTML in plain-text messages. But processKeyword
             * introduces HTML. So we'll allow HTML if processKeyword has
             * introduced it in order to not break highlighting.
             */
            boolean processHTMLChars;

            if ((keyword != null) && (keyword.length() != 0))
            {
                String messageWithProcessedKeyword
                    = processKeyword(message, contentType, keyword);

                /*
                 * The same String instance will be returned if there was no
                 * keyword match. Calling #equals() is expensive so == is
                 * intentional.
                 */
                processHTMLChars = (messageWithProcessedKeyword == message);
                message = messageWithProcessedKeyword;
            }
            else
                processHTMLChars = true;

            message = processNewLines(processLinksAndHTMLChars(
                    message, processHTMLChars, contentType), contentType);
        }
        // If the message content is HTML, we process br and img tags.
        else
        {
            if ((keyword != null) && (keyword.length() != 0))
                message = processKeyword(message, contentType, keyword);
            message = processImgTags(processBrTags(message));
        }

        return message;
    }

    /**
     * Highlights keywords searched in the history.
     *
     * @param message the source message
     * @param contentType the content type
     * @param keyword the searched keyword
     * @return the formatted message
     */
    private static String processKeyword(String message,
                                  String contentType,
                                  String keyword)
    {
        // We want search terms to match entire words, so add (\S)* to the start
        // and end of the keyword to include all non-whitespace characters.
        Matcher m = Pattern.compile("(\\S)*" + Pattern.quote(keyword) + "(\\S)*",
                                    Pattern.CASE_INSENSITIVE).matcher(message);
        StringBuilder msgBuffer = new StringBuilder();
        int prevEnd = 0;

        while (m.find())
        {
            msgBuffer.append(message.substring(prevEnd, m.start()));
            prevEnd = m.end();

            String keywordMatch = m.group().trim();

            msgBuffer.append(ChatHtmlUtils.createEndPlainTextTag(contentType));
            msgBuffer.append("<b>");
            msgBuffer.append(keywordMatch);
            msgBuffer.append("</b>");
            msgBuffer.append(ChatHtmlUtils.createStartPlainTextTag(contentType));
        }

        /*
         * If the keyword didn't match, let the outside world be able to
         * discover it.
         */
        if (prevEnd == 0)
            return message;

        msgBuffer.append(message.substring(prevEnd));
        return msgBuffer.toString();
    }

    /**
     * Formats all links in a given message and optionally escapes special HTML
     * characters such as &lt;, &gt;, &amp; and &quot; in order to prevent HTML
     * injection in plain-text messages such as writing
     * <code>&lt;/PLAINTEXT&gt;</code>, HTML which is going to be rendered as
     * such and <code>&lt;PLAINTEXT&gt;</code>. The two procedures are carried
     * out in one call in order to not break URLs which contain special HTML
     * characters such as &amp;.
     *
     * @param message The source message string.
     * @param processHTMLChars  <tt>true</tt> to escape the special HTML chars;
     * otherwise, <tt>false</tt>
     * @param contentType the message content type (html or plain text)
     * @return The message string with properly formatted links.
     */
    public static String processLinksAndHTMLChars(String message,
                                                  boolean processHTMLChars,
                                                  String contentType)
    {
        StringBuilder msgBuffer = new StringBuilder();

        if (message != null)
        {
            Matcher m = URL_PATTERN.matcher(message);

            int prevEnd = 0;

            while (m.find())
            {
                String fromPrevEndToStart = message.substring(prevEnd, m.start());

                if (processHTMLChars)
                {
                    fromPrevEndToStart =
                        GuiUtils.escapeHTMLChars(fromPrevEndToStart);
                }

                // This is a bit of a horrible hack, brought about because the
                // message formatting code searches through the message for words
                // matching a search term, then searches for URLs.  Thus it is
                // possible that the URL match we have just found is already
                // outside the PLAINTEXT tags.  In which case, we don't want to
                // remove it again.
                boolean alreadyOutsidePlainText =
                                 fromPrevEndToStart.endsWith("</PLAINTEXT><b>");

                msgBuffer.append(fromPrevEndToStart);

                String url = m.group().trim();
                int firstBadParen = findUnmatchedCloseBracket(url);
                if (firstBadParen != -1)
                {
                    // The URL contains a ')' with no matching '('.
                    // This is potentially valid, but it's more likely that the
                    // URL itself is contained within wider parentheses: e.g.
                    // "(here's a link: www.google.com)".
                    //
                    // Truncate the match before that bracket, and restart the
                    // regex matcher for the rest of the string so as to avoid
                    // missing matches starting in the truncated part -
                    // "(here's a link: www.foo.com)www.example.com is better".
                    //
                    // Note that for any URL containing a paren, the prefix
                    // before the paren is always a (syntactically) valid URL,
                    // so we don't need to revalidate the truncated URL.
                    url = url.substring(0, firstBadParen);
                    prevEnd = prevEnd + firstBadParen;
                    message = message.substring(m.start() + firstBadParen);
                    m = URL_PATTERN.matcher(message);
                    prevEnd = 0;
                }
                else
                {
                    prevEnd = m.end();
                }

                if (!alreadyOutsidePlainText)
                    msgBuffer.append(ChatHtmlUtils.createEndPlainTextTag(contentType));
                msgBuffer.append("<A href=\"");
                if (url.startsWith("www"))
                    msgBuffer.append("http://");
                msgBuffer.append(url);
                msgBuffer.append("\">");
                msgBuffer.append(url);

                if (!alreadyOutsidePlainText)
                    msgBuffer.append(ChatHtmlUtils.createStartPlainTextTag(contentType));
            }

            String fromPrevEndToEnd = message.substring(prevEnd);
            if (processHTMLChars)
                fromPrevEndToEnd = GuiUtils.escapeHTMLChars(fromPrevEndToEnd);
            msgBuffer.append(fromPrevEndToEnd);
        }

        return msgBuffer.toString();
    }

    /**
     * Formats message new lines.
     *
     * @param message The source message string.
     * @param contentType message contentType (html or plain text)
     * @return The message string with properly formatted new lines.
     */
    private static String processNewLines(String message, String contentType)
    {
        /*
         * <br> tags are needed to visualize a new line in the html format, but
         * when copied to the clipboard they are exported to the plain text
         * format as ' ' and not as '\n'.
         *
         * See bug N4988885:
         * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4988885
         *
         * To fix this we need "&#10;" - the HTML-Code for ASCII-Character No.10
         * (Line feed).
         */
        Matcher divMatcher = DIV_PATTERN.matcher(message);
        String openingTag = "";
        String closingTag = "";
        if (divMatcher.find())
        {
            openingTag = divMatcher.group(1);
            message = divMatcher.group(2);
            closingTag = divMatcher.group(3);
        }
        return
            openingTag +
            message
                .replaceAll(
                    "\n",
                    ChatHtmlUtils.createEndPlainTextTag(contentType)
                    + "<BR/>&#10;"
                    + ChatHtmlUtils.createStartPlainTextTag(contentType))
            + closingTag;
    }

    /**
     * Formats HTML tags &lt;br/&gt; to &lt;br&gt; or &lt;BR/&gt; to &lt;BR&gt;.
     * The reason of this function is that the ChatPanel does not support
     * &lt;br /&gt; closing tags (XHTML syntax), thus we have to remove every
     * slash from each &lt;br /&gt; tags.
     * @param message The source message string.
     * @return The message string with properly formatted &lt;br&gt; tags.
     */
    private static String processBrTags(String message)
    {
        // The resulting message after being processed by this function.
        StringBuilder processedMessage = new StringBuilder();

        // Compile the regex to match something like <br .. /> or <BR .. />.
        // This regex is case sensitive and keeps the style or other
        // attributes of the <br> tag.
        Matcher m
            = Pattern.compile("<\\s*[bB][rR](.*?)(/\\s*>)").matcher(message);
        int start = 0;

        // while we find some <br /> closing tags with a slash inside.
        while (m.find())
        {
            // First, we have to copy all the message preceding the <br> tag.
            processedMessage.append(message.substring(start, m.start()));
            // Then, we find the position of the slash inside the tag.
            int slash_index = m.group().lastIndexOf("/");
            // We copy the <br> tag till the slash exclude.
            processedMessage.append(m.group().substring(0, slash_index));
            // We copy all the end of the tag following the slash exclude.
            processedMessage.append(m.group().substring(slash_index+1));
            start = m.end();
        }
        // Finally, we have to add the end of the message following the last
        // <br> tag, or the whole message if there is no <br> tag.
        processedMessage.append(message.substring(start));

        return processedMessage.toString();
    }

    /**
     * Formats HTML tags &lt;img ... /&gt; to &lt; img ... &gt;&lt;/img&gt; or
     * &lt;IMG ... /&gt; to &lt;IMG&gt;&lt;/IMG&gt;.
     * The reason of this function is that the ChatPanel does not support
     * &lt;img /&gt; tags (XHTML syntax).
     * Thus, we remove every slash from each &lt;img /&gt; and close it with a
     * separate closing tag.
     * @param message The source message string.
     * @return The message string with properly formatted &lt;img&gt; tags.
     */
    private static String processImgTags(String message)
    {
        // The resulting message after being processed by this function.
        StringBuilder processedMessage = new StringBuilder();

        // Compile the regex to match something like <img ... /> or
        // <IMG ... />. This regex is case sensitive and keeps the style,
        // src or other attributes of the <img> tag.
        Pattern p = Pattern.compile("<\\s*[iI][mM][gG](.*?)(/\\s*>)");
        Matcher m = p.matcher(message);
        int slash_index;
        int start = 0;

        // while we find some <img /> self-closing tags with a slash inside.
        while (m.find())
        {
            // First, we have to copy all the message preceding the <img> tag.
            processedMessage.append(message.substring(start, m.start()));
            // Then, we find the position of the slash inside the tag.
            slash_index = m.group().lastIndexOf("/");
            // We copy the <img> tag till the slash exclude.
            processedMessage.append(m.group().substring(0, slash_index));
            // We copy all the end of the tag following the slash exclude.
            processedMessage.append(m.group().substring(slash_index+1));
            // We close the tag with a separate closing tag.
            processedMessage.append("</img>");
            start = m.end();
        }
        // Finally, we have to add the end of the message following the last
        // <img> tag, or the whole message if there is no <img> tag.
        processedMessage.append(message.substring(start));

        return processedMessage.toString();
    }

    /**
     * Finds the first incorrectly matched '[' ']' or '(' ')' pair.
     * @param text The string to search.
     * @return The index of the first unmatched ')' or ']', or -1 if there are none.
     */
    private static int findUnmatchedCloseBracket(String text)
    {
        Deque<Character> stack = new ArrayDeque<>();

        for (int ii = 0; ii < text.length(); ii++)
        {
            char c = text.charAt(ii);

            if (c == '(' || c == '[')
            {
                stack.push(c);
            }
            else if (c == ')' || c == ']')
            {
                if (stack.isEmpty())
                {
                    return ii;
                }
                else
                {
                    char bracket = stack.pop();
                    if (!((bracket == '(' && c == ')') ||
                          (bracket == '[' && c == ']')))
                    {
                        return ii;
                    }
                }
            }
        }

        return -1;
    }

    /**
     * Parses a string and calls on the various replacement services to replace select
     * parts of it with images/other media. Currently, it calls on emoji and emoticon
     * replacement services, and therefore replaces emoticons/emojis
     * in the message with the appropriate image.
     * @param stringForReplacements The string on which to perform replacements
     * @return The string with replacements performed on it
     */
    @VisibleForTesting
    public static String performReplacementsOnString(String stringForReplacements)
    {
        ConfigurationService cfg = GuiActivator.getConfigurationService();

        ArrayList<DisplayablePartOfMessage> tempChatPartsArray = new ArrayList<>();
        tempChatPartsArray.add(new TextPartOfMessage(stringForReplacements));

        ArrayList<DisplayablePartOfMessage> finalChatPartsArray = new ArrayList<>();

        sLog.debug("Starting replacement processes on string.");

        // Loops over the message once per replacement service (e.g. emoticon
        // replacement, emoji replacement) and replaces bits of text with
        // appropriate images/multimedia.
        for (Map.Entry<String, ChatPartReplacementService> entry :
                GuiActivator.getReplacementSources().entrySet())
        {
            // The replacementService object determines what is being replaced
            // and what it is being replaced by. For example, emoticon string
            // (':-)') by images, or emoji codepoints by images.
            ChatPartReplacementService replacementService = entry.getValue();

            if (!(cfg.user().getBoolean(ReplacementProperty.getPropertyName(
                   replacementService.getServiceName()), true)))
            {
                continue;
            }

            sLog.debug("Starting the following replacement on the message: " + replacementService.getClass());

            finalChatPartsArray.clear();
            for (DisplayablePartOfMessage displayablePartOfMessage : tempChatPartsArray)
            {
                // We only perform replacements on text parts, because non-text
                // parts must have come from a previous replacementService
                // replacing some text.
                if (!TextPartOfMessage.class.isInstance(displayablePartOfMessage))
                {
                    finalChatPartsArray.add(displayablePartOfMessage);
                    continue;
                }

                finalChatPartsArray.addAll(replacementService.
                    replaceText((TextPartOfMessage) displayablePartOfMessage));

            }

            tempChatPartsArray = new ArrayList<>(finalChatPartsArray);
        }

        StringBuilder stringToOutput = new StringBuilder();

        for (DisplayablePartOfMessage x : finalChatPartsArray)
        {
            stringToOutput.append(x.toHTML());
        }

        return stringToOutput.toString();
    }
}
