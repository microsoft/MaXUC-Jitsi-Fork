// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.database;

import java.sql.*;

/**
 * This service makes the HSQLDB (2.3.3) available for other services to use.
 */
public interface DatabaseService
{
    /**
     * Connect to database.  Once connected, the caller can use the Connection
     * object to issue SQL statements.
     *
     * @throws SQLException on SQL error.
     */
    DatabaseConnection connect()
        throws SQLException;

    /**
     * Shutdown the database.
     */
    void shutdown();
}
