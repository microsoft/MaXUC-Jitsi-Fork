// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.netaddr.WifiInformation;

import com.sun.jna.*;
import com.sun.jna.Structure.FieldOrder;

/**
 * A pointer to a {@link WlanInterfaceInfoListEmpty}
 */
@FieldOrder({"WlanInterfaceInfoList"})
public class PWlanInterfaceInfoList
    extends Structure
{
    public static class ByReference
        extends PWlanInterfaceInfoList
        implements Structure.ByReference
    {
    }

    // Fields making up the structure
    public WlanInterfaceInfoListEmpty.ByReference WlanInterfaceInfoList;
}
