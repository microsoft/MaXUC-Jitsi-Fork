// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.phonenumberutils;

public class InvalidPhoneNumberException extends Exception
{
    private static final long serialVersionUID = 0L;

    public InvalidPhoneNumberException()
    {
        super();
    }

    public InvalidPhoneNumberException(String message)
    {
        super(message);
    }

    public InvalidPhoneNumberException(Throwable t)
    {
        super(t);
    }
}
