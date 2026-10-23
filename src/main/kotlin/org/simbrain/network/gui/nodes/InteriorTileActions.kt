/**
 * Tile-level actions inside the language model nodes: the right-click menu on an interior tile
 * (weight edits, plots and recordings, the data-flow trace, the tile dialog), the tile dialog
 * itself (also opened by double-clicking a tile, like a neuron array's pixels), and the routing
 * that sends the network panel's randomize, up, down, and clear keys to selected weight tiles, the
 * way pixel selections take them on neuron arrays and weight matrices. The models own the edits
 * and probes ([GenerativeModel]); this file only presents them.
 */
package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.piccolo2d.event.PInputEvent
import org.simbrain.network.compositor.*
import org.simbrain.network.gui.MouseEventUtils
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.llm.GenerativeModel
import org.simbrain.network.llm.LanguageModel
import org.simbrain.network.llm.TileProbe
import org.simbrain.network.llm.WeightEdit
import org.simbrain.plot.projection.ProjectionComponent
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.table.MatrixDataFrame
import org.simbrain.util.table.SimbrainTablePanel
import org.simbrain.util.table.addSimpleDefaults
import org.simbrain.util.table.createShowEigenValuesAction
import org.simbrain.workspace.couplings.getConsumer
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.workspace.gui.SimbrainDesktop.actionManager
import smile.math.matrix.Matrix
import javax.swing.JCheckBoxMenuItem
import javax.swing.JOptionPane
import javax.swing.JPopupMenu
import javax.swing.JTabbedPane
import javax.swing.SwingUtilities
import kotlin.math.sqrt

/** A canvas node whose interior is a compositor scene of a [GenerativeModel]. */
interface InteriorTileHost {
    val generativeModel: GenerativeModel
    val interior: CompositorNode?
}

private const val LFM_EDIT_WARNING = "Edited weights live in memory only. Saving keeps the path to the " +
    "weights file, not your edits, so reopening starts from the original weights; " +
    "\"Restore original weights\" also brings them back.\n\n" +
    "Each edit re-runs the context window from its first token, since everything computed so far used " +
    "the old weights."

private val NetworkPanel.tileHosts: List<InteriorTileHost>
    get() = screenElements.filterIsInstance<InteriorTileHost>()

private fun InteriorTileHost.selectedWeightTiles(): List<TensorTile> =
    interior?.selectedTiles.orEmpty().filter { it.kind == TileKind.WEIGHT && it is MatrixTile }

/** True when any interior has weight tiles selected; they then take the edit keys instead of the canvas selection. */
fun NetworkPanel.hasAnyWeightTileSelection(): Boolean = tileHosts.any { it.selectedWeightTiles().isNotEmpty() }

fun NetworkPanel.editSelectedWeightTiles(edit: WeightEdit) {
    tileHosts.forEach { host ->
        val tiles = host.selectedWeightTiles()
        if (tiles.isNotEmpty()) editTileWeights(host.generativeModel, tiles, edit)
    }
}

/**
 * Keeps the canvas selection and an interior selection in step: selecting tiles selects their
 * node, so the network panel's edit actions are enabled, and a canvas selection that drops the
 * node drops its tiles, so stale tiles never capture the keys.
 */
fun NetworkPanel.syncInteriorSelection(host: InteriorTileHost) {
    val node = host as? ScreenElement ?: return
    if (host.interior?.selectedTiles.isNullOrEmpty()) return
    if (node !in selectionManager.selection) selectionManager.set(node)
}

fun NetworkPanel.dropInteriorSelectionIfDeselected(host: InteriorTileHost, selection: Set<ScreenElement>) {
    if ((host as? ScreenElement) !in selection) host.interior?.clearSelection()
}

/** An LFM's first edit since its weights loaded is confirmed; edits afterwards go straight through. */
private fun confirmWeightEdit(model: GenerativeModel): Boolean =
    model !is LanguageModel || model.hasEditedWeights ||
        showWarningConfirmDialog(LFM_EDIT_WARNING) == JOptionPane.OK_OPTION

fun NetworkPanel.editTileWeights(model: GenerativeModel, tiles: List<TensorTile>, edit: WeightEdit) {
    if (!confirmWeightEdit(model)) return
    network.launch(Dispatchers.Default) { model.editWeights(tiles, edit) }
}

fun NetworkPanel.showTileMenu(host: InteriorTileHost, tile: TensorTile, event: PInputEvent) {
    val menu = createTileMenu(host, tile)
    MouseEventUtils.applyContextMenuFixes(this, event, menu)
    val (x, y) = event.canvasPosition.int
    menu.show(canvas, x, y)
}

