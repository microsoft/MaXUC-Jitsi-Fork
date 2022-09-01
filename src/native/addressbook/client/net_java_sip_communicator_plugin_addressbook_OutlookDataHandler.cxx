/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "net_java_sip_communicator_plugin_addressbook_OutlookDataHandler.h"

#include "../MAPIBitness.h"
#include "../StringUtils.h"
#include "../Logger.h"
#include "../ProductName.h"

#include "OutlookMAPIHResultException.h"

#include <Mapix.h>
#include <unistd.h>

/**
 * Forward reference so that we can stop any old server instances before we
 * try to start a new one.
 */
void stopServer(JavaLogger* logger);
HRESULT startServerBitness(JavaLogger* logger, const char* logFile, int port, const char* logDir, int bitness);

static HANDLE serverHandle = NULL;

/**
 * Starts the server.
 *
 * @param S_OK if the server started correctly. E_FAIL otherwise.
 */
HRESULT startServer(JavaLogger* logger, const char* logFile, int port, const char* logDir)
{
    int bitness = MAPIBitness_getOutlookBitnessVersion(logger);
    HRESULT result;

    if (serverHandle != NULL)
    {
      logger->error("Server already running: %8.8X", serverHandle);
      stopServer(logger);
    }

    if (bitness != -1)
    {
        result = startServerBitness(logger, logFile, port, logDir, bitness);
    }
    else
    {
        // We've failed to determine the bitness of Outlook from looking at the registry.
        // Try 32-bit, wait five seconds to see if the server's still running, and if it isn't try 64-bit.
        logger->info("Failed to determine bitness - attempt both");
        result = startServerBitness(logger, logFile, port, logDir, 32);

        sleep(5);
        DWORD exitCode;
        GetExitCodeProcess(serverHandle, &exitCode);
        
        if (exitCode != STILL_ACTIVE)
        {
            logger->error("32-bit server isn't alive: %d.  Trying 64-bit server.", exitCode);
            result = startServerBitness(logger, logFile, port, logDir, 64);
        }
    }

    logger->info("startServer result: %d.", result);

    return result;
}

/**
 * Start the server at a specifed bitness.
 */
HRESULT startServerBitness(JavaLogger* logger, const char* logFile, int port, const char* logDir, int bitness)
{
    // Start the server executable service
    char applicationName32[] = "32/" PRODUCT_NAME "OutlookServer32.exe";
    char applicationName64[] = "64/" PRODUCT_NAME "OutlookServer64.exe";
    char * applicationName = applicationName32;
    if(bitness == 64)
    {
        applicationName = applicationName64;
    }
    int applicationNameLength = strlen(applicationName);
    char *currentDirectory = (char *)malloc(FILENAME_MAX - applicationNameLength - 8);
    GetCurrentDirectory(
            FILENAME_MAX - applicationNameLength - 8,
            currentDirectory);
    char server[FILENAME_MAX];
    snprintf(server, FILENAME_MAX, "%s/native/%s", currentDirectory, applicationName);
    free(currentDirectory);

    STARTUPINFO startupInfo;
    PROCESS_INFORMATION processInfo;
    memset(&startupInfo, 0, sizeof(startupInfo));
    memset(&processInfo, 0, sizeof(processInfo));
    startupInfo.dwFlags = STARTF_USESHOWWINDOW;
    startupInfo.wShowWindow = SW_HIDE;

    // Test 2 files: 0 for the build version, 1 for the git source version.
    char buildVersion[MAX_PATH];
    snprintf(buildVersion, MAX_PATH, "%s \"%s\" %d \"%s\"", server, logFile, port, logDir);
    char testVersion[MAX_PATH];
    snprintf(testVersion, MAX_PATH, "%s \"%s\" %d \"%s\"", applicationName, logFile, port, logDir);

    char * serverExec[2];
    serverExec[0] = buildVersion;
    serverExec[1] = testVersion;
    for(int i = 0; i < 2; ++i)
    {
        logger->info("Starting: '%s'", serverExec[i]);

        // Create the server
        if(CreateProcess(
                    NULL,
                    serverExec[i],
                    NULL, NULL, false, 0, NULL, NULL,
                    &startupInfo,
                    &processInfo))
        {
            serverHandle
                = processInfo.hProcess;

            logger->info("Succeeded starting server: %8.8x", serverHandle);

            return S_OK;
        }
        else
        {
          logger->warn("Failed with error: %lx", GetLastError());
        }
    }

    return E_FAIL;
}

