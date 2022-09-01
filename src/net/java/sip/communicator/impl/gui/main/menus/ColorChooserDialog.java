// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.colorchooser.*;
import javax.swing.event.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.SIPCommFrame.*;
import net.java.sip.communicator.util.Logger;

/**
 * Config panel that allows the user to configure the color of the app
 */
public class ColorChooserDialog
{
    /**
     * The resources service
     */
    private static ResourceManagementService resources = GuiActivator.getResources();

    private enum UserColor
    {
        TopGradient(resources.getI18NString("plugin.generalconfig.COLOR.COLOR_1"),
                    "color1",
                    "service.gui.MAIN_BACKGROUND"),
        BottomGradient(resources.getI18NString("plugin.generalconfig.COLOR.COLOR_2"),
                       "color2",
                       "service.gui.MAIN_BACKGROUND_GRADIENT");

        /**
         * Text used on the radio button for the color
         */
        public final String name;

        /**
         * The internal name of the radio button to select the first selectable
         * color
         */
        public final String internalName;

        /**
         * This color
         */
        private Color color;

        /**
         * The original color of when the frame was opened. This allows the
         * user to revert their changes
         */
        private Color originalColor;

        /**
         * The config string used to store this colour
         */
        private final String configString;

        /**
         * Construct a new user color
         *
         * @param name the text used on the radio button
         * @param internalName the internal name for the radio button
         * @param configString
         */
        UserColor(String name, String internalName, String configString)
        {
            this.name = name;
            this.internalName = internalName;
            this.configString = configString;
            this.color = new Color(resources.getColor(configString));
        }

        /**
         * Sets the color
         */
        private void setColor(Color color)
        {
            this.color = color;
        }

        /**
         * Sets the original color
         */
        private void setOriginalColor(Color color)
        {
            this.originalColor = color;
        }
    }

    /**
     * An index which defines which color we are setting
     */
    private UserColor currentColor = UserColor.TopGradient;

    /**
     * The color chooser that allows the user to pick a color
     */
    private BasicColorChooser colorChooser;

    /**
     * Local logger.
     */
    private final Logger sLog = Logger.getLogger(ColorChooserDialog.class);

    /**
     * Text used on the button to open the color chooser dialog, and on the
     * dialog title itself.
     */
    private static final String CHOOSE_COLOR_TEXT = resources.getI18NString(
                                     "plugin.generalconfig.COLOR.CHOOSE_COLOR");

    /**
     * The configuration service
     */
    private static final ConfigurationService config =
                         GuiActivator.getConfigurationService();

    /**
     * The dialog containing the color chooser UI
     */
    private JDialog colorChooserDialog;

    /**
     * The preview panel that shows the current colours
     */
    private MainContentPane previewPanel = new SIPCommFrame.MainContentPane();