fun NetworkPanel.createTileMenu(host: InteriorTileHost, tile: TensorTile) = JPopupMenu().apply {
    val model = host.generativeModel
    if (tile.kind == TileKind.WEIGHT && tile is MatrixTile) {
        val tiles = host.selectedWeightTiles().ifEmpty { listOf(tile) }
        val what = if (tiles.size == 1) "weights" else "weights of ${tiles.size} tiles"
        fun edit(name: String, description: String, edit: WeightEdit) =
            add(createAction(name = name, description = description) { editTileWeights(model, tiles, edit) })
        edit("Clear $what", "Set every weight to zero (Shift-C)", WeightEdit.CLEAR)
        edit("Randomize $what", randomizeDescription(model), WeightEdit.RANDOMIZE)
        edit("Increase $what", "Add the tile's increment to every weight (up arrow)", WeightEdit.INCREMENT)
        edit("Decrease $what", "Subtract the tile's increment from every weight (down arrow)", WeightEdit.DECREMENT)
        if (model is LanguageModel && model.hasEditedWeights) {
            add(createAction(
                name = "Restore original weights",
                description = "Re-read every edited weight matrix from the weights file",
            ) { network.launch(Dispatchers.Default) { model.restoreWeights() } })
        }
        addSeparator()
    }
    if (tile.isReadable) {
        pruneUncoupledProbes(model)
        val probe = model.probeFor(tile)
        val producer = probe.getProducer(TileProbe::values)
        val plotMenu = actionManager.createCoupledPlotMenu(producer, objectName = probe.attributeName, menuTitle = "Plot ${pinText(tile)}")
        if (model.hasLensReading(tile)) {
            plotMenu.addSeparator()
            plotMenu.add(createLensLabeledProjectionAction(probe))
        }
        add(plotMenu)
        add(actionManager.createCoupledDataWorldAction(
            name = "Record ${pinText(tile)}",
            producer = producer,
            sourceName = probe.attributeName,
            numCols = probe.values.size,
        ))
        addSeparator()
    }
    add(JCheckBoxMenuItem("Trace data flow", host.interior?.traceFocus == tile).apply {
        toolTipText = "Highlight every path data takes into and out of this tile"
        addActionListener { host.interior?.setTrace(if (isSelected) tile else null) }
    })
    add(createAction(
        name = "Edit ${tile.displayTitle}...",
        description = "Rename the tile and view or edit its values (double-click)",
    ) { createTileDialog(host, tile).display() })
}

/** Double-clicking a tile opens its dialog, the way double-clicking a pixel opens the pixel dialog. */
fun NetworkPanel.openTileDialog(host: InteriorTileHost, tile: TensorTile) {
    SwingUtilities.invokeLater { createTileDialog(host, tile).display() }
}

/** "Clear trace" for a node's whole-model menu, present only while a trace is showing. */
fun JPopupMenu.addClearTraceItem(host: InteriorTileHost) {
    val interior = host.interior ?: return
    if (interior.traceFocus == null) return
    add(createAction(
        name = "Clear trace",
        description = "Remove the data-flow highlight from the interior",
    ) { interior.setTrace(null) })
}

private fun randomizeDescription(model: GenerativeModel) = when (model) {
    is LanguageModel -> "Redraw every weight from the network's weight randomizer (r)"
    else -> "Redraw every weight with the model's weight initialization (r)"
}

/** The layer and head the tile shows now, e.g. "layer 3, head 2", or null for a tile with neither. */
private fun pinDescription(tile: TensorTile): String? = listOfNotNull(
    tile.pinnableLayer.takeIf { it >= 0 }?.let { "layer $it" },
    tile.pinnableHead.takeIf { it >= 0 }?.let { "head $it" },
).takeIf { it.isNotEmpty() }?.joinToString()

/**
 * The tile's name plus what a plot made now pins to, e.g. "+ mlp (block out), layer 13", so the
 * menu says which tile and which layer the plot will keep reading.
 */
private fun pinText(tile: TensorTile): String =
    listOfNotNull(tile.displayTitle, pinDescription(tile)).joinToString()

/**
 * A projection plot whose points carry the logit lens's prediction as their label. Two
 * couplings: the checkpoint values add a point, then the lens token labels the point just added.
 * Couplings update in creation order, so the label coupling is made second.
 */
private fun NetworkPanel.createLensLabeledProjectionAction(probe: TileProbe) = createAction(
    name = "Projection plot labeled by logit lens",
    description = "Each point is labeled with the token the logit lens predicts from this checkpoint; " +
        "the logit lens must be on",
    iconPath = "menu_icons/CubeShadow.png",
) {
    val workspace = networkComponent.workspace
    val plot = ProjectionComponent("Projection plot of ${probe.attributeName}")
    workspace.addWorkspaceComponent(plot)
    with(workspace.couplingManager) {
        probe.getProducer(TileProbe::values) couple plot.getConsumer("addPoint")
        probe.getProducer(TileProbe::lensToken) couple plot.getConsumer(ProjectionComponent::setLabel)
    }
}

