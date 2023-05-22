// Copyright (c) Microsoft Corporation. All rights reserved.

#include <sys/types.h>
#include <mapix.h>
#include <jansson.h>
#include <windows.h>
#include <aclapi.h>

#include "../StringUtils.h"
#include "../Logger.h"
#include "../ProductName.h"

#include "MAPISession.h"

#include "RpcServer.h"
#include "RpcClient.h"
#include "Server.h"
#include "MsOutlookAddrBookContactQuery.h"
#include "CrashReporter.h"

#define BUFFER_SIZE 16384
#define MAX_PIPE_NAME_LENGTH 256

HANDLE create_named_pipe(LPTSTR pipeName);
LPSTR read_request(HANDLE pipeHandle, LPSTR buffer, DWORD bufferSize);
bool write_response(LPSTR response);

DWORD WINAPI server_thread(LPVOID);
LPSTR server_process_request(LPSTR);

void server_contact_query(json_t* input, json_t* output);
bool server_contact_query_cb(LPSTR contact, void* cb);
bool server_calendar_query_cb(LPSTR calendar);

void server_contact_add(json_t* input, json_t* output);
void server_contact_delete(json_t* input, json_t* output);

void server_calendar_get(json_t* input, json_t* output);

void server_get_default_contact_folder_id(json_t* input, json_t* output);
void server_get_default_calendar_folder_id(json_t* input, json_t* output);

void server_props_get(json_t* input, json_t* output);
void server_props_set(json_t* input, json_t* output);

void server_id_compare(json_t* input, json_t* output);

void server_write_dump(json_t* input, json_t* output);

void server_quit(json_t* input, json_t* output);

typedef void (*server_handler)(json_t* input, json_t* output);

TCHAR pipeName[MAX_PIPE_NAME_LENGTH];
HANDLE pipeHandle = INVALID_HANDLE_VALUE;
HANDLE serverThreadHandle = NULL;
volatile bool stopServer = false;

/**
 * Start the RPC server on a named pipe.
 * @return true if server started successfully
 */
bool start_server()
{
  DWORD threadId;
  DWORD pid;

  pid = GetCurrentProcessId();
  _sntprintf(pipeName, MAX_PIPE_NAME_LENGTH, _T("\\\\.\\pipe\\%sOutlookServer.%lu"), _T(PRODUCT_NAME), pid);
  LOG_INFO("Trying to create a named pipe %s", pipeName);

  pipeHandle = create_named_pipe(pipeName);
  if (pipeHandle == INVALID_HANDLE_VALUE) 
  {
      LOG_ERROR("CreateNamedPipe failed, GLE=%d.", GetLastError());
      return false;
  }
  LOG_INFO("Created the named pipe %s", pipeName);

  stopServer = false;

  // Create a thread for this client
  serverThreadHandle = CreateThread(
          NULL,                // no security attribute -> handle cannot be inherited
          0,                   // default stack size 
          server_thread,       // thread proc
          (LPVOID) pipeHandle, // thread parameter - named pipe handle
          0,                   // not suspended 
          &threadId);          // returns thread ID

  if (serverThreadHandle == NULL) 
  {
    LOG_ERROR("CreateThread failed, GLE=%d.", GetLastError());
    CloseHandle(pipeHandle);
    return false;
  }
   
  LOG_INFO("Created a processing thread, PID=%d", threadId); 

  return true;
}

/**
 * Stop the RPC server.
 */
void stop_server()
{
  LOG_INFO("Stopping server daemon");
  CloseHandle(serverThreadHandle); 
  CloseHandle(pipeHandle);
  LOG_INFO("Stopped server");
}

/**
 * Get the pipe name the server is listening on.
 * @return a pipe name
 */
char* server_pipe_name()
{
  return (char*) pipeName;
}

