/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.

#include "net_java_sip_communicator_plugin_addressbook_MacAddressBookUtils.h"

#include "AddrBookContactQuery.h"

#import <AddressBook/AddressBook.h>
#import <Foundation/NSArray.h>
#import <Foundation/NSAutoreleasePool.h>
#import <Foundation/NSData.h>
#import <AddressBook/ABGlobals.h>
#import <Foundation/NSNotification.h>
#import <Foundation/NSObject.h>

static void MacOSXAddrBookContactQuery_idToJObject
    (JNIEnv *jniEnv, id o, jobjectArray jos, jint i, jclass objectClass);

JNIEXPORT jbyteArray JNICALL
Java_net_java_sip_communicator_plugin_addressbook_MacAddressBookUtils_ABPerson_1imageData
    (JNIEnv *jniEnv, jclass clazz, jlong person)
{
    NSData *imageData = [((ABPerson *) person) imageData];
    jbyteArray jImageData;

    if (imageData)
    {
        NSUInteger length = [imageData length];

        if (length)
        {
            jImageData = (*jniEnv)->NewByteArray(jniEnv, length);
            if (jImageData)
            {
                (*jniEnv)->SetByteArrayRegion(
                    jniEnv,
                    jImageData, 0, length,
                    [imageData bytes]);
            }
        }
        else
            jImageData = NULL;
    }
    else
        jImageData = NULL;
    return jImageData;
}

