package org.simbrain.network.gui

import org.simbrain.network.LocatableModel
import org.simbrain.network.core.Neuron
import org.simbrain.network.groups.NeuronGroup
import org.simbrain.network.groups.Subnetwork
import org.simbrain.network.gui.PlacementManager.DefaultOffsets
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.moveToOrigin
import org.simbrain.network.subnetworks.CompetitiveNetwork
import org.simbrain.network.subnetworks.Hopfield
import org.simbrain.network.topLeftLocation
import org.simbrain.network.translate
import org.simbrain.util.magnitude
import org.simbrain.util.plus
import org.simbrain.util.point
import org.simbrain.util.times
import java.awt.geom.Point2D

/**
 * Manage intelligent placement of new model elements in a [org.simbrain.network.gui.NetworkPanel].
 *
 * Placement is managed using two concepts. First, an anchor point. Second, a delta between the current anchor point and
 * previous anchor point. There are cases to keep in mind:
 *
 *   1. The anchor point is reset when you click on the screen, to the point you clicked on.
 *   2. Repeatedly adding an object (using new Neuron, etc.) adds them at a fixed offset from the anchor point using
 *   [DefaultOffsets]. With each addition, the current and previous anchor points are updated. See [placeObject].
 *   3. Adding an object using copy-paste or duplicate, adds them using the delta between the current anchor point and
 *   the previous anchor point. This allows custom "paste trails" to be created.
 *
 * @author Yulin Li
 * @author Jeff Yoshimi
 */
class PlacementManager() {

    /**
     * Offsets associated with specific types of objects.
     */
    object DefaultOffsets {
        operator fun get(model: LocatableModel?) = when (model) {
            is Neuron -> point(45, 0)
            is NeuronArray -> point(300, 0)
            is NeuronGroup -> point(400, 0)
            is Hopfield -> point(300, 0)
            is CompetitiveNetwork -> point(300, 0)
            is Subnetwork -> point(220, 0)
            else -> point(45, 0)
        }
    }

    /**
     * Location of the most recently placed object.
     */
    var anchorPoint = point(0.0, 0.0)

    /**
     * Last anchor point, which is used to compute [deltaDrag]
     */
    var previousAnchorPoint = point(0.0, 0.0)

    /**
     * Difference between last two anchor points, which is set when dragging network objects, and then used when
     * repeatedly adding objects, which is convenient for creating "paste trails".
     */
    var deltaDrag: Point2D? = null
        set(value) {
            // println("set deltaDrag = $value")
            // Mouse clicks are treated as drag events so need to disregard very small deltas
            if (value != null && value.magnitude > .1) {
                field = value
            }
        }

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
    private var useLastClickedLocation = true

    /**
     * Place an object.
     */
    fun placeObject(model: LocatableModel) {
        placeObjects(listOf(model))
    }

    /**
     * Paste a list of objects using the delta between the current anchor point and the
     * previous anchor point.
     */
    fun placeObjects(models: List<LocatableModel>) {
        if (models.isEmpty()) {
            return
        }

        models.moveToOrigin()

        if (useLastClickedLocation) {
            // println("Case 1: Clicked location")
            // Reset the anchor to wherever was last clicked and put objects there
            anchorPoint = lastClickedLocation
            useLastClickedLocation = false
            offsetFromAnchor(models, point(0.0,0.0))
        } else {
            if (deltaDrag == null) {
                // println("Case 2: Default")
                // Place objects at a default offset from wherever they were last placed
                offsetFromAnchor(models, DefaultOffsets[models[0]] * models.size)
            } else {
                // println("Case 3: Delta")
                offsetFromAnchor(models, deltaDrag!!)
            }
        }
    }

    /**
     * Translate models by the anchor point + delta.
     */
    private fun offsetFromAnchor(models: List<LocatableModel>, offset: Point2D) {

        // println("\tanchor = $anchorPoint\n\toffset = $offset")

        // Move the objects
        models.translate(anchorPoint + offset)

        previousAnchorPoint = anchorPoint

        // Reset anchor point to wherever objects were just placed
        anchorPoint = models.topLeftLocation
    }

}

