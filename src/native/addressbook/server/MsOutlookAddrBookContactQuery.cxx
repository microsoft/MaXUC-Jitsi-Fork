/*
* Jitsi, the OpenSource Java VoIP and Instant Messaging client.
*
* Distributable under LGPL license.
* See terms of license at gnu.org.
*/
// Portions (c) Microsoft Corporation. All rights reserved.

#include "MsOutlookAddrBookContactQuery.h"

#include "MsOutlookAddrBookContactSourceService.h"

#include "Tchar.h"
#include "../MAPIBitness.h"
#include "MAPISession.h"
#include "MsOutlookUtils.h"

#include <initguid.h>
#include <jni.h>
#include <Mapidefs.h>
#include <Mapix.h>
#include <windows.h>
#include <time.h>

#define BODY_ENCODING_TEXT_AND_HTML    ((ULONG) 0x00100000)
#define DELETE_HARD_DELETE          ((ULONG) 0x00000010)
#define ENCODING_PREFERENCE            ((ULONG) 0x00020000)
#define ENCODING_MIME                ((ULONG) 0x00040000)
#define OOP_DONT_LOOKUP             ((ULONG) 0x10000000)
#define PR_ATTACHMENT_CONTACTPHOTO PROP_TAG(PT_BOOLEAN, 0x7FFF)

DEFINE_OLEGUID(PSETID_Address, MAKELONG(0x2000 + (0x04), 0x0006), 0, 0);

const MAPIUID MsOutlookAddrBookContactQuery_MuidOneOffEntryID =
{ {
    0x81, 0x2b, 0x1f, 0xa4, 0xbe, 0xa3, 0x10, 0x19,
    0x9d, 0x6e, 0x00, 0xdd, 0x01, 0x0f, 0x54, 0x02
    } };

typedef struct MsOutlookAddrBookContactQuery_OneOffEntryID
{
    ULONG    ulFlags;
    MAPIUID muid;
    ULONG   ulBitMask;
    BYTE    bData[];
} ONEOFFENTRYID;

typedef UNALIGNED ONEOFFENTRYID *MsOutlookAddrBookContactQuery_LPONEOFFENTRYID;

typedef
jboolean(*MsOutlookAddrBookContactQuery_ForeachRowInTableCallback)
(LPUNKNOWN iUnknown,
ULONG entryIDByteCount, LPENTRYID entryID, ULONG objType,
LPCTSTR displayName, LPCTSTR messageClass, LPCTSTR container,
LPCTSTR query, void * callback, void * callbackObject, int folderType);

static ULONG MsOutlookAddrBookContactQuery_rdOpenEntryUlFlags = 0x0;
static ULONG MsOutlookAddrBookContactQuery_rwOpenEntryUlFlags
= MAPI_BEST_ACCESS;

static HRESULT MsOutlookAddrBookContactQuery_HrGetOneProp
(LPMAPIPROP mapiProp, ULONG propTag, LPSPropValue *prop);

HRESULT MsOutlookAddrBookContactQuery_buildOneOff
(LPWSTR displayName, LPWSTR addressType, LPWSTR emailAddress,
ULONG* oneOffEntryIdLength, LPBYTE* oneOffEntryId);
HRESULT MsOutlookAddrBookContactQuery_createEmailAddress
(LPMESSAGE contact, LPWSTR displayName, LPWSTR addressType,
LPWSTR emailAddress, LPWSTR originalDisplayName, LONG providerEmailList[],
LONG providerArrayType, ULONG propIds[], int nbPropId);
static jboolean MsOutlookAddrBookContactQuery_foreachContactInMsgStoresTable
(LPMAPISESSION mapiSession, LPCTSTR query,
void * callback, void * callbackObject, int folderType);
static jboolean MsOutlookAddrBookContactQuery_foreachMailUser
(ULONG objType, LPUNKNOWN iUnknown,
LPCTSTR container,
LPCTSTR query, void * callback, void * callbackObject, int folderType);
static jboolean MsOutlookAddrBookContactQuery_foreachMailUserInContainerTable
(LPMAPICONTAINER mapiContainer, LPMAPITABLE mapiTable,
LPCTSTR container, LPCTSTR table,
LPCTSTR query, void * callback, void * callbackObject, int folderType);
static jboolean MsOutlookAddrBookContactQuery_foreachRowInTable
(LPMAPITABLE mapiTable,
MsOutlookAddrBookContactQuery_ForeachRowInTableCallback rowCallback,
LPUNKNOWN iUnknown, LPCTSTR container, LPCTSTR queryType,
LPCTSTR query, void * callback, void * callbackObject, int folderType);
static void* MsOutlookAddrBookContactQuery_getAttachmentContactPhoto
(LPMESSAGE message, ULONGLONG * length);
void MsOutlookAddrBookContactQuery_getBinaryProp
(LPMAPIPROP entry, ULONG propId, LPSBinary binaryProp);
static HRESULT MsOutlookAddrBookContactQuery_getFolderEntryID
(LPMDB msgStore,
ULONG folderEntryIDByteCount, LPENTRYID folderEntryID,
ULONG *contactsFolderEntryIDByteCount, LPENTRYID *contactsFolderEntryID,
ULONG flags, int folderType);
LPSTR MsOutlookAddrBookContactQuery_getContactId(LPMAPIPROP contact);
LPMAPIFOLDER MsOutlookAddrBookContactQuery_getDefaultFolderId
(ULONG flags, int folderType);
LPMDB MsOutlookAddrBookContactQuery_getDefaultMsgStores(ULONG flags);
ULONG MsOutlookAddrBookContactQuery_getPropTag
(LPMAPIPROP mapiProp, long propId, long propType, int guidType);
static ULONG MsOutlookAddrBookContactQuery_getPropTagFromLid
(LPMAPIPROP mapiProp, LONG lid, int guidType);
static jboolean MsOutlookAddrBookContactQuery_mailUserMatches
(LPMAPIPROP mailUser, LPCTSTR query);
static jboolean MsOutlookAddrBookContactQuery_onForeachContactInMsgStoresTableRow
(LPUNKNOWN mapiSession,
ULONG entryIDByteCount, LPENTRYID entryID, ULONG objType,
LPCTSTR displayName, LPCTSTR messageClass, LPCTSTR buffer,
LPCTSTR query, void * callback, void * callbackObject, int folderType);
static jboolean MsOutlookAddrBookContactQuery_forEachReceiveFolder
(LPMDB msgStore,
LPCTSTR type, LPCTSTR displayName,
ULONG entryIDByteCount, LPENTRYID entryID,
LPCTSTR query, void * callback, void * callbackObject, int folderType);
static jboolean MsOutlookAddrBookContactQuery_onForEachReceiveFolder
(LPUNKNOWN msgStore,
ULONG entryIDByteCount, LPENTRYID entryID, ULONG objType,
LPCTSTR displayName, LPCTSTR type, LPCTSTR msgStoreName,
LPCTSTR query, void * callback, void * callbackObject, int folderType);
static jboolean MsOutlookAddrBookContactQuery_onForEachContactsFolder
(LPMDB msgStore,
ULONG entryIDByteCount, LPENTRYID entryID,
LPCTSTR query, void * callback, void * callbackObject, int folderType);
static jboolean MsOutlookAddrBookContactQuery_onForeachMailUserInContainerTableRow
(LPUNKNOWN mapiContainer,
ULONG entryIDByteCount, LPENTRYID entryID, ULONG objType,
LPCTSTR displayName, LPCTSTR messageClass, LPCTSTR buffer,
LPCTSTR query, void * callback, void * callbackObject, int folderType);
LPUNKNOWN MsOutlookAddrBookContactQuery_openEntryId
(ULONG entryIdSize, LPENTRYID entryId, ULONG flags);
LPUNKNOWN MsOutlookAddrBookContactQuery_openEntryIdStr
(LPCTSTR entryId, ULONG flags);
static void* MsOutlookAddrBookContactQuery_readAttachment
(LPMESSAGE message, LONG method, ULONG num, ULONG cond, ULONGLONG * length);

/**
 * Creates a One-Off entry to register an email address.
 *
 * @param displayName The display name corresponding to the new email address.
 * @param addressType The address type corresponding to the new email address
 * (SMTP).
 * @param emailAddress The email address string.
 * @param oneOffEntryIdLength The length of the One-Off entry id once created.
 * @param oneOffEntriId Pointer used to store the One-Off entry id created.
 *
 * @return S_OK if everything was fine. MAPI_E_INVALID_PARAMETER, if there was
 * an invalid argument. MAPI_E_CALL_FAILED otherwise.
 */
HRESULT MsOutlookAddrBookContactQuery_buildOneOff
(LPWSTR displayName, LPWSTR addressType, LPWSTR emailAddress,
ULONG* oneOffEntryIdLength, LPBYTE* oneOffEntryId)
{
    if (!displayName || !addressType || !emailAddress
        || !oneOffEntryIdLength || !oneOffEntryId)
    {
        if (!displayName)
        {
            LOG_ERROR("Invalid parameter displayName");
        }

        if (!addressType)
        {
            LOG_ERROR("Invalid parameter addressType");
        }

        if (!emailAddress)
        {
            LOG_ERROR("Invalid parameter emailAddress");
        }

        if (!oneOffEntryIdLength)
        {
            LOG_ERROR("Invalid parameter oneOffEntryId");
        }

        if (!oneOffEntryId)
        {
            LOG_ERROR("Invalid parameter oneOffEntryId");
        }

        return MAPI_E_INVALID_PARAMETER;
    }

    // Calculate how large our EID will be
    size_t cbDisplayName
        = wcslen(displayName) * sizeof(WCHAR) + sizeof(WCHAR);
    size_t cbAddressType
        = wcslen(addressType) * sizeof(WCHAR) + sizeof(WCHAR);
    size_t cbEmailAddress
        = wcslen(emailAddress) * sizeof(WCHAR) + sizeof(WCHAR);
    size_t cbEID = sizeof(ONEOFFENTRYID)
        + cbDisplayName + cbAddressType + cbEmailAddress;

    // Allocate our buffer
    MsOutlookAddrBookContactQuery_LPONEOFFENTRYID lpEID
        = (MsOutlookAddrBookContactQuery_LPONEOFFENTRYID)
        malloc(cbEID * sizeof(BYTE));

    // Populate it
    if (lpEID)
    {
        memset(lpEID, 0, cbEID);
        lpEID->muid = MsOutlookAddrBookContactQuery_MuidOneOffEntryID;
        lpEID->ulBitMask |= MAPI_UNICODE; // Set U, the unicode bit
        lpEID->ulBitMask |= OOP_DONT_LOOKUP; // Set L, the no lookup bit
        lpEID->ulBitMask |= MAPI_SEND_NO_RICH_INFO; // Set M, the mime bit
        // Set the encoding format
        lpEID->ulBitMask |=
            ENCODING_PREFERENCE | ENCODING_MIME | BODY_ENCODING_TEXT_AND_HTML;

        LPBYTE pb = lpEID->bData;
        // this will copy the string and the NULL terminator together
        memcpy(pb, displayName, cbDisplayName);
        pb += cbDisplayName;
        memcpy(pb, addressType, cbAddressType);
        pb += cbAddressType;
        memcpy(pb, emailAddress, cbEmailAddress);
        pb += cbEmailAddress;

        // Return it
        *oneOffEntryIdLength = cbEID;
        *oneOffEntryId = (LPBYTE)lpEID;

        return S_OK;
    }
    else
    {
        LOG_ERROR("Failed allocate buffer");
    }

    return MAPI_E_CALL_FAILED;
}

