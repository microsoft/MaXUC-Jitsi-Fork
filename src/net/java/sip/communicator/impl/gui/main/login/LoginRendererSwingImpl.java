/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.login;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

/**
 * The <tt>LoginRendererSwingImpl</tt> provides a Swing base implementation of
 * the <tt>LoginRenderer</tt> interface.
 *
 * @author Yana Stamcheva
 */
public class LoginRendererSwingImpl
    implements LoginRenderer
{
    /**
     * The logger.
     */
    private static final Logger logger =
        Logger.getLogger(LoginRendererSwingImpl.class);

    private final AbstractMainFrame mainFrame
        = GuiActivator.getUIService().getMainFrame();

    /**
     * Adds the user interface related to the given protocol provider.
     *
     * @param protocolProvider the protocol provider for which we add the user
     * interface
     */
    public void addProtocolProviderUI(ProtocolProviderService protocolProvider)
    {
        this.mainFrame.addProtocolProvider(protocolProvider);
    }

    /**
     * Removes the user interface related to the given protocol provider.
     *
     * @param protocolProvider the protocol provider to remove
     */
    public void removeProtocolProviderUI(
        ProtocolProviderService protocolProvider)
    {
        this.mainFrame.removeProtocolProvider(protocolProvider);
    }

    /**
     * Removes the user interface related to the protocol provider with the
     * given unique account ID.
     *
     * @param accountID unique account ID of protocol provider to remove
     */
    public void removeProtocolProviderUI(AccountID accountID)
    {
        this.mainFrame.removeProtocolProvider(accountID);
    }

    /**
     * Called when the given protocol provider is connecting.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> that is
     * connecting
     * @param date the date on which the event occurred
     */
    public void protocolProviderConnecting(
        ProtocolProviderService protocolProvider, long date)
    {
        OperationSetPresence presence
            = AccountStatusUtils.getProtocolPresenceOpSet(protocolProvider);

        if (presence != null)
        {
            logger.info("Setting authorization handler for " + protocolProvider);
            presence.setAuthorizationHandler(
                    GuiActivator.getAuthorizationHandlerService());
        }
        else
        {
            logger.debug("Null presence ops set for " + protocolProvider);
        }
    }

    /**
     * Called when the given protocol provider is connected.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> that is
     * connected
     * @param date the date on which the event occurred
     */
    public void protocolProviderConnected(
        ProtocolProviderService protocolProvider, long date)
    {
        OperationSetMultiUserChat multiUserChat =
            mainFrame.getMultiUserChatOpSet(protocolProvider);

        if(multiUserChat != null && ConfigurationUtils.isMultiUserChatEnabled() &&
            GuiActivator.getUIService() != null &&
            GuiActivator.getUIService().getConferenceChatManager() != null)
        {
            GuiActivator.getUIService().getConferenceChatManager()
                .getChatRoomList().synchronizeOpSetWithLocalContactList(
                    protocolProvider, multiUserChat);
        }
    }

    /**
     * Indicates that a protocol provider connection has failed.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, which
     * connection failed
     * @param loginManagerCallback the <tt>LoginManager</tt> implementation,
     * which is managing the process
     */
    public void protocolProviderConnectionFailed(
        ProtocolProviderService protocolProvider,
        LoginManager loginManagerCallback)
    {
        AccountID accountID = protocolProvider.getAccountID();
        String errorMessage = GuiActivator.getResources().getI18NString(
            "service.gui.LOGIN_NETWORK_ERROR",
            new String[]
               { accountID.getUserID(), accountID.getService() });

        int result =
            new MessageDialog(
                null,
                GuiActivator.getResources()
                    .getI18NString("service.gui.ERROR"),
                errorMessage,
                GuiActivator.getResources()
                    .getI18NString("service.gui.RETRY"), false)
            .showDialog();

        if (result == MessageDialog.OK_RETURN_CODE)
        {
            loginManagerCallback.login(protocolProvider);
        }
    }

    /**
     * Returns the <tt>SecurityAuthority</tt> implementation related to this
     * login renderer.
     *
     * @param protocolProvider the specific <tt>ProtocolProviderService</tt>,
     * for which we're obtaining a security authority
     * @return the <tt>SecurityAuthority</tt> implementation related to this
     * login renderer
     */
    public SecurityAuthority getSecurityAuthorityImpl(
        ProtocolProviderService protocolProvider)
    {
        return GuiActivator.getUIService()
                .getDefaultSecurityAuthority(protocolProvider);
    }

}
