/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import org.jitsi.service.resources.ResourceManagementService;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.service.wispaservice.*;

/**
 * Implements a <tt>JDialog</tt> which displays an error message and,
 * optionally, a <tt>Throwable</tt> stack trace. <tt>ErrorDialog</tt> has an OK
 * button which dismisses the message and a link to display the
 * <tt>Throwable</tt> stack trace upon request if available.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 * @author Lyubomir Marinov
 */
public class ErrorDialog
{
    /**
     * The <tt>Logger</tt> used by the <tt>ErrorDialog</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(ErrorDialog.class);

    private final ResourceManagementService resources =
        DesktopUtilActivator.getResources();

    private final WISPAService wispaService =
        DesktopUtilActivator.getWISPAService();

    /**
     * The title of the error dialog
     */
    private String title = null;

    /**
     * The main message of the error dialog (ignoring additional details and
     * stack trace).
     */
    private String message = null;

    /**
     * An optional string representing a config option. If present then a
     * checkbox will be shown at the bottom of this message asking the user if
     * they want to see this warning again.
     */
    private String configOption = null;

    /**
     * The text to use for the 'ok' button
     */
    private String buttonText = resources.getI18NString("service.gui.OK");

    /**
     * Whether the app should do anything when the error is dismissed
     */
    private OnDismiss dismissAction;

    public enum OnDismiss
    {
        DO_NOTHING, FORCE_EXIT, FORCE_LOGOUT_RESTART
    }

    /**
     * Icon to display on the error dialog
     */
    private Icon icon = Icon.ERROR_ICON;

    public enum Icon
    {
        ERROR_ICON, SUCCESS_ICON
    }

    /**
     * Whether to show error as in-app modal popup. Is a secondary electron
     * window when false.
     */
    private boolean modal = false;

    /**
     * Initializes a new <tt>ErrorDialog</tt> with title and message to be
     * displayed.
     *
     * @param title   the title of the dialog
     * @param message the message to be displayed
     */
    public ErrorDialog(String title, String message)
    {
        this(title, message, OnDismiss.DO_NOTHING);
    }

    /**
     * Initializes a new <tt>ErrorDialog</tt> with title, error message to be
     * displayed and action when dialog is dismissed
     *
     * @param title     the title of the dialog
     * @param message   the message to be displayed
     * @param dismissAction whether to do anything when dismissed
     */
    public ErrorDialog(String title, String message, OnDismiss dismissAction)
    {
        this.title = title;
        this.message = replaceHtmlTags(message);
        this.dismissAction = dismissAction;
    }

    public void setConfigOption(String configOption)
    {
        this.configOption = configOption;
    }

    public void setButtonText(String buttonText)
    {
        this.buttonText = buttonText;
    }

    public void setIcon (Icon icon)
    {
        this.icon = icon;
    }

    public void setModal(boolean modal)
    {
        this.modal = modal;
    }

    public void showDialog()
    {
        WISPAAction wispaAction;
        Object wispaActionData;

        logger.debug("Show error message to user with title: " + this.title);

        ErrorInfo errorInfo =
            new ErrorInfo(title, message, configOption, buttonText, dismissAction, icon);

        if (this.modal)
        {
            wispaAction = WISPAAction.MOTION;
            wispaActionData = new WISPAMotion(WISPAMotionType.ERROR, errorInfo);
        }
        else
        {
            wispaAction = WISPAAction.NOTION;
            wispaActionData = new WISPANotion(WISPANotionType.ERROR, errorInfo);
        }

        if (wispaService != null)
        {
            wispaService.notify(WISPANamespace.EVENTS, wispaAction,
                wispaActionData);
        }
        else
        {
            logger.error("Cannot show error - WISPAService is null.");
        }
    }

    /**
     * Our translated strings have a lot of <br> tags because we used to parse
     * HTML when displaying them. As they are impractical to remove in all languages,
     * we have a hack here to replace the <br> tag with a newline to add line breaks
     * to the error message. Also, have created a specific tag <spc> which is to be
     * replaced with a space as spaces in the translation files pose the risk of being
     * removed. The elements that use this tag are concatenated into one string so
     * needed for formatting.
     * Other HTML tags have been manually removed to best effort.
     *
     * @param htmlString text from a template
     */
    static String replaceHtmlTags(String htmlString)
    {
        return htmlString == null ? htmlString
            : htmlString.replaceAll("<[bB][rR]\\s*?[/]?>", "\n").replace("<spc>", " ");
    }
}