    /**
     * Shows the colour chooser dialog
     */
    public void showDialog()
    {
        if (colorChooser == null)
        {
            initUI();
        }

        saveOriginalColours();

        colorChooserDialog.setVisible(true);

        colorChooserDialog.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent event)
            {
                cancel();
            }
        });
    }

    /**
     * Gets and repaints the main frame and the options window
     */
    protected void repaintWindows()
    {
        GuiActivator.getUIService().getMainFrame().repaintWindow();

        GuiActivator.getUIService().getConfigurationContainer().
                                                          validateCurrentForm();
    }

    /**
     * Saves the current colors as the original colors allowing the user to
     * revert their changes
     */
    private void saveOriginalColours()
    {
        UserColor.BottomGradient.setOriginalColor(UserColor.BottomGradient.color);
        UserColor.TopGradient.setOriginalColor(UserColor.TopGradient.color);
    }

    /**
     * Creates and initialises all UI components
     */
    private void initUI()
    {
        JPanel contentPane = new JPanel();
        colorChooser = new BasicColorChooser();
        colorChooser.setColor(currentColor.color);
        colorChooser.getSelectionModel().addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent event)
            {
                currentColor.setColor(colorChooser.getColor());
                saveColors();
                previewPanel.repaint();
            }
        });

        ColorChooserButtonListener actionListener = new ColorChooserButtonListener();

        colorChooserDialog = new SIPCommDialog(true);
        colorChooserDialog.setTitle(CHOOSE_COLOR_TEXT);
        colorChooserDialog.setContentPane(contentPane);
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.LINE_AXIS));
        JPanel configPanel = new JPanel();
        configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.PAGE_AXIS));
        TransparentPanel colorChooserPanel = new TransparentPanel();
        colorChooserPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        colorChooserPanel.add(colorChooser);
        configPanel.add(colorChooserPanel);

        // Construct the radio buttons for choosing which color to set
        JPanel radioButtonsPanel = new TransparentPanel(new FlowLayout());
        ButtonGroup group = new ButtonGroup();

        JRadioButton firstColor = new JRadioButton(UserColor.TopGradient.name);
        ScaleUtils.scaleFontAsDefault(firstColor);
        firstColor.addActionListener(actionListener);
        firstColor.setOpaque(false);
        firstColor.setName(UserColor.TopGradient.internalName);
        firstColor.setSelected(true);

        JRadioButton secondColor = new JRadioButton(UserColor.BottomGradient.name);
        ScaleUtils.scaleFontAsDefault(secondColor);
        secondColor.addActionListener(actionListener);
        secondColor.setOpaque(false);
        secondColor.setName(UserColor.BottomGradient.internalName);

        group.add(firstColor);
        group.add(secondColor);
        radioButtonsPanel.add(firstColor);
        radioButtonsPanel.add(secondColor);

        configPanel.add(radioButtonsPanel);

        JPanel buttonsPanel = new TransparentPanel(new FlowLayout());
        SIPCommBasicTextButton okButton = new SIPCommBasicTextButton(
            resources.getI18NString("service.gui.OK"));

        SIPCommBasicTextButton cancelButton = new SIPCommBasicTextButton(
            resources.getI18NString("service.gui.CANCEL"));

        SIPCommBasicTextButton restoreButton = new SIPCommBasicTextButton(
            resources.getI18NString("plugin.generalconfig.RESTORE"));

        okButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                sLog.user("Ok button clicked in colour chooser dialog");
                saveColors();
                colorChooserDialog.setVisible(false);
                repaintWindows();
            }
        });

        cancelButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                sLog.user("Cancel button clicked in colour chooser dialog");
                cancel();
            }
        });

        restoreButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e)
            {
                sLog.user("Restore default colours button clicked");
                config.user().removeProperty(UserColor.TopGradient.configString);
                config.user().removeProperty(UserColor.BottomGradient.configString);

                UserColor.TopGradient.setColor(
                    new Color(resources.
                        getColor(UserColor.TopGradient.configString)));

                UserColor.BottomGradient.setColor(
                    new Color(resources.
                        getColor(UserColor.BottomGradient.configString)));

                colorChooser.setColor(currentColor.color);

                saveColors();

                previewPanel.repaint();
            }
        });

        JPanel previewLabelPanel = new TransparentPanel();
        previewLabelPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        JLabel previewLabel = new JLabel(resources.getI18NString("impl.media.configform.NO_PREVIEW"));
        previewLabelPanel.add(previewLabel);
        previewLabel.setOpaque(false);
        previewPanel.add(previewLabelPanel);

        buttonsPanel.add(okButton);
        buttonsPanel.add(cancelButton);
        buttonsPanel.add(restoreButton);

        configPanel.add(buttonsPanel);

        contentPane.add(configPanel);
        contentPane.add(previewPanel);

        colorChooserDialog.pack();
        colorChooserDialog.setResizable(false);
    }

    /**
     * Saves the colors that the user has picked
     */
    public void saveColors()
    {
        config.user().setProperty(UserColor.TopGradient.configString,
                           getHexCode(UserColor.TopGradient.color));
        config.user().setProperty(UserColor.BottomGradient.configString,
                           getHexCode(UserColor.BottomGradient.color));
    }

    /**
     * Restores the original colors and closes the window
     */
    private void cancel()
    {
        UserColor.TopGradient.setColor(UserColor.TopGradient.originalColor);
        UserColor.BottomGradient.setColor(UserColor.BottomGradient.originalColor);

        saveColors();

        colorChooser.setColor(currentColor.color);
        colorChooserDialog.setVisible(false);
    }

    /**
     * Converts the color to the hex code representing it
     *
     * @param color the color for which to get the hex codee
     * @return the hex code of the given color
     */
    public static String getHexCode(Color color)
    {
        return String.format("%06x", color.getRGB() & 0xffffff);
    }

    /**
     * The action listener class that is invoked when the user clicks on one of
     * the radio buttons
     */
    private class ColorChooserButtonListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent event)
        {
            JRadioButton source = (JRadioButton) event.getSource();
            if (source.getName().equals(UserColor.TopGradient.internalName))
            {
                sLog.user("Top colour selected");
                currentColor = UserColor.TopGradient;
            }
            else if (source.getName().equals(UserColor.BottomGradient.internalName))
            {
                sLog.user("Bottom colour selected");
                currentColor = UserColor.BottomGradient;
            }

            colorChooser.setColor(currentColor.color);
        }
    }

    /**
     * A basic implementation of the JColorChooser that hides all unneccesary
     * UI elements
     */
    private class BasicColorChooser extends JColorChooser
    {
        private static final long serialVersionUID = 0L;

        private BasicColorChooser()
        {
            super();
            setPreviewPanel(new JPanel());
            for (AbstractColorChooserPanel panel : getChooserPanels())
            {
                if (!panel.getDisplayName().equals("HSV"))
                {
                    removeChooserPanel(panel);
                }
                else
                {
                    setUI(panel);
                }
            }
        }

        /**
         * This is a really hacky way of creating a color chooser UI. The
         * default provided by Swing is really ugly and contains a lot more UI
         * elements than we need, but it has a lot of functionality we don't
         * want to write ourselves. Therefore go through each component and
         * hide the ones we don't want.
         */
        private void setUI(JComponent component)
        {
            // Go through every component and make it transparent
            int sliderCount = 0;
            int radioCount = 0;
            for (Component childComponent : component.getComponents())
            {
                if (childComponent instanceof JSpinner || childComponent instanceof JLabel)
                {
                    component.remove(childComponent);
                }
                else if (childComponent instanceof JSlider)
                {
                    sliderCount++;
                    if (sliderCount == 1)
                    {
                        ((JSlider) childComponent).setValue(100);
                    }
                    component.remove(childComponent);
                }
                else if (childComponent instanceof JRadioButton)
                {
                    radioCount++;
                    if (radioCount == 2)
                    {
                        ((JRadioButton) childComponent).setSelected(true);
                        ((JRadioButton) childComponent).doClick();
                    }
                    component.remove(childComponent);
                }

                if (childComponent instanceof JComponent)
                    setUI((JComponent)childComponent);
            }
        }
    }
}
