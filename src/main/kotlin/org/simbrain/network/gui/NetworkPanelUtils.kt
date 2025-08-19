package org.simbrain.network.gui

import org.simbrain.network.core.LocatableModel
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.gui.nodes.ScreenElement
import org.simbrain.util.KeyCombination
import org.simbrain.util.createAction
import org.simbrain.util.format
import java.awt.event.ActionEvent
import kotlin.coroutines.EmptyCoroutineContext

/**
 * [createAction] that is conditionally enabled based on the state of the network, using [ConditionallyEnabledAction.EnablingCondition]
 * with a list of keyboard shortcuts.
 */
fun NetworkPanel.createConditionallyEnabledAction(
    iconPath: String? = null,
    name: String,
    enablingCondition: ConditionallyEnabledAction.EnablingCondition,
    description: String = name,
    keyboardShortcuts: List<KeyCombination>,
    block: suspend NetworkPanel.(e: ActionEvent) -> Unit
) = this.createAction(
    name = name,
    description = description,
    iconPath = iconPath,
    keyboardShortcuts = keyboardShortcuts,
    initBlock = {
        fun updateAction() {
            isEnabled = selectionManager.checkEnablingFunction(enablingCondition)
        }
        updateAction()
        selectionManager.events.selection.on { _, _ -> updateAction() }
        selectionManager.events.sourceSelection.on { _, _ -> updateAction() }
    },
    coroutineScope = null,
    coroutineContext = EmptyCoroutineContext,
    block
)

/**
 * [createConditionallyEnabledAction] with one or no keyboard shortcut.
 */
fun NetworkPanel.createConditionallyEnabledAction(
    iconPath: String? = null,
    name: String,
    enablingCondition: ConditionallyEnabledAction.EnablingCondition,
    description: String = name,
    keyboardShortcuts: KeyCombination? = null,
    block: suspend NetworkPanel.(e: ActionEvent) -> Unit
) = this.createConditionallyEnabledAction(
    iconPath,
    name,
    enablingCondition,
    description,
    keyboardShortcuts?.let { listOf(it) } ?: listOf(),
    block
)

inline fun <reified T: NetworkModel> NetworkPanel.filterSelectedModelByClass(): List<T> = selectionManager.selectedModels.filterIsInstance<T>()
inline fun <reified T: ScreenElement> NetworkPanel.filterSelectedNodeByClass(): List<T> = selectionManager.selection.filterIsInstance<T>()

fun createTooltipText(networkModel: NetworkModel, convertToHtml: Boolean = true, stringSupplier: (NetworkModel) -> String = { it.toString() }) = """
        <html>
        ${stringSupplier(networkModel).let { if (convertToHtml) it.split("\n").joinToString("<br>") else it }} <br>
        </html>
    """.trimIndent()

fun createTooltipTextWithLocation(locatableModel: LocatableModel, convertToHtml: Boolean = true, stringSupplier: (LocatableModel) -> String = { it.toString() }) = """
        <html>
        ${stringSupplier(locatableModel).let { if (convertToHtml) it.split("\n").joinToString("<br>") else it }} <br>
        Location: ${locatableModel.location.format(0)}
        </html>
    """.trimIndent()