/**
 * Creates a new contact from the outlook database.
 *
 * @return The identifer of the created outlook contact. NULL on failure.
 */
char* MsOutlookAddrBookContactQuery_createContact(void)
{
    LOG_INFO("Creating contact");

    char* messageIdStr = NULL;

    MAPISession_lock();

    LPMAPIFOLDER parentEntry
        = MsOutlookAddrBookContactQuery_getDefaultFolderId(
                          MsOutlookAddrBookContactQuery_rwOpenEntryUlFlags, FOLDER_TYPE_CONTACTS);

    LPMESSAGE message;
    HRESULT res = S_FALSE;

    if (parentEntry != NULL)
    {
        // No need to log here; getDefaultFolderId has already done so.
        res = parentEntry->CreateMessage(NULL, 0, &message);
    }

    if (res == S_OK)
    {
        SPropValue updateValue;

        // PR_MESSAGE_CLASS_W
        updateValue.ulPropTag = PROP_TAG(PT_UNICODE, 0x001A);
        updateValue.Value.lpszW = (LPWSTR)L"IPM.Contact";
        HRESULT nres = ((LPMAPIPROP)message)->SetProps(
            1,
            (LPSPropValue)&updateValue,
            NULL);
        if (nres == S_OK)
        {
            HRESULT hres = ((LPMAPIPROP)message)->SaveChanges(
                FORCE_SAVE | KEEP_OPEN_READWRITE);
            if (hres != S_OK)
            {
                LOG_ERROR("Failed to save changes with error: %lx", hres);
            }
        }
        else
        {
            LOG_ERROR("Failed to set properties with error: %lx", res);
        }

        updateValue.ulPropTag = PROP_TAG(PT_LONG, 0x1080); // PR_ICON_INDEX
        updateValue.Value.l = 512;
        nres = ((LPMAPIPROP)message)->SetProps(
            1,
            (LPSPropValue)&updateValue,
            NULL);
        if (nres == S_OK)
        {
            HRESULT hres = ((LPMAPIPROP)message)->SaveChanges(
                FORCE_SAVE | KEEP_OPEN_READWRITE);
            if (hres != S_OK)
            {
                LOG_ERROR("Failed to save changes with error: %lx", hres);
            }
        }
        else
        {
            LOG_ERROR("Failed to set properties with error: %lx", res);
        }

        messageIdStr
            = MsOutlookAddrBookContactQuery_getContactId((LPMAPIPROP)message);

        ((LPMAPIPROP)message)->Release();

        parentEntry->Release();

        LOG_INFO("Created contact: %s", messageIdStr);
    }
    else
    {
        LOG_ERROR("Failed to create message with error: %lx", res);
    }

    MAPISession_unlock();

    return messageIdStr;
}

/**
 * Creates or modifies an email address.
 *
 * @param contact The contact to add the email address.
 * @param displayName The display name for the email address.
 * @param addressType the address type for the email address (SMTP).
 * @param emailAddress The email address.
 * @param originalDisplayName The original display name for the email address.
 * @param providerEmailList A list of values used to define which email address
 * is set.
 * @param providerArrayType A bitsmask used to define which email address is
 * set.
 * @param propIds A list of property to set for this email address.
 * @param nbPropId The number of properties contained in propIds.
 *
 * @return S_OK if the email address was created/modified.
 */
HRESULT MsOutlookAddrBookContactQuery_createEmailAddress
(LPMESSAGE contact, LPWSTR displayName, LPWSTR addressType,
LPWSTR emailAddress, LPWSTR originalDisplayName, LONG providerEmailList[],
LONG providerArrayType, ULONG propIds[], int nbPropId)
{
    LOG_DEBUG("Creating email address");

    SBinary parentId;
    parentId.cb = 0;
    MsOutlookAddrBookContactQuery_getBinaryProp(
        (LPMAPIPROP)contact,
        0x0E09, //PR_PARENT_ENTRYID,
        &parentId);
    LPMAPIFOLDER parentEntry
        = (LPMAPIFOLDER)MsOutlookAddrBookContactQuery_openEntryId(
        parentId.cb,
        (LPENTRYID)parentId.lpb,
        MsOutlookAddrBookContactQuery_rdOpenEntryUlFlags);
    HRESULT hRes = S_OK;
    MAPINAMEID* rgnmid = new MAPINAMEID[nbPropId];
    LPMAPINAMEID* rgpnmid = new LPMAPINAMEID[nbPropId];
    LPSPropTagArray lpNamedPropTags = NULL;

    for (int i = 0; i < nbPropId; i++)
    {
        rgnmid[i].lpguid = (LPGUID)&PSETID_Address;
        rgnmid[i].ulKind = MNID_ID;
        rgnmid[i].Kind.lID = propIds[i];
        rgpnmid[i] = &rgnmid[i];
    }

    hRes = parentEntry->GetIDsFromNames(
        nbPropId,
        (LPMAPINAMEID*)rgpnmid,
        0,
        &lpNamedPropTags);

    if (SUCCEEDED(hRes))
    {
        if (lpNamedPropTags)
        {
            SPropValue* spvProps = new SPropValue[nbPropId];
            spvProps[0].ulPropTag
                = CHANGE_PROP_TYPE(lpNamedPropTags->aulPropTag[0], PT_MV_LONG);
            spvProps[1].ulPropTag
                = CHANGE_PROP_TYPE(lpNamedPropTags->aulPropTag[1], PT_LONG);
            spvProps[2].ulPropTag
                = CHANGE_PROP_TYPE(lpNamedPropTags->aulPropTag[2], PT_UNICODE);
            spvProps[3].ulPropTag
                = CHANGE_PROP_TYPE(lpNamedPropTags->aulPropTag[3], PT_UNICODE);
            spvProps[4].ulPropTag
                = CHANGE_PROP_TYPE(lpNamedPropTags->aulPropTag[4], PT_UNICODE);
            spvProps[5].ulPropTag
                = CHANGE_PROP_TYPE(lpNamedPropTags->aulPropTag[5], PT_UNICODE);
            spvProps[6].ulPropTag
                = CHANGE_PROP_TYPE(lpNamedPropTags->aulPropTag[6], PT_BINARY);

            spvProps[0].Value.MVl.cValues = 1;
            spvProps[0].Value.MVl.lpl = providerEmailList;

            spvProps[1].Value.l = providerArrayType;

            spvProps[2].Value.lpszW = displayName;
            spvProps[3].Value.lpszW = addressType;
            spvProps[4].Value.lpszW = emailAddress;
            spvProps[5].Value.lpszW = originalDisplayName;

            hRes = MsOutlookAddrBookContactQuery_buildOneOff(
                displayName,
                addressType,
                emailAddress,
                &spvProps[6].Value.bin.cb,
                &spvProps[6].Value.bin.lpb);

            if (SUCCEEDED(hRes))
            {
                hRes = contact->SetProps(nbPropId, spvProps, NULL);
                if (SUCCEEDED(hRes))
                {
                    hRes = contact->SaveChanges(FORCE_SAVE);
                }
                else
                {
                    LOG_ERROR("Failed to set properties: %lx", hRes);
                }
            }
            else
            {
                LOG_ERROR("Failed to build one off contact query: %lx", hRes);
            }

            if (spvProps[6].Value.bin.lpb)
            {
                free(spvProps[6].Value.bin.lpb);
            }

            MAPIFreeBuffer(lpNamedPropTags);

            delete[] spvProps;
        }
        else
        {
            LOG_ERROR("Failed to get ids from names");
        }
    }
    else
    {
        LOG_ERROR("Failed to get ids from names with: %lx", hRes);
    }

    MAPIFreeBuffer(parentId.lpb);
    parentEntry->Release();

    delete[] rgpnmid;
    delete[] rgnmid;

    return hRes;
}

/**
 * Delete the given contact from the outlook database.
 *
 * @param nativeEntryId The identifer of the outlook contact to remove.
 *
 * @return 1 if the deletion succeded. 0 otherwise.
 */
int MsOutlookAddrBookContactQuery_deleteContact(LPCTSTR nativeEntryId)
{
    MAPISession_lock();

    LOG_INFO("Deleting contact with ID: %s", nativeEntryId);

    int res = 0;

    LPUNKNOWN mapiProp;
    if ((mapiProp = MsOutlookAddrBookContactQuery_openEntryIdStr(
        nativeEntryId,
        MsOutlookAddrBookContactQuery_rdOpenEntryUlFlags))
        == NULL)
    {
        LOG_ERROR("Failed to open entry to delete contact");

        MAPISession_unlock();

        return 0;
    }

    SBinary contactId;
    contactId.cb = 0;
    MsOutlookAddrBookContactQuery_getBinaryProp(
        (LPMAPIPROP)mapiProp,
        0x0FFF,
        &contactId);

    SBinary parentId;
    parentId.cb = 0;
    MsOutlookAddrBookContactQuery_getBinaryProp(
        (LPMAPIPROP)mapiProp,
        0x0E09, //PR_PARENT_ENTRYID,
        &parentId);
    LPUNKNOWN parentEntry = MsOutlookAddrBookContactQuery_openEntryId(
        parentId.cb,
        (LPENTRYID)parentId.lpb,
        MsOutlookAddrBookContactQuery_rwOpenEntryUlFlags);

    SBinaryArray deleteIdArray;
    deleteIdArray.cValues = 1;
    deleteIdArray.lpbin = &contactId;

    SCODE sres = ((LPMAPIFOLDER)parentEntry)->DeleteMessages(
        &deleteIdArray,
        0,
        NULL,
        DELETE_HARD_DELETE);

    if (sres == MAPI_E_UNKNOWN_FLAGS)
    {
        // We sometimes see contact deletes fail due to the DELETE_HARD_DELETE flag.
        // If we receive MAPI_E_UNKNOWN_FLAGS, so retry without any flags.
        sres = ((LPMAPIFOLDER)parentEntry)->DeleteMessages(
            &deleteIdArray,
            0,
            NULL,
            0);
    }

    if (sres != S_OK)
    {
        LOG_ERROR("Failed to delete messages with error: %lx", sres);
    }

    res = (sres == S_OK);

    ((LPMAPIPROP)parentEntry)->Release();
    MAPIFreeBuffer(parentId.lpb);
    MAPIFreeBuffer(contactId.lpb);
    ((LPMAPIPROP)mapiProp)->Release();

    MAPISession_unlock();

    return res;
}

void MsOutlookAddrBookContactQuery_foreachMailUser(
    LPCTSTR query, void * callback, void * callbackObject, int folderType)
{
    LOG_DEBUG("Querying mail users: '%s', contacts: %d", query, folderType);

    MAPISession_lock();

    LPMAPISESSION mapiSession = MAPISession_getMapiSession();
    if (!mapiSession)
    {
        MAPISession_unlock();
        return;
    }

    MsOutlookAddrBookContactQuery_foreachContactInMsgStoresTable(
        mapiSession,
        query,
        callback,
        callbackObject,
        folderType);

    MAPISession_unlock();

    LOG_DEBUG("Contact query finished");
}

