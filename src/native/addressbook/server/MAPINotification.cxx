/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
#include "MAPINotification.h"

#include "MAPISession.h"
#include "MsOutlookAddrBookContactSourceService.h"
#include "MsOutlookAddrBookContactQuery.h"

#include <mapidefs.h>
#include <stdio.h>
#include <unknwn.h>

/**
 * Manages notification for the message data base (used to get the list of
 * contact).
 *
 * @author Vincent Lucas
 */

/**
 * The List of events we want to retrieve.
 */
static ULONG MAPINotification_EVENT_MASK
    = fnevObjectCreated
        | fnevObjectDeleted
        | fnevObjectModified
        | fnevObjectMoved
        | fnevObjectCopied;

static LPMDB * MAPINotification_msgStores = NULL;
static LPMAPIADVISESINK * MAPINotification_adviseSinks = NULL;
static ULONG * MAPINotification_msgStoresConnection = NULL;
static LPMAPITABLE MAPINotification_msgStoresTable = NULL;
static LPMAPIADVISESINK MAPINotification_msgStoresTableAdviseSink = NULL;
static ULONG MAPINotification_msgStoresTableConnection = 0;
static ULONG MAPINotification_nbMsgStores = 0;
static ULONG MAPINotification_openEntryUlFlags = MAPI_BEST_ACCESS;

void (*MAPINotification_callDeletedMethod)(LPSTR iUnknown) = NULL;
void (*MAPINotification_callInsertedMethod)(LPSTR iUnknown) = NULL;
void (*MAPINotification_callUpdatedMethod)(LPSTR iUnknown) = NULL;

ULONG MAPINotification_registerNotifyMessageDataBase
    (LPMDB iUnknown, LPMAPIADVISESINK * adviseSink);
ULONG MAPINotification_registerNotifyTable
    (LPMAPITABLE iUnknown, LPMAPIADVISESINK * adviseSink);
LONG STDAPICALLTYPE MAPINotification_tableChanged
    (LPVOID lpvContext, ULONG cNotifications, LPNOTIFICATION lpNotifications);

/**
 * Functions called when an event is fired from the message data base.
 *
 * @param lpvContext A pointer to the message data base.
 * @param cNotifications The number of event in this call.
 * @param lpNotifications The list of notifications.
 */
