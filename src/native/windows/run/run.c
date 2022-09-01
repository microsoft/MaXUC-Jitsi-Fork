/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "run.h"

#include <ctype.h> /* isspace */
#include <jni.h>
#include <psapi.h> /* GetModuleFileNameEx */
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <tchar.h>
#include <tlhelp32.h> /* CreateToolhelp32Snapshot */
#include <sys/time.h>
#include <sys/stat.h>
#include <process.h>

#include "registry.h"
#include "../setup/nls.h"

#define JAVA_MAIN_CLASS _T("net.java.sip.communicator.launcher.SIPCommunicator")

/**
 * The pipe through which the launcher is to communicate with the crash handler.
 */
static HANDLE Run_channel = INVALID_HANDLE_VALUE;

/**
 * The command line that the application has received as the <tt>cmdLine</tt>
 * function argument of its <tt>WinMain</tt> entry point and that is currently
 * unparsed i.e. the parts which have already been parsed are no longer present.
 * The --channel argument has already been parsed, but the --old_name argument
 * remains (if used).
 * WARNING: If --channel and --old_name are used at the same time they must
 * appear with --channel first, then --old_name.
 */
static LPSTR Run_cmdLine = NULL;

/**
 * The indicator which determines whether the crash handler is to launch the
 * application.
 */
static BOOL Run_launch = TRUE;

static DWORD Run_addPath(LPCTSTR path);
static DWORD Run_callStaticVoidMain(JNIEnv *jniEnv, BOOL *searchForJava);
static int Run_displayMessageBoxFromString(DWORD textId, DWORD_PTR *textArgs, LPCTSTR caption, UINT type);
static DWORD Run_equalsParentProcessExecutableFilePath(LPCTSTR executableFilePath, BOOL *equals);
static DWORD Run_getExecutableFilePath(LPTSTR *executableFilePath);
static DWORD Run_getJavaExeCommandLine(LPCTSTR javaExe, LPTSTR *commandLine);
static LPTSTR Run_getJavaLibraryPath();
static DWORD Run_getJavaVMOptionStrings(size_t head, TCHAR separator, size_t tail, LPTSTR *optionStrings, jint *optionStringCount);
static LPTSTR Run_getLockFilePath();
static LPTSTR Run_getGCLogPath();
static LPTSTR Run_getGCLogOptions();
static DWORD Run_getParentProcessId(DWORD *ppid);
static DWORD Run_handleLauncherExitCode(DWORD exitCode, LPCTSTR lockFilePath, LPCTSTR executableFilePath);
static BOOL Run_isDirectory(LPCTSTR fileName);
static BOOL Run_isFile(LPCTSTR fileName);
static DWORD Run_openProcessAndResumeThread(DWORD processId, DWORD threadId, HANDLE *process);
static DWORD Run_runAsCrashHandler(LPCTSTR executableFilePath, LPSTR cmdLine);
static DWORD Run_runAsCrashHandlerWithPipe(LPCTSTR executableFilePath, LPSTR cmdLine, HANDLE *readPipe, HANDLE *writePipe);
static DWORD Run_runAsLauncher(LPCTSTR executableFilePath, LPSTR cmdLine);
static DWORD Run_runJava(LPCTSTR executableFilePath, LPSTR cmdLine);
static DWORD Run_runJavaExe(LPCTSTR javaExe, BOOL *searchForJava);
static DWORD Run_runJavaFromJavaHome(LPCTSTR javaHome, BOOL *searchForJava);
static DWORD Run_runJavaFromRuntimeLib(LPCTSTR runtimeLib, LPCTSTR javaHome, BOOL *searchForJava);
static LPSTR Run_skipWhitespace(LPSTR str);

// Add the given directory to the START of the path
static DWORD
Run_addPath(LPCTSTR path)
{
    // There is no practical limit to the size of the PATH environment variable, so the
    // limit here is simply how much stack space we are prepared to allow for
    TCHAR envVar[10000];
    LPTSTR str = envVar;

    DWORD pathLength = _tcslen(path);
    // Allow for the separator, and the null terminator when calculating the space after the path component
    DWORD envVarCapacity = sizeof(envVar) / sizeof(TCHAR) - pathLength - 2;

    LPCTSTR envVarName = _T("PATH");
    DWORD envVarLength;
    DWORD error;

    // Copy the new component to be the start of the path
    _tcsncpy(str, path, pathLength);

    // add the path separator
    str += pathLength;
    *str = _T(';');
    str++;

    // Read the existing PATH after the new component and the separator
    envVarLength = GetEnvironmentVariable(envVarName, str, envVarCapacity);

    if (envVarLength)
    {
        if (envVarLength >= envVarCapacity)
            error = ERROR_NOT_ENOUGH_MEMORY;
        else
        {
            // Add the NUL terminator
            str += envVarLength;
            *str = 0;

            if (SetEnvironmentVariable(envVarName, envVar))
                error = ERROR_SUCCESS;
            else
                error = GetLastError();
        }
    }
    else
        error = GetLastError();
    return error;
}

