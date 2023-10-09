package net.java.sip.communicator.service.httputil;

import java.security.Principal;

import org.apache.http.auth.Credentials;

/**
 * An implementation of Credentials used strictly for holding SSO tokens.
 */
public class SSOCredentials implements Credentials
{
    private String ssoToken;

    public SSOCredentials(String ssoToken) {
        this.ssoToken = ssoToken;
    }

    /**
     * Always returns null.
     */
    @Override
    public Principal getUserPrincipal()
    {
        return null;
    }

    /**
     * Returns an SSO token.
     */
    @Override
    public String getPassword()
    {
        return ssoToken;
    }
}
