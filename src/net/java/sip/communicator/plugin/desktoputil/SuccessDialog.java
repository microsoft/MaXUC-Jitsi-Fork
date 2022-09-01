/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

/**
 * Implements a <tt>JDialog</tt> which displays a success message.
 * <tt>SuccessDialog</tt> has an OK button which dismisses the message.
 *
 * This class shouldn't exist in this form - it was block copy created from
 * the ErrorDialog, and really should share common code with that class.
 * However now is not the time to refactor it!
 * I've tried to update it with the bug fixes that have been made to that class,
 * but failed to be applied here.
 */
public class SuccessDialog
    extends SIPCommDialog
    implements  ActionListener
{
    private static final long serialVersionUID = 1L;

    private JButton okButton
        = new SIPCommBasicTextButton(
                DesktopUtilActivator.getResources().getI18NString("service.gui.OK"));

    private TransparentPanel buttonsPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));

    private TransparentPanel infoMessagePanel = new TransparentPanel();

    private TransparentPanel messagePanel
        = new TransparentPanel(new BorderLayout());

    private TransparentPanel mainPanel
        = new TransparentPanel(new BorderLayout(THIN_BORDER, THIN_BORDER));

    /**
     * The maximum width that we allow message dialogs to have.
     */
    private static final int MAX_MSG_PANE_WIDTH = ScaleUtils.scaleInt(340);

    /**
     * The maximum height that we allow message dialogs to have.
     */
    private static final int MAX_MSG_PANE_HEIGHT = ScaleUtils.scaleInt(800);

    /**
     * The width of thin borders/gaps used in this dialog.
     */
    private static final int THIN_BORDER = ScaleUtils.scaleInt(10);

    /**
     * The width of thick borders/gaps used in this dialog.
     */
    private static final int THICK_BORDER = ScaleUtils.scaleInt(20);

    /**
     * Initializes a new <tt>ErrorDialog</tt> with a specific owner
     * <tt>Frame</tt>, title and message to be displayed.
     *
     * @param owner the dialog owner
     * @param title the title of the dialog
     * @param message the message to be displayed
     */
    public SuccessDialog(Frame owner,
                         String title,
                         String message)
    {
        super(owner, false);

        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(
            THICK_BORDER, THICK_BORDER, THIN_BORDER, THICK_BORDER));

        this.setTitle(title);
        this.infoMessagePanel.setLayout(new BorderLayout());

        JTextArea messageArea = new JTextArea();
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        ScaleUtils.scaleFontAsDefault(messageArea);

        messageArea.setOpaque(false);
        messageArea.setEditable(false);
        messageArea.setText(message);

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

        this.init();
    }

    /**
     * Initializes this dialog.
     */
    private void init()
    {
        this.getRootPane().setDefaultButton(okButton);

        this.buttonsPanel.add(okButton);

        this.okButton.addActionListener(this);

        this.messagePanel.add(infoMessagePanel, BorderLayout.NORTH);

        this.mainPanel.add(messagePanel, BorderLayout.CENTER);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel);
    }

    /**
     * Shows the dialog.
     */
    public void showDialog()
    {
        this.pack();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        this.setLocation(screenSize.width/2 - this.getWidth()/2,
                screenSize.height/2 - this.getHeight()/2);

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
            this.dispose();
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
}
