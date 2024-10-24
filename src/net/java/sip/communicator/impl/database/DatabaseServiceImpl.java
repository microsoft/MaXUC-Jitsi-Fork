// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.database;

import java.io.*;
import java.sql.*;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hsqldb.jdbc.*;
import org.jitsi.service.fileaccess.*;
import org.jitsi.util.RuntimeUtils;

import net.java.sip.communicator.impl.database.migration.*;
import net.java.sip.communicator.service.database.*;
import net.java.sip.communicator.service.database.schema.*;
import net.java.sip.communicator.service.database.util.*;
import net.java.sip.communicator.util.*;

/**
 * This service makes the HSQLDB (2.3.3) available for other services to use.
 *
 * This implementation only supports ONE DATABASE, but multiple connections to
 * that database.
 */
public class DatabaseServiceImpl implements DatabaseService
{
    /**
     * The logger for this class.
     */
    private static final Logger sLog = Logger.getLogger(DatabaseServiceImpl.class);

    /**
     * The maximum number of simultaneous database connections to allow.
     * This is an arbitrary value, much larger than we expect to need.
     */
    private static final int MAX_CONNECTIONS = 50;

    /**
     * The directory in which to store the database (only one database is
     * supported).
     */
    private static final String DATABASE_DIRECTORY = "database";

    /**
     * A pool to allocate JDBC connections from. Must only be accessed whilst holding connectionPoolLock.
     */
    private JDBCPool mConnectionPool;
    private final Object connectionPoolLock = new Object();

    public DatabaseServiceImpl(FileAccessService fileAccessService)
            throws Exception
    {
        // Log entry and exit because we need to know if this takes too long.
        sLog.info("DatabaseService: initialization start");

        // All database files live in the home directory under "database".
        File databaseDir = fileAccessService
            .getPrivatePersistentActiveUserDirectory(DATABASE_DIRECTORY);
        sLog.info("Database location: subscriber home directory under 'database'");

        printDatabaseLogs(databaseDir);

        String databaseUrl = "jdbc:hsqldb:file:" + databaseDir +
            File.separator +
            "database";   // Prefix for all files in DATABASE_DIRECTORY.

        // Stop HSQLDB reconfiguring our logging - this would remove special
        // log handlers/formatters we create etc.
        SystemUtils.setProperty("hsqldb.reconfig_logging", "false");

        // Register the HSQLDB driver.
        Class.forName("org.hsqldb.jdbc.JDBCDriver");

        // Create a connection pool, this allows multiple connections.
        synchronized (connectionPoolLock)
        {
            mConnectionPool = new JDBCPool(MAX_CONNECTIONS);
            mConnectionPool.setUrl(databaseUrl);
            mConnectionPool.setUser("SA");
            mConnectionPool.setPassword("");
            mConnectionPool.setDatabase(databaseUrl);
        }

        upgradeDatabaseToLatestVersion(fileAccessService);

        // We should have the DB shutdown as part of the Felix bundle shutdown processing. However,
        // sometimes that doesn't fire, and as closing the DB safely is very important we go for the
        // belt and braces approach of adding our own shutdown hook here.
        RuntimeUtils.addShutdownHook(
            new Thread("DatabaseServiceImpl-ShutdownHook")
            {
                public void run()
                {
                    sLog.info("Running shutdown hook for DatabaseServiceImpl");
                    shutdownImmediately();
                }
            });

        sLog.info("DatabaseService: initialization complete");
    }

    /**
     * Print some useful logs about the state of the database before we attempt to open it.
     * This should prove useful if it turns out the database is in a bad state from last time.
     * @param databaseDirectory
     */
    private void printDatabaseLogs(File databaseDirectory)
    {
        // First just log a list of files in the directory.
        if (databaseDirectory != null)
        {
            sLog.info("Database files at startup: " +
                Stream.of(databaseDirectory.listFiles()).map(File::getName).collect(Collectors.joining (",")));
        }

        // Log out the contents of database.properties if it exists - it contains info on the state of the DB
        File propertiesFile = new File(databaseDirectory, "database.properties");
        try (FileInputStream inputStream = new FileInputStream(propertiesFile)) // CodeQL [SM00697] Not Exploitable. The file/path is not user provided.
        {
            Properties databaseProperties = new Properties();
            databaseProperties.load(inputStream);
            sLog.info("database.properties file: " + databaseProperties.toString());
        }
        catch (FileNotFoundException e)
        {
            sLog.warn("No database.properties file found");
        }
        catch (IOException e)
        {
            sLog.warn("IOException reading database.properties file: " + e);
        }
    }

