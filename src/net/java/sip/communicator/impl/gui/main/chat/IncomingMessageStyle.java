/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;

/**
 * Defines the CSS style of incoming chat message elements.
 *
 * @author Yana Stamcheva
 */
public class IncomingMessageStyle
{
    /**
     * The incoming message indicator image path.
     */
    private static final String INCOMING_MESSAGE_INDICATOR_PATH
        = GuiActivator.getResources().getImageURL(
            "service.gui.lookandfeel.INCOMING_MESSAGE_INDICATOR").toString();

    /**
     * The incoming SMS indicator image path.
     */
    private static final String INCOMING_SMS_INDICATOR_PATH
        = GuiActivator.getResources().getImageURL(
            "service.gui.lookandfeel.INCOMING_SMS_INDICATOR").toString();

    /**
     * The colour used for incoming IM messages.
     */
    private static final String INCOMING_IM_COLOR = "#" +
        GuiActivator.getResources().getColorString("service.gui.INCOMING_CHAT_COLOR");

    /**
     * The colour used for imcoming SMS messages.
     */
    private static final String INCOMING_SMS_COLOR = "#" +
        GuiActivator.getResources().getColorString("service.gui.INCOMING_CHAT_COLOR");

    public static String createSingleMessageStyle(boolean isEdited)
    {
        StringBuilder style = new StringBuilder();

        style.append("style=\"padding:0px 0px ")
             .append(ScaleUtils.scaleInt(3))
             .append("px 0px;");

        if (isEdited)
        {
            style.append("font-style:italic;");
            style.append("\"");
        }
        style.append("\"");

        return style.toString().replaceFirst(";\"", "\"");
    }

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
        return "style =\" width:"
                + ScaleUtils.scaleInt(12)
                + "px; height:"
                + ScaleUtils.scaleInt(8)
                + "px; background-image: url('"+getMessageIndicatorPath(messageType)+"');"
                + " background-repeat: no-repeat; background-position: top right;\"";
    }

    /**
     * Returns the background color to use for the given message type.
     * @param messageType the messageType
     *
     * @return the background color
     */
    private static String getMessageColor(String messageType)
    {
        return Chat.INCOMING_SMS_MESSAGE.equals(messageType) ?
                                         INCOMING_SMS_COLOR : INCOMING_IM_COLOR;
    }

    /**
     * Returns the message indicator path to use for the given message type.
     * @param messageType the messageType
     *
     * @return the message indicator path
     */
    private static String getMessageIndicatorPath(String messageType)
    {
        return Chat.INCOMING_SMS_MESSAGE.equals(messageType) ?
                  INCOMING_SMS_INDICATOR_PATH : INCOMING_MESSAGE_INDICATOR_PATH;
    }
}
