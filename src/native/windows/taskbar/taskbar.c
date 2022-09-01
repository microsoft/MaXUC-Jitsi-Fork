/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "taskbar.h"

#include <shlobj.h>
#include <shellapi.h>
#include <windows.h>
#include <jni.h>

typedef struct {
    UINT  cbSize;
    HWND  hwnd;
    DWORD dwFlags;
    UINT  uCount;
    DWORD dwTimeout;
} FLASHEXINFO, *PFLASHEXINFO;
	
#define FLASHW_STOP         0
#define FLASHW_CAPTION      0x00000001
#define FLASHW_TRAY         0x00000002
#define FLASHW_ALL          (FLASHW_CAPTION | FLASHW_TRAY)
#define FLASHW_TIMER        0x00000004
#define FLASHW_TIMERNOFG    0x0000000C

const GUID IID_ITaskbarList3 = { 0xea1afb91,0x9e28,0x4b86,{0x90,0xe9,0x9e,0x9f,0x8a,0x5e,0xef,0xaf}};

typedef BOOL (WINAPI *PSFLEX)(PFLASHEXINFO);
	
	
static PSFLEX pFlashWindowEx = NULL; // Pointer to actual function
static BOOL initFlash = FALSE; // Loaded status

HINSTANCE hInst;

BOOL APIENTRY DllMain( HANDLE hModule, DWORD ul_reason_for_call, LPVOID lpvReserverd)
{	 
	switch (ul_reason_for_call) {
		case DLL_PROCESS_ATTACH:
			hInst = (HINSTANCE)hModule;
	}
	return TRUE;
}

ITaskbarList3* getTaskBar()
{
	ITaskbarList3 *taskBar;    

	CoInitializeEx(NULL, COINIT_APARTMENTTHREADED);
	CoCreateInstance(__uuidof(CLSID_TaskbarList), NULL, CLSCTX_ALL, __uuidof(IID_ITaskbarList3), (void**)&taskBar);
	return taskBar;
}

HRESULT
taskbar_iidFromString(JNIEnv *env, jstring str, LPIID iid)
{
    HRESULT hr;

    if (str)
    {
        const jchar *sz = (*env)->GetStringChars(env, str, NULL);

        if (sz)
        {
            hr = IIDFromString((LPOLESTR) sz, iid);
            (*env)->ReleaseStringChars(env, str, sz);
        }
        else
            hr = E_OUTOFMEMORY;
    }
    else
        hr = S_OK;
    return hr;
}

JNIEXPORT jint JNICALL Java_net_java_sip_communicator_service_systray_TaskbarIconOverlay_SetOverlayIcon
	(JNIEnv *env, jclass cls, jint iconid, jstring title)
{
	ITaskbarList3 *taskBar = getTaskBar();

	// To cope with non-ASCII characters, use GetStringChars (not GetStringUTFChars) and FindWindowW (not FindWindow)
	const jchar *str = (*env)->GetStringChars(env, title, 0);
	HWND hwnd = FindWindowW(NULL, str);
	(*env)->ReleaseStringChars(env, title, str);

	if (taskBar != NULL)
	{
		HRESULT hr;
		HICON hIcon = NULL;

		// Load the requested icon
		hIcon = LoadIcon(hInst, MAKEINTRESOURCE(iconid));

		// Set the window's overlay icon
		hr = ITaskbarList3_SetOverlayIcon(taskBar, hwnd, hIcon, NULL);

		if (hIcon) 
		{
			// Need to clean up the icon as we no longer need it
			DestroyIcon(hIcon);
		}

		return hr;
	}
	return -1;
}

BOOL uiFlashWindowEx( HWND hWnd, UINT uCount /* Flash Count */, DWORD dwFlags /* check SDK */ )
{
	if ( ! initFlash )
	{ // Not loaded yet
		HMODULE hDLL = NULL;
		hDLL = LoadLibrary( TEXT( "user32.dll" ) );
		if( hDLL )
		{
			pFlashWindowEx  = ( PSFLEX ) GetProcAddress( hDLL, "FlashWindowEx" );
			initFlash = TRUE; // Loaded OK
		}
	}
	
	if ( pFlashWindowEx == NULL ) 
		return FALSE; // Not Supported
	
	FLASHEXINFO fwi;
	ZeroMemory( & fwi, sizeof( FLASHEXINFO ) );
	fwi.cbSize = sizeof( FLASHEXINFO );
	fwi.dwFlags = dwFlags;
	fwi.hwnd = hWnd;
	fwi.uCount = uCount;
	fwi.dwTimeout = 0L;
	return pFlashWindowEx ( & fwi );
}

JNIEXPORT jint JNICALL Java_net_java_sip_communicator_impl_osdependent_jdic_SystrayServiceJdicImpl_AlertWindow
	(JNIEnv *env, jclass cls, jstring title)
{
	// To cope with non-ASCII characters, use GetStringChars (not GetStringUTFChars) and FindWindowW (not FindWindow)
	const jchar *str = (*env)->GetStringChars(env, title, 0);
	HWND hwnd = FindWindowW(NULL, str);
	(*env)->ReleaseStringChars(env, title, str);

	BOOL res = uiFlashWindowEx(hwnd, 3, FLASHW_TRAY);

	if (res)
	{
		return 0;
	}
	else
	{
		return -1;
	}
}