static DWORD
Run_callStaticVoidMain(JNIEnv *jniEnv, BOOL *searchForJava)
{
    LPTSTR mainClassName;
    jclass mainClass;
    DWORD error;

    mainClassName = _tcsdup(JAVA_MAIN_CLASS);
    if (mainClassName)
    {
        LPTSTR ch;

        for (ch = mainClassName; *ch; ch++)
            if (_T('.') == *ch)
                *ch = _T('/');
        mainClass = (*jniEnv)->FindClass(jniEnv, mainClassName);
        free(mainClassName);
    }
    else
        mainClass = NULL;
    if (mainClass)
    {
        jmethodID mainMethodID
            = (*jniEnv)->GetStaticMethodID(
                jniEnv,
                mainClass,
                "main",
                "([Ljava/lang/String;)V");

        if (mainMethodID)
        {
            jclass stringClass
                = (*jniEnv)->FindClass(jniEnv, "java/lang/String");

            if (stringClass)
            {
                int argc = 0;
                LPWSTR *argv = NULL;

                if (Run_cmdLine && strlen(Run_cmdLine))
                {
                    LPWSTR cmdLineW = NLS_str2wstr(Run_cmdLine);

                    if (cmdLineW)
                    {
                        argv = CommandLineToArgvW(cmdLineW, &argc);
                        free(cmdLineW);
                        error = argv ? ERROR_SUCCESS : GetLastError();
                    }
                    else
                        error = ERROR_NOT_ENOUGH_MEMORY;
                }
                else
                    error = ERROR_SUCCESS;
                if (ERROR_SUCCESS == error)
                {
                    jobjectArray mainArgs
                        = (*jniEnv)->NewObjectArray(
                                jniEnv,
                                argc, stringClass, NULL);

                    if (mainArgs)
                    {
                        int i;

                        for (i = 0; (ERROR_SUCCESS == error) && (i < argc); i++)
                        {
                            LPWSTR arg = *(argv + i);
                            jstring mainArg
                                = (*jniEnv)->NewString(
                                        jniEnv,
                                        arg, wcslen(arg));

                            if (mainArg)
                            {
                                (*jniEnv)->SetObjectArrayElement(
                                        jniEnv,
                                        mainArgs, i, mainArg);
                                if (JNI_TRUE
                                        == (*jniEnv)->ExceptionCheck(jniEnv))
                                    error = ERROR_FUNCTION_FAILED;
                            }
                            else
                                error = ERROR_NOT_ENOUGH_MEMORY;
                        }
                        if (argv)
                        {
                            LocalFree(argv);
                            argv = NULL;
                        }

                        if (ERROR_SUCCESS == error)
                        {
                            *searchForJava = FALSE;

                            /*
                             * The parent process will have to wait for and get
                             * the exit code of its child, not java.exe so it
                             * does not need telling who to wait for or get the
                             * exit code of.
                             */
                            if (INVALID_HANDLE_VALUE != Run_channel)
                            {
                                CloseHandle(Run_channel);
                                Run_channel = INVALID_HANDLE_VALUE;
                            }

                            (*jniEnv)->CallStaticVoidMethod(
                                    jniEnv,
                                    mainClass, mainMethodID, mainArgs);
                        }
                    }
                    else
                        error = ERROR_NOT_ENOUGH_MEMORY;

                    if (argv)
                        LocalFree(argv);
                }
            }
            else
                error = ERROR_CLASS_DOES_NOT_EXIST;
        }
        else
            error = ERROR_INVALID_FUNCTION;
    }
    else
        error = ERROR_CLASS_DOES_NOT_EXIST;

    return error;
}

static int
Run_displayMessageBoxFromString(
    DWORD textId, DWORD_PTR *textArgs,
    LPCTSTR caption,
    UINT type)
{
    TCHAR format[1024];
    int formatLength
        = LoadString(
                GetModuleHandle(NULL),
                textId,
                format, sizeof(format) / sizeof(TCHAR));
    int answer = 0;

    if (formatLength > 0)
    {
        LPTSTR message = NULL;
        DWORD messageLength
            = FormatMessage(
                    FORMAT_MESSAGE_ALLOCATE_BUFFER
                        | FORMAT_MESSAGE_ARGUMENT_ARRAY
                        | FORMAT_MESSAGE_FROM_STRING,
                    format,
                    0,
                    LANG_USER_DEFAULT,
                    (LPTSTR) &message,
                    0,
                    (va_list *) textArgs);

        if (messageLength)
        {
            answer
                = MessageBox(
                        NULL,
                        message,
                        caption,
                        type);
            LocalFree(message);
        }
    }
    return answer;
}

static DWORD
Run_equalsParentProcessExecutableFilePath(
    LPCTSTR executableFilePath,
    BOOL *equals)
{
    DWORD ppid = 0;
    DWORD error = Run_getParentProcessId(&ppid);

    if (ERROR_SUCCESS == error)
    {
        HANDLE parentProcess
            = OpenProcess(
                    PROCESS_QUERY_INFORMATION | PROCESS_VM_READ,
                    FALSE,
                    ppid);

        if (parentProcess)
        {
            TCHAR parentProcessExecutableFilePath[MAX_PATH + 1];
            DWORD parentProcessExecutableFilePathLength
                = GetModuleFileNameEx(
                        parentProcess,
                        NULL,
                        parentProcessExecutableFilePath,
                        sizeof(parentProcessExecutableFilePath));

            if (parentProcessExecutableFilePathLength)
            {
                *equals
                    = (_tcsnicmp(
                                parentProcessExecutableFilePath,
                                executableFilePath,
                                parentProcessExecutableFilePathLength)
                            == 0);
            }
            else
                error = GetLastError();

            CloseHandle(parentProcess);
        }
        else
            error = GetLastError();
    }
    return error;
}

static DWORD
Run_getExecutableFilePath(LPTSTR *executableFilePath)
{
    TCHAR str[MAX_PATH + 1];
    DWORD capacity = sizeof(str);
    DWORD length = GetModuleFileName(NULL, str, capacity);
    DWORD error;

    if (length)
    {
        /* Make sure str is null terminated on Windows XP/2000. */
        if (length == capacity)
        {
            length--;
            str[length] = 0;
        }

        *executableFilePath = (LPTSTR) malloc(sizeof(TCHAR) * (length + 1));
        if (*executableFilePath)
        {
            _tcsncpy(*executableFilePath, str, length);
            *((*executableFilePath) + length) = 0;
            error = ERROR_SUCCESS;
        }
        else
            error = ERROR_OUTOFMEMORY;
    }
    else
        error = GetLastError();
    return error;
}

static DWORD
Run_getJavaExeCommandLine(LPCTSTR javaExe, LPTSTR *commandLine)
{
    LPCTSTR mainClass = JAVA_MAIN_CLASS;

    size_t javaExeLength;
    size_t mainClassLength;
    size_t cmdLineLength;
    DWORD error;

    javaExeLength = _tcslen(javaExe);
    mainClassLength = _tcslen(mainClass);
    if (Run_cmdLine)
    {
        cmdLineLength = _tcslen(Run_cmdLine);
        if (cmdLineLength)
            cmdLineLength++; /* ' ' */
    }
    else
        cmdLineLength = 0;

    error
        = Run_getJavaVMOptionStrings(
            javaExeLength + 1 /* ' ' */,
            _T(' '),
            mainClassLength + cmdLineLength,
            commandLine,
            NULL);
    if (ERROR_SUCCESS == error)
    {
        LPTSTR str = *commandLine;

        _tcsncpy(str, javaExe, javaExeLength);
        str += javaExeLength;
        *str = _T(' ');
        str++;

        str += _tcslen(str);

        _tcsncpy(str, mainClass, mainClassLength);
        str += mainClassLength;
        if (cmdLineLength)
        {
            *str = _T(' ');
            str++;
            cmdLineLength--;
            _tcsncpy(str, Run_cmdLine, cmdLineLength);
            str += cmdLineLength;
        }
        *str = 0;
    }

    return error;
}

static LPTSTR
Run_getJavaLibraryPath()
{
    LPCTSTR relativeJavaLibraryPath = _T("native");
    TCHAR javaLibraryPath[MAX_PATH + 1];
    DWORD javaLibraryPathCapacity
        = sizeof(javaLibraryPath) / sizeof(TCHAR);
    DWORD javaLibraryPathLength
        = GetFullPathName(
                relativeJavaLibraryPath,
                javaLibraryPathCapacity, javaLibraryPath,
                NULL);
    LPCTSTR dup;

    if (javaLibraryPathLength
            && (javaLibraryPathLength < javaLibraryPathCapacity))
    {
        LPTSTR str = javaLibraryPath;

        str += javaLibraryPathLength;
        *str = 0;

        dup = javaLibraryPath;
    }
    else
        dup = relativeJavaLibraryPath;
    return _tcsdup(dup);
}

