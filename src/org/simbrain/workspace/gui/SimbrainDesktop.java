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
package org.simbrain.workspace.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.apache.log4j.Logger;
import org.simbrain.console.ConsoleComponent;
import org.simbrain.console.ConsoleDesktopComponent;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.desktop.NetworkDesktopComponent;
import org.simbrain.plot.barchart.BarChartComponent;
import org.simbrain.plot.barchart.BarChartGui;
import org.simbrain.plot.piechart.PieChartComponent;
import org.simbrain.plot.piechart.PieChartGui;
import org.simbrain.plot.projection.ProjectionComponent;
import org.simbrain.plot.projection.ProjectionGui;
import org.simbrain.plot.scatterplot.ScatterPlotComponent;
import org.simbrain.plot.scatterplot.ScatterPlotGui;
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent;
import org.simbrain.plot.timeseries.TimeSeriesPlotGui;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.ToggleButton;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceListener;
import org.simbrain.workspace.WorkspaceSerializer;
import org.simbrain.workspace.updator.InterceptingEventQueue;
import org.simbrain.world.dataworld.DataWorldComponent;
import org.simbrain.world.dataworld.DataWorldDesktopComponent;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.world.odorworld.OdorWorldDesktopComponent;
import org.simbrain.world.oscworld.OscWorldComponent;
import org.simbrain.world.oscworld.OscWorldDesktopComponent;
import org.simbrain.world.textworld.TextWorldComponent;
import org.simbrain.world.textworld.TextWorldDesktopComponent;
import org.simbrain.world.threedee.ThreeDeeComponent;
import org.simbrain.world.threedee.gui.MainConsole;
import org.simbrain.world.visionworld.VisionWorldComponent;
import org.simbrain.world.visionworld.VisionWorldDesktopComponent;

import bsh.Interpreter;
import bsh.util.JConsole;

/**
 * Creates a Swing-based environment for working with a workspace.
 * 
 * Also provides wrappers for GUI elements called from a terminal.
 * 
 * @author Matt Watson
 * @author Jeff Yoshimi
 */
public class SimbrainDesktop {

    /** The x offset for popup menus. */
    private static final int MENU_X_OFFSET = 5;

    /** The y offset for popup menus. */
    private static final int MENU_Y_OFFSET = 53;
    
    /** The default serial version ID. */
    private static final long serialVersionUID = 1L;

    /** Log4j logger. */
    private static final Logger LOGGER = Logger.getLogger(Workspace.class);

    /** Initial indent of entire workspace. */
    private static final int WORKSPACE_INSET = 80;

    /** After placing one simbrain window how far away to put the next one. */
    private static final int DEFAULT_WINDOW_OFFSET = 30;

    /** TODO: Create Javadoc comment. */
    private static final Map<Workspace, SimbrainDesktop> INSTANCES =
         new HashMap<Workspace, SimbrainDesktop>();
    
    /** Desktop pane. */
    private JDesktopPane desktop;

    /** Cached context menu. */
    private JPopupMenu contextMenu;

    /** Workspace tool bar. */
    private JToolBar wsToolBar = new JToolBar();

    /** Whether the bottom dock is visible. */
    private boolean dockVisible = true;

    /** the frame that will hold the workspace. */
    private JFrame frame;
    
    /** Used to track whether the gui has been modified since the last save. */
    private boolean guiChanged = false;
    
    /** The bottom dock. */
    private JTabbedPane bottomDock;

    /** Pane splitter for bottom dock. */
    private JSplitPane horizontalSplitter;
    
    /** The workspace this desktop wraps. */
    private final Workspace workspace;

    /** Boundary of workspace. */
    private Rectangle workspaceBounds;
    
    /** Workspace action manager. */
    private WorkspaceActionManager actionManager;
    
    /** Interpreter for terminal. */
    Interpreter interpreter;

    /** All the guiComponents in the workspace. */
    private Map<WorkspaceComponent<?>, GuiComponent<?>> guiComponents
        = new LinkedHashMap<WorkspaceComponent<?>, GuiComponent<?>>();
    
