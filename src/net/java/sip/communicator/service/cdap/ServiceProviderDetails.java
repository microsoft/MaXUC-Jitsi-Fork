// Copyright (c) Microsoft Corporation. All rights reserved.
// Highly Confidential Material
package net.java.sip.communicator.service.cdap;

import java.io.*;

/**
 * Class which contains the service provider details retrieved from CDAP.
 * <p>
 * This file has been copied verbatim from the AccessionAndroid source.
 */
public class ServiceProviderDetails implements Serializable
{
    private static final long serialVersionUID = 4392596649121405420L;
    private final String mId;
    private final String mName;
    private final String mRegion;
    private final Boolean mHidden;

    /**
     * The service provider details can only be set on construction. Once the
     * object has been initialized, they are read-only.
     * @param id - service provider ID (as defined on CDAP)
     * @param name - service provide name
     * @param region - service provider region
     * @param hidden - whether this service provider is hidden
     */
    public ServiceProviderDetails(String id, String name, String region, Boolean hidden)
    {
        mId = id;
        mName = name;
        mRegion = region;
        mHidden = hidden;
    }

    /**
     * @return service provider's CDAP ID.
     */
    public String getId()
    {
        return mId;
    }

    /**
     * @return service provider's CDAP name.
     */
    public String getName()
    {
        return mName;
    }

    /**
     * @return service provider's CDAP region
     */
    public String getRegion()
    {
        return mRegion;
    }

    /**
     * @return whether the service provider's name should be hidden in the list
     *         of service providers.
     */
    public boolean isHidden()
    {
        return mHidden;
    }

    @Override
    public boolean equals(Object object)
    {
        boolean isEqual = false;

        if (object == null)
        {
            // By definition not equal
            isEqual = false;
        }
        else if (this == object)
        {
            // Refer to the same object, must be the same
            isEqual = true;
        }
        else
        {
            try
            {
                ServiceProviderDetails serviceProviderDetails =
                        (ServiceProviderDetails)object;

                isEqual = mId.equals(serviceProviderDetails.getId()) &&
                          mName.equals(serviceProviderDetails.getName()) &&
                          mRegion.equals(serviceProviderDetails.getRegion()) &&
                          mHidden == serviceProviderDetails.isHidden();
            }
            catch (ClassCastException e)
            {
                // Do nothing, they aren't the same type
            }
        }

        return isEqual;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(128);

        builder.append("\tmId=").append(mId)
                .append("\n\tmName=").append(mName)
                .append("\n\tmRegion=").append(mRegion)
                .append("\n\tmHidden=").append(mHidden);

        return builder.toString();
    }
}
