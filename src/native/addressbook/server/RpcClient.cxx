/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include <curl/curl.h>
#include <jansson.h>
#include <mapitags.h>
#include <stdlib.h>

#include "../Logger.h"

#include "MapiClient.h"
#include "MapiCode.h"
#include "MsOutlookAddrBookContactQuery.h"
#include "RpcClient.h"
#include "RpcServer.h"

struct data {
  char *memory;
  size_t ptr;
  size_t size;
};

struct request {
  struct data input;
  struct data output;
};

struct request* client_request_create();
bool client_request(const char* url, struct request* req);
void client_request_cleanup(struct request* req);

size_t client_write(void *ptr, size_t size, size_t nmemb, void *userp);
size_t client_read(void *contents, size_t size, size_t nmemb, void *userp);

char serverAddress[50];

/**
 * Start the RPC client
 */
void start_client(int port)
{
  curl_global_init(CURL_GLOBAL_ALL);

  snprintf(serverAddress, 50, "http://127.0.0.1:%d", port);

  LOG_INFO("Started client with address: %s", serverAddress);
}

/**
 * Stop the RPC client
 */
void stop_client()
{
  curl_global_cleanup();
}

bool client_contact_operation(LPSTR id, const char* operation)
{
  bool ret;
  char url[100];
  snprintf(url, 100, "%s/contact/%s", serverAddress, operation);

  struct request* req;
  req = client_request_create();

  json_t* output = json_object();
  json_t* type = client_get_type(id);
  json_t* contact = json_string(id);

  json_object_set_new(output, "contact", contact);
  json_object_set_new(output, "type", type);
  req->input.memory = json_dumps(output, 0);

  LOG_INFO("Contact %s: %s", operation, req->input.memory);

  ret = client_request(url, req);

  client_request_cleanup(req);
  json_decref(output);

  return ret;
}

bool client_calendar_operation(json_t * data, const char* operation)
{
    bool ret;
    char url[100];
    snprintf(url, 100, "%s/calendar/%s", serverAddress, operation);

    struct request* req;
    req = client_request_create();
    req->input.memory = json_dumps(data, 0);

    LOG_INFO("Calendar %s: %s", operation, req->input.memory);
    ret = client_request(url, req);
    client_request_cleanup(req);

    return ret;
}

json_t* client_get_type(LPSTR id)
{
    json_t* type;

    if (id != NULL)
    {
        // Only getting 1 property - the type.
        long propIds[] = { PROP_ID(PR_MESSAGE_CLASS) };
        long flags = 0;
        void* props[1];
        unsigned long propsLength[1];
        char propsType[1];
        int guidType = GUID_TYPE_ADDRESS;

        memset(props, 0, sizeof(void*));

        HRESULT hr = MsOutlookAddrBookContactQuery_IMAPIProp_1GetProps(
            id, 
            1, 
            propIds, 
            flags,
            props, 
            propsLength, 
            propsType, 
            guidType);

        if (HR_SUCCEEDED(hr))
        {
            type = json_string((LPCSTR)props[0]);
            free(props[0]);
        }
        else
        {
            LOG_ERROR("Error 0x%x getting props for ID %s", hr, id);
            type = json_null();
        }
    }
    else
    {
        LOG_ERROR("Asked to get props for null ID");
        type = json_null();
    }

    return type;
}

bool client_contact_deleted(LPSTR id)
{
    return client_contact_operation(id, "delete");
}

bool client_contact_inserted(LPSTR id)
{
    return client_contact_operation(id, "add");
}

bool client_contact_updated(LPSTR id)
{
    return client_contact_operation(id, "update");
}

bool client_calendar_inserted(json_t * output)
{
    return client_calendar_operation(output, "insert");
}

void client_mapi_status()
{
  char url[100];
  snprintf(url, 100, "%s/mapi/status", serverAddress);

  struct request* req;

  req = client_request_create();
  req->input.memory = (char*) malloc(sizeof(char) * 20);
  snprintf(req->input.memory, 20, "{\"status\":%ld}", mapi_status());

  LOG_INFO("MAPI Status: %s", req->input.memory);

  client_request(url, req);

  client_request_cleanup(req);
}

void client_start()
{
  const size_t maxRequestBodySize = 1024U;

  char url[100];
  snprintf(url, 100, "%s/client/start", serverAddress);

  struct request* req;

  req = client_request_create();
  req->input.memory = (char*) malloc(sizeof(char) * maxRequestBodySize);

  json_t* input = json_object();
  json_object_set_new(input, "pipeName", json_string(server_pipe_name()));

  snprintf(req->input.memory, maxRequestBodySize, json_dumps(input, 0));

  LOG_INFO("Start: %s", req->input.memory);

  client_request(url, req);

  client_request_cleanup(req);
}

