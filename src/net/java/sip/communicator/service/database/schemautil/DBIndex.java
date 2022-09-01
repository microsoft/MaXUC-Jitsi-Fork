// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.database.schemautil;

import java.util.ArrayList;

public class DBIndex
{
    private final String mName;
    private final String mTableName;
    private final ArrayList<IndexColumn> mColumns = new ArrayList<>();
    private boolean mUnique;

    public DBIndex(String name, String tableName)
    {
        mName = name;
        mTableName = tableName;
    }

    public DBIndex unique()
    {
        mUnique = true;
        return this;
    }

    public String getCreateString()
    {
        StringBuilder sb = new StringBuilder("CREATE ");

        if (mUnique)
        {
            sb.append("UNIQUE ");
        }

        sb.append("INDEX ")
        .append(mName)
        .append(" ON ")
        .append(mTableName)
        .append("(");

        int size = mColumns.size();
        for (int index = 0; index < size; index++)
        {
            IndexColumn col = mColumns.get(index);
            col.appendCreateString(sb);
            if (index < size - 1)
            {
               sb.append(",");
            }
        }

        sb.append(")");

        return sb.toString();
    }

    public String getDropString()
    {
        return "DROP INDEX IF EXISTS " + mName;
    }

    public DBIndex addColumn(IndexColumn column)
    {
        mColumns.add(column);
        return this;
    }
}
