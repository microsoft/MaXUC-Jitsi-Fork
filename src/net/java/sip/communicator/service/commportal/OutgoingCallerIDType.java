package net.java.sip.communicator.service.commportal;

/**
 * This enum lists all possible types of Outgoing caller ID.
 *
 * Logged-in user might have an option to control the number which will be
 * displayed to the callees during outgoing calls.
 *
 * They can "override" their directory number (DN) in the business group (BG)
 * with other preconfigured numbers (e.g. business group party number).
 *
 * For now, there are two possible types:
 *      1) Directory number - user's default number in the BG
 *      2) Calling party number - Main business group party number
 *      configured by admin of the BG.
 */
public enum OutgoingCallerIDType
{
    /**
     * User's default number in the business group.
     */
    SUBSCRIBER_DN("Subscriber DN"),

    /**
     * Main business group party number configured by admin of the BG
     */
    CONFIGURED_CALLING_PARTY_NUMBER("Configured Calling Party Number");

    private final String callerIDTypeName;

    OutgoingCallerIDType(String callerIDTypeName)
    {
        this.callerIDTypeName = callerIDTypeName;
    }

    public String getValue()
    {
        return callerIDTypeName;
    }

    /**
     * Finds an enum type by value.
     * If the type has not been found, method will return null.
     * @return OutgoingCallerIDType if it exists for a given value,
     * null otherwise.
     */
    public static OutgoingCallerIDType findByValue(String testValue)
    {
        for (OutgoingCallerIDType type: OutgoingCallerIDType.values())
        {
            if (type.getValue().equals(testValue))
            {
                return type;
            }
        }
        return null;
    }
}
