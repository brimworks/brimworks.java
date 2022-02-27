package com.brimworks.nio;

import java.nio.channels.SelectionKey;
import java.io.IOException;

public class IOWatcher extends Watcher {
    public enum Operation {
        ACCEPT(SelectionKey.OP_ACCEPT),
        CONNECT(SelectionKey.OP_CONNECT),
        READ(SelectionKey.OP_READ),
        WRITE(SelectionKey.OP_WRITE);

        private int code;
        Operation(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    private SelectionKey selectionKey;
    private boolean isActive = false;
    private int interestOps = 0;
    private Callback<IOWatcher> callback = w -> {};

    protected IOWatcher(EventLoop loop, int priority, SelectionKey selectionKey) {
        super(loop, priority);
        this.selectionKey = selectionKey;
    }

    public IOWatcher callback(Callback<IOWatcher> callback) {
        if (null == callback) {
            throw new IllegalArgumentException("callback must be non-null");
        }
        this.callback = callback;
        return this;
    }

    public Callback<IOWatcher> callback() {
        return this.callback;
    }

    public IOWatcher interestOps(Operation... operations) {
        int ops = 0;
        for (IOWatcher.Operation operation : operations) {
            ops |= operation.getCode();
        }
        return interestOps(ops);
    }

    public IOWatcher interestOps(int ops) {
        this.interestOps = ops;
        if (isActive) {
            selectionKey.interestOps(ops);
        }
        return this;
    }

    public int interestOps() {
        return this.interestOps;
    }

    public SelectionKey selectionKey() {
        return selectionKey;
    }

    public boolean isAcceptable() {
        return selectionKey.isAcceptable();
    }

    public boolean isConnectable() {
        return selectionKey.isConnectable();
    }

    public boolean isReadable() {
        return selectionKey.isReadable();
    }

    public boolean isWritable() {
        return selectionKey.isWritable();
    }

    @Override
    public IOWatcher start() {
        if (0 == interestOps) {
            throw new IllegalStateException(
                "Attempt to start a watcher without specifying the interestOps");
        }
        selectionKey.interestOps(interestOps);
        eventLoop().add(this);
        isActive = true;
        return this;
    }

    @Override
    public IOWatcher stop() {
        selectionKey.interestOps(0);
        eventLoop().remove(this);
        isActive = false;
        return this;
    }

    @Override
    public void run() throws IOException {
        if (isActive) {
            callback.accept(this);
        }
    }
}
