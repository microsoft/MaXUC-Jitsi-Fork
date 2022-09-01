// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.websocketserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Used to store WebSocket API constants and fixed strings - see the API
 * specification for more information.
 *
 * Details about the WebSocket API and HLD of the code can be found on SharePoint
 * in https://[indeterminate link]
 */
public class WebSocketApiConstants
{
  // The following section covers the types of messages that can be received
  // over the API.

  /** Allowed values for the 'message' field of API messages. */
  public static final String WS_API_MESSAGE_VARIANT_REQUEST="request";
  public static final String
          WS_API_MESSAGE_VARIANT_REQUEST_RESPONSE="requestResponse";
  public static final String WS_API_MESSAGE_VARIANT_EVENT="event";
  public static final String WS_API_MESSAGE_VARIANT_ERROR="error";
  public static final List<String> WebSocketApiMessageVariants =
          new ArrayList<>(Arrays.asList(
                  WS_API_MESSAGE_VARIANT_REQUEST,
                  WS_API_MESSAGE_VARIANT_REQUEST_RESPONSE,
                  WS_API_MESSAGE_VARIANT_EVENT,
                  WS_API_MESSAGE_VARIANT_ERROR));

  /** Allowed values for the 'type' field of inbound request API messages. */
  public static final String
          WS_API_INBOUND_REQUEST_CONFIG_INIT="configInit";
  public static final String
          WS_API_INBOUND_REQUEST_START_CALL="startCall";
  public static final List<String> WS_API_INBOUND_REQUESTS =
          new ArrayList<>(Arrays.asList(
                  WS_API_INBOUND_REQUEST_CONFIG_INIT,
                  WS_API_INBOUND_REQUEST_START_CALL));

  /** Allowed values for the 'type' field of outbound request API messages. */
  public static final String
          WS_API_OUTBOUND_REQUEST_DN_LOOKUP ="dnLookup";
  public static final String
          WS_API_OUTBOUND_REQUEST_LAUNCH_CRM_INTEGRATION="launchCrmIntegration";

  /** Allowed values for the 'type' field of (outbound) API event messages. */
  public static final String
          WS_API_EVENT_INTEGRATION_DISABLED="integrationDisabled";

  /** Allowed values for the 'type' field of (outbound) API error messages. */
  public static final String
          WS_CONFIG_INIT_ERROR ="configInitError";
  public static final String
          WS_AUTH_ERROR ="authError";
  public static final String
          WS_VERSION_ERROR ="versionError";
  public static final String
          WS_OPERATION_COMPAT_ERROR ="operationCompatError";
  public static final String
          WS_UNSUPPORTED_OPERATION_ERROR ="unsupportedOperationError";
  public static final String
          WS_JSON_ERROR ="jsonError";
  public static final String
          WS_MESSAGE_FORMAT_ERROR ="messageFormatError";

  /** Map between API error types and the associated error reasons. */
  static final Map<String, String>
      WEBSOCKET_API_ERROR_REASON = new HashMap<>();
  static
  {
    WEBSOCKET_API_ERROR_REASON.put(
            WS_CONFIG_INIT_ERROR,
            "No initial configuration provided");
    WEBSOCKET_API_ERROR_REASON.put(
            WS_AUTH_ERROR,
            "Incorrect applicationId or userId provided in initial configuration");
    WEBSOCKET_API_ERROR_REASON.put(
            WS_VERSION_ERROR,
            "The server does not support any of the specified API versions");
    WEBSOCKET_API_ERROR_REASON.put(
            WS_OPERATION_COMPAT_ERROR,
            "The supported operations do not match the minimal required list");
    WEBSOCKET_API_ERROR_REASON.put(
            WS_UNSUPPORTED_OPERATION_ERROR,
            "The requested operation is not supported");
    WEBSOCKET_API_ERROR_REASON.put(
            WS_JSON_ERROR,
            "The received message was not a valid JSON object");
    WEBSOCKET_API_ERROR_REASON.put(
            WS_MESSAGE_FORMAT_ERROR,
            "There was a problem with the \'%s\' parameter");
  }

