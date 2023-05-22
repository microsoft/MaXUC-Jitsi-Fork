/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;

/**
 * Defines the CSS style of outgoing chat message elements.
 *
 * @author Yana Stamcheva
 */
public class OutgoingMessageStyle
{
    /**
     * The outgoing message indicator image path.
     */
    private static final String OUTGOING_MESSAGE_INDICATOR_PATH
        = GuiActivator.getResources().getImageURL(
            "service.gui.lookandfeel.OUTGOING_MESSAGE_INDICATOR").toString();

    /**
     * The outgoing SMS indicator image path.
     */
    private static final String OUTGOING_SMS_INDICATOR_PATH
        = GuiActivator.getResources().getImageURL(
            "service.gui.lookandfeel.OUTGOING_SMS_INDICATOR").toString();

    /**
     * The colour used for outgoing IM messages.
     */
    private static final String OUTGOING_IM_COLOR = "#" +
        GuiActivator.getResources().getColorString("service.gui.OUTGOING_CHAT_COLOR");

    /**
     * The colour used for outgoing SMS messages.
     */
    private static final String OUTGOING_SMS_COLOR = "#" +
        GuiActivator.getResources().getColorString("service.gui.OUTGOING_CHAT_COLOR");
    /**
     * Creates the style of the table bubble (wrapping the message table).
     *
     * @return the style of the table bubble
     */
    public static String createTableBubbleStyle()
    {
        return "style=\"width:100%; position:relative;\"";
    }

    /**
     * Creates the style of the message table bubble.
     * @param messageType The type of message (IM or SMS)
     *
     * @return the style of the message table bubble
     */
    public static String createTableBubbleMessageStyle(String messageType)
    {
        return "style=\"font-size:"
                + ScaleUtils.scaleInt(10)
                + "px;padding:"
                + ScaleUtils.scaleInt(6)
                + "px; background-color: " + getMessageColor(messageType)
                + ";\"";
    }

    /**
     * Creates the style of the indicator pointing to the avatar image.
     * @param messageType The type of message (IM or SMS)
     *
     * @return the style of the indicator pointing to the avatar image
     */
    public static String createIndicatorStyle(String messageType)
    {
        return "style =\"width:"
                + ScaleUtils.scaleInt(8)
                + "px; height:"
                + ScaleUtils.scaleInt(8)
                + "px; background-image: url('"+getMessageIndicatorPath(messageType)+"');"
                + " background-repeat: no-repeat; background-position: top left;\"";
    }

    /**
     * Creates the style of the date.
     *
     * @return the style of the date
     */
    public static String createDateStyle()
    {
        return "style =\"font-size:"
            + ScaleUtils.scaleInt(7)
            + "px;color:5a5a5a;text-align:right;\"";
    }

    /**
     * Returns the background color to use for the given message type.
     * @param messageType the messageType
     *
     * @return the background color
     */
    private static String getMessageColor(String messageType)
    {
        return Chat.OUTGOING_SMS_MESSAGE.equals(messageType) ?
                                         OUTGOING_SMS_COLOR : OUTGOING_IM_COLOR;
    }

    /**
     * Returns the message indicator path to use for the given message type.
     * @param messageType the messageType
     *
     * @return the message indicator path
     */
    private static String getMessageIndicatorPath(String messageType)
    {
        return Chat.OUTGOING_SMS_MESSAGE.equals(messageType) ?
                  OUTGOING_SMS_INDICATOR_PATH : OUTGOING_MESSAGE_INDICATOR_PATH;
    }
}
