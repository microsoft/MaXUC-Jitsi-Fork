// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;

/**
 * Implements a <tt>JDialog</tt> which allows the user to enter some text. Has
 * a cancel button which dismisses the dialog without recording the value that
 * the user entered and an OK button which dismisses the dialog and records the
 * value that was entered.
 */
public class SIPCommEnterTextDialog extends SIPCommDialog
{
    private static final long serialVersionUID = 1L;

    private static final Logger sLog =
                                 Logger.getLogger(SIPCommEnterTextDialog.class);

    private final ResourceManagementService mResourceService =
                                                   UtilActivator.getResources();

    /**
     * The resource of the title to display
     */
    private final String mTitleRes;

    /**
     * The resource of the text to display
     */
    private final String mTextRes;

    /**
     * The initial value to display in the text box
     */
    private final String mInitialValue;

    /**
     * The resource of the icon to display
     */
    private final String mIconRes;

    /**
     * True if the user cancelled the result.  Defaults to true so that if the
     * user closes the window we assume that the operation was cancelled
     */
    private boolean mCancelled = true;

    /**
     * The value entered by the user
     */
    private String mNewValue;

    /**
     * Creates an instance of the dialog
     *
     * @param titleRes The resource of the title to display
     * @param textRes The resource of the text to display
     * @param value The initial value to display in the text
     */
    public SIPCommEnterTextDialog(String titleRes,
                                  String textRes,
                                  String value)
    {
        this(titleRes, textRes, "service.gui.SIP_COMMUNICATOR_LOGO_64x64", value);
    }

    /**
     * Creates an instance of the dialog
     *
     * @param titleRes The resource of the title to display
     * @param textRes The resource of the text to display
     * @param iconRes the resource of the icon to display
     * @param value The initial value to display in the text
     */
    public SIPCommEnterTextDialog(String titleRes,
                                  String textRes,
                                  String iconRes,
                                  String value)
    {
        super(false);

        mTitleRes = titleRes;
        mTextRes = textRes;
        mInitialValue = value;
        mIconRes = iconRes;
        mNewValue = mInitialValue;

        init();
    }

    /**
     * Initializes this window
     */
    private void init()
    {
        initIcon();
        initContent();
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        pack();
    }

    /**
     * Initializes the icon image.
     */
    private void initIcon()
    {
        // Load the icon from the resources.
        ImageIconFuture icon = mResourceService.getImage(mIconRes);

        // Set the layout and appearance of the icon.
        JLabel iconLabel = icon.addToLabel(new JLabel());
        int border = ScaleUtils.scaleInt(20);
        iconLabel.setBorder(BorderFactory.createEmptyBorder(border, border, border, border));
        iconLabel.setAlignmentY(Component.TOP_ALIGNMENT);

        JPanel iconPanel = new TransparentPanel(new BorderLayout());
        iconPanel.add(iconLabel, BorderLayout.NORTH);
        getContentPane().add(iconPanel, BorderLayout.WEST);
    }

    /**
     * Constructs the window and all its components.
     */
    private void initContent()
    {
        setTitle(mResourceService.getI18NString(mTitleRes));

        // Set the style of the hint text area.
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setOpaque(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(textArea.getFont().deriveFont(Font.BOLD));
        textArea.setText(mResourceService.getI18NString(mTextRes));
        textArea.setAlignmentX(0.5f);

        // Create the text entry area:
        final JTextField textField = new JTextField(mInitialValue);
        int width = ScaleUtils.scaleInt(200);
        Dimension size = new Dimension(width,textField.getPreferredSize().height);
        Dimension minSize = new Dimension(width,textField.getMinimumSize().height);
        textField.setPreferredSize(size);
        textField.setMinimumSize(minSize);

        // Add listener so that we can focus on the text field when it is made
        addWindowListener(new WindowAdapter()
        {
            public void windowOpened(WindowEvent arg0)
            {
                textField.requestFocus();
                removeWindowListener(this);
            }
        });

        int gap = ScaleUtils.scaleInt(8);
        TransparentPanel textBoxPanel = new TransparentPanel(
                                                    new GridLayout(0, 1, gap, gap));
        textBoxPanel.add(textField);

        // Buttons
        JButton okButton =
                  new JButton(mResourceService.getI18NString("service.gui.OK"));
        JButton cancelButton =
              new JButton(mResourceService.getI18NString("service.gui.CANCEL"));
        JPanel buttonPanel = new TransparentPanel(
                                             new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        JPanel southEastPanel = new TransparentPanel(new BorderLayout());
        southEastPanel.add(buttonPanel, BorderLayout.EAST);

        // Create a main panel for the window and add the other panels to it.
        int borderGap = ScaleUtils.scaleInt(10);
        TransparentPanel mainPanel = new TransparentPanel(
                                                      new BorderLayout(borderGap, borderGap));
        int outerBorderGap = ScaleUtils.scaleInt(20);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(outerBorderGap, 0, outerBorderGap, outerBorderGap));

        mainPanel.add(textArea, BorderLayout.NORTH);
        mainPanel.add(textBoxPanel, BorderLayout.CENTER);
        mainPanel.add(southEastPanel, BorderLayout.SOUTH);

        // Add the UI to the main dialog
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        getRootPane().setDefaultButton(okButton);

        // Add an action listener for the buttons.
        okButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                sLog.user("Ok button clicked in enter text dialog");
                mCancelled = false;
                mNewValue = textField.getText();
                SIPCommEnterTextDialog.this.setVisible(false);
            }
        });

        cancelButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                sLog.user("Cancel button clicked in enter text dialog");
                mCancelled = true;
                SIPCommEnterTextDialog.this.setVisible(false);
            }
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
     * @return true if the user cancelled the dialog
     */
    public boolean wasCancelled()
    {
        return mCancelled;
    }

    /**
     * @return the value entered by the user
     */
    public String getValueEntered()
    {
        return mNewValue;
    }
}
