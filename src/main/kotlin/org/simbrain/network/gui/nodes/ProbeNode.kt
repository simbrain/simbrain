package org.simbrain.network.gui.nodes

import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.trainers.Probe
import org.simbrain.network.trainers.createShuffledControl
import org.simbrain.network.trainers.majorityClassProportion
import org.simbrain.util.NetworkTheme
import org.simbrain.util.createAction
import org.simbrain.util.showInfoDialog
import java.awt.Color
import javax.swing.JPopupMenu

class ProbeNode(networkPanel: NetworkPanel, val probe: Probe) : SupervisedModelNode(networkPanel, probe) {

    override val tabFill: Color get() = NetworkTheme.current.tabFillProbe

    override val removeActionName: String get() = "Remove Probe..."

    override val contextMenu: JPopupMenu
        get() = super.contextMenu.apply {
            addSeparator()
            add(createAction(
                name = "Select Probed Layer",
                description = "Select the host layer this probe reads from"
            ) {
                networkPanel.selectionManager.clear()
                probe.probedModel.select()
            })
            add(createAction(
                name = "Rebuild Probe Dataset",
                description = "Re-harvest host activations into this probe's datasets",
                coroutineScope = networkPanel.network
            ) {
                probe.rebuildDataset()
            }.apply { isEnabled = probe.datasetRebuilder != null })
            add(createAction(
                name = "Add Shuffled-Label Control",
                description = "Create a copy of this probe trained on shuffled targets, to check for probe memorization",
                coroutineScope = networkPanel.network
            ) {
                with(networkPanel.network) { probe.createShuffledControl() }
            })
            add(createAction(name = "Probe Info...") {
                showInfoDialog(probeInfo(), "Probe: ${probe.displayName}")
            })
        }

    private fun probeInfo() = buildString {
        appendLine("Probed layer: ${probe.probedModel.displayName}")
        if (probe.targetDescription.isNotBlank()) appendLine("Targets: ${probe.targetDescription}")
        appendLine("Training rows: ${probe.trainingSet.size}, testing rows: ${probe.testingSet.size}")
        if (probe.trainingSet.targets.isNotEmpty()) {
            appendLine("Majority baseline (train): ${"%.1f".format(majorityClassProportion(probe.trainingSet.targets) * 100)}%")
        }
        if (probe.testingSet.targets.isNotEmpty()) {
            appendLine("Majority baseline (test): ${"%.1f".format(majorityClassProportion(probe.testingSet.targets) * 100)}%")
        }
        append(if (probe.stale) "Dataset is STALE: host weights changed since the last harvest." else "Dataset is up to date.")
    }
}
