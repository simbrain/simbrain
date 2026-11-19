/**
 * Automatic placement of new and copied model elements in a network.
 *
 * Three verbs, one rule each:
 * - Add: new objects go to the insertion point (last click on empty canvas) and take the first free slot from there,
 *   so repeated adds form a row, which wraps to a new row below when it is full.
 * - Duplicate: the copy goes next to its source (right, else below, else further right).
 * - Paste: goes to the insertion point if it was set after the copy, otherwise next to the source, with successive
 *   pastes trailing from the previous paste.
 *
 * Duplicates and pastes that continue from the previous copy repeat any move the user made to that copy, so dragging
 * a copy somewhere and pasting or duplicating again lays out an evenly spaced trail in that direction. A paste made
 * at the insertion point starts a new trail: the next paste goes next to it.
 *
 * Copies can also be left in place, on top of their source, for callers that move them themselves (alt-drag).
 *
 * Automatic placement never lands on an existing object. Placement the user asked for explicitly (a clicked paste
 * point, a repeated duplicate move) is honored exactly. Placement runs on the model side, before GUI nodes exist, so
 * occupancy is judged from model-side footprints rather than node bounds.
 *
 * The clipboard is shared by every network, so whether a paste continues from the previous one, or goes to a click,
 * is judged by the clipboard's contents ([Clipboard.contentVersion]) rather than by what this network last copied.
 */
package org.simbrain.network.gui

import org.simbrain.network.core.*
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.network.trainers.SupervisedModel
import org.simbrain.util.addPadding
import org.simbrain.util.minus
import org.simbrain.util.plus
import org.simbrain.util.point
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.util.BitSet
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Center-to-center spacing of automatically placed neurons, which is also a neuron's footprint.
 */
internal const val NEURON_FOOTPRINT = 45.0

private const val LAYER_PADDING = 20.0
private const val GROUP_PADDING = 30.0
internal const val SLOTS_PER_ROW = 50
private const val MAX_SLOT_ROWS = 1000
private const val MOVE_TOLERANCE = 0.01

private val nominalLayerSize = point(260.0, 60.0)
private val nominalTextSize = point(120.0, 40.0)
private val nominalModelSize = point(300.0, 200.0)

/**
 * How copies are positioned relative to their source.
 */
enum class CopyPlacement {

    /**
     * At the insertion point if it was set after the copy, otherwise trailing from the source or the previous copy,
     * repeating any move the user made to that copy.
     */
    PASTE,

    /**
     * Next to the source, or repeating the move the user made to the previous copy when duplicating that copy.
     */
    DUPLICATE,

    /**
     * On top of the source. The caller is expected to move the copies.
     */
    IN_PLACE
}

class PlacementManager(private val network: Network) {

    /**
     * Where new objects are added. Set by clicking on empty canvas; stays put until the next click.
     */
    var insertionPoint: Point2D? = null
        set(value) {
            field = value
            fallbackInsertionPoint = null
            clickAwaitingPaste = if (value != null) Clipboard.contentVersion else null
        }

    /**
     * The clipboard contents (a [Clipboard.contentVersion]) that were current when the insertion point was set, until
     * a paste goes there. The next paste of those contents goes to the insertion point. Once the clipboard holds
     * something else the click no longer says where to paste.
     */
    private var clickAwaitingPaste: Int? = null

    /**
     * Where adds go while there is no insertion point on-screen: the center of the visible canvas as it was at the
     * first of those adds. Auto zoom re-centers the view after each add, so taking the center afresh every time would
     * start each add from a different point and leave the row unevenly spaced.
     */
    private var fallbackInsertionPoint: Point2D? = null

    /**
     * Supplies the visible part of the canvas in model coordinates, so placement can avoid going off-screen.
     */
    var visibleBounds: (() -> Rectangle2D?)? = null

    /**
     * The insertion point if it is set and on-screen, otherwise where adds have been going instead if that is still
     * on-screen, otherwise the center of the visible canvas.
     */
    val insertionLocation: Point2D
        get() {
            val visible = visibleBounds?.invoke()?.takeIf { !it.isEmpty }
            fun Point2D?.ifOnScreen() = this?.takeIf { visible == null || visible.contains(it) }
            return insertionPoint.ifOnScreen()
                ?: fallbackInsertionPoint.ifOnScreen()
                ?: visible?.let { point(it.centerX, it.centerY) }
                ?: point(0.0, 0.0)
        }

    /**
     * The last paste or duplicate, which the next one can continue from.
     */
    private var lastCopy: LastCopy? = null

    /**
     * @param placedAt where the copies were put, used to tell whether the user has moved them since
     * @param anchoredAt where the thing the copies were placed relative to was. Null for copies pasted at the
     * insertion point, which were not placed relative to anything.
     * @param repeatedStep the offset from the anchor, when it repeated an earlier offset rather than being found by
     * looking for free space
     * @param clipboardVersion the clipboard contents the copies were made from
     */
    private class LastCopy(
        val copies: Set<LocatableModel>,
        val placedAt: Point2D,
        val anchoredAt: Point2D?,
        val repeatedStep: Point2D?,
        val clipboardVersion: Int
    )

