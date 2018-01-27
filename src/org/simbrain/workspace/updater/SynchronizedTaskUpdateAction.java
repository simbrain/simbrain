package org.simbrain.workspace.updater;

/**
 * SynchronizedTaskUpdateAction executes AWT-driven events which may have been queued during the workspace update.
 */
public class SynchronizedTaskUpdateAction implements UpdateAction {
    private transient TaskSynchronizationManager syncManager;

    public SynchronizedTaskUpdateAction(TaskSynchronizationManager syncManager) {
        this.syncManager = syncManager;
    }

    public void setSyncManager(TaskSynchronizationManager value) {
        syncManager = value;
    }

    @Override
    public String getDescription() {
        return "Run Synchronized Tasks";
    }

    @Override
    public String getLongDescription() {
        return "Dispatches invocation events which may have been queued. This action must be run regularly.";
    }

    @Override
    public void invoke() {
        syncManager.runTasks();
    }
}
