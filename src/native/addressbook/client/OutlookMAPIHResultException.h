/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.

#ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRESSBOOK_OUTLOOKMAPIHRESULTEXCEPTION_H_
#define _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRESSBOOK_OUTLOOKMAPIHRESULTEXCEPTION_H_

#include <jni.h>
#include <Mapix.h>
#include <tchar.h>

#ifdef __cplusplus
extern "C" {
#endif /* #ifdef __cplusplus */

void OutlookMAPIHResultException_throwNew
    (JNIEnv *jniEnv, HRESULT hResult, LPCSTR file, ULONG line);

#ifdef __cplusplus
}
#endif /* #ifdef __cplusplus */

#endif /* _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRESSBOOK_OUTLOOKMAPIHRESULTEXCEPTION_ */