static DWORD
Run_getJavaVMOptionStrings
    (size_t head, TCHAR separator, size_t tail,
        LPTSTR *optionStrings, jint *optionStringCount)
{
    LPTSTR javaLibraryPath = Run_getJavaLibraryPath();
    jint _optionStringCount = 0;
    DWORD error;
    TCHAR oldNameValue[1000];
    oldNameValue[0] = '\0';

    /* Extract and remove the --old_name command-line argument, if present. */
    /* If used, it will now be the first argument in Run_cmdLine            */
    if (Run_cmdLine && strlen(Run_cmdLine))
    {
        LPCSTR oldNameArg = "--old_name=";
        size_t oldNameArgLength = strlen(oldNameArg);

        /* Get the value of the "--old_name" command-line argument.           */
        if ((strlen(Run_cmdLine) > oldNameArgLength)
                 && (strnicmp(Run_cmdLine, oldNameArg, oldNameArgLength) == 0))
        {
          Run_cmdLine += oldNameArgLength;
          if (*Run_cmdLine == '\"')
          {
              /* Copy the value of the old_name argument up to the closing */
              /* quotation mark or the end of the string.                  */
              Run_cmdLine++;
              int ii = 0;
              while ((*Run_cmdLine != '\0') && (*Run_cmdLine != '\"'))
              {
                // Assignment with * occurs before incrementing Run_cmdLine.
                oldNameValue[ii] = *Run_cmdLine++;
                ii++;
              }
              oldNameValue[ii] = '\0';

              /*
               * Skip the quotation mark and whitespace after the value of oldNameArg.
               */
              if (*Run_cmdLine == '\"')
              {
                Run_cmdLine++;
              }
              Run_cmdLine = Run_skipWhitespace(Run_cmdLine);
          }
       }
    }

    /* If there was no --old_name argument, use the branded-in value. */
    if (oldNameValue[0] == '\0')
    {
        strncpy(oldNameValue, OLDPRODUCTNAME, 1000);
    }

    if (javaLibraryPath)
    {
        // Add library path to PATH
        Run_addPath(javaLibraryPath);

        // List of classpath paths
        LPCTSTR classpath[]
            = {
                _T("lib\\org.apache.felix.main.jar"),
                _T("lib\\bcprovider.jar"),
                _T("lib\\bundle\\Java-WebSocket.jar"),
                _T("lib\\bundle\\metaswitch-protobuf.jar"),
                _T("lib\\bundle\\protobuf-java.jar"),
                _T("lib\\javafx-win.jar"),
                _T("lib\\swt.jar"),
                _T("sc-bundles\\sc-launcher.jar"),
                _T("sc-bundles\\util.jar"),
                _T("sc-bundles\\libjitsi.jar"),
                _T("sc-bundles\\jnalib.jar"),
                _T("lib"),
                NULL
            };
        // List of JVM properties in pairs of property name then value
        // e.g. -config=file:./lib/config.properties
        LPCTSTR properties[]
            = {
                _T("felix.config.properties"),
                _T("file:./lib/felix.client.run.properties"),
                _T("java.util.logging.config.file"),
                _T("lib/logging.properties"),
                _T("java.library.path"),
                javaLibraryPath,
                _T("jna.library.path"),
                javaLibraryPath,
                _T("jna.boot.library.path"),
                javaLibraryPath,
                _T("jna.nounpack"),
                _T("true"),
                _T("net.java.sip.communicator.SC_HOME_DIR_NAME"),
                PRODUCTNAME,
                _T("net.java.sip.communicator.SC_OLD_APP_NAME"),
                oldNameValue,
                _T("visualvm.display.name"),
                _T("sip.communicator"),
                _T("sun.java2d.uiScale"),
                _T("1.0"),
                _T("sun.awt.nopixfmt"),
                _T("true"),
                // Explicitly turn on JavaAccessBridge, which avoids the user having
                // to turn it on with jabswitch
                _T("javax.accessibility.assistive_technologies"),
                _T("com.sun.java.accessibility.AccessBridge"),
                // Remove limit on javacpp physicalBytes counter, due to issues where it miscounts on Mac. See https://github.com/bytedeco/javacpp/issues/516 and linked issues
                _T("org.bytedeco.javacpp.maxPhysicalBytes"),
                _T("0"),
                // Restrict the Netty heap usage by limiting it to a single 4 MiB buffer (buffer size = 2^maxOrder * 8192)
                _T("io.netty.allocator.numHeapArenas"),
                _T("1"),
                _T("io.netty.allocator.maxOrder"),
                _T("9"),
                NULL
            };
        // List of other standard JVM options that we provide, again in pairs of option name then value
        // e.g. -agentlib:server=y
        // If changing these options, consider whether you need to make the same change to the Mac build options in the bundleapp
        LPCTSTR options[]
          = {
                  _T("XX"),
                  _T("+UseG1GC"),
                  _T("Xlog"),
                  Run_getGCLogOptions(),
                  NULL
            };

        // List of JVM options that don't come in pairs.
        LPCTSTR params[]
          = {
                  _T("Xss576k"), // Stack size for each thread
                  _T("Xmx256m"), // Max heap size
                  NULL
          };

        size_t classpathLength;
        size_t propertiesLength;
        size_t optionsLength;
        size_t paramsLength;
        BOOL quote = separator;

        // Calculate the length of the classpath string
        {
            LPCTSTR cp;
            size_t i = 0;

            classpathLength = 0;
            while ((cp = classpath[i++]))
                classpathLength += (_tcslen(cp) + 1 /* ';' */);
            if (classpathLength)
                classpathLength += 18 /* "-Djava.class.path=" */;
        }
        // Calculate the length of the properties string
        {
            LPCTSTR property;
            size_t i = 0;

            propertiesLength = 0;
            while ((property = properties[i++]))
            {
                propertiesLength
                    += (2 /* "\"-D" */
                        + _tcslen(property)
                        + 1 /* '=' */
                        + _tcslen(properties[i++])
                        + 1 /* ' ' */);
                if (quote)
                    propertiesLength += 2;
            }
        }
        // Calculate the length of the options string
        {
            LPCTSTR option;
            size_t i = 0;

            optionsLength = 0;
            while ((option = options[i++]))
            {
                optionsLength
                    += (1 /* '-' */
                        + _tcslen(option)
                        + 1 /* ':' */
                        + _tcslen(options[i++])
                        + 1 /* ' '*/);
                if (quote)
                    optionsLength += 2;
            }
        }
        // Calculate the length of the parameters string
        {
            LPCTSTR param;
            size_t i = 0;

            paramsLength = 0;
            while ((param = params[i++]))
            {
                paramsLength
                += (1 /* '-' */
                    + _tcslen(param)
                    + 1 /* ' '*/);
                if (quote)
                    paramsLength += 2;
            }
        }

        *optionStrings
            = (LPTSTR)
                malloc(
                    sizeof(TCHAR)
                        * (head
                            + classpathLength
                            + propertiesLength
                            + optionsLength
                            + paramsLength
                            + 1 /* 0 */
                            + tail));
        if (*optionStrings)
        {
            LPTSTR str = (*optionStrings) + head;

            if (classpathLength)
            {
                LPCTSTR cp;
                size_t i = 0;

                _tcscpy(str, _T("-Djava.class.path="));
                str += 18;
                while ((cp = classpath[i++]))
                {
                    size_t length = _tcslen(cp);

                    _tcsncpy(str, cp, length);
                    str += length;
                    *str = _T(';');
                    str++;
                }
                str--; /* Drop the last ';'. */
                *str = separator;
                str++;

                _optionStringCount++;
            }
            if (propertiesLength)
            {
                LPCTSTR property;
                size_t i = 0;

                while ((property = properties[i++]))
                {
                    size_t length;
                    LPCTSTR value;

                    if (quote)
                        *str++ = _T('"');
                    _tcscpy(str, _T("-D"));
                    str += 2;
                    length = _tcslen(property);
                    _tcsncpy(str, property, length);
                    str += length;
                    *str++ = _T('=');

                    value = properties[i++];
                    length = _tcslen(value);
                    _tcsncpy(str, value, length);
                    str += length;
                    if (quote)
                        *str++ = _T('"');
                    *str++ = separator;

                    _optionStringCount++;
                }
            }
            if (optionsLength)
            {
                LPCTSTR option;
                size_t i = 0;

                while ((option = options[i++]))
                {
                    size_t length;
                    LPCTSTR value;

                    if (quote)
                        *str++ = _T('"');
                    _tcscpy(str, _T("-"));
                    str++;
                    length = _tcslen(option);
                    _tcsncpy(str, option, length);
                    str += length;
                    *str++ = _T(':');

                    value = options[i++];
                    length = _tcslen(value);
                    _tcsncpy(str, value, length);
                    str += length;
                    if (quote)
                        *str++ = _T('"');
                    *str++ = separator;

                    _optionStringCount++;
                }
            }
            if (paramsLength)
            {
                LPCTSTR param;
                size_t i = 0;

                while ((param = params[i++]))
                {
                    size_t length;

                    if (quote)
                        *str++ = _T('"');
                    _tcscpy(str, _T("-"));
                    str++;
                    length = _tcslen(param);
                    _tcsncpy(str, param, length);
                    str += length;
                    if (quote)
                        *str++ = _T('"');
                    *str++ = separator;

                    _optionStringCount++;
                }
            }
            *str = 0;

            if (optionStringCount)
                *optionStringCount = _optionStringCount;
            error = ERROR_SUCCESS;
        }
        else
            error = ERROR_OUTOFMEMORY;

        free(javaLibraryPath);
    }
    else
        error = ERROR_OUTOFMEMORY;

    return error;
}

