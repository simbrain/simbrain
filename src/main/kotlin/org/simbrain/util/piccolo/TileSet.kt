package org.simbrain.util.piccolo

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import com.thoughtworks.xstream.annotations.XStreamImplicit
import java.awt.Image

@XStreamAlias("tileset")
class TileSet {

    /**
     * The first global tile ID of this tileset (this global ID maps to the first tile in this tileset).
     */
    @XStreamAsAttribute
    val firstgid = 1

    /**
     * The name of this tileset.
     */
    @XStreamAsAttribute
    val name: String = "(no name)"

    /**
     * The (maximum) width of the tiles in this tileset.
     */
    @XStreamAsAttribute
    val tilewidth = 32

    /**
     * The (maximum) height of the tiles in this tileset.
     */
    @XStreamAsAttribute
    val tileheight = 32

    /**
     * The spacing in pixels between the tiles in this tileset (applies to the tileset image).
     */
    @XStreamAsAttribute
    private val spacing = 0

    /**
     * The margin around the tiles in this tileset (applies to the tileset image).
     */
    @XStreamAsAttribute
    private val margin = 0

    /**
     * The number of tiles in this tileset (since 0.13)
     */
    @XStreamAsAttribute
    val tilecount = 0

    /**
     * The number of tile columns in the tileset. For image collection tilesets it is editable and is used when displaying the tileset. (since 0.15)
     */
    @XStreamAsAttribute
    val columns = 1

    /**
     * Horizontal offset in pixels
     */
    @XStreamAsAttribute
    private val offsetX = 0

    /**
     * Vertical offset in pixels (positive is down)
     */
    @XStreamAsAttribute
    private val offsetY = 0

    /**
     * The tileset image
     */
    private lateinit var image: TiledImage

    /**
     * List of tile explicitly defined in the tmx/tsx. This is used only when parsing.
     * It is complicated to directly parse the tile info into a map, so first the tiles are store in this list,
     * and later when the tiles are access, they will be store into the [.idTileMap].
     */
    @XStreamImplicit
    private val tiles: MutableList<Tile>? = null

    /**
     * A map of tile id to tile for fast lookup.
     */
    @Transient
    private lateinit var idTileMap: MutableMap<Int, Tile?>

    init {
        init()
    }

    fun init() {
        idTileMap = HashMap()
    }

    /**
     * Get the corresponding tile image from the tileset.
     * @param gid index of the tile
     * @return the image of the tile
     */
    fun getTileImage(gid: Int): Image {
        val index = gid - firstgid
        return if (index !in 0..tilecount) {
            transparentTexture(tilewidth, tileheight)
        } else {
            image.image!!.getSubimage(
                    index % columns * (tilewidth + spacing),
                    index / columns * (tileheight + spacing),
                    tilewidth,
                    tileheight
            )
        }
    }

    /**
     * Get tile of a given id.
     *
     * @param gid the global id of the tile
     * @return the tile of the given id.
     */
    operator fun get(gid: Int): Tile {
        val localId = gid - firstgid
        return idTileMap[localId] ?: cacheNewTile(gid)
    }

    private fun cacheNewTile(localId: Int): Tile = when (localId) {
        in 0..tilecount -> (idTileMap[localId] ?: Tile(localId).also { idTileMap[localId] = it })
        else -> throw IllegalArgumentException("Tile set has only $tilecount tiles, but requesting tile $localId.")
    }

    /**
     * See {@link org.simbrain.workspace.serialization.WorkspaceComponentDeserializer}
     */
    private fun readResolve(): Any {
        init()
        return this
    }
}

