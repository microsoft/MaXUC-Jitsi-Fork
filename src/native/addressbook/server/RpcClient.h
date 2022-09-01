#include <jansson.h>

size_t writeRequest(void *ptr, size_t size, size_t nmemb, void *userp);
size_t receiveResponse(void *contents, size_t size, size_t nmemb, void *userp);

void start_client(int port);
void stop_client();

void client_start();
void client_mapi_status();

bool client_contact_deleted(LPSTR id);
bool client_contact_inserted(LPSTR id);
bool client_contact_updated(LPSTR id);

bool client_calendar_inserted(json_t * data);

/**
 * Get the "type" (or PR_MESSAGE_CLASS) for the passed ID.  Returns
 * a json_t* which the caller is responsible for calling json_decref
 * on.
 *
 * @param id The ID of the event to get the type for
 * @return the type of the object with that ID
 */
json_t* client_get_type(LPSTR id);

void Server_inserted(LPSTR id);
void Server_updated(LPSTR id);
void Server_deleted(LPSTR id);