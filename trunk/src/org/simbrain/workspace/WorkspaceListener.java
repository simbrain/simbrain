package org.simbrain.workspace;

public interface WorkspaceListener {
    /**
     * event when the workspace is requested to be cleared
     * listeners should not clear themselves until the
     * clearWorkspace event
     * 
     * @return whether to continue (true) or to cancel (false)
     */
    boolean clearWorkspace();
    
    void workspaceCleared();
    
    void componentAdded(WorkspaceComponent<?> component);
    
    void componentRemoved(WorkspaceComponent<?> component);
}
