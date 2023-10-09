/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * The operation set is a very simplified version of the server stored info
 * operation sets, allowing protocol providers to implement a quick way of
 * showing user information, by simply returning a URL where the information
 * of a specific user is to be found.
 */
public interface OperationSetWebContactInfo
    extends OperationSet
{
}
