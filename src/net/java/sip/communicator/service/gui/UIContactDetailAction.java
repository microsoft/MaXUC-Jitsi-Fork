/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

/**
 * Defines an action for an <tt>UIContactDetail</tt>.
 *
 * @author Yana Stamcheva
 */
public interface UIContactDetailAction
{
    /**
     * Indicates this action is executed for the given <tt>UIContactDetail</tt>.
     *
     * @param contactDetail the <tt>UIContactDetail</tt> for which this action
     * is performed
     * @param x the x coordinate of the action
     * @param y the y coordinate of the action
     */
    void actionPerformed(UIContactDetail contactDetail, int x, int y);
}
