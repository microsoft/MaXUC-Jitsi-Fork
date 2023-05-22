/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.

#include "MsOutlookAddrBookContactSourceService.h"

#include "MAPINotification.h"
#include "MAPISession.h"
#include "../MAPIBitness.h"
#include "../Logger.h"
#include <Tchar.h>
#include "../ProductName.h"

typedef BOOL (STDAPICALLTYPE *LPFBINFROMHEX)(LPSTR, LPBYTE);
typedef void (STDAPICALLTYPE *LPFREEPROWS)(LPSRowSet);
typedef void (STDAPICALLTYPE *LPHEXFROMBIN)(LPBYTE, int, LPSTR);
typedef HRESULT (STDAPICALLTYPE *LPHRALLOCADVISESINK)(LPNOTIFCALLBACK, LPVOID, LPMAPIADVISESINK FAR *);
typedef HRESULT (STDAPICALLTYPE *LPHRQUERYALLROWS)(LPMAPITABLE, LPSPropTagArray,
LPSRestriction, LPSSortOrderSet, LONG, LPSRowSet FAR *);

static LPFBINFROMHEX MsOutlookAddrBookContactSourceService_fBinFromHex;
static LPFREEPROWS MsOutlookAddrBookContactSourceService_freeProws;
static LPHEXFROMBIN MsOutlookAddrBookContactSourceService_hexFromBin;
static LPHRALLOCADVISESINK MsOutlookAddrBookContactSourceService_hrAllocAdviseSink;
static LPHRQUERYALLROWS MsOutlookAddrBookContactSourceService_hrQueryAllRows;
static LPMAPIALLOCATEBUFFER
    MsOutlookAddrBookContactSourceService_mapiAllocateBuffer;
static LPMAPIFREEBUFFER MsOutlookAddrBookContactSourceService_mapiFreeBuffer;
static LPMAPIINITIALIZE MsOutlookAddrBookContactSourceService_mapiInitialize;
static LPMAPILOGONEX MsOutlookAddrBookContactSourceService_mapiLogonEx;
static LPMAPIUNINITIALIZE
    MsOutlookAddrBookContactSourceService_mapiUninitialize;
static HMODULE MsOutlookAddrBookContactSourceService_hMapiLib = NULL;

static jboolean
MsOutlookAddrBookContactSourceService_isValidDefaultMailClient
    (LPCTSTR name, DWORD nameLength);

