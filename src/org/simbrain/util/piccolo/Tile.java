package org.simbrain.util.piccolo;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.converters.extended.NamedMapConverter;
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor2.EditableObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Simbrain wrapper for a {@link TileMap} tile.
 */
@XStreamAlias("tile")
public class Tile  implements EditableObject {

    // Ideal structure for Odor World version / notes for future javadoc comments
    //
    // TileType (= tmx tile id).
    //  Based on tmx ids, integer.
    //  Display label is "Tile Type ID".
    //  This determines what image is used
    //   Associated with custom properties.
    //   We can add a custom property called SimbrainType, and if filled, use it to set SimbrainType
    //
    // SimbrainTileType (Grass, Water, etc). Initially just use a string.
    //   Maybe later a list of strings if multiple type systems

    /**
     * The local tile ID within its tileset.
     */
    @XStreamAsAttribute
    private int id;

    @XStreamConverter(
            value = NamedMapConverter.class,
            strings = {"property", "name", "value"},
            types = {String.class, String.class},
            booleans = {true, true}
    )
    private HashMap<String, String> properties;

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
