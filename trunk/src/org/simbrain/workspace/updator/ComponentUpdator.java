package org.simbrain.workspace.updator;

import org.simbrain.workspace.WorkspaceComponent;

/**
 * Callback interface for updating components.
 * 
 * <p>This package doesn't have access to update
 * components so this interface allows it to
 * without giving global access.
 * 
 * @author Matt Watson
 */
public interface ComponentUpdator {
    /**
     * Updates the given component.
     * 
     * @param component The component to update.
     */
    void update(WorkspaceComponent<?> component);
}
