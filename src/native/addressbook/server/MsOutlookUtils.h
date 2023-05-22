// Copyright (c) Microsoft Corporation. All rights reserved.

#include "../Logger.h"
#include <jni.h>

HRESULT
MsOutlookUtils_getFolderEntryIDByType(LPMDB msgStore,
                                      ULONG folderEntryIDByteCount,
									  LPENTRYID folderEntryID,
									  ULONG *contactsFolderEntryIDByteCount,
									  LPENTRYID *contactsFolderEntryID,
									  ULONG flags,
									  ULONG type);
HRESULT
MsOutlookUtils_HrGetOneProp(LPMAPIPROP mapiProp,
                            ULONG propTag,
							LPSPropValue *prop);