    fun placeObject(model: LocatableModel) {
        placeObjects(listOf(model))
    }

    /**
     * Place newly added objects at the insertion point, in the first free slot from there.
     */
    fun placeObjects(models: List<LocatableModel>) {
        val movable = models.movable()
        if (movable.isEmpty()) return

        val start = insertionLocation
        if (start != insertionPoint && visibleBounds != null) fallbackInsertionPoint = start
        movable.moveTopLeftTo(start)
        movable.translate(SlotGrid(models.footprint(), occupiedFootprints(models)).firstFreeFrom())
        lastCopy = null
    }

    /**
     * Place pasted or duplicated [copies] of [source].
     */
    fun placeCopies(copies: List<LocatableModel>, source: List<LocatableModel>, placement: CopyPlacement) {
        val movable = copies.movable()
        if (movable.isEmpty()) return

        val expandedSource = source.expanded()
        val present = network.allModels.toSet()
        val clipboardVersion = Clipboard.contentVersion

        // What the copies are placed relative to: the previous copies when continuing from them, else the source
        val previous = lastCopy?.takeIf { it.isContinuedBy(placement, expandedSource, present, clipboardVersion) }
        val anchor = previous?.copies?.toList() ?: expandedSource
        val movableAnchor = anchor.movable()
        val anchorTopLeft = if (movableAnchor.isEmpty()) movable.topLeftLocation else movableAnchor.topLeftLocation

        val atInsertionPoint = placement == CopyPlacement.PASTE && clickAwaitingPaste == clipboardVersion
        val step = if (atInsertionPoint) null else previous?.stepToRepeat(anchorTopLeft)

        if (atInsertionPoint) {
            clickAwaitingPaste = null
            val target = insertionLocation
            movable.moveTopLeftTo(target)
            // With the clicked point off-screen this is not a place the user chose, so make room as an add does
            if (target != insertionPoint) {
                movable.translate(SlotGrid(copies.footprint(), occupiedFootprints(copies)).firstFreeFrom())
            }
        } else if (placement == CopyPlacement.IN_PLACE) {
            movable.moveTopLeftTo(anchorTopLeft)
        } else if (step != null) {
            movable.moveTopLeftTo(anchorTopLeft + step)
        } else {
            movable.moveTopLeftTo(anchorTopLeft)
            // The anchor has been laid out by the GUI, so its footprint is more reliable than that of fresh copies
            val footprint = if (movableAnchor.isNotEmpty()) anchor.footprint() else copies.footprint()
            val slots = SlotGrid(footprint, occupiedFootprints(copies))
            val offset = if (anchor.any { it in present }) {
                // Right of the anchor, else below it, else further right
                slots.offsetIfFree(column = 1, row = 0)
                    ?: slots.offsetIfFree(column = 0, row = 1)
                    ?: slots.firstFreeFrom(column = 2)
            } else {
                // The source is gone (cut) or lives in another network, so its old spot is the first choice
                slots.firstFreeFrom()
            }
            movable.translate(offset)
        }

        lastCopy = LastCopy(
            copies = copies.toSet(),
            placedAt = movable.topLeftLocation,
            anchoredAt = anchorTopLeft.takeUnless { atInsertionPoint },
            repeatedStep = step,
            clipboardVersion = clipboardVersion
        )
    }

    /**
     * A duplicate continues from the previous copies when they are what is being duplicated. A paste continues from
     * them as long as the clipboard has not changed and they still exist.
     */
    private fun LastCopy.isContinuedBy(
        placement: CopyPlacement,
        expandedSource: List<LocatableModel>,
        present: Set<NetworkModel>,
        currentClipboardVersion: Int
    ) = when (placement) {
        CopyPlacement.DUPLICATE -> expandedSource.toSet() == copies
        CopyPlacement.PASTE -> clipboardVersion == currentClipboardVersion && present.containsAll(copies)
        CopyPlacement.IN_PLACE -> false
    }

    /**
     * The offset to repeat when continuing from these copies: the offset from their anchor to where the user has
     * since moved them (by dragging them, or by dragging them out in the first place), or else the offset already
     * being repeated. Null means place automatically, which is also what a move of copies that had no anchor leads
     * to.
     */
    private fun LastCopy.stepToRepeat(currentTopLeft: Point2D): Point2D? {
        val moved = currentTopLeft.distance(placedAt) > MOVE_TOLERANCE
        return if (moved) anchoredAt?.let { currentTopLeft - it } else repeatedStep
    }

    private fun occupiedFootprints(exclude: Collection<LocatableModel>): List<Rectangle2D> {
        val excluded = exclude.toSet()
        return network.allModels
            .filterIsInstance<LocatableModel>()
            .filter { it !in excluded && it !is InfoText }
            .map { it.footprint() }
    }

}

/**
 * The places a footprint could be moved to, as a grid: slot (column, row) is the footprint shifted right by that many
 * of its widths and down by that many of its heights. A slot is taken when the footprint would overlap something in
 * [occupied] there. Rows are [SLOTS_PER_ROW] slots long. Building the grid takes one pass over [occupied], however
 * many slots are taken.
 */
