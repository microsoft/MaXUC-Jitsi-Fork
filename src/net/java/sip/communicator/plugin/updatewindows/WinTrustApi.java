// Copyright (c) Microsoft Corporation. All rights reserved.
// Highly Confidential Material
package net.java.sip.communicator.plugin.updatewindows;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.WTypes.LPWSTR;
import com.sun.jna.platform.win32.WinBase.FILETIME;
import com.sun.jna.platform.win32.WinCrypt;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.DWORDByReference;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPVOID;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinNT.HRESULT;

/**
 * This is the interface for communicating with the WinTrust native library.
 */
public interface WinTrustApi extends Library
{
    WinTrustApi INSTANCE = Native.load("WinTrust", WinTrustApi.class);

    /**
     * The function performs a trust verification action on a specified object.
     * @param hwnd Optional handle to a caller window
     * @param pgActionID a GUID structure that identifies an action and the trust provider that supports that action
     * @param pWVTData  contains information that the trust provider needs to process the specified action identifier
     * @return If the trust provider verifies that the subject is trusted for the specified action, the return value is zero.
     *         No other value besides zero should be considered a successful return.
     */
    HRESULT WinVerifyTrust(HWND hwnd, GUID pgActionID, WINTRUST_DATA pWVTData);

    /**
     * The function retrieves trust provider information from a specified handle.
     * @param hStateData A handle previously set by the WinVerifyTrust function as the hWVTStateData member of the WINTRUST_DATA structure.
     * @return the function returns a CRYPT_PROVIDER_DATA structure
     */
    CRYPT_PROVIDER_DATA WTHelperProvDataFromStateData(HANDLE hStateData);

    /**
     * The function retrieves a signer or countersigner by index from the chain.
     *
     * @param pProvData        A pointer to the CRYPT_PROVIDER_DATA structure that contains the signer and countersigner
     *                         information.
     * @param idxSigner        The index of the signer. The index is zero based.
     * @param fCounterSigner   If true, the countersigner, as specified by idxCounterSigner, is retrieved by this
     *                         function; the signer that contains the countersigner is identified by idxSigner. If
     *                         false, the signer, as specified by idxSigner, is retrieved by this function.
     * @param idxCounterSigner The index of the countersigner. The index is zero based. The countersigner applies to the
     *                         signer identified by idxSigner.
     * @return the function returns a CRYPT_PROVIDER_SGNR structure for the requested signer or countersigner.
     */
    CRYPT_PROVIDER_SGNR WTHelperGetProvSignerFromChain(CRYPT_PROVIDER_DATA pProvData,
                                                       int idxSigner,
                                                       boolean fCounterSigner,
                                                       int idxCounterSigner);
    int SUCCEEDED = 0;

    GUID WINTRUST_ACTION_GENERIC_VERIFY_V2 = GUID.fromString("{00AAC56B-CD44-11d0-8CC2-00C04FC295EE}");

    DWORD WTD_UI_NONE = new DWORD(2);
    DWORD WTD_REVOKE_NONE = new DWORD(0);
    DWORD WTD_CHOICE_FILE = new DWORD(1);
    DWORD WTD_STATEACTION_VERIFY = new DWORD(1);
    DWORD WTD_STATEACTION_CLOSE = new DWORD(2);
    DWORD WTD_CACHE_ONLY_URL_RETRIEVAL = new DWORD(4096);

    @Structure.FieldOrder({"cbStruct", "pPolicyCallbackData", "pSIPClientData",
            "dwUIChoice", "fdwRevocationChecks", "dwUnionChoice", "pFile",
            "dwStateAction", "hWVTStateData", "pwszURLReference", "dwProvFlags",
            "dwUIContext", "pSignatureSettings"})
    class WINTRUST_DATA extends Structure
    {
        public DWORD cbStruct;
        public LPVOID pPolicyCallbackData;
        public LPVOID pSIPClientData;
        public DWORD dwUIChoice;
        public DWORD fdwRevocationChecks;
        public DWORD dwUnionChoice;

        public WINTRUST_FILE_INFO.ByReference pFile;

        public DWORD dwStateAction;
        public HANDLE hWVTStateData;
        public LPWSTR pwszURLReference;
        public DWORD dwProvFlags;
        public DWORD dwUIContext;
        public WINTRUST_SIGNATURE_SETTINGS.ByReference pSignatureSettings;

        public static class ByReference extends WINTRUST_DATA implements Structure.ByReference {}
    }

    @Structure.FieldOrder({"cbStruct", "pcwszFilePath", "hFile", "pgKnownSubject"})
    class WINTRUST_FILE_INFO extends Structure
    {
        public DWORD cbStruct;
        public LPWSTR pcwszFilePath;
        public HANDLE hFile;
        public GUID pgKnownSubject;

        public static class ByReference extends WINTRUST_FILE_INFO implements Structure.ByReference {}
    }

    @Structure.FieldOrder({"cbStruct", "dwIndex", "dwFlags", "cSecondarySigs",
            "dwVerifiedSigIndex", "pCryptoPolicy"})
    class WINTRUST_SIGNATURE_SETTINGS extends Structure
    {
        public DWORD cbStruct;
        public DWORD dwIndex;
        public DWORD dwFlags;
        public DWORD cSecondarySigs;
        public DWORD dwVerifiedSigIndex;
        public WinCryptApi.CERT_STRONG_SIGN_PARA.ByReference pCryptoPolicy;

