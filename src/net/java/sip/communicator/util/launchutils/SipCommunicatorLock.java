/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util.launchutils;

import static net.java.sip.communicator.util.launchutils.LaunchArgHandler.sanitiseArgument;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.jitsi.util.CustomAnnotations.*;
import com.google.common.annotations.VisibleForTesting;

import net.java.sip.communicator.launcher.SIPCommunicator;
import net.java.sip.communicator.util.Logger;

/**
 * This class is used to prevent from running multiple instances of Jitsi.
 * The class binds a socket somewhere on the localhost domain and
 * records its socket address in the Jitsi configuration directory.
 * All following instances of Jitsi (and hence this class) will look
 * for this record in the configuration directory and try to connect to the
 * original instance through the socket address in there.
 *
 * @author Emil Ivov
 */
public class SipCommunicatorLock
{
    private static final Logger logger = Logger.getLogger(SipCommunicatorLock.class);

    /**
     * Indicates that something went wrong. More information will probably be
     * available in the console ... if anyone cares at all.
     */
    public static final int LOCK_ERROR = 300;

    /**
     * Returned by the soft start method to indicate that we have successfully
     * started and locked the configuration directory.
     */
    public static final int SUCCESS = 0;

    /**
     * Returned by the soft start method to indicate that an instance of Jitsi
     * has been already started and we should exit. This return
     * code also indicates that all arguments were passed to that new instance.
     */
    public static final int ALREADY_STARTED = 301;

    /**
     * The name of the file that we use to store the address and port that this
     * lock is bound on.
     */
    private static final String LOCK_FILE_NAME = ".lock";

    /**
     * The name of the property that we use to store the address that we bind on
     * in this class.
     */
    private static final String PNAME_LOCK_ADDRESS = "lockAddress";

    /**
     * The name of the property that we use to store the address that we bind on
     * in this class.
     */
    private static final String PNAME_LOCK_PORT = "lockPort";

    /**
     * The header preceding each of the arguments that we toss around between
     * instances of Jitsi
     */
    private static final String ARGUMENT = "Argument";

    /**
     * The name of the header that contains the number of arguments that we send
     * from one instance to another.
     */
    private static final String ARG_COUNT = "Arg-Count";

    /**
     * The name of the header that contains any error messages resulting from
     * remote argument handling.
     */
    private static final String ERROR_ARG = "ERROR";

    /**
     * The carriage return, line feed sequence (\r\n).
     */
    private static final String CRLF = "\r\n";

    /**
     * The number of milliseconds that we should wait for a remote SC instance
     * to come back to us.
     */
    private long LOCK_COMMUNICATION_DELAY = 1000;

    /**
     * The socket that we use for cross instance lock and communication.
     */
    private ServerSocket instanceServerSocket = null;

    /**
     * Number of times to attempt to create the lock server.
     */
    private static final int LOCK_SERVER_ATTEMPTS = 7;

    /**
     * Retry times reading the lock file.
     */
    private static final int LOCK_FILE_READ_ATTEMPTS = 6;

    /**
     * Time between retires reading lock file in milliseconds.
     */
    private static final long LOCK_FILE_READ_WAIT = 1000;

    /**
     * An address that is reported not local on macosx and is
     * assigned as default on loopback interface.
     */
    private static final String WEIRD_MACOSX_LOOPBACK_ADDRESS = "fe80:0:0:0:0:0:0:1";

    /**
     * Channel for lock file. Opened in tryLock and closed when Accession quits.
     */
    private FileChannel lockChannel;