void server_props_get(json_t* input, json_t* output)
{
  json_t* flags_j = json_object_get(input, "flags");
  json_t* properties_j = json_object_get(input, "properties");
  json_t* entry_j = json_object_get(input, "entryId");
  json_t* type_j = json_object_get(input, "type");

  if (entry_j && properties_j && flags_j &&
      json_is_array(properties_j) &&
      json_is_string(entry_j) &&
      json_is_integer(flags_j))
  {
    const char* entry_s = json_string_value(entry_j);
    unsigned int propIdCount = json_array_size(properties_j);
    long flags = json_integer_value(flags_j);
    long* propIds = (long*) malloc(sizeof(long) * propIdCount);
    void** props = (void**) malloc(sizeof(void*) * propIdCount);
    unsigned long* propsLength = (unsigned long*) malloc(sizeof(unsigned long) * propIdCount);
    char* propsType = (char*) malloc(sizeof(char) * propIdCount);

    int guidType = GUID_TYPE_ADDRESS;

    if (type_j)
    {
        guidType = json_integer_value(type_j);
    }

    if (propIds != 0 && props != 0 && entry_s != 0 && propsLength != 0 && propsType != 0)
    {
      memset(props, 0, sizeof(void*) * propIdCount);

      {
        size_t index;
        json_t* value;

        json_array_foreach(properties_j, index, value)
        {
          if (json_is_integer(value))
          {
            propIds[index] = json_integer_value(value);
          }
          else
          {
            LOG_ERROR("None integer flag dropped");
            propIds[index] = 0;
          }
        }
      }

      HRESULT hr = MsOutlookAddrBookContactQuery_IMAPIProp_1GetProps(
              entry_s, propIdCount, propIds, flags,
              props, propsLength, propsType, guidType);

      if(HR_SUCCEEDED(hr))
      {
        LOG_DEBUG("Retrieved %d properties for %s with result %d", propIdCount, entry_s, hr);

        json_t* props_r = json_array();

        for(size_t i = 0; i < propIdCount; ++i)
        {
          LOG_TRACE("%d - %c - %x - %d", i, propsType[i], props[i], propsLength[i]);

          if (propsType[i] == 'u' && props[i] != NULL)
          {
            if (propsLength[i] > 0)
            {
              // Defined as (wcslen(str) + 1) * 2 in
              // MsOutlookAddrBookContactQuery_IMAPIProp_1GetProps
              unsigned long wlen = (propsLength[i] / 2) - 1;

              if (wlen > 0)
              {
                int len = WideCharToMultiByte(CP_UTF8, 0, (LPCWCH) props[i], wlen, NULL, 0, 0, 0);

                if (len > 0)
                {
                  char* buffer = (char*) malloc(sizeof(char) * (len + 1));

                  if (buffer)
                  {
                    buffer[len] = '\0';

                    int transferred =
                      WideCharToMultiByte(CP_UTF8, 0, (LPCWCH) props[i], wlen, buffer, len, 0, 0);

                    if (transferred == len)
                    {
                      json_t*  buffer_j = json_string(buffer);

                      if (buffer_j)
                      {
                        json_array_append_new(props_r, buffer_j);
                      }
                      else
                      {
                        LOG_ERROR("Failed to create JSON string for: %s", buffer);
                      }
                    }
                    else if (transferred != 0)
                    {
                      LOG_ERROR("Multibyte conversion failed after %d/%d characters (%d/%d)",
                        transferred, len, propsLength[i], wlen);
                      json_array_append_new(props_r, json_null());
                    }
                    else
                    {
                      LOG_ERROR("Multibyte conversion failed with %d characters (%d/%d)",
                        len, propsLength[i], wlen);
                      json_array_append_new(props_r, json_null());
                    }

                    free(buffer);
                  }
                  else
                  {
                    LOG_ERROR("Failed to allocate buffer of size: %u", len + 1);
                    json_array_append_new(props_r, json_null());
                  }
                }
                else
                {
                  LOG_WARN("Multibyte conversion failed: %d/%d", propsLength[i], wlen);
                  json_array_append_new(props_r, json_null());
                }
              }
              else if (wlen == 0)
              {
                json_array_append_new(props_r, json_string(""));
              }
              else
              {
                LOG_WARN("Unexpected string conversion size: %d/%d", propsLength[i]);
                json_array_append_new(props_r, json_null());
              }
            }
            else
            {
              json_array_append_new(props_r, json_string(""));
            }
          }
          else if (propsType[i] == 's' && props[i] != NULL)
          {
            json_t* string_j = json_string((const char*) props[i]);

            if (string_j == NULL)
            {
              LOG_WARN("String %s failed to convert to JSON", props[i]);
              json_array_append_new(props_r, json_null());
            }
            else
            {
              json_array_append_new(props_r, string_j);
            }
          }
          else if (propsType[i] == 'l' && props[i] != NULL)
          {
            long long val = 0;
            if (propsLength[i] > sizeof(long long))
            {
              LOG_WARN("Integer too large %ld v %ld for %d",
                propsLength[i], sizeof(long long), i);
              json_array_append_new(props_r, json_null());
            }
            else
            {
              memcpy(&val, props[i], propsLength[i]);

              json_t* intval = json_integer(val);

              if (intval != NULL)
              {
                json_array_append_new(props_r, intval);
              }
              else
              {
                LOG_WARN("Invalid integer %ld", val);
                json_array_append_new(props_r, json_null());
              }
            }
          }
          else if (propsType[i] == 'd' && props[i] != NULL)
          {
              double val = 0;
              if (propsLength[i] > sizeof(double))
              {
                  LOG_WARN("Integer too large %ld v %ld for %d",
                      propsLength[i], sizeof(double), i);
                  json_array_append_new(props_r, json_null());
              }
              else
              {
                  memcpy(&val, props[i], propsLength[i]);

                  json_t* realval = json_real(val);

                  if (realval != NULL)
                  {
                      json_array_append_new(props_r, realval);
                  }
                  else
                  {
                      LOG_WARN("Invalid integer %ld", val);
                      json_array_append_new(props_r, json_null());
                  }
              }
          }
          else if (propsType[i] == 't' && props[i] != NULL)
          {
              if (propsLength[i] > sizeof(SYSTEMTIME))
              {
                  LOG_WARN("System time too large %ld v %ld for %d",
                           propsLength[i],
                           sizeof(SYSTEMTIME),
                           i);

                  json_array_append_new(props_r, json_null());
              }
              else
              {
                  char dateTime[20];
                  LPSYSTEMTIME sysTime = (LPSYSTEMTIME)props[i];
                  sprintf(dateTime,
                      "%u-%02u-%02u %02u:%02u:%02u",
                      sysTime->wYear, sysTime->wMonth,
                      sysTime->wDay, sysTime->wHour,
                      sysTime->wMinute, sysTime->wSecond);
                  json_array_append_new(props_r, json_string(dateTime));
              }
          }
          else
          {
            // Treat all of the following cases as a null value.

            // Have we missed a property type?
            if (propsType[i] != '\0' && props[i] != NULL)
            {
              LOG_ERROR("Unimplemented property type: %d - %c - %x - %d",
                i, propsType[i], props[i], propsLength[i]);
            }
            // Did a property result in a null value?
            else if (propsType[i] != '\0' && props[i] == NULL)
            {
              LOG_WARN("Missing property: %d - %c - NULL - %d",
                i, propsType[i], propsLength[i]);
            }
            // Does the property not have a value?
            else if (propsType[i] == '\0')
            {
              LOG_TRACE("Null property: %d", i);
            }

            json_array_append_new(props_r, json_null());
          }
        }

        LOG_DEBUG("Returned %d properties for %s with result %d", propIdCount, entry_s, hr);

        json_object_set_new(output, "props", props_r);
        json_object_set_new(output, "result", json_string("success"));
      }
      else
      {
        json_object_set_new(output, "result", json_string("error"));
        json_object_set_new(output, "reason", json_string("failed to query property"));
        json_object_set_new(output, "code", json_integer(hr));
      }
    }
    else
    {
      json_object_set_new(output, "result", json_string("error"));
      json_object_set_new(output, "reason", json_string("failed to alloc data"));
    }

    if (props != NULL)
    {
      for (unsigned int ix = 0; ix < propIdCount; ix++)
      {
          free(props[ix]);
          props[ix] = NULL;
      }
      free(props);
    }
    free(propIds);
    free(propsLength);
    free(propsType);
  }
}

