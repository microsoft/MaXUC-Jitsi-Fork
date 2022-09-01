/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

/**
 * The <tt>AlertUIService</tt> is a service that allows to show error messages
 * and warnings.
 *
 * @author Yana Stamcheva
 */
public interface AlertUIService
{
    /**
     * Indicates that the OK button is pressed.
     */
    int OK_RETURN_CODE = 0;

    /**
     * Indicates that the Cancel button is pressed.
     */
    int CANCEL_RETURN_CODE = 1;

    /**
     * Indicates that the OK button is pressed and the Don't ask check box is
     * checked.
     */
    int OK_DONT_ASK_CODE = 2;

    /**
     * Shows an alert dialog with the given title and message.
     *
     * @param title the title of the dialog
     * @param message the message to be displayed
     */
    void showAlertDialog(String title,
                         String message);

    /**
     * Shows an alert dialog with the given title message and exception
     * corresponding to the error.
     *
     * @param title the title of the dialog
     * @param message the message to be displayed
     * @param e the exception corresponding to the error
     */
    void showAlertDialog(String title,
                         String message,
                         Throwable e);
}