    /**
     * Tries to lock the configuration directory. If lock-ing is not possible
     * because a previous instance is already running, then it transmits the
     * list of args to that running instance.
     * <p>
     * There are three possible outcomes of this method. 1. We lock
     * successfully; 2. We fail to lock because another instance of Jitsi
     * is already running; 3. We fail to lock for some unknown
     * error. Each of these cases is represented by an error code returned as a
     * result.
     *
     * @param args
     *            the array of arguments that we are to submit in case an
     *            instance of Jitsi has already been started.
     *
     * @return an error or success code indicating the outcome of the lock
     *         operation.
     */
    // Not closing lock channel yet, since this would release the lock. Channel
    // will be closed immediately if a lock is not acquired and will be closed
    // on shutdown otherwise, so suppress warning.
    public int tryLock(String[] args)
    {
        // Here, we look for a lock file and attempt to acquire an exclusive
        // lock on it. The implementation of this lock is OS dependent, but on
        // Windows prevents other processes from changing the locked region of
        // the file.
        //
        // Theoretically*, no two processes can acquire an exclusive lock on
        // the same file. Therefore, if we have successfully acquired a lock
        // then we are the only instance of Accession running. We create a lock
        // server, store its address in the lock file, and return SUCCESS.
        //
        // If we did not successfully acquire the lock, then most likely
        // another instance of Accession is running. We attempt to read their
        // address from the lock file and communicate our startup arguments
        // before returning ALREADY_RUNNING.
        //
        // If we fail trying to read the address or connect to the socket, then
        // either a previous instance has quit without releasing its lock
        // (unlikely) or another instance is currently starting up and has not
        // yet written to the lock file. We wait and retry.
        //
        // *It should be noted that exclusive locking fails on OS X, but OS X
        // already prevents a second instance of an application from starting
        // by most usual means, so this shouldn't have any impact on the user.
        // Certainly, opening Accession by a URI (for example by Accession
        // Meeting) does not result in a second instance.
        File lockFile = getLockFile();

        boolean lockFileAlreadyExists;
        boolean lockAcquired;

        logger.info("Checking for lock file...");

        try
        {
            lockFile.getParentFile().mkdirs();
            lockFileAlreadyExists = !lockFile.createNewFile();
            lockAcquired = getLock();
        }
        catch (IOException e)
        {
            logger.error("Error attempting to create new lock file.", e);
            return LOCK_ERROR;
        }

        if (lockAcquired)
        {
            // No instances of Accession currently running.
            logger.info("Lock acquired on " + (lockFileAlreadyExists ? "existing lock file." : "new lock file."));

            // Schedule lockChannel to be closed on shutdown. This will
            // also release the lock if this has not already happened.
            Runtime.getRuntime().addShutdownHook(
                new Thread("SipCommunicatorLock-ShutdownHook")
                {
                    public void run()
                    {
                        try
                        {
                            lockChannel.close();
                        }
                        catch (IOException e)
                        {
                            logger.error("Error closing channel.", e);
                        }
                    }
                });

            // The lock file is now associated with this instance of Accession,
            // so schedule its deletion on shutdown.
            lockFile.deleteOnExit();

            return lock(lockFile);
        }
        else
        {
            // Another instance of Accession has already locked the file.
            logger.info("Lock could not be acquired. Try to pass arguments to existing instance of client.");

            // No point leaving channel open since we have not acquired a lock.
            try
            {
                lockChannel.close();
            }
            catch (IOException e)
            {
                logger.error("Error closing channel.", e);
            }

            InetSocketAddress lockAddress = null;
            for (int retries = LOCK_FILE_READ_ATTEMPTS; retries > 0; retries--)
            {
                try
                {
                    lockAddress = readLockFile(lockFile);
                }
                catch (Exception e)
                {
                    logger.warn("Failed to read lock file", e);
                }

                if (lockAddress != null)
                {
                    // Try to broadcast our arguments to the instance of
                    // Accession which last wrote to the lock file.
                    if (interInstanceConnect(lockAddress, args) == SUCCESS)
                    {
                        logger.info("Connected to instance at: " + lockAddress);
                        return ALREADY_STARTED;
                    }
                    else
                    {
                        logger.error("Failed to connect to instance at: " + lockAddress);
                    }
                }
                else
                {
                    logger.error("Failed to retrieve lock address from: " + lockFile.getName());
                }

                // Maybe another starting instance has locked the file but has
                // not yet overwritten the socket address. Wait and then retry.
                try
                {
                    Thread.sleep(LOCK_FILE_READ_WAIT);
                }
                catch (InterruptedException e)
                {
                    logger.debug("Sleep before lock file retry interrupted.", e);
                }
            }

            // A lock still exists on the lock file, but we can't contact anything at the address.
            // Either:
            // 1. there's no other process but the lock file wasn't unlocked correctly
            // 2. the (locked) lock file's contents are corrupted and there isn't another process
            // 3. the (locked) lock file's contents are corrupted and there is another (presumably healthy) process
            // 4. the process which owns the lock is alive but unresponsive
            // The best course of action is to start up as normal, in case of 1. or 2. above.
            // We should write our address to the lock file, in case of 3. or 4. above.
            // It's important we try and take the lock though, in case of 4. and the unresponsive process
            // crashes, releases the lock, and respawns.
            // The unresponsive process may recover though, so we should continue trying to contact it, and if
            // we can then rewrite this process's address to the lock file and exit ourselves.
            // This behaviour will mean that if we are in situation:
            // 1. Unclear when the lock file will eventually become unlocked (perhaps PC restart) but
            // future instances should hand off their args to this new process.
            // 2. Unclear when the lock file will eventually become unlocked (perhaps PC restart) but
            // future instances should hand off their args to this new process.
            // 3. There will be two instances until manual intervention, PC restart etc.  No obvious improvement on
            // this behaviour, very difficult to recover from an existing healthy instance whose address we've lost.
            // No signs this has ever been hit though.
            // 4. The unresponsive process will either crash and we'll take its lock, or will recover and we'll contact
            // it and exit, returning things to how they were before.
            logger.error("Lock file corrupt or other instance of client unresponsive. Startup as usual.");

            try
            {
                lockAddress = readLockFile(lockFile);
            }
            catch (Exception e)
            {
                logger.error("Error reading lock file", e);
            }

            // Just start our own thread, this instance mightn't have started up enough to use a ThreadingService.
            new NoLockOrIpcRecoveryThread(lockAddress).start();

            return lock(lockFile);
        }
    }

