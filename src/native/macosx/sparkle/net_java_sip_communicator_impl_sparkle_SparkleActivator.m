/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.

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
 * Implementation of Sparkle's SPUUpdaterDelegate protocol
 * implementing a single method which provides a feedURL to Sparkle dynamically
 */
@implementation SPUUpdaterDelegateImpl

@synthesize updateURL;
@synthesize isBelowMinVersion;
@synthesize userDriver;

- (NSString * _Nullable)feedURLStringForUpdater:(SPUUpdater *)updater {
    os_log(OS_LOG_DEFAULT, "[Sparkle JNI] Dynamically providing feedURL string to Sparkle");
    return [self updateURL];
}

- (void)updater:(SPUUpdater *)updater failedToDownloadUpdate:(SUAppcastItem *)item error:(NSError *)error{
    if (item.criticalUpdate)
    {
      os_log(OS_LOG_DEFAULT, "[Sparkle JNI] Download failed for critical update. Will terminate app");
      dispatch_async(dispatch_get_main_queue(), ^{
        [self.userDriver showUpdaterError:error acknowledgement:^{
                  dispatch_async(dispatch_get_main_queue(), ^{
                    [[NSApplication sharedApplication] terminate:self];
                  });
              }];
      });
      os_log(OS_LOG_DEFAULT, "[Sparkle JNI] Terminating app");
    }
    else
    {
      os_log(OS_LOG_DEFAULT, "[Sparkle JNI] Failed to download update");
    }
}

- (void)userDidCancelDownload:(SPUUpdater *)updater {
  os_log(OS_LOG_DEFAULT, "[Sparkle JNI] User cancelled");
  if (self.isBelowMinVersion)
  {
    os_log(OS_LOG_DEFAULT, "[Sparkle JNI] User cancelled critical update. Terminating app");
    [[NSApplication sharedApplication] terminate:self];
  }
}

@end

// Ensures reference to delegate object is maintained after creating JNI call ends
static SPUUpdaterDelegateImpl *updateUrlDelegate;
static SPUStandardUserDriver *spuUserDriver;
static SPUUpdater *spuUpdater;

