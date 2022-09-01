// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.database.schemautil;

public class IndexColumn
{
    private final String mName;

    public IndexColumn(String name)
    {
        mName = name;
    }

    public void appendCreateString(StringBuilder sb)
    {
       sb.append(mName);
    }
}
