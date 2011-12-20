package org.simbrain.workspace.updator;

/**
 * This thread class adds some special methods specific to this
 * class.
 *
 * @author Matt Watson
 */
class UpdateThread extends Thread {
    /** */
    private final WorkspaceUpdator updator;
    /** The thread number. */
    final int thread;

    /**
     * Creates a new instance with the given runnable and thread.
     *
     * @param updator The updator for to notify
     * @param runnable The runnable instance.
     * @param thread The thread number.
     */
    UpdateThread(final WorkspaceUpdator updator, final Runnable runnable, final int thread) {
        super(runnable);
        this.updator = updator;
        this.thread = thread;
    }

    /**
     * Sets the current component update that is being executed.
     *
     * @param update the component update that is executing.
     */
    void setCurrentTask(final ComponentUpdatePart update) {
        updator.notifyComponentUpdateStarted(update.getParent(), thread);
    }

    /**
     * Clears the current component update.
     *
     * @param update The component update to be cleared.
     */
    void clearCurrentTask(final ComponentUpdatePart update) {
        updator.notifyComponentUpdateFinished(update.getParent(), thread);
    }
}