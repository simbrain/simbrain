package org.simbrain.network.gui

import org.simbrain.network.core.*
import org.simbrain.network.subnetworks.CompetitiveNetwork
import org.simbrain.network.subnetworks.Hopfield
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.util.format
import org.simbrain.util.plus
import org.simbrain.util.point
import java.awt.geom.Point2D

/**
 * Manage intelligent placement of new model elements in a [org.simbrain.network.gui.NetworkPanel].
 *
 * The system works via two modes:
 *
 * 1. Click mode. Base case is last clicked location. When clicking on the screen there is a click the next object is
 * placed there.
 *
 * 2. Offset mode. Any additional objects added without clicking on the screen (paste, duplicate, add object) is offset
 * from the last placed object by a default amount, depending on what type of object it is. This allows "paste trails"
 * to be created
 *
 * @author Yulin Li
 * @author Jeff Yoshimi
 */
class PlacementManager() {

    var lastSelectedModel: LocatableModel? = null

    /**
     * Origination point when setting a custom offset
     */
    var customOffsetAnchor: LocatableModel? = null

    /**
     * Set last location clicked on screen.
     */
    var lastClickedLocation: Point2D = point(0, 0)
        set(point) {
            field = point
            useLastClickedLocation = true
        }

    /**
     * Set to true when a location on the screen is clicked.
     */
    var useLastClickedLocation = true

    var customOffset: Point2D = point(0, 0)
        set(point) {
            field = point
            useCustomOffset = true
        }

    var useCustomOffset = false

    fun computeOffset(model: LocatableModel) = if (useCustomOffset) {
        customOffset
    } else {
        when (model) {
            is Neuron -> point(45, 0)
            is NeuronCollection -> point(400, 0)
            is NeuronArray -> point(300,0)
            is Hopfield -> point(300,0)
            is CompetitiveNetwork -> point(300,0)
            is Subnetwork -> point(220,0)
            else -> point(45, 0)
        }
    }

    /**
     * Place an object.
     */
    fun placeObject(model: LocatableModel) {
        placeObjects(listOf(model))
    }

    /**
     * Paste a list of objects at some offset from the last placed object.
     */
    fun placeObjects(initModels: List<LocatableModel>) {

        customOffsetAnchor = lastSelectedModel

        // NeuronCollections should not be placed.
        val models = initModels.filter { it !is NeuronCollection }
        if (models.isEmpty()) {
            return
        }

        if (useLastClickedLocation) {
            useLastClickedLocation = false
            models.moveTo(lastClickedLocation)
        } else {
            models.moveTo((lastSelectedModel?.location ?: point(0, 0)) + computeOffset(models.first()))
        }

        lastSelectedModel = models.sortLeftRightTopBottom().first()
    }

    private fun pointToString(point: Point2D?) = if (point != null) "(${point.x.format(2)}, ${point.y.format(2)})" else "null"

    private fun printState() {
        println("lastPlacedModel = ${pointToString(lastSelectedModel?.location)}, lastClickedLocation = ${pointToString(lastClickedLocation)}, useLastClickedLocation = $useLastClickedLocation")
    }

}

