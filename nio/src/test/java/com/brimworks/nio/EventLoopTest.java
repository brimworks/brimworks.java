package com.brimworks.nio;

import org.junit.jupiter.api.Test;
import java.nio.channels.Pipe;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.hamcrest.MatcherAssert.assertThat; 
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.equalTo;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EventLoopTest {
    @Test
    public void test() throws Exception {
        EventLoop loop = new EventLoop();
        Pipe pipe = Pipe.open();
        pipe.sink().configureBlocking(false);
        pipe.source().configureBlocking(false);
        ByteBuffer output = ByteBuffer.allocate(1000);
        AtomicBoolean wrote = new AtomicBoolean(false);
        AtomicBoolean timerTriggered = new AtomicBoolean(false);

        IOWatcher writeWatcher = loop.ioWatcher(1, pipe.sink())
            .callback(watcher -> {
                pipe.sink().write(ByteBuffer.wrap("hello".getBytes(UTF_8)));
                wrote.set(true);
                watcher.stop();
            })
            .interestOps(IOWatcher.Operation.WRITE);

        IOWatcher readWatcher = loop.ioWatcher(2, pipe.source())
            .callback(watcher -> {
                pipe.source().read(output);
                watcher.stop();
            })
            .interestOps(IOWatcher.Operation.READ);

        Timer timer = loop.timer(3)
            .callback(watcher -> {
                timerTriggered.set(true);
                loop.breakOne();
            })
            .afterMillis(60000);

        // Should be a no-op:
        long t0 = System.nanoTime();
        loop.run();
        assertEquals(1, loop.loopIterations());
        assertEquals(0, loop.loopDepth());
        assertFalse(timerTriggered.get());
        assertThat(System.nanoTime() - t0, lessThan(TimeUnit.MILLISECONDS.toNanos(100)));
        assertFalse(wrote.get());
        assertThat(output.position(), equalTo(0));

        // Start the writer:
        writeWatcher.start();
        loop.run();
        assertEquals(3, loop.loopIterations());
        assertEquals(0, loop.loopDepth());
        assertFalse(timerTriggered.get());
        assertTrue(wrote.get());
        assertThat(output.position(), equalTo(0));

        // Start the reader:
        readWatcher.start();
        loop.run();
        assertEquals(5, loop.loopIterations());
        assertEquals(0, loop.loopDepth());
        assertFalse(timerTriggered.get());
        assertThat(output.position(), equalTo(5));

        // Start the timer:
        timer.afterMillis(10)
            .start();
        loop.run();

        assertEquals(6, loop.loopIterations());
        assertEquals(0, loop.loopDepth());
        assertTrue(timerTriggered.get());
    }
}