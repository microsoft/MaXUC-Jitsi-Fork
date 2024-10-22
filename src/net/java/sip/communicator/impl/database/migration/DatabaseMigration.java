// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.database.migration;

import static net.java.sip.communicator.util.PrivacyUtils.sanitiseChatAddress;
import static net.java.sip.communicator.util.PrivacyUtils.sanitiseFilePath;

import java.io.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.Date;

import javax.xml.parsers.*;

import org.jitsi.service.fileaccess.*;
import org.w3c.dom.*;

import net.java.sip.communicator.impl.database.*;
import net.java.sip.communicator.service.database.*;
import net.java.sip.communicator.service.diagnostics.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.ServiceUtils.*;

/**
 * Implements one-off migration of all stored data from the obsolete
 * HistoryService (XML) to the DatabaseService.
 *
 * All code to perform the migration is isolated here.  It is only executed
 * once, when upgrading from a backlevel version that does not have
 * database support.
 *
 * The intention is that at some future point when all installed clients have
 * been upgraded to include database support, this class can be removed.
 *
 * Much of the code here was taken from the retired HistoryService.  This goal
 * here was NOT to write maintainable, reusable code, but rather to pull
 * together bits of existing code to do the job of reading the old XML files.
 *
 * The old history records are NOT DELETED after migration.  It is just not
 * worth the risk of losing them in case anything goes wrong with the migration
 * process, at the expense of wasting a small amount of disk space indefinitely.
 */
public abstract class DatabaseMigration
{
    /**
     * The logger for this class.
     */
    private static final Logger sLog = Logger.getLogger(DatabaseMigration.class);

    /**
     * The directory containing the old XML history files.
     */
    private static final String DATA_DIRECTORY = "history_ver1.0";

    /**
     * The data file.
     */
    private static final String DATA_FILE = "dbstruct.dat";

    /**
     * We onl want to read files with this suffix.
     */
    private static final String SUPPORTED_FILETYPE = "xml";

    /**
     * Don't know where these subdirectories come from, but we want to ignore
     * them.
     */
    private static final String ACCESSION_UID = "client_contact_id:";

    /**
     * The subdirectory for each IM account begins with this prefix.
     */
    private static final String JABBER_PREFIX = "Jabber_";

    /**
     * The subdirectory for each Group Chat begins with this prefix.36++639
     */
    protected static final String CHATROOM_PREFIX = "chatroom-";

    /**
     * Some fields in the XML files are comma separated and we need to split
     * them in order to parse them before putting in the database.
     */
    private static final String CSV_DELIM = ",";

    /**
     * Date format used in the XML history database.
     */
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    /**
     * The DatabaseConnection to use for all updates.
     */
    protected final DatabaseConnection mConnection;

    /**
     * We only want to build the prepared statement once.
     */
    protected PreparedStatement mPreparedStatement;

    protected File mHistoryDir = null;

    private DocumentBuilder mBuilder = null;

    private SimpleDateFormat mSdf = null;

    /**
     * Track how successful the conversion is.
     */
    private int mFilesAttempted = 0;
    private int mFilesFailed = 0;
    private int mRecordsAttempted = 0;
    private int mRecordsFailed = 0;

    /**
     * This is the only publicly available method.  The calling code creates
     * this once to migrate the XML files into the database then destroys it.
     * @param connection The DatabaseConnection.
     * @throws SQLException on any SQL error.
     */
    public DatabaseMigration(FileAccessService fileAccessService,
                             DatabaseConnection connection)
    {
        // Log entry and exit because we need to know if this takes too long.
        sLog.info("DatabaseMigration: start");

        mConnection = connection;
        boolean initialized = false;

        try
        {
            // All history files live in the home directory under here.
            mHistoryDir = fileAccessService
                .getPrivatePersistentActiveUserDirectory(DATA_DIRECTORY);

            mBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            mSdf = new SimpleDateFormat(DATE_FORMAT);

            buildPreparedStatement();

            initialized = true;
        }
        catch (SQLException e)
        {
            sLog.error("SQL exception in initialization: ", e);
        }
        catch (IOException e)
        {
            sLog.error("IO exception in initialization: ", e);
        }
        catch (Exception e)
        {
            sLog.error("Exception in initialization: ", e);
        }

        if (initialized)
        {
            populateDatabaseFromOldXmlFiles();

            sLog.info("Processed " + mFilesAttempted + " files (" +
                mFilesFailed + " failures), processed " +
                mRecordsAttempted + " records (" +
                mRecordsFailed + " failures)");
        }

        // If we failed to convert any records then send an automatic error
        // report.  We really, really don't expect this, and it is essential
        // to get diagnostics to understand what happened.
        if ((!initialized) || (mFilesFailed != 0) || (mRecordsFailed != 0))
        {
            raiseErrorReport();
        }

        sLog.info("DatabaseMigration: complete");
    }