static jboolean
MsOutlookAddrBookContactQuery_foreachContactInMsgStoresTable
(LPMAPISESSION mapiSession,
LPCTSTR query, void * callback, void * callbackObject, int folderType)
{
    LOG_DEBUG("Querying message stores table: '%s'", query);

    HRESULT hResult;
    LPMAPITABLE msgStoresTable = NULL;
    jboolean proceed = JNI_FALSE;

    hResult = mapiSession->GetMsgStoresTable(0, &msgStoresTable);
    if (HR_SUCCEEDED(hResult))
    {
        if (msgStoresTable)
        {
            proceed
                = MsOutlookAddrBookContactQuery_foreachRowInTable(
                msgStoresTable,
                MsOutlookAddrBookContactQuery_onForeachContactInMsgStoresTableRow,
                (LPUNKNOWN)mapiSession, NULL, _T("message store"),
                query, callback, callbackObject, folderType);
            msgStoresTable->Release();
        }
        else
        {
            LOG_ERROR("Message stores table was null");
        }
    }
    else
    {
        LOG_ERROR("Failed to get message stores table: %lx", hResult);
    }


    return proceed;
}

static jboolean
MsOutlookAddrBookContactQuery_foreachMailUser
(ULONG objType, LPUNKNOWN iUnknown,
LPCTSTR container,
LPCTSTR query, void * callback, void * callbackObject, int folderType)
{
    jboolean proceed = JNI_TRUE;

    switch (objType)
    {
    case MAPI_ABCONT:
    case MAPI_FOLDER:
    {
        LPMAPICONTAINER mapiContainer = (LPMAPICONTAINER)iUnknown;

        HRESULT hResult;
        LPMAPITABLE mapiTable;

        /* Look for MAPI_MAILUSER through the contents. */
        mapiTable = NULL;
        hResult = mapiContainer->GetContentsTable(0, &mapiTable);
        if (HR_SUCCEEDED(hResult))
        {
            if (mapiTable)
            {
                proceed
                    = MsOutlookAddrBookContactQuery_foreachMailUserInContainerTable(
                    mapiContainer, mapiTable,
                    container, _T("contents"),
                    query, callback, callbackObject, folderType);
                mapiTable->Release();
            }
            else
            {
                LOG_ERROR("MAPI Table was null");
            }
        }
        else
        {
            LOG_ERROR("Failed to get contents table: %lx", hResult);
        }

        /* Drill down the hierarchy. */
        if (proceed)
        {
            mapiTable = NULL;
            hResult = mapiContainer->GetHierarchyTable(0, &mapiTable);
            if (HR_SUCCEEDED(hResult))
            {
                if (mapiTable)
                {
                    proceed
                        = MsOutlookAddrBookContactQuery_foreachMailUserInContainerTable(
                        mapiContainer, mapiTable,
                        container, _T("hierarchy"),
                        query, callback, callbackObject, folderType);
                    mapiTable->Release();
                }
                else
                {
                    LOG_ERROR("MAPI Table was null");
                }
            }
            else
            {
                LOG_ERROR("Failed to get hierarchy table: %lx", hResult);
            }
        }

        break;
    }

    case MAPI_MAILUSER:
    case MAPI_MESSAGE:
    {
        if (MsOutlookAddrBookContactQuery_mailUserMatches(
            (LPMAPIPROP)iUnknown, query))
        {
            LPSTR contactId = MsOutlookAddrBookContactQuery_getContactId(
                (LPMAPIPROP)iUnknown);

            boolean(*cb)(LPSTR, void*) = (boolean(*)(LPSTR, void*)) callback;
            proceed = cb(contactId, callbackObject);

            ::free(contactId);
            contactId = NULL;
        }
        break;
    }
    }
    return proceed;
}

static jboolean
MsOutlookAddrBookContactQuery_foreachMailUserInContainerTable
(LPMAPICONTAINER mapiContainer, LPMAPITABLE mapiTable,
LPCTSTR container, LPCTSTR table,
LPCTSTR query, void * callback, void * callbackObject, int folderType)
{
    return
        MsOutlookAddrBookContactQuery_foreachRowInTable(
        mapiTable,
        MsOutlookAddrBookContactQuery_onForeachMailUserInContainerTableRow,
        (LPUNKNOWN)mapiContainer, container, table,
        query, callback, callbackObject, folderType);
}

static jboolean
MsOutlookAddrBookContactQuery_foreachRowInTable
(LPMAPITABLE mapiTable,
MsOutlookAddrBookContactQuery_ForeachRowInTableCallback rowCallback,
LPUNKNOWN iUnknown, LPCTSTR container, LPCTSTR table,
LPCTSTR query, void * callback, void * callbackObject, int folderType)
{
    HRESULT hResult;
    // In case,  that we have failed but other parts of the hierarchy may still
    // succeed.
    jboolean proceed = JNI_TRUE;

    LOG_DEBUG("For each row in '%s' %s table from query: '%s'", container, table, query);

    hResult = mapiTable->SeekRow(BOOKMARK_BEGINNING, 0, NULL);
    if (HR_SUCCEEDED(hResult))
    {
        while (proceed)
        {
            LPSRowSet rows;

            hResult = mapiTable->QueryRows(1, 0, &rows);
            if (HR_FAILED(hResult))
            {
                LOG_ERROR("Failed to query row: %lx", hResult);
                break;
            }

            if (rows->cRows == 1)
            {
                ULONG i;
                LPSRow row = rows->aRow;

                LOG_TRACE("Found row: %x", row);

                ULONG objType = 0;
                SBinary entryIDBinary = { 0, NULL };
                LPTSTR displayName = NULL;
                LPTSTR messageClass = NULL;

                for (i = 0; i < row->cValues; i++)
                {
                    LPSPropValue prop = (row->lpProps) + i;

                    switch (prop->ulPropTag)
                    {
                    case PR_OBJECT_TYPE:
                        objType = prop->Value.ul;
                        break;
                    case PR_MESSAGE_CLASS:
                        if (messageClass == NULL)
                        {
                            size_t sl = strlen(prop->Value.lpszA);
                            HRESULT hr = MAPIAllocateBuffer(
                                sl + 1,
                                (void**)&messageClass);

                            if (S_OK == hr)
                            {
                                CopyMemory(
                                    messageClass,
                                    prop->Value.lpszA,
                                    sl);
                                messageClass[sl] = '\0';
                            }
                            else
                            {
                                messageClass = NULL;
                            }
                        }
                        break;
                    case PR_DISPLAY_NAME:
                        if (displayName == NULL)
                        {
                            size_t sl = strlen(prop->Value.lpszA);
                            HRESULT hr = MAPIAllocateBuffer(
                                sl + 1,
                                (void**)&displayName);

                            if (S_OK == hr)
                            {
                                CopyMemory(
                                    displayName,
                                    prop->Value.lpszA,
                                    sl);
                                displayName[sl] = '\0';
                            }
                            else
                            {
                                displayName = NULL;
                            }
                        }
                        break;
                    case PR_ENTRYID:
                        entryIDBinary = prop->Value.bin;
                        break;
                    }
                }

                if (entryIDBinary.cb && entryIDBinary.lpb)
                {
                    LPENTRYID entryID = NULL;

                    HRESULT hr = MAPIAllocateBuffer(
                        entryIDBinary.cb,
                        (void **)&entryID);

                    if (S_OK == hr)
                    {
                        CopyMemory(
                            entryID,
                            entryIDBinary.lpb,
                            entryIDBinary.cb);

                        /*
                        * We no longer need the rows at this point so free them
                        * before we drill down the hierarchy and allocate even
                        * more rows.
                        */
                        FreeProws(rows);

                        proceed
                            = rowCallback(
                            iUnknown,
                            entryIDBinary.cb, entryID, objType,
                            displayName, messageClass, container,
                            query, callback, callbackObject,
                            folderType);

                        MAPIFreeBuffer(entryID);
                    }
                    else
                    {
                        LOG_ERROR("Failed to allocate buffer");
                        FreeProws(rows);
                    }
                }
                else
                {
                    FreeProws(rows);
                }
            }
            else
            {
                MAPIFreeBuffer(rows);
                break;
            }
        }
    }
    else
    {
        LOG_ERROR("Failed to get attachment table: %lx", hResult);
    }

    return proceed;
}

static void*
MsOutlookAddrBookContactQuery_getAttachmentContactPhoto
(LPMESSAGE message, ULONGLONG * length)
{
    LOG_DEBUG("Getting attachement contact photo: %lx", message);

    HRESULT hResult;
    LPMAPITABLE attachmentTable;
    void* attachmentContactPhoto = NULL;

    hResult = message->GetAttachmentTable(0, &attachmentTable);
    if (HR_SUCCEEDED(hResult))
    {
        hResult = attachmentTable->SeekRow(BOOKMARK_BEGINNING, 0, NULL);
        if (HR_SUCCEEDED(hResult))
        {
            while (1)
            {
                LPSRowSet rows;

                hResult = attachmentTable->QueryRows(1, 0, &rows);
                if (HR_FAILED(hResult))
                {
                    LOG_ERROR("Failed to query row: %lx", hResult);
                    break;
                }

                if (rows->cRows == 1)
                {
                    ULONG i;
                    LPSRow row = rows->aRow;
                    jboolean isAttachmentContactPhotoRow = JNI_FALSE;
                    jboolean hasAttachmentContactPhoto = JNI_FALSE;
                    ULONG attachNum = 0;
                    LONG attachMethod = NO_ATTACHMENT;

                    for (i = 0; i < row->cValues; i++)
                    {
                        LPSPropValue prop = (row->lpProps) + i;

                        switch (prop->ulPropTag)
                        {
                        case PR_ATTACHMENT_CONTACTPHOTO:
                            isAttachmentContactPhotoRow = JNI_TRUE;
                            hasAttachmentContactPhoto
                                = prop->Value.b ? JNI_TRUE : JNI_FALSE;
                            break;
                        case PR_ATTACH_METHOD:
                            attachMethod = prop->Value.l;
                            break;
                        case PR_ATTACH_NUM:
                            attachNum = prop->Value.l;
                            break;
                        }
                    }

                    FreeProws(rows);

                    /*
                    * As the reference says and as discovered in practice,
                    * PR_ATTACHMENT_CONTACTPHOTO is sometimes in IAttach.
                    */
                    if ((isAttachmentContactPhotoRow
                        && hasAttachmentContactPhoto)
                        || !isAttachmentContactPhotoRow)
                    {
                        attachmentContactPhoto
                            = MsOutlookAddrBookContactQuery_readAttachment(
                            message,
                            attachMethod, attachNum,
                            (!isAttachmentContactPhotoRow)
                            ? PR_ATTACHMENT_CONTACTPHOTO
                            : PROP_TAG(PT_UNSPECIFIED, 0),
                            length);
                    }
                    if (isAttachmentContactPhotoRow
                        || attachmentContactPhoto)
                    {
                        /*
                        * The reference says there can be only 1
                        * PR_ATTACHMENT_CONTACTPHOTO.
                        */
                        break;
                    }
                }
                else
                {
                    LOG_ERROR("Query rows returned: %lx", rows->cRows);
                    MAPIFreeBuffer(rows);
                    break;
                }
            }
        }
        else
        {
            LOG_ERROR("Failed to seek to row: %lx", hResult);
        }

        attachmentTable->Release();
    }
    else
    {
        LOG_ERROR("Failed to get attachment table: %lx", hResult);
    }

    return attachmentContactPhoto;
}

