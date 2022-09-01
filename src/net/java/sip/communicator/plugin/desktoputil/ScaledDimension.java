// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;

/**
 * A ScaledDimension which is a Dimension using automatically scaled up
 * width and height based on the current display scaling
 */
public class ScaledDimension extends Dimension
{
    private static final long serialVersionUID = 1L;

    public ScaledDimension(int width, int height)
    {
        // Scale the provided width and height
        super(ScaleUtils.scaleInt(width), ScaleUtils.scaleInt(height));
    }

    public ScaledDimension(Dimension dimension)
    {
        this(dimension.width, dimension.height);
    }

    @Override
    public void setSize(int width, int height)
    {
        // Scale the provided width and height
        super.setSize(ScaleUtils.scaleInt(width), ScaleUtils.scaleInt(height));
    }
}