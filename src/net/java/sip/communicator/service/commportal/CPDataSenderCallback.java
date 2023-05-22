// Copyright (c) Microsoft Corporation. All rights reserved.
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.commportal;

/**
 * Defines a callback for sending data to CP
 */
public interface CPDataSenderCallback extends CPDataCallback
{
    /**
     * Gets the data that should be sent for a particular service indication.
     *
     * @param siName the name of the service indication whose data we are looking for
     * @return the data to send to CP
     */
    String getData(String siName);

    /**
     * Called when the data has been sent to CommPortal successfully
     *
     * @return true if the data was parsed successfully, false otherwise.
     */
    boolean onDataSent(String result);

    /**
     * @return the format with which to request the data
     */
    DataFormat getDataFormat();
}
