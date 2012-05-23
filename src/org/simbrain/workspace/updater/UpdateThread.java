package org.simbrain.workspace.updater;

/**
 * This thread class adds some special methods specific to this
 * class.
 *
 * @author Matt Watson
 */
class UpdateThread extends Thread {

    private final WorkspaceUpdater updater;

    /** The thread number. */
    final int thread;

    /**
     * Creates a new instance with the given runnable and thread.
     *
     * @param updater The updater for to notify
     * @param runnable The runnable instance.
     * @param thread The thread number.
     */
    UpdateThread(final WorkspaceUpdater updater, final Runnable runnable, final int thread) {
        super(runnable);
        this.updater = updater;
        this.thread = thread;
    }

    /**
     * Sets the current component update that is being executed.
     *
     * @param update the component update that is executing.
     */
    void setCurrentTask(final ComponentUpdatePart update) {
        updater.notifyComponentUpdateStarted(update.getParent(), thread);
    }

    /**
     * Clears the current component update.
     *
     * @param update The component update to be cleared.
     */
    void clearCurrentTask(final ComponentUpdatePart update) {
        updater.notifyComponentUpdateFinished(update.getParent(), thread);
    }
}