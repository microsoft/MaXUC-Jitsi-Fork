// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.sip.security;

import java.util.ArrayList;
import java.util.List;

import gov.nist.javax.sip.header.ims.SecurityServerHeader;
import gov.nist.javax.sip.header.ims.SecurityVerify;
import gov.nist.javax.sip.header.ims.SecurityVerifyHeader;

import net.java.sip.communicator.util.Logger;

/**
 * Class for which each instance stores the latest Security-Server headers
 * received from one registrar for use in Security-Verify replies.
 */
public class SecurityServerCache
{
    /**
     * The standard logger.
     */
    private static final Logger sLog =
            Logger.getLogger(SecurityServerCache.class);

    /**
     * The list of stored headers to add as Security-Verify headers in our next
     * request.
     */
    private final ArrayList<SecurityVerifyHeader> mVerifyHeaders = new ArrayList<>();

    /**
     * Object for synchronising access to the Security-Verify header list.
     */
    private final Object mHeaderLock = new Object();

    /**
     * Initialises one instance of this class and its stored header list mapping
     * to a given account ID.
     */
    SecurityServerCache()
    {
        sLog.debug("Initialised security header cache");
    }

    /**
     * Store Security-Server headers received as Security-Verify headers.
     *
     * @param headers A list of Security-Server headers received in a given SIP
     *                response
     */
    void storeHeaders(List<SecurityServerHeader> headers)
    {
        synchronized (mHeaderLock)
        {
            // Re-initialise the stored header list each time in case the list
            // has changed, to prevent accidental duplication of headers.
            mVerifyHeaders.clear();
            for (SecurityServerHeader serverHeader : headers)
            {
                sLog.debug("Storing header " + serverHeader);
                SecurityVerify verifyHeader = new SecurityVerify();
                transferHeaderContents(serverHeader, verifyHeader);
                mVerifyHeaders.add(verifyHeader);
            }
        }
    }

    /**
     * Get stored headers for adding to subsequent SIP requests.
     *
     * @return A list of stored Security-Verify headers
     */
    ArrayList<SecurityVerifyHeader> getHeaders()
    {
        synchronized (mHeaderLock)
        {
            return mVerifyHeaders;
        }
    }

    /**
     * Transfers parameters from a Security-Server header to a Security-Verify
     * header, effectively cloning it.
     *
     * @param serverHeader The Security-Server header we want to copy from
     * @param verifyHeader The Security-Verify header we want to copy to
     */
    private void transferHeaderContents(SecurityServerHeader serverHeader,
                                        SecurityVerify verifyHeader)
    {
        if (serverHeader.getSecurityMechanism() != null)
        {
            verifyHeader.setSecurityMechanism(serverHeader.getSecurityMechanism());
        }
        else
        {
            sLog.error("No security mechanism in header " + serverHeader);
        }

        // We do not currently support any Security-Server/Security-Verify
        // parameters besides sdes-srtp; mediasec, but if we ever wish to
        // expand to signalling too, we'll have a lot more parameters to
        // clone between the headers, e.g. alg, spi-c, spi-s. This method exists
        // to extend this function to every header parameter in future, but
        // there are implementation problems it's not worth addressing now so
        // this method remains otherwise empty.
    }
}
