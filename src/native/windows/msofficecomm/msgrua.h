

/* this ALWAYS GENERATED file contains the definitions for the interfaces */


 /* File created by MIDL compiler version 7.00.0555 */
/* at Tue Jan 26 16:59:46 2016
 */
/* Compiler settings for C:\Program Files (x86)\Microsoft Office Communicator\SDK\msgrua.idl:
    Oicf, W1, Zp8, env=Win32 (32b run), target_arch=X86 7.00.0555 
    protocol : dce , ms_ext, c_ext, robust
    error checks: allocation ref bounds_check enum stub_data 
    VC __declspec() decoration level: 
         __declspec(uuid()), __declspec(selectany), __declspec(novtable)
         DECLSPEC_UUID(), MIDL_INTERFACE()
*/
/* @@MIDL_FILE_HEADING(  ) */

#pragma warning( disable: 4049 )  /* more than 64k source lines */


/* verify that the <rpcndr.h> version is high enough to compile this file*/
#ifndef __REQUIRED_RPCNDR_H_VERSION__
#define __REQUIRED_RPCNDR_H_VERSION__ 475
#endif

#include "rpc.h"
#include "rpcndr.h"

#ifndef __RPCNDR_H_VERSION__
#error this stub requires an updated version of <rpcndr.h>
#endif // __RPCNDR_H_VERSION__


#ifndef __msgrua_h__
#define __msgrua_h__

#if defined(_MSC_VER) && (_MSC_VER >= 1020)
#pragma once
#endif

/* Forward Declarations */ 

#ifndef __IMessenger_FWD_DEFINED__
#define __IMessenger_FWD_DEFINED__
typedef interface IMessenger IMessenger;
#endif 	/* __IMessenger_FWD_DEFINED__ */


#ifndef __IMessenger2_FWD_DEFINED__
#define __IMessenger2_FWD_DEFINED__
typedef interface IMessenger2 IMessenger2;
#endif 	/* __IMessenger2_FWD_DEFINED__ */


#ifndef __IMessenger3_FWD_DEFINED__
#define __IMessenger3_FWD_DEFINED__
typedef interface IMessenger3 IMessenger3;
#endif 	/* __IMessenger3_FWD_DEFINED__ */


#ifndef __IMessengerAdvanced_FWD_DEFINED__
#define __IMessengerAdvanced_FWD_DEFINED__
typedef interface IMessengerAdvanced IMessengerAdvanced;
#endif 	/* __IMessengerAdvanced_FWD_DEFINED__ */


#ifndef __IMessengerContactResolution_FWD_DEFINED__
#define __IMessengerContactResolution_FWD_DEFINED__
typedef interface IMessengerContactResolution IMessengerContactResolution;
#endif 	/* __IMessengerContactResolution_FWD_DEFINED__ */


#ifndef __DMessengerEvents_FWD_DEFINED__
#define __DMessengerEvents_FWD_DEFINED__
typedef interface DMessengerEvents DMessengerEvents;
#endif 	/* __DMessengerEvents_FWD_DEFINED__ */


#ifndef __IMessengerWindow_FWD_DEFINED__
#define __IMessengerWindow_FWD_DEFINED__
typedef interface IMessengerWindow IMessengerWindow;
#endif 	/* __IMessengerWindow_FWD_DEFINED__ */


#ifndef __IMessengerConversationWnd_FWD_DEFINED__
#define __IMessengerConversationWnd_FWD_DEFINED__
typedef interface IMessengerConversationWnd IMessengerConversationWnd;
#endif 	/* __IMessengerConversationWnd_FWD_DEFINED__ */


#ifndef __IMessengerConversationWndAdvanced_FWD_DEFINED__
#define __IMessengerConversationWndAdvanced_FWD_DEFINED__
typedef interface IMessengerConversationWndAdvanced IMessengerConversationWndAdvanced;
#endif 	/* __IMessengerConversationWndAdvanced_FWD_DEFINED__ */


#ifndef __IMessengerContact_FWD_DEFINED__
#define __IMessengerContact_FWD_DEFINED__
typedef interface IMessengerContact IMessengerContact;
#endif 	/* __IMessengerContact_FWD_DEFINED__ */


#ifndef __IMessengerContactAdvanced_FWD_DEFINED__
#define __IMessengerContactAdvanced_FWD_DEFINED__
typedef interface IMessengerContactAdvanced IMessengerContactAdvanced;
#endif 	/* __IMessengerContactAdvanced_FWD_DEFINED__ */


#ifndef __IMessengerContacts_FWD_DEFINED__
#define __IMessengerContacts_FWD_DEFINED__
typedef interface IMessengerContacts IMessengerContacts;
#endif 	/* __IMessengerContacts_FWD_DEFINED__ */


#ifndef __IMessengerService_FWD_DEFINED__
#define __IMessengerService_FWD_DEFINED__
typedef interface IMessengerService IMessengerService;
#endif 	/* __IMessengerService_FWD_DEFINED__ */


#ifndef __IMessengerServices_FWD_DEFINED__
#define __IMessengerServices_FWD_DEFINED__
typedef interface IMessengerServices IMessengerServices;
#endif 	/* __IMessengerServices_FWD_DEFINED__ */


#ifndef __IMessengerGroup_FWD_DEFINED__
#define __IMessengerGroup_FWD_DEFINED__
typedef interface IMessengerGroup IMessengerGroup;
#endif 	/* __IMessengerGroup_FWD_DEFINED__ */


#ifndef __IMessengerGroups_FWD_DEFINED__
#define __IMessengerGroups_FWD_DEFINED__
typedef interface IMessengerGroups IMessengerGroups;
#endif 	/* __IMessengerGroups_FWD_DEFINED__ */


#ifndef __Messenger_FWD_DEFINED__
#define __Messenger_FWD_DEFINED__

#ifdef __cplusplus
typedef class Messenger Messenger;
#else
typedef struct Messenger Messenger;
#endif /* __cplusplus */

#endif 	/* __Messenger_FWD_DEFINED__ */


/* header files for imported files */
#include "ocidl.h"

#ifdef __cplusplus
extern "C"{
#endif 


/* interface __MIDL_itf_msgrua_0000_0000 */
/* [local] */ 

//+-------------------------------------------------------------------------
//
//  Microsoft Windows
//  Copyright (c) Microsoft Corporation.  All rights reserved.
//
//  File: msgrua.h
//
//--------------------------------------------------------------------------
#pragma once


extern RPC_IF_HANDLE __MIDL_itf_msgrua_0000_0000_v0_0_c_ifspec;
extern RPC_IF_HANDLE __MIDL_itf_msgrua_0000_0000_v0_0_s_ifspec;


#ifndef __CommunicatorAPI_LIBRARY_DEFINED__
#define __CommunicatorAPI_LIBRARY_DEFINED__

/* library CommunicatorAPI */
/* [helpstring][version][uuid] */ 


















#ifndef _MSGR_CONSTANTS_
#define _MSGR_CONSTANTS_
#define MSGR_S(e) ((HRESULT)(0x01000300 + (e)))
#define MSGR_E(e) ((HRESULT)(0x81000300 + (e)))
#define MSGR_E_CONNECT                          MSGR_E(0x0001)
#define MSGR_E_INVALID_SERVER_NAME              MSGR_E(0x0002)
#define MSGR_E_INVALID_PASSWORD                 MSGR_E(0x0003)
#define MSGR_E_ALREADY_LOGGED_ON                MSGR_E(0x0004)
#define MSGR_E_SERVER_VERSION                   MSGR_E(0x0005)
#define MSGR_E_LOGON_TIMEOUT                    MSGR_E(0x0006)
#define MSGR_E_LIST_FULL                        MSGR_E(0x0007)
#define MSGR_E_AI_REJECT                        MSGR_E(0x0008)
#define MSGR_E_AI_REJECT_NOT_INST               MSGR_E(0x0009)
#define MSGR_E_USER_NOT_FOUND                   MSGR_E(0x000A)
#define MSGR_E_ALREADY_IN_LIST                  MSGR_E(0x000B)
#define MSGR_E_DISCONNECTED                     MSGR_E(0x000C)
#define MSGR_E_UNEXPECTED                       MSGR_E(0x000D)
#define MSGR_E_SERVER_TOO_BUSY                  MSGR_E(0x000E)
#define MSGR_E_INVALID_AUTH_PACKAGES            MSGR_E(0x000F)
#define MSGR_E_NEWER_CLIENT_AVAILABLE           MSGR_E(0x0010)
#define MSGR_E_AI_TIMEOUT                       MSGR_E(0x0011)
#define MSGR_E_CANCEL                           MSGR_E(0x0012)
#define MSGR_E_TOO_MANY_MATCHES                 MSGR_E(0x0013)
#define MSGR_E_SERVER_UNAVAILABLE               MSGR_E(0x0014)
#define MSGR_E_LOGON_UI_ACTIVE                  MSGR_E(0x0015)
#define MSGR_E_OPTION_UI_ACTIVE                 MSGR_E(0x0016)
#define MSGR_E_CONTACT_UI_ACTIVE                MSGR_E(0x0017)
#define MSGR_E_PRIMARY_SERVICE_NOT_LOGGED_ON    MSGR_E(0x0018)
#define MSGR_E_LOGGED_ON                        MSGR_E(0x0019)
#define MSGR_E_CONNECT_PROXY                    MSGR_E(0x001A)
#define MSGR_E_PROXY_AUTH                       MSGR_E(0x001B)
#define MSGR_E_PROXY_AUTH_TYPE                  MSGR_E(0x001C)
#define MSGR_E_INVALID_PROXY_NAME               MSGR_E(0x001D)
#define MSGR_E_NOT_LOGGED_ON                    MSGR_E(0x001E)
#define MSGR_E_POPUP_UI_ACTIVE                  MSGR_E(0x001F)
#define MSGR_E_NOT_PRIMARY_SERVICE              MSGR_E(0x0020)
#define MSGR_E_TOO_MANY_SESSIONS                MSGR_E(0x0021)
#define MSGR_E_TOO_MANY_MESSAGES                MSGR_E(0x0022)
#define MSGR_E_REMOTE_LOGIN                     MSGR_E(0x0023)
#define MSGR_E_INVALID_FRIENDLY_NAME            MSGR_E(0x0024)
#define MSGR_E_SESSION_FULL                     MSGR_E(0x0025)
#define MSGR_E_NOT_ALLOWING_NEW_USERS           MSGR_E(0x0026)
#define MSGR_E_INVALID_DOMAIN                   MSGR_E(0x0027)
#define MSGR_E_TCP_ERROR                        MSGR_E(0x0028)
#define MSGR_E_SESSION_TIMEOUT                  MSGR_E(0x0029)
#define MSGR_E_MULTIPOINT_SESSION_BEGIN_TIMEOUT MSGR_E(0x002a)
#define MSGR_E_MULTIPOINT_SESSION_END_TIMEOUT   MSGR_E(0x002b)
#define MSGR_E_REVERSE_LIST_FULL                MSGR_E(0x002c)
#define MSGR_E_SERVER_ERROR                     MSGR_E(0x002d)
#define MSGR_E_SYSTEM_CONFIG                    MSGR_E(0x002e)
#define MSGR_E_NO_DIRECTORY                     MSGR_E(0x002f)
#define MSGR_E_RETRY_SET                        MSGR_E(0x0030)
#define MSGR_E_CHILD_WITHOUT_CONSENT            MSGR_E(0x0031)
#define MSGR_E_USER_CANCELLED                   MSGR_E(0x0032)
#define MSGR_E_CANCEL_BEFORE_CONNECT            MSGR_E(0x0033)
#define MSGR_E_VOICE_IM_TIMEOUT                 MSGR_E(0x0034)
#define MSGR_E_NOT_ACCEPTING_PAGES              MSGR_E(0x0035)
#define MSGR_E_EMAIL_PASSPORT_NOT_VALIDATED     MSGR_E(0x0036)
#define MSGR_E_AUDIO_UI_ACTIVE                  MSGR_E(0x0037)
#define MSGR_E_NO_HARDWARE                      MSGR_E(0x0038)
#define MSGR_E_PAGING_UNAVAILABLE               MSGR_E(0x0039)
#define MSGR_E_PHONE_INVALID_NUMBER             MSGR_E(0x003a)
#define MSGR_E_PHONE_NO_FUNDS                   MSGR_E(0x003b)
#define MSGR_E_VOICE_NO_ANSWER                  MSGR_E(0x003c)
#define MSGR_E_VOICE_WAVEIN_DEVICE              MSGR_E(0x003d)
#define MSGR_E_FT_TIMEOUT                       MSGR_E(0x003e)
#define MSGR_E_MESSAGE_TOO_LONG                 MSGR_E(0x003f)
#define MSGR_E_VOICE_FIREWALL                   MSGR_E(0x0040)
#define MSGR_E_VOICE_NETCONN                    MSGR_E(0x0041)
#define MSGR_E_PHONE_CIRCUITS_BUSY              MSGR_E(0x0042)
#define MSGR_E_SERVER_PROTOCOL                  MSGR_E(0x0043)
#define MSGR_E_UNAVAILABLE_VIA_HTTP             MSGR_E(0x0044)
#define MSGR_E_PHONE_INVALID_PIN                MSGR_E(0x0045)
#define MSGR_E_PHONE_PINPROCEED_TIMEOUT         MSGR_E(0x0046)
#define MSGR_E_SERVER_SHUTDOWN                  MSGR_E(0x0047)
#define MSGR_E_CLIENT_DISALLOWED                MSGR_E(0x0048)
#define MSGR_E_PHONE_CALL_NOT_COMPLETE          MSGR_E(0x0049)
#define MSGR_E_GROUPS_NOT_ENABLED               MSGR_E(0x004a)
#define MSGR_E_GROUP_ALREADY_EXISTS             MSGR_E(0x004b)
#define MSGR_E_TOO_MANY_GROUPS                  MSGR_E(0x004c)
#define MSGR_E_GROUP_DOES_NOT_EXIST             MSGR_E(0x004d)
#define MSGR_E_USER_NOT_GROUP_MEMBER            MSGR_E(0x004e)
#define MSGR_E_GROUP_NAME_TOO_LONG              MSGR_E(0x004f)
#define MSGR_E_GROUP_NOT_EMPTY                  MSGR_E(0x0050)
#define MSGR_E_BAD_GROUP_NAME                   MSGR_E(0x0051)
#define MSGR_E_PHONESERVICE_UNAVAILABLE         MSGR_E(0x0052)
#define MSGR_E_CANNOT_RENAME                    MSGR_E(0x0053)
#define MSGR_E_CANNOT_DELETE                    MSGR_E(0x0054)
#define MSGR_E_INVALID_SERVICE                  MSGR_E(0x0055)
#define MSGR_E_POLICY_RESTRICTED                MSGR_E(0x0056)
#define MSGR_E_BUSY                             MSGR_E(0x0057)
#define MSGR_E_DNS_SRV_FAIL                     MSGR_E(0x0058)
#define MSGR_E_DNS_A_RES_FAIL                   MSGR_E(0x0059)
#define MSGR_E_NO_SERVER_ADDRESS_SPECIFIED      MSGR_E(0x0060)
#define MSGR_E_TLS_FAIL                         MSGR_E(0x0061)
#define MSGR_E_INCOMPATIBLE_ENCRYPTION          MSGR_E(0x0062)
#define MSGR_E_SSL_TUNNEL_FAILED                MSGR_E(0x0063)
#define MSGR_E_SIP_TIMEOUT                      MSGR_E(0x0064)
#define MSGR_E_INCOMPATIBLE_IM                  MSGR_E(0x0065)
#define MSGR_E_MIM_ADD_TO_CONTACTS_FAIL         MSGR_E(0x0066)
#define MSGR_E_INVALID_ADDRESS_FORMAT           MSGR_E(0x0067)
#define MSGR_E_INVALID_CERTIFICATE              MSGR_E(0x0068)
#define MSGR_E_AUTH_TIME_SKEW                   MSGR_E(0x0069)
#define MSGR_E_CHANGED_CREDENTIALS              MSGR_E(0x0070)
#define MSGR_E_SIP_LOGIN_FORBIDDEN              MSGR_E(0x0071)
#define MSGR_E_SIP_HIGH_SECURITY_SET_TLS        MSGR_E(0x0072)
#define MSGR_E_CALLEE_INSUFFICIENT_SECURITY_LEVEL MSGR_E(0x0073)
#define MSGR_E_CALLER_PEER2PEER_CALLS_NOT_ALLOWED MSGR_E(0x0074)
#define MSGR_E_SIP_UDP_UNSUPPORTED                MSGR_E(0x0075)
#define MSGR_E_SIP_SEARCH_FORBIDDEN               MSGR_E(0x0076)
#define MSGR_E_INVALID_SERVER_VERSION           MSGR_E(0x0077)
#define MSGR_E_AUTH_SERVER_UNAVAILABLE          MSGR_E(0x0078)
#define MSGR_E_SESSION_RESTRICTED               MSGR_E(0x0080)
#define MSGR_E_MANAGED_USER_INVALID_CVR         MSGR_E(0x0081)
#define MSGR_E_RESTRICTED_USER                  MSGR_E(0x0082)
#define MSGR_E_PROXY_AUTH_REQUIRED              MSGR_E(0x0083)
#define MSGR_E_PROXY_REALM_MISMATCH             MSGR_E(0x0084)
#define MSGR_E_PROXY_PASSWORD_INCORRECT         MSGR_E(0x0085)
#define MSGR_E_RESTRICTED_USER_LOGON_RESTRICED  MSGR_E(0x0086)
#define MSGR_E_IE_OFFLINE                       MSGR_E(0x0087)
#define MSGR_E_IE_CANT_CONNECT                  MSGR_E(0x0088)
#define MSGR_E_ACCOUNT_ERROR_REDIRECT           MSGR_E(0x0089)
#define MSGR_E_INCOMING_INVITE_CANCELLED        MSGR_E(0x0090)
#define MSGR_E_INVITE_ACCEPTED_ELSEWHERE        MSGR_E(0x0091)
#define MSGR_E_APP_INVITE_NOT_ACCEPTABLE        MSGR_E(0x0092)
#define MSGR_S_ALREADY_IN_THE_MODE              MSGR_S(0x0001)
#define MSGR_S_TRANSFER_SEND_BEGUN              MSGR_S(0x0002)
#define MSGR_S_TRANSFER_SEND_FINISHED           MSGR_S(0x0003)
#define MSGR_S_TRANSFER_RECEIVE_BEGUN           MSGR_S(0x0004)
#define MSGR_S_TRANSFER_RECEIVE_FINISHED        MSGR_S(0x0005)
#define MSGR_S_GROUP_ALREADY_EXISTS             MSGR_S(0x0006)
#define MSGR_S_SESSION_PENDING                  MSGR_S(0x0007)
#define MSGR_S_CONV_ID_MATCH                    MSGR_S(0x0008)
#endif
typedef /* [public][public][public][public][public][public][public] */ 
enum __MIDL___MIDL_itf_msgrua_0000_0000_0001
    {	MISTATUS_UNKNOWN	= 0,
	MISTATUS_OFFLINE	= 0x1,
	MISTATUS_ONLINE	= 0x2,
	MISTATUS_INVISIBLE	= 0x6,
	MISTATUS_BUSY	= 0xa,
	MISTATUS_BE_RIGHT_BACK	= 0xe,
	MISTATUS_IDLE	= 0x12,
	MISTATUS_AWAY	= 0x22,
	MISTATUS_ON_THE_PHONE	= 0x32,
	MISTATUS_OUT_TO_LUNCH	= 0x42,
	MISTATUS_IN_A_MEETING	= 0x52,
	MISTATUS_OUT_OF_OFFICE	= 0x62,
	MISTATUS_DO_NOT_DISTURB	= 0x72,
	MISTATUS_IN_A_CONFERENCE	= 0x82,
	MISTATUS_ALLOW_URGENT_INTERRUPTIONS	= 0x92,
	MISTATUS_MAY_BE_AVAILABLE	= 0xa2,
	MISTATUS_CUSTOM	= 0xb2,
	MISTATUS_LOCAL_FINDING_SERVER	= 0x100,
	MISTATUS_LOCAL_CONNECTING_TO_SERVER	= 0x200,
	MISTATUS_LOCAL_SYNCHRONIZING_WITH_SERVER	= 0x300,
	MISTATUS_LOCAL_DISCONNECTING_FROM_SERVER	= 0x400
    } 	MISTATUS;

typedef /* [public][public][public] */ 
enum __MIDL___MIDL_itf_msgrua_0000_0000_0002
    {	MMESSENGERPROP_VERSION	= 0,
	MMESSENGERPROP_PLCID	= 1
    } 	MMESSENGERPROPERTY;

typedef /* [public][public][public][public][public][public][public] */ 
enum __MIDL___MIDL_itf_msgrua_0000_0000_0003
    {	MCONTACTPROP_INVALID_PROPERTY	= -1,
	MCONTACTPROP_GROUPS_PROPERTY	= 0,
	MCONTACTPROP_EMAIL	= 1
    } 	MCONTACTPROPERTY;

typedef /* [public][public][public] */ 
enum __MIDL___MIDL_itf_msgrua_0000_0000_0004
    {	MWINDOWPROP_INVALID_PROPERTY	= -1,
	MWINDOWPROP_VIEW_SIDEBAR	= 0,
	MWINDOWPROP_VIEW_TOOLBAR	= 1
    } 	MWINDOWPROPERTY;

typedef /* [public][public][public][public][public][public] */ 
enum __MIDL___MIDL_itf_msgrua_0000_0000_0005
    {	MPHONE_TYPE_ALL	= -1,
	MPHONE_TYPE_HOME	= 0,
	MPHONE_TYPE_WORK	= 1,
	MPHONE_TYPE_MOBILE	= 2,
	MPHONE_TYPE_CUSTOM	= 3
    } 	MPHONE_TYPE;

typedef /* [public][public] */ 
enum __MIDL___MIDL_itf_msgrua_0000_0000_0006
    {	MOPT_GENERAL_PAGE	= 0,
	MOPT_PRIVACY_PAGE	= 1,
	MOPT_EXCHANGE_PAGE	= 2,
	MOPT_ACCOUNTS_PAGE	= 3,
	MOPT_CONNECTION_PAGE	= 4,
	MOPT_PREFERENCES_PAGE	= 5,
	MOPT_SERVICES_PAGE	= 6,
	MOPT_PHONE_PAGE	= 7
    } 	MOPTIONPAGE;

typedef /* [public][public][public] */ 
enum __MIDL___MIDL_itf_msgrua_0000_0000_0007
    {	MUAFOLDER_INBOX	= 0,
	MUAFOLDER_ALL_OTHER_FOLDERS	= 1
    } 	MUAFOLDER;

typedef /* [public][public][public] */ 
enum __MIDL___MIDL_itf_msgrua_0000_0000_0008
    {	MSERVICEPROP_INVALID_PROPERTY	= -1
    } 	MSERVICEPROPERTY;

typedef /* [public][public][public] */ 
enum __MIDL___MIDL_itf_msgrua_0000_0000_0009
    {	MUASORT_GROUPS	= 0,
	MUASORT_ONOFFLINE	= 1
    } 	MUASORT;

typedef /* [public][public][public] */ 
enum __MIDL___MIDL_itf_msgrua_0001_0050_0001
    {	ADDRESS_TYPE_SMTP	= 1,
	ADDRESS_TYPE_DISPLAY_NAME	= 2,
	ADDRESS_TYPE_EXTERNAL	= 3
    } 	ADDRESS_TYPE;

typedef /* [public][public] */ 
enum __MIDL___MIDL_itf_msgrua_0001_0050_0002
    {	CONTACT_RESOLUTION_CACHED_ONLY	= 1,
	CONTACT_RESOLUTION_ANY	= 2
    } 	CONTACT_RESOLUTION_TYPE;

typedef /* [public][public] */ 
enum __MIDL___MIDL_itf_msgrua_0001_0050_0003
    {	CONVERSATION_TYPE_IM	= 1,
	CONVERSATION_TYPE_PHONE	= 2,
	CONVERSATION_TYPE_LIVEMEETING	= 4,
	CONVERSATION_TYPE_AUDIO	= 8,
	CONVERSATION_TYPE_VIDEO	= 16,
	CONVERSATION_TYPE_PSTN	= 32
    } 	CONVERSATION_TYPE;

typedef 
enum EXTENDED_STATUS_TYPE
    {	EXTENDED_STATUS_WEB	= 1,
	EXTENDED_STATUS_MOBILE	= 2,
	EXTENDED_STATUS_BLOCKED	= 0x100
    } 	EXTENDED_STATUS_TYPE;

typedef /* [public] */ 
enum __MIDL___MIDL_itf_msgrua_0001_0054_0001
    {	CONTACT_PROP_TITLE	= 0,
	CONTACT_PROP_OFFICE	= 0x1
    } 	CONTACT_PROPERTY;

typedef /* [public] */ 
enum __MIDL___MIDL_itf_msgrua_0001_0054_0002
    {	CALENDAR_STATE_NOT_AVAILABLE	= 0,
	CALENDAR_STATE_FREE	= ( CALENDAR_STATE_NOT_AVAILABLE + 1 ) ,
	CALENDAR_STATE_TENTATIVE	= ( CALENDAR_STATE_FREE + 1 ) ,
	CALENDAR_STATE_NOTWORKING	= ( CALENDAR_STATE_TENTATIVE + 1 ) ,
	CALENDAR_STATE_BUSY	= ( CALENDAR_STATE_NOTWORKING + 1 ) ,
	CALENDAR_STATE_OUT_OF_OFFICE	= ( CALENDAR_STATE_BUSY + 1 ) 
    } 	CALENDAR_STATE;

typedef /* [public] */ 
enum __MIDL___MIDL_itf_msgrua_0001_0054_0003
    {	PRESENCE_PROP_MSTATE	= 0,
	PRESENCE_PROP_AVAILABILITY	= 0x1,
	PRESENCE_PROP_IS_BLOCKED	= 0x2,
	PRESENCE_PROP_PRESENCE_NOTE	= 0x3,
	PRESENCE_PROP_IS_OOF	= 0x4,
	PRESENCE_PROP_TOOL_TIP	= 0x5,
	PRESENCE_PROP_CUSTOM_STATUS_STRING	= 0x6,
	PRESENCE_PROP_DEVICE_TYPE	= 0x7,
	PRESENCE_PROP_CURRENT_CALENDAR_STATE	= 0x8,
	PRESENCE_PROP_NEXT_CALENDAR_STATE	= 0x9,
	PRESENCE_PROP_NEXT_CALENDAR_STATE_TIME	= 0xa,
	PRESENCE_PROP_MAX	= 0xb
    } 	PRESENCE_PROPERTY;


EXTERN_C const IID LIBID_CommunicatorAPI;

#ifndef __IMessenger_INTERFACE_DEFINED__
#define __IMessenger_INTERFACE_DEFINED__

/* interface IMessenger */
/* [object][oleautomation][dual][helpcontext][helpstring][uuid] */ 


EXTERN_C const IID IID_IMessenger;

#if defined(__cplusplus) && !defined(CINTERFACE)
    
    MIDL_INTERFACE("D50C3186-0F89-48f8-B204-3604629DEE10")
    IMessenger : public IDispatch
    {
    public:
        virtual /* [helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_Window( 
            /* [retval][out] */ IDispatch **ppMWindow) = 0;
        
        virtual /* [helpstring][id] */ HRESULT STDMETHODCALLTYPE ViewProfile( 
            /* [in] */ VARIANT vContact) = 0;
        
        virtual /* [helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_ReceiveFileDirectory( 
            /* [retval][out] */ BSTR *bstrPath) = 0;
        
        virtual /* [helpstring][id] */ HRESULT STDMETHODCALLTYPE StartVoice( 
            /* [in] */ VARIANT vContact,
            /* [retval][out] */ IDispatch **ppMWindow) = 0;
        
        virtual /* [helpstring][id] */ HRESULT STDMETHODCALLTYPE InviteApp( 
            /* [in] */ VARIANT vContact,
            /* [in] */ BSTR bstrAppID,
            /* [retval][out] */ IDispatch **ppMWindow) = 0;
        
        virtual /* [helpstring][id] */ HRESULT STDMETHODCALLTYPE SendMail( 
            /* [in] */ VARIANT vContact) = 0;
        
        virtual /* [helpstring][id] */ HRESULT STDMETHODCALLTYPE OpenInbox( void) = 0;
        
        virtual /* [helpstring][id] */ HRESULT STDMETHODCALLTYPE SendFile( 
            /* [in] */ VARIANT vContact,
            /* [in] */ BSTR bstrFileName,
            /* [retval][out] */ IDispatch **ppMWindow) = 0;
        
        virtual /* [helpcontext][helpstring][id] */ HRESULT STDMETHODCALLTYPE Signout( void) = 0;
        
        virtual /* [helpcontext][helpstring][id] */ HRESULT STDMETHODCALLTYPE Signin( 
            /* [in] */ long hwndParent,
            /* [in] */ BSTR bstrSigninName,
            /* [in] */ BSTR bstrPassword) = 0;
        
        virtual /* [helpstring][id] */ HRESULT STDMETHODCALLTYPE GetContact( 
            /* [in] */ BSTR bstrSigninName,
            /* [in] */ BSTR bstrServiceId,
            /* [retval][out] */ IDispatch **ppMContact) = 0;
        
        virtual /* [helpcontext][helpstring][id] */ HRESULT STDMETHODCALLTYPE OptionsPages( 
            /* [in] */ long hwndParent,
            /* [in] */ MOPTIONPAGE mOptionPage) = 0;
        
        virtual /* [helpcontext][helpstring][id] */ HRESULT STDMETHODCALLTYPE AddContact( 
            /* [in] */ long hwndParent,
            /* [in] */ BSTR bstrEMail) = 0;
        
        virtual /* [helpcontext][helpstring][id] */ HRESULT STDMETHODCALLTYPE FindContact( 
            /* [in] */ long hwndParent,
            /* [in] */ BSTR bstrFirstName,
            /* [in] */ BSTR bstrLastName,
            /* [optional][in] */ VARIANT vbstrCity,
            /* [optional][in] */ VARIANT vbstrState,
            /* [optional][in] */ VARIANT vbstrCountry) = 0;
        
        virtual /* [helpcontext][helpstring][id] */ HRESULT STDMETHODCALLTYPE InstantMessage( 
            /* [in] */ VARIANT vContact,
            /* [retval][out] */ IDispatch **ppMWindow) = 0;
        
        virtual /* [helpcontext][helpstring][id] */ HRESULT STDMETHODCALLTYPE Phone( 
            /* [in] */ VARIANT vContact,
            /* [in] */ MPHONE_TYPE ePhoneNumber,
            /* [in] */ BSTR bstrNumber,
            /* [retval][out] */ IDispatch **ppMWindow) = 0;
        
        virtual /* [helpcontext][helpstring][id] */ HRESULT STDMETHODCALLTYPE MediaWizard( 
            /* [in] */ long hwndParent) = 0;
        
        virtual /* [helpcontext][helpstring][id] */ HRESULT STDMETHODCALLTYPE Page( 
            /* [in] */ VARIANT vContact,
            /* [retval][out] */ IDispatch **ppMWindow) = 0;
        
        virtual /* [helpcontext][helpstring][id] */ HRESULT STDMETHODCALLTYPE AutoSignin( void) = 0;
        
        virtual /* [helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_MyContacts( 
            /* [retval][out] */ IDispatch **ppMContacts) = 0;
        
        virtual /* [helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_MySigninName( 
            /* [retval][out] */ BSTR *pbstrName) = 0;
        
        virtual /* [helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_MyFriendlyName( 
            /* [retval][out] */ BSTR *pbstrName) = 0;
        
        virtual /* [helpstring][propput][id] */ HRESULT STDMETHODCALLTYPE put_MyStatus( 
            /* [in] */ MISTATUS mStatus) = 0;
        
        virtual /* [helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_MyStatus( 
            /* [retval][out] */ MISTATUS *pmStatus) = 0;
        
        virtual /* [helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_UnreadEmailCount( 
            /* [in] */ MUAFOLDER mFolder,
            /* [retval][out] */ LONG *plCount) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_MyServiceName( 
            /* [retval][out] */ BSTR *pbstrServiceName) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_MyPhoneNumber( 
            /* [in] */ MPHONE_TYPE PhoneType,
            /* [retval][out] */ BSTR *pbstrNumber) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_MyProperty( 
            /* [in] */ MCONTACTPROPERTY ePropType,
            /* [retval][out] */ VARIANT *pvPropVal) = 0;
        
        virtual /* [helpcontext][helpstring][propput][id] */ HRESULT STDMETHODCALLTYPE put_MyProperty( 
            /* [in] */ MCONTACTPROPERTY ePropType,
            /* [in] */ VARIANT vPropVal) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_MyServiceId( 
            /* [retval][out] */ BSTR *pbstrServiceId) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_Services( 
            /* [retval][out] */ IDispatch **ppdispServices) = 0;
        
    };
    
