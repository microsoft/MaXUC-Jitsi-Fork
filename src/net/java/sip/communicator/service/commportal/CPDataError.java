// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.commportal;

import net.java.sip.communicator.util.Logger;

/**
 * enum to cover problems communicating with CommPortal due to the data that we
 * tried to get or send
 */
public enum CPDataError
{
    authenticationFailed,
    badRequest,
    expiredToken,
    retryLimitExceeded,
    accountBlocked,
    unknownDataType,
    applicationVersionTooOld,
    serviceUnavailable,
    intermittentFailure,
    skipped,
    invalidParameters,
    noSuchObject,
    fileNotFound,
    inconsistentValueOldPassword,
    inconsistentValue,
    internalError,
    invalidValue,
    invalidValueZeros,
    invalidValueOld,
    invalidValueNonAlpha,
    invalidValueNonNumeric,
    invalidValuePasswordContainsTN,
    invalidValuePINNumericSequence,
    invalidValuePINRepeatedDigit,
    invalidValuePassword;

    private static final Logger sLog = Logger.getLogger(CPDataError.class);

    /**
     * Check whether the given string refers to an old password.
     *
     * @param str
     * @return boolean
     */
    private static boolean isAnyOldPassword(String str)
    {
        return (str.equals("OldPassword")  ||
            str.equals("OldPINAsPassword") ||
            str.equals("OldPIN"));
    }

    /**
     * Check whether the given string refers to a new password.
     * @param str
     * @return boolean
     */
    private static boolean isAnyNewPassword(String str)
    {
        return (str.equals("Password")  ||
            str.equals("PINAsPassword") ||
            str.equals("PIN"));
    }

    public static CPDataError generateCPDataError(String typeString,
        String subtypeString, String fieldString)
    {
        CPDataError returnVal = null;

        if (fieldString != null && typeString != null)
        {
            if (typeString.equals("inconsistentValue") &&
                isAnyOldPassword(fieldString))
            {
                returnVal = inconsistentValueOldPassword;
            }
            else if (typeString.equals("invalidValueZeros") &&
                isAnyNewPassword(fieldString))
            {
                returnVal = invalidValueZeros;
            }
            else if (typeString.equals("invalidValue"))
            {
                if (subtypeString == null)
                {
                    if (isAnyOldPassword(fieldString))
                    {
                        returnVal = invalidValueOld;
                    }
                    else if (fieldString.equals("Password"))
                    {
                        returnVal = invalidValueNonAlpha;
                    }
                    else if (fieldString.equals("PIN") ||
                        fieldString.equals("PINAsPassword"))
                    {
                        returnVal = invalidValueNonNumeric;
                    }
                }
                else if ((subtypeString.equals("PasswordMinDigits") ||
                    subtypeString.equals("PasswordMinLetters") ||
                    subtypeString.equals("PasswordMinSpecChars")) &&
                    isAnyNewPassword(fieldString))
                {
                    returnVal = invalidValuePassword;
                }
                else if ((subtypeString.equals("PasswordContainsTN") ||
                    subtypeString.equals("PINContainsTN")) &&
                    isAnyNewPassword(fieldString))
                {
                    returnVal = invalidValuePasswordContainsTN;
                }
                else if ((subtypeString.equals("PasswordNumericSequence") ||
                    subtypeString.equals("PINNumericSequence")) &&
                    isAnyNewPassword(fieldString))
                {
                    returnVal = invalidValuePINNumericSequence;
                }
                else if ((subtypeString.equals("PasswordRepeatedCharacter") ||
                    subtypeString.equals("PINRepeatedDigit")) &&
                    isAnyNewPassword(fieldString))
                {
                    returnVal = invalidValuePINRepeatedDigit;
                }
            }
        }

        if (returnVal == null)
        {
            // Default to returning the typeString if we've not found anything
            // more precise.
            try
            {
                returnVal = CPDataError.valueOf(typeString);
            }
            catch (IllegalArgumentException e)
            {
                sLog.debug("Unknown error type: " + typeString, e);
            }
        }

        return returnVal;
    }
}