    /**
     * By locking a region of the file far beyond any region that may be written to, we can acquire
     * the lock (and prevent the lock file being deleted on Windows until the lock is released) while not
     * preventing other instances from reading/writing the socket address from/to the file.
     * @return whether or not we got the lock
     * @throws IOException
     */
    @SuppressWarnings("resource")
    private boolean getLock() throws IOException
    {
        File lockFile = getLockFile();

        lockChannel = new RandomAccessFile(lockFile, "rw").getChannel();

        FileLock lock = lockChannel.tryLock(Long.MAX_VALUE - 1L, 1L, false);
        return (lock != null);
    }

    /**
     * Thread to recover from the state where we've not been able to get the lock but
     * couldn't contact another instance using the address in the lock file. We repeatedly
     * attempt to take the lock and contact the address until we can do one of these things.
     * Note that SipCommunicatorLock objects are generally not GC'ed, because we'd usually
     * have the lock and so retain a reference to the object via the ShutdownHook added when
     * we have the lock. Thus retaining a reference to the object via this thread shouldn't
     * be problematic.
     */
    private class NoLockOrIpcRecoveryThread extends Thread
    {
        @Nullable private final InetSocketAddress unresponsiveInstance;

        private NoLockOrIpcRecoveryThread(@Nullable InetSocketAddress unresponsiveAddress)
        {
            this.unresponsiveInstance = unresponsiveAddress;
            setDaemon(true);
        }