LONG
STDAPICALLTYPE MAPINotification_onNotify
    (LPVOID lpvContext, ULONG cNotifications, LPNOTIFICATION lpNotifications)
{
    LOG_DEBUG("MAPINotification_onNotify enter: %lu", cNotifications);

    for(unsigned int i = 0; i < cNotifications; ++i)
    {
        // A contact has been created (a new one or a copy).
        if(lpNotifications[i].ulEventType == fnevObjectCreated
                || lpNotifications[i].ulEventType == fnevObjectCopied)
        {
            if(lpvContext != NULL)
            {
                LPSTR entryIdStr = (LPSTR)
                    ::malloc((lpNotifications[i].info.obj.cbEntryID + 1) * 2);

                HexFromBin(
                        (LPBYTE) lpNotifications[i].info.obj.lpEntryID,
                        lpNotifications[i].info.obj.cbEntryID,
                        entryIdStr);

                if(lpNotifications[i].info.obj.ulObjType == MAPI_MESSAGE
                        && MAPINotification_callInsertedMethod != NULL)
                {
                    MAPINotification_callInsertedMethod(entryIdStr);
                }

                ::free(entryIdStr);
                entryIdStr = NULL;
            }
        }
        // A contact has been Modified
        else if(lpNotifications[i].ulEventType == fnevObjectModified)
        {
            if(lpvContext != NULL)
            {
                LPSTR entryIdStr = (LPSTR)
                    ::malloc((lpNotifications[i].info.obj.cbEntryID + 1) * 2);

                HexFromBin(
                        (LPBYTE) lpNotifications[i].info.obj.lpEntryID,
                        lpNotifications[i].info.obj.cbEntryID,
                        entryIdStr);

                if(lpNotifications[i].info.obj.ulObjType == MAPI_MESSAGE
                        && MAPINotification_callUpdatedMethod != NULL)
                {
                    MAPINotification_callUpdatedMethod(entryIdStr);
                }

                ::free(entryIdStr);
                entryIdStr = NULL;

                // If the entry identifier has changed, then deletes the old
                // one.
                if(lpNotifications[i].info.obj.lpOldID != NULL
                        && lpNotifications[i].info.obj.cbOldID > 0)
                {
                    LPSTR oldEntryIdStr = (LPSTR)
                        ::malloc((lpNotifications[i].info.obj.cbOldID + 1) * 2);
                    HexFromBin(
                            (LPBYTE) lpNotifications[i].info.obj.lpOldID,
                            lpNotifications[i].info.obj.cbOldID,
                            oldEntryIdStr);
                    if(lpNotifications[i].info.obj.ulObjType == MAPI_MESSAGE
                            && MAPINotification_callDeletedMethod != NULL)
                    {
                        MAPINotification_callDeletedMethod(oldEntryIdStr);
                    }
                    ::free(oldEntryIdStr);
                    oldEntryIdStr = NULL;
                }
            }
        }
        // A contact has been deleted.
        else if(lpNotifications[i].ulEventType == fnevObjectDeleted)
        {
            if(lpvContext != NULL)
            {
                LPSTR entryIdStr = (LPSTR)
                    ::malloc((lpNotifications[i].info.obj.cbEntryID + 1) * 2);

                HexFromBin(
                        (LPBYTE) lpNotifications[i].info.obj.lpEntryID,
                        lpNotifications[i].info.obj.cbEntryID,
                        entryIdStr);

                if(lpNotifications[i].info.obj.ulObjType == MAPI_MESSAGE
                        && MAPINotification_callDeletedMethod != NULL)
                {
                    MAPINotification_callDeletedMethod(entryIdStr);
                }

                ::free(entryIdStr);
                entryIdStr = NULL;
            }
        }
        // A contact has been deleted (moved to trash).
        else if(lpNotifications[i].ulEventType == fnevObjectMoved)
        {
            if(lpvContext != NULL)
            {
                LPSTR entryIdStr = (LPSTR)
                    ::malloc((lpNotifications[i].info.obj.cbEntryID + 1) * 2);
                HexFromBin(
                        (LPBYTE) lpNotifications[i].info.obj.lpEntryID,
                        lpNotifications[i].info.obj.cbEntryID,
                        entryIdStr);
                LPSTR parentEntryIdStr = (LPSTR)
                    ::malloc((lpNotifications[i].info.obj.cbParentID + 1) * 2);
                HexFromBin(
                        (LPBYTE) lpNotifications[i].info.obj.lpParentID,
                        lpNotifications[i].info.obj.cbParentID,
                        parentEntryIdStr);
                ULONG wasteBasketTags[] = {1, PR_IPM_WASTEBASKET_ENTRYID};  
                ULONG wasteBasketNbValues = 0;  
                LPSPropValue wasteBasketProps = NULL;
                ((LPMDB)lpvContext)->GetProps(
                        (LPSPropTagArray) wasteBasketTags,
                        MAPI_UNICODE,
                        &wasteBasketNbValues,
                        &wasteBasketProps); 
                LPSTR wasteBasketEntryIdStr = (LPSTR)
                    ::malloc((wasteBasketProps[0].Value.bin.cb + 1) * 2);
                HexFromBin(
                        (LPBYTE) wasteBasketProps[0].Value.bin.lpb,
                        wasteBasketProps[0].Value.bin.cb,
                        wasteBasketEntryIdStr);

                if(lpNotifications[i].info.obj.ulObjType == MAPI_MESSAGE
                        && strcmp(parentEntryIdStr, wasteBasketEntryIdStr) == 0
                        && MAPINotification_callDeletedMethod != NULL)
                {
                    MAPINotification_callDeletedMethod(entryIdStr);
                }

                ::free(entryIdStr);
                entryIdStr = NULL;
                ::free(parentEntryIdStr);
                parentEntryIdStr = NULL;
                ::free(wasteBasketEntryIdStr);
                wasteBasketEntryIdStr = NULL;
                MAPIFreeBuffer(wasteBasketProps);

                // If the entry identifier has changed, then deletes the old
                // one.
                if(lpNotifications[i].info.obj.lpOldID != NULL
                        && lpNotifications[i].info.obj.cbOldID > 0)
                {
                    LPSTR oldEntryIdStr = (LPSTR)
                        ::malloc((lpNotifications[i].info.obj.cbOldID + 1) * 2);
                    HexFromBin(
                            (LPBYTE) lpNotifications[i].info.obj.lpOldID,
                            lpNotifications[i].info.obj.cbOldID,
                            oldEntryIdStr);
                    if(lpNotifications[i].info.obj.ulObjType == MAPI_MESSAGE
                            && MAPINotification_callDeletedMethod != NULL)
                    {
                        MAPINotification_callDeletedMethod(oldEntryIdStr);
                    }
                    ::free(oldEntryIdStr);
                    oldEntryIdStr = NULL;
                }
            }
        }
    }

    // A client must always return a S_OK.
    LOG_DEBUG("MAPINotification_onNotify exit: %lu", cNotifications);
    return S_OK;
}

