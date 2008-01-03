package org.simbrain.workspace;

/**
 * Base API for consumers and producers.
 * 
 * @author Matt Watson
 */
public interface AttributeHolder {
    
    /**
     * Returns the parent component for this object.
     * 
     * @return The parent component for this object.
     */
    WorkspaceComponent<?> getParentComponent();
    
    /**
     * Returns the description for this object.
     * 
     * @return The description for this object.
     */
    String getDescription();
}
