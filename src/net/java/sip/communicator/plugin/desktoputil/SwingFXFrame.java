// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import javax.swing.JFrame;

import javafx.application.Platform;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

/**
 * Provides a JFrame that acts like a JavaFX stage and so is capable of showing
 * a JavaFX scene.
 */
public class SwingFXFrame extends JFrame
{
    private static final long serialVersionUID = 1L;

    private JFXPanel fxPanel;

    public SwingFXFrame()
    {
        super();
        init();
    }

    public SwingFXFrame(boolean isSaveSizeAndLocation)
    {
        super();
        init();
    }

    private void init()
    {
        fxPanel = new JFXPanel();
        add(fxPanel);
    }

    /**
     * Sets the stage's scene so that it can be displayed.
     *
     * @param scene is the Scene that the stage should display
     */
    public void setScene(Scene scene)
    {
        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                fxPanel.setScene(scene);
            }
        });
    }

    /**
     * Get the JFXPanel that's acting as the JavaFX stage.
     *
     * @return JFXPanel that wraps JavaFX content.
     */
    public JFXPanel getFXPanel()
    {
        return fxPanel;
    }

}
