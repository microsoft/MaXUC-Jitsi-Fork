/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#ifndef _JMSOFFICECOMM_UNKNOWNIMPL_H_
#define _JMSOFFICECOMM_UNKNOWNIMPL_H_

#include "Log.h"
#include "OutOfProcessServer.h"
#include "StringUtils.h"
#include "WeakReferenceSource.h"

#ifdef _MSC_VER
#define FUNC_NAME_MACRO __FUNCSIG__
#else
#define FUNC_NAME_MACRO __PRETTY_FUNCTION__
#endif

#define STDMETHODIMP_E_NOTIMPL_STUB \
    { \
        Log::d(_T("%s\n"), FUNC_NAME_MACRO); \
        return E_NOTIMPL; \
    }

/**
 * Represents a base implementation of the <tt>IUnknown</tt> interface.
 *
 * @author Lyubomir Marinov
 */
template <class T, REFIID IID_T>
class UnknownImpl
    : public T
{
public:
    // IUnknown
    STDMETHODIMP QueryInterface(REFIID iid, PVOID *obj)
        {
            HRESULT hr;

            if (!obj)
                hr = E_POINTER;
            else if (IID_IUnknown == iid)
            {
                AddRef();
                *obj = static_cast<LPUNKNOWN>(this);
                hr = S_OK;
            }
            else if (IID_T == iid)
            {
                AddRef();
                *obj = static_cast<T *>(this);
                hr = S_OK;
            }
            else if (IID_IWeakReferenceSource == iid)
            {
                if (!_weakReferenceSource)
                    _weakReferenceSource = new WeakReferenceSource(this);
                _weakReferenceSource->AddRef();
                *obj = static_cast<IWeakReferenceSource *>(_weakReferenceSource);
                hr = S_OK;
            }
            else
            {
                *obj = NULL;
                hr = E_NOINTERFACE;
            }

            // This log is quite spammy in real use so commented out.  However
            // it may be useful for debugging during dev so not removing
            // completely.

            /*if (FAILED(hr))
            {
                LPOLESTR olestr;

                if (SUCCEEDED(::StringFromIID(iid, &olestr)))
                {
                    LPTSTR tstr
#ifdef _UNICODE
                        = olestr;
#else
                        = StringUtils::WideCharToMultiByte(olestr);
#endif // #ifdef _UNICODE

                    if (tstr)
                    {
                        Log::d(
                                _T("UnknownImpl::QueryInterface: this=%p; iid=%s;\n"),
                                (PVOID) this,
                                tstr);
                        if (tstr != (LPTSTR)olestr)
                            ::free(tstr);
                    }
                    ::CoTaskMemFree(olestr);
                }
            }*/

            return hr;
        }

    STDMETHODIMP_(ULONG) AddRef() { return ++_refCount; }

    STDMETHODIMP_(ULONG) Release()
        {
            ULONG refCount = --_refCount;

            if (!refCount)
                delete this;
            // This log is quite spammy in real use so commented out.  However
            // it may be useful for debugging during dev so not removing
            // completely.
            /*Log::d(
                    _T("UnknownImpl::Release: this=%p; refCount=%lu;\n"),
                    (PVOID) this,
                    refCount);*/
            return refCount;
        }

protected:
    UnknownImpl()
            : _refCount(1),
              _weakReferenceSource(NULL)
        {
            OutOfProcessServer::addRef();
        }

    virtual ~UnknownImpl()
        {
            if (_weakReferenceSource)
                delete _weakReferenceSource;

            OutOfProcessServer::release();
        }

private:
    ULONG _refCount;
    WeakReferenceSource *_weakReferenceSource;
};

#endif /* #ifndef _JMSOFFICECOMM_UNKNOWNIMPL_H_ */
