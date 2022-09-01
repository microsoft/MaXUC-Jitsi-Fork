// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.sip.security;

import java.util.List;
import javax.sip.message.Message;

import gov.nist.javax.sip.header.ProxyRequire;
import gov.nist.javax.sip.header.Require;
import gov.nist.javax.sip.header.ims.SecurityClient;
import gov.nist.javax.sip.header.ims.SecurityMechanism;
import gov.nist.javax.sip.header.ims.SecurityServerHeader;
import gov.nist.javax.sip.header.ims.SecurityVerifyHeader;

import net.java.sip.communicator.util.Logger;

/**
 * Stateless utility class holding all methods that add 3GPP security headers,
 * for reference throughout the codebase.
 */
public class SecurityHeaderFactory
{
    /**
     * The logger for this class
     */
    public static final Logger sLog
            = Logger.getLogger(SecurityHeaderFactory.class);

    /**
     * Adds all required headers for REGISTER requests when using mediasec.
     *
     * @param request The message requiring the extra headers
     */
    public void addMediaSecurityHeaders(Message request)
    {
        addSecurityClientHeader(request, SecurityMechanism.SRTP);
        addProxyRequireHeader(request, SecurityMechanism.SRTP);
        addRequireHeader(request, SecurityMechanism.SRTP);
    }

    /**
     * Adds all required headers for REGISTER requests when using sec-agree.
     * NOTE THIS IS CURRENTLY NOT IMPLEMENTED - DO NOT USE FOR THE TIME BEING.
     *
     * @param request The message requiring the extra headers
     */
    public void addSignallingSecurityHeaders(Message request)
    {
        // Empty for now as we do not implement the signalling portion of the
        // 3GPP specs yet. Keep around as extensibility framework.
    }

    /**
     * Adds Security-Verify with required parameters.
     *
     * @param message The message requiring extra headers
     * @param cache Cached Security-Server headers for this registrar
     */
    public void addSecurityVerifyHeader(Message message,
                                               SecurityServerCache cache)
    {
        for (SecurityVerifyHeader header : cache.getHeaders())
        {
            sLog.debug("Setting Security-Verify header " + header);
            message.addHeader(header);
        }
    }

    /**
     * Adds Security-Client with required parameters.
     *
     * @param message The message requiring extra headers
     * @param mechanism The security mechanism for the header
     */
    private void addSecurityClientHeader(Message message,
                                         SecurityMechanism mechanism)
    {
        SecurityClient securityClientHeader = new SecurityClient();
        securityClientHeader.setSecurityMechanism(mechanism);
        sLog.debug("Setting Security-Client header " +
                   securityClientHeader.encode());
        message.addHeader(securityClientHeader);
    }

    /**
     * Adds Require with required parameters.
     *
     * @param message The message requiring extra headers
     * @param mechanism The security mechanism for the header
     */
    private void addRequireHeader(Message message,
                                         SecurityMechanism mechanism)
    {
        Require requireHeader = new Require(mechanism.getSecurityPlane());
        sLog.debug("Setting Require header " + requireHeader.encode());
        message.addHeader(requireHeader);
    }

    /**
     * Adds Proxy-Require with required parameters.
     *
     * @param message The message requiring extra headers
     * @param mechanism The security mechanism for the header
     */
    private void addProxyRequireHeader(Message message,
                                              SecurityMechanism mechanism)
    {
        ProxyRequire proxyRequireHeader = new ProxyRequire(mechanism.getSecurityPlane());
        sLog.debug("Setting Proxy-Require header " +
                   proxyRequireHeader.encode());
        message.addHeader(proxyRequireHeader);
    }

    /**
     * Ensure a Security-Server header that we support exists in a server
     * response.
     *
     * @param headers The list of Security-Server headers in the 401 response
     * @param mechanism The mechanism we want to ensure is listed
     */
    boolean doesSupportedSecurityHeaderExist(List<SecurityServerHeader> headers,
                                             SecurityMechanism mechanism)
    {
        for (SecurityServerHeader header : headers)
        {
            if (header.getSecurityMechanism() == mechanism)
            {
                sLog.debug("Supported header found");
                return true;
            }
        }
        sLog.debug("No supported headers found");
        return false;
    }
}
