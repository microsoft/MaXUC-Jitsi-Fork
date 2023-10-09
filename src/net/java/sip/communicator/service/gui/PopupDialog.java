/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.gui;

/**
 * A configurable popup dialog, that could be used from other services for
 * simple interactions with the user, throught the gui interface. This dialog
 * allows showing error, warning or info messages, prompting the user for simple
 * one field input or choice, or asking the user for certain confirmation.
 *
 * Three types of dialogs are differentiated: Message, Confirm and Input dialog.
 * Each of them has several show methods corresponging, allowing additional
 * specific configuration, like specifying or not a title, confirmation option
 * or initial value.
 *
 * @author Yana Stamcheva
 */
public interface PopupDialog extends ExportedWindow
{
    WindowID WINDOW_GENERAL_POPUP
        = new WindowID("GeneralPopupWindow");
    //
    // Option types
    //
    /** Type used for <code>showConfirmDialog</code>. */
    int         YES_NO_OPTION = 0;
    /** Type used for <code>showConfirmDialog</code>. */
    int         YES_NO_CANCEL_OPTION = 1;
    /** Type used for <code>showConfirmDialog</code>. */
    int         OK_CANCEL_OPTION = 2;

    //
    // Return values.
    //
    /** Return value from class method if YES is chosen. */
    int         YES_OPTION = 0;
    /** Return value from class method if NO is chosen. */
    int         NO_OPTION = 1;

    /*
     * Message types. Meant to be used by the UI implementation to determine
     * what icon to display and possibly what behavior to give based on the
     * type.
     */
    /** Used for error messages. */
    int  ERROR_MESSAGE = 0;
    /** Used for information messages. */
    int  INFORMATION_MESSAGE = 1;
    /** Used for warning messages. */
    int  WARNING_MESSAGE = 2;
    /** Used for questions. */
    int  QUESTION_MESSAGE = 3;

    /**
     * Shows an input dialog, where all options like title, type of message
     * etc., could be configured. The user will be able to choose from
     * <code>selectionValues</code>, where <code>null</code> implies the
     * users can input whatever they wish.
     * <code>initialSelectionValue</code> is the initial value to prompt
     * the user with.
     * It is up to the UI implementation to decide how best to represent the
     * <code>selectionValues</code>. In the case of swing per example it could
     * be a <code>JComboBox</code>, <code>JList</code> or
     * <code>JTextField</code>. The message type is meant to be used by the ui
     * implementation to determine the icon of the dialog.
     *
     * @param message  the <code>Object</code> to display
     * @param title    the <code>String</code> to display in the
     *          dialog title bar
     * @param messageType the type of message to be displayed:
     *                  <code>ERROR_MESSAGE</code>,
     *                  <code>INFORMATION_MESSAGE</code>,
     *                  <code>WARNING_MESSAGE</code>,
     *                  <code>QUESTION_MESSAGE</code>,
     *                  or <code>PLAIN_MESSAGE</code>
     *
     * @param selectionValues an array of <code>Object</code>s that
     *          gives the possible selections
     * @param initialSelectionValue the value used to initialize the input
     *                 field
     * @return user's input, or <code>null</code> meaning the user
     *          canceled the input
     */
    Object showInputPopupDialog(Object message, String title,
                                int messageType, Object[] selectionValues,
                                Object initialSelectionValue);

    /**
     * Shows an information-message dialog titled "Message".
     *
     * @param message  the <code>Object</code> to display
     */
    void showMessagePopupDialog(Object message);

    /**
     * Shows a dialog that displays a message using a default
     * icon determined by the <code>messageType</code> parameter.
     *
     * @param message   the <code>Object</code> to display
     * @param title     the title string for the dialog
     * @param messageType the type of message to be displayed:
     *                  <code>ERROR_MESSAGE</code>,
     *                  <code>INFORMATION_MESSAGE</code>,
     *                  <code>WARNING_MESSAGE</code>,
     *                  <code>QUESTION_MESSAGE</code>,
     *                  or <code>PLAIN_MESSAGE</code>
     */
    void showMessagePopupDialog(Object message,
                                String title, int messageType);

    /**
     * Shows a dialog where the number of choices is determined
     * by the <code>optionType</code> parameter.
     *
     * @param message   the <code>Object</code> to display
     * @param title     the title string for the dialog
     * @param optionType an int designating the options available on the dialog:
     *                  <code>YES_NO_OPTION</code>, or
     *                  <code>YES_NO_CANCEL_OPTION</code>
     * @return one of the YES_OPTION, NO_OPTION,.., XXX_OPTION, indicating the
     * option selected by the user
     */
    int showConfirmPopupDialog(Object message,
                               String title, int optionType);

    /**
     * Shows a dialog where the number of choices is determined
     * by the <code>optionType</code> parameter, where the
     * <code>messageType</code> parameter determines the icon to display.
     * The <code>messageType</code> parameter is primarily used to supply
     * a default icon for the dialog.
     *
     * @param message   the <code>Object</code> to display
     * @param title     the title string for the dialog
     * @param optionType an integer designating the options available
     *          on the dialog: <code>YES_NO_OPTION</code>,
     *          or <code>YES_NO_CANCEL_OPTION</code>
     * @param messageType an integer designating the kind of message this is;
     *                  <code>ERROR_MESSAGE</code>,
     *                  <code>INFORMATION_MESSAGE</code>,
     *                  <code>WARNING_MESSAGE</code>,
     *                  <code>QUESTION_MESSAGE</code>,
     *          or <code>PLAIN_MESSAGE</code>
     * @return one of the YES_OPTION, NO_OPTION,.., XXX_OPTION, indicating the
     * option selected by the user
     */
    int showConfirmPopupDialog(Object message, String title,
                               int optionType, int messageType);

}

