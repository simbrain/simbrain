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

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.SimbrainPreferences;
import org.simbrain.util.SimbrainPreferences.PropertyNotFoundException;
import org.simbrain.util.propertyeditor.gui.ReflectivePropertyEditor;

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
    public static Action getExtractDictionaryAction(final DisplayWorld world) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Open.png"));
                putValue(NAME, "Extract dictionary...");
                putValue(SHORT_DESCRIPTION,
                        "Extract dictionary from text file...");
                //KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_L,
                //        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
                //putValue(ACCELERATOR_KEY, keyStroke);
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                SFileChooser chooser = new SFileChooser(
                        getDictionaryDirectory(), "text file", "txt");
                chooser.addExtension("rtf"); // (Must do other stuff to support
                // rich text)
                File theFile = chooser.showOpenDialog();
                if (theFile != null) {
                    // Adapted from
                    // http://www.javapractices.com/topic/TopicAction.do?Id=87
                    Scanner scanner;
                    try {
                        scanner = new Scanner(new FileReader(theFile));
                        try {
                            // first use a Scanner to get each line
                            while (scanner.hasNextLine()) {
                                Scanner lineScan = new Scanner(
                                        scanner.nextLine());
                                while (lineScan.hasNext()) {
                                    String word = lineScan.next();
                                    world.addWordToDictionary(word);
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
                            setDictionaryDirectory(chooser.getCurrentLocation());
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    /**
     * Action for showing the vector dictionary editor, used in reader world.
     *
     * @param world the world whose dictionary should be displayed
     * @return the action
     */
    public static Action showVectorDictionaryEditor(final ReaderWorld world) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Table.png"));
                putValue(NAME, "Edit dictionary...");
                putValue(SHORT_DESCRIPTION, "Display token > vector map");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                VectorDictionaryEditor dialog = VectorDictionaryEditor
                        .createVectorDictionaryEditor(world);
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);

            }
        };

    }

    /**
     * Action for showing the dictionary editor, used in display world.
     *
     * @param world the world whose dictionary should be displayed
     * @return the action
     */
    public static Action showDictionaryEditor(final DisplayWorld world) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Table.png"));
                putValue(NAME, "Edit dictionary...");
                putValue(SHORT_DESCRIPTION, "Display and edit dictionary");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                DictionaryEditor dialog = DictionaryEditor
                        .createDictionaryEditor(world);
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);

            }
        };
    }


    /**
     * Action for displaying a preference dialog.
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
                editor.setExcludeList(new String[] { "text", "position", "parseStyle" });
                editor.setUseSuperclass(false);
                editor.setObject(world);
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
        try {
            return SimbrainPreferences
                    .getString("textWorldDictionaryDirectory");
        } catch (PropertyNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

}
