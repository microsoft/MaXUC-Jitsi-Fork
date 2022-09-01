/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.generalconfig.autoaway;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.generalconfig.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>ConfigurationForm</tt> that would be added in the user interface
 * configuration window.
 *
 * @author Damien Roth
 */
public class AutoAwayConfigurationPanel
    extends ConfigSectionPanel
{
    /**
     * The <tt>Logger</tt> used by this <tt>AutoAwayConfigurationPanel</tt>
     * instance for logging output.
     */
    private final Logger logger = Logger.getLogger(AutoAwayConfigurationPanel.class);

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private JCheckBox enable;

    private JSpinner timer;

    private static final Color TEXT_COLOR =
        ConfigurationUtils.useNativeTheme() ? Color.BLACK :
            new Color(DesktopUtilActivator.getResources().getColor(
                "service.gui.DARK_TEXT"));

    /**
     * Create an instance of <tt>StatusConfigForm</tt>
     */
    public AutoAwayConfigurationPanel()
    {
        super(false);

        addPanel(createMainPanel());

        initValues();
    }

    /**
     * Init the main panel.
     * @return the created component
     */
    private Component createMainPanel()
    {
        String labels[] = ChatConfigurationPanel.getSplitResourcesString(
                                        "plugin.autoaway.ENABLE_CHANGE_STATUS");
        String changeStatusText1 = (labels[0] == null) ? "" : labels[0];
        String changeStatusText2 = labels[1];

        enable = new SIPCommCheckBox(changeStatusText1);
        enable.setForeground(TEXT_COLOR);
        enable.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        logger.user("Auto away setting toggled to: " +
                                            enable.isSelected());
                        timer.setEnabled(enable.isSelected());
                        saveData();
                    }
                });

        // Spinner
        timer
            = new JSpinner(
                    new SpinnerNumberModel(
                            Preferences.DEFAULT_TIMER,
                            Preferences.MIN_TIMER,
                            Preferences.MAX_TIMER,
                            1));
        ScaleUtils.scaleFontAsDefault(timer);
        timer.addChangeListener(
                new ChangeListener()
                {
                    public void stateChanged(ChangeEvent e)
                    {
                        saveData();
                    }
                });

        JPanel mainPanel = new TransparentPanel(new FlowLayout((FlowLayout.LEADING), 0, 0));

        mainPanel.add(enable);

        JPanel spinnerPanel = new TransparentPanel();
        spinnerPanel.add(timer);

        mainPanel.add(spinnerPanel);

        if (changeStatusText2 != null)
        {
            JLabel changeStatusText2Label = new JLabel(changeStatusText2);
            changeStatusText2Label.setForeground(TEXT_COLOR);
            ScaleUtils.scaleFontAsDefault(changeStatusText2Label);
            mainPanel.add(changeStatusText2Label);
        }

        return mainPanel;
    }

    /**
     * Init the values of the widgets
     */
    private void initValues()
    {
        boolean enabled = Preferences.isEnabled();

        this.enable.setSelected(enabled);
        this.timer.setEnabled(enabled);

        this.timer.setValue(Preferences.getTimer());
    }

    /**
     * Save data in the configuration file
     */
    private void saveData()
    {
        int timerValue = (Integer) timer.getValue();

        if (timerValue < Preferences.MIN_TIMER)
        {
            logger.debug("User entered auto-away timer value: " + timerValue +
                ".  Setting to minimum: " + Preferences.MIN_TIMER);
            timerValue = Preferences.MIN_TIMER;
        }
        else if (timerValue > Preferences.MAX_TIMER)
        {
            logger.debug("User entered auto-away timer value: " + timerValue +
                ".  Setting to maximum: " + Preferences.MAX_TIMER);
            timerValue = Preferences.MAX_TIMER;
        }

        Preferences.saveData(enable.isSelected(), Integer.toString(timerValue));
    }
}
