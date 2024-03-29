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
import net.java.sip.communicator.util.*;

/**
 * Implements standard functionality of <tt>ProtocolProviderService</tt> in
 * order to make it easier for implementers to provide complete solutions while
 * focusing on protocol-specific details.
 *
 * @author Lyubomir Marinov
 */
public abstract class AbstractProtocolProviderService
    implements ProtocolProviderService
{
    /**
     * The <tt>Logger</tt> instances used by the
     * <tt>AbstractProtocolProviderService</tt> class and its instances for
     * logging output.
     */
    private static final Logger logger =
        Logger.getLogger(AbstractProtocolProviderService.class);

    /**
     * A list of all listeners registered for
     * <tt>RegistrationStateChangeEvent</tt>s.
     */
    private final List<RegistrationStateChangeListener> registrationListeners =
            new ArrayList<>();

    /**
     * The hashtable with the operation sets that we support locally.
     */
    private final Map<String, OperationSet> supportedOperationSets
        = new Hashtable<>();

    public AbstractProtocolProviderService()
    {
        logger.info("Create " + this);
    }

    /**
     * Registers the specified listener with this provider so that it would
     * receive notifications on changes of its state or other properties such
     * as its local address and display name.
     *
     * @param listener the listener to register.
     */
    public void addRegistrationStateChangeListener(
        RegistrationStateChangeListener listener)
    {
        synchronized(registrationListeners)
        {
            if (!registrationListeners.contains(listener))
                registrationListeners.add(listener);
        }
    }

    /**
     * Adds a specific <tt>OperationSet</tt> implementation to the set of
     * supported <tt>OperationSet</tt>s of this instance. Serves as a type-safe
     * wrapper around {@link #supportedOperationSets} which works with class
     * names instead of <tt>Class</tt> and also shortens the code which performs
     * such additions.
     *
     * @param <T> the exact type of the <tt>OperationSet</tt> implementation to
     * be added
     * @param opsetClass the <tt>Class</tt> of <tt>OperationSet</tt> under the
     * name of which the specified implementation is to be added
     * @param opset the <tt>OperationSet</tt> implementation to be added
     */
    protected <T extends OperationSet> void addSupportedOperationSet(
            Class<T> opsetClass,
            T opset)
    {
        supportedOperationSets.put(opsetClass.getName(), opset);
    }

    /**
     * Removes an <tt>OperationSet</tt> implementation from the set of
     * supported <tt>OperationSet</tt>s for this instance.
     *
     * @param <T> the exact type of the <tt>OperationSet</tt> implementation to
     * be added
     * @param opsetClass the <tt>Class</tt> of <tt>OperationSet</tt> under the
     * name of which the specified implementation is to be added
     */
    protected <T extends OperationSet> void removeSupportedOperationSet(
                                                Class<T> opsetClass)
    {
        supportedOperationSets.remove(opsetClass.getName());
    }

    /**
     * Removes all <tt>OperationSet</tt> implementation from the set of
     * supported <tt>OperationSet</tt>s for this instance.
     */
    protected void clearSupportedOperationSet()
    {
        supportedOperationSets.clear();
    }

    /**
     * Creates a RegistrationStateChange event corresponding to the specified
     * old and new states and notifies all currently registered listeners.
     *
     * @param oldState the state that the provider had before the change
     * occurred
     * @param newState the state that the provider is currently in.
     * @param reasonCode a value corresponding to one of the REASON_XXX fields
     * of the RegistrationStateChangeEvent class, indicating the reason for
     * this state transition.
     * @param reason a String further explaining the reason code or null if
     * no such explanation is necessary.
     */
    public void fireRegistrationStateChanged( RegistrationState oldState,
                                               RegistrationState newState,
                                               int               reasonCode,
                                               String            reason)
    {
        RegistrationStateChangeEvent event =
            new RegistrationStateChangeEvent(
                            this, oldState, newState, reasonCode, reason);

        RegistrationStateChangeListener[] listeners;
        synchronized (registrationListeners)
        {
            listeners
                = registrationListeners.toArray(
                        new RegistrationStateChangeListener[
                                registrationListeners.size()]);
        }

        logger.debug(
                    this + " dispatching " + event + " to " + listeners.length
                        + " listeners.");

        for (RegistrationStateChangeListener listener : listeners)
            try
            {
                listener.registrationStateChanged(event);
            }
            catch (Throwable throwable)
            {
                /*
                 * The registration state has already changed and we're not
                 * using the RegistrationStateChangeListeners to veto the change
                 * so it doesn't make sense to, for example, disconnect because
                 * one of them malfunctioned.
                 *
                 * Of course, death cannot be ignored.
                 */
                if (throwable instanceof ThreadDeath)
                    throw (ThreadDeath) throwable;
                logger.error(
                    "An error occurred while executing "
                        + "RegistrationStateChangeListener"
                        + "#registrationStateChanged"
                        + "(RegistrationStateChangeEvent) of "
                        + listener,
                    throwable);
            }

        logger.trace("Done.");
    }

    /**
     * Returns the operation set corresponding to the specified class or null if
     * this operation set is not supported by the provider implementation.
     *
     * @param <T> the exact type of the <tt>OperationSet</tt> that we're looking
     * for
     * @param opsetClass the <tt>Class</tt> of the operation set that we're
     * looking for.
     * @return returns an <tt>OperationSet</tt> of the specified <tt>Class</tt>
     * if the underlying implementation supports it; <tt>null</tt>, otherwise.
     */
    @SuppressWarnings("unchecked")
    public <T extends OperationSet> T getOperationSet(Class<T> opsetClass)
    {
        return (T) supportedOperationSets.get(opsetClass.getName());
    }

    /**
     * Returns the protocol display name. This is the name that would be used
     * by the GUI to display the protocol name.
     *
     * @return a String containing the display name of the protocol this service
     * is implementing
     */
    public String getProtocolDisplayName()
    {
        String displayName
            = getAccountID().getAccountPropertyString(
                    ProtocolProviderFactory.PROTOCOL);

        return (displayName == null) ? getProtocolName() : displayName;
    }

    /**
     * Returns an array containing all operation sets supported by the current
     * implementation. When querying this method users must be prepared to
     * receive any subset of the OperationSet-s defined by this service. They
     * MUST ignore any OperationSet-s that they are not aware of and that may be
     * defined by future version of this service. Such "unknown" OperationSet-s
     * though not encouraged, may also be defined by service implementors.
     *
     * @return a java.util.Map containing instance of all supported operation
     *         sets mapped against their class names (e.g.
     *         OperationSetPresence.class.getName()) .
     */
    public Map<String, OperationSet> getSupportedOperationSets()
    {
        return new Hashtable<>(supportedOperationSets);
    }

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
    @SuppressWarnings("unchecked")
    public Collection<Class<? extends OperationSet>>
                                            getSupportedOperationSetClasses()
    {
        Collection<Class<? extends OperationSet>> opSetClasses
            = new ArrayList<>();

        Iterator<String> opSets
            = getSupportedOperationSets().keySet().iterator();

        while (opSets.hasNext())
        {
            String opSetClassName = opSets.next();
            try
            {
                opSetClasses.add(
                    (Class<? extends OperationSet>) getSupportedOperationSets()
                        .get(opSetClassName).getClass().getClassLoader()
                            .loadClass(opSetClassName));
            }
            catch (ClassNotFoundException e)
            {
                e.printStackTrace();
            }
        }

        return opSetClasses;
    }

    /**
     * Indicates whether or not this provider is registered
     *
     * @return <tt>true</tt> if the provider is currently registered and
     * <tt>false</tt> otherwise.
     */
    public boolean isRegistered()
    {
        return getRegistrationState().equals(RegistrationState.REGISTERED);
    }

    /**
     * Whether the protocol provider has completed initialization
     * @return the initialization state
     */
    public boolean isInitialized()
    {
       return true;
    }

    /**
     * Removes the specified registration state change listener so that it does
     * not receive any further notifications upon changes of the
     * RegistrationState of this provider.
     *
     * @param listener the listener to register for
     * <tt>RegistrationStateChangeEvent</tt>s.
     */
    public void removeRegistrationStateChangeListener(
        RegistrationStateChangeListener listener)
    {
        synchronized(registrationListeners)
        {
            registrationListeners.remove(listener);
        }
    }

    /**
     * Clear all registration state change listeners.
     */
    public void clearRegistrationStateChangeListener()
    {
        synchronized(registrationListeners)
        {
            registrationListeners.clear();
        }
    }

    /**
     * A clear display for ProtocolProvider when its printed in logs.
     * We now redact the account ID for privacy purposes.
     * @return the class name and the currently handled account.
     */
    public String toString()
    {
        return getClass().getSimpleName() +
            "(" +
            (getAccountID() != null ? getAccountID().getLoggableAccountID() : "no account ID") +
            ") " +
            Integer.toHexString(System.identityHashCode(this));
    }

    public boolean supportsStatus()
    {
        // By default, all accounts support status
        return true;
    }

    /**
     * Whether this protocol provider supports reconnection using the
     * reconnect plugin
     *
     * @return whether the protocol provider supports reconnection using the
     * reconnect plugin
     */
    public boolean supportsReconnection()
    {
        return true;
    }

    public boolean hasIpChanged()
    {
        logger.debug("Protocol " + this + " does not support ip change " +
                     "queries; return false");
        return false;
    }

    /**
     *  {@inheritDoc}
     *
     * <p>The default implementation of this method does nothing, so does not
     * prevent the
     * {@link net.java.sip.communicator.plugin.reconnectplugin.ReconnectPluginActivator ReconnectPlugin}
     * from running its
     * {@link net.java.sip.communicator.plugin.reconnectplugin.ReconnectPluginActivator#reconnect reconnect()}
     * method.</p>
     */
    @Override
    public void pollConnection()
    {
        // Default implementation does nothing
    }
}
