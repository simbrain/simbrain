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

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.simbrain.util.SFileChooser;
import org.simbrain.workspace.gui.CouplingMenuComponent;
import org.simbrain.workspace.gui.GenericFrame;
import org.simbrain.workspace.gui.GuiComponent;

/**
 * <b>TextWorldComponent</b> is the container for the world component. Handles
 * toolbar buttons, and serializing of world data. The main environment codes is
 * in {@link TextWorld}.
 */
public class TextWorldDesktopComponent extends GuiComponent<TextWorldComponent> {

    /** Script directory. */
    private static final String SCRIPT_MENU_DIRECTORY = "."
            + System.getProperty("file.separator") + "scripts"
            + System.getProperty("file.separator") + "scriptmenu";

    private static final long serialVersionUID = 1L;

    /** Default height. */
    private static final int DEFAULT_HEIGHT = 450;

    /** Default width. */
    private static final int DEFAULT_WIDTH = 500;

    /** Menu Bar. */
    private JMenuBar menuBar = new JMenuBar();

    /** File menu for saving and opening world files. */
    private JMenu file = new JMenu("File  ");

    /** Opens an existing world file. */
    private JMenuItem open = new JMenuItem("Open");

    /** Saves the world. */
    private JMenuItem save = new JMenuItem("Save");

    /** Saves the world as a new file name. */
    private JMenuItem saveAs = new JMenuItem("Save As");

    /** Closes the current world. */
    private JMenuItem close = new JMenuItem("Close");

    /** Edit menu Item. */
    private JMenu edit = new JMenu("Edit  ");

    /** Opens the dialog to define TextWorld Dictionary. */
    private JMenu dictionary = new JMenu("Dictionary");

    /** Opens the dialog to define TextWorld Dictionary. */
    private JMenuItem loadDictionary = new JMenuItem("Load dictionary");

    /** Opens user preferences dialog. */
    private JMenuItem preferences = new JMenuItem("Preferences");

    /** Opens the help dialog for TextWorld. */
    private JMenu help = new JMenu("Help");

    /** Current file. */
    private File scriptFile;

    /** The pane representing the text world. */
    private TextWorldPanel panel;

    /**
     * Creates a new frame of type TextWorld.
     */
    public TextWorldDesktopComponent(GenericFrame frame,
            TextWorldComponent component) {
        super(frame, component);

        panel = new TextWorldPanel(component.getWorld());
        panel.setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        this.setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        addMenuBar();
        add(panel);
        frame.pack();
    }

    @Override
    public void postAddInit() {
        super.postAddInit();
        this.getParentFrame().pack();
    }

    /**
     * Adds menu bar to the top of TextWorldComponent.
     */
    private void addMenuBar() {

        // open.setActionCommand("open");
        open.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                SFileChooser fileChooser = new SFileChooser(
                        SCRIPT_MENU_DIRECTORY, "Open file");
                scriptFile = fileChooser.showOpenDialog();
                TextWorldDesktopComponent.this.getParentFrame().setTitle(
                        scriptFile.getName());
                StringBuffer fileData = new StringBuffer(1000);
                BufferedReader reader;
                try {
                    reader = new BufferedReader(new FileReader(scriptFile));
                    char[] buf = new char[1024];
                    int numRead = 0;
                    while ((numRead = reader.read(buf)) != -1) {
                        String readData = String.valueOf(buf, 0, numRead);
                        fileData.append(readData);
                        buf = new char[1024];
                    }
                    reader.close();
                    // textArea.setText(fileData.toString());
                    panel.setInputText(fileData.toString());
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
        open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit
                .getDefaultToolkit().getMenuShortcutKeyMask()));
        save.setActionCommand("save");
        save.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                try {
                    BufferedWriter out = new BufferedWriter(new FileWriter(
                            scriptFile));
                    // out.write(textArea.getText());
                    out.close();
                } catch (IOException exception) {
                    System.out.println("Exception ");

                }
            }
        });
        save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit
                .getDefaultToolkit().getMenuShortcutKeyMask()));
        close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit
                .getDefaultToolkit().getMenuShortcutKeyMask()));
        menuBar.add(file);
        file.add(open);
        file.add(save);
        file.add(saveAs);
        file.add(close);

        loadDictionary.setActionCommand("loadDictionary");
        loadDictionary.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        // preferences.setActionCommand("prefs");
        preferences.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                panel.showTextWorldDialog();
            }
        });

        preferences.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuBar.add(edit);
        dictionary.add(loadDictionary);
        edit.add(dictionary);
        edit.add(preferences);

        menuBar.add(new CouplingMenuComponent("Couple", this.getWorkspaceComponent()
                .getWorkspace(), this.getWorkspaceComponent()));

        menuBar.add(help);

        getParentFrame().setJMenuBar(menuBar);
    }


    @Override
    public void closing() {
        // TODO Auto-generated method stub

    }

}