void server_props_set(json_t* input, json_t* output)
{
  json_t* prop_j = json_object_get(input, "propId");
  json_t* entry_j = json_object_get(input, "entryId");
  json_t* value_j = json_object_get(input, "value");

  if (entry_j && prop_j && value_j &&
      json_is_integer(prop_j) &&
      json_is_string(entry_j) &&
      json_is_string(value_j))
  {
    int wchars_num = MultiByteToWideChar(CP_UTF8, 0, json_string_value(value_j), -1, NULL, 0);
    LPWSTR value_w = (LPWSTR) malloc(sizeof(wchar_t) * wchars_num);
    MultiByteToWideChar(CP_UTF8, 0, json_string_value(value_j), -1, value_w, wchars_num);

    const char* entry_s = json_string_value(entry_j);

    int result = MsOutlookAddrBookContactQuery_IMAPIProp_1SetPropString(
                    json_integer_value(prop_j),
                    value_w,
                    entry_s);

    if(result == 1)
    {
      json_object_set_new(output, "result", json_string("success"));
    }
    else
    {
      json_object_set_new(output, "result", json_string("error"));
      json_object_set_new(output, "reason", json_string("failed to set property"));
    }

    free(value_w);
  }
}

void server_quit(json_t* input, json_t* output)
{
  stop();
  json_object_set_new(output, "result", json_string("success"));
}

