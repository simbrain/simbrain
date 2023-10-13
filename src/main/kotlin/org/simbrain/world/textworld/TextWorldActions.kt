package org.simbrain.world.textworld

import org.simbrain.util.*
import org.simbrain.util.table.SimbrainDataViewer
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
    iconPath = "menu_icons/Import.png"
) {
    val chooser = SFileChooser(tokenEmbeddingDirectory, "text file", "txt")
    val theFile = chooser.showOpenDialog()
    if (theFile != null) {
        extractEmbedding(Utils.readFileContents(theFile))
    }
}

/**
 * Action for viewing and editing the embedding.
 */
val TextWorld.viewTokenEmbedding
    get() = createAction(
        name = "View embedding...",
        description = "View token embedding (mapping from tokens to vectors)...",
        iconPath = "menu_icons/Table.png"
    ) {

        val viewer = SimbrainDataViewer(tokenEmbedding.createTableModel(embeddingType).apply {
            isMutable = false
        })
        viewer.displayInDialog().apply {
            title = "Embedding has ${tokenEmbedding.size} unique entries"
        }
        // TODO: Use this when reintroducing editable embeddings
        // events.tokenVectorMapChanged.on {
        //     viewer.model = tokenVectorMap.createTableModel(embeddingType)
        // }
    }

/**
 * Load text into text world.
 */
val TextWorld.loadText
    get() = createAction(
        name = "Load text...",
        iconPath = "menu_icons/Import.png"
    ) {
        val chooser = SFileChooser(".", "Text import", "txt")
        val theFile = chooser.showOpenDialog()
        if (theFile != null) {
            text = Utils.readFileContents(theFile)
        }
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
        createDialog {}.also {
            it.title = "Text World Preferences"
            it.addClosingTask {
                extractEmbedding(text)
            }
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