// Copyright (c) Microsoft Corporation. All rights reserved.
// Highly Confidential Material
package net.java.sip.communicator.service.cdap;

import java.io.IOException;
import java.util.HashMap;
import org.jitsi.service.resources.BufferedImageFuture;

/**
 * A public interface for CDAPServiceImpl accessible from jitsi.
 */
public interface CDAPService
{
    /**
     * @return the ID of the service provider from the login link.
     */
    String getServiceProviderIdIfProvidedViaLink();

    /**
     * Get branding information from the server and save it.
     *
     * @param serviceProviderID - the ID of the service provider to get branding
     * info for e.g. 71001
     * @param forceRefresh - true if branding info should be fetched even if
     *                     we have it saved locally.
     * @throws Exception - exceptions are logged in the methods that throw them,
     *  and are passed straight to the calling method.
     */
    void fetchAndSaveBrandingInfo(String serviceProviderID, boolean forceRefresh)
            throws IOException, CDAPServiceProviderNotFoundException;

    /**
     * Returns a map of service providers with their ids fetched from the server.
     */
    HashMap<String, ServiceProviderDetails> getServiceProviders();

    /**
     * Get branding logo information
     */
    BufferedImageFuture getBrandingLogo();

    String getAppPrivacyStatementContent();
}
