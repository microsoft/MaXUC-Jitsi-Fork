// Copyright (c) Microsoft Corporation. All rights reserved.

#include <client/windows/handler/exception_handler.h>
#include <Tchar.h>

#include "CrashReporter.h"
#include "../Logger.h"

google_breakpad::ExceptionHandler* _handler;

void start_crash_reporter(LPCTSTR folder)
{
    _handler = new google_breakpad::ExceptionHandler(folder,
                                NULL,
                                NULL,
                                NULL,
                                google_breakpad::ExceptionHandler::HANDLER_ALL);

    LOG_INFO("Started crash reporter: 0x%x", _handler);

}

void stop_crash_reporter()
{
    delete _handler;
}

bool write_minidump()
{
    bool result = FALSE;
    
    if (_handler)
    {
        LOG_INFO("Asking handler 0x%x to write minidump", _handler);
        result = _handler->WriteMinidump();
    }
    else
    {
        LOG_ERROR("Can't write minidump as no handler");
    }

    return result;
}
