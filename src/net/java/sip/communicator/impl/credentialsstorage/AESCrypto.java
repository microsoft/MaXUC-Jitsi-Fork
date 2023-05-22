/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.credentialsstorage;

import java.security.*;
import java.security.spec.*;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.*;
import javax.crypto.spec.*;

import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.util.*;

/**
 * Performs encryption and decryption of text using AES algorithm.
 *
 * @author Dmitri Melnikov
 */
public class AESCrypto
    implements Crypto
{
    private static final Logger sLog = Logger.getLogger(AESCrypto.class);

    /**
     * Salt used when creating the key.
     */
    private static final byte[] SALT =
    { 0x0C, 0x0A, 0x0F, 0x0E, 0x0B, 0x0E, 0x0E, 0x0F };

    /**
     * Possible length of the keys in bits.
     */
    private static final int[] KEY_LENGTHS = new int[]{256, 128};

    /**
     * Number of iterations to use when creating the key.
     */
    private static final int ITERATION_COUNT = 1024;

    /**
     * Default master password.
     */
    private static final char[] DEFAULT_MASTER_PASSWORD = {' '}; // lgtm[java/hardcoded-credential-api-call] Required only for migration from old version.

    /**
     * A list of keys of all supported lengths derived from the master password
     * to use for encryption/decryption.
     */
    private final List<AESEncryptionKey> keys = new ArrayList<>();

    /**
     * Creates the encryption and decryption objects and the key.
     *
     * @param masterPassword used to derive the key. Can be null.
     */
    public AESCrypto(char[] masterPassword)
    {
        try
        {
            // We try to initialise keys with all supplied lengths and store
            // all those that succeed.  Over upgrade we may begin supporting
            // new lengths that were previously unsupported and we always want
            // to encrypt using the highest level of encryption available.
            // However, we need keys at all valid lower levels of encryption so
            // that we can decrypt strings that were encrypted with the
            // previously supported lengths by the previous client version.
            for (int i = 0; i < KEY_LENGTHS.length; i++)
            {
                int keyLength = KEY_LENGTHS[i];

                try
                {
                    sLog.debug("Attempting to initialise key with length " + keyLength);
                    keys.add(initKey(masterPassword, keyLength));
                }
                catch (InvalidKeyException e)
                {
                    sLog.debug("Unsupported key length found " + keyLength);

                    // We expect that we may hit invalid key exceptions for
                    // some key lengths, so we only care if we have tried all
                    // key lengths and generated no valid keys.
                    if((i == (KEY_LENGTHS.length - 1)) && keys.isEmpty())
                    {
                        throw e;
                    }
                }
            }
        }
        catch (InvalidKeyException e)
        {
            sLog.error("No valid keys found", e);
            throw new RuntimeException("No valid keys found", e);
        }
        catch (InvalidKeySpecException e)
        {
            sLog.error("Invalid key specification", e);
            throw new RuntimeException("Invalid key specification", e);
        }
        catch (NoSuchAlgorithmException e)
        {
            sLog.error("Algorithm not found", e);
            throw new RuntimeException("Algorithm not found", e);
        }
        catch (NoSuchPaddingException e)
        {
            sLog.error("Padding not found", e);
            throw new RuntimeException("Padding not found", e);
        }
    }

    /**
     * Initialize key with specified length.
     *
     * @param masterPassword used to derive the key. Can be null.
     * @param keyLength Length of the key in bits.
     * @throws InvalidKeyException if the key is invalid (bad encoding,
     * wrong length, uninitialized, etc).
     * @throws NoSuchAlgorithmException if the algorithm chosen does not exist
     * @throws InvalidKeySpecException if the key specifications are invalid
     * @throws NoSuchPaddingException if the padding is incorrect
     *
     * @return the key
     */
    private AESEncryptionKey initKey(char[] masterPassword, int keyLength)
        throws  InvalidKeyException,
                NoSuchAlgorithmException,
                InvalidKeySpecException,
                NoSuchPaddingException
    {
        // if the password is empty, we get an exception constructing the key
        if (masterPassword == null)
        {
            // here a default password can be set,
            // cannot be an empty string
            masterPassword = DEFAULT_MASTER_PASSWORD;
        }

        // Password-Based Key Derivation Function found in PKCS5 v2.0.
        // This is only available with java 6.
        SecretKeyFactory factory =
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        // Make a key from the master password
        KeySpec spec =
            new PBEKeySpec(masterPassword, SALT,
                ITERATION_COUNT, keyLength);
        SecretKey tmp = factory.generateSecret(spec);
        // Make an algorithm specific key
        AESEncryptionKey key = new AESEncryptionKey(tmp.getEncoded(), keyLength);

        // just a check whether the key size is wrong
        key.getDecryptCipher().init(Cipher.DECRYPT_MODE, key);
        key.getEncryptCipher().init(Cipher.ENCRYPT_MODE, key);

        return key;
    }

    /**
     * Tries to decrypt the ciphertext using the each of the valid keys we
     * have in turn.  As soon as 1 key works, the decrypted string is returned.
     * If none of the keys work, a CryptoException is thrown.
     *
     * @param ciphertext base64 encoded encrypted data
     * @return decrypted data
     * @throws CryptoException when the ciphertext cannot be decrypted with the
     *             key or on decryption error.
     */
    public String decrypt(String ciphertext) throws CryptoException
    {
        CryptoException exception = null;
        String decrypted = null;

        for (AESEncryptionKey key : keys)
        {
            try
            {
                sLog.debug("Attempting to decrypt with " + key);
                Cipher decryptCipher = key.getDecryptCipher();
                decryptCipher.init(Cipher.DECRYPT_MODE, key);
                decrypted = new String(decryptCipher.doFinal(Base64.decode(ciphertext)),
                                       "UTF-8");
                break;
            }
            catch (BadPaddingException e)
            {
                sLog.debug("Failed to decrypt with key " + key, e);
                exception = new CryptoException(CryptoException.WRONG_KEY, e);
            }
            catch (Exception e)
            {
                sLog.debug("Failed to decrypt with key " + key, e);
                exception = new CryptoException(CryptoException.DECRYPTION_ERROR, e);
            }
        }

        if (decrypted == null)
        {
            sLog.error("Decryption failed with all keys", exception);
            throw exception;
        }

        return decrypted;
    }

    /**
     * Tries to encrypt the plaintext using the each of the valid keys we
     * have in turn.  As soon as 1 key works, the encrypted string is returned.
     * If none of the keys work, a CryptoException is thrown.
     *
     * @param plaintext data to be encrypted
     * @return base64 encoded encrypted data
     * @throws CryptoException on encryption error
     */
    public String encrypt(String plaintext) throws CryptoException
    {
        CryptoException exception = null;
        String encrypted = null;

        for (AESEncryptionKey key : keys)
        {
            try
            {
                sLog.debug("Attempting to encrypt with " + key);
                Cipher encryptCipher = key.getEncryptCipher();
                encryptCipher.init(Cipher.ENCRYPT_MODE, key);
                encrypted = new String(Base64.encode(encryptCipher.doFinal(plaintext
                    .getBytes("UTF-8"))));
                break;
            }
            catch (Exception e)
            {
                sLog.debug("Failed to encrypt with key " + key, e);
                exception = new CryptoException(CryptoException.ENCRYPTION_ERROR, e);
            }
        }

        if (encrypted == null)
        {
            sLog.error("Encryption failed with all keys", exception);
            throw exception;
        }

        return encrypted;
    }

    /**
     * Class to represent an AES Encryption key.
     */
    private class AESEncryptionKey extends SecretKeySpec
    {
        private static final long serialVersionUID = 1L;

        /**
         * AES in ECB mode with padding.
         */
        private static final String CIPHER_ALGORITHM = "AES/ECB/PKCS5PADDING";

        /**
         * The algorithm associated with the key.
         */
        private static final String KEY_ALGORITHM = "AES";

        /**
         * Decryption object.
         */
        private final Cipher mDecryptCipher;

        /**
         * Encryption object.
         */
        private final Cipher mEncryptCipher;

        /**
         * The length of the key.
         */
        private final int mKeyLength;

        /**
         * Creates a new instance of an AES Encryption Key.
         *
         * @param paramArrayOfByte
         * @param keyLength
         * @throws NoSuchAlgorithmException
         * @throws NoSuchPaddingException
         */
        public AESEncryptionKey(byte[] paramArrayOfByte, int keyLength)
            throws NoSuchAlgorithmException, NoSuchPaddingException
        {
            super(paramArrayOfByte, KEY_ALGORITHM);

            mKeyLength = keyLength;
            mDecryptCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            mEncryptCipher = Cipher.getInstance(CIPHER_ALGORITHM);
        }

        /**
         * @return the cipher used for decryption
         */
        public Cipher getDecryptCipher()
        {
            return mDecryptCipher;
        }

        /**
         * @return the cipher used for encryption
         */
        public Cipher getEncryptCipher()
        {
            return mEncryptCipher;
        }

        @Override
        public String toString()
        {
            // Return useful information to help with debugging
            return "<Key length " + mKeyLength + ", instance ID " + hashCode() + ">";
        }
    }
}
