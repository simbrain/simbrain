package org.simbrain.workspace.updator;

import java.util.concurrent.CountDownLatch;

import org.simbrain.workspace.WorkspaceComponent;

public class ComponentUpdatePart {
    private final WorkspaceComponent<?> parent;
    private final Runnable runnable;
    private final String description;
    private final Object lock;
    
    public ComponentUpdatePart(WorkspaceComponent<?> parent, Runnable runnable, String description, Object lock) {
        this.parent = parent;
        this.runnable = runnable;
        this.description = description;
        this.lock = lock;
    }
    
    public ComponentUpdatePart(WorkspaceComponent<?> parent, Runnable runnable, String description) {
        this.parent = parent;
        this.runnable = runnable;
        this.description = description;
        this.lock = this;
    }
    
    public WorkspaceComponent<?> getParent() {
        return parent;
    }
    
    Object getLock() {
        return lock;
    }
    
    public String getDescription() {
        return description;
    }
    
    Runnable getUpdate(final CountDownLatch latch) {
        return new Runnable() {
            public void run() {
                synchronized (lock) {
                    UpdateThread thread = (UpdateThread) Thread.currentThread();
                    
                    thread.setCurrentTask(ComponentUpdatePart.this);
                    
                    WorkspaceUpdator.LOGGER.trace("updating component part: " + getDescription());
                    
                    runnable.run();
                    
                    thread.clearCurrentTask(ComponentUpdatePart.this);
                    
                    latch.countDown();
                }
            }
        };
    }
}
