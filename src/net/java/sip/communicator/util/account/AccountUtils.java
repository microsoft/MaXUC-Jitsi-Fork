/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.account;

import java.util.*;

import org.jitsi.service.neomedia.codec.Constants;
import org.jitsi.service.neomedia.codec.EncodingConfiguration;
import org.jitsi.service.neomedia.device.*;
import org.jitsi.service.neomedia.format.*;
import org.osgi.framework.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>AccountUtils</tt> provides utility methods helping us to easily
 * obtain an account or a groups of accounts or protocol providers by some
 * specific criteria.
 *
 * @author Yana Stamcheva
 */
public class AccountUtils
{
    /**
     * The logger.
     */
    private static final Logger logger = Logger.getLogger(AccountUtils.class);

    /**
     * Returns an iterator over a list of all stored <tt>AccountID</tt>-s.
     *
     * @return an iterator over a list of all stored <tt>AccountID</tt>-s
     */
    public static Collection<AccountID> getStoredAccounts()
    {
        AccountManager accountManager =
            ServiceUtils.getService(UtilActivator.bundleContext,
                                    AccountManager.class);

        if (accountManager == null)
        {
            logger.warn("Asked for stored account before account manager available");
        }

        return accountManager == null ? new Vector<>() : accountManager.getStoredAccounts();
    }

    /**
     * Return the <tt>AccountID</tt> corresponding to the given string account
     * identifier.
     *
     * @param accountID the account identifier string
     * @return the <tt>AccountID</tt> corresponding to the given string account
     * identifier
     */
    public static AccountID getAccountForID(String accountID)
    {
        Collection<AccountID> allAccounts = getStoredAccounts();
        for(AccountID account : allAccounts)
        {
            if(account.getAccountUniqueID().equals(accountID))
                return account;
        }
        return null;
    }

    /**
     * Returns a list of all currently registered providers, which support the
     * given <tt>operationSetClass</tt>.
     *
     * @param opSetClass the operation set class for which we're looking
     * for providers
     * @return a list of all currently registered providers, which support the
     * given <tt>operationSetClass</tt>
     */
    public static List<ProtocolProviderService> getRegisteredProviders(
        Class<? extends OperationSet> opSetClass)
    {
        return getProviders(opSetClass, true);
    }

    public static List<ProtocolProviderService> getAllProviders(
        Class<? extends OperationSet> opSetClass)
    {
        List<ProtocolProviderService> providers = getProviders(opSetClass, false);
        return providers;
    }

    /**
     * Returns a list of providers which support the given operation set.  If
     * shouldBeRegistered is true, then all providers will be registered.
     * Otherwise, this returns all providers
     *
     * @param opSetClass the operation set calls we're interested in
     * @param shouldBeRegistered true if only registered providers should be returned
     * @return the list of providers
     */
    private static List<ProtocolProviderService> getProviders(
                                       Class<? extends OperationSet> opSetClass,
                                       boolean shouldBeRegistered)
    {
        List<ProtocolProviderService> opSetProviders
            = new LinkedList<>();

        for (ProtocolProviderFactory providerFactory : UtilActivator
            .getProtocolProviderFactories().values())
        {
            ServiceReference<?> serRef;
            ProtocolProviderService protocolProvider;

            for (AccountID accountID : providerFactory.getRegisteredAccounts())
            {
                serRef = providerFactory.getProviderForAccount(accountID);

                protocolProvider
                    = (ProtocolProviderService) UtilActivator.bundleContext
                        .getService(serRef);

                OperationSet operationSet = protocolProvider.getOperationSet(opSetClass);

                if (operationSet != null &&
                    (!shouldBeRegistered || protocolProvider.isRegistered()))
                {
                    opSetProviders.add(protocolProvider);
                }
            }
        }

        return opSetProviders;
    }

    /**
     * Returns a list of all currently registered telephony providers for the
     * given protocol name.
     * @param protocolName the protocol name
     * @param operationSetClass the operation set class for which we're looking
     * for providers
     * @return a list of all currently registered providers for the given
     * <tt>protocolName</tt> and supporting the given <tt>operationSetClass</tt>
     */
    private static List<ProtocolProviderService> getRegisteredProviders(
        String protocolName,
        Class<? extends OperationSet> operationSetClass)
    {
        List<ProtocolProviderService> opSetProviders
            = new LinkedList<>();

        ProtocolProviderFactory providerFactory
            = getProtocolProviderFactory(protocolName);

        if (providerFactory != null)
        {
            ServiceReference<?> serRef;
            ProtocolProviderService protocolProvider;

            for (AccountID accountID : providerFactory.getRegisteredAccounts())
            {
                serRef = providerFactory.getProviderForAccount(accountID);

                protocolProvider
                    = (ProtocolProviderService) UtilActivator.bundleContext
                        .getService(serRef);

                if (protocolProvider.getOperationSet(operationSetClass) != null
                    && protocolProvider.isRegistered())
                {
                    opSetProviders.add(protocolProvider);
                }
            }
        }
        return opSetProviders;
    }

