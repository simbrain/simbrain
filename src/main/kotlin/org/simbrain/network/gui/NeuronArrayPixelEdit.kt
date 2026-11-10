/**
 * Helpers for the per-pixel quick-edit feature on neuron arrays, weight matrices, activation sequences, tensor
 * layers and convolution kernels.
 *
 * Pixels behave like small selectable models inside their node: a plain click on one selects it (and the
 * node), shift-click extends the pixel selection, and a plain click on the node outside its pixels selects
 * the whole node and drops the pixel selection. Pixel selections live on the visual nodes, which expose them
 * through [PixelSelectionHost]; these utilities aggregate them across the panel and route increment /
 * decrement / clear / randomize requests to the underlying model.
 */
package org.simbrain.network.gui

import org.piccolo2d.PNode
import org.piccolo2d.event.PInputEvent
import org.simbrain.network.core.ActivationSequence
import org.simbrain.network.core.ConvolutionConnector
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.TensorLayer
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.gui.nodes.ScreenElement
import org.simbrain.util.StandardDialog
import org.simbrain.util.UserParameter
import org.simbrain.util.createEditorDialog
import org.simbrain.util.propertyeditor.EditableObject
import java.awt.event.MouseEvent
import java.awt.geom.Ellipse2D
import java.awt.geom.Rectangle2D
import javax.swing.ToolTipManager

private const val PIXEL_TARGET_ATTRIBUTE = "pixelTarget"

/**
 * A node whose cells can be selected and edited one at a time. The panel-wide pixel utilities work through this
 * interface, so a node type joins the feature by implementing it and routing presses on its cells through
 * [pressPixel].
 */
interface PixelSelectionHost {

    val hasPixelSelection: Boolean

    fun clearPixelSelection()

    /** The selected cells as addressable values. */
    fun selectedPixels(): List<PixelTarget>

    /** The displayed cells whose visual area intersects [ellipse], given in global coordinates. */
    fun pixelsUnder(ellipse: Ellipse2D): List<PixelTarget>
}

private val NetworkPanel.pixelSelectionHosts: List<PixelSelectionHost>
    get() = canvas.layer.allNodes.filterIsInstance<PixelSelectionHost>()

/** Tag a node whose cells are pixels, so the canvas handler can tell a pixel press from a whole-node press. */
fun PNode.markAsPixelTarget() {
    addAttribute(PIXEL_TARGET_ATTRIBUTE, true)
}

/** True if this node or any ancestor is a pixel target, since picks land on the innermost child of a composite. */
val PNode?.isPixelTarget: Boolean
    get() = generateSequence(this) { it.parent }.any { it.getAttribute(PIXEL_TARGET_ATTRIBUTE) == true }

/**
 * Handle a press on a pixel of [node]. A plain left press selects just that pixel and is left unhandled so the
 * canvas handler goes on to select the node, and drag it where it is draggable, as for any screen element. A
 * shift press toggles the pixel in the node's pixel selection, keeps the node selected, and is marked handled so
 * the canvas handler does not toggle the node off. Other buttons, popup triggers, the pan key, and cursor modes
 * other than selection are ignored.
 */
fun NetworkPanel.pressPixel(event: PInputEvent, node: ScreenElement, select: (addToSelection: Boolean) -> Unit) {
    if (event.button != MouseEvent.BUTTON1 || event.isPopupTrigger || event.isPanKeyDown) return
    if (mouseCursor != MouseEventHandler.MouseCursor.Selection) return
    if (event.isShiftDown) {
        select(true)
        if (node !in selectionManager) selectionManager.add(node)
        event.isHandled = true
    } else {
        select(false)
    }
}

/**
 * Shared cell-hit logic for the nodes' `collectCellsInGlobalEllipse`. Converts [ellipse] (global
 * coordinates) into this node's local frame, then walks a [rows] x [cols] grid of [cellW] x [cellH]
 * cells, applying [map] to each cell whose rect intersects the brush. `null` results are skipped.
 */
inline fun <T> PNode.cellsIntersectingGlobalEllipse(
    ellipse: Ellipse2D,
    rows: Int,
    cols: Int,
    cellW: Double,
    cellH: Double,
    map: (row: Int, col: Int) -> T?
): List<T> {
    val gb = ellipse.bounds2D
    val local = Rectangle2D.Double(gb.x, gb.y, gb.width, gb.height)
    globalToLocal(local)
    val result = mutableListOf<T>()
    for (row in 0 until rows) {
        for (col in 0 until cols) {
            if (local.intersects(col * cellW, row * cellH, cellW, cellH)) {
                map(row, col)?.let { result += it }
            }
        }
    }
    return result
}

