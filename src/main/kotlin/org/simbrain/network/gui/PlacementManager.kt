package org.simbrain.network.gui

import org.simbrain.network.LocatableModel
import org.simbrain.network.core.Neuron
import org.simbrain.network.groups.NeuronGroup
import org.simbrain.network.gui.PlacementManager.DefaultOffsets
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.topLeftLocation
import org.simbrain.util.minus
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
 * [DefaultOffsets]. With each addition, the current and previous anchor points are updated. See [addNewModelObject].
 * 3. Adding an object using copy-paste or duplicate, adds them using the delta between the current anchor point and
 * the previous anchor point. This allows custom "paste trails" to be created.
 *
 * @author Yulin Li
 * @author Jeff Yoshimi
 */
class PlacementManager {

    /**
     * Offsets associated with specific types of objects.
     */
    object DefaultOffsets {
        operator fun get(model: LocatableModel?) = when (model) {
            is Neuron -> point(45, 0)
            is NeuronArray -> point(0, -145)
            is NeuronGroup -> point(200, 50)
            else -> point(45, 0)
        }
    }

    /**
     * Tells you the location of the most recently placed object.
     */
    private var anchorPoint: () -> Point2D = { point(0, 0) }

    /**
     * Last used anchor point.
     */
    private var previousAnchorPoint: Point2D = point(0, 0)

    /**
     * Last location clicked on screen.
     */
    var lastClickedLocation: Point2D = point(0, 0)
        set(point) {
            field = point
            useLastClickedLocation = true
        }

    /**
     * Set to true when a location on the screen is clicked.
     */
    private var useLastClickedLocation = false

    /**
     * Second paste after changing location
     */
    private var secondPaste = false

    /**
     * Set to true right after "copying". Allows pastes to grow out from whatever objects were just copied.
     */
    private var newCopy = false

    /**
     * Add a new model object and use default offsets.
     */
    fun addNewModelObject(model: LocatableModel): Point2D {
        previousAnchorPoint = anchorPoint()
        val nextLocation: Point2D
        if (useLastClickedLocation) {
            nextLocation = lastClickedLocation
            useLastClickedLocation = false
        } else { // Use "default" offset
            nextLocation = anchorPoint() + DefaultOffsets[model]
        }
        model.location = nextLocation
        anchorPoint = { model.location }
        return nextLocation
    }

    /**
     * Paste a list of objects and place it using the delta between the current anchor point and the
     * previous anchor point.
     */
    fun pasteObjects(models: List<LocatableModel>) {
        if (models.isEmpty()) {
            anchorPoint()
            return
        }
        val modelLocation = models::topLeftLocation
        val delta: Point2D
        if (useLastClickedLocation) { // Paste objects at last clicked location
            delta = lastClickedLocation - modelLocation()
            useLastClickedLocation = false
            secondPaste = true
        } else if (secondPaste) { // Location was changed during a paste trail
            val newLocation = (anchorPoint() - previousAnchorPoint) + lastClickedLocation
            delta = newLocation - modelLocation()
            previousAnchorPoint = lastClickedLocation
            anchorPoint = modelLocation
            secondPaste = false
        } else if (newCopy) { // Objects were just copied;  update the anchor point so paste trail grows from there.
            delta = anchorPoint() - previousAnchorPoint
            previousAnchorPoint = modelLocation()
            anchorPoint = modelLocation
            newCopy = false
        } else { // Standard case: Offset by delta between last and current anchor point
            val newLocation = anchorPoint() + (anchorPoint() - previousAnchorPoint)
            delta = newLocation - modelLocation()
            previousAnchorPoint = anchorPoint()
            anchorPoint = modelLocation
        }
        // Update the locations
        for (model in models) {
            model.location = model.location + delta
        }
    }

    /**
     * When an explicit location is needed. TODO: Phase out use of this method and remove when no longer called.
     */
    @Deprecated("")
    fun getLocationAndIncrement(): Point2D {
        val nextLocation: Point2D
        if (useLastClickedLocation) {
            nextLocation = anchorPoint()
            useLastClickedLocation = false
        } else {
            nextLocation = anchorPoint() + DefaultOffsets[null]
        }
        previousAnchorPoint = anchorPoint()
        anchorPoint = { nextLocation }
        return nextLocation
    }

    fun setNewCopy() {
        newCopy = true
    }
}