void server_calendar_get(json_t* input, json_t* output)
{
    LOG_DEBUG("Requesting all calendar data");
    MsOutlookAddrBookContactQuery_foreachMailUser(NULL,
                                                  (void *)server_calendar_query_cb,
                                                  NULL,
                                                  FOLDER_TYPE_CALENDAR);
    LOG_DEBUG("Request complete!");

    json_object_set_new(output, "result", json_string("success"));
}

bool server_calendar_query_cb(LPSTR entryId)
{
    LOG_DEBUG("Calendar query callback '%s'", entryId);

    // Just return the id immediately
    json_t* output = json_object();

    // Include the id in the output
    json_object_set_new(output, "calendarId", json_string(entryId));
    json_object_set_new(output, "result", json_string("success"));
    json_object_set_new(output, "type", client_get_type(entryId));
    client_calendar_inserted(output);

    json_decref(output);

    return true;
}

void server_id_compare(json_t* input, json_t* output)
{
    LOG_DEBUG("Compare IDs");

    // We get passed the ID to compare and an array containing all the
    // other IDs to compare against.
    json_t* id_j        = json_object_get(input, "id");
    json_t* other_ids_j = json_object_get(input, "otherIds");
    int match_index = -1;

    if (id_j && other_ids_j)
    {
        const char* id = json_string_value(id_j);
        unsigned int id_count = json_array_size(other_ids_j);
        json_t* other_id_j;

        MAPISession_lock();

        // Compare against all each of the other IDs in turn
        for (unsigned int index = 0;
            index < id_count && (other_id_j = json_array_get(other_ids_j, index));
            index++)
        {
            const char* other_id = json_string_value(other_id_j);
            int result = MsOutlookAddrBookContactQuery_compareEntryIds(id, other_id);

            if (result)
            {
                match_index = index;
                break;
            }
        }

        MAPISession_unlock();
    }

    json_object_set_new(output, "match", json_integer(match_index));
    json_object_set_new(output, "result", json_string("success"));
}

void server_write_dump(json_t* input, json_t* output)
{
    bool success = write_minidump();
    
    if (success)
    {
        json_object_set_new(output, "result", json_string("success"));
    }
    else
    {
        json_object_set_new(output, "result", json_string("error"));
        json_object_set_new(output, "reason", json_string("minidump failed"));
    }
}

