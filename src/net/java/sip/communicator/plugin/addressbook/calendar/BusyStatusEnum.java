// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.addressbook.calendar;

/**
 * Defines the possible free or busy statuses in an Outlook
 */
enum BusyStatusEnum
{
    /**
     * The Free status.
     */
    FREE(0x0000),

    /**
     * The tentative status.
     */
    TENTATIVE(0x0001),

    /**
     * The busy status.
     */
    BUSY(0x0002),

    /**
     * The out of office status.
     */
    OUT_OF_OFFICE(0x0003);

    /**
     * The value of the status.
     */
    private final long value;

    /**
     * Constructs new status.
     * @param value the value of the status
     */
    BusyStatusEnum(int value)
    {
        this.value = value;
    }

    /**
     * Returns the value of the status.
     * @return the value of the status.
     */
    public long getValue()
    {
        return value;
    }

    /**
     * Finds <tt>BusyStatusEnum</tt> instance by given value of the status.
     * @param value the value of the status we are searching for.
     * @return the status or <tt>FREE</tt> if no status is found.
     */
    public static BusyStatusEnum getFromLong(long value)
    {
        for (BusyStatusEnum state : values())
        {
            if (state.getValue() == value)
            {
                return state;
            }
        }
        return FREE;
    }
}