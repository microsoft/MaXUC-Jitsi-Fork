// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.threading;

import java.util.concurrent.*;

/**
 * The ThreadingService allows access to a thread pool that is shared across
 * the whole application.
 */
public interface ThreadingService
{
    /**
     * Submits a value-returning task for execution and returns a
     * Future representing the pending results of the task. The
     * Future's <tt>get</tt> method will return the task's result upon
     * successful completion.
     *
     * <p>
     * If you would like to immediately block waiting
     * for a task, you can use constructions of the form
     * <tt>result = exec.submit(aCallable).get();</tt>
     *
     * @param name a name for the task to be run
     * @param callable the task to submit
     * @return a Future representing pending completion of the task
     */
    <T> Future<T> submit(String name, Callable<T> callable);

    /**
     * Submits a Runnable task for execution.
     *
     * @param name a name for the task to be run
     * @param runnable the task to submit
     */
    void submit(String name, Runnable runnable);

    /**
     * @return Information on the currently scheduled tasks that haven't been run yet.  For
     * diagnostics purposes.
     */
    String getScheduledTasksInfo();

    /**
     * Schedules the specified runnable for execution after the specified delay.
     *
     * <p>
     * This task can be cancelled before it is run in which case it won't be run.
     *
     * <p>
     * Note that the runnable is executed on one of the threads in the thread
     * pool.  Exactly which thread can't be determined.  Thus if you schedule
     * two tasks at the same time, there is no guarantee in which order they
     * will be executed - they may even be executed concurrently
     *
     * @param name a name for the task to be scheduled
     * @param runnable the runnable to be scheduled.
     * @param delay delay in milliseconds before task is to be executed.
     */
    void schedule(String name, CancellableRunnable runnable, long delay);

    /**
     * Similar to schedule, this schedules the specified runnable for execution
     * after the specified delay.  However, the runnable will be invoked on the
     * EDT.
     *
     * <p>
     * This task can be cancelled before it is run in which case it won't be run.
     *
     * @param name a name for the task to be scheduled
     * @param runnable the runnable to be scheduled.
     * @param delay delay in milliseconds before task is to be executed.
     */
    void scheduleOnEDT(String name, CancellableRunnable runnable, long delay);

    /**
     * Schedules the specified runnable for execution after the specified delay
     * and to be repeated after the specified period forever or until cancelled.
     *
     * <p>
     * This task can be cancelled before it is run in which case it won't be run.
     *
     * <p>
     * Note that the runnable is executed on one of the threads in the thread
     * pool.  Exactly which thread can't be determined.  Thus if you schedule
     * two tasks at the same time, there is no guarantee in which order they
     * will be executed - they may even be executed concurrently
     *
     * @param name a name for the task to be scheduled
     * @param runnable the runnable to be scheduled.
     * @param delay delay in milliseconds before task is to be executed.
     * @param period period in milliseconds between repeated executions of the task.
     */
    void scheduleAtFixedRate(String name,
                             CancellableRunnable runnable,
                             long delay,
                             long period);
}
