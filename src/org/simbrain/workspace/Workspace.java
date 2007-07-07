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
package org.simbrain.workspace;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.ToggleButton;

/**
 * <b>Workspace</b> is the container for all Simbrain windows--network, world, and gauge.
 */
public class Workspace extends JFrame implements WindowListener,
                                    ComponentListener, MenuListener, MouseListener {

    /** Desktop pane. */
    private JDesktopPane desktop;

    /** List of workspace components. */
    private ArrayList<WorkspaceComponent> componentList = new ArrayList<WorkspaceComponent>();

    /** Global workspace singleton. */
    private final static Workspace WORKSPACE = new Workspace();

    /** Default workspace file to be opened upon initalization. */
    private static final String DEFAULT_FILE = WorkspacePreferences.getDefaultFile();

    /** File system property. */
    private static final String FS = System.getProperty("file.separator");

    /** Initial indent of entire workspace. */
    private static final int WORKSPACE_INSET = 50;

    /** After placing one simbrain windo how far away to put the next one. */
    private static final int DEFAULT_WINDOW_OFFSET = 30;

    /** Current workspace file. */
    private File currentFile = null;

    /** Current workspace directory. */
    private String currentDirectory = WorkspacePreferences.getCurrentDirectory();

    /** The offset amount for each new subsequent frame. */
    private static final int NEXT_FRAME_OFFSET = 40;

    /** Sentinal for determining if workspace has been changed since last save. */
    private boolean workspaceChanged = false;

    /** Simbrain initial launch check. */
    private boolean initialLaunch = true;

    /** Workspace action manager. */
    private WorkspaceActionManager actionManager;

    /** Cached context menu. */
    private JPopupMenu contextMenu;

    /** Last clicked point. */
    private Point lastClickedPoint = null;

    /** Workspace tool bar. */
    private JToolBar wsToolBar = new JToolBar();

    /** Whether network has been updated yet; used by thread. */
    private boolean updateCompleted;

    private WorkspaceThread workspaceThread;

    /**
     * Default constructor.
     */
    public Workspace() {
        super("Simbrain");

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(WORKSPACE_INSET, WORKSPACE_INSET, screenSize.width - (WORKSPACE_INSET * 2),
                screenSize.height - (WORKSPACE_INSET * 2));

        //Set up the GUI.
        desktop = new JDesktopPane(); //a specialized layered pane

        actionManager = new WorkspaceActionManager();
        createAndAttachMenus();

        wsToolBar = createToolBar();

        JPanel mainPanel = new JPanel(new BorderLayout());
        JScrollPane workspaceScroller = new JScrollPane();
        mainPanel.add("North", wsToolBar);
        mainPanel.add("Center", workspaceScroller);
        setContentPane(mainPanel);
        workspaceScroller.setViewportView(desktop);
        workspaceScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        workspaceScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        addWindowListener(this);
        desktop.addMouseListener(this);
        addKeyListener(new WorkspaceKeyAdapter());
        desktop.addKeyListener(new WorkspaceKeyAdapter());
        createContextMenu();

        //Make dragging a little faster but perhaps uglier.
        //desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
    }

    /**
     * Creates the workspace tool bar.
     *
     * @return JToolBar tool bar created
     */
    private JToolBar createToolBar() {
        JToolBar bar = new JToolBar();

        bar.add(actionManager.getOpenWorkspaceAction());
        bar.add(actionManager.getSaveWorkspaceAction());
        bar.addSeparator();
        bar.add(actionManager.getGlobalUpdateAction());
        bar.add(new ToggleButton(actionManager.getGlobalControlActions()));

        bar.addSeparator();
        bar.add(actionManager.getOpenCouplingManagerAction());

        bar.addSeparator();
        bar.add(actionManager.getNewNetworkAction());
        JButton button = new JButton();
        button.setIcon(ResourceManager.getImageIcon("World.png"));
        final JPopupMenu menu = new JPopupMenu();
        for (Action action : actionManager.getNewWorldActions()) {
            menu.add(action);
        }
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JButton button = (JButton)e.getSource();
                menu.show(button, 0, button.getHeight());
            }
        });
        button.setComponentPopupMenu(menu);
        bar.add(button);
        bar.add(actionManager.getNewGaugeAction());
        bar.add(actionManager.getNewConsoleAction());

        return bar;
    }



    /**
     * Get a reference to the global workspace.
     *
     * @return Workspace instance
     */
    public static Workspace getInstance() {
        return WORKSPACE;
    }

    /**
     * Update all couplings on all components.  Currently use a buffering method.
     * TODO: Add other methods.
     */
    public void globalUpdate() {
        for (WorkspaceComponent component : componentList) {
            if (component.getCouplingContainer() != null) {
                for (Coupling coupling : component.getCouplingContainer().getCouplings()) {
                    coupling.setBuffer();
                }
            }
        }
        for (WorkspaceComponent component : componentList) {
            if (component.getCouplingContainer() != null) {
                for (Coupling coupling : component.getCouplingContainer().getCouplings()) {
                    coupling.update();
                }
            }
        }
        for (WorkspaceComponent component : componentList) {
            if (component.getCouplingContainer() != null) {
                component.updateComponent();
            }
        }
        updateCompleted = true;
    }

    /**
     * Iterates all couplings on all components until halted by user.
     */
    public void globalRun() {
        if (getWorkspaceThread() == null) {
            setWorkspaceThread(new WorkspaceThread());
        }

        WorkspaceThread workspaceThread = getWorkspaceThread();

        if (!workspaceThread.isRunning()) {
            workspaceThread.setRunning(true);
            workspaceThread.start();
        } else {
            workspaceThread.setRunning(false);
        }
    }

    /**
     * Stops itaration of all couplings on all components.
     */
    public void globalStop() {
        if (getWorkspaceThread() == null) {
            setWorkspaceThread(new WorkspaceThread());
        }

        WorkspaceThread workspaceThread = getWorkspaceThread();

        workspaceThread.setRunning(false);
        setWorkspaceThread(null);
    }

    /**
     * Add a new <c>SimbrainComponent</c>.
     *
     * @param component
     */
    public void addWorkspaceComponent(final WorkspaceComponent component) {

        // Add component at wherever was last clicked.
        // If nothing last clicked is null the workspace was just opened
        //          (in that case use defaults)
        //  Or a component was recently added
        //          (in that case put it near the last one)        
        if (lastClickedPoint != null) {
            component.setBounds((int) lastClickedPoint.getX(), (int) lastClickedPoint.getY(),
                    component.getDefaultWidth(), component.getDefaultHeight());
        } else {
            if (componentList.size() == 0) {
                component.setBounds(DEFAULT_WINDOW_OFFSET, DEFAULT_WINDOW_OFFSET,
                        component.getDefaultWidth(), component.getDefaultHeight());
            } else {
                int lastIndex = componentList.size() - 1;
                int lastX = componentList.get(lastIndex).getX();
                int lastY = componentList.get(lastIndex).getY();
                component.setBounds(lastX + DEFAULT_WINDOW_OFFSET, lastY + DEFAULT_WINDOW_OFFSET,
                        component.getDefaultWidth(), component.getDefaultHeight());

            }
        }

        componentList.add(component);
        desktop.add(component);
        component.setVisible(true); //necessary as of 1.3

        try {
            component.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
            System.out.print(e.getStackTrace());
        }

        // So that after creating a window the next ones run out in a trail
        lastClickedPoint = null;
        this.workspaceChanged = true;
        component.addComponentListener(this);
    }

    /**
     * Remove the specified window.
     * @param window
     */
    public void removeWorkspaceComponent(WorkspaceComponent window) {
        componentList.remove(window);
    }

    /**
     * Create and attach workspace menus.
     */
    private void createAndAttachMenus() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createFileMenu());
        menuBar.add(createInsertMenu());

        menuBar.add(createCoupleMenu());

        menuBar.add(createHelpMenu());

        setJMenuBar(menuBar);
    }

    /**
     * Create the workspace file menu.
     *
     * @return file menu
     */
    private JMenu createFileMenu() {
        JMenu fileMenu = new JMenu("File");
        fileMenu.addMenuListener(this);
        for (Iterator i = actionManager.getOpenSaveWorkspaceActions().iterator(); i.hasNext(); ) {
            fileMenu.add((Action) i.next());
        }
        fileMenu.addSeparator();
        for (Iterator i = actionManager.getImportExportActions().iterator(); i.hasNext(); ) {
            fileMenu.add((Action) i.next());
        }
        fileMenu.addSeparator();
        fileMenu.add(actionManager.getClearWorkspaceAction());
        fileMenu.addSeparator();
        fileMenu.add(actionManager.getOpenNetworkAction());
        fileMenu.add(actionManager.getOpenGaugeAction());

        JMenu worldSubMenu = new JMenu("Open World");
        for (Iterator i = actionManager.getOpenWorldActions().iterator(); i.hasNext(); ) {
            worldSubMenu.add((Action) i.next());
        }
        fileMenu.add(worldSubMenu);
        fileMenu.addSeparator();
        fileMenu.add(actionManager.getQuitWorkspaceAction());
        return fileMenu;
    }

    /**
     * Create the workspace insert menu.
     *
     * @return insert menu
     */
    private JMenu createInsertMenu() {
        JMenu insertMenu = new JMenu("Insert");
        insertMenu.add(actionManager.getNewNetworkAction());
        insertMenu.add(actionManager.getNewGaugeAction());
        insertMenu.add(actionManager.getNewPlotAction());
        JMenu newWorldSubMenu = new JMenu("New World");
        for (Iterator i = actionManager.getNewWorldActions().iterator(); i.hasNext(); ) {
            newWorldSubMenu.add((Action) i.next());
        }
        insertMenu.add(newWorldSubMenu);
        insertMenu.addSeparator();
        insertMenu.add(actionManager.getNewConsoleAction());
        return insertMenu;
    }

    /**
     * Create the workspace couplings menu.
     *
     * @return couplings menu
     */
    private JMenu createCoupleMenu(){
        JMenu coupleMenu = new JMenu("Couplings");
        coupleMenu.add(actionManager.getOpenCouplingManagerAction());
        coupleMenu.add(actionManager.getGlobalUpdateAction());
        return coupleMenu;
    }

    /**
     * Create the workspace help menu.
     *
     * @return help menu
     */
    private JMenu createHelpMenu() {
        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(actionManager.getWorkspaceHelpAction());
        return helpMenu;
    }

    /**
     * Create a new context menu for this network panel.
     */
    private void createContextMenu() {
        contextMenu = new JPopupMenu();
        contextMenu.add(actionManager.getNewNetworkAction());
        contextMenu.add(actionManager.getNewGaugeAction());
        JMenu newWorldSubMenu = new JMenu("New World");
        for (Iterator i = actionManager.getNewWorldActions().iterator(); i.hasNext(); ) {
            newWorldSubMenu.add((Action) i.next());
        }
        contextMenu.add(newWorldSubMenu);
        contextMenu.addSeparator();
        contextMenu.add(actionManager.getNewConsoleAction());

    }

    /**
     * Open a specific workspace component (network, world, etc).
     *
     * @param type the type of the component to open.
     */
    public void openWorkspaceComponent(final Class type) {

        WorkspaceComponent component;
        try {
            component = (WorkspaceComponent) type.newInstance();
            SFileChooser chooser = new SFileChooser(component.getCurrentDirectory(), component.getFileExtension());
            File theFile = chooser.showOpenDialog();

            if (theFile != null) {
                this.addWorkspaceComponent(component);
                component.open(theFile);
                component.setCurrentDirectory(chooser.getCurrentLocation());
                //NetworkPreferences.setCurrentDirectory(currentDirectory.toString());  //TODO: Put this in the setCurrentDirectory overrides
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Responds to mouse events.
     *
     * @param mouseEvent Mouse Event
     */
    public void mousePressed(final MouseEvent mouseEvent) {
        lastClickedPoint = mouseEvent.getPoint();
        if (mouseEvent.isControlDown() || (mouseEvent.getButton() == MouseEvent.BUTTON3)) {
            contextMenu.show(this, (int) lastClickedPoint.getX() + 5, (int) lastClickedPoint.getY() + 53);
        }
     }

    /**
     * Responds to mouse events.
     *
     * @param e Mouse Event
     */
     public void mouseReleased(final MouseEvent e) {
     }

     /**
      * Responds to mouse events.
      *
      * @param e Mouse Event
      */
     public void mouseEntered(final MouseEvent e) {
         //empty
     }

     /**
      * Responds to mouse events.
      *
      * @param e Mouse Event
      */
     public void mouseExited(final MouseEvent e) {
         //empty
     }

     /**
      * Responds to mouse events.
      *
      * @param e Mouse Event
      */
     public void mouseClicked(final MouseEvent e) {
         //empty
     }

 
    /**
     * Remove all items (networks, worlds, etc.) from this workspace.
     */
    public void clearWorkspace() {
        if (changesExist()) {
            WorkspaceChangedDialog dialog = new WorkspaceChangedDialog();

            if (dialog.hasUserCancelled()) {
                return;
            }
        }
        workspaceChanged = false;
        removeAllComponents();
        currentFile = null;
        this.setTitle("Simbrain");
    }

    /**
     * Disposes all Simbrain Windows.
     */
    public void removeAllComponents() {
        for (WorkspaceComponent component : componentList) {
            component.dispose();
        }
        componentList.clear();
    }

    /**
     * Shows the dialog for opening a workspace file.
     */
    public void openWorkspace() {

        if (changesExist()) {
            WorkspaceChangedDialog theDialog = new WorkspaceChangedDialog();

            if (theDialog.hasUserCancelled()) {
                return;
            }
        }
        workspaceChanged = false;

        SFileChooser simulationChooser = new SFileChooser(currentDirectory, "sim");
        File simFile = simulationChooser.showOpenDialog();

        if (simFile != null) {
            WorkspaceSerializer.readWorkspace(simFile, false);
            currentFile = simFile;
            currentDirectory = simulationChooser.getCurrentLocation();
            WorkspacePreferences.setCurrentDirectory(currentDirectory);
            WorkspacePreferences.setDefaultFile(currentFile.toString());
        }

    }

    /**
     * Shows the dialog for saving a workspace file.
     */
    public void showSaveFileAsDialog() {
        SFileChooser simulationChooser = new SFileChooser(currentDirectory, "sim");
        workspaceChanged = false;

        if (changesExist()) {
            WorkspaceChangedDialog theDialog = new WorkspaceChangedDialog();

            if (theDialog.hasUserCancelled()) {
                return;
            }
        }

        File simFile = simulationChooser.showSaveDialog();

        if (simFile != null) {
            WorkspaceSerializer.writeWorkspace(simFile);
            currentFile = simFile;
            currentDirectory = simulationChooser.getCurrentLocation();
        }
    }

    /**
     * Import a workspace.  Assumes the workspace file has the same name as the directory
     * which contains the exported workspace.
     */
    public void importWorkspace() {
        if (changesExist()) {
            WorkspaceChangedDialog theDialog = new WorkspaceChangedDialog();

            if (theDialog.hasUserCancelled()) {
                return;
            }
        }
        workspaceChanged = false;

        JFileChooser simulationChooser = new JFileChooser();
        simulationChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        File dir = new File(currentDirectory);
        try {
           simulationChooser.setCurrentDirectory(dir.getCanonicalFile());
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        simulationChooser.showOpenDialog(null);
        File simFile = simulationChooser.getSelectedFile();

        if (simFile != null) {
            String path = simFile + FS + simFile.getName() + ".sim";
            File theFile = new File(path);
            currentDirectory = simFile.getParent();
            WorkspacePreferences.setCurrentDirectory(simFile.getParent());
            WorkspaceSerializer.readWorkspace(theFile, true);
        }
    }

    /**
     * Export a workspace file: that is, save all workspace components and then a simple
     * workspace file correpsonding to them.
     */
    public void exportWorkspace() {
        SFileChooser chooser = new SFileChooser(currentDirectory, "sim");
        File simFile = chooser.showSaveDialog();

        if (simFile == null) {
            return;
        }

        WorkspacePreferences.setCurrentDirectory(simFile.getParent());
        currentDirectory = simFile.getParent();

        String newDir = simFile.getName().substring(0, simFile.getName().length() - 4);
        String newDirPath = simFile.getParent() + FS + newDir;
        String exportName = newDirPath + FS + simFile.getName();

        // Make the new directory
        boolean success = new File(newDirPath).mkdir();
        if (!success) {
            return;
        }

        for (WorkspaceComponent component : componentList) {
            String pathName = checkName(component.getTitle(), component.getFileExtension());
            File file = new File(newDirPath, pathName);
            component.save(file);
            component.setPath(pathName);
        }

        WorkspaceSerializer.writeWorkspace(new File(exportName));

    }

    /**
     * If the filename does not have the proper extension add it.
     *
     * @param name string name the string to check
     * @param extension extension (e.g. ".xml")
     * @return the checked string (filename + "." + extension)
     */
    private String checkName(final String name, final String extension) {
        String ret = new String(name);
        if (!ret.endsWith(extension)) {
            ret += "." + extension;
        }
        return ret;
    }

    /**
     * Show the save dialog.
     */
    public void saveFile() {
        workspaceChanged = false;

        if (changesExist()) {
            WorkspaceChangedDialog theDialog = new WorkspaceChangedDialog();

            if (theDialog.hasUserCancelled()) {
                return;
            }
        }

        if (currentFile != null) {
            WorkspaceSerializer.writeWorkspace(currentFile);
        } else {
            showSaveFileAsDialog();
        }
    }

    /**
     * Create the GUI and show it. For thread safety, this method should be invoked from the event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Make sure we have nice window decorations.
        //JFrame.setDefaultLookAndFeelDecorated(true);
        //Create and set up the window.
        getInstance().setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        //Display the window.
        getInstance().setVisible(true);

        //Open initial workspace
        WorkspaceSerializer.readWorkspace(new File(DEFAULT_FILE), false);

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
     * Check whether there have been changes in the workspace or its components.
     *
     * @return true if changes exist, false otherwise
     */
    public boolean changesExist() {
        boolean hasChanged = false;
        for (WorkspaceComponent window : componentList) {
            if (window.isChangedSinceLastSave()) {
                hasChanged = true;
            }
        }
        return hasChanged;
    }

    public ArrayList<WorkspaceComponent> getChangedWindows() {
        ArrayList<WorkspaceComponent> ret = new ArrayList<WorkspaceComponent>();
        for (WorkspaceComponent window : componentList) {
            if (window.isChangedSinceLastSave()) {
                ret.add(window);
            }
        }
        return ret;
    }

    /**
     * Quit application.
     */
    public void quit() {
        //ensures that frameClosing events are called
        removeAllComponents();

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
            WorkspaceChangedDialog dialog = new WorkspaceChangedDialog();

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
     * @param arg0 SimbrainComponent event
     */
    public void componentHidden(final ComponentEvent arg0) {
    }

    /**
     * Responds to component moved events.
     * @param arg0 SimbrainComponent event
     */
    public void componentMoved(final ComponentEvent arg0) {
        setWorkspaceChanged(true);
    }

    /**
     * Responds to component resized events.
     * @param arg0 SimbrainComponent event
     */
    public void componentResized(final ComponentEvent arg0) {
        setWorkspaceChanged(true);
    }

    /**
     * Responds to component shown events.
     * @param arg0 SimbrainComponent event
     */
    public void componentShown(final ComponentEvent arg0) {
    }

    /**
     * Responds to menu selected events.
     * @param arg0 Menu event
     */
    public void menuSelected(final MenuEvent arg0) {
        if (changesExist()) {
            actionManager.getSaveWorkspaceAction().setEnabled(true);
        } else {
            actionManager.getSaveWorkspaceAction().setEnabled(false);
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


    /**
     * Get a menu representing all available producers.
     *
     * @param listener the component which will listens to the menu items in this menu
     * @return the menu containing all available producers
     */
    public JMenu getProducerMenu(final ActionListener listener) {
        JMenu producerMenu = new JMenu("Producers");
        for (WorkspaceComponent component : componentList) {
            if (component.getCouplingContainer() != null) {
                JMenu componentMenu = new JMenu(component.getName());
                for (Producer producer : component.getCouplingContainer().getProducers()) {
                    JMenu producerItem = new JMenu(producer.getProducerDescription());
                    for (ProducingAttribute attribute : producer.getProducingAttributes()) {
                        CouplingMenuItem attributeItem = new CouplingMenuItem(attribute);
                        attributeItem.addActionListener(listener);
                        producerItem.add(attributeItem);
                    }
                    componentMenu.add(producerItem);
                }
                producerMenu.add(componentMenu);
            }
        }
        return producerMenu;
    }

    /**
     * Get a menu representing all available consumers.
     *
     * @param listener the component which will listens to the menu items in this menu
     * @return the menu containing all available consumers
     */
    public JMenu getConsumerMenu(final ActionListener listener) {
        JMenu consumerMenu = new JMenu("Consumers");
        for (WorkspaceComponent component : componentList) {
            if (component.getCouplingContainer() != null) {
                JMenu componentMenu = new JMenu(component.getName());
                for (Consumer consumer : component.getCouplingContainer().getConsumers()) {
                    JMenu consumerItem = new JMenu(consumer.getConsumerDescription());
                    for (ConsumingAttribute attribute : consumer.getConsumingAttributes()) {
                        CouplingMenuItem attributeItem = new CouplingMenuItem(attribute);
                        attributeItem.addActionListener(listener);
                        consumerItem.add(attributeItem);
                    }
                    componentMenu.add(consumerItem);
                }
                consumerMenu.add(componentMenu);
            }
        }
        return consumerMenu;
    }

    /**
     * @return Returns true if initial launching.
     */
    public boolean isInitialLaunch() {
        return initialLaunch;
    }

    /**
     * @param val The initial launch determination.
     */
    public void setInitialLaunch(final boolean val) {
        initialLaunch = val;
    }

    /**
     * @return the componentList
     */
    public ArrayList<WorkspaceComponent> getComponentList() {
        return componentList;
    }


    /**
     * @return the lastClickedPoint
     */
    public Point getLastClickedPoint() {
        return lastClickedPoint;
    }


    /**
     * @return the currentDirectory
     */
    public String getCurrentDirectory() {
        return currentDirectory;
    }


    /**
     * @param currentDirectory the currentDirectory to set
     */
    public void setCurrentDirectory(String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    /**
     * Used by Network thread to ensure that an update cycle is complete before
     * updating again.
     *
     * @return whether the network has been updated or not
     */
    public boolean isUpdateCompleted() {
        return updateCompleted;
    }

    /**
     * Used by Network thread to ensure that an update cycle is complete before
     * updating again.
     *
     * @param b whether the network has been updated or not.
     */
    public void setUpdateCompleted(final boolean b) {
        updateCompleted = b;
    }

    /**
     * @return the workspaceThread.
     */
    public WorkspaceThread getWorkspaceThread() {
        return workspaceThread;
    }

    /**
     * @param workspaceThread the workspaceThread to set
     */
    public void setWorkspaceThread(final WorkspaceThread workspaceThread) {
        this.workspaceThread = workspaceThread;
    }
}
