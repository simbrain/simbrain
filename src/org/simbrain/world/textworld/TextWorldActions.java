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
package org.simbrain.world.textworld;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.SimbrainPreferences;
import org.simbrain.util.Utils;
import org.simbrain.util.propertyeditor.gui.ReflectivePropertyEditor;
import org.simbrain.world.textworld.dictionary.DictionarySelector;
import org.simbrain.world.textworld.dictionary.TokenDictionaryPanel;
import org.simbrain.world.textworld.dictionary.TokenToVectorPanel;
import org.simbrain.world.textworld.dictionary.VectorToTokenPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Contains actions relating to Text World.
 *
 * @author jyoshimi
 */
public class TextWorldActions {

    /**
     * Action for loading a dictionary, by finding every distinct word and
     * punctuation mark in a text file. TODO: Add more flexibility in terms of
     * parsing the loaded file.
     *
     * @param world the world whose dictionary should be loaded
     * @return the action
     */
    public static Action getExtractDictionaryAction(final TextWorld world) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Import.png"));
                putValue(NAME, "Extract dictionary...");
                putValue(SHORT_DESCRIPTION, "Extract dictionary from text file...");
                //KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_L,
                //        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
                //putValue(ACCELERATOR_KEY, keyStroke);
            }

            @Override
            public void actionPerformed(ActionEvent arg0) {
                for (String word : extractTextItems()) {
                    world.addWordToTokenDictionary(word);
                }
                world.fireDictionaryChangedEvent();
            }
        };
    }

    public static Action getExtractDictionaryFromTextAction(final TextWorld world) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Import.png"));
                putValue(NAME, "Extract dictionary...");
                putValue(SHORT_DESCRIPTION, "Extract dictionary from text file...");
                //KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_L,
                //        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
                //putValue(ACCELERATOR_KEY, keyStroke);
            }

            @Override
            public void actionPerformed(ActionEvent arg0) {
                Scanner scanner = new Scanner(world.getText());
                List<String> words = parseText(scanner);
                for (String word : words) {
                    world.addWordToTokenDictionary(word);
                }
                world.fireDictionaryChangedEvent();
            }
        };
    }

    /**
     * Extract all the text items from a file.
     *
     * @return the text items as a list.
     */
    private static List<String> extractTextItems() {
        List<String> retList = new ArrayList<String>();
        SFileChooser chooser = new SFileChooser(getDictionaryDirectory(), "text file", "txt");
        chooser.addExtension("rtf"); // (Must do other stuff to support
        // rich text)
        File theFile = chooser.showOpenDialog();
        if (theFile != null) {
            try {
                Scanner scanner = new Scanner(theFile);
                parseText(scanner);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return retList;
    }

    private static List<String> parseText(Scanner scanner) {
        List<String> retList = new ArrayList<String>();
        // Adapted from
        // http://www.javapractices.com/topic/TopicAction.do?Id=87
        try {
            try {
                // first use a Scanner to get each line
                while (scanner.hasNextLine()) {
                    Scanner lineScan = new Scanner(scanner.nextLine());
                    while (lineScan.hasNext()) {
                        String word = lineScan.next();
                        retList.add(word);
                        // System.out.println("Entry is : " + word);
                    }
                }
            } finally {
                // Ensure the underlying stream is always closed
                // this only has any effect if the item passed to
                // the Scanner constructor implements Closeable
                // (which it does
                // in this case).
                scanner.close();
                return retList;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Action for showing the vector dictionary editor, either the
     * token-to-vector dictionary used in readerworld or the vector-to-token
     * dictionary used in display world.
     *
     * @param world the world whose dictionary should be displayed
     * @return the action
     */
    public static Action showDictionaryEditor(final TextWorld world) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Table.png"));
                putValue(NAME, "Edit dictionary...");
                putValue(SHORT_DESCRIPTION, "Edit dictionary...");
            }

            @Override
            public void actionPerformed(ActionEvent arg0) {
                TokenDictionaryPanel scalarPanel = new TokenDictionaryPanel(world);
                DictionarySelector dialog;
                if (world instanceof ReaderWorld) {
                    TokenToVectorPanel vectorPanel = new TokenToVectorPanel((ReaderWorld) world);
                    dialog = DictionarySelector.createVectorDictionaryEditor(scalarPanel, vectorPanel);
                } else {
                    VectorToTokenPanel vectorPanel = new VectorToTokenPanel((DisplayWorld) world);
                    dialog = DictionarySelector.createVectorDictionaryEditor(scalarPanel, vectorPanel);
                }
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);

            }
        };

    }

    /**
     * Action for loading text in to a display or reader world.
     *
     * @param world the world in to which text should be loaded
     * @return the action
     */
    public static Action getTextAction(final TextWorld world) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(NAME, "Load text...");
                putValue(SHORT_DESCRIPTION, "Load text.");
            }

            @Override
            public void actionPerformed(ActionEvent arg0) {
                SFileChooser chooser = new SFileChooser(".", "Text import", "txt");
                File theFile = chooser.showOpenDialog();
                if (theFile != null) {
                    world.setText(Utils.readFileContents(theFile));
                }
            }
        };
    }

    /**
     * Action for displaying a default preference dialog. (Not currently used).
     *
     * @param world the world for which a dialog should be shown
     * @return the action
     */
    public static Action getShowPreferencesDialogAction(final TextWorld world) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Prefs.png"));
                putValue(NAME, "Preferences...");
                putValue(SHORT_DESCRIPTION, "Show preferences dialog");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                ReflectivePropertyEditor editor = (new ReflectivePropertyEditor());
                editor.setUseSuperclass(false);
                editor.setObjectToEdit(world);
                JDialog dialog = editor.getDialog();
                dialog.setLocationRelativeTo(null);
                dialog.pack();
                dialog.setVisible(true);
            }
        };

    }

    /**
     * Sets the current directory for the dictionary file (memory for file
     * chooser).
     *
     * @param dir directory to set
     */
    public static void setDictionaryDirectory(final String dir) {
        SimbrainPreferences.putString("textWorldDictionaryDirectory", dir);
    }

    /**
     * Return the current directory for the dictionary file.
     *
     * @return return the dictionary directory
     */
    public static String getDictionaryDirectory() {
        return SimbrainPreferences.getString("textWorldDictionaryDirectory");
    }

}
