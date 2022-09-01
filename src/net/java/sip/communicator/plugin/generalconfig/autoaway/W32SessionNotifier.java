// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.generalconfig.autoaway;

import org.jitsi.util.*;

import com.sun.jna.*;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef.*;

/**
 * A JNA-class wrapped Windows application whose sole purpose is to receive
 * Windows session change messages so that Accession Desktop can determine
 * when the screen has (un)locked.
 *
 * The class is very much structured like an old style Windows-32 application,
 * which is how the JNA is designed.
 *
 * The resulting application is completely hidden with no visible windows and
 * just sits waiting for notifications until told to terminate.
 */
public class W32SessionNotifier
    implements WinUser.WindowProc, Runnable
{
    static final Logger sLog = Logger.getLogger(W32SessionNotifier.class);

    // WM_USER is not defined by JNA but we want a user message to allow us to
    // end this thread.  WM_USER defines the start of a range of values that
    // are available for user define messages within the Windows architecture.
    // Typically each program defines its own set of messages starting from
    // WM_USER upwards.
    //
    // Note that messages do not normally flow between programs so every
    // program is safe using the same value(s) as any other program.
    static final int WM_USER = 0x0400;
    static final int WM_USER_STOP = (WM_USER + 1);

    // Global Variables for the instance and window.
    private final WinDef.HINSTANCE hInst = new HINSTANCE();
    private WinDef.HWND hWnd = null;

    // The parent SessionNotifier and the name of Windows class used to define
    // this 'application'.
    private AutoAwayWatcher mAutoAwayWatcher = null;
    private String myClass = this.getClass().toString();

    W32SessionNotifier(AutoAwayWatcher autoAwayWatcher)
    {
        sLog.info("AutoAway watcher");
        mAutoAwayWatcher = autoAwayWatcher;
    }

    public void run()
    {
        /**
         * Launch the Windows 'application' which is minimized and completely
         * hidden but, importantly, still receives system messages.
         */
        sLog.info("Starting W32SessionNotifier.");
        if (registerClass())
        {
            try
            {
                // Perform application initialization:
                if (initInstance())
                {
                    // Main message loop.  This blocks on GetMessage() and
                    // processes messages as they are received.  The processing
                    // callback below can post the 'Quit' message which has value
                    // 0 to break the loop.
                    sLog.debug("Entering message loop");

                    WinUser.MSG lpMsg = new WinUser.MSG();

                    // This sequence of Translate/Dispatch Message is standard
                    // Windows and typically allows translation of keyboard
                    // presses.  This does no harm even when we are not using
                    // keyboard input so we have left this as 'as standard
                    // Windows as possible'.
                    while (0 != User32.INSTANCE.GetMessage(lpMsg, hWnd, 0, 0))
                    {
                        User32.INSTANCE.TranslateMessage(lpMsg);
                        User32.INSTANCE.DispatchMessage(lpMsg);
                    }
                }
                else
                {
                    sLog.error("initInstance failed.");
                }
            }
            catch (java.lang.UnsatisfiedLinkError e)
            {
                sLog.warn("Exception initializing auto-away watcher. " +
                          "This is expected when running in ant.");
            }
        }
        else
        {
            sLog.error(
                "RegisterClassEx failed: " + Kernel32.INSTANCE.GetLastError());
        }
    }

    /**
     * Process a session change notification.
     *
     * @param sessionChange - The session change being notified.
     */
    private void handleSessionChange(int sessionChange)
    {
        sLog.info("Session change: " + sessionChange);
        switch (sessionChange) {

        case Wtsapi32.WTS_SESSION_LOCK:
            sLog.debug("Session lock");
            mAutoAwayWatcher.changeGlobalAwayState(true);
            break;

        case Wtsapi32.WTS_SESSION_UNLOCK:
            sLog.debug("Session unlock");
            mAutoAwayWatcher.changeGlobalAwayState(false);
            break;

        default:
            sLog.debug("Session change: " + sessionChange);
            break;
        }
    }

    /**
     * The message processing callback which in a pure Windows application
     * would be the WndProc() function.
     *
     * @param hWnd - Identifies this window.
     * @param message - Identifes the type of message being processed.
     * @param wParam - Additional information associated with some message
     *     types.
     *
     * @return 0 if the message was processed else the message to pass on
     *     and process elsewhere.
     */
    public LRESULT callback(HWND hWnd, int message, WPARAM wParam,
        LPARAM lParam)
    {
        sLog.info("Message: " + message);
        switch (message)
        {
        // Session change and potential (un)lock notification.
        case WinUser.WM_SESSION_CHANGE:
            handleSessionChange(wParam.intValue());
            return new WinDef.LRESULT(0L);

        // Regular Windows destroy but also our user 'Java says stop' message
        // both cause this thread to terminate.
        case WinUser.WM_DESTROY:
        case WM_USER_STOP:
            sLog.info("Destroy/Stop");
            Wtsapi32.INSTANCE.WTSUnRegisterSessionNotification(hWnd);
            User32.INSTANCE.PostQuitMessage(0);
            break;

        // Windows itself may process messages that this application doesn't
        // case about so other messages are passed to the Windows default
        // message processor.
        default:
            sLog.debug("Message: " + message);
            return User32.INSTANCE.DefWindowProc(
                hWnd, message, wParam, lParam);
        }
        return new WinDef.LRESULT(0L);
    }

    /**
     * registerClass() registers the definition of the Windows 'application'
     * including the callback that processes messages.
     *
     * The name of this method derives from standard Win32 naming. You might
     * also see it named 'MyRegisterClass()' in some Windows examples.
     *
     * @return 0 is error else an ATOM.
     */
    private boolean registerClass()
    {
        WinUser.WNDCLASSEX wcex = new WinUser.WNDCLASSEX();

        sLog.info("Register Window class");

        /**
         * The only interesting fields are the hInstances, the lpfnWndProc
         * that provides the message processing look and the name of the class
         * being registered.  But also beware of the size() which is unusual
         * in Java but required for the JNA interface.
         */
        wcex.cbSize = wcex.size();
        wcex.style = 0;
        wcex.lpfnWndProc = this;
        wcex.cbClsExtra = 0;
        wcex.cbWndExtra = 0;
        wcex.hInstance = hInst;
        wcex.hIcon = null;
        wcex.hCursor = null;
        wcex.hbrBackground = null;
        wcex.lpszMenuName = null;
        wcex.lpszClassName = myClass;
        wcex.hIconSm = null;

        return(User32.INSTANCE.RegisterClassEx(wcex) != new WinDef .ATOM(0));
    }

    /**
     * InitInstance creates main window, registers for session notifications
     * (lock/unlock events) and completes window initialization.
     *
     * @return true if successful else false.
     */
    private boolean initInstance()
    {
        boolean rc = true;

        sLog.info("Initialize W32SessionNotifier");

        // Create the window based on the class that we have created earlier.
        //
        // - myClass is registered above and the main purpose is to provide
        //   a Window smessage processing loop.
        // - WS_MINIMIZE forces any attempted Window to be minimized but...
        // - HWND_MESSAGE also forces an 'application' that is hidden but
        //   still receives Windows messages, such as the ones sent to all
        //   Windows programs when the screen is (un)locked.
        // - Everything else is defaulted because we don't care!
        //
        hWnd = User32.INSTANCE.CreateWindowEx(
            0,
            myClass,
            "",
            WinUser.WS_MINIMIZE,
            0, 0, 0, 0,
            WinUser.HWND_MESSAGE,
            null,
            hInst,
            null);

        if (hWnd == null)
        {
            sLog.error(
                "null windows handle: " + Kernel32.INSTANCE.GetLastError());
            rc = false;
        }
        else
        {
            // Register for the session notifications i.e. (un)lock.
            if (!Wtsapi32.INSTANCE.WTSRegisterSessionNotification(hWnd,
                Wtsapi32.NOTIFY_FOR_ALL_SESSIONS))
            {
                sLog.error("WTSRegisterSessionNotification failed: "
                    + Kernel32.INSTANCE.GetLastError());
                rc = false;
            }
            else
            {
                User32.INSTANCE.ShowWindow(hWnd, WinUser.SW_HIDE);
                User32.INSTANCE.UpdateWindow(hWnd);

                sLog.debug("InitInstance succeeded");
            }
        }
        return rc;
    }

    /**
     * Called to stop this Windows application, typically when Accession
     * Desktop is terminating.  Signal the Windows application to terminate
     * using a user-specific Windows message.
     */
    public void stop()
    {
        // We could probably have posted WM_DESTROY but that's normally
        // considered bad-form as WM_DESTROY is something that only Windows
        // itself should be posting.  So we use our own user-defined message.
        sLog.info("Stopping thread");
        User32.INSTANCE.PostMessage(hWnd, WM_USER_STOP, new WPARAM(0),
            new LPARAM(0));
    }
}
