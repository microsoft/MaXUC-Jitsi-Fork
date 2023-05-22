/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
#include "MessengerClassFactory.h"

#include "Messenger.h"
#include "Log.h"

EXTERN_C const GUID DECLSPEC_SELECTANY CLSID_Messenger
    = { 0x8885370D, 0xB33E, 0x44b7, { 0x87, 0x5D, 0x28, 0xE4, 0x03, 0xCF, 0x92, 0x70 } };

STDMETHODIMP
MessengerClassFactory::CreateInstance(LPUNKNOWN outer, REFIID iid, PVOID *obj)
{
    HRESULT hr;

    Log::d(_T("MessengerClassFactory::CreateInstance\n"));

    if (outer)
    {
        *obj = NULL;
        hr = CLASS_E_NOAGGREGATION;
    }
    else
    {
        IMessenger *messenger;

        if (_messenger)
        {
            Log::d(_T("_messenger"));
            hr = _messenger->Resolve(IID_IMessenger, (PVOID *) &messenger);
            if (FAILED(hr) && (E_NOINTERFACE != hr))
            {
                Log::d(_T("failed (1): %lx\n"), hr);
                _messenger->Release();
                _messenger = NULL;
            }
        }
        else
            messenger = NULL;

        if (!messenger)
        {
            Log::d(_T("New messenger\n"));
            messenger = new Messenger();

            IWeakReferenceSource *weakReferenceSource;

            hr
                = messenger->QueryInterface(
                        IID_IWeakReferenceSource,
                        (PVOID *) &weakReferenceSource);
            if (SUCCEEDED(hr))
            {
                Log::d(_T("Got new messenger\n"));
                IWeakReference *weakReference;

                hr = weakReferenceSource->GetWeakReference(&weakReference);
                if (SUCCEEDED(hr))
                {
                    if (_messenger)
                        _messenger->Release();
                    _messenger = weakReference;
                }
            }
        }
        Log::d(_T("Query and release\n"));
        hr = messenger->QueryInterface(iid, obj);
        messenger->Release();
    }
    Log::d(_T("Exit: %lx\n"), hr);
    return hr;
}
