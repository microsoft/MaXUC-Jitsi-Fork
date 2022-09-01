/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.resources;

/**
 * Represents the Image Identifier.
 */
public class ImageID
{
    private final String id;

    public ImageID(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }

    public int hashCode()
    {
        return id == null ? 0 : id.hashCode();
    }

    public boolean equals(Object object)
    {
        if (object == this)
        {
            return true;
        }
        else if (object instanceof ImageID)
        {
            ImageID imageId = (ImageID) object;

            if (this.id == null)
            {
                return imageId.id == null;
            }
            else
            {
                return this.id.equals(imageId.id);
            }
        }
        else
        {
            return false;
        }
    }

    public String toString()
    {
        return "<ImageID: " + hashCode() + " id: " + id + ">";
    }
}
