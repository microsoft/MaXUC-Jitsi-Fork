/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.addressbook;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Address Book implementation of the account id.
 */
public class AddressBookAccountID
    extends AccountID
{
    /**
     * Creates an account id for the specified provider userid and
     * accountProperties.
     * If account uid exists in account properties, we are loading the account
     * and so load its value from there, prevent changing account uid
     * when server changed (serviceName has changed).
     *
     * @param userID            a String that uniquely identifies the user.
     * @param accountProperties a Map containing any other protocol and
     *                          implementation specific account initialization
     *                          properties
     */
    public AddressBookAccountID(String userID,
                                Map<String, String> accountProperties)
    {
        super(userID,
              accountProperties,
              ProtocolNames.ADDRESS_BOOK,
              getServiceName());
    }

    /**
     * Use package name as service name.
     * @return the service name for this account.
     */
    private static String getServiceName()
    {
        return "AddressBookService";
    }
}
