// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.swing.TransparentPanel;

import net.java.sip.communicator.service.analytics.AnalyticsParameter;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.util.AccessibilityUtils;
import net.java.sip.communicator.util.Logger;

/**
 * Creates dialogs displayed to the user when they are starting / being invited
 * to a conference or receiving a call.  The class itself isn't a dialog, as
 * the type of dialog that is created varies depending on whether we are
 * running on Mac.
 */
public abstract class AlertDialogCreator implements ActionListener
{
    private static final Logger sLog = Logger.getLogger(AlertDialogCreator.class);

    /**
     * Text color for the caller's name and number in an incoming call.
     */
    protected static final Color TEXT_COLOR = new Color(DesktopUtilActivator
        .getResources().getColor("service.gui.LIGHT_TEXT"));

    /**
     * Background color for windows incoming call and invite.
     */
    protected static final Color BKG_COLOR = new Color(DesktopUtilActivator
        .getResources().getColor("refresh.gui.MAIN_COLOR"));

    /**
     * The image loader service
     */
    protected static final ImageLoaderService sImageLoaderService =
                                   DesktopUtilActivator.getImageLoaderService();

    /**
     * The resource management service.
     */
    protected static final ResourceManagementService resources =
                                            DesktopUtilActivator.getResources();

    /**
     * The start/join button name.
     */
    protected static final String OK_BUTTON_NAME = "OkButton";

    /**
     * The reject/cancel button name.
     */
    protected static final String CANCEL_BUTTON_NAME = "CancelButton";

    /**
     * The minimum width of the center panel of the dialog
     */
    private static final int MIN_CENTER_WIDTH = ScaleUtils.scaleInt(180);

    /**
     * The width of thin borders/gaps used in this dialog.
     */
    private static final int THIN_BORDER = ScaleUtils.scaleInt(2);

    /**
     * The width of thick borders/gaps used in this dialog.
     */
    private static final int THICK_BORDER = ScaleUtils.scaleInt(4);

    /**
     * The size that the icon should be scaled to.
     */
    protected static final int ICON_SIZE = ScaleUtils.scaleInt(60);

    /**
     * The window to display.
     */
    protected final Window alertWindow;

    /**
     * The type of dialog, used to find resources (i.e. strings and images)
     * specific to this dialog.  For example,
     * "service.gui.<mResourceType>.ACCEPT" is used to get the string for the
     * "ok" button.
     */
    protected final String mResourceType;

    /**
     * The central panel that contains the heading, description and CRM button
     * is appropriate.
     */
    protected JPanel mCenterPanel;

    /**
     * Creates an instance of <tt>AlertDialogCreator</tt>.
     *
     * @param buttonResourceType The type of dialog, used to find resources
     * (i.e. strings and images) specific to this dialog.  For example,
     * "service.gui.<mResourceType>.ACCEPT" is used to get the string for the
     * "ok" button.
     */
    public AlertDialogCreator(String buttonResourceType)
    {
        mResourceType = buttonResourceType;

        SIPCommFrame frame = new SIPCommFrame(false, BKG_COLOR, BKG_COLOR);

        // Get rid of the title bar and border but still allow the user to
        // drag the frame to reposition it on the screen.
        frame.setDraggableUndecorated();
        alertWindow = frame;

        alertWindow.setAlwaysOnTop(true);

        // Prevent the window from being blocked by modal dialogs - since we
        // are always-on-top, we don't want to appear over a modal dialog but
        // be unable to be moved away from it.
        alertWindow.setModalExclusionType(
                                 Dialog.ModalExclusionType.APPLICATION_EXCLUDE);

        AccessibilityUtils.setName(alertWindow, getDescriptionText());

        // We previously used setFocusableWindowState(false) on this window, specifically mentioning
        // issues with the dialog stealing focus. However it is important that the dialog is
        // focusable for accessibility reasons (so e.g. users can Alt + Tab to it and answer/reject
        // with the keyboard).  Instead, we stop it stealing focus with the hack in
        // AbstractUIServiceImpl.showStackableAlertWindow().
    }

