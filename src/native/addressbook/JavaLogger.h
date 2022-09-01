#include <jni.h>

#ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRESSBOOK_JAVA_LOGGER_H_
#define _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRESSBOOK_JAVA_LOGGER_H_

#include <stdio.h>
#include <stdarg.h>
#include "Logger.h"

/**
* JavaLogger
*
* Wraps a net.java.sip.communicator.util.Logger
**/
class JavaLogger
{
  private:
    JNIEnv *env;
    jobject logger;

    void log(const char* level, const char* message)
    {
        jclass cls = env->GetObjectClass(logger);
        if (cls == 0)
        {
          fprintf(stderr, "Failed to find class for JavaLogger \n");
          fprintf(stderr, message);
          return;
        }
        jmethodID mid = env->GetMethodID(
          cls, level, "(Ljava/lang/Object;)V");
        if (mid == 0)
        {
          fprintf(stderr, "Failed to find method for JavaLogger with level: %s \n", level);
          fprintf(stderr, message);
          return;
        }
        jstring log = env->NewStringUTF(message);
        env->CallVoidMethod(logger, mid, log);
        env->DeleteLocalRef(log);
    }

  public:
    JavaLogger(JNIEnv *jniEnv, jclass cls) : env(jniEnv)
    {
        jfieldID fid = env->GetStaticFieldID(cls, "logger", "Lnet/java/sip/communicator/util/Logger;");
        logger = env->GetStaticObjectField(cls, fid);
    }

    void debug(const char* message, ...)
    {
        char buffer[255];
        va_list args;
        va_start (args, message);
        vsnprintf (buffer, 255, message, args);
        va_end (args);
        LOG_DEBUG(buffer);
        log("debug", buffer);
    }

    void trace(const char* message, ...)
    {
        char buffer[255];
        va_list args;
        va_start (args, message);
        vsnprintf (buffer, 255, message, args);
        va_end (args);
        LOG_TRACE(buffer);
        log("trace", buffer);
    }

    void info(const char* message, ...)
    {
        char buffer[255];
        va_list args;
        va_start (args, message);
        vsnprintf (buffer, 255, message, args);
        va_end (args);
        LOG_INFO(buffer);
        log("info", buffer);
    }

    void warn(const char* message, ...)
    {
        char buffer[255];
        va_list args;
        va_start (args, message);
        vsnprintf (buffer, 255, message, args);
        va_end (args);
        LOG_WARN(buffer);
        log("warn", buffer);
    }

    void error(const char* message, ...)
    {
        char buffer[255];
        va_list args;
        va_start (args, message);
        vsnprintf (buffer, 255, message, args);
        va_end (args);
        LOG_ERROR(buffer);
        log("error", buffer);
    }

};

#endif
 