/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

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
    extends SIPCommDialog
    implements  ActionListener,
                HyperlinkListener,
                Skinnable
{
    private static final long serialVersionUID = 1L;

    /**
     * The <tt>Logger</tt> used by the <tt>ErrorDialog</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(ErrorDialog.class);

    /**
     * The maximum width that we allow message dialogs to have.
     */
    protected static final int MAX_MSG_PANE_WIDTH = ScaleUtils.scaleInt(340);

    /**
     * The maximum height that we allow message dialogs to have.
     */
    protected static final int MAX_MSG_PANE_HEIGHT = ScaleUtils.scaleInt(800);

    /**
     * The width of thin borders/gaps used in this dialog.
     */
    private static final int THIN_BORDER = ScaleUtils.scaleInt(10);

    /**
     * The width of thick borders/gaps used in this dialog.
     */
    private static final int THICK_BORDER = ScaleUtils.scaleInt(20);

    protected JButton okButton;
    protected JButton actionButton;

    protected final JLabel iconLabel
        = DesktopUtilActivator.getImage("service.gui.icons.ERROR_ICON")
        .getImageIcon()
        .addToLabel(new JLabel());

    protected StyledHTMLEditorPane htmlMsgEditorPane = new StyledHTMLEditorPane();

    private JTextArea stackTraceTextArea = new JTextArea();

    private JScrollPane stackTraceScrollPane = new JScrollPane();

    protected TransparentPanel infoMessagePanel = new TransparentPanel();

    private TransparentPanel messagePanel
        = new TransparentPanel(new BorderLayout());

    private TransparentPanel mainPanel
        = new TransparentPanel(new BorderLayout(THIN_BORDER, THIN_BORDER));

    /**
     * The main message of the error dialog (ignoring additional details and
     * stack trace).
     */
    private String message;

    /**
     * Load the "net.java.sip.communicator.SHOW_STACK_TRACE" property to
     * determine whether we should show stack trace in error dialogs.
     * Default is show.
     */
    private static final String showStackTraceDefaultProp
        = DesktopUtilActivator.getResources().getSettingsString(
            "net.java.sip.communicator.SHOW_STACK_TRACE");

    /**
     * Should we show stack trace.
     */
    private static final boolean showStackTrace =
            showStackTraceDefaultProp == null || Boolean.parseBoolean(showStackTraceDefaultProp);

    /**
     * The indicator which determines whether the details of the error are
     * currently shown.
     * <p>
     * The indicator is initially set to <tt>true</tt> because the constructor
     * {@link #ErrorDialog(Frame, String, String, Throwable)} calls
     * {@link #showOrHideDetails()} and thus <tt>ErrorDialog</tt> defaults to
     * not showing the details of the error.
     * </p>
     */
    private boolean detailsShown = true;

    /**
     * The type of this <tt>ErrorDialog</tt>.
     */
    public enum ErrorType
    {
        WARNING,
        ERROR,
        LOCATION
    }

    /**
     * The default <tt>ErrorDialog</tt> displays an error
     */
    private ErrorType type = ErrorType.ERROR;

    /**
     * An optional string representing a config option.  If present then a
     * checkbox will be shown at the bottom of this message asking the user if
     * they want to see this warning again.
     */
    private String configOption = null;

    /**
     * Checkbox that should be shown if this dialog is created with a config
     * option. When the user dismisses this dialog the config option will be
     * set to have the value of this checkbox
     */
    private JCheckBox checkBox = null;

    /**
     * The text to use for the 'ok' button
     */
    private String buttonText;

    /**
     * Initializes a new <tt>ErrorDialog</tt> with a specific owner
     * <tt>Frame</tt>, title and message to be displayed.
     *
     * @param owner the dialog owner
     * @param title the title of the dialog
     * @param message the message to be displayed
     */
    public ErrorDialog(Frame owner,
                       String title,
                       String message)
    {
        this(owner, title, message, (String)null);
    }

    /**
     * Initializes a new <tt>ErrorDialog</tt> with a specific owner
     * <tt>Frame</tt>, title and message to be displayed.
     *
     * @param owner the dialog owner
     * @param title the title of the dialog
     * @param message the message to be displayed
     * @param configOption an optional config location. If not null then a check
     *        box will be displayed which allows the user to say whether this
     *        dialog should ever be shown again
     */
    public ErrorDialog(Frame owner,
                       String title,
                       String message,
                       String configOption)
    {
        this(owner, title, message, configOption, null);
    }

    /**
     * Initializes a new <tt>ErrorDialog</tt> with a specific owner
     * <tt>Frame</tt>, title and message to be displayed.
     *
     * @param owner the dialog owner
     * @param title the title of the dialog
     * @param message the message to be displayed
     * @param configOption an optional config location. If not null then a check
     *        box will be displayed which allows the user to say whether this
     *        dialog should ever be shown again
     * @param buttonText the text to use for the 'OK' button
     */
    public ErrorDialog(Frame owner,
                       String title,
                       String message,
                       String configOption,
                       String buttonText)
    {
        super(owner, false);

        init(title, message, configOption, buttonText);
    }

    /**
     * Initializes a new <tt>ErrorDialog</tt> with a specific owner
     * <tt>Frame</tt>, title, error message to be displayed and the
     * <tt>Throwable</tt> associated with the error.
     *
     * @param owner the dialog owner
     * @param title the title of the dialog
     * @param message the message to be displayed
     * @param e the exception corresponding to the error
     */
    public ErrorDialog(Frame owner,
                       String title,
                       String message,
                       Throwable e)
    {
        this(owner, title, message);

        if (showStackTrace && e != null)
        {
            this.setTitle(title);

            this.htmlMsgEditorPane.setEditable(false);
            this.htmlMsgEditorPane.setOpaque(false);

            this.htmlMsgEditorPane.addHyperlinkListener(this);

            showOrHideDetails();

            this.infoMessagePanel.add(htmlMsgEditorPane, BorderLayout.SOUTH);

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.close();

            String stackTrace = sw.toString();

            try
            {
                sw.close();
            }
            catch (IOException ex)
            {
                //really shouldn't happen. but log anyway
                logger.error("Failed to close a StringWriter. ", ex);
            }

            this.stackTraceTextArea.setText(stackTrace);
        }
    }

    /**
     * Initializes a new <tt>ErrorDialog</tt> with a specific owner
     * <tt>Frame</tt>, title and message to be displayed and of a specific type.
     *
     * @param owner the dialog owner
     * @param title the title of the error dialog
     * @param message the message to be displayed
     * @param type the dialog type
     */
    public ErrorDialog(Frame owner,
                       String title,
                       String message,
                       ErrorType type)
    {
        this(owner, title, message);
        this.type = type;
        loadSkin();
    }

    /**
     * Initializes a new <tt>ErrorDialog</tt> with a specific owner
     * <tt>Frame</tt>, title and message to be displayed, text for the 'ok'
     * button and of a specific type.
     *
     * @param owner the dialog owner
     * @param title the title of the error dialog
     * @param message the message to be displayed
     * @param type the dialog type
     * @param buttonText the text to use for the 'OK' button
     */
    public ErrorDialog(Frame owner,
                       String title,
                       String message,
                       ErrorType type,
                       String buttonText)
    {
        this(owner, title, message, type);

        if (buttonText != null)
        {
            okButton.setText(buttonText);
        }
    }

    /**
     * Initializes a new <tt>ErrorDialog</tt> with a specific owner
     * <tt>Dialog</tt>, title and message to be displayed.
     *
     * @param title the title of the dialog
     * @param message the message to be displayed
     * @param owner the dialog owner
     */
    public ErrorDialog(String title,
                       String message,
                       Dialog owner)
    {
        this(title, message, (String)null, owner);
    }

    /**
     * Initializes a new <tt>ErrorDialog</tt> with a specific owner
     * <tt>Dialog</tt>, title and message to be displayed.
     *
     * @param title the title of the dialog
     * @param message the message to be displayed
     * @param configOption an optional config location. If not null then a check
     *        box will be displayed which allows the user to say whether this
     *        dialog should ever be shown again
     * @param owner the dialog owner
     */
    public ErrorDialog(String title,
                       String message,
                       String configOption,
                       Dialog owner)
    {
        this(title, message, configOption, null, owner);
    }

    /**
     * Initializes a new <tt>ErrorDialog</tt> with a specific owner
     * <tt>Frame</tt>, title and message to be displayed.
     *
     * @param title the title of the dialog
     * @param message the message to be displayed
     * @param configOption an optional config location. If not null then a check
     *        box will be displayed which allows the user to say whether this
     *        dialog should ever be shown again
     * @param buttonText the text to use for the 'OK' button
     * @param owner the dialog owner
     */
    public ErrorDialog(String title,
                       String message,
                       String configOption,
                       String buttonText,
                       Dialog owner)
    {
        super(owner, false);

        init(title, message, configOption, buttonText);
    }

    /**
     * Initializes a new <tt>ErrorDialog</tt> with a specific owner
     * <tt>Dialog</tt>, title and message to be displayed and of a specific type.
     *
     * @param title the title of the error dialog
     * @param message the message to be displayed
     * @param type the dialog type
     * @param owner the dialog owner
     */
    public ErrorDialog(String title,
                       String message,
                       ErrorType type,
                       Dialog owner)
    {
        this(title, message, owner);
        this.type = type;
        loadSkin();
    }

    /**
     * Initializes a new <tt>ErrorDialog</tt> with a specific owner
     * <tt>Dialog</tt>, title and message to be displayed, text for the 'ok'
     * button and of a specific type.
     *
     * @param title the title of the error dialog
     * @param message the message to be displayed
     * @param type the dialog type
     * @param buttonText the text to use for the 'OK' button
     * @param owner the dialog owner
     */
    public ErrorDialog(String title,
                       String message,
                       ErrorType type,
                       String buttonText,
                       Dialog owner)
    {
        this(title, message, type, owner);

        if (buttonText != null)
        {
            okButton.setText(buttonText);
        }
    }

    /**
     * Initializes this dialog.
     * @param title the title of the dialog
     * @param message the message to be displayed
     * @param configOption an optional config location. If not null then a
     *        check box will be displayed which allows the user to say whether
     *        this dialog should ever be shown again
     * @param buttonText the text to use for the 'OK' button
     */
    private void init(String title,
                      String message,
                      String configOption,
                      String buttonText)
    {
        this.message = replaceBreakWithNewline(message);

        if (buttonText != null)
        {
            this.buttonText = buttonText;
        }
        else
        {
            this.buttonText = DesktopUtilActivator.getResources().
                                                getI18NString("service.gui.OK");
        }

        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(
            THICK_BORDER, THICK_BORDER, THIN_BORDER, THICK_BORDER));

        if (showStackTrace)
        {
            this.stackTraceScrollPane.setBorder(BorderFactory.createLineBorder(
                iconLabel.getForeground()));

            this.stackTraceScrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        }
        else
        {
            // For regular errors, without a stack trace, we don't need the
            // user to be able to resize the dialog.
            setResizable(false);
        }

        this.setTitle(title);
        this.infoMessagePanel.setLayout(new BorderLayout());

        JTextArea messageArea = new JTextArea();
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        ScaleUtils.scaleFontAsDefault(messageArea);

        messageArea.setOpaque(false);
        messageArea.setEditable(false);
        messageArea.setText(this.message);

        //try to reevaluate the preferred size of the message pane.
        //(this is definitely not a neat way to do it ... but it works).
        messageArea.setSize(
                new Dimension(MAX_MSG_PANE_WIDTH, MAX_MSG_PANE_HEIGHT));
        messageArea.setPreferredSize(
                new Dimension(
                        MAX_MSG_PANE_WIDTH,
                        messageArea.getPreferredSize().height));
        messageArea.setForeground(
            new Color(DesktopUtilActivator.getResources().getColor(
                "service.gui.DARK_TEXT")));

        this.infoMessagePanel.add(messageArea, BorderLayout.CENTER);

        // Create the checkbox to set whether or not this should be displayed
        // again
        if (configOption != null)
        {
            this.configOption = configOption;

            // Although this string never changes, we can't fetch it at class
            // init time because the resource service may not be available then
            checkBox = new JCheckBox(DesktopUtilActivator.getResources().getI18NString("service.gui.DO_NOT_SHOW_AGAIN"));
            ScaleUtils.scaleFontAsDefault(checkBox);
            checkBox.setOpaque(false);
            checkBox.setSelected(DesktopUtilActivator.getConfigurationService()
                                       .user().getBoolean(configOption, false));

            checkBox.setForeground(
                new Color(DesktopUtilActivator.getResources().getColor(
                    "service.gui.DARK_TEXT")));

            infoMessagePanel.add(checkBox, BorderLayout.SOUTH);
        }

        okButton = new SIPCommBasicTextButton(this.buttonText);

        this.getRootPane().setDefaultButton(okButton);

        // Some dialogs have an action button, so create it in case, but set it invisible
        actionButton = new SIPCommBasicTextButton();
        actionButton.setVisible(false);

        this.stackTraceScrollPane.getViewport().add(stackTraceTextArea);
        this.stackTraceScrollPane.setPreferredSize(
            new Dimension(this.getWidth(), ScaleUtils.scaleInt(100)));

        this.okButton.addActionListener(this);

        iconLabel.setVerticalAlignment(SwingConstants.TOP);
        this.mainPanel.add(iconLabel, BorderLayout.WEST);

        this.messagePanel.add(infoMessagePanel, BorderLayout.NORTH);

        this.mainPanel.add(messagePanel, BorderLayout.CENTER);
        this.mainPanel.add(getButtonsPanel(), BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel);

        this.setMinimumSize(this.getPreferredSize());

        // Prevent the window from stealing focus if it is brought to the front.
        setAutoRequestFocus(false);

        // Error messages should always be on top
        this.setAlwaysOnTop(true);
    }

    /**
     * Creates and returns the panel containing the buttons for this dialog.
     *
     * @return The panel containing the buttons for this dialog.
     */
    protected TransparentPanel getButtonsPanel()
    {
        TransparentPanel buttonsPanel =
            new TransparentPanel(new FlowLayout(FlowLayout.CENTER));
        buttonsPanel.add(okButton);
        buttonsPanel.add(actionButton);

        return buttonsPanel;
    }

    /**
     * Shows if previously hidden or hides if previously shown the details of
     * the error. Called when the "more" link is clicked.
     */
    public void showOrHideDetails()
    {
        String startDivTag = "<div id=\"message\">";
        String endDivTag = "</div>";
        String msgString;

        detailsShown = !detailsShown;

        if(detailsShown)
        {
             msgString = startDivTag
                + " <p align=\"right\"><a href=\"\">&lt;&lt; Hide info</a></p>"
                + endDivTag;
             this.messagePanel.add(stackTraceScrollPane, BorderLayout.CENTER);
        }
        else
        {
             msgString = startDivTag
                + " <p align=\"right\"><a href=\"\">More info &gt;&gt;</a></p>"
                + endDivTag;
             this.messagePanel.remove(stackTraceScrollPane);
        }

        htmlMsgEditorPane.setText(msgString);

        this.messagePanel.revalidate();
        this.messagePanel.repaint();
        // restore default values for preferred size,
        // as we have resized its components let it calculate
        // that size
        setPreferredSize(null);
        this.pack();
    }

    /**
     * Shows the dialog.
     */
    public void showDialog()
    {
        // setVisible packs, positions and makes visible the dialog.
        logger.info("Displaying error dialog with title '" + getTitle() +
            "' and text '" + message + "'.");
        this.setVisible(true);
        this.toFront();
    }

    /**
     * Handles the <tt>ActionEvent</tt>. Depending on the user choice sets
     * the return code to the appropriate value.
     *
     * @param e the <tt>ActionEvent</tt> instance that has just been fired.
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton) e.getSource();

        if(button.equals(okButton))
        {
            if (checkBox != null)
            {
                DesktopUtilActivator.getConfigurationService().user()
                .setProperty(configOption, checkBox.isSelected());
            }
            this.dispose();
        }
    }

    /**
     * Close the ErrorDialog. This function is invoked when user
     * presses the Escape key.
     *
     * @param isEscaped Specifies whether the close was triggered by pressing
     * the escape key.
     */
    protected void close(boolean isEscaped)
    {
        this.okButton.doClick();
    }

    /**
     * Update the ErrorDialog when the user clicks on the hyperlink.
     *
     * @param e The event generated by the click on the hyperlink.
     */
    public void hyperlinkUpdate(HyperlinkEvent e)
    {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
            showOrHideDetails();
    }

    /**
     * Reloads icon.
     */
    public void loadSkin()
    {
        String icon =
            type == ErrorType.WARNING ? "service.gui.icons.WARNING_ICON" :
            type == ErrorType.ERROR ? "service.gui.icons.ERROR_ICON" :
                                      "service.gui.icons.LOCATION_ICON";

        DesktopUtilActivator.getImage(icon)
        .getImageIcon()
        .addToLabel(iconLabel);
    }

    @Override
    public void setModal(boolean modal)
    {
        super.setModal(modal);

        //modal dialogs should always be on top to avoid clashing with other
        // always on top windows.
        if (modal)
            setAlwaysOnTop(true);
    }

    /**
     * messageArea in the ErrorDialog changed from JEditorPane to JTextArea.
     * JTextArea shows HTML tags as they are without parsing them.
     * Here we replace the <br> tag with a newline to add line breaks to the
     * error message.
     *
     * @param htmlString text from a template
     * */
    static String replaceBreakWithNewline(String htmlString) {
        return htmlString == null ? htmlString :
                htmlString.replaceAll("<[bB][rR]\\s*?[/]?>", "\n");
    }
}
