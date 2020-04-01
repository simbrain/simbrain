@file:JvmName("TMXUtils")

package org.simbrain.util.piccolo

import org.piccolo2d.PCanvas
import org.piccolo2d.PLayer
import org.piccolo2d.nodes.PPath
import org.simbrain.util.StandardDialog
import org.simbrain.util.Utils
import org.simbrain.util.present
import org.simbrain.util.transparentImage
import org.simbrain.world.odorworld.OdorWorldResourceManager
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Point2D
import java.io.File
import javax.swing.*
import javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
import javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
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

    var pickedTiled = currentGid
    title = "Pick / Edit Tile"

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

    var remover: () -> Unit = { }

    contentPane = JTabbedPane().apply {
        this@tilePicker.forEach { tileSet ->
            addTab(tileSet.name, PCanvas().apply {
                layer.renderTileSet(tileSet)
                addInputEventListener { event, type ->
                    event.pickedNode.let {
                        if (it is PTiledImage && type == MouseEvent.MOUSE_CLICKED) {
                            remover()
                            it.addChild(PPath.createRectangle(-1.0, -1.0, 32.0, 32.0).apply {
                                stroke = BasicStroke(2.0f)
                                paint = null
                            })
                            remover = { it.removeAllChildren() }
                            block(it.gid)
                        }
                    }
                }
            }.let {
                JScrollPane(it, VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_NEVER)
                        .apply { preferredSize = Dimension(320, 640) }
            })
        }
    }
}

/**
 * Returns the main editor dialog.
 */
fun TileMap.editor(pixelCoordinate: Point2D) = StandardDialog().apply {

    BoxLayout(this, BoxLayout.LINE_AXIS)

    title = "Set tile(s) at (${pixelCoordinate.x}, ${pixelCoordinate.y})"

    val mainPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }

    fun tilePanel(name: String, tile: Tile) = JPanel(SpringLayout()).apply titlePanel@{

        border = TitledBorder(MatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY), name)

        background = Color.red;

        val image = tileImage(tile.id)

        val tileViewer = JButton(ImageIcon(image)).apply button@{
            border = BorderFactory.createLoweredSoftBevelBorder()
            preferredSize = Dimension(image.getWidth(null) + 8, image.getHeight(null) + 8)
            isContentAreaFilled = false
            isFocusPainted = true
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    if (e?.clickCount == 2) {
                        tileSets.tilePicker(tile.id) {
                            this@button.icon = ImageIcon(tileImage(it))
                        }.also { it.pack() }.present()
                    }
                }
            })
        }

        val propertiesPanel = JPanel().apply propertiesPanel@{
            val labelLabel = JLabel("Label")
            val typeLabel = JLabel("Type")

            val labelField = JTextField().apply { preferredSize = Dimension(240, 24) }
            val typeField = JTextField().apply { preferredSize = Dimension(240, 24) }

            //val labelInherit = JCheckBox("inherit?")
            //val typeInherit = JCheckBox("inherit?")

            add(labelLabel)
            add(labelField)
            //add(labelInherit)
            add(typeLabel)
            add(typeField)
            //add(typeInherit)

            layout = GroupLayout(this).apply {
                autoCreateGaps = true

                setHorizontalGroup(
                        createSequentialGroup()
                                .addGroup(
                                        createParallelGroup()
                                                .addComponent(labelLabel)
                                                .addComponent(typeLabel)
                                )
                                .addGroup(
                                        createParallelGroup()
                                                .addComponent(labelField)
                                                .addComponent(typeField)
                                )
                                //.addGroup(
                                //        createParallelGroup()
                                //                .addComponent(labelInherit)
                                //                .addComponent(typeInherit)
                                //)
                )
                setVerticalGroup(
                        createSequentialGroup()
                                .addGroup(createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(labelLabel)
                                        .addComponent(labelField)
                                        //.addComponent(labelInherit)
                                )
                                .addGroup(createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(typeLabel)
                                        .addComponent(typeField)
                                        //.addComponent(typeInherit)
                                )
                )
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
        //add(propertiesPanel)
    }

    (layers.map { it.name } zip getTileStackAtPixel(pixelCoordinate)).reversed().forEach { (name, tile) ->
        mainPanel.add(tilePanel(name, tile))
    }

    contentPane = mainPanel

    pack()
}
