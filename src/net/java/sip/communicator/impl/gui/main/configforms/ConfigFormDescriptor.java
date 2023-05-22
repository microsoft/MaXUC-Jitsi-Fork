/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.configforms;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

/**
 * The <tt>ConfigFormDescriptor</tt> saves information about the
 * <tt>ConfigurationForm</tt>. When a <tt>ConfigurationForm</tt> is added in the
 * <tt>ConfigurationWindow</tt> we create the corresponding descriptor and load
 * all the data we need in order to show this configuration form.
 *
 * @author Yana Stamcheva
 */
public class ConfigFormDescriptor
{
    private final Logger logger = Logger.getLogger(ConfigFormDescriptor.class);

    private final ConfigurationForm configForm;

    private ImageIconFuture configFormIcon;

    private ConfigurationPanel configFormPanel;

    private String configFormTitle;

    /**
     * Loads the given <tt>ConfigurationForm</tt>.
     *
     * @param configForm the <tt>ConfigurationForm</tt> to load
     */
    public ConfigFormDescriptor(ConfigurationForm configForm)
    {
        this.configForm = configForm;

        BufferedImageFuture icon = null;

        try
        {
            icon = configForm.getIcon();

            configFormTitle = configForm.getTitle();
        }
        catch (Exception e)
        {
            logger.error("Could not load configuration form.", e);
        }

        if(icon != null)
            configFormIcon = icon.getImageIcon();
    }

    /**
     * Returns the icon of the corresponding <tt>ConfigurationForm</tt>.
     *
     * @return the icon of the corresponding <tt>ConfigurationForm</tt>
     */
    public ImageIconFuture getConfigFormIcon()
    {
        return configFormIcon;
    }

    /**
     * Returns the form of the corresponding <tt>ConfigurationForm</tt>.
     *
     * @return the form of the corresponding <tt>ConfigurationForm</tt>
     */
    public ConfigurationPanel getConfigFormPanel()
    {
        // On Macs we don't want to cache the config panel - we want to recreate
        // it each time
        if (configFormPanel == null || OSUtils.IS_MAC)
        {
            Object form = configForm.getForm();
            if (!(form instanceof ConfigurationPanel))
            {
                throw new ClassCastException("ConfigurationFrame :"
                    + form.getClass()
                    + " is not a class supported by this ui implementation");
            }

            configFormPanel = (ConfigurationPanel) form;
        }
        return configFormPanel;
    }

    /**
     * Returns the title of the corresponding <tt>ConfigurationForm</tt>.
     *
     * @return the title of the corresponding <tt>ConfigurationForm</tt>
     */
    public String getConfigFormTitle()
    {
        return configFormTitle;
    }

    /**
     * Returns the corresponding <tt>ConfigurationForm</tt>.
     *
     * @return the corresponding <tt>ConfigurationForm</tt>
     */
    public ConfigurationForm getConfigForm()
    {
        return configForm;
    }

    @Override
    public String toString()
    {
        return "ConfigFormDescriptor{" +
               "configFormTitle='" + configFormTitle + '\'' +
               '}';
    }
}