    /**
     * Build the prepared statement once only.
     */
    protected abstract void buildPreparedStatement()
        throws SQLException;

    protected abstract void populateDatabaseFromOldXmlFiles();

    /**
     * Migrate all records from the specified XML file into the database.
     * @param xmlFile The XML file to process.
     * @param localJid The local JID, if relevant.
     * @param remoteJid The remote JID, if relevant.
     */
    protected void processXmlFile(File xmlFile, String localJid, String remoteJid)
    {
        mFilesAttempted++;

        try
        {
            Document doc = mBuilder.parse(xmlFile.getPath());
            NodeList nodes = doc.getElementsByTagName("record");
            sLog.info("Migrate " + nodes.getLength() + " records from: " +
                sanitiseFilePath(xmlFile.toString()) + ", " +
                sanitiseChatAddress(localJid) + ", " + sanitiseChatAddress(remoteJid));

            for (int i = 0; i < nodes.getLength(); i++)
            {
                // Get all the properties.  These are all strings, which may
                // not be what we want for the database.  Conversation follows.
                // Note we don't read the record timestamp because we don't
                // need it.
                mRecordsAttempted++;

                try
                {
                    processXmlRecord((Element) nodes.item(i), localJid, remoteJid);
                }
                catch (SQLException e)
                {
                    // Catch a failure in processing a single record, and make
                    // a best effort to continue processing.
                    mRecordsFailed++;
                    sLog.error("Failed to process an XML record: ", e);
                }
            }
        }
        catch (Exception e)
        {
            mFilesFailed++;
            sLog.error("Failed to process an XML file: ", e);
        }
    }

    /**
     * Read one XML record and write it to the database.
     * @param node The XML node to process.
     * @param localJid The local JID, if relevant.
     * @param remoteJid The remote JID, if relevant.
     */
    protected abstract void processXmlRecord(Element node,
                                             String localJid,
                                             String remoteJid)
        throws SQLException;

    /**
     * Return a list of XML files under the specified directory.  Filter
     * out XML files that we don't want.
     * @param histDir Directory to start from.
     * @return List of subdirectories.
     */
    protected List<File> getXmlFilesToProcess(File histDir)
    {
        String sanitisedHistDir = sanitiseFilePath(histDir.getPath());

        sLog.info("From: " + sanitisedHistDir);

        List<File> listOfXmlFiles = new ArrayList<>();

        // The directory won't exist if none of that type of history record
        // have ever been created.
        if (histDir.exists()) // CodeQL [SM00697] Not Exploitable. The file/path is not user provided.
        {
            // First get a list of all directories which contain a dat file.
            List<File> listOfDirs = new ArrayList<>();
            recursiveFindDatDirectories(listOfDirs, histDir);

            // Now get the list of XML files in those directories.
            for (File directory : listOfDirs)
            {
                for (File file : directory.listFiles())
                {
                    if (!file.isDirectory())
                    {
                        String filename = file.getName();

                        if (filename.endsWith(SUPPORTED_FILETYPE))
                        {
                            sLog.info("Found xml file: " + sanitiseFilePath(directory.getPath()) + ", " + filename);
                            listOfXmlFiles.add(file);
                        }
                    }
                }
            }
        }
        else
        {
            sLog.info("Directory does not exist");
        }

        return listOfXmlFiles;
    }

