/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.replacement.emoticon;

import java.net.*;
import java.util.*;

/**
 * The <tt>Emoticon</tt> is used to store a emoticon.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class Emoticon
{
    /**
     * The description of the emoticon
     */
    private final String description;

    /**
     * The identifier of the emoticon icon.
     */
    private final String imageID;

    /**
     * The strings corresponding to this emoticon, including all case variants
     * e.g. :p, :P, :-p, :-P, etc.
     */
    private final List<String> emoticonStrings;

    /**
     * Creates an instance of <tt>Emoticon</tt>, by specifying the emoticon
     * image identifier and the strings corresponding to it.
     *
     * @param imageID The image identifier of the emoticon icon.
     * @param emoticonStrings A set of strings corresponding to the emoticon
     *                        icon. Matching of these strings is case-sensitive
     *                        so include all case variants.
     * @param description the description of the emoticon
     */
    public Emoticon(String imageID, String[] emoticonStrings,
        String description)
    {
        this.imageID = imageID;
        this.emoticonStrings = Collections
            .unmodifiableList(Arrays.asList(emoticonStrings.clone()));
        this.description = description;
    }

    /**
     * Returns the set of Strings, including all case variants, corresponding to
     * this emoticon.
     *
     * @return the set of Strings corresponding to this emoticon.
     */
    public List<String> getSmileyStrings()
    {
        return emoticonStrings;
    }

    /**
     * Returns the description of this emoticon.
     *
     * @return the description of this emoticon.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Returns the default String corresponding for this emoticon. For example
     * ":-)".
     *
     * @return the default String corresponding for this emoticon.
     */
    public String getDefaultString()
    {
        return emoticonStrings.get(0);
    }

    /**
     * Returns the identifier of the image corresponding to this emoticon.
     *
     * @return the identifier of the image corresponding to this emoticon.
     */
    public String getImageID()
    {
        return imageID;
    }

    /**
     * Returns the URL of the image corresponding to this emoticon.
     *
     * @return the URL of the image corresponding to this emoticon.
     */
    public String getImageURL()
    {
        URL url = EmoticonActivator.getResources().getImageURL(imageID);

        if (url == null)
            return null;

        return url.toString();
    }
}
