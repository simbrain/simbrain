package org.simbrain.util.piccolo

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import com.thoughtworks.xstream.annotations.XStreamConverter
import com.thoughtworks.xstream.converters.extended.NamedMapConverter
import org.piccolo2d.nodes.PImage
import java.awt.RenderingHints
import java.awt.image.BufferedImage

@XStreamAlias("layer")
class TileMapLayer(
        @XStreamAsAttribute var name: String,
        @XStreamAsAttribute private var width: Int,
        @XStreamAsAttribute private var height: Int,
        collision: Boolean
) {

    /**
     * Custom properties defined in tmx.
     */
    @XStreamConverter(
            value = NamedMapConverter::class,
            strings = ["property", "name", "value"],
            types = [String::class, String::class],
            booleans = [true, true]
    )
    @XStreamAlias("properties")
    private var _properties: HashMap<String, String?>? = null

    val properties: HashMap<String, String?>
        get() = _properties ?: HashMap<String, String?>().also { _properties = it }

    /**
     * The data from parsing tmx file.
     * Use [TiledData.getGid] to retrieve the processed data (tile id list)
     */
    private var data: TiledData

    /**
     * The rendered image of the layer
     */
    @Transient
    var layerImage: PImage? = null
        private set

    /**
     * Render one layer of a tileset.
     *
     * @param tileSets the tileset to use on this layer
     * @return the image of this layer
     */
    @JvmOverloads
    fun renderImage(tileSets: List<TileSet>, forced: Boolean = false): PImage {
        if (layerImage == null || forced) {

            val tileWidth = if (tileSets.isNotEmpty()) tileSets[0].tilewidth else 32
            val tileHeight = if (tileSets.isNotEmpty()) tileSets[0].tileheight else 32
            val layerImage = BufferedImage(width * tileWidth, height * tileHeight, BufferedImage.TYPE_INT_ARGB)
            with(layerImage.createGraphics()) {
                setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED)
                for (j in 0 until height) {
                    for (i in 0 until width) {
                        val image = tileSets[0].getTileImage(this@TileMapLayer[i, j])
                        drawImage(image, i * tileWidth, j * tileHeight, null)
                    }
                }
                dispose()
            }
            return PImage(layerImage).also {
                it.pickable = false
                this.layerImage = it
            }
        } else {
            return layerImage!!
        }
    }

    /**
     * Return true if the tiles on this layer are set to have collision bounds.
     *
     * @return true if tiles are blocking
     */
    val collision: Boolean
        get() = properties["collide"] == "true"

    /**
     * Get the id of a tile at the given tile coordinate location.
     *
     * @param x tile coordinate x
     * @param y tile coordinate y
     * @return the tile id
     */
    operator fun get(x: Int, y: Int): Int {
        infix fun Int.wrap(other: Int) = (this + other) % other
        return data.gid[(x wrap width) + (y wrap height) * width]
    }

    /**
     * Modify a tile id at a given location.
     * NOTE: This does NOT update of the tile map layer image. To update the image after changing tile id, use
     * [.renderImage] and update the corresponding PImage in TileMap#renderedLayers
     *
     * @param tileID the new tile id
     * @param x the x coordinate on map
     * @param y the y coordinate on map
     */
    operator fun set(tileID: Int, x: Int, y: Int) {
        data.gid[x + y * width] = tileID
    }

    /**
     * Clear all tiles on this layer and set the layer to the specified size.
     *
     * @param width the new width
     * @param height the new height
     */
    /**
     * Clear all tiles on this layer.
     */
    @JvmOverloads
    fun clear(width: Int = this.width, height: Int = this.height) {
        this.width = width
        this.height = height
        data = TiledData(width, height)
    }

    fun setProperty(propertyName: String, propertyValue: String?) {
        properties[propertyName] = propertyValue
    }

    init {
        properties["collide"] = if (collision) "true" else "false"
        data = TiledData(width, height)
    }

}