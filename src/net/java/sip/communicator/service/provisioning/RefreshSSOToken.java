// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.provisioning;

import org.jitsi.util.CustomAnnotations.NotNull;

/**
 * This class contains applicationID and tokenCache. The properties are serialized via protobuf
 * to be passed on to the Electron application. The properties determine if SSO is enabled and if there is token cache
 * already saved (user logged in).
 * The fields in this class must not be null.
 */
public class RefreshSSOToken
{
    private final String token;
    private final String tokenCache;

    public RefreshSSOToken(@NotNull String token, @NotNull String tokenCache) {
        this.token = token;
        this.tokenCache = tokenCache;
    }

    public String getToken()
    {
        return token;
    }

    public String getTokenCache()
    {
        return tokenCache;
    }
}
