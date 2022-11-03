// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.certificate;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import net.java.sip.communicator.launcher.SIPCommunicator;
import net.java.sip.communicator.util.Logger;

/**
 * This class handles generation and retrieval of TLS certificates for use on
 * the WISPA interface.
 *
 * We use a WSS (WebSocket Secure) connection for WISPA traffic.  This is for
 * privacy reasons, relating to the use-case where there are multiple users
 * signed-in to a single machine.  Specifically:
 *    1)  We want to encrypt traffic on the WISPA interface, so that one user
 *        can't use Wireshark to snoop on the WISPA traffic of another user.
 *    2)  We want to prevent one user's Electron UI from connecting to another
 *        user's Java backend.
 *
 * We use self-signed certificates for both WISPA client and WISPA server,
 * allowing both to ensure that they have connected to processes running within
 * the same user's context.
 *
 * On start-up, the Java process will create private/public key pairs and
 * self-signed certificates, for the client and server.  These are written into
 * the application's home directory, in the formats required by client and
 * server.  Specifically:
 *  - The client needs its certificate and private key in PEM format (for the
 *    JavaScript socket.io-client library).  It also needs the server
 *    certificate in PEM format, to know which server to trust.
 *  - The server needs a PFX key store containing its own certificate and
 *    private key.  It also needs a "trust store", which is just another PFX
 *    key store, containing the certificates to trust (i.e. just the client's
 *    certificate).
 *
 * All these certificates and keys are currently just written to local disk.
 * This is sufficient to prevent one user from accessing the keys required to
 * snoop on or access another user's personal data, as one user cannot access
 * another user's files, unless they have machine administrator permissions.
 */
public class WISPACertificateInfo
{
    private static final Logger logger = Logger.getLogger(WISPACertificateInfo.class);

    // Constants used in certificate generation.
    private static final String SERVER_ALIAS = "server";
    private static final String CLIENT_ALIAS = "client";
    private static final String RSA_ALGORITHM = "RSA";
    private static final int RSA_KEYSIZE = 2048;
    private static final String SHA_256_WITH_RSA = "SHA256WithRSA";
    private static final String KEYSTORE_PKCS12 = "PKCS12";

    // Details of the server keystores, and the passwords required to access them.
    private final String serverPassphrase;
    private final String keyStoreFile;
    private final String trustStoreFile;
    private final String storeFormat = KEYSTORE_PKCS12;

    /**
     * Constructor
     *
     * Generates a new set of client and server certificates.  This is done at
     * runtime so that if multiple different users run the application on the
     * same machine at the same time, there is no way for the Electron UI of one
     * user to connect to the Java backend of another user.
     *
     * If we hit any failures during the generation and saving of these keys
     * and certificates, there is nothing we can do to continue.  We just log
     * the error and throw an exception.
     */
    public WISPACertificateInfo()
    {
        // Read the system properties relating to the WISPA keys/certificates.
        // These will have been set by the launcher, on startup.
        String wispaDir = System.getProperty(SIPCommunicator.WISPA_KEYS_DIR_PROPERTY);
        keyStoreFile = System.getProperty(SIPCommunicator.WISPA_SERVER_KEY_STORE_PROPERTY);
        trustStoreFile = System.getProperty(SIPCommunicator.WISPA_SERVER_TRUST_STORE_PROPERTY);
        String clientKeyStoreFile = System.getProperty(SIPCommunicator.WISPA_CLIENT_KEY_STORE_PROPERTY);
        String serverCertFile = System.getProperty(SIPCommunicator.WISPA_SERVER_CERTIFICATE_PROPERTY);
        String clientPassphrase = System.getProperty(SIPCommunicator.WISPA_CLIENT_PASSPHRASE_PROPERTY);

        // Generate one-time random passwords for the server key store and trust store.
        serverPassphrase = UUID.randomUUID().toString();

        try
        {
            // Ensure that the WISPA directory exists
            Files.createDirectories(Paths.get(wispaDir));
        }
        catch (IOException e)
        {
            // Can't continue without WISPA directory
            String msg = "Failed to create WISPA certificate directory: " + wispaDir;
            logger.error(msg, e);
            throw new WISPACertificateException(msg, e);
        }

        // Get hold of a key pair generator.
        KeyPairGenerator kpGen;
        try
        {
            kpGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            kpGen.initialize(RSA_KEYSIZE);
        }
        catch (NoSuchAlgorithmException e)
        {
            // Should be impossible to hit - RSA algorithm definitely exists.
            String msg = "Failed to create key pair generator";
            logger.error(msg, e);
            throw new WISPACertificateException(msg, e);
        }

        // Generate a client and server keypair
        KeyPair clientKeyPair = kpGen.generateKeyPair();
        KeyPair serverKeyPair = kpGen.generateKeyPair();

        // Create a server certificate
        X509Certificate serverCert;
        try
        {
            serverCert = generateCertificate(SERVER_ALIAS, serverKeyPair);
            writePEMToFile(serverCert, serverCertFile);
        }
        catch (OperatorCreationException | IOException | CertificateException | NoSuchAlgorithmException e)
        {
            String msg = "Failed to generate WISPA server certificate";
            logger.error(msg, e);
            throw new WISPACertificateException(msg, e);
        }

        // Create a client certificate
        X509Certificate clientCert;
        try
        {
            clientCert = generateCertificate(CLIENT_ALIAS, clientKeyPair);
        }
        catch (OperatorCreationException | IOException | CertificateException | NoSuchAlgorithmException e)
        {
            String msg = "Failed to generate WISPA client certificate";
            logger.error(msg, e);
            throw new WISPACertificateException(msg, e);
        }

        try
        {
            // Create the client key store
            createKeyStore(clientKeyStoreFile,
                           clientPassphrase,
                           CLIENT_ALIAS,
                           clientCert,
                           clientKeyPair.getPrivate());
        }
        catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException e)
        {
            String msg = "Failed to create WISPA client key store";
            logger.error(msg, e);
            throw new WISPACertificateException(msg, e);
        }

