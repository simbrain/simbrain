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
package org.simbrain.world.odorworld;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;

import org.simbrain.util.Utils;
import org.simbrain.workspace.actions.CloseComponentAction;
import org.simbrain.workspace.gui.CouplingMenuComponent;
import org.simbrain.world.odorworld.actions.OpenWorldAction;
import org.simbrain.world.odorworld.actions.SaveWorldAction;
import org.simbrain.world.odorworld.actions.SaveWorldAsAction;
import org.simbrain.world.odorworld.actions.ShowWorldPrefsAction;


/**
 * <b>OdorWorldFrameMenu</b>.
 */
public class OdorWorldFrameMenu extends JMenuBar {

    private static final long serialVersionUID = 1L;

    //TODO: Replace all this with actions.

    /** Parent frame. */
    private OdorWorldDesktopComponent parent;
    /** File menu. */
    private JMenu fileMenu = new JMenu("File  ");
    /** Save menu item. */
    private JMenuItem saveItem = new JMenuItem("Save");
    /** Save as menu item. */
    private JMenuItem saveAsItem = new JMenuItem("Save As");
    /** Preferences menu item. */
    private JMenuItem prefsItem = new JMenuItem("World preferences");
    /** Close menu item. */
    private JMenuItem close = new JMenuItem("Close");
    /** Edit menu. */
    private JMenu editMenu = new JMenu("Edit  ");
    /** Copy menu item. */
    private JMenuItem copyItem = new JMenuItem("Copy");
    /** Cut menu item. */
    private JMenuItem cutItem = new JMenuItem("Cut");
    /** Paste menu item. */
    private JMenuItem pasteItem = new JMenuItem("Paste");
    /** Clear all menu item. */
    private JMenuItem clearAllItem = new JMenuItem("Clear all entities");
    /** Help menu. */
    private JMenu helpMenu = new JMenu("Help");
    /** Help menu item. */
    private JMenuItem helpItem = new JMenuItem("World Help");

    /**
     * Odor world frame menu constructor.
     * @param frame Frame to create menu
     */
    public OdorWorldFrameMenu(final OdorWorldDesktopComponent frame) {
        parent = frame;
    }