    /**
     * Recursive function: if the specified directory contains a dat file,
     * and the directory to the list, other call again for each subdirectory
     * in the specified directory.
     * @param list Add the specified directory if it contains a dat file.
     * @param directory The directory to check.
     */
    private void recursiveFindDatDirectories(List<File> list, File directory)
    {
        for (File file : directory.listFiles())
        {
            if (file.isDirectory())
            {
                // Not sure where these directories come from, but we don't
                // want them.
                if (!file.getName().startsWith(ACCESSION_UID))
                {
                    recursiveFindDatDirectories(list, file);
                }
            }
            else if (DATA_FILE.equalsIgnoreCase(file.getName()))
            {
                list.add(directory);
            }
        }
    }

    /**
     * Take a directory name and extract the local JID from it.
     * @param directoryName
     * @return The local JID.
     */
    protected String getLocalJidFromDirectoryName(String directoryName)
    {
        String localJid = null;

        // We expect a directoryName of this form:
        // Jabber_2345553913@AMS-DOMAIN.COM@AMS-DOMAIN.COM$4d436c20
        if (directoryName.startsWith(JABBER_PREFIX))
        {
            String tempString = directoryName.substring(JABBER_PREFIX.length());
            tempString = tempString.split("$")[0];

            String[] splitParts = tempString.split("@");

            if (splitParts.length >= 2)
            {
                tempString = splitParts[0] + "@" + splitParts[1];

                // We expect lower case but need to cope with old bugs where it
                // might not have been.
                localJid = tempString.toLowerCase();
            }
        }

        return localJid;
    }

    /**
     * Convert a timestamp string from the database (expected to be in format
     * DATE_FORMAT) into an integer epoch time.
     * @param databaseTimestamp
     * @return An integer epoch timestamp.
     */
    protected long xmlTimestampToDatabaseTimestamp(String databaseTimestamp)
    {
        Date date;
        try
        {
            date = mSdf.parse(databaseTimestamp);
        }
        catch (ParseException e)
        {
            date = new Date(Long.parseLong(databaseTimestamp));
        }
        return date.getTime();
    }

    /**
     * Convert a CSV list of timestamp strings from the database (each expected
     * to be in format DATE_FORMAT) into a list of epoch times.
     * @param databaseTimestamps
     * @return A CSV list of integer epoch times, as a string.
     */
    protected String xmlTimestampsToDatabaseTimestamps(String databaseTimestamps)
    {
        boolean first = true;
        StringBuilder sb = new StringBuilder();

        if (databaseTimestamps == null)
        {
            return "";
        }

        StringTokenizer toks = new StringTokenizer(databaseTimestamps, CSV_DELIM);
        while(toks.hasMoreTokens())
        {
            if (!first)
            {
                sb.append(CSV_DELIM);
            }
            sb.append(xmlTimestampToDatabaseTimestamp(toks.nextToken()));
            first = false;
        }
        return sb.toString();
    }

    /**
     * Get the value for an XML element, or an empty string if the element is absent.
     * @param element The XML element.
     * @param tagName The name of the tag.
     * @return The element value, or an empty string if not found.
     */
    protected String valueFromElement(Element element, String tagName)
    {
        NodeList nodeList = element.getElementsByTagName(tagName);
        String value = (nodeList.getLength() == 0) ?
            "" : nodeList.item(0).getTextContent();
        // sLog.debug(String.format("    %32s: %s", tagName, value));
        return value;
    }

    /**
     * Generate an automatic error report for any unexpected and unhandled
     * error in the database migration process.
     */
    private void raiseErrorReport()
    {
        ServiceUtils.getService(DatabaseActivator.getBundleContext(),
            DiagnosticsService.class,
            new ServiceCallback<DiagnosticsService>()
        {
            @Override
            public void onServiceRegistered(DiagnosticsService service)
            {
                sLog.info("Got reference to the diagnostics service");
                service.openErrorReportFrame(ReportReason.DATABASE_MIGRATION_ERROR);
            }
        });
    }

    /**
     * Truncate a string if necessary for storing in the database.
     * @param inputString The input string.
     * @param maxLen The maximum allowed length.
     * @return If the string exceeded maxLen, the input truncated to maxLen,
     *         otherwise the input unchanged.
     */
    protected String truncateString(String inputString, int maxLen)
    {
        String outputString = inputString;

        if (inputString.length() > maxLen)
        {
            // We can't log the string content because it might be private,
            // such as the content of an IM.
            sLog.info("Truncated string length from " + inputString.length() +
                " to " + maxLen);
            outputString = inputString.substring(0, maxLen);
        }

        return outputString;
    }
}