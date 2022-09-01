// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.httputil;

import java.io.*;

import org.apache.http.client.*;

/**
 * Input stream wrapper which handles closing the httpclient when
 * everything is retrieved.
 */
public class HttpClientInputStream
    extends InputStream
{
    /**
     * The original input stream.
     */
    private final InputStream in;

    /**
     * The http client to close.
     */
    private final HttpClient httpClient;

    /**
     * Creates HttpClientInputStream.
     * @param in the original input stream.
     * @param httpClient the http client to close.
     */
    HttpClientInputStream(InputStream in, HttpClient httpClient)
    {
        this.in = in;
        this.httpClient = httpClient;
    }

    /**
     * Uses parent InputStream read method.
     *
     * @return the next byte of data, or <code>-1</code> if the end of the
     *         stream is reached.
     * @throws java.io.IOException if an I/O error occurs.
     */
    @Override
    public int read()
        throws IOException
    {
        return in.read();
    }

    /**
     * Closes this input stream and releases any system resources associated
     * with the stream. Releases httpclient connections.
     *
     * <p> The <code>close</code> method of <code>InputStream</code> does
     * nothing.
     *
     * @exception  IOException  if an I/O error occurs.
     */
    public void close()
        throws IOException
    {
        super.close();

        // When HttpClient instance is no longer needed,
        // shut down the connection manager to ensure
        // immediate de-allocation of all system resources
        httpClient.getConnectionManager().shutdown();
    }
}