/**
 * Gets a binary property for a given entry.
 *
 * @param entry The entry to red the property from.
 * @param propId The property identifier.
 * @param binaryProp A pointer to a SBinary to store the property value
 * retrieved.
 */
void MsOutlookAddrBookContactQuery_getBinaryProp
(LPMAPIPROP entry, ULONG propId, LPSBinary binaryProp)
{
    binaryProp->cb = 0;

    SPropTagArray tagArray;
    tagArray.cValues = 1;
    tagArray.aulPropTag[0] = PROP_TAG(PT_BINARY, propId);

    ULONG propCount;
    LPSPropValue propArray;
    HRESULT hResult = entry->GetProps(
        &tagArray,
        0x80000000, // MAPI_UNICODE.
        &propCount,
        &propArray);

    if (HR_SUCCEEDED(hResult))
    {
        SPropValue prop = propArray[0];
        SCODE res = MAPIAllocateBuffer(prop.Value.bin.cb, (void **)&binaryProp->lpb);

        if (res == S_OK)
        {
            binaryProp->cb = prop.Value.bin.cb;
            memcpy(binaryProp->lpb, prop.Value.bin.lpb, binaryProp->cb);
        }
        else
        {
            LOG_ERROR("Failed to allocate buffer: %lx", res);
        }

        MAPIFreeBuffer(propArray);
    }
    else
    {
        LOG_ERROR("Failed to get properties: %lx", hResult);
    }
}


static HRESULT
MsOutlookAddrBookContactQuery_getFolderEntryID
(LPMDB msgStore,
ULONG folderEntryIDByteCount, LPENTRYID folderEntryID,
ULONG *contactsFolderEntryIDByteCount, LPENTRYID *contactsFolderEntryID,
ULONG flags, int folderType)
{
    HRESULT hResult;
    ULONG objType;
    LPUNKNOWN folder;

    hResult = msgStore->OpenEntry(
        folderEntryIDByteCount,
        folderEntryID,
        NULL,
        flags,
        &objType,
        &folder);

    if (HR_SUCCEEDED(hResult))
    {
        LPSPropValue prop;

        ULONG folderId;
        if (folderType == FOLDER_TYPE_CONTACTS)
        {
            // PR_IPM_CONTACT_ENTRYID
            LOG_DEBUG("Using contact folder id");
            folderId = 0x36D10102;
        }
        else if (folderType == FOLDER_TYPE_CALENDAR)
        {
            // Calendar entry ID
            LOG_DEBUG("Using calendar folder id");
            folderId = 0x36D00102;
        }
        else
        {
            // Default to contacts
            LOG_ERROR("Unknown folder type %d", folderType);
            folderId = 0x36D10102;
        }

        hResult
            = MsOutlookAddrBookContactQuery_HrGetOneProp(
            (LPMAPIPROP)folder,
            folderId,
            &prop);
        if (HR_SUCCEEDED(hResult))
        {
            LPSBinary bin = &(prop->Value.bin);
            SCODE res = MAPIAllocateBuffer(
                bin->cb,
                (void **)contactsFolderEntryID);
            if (S_OK == res)
            {
                hResult = S_OK;
                *contactsFolderEntryIDByteCount = bin->cb;
                CopyMemory(*contactsFolderEntryID, bin->lpb, bin->cb);
            }
            else
            {
                LOG_ERROR("Failed to allocate buffer: %lx", res);
                hResult = MAPI_E_NOT_ENOUGH_MEMORY;
            }
            MAPIFreeBuffer(prop);
        }
        else
        {
            LOG_ERROR("Failed to get property: %lx", hResult);
        }

        folder->Release();
    }
    else
    {
        LOG_ERROR("Failed to open entry: %lx", hResult);
    }

    return hResult;
}

/**
 * Retrieves a string representation of the contact id. This string must be
 * freed by the caller.
 *
 * @param contact A pointer to the instance of the contact.
 *
 * @return A string representation of the contact id. NULL if failed. This
 * string must be freed by yhe caller.
 */
LPSTR MsOutlookAddrBookContactQuery_getContactId(LPMAPIPROP contact)
{
    LPSTR entryId = NULL;

    SBinary binaryProp;
    binaryProp.cb = 0;
    MsOutlookAddrBookContactQuery_getBinaryProp(contact, 0x0FFF, &binaryProp);

    if (binaryProp.cb != 0)
    {
        entryId = (LPSTR)::malloc(binaryProp.cb * 2 + 1);
        HexFromBin(binaryProp.lpb, binaryProp.cb, entryId);
        MAPIFreeBuffer(binaryProp.lpb);
    }
    else
    {
        LOG_ERROR("Failed to get contact ID");
    }

    return entryId;
}

/**
 * Returns a pointer to the default contact folder.
 *
 * @param flags The flags bitmap to control entry id permissions.
 * @param folderType The type of folder to find the default for.
 *
 * @return A pointer to the default contact folder. Or NULL if unavailable.
 */
LPMAPIFOLDER MsOutlookAddrBookContactQuery_getDefaultFolderId
(ULONG flags, int folderType)
{
    LPMAPIFOLDER rootFolder = NULL;
    LPMDB msgStore = MsOutlookAddrBookContactQuery_getDefaultMsgStores(flags);

    if (msgStore != NULL)
    {
        ULONG entryIdLength = 0;
        LPENTRYID receiveFolderEntryID = NULL;

        ULONG contactEntryIdLength = 0;
        LPENTRYID contactsFolderEntryID = NULL;

        HRESULT hResult = msgStore->GetReceiveFolder(
            NULL,
            0,
            &entryIdLength,
            &receiveFolderEntryID,
            NULL);

        if (HR_SUCCEEDED(hResult))
        {
            hResult = MsOutlookAddrBookContactQuery_getFolderEntryID(
                msgStore,
                entryIdLength,
                receiveFolderEntryID,
                &contactEntryIdLength,
                &contactsFolderEntryID,
                flags,
                folderType);
            if (!HR_SUCCEEDED(hResult))
            {
                LOG_ERROR("Failed to get folder entry for type %lx: %lx",
                		  folderType, hResult);
            }
            MAPIFreeBuffer(receiveFolderEntryID);
        }
        else
        {
            LOG_ERROR("Failed to get receive folder of type %lx: %lx",
            		  folderType, hResult);
        }

        ULONG objType;
        hResult = msgStore->OpenEntry(
            contactEntryIdLength,
            contactsFolderEntryID,
            NULL,
            flags,
            &objType,
            (LPUNKNOWN *)&rootFolder);

        if (!HR_SUCCEEDED(hResult))
        {
            LOG_ERROR("Failed to open entry of type %lx: %lx", folderType, hResult);
        }

        if (contactsFolderEntryID != NULL)
        {
            MAPIFreeBuffer(contactsFolderEntryID);
        }

        msgStore->Release();
    }
    else
    {
        LOG_ERROR("Message store was null");
    }

    return rootFolder;
}

/**
 * Returns the entry ID of the default folder for the supplied type.
 *
 * @param folderType The type of folder to find the default for.
 *
 * @return the entry ID of the default folder.
 */
char* MsOutlookAddrBookContactQuery_getDefaultFolderEntryId(int folderType)
{
    MAPISession_lock();

    ULONG flags = MsOutlookAddrBookContactQuery_rwOpenEntryUlFlags;
    LPMDB msgStore = MsOutlookAddrBookContactQuery_getDefaultMsgStores(flags);
    LPENTRYID contactsFolderEntryID = NULL;
    char* defaultFolderEntryId = NULL;

    if (msgStore != NULL)
    {
        ULONG entryIdLength = 0;
        LPENTRYID receiveFolderEntryID = NULL;

        ULONG contactEntryIdLength = 0;

        HRESULT hResult = msgStore->GetReceiveFolder(
            NULL,
            0,
            &entryIdLength,
            &receiveFolderEntryID,
            NULL);

        if (HR_SUCCEEDED(hResult))
        {
            hResult = MsOutlookAddrBookContactQuery_getFolderEntryID(
                msgStore,
                entryIdLength,
                receiveFolderEntryID,
                &contactEntryIdLength,
                &contactsFolderEntryID,
                flags,
                folderType);

            char* entryString = (char*)malloc((contactEntryIdLength * 2) + 1);
            HexFromBin((LPBYTE)contactsFolderEntryID, contactEntryIdLength, entryString);

            LOG_INFO("Got default folder ID for type %lx: %s", folderType, entryString);

            defaultFolderEntryId = entryString;

            if (!HR_SUCCEEDED(hResult))
            {
                LOG_ERROR("Failed to get default folder entry of type %lx: %lx",
                		  folderType, hResult);
            }
            MAPIFreeBuffer(receiveFolderEntryID);
        }
        else
        {
            LOG_ERROR("Failed to get receive folder of type %lx: %lx", folderType, hResult);
        }
        msgStore->Release();
    }
    else
    {
        LOG_ERROR("Message store was null");
    }

    MAPISession_unlock();

    return defaultFolderEntryId;
}

/**
 * Open the default message store.
 *
 * @param flags The flags bitmap to control entry id permissions.
 *
 * @return The default message store. Or NULL if unavailable.
 */
LPMDB MsOutlookAddrBookContactQuery_getDefaultMsgStores(ULONG flags)
{
    LPMDB msgStore = NULL;
    LPMAPITABLE msgStoresTable;
    LPMAPISESSION mapiSession = MAPISession_getMapiSession();
    HRESULT hResult;

    hResult = mapiSession->GetMsgStoresTable(0, &msgStoresTable);
    if (HR_SUCCEEDED(hResult))
    {
        if (msgStoresTable)
        {
            hResult = msgStoresTable->SeekRow(BOOKMARK_BEGINNING, 0, NULL);
            if (HR_SUCCEEDED(hResult))
            {
                LPSRowSet rows;

                SBitMaskRestriction bitMaskRestriction;
                bitMaskRestriction.relBMR = BMR_NEZ;
                bitMaskRestriction.ulPropTag = PR_RESOURCE_FLAGS;
                bitMaskRestriction.ulMask = STATUS_DEFAULT_STORE;

                SRestriction defaultFolderRestriction;
                memset(
                    &defaultFolderRestriction,
                    0,
                    sizeof(defaultFolderRestriction));
                defaultFolderRestriction.rt = RES_BITMASK;
                defaultFolderRestriction.res.resBitMask = bitMaskRestriction;
                hResult = HrQueryAllRows(
                    msgStoresTable,
                    NULL,
                    &defaultFolderRestriction, // restriction
                    NULL,
                    0,
                    &rows);
                if (HR_SUCCEEDED(hResult))
                {
                    if (rows->cRows == 1)
                    {
                        SRow row = rows->aRow[0];
                        SBinary entryIDBinary = { 0, NULL };

                        for (ULONG i = 0; i < row.cValues; ++i)
                        {
                            LPSPropValue prop = (row.lpProps) + i;
                            switch (prop->ulPropTag)
                            {
                            case PR_ENTRYID:
                                entryIDBinary = prop->Value.bin;
                                break;
                            }
                        }

                        if (entryIDBinary.cb && entryIDBinary.lpb)
                        {
                            hResult = mapiSession->OpenMsgStore(
                                0,
                                entryIDBinary.cb,
                                (LPENTRYID)entryIDBinary.lpb,
                                NULL,
                                MDB_NO_MAIL | flags,
                                &msgStore);
                        }
                    }
                }
                else
                {
                    LOG_ERROR("Failed to query all rows: %lx", hResult);
                }
                FreeProws(rows);
            }
            else
            {
                LOG_ERROR("Failed to seek to row: %lx", hResult);
            }
            msgStoresTable->Release();
        }
        else
        {
            LOG_ERROR("Failed to get message store from session");
        }
    }
    else
    {
        LOG_ERROR("Failed to get message store with error: %lx", hResult);
    }

    return msgStore;
}

