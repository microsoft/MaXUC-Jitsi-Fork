// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.database.schemautil;

/**
 * A Boolean column
 */
public class BoolDBColumn extends DBColumn
{
    private final boolean mDefault;

    public BoolDBColumn(String name)
    {
        super(name);
        mDefault = false;
    }

    @Override
    protected String getTypeString()
    {
        return "BOOLEAN";
    }

    @Override
    protected void appendDefault(StringBuilder sb)
    {
        sb.append(mDefault ? "1" : "0");
    }
}
