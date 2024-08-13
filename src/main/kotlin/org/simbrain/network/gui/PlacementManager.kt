package org.simbrain.network.gui

import org.simbrain.network.core.*
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.subnetworks.CompetitiveNetwork
import org.simbrain.network.subnetworks.Hopfield
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.util.format
import org.simbrain.util.plus
import org.simbrain.util.point
import java.awt.geom.Point2D
import kotlin.reflect.KClass

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
     * For each object type, the offset to use between pastes, to sue when  repeatedly adding objects, which is
     * convenient for creating "paste trails". Initialized to defaults for each object type.
     */
    var offsetMap = mutableMapOf<KClass<out LocatableModel>, Point2D> (
        Neuron::class to point(45, 0),
        NeuronGroup::class to point(400, 0),
        NeuronArray::class to point(300,0),
        Hopfield::class to point(300,0),
        CompetitiveNetwork::class to point(300,0),
        Subnetwork::class to point(220,0)
    )

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
            models.moveTo((lastSelectedModel?.location ?: point(0, 0)) + offsetMap[models.first()::class]!!)
        }

        lastSelectedModel = models.sortTopBottom().first()
    }

    private fun pointToString(point: Point2D?) = if (point != null) "(${point.x.format(2)}, ${point.y.format(2)})" else "null"

    private fun printState() {
        println("lastPlacedModel = ${pointToString(lastSelectedModel?.location)}, lastClickedLocation = ${pointToString(lastClickedLocation)}, useLastClickedLocation = $useLastClickedLocation")
    }

}

