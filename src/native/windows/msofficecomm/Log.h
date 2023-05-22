/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
#ifndef _JMSOFFICECOMM_LOG_H_
#define _JMSOFFICECOMM_LOG_H_

#include <stdio.h>
#include <tchar.h>
#include <windows.h>

// The maximum size of the two rotated log files in bytes.
#define MAX_LOG_FILE_SIZE 10000000

class Log
{
public:
    static void close()
        {
            if (_stderr && (_stderr != stderr))
            {
                ::fclose(_stderr);
                _stderr = stderr;
            }

            if (_filename != NULL)
            {
                ::free(_filename);
                _filename = NULL;
            }
        }

#ifdef _UNICODE
    static int d(LPCTSTR format, LPCSTR str);
#endif /* #ifdef _UNICODE */
    static int d(LPCTSTR format, ...);
    static FILE *open();

private:
    static LPTSTR getAppName();
    static LPTSTR getModuleFileName();

    static FILE *_stderr;
    static LPTSTR _filename;
};

#endif /* #ifndef _JMSOFFICECOMM_LOG_H_ */
