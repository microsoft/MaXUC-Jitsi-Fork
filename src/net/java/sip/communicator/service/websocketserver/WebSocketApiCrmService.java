// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.websocketserver;

/**
 * Service used for providing CRM integration (with one or multiple CRMs)
 * via a third party connected on the WebSocket API.
 *
 * Details about the WebSocket API and HLD of the code can be found on SharePoint
 * in https://[indeterminate link]
 */
public interface WebSocketApiCrmService
{
  /**
   * @return True if there are active WebSocket connections that can process CRM
   * integration requests, false otherwise.
   */
  boolean readyForCrmIntegration();

  /**
   * Verifies whether a given address can be parsed to extract a plain DN, which
   * is the required format for CRM lookups. Can be used to check whether any
   * prep needs to be done for a CRM lookup before initiating the lookup.
   *
   * @param crmLookupAddress The 'address' of the remote party that the CRM
   *                         request is intended for.
   * @return Whether the address can be parsed to extract a DN in the format
   * required for CRM lookups.
   */
  boolean canParseCrmAddress(String crmLookupAddress);

  /**
   * Opens the contact page for the given DN in the integrated CRM(s).
   *
   * @param crmLookupAddress The 'address' of the remote party that the CRM
   *                         request is intended for. It is expected that it
   *                         will contain a parsable DN, but this method will
   *                         handle exceptions to that rule.
   * @param listener The listener that should be notified of responses to this
   *                 request.
   */
  void openCrmContactPage(String crmLookupAddress, WebSocketApiRequestListener listener);

  /**
   * Looks-up the contact information for the given DN in the integrated CRM(s).
   *
   * @param crmLookupAddress The 'address' of the remote party that the CRM
   *                         request is intended for. It is expected that it
   *                         will contain a parsable DN, but this method will
   *                         handle exceptions to that rule.
   * @param listener The listener that should be notified of responses to this
   *                 request.
   */
  void crmDnLookup(String crmLookupAddress, WebSocketApiRequestListener listener);
}
