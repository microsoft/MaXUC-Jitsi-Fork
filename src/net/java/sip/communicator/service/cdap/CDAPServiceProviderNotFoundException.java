// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.cdap;

/**
 * CDAPServiceProviderNotFoundException
 * <p>
 * Thrown if the configured service provider doesn't exist on the CDAP server.
 */
public class CDAPServiceProviderNotFoundException extends Exception
{
    /**
     * The value of the Service Provider ID if there is no configured Service
     * Provider.
     */
    private static final String NO_SERVICE_PROVIDER = "-1";

    private static final long serialVersionUID = 1L;

    /** Remapped service provider */
    public final String serviceProvider;

    /**
     * Service provider not found
     */
    public CDAPServiceProviderNotFoundException()
    {
        serviceProvider = NO_SERVICE_PROVIDER;
    }

    /**
     * Service provider not found, but a
     * mapped service provider is available.
     */
    public CDAPServiceProviderNotFoundException(String id)
    {
        serviceProvider = id;
    }
}
