package org.simbrain.network.gui

import org.simbrain.network.core.Neuron
import org.simbrain.network.core.getSynapse
import org.simbrain.util.table.BasicDataFrame
import org.simbrain.util.table.SimbrainTablePanel
import javax.swing.JPanel

class WeightMatrixViewer(val sources: List<Neuron>, val targets: List<Neuron>): JPanel() {

    private val sourceToTargetSynapseMap = sources.map { source ->
        targets.map { target ->
            getSynapse(source, target)
        }.toMutableList()
    }.toMutableList()

    val dataModel = BasicDataFrame(
        data = sourceToTargetSynapseMap.map {
            it.map {
                it?.strength as Any?
            }.toMutableList()
        }.toMutableList()
    ).apply {
        columnNames = targets.map { it.id }
        rowNames = sources.map { it.id }
    }

    val dataViewer = SimbrainTablePanel(dataModel).also {
        add(it)
    }

    fun commitChanges() {
        sourceToTargetSynapseMap.forEachIndexed { sourceIndex, source ->
            source.forEachIndexed { targetIndex, synapse ->
                val value = dataModel.data[sourceIndex][targetIndex]
                if (value is Double) {
                    synapse?.strength = value
                }
            }
        }
    }

}