#else 	/* C style interface */

    typedef struct IMessengerVtbl
    {
        BEGIN_INTERFACE
        
        HRESULT ( STDMETHODCALLTYPE *QueryInterface )( 
            IMessenger * This,
            /* [in] */ REFIID riid,
            /* [annotation][iid_is][out] */ 
            __RPC__deref_out  void **ppvObject);
        
        ULONG ( STDMETHODCALLTYPE *AddRef )( 
            IMessenger * This);
        
        ULONG ( STDMETHODCALLTYPE *Release )( 
            IMessenger * This);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfoCount )( 
            IMessenger * This,
            /* [out] */ UINT *pctinfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfo )( 
            IMessenger * This,
            /* [in] */ UINT iTInfo,
            /* [in] */ LCID lcid,
            /* [out] */ ITypeInfo **ppTInfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetIDsOfNames )( 
            IMessenger * This,
            /* [in] */ REFIID riid,
            /* [size_is][in] */ LPOLESTR *rgszNames,
            /* [range][in] */ UINT cNames,
            /* [in] */ LCID lcid,
            /* [size_is][out] */ DISPID *rgDispId);
        
        /* [local] */ HRESULT ( STDMETHODCALLTYPE *Invoke )( 
            IMessenger * This,
            /* [in] */ DISPID dispIdMember,
            /* [in] */ REFIID riid,
            /* [in] */ LCID lcid,
            /* [in] */ WORD wFlags,
            /* [out][in] */ DISPPARAMS *pDispParams,
            /* [out] */ VARIANT *pVarResult,
            /* [out] */ EXCEPINFO *pExcepInfo,
            /* [out] */ UINT *puArgErr);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Window )( 
            IMessenger * This,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *ViewProfile )( 
            IMessenger * This,
            /* [in] */ VARIANT vContact);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_ReceiveFileDirectory )( 
            IMessenger * This,
            /* [retval][out] */ BSTR *bstrPath);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *StartVoice )( 
            IMessenger * This,
            /* [in] */ VARIANT vContact,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *InviteApp )( 
            IMessenger * This,
            /* [in] */ VARIANT vContact,
            /* [in] */ BSTR bstrAppID,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *SendMail )( 
            IMessenger * This,
            /* [in] */ VARIANT vContact);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *OpenInbox )( 
            IMessenger * This);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *SendFile )( 
            IMessenger * This,
            /* [in] */ VARIANT vContact,
            /* [in] */ BSTR bstrFileName,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *Signout )( 
            IMessenger * This);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *Signin )( 
            IMessenger * This,
            /* [in] */ long hwndParent,
            /* [in] */ BSTR bstrSigninName,
            /* [in] */ BSTR bstrPassword);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *GetContact )( 
            IMessenger * This,
            /* [in] */ BSTR bstrSigninName,
            /* [in] */ BSTR bstrServiceId,
            /* [retval][out] */ IDispatch **ppMContact);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *OptionsPages )( 
            IMessenger * This,
            /* [in] */ long hwndParent,
            /* [in] */ MOPTIONPAGE mOptionPage);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *AddContact )( 
            IMessenger * This,
            /* [in] */ long hwndParent,
            /* [in] */ BSTR bstrEMail);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *FindContact )( 
            IMessenger * This,
            /* [in] */ long hwndParent,
            /* [in] */ BSTR bstrFirstName,
            /* [in] */ BSTR bstrLastName,
            /* [optional][in] */ VARIANT vbstrCity,
            /* [optional][in] */ VARIANT vbstrState,
            /* [optional][in] */ VARIANT vbstrCountry);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *InstantMessage )( 
            IMessenger * This,
            /* [in] */ VARIANT vContact,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *Phone )( 
            IMessenger * This,
            /* [in] */ VARIANT vContact,
            /* [in] */ MPHONE_TYPE ePhoneNumber,
            /* [in] */ BSTR bstrNumber,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *MediaWizard )( 
            IMessenger * This,
            /* [in] */ long hwndParent);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *Page )( 
            IMessenger * This,
            /* [in] */ VARIANT vContact,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *AutoSignin )( 
            IMessenger * This);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyContacts )( 
            IMessenger * This,
            /* [retval][out] */ IDispatch **ppMContacts);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MySigninName )( 
            IMessenger * This,
            /* [retval][out] */ BSTR *pbstrName);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyFriendlyName )( 
            IMessenger * This,
            /* [retval][out] */ BSTR *pbstrName);
        
        /* [helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_MyStatus )( 
            IMessenger * This,
            /* [in] */ MISTATUS mStatus);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyStatus )( 
            IMessenger * This,
            /* [retval][out] */ MISTATUS *pmStatus);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_UnreadEmailCount )( 
            IMessenger * This,
            /* [in] */ MUAFOLDER mFolder,
            /* [retval][out] */ LONG *plCount);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyServiceName )( 
            IMessenger * This,
            /* [retval][out] */ BSTR *pbstrServiceName);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyPhoneNumber )( 
            IMessenger * This,
            /* [in] */ MPHONE_TYPE PhoneType,
            /* [retval][out] */ BSTR *pbstrNumber);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyProperty )( 
            IMessenger * This,
            /* [in] */ MCONTACTPROPERTY ePropType,
            /* [retval][out] */ VARIANT *pvPropVal);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_MyProperty )( 
            IMessenger * This,
            /* [in] */ MCONTACTPROPERTY ePropType,
            /* [in] */ VARIANT vPropVal);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyServiceId )( 
            IMessenger * This,
            /* [retval][out] */ BSTR *pbstrServiceId);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Services )( 
            IMessenger * This,
            /* [retval][out] */ IDispatch **ppdispServices);
        
        END_INTERFACE
    } IMessengerVtbl;

    interface IMessenger
    {
        CONST_VTBL struct IMessengerVtbl *lpVtbl;
    };

    

#ifdef COBJMACROS


#define IMessenger_QueryInterface(This,riid,ppvObject)	\
    ( (This)->lpVtbl -> QueryInterface(This,riid,ppvObject) ) 

#define IMessenger_AddRef(This)	\
    ( (This)->lpVtbl -> AddRef(This) ) 

#define IMessenger_Release(This)	\
    ( (This)->lpVtbl -> Release(This) ) 


#define IMessenger_GetTypeInfoCount(This,pctinfo)	\
    ( (This)->lpVtbl -> GetTypeInfoCount(This,pctinfo) ) 

#define IMessenger_GetTypeInfo(This,iTInfo,lcid,ppTInfo)	\
    ( (This)->lpVtbl -> GetTypeInfo(This,iTInfo,lcid,ppTInfo) ) 

#define IMessenger_GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId)	\
    ( (This)->lpVtbl -> GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId) ) 

#define IMessenger_Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr)	\
    ( (This)->lpVtbl -> Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr) ) 


#define IMessenger_get_Window(This,ppMWindow)	\
    ( (This)->lpVtbl -> get_Window(This,ppMWindow) ) 

#define IMessenger_ViewProfile(This,vContact)	\
    ( (This)->lpVtbl -> ViewProfile(This,vContact) ) 

#define IMessenger_get_ReceiveFileDirectory(This,bstrPath)	\
    ( (This)->lpVtbl -> get_ReceiveFileDirectory(This,bstrPath) ) 

#define IMessenger_StartVoice(This,vContact,ppMWindow)	\
    ( (This)->lpVtbl -> StartVoice(This,vContact,ppMWindow) ) 

#define IMessenger_InviteApp(This,vContact,bstrAppID,ppMWindow)	\
    ( (This)->lpVtbl -> InviteApp(This,vContact,bstrAppID,ppMWindow) ) 

#define IMessenger_SendMail(This,vContact)	\
    ( (This)->lpVtbl -> SendMail(This,vContact) ) 

#define IMessenger_OpenInbox(This)	\
    ( (This)->lpVtbl -> OpenInbox(This) ) 

#define IMessenger_SendFile(This,vContact,bstrFileName,ppMWindow)	\
    ( (This)->lpVtbl -> SendFile(This,vContact,bstrFileName,ppMWindow) ) 

#define IMessenger_Signout(This)	\
    ( (This)->lpVtbl -> Signout(This) ) 

#define IMessenger_Signin(This,hwndParent,bstrSigninName,bstrPassword)	\
    ( (This)->lpVtbl -> Signin(This,hwndParent,bstrSigninName,bstrPassword) ) 

#define IMessenger_GetContact(This,bstrSigninName,bstrServiceId,ppMContact)	\
    ( (This)->lpVtbl -> GetContact(This,bstrSigninName,bstrServiceId,ppMContact) ) 

#define IMessenger_OptionsPages(This,hwndParent,mOptionPage)	\
    ( (This)->lpVtbl -> OptionsPages(This,hwndParent,mOptionPage) ) 

#define IMessenger_AddContact(This,hwndParent,bstrEMail)	\
    ( (This)->lpVtbl -> AddContact(This,hwndParent,bstrEMail) ) 

#define IMessenger_FindContact(This,hwndParent,bstrFirstName,bstrLastName,vbstrCity,vbstrState,vbstrCountry)	\
    ( (This)->lpVtbl -> FindContact(This,hwndParent,bstrFirstName,bstrLastName,vbstrCity,vbstrState,vbstrCountry) ) 

#define IMessenger_InstantMessage(This,vContact,ppMWindow)	\
    ( (This)->lpVtbl -> InstantMessage(This,vContact,ppMWindow) ) 

#define IMessenger_Phone(This,vContact,ePhoneNumber,bstrNumber,ppMWindow)	\
    ( (This)->lpVtbl -> Phone(This,vContact,ePhoneNumber,bstrNumber,ppMWindow) ) 

#define IMessenger_MediaWizard(This,hwndParent)	\
    ( (This)->lpVtbl -> MediaWizard(This,hwndParent) ) 

#define IMessenger_Page(This,vContact,ppMWindow)	\
    ( (This)->lpVtbl -> Page(This,vContact,ppMWindow) ) 

#define IMessenger_AutoSignin(This)	\
    ( (This)->lpVtbl -> AutoSignin(This) ) 

#define IMessenger_get_MyContacts(This,ppMContacts)	\
    ( (This)->lpVtbl -> get_MyContacts(This,ppMContacts) ) 

#define IMessenger_get_MySigninName(This,pbstrName)	\
    ( (This)->lpVtbl -> get_MySigninName(This,pbstrName) ) 

#define IMessenger_get_MyFriendlyName(This,pbstrName)	\
    ( (This)->lpVtbl -> get_MyFriendlyName(This,pbstrName) ) 

#define IMessenger_put_MyStatus(This,mStatus)	\
    ( (This)->lpVtbl -> put_MyStatus(This,mStatus) ) 

#define IMessenger_get_MyStatus(This,pmStatus)	\
    ( (This)->lpVtbl -> get_MyStatus(This,pmStatus) ) 

#define IMessenger_get_UnreadEmailCount(This,mFolder,plCount)	\
    ( (This)->lpVtbl -> get_UnreadEmailCount(This,mFolder,plCount) ) 

#define IMessenger_get_MyServiceName(This,pbstrServiceName)	\
    ( (This)->lpVtbl -> get_MyServiceName(This,pbstrServiceName) ) 

