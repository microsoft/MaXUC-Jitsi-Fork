/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
#include "OutOfProcessServer.h"

#include "Log.h"
#include "Messenger.h"
#include "MessengerClassFactory.h"
#include "MessengerContact.h"
#include "net_java_sip_communicator_plugin_msofficecomm_OutOfProcessServer.h"
#include "process.h"
#include <jni.h>

EXTERN_C const GUID DECLSPEC_SELECTANY LIBID_CommunicatorUA
    = { 0x2B317E1D, 0x50E5, 0x4f5e, { 0xA3, 0xA4, 0xFB, 0x85, 0x20, 0x6E, 0xDA, 0x48 } };

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_plugin_msofficecomm_OutOfProcessServer_start
    (JNIEnv *env, jclass clazz, jstring appName, jboolean legacyMode)
{
    Log::d(_T("OutOfProcessServer::(JNI)start\n"));
    LPSTR functionName = ::_strdup(__FUNCTION__);
    LPSTR packageName;

    if (functionName)
    {
        packageName = functionName + 5 /* Java_ */;

        size_t packageNameLength
            = ::strlen(packageName) - 24 /* OutOfProcessServer_start */;

        packageName[packageNameLength] = '\0';

        char ch;
        LPSTR str = packageName;

        while ((ch = *str))
        {
            if ('_' == ch)
                *str = '/';
            str++;
        }
    }
    else
        packageName = NULL;

    jint ret = OutOfProcessServer::start(env,
                                         clazz,
                                         appName,
                                         packageName,
                                         legacyMode);

    if (functionName)
        ::free(functionName);

    return ret;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_plugin_msofficecomm_OutOfProcessServer_stop
    (JNIEnv *env, jclass clazz)
{
    Log::d(_T("OutOfProcessServer::(JNI)stop\n"));
    return OutOfProcessServer::stop(env, clazz);
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
    Log::open();
    return OutOfProcessServer::JNI_OnLoad(vm);
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved)
{
    OutOfProcessServer::JNI_OnUnload(vm);
}

CRITICAL_SECTION OutOfProcessServer::_criticalSection;
LPTYPELIB        OutOfProcessServer::_iTypeLib;
ClassFactory *   OutOfProcessServer::_messengerClassFactory = NULL;
LPSTR            OutOfProcessServer::_packageName;
LPTSTR           OutOfProcessServer::_appName;
HANDLE           OutOfProcessServer::_threadHandle;
DWORD            OutOfProcessServer::_threadId;
JavaVM *         OutOfProcessServer::_vm = NULL;
BOOL             OutOfProcessServer::_legacyMode = FALSE;

LPSTR OutOfProcessServer::getClassName(LPCSTR className)
{
    Log::d(_T("OutOfProcessServer::getClassName\n"));
    size_t packageNameLength = _packageName ? ::strlen(_packageName) : 0;
    size_t classNameLength = ::strlen(className);
    LPSTR ret = (LPSTR) ::malloc(packageNameLength + classNameLength + 1);

    if (ret)
    {
        LPSTR str = ret;

        if (packageNameLength)
        {
            ::memcpy(str, _packageName, packageNameLength);
            str += packageNameLength;
        }
        if (classNameLength)
        {
            ::memcpy(str, className, classNameLength);
            str += classNameLength;
        }
        *str = '\0';
    }
    return ret;
}

/**
 * It would be nice if we could pass the application name to this function
 * because it's the first function called when this DLL loads.  Unfortunately
 * that's not possible so we have to wait for the start() function to be
 * called.
 */
jint OutOfProcessServer::JNI_OnLoad(JavaVM *vm)
{
    jint version;

    /**
     * At this point we do not know the name of the application so we cannot
     * check the registry to see if we are the default runnning application.
     *
     * So just start the DLL and wait for start/stop to enable us providing
     * IM presence to Outlook, or not.
     */
    Log::d(_T("OutOfProcessServer::Loading\n"));
    ::InitializeCriticalSection(&_criticalSection);
    _vm = vm;
    version = JNI_VERSION_1_4;

    return version;
}

HRESULT OutOfProcessServer::loadRegTypeLib()
{
    Log::d(_T("OutOfProcessServer::loadRegLib\n"));
    /*
     * Microsoft Office will need the Office Communicator 2007 API to be able to
     * talk to us. Make sure it is available.
     */

    LPTYPELIB iTypeLib;
    HRESULT hr = ::LoadRegTypeLib(LIBID_CommunicatorUA, 1, 0, 0, &iTypeLib);

    if (SUCCEEDED(hr))
    {
        Log::d(_T("OutOfProcessServer::loadRegLib Load reg type lib succeeded\n"));
        _iTypeLib = iTypeLib;
    }
    else
    {
        Log::d(_T("OutOfProcessServer::loadRegLib Load reg type lib failed: %lx\n"), hr);

        HMODULE module;

        _iTypeLib = NULL;

        if (::GetModuleHandleEx(
                GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS
                    | GET_MODULE_HANDLE_EX_FLAG_UNCHANGED_REFCOUNT,
                (LPCTSTR) (OutOfProcessServer::loadRegTypeLib),
                &module))
        {
            Log::d(_T("OutOfProcessServer::loadRegLib Got module handle ex\n"));
            WCHAR path[MAX_PATH + 1];
            DWORD pathCapacity = sizeof(path) / sizeof(WCHAR);
            DWORD pathLength = ::GetModuleFileNameW(module, path, pathCapacity);

            if (pathLength && (pathLength < pathCapacity))
            {
                Log::d(_T("OutOfProcessServer::loadRegLib got valid path %S\n"),
                       path);
                hr = ::LoadTypeLibEx(path, REGKIND_NONE, &iTypeLib);
                if (SUCCEEDED(hr))
                {
                    Log::d(_T("OutOfProcessServer::loadRegLib loaded type lib ex\n"));
                    HMODULE oleaut32 = ::GetModuleHandle(_T("oleaut32.dll"));

                    if (oleaut32)
                    {
                        Log::d(_T("OutOfProcessServer::loadRegLib loaded oleaut32\n"));

                        typedef HRESULT (WINAPI *RTLFU)(LPTYPELIB,LPOLESTR,LPOLESTR);
                        RTLFU registerTypeLibForUser
                            = (RTLFU)
                                ::GetProcAddress(
                                        oleaut32,
                                        "RegisterTypeLibForUser");

                        if (registerTypeLibForUser)
                        {
                            Log::d(_T("OutOfProcessServer::loadRegLib got registerTypeLibForUser\n"));
                            hr = registerTypeLibForUser(iTypeLib, path, NULL);
                            if (SUCCEEDED(hr))
                            {

                                Log::d(_T("OutOfProcessServer::loadRegLib called registerTypeLibForUser\n"));
                                /*
                                 * The whole point of what has been done till
                                 * now is securing the success of future calls
                                 * to LoadRegTypeLib. Make sure that is indeed
                                 * the case.
                                 */

                                iTypeLib->Release();

                                hr
                                    = ::LoadRegTypeLib(
                                            LIBID_CommunicatorUA,
                                            1,
                                            0,
                                            0,
                                            &iTypeLib);
                                if (SUCCEEDED(hr))
                                {
                                    Log::d(_T("OutOfProcessServer::loadRegLib loadRegTypeLib\n"));
                                    _iTypeLib = iTypeLib;
                                }
                                else
                                {
                                    Log::d(_T("OutOfProcessServer::loadRegLib loadRegTypeLib failed\n"));
                                }
                            }
                            else
                            {
                                Log::d(_T("OutOfProcessServer::loadRegLib failed registerTypeLibForUser\n"));
                            }
                        }
                        else
                        {
                            Log::d(_T("OutOfProcessServer::loadRegLib failed to get registerTypeLibForUser\n"));
                            hr = E_UNEXPECTED;
                        }
                    }
                    else
                    {
                        Log::d(_T("OutOfProcessServer::loadRegLib load oleaut32 - failed\n"));
                        hr = E_UNEXPECTED;
                    }
                    if (iTypeLib != _iTypeLib)
                        iTypeLib->Release();
                }
                else
                {
                    Log::d(_T("OutOfProcessServer::loadRegLib loaded type lib ex - failed\n"));
                }
            }
            else if (pathLength)
            {
                Log::d(_T("OutOfProcessServer::loadRegLib path length too long\n"));
            }
            else
            {
                Log::d(_T("OutOfProcessServer::loadRegLib no path length\n"));
            }
        }
        else
        {
            Log::d(_T("OutOfProcessServer::loadRegLib getModuleHandleEx failed\n"));
        }
    }

    Log::d(_T("OutOfProcessServer::loadRegLib done\n"));

    return hr;
}

DWORD
OutOfProcessServer::regCreateKeyAndSetValue
    (LPCTSTR key, LPCTSTR valueName, DWORD data)
{
    SYSTEM_INFO systemInfo;
    REGSAM alternatives86[] = { 0 };
    REGSAM alternatives64[] = { KEY_WOW64_32KEY, KEY_WOW64_64KEY };
    REGSAM *alternatives;
    size_t alternativeCount;

    ::GetNativeSystemInfo(&systemInfo);
    if (PROCESSOR_ARCHITECTURE_INTEL == systemInfo.wProcessorArchitecture)
    {
        Log::d(_T("OutOfProcessServer::regCreateKeyAndSetValue on 32\n"));
        alternatives = alternatives86;
        alternativeCount = sizeof(alternatives86) / sizeof(REGSAM);
    }
    else
    {
        Log::d(_T("OutOfProcessServer::regCreateKeyAndSetValue on 64\n"));
        alternatives = alternatives64;
        alternativeCount = sizeof(alternatives64) / sizeof(REGSAM);
    }

    DWORD lastError;

    for (size_t i = 0; i < alternativeCount; i++)
    {
        HKEY hkey;

        lastError
            = ::RegCreateKeyEx(
                    HKEY_CURRENT_USER,
                    key,
                    0,
                    NULL,
                    REG_OPTION_VOLATILE,
                    KEY_SET_VALUE | alternatives[i],
                    NULL,
                    &hkey,
                    NULL);
        if (ERROR_SUCCESS == lastError)
        {
            lastError
                = ::RegSetValueEx(
                        hkey,
                        valueName,
                        0,
                        REG_DWORD,
                        (const BYTE *) &data,
                        sizeof(data));
            ::RegCloseKey(hkey);
        }
        if (ERROR_SUCCESS != lastError)
            break;
    }
    return lastError;
}

HRESULT OutOfProcessServer::registerClassObjects()
{
    Log::d(_T("OutOfProcessServer::registerClassObjects\n"));
    ClassFactory *classObject = new MessengerClassFactory();
    Log::d(_T("OutOfProcessServer::MessengerClassFactory: %p\n"), classObject);
    HRESULT hresult = classObject->registerClassObject();

    if (SUCCEEDED(hresult))
        _messengerClassFactory = classObject;
    else
        classObject->Release();

    if (SUCCEEDED(hresult))
    {
        hresult = ::CoResumeClassObjects();
        if (FAILED(hresult))
            revokeClassObjects();
    }

    return hresult;
}

ULONG OutOfProcessServer::releaseTypeLib()
{
    // TODO UnRegisterTypeLibForUser
    return _iTypeLib->Release();
}

HRESULT OutOfProcessServer::revokeClassObjects()
{
    Log::d(_T("OutOfProcessServer::revokeClassObjects\n"));
    HRESULT ret = ::CoSuspendClassObjects();

    if (SUCCEEDED(ret))
    {
        ClassFactory *classObject = _messengerClassFactory;

        if (classObject)
        {
            _messengerClassFactory = NULL;

            HRESULT hr = classObject->revokeClassObject();

            classObject->Release();
            if (FAILED(hr))
                ret = hr;
        }
    }
    return ret;
}

unsigned __stdcall OutOfProcessServer::run(void *)
{
    Log::d(_T("OutOfProcessServer::run Opened log\n"));

    HRESULT hr = ::CoInitializeEx(NULL, COINIT_APARTMENTTHREADED);
    unsigned ret = 0;

    if (SUCCEEDED(hr))
    {
        Log::d(_T("OutOfProcessServer::run CoInitialize success\n"));
        hr = loadRegTypeLib();
        if (SUCCEEDED(hr))
        {
            Log::d(_T("OutOfProcessServer::run loadRegTypeLib success\n"));
            if (ERROR_SUCCESS == setIMProvidersCommunicatorUpAndRunning(1))
            {
                Log::d(_T("OutOfProcessServer::run setIMProvidersUpAndRunning 1 success\n"));
                MSG msg;

                /*
                 * Create the message queue of this thread before any other part
                 * of the code (e.g. the release method) has a chance to invoke
                 * PostThreadMessage.
                 */
                ::PeekMessage(&msg, NULL, 0, 0, PM_NOREMOVE);

                hr = registerClassObjects();
                if (SUCCEEDED(hr))
                {
                    Log::d(_T("OutOfProcessServer::run::registerClassObjects success\n"));
                    if (ERROR_SUCCESS
                            == setIMProvidersCommunicatorUpAndRunning(2))
                    {
                        Log::d(_T("OutOfProcessServer::run::setIMCProvidersUpAndRunning 2 success\n"));
                        HANDLE threadHandle = _threadHandle;
                        BOOL logMsgWaitForMultipleObjectsExFailed = TRUE;
                        BOOL quit = FALSE;

                        do
                        {
                            /*
                             * Enable the use of the QueueUserAPC function by
                             * entering an alertable state.
                             */
                            if ((WAIT_FAILED
                                        == ::MsgWaitForMultipleObjectsEx(
                                                1,
                                                &threadHandle,
                                                INFINITE,
                                                QS_ALLINPUT | QS_ALLPOSTMESSAGE,
                                                MWMO_ALERTABLE
                                                    | MWMO_INPUTAVAILABLE))
                                    && logMsgWaitForMultipleObjectsExFailed)
                            {
                                /*
                                 * Logging the possible failures of the calls to
                                 * MsgWaitForMultipleObjectsEx multiple times is
                                 * unlikely to be useful. Besides, the call in
                                 * question is performed inside the message loop
                                 * and the logging will be an unnecessary
                                 * performance penalty.
                                 */
                                logMsgWaitForMultipleObjectsExFailed = FALSE;
                                Log::d(
                                    _T("OutOfProcessServer::run:")
                                    _T(" MsgWaitForMultipleObjectsEx=WAIT_FAILED;")
                                    _T("\n"));
                            }
                            while (::PeekMessage(&msg, NULL, 0, 0, PM_REMOVE))
                            {
                                if (WM_QUIT == msg.message)
                                {
                                    quit = TRUE;
                                    ret = msg.wParam;
                                    break;
                                }
                                else if (msg.hwnd)
                                {
                                    ::TranslateMessage(&msg);
                                    ::DispatchMessage(&msg);
                                }
                            }
                        }
                        while (!quit);
                    }
                    else
                    {
                        Log::d(_T("OutOfProcessServer::run ")
                               _T("setIMCProvidersUpAndRunning 2 failed\n"));
                    }

                    revokeClassObjects();
                }
                else
                {
                    Log::d(_T("OutOfProcessServer::run ")
                           _T("registerClassObjects failed\n"));
                }
            }
            else
            {
                Log::d(_T("OutOfProcessServer::run ")
                       _T("setIMCProvidersUpAndRunning 1 failed\n"));
            }

            /*
             * Even if setIMProvidersCommunicatorUpAndRunning(DWORD) failed, it
             * may have successfully set some of the multiple related registry
             * keys.
             */
            setIMProvidersCommunicatorUpAndRunning(0);

            releaseTypeLib();
        }
        else
        {
            Log::d(_T("OutOfProcessServer::run loadRegTypeLib failed\n"));
        }

        ::CoUninitialize();
    }
    else
    {
        Log::d(_T("OutOfProcessServer::run CoInitialize failed\n"));
    }

    Log::d(_T("OutOfProcessServer::run Closing log\n"));
    Log::close();
    return ret;
}

DWORD OutOfProcessServer::setIMProvidersCommunicatorUpAndRunning(DWORD dw)
{
    DWORD lastError;

    /*
     * Note that for click-to-call/IM to work from Accession we have to set the
     * use the Communicator IM Provider.  However if we're not running in
     * legacyMode we must instead use the appName.
     */
    TCHAR regpath[256];
    if (_legacyMode)
    {
        Log::d(_T("OutOfProcessServer::setIMProvidersCommunicatorUpAndRunning in legacy mode\n"));
      _sntprintf(regpath, 256,  _T("Software\\IM Providers\\Communicator"));
    }
    else
    {
        Log::d(_T("OutOfProcessServer::setIMProvidersCommunicatorUpAndRunning not legacy mode\n"));
      _sntprintf(regpath, 256,  _T("Software\\IM Providers\\%s"), _appName);
    }

    if (dw)
    {
        /*
         * Testing on various machines/setups has shown that the following may
         * or may not succeed without affecting the presence integration so just
         * try them and then go on with the rest regardless of their success.
         */
        lastError = ERROR_SUCCESS;
        regCreateKeyAndSetValue(
                _T("Software\\Microsoft\\Office\\11.0\\Common\\PersonaMenu"),
                _T("RTCApplication"),
                3);
        regCreateKeyAndSetValue(
                _T("Software\\Microsoft\\Office\\12.0\\Common\\PersonaMenu"),
                _T("RTCApplication"),
                3);
        regCreateKeyAndSetValue(
                _T("Software\\Microsoft\\Office\\11.0\\Common\\PersonaMenu"),
                _T("QueryServiceForStatus"),
                2);
        regCreateKeyAndSetValue(
                _T("Software\\Microsoft\\Office\\12.0\\Common\\PersonaMenu"),
                _T("QueryServiceForStatus"),
                2);
        regCreateKeyAndSetValue(
                _T("Software\\Microsoft\\Office\\11.0\\Outlook\\IM"),
                _T("SetOnlineStatusLevel"),
                3);
        regCreateKeyAndSetValue(
                _T("Software\\Microsoft\\Office\\12.0\\Outlook\\IM"),
                _T("SetOnlineStatusLevel"),
                3);
    }
    else
        lastError = ERROR_SUCCESS;

    if (ERROR_SUCCESS == lastError)
    {
        lastError
            = regCreateKeyAndSetValue(
                    regpath,
                    _T("UpAndRunning"),
                    dw);
    }
    return lastError;
}

HRESULT OutOfProcessServer::start(JNIEnv *env,
                                  jclass clazz,
                                  jstring appName,
                                  LPCSTR packageName,
                                  jboolean legacyMode)
{
    HRESULT hr = S_OK;
    LPCSTR appNameC = env->GetStringUTFChars(appName, NULL);

    if (packageName)
        hr = ((_packageName = ::_strdup(packageName))) ? S_OK : E_OUTOFMEMORY;
    else
    {
        _packageName = NULL;
        hr = S_OK;
    }

    if (SUCCEEDED(hr))
    {
        if (legacyMode)
        {
            _legacyMode = TRUE;
        }
        else
        {
            _legacyMode = FALSE;
        }

#ifdef UNICODE
        int size = MultiByteToWideChar(CP_UTF8,
                              0,
                              appNameC,
                              -1,
                              NULL,
                              0);

        _appName = (LPTSTR)::malloc(size * sizeof(TCHAR));

        if (_appName == NULL)
        {
            hr = E_OUTOFMEMORY;
        }
        else
        {
            MultiByteToWideChar(CP_UTF8,
                                0,
                                appNameC,
                                -1,
                                _appName,
                                size);
        }
#else
        int size = strlen(appNameC);
        _appName = ::malloc(size * sizeof(TCHAR));

        if (_appName == NULL)
        {
            hr = E_OUTOFMEMORY;
        }
        else
        {
            strncpy(_appName,
                       appNameC,
                       size);
        }
#endif
        env->ReleaseStringUTFChars(appName, appNameC);
    }

    if (SUCCEEDED(hr))
    {
        Log::d(_T("OutOfProcessServer::start Starting...: %s (%d)\n"),
               _appName,
               _legacyMode);
        hr = Messenger::start(env);
        if (SUCCEEDED(hr))
        {
            Log::d(_T("OutOfProcessServer::start start the Messenger\n"));
            hr = MessengerContact::start(env);
            if (SUCCEEDED(hr))
            {
                Log::d(_T("OutOfProcessServer::start start the thread\n"));
                unsigned threadId;
                HANDLE threadHandle
                    = (HANDLE)
                        ::_beginthreadex(
                                NULL,
                                0,
                                OutOfProcessServer::run,
                                NULL,
                                CREATE_SUSPENDED,
                                &threadId);

                if (threadHandle)
                {
                    Log::d(_T("OutOfProcessServer::start reset the thread\n"));
                    enterCriticalSection();

                    _threadHandle = threadHandle;
                    _threadId = (DWORD) threadId;
                    if (((DWORD) -1) == ::ResumeThread(threadHandle))
                    {
                        DWORD lastError = ::GetLastError();

                        _threadHandle = NULL;

                        ::CloseHandle(threadHandle);
                        hr = HRESULT_FROM_WIN32(lastError);
                    }

                    leaveCriticalSection();
                }
                else
                    hr = E_UNEXPECTED;

                if (FAILED(hr))
                    MessengerContact::stop(env);
            }

            if (FAILED(hr))
                Messenger::stop(env);
        }

        if (FAILED(hr) && _packageName)
        {
            ::free(_packageName);
            _packageName = NULL;
        }
    }

    Log::d(_T("OutOfProcessServer::start Exit: %lx\n"), hr);
    return hr;
}

HRESULT OutOfProcessServer::stop(JNIEnv *env, jclass clazz)
{
    DWORD lastError;

    Log::d(_T("OutOfProcessServer::Stop Entry\n"));

    if (::PostThreadMessage(_threadId, WM_QUIT, 0, 0))
    {
        Log::d(_T("OutOfProcessServer::stop WM_QUIT sent\n"));
        do
        {
            DWORD exitCode;

            if (::GetExitCodeThread(_threadHandle, &exitCode))
            {
                if (STILL_ACTIVE == exitCode)
                {
                    if (WAIT_FAILED
                            == ::WaitForSingleObject(_threadHandle, INFINITE))
                        break;
                }
                else
                    break;
            }
            else
                break;
        }
        while (1);

        Log::d(_T("OutOfProcessServer::stop Closing\n"));
        if (::CloseHandle(_threadHandle))
            lastError = 0;
        else
            lastError = ::GetLastError();

        MessengerContact::stop(env);
        Messenger::stop(env);

        if (_appName)
        {
            ::free(_appName);
            _appName = NULL;
        }
        if (_packageName)
        {
            ::free(_packageName);
            _packageName = NULL;
        }
    }
    else
        lastError = ::GetLastError();

    Log::d(_T("OutOfProcessServer::stop Exit: %lx\n"), lastError);
    return lastError ? HRESULT_FROM_WIN32(lastError) : S_OK;
}