HRESULT MsOutlookAddrBookContactSourceService_MAPIInitialize
    (jlong version, jlong flags)
{
    HKEY regKey;
    HRESULT hResult = MAPI_E_NO_SUPPORT;

    /*
     * In the absence of a default e-mail program, MAPIInitialize may show a
     * dialog to notify of the fact. The dialog is undesirable here. Because we
     * implement ContactSourceService for Microsoft Outlook, we will try to
     * mitigate the problem by implementing an ad-hoc check whether Microsoft
     * Outlook is installed.
     */

    LONG regResult = RegOpenKeyEx(
                    HKEY_LOCAL_MACHINE,
                    _T("Software\\Microsoft\\Office"),
                    0,
                    KEY_ENUMERATE_SUB_KEYS,
                    &regKey);
    if (ERROR_SUCCESS == regResult)
    {
        LOG_DEBUG("Opened HKLM/Software/Microsoft/Office");

        DWORD i = 0;
        TCHAR installRootKeyName[
                255 // The size limit of key name as documented in MSDN
                    + 20 // \Outlook\InstallRoot
                    + 1]; // The terminating null character

        while (1)
        {
            LONG regEnumKeyEx;
            DWORD subkeyNameLength = 255 + 1;
            LPTSTR str;
            HKEY installRootKey;
            DWORD pathValueType;
            DWORD pathValueSize;

            regEnumKeyEx = RegEnumKeyEx(
                        regKey,
                        i,
                        installRootKeyName,
                        &subkeyNameLength,
                        NULL,
                        NULL,
                        NULL,
                        NULL);
            if (ERROR_NO_MORE_ITEMS == regEnumKeyEx)
            {
                LOG_WARN("No more items");
                break;
            }

            i++;
            if (ERROR_SUCCESS != regEnumKeyEx)
            {
                LOG_ERROR("Failed with error: 0x%lx", regEnumKeyEx);
                continue;
            }

            str = installRootKeyName + subkeyNameLength;
            memcpy(str, _T("\\Outlook\\InstallRoot"), 20 * sizeof(TCHAR));
            *(str + 20) = 0;

            LONG installRegResult = RegOpenKeyEx(
                            regKey,
                            installRootKeyName,
                            0,
                            KEY_QUERY_VALUE,
                            &installRootKey);

            if (ERROR_SUCCESS == installRegResult)
            {
                LOG_DEBUG("Opened %s", installRootKeyName);

                LONG pathRegResult = RegQueryValueEx(
                                installRootKey,
                                _T("Path"),
                                NULL,
                                &pathValueType,
                                NULL,
                                &pathValueSize);

                if ((ERROR_SUCCESS == pathRegResult)
                    && (REG_SZ == pathValueType)
                    && pathValueSize)
                {
                    LOG_DEBUG("Opened Path for %s", installRootKeyName);

                    LPTSTR pathValue;

                    // MSDN says "the string may not have been stored with the
                    // proper terminating null characters."
                    pathValueSize
                        += sizeof(TCHAR)
                            * (12 // \Outlook.exe
                                + 1); // The terminating null character

                    if (pathValueSize <= sizeof(installRootKeyName))
                        pathValue = installRootKeyName;
                    else
                    {
                        pathValue = (LPTSTR)::malloc(pathValueSize);
                        if (!pathValue)
                            continue;
                    }

                    LONG pathValueEx = RegQueryValueEx(
                                    installRootKey,
                                    _T("Path"),
                                    NULL,
                                    NULL,
                                    (LPBYTE) pathValue, &pathValueSize);

                    if (ERROR_SUCCESS == pathValueEx)
                    {
                        DWORD pathValueLength = pathValueSize / sizeof(TCHAR);

                        if (pathValueLength)
                        {
                            DWORD fileAttributes;

                            str = pathValue + (pathValueLength - 1);
                            if (*str)
                                str++;
                            memcpy(str, _T("\\Outlook.exe"), 12 * sizeof(TCHAR));
                            *(str + 12) = 0;

                            fileAttributes = GetFileAttributes(pathValue);
                            if (INVALID_FILE_ATTRIBUTES == fileAttributes)
                            {
                                LOG_ERROR("Path %s has invalid file attributes",
                                  pathRegResult);
                            }
                            else
                            {
                                LOG_DEBUG("Found path: %s", pathValue);
                                hResult = S_OK;
                            }
                        }
                        else
                        {
                          LOG_ERROR("Failed to read length for Path for %s: %x",
                                   pathRegResult);
                        }
                    }
                    else
                    {
                      LOG_ERROR("Failed to read Path for %s: %x",
                               pathRegResult, pathValueEx);
                    }

                    if (pathValue != installRootKeyName)
                        free(pathValue);
                }
                else
                {
                    LOG_ERROR("Failed to open path: %x, %x, %x",
                               pathRegResult, pathValueType, pathValueSize);
                }
                RegCloseKey(installRootKey);
            }
            else if(ERROR_FILE_NOT_FOUND == installRegResult)
            {
                LOG_DEBUG("%s not found",
                           installRootKeyName);
            }
            else
            {
                LOG_ERROR("Failed to open %s: %x",
                           installRootKeyName, installRegResult);
            }
        }
        RegCloseKey(regKey);

        // Make sure that Microsoft Outlook is the default mail client in order
        // to prevent its dialog in the case of it not being the default mail
        // client.
        if (HR_SUCCEEDED(hResult))
        {
            LOG_DEBUG("Check that Microsoft Outlook is the default mail client");

            DWORD defaultValueType;
            // The buffer installRootKeyName is long enough to receive
            // "Microsoft Outlook" so use it in order to not have to allocate
            // more memory.
            LPTSTR defaultValue = (LPTSTR) installRootKeyName;
            DWORD defaultValueCapacity = sizeof(installRootKeyName);
            jboolean checkHKeyLocalMachine;

            hResult = MAPI_E_NO_SUPPORT;

            LONG hkcuRegResult = RegOpenKeyEx(
                            HKEY_CURRENT_USER,
                            _T("Software\\Clients\\Mail"),
                            0,
                            KEY_QUERY_VALUE,
                            &regKey);
            if (ERROR_SUCCESS == hkcuRegResult)
            {
                DWORD defaultValueSize = defaultValueCapacity;
                LONG regQueryValueEx = RegQueryValueEx(
                        regKey,
                        NULL,
                        NULL,
                        &defaultValueType,
                        (LPBYTE) defaultValue,
                        &defaultValueSize);

                LOG_DEBUG("HKCU/Software/Clients/Mail result: %x", regQueryValueEx);

                switch (regQueryValueEx)
                {
                case ERROR_SUCCESS:
                {
                    if (REG_SZ == defaultValueType)
                    {
                        DWORD defaultValueLength
                            = defaultValueSize / sizeof(TCHAR);

                        if (JNI_TRUE
                                == MsOutlookAddrBookContactSourceService_isValidDefaultMailClient(
                                        defaultValue,
                                        defaultValueLength))
                        {
                            checkHKeyLocalMachine = JNI_FALSE;
                            if (_tcsnicmp(
                                        _T("Microsoft Outlook"), defaultValue,
                                        defaultValueLength)
                                    == 0)
                            {
                                hResult = S_OK;
                                LOG_DEBUG("Valid default mail client in HKCU: %s", defaultValue);
                            }
                            else
                            {
                                LOG_ERROR("Invalid default mail client in HKCU: %s %x", defaultValue, defaultValueLength);
                            }
                        }
                        else
                        {
                            LOG_WARN("Invalid default mail client in HKCU: %s %x", defaultValue, defaultValueLength);
                            checkHKeyLocalMachine = JNI_TRUE;
                        }
                    }
                    else
                    {
                        LOG_ERROR("Invalid default value type: %lx", defaultValueType);
                        checkHKeyLocalMachine = JNI_FALSE;
                    }
                    break;
                }
                case ERROR_FILE_NOT_FOUND:
                    checkHKeyLocalMachine = JNI_TRUE;
                    break;
                case ERROR_MORE_DATA:
                    checkHKeyLocalMachine = JNI_FALSE;
                    break;
                default:
                    checkHKeyLocalMachine = JNI_FALSE;
                    break;
                }
                RegCloseKey(regKey);
            }
            else
            {
                LOG_DEBUG("Failed to check HKCU/Software/Clients/Mail: %lx", hkcuRegResult);
                checkHKeyLocalMachine = JNI_TRUE;
            }

            if (JNI_TRUE == checkHKeyLocalMachine)
            {
                LONG hklmRegResult = RegOpenKeyEx(
                                    HKEY_LOCAL_MACHINE,
                                    _T("Software\\Clients\\Mail"),
                                    0,
                                    KEY_QUERY_VALUE,
                                    &regKey);

                if (ERROR_SUCCESS == hklmRegResult)
                {
                    DWORD defaultValueSize = defaultValueCapacity;
                    LONG regQueryValueEx
                        = RegQueryValueEx(
                                regKey,
                                NULL,
                                NULL,
                                &defaultValueType,
                                (LPBYTE) defaultValue, &defaultValueSize);

                    if ((ERROR_SUCCESS == regQueryValueEx)
                            && (REG_SZ == defaultValueType))
                    {
                        DWORD defaultValueLength = defaultValueSize / sizeof(TCHAR);

                        if ((_tcsnicmp(
                                        _T("Microsoft Outlook"), defaultValue,
                                        defaultValueLength)
                                    == 0)
                                && (JNI_TRUE
                                        == MsOutlookAddrBookContactSourceService_isValidDefaultMailClient(_T("Microsoft Outlook"), 17)))
                        {
                            hResult = S_OK;
                        }
                        else
                        {
                            LOG_ERROR("Invalid default mail client in HKLM: %s %x", defaultValue, defaultValueLength);
                        }
                    }
                    else
                    {
                        LOG_ERROR("Failed to query HKLM/Software/Clients/Mail: %lx %lx",
                                    regQueryValueEx, defaultValueType);
                    }
                    RegCloseKey(regKey);
                }
                else
                {
                    LOG_ERROR("Failed to check HKLM/Software/Clients/Mail: %lx", hklmRegResult);
                }
            }
        }
        else
        {
            LOG_ERROR("Error finding Microsoft Outlook: %x", hResult);
        }
    }
    else
    {
      LOG_ERROR("Failed to open HLKM/Software/Microsoft/Office: %x", regResult);
    }

    // If we've determined that we'd like to go on with MAPI, try to load it.
    if (HR_SUCCEEDED(hResult))
    {
        LOG_DEBUG("Attempting to load MAPI");

        LPCSTR mapiLibraryLocation = getMapiLibraryLocation();

        MsOutlookAddrBookContactSourceService_hMapiLib = ::LoadLibrary(mapiLibraryLocation);

        hResult = MAPI_E_NO_SUPPORT;
        if(MsOutlookAddrBookContactSourceService_hMapiLib)
        {
            LOG_DEBUG("Loaded MAPI dll");

            // get and check function pointers
            MsOutlookAddrBookContactSourceService_mapiInitialize
                = (LPMAPIINITIALIZE) GetProcAddress(
                        MsOutlookAddrBookContactSourceService_hMapiLib,
                        "MAPIInitialize");
            MsOutlookAddrBookContactSourceService_mapiUninitialize
                = (LPMAPIUNINITIALIZE) GetProcAddress(
                            MsOutlookAddrBookContactSourceService_hMapiLib,
                            "MAPIUninitialize");
            MsOutlookAddrBookContactSourceService_mapiAllocateBuffer
                = (LPMAPIALLOCATEBUFFER) GetProcAddress(
                        MsOutlookAddrBookContactSourceService_hMapiLib,
                        "MAPIAllocateBuffer");
            MsOutlookAddrBookContactSourceService_mapiFreeBuffer
                = (LPMAPIFREEBUFFER) GetProcAddress(
                        MsOutlookAddrBookContactSourceService_hMapiLib,
                        "MAPIFreeBuffer");
            MsOutlookAddrBookContactSourceService_mapiLogonEx
                = (LPMAPILOGONEX) GetProcAddress(
                        MsOutlookAddrBookContactSourceService_hMapiLib,
                        "MAPILogonEx");

            // Depending on mapi32.dll version the following functions must be
            // loaded with or without "...@#".
            MsOutlookAddrBookContactSourceService_fBinFromHex
                = (LPFBINFROMHEX) GetProcAddress(
                        MsOutlookAddrBookContactSourceService_hMapiLib,
                        "FBinFromHex");
            if(MsOutlookAddrBookContactSourceService_fBinFromHex == NULL)
            {
                MsOutlookAddrBookContactSourceService_fBinFromHex
                    = (LPFBINFROMHEX) GetProcAddress(
                            MsOutlookAddrBookContactSourceService_hMapiLib,
                            "FBinFromHex@8");
            }
            MsOutlookAddrBookContactSourceService_freeProws
                = (LPFREEPROWS) GetProcAddress(
                        MsOutlookAddrBookContactSourceService_hMapiLib,
                        "FreeProws");
            if(MsOutlookAddrBookContactSourceService_freeProws == NULL)
            {
                MsOutlookAddrBookContactSourceService_freeProws
                    = (LPFREEPROWS) GetProcAddress(
                            MsOutlookAddrBookContactSourceService_hMapiLib,
                            "FreeProws@4");
            }
            MsOutlookAddrBookContactSourceService_hexFromBin
                = (LPHEXFROMBIN) GetProcAddress(
                        MsOutlookAddrBookContactSourceService_hMapiLib,
                        "HexFromBin");
            if(MsOutlookAddrBookContactSourceService_hexFromBin == NULL)
            {
                MsOutlookAddrBookContactSourceService_hexFromBin
                    = (LPHEXFROMBIN) GetProcAddress(
                            MsOutlookAddrBookContactSourceService_hMapiLib,
                            "HexFromBin@12");
            }
            MsOutlookAddrBookContactSourceService_hrAllocAdviseSink
                = (LPHRALLOCADVISESINK)
                    GetProcAddress(
                            MsOutlookAddrBookContactSourceService_hMapiLib,
                            "HrAllocAdviseSink");
            if(MsOutlookAddrBookContactSourceService_hrAllocAdviseSink == NULL)
            {
                MsOutlookAddrBookContactSourceService_hrAllocAdviseSink
                    = (LPHRALLOCADVISESINK)
                    GetProcAddress(
                            MsOutlookAddrBookContactSourceService_hMapiLib,
                            "HrAllocAdviseSink@12");
            }
            MsOutlookAddrBookContactSourceService_hrQueryAllRows
                = (LPHRQUERYALLROWS) GetProcAddress(
                            MsOutlookAddrBookContactSourceService_hMapiLib,
                            "HrQueryAllRows");
            if(MsOutlookAddrBookContactSourceService_hrQueryAllRows == NULL)
            {
                MsOutlookAddrBookContactSourceService_hrQueryAllRows
                    = (LPHRQUERYALLROWS) GetProcAddress(
                            MsOutlookAddrBookContactSourceService_hMapiLib,
                            "HrQueryAllRows@24");
            }

            LOG_DEBUG("Loaded MAPI references");

            if (MsOutlookAddrBookContactSourceService_mapiInitialize
                && MsOutlookAddrBookContactSourceService_mapiUninitialize
                && MsOutlookAddrBookContactSourceService_mapiAllocateBuffer
                && MsOutlookAddrBookContactSourceService_mapiFreeBuffer
                && MsOutlookAddrBookContactSourceService_mapiLogonEx
                && MsOutlookAddrBookContactSourceService_fBinFromHex
                && MsOutlookAddrBookContactSourceService_freeProws
                && MsOutlookAddrBookContactSourceService_hexFromBin
                && MsOutlookAddrBookContactSourceService_hrAllocAdviseSink
                && MsOutlookAddrBookContactSourceService_hrQueryAllRows)
            {
                MAPIINIT_0 mapiInit = { (ULONG) version, (ULONG) flags };

                // Opening MAPI changes the working directory. Make a backup of
                // the current directory, login to MAPI and restore it
                DWORD dwSize = ::GetCurrentDirectory(0, NULL);
                if (dwSize > 0)
                {
                    LPTSTR lpszWorkingDir
                        = (LPTSTR)::malloc(dwSize*sizeof(TCHAR));
                    DWORD dwResult
                        = ::GetCurrentDirectory(dwSize, lpszWorkingDir);
                    if (dwResult != 0)
                    {
                        LOG_DEBUG("Changed directory to: %s", lpszWorkingDir);

                        MAPISession_lock();

                        LOG_INFO("Got session lock and intialising MAPI");
                        hResult
                            = MsOutlookAddrBookContactSourceService_mapiInitialize(
                            &mapiInit);
                        LOG_INFO("MAPI initialised");

                        if(HR_SUCCEEDED(hResult))
                        {
                            LOG_DEBUG("mapiInitialize succeeded");
                            LPMAPISESSION mapiSession = MAPISession_getMapiSession();

                            if (mapiSession == NULL)
                            {
                                LOG_DEBUG("No session yet - call mapiLogonEx");
                                LPMAPISESSION mapiSession = NULL;
                                hResult = MsOutlookAddrBook_mapiLogonEx(
                                        0,
                                        NULL, NULL,
                                        MAPI_EXTENDED
                                            | MAPI_NO_MAIL
                                            | MAPI_USE_DEFAULT,
                                        &mapiSession);
                                if(HR_SUCCEEDED(hResult))
                                {
                                    LOG_DEBUG("mapiLogonEx succeeded");
                                    // Register the notification of contact changed,
                                    // created and deleted.
                                    MAPINotification_registerNotifyAllMsgStores(
                                            mapiSession);
                                    LOG_DEBUG("Successfully logged on to MAPI");
                                }
                                else
                                {
                                    LOG_ERROR("Failed to get logon to MAPI: %x",
                                               hResult);
                                }
                            }
                            else
                            {
                                LOG_ERROR("There is a currently open MAPI session: %x",
                                           mapiSession);
                            }
                        }
                        else
                        {
                            LOG_ERROR("Failed to initialize MAPI: %x",
                                       hResult);
                        }
                        ::SetCurrentDirectory(lpszWorkingDir);
                        MAPISession_unlock();
                    }
                    else
                    {
                        hResult = HRESULT_FROM_WIN32(::GetLastError());
                    }

                    ::free(lpszWorkingDir);
                }
                else
                {
                    hResult = HRESULT_FROM_WIN32(::GetLastError());
                    LOG_ERROR("Failed to get the current working directory: %x",
                               hResult);
                }
            }
            else
            {
                LOG_ERROR("Invalid MAPI function");
            }
        }
        else
        {
            LOG_ERROR("Failed to load mapi32.dll library");
        }
    }
    else
    {
        LOG_ERROR("Error loading Microsoft Outlook: %x", hResult);
    }

    if (HR_FAILED(hResult))
    {
        if(MsOutlookAddrBookContactSourceService_hMapiLib)
        {
            FreeLibrary(MsOutlookAddrBookContactSourceService_hMapiLib);
            MsOutlookAddrBookContactSourceService_hMapiLib = NULL;
        }
    }

    return hResult;
}
   

 /**
 * Determine the path of the mapi32.dll we should load.
 * Usually just loading "mapi32.dll" works fine (and we pick up a working version of that dll), but for some users
 * that .dll loads fine but then returns an error code when we call initialize.  The .dll referenced in the DllPathEx
 * value of the Software\Clients\Mail\Microsoft Outlook registry key seems to always point to a .dll that does work.
 */  