void server_contact_add(json_t* input, json_t* output)
{
  char* contact_s = MsOutlookAddrBookContactQuery_createContact();

  if (contact_s != NULL)
  {
    json_t* contact_j = json_string(contact_s);

    if (contact_j)
    {
      json_object_set_new(output, "result", json_string("success"));
      json_object_set_new(output, "id", contact_j);
    }
    else
    {
      json_object_set_new(output, "result", json_string("error"));
      json_object_set_new(output, "reason", json_string("failed to allocate string"));
    }

    free(contact_s);
  }
  else
  {
    json_object_set_new(output, "result", json_string("error"));
    json_object_set_new(output, "reason", json_string("failed to create contact"));
  }
}

void server_get_default_contact_folder_id(json_t* input, json_t* output)
{
  char* folder_id_s = MsOutlookAddrBookContactQuery_getDefaultFolderEntryId(FOLDER_TYPE_CONTACTS);

  if (folder_id_s != NULL)
  {
    json_t* folder_id_j = json_string(folder_id_s);

    if (folder_id_j)
    {
      json_object_set_new(output, "result", json_string("success"));
      json_object_set_new(output, "id", folder_id_j);
    }
    else
    {
      json_object_set_new(output, "result", json_string("error"));
      json_object_set_new(output, "reason", json_string("failed to allocate string"));
    }

    free(folder_id_s);
  }
  else
  {
    json_object_set_new(output, "result", json_string("error"));
    json_object_set_new(output, "reason", json_string("failed to get default contact folder id"));
  }
}

void server_contact_delete(json_t* input, json_t* output)
{
  json_t* contact_j = json_object_get(input, "id");

  if (contact_j && json_is_string(contact_j))
  {
    const char* contact_s = json_string_value(contact_j);

    int result = MsOutlookAddrBookContactQuery_deleteContact(contact_s);

    if(result)
    {
      json_object_set_new(output, "result", json_string("success"));
    }
    else
    {
      json_object_set_new(output, "result", json_string("error"));
      json_object_set_new(output, "reason", json_string("failed to delete contact"));
    }
  }
}

void server_contact_query(json_t* input, json_t* output)
{
  json_t* query = json_object_get(input, "query");

  if (query)
  {
    const char* query_s = json_string_value(query);

    if (query_s)
    {
      MsOutlookAddrBookContactQuery_foreachMailUser(
                query_s,
                (void *) server_contact_query_cb,
                NULL,
                FOLDER_TYPE_CONTACTS);
      json_object_set_new(output, "result", json_string("success"));
    }
    else
    {
      json_object_set_new(output, "result", json_string("error"));
      json_object_set_new(output, "reason", json_string("query provided wasn't a string"));
    }
  }
  else
  {
    json_object_set_new(output, "result", json_string("error"));
    json_object_set_new(output, "reason", json_string("no query provided"));
  }
}

bool server_contact_query_cb(LPSTR contact, void* cb)
{
  return client_contact_inserted(contact);
}

void server_get_default_calendar_folder_id(json_t* input, json_t* output)
{
  char* folder_id_s = MsOutlookAddrBookContactQuery_getDefaultFolderEntryId(FOLDER_TYPE_CALENDAR);

  if (folder_id_s != NULL)
  {
    json_t* folder_id_j = json_string(folder_id_s);

    if (folder_id_j)
    {
      json_object_set_new(output, "result", json_string("success"));
      json_object_set_new(output, "id", folder_id_j);
    }
    else
    {
      json_object_set_new(output, "result", json_string("error"));
      json_object_set_new(output, "reason", json_string("failed to allocate string"));
    }

    free(folder_id_s);
  }
  else
  {
    json_object_set_new(output, "result", json_string("error"));
    json_object_set_new(output, "reason", json_string("failed to get default contact folder id"));
  }
}

/**
 * Process a request in JSON format and produce a response in JSON.
 * Every request has a command and input data for the command.
 * @param parameter request as JSON string
 * @return response as JSON string
 */
