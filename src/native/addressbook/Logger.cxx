// Copyright (c) Microsoft Corporation. All rights reserved.

#include "Logger.h"

static Logger* logger = NULL;

void createLogger(const char* name)
{
    if (logger)
    {
        delete logger;
    }

    logger = new Logger(name);
}

void destroyLogger()
{
    if (logger)
    {
        delete logger;
    }

    logger = NULL;
}

Logger* getLogger()
{
    return logger;
}
