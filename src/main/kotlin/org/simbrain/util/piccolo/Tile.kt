package org.simbrain.util.piccolo

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import com.thoughtworks.xstream.annotations.XStreamConverter
import com.thoughtworks.xstream.converters.extended.NamedMapConverter
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject

/**
 * Simbrain wrapper for a [TileMap] tile.
 */
@XStreamAlias("tile")
class Tile(@XStreamAsAttribute @UserParameter(label = "ID", order = 0, displayOnly = true) val id: Int) : EditableObject {

    /**
     * Type of this tile. This way multiple tiles can be associated with the same type.
     */
    @Transient
    @UserParameter(label = "Type", description = "type", order = 20)
    var type: String = "Type $id"

    /**
     * Human readable name associated to some tile ids. It's up to the person making the tmx file to provide these.
     */
    @Transient
    @UserParameter(label = "Tile label", description = "Name for tile", order = 30)
    var label: String? = null

    /**
     * Custom properties defined in tmx.
     */
    @XStreamConverter(
        value = NamedMapConverter::class,
        strings = ["property", "name", "value"],
        types = [String::class, String::class],
        booleans = [true, true]
    )
    val properties = HashMap<String, String>()

    /**
     * Type of this tile. Multiple tiles can be associated with the same type.
     */
    @Transient
    @UserParameter(label = "Collision", description = "If true, objects will collide with tiles that have this id", order = 30)
    var collision: Boolean = false

    /**
     * See {@link org.simbrain.workspace.serialization.WorkspaceComponentDeserializer}
     */
    private fun readResolve(): Any {
        type = properties["type"] ?: "$id"
        label = properties["label"]
        collision = properties["collision"]?.toBoolean() ?: false
        return this
    }

    override val name = "$id"
}