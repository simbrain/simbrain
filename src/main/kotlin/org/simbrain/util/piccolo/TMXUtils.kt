@file:JvmName("TMXUtils")

package org.simbrain.util.piccolo

import org.piccolo2d.PCanvas
import org.piccolo2d.PLayer
import org.piccolo2d.nodes.PImage
import org.piccolo2d.nodes.PPath
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.OdorWorldResourceManager
import java.awt.*
import java.awt.event.MouseEvent
import java.awt.geom.Point2D
import java.io.File
import java.util.function.Consumer
import javax.swing.*
import javax.swing.border.MatteBorder
import javax.swing.border.TitledBorder
import kotlin.math.floor


val zeroTile by lazy { Tile(0) }

val missingTexture by lazy { OdorWorldResourceManager.getBufferedImage("tilemap/missing32x32.png") }

/**
 * Return id corresponding to a label or 0 (empty tile) if nothing is found
 */
fun Collection<TileSet>.getIdFromLabel(label: String): Int {
    return firstNotNullOf { tileSet -> tileSet[label] }.id ?: 0
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
    return OdorWorldComponent.getOdorWorldXStream().fromXML(file) as TileMap
}

/**
 * Create a tilemap by parsing a tmx file, which is an xml representation
 * of a tilemap.
 *
 * @param filename name of the file to parse
 * @return the tilemap object from the given file
 */
fun loadTileMap(filename: String): TileMap {
    return OdorWorldComponent.getOdorWorldXStream().fromXML(
        OdorWorldResourceManager.getFileURL("tilemap" + File.separator + filename)
    ) as TileMap
}

/**
 * Makes it possible to click on a PImage and get the global id associated with the underlying image.
 */
class PTiledImage(image: Image, val gid: Int) : PImage(image)

fun showTilePicker(tileSets: List<TileSet>, block: Consumer<Int>): StandardDialog {
    return tileSets.tilePicker(1) {
        block.accept(it)
    }.apply { makeVisible() }
}

/**
 * Returns a dialog that is used to pick a tile from a tilset. Double clicking edits the tile.
 */
fun List<TileSet>.tilePicker(currentGid: Int, block: (Int) -> Unit) = StandardDialog().apply {

    var pickedTile = currentGid
    title = "Pick / Edit Tile"

    /**
     * Make a PNode for every tile in a tileset and add to the canvas.
     */
    fun PLayer.renderTileSet(tileSet: TileSet) {
        with(tileSet) {
            (firstgid until firstgid + tilecount).map { PTiledImage(getTileImage(it), it) }
                .chunked(tileSet.columns)
                .mapIndexed { y, tiles ->
                    tiles.mapIndexed { x, image ->
                        image.let {
                            it.translate((x * tilewidth).toDouble(), (y * tileheight).toDouble())
                            this@renderTileSet.addChild(it)
                        }
                    }
                }
        }
    }

    // For selector
    var selectionBoxRemover: () -> Unit = { }

    /**
     * Black rectangle around tile
     */
    fun PTiledImage.select() {
        // Remove any previous black rectangle
        selectionBoxRemover()
        addChild(PPath.createRectangle(-1.0, -1.0, 32.0, 32.0).apply {
            stroke = BasicStroke(2.0f)
            paint = null
        })
        selectionBoxRemover = { removeAllChildren() }
    }

    // Set content pane to a set of tabs, each showing a tileset
    val tabbedPane = JTabbedPane().apply {
        this@tilePicker.forEach { tileSet ->
            // Add a new tab for each tileset
            addTab(tileSet.name, PCanvas().apply {
                layer.renderTileSet(tileSet)

                // Select the tile that was initially clicked on
                this.layer.allNodes.filterIsInstance<PTiledImage>().find { it.gid == pickedTile }?.let {
                    it.select()
                    // camera.centerBoundsOnPoint(it.bounds.centerX, it.bounds.centerY)
                    // TODO: Figure out what to center on what.
                }

                // Respond to clicks
                addInputEventListener { event, type ->
                    event.pickedNode.let {
                        if (it is PTiledImage && type == MouseEvent.MOUSE_CLICKED) {
                            it.select()
                            pickedTile = it.gid
                        } else if (it is PTiledImage && event.clickCount == 2) {
                            AnnotatedPropertyEditor.getDialog(tileSet[pickedTile]).makeVisible()
                        }
                    }
                }
            }.apply {
                preferredSize = Dimension(300, 600)
            })
        }
    }
    contentPane = ScrollPane().apply { add(tabbedPane) }
    addClosingTask { block(pickedTile) }
}