/**
 * Stops the server.
 */
void stopServer(JavaLogger* logger)
{
    logger->info("Terminating server");

    if(serverHandle != NULL)
    {
        logger->info("Server handle: %8.8x", serverHandle);

        if (! TerminateProcess(
                serverHandle,
                1))
        {
            logger->error("Failed to terminate server: %lx", GetLastError());
        }

        if (! CloseHandle(serverHandle))
        {
            logger->error("Failed to close server handle: %lx", GetLastError());
        }

        serverHandle = NULL;
    }
    else
    {
      logger->error("Null server handle");
    }
}

/**
 * Check whether the server is still alive
 *
 * @return true if it is still alive, false otherwise
 */
JNIEXPORT jboolean JNICALL
Java_net_java_sip_communicator_plugin_addressbook_OutlookDataHandler_CheckServerIsAlive
    (JNIEnv *jniEnv, jclass clazz)
{
    JavaLogger logger(jniEnv, clazz);

    DWORD exitCode;
    GetExitCodeProcess(serverHandle, &exitCode);
    if(exitCode == STILL_ACTIVE)
    {
        return true;
    }
    else
    {
        // We don't actually care about the exit code, but log it anyway
        logger.error("Server is not alive: %d", exitCode);
        return false;
    }
}

/**
 * Initializes the native libraries by creating the logger and starting the server
 */
JNIEXPORT void JNICALL
Java_net_java_sip_communicator_plugin_addressbook_OutlookDataHandler_Initialize
    (JNIEnv *jniEnv, jclass clazz, jint port,
     jstring serverLogFile, jstring clientLogFile, jstring logDir)
{
    const char *nativeClientLogFile = jniEnv->GetStringUTFChars(clientLogFile, NULL);

    createLogger(nativeClientLogFile);

    jniEnv->ReleaseStringUTFChars(clientLogFile, nativeClientLogFile);

    JavaLogger logger(jniEnv, clazz);

    logger.debug("Initializing interface");

    const char *nativeServerLogFile = jniEnv->GetStringUTFChars(serverLogFile, NULL);
    const char *nativeLogDir = jniEnv->GetStringUTFChars(logDir, NULL);

    HRESULT hr;

    hr = startServer(&logger, nativeServerLogFile, port, nativeLogDir);

    jniEnv->ReleaseStringUTFChars(logDir, nativeLogDir);
    jniEnv->ReleaseStringUTFChars(serverLogFile, nativeServerLogFile);

    if (HR_FAILED(hr))
    {
        // Report any possible error regardless of where it has come from.
        OutlookMAPIHResultException_throwNew(
                jniEnv,
                hr,
                __FILE__, __LINE__);
    }
}

/**
 * Unitializes the native libraries by stopping the server and closing the log files
 */
JNIEXPORT void JNICALL
Java_net_java_sip_communicator_plugin_addressbook_OutlookDataHandler_Uninitialize
    (JNIEnv *jniEnv, jclass clazz)
{
    JavaLogger logger(jniEnv, clazz);

    stopServer(&logger);

    destroyLogger();
}

/**
 * Returns the bitness of the Outlook installation.
 *
 * @return 64 if Outlook 64 bits version is installed. 32 if Outlook 32 bits
 * version is installed. -1 otherwise.
 */
JNIEXPORT int JNICALL
Java_net_java_sip_communicator_plugin_addressbook_OutlookDataHandler_getOutlookBitnessVersion
    (JNIEnv *jniEnv, jclass clazz)
{
    JavaLogger logger(jniEnv, clazz);

    return MAPIBitness_getOutlookBitnessVersion(&logger);
}

/**
 * Returns the Outlook version installed.
 *
 * @return 2013 for "Outlook 2013", 2010 for "Outlook 2010", 2007 for "Outlook
 * 2007" or 2003 for "Outlook 2003". -1 otherwise.
 */
JNIEXPORT int JNICALL
Java_net_java_sip_communicator_plugin_addressbook_OutlookDataHandler_getOutlookVersion
    (JNIEnv *jniEnv, jclass clazz)
{
    JavaLogger logger(jniEnv, clazz);

    return MAPIBitness_getOutlookVersion(&logger);
}
