/**
 * Message dialogs from [createMessageDialog] in each kind: a plain warning, a confirmation with detail rows, an
 * error with a heading, and a three-way choice. Also input dialogs from [createInputDialog], with and without a
 * rejected entry, and a results dialog from [createResultsDialog].
 */
package org.simbrain.util.uisnapshot

import org.simbrain.util.MessageKind
import org.simbrain.util.createInputDialog
import org.simbrain.util.createMessageDialog
import org.simbrain.util.createResultsDialog
import java.awt.Component
import java.awt.Container
import javax.swing.JButton
import javax.swing.JTextField

class MessageWarningSnapshot : UiSnapshotDef {
    override val name = "message_warning"
    override fun build(): Component = createMessageDialog(
        "Select at least one consuming and one producing attribute to create couplings.",
        kind = MessageKind.WARNING,
        heading = "No attributes selected"
    )
}

class MessageConfirmSnapshot : UiSnapshotDef {
    override val name = "message_confirm"
    override fun build(): Component = createMessageDialog(
        "The weights by Liquid AI, Inc. (~460 MB) will be downloaded from Hugging Face.\n\n" +
            "They are licensed under the LFM Open License v1.0, whose terms bind you as the downloader. " +
            "Commercial use is restricted for organizations above \$10M annual revenue.",
        options = listOf("Download", "Cancel"),
        kind = MessageKind.WARNING,
        heading = "Download LFM2.5-230M weights?",
        title = "Download Weights",
        details = listOf(
            "Destination" to "~/Library/Caches/Simbrain/lfm2.5-230m",
            "License" to "https://www.liquid.ai/lfm-license",
        )
    )
}

class MessageErrorSnapshot : UiSnapshotDef {
    override val name = "message_error"
    override fun build(): Component = createMessageDialog(
        "The checksum of model.safetensors does not match. The file may be incomplete or corrupted; " +
            "try downloading again.",
        kind = MessageKind.ERROR,
        heading = "Download failed",
        details = listOf("Expected" to "9f2c4e81a7d03b56", "Actual" to "41be07c95d2a8f13")
    )
}

class MessageChoiceSnapshot : UiSnapshotDef {
    override val name = "message_choice"
    override fun build(): Component = createMessageDialog(
        "Would you like to save these changes?",
        options = listOf("Save", "Don't Save", "Cancel"),
        kind = MessageKind.WARNING,
        heading = "The workspace has changed since it was last saved",
        title = "Workspace Has Changed"
    )
}

class MessageShortInfoSnapshot : UiSnapshotDef {
    override val name = "message_short_info"
    override fun build(): Component = createMessageDialog("3 occurrence(s) replaced.", title = "Replace All")
}

class InputTextSnapshot : UiSnapshotDef {
    override val name = "input_text"
    override fun build(): Component = createInputDialog("Name:", "Neuron Group 1", parse = { it })
}

class InputNumberErrorSnapshot : UiSnapshotDef {
    override val name = "input_number_error"
    override fun build(): Component {
        val dialog = createInputDialog(
            "Number of neurons",
            "10",
            error = "Enter a whole number.",
            parse = { it.trim().toIntOrNull() }
        )
        dialog.descendants<JTextField>().first().text = "12.5"
        dialog.descendants<JButton>().first { it.text == "OK" }.doClick()
        return dialog
    }
}

class ResultsSnapshot : UiSnapshotDef {
    override val name = "results"
    override fun build(): Component = createResultsDialog("Eigenvalues") {
        section("Matrix") {
            row("Size", "8 × 8")
            row("Spectral radius", "2.41")
        }
        section("Eigenvalues") {
            note("Largest magnitude first; complex values as a ± bi")
            row("1–4", "-2.41", "1.87±.92i", "1.87±.92i", "-1.33")
            row("5–8", ".98±1.02i", ".98±1.02i", "-.41", ".07")
        }
    }
}

private inline fun <reified T> Container.descendants(): List<T> = allDescendants().filterIsInstance<T>()

private fun Container.allDescendants(): List<Component> = components.flatMap { child ->
    listOf(child) + ((child as? Container)?.allDescendants() ?: listOf())
}
