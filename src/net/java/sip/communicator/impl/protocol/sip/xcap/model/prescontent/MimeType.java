/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.prescontent;

import java.util.*;

import javax.xml.namespace.*;

/**
 * The PRES-CONTENT mime-type element.
 * <p/>
 * Compliant with Presence Content XDM Specification v1.0
 *
 * @author Grigorii Balutsel
 */
public class MimeType
{
    /**
     * The element value.
     */
    protected String value;

    /**
     * The map of any attributes.
     */
    private Map<QName, String> anyAttributes = new HashMap<>();

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

    /**
     * Gets the value of the anyAttributes property.
     *
     * @return the anyAttributes property.
     */
    public Map<QName, String> getAnyAttributes()
    {
        return anyAttributes;
    }
}