        public void run()
        {
            boolean gotLock = false;

            while (true)
            {
                try
                {
                    TimeUnit.SECONDS.sleep(1);
                }
                catch (InterruptedException e)
                {
                }

                // Can we now claim the lock?
                try
                {
                    gotLock = getLock();
                }
                catch (IOException e)
                {
                    logger.debug("Error attempting to get lock.", e);
                }

                if (gotLock)
                {
                    // If this instance now has the lock, we can just quit this recovery thread
                    // and continue as normal.
                    logger.error("Got lock!");
                    break;
                }
                else
                {
                    logger.debug("Still no lock");
                }

                // We didn't get the lock, but can we now connect to the unresponsive address?
                if (unresponsiveInstance != null)
                {
                    if (interInstanceConnect(unresponsiveInstance, new String[] {}) == SUCCESS)
                    {
                        // If we've now contacted the unresponsive instance, it must have recovered.
                        // Rewrite the recovered instance's address to the lock file so further URIs are passed to it.
                        // Experience suggests that the now recovered instance will have prevented this instance from
                        // properly initializing (due to e.g. locks on config files and the DB).  This means that
                        // calling System.exit() is likely to fail (due to a Felix Shutdown hook that won't complete
                        // if Felix didn't fully finish starting up) and moreover will be stuck in that bad state even
                        // if/when the other instance is shutdown. The safest thing to do is leave this instance
                        // running without the lock or address in the lockfile; when the user manually closes the other
                        // instance, this instance will likely finish starting up, but they can then close this instance
                        // manually and things should be in a good state again. Whilst this would be confusing for the user
                        // (as MaX UC would reopen just after they close it), it's the safest option.
                        logger.error("Contacted " + unresponsiveInstance);

                        if (writeLockFile(getLockFile(), unresponsiveInstance) == SUCCESS)
                        {
                            logger.error("Wrote " + unresponsiveInstance + " to lock file. Stopping recovery thread.");
                            break;
                        }
                        else
                        {
                            logger.error("Failed to write " + unresponsiveInstance + " to address to file ");
                        }
                    }
                    else
                    {
                        logger.debug("Still no IPC contact");
                    }
                }
            }
        }
    }

    /**
     * Binds our lock socket and records the socket address to the lock file.
     *
     * This method does not return the ALREADY_RUNNING code as it is assumed
     * that this has already been checked before calling this method.
     *
     * @param lockFile The file in which to record the socket address.
     *
     * @return the SUCCESS or LOCK_ERROR codes defined by this class.
     */
    private int lock(File lockFile)
    {
        logger.info("Saving socket details to file: " + lockFile.getName());

        InetAddress lockAddress = getRandomBindAddress();

        if (lockAddress == null)
        {
            return LOCK_ERROR;
        }

        // create a new socket
        // seven time retry binding to port
        int attempts = LOCK_SERVER_ATTEMPTS;
        int port;
        InetSocketAddress serverSocketAddress;
        int lockServerResult;

        do
        {
            port = getRandomPortNumber();
            serverSocketAddress = new InetSocketAddress(lockAddress, port);
            lockServerResult = startLockServer(serverSocketAddress);
        }
        while (lockServerResult != SUCCESS && --attempts > 0);

        if (lockServerResult != SUCCESS)
        {
            logger.error("Failed to create lock server after " +
                         LOCK_SERVER_ATTEMPTS + " retries with address: " +
                         lockAddress);
            return LOCK_ERROR;
        }

        if (writeLockFile(lockFile, serverSocketAddress) != SUCCESS)
        {
            logger.error("Failed to write bind address to file " + lockFile.getName());
            return LOCK_ERROR;
        }

        logger.info("Created lock server on: " + serverSocketAddress);
        return SUCCESS;
    }

    /**
     * Creates and binds a socket on <tt>lockAddress</tt> and then starts a
     * <tt>LockServer</tt> instance so that we would start interacting with
     * other instances of Jitsi that are trying to start.
     *
     * @return the <tt>ERROR</tt> code if something goes wrong and
     *         <tt>SUCCESS</tt> otherwise.
     */
    private int startLockServer(InetSocketAddress localAddress)
    {
        try
        {
            // check config directory
            instanceServerSocket = new ServerSocket();
        }
        catch (IOException exc)
        {
            // Just checked the impl and this doesn't seem to ever be thrown
            // .... ignore ...
            logger.error("Couldn't create server socket", exc);
            return LOCK_ERROR;
        }

        try
        {
            instanceServerSocket.bind(localAddress, 16);// Why 16? 'cos I say
            // so.
        }
        catch (IOException exc)
        {
            logger.error("Couldn't create server socket", exc);
            return LOCK_ERROR;
        }

        LockServer lockServ = new LockServer(instanceServerSocket);

        lockServ.start();

        return SUCCESS;
    }

