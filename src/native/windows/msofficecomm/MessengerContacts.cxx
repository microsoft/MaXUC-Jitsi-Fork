/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "MessengerContacts.h"

#include "MessengerContact.h"

EXTERN_C const GUID DECLSPEC_SELECTANY IID_IMessengerContacts
    = { 0xE7479A0D, 0xBB19, 0x44a5, { 0x96, 0x8F, 0x6F, 0x41, 0xD9, 0x3E, 0xE0, 0xBC } };

class SelfMessengerContact
    : public MessengerContact
{
public:
    SelfMessengerContact(IMessenger *messenger)
        : MessengerContact(messenger, NULL) {}

    // IMessengerContact
    STDMETHODIMP get_IsSelf(VARIANT_BOOL *pBoolSelf)
        {
            HRESULT hr;

            if (pBoolSelf)
            {
                *pBoolSelf = VARIANT_TRUE;
                hr = S_OK;
            }
            else
                hr = RPC_X_NULL_REF_POINTER;
            return hr;
        }

    STDMETHODIMP get_SigninName(BSTR *pbstrSigninName)
        { return _messenger->get_MySigninName(pbstrSigninName); }

    STDMETHODIMP get_Status(MISTATUS *pMstate)
        {
            HRESULT hr;

            if (pMstate)
            {
                *pMstate = MISTATUS_ONLINE;
                hr = S_OK;
            }
            else
                hr = RPC_X_NULL_REF_POINTER;
            return hr;
        }

protected:
    virtual ~SelfMessengerContact() {}
};

MessengerContacts::MessengerContacts(IMessenger *messenger)
    : _messenger(messenger),
      _self(NULL)
{
    _messenger->AddRef();
}

MessengerContacts::~MessengerContacts()
{
    _messenger->Release();
    if (_self)
        _self->Release();
}

STDMETHODIMP MessengerContacts::get__NewEnum(IUnknown **ppUnknown)
    STDMETHODIMP_E_NOTIMPL_STUB

STDMETHODIMP MessengerContacts::get_Count(LONG *pcContacts)
{
    HRESULT hr;

    if (pcContacts)
    {
        *pcContacts = 1;
        hr = S_OK;
    }
    else
        hr = RPC_X_NULL_REF_POINTER;
    return hr;
}

STDMETHODIMP MessengerContacts::getSelf(IDispatch **ppMContact)
    STDMETHODIMP_RESOLVE_WEAKREFERENCE_OR_NEW(ppMContact,_self,SelfMessengerContact,_messenger)

STDMETHODIMP MessengerContacts::Item(LONG Index, IDispatch **ppMContact)
{
    HRESULT hr;

    if (ppMContact)
    {
        if (0 > Index)
        {
            *ppMContact = NULL;
            hr = E_INVALIDARG;
        }
        else if (0 == Index)
            hr = getSelf(ppMContact);
        else
        {
            *ppMContact = NULL;
            hr = E_FAIL;
        }
    }
    else
        hr = RPC_X_NULL_REF_POINTER;
    return hr;
}

STDMETHODIMP MessengerContacts::Remove(IDispatch *pMContact)
    STDMETHODIMP_E_NOTIMPL_STUB