struct request* client_request_create()
{
  LOG_TRACE("Creating request");

  struct request* req = (struct request*) malloc(sizeof(struct request));

  req->input.memory = (char*) malloc(sizeof(char));
  req->input.memory[0] = '\0';
  req->input.size = 0;
  req->input.ptr = 0;

  req->output.memory = 0;
  req->output.size = 0;
  req->output.ptr = 0;

  return req;
}

void client_request_cleanup(struct request* req)
{
  LOG_TRACE("Cleaning up request");

  if (req->input.memory)
  {
      free(req->input.memory);
  }

  if (req->output.memory)
  {
      free(req->output.memory);
  }

  free(req);
}

int client_logger(CURL * curl, curl_infotype it, char* message, size_t len, void* userp)
{
  // Remove whitespace
  while((len > 0) &&
        ((message[len - 1] == '\n') ||
         (message[len - 1] == '\r') ||
         (message[len - 1] == '\t') ||
         (message[len - 1] == ' ')))
  {
    len --;
  }

  if (len)
  {
    char* data = (char*) malloc(sizeof(char) * (len + 1));
    if (data)
    {
      memcpy(data, message, len);
      data[len] = '\0';
      LOG_TRACE("[%d] %s", it, data);
      free(data);
    }
  }

  return 0;
}

bool client_request(const char* url, struct request* req)
{
  bool status = true;

  if (req->input.memory != NULL)
  {
    req->input.size = strlen(req->input.memory);
  }
  else
  {
    req->input.size = 0;
  }

  LOG_INFO("Request for %s", url);
  LOG_TRACE("Input: %s", req->input.memory);

  CURL *curl = curl_easy_init();

  if (curl)
  {
    // The response to the RPC call
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, client_read);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, (void *)&req->output);

    // The JSON data - arguments to the RPC call
    curl_easy_setopt(curl, CURLOPT_READFUNCTION, client_write);
    curl_easy_setopt(curl, CURLOPT_READDATA, (void *)&req->input);
    curl_easy_setopt(curl, CURLOPT_INFILESIZE_LARGE, (curl_off_t)req->input.size);

    // Disable Expect: 100-continue.
    struct curl_slist *chunk = NULL;
    chunk = curl_slist_append(chunk, "Expect:");
    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, chunk);

#ifdef DEBUG
    // Enable verbose logging
    curl_easy_setopt(curl, CURLOPT_VERBOSE, 1L);
    curl_easy_setopt(curl, CURLOPT_DEBUGFUNCTION, client_logger);
#endif

    // Upload, using POST
    curl_easy_setopt(curl, CURLOPT_UPLOAD, 1L);

    // Set the URL
    curl_easy_setopt(curl, CURLOPT_URL, url);

    LOG_TRACE("Performing request");
    CURLcode res = curl_easy_perform(curl);

    LOG_TRACE("Result: %d", res);

    if (res != CURLE_OK)
    {
      LOG_ERROR(curl_easy_strerror(res));

      status = false;
    }
    else
    {
      LOG_TRACE("Output: %s", req->output.memory);
    }

    curl_slist_free_all(chunk);
    curl_easy_cleanup(curl);
  }
  else
  {
    LOG_ERROR("Failed to get CURL");

    status = false;
  }

  return status;
}

size_t client_write(void *ptr, size_t size, size_t nmemb, void *userp)
{
  LOG_TRACE("Writing data for request");

  size_t real = size * nmemb;

  struct data *mem = (struct data*) userp;

  size_t result;

  if (mem->size == mem->ptr)
  {
    LOG_TRACE("No data to write");
    result = 0;
  }
  else if (real < (mem->size - mem->ptr))
  {
    // Pass in real
    LOG_TRACE("Writing %d bytes", real);
    memcpy(ptr, &(mem->memory[mem->ptr]), real);
    result = real;
  }
  else
  {
    // Pass in the rest
    result = mem->size - mem->ptr;
    LOG_TRACE("Writing the rest - %d bytes", result);
    memcpy(ptr, &(mem->memory[mem->ptr]), result);
  }

  mem->ptr += result;

  return result;
}

size_t client_read(void *contents, size_t size, size_t nmemb, void *userp)
{
  LOG_TRACE("Reading data for request");

  size_t realsize = size * nmemb;

  struct data *mem = (struct data *)userp;

  if (mem->memory)
  {
    mem->memory = (char*) realloc(mem->memory, mem->size + realsize + 1);
  }
  else
  {
    mem->memory = (char*) malloc(realsize + 1);
  }

  if(mem->memory == NULL)
  {
    LOG_ERROR("Not enough memory (alloc returned NULL)");
    return 0;
  }

  memcpy(&(mem->memory[mem->size]), contents, realsize);
  mem->size += realsize;
  mem->memory[mem->size] = 0;
 
  return realsize;
}
