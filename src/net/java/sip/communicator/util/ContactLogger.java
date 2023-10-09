// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import java.util.logging.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * A logging class for logging Contact information to a separate file
 */
public class ContactLogger
{

    /**
     * The java.util.Logger that actually does the logging
     */
    private static final java.util.logging.Logger sLoggerDelegate
                    = java.util.logging.Logger.getLogger("jitsi.ContactLogger");

    private static final ContactLogger sLogger = new ContactLogger();

    /**
     * Get an instance of the contact logger for the passed in class
     *
     * @return a contact logger
     */
    public static ContactLogger getLogger()
    {
        return sLogger;
    }

    public void note(Object msg)
    {
        logContact(null, msg, null, Logger.NOTE);
    }

    public void note(MetaContact metaContact, Object msg)
    {
        logMetaContact(metaContact, msg, null, Logger.NOTE);
    }

    public void note(MetaContact metaContact, Object msg, Throwable t)
    {
        logMetaContact(metaContact, msg, t, Logger.NOTE);
    }

    public void note(Contact contact, Object msg)
    {
        logContact(contact, msg, null, Logger.NOTE);
    }

    public void note(Contact contact, Object msg, Throwable t)
    {
        logContact(contact, msg, t, Logger.NOTE);
    }

    public void trace(MetaContact metaContact, Object msg)
    {
        logMetaContact(metaContact, msg, null, Level.FINER);
    }

    public void trace(MetaContact metaContact, Object msg, Throwable t)
    {
        logMetaContact(metaContact, msg, t, Level.FINER);
    }

    public void trace(Object msg)
    {
        logContact(null, msg, null, Level.FINER);
    }

    public boolean isDebugEnabled()
    {
        return sLoggerDelegate.isLoggable(Level.FINE);
    }

    public void debug(MetaContact metaContact, Object msg)
    {
        logMetaContact(metaContact, msg, null, Level.FINE);
    }

    public void debug(Contact contact, Object msg)
    {
        logContact(contact, msg, null, Level.FINE);
    }

    public void debug(Object msg)
    {
        logContact(null, msg, null, Level.FINE);
    }

    public void debug(MetaContact metaContact, Object msg, Throwable t)
    {
        logMetaContact(metaContact, msg, t, Level.FINE);
    }

    public void info(MetaContact metaContact, Object msg)
    {
        logMetaContact(metaContact, msg, null, Level.INFO);
    }

    public void info(Contact contact, Object msg)
    {
        logContact(contact, msg, null, Level.INFO);
    }

    public void info(Object msg)
    {
        logContact(null, msg, null, Level.INFO);
    }

    public void info(MetaContact metaContact, Object msg, Throwable t)
    {
        logMetaContact(metaContact, msg, t, Level.INFO);
    }

    public void warn(Object msg)
    {
        logMetaContact(null, msg, null, Level.WARNING);
    }

    public void warn(Object msg, Throwable t)
    {
        logMetaContact(null, msg, t, Level.WARNING);
    }

    public void warn(MetaContact metaContact, Object msg)
    {
        logMetaContact(metaContact, msg, null, Level.WARNING);
    }

    public void warn(MetaContact metaContact, Object msg, Throwable t)
    {
        logMetaContact(metaContact, msg, t, Level.WARNING);
    }

    public void warn(Contact contact, Object msg)
    {
        logContact(contact, msg, null, Level.WARNING);
    }

    public void warn(Contact contact, Object msg, Throwable t)
    {
        logContact(contact, msg, t, Level.WARNING);
    }

    public void error(MetaContact metaContact, Object msg)
    {
        logMetaContact(metaContact, msg, null, Level.SEVERE);
    }

    public void error(Object msg)
    {
        logContact(null, msg, null, Level.SEVERE);
    }

    public void error(Object msg, Throwable t)
    {
        logContact(null, msg, t, Level.SEVERE);
    }

    public void error(MetaContact metaContact, Object msg, Throwable t)
    {
        logMetaContact(metaContact, msg, t, Level.SEVERE);
    }

    public void error(Contact contact, Object msg)
    {
        logContact(contact, msg, null, Level.SEVERE);
    }

    public void error(Contact contact, Object msg, Throwable t)
    {
        logContact(contact, msg, t, Level.SEVERE);
    }

    /**
     * Actually log the message
     *
     * @param metaContact An optional meta contact, the details of which will
     *                    be logged if present
     * @param msg The log message
     * @param t An optional throwable to log too
     * @param level The level to log the message at
     */
    private void logMetaContact(MetaContact metaContact,
                                Object msg,
                                Throwable t,
                                Level level)
    {
        if (!sLoggerDelegate.isLoggable(level))
            return;

        String message;
        if (metaContact != null)
            message = "<" + metaContact + "> " + String.valueOf(msg);
        else
            message = String.valueOf(msg);

        if (t == null)
            sLoggerDelegate.log(level, message);
        else
            sLoggerDelegate.log(level, message, t);
    }

    /**
     * Actually log the message
     *
     * @param contact An optional contact, the details of which will
     *                    be logged if present
     * @param msg The log message
     * @param t An optional throwable to log too
     * @param level The level to log the message at
     */
    private void logContact(Contact contact,
                            Object msg,
                            Throwable t,
                            Level level)
    {
        if (!sLoggerDelegate.isLoggable(level))
            return;

        String message;
        if (contact != null)
            message = contact + " " +
                      String.valueOf(msg);
        else
            message = String.valueOf(msg);

        if (t == null)
            sLoggerDelegate.log(level, message);
        else
            sLoggerDelegate.log(level, message, t);
    }
}
