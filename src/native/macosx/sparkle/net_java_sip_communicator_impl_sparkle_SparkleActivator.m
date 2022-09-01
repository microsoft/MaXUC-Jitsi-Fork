/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

/**
 * Init the Sparkle subsystem.
 *
 * To generate the .h file, compile SC, go to the 
 * classes/ directory, and execute:
 * javah -jni net.java.sip.communicator.impl.sparkle.SparkleActivator
 *
 * For compilation, this requires the Sparkle.framework 
 * installed in /Library/Frameworks/. This Framework is 
 * available at http://sparkle.andymatuschak.org/
 *
 * @author Romain Kuntz
 * @author Egidijus Jankauskas
 */

#include <Cocoa/Cocoa.h>
#include "net_java_sip_communicator_impl_sparkle_SparkleActivator.h"

#import <os/log.h>

/**
 * Implementation of Sparkle's SUUpdaterDelegate protocol
 * implementing a single method which provides a feedURL to Sparkle dynamically
 */
@implementation SUUpdaterDelegateImpl

@synthesize updateURL;

- (NSString *)feedURLStringForUpdater:(SUUpdater *)updater {
    os_log(OS_LOG_DEFAULT, "[Sparkle JNI] Dynamically providing feedURL string to Sparkle");
    return [self updateURL];
}

@end

// Ensures reference to delegate object is maintained after creating JNI call ends
static SUUpdaterDelegateImpl * updateUrlDelegate;

