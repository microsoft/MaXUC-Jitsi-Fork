// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui;

import net.java.sip.communicator.impl.gui.main.StandaloneMainFrame;
import net.java.sip.communicator.util.Logger;

/**
 * UI Service Impl for Standalone meeting users. This extends
 * AbstractUIServiceImpl but only requires an implementation for the
 * createMainFrame() method as Chats and Calls are not supported.
 */
public class StandaloneUIServiceImpl extends AbstractUIServiceImpl
{
    private static final Logger logger =
        Logger.getLogger(StandaloneUIServiceImpl.class);

    @Override
    public void createMainFrame()
    {
        logger.debug("start standalone meeting main frame.");
        this.mainFrame = new StandaloneMainFrame();
    }
}
