/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.

#ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRESSBOOK_ADDRBOOKCONTACTQUERY_H_
#define _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRESSBOOK_ADDRBOOKCONTACTQUERY_H_

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif /* #ifdef __cplusplus */

jmethodID AddrBookContactQuery_getPtrCallbackMethodID
    (JNIEnv *jniEnv, jobject callback);

jmethodID AddrBookContactQuery_getPtrQueryCompleteMethodID
    (JNIEnv *jniEnv, jobject callback);

#ifdef __cplusplus
}
#endif /* #ifdef __cplusplus */

#endif /* #ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRESSBOOK_ADDRBOOKCONTACTQUERY_H_ */
