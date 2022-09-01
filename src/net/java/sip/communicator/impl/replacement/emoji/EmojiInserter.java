// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.replacement.emoji;

import java.util.List;

import net.java.sip.communicator.impl.gui.main.chat.DisplayablePartOfMessage;
import net.java.sip.communicator.impl.gui.main.chat.TextPartOfMessage;
import net.java.sip.communicator.impl.replacement.emoji.EmojiResources;
import net.java.sip.communicator.service.replacement.TabOfInsertableIcons;
import net.java.sip.communicator.service.replacement.TabbedImagesSelectorService;

public class EmojiInserter implements TabbedImagesSelectorService
{
    /**
     * Configuration label shown in the config form.
     */
    public static final String EMOJI_INSERTER_SERVICE = "EMOJI_INSERTER";

    private EmojiReplacementService replacementService;

    public EmojiInserter(EmojiReplacementService replacementService)
    {
        this.replacementService = replacementService;
    }

    /**
     * Returns the list of tabs, each of which contains a list of similarly themed emojis
     * to be displayed together on that tab
     * @return list of emoji tabs
     */
    @Override
    public List<TabOfInsertableIcons> getTabs()
    {
        return EmojiResources.getTabs();
    }

    /**
     * @return true if all the emoji icons are loaded
     */
    @Override
    public boolean allIconsLoaded()
    {
        return EmojiResources.allEmojisLoaded();
    }

    @Override
    public String getServiceName()
    {
        return replacementService.getServiceName();
    }

    @Override
    public List<DisplayablePartOfMessage> replaceText(TextPartOfMessage stringMessagePart)
    {
        return replacementService.replaceText(stringMessagePart);
    }
}
