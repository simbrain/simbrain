/** An interactive tutorial contrasting one-hot and overlapping object representations. */
package org.simbrain.custom_sims.simulations.tutorials

import org.simbrain.custom_sims.*
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.core.setLabels
import org.simbrain.util.SmellSource
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.sensors.SmellSensor

private enum class RepresentationStyle { Localist, Distributed }

private data class TutorialObject(
    val name: String,
    val entityType: EntityType,
    val location: Pair<Int, Int>,
    val distributedPattern: DoubleArray,
)

val localistAndDistributedRepresentations = newSim {
    workspace.clearWorkspace()

    val objects = listOf(
        TutorialObject("Gouda", EntityType.Gouda, 45 to 45, doubleArrayOf(1.0, 0.1, 0.7, 0.0, 0.2)),
        TutorialObject("Blue cheese", EntityType.BlueCheese, 290 to 45, doubleArrayOf(0.8, 1.0, 0.5, 0.1, 0.0)),
        TutorialObject("Fish", EntityType.Fish, 45 to 260, doubleArrayOf(0.2, 0.5, 1.0, 0.8, 0.1)),
        TutorialObject("Poison", EntityType.Poison, 290 to 260, doubleArrayOf(0.0, 0.2, 0.5, 1.0, 0.8)),
        TutorialObject("Pansy", EntityType.Pansy, 165 to 295, doubleArrayOf(0.5, 0.8, 0.1, 0.3, 1.0)),
    )

    val networkComponent = addNetworkComponent("Representation")
    val representation = networkComponent.network.addNeuronCollection(objects.size).apply {
        label = "Object units"
        betweenNeuronInterval = 65
        setLayoutBasedOnSize(point(0.0, 0.0))
        applyLayout()
        isClamped = true
        neuronList.forEach {
            it.lowerBound = 0.0
            it.upperBound = 1.0
        }
    }

    val worldComponent = addOdorWorldComponent("Objects")
    val world = worldComponent.world.apply {
        isObjectsBlockMovement = false
        isUseCameraCentering = false
    }
    val objectEntities = objects.map { objectInfo ->
        objectInfo to world.run {
            addEntity(objectInfo.location.first, objectInfo.location.second, objectInfo.entityType).apply {
                name = objectInfo.name
                smellSource = SmellSource(objectInfo.distributedPattern)
            }
        }
    }
    val smellSensor = SmellSensor("Smell-Center", theta = 0.0, radius = 0.0)
    world.addEntity(EntityType.Mouse).apply {
        location = point(190.0, 170.0)
        heading = 0.0
        addSensor(smellSensor)
    }

    var style = RepresentationStyle.Localist

    fun refreshRepresentation() {
        representation.label = if (style == RepresentationStyle.Localist) "Object units" else "Feature units"
        representation.setLabels(
            if (style == RepresentationStyle.Localist) objects.map(TutorialObject::name)
            else List(objects.size) { "Feature ${it + 1}" }
        )
        objectEntities.forEachIndexed { index, (objectInfo, entity) ->
            val pattern = when (style) {
                RepresentationStyle.Localist -> DoubleArray(objects.size) { if (it == index) 1.0 else 0.0 }
                RepresentationStyle.Distributed -> objectInfo.distributedPattern
            }
            entity.smellSource = SmellSource(pattern).apply { dispersion = 100.0 }
        }
    }

    refreshRepresentation()
    with(couplingManager) {
        smellSensor.getProducer(SmellSensor::smellVector) couple
            representation.getConsumer(NeuronCollection::setActivations)
    }

    addSidebarInfo(
        """
        # Localist and Distributed Representations

        This tutorial compares two ways a network can represent objects. A mouse begins in the center of the `Objects` window. Run the workspace and drag the mouse toward an object to see its smell-driven code in `Representation`.

        # Background

        **Localist representations** assign a dedicated unit to each object. This is a simplified version of the [grandmother-cell hypothesis](https://en.wikipedia.org/wiki/Grandmother_cell). Localist representations are easy to interpret but are subject to catastrophic damage: losing a unit removes the representation assigned to it. 

        **Distributed representations** use a pattern across shared units. This makes them potentially more tolerant of damage: changing or losing one unit may degrade several codes, but does not necessarily erase any one object completely. The trade-off is that interpreting a pattern requires a decoder and depends on how redundant and well-separated the codes are.

        Five binary units have `2^5 = 32` possible activity patterns, versus five one-hot localist codes. Real-valued units can express still more patterns. Not every mathematically possible pattern will be useful or reliably distinguishable, but shared units can provide substantially greater storage capacity. This is closely related to [superposition](https://www.anthropic.com/news/distributed-representations-composition-superposition), where more features are represented than there are individual dimensions. The [linear representation hypothesis](https://proceedings.mlr.press/v235/park24c.html) proposes that high-level features are represented as directions in activation space, so a pattern can be understood geometrically rather than as a list of dedicated detectors.

        # Simulation Details

        In a **localist** representation, one unit stands for one object. Moving the mouse close to `Fish`, for example, mainly activates only the unit labeled `Fish`. This one-hot code is easy to read, but each new object needs its own dedicated unit.

        In a **distributed** representation, every object is an activation pattern across shared feature units. Similar objects can share some active features, so a small collection of units can express many distinct patterns. The feature values here are fixed teaching examples, not learned values.

        ## Control Panel Settings

        - `Representation style`: Switches the odors received by the mouse between localist object codes and distributed feature patterns.

        # What to Do

        1. Run the workspace and drag the mouse toward each object. In localist mode, one matching object unit becomes most active.
        2. Select `Distributed` from the control panel and repeat. Several feature units now activate at different strengths.
        3. Compare nearby objects in distributed mode. Shared feature activity makes their codes partly overlap.
        """.trimIndent()
    )

    withGui {
        val controls = createControlPanel("Representation controls", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            addComboBox("Representation style", RepresentationStyle.entries, style, labelToolTip = "Switch between localist object codes and distributed feature patterns.") { selectedStyle ->
                style = selectedStyle
                refreshRepresentation()
            }.apply {
                toolTipText = "Switch between localist object codes and distributed feature patterns."
            }
        }.awaitLayout()
        place(networkComponent, controls.rightEdgeWithGap(), SIM_WINDOW_GAP, 500, 270)
        place(worldComponent, controls.rightEdgeWithGap(), SIM_WINDOW_GAP + 270 + SIM_WINDOW_GAP, 500, 430)
    }
}