LPCSTR getMapiLibraryLocation()
{
    LOG_DEBUG("Get MAPI library location");

    LPCSTR returnPathValue = "mapi32.dll";
    HKEY regOutlookKey;
    
    if (RegOpenKeyEx(HKEY_LOCAL_MACHINE,
                     _T("Software\\Clients\\Mail\\Microsoft Outlook"),
                     0,
                     KEY_QUERY_VALUE,
                     &regOutlookKey) == ERROR_SUCCESS)
    {
        LOG_DEBUG("Opened registry key Software\\Clients\\Mail\\Microsoft Outlook");
        
        LPSTR pathValue = (LPSTR)::malloc(300);
        DWORD pathValueType;
        DWORD pathValueSize;

        if ((RegQueryValueEx(regOutlookKey,
                             _T("DllPathEx"),
                             NULL,
                             &pathValueType,
                             (LPBYTE) pathValue, 
                             &pathValueSize) == ERROR_SUCCESS) && 
            (pathValueType == REG_SZ))
        {
            LOG_DEBUG("Found path to mapi.dll: %s", pathValue);
            returnPathValue = pathValue;
        }

        // free(pathValue);
    }

    RegCloseKey(regOutlookKey);

    LOG_DEBUG("Returning %s", returnPathValue);

    return returnPathValue;
}

