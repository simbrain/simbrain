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

import java.awt.Toolkit;
import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;


/**
 * <b>OdorWorldFrameMenu</b>.
 */
public class OdorWorldFrameMenu extends JMenuBar implements MenuListener {
    /** Parent frame. */
    private OdorWorldFrame parentFrame;
    /** File menu. */
    private JMenu fileMenu = new JMenu("File  ");
    /** Save menu item. */
    private JMenuItem saveItem = new JMenuItem("Save");
    /** Save as menu item. */
    private JMenuItem saveAsItem = new JMenuItem("Save As");
    /** Open menu item. */
    private JMenuItem openItem = new JMenuItem("Open world");
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
    /** Script menu. */
    private JMenu scriptMenu = new JMenu("Script ");
    /** Script menu Item. */
    private JMenuItem scriptItem = new JMenuItem("Open script dialog");
    /** Help menu. */
    private JMenu helpMenu = new JMenu("Help");
    /** Help menu item. */
    private JMenuItem helpItem = new JMenuItem("World Help");

    /**
     * Odor world frame menu constructor.
     * @param frame Frame to create menu
     */
    public OdorWorldFrameMenu(final OdorWorldFrame frame) {
        parentFrame = frame;
    }

    /**
     * Sets up menus.
     */
    public void setUpMenus() {
        parentFrame.setJMenuBar(this);

        setUpFileMenu();

        setUpEditMenu();

        add(getScriptMenu());
        getScriptMenu().add(getScriptItem());
        getScriptItem().addActionListener(parentFrame);

        add(getHelpMenu());
        getHelpMenu().add(getHelpItem());
        getHelpItem().addActionListener(parentFrame);
    }

    /**
     * Sets up file menu items.
     */
    public void setUpFileMenu() {
        add(getFileMenu());
        getFileMenu().add(getOpenItem());
        getFileMenu().add(getSaveItem());
        getFileMenu().add(getSaveAsItem());
        getFileMenu().addSeparator();
        getFileMenu().add(getPrefsItem());
        getFileMenu().add(getClose());
        getFileMenu().addMenuListener(this);

        getClose().addActionListener(parentFrame);
        getClose().setAccelerator(KeyStroke.getKeyStroke(
                                                KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        getSaveItem().addActionListener(parentFrame);
        getSaveItem().setAccelerator(KeyStroke.getKeyStroke(
                                                       KeyEvent.VK_S,
                                                       Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        getSaveAsItem().addActionListener(parentFrame);
        getOpenItem().addActionListener(parentFrame);
        getOpenItem().setAccelerator(KeyStroke.getKeyStroke(
                                                       KeyEvent.VK_O,
                                                       Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        getPrefsItem().addActionListener(parentFrame);
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

        getCutItem().addActionListener(parentFrame.getWorld());
        getCutItem().setAccelerator(KeyStroke.getKeyStroke(
                                                      KeyEvent.VK_X,
                                                      Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        getCopyItem().addActionListener(parentFrame.getWorld());
        getCopyItem().setAccelerator(KeyStroke.getKeyStroke(
                                                       KeyEvent.VK_C,
                                                       Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        getPasteItem().addActionListener(parentFrame.getWorld());
        getPasteItem().setAccelerator(KeyStroke.getKeyStroke(
                                                        KeyEvent.VK_V,
                                                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        getClearAllItem().addActionListener(parentFrame.getWorld());
    }

    /**
     * Tasks to perform when menu selected.
     * @param e Menu event
     */
    public void menuSelected(final MenuEvent e) {
        if (e.getSource().equals(getFileMenu())) {
            if (parentFrame.isChangedSinceLastSave()) {
                getSaveItem().setEnabled(true);
            } else if (!parentFrame.isChangedSinceLastSave()) {
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
     * @param close The close to set.
     */
    public void setClose(final JMenuItem close) {
        this.close = close;
    }

    /**
     * @return Returns the close.
     */
    public JMenuItem getClose() {
        return close;
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
     * @param openItem The openItem to set.
     */
    public void setOpenItem(final JMenuItem openItem) {
        this.openItem = openItem;
    }

    /**
     * @return Returns the openItem.
     */
    public JMenuItem getOpenItem() {
        return openItem;
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

    /**
     * @param scriptItem The scriptItem to set.
     */
    public void setScriptItem(final JMenuItem scriptItem) {
        this.scriptItem = scriptItem;
    }

    /**
     * @return Returns the scriptItem.
     */
    public JMenuItem getScriptItem() {
        return scriptItem;
    }

    /**
     * @param scriptMenu The scriptMenu to set.
     */
    public void setScriptMenu(final JMenu scriptMenu) {
        this.scriptMenu = scriptMenu;
    }

    /**
     * @return Returns the scriptMenu.
     */
    public JMenu getScriptMenu() {
        return scriptMenu;
    }
}
