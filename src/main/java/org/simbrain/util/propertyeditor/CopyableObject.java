package org.simbrain.util.propertyeditor;

/**
 * Indicates that an object can be used by the {@link ObjectTypeEditor}.
 * A more apt but uglier name might have been "UsableByObjectTypeEditor".
 * Being used by the object type editor requires that a prototype object is created
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
    public CopyableObject copy();

}
