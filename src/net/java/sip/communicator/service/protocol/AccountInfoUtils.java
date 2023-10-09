/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.ServerStoredDetails.AddressDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.DisplayNameDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.FirstNameDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.ImageDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.LastNameDetail;

import org.jitsi.service.resources.*;

/**
 * Utility class that would give to interested parties an easy access to some of
 * most popular account details, like : first name, last name, birth date, image,
 * etc.
 *
 * @author Yana Stamcheva
 */
public class AccountInfoUtils
{
    /**
     * Returns the first name of the account, to which the given
     * accountInfoOpSet belongs.
     *
     * @param accountInfoOpSet The account info operation set corresponding to
     * the searched account.
     * @return the first name of the account, to which the given
     * accountInfoOpSet belongs.
     */
    public static String getFirstName(
            OperationSetServerStoredAccountInfo accountInfoOpSet)
    {
        Iterator<GenericDetail> firstNameDetails
            =  accountInfoOpSet.getDetails(FirstNameDetail.class);

        if (firstNameDetails.hasNext())
        {
            FirstNameDetail firstName
                = (FirstNameDetail) firstNameDetails.next();

            if (firstName != null)
                return firstName.toString();
        }
        return null;
    }

    /**
     * Returns the last name of the account, to which the given
     * accountInfoOpSet belongs.
     *
     * @param accountInfoOpSet The account info operation set corresponding to
     * the searched account.
     * @return the last name of the account, to which the given
     * accountInfoOpSet belongs.
     */
    public static String getLastName(
            OperationSetServerStoredAccountInfo accountInfoOpSet)
    {
        LastNameDetail lastName = null;
        Iterator<GenericDetail> lastNameDetails
            =  accountInfoOpSet.getDetails(LastNameDetail.class);

        if (lastNameDetails.hasNext())
            lastName = (LastNameDetail) lastNameDetails.next();

        if(lastName == null)
            return null;

        return lastName.getString();
    }

    /**
     * Returns the display name of the account, to which the given
     * accountInfoOpSet belongs.
     *
     * @param accountInfoOpSet The account info operation set corresponding to
     * the searched account.
     * @return the display name of the account, to which the given
     * accountInfoOpSet belongs.
     */
    public static String getDisplayName(
            OperationSetServerStoredAccountInfo accountInfoOpSet)
    {
        DisplayNameDetail displayName = null;
        Iterator<GenericDetail> displayNameDetails
            =  accountInfoOpSet.getDetails(DisplayNameDetail.class);

        if (displayNameDetails.hasNext())
            displayName = (DisplayNameDetail) displayNameDetails.next();

        if(displayName == null)
            return null;

        return displayName.getString();
    }

    /**
     * Returns the image of the account, to which the given accountInfoOpSet
     * belongs.
     *
     * @param accountInfoOpSet The account info operation set corresponding to
     * the searched account.
     * @return the image of the account, to which the given accountInfoOpSet
     * belongs.
     */
    public static BufferedImageFuture getImage(
            OperationSetServerStoredAccountInfo accountInfoOpSet)
    {
        ImageDetail image = null;
        Iterator<GenericDetail> imageDetails
            =  accountInfoOpSet.getDetails(ImageDetail.class);

        if (imageDetails.hasNext())
            image = (ImageDetail) imageDetails.next();

        return (image != null)
            ? image.getImage()
            : null;
    }

    /**
     * Returns the address of the account, to which the given
     * accountInfoOpSet belongs.
     *
     * @param accountInfoOpSet The account info operation set corresponding to
     * the searched account.
     * @return the address of the account, to which the given
     * accountInfoOpSet belongs.
     */
    public static String getAddress(
            OperationSetServerStoredAccountInfo accountInfoOpSet)
    {
        AddressDetail address = null;
        Iterator<GenericDetail> addressDetails
            =  accountInfoOpSet.getDetails(AddressDetail.class);

        if (addressDetails.hasNext())
            address = (AddressDetail) addressDetails.next();

        if(address == null)
            return null;

        return address.getAddress();
    }
}
