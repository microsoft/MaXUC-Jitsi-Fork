/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.customcontactactions;

import net.java.sip.communicator.service.protocol.*;

import org.jitsi.service.resources.*;

/**
 * A custom contact action, used to define an action that can be represented in
 * the contact list entry in the user interface.
 *
 * @author Damian Minkov
 * @author Yana Stamcheva
 */
public interface ContactAction<T>
{
    /**
     * Invoked when an action occurs.
     *
     * @param actionSource the source of the action
     * @param x the x coordinate of the action
     * @param y the y coordinate of the action
     */
    void actionPerformed(T actionSource, int x, int y)
    ;

    /**
     * The icon used by the UI to visualize this action.
     * @return the button icon.
     */
    BufferedImageFuture getIcon();

    /**
     * The icon used by the UI to visualize the roll over state of the button.
     * @return the button icon.
     */
    BufferedImageFuture getRolloverIcon();

    /**
     * The icon used by the UI to visualize the pressed state of the button
     * @return the button icon.
     */
    BufferedImageFuture getPressedIcon();

    /**
     * Returns the tool tip text of the component to create for this contact
     * action.
     *
     * @return the tool tip text of the component to create for this contact
     * action
     */
    String getToolTipText();

    /**
     * Indicates if this action is visible for the given <tt>actionSource</tt>.
     *
     * @param actionSource the action source for which we're verifying the
     * action.
     * @return <tt>true</tt> if the action should be visible for the given
     * <tt>actionSource</tt>, <tt>false</tt> - otherwise
     */
    boolean isVisible(T actionSource);
}
