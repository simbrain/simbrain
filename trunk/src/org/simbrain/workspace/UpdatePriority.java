package org.simbrain.workspace;

/**
 * Used in priority based updating.  Instances of classes which implement this interface
 * have an update priority, and when the workspace is globally updated components are
 * updated in the order of their priority.  Lower numbers have higher priority; they are updated
 * first.
 * 
 * @author jyoshimi
 *
 */
public interface UpdatePriority {

    /** Return the priority of this component. */
    public int getPriority();

    /** Set the priority of this component. */
    public void setPriority(int value);

}
