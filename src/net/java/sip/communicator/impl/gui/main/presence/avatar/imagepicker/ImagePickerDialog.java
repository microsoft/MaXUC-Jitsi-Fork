/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.presence.avatar.imagepicker;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;

import javax.swing.*;

import net.java.sip.communicator.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import org.jitsi.util.Logger;

/**
 * Dialog in which we can load an image from file or take new one by using
 * the webcam. Scaling the image to desired size.
 *
 * @author Shashank Tyagi
 * @author Damien Roth
 * @author Damian Minkov
 */
public class ImagePickerDialog
    extends SIPCommDialog
    implements ActionListener
{
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>Logger</tt> used by this class.
     */
    private static final Logger logger =
                                      Logger.getLogger(ImagePickerDialog.class);

    private EditPanel editPanel;

    private JButton okButton, cancelButton;
    private JButton selectFileButton, webcamButton;

    private boolean editCanceled = false;

    public ImagePickerDialog(int clipperZoneWidth, int clipperZoneHeight)
    {
        super();
        this.initComponents(clipperZoneWidth, clipperZoneHeight);
        this.initDialog();
        this.setLocationRelativeTo(null);
    }

    /**
     * Initialize the dialog with the already created components.
     */
    private void initDialog()
    {
        this.setTitle(GuiActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.IMAGE_PICKER"));
        this.setModal(true);
        this.setResizable(true);

        this.setLayout(new BorderLayout());

        TransparentPanel editButtonsPanel = new TransparentPanel();
        editButtonsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        editButtonsPanel.add(this.selectFileButton);
        editButtonsPanel.add(this.webcamButton);

        TransparentPanel okCancelPanel = new TransparentPanel();
        okCancelPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        okCancelPanel.add(cancelButton);
        okCancelPanel.add(okButton);

        TransparentPanel buttonsPanel = new TransparentPanel();
        buttonsPanel.setLayout(new BorderLayout());
        buttonsPanel.add(editButtonsPanel, BorderLayout.WEST);
        buttonsPanel.add(okCancelPanel, BorderLayout.CENTER);

        this.add(this.editPanel, BorderLayout.CENTER);
        this.add(buttonsPanel, BorderLayout.SOUTH);

        this.pack();
    }

    /**
     * Initialize UI components.
     * @param clipperZoneWidth width
     * @param clipperZoneHeight height
     */
    private void initComponents(int clipperZoneWidth, int clipperZoneHeight)
    {
        // Edit panel
        this.editPanel = new EditPanel(clipperZoneWidth, clipperZoneHeight);

        // Buttons
        this.okButton = new SIPCommBasicTextButton(
            GuiActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.SET"));
        this.okButton.addActionListener(this);
        this.okButton.setName("okButton");

        this.cancelButton = new SIPCommBasicTextButton(
            GuiActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.CANCEL"));
        this.cancelButton.addActionListener(this);
        this.cancelButton.setName("cancelButton");

        this.selectFileButton = new SIPCommBasicTextButton(
            GuiActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.CHOOSE_FILE"));
        this.selectFileButton.addActionListener(this);
        this.selectFileButton.setName("selectFileButton");

        this.webcamButton = new SIPCommBasicTextButton(
            GuiActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.TAKE_PHOTO"));
        this.webcamButton.addActionListener(this);
        this.webcamButton.setName("webcamButton");
    }

    /**
     * Shows current dialog and setting initial picture.
     * @param image the initial picture to show.
     * @return the result: clipped image (from file or webcam).
     */
    public byte[] showDialog(Image image)
    {
        if (image != null)
        {
            editPanel.setImage(ImageUtils.getBufferedImage(image));
            editPanel.reset();
        }

        WindowUtils.makeWindowVisible(this, true);

        return editCanceled ? null : editPanel.getClippedImage();
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        String name = ((JButton) e.getSource()).getName();
        switch (name)
        {
            case "cancelButton":
                logger.user("Image picker dialog cancelled");
                editCanceled = true;
                this.setVisible(false);
                break;
            case "selectFileButton":
                logger.user("Select image file button clicked");
                SipCommFileChooser chooser = GenericFileDialog.create(
                        this,
                        GuiActivator.getResources().getI18NString(
                                "service.gui.avatar.imagepicker.CHOOSE_FILE"),
                        SipCommFileChooser.LOAD_FILE_OPERATION);

                ImageFileFilter filter = new ImageFileFilter();
                chooser.addFilter(filter);
                chooser.setFileFilter(filter);

                File selectedFile = chooser.getFileFromDialog();
                if (selectedFile != null)
                {
                    logger.user("Image file selected");
                    try
                    {
                        // Specify a maximum avatar size to scale selected
                        // images down to before loading into memory to prevent
                        // OutOfMemoryError
                        BufferedImage image =
                                ImageUtils.loadImageAndCorrectOrientation(
                                        selectedFile,
                                        ConfigurationUtils.getMaximumAvatarSideLength());

                        if (image != null)
                        {
                            this.editPanel.setImage(image);
                        } else
                        {
                            logger.error("Failed to load image");
                        }
                    }
                    catch (IOException ioe)
                    {
                        logger.error("Failed to read image file", ioe);
                    }
                } else
                {
                    logger.user("Image file chooser dialog cancelled");
                }
                break;
            case "okButton":
                logger.user("Image selection confirmed");
                editCanceled = false;
                this.setVisible(false);
                break;
            case "webcamButton":
                logger.user("Set new image via webcam selected");
                WebcamDialog dialog = new WebcamDialog(this);
                dialog.setVisible(true);
                byte[] bimage = dialog.getGrabbedImage();

                if (bimage != null)
                {
                    Image i = new ImageIcon(bimage).getImage();
                    editPanel.setImage(ImageUtils.getBufferedImage(i));
                    logger.user("Webcam image captured");
                }
                else
                {
                    logger.user("Webcam capture cancelled");
                }
                break;
        }
    }

    @Override
    protected void close(boolean isEscaped)
    {
        editCanceled = true;
        dispose();
    }

    /**
     * The filter for file chooser.
     */
    static class ImageFileFilter extends SipCommFileFilter
    {
        public boolean accept(File f)
        {
            String path = f.getAbsolutePath().toLowerCase();
            return (path.matches("(.*)\\.(jpg|jpeg|png|bmp)$") ||
                    f.isDirectory());
        }

        public String getDescription()
        {
            return GuiActivator.getResources()
                .getI18NString("service.gui.avatar.imagepicker.IMAGE_FILES");
        }
    }
}
