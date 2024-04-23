@file:JvmName("TMXUtils")

package org.simbrain.util.piccolo

import org.piccolo2d.nodes.PImage
import org.simbrain.util.*
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.OdorWorldResourceManager
import java.awt.Image
import java.awt.geom.Point2D
import java.io.File
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.random.Random

/**
 * Return gid corresponding to a label or 0 (empty tile) if nothing is found
 */
fun Collection<TileSet>.getGidFromLabel(label: String): Int {
    val result = firstNotNullOfOrNull { tileSet -> tileSet to tileSet[label] }
    if (result != null) {
        val (tileSet, tile) = result
        return (tile?.id ?: 0) + tileSet.firstgid
    }
    return 0
}

fun transparentTexture(width: Int, height: Int) = transparentImage(width, height)

/**
 * Create a tilemap by parsing a tmx file, which is an xml representation
 * of a tilemap.
 *
 * @param file file to parse
 * @return the tilemap object from the given file
 */
fun loadTileMap(file: File?): TileMap {
    return OdorWorldComponent.odorWorldXStream.fromXML(file) as TileMap
}

/**
 * Create a tilemap by parsing a tmx file, which is an xml representation
 * of a tilemap.
 *
 * @param filename name of the file to parse
 * @return the tilemap object from the given file
 */
fun loadTileMap(filename: String): TileMap {
    return OdorWorldComponent.odorWorldXStream.fromXML(
        OdorWorldResourceManager.getFileURL("tilemap" + File.separator + filename)
    ) as TileMap
}

/**
 * Makes it possible to click on a PImage and get the global id associated with the underlying image.
 */
class PTiledImage(image: Image, val gid: Int) : PImage(image)

val zeroTile by lazy { Tile(0) }

val missingTexture by lazy { OdorWorldResourceManager.getBufferedImage("tilemap/missing32x32.png") }


/**
 * Superclass for our x,y coordinate classes, which extends Point2D for backwards compatibility. Prevents unintentional
 * double conversions.
 */
sealed class Coordinate(x: kotlin.Double, y: kotlin.Double) : Point2D.Double(x, y) {
    data class IntCoordinate(val x: Int, val y: Int)
    val int get() = IntCoordinate(x.toInt(), y.toInt())
}

val Collection<Coordinate>.int get() = map { it.int }

/**
 * Coordinates in the tilemap. For example, in a 14x20 tile map, the first tile is (1,1). Stored as double for
 * backward compatibility with Point2D, but can be constructed with ints.
 */
class GridCoordinate(x: kotlin.Double, y: kotlin.Double) : Coordinate(x, y) {
    constructor(x: Int, y: Int): this(x.toDouble(), y.toDouble())
    fun copy() = GridCoordinate(x, y)
}

/**
 * Provides a random grid coordinate in the tile map.
 */
context (TileMap)
fun Random.nextGridCoordinate() = GridCoordinate(nextInt(width), nextInt(height))

/**
 * Casts a Point2D to grid coordinates.
 */
fun Point2D.asGridCoordinate() = GridCoordinate(x, y)

/**
 * Coordinates in the pixel space. For example, a 14x14 tile map this might be 300x300 pixels.
 */
class PixelCoordinate(x: kotlin.Double, y: kotlin.Double) : Coordinate(x, y) {
    constructor(x: Int, y: Int): this(x.toDouble(), y.toDouble())
    fun copy() = PixelCoordinate(x, y)
}

/**
 * Casts Point2D to pixel coordinates.
 */
fun Point2D.asPixelCoordinate() = PixelCoordinate(x, y)

/**
 * Convert pixel coordinates to grid coordinates.
 */
context(TileMap)
fun PixelCoordinate.toGridCoordinate() = point(floor(x / tileWidth), floor(y / tileHeight)).asGridCoordinate()

/**
 * Convert grid coordinates to pixel coordinates.
 */
context(TileMap)
fun GridCoordinate.toPixelCoordinate() = point((x + 0.5) * tileWidth, (y + 0.5) * tileHeight).asPixelCoordinate()

/**
 * Creates a circle of the provided radius around the center of the first grid coordinate, checks to see which tile
 * centers are contained in that circle, and returns a list of the grid coordinates of all those tiles.
 *
 * Uses relative coordinates so that this list can be computed once and then superimposed on other locations.
 */
