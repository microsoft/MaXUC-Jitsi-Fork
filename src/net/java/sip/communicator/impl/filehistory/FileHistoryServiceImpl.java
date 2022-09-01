/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.filehistory;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;

import org.osgi.framework.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.database.*;
import net.java.sip.communicator.service.database.schema.*;
import net.java.sip.communicator.service.database.util.*;
import net.java.sip.communicator.service.filehistory.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

/**
 * File History Service stores info for file transfers from various protocols.
 * Uses the Database Service to store data.
 *
 * @author Damian Minkov
 */
public class FileHistoryServiceImpl
    implements  FileHistoryService,
                ServiceListener,
                FileTransferStatusListener,
                FileTransferListener
{
    /**
     * The logger for this class.
     */
    private static final Logger sLog =
        Logger.getLogger(FileHistoryServiceImpl.class);

    /**
     * The BundleContext that we got from the OSGI bus.
     */
    private BundleContext mBundleContext = null;

    /**
     * The <tt>DatabaseService</tt> reference.
     */
    private final DatabaseService mDatabaseService;

    /**
     * The IM ProtocolProviderService.
     */
    private ProtocolProviderService mImProvider;

    public FileHistoryServiceImpl(DatabaseService databaseService)
    {
        mDatabaseService = databaseService;
    }

    /**
     * Starts the service. Check the current registered protocol providers
     * which supports FileTransfer and adds a listener to them.
     *
     * @param bundleContext BundleContext
     */
    public void start(BundleContext bundleContext)
    {
        sLog.debug("Starting the file history implementation.");

        mBundleContext = bundleContext;

        // Start listening for newly register or removed protocol providers.
        bundleContext.addServiceListener(this);

        ServiceReference<?>[] protocolProviderRefs = null;
        try
        {
            protocolProviderRefs = bundleContext.getServiceReferences(
                ProtocolProviderService.class.getName(),
                null);
        }
        catch (InvalidSyntaxException ex)
        {
            // This shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            sLog.error("Error while retrieving service refs", ex);
        }

        // In case we found any.
        if (protocolProviderRefs != null)
        {
            sLog.debug("Found "
                     + protocolProviderRefs.length
                     + " already installed providers.");
            for (int i = 0; i < protocolProviderRefs.length; i++)
            {
                ProtocolProviderService provider = (ProtocolProviderService) bundleContext
                    .getService(protocolProviderRefs[i]);

                handleProviderAdded(provider);
            }
        }

        // Tidy up previously active file transfers
        cleanUpActiveTransfers();
    }

    /**
     * Stops the service.
     *
     * @param bc BundleContext
     */
    public void stop(BundleContext bc)
    {
        bc.removeServiceListener(this);

        ServiceReference<?>[] protocolProviderRefs = null;
        try
        {
            protocolProviderRefs = bc.getServiceReferences(
                ProtocolProviderService.class.getName(),
                null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            sLog.error("Error while retrieving service refs", ex);
        }

        // in case we found any
        if (protocolProviderRefs != null)
        {
            for (int i = 0; i < protocolProviderRefs.length; i++)
            {
                ProtocolProviderService provider = (ProtocolProviderService) bc
                    .getService(protocolProviderRefs[i]);

                handleProviderRemoved(provider);
            }
        }
    }

    /**
     * Used to attach the File History Service to existing or
     * just registered protocol provider. Checks if the provider has implementation
     * of OperationSetFileTransfer
     *
     * @param provider ProtocolProviderService
     */
    private void handleProviderAdded(ProtocolProviderService provider)
    {
        sLog.debug("Adding protocol provider " + provider.getProtocolName());

        // check whether the provider has a file transfer operation set
        OperationSetFileTransfer opSetFileTransfer
            = provider.getOperationSet(OperationSetFileTransfer.class);

        if (opSetFileTransfer != null)
        {
            opSetFileTransfer.addFileTransferListener(this);
        }
        else
        {
            sLog.trace("Service did not have a file transfer op. set.");
        }
    }

    /**
     * Removes the specified provider from the list of currently known providers
     *
     * @param provider the ProtocolProviderService that has been unregistered.
     */
    private void handleProviderRemoved(ProtocolProviderService provider)
    {
        OperationSetFileTransfer opSetFileTransfer
            = provider.getOperationSet(OperationSetFileTransfer.class);

        if (opSetFileTransfer != null)
        {
            opSetFileTransfer.addFileTransferListener(this);
        }
    }

    /**
     * Gets all the JIDs for the contacts in the given MetaContact.  There is
     * probably only one.
     * @param metaContact MetaContact
     * @return HashMap.
     */
    private Map<Contact, String> getJidsForMetaContact(MetaContact metaContact)
    {
        Map<Contact, String> jids = new HashMap<>();

        Iterator<Contact> iter = metaContact.getContacts();
        while (iter.hasNext())
        {
            Contact contact = iter.next();

            // Contacts can be created with arbitrary capitalization, which we
            // must ignore (by converting to lower case), so we store all JIDs
            // in the database in lower case only.
            String remoteJid = contact.getAddress().toLowerCase();
            jids.put(contact, remoteJid);
        }
        sLog.info("Remote JIDs:: " + jids);

        return jids;
    }

    /**
     * Returns all the file transfers made after the given date, in ascending
     * date order.
     *
     * @param metaContact MetaContact the receiver or sender of the file
     * @param startDate Date the start date of the transfers
     * @return Collection of FileRecords
     */
    public Collection<FileRecord> findByStartDate(
            MetaContact metaContact, Date startDate)
    {
        List<FileRecord> result = new ArrayList<>();

        Map<Contact, String> jids = getJidsForMetaContact(metaContact);
        sLog.debug("startDate: " + startDate.getTime() + ", jids: " + jids);

        for (Map.Entry<Contact, String> jidEntry : jids.entrySet())
        {
            Contact contact = jidEntry.getKey();
            DatabaseConnection connection = null;
            ResultSet resultSet = null;

            try
            {
                connection = mDatabaseService.connect();
                resultSet = connection.findAfterDate(
                    FileHistoryTable.NAME,
                    FileHistoryTable.COL_LOCAL_JID,
                    getImAccountJid(),
                    FileHistoryTable.COL_REMOTE_JID,
                    jidEntry.getValue(),
                    FileHistoryTable.COL_DATE,
                    FileHistoryTable.COL_FT_ID,
                    startDate);

                while (resultSet.next())
                {
                    result.add(new FileRecord(resultSet, contact));
                }
            }
            catch (SQLException e)
            {
                sLog.error("Failed in findByStartDate: ", e);
            }
            finally
            {
                DatabaseUtils.safeClose(connection, resultSet);
            }
        }

        sLog.debug("found " + result.size() + " records");
        return result;
    }

    /**
     * Returns all the file transfers made before the given date, in ascending
     * date order.
     *
     * @param metaContact MetaContact the receiver or sender of the file
     * @param endDate Date the end date of the transfers
     * @return Collection of FileRecords
     */
    public Collection<FileRecord> findByEndDate(MetaContact metaContact, Date endDate)
    {
        List<FileRecord> result = new ArrayList<>();

        Map<Contact, String> jids = getJidsForMetaContact(metaContact);
        sLog.debug("endDate: " + endDate.getTime() + ", jids: " + jids);

        for (Map.Entry<Contact, String> jidEntry : jids.entrySet())
        {
            Contact contact = jidEntry.getKey();
            DatabaseConnection connection = null;
            ResultSet resultSet = null;

            try
            {
                connection = mDatabaseService.connect();
                resultSet = connection.findBeforeDate(
                    FileHistoryTable.NAME,
                    FileHistoryTable.COL_LOCAL_JID,
                    getImAccountJid(),
                    FileHistoryTable.COL_REMOTE_JID,
                    jidEntry.getValue(),
                    FileHistoryTable.COL_DATE,
                    FileHistoryTable.COL_FT_ID,
                    endDate);

                while (resultSet.next())
                {
                    result.add(new FileRecord(resultSet, contact));
                }
            }
            catch (SQLException e)
            {
                sLog.error("Failed in findByEndDate: ", e);
            }
            finally
            {
                DatabaseUtils.safeClose(connection, resultSet);
            }
        }

        sLog.debug("found " + result.size() + " records");
        return result;
    }

    /**
     * Returns all the file transfers made between the given dates, in ascending
     * date order.
     *
     * @param metaContact MetaContact the receiver or sender of the file
     * @param startDate Date the start date of the transfers
     * @param endDate Date the end date of the transfers
     * @return Collection of FileRecords
     */
    public Collection<FileRecord> findByPeriod(
            MetaContact metaContact, Date startDate, Date endDate)
    {
        List<FileRecord> result = new ArrayList<>();

        Map<Contact, String> jids = getJidsForMetaContact(metaContact);
        sLog.debug("startDate: " + startDate.getTime() + ", endDate: " +
            endDate.getTime() + ", jids: " + jids);

        for (Map.Entry<Contact, String> jidEntry : jids.entrySet())
        {
            Contact contact = jidEntry.getKey();
            DatabaseConnection connection = null;
            ResultSet resultSet = null;

            try
            {
                connection = mDatabaseService.connect();
                resultSet = connection.findByPeriod(
                    FileHistoryTable.NAME,
                    FileHistoryTable.COL_LOCAL_JID,
                    getImAccountJid(),
                    FileHistoryTable.COL_REMOTE_JID,
                    jidEntry.getValue(),
                    FileHistoryTable.COL_DATE,
                    FileHistoryTable.COL_FT_ID,
                    startDate,
                    endDate);

                while (resultSet.next())
                {
                    result.add(new FileRecord(resultSet, contact));
                }
            }
            catch (SQLException e)
            {
                sLog.error("Failed in findByPeriod: ", e);
            }
            finally
            {
                DatabaseUtils.safeClose(connection, resultSet);
            }
        }

        sLog.debug("found " + result.size() + " records");
        return result;
    }

    /**
     * Returns the supplied number of file transfers, in ascending date order.
     *
     * @param count filetransfer count
     * @return List, with each entry containing a list of FileRecords
     */
    public List<List<FileRecord>> findLastForAll(int count)
    {
        sLog.debug("Finding last " + count + " file transfer messages for each thread");
        String accountJid = getImAccountJid();

        DatabaseConnection conn = null;
        ResultSet rs = null;

        // Get a set of all the thread IDs
        List<String> threadIds = new ArrayList<>();
        try
        {
            conn = mDatabaseService.connect();
            rs = conn.findUniqueColumnValues(
                FileHistoryTable.NAME,
                FileHistoryTable.COL_LOCAL_JID,
                accountJid,
                FileHistoryTable.COL_REMOTE_JID);

            while (rs.next())
            {
                threadIds.add(rs.getString(FileHistoryTable.COL_REMOTE_JID));
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed to read File History: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(conn, rs);
        }
        sLog.debug(threadIds.size() + "file history threads found");

        // For each thread, get the most recent n messages
        List<List<FileRecord>> result = new ArrayList<>();
        Iterator<String> threadIter = threadIds.iterator();
        while (threadIter.hasNext())
        {
            result.add(findLastForJid(threadIter.next(), count));
        }

        return result;
    }

    /**
     * Returns the supplied number of file transfers, in ascending date order.
     *
     * @param jid the receiver or sender of the file
     * @param count filetransfer count
     * @return List of FileRecords
     */
    public List<FileRecord> findLastForJid(String jid, int count)
    {
        List<FileRecord> result = new ArrayList<>();
        sLog.debug("count: " + count);

        DatabaseConnection connection = null;
        ResultSet resultSet = null;

        try
        {
            connection = mDatabaseService.connect();
            resultSet = connection.findLast(
                FileHistoryTable.NAME,
                FileHistoryTable.COL_LOCAL_JID,
                getImAccountJid(),
                FileHistoryTable.COL_REMOTE_JID,
                jid,
                FileHistoryTable.COL_DATE,
                FileHistoryTable.COL_FT_ID,
                count);

            while (resultSet.next())
            {
                MetaContact metaContact = jidToMetaContact(jid);
                Contact contact = metaContact != null ? metaContact.getIMContact() : null;
                result.add(new FileRecord(resultSet, contact));
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed in findLast: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, resultSet);
        }

        sLog.debug("found " + result.size() + " records");
        return result;
    }

    /**
     * Returns the supplied number of file transfers, in ascending date order.
     *
     * @param metaContact MetaContact the receiver or sender of the file
     * @param count filetransfer count
     * @return Collection of FileRecords
     */
    public Collection<FileRecord> findLast(MetaContact metaContact, int count)
    {
        List<FileRecord> result = new ArrayList<>();

        Map<Contact, String> jids = getJidsForMetaContact(metaContact);
        sLog.debug("count: " + count + ", jids: " + jids);

        for (Map.Entry<Contact, String> jidEntry : jids.entrySet())
        {
            Contact contact = jidEntry.getKey();
            DatabaseConnection connection = null;
            ResultSet resultSet = null;

            try
            {
                connection = mDatabaseService.connect();
                resultSet = connection.findLast(
                    FileHistoryTable.NAME,
                    FileHistoryTable.COL_LOCAL_JID,
                    getImAccountJid(),
                    FileHistoryTable.COL_REMOTE_JID,
                    jidEntry.getValue(),
                    FileHistoryTable.COL_DATE,
                    FileHistoryTable.COL_FT_ID,
                    count);

                while (resultSet.next())
                {
                    result.add(new FileRecord(resultSet, contact));
                }
            }
            catch (SQLException e)
            {
                sLog.error("Failed in findLast: ", e);
            }
            finally
            {
                DatabaseUtils.safeClose(connection, resultSet);
            }
        }

        sLog.debug("found " + result.size() + " records");
        return result;
    }

    /**
     * Returns all the file transfers having the given keyword in the filename
     *
     * @param metaContact MetaContact the receiver or sender of the file
     * @param keyword keyword
     * @return Collection of FileRecords
     */
    public Collection<FileRecord> findByKeyword(
        MetaContact metaContact, String keyword)
    {
        List<FileRecord> result = new ArrayList<>();

        Map<Contact, String> jids = getJidsForMetaContact(metaContact);
        sLog.debug("keyword: " + keyword + ", jids: " + jids);

        for (Map.Entry<Contact, String> jidEntry : jids.entrySet())
        {
            Contact contact = jidEntry.getKey();
            DatabaseConnection connection = null;
            ResultSet resultSet = null;

            try
            {
                connection = mDatabaseService.connect();
                resultSet = connection.findByKeyword(
                    FileHistoryTable.NAME,
                    FileHistoryTable.COL_LOCAL_JID,
                    getImAccountJid(),
                    FileHistoryTable.COL_REMOTE_JID,
                    Arrays.asList(jidEntry.getValue()),
                    FileHistoryTable.COL_DATE,
                    FileHistoryTable.COL_FT_ID,
                    FileHistoryTable.COL_FILE,
                    keyword);

                while (resultSet.next())
                {
                    result.add(new FileRecord(resultSet, contact));
                }
            }
            catch (SQLException e)
            {
                sLog.error("Failed in findByKeyword: ", e);
            }
            finally
            {
                DatabaseUtils.safeClose(connection, resultSet);
            }
        }

        sLog.debug("found " + result.size() + " records");
        return result;
    }

    /**
     * Returns the file transfer with the provided transfer id
     *
     * @param uid id of the transfer
     * @return FileRecord if it exists, null otherwise
     */
    public FileRecord findByID(String uid)
    {
        FileRecord result = null;
        DatabaseConnection connection = null;
        ResultSet resultSet = null;

        try
        {
            connection = mDatabaseService.connect();
            String [] searchColumnNames = {FileHistoryTable.COL_FT_ID};
            resultSet = connection.findByKeyword(
                FileHistoryTable.NAME,
                FileHistoryTable.COL_DATE,
                uid,
                searchColumnNames,
                1);

            if (resultSet.next())
            {
                MetaContact metaContact = jidToMetaContact(resultSet.getString(FileHistoryTable.COL_REMOTE_JID));
                Contact contact = metaContact != null ? metaContact.getIMContact() : null;
                result = new FileRecord(resultSet, contact);
            }
        }
        catch (SQLException e)
        {
            sLog.error("Failed in findByKeyword: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection, resultSet);
        }

        return result;
    }

    /**
     * Returns the supplied number of recent file transfers after the given
     * date, in ascending date order.
     *
     * @param metaContact MetaContact the receiver or sender of the file
     * @param date transfers after date
     * @param count transfers count
     * @return Collection of FileRecords
     */
    public Collection<FileRecord> findFirstRecordsAfter(
            MetaContact metaContact, Date date, int count)
    {
        List<FileRecord> result = new ArrayList<>();

        Map<Contact, String> jids = getJidsForMetaContact(metaContact);
        sLog.debug("date: " + date.getTime() + ", count: " + count +
            ", jids: " + jids);

        for (Map.Entry<Contact, String> jidEntry : jids.entrySet())
        {
            Contact contact = jidEntry.getKey();
            DatabaseConnection connection = null;
            ResultSet resultSet = null;

            try
            {
                connection = mDatabaseService.connect();
                resultSet = connection.findFirstRecordsAfter(
                    FileHistoryTable.NAME,
                    FileHistoryTable.COL_LOCAL_JID,
                    getImAccountJid(),
                    FileHistoryTable.COL_REMOTE_JID,
                    jidEntry.getValue(),
                    FileHistoryTable.COL_DATE,
                    FileHistoryTable.COL_FT_ID,
                    date,
                    count);

                while (resultSet.next())
                {
                    result.add(new FileRecord(resultSet, contact));
                }
            }
            catch (SQLException e)
            {
                sLog.error("Failed in findFirstRecordsAfter: ", e);
            }
            finally
            {
                DatabaseUtils.safeClose(connection, resultSet);
            }
        }

        LinkedList<FileRecord> resultAsList = new LinkedList<>(result);

        int toIndex = count;
        if (toIndex > resultAsList.size())
        {
            toIndex = resultAsList.size();
        }

        sLog.debug("found " + resultAsList.size() + " records, return subset");
        return resultAsList.subList(0, toIndex);
    }

    /**
     * Returns the supplied number of recent file transfers before the given
     * date, in ascending date order.
     *
     * @param metaContact MetaContact the receiver or sender of the file
     * @param date transfers before date
     * @param count transfers count
     * @return Collection of FileRecords
     */
    public Collection<FileRecord> findLastRecordsBefore(
            MetaContact metaContact, Date date, int count)
    {
        List<FileRecord> result = new ArrayList<>();

        Map<Contact, String> jids = getJidsForMetaContact(metaContact);
        sLog.debug("date: " + date.getTime() + ", count: " + count +
            ", jids: " + jids);

        for (Map.Entry<Contact, String> jidEntry : jids.entrySet())
        {
            Contact contact = jidEntry.getKey();
            DatabaseConnection connection = null;
            ResultSet resultSet = null;

            try
            {
                connection = mDatabaseService.connect();
                resultSet = connection.findLastRecordsBefore(
                    FileHistoryTable.NAME,
                    FileHistoryTable.COL_LOCAL_JID,
                    getImAccountJid(),
                    FileHistoryTable.COL_REMOTE_JID,
                    Arrays.asList(jidEntry.getValue()),
                    FileHistoryTable.COL_DATE,
                    FileHistoryTable.COL_FT_ID,
                    date,
                    count);

                while (resultSet.next())
                {
                    result.add(new FileRecord(resultSet, contact));
                }
            }
            catch (SQLException e)
            {
                sLog.error("Failed in findLastRecordsBefore: ", e);
            }
            finally
            {
                DatabaseUtils.safeClose(connection, resultSet);
            }
        }

        LinkedList<FileRecord> resultAsList = new LinkedList<>(result);
        int startIndex = resultAsList.size() - count;

        if (startIndex < 0)
        {
            startIndex = 0;
        }

        sLog.debug("found " + resultAsList.size() + " records, return subset");
        return resultAsList.subList(startIndex, resultAsList.size());
    }

    /**
     * Removes the file transfer with the provided transfer id from the database
     *
     * @param uid id of the transfer
     */
    public void removeByID(String uid)
    {
        DatabaseConnection connection = null;
        PreparedStatement preparedStatement = null;

        try
        {
            connection = mDatabaseService.connect();

            preparedStatement = connection.prepare("DELETE FROM " +
            FileHistoryTable.NAME + " WHERE " +
            FileHistoryTable.COL_FT_ID + " = ?");

            preparedStatement.setString(1, uid);
            connection.execute(preparedStatement);
        }
        catch (SQLException e)
        {
            sLog.error("Failed to remove file record ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection);
        }
    }

    /**
     * When new protocol provider is registered we check
     * does it supports FileTransfer and if so add a listener to it
     *
     * @param serviceEvent ServiceEvent
     */
    public void serviceChanged(ServiceEvent serviceEvent)
    {
        Object sService = mBundleContext.getService(serviceEvent.getServiceReference());

        sLog.trace("Received a service event for: " + sService.getClass().getName());

        // we don't care if the source service is not a protocol provider
        if (!(sService instanceof ProtocolProviderService))
        {
            return;
        }

        sLog.debug("Service is a protocol provider.");

        if (serviceEvent.getType() == ServiceEvent.REGISTERED)
        {
            sLog.debug("Handling registration of a new Protocol Provider.");

            handleProviderAdded((ProtocolProviderService)sService);
        }
        else if (serviceEvent.getType() == ServiceEvent.UNREGISTERING)
        {
            handleProviderRemoved((ProtocolProviderService) sService);
        }
    }

    /**
     * Listens for changes in file transfers.
     * @param event
     */
    public void statusChanged(FileTransferStatusChangeEvent event)
    {
        FileTransfer ft = event.getFileTransfer();
        FileHistoryTable.STATUS status = getStatus(ft.getStatus());

        // Ignore events we don't need.
        if (status != null)
        {
            updateFileTransferStatus(ft.getID(), status);
        }
    }

    private static FileHistoryTable.DIRECTION getDirection(int direction)
    {
        switch(direction)
        {
            case FileTransfer.IN :
                return FileHistoryTable.DIRECTION.IN;
            case FileTransfer.OUT :
                return FileHistoryTable.DIRECTION.OUT;
            default: return null;
        }
    }

    /**
     * Maps only the statuses we are interested in, otherwise returns null.
     * @param status the status as receive from FileTransfer
     * @return the corresponding status of FileRecord.
     */
    private static FileHistoryTable.STATUS getStatus(int status)
    {
        switch(status)
        {
            case FileTransferStatusChangeEvent.CANCELED:
                return FileHistoryTable.STATUS.CANCELED;
            case FileTransferStatusChangeEvent.COMPLETED:
                return FileHistoryTable.STATUS.COMPLETED;
            case FileTransferStatusChangeEvent.FAILED:
                return FileHistoryTable.STATUS.FAILED;
            case FileTransferStatusChangeEvent.REFUSED:
                return FileHistoryTable.STATUS.REFUSED;
            case FileTransferStatusChangeEvent.NO_RESPONSE:
                return FileHistoryTable.STATUS.FAILED;
            case FileTransferStatusChangeEvent.IN_PROGRESS:
                return FileHistoryTable.STATUS.ACTIVE;
            case FileTransferStatusChangeEvent.PREPARING:
                return FileHistoryTable.STATUS.PREPARING;
            case FileTransferStatusChangeEvent.WAITING:
                return FileHistoryTable.STATUS.WAITING;
            default: return null;
        }
    }

    /**
     * @param event
     */
    public void fileTransferRequestReceived(FileTransferRequestEvent event)
    {
        IncomingFileTransferRequest req = event.getRequest();

        addFileTransferEntry(
            req.getSender().getAddress(),
            req.getID(),
            req.getFileName(),
            getDirection(FileTransfer.IN),
            event.getTimestamp(),
            FileHistoryTable.STATUS.WAITING);
    }

    /**
     * @param event
     */
    public void fileTransferRequestThumbnailUpdate(FileTransferRequestEvent event)
    {
        // We only store the file path in the database, updating the thumbnail
        // doesn't change anything in the database.
    }

    /**
     * New file transfer was created.
     * @param event fileTransfer
     */
    public void fileTransferCreated(FileTransferCreatedEvent event)
    {
        FileTransfer fileTransfer = event.getFileTransfer();

        fileTransfer.addStatusListener(this);

        try
        {
            if (fileTransfer.getDirection() == FileTransfer.IN)
            {
                updateFileTransferStatus(fileTransfer.getID(),
                                         FileHistoryTable.STATUS.ACTIVE);
                updateFileTransferFilename(
                    fileTransfer.getID(),
                    fileTransfer.getLocalFile().getCanonicalPath());
            }
            else if (fileTransfer.getDirection() == FileTransfer.OUT)
            {
                addFileTransferEntry(
                    fileTransfer.getContact().getAddress(),
                    fileTransfer.getID(),
                    fileTransfer.getLocalFile().getCanonicalPath(),
                    getDirection(FileTransfer.OUT),
                    event.getTimestamp(),
                    FileHistoryTable.STATUS.WAITING);
            }
        }
        catch (IOException e)
        {
            sLog.error("Could not add file transfer log to history", e);
        }
    }

    /**
     * Called when a new <tt>IncomingFileTransferRequest</tt> has been rejected.
     *
     * @param event the <tt>FileTransferRequestEvent</tt> containing the
     * received request which was rejected.
     */
    public void fileTransferRequestRejected(FileTransferRequestEvent event)
    {
        IncomingFileTransferRequest req = event.getRequest();

        updateFileTransferStatus(req.getID(), FileHistoryTable.STATUS.FAILED);
    }

    /**
     * Called when an <tt>IncomingFileTransferRequest</tt> has been canceled
     * from the contact who sent it.
     *
     * @param event the <tt>FileTransferRequestEvent</tt> containing the
     * request which was canceled.
     */
    public void fileTransferRequestCanceled(FileTransferRequestEvent event)
    {
        IncomingFileTransferRequest req = event.getRequest();

        updateFileTransferStatus(req.getID(),
            FileHistoryTable.STATUS.CANCELED);
    }

    /**
     * Add a new File Transfer entry into the database.
     * @param remoteJid The JID of the other party.
     * @param ftid ID of a FileTransfer object.
     * @param filename To set in the database.
     * @param direction To set in the database.
     * @param date To set in the database.
     * @param status To set in the database.
     */
    private void addFileTransferEntry(String remoteJid,
                                      String ftid,
                                      String filename,
                                      FileHistoryTable.DIRECTION direction,
                                      Date date,
                                      FileHistoryTable.STATUS status)
    {
        sLog.debug("Add file transfer entry: " + ftid);

        DatabaseConnection connection = null;

        try
        {
            connection = mDatabaseService.connect();

            PreparedStatement preparedStatement = connection.prepare("INSERT INTO " + FileHistoryTable.NAME +
                "(" + FileHistoryTable.COL_LOCAL_JID + "," +
                FileHistoryTable.COL_REMOTE_JID + "," +
                FileHistoryTable.COL_FT_ID + "," +
                FileHistoryTable.COL_FILE + "," +
                FileHistoryTable.COL_DIR + "," +
                FileHistoryTable.COL_DATE + "," +
                FileHistoryTable.COL_STATUS + "," +
                FileHistoryTable.COL_ATTENTION + ")  VALUES (?,?,?,?,?,?,?,?)");

            preparedStatement.setString(1, getImAccountJid());
            preparedStatement.setString(2, remoteJid.toLowerCase());
            preparedStatement.setString(3, ftid);
            preparedStatement.setString(4, filename);
            preparedStatement.setInt(5, direction.ordinal());
            preparedStatement.setLong(6, date.getTime());
            preparedStatement.setInt(7, status.ordinal());
            preparedStatement.setBoolean(8, direction == FileHistoryTable.DIRECTION.IN);

            // We can't log the actual statement as it contains the full text
            // of the file path. Instead, log a copy of the statement with the
            // path removed.
            connection.log(DatabaseUtils.createInsertTransferLogString(
                getImAccountJid(),
                remoteJid,
                ftid,
                direction.ordinal(),
                date.getTime(),
                status.ordinal(),
                direction == FileHistoryTable.DIRECTION.IN));

            connection.executeNoLog(preparedStatement);
        }
        catch (SQLException e)
        {
            sLog.error("Failed to add File Transfer entry");
        }
        finally
        {
            DatabaseUtils.safeClose(connection);
        }
    }

    /**
     * Write a new File Transfer status into the database.
     * @param ftid ID of a FileTransfer object.
     * @param status New status to set.
     */
    private void updateFileTransferStatus(String ftid,
                                          FileHistoryTable.STATUS status)
    {
        sLog.debug("Update file transfer status for: " + ftid);

        DatabaseConnection connection = null;

        try
        {
            connection = mDatabaseService.connect();
            PreparedStatement preparedStatement = connection.prepare(
                "UPDATE " + FileHistoryTable.NAME +
                " SET " + FileHistoryTable.COL_STATUS + "=?" +
                " WHERE " + FileHistoryTable.COL_FT_ID + "=? AND " +
                FileHistoryTable.COL_LOCAL_JID + "=?");

            preparedStatement.setInt(1, status.ordinal());
            preparedStatement.setString(2, ftid);
            preparedStatement.setString(3, getImAccountJid());

            connection.execute(preparedStatement);
        }
        catch (SQLException e)
        {
            sLog.error("Could not update file transfer status: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection);
        }
    }

    /**
     * Write a new File Transfer status into the database.
     * @param ftid ID of a FileTransfer object.
     * @param filename New filename to set.
     */
    private void updateFileTransferFilename(String ftid,
                                            String filename)
    {
        sLog.debug("Update file transfer filename for: " + ftid);

        DatabaseConnection connection = null;

        try
        {
            connection = mDatabaseService.connect();
            PreparedStatement preparedStatement = connection.prepare(
                "UPDATE " + FileHistoryTable.NAME +
                " SET " + FileHistoryTable.COL_FILE + "=?" +
                " WHERE " + FileHistoryTable.COL_FT_ID + "=? AND " +
                FileHistoryTable.COL_LOCAL_JID + "=?");

            preparedStatement.setString(1, filename);
            preparedStatement.setString(2, ftid);
            preparedStatement.setString(3, getImAccountJid());

            connection.execute(preparedStatement);
        }
        catch (SQLException e)
        {
            sLog.error("Could not update file transfer filename: ", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection);
        }
    }

    /**
     * Update the attention state of all file records with the associated jid
     *
     * @param jid the remote jid of the file records to update
     * @param attention the state to update the attention flag to
     */
    public void updateAttentionState(String jid, boolean attention)
    {
        DatabaseConnection connection = null;
        PreparedStatement preparedStatement = null;

        try
        {
            connection = mDatabaseService.connect();

            preparedStatement = connection.prepare("UPDATE " +
                FileHistoryTable.NAME + " SET " +
                FileHistoryTable.COL_ATTENTION + "=? WHERE " +
                FileHistoryTable.COL_REMOTE_JID + "=?");

            preparedStatement.setBoolean(1, attention);
            preparedStatement.setString(2, jid);

            connection.execute(preparedStatement);
        }
        catch (SQLException e)
        {
            sLog.error("Could not update attention state", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection);
        }
    }

    /**
     * Mark all active transfers as failed at the start of day - transfers
     * are not active after a restart.
     */
    public void cleanUpActiveTransfers()
    {
        DatabaseConnection connection = null;
        PreparedStatement preparedStatement = null;

        try
        {
            connection = mDatabaseService.connect();

            preparedStatement = connection.prepare("UPDATE " +
                FileHistoryTable.NAME + " SET " +
                FileHistoryTable.COL_STATUS + "=? WHERE (" +
                FileHistoryTable.COL_STATUS + "=? OR " +
                FileHistoryTable.COL_STATUS + "=? OR " +
                FileHistoryTable.COL_STATUS + "=?)");

            preparedStatement.setInt(1, FileHistoryTable.STATUS.FAILED.ordinal());
            preparedStatement.setInt(2, FileHistoryTable.STATUS.WAITING.ordinal());
            preparedStatement.setInt(3, FileHistoryTable.STATUS.PREPARING.ordinal());
            preparedStatement.setInt(4, FileHistoryTable.STATUS.ACTIVE.ordinal());

            connection.execute(preparedStatement);
        }
        catch (SQLException e)
        {
            sLog.error("Could not tidy up active transfers", e);
        }
        finally
        {
            DatabaseUtils.safeClose(connection);
        }
    }

    /**
     * Returns the IM ProtocolProviderService.
     *
     * @return the IM ProtocolProviderService.
     */
    private ProtocolProviderService getImProvider()
    {
        // This can't be initialized when this service starts, as the IM
        // provider won't be available then.
        if (mImProvider == null)
        {
            mImProvider = AccountUtils.getImProvider();
        }

        return mImProvider;
    }

    /**
     * Returns the account ID of the IM account in lower case, something like:
     *     <phonenumber>@<serveraddress>
     * The case is important as the user can add the account with any case, but
     * we must store all variants the same way.
     *
     * @return the account ID of the IM account.
     */
    private String getImAccountJid()
    {
        ProtocolProviderService provider = getImProvider();
        return (provider != null) ?
            provider.getAccountID().getUserID().toLowerCase() : "unknown";
    }

    /**
     * @return The MetaContact for the jid, if found.
     */
    private MetaContact jidToMetaContact(String jid)
    {
        ProtocolProviderService provider = getImProvider();
        String accountID = provider != null ? provider.getAccountID().getAccountUniqueID(): null;
        if (accountID == null)
        {
            return null;
        }
        return FileHistoryActivator.getContactListService().
                                        findMetaContactByContact(jid, accountID);
    }
}