LPSTR server_process_request(LPSTR request)
{
  server_handler handler = NULL;

  json_t* input;
  json_error_t error;
  json_t* output = json_object();

  LOG_DEBUG("Input: %s", request);

  input = json_loads(request, 0, &error);
  if (input)
  {
    const char* url = json_string_value(json_object_get(input, "command"));
    LOG_INFO("Pipe request for: %s with %d bytes", url, strlen(request));

    json_t* data = json_object_get(input, "data");

    if (0 == strcmp (url, "/contact/query"))
    {
      handler = &server_contact_query;
    }
    else if (0 == strcmp (url, "/contact/add"))
    {
      handler = &server_contact_add;
    }
    else if (0 == strcmp (url, "/contact/delete"))
    {
      handler = &server_contact_delete;
    }
    else if (0 == strcmp (url, "/quit"))
    {
      handler = &server_quit;
      // Set flag to exit from server thread loop
      stopServer = true;
    }
    else if (0 == strcmp (url, "/props/get"))
    {
      handler = &server_props_get;
    }
    else if (0 == strcmp (url, "/props/set"))
    {
      handler = &server_props_set;
    }
    else if (0 == strcmp(url, "/getdefaultcontactsfolder"))
    {
      handler = &server_get_default_contact_folder_id;
    }
    else if (0 == strcmp(url, "/getdefaultcalendarfolder"))
    {
      handler = &server_get_default_calendar_folder_id;
    }
    else if (0 == strcmp(url, "/calendar/query"))
    {
      handler = &server_calendar_get;
    }
    else if (0 == strcmp(url, "/compareids"))
    {
      handler = &server_id_compare;
    }
    else if (0 == strcmp(url, "/dump"))
    {
      handler = &server_write_dump;
    }

    if (handler != NULL)
    {
      // RPC command is valid
      (*handler)(data, output);
    }
    else
    {
      LOG_ERROR("Unexpected URL: %s", url);

      json_object_set_new(output, "result", json_string("error"));
      json_object_set_new(output, "reason", json_string("unknown url"));
    }

    json_decref(input);
  }
  else
  {
    LOG_ERROR("JSON decoding error on line %d - %s in: %s", error.line, error.text, request);

    json_object_set_new(output, "result", json_string("error"));
    json_object_set_new(output, "reason", json_string("json decoding error"));
    json_object_set_new(output, "message", json_string(error.text));
    json_object_set_new(output, "line", json_integer(error.line));
    json_object_set_new(output, "input", json_string((LPSTR)request));
  }

  LPSTR page = json_dumps(output, 0);
  json_decref(output);

  LOG_DEBUG("Output: %s", page);

  return page;
}

/**
 * This routine is a thread processing function to read from and reply to a client
 * via the open pipe connection passed from the main function.
 * @param param passed from CreateThread to the thread func, the actual value is a named pipe handle
 * @return thread exit code
 */
