/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy;

/**
 * The Authorization Rules sphere element.
 * <p/>
 * Compliant with rfc5025
 *
 * @author Grigorii Balutsel
 */
public class SphereType
{
    /**
     * The value attribute.
     */
    private String value;

    /**
     * Create the sphere element.
     */
    public SphereType()
    {
    }

    /**
     * Gets the value of the value property.
     *
     * @return the value property.
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Sets the value of the value property.
     *
     * @param value the value to set.
     */
    public void setValue(String value)
    {
        this.value = value;
    }
}