    /**
     * Returns a list of all registered protocol providers that could be used
     * for the operation given by the operation set. Prefers the given preferred
     * protocol provider and preferred protocol name if they're available and
     * registered.
     *
     * @param opSet
     * @param preferredProvider
     * @param preferredProtocolName
     * @return a list of all registered protocol providers that could be used
     * for the operation given by the operation set
     */
    public static List<ProtocolProviderService> getOpSetRegisteredProviders(
        Class<? extends OperationSet> opSet,
        ProtocolProviderService preferredProvider,
        String                  preferredProtocolName)
    {
        List<ProtocolProviderService> providers
            = new ArrayList<>();

        if (preferredProvider != null)
        {
            if (preferredProvider.isRegistered())
                providers.add(preferredProvider);

            // If we have a provider, but it's not registered we try to
            // obtain all registered providers for the same protocol as the
            // given preferred provider.
            else
            {
                providers
                    = getRegisteredProviders(
                        preferredProvider.getProtocolName(), opSet);
            }
        }
        // If we don't have a preferred provider we try to obtain a
        // preferred protocol name and all registered providers for it.
        else
        {
            if (preferredProtocolName != null)
                providers
                    = getRegisteredProviders(
                        preferredProtocolName, opSet);
            // If the protocol name is null we simply obtain all telephony
            // providers.
            else
                providers = getRegisteredProviders(opSet);
        }

        return providers;
    }

    /**
     * @return the provider that supports
     * <tt>OperationSetBasicInstantMessaging</tt>, regardless of whether it is
     * registered.
     */
    public static ProtocolProviderService getImProvider()
    {
        List<ProtocolProviderService> providers =
                          getAllProviders(OperationSetBasicInstantMessaging.class);

        // Accession Desktop only supports one IM provider so just return the
        //first one in the list.
        return (providers.size() > 0) ? providers.get(0) : null;
    }

    /**
     * @return the provider that supports
     * <tt>OperationSetBasicTelephony</tt>, regardless of whether it is registered.
     */
    public static ProtocolProviderService getSipProvider()
    {
        List<ProtocolProviderService> providers =
                          getAllProviders(OperationSetBasicTelephony.class);

        // Accession Desktop only supports one SIP provider so just return the
        // first one in the list.
        return (providers.size() > 0) ? providers.get(0) : null;
    }

    /**
     * @return true if the provider that supports
     * <tt>OperationSetBasicInstantMessaging</tt> is registered, false
     * otherwise.
     */
    public static boolean isImProviderRegistered()
    {
        ProtocolProviderService imProvider = getImProvider();
        return (imProvider != null && imProvider.isRegistered());
    }

    /**
     * @return the provider that supports <tt>OperationSetGroupContacts</tt>.
     */
    public static ProtocolProviderService getGroupContactProvider()
    {
        List<ProtocolProviderService> providers =
                          getAllProviders(OperationSetGroupContacts.class);

        // Accession Desktop only supports one group contacts provider so just
        // return the first one in the list.
        return (providers.size() > 0) ? providers.get(0) : null;
    }

    /**
     * Get the account ID associated with the IM account.
     *
     * @return the IM account (even if it has been signed out) or null if there
     *         isn't one.
     */
    public static AccountID getImAccount()
    {
        ProtocolProviderService imProvider = getImProvider();

        if (imProvider != null)
            return imProvider.getAccountID();

        // There's no logged in IM account, but there may be a signed out chat
        // one:
        for (AccountID account : getStoredAccounts())
        {
            if (account.getProtocolName().equals(ProtocolNames.JABBER))
            {
                return account;
            }
        }

        return null;
    }

    /**
     * Get the account ID associated with the SIP account.
     *
     * @return the SIP account (even if it has been signed out) or null if there isn't one.
     */
    public static AccountID getSipAccount()
    {
        ProtocolProviderService sipProvider = getSipProvider();

        if (sipProvider != null)
        {
            return sipProvider.getAccountID();
        }

        // There's no logged in SIP account, but there may be a signed out one:
        for (AccountID account : getStoredAccounts())
        {
            if (account.getProtocolName().equals(ProtocolNames.SIP))
            {
                return account;
            }
        }

        return null;
    }

