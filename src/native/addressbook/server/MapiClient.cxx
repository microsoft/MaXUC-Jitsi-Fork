// Copyright (c) Microsoft Corporation. All rights reserved.

#include "../MAPIBitness.h"
#include "MAPISession.h"
#include "MsOutlookAddrBookContactSourceService.h"
#include "../Logger.h"
#include "MapiClient.h"
#include "RpcClient.h"

#define MAPI_NO_COINIT 8

HRESULT hMAPI = 0;

void mapi_deleted(LPSTR id);
void mapi_inserted(LPSTR id);
void mapi_updated(LPSTR id);

bool start_mapi()
{
    // Initialize the critical section which protects the MAPI interface
    MAPISession_initLock();

    LOG_INFO("About to init native MAPI interface");

    // Start the MAPI Interface
    hMAPI = MsOutlookAddrBookContactSourceService_NativeMAPIInitialize(
                MAPI_INIT_VERSION,
                MAPI_MULTITHREAD_NOTIFICATIONS,
                (void*) mapi_deleted,
                (void*) mapi_inserted,
                (void*) mapi_updated);

    if(hMAPI == S_OK)
    {
        LOG_INFO("Initialized native MAPI interface");
        return true;
    }
    else
    {
        LOG_ERROR("Failed to initialize native MAPI interface: %lx", hMAPI);

        MAPISession_freeLock();

        return false;
    }
}

void stop_mapi()
{
    LOG_DEBUG("stop_mapi");
    MsOutlookAddrBookContactSourceService_NativeMAPIUninitialize();

    MAPISession_freeLock();

    LOG_INFO("stop_mapi completed");
}

unsigned long mapi_status()
{
  return hMAPI;
}

/**
 * Notify the client that a contact has been deleted from MAPI
 *
 * @param id The contact identifer.
 */
void mapi_deleted(LPSTR id)
{
  LOG_INFO("MAPI Notification of deletion: %s", id);
  client_contact_deleted(id);
}

/**
 * Notify the client that a contact has been inserted into MAPI
 *
 * @param id The contact identifer.
 */
void mapi_inserted(LPSTR id)
{
  LOG_INFO("MAPI Notification of insertion: %s", id);
  client_contact_inserted(id);
}

/**
 * Notify the client that a contact has been updated in MAPI
 *
 * @param id The contact identifer.
 */
void mapi_updated(LPSTR id)
{
  LOG_INFO("MAPI Notification of update: %s", id);
  client_contact_updated(id);
}
