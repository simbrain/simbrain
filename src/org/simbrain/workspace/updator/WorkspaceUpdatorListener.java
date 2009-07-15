 package org.simbrain.workspace.updator;

import org.simbrain.workspace.WorkspaceComponent;

/**
 * The listener interface for observers interested in events related to
 * component update activities.
 * 
 * @author Matt Watson
 */
public interface WorkspaceUpdatorListener {

	/**
     * Called when a component update begins.
     *  
     * @param component The component being updated.
     * @param update The number of the update.
     * @param thread The thread doing the update.
     */
    void startingComponentUpdate(WorkspaceComponent component, int update, int thread);
    
    /**
     * Called when a component update ends.
     *  
     * @param component The component that was updated.
     * @param update The number of the update.
     * @param thread The thread doing the update.
     */
    void finishedComponentUpdate(WorkspaceComponent component, int update, int thread);
    
    /**
     * Called when the couplings are updated.
     * 
     * @param update The number of the update.
     */
    void updatedCouplings(int update);
    
    /**
     * Called when the update controller is changed.
     */
    void changedUpdateController();
    
}