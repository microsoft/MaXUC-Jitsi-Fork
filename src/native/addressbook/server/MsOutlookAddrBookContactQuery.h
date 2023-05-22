/*
* Jitsi, the OpenSource Java VoIP and Instant Messaging client.
*
* Distributable under LGPL license.
* See terms of license at gnu.org.
*/
// Portions (c) Microsoft Corporation. All rights reserved.
#ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRESSBOOK_MSOUTLOOKADDRBOOKCONTACTQUERY_H_
#define _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRESSBOOK_MSOUTLOOKADDRBOOKCONTACTQUERY_H_

#define GUID_TYPE_ADDRESS     0
#define GUID_TYPE_APPOINTMENT 1
#define GUID_TYPE_MEETING     2

#define FOLDER_TYPE_CALENDAR  0
#define FOLDER_TYPE_CONTACTS  1

#include <mapidefs.h>

int MsOutlookAddrBookContactQuery_IMAPIProp_1DeleteProp
(long propId, const char * nativeEntryId);

long MsOutlookAddrBookContactQuery_IMAPIProp_1GetProps(
    const char* nativeEntryId,
    int propIdCount,
    long * propIds,
    long flags,
    void ** props,
    unsigned long* propsLength,
    char * propsType,
    int guidType);

bool MsOutlookAddrBookContactQuery_Handle_Property(
    LPSPropValue prop,
    LPUNKNOWN mapiProp,
    unsigned long* length,
    char* type,
    void** result);

int MsOutlookAddrBookContactQuery_IMAPIProp_1SetPropString
(long propId, const wchar_t* nativeValue, const char* nativeEntryId);

char* MsOutlookAddrBookContactQuery_createContact(void);

int MsOutlookAddrBookContactQuery_deleteContact(const char * nativeEntryId);

void MsOutlookAddrBookContactQuery_foreachMailUser
(const char * query, void * callback, void * callbackObject, int folderType);

char* MsOutlookAddrBookContactQuery_getStringUnicodeProp
(LPUNKNOWN entry, ULONG propId);

int MsOutlookAddrBookContactQuery_compareEntryIds
(LPCSTR id1, LPCSTR id2);

LPSTR MsOutlookAddrBookContactQuery_getContactId(LPMAPIPROP contact);

char* MsOutlookAddrBookContactQuery_getDefaultFolderEntryId(int contacts);

#endif
