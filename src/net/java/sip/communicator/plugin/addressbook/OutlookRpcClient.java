// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.addressbook;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

import org.json.simple.*;

import net.java.sip.communicator.service.threading.*;
import net.java.sip.communicator.util.*;

public class OutlookRpcClient
{
    private static final ContactLogger logger = ContactLogger.getLogger();

    /**
     * If we're running in QA mode, we can log details of what the user types
     * into the search box, otherwise we can't as that may contain PII.
     */
    private final boolean mQAMode;

    private RandomAccessFile clientNamedPipe;
    private boolean started = false;
    private boolean stopped = false;
    private final OutlookDataHandler mHandler;
    private Date mGettingStateSince;
    private final Object mGetStateDumpLock = new Object();

    // The maximum number of times we should retry a request to Outlook
    private static final int MAX_RETRY_ATTEMPTS = 3;

    /**
     * Buffer size when reading AOS response from the named pipe.
     */
    private static final int PIPE_READ_BUFFER_SIZE = 16384;

    public OutlookRpcClient(OutlookDataHandler handler)
    {
        mHandler = handler;

        mQAMode = AddressBookProtocolActivator.getConfigService().global().getBoolean(
                                   "net.java.sip.communicator.QA_MODE", false);
    }

    public synchronized void start(String pipeName)
    {
        try
        {
            clientNamedPipe = new RandomAccessFile(pipeName, "rwd");
            started = true;
        }
        catch (FileNotFoundException e)
        {
            logger.error("Failed to start the client: ", e);
        }

        notifyAll();
    }

    /**
     * Notifies the client that the server is restarting and therefore will
     * be on a different port.
     */
    public synchronized void restart()
    {
        started = false;

        notifyAll();
    }

    /**
     * Sends a stop command to the native libraries to unload them.
     */
    public synchronized void stop()
    {
        if (started)
        {
            stopped = quit();

            try
            {
                clientNamedPipe.close();
            }
            catch (IOException e)
            {
                logger.error("Failed to close the client pipe: ", e);
            }
        }

        notifyAll();
    }

    /**
     * Signals to the server that it should quit.
     *
     * @return Whether the operation succeeded
     */
    public boolean quit()
    {
        JSONObject result;

        try
        {
            JSONObject args = new JSONObject();
            result = makeRequest("quit", args);
        }
        catch (Exception e)
        {
            // In most cases an exception here means the server is already terminated.
            // So we can consider "quit" was successful.
            return true;
        }

        return result.get("result").equals("success");
    }

    /**
     * Calls back to the client for each
     * <tt>MAPI_MAILUSER</tt> found in the Address Book of Microsoft Outlook
     * which matches a specific <tt>String</tt> query.
     *
     * @param query the <tt>String</tt> for which the Address Book of Microsoft
     * Outlook is to be queried. <b>Warning</b>: Ignored at the time of this
     * writing.
     */
    // JSON simple doesn't allow for generics.
    @SuppressWarnings("unchecked")
    public boolean queryContacts(String query)
    {
        logger.debug("Query contacts start");
        boolean result = false;
        try
        {
            JSONObject args = new JSONObject();
            args.put("query", query);
            JSONObject response = makeRequest("contact/query", args);
            if (response.get("result").equals("success"))
            {
                result = true;
            }
        }
        catch (Exception e)
        {
            logger.error("Failed to query for contacts: " + query, e);
        }

        logger.debug("Query contacts complete");

        return result;
    }

