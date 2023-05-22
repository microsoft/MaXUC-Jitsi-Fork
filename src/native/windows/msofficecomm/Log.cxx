/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
#include "Log.h"

#include <stdarg.h>
#include "StringUtils.h"

FILE *Log::_stderr = NULL;
LPTSTR Log::_filename = NULL;

#ifdef _UNICODE
int Log::d(LPCTSTR format, LPCSTR str)
{
    LPWSTR wstr = StringUtils::MultiByteToWideChar(str);
    int ret;

    if (wstr)
    {
        ret = Log::d(format, wstr);
        ::free(wstr);
    }
    else
        ret = 0;
    return ret;
}
#endif /* #ifdef _UNICODE */

int Log::d(LPCTSTR format, ...)
{
    va_list args;
    SYSTEMTIME timestamp;
    LARGE_INTEGER fileSize;
    WIN32_FILE_ATTRIBUTE_DATA fad;

    // We don't want to fill up the file system so wrap between two files.
    if ((_filename != NULL) &&
        ::GetFileAttributesEx(_filename, GetFileExInfoStandard, &fad))
    {
        fileSize.HighPart = fad.nFileSizeHigh;
        fileSize.LowPart = fad.nFileSizeLow;

        if (fileSize.QuadPart > MAX_LOG_FILE_SIZE)
        {
          TCHAR wrappedFile[MAX_PATH + 1];
          ::_tcscpy(wrappedFile, _filename);
          ::_tcscat(wrappedFile, _T(".1"));
          ::CopyFile(_filename,
                     wrappedFile,
                     FALSE);

#ifdef _MSC_VER
          _stderr = ::_tfreopen_s(_filename, _T("w+"), _stderr);
#else
          _stderr = ::_tfreopen(_filename, _T("w+"), _stderr);
#endif
          if (_stderr == NULL)
          {
            _stderr = stderr;
          }
        }
    }

    ::GetLocalTime(&timestamp);

    va_start(args, format);

    fprintf(_stderr,
            "%02d-%02d-%04d %02d:%02d:%02d.%03d ",
            timestamp.wDay,
            timestamp.wMonth,
            timestamp.wYear,
            timestamp.wHour,
            timestamp.wMinute,
            timestamp.wSecond,
            timestamp.wMilliseconds);
    int ret = ::_vftprintf(_stderr, format, args);

    ::fflush(_stderr);
    va_end(args);
    return ret;
}

/* Get the name of the application that is running this logger (e.g. MaX UC).
 *
 * Calls into the Windows API to retrieve the full path to  running application and looks at
 * the name of the folder that's in.  That should match the application name, e.g.
 * C:\Program Files (x86)\MaX UC\MaXUC.exe
 * Note we can't look at the executable name because it's ambiguous where spaces should be added to that.
 */
LPTSTR Log::getAppName()
{
    LPTSTR ret = NULL;
    TCHAR path[MAX_PATH + 1];
    DWORD pathCapacity = sizeof(path) / sizeof(TCHAR);
    DWORD pathLength = ::GetModuleFileName(NULL, path, pathCapacity);

    if (pathLength && (pathLength < pathCapacity))
    {
        LPTSTR appName = NULL;

        // The retrieved name is full directory path and application name.
        // Spin through from the end until we find a directory separator, and null terminate there.
        for (LPTSTR str = path + (pathLength - 1); str != path; str--)
        {
            TCHAR ch = *str;

            if ((ch == '\\') || (ch == '/'))
            {
                // Null terminate at the final separator and go back from there to the next separator.
                *str = '\0';

                for (LPTSTR str2 = str; str2 != path; str2--)
                {
                    ch = *str2;

                    if ((ch == '\\') || (ch == '/'))
                    {
                        appName = str2 + 1;
                        break;
                    }
                }

                break;
            }
        }

        if (appName && (*appName != '\0'))
        {
            ret = ::_tcsdup(appName);
        }
    }
    return ret;
}

/*
 * Get the name of the module, i.e. the library running this code.  Should be jmsofficecomm.dll, and we strip off the .dll.
 */
