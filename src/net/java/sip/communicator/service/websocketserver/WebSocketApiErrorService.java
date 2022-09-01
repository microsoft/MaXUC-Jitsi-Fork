// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.websocketserver;

/**
 * Service used for raising WebSocket API errors.
 *
 * Details about the WebSocket API and HLD of the code can be found on SharePoint
 * in https://[indeterminate link]
 */
public interface WebSocketApiErrorService
{
  /**
   * Raise a WebSocket API error.
   *
   * @param error The error to raise.
   */
  void raiseError(WebSocketApiError error);
}
