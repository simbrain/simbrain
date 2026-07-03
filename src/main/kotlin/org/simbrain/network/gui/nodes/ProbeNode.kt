package org.simbrain.network.gui.nodes

import kotlinx.coroutines.launch
import org.piccolo2d.event.PBasicInputEventHandler
import org.piccolo2d.event.PInputEvent
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.trainers.Probe
import org.simbrain.network.trainers.createShuffledControl
import org.simbrain.network.trainers.majorityClassProportion
import org.simbrain.network.trainers.rebuildStaleProbeDatasets
import org.simbrain.util.NetworkTheme
import org.simbrain.util.createAction
import org.simbrain.util.showInfoDialog
import org.simbrain.util.showYesNoDialog
import java.awt.Color
import javax.swing.JPopupMenu

class ProbeNode(networkPanel: NetworkPanel, val probe: Probe) : SupervisedModelNode(networkPanel, probe) {

    override val tabFill: Color get() = NetworkTheme.current.tabFillProbe

    override val removeActionName: String get() = "Remove Probe..."

    override fun setInfoTextNode(infoTextNode: ScreenElement) {
        super.setInfoTextNode(infoTextNode)
        infoTextNode.addInputEventListener(object : PBasicInputEventHandler() {
            override fun mouseClicked(event: PInputEvent) {
                if (!probe.stale || probe.rebuilding || probe.datasetRebuilder == null) return
                event.isHandled = true
                rebuildFromStaleWarning()
            }
        })
    }

    /**
     * Resolves a click on the "Stale: click to rebuild" line. When other probes are also stale,
     * offers to rebuild them all in one go; declining (or closing the dialog) rebuilds just this one.
     */
    private fun rebuildFromStaleWarning() {
        val network = networkPanel.network
        val otherStale = network.getModels(Probe::class.java)
            .filter { it !== probe && it.stale && it.datasetRebuilder != null }
        val rebuildAll = otherStale.isNotEmpty() && showYesNoDialog(
            "${otherStale.size} other probe${if (otherStale.size == 1) " is" else "s are"} also stale. " +
                    "Rebuild all ${otherStale.size + 1} datasets?",
            "Rebuild Probe Datasets"
        )
        network.launch {
            if (rebuildAll) network.rebuildStaleProbeDatasets() else probe.rebuildDataset()
        }
    }

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
                name = "Rebuild All Stale Probe Datasets",
                description = "Re-harvest datasets for every stale probe with a registered rebuilder",
                coroutineScope = networkPanel.network
            ) {
                networkPanel.network.rebuildStaleProbeDatasets()
            }.apply {
                isEnabled = networkPanel.network.getModels(Probe::class.java)
                    .any { it.stale && it.datasetRebuilder != null }
            })
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
