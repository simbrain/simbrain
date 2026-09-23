/**
 * Plans property editing from a snapshot of selected models. Menu descriptions and dialog dispatch share
 * these targets, including unified text/font editors. Trainers and generated info-text settings stay separate.
 */
package org.simbrain.network.gui

import org.simbrain.network.core.*
import org.simbrain.network.gui.dialogs.neuron.NeuronDialog
import org.simbrain.network.gui.dialogs.synapse.SynapseDialog
import org.simbrain.network.gui.dialogs.text.TextDialog
import org.simbrain.network.gui.nodes.WeightMatrixNode
import org.simbrain.network.llm.LanguageModel
import org.simbrain.network.llm.TinyLanguageModel
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.util.StandardDialog
import org.simbrain.util.createEditorDialog
import org.simbrain.util.propertyeditor.EditableObject

/** Property editing is independent of whether the model also has a trainer. */
internal val NetworkModel.supportsSelectionEditing: Boolean
    get() = when (this) {
        is InfoText -> false
        is NetworkTextObject -> true
        is Neuron, is Synapse, is GapJunction, is NeuronArray, is WeightMatrix,
        is ActivationSequence, is TensorLayer, is ConvolutionConnector, is PoolingConnector,
        is FlattenConnector, is NeuronCollection, is LanguageModel, is TinyLanguageModel,
        is Subnetwork -> true
        else -> false
    }

internal fun modelTypeNoun(model: NetworkModel, count: Int): String {
    val singular = when (model) {
        is InfoText -> "info text object"
        is NetworkTextObject -> "text object"
        else -> model.javaClass.simpleName
            .replace(Regex("(?<=[A-Z])(?=[A-Z][a-z])|(?<=[a-z0-9])(?=[A-Z])"), " ")
            .lowercase()
    }
    if (count == 1) return singular
    return when {
        singular.endsWith("matrix") -> singular.removeSuffix("x") + "ces"
        singular.endsWith("s") || singular.endsWith("x") || singular.endsWith("ch") -> singular + "es"
        singular.endsWith("y") && singular.length > 1 && singular[singular.lastIndex - 1] !in "aeiou" -> singular.dropLast(1) + "ies"
        else -> singular + "s"
    }
}

internal fun NetworkPanel.createSelectionEditDialog(model: NetworkModel): StandardDialog {
    val selected = selectionManager.selectedModels
    val targets = if (model in selected) selected.filter { it.javaClass == model.javaClass } else listOf(model)
    return SelectionEditPlan(targets).createDialogs(this).single()
}

internal class SelectionEditPlan(models: Collection<NetworkModel>) {
    private val partition = models.distinct().partition { it.supportsSelectionEditing }
    val groups: List<List<NetworkModel>> = partition.first.groupBy { it.javaClass }.values
        .sortedByDescending { it.size }
    val excluded: List<NetworkModel> = partition.second

    private fun List<NetworkModel>.description() = "$size ${modelTypeNoun(first(), size)}"

    val description: Pair<String?, String>
        get() {
            val parts = groups.map { it.description() }
            val label = when (parts.size) {
                0 -> null
                1 -> "Edit ${parts[0]}..."
                2 -> "Edit ${parts[0]} and ${parts[1]}..."
                else -> "Edit ${groups.sumOf { it.size }} models of ${groups.size} types..."
            }
            val tooltip = buildString {
                append("<html>")
                if (parts.isEmpty()) append("Nothing selected supports selection editing")
                else {
                    append("Edit ${parts.joinToString(", ")}")
                    if (groups.size > 1) append("<br>Opens ${groups.size} dialogs, one per model type")
                }
                if (excluded.isNotEmpty()) {
                    append("<br>Not included: ")
                    append(excluded.groupBy { it.javaClass }.values.joinToString(", ") { it.description() })
                }
                append("</html>")
            }
            return label to tooltip
        }

    fun createDialogs(panel: NetworkPanel): List<StandardDialog> = groups.map { models ->
        when (val first = models.first()) {
            is NetworkTextObject -> TextDialog(models.filterIsInstance<NetworkTextObject>())
            is Neuron -> NeuronDialog(models.filterIsInstance<Neuron>())
            is Synapse -> requireNotNull(SynapseDialog.createSynapseDialog(models.filterIsInstance<Synapse>()))
            is WeightMatrix -> requireNotNull(
                (panel.getNode(first) as WeightMatrixNode).createEditDialog(models.filterIsInstance<WeightMatrix>())
            )
            else -> models.map { it as EditableObject }.createEditorDialog(
                titleName = if (models.size == 1) "Edit ${first.displayName}" else "Edit ${models.description()}"
            )
        }
    }
}
