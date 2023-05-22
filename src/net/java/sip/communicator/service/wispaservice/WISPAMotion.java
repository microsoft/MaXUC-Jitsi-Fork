// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.wispaservice;

/**
 * WISPA motions are operational requests sent by Java to the Electron UI as
 * WISPAActions of type MOTION.
 *
 * To ensure consistency in the content of the Motion requests, we store all
 * valid motion values that can be sent in this simple enum, which is
 * accessible throughout the codebase.
 */
public class WISPAMotion
{
    /**
     * Used by CoreNamespaceHandler to send a request to ask the Electron UI
     * to shut down.
     */

    private WISPAMotionType type;
    private Object data;
    public WISPAMotion(WISPAMotionType type)
    {
        this.type = type;
    }

    public WISPAMotion(WISPAMotionType type, Object data)
    {
        this.type = type;
        this.data = data;
    }

    public Object getData()
    {
        return data;
    }

    public WISPAMotionType getType()
    {
        return type;
    }
}