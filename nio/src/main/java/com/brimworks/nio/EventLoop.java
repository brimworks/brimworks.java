package com.brimworks.nio;

import java.io.Closeable;
import java.util.Iterator;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.PriorityQueue;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.HashSet;

public class EventLoop implements Iterable<Watcher>, Closeable {
    private enum Break {
        NONE,
        ONE,
        ALL;
    }

    // Selector:
    private Selector selector;

    // Active SelectionKeys:
    private Set<SelectionKey> activeSelectionKeys = new HashSet<>();

    // Used for breaking out of the event loop:
    private Break break_ = Break.NONE;

    // Used for timers:
    private PriorityQueue<Timer> timerQueue = new PriorityQueue<>(Timer.COMPARE_END_NANOS);

    // Start of event loop:
    private long now = System.nanoTime();

    // Count loop iterations:
    private long loopIterations = 0;

    // Count loop depth:
    private int loopDepth = 0;

    // Used for pending watchers that need to be ran:
    private PriorityQueue<Watcher> pending = new PriorityQueue<>();

    public EventLoop() throws IOException {
        this(Selector.open());
    }

    public EventLoop(Selector selector) throws IOException {
        this.selector = selector;
    }

    @Override
    public void close() throws IOException {
        selector.close();
    }

    @Override
    public Iterator<Watcher> iterator() {
        throw new UnsupportedOperationException();
    }

    protected void add(Timer timer) {
        timerQueue.add(timer);
    }

    protected void add(IOWatcher watcher) {
        activeSelectionKeys.add(watcher.selectionKey());
    }

    protected void remove(Timer timer) {
        timerQueue.remove(timer);
        pending.remove(timer);
    }

    protected void remove(IOWatcher watcher) {
        activeSelectionKeys.remove(watcher.selectionKey());
        pending.remove(watcher);
    }

    public Watcher next() throws IOException {
        Watcher result = pending.poll();
        if (null != result) {
            return result;
        }
        now = System.nanoTime();
        Timer timer = timerQueue.peek();
        loopIterations++;
        if (null == timer) {
            if (activeSelectionKeys.isEmpty()) {
                breakOne();
                return null;
            }
            selector.select();
            now = System.nanoTime();
        } else {
            long millis = timer.remainingMillisPriorityQueue(now);
            if (millis > 0) {
                selector.select(millis);
                now = System.nanoTime();
            }
        }
        for ( SelectionKey key : selector.selectedKeys()) {
            IOWatcher watcher = (IOWatcher)key.attachment();
            pending.add(watcher);
        }
        while (true) {
            timer = timerQueue.peek();
            if (null == timer || timer.remainingMillisPriorityQueue(now) > 0) {
                break;
            }
            timerQueue.remove(timer);
            pending.add(timer);
        }
        return pending.poll();
    }

    public void breakOne() {
        break_ = Break.ONE;
    }

    public void breakAll() {
        break_ = Break.ALL;
    }

    public void run() throws IOException {
        try {
            //  Increment loop depth.
            loopDepth++;
            // Reset the ev_break status.
            break_ = Break.NONE;
            do {
                Watcher watcher = next();
                if (null == watcher) {
                    break;
                }
                watcher.run();
            } while (break_ == Break.NONE);
        } finally {
            if (break_ == Break.ONE) {
                break_ = Break.NONE;
            }
            loopDepth--;
        }
    }

    public long nanoTime() {
        return now;
    }

    public int loopDepth() {
        return loopDepth;
    }

    public long loopIterations() {
        return loopIterations;
    }

    public Timer timer(int priority) {
        return new Timer(this, priority);
    }

    public IOWatcher ioWatcher(int priority, SelectableChannel channel) throws IOException {
        SelectionKey key = channel.register(selector, 0);
        IOWatcher watcher = (IOWatcher)key.attachment();
        if (null == watcher) {
            watcher = new IOWatcher(this, priority, key);
            key.attach(watcher);
        }
        return watcher;
    }
}
