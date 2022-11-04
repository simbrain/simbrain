package org.simbrain.util.piccolo

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import com.thoughtworks.xstream.annotations.XStreamImplicit
import java.awt.Image

/**
 * A set of tiles based on an underlying image, e.g. . See https://en.wikipedia.org/wiki/Tile-based_video_game
 *
 * As an example see tileset_kenney.png.
 *
 * Tiles are accessed using a global id or "gid". 0 is reserved for an empty slot, and the index can span multiple
 * tilesets. Hence the [#firstgid] is set to 1 for the first tileset, and to another nubmer for subsequent tileset.
 */
@XStreamAlias("tileset")
class TileSet(

    /**
     * Path to image containing the tile map image.
     */
    @XStreamAsAttribute
    private val source: String,

    /**
     * The spacing in pixels between the tiles in this tileset (applies to the tileset image).
     */
    @XStreamAsAttribute
    private val spacing: Int = 0,

    /**
     * The number of tiles in this tileset (since 0.13)
     */
    @XStreamAsAttribute
    val tilecount: Int = 0,

    /**
     * The number of tile columns in the tileset. For image collection tilesets it is editable and is used when displaying the tileset. (since 0.15)
     */
    @XStreamAsAttribute
    val columns: Int = 1

) {

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
     * The margin around the tiles in this tileset (applies to the tileset image).
     */
    @XStreamAsAttribute
    private val margin = 0

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
    private var image: TiledImage = TiledImage(source)

    /**
     * List of tiles explicitly defined in the tmx/tsx. This is used only when parsing.
     * It is complicated to directly parse the tile info into a map, so first the tiles are store in this list,
     * and later when the tiles are access, they will be store into the [.idTileMap].
     */
    @XStreamImplicit
    private var tiles: MutableList<Tile> = mutableListOf()

    /**
     * A map from tile ids to tiles for fast lookup.
     */
    @Transient
    private lateinit var idTileMap: MutableMap<Int, Tile>

    /**
     * A map from tile string labels to tiles.
     */
    // TODO: Directly editing labels on tiles in APE does not update this map
    @Transient
    private lateinit var labelTileMap: MutableMap<String, Tile>

    init {
        initTileMaps()
    }

    fun initTileMaps() {
        idTileMap = tiles.associateBy { it.id }.toMutableMap()
        labelTileMap = tiles.filter { it.label != null }.associateBy { it.label!! }.toMutableMap()
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
        return idTileMap[localId] ?: Tile(localId).also { idTileMap[localId] = it }
    }

    /**
     * Get tile associated with a string label.
     */
    operator fun get(label: String): Tile? {
        return labelTileMap[label]
    }

    /**
     * See {@link org.simbrain.workspace.serialization.WorkspaceComponentDeserializer}
     */
    private fun readResolve(): Any {
        if (tiles == null) {
            tiles = mutableListOf()
        }
        initTileMaps()
        return this
    }
}

/**
 * Create tileset with parameters for the default TiledImage.
 */
fun createDefaultTileSet(): TileSet {
    return TileSet("tileset_kenney.png", 2, 1767, 57)
}