        try
        {
            // Create the server key store
            createKeyStore(keyStoreFile,
                           serverPassphrase,
                           SERVER_ALIAS,
                           serverCert,
                           serverKeyPair.getPrivate());
        }
        catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException e)
        {
            String msg = "Failed to create WISPA server key store";
            logger.error(msg, e);
            throw new WISPACertificateException(msg, e);
        }

        try
        {
            // Create the server trust store, trusting the client certificate
            createKeyStore(trustStoreFile,
                           serverPassphrase,
                           CLIENT_ALIAS,
                           clientCert,
                           null);
        }
        catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException e)
        {
            String msg = "Failed to create WISPA server trust store";
            logger.error(msg, e);
            throw new WISPACertificateException(msg, e);
        }
    }

    /**
     * Open the WISPA server key store file for reading.  This contains the
     * certificate and private key for the WISPA server.  The key store password
     * will be required to interpret the contents.
     *
     * @return An open input stream for the WISPA server key store file.
     */
    public InputStream getKeyStoreStream()
    {
        FileInputStream keyStoreStream;
        try
        {
            keyStoreStream = new FileInputStream(keyStoreFile);
        }
        catch (FileNotFoundException e)
        {
            String msg = "Failed to read WISPA server key store file: " + keyStoreFile;
            logger.error(msg, e);
            throw new WISPACertificateException(msg, e);
        }
        return keyStoreStream;
    }

    /**
     * Open the WISPA server trust store file for reading.  The trust store
     * password will be required to interpret the contents.
     *
     * @return An open input stream for the WISPA server trust store file.
     */
    public InputStream getTrustStoreStream()
    {
        InputStream trustStoreStream;
        try
        {
            trustStoreStream = new FileInputStream(trustStoreFile);
        }
        catch (FileNotFoundException e)
        {
            String msg = "Failed to read WISPA server trust store file: " + trustStoreFile;
            logger.error(msg, e);
            throw new WISPACertificateException(msg, e);
        }
        return trustStoreStream;
    }

    /**
     * Get the password required to access the WISPA server key store and trust
     * store files.
     *
     * @return The store password.
     */
    public String getStorePassword()
    {
        return serverPassphrase;
    }

    /**
     * Get the password required to access the WISPA server trust store file.
     *
     * @return The trust store password.
     */
    public String getStoreFormat()
    {
        return storeFormat;
    }

    /**
     * Generate a new, self-signed certificate for the specified key pair.
     *
     * @param commonName The name to associate with the certificate.
     * @param keyPair The key pair to use in generating the certificate.
     * @return The generated certificate.
     */
    private X509Certificate generateCertificate(String commonName, KeyPair keyPair)
            throws NoSuchAlgorithmException, CertIOException, OperatorCreationException, CertificateException
    {
        // Set the certificate to expire a long time in the future (100 years).
        Instant now = Instant.now();
        Date notBeforeDate = Date.from(now);
        Date notAfterDate = Date.from(now.plus(Duration.ofDays(36500)));
        BigInteger serial = BigInteger.valueOf(now.toEpochMilli());

        // Self-signed certificate, so subject is the same as the issuer.
        X500Name issuer = new X500Name("CN=" + commonName);
        X500Name subject = issuer;
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        // The default Calendar in some locales (e.g. Thai's Buddhist lunar Calendar) will view
        // these Date objects as after the year 2050. That will cause the X509v3CertificateBuilder
        // to convert them to ASN1GeneralizedTime objects (see org.bouncycastle.asn1.x509.Time's
        // constructor), and will thus result in creating certificates with validFrom/To dates 'in
        // the future according to the Gregorian calendar'. We should expect anyone viewing these
        // certificates to interpret them using the Gregorian calendar, so should write dates using
        // it. We can achieve that by specifying the English locale.
        logger.info("Create certificate for " + commonName + " from " + notBeforeDate + " to " + notAfterDate);

        // Set up a minimum of required fields for the certificate,
        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                issuer, serial, notBeforeDate, notAfterDate, Locale.ENGLISH, subject, SubjectPublicKeyInfo.getInstance(publicKey.getEncoded()));
        JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();
        certBuilder.addExtension(Extension.subjectKeyIdentifier,
                                 false,
                                 extensionUtils.createSubjectKeyIdentifier(publicKey));

        GeneralName[] altNames = {
            new GeneralName(GeneralName.iPAddress, "127.0.0.1")
        };
        GeneralNames subjectAltNames = GeneralNames.getInstance(new DERSequence(altNames));
        certBuilder.addExtension(Extension.subjectAlternativeName, false, subjectAltNames);

        // Set up to sign the certificate.
        BouncyCastleProvider provider = new BouncyCastleProvider();
        JcaContentSignerBuilder contentSignerBuilder = new JcaContentSignerBuilder(SHA_256_WITH_RSA);
        contentSignerBuilder.setProvider(provider);
        JcaX509CertificateConverter certificateConverter = new JcaX509CertificateConverter();
        certificateConverter.setProvider(provider);

        // Finally, build and return the new certificate, signing it with the
        // private key from the provided key pair.
        X509Certificate cert = certificateConverter.getCertificate(
                certBuilder.build(contentSignerBuilder.build(privateKey)));

        return cert;
    }

    /**
     * Create a new key store, in PFX format, and save it to disk.  The key
     * stores created by this method contain a single entry, which can either be
     * a certificate/private-key pair or just a single certificate (used for
     * trust stores).
     *
     * @param keyStoreName The filename to use for the key store.
     * @param keyStorePassword The password to set for the key store.
     * @param alias The alias for the single entry to add to the key store.
     * @param cert The certificate to use for the single entry.
     * @param privateKey The private key to associate with the single entry.  If
     *                   set to null, no private key will be stored - just the
     *                   certificate.
     */
    private void createKeyStore(String keyStoreName,
                                String keyStorePassword,
                                String alias,
                                X509Certificate cert,
                                PrivateKey privateKey)
            throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException
    {
        KeyStore keyStore = KeyStore.getInstance(storeFormat);
        char[] pwdArray = keyStorePassword.toCharArray();
        keyStore.load(null, pwdArray);

        if (privateKey != null)
        {
            // Store public/private key
            X509Certificate[] chain = new X509Certificate[1];
            chain[0] = cert;
            keyStore.setKeyEntry(alias, privateKey, pwdArray, chain);
        }
        else
        {
            // Store trusted certificate
            keyStore.setCertificateEntry(alias, cert);
        }

        // Write the key store to disk.
        FileOutputStream stream = new FileOutputStream(keyStoreName);
        try
        {
            keyStore.store(stream, pwdArray);
        }
        finally
        {
            stream.close();
        }
    }

    private void writePEMToFile(Object pemObj, String filename) throws IOException
    {
        JcaPEMWriter pemWriter = new JcaPEMWriter(new FileWriter(filename));
        pemWriter.writeObject(pemObj);
        pemWriter.flush();
        pemWriter.close();
    }

    /**
     * Exception class for any errors hit while trying to generate WISPA
     * certificates and keys.
     */
    static class WISPACertificateException extends RuntimeException
    {
        public WISPACertificateException(String msg, Throwable cause)
        {
            super(msg, cause);
        }
    }
}
