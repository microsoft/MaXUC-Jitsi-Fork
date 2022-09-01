// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.websocketserver;

import java.util.Map;

/**
 * Helper service to indicate the ability to process incoming WebSocket API
 * requests. It should not be used directly - instead all specific request
 * handler services (for the different types of function requested) should be
 * created as abstract classes implementing this interface, with
 * (abstract) methods  for each type of request.
 *
 * Details about the WebSocket API and HLD of the code can be found on SharePoint
 * in https://[indeterminate link]
 */
public interface WebSocketApiRequestHandlerService
{
  /**
   * Process an incoming API request.
   *
   * @param requestType The 'type' of the request message.
   * @param requestMessage The WebSocket API request message map.
   * @return The 'data' to include in the response message, or null if an
   * error is being raised by the implementing method
   * (via the WebSocketApiErrorService).
   */
  Map<String, Object> handleApiRequest(
          String requestType,
          WebSocketApiMessageMap requestMessage);
}
