// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;

/**
 * Subclass of TransparentPanel to be used for the main panels of configuration
 * forms.
 */
public class ConfigurationPanel extends TransparentPanel
{
    private static final long serialVersionUID = 1L;

    public ConfigurationPanel()
    {
        super();
    }

    public ConfigurationPanel(LayoutManager layout)
    {
        super(layout);
    }

    /**
     * Called when the ConfigurationPanel ceases to be visible in the UI,
     * either because its parent Container has closed, or because it is in a
     * tab view which has changed to focus on a different configuration form.
     */
    public void onDismissed()
    {
        // Default does nothing.
    }

    /**
     * Called when the contents of the ConfigurationPanel need to be refreshed.
     */
    public void onRefresh()
    {
        // Default does nothing.
    }

    /**
     * Called when the ConfigurationPanel is deactivated because either it has
     * lost focus or has been closed.
     */
    public void onDeactivated()
    {
        // Default does nothing.
    }
}
