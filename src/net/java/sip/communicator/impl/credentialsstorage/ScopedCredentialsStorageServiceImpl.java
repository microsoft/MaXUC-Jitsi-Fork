/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.credentialsstorage;

import static net.java.sip.communicator.util.PrivacyUtils.sanitiseDirectoryNumberWithAccPrefix;
import static org.jitsi.util.Hasher.logHasher;

import java.security.SecureRandom;
import java.util.*;

import com.microsoft.credentialstorage.SecretStore;
import com.microsoft.credentialstorage.StorageProvider;
import com.microsoft.credentialstorage.StorageProvider.SecureOption;
import com.microsoft.credentialstorage.model.StoredToken;
import com.microsoft.credentialstorage.model.StoredTokenType;
import org.jitsi.impl.configuration.ConfigurationServiceImpl;
import org.jitsi.service.configuration.*;

import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Base64;

/**
 * Implements {@link CredentialsStorageService} to load and store user
 * credentials from/to the {@link ConfigurationService}.
 *
 * @author Dmitri Melnikov
 */
public class ScopedCredentialsStorageServiceImpl
    implements ScopedCredentialsStorageService
{
    /**
     * The <tt>Logger</tt> used by this <tt>CredentialsStorageServiceImpl</tt>
     * for logging output.
     */
    private static final Logger sLog
        = Logger.getLogger(ScopedCredentialsStorageServiceImpl.class);

    /**
     * The name of a property which represents an unencrypted password.
     */
    public static final String ACCOUNT_UNENCRYPTED_PASSWORD = "PASSWORD";

    /**
     * The name of a property which represents an encrypted password.
     */
    public static final String ACCOUNT_ENCRYPTED_PASSWORD = "ENCRYPTED_PASSWORD";

    /** Token used to hold the master password in the OS secure storage. */
    private static final String OS_ACCESS_TOKEN_STEM = "MaX UC token for ";

    /** Characters used to generate a random char sequence. */
    private static final char[] ENGLISH_MIXED_CASE_LETTER_CHARACTERS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

   /** The configuration service. */
    private final ScopedConfigurationService mConfigurationService;

    /** A {@link Crypto} instance that does the actual encryption and decryption. */
    private Crypto mCrypto;

    /** The secure OS storage */
    private static SecretStore<StoredToken> sTokenStorage;

    /**
     * Initializes the credentials service by fetching the configuration service
     * reference from the bundle context. Encrypts and moves all passwords to
     * new properties.
     *
     * @param configurationService scoped configuration service
     */
    ScopedCredentialsStorageServiceImpl(ScopedConfigurationService configurationService)
    {
        this.mConfigurationService = configurationService;

        synchronized (ScopedCredentialsStorageServiceImpl.class)
        {
            if (sTokenStorage == null)
            {
                sTokenStorage = StorageProvider.getTokenStorage(true, SecureOption.REQUIRED);
            }
        }
    }

    /**
     * Stores the password for the specified account. When password is
     * null the property is cleared.
     *
     * Many threads can call this method at the same time, and the
     * first thread may present the user with the master password prompt and
     * create a <tt>Crypto</tt> instance based on the input
     * (<tt>createCrypto</tt> method). This instance will be used later by all
     * other threads.
     *
     * @param accountPrefix account prefix
     * @param password the password to store
     * @return <tt>true</tt> if the specified <tt>password</tt> was successfully
     * stored; otherwise, <tt>false</tt>
     * @see ScopedCredentialsStorageServiceImpl#storePassword(String, String)
     */
    public synchronized boolean storePassword(
            String accountPrefix,
            String password)
    {
        sLog.info("Store password for " +
                  sanitiseDirectoryNumberWithAccPrefix(accountPrefix));

        if (createCryptoInstance())
        {
            String encryptedPassword = null;
            try
            {
                if (password != null)
                {
                    // Spice with the property prefix
                    encryptedPassword = mCrypto.encrypt(accountPrefix + password);
                }
                setEncrypted(accountPrefix, encryptedPassword);
                return true;
            }
            catch (Exception ex)
            {
                sLog.error("Encryption failed, password not saved", ex);
                return false;
            }
        }

        return false;
    }

    /**
     * Loads the password for the specified account. If the password is stored
     * encrypted, decrypts it with the master password.
     *
     * Many threads can call this method at the same time, and the first thread
     * may present the user with the master password prompt and create a
     * <tt>Crypto</tt> instance based on the input (<tt>createCrypto</tt>
     * method). This instance will be used later by all other threads.
     *
     * @param accountPrefix account prefix
     * @return the loaded password for the <tt>accountPrefix</tt>
     * @see ScopedCredentialsStorageServiceImpl#createCryptoInstance()
     */
    public synchronized String loadPassword(String accountPrefix)
    {
        // Redact phone number from accountPrefix
        sLog.debug("Load password for " +
                   sanitiseDirectoryNumberWithAccPrefix(accountPrefix));
        String password = null;
        if (isStoredEncrypted(accountPrefix) && createCryptoInstance())
        {
            try
            {
                String decrypted = mCrypto.decrypt(getEncrypted(accountPrefix));
                // Strip the spice from the decrypted value
                password = decrypted.substring(accountPrefix.length());
            }
            catch (Exception ex)
            {
                sLog.error("Decryption with master password failed", ex);
                // password stays null
            }
        }

        return password;
    }

    /**
     * Removes the password for the account that starts with the given prefix by
     * setting its value in the configuration to null.
     *
     * @param accountPrefix account prefix
     * @return <tt>true</tt> if the password for the specified
     * <tt>accountPrefix</tt> was successfully removed; otherwise,
     * <tt>false</tt>
     */
    public boolean removePassword(String accountPrefix)
    {
        setEncrypted(accountPrefix, null);
        sLog.info("Password for '" +
                  sanitiseDirectoryNumberWithAccPrefix(accountPrefix) +
                  "' removed");
        return true;
    }

    /**
     * Changes the old master password to the new one.
     * For all saved account passwords it decrypts them with the old MP and then
     * encrypts them with the new MP.
     * If the old password was null, then we are migrating to use a proper master
     * password (upgrade to V3.10), so we also add spice.
     * If just changing the master password, there is already spice.
     * Spice is a bit like salt for a hash, but used for two-way encryption (
     * Note, this isn't a standard term).
     *
     * @param oldPassword the old master password
     * @param newPassword the new master password
     * @return <tt>true</tt> if master password was changed successfully;
     * <tt>false</tt>, otherwise
     */
    private boolean changeMasterPassword(char[] oldPassword, char[] newPassword)
    {
        sLog.info("Change master password");

        // get all encrypted account password properties
        List<String> encryptedAccountProps =
            mConfigurationService.getPropertyNamesBySuffix(ACCOUNT_ENCRYPTED_PASSWORD);

        // this map stores propName -> password
        Map<String, String> passwords = new HashMap<>();
        try
        {
            // read from the config and decrypt with the old MP...
            // if the old password was null (means we are migrating to 3.10),
            // default master password will be used to decrypt properties.
            setMasterPassword(oldPassword);
            for (String propName : encryptedAccountProps)
            {
                String propValue = mConfigurationService.getString(propName);
                if (propValue != null)
                {
                    String decrypted = mCrypto.decrypt(propValue);
                    passwords.put(propName, decrypted);
                }
            }

            // ...and encrypt again with the new, write to the config
            setMasterPassword(newPassword);
            for (Map.Entry<String, String> entry : passwords.entrySet())
            {
                String prefix = "";
                if (oldPassword == null)
                {
                    // There was no spice when using the default master password, so add
                    // some using the prefix name (i.e. without .ENCRYPTED_PASSWORD)
                    String propertyName = entry.getKey();
                    prefix = propertyName.substring(0, propertyName.lastIndexOf("."));
                }
                String encrypted = mCrypto.encrypt(prefix + entry.getValue());
                mConfigurationService.setProperty(entry.getKey(), encrypted);
            }
        }
        catch (CryptoException ce)
        {
            sLog.warn(ce);
            mCrypto = null;
            passwords = null;
            return false;
        }
        return true;
    }

    /**
     * Sets the master password to the argument value.
     *
     * @param master master password
     */
    private void setMasterPassword(char[] master)
    {
        sLog.info("Set master password");
        mCrypto = new AESCrypto(master);
    }

    /**
     * Moves all password properties from unencrypted
     * {@link #ACCOUNT_UNENCRYPTED_PASSWORD} to the corresponding encrypted
     * {@link #ACCOUNT_ENCRYPTED_PASSWORD}.
     */
    private void encryptAllPasswordProperties()
    {
        sLog.debug("Encrypt all passwords");
        List<String> unencryptedProperties
            = mConfigurationService.getPropertyNamesBySuffix(
                    ACCOUNT_UNENCRYPTED_PASSWORD);

        sLog.debug(unencryptedProperties.size() + " pwds to move");
        for (String prop : unencryptedProperties)
        {
            int idx = prop.lastIndexOf('.');

            if (idx != -1)
            {
                String prefix = prop.substring(0, idx);
                String encodedPassword = getUnencrypted(prefix);

                // If the password is stored unencrypted, we have to migrate it,
                // of course. But if it is also stored encrypted in addition to
                // being stored unencrypted, the situation starts to look
                // unclear and it may be better to just not migrate it.
                if ((encodedPassword == null)
                        || (encodedPassword.length() == 0)
                        || isStoredEncrypted(prefix))
                {
                    // Suspected dead code path!
                    setUnencrypted(prefix, null);
                }
                else if (!encryptPasswordProperty(
                        prefix,
                        new String(Base64.decode(encodedPassword))))
                {
                    sLog.warn("Failed to move password for prefix " +
                              sanitiseDirectoryNumberWithAccPrefix(prefix));
                }
            }
        }
    }

    /**
     * Asks for master password if needed, encrypts the password, saves it to
     * the new property and removes the old property.
     *
     * @param accountPrefix prefix of the account
     * @param password unencrypted password
     * @return <tt>true</tt> if the specified <tt>password</tt> was successfully
     * moved; otherwise, <tt>false</tt>
     */
    private boolean encryptPasswordProperty(String accountPrefix, String password)
    {
        sLog.info("Encrypt password for " +
                  sanitiseDirectoryNumberWithAccPrefix(accountPrefix));
        if (createCryptoInstance())
        {
            try
            {
                setEncrypted(accountPrefix, mCrypto.encrypt(accountPrefix + password));
                setUnencrypted(accountPrefix, null);
                return true;
            }
            catch (CryptoException cex)
            {
                sLog.debug("Encryption failed", cex);
            }
        }
        // properties are not moved
        return false;
    }

    /**
     * Creates a Crypto instance only when it's null, either with a user input
     * master password or with null. If the user decided not to input anything,
     * the instance is not created.
     *
     * @return <tt>true</tt> if the Crypto instance was created; <tt>false</tt>,
     * otherwise
     */
    private boolean createCryptoInstance()
    {
        // Statically synchronized to prevent multiple instances (global and user), both
        // trying to set the single master password stored with the OS.
        synchronized (ScopedCredentialsStorageServiceImpl.class)
        {
            if (mCrypto == null)
            {
                sLog.info("Create new Crypto instance");
                char[] masterPassword = getMasterPasswordFromOS();

                if (masterPassword == null)
                {
                    rotateMasterPassword();
                }
                else
                {
                    try
                    {
                        // We now have a master password, so we set the crypto instance to use it.
                        setMasterPassword(masterPassword);

                        // This just encrypts any passwords that weren't yet encrypted.
                        encryptAllPasswordProperties();
                    }
                    finally
                    {
                        // Clear the password value to protect against inspection of process memory.
                        Arrays.fill(masterPassword, (char) 0x00);
                    }
                }
            }

            return (mCrypto != null);
        }
    }

    @Override
    public void rotateMasterPassword()
    {
        sLog.info("Rotate the master password");

        // Statically synchronized to prevent multiple instances (global and user), both
        // trying to set the single master password stored with the OS.
        synchronized (ScopedCredentialsStorageServiceImpl.class)
        {
            sLog.info("synchronized - create new Crypto instance");
            char[] oldMasterPassword = getMasterPasswordFromOS();

            if (oldMasterPassword == null)
            {
                sLog.info("There was no master password stored with the OS");
            }

            // Generate a new master password at random and save it for next time.
            // 64 mixed case ascii chars is ~300bits, so should be enough!
            char[] newMasterPassword = randomLetterArray(64);

            try
            {
                storeMasterPasswordInOS(newMasterPassword);

                // We can now migrate all password properties from the old default
                // master password to the new secure master password.
                changeMasterPassword(oldMasterPassword, newMasterPassword);

                // We now have a master password, so we set the crypto instance to use it.
                setMasterPassword(newMasterPassword);

                // This just encrypts any passwords that weren't yet encrypted.
                encryptAllPasswordProperties();
            }
            finally
            {
                // Clear the password values to protect against inspection of process memory.
                if (oldMasterPassword != null)
                {
                    Arrays.fill(oldMasterPassword, (char) 0x00);
                }
                Arrays.fill(newMasterPassword, (char) 0x00);
            }
        }
    }

    /**
     * Simple helper function that must be called inside a statically synchronised block
     * as per the body of createCrypto.
     */
    private char[] getMasterPasswordFromOS()
    {
        // Retrieve an existing token from the store if there
        String tokenName = OS_ACCESS_TOKEN_STEM +
            mConfigurationService.getGlobalConfigurationService().getProperty(
                ConfigurationServiceImpl.PROPERTY_ACTIVE_USER);
        StoredToken storedToken = sTokenStorage.get(tokenName);
        sLog.info("Retrieved token under " + logHasher(tokenName) + ": " + (storedToken != null));
        return storedToken != null ? storedToken.getValue() : null;
    }

    /**
     * Simple helper function that must be called inside a statically synchronised block
     * as per the body of createCrypto.
     */
    private void storeMasterPasswordInOS(char[] masterPassword)
    {
        StoredToken token = new StoredToken(masterPassword, StoredTokenType.ACCESS);

        try
        {
            // Save the token to the store
            String tokenName = OS_ACCESS_TOKEN_STEM +
                               mConfigurationService.getGlobalConfigurationService().getProperty(
                                       ConfigurationServiceImpl.PROPERTY_ACTIVE_USER);
            sTokenStorage.add(tokenName, token);
            sLog.info("Added/Updated token to OS Credential Manager under the key: " + logHasher(tokenName));
        }
        finally
        {
            // Clear the token value to protect against inspection of process memory.
            token.clear();
        }
    }

    /**
     * Retrieves the encrypted account password using configuration service.
     *
     * @param accountPrefix account prefix
     * @return the encrypted account password.
     */
    private String getEncrypted(String accountPrefix)
    {
        return mConfigurationService.getString(accountPrefix + "." + ACCOUNT_ENCRYPTED_PASSWORD);
    }

    /**
     * Saves the encrypted account password using configuration service.
     *
     * @param accountPrefix account prefix
     * @param encryptedValue the encrypted account password.
     */
    private void setEncrypted(String accountPrefix, String encryptedValue)
    {
        mConfigurationService.setProperty(
                accountPrefix + "." + ACCOUNT_ENCRYPTED_PASSWORD,
                encryptedValue);
    }

    /**
     * Check if encrypted account password is saved in the configuration.
     *
     * @param accountPrefix account prefix
     * @return <tt>true</tt> if saved, <tt>false</tt> if not
     */
    public boolean isStoredEncrypted(String accountPrefix)
    {
        return mConfigurationService.getString(
                    accountPrefix + "." + ACCOUNT_ENCRYPTED_PASSWORD)
                != null;
    }

    /**
     * Retrieves the unencrypted account password using configuration service.
     *
     * @param accountPrefix account prefix
     * @return the unencrypted account password
     */
    private String getUnencrypted(String accountPrefix)
    {
        return mConfigurationService.getString(
                    accountPrefix + "." + ACCOUNT_UNENCRYPTED_PASSWORD);
    }

    /**
     * Saves the unencrypted account password using configuration service.
     *
     * @param accountPrefix account prefix
     * @param value the unencrypted account password
     */
    private void setUnencrypted(String accountPrefix, String value)
    {
        mConfigurationService.setProperty(
                accountPrefix + "." + ACCOUNT_UNENCRYPTED_PASSWORD, value);
    }

    /**
     * Generates a char array of the specified size
     * using random English mixed-case letters.
     *
     * @param size of the result array
     * @return random char array
     */
    private char[] randomLetterArray(int size)
    {
        char[] result = new char[size];

        Random random = new SecureRandom();
        int range = ENGLISH_MIXED_CASE_LETTER_CHARACTERS.length;

        for (int i = 0; i < size; i++)
        {
            result[i] = ENGLISH_MIXED_CASE_LETTER_CHARACTERS[random.nextInt(range)];
        }

        return result;
    }
}
