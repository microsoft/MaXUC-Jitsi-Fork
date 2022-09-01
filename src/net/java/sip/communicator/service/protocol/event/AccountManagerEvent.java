/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Represents a notifying event fired by a specific {@link AccountManager}.
 *
 * @author Lubomir Marinov
 */
public class AccountManagerEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The type of event notifying that the loading of the stored accounts of a
     * specific <code>ProtocolProviderFactory</code> has finished.
     */
    public static final int STORED_ACCOUNTS_LOADED = 1;

    /**
     * Type of event indicating that a single account has been removed
     */
    public static final int ACCOUNT_REMOVED = 2;

    /**
     * Type of event indicating that a single account has been loaded
     */
    public static final int ACCOUNT_LOADED = 3;

    /**
     * The <code>ProtocolProviderFactory</code> being worked on at the time this
     * event has been fired.
     */
    private final ProtocolProviderFactory factory;

    /**
     * The <code>AccountID</code> of the account that has been changed.  May be
     * null if this event refers to all accounts
     */
    private final AccountID accountID;

    /**
     * The (detail) type of this event which is one of
     * {@link #STORED_ACCOUNTS_LOADED}.
     */
    private final int type;

    /**
     * Initializes a new <code>AccountManagerEvent</code> instance fired by a
     * specific <code>AccountManager</code> in order to notify of an event of a
     * specific type occurring while working on a specific
     * <code>ProtocolProviderFactory</code>.
     *
     * @param accountManager the <code>AccountManager</code> issuing the
     *            notification i.e. the source of the event
     * @param type the type of the event which is one of
     * {@link #STORED_ACCOUNTS_LOADED}
     * @param factory the <code>ProtocolProviderFactory</code> being worked on
     *            at the time this event has been fired
     * @param accountID the account that has been loaded / unloaded
     */
    public AccountManagerEvent(AccountManager accountManager,
                               int type,
                               ProtocolProviderFactory factory,
                               AccountID accountID)
    {
        super(accountManager);

        this.type = type;
        this.factory = factory;
        this.accountID = accountID;
    }

    /**
     * Gets the <code>ProtocolProviderFactory</code> being worked on at the time
     * this event has been fired.
     *
     * @return the <code>ProtocolProviderFactory</code> being worked on at the
     *         time this event has been fired
     */
    public ProtocolProviderFactory getFactory()
    {
        return factory;
    }

    /**
     * Gets the (detail) type of this event which is one of
     * <code>STORED_ACCOUNTS_LOADED</code>.
     *
     * @return the (detail) type of this event which is one of
     *         <code>STORED_ACCOUNTS_LOADED</code>
     */
    public int getType()
    {
        return type;
    }

    /**
     * @return The <code>AccountID</code> of the account that has been changed.
     *         May be null if this event refers to all accounts.
     */
    public AccountID getAccountID()
    {
        return accountID;
    }

    @Override
    public String toString()
    {
        return "AccountManagerEvent@" + Integer.toHexString(hashCode()) + ";" +
               "type:" + type +
               ";account:" + accountID;
    }
}