    /**
     * Returns a randomly chosen socket address using a loopback interface (or
     * another one in case the loopback is not available) that we should bind
     * on.
     *
     * @return an InetAddress (most probably a loopback) that we can use to bind
     *         our semaphore socket on.
     */
    private InetAddress getRandomBindAddress()
    {
        NetworkInterface loopback;
        try
        {
            // find a loopback interface
            Enumeration<NetworkInterface> interfaces;
            try
            {
                interfaces = NetworkInterface.getNetworkInterfaces();
            }
            catch (SocketException exc)
            {
                // I don't quite understand why this would happen ...
                logger.error(
                      "Failed to obtain a list of the local interfaces.",
                      exc);
                return null;
            }

            loopback = null;
            while (interfaces.hasMoreElements())
            {
                NetworkInterface iface = interfaces.nextElement();

                if (isLoopbackInterface(iface))
                {
                    loopback = iface;
                    break;
                }
            }

            // if we didn't find a loopback (unlikely but possible)
            // return the first available interface on this machine
            if (loopback == null)
            {
                loopback = NetworkInterface.getNetworkInterfaces()
                                .nextElement();
            }
        }
        catch (SocketException exc)
        {
            // I don't quite understand what could possibly cause this ...
            logger.error("Could not find the loopback interface", exc);
            return null;
        }

        // get the first address on the loopback.
        InetAddress addr = loopback.getInetAddresses().nextElement();

        return addr;
    }

    /**
     * Returns a random port number that we can use to bind a socket on.
     *
     * @return a random port number that we can use to bind a socket on.
     */
    private int getRandomPortNumber()
    {
        return (int) (Math.random() * 64509) + 1025;
    }

    /**
     * Parses the <tt>lockFile</tt> into a standard Properties Object and
     * verifies it for completeness. The method also tries to validate the
     * contents of <tt>lockFile</tt> and asserts presence of all properties
     * mandated by this version.
     *
     * @param lockFile The lock file.
     *
     * @return the <tt>SocketAddress</tt> that we should use to communicate with
     *         a possibly already running version of Jitsi.
     */
    private InetSocketAddress readLockFile(File lockFile)
    {
        Properties lockProperties = new Properties();
        InputStream stream = null;

        try
        {
            stream = new FileInputStream(lockFile);
            lockProperties.load(stream);
        }
        catch (IOException e)
        {
            logger.error("Failed to read socket server address from lock file: "
                                              + lockFile.getName(), e);
            return null;
        }
        finally
        {
            if (stream != null)
            {
                try
                {
                    stream.close();
                }
                catch (IOException e)
                {
                    logger.error("Error closing stream.", e);
                }
            }
        }

        return parseLockFile(lockProperties);
    }

    @VisibleForTesting
    InetSocketAddress parseLockFile(Properties lockProperties)
    {
        String lockAddressStr = lockProperties.getProperty(PNAME_LOCK_ADDRESS);
        if (lockAddressStr == null)
        {
            logger.error("Lock file contains no lock address.");
            return null;
        }

        String lockPort = lockProperties.getProperty(PNAME_LOCK_PORT);
        if (lockPort == null)
        {
            logger.error("Lock file contains no lock port.");
            return null;
        }

        InetAddress lockAddress = findLocalAddress(lockAddressStr);

        if (lockAddress == null)
        {
            logger.error(lockAddressStr + " is not a valid local address.");
            return null;
        }

        int port;
        InetSocketAddress lockSocketAddress;
        try
        {
            port = Integer.parseInt(lockPort);
            lockSocketAddress = new InetSocketAddress(lockAddress, port);
        }
        catch (IllegalArgumentException exc)
        {
            // Note this could be from an invalid argument to new InetSocketAddress() (e.g. too high a port number)
            // or a NumberFormatException from Integer.parseInt.
            logger.error(lockPort + " is not a valid port number.", exc);
            return null;
        }

        logger.debug("Got address " + lockSocketAddress);
        return lockSocketAddress;
    }