/**
 * Iterate every pixel whose visual cell intersects [ellipse] in global coordinates. Used by the wand handler so
 * wand actions can apply to per-pixel values.
 */
fun NetworkPanel.pixelsInGlobalEllipse(ellipse: Ellipse2D): Sequence<PixelTarget> =
    pixelSelectionHosts.asSequence().flatMap { it.pixelsUnder(ellipse) }

/** True if any visible node has a non-empty pixel selection. */
fun NetworkPanel.hasAnyPixelSelection(): Boolean = pixelSelectionHosts.any { it.hasPixelSelection }

/**
 * Hide any visible canvas tooltip right away and keep it hidden until the mouse next enters a node.
 * Called when the mouse enters or leaves a pixel surface and when a pixel selection begins, so the node-level
 * summary box does not sit over the cells being traced or edited. Clearing the text alone leaves an
 * already-open tooltip up until Swing's dismiss delay, so the shared manager is toggled to force the
 * popup closed.
 */
fun NetworkPanel.hideCanvasTooltip() {
    canvas.toolTipText = null
    ToolTipManager.sharedInstance().apply {
        isEnabled = false
        isEnabled = true
    }
}

/**
 * Set the canvas tooltip to the value readout for the pixel under the mouse. The readout is withheld while Alt
 * (the pixel-edit modifier) is held, while the hovered node has a pixel selection, outside the selection cursor,
 * and while the workspace runs: a tooltip only refreshes when the mouse moves, so during a run it would show a
 * stale value.
 */
fun NetworkPanel.showPixelTooltip(event: PInputEvent, hasPixelSelection: Boolean, text: () -> String?) {
    val suppressed = event.isAltDown || hasPixelSelection ||
        mouseCursor != MouseEventHandler.MouseCursor.Selection || isWorkspaceRunning
    canvas.toolTipText = if (suppressed) null else text()
}

private val NetworkPanel.isWorkspaceRunning: Boolean
    get() = try {
        networkComponent.workspace.updater.isRunning
    } catch (_: UninitializedPropertyAccessException) {
        false
    }

/** Drop all pixel selections (e.g. on Escape). */
fun NetworkPanel.clearAllPixelSelections() {
    pixelSelectionHosts.forEach { it.clearPixelSelection() }
}

/**
 * Wire pixel selection to the component selection: whenever a [PixelSelectionHost] is removed from the panel
 * selection, drop its pixel selection too.
 * Establishes the invariant that pixel selection only exists for currently-selected components,
 * which keeps the conditionally-enabled randomize action's affordance consistent.
 */
fun NetworkPanel.bindPixelSelectionToComponentSelection() {
    selectionManager.events.selection.on { old, new ->
        (old - new).forEach { (it as? PixelSelectionHost)?.clearPixelSelection() }
    }
}

/** Apply [edit] to every selected pixel and fire `updated` once per affected model. */
private fun NetworkPanel.editSelectedPixels(edit: (PixelTarget) -> Unit) {
    val pixels = pixelSelectionHosts.flatMap { it.selectedPixels() }
    pixels.forEach(edit)
    pixels.fireUpdatedOncePerModel()
}

/** Fire `updated` on each model that owns one of these pixels. */
fun Iterable<PixelTarget>.fireUpdatedOncePerModel() {
    distinctBy { it.model }.forEach { it.fireUpdated() }
}

fun NetworkPanel.incrementSelectedPixels() = editSelectedPixels { it.write(it.read() + it.increment) }

fun NetworkPanel.decrementSelectedPixels() = editSelectedPixels { it.write(it.read() - it.increment) }

fun NetworkPanel.clearSelectedPixels() = editSelectedPixels { it.write(0.0) }

fun NetworkPanel.randomizeSelectedPixels() = editSelectedPixels { it.write(it.randomValue()) }

/**
 * Addressable pixel inside a model that is drawn as a grid of values. Used by both the pixel batch-edit
 * dialog flow and by wand-mode actions that operate on per-cell values.
 */
sealed class PixelTarget {

    /** The model the pixel belongs to. */
    abstract val model: NetworkModel

    /** True if the pixel holds an activation, false if it holds a connection strength. */
    abstract val isActivation: Boolean

    /** Amount the increment and decrement keys change the pixel by. */
    abstract val increment: Double

    /** Current numeric value of the pixel. */
    abstract fun read(): Double

