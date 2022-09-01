// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.commportal;

/**
 * An interface used when requesting a token from the CommPortal service
 */
public interface CPTokenGetterCallback
{
    /**
     * Called when the token has been obtained from CommPortal
     *
     * @param token The token from CommPortal
     */
    void onDataReceived(String token);

    /**
     * Called if there is a problem with the request
     *
     * @param error The error that we hit
     */
    void onDataError(CPDataError error);

    /**
     * Return how long this token should be valid for, or -1 if the token should
     * be valid for as long as possible.
     *
     * @return optional period in milliseconds until the token should expire.
     */
    int getValidFor();

    /**
     * Gets the capability that this token should have. Must not be NULL
     *
     * @return the capability of this token
     */
    Capability getCapability();

    /**
     * Enum of the capabilities that we can request tokens with.
     */
    enum Capability
    {
        AccessionMeetingSSO,
        CallMe;     // Required to generate Call Me buttons through embedded CPWeb.  Not used directly by AD.

        /**
         * Converts this enum to a string that allows it to be sent to CommPortal
         *
         * @return a comma separated string of the capability options
         */
        public static String allToString()
        {
            StringBuilder builder = new StringBuilder();

            boolean first = true;
            for (Capability capability : values())
            {
                if (!first)
                    builder.append(",");

                builder.append(capability);
                first = false;
            }

            return builder.toString();
        }
    }
}