    /**
     * Ask the outlook server to perform a state dump.  Returns immediately if
     * a request is still in progress
     *
     * @return true if the state dump was performed.
     */
    public boolean requestStateDump()
    {
        boolean gotDump = false;
        boolean shouldRequestStateDump;
        final Date dateRequestedDump;

        synchronized (mGetStateDumpLock)
        {
            if (mGettingStateSince != null)
            {
                // Already getting the data
                shouldRequestStateDump = false;
                logger.warn("Request for state ongoing since " + mGettingStateSince);
            }
            else
            {
                shouldRequestStateDump = true;
                mGettingStateSince = new Date();
                logger.info("About to request state at " + mGettingStateSince);
            }

            dateRequestedDump = mGettingStateSince;
        }

        if (shouldRequestStateDump)
        {
            // Because requesting the state dump involves a request to the
            // Outlook server, it may take a while, particularly if the Outlook
            // server has hung.  In which case, submit it on a separate thread
            // so that we can abandon the request if it takes too long.
            Callable<Boolean> callable = new Callable<Boolean>()
            {
                @Override
                public Boolean call()
                {
                    boolean succeeded = false;

                    try
                    {
                        JSONObject response = makeRequest("dump",
                                                          new JSONObject());
                        long took = (System.currentTimeMillis() -
                                            dateRequestedDump.getTime()) / 1000;
                        logger.info("Got dump reponse " + response + ", took " +
                                                             took + " seconds");
                        succeeded = "success".equals(response.get("result"));
                    }
                    catch (IOException e)
                    {
                        // Not a lot we can do - just log.
                        logger.error("Unable to get state dump", e);
                    }
                    finally
                    {
                        synchronized (mGetStateDumpLock)
                        {
                            // Don't clear out the getting state since flag if
                            // it has been changed to refer to another get
                            // request.  This could happen if the requesting
                            // thread was interrupted.
                            if (dateRequestedDump == mGettingStateSince)
                            {
                                mGettingStateSince = null;
                            }
                        }
                    }

                    return succeeded;
                }
            };

            ThreadingService service =
                             AddressBookProtocolActivator.getThreadingService();
            Future<Boolean> future =
                  service.submit("OutlookRpcClient.requestStateDump", callable);

            try
            {
                gotDump = future.get(30, TimeUnit.SECONDS);
            }
            catch (TimeoutException e)
            {
                // Took too long - so we are still getting the request.
                logger.error("Took too long to get dump ", e);
            }
            catch (InterruptedException | ExecutionException e)
            {
                // Failed, thus aren't getting the dump any more
                logger.error("Exception getting dump", e);
                synchronized (mGetStateDumpLock)
                {
                    if (dateRequestedDump == mGettingStateSince)
                    {
                        mGettingStateSince = null;
                    }
                }
            }
        }

        return gotDump;
    }

    /**
     * Method for retrieving the Outlook properties for the given
     * Outlook identifier
     *
     * @param entryId the Outlook identifier for which to retrieve properties
     * @param propIds a list of property identifiers that we wish to retrieve
     * @param flags a bit mask for various MAPI flags
     *
     * @return a list of property values for the given Outlook ID.
     * @throws Exception
     */
    public Object[] IMAPIProp_GetProps(String entryId, long[] propIds, long flags) throws Exception
    {
        return IMAPIProp_GetProps(entryId, propIds, flags, 0);
    }

    /**
     * Method for retrieving the Outlook properties for the given
     * Outlook identifier
     *
     * @param entryId the Outlook identifier for which to retrieve properties
     * @param propIds a list of property identifiers that we wish to retrieve
     * @param flags a bit mask for various MAPI flags
     * @param folderType The folder type to get the data from.
     *
     * @return a list of property values for the given Outlook ID.
     * @throws Exception
     */
    // JSON simple doesn't allow for generics.
    @SuppressWarnings("unchecked")
    public Object[] IMAPIProp_GetProps(String entryId,
                                       long[] propIds,
                                       long flags,
                                       int folderType) throws IOException
    {
        JSONObject args = new JSONObject();
        args.put("entryId", entryId);
        args.put("type", folderType);
        JSONArray props = new JSONArray();
        for (long prop: propIds)
        {
            props.add(new Long(prop));
        }
        args.put("properties", props);
        args.put("flags", new Long(flags));
        JSONObject obj = makeRequest("props/get", args);
        JSONArray array = (JSONArray) obj.get("props");
        return array.toArray();
    }

