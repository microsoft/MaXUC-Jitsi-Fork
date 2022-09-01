// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import net.java.sip.communicator.util.Logger;

/**
 * An Accession-styled dialog box that shows a message to the user and gives
 * them a confirm and cancel button.
 *
 */
public class SIPCommConfirmDialog
    extends SIPCommDialog implements ActionListener
{
    private static final long serialVersionUID = 1L;

    private static final Logger sLog = Logger.getLogger(SIPCommConfirmDialog.class);

    /**
     * The resources service
     */
    protected final ResourceManagementService resources = DesktopUtilActivator.getResources();

    /**
     * Resource for the message panel.
     */
    String messageRes = null;

    /**
     * The config service
     */
    private final ConfigurationService config = DesktopUtilActivator.getConfigurationService();

    /**
     * The panel that contains the confirm and cancel buttons
     */
    protected TransparentPanel buttonsPanel
                      = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

    /**
     * The panel that contains the message to display
     */
    protected TransparentPanel messagePanel
                                     = new TransparentPanel(new BorderLayout());

    /**
     * The panel that contains the message panel and buttons panel
     */
    protected TransparentPanel mainPanel
                                = new TransparentPanel(new BorderLayout(10, 0));

    /**
     * The confirm button
     */
    protected SIPCommBasicTextButton confirmButton = new SIPCommBasicTextButton();

    /**
     * The cancel button
     */
    protected SIPCommBasicTextButton cancelButton = new SIPCommBasicTextButton();

    /**
     * The icon to display on the left of this dialog
     */
    protected final ImageIconFuture icon;

    protected final JLabel iconLabel;

    /**
     * The response the user gave. Only true if the user clicks the confirm
     * button
     */
    protected boolean response = false;

    /**
     * The default width that we set the message pane to.
     */
    protected static final int DEFAULT_MSG_PANE_WIDTH = ScaleUtils.scaleInt(485);

    /**
     * Construct a new confirm dialog
     *
     * @param titleRes the resource for the title text
     * @param messageRes the resource for the message text
     * @param confirmButtonRes the resource for the confirm button text.  If
     *                         null, this button will be omitted.
     * @param cancelButtonRes the resource for the cancel button text.  If
     *                        null, this button will be omitted.
     */
    public SIPCommConfirmDialog(String titleRes,
                                String messageRes,
                                String confirmButtonRes,
                                String cancelButtonRes)
    {
        this(titleRes,
             messageRes,
             confirmButtonRes,
             cancelButtonRes,
             null);
    }

    /**
     * Construct a new confirm dialog
     *
     * @param owner the owner of this dialog
     * @param titleRes the resource for the title text
     * @param messageRes the resource for the message text
     * @param confirmButtonRes the resource for the confirm button text.  If
     *                         null, this button will be omitted.
     * @param cancelButtonRes the resource for the cancel button text.  If
     *                        null, this button will be omitted.
     */
    public SIPCommConfirmDialog(Window owner,
                                String titleRes,
                                String messageRes,
                                String confirmButtonRes,
                                String cancelButtonRes)
    {
        this(owner,
             titleRes,
             messageRes,
             0,
             null,
             confirmButtonRes,
             cancelButtonRes,
             null);
    }

    /**
     * Construct a new confirm dialog
     *
     * @param titleRes the resource for the title text
     * @param messageRes the resource for the message text
     * @param confirmButtonRes the resource for the confirm button text.  If
     *                         null, this button will be omitted.
     * @param cancelButtonRes the resource for the cancel button text.  If
     *                        null, this button will be omitted.
     * @param cfgDontAskAgain if non-null, then a checkbox will appear in the
     *        dialog which asks the user if they want to see this dialog again.
     *        The result is stored in the config location
     */
    public SIPCommConfirmDialog(String titleRes,
                                String messageRes,
                                String confirmButtonRes,
                                String cancelButtonRes,
                                final String cfgDontAskAgain)
    {
        this(null,
             titleRes,
             messageRes,
             0,
             null,
             confirmButtonRes,
             cancelButtonRes,
             cfgDontAskAgain);
    }

    /**
     * Construct a new confirm dialog
     *
     * @param owner the owner of this dialog
     * @param titleRes the resource for the title text
     * @param messageRes the resource for the message text
     * @param scaledMessageWidth the scaled value to set as the width of the
     *        message pane.  If this is 0, the default will be used.
     * @param iconRes the resource for the dialog's icon.  If this is null, the
     *                default warning icon will be use.
     * @param confirmButtonRes the resource for the confirm button text.  If
     *                         null, this button will be omitted.
     * @param cancelButtonRes the resource for the cancel button text.  If
     *                        null, this button will be omitted.
     * @param cfgDontAskAgain if non-null, then a checkbox will appear in the
     *        dialog which asks the user if they want to see this dialog again.
     *        The result is stored in the config location
     */
    public SIPCommConfirmDialog(Window owner,
                                String titleRes,
                                String messageRes,
                                final int scaledMessageWidth,
                                String iconRes,
                                String confirmButtonRes,
                                String cancelButtonRes,
                                final String cfgDontAskAgain)
    {
        // 'isSaveSizeAndLocation' is set to 'owner == null' here so that
        // confirmation dialogs that don't have owners appear in the centre
        // of the main monitor screen and remember their location, whereas
        // confirmation dialogs that do have owners appear in the centre of the
        // owning window and don't remember their location (just like error
        // dialogs with owners) so that they appear in the centre of the
        // owning window again on next display.
        super(owner, owner == null);

        this.messageRes = messageRes;
        setTitle(resources.getI18NString(titleRes));

        setModalityType(DEFAULT_MODALITY_TYPE);

        mainPanel.setBorder(
            ScaleUtils.createEmptyBorder(10, 20, 10, 20));

        final JComponent messageArea = createMessagePanel();
        messagePanel.add(messageArea, BorderLayout.NORTH);

        int checkboxHeight = 0;

        if (cfgDontAskAgain != null)
        {
            String text = resources.getI18NString("service.gui.DO_NOT_SHOW_AGAIN");
            final JCheckBox checkbox = new JCheckBox(text);
            checkbox.setOpaque(false);

            checkbox.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    config.user().setProperty(cfgDontAskAgain, checkbox.isSelected());
                }
            });

            messagePanel.add(checkbox, BorderLayout.SOUTH);
            checkboxHeight = checkbox.getPreferredSize().height;
        }

        if (OSUtils.IS_MAC)
        {
            buttonsPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        }

        // Only add the confirm and/or cancel button(s) if we've got a resource
        // to get text to display on them.
        if (confirmButtonRes != null)
        {
            confirmButton.setText(resources.getI18NString(confirmButtonRes));
            confirmButton.addActionListener(this);
            buttonsPanel.add(confirmButton);
            getRootPane().setDefaultButton(confirmButton);
        }

        if (cancelButtonRes != null)
        {
            cancelButton.setText(resources.getI18NString(cancelButtonRes));
            cancelButton.addActionListener(this);
            buttonsPanel.add(cancelButton);
            if (confirmButtonRes == null)
            {
                sLog.debug("No confirm button - setting cancel button as default");
                getRootPane().setDefaultButton(cancelButton);
            }
        }

        setResizable(false);

        // If no icon resource is provided, use the warning icon.
        iconRes = (iconRes == null) ? "service.gui.icons.WARNING_ICON" : iconRes;
        icon = DesktopUtilActivator.getImage(iconRes).getImageIcon();
        iconLabel = icon.addToLabel(new JLabel());
        mainPanel.add(iconLabel, BorderLayout.WEST);

        mainPanel.add(messagePanel, BorderLayout.CENTER);
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        getContentPane().add(mainPanel);

        final int fCheckboxHeight = checkboxHeight;

        // Need to wait for the icon to resolve before we can pack the dialog.
        icon.onUiResolve(new Resolution<ImageIcon>()
        {
            @Override
            public void onResolution(ImageIcon resolved)
            {
                // This is a slight hack to ensure that the dialog is the right size for
                // the text (which will vary in length according to language).
                // Basically, we make sure that the messagePanel is the desired width.
                // We then call pack which causes the text in the messageArea to be
                // wrapped onto the correct number of lines.  The messageArea then has
                // the right height so can be used to update the size of the messagePanel.
                int messagePaneWidth = (scaledMessageWidth == 0) ?
                                    DEFAULT_MSG_PANE_WIDTH : scaledMessageWidth;
                messagePanel.setPreferredSize(new Dimension(messagePaneWidth, 100000));
                pack();
                messagePanel.setPreferredSize(new Dimension(messagePaneWidth,
                               messageArea.getPreferredSize().height + fCheckboxHeight));
                pack();
            }
        });
    }

    protected JComponent createMessagePanel()
    {
        JEditorPane messageArea = new JEditorPane();
        ScaleUtils.scaleFontAsDefault(messageArea);

        /*
         * Make JEditorPane respect our default font because we will be using it
         * to just display text.
         */
        messageArea.putClientProperty(
                JEditorPane.HONOR_DISPLAY_PROPERTIES,
                true);

        messageArea.setOpaque(false);
        messageArea.setEditable(false);
        messageArea.setContentType("text/html");
        messageArea.setText("<html><body><p align=\"left\" >" +
                            getMessageString() +
                            "</p></body></html>");

        messagePanel.add(messageArea, BorderLayout.NORTH);
        return(messageArea);
    }

    /**
     * Returns an internationalised string for the message text for use in the
     * dialog main panel.
     *
     * Defaults to getting the internationalised string from the resources
     * service using the {@link #messageRes messageRes} field.
     *
     * Override in a subclass if this default behaviour is not correct
     * e.g. your message string has parameters.
     * @return an internationalised string for the dialog message
     */
    protected String getMessageString()
    {
        return resources.getI18NString(messageRes);
    }

    @Override
    public void actionPerformed(ActionEvent evt)
    {
        if (evt.getSource().equals(cancelButton))
        {
            setVisible(false);
            dispose();
        }
        else if (evt.getSource().equals(confirmButton))
        {
            response = true;
            setVisible(false);
            dispose();
        }
    }

    /**
     * Shows the dialog and returns true if the user confirmed the action, false
     * otherwise
     *
     * @return true if the user pressed the confirm button, false otherwise
     */
    public boolean showDialog()
    {
        // This is a modal dialog so setVisible does not return until the dialog
        // is dismissed. Hence we will only return the response when the dialog
        // is dismissed.
        setVisible(true);
        return response;
    }
}