#define IMessenger_get_MyPhoneNumber(This,PhoneType,pbstrNumber)	\
    ( (This)->lpVtbl -> get_MyPhoneNumber(This,PhoneType,pbstrNumber) ) 

#define IMessenger_get_MyProperty(This,ePropType,pvPropVal)	\
    ( (This)->lpVtbl -> get_MyProperty(This,ePropType,pvPropVal) ) 

#define IMessenger_put_MyProperty(This,ePropType,vPropVal)	\
    ( (This)->lpVtbl -> put_MyProperty(This,ePropType,vPropVal) ) 

#define IMessenger_get_MyServiceId(This,pbstrServiceId)	\
    ( (This)->lpVtbl -> get_MyServiceId(This,pbstrServiceId) ) 

#define IMessenger_get_Services(This,ppdispServices)	\
    ( (This)->lpVtbl -> get_Services(This,ppdispServices) ) 

#endif /* COBJMACROS */


#endif 	/* C style interface */




#endif 	/* __IMessenger_INTERFACE_DEFINED__ */


#ifndef __IMessenger2_INTERFACE_DEFINED__
#define __IMessenger2_INTERFACE_DEFINED__

/* interface IMessenger2 */
/* [object][oleautomation][dual][helpcontext][helpstring][uuid] */ 


EXTERN_C const IID IID_IMessenger2;

#if defined(__cplusplus) && !defined(CINTERFACE)
    
    MIDL_INTERFACE("D50C3286-0F89-48f8-B204-3604629DEE10")
    IMessenger2 : public IMessenger
    {
    public:
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_ContactsSortOrder( 
            /* [retval][out] */ MUASORT *pSort) = 0;
        
        virtual /* [helpcontext][helpstring][propput][id] */ HRESULT STDMETHODCALLTYPE put_ContactsSortOrder( 
            /* [in] */ MUASORT Sort) = 0;
        
        virtual /* [helpcontext][helpstring][id] */ HRESULT STDMETHODCALLTYPE StartVideo( 
            /* [in] */ VARIANT vContact,
            /* [retval][out] */ IDispatch **ppMWindow) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_MyGroups( 
            /* [retval][out] */ IDispatch **ppMGroups) = 0;
        
        virtual /* [helpcontext][helpstring][id] */ HRESULT STDMETHODCALLTYPE CreateGroup( 
            /* [in] */ BSTR bstrName,
            /* [in] */ VARIANT vService,
            /* [retval][out] */ IDispatch **ppGroup) = 0;
        
    };
    
#else 	/* C style interface */

    typedef struct IMessenger2Vtbl
    {
        BEGIN_INTERFACE
        
        HRESULT ( STDMETHODCALLTYPE *QueryInterface )( 
            IMessenger2 * This,
            /* [in] */ REFIID riid,
            /* [annotation][iid_is][out] */ 
            __RPC__deref_out  void **ppvObject);
        
        ULONG ( STDMETHODCALLTYPE *AddRef )( 
            IMessenger2 * This);
        
        ULONG ( STDMETHODCALLTYPE *Release )( 
            IMessenger2 * This);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfoCount )( 
            IMessenger2 * This,
            /* [out] */ UINT *pctinfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfo )( 
            IMessenger2 * This,
            /* [in] */ UINT iTInfo,
            /* [in] */ LCID lcid,
            /* [out] */ ITypeInfo **ppTInfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetIDsOfNames )( 
            IMessenger2 * This,
            /* [in] */ REFIID riid,
            /* [size_is][in] */ LPOLESTR *rgszNames,
            /* [range][in] */ UINT cNames,
            /* [in] */ LCID lcid,
            /* [size_is][out] */ DISPID *rgDispId);
        
        /* [local] */ HRESULT ( STDMETHODCALLTYPE *Invoke )( 
            IMessenger2 * This,
            /* [in] */ DISPID dispIdMember,
            /* [in] */ REFIID riid,
            /* [in] */ LCID lcid,
            /* [in] */ WORD wFlags,
            /* [out][in] */ DISPPARAMS *pDispParams,
            /* [out] */ VARIANT *pVarResult,
            /* [out] */ EXCEPINFO *pExcepInfo,
            /* [out] */ UINT *puArgErr);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Window )( 
            IMessenger2 * This,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *ViewProfile )( 
            IMessenger2 * This,
            /* [in] */ VARIANT vContact);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_ReceiveFileDirectory )( 
            IMessenger2 * This,
            /* [retval][out] */ BSTR *bstrPath);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *StartVoice )( 
            IMessenger2 * This,
            /* [in] */ VARIANT vContact,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *InviteApp )( 
            IMessenger2 * This,
            /* [in] */ VARIANT vContact,
            /* [in] */ BSTR bstrAppID,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *SendMail )( 
            IMessenger2 * This,
            /* [in] */ VARIANT vContact);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *OpenInbox )( 
            IMessenger2 * This);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *SendFile )( 
            IMessenger2 * This,
            /* [in] */ VARIANT vContact,
            /* [in] */ BSTR bstrFileName,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *Signout )( 
            IMessenger2 * This);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *Signin )( 
            IMessenger2 * This,
            /* [in] */ long hwndParent,
            /* [in] */ BSTR bstrSigninName,
            /* [in] */ BSTR bstrPassword);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *GetContact )( 
            IMessenger2 * This,
            /* [in] */ BSTR bstrSigninName,
            /* [in] */ BSTR bstrServiceId,
            /* [retval][out] */ IDispatch **ppMContact);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *OptionsPages )( 
            IMessenger2 * This,
            /* [in] */ long hwndParent,
            /* [in] */ MOPTIONPAGE mOptionPage);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *AddContact )( 
            IMessenger2 * This,
            /* [in] */ long hwndParent,
            /* [in] */ BSTR bstrEMail);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *FindContact )( 
            IMessenger2 * This,
            /* [in] */ long hwndParent,
            /* [in] */ BSTR bstrFirstName,
            /* [in] */ BSTR bstrLastName,
            /* [optional][in] */ VARIANT vbstrCity,
            /* [optional][in] */ VARIANT vbstrState,
            /* [optional][in] */ VARIANT vbstrCountry);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *InstantMessage )( 
            IMessenger2 * This,
            /* [in] */ VARIANT vContact,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *Phone )( 
            IMessenger2 * This,
            /* [in] */ VARIANT vContact,
            /* [in] */ MPHONE_TYPE ePhoneNumber,
            /* [in] */ BSTR bstrNumber,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *MediaWizard )( 
            IMessenger2 * This,
            /* [in] */ long hwndParent);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *Page )( 
            IMessenger2 * This,
            /* [in] */ VARIANT vContact,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *AutoSignin )( 
            IMessenger2 * This);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyContacts )( 
            IMessenger2 * This,
            /* [retval][out] */ IDispatch **ppMContacts);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MySigninName )( 
            IMessenger2 * This,
            /* [retval][out] */ BSTR *pbstrName);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyFriendlyName )( 
            IMessenger2 * This,
            /* [retval][out] */ BSTR *pbstrName);
        
        /* [helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_MyStatus )( 
            IMessenger2 * This,
            /* [in] */ MISTATUS mStatus);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyStatus )( 
            IMessenger2 * This,
            /* [retval][out] */ MISTATUS *pmStatus);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_UnreadEmailCount )( 
            IMessenger2 * This,
            /* [in] */ MUAFOLDER mFolder,
            /* [retval][out] */ LONG *plCount);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyServiceName )( 
            IMessenger2 * This,
            /* [retval][out] */ BSTR *pbstrServiceName);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyPhoneNumber )( 
            IMessenger2 * This,
            /* [in] */ MPHONE_TYPE PhoneType,
            /* [retval][out] */ BSTR *pbstrNumber);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyProperty )( 
            IMessenger2 * This,
            /* [in] */ MCONTACTPROPERTY ePropType,
            /* [retval][out] */ VARIANT *pvPropVal);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_MyProperty )( 
            IMessenger2 * This,
            /* [in] */ MCONTACTPROPERTY ePropType,
            /* [in] */ VARIANT vPropVal);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyServiceId )( 
            IMessenger2 * This,
            /* [retval][out] */ BSTR *pbstrServiceId);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Services )( 
            IMessenger2 * This,
            /* [retval][out] */ IDispatch **ppdispServices);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_ContactsSortOrder )( 
            IMessenger2 * This,
            /* [retval][out] */ MUASORT *pSort);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_ContactsSortOrder )( 
            IMessenger2 * This,
            /* [in] */ MUASORT Sort);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *StartVideo )( 
            IMessenger2 * This,
            /* [in] */ VARIANT vContact,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyGroups )( 
            IMessenger2 * This,
            /* [retval][out] */ IDispatch **ppMGroups);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *CreateGroup )( 
            IMessenger2 * This,
            /* [in] */ BSTR bstrName,
            /* [in] */ VARIANT vService,
            /* [retval][out] */ IDispatch **ppGroup);
        
        END_INTERFACE
    } IMessenger2Vtbl;

    interface IMessenger2
    {
        CONST_VTBL struct IMessenger2Vtbl *lpVtbl;
    };

    

#ifdef COBJMACROS


#define IMessenger2_QueryInterface(This,riid,ppvObject)	\
    ( (This)->lpVtbl -> QueryInterface(This,riid,ppvObject) ) 

#define IMessenger2_AddRef(This)	\
    ( (This)->lpVtbl -> AddRef(This) ) 

#define IMessenger2_Release(This)	\
    ( (This)->lpVtbl -> Release(This) ) 


#define IMessenger2_GetTypeInfoCount(This,pctinfo)	\
    ( (This)->lpVtbl -> GetTypeInfoCount(This,pctinfo) ) 

#define IMessenger2_GetTypeInfo(This,iTInfo,lcid,ppTInfo)	\
    ( (This)->lpVtbl -> GetTypeInfo(This,iTInfo,lcid,ppTInfo) ) 

#define IMessenger2_GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId)	\
    ( (This)->lpVtbl -> GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId) ) 

#define IMessenger2_Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr)	\
    ( (This)->lpVtbl -> Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr) ) 


#define IMessenger2_get_Window(This,ppMWindow)	\
    ( (This)->lpVtbl -> get_Window(This,ppMWindow) ) 

#define IMessenger2_ViewProfile(This,vContact)	\
    ( (This)->lpVtbl -> ViewProfile(This,vContact) ) 

#define IMessenger2_get_ReceiveFileDirectory(This,bstrPath)	\
    ( (This)->lpVtbl -> get_ReceiveFileDirectory(This,bstrPath) ) 

#define IMessenger2_StartVoice(This,vContact,ppMWindow)	\
    ( (This)->lpVtbl -> StartVoice(This,vContact,ppMWindow) ) 

#define IMessenger2_InviteApp(This,vContact,bstrAppID,ppMWindow)	\
    ( (This)->lpVtbl -> InviteApp(This,vContact,bstrAppID,ppMWindow) ) 

#define IMessenger2_SendMail(This,vContact)	\
    ( (This)->lpVtbl -> SendMail(This,vContact) ) 

#define IMessenger2_OpenInbox(This)	\
    ( (This)->lpVtbl -> OpenInbox(This) ) 

#define IMessenger2_SendFile(This,vContact,bstrFileName,ppMWindow)	\
    ( (This)->lpVtbl -> SendFile(This,vContact,bstrFileName,ppMWindow) ) 

#define IMessenger2_Signout(This)	\
    ( (This)->lpVtbl -> Signout(This) ) 

#define IMessenger2_Signin(This,hwndParent,bstrSigninName,bstrPassword)	\
    ( (This)->lpVtbl -> Signin(This,hwndParent,bstrSigninName,bstrPassword) ) 

#define IMessenger2_GetContact(This,bstrSigninName,bstrServiceId,ppMContact)	\
    ( (This)->lpVtbl -> GetContact(This,bstrSigninName,bstrServiceId,ppMContact) ) 

#define IMessenger2_OptionsPages(This,hwndParent,mOptionPage)	\
    ( (This)->lpVtbl -> OptionsPages(This,hwndParent,mOptionPage) ) 

#define IMessenger2_AddContact(This,hwndParent,bstrEMail)	\
    ( (This)->lpVtbl -> AddContact(This,hwndParent,bstrEMail) ) 

#define IMessenger2_FindContact(This,hwndParent,bstrFirstName,bstrLastName,vbstrCity,vbstrState,vbstrCountry)	\
    ( (This)->lpVtbl -> FindContact(This,hwndParent,bstrFirstName,bstrLastName,vbstrCity,vbstrState,vbstrCountry) ) 

#define IMessenger2_InstantMessage(This,vContact,ppMWindow)	\
    ( (This)->lpVtbl -> InstantMessage(This,vContact,ppMWindow) ) 

#define IMessenger2_Phone(This,vContact,ePhoneNumber,bstrNumber,ppMWindow)	\
    ( (This)->lpVtbl -> Phone(This,vContact,ePhoneNumber,bstrNumber,ppMWindow) ) 

#define IMessenger2_MediaWizard(This,hwndParent)	\
    ( (This)->lpVtbl -> MediaWizard(This,hwndParent) ) 

#define IMessenger2_Page(This,vContact,ppMWindow)	\
    ( (This)->lpVtbl -> Page(This,vContact,ppMWindow) ) 

#define IMessenger2_AutoSignin(This)	\
    ( (This)->lpVtbl -> AutoSignin(This) ) 

#define IMessenger2_get_MyContacts(This,ppMContacts)	\
    ( (This)->lpVtbl -> get_MyContacts(This,ppMContacts) ) 

#define IMessenger2_get_MySigninName(This,pbstrName)	\
    ( (This)->lpVtbl -> get_MySigninName(This,pbstrName) ) 

#define IMessenger2_get_MyFriendlyName(This,pbstrName)	\
    ( (This)->lpVtbl -> get_MyFriendlyName(This,pbstrName) ) 

#define IMessenger2_put_MyStatus(This,mStatus)	\
    ( (This)->lpVtbl -> put_MyStatus(This,mStatus) ) 

#define IMessenger2_get_MyStatus(This,pmStatus)	\
    ( (This)->lpVtbl -> get_MyStatus(This,pmStatus) ) 

#define IMessenger2_get_UnreadEmailCount(This,mFolder,plCount)	\
    ( (This)->lpVtbl -> get_UnreadEmailCount(This,mFolder,plCount) ) 

#define IMessenger2_get_MyServiceName(This,pbstrServiceName)	\
    ( (This)->lpVtbl -> get_MyServiceName(This,pbstrServiceName) ) 

#define IMessenger2_get_MyPhoneNumber(This,PhoneType,pbstrNumber)	\
    ( (This)->lpVtbl -> get_MyPhoneNumber(This,PhoneType,pbstrNumber) ) 

#define IMessenger2_get_MyProperty(This,ePropType,pvPropVal)	\
    ( (This)->lpVtbl -> get_MyProperty(This,ePropType,pvPropVal) ) 

#define IMessenger2_put_MyProperty(This,ePropType,vPropVal)	\
    ( (This)->lpVtbl -> put_MyProperty(This,ePropType,vPropVal) ) 

#define IMessenger2_get_MyServiceId(This,pbstrServiceId)	\
    ( (This)->lpVtbl -> get_MyServiceId(This,pbstrServiceId) ) 

#define IMessenger2_get_Services(This,ppdispServices)	\
    ( (This)->lpVtbl -> get_Services(This,ppdispServices) ) 


#define IMessenger2_get_ContactsSortOrder(This,pSort)	\
    ( (This)->lpVtbl -> get_ContactsSortOrder(This,pSort) ) 

#define IMessenger2_put_ContactsSortOrder(This,Sort)	\
    ( (This)->lpVtbl -> put_ContactsSortOrder(This,Sort) ) 

#define IMessenger2_StartVideo(This,vContact,ppMWindow)	\
    ( (This)->lpVtbl -> StartVideo(This,vContact,ppMWindow) ) 

#define IMessenger2_get_MyGroups(This,ppMGroups)	\
    ( (This)->lpVtbl -> get_MyGroups(This,ppMGroups) ) 

#define IMessenger2_CreateGroup(This,bstrName,vService,ppGroup)	\
    ( (This)->lpVtbl -> CreateGroup(This,bstrName,vService,ppGroup) ) 

#endif /* COBJMACROS */


#endif 	/* C style interface */




#endif 	/* __IMessenger2_INTERFACE_DEFINED__ */


#ifndef __IMessenger3_INTERFACE_DEFINED__
#define __IMessenger3_INTERFACE_DEFINED__

/* interface IMessenger3 */
/* [object][oleautomation][dual][helpcontext][helpstring][uuid] */ 


EXTERN_C const IID IID_IMessenger3;

#if defined(__cplusplus) && !defined(CINTERFACE)
    
    MIDL_INTERFACE("D50C3386-0F89-48f8-B204-3604629DEE10")
    IMessenger3 : public IMessenger2
    {
    public:
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_Property( 
            /* [in] */ MMESSENGERPROPERTY ePropType,
            /* [retval][out] */ VARIANT *pvPropVal) = 0;
        
        virtual /* [helpcontext][helpstring][propput][id] */ HRESULT STDMETHODCALLTYPE put_Property( 
            /* [in] */ MMESSENGERPROPERTY ePropType,
            /* [in] */ VARIANT vPropVal) = 0;
        
    };
    
#else 	/* C style interface */

    typedef struct IMessenger3Vtbl
    {
        BEGIN_INTERFACE
        
        HRESULT ( STDMETHODCALLTYPE *QueryInterface )( 
            IMessenger3 * This,
            /* [in] */ REFIID riid,
            /* [annotation][iid_is][out] */ 
            __RPC__deref_out  void **ppvObject);
        
        ULONG ( STDMETHODCALLTYPE *AddRef )( 
            IMessenger3 * This);
        
        ULONG ( STDMETHODCALLTYPE *Release )( 
            IMessenger3 * This);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfoCount )( 
            IMessenger3 * This,
            /* [out] */ UINT *pctinfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfo )( 
            IMessenger3 * This,
            /* [in] */ UINT iTInfo,
            /* [in] */ LCID lcid,
            /* [out] */ ITypeInfo **ppTInfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetIDsOfNames )( 
            IMessenger3 * This,
            /* [in] */ REFIID riid,
            /* [size_is][in] */ LPOLESTR *rgszNames,
            /* [range][in] */ UINT cNames,
            /* [in] */ LCID lcid,
            /* [size_is][out] */ DISPID *rgDispId);
        
        /* [local] */ HRESULT ( STDMETHODCALLTYPE *Invoke )( 
            IMessenger3 * This,
            /* [in] */ DISPID dispIdMember,
            /* [in] */ REFIID riid,
            /* [in] */ LCID lcid,
            /* [in] */ WORD wFlags,
            /* [out][in] */ DISPPARAMS *pDispParams,
            /* [out] */ VARIANT *pVarResult,
            /* [out] */ EXCEPINFO *pExcepInfo,
            /* [out] */ UINT *puArgErr);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Window )( 
            IMessenger3 * This,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *ViewProfile )( 
            IMessenger3 * This,
            /* [in] */ VARIANT vContact);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_ReceiveFileDirectory )( 
            IMessenger3 * This,
            /* [retval][out] */ BSTR *bstrPath);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *StartVoice )( 
            IMessenger3 * This,
            /* [in] */ VARIANT vContact,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *InviteApp )( 
            IMessenger3 * This,
            /* [in] */ VARIANT vContact,
            /* [in] */ BSTR bstrAppID,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *SendMail )( 
            IMessenger3 * This,
            /* [in] */ VARIANT vContact);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *OpenInbox )( 
            IMessenger3 * This);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *SendFile )( 
            IMessenger3 * This,
            /* [in] */ VARIANT vContact,
            /* [in] */ BSTR bstrFileName,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *Signout )( 
            IMessenger3 * This);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *Signin )( 
            IMessenger3 * This,
            /* [in] */ long hwndParent,
            /* [in] */ BSTR bstrSigninName,
            /* [in] */ BSTR bstrPassword);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *GetContact )( 
            IMessenger3 * This,
            /* [in] */ BSTR bstrSigninName,
            /* [in] */ BSTR bstrServiceId,
            /* [retval][out] */ IDispatch **ppMContact);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *OptionsPages )( 
            IMessenger3 * This,
            /* [in] */ long hwndParent,
            /* [in] */ MOPTIONPAGE mOptionPage);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *AddContact )( 
            IMessenger3 * This,
            /* [in] */ long hwndParent,
            /* [in] */ BSTR bstrEMail);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *FindContact )( 
            IMessenger3 * This,
            /* [in] */ long hwndParent,
            /* [in] */ BSTR bstrFirstName,
            /* [in] */ BSTR bstrLastName,
            /* [optional][in] */ VARIANT vbstrCity,
            /* [optional][in] */ VARIANT vbstrState,
            /* [optional][in] */ VARIANT vbstrCountry);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *InstantMessage )( 
            IMessenger3 * This,
            /* [in] */ VARIANT vContact,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *Phone )( 
            IMessenger3 * This,
            /* [in] */ VARIANT vContact,
            /* [in] */ MPHONE_TYPE ePhoneNumber,
            /* [in] */ BSTR bstrNumber,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *MediaWizard )( 
            IMessenger3 * This,
            /* [in] */ long hwndParent);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *Page )( 
            IMessenger3 * This,
            /* [in] */ VARIANT vContact,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *AutoSignin )( 
            IMessenger3 * This);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyContacts )( 
            IMessenger3 * This,
            /* [retval][out] */ IDispatch **ppMContacts);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MySigninName )( 
            IMessenger3 * This,
            /* [retval][out] */ BSTR *pbstrName);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyFriendlyName )( 
            IMessenger3 * This,
            /* [retval][out] */ BSTR *pbstrName);
        
        /* [helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_MyStatus )( 
            IMessenger3 * This,
            /* [in] */ MISTATUS mStatus);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyStatus )( 
            IMessenger3 * This,
            /* [retval][out] */ MISTATUS *pmStatus);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_UnreadEmailCount )( 
            IMessenger3 * This,
            /* [in] */ MUAFOLDER mFolder,
            /* [retval][out] */ LONG *plCount);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyServiceName )( 
            IMessenger3 * This,
            /* [retval][out] */ BSTR *pbstrServiceName);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyPhoneNumber )( 
            IMessenger3 * This,
            /* [in] */ MPHONE_TYPE PhoneType,
            /* [retval][out] */ BSTR *pbstrNumber);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyProperty )( 
            IMessenger3 * This,
            /* [in] */ MCONTACTPROPERTY ePropType,
            /* [retval][out] */ VARIANT *pvPropVal);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_MyProperty )( 
            IMessenger3 * This,
            /* [in] */ MCONTACTPROPERTY ePropType,
            /* [in] */ VARIANT vPropVal);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyServiceId )( 
            IMessenger3 * This,
            /* [retval][out] */ BSTR *pbstrServiceId);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Services )( 
            IMessenger3 * This,
            /* [retval][out] */ IDispatch **ppdispServices);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_ContactsSortOrder )( 
            IMessenger3 * This,
            /* [retval][out] */ MUASORT *pSort);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_ContactsSortOrder )( 
            IMessenger3 * This,
            /* [in] */ MUASORT Sort);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *StartVideo )( 
            IMessenger3 * This,
            /* [in] */ VARIANT vContact,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyGroups )( 
            IMessenger3 * This,
            /* [retval][out] */ IDispatch **ppMGroups);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *CreateGroup )( 
            IMessenger3 * This,
            /* [in] */ BSTR bstrName,
            /* [in] */ VARIANT vService,
            /* [retval][out] */ IDispatch **ppGroup);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Property )( 
            IMessenger3 * This,
            /* [in] */ MMESSENGERPROPERTY ePropType,
            /* [retval][out] */ VARIANT *pvPropVal);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_Property )( 
            IMessenger3 * This,
            /* [in] */ MMESSENGERPROPERTY ePropType,
            /* [in] */ VARIANT vPropVal);
        
        END_INTERFACE
    } IMessenger3Vtbl;

    interface IMessenger3
    {
        CONST_VTBL struct IMessenger3Vtbl *lpVtbl;
    };

    