  /**
   * Stores the names of all API fields, and the type of the associated field
   * values.
   */
  public enum WebSocketApiMessageField
  {
    // Mandatory fields of every message
    MESSAGE_KEY("message", String.class),
    TYPE_KEY("type", String.class),
    ID_KEY("id", String.class),
    DATA_KEY("data", Map.class),
    // The remaining fields are all found in the JSON object under the 'data'
    // field.
    // Data fields common to all response messages.
    // Mandatory
    RESPONSE_SUCCESS("success", Boolean.class),
    // Optional - used for unsuccessful responses
    FAILURE_REASON("failureReason", String.class),
    // Data field specific to error messages
    ERROR_REASON("reason", String.class),
    // Data fields specific to 'configInit' type *requests*
    CONFIG_INIT_APPLICATION_ID("applicationId", String.class),
    CONFIG_INIT_APPLICATION_NAME("applicationName", String.class),
    CONFIG_INIT_USER_ID("userId", String.class),
    CONFIG_INIT_API_VERSIONS("versions", List.class),
    CONFIG_INIT_SUPPORTED_OPERATIONS("supportedOperations", List.class),
    // Data field specific to 'configInit' type *responses* - note that the
    // CONFIG_INIT_SUPPORTED_OPERATIONS field is also used in responses, as well
    // as requests
    CONFIG_INIT_API_VERSION("version", String.class),
    DN_LOOKUP_NUMBER("number", String.class),
    DN_LOOKUP_DISPLAY_NAME("displayName", String.class),
    LAUNCH_CRM_INTEGRATION_NUMBER("number", String.class),
    START_CALL_TO_NUMBER("toNumber", String.class);

    private String mApiFieldName;
    private Class mApiFieldType;

    WebSocketApiMessageField(String apiFieldName, Class apiFieldType)
    {
      mApiFieldName = apiFieldName;
      mApiFieldType = apiFieldType;
    }

    @Override
    public String toString()
    {
      return mApiFieldName;
    }

    public Class getType()
    {
      return mApiFieldType;
    }
  }

  // The following section covers Accession start-of-day configuration for
  // the WebSocket API.

  /** List of supported API versions, in ascending order. */
  public static final List<String> SUPPORTED_API_VERSIONS =
      new ArrayList<>(List.of("V1.0"));
  // Sort the list in ascending order - a simple lexicographic string comparison
  // is enough to do the job.
  static
  {
    SUPPORTED_API_VERSIONS.sort(String::compareTo);
  }

  /** List of Accession Desktop's supported API operations. */
  public static final List<String> ACCESSION_SUPPORTED_API_OPERATIONS =
      new ArrayList<>(Arrays.asList(
          // Inbound requests - ignore configInit, as that is a part of the
          // API infrastructure, rather than a normal API request.
          WS_API_INBOUND_REQUEST_START_CALL,
          // Outbound requests
          WS_API_OUTBOUND_REQUEST_DN_LOOKUP,
          WS_API_OUTBOUND_REQUEST_LAUNCH_CRM_INTEGRATION,
          // Events
          WS_API_EVENT_INTEGRATION_DISABLED));

  /**
   * List of the minimum API operations that the connecting application
   * must support.
   */
  public static final List<String> MINIMUM_REQUIRED_API_OPERATIONS =
      new ArrayList<>(Arrays.asList(
          // Requests
          WS_API_OUTBOUND_REQUEST_DN_LOOKUP,
          WS_API_OUTBOUND_REQUEST_LAUNCH_CRM_INTEGRATION));

  /** List of trusted 3-rd party application IDs. */
  public static final List<String> WEBSOCKET_TRUSTED_APPLICATIONS =
          new ArrayList<>(Arrays.asList(
                  // Mondago GoCommunicator ID
                  "d5b31815-d6b5-4d9c-a872-fbca779bb5d2",
                  // CloudCTI ID
                  "854d72ca-4444-4429-8ee1-a7a924a77bd9",
                  // Reserved ID 1
                  "623d1635-1423-46d8-b1cb-4383431313f1",
                  // Reserved ID 2
                  "d3ef53a3-fac3-4c49-aa7a-e6ff4b654d17",
                  // Reserve ID 3
                  "e7212527-3b35-4056-bbdd-fa987e2fe023"));

  // The following section covers miscellaneous WebSocket server constants.

  /** The timeout period used for all outbound requests, in milliseconds. */
  public static final int OUTBOUND_REQUEST_TIMEOUT_PERIOD = 10000;

  /**
   * The timeout period before closing a new connection if no configuration
   * has been provided by the connected application.
   */
  public static final int CONFIG_INIT_TIMEOUT_PERIOD = 2000;

  /**
   * The interval used for checking the activity of an open connection, in
   * seconds.
   */
  public static final int CONNECTION_CHECK_INTERVAL = 30;

  /** The port that the WebSocket listens on. */
  public static final int WEBSOCKET_PORT = 8443;

  /** The address to listen on - specifically the IPv4 localhost IP. */
  public static final String WEBSOCKET_IP = "127.0.0.1";
}