/**
 * Returns the property tag associated for the given identifier and type.
 *
 * @param mapiProp The MAPI object from which we need to get the property tag
 * for a given identifier.
 * @param propId The identifier to resolve into a tag.
 * @param propType The type of the property (PT_UNSPECIFIED, PT_UNICODE, etc.).
 * @param guidType The type of the GUID to use when requesting the prperty
 *
 * @return The property tag associated for the given identifier and type.
 */
ULONG MsOutlookAddrBookContactQuery_getPropTag
(LPMAPIPROP mapiProp, long propId, long propType, int guidType)
{
    ULONG propTag = 0;

    if (propId < 0x8000)
    {
        if (propId == PROP_ID(PR_ATTACHMENT_CONTACTPHOTO))
            propTag = PR_HASATTACH;
        else
            propTag = PROP_TAG(propType, propId);
    }
    else
    {
        propTag = MsOutlookAddrBookContactQuery_getPropTagFromLid(
            (LPMAPIPROP)mapiProp,
            (LONG)propId,
            guidType);
        propTag = CHANGE_PROP_TYPE(propTag, propType);
    }

    return propTag;
}

static ULONG
MsOutlookAddrBookContactQuery_getPropTagFromLid(LPMAPIPROP mapiProp, LONG lid, int guidType)
{
    // Convert the GUID type into the GUID
    // GUI definitions from http://msdn.microsoft.com/en-us/library/bb905283(v=office.12).aspx
    GUID guid;
    GUID PSETID_Appointment = { 0x00062002, 0x0000, 0x0000, { 0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x46 } };
    GUID PSETID_Address     = { 0x00062004, 0x0000, 0x0000, { 0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x46 } };
    GUID PSETID_Meeting     = { 0x6ED8DA90, 0x450B, 0x101B, { 0x98, 0xDA, 0x00, 0xAA, 0x00, 0x3F, 0x13, 0x05 } };

    switch (guidType)
    {
        case GUID_TYPE_ADDRESS:
            guid = PSETID_Address;
            break;

        case GUID_TYPE_APPOINTMENT:
            guid = PSETID_Appointment;
            break;

        case GUID_TYPE_MEETING:
            guid = PSETID_Meeting;
            break;

        default:
            LOG_ERROR("Unknown guid type %x", guidType);
            guid = PSETID_Address;
            break;
    }

    MAPINAMEID propName;
    LPMAPINAMEID propNamePtr;
    HRESULT hResult;
    LPSPropTagArray propTagArray;

    propName.lpguid = (LPGUID)&guid;
    propName.ulKind = MNID_ID;
    propName.Kind.lID = lid;
    propNamePtr = &propName;
    hResult
        = mapiProp->GetIDsFromNames(
        1,
        &propNamePtr,
        MAPI_CREATE,
        &propTagArray);
    if (HR_SUCCEEDED(hResult))
    {
        if (1 == propTagArray->cValues)
        {
            //SPropTagArry.aulPropTag is an array of property tags
            ULONG propTag = propTagArray->aulPropTag[0];

            if (PT_ERROR == PROP_TYPE(propTag))
            {
                LOG_ERROR("Tag has error type: %lx", propTag);
                propTag = PROP_TAG(PT_UNSPECIFIED, lid);
            }
            MAPIFreeBuffer(propTagArray);
            return propTag;
        }
        else
        {
            LOG_ERROR("Unspecifed tag");
            return PROP_TAG(PT_UNSPECIFIED, lid);
        }
    }
    else
    {
        LOG_ERROR("Failed to get id from name: %lx", hResult);
        return PROP_TAG(PT_UNSPECIFIED, lid);
    }
}

/**
 * Get one property for a given contact.
 *
 * @param mapiProp A pointer to the contact.
 * @param propTag The tag of the property to get.
 * @param prop The memory location to store the property value.
 *
 * @return S_OK if everything work fine. Any other value is a failure.
 */
static HRESULT
MsOutlookAddrBookContactQuery_HrGetOneProp(
LPMAPIPROP mapiProp,
ULONG propTag,
LPSPropValue *prop)
{
    SPropTagArray propTagArray;
    HRESULT hResult;
    ULONG valueCount;
    LPSPropValue values;

    propTagArray.cValues = 1;
    propTagArray.aulPropTag[0] = propTag;

    hResult = mapiProp->GetProps(&propTagArray, 0, &valueCount, &values);
    if (HR_SUCCEEDED(hResult))
    {
        ULONG i;
        jboolean propHasBeenAssignedTo = JNI_FALSE;

        for (i = 0; i < valueCount; i++)
        {
            LPSPropValue value = values;

            values++;
            if (value->ulPropTag == propTag)
            {
                *prop = value;
                propHasBeenAssignedTo = JNI_TRUE;
            }
            else
                MAPIFreeBuffer(value);
        }
        if (!propHasBeenAssignedTo)
            hResult = MAPI_E_NOT_FOUND;
        MAPIFreeBuffer(values);
    }
    else
    {
        LOG_ERROR("Failed to get property: %lx", hResult);
    }

    return hResult;
}

/**
 * Deletes one property from a contact.
 *
 * @param propId The outlook property identifier.
 * @param nativeEntryId The identifer of the outlook entry to modify.
 *
 * @return 1 if the deletion succeded. 0 otherwise.
 */
int MsOutlookAddrBookContactQuery_IMAPIProp_1DeleteProp
(long propId, LPCTSTR nativeEntryId)
{
    LOG_INFO("Deleting propertie: %lx for: %s", propId, nativeEntryId);

    LPUNKNOWN mapiProp;
    if ((mapiProp = MsOutlookAddrBookContactQuery_openEntryIdStr(
        nativeEntryId,
        MsOutlookAddrBookContactQuery_rwOpenEntryUlFlags))
        == NULL)
    {
        LOG_ERROR("Failed to open entry to delete properties");
        return 0;
    }

    ULONG baseGroupEntryIdProp = 0;
    switch (propId)
    {
    case 0x00008084: // PidLidEmail1OriginalDisplayName
        baseGroupEntryIdProp = 0x00008080;
        break;
    case 0x00008094: // PidLidEmail2OriginalDisplayName
        baseGroupEntryIdProp = 0x00008090;
        break;
    case 0x000080A4: // PidLidEmail3OriginalDisplayName
        baseGroupEntryIdProp = 0x000080A0;
        break;
    }
    // If this is a special entry (for email only), then deletes all the
    // corresponding properties to make it work.
    if (baseGroupEntryIdProp != 0)
    {
        LOG_DEBUG("Deleting corresponding properties: %lx", baseGroupEntryIdProp);

        ULONG nbProps = 5;
        ULONG propIds[] =
        {
            (baseGroupEntryIdProp + 0), //0x8080 PidLidEmail1DisplayName
            (baseGroupEntryIdProp + 2), // 0x8082 PidLidEmail1AddressType
            (baseGroupEntryIdProp + 3), // 0x8083 PidLidEmail1EmailAddress
            (baseGroupEntryIdProp + 4), // 0x8084 PidLidEmail1OriginalDisplayName
            (baseGroupEntryIdProp + 5)  // 0x8085 PidLidEmail1OriginalEntryID
        };
        ULONG propTag;
        LPSPropTagArray propTagArray;
        MAPIAllocateBuffer(
            CbNewSPropTagArray(nbProps),
            (void **)&propTagArray);
        propTagArray->cValues = nbProps;
        for (unsigned int i = 0; i < nbProps; ++i)
        {
            // This is only used for contacts so use the contacts GUID
            propTag = MsOutlookAddrBookContactQuery_getPropTag(
                (LPMAPIPROP)mapiProp,
                propIds[i],
                PT_UNICODE,
                GUID_TYPE_ADDRESS);
            *(propTagArray->aulPropTag + i) = propTag;
        }

        HRESULT hResult
            = ((LPMAPIPROP)mapiProp)->DeleteProps(
            propTagArray,
            NULL);

        if (HR_SUCCEEDED(hResult))
        {
            hResult
                = ((LPMAPIPROP)mapiProp)->SaveChanges(
                FORCE_SAVE | KEEP_OPEN_READWRITE);

            if (HR_SUCCEEDED(hResult))
            {
                MAPIFreeBuffer(propTagArray);
                ((LPMAPIPROP)mapiProp)->Release();

                return 1;
            }
            else
            {
                LOG_ERROR("Failed to save changes: %lx", hResult);
            }
        }
        else
        {
            LOG_ERROR("Failed to delete properties: %lx", hResult);
        }

        MAPIFreeBuffer(propTagArray);
        ((LPMAPIPROP)mapiProp)->Release();

        return 0;
    }

    SPropTagArray propToDelete;
    propToDelete.cValues = 1;
    propToDelete.aulPropTag[0] = MsOutlookAddrBookContactQuery_getPropTag(
        (LPMAPIPROP)mapiProp,
        propId,
        PT_UNICODE,
        GUID_TYPE_ADDRESS);

    HRESULT hResult
        = ((LPMAPIPROP)mapiProp)->DeleteProps(
        (LPSPropTagArray)&propToDelete,
        NULL);

    if (HR_SUCCEEDED(hResult))
    {
        hResult
            = ((LPMAPIPROP)mapiProp)->SaveChanges(
            FORCE_SAVE | KEEP_OPEN_READWRITE);

        if (HR_SUCCEEDED(hResult))
        {
            ((LPMAPIPROP)mapiProp)->Release();

            return 1;
        }
        else
        {
            LOG_ERROR("Failed to save changes: %lx", hResult);
        }
    }
    else
    {
        LOG_ERROR("Failed to delete properties: %lx", hResult);
    }

    ((LPMAPIPROP)mapiProp)->Release();

    return 0;
}

