package org.simbrain.network.gui

import org.simbrain.network.core.Neuron
import org.simbrain.network.core.getSynapse
import org.simbrain.network.gui.dialogs.NetworkPreferences
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
        data = if (NetworkPreferences.weightMatrixTargetSource) {
            targets.map { target ->
                sources.map { source ->
                    getSynapse(source, target)?.strength as Any?
                }.toMutableList()
            }.toMutableList()
        } else {
            sourceToTargetSynapseMap.map {
                it.map {
                    it?.strength as Any?
                }.toMutableList()
            }.toMutableList()
        }
    ).apply {
        if (NetworkPreferences.weightMatrixTargetSource) {
            columnNames = sources.map { it.displayName }
            rowNames = targets.map { it.displayName }
        } else {
            columnNames = targets.map { it.displayName }
            rowNames = sources.map { it.displayName }
        }
    }

    val dataViewer = SimbrainTablePanel(dataModel).also {
        add(it)
    }

    fun commitChanges() {
        if (NetworkPreferences.weightMatrixTargetSource) {
            targets.forEachIndexed { targetIndex, target ->
                sources.forEachIndexed { sourceIndex, source ->
                    val value = dataModel.data[targetIndex][sourceIndex]
                    if (value is Double) {
                        getSynapse(source, target)?.strength = value
                    }
                }
            }
        } else {
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

    fun refreshValues() {
        dataModel.data = if (NetworkPreferences.weightMatrixTargetSource) {
            targets.map { target ->
                sources.map { source ->
                    getSynapse(source, target)?.strength as Any?
                }.toMutableList()
            }.toMutableList()
        } else {
            sourceToTargetSynapseMap.map {
                it.map {
                    it?.strength as Any?
                }.toMutableList()
            }.toMutableList()
        }
    }

}