package org.simbrain.workspace.updator;

import org.simbrain.workspace.WorkspaceComponent;

/**
 * Represents a single part of a component update.
 * 
 * @author Matt Watson
 */
public class ComponentUpdatePart {
    /** The parent component. */
    private final WorkspaceComponent parent;
    /** The runnable task. */
    private final Runnable runnable;
    /** The description of the part. */
    private final String description;
    /** The lock for synchronization. */
    private final Object lock;
    
    /**
     * Creates a new instance with the given parent, runnable, description and lock.
     * 
     * @param parent The parent component.
     * @param runnable The task to execute.
     * @param description The description of the part.
     * @param lock The lock to use.
     */
    public ComponentUpdatePart(final WorkspaceComponent parent, final Runnable runnable,
            final String description, final Object lock) {
        this.parent = parent;
        this.runnable = runnable;
        this.description = description;
        this.lock = lock;
    }
    
    /**
     * Creates a new instance with the given parent, runnable, description using this object
     * as the lock.
     * 
     * @param parent The parent component.
     * @param runnable The task to execute.
     * @param description The description of the part.
     */
    public ComponentUpdatePart(final WorkspaceComponent parent, final Runnable runnable,
            final String description) {
        this.parent = parent;
        this.runnable = runnable;
        this.description = description;
        this.lock = this;
    }
    
    /**
     * Returns the parent of this component.
     * 
     * @return the parent of this component.
     */
    public WorkspaceComponent getParent() {
        return parent;
    }
    
    /**
     * Returns the lock for this object.
     * 
     * @return the lock for this object.
     */
    Object getLock() {
        return lock;
    }
    
    /**
     * Returns the description for this part.
     *
     * @return the description for this part.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get's a runnable that counts down the latch after completion.
     *
     * @param signal the latch to count down on.
     * @return The runnable to execute.
     */
    Runnable getUpdate(final CompletionSignal signal) {
        return new Runnable() {
            public void run() {
                synchronized (lock) {
                    UpdateThread thread = (UpdateThread) Thread.currentThread();

                    thread.setCurrentTask(ComponentUpdatePart.this);

                    WorkspaceUpdator.LOGGER.trace("updating component part: " + getDescription());

                    runnable.run();

                    thread.clearCurrentTask(ComponentUpdatePart.this);
                    signal.done();
                }
            }
        };
    }
}
