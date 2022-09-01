// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.database.schemautil;

/**
 * A text column
 */
public class TextDBColumn extends DBColumn
{
    private final String mDefault;
    private final Integer mMaxLength;

    private static final int DEFAULT_MAX_LENGTH = 512;

    public TextDBColumn(String name)
    {
        super(name);
        mMaxLength = DEFAULT_MAX_LENGTH;
        mDefault = null;
    }

    public TextDBColumn(String name, String defaultVal)
    {
        super(name);
        mMaxLength = DEFAULT_MAX_LENGTH;
        mHasDefault = true;
        mDefault = defaultVal;
    }

    public TextDBColumn(String name, int maxLength)
    {
        super(name);
        mMaxLength = maxLength;
        mDefault = null;
    }

    public TextDBColumn(String name, int maxLength, String defaultVal)
    {
        super(name);
        mMaxLength = maxLength;
        mHasDefault = true;
        mDefault = defaultVal;
    }

    @Override
    protected String getTypeString()
    {
        // We have to specify a maximum length.
        // See http://hsqldb.org/doc/guide/sqlgeneral-chapt.html#sgc_data_type_guide
        return "VARCHAR(" + mMaxLength.toString() + ")";
    }

    @Override
    protected void appendDefault(StringBuilder sb)
    {
        // TODO Fix properly - this was copied from our Android code.
        // sb.append(DatabaseUtils.sqlEscapeString(mDefault));
        sb.append("\"" + mDefault + "\"");
    }
}
