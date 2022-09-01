// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.websocketserver;

import static net.java.sip.communicator.service.websocketserver.WebSocketApiConstants.WS_MESSAGE_FORMAT_ERROR;

import java.util.Map;

import net.java.sip.communicator.service.websocketserver.WebSocketApiError.WebSocketApiMessageException;

/**
 * Data class used to represent WebSocket API messages.
 *
 * Details about the WebSocket API and HLD of the code can be found on SharePoint
 * in https://[indeterminate link]
 */
public class WebSocketApiMessageMap
{
    /** Map storing the whole WebSocket API message. */
    private Map<String, Object> mMessageMap;

    /**
     * The ID of the WebSocket API message - pulled out of the full map
     * for convenience.
     */
    private String mMessageId;

    /**
     * The WebSocket API 'message' field - pulled out of the full
     * map for convenience.
     */
    private String mMessage;

    /**
     * The WebSocket API 'type' field - pulled out of the full
     * map for convenience.
     */
    private String mType;

    /**
     * The WebSocket API 'data' field - pulled out of the full
     * map for convenience.
     */
    private Map mMessageDataMap;

    /**
     * Stores the API error associated with the WebSocket API message,
     * if any (else it's null).
     */
    private WebSocketApiError mWebSocketApiError;

    /**
     * Takes in a map representing a whole WebSocket API message and parses it,
     * extracting the key message fields into separate fields. Throws an
     * exception if there is a problem extracting any of the fields
     * from the map.
     *
     * Should generally be called only once for every object of this type, but
     * split out as a separate method (as opposed to being in the constructor)
     * to make error handling easier - throwing exceptions straight out of the
     * constructor would make it slightly more difficult to handle recovering
     * (and raising) the API error field.
     *
     * @param messageMap The full WebSocket API message.
     * @throws WebSocketApiMessageException Exception raised if a problem was
     * found extracting any of the mandatory fields from the message map.
     */
    public void setMessageMap(Map<String, Object> messageMap)
            throws WebSocketApiMessageException
    {
        // Store the whole message map.
        mMessageMap = messageMap;

        // Try to extract each top-level field from the message map and throw
        // an error if there is a problem with any of them. Store each field
        // separately for convenient access.
        mMessageId = (String) getInternalFieldOfType(
                WebSocketApiConstants.WebSocketApiMessageField.ID_KEY);
        mMessage = (String) getInternalFieldOfType(
                WebSocketApiConstants.WebSocketApiMessageField.MESSAGE_KEY);
        mType = (String) getInternalFieldOfType(
                WebSocketApiConstants.WebSocketApiMessageField.TYPE_KEY);
        mMessageDataMap = (Map) getInternalFieldOfType(
                WebSocketApiConstants.WebSocketApiMessageField.DATA_KEY);
    }

    /**
     * @return The full WebSocket API message map.
     */
    public Map getFullMessageMap()
    {
        return mMessageMap;
    }

    /**
     * @return The 'message' field of the WebSocket API message.
     */
    public String getMessageField()
    {
        return mMessage;
    }

    /**
     * @return The 'type' field of the WebSocket API message.
     */
    public String getTypeField()
    {
        return mType;
    }

    /**
     * @return The 'id' field of the WebSocket API message.
     */
    public String getIdField()
    {
        return mMessageId;
    }

    /**
     * @return The WebSocket API error hit while parsing the fields of the
     * stored WebSocket API message. Could be a problem with either the
     * top-level fields, or the data fields.
     */
    public WebSocketApiError getApiError()
    {
        return mWebSocketApiError;
    }

    /**
     * Parses the data fields of the stored WebSocket API message map.
     *
     * @param fieldKey The key of the field to search for.
     * @return The value of the extracted field.
     * @throws WebSocketApiMessageException If the key could not be found, or
     * if the value is not of the expected type.
     */
    public Object getFieldOfType(
            WebSocketApiConstants.WebSocketApiMessageField fieldKey)
            throws WebSocketApiMessageException
    {
        return getFieldOfType(mMessageDataMap, fieldKey);
    }

    /**
     * Parses the top-level fields of the stored WebSocket API message map.
     *
     * Note that if this method encounters an error while processing the 'id'
     * message field, it will store a WebSocketApiError object in
     * mWebSocketApiError with a null id. This is expected and WAD - see the
     * constructor of WebSocketApiError for more information.
     *
     * @param fieldKey The key of the field to search for.
     * @return The value of the extracted field.
     * @throws WebSocketApiMessageException If the key could not be found, or
     * if the value is not of the expected type.
     */
    private Object getInternalFieldOfType(
            WebSocketApiConstants.WebSocketApiMessageField fieldKey)
            throws WebSocketApiMessageException
    {
        return getFieldOfType(mMessageMap, fieldKey);
    }

    /**
     * Parses the fields of the given WebSocket API message map and
     * returns the value of the given key, if the key is found in the message,
     * and if the value matches the given type. Else, it stores the error
     * encountered in the mWebSocketApiError object field and throws
     * an exception.
     *
     * @param messageMap The WebSocket API message map to parse.
     * @param fieldKey The key of the field to search for.
     * @return The value of the extracted field.
     * @throws WebSocketApiMessageException If the key could not be found, or
     * if the value is not of the expected type.
     */
    private Object getFieldOfType(
            Map messageMap,
            WebSocketApiConstants.WebSocketApiMessageField fieldKey)
            throws WebSocketApiMessageException
    {
        Object fieldValue;

        // Search for the required field by its key and try to cast the returned
        // value to the expected type. Note that if no match is found, the get()
        // method will return null, which will then successfully be cast to any
        // type and won't throw a ClassCastException.
        try
        {
            fieldValue = fieldKey.getType().cast(messageMap.get(fieldKey.toString()));
        }
        catch (ClassCastException e)
        {
            // If the field value cannot be converted to the given type, there's
            // something wrong with it, so record the error and throw
            // an exception.
            mWebSocketApiError = new WebSocketApiError(
                    mMessageId,
                    WS_MESSAGE_FORMAT_ERROR,
                    fieldKey.toString());
            throw new WebSocketApiMessageException(
                    "Problem parsing field \'" + fieldKey + "\': " + e);
        }

        // If the field key was not found in the data map, record the error
        // and raise an exception.
        if (fieldValue == null)
        {
            // The failureReason parameter is optional, so don't raise an
            // exception and API error if it is not found - just return an empty
            // string (although if it is provided in a bad format, an exception
            // should still be thrown, which is why there is no check like this
            // above).
            if (fieldKey ==
                    WebSocketApiConstants.WebSocketApiMessageField.FAILURE_REASON)
            {
                fieldValue = "";
            }
            else
            {
                mWebSocketApiError = new WebSocketApiError(
                        mMessageId,
                        WS_MESSAGE_FORMAT_ERROR,
                        fieldKey.toString());
                throw new WebSocketApiMessageException(
                        String.format("Field \'%s\' not found in message",
                                      fieldKey));
            }
        }

        // If there were no problems getting the field value, return it.
        return fieldValue;
    }
}
