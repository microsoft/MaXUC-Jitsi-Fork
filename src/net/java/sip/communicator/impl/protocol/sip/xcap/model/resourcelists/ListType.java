/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.resourcelists;

import java.util.*;

import javax.xml.namespace.*;

import org.w3c.dom.*;

/**
 * The XCAP list element.
 * <p/>
 * Compliant with rfc4825, rfc4826
 *
 * @author Grigorii Balutsel
 */
public class ListType
{
    /**
     * The name attribute.
     */
    protected String name;

    /**
     * The display-name element.
     */
    protected DisplayNameType displayName;

    /**
     * The list of entry elements.
     */
    protected List<EntryType> entries;

    /**
     * The list of external elements.
     */
    protected List<ExternalType> externals;

    /**
     * The list of list elements.
     */
    protected List<ListType> lists;

    /**
     * The list of entry-ref elements.
     */
    protected List<EntryRefType> entryRefs;

    /**
     * The list of any elements.
     */
    private List<Element> any;

    /**
     * The map of any attributes.
     */
    private Map<QName, String> anyAttributes = new HashMap<>();

    /**
     * Gets the value of the displayName property.
     *
     * @return the displayName property.
     */
    public DisplayNameType getDisplayName()
    {
        return displayName;
    }

    /**
     * Sets the value of the displayName property.
     *
     * @param displayName the displayName to set.
     */
    public void setDisplayName(DisplayNameType displayName)
    {
        this.displayName = displayName;
    }

    /**
     * Gets the value of the name property.
     *
     * @return the name property.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param name the name to set.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Gets the value of the entries property.
     *
     * @return the entries property.
     */
    public List<EntryType> getEntries()
    {
        if (entries == null)
        {
            entries = new ArrayList<>();
        }
        return entries;
    }

    /**
     * Gets the value of the externals property.
     *
     * @return the externals property.
     */
    public List<ExternalType> getExternals()
    {
        if (externals == null)
        {
            externals = new ArrayList<>();
        }
        return externals;
    }

    /**
     * Gets the value of the lists property.
     *
     * @return the lists property.
     */
    public List<ListType> getLists()
    {
        if (lists == null)
        {
            lists = new ArrayList<>();
        }
        return lists;
    }

    /**
     * Gets the value of the entryRefs property.
     *
     * @return the entryRefs property.
     */
    public List<EntryRefType> getEntryRefs()
    {
        if (entryRefs == null)
        {
            entryRefs = new ArrayList<>();
        }
        return entryRefs;
    }

    /**
     * Gets the value of the any property.
     *
     * @return the any property.
     */
    public List<Element> getAny()
    {
        if (any == null)
        {
            any = new ArrayList<>();
        }
        return this.any;
    }

    /**
     * Sets the value of the any property.
     *
     * @param any the any to set.
     */
    public void setAny(List<Element> any)
    {
        this.any = any;
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

    /**
     * Sets the value of the anyAttributes property.
     *
     * @param anyAttributes the anyAttributes to set.
     */
    public void setAnyAttributes(Map<QName, String> anyAttributes)
    {
        this.anyAttributes = anyAttributes;
    }
}
