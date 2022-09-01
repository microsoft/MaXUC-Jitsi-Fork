/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.resources;

import java.io.*;
import java.net.*;

/**
 *
 * @author Damian Minkov
 */
public interface ImagePack
    extends ResourcePack
{
    String RESOURCE_NAME_DEFAULT_VALUE = "DefaultImagePack";

    /**
     * Return the requested image as an <tt>InputStream</tt>.
     *
     * @return a <tt>InputStream</tt> containing the requested image.
     */
    InputStream getImageAsStream(String path);

    /**
     * Returns the reuqested images as a <tt>URL</tt>
     *
     * @return the <tt>URL</tt> of the requested image.
     */
    URL getImageURL(String path);
}
