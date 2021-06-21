package org.simbrain.network.gui

import org.simbrain.network.*
import org.simbrain.network.core.Neuron
import org.simbrain.network.groups.NeuronGroup
import org.simbrain.network.groups.Subnetwork
import org.simbrain.network.gui.PlacementManager.DefaultOffsets
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.subnetworks.CompetitiveNetwork
import org.simbrain.network.subnetworks.Hopfield
import org.simbrain.util.plus
import org.simbrain.util.point
import java.awt.geom.Point2D

/**
 * Manage intelligent placement of new model elements in a [org.simbrain.network.gui.NetworkPanel].
 *
 * Placement is managed using two concepts. First, an anchor point. Second, a delta between the current anchor point and
 * previous anchor point.  There are cases to keep in mind:
 * 1. The anchor point is reset when you click on the screen, to the point you clicked on.
 * 2. Repeatedly adding an object (using new Neuron, etc) adds them at a fixed offset from the anchor point using
 * [DefaultOffsets]. With each addition, the current and previous anchor points are updated. See [placeObject].
 * 3. Adding an object using copy-paste or duplicate, adds them using the delta between the current anchor point and
 * the previous anchor point. This allows custom "paste trails" to be created.
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
            is NeuronArray -> point(0, -250)
            is NeuronGroup -> point(270, 0)
            is Hopfield -> point(300, 0)
            is CompetitiveNetwork -> point(300, 0)
            is Subnetwork -> point(220, 0)
            else -> point(45, 0)
        }
    }

    /**
     * Tells you the location of the most recently placed object.
     */
    var anchorPoint =  point(0.0, 0.0)

    /**
     * Set last location clicked on screen.
     */
    var lastClickedLocation: Point2D = point(0, 0)
        set(point) {
            // println("Reset last clicked")
            field = point
            useLastClickedLocation = true
        }

    /**
     * Set to true when a location on the screen is clicked.
     */
    private var useLastClickedLocation = true

    /**
     * Set to true right after duplicating or copy-pasting.
     */
    private var pasted = false

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
            // Reset the anchor to wherever was last clicked and put objects there
            anchorPoint = lastClickedLocation
            useLastClickedLocation = false
            update(models, point(0.0,0.0))
        } else {
            // Place objects at a default offset from wherever they were last placed
            update(models, DefaultOffsets[models[0]])
        }
    }

    /**
     * Translate models by the anchor point + delta.
     */
    private fun update(models: List<LocatableModel>, delta: Point2D) {

        // TODO: Later after this code stabilizes, add back the concept of a drag-based delta.
        //  After a drag event, delta with last anchor point, and use that as the new delta.

        // Move the objects
        if (models.size == 1) {
            models.translate(anchorPoint + delta)
        } else {
            // Special handling for multiple placed objects
            // TODO: Remove this once drag-based delta is re-implemented
            models.translate(point(models.bound.width, models.bound.height + 10) + anchorPoint + delta)
        }

        // Reset anchor point to wherever objects were just pasted
        anchorPoint = models.topLeftLocation
    }

}

