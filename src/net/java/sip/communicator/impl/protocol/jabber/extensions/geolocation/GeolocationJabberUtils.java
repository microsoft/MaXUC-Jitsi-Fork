/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.geolocation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.java.sip.communicator.service.protocol.OperationSetGeolocation;
import net.java.sip.communicator.util.Logger;

/**
 * This class give some static methods for converting a geolocation message
 * to a different format.
 *
 * @author Guillaume Schreiner
 */
public class GeolocationJabberUtils
{
    /**
     * The logger of this class.
     */
    private static final Logger logger =
        Logger.getLogger(GeolocationJabberUtils.class);

    /**
     * Convert geolocation from GeolocationExtension format to Map format
     *
     * @param geolocExt the GeolocationExtension XML message
     * @return a Map with geolocation information
     */
    public static Map<String, String> convertExtensionToMap(
                                        GeolocationExtensionElement geolocExt)
    {
        Map<String, String> geolocMap = new Hashtable<>();

        addFloatToMap(  geolocMap
                      , OperationSetGeolocation.ALT
                      , geolocExt.getAlt());

        addStringToMap(  geolocMap
                       , OperationSetGeolocation.AREA
                       , geolocExt.getArea());

        addFloatToMap(geolocMap
                      , OperationSetGeolocation.BEARING
                      , geolocExt.getBearing());

        addStringToMap(geolocMap
                       , OperationSetGeolocation.BUILDING
                       , geolocExt.getBuilding());

        addStringToMap(geolocMap
                       , OperationSetGeolocation.COUNTRY
                       , geolocExt.getCountry());

        addStringToMap(geolocMap
                       , OperationSetGeolocation.DATUM
                       , geolocExt.getDatum());

        addStringToMap(geolocMap
                       , OperationSetGeolocation.DESCRIPTION
                       , geolocExt.getDescription());

        addFloatToMap(geolocMap
                      , OperationSetGeolocation.ERROR
                      , geolocExt.getError());

        addStringToMap(geolocMap
                       , OperationSetGeolocation.FLOOR
                       , geolocExt.getFloor());

        addFloatToMap(geolocMap
                      , OperationSetGeolocation.LAT
                      , geolocExt.getLat());

        addStringToMap(geolocMap
                       , OperationSetGeolocation.LOCALITY
                       , geolocExt.getLocality());

        addFloatToMap(geolocMap
                      , OperationSetGeolocation.LON
                      , geolocExt.getLon());

        addStringToMap(geolocMap
                       , OperationSetGeolocation.POSTALCODE
                       , geolocExt.getPostalCode());

        addStringToMap(geolocMap
                       , OperationSetGeolocation.REGION
                       , geolocExt.getRegion());

        addStringToMap(geolocMap
                       , OperationSetGeolocation.ROOM
                       , geolocExt.getRoom());

        addStringToMap(geolocMap
                       , OperationSetGeolocation.STREET
                       , geolocExt.getStreet());

        addStringToMap(geolocMap
                       , OperationSetGeolocation.TEXT
                       , geolocExt.getText());

        addStringToMap(geolocMap
                       , OperationSetGeolocation.TIMESTAMP
                       , geolocExt.getTimestamp());

        return geolocMap;
    }

    /**
     * Utility function for adding a float var to a Map.
     *
     * @param map the map we're adding a new value to.
     * @param key the key that we're adding the new value against
     * @param value the float var that we're adding to <tt>map</tt> against the
     * <tt>key</tt> key.
     */
    private static void addFloatToMap(Map<String, String> map,
                                      String key,
                                      float value)
    {
        if (value != -1)
        {
            Float valor = new Float(value);
            map.put(key, valor.toString());
        }
    }

    /**
     * Utility function that we use when adding a String to a map ()
     * @param map Map
     * @param key String
     * @param value String
     */
    private static void addStringToMap(Map<String, String> map,
                                       String key,
                                       String value)
    {
        if (value != null)
        {
            map.put(key, value);
        }
    }

    /**
     * Convert a geolocation details Map to a GeolocationPacketExtension format
     *
     * @param geolocation a Map with geolocation information
     * @return a GeolocationExtension ready to be included into a Jabber
     * message
     */
    public static GeolocationExtensionElement convertMapToExtension(
                                               Map<String, String> geolocation)
    {
        GeolocationExtensionElement geolocExt = new GeolocationExtensionElement();

        Set<Entry<String, String>> entries = geolocation.entrySet();
        Iterator<Entry<String, String>> itLine = entries.iterator();

        while (itLine.hasNext())
        {
            Entry<String, String> line = itLine.next();

            String curParam = line.getKey();
            String curValue = line.getValue();

            String prototype = Character.toUpperCase(curParam.charAt(0))
                + curParam.substring(1);

            String setterFunction = "set" + prototype;

            try
            {
                Method toCall
                    = GeolocationExtensionElement
                        .class.getMethod(setterFunction, String.class);
                Object[] arguments = new Object[]{curValue};

                try
                {
                    toCall.invoke(geolocExt, arguments);
                }
                catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException exc)
                {
                    logger.error(exc);
                }
            }
            catch (SecurityException | NoSuchMethodException exc)
            {
                logger.error(exc);
            }
        }
        return geolocExt;
    }
}
