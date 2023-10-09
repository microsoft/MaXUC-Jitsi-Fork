// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.database.schemautil;

import java.util.*;

import net.java.sip.communicator.util.*;

/**
 * Class representing a database table.
 * <p>
 * This class allows the caller to declare a database and the columns associated
 * with it.  At the moment the only use is to build the string used to create
 * the table.
 */
public class DBTable
{
    /**
     * See http://hsqldb.org/doc/2.0/guide/sqlgeneral-chapt.html
     * For both types, the database is always persisted to disk.
     */
    public enum STORAGE
    {
        /**
         * A MEMORY table is stored entirely in memory.  It is read from disk
         * into memory on startup.  Recommended for small tables.
         */
        MEMORY,

        /**
         * A CACHED table is partly read from disk into memory as needed.
         * Recommended for larger tables where startup time is faster and
         * occupancy lower.
         */
        CACHED
    }

    private static final Logger sLog = Logger.getLogger(DBTable.class);
    private String mName;
    private STORAGE mStorage;
    private Hashtable<String, DBColumn> mColumns = new Hashtable<>();
    private ArrayList<TableConstraint> mTableConstraints = new ArrayList<>();

    /**
     * Constructor
     * @param name - the name of the database table
     * @param storage - how to store the database while running
     */
    public DBTable(String name, STORAGE storage)
    {
        mName = name;
        mStorage = storage;
    }

    /**
     * Add a column to the table.
     * @param column A DBColumn to add to the table
     * @return this A self-reference so columns can be added in a declarative
     * style - e.g. new DBTable("fred").addColumn(new IntDBColumn("brian"))
     * .addColumn(new IntDBColumn("george");
     */
    public DBTable addColumn(DBColumn column)
    {
        sLog.debug("Adding column: " + column);
        mColumns.put(column.mName, column);
        return this;
    }

    /**
     * Get a string suitable for submitting to SQL to create the table
     * @return A string containing the create table command.
     */
    public String getCreateString()
    {
        sLog.debug("Building create string");
        StringBuilder sb = new StringBuilder();

        sb.append("CREATE ");

        // MEMORY is the default type.
        if (mStorage == STORAGE.CACHED)
        {
            sb.append("CACHED ");
        }
        sb.append("TABLE ");
        sb.append(mName);
        sb.append(" (");

        /*
         * Get the strings for declaring each column
         */
        Enumeration<DBColumn> cols = mColumns.elements();
        if (cols.hasMoreElements())
        {
            while (true)
            {
                DBColumn col = cols.nextElement();

                col.appendCreateString(sb);

                if (cols.hasMoreElements())
                {
                    sb.append(",");
                }
                else
                {
                   break;
                }
            }
        }

        int size = mTableConstraints.size();
        for (int index = 0; index < size; index++)
        {
            sb.append(",");
            TableConstraint constraint = mTableConstraints.get(index);
            constraint.appendCreateString(sb);
        }

        sb.append(");");

        String createTableSql = sb.toString();
        sLog.debug(createTableSql);

        return createTableSql;
    }

    /**
     * @return An SQL command to drop the table
     */
    public String getDropString()
    {
        return "DROP TABLE IF EXISTS " + mName;
    }

    /**
     * @return database name.
     */
    public String getName()
    {
       return mName;
    }
}
