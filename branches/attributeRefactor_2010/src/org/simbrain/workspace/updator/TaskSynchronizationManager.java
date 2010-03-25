package org.simbrain.workspace.updator;

/**
 * Interface for integrating user interface threads with updator.
 * 
 * @author Matt Watson
 */
public interface TaskSynchronizationManager {
    /**
     * Tells the manager to queue tasks.
     */
    void queueTasks();

    /**
     * Tells the manager to stop queuing tasks.
     */
    void releaseTasks();

    /**
     * Tells the managers to run all queued tasks.
     */
    void runTasks();
}