static LPTSTR
Run_getLockFilePath()
{
    TCHAR appData[MAX_PATH + 1];
    DWORD appDataCapacity = sizeof(appData) / sizeof(TCHAR);
    DWORD appDataLength
        = GetEnvironmentVariable(
                _T("APPDATA"),
                appData, appDataCapacity);
    LPTSTR lockFilePath = NULL;

    if (appDataLength && (appDataLength < appDataCapacity))
    {
        LPCTSTR productName = PRODUCTNAME;
        size_t productNameLength = _tcslen(productName);
        LPCTSTR lockFileName = _T(".lock");
        size_t lockFileNameLength = _tcslen(lockFileName);

        lockFilePath
            = (LPTSTR)
                malloc(
                        sizeof(TCHAR)
                            * (appDataLength
                                    + 1
                                    + productNameLength
                                    + 1
                                    + lockFileNameLength
                                    + 1));
        if (lockFilePath)
        {
            LPTSTR str = lockFilePath;

            _tcsncpy(str, appData, appDataLength);
            str += appDataLength;
            *str = _T('\\');
            str++;
            _tcsncpy(str, productName, productNameLength);
            str += productNameLength;
            *str = _T('\\');
            str++;
            _tcsncpy(str, lockFileName, lockFileNameLength);
            str += lockFileNameLength;
            *str = 0;
        }
    }
    return lockFilePath;
}

static LPTSTR
Run_getGCLogPath()
{
    TCHAR appData[MAX_PATH + 1];
    DWORD appDataCapacity = sizeof(appData) / sizeof(TCHAR);
    DWORD appDataLength
        = GetEnvironmentVariable(
                _T("APPDATA"),
                appData, appDataCapacity);
    LPTSTR gcLogPath = NULL;

    if (appDataLength && (appDataLength < appDataCapacity))
    {
        // Create a GC log with a process ID and timestamp to make sure we
        // can identify and order them for deletion later (see
        // ErrorReportActivator).
        struct timeval now;
        gettimeofday(&now, NULL);
        char strLogName[50];
        sprintf(strLogName, "gc.%ld.%ld%ld.log", getpid(),
                                now.tv_sec, now.tv_usec / 1000);

        LPCTSTR productName = PRODUCTNAME;
        size_t  productNameLength = _tcslen(productName);
        LPCTSTR gcLogFolderName = _T("log");
        size_t  gcLogFolderNameLength = _tcslen(gcLogFolderName);
        LPCTSTR gcLogFileName = _T(strLogName);
        size_t  gcLogFileNameLength = _tcslen(gcLogFileName);

        gcLogPath
            = (LPTSTR)
                malloc(
                        sizeof(TCHAR)
                            * (appDataLength
                                    + 1
                                    + productNameLength
                                    + 1
                                    + gcLogFolderNameLength
                                    + 1
                                    + gcLogFileNameLength
                                    + 1));
        if (gcLogPath)
        {
            LPTSTR str = gcLogPath;

            _tcsncpy(str, appData, appDataLength);
            str += appDataLength;
            *str = _T('\\');
            str++;
            _tcsncpy(str, productName, productNameLength);
            str += productNameLength;
            *str = _T('\\');
            str++;
            _tcsncpy(str, gcLogFolderName, gcLogFolderNameLength);
            str += gcLogFolderNameLength;
            *str = _T('\\');
            str++;
            _tcsncpy(str, gcLogFileName, gcLogFileNameLength);
            str += gcLogFileNameLength;
            *str = 0;

            LPTSTR  mkdircmd = NULL;
            LPCTSTR mkdir    = _T("mkdir \"");
            LPCTSTR endQuote = _T("\"");
            size_t  mkdirLength     = _tcslen(mkdir);
            size_t  gcLogPathLength = _tcslen(gcLogPath);
            size_t  endQuoteLength  = _tcslen(endQuote);

            // We also need to ensure that the log _directory_ exists before
            // writing the log file, otherwise JVM startup fails as it can't
            // write the gc log.
            // malloc slightly more than we need for simplicity.
            mkdircmd = (LPTSTR) malloc(sizeof(TCHAR) * (mkdirLength +
                                                        gcLogPathLength +
                                                        endQuoteLength +
                                                        1));
            if (mkdircmd)
            {
                LPTSTR str = mkdircmd;

                _tcsncpy(str, mkdir, mkdirLength);
                str += mkdirLength;
                _tcsncpy(str, gcLogPath, gcLogPathLength);
                // Overwrite the \file portion of the path as we only want to
                // create the folder.
                str += gcLogPathLength - 1 - gcLogFileNameLength;
                _tcsncpy(str, endQuote, endQuoteLength);
                str += endQuoteLength;
                *str = 0;
            }

            // Get path to the prospective log folder and create it, only if
            // needed.
            struct stat info;
            LPTSTR logFolder = mkdircmd + 7;
            if (stat(logFolder, &info) != 0)
            {
                system(mkdircmd);
            }
            free(mkdircmd);
        }
    }

    return gcLogPath;
}

