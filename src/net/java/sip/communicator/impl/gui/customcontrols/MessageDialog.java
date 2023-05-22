/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.skin.*;
import org.jitsi.util.*;

/**
 * The <tt>MessageDialog</tt> is a <tt>JDialog</tt> that contains a question
 * message, two buttons to confirm or cancel the question and a check box that
 * allows user to choose to not be questioned any more over this subject.
 * <p>
 * The message and the name of the "OK" button could be configured.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class MessageDialog
    extends SIPCommDialog
    implements  ActionListener,
                Skinnable
{
    private static final long serialVersionUID = 1L;

    private JButton cancelButton = new SIPCommBasicTextButton(
        GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

    private JButton okButton = new SIPCommBasicTextButton(
        GuiActivator.getResources().getI18NString("service.gui.OK"));

    private JCheckBox doNotAskAgain = new SIPCommCheckBox(
        GuiActivator.getResources()
            .getI18NString("service.gui.DO_NOT_ASK_AGAIN"));

    private JLabel iconLabel;

    private StyledHTMLEditorPane messageArea = new StyledHTMLEditorPane();

    private TransparentPanel buttonsPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

    private TransparentPanel messagePanel = new TransparentPanel(new BorderLayout());

    private boolean isConfirmationEnabled = true;

    private ImageID imageID;

    protected int returnCode;

    /**
     * Indicates that the OK button is pressed.
     */
    public static final int OK_RETURN_CODE = 0;

    /**
     * Indicates that the Cancel button is pressed.
     */
    public static final int CANCEL_RETURN_CODE = 1;

    /**
     * Indicates that the OK button is pressed and the Don't ask check box is
     * checked.
     */
    public static final int OK_DONT_ASK_CODE = 2;

    /**
     * Creates an instance of <tt>MessageDialog</tt> by specifying the
     * owner window and the message to be displayed.
     * @param owner the dialog owner
     * @param title the title of the message
     * @param message the message to be displayed
     * @param imageID the resource ID of the imaged to be displayed as the icon in the content of this dialog
     * @param isSaveSizeAndLocation indicates whether to save the size and location of this dialog
     */
    private MessageDialog(Frame owner, String title, String message, ImageID imageID, boolean isSaveSizeAndLocation)
    {
        super(owner, isSaveSizeAndLocation);

        this.imageID = imageID;

        ImageIcon icon = GuiActivator.getImageLoaderService()
            .getImage(imageID)
            .getImageIcon()
            .resolve();

        iconLabel = new JLabel(icon);
        iconLabel.setBorder(ScaleUtils.createEmptyBorder(0, 0, 0, 10));

        int gap = ScaleUtils.scaleInt(5);
        this.getContentPane().setLayout(new BorderLayout(gap, gap));

        ScaleUtils.scaleFontAsDefault(this.messageArea);
        this.messageArea.setOpaque(false);
        this.messageArea.setEditable(false);
        this.messageArea.setContentType("text/html");
        this.messagePanel.setBorder(ScaleUtils.createEmptyBorder(20, 15, 0, 25));
        this.setTitle(title);

        setMessage(message);
    }

    /**
     * Creates an instance of <tt>MessageDialog</tt> by specifying the
     * owner window and the message to be displayed.
     * @param owner the dialog owner
     * @param title the title of the message
     * @param message the message to be displayed
     * @param okButtonName ok button name
     */
    public MessageDialog(Frame owner,
                         String title,
                         String message,
                         String okButtonName)
    {
        this(owner, title, message, ImageLoaderService.WARNING_ICON, false);

        this.okButton.setText(okButtonName);
        this.okButton.setMnemonic(okButtonName.charAt(0));
        this.init();
    }

    public MessageDialog(Frame owner,
                         String title,
                         String message,
                         String okButtonName,
                         ImageID imageID,
                         boolean isConfirmationEnabled,
                         boolean isSaveSizeAndLocation)
    {
        this(owner, title, message, imageID, isSaveSizeAndLocation);

        this.isConfirmationEnabled = isConfirmationEnabled;
        this.okButton.setText(okButtonName);
        this.okButton.setMnemonic(okButtonName.charAt(0));
        this.init();
    }

    /**
     * Creates an instance of <tt>MessageDialog</tt> by specifying the
     * owner window and the message to be displayed.
     * @param owner the dialog owner
     * @param title the title of the message
     * @param message the message to be displayed
     * @param okButtonName ok button name
     * @param isConfirmationEnabled indicates whether the "Do not ask again"
     * button should be enabled or not
     */
    public MessageDialog(   Frame owner,
                            String title,
                            String message,
                            String okButtonName,
                            boolean isConfirmationEnabled)
    {
        this(owner, title, message, okButtonName, ImageLoaderService.WARNING_ICON, isConfirmationEnabled, false);
    }

    /**
     * Creates an instance of <tt>MessageDialog</tt> by specifying the
     * owner window and the message to be displayed.
     * @param owner the dialog owner
     * @param title the title of the message
     * @param message the message to be displayed
     * @param isCancelButtonEnabled <code>true</code> to show the Cancel button,
     * <code>false</code> - otherwise
     */
    public MessageDialog(   Frame owner,
                            String title,
                            String message,
                            boolean isCancelButtonEnabled)
    {
        this(owner, title, message, ImageLoaderService.WARNING_ICON, false);

        this.init();
        if(!isCancelButtonEnabled)
        {
            doNotAskAgain.setText(GuiActivator.getResources()
                .getI18NString("service.gui.DO_NOT_SHOW_AGAIN"));

            buttonsPanel.remove(cancelButton);
        }
    }

    /**
     * Initializes this dialog.
     */
    private void init()
    {
        getRootPane().setDefaultButton(okButton);

        okButton.addActionListener(this);
        cancelButton.addActionListener(this);

        cancelButton.setMnemonic(cancelButton.getText().charAt(0));

        if (OSUtils.IS_MAC)
        {
            this.buttonsPanel.add(cancelButton);
            this.buttonsPanel.add(okButton);
        }
        else
        {
            this.buttonsPanel.add(okButton);
            this.buttonsPanel.add(cancelButton);
        }

        buttonsPanel.setBorder(ScaleUtils.createEmptyBorder(10, 10, 10, 10));

        messagePanel.add(iconLabel, BorderLayout.LINE_START);

        JPanel rightPanel = new TransparentPanel(new BorderLayout());
        rightPanel.add(messageArea, BorderLayout.CENTER);

        if(isConfirmationEnabled)
        {
            rightPanel.add(doNotAskAgain, BorderLayout.PAGE_END);
        }

        messagePanel.add(rightPanel, BorderLayout.CENTER);

        getContentPane().add(messagePanel, BorderLayout.NORTH);
        getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

        setResizable(false);
        setModal(true);
    }

    /**
     * Sets the message to be displayed.
     * @param message The message to be displayed.
     */
    public void setMessage(String message)
    {
        this.messageArea.setText(message);

        int prefWidth = ScaleUtils.scaleInt(300);
        if (messageArea.getPreferredSize().width > prefWidth)
        {
            this.messageArea.setSize(
                new Dimension(prefWidth, ScaleUtils.scaleInt(600)));
            int height = this.messageArea.getPreferredSize().height;
            this.messageArea.setPreferredSize(new Dimension(prefWidth, height));
        }
    }

    /**
     * Sets the enabledness of the OK button
     *
     * @param isEnabled Whether the OK button should be enabled
     */
    public void setOkButtonEnabled(boolean isEnabled)
    {
        okButton.setEnabled(isEnabled);
    }

    /**
     * Shows the dialog.
     * @return The return code that should indicate what was the choice of
     * the user. If the user chooses cancel, the return code is the
     * CANCEL_RETURN_CODE.
     */
    public int showDialog()
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            final int[] returnCodes = new int[1];
            Exception exception = null;
            try
            {
                SwingUtilities.invokeAndWait(new Runnable()
                {
                    public void run()
                    {
                        returnCodes[0] = showDialog();
                    }
                });
            }
            catch (InterruptedException | InvocationTargetException ex)
            {
                exception = ex;
            }
            if (exception != null)
                throw new UndeclaredThrowableException(exception);
            return returnCodes[0];
        }

        pack();

        setAlwaysOnTop(true);
        toFront();
        requestFocus();
        setVisible(true);

        return returnCode;
    }

    /**
     * Handles the <tt>ActionEvent</tt>. Depending on the user choice sets
     * the return code to the appropriate value.
     *
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton)e.getSource();

        if(button.equals(okButton))
        {
            if (doNotAskAgain.isSelected())
            {
                this.returnCode = OK_DONT_ASK_CODE;
            }
            else
            {
                this.returnCode = OK_RETURN_CODE;
            }
        }
        else
        {
            this.returnCode = CANCEL_RETURN_CODE;
        }

        this.dispose();
    }

    /**
     * Visually clicks the cancel button on close.
     *
     * @param isEscaped indicates if the window was close by pressing the escape
     * button
     */
    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();
    }

    /**
     * Reloads icon.
     */
    public void loadSkin()
    {
        GuiActivator.getImageLoaderService()
        .getImage(imageID)
        .getImageIcon()
        .addToLabel(iconLabel);
    }
}
