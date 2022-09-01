// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.database.schema;

import net.java.sip.communicator.service.database.schemautil.*;

/**
 * The database version table.  Contains a single row with the current version
 * value.  Used only to detect a change of version on upgrade.
 */
public final class VersionTable
{
    public static final String NAME = "VersionTable";

    // Column name.
    public static final String COL_VERSION = "version";

    public static final DBTable TABLE = new DBTable(NAME, DBTable.STORAGE.MEMORY)
        .addColumn(new IntDBColumn(COL_VERSION).notNull())
       ;
}
