// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.reset;

/**
 * The reset service allows the application to optionally 'factory reset' all
 * application data, and optionally (and separately) restart the application
 * next time it shuts down.
 */
public interface ResetService
{
    /**
     * Sets whether we should reset the application (delete all user data for
     * the currently active user) next time we shut down

    * @param deleteUserConfig whether to reset or not user specific config on
    *        next shutdown
    */
    void setDeleteUserConfig(boolean deleteUserConfig);

   /**
    * Sets whether we should reset the application (delete data for all users
    * as well as global data) next time we shut down
   *
   * @param deleteGlobalConfig whether to reset or not on next shutdown
   */
   void setDeleteGlobalConfig(boolean deleteGlobalConfig);

   /**
    * Sets whether we should reset the application (delete all user data) next
    * time we shut down
   *
   * @param restart whether to restart or not following next shutdown
   */
   void setRestart(boolean restart);

   /**
    * Deletes the user's application data directory if doDeleteConfig is true;
    * restarts the application after shutdown if doRestart is true.
    */
   void shutdown();
}
