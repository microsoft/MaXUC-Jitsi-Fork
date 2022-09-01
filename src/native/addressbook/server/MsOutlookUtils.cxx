#include "MAPISession.h"
#include "MsOutlookUtils.h"

#include <jni.h>
#include "Tchar.h"
#include <Mapix.h>

#include "MsOutlookAddrBookContactQuery.h"
#include "MsOutlookAddrBookContactSourceService.h"

HRESULT
MsOutlookUtils_getFolderEntryIDByType
(LPMDB msgStore,
ULONG folderEntryIDByteCount, LPENTRYID folderEntryID,
ULONG *contactsFolderEntryIDByteCount, LPENTRYID *contactsFolderEntryID,
ULONG flags, ULONG type)
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

		hResult
			= MsOutlookUtils_HrGetOneProp(
			(LPMAPIPROP)folder,
			type,
			&prop);
		if (HR_SUCCEEDED(hResult))
		{
			LPSBinary bin = &(prop->Value.bin);
			if (S_OK
				== MAPIAllocateBuffer(
				bin->cb,
				(void **)contactsFolderEntryID))
			{
				hResult = S_OK;
				*contactsFolderEntryIDByteCount = bin->cb;
				CopyMemory(*contactsFolderEntryID, bin->lpb, bin->cb);
			}
			else
			{
				LOG_ERROR("MsOutlookUtils_getFolderEntryIDByType: Not enough memory.");
				hResult = MAPI_E_NOT_ENOUGH_MEMORY;
			}
			MAPIFreeBuffer(prop);
		}
		else
		{
			LOG_ERROR("MsOutlookUtils_getFolderEntryIDByType: Error getting the property.");
		}
		folder->Release();
	}
	else
	{
		LOG_ERROR("MsOutlookUtils_getFolderEntryIDByType: Error opening the folder.");
	}
	return hResult;
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
HRESULT
MsOutlookUtils_HrGetOneProp(
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
		{
			LOG_ERROR("MsOutlookUtils_HrGetOneProp: Property not found.");
			hResult = MAPI_E_NOT_FOUND;
		}
		MAPIFreeBuffer(values);
	}
	else
	{
		LOG_ERROR("MsOutlookUtils_HrGetOneProp: MAPI getProps error.");
	}
	return hResult;
}