HRESULT MsOutlookAddrBookContactQuery_IMAPIProp_1GetProps(
    LPCTSTR nativeEntryId,
    int propIdCount,
    long * propIds,
    long flags,
    void ** props,
    unsigned long* propsLength,
    char * propsType,
    int guidType)
{
    HRESULT hr = E_FAIL;
    LPSPropTagArray propTagArray;

    MAPISession_lock();

    LPUNKNOWN mapiProp = MsOutlookAddrBookContactQuery_openEntryIdStr(
        nativeEntryId,
        MsOutlookAddrBookContactQuery_rdOpenEntryUlFlags);

    if (mapiProp != NULL)
    {
        hr = MAPIAllocateBuffer(
            CbNewSPropTagArray(propIdCount),
            (void **)&propTagArray);

        if (hr == S_OK)
        {
            if (propTagArray)
            {
                propTagArray->cValues = propIdCount;
                for (int i = 0; i < propIdCount; ++i)
                {
                    propsLength[i] = 0;
                    props[i] = NULL;
                    propsType[i] = '\0';

                    long propId = propIds[i];

                    ULONG propTag = MsOutlookAddrBookContactQuery_getPropTag(
                        (LPMAPIPROP)mapiProp,
                        propId,
                        PT_UNSPECIFIED,
                        guidType);
                    *(propTagArray->aulPropTag + i) = propTag;
                    LOG_DEBUG("Prop tag is %x", propTag);
                }

                ULONG propCount = 0;
                LPSPropValue propArray = NULL;

                hr = ((LPMAPIPROP)mapiProp)->GetProps(
                    propTagArray,
                    (ULONG)flags,
                    &propCount,
                    &propArray);

                if (HR_SUCCEEDED(hr))
                {
                    if (propArray)
                    {
                        if (propCount)
                        {
                            ULONG j;

                            for (j = 0; j < propCount; ++j)
                            {
                                LPSPropValue prop = propArray + j;

                                MsOutlookAddrBookContactQuery_Handle_Property(
                                    prop,
                                    mapiProp,
                                    &propsLength[j],
                                    &propsType[j],
                                    &props[j]);
                            }

                            // @@@ SFR 463280
                            // It's not clear from the MSDN docs whether we
                            // should now MAPIFree each individual prop, or the
                            // whole array.  Previously we freed each
                            // individul property in the loop above; but we
                            // also saw what looked like scribblers (which
                            // suggests that freeing the first pointer (j=0)
                            // frees the whole array).
                            // For safety (if we're not sure), we could free
                            // all the props in reverse, finishing with the
                            // array pointer.  This should either be:
                            // - correct, if each prop should be freed; or
                            // - unnecessary but safe, if we should only free
                            //   the first prop.
                            // In fact, trying this led to exceptions in the
                            // MAPIFreeBuffer method.  Again, this suggests
                            // that we should only free the whole array.
                            // If in fact we are supposed to free each
                            // individual element, then not doing so will
                            // result in a memory leak.  But the evidence so
                            // far suggests freeing each individual property is
                            // not necessary.
                            MAPIFreeBuffer(propArray);
                        }
                        else
                        {
                            LOG_WARN("No properties found for %s", nativeEntryId);
                        }
                    }
                    else
                    {
                        LOG_ERROR("Unable to get properties");

                        hr = E_FAIL;
                    }

                    MAPIFreeBuffer(propTagArray);
                }
                else
                {
                    LOG_ERROR("Failed to get properties: %lx", hr);

                    MAPIFreeBuffer(propTagArray);
                }
            }
            else
            {
                LOG_ERROR("Failed to allocate array");

                hr = E_FAIL;
            }
        }
        else
        {
            LOG_ERROR("Failed to allocate buffer: %lx", hr);
        }

        ((LPMAPIPROP)mapiProp)->Release();
    }
    else
    {
        LOG_ERROR("Failed to open entryId when querying properties for %s: %lx", nativeEntryId, hr);
    }

    MAPISession_unlock();

    return hr;
}

/**
 * Parses one contact property.
 *
 * On success, length will contain the size in bytes, type will be a character
 * representing the decoded type, result will be a pointer to the result.
 *
 * On failure, length will be 0, type will be \0 and result will be NULL.
 *
 * @param prop The property to parse
 * @param mapiProp The parent MAPI object
 * @param length [Output] The length of the result
 * @param type [Output] The type of the result
 * @param result [Output] The resulting property to return
 *
 * @return true on success, else false
 */
bool MsOutlookAddrBookContactQuery_Handle_Property(
    LPSPropValue prop,
    LPUNKNOWN mapiProp,
    unsigned long* length,
    char* type,
    void** result)
{
    if (prop)
    {
        switch (PROP_TYPE(prop->ulPropTag))
        {
        case PT_BOOLEAN:
        {
            if (PR_HASATTACH == prop->ulPropTag)
            {
                if (prop->Value.b)
                {
                    *result = MsOutlookAddrBookContactQuery_getAttachmentContactPhoto(
                        (LPMESSAGE)mapiProp,
                        (ULONGLONG*)length);

                    if (result != NULL)
                    {
                        *type = 'b'; // byte array
                        return true;
                    }
                    else
                    {
                        LOG_ERROR("Failed to get attachment contact photo");
                    }
                }
                else
                {
                    LOG_ERROR("Binary value not set for property");
                }
            }
            else
            {
                // No attachment - don't care about this case.
            }
            break;
        }

        case PT_LONG:
        {
            *length = sizeof(long);
            if ((*result = malloc(*length))
                != NULL)
            {
                memcpy(*result,
                    &prop->Value.l,
                    *length);
                *type = 'l'; // long
                return true;
            }
            else
            {
                LOG_ERROR("Failed to allocate data for integer property");
            }
            break;
        }

        case PT_STRING8:
        {
            if (prop->Value.lpszA)
            {
                *length = strlen(prop->Value.lpszA) + 1;
                if ((*result = malloc(*length))
                    != NULL)
                {
                    memcpy(
                        *result,
                        prop->Value.lpszA,
                        *length);
                    *type = 's'; // 8 bits string
                    return true;
                }
                else
                {
                    LOG_ERROR("Failed to allocate data for string property");
                }
            }
            else
            {
                LOG_ERROR("String not set for string property");
            }
            break;
        }

        case PT_UNICODE:
        {
            if (prop->Value.lpszW)
            {
                *length = (wcslen(prop->Value.lpszW) + 1) * 2;
                if ((*result = malloc(*length))
                    != NULL)
                {
                    memcpy(
                        *result,
                        prop->Value.lpszW,
                        *length);
                    *type = 'u'; // 16 bits string
                    return true;
                }
                else
                {
                    LOG_ERROR("Failed to allocate data for unicode property");
                }
            }
            else
            {
                LOG_ERROR("String not set for Unicode property");
            }
            break;
        }

        case PT_BINARY:
        {
            *length = prop->Value.bin.cb * 2 + 1;
            if ((*result = malloc(*length))
                != NULL)
            {
                HexFromBin(
                    prop->Value.bin.lpb,
                    prop->Value.bin.cb,
                    (LPSTR)*result);

                *type = 's'; // 16 bits string
                return true;
            }
            else
            {
                LOG_ERROR("Failed to allocate data for binary property");
            }
            break;
        }

        case PT_SYSTIME:
        {
            FILETIME lpLocalFileTime;
            SYSTEMTIME systime;

            FileTimeToLocalFileTime(&prop->Value.ft, &lpLocalFileTime);
            FileTimeToSystemTime(&prop->Value.ft, &systime);

            *length = sizeof(SYSTEMTIME);
            if ((*result = malloc(*length)) != NULL)
            {
                memcpy(*result, &systime, *length);
                *type = 't'; // time
                return true;
            }
            else
            {
                LOG_ERROR("Failed to allocate data for integer property");
            }

            break;
        }

        case PT_ERROR:
        {
            LOG_ERROR("Error payload type, %x, tag: 0x%08X", prop->Value.err, prop->ulPropTag);
            break;
        }

        default:
        {
            // The tag is [id<<16|type], so logging it gets both.
            LOG_ERROR("Unknown property type for tag: 0x%08X", prop->ulPropTag);
        }
        }
    }
    else
    {
        LOG_ERROR("Property not set");
    }

    // Failed to handle property. Mark it as unknown.
    *length = 0;
    *type = '\0';
    *result = NULL;

    return false;
}

/**
 * Saves one contact property.
 *
 * @param propId The outlook property identifier.
 * @param nativeValue The value to set to the outlook property.
 * @param nativeEntryId The identifer of the outlook entry to modify.
 *
 * @return 1 if the modification succeded. 0 otherwise.
 */
