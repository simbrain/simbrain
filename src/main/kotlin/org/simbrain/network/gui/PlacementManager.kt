/**
 * Automatic placement of new and copied model elements in a network.
 *
 * Three verbs, one rule each:
 * - Add: new objects go to the insertion point (last click on empty canvas) and step right until they find free space,
 *   so repeated adds form a row.
 * - Duplicate: the copy goes next to its source (right, else below, else further right).
 * - Paste: goes to the insertion point if it was set after the copy, otherwise next to the source, with successive
 *   pastes trailing from the previous paste.
 *
 * Duplicates and pastes that continue from the previous copy repeat any move the user made to that copy, so dragging
 * a copy somewhere and pasting or duplicating again lays out an evenly spaced trail in that direction.
 *
 * Copies can also be left in place, on top of their source, for callers that move them themselves (alt-drag).
 *
 * Automatic placement never lands on an existing object. Placement the user asked for explicitly (a clicked paste
 * point, a repeated duplicate move) is honored exactly. Placement runs on the model side, before GUI nodes exist, so
 * occupancy is judged from model-side footprints rather than node bounds.
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

/**
 * Center-to-center spacing of automatically placed neurons, which is also a neuron's footprint.
 */
const val NEURON_FOOTPRINT = 45.0

private const val LAYER_PADDING = 20.0
private const val GROUP_PADDING = 30.0
private const val MAX_PROBE_STEPS = 50
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
            insertionPointIsFresh = value != null
        }

    /**
     * True when the insertion point was set after the last copy, in which case the next paste goes there.
     */
    private var insertionPointIsFresh = false

    /**
     * Supplies the visible part of the canvas in model coordinates, so placement can avoid going off-screen.
     */
    var visibleBounds: (() -> Rectangle2D?)? = null

    /**
     * The insertion point if it is set and on-screen, otherwise the center of the visible canvas.
     */
    val insertionLocation: Point2D
        get() {
            val visible = visibleBounds?.invoke()?.takeIf { !it.isEmpty }
            val point = insertionPoint
            return when {
                point != null && (visible == null || visible.contains(point)) -> point
                visible != null -> point(visible.centerX, visible.centerY)
                else -> point ?: point(0.0, 0.0)
            }
        }

    /**
     * The last paste or duplicate, which the next one can continue from.
     */
    private var lastCopy: LastCopy? = null

    /**
     * False once the clipboard has been refilled, after which a paste starts over from its new source.
     */
    private var pasteContinuesLastCopy = false

    /**
     * @param anchorTopLeft where the thing these copies were placed relative to was
     * @param placedTopLeft where the copies were put, used to tell whether the user has moved them since
     * @param repeatDelta the offset being repeated, if any
     */
    private class LastCopy(
        val copies: Set<LocatableModel>,
        val anchorTopLeft: Point2D,
        val placedTopLeft: Point2D,
        val repeatDelta: Point2D?
    )

    /**
     * Call when the clipboard is refilled: the next paste starts over from the new source.
     */
    fun onCopy() {
        insertionPointIsFresh = false
        pasteContinuesLastCopy = false
    }

    fun placeObject(model: LocatableModel) {
        placeObjects(listOf(model))
    }

    /**
     * Place newly added objects at the insertion point, stepping right past anything already there.
     */
    fun placeObjects(models: List<LocatableModel>) {
        val movable = models.movable()
        if (movable.isEmpty()) return

        val start = insertionLocation
        pinInsertionPoint(start)
        movable.moveTopLeftTo(start)
        val footprint = models.footprint()
        val delta = firstFreeOffset(footprint, occupiedFootprints(models), marchRight(footprint, startStep = 0))
        movable.translate(delta)
        lastCopy = null
    }

    /**
     * Keep adding from [start] even when it came from the center of the visible canvas. Auto zoom re-centers the view
     * after each add, so without this every add would march right from a different point and the row would be
     * unevenly spaced. Does not count as a click, so it does not redirect the next paste.
     */
    private fun pinInsertionPoint(start: Point2D) {
        if (start == insertionPoint) return
        val fresh = insertionPointIsFresh
        insertionPoint = start
        insertionPointIsFresh = fresh
    }

    /**
     * Place pasted or duplicated [copies] of [source].
     */
    fun placeCopies(copies: List<LocatableModel>, source: List<LocatableModel>, placement: CopyPlacement) {
        val movable = copies.movable()
        if (movable.isEmpty()) return

        val expandedSource = source.expanded()
        val present = network.allModels.toSet()

        // What the copies are placed relative to: the previous copies when continuing from them, else the source
        val previous = lastCopy?.takeIf { it.isContinuedBy(placement, expandedSource, present) }
        val anchor = previous?.copies?.toList() ?: expandedSource
        val movableAnchor = anchor.movable()
        val anchorTopLeft = if (movableAnchor.isEmpty()) movable.topLeftLocation else movableAnchor.topLeftLocation
        val repeatDelta = previous?.repeatDeltaFrom(anchorTopLeft)

        if (placement == CopyPlacement.IN_PLACE) {
            movable.moveTopLeftTo(anchorTopLeft)
        } else if (placement == CopyPlacement.PASTE && insertionPointIsFresh) {
            insertionPointIsFresh = false
            movable.moveTopLeftTo(insertionLocation)
        } else if (repeatDelta != null) {
            movable.moveTopLeftTo(anchorTopLeft + repeatDelta)
        } else {
            movable.moveTopLeftTo(anchorTopLeft)
            // The anchor has been laid out by the GUI, so its footprint is more reliable than that of fresh copies
            val footprint = if (movableAnchor.isNotEmpty()) anchor.footprint() else copies.footprint()
            val candidates = if (anchor.any { it in present }) {
                sequenceOf(point(footprint.width, 0.0), point(0.0, footprint.height)) +
                        marchRight(footprint, startStep = 2)
            } else {
                // The source is gone (cut) or lives in another network, so its old spot is the first choice
                marchRight(footprint, startStep = 0)
            }
            movable.translate(firstFreeOffset(footprint, occupiedFootprints(copies), candidates))
        }

        lastCopy = LastCopy(copies.toSet(), anchorTopLeft, movable.topLeftLocation, repeatDelta)
        pasteContinuesLastCopy = true
    }

    /**
     * A duplicate continues from the previous copies when they are what is being duplicated. A paste continues from
     * them as long as the clipboard has not changed and they still exist.
     */
    private fun LastCopy.isContinuedBy(
        placement: CopyPlacement,
        expandedSource: List<LocatableModel>,
        present: Set<NetworkModel>
    ) = when (placement) {
        CopyPlacement.DUPLICATE -> expandedSource.toSet() == copies
        CopyPlacement.PASTE -> pasteContinuesLastCopy && present.containsAll(copies)
        CopyPlacement.IN_PLACE -> false
    }

    /**
     * The offset to repeat when continuing from these copies: the move the user has made to them since they were
     * placed (by dragging them, or by dragging them out in the first place), or else the offset already being
     * repeated. Null means place automatically.
     */
    private fun LastCopy.repeatDeltaFrom(currentTopLeft: Point2D): Point2D? {
        val moved = currentTopLeft.distance(placedTopLeft) > MOVE_TOLERANCE
        return if (moved) currentTopLeft - anchorTopLeft else repeatDelta
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
 * Offsets that step right one footprint width at a time.
 */
private fun marchRight(footprint: Rectangle2D, startStep: Int) =
    (startStep..MAX_PROBE_STEPS).asSequence().map { point(it * footprint.width, 0.0) }

/**
 * The first of the [candidates] offsets at which [footprint] overlaps nothing in [occupied]. Falls back to the first
 * candidate when every candidate is blocked.
 */
fun firstFreeOffset(footprint: Rectangle2D, occupied: List<Rectangle2D>, candidates: Sequence<Point2D>): Point2D {
    // Shrink slightly so that footprints which merely touch do not count as overlapping
    val probe = footprint.addPadding(-1.0)
    return candidates.firstOrNull { offset ->
        val shifted = Rectangle2D.Double(probe.x + offset.x, probe.y + offset.y, probe.width, probe.height)
        occupied.none { it.intersects(shifted) }
    } ?: candidates.first()
}

/**
 * The area a model claims for placement purposes, including breathing room around it.
 */
fun LocatableModel.footprint(): Rectangle2D = when (this) {
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
fun Collection<LocatableModel>.footprint(): Rectangle2D {
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
