package net.java.sip.communicator.plugin.addressbook;

import net.java.sip.communicator.util.Logger;

/**
 * Wrapper around the native methods used by OutlookDataHandler to allow UT of that class.
 */
class OutlookServerNative
{
    // We need this logger variable for the native code.
    private static final Logger logger = Logger.getLogger(OutlookServerNative.class);

    /**
     * Native method to initialize the native libraries
     *
     * @param port The port the RPC Server is running on
     * @param serverLogFile The string path to the server log file
     * @param clientLogFile The string path to the client log file
     * @throws OutlookMAPIHResultException
     */
    private static native void Initialize(int port,
                                          String serverLogFile,
                                          String clientLogFile,
                                          String logDir)
                                          throws Exception;

    static void nativeInitialize(int port,
                                 String serverLogFile,
                                 String clientLogFile,
                                 String logDir)
                                 throws Exception
    {
        Initialize(port, serverLogFile, clientLogFile, logDir);
    }

    /**
     * Native method to check whether the server is still alive
     *
     * @return true if it is alive, false otherwise
     */
    private static native boolean CheckServerIsAlive();

    static boolean nativeCheckServerIsAlive()
    {
        return CheckServerIsAlive();
    }

    /**
     * Native method to uninitialize the native libraries
     */
    private static native void Uninitialize();

    static void nativeUninitialize()
    {
        Uninitialize();
    }

    /**
     * Native method to retrieve the bitness of the installed version of Outlook
     */
    private static native int getOutlookBitnessVersion();

    static int nativeGetOutlookBitnessVersion()
    {
        return getOutlookBitnessVersion();
    }

    /**
     * Native method to retrieve the version of Outlook that is installed
     */
    public static native int getOutlookVersion();

    static int nativeGetOutlookVersion()
    {
        return getOutlookVersion();
    }

    static void loadOutlookLibrary()
    {
        System.loadLibrary("jmsoutlookaddrbook");
    }
}