int MsOutlookAddrBookContactQuery_IMAPIProp_1SetPropString
(long propId, const wchar_t* nativeValue, LPCTSTR nativeEntryId)
{
    HRESULT hResult;
    LPUNKNOWN mapiProp;

    MAPISession_lock();

    if ((mapiProp = MsOutlookAddrBookContactQuery_openEntryIdStr(
        nativeEntryId,
        MsOutlookAddrBookContactQuery_rwOpenEntryUlFlags))
        == NULL)
    {
        LOG_ERROR("Failed to open entryId %s when setting properties");

        MAPISession_unlock();
        return 0;
    }

    size_t valueLength = wcslen(nativeValue);
    LPWSTR wCharValue = (LPWSTR)::malloc((valueLength + 1) * sizeof(wchar_t));
    memcpy(wCharValue, nativeValue, (valueLength + 1) * sizeof(wchar_t));

    ULONG baseGroupEntryIdProp = 0;
    switch (propId)
    {
    case 0x00008084: // PidLidEmail1OriginalDisplayName
        baseGroupEntryIdProp = 0x00008080;
        break;
    case 0x00008094: // PidLidEmail2OriginalDisplayName
        baseGroupEntryIdProp = 0x00008090;
        break;
    case 0x000080A4: // PidLidEmail3OriginalDisplayName
        baseGroupEntryIdProp = 0x000080A0;
        break;
    }
    // If this is a special entry (for email only), then updates all the
    // corresponding properties to make it work.
    if (baseGroupEntryIdProp != 0)
    {
        ULONG nbProps = 7;
        ULONG propIds[] =
        {
            0x8028, // PidLidAddressBookProviderEmailList
            0x8029, // PidLidAddressBookProviderArrayType
            (baseGroupEntryIdProp + 0), //0x8080 PidLidEmail1DisplayName
            (baseGroupEntryIdProp + 2), // 0x8082 PidLidEmail1AddressType
            (baseGroupEntryIdProp + 3), // 0x8083 PidLidEmail1EmailAddress
            (baseGroupEntryIdProp + 4), // 0x8084 PidLidEmail1OriginalDisplayName
            (baseGroupEntryIdProp + 5) // 0x8085 PidLidEmail1OriginalEntryID
        };
        ULONG propTag;
        ULONG propCount;
        LPSPropValue propArray;
        LPSPropTagArray propTagArray;
        MAPIAllocateBuffer(
            CbNewSPropTagArray(nbProps),
            (void **)&propTagArray);
        propTagArray->cValues = nbProps;
        for (unsigned int i = 0; i < nbProps; ++i)
        {
            // Updating a property is for contacts only, thus use the
            // contacts GUID
            propTag = MsOutlookAddrBookContactQuery_getPropTag(
                (LPMAPIPROP)mapiProp,
                propIds[i],
                PT_UNSPECIFIED,
                GUID_TYPE_ADDRESS);
            *(propTagArray->aulPropTag + i) = propTag;
        }
        hResult = ((LPMAPIPROP)mapiProp)->GetProps(
            propTagArray,
            MAPI_UNICODE,
            &propCount,
            &propArray);

        if (SUCCEEDED(hResult))
        {
            LPWSTR addressType = (LPWSTR)L"SMTP";
            LONG providerEmailList[1];
            switch (propId)
            {
            case 0x00008084: // PidLidEmail1OriginalDisplayName
                providerEmailList[0] = 0x00000000;
                propArray[1].Value.l |= 0x00000001;
                break;
            case 0x00008094: // PidLidEmail2OriginalDisplayName
                providerEmailList[0] = 0x00000001;
                propArray[1].Value.l |= 0x00000002;
                break;
            case 0x000080A4: // PidLidEmail3OriginalDisplayName
                providerEmailList[0] = 0x00000002;
                propArray[1].Value.l |= 0x00000004;
                break;
            }

            propArray[0].Value.MVl.cValues = 1;
            propArray[0].Value.MVl.lpl = providerEmailList;

            if (propArray[2].ulPropTag == PT_ERROR
                || propArray[2].Value.err == MAPI_E_NOT_FOUND
                || propArray[2].Value.lpszW == NULL)
            {
                propArray[2].Value.lpszW = wCharValue;
            }
            if (propArray[3].ulPropTag == PT_ERROR
                || propArray[3].Value.err == MAPI_E_NOT_FOUND
                || propArray[3].Value.lpszW == NULL)
            {
                propArray[3].Value.lpszW = addressType;
            }
            if (propArray[4].ulPropTag == PT_ERROR
                || propArray[4].Value.err == MAPI_E_NOT_FOUND
                || propArray[4].Value.lpszW == NULL
                || wcsncmp(propArray[3].Value.lpszW, addressType, 4) == 0)
            {
                propArray[4].Value.lpszW = wCharValue;
            }
            propArray[5].Value.lpszW = wCharValue;

            HRESULT hr = MsOutlookAddrBookContactQuery_createEmailAddress(
                (LPMESSAGE)mapiProp,
                wCharValue, // displayName
                addressType, // addressType
                wCharValue, // emailAddress
                wCharValue, // originalDisplayName
                providerEmailList,
                propArray[1].Value.l,
                propIds,
                7);

            if (hr == S_OK)
            {
                MAPIFreeBuffer(propArray);
                MAPIFreeBuffer(propTagArray);
                ((LPMAPIPROP)mapiProp)->Release();
                ::free(wCharValue);
                wCharValue = NULL;

                MAPISession_unlock();

                return 1;
            }
            else
            {
                LOG_ERROR("Failed to create email address: %lx", hr);
            }
        }
        else
        {
            LOG_ERROR("Failed to get properties: %lx", hResult);
        }
        MAPIFreeBuffer(propTagArray);
        ((LPMAPIPROP)mapiProp)->Release();
        ::free(wCharValue);
        wCharValue = NULL;

        MAPISession_unlock();

        return 0;
    }

    // Setting a property only for contacts, thus use the contact GUID
    SPropValue updateValue;
    updateValue.ulPropTag = MsOutlookAddrBookContactQuery_getPropTag(
        (LPMAPIPROP)mapiProp,
        propId,
        PT_UNICODE,
        GUID_TYPE_ADDRESS);
    updateValue.Value.lpszW = wCharValue;

    hResult = ((LPMAPIPROP)mapiProp)->SetProps(
        1,
        (LPSPropValue)&updateValue,
        NULL);

    if (HR_SUCCEEDED(hResult))
    {
        HRESULT hResult
            = ((LPMAPIPROP)mapiProp)->SaveChanges(
            FORCE_SAVE | KEEP_OPEN_READWRITE);

        if (HR_SUCCEEDED(hResult))
        {
            ((LPMAPIPROP)mapiProp)->Release();
            ::free(wCharValue);
            wCharValue = NULL;

            MAPISession_unlock();

            return 1;
        }
        else
        {
            LOG_ERROR("Failed to save changes to entryId %s when setting properties: %lx",
                nativeEntryId, hResult);
        }
    }
    else
    {
        LOG_ERROR("Failed to set properties on entryId %s: %lx",
            nativeEntryId, hResult);
    }

    ((LPMAPIPROP)mapiProp)->Release();
    ::free(wCharValue);
    wCharValue = NULL;

    MAPISession_unlock();

    return 0;
}

static jboolean
MsOutlookAddrBookContactQuery_mailUserMatches
(LPMAPIPROP mailUser, LPCTSTR query)
{
    // TODO Auto-generated method stub
    return JNI_TRUE;
}

// Called for each message store
static jboolean
MsOutlookAddrBookContactQuery_onForeachContactInMsgStoresTableRow
(LPUNKNOWN mapiSession,
ULONG entryIDByteCount, LPENTRYID entryID, ULONG objType,
LPCTSTR displayName, LPCTSTR messageClass, LPCTSTR buffer,
LPCTSTR query, void * callback, void * callbackObject, int folderType)
{
    LOG_DEBUG("Found message store '%s' from query: '%s' with entry: '%lx', type: '%lx'", displayName, query, entryID, objType);

    HRESULT hResult;
    LPMDB msgStore;
    // In case, that we've failed but other parts of the hierarchy may still
    // succeed.
    jboolean proceed = JNI_TRUE;

    hResult = ((LPMAPISESSION)mapiSession)->OpenMsgStore(
        0,
        entryIDByteCount, entryID,
        NULL,
        MDB_NO_MAIL | MsOutlookAddrBookContactQuery_rdOpenEntryUlFlags,
        &msgStore);

    if (HR_SUCCEEDED(hResult))
    {
        LOG_DEBUG("Opened Message Store: '%s' with result: %lx", displayName, hResult);

        LPMAPITABLE receiveFolderTable;
        hResult = msgStore->GetReceiveFolderTable(0, &receiveFolderTable);

        if (HR_SUCCEEDED(hResult))
        {
            LOG_DEBUG("Got receive folder table for message store %s with result: %lx", displayName, hResult);

            proceed = MsOutlookAddrBookContactQuery_foreachRowInTable(
                receiveFolderTable,
                MsOutlookAddrBookContactQuery_onForEachReceiveFolder,
                (LPUNKNOWN)msgStore, displayName, _T("receive folder"),
                query, callback, callbackObject, folderType);

            receiveFolderTable->Release();
        }
        else
        {
            LOG_ERROR("Failed to get receive folder table for message store %s with result: %lx", displayName, hResult);
        }

        ULONG receiveFolderEntryIDByteCount = 0;
        LPENTRYID receiveFolderEntryID = NULL;
        LPTSTR receiveFolderEntryType = NULL;
        char* receiveFolderString = NULL;

        hResult = msgStore->GetReceiveFolder(
            NULL,
            0,
            &receiveFolderEntryIDByteCount,
            &receiveFolderEntryID,
            &receiveFolderEntryType);
        if (HR_SUCCEEDED(hResult))
        {
            receiveFolderString = (char*)malloc((receiveFolderEntryIDByteCount * 2) + 1);
            HexFromBin((LPBYTE)receiveFolderEntryID, receiveFolderEntryIDByteCount, receiveFolderString);

            LOG_DEBUG("Got receive folder '%s' for receive folder type '%s' for message store %s with result: %lx",
                receiveFolderString,
                receiveFolderEntryType,
                displayName,
                hResult);

            proceed = MsOutlookAddrBookContactQuery_forEachReceiveFolder(
                msgStore,
                receiveFolderEntryType, displayName,
                receiveFolderEntryIDByteCount, receiveFolderEntryID,
                query,
                callback,
                callbackObject,
                folderType);

            free(receiveFolderString);
            MAPIFreeBuffer(receiveFolderEntryID);
        }
        else
        {
            LOG_ERROR("Failed to get receive folder for message store '%s' with result: %lx", displayName, hResult);
        }

        LOG_DEBUG("Using default contacts folder for message store %s with result: %lx", displayName, hResult);

        proceed = MsOutlookAddrBookContactQuery_forEachReceiveFolder(
            msgStore,
            _T("default"), displayName,
            0, NULL,
            query,
            callback,
            callbackObject,
            folderType);

        msgStore->Release();
    }
    else
    {
        LOG_ERROR("Failed to open message store: %lx", hResult);
    }

    return proceed;
}

static jboolean
MsOutlookAddrBookContactQuery_onForEachReceiveFolder
(LPUNKNOWN msgStore,
ULONG entryIDByteCount, LPENTRYID entryID, ULONG objType,
LPCTSTR displayName, LPCTSTR type, LPCTSTR msgStoreName,
LPCTSTR query, void * callback, void * callbackObject, int folderType)
{
    jboolean proceed = JNI_TRUE;
    char* entryString = (char*)malloc((entryIDByteCount * 2) + 1);
    HexFromBin((LPBYTE)entryID, entryIDByteCount, entryString);

    LOG_DEBUG("Found receive folder entry ID - '%s' - for receive folder type '%s' for message store '%s'", entryString, type, msgStoreName);

    free(entryString);

    proceed = MsOutlookAddrBookContactQuery_forEachReceiveFolder(
        ((LPMDB)msgStore),
        type, msgStoreName,
        entryIDByteCount, entryID,
        query, callback, callbackObject,
        folderType);

    return proceed;
}

static jboolean
MsOutlookAddrBookContactQuery_forEachReceiveFolder
(LPMDB msgStore,
LPCTSTR type, LPCTSTR msgStoreName,
ULONG entryIDByteCount, LPENTRYID entryID,
LPCTSTR query, void * callback, void * callbackObject, int folderType)
{
    jboolean proceed = JNI_TRUE;
    ULONG contactsFolderEntryIDByteCount = 0;
    LPENTRYID contactsFolderEntryID = NULL;

    HRESULT hResult = MsOutlookAddrBookContactQuery_getFolderEntryID(
        msgStore,
        entryIDByteCount,
        entryID,
        &contactsFolderEntryIDByteCount,
        &contactsFolderEntryID,
        MsOutlookAddrBookContactQuery_rdOpenEntryUlFlags,
        folderType);

    if (HR_SUCCEEDED(hResult))
    {
        proceed = MsOutlookAddrBookContactQuery_onForEachContactsFolder(
            msgStore,
            contactsFolderEntryIDByteCount,
            contactsFolderEntryID,
            query,
            callback,
            callbackObject,
            folderType);

        MAPIFreeBuffer(contactsFolderEntryID);
    }
    else
    {
        LOG_ERROR("Failed to get contacts folder entry ID for '%s' receive folder for message store '%s' with result: %lx", type, msgStoreName, hResult);
    }

    return proceed;
}

static jboolean
MsOutlookAddrBookContactQuery_onForEachContactsFolder
(LPMDB msgStore,
ULONG entryIDByteCount, LPENTRYID entryID,
LPCTSTR query, void * callback, void * callbackObject, int folderType)
{
    jboolean proceed = JNI_TRUE;
    ULONG contactsFolderObjType;
    LPUNKNOWN contactsFolder;

    LPTSTR folderName = NULL;
    LPCTSTR displayName = _T("");

    HRESULT hResult = msgStore->OpenEntry(
        entryIDByteCount,
        entryID,
        NULL,
        MsOutlookAddrBookContactQuery_rdOpenEntryUlFlags,
        &contactsFolderObjType,
        &contactsFolder);

    if (HR_SUCCEEDED(hResult))
    {
        LPSPropValue prop;

        HRESULT hres = MsOutlookAddrBookContactQuery_HrGetOneProp((LPMAPIPROP)contactsFolder, PR_DISPLAY_NAME, &prop);

        if (HR_SUCCEEDED(hres))
        {
            size_t folderNameL = strlen(prop->Value.lpszA);
            hres = MAPIAllocateBuffer(
                folderNameL + 1,
                (void**)&folderName);
            if (S_OK == hres)
            {
                CopyMemory(
                    folderName,
                    prop->Value.lpszA,
                    folderNameL);
                folderName[folderNameL] = '\0';

                LOG_ERROR("Opened contacts folder %s", folderName);
            }

            displayName = folderName;

            MAPIFreeBuffer(prop);
        }

        if (hres != S_OK)
        {
            LOG_ERROR("Opened contacts folder - failed to get name");
            displayName = _T("Unknown");
        }

        proceed = MsOutlookAddrBookContactQuery_foreachMailUser(
            contactsFolderObjType,
            contactsFolder,
            displayName,
            query,
            callback,
            callbackObject,
            folderType);

        if (folderName != NULL)
        {
            MAPIFreeBuffer(folderName);
        }

        contactsFolder->Release();
    }
    else
    {
        LOG_ERROR("Failed to open contacts folder with error: %lx", hResult);
    }

    return proceed;
}