/**
 * Returns a dialog that shows the images in each layer at a point.
 */
fun TileMap.editor(pixelCoordinate: Point2D) = StandardDialog().apply {

    val gbc = GridBagConstraints()

    val (x, y) = pixelCoordinate.toTileCoordinate()

    title = "Set tile(s) at ($x, $y)"

    preferredSize = Dimension(250, 350)

    val mainPanel = JPanel().apply {
        layout = GridBagLayout()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        gbc.gridx = 0
    }

    class TilePanel(var onCommit: () -> Unit = { }) : JPanel()

    fun tilePanel(name: String, tile: Tile, change: (Int) -> Unit) = TilePanel().apply titlePanel@{

        border = TitledBorder(MatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY), name)

        val image = tileImage(tile.id)

        val tileViewer = JButton(ImageIcon(image)).apply button@{
            border = BorderFactory.createLoweredSoftBevelBorder()
            preferredSize = Dimension(image.getWidth(null) + 8, image.getHeight(null) + 8)
            isContentAreaFilled = false
            isFocusPainted = true
            onDoubleClick {
                tileSets.tilePicker(tile.id) {
                    this@button.icon = ImageIcon(tileImage(it))
                    onCommit = { change(it) }
                }.also { it.makeVisible() }
            }
        }

        layout = GroupLayout(this).apply {

            autoCreateGaps = true
            autoCreateContainerGaps = true

            setHorizontalGroup(
                createSequentialGroup()
                    .addComponent(tileViewer)
                //.addComponent(propertiesPanel)
            )
            setVerticalGroup(
                createParallelGroup(GroupLayout.Alignment.CENTER)
                    .addComponent(tileViewer)
                //.addComponent(propertiesPanel)
            )
        }

        add(tileViewer)

    }

    val panels = (layers zip getTileStackAt(x, y)).reversed()
        .map { (layer, tile) -> tilePanel(layer.name, tile) { layer.editTile(x, y, it) } }
        .onEach { mainPanel.add(it, gbc) }

    addClosingTask { panels.forEach { it.onCommit() } }

    contentPane = JScrollPane(mainPanel)

    pack()
    setLocationRelativeTo(null)
}

sealed class Coordinate(x: kotlin.Double, y: kotlin.Double): Point2D.Double(x, y)

class GridCoordinate(x: kotlin.Double, y: kotlin.Double): Coordinate(x, y) {
    fun copy() = GridCoordinate(x, y)
}
fun Point2D.asGridCoordinate() = GridCoordinate(x, y)

class PixelCoordinate(x: kotlin.Double, y: kotlin.Double): Coordinate(x, y) {
    fun copy() = PixelCoordinate(x, y)
}
fun Point2D.asPixelCoordinate() = PixelCoordinate(x, y)

context(TileMap)
fun PixelCoordinate.toGridCoordinate() = point(floor(x / tileWidth), floor(y / tileHeight)).asGridCoordinate()

context(TileMap)
fun GridCoordinate.toPixelCoordinate() = point((x + 0.5) * tileWidth, (y + 0.5) * tileHeight).asPixelCoordinate()

fun TileMap.getGridLocationsInRadius(staringLocation: PixelCoordinate, radiusInPixel: Double) = sequence {

    fun Point2D.isInRadius() = distanceSqTo(staringLocation) < radiusInPixel * radiusInPixel

    fun step(dx: Int, dy: Int) = sequence {
        val currentPoint = staringLocation.copy()
        currentPoint.setLocation(currentPoint.x + dx * tileWidth, currentPoint.y)
        while (currentPoint.isInRadius()) {
            while (currentPoint.isInRadius()) {
                val (x, y) = currentPoint
                yield(currentPoint.toGridCoordinate())
                currentPoint.setLocation(x + dx * tileWidth, y)
            }
            currentPoint.setLocation(staringLocation.x, currentPoint.y + dy * tileHeight)
        }
    }

    yield(staringLocation.asPixelCoordinate().toGridCoordinate())
    yieldAll(step(1, 1))
    yieldAll(step(-1, 1))
    yieldAll(step(-1, -1))
    yieldAll(step(1, -1))
}.toSet()

fun TileMap.getTileStackNear(location: Point2D, radius: Double = 10.0): List<Pair<GridCoordinate, List<Tile>>> {
    val a = getGridLocationsInRadius(location.asPixelCoordinate(), radius)
    val b = a.map { (x, y) ->
        GridCoordinate(x, y) to getTileStackAt(x.toInt(), y.toInt())
    }
    return b
}
