// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.updatewindows;

import static net.java.sip.communicator.plugin.updatewindows.WinCryptApi.*;
import static net.java.sip.communicator.plugin.updatewindows.WinTrustApi.*;

import java.util.Set;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.WTypes.LPSTR;
import com.sun.jna.platform.win32.WTypes.LPWSTR;
import com.sun.jna.platform.win32.WinCrypt;
import com.sun.jna.platform.win32.WinCrypt.CERT_CHAIN_ELEMENT;
import com.sun.jna.platform.win32.WinCrypt.CERT_CONTEXT;
import com.sun.jna.platform.win32.WinCrypt.CERT_SIMPLE_CHAIN;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.DWORDByReference;
import com.sun.jna.platform.win32.WinNT;
import org.apache.commons.codec.binary.Hex;

import net.java.sip.communicator.util.Logger;

/**
 * Utility class to verify Windows PE file signatures via WinAPI WinVerifyTrust..
 */
public final class SignatureVerificationUtil
{
    private static final Logger logger = Logger.getLogger(SignatureVerificationUtil.class);

    /**
     * SHA1 thumbprints of known Microsoft Root CA certificates. SHA1 is OK for thumbprint hash, it's only discouraged
     * for signature hash.
     */
    static final Set<String> MS_ROOT_CA_THUMBPRINT =
            Set.of("8f43288ad272f3103b6fb1428485ea3014c0bcfe");

    /**
     * Verifies the file is signed with valid signature trusted by MS Root CA.
     * @param filePath file to check
     */
    static void verifyFileSignature(String filePath) throws Exception
    {
        logger.info("Verifying signature of the file");

        WINTRUST_DATA trustData = prepareTrustData(filePath);

        logger.debug("Checking primary signature at index #0");
        WinNT.HRESULT res = WinTrustApi.INSTANCE.WinVerifyTrust(null, WINTRUST_ACTION_GENERIC_VERIFY_V2, trustData);

        try
        {
            if (res.intValue() != SUCCEEDED)
            {
                throw new Exception("Signature verification failed with error " +
                                    Integer.toHexString(res.intValue()));
            }

            logger.info("Signature #0 is OK");

            // verify root certificate pinning
            verifyRootCertificatePinning(trustData);

            // verify other signatures if present
            for (int secondarySigIndex = 1;
                 secondarySigIndex <= trustData.pSignatureSettings.cSecondarySigs.intValue(); secondarySigIndex++)
            {
                logger.debug("Checking secondary signature at index #" + secondarySigIndex);

                // Need to clear the previous state data from the last call to WinVerifyTrust
                trustData.dwStateAction = WTD_STATEACTION_CLOSE;
                res = WinTrustApi.INSTANCE.WinVerifyTrust(null, WINTRUST_ACTION_GENERIC_VERIFY_V2, trustData);
                if (res.intValue() != SUCCEEDED)
                {
                    // No need to call WinVerifyTrust again to close
                    trustData = null;
                    throw new Exception("Failed to clear previous state data for WinVerifyTrust: code = " +
                                        Integer.toHexString(res.intValue()));
                }

                // reset handle
                trustData.hWVTStateData = null;

                // caller must reset dwStateAction as it may have been changed during the last call
                trustData.dwStateAction = WTD_STATEACTION_VERIFY;
                trustData.pSignatureSettings.dwIndex = new DWORD(secondarySigIndex);

                res = WinTrustApi.INSTANCE.WinVerifyTrust(null, WINTRUST_ACTION_GENERIC_VERIFY_V2, trustData);
                if (res.intValue() != SUCCEEDED)
                {
                    throw new Exception("Failed to clear previous state data for WinVerifyTrust: code = " +
                                        Integer.toHexString(res.intValue()));
                }

                logger.info("Signature #" + secondarySigIndex + " is OK");

                // verify root certificate pinning
                verifyRootCertificatePinning(trustData);
            }

        }
        finally
        {
            if (trustData != null)
            {
                // close and free resources
                trustData.dwStateAction = WTD_STATEACTION_CLOSE;
                WinTrustApi.INSTANCE.WinVerifyTrust(null, WINTRUST_ACTION_GENERIC_VERIFY_V2, trustData);
            }
        }
    }

    private static WINTRUST_DATA prepareTrustData(String filePath)
    {
        // prepare input data for WinVerifyTrust call
        WINTRUST_DATA trustData = new WINTRUST_DATA();
        trustData.cbStruct = new DWORD(trustData.size());
        trustData.dwUIChoice = WTD_UI_NONE;
        trustData.fdwRevocationChecks = WTD_REVOKE_NONE;
        trustData.dwUnionChoice = WTD_CHOICE_FILE;
        trustData.dwStateAction = WTD_STATEACTION_VERIFY;
        trustData.dwUIContext = new DWORD(0);
        trustData.dwProvFlags = WTD_CACHE_ONLY_URL_RETRIEVAL;

        // set location of the file to verify
        trustData.pFile = new WINTRUST_FILE_INFO.ByReference();
        trustData.pFile.cbStruct = new DWORD(trustData.pFile.size());
        trustData.pFile.pcwszFilePath = new LPWSTR(filePath);

        // From https://github.com/microsoft/Windows-classic-samples/blob/main/Samples/Security/CodeSigning/cpp/codesigning.cpp
        trustData.pSignatureSettings = new WINTRUST_SIGNATURE_SETTINGS.ByReference();
        trustData.pSignatureSettings.cbStruct = new DWORD(trustData.pSignatureSettings.size());
        trustData.pSignatureSettings.dwFlags = new DWORD(WSS_GET_SECONDARY_SIG_COUNT | WSS_VERIFY_SPECIFIC);
        trustData.pSignatureSettings.dwIndex = new DWORD(0);

        // Enforce strong policy check
        // szOID_CERT_STRONG_SIGN_OS_1 means only SHA2 (not SHA1) for signature algorithm
        // and only RSA/ECDSA (not DSA) algorithms with key lengths >= 2048/256
        trustData.pSignatureSettings.pCryptoPolicy = new CERT_STRONG_SIGN_PARA.ByReference();
        trustData.pSignatureSettings.pCryptoPolicy.cbSize = trustData.pSignatureSettings.pCryptoPolicy.size();
        trustData.pSignatureSettings.pCryptoPolicy.dwInfoChoice = CERT_STRONG_SIGN_OID_INFO_CHOICE;
        trustData.pSignatureSettings.pCryptoPolicy.pszOID = new LPSTR(szOID_CERT_STRONG_SIGN_OS_1);

        return trustData;
    }