fun TileMap.getRelativeGridLocationsInRadius(radiusInPixel: Double) = sequence {

    val origin = GridCoordinate(0, 0).toPixelCoordinate()
    fun Point2D.isInRadius() = this distanceSqTo origin < radiusInPixel * radiusInPixel

    // Create a "search area" that roughly inscribes the circle with the provided radius
    val maxX = ceil(radiusInPixel / tileWidth / 2).toInt()
    val maxY = ceil(radiusInPixel / tileHeight / 2).toInt()

    for (j in -maxY .. maxY) {
        for (i in -maxX .. maxX) {
            val gridCoordinate = point(i, j).asGridCoordinate()
            if (gridCoordinate.toPixelCoordinate().isInRadius()) {
                yield(gridCoordinate)
            }
        }
    }

}

context (TileMap)
val GridCoordinate.isInMap get() = x.toInt() in 0 until width && y.toInt() in 0 until height

fun TileMap.createTileMapLayer(name: String, collision: Boolean = false) = TileMapLayer(name, width, height, collision)

fun TileMap.getCoordinates(topLeftLocation: GridCoordinate, width: Int, height: Int): List<GridCoordinate> {
    val (x, y) = topLeftLocation.int
    return getCoordinates(x, y, width, height)
}

fun TileMap.getCoordinates(from: GridCoordinate, to: GridCoordinate): List<GridCoordinate> {
    val (x, y) = listOf(from, to).reduce { acc, gridCoordinate ->
        val (ax, ay) = acc
        val (gx, gy) = gridCoordinate
        GridCoordinate(min(ax, gx), min(ay, gy))
    }.int
    val (w, h) = (to - from).abs.int
    return getCoordinates(x, y, w, h)
}

fun TileMap.getCoordinates(x: Int, y: Int, width: Int, height: Int): List<GridCoordinate> {
    return (0 until height).flatMap { j ->
        (0 until width).mapNotNull { i ->
            GridCoordinate(x + i, y + j).let { if (it.isInMap) it else null }
        }
    }
}


context (TileMap)
fun getTile(label: String) = tileSets.firstNotNullOfOrNull { tileSet -> tileSet[label] }

/**
 * Make a lake of the indicated size (in # of tiles) starting at the top left grid location.
 * If two lakes are put together, they are merged into one with shared edges removed.
 */
fun TileMap.makeLake(topLeftLocation: GridCoordinate, width: Int, height: Int, layer: TileMapLayer = layers.first()) {

    /**
     * Compute edge type string based on surround
     */
    fun computeShape(surroundingLandBits: Int): String {
        val tl = 1 shl 0
        val tc = 1 shl 1
        val tr = 1 shl 2
        val cl = 1 shl 3
        val cc = 1 shl 4
        val cr = 1 shl 5
        val bl = 1 shl 6
        val bc = 1 shl 7
        val br = 1 shl 8

        if (surroundingLandBits and cc > 0) return "land"

        if (surroundingLandBits and (tc or cl or cr or bc) == 0) {
            if (surroundingLandBits and tl > 0) return "inner_br"
            if (surroundingLandBits and tr > 0) return "inner_bl"
            if (surroundingLandBits and bl > 0) return "inner_tr"
            if (surroundingLandBits and br > 0) return "inner_tl"
        }
        var dir = ""
        if (surroundingLandBits and bc > 0) dir += "t"
        if (surroundingLandBits and tc > 0) dir += "b"
        if (surroundingLandBits and cr > 0) dir += "l"
        if (surroundingLandBits and cl > 0) dir += "r"

        return if (dir.length > 2) return "" else dir
    }

    val lakeCoordinates = getCoordinates(topLeftLocation, width, height)

    // make a basic lake
    lakeCoordinates.forEach {
        val p = it.int
        setTile(p.x, p.y,2, layer)
    }

    // replace edges and corners
    lakeCoordinates.forEach { p ->
        val isLandList = (-1..1).flatMap { ly ->
            (-1..1).map { lx ->
                val lp = (p + point(lx, ly)).asGridCoordinate()
                if (lp.isInMap) {
                    val (lpx, lpy) = lp.int
                    val lpTileId = layer[lpx, lpy]
                    val tileType = getTile(lpTileId).type
                    if (tileType == "water") 0 else 1
                } else {
                    1
                }
            }
        }
        val surroundingLandBits = isLandList.reduce { acc, i -> (acc shl 1) + i }
        val shape = computeShape(surroundingLandBits)
        setTile(
            p.x.toInt(), p.y.toInt(),
            listOf("water", shape).filter { it.isNotEmpty() }.joinToString("_"),
            layer,
            suppressMissingNames = listOf("water", "water_lr", "water_tb")
        )
    }

}