    /**
     * Records our <tt>lockAddress</tt> into <tt>lockFile</tt> using the
     * standard properties format.
     *
     * @param lockFile The lock file.
     * @param lockAddress The address that we have to record.
     *
     * @return <tt>SUCCESS</tt> upon success and <tt>ERROR</tt> if we fail to
     *         store the file.
     */
    private int writeLockFile(File lockFile,
                              InetSocketAddress lockAddress)
    {
        Properties lockProperties = new Properties();
        OutputStream stream = null;

        lockProperties.setProperty(PNAME_LOCK_ADDRESS, lockAddress.getAddress()
                        .getHostAddress());

        lockProperties.setProperty(PNAME_LOCK_PORT, Integer
                        .toString(lockAddress.getPort()));

        try
        {
            stream = new FileOutputStream(lockFile);

            lockProperties.store(stream,
                "Jitsi lock file. This file will be automatically "
                + "removed when execution of Jitsi terminates.");

            logger.info("Written: " + lockAddress + " to file: " +
                        lockFile.getName());
        }
        catch (FileNotFoundException e)
        {
            logger.error("Lock file " + lockFile.getName() +
                " not found.", e);
            return LOCK_ERROR;
        }
        catch (IOException e)
        {
            logger.error("Failed to create lock file.", e);
            return LOCK_ERROR;
        }
        finally
        {
            if (stream != null)
            {
                try
                {
                    stream.close();
                }
                catch (IOException e)
                {
                    logger.error("Error closing stream.", e);
                }
            }
        }

        return SUCCESS;
    }

    /**
     * Returns a reference to the file that we should be using to lock Jitsi's
     * home directory, whether it exists or not.
     *
     * @return a reference to the file that we should be using to lock Jitsi's
     * home directory.
     */
    private File getLockFile()
    {
        String homeDirLocation = System.getProperty(SIPCommunicator.PNAME_SC_HOME_DIR_LOCATION);
        String homeDirName = System.getProperty(SIPCommunicator.PNAME_SC_HOME_DIR_NAME);
        String fileSeparator = System.getProperty("file.separator");

        return new File(homeDirLocation + fileSeparator + homeDirName + fileSeparator + LOCK_FILE_NAME);
    }

    /**
     * Returns an <tt>InetAddress</tt> instance corresponding to
     * <tt>addressStr</tt> or <tt>null</tt> if no such address exists on the
     * local interfaces.
     *
     * @param addressStr
     *            the address string that we are trying to resolve into an
     *            <tt>InetAddress</tt>
     *
     * @return an <tt>InetAddress</tt> instance corresponding to
     *         <tt>addressStr</tt> or <tt>null</tt> if none of the local
     *         interfaces has such an address.
     */
    private InetAddress findLocalAddress(String addressStr)
    {
        Enumeration<NetworkInterface> ifaces;

        try
        {
            ifaces = NetworkInterface.getNetworkInterfaces();
        }
        catch (SocketException exc)
        {
            logger.error(
                  "Could not extract the list of local intefcaces.",
                  exc);
            return null;
        }

        // loop through local interfaces
        while (ifaces.hasMoreElements())
        {
            NetworkInterface iface = ifaces.nextElement();

            Enumeration<InetAddress> addreses = iface.getInetAddresses();

            // loop iface addresses
            while (addreses.hasMoreElements())
            {
                InetAddress addr = addreses.nextElement();

                if (addr.getHostAddress().equals(addressStr))
                    return addr;
            }
        }
        return null;
    }

    /**
     * Initializes a client TCP socket, connects it to <tt>sockAddr</tt> and
     * sends all <tt>args</tt> to it.
     *
     * @param sockAddr the address that we are to connect to.
     * @param args the args that we need to send to <tt>sockAddr</tt>.
     *
     * @return <tt>SUCCESS</tt> upon success and <tt>ERROR</tt> if anything goes wrong.
     */
    private int interInstanceConnect(InetSocketAddress sockAddr, String[] args)
    {
        logger.debug("IPC to " + sockAddr);
        Socket interInstanceSocket = null;

        try
        {
            interInstanceSocket = new Socket(sockAddr.getAddress(), sockAddr.getPort());
            LockClient lockClient = new LockClient(interInstanceSocket);
            lockClient.start();

            PrintStream printStream = new PrintStream(interInstanceSocket.getOutputStream());
            printStream.print(ARG_COUNT + "=" + args.length + CRLF);

            for (int i = 0; i < args.length; i++)
            {
                printStream.print(ARGUMENT + "=" + args[i] + CRLF);
            }

            logger.debug("Sent message");

            lockClient.waitForReply(LOCK_COMMUNICATION_DELAY);

            // NPEs are handled in catch so no need to check whether or not we actually have a reply.
            String serverReadArgCountStr = lockClient.message.substring((ARG_COUNT + "=").length());

            int serverReadArgCount = Integer.parseInt(serverReadArgCountStr);
            logger.debug("Server read " + serverReadArgCount + " args.");

            if (serverReadArgCount != args.length)
            {
                logger.warn("Received " + serverReadArgCountStr + " args, sent " + args.length);
                return LOCK_ERROR;
            }

            printStream.flush();
            printStream.close();
        }
        //catch IOExceptions, NPEs and NumberFormatExceptions here.
        catch (Exception e)
        {
            logger.warn("Failed to connect to a running instance: address: " + sockAddr + " " + "arguments: " + args, e);
            return LOCK_ERROR;
        }
        finally
        {
            try
            {
                interInstanceSocket.close();
            }
            catch (IOException e)
            {
                logger.warn("Failed to close inter instance socket: address: " + sockAddr + " " + "arguments: " + args, e);
                return LOCK_ERROR;
            }
        }

        return SUCCESS;
    }

