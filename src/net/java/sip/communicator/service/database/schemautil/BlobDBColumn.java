// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.database.schemautil;

/**
 * A blob column
 */
public class BlobDBColumn extends DBColumn
{
    public BlobDBColumn(String name)
    {
        super(name);
    }

    @Override
    protected String getTypeString()
    {
        return "BLOB";
    }

    @Override
    protected void appendDefault(StringBuilder sb)
    {
        // No-op.
    }
}