    private final ActionListener saveListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            parent.save();
        }
    };

    private final ActionListener saveAsListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            parent.showSaveFileDialog();
        }
    };

    private final ActionListener helpItemListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            Utils.showQuickRef("World.html");
        }
    };

    /**
     * Sets up menus.
     */
    public void setUpMenus() {

        setUpFileMenu();
        setUpEditMenu();
        add(getHelpMenu());
        getHelpMenu().add(getHelpItem());
        getHelpItem().addActionListener(helpItemListener);
        add(new CouplingMenuComponent("Couple", parent.getWorkspaceComponent()
                .getWorkspace(), parent.getWorkspaceComponent()));
    }

    /**
     * Sets up file menu items.
     */
    public void setUpFileMenu() {
        add(getFileMenu());
        getFileMenu().add(new OpenWorldAction(parent));
        getFileMenu().add(new SaveWorldAction(parent));
        getFileMenu().add(new SaveWorldAsAction(parent));
        getFileMenu().addSeparator();
        getFileMenu().add(new ShowWorldPrefsAction(parent.getWorldPanel()));
        getFileMenu().add(new CloseComponentAction(parent.getWorkspaceComponent()));
    }

    /**
     * Sets up edit menu items.
     */
    public void setUpEditMenu() {
        add(getEditMenu());

        getEditMenu().add(getCutItem());
        getEditMenu().add(getCopyItem());
        getEditMenu().add(getPasteItem());
        getEditMenu().addSeparator();
        getEditMenu().addSeparator();
        getEditMenu().add(getClearAllItem());

//        getCutItem().addActionListener(parentFrame.getWorldPanel().cutListener);
//        getCutItem().setAccelerator(KeyStroke.getKeyStroke(
//                                                      KeyEvent.VK_X,
//                                                      Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
//        getCopyItem().addActionListener(parentFrame.getWorldPanel().copyListener);
//        getCopyItem().setAccelerator(KeyStroke.getKeyStroke(
//                                                       KeyEvent.VK_C,
//                                                       Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
//        getPasteItem().addActionListener(parentFrame.getWorldPanel().pasteListener);
//        getPasteItem().setAccelerator(KeyStroke.getKeyStroke(
//                                                        KeyEvent.VK_V,
//                                                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
//        getClearAllItem().addActionListener(parentFrame.getWorldPanel().clearAllListener);
    }

    /**
     * Tasks to perform when menu selected.
     * @param e Menu event
     */
    public void menuSelected(final MenuEvent e) {
        if (e.getSource().equals(getFileMenu())) {
            if (parent.getWorkspaceComponent().hasChangedSinceLastSave()) {
                getSaveItem().setEnabled(true);
            } else if (!parent.getWorkspaceComponent().hasChangedSinceLastSave()) {
                getSaveItem().setEnabled(false);
            }
        }
    }

    /**
     * Tasks to perform when menu deselected.
     * @param arg0 Menu event
     */
    public void menuDeselected(final MenuEvent arg0) {
    }

    /**
     * Tasks to perform when menu is cancled.
     * @param arg0 Menu event
     */
    public void menuCanceled(final MenuEvent arg0) {
    }

    /**
     * @param clearAllItem The clearAllItem to set.
     */
    public void setClearAllItem(final JMenuItem clearAllItem) {
        this.clearAllItem = clearAllItem;
    }

    /**
     * @return Returns the clearAllItem.
     */
    public JMenuItem getClearAllItem() {
        return clearAllItem;
    }

    /**
     * @param copyItem The copyItem to set.
     */
    public void setCopyItem(final JMenuItem copyItem) {
        this.copyItem = copyItem;
    }

    /**
     * @return Returns the copyItem.
     */
    public JMenuItem getCopyItem() {
        return copyItem;
    }

    /**
     * @param cutItem The cutItem to set.
     */
    public void setCutItem(final JMenuItem cutItem) {
        this.cutItem = cutItem;
    }

    /**
     * @return Returns the cutItem.
     */
    public JMenuItem getCutItem() {
        return cutItem;
    }

    /**
     * @param editMenu The editMenu to set.
     */
    public void setEditMenu(final JMenu editMenu) {
        this.editMenu = editMenu;
    }

    /**
     * @return Returns the editMenu.
     */
    public JMenu getEditMenu() {
        return editMenu;
    }

    /**
     * @param fileMenu The fileMenu to set.
     */
    public void setFileMenu(final JMenu fileMenu) {
        this.fileMenu = fileMenu;
    }

    /**
     * @return Returns the fileMenu.
     */
    public JMenu getFileMenu() {
        return fileMenu;
    }

    /**
     * @param helpItem The helpItem to set.
     */
    public void setHelpItem(final JMenuItem helpItem) {
        this.helpItem = helpItem;
    }

    /**
     * @return Returns the helpItem.
     */
    public JMenuItem getHelpItem() {
        return helpItem;
    }

    /**
     * @param helpMenu The helpMenu to set.
     */
    public void setHelpMenu(final JMenu helpMenu) {
        super.setHelpMenu(helpMenu);
        this.helpMenu = helpMenu;
    }

    /**
     * @return Returns the helpMenu.
     */
    public JMenu getHelpMenu() {
        return helpMenu;
    }

    /**
     * @param pasteItem The pasteItem to set.
     */
    public void setPasteItem(final JMenuItem pasteItem) {
        this.pasteItem = pasteItem;
    }

    /**
     * @return Returns the pasteItem.
     */
    public JMenuItem getPasteItem() {
        return pasteItem;
    }

    /**
     * @param prefsItem The prefsItem to set.
     */
    public void setPrefsItem(final JMenuItem prefsItem) {
        this.prefsItem = prefsItem;
    }

    /**
     * @return Returns the prefsItem.
     */
    public JMenuItem getPrefsItem() {
        return prefsItem;
    }

    /**
     * @param saveAsItem The saveAsItem to set.
     */
    public void setSaveAsItem(final JMenuItem saveAsItem) {
        this.saveAsItem = saveAsItem;
    }

    /**
     * @return Returns the saveAsItem.
     */
    public JMenuItem getSaveAsItem() {
        return saveAsItem;
    }

    /**
     * @param saveItem The saveItem to set.
     */
    public void setSaveItem(final JMenuItem saveItem) {
        this.saveItem = saveItem;
    }

    /**
     * @return Returns the saveItem.
     */
    public JMenuItem getSaveItem() {
        return saveItem;
    }
}
