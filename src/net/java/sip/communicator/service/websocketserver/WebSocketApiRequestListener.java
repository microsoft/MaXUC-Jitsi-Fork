// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.websocketserver;

/**
 * This interface should be implemented by listeners used for processing
 * responses from WebSocket API outbound requests.
 *
 * Details about the WebSocket API and HLD of the code can be found on SharePoint
 * in https://[indeterminate link]
 */
public interface WebSocketApiRequestListener
{
  /**
   * Process the response to the outbound request. Implementing methods should
   * take care of extracting the specific data fields they need from the message
   * and raising any API errors if that is not possible.
   *
   * @param success Whether the request was successful.
   * @param responseMessage The WebSocket API message map.
   */
  void responseReceived(boolean success, WebSocketApiMessageMap responseMessage);

  /**
   * Process a request termination - this could be e.g. due to getting a
   * timeout or receiving an error response for the request. In the latter case,
   * there is no need to provide the error response to the implementing method
   * as API errors received are only logged, but not processed further.
   */
  void requestTerminated();
}
