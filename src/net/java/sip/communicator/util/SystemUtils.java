// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

/**
 * A simple helper class which we use to proxy calls to System
 * in order to make it mockable in tests.
 */
public class SystemUtils
{
    /**
     * Just proxies the call to System.setProperty.
     */
    public static String setProperty(String key, String value) {
        return System.setProperty(key, value);
    }

    /**
     * Just proxies the call to System.getProperty.
     */
    public static String getProperty(String key) {
        return System.getProperty(key);
    }
}
