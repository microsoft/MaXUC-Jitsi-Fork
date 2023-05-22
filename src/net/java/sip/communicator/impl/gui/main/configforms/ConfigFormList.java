/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.configforms;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.service.gui.ConfigurationForm;
import net.java.sip.communicator.util.Logger;
import org.jitsi.util.OSUtils;

/**
 * The list containing all <tt>ConfigurationForm</tt>s.
 *
 * @author Yana Stamcheva
 */
public class ConfigFormList
    extends JList<ConfigFormDescriptor>
    implements ListSelectionListener
{
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>Logger</tt> used by the <tt>ConfigFormList</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(ConfigFormList.class);

    private final DefaultListModel<ConfigFormDescriptor> listModel = new DefaultListModel<>();

    private final ConfigurationFrame configFrame;

    private ConfigFormDescriptor currentFormDescriptor;

    /**
     * Creates an instance of <tt>ConfigFormList</tt>
     * @param configFrame the parent configuration frame
     */
    public ConfigFormList(ConfigurationFrame configFrame)
    {
        this.configFrame = configFrame;

        this.setOpaque(false);
        this.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        this.setVisibleRowCount(1);
        this.setCellRenderer(new ConfigFormListCellRenderer());
        this.setModel(listModel);

        this.addListSelectionListener(this);
    }

    /**
     * Adds a new <tt>ConfigurationForm</tt> to this list.
     * @param configForm The <tt>ConfigurationForm</tt> to add.
     */
    public void addConfigForm(ConfigurationForm configForm)
    {
        if (configForm == null)
            throw new IllegalArgumentException("configForm");

        if (hasConfigForm(configForm))
        {
            logger.warn("Config form already exists: " +
                        configForm.getTitle() + " (" + configForm + ")");
        }
        else
        {
            int i = 0;
            int count = listModel.size();
            int configFormIndex = configForm.getIndex();
            for (; i < count; i++)
            {
                ConfigFormDescriptor descriptor = listModel.get(i);

                if (configFormIndex < descriptor.getConfigForm().getIndex())
                    break;
            }

            logger.info("Adding config form: " +
                        configForm.getTitle() + " (" + configForm + ") " +
                        "with index: " + i);

            listModel.add(i, new ConfigFormDescriptor(configForm));
        }
    }

    /**
     * Is this ConfigurationForm already in the ConfigFormList?
     *
     * @param configForm The <tt>ConfigurationForm</tt> to check for.
     */
    public boolean hasConfigForm(ConfigurationForm configForm)
    {
        ConfigFormDescriptor descriptor = findDescriptor(configForm);
        return (descriptor != null);
    }

    /**
     * Removes a <tt>ConfigurationForm</tt> from this list.
     * @param configForm The <tt>ConfigurationForm</tt> to remove.
     */
    public void removeConfigForm(ConfigurationForm configForm)
    {
        ConfigFormDescriptor descriptor = findDescriptor(configForm);

        if (descriptor != null)
            listModel.removeElement(descriptor);
    }

    /**
     * Selects the given <tt>ConfigurationForm</tt>.
     *
     * @param configForm the <tt>ConfigurationForm</tt> to select
     */
    public void setSelected(ConfigurationForm configForm)
    {
        ConfigFormDescriptor descriptor = findDescriptor(configForm);

        if (descriptor != null)
        {
            setSelectedValue(descriptor, true);
        }
    }

    /**
     * Selects the <tt>ConfigurationForm</tt> with the given title
     *
     * @param title the title of the <tt>ConfigurationForm</tt> to select
     */
    public void setSelected(String title)
    {
        for (int i = 0; i < listModel.getSize(); i++)
        {
            ConfigFormDescriptor descriptor = listModel.getElementAt(i);

            if (descriptor.getConfigForm().getTitle().equals(title))
            {
                setSelectedValue(descriptor, true);
            }
        }
    }

    /**
     * Called when user selects a component in the list of configuration forms.
     */
    public void valueChanged(ListSelectionEvent e)
    {
        if(!e.getValueIsAdjusting())
        {
            ConfigFormDescriptor configFormDescriptor = this.getSelectedValue();

            String videoTitle = GuiActivator.getResources()
                              .getI18NString("impl.neomedia.configform.VIDEO");

            boolean currentlyOnVideoTab =
                    currentFormDescriptor != null &&
                    videoTitle.equals(currentFormDescriptor.getConfigFormTitle());
            boolean changingToNonVideoTab =
                    configFormDescriptor == null ||
                    !videoTitle.equals(configFormDescriptor.getConfigFormTitle());

            if (OSUtils.IS_MAC && currentlyOnVideoTab && changingToNonVideoTab)
            {
                /*
                 This is a Mac, and the tab is being changed from the video tab
                 to some other tab. We must therefore re-create the
                 configuration window. This is to avoid a Mac bug where the
                 video frame will be shown on all frames otherwise.

                 Note that we can receive ListSelectionEvents saying
                 that the tab has changed to video even when we're already
                 on the video tab, hence why we're checking *both* that
                 we're currently on the video tab *and* that the new tab
                 isn't a video tab.
                */
                logger.info("Mac, switching away from video tab to " + configFormDescriptor);

                configFrame.setVisible(false);

                // Create a replacement frame:
                ConfigurationFrame frame = (ConfigurationFrame)
                        GuiActivator.getUIService().getConfigurationContainer();

                // Remove the listener before we show the frame and change tabs,
                // as otherwise we enter a loop involving this method
                ConfigFormList newConfigList = frame.getConfigList();
                newConfigList.removeListSelectionListener(newConfigList);
                frame.setVisible(true);
                frame.showFormContent(configFormDescriptor);
                newConfigList.setSelected(configFormDescriptor.getConfigForm());
                newConfigList.addListSelectionListener(newConfigList);
            }
            else
            {
                if(configFormDescriptor != null)
                    configFrame.showFormContent(configFormDescriptor);
            }

            currentFormDescriptor = configFormDescriptor;
        }
    }

    /**
     * Finds the list descriptor corresponding the given
     * <tt>ConfigurationForm</tt>.
     *
     * @param configForm the <tt>ConfigurationForm</tt>, which descriptor we're
     * looking for
     * @return the list descriptor corresponding the given
     * <tt>ConfigurationForm</tt>
     */
    private ConfigFormDescriptor findDescriptor(ConfigurationForm configForm)
    {
        for(int i = 0; i < listModel.getSize(); i++)
        {
            ConfigFormDescriptor descriptor = listModel.getElementAt(i);

            if(descriptor.getConfigForm().equals(configForm))
            {
                return descriptor;
            }
        }

        return null;
    }
}
