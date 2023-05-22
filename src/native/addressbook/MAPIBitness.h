/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.

#ifndef _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRESSBOOK_MAPIBITNESS_H_
#define _NET_JAVA_SIP_COMMUNICATOR_PLUGIN_ADDRESSBOOK_MAPIBITNESS_H_

#include "JavaLogger.h"

/**
 * Checks the bitness of the Outlook installation and of the Jitsi executable.
 *
 * @author Vincent Lucas
 */

int MAPIBitness_getOutlookBitnessVersion(JavaLogger* logger);

int MAPIBitness_getOutlookVersion(JavaLogger* logger);
int findBitnessRegEntry(JavaLogger* logger);

#endif
