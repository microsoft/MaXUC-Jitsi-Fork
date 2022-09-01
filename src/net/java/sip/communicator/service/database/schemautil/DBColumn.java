// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.database.schemautil;

/**
 * Abstract representation of a database column
 */
public abstract class DBColumn
{
    /*
     * Name of this column
     */
    protected String mName;

    /*
     * Does the column have a default?
     */
    protected boolean mHasDefault = false;

    /*
     * Is the column unique?
     */
    protected boolean mUnique = false;

    /*
     * Can the column be null?
     */
    protected boolean mNotNull = false;

    protected boolean mCaseSensitive = true;

    /**
     * Constructor
     * @param name - name of the column
     */
    public DBColumn(String name)
    {
        mName = name;
    }

    /**
     * Called if the column should be unique
     * @return this.
     */
    public DBColumn unique()
    {
        mUnique = true;
        return this;
    }

    public DBColumn caseInsensitive()
    {
        mCaseSensitive = false;
        return this;
    }

    /**
     * Called if the column cannot be null
     * @return this
     */
    public DBColumn notNull()
    {
        mNotNull = true;
        return this;
    }

    public void appendCreateString(StringBuilder sb)
    {
        sb.append(mName).append(" ").append(getTypeString());
        qualify(sb);
    }

    protected void qualify(StringBuilder sb)
    {
        if (mNotNull)
        {
            sb.append(" NOT NULL");
        }
        if (mHasDefault)
        {
            sb.append(" DEFAULT ");
            appendDefault(sb);
        }
        if (mUnique)
        {
            sb.append(" UNIQUE");
        }
        if (!mCaseSensitive)
        {
           sb.append(" COLLATE NOCASE");
        }
    }

    /**
     * Get the type string for including in the table definition
     * @return the SQL type string
     */
    protected abstract String getTypeString();

    /*
     * Append the default value for the column.
     * @param sb StringBuilder to append to.
     */
    protected abstract void appendDefault(StringBuilder sb);
}
