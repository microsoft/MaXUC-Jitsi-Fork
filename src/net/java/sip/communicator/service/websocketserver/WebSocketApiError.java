// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.websocketserver;

/**
 * Data class used to represent WebSocket API errors.
 *
 * Details about the WebSocket API and HLD of the code can be found on SharePoint
 * in https://[indeterminate link]
 */
public class WebSocketApiError
{
    /** The special id value to use for errors that cannot be associated
     * with any other API messages. */
    private static final String UNSPECIFIED_ERROR_ID = "-1";

    /** The ID to use in the error message. */
    public final String mMessageId;

    /** The type of API error. */
    public final String mErrorType;

    /** The errorReason to include in the error message. */
    public final String mErrorReason;

    /**
     * Constructor.
     *
     * @param messageId The WebSocket API ID of the error message -
     *                  should either match the ID of the message that the
     *                  error is associated with, or be null if said message
     *                  did not have a proper ID.
     * @param errorType The type of WebSocket API error.
     * @param additionalErrorReason Any additional information to include in the
     *                              error message. See
     *                              WEBSOCKET_API_ERROR_REASON for more info on
     *                              the available errorReason template strings.
     */
    public WebSocketApiError(String messageId,
                             String errorType,
                             String... additionalErrorReason)
    {
        mMessageId = messageId != null ? messageId : UNSPECIFIED_ERROR_ID;
        mErrorType = errorType;

        // Add additional information to the errorReason, if provided.
        if (additionalErrorReason != null && additionalErrorReason.length > 0)
        {
            mErrorReason = String.format(
                    WebSocketApiConstants.WEBSOCKET_API_ERROR_REASON.get(
                            errorType),
                    (Object[]) additionalErrorReason);
        }
        else
        {
            mErrorReason = WebSocketApiConstants.WEBSOCKET_API_ERROR_REASON.get(
                    errorType);
        }
    }

    /**
     * Exception indicating a problem with a WebSocket API message.
     */
    public static class WebSocketApiMessageException extends Exception
    {
        public WebSocketApiMessageException(String errorMessage)
        {
            super(errorMessage);
        }
    }
}
