/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
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
     * Shows an alert dialog with the given title and message.
     *
     * @param title the title of the dialog
     * @param message the message to be displayed
     */
    void showAlertDialog(String title,
                         String message);
}
