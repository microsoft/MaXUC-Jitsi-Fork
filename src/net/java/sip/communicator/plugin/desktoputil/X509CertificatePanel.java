/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.security.*;
import java.security.cert.*;
import java.security.interfaces.*;
import java.text.*;
import java.util.*;

import javax.naming.*;
import javax.naming.ldap.*;
import javax.security.auth.x500.*;
import javax.swing.*;

import org.jitsi.service.resources.*;

/**
 * Panel that shows the content of an X509Certificate.
 */
public class X509CertificatePanel
    extends TransparentPanel
{
    private static final long serialVersionUID = -8368302061995971947L;

    /**
     * Constructs a X509 certificate panel.
     *
     * @param certificate <tt>X509Certificate</tt> object
     */
    public X509CertificatePanel(X509Certificate certificate)
    {
        // The text color must be set to black as the background color is not
        // brandable. Save off the default colours first so that they can be
        // re-applied once the certificate has been created.
        Color originalLabelColor = UIManager.getColor("Label.foreground");
        Color originalTextAreaColor = UIManager.getColor("TextArea.foreground");
        UIManager.put("Label.foreground", Color.BLACK);
        UIManager.put("TextArea.foreground", Color.BLACK);

        ResourceManagementService R = DesktopUtilActivator.getResources();
        DateFormat dateFormatter
            = DateFormat.getDateInstance(DateFormat.MEDIUM);

        Insets valueInsets = new Insets(2,10,0,0);
        Insets titleInsets = new Insets(10,5,0,0);

        setLayout(new GridBagLayout());

        int currentRow = 0;

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(2,5,0,0);
        constraints.gridx = 0;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.gridy = currentRow++;

        X500Principal issuer = certificate.getIssuerX500Principal();
        X500Principal subject = certificate.getSubjectX500Principal();

        add(new JLabel(
            R.getI18NString("service.gui.CERT_INFO_ISSUED_TO")),
            constraints);

        // subject
        constraints.insets = valueInsets;
        try
        {
            for(Rdn name : new LdapName(subject.getName()).getRdns())
            {
                String nameType = name.getType();
                String lblKey = "service.gui.CERT_INFO_" + nameType;
                String lbl = R.getI18NString(lblKey);

                if ((lbl == null) || ("!" + lblKey + "!").equals(lbl))
                    lbl = nameType;

                constraints.gridy = currentRow++;
                constraints.gridx = 0;
                add(new JLabel(lbl), constraints);

                Object nameValue = name.getValue();

                if (nameValue instanceof byte[])
                {
                    byte[] nameValueAsByteArray = (byte[]) nameValue;

                    lbl
                        = getHex(nameValueAsByteArray) + " ("
                            + new String(nameValueAsByteArray) + ")";
                }
                else
                    lbl = nameValue.toString();

                constraints.gridx = 1;
                add(new JLabel(lbl), constraints);
            }
        }
        catch (InvalidNameException ine)
        {
            constraints.gridy = currentRow++;
            add(new JLabel(
                R.getI18NString("service.gui.CERT_INFO_CN")),
                constraints);
            constraints.gridx = 1;
            add(
                new JLabel(subject.getName()),
                constraints);
        }

        // issuer
        constraints.gridy = currentRow++;
        constraints.gridx = 0;
        constraints.insets = titleInsets;
        add(new JLabel(
            R.getI18NString("service.gui.CERT_INFO_ISSUED_BY")),
            constraints);
        constraints.insets = valueInsets;
        try
        {
            for(Rdn name : new LdapName(issuer.getName()).getRdns())
            {
                String nameType = name.getType();
                String lblKey = "service.gui.CERT_INFO_" + nameType;
                String lbl = R.getI18NString(lblKey);

                if ((lbl == null) || ("!" + lblKey + "!").equals(lbl))
                    lbl = nameType;

                constraints.gridy = currentRow++;
                constraints.gridx = 0;
                constraints.gridx = 0;
                add(new JLabel(lbl), constraints);

                Object nameValue = name.getValue();

                if (nameValue instanceof byte[])
                {
                    byte[] nameValueAsByteArray = (byte[]) nameValue;

                    lbl
                        = getHex(nameValueAsByteArray) + " ("
                            + new String(nameValueAsByteArray) + ")";
                }
                else
                    lbl = nameValue.toString();

                constraints.gridx = 1;
                add(new JLabel(lbl), constraints);
            }
        }
        catch (InvalidNameException ine)
        {
            constraints.gridy = currentRow++;
            add(new JLabel(
                R.getI18NString("service.gui.CERT_INFO_CN")),
                constraints);
            constraints.gridx = 1;
            add(
                new JLabel(issuer.getName()),
                constraints);
        }

        // validity
        constraints.gridy = currentRow++;
        constraints.gridx = 0;
        constraints.insets = titleInsets;
        add(new JLabel(
            R.getI18NString("service.gui.CERT_INFO_VALIDITY")),
            constraints);
        constraints.insets = valueInsets;

        constraints.gridy = currentRow++;
        constraints.gridx = 0;
        add(new JLabel(
            R.getI18NString("service.gui.CERT_INFO_ISSUED_ON")),
            constraints);
        constraints.gridx = 1;
        add(
            new JLabel(dateFormatter.format(certificate.getNotBefore())),
            constraints);

        constraints.gridy = currentRow++;
        constraints.gridx = 0;
        add(new JLabel(
            R.getI18NString("service.gui.CERT_INFO_EXPIRES_ON")),
            constraints);
        constraints.gridx = 1;
        add(
            new JLabel(dateFormatter.format(certificate.getNotAfter())),
            constraints);

        constraints.gridy = currentRow++;
        constraints.gridx = 0;
        constraints.insets = titleInsets;
        add(new JLabel(
            R.getI18NString("service.gui.CERT_INFO_FINGERPRINTS")),
            constraints);
        constraints.insets = valueInsets;

        try
        {
            String sha1String = getThumbprint(certificate, "SHA1");
            String md5String = getThumbprint(certificate, "MD5");

            JTextArea sha1Area = new JTextArea(sha1String);
            sha1Area.setLineWrap(false);
            sha1Area.setOpaque(false);
            sha1Area.setWrapStyleWord(true);
            sha1Area.setEditable(false);

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            add(new JLabel("SHA1:"),
                constraints);

            constraints.gridx = 1;
            add(
                sha1Area,
                constraints);

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            add(new JLabel("MD5:"),
                constraints);

            JTextArea md5Area = new JTextArea(md5String);
            md5Area.setLineWrap(false);
            md5Area.setOpaque(false);
            md5Area.setWrapStyleWord(true);
            md5Area.setEditable(false);

            constraints.gridx = 1;
            add(
                md5Area,
                constraints);
        }
        catch (Exception e)
        {
            // do nothing as we cannot show this value
        }

        constraints.gridy = currentRow++;
        constraints.gridx = 0;
        constraints.insets = titleInsets;
        add(new JLabel(
            R.getI18NString("service.gui.CERT_INFO_CERT_DETAILS")),
            constraints);
        constraints.insets = valueInsets;

        constraints.gridy = currentRow++;
        constraints.gridx = 0;
        add(new JLabel(
            R.getI18NString("service.gui.CERT_INFO_SER_NUM")),
            constraints);
        constraints.gridx = 1;
        add(
            new JLabel(certificate.getSerialNumber().toString()),
            constraints);

        constraints.gridy = currentRow++;
        constraints.gridx = 0;
        add(new JLabel(
            R.getI18NString("service.gui.CERT_INFO_VER")),
            constraints);
        constraints.gridx = 1;
        add(
            new JLabel(String.valueOf(certificate.getVersion())),
            constraints);

        constraints.gridy = currentRow++;
        constraints.gridx = 0;
        add(new JLabel(
            R.getI18NString("service.gui.CERT_INFO_SIGN_ALG")),
            constraints);
        constraints.gridx = 1;
        add(
            new JLabel(String.valueOf(certificate.getSigAlgName())),
            constraints);

        constraints.gridy = currentRow++;
        constraints.gridx = 0;
        constraints.insets = titleInsets;
        add(new JLabel(
            R.getI18NString("service.gui.CERT_INFO_PUB_KEY_INFO")),
            constraints);
        constraints.insets = valueInsets;

        constraints.gridy = currentRow++;
        constraints.gridx = 0;
        add(new JLabel(
            R.getI18NString("service.gui.CERT_INFO_ALG")),
            constraints);
        constraints.gridx = 1;
        add(
            new JLabel(certificate.getPublicKey().getAlgorithm()),
            constraints);

        if(certificate.getPublicKey().getAlgorithm().equals("RSA"))
        {
            RSAPublicKey key = (RSAPublicKey)certificate.getPublicKey();

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            add(new JLabel(
                R.getI18NString("service.gui.CERT_INFO_PUB_KEY")),
                constraints);

            JTextArea pubkeyArea = new JTextArea(
                R.getI18NString(
                    "service.gui.CERT_INFO_KEY_BYTES_PRINT",
                    new String[]{
                        String.valueOf(key.getModulus().toByteArray().length-1),
                        key.getModulus().toString(16)
                    }));
            pubkeyArea.setLineWrap(false);
            pubkeyArea.setOpaque(false);
            pubkeyArea.setWrapStyleWord(true);
            pubkeyArea.setEditable(false);

            constraints.gridx = 1;
            add(
                pubkeyArea,
                constraints);

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            add(new JLabel(
                R.getI18NString("service.gui.CERT_INFO_EXP")),
                constraints);
            constraints.gridx = 1;
            add(
                new JLabel(key.getPublicExponent().toString()),
                constraints);

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            add(new JLabel(
                R.getI18NString("service.gui.CERT_INFO_KEY_SIZE")),
                constraints);
            constraints.gridx = 1;
            add(
                new JLabel(R.getI18NString(
                    "service.gui.CERT_INFO_KEY_BITS_PRINT",
                    new String[]{
                        String.valueOf(key.getModulus().bitLength())})),
                constraints);
        }
        else if(certificate.getPublicKey().getAlgorithm().equals("DSA"))
        {
            DSAPublicKey key =
                (DSAPublicKey)certificate.getPublicKey();

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            add(new JLabel("Y:"), constraints);

            JTextArea yArea = new JTextArea(key.getY().toString(16));
            yArea.setLineWrap(false);
            yArea.setOpaque(false);
            yArea.setWrapStyleWord(true);
            yArea.setEditable(false);

            constraints.gridx = 1;
            add(
                yArea,
                constraints);
        }

        constraints.gridy = currentRow++;
        constraints.gridx = 0;
        add(new JLabel(
            R.getI18NString("service.gui.CERT_INFO_SIGN")),
            constraints);

        JTextArea signArea = new JTextArea(
            R.getI18NString(
                    "service.gui.CERT_INFO_KEY_BYTES_PRINT",
                    new String[]{
                        String.valueOf(certificate.getSignature().length),
                        getHex(certificate.getSignature())
                    }));
        signArea.setLineWrap(false);
        signArea.setOpaque(false);
        signArea.setWrapStyleWord(true);
        signArea.setEditable(false);

        constraints.gridx = 1;
        add(
            signArea,
            constraints);

        // The text color has been set to black as the background is not
        // brandable. Put back the original colors.
        UIManager.put("Label.foreground", originalLabelColor);
        UIManager.put("TextArea.foreground", originalTextAreaColor);
    }

    /**
     * Converts the byte array to hex string.
     * @param raw the data.
     * @return the hex string.
     */
    private String getHex( byte [] raw )
    {
        if (raw == null)
            return null;

        StringBuilder hex = new StringBuilder(2 * raw.length);
        try (Formatter f = new Formatter(hex))
        {
            for (byte b : raw)
                f.format("%02x", b);
        }
        return hex.toString();
    }

    /**
     * Calculates the hash of the certificate known as the "thumbprint"
     * and returns it as a string representation.
     *
     * @param cert The certificate to hash.
     * @param algorithm The hash algorithm to use.
     * @return The SHA-1 hash of the certificate.
     * @throws CertificateException
     */
    private static String getThumbprint(X509Certificate cert, String algorithm)
        throws CertificateException
    {
        MessageDigest digest;
        try
        {
            digest = MessageDigest.getInstance(algorithm);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new CertificateException(e);
        }
        byte[] encodedCert = cert.getEncoded();
        StringBuilder sb = new StringBuilder(encodedCert.length * 2);
        try (Formatter f = new Formatter(sb))
        {
            for (byte b : digest.digest(encodedCert))
                f.format("%02x", b);
        }
        return sb.toString();
    }
}