#ifdef COBJMACROS


#define IMessenger3_QueryInterface(This,riid,ppvObject)	\
    ( (This)->lpVtbl -> QueryInterface(This,riid,ppvObject) ) 

#define IMessenger3_AddRef(This)	\
    ( (This)->lpVtbl -> AddRef(This) ) 

#define IMessenger3_Release(This)	\
    ( (This)->lpVtbl -> Release(This) ) 


#define IMessenger3_GetTypeInfoCount(This,pctinfo)	\
    ( (This)->lpVtbl -> GetTypeInfoCount(This,pctinfo) ) 

#define IMessenger3_GetTypeInfo(This,iTInfo,lcid,ppTInfo)	\
    ( (This)->lpVtbl -> GetTypeInfo(This,iTInfo,lcid,ppTInfo) ) 

#define IMessenger3_GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId)	\
    ( (This)->lpVtbl -> GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId) ) 

#define IMessenger3_Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr)	\
    ( (This)->lpVtbl -> Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr) ) 


#define IMessenger3_get_Window(This,ppMWindow)	\
    ( (This)->lpVtbl -> get_Window(This,ppMWindow) ) 

#define IMessenger3_ViewProfile(This,vContact)	\
    ( (This)->lpVtbl -> ViewProfile(This,vContact) ) 

#define IMessenger3_get_ReceiveFileDirectory(This,bstrPath)	\
    ( (This)->lpVtbl -> get_ReceiveFileDirectory(This,bstrPath) ) 

#define IMessenger3_StartVoice(This,vContact,ppMWindow)	\
    ( (This)->lpVtbl -> StartVoice(This,vContact,ppMWindow) ) 

#define IMessenger3_InviteApp(This,vContact,bstrAppID,ppMWindow)	\
    ( (This)->lpVtbl -> InviteApp(This,vContact,bstrAppID,ppMWindow) ) 

#define IMessenger3_SendMail(This,vContact)	\
    ( (This)->lpVtbl -> SendMail(This,vContact) ) 

#define IMessenger3_OpenInbox(This)	\
    ( (This)->lpVtbl -> OpenInbox(This) ) 

#define IMessenger3_SendFile(This,vContact,bstrFileName,ppMWindow)	\
    ( (This)->lpVtbl -> SendFile(This,vContact,bstrFileName,ppMWindow) ) 

#define IMessenger3_Signout(This)	\
    ( (This)->lpVtbl -> Signout(This) ) 

#define IMessenger3_Signin(This,hwndParent,bstrSigninName,bstrPassword)	\
    ( (This)->lpVtbl -> Signin(This,hwndParent,bstrSigninName,bstrPassword) ) 

#define IMessenger3_GetContact(This,bstrSigninName,bstrServiceId,ppMContact)	\
    ( (This)->lpVtbl -> GetContact(This,bstrSigninName,bstrServiceId,ppMContact) ) 

#define IMessenger3_OptionsPages(This,hwndParent,mOptionPage)	\
    ( (This)->lpVtbl -> OptionsPages(This,hwndParent,mOptionPage) ) 

#define IMessenger3_AddContact(This,hwndParent,bstrEMail)	\
    ( (This)->lpVtbl -> AddContact(This,hwndParent,bstrEMail) ) 

#define IMessenger3_FindContact(This,hwndParent,bstrFirstName,bstrLastName,vbstrCity,vbstrState,vbstrCountry)	\
    ( (This)->lpVtbl -> FindContact(This,hwndParent,bstrFirstName,bstrLastName,vbstrCity,vbstrState,vbstrCountry) ) 

#define IMessenger3_InstantMessage(This,vContact,ppMWindow)	\
    ( (This)->lpVtbl -> InstantMessage(This,vContact,ppMWindow) ) 

#define IMessenger3_Phone(This,vContact,ePhoneNumber,bstrNumber,ppMWindow)	\
    ( (This)->lpVtbl -> Phone(This,vContact,ePhoneNumber,bstrNumber,ppMWindow) ) 

#define IMessenger3_MediaWizard(This,hwndParent)	\
    ( (This)->lpVtbl -> MediaWizard(This,hwndParent) ) 

#define IMessenger3_Page(This,vContact,ppMWindow)	\
    ( (This)->lpVtbl -> Page(This,vContact,ppMWindow) ) 

#define IMessenger3_AutoSignin(This)	\
    ( (This)->lpVtbl -> AutoSignin(This) ) 

#define IMessenger3_get_MyContacts(This,ppMContacts)	\
    ( (This)->lpVtbl -> get_MyContacts(This,ppMContacts) ) 

#define IMessenger3_get_MySigninName(This,pbstrName)	\
    ( (This)->lpVtbl -> get_MySigninName(This,pbstrName) ) 

#define IMessenger3_get_MyFriendlyName(This,pbstrName)	\
    ( (This)->lpVtbl -> get_MyFriendlyName(This,pbstrName) ) 

#define IMessenger3_put_MyStatus(This,mStatus)	\
    ( (This)->lpVtbl -> put_MyStatus(This,mStatus) ) 

#define IMessenger3_get_MyStatus(This,pmStatus)	\
    ( (This)->lpVtbl -> get_MyStatus(This,pmStatus) ) 

#define IMessenger3_get_UnreadEmailCount(This,mFolder,plCount)	\
    ( (This)->lpVtbl -> get_UnreadEmailCount(This,mFolder,plCount) ) 

#define IMessenger3_get_MyServiceName(This,pbstrServiceName)	\
    ( (This)->lpVtbl -> get_MyServiceName(This,pbstrServiceName) ) 

#define IMessenger3_get_MyPhoneNumber(This,PhoneType,pbstrNumber)	\
    ( (This)->lpVtbl -> get_MyPhoneNumber(This,PhoneType,pbstrNumber) ) 

#define IMessenger3_get_MyProperty(This,ePropType,pvPropVal)	\
    ( (This)->lpVtbl -> get_MyProperty(This,ePropType,pvPropVal) ) 

#define IMessenger3_put_MyProperty(This,ePropType,vPropVal)	\
    ( (This)->lpVtbl -> put_MyProperty(This,ePropType,vPropVal) ) 

#define IMessenger3_get_MyServiceId(This,pbstrServiceId)	\
    ( (This)->lpVtbl -> get_MyServiceId(This,pbstrServiceId) ) 

#define IMessenger3_get_Services(This,ppdispServices)	\
    ( (This)->lpVtbl -> get_Services(This,ppdispServices) ) 


#define IMessenger3_get_ContactsSortOrder(This,pSort)	\
    ( (This)->lpVtbl -> get_ContactsSortOrder(This,pSort) ) 

#define IMessenger3_put_ContactsSortOrder(This,Sort)	\
    ( (This)->lpVtbl -> put_ContactsSortOrder(This,Sort) ) 

#define IMessenger3_StartVideo(This,vContact,ppMWindow)	\
    ( (This)->lpVtbl -> StartVideo(This,vContact,ppMWindow) ) 

#define IMessenger3_get_MyGroups(This,ppMGroups)	\
    ( (This)->lpVtbl -> get_MyGroups(This,ppMGroups) ) 

#define IMessenger3_CreateGroup(This,bstrName,vService,ppGroup)	\
    ( (This)->lpVtbl -> CreateGroup(This,bstrName,vService,ppGroup) ) 


#define IMessenger3_get_Property(This,ePropType,pvPropVal)	\
    ( (This)->lpVtbl -> get_Property(This,ePropType,pvPropVal) ) 

#define IMessenger3_put_Property(This,ePropType,vPropVal)	\
    ( (This)->lpVtbl -> put_Property(This,ePropType,vPropVal) ) 

#endif /* COBJMACROS */


#endif 	/* C style interface */




#endif 	/* __IMessenger3_INTERFACE_DEFINED__ */


#ifndef __IMessengerAdvanced_INTERFACE_DEFINED__
#define __IMessengerAdvanced_INTERFACE_DEFINED__

/* interface IMessengerAdvanced */
/* [object][oleautomation][dual][helpcontext][helpstring][uuid] */ 


EXTERN_C const IID IID_IMessengerAdvanced;

#if defined(__cplusplus) && !defined(CINTERFACE)
    
    MIDL_INTERFACE("DA0635E8-09AF-480c-88B2-AA9FA1D9DB27")
    IMessengerAdvanced : public IMessenger3
    {
    public:
        virtual /* [helpcontext][helpstring] */ HRESULT STDMETHODCALLTYPE StartConversation( 
            /* [in] */ CONVERSATION_TYPE ConversationType,
            /* [in] */ VARIANT vParticipants,
            /* [in][optional] */ VARIANT vContextualData,
            /* [in][optional] */ VARIANT vSubject,
            /* [in][optional] */ VARIANT vConversationIndex,
            /* [in][optional] */ VARIANT vConversationData,
            /* [retval][out][optional] */ VARIANT *pvWndHnd) = 0;
        
        virtual /* [helpcontext][helpstring] */ HRESULT STDMETHODCALLTYPE GetAuthenticationInfo( 
            /* [retval][out] */ BSTR *pbstrAuthInfo) = 0;
        
    };
    
#else 	/* C style interface */

    typedef struct IMessengerAdvancedVtbl
    {
        BEGIN_INTERFACE
        
        HRESULT ( STDMETHODCALLTYPE *QueryInterface )( 
            IMessengerAdvanced * This,
            /* [in] */ REFIID riid,
            /* [annotation][iid_is][out] */ 
            __RPC__deref_out  void **ppvObject);
        
        ULONG ( STDMETHODCALLTYPE *AddRef )( 
            IMessengerAdvanced * This);
        
        ULONG ( STDMETHODCALLTYPE *Release )( 
            IMessengerAdvanced * This);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfoCount )( 
            IMessengerAdvanced * This,
            /* [out] */ UINT *pctinfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfo )( 
            IMessengerAdvanced * This,
            /* [in] */ UINT iTInfo,
            /* [in] */ LCID lcid,
            /* [out] */ ITypeInfo **ppTInfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetIDsOfNames )( 
            IMessengerAdvanced * This,
            /* [in] */ REFIID riid,
            /* [size_is][in] */ LPOLESTR *rgszNames,
            /* [range][in] */ UINT cNames,
            /* [in] */ LCID lcid,
            /* [size_is][out] */ DISPID *rgDispId);
        
        /* [local] */ HRESULT ( STDMETHODCALLTYPE *Invoke )( 
            IMessengerAdvanced * This,
            /* [in] */ DISPID dispIdMember,
            /* [in] */ REFIID riid,
            /* [in] */ LCID lcid,
            /* [in] */ WORD wFlags,
            /* [out][in] */ DISPPARAMS *pDispParams,
            /* [out] */ VARIANT *pVarResult,
            /* [out] */ EXCEPINFO *pExcepInfo,
            /* [out] */ UINT *puArgErr);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Window )( 
            IMessengerAdvanced * This,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *ViewProfile )( 
            IMessengerAdvanced * This,
            /* [in] */ VARIANT vContact);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_ReceiveFileDirectory )( 
            IMessengerAdvanced * This,
            /* [retval][out] */ BSTR *bstrPath);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *StartVoice )( 
            IMessengerAdvanced * This,
            /* [in] */ VARIANT vContact,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *InviteApp )( 
            IMessengerAdvanced * This,
            /* [in] */ VARIANT vContact,
            /* [in] */ BSTR bstrAppID,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *SendMail )( 
            IMessengerAdvanced * This,
            /* [in] */ VARIANT vContact);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *OpenInbox )( 
            IMessengerAdvanced * This);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *SendFile )( 
            IMessengerAdvanced * This,
            /* [in] */ VARIANT vContact,
            /* [in] */ BSTR bstrFileName,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *Signout )( 
            IMessengerAdvanced * This);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *Signin )( 
            IMessengerAdvanced * This,
            /* [in] */ long hwndParent,
            /* [in] */ BSTR bstrSigninName,
            /* [in] */ BSTR bstrPassword);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *GetContact )( 
            IMessengerAdvanced * This,
            /* [in] */ BSTR bstrSigninName,
            /* [in] */ BSTR bstrServiceId,
            /* [retval][out] */ IDispatch **ppMContact);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *OptionsPages )( 
            IMessengerAdvanced * This,
            /* [in] */ long hwndParent,
            /* [in] */ MOPTIONPAGE mOptionPage);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *AddContact )( 
            IMessengerAdvanced * This,
            /* [in] */ long hwndParent,
            /* [in] */ BSTR bstrEMail);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *FindContact )( 
            IMessengerAdvanced * This,
            /* [in] */ long hwndParent,
            /* [in] */ BSTR bstrFirstName,
            /* [in] */ BSTR bstrLastName,
            /* [optional][in] */ VARIANT vbstrCity,
            /* [optional][in] */ VARIANT vbstrState,
            /* [optional][in] */ VARIANT vbstrCountry);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *InstantMessage )( 
            IMessengerAdvanced * This,
            /* [in] */ VARIANT vContact,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *Phone )( 
            IMessengerAdvanced * This,
            /* [in] */ VARIANT vContact,
            /* [in] */ MPHONE_TYPE ePhoneNumber,
            /* [in] */ BSTR bstrNumber,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *MediaWizard )( 
            IMessengerAdvanced * This,
            /* [in] */ long hwndParent);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *Page )( 
            IMessengerAdvanced * This,
            /* [in] */ VARIANT vContact,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *AutoSignin )( 
            IMessengerAdvanced * This);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyContacts )( 
            IMessengerAdvanced * This,
            /* [retval][out] */ IDispatch **ppMContacts);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MySigninName )( 
            IMessengerAdvanced * This,
            /* [retval][out] */ BSTR *pbstrName);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyFriendlyName )( 
            IMessengerAdvanced * This,
            /* [retval][out] */ BSTR *pbstrName);
        
        /* [helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_MyStatus )( 
            IMessengerAdvanced * This,
            /* [in] */ MISTATUS mStatus);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyStatus )( 
            IMessengerAdvanced * This,
            /* [retval][out] */ MISTATUS *pmStatus);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_UnreadEmailCount )( 
            IMessengerAdvanced * This,
            /* [in] */ MUAFOLDER mFolder,
            /* [retval][out] */ LONG *plCount);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyServiceName )( 
            IMessengerAdvanced * This,
            /* [retval][out] */ BSTR *pbstrServiceName);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyPhoneNumber )( 
            IMessengerAdvanced * This,
            /* [in] */ MPHONE_TYPE PhoneType,
            /* [retval][out] */ BSTR *pbstrNumber);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyProperty )( 
            IMessengerAdvanced * This,
            /* [in] */ MCONTACTPROPERTY ePropType,
            /* [retval][out] */ VARIANT *pvPropVal);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_MyProperty )( 
            IMessengerAdvanced * This,
            /* [in] */ MCONTACTPROPERTY ePropType,
            /* [in] */ VARIANT vPropVal);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyServiceId )( 
            IMessengerAdvanced * This,
            /* [retval][out] */ BSTR *pbstrServiceId);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Services )( 
            IMessengerAdvanced * This,
            /* [retval][out] */ IDispatch **ppdispServices);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_ContactsSortOrder )( 
            IMessengerAdvanced * This,
            /* [retval][out] */ MUASORT *pSort);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_ContactsSortOrder )( 
            IMessengerAdvanced * This,
            /* [in] */ MUASORT Sort);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *StartVideo )( 
            IMessengerAdvanced * This,
            /* [in] */ VARIANT vContact,
            /* [retval][out] */ IDispatch **ppMWindow);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyGroups )( 
            IMessengerAdvanced * This,
            /* [retval][out] */ IDispatch **ppMGroups);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *CreateGroup )( 
            IMessengerAdvanced * This,
            /* [in] */ BSTR bstrName,
            /* [in] */ VARIANT vService,
            /* [retval][out] */ IDispatch **ppGroup);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Property )( 
            IMessengerAdvanced * This,
            /* [in] */ MMESSENGERPROPERTY ePropType,
            /* [retval][out] */ VARIANT *pvPropVal);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_Property )( 
            IMessengerAdvanced * This,
            /* [in] */ MMESSENGERPROPERTY ePropType,
            /* [in] */ VARIANT vPropVal);
        
        /* [helpcontext][helpstring] */ HRESULT ( STDMETHODCALLTYPE *StartConversation )( 
            IMessengerAdvanced * This,
            /* [in] */ CONVERSATION_TYPE ConversationType,
            /* [in] */ VARIANT vParticipants,
            /* [in][optional] */ VARIANT vContextualData,
            /* [in][optional] */ VARIANT vSubject,
            /* [in][optional] */ VARIANT vConversationIndex,
            /* [in][optional] */ VARIANT vConversationData,
            /* [retval][out][optional] */ VARIANT *pvWndHnd);
        
        /* [helpcontext][helpstring] */ HRESULT ( STDMETHODCALLTYPE *GetAuthenticationInfo )( 
            IMessengerAdvanced * This,
            /* [retval][out] */ BSTR *pbstrAuthInfo);
        
        END_INTERFACE
    } IMessengerAdvancedVtbl;

    interface IMessengerAdvanced
    {
        CONST_VTBL struct IMessengerAdvancedVtbl *lpVtbl;
    };

    

#ifdef COBJMACROS


#define IMessengerAdvanced_QueryInterface(This,riid,ppvObject)	\
    ( (This)->lpVtbl -> QueryInterface(This,riid,ppvObject) ) 

#define IMessengerAdvanced_AddRef(This)	\
    ( (This)->lpVtbl -> AddRef(This) ) 

#define IMessengerAdvanced_Release(This)	\
    ( (This)->lpVtbl -> Release(This) ) 


#define IMessengerAdvanced_GetTypeInfoCount(This,pctinfo)	\
    ( (This)->lpVtbl -> GetTypeInfoCount(This,pctinfo) ) 

#define IMessengerAdvanced_GetTypeInfo(This,iTInfo,lcid,ppTInfo)	\
    ( (This)->lpVtbl -> GetTypeInfo(This,iTInfo,lcid,ppTInfo) ) 

#define IMessengerAdvanced_GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId)	\
    ( (This)->lpVtbl -> GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId) ) 

#define IMessengerAdvanced_Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr)	\
    ( (This)->lpVtbl -> Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr) ) 


#define IMessengerAdvanced_get_Window(This,ppMWindow)	\
    ( (This)->lpVtbl -> get_Window(This,ppMWindow) ) 

#define IMessengerAdvanced_ViewProfile(This,vContact)	\
    ( (This)->lpVtbl -> ViewProfile(This,vContact) ) 

#define IMessengerAdvanced_get_ReceiveFileDirectory(This,bstrPath)	\
    ( (This)->lpVtbl -> get_ReceiveFileDirectory(This,bstrPath) ) 

#define IMessengerAdvanced_StartVoice(This,vContact,ppMWindow)	\
    ( (This)->lpVtbl -> StartVoice(This,vContact,ppMWindow) ) 

#define IMessengerAdvanced_InviteApp(This,vContact,bstrAppID,ppMWindow)	\
    ( (This)->lpVtbl -> InviteApp(This,vContact,bstrAppID,ppMWindow) ) 

#define IMessengerAdvanced_SendMail(This,vContact)	\
    ( (This)->lpVtbl -> SendMail(This,vContact) ) 

#define IMessengerAdvanced_OpenInbox(This)	\
    ( (This)->lpVtbl -> OpenInbox(This) ) 

#define IMessengerAdvanced_SendFile(This,vContact,bstrFileName,ppMWindow)	\
    ( (This)->lpVtbl -> SendFile(This,vContact,bstrFileName,ppMWindow) ) 

#define IMessengerAdvanced_Signout(This)	\
    ( (This)->lpVtbl -> Signout(This) ) 

#define IMessengerAdvanced_Signin(This,hwndParent,bstrSigninName,bstrPassword)	\
    ( (This)->lpVtbl -> Signin(This,hwndParent,bstrSigninName,bstrPassword) ) 

#define IMessengerAdvanced_GetContact(This,bstrSigninName,bstrServiceId,ppMContact)	\
    ( (This)->lpVtbl -> GetContact(This,bstrSigninName,bstrServiceId,ppMContact) ) 

#define IMessengerAdvanced_OptionsPages(This,hwndParent,mOptionPage)	\
    ( (This)->lpVtbl -> OptionsPages(This,hwndParent,mOptionPage) ) 

