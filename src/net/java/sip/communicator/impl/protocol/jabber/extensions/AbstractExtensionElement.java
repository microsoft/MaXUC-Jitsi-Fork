/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.ExtensionElement;

/**
 * A generic implementation of <tt>ExtensionElement</tt>. The purpose of this
 * class is quite similar to that of smack's {@link StandardExtensionElement}
 * with the main difference being that this one is meant primarily for
 * extension rather than using as a fallback for unknown elements. We let for
 * example our descendants handle child elements and we automate attribute
 * handling instead.
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public abstract class AbstractExtensionElement
    implements ExtensionElement
{
    /**
     * The name space of this packet extension. Should remain <tt>null</tt> if
     * there's no namespace associated with this element.
     */
    private String namespace;

    /**
     * A map of all attributes that this extension is currently using.
     */
    protected final Map<String, String> attributes
                                    = new LinkedHashMap<>();

    /**
     * The text content of this packet extension, if any.
     */
    private String textContent;

    /**
     * A list of extensions registered with this element.
     */
    private List<ExtensionElement> childExtensions
                                = new ArrayList<>();

    /**
     * Set the XML namespace for this element.
     *
     * @param namespace the XML namespace for this element.
     */
    public void setNamespace(String namespace)
    {
        this.namespace = namespace;
    }

    /**
     * Returns the XML namespace for this element or <tt>null</tt> if the
     * element does not live in a namespace of its own.
     *
     * @return the XML namespace for this element or <tt>null</tt> if the
     * element does not live in a namespace of its own.
     */
    public String getNamespace()
    {
        return namespace;
    }

    /**
     * Returns an XML representation of this extension.
     *
     * @return an XML representation of this extension.
     */
    public String toXML()
    {
        StringBuilder bldr = new StringBuilder();

        bldr.append("<").append(getElementName()).append(" ");

        if(getNamespace() != null)
            bldr.append("xmlns='").append(getNamespace()).append("'");

        //add the rest of the attributes if any
        for(Map.Entry<String, String> entry : attributes.entrySet())
        {
            bldr.append(" ")
                    .append(entry.getKey())
                        .append("='")
                            .append(entry.getValue())
                                .append("'");
        }

        //add child elements if any
        List<? extends ExtensionElement> childElements = getChildExtensions();
        String text = getText();

        if (childElements == null)
        {
            if ((text == null) || (text.length() == 0))
            {
                bldr.append("/>");
                return bldr.toString();
            }
            else
                bldr.append('>');
        }
        else
        {
            synchronized(childElements)
            {
                if (childElements.isEmpty()
                        && ((text == null) || (text.length() == 0)))
                {
                    bldr.append("/>");
                    return bldr.toString();
                }
                else
                {
                    bldr.append(">");
                    for(ExtensionElement packExt : childElements)
                        bldr.append(packExt.toXML());
                }
            }
        }

        //text content if any
        if((text != null) && (text.trim().length() > 0))
            bldr.append(text);

        bldr.append("</").append(getElementName()).append(">");

        return bldr.toString();
    }

    /**
     * Returns all sub-elements for this <tt>AbstractExtensionElement</tt> or
     * <tt>null</tt> if there aren't any.
     * <p>
     * Overriding extensions may need to override this method if they would like
     * to have anything more elaborate than just a list of extensions.
     *
     * @return the {@link List} of elements that this packet extension contains.
     */
    public List<? extends ExtensionElement> getChildExtensions()
    {
        return childExtensions;
    }

    /**
     * Sets the value of the attribute named <tt>name</tt> to <tt>value</tt>.
     *
     * @param name the name of the attribute that we are setting.
     * @param value an {@link Object} whose <tt>toString()</tt> method returns
     * the XML value of the attribute we are setting or <tt>null</tt> if we'd
     * like to remove the attribute with the specified <tt>name</tt>.
     */
    public void setAttribute(String name, Object value)
    {
        synchronized(attributes)
        {
            if(value != null)
                this.attributes.put(name, value.toString());
            else
                this.attributes.remove(name);
        }
    }

    /**
     * Returns the attribute with the specified <tt>name</tt> from the list of
     * attributes registered with this packet extension.
     *
     * @param attribute the name of the attribute that we'd like to retrieve.
     *
     * @return the value of the specified <tt>attribute</tt> or <tt>null</tt>
     * if no such attribute is currently registered with this extension.
     */
    public Object getAttribute(String attribute)
    {
        synchronized(attributes)
        {
            return attributes.get(attribute);
        }
    }

    /**
     * Specifies the text content of this extension.
     *
     * @param text the text content of this extension.
     */
    public void setText(String text)
    {
        this.textContent = text;
    }

    /**
     * Returns the text content of this extension or <tt>null</tt> if no text
     * content has been specified so far.
     *
     * @return the text content of this extension or <tt>null</tt> if no text
     * content has been specified so far.
     */
    public String getText()
    {
        return textContent;
    }

}
