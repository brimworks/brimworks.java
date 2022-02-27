package com.brimworks.nio;

import java.io.IOException;

/**
 * Note that all callbacks are ran in the event loop. Therefore a callback should not
 * block!
 */
public abstract class Watcher implements Comparable<Watcher> {
    private EventLoop loop;
    private int priority;

    protected Watcher(EventLoop loop, int priority) {
        this.loop = loop;
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }

    public abstract Watcher stop();

    public abstract Watcher start();

    public abstract void run() throws IOException;

    public EventLoop eventLoop() {
        return loop;
    }

    public int compareTo(Watcher other) {
        return Integer.compare(other.priority, priority);
    }
}