// We want the Xlog GC option to be like this:
//   gc*=info:file=XXX:t,ti,l,tg:filecount=2,filesize=256k
//
// This will:
//   - Log all garbage collection logs (gc) to the specified file at 'info' level
//   - Include timestamps (t), Thread IDs (ti), log levels (l) and tags (tg)
//   - It will rotate the files at 256 kB, and keep two old ones (in addition to the one currently being written)
//
// We use info level logs, rather than debug, so that our GC log files cover a longer period.  This is expected to
// be more useful than very high verbosity logs, but only covering the last few seconds.  Info logs still include
// the most useful things (GC pause times, number/type of collections, heap usage before/after collection).
static LPTSTR
Run_getGCLogOptions()
{
    LPTSTR  option = NULL;
    LPCTSTR prefix  = _T("gc*=info:file=\"");
    LPTSTR  logPath = Run_getGCLogPath();
    LPCTSTR postfix = _T("\":t,ti,l,tg:filecount=2,filesize=256k");
    size_t prefixLength  = _tcslen(prefix);
    size_t logPathLength = _tcslen(logPath);
    size_t postfixLength = _tcslen(postfix);

    option = (LPTSTR) malloc(sizeof(TCHAR) * (prefixLength +
                                              logPathLength +
                                              postfixLength +
                                              1));
    if (option)
    {
        LPTSTR str = option;

        _tcsncpy(str, prefix, prefixLength);
        str += prefixLength;
        _tcsncpy(str, logPath, logPathLength);
        str += logPathLength;
        _tcsncpy(str, postfix, postfixLength);
        str += postfixLength;
        *str = 0;
    }

    free(logPath);
    return option;
}

static DWORD
Run_getParentProcessId(DWORD *ppid)
{
    HANDLE snapshot = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    DWORD error;

    if (INVALID_HANDLE_VALUE == snapshot)
        error = GetLastError();
    else
    {
        PROCESSENTRY32 pe32;

        pe32.dwSize = sizeof(PROCESSENTRY32);
        if (Process32First(snapshot, &pe32))
        {
            DWORD pid = GetCurrentProcessId();

            error = ERROR_FILE_NOT_FOUND;
            do
            {
                if (pe32.th32ProcessID == pid)
                {
                    error = ERROR_SUCCESS;
                    *ppid = pe32.th32ParentProcessID;
                    break;
                }

                if (!Process32Next(snapshot, &pe32))
                {
                    error = GetLastError();
                    break;
                }
            }
            while (1);
        }
        else
            error = GetLastError();

        CloseHandle(snapshot);
    }
    return error;
}

static DWORD
Run_handleLauncherExitCode(
    DWORD exitCode, LPCTSTR lockFilePath,
    LPCTSTR executableFilePath)
{
    DWORD error = ERROR_SUCCESS;

    if (Run_isFile(lockFilePath))
    {
        DWORD_PTR arguments[] = { (DWORD_PTR) PRODUCTNAME };
        int answer
            = Run_displayMessageBoxFromString(
                    IDS_CRASHANDRELAUNCH, arguments,
                    executableFilePath,
                    MB_ICONEXCLAMATION | MB_YESNO | MB_TOPMOST);

        if (answer)
        {
            if (IDYES == answer)
                Run_launch = TRUE;

            /*
             * We believe the lockFilePath is related to the reported crash
             * instance so we have to remove it after notifying the user in
             * order to not take it into account upon a possible next launch.
             */
            DeleteFile(lockFilePath);
        }
        else
            error = GetLastError();
    }
    return error;
}

static BOOL
Run_isDirectory(LPCTSTR fileName)
{
    DWORD fileAttributes = GetFileAttributes(fileName);

    return
        (INVALID_FILE_ATTRIBUTES != fileAttributes)
            && (0 != (FILE_ATTRIBUTE_DIRECTORY & fileAttributes));
}

static BOOL
Run_isFile(LPCTSTR fileName)
{
    DWORD fileAttributes = GetFileAttributes(fileName);

    return
        (INVALID_FILE_ATTRIBUTES != fileAttributes)
            && (0 == (FILE_ATTRIBUTE_DIRECTORY & fileAttributes));
}

static DWORD
Run_openProcessAndResumeThread(DWORD processId, DWORD threadId, HANDLE *process)
{
    HANDLE p
        = OpenProcess(
                PROCESS_QUERY_INFORMATION | SYNCHRONIZE,
                FALSE,
                processId);
    DWORD error;

    if (p)
    {
        HANDLE t = OpenThread(THREAD_SUSPEND_RESUME, FALSE, threadId);

        if (t)
        {
            DWORD prevSuspendCount = ResumeThread(t);

            if (1 == prevSuspendCount)
            {
                *process = p;
                error = ERROR_SUCCESS;
            }
            else
                error = ERROR_NOT_FOUND;
            CloseHandle(t);
        }
        else
            error = GetLastError();
        if (ERROR_SUCCESS != error)
            CloseHandle(p);
    }
    else
        error = GetLastError();
    return error;
}

