package org.simbrain.util.piccolo

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import com.thoughtworks.xstream.annotations.XStreamConverter
import com.thoughtworks.xstream.converters.extended.NamedMapConverter
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import java.util.*

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
 * Simbrain wrapper for a [TileMap] tile.
 */
@XStreamAlias("tile")
class Tile(@XStreamAsAttribute @UserParameter(label = "ID", order = 0, editable = false) var id: Int) : EditableObject {

    /**
     * Custom properties defined in tmx.
     */
    @XStreamConverter(
            value = NamedMapConverter::class,
            strings = ["property", "name", "value"],
            types = [String::class, String::class],
            booleans = [true, true]
    )
    private val properties = HashMap<String, String>()

    /**
     * Type of this tile. This way multiple tiles can be associated with the same type.
     */
    @Transient
    @UserParameter(label = "Type", description = "type", order = 20)
    private var type: String = "Type $id"

    /**
     * Type of this tile. Multiple tiles can be associated with the same type.
     */
    @Transient
    @UserParameter(label = "Collision", description = "If true, objects will collide with tiles that have this id", order = 30)
    var collision: Boolean = false

    /**
     * Standard method call made to objects after they are deserialized. See:
     * http://java.sun.com/developer/JDCTechTips/2002/tt0205.html#tip2
     * http://xstream.codehaus.org/faq.html
     *
     * @return Initialized object.
     */
    private fun readResolve(): Any {
        type = properties["type"] ?: "$id"
        collision = properties["collision"]?.toBoolean() ?: false
        return this
    }

    override fun getName(): String {
        return "$id"
    }
}