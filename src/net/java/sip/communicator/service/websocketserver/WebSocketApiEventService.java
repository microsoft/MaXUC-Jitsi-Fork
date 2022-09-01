// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.websocketserver;

import java.util.Map;

/**
 * Service used for broadcasting WebSocket API events.
 *
 * Details about the WebSocket API and HLD of the code can be found on SharePoint
 * in https://[indeterminate link]
 */
public interface WebSocketApiEventService
{
  /**
   * Send an outbound event.
   *
   * @param event The type of event to send.
   * @param messageDataMap The 'data' field to include in the event message.
   */
  void broadcastApiEvent(String event,
                         Map<String, Object> messageDataMap);
}
