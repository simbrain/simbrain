package org.simbrain.util.piccolo

import com.Ostermiller.util.CSVParser
import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import com.thoughtworks.xstream.annotations.XStreamConverter
import com.thoughtworks.xstream.converters.MarshallingContext
import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.thoughtworks.xstream.converters.extended.NamedMapConverter
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import com.thoughtworks.xstream.mapper.Mapper
import org.piccolo2d.nodes.PImage
import org.simbrain.util.swingInvokeLater
import org.simbrain.world.odorworld.entities.Bounded
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream

@XStreamAlias("layer")
class TileMapLayer(
    @XStreamAsAttribute var name: String,
    @XStreamAsAttribute private var width: Int,
    @XStreamAsAttribute private var height: Int,
    blocking: Boolean
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

    @XStreamAsAttribute
    @XStreamAlias("visible")
    private var _visible: Boolean? = null

    var visible: Boolean
        get() = _visible ?: true
        set(value) {
            _visible = value
            layerImage?.let {
                swingInvokeLater {
                    it.visible = value
                }
            }
        }

    var boundsNeedRecompute: Boolean? = null

    val properties: HashMap<String, String?>
        get() = _properties ?: HashMap<String, String?>().also { _properties = it }

    /**
     * An intermediate data structure for persistence.
     */
    private var data: TileMapLayerData

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
                it.visible = visible
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
    var blocking: Boolean
        get() = properties["blocking"] == "true"
        set(value) {
            properties["blocking"] = if (value) "true" else "false"
            boundsNeedRecompute = true
        }

    /**
     * Get the id of a tile at the given tile coordinate location.
     *
     * @param x tile coordinate x
     * @param y tile coordinate y
     * @return the tile id
     */
    operator fun get(x: Int, y: Int): Int {
        infix fun Int.wrap(other: Int) = (this % other).let { if (it < 0) it + other else it }
        return data[x wrap width, y wrap height]
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
    operator fun set(x: Int, y: Int, tileID: Int) {
        data[x, y] = tileID
        boundsNeedRecompute = true
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
        data = TileMapLayerData(width, height)
    }

    fun setProperty(propertyName: String, propertyValue: String?) {
        properties[propertyName] = propertyValue
        boundsNeedRecompute = true
    }

    context(TileMap)
    fun getCollisionBounds(): List<Bounded> {
        return buildList {
            for (j in 0 until height) {
                for (i in 0 until width) {
                    val tileId = this@TileMapLayer[i, j]
                    if (tileId != 0) {
                        add(TileInstance(getTile(tileId), GridCoordinate(i, j)))
                    }
                }
            }
            boundsNeedRecompute = false
        }
    }

    init {
        properties["block"] = if (blocking) "true" else "false"
        data = TileMapLayerData(width, height)
        boundsNeedRecompute = true
    }

}

/**
 * Stores the grid of global ids that correspond to the data for a [TileMapLayer]
 */
@XStreamAlias("data")
class TileMapLayerData(val gidMatrix: MutableList<MutableList<Int>>) {
    constructor(width: Int, height: Int) : this(MutableList(height) { MutableList(width) { 0 } })

    operator fun get(x: Int, y: Int) = gidMatrix[y][x]
    operator fun set(x: Int, y: Int, tileId: Int) {
        gidMatrix[y][x] = tileId
    }
}

/**
 * Custom serializer using reflection converter, which allows constructor calls.
 */
class TiledDataConverter(mapper: Mapper, reflectionProvider: ReflectionProvider) :
    ReflectionConverter(mapper, reflectionProvider, TileMapLayerData::class.java) {

    override fun marshal(source: Any?, writer: HierarchicalStreamWriter, context: MarshallingContext) {
        val data = source as TileMapLayerData
        writer.addAttribute("encoding", "csv")
        val csv = data.gidMatrix.joinToString("\n") { it.joinToString(",") }
        writer.setValue(csv)
    }

    /**
     * Decode the raw [data] into a list of tile ids.
     *
     * @return the list of tile id this data represents.
     */
    private fun decodeData(value: String, encoding: String, compression: String?): List<List<Int>> {

        fun decodeCSV() =
            CSVParser(value.byteInputStream()).allValues
                .map { row ->
                    row.filter { it.isNotEmpty() }
                        .map { it.toInt() }
                }

        fun decodeBase64() = value.split("[ \n]".toRegex())
            .map { row -> row.let { Base64.getDecoder().decode(it)!! } }

        fun ByteArray.decompressGzip() = GZIPInputStream(ByteArrayInputStream(this))
            .use { it.readAllBytes()!! }

        fun ByteArray.decompressZlib() = InflaterInputStream(ByteArrayInputStream(this)).readAllBytes()!!

        fun ByteBuffer.asIntSequence() = sequence {
            while (hasRemaining()) {
                yield(int)
            }
        }

        return when (encoding) {
            "csv" -> decodeCSV()
            "base64" -> when (compression) {
                "gzip" -> decodeBase64().map { it.decompressGzip() }
                "zlib" -> decodeBase64().map { it.decompressZlib() }
                else -> decodeBase64()
            }.map { ByteBuffer.wrap(it).apply { order(ByteOrder.LITTLE_ENDIAN) }.asIntSequence().toList() }

            else -> throw IllegalStateException("Unknown encoding $encoding")
        }

    }

    override fun unmarshal(reader: HierarchicalStreamReader, context: UnmarshallingContext): Any {
        val encoding = reader.getAttribute("encoding")
        val compression = reader.getAttribute("compression")
        val gid = decodeData(reader.value, encoding, compression)
            .map { it.toMutableList() }
            .toMutableList()
        return TileMapLayerData(gid)
    }
}