    /**
     * Method for setting an Outlook property for the given Outlook
     * identifier
     *
     * @param propId the property identifier for the field we wish to set
     * @param value the string property value to set
     * @param entryId the Outlook contact identifier
     *
     * @return whether the operation succeeded
     */
    // JSON simple doesn't allow for generics.
    @SuppressWarnings("unchecked")
    public boolean IMAPIProp_SetPropString(long propId,
            String value,
            String entryId)
    {
        if (propId < 0 || value == null || entryId == null)
        {
            return false;
        }

        try
        {
            JSONObject args = new JSONObject();
            args.put("entryId", entryId);
            args.put("propId", propId);
            args.put("value", value);
            makeRequest("props/set", args);
            return true;
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    /**
     * Gets the default contact folder from the address book
     *
     * @return the id of the contact folder from Outlook
     */
    public String getDefaultContactsFolder()
    {
        try
        {
            JSONObject args = new JSONObject();
            JSONObject result = makeRequest("getdefaultcontactsfolder", args);

            return (String) result.get("id");
        }
        catch (Exception ex)
        {
            logger.error("Failed to get contacts folder: ", ex);
        }

        return null;
    }

    /**
    * Removes a contact from the address book.
    *
    * @param id the person id.
    *
    * @return whether the contact was successfully removed.
    */
    // JSON simple doesn't allow for generics.
    @SuppressWarnings("unchecked")
    public boolean deleteContact(String id)
    {
        try
        {
            JSONObject args = new JSONObject();
            args.put("id", id);
            makeRequest("contact/delete", args);

            return true;
        }
        catch(Exception ex)
        {
            logger.error("Failed to delete contact: " + id, ex);
            return false;
        }
    }

    /**
    * Creates an empty contact from the address book.
    *
    * @return The id of the new contact created. Or NULL if the creation
    * failed.
    */
    public String createContact()
    {
        try
        {
            JSONObject args = new JSONObject();
            JSONObject result = makeRequest("contact/add", args);

            return (String) result.get("id");
        }
        catch (Exception ex)
        {
            logger.error("Failed to create contact: ", ex);
        }

        return null;
    }

    /**
     * Gets the default calendar folder for the current Outlook user
     *
     * @return the id of the calendar folder from Outlook
     */
    public String getDefaultCalendarFolder()
    {
        try
        {
            JSONObject args = new JSONObject();
            JSONObject result = makeRequest("getdefaultcalendarfolder", args);

            return (String) result.get("id");
        }
        catch (Exception ex)
        {
            logger.error("Failed to get calendar folder: ", ex);
        }

        return null;
    }

    /**
     * Request all the calendar information from Outlook.  The details of each
     * meeting is returned separately by the Outlook native code making a request
     * of the OutlookRpcServer
     */
    public void queryCalendar()
    {
        logger.debug("Query calendar start");
        try
        {
            JSONObject args = new JSONObject();
            makeRequest("calendar/query", args);
        }
        catch (IOException e)
        {
            logger.error("Failed to get calendar info", e);
        }

        logger.debug("Query calendar complete");
    }

    /**
     * Outlook IDs can vary over the lifetime of an object, thus this method
     * compares one ID with a list of other IDs to see if it matches any of them
     *
     * @param id The id to compare with
     * @param otherIds The list of ids to compare against
     * @return The index in the list of other ids of the match or -1 if there
     *         was no match.
     */
    // JSON simple doesn't allow for generics.
    @SuppressWarnings("unchecked")
    public int compareIds(String id, Collection<String> otherIds)
    {
        try
        {
            JSONObject args = new JSONObject();

            JSONArray otherIdsJson = new JSONArray();
            for (String otherId : otherIds)
            {
                otherIdsJson.add(otherId);
            }

            args.put("id", id);
            args.put("otherIds", otherIdsJson);

            JSONObject output = makeRequest("compareids", args);
            int result = ((Long)output.get("match")).intValue();

            return result;
        }
        catch (IOException e)
        {
            logger.error("Failed to compare ids");
            return -1;
        }
    }

    /**
     * Make a request over the API
     *
     * @param command - the API method to call
     * @param data - the arguments to pass to the API, as a JSON Object
     * @return Result, as a JSON object
     * @throws Exception
     */
    private JSONObject makeRequest(String command, JSONObject data)
        throws IOException
    {
        JSONObject result;
        long now = System.currentTimeMillis();
        // Redact the PII if necessary
        String dataToLog = mQAMode ? data.toString() : "<redacted>";
        String resultToLog = "null";

        try
        {
            String response = null;
            IOException ex = null;

            for(int retriesLeft = MAX_RETRY_ATTEMPTS; retriesLeft > 0; retriesLeft --)
            {
                ex = null;

                // Wait for the client to be started by the server.
                synchronized(this)
                {
                    while (! started && ! stopped)
                    {
                        try
                        {
                            this.wait();
                        }
                        catch (InterruptedException e)
                        {
                            // Not a lot to do...
                            logger.error("Interrupt exception", e);
                        }
                    }
                }

                if (stopped)
                {
                    throw new IOException("Outlook connection was stopped.");
                }

                long lastFailure = mHandler.getLastFailure();

                try
                {
                    response = sendRequest(command, data);
                    if (response != null)
                    {
                        break;
                    }
                }
                catch (Exception e)
                {
                    ex = new IOException(e);

                    // A socket exception indicates a failure to communicate with the Outlook Server.
                    logger.error("Failed to communicate with the Outlook Server", e);

                    // If the Outlook server has restarted since we started the request
                    // or is currently dead, retry the request
                    if (mHandler.hasServerFailed(lastFailure))
                    {
                        // The Outlook server has failed since we started the request - retry
                    }
                    else
                    {
                        // We don't think it was e
                        break;
                    }
                }
            }

            // We've either retired three times, and failed each time,
            // or we've failed without an explainable error.
            if (ex != null)
            {
                // This will have been logged further up
                throw ex;
            }

            // We should never hit this - if the response is null, then we should have
            // hit an exception above.
            if (response == null)
            {
                ex = new IOException("Unexpected failure to retrieve response from Outlook");
                logger.error("Failed: ", ex);
                throw ex;
            }

            result = (JSONObject) JSONValue.parse(response);
            if (result != null)
            {
                // Redact the PII if necessary
                resultToLog = mQAMode ? result.toString() : "<redacted>";

                if (result.get("result") == null ||
                    !result.get("result").equals("success"))
                {
                    IOException e = new IOException("Request: " + command +
                        " with data: " + dataToLog +
                        " returned: " + resultToLog +
                        " with reason:"  + result.get("reason"));

                    logger.warn(e);

                    throw e;
                }
            }

            return result;
        }
        finally
        {
            logger.debug("Request " + command + " took " + (System.currentTimeMillis() - now));
        }
    }

    /**
     * Writes request data to the named pipe and read the response.
     * @param command method to call
     * @param data input data
     * @return response string
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    private String sendRequest(String command, JSONObject data) throws IOException
    {
        JSONObject request = new JSONObject();
        request.put("command", "/" + command);
        request.put("data", data);

        ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();

        synchronized(this)
        {
            clientNamedPipe.write(request.toString().getBytes(StandardCharsets.UTF_8));

            int bytesRead;
            byte[] buf = new byte[PIPE_READ_BUFFER_SIZE];

            do
            {
                bytesRead = clientNamedPipe.read(buf);
                if (bytesRead > 0)
                {
                    responseBuffer.write(buf, 0, bytesRead);
                }
            } while (bytesRead == buf.length);
        }

        if (responseBuffer.size() > 0)
        {
            return responseBuffer.toString(StandardCharsets.UTF_8);
        }

        return null;
    }
}
