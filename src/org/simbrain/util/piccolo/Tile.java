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

    /**
     * Custom properties defined in tmx.
     */
    @XStreamConverter(
            value = NamedMapConverter.class,
            strings = {"property", "name", "value"},
            types = {String.class, String.class},
            booleans = {true, true}
    )
    private HashMap<String, String> properties;

    /**
     * Label for this tile
     */
    @UserParameter(label = "Label", description = "label", order = 1)
    private transient String label;

    /**
     * Type of this tile. Multiple tiles can be associated with the same type.
     */
    @UserParameter(label = "Type", description = "type", order = 2)
    private transient String type;

    /**
     * Standard method call made to objects after they are deserialized. See:
     * http://java.sun.com/developer/JDCTechTips/2002/tt0205.html#tip2
     * http://xstream.codehaus.org/faq.html
     *
     * @return Initialized object.
     */
    private Object readResolve() {
        label = properties.containsKey("label") ? properties.get("label") : "Tile" + id;
        type = properties.containsKey("type") ?  properties.get("type:") : "" + id;
        return this;
    }

    /**
     * Construct a tile with a given local tileset id.
     * @param id the local tileset id of this tile
     */
    public Tile(int id) {
        this.id = id;
        this.label = "Tile " + id;
        this.type = "Type " + id;
    }

    public int getId() {
        return id;
    }
}
