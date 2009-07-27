package org.simbrain.workspace;

/**
 * Interface for workspace component listeners.
 * 
 * @author Matt Watson
 */
public interface WorkspaceComponentListener {

    /**
     * Called when the workspace component is updated.
     */
    void componentUpdated();

    /**
     * Called when the component's gui(s) are turned on or off.
     */
	void guiToggled();

    /**
     * Called when the component is turned on or off.
     */
	void componentOnOffToggled();
    
}
