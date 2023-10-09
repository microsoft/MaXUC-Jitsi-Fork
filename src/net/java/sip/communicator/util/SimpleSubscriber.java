package net.java.sip.communicator.util;

import java.util.concurrent.Flow;

/**
 * A simple implementation of Flow.Subscriber which allows you to only override
 * methods you want, onNext for example, thus avoiding boilerplate.
 */
public abstract class SimpleSubscriber<T> implements Flow.Subscriber<T>
{
    private Flow.Subscription subscription;

    @Override
    public void onSubscribe(Flow.Subscription subscription)
    {
        this.subscription = subscription;
        // Need to explicitly request data.
        this.subscription.request(1);
        // Do nothing by default.
    }

    @Override
    public void onNext(T item)
    {
        // Every time we process an onNext event, we request a next other -
        // otherwise we would stop receiving events.
        this.subscription.request(1);
        // Do nothing by default.
    }

    @Override
    public void onError(Throwable throwable)
    {
        // Do nothing by default.
    }

    @Override
    public void onComplete()
    {
        // Do nothing by default.
    }
}
