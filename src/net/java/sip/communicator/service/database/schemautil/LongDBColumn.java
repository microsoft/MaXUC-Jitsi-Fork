// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.database.schemautil;

/**
 * Long column which can be used to store timestamps (in ms since the epoch)
 */
public class LongDBColumn extends DBColumn
{
    private final long mDefault;
    private String mStringDefault;

    public LongDBColumn(String name)
    {
        super(name);
        mDefault = -1;
    }

    public LongDBColumn(String name, long defaultVal)
    {
        super(name);
        mHasDefault = true;
        mDefault = defaultVal;
    }

    public LongDBColumn setDefault(String stringDefault)
    {
        mHasDefault = true;
        mStringDefault = stringDefault;
        return this;
    }

    @Override
    protected String getTypeString()
    {
        return "BIGINT";
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

