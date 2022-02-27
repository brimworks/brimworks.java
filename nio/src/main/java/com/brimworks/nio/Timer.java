package com.brimworks.nio;

import java.io.Closeable;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Timer extends Watcher {
    public static Comparator<Timer> COMPARE_END_NANOS = (x, y) ->
        Long.compareUnsigned(x.priorityQueueEndNanos, y.priorityQueueEndNanos);

    private long afterMillis;
    private long repeatMillis;
    // Only updated when remove/add from priority queue is performed:
    private long priorityQueueEndNanos;
    private long endNanos;
    private Callback<Timer> callback = t -> {};

    protected Timer(EventLoop loop, int priority) {
        super(loop, priority);
    }

    public Timer callback(Callback<Timer> callback) {
        this.callback = callback;
        return this;
    }

    public Callback<Timer> callback() {
        return this.callback;
    }

    public long afterMillis() {
        return this.afterMillis;
    }

    public Timer afterMillis(long afterMillis) {
        if (0 != endNanos) {
            throw new IllegalStateException("Attempt to set afterMillis when timer is already started");
        }
        this.afterMillis = afterMillis;
        return this;
    }

    public long repeatMillis() {
        return this.repeatMillis;
    }

    public Timer repeatMillis(long repeatMillis) {
        if (0 != endNanos) {
            throw new IllegalStateException("Attempt to set repeatMillis when timer is already started");
        }
        this.repeatMillis = repeatMillis;
        return this;
    }

    public long remainingMillis() {
        if (endNanos != 0) {
            return nanosToMillis(endNanos - eventLoop().nanoTime());
        }
        return afterMillis;
    }

    protected long remainingMillisPriorityQueue(long now) {
        if (priorityQueueEndNanos != 0) {
            return nanosToMillis(priorityQueueEndNanos - now);
        }
        return afterMillis;
    }

    @Override
    public Timer start() {
        if (0 != endNanos) {
            throw new IllegalStateException("Attempt to set start when timer is already started");
        }
        endNanos = eventLoop().nanoTime() + TimeUnit.MILLISECONDS.toNanos(afterMillis);
        priorityQueueEndNanos = endNanos;
        eventLoop().add(this);
        return this;
    }

    @Override
    public Timer stop() {
        eventLoop().remove(this);
        endNanos = 0;
        priorityQueueEndNanos = 0;
        return this;
    }

    /**
     * Acts as though the timer expired now. Note that this behaves the same
     * as calling stop() if repeatMillis is <= 0.
     */
    public Timer again() {
        if (repeatMillis <= 0) {
            return stop();
        }
        priorityQueueEndNanos = eventLoop().nanoTime() + TimeUnit.MILLISECONDS.toNanos(repeatMillis);
        return this;
    }

    /**
     * NOTE: This method assumes it is ONLY called by the EventLoop, and thus is no longer in the
     * priority queue of timers.
     */
    @Override
    public void run() throws IOException {
        if (priorityQueueEndNanos != endNanos) {
            // Force re-enqueue into priority queue:
            endNanos = priorityQueueEndNanos;
            eventLoop().add(this);
            return;
        }
        try {
            callback.accept(this);
        } finally {
            if (repeatMillis > 0) {
                endNanos = eventLoop().nanoTime() + TimeUnit.MILLISECONDS.toNanos(repeatMillis);
                priorityQueueEndNanos = endNanos;
                eventLoop().add(this);
            } else {
                endNanos = 0;
                priorityQueueEndNanos = 0;
            }
        }
    }

    private static long nanosToMillis(long nanos) {
        return TimeUnit.NANOSECONDS.toMillis(nanos);
    }
}
