/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
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
    private var label = ""

    /**
     * Handle events.
     */
    @Transient
    private var events = SensorEffectorEvents()

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

    override fun getLabel(): String {
        return label
    }

    override fun setLabel(label: String) {
        this.label = label
    }

    abstract override fun copy(): Effector

    override fun getEvents(): SensorEffectorEvents {
        return events
    }

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