void MsOutlookAddrBookContactSourceService_MAPIUninitialize(void)
{
    LOG_INFO("Uninitialize MAPI");
    MAPISession_lock();

    LPMAPISESSION mapiSession = MAPISession_getMapiSession();
    if(mapiSession != NULL)
    {
        MAPINotification_unregisterNotifyAllMsgStores();
        mapiSession->Logoff(0, 0, 0);
        mapiSession->Release();
        MAPISession_setMapiSession(NULL);
    }

    if(MsOutlookAddrBookContactSourceService_hMapiLib)
    {
        MsOutlookAddrBookContactSourceService_mapiUninitialize();

        MsOutlookAddrBookContactSourceService_mapiInitialize = NULL;
        MsOutlookAddrBookContactSourceService_mapiUninitialize = NULL;
        MsOutlookAddrBookContactSourceService_mapiAllocateBuffer = NULL;
        MsOutlookAddrBookContactSourceService_mapiFreeBuffer = NULL;
        MsOutlookAddrBookContactSourceService_mapiLogonEx = NULL;
        MsOutlookAddrBookContactSourceService_fBinFromHex = NULL;
        MsOutlookAddrBookContactSourceService_freeProws = NULL;
        MsOutlookAddrBookContactSourceService_hexFromBin = NULL;
        MsOutlookAddrBookContactSourceService_hrAllocAdviseSink = NULL;
        MsOutlookAddrBookContactSourceService_hrQueryAllRows = NULL;
        ::FreeLibrary(MsOutlookAddrBookContactSourceService_hMapiLib);
        MsOutlookAddrBookContactSourceService_hMapiLib = NULL;
    }

    MAPISession_unlock();

    LOG_INFO("MAPIUnitialize succeeded");
}

