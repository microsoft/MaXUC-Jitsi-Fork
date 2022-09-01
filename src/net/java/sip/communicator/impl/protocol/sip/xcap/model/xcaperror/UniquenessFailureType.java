/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.xcaperror;

import java.util.*;

/**
 * The XCAP uniqueness-failure element. Indicates that the requested operation
 * would result in a document that did not meet a uniqueness constraint defined
 * by the application usage. For each URI, element, or attribute specified by
 * the client that is not unique, an exists element is present as the content of
 * the error element. Each exists element has a "field" attribute that contains
 * a relative URI identifying the XML element or attribute whose value needs to
 * be unique, but wasn't. The relative URI is relative to the document itself,
 * and will therefore start with the root element. The query component of the
 * URI MUST be present if the node selector portion of the URI contains
 * namespace prefixes. Since the "field" node selector is a valid HTTP URI, it
 * MUST be percent-encoded. The exists element can optionally contain a list of
 * alt-value elements. Each one is a suggested alternate value that does not
 * currently exist on the server.
 * <p/>
 * Compliant with rfc4825
 *
 * @author Grigorii Balutsel
 */
public class UniquenessFailureType extends BaseXCapError
{
    /**
     * The list of exists elements.
     */
    protected List<ExistsType> exists;

    /**
     * Creates the XCAP uniqueness-failure error.
     */
    UniquenessFailureType()
    {
        super(null);
    }

    /**
     * Creates the XCAP uniqueness-failure error with phrase attribute.
     *
     * @param phrase the phrase to set.
     */
    public UniquenessFailureType(String phrase)
    {
        super(phrase);
    }

    /**
     * Gets the exists attribute.
     *
     * @return the exists property.
     */
    public List<ExistsType> getExists()
    {
        if (exists == null)
        {
            exists = new ArrayList<>();
        }
        return this.exists;
    }

    /**
     * The XCAP exists element.
     */
    public static class ExistsType
    {
        /**
         * The list of alt-value elements.
         */
        protected List<String> altValue;

        /**
         * The field attribute.
         */
        protected String field;

        /**
         * Gets the altValue attribute.
         *
         * @return the altValue property.
         */
        public List<String> getAltValue()
        {
            if (altValue == null)
            {
                altValue = new ArrayList<>();
            }
            return this.altValue;
        }

        /**
         * Gets the field attribute.
         *
         * @return the field property.
         */
        public String getField()
        {
            return field;
        }

        /**
         * Sets the value of the field property.
         *
         * @param field the field to set.
         */
        public void setField(String field)
        {
            this.field = field;
        }
    }
}
