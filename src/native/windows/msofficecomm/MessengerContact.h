/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#ifndef _JMSOFFICECOMM_MESSENGERCONTACT_H_
#define _JMSOFFICECOMM_MESSENGERCONTACT_H_

#include "DispatchImpl.h"
#include <msgrua.h>

/**
 * Implements the <tt>IMessengerContact</tt> interface.
 *
 * @author Lyubomir Marinov
 */
class MessengerContact
    : public DispatchImpl<IMessengerContactAdvanced, IID_IMessengerContactAdvanced>
{
public:
    static HRESULT start(JNIEnv *env);
    static HRESULT stop(JNIEnv *env);

    MessengerContact(IMessenger *messenger, LPCOLESTR signinName);

    // IUnknown
    STDMETHODIMP QueryInterface(REFIID, PVOID *);

    // IMessengerContact
    STDMETHODIMP get_FriendlyName(BSTR *pbstrFriendlyName);
    STDMETHODIMP get_Status(MISTATUS *pMstate);
    STDMETHODIMP get_SigninName(BSTR *pbstrSigninName);
    STDMETHODIMP get_ServiceName(BSTR *pbstrServiceName);
    STDMETHODIMP get_Blocked(VARIANT_BOOL *pBoolBlock);
    STDMETHODIMP put_Blocked(VARIANT_BOOL pBoolBlock);
    STDMETHODIMP get_CanPage(VARIANT_BOOL *pBoolPage);
    STDMETHODIMP get_PhoneNumber(MPHONE_TYPE PhoneType, BSTR *bstrNumber);
    STDMETHODIMP get_IsSelf(VARIANT_BOOL *pBoolSelf);
    STDMETHODIMP get_Property(MCONTACTPROPERTY ePropType, VARIANT *pvPropVal);
    STDMETHODIMP put_Property(MCONTACTPROPERTY ePropType, VARIANT vPropVal);
    STDMETHODIMP get_ServiceId(BSTR *pbstrServiceID);

    // IMessengerContactAdvanced
    STDMETHODIMP get_IsTagged(VARIANT_BOOL *pBoolIsTagged);
    STDMETHODIMP put_IsTagged(VARIANT_BOOL pBoolIsTagged);
    STDMETHODIMP get_PresenceProperties(VARIANT *pvPresenceProperties);
    STDMETHODIMP put_PresenceProperties(VARIANT vPresenceProperties);

    static BOOL signinNameEquals(LPDISPATCH contact, BSTR signinName);

protected:
    virtual ~MessengerContact();

    IMessenger *_messenger;

private:
    static jclass    _jclass;
    static jmethodID _jctorMethodID;
    static jmethodID _jgetPhoneNumberMethodID;
    static jmethodID _jgetStatusMethodID;
    static jmethodID _jisSelfMethodID;

    HRESULT constructJobject(LPCOLESTR signinName);
    HRESULT destructJobject();

    jobject          _jobject;
    LPOLESTR         _signinName;
};

#endif /* #ifndef _JMSOFFICECOMM_MESSENGERCONTACT_H_ */