/**
 * Initializes the plugin but from the COM server point of view: natif side, no
 * java available here.
 *
 * @param version The version of MAPI to load.
 * @param flags The option choosen to load the MAPI to lib.
 * @param deletedMethod A function pointer used as a callback on notification
 * from outlook when a contact has been removed.
 * @param insertedMethod A function pointer used as a callback on notification
 * from outlook when a contact has been added.
 * @param updatedMethod A function pointer used as a callback on notification
 * from outlook when a contact has been modified.
 *
 * @return  S_OK if everything was alright.
 */
HRESULT MsOutlookAddrBookContactSourceService_NativeMAPIInitialize
    (jlong version, jlong flags,
     void * deletedMethod, void * insertedMethod, void * updatedMethod)
{
    MAPINotification_registerNativeNotificationsDelegate(
            deletedMethod, insertedMethod, updatedMethod);

    return MsOutlookAddrBookContactSourceService_MAPIInitialize(
            version, flags);
}

void MsOutlookAddrBookContactSourceService_NativeMAPIUninitialize(void)
{
    LOG_DEBUG("NativeMAPIUnitialize");
    MAPINotification_unregisterNativeNotificationsDelegate();

    MsOutlookAddrBookContactSourceService_MAPIUninitialize();

    LOG_DEBUG("MAPIUnitialize succeeded");
}

