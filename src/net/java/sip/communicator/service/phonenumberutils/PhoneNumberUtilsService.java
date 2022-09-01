// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.phonenumberutils;

public interface PhoneNumberUtilsService
{
    /**
     * Format a number to be edited and saved.
     * <li/> Just as a leading additional character and digits if it begins with
     *       an additional character such as # or *.
     * <li/> International number for our EAS - formatted in E164 format (+ on
     *       the beginning and no spaces or punctuation).
     * <li/> National number - formatted as digits only with no spaces or
     *                         punctuation.
     * If the number cannot be parsed by the formatting library (i.e. when it
     * thinks the number entered is invalid) we simply return the digits that we
     * passed in and remove all the non-digits.
     *
     * @param phoneNumberString - phone number to be formatted
     * @return phone number formatted for saving.
     */
    String formatNumberToEditAndStore(String phoneNumberString);

    /**
     * Format a phone number to send to CommPortal server:
     * <li/> Just as a leading additional character and digits if it begins with
     *       an additional character such as # or *.
     * <li/> International number is formated with the full international
     *       dialling code and country code (since CP server does not accept +).
     *       There is no punctuation or spaces.
     * <li/> National number is sent as digits only with no punctuation or
     *       spaces.
     * If the number cannot be parsed by the formatting library (i.e. when it
     * thinks the number entered is invalid) we simply return the digits that we
     * passed in and remove all the non-digits.
     *
     * @param phoneNumberString
     * @return number formatted to send to CommPortal server.
     */
    String formatNumberToSendToCommPortal(String phoneNumberString);

    /**
     * Format a phone number to E164. If the number cannot be parsed by the
     * formatting library  (i.e. when it thinks the number entered is invalid)
     * we simply return the digits that were passed in.
     *
     * @param phoneNumberString
     * @return number formatted as E164
     */
    String formatNumberToE164(String phoneNumberString);

    /**
     * Format a phone number to dialing (likely to be either E164 or adding an
     * ELC). If the number cannot be parsed by the formatting library (i.e. when
     * it thinks the number entered is invalid) we simply return the digits that
     * were passed in.
     *
     * @param phoneNumberString
     * @return number formatted as E164/with ELC/unchanged
     */
    String formatNumberForDialing(String phoneNumberString);

    /**
     * Format a phone number to national format. If the number cannot be
     * parsed by the formatting library  (i.e. when it thinks the number
     * entered is invalid) we simply return the digits that were passed in.
     *
     * @param phoneNumberString
     * @return number formatted as a national number
     */
    String formatNumberToNational(String phoneNumberString);

    /**
     * Format a phone number for use in SIP Refer headers:
     * <li/> If in E164 format, strip the leading + and international dialing
     * prefix if it is not required.
     * <li/> If not in E164 format and is an international number, add the
     * international dialing prefix without a +
     * <li/> If an External Line Code is required then prepend it
     * If the number cannot be parsed by the formatting library (i.e. when it
     * thinks the number entered is invalid) we simply return the digits that
     * we passed in.
     *
     * @param phoneNumberString
     * @return number formatted to send to CommPortal server.
     */
    String formatNumberForRefer(String phoneNumberString);

    /**
     * Format a phone number to display nicely in the UI (i.e. with spacing,
     * brackets and dashes, as appropriate).  If the string provided starts
     * with "+" or the international dialing code, it will be returned
     * formatted in international format, otherwise it will be returned in
     * national format.  If the number cannot be parsed by the formatting
     * library  (i.e. when it thinks the number entered is invalid) we simply
     * return the digits that were passed in.
     *
     * @param phoneNumberString the phone number to format.
     * @return number formatted for display in the UI.
     */
    String formatNumberForDisplay(String phoneNumberString);

    /**
     * Format the phone number as a national number if it is local, and E164
     * otherwise.  For example, in the US:
     *  +1-234-555-0101 -> 234-555-0101
     *  +44 7810 459845 -> +44 7810 459845
     * Also removes any External Line Code if the result is a valid public phone number.
     *
     * @param number The number to format
     * @return The formatted phone number
     */
    String formatNumberToLocalOrE164(String number);

    /**
     * Horrible hack to get around a server bug whereby it breaks
     * internationalised numbers in call lists by stripping off the
     * international dialling prefix. We check to see if a number on a call list
     * is valid and, if not, we try adding a plus to convert to E164 format. If
     * it is now valid, we return the number in E164 format; if it is still not
     * valid we return the original number we were originally given).
     *
     * @param numberToCheck - number to check if it's broken and fix.
     *
     * @return original number if this hack doesn't work; otherwise the new valid
     *         number.
     */
    String maybeFixBrokenInternationalisedNumber(String numberToCheck);

    /**
     * Removes any external line code from the given phone number.
     *
     * @param phoneNumberString The phone number from which to remove the ELC.
     * @return The phone number, with any initial ELC stripped.
     */
    String stripELC(String phoneNumberString);

    /**
     * Determines if a specified phone number is local or international.
     * @param phoneNumberString The number to test.
     * @return <tt>true</tt> if the number is local to the EAS deployment, and
     * <tt>false</tt> otherwise.
     * @throws InvalidPhoneNumberException Thrown if the given number is not a
     * valid phone number.
     */
    boolean isLocal(String phoneNumberString)
                                             throws InvalidPhoneNumberException;

    /**
     * Determines if the number that is passed in is a valid number or not
     * (according to CommPortal)
     *
     * @param number the number to test
     * @return true if the number is valid.
     */
    boolean isValidNumber(String number);

    /**
     * Determines if the number that is passed in is a valid SMS number or not
     *
     * @param number the number to test
     * @return true if the number is valid.
     */
    boolean isValidSmsNumber(String number);

    /**
     * @param number The unformatted number
     * @param regex The regular expression which matches valid numbers
     * @return A valid number with ignored characters stripped out or null if
     *         the supplied number is not matched by the supplied regex
     */
    String getValidNumber(String number, String regex);

    /**
     * @param address Input address to parse.
     * @return Extracts a plain DN from a general address (e.g. SIP URI) or
     * returns null if no such DN can be extracted.
     */
    String extractPlainDnFromAddress(String address);

    /**
     * @return the EAS region for this client.
     */
    String getEASRegion();
}