    /**
     * We use this thread to communicate with an already running instance of
     * Jitsi. This thread will listen for a reply to a message that we've
     * sent to the other instance. We will wait for this message for a maximum
     * of <tt>runDuration</tt> milliseconds and then consider the remote
     * instance dead.
     */
    private class LockClient extends Thread
    {
        /**
         * The <tt>String</tt> that we've read from the socketInputStream
         */
        public String message = null;

        /**
         * The socket that this <tt>LockClient</tt> is created to read from.
         */
        private Socket interInstanceSocket = null;

        /**
         * Creates a <tt>LockClient</tt> that should read whatever data we
         * receive on <tt>sockInputStream</tt>.
         *
         * @param commSocket the socket that this client should be reading from.
         */
        public LockClient(Socket commSocket)
        {
            super(LockClient.class.getName());
            setDaemon(true);
            this.interInstanceSocket = commSocket;
        }

        /**
         * Blocks until a reply has been received or until <tt>runDuration</tt> milliseconds have passed.
         *
         * @param runDuration the number of seconds to wait for a reply from the remote instance
         */
        public void waitForReply(long runDuration)
        {
            try
            {
                synchronized(this)
                {
                    // return if we have already received a message.
                    if (message != null)
                    {
                        return;
                    }

                    wait(runDuration);
                }

                logger.debug("Done waiting");
            }
            catch (Exception exception)
            {
                logger.error("Failed to close our inter instance input stream", exception);
            }
        }

        /**
         * Simply collects everything that we read from the InputStream that
         * this <tt>InterInstanceCommunicationClient</tt> was created with.
         */
        public void run()
        {
            try
            {
                BufferedReader lineReader = new BufferedReader(new InputStreamReader(interInstanceSocket.getInputStream()));

                // We only need to read a single line and then bail out.
                message = lineReader.readLine();
                logger.debug("Message is " + message);
                synchronized(this)
                {
                    notifyAll();
                }
            }
            catch (IOException exc)
            {
                // does not necessarily mean something is wrong. Could be
                // that we got tired of waiting and want to quit.
                logger.info("An IOException is thrown while reading sock", exc);
            }
        }
    }

    /**
     * We start this thread when running Jitsi as a means of notifying others that this is
     */
    private class LockServer extends Thread
    {
        private boolean keepAccepting = true;

        /**
         * The socket that we use for cross instance lock and communication.
         */
        private ServerSocket lockSocket = null;

        /**
         * Creates an instance of this <tt>LockServer</tt> wrapping the
         * specified <tt>serverSocket</tt>. It is expected that the serverSocket
         * will be already bound and ready to accept.
         *
         * @param serverSocket the serverSocket that we should use for inter instance communication.
         */
        public LockServer(ServerSocket serverSocket)
        {
            super("LockServer" + serverSocket.getLocalSocketAddress());
            setDaemon(true);
            this.lockSocket = serverSocket;
        }

        public void run()
        {
            try
            {
                while (keepAccepting)
                {
                    Socket instanceSocket = lockSocket.accept();
                    new LockServerConnectionProcessor(instanceSocket).start();
                }
            }
            catch (Exception exc)
            {
                logger.error("Something went wrong during LockServer run.", exc);
            }
        }
    }