#define IMessengerAdvanced_AddContact(This,hwndParent,bstrEMail)	\
    ( (This)->lpVtbl -> AddContact(This,hwndParent,bstrEMail) ) 

#define IMessengerAdvanced_FindContact(This,hwndParent,bstrFirstName,bstrLastName,vbstrCity,vbstrState,vbstrCountry)	\
    ( (This)->lpVtbl -> FindContact(This,hwndParent,bstrFirstName,bstrLastName,vbstrCity,vbstrState,vbstrCountry) ) 

#define IMessengerAdvanced_InstantMessage(This,vContact,ppMWindow)	\
    ( (This)->lpVtbl -> InstantMessage(This,vContact,ppMWindow) ) 

#define IMessengerAdvanced_Phone(This,vContact,ePhoneNumber,bstrNumber,ppMWindow)	\
    ( (This)->lpVtbl -> Phone(This,vContact,ePhoneNumber,bstrNumber,ppMWindow) ) 

#define IMessengerAdvanced_MediaWizard(This,hwndParent)	\
    ( (This)->lpVtbl -> MediaWizard(This,hwndParent) ) 

#define IMessengerAdvanced_Page(This,vContact,ppMWindow)	\
    ( (This)->lpVtbl -> Page(This,vContact,ppMWindow) ) 

#define IMessengerAdvanced_AutoSignin(This)	\
    ( (This)->lpVtbl -> AutoSignin(This) ) 

#define IMessengerAdvanced_get_MyContacts(This,ppMContacts)	\
    ( (This)->lpVtbl -> get_MyContacts(This,ppMContacts) ) 

#define IMessengerAdvanced_get_MySigninName(This,pbstrName)	\
    ( (This)->lpVtbl -> get_MySigninName(This,pbstrName) ) 

#define IMessengerAdvanced_get_MyFriendlyName(This,pbstrName)	\
    ( (This)->lpVtbl -> get_MyFriendlyName(This,pbstrName) ) 

#define IMessengerAdvanced_put_MyStatus(This,mStatus)	\
    ( (This)->lpVtbl -> put_MyStatus(This,mStatus) ) 

#define IMessengerAdvanced_get_MyStatus(This,pmStatus)	\
    ( (This)->lpVtbl -> get_MyStatus(This,pmStatus) ) 

#define IMessengerAdvanced_get_UnreadEmailCount(This,mFolder,plCount)	\
    ( (This)->lpVtbl -> get_UnreadEmailCount(This,mFolder,plCount) ) 

#define IMessengerAdvanced_get_MyServiceName(This,pbstrServiceName)	\
    ( (This)->lpVtbl -> get_MyServiceName(This,pbstrServiceName) ) 

#define IMessengerAdvanced_get_MyPhoneNumber(This,PhoneType,pbstrNumber)	\
    ( (This)->lpVtbl -> get_MyPhoneNumber(This,PhoneType,pbstrNumber) ) 

#define IMessengerAdvanced_get_MyProperty(This,ePropType,pvPropVal)	\
    ( (This)->lpVtbl -> get_MyProperty(This,ePropType,pvPropVal) ) 

#define IMessengerAdvanced_put_MyProperty(This,ePropType,vPropVal)	\
    ( (This)->lpVtbl -> put_MyProperty(This,ePropType,vPropVal) ) 

#define IMessengerAdvanced_get_MyServiceId(This,pbstrServiceId)	\
    ( (This)->lpVtbl -> get_MyServiceId(This,pbstrServiceId) ) 

#define IMessengerAdvanced_get_Services(This,ppdispServices)	\
    ( (This)->lpVtbl -> get_Services(This,ppdispServices) ) 


#define IMessengerAdvanced_get_ContactsSortOrder(This,pSort)	\
    ( (This)->lpVtbl -> get_ContactsSortOrder(This,pSort) ) 

#define IMessengerAdvanced_put_ContactsSortOrder(This,Sort)	\
    ( (This)->lpVtbl -> put_ContactsSortOrder(This,Sort) ) 

#define IMessengerAdvanced_StartVideo(This,vContact,ppMWindow)	\
    ( (This)->lpVtbl -> StartVideo(This,vContact,ppMWindow) ) 

#define IMessengerAdvanced_get_MyGroups(This,ppMGroups)	\
    ( (This)->lpVtbl -> get_MyGroups(This,ppMGroups) ) 

#define IMessengerAdvanced_CreateGroup(This,bstrName,vService,ppGroup)	\
    ( (This)->lpVtbl -> CreateGroup(This,bstrName,vService,ppGroup) ) 


#define IMessengerAdvanced_get_Property(This,ePropType,pvPropVal)	\
    ( (This)->lpVtbl -> get_Property(This,ePropType,pvPropVal) ) 

#define IMessengerAdvanced_put_Property(This,ePropType,vPropVal)	\
    ( (This)->lpVtbl -> put_Property(This,ePropType,vPropVal) ) 


#define IMessengerAdvanced_StartConversation(This,ConversationType,vParticipants,vContextualData,vSubject,vConversationIndex,vConversationData,pvWndHnd)	\
    ( (This)->lpVtbl -> StartConversation(This,ConversationType,vParticipants,vContextualData,vSubject,vConversationIndex,vConversationData,pvWndHnd) ) 

#define IMessengerAdvanced_GetAuthenticationInfo(This,pbstrAuthInfo)	\
    ( (This)->lpVtbl -> GetAuthenticationInfo(This,pbstrAuthInfo) ) 

#endif /* COBJMACROS */


#endif 	/* C style interface */




#endif 	/* __IMessengerAdvanced_INTERFACE_DEFINED__ */


#ifndef __IMessengerContactResolution_INTERFACE_DEFINED__
#define __IMessengerContactResolution_INTERFACE_DEFINED__

/* interface IMessengerContactResolution */
/* [object][oleautomation][dual][helpcontext][helpstring][uuid] */ 


EXTERN_C const IID IID_IMessengerContactResolution;

#if defined(__cplusplus) && !defined(CINTERFACE)
    
    MIDL_INTERFACE("53A5023D-6872-454a-9A4F-827F18CFBE02")
    IMessengerContactResolution : public IDispatch
    {
    public:
        virtual /* [helpcontext][helpstring] */ HRESULT STDMETHODCALLTYPE ResolveContact( 
            /* [in] */ ADDRESS_TYPE AddressType,
            /* [in] */ CONTACT_RESOLUTION_TYPE ResolutionType,
            /* [in] */ BSTR bstrAddress,
            /* [retval][out] */ BSTR *pbstrIMAddress) = 0;
        
    };
    
#else 	/* C style interface */

    typedef struct IMessengerContactResolutionVtbl
    {
        BEGIN_INTERFACE
        
        HRESULT ( STDMETHODCALLTYPE *QueryInterface )( 
            IMessengerContactResolution * This,
            /* [in] */ REFIID riid,
            /* [annotation][iid_is][out] */ 
            __RPC__deref_out  void **ppvObject);
        
        ULONG ( STDMETHODCALLTYPE *AddRef )( 
            IMessengerContactResolution * This);
        
        ULONG ( STDMETHODCALLTYPE *Release )( 
            IMessengerContactResolution * This);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfoCount )( 
            IMessengerContactResolution * This,
            /* [out] */ UINT *pctinfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfo )( 
            IMessengerContactResolution * This,
            /* [in] */ UINT iTInfo,
            /* [in] */ LCID lcid,
            /* [out] */ ITypeInfo **ppTInfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetIDsOfNames )( 
            IMessengerContactResolution * This,
            /* [in] */ REFIID riid,
            /* [size_is][in] */ LPOLESTR *rgszNames,
            /* [range][in] */ UINT cNames,
            /* [in] */ LCID lcid,
            /* [size_is][out] */ DISPID *rgDispId);
        
        /* [local] */ HRESULT ( STDMETHODCALLTYPE *Invoke )( 
            IMessengerContactResolution * This,
            /* [in] */ DISPID dispIdMember,
            /* [in] */ REFIID riid,
            /* [in] */ LCID lcid,
            /* [in] */ WORD wFlags,
            /* [out][in] */ DISPPARAMS *pDispParams,
            /* [out] */ VARIANT *pVarResult,
            /* [out] */ EXCEPINFO *pExcepInfo,
            /* [out] */ UINT *puArgErr);
        
        /* [helpcontext][helpstring] */ HRESULT ( STDMETHODCALLTYPE *ResolveContact )( 
            IMessengerContactResolution * This,
            /* [in] */ ADDRESS_TYPE AddressType,
            /* [in] */ CONTACT_RESOLUTION_TYPE ResolutionType,
            /* [in] */ BSTR bstrAddress,
            /* [retval][out] */ BSTR *pbstrIMAddress);
        
        END_INTERFACE
    } IMessengerContactResolutionVtbl;

    interface IMessengerContactResolution
    {
        CONST_VTBL struct IMessengerContactResolutionVtbl *lpVtbl;
    };

    

#ifdef COBJMACROS


#define IMessengerContactResolution_QueryInterface(This,riid,ppvObject)	\
    ( (This)->lpVtbl -> QueryInterface(This,riid,ppvObject) ) 

#define IMessengerContactResolution_AddRef(This)	\
    ( (This)->lpVtbl -> AddRef(This) ) 

#define IMessengerContactResolution_Release(This)	\
    ( (This)->lpVtbl -> Release(This) ) 


#define IMessengerContactResolution_GetTypeInfoCount(This,pctinfo)	\
    ( (This)->lpVtbl -> GetTypeInfoCount(This,pctinfo) ) 

#define IMessengerContactResolution_GetTypeInfo(This,iTInfo,lcid,ppTInfo)	\
    ( (This)->lpVtbl -> GetTypeInfo(This,iTInfo,lcid,ppTInfo) ) 

#define IMessengerContactResolution_GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId)	\
    ( (This)->lpVtbl -> GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId) ) 

#define IMessengerContactResolution_Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr)	\
    ( (This)->lpVtbl -> Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr) ) 


#define IMessengerContactResolution_ResolveContact(This,AddressType,ResolutionType,bstrAddress,pbstrIMAddress)	\
    ( (This)->lpVtbl -> ResolveContact(This,AddressType,ResolutionType,bstrAddress,pbstrIMAddress) ) 

#endif /* COBJMACROS */


#endif 	/* C style interface */




#endif 	/* __IMessengerContactResolution_INTERFACE_DEFINED__ */


#ifndef __DMessengerEvents_DISPINTERFACE_DEFINED__
#define __DMessengerEvents_DISPINTERFACE_DEFINED__

/* dispinterface DMessengerEvents */
/* [hidden][helpstring][uuid] */ 


EXTERN_C const IID DIID_DMessengerEvents;

#if defined(__cplusplus) && !defined(CINTERFACE)

    MIDL_INTERFACE("C9A6A6B6-9BC1-43a5-B06B-E58874EEBC96")
    DMessengerEvents : public IDispatch
    {
    };
    
#else 	/* C style interface */

    typedef struct DMessengerEventsVtbl
    {
        BEGIN_INTERFACE
        
        HRESULT ( STDMETHODCALLTYPE *QueryInterface )( 
            DMessengerEvents * This,
            /* [in] */ REFIID riid,
            /* [annotation][iid_is][out] */ 
            __RPC__deref_out  void **ppvObject);
        
        ULONG ( STDMETHODCALLTYPE *AddRef )( 
            DMessengerEvents * This);
        
        ULONG ( STDMETHODCALLTYPE *Release )( 
            DMessengerEvents * This);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfoCount )( 
            DMessengerEvents * This,
            /* [out] */ UINT *pctinfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfo )( 
            DMessengerEvents * This,
            /* [in] */ UINT iTInfo,
            /* [in] */ LCID lcid,
            /* [out] */ ITypeInfo **ppTInfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetIDsOfNames )( 
            DMessengerEvents * This,
            /* [in] */ REFIID riid,
            /* [size_is][in] */ LPOLESTR *rgszNames,
            /* [range][in] */ UINT cNames,
            /* [in] */ LCID lcid,
            /* [size_is][out] */ DISPID *rgDispId);
        
        /* [local] */ HRESULT ( STDMETHODCALLTYPE *Invoke )( 
            DMessengerEvents * This,
            /* [in] */ DISPID dispIdMember,
            /* [in] */ REFIID riid,
            /* [in] */ LCID lcid,
            /* [in] */ WORD wFlags,
            /* [out][in] */ DISPPARAMS *pDispParams,
            /* [out] */ VARIANT *pVarResult,
            /* [out] */ EXCEPINFO *pExcepInfo,
            /* [out] */ UINT *puArgErr);
        
        END_INTERFACE
    } DMessengerEventsVtbl;

    interface DMessengerEvents
    {
        CONST_VTBL struct DMessengerEventsVtbl *lpVtbl;
    };

    

#ifdef COBJMACROS


#define DMessengerEvents_QueryInterface(This,riid,ppvObject)	\
    ( (This)->lpVtbl -> QueryInterface(This,riid,ppvObject) ) 

#define DMessengerEvents_AddRef(This)	\
    ( (This)->lpVtbl -> AddRef(This) ) 

#define DMessengerEvents_Release(This)	\
    ( (This)->lpVtbl -> Release(This) ) 


#define DMessengerEvents_GetTypeInfoCount(This,pctinfo)	\
    ( (This)->lpVtbl -> GetTypeInfoCount(This,pctinfo) ) 

#define DMessengerEvents_GetTypeInfo(This,iTInfo,lcid,ppTInfo)	\
    ( (This)->lpVtbl -> GetTypeInfo(This,iTInfo,lcid,ppTInfo) ) 

#define DMessengerEvents_GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId)	\
    ( (This)->lpVtbl -> GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId) ) 

#define DMessengerEvents_Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr)	\
    ( (This)->lpVtbl -> Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr) ) 

#endif /* COBJMACROS */


#endif 	/* C style interface */


#endif 	/* __DMessengerEvents_DISPINTERFACE_DEFINED__ */


#ifndef __IMessengerWindow_INTERFACE_DEFINED__
#define __IMessengerWindow_INTERFACE_DEFINED__

/* interface IMessengerWindow */
/* [object][oleautomation][dual][helpcontext][helpstring][uuid] */ 


EXTERN_C const IID IID_IMessengerWindow;

#if defined(__cplusplus) && !defined(CINTERFACE)
    
    MIDL_INTERFACE("D6B0E4C8-FAD6-4885-B271-0DC5A584ADF8")
    IMessengerWindow : public IDispatch
    {
    public:
        virtual /* [helpcontext][helpstring][id] */ HRESULT STDMETHODCALLTYPE Close( void) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_HWND( 
            /* [retval][out] */ long *phWnd) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_Left( 
            /* [retval][out] */ LONG *plLeft) = 0;
        
        virtual /* [helpcontext][helpstring][propput][id] */ HRESULT STDMETHODCALLTYPE put_Left( 
            /* [in] */ LONG lLeft) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_Top( 
            /* [retval][out] */ LONG *plTop) = 0;
        
        virtual /* [helpcontext][helpstring][propput][id] */ HRESULT STDMETHODCALLTYPE put_Top( 
            /* [in] */ LONG lTop) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_Width( 
            /* [retval][out] */ LONG *plWidth) = 0;
        
        virtual /* [helpcontext][helpstring][propput][id] */ HRESULT STDMETHODCALLTYPE put_Width( 
            /* [in] */ LONG lWidth) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_Height( 
            /* [retval][out] */ LONG *plHeight) = 0;
        
        virtual /* [helpcontext][helpstring][propput][id] */ HRESULT STDMETHODCALLTYPE put_Height( 
            /* [in] */ LONG lHeight) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_IsClosed( 
            /* [retval][out] */ VARIANT_BOOL *pBoolClose) = 0;
        
        virtual /* [helpcontext][helpstring][id] */ HRESULT STDMETHODCALLTYPE Show( void) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_Property( 
            /* [in] */ MWINDOWPROPERTY ePropType,
            /* [retval][out] */ VARIANT *pvPropVal) = 0;
        
        virtual /* [helpcontext][helpstring][propput][id] */ HRESULT STDMETHODCALLTYPE put_Property( 
            /* [in] */ MWINDOWPROPERTY ePropType,
            /* [in] */ VARIANT vPropVal) = 0;
        
    };
    
#else 	/* C style interface */

    typedef struct IMessengerWindowVtbl
    {
        BEGIN_INTERFACE
        
        HRESULT ( STDMETHODCALLTYPE *QueryInterface )( 
            IMessengerWindow * This,
            /* [in] */ REFIID riid,
            /* [annotation][iid_is][out] */ 
            __RPC__deref_out  void **ppvObject);
        
        ULONG ( STDMETHODCALLTYPE *AddRef )( 
            IMessengerWindow * This);
        
        ULONG ( STDMETHODCALLTYPE *Release )( 
            IMessengerWindow * This);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfoCount )( 
            IMessengerWindow * This,
            /* [out] */ UINT *pctinfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfo )( 
            IMessengerWindow * This,
            /* [in] */ UINT iTInfo,
            /* [in] */ LCID lcid,
            /* [out] */ ITypeInfo **ppTInfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetIDsOfNames )( 
            IMessengerWindow * This,
            /* [in] */ REFIID riid,
            /* [size_is][in] */ LPOLESTR *rgszNames,
            /* [range][in] */ UINT cNames,
            /* [in] */ LCID lcid,
            /* [size_is][out] */ DISPID *rgDispId);
        
        /* [local] */ HRESULT ( STDMETHODCALLTYPE *Invoke )( 
            IMessengerWindow * This,
            /* [in] */ DISPID dispIdMember,
            /* [in] */ REFIID riid,
            /* [in] */ LCID lcid,
            /* [in] */ WORD wFlags,
            /* [out][in] */ DISPPARAMS *pDispParams,
            /* [out] */ VARIANT *pVarResult,
            /* [out] */ EXCEPINFO *pExcepInfo,
            /* [out] */ UINT *puArgErr);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *Close )( 
            IMessengerWindow * This);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_HWND )( 
            IMessengerWindow * This,
            /* [retval][out] */ long *phWnd);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Left )( 
            IMessengerWindow * This,
            /* [retval][out] */ LONG *plLeft);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_Left )( 
            IMessengerWindow * This,
            /* [in] */ LONG lLeft);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Top )( 
            IMessengerWindow * This,
            /* [retval][out] */ LONG *plTop);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_Top )( 
            IMessengerWindow * This,
            /* [in] */ LONG lTop);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Width )( 
            IMessengerWindow * This,
            /* [retval][out] */ LONG *plWidth);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_Width )( 
            IMessengerWindow * This,
            /* [in] */ LONG lWidth);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Height )( 
            IMessengerWindow * This,
            /* [retval][out] */ LONG *plHeight);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_Height )( 
            IMessengerWindow * This,
            /* [in] */ LONG lHeight);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_IsClosed )( 
            IMessengerWindow * This,
            /* [retval][out] */ VARIANT_BOOL *pBoolClose);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *Show )( 
            IMessengerWindow * This);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Property )( 
            IMessengerWindow * This,
            /* [in] */ MWINDOWPROPERTY ePropType,
            /* [retval][out] */ VARIANT *pvPropVal);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_Property )( 
            IMessengerWindow * This,
            /* [in] */ MWINDOWPROPERTY ePropType,
            /* [in] */ VARIANT vPropVal);
        
        END_INTERFACE
    } IMessengerWindowVtbl;

    interface IMessengerWindow
    {
        CONST_VTBL struct IMessengerWindowVtbl *lpVtbl;
    };

    

#ifdef COBJMACROS


#define IMessengerWindow_QueryInterface(This,riid,ppvObject)	\
    ( (This)->lpVtbl -> QueryInterface(This,riid,ppvObject) ) 

#define IMessengerWindow_AddRef(This)	\
    ( (This)->lpVtbl -> AddRef(This) ) 

#define IMessengerWindow_Release(This)	\
    ( (This)->lpVtbl -> Release(This) ) 


#define IMessengerWindow_GetTypeInfoCount(This,pctinfo)	\
    ( (This)->lpVtbl -> GetTypeInfoCount(This,pctinfo) ) 

#define IMessengerWindow_GetTypeInfo(This,iTInfo,lcid,ppTInfo)	\
    ( (This)->lpVtbl -> GetTypeInfo(This,iTInfo,lcid,ppTInfo) ) 

#define IMessengerWindow_GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId)	\
    ( (This)->lpVtbl -> GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId) ) 

#define IMessengerWindow_Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr)	\
    ( (This)->lpVtbl -> Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr) ) 


#define IMessengerWindow_Close(This)	\
    ( (This)->lpVtbl -> Close(This) ) 

#define IMessengerWindow_get_HWND(This,phWnd)	\
    ( (This)->lpVtbl -> get_HWND(This,phWnd) ) 

#define IMessengerWindow_get_Left(This,plLeft)	\
    ( (This)->lpVtbl -> get_Left(This,plLeft) ) 

#define IMessengerWindow_put_Left(This,lLeft)	\
    ( (This)->lpVtbl -> put_Left(This,lLeft) ) 

#define IMessengerWindow_get_Top(This,plTop)	\
    ( (This)->lpVtbl -> get_Top(This,plTop) ) 

#define IMessengerWindow_put_Top(This,lTop)	\
    ( (This)->lpVtbl -> put_Top(This,lTop) ) 

#define IMessengerWindow_get_Width(This,plWidth)	\
    ( (This)->lpVtbl -> get_Width(This,plWidth) ) 

#define IMessengerWindow_put_Width(This,lWidth)	\
    ( (This)->lpVtbl -> put_Width(This,lWidth) ) 

#define IMessengerWindow_get_Height(This,plHeight)	\
    ( (This)->lpVtbl -> get_Height(This,plHeight) ) 

#define IMessengerWindow_put_Height(This,lHeight)	\
    ( (This)->lpVtbl -> put_Height(This,lHeight) ) 

#define IMessengerWindow_get_IsClosed(This,pBoolClose)	\
    ( (This)->lpVtbl -> get_IsClosed(This,pBoolClose) ) 