/**
 * Registers C callback functions when a contact is deleted, inserted or
 * updated.
 *
 * @param deletedMethod The method to call when a contact has been deleted.
 * @param insertedMethod The method to call when a contact has been inserted.
 * @param updatedMethod The method to call when a contact has been updated.
 */
void
MAPINotification_registerNativeNotificationsDelegate
    (void * deletedMethod, void * insertedMethod, void *updatedMethod)
{
    // If this function is called once again, then check first to unregister
    // previous notification advises.
    MAPINotification_unregisterNativeNotificationsDelegate();

    MAPINotification_callDeletedMethod = (void (*)(char*)) deletedMethod;
    MAPINotification_callInsertedMethod = (void (*)(char*)) insertedMethod;
    MAPINotification_callUpdatedMethod = (void (*)(char*)) updatedMethod;

    LOG_INFO("Registered native notification delegate");
}

/**
 * Opens all the message store and register to notifications.
 * 
 * !! The caller must hold the MAPISession_lock !!
 *
 * @param mapiSession The current MAPI session.
 */
void MAPINotification_registerNotifyAllMsgStores(LPMAPISESSION mapiSession)
{
    HRESULT hResult;

    hResult = mapiSession->GetMsgStoresTable(
            0, 
            &MAPINotification_msgStoresTable);
    if(HR_SUCCEEDED(hResult) && (MAPINotification_msgStoresTable != NULL))
    {
        LOG_DEBUG("GetMsgStoresTable succeeded");
        MAPINotification_msgStoresTableConnection
            = MAPINotification_registerNotifyTable(
                    MAPINotification_msgStoresTable,
                    &MAPINotification_msgStoresTableAdviseSink);

        hResult = MAPINotification_msgStoresTable->SeekRow(
                BOOKMARK_BEGINNING,
                0,
                NULL);
        if (HR_SUCCEEDED(hResult))
        {
            LOG_DEBUG("SeekRow succeeded");
            LPSRowSet rows = NULL;
            hResult = HrQueryAllRows(
                    MAPINotification_msgStoresTable,
                    NULL,
                    NULL,
                    NULL,
                    0,
                    &rows);
            if (HR_SUCCEEDED(hResult) && (rows != NULL))
            {
                MAPINotification_nbMsgStores = rows->cRows;
                LOG_DEBUG("HrQueryAllRows succeeded: %lu", MAPINotification_nbMsgStores);

                if (MAPINotification_nbMsgStores > 0)
                {
                    MAPINotification_msgStores
                        = (LPMDB*) malloc(MAPINotification_nbMsgStores * sizeof(LPMDB));
                    MAPINotification_msgStoresConnection
                        = (ULONG*) malloc(MAPINotification_nbMsgStores * sizeof(ULONG));
                    MAPINotification_adviseSinks = (LPMAPIADVISESINK*)
                        malloc(MAPINotification_nbMsgStores * sizeof(LPMAPIADVISESINK));
                    LOG_DEBUG("stores %p, connection %p, sinks %p",
                              MAPINotification_msgStores,
                              MAPINotification_msgStoresConnection,
                              MAPINotification_adviseSinks);

                    if((MAPINotification_msgStores != NULL) &&
                       (MAPINotification_msgStoresConnection != NULL) &&
                       (MAPINotification_adviseSinks != NULL))
                    {
                        memset(MAPINotification_msgStores,
                               0,
                               MAPINotification_nbMsgStores * sizeof(LPMDB));
                        memset(MAPINotification_msgStoresConnection,
                               0,
                               MAPINotification_nbMsgStores * sizeof(ULONG));
                        memset(MAPINotification_adviseSinks,
                               0,
                               MAPINotification_nbMsgStores * sizeof(LPMAPIADVISESINK));

                        LOG_DEBUG("Look at message stores...");

                        for(unsigned int r = 0; r < MAPINotification_nbMsgStores; ++r)
                        {
                            SRow row = rows->aRow[r];
                            ULONG i;
                            ULONG objType = 0;
                            SBinary entryIDBinary = { 0, NULL };

                            for(i = 0; i < row.cValues; ++i)
                            {
                                LPSPropValue prop = (row.lpProps) + i;

                                switch (prop->ulPropTag)
                                {
                                    case PR_OBJECT_TYPE:
                                        objType = prop->Value.ul;
                                        break;
                                    case PR_ENTRYID:
                                        entryIDBinary = prop->Value.bin;
                                        break;
                                }
                            }

                            if(objType && entryIDBinary.cb && entryIDBinary.lpb)
                            {
                                hResult = mapiSession->OpenMsgStore(
                                        0,
                                        entryIDBinary.cb,
                                        (LPENTRYID) entryIDBinary.lpb,
                                        NULL,
                                        MDB_NO_MAIL
                                            | MAPINotification_openEntryUlFlags,
                                        &MAPINotification_msgStores[r]);
                                if (HR_SUCCEEDED(hResult))
                                {
                                    LOG_DEBUG("OpenMsgStore succeeded for row %d", r);
                                    MAPINotification_msgStoresConnection[r]
                                        = MAPINotification_registerNotifyMessageDataBase(
                                                MAPINotification_msgStores[r],
                                                &MAPINotification_adviseSinks[r]);
                                }
                                else
                                {
                                    LOG_ERROR("OpenMsgStore failed on row %d: %x",
                                              r, hResult);
                                    MAPINotification_msgStores[r] = NULL;
                                }
                            }
                            else
                            {
                              LOG_INFO("No info for row %d (type %lu, cb %lu, lpb %d)",
                                       r, objType, entryIDBinary.cb, entryIDBinary.lpb);
                            }
                        }
                    }
                    else
                    {
                      LOG_ERROR("Couldn't malloc space for stores");
                    }
                }
                else
                {
                    LOG_ERROR("No message stores");
                }
                FreeProws(rows);
            }
            else
            {
                LOG_ERROR("HrQueryAllRows failed: %x %p", hResult, rows);
            }
        }
        else
        {
            LOG_ERROR("SeekRow failed: %x", hResult);
        }
    }
    else
    {
        LOG_ERROR("GetMsgStoresTable failed: %x %p", hResult, MAPINotification_msgStoresTable);
    }
}


