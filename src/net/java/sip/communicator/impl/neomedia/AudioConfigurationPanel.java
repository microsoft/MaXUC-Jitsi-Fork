/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.neomedia;

import java.awt.*;

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The audio configuration form.
 *
 * @author Yana Stamcheva
 */
public class AudioConfigurationPanel
    extends ConfigurationPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The MediaConfigService used to create the audio config panel.
     */
    private static MediaConfigurationImpl mediaConfigService;

    /**
     * Creates an instance of the <tt>AudioConfigurationPanel</tt>.
     */
    public AudioConfigurationPanel()
    {
        super(new BorderLayout());

        mediaConfigService = (MediaConfigurationImpl) NeomediaActivator.getMediaConfiguration();
        add(mediaConfigService.createAudioConfigPanel(), BorderLayout.NORTH);
    }

    @Override
    public void onDismissed()
    {
        mediaConfigService.onDismissed();
    }
}
