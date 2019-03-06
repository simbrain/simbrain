package org.simbrain.util.propertyeditor;

/**
 * Indicates that an object can be copied. Used by the {@link ObjectTypeEditor}.
 * When an object's type is changed using the editor, a prototype object is created
 * and then copies of that object are made and applied to all the edited objects.
 *
 * @author Jeff Yoshimi
 */
public interface CopyableObject extends EditableObject {

    /**
     * Return a deep copy of this object.
     *
     * @return a copy of the object.
     */
    public EditableObject copy();

}
