// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.netaddr.WifiInformation;
import com.sun.jna.*;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.ptr.*;

public interface WlanApi
    extends Library
{
    WlanApi INSTANCE = Native.loadLibrary("wlanapi", WlanApi.class);

    int WlanOpenHandle(int dwClientVersion,
                       PointerByReference pReserved,
                       PointerByReference pdwNegotiatedVersion,
                       Pointer phClientHandle);

    int WlanCloseHandle(Pointer phClientHandle,
                        PointerByReference pReserved);

    int WlanEnumInterfaces(Pointer hClientHandle,
                           PointerByReference pReserved,
                           PWlanInterfaceInfoList.ByReference ppInterfaceList);

    int WlanQueryInterface(
            Pointer hClientHandle,
            GUID.ByReference pInterfaceGuid,
            int OpCode, // WLAN_INTF
            Pointer pReserved,
            IntByReference pdwDataSize,
            PointerByReference ppData,
            Pointer pWlanOpcodeValueType);
}