static DWORD
Run_runAsCrashHandler(LPCTSTR executableFilePath, LPSTR cmdLine)
{
    SECURITY_ATTRIBUTES pipeAttributes;
    HANDLE readPipe = INVALID_HANDLE_VALUE;
    HANDLE writePipe = INVALID_HANDLE_VALUE;
    DWORD error;

    ZeroMemory(&pipeAttributes, sizeof(pipeAttributes));
    pipeAttributes.nLength = sizeof(pipeAttributes);
    pipeAttributes.bInheritHandle = TRUE;
    if (CreatePipe(&readPipe, &writePipe, &pipeAttributes, 0))
    {
        /*
         * Do not let the child process inherit the readPipe because it does not
         * need it.
         */
        HANDLE currentProcess = GetCurrentProcess();
        HANDLE readPipeDuplicate = INVALID_HANDLE_VALUE;

        if (DuplicateHandle(
                currentProcess, readPipe,
                currentProcess, &readPipeDuplicate,
                0,
                FALSE,
                DUPLICATE_SAME_ACCESS))
        {
            CloseHandle(readPipe);
            readPipe = readPipeDuplicate;

            error
                = Run_runAsCrashHandlerWithPipe(
                        executableFilePath, cmdLine,
                        &readPipe, &writePipe);
        }
        else
            error = GetLastError();

        if (INVALID_HANDLE_VALUE != readPipe)
            CloseHandle(readPipe);
        if (INVALID_HANDLE_VALUE != writePipe)
            CloseHandle(writePipe);
    }
    else
        error = GetLastError();
    return error;
}

static DWORD
Run_runAsCrashHandlerWithPipe(
    LPCTSTR executableFilePath, LPSTR cmdLine,
    HANDLE *readPipe, HANDLE *writePipe)
{
    LPCTSTR commandLineFormat = _T("run.exe --channel=%d %s");
    int commandLineLength = 256 + _tcslen(cmdLine);
    LPTSTR commandLine
        = (LPTSTR) malloc(sizeof(TCHAR) * (commandLineLength + 1));
    DWORD error;

    if (commandLine)
    {
        LPTSTR lockFilePath = Run_getLockFilePath();
        DWORD exitCode = 0;

        commandLineLength
            = _sntprintf(
                    commandLine,
                    commandLineLength,
                    commandLineFormat,
                    (int) (intptr_t) (*writePipe),
                    cmdLine);
        if (commandLineLength < 0)
        {
            free(commandLine);
            commandLine = NULL;
            error = ERROR_NOT_ENOUGH_MEMORY;
        }
        else
        {
            *(commandLine + commandLineLength) = 0;
            error = ERROR_SUCCESS;
        }

        if (ERROR_SUCCESS == error)
        {
            BOOL waitForChildProcess
                = !(lockFilePath && Run_isFile(lockFilePath));
            STARTUPINFO si;
            PROCESS_INFORMATION pi;

            ZeroMemory(&si, sizeof(si));
            si.cb = sizeof(si);
            if (CreateProcess(
                    executableFilePath,
                    commandLine,
                    NULL,
                    NULL,
                    TRUE,
                    CREATE_NO_WINDOW,
                    NULL,
                    NULL,
                    &si,
                    &pi))
            {
                HANDLE childProcessToWaitFor = NULL;
                DWORD event;

                /* We didn't really want to hold on to the thread. */
                CloseHandle(pi.hThread);

                /*
                 * The command line of the child process is no longer necessary.
                 */
                free(commandLine);
                commandLine = NULL;

                /*
                 * The child process has inherited the writePipe so close it in
                 * the current process in order to let it know that it is only
                 * waiting for the child process.
                 */
                CloseHandle(*writePipe);
                *writePipe = INVALID_HANDLE_VALUE;

                /*
                 * Wait for the child process to tell the current process if it
                 * is to wait for another child process in order to get the exit
                 * code from it.
                 */
                if (INVALID_HANDLE_VALUE != *readPipe)
                {
                    DWORD childToWaitFor[2];
                    DWORD numberOfBytesRead = 0;

                    if (ReadFile(
                                *readPipe,
                                childToWaitFor,
                                sizeof(childToWaitFor),
                                &numberOfBytesRead,
                                NULL)
                            && (numberOfBytesRead
                                    == sizeof(childToWaitFor))
                            && childToWaitFor[0]
                            && childToWaitFor[1])
                    {
                        error
                            = Run_openProcessAndResumeThread(
                                    childToWaitFor[0],
                                    childToWaitFor[1],
                                    &childProcessToWaitFor);
                    }
                    CloseHandle(*readPipe);
                    *readPipe = INVALID_HANDLE_VALUE;
                }
                if (childProcessToWaitFor)
                {
                    /*
                     * We'll have to wait for another process, not the one that
                     * we have just created ourselves.
                     */
                    CloseHandle(pi.hProcess);
                }
                else
                    childProcessToWaitFor = pi.hProcess;

                if (waitForChildProcess)
                {
                    error = ERROR_SUCCESS;
                    do
                    {
                        event
                            = WaitForSingleObject(
                                    childProcessToWaitFor,
                                    INFINITE);
                        if (WAIT_FAILED == event)
                        {
                            error = GetLastError();
                            break;
                        }
                    }
                    while (WAIT_TIMEOUT == event);

                    if ((ERROR_SUCCESS == error)
                            && !GetExitCodeProcess(
                                    childProcessToWaitFor,
                                    &exitCode))
                        error = GetLastError();
                }

                CloseHandle(childProcessToWaitFor);
            }
            else
                error = GetLastError();
        }

        if (commandLine)
            free(commandLine);

        if (lockFilePath)
        {
            /*
             * Notify the user if the application has crashed and ask whether it
             * is to be relaunched.
             */
            if ((ERROR_SUCCESS == error) && exitCode)
            {
                error
                    = Run_handleLauncherExitCode(
                            exitCode, lockFilePath,
                            executableFilePath);
            }
            free(lockFilePath);
        }
    }
    else
        error = ERROR_OUTOFMEMORY;
    return error;
}