static jboolean
MsOutlookAddrBookContactSourceService_isValidDefaultMailClient
    (LPCTSTR name, DWORD nameLength)
{
    jboolean validDefaultMailClient = JNI_FALSE;

    if ((0 != nameLength) && (0 != name[0]))
    {
        LPTSTR str;
        TCHAR keyName[
                22 /* Software\Clients\Mail\ */
                    + 255
                    + 1 /* The terminating null character */];
        HKEY key;

        str = keyName;
        _tcsncpy(str, _T("Software\\Clients\\Mail\\"), 22);
        str += 22;
        if (nameLength > 255)
            nameLength = 255;
        _tcsncpy(str, name, nameLength);
        *(str + nameLength) = 0;

        if (ERROR_SUCCESS
                == RegOpenKeyEx(
                        HKEY_LOCAL_MACHINE,
                        keyName,
                        0,
                        KEY_QUERY_VALUE,
                        &key))
        {
            validDefaultMailClient = JNI_TRUE;
            RegCloseKey(key);
        }
    }
    return validDefaultMailClient;
}

BOOL MsOutlookAddrBook_fBinFromHex(LPSTR lpsz, LPBYTE lpb)
{
    LOG_DEBUG("MsOutlookAddrBookContactSourceService_fBinFromHex - enter");
    BOOL result = MsOutlookAddrBookContactSourceService_fBinFromHex(lpsz, lpb);
    LOG_DEBUG("MsOutlookAddrBookContactSourceService_fBinFromHex - exit");

    return result;
}

