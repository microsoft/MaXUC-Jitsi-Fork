/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.

#ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRESSBOOK_CBSTR_H_
#define _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRESSBOOK_CBSTR_H_

#include "StringUtils.h"


/**
 * Allows a BSTR to printed using printf.
 *
 * Invoke as printf("%s\n", cbstr(bstr).c_str());
 **/
class cbstr
{

private:
    char* buffer;

public:
    cbstr(BSTR bstr)
    {
        buffer = StringUtils::WideCharToMultiByte(bstr);
    }

    const char* c_str()
    {
        if (buffer)
        {
              return buffer;
        }
        else
        {
              return "\0";
        }
    }

    ~cbstr()
    {
        if (buffer)
        {
            free(buffer);
        }
    }
};

#endif
