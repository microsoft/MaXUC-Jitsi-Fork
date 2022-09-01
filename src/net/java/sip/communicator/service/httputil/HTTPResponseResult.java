// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.httputil;

import java.io.*;

import org.apache.http.*;
import org.apache.http.impl.client.*;
import org.apache.http.util.*;

/**
 * Utility class wraps the http requests result and some utility methods
 * for retrieving info and content for the result.
 */
public class HTTPResponseResult
{
    /**
     * The HttpResponse.
     */
    private final HttpResponse response;

    /**
     * The HttpClient entity.
     */
    private final HttpEntity entity;

    /**
     * The HttpClient.
     */
    private final DefaultHttpClient httpClient;

    /**
     * Creates HTTPResponseResult.
     * @param response the HttpResponse.
     * @param httpClient the HttpClient.
     */
    HTTPResponseResult(HttpResponse response, DefaultHttpClient httpClient)
    {
        this.response = response;
        this.entity = response.getEntity();
        this.httpClient = httpClient;
    }

    /**
     * Tells the length of the content, if known.
     *
     * @return  the number of bytes of the content, or
     *          a negative number if unknown. If the content length is known
     *          but exceeds {@link java.lang.Long#MAX_VALUE Long.MAX_VALUE},
     *          a negative number is returned.
     */
    public long getContentLength()
    {
        return entity.getContentLength();
    }

    /**
     * Get an HTTP header from the response.
     * @param headerName
     * @return
     */
    public String getResponseHeader(String headerName)
    {
        Header header = response.getFirstHeader(headerName);
        return (header != null ? header.getValue() : null);
    }

    /**
     * Returns a content stream of the entity.
     *
     * @return content stream of the entity.
     *
     * @throws IOException if the stream could not be created
     * @throws IllegalStateException
     *  if content stream cannot be created.
     */
    public InputStream getContent()
        throws IOException, IllegalStateException
    {
        return new HttpClientInputStream(entity.getContent(), httpClient);
    }

    /**
     * Returns a content string of the entity.
     *
     * @return content string of the entity.
     *
     * @throws IOException if the stream could not be created
     */
    public String getContentString()
        throws IOException
    {
        try
        {
            return EntityUtils.toString(entity);
        }
        finally
        {
            if(httpClient != null)
                httpClient.getConnectionManager().shutdown();
        }
    }

    /**
     * Get the credentials used by the request.
     *
     * @return the credentials (login at index 0 and password at index 1)
     */
    public String[] getCredentials()
    {
        String cred[] = new String[2];

        if(httpClient != null)
        {
            HTTPCredentialsProvider prov = (HTTPCredentialsProvider)
                    httpClient.getCredentialsProvider();
            cred[0] = prov.getAuthenticationUsername();
            cred[1] = prov.getAuthenticationPassword();
        }

        return cred;
    }
}
