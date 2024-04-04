package org.simbrain.world.textworld

import org.simbrain.util.*
import org.simbrain.world.textworld.gui.TextWorldDesktopComponent
import org.simbrain.world.textworld.gui.TokenEmbeddingDialog
import org.simbrain.world.textworld.gui.showComparisonDialog
import java.util.*

/**
 * Action for loading a token embedding, by finding every distinct word and
 * punctuation mark in a text file. TODO: Add more flexibility in terms of
 * parsing the loaded file.
 */
val TextWorld.extractEmbedding get() = createAction(
    name = "Extract embedding...",
    description = "Extract embedding from text file...",
    iconPath = "menu_icons/import.png"
) {
    val chooser = SFileChooser(tokenEmbeddingDirectory, "text file", "txt")
    val theFile = chooser.showOpenDialog()
    if (theFile != null) {
        val tokenEmbeddingBuilder = TokenEmbeddingBuilder()
        tokenEmbeddingBuilder.createEditorDialog {
            tokenEmbedding = tokenEmbeddingBuilder.build(Utils.readFileContents(theFile))
        }.display()
    }
}

fun createTrainEmbeddingAction(block: (TokenEmbedding) -> Unit) = createAction(
    name = "Train embedding...",
    description = "Train embedding on text file...",
    iconPath = "menu_icons/import.png"
) {
    val chooser = SFileChooser(tokenEmbeddingDirectory, "text file", "txt")
    val trainingDocument = chooser.showOpenDialog()
    if (trainingDocument != null) {
        val tokenEmbeddingBuilder = TokenEmbeddingBuilder()
        tokenEmbeddingBuilder.createEditorDialog {
            val tokenEmbedding = tokenEmbeddingBuilder.build(Utils.readFileContents(trainingDocument))
            tokenEmbedding.trainingDocument = Utils.readFileContents(trainingDocument)
            block(tokenEmbedding)
        }.display()
    }
}

/**
 * Action for viewing and editing the embedding.
 */
val TextWorld.viewTokenEmbedding
    get() = createAction(
        name = "View / edit token embedding...",
        description = "View token embedding (mapping from tokens to vectors)...",
        iconPath = "menu_icons/Table.png"
    ) {
        TokenEmbeddingDialog(tokenEmbedding) { tokenEmbedding = it }.display()
    }

/**
 * Load text into text world.
 */
val TextWorldDesktopComponent.loadTextAction
    get() = createAction(
        name = "Load text...",
        iconPath = "menu_icons/Import.png"
    ) {
        val chooser = SFileChooser(".", "Text import", "txt")
        val theFile = chooser.showOpenDialog()
        if (theFile != null) {
            workspaceComponent.world.text = Utils.readFileContents(theFile)
        }
    }

fun TextWorldDesktopComponent.createShowFindAndReplaceAction() = createAction(
    name = "Find / Replace...",
    keyboardShortcut = CmdOrCtrl + 'F'
) {
    panel.textArea.showFindReplaceDialog()
}

val TextWorld.calculateCosineSimilarity
    get() = createAction(
        name = "Calculate similarity",
        iconPath = "menu_icons/Gauge.png"
    ) {
        showComparisonDialog().display()
    }

val TextWorld.textWorldPrefs
    get() = createAction(
        name = "Show preferences...",
        iconPath = "menu_icons/Prefs.png"
    ) {
        createEditorDialog {}.also {
            it.title = "Text World Preferences"
        }.display()
    }

/**
 * Sets the current directory for token embedding files (memory for file chooser).
 *
 * @param dir directory to set
 */
var tokenEmbeddingDirectory: String?
    get() = TextWorldPreferences.tokenEmbeddingDirectory
    set(dir) {
        TextWorldPreferences.tokenEmbeddingDirectory = dir.toString()
    }