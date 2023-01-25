package org.simbrain.world.textworld

import org.simbrain.util.*
import org.simbrain.util.table.Column
import org.simbrain.util.table.SimbrainDataViewer
import org.simbrain.util.table.createFromDoubleArray
import org.simbrain.world.textworld.gui.showComparisonDialog
import java.util.*

/**
 * Action for loading a dictionary, by finding every distinct word and
 * punctuation mark in a text file. TODO: Add more flexibility in terms of
 * parsing the loaded file.
 */
val TextWorld.extractDictionary get() = createAction(
    name = "Extract dictionary...",
    description = "Extract dictionary from text file...",
    iconPath = "menu_icons/Import.png"
) {
    val chooser = SFileChooser(dictionaryDirectory, "text file", "txt")
    val theFile = chooser.showOpenDialog()
    if (theFile != null) {
        loadDictionary(Utils.readFileContents(theFile))
    }
}

/**
 * Action for showing the vector dictionary editor, either the
 * token-to-vector dictionary used in readerworld or the vector-to-token
 * dictionary used in display world.
 */
val TextWorld.dictionaryEditor
    get() = createAction(
        name = "Edit dictionary...",
        description = "Edit dictionary...",
        iconPath = "menu_icons/Table.png"
    ) {
        // TODO: Find a way to show the tokens as rowHeaders
        // Find a way to make it immutable
        val model = createFromDoubleArray(tokenVectorMap.tokenVectorMatrix.replaceNaN(0.0).toArray())
        model.insertColumn(0, "Token", Column.DataType.StringType)
        tokenVectorMap.tokensMap.keys.forEachIndexed { rowIndex, token ->
            model.setValueAt(token, rowIndex, 0)
        }
        SimbrainDataViewer(model).displayInDialog().apply {
            title = "Dictionary with ${tokenVectorMap.size} unique entries"
        }
    }

/**
 * Action for showing the vector dictionary editor, either the
 * token-to-vector dictionary used in readerworld or the vector-to-token
 * dictionary used in display world.
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
                loadDictionary(text)
            }
        }.display()
    }

/**
 * Sets the current directory for the dictionary file (memory for file
 * chooser).
 *
 * @param dir directory to set
 */
var dictionaryDirectory: String?
    get() = SimbrainPreferences.getString("textWorldDictionaryDirectory")
    set(dir) {
        SimbrainPreferences.putString("textWorldDictionaryDirectory", dir)
    }