#define IMessengerWindow_Show(This)	\
    ( (This)->lpVtbl -> Show(This) ) 

#define IMessengerWindow_get_Property(This,ePropType,pvPropVal)	\
    ( (This)->lpVtbl -> get_Property(This,ePropType,pvPropVal) ) 

#define IMessengerWindow_put_Property(This,ePropType,vPropVal)	\
    ( (This)->lpVtbl -> put_Property(This,ePropType,vPropVal) ) 

#endif /* COBJMACROS */


#endif 	/* C style interface */




#endif 	/* __IMessengerWindow_INTERFACE_DEFINED__ */


#ifndef __IMessengerConversationWnd_INTERFACE_DEFINED__
#define __IMessengerConversationWnd_INTERFACE_DEFINED__

/* interface IMessengerConversationWnd */
/* [object][oleautomation][dual][helpcontext][helpstring][uuid] */ 


EXTERN_C const IID IID_IMessengerConversationWnd;

#if defined(__cplusplus) && !defined(CINTERFACE)
    
    MIDL_INTERFACE("D6B0E4C9-FAD6-4885-B271-0DC5A584ADF8")
    IMessengerConversationWnd : public IMessengerWindow
    {
    public:
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_Contacts( 
            /* [retval][out] */ IDispatch **pContacts) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_History( 
            /* [retval][out] */ BSTR *bstrHistoryText) = 0;
        
        virtual /* [helpcontext][helpstring][id] */ HRESULT STDMETHODCALLTYPE AddContact( 
            /* [in] */ VARIANT vContact) = 0;
        
    };
    
#else 	/* C style interface */

    typedef struct IMessengerConversationWndVtbl
    {
        BEGIN_INTERFACE
        
        HRESULT ( STDMETHODCALLTYPE *QueryInterface )( 
            IMessengerConversationWnd * This,
            /* [in] */ REFIID riid,
            /* [annotation][iid_is][out] */ 
            __RPC__deref_out  void **ppvObject);
        
        ULONG ( STDMETHODCALLTYPE *AddRef )( 
            IMessengerConversationWnd * This);
        
        ULONG ( STDMETHODCALLTYPE *Release )( 
            IMessengerConversationWnd * This);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfoCount )( 
            IMessengerConversationWnd * This,
            /* [out] */ UINT *pctinfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfo )( 
            IMessengerConversationWnd * This,
            /* [in] */ UINT iTInfo,
            /* [in] */ LCID lcid,
            /* [out] */ ITypeInfo **ppTInfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetIDsOfNames )( 
            IMessengerConversationWnd * This,
            /* [in] */ REFIID riid,
            /* [size_is][in] */ LPOLESTR *rgszNames,
            /* [range][in] */ UINT cNames,
            /* [in] */ LCID lcid,
            /* [size_is][out] */ DISPID *rgDispId);
        
        /* [local] */ HRESULT ( STDMETHODCALLTYPE *Invoke )( 
            IMessengerConversationWnd * This,
            /* [in] */ DISPID dispIdMember,
            /* [in] */ REFIID riid,
            /* [in] */ LCID lcid,
            /* [in] */ WORD wFlags,
            /* [out][in] */ DISPPARAMS *pDispParams,
            /* [out] */ VARIANT *pVarResult,
            /* [out] */ EXCEPINFO *pExcepInfo,
            /* [out] */ UINT *puArgErr);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *Close )( 
            IMessengerConversationWnd * This);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_HWND )( 
            IMessengerConversationWnd * This,
            /* [retval][out] */ long *phWnd);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Left )( 
            IMessengerConversationWnd * This,
            /* [retval][out] */ LONG *plLeft);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_Left )( 
            IMessengerConversationWnd * This,
            /* [in] */ LONG lLeft);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Top )( 
            IMessengerConversationWnd * This,
            /* [retval][out] */ LONG *plTop);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_Top )( 
            IMessengerConversationWnd * This,
            /* [in] */ LONG lTop);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Width )( 
            IMessengerConversationWnd * This,
            /* [retval][out] */ LONG *plWidth);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_Width )( 
            IMessengerConversationWnd * This,
            /* [in] */ LONG lWidth);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Height )( 
            IMessengerConversationWnd * This,
            /* [retval][out] */ LONG *plHeight);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_Height )( 
            IMessengerConversationWnd * This,
            /* [in] */ LONG lHeight);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_IsClosed )( 
            IMessengerConversationWnd * This,
            /* [retval][out] */ VARIANT_BOOL *pBoolClose);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *Show )( 
            IMessengerConversationWnd * This);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Property )( 
            IMessengerConversationWnd * This,
            /* [in] */ MWINDOWPROPERTY ePropType,
            /* [retval][out] */ VARIANT *pvPropVal);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_Property )( 
            IMessengerConversationWnd * This,
            /* [in] */ MWINDOWPROPERTY ePropType,
            /* [in] */ VARIANT vPropVal);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Contacts )( 
            IMessengerConversationWnd * This,
            /* [retval][out] */ IDispatch **pContacts);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_History )( 
            IMessengerConversationWnd * This,
            /* [retval][out] */ BSTR *bstrHistoryText);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *AddContact )( 
            IMessengerConversationWnd * This,
            /* [in] */ VARIANT vContact);
        
        END_INTERFACE
    } IMessengerConversationWndVtbl;

    interface IMessengerConversationWnd
    {
        CONST_VTBL struct IMessengerConversationWndVtbl *lpVtbl;
    };

    

#ifdef COBJMACROS


#define IMessengerConversationWnd_QueryInterface(This,riid,ppvObject)	\
    ( (This)->lpVtbl -> QueryInterface(This,riid,ppvObject) ) 

#define IMessengerConversationWnd_AddRef(This)	\
    ( (This)->lpVtbl -> AddRef(This) ) 

#define IMessengerConversationWnd_Release(This)	\
    ( (This)->lpVtbl -> Release(This) ) 


#define IMessengerConversationWnd_GetTypeInfoCount(This,pctinfo)	\
    ( (This)->lpVtbl -> GetTypeInfoCount(This,pctinfo) ) 

#define IMessengerConversationWnd_GetTypeInfo(This,iTInfo,lcid,ppTInfo)	\
    ( (This)->lpVtbl -> GetTypeInfo(This,iTInfo,lcid,ppTInfo) ) 

#define IMessengerConversationWnd_GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId)	\
    ( (This)->lpVtbl -> GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId) ) 

#define IMessengerConversationWnd_Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr)	\
    ( (This)->lpVtbl -> Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr) ) 


#define IMessengerConversationWnd_Close(This)	\
    ( (This)->lpVtbl -> Close(This) ) 

#define IMessengerConversationWnd_get_HWND(This,phWnd)	\
    ( (This)->lpVtbl -> get_HWND(This,phWnd) ) 

#define IMessengerConversationWnd_get_Left(This,plLeft)	\
    ( (This)->lpVtbl -> get_Left(This,plLeft) ) 

#define IMessengerConversationWnd_put_Left(This,lLeft)	\
    ( (This)->lpVtbl -> put_Left(This,lLeft) ) 

#define IMessengerConversationWnd_get_Top(This,plTop)	\
    ( (This)->lpVtbl -> get_Top(This,plTop) ) 

#define IMessengerConversationWnd_put_Top(This,lTop)	\
    ( (This)->lpVtbl -> put_Top(This,lTop) ) 

#define IMessengerConversationWnd_get_Width(This,plWidth)	\
    ( (This)->lpVtbl -> get_Width(This,plWidth) ) 

#define IMessengerConversationWnd_put_Width(This,lWidth)	\
    ( (This)->lpVtbl -> put_Width(This,lWidth) ) 

#define IMessengerConversationWnd_get_Height(This,plHeight)	\
    ( (This)->lpVtbl -> get_Height(This,plHeight) ) 

#define IMessengerConversationWnd_put_Height(This,lHeight)	\
    ( (This)->lpVtbl -> put_Height(This,lHeight) ) 

#define IMessengerConversationWnd_get_IsClosed(This,pBoolClose)	\
    ( (This)->lpVtbl -> get_IsClosed(This,pBoolClose) ) 

#define IMessengerConversationWnd_Show(This)	\
    ( (This)->lpVtbl -> Show(This) ) 

#define IMessengerConversationWnd_get_Property(This,ePropType,pvPropVal)	\
    ( (This)->lpVtbl -> get_Property(This,ePropType,pvPropVal) ) 

#define IMessengerConversationWnd_put_Property(This,ePropType,vPropVal)	\
    ( (This)->lpVtbl -> put_Property(This,ePropType,vPropVal) ) 


#define IMessengerConversationWnd_get_Contacts(This,pContacts)	\
    ( (This)->lpVtbl -> get_Contacts(This,pContacts) ) 

#define IMessengerConversationWnd_get_History(This,bstrHistoryText)	\
    ( (This)->lpVtbl -> get_History(This,bstrHistoryText) ) 

#define IMessengerConversationWnd_AddContact(This,vContact)	\
    ( (This)->lpVtbl -> AddContact(This,vContact) ) 

#endif /* COBJMACROS */


#endif 	/* C style interface */




#endif 	/* __IMessengerConversationWnd_INTERFACE_DEFINED__ */


#ifndef __IMessengerConversationWndAdvanced_INTERFACE_DEFINED__
#define __IMessengerConversationWndAdvanced_INTERFACE_DEFINED__

/* interface IMessengerConversationWndAdvanced */
/* [object][oleautomation][dual][helpcontext][helpstring][uuid] */ 


EXTERN_C const IID IID_IMessengerConversationWndAdvanced;

#if defined(__cplusplus) && !defined(CINTERFACE)
    
    MIDL_INTERFACE("7C613A61-0633-4c69-AFF0-5BA9F1D28887")
    IMessengerConversationWndAdvanced : public IMessengerConversationWnd
    {
    public:
        virtual /* [helpcontext][helpstring] */ HRESULT STDMETHODCALLTYPE SendText( 
            /* [in] */ BSTR bstrTextMessage) = 0;
        
    };
    
#else 	/* C style interface */

    typedef struct IMessengerConversationWndAdvancedVtbl
    {
        BEGIN_INTERFACE
        
        HRESULT ( STDMETHODCALLTYPE *QueryInterface )( 
            IMessengerConversationWndAdvanced * This,
            /* [in] */ REFIID riid,
            /* [annotation][iid_is][out] */ 
            __RPC__deref_out  void **ppvObject);
        
        ULONG ( STDMETHODCALLTYPE *AddRef )( 
            IMessengerConversationWndAdvanced * This);
        
        ULONG ( STDMETHODCALLTYPE *Release )( 
            IMessengerConversationWndAdvanced * This);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfoCount )( 
            IMessengerConversationWndAdvanced * This,
            /* [out] */ UINT *pctinfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfo )( 
            IMessengerConversationWndAdvanced * This,
            /* [in] */ UINT iTInfo,
            /* [in] */ LCID lcid,
            /* [out] */ ITypeInfo **ppTInfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetIDsOfNames )( 
            IMessengerConversationWndAdvanced * This,
            /* [in] */ REFIID riid,
            /* [size_is][in] */ LPOLESTR *rgszNames,
            /* [range][in] */ UINT cNames,
            /* [in] */ LCID lcid,
            /* [size_is][out] */ DISPID *rgDispId);
        
        /* [local] */ HRESULT ( STDMETHODCALLTYPE *Invoke )( 
            IMessengerConversationWndAdvanced * This,
            /* [in] */ DISPID dispIdMember,
            /* [in] */ REFIID riid,
            /* [in] */ LCID lcid,
            /* [in] */ WORD wFlags,
            /* [out][in] */ DISPPARAMS *pDispParams,
            /* [out] */ VARIANT *pVarResult,
            /* [out] */ EXCEPINFO *pExcepInfo,
            /* [out] */ UINT *puArgErr);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *Close )( 
            IMessengerConversationWndAdvanced * This);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_HWND )( 
            IMessengerConversationWndAdvanced * This,
            /* [retval][out] */ long *phWnd);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Left )( 
            IMessengerConversationWndAdvanced * This,
            /* [retval][out] */ LONG *plLeft);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_Left )( 
            IMessengerConversationWndAdvanced * This,
            /* [in] */ LONG lLeft);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Top )( 
            IMessengerConversationWndAdvanced * This,
            /* [retval][out] */ LONG *plTop);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_Top )( 
            IMessengerConversationWndAdvanced * This,
            /* [in] */ LONG lTop);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Width )( 
            IMessengerConversationWndAdvanced * This,
            /* [retval][out] */ LONG *plWidth);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_Width )( 
            IMessengerConversationWndAdvanced * This,
            /* [in] */ LONG lWidth);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Height )( 
            IMessengerConversationWndAdvanced * This,
            /* [retval][out] */ LONG *plHeight);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_Height )( 
            IMessengerConversationWndAdvanced * This,
            /* [in] */ LONG lHeight);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_IsClosed )( 
            IMessengerConversationWndAdvanced * This,
            /* [retval][out] */ VARIANT_BOOL *pBoolClose);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *Show )( 
            IMessengerConversationWndAdvanced * This);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Property )( 
            IMessengerConversationWndAdvanced * This,
            /* [in] */ MWINDOWPROPERTY ePropType,
            /* [retval][out] */ VARIANT *pvPropVal);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_Property )( 
            IMessengerConversationWndAdvanced * This,
            /* [in] */ MWINDOWPROPERTY ePropType,
            /* [in] */ VARIANT vPropVal);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Contacts )( 
            IMessengerConversationWndAdvanced * This,
            /* [retval][out] */ IDispatch **pContacts);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_History )( 
            IMessengerConversationWndAdvanced * This,
            /* [retval][out] */ BSTR *bstrHistoryText);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *AddContact )( 
            IMessengerConversationWndAdvanced * This,
            /* [in] */ VARIANT vContact);
        
        /* [helpcontext][helpstring] */ HRESULT ( STDMETHODCALLTYPE *SendText )( 
            IMessengerConversationWndAdvanced * This,
            /* [in] */ BSTR bstrTextMessage);
        
        END_INTERFACE
    } IMessengerConversationWndAdvancedVtbl;

    interface IMessengerConversationWndAdvanced
    {
        CONST_VTBL struct IMessengerConversationWndAdvancedVtbl *lpVtbl;
    };

    

#ifdef COBJMACROS


#define IMessengerConversationWndAdvanced_QueryInterface(This,riid,ppvObject)	\
    ( (This)->lpVtbl -> QueryInterface(This,riid,ppvObject) ) 

#define IMessengerConversationWndAdvanced_AddRef(This)	\
    ( (This)->lpVtbl -> AddRef(This) ) 

#define IMessengerConversationWndAdvanced_Release(This)	\
    ( (This)->lpVtbl -> Release(This) ) 


#define IMessengerConversationWndAdvanced_GetTypeInfoCount(This,pctinfo)	\
    ( (This)->lpVtbl -> GetTypeInfoCount(This,pctinfo) ) 

#define IMessengerConversationWndAdvanced_GetTypeInfo(This,iTInfo,lcid,ppTInfo)	\
    ( (This)->lpVtbl -> GetTypeInfo(This,iTInfo,lcid,ppTInfo) ) 

#define IMessengerConversationWndAdvanced_GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId)	\
    ( (This)->lpVtbl -> GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId) ) 

#define IMessengerConversationWndAdvanced_Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr)	\
    ( (This)->lpVtbl -> Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr) ) 


#define IMessengerConversationWndAdvanced_Close(This)	\
    ( (This)->lpVtbl -> Close(This) ) 

#define IMessengerConversationWndAdvanced_get_HWND(This,phWnd)	\
    ( (This)->lpVtbl -> get_HWND(This,phWnd) ) 

#define IMessengerConversationWndAdvanced_get_Left(This,plLeft)	\
    ( (This)->lpVtbl -> get_Left(This,plLeft) ) 

#define IMessengerConversationWndAdvanced_put_Left(This,lLeft)	\
    ( (This)->lpVtbl -> put_Left(This,lLeft) ) 

#define IMessengerConversationWndAdvanced_get_Top(This,plTop)	\
    ( (This)->lpVtbl -> get_Top(This,plTop) ) 

#define IMessengerConversationWndAdvanced_put_Top(This,lTop)	\
    ( (This)->lpVtbl -> put_Top(This,lTop) ) 

#define IMessengerConversationWndAdvanced_get_Width(This,plWidth)	\
    ( (This)->lpVtbl -> get_Width(This,plWidth) ) 

#define IMessengerConversationWndAdvanced_put_Width(This,lWidth)	\
    ( (This)->lpVtbl -> put_Width(This,lWidth) ) 

#define IMessengerConversationWndAdvanced_get_Height(This,plHeight)	\
    ( (This)->lpVtbl -> get_Height(This,plHeight) ) 

#define IMessengerConversationWndAdvanced_put_Height(This,lHeight)	\
    ( (This)->lpVtbl -> put_Height(This,lHeight) ) 

#define IMessengerConversationWndAdvanced_get_IsClosed(This,pBoolClose)	\
    ( (This)->lpVtbl -> get_IsClosed(This,pBoolClose) ) 

#define IMessengerConversationWndAdvanced_Show(This)	\
    ( (This)->lpVtbl -> Show(This) ) 

#define IMessengerConversationWndAdvanced_get_Property(This,ePropType,pvPropVal)	\
    ( (This)->lpVtbl -> get_Property(This,ePropType,pvPropVal) ) 

#define IMessengerConversationWndAdvanced_put_Property(This,ePropType,vPropVal)	\
    ( (This)->lpVtbl -> put_Property(This,ePropType,vPropVal) ) 


#define IMessengerConversationWndAdvanced_get_Contacts(This,pContacts)	\
    ( (This)->lpVtbl -> get_Contacts(This,pContacts) ) 

#define IMessengerConversationWndAdvanced_get_History(This,bstrHistoryText)	\
    ( (This)->lpVtbl -> get_History(This,bstrHistoryText) ) 

#define IMessengerConversationWndAdvanced_AddContact(This,vContact)	\
    ( (This)->lpVtbl -> AddContact(This,vContact) ) 


#define IMessengerConversationWndAdvanced_SendText(This,bstrTextMessage)	\
    ( (This)->lpVtbl -> SendText(This,bstrTextMessage) ) 

#endif /* COBJMACROS */


#endif 	/* C style interface */




#endif 	/* __IMessengerConversationWndAdvanced_INTERFACE_DEFINED__ */


#ifndef __IMessengerContact_INTERFACE_DEFINED__
#define __IMessengerContact_INTERFACE_DEFINED__

/* interface IMessengerContact */
/* [object][oleautomation][dual][helpstring][uuid] */ 


EXTERN_C const IID IID_IMessengerContact;

#if defined(__cplusplus) && !defined(CINTERFACE)
    
    MIDL_INTERFACE("E7479A0F-BB19-44a5-968F-6F41D93EE0BC")
    IMessengerContact : public IDispatch
    {
    public:
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_FriendlyName( 
            /* [retval][out] */ BSTR *pbstrFriendlyName) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_Status( 
            /* [retval][out] */ MISTATUS *pMstate) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_SigninName( 
            /* [retval][out] */ BSTR *pbstrSigninName) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_ServiceName( 
            /* [retval][out] */ BSTR *pbstrServiceName) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_Blocked( 
            /* [retval][out] */ VARIANT_BOOL *pBoolBlock) = 0;
        
        virtual /* [helpcontext][helpstring][propput][id] */ HRESULT STDMETHODCALLTYPE put_Blocked( 
            /* [in] */ VARIANT_BOOL pBoolBlock) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_CanPage( 
            /* [retval][out] */ VARIANT_BOOL *pBoolPage) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_PhoneNumber( 
            /* [in] */ MPHONE_TYPE PhoneType,
            /* [retval][out] */ BSTR *bstrNumber) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_IsSelf( 
            /* [retval][out] */ VARIANT_BOOL *pBoolSelf) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_Property( 
            /* [in] */ MCONTACTPROPERTY ePropType,
            /* [retval][out] */ VARIANT *pvPropVal) = 0;
        
        virtual /* [helpcontext][helpstring][propput][id] */ HRESULT STDMETHODCALLTYPE put_Property( 
            /* [in] */ MCONTACTPROPERTY ePropType,
            /* [in] */ VARIANT vPropVal) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_ServiceId( 
            /* [retval][out] */ BSTR *pbstrServiceID) = 0;
        
    };
    
#else 	/* C style interface */

    typedef struct IMessengerContactVtbl
    {
        BEGIN_INTERFACE
        
        HRESULT ( STDMETHODCALLTYPE *QueryInterface )( 
            IMessengerContact * This,
            /* [in] */ REFIID riid,
            /* [annotation][iid_is][out] */ 
            __RPC__deref_out  void **ppvObject);
        
        ULONG ( STDMETHODCALLTYPE *AddRef )( 
            IMessengerContact * This);
        
        ULONG ( STDMETHODCALLTYPE *Release )( 
            IMessengerContact * This);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfoCount )( 
            IMessengerContact * This,
            /* [out] */ UINT *pctinfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfo )( 
            IMessengerContact * This,
            /* [in] */ UINT iTInfo,
            /* [in] */ LCID lcid,
            /* [out] */ ITypeInfo **ppTInfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetIDsOfNames )( 
            IMessengerContact * This,
            /* [in] */ REFIID riid,
            /* [size_is][in] */ LPOLESTR *rgszNames,
            /* [range][in] */ UINT cNames,
            /* [in] */ LCID lcid,
            /* [size_is][out] */ DISPID *rgDispId);
        
        /* [local] */ HRESULT ( STDMETHODCALLTYPE *Invoke )( 
            IMessengerContact * This,
            /* [in] */ DISPID dispIdMember,
            /* [in] */ REFIID riid,
            /* [in] */ LCID lcid,
            /* [in] */ WORD wFlags,
            /* [out][in] */ DISPPARAMS *pDispParams,
            /* [out] */ VARIANT *pVarResult,
            /* [out] */ EXCEPINFO *pExcepInfo,
            /* [out] */ UINT *puArgErr);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_FriendlyName )( 
            IMessengerContact * This,
            /* [retval][out] */ BSTR *pbstrFriendlyName);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Status )( 
            IMessengerContact * This,
            /* [retval][out] */ MISTATUS *pMstate);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_SigninName )( 
            IMessengerContact * This,
            /* [retval][out] */ BSTR *pbstrSigninName);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_ServiceName )( 
            IMessengerContact * This,
            /* [retval][out] */ BSTR *pbstrServiceName);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Blocked )( 
            IMessengerContact * This,
            /* [retval][out] */ VARIANT_BOOL *pBoolBlock);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_Blocked )( 
            IMessengerContact * This,
            /* [in] */ VARIANT_BOOL pBoolBlock);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_CanPage )( 
            IMessengerContact * This,
            /* [retval][out] */ VARIANT_BOOL *pBoolPage);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_PhoneNumber )( 
            IMessengerContact * This,
            /* [in] */ MPHONE_TYPE PhoneType,
            /* [retval][out] */ BSTR *bstrNumber);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_IsSelf )( 
            IMessengerContact * This,
            /* [retval][out] */ VARIANT_BOOL *pBoolSelf);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Property )( 
            IMessengerContact * This,
            /* [in] */ MCONTACTPROPERTY ePropType,
            /* [retval][out] */ VARIANT *pvPropVal);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_Property )( 
            IMessengerContact * This,
            /* [in] */ MCONTACTPROPERTY ePropType,
            /* [in] */ VARIANT vPropVal);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_ServiceId )( 
            IMessengerContact * This,
            /* [retval][out] */ BSTR *pbstrServiceID);
        
        END_INTERFACE
    } IMessengerContactVtbl;

    interface IMessengerContact
    {
        CONST_VTBL struct IMessengerContactVtbl *lpVtbl;
    };

    

