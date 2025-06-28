package org.simbrain.world.odorworld.effectors

import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.world.odorworld.entities.PeripheralAttribute
import org.simbrain.world.odorworld.events.SensorEffectorEvents

/**
 * Abstract class for Odor World effectors.
 */
abstract class Effector : PeripheralAttribute {

    /**
     * The id of this smell effector.
     */
    // @UserParameter(label = "Effector ID", description = "A unique id for this effector",
    //         order = 0, displayOnly = true)
    override var id: String? = null

    private var _containerName: String? = null

    override var containerName: String?
        get() = _containerName
        set(value) {
            _containerName = value
        }

    /**
     * Public label of this effector.
     */
    @UserParameter(
        label = "Label",
        description = "Optional string description associated with this effector",
        order = 2
    )
    override var label = ""

    /**
     * Handle events.
     */
    @Transient
    override var events = SensorEffectorEvents()

    /**
     * Construct an effector.
     *
     * @param label  a label for this effector
     */
    constructor(label: String) : super() {
        this.label = label
    }

    /**
     * Construct a copy of an effector.
     *
     * @param effector the effector to copy
     */
    constructor(effector: Effector) : super() {
        this.label = effector.label
    }

    /**
     * Default no-arg constructor for [org.simbrain.util.propertyeditor.AnnotatedPropertyEditor].
     */
    constructor()

    abstract override fun copy(): Effector

    fun readResolve(): Any {
        events = SensorEffectorEvents()
        return this
    }

    override fun getTypeList(): List<Class<out CopyableObject>> = effectorList

}

val effectorList = listOf(
    Speech::class.java,
    StraightMovement::class.java,
    Turning::class.java
)