    /** Listener on the workspace. */
    private final WorkspaceListener listener = new WorkspaceListener() {
        public boolean clearWorkspace() {
            save();
            return true;
        }
        
        public void workspaceCleared() {
            if (changesExist()) {
                save();
            }
            frame.setTitle("Simbrain");
            return;
        }
        
        /**
         * Add a new <c>SimbrainComponent</c>.
         *
         * @param component
         */
        @SuppressWarnings("unchecked")
        public void componentAdded(final WorkspaceComponent workspaceComponent) {
            addComponent(workspaceComponent, null);
        }

        @SuppressWarnings("unchecked")
        public void componentRemoved(final WorkspaceComponent workspaceComponent) {
            GuiComponent<?> component = guiComponents.get(workspaceComponent);
            guiComponents.remove(component);
            component.getParentFrame().dispose();
        }
    };
    
    // TODO this should be addressed at a higher level
    public static SimbrainDesktop getDesktop(final Workspace workspace) {
        return INSTANCES.get(workspace);
    }
    
    /**
     * Default constructor.
     * 
     * @param workspace
     *            The workspace for this desktop.
     */
    public SimbrainDesktop(final Workspace workspace) {

        INSTANCES.put(workspace, this);
        this.workspace = workspace;
        frame = new JFrame("Simbrain");
        actionManager = new WorkspaceActionManager(this);
        createAndAttachMenus();
        wsToolBar = createToolBar();
        createContextMenu();
        workspace.addListener(listener);
        SimbrainDesktop.registerComponents();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        workspaceBounds = new Rectangle(WORKSPACE_INSET,
                WORKSPACE_INSET, screenSize.width - (WORKSPACE_INSET * 2),
                screenSize.height - (WORKSPACE_INSET * 2));

        // Set up Desktop
        desktop = new JDesktopPane();
        desktop.addMouseListener(mouseListener);
        desktop.addKeyListener(new WorkspaceKeyAdapter(workspace));
        desktop.setPreferredSize(new Dimension(screenSize.width
                - (WORKSPACE_INSET * 2), screenSize.height - (WORKSPACE_INSET * 3)));

        // Create the Tabbed Pane for bottom of the desktop
        bottomDock = new JTabbedPane();
        bottomDock.addTab("Terminal", null, this.getTerminalPanel(), "Simbrain terminal");
        bottomDock.addTab("Updator", null, new ThreadViewerPanel(this
                .getWorkspace()), "Simbrain thread viewer");
        bottomDock.addTab("Components", null, new ComponentPanel(
                this), "Show workspace components");
        // List of current couplings for populating couplings panel.
        Vector<Coupling<?>> couplings = new Vector<Coupling<?>>(workspace
                .getCouplingManager().getCouplings());
        bottomDock.addTab("Couplings", null, new CouplingListPanel(this,
                couplings), "Show current couplings");
        // Set up the main panel
        horizontalSplitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        horizontalSplitter.setDividerLocation((int) (3 * (workspaceBounds
                .getHeight() / 4)));
        horizontalSplitter.setTopComponent(desktop);
        horizontalSplitter.setBottomComponent(bottomDock);
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(wsToolBar, "North");
        mainPanel.add(horizontalSplitter, "Center");

        // Set up Frame
        frame.setBounds(workspaceBounds);
        frame.setContentPane(mainPanel);
        frame.pack();
        frame.addWindowListener(windowListener);
        frame.addKeyListener(new WorkspaceKeyAdapter(workspace));
        
        // Start terminal
        new Thread(interpreter).start();

        // Make dragging a little faster but perhaps uglier.
        // desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);

    }
    