/**
 * Registers to notification for the given message data base.
 *
 * @param iUnknown The data base to register to in order to receive events.
 * @param adviseSink The advice sink that will be generated resulting o fthis
 * function call.
 * 
 * !! The caller must hold the MAPISession_lock !!
 *
 * @return An unsigned long which is a token which must be used to call the
 * unadvise function for the same message data base.
 */
ULONG MAPINotification_registerNotifyMessageDataBase(
        LPMDB iUnknown,
        LPMAPIADVISESINK * adviseSink)
{
    HrAllocAdviseSink(&MAPINotification_onNotify, iUnknown, adviseSink);
    LOG_DEBUG("_registerNotifyMessageDataBase:HrAllocAdviseSink OK %p", *adviseSink);

    // There's a mismatch between MinGW's mapidefs.h and the latest MSDN
    // mapidefs.h in that the final parameter to IMsgStore::Advise should be of
    // type ULONG_PTR*, not ULONG* (as in MinGW).  So we don't scribble when
    // running against 64-bit outlook, declare nbConnection to be of type
    // ULONG_PTR (which is 8 bytes wide on 64-bit); and then to get around the
    // MinGW compiler, cast it to ULONG*.
    ULONG_PTR nbConnection = 0;
    iUnknown->Advise(
            (ULONG) 0,
            (LPENTRYID) NULL,
            MAPINotification_EVENT_MASK,
            *adviseSink,
            (ULONG *) &nbConnection);
    LOG_DEBUG("nbConnection: %lu", nbConnection);

    return nbConnection;
}

