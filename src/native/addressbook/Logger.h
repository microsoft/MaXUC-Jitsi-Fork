/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
#ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRESSBOOK_LOGGER_H_
#define _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRESSBOOK_LOGGER_H_

#include <stdio.h>
#include <stdarg.h>
#include <time.h>
#include <windows.h>

#define MAX(A, B) (((A) > (B)) ? (A) : (B))

#define ADDRESSBOOK_LOGGER_MAX_FILE_SIZE (10 * 1024 * 1024) // Ten megabytes
#define LOG_STARTUP_SUFFIX ".startup"
#define LOG_OLD_SUFFIX ".old"

/**
* Logger
*
* Wraps a file based logger.
**/
class Logger
{
  private:
    FILE* logfile;
    const char *logfile_base_name;
    int bytes_written;
    bool startup_file_written;
    CRITICAL_SECTION critical_section;

    FILE *create_log_file(const char* name)
    {
      if (name == NULL) 
      {
        printf("ERROR: null log name passed to addressbook logger\n");
        fflush(stdout);
      }

      logfile = fopen(name, "w");

      if (logfile == 0)
      {
        printf("Failed to open log file");
        fflush(stdout);
      }

      return logfile;
    }

    FILE *rotate_log_file()
    {
      char *old_file_name = (char *) malloc(sizeof(char) * 
                     (strlen(logfile_base_name) +
                      MAX(strlen(LOG_STARTUP_SUFFIX), strlen(LOG_OLD_SUFFIX)) +
                      1));

      fclose(logfile);
      bytes_written = 0;

      if (!startup_file_written) 
      {
        sprintf(old_file_name, "%s" LOG_STARTUP_SUFFIX, logfile_base_name);
        startup_file_written = true;
      }
      else
      {
        sprintf(old_file_name, "%s" LOG_OLD_SUFFIX, logfile_base_name);
      }

      CopyFile(logfile_base_name, old_file_name, false);

      return create_log_file(logfile_base_name);
    }


  public:
    Logger(const char* name)
    {
      bytes_written = 0;
      startup_file_written = false;

      if (name != NULL)
      {
        logfile_base_name = name;
        logfile = create_log_file(name);
        fprintf(logfile, "====== Opened Log ======\n");
        fflush(logfile);
      }
      else
      {
        logfile = 0;
        printf("====== Started Log ======\n");
      }

      InitializeCriticalSection(&critical_section);
    }
  
    void log(const char* level, const char* function, const int line, const char* message, ...)
    {      
      FILE* log = stdout;
      SYSTEMTIME st;
      GetSystemTime(&st);
      char tbuf[100];

      sprintf(tbuf, 
	          "%04d_%02d_%02d %02d:%02d:%02d.%03d",
			  st.wYear,
			  st.wMonth, 
			  st.wDay,
			  st.wHour, 
			  st.wMinute,
			  st.wSecond, 
			  st.wMilliseconds);

      unsigned long thread = GetCurrentThreadId();

      EnterCriticalSection(&critical_section);

      if (logfile != 0)
      {
        if (bytes_written > ADDRESSBOOK_LOGGER_MAX_FILE_SIZE)
        {
          logfile = rotate_log_file();
        }

        if (logfile != 0) 
        {
          log = logfile;
        }
      }

      bytes_written += fprintf(log, "%s %s: [%ld] %s:%u ", tbuf, level, thread, function, line);
      va_list args;
      va_start (args, message);
      bytes_written += vfprintf (log, message, args);
      va_end (args);
      bytes_written += fprintf(log, "\n");
      fflush(log);

      LeaveCriticalSection(&critical_section);
    }

    ~Logger()
    {
      if (logfile != 0)
      {
        fprintf(logfile, "====== Closing Log ======\n");
        fclose(logfile);
      }
      else
      {
        printf("====== Closing Log ======\n");
        fflush(stdout);
      }

      DeleteCriticalSection(&critical_section);
    }
};

// Global access to the current logger

// Get a reference to the global logger
Logger* getLogger();

// Create the global logger
void createLogger(const char* name);

// Destroy the current global logger
void destroyLogger();

#define LOG_ERROR(message, ...) do { Logger* l = getLogger(); if(l){ l->log("ERROR",__FUNCTION__,__LINE__,(message), ##__VA_ARGS__); } } while(0)
#define LOG_WARN(message, ...)  do { Logger* l = getLogger(); if(l){ l->log("WARN",__FUNCTION__,__LINE__,(message), ##__VA_ARGS__); } } while(0)
#define LOG_INFO(message, ...)  do { Logger* l = getLogger(); if(l){ l->log("INFO",__FUNCTION__,__LINE__,(message), ##__VA_ARGS__); } } while(0)
#define LOG_DEBUG(message, ...) do { Logger* l = getLogger(); if(l){ l->log("DEBUG",__FUNCTION__,__LINE__,(message), ##__VA_ARGS__); } } while(0)

#ifdef DEBUG

#define LOG_TRACE(message, ...) do { Logger* l = getLogger(); if(l){ l->log("TRACE",__FUNCTION__,__LINE__,(message), ##__VA_ARGS__); } } while(0)

#else

#define LOG_TRACE(message, ...) do { } while(0)

#endif 

#endif