/*
 * Class:     net_java_sip_communicator_impl_sparkle_SparkleActivator
 * Method:    initSparkle
 * Signature: (Ljava/lang/String;ZILjava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_sparkle_SparkleActivator_initSparkle
  (JNIEnv *env, jclass obj, jstring pathToSparkleFramework, 
   jboolean updateAtStartup, jint checkInterval, jstring downloadLink,
   jstring menuItemTitle, jstring userAgentString, jboolean isAppBelowMinVersion)
{
    os_log(OS_LOG_DEFAULT, "[Sparkle JNI]  init Sparkle started");
    BOOL hasLaunchedBefore = [[NSUserDefaults standardUserDefaults] boolForKey:@"SCHasLaunchedBefore"];

    if(!hasLaunchedBefore)
    {
        os_log(OS_LOG_DEFAULT, "[Sparkle JNI]  SC has not launched before");
        [[NSUserDefaults standardUserDefaults] setBool:YES forKey:@"SCHasLaunchedBefore"];
        [[NSUserDefaults standardUserDefaults] synchronize];
    }

    dispatch_async(dispatch_get_main_queue(), ^{
      spuUserDriver = [[SPUStandardUserDriver alloc]
                                              initWithHostBundle:[NSBundle mainBundle]
                                              delegate:nil];
    });
    os_log(OS_LOG_DEFAULT, "[Sparkle JNI] SPUStandardUserDriver initialised");

    NSString* sLink = nil;
    if (downloadLink)
    {
        const char* link = (*env)->GetStringUTFChars(env, downloadLink, 0);
        sLink = [NSString stringWithCString: link length: strlen(link)];
    }
    else
    {
        os_log(OS_LOG_DEFAULT, "[Sparkle JNI] Download link is null");
    }

    // We used to provide the download link to Sparkle using setFeedURL. However, when this method
    // was used, Sparkle would log the URL in a user preferences file. This was not acceptable if the URL
    // contained sensitive information, so now we use a delegate object to provide Sparkle with the download link
    // at the last possible moment (see SFR536630).
    dispatch_async(dispatch_get_main_queue(), ^{
      updateUrlDelegate = [[SPUUpdaterDelegateImpl alloc] init];
      [updateUrlDelegate setUpdateURL: sLink];
      [updateUrlDelegate setUserDriver: spuUserDriver];
      [updateUrlDelegate setIsBelowMinVersion: (BOOL) (isAppBelowMinVersion == JNI_TRUE)];
    });
    os_log(OS_LOG_DEFAULT, "[Sparkle JNI] updateUrlDelegate initialised");

    dispatch_async(dispatch_get_main_queue(), ^{
      spuUpdater = [[SPUUpdater alloc]
                                initWithHostBundle:[NSBundle mainBundle]
                                applicationBundle:[NSBundle mainBundle]
                                userDriver:spuUserDriver
                                delegate:updateUrlDelegate];
    });
    os_log(OS_LOG_DEFAULT, "[Sparkle JNI] SPUUpdater initialised");

    if(userAgentString)
    {
        const char* userAgent = (*env)->GetStringUTFChars(env, userAgentString, 0);
        NSString* sUserAgent = [NSString stringWithCString: userAgent length: strlen(userAgent)];

        if(sUserAgent)
        {
            dispatch_async(dispatch_get_main_queue(), ^{
              [spuUpdater setUserAgentString: sUserAgent];
            });
        }
    }
    
    if ((int)checkInterval > 0)
    {
      dispatch_async(dispatch_get_main_queue(), ^{
        [spuUpdater setUpdateCheckInterval: (int) checkInterval];
        [spuUpdater setAutomaticallyChecksForUpdates: YES];
      });
    	os_log(OS_LOG_DEFAULT, "[Sparkle JNI]  setAutomaticallyChecksForUpdates: YES");
    }
    else 
    {
        // Calling initSparkle with a checkInterval of 0 means we want to disable scheduled updates
    	dispatch_async(dispatch_get_main_queue(), ^{
        [spuUpdater setAutomaticallyChecksForUpdates: NO];
      });
    	os_log(OS_LOG_DEFAULT, "[Sparkle JNI]  setAutomaticallyChecksForUpdates: NO");
    }

    dispatch_async(dispatch_get_main_queue(), ^{
        [spuUpdater startUpdater:nil];
    });
    os_log(OS_LOG_DEFAULT, "[Sparkle JNI] Started SPUUpdater");

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
    [checkForUpdatesMenuItem setTarget:spuUpdater];

    // 0 => top, 1 => after "About..."
    [applicationMenu insertItem:checkForUpdatesMenuItem atIndex:1];

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
        const char* link = (*env)->GetStringUTFChars(env, downloadLink, 0);
        NSString* sLink = [NSString stringWithCString: link length: strlen(link)];

        if (updateUrlDelegate)
        {
            dispatch_async(dispatch_get_main_queue(), ^{
              [updateUrlDelegate setUpdateURL:sLink];
            });
        }
        else
        {
            os_log(OS_LOG_DEFAULT, "[Sparkle JNI] SPUUpdaterDelegate object does not exist, so creating one");
            dispatch_async(dispatch_get_main_queue(), ^{
              updateUrlDelegate = [[SPUUpdaterDelegateImpl alloc] init];
              [updateUrlDelegate setUpdateURL:sLink];
              [spuUpdater setDelegate: updateUrlDelegate];
            });
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

    // Sparkle should schedule the next update check
    [spuUpdater setUpdateCheckInterval: (int) checkInterval];
    [spuUpdater setAutomaticallyChecksForUpdates: YES];
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

    // Sparkle should cancel the next update check
    [spuUpdater setAutomaticallyChecksForUpdates: NO];
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

    [spuUpdater checkForUpdates];
  });
}

/*
 * Class:     net_java_sip_communicator_impl_sparkle_SparkleActivator
 * Method:    setIsAppBelowMinVersion
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_sparkle_SparkleActivator_setIsAppBelowMinVersion
  (JNIEnv *env, jclass obj, jboolean isAppBelowMinVersion)
{
    os_log(OS_LOG_DEFAULT, "[Sparkle JNI] Setting minimum version");
  dispatch_async(dispatch_get_main_queue(), ^{
    [updateUrlDelegate setIsBelowMinVersion: (BOOL) (isAppBelowMinVersion == JNI_TRUE)];
  });
}