/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.netaddr.event;

import static org.jitsi.util.Hasher.logHasher;

import java.net.*;

/**
 * A ChangeEvent is fired on change of the network configuration of the computer.
 *
 * @author Damian Minkov
 */
public class ChangeEvent
    extends java.util.EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Event type for interface going down.
     */
    public static final int IFACE_DOWN = 0;

    /**
     * Event type for interface going up.
     */
    public static final int IFACE_UP = 1;

    /**
     * Event type for address going down.
     */
    public static final int ADDRESS_DOWN = 2;

    /**
     * Event type for interface going down.
     */
    public static final int ADDRESS_UP = 3;

    /**
     * Event type for dns change.
     */
    public static final int DNS_CHANGE = 4;

    /**
     * Event type for becoming restricted by a captive wifi portal
     */
    public static final int NOW_RESTRICTED_BY_CAPTIVE_WIFI = 5;

    /**
     * Event type for no longer being restricted by a captive wifi portal
     */
    public static final int NO_LONGER_RESTRICTED_BY_CAPTIVE_WIFI = 6;

    /**
     * Event type indicating that WiFi information has changed
     */
    public static final int WIFI_INFO_CHANGED = 7;

    /**
     * The type of the current event.
     */
    private int type = -1;

    /**
     * Whether this event is after computer have been suspended.
     */
    private boolean standby = false;

    /**
     * The address that changed.
     */
    private InetAddress address;

    /**
     * Is this event initial one. When starting, no actual
     * change has occurred in the system.
     */
    private boolean initial;

    /**
     * Creates event.
     * @param source the source of the event, the interface.
     * @param type the type of the event.
     * @param address the address that changed.
     * @param standby is the event after a suspend of the computer.
     * @param initial is this event initial one.
     */
    public ChangeEvent(Object source,
                       int type,
                       InetAddress address,
                       boolean standby,
                       boolean initial)
    {
        super(source);

        this.type = type;
        this.address = address;
        this.standby = standby;
        this.initial = initial;
    }

    /**
     * Creates event.
     * @param source the source of the event, the interface.
     * @param type the type of the event.
     * @param address the address that changed.
     */
    public ChangeEvent(Object source,
                       int type,
                       InetAddress address)
    {
        this(source, type, address, false, false);
    }

    /**
     * Creates event.
     * @param source the source of the event, the interface.
     * @param type the type of the event.
     */
    public ChangeEvent(Object source, int type)
    {
        this(source, type, null, false, false);
    }

    /**
     * Creates event.
     * @param source the source of the event.
     * @param type the type of the event.
     * @param standby is the event after a suspend of the computer.
     */
    public ChangeEvent(Object source, int type, boolean standby)
    {
        this(source, type, null, standby, false);
    }

    /**
     * The type of this event.
     * @return the type
     */
    public int getType()
    {
        return type;
    }

    /**
     * The address that changed.
     * @return the address
     */
    public InetAddress getAddress()
    {
        return address;
    }

    /**
     * Whether this event is after suspend of the computer.
     * @return the standby
     */
    public boolean isStandby()
    {
        return standby;
    }

    /**
     * Overrides toString method.
     * @return string representing the event.
     */
    @Override
    public String toString()
    {
        StringBuilder buff = new StringBuilder();
        buff.append("ChangeEvent ");

        switch(type)
        {
            case IFACE_DOWN: buff.append("Interface down"); break;
            case IFACE_UP: buff.append("Interface up"); break;
            case ADDRESS_DOWN : buff.append("Address down"); break;
            case ADDRESS_UP : buff.append("Address up"); break;
            case DNS_CHANGE : buff.append("Dns has changed"); break;
            case NOW_RESTRICTED_BY_CAPTIVE_WIFI : buff.append("Restricted by captive wifi portal"); break;
            case NO_LONGER_RESTRICTED_BY_CAPTIVE_WIFI : buff.append("No longer restricted by captive wifi portal"); break;
            case WIFI_INFO_CHANGED: buff.append("WiFi info changed"); break;
        }

        buff.append(", standby=" + standby)
            .append(", source=" + source)
            .append(", address=" + logHasher(address != null ? address.getHostAddress() : null))
            .append(", isInitial=" + initial);

        return buff.toString();
    }

    /**
     * Is this event initial one. When starting, no actual
     * change has occurred in the system.
     * @return is this event initial one.
     */
    public boolean isInitial()
    {
        return initial;
    }
}
