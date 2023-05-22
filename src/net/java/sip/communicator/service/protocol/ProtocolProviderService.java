/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * The ProtocolProvider interface should be implemented by bundles that wrap
 * Instant Messaging and telephony protocol stacks. It gives the user interface
 * a way to plug into these stacks and receive notifications on status change
 * and incoming calls, as well as deliver user requests for establishing or
 * ending calls, putting peers on hold etc.
 * <p>
 * An instance of a ProtocolProviderService corresponds to a particular user
 * account and all operations performed through a provider (sending messages,
 * modifying contact lists, receiving calls)would pertain to this particular
 * user account.
 *<p>
 * ProtocolProviderService instances are created through the provider factory.
 * Each protocol provider is assigned a unique AccountID instance that uniquely
 * identifies it. Account id's for different accounts are guaranteed to be
 * different and in the same time the ID of a particular account against a given
 * service over any protocol will always be the same (so that we detect attempts
 * for creating the same account twice.)
 *
 * @author Emil Ivov
 * @see AccountID
 */
public interface ProtocolProviderService
{
    /**
     * The name of the property containing the number of binds that a Protocol
     * Provider Service Implementation should execute in case a port is already
     * bound to (each retry would be on a new random port).
     */
    String BIND_RETRIES_PROPERTY_NAME
        = "net.java.sip.communicator.service.protocol.BIND_RETRIES";

    /**
     * The default number of binds that a Protocol Provider Service
     * Implementation should execute in case a port is already bound to
     * (each retry would be on a new random port).
     */
    int BIND_RETRIES_DEFAULT_VALUE = 50;

    /**
     * Starts the registration process. Connection details such as
     * registration server, user name/number are provided through the
     * configuration service through implementation specific properties.
     *
     * @param authority the security authority that will be used for resolving
     *        any security challenges that may be returned during the
     *        registration or at any moment while we're registered.
     * @throws OperationFailedException with the corresponding code if the
     * registration fails for some reason (e.g. a networking error or an
     * implementation problem).
     */
    void register(SecurityAuthority authority)
        throws OperationFailedException;

    /**
     * Ends the registration of this protocol provider with the current
     * registration service.
     * @throws OperationFailedException with the corresponding code if the
     * registration fails for some reason (e.g. a networking error or an
     * implementation problem).
     */
    void unregister()
        throws OperationFailedException;

    /**
     * Indicates whether or not this provider is registered
     * @return true if the provider is currently registered and false otherwise.
     */
    boolean isRegistered();

    /**
     * Whether the protocol provider has completed initialization
     * @return the initialization state
     */
    boolean isInitialized();

    /**
     * Returns the state of the registration of this protocol provider with the
     * corresponding registration service.
     * @return ProviderRegistrationState
     */
    RegistrationState getRegistrationState();

    /**
     * Returns the short name of the protocol that the implementation of this
     * provider is based upon (like SIP, Jabber, ICQ/AIM, or others for
     * example). If the name of the protocol has been enumerated in
     * ProtocolNames then the value returned by this method must be the same as
     * the one in ProtocolNames.
     *
     * @return a String containing the short name of the protocol this service
     * is implementing (most often that would be a name in ProtocolNames).
     */
    String getProtocolName();

    /**
     * Returns the protocol display name. This is the name that would be used
     * by the GUI to display the protocol name.
     *
     * @return a String containing the display name of the protocol this service
     * is implementing
     */
    String getProtocolDisplayName();

    /**
     * Returns the protocol logo icon.
     * @return the protocol logo icon
     */
    ProtocolIcon getProtocolIcon();

    /**
     * Registers the specified listener with this provider so that it would
     * receive notifications on changes of its state or other properties such
     * as its local address and display name.
     * @param listener the listener to register.
     */
    void addRegistrationStateChangeListener(
            RegistrationStateChangeListener listener);

    /**
     * Removes the specified listener.
     * @param listener the listener to remove.
     */
    void removeRegistrationStateChangeListener(
            RegistrationStateChangeListener listener);

    /**
     * Returns an array containing all operation sets supported by the current
     * implementation. When querying this method users must be prepared to
     * receive any subset of the OperationSet-s defined by this service. They
     * MUST ignore any OperationSet-s that they are not aware of and that may be
     * defined by future versions of this service. Such "unknown" OperationSet-s
     * though not encouraged, may also be defined by service implementors.
     *
     * @return a {@link Map} containing instances of all supported operation
     * sets mapped against their class names (e.g.
     * <tt>OperationSetPresence.class.getName()</tt> associated with a
     * <tt>OperationSetPresence</tt> instance).
     */
    Map<String, OperationSet> getSupportedOperationSets();

    /**
     * Returns a collection containing all operation sets classes supported by
     * the current implementation. When querying this method users must be
     * prepared to receive any subset of the OperationSet-s defined by this
     * service. They MUST ignore any OperationSet-s that they are not aware of
     * and that may be defined by future versions of this service. Such
     * "unknown" OperationSet-s though not encouraged, may also be defined by
     * service implementors.
     *
     * @return a {@link Collection} containing instances of all supported
     * operation set classes (e.g. <tt>OperationSetPresence.class</tt>.
     */
    Collection<Class<? extends OperationSet>>
                                            getSupportedOperationSetClasses();