/*
 * Class:     net_java_sip_communicator_impl_sparkle_SparkleActivator
 * Method:    initSparkle
 * Signature: (Ljava/lang/String;ZILjava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_sparkle_SparkleActivator_initSparkle
  (JNIEnv *env, jclass obj, jstring pathToSparkleFramework, 
   jboolean updateAtStartup, jint checkInterval, jstring downloadLink,
   jstring menuItemTitle, jstring userAgentString)
{
    BOOL hasLaunchedBefore = [[NSUserDefaults standardUserDefaults] boolForKey:@"SCHasLaunchedBefore"];

    if(!hasLaunchedBefore)
    {
        [[NSUserDefaults standardUserDefaults] setBool:YES forKey:@"SCHasLaunchedBefore"];
        [[NSUserDefaults standardUserDefaults] synchronize];
    }

    SUUpdater *suUpdater = [SUUpdater updaterForBundle:[NSBundle mainBundle]];
    
    if(userAgentString)
    {
        const char* userAgent = (*env)->GetStringUTFChars(env, userAgentString, 0);
        NSString* sUserAgent = [NSString stringWithCString: userAgent length: strlen(userAgent)];
        
        if(sUserAgent)
        {
            [suUpdater setUserAgentString: sUserAgent];
        }
    }
    
    if ((int)checkInterval > 0)
    {
    	[suUpdater setUpdateCheckInterval: (int) checkInterval];
    	[suUpdater setAutomaticallyChecksForUpdates: YES];
    }
    else 
    {
        // Calling initSparkle with a checkInterval of 0 means we want to disable scheduled updates
    	[suUpdater setAutomaticallyChecksForUpdates: NO];
    }

    if (downloadLink)
    {
        const char* link = (*env)->GetStringUTFChars(env, downloadLink, 0);
        NSString* sLink = [NSString stringWithCString: link length: strlen(link)];

        // We used to provide the download link to Sparkle using setFeedURL. However, when this method
        // was used, Sparkle would log the URL in a user preferences file. This was not acceptable if the URL
        // contained sensitive information, so now we use a delegate object to provide Sparkle with the download link
        // at the last possible moment (see SFR536630).
        updateUrlDelegate = [[SUUpdaterDelegateImpl alloc] init];
        [updateUrlDelegate setUpdateURL: sLink];
        [suUpdater setDelegate: updateUrlDelegate];
        os_log(OS_LOG_DEFAULT, "[Sparkle JNI] Created SUUpdaterDelegate object");
    }
    else
    {
        os_log(OS_LOG_DEFAULT, "[Sparkle JNI] Download link is null, so not creating delegate");
    }

    NSString* menuTitle;
    if(!menuItemTitle)
    {
        menuTitle = @"Check for Updates...";
    }
    else
    {
        const char* menuTitleChars =
            (const char *)(*env)->GetStringUTFChars(env, menuItemTitle, 0);
        menuTitle = [NSString stringWithUTF8String: menuTitleChars];
    }

    NSMenu* menu = [[NSApplication sharedApplication] mainMenu];
    NSMenu* applicationMenu = [[menu itemAtIndex:0] submenu];
    NSMenuItem* checkForUpdatesMenuItem = [[NSMenuItem alloc]
                                            initWithTitle:menuTitle
                                            action:@selector(checkForUpdates:)
                                            keyEquivalent:@""];

    [checkForUpdatesMenuItem setEnabled:YES];
    [checkForUpdatesMenuItem setTarget:suUpdater];

    // 0 => top, 1 => after "About..."
    [applicationMenu insertItem:checkForUpdatesMenuItem atIndex:1];

    // Update is launched only at the second startup
    if (hasLaunchedBefore && updateAtStartup == JNI_TRUE)
    {
        // This method needs to be executed on the main thread because it may result
        // in GUI showing up. Besides, Sparkle uses asynchronous URLConnection which
        // requires to be called from a thread which runs in a default run loop mode.
        [suUpdater performSelectorOnMainThread:@selector(checkForUpdatesInBackground)
                                withObject:nil
                                waitUntilDone:NO];
    }
}
 
/*
 * Class:     net_java_sip_communicator_impl_sparkle_SparkleActivator
 * Method:    setDownloadLink
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_sparkle_SparkleActivator_setDownloadLink
  (JNIEnv *env, jclass obj, jstring downloadLink)
{
    if(downloadLink)
    {
        SUUpdater *suUpdater = [SUUpdater updaterForBundle:[NSBundle mainBundle]];
        const char* link = (*env)->GetStringUTFChars(env, downloadLink, 0);
        NSString* sLink = [NSString stringWithCString: link length: strlen(link)];

        if (updateUrlDelegate)
        {
            [updateUrlDelegate setUpdateURL:sLink];
        }
        else
        {
            os_log(OS_LOG_DEFAULT, "[Sparkle JNI] SUUpdaterDelegate object does not exist, so creating one");
            updateUrlDelegate = [[SUUpdaterDelegateImpl alloc] init];
            [updateUrlDelegate setUpdateURL:sLink];
            [suUpdater setDelegate: updateUrlDelegate];
        }
    }
}

/*
 * Class:     net_java_sip_communicator_impl_sparkle_SparkleActivator
 * Method:    setNewUpdateCheckInterval
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_sparkle_SparkleActivator_setNewUpdateCheckInterval
  (JNIEnv *env, jclass obj, jint checkInterval)
{
  dispatch_async(dispatch_get_main_queue(), ^{
    SUUpdater *suUpdater = [SUUpdater updaterForBundle:[NSBundle mainBundle]];
    
    // Sparkle should schedule the next update check
    [suUpdater setUpdateCheckInterval: (int) checkInterval];
    [suUpdater setAutomaticallyChecksForUpdates: YES];
  });
}

/*
 * Class:     net_java_sip_communicator_impl_sparkle_SparkleActivator
 * Method:    cancelScheduledUpdateChecks
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_sparkle_SparkleActivator_cancelScheduledUpdateChecks
  (JNIEnv *env, jclass obj)
{
  dispatch_async(dispatch_get_main_queue(), ^{
    SUUpdater *suUpdater = [SUUpdater updaterForBundle:[NSBundle mainBundle]];
    
    // Sparkle should cancel the next update check
    [suUpdater setAutomaticallyChecksForUpdates: NO];
  });
}

/*
 * Class:     net_java_sip_communicator_impl_sparkle_SparkleActivator
 * Method:    checkForUpdates
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_sparkle_SparkleActivator_checkForUpdates
  (JNIEnv *env, jclass obj)
{
  os_log(OS_LOG_DEFAULT, "[Sparkle JNI] Client-triggered update check");
  dispatch_async(dispatch_get_main_queue(), ^{
    SUUpdater *suUpdater = [SUUpdater updaterForBundle:[NSBundle mainBundle]];

    // Parameter of checkForUpdates is 'sender', the UI object that initiated the call
    // When calling an action handler like this from code, convention is to use nil
    [suUpdater checkForUpdates:nil];
  });
}
  

