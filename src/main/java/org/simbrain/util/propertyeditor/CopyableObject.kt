package org.simbrain.util.propertyeditor;

/**
 * Indicates that an object can be copied.
 *
 * Used by the {@link ObjectTypeEditor}, which creates and edits a prototype object and then makes copies of
 * that object. For example, if editing a bunch of neurons that have different update rules, setting one of them to a
 * certain udpate rule  will require that they all now have that rule. A single prototype rule is copied and written
 * to all the neurons.
 *
 * @author Jeff Yoshimi
 */
public interface CopyableObject extends EditableObject {

    /**
     * Return a deep copy of this object.
     *
     * @return a copy of the object.
     */
    public CopyableObject copy();

}