/** Menus make a probe before the user picks anything, so probes nobody coupled to are dropped here instead of saved. */
private fun NetworkPanel.pruneUncoupledProbes(model: GenerativeModel) {
    val coupled = networkComponent.workspace.couplingManager.couplings
        .flatMapTo(HashSet()) { listOf(it.producer.baseObject, it.consumer.baseObject) }
    model.tileProbes.removeAll { it !in coupled }
}

internal class TileProperties(tile: TensorTile, increment: Double, summary: String) : EditableObject {

    var isWeight = tile.kind == TileKind.WEIGHT

    var isPinnable = pinDescription(tile) != null

    var label by GuiEditable(
        initValue = tile.displayTitle,
        label = "Label",
        description = "Name shown under the tile; clear it to restore the original name (${tile.title})",
        order = 1,
    )

    var shape by GuiEditable(
        initValue = tile.tooltipShape,
        label = "Shape",
        displayOnly = true,
        order = 2,
    )

    var showing by GuiEditable(
        initValue = pinDescription(tile) ?: "",
        label = "Showing",
        description = "The layer and head the values below come from",
        displayOnly = true,
        conditionallyVisibleBy = TileProperties::isPinnable,
        order = 3,
    )

    var summary by GuiEditable(
        initValue = summary,
        label = "Values",
        displayOnly = true,
        order = 4,
    )

    var increment by GuiEditable(
        initValue = increment,
        label = "Increment",
        description = "How far the up and down keys move every weight in this tile",
        min = 0.0,
        increment = 0.001,
        conditionallyVisibleBy = TileProperties::isWeight,
        order = 5,
    )

    override val name = tile.displayTitle
}

private fun summarize(values: DoubleArray): String {
    if (values.isEmpty()) return "none"
    val mean = values.average()
    val sd = sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size)
    return "min ${values.min().roundToString(4)}, max ${values.max().roundToString(4)}, " +
        "mean ${mean.roundToString(4)}, sd ${sd.roundToString(4)}"
}

/**
 * The tile dialog, laid out like the weight matrix dialog: properties, then the values. A weight
 * tile's table is the matrix it shows, rows as outputs and columns as inputs, and edits commit
 * back to the model; any other tile shows a read-only snapshot of what it displays.
 */
fun NetworkPanel.createTileDialog(host: InteriorTileHost, tile: TensorTile): StandardDialog {
    val model = host.generativeModel
    val weightTile = (tile as? MatrixTile)?.takeIf { it.kind == TileKind.WEIGHT }
    val matrix = if (weightTile != null) {
        val tensor = weightTile.tensor
        Matrix(tensor.rows, tensor.cols).also { m ->
            for (r in 0 until tensor.rows) for (c in 0 until tensor.cols) m[r, c] = tensor[r, c].toDouble()
        }
    } else {
        Matrix(tile.rows, tile.cols).also { m ->
            for (r in 0 until tile.rows) for (c in 0 until tile.cols) m[r, c] = tile.valueAt(r, c).toDouble()
        }
    }
    val cols = matrix.ncol()
    val summary = summarize(DoubleArray(matrix.nrow() * cols) { matrix[it / cols, it % cols] })
    val properties = TileProperties(tile, model.incrementFor(tile), summary)
    val editor = AnnotatedPropertyEditor(listOf(properties))

    val table = MatrixDataFrame(matrix, isMutable = weightTile != null)
    var valuesEdited = false
    table.addTableModelListener { valuesEdited = true }
    val tablePanel = SimbrainTablePanel(table, false).apply {
        if (weightTile != null) {
            addSimpleDefaults()
            addSeparator()
            addAction(this.table.createShowEigenValuesAction())
        }
    }

    return StandardDialog().apply {
        title = "Edit ${tile.displayTitle}"
        contentPane = JTabbedPane().apply {
            addTab("Properties", editor)
            addTab(if (weightTile != null) "Weights" else "Values (snapshot)", tablePanel)
        }
        addCommitTask {
            editor.commitChanges()
            model.setTileLabel(tile, properties.label)
            if (properties.increment != model.incrementFor(tile)) model.setTileIncrement(tile, properties.increment)
            host.interior?.refreshLabels()
            if (weightTile != null && valuesEdited && confirmWeightEdit(model)) {
                val m = table.data
                val values = FloatArray(m.nrow() * m.ncol()) { m[it / m.ncol(), it % m.ncol()].toFloat() }
                network.launch(Dispatchers.Default) { model.replaceWeights(weightTile, values) }
            }
        }
        pack()
        setLocationRelativeTo(null)
    }
}
