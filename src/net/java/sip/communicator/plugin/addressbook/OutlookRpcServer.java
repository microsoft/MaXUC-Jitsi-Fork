// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.addressbook;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.*;
import org.jitsi.service.resources.*;
import org.json.simple.*;
import org.simpleframework.http.Method;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.simpleframework.http.core.*;
import org.simpleframework.transport.*;
import org.simpleframework.transport.connect.*;

import net.java.sip.communicator.plugin.addressbook.calendar.*;
import net.java.sip.communicator.util.*;

/**
 * The Outlook RPC Server handles incoming requests from the MAPI client and
 * adds any contact events to the notification thread.
 */
public class OutlookRpcServer
{
    /**
     * The logger for this class
     */
    private static final Logger logger = Logger.getLogger(OutlookRpcServer.class);

    private static final ContactLogger contactlogger = ContactLogger.getLogger();

    /**
     * The HTTP container that is used to process HTTP requests and compose
     * HTTP responses
     */
    private Container container;

    /**
     * The HTTP server that this class creates
     */
    private Server server;

    /**
     * The socket connection to the HTTP server
     */
    private Connection connection;

    /**
     * The socket address to the HTTP server
     */
    private SocketAddress address;

    /**
     * The data handler that created us
     */
    private OutlookDataHandler outlookDataHandler;

    /**
     * The calendar data handler
     */
    private OutlookCalendarDataHandler outlookCalendarHandler;

    /**
     * The Outlook RPC Client
     */
    private OutlookRpcClient client;
    private int port;

    /**
     * Instantiates a new Outlook RPC Server
     *
     * @param parent - The contacts Data Handler that creates us
     * @param calendarDataHandler The data handler that handles calendar events
     * @param client - The RPC Client
     * @throws Exception
     */
    public OutlookRpcServer(OutlookDataHandler parent,
                            OutlookCalendarDataHandler calendarDataHandler,
                            OutlookRpcClient client)
            throws Exception
    {
        // Choose a random port to start the server on

        outlookCalendarHandler = calendarDataHandler;
        outlookDataHandler = parent;
        this.client = client;

        // Get the current application name and version so we can start a
        // server for each instance of the application
        ResourceManagementService resources =
                                    AddressBookProtocolActivator.getResources();
        String applicationName
            = resources.getSettingsString("service.gui.APPLICATION_NAME");
        String version = System.getProperty("sip-communicator.version");

        final String serverName = applicationName + "/" + version;

        // Create the HTTP container that will handle all requests and compose
        // responses
        container = new Container()
        {
            @SuppressWarnings("unchecked")
            public void handle(Request request, Response response)
            {
                try
                {
                    String path = request.getPath().getPath();
                    long now = System.currentTimeMillis();

                    // Set up the response
                    response.setValue("Content-Type", "application/json");
                    response.setValue("Server", serverName);
                    response.setDate("Date", now);
                    response.setDate("Last-Modified", now);
                    response.setValue("Allows","POST");

                    if (request.getMethod().equals(Method.PUT))
                    {
                        byte[] data = IOUtils.toByteArray(request.getInputStream());

                        String json = new String(data, StandardCharsets.UTF_8);

                        JSONObject input = (JSONObject) JSONValue.parse(json);
                        JSONObject output = new JSONObject();

                        // Determine the request type and perform the
                        // appropriate contact action
                        switch (path)
                        {
                            case "/contact/add":
                                contactOperation(input, 'i');
                                response.setStatus(Status.OK);
                                output.put("result", "success");
                                break;
                            case "/contact/update":
                                contactOperation(input, 'u');
                                response.setStatus(Status.OK);
                                output.put("result", "success");
                                break;
                            case "/contact/delete":
                                contactOperation(input, 'd');
                                response.setStatus(Status.OK);
                                output.put("result", "success");
                                break;
                            case "/client/start":
                                startClient(input);
                                response.setStatus(Status.OK);
                                output.put("result", "success");
                                break;
                            case "/mapi/status":
                                output = mapiStatus(input);
                                response.setStatus(Status.OK);
                                output.put("result", "success");
                                break;
                            case "/calendar/insert":
                                outlookCalendarHandler.eventInserted(input);
                                response.setStatus(Status.OK);
                                output.put("result", "success");
                                break;
                            default:
                                response.setStatus(Status.NOT_FOUND);
                                output.put("result", "error");
                                output.put("reason", "unknown method");
                                break;
                        }

                        contactlogger.debug("Received request for: " + path +
                                 " with data: " + json +
                                 " with response: " + output +
                                 " took " + (System.currentTimeMillis() - now));

                        PrintStream body;
                        body = response.getPrintStream();
                        body.println(output);
                        body.close();
                    }
                    else
                    {
                        // Unknown method on the request reply with Method Not
                        // Allowed
                        logger.warn("Received request for: " + path +
                              " with incorrect method: " + request.getMethod());
                        response.setStatus(Status.METHOD_NOT_ALLOWED);
                    }
                }
                catch (IOException e)
                {
                    logger.error("Failed to send HTTP response", e);
                }
                catch (Exception e)
                {
                    // We don't get logs unless exceptions are caught and logged here.
                    // Downgrade the log if we've stopped, it's an expected window condition.
                    if (outlookCalendarHandler == null)
                    {
                        logger.info("Exception dealing with request whilst stopped" + request, e);
                    }
                    else
                    {
                        logger.error("Exception dealing with request " + request, e);
                    }
                }
                finally
                {
                    try
                    {
                        response.close();
                    }
                    catch (IOException e)
                    {
                        logger.error("Error closing the response", e);
                    }
                }
            }
        };

        server = new ContainerServer(container);
        connection = new SocketConnection(server);

        start();
    }