    /** Overwrite the pixel's numeric value. Does not fire `events.updated` — call [fireUpdated] when done. */
    abstract fun write(value: Double)

    /** Lower/upper bounds for the pixel — used by wand amount calculations and clamping. */
    abstract fun bounds(): Pair<Double, Double>

    /** Fire the underlying component's `events.updated` so the view repaints. */
    abstract fun fireUpdated()

    /** A value drawn from the randomizer the preferences set for this kind of pixel. */
    fun randomValue(): Double = if (isActivation) {
        NetworkPreferences.activationRandomizer.sampleDouble()
    } else {
        NetworkPreferences.weightRandomizer.sampleDouble()
    }

    data class ArrayPixel(val array: NeuronArray, val idx: Int) : PixelTarget() {
        override val model get() = array
        override val isActivation get() = true
        override val increment get() = array.increment
        override fun read(): Double = array.activations[idx, 0]
        override fun write(value: Double) { array.activations[idx, 0] = value }
        override fun bounds(): Pair<Double, Double> = array.updateRule.graphicalBounds.let { it.start to it.endInclusive }
        override fun fireUpdated() { array.events.updated.fire() }
    }

    data class MatrixPixel(val wm: WeightMatrix, val row: Int, val col: Int) : PixelTarget() {
        override val model get() = wm
        override val isActivation get() = false
        override val increment get() = wm.increment
        override fun read(): Double = wm.weights[row, col]
        override fun write(value: Double) { wm.weights[row, col] = value }
        override fun bounds(): Pair<Double, Double> = -1.0 to 1.0
        override fun fireUpdated() { wm.events.updated.fire() }
    }

    data class SequencePixel(val seq: ActivationSequence, val row: Int, val col: Int) : PixelTarget() {
        override val model get() = seq
        override val isActivation get() = true
        override val increment get() = seq.increment
        override fun read(): Double = seq.activations[row, col]
        override fun write(value: Double) { seq.activations[row, col] = value }
        override fun bounds(): Pair<Double, Double> = seq.updateRule.graphicalBounds.let { it.start to it.endInclusive }
        override fun fireUpdated() { seq.events.updated.fire() }
    }

    /** One activation of a tensor layer, addressed by its index in the layer's flat HWC array. */
    data class TensorPixel(val tensor: TensorLayer, val idx: Int) : PixelTarget() {
        override val model get() = tensor
        override val isActivation get() = true
        override val increment get() = tensor.increment
        override fun read(): Double = tensor.activations[idx]
        override fun write(value: Double) { tensor.activations[idx] = value }
        override fun bounds(): Pair<Double, Double> = tensor.graphicalBounds.let { it.start to it.endInclusive }
        override fun fireUpdated() { tensor.events.updated.fire() }
    }

    /** One kernel weight of a convolution, addressed by its index in the flat filter-major kernel array. */
    data class KernelPixel(val conv: ConvolutionConnector, val idx: Int) : PixelTarget() {
        override val model get() = conv
        override val isActivation get() = false
        override val increment get() = conv.increment
        override fun read(): Double = conv.kernels[idx]
        override fun write(value: Double) { conv.kernels[idx] = value }
        override fun bounds(): Pair<Double, Double> = -1.0 to 1.0
        override fun fireUpdated() { conv.events.updated.fire() }
    }
}

/**
 * Single-field wrapper used by the pixel batch-edit dialog. One wrapper per selected pixel; the
 * [AnnotatedPropertyEditor]'s multi-object consistency check shows the common value (or "...") and
 * commits a single user-entered value back to every wrapper.
 */
class PixelValueWrapper(initValue: Double = 0.0) : EditableObject {
    @UserParameter(label = "Value", description = "Value to assign to each selected pixel", order = 1)
    var value: Double = initValue
    override val name = "Pixel"
}

/**
 * Build a batch-edit dialog over every pixel currently selected on any visible node. Returns null if nothing is
 * selected.
 */
fun NetworkPanel.createPixelEditDialog(): StandardDialog? {
    val entries = pixelSelectionHosts.flatMap { it.selectedPixels() }.map { it to PixelValueWrapper(it.read()) }
    if (entries.isEmpty()) return null

    val title = "Edit ${entries.size} pixel${if (entries.size != 1) "s" else ""}"
    return entries.map { it.second }.createEditorDialog(titleName = title) {
        entries.forEach { (pixel, wrapper) -> pixel.write(wrapper.value) }
        entries.map { it.first }.fireUpdatedOncePerModel()
    }
}
