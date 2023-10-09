/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.sip;

import static org.jitsi.util.Hasher.logHasher;
import static org.jitsi.util.SanitiseUtils.sanitise;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;

import org.osgi.framework.*;

import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.argdelegation.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.UIService.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * The sip implementation of the URI handler. This class handles sip URIs by
 * trying to establish a call to them.
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public class UriHandlerSipImpl
    implements UriHandler, ServiceListener, AccountManagerListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>UriHandlerSipImpl</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(UriHandlerSipImpl.class);

    /**
     * The protocol provider factory that created us.
     */
    private final ProtocolProviderFactory protoFactory;

    /**
     * A reference to the OSGi registration we create with this handler.
     */
    private ServiceRegistration<?> ourServiceRegistration = null;

    /**
     * The object that we are using to synchronize our service registration.
     */
    private final Object registrationLock = new Object();

    /**
     * The <code>AccountManager</code> which loads the stored accounts of
     * {@link #protoFactory} and to be monitored when the mentioned loading is
     * complete so that any pending {@link #uris} can be handled
     */
    private AccountManager accountManager;

    /**
     * The indicator (and its synchronization lock) which determines whether the
     * stored accounts of {@link #protoFactory} have already been loaded.
     * <p>
     * Before the loading of the stored accounts (even if there're none) of the
     * <code>protoFactory</code> is complete, no handling of URIs is to be
     * performed because there's neither information which account to handle the
     * URI in case there're stored accounts available nor ground for warning the
     * user a registered account is necessary to handle URIs at all in case
     * there're no stored accounts.
     * </p>
     */
    private final boolean[] storedAccountsAreLoaded = new boolean[1];

    /**
     * The list of URIs which have received requests for handling before the
     * stored accounts of the {@link #protoFactory} have been loaded. They will
     * be handled as soon as the mentioned loading completes.
     */
    private List<String> uris;

    /**
     * The protocol that this URI handler handles (<tt>sip</tt>, <tt>tel</tt>,
     * <tt>callto</tt> or <tt>maxuccall</tt>).
     */
    private final String protocol;

    /**
     * Regex statement used to hash personal data contained in URIs i.e. the data that
     * follow the protocol, which will typically be a user's Directory Number (DN).
     */
    private final Pattern protocolPattern;

    /**
     * Creates an instance of this uri handler, so that it would start handling
     * URIs by passing them to the providers registered by <tt>protoFactory</tt>
     * .
     *
     * @param protoFactory the provider that created us.
     * @param protocol the protocol that this URI handler handles (<tt>sip</tt>,
     *                 <tt>tel</tt>, <tt>callto</tt> or <tt>maxuccall</tt>).
     *
     * @throws NullPointerException if <tt>protoFactory</tt> is <tt>null</tt>.
     */
    public UriHandlerSipImpl(ProtocolProviderFactorySipImpl protoFactory,
                             String protocol)
        throws NullPointerException
    {
        if (protoFactory == null)
        {
            throw new NullPointerException(
                "The ProtocolProviderFactory that a UriHandler is created with "
                    + " cannot be null.");
        }

        this.protoFactory = protoFactory;
        this.protocol = protocol;

        // Regex for URIs of the form "sip:12345...". For example, for this
        // SIP URI, it would pick out the "12345" part of the string.
        this.protocolPattern = Pattern.compile("(?<=" + protocol + ":)(.*)");

        hookStoredAccounts();

        this.protoFactory.getBundleContext().addServiceListener(this);
        /*
         * Registering the UriHandler isn't strictly necessary if the
         * requirement to register the protoFactory after creating this instance
         * is met.
         */
        registerHandlerService();
    }

    /**
     * Disposes of this <code>UriHandler</code> by, for example, removing the
     * listeners it has added in its constructor (in order to prevent memory
     * leaks, for one).
     */
    public void dispose()
    {
        protoFactory.getBundleContext().removeServiceListener(this);
        unregisterHandlerService();

        unhookStoredAccounts();
    }

    /**
     * Sets up (if not set up already) listening for the loading of the stored
     * accounts of {@link #protoFactory} in order to make it possible to
     * discover when the prerequisites for handling URIs are met.
     */
    private void hookStoredAccounts()
    {
        if (accountManager == null)
        {
            BundleContext bundleContext = protoFactory.getBundleContext();

            accountManager =
                (AccountManager) bundleContext.getService(bundleContext
                    .getServiceReference(AccountManager.class.getName()));
            accountManager.addListener(this);
        }
    }

    /**
     * Reverts (if not reverted already) the setup performed by a previous call
     * to {@link #hookStoredAccounts()}.
     */
    private void unhookStoredAccounts()
    {
        if (accountManager != null)
        {
            accountManager.removeListener(this);
            accountManager = null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.java.sip.communicator.service.protocol.event.AccountManagerListener
     * #handleAccountManagerEvent
     * (net.java.sip.communicator.service.protocol.event.AccountManagerEvent)
     */
    public void handleAccountManagerEvent(AccountManagerEvent event)
    {
        logger.debug("Handle event in SIP URI handler: " + event);

        /*
         * When the loading of the stored accounts of protoFactory is complete,
         * the prerequisites for handling URIs have been met so it's time to
         * load any handling requests which have come before the loading and
         * were thus delayed in uris.
         */
        if ((AccountManagerEvent.STORED_ACCOUNTS_LOADED == event.getType())
            && (protoFactory == event.getFactory()))
        {
            List<String> uris = null;

            synchronized (storedAccountsAreLoaded)
            {
                storedAccountsAreLoaded[0] = true;

                if (this.uris != null)
                {
                    uris = this.uris;
                    this.uris = null;
                }
            }

            unhookStoredAccounts();

            if (uris != null)
            {
                for (Iterator<String> uriIter = uris.iterator(); uriIter
                    .hasNext();)
                {
                    handleUri(uriIter.next());
                }
            }
        }
    }

    /**
     * Registers this UriHandler with the bundle context so that it could start
     * handling URIs
     */
    public void registerHandlerService()
    {
        synchronized (registrationLock)
        {
            String uriProtocol = getProtocol();

            if (ourServiceRegistration != null)
            {
                // ... we are already registered (this is probably
                // happening during startup)
                logger.debug("Already have serviceRegistration <" +
                             ourServiceRegistration + "> to handle " + uriProtocol);
                return;
            }

            logger.info(
                "Asked to register <" + this + "> as URI handler for " + uriProtocol);

            if (ConfigurationUtils.isCallingEnabled())
            {
                Hashtable<String, String> registrationProperties =
                        new Hashtable<>();

                registrationProperties.put(UriHandler.PROTOCOL_PROPERTY, uriProtocol);

                ourServiceRegistration =
                    SipActivator.bundleContext.registerService(UriHandler.class
                        .getName(), this, registrationProperties);
            }
            else
            {
                logger.info("Not registering URI handler for " +
                             uriProtocol + " as all calling is disabled");
            }
        }
    }

    /**
     * Unregisters this UriHandler from the bundle context.
     */
    public void unregisterHandlerService()
    {
        synchronized (registrationLock)
        {
            if (ourServiceRegistration != null)
            {
                logger.info("Unregistering service registration <" +
                                                  ourServiceRegistration + ">");
                ourServiceRegistration.unregister();
                ourServiceRegistration = null;
            }
        }
    }

    /**
     * Returns the protocol that this handler is responsible for or "sip" in
     * other words.
     *
     * @return the "sip" string to indicate that this handler is responsible for
     *         handling "sip" uris.
     */
    public String getProtocol()
    {
        return protocol;
    }

    /**
     * Parses the specified URI and creates a call with the currently active
     * telephony operation set.
     *
     * @param uri the SIP URI that we have to call.
     */
    public void handleUri(String uri)
    {
        String loggableUri = sanitiseUriForLogging(uri);
        logger.info("Handle SIP/TEL/CALLTO/MAXUCCALL URI: " + loggableUri);

        /*
         * TODO If the requirement to register the factory service after
         * creating this instance is broken, we'll end up not handling the URIs.
         */
        synchronized (storedAccountsAreLoaded)
        {
            if (!storedAccountsAreLoaded[0])
            {
                logger.info("Stored accounts not yet loaded - will handle URI later");

                if (uris == null)
                {
                    uris = new LinkedList<>();
                }
                uris.add(uri);
                return;
            }
        }

        if (!ConfigurationUtils.isCallingEnabled())
        {
            logger.warn("Ignoring SIP/TEL/CALLTO/MAXUCCALL URI as all calling is disabled: " + loggableUri);
            return;
        }

        // MAXUCCALL URIs are to be enabled for all users with calling features, regardless
        // of whether or not they are registered as the URL handler for the other SIP URIs.
        if (!uri.startsWith("maxuccall") && !ConfigurationUtils.isProtocolURLHandler())
        {
            logger.warn("Ignoring URI call as app is not configured to handle SIP/TEL/CALLTO URI protocols: " + loggableUri);
            return;
        }

        String decodedURI;
        try
        {
            // Replace instances of '+' with their percent-encoded form before
            // decoding, as URLDecoder interprets '+' as an encoding of the
            // ' ' character, but we want them to be read as literal plus signs.
            decodedURI = URLDecoder.decode(uri.replace("+", "%2B"), "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            logger.error("Failed to decode URI '" + loggableUri + "'", e);
            decodedURI = uri;
        }
        catch (IllegalArgumentException e)
        {
            logger.error("Failed to decode URI '" + loggableUri + "': " + e);
            decodedURI = uri;
        }

        // Simply hand the number to the UI service to make the call. That will
        // decide whether to use SIP or another protocol for the call.
        String number = decodedURI.substring(protocol.length() + 1);
        logger.info("Parsed URI to " + logHasher(number));

        if (SipActivator.getUIService() != null)
        {
            logger.info("Received URI to start a call: " + protocol);
            SipActivator.getAnalyticsService().onEvent(AnalyticsEventType.OUTBOUND_CALL,
                                                       "Calling from",
                                                       "URI: " + protocol);
            SipActivator.getUIService().createCall(Reformatting.NEEDED,
                                                   number);
        }
    }

    /**
     * Sanitises personal data (i.e. DNs) exposed by URIs.
     */
    private String sanitiseUriForLogging(String uri)
    {
        return sanitise(uri, protocolPattern);
    }

    /**
     * The point of implementing a service listener here is so that we would
     * only register our own uri handling service and thus only handle URIs
     * while the factory is available as an OSGi service. We remove ourselves
     * when our factory unregisters its service reference.
     *
     * @param event the OSGi <tt>ServiceEvent</tt>
     */
    public void serviceChanged(ServiceEvent event)
    {
        Object sourceService =
            SipActivator.bundleContext.getService(event.getServiceReference());

        // ignore anything but our protocol factory.
        if (sourceService != protoFactory)
        {
            return;
        }

        switch (event.getType())
        {
        case ServiceEvent.REGISTERED:
            // our factory has just been registered as a service ...
            registerHandlerService();
            break;
        case ServiceEvent.UNREGISTERING:
            // our factory just died
            unregisterHandlerService();
            break;
        default:
            // we don't care.
            break;
        }
    }
}
