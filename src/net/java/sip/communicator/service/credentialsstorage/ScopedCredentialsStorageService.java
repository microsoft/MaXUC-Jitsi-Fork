/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.credentialsstorage;

/**
 * Loads and saves user credentials from/to the persistent storage
 * (configuration file in the default implementation).
 *
 * @author Dmitri Melnikov
 */
public interface ScopedCredentialsStorageService
{
    /**
     * The name of a property which represents an encrypted password.
     */
    String ACCOUNT_ENCRYPTED_PASSWORD = "ENCRYPTED_PASSWORD";

    /**
     * Store the password for the account that starts with the given prefix.
     *
     * @param accountPrefix account prefix
     * @param password the password to store
     * @return <tt>true</tt> if the specified <tt>password</tt> was successfully
     * stored; otherwise, <tt>false</tt>
     */
    boolean storePassword(String accountPrefix, String password);

    /**
     * Load the password for the account that starts with the given prefix.
     *
     * @param accountPrefix account prefix
     * @return the loaded password for the <tt>accountPrefix</tt>
     */
    String loadPassword(String accountPrefix);

    /**
     * Remove the password for the account that starts with the given prefix.
     *
     * @param accountPrefix account prefix
     * @return <tt>true</tt> if the password for the specified
     * <tt>accountPrefix</tt> was successfully removed; otherwise,
     * <tt>false</tt>
     */
    boolean removePassword(String accountPrefix);

    /**
     * Checks if the account password that starts with the given prefix is saved
     * in encrypted form.
     *
     * @param accountPrefix account prefix
     * @return <tt>true</tt> if saved, <tt>false</tt> if not
     */
    boolean isStoredEncrypted(String accountPrefix);

    /**
     * Trigger a rotation of the master password.  Should be done when the user is
     * changing their password as that may indicate a credentials leak.
     */
    void rotateMasterPassword();

    /**
     * Trigger copying a master password from one key (if one exists under it) to a second key
     * (blatting over any password already stored under that key).
     */
    void copyMasterPassword(String from, String to);
}