private class SlotGrid(footprint: Rectangle2D, occupied: List<Rectangle2D>) {

    private val slotWidth = footprint.width.coerceAtLeast(1.0)

    private val slotHeight = footprint.height.coerceAtLeast(1.0)

    private val taken = BitSet()

    init {
        // Shrink slightly so that footprints which merely touch do not count as overlapping
        val probe = footprint.addPadding(-1.0)
        // A footprint that shrinks to nothing overlaps nothing, wherever it is
        val hasArea = footprint.width > 2 && footprint.height > 2
        for (other in occupied) {
            if (!hasArea || other.isEmpty) continue
            val columns = shiftsBetween(other.minX - probe.maxX, other.maxX - probe.minX, slotWidth, SLOTS_PER_ROW)
            val rows = shiftsBetween(other.minY - probe.maxY, other.maxY - probe.minY, slotHeight, MAX_SLOT_ROWS)
            if (columns.isEmpty()) continue
            for (row in rows) taken.set(index(columns.first, row), index(columns.last, row) + 1)
        }
    }

    /**
     * The offset to a slot, if it is free.
     */
    fun offsetIfFree(column: Int, row: Int): Point2D? = index(column, row).takeIf { !taken[it] }?.let { offsetOf(it) }

    /**
     * The offset to the first free slot at or after [column] in the first row, reading along each row and then down
     * to the next.
     */
    fun firstFreeFrom(column: Int = 0): Point2D = offsetOf(taken.nextClearBit(column))

    private fun index(column: Int, row: Int) = row * SLOTS_PER_ROW + column

    private fun offsetOf(index: Int) = point((index % SLOTS_PER_ROW) * slotWidth, (index / SLOTS_PER_ROW) * slotHeight)

}

/**
 * The whole numbers n from 0 up to, but not including, [count] for which n * [step] lies strictly between [low] and
 * [high].
 */
private fun shiftsBetween(low: Double, high: Double, step: Double, count: Int): IntRange {
    val first = (floor(low / step) + 1).coerceIn(0.0, count.toDouble()).toInt()
    val last = (ceil(high / step) - 1).coerceIn(-1.0, count - 1.0).toInt()
    return first..last
}

/**
 * The area a model claims for placement purposes, including breathing room around it.
 */
internal fun LocatableModel.footprint(): Rectangle2D = when (this) {
    is Neuron -> centeredRectangle(location, point(NEURON_FOOTPRINT, NEURON_FOOTPRINT))
    is NeuronCollection -> neuronList.footprint()
    is Layer -> sizedFootprint(width, height, nominalLayerSize)
    is TensorLayer -> sizedFootprint(renderWidth, renderHeight, nominalLayerSize)
    is SupervisedModel -> layers.footprint()
    is Subnetwork -> {
        val children = modelList.all.filterIsInstance<LocatableModel>()
        if (children.isEmpty()) {
            centeredRectangle(location, nominalModelSize)
        } else {
            children.footprint().addPadding(GROUP_PADDING)
        }
    }
    is NetworkTextObject -> centeredRectangle(location, nominalTextSize)
    else -> centeredRectangle(location, nominalModelSize)
}

/**
 * Union of the footprints of the models in the collection.
 */
internal fun Collection<LocatableModel>.footprint(): Rectangle2D {
    val result = Rectangle2D.Double()
    forEachIndexed { i, model ->
        if (i == 0) result.setRect(model.footprint()) else result.add(model.footprint())
    }
    return result
}

/**
 * Footprint from a GUI-reported size, or from a nominal size when the GUI has not laid the model out yet.
 */
private fun LocatableModel.sizedFootprint(width: Double, height: Double, nominal: Point2D): Rectangle2D {
    val size = if (width > 0 && height > 0) point(width, height) else nominal
    return centeredRectangle(location, size).addPadding(LAYER_PADDING)
}

private fun centeredRectangle(center: Point2D, size: Point2D): Rectangle2D =
    Rectangle2D.Double(center.x - size.x / 2, center.y - size.y / 2, size.x, size.y)

/**
 * The models that must be moved to move the whole list: models whose location merely reflects other models in the
 * list are dropped, so nothing is moved twice.
 */
private fun List<LocatableModel>.movable(): List<LocatableModel> {
    val all = toSet()
    return filter { model ->
        when (model) {
            is NeuronCollection -> false
            is SupervisedModel -> model.layers.none { it in all }
            else -> true
        }
    }
}

/**
 * Add the models that the clipboard folds into their parents, so a source list can be compared with its copies.
 */
private fun List<LocatableModel>.expanded(): List<LocatableModel> = flatMap { model ->
    when (model) {
        is NeuronCollection -> listOf(model) + model.neuronList
        is SupervisedModel -> listOf(model) + model.layers.toList<LocatableModel>().expanded()
        else -> listOf(model)
    }
}.distinct()

private fun List<LocatableModel>.moveTopLeftTo(target: Point2D) = translate(target - topLeftLocation)