    /**
     * Create mappings from guiComponents to their GUI wrappers.
     */
    private static void registerComponents() {
        // TODO use a configuration file
        registerComponent(BarChartComponent.class, BarChartGui.class);
        registerComponent(ConsoleComponent.class, ConsoleDesktopComponent.class);
        registerComponent(DataWorldComponent.class, DataWorldDesktopComponent.class);
//        registerComponent(MidiWorldComponent.class, MidiWorldDesktopComponent.class);
        registerComponent(NetworkComponent.class, NetworkDesktopComponent.class);
        registerComponent(OdorWorldComponent.class, OdorWorldDesktopComponent.class);
        registerComponent(OscWorldComponent.class, OscWorldDesktopComponent.class);
        registerComponent(PieChartComponent.class, PieChartGui.class);
        registerComponent(ProjectionComponent.class, ProjectionGui.class);
        registerComponent(ScatterPlotComponent.class, ScatterPlotGui.class);
        registerComponent(ThreeDeeComponent.class, MainConsole.class);
        registerComponent(TimeSeriesPlotComponent.class, TimeSeriesPlotGui.class);
        registerComponent(TextWorldComponent.class, TextWorldDesktopComponent.class);
        registerComponent(VisionWorldComponent.class, VisionWorldDesktopComponent.class);
    }

    /**
     * @return Terminal panel.
     */
    private JConsole getTerminalPanel() {
        JConsole console = new JConsole();
        interpreter = ConsoleDesktopComponent.getSimbrainInterpreter(console, this.getWorkspace());
        try {
            interpreter.set("desktop", this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        console.setPreferredSize(new Dimension(400,300));
        return console;
    }
    
    /**
     * Print text to terminal.
     * 
     * @param toPrint text to print
     */
    public void printToTerminal(final String toPrint) {
        interpreter.println(toPrint);
    }
    /**
     * Returns the workspace.
     * 
     * @return the workspace.
     */
    public Workspace getWorkspace() {
        return workspace;
    }
    
    /**
     * Returns the main frame for the desktop.
     * 
     * @return the main frame for the desktop.
     */
    public JFrame getFrame() {
        return frame;
    }
    
    /**
     * Whether any changes have been made to the gui or
     * the underlying workspace.
     * 
     * @return whether any changes were made.
     */
    public boolean changesExist() {
        return guiChanged || workspace.changesExist();
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

        /* World menu button. */
        JButton button = new JButton();
        button.setIcon(ResourceManager.getImageIcon("World.png"));
        final JPopupMenu worldMenu = new JPopupMenu();
        for (Action action : actionManager.getNewWorldActions()) {
            worldMenu.add(action);
        }
        button.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                JButton button = (JButton) e.getSource();
                worldMenu.show(button, 0, button.getHeight());
            }
        });
        button.setComponentPopupMenu(worldMenu);
        bar.add(button);

