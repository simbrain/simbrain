package org.simbrain.world.odorworld

import kotlinx.coroutines.delay
import org.piccolo2d.PCanvas
import org.piccolo2d.PLayer
import org.piccolo2d.event.PDragSequenceEventHandler
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.event.PInputEventListener
import org.piccolo2d.nodes.PPath
import org.piccolo2d.util.PPaintContext
import org.simbrain.util.*
import org.simbrain.util.piccolo.*
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import java.awt.*
import java.awt.event.MouseEvent
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import javax.swing.*
import javax.swing.table.DefaultTableModel
import kotlin.math.min


fun showTilePicker(tileSets: List<TileSet>, currentTileId: Int? = null, block: suspend (Int) -> Unit): StandardDialog {
    return tileSets.tilePicker(currentTileId ?: 1) {
        block(it)
    }.apply { makeVisible() }
}

/**
 * Returns a dialog that is used to pick a tile from a tilset. Double clicking edits the tile.
 */
fun List<TileSet>.tilePicker(currentGid: Int, block: suspend (Int) -> Unit) = StandardDialog().apply {

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
                setDefaultRenderQuality(PPaintContext.LOW_QUALITY_RENDERING)

                // Remove default event handlers
                val panEventHandler: PInputEventListener = panEventHandler
                val zoomEventHandler: PInputEventListener = zoomEventHandler
                removeInputEventListener(panEventHandler)
                removeInputEventListener(zoomEventHandler)

                fun setViewBounds(bounds: Rectangle2D) {
                    val (x, y, w, h) = bounds
                    val padding = 5.0
                    val width = (tileSet.columns * tileSet.tilewidth).toDouble()
                    val height = (tileSet.tilecount / tileSet.columns * tileSet.tileheight).toDouble()
                    val newWidth = min(w, width + padding)
                    val newHeight = min(h, height + padding)
                    val newX = x.coerceIn(-padding, width - newWidth)
                    val newY = y.coerceIn(-padding, height - newHeight)
                    camera.setViewBoundsNoOverflow(
                        Rectangle2D.Double(
                            newX,
                            newY,
                            newWidth,
                            newHeight
                        )
                    )
                }

                addInputEventListener(object : PDragSequenceEventHandler() {

                    override fun drag(event: PInputEvent) {
                        val (x, y, w, h) = event.camera.viewBounds
                        val dx = event.delta.width
                        val dy = event.delta.height
                        setViewBounds(Rectangle2D.Double(x - dx, y - dy, w, h))
                    }
                })

                layer.renderTileSet(tileSet)

                // Select the tile that was initially clicked on
                this.layer.allNodes.filterIsInstance<PTiledImage>().find { it.gid == pickedTile }?.let {
                    swingInvokeLater {
                        delay(1L) // to prevent intermittent transformation calculation error when the view is not fully rendered yet
                        it.select()
                        val centerX = it.globalBounds.centerX
                        val centerY = it.globalBounds.centerY
                        setViewBounds(
                            Rectangle2D.Double(
                                centerX - camera.viewBounds.width / 2,
                                centerY - camera.viewBounds.height / 2,
                                camera.viewBounds.width,
                                camera.viewBounds.height
                            )
                        )
                    }
                }

                // Respond to clicks
                addInputEventListener { event, type ->
                    event.pickedNode.let {
                        if (it is PTiledImage && type == MouseEvent.MOUSE_CLICKED) {
                            it.select()
                            pickedTile = it.gid
                        } else if (it is PTiledImage && event.clickCount == 2) {
                            AnnotatedPropertyEditor(tileSet[pickedTile]).displayInDialog()
                        }
                    }
                }
            })
        }
    }
    contentPane = tabbedPane
    addCommitTask { swingInvokeLater { block(pickedTile) } }
    preferredSize = Dimension(600, 600)
}

fun OdorWorld.layerEditor() = StandardDialog().apply {

    title = "Edit Layers"
    setSize(300, 400)

    val columnNames = arrayOf("Name", "Visible", "Blocking")
    val data = tileMap.layers.map { layer ->
        arrayOf(layer.name, layer.visible, layer.blocking)
    }.toTypedArray()

    val model = object : DefaultTableModel(data, columnNames) {
        override fun getColumnClass(column: Int): Class<*> {
            return getValueAt(0, column).javaClass
        }

        override fun getValueAt(row: Int, column: Int): Any {
            return when (column) {
                0 -> tileMap.layers[row].name
                1 -> tileMap.layers[row].visible
                2 -> tileMap.layers[row].blocking
                else -> throw IllegalArgumentException("Invalid column index")
            }
        }

        override fun setValueAt(aValue: Any?, row: Int, column: Int) {
            when (column) {
                0 -> tileMap.layers[row].name = aValue as String
                1 -> tileMap.layers[row].visible = aValue as Boolean
                2 -> tileMap.layers[row].blocking = aValue as Boolean
                else -> throw IllegalArgumentException("Invalid column index")
            }
        }

        override fun isCellEditable(row: Int, column: Int): Boolean {
            return true
        }
    }

    val table = JTable(model).apply {
        setRowSelectionAllowed(true)
        setShowGrid(true)
        intercellSpacing = Dimension(1, 1)
        gridColor = Theme.divider
    }
    val panel = JPanel(BorderLayout())
    panel.add(BorderLayout.CENTER,JScrollPane(table))
    panel.add(BorderLayout.SOUTH, JPanel().apply {
        add(JButton("Add", ResourceManager.getSmallIcon("menu_icons/plus.png")).apply {
            addActionListener {
                val newLayer = tileMap.createTileMapLayer("Layer ${tileMap.layers.size}")
                tileMap.addLayer(newLayer)
                model.addRow(arrayOf(newLayer.name, newLayer.visible))
            }
        })
        add(JButton("Remove", ResourceManager.getSmallIcon("menu_icons/minus.png")).apply {
            addActionListener {
                val selectedRowIndices = table.selectedRows.sortedDescending()
                val selectedLayers = tileMap.layers.filterIndexed { idx, _ -> idx in selectedRowIndices }
                selectedRowIndices.forEach { model.removeRow(it) }
                selectedLayers.forEach { tileMap.removeLayer(it) }
                model.fireTableDataChanged()
            }
        })
    })

    contentPane = panel


}

