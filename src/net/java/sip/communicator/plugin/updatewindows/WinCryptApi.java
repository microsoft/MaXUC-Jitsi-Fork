// Copyright (c) Microsoft Corporation. All rights reserved.
// Highly Confidential Material
package net.java.sip.communicator.plugin.updatewindows;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WTypes.LPSTR;
import com.sun.jna.platform.win32.WinCrypt;
import com.sun.jna.platform.win32.WinDef;

/**
 * This is the interface for communicating with the Crypto32 native library.
 */
public interface WinCryptApi extends Library
{
    WinCryptApi INSTANCE = Native.load("Crypt32", WinCryptApi.class);

    int CERT_HASH_PROP_ID = 3;
    int WSS_VERIFY_SPECIFIC = 0x00000001;
    int WSS_GET_SECONDARY_SIG_COUNT = 0x00000002;

    int CERT_STRONG_SIGN_OID_INFO_CHOICE = 2;

    /**
     * The SHA2 hash algorithm is supported. MD2, MD4, MD5, and SHA1 are not supported.
     * The signing and public key algorithms can be RSA or ECDSA. The DSA algorithm is not supported.
     * The key size for the RSA algorithm must equal or be greater than 2047 bits.
     * The key size for the ECDSA algorithm must equal or be greater than 256 bits.
     * Strong signing of CRLs and OCSP responses are enabled.
     * See <a href="https://learn.microsoft.com/en-us/windows/win32/api/wincrypt/ns-wincrypt-cert_strong_sign_para">more details.</a>
     */
    String szOID_CERT_STRONG_SIGN_OS_1 = "1.3.6.1.4.1.311.72.1.1";

    /**
     * The function retrieves the information contained in an extended property of a certificate context.
     * @param pCertContext structure of the certificate that contains the property to be retrieved.
     * @param dwPropId the property to be retrieved.
     * @param pvData A pointer to a buffer to receive the data as determined by dwPropId.
     *               Structures pointed to by members of a structure returned are also returned following the base structure.
     *               Therefore, the size contained in pcbData often exceeds the size of the base structure.
     *               This parameter can be NULL to set the size of the information for memory allocation purposes.
     * @param pcbData A pointer to a DWORD value that specifies the size, in bytes, of the buffer pointed to by the pvData parameter.
     *                When the function returns, the DWORD value contains the number of bytes to be stored in the buffer.
     * @return if the function succeeds, the function returns true
     */
    boolean CertGetCertificateContextProperty(WinCrypt.CERT_CONTEXT pCertContext, int dwPropId,
                                              Pointer pvData, WinDef.DWORDByReference pcbData);

    /**
     * This data structure is defined in JNA WinCrypt
     * but has an error in ByReference definition, so redefined.
     */
    @Structure.FieldOrder({"cbSize", "dwInfoChoice", "pszOID"})
    class CERT_STRONG_SIGN_PARA extends Structure {
        public int cbSize;
        public int dwInfoChoice;
        public LPSTR pszOID;

        public static class ByReference extends CERT_STRONG_SIGN_PARA implements Structure.ByReference {}
    }
}
