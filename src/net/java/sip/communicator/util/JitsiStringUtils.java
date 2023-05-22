/**
 *
 * Copyright Jive Software.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

/**
 * A collection of utility methods for String objects. Legacy code derived from
 * pre-4.4.x Smack
 */
public class JitsiStringUtils
{
    /**
     * Returns the XMPP address with any resource information removed. For example,
     * for the address "matt@jivesoftware.com/Smack", "matt@jivesoftware.com" would
     * be returned.
     *
     * @param xmppAddress the XMPP address.
     * @return the bare XMPP address without resource information.
     */
    public static String parseBareAddress(String xmppAddress) {
        if (xmppAddress == null) {
            return null;
        }
        int slashIndex = xmppAddress.indexOf("/");
        if (slashIndex < 0) {
            return xmppAddress;
        }
        else if (slashIndex == 0) {
            return "";
        }
        else {
            return xmppAddress.substring(0, slashIndex);
        }
    }

    /**
     * Returns the server portion of a XMPP address. For example, for the
     * address "matt@jivesoftware.com/Smack", "jivesoftware.com" would be returned.
     * If no server is present in the address, the empty string will be returned.
     *
     * @param xmppAddress the XMPP address.
     * @return the server portion of the XMPP address.
     */
    public static String parseServer(String xmppAddress) {
        if (xmppAddress == null) {
            return null;
        }

        int slashIndex = xmppAddress.indexOf("/");
        String noResourceSubstring = xmppAddress;

        if (slashIndex > 0) {
            noResourceSubstring = xmppAddress.substring(0, slashIndex);
        }

        int atIndex = noResourceSubstring.lastIndexOf("@");
        return noResourceSubstring.substring(atIndex + 1);
    }

    /**
     * Returns the name portion of a XMPP address. For example, for the
     * address "matt@jivesoftware.com/Smack", "matt" would be returned. If no
     * username is present in the address, the empty string will be returned.
     *
     * @param xmppAddress the XMPP address.
     * @return the name portion of the XMPP address.
     */
    public static String parseName(String xmppAddress) {
        if (xmppAddress == null) {
            return null;
        }
        int atIndex = xmppAddress.lastIndexOf("@");
        if (atIndex <= 0) {
            return "";
        }
        else {
            return xmppAddress.substring(0, atIndex);
        }
    }
}
