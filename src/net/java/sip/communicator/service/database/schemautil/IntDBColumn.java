// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.database.schemautil;

/**
 * Integer column
 */
public class IntDBColumn extends DBColumn
{
    private final int mDefault;
    private String mStringDefault;

    public IntDBColumn(String name)
    {
        super(name);
        mDefault = -1;
    }

    public IntDBColumn(String name, int defaultVal)
    {
        super(name);
        mHasDefault = true;
        mDefault = defaultVal;
    }

    public IntDBColumn setDefault(String stringDefault)
    {
        mHasDefault = true;
        mStringDefault = stringDefault;
        return this;
    }

    @Override
    protected String getTypeString()
    {
        return "INTEGER";
    }

    @Override
    protected void appendDefault(StringBuilder sb)
    {
        if (mStringDefault != null)
        {
            sb.append(mStringDefault);
        }
        else
        {
           sb.append(mDefault);
        }
    }
}