/**
 * Registers a callback function for when the message store table changes.
 *
 * @param iUnknown The message store table to register to in order to receive
 * events.
 * @param adviseSink The advice sink that will be generated resulting of this
 * function call.
 *
 * @return A unsigned long which is a token which must be used to call the
 * unadvise function for the same message store table.
 */
ULONG MAPINotification_registerNotifyTable(
        LPMAPITABLE iUnknown,
        LPMAPIADVISESINK * adviseSink)
{
    HrAllocAdviseSink(&MAPINotification_tableChanged, iUnknown, adviseSink);
    LOG_DEBUG("_registerNotifyTable:HrAllocAdviseSink OK: %p", *adviseSink);
    // There's a mismatch between MinGW's mapidefs.h and the latest MSDN
    // mapidefs.h in that the final parameter to IMsgTable::Advise should be of
    // type ULONG_PTR*, not ULONG* (as in MinGW).  So we don't scribble when
    // running against 64-bit outlook, declare nbConnection to be of type
    // ULONG_PTR (which is 8 bytes wide on 64-bit); and then to get around the
    // MinGW compiler, cast it to ULONG*.
    ULONG_PTR nbConnection = 0;
    iUnknown->Advise(fnevTableModified, *adviseSink, (ULONG *) &nbConnection);
    LOG_DEBUG("nbConnection: %lu", nbConnection);

    return nbConnection;
}

/**
 * Function called when a message store table changed.
 */
LONG
STDAPICALLTYPE MAPINotification_tableChanged
    (LPVOID lpvContext, ULONG cNotifications, LPNOTIFICATION lpNotifications)
{
    LOG_INFO("MAPINotification_tableChanged enter - %d notifications at 0x%x", cNotifications, lpNotifications);
    if(lpNotifications->ulEventType == fnevTableModified
            && (lpNotifications->info.tab.ulTableEvent == TABLE_CHANGED
                || lpNotifications->info.tab.ulTableEvent == TABLE_ERROR
                || lpNotifications->info.tab.ulTableEvent == TABLE_RELOAD
                || lpNotifications->info.tab.ulTableEvent == TABLE_ROW_ADDED
                || lpNotifications->info.tab.ulTableEvent == TABLE_ROW_DELETED))
    {
        MAPISession_lock();
        LOG_INFO("Un- and then re-register the message stores");
        // Frees and recreates all the notification for the table.
        MAPINotification_unregisterNotifyAllMsgStores();
        MAPINotification_registerNotifyAllMsgStores(
                MAPISession_getMapiSession());
        MAPISession_unlock();
    }

    // A client must always return a S_OK.
    LOG_INFO("MAPINotification_tableChanged exit");
    return S_OK;
}

/**
 * Unregisters C callback functions when a contact is deleted, inserted or
 * updated.
 */
void MAPINotification_unregisterNativeNotificationsDelegate()
{
}

/**
 * Frees all memory used to keep in mind the list of the message store and
 * unregister each of them from the notifications.
 */
void MAPINotification_unregisterNotifyAllMsgStores(void)
{
    if(MAPINotification_msgStoresConnection != NULL)
    {
        for(unsigned int i = 0; i < MAPINotification_nbMsgStores; ++i)
        {
            if(MAPINotification_msgStoresConnection[i] != 0)
            {
                MAPINotification_adviseSinks[i]->Release();
                MAPINotification_msgStores[i]->Unadvise(
                        MAPINotification_msgStoresConnection[i]);
            }
        }
        free(MAPINotification_adviseSinks);
        MAPINotification_adviseSinks = NULL;
        free(MAPINotification_msgStoresConnection);
        MAPINotification_msgStoresConnection = NULL;
    }

    if(MAPINotification_msgStores != NULL)
    {
        for(unsigned int i = 0; i < MAPINotification_nbMsgStores; ++i)
        {
            if(MAPINotification_msgStores[i] != NULL)
            {
                MAPINotification_msgStores[i]->Release();
            }
        }
        free(MAPINotification_msgStores);
        MAPINotification_msgStores = NULL;
    }

    if(MAPINotification_msgStoresTable != NULL)
    {
        MAPINotification_msgStoresTableAdviseSink->Release();
        MAPINotification_msgStoresTableAdviseSink = NULL;
        MAPINotification_msgStoresTable->Unadvise(
                MAPINotification_msgStoresTableConnection);
        MAPINotification_msgStoresTable->Release();
        MAPINotification_msgStoresTable = NULL;
    }
}
