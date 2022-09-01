// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.websocketserver;

import java.util.Map;

/**
 * Operation set for WebSocket API telephony-related inbound requests.
 *
 * Details about the WebSocket API and HLD of the code can be found on SharePoint
 * in https://[indeterminate link]
 */
public abstract class WebSocketApiRequestHandlerTelephonyService implements
    WebSocketApiRequestHandlerService
{
  /**
   * Processes the inbound request (by parsing it and passing it to the
   * appropriate method defined below). Only returns the 'data' field of the
   * response message as all other fields will be known by the calling object.
   *
   * @param requestType The type of the API request.
   * @param requestMessage The WebSocket API request message map.
   * @return The 'data' to include in the response message (could be
   * an empty string), or null if an error is being raised by the implementing
   * method (via the WebSocketApiErrorService).
   */
  @Override
  public abstract Map<String, Object> handleApiRequest(
          String requestType,
          WebSocketApiMessageMap requestMessage);

  /**
   * Starts an outbound call to the given DN.
   *
   * @param requestMessage The WebSocket API request message map.
   * @return The 'data' to include in the response message.
   */
  protected abstract Map<String, Object> startCall(WebSocketApiMessageMap requestMessage);
}
