/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.systray;

import javax.swing.*;

import org.jitsi.service.resources.*;

/**
 * The <tt>PopupMessage</tt> class encloses informations to show in a popup.
 * While a message title and a message body are mandatory informations,
 * a popup message could provides more stuffs like a component or an image which
 * may be used by a <tt>PopupMessageHandler</tt> capable to handle it.
 *
 * @author Symphorien Wanko
 */
public class PopupMessage
{
    /**
     * The maximum length that we allow a message to be
     */
    private static final int MAX_MSG_LENGTH = 500;

    /**
     * Message to show in the popup.
     */
    private final String message;

    /**
     * Title of the message.
     */
    private final String messageTitle;

    /**
     * An icon representing the contact from which the notification comes.
     */
    private BufferedImageFuture imageIcon;

    /**
     * A ready to show <tt>JComponet</tt> for this <tt>PopupMessage</tt>.
     */
    private JComponent component;

    /**
     * The type of the message.
     */
    private int messageType;

    /**
     * Additional info to be used by the <tt>PopupMessageHandler</tt>.
     */
    private Object tag;

    /**
     * Whether this message is due to an error.
     */
    private boolean isError = false;

    /**
     * The action, if any, to be taken when the toast is clicked.
     */
    private Runnable onClickAction;

    /**
     * Creates a <tt>PopupMessage</tt> with the given title and message inside.
     *
     * @param title title of the message
     * @param message message to show in the systray
     */
    public PopupMessage(String title, String message)
    {
        this.messageTitle = title;

        // Make sure we don't display ridiculously long messages
        this.message = (message != null && message.length() > MAX_MSG_LENGTH) ?
                         message.substring(0, MAX_MSG_LENGTH) + "..." : message;
    }

    /**
     * Creates a system tray message with the given title and message content.
     * The message type will affect the icon used to present the message.
     *
     * @param title the title, which will be shown
     * @param message the content of the message to display
     * @param messageType the message type; one of XXX_MESSAGE_TYPE constants
     * declared in <tt>SystrayService
     */
    public PopupMessage(String title, String message, int messageType)
    {
        this(title, message);
        this.messageType = messageType;
    }

    /**
     * Creates a new <tt>PopupMessage</tt> with the given title, message and
     * icon.
     *
     * @param title the title of the message
     * @param message message to show in the systray
     * @param imageIcon an incon to show in the popup message.
     */
    public PopupMessage(String title, String message, BufferedImageFuture imageIcon)
    {
        this(title, message);
        this.imageIcon = imageIcon;
    }

    /**
     * Creates a new <tt>PopupMessage</tt> with the given
     * <tt>JComponent</tt> as its content. This constructor also takes a title
     * and a message as replacements in cases the component is not usable.
     *
     * @param component the component to put in the <tt>PopupMessage</tt>
     * @param title of the message
     * @param message message to use in place of the component
     */
    public PopupMessage(JComponent component, String title, String message)
    {
        this(title, message);
        this.component = component;
    }

    /**
     * Creates a new <tt>PopupMessage</tt> with the given
     * <tt>JComponent</tt> as its content. This constructor also takes a title
     * and a message as replacements in cases the component is not usable.
     *
     * @param title of the message
     * @param message the message to show in this popup
     * @param tag additional info to be used by the <tt>PopupMessageHandler</tt>
     */
    public PopupMessage(String title, String message, Object tag)
    {
        this(title, message);
        this.tag = tag;
    }

    /**
     * Creates a new <tt>PopupMessage</tt> with the given
     * <tt>JComponent</tt> as its content. This constructor also takes a title
     * and a message as replacements in cases the component is not usable.
     *
     * @param title the title of the message
     * @param message the message to show in this popup
     * @param imageIcon the image icon to show in this popup message
     * @param tag additional info to be used by the <tt>PopupMessageHandler</tt>
     */
    public PopupMessage(String title, String message,
        BufferedImageFuture imageIcon, Object tag)
    {
        this(title, message, imageIcon, tag, false);
    }

    /**
     * Creates a new <tt>PopupMessage</tt> with the given
     * <tt>JComponent</tt> as its content. This constructor also takes a title
     * and a message as replacements in cases the component is not usable.
     *
     * @param title the title of the message
     * @param message the message to show in this popup
     * @param imageIcon the image icon to show in this popup message
     * @param tag additional info to be used by the <tt>PopupMessageHandler</tt>
     * @param isError whether this pop-up message is due to an error
     */
    public PopupMessage(String title, String message,
        BufferedImageFuture imageIcon, Object tag, Boolean isError)
    {
        this(title, message, imageIcon);
        this.tag = tag;
        this.isError = isError;
    }

    /**
     * Creates a new <tt>PopupMessage</tt> with the given
     * <tt>JComponent</tt> as its content. This constructor also takes a title
     * and a message as replacements in cases the component is not usable.
     *
     * @param title the title of the message
     * @param message the message to show in this popup
     * @param imageIcon the image icon to show in this popup message
     * @param tag additional info to be used by the <tt>PopupMessageHandler</tt>
     * @param onClickAction The action to be taken when the toast is clicked.
     **/
    public PopupMessage(String title,
                        String message,
                        BufferedImageFuture imageIcon,
                        Object tag,
                        Runnable onClickAction)
    {
        this(title, message, imageIcon, tag, false);
        this.onClickAction = onClickAction;
    }

    /**
     * Returns the message contained in this popup.
     *
     * @return the message contained in this popup
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * Returns the title of this popup message.
     *
     * @return the title of this popup message
     */
    public String getMessageTitle()
    {
        return messageTitle;
    }

    /**
     * Returns the component contained in this popup message.
     *
     * @return the component contained in this popup message.
     */
    public JComponent getComponent()
    {
        return component;
    }

    /**
     * Sets the component to be showed in this popup message.
     *
     * @param component the component to set
     */
    public void setComponent(JComponent component)
    {
        this.component = component;
    }

    /**
     * Returns the icon of this popup message.
     *
     * @return the icon of this popup message
     */
    public BufferedImageFuture getIcon()
    {
        return imageIcon;
    }

    /**
     * Sets the icon of this popup message.
     *
     * @param imageIcon the icon to set
     */
    public void setIcon(BufferedImageFuture imageIcon)
    {
        this.imageIcon = imageIcon;
    }

    /**
     * Returns the type of this popup message.
     *
     * @return the type of this popup message.
     */
    public int getMessageType()
    {
        return messageType;
    }

    /**
     * Sets the type of this popup message.
     *
     * @param messageType the type to set
     */
    public void setMessageType(int messageType)
    {
        this.messageType = messageType;
    }

    /**
     * Returns the object used to tag this <tt>PopupMessage</tt>.
     *
     * @return the object used to tag this <tt>PopupMessage</tt>
     */
    public Object getTag()
    {
        return tag;
    }

    /**
     * Returns whether this message is due to an error
     *
     * @return whether this message is due to an error
     */
    public boolean isError()
    {
        return isError;
    }

    /**
     * Sets the object used to tag this popup message.
     *
     * @param tag the object to set
     */
    public void setTag(Object tag)
    {
        this.tag = tag;
    }

    /**
     * Called when the popup is clicked.
     */
    public void onPopupClicked()
    {
        if (onClickAction != null)
        {
            // There is an action registered for when this toast is clicked,
            // so run it.
            new Thread(onClickAction, "ToastClickHandler").start();
        }
    }
}
