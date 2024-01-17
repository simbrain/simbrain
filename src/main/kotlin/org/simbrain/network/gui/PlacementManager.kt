package org.simbrain.network.gui

import org.simbrain.network.*
import org.simbrain.network.core.Neuron
import org.simbrain.network.groups.NeuronCollection
import org.simbrain.network.groups.Subnetwork
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.subnetworks.CompetitiveNetwork
import org.simbrain.network.subnetworks.Hopfield
import org.simbrain.util.plus
import org.simbrain.util.point
import java.awt.geom.Point2D
import kotlin.reflect.KClass

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
     * Location of the most recently placed object.
     */
    var anchorPoint = point(0.0, 0.0)

    /**
     * Last anchor point, which is used to compute [deltaDrag]
     */
    var previousAnchorPoint = point(0.0, 0.0)

    /**
     * For each object type, the offset to use between pastes, to sue when  repeatedly adding objects, which is
     * convenient for creating "paste trails". Initialized to defaults for each object type.
     */
    var deltaDragMap = mutableMapOf<KClass<out LocatableModel>, Point2D> (
        Neuron::class to point(45, 0),
        NeuronGroup::class to point(400, 0),
        NeuronArray::class to point(300,0),
        Hopfield::class to point(300,0),
        CompetitiveNetwork::class to point(300,0),
        Subnetwork::class to point(220,0))

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
    fun placeObjects(initModels: List<LocatableModel>) {

        // NeuronCollections should not be placed.
        val models = initModels.filter { it !is NeuronCollection }
        if (models.isEmpty()) {
            return
        }

        models.moveToOrigin()

        if (useLastClickedLocation) {
            // Reset the anchor to wherever was last clicked and put objects there
            anchorPoint = lastClickedLocation
            useLastClickedLocation = false
            offsetFromAnchor(models, point(0.0,0.0))
        } else {
            val offset = deltaDragMap.getOrDefault(models.first()::class, point(45,0))
            offsetFromAnchor(models, offset)
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