    /**
     * Start the Outlook RPC Server
     *
     * @throws IOException
     */
    private void start() throws IOException
    {
        for (int retry = 1; retry <= 10; ++retry)
        {
            try
            {
                port = NetworkUtils.getRandomPortNumber();
                logger.info("Starting RPC Server" +
                            " on port: " + port +
                            " retry: " + retry);
                address = new InetSocketAddress("127.0.0.1", port);
                connection.connect(address);
                return;
            }
            catch (BindException e)
            {
                logger.warn("Port " + port + " in use", e);

                if (retry == 10)
                {
                    throw e;
                }
            }
        }
    }

    /**
     * Returns the port that this server is running on
     *
     * @return the port this server is running on
     */
    public int getPort()
    {
        return port;
    }

    /**
     * Notifies the Outlook Data Handler of a change in the MAPI client status
     *
     * @param input the JSONObject contained in the HTTP request
     * @return a blank JSONObject
     */
    public JSONObject mapiStatus(JSONObject input)
    {
        Long status = (Long) input.get("status");

        outlookDataHandler.setMapiStatus(status);

        return new JSONObject();
    }

    /**
     * Starts the RPC client on the given port
     *
     * @param input the port to start the RPC client on
     */
    private void startClient(JSONObject input)
    {
        String pipeName = (String) input.get("pipeName");
        logger.info("Starting client for named pipe " + pipeName);
        client.start(pipeName);
    }

    /**
     * A contact operation has been received, pass this on to the Outlook Data
     * Handler
     *
     * @param input the JSON input from the HTTP request
     * @param operation the contact operation, 'u', 'i', or 'd' for 'updated',
     * 'inserted', or 'deleted' operations
     */
    private void contactOperation(JSONObject input, char operation)
    {
        String id = (String) input.get("contact");
        String type = (String) input.get("type");

        if (outlookDataHandler.isRelevantItem(id, type))
        {
            outlookDataHandler.addNotification(id, operation);
        }
        else if (outlookCalendarHandler.isRelevantItem(id, type))
        {
            outlookCalendarHandler.addNotification(id, operation);
        }
        else
        {
            logger.debug("Ignoring message with type " + type + " and id " + id);
        }
    }

    /**
     * Stop the RPC server and close the connections
     */
    public void stop()
    {
        try
        {
            if (outlookCalendarHandler != null)
            {
                outlookCalendarHandler.stop();
                outlookCalendarHandler = null;
            }

            if (connection != null)
            {
                connection.close();
                connection = null;
            }

            if (server != null)
            {
                server.stop();
                server = null;
            }

            address = null;
        }
        catch (IOException e)
        {
            logger.error("Error stopping RPC Server: ", e);
        }
    }
}
