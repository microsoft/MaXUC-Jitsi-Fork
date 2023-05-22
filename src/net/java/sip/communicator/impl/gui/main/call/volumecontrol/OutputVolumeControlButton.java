/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.call.volumecontrol;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The button which controls the output volume.
 */
public class OutputVolumeControlButton extends AbstractVolumeControlButton
{
    private static final long serialVersionUID = 0L;

    public OutputVolumeControlButton(CallConference conference)
    {
        super(conference,
              ImageLoaderService.VOLUME_CONTROL_BUTTON,
              ImageLoaderService.VOLUME_MUTED_BUTTON,
              GuiActivator.getMediaService().getCallVolumeControl(),
              "service.gui.soundlevel.OUTPUT_BACKGROUND",
              "service.gui.soundlevel.OUTPUT_BACKGROUND_MUTED",
              "service.gui.MUTE_BUTTON_TOOL_TIP",
              "service.gui.accessibility.OUTPUT_VOLUME");
    }

    @Override
    public String getName()
    {
        return "Speaker";
    }
}
