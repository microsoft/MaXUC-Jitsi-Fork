/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRESSBOOK_MAPINOTIFICATION_H_
#define _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRESSBOOK_MAPINOTIFICATION_H_

#include "../Logger.h"

#ifdef __cplusplus
extern "C" {
#endif

#include <mapidefs.h>
#include <mapix.h>

/**
 * Manages notification for the message data base (used to get the list of
 * contact).
 *
 * @author Vincent Lucas
 */

boolean MAPINotification_callCallbackMethod(LPSTR iUnknown, void * object);

void MAPINotification_jniCallDeletedMethod(LPSTR iUnknown);
void MAPINotification_jniCallInsertedMethod(LPSTR iUnknown);
void MAPINotification_jniCallUpdatedMethod(LPSTR iUnknown);
void MAPINotification_jniCallQueryCompletedMethod();

LONG
STDAPICALLTYPE MAPINotification_onNotify
    (LPVOID lpvContext, ULONG cNotifications, LPNOTIFICATION lpNotifications);

void
MAPINotification_registerNativeNotificationsDelegate
    (void * deletedMethod, void * insertedMethod, void *updatedMethod);
void MAPINotification_registerNotifyAllMsgStores(LPMAPISESSION mapiSession);

void MAPINotification_unregisterNativeNotificationsDelegate();
void MAPINotification_unregisterNotifyAllMsgStores(void);


#ifdef __cplusplus
}
#endif

#endif /* #ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRESSBOOK_MAPINOTIFICATION_H_ */
