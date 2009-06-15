package org.simbrain.workspace.updator;

import java.util.List;

import org.simbrain.workspace.WorkspaceComponent;

/**
 * Interface provided to custom update methods.
 * 
 * @author Matt Watson
 */
public interface UpdateControls {
    /**
     * Updates all the couplings.
     */
    void updateCouplings();
    
    /**
     * Returns the components in the workspace.
     * 
     * @return the components in the workspace.
     */
    List<? extends WorkspaceComponent> getComponents();
    
    /**
     * Submits a component for update, calling countDown on the given latch
     * when it is updated.
     * 
     * @param component The component to update.
     * @param signal The signal to call done on when complete.
     */
    void updateComponent(WorkspaceComponent component, CompletionSignal signal);
}