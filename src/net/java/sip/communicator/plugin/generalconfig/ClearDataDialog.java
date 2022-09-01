// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.generalconfig;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.util.*;

/**
 * An Accession-styled dialog box that shows a message to the user and gives
 * them a confirm and cancel button.  But also has some radio buttons!
 *
 */
public class ClearDataDialog
    extends SIPCommConfirmDialog implements ActionListener
{
    private static final long serialVersionUID = 1L;

    private static final String TITLE_RES      = "service.gui.RESET_DIALOG_TITLE";
    private static final String MSG_ABOVE_RES  = "service.gui.RESET_DIALOG_TEXT";
    private static final String MSG_BELOW_RES  = "service.gui.RESET_DIALOG_RELAUNCH";
    private static final String CONFIRM_RES    = "service.gui.RESET_DIALOG_CONFIRM";
    private static final String CANCEL_RES     = "service.gui.CANCEL";
    private static final String RESET_USER_RES = "service.gui.RESET_DIALOG_USER";
    private static final String RESET_ALL_RES  = "service.gui.RESET_DIALOG_ALL";

    /**
     * The <tt>Logger</tt> used by this <tt>ClearDataDialog</tt>
     * instance for logging output.
     */
    private static final Logger logger = Logger.getLogger(ClearDataDialog.class);

    // The clear user/clear all data scope radio buttons.
    private JRadioButton clearUserButton;
    private JRadioButton clearAllButton;

    /**
     * Construct a new clear data dialog
     */
    public ClearDataDialog()
    {
        super(TITLE_RES, null, CONFIRM_RES, CANCEL_RES);
    }

    protected JComponent createMessagePanel()
    {
        TransparentPanel messagePanel = new TransparentPanel(new BorderLayout());

        JEditorPane messageAreaAbove =
                        createTextPanel(resources.getI18NString(MSG_ABOVE_RES));

        clearUserButton = new SIPCommRadioButton(resources.getI18NString(RESET_USER_RES));
        clearAllButton = new SIPCommRadioButton(resources.getI18NString(RESET_ALL_RES));
        clearUserButton.setSelected(true);

        // Add the radio buttons that all the user to select clear user or
        // clear all.
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(clearUserButton);
        buttonGroup.add(clearAllButton);

        JPanel radioButtonPanel = new TransparentPanel();
        BoxLayout layout = new BoxLayout(radioButtonPanel, BoxLayout.PAGE_AXIS);
        radioButtonPanel.setLayout(layout);

        radioButtonPanel.add(clearUserButton);
        radioButtonPanel.add(clearAllButton);

        JEditorPane messageAreaBelow =
                        createTextPanel(resources.getI18NString(MSG_BELOW_RES));

        messagePanel.add(messageAreaAbove, BorderLayout.PAGE_START);
        messagePanel.add(radioButtonPanel, BorderLayout.LINE_START);
        messagePanel.add(messageAreaBelow, BorderLayout.PAGE_END);

        initListeners();

        return messagePanel;
    }

    /**
     * @param text the text to display in the text panel.
     * @return the created text panel
     */
    private JEditorPane createTextPanel(String text)
    {
        JEditorPane textPanel = new JEditorPane();

        // Make JEditorPane respect our default font because we will be using it
        // to just display text.
        textPanel.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        textPanel.setOpaque(false);
        textPanel.setEditable(false);
        textPanel.setContentType("text/html");
        textPanel.setText("<html><body>" + text + "</body></html>");
        textPanel.setBorder(ScaleUtils.createEmptyBorder(10, 0, 10, 0));

        return textPanel;
    }

    @Override
    public void actionPerformed(ActionEvent evt)
    {
        if (evt.getSource().equals(cancelButton))
        {
            logger.info("CANCEL pressed");
            setVisible(false);
            dispose();
        }
        else if (evt.getSource().equals(confirmButton))
        {
            logger.info("Confirm pressed");
            response = true;
            setVisible(false);
            dispose();
        }
    }

    private void initListeners()
    {
        clearUserButton.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                if (clearUserButton.isSelected())
                {
                    logger.info("Clear single User data selected");
                }
            }
        });

        clearAllButton.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                if (clearAllButton.isSelected())
                {
                    logger.info("Clear ALL data selected");
                }
            }
        });
    }

    /**
     * Return the value of the clearAll field; this should only be accessed
     * once the dialog has completed.
     */
    public boolean isClearAll()
    {
        boolean clearAll = clearAllButton != null && clearAllButton.isSelected();
        logger.info("clearAll: " + clearAll);

        return clearAll;
    }
}