    /**
     * Initializes all components in this panel.
     */
    protected void initComponents()
    {
        // Treat the panel as 3 separate vertical sections - using a Border
        // layout would mean we don't have to keep inserting empty panels
        // between sections, but it leads to other problems with the UI.
        JPanel mainPanel = new TransparentPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

        // Add a grey border to the popup.
        mainPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 0));

        // Create the icon on the left of the dialog.
        JLabel iconLabel = createIconLabel();
        JPanel iconLabelPanel = new TransparentPanel();
        iconLabelPanel.setBorder(ScaleUtils.createEmptyBorder(
            THIN_BORDER, THIN_BORDER, 0, 0));
        iconLabelPanel.add(iconLabel);

        mainPanel.add(iconLabelPanel);

        // Create the panel in the center of the dialog.
        mCenterPanel = new TransparentPanel();
        mCenterPanel.setBorder(ScaleUtils.createEmptyBorder(
            THICK_BORDER, 0, THICK_BORDER, 0));
        mCenterPanel.setLayout(new BoxLayout(mCenterPanel, BoxLayout.Y_AXIS));

        // Create the heading to be added to the center panel
        JComponent headingComponent = createHeadingComponent();

        // Create the description to be added to the center panel
        JComponent descriptionComponent = createDescriptionComponent();

        // Set the width of the center panel to either be the default minimum,
        // or the wider of the heading and description components, whichever is
        // larger.
        Dimension headingDim = headingComponent.getPreferredSize();
        Dimension descriptionDim = descriptionComponent.getPreferredSize();

        int headingWidth = headingDim.width;
        double descriptionWidth = descriptionDim.getWidth();
        double prefWidth =
            headingWidth > descriptionWidth ? headingWidth : descriptionWidth;
        prefWidth = prefWidth > MIN_CENTER_WIDTH ? prefWidth : MIN_CENTER_WIDTH;

        headingDim.setSize(prefWidth, headingDim.getHeight());
        headingComponent.setPreferredSize(headingDim);

        descriptionDim.setSize(prefWidth, descriptionDim.getHeight());
        descriptionComponent.setPreferredSize(descriptionDim);

        // Create panels for the heading and description and add them to the
        // center panel then add the center panel to the main panel
        JPanel headingLabelPanel = new TransparentPanel(new BorderLayout());
        headingLabelPanel.add(headingComponent, BorderLayout.CENTER);
        mCenterPanel.add(headingLabelPanel);

        JPanel descriptionComponentPanel = new TransparentPanel(new BorderLayout());
        descriptionComponentPanel.add(descriptionComponent, BorderLayout.CENTER);
        mCenterPanel.add(descriptionComponentPanel);

        // Add spacing between elements.
        mainPanel.add(createEmptyPanel());

        mainPanel.add(mCenterPanel);

        TransparentPanel buttonsPanel = new TransparentPanel();
        buttonsPanel.setLayout(new BorderLayout(0, 0));
        buttonsPanel.setBorder(ScaleUtils.createEmptyBorder(
                THIN_BORDER, 0, THIN_BORDER, THIN_BORDER));

        // Create the OK and Cancel buttons
        JButton okButton = createOkButton();
        JComponent cancelButton = createCancelButton();
        cancelButton.setForeground(Color.WHITE);

        // Set the width of the OK and cancel buttons to be the width of the
        // wider of the 2
        Dimension okDim = okButton.getPreferredSize();
        Dimension cancelDim = cancelButton.getPreferredSize();

        double okWidth = okDim.getWidth();
        double cancelWidth = cancelDim.getWidth();
        Dimension prefDim = okWidth > cancelWidth ? okDim : cancelDim;

        okButton.setPreferredSize(prefDim);
        cancelButton.setPreferredSize(prefDim);

        // Create panels for the buttons and add them to the buttons panel and
        // add that to the main panel
        JPanel okButtonPanel =
            new TransparentPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        okButtonPanel.add(okButton);
        buttonsPanel.add(okButtonPanel, BorderLayout.PAGE_START);

        JPanel cancelButtonPanel =
            new TransparentPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        cancelButtonPanel.add(cancelButton);
        buttonsPanel.add(cancelButtonPanel, BorderLayout.PAGE_END);

        // Add spacing between elements.
        mainPanel.add(createEmptyPanel());

        mainPanel.add(buttonsPanel);

        alertWindow.add(mainPanel);
    }

    /**
     * Returns a new SIPCommSnakeButton
     *
     * @param textRes the resource for the text to display on the button
     * @param imageRes the resource to use for the images used to construct the
     * button
     * @param name the name used to identify the button
     * @param actionListener the action listener to call back to when the
     * button is clicked.
     *
     * @return the new SIPCommSnakeButton
     */
    protected SIPCommSnakeButton createSIPCommSnakeButton(String textRes,
                                                          String imageRes,
                                                          String name,
                                                          ActionListener actionListener)
    {
        SIPCommSnakeButton button =
            new SIPCommSnakeButton(resources.getI18NString(textRes), imageRes, true);
        button.addActionListener(actionListener);
        button.setName(name);

        return button;
    }

    /**
     * Shows this dialog.
     */
    public void show()
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(() -> DesktopUtilActivator.getUIService().showStackableAlertWindow(alertWindow));
        }
        else
        {
            DesktopUtilActivator.getUIService().showStackableAlertWindow(alertWindow);
        }
    }

    /**
     * Disposes this window.
     */
    public void dispose()
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(() -> alertWindow.dispose());
        }
        else
        {
            alertWindow.dispose();
        }
    }

    /**
     * @return true if this dialog is visible
     */
    public boolean isVisible()
    {
        return alertWindow != null && alertWindow.isVisible();
    }

    /**
     * Handles <tt>ActionEvent</tt>s triggered by pressing the call or the
     * hangup buttons.
     * @param e The <tt>ActionEvent</tt> to handle.
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton) e.getSource();
        String buttonName = button.getName();

        sLog.info("User pressed " + buttonName);

        String answeredWithAnalyticParam = ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0) ?
            AnalyticsParameter.VALUE_MOUSE :
            AnalyticsParameter.VALUE_KEYBOARD;

        if (buttonName.equals(OK_BUTTON_NAME))
        {
            okButtonPressed(answeredWithAnalyticParam);
        }
        else if (buttonName.equals(CANCEL_BUTTON_NAME))
        {
            cancelButtonPressed(answeredWithAnalyticParam);
        }

        // Once the user has clicked a button we need to close the dialog.
        dispose();
    }

    /**
     * @return the icon label
     */
    protected JLabel createIconLabel()
    {
        JLabel iconLabel = new JLabel();
        getIconImage().addToLabel(iconLabel);

        return iconLabel;
    }

    /**
     * @return the heading component
     */
    protected JComponent createHeadingComponent()
    {
        JPanel headingPanel =
            new TransparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

        JLabel headingLabel = createHeadingLabel(getHeadingText());
        headingPanel.add(headingLabel);

        return headingPanel;
    }

    /**
     * @return the heading label
     */
    protected JLabel createHeadingLabel(String headingText)
    {
        JLabel headingLabel = new JLabel(headingText);
        headingLabel.setFont(new JLabel().getFont().deriveFont(
            Font.BOLD, ScaleUtils.getScaledFontSize(12.5f)));
        headingLabel.setForeground(TEXT_COLOR);

        return headingLabel;
    }

    /**
     * @return the description label
     */
    protected JLabel createDescriptionLabel(String text)
    {
        JLabel descriptionLabel = new JLabel(text);
        descriptionLabel.setFont(new JLabel().getFont().deriveFont(
            Font.PLAIN, ScaleUtils.getScaledFontSize(12.5f)));
        descriptionLabel.setForeground(TEXT_COLOR);

        return descriptionLabel;
    }

    /**
     * @return the description component
     */
    protected abstract JComponent createDescriptionComponent();

    /**
     * @return the icon image
     */
    protected abstract ImageIconFuture getIconImage();

    /**
     * @return the text for the heading label
     */
    protected abstract String getHeadingText();

    /**
     * @return the text for the description label
     */
    protected abstract String getDescriptionText();

    /**
     * @return the extra text for the description label, may be NULL
     */
    protected String getExtraText()
    {
        return null;
    }

    /**
     * @return the OK button
     */
    protected abstract JButton createOkButton();

    /**
     * @return the cancel button
     */
    protected abstract JComponent createCancelButton();

    /**
     * Called when the ok button is pressed.
     *
     * @param answeredWithAnalyticParam - value to use in analytics for the
     *        AnalyticsParameter.NAME_DIALOG_ANSWERED_WITH param.
     */
    public abstract void okButtonPressed(String answeredWithAnalyticParam);

    /**
     * Called when the cancel button is pressed.
     *
     * @param answeredWithAnalyticParam - value to use in analytics for the
     *        AnalyticsParameter.NAME_DIALOG_ANSWERED_WITH param.
     */
    public abstract void cancelButtonPressed(String answeredWithAnalyticParam);

    /**
     * @return An empty panel to use as a spacing between the columns of the
     * alert.
     */
    private JPanel createEmptyPanel()
    {
        Dimension spacing = new Dimension(THICK_BORDER, 0);

        JPanel emptyPanel = new JPanel();
        emptyPanel.setPreferredSize(spacing);
        emptyPanel.setMinimumSize(spacing);
        emptyPanel.setMaximumSize(spacing);

        return emptyPanel;
    }

    protected JPanel getCenterPanel()
    {
        return mCenterPanel;
    }
}