#ifdef COBJMACROS


#define IMessengerContact_QueryInterface(This,riid,ppvObject)	\
    ( (This)->lpVtbl -> QueryInterface(This,riid,ppvObject) ) 

#define IMessengerContact_AddRef(This)	\
    ( (This)->lpVtbl -> AddRef(This) ) 

#define IMessengerContact_Release(This)	\
    ( (This)->lpVtbl -> Release(This) ) 


#define IMessengerContact_GetTypeInfoCount(This,pctinfo)	\
    ( (This)->lpVtbl -> GetTypeInfoCount(This,pctinfo) ) 

#define IMessengerContact_GetTypeInfo(This,iTInfo,lcid,ppTInfo)	\
    ( (This)->lpVtbl -> GetTypeInfo(This,iTInfo,lcid,ppTInfo) ) 

#define IMessengerContact_GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId)	\
    ( (This)->lpVtbl -> GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId) ) 

#define IMessengerContact_Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr)	\
    ( (This)->lpVtbl -> Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr) ) 


#define IMessengerContact_get_FriendlyName(This,pbstrFriendlyName)	\
    ( (This)->lpVtbl -> get_FriendlyName(This,pbstrFriendlyName) ) 

#define IMessengerContact_get_Status(This,pMstate)	\
    ( (This)->lpVtbl -> get_Status(This,pMstate) ) 

#define IMessengerContact_get_SigninName(This,pbstrSigninName)	\
    ( (This)->lpVtbl -> get_SigninName(This,pbstrSigninName) ) 

#define IMessengerContact_get_ServiceName(This,pbstrServiceName)	\
    ( (This)->lpVtbl -> get_ServiceName(This,pbstrServiceName) ) 

#define IMessengerContact_get_Blocked(This,pBoolBlock)	\
    ( (This)->lpVtbl -> get_Blocked(This,pBoolBlock) ) 

#define IMessengerContact_put_Blocked(This,pBoolBlock)	\
    ( (This)->lpVtbl -> put_Blocked(This,pBoolBlock) ) 

#define IMessengerContact_get_CanPage(This,pBoolPage)	\
    ( (This)->lpVtbl -> get_CanPage(This,pBoolPage) ) 

#define IMessengerContact_get_PhoneNumber(This,PhoneType,bstrNumber)	\
    ( (This)->lpVtbl -> get_PhoneNumber(This,PhoneType,bstrNumber) ) 

#define IMessengerContact_get_IsSelf(This,pBoolSelf)	\
    ( (This)->lpVtbl -> get_IsSelf(This,pBoolSelf) ) 

#define IMessengerContact_get_Property(This,ePropType,pvPropVal)	\
    ( (This)->lpVtbl -> get_Property(This,ePropType,pvPropVal) ) 

#define IMessengerContact_put_Property(This,ePropType,vPropVal)	\
    ( (This)->lpVtbl -> put_Property(This,ePropType,vPropVal) ) 

#define IMessengerContact_get_ServiceId(This,pbstrServiceID)	\
    ( (This)->lpVtbl -> get_ServiceId(This,pbstrServiceID) ) 

#endif /* COBJMACROS */


#endif 	/* C style interface */




#endif 	/* __IMessengerContact_INTERFACE_DEFINED__ */


#ifndef __IMessengerContactAdvanced_INTERFACE_DEFINED__
#define __IMessengerContactAdvanced_INTERFACE_DEFINED__

/* interface IMessengerContactAdvanced */
/* [object][oleautomation][dual][helpstring][uuid] */ 


EXTERN_C const IID IID_IMessengerContactAdvanced;

#if defined(__cplusplus) && !defined(CINTERFACE)
    
    MIDL_INTERFACE("086F69C0-2FBD-46b3-BE50-EC401AB86099")
    IMessengerContactAdvanced : public IMessengerContact
    {
    public:
        virtual /* [helpcontext][helpstring][propget] */ HRESULT STDMETHODCALLTYPE get_IsTagged( 
            /* [retval][out] */ VARIANT_BOOL *pBoolIsTagged) = 0;
        
        virtual /* [helpcontext][helpstring][propput] */ HRESULT STDMETHODCALLTYPE put_IsTagged( 
            /* [in] */ VARIANT_BOOL pBoolIsTagged) = 0;
        
        virtual /* [helpcontext][helpstring][propget] */ HRESULT STDMETHODCALLTYPE get_PresenceProperties( 
            /* [retval][out] */ VARIANT *pvPresenceProperties) = 0;
        
        virtual /* [helpcontext][helpstring][propput] */ HRESULT STDMETHODCALLTYPE put_PresenceProperties( 
            /* [in] */ VARIANT vPresenceProperties) = 0;
        
    };
    
#else 	/* C style interface */

    typedef struct IMessengerContactAdvancedVtbl
    {
        BEGIN_INTERFACE
        
        HRESULT ( STDMETHODCALLTYPE *QueryInterface )( 
            IMessengerContactAdvanced * This,
            /* [in] */ REFIID riid,
            /* [annotation][iid_is][out] */ 
            __RPC__deref_out  void **ppvObject);
        
        ULONG ( STDMETHODCALLTYPE *AddRef )( 
            IMessengerContactAdvanced * This);
        
        ULONG ( STDMETHODCALLTYPE *Release )( 
            IMessengerContactAdvanced * This);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfoCount )( 
            IMessengerContactAdvanced * This,
            /* [out] */ UINT *pctinfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfo )( 
            IMessengerContactAdvanced * This,
            /* [in] */ UINT iTInfo,
            /* [in] */ LCID lcid,
            /* [out] */ ITypeInfo **ppTInfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetIDsOfNames )( 
            IMessengerContactAdvanced * This,
            /* [in] */ REFIID riid,
            /* [size_is][in] */ LPOLESTR *rgszNames,
            /* [range][in] */ UINT cNames,
            /* [in] */ LCID lcid,
            /* [size_is][out] */ DISPID *rgDispId);
        
        /* [local] */ HRESULT ( STDMETHODCALLTYPE *Invoke )( 
            IMessengerContactAdvanced * This,
            /* [in] */ DISPID dispIdMember,
            /* [in] */ REFIID riid,
            /* [in] */ LCID lcid,
            /* [in] */ WORD wFlags,
            /* [out][in] */ DISPPARAMS *pDispParams,
            /* [out] */ VARIANT *pVarResult,
            /* [out] */ EXCEPINFO *pExcepInfo,
            /* [out] */ UINT *puArgErr);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_FriendlyName )( 
            IMessengerContactAdvanced * This,
            /* [retval][out] */ BSTR *pbstrFriendlyName);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Status )( 
            IMessengerContactAdvanced * This,
            /* [retval][out] */ MISTATUS *pMstate);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_SigninName )( 
            IMessengerContactAdvanced * This,
            /* [retval][out] */ BSTR *pbstrSigninName);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_ServiceName )( 
            IMessengerContactAdvanced * This,
            /* [retval][out] */ BSTR *pbstrServiceName);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Blocked )( 
            IMessengerContactAdvanced * This,
            /* [retval][out] */ VARIANT_BOOL *pBoolBlock);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_Blocked )( 
            IMessengerContactAdvanced * This,
            /* [in] */ VARIANT_BOOL pBoolBlock);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_CanPage )( 
            IMessengerContactAdvanced * This,
            /* [retval][out] */ VARIANT_BOOL *pBoolPage);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_PhoneNumber )( 
            IMessengerContactAdvanced * This,
            /* [in] */ MPHONE_TYPE PhoneType,
            /* [retval][out] */ BSTR *bstrNumber);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_IsSelf )( 
            IMessengerContactAdvanced * This,
            /* [retval][out] */ VARIANT_BOOL *pBoolSelf);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Property )( 
            IMessengerContactAdvanced * This,
            /* [in] */ MCONTACTPROPERTY ePropType,
            /* [retval][out] */ VARIANT *pvPropVal);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_Property )( 
            IMessengerContactAdvanced * This,
            /* [in] */ MCONTACTPROPERTY ePropType,
            /* [in] */ VARIANT vPropVal);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_ServiceId )( 
            IMessengerContactAdvanced * This,
            /* [retval][out] */ BSTR *pbstrServiceID);
        
        /* [helpcontext][helpstring][propget] */ HRESULT ( STDMETHODCALLTYPE *get_IsTagged )( 
            IMessengerContactAdvanced * This,
            /* [retval][out] */ VARIANT_BOOL *pBoolIsTagged);
        
        /* [helpcontext][helpstring][propput] */ HRESULT ( STDMETHODCALLTYPE *put_IsTagged )( 
            IMessengerContactAdvanced * This,
            /* [in] */ VARIANT_BOOL pBoolIsTagged);
        
        /* [helpcontext][helpstring][propget] */ HRESULT ( STDMETHODCALLTYPE *get_PresenceProperties )( 
            IMessengerContactAdvanced * This,
            /* [retval][out] */ VARIANT *pvPresenceProperties);
        
        /* [helpcontext][helpstring][propput] */ HRESULT ( STDMETHODCALLTYPE *put_PresenceProperties )( 
            IMessengerContactAdvanced * This,
            /* [in] */ VARIANT vPresenceProperties);
        
        END_INTERFACE
    } IMessengerContactAdvancedVtbl;

    interface IMessengerContactAdvanced
    {
        CONST_VTBL struct IMessengerContactAdvancedVtbl *lpVtbl;
    };

    

#ifdef COBJMACROS


#define IMessengerContactAdvanced_QueryInterface(This,riid,ppvObject)	\
    ( (This)->lpVtbl -> QueryInterface(This,riid,ppvObject) ) 

#define IMessengerContactAdvanced_AddRef(This)	\
    ( (This)->lpVtbl -> AddRef(This) ) 

#define IMessengerContactAdvanced_Release(This)	\
    ( (This)->lpVtbl -> Release(This) ) 


#define IMessengerContactAdvanced_GetTypeInfoCount(This,pctinfo)	\
    ( (This)->lpVtbl -> GetTypeInfoCount(This,pctinfo) ) 

#define IMessengerContactAdvanced_GetTypeInfo(This,iTInfo,lcid,ppTInfo)	\
    ( (This)->lpVtbl -> GetTypeInfo(This,iTInfo,lcid,ppTInfo) ) 

#define IMessengerContactAdvanced_GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId)	\
    ( (This)->lpVtbl -> GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId) ) 

#define IMessengerContactAdvanced_Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr)	\
    ( (This)->lpVtbl -> Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr) ) 


#define IMessengerContactAdvanced_get_FriendlyName(This,pbstrFriendlyName)	\
    ( (This)->lpVtbl -> get_FriendlyName(This,pbstrFriendlyName) ) 

#define IMessengerContactAdvanced_get_Status(This,pMstate)	\
    ( (This)->lpVtbl -> get_Status(This,pMstate) ) 

#define IMessengerContactAdvanced_get_SigninName(This,pbstrSigninName)	\
    ( (This)->lpVtbl -> get_SigninName(This,pbstrSigninName) ) 

#define IMessengerContactAdvanced_get_ServiceName(This,pbstrServiceName)	\
    ( (This)->lpVtbl -> get_ServiceName(This,pbstrServiceName) ) 

#define IMessengerContactAdvanced_get_Blocked(This,pBoolBlock)	\
    ( (This)->lpVtbl -> get_Blocked(This,pBoolBlock) ) 

#define IMessengerContactAdvanced_put_Blocked(This,pBoolBlock)	\
    ( (This)->lpVtbl -> put_Blocked(This,pBoolBlock) ) 

#define IMessengerContactAdvanced_get_CanPage(This,pBoolPage)	\
    ( (This)->lpVtbl -> get_CanPage(This,pBoolPage) ) 

#define IMessengerContactAdvanced_get_PhoneNumber(This,PhoneType,bstrNumber)	\
    ( (This)->lpVtbl -> get_PhoneNumber(This,PhoneType,bstrNumber) ) 

#define IMessengerContactAdvanced_get_IsSelf(This,pBoolSelf)	\
    ( (This)->lpVtbl -> get_IsSelf(This,pBoolSelf) ) 

#define IMessengerContactAdvanced_get_Property(This,ePropType,pvPropVal)	\
    ( (This)->lpVtbl -> get_Property(This,ePropType,pvPropVal) ) 

#define IMessengerContactAdvanced_put_Property(This,ePropType,vPropVal)	\
    ( (This)->lpVtbl -> put_Property(This,ePropType,vPropVal) ) 

#define IMessengerContactAdvanced_get_ServiceId(This,pbstrServiceID)	\
    ( (This)->lpVtbl -> get_ServiceId(This,pbstrServiceID) ) 


#define IMessengerContactAdvanced_get_IsTagged(This,pBoolIsTagged)	\
    ( (This)->lpVtbl -> get_IsTagged(This,pBoolIsTagged) ) 

#define IMessengerContactAdvanced_put_IsTagged(This,pBoolIsTagged)	\
    ( (This)->lpVtbl -> put_IsTagged(This,pBoolIsTagged) ) 

#define IMessengerContactAdvanced_get_PresenceProperties(This,pvPresenceProperties)	\
    ( (This)->lpVtbl -> get_PresenceProperties(This,pvPresenceProperties) ) 

#define IMessengerContactAdvanced_put_PresenceProperties(This,vPresenceProperties)	\
    ( (This)->lpVtbl -> put_PresenceProperties(This,vPresenceProperties) ) 

#endif /* COBJMACROS */


#endif 	/* C style interface */




#endif 	/* __IMessengerContactAdvanced_INTERFACE_DEFINED__ */


#ifndef __IMessengerContacts_INTERFACE_DEFINED__
#define __IMessengerContacts_INTERFACE_DEFINED__

/* interface IMessengerContacts */
/* [object][oleautomation][dual][helpstring][uuid] */ 


EXTERN_C const IID IID_IMessengerContacts;

#if defined(__cplusplus) && !defined(CINTERFACE)
    
    MIDL_INTERFACE("E7479A0D-BB19-44a5-968F-6F41D93EE0BC")
    IMessengerContacts : public IDispatch
    {
    public:
        virtual /* [helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_Count( 
            /* [retval][out] */ LONG *pcContacts) = 0;
        
        virtual /* [helpstring][id] */ HRESULT STDMETHODCALLTYPE Item( 
            /* [in] */ LONG Index,
            /* [retval][out] */ IDispatch **ppMContact) = 0;
        
        virtual /* [helpstring][id] */ HRESULT STDMETHODCALLTYPE Remove( 
            /* [in] */ IDispatch *pMContact) = 0;
        
        virtual /* [helpstring][restricted][propget][id] */ HRESULT STDMETHODCALLTYPE get__NewEnum( 
            /* [retval][out] */ IUnknown **ppUnknown) = 0;
        
    };
    
#else 	/* C style interface */

    typedef struct IMessengerContactsVtbl
    {
        BEGIN_INTERFACE
        
        HRESULT ( STDMETHODCALLTYPE *QueryInterface )( 
            IMessengerContacts * This,
            /* [in] */ REFIID riid,
            /* [annotation][iid_is][out] */ 
            __RPC__deref_out  void **ppvObject);
        
        ULONG ( STDMETHODCALLTYPE *AddRef )( 
            IMessengerContacts * This);
        
        ULONG ( STDMETHODCALLTYPE *Release )( 
            IMessengerContacts * This);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfoCount )( 
            IMessengerContacts * This,
            /* [out] */ UINT *pctinfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfo )( 
            IMessengerContacts * This,
            /* [in] */ UINT iTInfo,
            /* [in] */ LCID lcid,
            /* [out] */ ITypeInfo **ppTInfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetIDsOfNames )( 
            IMessengerContacts * This,
            /* [in] */ REFIID riid,
            /* [size_is][in] */ LPOLESTR *rgszNames,
            /* [range][in] */ UINT cNames,
            /* [in] */ LCID lcid,
            /* [size_is][out] */ DISPID *rgDispId);
        
        /* [local] */ HRESULT ( STDMETHODCALLTYPE *Invoke )( 
            IMessengerContacts * This,
            /* [in] */ DISPID dispIdMember,
            /* [in] */ REFIID riid,
            /* [in] */ LCID lcid,
            /* [in] */ WORD wFlags,
            /* [out][in] */ DISPPARAMS *pDispParams,
            /* [out] */ VARIANT *pVarResult,
            /* [out] */ EXCEPINFO *pExcepInfo,
            /* [out] */ UINT *puArgErr);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Count )( 
            IMessengerContacts * This,
            /* [retval][out] */ LONG *pcContacts);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *Item )( 
            IMessengerContacts * This,
            /* [in] */ LONG Index,
            /* [retval][out] */ IDispatch **ppMContact);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *Remove )( 
            IMessengerContacts * This,
            /* [in] */ IDispatch *pMContact);
        
        /* [helpstring][restricted][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get__NewEnum )( 
            IMessengerContacts * This,
            /* [retval][out] */ IUnknown **ppUnknown);
        
        END_INTERFACE
    } IMessengerContactsVtbl;

    interface IMessengerContacts
    {
        CONST_VTBL struct IMessengerContactsVtbl *lpVtbl;
    };

    

#ifdef COBJMACROS


#define IMessengerContacts_QueryInterface(This,riid,ppvObject)	\
    ( (This)->lpVtbl -> QueryInterface(This,riid,ppvObject) ) 

#define IMessengerContacts_AddRef(This)	\
    ( (This)->lpVtbl -> AddRef(This) ) 

#define IMessengerContacts_Release(This)	\
    ( (This)->lpVtbl -> Release(This) ) 


#define IMessengerContacts_GetTypeInfoCount(This,pctinfo)	\
    ( (This)->lpVtbl -> GetTypeInfoCount(This,pctinfo) ) 

#define IMessengerContacts_GetTypeInfo(This,iTInfo,lcid,ppTInfo)	\
    ( (This)->lpVtbl -> GetTypeInfo(This,iTInfo,lcid,ppTInfo) ) 

#define IMessengerContacts_GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId)	\
    ( (This)->lpVtbl -> GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId) ) 

#define IMessengerContacts_Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr)	\
    ( (This)->lpVtbl -> Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr) ) 


#define IMessengerContacts_get_Count(This,pcContacts)	\
    ( (This)->lpVtbl -> get_Count(This,pcContacts) ) 

#define IMessengerContacts_Item(This,Index,ppMContact)	\
    ( (This)->lpVtbl -> Item(This,Index,ppMContact) ) 

#define IMessengerContacts_Remove(This,pMContact)	\
    ( (This)->lpVtbl -> Remove(This,pMContact) ) 

#define IMessengerContacts_get__NewEnum(This,ppUnknown)	\
    ( (This)->lpVtbl -> get__NewEnum(This,ppUnknown) ) 

#endif /* COBJMACROS */


#endif 	/* C style interface */




#endif 	/* __IMessengerContacts_INTERFACE_DEFINED__ */


#ifndef __IMessengerService_INTERFACE_DEFINED__
#define __IMessengerService_INTERFACE_DEFINED__

/* interface IMessengerService */
/* [object][oleautomation][dual][helpcontext][helpstring][uuid] */ 


EXTERN_C const IID IID_IMessengerService;

#if defined(__cplusplus) && !defined(CINTERFACE)
    
    MIDL_INTERFACE("2E50547C-A8AA-4f60-B57E-1F414711007B")
    IMessengerService : public IDispatch
    {
    public:
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_ServiceName( 
            /* [retval][out] */ BSTR *pbstrServiceName) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_ServiceID( 
            /* [retval][out] */ BSTR *pbstrID) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_MyFriendlyName( 
            /* [retval][out] */ BSTR *pbstrName) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_MyStatus( 
            /* [retval][out] */ MISTATUS *pmiStatus) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_MySigninName( 
            /* [retval][out] */ BSTR *pbstrName) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_Property( 
            /* [in] */ MSERVICEPROPERTY ePropType,
            /* [retval][out] */ VARIANT *pvPropVal) = 0;
        
        virtual /* [helpcontext][helpstring][propput][id] */ HRESULT STDMETHODCALLTYPE put_Property( 
            /* [in] */ MSERVICEPROPERTY ePropType,
            /* [in] */ VARIANT vPropVal) = 0;
        
    };
    