    /**
     * Connect to database.  Once connected, the caller can use the Connection
     * object to issue SQL statements.
     *
     * @throws SQLException on SQL error.
     */
    public DatabaseConnection connect()
        throws SQLException
    {
        synchronized (connectionPoolLock)
        {
            return new DatabaseConnectionImpl(mConnectionPool.getConnection());
        }
    }

    /**
     * Shutdown the database.
     */
    public void shutdown()
    {
        executeShutdownCommand("SHUTDOWN");
    }

    /**
     * Shutdown the database immediately.
     */
    void shutdownImmediately()
    {
        executeShutdownCommand("SHUTDOWN IMMEDIATELY");
    }

    /**
     * There are several options to the SHUTDOWN command documented at
     * http://hsqldb.org/doc/guide/management-chapt.html. It is recommended to use just "SHUTDOWN"
     * and we have seen database corruption previously with "SHUTDOWN COMPACT".  Our suspicion
     * (never proved) was that shutting down with compaction took too long and Accession got killed
     * with the database files open part way through compacting them. We also have seen occasional
     * DB corruption with SHUTDOWN, and again the suspicion is that when the DB is closed through
     * Java shutdown hooks, or Felix bundle stop() methods (which rely on Java shutdown hooks), Java
     * doesn't give us enough time to finish the SHUTDOWN, leaving a corrupt database. Therefore we
     * take the belts-and-braces approach of an explicit call (from AbstractUIServiceImpl) to
     * SHUTDOWN before Java begins shutting down), but as a backup we try a SHUTDOWN IMMEDIATELY if
     * we bypass that codepath (as is the case when e.g. the user just shuts down their PC without
     * quitting the client).
     *
     * @param shutdownCommand Which HSQLDB shutdown command to run
     */
    private void executeShutdownCommand(String shutdownCommand)
    {
        sLog.info("DatabaseService: shutdown start with command " + shutdownCommand);

        synchronized (connectionPoolLock)
        {
            if (mConnectionPool != null)
            {
                DatabaseConnection connection = null;
                try
                {
                    sLog.info("Shutdown connection pool " + mConnectionPool);
                    connection = connect();
                    sLog.info("Shutdown connection " + connection);
                    connection.execute(shutdownCommand);
                    sLog.info("Executed " + shutdownCommand);
                }
                catch (Exception e)
                {
                    sLog.error("Failed to shutdown database: ", e);
                }
                finally
                {
                    mConnectionPool = null;
                    DatabaseUtils.safeClose(connection);
                    sLog.info("safeClose complete");
                }
            }
        }

        sLog.info("DatabaseService: shutdown complete");
    }

