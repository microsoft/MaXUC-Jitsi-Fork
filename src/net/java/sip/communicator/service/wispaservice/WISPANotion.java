// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.wispaservice;

/**
 * WISPA notions are operational requests sent by Java to the Electron UI as
 * WISPAActions of type NOTION. Unlike Motions, these are performed by Node
 * and not written to the redux store.
 *
 * To ensure consistency in the content of the Notion requests, we store all
 * valid notion values that can be sent in this simple enum, which is
 * accessible throughout the codebase.
 */
public class WISPANotion
{
    private WISPANotionType type;
    private Object data;
    public WISPANotion(WISPANotionType type)
    {
        this.type = type;
    }

    public WISPANotion(WISPANotionType type, Object data)
    {
        this.type = type;
        this.data = data;
    }

    public Object getData()
    {
        return data;
    }

    public WISPANotionType getType()
    {
        return type;
    }
}