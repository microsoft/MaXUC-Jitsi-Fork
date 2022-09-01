/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

import java.io.*;

import javax.swing.text.html.*;
import javax.swing.text.html.parser.*;

/**
 * A utility class that allows to extract the text content of an HTML page
 * stripped from all formatting tags.
 *
 * @author Emil Ivov <emcho at sip-communicator.org>
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class Html2Text
{
    /**
     * The <tt>Logger</tt> used by the <tt>Html2Text</tt> class for logging
     * output.
     */
    private static final Logger logger
        = Logger.getLogger(Html2Text.class);

    /**
     * The HTML parser used by {@link #extractText(String)} to parse HTML so
     * that plain text can be extracted from it.
     */
    private static HTMLParserCallback parser;

    /**
     * A utility method that allows to extract the text content of an HTML page
     * stripped from all formatting tags. Method is synchronized to avoid
     * concurrent access to the underlying <tt>HTMLEditorKit</tt>.
     *
     * @param html the HTML string that we will extract the text from.
     * @return the text content of the <tt>html</tt> parameter.
     */
    public static synchronized String extractText(String html)
    {
        if(html == null)
            return null;

        if (parser == null)
            parser = new HTMLParserCallback();

        try
        {

            try (StringReader in = new StringReader(html))
            {
                return parser.parse(in);
            }
        }
        catch (Exception ex)
        {
            logger.info("Failed to extract plain text from html="+html, ex);
            return html;
        }
    }

    /**
     * The ParserCallback that will parse the HTML.
     */
    private static class HTMLParserCallback
        extends HTMLEditorKit.ParserCallback
    {
        /**
         * The <tt>StringBuilder</tt> which accumulates the parsed text while it
         * is being parsed.
         */
        private StringBuilder sb;

        /**
         * Parses the text contained in the given reader.
         *
         * @param in the reader to parse.
         * @return the parsed text
         * @throws IOException thrown if we fail to parse the reader.
         */
        public String parse(Reader in)
            throws IOException
        {
            sb = new StringBuilder();

            String s;

            try
            {
                new ParserDelegator().parse(in, this, /* ignoreCharSet */ true);
                s = sb.toString();
            }
            finally
            {
                /*
                 * Since the Html2Text class keeps this instance in a static
                 * reference, the field sb should be reset to null as soon as
                 * completing its goad in order to avoid keeping the parsed
                 * text in memory after it is no longer needed i.e. to prevent
                 * a memory leak. This method has been converted to return the
                 * parsed string instead of having a separate getter method for
                 * the parsed string for the same purpose.
                 */
                sb = null;
            }
            return s;
        }

        /**
         * Appends the given text to the string buffer.
         *
         * @param text the text of a text node which has been parsed from the
         * specified HTML
         * @param pos the zero-based position of the specified <tt>text</tt> in
         * the specified HTML
         */
        @Override
        public void handleText(char[] text, int pos)
        {
            sb.append(text);
        }
    }
}
