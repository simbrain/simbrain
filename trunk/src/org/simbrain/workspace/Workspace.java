/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.workspace;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.simbrain.gauge.GaugeFrame;
import org.simbrain.network.NetworkFrame;
import org.simbrain.network.nodes.NeuronNode;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.Utils;
import org.simbrain.world.Agent;
import org.simbrain.world.World;
import org.simbrain.world.dataworld.DataWorldFrame;
import org.simbrain.world.odorworld.OdorWorldFrame;
import org.simbrain.world.textworld.TextWorldFrame;
import org.simbrain.world.visionworld.VisionWorldFrame;
import org.simnet.coupling.Coupling;

/**
 * <b>Workspace</b> is the high-level container for all Simbrain windows--network, world, and gauge.  These components
 * are handled here, as are couplings and linkages between them.
 */
public class Workspace extends JFrame implements ActionListener, WindowListener, ComponentListener, MenuListener {

    /** Desktop pane. */
    private JDesktopPane desktop;

    /** Default workspace file to be opened upon initalization. */
    private static final String DEFAULT_FILE = WorkspacePreferences.getDefaultFile();

    /** Initial frame indent. */
    private static final int INITIAL_FRAME_INDENT = 100;

    /** Current workspace file. */
    private File currentFile = null;

    /** Current workspace directory. */
    private String currentDirectory = WorkspacePreferences.getCurrentDirectory();

    /** Network index. */
    private int netIndex = 1;

    /** Odor world index. */
    private int odorWorldIndex = 1;

    /** Data world index. */
    private int dataWorldIndex = 1;

    /** Gauge index. */
    private int gaugeIndex = 1;

    /** Vision world index. */
    private int visionWorldIndex = 1;

    /** Text world index. */
    private int textWorldIndex = 1;

    /** List of networks. */
    private ArrayList networkList = new ArrayList();

    /** List of odor worlds. */
    private ArrayList odorWorldList = new ArrayList();

    /** List of data worlds. */
    private ArrayList dataWorldList = new ArrayList();

    /** List of gauges. */
    private ArrayList gaugeList = new ArrayList();

    /** List of text worlds. */
    private ArrayList textWorldList = new ArrayList();

    /** List of vision worlds. */
    private ArrayList visionWorldList = new ArrayList();

    /** Default desktpo width. */
    private final int desktopWidth = 1500;

    /** Default desktop height. */
    private final int desktopHeight = 1500;

    /** Default window width. */
    private final int width = 450;

    /** Default window height. */
    private final int height = 450;

    /** Sentinal for determining if workspace has been changed since last save. */
    private boolean workspaceChanged = false;

    /** Save workspace menu item. */
    private JMenuItem saveItem = new JMenuItem("Save Workspace");

    /**
     * Default constructor.
     */
    public Workspace() {
        super("Simbrain");

        //Make the big window be indented 50 pixels from each edge
        //of the screen.
        int inset = 50;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(inset, inset, screenSize.width - (inset * 2), screenSize.height - (inset * 2));

        //Set up the GUI.
        desktop = new JDesktopPane(); //a specialized layered pane
        setJMenuBar(createMenuBar());
        desktop.setPreferredSize(new Dimension(desktopWidth, desktopHeight));

        JScrollPane workspaceScroller = new JScrollPane();
        setContentPane(workspaceScroller);
        workspaceScroller.setViewportView(desktop);
        workspaceScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        workspaceScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        addWindowListener(this);

        //Make dragging a little faster but perhaps uglier.
        //desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
    }


    /**
     * Build the menu bar.
     *
     * @return the menu bar
     */
    protected JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.addMenuListener(this);
        fileMenu.setMnemonic(KeyEvent.VK_D);
        menuBar.add(fileMenu);

