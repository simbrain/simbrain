package org.simbrain.util.propertyeditor2;

import org.simbrain.util.BiMap;

import java.util.HashMap;

/**
 * Objects that implement this interface can be edited using an
 * {@link ObjectTypeEditor}.
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
