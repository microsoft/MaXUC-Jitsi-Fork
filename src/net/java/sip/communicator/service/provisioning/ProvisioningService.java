/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.provisioning;

/**
 * Provisioning service.
 *
 * @author Sebastien Vincent
 */
public interface ProvisioningService
{
    /**
     * Indicates if the provisioning has been enabled.
     *
     * @return <tt>true</tt> if the provisioning is enabled, <tt>false</tt> -
     * otherwise
     */
    String getProvisioningMethod();

    /**
     * Enables the provisioning with the given method. If the provisioningMethod
     * is null disables the provisioning.
     *
     * @param provisioningMethod the provisioning method
     */
    void setProvisioningMethod(String provisioningMethod);

    /**
     * Returns provisioning username if any.
     *
     * @return provisioning username
     */
    String getProvisioningUsername();

    /**
     * Returns the provisioning DN if any.
     *
     * @return provisioning DN or null if the DN is not set
     */
    String getProvisioningNumber();

    /**
     * Returns the provisioning URL.
     *
     * @return the provisioning URL
     */
    String getProvisioningUrl();

    /**
     * Refresh config from SIP-PS.
     */
    void getAndStoreConfig();

    /**
     * Get a fresh set of config from SIP-PS using the provisioning URL from the branding.
     */
    void getAndStoreFreshConfig();
}
