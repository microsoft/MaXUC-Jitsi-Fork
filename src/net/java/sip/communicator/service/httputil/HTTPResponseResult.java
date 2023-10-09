// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.httputil;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.Credentials;
import org.apache.http.client.HttpClient;

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
    private final HttpClient httpClient;

    /**
     * The Credentials.
     */
    private final Credentials credentials;

    /**
     * Creates HTTPResponseResult.
     * @param response the HttpResponse.
     * @param httpClient the HttpClient.
     */
    HTTPResponseResult(HttpResponse response, HttpClient httpClient, Credentials cred)
    {
        this.response = response;
        this.entity = response.getEntity();
        this.httpClient = httpClient;
        this.credentials = cred;
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
     * @param headerName name of the header
     * @return the response header
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
     * Get the credentials used by the request.
     *
     * @return the credentials
     */
    public Credentials getCredentials()
    {
        return credentials;
    }

    public int getStatusCode()
    {
        return response.getStatusLine().getStatusCode();
    }
}
