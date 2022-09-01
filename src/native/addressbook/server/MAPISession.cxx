/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "MAPISession.h"
#include "../Logger.h"

static LPMAPISESSION MAPISession_mapiSession = NULL;
static CRITICAL_SECTION MAPISession_mapiSessionCriticalSection;
static int MAPISession_lockCount = 0;

/**
 * Returns the current mapi session which have been created using the
 * MAPILogonEx function.
 *
 * @return The current mapi session which have been created using the
 * MAPILogonEx function. NULL if no session is currently opened.
 */
LPMAPISESSION MAPISession_getMapiSession(void)
{
    return MAPISession_mapiSession;
}

/**
 * Sets the current mapi session which have been created using the
 * MAPILogonEx function.
 *
 * @param mapiSession The current mapi session which have been created using the
 * MAPILogonEx function.
 */
void MAPISession_setMapiSession(LPMAPISESSION mapiSession)
{
    LOG_INFO("Setting Mapi session to 0x%x", mapiSession);
    MAPISession_mapiSession = mapiSession;
}

void MAPISession_initLock()
{
    LOG_INFO("Init lock on session 0x%x", MAPISession_mapiSession);
    InitializeCriticalSection(&MAPISession_mapiSessionCriticalSection);
}

void MAPISession_lock()
{
    EnterCriticalSection(&MAPISession_mapiSessionCriticalSection);
    MAPISession_lockCount++;
    LOG_TRACE("Locked on session 0x%x, lock count now %d", MAPISession_mapiSession, MAPISession_lockCount);
}

void MAPISession_unlock()
{
    MAPISession_lockCount--;
    LOG_TRACE("Unlock on session 0x%x, lock count now %d", MAPISession_mapiSession, MAPISession_lockCount);
    LeaveCriticalSection(&MAPISession_mapiSessionCriticalSection);
}

void MAPISession_freeLock()
{
    LOG_INFO("Free lock on session 0x%x", MAPISession_mapiSession);
    DeleteCriticalSection(&MAPISession_mapiSessionCriticalSection);
}
