// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.cdap;

import java.io.*;

/**
 * Class which contains the service provider details retrieved from CDAP.
 * <p>
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
}