static DWORD
Run_runAsLauncher(LPCTSTR executableFilePath, LPSTR cmdLine)
{
    LPSTR commandLine;
    DWORD error = ERROR_SUCCESS;

    /* Parse the command line. */
    if (cmdLine)
    {
        size_t commandLineLength;
        LPCSTR channelArg = "--channel=";
        size_t channelArgLength = strlen(channelArg);

        commandLine = Run_skipWhitespace(cmdLine);
        commandLineLength = strlen(commandLine);

        /* Get the value of the "--channel=" command-line argument. */
        if ((commandLineLength > channelArgLength)
                && (strnicmp(commandLine, channelArg, channelArgLength) == 0))
        {
            commandLine += channelArgLength;
            if (!isspace(*commandLine))
            {
                HANDLE channel = (HANDLE) (intptr_t) atoi(commandLine);
                DWORD flags;
                char ch;

                if (channel && GetHandleInformation(channel, &flags))
                {
                    /*
                     * Make sure channel will not be inherited by any child
                     * process.
                     */
                    HANDLE currentProcess = GetCurrentProcess();
                    HANDLE channelDuplicate = INVALID_HANDLE_VALUE;

                    if (DuplicateHandle(
                            currentProcess, channel,
                            currentProcess, &channelDuplicate,
                            0,
                            FALSE,
                            DUPLICATE_SAME_ACCESS))
                        Run_channel = channelDuplicate;
                    CloseHandle(channel);
                }

                /*
                 * Skip the value of the channelArg and the whitespace after it.
                 */
                while ((ch = *commandLine) && !isspace(ch))
                    commandLine++;
                commandLine = Run_skipWhitespace(commandLine);
            }
        }
    }
    else
        commandLine = cmdLine;

    /* Run the Java process in the directory of the executable file. */
    if (_tcslen(executableFilePath) <= MAX_PATH)
    {
        TCHAR path[MAX_PATH];
        DWORD pathCapacity = sizeof(path) / sizeof(TCHAR);
        LPTSTR filePart = NULL;
        DWORD pathLength
            = GetFullPathName(
                    executableFilePath,
                    pathCapacity, path, &filePart);

        if (!pathLength)
            error = GetLastError();
        else if (pathLength >= pathCapacity)
            error = ERROR_NOT_ENOUGH_MEMORY;
        else
        {
            /*
             * Strip the filePart because only the directory of the executable
             * file is necessary.
             */
            if (filePart && *filePart)
                *filePart = 0;
            if (!SetCurrentDirectory(path))
                error = GetLastError();
        }
    }

    error = Run_runJava(executableFilePath, commandLine);
    return error;
}

static DWORD
Run_runJava(LPCTSTR executableFilePath, LPSTR cmdLine)
{
    DWORD cdLength;
    DWORD error = ERROR_CALL_NOT_IMPLEMENTED;
    BOOL searchForJava = TRUE;

    Run_cmdLine = cmdLine;

    /* Try to use the private Java distributed with the application. */
    if ((cdLength = GetCurrentDirectory(0, NULL)))
    {
        LPTSTR cd = (LPTSTR) malloc(sizeof(TCHAR) * cdLength);

        if (cd)
        {
            cdLength = GetCurrentDirectory(cdLength, cd);
            if (cdLength)
            {
                if ((ERROR_SUCCESS != error) || searchForJava)
                    error = Run_runJavaFromJavaHome(cd, &searchForJava);
            }
            else
                error = GetLastError();
            free(cd);
        }
        else
            error = ERROR_OUTOFMEMORY;
    }
    else
        error = GetLastError();

    /* Notify the user that Java could not be found. */
    if ((ERROR_SUCCESS != error) || searchForJava)
    {
        DWORD_PTR arguments[] = { (DWORD_PTR) PRODUCTNAME };

        if (Run_displayMessageBoxFromString(
                IDS_JAVANOTFOUND, arguments,
                executableFilePath,
                MB_ICONSTOP | MB_OK | MB_TOPMOST))
        {
            /*
             * We have failed to locate Java but we've just notified the user
             * about this fact so the execution is according to plan.
             */
            error = ERROR_SUCCESS;
        }
        else
            error = GetLastError();
    }

    return error;
}

static DWORD
Run_runJavaExe(LPCTSTR javaExe, BOOL *searchForJava)
{
    DWORD error;

    if (Run_isFile(javaExe))
    {
        LPCTSTR applicationName;
        LPCTSTR fileName;
        LPTSTR commandLine = NULL;

        applicationName = javaExe;
        fileName = _T("java.exe");

        error = Run_getJavaExeCommandLine(fileName, &commandLine);

        if (ERROR_SUCCESS == error)
        {
            DWORD creationFlags;
            STARTUPINFO si;
            PROCESS_INFORMATION pi;

            creationFlags = CREATE_NO_WINDOW;
            if (INVALID_HANDLE_VALUE != Run_channel)
                creationFlags |= CREATE_SUSPENDED;
            ZeroMemory(&si, sizeof(si));
            si.cb = sizeof(si);
            if (CreateProcess(
                    applicationName,
                    commandLine,
                    NULL,
                    NULL,
                    TRUE,
                    creationFlags,
                    NULL,
                    NULL,
                    &si,
                    &pi))
            {
                *searchForJava = FALSE;

                /*
                 * Tell the parent process it will have to wait for java.exe and
                 * get its exit code.
                 */
                if (INVALID_HANDLE_VALUE != Run_channel)
                {
                    DWORD processAndThreadIds[2];
                    DWORD numberOfBytesWritten;

                    processAndThreadIds[0] = pi.dwProcessId;
                    processAndThreadIds[1] = pi.dwThreadId;
                    WriteFile(
                            Run_channel,
                            processAndThreadIds,
                            sizeof(processAndThreadIds),
                            &numberOfBytesWritten,
                            NULL);
                    FlushFileBuffers(Run_channel);
                    CloseHandle(Run_channel);
                    Run_channel = INVALID_HANDLE_VALUE;
                }

                CloseHandle(pi.hProcess);
                CloseHandle(pi.hThread);
            }
            else
                error = GetLastError();
        }

        if (commandLine)
            free(commandLine);
    }
    else
        error = ERROR_CALL_NOT_IMPLEMENTED;
    return error;
}

static DWORD
Run_runJavaFromJavaHome(LPCTSTR javaHome, BOOL *searchForJava)
{
    DWORD error;

    if (Run_isDirectory(javaHome))
    {
        size_t javaHomeLength = _tcslen(javaHome);
        LPTSTR path
            = (LPTSTR) malloc(sizeof(TCHAR) * (javaHomeLength + 19 + 1));

        if (path)
        {
            if (javaHomeLength >= 1)
            {
                TCHAR *ch = (TCHAR *) (javaHome + (javaHomeLength - 1));

                if ((_T('\\') == *ch) || (_T('/') == *ch))
                {
                    *ch = 0;
                    javaHomeLength--;
                }
            }

            _tcscpy(path, javaHome);
            error = ERROR_CALL_NOT_IMPLEMENTED;

            _tcscpy(path + javaHomeLength, _T("\\bin\\client\\jvm.dll"));
            error = Run_runJavaFromRuntimeLib(path, javaHome, searchForJava);

            if ((ERROR_SUCCESS != error) || *searchForJava)
            {
                _tcscpy(
                        path + javaHomeLength,
                        _T("\\bin\\server\\jvm.dll"));
                error
                    = Run_runJavaFromRuntimeLib(
                        path,
                        javaHome,
                        searchForJava);
            }

            if ((ERROR_SUCCESS != error) || *searchForJava)
            {
                if ((javaHomeLength >= 4)
                        && (_tcsnicmp(
                                    javaHome + javaHomeLength - 4,
                                    _T("\\jre"),
                                    4)
                                != 0))
                {
                    _tcscpy(path + javaHomeLength, _T("\\jre"));
                    error = Run_runJavaFromJavaHome(path, searchForJava);
                }

                if ((ERROR_SUCCESS != error) || *searchForJava)
                {
                    _tcscpy(path + javaHomeLength, _T("\\bin\\javaw.exe"));
                    error = Run_runJavaExe(path, searchForJava);

                    if ((ERROR_SUCCESS != error) || *searchForJava)
                    {
                        _tcscpy(path + javaHomeLength, _T("\\bin\\java.exe"));
                        error = Run_runJavaExe(path, searchForJava);
                    }
                }
            }

            free(path);
        }
        else
            error = ERROR_OUTOFMEMORY;
    }
    else
        error = ERROR_FILE_NOT_FOUND;
    return error;
}

