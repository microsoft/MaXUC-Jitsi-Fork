// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil.plaf;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.*;

/**
 * A utility class for showing a pop-up with a list of options for the user to
 * select.  This is intended to replace JOPtionPane.showInputDialog as it uses
 * a more appropriate UI.
 */
public class SIPCommOptionsDialog extends SIPCommDialog
{
    private static final long serialVersionUID = 1L;

    /**
     * The object selected by the user - null if the dialog was cancelled
     */
    private Object mSelectedObject = null;

    /**
     * Create but do not show an options dialog
     *
     * @param options The options to display in the combo box
     * @param messageRes The resource of the message to show
     * @param titleRes The title to display
     * @param owner The owning dialog - used for positioning purposes
     */
    private SIPCommOptionsDialog(Object[] options,
                                 String messageRes,
                                 String titleRes,
                                 Dialog owner)
    {
        super(owner, false);

        ResourceManagementService res = DesktopUtilActivator.getResources();

        // Set up the dialog
        setLocation(owner.getLocation());
        setModal(true);

        if(ConfigurationUtils.isCallAlwaysOnTop())
        {
            setAlwaysOnTop(true);
        }

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle(res.getI18NString(titleRes));
        setResizable(false);
        setMinimumSize(new ScaledDimension(200, 175));

        // Set the style of the text area.
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setOpaque(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(textArea.getFont().deriveFont(Font.BOLD));
        textArea.setText(res.getI18NString(messageRes));
        textArea.setAlignmentX(Component.CENTER_ALIGNMENT);

        JComboBox<Object> comboBox = new JComboBox<>(options);
        add(comboBox);

        // Create the 'OK' and 'cancel' buttons and add them to a panel.
        JButton okButton = new SIPCommBasicTextButton(res.getI18NString("service.gui.OK"));
        JButton cancelButton = new SIPCommBasicTextButton(res.getI18NString("service.gui.CANCEL"));

        JPanel buttonPanel = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));

        if (OSUtils.IS_MAC)
        {
            buttonPanel.add(cancelButton);
            buttonPanel.add(okButton);
        }
        else
        {
            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);
        }

        JPanel southPanel = new TransparentPanel(new BorderLayout());
        southPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Create a main panel for the window and add the other panels to it.
        TransparentPanel mainPanel = new TransparentPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(ScaleUtils.createEmptyBorder(10, 10, 0, 10));

        mainPanel.add(textArea);
        mainPanel.add(comboBox);
        mainPanel.add(southPanel);
        add(mainPanel);

        // Set the names and hotkeys for the buttons, and the default button.
        okButton.setName(res.getI18NString("service.gui.OK"));
        cancelButton.setName(res.getI18NString("service.gui.CANCEL"));
        okButton.setMnemonic(res.getI18nMnemonic("service.gui.OK"));
        cancelButton.setMnemonic(res.getI18nMnemonic("service.gui.CANCEL"));

        okButton.addActionListener(e ->
        {
            mSelectedObject = comboBox.getSelectedItem();
            setVisible(false);
        });

        cancelButton.addActionListener(e ->
        {
            mSelectedObject = null;
            setVisible(false);
        });

        // Set both buttons to be the same size (using the largest 'preferred
        // size').
        if (okButton.getPreferredSize().width >
            cancelButton.getPreferredSize().width)
        {
            cancelButton.setPreferredSize(okButton.getPreferredSize());
        }
        else
        {
            okButton.setPreferredSize(cancelButton.getPreferredSize());
        }
    }

    /**
     * Show a dialog with a combo-box list of options for the user to select one
     * Similar to JOptionPane.showInputDialog but with an appropriate UI.
     * @param titleRes The resource of the title of the dialog
     * @param messageRes The resource of the message to be displayed in the dialog
     * @param options The list of options to be presented to the user
     * @param owner The owning dialog - used for positioning
     * @param forceWindowToTop If true enables alwaysOnTop for the new dialog
     * @param excludeModality If true, the dialog will not be blocked by modal
     * dialogs.
     * @return the selected option or null if the dialog was cancelled
     */
    public static Object showInputDialog(String titleRes,
                                         String messageRes,
                                         Object[] options,
                                         Dialog owner,
                                         boolean forceWindowToTop,
                                         boolean excludeModality)
    {
        final SIPCommOptionsDialog dialog =
                 new SIPCommOptionsDialog(options, messageRes, titleRes, owner);

        // Add a listener so that if the owner is hidden then we can hide this.
        if (owner != null)
        {
            // The modality of this dialog does not prevent the user from
            // selecting another different contact from the parent dialog,
            // resulting in multiple instances of this dialog. To get around
            // this, disable parent now and re-enable when this dialog is no
            // longer visible
            owner.setEnabled(false);

            owner.addComponentListener(new ComponentAdapter()
            {
                @Override
                public void componentHidden(ComponentEvent e)
                {
                    dialog.setVisible(false);
                    dialog.dispose();
                }
            });
        }

        if (forceWindowToTop)
        {
            dialog.setAlwaysOnTop(true);
        }

        dialog.setExcludeModality(excludeModality);

        // Due to modality, this is blocking until the dialog is no longer
        // visible
        dialog.setVisible(true);

        if (owner != null)
        {
            owner.setEnabled(true);
        }

        return dialog.mSelectedObject;
    }
}
