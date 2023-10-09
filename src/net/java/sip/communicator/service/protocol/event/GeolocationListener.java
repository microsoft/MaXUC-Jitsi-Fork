/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * The listener interface for receiving geolocation events.
 * The class that is interested in processing a geolocation event
 * implements this interface, and the object created with that
 * class is registered with the geolocation operation set, using its
 * <code>addGeolocationListener</code> method. When a geolocation event
 * occurs, that object's <code>contactGeolocationChanged</code> method is
 * invoked.
 *
 * @see GeolocationEvent
 *
 * @author Guillaume Schreiner
 */
public interface GeolocationListener
    extends EventListener
{
}