void MsOutlookAddrBook_freeProws(LPSRowSet lpRows)
{
    LOG_DEBUG("MsOutlookAddrBook_freeProws - enter");
    MsOutlookAddrBookContactSourceService_freeProws(lpRows);
    LOG_DEBUG("MsOutlookAddrBook_freeProws - exit");
}

void MsOutlookAddrBook_hexFromBin(LPBYTE pb, int cb, LPSTR sz)
{
    LOG_DEBUG("MsOutlookAddrBookContactSourceService_hexFromBin - enter");
    MsOutlookAddrBookContactSourceService_hexFromBin(pb, cb, sz);
    LOG_DEBUG("MsOutlookAddrBookContactSourceService_hexFromBin - exit");
}

void
MsOutlookAddrBook_hrAllocAdviseSink
    (LPNOTIFCALLBACK lpfnCallback, LPVOID lpvContext, LPMAPIADVISESINK*
      lppAdviseSink)
{
    LOG_DEBUG("MsOutlookAddrBook_hrAllocAdviseSink - enter");
    MsOutlookAddrBookContactSourceService_hrAllocAdviseSink(
            lpfnCallback,
            lpvContext,
            lppAdviseSink);
    LOG_DEBUG("MsOutlookAddrBook_hrAllocAdviseSink - exit");
}

