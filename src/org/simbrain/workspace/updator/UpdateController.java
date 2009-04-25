package org.simbrain.workspace.updator;


/**
 * Interface for controllers to receive the controls required to
 * run custom updates.
 * 
 * @author Matt Watson
 */
public interface UpdateController {

    /**
     * Starts an update the provided controls should be used to manage the update.
     * 
     * @param controls controls required to run custom updates.
     */
    void doUpdate(UpdateControls controls);
    
    /**
     * Returns the name of this updator.  Useful in the GUI representation.
     * 
     * @return the updator name.
     */
    String getName();
}