package org.simbrain.workspace.updater;

import java.util.concurrent.CountDownLatch;

/**
 * Signal implementation that wraps a count-down latch.
 * 
 * @author Matt Watson
 */
public class LatchCompletionSignal implements CompletionSignal {

    /** The underlying latch. */
    private volatile CountDownLatch latch;

    /**
     * Creates a new instance with the given latch.
     * 
     * @param latch the latch to back this object.
     */
    public LatchCompletionSignal(final CountDownLatch latch) {
        this.latch = latch;
    }

    /**
     * Creates a new instance which waits for a single done call.
     * 
     * @param signals the number of signals to wait for.
     */
    public LatchCompletionSignal(final int signals) {
        this(new CountDownLatch(signals));
    }

    /**
     * Creates a new instance which waits for a single done call.
     */
    public LatchCompletionSignal() {
        this(1);
    }

    /**
     * Called when a task is done.
     */
    public void done() {
        CountDownLatch latch = this.latch;

        latch.countDown();
    }

    /**
     * Returns the latch underlying this signal.
     * 
     * @return The latch underlying this signal.
     */
    public CountDownLatch getLatch() {
        return latch;
    }

    /**
     * Calls await on the underlying latch.
     */
    public void await() {
        try {
            CountDownLatch latch = this.latch;

            latch.await();
        } catch (InterruptedException e) {
            return;
        }
    }
}