    /**
     * Returns the <tt>ProtocolProviderService</tt> corresponding to the given
     * account identifier that is registered in the given factory
     * @param accountID the identifier of the account
     * @return the <tt>ProtocolProviderService</tt> corresponding to the given
     * account identifier that is registered in the given factory
     */
    public static ProtocolProviderService getRegisteredProviderForAccount(
                                                        AccountID accountID)
    {
        for (ProtocolProviderFactory factory
                : UtilActivator.getProtocolProviderFactories().values())
        {
            if (factory.getRegisteredAccounts().contains(accountID))
            {
                ServiceReference<?> serRef
                    = factory.getProviderForAccount(accountID);

                if (serRef != null)
                {
                    return
                        (ProtocolProviderService)
                            UtilActivator.bundleContext.getService(serRef);
                }
            }
        }
        return null;
    }

    /**
     * Returns a <tt>ProtocolProviderFactory</tt> for a given protocol
     * provider.
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, which
     * factory we're looking for
     * @return a <tt>ProtocolProviderFactory</tt> for a given protocol
     * provider
     */
    public static ProtocolProviderFactory getProtocolProviderFactory(
            ProtocolProviderService protocolProvider)
    {
        return getProtocolProviderFactory(protocolProvider.getProtocolName());
    }

    /**
     * Returns a <tt>ProtocolProviderFactory</tt> for a given protocol
     * provider.
     * @param protocolName the name of the protocol
     * @return a <tt>ProtocolProviderFactory</tt> for a given protocol
     * provider
     */
    public static ProtocolProviderFactory getProtocolProviderFactory(
            String protocolName)
    {
        String osgiFilter = "("
            + ProtocolProviderFactory.PROTOCOL
            + "="+protocolName+")";

        ProtocolProviderFactory protocolProviderFactory = null;
        try
        {
            ServiceReference<?>[] serRefs
                = UtilActivator.bundleContext.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), osgiFilter);

            if (serRefs != null && serRefs.length > 0)
                protocolProviderFactory
                    = (ProtocolProviderFactory) UtilActivator.bundleContext
                        .getService(serRefs[0]);
        }
        catch (InvalidSyntaxException ex)
        {
            logger.error("AccountUtils : " + ex);
        }

        return protocolProviderFactory;
    }

    /**
     * Returns all registered protocol providers.
     *
     * @return a list of all registered providers
     */
    public static Collection<ProtocolProviderService> getRegisteredProviders()
    {
        List<ProtocolProviderService> registeredProviders
            = new LinkedList<>();

        for (ProtocolProviderFactory providerFactory : UtilActivator
            .getProtocolProviderFactories().values())
        {
            ServiceReference<?> serRef;
            ProtocolProviderService protocolProvider;

            for (AccountID accountID : providerFactory.getRegisteredAccounts())
            {
                serRef = providerFactory.getProviderForAccount(accountID);

                protocolProvider
                    = (ProtocolProviderService) UtilActivator.bundleContext
                        .getService(serRef);

                registeredProviders.add(protocolProvider);
            }
        }
        return registeredProviders;
    }

    /**
     * Returns of supported/enabled list of audio formats for a provider.
     *
     * @param device the device to check against the provider for compatibility
     * @param protocolProvider the provider to check.
     * @param encodingConfiguration the Encoding Configuration Service
     * @return list of supported/enabled auido formats or empty list
     * otherwise.
     */
    public static List<MediaFormat> getAudioFormats(
                                    MediaDevice device,
                                    ProtocolProviderService protocolProvider,
                                    EncodingConfiguration encodingConfiguration)
    {
        List<MediaFormat> res = new ArrayList<>();

        List<MediaFormat> formats = device.getSupportedFormats();

        // skip the special telephony event.
        for(MediaFormat format : formats)
        {
            if(!format.getEncoding().equals(Constants.TELEPHONE_EVENT))
                res.add(format);
        }

        return res;
    }

    /**
     * Gets the display name of the MetaContact with the given chat address
     *
     * @param chatAddress The chat address
     * @return The display name of the MetaContact with the given chat address
     */
    public static String getDisplayNameFromChatAddress(String chatAddress)
    {
        String displayName = chatAddress;

        MetaContactListService contactListService =
                                         UtilActivator.getContactListService();

        ProtocolProviderService imProvider = getImProvider();
        String accountId = (imProvider != null) ?
            imProvider.getAccountID().getAccountUniqueID() : "unknown";

        MetaContact metaContact =
           contactListService.findMetaContactByContact(chatAddress, accountId);

        if (metaContact != null)
        {
            displayName = metaContact.getDisplayName();
        }

        return displayName;
    }

    public static void enableAccount(AccountID account)
    {
        if (account == null)
        {
            logger.warn("Cannot activate a null account.");
        }
        else if (account.isEnabled())
        {
            logger.warn("Account is already enabled.");
        }
        else
        {
            AccountManager accountManager
                    = ServiceUtils.getService(UtilActivator.bundleContext,
                                              AccountManager.class);

            if (accountManager != null)
            {
                accountManager.toggleAccountEnabled(account);
            }
            else
            {
                logger.warn("Could not enable account.");
            }
        }
    }
}
