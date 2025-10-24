package org.simbrain.world.textworld

import org.simbrain.util.*
import org.simbrain.world.textworld.gui.TextWorldDesktopComponent
import org.simbrain.world.textworld.gui.TokenEmbeddingDialog
import org.simbrain.world.textworld.gui.showComparisonDialog
import java.io.File

/**
 * Action for loading a token embedding, by finding every distinct word and
 * punctuation mark in a text file. TODO: Add more flexibility in terms of
 * parsing the loaded file.
 */
fun createExtractEmbeddingAction(block: (TokenEmbedding) -> Unit) = createAction(
    name = "Extract embedding...",
    description = "Generate word embedding from selected document.",
    iconPath = "menu_icons/Extract.png"
) {
    val options = ExtractEmbeddingOptions()
    val editor = org.simbrain.util.propertyeditor.AnnotatedPropertyEditor(listOf(options))
    val dialog = StandardDialog(editor).apply {
        title = "Generate Word Embedding From Document"
        
        // Add validation to prevent dialog from closing if validation fails
        setClosingCheck {
            // Commit changes first so we can validate the updated values
            editor.commitChanges()
            
            if (options.documentPath.isEmpty()) {
                showWarningDialog("Please select a training document")
                false
            } else {
                val trainingDocument = File(options.documentPath)
                if (!trainingDocument.exists()) {
                    showWarningDialog("Selected file does not exist")
                    false
                } else {
                    true
                }
            }
        }
        
        addCommitTask {
            block(options.buildEmbedding())
        }
    }
    
    dialog.display()
}

/**
 * Action for viewing and editing the embedding.
 */
val TextWorld.viewTokenEmbedding
    get() = createAction(
        name = "Word embedding editor...",
        description = "View word embedding editor",
        iconPath = "menu_icons/TableBold.png"
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
        val chooser = SFileChooser(TextWorldPreferences.sampleTextsDirectory,"Text import", "txt")
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
        iconPath = "menu_icons/Tools.png"
    ) {
        createEditorDialog {
            events.preferencesChanged.fire()
        }.also {
            it.title = "Text World Preferences"
        }.display()
    }

/**
 * Sets the current directory for token embedding files (memory for file chooser).
 *
 * @param dir directory to set
 */
var tokenEmbeddingDirectory: String
    get() = TextWorldPreferences.tokenEmbeddingDirectory
    set(dir) {
        TextWorldPreferences.tokenEmbeddingDirectory = dir.toString()
    }