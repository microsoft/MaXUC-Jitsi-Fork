/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * This interface is an extension of the operation set, meant to be
 * implemented by protocols that support exchange of geolocation details (like
 * Jabber for example).
 *
 * @author Guillaume Schreiner
 */
public interface OperationSetGeolocation
    extends OperationSet
{
    //Names of keys used for storing geolocation data in geolocation Maps.

    /**
     * The name of the geolocation map key corresponding to the altitude in
     * meters above or below sea level (e.g. 1609).
     */
    String ALT = "alt";

    /**
     * The name of the geolocation map key that we use for storing named areas
     * such as a campus or neighborhood (e.g. Central Park).
     */
    String AREA = "area";

    /**
     * The name of the geolocation map key that we use for storing GPS bearing
     * (direction in which the entity is heading to reach its next waypoint),
     * measured in decimal degrees relative to true north.
     */
    String BEARING = "bearing";

    /**
     * The name of the geolocation map key that we use for indicating a
     * specific building on a street or in an area (e.g. The Empire State
     * Building).
     */
    String BUILDING = "building";

    /**
     * The name of the geolocation map key that we use for indicating the
     * nation where the user is located (e.g. Greenland).
     */
    String COUNTRY = "country";

    /**
     * GPS datum.
     */
    String DATUM = "datum";

    /**
     * The name of the geolocation map key that we use for storing a
     * natural-language name for or description of a given location (e.g.
     * Bill's house).
     */
    String DESCRIPTION = "description";

    /**
     * The name of the geolocation map key that we use for storing horizontal
     * GPS errors in arc minutes (e.g. 10).
     */
    String ERROR = "error";

    /**
     * The name of the geolocation map key that we use for storing a particular
     * floor in a building (e.g. 102).
     */
    String FLOOR = "floor";

    /**
     * The name of the geolocation map key that we use for storing geographic
     * latitude in decimal degrees North (e.g. 39.75).
     */
    String LAT = "lat";

    /**
     * The name of the geolocation map key that we use for indicating a
     * locality within the administrative region, such as a town or city (e.g.
     * Paris).
     */
    String LOCALITY = "locality";

    /**
     * The name of the geolocation map key that we use for indicating
     * longitude in decimal degrees East (e.g. -104.99).
     */
    String LON = "lon";

    /**
     * The name of the geolocation map key that we use for storing post codes
     * (or any code used for postal delivery) (e.g. 67000).
     */
    String POSTALCODE = "postalcode";

    /**
     * The name of the geolocation map key that we use for indicating an
     * administrative region of the nation, such as a state or province (e.g.
     * Ile de France).
     */
    String REGION = "region";

    /**
     * The name of the geolocation map key that we use for indicating a
     * particular room in a building (e.g. C-425).
     */
    String ROOM = "room";

    /**
     * The name of the geolocation map key that we use for storing a
     * thoroughfare within a locality, or a crossing of two thoroughfares (e.g.
     * 34th and Broadway).
     */
    String STREET = "street";

    /**
     * The name of the geolocation map key that we use to indicate a catch-all
     * element that captures any other information about the location (e.g.
     * North-West corner of the lobby).
     */
    String TEXT = "text";

    /**
     * The name of the geolocation map key that we use to indicate UTC
     * timestamp specifying the moment when the reading was taken
     * (e.g. 2007-05-27T21:12Z).
     */
    String TIMESTAMP = "timestamp";

    /**
     * Publish the location contained in the <tt>geolocation</tt> map to all
     * contacts in our contact list.
     *
     * @param geolocation a <tt>java.uil.Map</tt> containing the geolocation
     * details of the position we'd like to publish.
     */
    void publishGeolocation(Map<String, String> geolocation);

    /**
     * Retrieve the geolocation of the contact corresponding to
     * <tt>contactIdentifier</tt>.
     *
     * @param contactIdentifier the address of the <tt>Contact</tt> whose
     * geolocation details we'd like to retrieve.
     *
     * @return a <tt>java.util.Map</tt> containing the geolocation details of
     * the contact with address <tt>contactIdentifier</tt>.
     */
    Map<String, String> queryContactGeolocation(String contactIdentifier);

    /**
     * Registers a listener that would get notifications any time a contact
     * publishes a new geolocation.
     *
     * @param listener the <tt>GeolocationListener</tt> to register
     */
    void addGeolocationListener(GeolocationListener listener);

    /**
     * Removes a listener previously registered for notifications of changes in
     * the contact geolocation details.
     *
     * @param listener the <tt>GeolocationListener</tt> to unregister
     */
    void removeGeolocationListener(GeolocationListener listener);
}
