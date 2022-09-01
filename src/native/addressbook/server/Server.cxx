/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */


#include "../StringUtils.h"
#include <stdio.h>
#include <TlHelp32.h>

#include "../Logger.h"
#include "CrashReporter.h"
#include "MapiClient.h"
#include "RpcClient.h"
#include "RpcServer.h"

void wait_parent();

volatile bool stopped;

/**
 * Request to stop the MAPI server
 */
void stop()
{
  LOG_INFO("Stop called");
  stopped = true;
}

/**
 * Run the MAPI server
 */
int main(int argc, char** argv)
{
    int rport;
    stopped = false;

    if (argc >= 2)
    {
        createLogger(argv[1]);
    }
    else
    {
        createLogger(NULL);
    }

    LOG_INFO("Started logger");

    if (argc >= 4)
    {
        start_crash_reporter((LPTSTR) argv[3]);
    }

    if (argc >= 3)
    {
      rport = atoi(argv[2]);
    }
    else
    {
      rport = 8080;
    }

    start_client(rport);

    if (start_mapi())
    {
      if (start_server())
      {
        client_start();

        client_mapi_status();

        LOG_INFO("Running the server");
        wait_parent();

        LOG_INFO("Stopping the server");
        stop_server();
      }

      LOG_INFO("Stopping MAPI");
      stop_mapi();
    }

    LOG_INFO("Stopping the client");
    stop_client();

    LOG_INFO("Destroying the logger");
    destroyLogger();

    if (argc >= 4)
    {
      stop_crash_reporter();
    }

    return 0;
}

/**
 * Wait that the parent process stops.
 */
void wait_parent()
{
    HANDLE handle = INVALID_HANDLE_VALUE;
    LOG_DEBUG("Wait for parent");

    handle = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    if(handle != INVALID_HANDLE_VALUE)
    {
        PROCESSENTRY32 processEntry;
        LOG_INFO("Got snapshot");
        memset(&processEntry, 0, sizeof(processEntry));
        processEntry.dwSize = sizeof(PROCESSENTRY32);
        DWORD id = GetCurrentProcessId();
        if(Process32First(handle, &processEntry))
        {
            do
            {
                // We have found this process
                if(processEntry.th32ProcessID == id)
                {
                    // Get the parent process handle.
                    HANDLE parentHandle
                        = OpenProcess(
                                SYNCHRONIZE
                                | PROCESS_QUERY_INFORMATION
                                | PROCESS_VM_READ,
                                FALSE,
                                processEntry.th32ParentProcessID);
                    LOG_DEBUG("Parent handle: %8.8X", parentHandle);
                    LOG_DEBUG("Parent PID: %8.8X", processEntry.th32ParentProcessID);

                    // Wait for our parent to stop.
                    DWORD exitCode;
                    GetExitCodeProcess(parentHandle, &exitCode);
                    while(exitCode == STILL_ACTIVE && !stopped)
                    {
                        BOOL rc;

                        WaitForSingleObject(parentHandle, 1000);
                        rc = GetExitCodeProcess(parentHandle, &exitCode);
                        LOG_TRACE("ec: %d s: %d, rc: %d", exitCode, stopped, (int)rc);
                    }
                    LOG_DEBUG("Parent has ended");
                    CloseHandle(parentHandle);
                    return;
                }
            }
            while(Process32Next(handle, &processEntry));
        }
        CloseHandle(handle);
    }
}