LPTSTR Log::getModuleFileName()
{
    HMODULE module;
    LPTSTR ret = NULL;

    if (::GetModuleHandleEx(
            GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS
                | GET_MODULE_HANDLE_EX_FLAG_UNCHANGED_REFCOUNT,
            (LPCTSTR) (Log::getModuleFileName),
            &module))
    {
        TCHAR path[MAX_PATH + 1];
        DWORD pathCapacity = sizeof(path) / sizeof(TCHAR);
        DWORD pathLength = ::GetModuleFileName(module, path, pathCapacity);

        // pathLength is the full path to/including the library name.  We just want the library name, without the file extension.
        if (pathLength && (pathLength < pathCapacity))
        {
            LPTSTR fileName = NULL;

            for (LPTSTR str = path + (pathLength - 1); str != path; str--)
            {
                TCHAR ch = *str;

                if ((ch == '\\') || (ch == '/'))
                {
                    fileName = str + 1;
                    break;
                }
                else if (ch == '.')
                {
                    // Null terminate at the . to remove the file extenion.
                    *str = '\0';
                }
            }
            if (fileName && (*fileName != '\0'))
                ret = ::_tcsdup(fileName);
        }
    }
    return ret;
}

FILE *Log::open()
{
    LPCTSTR envVarName = _T("APPDATA");
    DWORD envVarValueLength1 = ::GetEnvironmentVariable(envVarName, NULL, 0);
    FILE *_stderr = NULL;

    if (envVarValueLength1)
    {
        LPTSTR moduleFileName = getModuleFileName();
        LPTSTR appFileName = getAppName();
        
        if (moduleFileName && appFileName)
        {
            size_t appFileNameLength = ::_tcslen(appFileName);
            size_t appFileNameSize = sizeof(TCHAR) * appFileNameLength;
            LPCTSTR separator = _T("\\");
            size_t separatorLength = ::_tcslen(separator);
            size_t separatorSize = sizeof(TCHAR) * separatorLength;
            LPCTSTR logFolder = _T("\\log\\");
            size_t logFolderLength = ::_tcslen(logFolder);
            size_t logFolderSize = sizeof(TCHAR) * logFolderLength;
            size_t moduleFileNameLength = ::_tcslen(moduleFileName);
            size_t moduleFileNameSize = sizeof(TCHAR) * moduleFileNameLength;
            LPCTSTR log = _T(".log");
            size_t logLength = ::_tcslen(log);
            size_t logSize = sizeof(TCHAR) * logLength;
            LPTSTR logPath
                = (LPTSTR)
                    ::malloc(
                            (sizeof(TCHAR) * envVarValueLength1)
                                + separatorSize
                                + appFileNameSize
                                + logFolderSize
                                + moduleFileNameSize
                                + logSize);

            if (logPath)
            {
                DWORD envVarValueLength
                    = ::GetEnvironmentVariable(
                            envVarName,
                            logPath,
                            envVarValueLength1);

                if (envVarValueLength
                        && (envVarValueLength < envVarValueLength1))
                {
                    LPTSTR str = logPath + envVarValueLength;

                    ::memcpy(str, separator, separatorSize);
                    str += separatorLength;
                    ::memcpy(str, appFileName, appFileNameSize);
                    str += appFileNameLength;
                    ::memcpy(str, logFolder, logFolderSize);
                    str += logFolderLength;
                    ::memcpy(str, moduleFileName, moduleFileNameSize);
                    str += moduleFileNameLength;
                    ::memcpy(str, log, logSize);
                    str += logLength;
                    *str = '\0';

#ifdef _MSC_VER
                    ::_tfopen_s(&_stderr, logPath, _T("a+"));
#else
                    _stderr = ::_tfopen(logPath, _T("a+"));
#endif
                    _filename = ::_tcsdup(logPath);
                }
                ::free(logPath);
            }
        }

        if (moduleFileName)
        {
            ::free(moduleFileName);
        }
        if (appFileName)
        {
            ::free(appFileName);
        }
    }

    Log::_stderr = _stderr ? _stderr : stderr;
    return Log::_stderr;
}
