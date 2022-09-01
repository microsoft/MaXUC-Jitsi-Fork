/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.credentialsstorage;

/**
 * Loads and saves user credentials from/to the persistent storage
 * (configuration file in the default implementation).
 *
 * @author Dmitri Melnikov
 */
public interface CredentialsStorageService
{
    /**
     * Global credentials storage.
     */
    ScopedCredentialsStorageService global();

    /**
     * User credentials storage.
     */
    ScopedCredentialsStorageService user();

    /**
     * Set user credentials.
     */
    void setActiveUser();

    /**
     * Stores the password locally in a map.
     * @param propertyName The password property name
     * @param password The password being stored
     */
    void storePasswordLocally(String propertyName, String password);
}