#else 	/* C style interface */

    typedef struct IMessengerServiceVtbl
    {
        BEGIN_INTERFACE
        
        HRESULT ( STDMETHODCALLTYPE *QueryInterface )( 
            IMessengerService * This,
            /* [in] */ REFIID riid,
            /* [annotation][iid_is][out] */ 
            __RPC__deref_out  void **ppvObject);
        
        ULONG ( STDMETHODCALLTYPE *AddRef )( 
            IMessengerService * This);
        
        ULONG ( STDMETHODCALLTYPE *Release )( 
            IMessengerService * This);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfoCount )( 
            IMessengerService * This,
            /* [out] */ UINT *pctinfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfo )( 
            IMessengerService * This,
            /* [in] */ UINT iTInfo,
            /* [in] */ LCID lcid,
            /* [out] */ ITypeInfo **ppTInfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetIDsOfNames )( 
            IMessengerService * This,
            /* [in] */ REFIID riid,
            /* [size_is][in] */ LPOLESTR *rgszNames,
            /* [range][in] */ UINT cNames,
            /* [in] */ LCID lcid,
            /* [size_is][out] */ DISPID *rgDispId);
        
        /* [local] */ HRESULT ( STDMETHODCALLTYPE *Invoke )( 
            IMessengerService * This,
            /* [in] */ DISPID dispIdMember,
            /* [in] */ REFIID riid,
            /* [in] */ LCID lcid,
            /* [in] */ WORD wFlags,
            /* [out][in] */ DISPPARAMS *pDispParams,
            /* [out] */ VARIANT *pVarResult,
            /* [out] */ EXCEPINFO *pExcepInfo,
            /* [out] */ UINT *puArgErr);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_ServiceName )( 
            IMessengerService * This,
            /* [retval][out] */ BSTR *pbstrServiceName);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_ServiceID )( 
            IMessengerService * This,
            /* [retval][out] */ BSTR *pbstrID);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyFriendlyName )( 
            IMessengerService * This,
            /* [retval][out] */ BSTR *pbstrName);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MyStatus )( 
            IMessengerService * This,
            /* [retval][out] */ MISTATUS *pmiStatus);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_MySigninName )( 
            IMessengerService * This,
            /* [retval][out] */ BSTR *pbstrName);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Property )( 
            IMessengerService * This,
            /* [in] */ MSERVICEPROPERTY ePropType,
            /* [retval][out] */ VARIANT *pvPropVal);
        
        /* [helpcontext][helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_Property )( 
            IMessengerService * This,
            /* [in] */ MSERVICEPROPERTY ePropType,
            /* [in] */ VARIANT vPropVal);
        
        END_INTERFACE
    } IMessengerServiceVtbl;

    interface IMessengerService
    {
        CONST_VTBL struct IMessengerServiceVtbl *lpVtbl;
    };

    

#ifdef COBJMACROS


#define IMessengerService_QueryInterface(This,riid,ppvObject)	\
    ( (This)->lpVtbl -> QueryInterface(This,riid,ppvObject) ) 

#define IMessengerService_AddRef(This)	\
    ( (This)->lpVtbl -> AddRef(This) ) 

#define IMessengerService_Release(This)	\
    ( (This)->lpVtbl -> Release(This) ) 


#define IMessengerService_GetTypeInfoCount(This,pctinfo)	\
    ( (This)->lpVtbl -> GetTypeInfoCount(This,pctinfo) ) 

#define IMessengerService_GetTypeInfo(This,iTInfo,lcid,ppTInfo)	\
    ( (This)->lpVtbl -> GetTypeInfo(This,iTInfo,lcid,ppTInfo) ) 

#define IMessengerService_GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId)	\
    ( (This)->lpVtbl -> GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId) ) 

#define IMessengerService_Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr)	\
    ( (This)->lpVtbl -> Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr) ) 


#define IMessengerService_get_ServiceName(This,pbstrServiceName)	\
    ( (This)->lpVtbl -> get_ServiceName(This,pbstrServiceName) ) 

#define IMessengerService_get_ServiceID(This,pbstrID)	\
    ( (This)->lpVtbl -> get_ServiceID(This,pbstrID) ) 

#define IMessengerService_get_MyFriendlyName(This,pbstrName)	\
    ( (This)->lpVtbl -> get_MyFriendlyName(This,pbstrName) ) 

#define IMessengerService_get_MyStatus(This,pmiStatus)	\
    ( (This)->lpVtbl -> get_MyStatus(This,pmiStatus) ) 

#define IMessengerService_get_MySigninName(This,pbstrName)	\
    ( (This)->lpVtbl -> get_MySigninName(This,pbstrName) ) 

#define IMessengerService_get_Property(This,ePropType,pvPropVal)	\
    ( (This)->lpVtbl -> get_Property(This,ePropType,pvPropVal) ) 

#define IMessengerService_put_Property(This,ePropType,vPropVal)	\
    ( (This)->lpVtbl -> put_Property(This,ePropType,vPropVal) ) 

#endif /* COBJMACROS */


#endif 	/* C style interface */




#endif 	/* __IMessengerService_INTERFACE_DEFINED__ */


#ifndef __IMessengerServices_INTERFACE_DEFINED__
#define __IMessengerServices_INTERFACE_DEFINED__

/* interface IMessengerServices */
/* [object][oleautomation][dual][helpcontext][helpstring][uuid] */ 


EXTERN_C const IID IID_IMessengerServices;

#if defined(__cplusplus) && !defined(CINTERFACE)
    
    MIDL_INTERFACE("2E50547B-A8AA-4f60-B57E-1F414711007B")
    IMessengerServices : public IDispatch
    {
    public:
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_PrimaryService( 
            /* [retval][out] */ IDispatch **ppService) = 0;
        
        virtual /* [helpcontext][helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_Count( 
            /* [retval][out] */ long *pcServices) = 0;
        
        virtual /* [helpcontext][helpstring][id] */ HRESULT STDMETHODCALLTYPE Item( 
            /* [in] */ long Index,
            /* [retval][out] */ IDispatch **ppService) = 0;
        
        virtual /* [helpcontext][helpstring][restricted][propget][id] */ HRESULT STDMETHODCALLTYPE get__NewEnum( 
            /* [retval][out] */ IUnknown **ppUnknown) = 0;
        
    };
    
#else 	/* C style interface */

    typedef struct IMessengerServicesVtbl
    {
        BEGIN_INTERFACE
        
        HRESULT ( STDMETHODCALLTYPE *QueryInterface )( 
            IMessengerServices * This,
            /* [in] */ REFIID riid,
            /* [annotation][iid_is][out] */ 
            __RPC__deref_out  void **ppvObject);
        
        ULONG ( STDMETHODCALLTYPE *AddRef )( 
            IMessengerServices * This);
        
        ULONG ( STDMETHODCALLTYPE *Release )( 
            IMessengerServices * This);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfoCount )( 
            IMessengerServices * This,
            /* [out] */ UINT *pctinfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfo )( 
            IMessengerServices * This,
            /* [in] */ UINT iTInfo,
            /* [in] */ LCID lcid,
            /* [out] */ ITypeInfo **ppTInfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetIDsOfNames )( 
            IMessengerServices * This,
            /* [in] */ REFIID riid,
            /* [size_is][in] */ LPOLESTR *rgszNames,
            /* [range][in] */ UINT cNames,
            /* [in] */ LCID lcid,
            /* [size_is][out] */ DISPID *rgDispId);
        
        /* [local] */ HRESULT ( STDMETHODCALLTYPE *Invoke )( 
            IMessengerServices * This,
            /* [in] */ DISPID dispIdMember,
            /* [in] */ REFIID riid,
            /* [in] */ LCID lcid,
            /* [in] */ WORD wFlags,
            /* [out][in] */ DISPPARAMS *pDispParams,
            /* [out] */ VARIANT *pVarResult,
            /* [out] */ EXCEPINFO *pExcepInfo,
            /* [out] */ UINT *puArgErr);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_PrimaryService )( 
            IMessengerServices * This,
            /* [retval][out] */ IDispatch **ppService);
        
        /* [helpcontext][helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Count )( 
            IMessengerServices * This,
            /* [retval][out] */ long *pcServices);
        
        /* [helpcontext][helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *Item )( 
            IMessengerServices * This,
            /* [in] */ long Index,
            /* [retval][out] */ IDispatch **ppService);
        
        /* [helpcontext][helpstring][restricted][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get__NewEnum )( 
            IMessengerServices * This,
            /* [retval][out] */ IUnknown **ppUnknown);
        
        END_INTERFACE
    } IMessengerServicesVtbl;

    interface IMessengerServices
    {
        CONST_VTBL struct IMessengerServicesVtbl *lpVtbl;
    };

    

#ifdef COBJMACROS


#define IMessengerServices_QueryInterface(This,riid,ppvObject)	\
    ( (This)->lpVtbl -> QueryInterface(This,riid,ppvObject) ) 

#define IMessengerServices_AddRef(This)	\
    ( (This)->lpVtbl -> AddRef(This) ) 

#define IMessengerServices_Release(This)	\
    ( (This)->lpVtbl -> Release(This) ) 


#define IMessengerServices_GetTypeInfoCount(This,pctinfo)	\
    ( (This)->lpVtbl -> GetTypeInfoCount(This,pctinfo) ) 

#define IMessengerServices_GetTypeInfo(This,iTInfo,lcid,ppTInfo)	\
    ( (This)->lpVtbl -> GetTypeInfo(This,iTInfo,lcid,ppTInfo) ) 

#define IMessengerServices_GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId)	\
    ( (This)->lpVtbl -> GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId) ) 

#define IMessengerServices_Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr)	\
    ( (This)->lpVtbl -> Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr) ) 


#define IMessengerServices_get_PrimaryService(This,ppService)	\
    ( (This)->lpVtbl -> get_PrimaryService(This,ppService) ) 

#define IMessengerServices_get_Count(This,pcServices)	\
    ( (This)->lpVtbl -> get_Count(This,pcServices) ) 

#define IMessengerServices_Item(This,Index,ppService)	\
    ( (This)->lpVtbl -> Item(This,Index,ppService) ) 

#define IMessengerServices_get__NewEnum(This,ppUnknown)	\
    ( (This)->lpVtbl -> get__NewEnum(This,ppUnknown) ) 

#endif /* COBJMACROS */


#endif 	/* C style interface */




#endif 	/* __IMessengerServices_INTERFACE_DEFINED__ */


#ifndef __IMessengerGroup_INTERFACE_DEFINED__
#define __IMessengerGroup_INTERFACE_DEFINED__

/* interface IMessengerGroup */
/* [object][oleautomation][dual][helpstring][uuid] */ 


EXTERN_C const IID IID_IMessengerGroup;

#if defined(__cplusplus) && !defined(CINTERFACE)
    
    MIDL_INTERFACE("E1AF1038-B884-44cb-A535-1C3C11A3D1DB")
    IMessengerGroup : public IDispatch
    {
    public:
        virtual /* [helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_Contacts( 
            /* [retval][out] */ IDispatch **ppMContacts) = 0;
        
        virtual /* [helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_Name( 
            /* [retval][out] */ BSTR *bstrName) = 0;
        
        virtual /* [helpstring][propput][id] */ HRESULT STDMETHODCALLTYPE put_Name( 
            /* [in] */ BSTR bstrName) = 0;
        
        virtual /* [helpstring][id] */ HRESULT STDMETHODCALLTYPE AddContact( 
            /* [in] */ VARIANT vContact) = 0;
        
        virtual /* [helpstring][id] */ HRESULT STDMETHODCALLTYPE RemoveContact( 
            /* [in] */ VARIANT vContact) = 0;
        
        virtual /* [helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_Service( 
            /* [retval][out] */ IDispatch **pService) = 0;
        
    };
    
#else 	/* C style interface */

    typedef struct IMessengerGroupVtbl
    {
        BEGIN_INTERFACE
        
        HRESULT ( STDMETHODCALLTYPE *QueryInterface )( 
            IMessengerGroup * This,
            /* [in] */ REFIID riid,
            /* [annotation][iid_is][out] */ 
            __RPC__deref_out  void **ppvObject);
        
        ULONG ( STDMETHODCALLTYPE *AddRef )( 
            IMessengerGroup * This);
        
        ULONG ( STDMETHODCALLTYPE *Release )( 
            IMessengerGroup * This);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfoCount )( 
            IMessengerGroup * This,
            /* [out] */ UINT *pctinfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfo )( 
            IMessengerGroup * This,
            /* [in] */ UINT iTInfo,
            /* [in] */ LCID lcid,
            /* [out] */ ITypeInfo **ppTInfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetIDsOfNames )( 
            IMessengerGroup * This,
            /* [in] */ REFIID riid,
            /* [size_is][in] */ LPOLESTR *rgszNames,
            /* [range][in] */ UINT cNames,
            /* [in] */ LCID lcid,
            /* [size_is][out] */ DISPID *rgDispId);
        
        /* [local] */ HRESULT ( STDMETHODCALLTYPE *Invoke )( 
            IMessengerGroup * This,
            /* [in] */ DISPID dispIdMember,
            /* [in] */ REFIID riid,
            /* [in] */ LCID lcid,
            /* [in] */ WORD wFlags,
            /* [out][in] */ DISPPARAMS *pDispParams,
            /* [out] */ VARIANT *pVarResult,
            /* [out] */ EXCEPINFO *pExcepInfo,
            /* [out] */ UINT *puArgErr);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Contacts )( 
            IMessengerGroup * This,
            /* [retval][out] */ IDispatch **ppMContacts);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Name )( 
            IMessengerGroup * This,
            /* [retval][out] */ BSTR *bstrName);
        
        /* [helpstring][propput][id] */ HRESULT ( STDMETHODCALLTYPE *put_Name )( 
            IMessengerGroup * This,
            /* [in] */ BSTR bstrName);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *AddContact )( 
            IMessengerGroup * This,
            /* [in] */ VARIANT vContact);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *RemoveContact )( 
            IMessengerGroup * This,
            /* [in] */ VARIANT vContact);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Service )( 
            IMessengerGroup * This,
            /* [retval][out] */ IDispatch **pService);
        
        END_INTERFACE
    } IMessengerGroupVtbl;

    interface IMessengerGroup
    {
        CONST_VTBL struct IMessengerGroupVtbl *lpVtbl;
    };

    

#ifdef COBJMACROS


#define IMessengerGroup_QueryInterface(This,riid,ppvObject)	\
    ( (This)->lpVtbl -> QueryInterface(This,riid,ppvObject) ) 

#define IMessengerGroup_AddRef(This)	\
    ( (This)->lpVtbl -> AddRef(This) ) 

#define IMessengerGroup_Release(This)	\
    ( (This)->lpVtbl -> Release(This) ) 


#define IMessengerGroup_GetTypeInfoCount(This,pctinfo)	\
    ( (This)->lpVtbl -> GetTypeInfoCount(This,pctinfo) ) 

#define IMessengerGroup_GetTypeInfo(This,iTInfo,lcid,ppTInfo)	\
    ( (This)->lpVtbl -> GetTypeInfo(This,iTInfo,lcid,ppTInfo) ) 

#define IMessengerGroup_GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId)	\
    ( (This)->lpVtbl -> GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId) ) 

#define IMessengerGroup_Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr)	\
    ( (This)->lpVtbl -> Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr) ) 


#define IMessengerGroup_get_Contacts(This,ppMContacts)	\
    ( (This)->lpVtbl -> get_Contacts(This,ppMContacts) ) 

#define IMessengerGroup_get_Name(This,bstrName)	\
    ( (This)->lpVtbl -> get_Name(This,bstrName) ) 

#define IMessengerGroup_put_Name(This,bstrName)	\
    ( (This)->lpVtbl -> put_Name(This,bstrName) ) 

#define IMessengerGroup_AddContact(This,vContact)	\
    ( (This)->lpVtbl -> AddContact(This,vContact) ) 

#define IMessengerGroup_RemoveContact(This,vContact)	\
    ( (This)->lpVtbl -> RemoveContact(This,vContact) ) 

#define IMessengerGroup_get_Service(This,pService)	\
    ( (This)->lpVtbl -> get_Service(This,pService) ) 

#endif /* COBJMACROS */


#endif 	/* C style interface */




#endif 	/* __IMessengerGroup_INTERFACE_DEFINED__ */


#ifndef __IMessengerGroups_INTERFACE_DEFINED__
#define __IMessengerGroups_INTERFACE_DEFINED__

/* interface IMessengerGroups */
/* [object][oleautomation][dual][helpstring][uuid] */ 


EXTERN_C const IID IID_IMessengerGroups;

#if defined(__cplusplus) && !defined(CINTERFACE)
    
    MIDL_INTERFACE("E1AF1028-B884-44cb-A535-1C3C11A3D1DB")
    IMessengerGroups : public IDispatch
    {
    public:
        virtual /* [helpstring][id] */ HRESULT STDMETHODCALLTYPE Remove( 
            /* [in] */ IDispatch *pGroup) = 0;
        
        virtual /* [helpstring][propget][id] */ HRESULT STDMETHODCALLTYPE get_Count( 
            /* [retval][out] */ LONG *pcCount) = 0;
        
        virtual /* [helpstring][id] */ HRESULT STDMETHODCALLTYPE Item( 
            /* [in] */ LONG Index,
            /* [retval][out] */ IDispatch **ppMGroup) = 0;
        
        virtual /* [helpstring][restricted][propget][id] */ HRESULT STDMETHODCALLTYPE get__NewEnum( 
            /* [retval][out] */ IUnknown **ppUnknown) = 0;
        
    };
    
#else 	/* C style interface */

    typedef struct IMessengerGroupsVtbl
    {
        BEGIN_INTERFACE
        
        HRESULT ( STDMETHODCALLTYPE *QueryInterface )( 
            IMessengerGroups * This,
            /* [in] */ REFIID riid,
            /* [annotation][iid_is][out] */ 
            __RPC__deref_out  void **ppvObject);
        
        ULONG ( STDMETHODCALLTYPE *AddRef )( 
            IMessengerGroups * This);
        
        ULONG ( STDMETHODCALLTYPE *Release )( 
            IMessengerGroups * This);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfoCount )( 
            IMessengerGroups * This,
            /* [out] */ UINT *pctinfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetTypeInfo )( 
            IMessengerGroups * This,
            /* [in] */ UINT iTInfo,
            /* [in] */ LCID lcid,
            /* [out] */ ITypeInfo **ppTInfo);
        
        HRESULT ( STDMETHODCALLTYPE *GetIDsOfNames )( 
            IMessengerGroups * This,
            /* [in] */ REFIID riid,
            /* [size_is][in] */ LPOLESTR *rgszNames,
            /* [range][in] */ UINT cNames,
            /* [in] */ LCID lcid,
            /* [size_is][out] */ DISPID *rgDispId);
        
        /* [local] */ HRESULT ( STDMETHODCALLTYPE *Invoke )( 
            IMessengerGroups * This,
            /* [in] */ DISPID dispIdMember,
            /* [in] */ REFIID riid,
            /* [in] */ LCID lcid,
            /* [in] */ WORD wFlags,
            /* [out][in] */ DISPPARAMS *pDispParams,
            /* [out] */ VARIANT *pVarResult,
            /* [out] */ EXCEPINFO *pExcepInfo,
            /* [out] */ UINT *puArgErr);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *Remove )( 
            IMessengerGroups * This,
            /* [in] */ IDispatch *pGroup);
        
        /* [helpstring][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get_Count )( 
            IMessengerGroups * This,
            /* [retval][out] */ LONG *pcCount);
        
        /* [helpstring][id] */ HRESULT ( STDMETHODCALLTYPE *Item )( 
            IMessengerGroups * This,
            /* [in] */ LONG Index,
            /* [retval][out] */ IDispatch **ppMGroup);
        
        /* [helpstring][restricted][propget][id] */ HRESULT ( STDMETHODCALLTYPE *get__NewEnum )( 
            IMessengerGroups * This,
            /* [retval][out] */ IUnknown **ppUnknown);
        
        END_INTERFACE
    } IMessengerGroupsVtbl;

    interface IMessengerGroups
    {
        CONST_VTBL struct IMessengerGroupsVtbl *lpVtbl;
    };

    

#ifdef COBJMACROS


#define IMessengerGroups_QueryInterface(This,riid,ppvObject)	\
    ( (This)->lpVtbl -> QueryInterface(This,riid,ppvObject) ) 

#define IMessengerGroups_AddRef(This)	\
    ( (This)->lpVtbl -> AddRef(This) ) 

#define IMessengerGroups_Release(This)	\
    ( (This)->lpVtbl -> Release(This) ) 


#define IMessengerGroups_GetTypeInfoCount(This,pctinfo)	\
    ( (This)->lpVtbl -> GetTypeInfoCount(This,pctinfo) ) 

#define IMessengerGroups_GetTypeInfo(This,iTInfo,lcid,ppTInfo)	\
    ( (This)->lpVtbl -> GetTypeInfo(This,iTInfo,lcid,ppTInfo) ) 

#define IMessengerGroups_GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId)	\
    ( (This)->lpVtbl -> GetIDsOfNames(This,riid,rgszNames,cNames,lcid,rgDispId) ) 

#define IMessengerGroups_Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr)	\
    ( (This)->lpVtbl -> Invoke(This,dispIdMember,riid,lcid,wFlags,pDispParams,pVarResult,pExcepInfo,puArgErr) ) 


#define IMessengerGroups_Remove(This,pGroup)	\
    ( (This)->lpVtbl -> Remove(This,pGroup) ) 

#define IMessengerGroups_get_Count(This,pcCount)	\
    ( (This)->lpVtbl -> get_Count(This,pcCount) ) 

#define IMessengerGroups_Item(This,Index,ppMGroup)	\
    ( (This)->lpVtbl -> Item(This,Index,ppMGroup) ) 

#define IMessengerGroups_get__NewEnum(This,ppUnknown)	\
    ( (This)->lpVtbl -> get__NewEnum(This,ppUnknown) ) 

#endif /* COBJMACROS */


#endif 	/* C style interface */




#endif 	/* __IMessengerGroups_INTERFACE_DEFINED__ */


EXTERN_C const CLSID CLSID_Messenger;

#ifdef __cplusplus

class DECLSPEC_UUID("8885370D-B33E-44b7-875D-28E403CF9270")
Messenger;
#endif
#endif /* __CommunicatorAPI_LIBRARY_DEFINED__ */

/* Additional Prototypes for ALL interfaces */

/* end of Additional Prototypes */

#ifdef __cplusplus
}
#endif

#endif


