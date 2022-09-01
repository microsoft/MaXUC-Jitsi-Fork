// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.commportal;

/**
 * An interface for getting or sending data to CommPortal
 */
public interface CPDataCallback
{
    /**
     * Gets the SI Name for the data that we are sending / getting.  This could
     * be a comma-separated string if we are trying to send / get multiple
     * service indications in one.
     *
     * @return the name of the service indication(s) that we are requesting
     */
    String getSIName();

    /**
     * Return a unique identifier for the bit of CP data we're getting.
     *
     * Usually the SI name is sufficient here, because the SI is a 'singleton' - e.g. there is only
     * the one CurrentCalls to fetch - http://<CP session>/events.js?events=CurrentCalls.
     * However, some CPDataCallbacks must override this with something more specific, such as the
     * Connection SI (which is used to track CTD calls), which must be done for each individual call
     *  - there is no such thing as '_the_ Connection SI', we poll for "a Call ID's Connection SI" -
     *  http://<CP session>/call<callID>/events.js?events=Connection.
     * @return something to uniquely identify the CP data being got.
     */
    default String getCPDataID()
    {
        return getSIName();
    }

    /**
     * Called if there is a problem with the request
     *
     * @param error The error that we hit
     */
    void onDataError(CPDataError error);

    /**
     * Some service indications require a specific API version, different to the
     * default that we use.  This method allows requesters or senders to specify
     * that version.
     * <p/>
     * To use the default, just return null.
     *
     * @return the CommPortal API version required or null.
     */
    default String getCommPortalVersion()
    {
        return null;
    }

    /**
     * @return true if this service indication contains private information
     *         which means that its data shouldn't be logged.
     */
    default boolean isPrivate()
    {
        return false;
    }

    /**
     * An enum of the different ways in which we can request data
     */
    enum DataFormat
    {
        DATA("data"),
        DATA_JS("data.js");

        /**
         * The format to use when requesting the data
         */
        private final String mFormat;

        DataFormat(String format)
        {
            mFormat = format;
        }

        public String getFormat()
        {
            return mFormat;
        }
    }
}
