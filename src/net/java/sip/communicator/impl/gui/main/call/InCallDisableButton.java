// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.call;

import java.util.*;

import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

/**
 * Class representing buttons that should be disabled for a short while once
 * they have been clicked.  The is normally to prevent spamming the servers with
 * multiple overlapping clicks.
 */
public abstract class InCallDisableButton extends InCallButton
{
    private static final long serialVersionUID = 0L;
    private static final Logger logger = Logger.getLogger(InCallDisableButton.class);

    /**
     * How long to disable the button for after it has been clicked.
     * Disabling the button prevents us sending overlapping or wrong INVITEs and
     * then being rejected by the server.  (We could fix this properly in JSIP
     * but disabling the button is a lot easier.)
     */
    private static final long DISABLE_TIME_MS = 300;

    /**
     * When <tt>true</tt>, the button has just been clicked, and is
     * temporarily disabled for a short while to prevent the user from spamming
     * the server with overlapping INVITEs.
     * <p>
     * We use this flag rather than disabling the component for two reasons.
     * <li>it means the button can keep focus (otherwise focus moves to the next
     * element when this once is disabled)
     * <li>we don't want to change the icon when we're temporarily disabled -
     * the disable period is short enough that changing the icon just results in
     * a weird flickery effect.
     */
    private boolean temporarilyDisabled = false;

    /**
     * Timer which is used to re-enable the button once a small amount of time
     * has passed
     */
    private Timer disableTimer = new Timer("InCallDisableButtonTimer");

    /**
     * Creates an in call disable button that is displayed in the in-call window
     *
     * @param imageNormal the normal button image ID
     * @param imageRollover the image ID to use on mouse over
     * @param imageFocus the image ID to use when this button has focus
     * @param imagePressed the image ID to use when the button is pressed
     * @param imageActive the image ID to use when this button is toggled on
     * @param imageActiveRollover the image ID to use when the button is toggled
     * on and has the mouse over
     * @param imageActiveFocus the image ID to use when the button is toggled on
     * and has focus
     * @param imageDisabled the image ID to use when the button is disabled
     * @param tooltipText the tooltip for this button
     */
    public InCallDisableButton(ImageID imageNormal,
                               ImageID imageRollover,
                               ImageID imageFocus,
                               ImageID imagePressed,
                               ImageID imageActive,
                               ImageID imageActiveRollover,
                               ImageID imageActiveFocus,
                               ImageID imageDisabled,
                               String tooltipText)
    {
        super(imageNormal,
              imageRollover,
              imageFocus,
              imagePressed,
              imageActive,
              imageActiveRollover,
              imageActiveFocus,
              imageDisabled,
              tooltipText);
    }

    /**
     * Return true if an action (i.e. a button click) should do anything or not.
     * This method should be called before actionPerformed. If the action should
     * be performed, then this method will disable the button for a short while.
     *
     * @return true if an action should be handle or not.
     */
    protected synchronized boolean shouldHandleAction()
    {
        if (temporarilyDisabled)
        {
            logger.debug("Button is disabled");
            return false;
        }

        // Temporarily disable the button for further clicks. Start a timer task
        // to re-enable the button later
        temporarilyDisabled = true;

        disableTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                synchronized (InCallDisableButton.this)
                {
                    logger.debug("Re-enabling button");
                    temporarilyDisabled = false;
                }
            }
        }, DISABLE_TIME_MS);

        return true;
    }

    /**
     * Called in order to dispose of the button, when it is no longer required
     */
    public void dispose()
    {
        logger.debug("Dispose of button");
        if (disableTimer != null)
        {
            disableTimer.cancel();
            disableTimer = null;
        }
    }
}
