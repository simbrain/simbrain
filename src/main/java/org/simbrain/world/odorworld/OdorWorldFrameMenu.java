package org.simbrain.world.odorworld;

import org.simbrain.util.SFileChooser;
import org.simbrain.util.piccolo.TMXUtils;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.gui.OdorWorldActions;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * <b>OdorWorldFrameMenu</b>.
 */
public class OdorWorldFrameMenu extends JMenuBar {

    private static final long serialVersionUID = 1L;

    // TODO: Replace all this with new actions.

    /**
     * Parent frame.
     */
    private final OdorWorldDesktopComponent parent;

    private final OdorWorldActions odorWorldActions;

    /**
     * File menu.
     */
    private final JMenu fileMenu = new JMenu("File  ");

    /**
     * Edit menu.
     */
    private final JMenu editMenu = new JMenu("Edit  ");

    /**
     * Copy menu item.
     */
    private final JMenuItem copyItem = new JMenuItem("Copy");

    /**
     * Cut menu item.
     */
    private final JMenuItem cutItem = new JMenuItem("Cut");

    /**
     * Paste menu item.
     */
    private final JMenuItem pasteItem = new JMenuItem("Paste");
    /**
     * Help menu.
     */
    private final JMenu helpMenu = new JMenu("Help");

    /**
     * Help menu item.
     */
    private final JMenuItem helpItem = new JMenuItem("World Help");

    /**
     * Reference to odor world.
     */
    private final OdorWorld world;

    /**
     * Odor world frame menu constructor.
     *
     * @param frame Frame to create menu
     * @param world
     */
    public OdorWorldFrameMenu(final OdorWorldDesktopComponent frame, OdorWorld world) {
        parent = frame;
        this.world = world;
        odorWorldActions = parent.getWorldPanel().getOdorWorldActions();
    }

    /**
     * Sets up menus.
     */
    public void setUpMenus() {

        setUpFileMenu();
        setUpEditMenu();

        // Help Menu
        add(helpMenu);
        ShowHelpAction helpAction = new ShowHelpAction("https://docs.simbrain.net/docs/worlds/odorworld.html");
        helpItem.setAction(helpAction);
        helpMenu.add(helpItem);
    }

    /**
     * Sets up file menu items.
     */
    public void setUpFileMenu() {
        add(fileMenu);
        fileMenu.add(SimbrainDesktop.INSTANCE.getActionManager().createImportAction(parent));
        fileMenu.add(SimbrainDesktop.INSTANCE.getActionManager().createExportAction(parent));
        fileMenu.addSeparator();

        fileMenu.add(new AbstractAction("Load Tile Map...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                SFileChooser chooser = new SFileChooser(OdorWorldPreferences.INSTANCE.getTileMapDirectory(), "Load TMX Tilemap", null, true);
                chooser.addExtension("tmx");
                File theFile = chooser.showOpenDialog();
                if (theFile != null) {
                    world.setTileMap(TMXUtils.loadTileMap(theFile));
                    OdorWorldPreferences.INSTANCE.setTileMapDirectory(chooser.getCurrentLocation());
                }
            }
        });

        fileMenu.add(odorWorldActions.clearTileMapAction());

        fileMenu.addSeparator();
        fileMenu.add(odorWorldActions.showWorldPrefsAction());
        fileMenu.addSeparator();
        fileMenu.add(SimbrainDesktop.INSTANCE.getActionManager().createRenameAction(parent));
        fileMenu.addSeparator();
        fileMenu.add(SimbrainDesktop.INSTANCE.getActionManager().createCloseAction(parent));
    }

    /**
     * Sets up edit menu items.
     */
    public void setUpEditMenu() {
        add(editMenu);

        //editMenu.add(cutItem);
        //editMenu.add(copyItem);
        //editMenu.add(pasteItem);
        //editMenu.addSeparator();

        // TODO: Factor the code for placing new entities out of network, to utils, and reuse here.
        JMenuItem addEntity = new JMenuItem("Add Entity");
        addEntity.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                world.addEntity();
            }
        });
        editMenu.add(addEntity);
        JMenuItem addAgent = new JMenuItem("Add Agent");
        addAgent.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                world.addAgent();
            }
        });
        editMenu.add(addAgent);
        editMenu.addSeparator();
        editMenu.add(odorWorldActions.deleteSelectedAction());
        editMenu.addSeparator();
        editMenu.add(odorWorldActions.getEditLayersAction());
        editMenu.addSeparator();

        editMenu.add(odorWorldActions.getToggleAllTrails());
        editMenu.add(odorWorldActions.getClearAllTrails());


    }

}
