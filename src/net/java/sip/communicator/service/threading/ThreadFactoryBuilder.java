// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.threading;

import java.util.concurrent.ThreadFactory;

/**
 * Utility class for building custom Thread Factory objects. Can be supplied to
 * ExecutorServices for more fine-grained control over the threads they create
 * (i.e. ensuring they are all daemon threads).
 */
public class ThreadFactoryBuilder
{
    private String name = "ThreadFactoryBuilder-Thread-" + nextThreadNum();
    private boolean isDaemon = true;

    private static int threadInitNumber;

    private static synchronized int nextThreadNum() {
        return threadInitNumber++;
    }

    public ThreadFactoryBuilder() { }

    public ThreadFactoryBuilder setDaemon(boolean flag)
    {
        this.isDaemon = flag;
        return this;
    }

    public ThreadFactoryBuilder setName(String name)
    {
        this.name = name;
        return this;
    }

    public ThreadFactory build()
    {
        return this::getThreadFactory;
    }

    private Thread getThreadFactory(Runnable r)
    {
        Thread t = new Thread(r);
        t.setName(name);
        t.setDaemon(isDaemon);
        return t;
    }

}
