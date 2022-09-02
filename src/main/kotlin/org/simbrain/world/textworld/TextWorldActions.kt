/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.world.textworld

import org.simbrain.util.*
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.table.Column
import org.simbrain.util.table.SimbrainDataViewer
import org.simbrain.util.table.createFromDoubleArray
import smile.math.matrix.Matrix
import java.awt.event.ActionEvent
import java.util.*
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JDialog

/**
 * Contains actions relating to Text World.
 *
 * @author jyoshimi
 */
object TextWorldActions {

    /**
     * Action for loading a dictionary, by finding every distinct word and
     * punctuation mark in a text file. TODO: Add more flexibility in terms of
     * parsing the loaded file.
     *
     * @param world the world whose dictionary should be loaded
     * @return the action
     */
    @JvmStatic
    fun getExtractDictionaryAction(world: TextWorld): Action {
        return object : AbstractAction() {
            // Initialize
            init {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Import.png"))
                putValue(NAME, "Extract dictionary...")
                putValue(SHORT_DESCRIPTION, "Extract dictionary from text file...")
                //KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_L,
                //        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
                //putValue(ACCELERATOR_KEY, keyStroke);
            }

            override fun actionPerformed(arg0: ActionEvent) {
                val chooser = SFileChooser(dictionaryDirectory, "text file", "txt")
                val theFile = chooser.showOpenDialog()
                if (theFile != null) {
                    val docString = Utils.readFileContents(theFile)
                    val wordList = uniqueTokensFromArray(tokenizeWordsFromSentence(docString))
                    // TODO: Temp, but can use this as a 'one-hot' style for loading
                    world.tokenVectorMap = TokenVectorMap(wordList, Matrix.eye(wordList.size))
                }
            }
        }
    }

    /**
     * Action for showing the vector dictionary editor, either the
     * token-to-vector dictionary used in readerworld or the vector-to-token
     * dictionary used in display world.
     *
     * @param world the world whose dictionary should be displayed
     * @return the action
     */
    @JvmStatic
    fun showDictionaryEditor(world: TextWorld?): Action {
        return object : AbstractAction() {
            // Initialize
            init {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Table.png"))
                putValue(NAME, "Edit dictionary...")
                putValue(SHORT_DESCRIPTION, "Edit dictionary...")
            }

            override fun actionPerformed(arg0: ActionEvent) {
                // TODO: Find a way to show the tokens as rowHeaders
                // Find a way to make it immutable
                val model = createFromDoubleArray(world!!.tokenVectorMap.tokenVectorMatrix.toArray())
                model.insertColumn(0, "Token", Column.DataType.StringType)
                world.tokenVectorMap.tokensMap.keys.forEachIndexed {
                    rowIndex, token -> model.setValueAt(token, rowIndex, 0 )
                }
                SimbrainDataViewer(model).displayInDialog()

            }
        }
    }

    /**
     * Action for loading text in to a display or reader world.
     *
     * @param world the world in to which text should be loaded
     * @return the action
     */
    @JvmStatic
    fun getTextAction(world: TextWorld): Action {
        return object : AbstractAction() {
            // Initialize
            init {
                putValue(NAME, "Load text...")
                putValue(SHORT_DESCRIPTION, "Load text.")
            }

            override fun actionPerformed(arg0: ActionEvent) {
                val chooser = SFileChooser(".", "Text import", "txt")
                val theFile = chooser.showOpenDialog()
                if (theFile != null) {
                    world.text = Utils.readFileContents(theFile)
                }
            }
        }
    }

    /**
     * Action for displaying a default preference dialog. (Not currently used).
     *
     * @param world the world for which a dialog should be shown
     * @return the action
     */
    @JvmStatic
    fun getShowPreferencesDialogAction(world: TextWorld?): Action {
        return object : AbstractAction() {
            // Initialize
            init {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Prefs.png"))
                putValue(NAME, "Preferences...")
                putValue(SHORT_DESCRIPTION, "Show preferences dialog")
            }

            override fun actionPerformed(arg0: ActionEvent) {
                val editor = AnnotatedPropertyEditor(world)
                val dialog: JDialog = editor.dialog
                dialog.setLocationRelativeTo(null)
                dialog.pack()
                dialog.isVisible = true
            }
        }
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
}