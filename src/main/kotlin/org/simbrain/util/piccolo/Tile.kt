package org.simbrain.util.piccolo

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import com.thoughtworks.xstream.annotations.XStreamConverter
import com.thoughtworks.xstream.converters.extended.NamedMapConverter
import org.simbrain.util.UserParameter
import org.simbrain.util.point
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.world.odorworld.entities.Bounded

/**
 * Simbrain wrapper for a [TileMap] tile.
 */
@XStreamAlias("tile")
class Tile(@XStreamAsAttribute @UserParameter(label = "ID", order = 0, displayOnly = true) val id: Int) : EditableObject {

    /**
     * Type of this tile. This way multiple tiles can be associated with the same type.
     */
    @Transient
    @UserParameter(label = "Type", description = "type", displayOnly = true, order = 20)
    var type: String = "Type $id"

    /**
     * Human readable name associated to some tile ids. It's up to the person making the tmx file to provide these.
     * Provides a more readable way to set tiles in scripts.
     */
    @Transient
    @UserParameter(label = "Tile label", description = "Name for tile", displayOnly = true, order = 30)
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
     * See {@link org.simbrain.workspace.serialization.WorkspaceComponentDeserializer}
     */
    private fun readResolve(): Any {
        type = properties["type"] ?: "$id"
        label = properties["label"]
        return this
    }

    override val name = "$id"
}

/**
 * A temporary data structure to associate a tile with location information during collision detection.
 */
context(TileMap)
class TileInstance(val tile: Tile, val gridCoordinate: GridCoordinate): Bounded {
    val pixelCoordinate = gridCoordinate.toPixelCoordinate()
    override val x by pixelCoordinate::x
    override val y by pixelCoordinate::y
    override val location = point(x, y)
    override val width = tileWidth.toDouble()
    override val height = tileHeight.toDouble()
}