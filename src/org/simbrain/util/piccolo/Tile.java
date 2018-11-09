package org.simbrain.util.piccolo;

import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor2.EditableObject;

/**
 * Simbrain wrapper for a {@link TileMap} tile.
 */
public class Tile  implements EditableObject {

    //TODO: Currently just based on the xml id.
    // Can enhance this later with custom properties as in sample.tmx

    private int id;

    @UserParameter(label = "Label", description = "label", order = 1)
    private String label = "Label";

    @UserParameter(label = "Type", description = "type", order = 2)
    private String type = "Label";

    public Tile(int id) {
        this.id = id;
        label = "type:" + id;
    }

    public int getId() {
        return id;
    }
}
