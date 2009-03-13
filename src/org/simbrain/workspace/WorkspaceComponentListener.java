package org.simbrain.workspace;

/**
 * Interface for workspace component listeners.
 * 
 * @author Matt Watson
 */
public interface WorkspaceComponentListener {

    /**
     * Called when the target workspace component is updated.
     */
    void componentUpdated();
    
    /**
     * Resets the component's name.
     */
    void setTitle(final String name);
    
//    void attributeAdded(AttributeHolder parent, Attribute attribute);
    
    /**
     * An attribute was removed.
     */
    void attributeRemoved(AttributeHolder parent, Attribute attribute);
}
