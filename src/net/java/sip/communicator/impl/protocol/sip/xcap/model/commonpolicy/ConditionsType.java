/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy;

import java.util.*;

import org.w3c.dom.*;

/**
 * The Authorization Rules conditions element.
 * <p/>
 * Compliant with rfc5025
 *
 * @author Grigorii Balutsel
 */
public class ConditionsType
{
    /**
     * The list of identity elements.
     */
    private List<IdentityType> identities;

    /**
     * The list of sphere elements.
     */
    private List<SphereType> spheres;

    /**
     * The list of validity elements.
     */
    private List<ValidityType> validities;

    /**
     * The list of any elements.
     */
    private List<Element> any;

    /**
     * Gets the value of the identities property.
     *
     * @return the identities property.
     */
    public List<IdentityType> getIdentities()
    {
        if (this.identities == null)
        {
            this.identities = new ArrayList<>();
        }
        return identities;
    }

    /**
     * Gets the value of the spheres property.
     *
     * @return the spheres property.
     */
    public List<SphereType> getSpheres()
    {
        if (this.spheres == null)
        {
            this.spheres = new ArrayList<>();
        }
        return spheres;
    }

    /**
     * Gets the value of the validities property.
     *
     * @return the validities property.
     */
    public List<ValidityType> getValidities()
    {
        if (this.validities == null)
        {
            this.validities = new ArrayList<>();
        }
        return validities;
    }

    /**
     * Gets the value of the any property.
     *
     * @return the any property.
     */
    public List<Element> getAny()
    {
        if (this.any == null)
        {
            this.any = new ArrayList<>();
        }
        return any;
    }
}
