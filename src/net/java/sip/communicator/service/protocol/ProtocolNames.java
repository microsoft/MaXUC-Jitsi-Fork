/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol;

/**
 * Specifies the names of some of the most popular instant messaging protocols.
 * These names should be used when registering an implementation of a protocol
 * with the osgi framework. These names must be set in the properties dictionary
 * that one specifies when registering an OSGI service. When setting one of
 * these names, a protocol implementor must map it against the
 * ProtocolProviderFactory.PROTOCOL_PROPERTY_NAME key.
 * @author Emil Ivov
 */
public interface ProtocolNames
{
    /**
     * The SIP (and SIMPLE) protocols.
     */
    String SIP  = "SIP";

    /**
     * The Jabber protocol.
     */
    String JABBER  = "Jabber";

    /**
     * The SIP Communicator MOCK protocol.
     */
    String SIP_COMMUNICATOR_MOCK  = "sip-communicator-mock";

    /**
     * The Address Book protocol.
     */
    String ADDRESS_BOOK = "CSProtocol";

    /**
     * The Group Contact protocol.
     */
    String GROUP_CONTACT = "GroupContacts";
}