DWORD WINAPI server_thread(LPVOID param)
{ 
  LPSTR buffer = NULL;
  bool pipeConnected = false;
  HANDLE pipeHandle  = NULL;

  // Check param
  if (param == NULL)
  {
    LOG_ERROR("Unexpected NULL value in param.");
    stop(); // Set flag to stop the parent thread
    return (DWORD)-1;
  }

  // The thread's parameter is a handle to a pipe object instance
  pipeHandle = (HANDLE) param;

  buffer = (LPSTR) malloc(BUFFER_SIZE * sizeof(char));
  if (buffer == NULL)
  {
    LOG_ERROR("Unexpected NULL memory allocation.");
    CloseHandle(pipeHandle);
    stop(); // Set flag to stop the parent thread
    return (DWORD)-1;
  }

  // Wait for a connection on pipeHandle, not overlapped. ERROR_PIPE_CONNECTED returned if client already connected.
  LOG_INFO("Waiting for a client to connect.");
  pipeConnected = ConnectNamedPipe(pipeHandle, NULL) ? true : (GetLastError() == ERROR_PIPE_CONNECTED);
  if (!pipeConnected)
  {
    LOG_ERROR("Client failed to connect, GLE=%d.", GetLastError());
    CloseHandle(pipeHandle);
    free(buffer);
    stop();
    return (DWORD)-1;
  }
  
  if (GetLastError() == ERROR_PIPE_CONNECTED) 
  {
    LOG_INFO("Client already connected.");
  }
  else
  {
    LOG_INFO("Client connected.");
  }

  // Loop until stopped
  LOG_INFO("Starting to receive and process messages.");
  while (!stopServer) 
  {
    // Wait and read a request from the pipe
    LPSTR request = read_request(pipeHandle, buffer, BUFFER_SIZE);
    if (request == NULL) {
      break;
    }

    // Process the incoming message
    LPSTR response = server_process_request(request);

    free(request);
    if (response == NULL) {
      break;
    }

    // Write the response back to the pipe
    bool writeSuccess = write_response(response);

    free(response);
    if (!writeSuccess) {
      break;
    }

    LOG_DEBUG("Finished response");
  }

  LOG_INFO("Disconnecting pipe.");

  // Flush the pipe to allow the client to read the pipe's contents
  FlushFileBuffers(pipeHandle); 

  // Disconnect the pipe, and close the handle to this pipe instance
  DisconnectNamedPipe(pipeHandle); 
  CloseHandle(pipeHandle); 

  free(buffer);

  LOG_INFO("Pipe server thread exiting.");

  // Set flag to stop the parent thread
  stop();

  return 1;
}

/**
 * Creates a named pipe with the provided name. 
 * Applied a security descriptor which grants an access to the pipe  only to the same user who ran the server process.
 * @return handle of the created named pipe
 */
HANDLE create_named_pipe(LPTSTR pipeName) {
  SECURITY_ATTRIBUTES sa;
  SECURITY_DESCRIPTOR sd;
  PSID sidOwner = NULL;
  PACL acl = NULL;

  // Retrieve current process owner
  if (GetSecurityInfo(
                  GetCurrentProcess(),        // handle of the current process
                  SE_KERNEL_OBJECT,           // process is a kernel object
                  OWNER_SECURITY_INFORMATION, // intereseted in owner information
                  &sidOwner,                  // output for owner
                  NULL,                       // not interested in group
                  NULL,                       // not interested in DACL
                  NULL,                       // not interested in SACL
                  NULL                        // not interested in security descriptor
                  ) != ERROR_SUCCESS)
  {
      LOG_ERROR("GetSecurityInfo failed with error %d", GetLastError());
      return INVALID_HANDLE_VALUE;
  }

  // Set up ACE
  EXPLICIT_ACCESS ace      = {0};
  ace.grfAccessMode        = SET_ACCESS;
  ace.grfAccessPermissions = FILE_GENERIC_READ | FILE_GENERIC_WRITE; // client must be able to read and write from the pipe
  ace.grfInheritance       = NO_INHERITANCE;
  ace.Trustee.TrusteeForm  = TRUSTEE_IS_SID;  // Trustee is an owner user
  ace.Trustee.TrusteeType  = TRUSTEE_IS_USER;
  ace.Trustee.ptstrName    = (LPTSTR) sidOwner;

  // Set the ACE entries in ACL
  if (SetEntriesInAcl(1, &ace, NULL, &acl) != ERROR_SUCCESS) {
      LOG_ERROR("SetEntriesInAcl failed with error %d", GetLastError());
      return INVALID_HANDLE_VALUE;
  }

  // Initialise SECURITY_DESCRIPTOR
  if (InitializeSecurityDescriptor(&sd, SECURITY_DESCRIPTOR_REVISION) == 0)
  {
      LOG_ERROR("InitializeSecurityDescriptor failed with error %d", GetLastError());
      return INVALID_HANDLE_VALUE;
  }

  // Set the DACL field in the SECURITY_DESCRIPTOR object to NULL
  if (SetSecurityDescriptorDacl(&sd, TRUE, acl, FALSE) == 0)
  {
      LOG_ERROR("SetSecurityDescriptorDacl failed with error %d", GetLastError());
      return INVALID_HANDLE_VALUE;
  }

  // Assign the new SECURITY_DESCRIPTOR object to the SECURITY_ATTRIBUTES object
  sa.nLength = sizeof(SECURITY_ATTRIBUTES);
  sa.lpSecurityDescriptor = &sd;
  sa.bInheritHandle = FALSE;

  HANDLE pipeHandle = CreateNamedPipe(
          pipeName,                      // pipe name
          PIPE_ACCESS_DUPLEX |           // read/write access
          FILE_FLAG_FIRST_PIPE_INSTANCE, // must be the first pipe instance (no existing pipe instance expected, otherwise fail)
          PIPE_TYPE_MESSAGE |            // message type pipe
          PIPE_READMODE_MESSAGE |        // message-read mode
          PIPE_WAIT |                    // blocking mode
          PIPE_REJECT_REMOTE_CLIENTS,    // allow only local connections
          1,                             // only one instance allowed (no one can create another instance)
          BUFFER_SIZE,                   // output buffer size
          BUFFER_SIZE,                   // input buffer size
          0,                             // client time-out (not used)
          &sa);                          // security attributes

  return pipeHandle;
}