        public static class ByReference extends WINTRUST_SIGNATURE_SETTINGS implements Structure.ByReference {}
    }

    @Structure.FieldOrder({"cbStruct", "pWintrustData", "fOpenedFile",
            "hWndParent", "pgActionID", "hProv", "dwError",
            "dwRegSecuritySettings", "dwRegPolicySettings", "psPfns",
            "cdwTrustStepErrors", "padwTrustStepErrors", "chStores",
            "pahStores", "dwEncoding", "hMsg", "csSigners", "pasSigners",
            "csProvPrivData", "pasProvPrivData", "dwSubjectChoice", "pPDSip",
            "pszUsageOID", "fRecallWithState", "sftSystemTime", "pszCTLSignerUsageOID",
            "dwProvFlags", "dwFinalError", "pRequestUsage", "dwTrustPubSettings",
            "dwUIStateFlags", "pSigState", "pSigSettings"})
    class CRYPT_PROVIDER_DATA extends Structure
    {
        public DWORD cbStruct;
        public WINTRUST_DATA.ByReference pWintrustData; // WINTRUST_DATA*
        public boolean fOpenedFile;
        public HWND hWndParent;
        public GUID.ByReference pgActionID; // GUID*
        public HANDLE hProv; // HCRYPTPROV
        public DWORD dwError;
        public DWORD dwRegSecuritySettings;
        public DWORD dwRegPolicySettings;
        public Pointer psPfns; // CRYPT_PROVIDER_FUNCTIONS*
        public DWORD cdwTrustStepErrors;
        public DWORDByReference padwTrustStepErrors; // DWORD*
        public DWORD chStores;
        public Pointer pahStores; // HCERTSTORE*
        public DWORD dwEncoding;
        public HANDLE hMsg; // HCRYPTMSG
        public DWORD csSigners;
        public Pointer pasSigners; // CRYPT_PROVIDER_SGNR*
        public DWORD csProvPrivData;
        public Pointer pasProvPrivData; // CRYPT_PROVIDER_PRIVDATA*
        public DWORD dwSubjectChoice;
        public Pointer pPDSip; // union
        public String pszUsageOID; // char*
        public boolean fRecallWithState;
        public FILETIME sftSystemTime;
        public String pszCTLSignerUsageOID;
        public DWORD dwProvFlags;
        public DWORD dwFinalError;
        public Pointer pRequestUsage; // PCERT_USAGE_MATCH
        public DWORD dwTrustPubSettings;
        public DWORD dwUIStateFlags;
        public Pointer pSigState; // CRYPT_PROVIDER_SIGSTATE*
        public Pointer pSigSettings; // WINTRUST_SIGNATURE_SETTINGS*
    }

    @Structure.FieldOrder({"cbStruct", "sftVerifyAsOf", "csCertChain",
            "pasCertChain", "dwSignerType", "psSigner", "dwError",
            "csCounterSigners", "pasCounterSigners", "pChainContext"})
    class CRYPT_PROVIDER_SGNR extends Structure
    {
        public DWORD cbStruct;
        public FILETIME sftVerifyAsOf;
        public DWORD csCertChain;
        public CRYPT_PROVIDER_CERT.ByReference pasCertChain;
        public DWORD dwSignerType;
        public Pointer psSigner; // CMSG_SIGNER_INFO*
        public DWORD dwError;
        public DWORD csCounterSigners;
        public CRYPT_PROVIDER_SGNR.ByReference pasCounterSigners;
        public WinCrypt.CERT_CHAIN_CONTEXT.ByReference pChainContext;

        public static class ByReference extends CRYPT_PROVIDER_SGNR implements Structure.ByReference {}
    }

    @Structure.FieldOrder({"cbStruct", "pCert", "fCommercial", "fTrustedRoot",
            "fSelfSigned", "fTestCert", "dwRevokedReason", "dwConfidence",
            "dwError", "pTrustListContext", "fTrustListSignerCert",
            "pCtlContext", "dwCtlError", "fIsCyclic", "pChainElement"})
    class CRYPT_PROVIDER_CERT extends Structure
    {
        public DWORD cbStruct;
        public WinCrypt.CERT_CONTEXT.ByReference pCert;
        public boolean fCommercial;
        public boolean fTrustedRoot;
        public boolean fSelfSigned;
        public boolean fTestCert;
        public DWORD dwRevokedReason;
        public DWORD dwConfidence;
        public DWORD dwError;
        public WinCrypt.CTL_CONTEXT.ByReference pTrustListContext;
        public boolean fTrustListSignerCert;
        public WinCrypt.CERT_CONTEXT.ByReference pCtlContext;
        public DWORD dwCtlError;
        public boolean fIsCyclic;
        public WinCrypt.CERT_CHAIN_CONTEXT.ByReference pChainElement;

        public static class ByReference extends CRYPT_PROVIDER_CERT implements Structure.ByReference {}
    }
}
