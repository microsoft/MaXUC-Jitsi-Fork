// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.database.schemautil;

/**
 * A foreign key.
 * <p>
 * At the moment this is always "ON DELETE CASCADE", so when the referenced row
 * is deleted the row in this table is deleted
 */
public class ForeignKey extends IntDBColumn
{
    private String mReferencedTable;

    /**
     * Constructor
     * @param name - name of the column
     * @param referencedTable - table that the key references
     */
    public ForeignKey(String name, String referencedTable)
    {
        super(name);
        mReferencedTable = referencedTable;
    }

    @Override
    protected void qualify(StringBuilder sb)
    {
        super.qualify(sb);
        sb.append(" REFERENCES ")
          .append(mReferencedTable)
          .append(" ON DELETE CASCADE");
    }
}
