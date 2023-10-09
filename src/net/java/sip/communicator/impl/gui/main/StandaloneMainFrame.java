// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main;

import java.awt.Dimension;
import java.awt.Font;

import net.java.sip.communicator.impl.gui.main.contactlist.ContactListPane;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.OperationSetMultiUserChat;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

/**
 * This UI class is no longer shown, as standalone meeting UI is shown in the
 * Electron frontend. This class persists as it is still used (but hidden) by
 * GuiActivator for a standalone client.
 */
public class StandaloneMainFrame
    extends AbstractMainFrame
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1L;

    public StandaloneMainFrame()
    {
        super(null, null, false);
    }

    @Override
    protected Dimension getMinimumBoundsSize()
    {
        return new Dimension(1, 1);
    }

    @Override
    protected Dimension getPreferredBoundsSize()
    {
        return getMinimumBoundsSize();
    }

    @Override
    public void setDefaultFont(Font font)
    {
        // Do nothing - UI component unused
    }

    @Override
    public ContactListPane getContactListPanel()
    {
        // Do nothing - UI component unused
        return null;
    }

    @Override
    public void enableUnknownContactView(boolean isEnabled)
    {
        // Do nothing - UI component unused
    }

    @Override
    public void selectTab(String tabName)
    {
        // Do nothing - UI component unused
    }

    @Override
    public void addProtocolProvider(ProtocolProviderService protocolProvider)
    {
        // Do nothing - UI component unused
    }

    @Override
    public void removeProtocolProvider(ProtocolProviderService protocolProvider)
    {
        // Do nothing - UI component unused
    }

    @Override
    public void removeProtocolProvider(AccountID accountID)
    {
        // Do nothing - UI component unused
    }

    @Override
    public OperationSetMultiUserChat getMultiUserChatOpSet(
        ProtocolProviderService protocolProvider)
    {
        // Do nothing - UI component unused
        return null;
    }

    @Override
    public boolean hasProtocolProvider(ProtocolProviderService protocolProvider)
    {
        // Do nothing - UI component unused
        return false;
    }

    @Override
    public int getProviderIndex(ProtocolProviderService protocolProvider)
    {
        // Do nothing - UI component unused
        return 0;
    }
}