/**
 * @brief Reads a message from the named pipe using the provided buffer.
 * @param pipeHandle a handle of the opened named pipe to read from
 * @param buffer allocated buffer
 * @param bufferSize size of the buffer
 * @return request string
 */
LPSTR read_request(HANDLE pipeHandle, LPSTR buffer, DWORD bufferSize) {
  LPSTR request = NULL;
  DWORD receivedBytes = 0;
  DWORD bytesRead = 0;
  bool fileOperationSuccess;

  do {
    // Read client requests from the pipe. 
    // If total message size is greater than bufferSize, several reads will be needed,
    // intermediate reads will return false with ERROR_MORE_DATA error.
    fileOperationSuccess = ReadFile( 
        pipeHandle,   // handle to pipe 
        buffer,       // buffer to receive data
        bufferSize,   // size of buffer
        &bytesRead,   // number of bytes read
        NULL);        // not overlapped I/O 

    // ERROR_MORE_DATA indicates there is more data to read, so it's not considered as "error".
    if (!fileOperationSuccess && GetLastError() != ERROR_MORE_DATA)
    {   
      if (GetLastError() == ERROR_BROKEN_PIPE)
      {
        LOG_WARN("Client disconnected.");
      }
      else
      {
        LOG_ERROR("ReadFile failed, GLE=%d.", GetLastError());
      }

      return NULL;
    }

    if (request)
    {
      // For subsequent reads, resize the destination buffer
      request = (LPSTR) realloc(request, receivedBytes + bytesRead + 1);
    }
    else
    {
      // For first read, create the destination buffer
      request = (LPSTR) malloc(bytesRead + 1);
    }

    if (request == NULL)
    {
      LOG_ERROR("Not enough memory (alloc returned NULL)");
      return NULL;
    }
    else
    {
      // Copy from read buffer to the destination buffer
      memcpy(&(request[receivedBytes]), buffer, bytesRead);
      receivedBytes += bytesRead;
      request[receivedBytes] = 0;
    }
  } while (!fileOperationSuccess);

  return request;
}

/**
 * Writes a message to the named pipe.
 * @param response string to write
 * @return true if write was successful
 */
bool write_response(LPSTR response) {
  DWORD bytesWritten = 0; 
  DWORD responseBytes = strlen(response);

  // Write the reply to the pipe
  bool fileOperationSuccess = WriteFile( 
        pipeHandle,      // handle to pipe
        response,        // buffer to write from
        responseBytes,   // number of bytes to write
        &bytesWritten,   // number of bytes written
        NULL);           // not overlapped I/O

  if (!fileOperationSuccess || responseBytes != bytesWritten)
  {   
      LOG_ERROR("WriteFile failed, GLE=%d.", GetLastError());
      return false;
  }

  return true;
}
