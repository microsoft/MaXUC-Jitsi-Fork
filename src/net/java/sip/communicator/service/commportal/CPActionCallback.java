// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.commportal;

import java.util.*;

/**
 * An interface for asking CommPortal to perform an action
 */
public interface CPActionCallback extends CPDataCallback
{
    /**
     * Returns the map of the parameters that this action requires.  Note that
     * this can be null if no parameters are required.  Should not include the
     * object type as that is provided by the SI name
     *
     * @return a map from each parameter that is required to the value of that
     *         parameter.
     */
    Map<String, Object> getRequestParams();

    /**
     * Called when the server has completed the action
     *
     * @param responseData the data that the server returned
     * @return true if the response data was valid - false indicates a server error
     */
    boolean onActionComplete(String responseData);
}
