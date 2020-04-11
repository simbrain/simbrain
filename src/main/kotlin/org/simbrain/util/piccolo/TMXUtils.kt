@file:JvmName("TMXUtils")

package org.simbrain.util.piccolo

import org.piccolo2d.PCanvas
import org.piccolo2d.PLayer
import org.piccolo2d.nodes.PPath
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.world.odorworld.OdorWorldResourceManager
import java.awt.*
import java.awt.event.MouseEvent
import java.awt.geom.Point2D
import java.io.File
import javax.swing.*
import javax.swing.border.MatteBorder
import javax.swing.border.TitledBorder


val zeroTile by lazy { Tile(0) }

val XStream get() = Utils.getSimbrainXStream()!!.apply { processAnnotations(TileMap::class.java) }

val missingTexture by lazy { OdorWorldResourceManager.getBufferedImage("tilemap/missing32x32.png") }

fun transparentTexture(width: Int, height: Int) = transparentImage(width, height)

/**
 * Create a tilemap by parsing a tmx file, which is an xml representation
 * of a tilemap.
 *
 * @param file file to parse
 * @return the tilemap object from the given file
 */
fun loadTileMap(file: File?): TileMap {
    return XStream.fromXML(file) as TileMap
}

/**
 * Create a tilemap by parsing a tmx file, which is an xml representation
 * of a tilemap.
 *
 * @param filename name of the file to parse
 * @return the tilemap object from the given file
 */
fun loadTileMap(filename: String): TileMap {
    return XStream.fromXML(OdorWorldResourceManager.getFileURL("tilemap" +
            File.separator + filename)) as TileMap
}

/**
 * Returns a dialog that is used to pick a tile image.
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
    fun PTiledImage.select(){
        // Remove any previous black rectangle
        selectionBoxRemover()
        addChild(PPath.createRectangle(-1.0, -1.0, 32.0, 32.0).apply {
            stroke = BasicStroke(2.0f)
            paint = null
        })
        selectionBoxRemover = { removeAllChildren() }
    }

    // Set content pane to a set of tabs, each showing a tileset
    contentPane = JTabbedPane().apply {
        this@tilePicker.forEach { tileSet ->
            // Add a new tab for each tileset
            addTab(tileSet.name, PCanvas().apply {
                layer.renderTileSet(tileSet)

                // Select the tile that was initially clicked on
                this.layer.allNodes.filterIsInstance<PTiledImage>().find { it.gid == pickedTile}?.let{
                    it.select()
                    //camera.centerBoundsOnPoint(it.bounds.centerX, it.bounds.centerY)
                    // TODO: Figure out what to center on what.
                }

                // Respond to clicks
                addInputEventListener { event, type ->
                    event.pickedNode.let {
                        if (it is PTiledImage && type == MouseEvent.MOUSE_CLICKED) {
                            it.select()
                            pickedTile = it.gid
                        } else if (it is PTiledImage && event.clickCount == 2) {
                            AnnotatedPropertyEditor.getDialog(tileSet[pickedTile]).apply {
                                pack()
                                isModal = true;
                                setLocationRelativeTo(null)
                                setVisible(true)
                            }
                        }
                    }
                }
            }.apply {
                preferredSize = Dimension(300,600) })
        }
    }

    addClosingTask { block(pickedTile) }
}

/**
 * Returns the main editor dialog.
 */
fun TileMap.editor(pixelCoordinate: Point2D) = StandardDialog().apply {

    val gbc = GridBagConstraints()

    val (x, y) = pixelCoordinate.toTileCoordinate()

    title = "Set tile(s) at ($x, $y)"

    preferredSize = Dimension(250,350)

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
                }.also { it.pack() }.present()
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
