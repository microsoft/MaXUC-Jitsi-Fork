// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.netaddr.WifiInformation;
import java.util.*;

import com.sun.jna.*;

/**
 * A pointer to a {@link WlanInterfaceInfoListEmpty}
 */
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

    @Override
    protected List<String> getFieldOrder()
    {
        return Arrays.asList(new String[] {"WlanInterfaceInfoList"});
    }
}
