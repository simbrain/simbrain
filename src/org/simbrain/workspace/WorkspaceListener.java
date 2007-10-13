package org.simbrain.workspace;

public interface WorkspaceListener {
    void workspaceCleared();
    
    void componentAdded(WorkspaceComponent component);
    
    void componentRemoved(WorkspaceComponent component);
}