JNIEXPORT jobjectArray JNICALL
Java_net_java_sip_communicator_plugin_addressbook_MacAddressBookUtils_ABRecord_1valuesForProperties
    (JNIEnv *jniEnv, jclass clazz, jlong record, jlongArray properties)
{
    jsize propertyCount;
    jobjectArray values = NULL;

    propertyCount = (*jniEnv)->GetArrayLength(jniEnv, properties);
    if (propertyCount)
    {
        jclass objectClass;

        objectClass = (*jniEnv)->FindClass(jniEnv, "java/lang/Object");
        if (objectClass)
        {
            values
                = (*jniEnv)->NewObjectArray(
                    jniEnv,
                    propertyCount, objectClass, NULL);
            if (values)
            {
                jint i;
                ABRecord *r = (ABRecord *) record;

                for (i = 0; i < propertyCount; i++)
                {
                    jlong property;

                    (*jniEnv)->GetLongArrayRegion(
                            jniEnv,
                            properties, i, 1, &property);
                    MacOSXAddrBookContactQuery_idToJObject(
                        jniEnv,
                        [r valueForProperty:(NSString *)property],
                        values, i,
                        objectClass);
                    if (JNI_TRUE == (*jniEnv)->ExceptionCheck(jniEnv))
                        break;
                }
            }
        }
    }
    return values;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_plugin_addressbook_MacAddressBookUtils_foreachPerson
    (JNIEnv *jniEnv, jclass clazz, jstring query, jobject callback)
{
    jmethodID callbackMethodID;
    jmethodID queryCompleteMethodID;
    NSAutoreleasePool *autoreleasePool;
    ABAddressBook *addressBook;
    NSArray *people;
    NSUInteger peopleCount;
    NSUInteger i;

    callbackMethodID
        = AddrBookContactQuery_getPtrCallbackMethodID(jniEnv, callback);
		
	queryCompleteMethodID
		= AddrBookContactQuery_getPtrQueryCompleteMethodID(jniEnv, callback);
	
    if (!callbackMethodID || !queryCompleteMethodID || (JNI_TRUE == (*jniEnv)->ExceptionCheck(jniEnv)))
        return;

    autoreleasePool = [[NSAutoreleasePool alloc] init];

    addressBook = [ABAddressBook sharedAddressBook];
    people = [addressBook people];
    peopleCount = [people count];
    for (i = 0; i < peopleCount; i++)
    {
        jboolean proceed;
        ABPerson *person = [people objectAtIndex:i];

        proceed
            = (*jniEnv)->CallBooleanMethod(
                jniEnv,
                callback, callbackMethodID,
                person);
        if ((JNI_FALSE == proceed)
                || (JNI_TRUE == (*jniEnv)->ExceptionCheck(jniEnv)))
            break;
    }

	// Finally callback to Java that the query has completed
	(*jniEnv)->CallVoidMethod(
                jniEnv,
                callback,
				queryCompleteMethodID);
	
    [autoreleasePool release];
}

/*
 * Class:     net_java_sip_communicator_plugin_addressbook_MacAddressBookUtils
 * Method:    ABRecord_uniqueId
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_net_java_sip_communicator_plugin_addressbook_MacAddressBookUtils_ABRecord_1uniqueId
  (JNIEnv *jniEnv, jclass clazz, jlong record)
{
    return (*jniEnv)->NewStringUTF(jniEnv, [[(ABRecord *)record uniqueId] UTF8String]);
}

NSString *JavaStringToNSString(JNIEnv *env, jstring aString)
{
  if(aString == NULL)
    return nil;

  const jchar *chars = (*env)->GetStringChars(env, aString, NULL);
  NSString *resultString = [NSString stringWithCharacters:(UniChar *)chars length:(*env)->GetStringLength(env, aString)];
  (*env)->ReleaseStringChars(env, aString, chars);
  return resultString;
}

/*
 * Class:     net_java_sip_communicator_plugin_addressbook_MacAddressBookUtils
 * Method:    setProperty
 * Signature: (Ljava/lang/String;JLjava/lang/String;Ljava/lang/Object;)Z
 */
JNIEXPORT jboolean JNICALL Java_net_java_sip_communicator_plugin_addressbook_MacAddressBookUtils_setProperty
  (JNIEnv *jniEnv, jclass clazz, jstring id, jlong prop, jstring subProperty, jobject value)
{
    void* data;
    ABAddressBook *addressBook;
    ABRecord *r;
    NSAutoreleasePool *autoreleasePool;
    NSString *property;
    BOOL res = FALSE;
    int i;

    autoreleasePool = [[NSAutoreleasePool alloc] init];

    addressBook = [ABAddressBook sharedAddressBook];
    r = [addressBook recordForUniqueId:JavaStringToNSString(jniEnv, id)];

    property = (NSString *)prop;

    if(property == kABFirstNameProperty
        || property == kABLastNameProperty
        || property == kABFirstNamePhoneticProperty
        || property == kABLastNamePhoneticProperty
        || property == kABNicknameProperty
        || property == kABMaidenNameProperty
        || property == kABOrganizationProperty
        || property == kABJobTitleProperty
        || property == kABHomePageProperty
        || property == kABDepartmentProperty
        || property == kABNoteProperty
        || property == kABMiddleNameProperty
        || property == kABMiddleNamePhoneticProperty
        || property == kABTitleProperty
        || property == kABSuffixProperty)
    {
        data = JavaStringToNSString(jniEnv, (jstring)value);
    }
    else if(property == kABBirthdayProperty)
    {
        data = [NSDate dateWithTimeIntervalSince1970:(jlong)value];
    }
    else if(property == kABURLsProperty
            || property == kABCalendarURIsProperty
            || property == kABEmailProperty
            || property == kABRelatedNamesProperty
            || property == kABPhoneProperty
            || property == kABAIMInstantProperty
            || property == kABJabberInstantProperty
            || property == kABMSNInstantProperty
            || property == kABYahooInstantProperty
            || property == kABICQInstantProperty)
    {
        data=[[ABMutableMultiValue alloc] init];
        jobjectArray arr = (jobjectArray)value;
        jsize propertyCount = (*jniEnv)->GetArrayLength(jniEnv, arr);

        for (i = 0; i < propertyCount; i+=2)
        {
            jstring value = (jstring) (*jniEnv)->GetObjectArrayElement(jniEnv, arr, i);
            jstring label = (jstring) (*jniEnv)->GetObjectArrayElement(jniEnv, arr, i+1);

            [(ABMutableMultiValue *) data
                addValue:JavaStringToNSString(jniEnv, value)
                withLabel:JavaStringToNSString(jniEnv, label)];
        }
    }
    else if(property == kABAddressProperty)
    {
        jobjectArray arr = (jobjectArray)value;
        jsize propertyCount = (*jniEnv)->GetArrayLength(jniEnv, arr);

        NSMutableDictionary *addr;
        addr = [NSMutableDictionary dictionary];

        data=[[ABMutableMultiValue alloc] init];
        NSString *subProp = NULL;
        NSString *lastSubProp;
        for (i = 0; i < propertyCount; i+=3)
        {
            jstring value
                = (jstring) (*jniEnv)->GetObjectArrayElement(jniEnv, arr, i);
            jstring label
                = (jstring) (*jniEnv)->GetObjectArrayElement(jniEnv, arr, i+1);
            jstring tmpLastSubProp
                = (jstring) (*jniEnv)->GetObjectArrayElement(jniEnv, arr, i+2);
            lastSubProp = JavaStringToNSString(jniEnv, tmpLastSubProp);
            // Initiates the first sub-property value.
            if(i == 0)
            {
                subProp = lastSubProp;
            }

            // If there is a change in the sub-property, then save the actual
            // one to the address property and create a new sub-property list
            // (Home, Work).
            if(![lastSubProp isEqualToString: subProp])
            {
                [(ABMutableMultiValue *) data addValue:addr withLabel:subProp];
                addr = [NSMutableDictionary dictionary];
                // Sets the new current proeperty
                subProp = lastSubProp;
            }

            //NSLog(@"key:%@, value:%@", JavaStringToNSString(jniEnv, label),
            //JavaStringToNSString(jniEnv, value));
            [addr setObject:JavaStringToNSString(jniEnv, value)
                forKey:JavaStringToNSString(jniEnv, label)];
        }
        // Adds the last sub-property to the address book.
        if(i > 0)
        {
            [(ABMutableMultiValue *) data addValue: addr withLabel: subProp];
        }
    }
    //else if(property == kABOtherDatesProperty)//kABMultiDateProperty
    else
    {
        data = NULL;
    }

    if(data)
        res = [r setValue:data forProperty:(NSString *)property];

    [addressBook save];

    [autoreleasePool release];

    return res;
}

/*
 * Class:     net_java_sip_communicator_plugin_addressbook_MacAddressBookUtils
 * Method:    removeProperty
 * Signature: (Ljava/lang/String;J)Z
 */
JNIEXPORT jboolean JNICALL Java_net_java_sip_communicator_plugin_addressbook_MacAddressBookUtils_removeProperty
  (JNIEnv *jniEnv, jclass clazz, jstring id, jlong property)
{
    ABAddressBook *addressBook;
    ABRecord *r;
    NSAutoreleasePool *autoreleasePool;

    autoreleasePool = [[NSAutoreleasePool alloc] init];

    addressBook = [ABAddressBook sharedAddressBook];
    r = [addressBook recordForUniqueId:JavaStringToNSString(jniEnv, id)];

    BOOL res = [r removeValueForProperty:(NSString *)property];

    [addressBook save];

    [autoreleasePool release];

    return res;
}

#define DEFINE_ABPERSON_PROPERTY_GETTER(property) \
    JNIEXPORT jlong JNICALL \
    Java_net_java_sip_communicator_plugin_addressbook_MacAddressBookUtils_##property \
        (JNIEnv *jniEnv, jclass clazz) \
    { \
        return (jlong) property; \
    }

DEFINE_ABPERSON_PROPERTY_GETTER(kABAIMInstantProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABEmailProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABFirstNameProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABFirstNamePhoneticProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABICQInstantProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABJabberInstantProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABLastNameProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABLastNamePhoneticProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABMiddleNameProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABMiddleNamePhoneticProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABMSNInstantProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABNicknameProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABOrganizationProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABPersonFlags)
DEFINE_ABPERSON_PROPERTY_GETTER(kABPhoneProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABYahooInstantProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABMaidenNameProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABBirthdayProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABJobTitleProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABHomePageProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABURLsProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABCalendarURIsProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABAddressProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABOtherDatesProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABOtherDateComponentsProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABRelatedNamesProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABDepartmentProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABNoteProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABTitleProperty)
DEFINE_ABPERSON_PROPERTY_GETTER(kABSuffixProperty)

#define DEFINE_ABLABEL_PROPERTY_GETTER(property) \
    JNIEXPORT jstring JNICALL \
    Java_net_java_sip_communicator_plugin_addressbook_MacAddressBookUtils_##property \
        (JNIEnv *jniEnv, jclass clazz) \
    { \
        return (*jniEnv)->NewStringUTF(jniEnv, [((NSString *) property) UTF8String]); \
    }
DEFINE_ABLABEL_PROPERTY_GETTER(kABHomePageLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABEmailWorkLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABEmailHomeLabel)
//DEFINE_ABLABEL_PROPERTY_GETTER(kABEmailMobileMeLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABAddressHomeLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABAddressWorkLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABAnniversaryLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABFatherLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABMotherLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABParentLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABBrotherLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABSisterLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABChildLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABFriendLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABSpouseLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABPartnerLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABAssistantLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABManagerLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABPhoneWorkLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABPhoneHomeLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABPhoneMobileLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABPhoneMainLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABPhoneHomeFAXLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABPhoneWorkFAXLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABPhonePagerLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABAIMWorkLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABAIMHomeLabel)
//DEFINE_ABLABEL_PROPERTY_GETTER(kABAIMMobileMeLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABJabberWorkLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABJabberHomeLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABMSNWorkLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABMSNHomeLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABYahooWorkLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABYahooHomeLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABICQWorkLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABICQHomeLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABAddressStreetKey)
DEFINE_ABLABEL_PROPERTY_GETTER(kABAddressCityKey)
DEFINE_ABLABEL_PROPERTY_GETTER(kABAddressStateKey)
DEFINE_ABLABEL_PROPERTY_GETTER(kABAddressZIPKey)
DEFINE_ABLABEL_PROPERTY_GETTER(kABAddressCountryKey)
DEFINE_ABLABEL_PROPERTY_GETTER(kABWorkLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABHomeLabel)
DEFINE_ABLABEL_PROPERTY_GETTER(kABOtherLabel)

static void
MacOSXAddrBookContactQuery_idToJObject
    (JNIEnv *jniEnv,
    id o,
    jobjectArray jos, jint i,
    jclass objectClass)
{
    if (o)
    {
        jobject jo;

        if ([o isKindOfClass:[NSString class]])
        {
            jo = (*jniEnv)->NewStringUTF(jniEnv, [((NSString *) o) UTF8String]);
        }
        else if ([o isKindOfClass:[ABMultiValue class]])
        {
            /*
             * We changed our minds after the initial implementation and decided
             * that we want to display not only the values but the labels as
             * well. In order to minimize the scope of the modifications, we'll
             * be returning each label in the same array right after its
             * corresponding value.
             */
            ABMultiValue *mv = (ABMultiValue *) o;
            NSUInteger mvCount = [mv count];
            jobjectArray joArray
                = (*jniEnv)->NewObjectArray(
                        jniEnv,
                        mvCount * 2 /* value, label */,
                        objectClass, NULL);

            jo = joArray;
            if (joArray)
            {
                NSUInteger j, j2;

                for (j = 0; j < mvCount; j++)
                {
                    j2 = j * 2;

                    //NSLog(@"key:%@, label:%@",
                    //    [mv valueAtIndex:j], [mv labelAtIndex:j]);

                    MacOSXAddrBookContactQuery_idToJObject(
                        jniEnv,
                        [mv valueAtIndex:j],
                        joArray, j2,
                        objectClass);
                    if (JNI_TRUE == (*jniEnv)->ExceptionCheck(jniEnv))
                    {
                        jo = NULL;
                        break;
                    }
                    MacOSXAddrBookContactQuery_idToJObject(
                        jniEnv,
                        [mv labelAtIndex:j],
                        joArray, j2 + 1,
                        objectClass);
                    if (JNI_TRUE == (*jniEnv)->ExceptionCheck(jniEnv))
                    {
                        jo = NULL;
                        break;
                    }
                }
            }
        }
        else if ([o isKindOfClass:[NSNumber class]])
        {
            jclass longClass = (*jniEnv)->FindClass(jniEnv, "java/lang/Long");

            jo = NULL;
            if (longClass)
            {
                jmethodID longMethodID
                    = (*jniEnv)->GetMethodID(
                            jniEnv,
                            longClass, "<init>", "(J)V");

                if (longMethodID)
                {
                    jo
                        = (*jniEnv)->NewObject(
                                jniEnv,
                                longClass, longMethodID,
                                (jlong) ([((NSNumber *) o) longValue]));
                }
            }
        }
        else if ([o isKindOfClass:[NSDictionary class]])
        {
            NSDictionary *dict = (NSDictionary *)o;

            NSUInteger dictCount = [dict count];
            jobjectArray joArray
                = (*jniEnv)->NewObjectArray(
                        jniEnv,
                        dictCount * 2, objectClass, NULL);
            jo = joArray;
            if (joArray)
            {
                NSEnumerator *enumerator = [dict keyEnumerator];
                id key;
                NSUInteger j, j2;
                j = 0;
                while ((key = [enumerator nextObject]))
                {
                    //NSLog(@"key:%@, value:%@", key, [dict objectForKey: key]);

                    j2 = j * 2;

                    MacOSXAddrBookContactQuery_idToJObject(
                        jniEnv,
                        [dict objectForKey: key],
                        joArray, j2,
                        objectClass);
                    if (JNI_TRUE == (*jniEnv)->ExceptionCheck(jniEnv))
                    {
                        jo = NULL;
                        break;
                    }
                    MacOSXAddrBookContactQuery_idToJObject(
                        jniEnv,
                        key,
                        joArray, j2 + 1,
                        objectClass);
                    if (JNI_TRUE == (*jniEnv)->ExceptionCheck(jniEnv))
                    {
                        jo = NULL;
                        break;
                    }

                    j++;
                }
            }
        }
        else
        {
            //NSLog(@"type:%@", NSStringFromClass([o class]));
            jo = NULL;
        }
        if (jo)
            (*jniEnv)->SetObjectArrayElement(jniEnv, jos, i, jo);
    }
}

JNIEXPORT jboolean JNICALL Java_net_java_sip_communicator_plugin_addressbook_MacAddressBookUtils_deleteContact
  (JNIEnv *jniEnv, jclass clazz, jstring id)
{
    ABAddressBook *addressBook;
    ABRecord *r;
    NSAutoreleasePool *autoreleasePool;

    autoreleasePool = [[NSAutoreleasePool alloc] init];

    addressBook = [ABAddressBook sharedAddressBook];
    r = [addressBook recordForUniqueId:JavaStringToNSString(jniEnv, id)];

    BOOL res = [addressBook removeRecord: r];

    [addressBook save];

    [autoreleasePool release];

    return res;
}

JNIEXPORT jstring JNICALL Java_net_java_sip_communicator_plugin_addressbook_MacAddressBookUtils_createContact
  (JNIEnv *jniEnv, jclass clazz)
{
    ABAddressBook *addressBook;
    NSAutoreleasePool *autoreleasePool;

    autoreleasePool = [[NSAutoreleasePool alloc] init];

    addressBook = [ABAddressBook sharedAddressBook];
    ABPerson* person = [[ABPerson alloc] initWithAddressBook:addressBook];

    [addressBook save];

    [autoreleasePool release];

    return
        Java_net_java_sip_communicator_plugin_addressbook_MacAddressBookUtils_ABRecord_1uniqueId(jniEnv, clazz, (jlong) person);
}

JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_plugin_addressbook_MacAddressBookUtils_getContactPointer
  (JNIEnv *jniEnv, jclass clazz, jstring id)
{
    ABAddressBook *addressBook;
    ABRecord *r;
    NSAutoreleasePool *autoreleasePool;

    autoreleasePool = [[NSAutoreleasePool alloc] init];

    addressBook = [ABAddressBook sharedAddressBook];
    r = [addressBook recordForUniqueId:JavaStringToNSString(jniEnv, id)];

    [addressBook save];

    [autoreleasePool release];

    return (jlong) r;
}

@interface MacOSXAddrBookContactSourceService : NSObject
{
@private
    jobject delegateObject;
    JavaVM *vm;
}

- (void)abDatabaseChangedExternallyNotification:(NSNotification *)notification;
- (void)abDatabaseChangedNotification:(NSNotification *)notification;

-(void)clean;
-(void) setDelegate:(jobject)delegate inJNIEnv:(JNIEnv *)jniEnv;
-(void) notify:(id)param methodName:(NSString *)mtdName;
@end /* MacOSXAddrBookContactSourceService */

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_plugin_addressbook_MacAddressBookUtils_start
    (JNIEnv *jniEnv, jclass clazz)
{
    NSAutoreleasePool *pool;
    MacOSXAddrBookContactSourceService *mabcss;

    pool = [[NSAutoreleasePool alloc] init];

    mabcss = [[MacOSXAddrBookContactSourceService alloc] init];
    if (mabcss)
    {
        NSNotificationCenter *notificationCenter
            = [NSNotificationCenter defaultCenter];

        [notificationCenter
            addObserver:mabcss
            selector:@selector(abDatabaseChangedExternallyNotification:)
            name:kABDatabaseChangedExternallyNotification
            object:nil];
        [notificationCenter
            addObserver:mabcss
            selector:@selector(abDatabaseChangedNotification:)
            name:kABDatabaseChangedNotification
            object:nil];
    }

    [pool release];
    return (jlong) mabcss;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_plugin_addressbook_MacAddressBookUtils_stop
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    MacOSXAddrBookContactSourceService *mabcss
        = (MacOSXAddrBookContactSourceService *) ptr;
    NSAutoreleasePool *pool;

    pool = [[NSAutoreleasePool alloc] init];

    [[NSNotificationCenter defaultCenter] removeObserver:mabcss];
    [mabcss clean];
    [mabcss release];

    [pool release];
}

/*
 * Class:     net_java_sip_communicator_plugin_addressbook_MacAddressBookUtils
 * Method:    setDelegate
 * Signature: (JLnet/java/sip/communicator/plugin/addressbook/MacAddressBookUtils/NotificationsDelegate;)V
 */
JNIEXPORT void JNICALL Java_net_java_sip_communicator_plugin_addressbook_MacAddressBookUtils_setDelegate
  (JNIEnv *jniEnv, jclass clazz, jlong ptr, jobject m_delegate)
{
    MacOSXAddrBookContactSourceService *oDelegate;

    if (m_delegate)
    {
        oDelegate = (MacOSXAddrBookContactSourceService *) ptr;
        [oDelegate setDelegate:m_delegate inJNIEnv:jniEnv];
    }
    else
        oDelegate = nil;
}

@implementation MacOSXAddrBookContactSourceService
- (void)clean
{
    [self setDelegate:NULL inJNIEnv:NULL];
}

- (void)setDelegate:(jobject) delegate inJNIEnv:(JNIEnv *)jniEnv
{
    if (self->delegateObject)
    {
        if (!jniEnv)
            (*(self->vm))->AttachCurrentThread(self->vm, (void **)&jniEnv, NULL);
        (*jniEnv)->DeleteGlobalRef(jniEnv, self->delegateObject);
        self->delegateObject = NULL;
        self->vm = NULL;
    }
    if (delegate)
    {
        delegate = (*jniEnv)->NewGlobalRef(jniEnv, delegate);
        if (delegate)
        {
            (*jniEnv)->GetJavaVM(jniEnv, &(self->vm));
            self->delegateObject = delegate;
        }
    }
}

-(void) notify:(id)param methodName:(NSString *)mName
{
    jobject delegate;
    JNIEnv *jniEnv;
    jclass delegateClass = NULL;

    delegate = self->delegateObject;
    if (!delegate)
        return;

    vm = self->vm;
    if (0 != (*vm)->AttachCurrentThreadAsDaemon(vm, (void **)&jniEnv, NULL))
        return;

    delegateClass = (*jniEnv)->GetObjectClass(jniEnv, delegate);
    if(delegateClass)
    {
        jmethodID methodid = NULL;

        if ([param isKindOfClass:[NSString class]])
        {
            methodid = (*jniEnv)->GetMethodID(jniEnv,
                                              delegateClass,
                                              [mName UTF8String],
                                              "(Ljava/lang/String;)V");

            if(methodid)
                (*jniEnv)->CallVoidMethod(jniEnv,
                                      delegate,
                                      methodid,
                                      (*jniEnv)->NewStringUTF(
                                                    jniEnv,
                                                    [param UTF8String]));

        }
        else
        {
            methodid = (*jniEnv)->GetMethodID(jniEnv,
                                              delegateClass,
                                              [mName UTF8String],
                                              "(J)V");
            if(methodid)
                (*jniEnv)->CallVoidMethod(jniEnv,
                                      delegate,
                                      methodid,
                                      (jlong)param);
        }
    }
    (*jniEnv)->ExceptionClear(jniEnv);
}

- (void)abDatabaseChangedExternallyNotification:(NSNotification *)notification
{
    ABAddressBook *addressBook;
    id inserted =
        [[notification userInfo] objectForKey:kABInsertedRecords];
    id updated =
        [[notification userInfo] objectForKey:kABUpdatedRecords];
    id deleted =
        [[notification userInfo] objectForKey:kABDeletedRecords];

    addressBook = [ABAddressBook sharedAddressBook];

    NSUInteger peopleCount;
    NSUInteger i;
    NSString *personID;

    if (inserted)
    {
        NSArray *people;

        if ([inserted isKindOfClass:[NSArray class]])
        {
            people = inserted;
        } else
        {
            people = [NSArray arrayWithObject:(ABPerson *)[addressBook recordForUniqueId:inserted]];
        }

        peopleCount = [people count];
        for (i = 0; i < peopleCount; i++)
        {
            personID = [people objectAtIndex:i];
            ABPerson *person =
                (ABPerson *)[addressBook recordForUniqueId:personID];
            [self notify:person methodName:@"inserted"];
        }
    }

    if (updated)
    {
        NSArray *people;

        if ([updated isKindOfClass:[NSArray class]])
        {
            people = updated;
        }
        else
        {
            people = [NSArray arrayWithObject:(ABPerson *)[addressBook recordForUniqueId:updated]];
        }

        peopleCount = [people count];
        for (i = 0; i < peopleCount; i++)
        {
            personID = [people objectAtIndex:i];
            ABPerson *person =
                (ABPerson *)[addressBook recordForUniqueId:personID];
            [self notify:person methodName:@"updated"];
        }
    }

    if (deleted)
    {
        NSArray *people;

        if ([deleted isKindOfClass:[NSArray class]])
        {
            people = deleted;
        }
        else
        {
            people = [NSArray arrayWithObject:(ABPerson *)[addressBook recordForUniqueId:deleted]];
        }

        peopleCount = [people count];
        for (i = 0; i < peopleCount; i++)
        {
            personID = [people objectAtIndex:i];

            [self notify:personID methodName:@"deleted"];
        }
    }
}

- (void)abDatabaseChangedNotification:(NSNotification *)notification
{
}
@end /* MacOSXAddrBookContactSourceService */
