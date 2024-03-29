/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.beans.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Dispatched to indicate that a change of a chat room property has failed.
 * The modification of a property could fail, because the implementation
 * doesn't support such a property.
 *
 * @author Yana Stamcheva
 */
public class ChatRoomPropertyChangeFailedEvent
    extends PropertyChangeEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The reason of the failure.
     */
    private final String reason;

    /**
     * Indicates why the failure occurred.
     */
    private final int reasonCode;

    /**
     * Creates a <tt>ChatRoomPropertyChangeEvent</tt> indicating that a change
     * has occurred for property <tt>propertyName</tt> in the <tt>source</tt>
     * chat room and that its value has changed from <tt>oldValue</tt> to
     * <tt>newValue</tt>.
     * <p>
     * @param source the <tt>ChatRoom</tt>, to which the property belongs
     * @param propertyName the name of the property
     * @param propertyValue the value of the property
     * @param expectedValue the expected after the change value of the property
     * @param reasonCode the code indicating the reason for the failure
     * @param reason more detailed explanation of the failure
     */
    public ChatRoomPropertyChangeFailedEvent(   ChatRoom source,
                                                String propertyName,
                                                Object propertyValue,
                                                Object expectedValue,
                                                int reasonCode,
                                                String reason)
    {
        super(source, propertyName, propertyValue, expectedValue);

        this.reasonCode = reasonCode;
        this.reason = reason;
    }

    /**
     * Returns the code of the failure. One of the static constants declared in
     * this class.
     * @return the code of the failure. One of the static constants declared in
     * this class
     */
    public int getReasonCode()
    {
        return reasonCode;
    }

    /**
     * Returns the reason of the failure.
     * @return the reason of the failure
     */
    public String getReason()
    {
        return reason;
    }

    /**
     * Returns a String representation of this event.
     *
     * @return String representation of this event
     */
    public String toString()
    {
        return "ChatRoomPropertyChangeEvent[type="
            + this.getPropertyName()
            + " sourceRoom="
            + this.getSource()
            + "oldValue="
            + this.getOldValue()
            + "newValue="
            + this.getNewValue()
            + "]";
    }
}