        JMenu helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);

        JMenuItem menuItem = new JMenuItem("Open Workspace");
        menuItem.setMnemonic(KeyEvent.VK_O);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                                                       KeyEvent.VK_O,
                                                       Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.setActionCommand("openWorkspace");
        menuItem.addActionListener(this);
        fileMenu.add(menuItem);

        saveItem.setMnemonic(KeyEvent.VK_S);
        saveItem.setAccelerator(KeyStroke.getKeyStroke(
                                                       KeyEvent.VK_S,
                                                       Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        saveItem.setActionCommand("saveWorkspace");
        saveItem.addActionListener(this);
        fileMenu.add(saveItem);

        menuItem = new JMenuItem("Save Workspace As");
        menuItem.setActionCommand("saveWorkspaceAs");
        menuItem.addActionListener(this);
        fileMenu.add(menuItem);
        fileMenu.addSeparator();

        menuItem = new JMenuItem("Export Workspace");
        menuItem.setActionCommand("exportWorkspace");
        menuItem.addActionListener(this);
        fileMenu.add(menuItem);
        menuItem = new JMenuItem("Import Workspace");
        menuItem.setActionCommand("importWorkspace");
        menuItem.addActionListener(this);
        fileMenu.add(menuItem);
        fileMenu.addSeparator();

        menuItem = new JMenuItem("Clear Workspace");
        menuItem.setActionCommand("clearWorkspace");
        menuItem.addActionListener(this);
        fileMenu.add(menuItem);
        fileMenu.addSeparator();

        menuItem = new JMenuItem("New Network");
        menuItem.setMnemonic(KeyEvent.VK_N);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,
                                                       Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.setActionCommand("newNetwork");
        menuItem.addActionListener(this);
        fileMenu.add(menuItem);

        menuItem = new JMenuItem("New Gauge");
        menuItem.setActionCommand("newGauge");
        menuItem.setMnemonic(KeyEvent.VK_G);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G,
                                                       Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(this);
        fileMenu.add(menuItem);

        menuItem = new JMenu("New World");
        JMenuItem subMenuItem = new JMenuItem("OdorWorld");
        subMenuItem.addActionListener(this);
        subMenuItem.setActionCommand("newOdorWorld");
        menuItem.add(subMenuItem);

        subMenuItem = new JMenuItem("DataWorld");
        subMenuItem.addActionListener(this);
        subMenuItem.setActionCommand("newDataWorld");
        menuItem.add(subMenuItem);

//        subMenuItem = new JMenuItem("TextWorld");
//        subMenuItem.addActionListener(this);
//        subMenuItem.setActionCommand("newTextWorld");
//        menuItem.add(subMenuItem);

//        subMenuItem = new JMenuItem("VisionWorld");
//        subMenuItem.setActionCommand("newVisionWorld");
//        subMenuItem.addActionListener(this);
//        menuItem.add(subMenuItem);
        fileMenu.add(menuItem);

        fileMenu.addSeparator();

        menuItem = new JMenuItem("Quit");
        menuItem.setMnemonic(KeyEvent.VK_Q);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
                                                       Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.setActionCommand("quit");
        menuItem.addActionListener(this);
        fileMenu.add(menuItem);

        menuItem = new JMenuItem("Help");
        menuItem.setActionCommand("help");
        menuItem.addActionListener(this);
        helpMenu.add(menuItem);

        return menuBar;
    }

    /**
     *  React to menu selections.
     *
     * @param e Action event
     */
    public void actionPerformed(final ActionEvent e) {
        String cmd = e.getActionCommand();

        if (cmd.equals("newNetwork")) {
            addNetwork();
        } else if (cmd.equals("newOdorWorld")) {
            addOdorWorld();
        } else if (cmd.equals("newDataWorld")) {
            addDataWorld();
        } else if (cmd.equals("newVisionWorld")) {
            addVisionWorld();
        } else if (cmd.equals("newGauge")) {
            addGauge();
        } else if (cmd.equals("newTextWorld")) {
            addTextWorld();
        } else if (cmd.equals("clearWorkspace")) {
            clearWorkspace();
        } else if (cmd.equals("openWorkspace")) {
            showOpenDialog();
        } else if (cmd.equals("saveWorkspace")) {
            saveFile();
        } else if (cmd.equals("saveWorkspaceAs")) {
            this.showSaveFileAsDialog();
        } else if (cmd.equals("exportWorkspace")) {
            exportWorkspace();
        } else if (cmd.equals("importWorkspace")) {
            importWorkspace();
        } else if (cmd.equals("quit")) {
            if (changesExist()) {
                WorkspaceChangedDialog dialog = new WorkspaceChangedDialog(this);
                if (!dialog.hasUserCancelled()) {
                    quit();
                } else {
                    return;
                }
            }
            quit();
        } else if (cmd.equals("help")) {
            Utils.showQuickRef();
        }
    }

    //TODO Abstract "simbrain_frame" concept
    //        to eliminate redundant code following
    //      setBounds, initBounds, openFile, getPath...

    /**
     * Add a network to the workspace, to be initialized with default values.
     */
    public void addNetwork() {

        NetworkFrame network = new NetworkFrame();
        network.setTitle("Network " + netIndex++);
        network.getNetworkPanel().getNetwork().setWorkspace(this);

       //TODO: Check that network list does not contain this name
        if (networkList.size() == 0) {
            network.setBounds(5, 35, width, height);
        } else {
            int newx = ((NetworkFrame) networkList.get(networkList.size() - 1)).getBounds().x + 40;
            int newy = ((NetworkFrame) networkList.get(networkList.size() - 1)).getBounds().y + 40;
            network.setBounds(newx, newy, width, height);
        }

        addNetwork(network);
    }

    /**
     * Add a network to the workspace.
     *
     * @param network the networkFrame to add
     */
    public void addNetwork(final NetworkFrame network) {
        desktop.add(network);
        networkList.add(network);
        network.setVisible(true); //necessary as of 1.3

        try {
            network.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
            System.out.print(e.getStackTrace());
        }

        this.workspaceChanged = true;
        network.addComponentListener(this);
    }

    /**
     * Add a new world to the workspace, to be initialized with default values.
     */
    public void addOdorWorld() {
        OdorWorldFrame world = new OdorWorldFrame(this);
        world.getWorld().setWorldName("Odor World " + odorWorldIndex++);

        if (odorWorldList.size() == 0) {
            world.setBounds(505, 35, width, height);
        } else {
            int newx = ((OdorWorldFrame) odorWorldList.get(odorWorldList.size() - 1)).getBounds().x + 40;
            int newy = ((OdorWorldFrame) odorWorldList.get(odorWorldList.size() - 1)).getBounds().y + 40;
            world.setBounds(newx, newy, width, height);
        }

        world.getWorld().setParentWorkspace(this);

        addOdorWorld(world);
    }

    /**
     * Add a world to the workspace.
     *
     * @param world the worldFrame to add
     */
    public void addOdorWorld(final OdorWorldFrame world) {
        desktop.add(world);
        odorWorldList.add(world);
        world.setVisible(true);

        try {
            world.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
            e.printStackTrace();
        }

        this.workspaceChanged = true;
        world.addComponentListener(this);
    }

    /**
     * Add a new world to the workspace, to be initialized with default values.
     */
    public void addDataWorld() {
        DataWorldFrame world = new DataWorldFrame(this);
        world.getWorld().setWorldName("Data World " + dataWorldIndex++);

        if (dataWorldList.size() == 0) {
            world.setBounds(INITIAL_FRAME_INDENT, INITIAL_FRAME_INDENT, width, height);
        } else {
            int newx = ((DataWorldFrame) dataWorldList.get(dataWorldList.size() - 1)).getBounds().x + 40;
            int newy = ((DataWorldFrame) dataWorldList.get(dataWorldList.size() - 1)).getBounds().y + 40;
            world.setBounds(newx, newy, width, height);
        }

        world.resize();
        addDataWorld(world);
    }

    /**
     * Add a world to the workspace.
     * @param world the worldFrame to add
     */
    public void addDataWorld(final DataWorldFrame world) {
        desktop.add(world);
        dataWorldList.add(world);
        world.setVisible(true);
        try {
            world.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
            System.out.println(e.getStackTrace());
        }

        this.workspaceChanged = true;

        world.addComponentListener(this);
    }

    /**
     * Add a new world to the workspace, to be initialized with default values.
     */
    public void addVisionWorld() {
        VisionWorldFrame world = new VisionWorldFrame(this);
        world.getWorld().setName("Vision World " + visionWorldIndex++);

        if (visionWorldList.size() == 0) {
            world.setBounds(INITIAL_FRAME_INDENT, INITIAL_FRAME_INDENT, width, height);
        } else {
            int newx = ((VisionWorldFrame) visionWorldList.get(visionWorldList.size() - 1)).getBounds().x + 40;
            int newy = ((VisionWorldFrame) visionWorldList.get(visionWorldList.size() - 1)).getBounds().y + 40;
            world.setBounds(newx, newy, width, height);
        }
        addVisionWorld(world);
    }

    /**
     * Add a world to the workspace.
     * @param world the worldFrame to add
     */
    public void addVisionWorld(final VisionWorldFrame world) {
        desktop.add(world);
        visionWorldList.add(world);
        world.setVisible(true);
        try {
            world.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
            System.out.println(e.getStackTrace());
        }

        this.workspaceChanged = true;

        world.addComponentListener(this);
    }

    /**
     * Adds a new text world to the workspace.
     */
    public void addTextWorld() {
        TextWorldFrame world = new TextWorldFrame(this);
        world.getWorld().setWorldName("Text world " + textWorldIndex++);
        if (textWorldList.size() == 0) {
            world.setBounds(INITIAL_FRAME_INDENT, INITIAL_FRAME_INDENT, width, height);
        } else {
            int newx = ((TextWorldFrame) textWorldList.get(textWorldList.size() - 1)).getBounds().x + 40;
            int newy = ((TextWorldFrame) textWorldList.get(textWorldList.size() - 1)).getBounds().y + 40;
            world.setBounds(newx, newy, width, height);
        }
        addTextWorld(world);
    }

    /**
     * Adds a new text world to the workspace.
     * @param world Text world to add
     */
    public void addTextWorld(final TextWorldFrame world) {
        desktop.add(world);
        textWorldList.add(world);
        world.setVisible(true);
        try {
            world.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
            e.printStackTrace();
        }

        this.workspaceChanged = true;

        world.addComponentListener(this);
    }

    /**
     * Add a new gauge to the workspace, to be initialized with default values.
     */
    public void addGauge() {
        GaugeFrame gauge = new GaugeFrame(this);
        gauge.setName("Gauge " + gaugeIndex++);
        if (gaugeList.size() == 0) {
            gauge.setBounds(5, 490, 300, 300);
        } else {
            int newx = ((GaugeFrame) gaugeList.get(gaugeList.size() - 1)).getBounds().x + 310;
            int newy = ((GaugeFrame) gaugeList.get(gaugeList.size() - 1)).getBounds().y;
            gauge.setBounds(newx, newy, 300, 300);
        }

        addGauge(gauge);
    }

    /**
     * Add a gauge to the workspace.
     *
     * @param gauge the worldFrame to add
     */
    public void addGauge(final GaugeFrame gauge) {
        desktop.add(gauge);
        gaugeList.add(gauge);
        gauge.setVisible(true);

        try {
            gauge.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
            e.printStackTrace();
        }

        this.workspaceChanged = true;
        gauge.addComponentListener(this);
    }

    /**
     * @return reference to the last network added to this workspace
     */
    public NetworkFrame getLastNetwork() {
        if (networkList.size() > 0) {
            return (NetworkFrame) networkList.get(networkList.size() - 1);
        } else {
            return null;
        }
    }

    /**
     * @return reference to the last world added to this workspace
     */
    public OdorWorldFrame getLastOdorWorld() {
        if (odorWorldList.size() > 0) {
            return (OdorWorldFrame) odorWorldList.get(odorWorldList.size() - 1);
        } else {
            return null;
        }
    }

    /**
     * @return reference to the last world added to this workspace
     */
    public DataWorldFrame getLastDataWorld() {
        if (dataWorldList.size() > 0) {
            return (DataWorldFrame) dataWorldList.get(dataWorldList.size() - 1);
        } else {
            return null;
        }
    }

    /**
     * @return reference to the last gauge added to this workspace
     */
    public GaugeFrame getLastGauge() {
        if (gaugeList.size() > 0) {
            return (GaugeFrame) gaugeList.get(gaugeList.size() - 1);
        } else {
            return null;
        }
    }

    /**
     * Return the gauge associated with a network (by name), null otherwise.
     * @param networkName Name of network to associate gauge
     * @return Returns the gauge frame, null if there are no gauges open
     */
    public GaugeFrame getGaugeAssociatedWithNetwork(final String networkName) {
        for (int i = 0; i < getGaugeList().size(); i++) {
            GaugeFrame gauge = (GaugeFrame) getGaugeList().get(i);
            if (gauge.getGaugedVars().getNetworkName().equalsIgnoreCase(networkName)) {
                return gauge;
            }
        }
        return null;
    }

    /**
     * Return a named gauge, null otherwise.
     * @param name Name of gauge
     * @return Returns the gauge frame, null if there are no gauges open
     */
    public GaugeFrame getGauge(final String name) {
        for (int i = 0; i < getGaugeList().size(); i++) {
            GaugeFrame gauge = (GaugeFrame) getGaugeList().get(i);

            if (gauge.getName().equalsIgnoreCase(name)) {
                return gauge;
            }
        }

        return null;
    }

    /**
     * Get those gauges gauged by the given network.
     * @param net Network frame
     * @return Returns the array list of gauges
     */
    public ArrayList getGauges(final NetworkFrame net) {
        ArrayList ret = new ArrayList();

        for (int i = 0; i < gaugeList.size(); i++) {
            GaugeFrame gauge = (GaugeFrame) gaugeList.get(i);

            if (gauge.getGaugedVars().getNetworkName() != null) {
                if (gauge.getGaugedVars().getNetworkName().equals(net.getName())) {
                    ret.add(gauge);
                }
            }
        }

        return ret;
    }

    /**
     * Return a named network, null otherwise.
     * @param name Name of network
     * @return Returns the networks or null if no networks open
     */
    public NetworkFrame getNetwork(final String name) {
        for (int i = 0; i < getNetworkList().size(); i++) {
            NetworkFrame network = (NetworkFrame) getNetworkList().get(i);

            if (network.getTitle().equalsIgnoreCase(name)) {
                return network;
            }
        }

        return null;
    }

    /**
     * Remove all items (networks, worlds, etc.) from this workspace.
     */
    public void clearWorkspace() {
        if (changesExist()) {
            WorkspaceChangedDialog dialog = new WorkspaceChangedDialog(this);

            if (dialog.hasUserCancelled()) {
                return;
            }
        }

        disposeAllFrames();
        currentFile = null;
        this.setTitle("Simbrain");
    }

    /**
     * Disposes all frames.
     */
    public void disposeAllFrames() {
        netIndex = 1;
        dataWorldIndex = 1;
        odorWorldIndex = 1;
        gaugeIndex = 1;

        //TODO: Is there a cleaner way to do this?  I have to use this while loop
        // because the windowclosing itself removes a window
        while (networkList.size() > 0) {
            for (int i = 0; i < networkList.size(); i++) {
                try {
                    ((NetworkFrame) networkList.get(i)).setClosed(true);
                } catch (java.beans.PropertyVetoException e) {
                    System.out.println(e.getStackTrace());
                }
            }
        }

        while (odorWorldList.size() > 0) {
            for (int i = 0; i < odorWorldList.size(); i++) {
                try {
                    ((OdorWorldFrame) odorWorldList.get(i)).setClosed(true);
                } catch (java.beans.PropertyVetoException e) {
                    System.out.println(e.getStackTrace());
                }
            }
        }

        while (dataWorldList.size() > 0) {
            for (int i = 0; i < dataWorldList.size(); i++) {
                try {
                    ((DataWorldFrame) dataWorldList.get(i)).setClosed(true);
                } catch (java.beans.PropertyVetoException e) {
                    System.out.println(e.getStackTrace());
                }
            }
        }

        while (gaugeList.size() > 0) {
            for (int i = 0; i < gaugeList.size(); i++) {
                try {
                    ((GaugeFrame) gaugeList.get(i)).setClosed(true);
                } catch (java.beans.PropertyVetoException e) {
                    System.err.println(e);
                }
            }
        }
    }

    /**
     * Import a workspace.
     */
    public void importWorkspace() {
        showOpenFileDialog(true);
    }

    /**
     * Show the open workpace dialog.
     */
    public void showOpenDialog() {
        showOpenFileDialog(false);
    }

    /**
     * Shows the dialog for opening a workspace file.
     *
     * @param isImport whether the selected file will be imported or simply opened.
     */
    private void showOpenFileDialog(final boolean isImport) {

        if (changesExist()) {
            WorkspaceChangedDialog theDialog = new WorkspaceChangedDialog(this);

            if (theDialog.hasUserCancelled()) {
                return;
            }
        }
        workspaceChanged = false;

        SFileChooser simulationChooser = new SFileChooser(currentDirectory, "xml");
        File simFile = simulationChooser.showOpenDialog();

        if (simFile != null) {
            WorkspaceSerializer.readWorkspace(this, simFile, isImport);
            if (!isImport) {
                currentFile = simFile;
                currentDirectory = simulationChooser.getCurrentLocation();
                WorkspacePreferences.setCurrentDirectory(currentDirectory);
                WorkspacePreferences.setDefaultFile(currentFile.toString());
            }
        }

    }

    /**
     * Shows the dialog for saving a workspace file.
     */
    public void showSaveFileAsDialog() {
        SFileChooser simulationChooser = new SFileChooser(currentDirectory, "xml");
        workspaceChanged = false;

        if (changesExist()) {
            WorkspaceChangedDialog theDialog = new WorkspaceChangedDialog(this);

            if (theDialog.hasUserCancelled()) {
                return;
            }
        }

        File simFile = simulationChooser.showSaveDialog();

        if (simFile != null) {
            WorkspaceSerializer.writeWorkspace(this, simFile);
            currentFile = simFile;
            currentDirectory = simulationChooser.getCurrentLocation();
        }
    }

    /**
     * Export a workspace file: that is, save all workspace components and then a simple
     * workspace file correpsonding to them.
     */
    public void exportWorkspace() {
        SFileChooser chooser = new SFileChooser(currentDirectory, "xml");
        File simFile = chooser.showSaveDialog();
        String newDir = simFile.getName().substring(0, simFile.getName().length() - 4);
        String newDirPath = simFile.getParent() + FS + newDir;

        boolean success = new File(newDirPath).mkdir();
        if (!success) {
            return;
        }

        for (int i = 0; i < networkList.size(); i++) {
            NetworkFrame network = (NetworkFrame) networkList.get(i);
            String name = checkName(network.getTitle());
            File netFile = new File(newDirPath, name);
            network.getNetworkPanel().saveNetwork(netFile);
        }
        for (int i = 0; i < dataWorldList.size(); i++) {
            DataWorldFrame dataworld = (DataWorldFrame) dataWorldList.get(i);
            String name = checkName(dataworld.getTitle());
            File worldFile = new File(newDirPath, name);
            dataworld.saveWorld(worldFile);
         }
        for (int i = 0; i < odorWorldList.size(); i++) {
            OdorWorldFrame odorworld = (OdorWorldFrame) odorWorldList.get(i);
            String name = checkName(odorworld.getTitle());
            File worldFile = new File(newDirPath, name);
            odorworld.saveWorld(worldFile);
         }

        WorkspaceSerializer.writeWorkspace(this, new File(newDirPath + FS + simFile.getName()));            
    }

    String FS = System.getProperty("file.separator");

    /**
     * If the string does not have ".xml" add it.
     *
     * @param name the string to check
     * @return the checked string
     */
    private String checkName(final String name) {
        String ret = new String(name);
        if (!ret.endsWith("xml")) {
            ret += ".xml";
        }
        return ret;
    }

    /**
     * Show the save dialog.
     */
    public void saveFile() {
        workspaceChanged = false;

        if (changesExist()) {
            WorkspaceChangedDialog theDialog = new WorkspaceChangedDialog(this);

            if (theDialog.hasUserCancelled()) {
                return;
            }
        }

        if (currentFile != null) {
            WorkspaceSerializer.writeWorkspace(this, currentFile);
        } else {
            showSaveFileAsDialog();
        }
    }

    /**
     * Repaint all open network panels. Useful when workspace changes happen that need to be broadcast; also essential
     * when default workspace is initially opened.
     */
    public void repaintAllNetworks() {
        for (int j = 0; j < getNetworkList().size(); j++) {
            NetworkFrame net = (NetworkFrame) getNetworkList().get(j);
            net.getNetworkPanel().repaint();
        }
    }

    /**
     * Create the GUI and show it. For thread safety, this method should be invoked from the event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Make sure we have nice window decorations.
        //JFrame.setDefaultLookAndFeelDecorated(true);
        //Create and set up the window.
        Workspace sim = new Workspace();
        sim.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        //Display the window.
        sim.setVisible(true);

        //Open initial workspace
        WorkspaceSerializer.readWorkspace(sim, new File(DEFAULT_FILE), false);

    }

    /**
     * Simbrain main method.  Creates a single instance of the Simulation class
     *
     * @param args currently not used
     */
    public static void main(final String[] args) {
        try {
            //UIManager.setLookAndFeel(new MetalLookAndFeel());
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        createAndShowGUI();
                    }
                });
        } catch (Exception e) {
            System.err.println("Couldn't set look and feel!");
        }
    }

    /**
     * @return Returns the networkList.
     */
    public ArrayList getNetworkList() {
        return networkList;
    }

    /**
     * @param networkList The networkList to set.
     */
    public void setNetworkList(final ArrayList networkList) {
        this.networkList = networkList;
    }

    /**
     * @return Returns the worldFrameList.
     */
    public ArrayList getWorldFrameList() {
        ArrayList ret = new ArrayList();
        ret.addAll(odorWorldList);
        ret.addAll(dataWorldList);

        return ret;
    }

    /**
     * @return Returns the gaugeList.
     */
    public ArrayList getGaugeList() {
        return gaugeList;
    }

    /**
     * @param gaugeList The gaugeList to set.
     */
    public void setGaugeList(final ArrayList gaugeList) {
        this.gaugeList = gaugeList;
    }

    /**
     * Get a list of all agents in the workspace.
     *
     * @return the list of agents.
     */
    public ArrayList getAgentList() {
        ArrayList ret = new ArrayList();

        //Go through worlds, and get each of their agent lists
        for (int i = 0; i < getWorldList().size(); i++) {
            World wld = (World) getWorldList().get(i);
            ret.addAll(wld.getAgentList());
        }

        return ret;
    }

    /**
     * Returns a menu which shows what possible sources there are for motor couplings in this workspace.
     * @param al Action listener
     * @param theNode Neuron node
     * @return Returns the motor commands menu
     */
    public JMenu getMotorCommandMenu(final ActionListener al, final NeuronNode theNode) {
        JMenu ret = new JMenu("Motor Commands");

        for (int i = 0; i < getWorldFrameList().size(); i++) {
            World wld = (World) getWorldList().get(i);
            JMenu wldMenu = wld.getMotorCommandMenu(al);

            if (wldMenu == null) {
                continue;
            }

            ret.add(wldMenu);
        }

        JMenuItem notOutputItem = new JMenuItem("Not output");
        notOutputItem.addActionListener(al);
        notOutputItem.setActionCommand("Not output");

        if (theNode.getNeuron().isOutput()) {
            ret.add(notOutputItem);
        }

        return ret;
    }

    /**
     * Returns a menu which shows what possible sources there are for sensory couplings in this workspace.
     * @param al Action listener
     * @param theNode Neuron node
     * @return Returns the sensor id menu
     */
    public JMenu getSensorIdMenu(final ActionListener al, final NeuronNode theNode) {
        JMenu ret = new JMenu("Sensors");

        for (int i = 0; i < getWorldFrameList().size(); i++) {
            World wld = (World) getWorldList().get(i);
            JMenu wldMenu = wld.getSensorIdMenu(al);

            if (wldMenu == null) {
                continue;
            }

            ret.add(wldMenu);
        }

        JMenuItem notInputItem = new JMenuItem("Not input");
        notInputItem.addActionListener(al);
        notInputItem.setActionCommand("Not input");

        if (theNode.getNeuron().isInput()) {
            ret.add(notInputItem);
        }

        return ret;
    }

    /**
     * Returns a menu which shows what gauges are currently in the workspace Returns null if ther are no gauges.
     * @param al Action listener
     * @return Returns the gauge menu
     */
    public JMenu getGaugeMenu(final ActionListener al) {
        if (getGaugeList().size() == 0) {
            return null;
        }

        JMenu ret = new JMenu("Set Gauge");

        for (int i = 0; i < getGaugeList().size(); i++) {
            JMenuItem temp = new JMenuItem(((GaugeFrame) getGaugeList().get(i)).getName());
            temp.setActionCommand("Gauge:" + temp.getText());
            temp.addActionListener(al);
            ret.add(temp);
        }

        return ret;
    }

    /**
     * Associates a coupling with a matching agent in the current workspace.  Returns null if no such agent can be
     * found. This method is used when opening networks, to see if any agents match the network's current couplings.
     * 1) Try to find a matching world-type, world-name, and agent-name 2) Try to find a matching world-type and
     * agent-name 3) Try to find a matching world-type and any agent
     *
     * @param c a temporary coupling which holds an agent-name, agent-type, and world-name
     *
     * @return a matching agent, or null of none is found
     */
    public Agent findMatchingAgent(final Coupling c) {

        // For worlds without agents, set agent name to world name
        if (c.getAgentName() == null) {
            c.setAgentName(c.getWorldName());
        }

        //First go for a matching agent in the named world
        for (int i = 0; i < getWorldList().size(); i++) {
            World wld = (World) getWorldList().get(i);

            if (c.getWorldName().equals(wld.getName()) && (c.getWorldType().equals(wld.getType()))) {
                for (int j = 0; j < wld.getAgentList().size(); j++) {
                    Agent a = (Agent) wld.getAgentList().get(j);

                    if (c.getAgentName().equals(a.getName())) {
                        return a;
                    }
                }
            }
        }

        //Then go for any matching agent
        for (int i = 0; i < getAgentList().size(); i++) {
            Agent a = (Agent) getAgentList().get(i);

            if (c.getAgentName().equals(a.getName()) && (c.getWorldType().equals(a.getParentWorld().getType()))) {
                return a;
            }
        }

        //Finally go for any matching world-type and ANY agent
        for (int i = 0; i < getAgentList().size(); i++) {
            Agent a = (Agent) getAgentList().get(i);

            if ((c.getWorldType().equals(a.getParentWorld().getType()))) {
                return a;
            }
        }

        //Otherwise give up
        return null;
    }

    /**
     * Look for "null" couplings (couplings with no agent field), and try to find suitable agents to attach them to.
     * These can occur when a neuron's coupling field stay alive but a world is changed (e.g., an agent is deleted).
     * Later, when a new world is opened, for example, this method is called so that the agents in those worlds can be
     * attached to null couplings.  More specifically, attach agents to to couplings where  (1) the agent field is
     * null (2) the agent's worldtype matches, and  (3) the agent's name matches
     *
     * @param couplings the set of couplings to check
     */
    public void attachAgentsToCouplings(final ArrayList couplings) {
        for (int i = 0; i < couplings.size(); i++) {
            Coupling c = (Coupling) couplings.get(i);

            for (int j = 0; j < getAgentList().size(); j++) {
                Agent a = (Agent) getAgentList().get(j);

                // if world-type and agent name matches, add this agent to the coupling
                if ((c.getAgent() == null) && c.getAgentName().equals(a.getName())
                        && c.getWorldType().equals(a.getParentWorld().getType())) {
                    c.setAgent(a);
                    break;
                }
            }
        }
    }

    /**
     * When a new world is opened, see if any open networks have "null" couplings  that that world's agents can attach
     * to.
     */
    public void attachAgentsToCouplings() {
        attachAgentsToCouplings(getCouplingList());
        repaintAllNetworks();
    }

    /**
     * Remove all given agents from the couplng list, by setting the agent field on those couplings to null.
     *
     * @param w the world whose agents should be removed
     */
    public void removeAgentsFromCouplings(final World w) {
        ArrayList agents = w.getAgentList();
        removeAgentsFromCouplings(agents);
    }

    /**
     * Remove all given agents from the couplng list, by setting the agent field on those couplings to null.
     *
     * @param agents the list of agents to be removed.
     */
    public void removeAgentsFromCouplings(final ArrayList agents) {

        ArrayList couplings = getCouplingList();

        for (int i = 0; i < couplings.size(); i++) {
            for (int j = 0; j < agents.size(); j++) {
                if (((Coupling) couplings.get(i)).getAgent() == agents.get(j)) {
                    ((Coupling) couplings.get(i)).setAgent(null);
                }
            }
        }
    }

    /**
     * @return Returns the couplingList.
     */
    public ArrayList getCouplingList() {
        ArrayList ret = new ArrayList();
        for (int i = 0; i < networkList.size(); i++) {
            ret.addAll(((NetworkFrame) networkList.get(i)).getNetworkPanel().getNetwork().getCouplingList());
        }
        return ret;
    }

    /**
     * @return Returns the worldList.
     */
    public ArrayList getWorldList() {
        ArrayList ret = new ArrayList();

        for (int i = 0; i < odorWorldList.size(); i++) {
            ret.add(((OdorWorldFrame) odorWorldList.get(i)).getWorld());
        }

        for (int i = 0; i < dataWorldList.size(); i++) {
            ret.add(((DataWorldFrame) dataWorldList.get(i)).getWorld());
        }

        return ret;
    }

    /**
     * @return Returns the dataWorldList.
     */
    public ArrayList getDataWorldList() {
        return dataWorldList;
    }

    /**
     * @param dataWorldList The dataWorldList to set.
     */
    public void setDataWorldList(final ArrayList dataWorldList) {
        this.dataWorldList = dataWorldList;
        this.workspaceChanged = true;
    }

    /**
     * @return Returns the odorWorldList.
     */
    public ArrayList getOdorWorldList() {
        return odorWorldList;
    }

    /**
     * @param odorWorldList The odorWorldList to set.
     */
    public void setOdorWorldList(final ArrayList odorWorldList) {
        this.odorWorldList = odorWorldList;
        this.workspaceChanged = true;
    }

    /**
     * Check whether there have been changes in the workspace or its components.
     *
     * @return true if changes exist, false otherwise
     */
    public boolean changesExist() {
        int odorWorldChanges = getOdorWorldChangeList().size();
        int dataWorldChanges = getDataWorldChangeList().size();
        int networkChanges = getNetworkChangeList().size();
        int gaugeChanges = getGaugeChangeList().size();

        if (((odorWorldChanges + dataWorldChanges + networkChanges + gaugeChanges) > 0)
                || (workspaceChanged)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return a list of odor worlds that have changed since last save.
     */
    public ArrayList getOdorWorldChangeList() {
        ArrayList ret = new ArrayList();

        int y = 0;

        for (int j = 0; j < odorWorldList.size(); j++) {
            OdorWorldFrame test = (OdorWorldFrame) getOdorWorldList().get(j);

            if (test.isChangedSinceLastSave()) {
                ret.add(y, test);
                y++;
            }
        }

        return ret;
    }

    /**
     * @return a list of networks that have changed since last save.
     */
    public ArrayList getNetworkChangeList() {
        ArrayList ret = new ArrayList();

        int x = 0;

        for (int i = 0; i < networkList.size(); i++) {
            NetworkFrame test = (NetworkFrame) getNetworkList().get(i);

            if (test.isChangedSinceLastSave()) {
                ret.add(x, test);
                x++;
            }
        }

        return ret;
    }

    /**
     * @return a list of data worlds that have changed since last save.
     */
    public ArrayList getDataWorldChangeList() {
        ArrayList ret = new ArrayList();

        int z = 0;

        for (int k = 0; k < dataWorldList.size(); k++) {
            DataWorldFrame test = (DataWorldFrame) getDataWorldList().get(k);

            if (test.isChangedSinceLastSave()) {
                ret.add(z, test);
                z++;
            }
        }

        return ret;
    }

    /**
     * @return a list of gauges that have changed since last save.
     */
    public ArrayList getGaugeChangeList() {
        ArrayList ret = new ArrayList();

        int x = 0;

        for (int i = 0; i < gaugeList.size(); i++) {
            GaugeFrame test = (GaugeFrame) getGaugeList().get(i);

            if (test.isChangedSinceLastSave()) {
                ret.add(x, test);
                x++;
            }
        }

        return ret;
    }

    /**
     * Quit application.
     *
     */
    protected void quit() {
        //ensures that frameClosing events are called
        disposeAllFrames();

        System.exit(0);
    }

    /**
     * Responds to window opened events.
     * @param arg0 Window event
     */
    public void windowOpened(final WindowEvent arg0) {
    }

    /**
     * Responds to window closing events.
     * @param arg0 Window event
     */
    public void windowClosing(final WindowEvent arg0) {
        if (changesExist()) {
            WorkspaceChangedDialog dialog = new WorkspaceChangedDialog(this);

            if (dialog.hasUserCancelled()) {
                return;
            }
        }

        quit();
    }

    /**
     * Responds to window closed events.
     * @param arg0 Window event
     */
    public void windowClosed(final WindowEvent arg0) {
    }

    /**
     * Responds to window iconified events.
     * @param arg0 Window event
     */
    public void windowIconified(final WindowEvent arg0) {
    }

    /**
     * Responds to window deiconified events.
     * @param arg0 Window event
     */
    public void windowDeiconified(final WindowEvent arg0) {
    }

    /**
     * Responds to window activated events.
     * @param arg0 Window event
     */
    public void windowActivated(final WindowEvent arg0) {
    }

    /**
     * Responds to window deactivated events.
     * @param arg0 Window event
     */
    public void windowDeactivated(final WindowEvent arg0) {
    }

    /**
     * @return Has the workspace been changed.
     */
    public boolean hasWorkspaceChanged() {
        return this.workspaceChanged;
    }

    /**
     * Sets whether the workspace has been changed.
     * @param workspaceChanged Has workspace been changed value
     */
    public void setWorkspaceChanged(final boolean workspaceChanged) {
        this.workspaceChanged = workspaceChanged;
    }

    /**
     * @return Returns the currentFile.
     */
    public File getCurrentFile() {
        return currentFile;
    }

    /**
     * @param currentFile The current_file to set.
     */
    public void setCurrentFile(final File currentFile) {
        this.currentFile = currentFile;
    }

    /**
     * Responds to component hidden events.
     * @param arg0 Component event
     */
    public void componentHidden(final ComponentEvent arg0) {
    }

    /**
     * Responds to component moved events.
     * @param arg0 Component event
     */
    public void componentMoved(final ComponentEvent arg0) {
        setWorkspaceChanged(true);
    }

    /**
     * Responds to component resized events.
     * @param arg0 Component event
     */
    public void componentResized(final ComponentEvent arg0) {
        setWorkspaceChanged(true);
    }

    /**
     * Responds to component shown events.
     * @param arg0 Component event
     */
    public void componentShown(final ComponentEvent arg0) {
    }

    /**
     * Responds to menu selected events.
     * @param arg0 Menu event
     */
    public void menuSelected(final MenuEvent arg0) {
        if (changesExist()) {
            saveItem.setEnabled(true);
        } else {
            saveItem.setEnabled(false);
        }
    }

    /**
     * Responds to menu deslected events.
     * @param arg0 Menu event
     */
    public void menuDeselected(final MenuEvent arg0) {
    }

    /**
     * Responds to menu canceled events.
     * @param arg0 Menu event
     */
    public void menuCanceled(final MenuEvent arg0) {
    }
}