    private static void verifyRootCertificatePinning(WINTRUST_DATA trustData) throws Exception
    {
        // retrieve data using the handle from WinVerifyTrust result
        CRYPT_PROVIDER_DATA cryptoProvData = WinTrustApi.INSTANCE.WTHelperProvDataFromStateData(trustData.hWVTStateData);
        if (cryptoProvData == null)
        {
            throw new Exception(
                    "Failed to read signature details: code = " + Integer.toHexString(Native.getLastError()));
        }

        // check all signers
        logger.debug("Total signers " + cryptoProvData.csSigners.intValue());
        for (int idxSigner = 0; idxSigner < cryptoProvData.csSigners.intValue(); idxSigner++)
        {
            CRYPT_PROVIDER_SGNR pProvSigner = WinTrustApi.INSTANCE.WTHelperGetProvSignerFromChain(cryptoProvData,
                                                                                                  idxSigner,
                                                                                                  false,
                                                                                                  0);
            if (pProvSigner == null)
            {
                throw new Exception("Failed to retrieve signer: code = " + Integer.toHexString(Native.getLastError()));
            }

            logger.debug("Signer #" + idxSigner + " of type " + pProvSigner.dwSignerType);

            if (isTrustedRoot(pProvSigner.pChainContext))
            {
                logger.debug("Found Microsoft Root CA certificate");
                return;
            }

        }

        // no signers with root certificate matched to MS Root CA
        throw new Exception("Root certificate is not Microsoft Root CA");
    }

    /**
     * Based on <a
     * href="https://github.com/microsoft/Windows-classic-samples/blob/main/Samples/Win7Samples/security/cryptoapi/VerifyNameTrust/VerifyNameTrust/VerifyNameTrust.cpp">...</a>
     */
    private static boolean isTrustedRoot(WinCrypt.CERT_CHAIN_CONTEXT.ByReference pChainContext)
    {
        CERT_SIMPLE_CHAIN[] rgpChain = pChainContext.getRgpChain();

        // validate all chains
        logger.debug("Total certificate chains " + rgpChain.length);
        for (int chainIndex = 0; chainIndex < rgpChain.length; chainIndex++)
        {
            logger.debug("Certificate chain #" + chainIndex);
            CERT_SIMPLE_CHAIN pChain = rgpChain[chainIndex];

            // certificates in the chain
            CERT_CHAIN_ELEMENT[] rgpElements = pChain.getRgpElement();

            // root certificate is the last one
            CERT_CONTEXT.ByReference pCertContext = rgpElements[rgpElements.length - 1].pCertContext;

            // compare thumbprint with Microsoft Root CA one
            if (!isThumbprintMatch(pCertContext, MS_ROOT_CA_THUMBPRINT))
            {
                return false;
            }
        }

        return true;
    }

    private static boolean isThumbprintMatch(CERT_CONTEXT.ByReference pCertContext,
                                             Set<String> expectedThumbprint)
    {
        byte[] rawPropertyValue = getCertificateProperty(pCertContext, CERT_HASH_PROP_ID);

        if (rawPropertyValue != null)
        {
            // convert to hex string
            String thumbprint = Hex.encodeHexString(rawPropertyValue);
            return expectedThumbprint.contains(thumbprint);
        }

        logger.debug("Failed to retrieve the certificate property");

        return false;
    }

    /**
     * Based on <a
     * href="https://learn.microsoft.com/en-us/windows/win32/seccrypto/example-c-program-getting-and-setting-certificate-properties">...</a>
     */
    private static byte[] getCertificateProperty(CERT_CONTEXT.ByReference pCertContext,
                                                 int dwPropId)
    {
        // data buffer size
        DWORDByReference cbData = new DWORDByReference();

        // first call to just get size of the data
        if (!(WinCryptApi.INSTANCE.CertGetCertificateContextProperty(
                pCertContext, dwPropId, null, cbData)))
        {
            logger.debug("Call #1 to get property length failed.");
            return null;
        }

        // Allocate required amount of memory, it will be freed on GC
        Memory pvData = new Memory(cbData.getValue().intValue());

        // second call to get the actual data
        if (!(WinCryptApi.INSTANCE.CertGetCertificateContextProperty(
                pCertContext, dwPropId, pvData, cbData)))
        {
            logger.debug("Call #2 to get the data failed.");
            return null;
        }

        // return as byte array
        return pvData.getByteArray(0, cbData.getValue().intValue());
    }
}
