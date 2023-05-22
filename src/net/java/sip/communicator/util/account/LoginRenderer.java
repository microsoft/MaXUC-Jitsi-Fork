/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util.account;

import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>LoginRenderer</tt> is the renderer of all login related operations.
 *
 * @author Yana Stamcheva
 */
public interface LoginRenderer
{
    /**
     * Adds the user interface related to the given protocol provider.
     *
     * @param protocolProvider the protocol provider for which we add the user
     * interface
     */
    void addProtocolProviderUI(
            ProtocolProviderService protocolProvider);

    /**
     * Removes the user interface related to the given protocol provider.
     *
     * @param protocolProvider the protocol provider to remove
     */
    void removeProtocolProviderUI(ProtocolProviderService protocolProvider);

    /**
     * Removes the user interface related to the protocol provider with the
     * given unique account ID.
     *
     * @param accountID unique account ID of protocol provider to remove
     */
    void removeProtocolProviderUI(AccountID accountID);

    /**
     * Indicates that the given protocol provider is now connecting.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> that is
     * connecting
     * @param date the date on which the event occured
     */
    void protocolProviderConnecting(
            ProtocolProviderService protocolProvider,
            long date);

    /**
     * Indicates that the given protocol provider is now connected.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> that is
     * connected
     * @param date the date on which the event occured
     */
    void protocolProviderConnected(
            ProtocolProviderService protocolProvider,
            long date);

    /**
     * Indicates that a protocol provider connection has failed.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, which
     * connection failed
     * @param loginManagerCallback the <tt>LoginManager</tt> implementation,
     * which is managing the process
     */
    void protocolProviderConnectionFailed(
            ProtocolProviderService protocolProvider,
            LoginManager loginManagerCallback);

    /**
     * Returns the <tt>SecurityAuthority</tt> implementation related to this
     * login renderer.
     *
     * @param protocolProvider the specific <tt>ProtocolProviderService</tt>,
     * for which we're obtaining a security authority
     * @return the <tt>SecurityAuthority</tt> implementation related to this
     * login renderer
     */
    SecurityAuthority getSecurityAuthorityImpl(
            ProtocolProviderService protocolProvider);

    /**
     * Indicates if the given <tt>protocolProvider</tt> related user interface
     * is already rendered.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, which
     * related user interface we're looking for
     * @return <tt>true</tt> if the given <tt>protocolProvider</tt> related user
     * interface is already rendered
     */
    boolean containsProtocolProviderUI(
            ProtocolProviderService protocolProvider);
}