static DWORD
Run_runJavaFromRuntimeLib
    (LPCTSTR runtimeLib, LPCTSTR javaHome, BOOL *searchForJava)
{
    HMODULE hRuntimeLib;
    DWORD error;

    if (Run_isFile(runtimeLib))
    {
        /*
         * It turns out that the bin directory in javaHome may contain
         * dependencies of the runtimeLib so add it to the PATH. Well, it may
         * not be standard but it happens to our private JRE.
         */
        if (javaHome && Run_isDirectory(javaHome))
        {
            size_t javaHomeLength = _tcslen(javaHome);
            LPTSTR javaHomeBin;

            /*
             * Drop the last file name separator if any because we will be
             * adding one later on.
             */
            while (javaHomeLength >= 1)
            {
                TCHAR ch = *(javaHome + (javaHomeLength - 1));

                if ((_T('\\') == ch) || (_T('/') == ch))
                    javaHomeLength--;
                else
                    break;
            }

            javaHomeBin
                = malloc(
                    sizeof(TCHAR) * (javaHomeLength + 4 /* "\\bin" */ + 1));
            if (javaHomeBin)
            {
                LPTSTR str = javaHomeBin;

                _tcsncpy(str, javaHome, javaHomeLength);
                str += javaHomeLength;
                _tcsncpy(str, _T("\\bin"), 4);
                str += 4;
                *str = 0;

                if (Run_isDirectory(javaHomeBin))
                    Run_addPath(javaHomeBin);

                free(javaHomeBin);
            }
        }

        hRuntimeLib = LoadLibrary(runtimeLib);
    }
    else
        hRuntimeLib = NULL;

    if (hRuntimeLib)
    {
        typedef jint (JNICALL *JNICreateJavaVMFunc)(JavaVM **, void **, void *);
        JNICreateJavaVMFunc jniCreateJavaVM
            = (JNICreateJavaVMFunc)
                GetProcAddress(hRuntimeLib, "JNI_CreateJavaVM");

        if (jniCreateJavaVM)
        {
            LPTSTR optionStrings = NULL;
            jint optionStringCount = 0;

            error
                = Run_getJavaVMOptionStrings(
                    0, 0, 0,
                    &optionStrings, &optionStringCount);
            if (ERROR_SUCCESS == error)
            {
                JavaVMOption *options
                    = calloc(optionStringCount, sizeof(JavaVMOption));

                if (options)
                {
                    jint i;
                    LPTSTR optionString;
                    JavaVMInitArgs javaVMInitArgs;
                    JavaVM *javaVM;
                    JNIEnv *jniEnv;

                    for (i = 0, optionString = optionStrings;
                            i < optionStringCount;
                            i++, optionString += (_tcslen(optionString) + 1))
                        (options + i)->optionString = optionString;

                    javaVMInitArgs.ignoreUnrecognized = JNI_FALSE;
                    javaVMInitArgs.nOptions = optionStringCount;
                    javaVMInitArgs.options = options;
                    javaVMInitArgs.version = JNI_VERSION_1_2;
                    if (jniCreateJavaVM(
                            &javaVM,
                            (void **) &jniEnv,
                            &javaVMInitArgs))
                        error = ERROR_FUNCTION_FAILED;
                    else
                    {
                        free(options);
                        options = NULL;
                        free(optionStrings);
                        optionStrings = NULL;

                        error = Run_callStaticVoidMain(jniEnv, searchForJava);
                        if (JNI_TRUE == (*jniEnv)->ExceptionCheck(jniEnv))
                            (*jniEnv)->ExceptionClear(jniEnv);

                        (*javaVM)->DestroyJavaVM(javaVM);
                    }
                    if (options)
                        free(options);
                }
                else
                    error = ERROR_OUTOFMEMORY;
                if (optionStrings)
                    free(optionStrings);
            }
        }
        else
            error = GetLastError();
        FreeLibrary(hRuntimeLib);
    }
    else
        error = GetLastError();
    return error;
}

static LPSTR
Run_skipWhitespace(LPSTR str)
{
    char ch;

    while ((ch = *str) && isspace(ch))
        str++;
    return str;
}

int CALLBACK
WinMain(HINSTANCE instance, HINSTANCE prevInstance, LPSTR cmdLine, int cmdShow)
{
    LPTSTR executableFilePath = NULL;
    DWORD error;

    AttachConsole(ATTACH_PARENT_PROCESS);

    error = Run_getExecutableFilePath(&executableFilePath);
    if (ERROR_SUCCESS == error)
    {
        BOOL runAsLauncher = FALSE;

        Run_equalsParentProcessExecutableFilePath(
                executableFilePath,
                &runAsLauncher);
        if (runAsLauncher)
        {
            error = Run_runAsLauncher(executableFilePath, cmdLine);

            if (ERROR_SUCCESS != error)
            {
                LPTSTR message;
                DWORD messageLength
                    = FormatMessage(
                            FORMAT_MESSAGE_ALLOCATE_BUFFER
                                | FORMAT_MESSAGE_FROM_SYSTEM,
                            NULL,
                            error,
                            LANG_USER_DEFAULT,
                            (LPTSTR) &message,
                            0,
                            NULL);

                if (messageLength)
                {
                    MessageBox(
                            NULL,
                            message,
                            executableFilePath,
                            MB_ICONERROR | MB_OK | MB_TOPMOST);
                    LocalFree(message);
                }
            }
        }
        else
        {
            while (Run_launch)
            {
                Run_launch = FALSE;
                error = Run_runAsCrashHandler(executableFilePath, cmdLine);
            }
        }
    }

    if (executableFilePath)
        free(executableFilePath);

    return 0;
}
