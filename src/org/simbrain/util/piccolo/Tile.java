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

    //TODO: Currently just based on the xml id.
    // Can enhance this later with custom properties as in sample.tmx

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
