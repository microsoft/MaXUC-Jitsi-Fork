// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.diagnostics;

import java.util.Set;

/**
 * A service which exposes various different diagnostic abilities to the rest
 * of the application
 */
public interface DiagnosticsService
{
    /**
     * Triggers an error report frame to open for errors with a known
     * identifying string. Opened for either automatically registered errors or
     * manual reports, such as user feedback or call quality frames.
     *
     * @param reason The reason for showing the error report frame
     * @param fullMessage The full error message details to send with this
     *                    error report
     * @param errorID The unique identifying string for this error
     */
    void openErrorReportFrame(ReportReason reason, String fullMessage,
                              String errorID);

    /**
     * Triggers an error report frame top open for errors with a detailed
     * message and no unique identifying string.
     *
     * @param reason The reason for showing the error report frame
     * @param fullMessage The full error message details to send with this
     *                    error report
     */
    void openErrorReportFrame(ReportReason reason, String fullMessage);

    /**
     * Triggers an error frame to open for a specific known reason.
     *
     * @param reason The reason for showing the error report frame
     */
    void openErrorReportFrame(ReportReason reason);

    /**
     * Erases the stored list of errors to allow showing more.
     */
    void clearEncounteredErrors();

    /**
     * Adds an error type to the list of those currently shown in open report
     * frames.
     *
     * @param errorID The unique identifying string for this error
     */
    void addActiveError(String errorID);

    /**
     * Removes an error type from the list of those currently shown in open
     * report frames.
     *
     * @param errorID The unique identifying string for this error
     */
    void removeActiveError(String errorID);

    /**
     * Called by different components of the application that wish to have some
     * state included in all error reports.
     *
     * @param stateDumper The object used to get the state to dump
     */
    void addStateDumper(StateDumper stateDumper);

    /**
     * Remove a registered state dumper if we no longer need it.
     *
     * @param stateDumper The state dumper to remove
     */
    void removeStateDumper(StateDumper stateDumper);

    /**
     * Called after a user sets an error to be suppressed, writing it to config
     * and updating the memory-stored error to match.
     */
    void setUserSuppressedErrors(String fullList);

    /**
     * Returns the list of errors the user has suppressed.
     *
     * @return The list of user-suppressed errors
     */
    String getUserSuppressedErrors();

    /**
     * @return a set of all the registered state dumpers
     */
    Set<StateDumper> getDumpers();
}
