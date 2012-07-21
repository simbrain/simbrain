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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.Scanner;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.ReflectivePropertyEditor;
import org.simbrain.util.table.TextTable;
import org.simbrain.util.table.SimbrainJTable;
import org.simbrain.util.table.SimbrainJTableScrollPanel;

/**
 * Contains actions relating to Text World.
 *
 * @author jyoshimi
 */
public class TextWorldActions {

    /** Directory where text files for dictionaries are stored. */
    private static String DICTIONARY_DIR = ".";

    /** User preference object. */
    private static final Preferences THE_PREFS = Preferences.userRoot().node(
            "org/simbrain/network/textworld");

    /**
     * Action for loading a dictionary, by finding every distinct word and
     * punctuation mark in a text file. TODO: Add more flexibility in terms of
     * parsing the loaded file.
     *
     * @param world the world whose dictionary should be loaded
     * @return the action
     */
    public static Action getLoadDictionaryAction(final TextWorld world) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Open.png"));
                putValue(NAME, "Load dictionary...");
                putValue(SHORT_DESCRIPTION,
                        "Extract dictionary from text file...");
                KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_L,
                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
                putValue(ACCELERATOR_KEY, keyStroke);
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {
                SFileChooser chooser = new SFileChooser(
                        getDictionaryDirectory(), "text file", "txt");
                chooser.addExtension("rtf"); //(Must do other stuff to support
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
     * Action for displaying the contents of the text world dictionary.
     *
     * @param world the world whose dictionary should be displayed
     * @return the action
     */
    public static Action getShowDictionaryAction(final TextWorld world) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Table.png"));
                putValue(NAME, "Display dictionary");
                putValue(SHORT_DESCRIPTION, "Display dictionary");
            }

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(ActionEvent arg0) {

                // Should really enable / disable the action depending
                // on if the dictionary is empty or not, but time is limited...
                if (world.getDictionary().size() == 0) {
                    return;
                }
                StandardDialog dialog = new StandardDialog();
                dialog.setTitle("View / Edit Dictionary");
                JPanel mainPanel = new JPanel(new BorderLayout());
                SimbrainJTable table = new SimbrainJTable(new TextTable(
                        world.getDictionary()));
                table.setShowCSVInPopupMenu(false);
                table.setShowDeleteColumnPopupMenu(false);
                table.setShowInsertColumnPopupMenu(false);
                table.setShowEditInPopupMenu(false);
                SimbrainJTableScrollPanel scroller = new SimbrainJTableScrollPanel(
                        table);
                JPanel toolbarPanel = new JPanel();
                toolbarPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
                toolbarPanel.add(table.getToolbarEditRows());
                mainPanel.add(toolbarPanel, BorderLayout.NORTH);
                scroller.setMaxVisibleRows(10);
                table.setDisplayColumnHeadings(false);
                mainPanel.add(scroller, BorderLayout.CENTER);

                // Display dialog
                dialog.setContentPane(mainPanel);
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
                if (!dialog.hasUserCancelled()) {
                    world.resetDictionary(table.getData().asFlatList());
                }
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
                editor.setExcludeList(new String[] { "text", "position" });
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
     * Sets the current dictionaryDirectory directory in user preferences
     * (memory for file chooser).
     *
     * @param dir directory to set
     */
    public static void setDictionaryDirectory(final String dir) {
        THE_PREFS.put("dictionaryDirectory", dir);
    }

    /**
     * Return the current dictionary directory.
     *
     * @return return the dictionaryDirectory directory
     */
    public static String getDictionaryDirectory() {
        return THE_PREFS.get("dictionaryDirectory", DICTIONARY_DIR);
    }

}