static jboolean
MsOutlookAddrBookContactQuery_onForeachMailUserInContainerTableRow
(LPUNKNOWN mapiContainer,
ULONG entryIDByteCount, LPENTRYID entryID, ULONG objType,
LPCTSTR displayName, LPCTSTR messageClass, LPCTSTR container,
LPCTSTR query, void * callback, void * callbackObject, int folderType)
{
    HRESULT hResult;
    LPUNKNOWN iUnknown;
    jboolean proceed;

    LOG_DEBUG("Found '%s' of type '%s' - '%lx' in '%s'", displayName, messageClass, objType, container);

    // Make write failed and image load.
    hResult = ((LPMAPICONTAINER)mapiContainer)->OpenEntry(
        entryIDByteCount,
        entryID,
        NULL,
        MsOutlookAddrBookContactQuery_rdOpenEntryUlFlags,
        &objType,
        &iUnknown);
    if (HR_SUCCEEDED(hResult))
    {
        proceed = MsOutlookAddrBookContactQuery_foreachMailUser(
            objType,
            iUnknown,
            displayName,
            query,
            callback,
            callbackObject,
            folderType);
        iUnknown->Release();
    }
    else
    {
        /* We've failed but other parts of the hierarchy may still succeed. */
        LOG_ERROR("Failed to open entry '%s' of type '%lx' in '%s' with result: %lx", displayName, objType, container, hResult);
        proceed = JNI_TRUE;
    }
    return proceed;
}

/**
 * Opens an object based on the string representation of its entry id.
 *
 * @param entryIdStr The identifier of the entry to open.
 * @param flags The flags bitmap to control entry id permissions.
 *
 * @return A pointer to the opened entry. NULL if anything goes wrong.
 */
LPUNKNOWN MsOutlookAddrBookContactQuery_openEntryIdStr(
    LPCTSTR entryIdStr,
    ULONG flags)
{
    LPUNKNOWN entry = NULL;
    ULONG entryIdSize = _tcslen(entryIdStr) / 2;
    LPENTRYID entryId = (LPENTRYID)malloc(entryIdSize * sizeof(char));

    if (entryId != NULL)
    {
        if (FBinFromHex((LPSTR)entryIdStr, (LPBYTE)entryId))
        {
            entry = MsOutlookAddrBookContactQuery_openEntryId(
                entryIdSize,
                entryId,
                flags);
        }
        else
        {
            LOG_ERROR("Failed to convert entryId to binary: %s", entryIdStr);
        }
        ::free(entryId);
    }
    else
    {
        LOG_ERROR("Attempting to open null entryId");
    }
    return entry;
}

/**
 * Opens an object based on its entry id.
 *
 * @param entryIdSize The size of the identifier of the entry to open.
 * @param entryId The identifier of the entry to open.
 * @param flags The flags bitmap to control entry id permissions.
 *
 * @return A pointer to the opened entry. NULL if anything goes wrong.
 */
LPUNKNOWN MsOutlookAddrBookContactQuery_openEntryId
(ULONG entryIdSize, LPENTRYID entryId, ULONG flags)
{
    LPMAPISESSION mapiSession = MAPISession_getMapiSession();
    ULONG objType;
    LPUNKNOWN entry = NULL;

    HRESULT hr = mapiSession->OpenEntry(
        entryIdSize,
        entryId,
        NULL,
        flags,
        &objType,
        &entry);

    if (hr == MAPI_E_NOT_FOUND)
    {
        LOG_TRACE("Entry was not found");
    }
    else if (hr != S_OK)
    {
        LOG_ERROR("Failed to open entry: %lx", hr);
    }

    return entry;
}

static void*
MsOutlookAddrBookContactQuery_readAttachment
(LPMESSAGE message, LONG method, ULONG num, ULONG cond, ULONGLONG * length)
{
    void* attachment = NULL;

    if (ATTACH_BY_VALUE == method)
    {
        HRESULT hResult;
        LPATTACH attach;

        hResult = message->OpenAttach(num, NULL, 0, &attach);

        if (HR_SUCCEEDED(hResult))
        {
            IStream *stream = NULL;

            if (PT_BOOLEAN == PROP_TYPE(cond))
            {
                LPSPropValue condValue;

                hResult = MsOutlookAddrBookContactQuery_HrGetOneProp(
                    (LPMAPIPROP)attach,
                    cond,
                    &condValue);
                if (HR_SUCCEEDED(hResult))
                {
                    if ((PT_BOOLEAN != PROP_TYPE(condValue->ulPropTag))
                        || !(condValue->Value.b))
                    {
                        hResult = MAPI_E_NOT_FOUND;
                        LOG_ERROR("Invalid boolean property type: %x value: %x",
                            PROP_TYPE(condValue->ulPropTag),
                            (condValue->Value.b));
                    }
                    MAPIFreeBuffer(condValue);
                }
                else
                {
                    LOG_ERROR("Failed to get boolean property for attachment: %lx",
                        hResult);
                }
            }

            if (HR_SUCCEEDED(hResult))
            {
                hResult
                    = ((LPMAPIPROP)attach)->OpenProperty(
                    PR_ATTACH_DATA_BIN,
                    &IID_IStream, 0,
                    0,
                    (LPUNKNOWN *)&stream);

                if (HR_SUCCEEDED(hResult) && stream)
                {
                    STATSTG statstg;

                    hResult = stream->Stat(&statstg, STATFLAG_NONAME);
                    if ((S_OK == hResult) && ((*length = statstg.cbSize.QuadPart)))
                    {
                        if ((attachment = (void*)malloc(*length)) != NULL)
                        {
                            ULONG read;
                            jint mode;

                            hResult = stream->Read(
                                attachment, (ULONG)(*length), &read);
                            mode = ((S_OK == hResult) || (S_FALSE == hResult))
                                ? 0
                                : JNI_ABORT;
                            if (0 != mode)
                            {
                                free(attachment);
                                attachment = NULL;
                            }
                        }
                    }

                    stream->Release();
                }
                else
                {
                    LOG_ERROR("Failed to open property for attachment: %lx", hResult);
                }
            }

            attach->Release();
        }
        else
        {
            LOG_ERROR("Failed to open attachment: %lx", hResult);
        }
    }
    return attachment;
}

/**
 * Gets a string property for a given entry.
 *
 * @param entry The entry to red the property from.
 * @param propId The property identifier.
 *
 * @return A string representation of the property value retrieved. Must be
 * freed by the caller.
 */
char* MsOutlookAddrBookContactQuery_getStringUnicodeProp
(LPUNKNOWN entry, ULONG propId)
{
    SPropTagArray tagArray;
    tagArray.cValues = 1;
    tagArray.aulPropTag[0] = PROP_TAG(PT_UNICODE, propId);

    ULONG propCount;
    LPSPropValue propArray;
    HRESULT hResult = ((LPMAPIPROP)entry)->GetProps(
        &tagArray,
        0x80000000, // MAPI_UNICODE.
        &propCount,
        &propArray);

    if (HR_SUCCEEDED(hResult))
    {
        unsigned int length = wcslen(propArray->Value.lpszW);
        char * value;
        if ((value = (char*)malloc((length + 1) * sizeof(char)))
            == NULL)
        {
            fprintf(stderr,
                "getStringUnicodeProp (addrbook/MsOutlookAddrBookContactQuery.c): \
                                    \n\tmalloc\n");
            fflush(stderr);
        }
        if (wcstombs(value, propArray->Value.lpszW, length + 1) != length)
        {
            fprintf(stderr,
                "getStringUnicodeProp (addrbook/MsOutlookAddrBookContactQuery.c): \
                                        \n\tmbstowcs\n");
            fflush(stderr);
            MAPIFreeBuffer(propArray);
            ::free(value);
            value = NULL;
            return NULL;
        }
        MAPIFreeBuffer(propArray);
        return value;
    }
    else
    {
        LOG_ERROR("Failed to get string property: %lx", hResult);
    }

    return NULL;
}

/**
 * Compares two identifiers to determine if they are part of the same
 * Outlook contact.
 *
 * @param id1 The first identifier.
 * @param id2 The second identifier.
 *
 * @result True if id1 and id2 are two identifiers of the same contact.  False
 * otherwise.
 */
int MsOutlookAddrBookContactQuery_compareEntryIds(
    LPCTSTR id1,
    LPCTSTR id2)
{
    int result = 0;
    LPMAPISESSION session = MAPISession_getMapiSession();

    LPMAPIPROP mapiId1;
    if ((mapiId1 = (LPMAPIPROP)
        MsOutlookAddrBookContactQuery_openEntryIdStr(
        id1,
        MsOutlookAddrBookContactQuery_rdOpenEntryUlFlags))
        == NULL)
    {
        return result;
    }
    SBinary contactId1;
    contactId1.cb = 0;
    MsOutlookAddrBookContactQuery_getBinaryProp(mapiId1, 0x0FFF, &contactId1);

    LPMAPIPROP mapiId2;
    if ((mapiId2 = (LPMAPIPROP)
        MsOutlookAddrBookContactQuery_openEntryIdStr(
        id2,
        MsOutlookAddrBookContactQuery_rdOpenEntryUlFlags))
        == NULL)
    {
        mapiId1->Release();
        MAPIFreeBuffer(contactId1.lpb);
        return result;
    }
    SBinary contactId2;
    contactId2.cb = 0;
    MsOutlookAddrBookContactQuery_getBinaryProp(mapiId2, 0x0FFF, &contactId2);

    if (session != NULL)
    {
        ULONG res;
        if (session->CompareEntryIDs(
            contactId1.cb,
            (LPENTRYID)contactId1.lpb,
            contactId2.cb,
            (LPENTRYID)contactId2.lpb,
            0,
            &res) != S_OK)
        {
            fprintf(stderr,
                "compareEntryIds (addrbook/MsOutlookAddrBookContactQuery.c): \
                                        \n\tMAPISession::CompareEntryIDs\n");
            fflush(stderr);
            mapiId1->Release();
            MAPIFreeBuffer(contactId1.lpb);
            mapiId2->Release();
            MAPIFreeBuffer(contactId2.lpb);
            return result;
        }
        result = res;
    }

    mapiId1->Release();
    MAPIFreeBuffer(contactId1.lpb);
    mapiId2->Release();
    MAPIFreeBuffer(contactId2.lpb);
    return result;
}
