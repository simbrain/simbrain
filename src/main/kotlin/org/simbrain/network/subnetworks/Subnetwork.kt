/*
 * Copyright (C) 2005,2007 The Authors. See http://www.simbrain.net/credits This
 * program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.subnetworks

import org.simbrain.network.LocatableModel
import org.simbrain.network.NetworkModel
import org.simbrain.network.centerLocation
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkModelList
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.events.SubnetworkEvents
import org.simbrain.util.minus
import org.simbrain.util.plus
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer
import java.awt.geom.Point2D

/**
 * A collection of [org.simbrain.network.NetworkModel] objects which functions as a subnetwork within the main
 * root network, which (1) is shown in the GUI with an outline around it and a custom interaction box and (2) has
 * a potentially custom update rule.
 * <br></br>
 * Subclasses use [.addModel] to add models, and subclass
 * [org.simbrain.network.gui.nodes.SubnetworkNode] to customize the presentation, and override NetworkModel
 * methods as needed for custom behavior.
 */
abstract class Subnetwork : LocatableModel(), EditableObject, AttributeContainer {

    @Transient
    override val events: SubnetworkEvents = SubnetworkEvents()

    val modelList: NetworkModelList = NetworkModelList()

    /**
     * Whether the GUI should display neuron groups contained in this subnetwork. This will usually be true, but in
     * cases where a subnetwork has just one neuron group it is redundant to display both. So this flag indicates to the
     * GUI that neuron groups in this subnetwork need not be displayed.
     */
    private val displayNeuronGroups = true

    fun addModel(model: NetworkModel) {
        modelList.add(model)
        if (model is LocatableModel) {
            model.events.locationChanged.on {
                events.locationChanged.fire()
            }
        }
        events.locationChanged.fire()
        model.events.deleted.on(wait = true) {
            modelList.remove(it)
            if (modelList.size == 0) {
                delete()
            }
        }
    }

    fun addModels(models: List<NetworkModel>) {
        models.forEach { this.addModel(it) }
    }

    fun addModels(vararg models: NetworkModel) {
        for (model in models) {
            addModel(model)
        }
    }

    /**
     * Delete this subnetwork and its children.
     */
    override fun delete() {
        modelList.all.toList().forEach {
            modelList.remove(it)
            it.delete()
        }
        customInfo?.let { it.events.deleted.fire(it) }
        events.deleted.fire(this)
    }

    /**
     * A "flat" list containing every neuron in every neuron group in this subnetwork
     */
    val flatNeuronList: List<Neuron>
        get() = modelList[Neuron::class.java].toList()

    /**
     * A "flat" list containing every synapse in every synapse group in this subnetwork.
     */
    val flatSynapseList: List<Synapse>
        get() = modelList[Synapse::class.java].toList()

    override fun toString(): String {
        return """
            $id: ${javaClass.simpleName}
            ${modelList.toStringTabbed()}
            """.trimIndent()
    }

    /**
     * Default subnetwork update just updates all neuron and synapse groups. Subclasses with custom update should
     * override this.
     */
    context(Network)
    override fun update() {
        modelList.allInReconstructionOrder.forEach { it.update() }
    }

    private val locatableModels: List<LocatableModel>
        get() = modelList.all.filterIsInstance<LocatableModel>()

    override var location: Point2D
        get() = locatableModels.centerLocation
        set(newLocation) {
            val delta = newLocation - location
            locatableModels.forEach { it.location += delta }
        }

    /**
     * Optional information about the current state of the group. For display in GUI.
     */
    open val customInfo: NetworkModel? = null
}
