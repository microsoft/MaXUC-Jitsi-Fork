/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.replacement.emoticon;

import java.util.*;

import net.java.sip.communicator.util.Logger;

import org.jitsi.service.resources.*;

/**
 * The <tt>EmoticonResources</tt> is used to access emoticon icons.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class EmoticonResources
{
    /**
     * The default pack of <tt>Emoticon</tt>s.
     */
    private static Collection<Emoticon> defaultEmoticonPack;

    private static final Logger sLog
        = Logger.getLogger(EmoticonResources.class);

    private static ResourceManagementService resources;

    /**
     * Load default emoticons pack.
     *
     * @return the ArrayList of all emoticons.
     */
    public static Collection<Emoticon> getDefaultEmoticonPack()
    {
        if (defaultEmoticonPack != null)
            return defaultEmoticonPack;

        resources = EmoticonActivator.getResources();

        List<Emoticon> defaultEmoticonList = new ArrayList<>();

        defaultEmoticonList.add(new Emoticon("service.gui.smileys.SMILE",
            new String[] {":)", ":-)"},
            resources.getI18NString("service.gui.smiley.SMILE_TEXT")));

        defaultEmoticonList.add(new Emoticon("service.gui.smileys.ANGRY",
            new String[] {"x(", "x-(", "X-(", "X("},
            resources.getI18NString("service.gui.smiley.ANGRY_TEXT")));

        defaultEmoticonList.add(new Emoticon("service.gui.smileys.THINKING",
            new String[] {":\\", ":-\\"},
            resources.getI18NString("service.gui.smiley.THINKING_TEXT")));

        defaultEmoticonList.add(new Emoticon("service.gui.smileys.TONGUEOUT",
            new String[] {":P", ":-P", ":p", ":-p"},
            resources.getI18NString("service.gui.smiley.TONGUEOUT_TEXT")));

        defaultEmoticonList.add(new Emoticon("service.gui.smileys.SAD",
            new String[] {":(", ":-("},
            resources.getI18NString("service.gui.smiley.SAD_TEXT")));

        defaultEmoticonList.add(new Emoticon("service.gui.smileys.CONFUSED",
            new String[] {":S", ":-S", ":s", ":-s"},
            resources.getI18NString("service.gui.smiley.CONFUSED_TEXT")));

        defaultEmoticonList.add(new Emoticon("service.gui.smileys.WINK",
            new String[] {";)", ";-)"},
            resources.getI18NString("service.gui.smiley.WINK_TEXT")));

        defaultEmoticonList.add(new Emoticon("service.gui.smileys.COOLDUDE",
            new String[] {"(cool)"},
                resources.getI18NString("service.gui.smiley.COOLDUDE_TEXT")));

        defaultEmoticonList.add(new Emoticon("service.gui.smileys.BLANK",
            new String[] {":|", ":-|"},
            resources.getI18NString("service.gui.smiley.BLANK_TEXT")));

        // Note this emoticon is case-sensitive as :d doesn't look like a smile
        defaultEmoticonList.add(new Emoticon("service.gui.smileys.BIGSMILE",
            new String[] {":D", ":-D"},
            resources.getI18NString("service.gui.smiley.BIGSMILE_TEXT")));

        defaultEmoticonList.add(new Emoticon("service.gui.smileys.EVIL",
            new String[] {"3:)", "3:-)"},
                resources.getI18NString("service.gui.smiley.EVIL_TEXT")));

        defaultEmoticonList.add(new Emoticon("service.gui.smileys.SHOCKED",
            new String[] {":O", ":-O", ":o", ":-o"},
                resources.getI18NString("service.gui.smiley.SHOCKED_TEXT")));

        defaultEmoticonPack
            = Collections.unmodifiableCollection(defaultEmoticonList);

        sLog.debug("Created default emoticon pack");
        return defaultEmoticonPack;
    }

    /**
     * Returns a Emoticon object for a given smiley string.
     * @param smileyString One of :-), ;-), etc.
     * @return A Emoticon object for a given smiley string.
     */
    public static Emoticon getEmoticon(String smileyString)
    {
        for (Emoticon smiley : getDefaultEmoticonPack())
            for (String srcString : smiley.getSmileyStrings())
                if (srcString.equals(smileyString))
                    return smiley;
        return null;
    }

    /**
     * Reloads smilies.
     */
    public static void reloadResources()
    {
        defaultEmoticonPack = null;
    }
}
