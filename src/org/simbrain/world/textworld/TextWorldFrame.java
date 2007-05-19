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

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.simbrain.workspace.Workspace;

/**
 * <b>TextWorldFrame</b> is the container for the world component.   Handles toolbar buttons, and serializing of world
 * data.  The main environment codes is in {@link TextWorld}.
 */
public class TextWorldFrame extends JInternalFrame implements ActionListener,
        InternalFrameListener, MenuListener {

    /** File system seperator based on current operating system. */
    public static final String FS = System.getProperty("file.separator");
    /** Instance of world of type TextWorld. */
    private TextWorld world;
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
    /** Current directory. */
    private String currentDirectory = "." + FS + "simulations" + FS + "worlds";
    /** Instance of the TextWorld dictionary. */
    private Dictionary theDictionary;

    /**
     * Creates a new frame of type TextWorld.
     * @param ws Workspace to add frame to
     */
    public TextWorldFrame(final Workspace ws) {
        theDictionary = new Dictionary(this);
        init();
    }

    /**
     * Creates instance of text frame and sets parameters.
     */
    private void init() {

        this.setResizable(true);
        this.setMaximizable(true);
        this.setIconifiable(true);
        this.setClosable(true);
        this.addInternalFrameListener(this);
        world = new TextWorld(this);
        addMenuBar();
        getContentPane().add(world);
        setVisible(true);
        pack();
    }

    /**
     * Adds menu bar to the top of TextWorldFrame.
     */
    private void addMenuBar() {
        open.addActionListener(this);
        open.setActionCommand("open");
        open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        save.addActionListener(this);
        save.setActionCommand("save");
        save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        saveAs.addActionListener(this);
        saveAs.setActionCommand("saveAs");
        close.addActionListener(this);
        close.setActionCommand("close");
        close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuBar.add(file);
        file.add(open);
        file.add(save);
        file.add(saveAs);
        file.add(close);
        file.addMenuListener(this);

        loadDictionary.addActionListener(this);
        loadDictionary.setActionCommand("loadDictionary");
        loadDictionary.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        preferences.addActionListener(this);
        preferences.setActionCommand("prefs");
        preferences.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuBar.add(edit);
        dictionary.add(loadDictionary);
        edit.add(dictionary);
        edit.add(preferences);
        edit.addMenuListener(this);

        menuBar.add(help);

        setJMenuBar(menuBar);
    }

    /**
     * Sets the name of TextWorldFrame.
     * @param name Name of frame
     */
    public void setWorldName(final String name) {
        this.setTitle(name);
        world.setWorldName(name);

    }

    /**
     * Gets a particular instance of TextWorld.
     * @return TextWorld
     */
    public TextWorld getWorld() {
        return world;
    }

    /**
     * Responds to action events.
     * @param arg0 ActionEvent
     */
    public void actionPerformed(final ActionEvent arg0) {
        Object o = arg0.getActionCommand();
        if (o == "prefs") {
            world.showTextWorldDialog();
        }
        if (o == "loadDictionary") {
            theDictionary.loadDictionary();
        }
    }

    /**
     * Responds whan a frame is activated.
     * @param arg0 InternalFrameEvent
     */
    public void internalFrameActivated(final InternalFrameEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * Responds when an internal frame has closed.
     * @param arg0 InternalFrameEvent
     */
    public void internalFrameClosed(final InternalFrameEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * Responds when an internal frame is closing.
     * @param arg0 InternalFrameEvent
     */
    public void internalFrameClosing(final InternalFrameEvent arg0) {
        dispose();

    }

    /**
     * Responds when an internal frame is deactivated.
     * @param arg0 InternalFrameEvent
     */
    public void internalFrameDeactivated(final InternalFrameEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * Responds when an internal frame is deiconified.
     * @param arg0 InternalFrameEvent
     */
    public void internalFrameDeiconified(final InternalFrameEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * Responds when an internal frame is iconified.
     * @param arg0 InternalFrameEvent
     */
    public void internalFrameIconified(final InternalFrameEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * Responds when an internal frame is opened.
     * @param arg0 InternalFrameEvent
     */
    public void internalFrameOpened(final InternalFrameEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * Responds to menu item cancelation.
     * @param arg0 MenuEvent
     */
    public void menuCanceled(final MenuEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * Responds to menu deselection.
     * @param arg0 MenuEvent
     */
    public void menuDeselected(final MenuEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * Responds when menu is selected.
     * @param arg0 MenuEvent
     */
    public void menuSelected(final MenuEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * @return Returns the currentDirectory.
     */
    public String getCurrentDirectory() {
        return currentDirectory;
    }

    /**
     * @param currentDirectory The currentDirectory to set.
     */
    public void setCurrentDirectory(final String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

}
