package org.simbrain.workspace.updater;

import org.simbrain.workspace.WorkspaceComponent;

/**
 * Represents a single part of a component update. (Note: Component updates
 * involving multiple parts have not been tested yet. Theoretically the idea is
 * that a type of workspace component would override getComponentParts and
 * return a set of update tasks. I believe they must be able to be separately
 * run in a given iteration but again, this has not been tested (JKY).
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
     * Creates a new instance with the given parent, runnable, description and
     * lock.
     *
     * @param parent The parent component.
     * @param runnable The task to execute.
     * @param description The description of the part.
     * @param lock The lock to use.
     */
    public ComponentUpdatePart(final WorkspaceComponent parent,
            final Runnable runnable, final String description, final Object lock) {
        this.parent = parent;
        this.runnable = runnable;
        this.description = description;
        this.lock = lock;
    }

    /**
     * Creates a new instance with the given parent, runnable, description using
     * this object as the lock.
     *
     * @param parent The parent component.
     * @param runnable The task to execute.
     * @param description The description of the part.
     */
    public ComponentUpdatePart(final WorkspaceComponent parent,
            final Runnable runnable, final String description) {
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

                    WorkspaceUpdater.LOGGER.trace("updating component part: "
                            + getDescription());

                    runnable.run();

                    thread.clearCurrentTask(ComponentUpdatePart.this);
                    signal.done();
                }
            }
        };
    }
}
