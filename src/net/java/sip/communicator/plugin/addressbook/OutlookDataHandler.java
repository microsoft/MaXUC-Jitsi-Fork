// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.addressbook;

import static org.jitsi.util.Hasher.logHasher;
import static net.java.sip.communicator.plugin.addressbook.OutlookServerNative.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.Map.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.jitsi.service.fileaccess.FileAccessService;
import org.jitsi.util.*;

import net.java.sip.communicator.plugin.addressbook.calendar.*;
import net.java.sip.communicator.service.diagnostics.ReportReason;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.util.ContactLogger;
import net.java.sip.communicator.util.Logger;

/**
 * Data handler responsible for all communication to and from the Outlook COM
 * server.
 */
public class OutlookDataHandler
       extends AbstractAddressBookDataHandler
       implements OperationSetServerStoredContactInfo,
                  OperationSetServerStoredUpdatableContactInfo
{
    /**
     * The logger.
     */
    private static final Logger logger
        = Logger.getLogger(OutlookDataHandler.class);

    private static final ContactLogger contactLogger = ContactLogger.getLogger();

    /**
     * Number of files to store COM Server logs for.
     *
     * Each file is a single startup of the COM server.
     */
    public static final int LOGGING_LOG_COUNT = 3;

    /**
     * Maximum number of crash dumps to allow at any time.
     */
    public static final int MAX_CRASH_DUMPS = 3;
    /**
     * Directory to store COM server logs in.
     */
    public static final String LOGGING_DIR_NAME = "log";

    /**
     * Filename for the COM server to use for logging.
     */
    public static final String LOGGING_SERVER_LOG_NAME = "msoutlookaddrbook";

    /**
     * Filename for the COM client (native code) to use for logging.
     */
    public static final String LOGGING_CLIENT_LOG_NAME = "msoutlookaddrbook-client";

    /**
     * Time between checking the server is alive in milliseconds.
     */
    public static final long SERVER_ALIVE_CHECK_TIME = 1000;

    /**
     * Number of times to wait before considering the server failed
     */
    public static final int MAX_FAILURE_COUNT = 10;

    /**
     * Prefix on all valid MAPI folder IDs (4 bytes of 0, converted to ASCII).
     * See <a href=http://msdn.microsoft.com/en-us/library/ee217297(v=exchg.80).aspx>http://msdn.microsoft.com/en-us/library/ee217297(v=exchg.80).aspx</a>
     */
    public static final String MAPI_VALID_FOLDER_ID_PREFIX = "00000000";

    /**
     * The lock object used to synchronize the notification thread.  If this
     * thread will cause the OutlookRpcClient lock to be obtained, that lock
     * <b>must</b> be obtained before the notificationThreadLock is obtained.<p>
     * Note that 'cause to be obtained' includes sending a request to the
     * MsOutlookAddrbook server process, if that request will cause the server
     * to respond by sending client updates (e.g. a QueryContacts call).
     */
    private final Object notificationThreadLock = new Object();

    /**
     * The thread used to collect the notifications.
     */
    private NotificationThread notificationThread;

    /**
     * A structure mapping from AccessionIDs to a Set of Outlook IDs. Each Accession ID can map to
     * multiple Outlook IDs. Note that although this map is a ConcurrentMap (so you won't get a
     * ConcurrentModificationException accessing it), access to it is not synchronized (and so not
     * fully thread safe), and the Sets within the map are accessed in an entirely un-thread-safe
     * way.
     */
    private final ConcurrentMap<String, Set<String>> idMap = new ConcurrentHashMap<>();

    /**
     * Calendar data handler - requests and parses and schedules meeting tasks
     * from Outlook
     */
    private OutlookCalendarDataHandler calendarDataHandler;

    /**
     * RPC Client to make requests to Outlook
     */
    private OutlookRpcClient client;

    /**
     * RPC Server to receive calls from Outlook on
     */
    private OutlookRpcServer server;

    /**
     * Whether we have marked all necessary contacts as resolved and purged
     * all unresolved contacts
     */
    private boolean allContactsResolved = false;

    /**
     * Last status from MAPI
     */
    private Long mapiStatus;

    /**
     * Are we in the process of shutting down?
     */
    private boolean uninitializing;

    /**
     * Lock for uninitializing
     */
    private final Object uninitializingLock = new Object();

    /**
     * The ID of the default contact folder within Outlook
     */
    private String defaultContactFolder;

    /**
     * The timer to schedule crash checks on.
     */
    private Timer mCrashTimer;

    /**
     * The number of times the server has failed
     */
    private int mFailureCount;

    /**
     * Last time the Outlook Server restarted
     */
    protected long mLastRestart;

    /**
     * Load the libraries we need
     */
    static
    {
        try
        {
            logger.debug("Loading the native library");
            loadOutlookLibrary();
        }
        catch (Throwable ex)
        {
            logger.error("Unable to load outlook native lib", ex);
            throw new RuntimeException(ex);
        }

        int bitness = nativeGetOutlookBitnessVersion();
        int version = nativeGetOutlookVersion();
        if(bitness != -1 && version != -1)
        {
            String outlookVersion = version + "-x" + bitness;
            logger.info("Outlook version is " + outlookVersion);

            // Store the version in config as well so that the info is
            // available even if the logs wrap.
            AddressBookProtocolActivator.getConfigService().user().setProperty(
                   "net.java.sip.communicator.OUTLOOK_VERSION", outlookVersion);
        }
    }

    /**
     * Constructor for this data handler
     *
     * @param parentProvider the protocol provider service that created us
     */
    protected OutlookDataHandler(
                      AbstractAddressBookProtocolProviderService parentProvider)
    {
        super(parentProvider);

        logger.debug("OutlookDataHandler started");

        // Add an operation set for presence
        parentProvider.addSupportedOperationSet(
                                           OperationSetPersistentPresence.class,
                                           this);

        // Add an operation set for contact details
        parentProvider.addSupportedOperationSet(
                                      OperationSetServerStoredContactInfo.class,
                                      this);

        // And finally, an operation set for editing the contact details.
        parentProvider.addSupportedOperationSet(
                             OperationSetServerStoredUpdatableContactInfo.class,
                             this);
    }

    /**
     * Initialize the server
     *
     * This will rotate the log files, and provide the port on which the
     * RpcServer is running, and start the timers.
     *
     * @throws Exception
     */
    private void Initialize() throws Exception
    {
        mLastRestart = System.currentTimeMillis();
        clearOldCrashFiles();
        String[] logs = rollLogFiles();

        if (mCrashTimer != null)
        {
            mCrashTimer.cancel();
        }

        mapiStatus = null;

        logger.info("Starting the server");
        nativeInitialize(server.getPort(), logs[0], logs[1], getCrashDir().getAbsolutePath());
    }

    /**
     * Get a File representing the directory to store crash files
     *
     * @return a File representing the crash directory
     * @throws Exception
     */
    private static File getCrashDir() throws Exception
    {
        return AddressBookProtocolActivator.getFileAccessService().
            getPrivatePersistentDirectory(LOGGING_DIR_NAME);
    }

    /**
     * Tracks a crash dump file in the log directory, generated by the Outlook Server
     */
    private static class CrashDump implements Comparable<CrashDump>
    {
        public CrashDump(File file)
        {
            mFile = file;
            mTime = file.lastModified();
        }

        private File mFile;
        private long mTime;

        public void delete()
        {
            logger.debug("Deleting: " + mFile);

            if (! mFile.delete())
            {
                logger.warn("Failed to delete crash dump: " + mFile);
            }
        }

        @Override
        public int hashCode()
        {
            return mFile.hashCode();
        }

        @Override
        public boolean equals(Object other)
        {
            if (other == this)
            {
                return true;
            }

            if (!(other instanceof CrashDump))
            {
                return false;
            }

            CrashDump otherDump = (CrashDump) other;

            return mFile.equals(otherDump.mFile);
        }

        @Override
        public int compareTo(CrashDump other)
        {
            int result = Long.compare(mTime, other.mTime);

            // The files have an identical modification time
            //
            // Return 0 only if they represent the same file.
            if (result == 0)
            {
                result = mFile.compareTo(other.mFile);
            }

            // We want to return later files as more important
            return (0 - result);
        }
    }

    /**
     * @return a Set of all crash dumps (any file in the log directory ending in ".dmp")
     */
    private static Set<CrashDump> getCrashDumps()
    {
        Set<CrashDump> dumps = new TreeSet<>();

        try
        {
            File logDirectory = getCrashDir();

            File[] files = logDirectory.listFiles();

            if (files == null)
            {
                throw new Exception("Failed to get a list of crash dump files");
            }

            for (File logFile : files)
            {
                if (logFile.getName().endsWith(".dmp"))
                {
                    dumps.add(new CrashDump(logFile));
                }
            }
        }
        catch (Exception e)
        {
            logger.error(e);
        }

        return dumps;
    }

    /**
     * Remove old crash dump files
     */
    private static void clearOldCrashFiles()
    {
        int ignore = 0;
        for (CrashDump dump : getCrashDumps())
        {
            ignore += 1;

            if (ignore > MAX_CRASH_DUMPS)
            {
                dump.delete();
            }
        }
    }

    /**
     * Rotate the Outlook log files
     *
     * @return Path to the latest server and client log files
     */
    private static String[] rollLogFiles()
    {
        File[] serverLogFiles = new File[LOGGING_LOG_COUNT];
        File[] serverLogStartFiles = new File[LOGGING_LOG_COUNT];
        File[] serverLogOldFiles = new File[LOGGING_LOG_COUNT];
        File[] clientLogFiles = new File[LOGGING_LOG_COUNT];

        FileAccessService fileService = AddressBookProtocolActivator.getFileAccessService();

        for(int i = 0; i < LOGGING_LOG_COUNT; i++)
        {
            try
            {
                serverLogFiles[i]
                    = fileService.getPrivatePersistentFile(
                        LOGGING_DIR_NAME
                        + File.separator
                        + LOGGING_SERVER_LOG_NAME
                        + i
                        + ".log");
                serverLogStartFiles[i]
                    = fileService.getPrivatePersistentFile(
                        LOGGING_DIR_NAME
                        + File.separator
                        + LOGGING_SERVER_LOG_NAME
                        + i
                        + ".log.startup"); // suffix must match LOG_STARTUP_SUFFIX in addressbook/Logger.h
                serverLogOldFiles[i]
                    = fileService.getPrivatePersistentFile(
                        LOGGING_DIR_NAME
                        + File.separator
                        + LOGGING_SERVER_LOG_NAME
                        + i
                        + ".log.old"); // suffix must match LOG_OLD_SUFFIX in addressbook/Logger.h
                clientLogFiles[i]
                    = fileService.getPrivatePersistentFile(
                        LOGGING_DIR_NAME
                        + File.separator
                        + LOGGING_CLIENT_LOG_NAME
                        + i
                        + ".log");
            }
            catch (Exception e)
            {
                logger.error("Couldn't get access to Outlook log file", e);
            }
        }

        for (int i = LOGGING_LOG_COUNT -2; i >= 0; i--)
        {
            moveFile(serverLogFiles[i], serverLogFiles[i+1]);
            moveFile(serverLogStartFiles[i], serverLogStartFiles[i+1]);
            moveFile(serverLogOldFiles[i], serverLogOldFiles[i+1]);
            moveFile(clientLogFiles[i], clientLogFiles[i+1]);
        }

        return new String[]{serverLogFiles[0].getAbsolutePath(), clientLogFiles[0].getAbsolutePath()};
    }

    private static void moveFile(File source, File dest)
    {
        try
        {
            Files.move(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            try
            {
                // It's possible that when AOS has just died, Windows hasn't had time to realise
                // that the handle on the main log file has gone. Sleep 100ms and try again.
                TimeUnit.MILLISECONDS.sleep(100);
                Files.move(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            catch (InterruptedException | IOException e1)
            {
                logger.warn("Error moving file " + source + " to " + dest, e1);
            }
        }
    }

    @Override
    public synchronized void init() throws OperationFailedException
    {
        synchronized (uninitializingLock)
        {
            uninitializing = false;
        }

        // Start our RPC end points
        try
        {
            logger.info("Initialising Outlook RPC endpoints");

            client = new OutlookRpcClient(this);
            calendarDataHandler = new OutlookCalendarDataHandler(client, parentProvider);

            server = new OutlookRpcServer(this, calendarDataHandler, client);

            // Initialise the COM server
            logger.info("Initialising Outlook server");

            // Reset the failure count
            mFailureCount = 0;

            try
            {
                Initialize();

                logger.debug("Init complete");
            }
            catch (OutlookMAPIHResultException e)
            {
                nativeUninitialize();
                throw e;
            }
        }
        catch (Exception e)
        {
            logger.error("Failed to connect to Outlook", e);
            throw new OperationFailedException(
                "Failed to initialize Outlook connection",
                OperationFailedException.NOT_FOUND);
        }
    }

    /**
     * Update the MAPI status as reported by the AOS.  The first time we learn of the MAPI status
     * we schedule the keep-alive checking/uninitialize AOS depending on success/failure.
     * @param status
     */
    public synchronized void setMapiStatus(long status)
    {
        Long oldMapiStatus = mapiStatus;
        mapiStatus = status;
        if (oldMapiStatus == null)
        {
            if (mapiStatus == 0)
            {
                logger.info("MAPI status 0 for the first time");

                // If this is the first MAPI status we have received then it means
                // we have successfully initialized the MAPI server for the first
                // time. Therefore query for all contacts
                queryOutlook();

                // Start checking for crashes
                mCrashTimer = new Timer("OutlookCrashHandler");
                mCrashTimer.scheduleAtFixedRate(
                    new OutlookCrashHandler(mCrashTimer),
                    0,
                    SERVER_ALIVE_CHECK_TIME);
            }
            else
            {
                logger.error("Bad MAPI status " + mapiStatus);
                nativeUninitialize();
            }
        }

        notifyAll();
    }

    @Override
    public synchronized void uninitialize()
    {
        logger.info("Uninitialize " + this);

        synchronized (uninitializingLock)
        {
            uninitializing = true;

            // Stop the crash check
            if (mCrashTimer != null)
            {
                mCrashTimer.cancel();
            }
        }

        synchronized (notificationThreadLock)
        {
            if (notificationThread != null)
            {
                notificationThread.kill();
            }
        }

        notifyAll();

        if (client != null)
        {
            client.stop();
        }

        if (server != null)
        {
            server.stop();
        }

        // Uninitialize the COM server
        logger.info("Uninitialising Outlook server");
        nativeUninitialize();

        server = null;
        client = null;
    }

    /**
     * Thread used to collect the notification.
     */
    private class NotificationThread
        extends Thread
    {
        /**
         * The list of notification collected.
         */
        private Vector<NotificationIdFunction> contactIds
            = new Vector<>();

        /**
         * Whether the notification thread should start processing events
         */
        private boolean processEvents;

        /**
         * The object to lock on for processing Outlook events
         */
        private final Object processLock = new Object();

        /**
         * True if this thread has been killed.
         */
        private boolean killed = false;

        /**
         * Initializes a new notification thread.
         */
        public NotificationThread()
        {
            super("OutlookDataHandler notification thread");
            logger.debug("NotificationThread created");
        }

        public void kill()
        {
            synchronized (notificationThreadLock)
            {
                killed = true;
            }
        }

        /**
         * Dispatches the collected notifications.
         */
        public void run()
        {
            // Do not start processing events unless we have not received a
            // notification from Outlook for over 500ms. This prevents us
            // overloading the COM server and missing contacts
            synchronized (processLock)
            {
                do
                {
                    try
                    {
                        // Set processEvents to true so if the timer pops
                        // before being notified we exit the loop
                        processEvents = true;
                        processLock.wait(500);
                    }
                    catch (InterruptedException ex)
                    {
                        logger.error("Interrupted notification thread, returning");
                        return;
                    }
                }
                while (!processEvents);
            }

            boolean hasMore = false;
            NotificationIdFunction idFunction = null;
            String id;
            char function;

            synchronized(notificationThreadLock)
            {
                hasMore = (contactIds.size() > 0) && !killed;
                if (hasMore)
                {
                    // Once we have retrieved an item from the contactID list
                    // we must remove it to be thread safe.
                    idFunction = contactIds.remove(0);
                }
            }

            // While there are more contacts to process...
            while (hasMore)
            {
                id = idFunction.getId();
                function = idFunction.getFunction();
                contactLogger.debug("Considering notification item " + idFunction);

                // If there's no client, then this has been shutdown.  And so
                // we should skip the contact
                boolean skipItem = (client == null);

                // If this is a delete and we have this id in our map, we can
                // skip getting the properties and go straight to deleting
                if (function == 'd' && isKnownOutlookId(id))
                {
                    contactLogger.note("Item is delete of known id");
                    contactDeleted(id);
                    skipItem = true;
                }

                // Get the properties for this contact from Outlook
                Object[] props = null;
                if (!skipItem)
                {
                    try
                    {
                        props
                        = client.IMAPIProp_GetProps(id,
                            OutlookUtils.MAPI_MAILUSER_PROP_IDS,
                            OutlookUtils.MAPI_UNICODE);
                    }
                    catch(Exception ex)
                    {
                        // Don't do anything, this is expected to fail a large
                        // number of times
                        logger.warn("Failed to get properties for " + id, ex);
                        skipItem = true;
                    }

                    // Check if this is a MAPI_MAILUSER object
                    long objType = 0;
                    if(props != null &&
                        props[OutlookUtils.PR_OBJECT_TYPE] != null &&
                        props[OutlookUtils.PR_OBJECT_TYPE] instanceof Long &&
                        OutlookUtils.hasContactDetails(props))
                    {
                        objType = (Long) props[OutlookUtils.PR_OBJECT_TYPE];
                    }
                    else
                    {
                        contactLogger.note("Item is not for a contact");
                        skipItem = true;
                    }

                    if (props != null &&
                        props[OutlookUtils.pidTagParentEntryId] != null &&
                        props[OutlookUtils.pidTagParentEntryId] instanceof String &&
                        defaultContactFolder != null)
                    {
                        String parentEntryId = (String) props[OutlookUtils.pidTagParentEntryId];
                        if (!parentEntryId.equals(defaultContactFolder))
                        {
                            // SFR 463280 - we sometimes get back invalid folder
                            // IDs from Outlook here.  This looks like a scribbler.
                            // Since any valid folder ID starts with 0s, it's
                            // easy enough to spot; and preferable to accept a
                            // possibly non-default contact than to reject it.
                            if (!parentEntryId.startsWith(MAPI_VALID_FOLDER_ID_PREFIX))
                            {
                                // Garbled parent ID - rather than rejecting it,
                                // assume the real ID is fine (default contacts
                                // folder) and allow the item.
                                contactLogger.warn("Parent folder ID is invalid. " +
                                    "Assume this contact is in the default folder anyway. " +
                                    " Invalid folder ID: " + parentEntryId);
                            }
                            else
                            {
                                contactLogger.note("Contact is not from the default contact folder so ignoring: " +
                                    parentEntryId + " vs " + defaultContactFolder);
                                skipItem = true;
                            }
                        }
                    }

                    // If we have results from the Contacts folder(s), don't read from the
                    // Address Book because there may be duplicates.
                    if (OutlookUtils.MAPI_MAILUSER == objType)
                    {
                        contactLogger.note("Item has obj type of mail user");
                        skipItem = true;
                    }
                }

                // Determine the action to take unless we are skipping this
                // mail item
                try
                {
                    if (!skipItem)
                    {
                        contactLogger.note("Dealing with item " + idFunction);
                        if (function == 'd')
                        {
                            fireContactSyncEvent();
                            contactDeleted(id, props);
                        }
                        else if (function == 'u' || function == 'i')
                        {
                            fireContactSyncEvent();
                            contactUpdated(id, props);
                        }
                    }
                }
                catch (Exception e)
                {
                    // If we hit an exception on one query, we don't want
                    // to kill the thread so log an error and continue.
                    logger.error("Hit exception doing " + function, e);
                }

                synchronized(notificationThreadLock)
                {
                    hasMore = (contactIds.size() > 0) && !killed;
                    if(hasMore)
                    {
                        // Once we have retrieved an item from the contactID
                        // list we must remove it to be thread safe.
                        idFunction = contactIds.remove(0);
                    }
                }
            }

            if (queryCompleted && !allContactsResolved && !killed)
            {
                // Set all contacts resolved to true so we do not process any
                // unresolved contacts again.  If we've been killed then there's
                // no need to purge the contacts - the provider is being shutdown
                // so the contacts have been removed.
                contactLogger.info("All contacts received from Outlook, " +
                                                "removing unresolved contacts");
                allContactsResolved = true;

                parentProvider.purgeUnresolvedContacts();
            }
        }

        /**
         * Adds a new notification. Avoids previous notification for the given
         * contact.
         *
         * @param id The contact id.
         * @param function The kind of notification: 'd' for deleted, 'u' for
         * updated and 'i' for inserted.
         */
        public void add(String id, char function)
        {
            synchronized(notificationThreadLock)
            {
                NotificationIdFunction idFunction
                                     = new NotificationIdFunction(id, function);

                synchronized (processLock)
                {
                    // Alert the notification thread that it should not start
                    // processing the events because we have just received another
                    notificationThread.processEvents = false;
                    processLock.notify();
                }

                // NotificationIdFunctions are considered the same if they are
                // for the same contact.  Thus remove any outstanding functions
                // as they replaced by this one.
                contactIds.remove(idFunction);
                contactIds.add(idFunction);
            }
        }

        /**
         * Returns the number of contact notifications to deal with.
         *
         * @return The number of contact notifications to deal with.
         */
        public int getRemainingNotifications()
        {
            return contactIds.size();
        }
    }

    /**
     * Collects a new notification and adds it to the notification thread.
     *
     * @param id The contact id.
     * @param function The kind of notification: 'd' for deleted, 'u' for
     * updated and 'i' for inserted.
     */
    public void addNotification(String id, char function)
    {
        synchronized(notificationThreadLock)
        {
            if(notificationThread == null
                    || !notificationThread.isAlive())
            {
                notificationThread = new NotificationThread();
                notificationThread.add(id, function);
                notificationThread.start();
            }
            else
            {
                notificationThread.add(id, function);
            }
        }
    }

    /**
     * Returns the number of contact notifications to deal with.
     *
     * @return The number of contact notifications to deal with.
     */
    public int getRemainingNotifications()
    {
        int notifications = 0;

        synchronized(notificationThreadLock)
        {
            if(notificationThread != null)
            {
                notifications = notificationThread.getRemainingNotifications();
            }
        }

        return notifications;
    }

    /**
     * Deletes the given contact in Outlook
     *
     * @param accessionID the Accession ID for this contact
     * @return Whether the deletion was successful
     */
    public void deleteInOutlook(String accessionID)
    {
        contactLogger.note("deleteInOutlook " + accessionID);

        // Get the Outlook IDs for this Accession ID from the ID map
        Set<String> outlookIDSet = idMap.get(accessionID);

        if (outlookIDSet == null || outlookIDSet.size() == 0)
        {
            contactLogger.warn("Asked to delete contact " + accessionID +
                        " but could not find the associated Outlook ID");
            return;
        }

        // Attempt delete for each Outlook ID in turn until we succeed or run
        // out of Outlook IDs to try
        for (String outlookID : outlookIDSet)
        {
            contactLogger.note("Deleting contact " + outlookID);
            if (client.deleteContact(outlookID))
            {
                idMap.remove(accessionID);
                break;
            }
        }
    }

    /**
     * This ID may be something that this data handler is interested in. Returns
     * true if it is.
     *
     * @param id The ID to check
     * @param type The type of object that the ID represents
     * @return True if it is a relevant ID
     */
    protected boolean isRelevantItem(String id, String type)
    {
        // Either we've seen the ID before, or the type will indicate that
        // this is an ID that we care about.  The type indicates what sort of
        // object the ID is for.  It's of the form
        //      "IPM.<something>.<some other things>"
        // Therefore check to see if it contains Contact.
        // Check type before id as that is faster.
        // Note that we can't just check the type, as deleted items have type of
        // "unknown"
        return (type != null && type.contains("Contact")) ||
               isKnownOutlookId(id);
    }

    /**
     * Called on an updated or inserted event from Outlook
     *
     * @param id the Outlook ID of this contact
     * @param props the list of properties for this contact.
     */
    private void contactUpdated(String id, Object[] props)
    {
        contactLogger.note("Contact updated " + id);

        // ---------------------------------------------------------------------
        // For upgrades between V2.5 and V2.6 we need to remap Favourite status.
        // Check whether this contact is a favourite by checking user fields
        // 1, 3 and 4. We do not have to check 2 as this is the new field used
        // for favourite status.
        boolean isFavorite = false;
        Object favoriteValue = props[OutlookUtils.accessionFavoriteProp];
        if (!(OutlookUtils.CONTACT_FAVORITE_STATUS.equals(favoriteValue) ||
              OutlookUtils.CONTACT_OLD_FAV_STATUS.equals(favoriteValue)))
        {
            int userPropIndexes[] = {OutlookUtils.dispidContactUser1,
                OutlookUtils.dispidContactUser3,
                OutlookUtils.dispidContactUser4};

            for (int prop : userPropIndexes)
            {
                if (props[prop] instanceof String &&
                    (props[prop].equals(OutlookUtils.CONTACT_FAVORITE_STATUS) ||
                     props[prop].equals(OutlookUtils.CONTACT_OLD_FAV_STATUS)))
                {
                    logger.debug("Contact with name " +
                                  logHasher(props[OutlookUtils.PR_DISPLAY_NAME].toString()) +
                                  "stores favourite status in UserField " + prop);
                    isFavorite = true;

                    // Set the correct property in the props list so we add
                    // this contact as a favourite in Accession.
                    props[OutlookUtils.accessionFavoriteProp] = OutlookUtils.CONTACT_FAVORITE_STATUS;

                    // Also un-set this value - we only want to use field 2 to
                    // hold favorite status.
                    client.IMAPIProp_SetPropString(
                            OutlookUtils.MAPI_MAILUSER_PROP_IDS[prop], "", id);
                }
            }

            if (isFavorite)
            {
                logger.info("Migrating old-style favourite with name " +
                        logHasher(props[OutlookUtils.PR_DISPLAY_NAME].toString()));

                client.IMAPIProp_SetPropString(
                    OutlookUtils.MAPI_MAILUSER_PROP_IDS[OutlookUtils.accessionFavoriteProp],
                    OutlookUtils.CONTACT_FAVORITE_STATUS,
                    id);
            }
        }
        // ---------------------------------------------------------------------

        // Check for the presence of an Accession ID
        String accessionID = null;
        boolean gotIdFromMap = false;

        if (props[OutlookUtils.accessionIDProp] instanceof String)
        {
            accessionID = (String) props[OutlookUtils.accessionIDProp];

            if (accessionID != null &&
                !accessionID.isEmpty() &&
                !accessionID.startsWith(OutlookUtils.ACCESSION_UID))
            {
                // We've got an Accession ID, but it's wrong or corrupted. Reset
                // it and we will look up the ID from the map
                contactLogger.warn("Contact has unknown accession ID " + accessionID);
                accessionID = null;
            }
        }

        // If we do not find an Accession ID in the Outlook properties, then
        // check our ID map
        if (StringUtils.isNullOrEmpty(accessionID))
        {
            accessionID = getAccessionIDFromOutlookID(id);

            // If we get here, then the ID is not in the contact, so we need to
            // update the contact
            gotIdFromMap = true;
        }

        // If there is no Accession ID then write one to Outlook now.
        boolean succeeded = true;
        if (gotIdFromMap ||
            StringUtils.isNullOrEmpty(accessionID) ||
            !accessionID.startsWith(OutlookUtils.ACCESSION_UID))
        {
            contactLogger.note("Got an event for a contact without an Accession ID. " + accessionID);

            if (StringUtils.isNullOrEmpty(accessionID) ||
                !accessionID.startsWith(OutlookUtils.ACCESSION_UID))
            {
                // We don't currently have an Outlook ID, so create one.
                accessionID = createOutlookID();
            }

            props[OutlookUtils.accessionIDProp] = accessionID;

            succeeded = client.IMAPIProp_SetPropString(
              OutlookUtils.MAPI_MAILUSER_PROP_IDS[OutlookUtils.accessionIDProp],
              accessionID,
              id);
        }

        // If we failed to set the accession ID then do nothing - we will end
        // up with duplicate contacts if we cannot write an ID.
        if (!succeeded)
        {
            contactLogger.error("Failed to write an Accession ID for contact " +
                         accessionID + ". Contact modification failed");
            return;
        }

        // Create a new set of Outlook IDs if we haven't seen this Accession
        // ID before
        Set<String> outlookIDs = idMap.get(accessionID);
        if (outlookIDs == null)
        {
            outlookIDs = new HashSet<>();
        }

        outlookIDs.add(id);

        // Update the map of Outlook IDs to Accession IDs.
        idMap.put(accessionID, outlookIDs);

        fireContactUpdated(props, accessionID);
    }

    /**
     * Called on a deleted event from Outlook
     *
     * @param id the Outlook ID of this contact
     * @param props the list of properties for this contact.
     */
    private void contactDeleted(String id, Object[] props)
    {
        contactLogger.note("contactDeleted " + id);

        // Check for the presence of an Accession ID
        String accessionID = null;
        if (props[OutlookUtils.accessionIDProp] instanceof String)
        {
            accessionID = (String) props[OutlookUtils.accessionIDProp];
        }

        // If there is no Accession ID saved then we don't know about this
        // contact in Accession so there is nothing to do.
        if (accessionID == null)
        {
            contactLogger.note("Got a deleted event for an unknown contact, Outlook ID: " + id);
            return;
        }

        // We have an Accession ID, get the set of Outlook IDs
        Set<String> outlookIDs = idMap.get(accessionID);

        // If we don't have an entry for this Accession ID then we don't know
        // about this contact in Accession so there is nothing to do.
        if (outlookIDs == null)
        {
            contactLogger.note("Got a deleted event for an unknown contact, Accession ID: " + accessionID);
            return;
        }

        // We have both an Accession ID and a set of Outlook IDs so fire a
        // deleted event to the Protocol Provider Service and remove the entry
        // from the ID map
        fireContactDeleted(accessionID);
        idMap.remove(accessionID);
    }

    /**
     * Called on a deleted event from Outlook when we do not need to fetch
     * properties for the contact
     *
     * @param id the Outlook ID of this contact
     */
    private void contactDeleted(String id)
    {
        // We already know the Outlook ID exists in the map, so find the
        // Accession ID
        contactLogger.note("contactDeleted " + id);
        String accessionID = getAccessionIDFromOutlookID(id);
        fireContactDeleted(accessionID);
        idMap.remove(accessionID);
    }

    /**
     * Performs a reverse lookup in the idMap. Search for an Accession ID by
     * Outlook ID.
     *
     * @param outlookID the outlookID for which to perform the search
     * @return the Accession ID if found. Empty string otherwise
     */
    private String getAccessionIDFromOutlookID(String outlookID)
    {
        String accessionID = "";
        for (Entry<String, Set<String>> entry : idMap.entrySet())
        {
            if (entry.getValue().contains(outlookID))
            {
                accessionID = entry.getKey();
                break;
            }
        }

        return accessionID;
    }

    /**
     * Notifies the Protocol Provider Service that a contact has been deleted
     *
     * @param accessionID the Accession ID for this contact
     */
    private void fireContactDeleted(String accessionID)
    {
        contactLogger.note("Notifying " + parentProvider + " of contact deletion " + accessionID);

        ((ProtocolProviderServiceOutlookImpl) parentProvider).
                                                    contactDeleted(accessionID);
    }

    /**
     * Notifies the Protocol Provider Service that a contact has been updated
     *
     * @param props The list of properties for this contact
     */
    private void fireContactUpdated(Object[] props, String accessionID)
    {
        contactLogger.note("Notifying " + parentProvider + " of contact update " + accessionID);

        ((ProtocolProviderServiceOutlookImpl) parentProvider).
                                                    contactUpdated(props,
                                                                   accessionID);
    }

    /**
     * Creates an ID to store in Outlook for a contact
     * @return
     */
    private String createOutlookID()
    {
        return OutlookUtils.ACCESSION_UID + UUID.randomUUID().toString();
    }

    /**
     * Runs after the COM server has initialized to sync with Outlook on
     * startup or when switching contact sources to Outlook.
     */
    private void queryOutlook()
    {
        new Thread("Outlook Query Thread")
        {
            public void run()
            {
                defaultContactFolder = client.getDefaultContactsFolder();
                logger.info("Got default contacts folder: " + defaultContactFolder);

                logger.info("Querying Outlook for all contacts");
                fireContactSyncEvent();
                if (client.queryContacts(""))
                {
                    queryCompleted();
                }
                else
                {
                    logger.error("Failed to query Outlook contacts");
                }

                // The above code blocks until all contact IDs are discovered,
                // but not while the contact properties are collected (which
                // happens on a different thread).
                // The notificationThread then issues queries one-by-one for
                // each returned ID, until all the incoming IDs have been
                // processed.  Once that's finished we can move on to calendar
                // sync.
                synchronized (notificationThreadLock)
                {
                    while (getRemainingNotifications() > 0)
                    {
                        try
                        {
                            notificationThreadLock.wait(200);
                            contactLogger.note("Waiting for contact sync to complete (" +
                                getRemainingNotifications() + " items remain)");
                        }
                        catch (InterruptedException e)
                        {
                            logger.warn("Unexpectedly interrupted");
                        }
                    }
                }

                // Now get the calendar info.  Note that we do that here because
                // we should request the calendar info (which can take a while),
                // _after_ getting contacts (which is more important), and
                // requesting one blocks the other.
                calendarDataHandler.start();
            }
        }.start();
    }

    /**
     * Called when we have received all contacts from Outlook
     */
    public void queryCompleted()
    {
        // If the notification thread has already completed then
        // we must call purge now, otherwise the notification
        // thread will get to it later
        queryCompleted = true;

        if(notificationThread == null
            || !notificationThread.isAlive())
        {
            // Set all contacts resolved to true so we do not process any
            // unresolved contacts again
            contactLogger.info("All contacts received from Outlook, removing " +
                         "unresolved contacts");
            allContactsResolved = true;

            parentProvider.purgeUnresolvedContacts();
        }
    }

    /**
     * Sets the details for the contact referenced by the given Accession ID
     *
     * @param accessionID the Accession ID for the contact to update
     * @param details the list of details to update the contact with
     * @param displayName the display name of this contact
     * @param fileUnder the string used to file this contact in Outlook
     * @return whether the operation succeeded
     */
    public boolean setDetails(String accessionID,
                              ArrayList<GenericDetail> details,
                              String displayName,
                              String fileUnder)
    {
        contactLogger.note("setDetails for contact " + logHasher(displayName) + ", " + accessionID);

        // Synchronize on the notificationThreadLock so we do not respond to
        // updated events for this contact until we have finished writing it.
        // Since we may block on client operations, we must first hold the
        // OutlookRPCClient lock.
        synchronized(client)
        {
            synchronized (notificationThreadLock)
            {
                // Determine if this is a create contact event by looking for the
                // Accession ID in our id map.
                String outlookID = null;
                Set<String> outlookIDSet = idMap.get(accessionID);
                if (outlookIDSet != null && outlookIDSet.size() > 0)
                {
                    // This is a contact update, get the first Outlook ID from the
                    // list
                    outlookID = outlookIDSet.iterator().next();
                }
                else
                {
                    // If we don't find it then we need to create the contact in
                    // Outlook before filling in the details
                    outlookID = client.createContact();
                }

                if (outlookID == null)
                {
                    contactLogger.error("Failed to create a blank contact in Outlook");
                    return false;
                }

                // Created a blank contact in Outlook so update the Accession ID
                // to Outlook ID map
                HashSet<String> outlookIDs = new HashSet<>();
                outlookIDs.add(outlookID);

                idMap.put(accessionID, outlookIDs);

                // We must ensure we write the AccessionID first as we are notified
                // about contact updates when we write to Outlook. If the AccessionID
                // is not written first then we can end up with duplicate contacts
                client.IMAPIProp_SetPropString(OutlookUtils.MAPI_MAILUSER_PROP_IDS[OutlookUtils.accessionIDProp],
                                        accessionID,
                                        outlookID);

                // Loop over each contact detail and write to Outlook
                for (GenericDetail contactDetail : details)
                {
                    // Get the Outlook property ID that relates to this contact detail.
                    long propID = OutlookUtils.getPropertyForDetail(contactDetail);

                    if (propID != 0)
                    {
                        String value = (String) contactDetail.getDetailValue();
                        if (value == null)
                        {
                            value = "";
                        }

                        // Write this detail to Outlook
                        client.IMAPIProp_SetPropString(propID,
                                                value,
                                                outlookID);
                    }
                }

                // Set the name fields
                for (long i : OutlookUtils.CONTACT_NAME_FIELDS)
                {
                    client.IMAPIProp_SetPropString(i,
                                            displayName,
                                            outlookID);
                }

                // Set the File Under fields in Outlook
                client.IMAPIProp_SetPropString(OutlookUtils.MAPI_MAILUSER_PROP_IDS[OutlookUtils.dispidFileUnder],
                                               fileUnder,
                                               outlookID);
                client.IMAPIProp_SetPropString(OutlookUtils.MAPI_MAILUSER_PROP_IDS[OutlookUtils.dispidFileUnderId],
                                               fileUnder,
                                               outlookID);
            }
        }
        return true;
    }

    @Override
    protected void setContactAsFavourite(String id, boolean isFavourite)
    {
        Set<String> outlookIDs = idMap.get(id);

        String propertyValue = (isFavourite) ?
                                OutlookUtils.CONTACT_FAVORITE_STATUS :
                                "";

        if (outlookIDs != null && outlookIDs.size() > 0)
        {
            // Use the first Outlook ID to set the favourite status
            client.IMAPIProp_SetPropString(
                   OutlookUtils.MAPI_MAILUSER_PROP_IDS[OutlookUtils.dispidContactUser2],
                   propertyValue,
                   outlookIDs.iterator().next());
            contactLogger.info("Setting outlook favorite status for contact with "+
                        "Accession ID " + id + " to " + isFavourite);
        }
        else if (outlookIDs == null)
        {
            contactLogger.error("Failed to set outlook favorite status for " +
                "contact with Accession ID " + id + " as returned ID list was null");
        }
        else
        {
            contactLogger.error("Failed to set outlook favorite status for " +
                "contact with Accession ID " + id + " as no outlook IDs matched.");
        }
    }

    @Override
    public void appendState(StringBuilder state)
    {
        state.append("Outlook mapi state: ").append(mapiStatus).append("\n");
        state.append("Outlook client: ").append(client).append("\n\n");

        for (Entry<String, Set<String>> entry : idMap.entrySet())
        {
            state.append(entry.getKey())
                 .append(", ")
                 .append(entry.getValue())
                 .append("\n");
        }

        state.append("Notification thread: ");
        state.append(getRemainingNotifications());
        state.append(" items remain to process\n");

        // Request a state dump from the contacts server process
        boolean gotDump = (client == null) ? false : client.requestStateDump();
        state.append("Retrieved state dump from contacts server? ").append(gotDump);
    }

    /**
     * Check whether the server is alive and restart if required
     *
     * Must be called synchronized on uninitializingLock.
     *
     * @return whether the server was restarted.
     */
    private boolean restartServerIfRequired(Timer timer)
    {
        // Performing Check of AOS
        boolean alive = nativeCheckServerIsAlive();

        if (!alive)
        {
            if (mFailureCount >= MAX_FAILURE_COUNT)
            {
                logger.error("Outlook server has failed too many times: " + mFailureCount);
                timer.cancel();

                return false;
            }
            else
            {
                logger.error("Outlook Server isn't alive");

                // The server has stopped - we should restart it.

                // Only trigger an error report if we have a recent .dmp file, likely to have been created from
                // the event that caused the server to stop just now.
                long currentTime = System.currentTimeMillis();
                for (CrashDump dump : getCrashDumps())
                {
                    // 10 minutes is a sensibly long to wait before considering a .dmp file as unlikely to be related
                    // to the event that caused AOS to fail the above aliveness check.
                    if (currentTime - dump.mTime < TimeUnit.MILLISECONDS.convert(10L, TimeUnit.MINUTES))
                    {
                        logger.info("Crash dump " + dump.mFile + " is recent - from " + dump.mTime);
                        AddressBookProtocolActivator.getDiagsService().openErrorReportFrame(ReportReason.OUTLOOK_CRASHED);
                        break;
                    }
                }

                // Notify the RPC client that the server will be restarted
                // and therefore it should hold any pending requests
                client.restart();

                // Restart the server.
                try
                {
                    // We actually call Uninitialize() first. This will:
                    // a) release the hold on the current log file, meaning we can rotate it in Initialize.
                    // b) try to stop any existing AOS server if we think one is running.
                    nativeUninitialize();
                    Initialize();
                    mFailureCount += 1;
                }
                catch (Exception e)
                {
                    logger.error("Failed to connect to Outlook", e);
                }

                return true;
            }
        }
        else
        {
            // AOS Check Passed
            return false;
        }
    }

    /**
     * Check the last time the server was restarted
     */
    public long getLastFailure()
    {
        return mLastRestart;
    }

    /**
     * Has the OutlookServer failed since the supplied time
     *
     * @return true if there was a connection error, false otherwise
     */
    public boolean hasServerFailed(long since)
    {
        synchronized(uninitializingLock)
        {
            if (uninitializing)
            {
                return false;
            }

            // We've hit a connection error and aren't shutting down.
            // Check whether the server needs restarting
            if (restartServerIfRequired(mCrashTimer))
            {
                return true;
            }

            // If we've restarted otherwise since then, return true
            if (since < mLastRestart)
            {
                return true;
            }

            return false;
        }
    }

    /**
     * Check to see if this ID is a known Outlook ID and therefore for a contact
     *
     * @param id the Outlook ID to check
     * @return true if this is known about
     */
    private boolean isKnownOutlookId(String id)
    {
        // ID Map values is a collection of sets - so can't just do "contains".
        return idMap.values().stream().anyMatch(s -> s.contains(id));
    }

    /**
     * Responsible for checking whether the Outlook Server has crashed, and if it has
     * restarting it, and sending an error report.
     */
    private class OutlookCrashHandler
        extends TimerTask
    {
        private final Timer mTimer;

        /**
         * Create the task
         *
         * @param timer The timer we are scheduled on
         */
        public OutlookCrashHandler(Timer timer)
        {
            mTimer = timer;
            logger.debug("Created crash handler");
        }

        public void run()
        {
            // Check that the Outlook server is running

            // This is synchronized on the uninitializingLock
            // in order to prevent us trying to shutdown the Outlook server
            // and restart it simultaneously.
            synchronized(uninitializingLock)
            {
                if (uninitializing)
                {
                    logger.info("Stopping checking that Outlook Server is alive");
                    mTimer.cancel();
                    return;
                }

                restartServerIfRequired(mTimer);
            }
        }
    }
}