    /**
     * We use this thread to handle individual messages in server side inter
     * instance communication.
     */
    private static class LockServerConnectionProcessor extends Thread
    {
        /**
         * The socket that we will be using to communicate with the fellow Jitsi
         * instance.
         */
        private final Socket connectionSocket;

        /**
         * Creates an instance of <tt>LockServerConnectionProcessor</tt> that
         * would handle parameters received through the
         * <tt>connectionSocket</tt>.
         *
         * @param connectionSocket
         *            the socket that we will be using to read arguments from
         *            the remote Jitsi instance.
         */
        public LockServerConnectionProcessor(Socket connectionSocket)
        {
            this.connectionSocket = connectionSocket;
        }

        /**
         * Starts reading messages arriving through the connection socket.
         */
        public void run()
        {
            InputStream is;
            PrintWriter printer;
            try
            {
                is = connectionSocket.getInputStream();
                printer = new PrintWriter(connectionSocket.getOutputStream());
            }
            catch (IOException exc)
            {
                logger.warn("Failed to read arguments from another SC instance", exc);
                return;
            }

            ArrayList<String> argsList = new ArrayList<>();

            logger.debug("Handling incoming connection");

            int argCount = 1024;
            try
            {
                BufferedReader lineReader = new BufferedReader(new InputStreamReader(is));

                while (true)
                {
                    String line = lineReader.readLine();

                    // We should always get at least one line sent - the "Arg-Count=x" line (see interInstanceConnect()).
                    // If not, handle this as per an IOException - log and return.
                    if (line == null)
                    {
                        logger.error("Received empty stream over IPC");
                        printer.print(ERROR_ARG + "=empty line read");
                        printer.close();
                        connectionSocket.close();

                        return;
                    }

                    logger.debug(sanitiseArgument(line));

                    if (line.startsWith(ARG_COUNT))
                    {
                        argCount = Integer.parseInt(line.substring((ARG_COUNT + "=").length()));
                    }
                    else if (line.startsWith(ARGUMENT))
                    {
                        String arg = line.substring((ARGUMENT + "=").length());
                        argsList.add(arg);
                    }
                    else
                    {
                        // ignore unknown headers.
                    }

                    if (argCount <= argsList.size())
                    {
                        break;
                    }
                }

                // first tell the remote application that everything went OK
                // and end the connection so that it could exit
                printer.print(ARG_COUNT + "=" + argCount + CRLF);
                printer.close();
                connectionSocket.close();

                // now let's handle what we've got
                String[] args = new String[argsList.size()];
                LaunchArgHandler.getInstance().handleConcurrentInvocationRequestArgs(argsList.toArray(args));
            }
            catch (IOException exc)
            {
                logger.info("An IOException is thrown while processing remote args", exc);
                printer.print(ERROR_ARG + "=" + exc.getMessage());
                printer.close();
            }
        }
    }

    /**
     * Determines whether or not the <tt>iface</tt> interface is a loopback
     * interface. We use this method as a replacement to the
     * <tt>NetworkInterface.isLoopback()</tt> method that only comes with
     * java 1.6.
     *
     * @param iface the inteface that we'd like to determine as loopback or not.
     *
     * @return true if <tt>iface</tt> contains at least one loopback address
     * and <tt>false</tt> otherwise.
     */
    private boolean isLoopbackInterface(NetworkInterface iface)
    {
        try
        {
            Method method = iface.getClass().getMethod("isLoopback");

            return (Boolean) method.invoke(iface, new Object[]{});
        }
        catch(Exception e)
        {
            //apparently we are not running in a JVM that supports the
            //is Loopback method. we'll try another approach.
            logger.error(e);
        }

        Enumeration<InetAddress> addresses = iface.getInetAddresses();

        if(addresses.hasMoreElements())
        {
            InetAddress address = addresses.nextElement();
            if(address.isLoopbackAddress()
                || address.getHostAddress()
                        .startsWith(WEIRD_MACOSX_LOOPBACK_ADDRESS))
                return true;
        }

        return false;
    }
}
