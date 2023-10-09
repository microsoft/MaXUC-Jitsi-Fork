// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.utils;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.PointerType;
import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.ptr.IntByReference;

/**
 * JNA mapping to IOKit internal macOS library.
 * Maps IOKit methods to control power management to prevent system going to sleep.
 * An assertion must be created to tell Power Management service to keep awake,
 * and it must be released to go back to normal mode.
 *
 * Currently active assertions can be seen using CLI command: pmset -g assertions
 * Assertion creations and releases log: pmset -g assertionslog
 */
public interface IOKitLibrary extends Library
{
    IOKitLibrary INSTANCE = Native.load("IOKit", IOKitLibrary.class);

    int kIOReturnSuccess = 0;
    int kIOPMAssertionLevelOn = 255;

    /**
     * When asserted and set to level kIOPMAssertionLevelOn, will prevent the display
     * from turning off due to a period of idle user activity.
     * Note that the display may still sleep from other reasons,
     * like a user closing a portable's lid or the machine sleeping.
     * If the display is already off, this assertion does not light up the display.
     * While the display is prevented from dimming, the system cannot go into idle sleep.
     * This assertion does not put the system into Dark Wake.
     * {@see https://developer.apple.com/documentation/iokit/kiopmassertiontypepreventuseridledisplaysleep/}
     */
    String kIOPMAssertionTypePreventUserIdleDisplaySleep = "PreventUserIdleDisplaySleep";

    /**
     * Prevents display going off and system going to sleep in idle state.
     * Creates an IOKit assertion of the corresponding type.
     * {@see https://developer.apple.com/library/archive/qa/qa1340/_index.html}
     * @param assertionNameString activity name
     * @return assertion ID if successfully created, otherwise -1
     */
    static int disableSleepWhenIdle(final String assertionNameString)
    {
        final char[] assertionTypeValue = kIOPMAssertionTypePreventUserIdleDisplaySleep.toCharArray();
        CoreFoundation.CFStringRef assertionType = CoreFoundation.INSTANCE.CFStringCreateWithCharacters(null, assertionTypeValue, new CoreFoundation.CFIndex(assertionTypeValue.length));

        final char[] assertionNameValue = assertionNameString.toCharArray();
        CoreFoundation.CFStringRef assertionName = CoreFoundation.INSTANCE.CFStringCreateWithCharacters(null, assertionNameValue, new CoreFoundation.CFIndex(assertionNameValue.length));

        final IntByReference assertionIdRef = new IntByReference();
        final int result = INSTANCE.IOPMAssertionCreateWithName(assertionType, kIOPMAssertionLevelOn, assertionName, assertionIdRef);
        if (result == kIOReturnSuccess)
        {
            return assertionIdRef.getValue();
        }

        return -1;
    }

    /**
     * Cancels preventing system going to sleep.
     * Releases the IOKit assertion with the specified ID.
     * @param assertionId
     */
    static void enableSleepWhenIdle(final int assertionId)
    {
        INSTANCE.IOPMAssertionRelease(assertionId);
    }

    /**
     * Dynamically requests a system behavior from the power management system.
     * {@see https://developer.apple.com/documentation/iokit/1557134-iopmassertioncreatewithname}
     *
     * @param assertionType assertion type to request from the PM system
     * @param assertionLevel kIOPMAssertionLevelOn or kIOPMAssertionLevelOff
     * @param assertionName A string that describes the name of the caller and the activity being handled by this assertion
     *                      (e.g. "Mail Compacting Mailboxes"). Name may be no longer than 128 characters.
     * @param assertionID a unique id will be returned in this parameter.
     * @return kIOReturnSuccess on success, any other return indicates PM could not successfully activate the specified assertion.
     */
    int IOPMAssertionCreateWithName(CoreFoundation.CFStringRef assertionType, int assertionLevel,
                                    CoreFoundation.CFStringRef assertionName, PointerType assertionID);

    /**
     * Decrements the assertion's retain count.
     * {@see https://developer.apple.com/documentation/iokit/1557090-iopmassertionrelease}
     * @param assertionID The assertion_id, returned from IOPMAssertionCreateWithName, to cancel.
     * @return kIOReturnSuccess on success.
     */
    int IOPMAssertionRelease(int assertionID);
}
