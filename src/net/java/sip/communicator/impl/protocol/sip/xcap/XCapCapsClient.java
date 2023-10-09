/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap;

/**
 * XCAP xcap-caps client interface.
 * <p/>
 * Compliant with rfc4825
 *
 * @author Grigorii Balutsel
 */
public interface XCapCapsClient
{
    /**
     * Xcap-caps uri format
     */
    String DOCUMENT_FORMAT = "xcap-caps/global/index";

    /**
     * Xcap-caps content type
     */
    String CONTENT_TYPE = "application/xcap-caps+xml";
}