    /**
     * Handle all database upgrade and migration from previous versions,
     * including a clean install with no previous version.
     * - Get the current database version number.  If this fails:
     *   - Assume the database does not exist and create it using the version 1
     *     schema.
     *   - If there is any existing XML history from the HistoryService,
     *     migrate that into the version 1 database.
     * - Run any database upgrade code to upgrade to the latest database
     *   version.
     * @param fileAccessService Reference to the FileAccessService.
     * @throws SQLException on SQL error.
     */
    private void upgradeDatabaseToLatestVersion(FileAccessService fileAccessService)
        throws SQLException
    {
        ResultSet resultSet = null;
        DatabaseConnection connection = connect();

        int oldVersion = 0;

        // Get current version number.
        try
        {
            resultSet = connection.query("SELECT " +
                VersionTable.COL_VERSION + " FROM " + VersionTable.NAME);

            if (resultSet.next())
            {
                oldVersion = resultSet.getInt(VersionTable.COL_VERSION);
                sLog.info("Found current database version: " + oldVersion +
                    ", latest version is: " + DatabaseSchema.VERSION);
            }
            else
            {
                sLog.info("No entry in the version table!");
            }
        }
        catch (SQLException e)
        {
            sLog.info("Database version not found, assume database does not exist");
        }

        try
        {
            if (oldVersion == 0)
            {
                // The database does not exist, OR we previously failed
                // part-way through creating it.  Try deleting it first.
                dropDatabase(connection);

                // Now create it and migrate any existing XML records from the
                // old HistoryService into the database.
                //
                // Do all of this as a single transaction - if anything fails
                // here, such as a crash or premature termination, we don't
                // want to have to try and recover next time we start up.
                // Better to just retry everything next time.
                connection.startTransaction();

                createDatabaseVersion1(connection);

                new DatabaseMigrationCallHistory(fileAccessService, connection);
                new DatabaseMigrationFileHistory(fileAccessService, connection);
                new DatabaseMigrationMessageHistory(fileAccessService, connection);
                new DatabaseMigrationGroupMessageHistory(fileAccessService, connection);

                setDatabaseVersion(connection, DatabaseSchema.VERSION);

                connection.commitTransaction();
            }

            // Upgrade the database to the latest version.
            if (oldVersion < DatabaseSchema.VERSION)
            {
                // Use a transaction so if anything fails, we will be OK to try
                // again the next time we run through here.  We don't want to
                // leave a partially completed upgrade.
                connection.startTransaction();

                if (oldVersion <= 1)
                {
                    upgradeDatabaseFromVersion1ToVersion2(connection);
                }

                if (oldVersion <= 2)
                {
                    upgradeDatabaseFromVersion2ToVersion3(connection);
                }

                if (oldVersion <= 3)
                {
                    upgradeDatabaseFromVersion3ToVersion4(connection);
                }

                if (oldVersion <= 4)
                {
                    upgradeDatabaseFromVersion4ToVersion5(connection);
                }

                // Finished upgrading, so write the new current version number
                // into the database.
                setDatabaseVersion(connection, DatabaseSchema.VERSION);

                connection.commitTransaction();
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to create/migrate/update database.", e);
        }
        catch (Exception e)
        {
            sLog.error("Failed to migrate database.", e);
        }

        DatabaseUtils.safeClose(connection, resultSet);
    }

    /**
     * The database is ALWAYS created here, regardless of the latest version.
     * Each version update is performed separately afterwards as necessary to
     * bring it up to the latest version.
     * @throws SQLException on SQL error.
     */
    private void createDatabaseVersion1(DatabaseConnection connection)
        throws SQLException
    {
        sLog.info("Create database at version 1");

        connection.execute(CallHistoryTable.TABLE.getCreateString());
        connection.execute(CallHistoryTable.INDEX_CALL_START.getCreateString());
        connection.execute(CallHistoryTable.INDEX_CALL_END.getCreateString());

        connection.execute(FileHistoryTable.TABLE.getCreateString());
        connection.execute(FileHistoryTable.INDEX_JIDS.getCreateString());
        connection.execute(FileHistoryTable.INDEX_JIDS_AND_DATE.getCreateString());
        connection.execute(FileHistoryTable.INDEX_LOCAL_JID_AND_FT_ID.getCreateString());

        connection.execute(MessageHistoryTable.TABLE.getCreateString());
        connection.execute(MessageHistoryTable.INDEX_JIDS.getCreateString());
        connection.execute(MessageHistoryTable.INDEX_JIDS_AND_DATE.getCreateString());
        connection.execute(MessageHistoryTable.INDEX_JIDS_AND_MSG_ID.getCreateString());
        connection.execute(MessageHistoryTable.INDEX_JIDS_AND_SMPP_ID.getCreateString());

        connection.execute(GroupMessageHistoryTable.TABLE.getCreateString());
        connection.execute(GroupMessageHistoryTable.INDEX_JIDS.getCreateString());
        connection.execute(GroupMessageHistoryTable.INDEX_JIDS_AND_DATE.getCreateString());
        connection.execute(GroupMessageHistoryTable.INDEX_JIDS_AND_MSG_ID.getCreateString());

        connection.execute(VersionTable.TABLE.getCreateString());
    }

    private void dropDatabase(DatabaseConnection connection)
        throws SQLException
    {
        sLog.info("Drop all database tables and indices");
        connection.execute(VersionTable.TABLE.getDropString());

        connection.execute(GroupMessageHistoryTable.INDEX_JIDS.getDropString());
        connection.execute(GroupMessageHistoryTable.INDEX_JIDS_AND_DATE.getDropString());
        connection.execute(GroupMessageHistoryTable.INDEX_JIDS_AND_MSG_ID.getDropString());
        connection.execute(GroupMessageHistoryTable.TABLE.getDropString());

        connection.execute(MessageHistoryTable.TABLE.getDropString());
        connection.execute(MessageHistoryTable.INDEX_JIDS.getDropString());
        connection.execute(MessageHistoryTable.INDEX_JIDS_AND_DATE.getDropString());
        connection.execute(MessageHistoryTable.INDEX_JIDS_AND_MSG_ID.getDropString());
        connection.execute(MessageHistoryTable.INDEX_JIDS_AND_SMPP_ID.getDropString());

        connection.execute(FileHistoryTable.TABLE.getDropString());
        connection.execute(FileHistoryTable.INDEX_JIDS.getDropString());
        connection.execute(FileHistoryTable.INDEX_JIDS_AND_DATE.getDropString());
        connection.execute(FileHistoryTable.INDEX_LOCAL_JID_AND_FT_ID.getDropString());

        connection.execute(CallHistoryTable.TABLE.getDropString());
        connection.execute(CallHistoryTable.INDEX_CALL_START.getDropString());
        connection.execute(CallHistoryTable.INDEX_CALL_END.getDropString());
    }

    /**
     * The caller should always call this as part of a transaction, together
     * with whatever database changes are made as part of the new version.
     * @param connection The DatabaseConnection.
     * @param newVersion The new database version.
     * @throws SQLException on SQL error.
     */
    private void setDatabaseVersion(DatabaseConnection connection,
                                    int newVersion)
        throws SQLException
    {
        // Use a transaction because executing only the first would leave the
        // database in a bad state.
        sLog.info("Set database version to: " + newVersion);
        connection.execute("DELETE FROM " + VersionTable.NAME);
        connection.execute("INSERT INTO " + VersionTable.NAME +
            " VALUES(" + newVersion + ")");
    }

    /**
     * Upgrade DB from v1 to v2.  Add CallHistoryTable.COL_CALL_ADDED_TO_DB column to CallHistory table.
     * @throws SQLException
     */
    private void upgradeDatabaseFromVersion1ToVersion2(DatabaseConnection connection) throws SQLException
    {
         sLog.info("upgradeDatabaseFromVersion1ToVersion2");

         // We want to add a 'not null' column to an existing table.  The easiest way to do that is in three stages.
         // 1. Add a nullable column
         // 2. Populate the column for all existing rows in the table.
         // 3. Make the column 'not null'
         String command = "ALTER TABLE " + CallHistoryTable.NAME + " ADD " + CallHistoryTable.COL_CALL_ADDED_TO_DB + " BIGINT";
         sLog.info("2.1 Executing " + command);
         connection.execute(command);

         // The new column is a timestamp for when the call log was added to the database.  It's used for correlating new local call logs
         // with new server call logs - that won't happen for any call logs in the DB at the point we migrate the DB so we can choose any
         // value for this column for existing rows.  The 'call start' column is a sensible value.
         command =  "UPDATE " + CallHistoryTable.NAME + " SET " + CallHistoryTable.COL_CALL_ADDED_TO_DB + " = " + CallHistoryTable.COL_CALL_START;
         sLog.info("2.2 Executing " + command);
         connection.execute(command);

         // Make the new column 'not null'.
         command = "ALTER TABLE " + CallHistoryTable.NAME + " ALTER COLUMN " + CallHistoryTable.COL_CALL_ADDED_TO_DB + " SET NOT NULL";
         sLog.info("2.3 Executing " + command);
         connection.execute(command);
    }

    /**
     * Version 3 (part of the UI refresh) added a new column to the call history
     * to track which calls need attending to (so that the state survives client
     * restart).
     */
    private void upgradeDatabaseFromVersion2ToVersion3(DatabaseConnection connection)
        throws SQLException
    {
        sLog.info("upgradeDatabaseFromVersion2ToVersion3");
        String command = "ALTER TABLE " + CallHistoryTable.NAME + " ADD " + CallHistoryTable.COL_CALL_RECORD_ATTENTION + " BOOLEAN";
        sLog.info("3.1 Executing " + command);
        connection.execute(command);
    }

    /**
     * Version 4 (part of the UI refresh) added a new column to the file history to track
     * which file transfer need attending to (so that the state survives client restart).
     */
    private void upgradeDatabaseFromVersion3ToVersion4(DatabaseConnection connection)
        throws SQLException
    {
        sLog.info("upgradeDatabaseFromVersion3ToVersion4");
        // Add attention column
        String command = "ALTER TABLE " + FileHistoryTable.NAME + " ADD " + FileHistoryTable.COL_ATTENTION + " BOOLEAN";
        sLog.info("4.1 Executing " + command);
        connection.execute(command);
    }

    /**
     * This is a sample method for the next time we need to change the database schema.
     */
    private void upgradeDatabaseFromVersion4ToVersion5(DatabaseConnection connection)
    {
        // sLog.info("upgradeDatabaseFromVersion4ToVersion5");
        // This code would typically do something like add a new column to a
        // table, or add a new table.
    }
}
