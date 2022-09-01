// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.commportal;

/**
 * Defines a callback for being notified of changes to a particular service
 * indication.
 */
public interface CPDataRegistrationCallback extends CPDataCallback
{
    /**
     * A method which allows implementations of the interface to specify a
     * URL specifier for the COMET poll
     *
     * @return the URL specifier to use, or null if none is required
     */
    String getUrlSpecifier();
}