HRESULT
MsOutlookAddrBook_hrQueryAllRows
    (LPMAPITABLE lpTable, LPSPropTagArray lpPropTags,
     LPSRestriction lpRestriction, LPSSortOrderSet lpSortOrderSet,
     LONG crowsMax, LPSRowSet* lppRows)
{
    LOG_DEBUG("MsOutlookAddrBook_hrQueryAllRows - enter");
    HRESULT hr = MsOutlookAddrBookContactSourceService_hrQueryAllRows(
            lpTable,
            lpPropTags,
            lpRestriction,
            lpSortOrderSet,
            crowsMax,
            lppRows);
    LOG_DEBUG("MsOutlookAddrBook_hrQueryAllRows - exit");

    return hr;
}

SCODE
MsOutlookAddrBook_mapiAllocateBuffer(ULONG size, LPVOID FAR *buffer)
{
    LOG_DEBUG("MsOutlookAddrBook_mapiAllocateBuffer - enter");
    SCODE result = MsOutlookAddrBookContactSourceService_mapiAllocateBuffer(size, buffer);
    LOG_DEBUG("MsOutlookAddrBook_mapiAllocateBuffer - exit");

    return result;
        
}

ULONG
MsOutlookAddrBook_mapiFreeBuffer(LPVOID buffer)
{
    LOG_DEBUG("MsOutlookAddrBook_mapiFreeBuffer - enter");
    ULONG result = MsOutlookAddrBookContactSourceService_mapiFreeBuffer(buffer);
    LOG_DEBUG("MsOutlookAddrBook_mapiFreeBuffer - exit");

    return result;
}

HRESULT
MsOutlookAddrBook_mapiLogonEx
    (ULONG_PTR uiParam,
    LPTSTR profileName, LPTSTR password,
    FLAGS flags,
    LPMAPISESSION FAR *mapiSession)
{
    LOG_DEBUG("MsOutlookAddrBook_mapiLogonEx - enter");
    HRESULT hResult;

    MAPISession_lock();
    LPMAPISESSION currentMapiSession = MAPISession_getMapiSession();
    if (currentMapiSession != NULL)
        hResult = S_OK;
    else
    {
        hResult
            = MsOutlookAddrBookContactSourceService_mapiLogonEx(
                    uiParam,
                    profileName, password,
                    flags,
                    &currentMapiSession);
    }

    if (HR_SUCCEEDED(hResult))
    {
        MAPISession_setMapiSession(currentMapiSession);
        *mapiSession = currentMapiSession;
    }

    MAPISession_unlock();
    LOG_DEBUG("MsOutlookAddrBook_mapiLogonEx - exit");
    return hResult;
}
