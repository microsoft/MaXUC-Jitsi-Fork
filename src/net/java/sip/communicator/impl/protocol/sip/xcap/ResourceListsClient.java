/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap;

import net.java.sip.communicator.impl.protocol.sip.xcap.model.resourcelists.*;

/**
 * XCAP resource-lists client interface.
 * <p/>
 * Compliant with rfc4825, rfc4826
 *
 * @author Grigorii Balutsel
 */
public interface ResourceListsClient
{
    /**
     * Resource-lists uri format.
     */
    String DOCUMENT_FORMAT = "resource-lists/users/%2s/index";

    /**
     * Resource-lists content type.
     */
    String RESOURCE_LISTS_CONTENT_TYPE =
            "application/resource-lists+xml";

    /**
     * Resource-lists content type.
     */
    String ELEMENT_CONTENT_TYPE = "application/xcap-el+xml";

    /**
     * Resource-lists namespace.
     */
    String NAMESPACE = "urn:ietf:params:xml:ns:xcap-caps";

    /**
     * Puts the resource-lists to the server.
     *
     * @param resourceLists the resource-lists to be saved on the server.
     * @throws IllegalStateException if the user has not been connected.
     * @throws XCapException         if there is some error during operation.
     */
    void putResourceLists(ResourceListsType resourceLists)
            throws XCapException;

    /**
     * Gets the resource-lists from the server.
     *
     * @return the resource-lists.
     * @throws IllegalStateException if the user has not been connected.
     * @throws XCapException         if there is some error during operation.
     */
    ResourceListsType getResourceLists()
            throws XCapException;

    /**
     * Deletes the resource-lists from the server.
     *
     * @throws IllegalStateException if the user has not been connected.
     * @throws XCapException         if there is some error during operation.
     */
    void deleteResourceLists()
            throws XCapException;

    /**
     * Gets the resource-lists from the server.
     *
     * @param anchor reference to the list.
     * @return the list.
     * @throws IllegalStateException if the user has not been connected.
     */
    ListType getList(String anchor)
    ;
}