        /* Chart menu button. */
        button = new JButton();
        button.setIcon(ResourceManager.getImageIcon("Gauge.png"));
        final JPopupMenu gaugeMenu = new JPopupMenu();
        for (Action action : actionManager.getPlotActions()) {
            gaugeMenu.add(action);
        }
        button.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                JButton button = (JButton) e.getSource();
                gaugeMenu.show(button, 0, button.getHeight());
            }
        });
        button.setComponentPopupMenu(gaugeMenu);
        bar.add(button);

        bar.add(actionManager.getNewConsoleAction());

        return bar;
    }

    /**
     * Create and attach workspace menus.
     */
    private void createAndAttachMenus() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createFileMenu());
        menuBar.add(createViewMenu());
        menuBar.add(createInsertMenu());
        menuBar.add(createScriptMenu());
        menuBar.add(createCoupleMenu());
        menuBar.add(createHelpMenu());
        frame.setJMenuBar(menuBar);
    }

    /**
     * Create script menu.
     * @return script JMenu
     */
    private JMenu createScriptMenu() {
        JMenu scriptMenu = new JMenu("Scripts");
        scriptMenu.add(actionManager.getRunScriptAction());
        scriptMenu.addSeparator();
        scriptMenu.addMenuListener(menuListener);
        for (Action action : actionManager.getScriptActions(this)) {
            scriptMenu.add(action);
        }
        return scriptMenu;
    }
    
    /**
     * Create the workspace file menu.
     *
     * @return file menu
     */
    private JMenu createFileMenu() {
        JMenu fileMenu = new JMenu("File");
        fileMenu.addMenuListener(menuListener);
        for (Action action : actionManager.getOpenSaveWorkspaceActions()) {
            fileMenu.add(action);
        }
        fileMenu.addSeparator();
        fileMenu.add(actionManager.getClearWorkspaceAction());
        fileMenu.addSeparator();
        fileMenu.add(actionManager.getOpenNetworkAction());

        JMenu worldSubMenu = new JMenu("Open World");
        for (Action action : actionManager.getOpenWorldActions()) {
            worldSubMenu.add(action);
        }
        fileMenu.add(worldSubMenu);
        fileMenu.addSeparator();
        fileMenu.add(actionManager.getQuitWorkspaceAction());
        return fileMenu;
    }

    /**
     * Create the workspace view menu.
     *
     * @return view menu
     */
    private JMenu createViewMenu() {
        JMenu viewMenu = new JMenu("View");
        viewMenu.add(actionManager.getPropertyTabAction());
        return viewMenu;
    }

    /**
     * Create the workspace insert menu.
     *
     * @return insert menu
     */
    private JMenu createInsertMenu() {
        JMenu insertMenu = new JMenu("Insert");
        insertMenu.add(actionManager.getNewNetworkAction());
        JMenu newGaugeSubMenu = new JMenu("New Plot");
        for (Action action : actionManager.getPlotActions()) {
            newGaugeSubMenu.add(action);
        }
        insertMenu.add(newGaugeSubMenu);
        JMenu newWorldSubMenu = new JMenu("New World");
        for (Action action : actionManager.getNewWorldActions()) {
            newWorldSubMenu.add(action);
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
    private JMenu createCoupleMenu() {
        JMenu coupleMenu = new JMenu("Couplings");
        coupleMenu.add(actionManager.getOpenCouplingManagerAction());
        coupleMenu.add(actionManager.getGlobalUpdateAction());
        coupleMenu.add(actionManager.getOpenCouplingListAction());
        coupleMenu.add(actionManager.getOpenWorkspaceComponentListAction());
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
        JMenu newGaugeSubMenu = new JMenu("New Plot");
        for (Action action : actionManager.getPlotActions()) {
            newGaugeSubMenu.add(action);
        }
        contextMenu.add(newGaugeSubMenu);
        JMenu newWorldSubMenu = new JMenu("New World");
        for (Action action : actionManager.getNewWorldActions()) {
            newWorldSubMenu.add(action);
        }
        contextMenu.add(newWorldSubMenu);
        contextMenu.addSeparator();
        contextMenu.add(actionManager.getNewConsoleAction());

    }
    
    /**
     * This nasty declaration creates a map of the workspace guiComponents to their associated
     * wrapper class.
     */
    private static final Map<Class<? extends WorkspaceComponent<?>>,
            Class<? extends GuiComponent<?>>> wrappers =
                new HashMap<Class<? extends WorkspaceComponent<?>>,
        Class<? extends GuiComponent<?>>>();
    
    /**
     * Registers a gui wrapper class association with a component class.
     * 
     * @param component The component class.
     * @param gui The gui class.
     */
    private static void registerComponent(final Class<? extends WorkspaceComponent<?>> component,
            final Class<? extends GuiComponent<?>> gui) {
        wrappers.put(component, gui);
    }
    
    /**
     * Returns the desktop component corresponding to a workspace component.
     * 
     * @param component component to check with
     * @return component guicomponent
     */
    public GuiComponent<?> getDesktopComponent(final WorkspaceComponent<?> component) {
        return guiComponents.get(component);
    }

    /**
     * Returns the desktop component corresponding to a named workspace
     * component.
     * 
     * @param component
     *            name of desktop component to return
     * @return component desktop component, or null if none found
     */
    public GuiComponent<?> getDesktopComponent(final String componentName) {
        WorkspaceComponent<?> wc = workspace.getComponent(componentName);
        if (wc != null) {
            return guiComponents.get(wc);
        } else {
            return null;
        }
    }

    /**
     * Utility class for adding internal frames, which are not
     * wrappers for WorkspaceComponents. Wraps GUI Component in a
     * JInternalFrame for Desktop.
     */
    private static class DesktopInternalFrame extends GenericJInternalFrame {
        
        /** Reference to workspace component. */
        private WorkspaceComponent workspaceComponent;
        
        /** Gui Component. */
        private GuiComponent guiComponent;

        /**
         * Construct an internal frame.
         * 
         * @param workspaceComponent workspace component.
         */
        public DesktopInternalFrame(final WorkspaceComponent workspaceComponent) {
            init();
            this.workspaceComponent = workspaceComponent;
        }

        /**
         * Internal desktop frame.
         * @param guiComponent component for gui rendering
         */
        public DesktopInternalFrame(final GuiComponent guiComponent) {
            init();
            this.guiComponent = guiComponent;
            this.workspaceComponent = guiComponent.getWorkspaceComponent();
        }
           
        /**
         * Initialize the frame.
         */
        private void init() {
            setResizable(true);
            setMaximizable(true);
            setIconifiable(true);
            setClosable(true);
            addInternalFrameListener(new WindowFrameListener());
        }
        
        /**
         * Set the Gui Component.
         * 
         * @param guiComponent the component to set.
         */
        public void setGuiComponent(final GuiComponent guiComponent) {
            this.guiComponent = guiComponent;
        }
        
        /**
         * Manage cleanup when a component is closed.
         */
        private class WindowFrameListener extends InternalFrameAdapter {
            /** @see InternalFrameAdapter */
            public void internalFrameClosing(final InternalFrameEvent e) {
                if (workspaceComponent.hasChangedSinceLastSave()) {
                    guiComponent.showHasChangedDialog();
                }
                
                guiComponent.close();
            }
        }
    } // End DesktopInternalFrame
    
    /**
     * Add internal frame.
     *
     * @param internalFrame the frame to add.
     */
    public void addInternalFrame(final JInternalFrame internalFrame) {
        desktop.add(internalFrame);
    }

    /**
     * Registers instance of guiComponents.
     *
     * @param workspaceComponent Workspace component
     * @param guiComponent GUI component
     */
    public void registerComponentInstance(final WorkspaceComponent workspaceComponent,
            final GuiComponent guiComponent) {
        guiComponents.put(workspaceComponent, guiComponent);
    }
    

    /**
     * Add a new <c>SimbrainComponent</c>.
     * Handles creation of new guiComponents and deserialization of guiComponents
     *
     * @param workspaceComponent Workspace Component
     * @param guiComp GUI component
     */
    @SuppressWarnings("unchecked")
    public void addComponent(final WorkspaceComponent workspaceComponent,
            final GuiComponent guiComp) {
        GuiComponent guiComponent = guiComp;

        LOGGER.trace("Adding workspace component: " + workspaceComponent);

        // How the component is created depends on whether it is being deserialized or not
        boolean isDeserialized = true;
        if (guiComponent == null) {
            isDeserialized = false;
        }
        
        final DesktopInternalFrame  componentFrame;
        if (isDeserialized) {
            componentFrame = new DesktopInternalFrame(guiComponent);
            guiComponent.setParentFrame(componentFrame);
        } else {
            componentFrame = new DesktopInternalFrame(workspaceComponent);
            guiComponent = createDesktopComponent(componentFrame, workspaceComponent);
            componentFrame.setGuiComponent(guiComponent);
        }
        
        // Set Title
        componentFrame.setTitle(workspaceComponent.getName());

        // Handle GuiComponent Bounds
        if (isDeserialized) {
           componentFrame.setBounds(guiComponent.getBounds());
        } else {
            if (guiComponents.size() == 0) {
                componentFrame.setBounds(DEFAULT_WINDOW_OFFSET, DEFAULT_WINDOW_OFFSET,
                    (int) guiComponent.getPreferredSize().getWidth(),
                    (int) guiComponent.getPreferredSize().getHeight());
                guiChanged = true;
            } else {
                int highestComponentNumber = workspace.getComponentList().size();

                componentFrame.setBounds(
                    (int) ((highestComponentNumber * DEFAULT_WINDOW_OFFSET)
                            % (desktop.getWidth() - guiComponent.getPreferredSize().getWidth())),
                    (int) ((highestComponentNumber * DEFAULT_WINDOW_OFFSET)
                            % (desktop.getHeight() - guiComponent.getPreferredSize().getHeight())),
                    (int) guiComponent.getPreferredSize().getWidth(),
                    (int) guiComponent.getPreferredSize().getHeight());
                guiChanged = true;
            }
        }

        // Finish adding component
        guiComponent.addComponentListener(componentListener);
        registerComponentInstance(workspaceComponent, guiComponent);
        componentFrame.setContentPane(guiComponent);
        componentFrame.setVisible(true);
        desktop.add(componentFrame);
        guiComponent.postAddInit();
   
        // Forces last component of the desktop to the front
        try {
            ((JInternalFrame) componentFrame).setSelected(true);
        } catch (PropertyVetoException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Creates an instance of the proper wrapper class around the provided instance.
     * 
     * @param component The component to wrap.
     * @param parentFrame The frame of this component
     * @return A new desktop component wrapping the provided component.
     */
    @SuppressWarnings("unchecked")
    static GuiComponent<?> createDesktopComponent(final GenericFrame parentFrame,
            final WorkspaceComponent<?> component) {
        Class<? extends WorkspaceComponent<?>> componentClass =
            (Class<? extends WorkspaceComponent<?>>) component.getClass();
        Class<? extends GuiComponent<?>> guiClass = wrappers.get(componentClass);
        
        if (guiClass == null) {
            throw new IllegalArgumentException(
                "no desktop component registered for " + component.getClass());
        }
        
        try {
            GenericFrame genericFrame = parentFrame != null ? parentFrame
                : new DesktopInternalFrame(component);
            
            Constructor<? extends GuiComponent<?>> constructor
                = guiClass.getConstructor(GenericFrame.class, componentClass);
            return constructor.newInstance(genericFrame, component);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    
    /**
     * Shows the dialog for opening a workspace file.
     */
    public void openWorkspace() {
        SFileChooser simulationChooser = new SFileChooser(
            workspace.getCurrentDirectory(), "Zip Archive", "zip");
        File simFile = simulationChooser.showOpenDialog();
        if (simFile != null) {
            openWorkspace(simFile);
        }
    }
    
    /**
     * Open a workspace from a file.
     *
     * @param file the file to use.
     */
    private void openWorkspace(final File file) {
        WorkspaceSerializer serializer = new WorkspaceSerializer(workspace);
        try {
            workspace.clearWorkspace();
            serializer.deserialize(new FileInputStream(file));
            workspace.setCurrentFile(file);
            frame.setTitle(file.getName());
            workspace.setWorkspaceChanged(false);
            guiChanged = false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Show Gui View of a workspace component.   Used from terminal.
     *
     * @param component component to view
     */
    public static void showJFrame(final WorkspaceComponent component) {
        
        SimbrainDesktop.registerComponents();
        GenericJFrame theFrame = new GenericJFrame();
        GuiComponent<?> desktopComponent = createDesktopComponent(theFrame, component);
        theFrame.setResizable(true);
        theFrame.setVisible(true);
        theFrame.setBounds(100,100, 200, 200);
        theFrame.setContentPane(desktopComponent);
        desktopComponent.postAddInit();
    }
    
    /**
     * If changes exist, show a change dialog, otherwise just save the current file.
     */
    public void save() {

        // Ignore the save command if there are no changes
        if (changesExist()) {
            if (workspace.getCurrentFile() != null) {
                try {
                    FileOutputStream ostream = new FileOutputStream(workspace.getCurrentFile());
                    try {
                        WorkspaceSerializer serializer =
                            new WorkspaceSerializer(this.getWorkspace());
                        serializer.serialize(ostream);
                        frame.setTitle(workspace.getCurrentFile().getName());
                        workspace.setWorkspaceChanged(false);
                        guiChanged = false;
                    } finally {
                        ostream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Show a save dialog.
     */
    public void saveAs() {
        SFileChooser chooser = new SFileChooser(
            workspace.getCurrentDirectory(), "Zip Archive", "zip");
        
        File theFile;

        if (workspace.getCurrentFile() != null) {
             theFile = chooser.showSaveDialog(workspace.getCurrentFile());
        } else {
             theFile = chooser.showSaveDialog("workspace");
        }
        
        if (theFile != null) {
            workspace.setCurrentFile(theFile);
            workspace.setCurrentDirectory(chooser.getCurrentLocation());
            save();
        }
    }

    /**
     * Create the GUI and show it. For thread safety, this method should be
     * invoked from the event-dispatching thread.
     */
    private void createAndShowGUI() {
        /*
         * Make sure we have nice window decorations.
         * JFrame.setDefaultLookAndFeelDecorated(true);
         * Create and set up the window.
         */
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        /** Open a default workspace */
        //openWorkspace(workspace.getCurrentFile());
        
        /* Display the window. */
        frame.setVisible(true);
        

    }

    /**
     * Simbrain main method.  Creates a single instance of the Simulation class
     *
     * @param args currently not used
     */
    public static void main(final String[] args) {
        final Workspace workspace = new Workspace();
        InterceptingEventQueue eventQueue = new InterceptingEventQueue(workspace);
        
        workspace.setTaskSynchronizationManager(eventQueue);
        
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(eventQueue);
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new SimbrainDesktop(workspace).createAndShowGUI();
            }
        });
    }

    /**
     * Checks to see if anything has changed and then offers to save if true.
     */
    private void showHasChangedDialog() {
        Object[] options = {"Save", "Don't Save", "Cancel" };
        int s = JOptionPane.showOptionDialog(frame,
                 "The workspace has changed since last save,"
                 + "\nWould you like to save these changes?",
                 "Workspace Has Changed", JOptionPane.YES_NO_OPTION,
                 JOptionPane.WARNING_MESSAGE, null, options, options[0]);

        if (s == JOptionPane.OK_OPTION) {
            this.save();
            quit(true);
        } else if (s == JOptionPane.NO_OPTION) {
            quit(true);
        } else if (s == JOptionPane.CANCEL_OPTION) {
            return;
        }
    }
    
    /**
     * Quit application.
     * @param forceQuit should quit be forced.
     */
    public void quit(final boolean forceQuit) {
                
        if (changesExist() && (!forceQuit)) {
            showHasChangedDialog();
        } else {
            workspace.removeAllComponents();
            System.exit(0);
        }
    }

    /** Listener for mouse presses. */
    private final MouseListener mouseListener = new MouseAdapter() {
        /**
         * Responds to mouse events.
         *
         * @param mouseEvent Mouse Event
         */
        public void mousePressed(final MouseEvent mouseEvent) {
            Point lastClickedPoint = mouseEvent.getPoint();
            if (mouseEvent.isControlDown() || (mouseEvent.getButton() == MouseEvent.BUTTON3)) {
                contextMenu.show(frame, (int) lastClickedPoint.getX() + MENU_X_OFFSET,
                    (int) lastClickedPoint.getY() + MENU_Y_OFFSET);
            }
        }
    };
    
    /** listener for window closing events. */
    private final WindowListener windowListener = new WindowAdapter() {
        /**
         * Responds to window closing events.
         * @param arg0 Window event
         */
        public void windowClosing(final WindowEvent arg0) {
            quit(false);
        }
    };
    
    /** Listener for swing component changes. */
    private final ComponentListener componentListener = new ComponentAdapter() {
        /**
         * Responds to component moved events.
         * @param arg0 SimbrainComponent event
         */
        public void componentMoved(final ComponentEvent arg0) {
            guiChanged = true;
        }

        /**
         * Responds to component resized events.
         * @param arg0 SimbrainComponent event
         */
        public void componentResized(final ComponentEvent arg0) {
            guiChanged = true;
        }
    };
    
    /** listens to menu events for setting save enabled. */
    private final MenuListener menuListener = new MenuListener() {
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
            /* no implementation */
        }

        /**
         * Responds to menu canceled events.
         * @param arg0 Menu event
         */
        public void menuCanceled(final MenuEvent arg0) {
            /* no implementation */
        }
    };

    /**
     * Provisional Code for toggling tab dock's visibility.
     */
    public void toggleDock() {
        if (dockVisible) {
            dockVisible = false;
            horizontalSplitter.getBottomComponent().setVisible(false);
        } else {
            dockVisible = true;
            horizontalSplitter.getBottomComponent().setVisible(true);
        }

    }
}
