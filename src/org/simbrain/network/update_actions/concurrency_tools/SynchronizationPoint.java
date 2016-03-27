package org.simbrain.network.update_actions.concurrency_tools;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SynchronizationPoint implements Task {

    private final AtomicInteger syncCounter;
    
    private final AtomicBoolean workAvailable = new AtomicBoolean();
    
    private final int maxHits;
    
    public SynchronizationPoint(final int maxHits) {
        this.maxHits = maxHits;
        syncCounter = new AtomicInteger(0);
    }
    
    @Override
    public void perform() {
        synchronized(syncCounter) {
            syncCounter.incrementAndGet();
            if (syncCounter.get() == maxHits) {
                    syncCounter.notifyAll();
            }
        }
        long t = System.currentTimeMillis();
        while (true) {
        	if (System.currentTimeMillis() - t > 30000) {
        		synchronized(this) {
        			try {
						wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
        		}
        	}
            if (workAvailable.get()) break;
        }
        syncCounter.decrementAndGet();
    }

    public void reset() {
        syncCounter.set(0);
    }
    
    public int getSyncCount() {
        return syncCounter.get();
    }
    
    public void setWorkAvailable(final boolean workAvailable) {
        this.workAvailable.set(workAvailable);
    }
    
    public boolean isWorkAvailable() {
        return workAvailable.get();
    }
    
    public AtomicInteger getSyncCounter() {
        return syncCounter;
    }

}