    /**
     * Returns the operation set corresponding to the specified class or
     * <tt>null</tt> if this operation set is not supported by the provider
     * implementation.
     *
     * @param <T> the type which extends <tt>OperationSet</tt> and which is to
     * be retrieved
     * @param opsetClass the <tt>Class</tt>  of the operation set that we're
     * looking for.
     * @return returns an OperationSet of the specified <tt>Class</tt> if the
     * underlying implementation supports it or null otherwise.
     */
    <T extends OperationSet> T getOperationSet(Class<T> opsetClass);

    /**
     * Makes the service implementation close all open sockets and release
     * any resources that it might have taken and prepare for shutdown/garbage
     * collection.
     */
    void shutdown();

    /**
     * A hashcode allowing usage of protocol providers as keys in Hashtables.
     * @return an int that may be used when storing protocol providers as
     * hashtable keys.
     */
    int hashCode();

    /**
     * Returns the AccountID that uniquely identifies the account represented by
     * this instance of the ProtocolProviderService.
     * @return the id of the account represented by this provider.
     */
    AccountID getAccountID();

    /**
     * Indicate if the signaling transport of this protocol instance uses a
     * secure (e.g. via TLS) connection.
     *
     * @return True when the connection is secured, false otherwise.
     */
    boolean isSignalingTransportSecure();

    /**
     * Returns the "transport" protocol of this instance used to carry the
     * control channel for the current protocol service.
     *
     * @return The "transport" protocol of this instance: UDP, TCP, TLS or
     * UNKNOWN.
     */
    TransportProtocol getTransportProtocol();

    /**
     * Returns true if accounts represented by this instance of the Protocol
     * provider service support status
     *
     * @return true if the protocol provider supports status
     */
    boolean supportsStatus();

    /**
     * Returns true if accounts represented by this instance of the Protocol
     * provider service allow the account status to be set based on the
     * account's registration state. If this is true and no accounts that
     * support presence status are available,  the client's global status will
     * be set to online/offline based on the account's registered/unregistered
     * state.  If presence status is available, that will be used in
     * preference to registration state to set the global status.
     *
     * @return true if the protocol provider allows the account status to be
     * set based on the account's registration state
     */
    boolean useRegistrationForStatus();

    /**
     * Returns true if accounts represented by this instance of the Protocol
     * provider service supports reconnection through the reconnect plugin
     *
     * @return whether the protocol provider supports reconnection using the
     * reconnect plugin
     */
    boolean supportsReconnection();

    /**
     * Determines whether there has been a change in the local IP address we use
     * when communicating using this protocol.
     *
     * @return <tt>true</tt> if the IP address this protocol uses has changed,
     * and <tt>false</tt> if it has not, or if the protocol does not expose IP
     * change details to interested parties. (This is a slightly leaky
     * abstraction, but helps simplify the architecture).
     */
    boolean hasIpChanged();

    /**
     * <p>This method is run by the
     * {@link net.java.sip.communicator.plugin.reconnectplugin.ReconnectPluginActivator ReconnectPlugin}
     * if our network changes but is not disconnected (e.g. if a laptop was
     * using WiFi but then was docked and gained a wired connection or
     * vice-versa).  Running this method means we will notice, before we've
     * reached the point where we need to re-register, whether a change to the
     * available networks also changes our preferred route to the registrar.
     * This allows us to re-register early to avoid passing through the
     * unregistered state, as it causes the
     * {@link net.java.sip.communicator.plugin.reconnectplugin.ReconnectPluginActivator ReconnectPlugin}
     * to conclude that it does not need to run its
     * {@link net.java.sip.communicator.plugin.reconnectplugin.ReconnectPluginActivator#reconnect reconnect()}
     * method (which would call unregister before re-registering).</p>
     *
     * <p>The {@link net.java.sip.communicator.plugin.reconnectplugin.ReconnectPluginActivator ReconnectPlugin}
     * would normally run its
     * {@link net.java.sip.communicator.plugin.reconnectplugin.ReconnectPluginActivator#reconnect reconnect()}
     * method for this sort of network change, as its call to
     * {@link net.java.sip.communicator.service.protocol.ProtocolProviderService#hasIpChanged hasIpChanged()}
     * would return true.  However, in this case, the
     * {@link net.java.sip.communicator.plugin.reconnectplugin.ReconnectPluginActivator ReconnectPlugin}'s
     * earlier call to this method causes the route to be changed before it
     * makes this check.  This can be desirable (e.g. for SIP it prevents us
     * from dropping calls), however it does mean that other things that
     * happen when we pass through the unregistered state are skipped (e.g.
     * SIP subscribes for presence), so they need to be handled explicitly
     * when this method is called by every protocol provider that implements
     * this method.</p>
     */
    void pollConnection();
}
