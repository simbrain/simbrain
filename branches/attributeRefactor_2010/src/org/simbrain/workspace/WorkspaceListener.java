package org.simbrain.workspace;

/**
 * Interface for listeners on a workspace.
 *
 * @author Matt Watson
 */
public interface WorkspaceListener {

    /**
     * Called after the workspace has been cleared.
     */
    void workspaceCleared();

    /**
     * Called when a component is added.
     *
     * @param component The component that was added.
     */
    void componentAdded(WorkspaceComponent component);

    /**
     * Called when a component is removed.
     *
     * @param component The component that was removed.
     */
    void componentRemoved(WorkspaceComponent component);

}
