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
     * Returns a description for this attribute holder.
     * This is used for serialization and in interface 
     * elements which display attributes.
     * 
     * NOTE: This description must be unique relative to
     * other AttributeHolders in a Component.
     * 
     * @return The description for this attribute holder.
     */
    String getDescription();
}
