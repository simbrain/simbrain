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

import bsh.Interpreter;
import bsh.util.JConsole;
import kotlin.Unit;
import kotlinx.coroutines.CoroutineScopeKt;
import org.pmw.tinylog.Logger;
import org.simbrain.console.ConsoleDesktopComponent;
import org.simbrain.custom_sims.NewSimulation;
import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.RegisteredSimulationsKt;
import org.simbrain.util.ResourceManager;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.genericframe.GenericJFrame;
import org.simbrain.util.genericframe.GenericJInternalFrame;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.util.widgets.ToggleButton;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.events.WorkspaceEvents;
import org.simbrain.workspace.updater.WorkspaceUpdaterListener;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyVetoException;
import java.io.File;
import java.util.*;

/**
 * Creates a Swing-based environment for working with a workspace.
 * <p>
 * Also provides wrappers for GUI elements called from a terminal.
 *
 * @author Matt Watson
 * @author Jeff Yoshimi
 */
public class SimbrainDesktop {

    /**
     * The x offset for popup menus.
     */
    private static final int MENU_X_OFFSET = 5;

    /**
     * The y offset for popup menus.
     */
    private static final int MENU_Y_OFFSET = 53;

    /**
     * The default serial version ID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Initial indent of entire workspace.
     */
    private static final int WORKSPACE_INSET = 80;

    /**
     * After placing one simbrain window how far away to put the next one.
     */
    private static final int DEFAULT_WINDOW_OFFSET = 30;

    /**
     * Reference to the last internal frames that were focused, so that they can get the focus when the next one is
     * closed.
     */
    private static final Stack<DesktopComponent<?>> lastFocusedStack = new Stack<DesktopComponent<?>>();

    /**
     * TODO: Create Javadoc comment.
     */
    private static final Map<Workspace, SimbrainDesktop> INSTANCES = new HashMap<Workspace, SimbrainDesktop>();

    // TODO: Review. Part of a hack solution in NeuronGroupNode
    // and SynapseGroup dialog, useful anyway?
    public static Map<Workspace, SimbrainDesktop> getInstances() {
        return INSTANCES;
    }

    /**
     * Desktop pane.
     */
    private JDesktopPane desktop;

    /**
     * Cached context menu.
     */
    private JPopupMenu contextMenu;

    /**
     * Workspace tool bar.
     */
    private JToolBar wsToolBar = new JToolBar();

    /**
     * Whether the bottom dock is visible.
     */
    private boolean dockVisible = true;

    /**
     * the frame that will hold the workspace.
     */
    private JFrame frame;

    /**
     * The bottom dock.
     */
    private JTabbedPane bottomDock;

    /**
     * Pane splitter for bottom dock.
     */
    private JSplitPane horizontalSplitter;

    /**
     * The workspace this desktop wraps.
     */
    private final Workspace workspace;

    /**
     * Boundary of workspace.
     */
    private Rectangle workspaceBounds;

    /**
     * Workspace action manager.
     */
    private WorkspaceActionManager actionManager;

    /**
     * Interpreter for terminal.
     */
    private Interpreter interpreter;

    /**
     * Time indicator.
     */
    private JLabel timeLabel = new JLabel();

    /**
     * "Throbber" to indicate a simulation is running.
     */
    private JLabel runningLabel = new JLabel();

    /**
     * Update rate for display.
     */
    private int updateRate = 0;

    /**
     * Timer to calculate update rate.
     */
    private long lastUpdateTimeMs = 0;

    /**
     * Timestep at the last update rate calculation.
     */
    private int lastTimestep = 0;

    /**
     * Name to display in Simbrain desktop window.
     */
    private static String FRAME_TITLE = "Simbrain 4 Beta";

    /**
     * Associates workspace components with their corresponding gui components.
     */
    private static Map<WorkspaceComponent, DesktopComponent<?>> guiComponents = new LinkedHashMap<WorkspaceComponent, DesktopComponent<?>>();

    /**
     * Associates script submenunames ({@link RegisteredSimulation#getSubmenuName()})
     * with submenus in the script menu.
     */
    private HashMap<String, JMenu> submenuMap = new HashMap<>();

    /**
     * Listens for workspace updater events.
     */
    private final WorkspaceUpdaterListener updaterListener = new WorkspaceUpdaterListener() {

        @Override
        public void changeNumThreads() {
        }

        @Override
        public void changedUpdateController() {
        }

        @Override
        public void updatedCouplings(int update) {
        }

        @Override
        public void updatingStarted() {
            StandardDialog.setSimulationRunning(true);
        }

        @Override
        public void updatingFinished() {
            StandardDialog.setSimulationRunning(false);
        }

        @Override
        public void workspaceUpdated() {
            updateTimeLabel();
        }
    };

    // TODO this should be addressed at a higher level
    public static SimbrainDesktop getDesktop(final Workspace workspace) {
        return INSTANCES.get(workspace);
    }

    /**
     * Default constructor.
     *
     * @param workspace The workspace for this desktop.
     */
    public SimbrainDesktop(final Workspace workspace) {
        INSTANCES.put(workspace, this);
        this.workspace = workspace;
        frame = new JFrame(FRAME_TITLE);
        frame.setIconImages(Arrays.asList(
                ResourceManager.getImage("simbrain_iconset" + Utils.FS + "20.png"),
                ResourceManager.getImage("simbrain_iconset" + Utils.FS + "32.png"),
                ResourceManager.getImage("simbrain_iconset" + Utils.FS + "40.png"),
                ResourceManager.getImage("simbrain_iconset" + Utils.FS + "64.png"),
                ResourceManager.getImage("simbrain_iconset" + Utils.FS + "128.png"),
                ResourceManager.getImage("simbrain_iconset" + Utils.FS + "512.png")
        ));
        actionManager = new WorkspaceActionManager(this);
        createAndAttachMenus();
        wsToolBar = createToolBar();
        createContextMenu();
        WorkspaceEvents events = workspace.getEvents();

        events.onWorkspaceCleared(() -> {
            guiComponents.clear();
            desktop.removeAll();
            desktop.repaint();
            frame.setTitle(FRAME_TITLE);
            lastTimestep = 0;
            updateTimeLabel();
        });

        events.onComponentAdded(workspaceComponent -> addDesktopComponent(workspaceComponent));

        events.onComponentRemoved(wc -> {
            DesktopComponent<?> component = guiComponents.get(wc);
            if (component == null) {
                return;
            }
            guiComponents.remove(component);
            component.getParentFrame().dispose();
            if (!lastFocusedStack.isEmpty()) {
                lastFocusedStack.remove(component);
            }
            moveLastFocusedComponentToFront();
        });

        events.onNewWorkspaceOpened(() -> {
            frame.setTitle(workspace.getCurrentFile().getName());
            lastTimestep = 0;
            updateTimeLabel();
        });

        workspace.getUpdater().addUpdaterListener(updaterListener);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        workspaceBounds = new Rectangle(WORKSPACE_INSET, WORKSPACE_INSET, screenSize.width - (WORKSPACE_INSET * 2), screenSize.height - (WORKSPACE_INSET * 2));

        // Set the bottom dock to visible or not based on the properties file.
        Properties properties = Utils.getSimbrainProperties();
        if (properties.containsKey("showBottomDock")) {
            dockVisible = Boolean.parseBoolean(properties.getProperty("showBottomDock"));
        }

        // Set up Desktop
        desktop = new JDesktopPane();
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            desktop.setBackground(Color.WHITE);
            desktop.setBorder(BorderFactory.createLoweredBevelBorder());
        }
        desktop.addMouseListener(mouseListener);
        desktop.addKeyListener(new WorkspaceKeyAdapter(workspace));
        desktop.setPreferredSize(new Dimension(screenSize.width - (WORKSPACE_INSET * 2), screenSize.height - (WORKSPACE_INSET * 3)));

        // Create the Tabbed Pane for bottom of the desktop
        bottomDock = new JTabbedPane();
        bottomDock.addTab("Components", null, new ComponentPanel(this), "Show workspace components");
        bottomDock.addTab("Terminal", null, this.getTerminalPanel(), "Simbrain terminal");
        bottomDock.addTab("Performance", null, new PerformanceMonitorPanel(this.getWorkspace()), "Performance and thread monitoring");

        // Set up the main panel
        horizontalSplitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        horizontalSplitter.setDividerLocation(getDividerLocation());
        horizontalSplitter.setTopComponent(desktop);
        horizontalSplitter.setBottomComponent(bottomDock);
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(wsToolBar, "North");
        mainPanel.add(horizontalSplitter, "Center");
        if (!dockVisible) {
            horizontalSplitter.getBottomComponent().setVisible(false);
        }

        // Set up Frame
        frame.setBounds(workspaceBounds);
        frame.setContentPane(mainPanel);
        frame.pack();
        frame.addWindowListener(windowListener);
        frame.addKeyListener(new WorkspaceKeyAdapter(workspace));

        // Set the "dock" image.
        if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
            Taskbar.getTaskbar().setIconImage((ResourceManager.getImage("simbrain_iconset" + Utils.FS + "128.png")));
        }

        // Start terminal
        new Thread(interpreter).start();

        // Make dragging a little faster but perhaps uglier.
        // desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
    }

    /**
     * Listener for swing component changes.
     */
    private final ComponentListener componentListener = new ComponentAdapter() {
        /**
         * Responds to component moved events.
         *
         * @param event SimbrainComponent event
         */
        @Override
        public void componentMoved(final ComponentEvent event) {

            // Prevent window from being moved outside of visible area
            int x = (int) event.getComponent().getBounds().getX();
            int y = (int) event.getComponent().getBounds().getY();
            int width = (int) event.getComponent().getBounds().getWidth();
            int height = (int) event.getComponent().getBounds().getHeight();
            if (x < desktop.getVisibleRect().getX()) {
                event.getComponent().setBounds(0, y, width, height);
            }
            if (y < desktop.getVisibleRect().getY()) {
                event.getComponent().setBounds(x, 0, width, height);
            }

            // Workspace has changed
            workspace.setWorkspaceChanged(true);
        }

        /**
         * Responds to component resized events.
         *
         * @param arg0 SimbrainComponent event
         */
        @Override
        public void componentResized(final ComponentEvent arg0) {
            // System.out.println("Component resized");
            workspace.setWorkspaceChanged(true);
        }
    };

    /**
     * Takes the last gui component opened and moves it to the front of the simbrain desktop, place it in focus.
     */
    private void moveLastFocusedComponentToFront() {
        if (!lastFocusedStack.isEmpty()) {
            DesktopComponent<?> lastFocused = lastFocusedStack.peek();
            if (lastFocused != null) {
                try {
                    ((JInternalFrame) lastFocused.getParentFrame()).setSelected(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
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
        console.setPreferredSize(new Dimension(400, 300));
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
     * Creates the workspace tool bar.
     *
     * @return JToolBar tool bar created
     */
    private JToolBar createToolBar() {
        JToolBar bar = new JToolBar();

        bar.add(actionManager.getOpenWorkspaceAction());
        bar.add(actionManager.getSaveWorkspaceAction());
        bar.addSeparator();
        bar.add(actionManager.getIterateAction());
        bar.add(new ToggleButton(actionManager.getRunControlActions()));

        bar.addSeparator();
        bar.add(actionManager.getOpenCouplingManagerAction());

        bar.addSeparator();
        bar.add(actionManager.getNewNetworkAction());

        /* World menu button. */
        JButton button = new JButton();
        button.setIcon(ResourceManager.getImageIcon("menu_icons/World.png"));
        final JPopupMenu worldMenu = new JPopupMenu();
        for (Action action : actionManager.getNewWorldActions()) {
            worldMenu.add(action);
        }
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                JButton button = (JButton) e.getSource();
                worldMenu.show(button, 0, button.getHeight());
            }
        });
        button.setComponentPopupMenu(worldMenu);
        bar.add(button);

        /* Chart menu button. */
        button = new JButton();
        button.setIcon(ResourceManager.getImageIcon("menu_icons/BarChart.png"));
        final JPopupMenu gaugeMenu = new JPopupMenu();
        for (Action action : actionManager.getPlotActions()) {
            gaugeMenu.add(action);
        }
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                JButton button = (JButton) e.getSource();
                gaugeMenu.show(button, 0, button.getHeight());
            }
        });
        button.setComponentPopupMenu(gaugeMenu);
        bar.add(button);
        bar.add(actionManager.getNewConsoleAction());

        // Initialize time label
        timeLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        timeLabel.addMouseListener(new MouseAdapter() {
            // Reset time if user double clicks on label.
            @Override
            public void mousePressed(final MouseEvent event) {
                if (event.getClickCount() == 2) {
                    workspace.getUpdater().resetTime();
                    lastTimestep = 0;
                    updateTimeLabel();
                }
            }
        });
        runningLabel.setIcon(ResourceManager.getImageIcon("menu_icons/Throbber.gif"));
        runningLabel.setVisible(false);
        updateTimeLabel();
        bar.add(timeLabel);
        bar.add(runningLabel);

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
     *
     * @return script JMenu
     */
    private JMenu createScriptMenu() {
        JMenu scriptMenu = new JMenu("Simulations");
        // scriptMenu.add(actionManager.getRunScriptAction());
        scriptMenu.add(actionManager.getShowScriptEditorAction());
        scriptMenu.addSeparator();
        scriptMenu.addMenuListener(menuListener);
        for (RegisteredSimulation rs : RegisteredSimulation.getRegisteredSims()) {

            JMenuItem menuItem = new JMenuItem(rs.getName());
            menuItem.addActionListener(ae -> {
                rs.instantiate(this).run();
            });

            String submenuName = rs.getSubmenuName();
            if (submenuName == null) {
                // There is no submenu; add to main menu
                scriptMenu.add(menuItem);
            } else {
                // Check whether the script menu contains the submenu
                JMenu existingMenu = submenuMap.get(submenuName);
                if(existingMenu == null) {
                    // Create a new submenu
                    JMenu submenu = new JMenu(submenuName);
                    submenuMap.put(submenuName, submenu);
                    submenu.add(menuItem);
                    scriptMenu.add(submenu);
                } else {
                    // Add to existing submenu
                    existingMenu.add(menuItem);
                }
            }
        }
        scriptMenu.addSeparator();
        RegisteredSimulationsKt.getSimulations().addToMenu(scriptMenu, newSimulation -> {
            if (newSimulation instanceof NewSimulation) {
                ((NewSimulation) newSimulation).run(this);
            } else if (newSimulation instanceof RegisteredSimulation) {
                ((RegisteredSimulation)newSimulation).instantiate(this).run();
            }
            return Unit.INSTANCE;
        });
        if (actionManager.getScriptActions(this) == null) {
            JOptionPane.showOptionDialog(null, "To use scripts place Simbrain.jar in the same directory as the scripts directory and restart.", "Warning", JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE, null, null, null);
        } else {
            scriptMenu.addSeparator();
            for (Action action : actionManager.getScriptActions(this)) {
                scriptMenu.add(action);
            }
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
        fileMenu.add(actionManager.getShowUpdaterDialog());
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
        viewMenu.addSeparator();
        viewMenu.add(new JMenuItem(actionManager.getResizeAllWindowsAction()));
        viewMenu.add(new JMenuItem(actionManager.getRepositionAllWindowsAction()));
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
        // insertMenu.add(new OpenEditorAction(this)); //TODO: Move this action
        // manager
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
        insertMenu.add(actionManager.getNewDocViewerAction());
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
        coupleMenu.add(actionManager.getOpenCouplingListAction());
        return coupleMenu;
    }

    /**
     * Create the workspace help menu.
     *
     * @return help menu
     */
    private JMenu createHelpMenu() {
        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(new ShowHelpAction("Main Help", "SimbrainDocs.html"));
        helpMenu.addSeparator();
        helpMenu.add(new ShowHelpAction("Quick start", "Pages/QuickStart.html"));
        helpMenu.add(new ShowHelpAction("Keyboard Shortcuts", "KeyboardShortcuts.html"));
        helpMenu.add(new ShowHelpAction("Credits", "SimbrainCredits.html"));
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
        contextMenu.add(actionManager.getNewDocViewerAction());
        contextMenu.add(actionManager.getNewConsoleAction());

    }

    /**
     * Returns a list of all desktop components.
     *
     * @return the list of components
     */
    public Collection<DesktopComponent<?>> getDesktopComponents() {
        return guiComponents.values();
    }

    /**
     * Returns the desktop component corresponding to a workspace component.
     *
     * @param component component to check with
     * @return component guicomponent
     */
    public DesktopComponent<?> getDesktopComponent(final WorkspaceComponent component) {
        return guiComponents.get(component);
    }

    /**
     * Returns the desktop component corresponding to a named workspace component.
     *
     * @param componentName name of desktop component to return
     * @return component desktop component, or null if none found
     */
    public DesktopComponent<?> getDesktopComponent(final String componentName) {
        WorkspaceComponent wc = workspace.getComponent(componentName);
        if (wc != null) {
            return guiComponents.get(wc);
        } else {
            return null;
        }
    }

    /**
     * Utility class for adding internal frames, which are not wrappers for WorkspaceComponents. Wraps GUI Component in
     * a JInternalFrame for Desktop.
     */
    private static class DesktopInternalFrame extends GenericJInternalFrame {

        /**
         * Reference to workspace component.
         */
        private WorkspaceComponent workspaceComponent;

        /**
         * Gui Component.
         */
        private DesktopComponent desktopComponent;

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
         * Initialize the frame.
         */
        private void init() {
            setResizable(true);
            setMaximizable(true);
            setIconifiable(true);
            setClosable(true);
            setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            addInternalFrameListener(new WindowFrameListener());
        }

        /**
         * Set the Gui Component.
         *
         * @param desktopComponent the component to set.
         */
        public void setGuiComponent(final DesktopComponent desktopComponent) {
            this.desktopComponent = desktopComponent;
        }

        /**
         * Manage cleanup when a component is closed.
         */
        private class WindowFrameListener extends InternalFrameAdapter {
            @Override
            public void internalFrameActivated(final InternalFrameEvent e) {
                // TODO: Does not work properly. Should be used so that
                // the last focused stack tracks changes in focus and not just
                // open / close events.
                // lastFocusedStack.remove(guiComponent);
                // lastFocusedStack.push(guiComponent);
            }

            @Override
            public void internalFrameOpened(InternalFrameEvent e) {
                super.internalFrameOpened(e);
            }

            @Override
            public void internalFrameClosing(final InternalFrameEvent e) {
                desktopComponent.close();
                guiComponents.remove(workspaceComponent);
            }

            @Override
            public void internalFrameClosed(InternalFrameEvent e) {
                super.internalFrameClosed(e);
            }

        }
    } // End DesktopInternalFrame

    /**
     * Add internal frame.
     *
     * @param internalFrame the frame to add.
     */
    public void addInternalFrame(final JInternalFrame internalFrame) {
        internalFrame.addInternalFrameListener(new InternalFrameListener() {

            @Override
            public void internalFrameActivated(InternalFrameEvent arg0) {
            }

            @Override
            public void internalFrameClosed(InternalFrameEvent arg0) {
            }

            @Override
            public void internalFrameClosing(InternalFrameEvent arg0) {
                moveLastFocusedComponentToFront();
            }

            @Override
            public void internalFrameDeactivated(InternalFrameEvent arg0) {
            }

            @Override
            public void internalFrameDeiconified(InternalFrameEvent arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void internalFrameIconified(InternalFrameEvent arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void internalFrameOpened(InternalFrameEvent arg0) {
                // TODO Auto-generated method stub

            }

        });
        desktop.add(internalFrame);
    }

    /**
     * Registers instance of guiComponents.
     *
     * @param workspaceComponent Workspace component
     * @param desktopComponent       GUI component
     */
    public void registerComponentInstance(final WorkspaceComponent workspaceComponent,
                                          final DesktopComponent desktopComponent) {
        desktopComponent.setDesktop(this);
        guiComponents.put(workspaceComponent, desktopComponent);
    }

    /**
     * Add a new SimbrainComponent.
     *
     * @param workspaceComponent Workspace Component
     */
    @SuppressWarnings("unchecked")
    public void addDesktopComponent(final WorkspaceComponent workspaceComponent) {
        Logger.trace("Adding workspace component: " + workspaceComponent);

        final DesktopInternalFrame componentFrame = new DesktopInternalFrame(workspaceComponent);
        // componentFrame.setFrameIcon(new ImageIcon(ResourceManager.getImage("icons/20.png")));

        DesktopComponent<?> desktopComponent = createDesktopComponent(componentFrame, workspaceComponent);
        componentFrame.setGuiComponent(desktopComponent);

        // Either add the window at a default location, or relative to the last
        // added window. Note that this is overridden when individual
        // components are opened
        if (guiComponents.size() == 0) {
            componentFrame.setBounds(DEFAULT_WINDOW_OFFSET, DEFAULT_WINDOW_OFFSET,
                    (int) desktopComponent.getPreferredSize().getWidth(),
                    (int) desktopComponent.getPreferredSize().getHeight());
        } else {
            // This should be coordinated with the logic in
            // RepositionAllWindowsSction
            int highestComponentNumber = guiComponents.size() + 1;
            double xMax = desktop.getWidth() - desktopComponent.getPreferredSize().getWidth();
            double yMax = desktop.getHeight() - desktopComponent.getPreferredSize().getHeight();
            componentFrame.setBounds(
                    (int) ((highestComponentNumber * DEFAULT_WINDOW_OFFSET) % xMax),
                    (int) ((highestComponentNumber * DEFAULT_WINDOW_OFFSET) % yMax),
                    (int) desktopComponent.getPreferredSize().getWidth(),
                    (int) desktopComponent.getPreferredSize().getHeight());
        }

        // Other initialization
        componentFrame.addComponentListener(componentListener);
        componentFrame.setContentPane(desktopComponent);
        registerComponentInstance(workspaceComponent, desktopComponent);
        componentFrame.setVisible(true);
        componentFrame.setTitle(workspaceComponent.getName());
        desktop.add(componentFrame);
        lastFocusedStack.push(desktopComponent);
        desktopComponent.getParentFrame().pack();
        // System.out.println(lastOpened.getName());

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
     * @param component   The component to wrap.
     * @param parentFrame The frame of this component
     * @return A new desktop component wrapping the provided component.
     */
    static DesktopComponent<?> createDesktopComponent(GenericFrame parentFrame, WorkspaceComponent component) {
        GenericFrame genericFrame = parentFrame != null ? parentFrame : new DesktopInternalFrame(component);
        return component.getWorkspace().getComponentFactory().createGuiComponent(genericFrame, component);
    }

    /**
     * Shows the dialog for opening a workspace file.
     */
    public void openWorkspace() {
        SFileChooser simulationChooser = new SFileChooser(workspace.getCurrentDirectory(), "Zip Archive", "zip");
        File simFile = simulationChooser.showOpenDialog();
        if (simFile != null) {
            workspace.openWorkspace(simFile);
            workspace.setCurrentDirectory(simulationChooser.getCurrentLocation());
            workspace.setCurrentFile(simFile);
        }
    }

    /**
     * Show Gui View of a workspace component. Used from terminal.
     *
     * @param component component to view
     */
    public static void showJFrame(final WorkspaceComponent component) {
        GenericJFrame theFrame = new GenericJFrame();
        DesktopComponent<?> desktopComponent = createDesktopComponent(theFrame, component);
        theFrame.setResizable(true);
        theFrame.setVisible(true);
        theFrame.setBounds(100, 100, 200, 200);
        theFrame.setContentPane(desktopComponent);
    }

    /**
     * Show a save-as dialog.
     */
    public void saveAs() {

        // Create the file chooser
        SFileChooser chooser = new SFileChooser(workspace.getCurrentDirectory(), "Zip Archive", "zip");

        // Set the file
        File theFile;
        if (workspace.getCurrentFile() != null) {
            theFile = chooser.showSaveDialog(workspace.getCurrentFile());
        } else {
            // Default workspace
            theFile = chooser.showSaveDialog("workspace");
        }

        // Save the file by setting the current file
        if (theFile != null) {
            workspace.setCurrentFile(theFile);
            workspace.setCurrentDirectory(chooser.getCurrentLocation());
            save(theFile);
        }
    }

    /**
     * If changes exist, show a change dialog, otherwise just save the current file.
     */
    public void save() {

        // Ignore the save command if there are no changes
        if (workspace.changesExist()) {
            if (workspace.getCurrentFile() != null) {
                save(workspace.getCurrentFile());
            } else {
                saveAs(); // Show save-as if there is no current file.
            }
        }
    }

    /**
     * Save a specified file.
     *
     * @param file file to save.
     */
    private void save(File file) {
        if (file != null) {
            frame.setTitle(file.getName());
            workspace.save(file);
        }
    }

    /**
     * Clear desktop of all components. Show a save-as dialog if there have been changes.
     */
    public void clearDesktop() {

        // If there have been changes, show a save-as dialog
        if (workspace.changesExist()) {
            int s = showHasChangedDialog();
            if (s == JOptionPane.OK_OPTION) {
                save();
                clearComponents();
            } else if (s == JOptionPane.NO_OPTION) {
                clearComponents();
            } else if (s == JOptionPane.CANCEL_OPTION) {
                return;
            }
        } else {
            // If there have been no changes, just clear away!
            clearComponents();
        }
    }

    /**
     * Helper method to clear all components from the desktop.
     */
    private void clearComponents() {
        guiComponents.clear();
        workspace.clearWorkspace();
    }

    /**
     * Create the GUI and show it. For thread safety, this method should be invoked from the event-dispatching thread.
     */
    private void createAndShowGUI() {
        /*
         * Make sure we have nice window decorations.
         * JFrame.setDefaultLookAndFeelDecorated(true); Create and set up the
         * window.
         */
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        /** Open a default workspace */
        // openWorkspace(workspace.getCurrentFile());

        /* Display the window. */
        frame.setVisible(true);

    }

    /**
     * Simbrain main method. Creates a single instance of the Simulation class
     *
     * @param args currently not used
     */
    public static void main(final String[] args) {

        var coroutineScope = CoroutineScopeKt.MainScope();

        final Workspace workspace = new Workspace(coroutineScope);

        try {
            // Line below for Ubuntu so that icons don't turn on by default
            // See https://stackoverflow.com/questions/10356725/jdesktoppane-has-a-toolbar-at-bottom-of-window-on-linux
            if (Utils.isLinux()) {
                UIManager.put("DesktopPaneUI","javax.swing.plaf.basic.BasicDesktopPaneUI");
            }
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // InterceptingEventQueue eventQueue = new InterceptingEventQueue(workspace);
        //
        // workspace.setTaskSynchronizationManager(eventQueue);
        //
        // Toolkit.getDefaultToolkit().getSystemEventQueue().push(eventQueue);


        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new SimbrainDesktop(workspace).createAndShowGUI();
            }
        });
    }

    /**
     * Checks to see if anything has changed and then offers to save if true.
     *
     * @return the JOptionPane pane result
     */
    private int showHasChangedDialog() {
        Object[] options = {"Save", "Don't Save", "Cancel"};
        return JOptionPane.showOptionDialog(frame, "The workspace has changed since last save," + "\nWould you like to save these changes?", "Workspace Has Changed", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
    }

    /**
     * Quit application.
     *
     * @param forceQuit should quit be forced.
     */
    public void quit(final boolean forceQuit) {

        if (workspace.changesExist() && (!forceQuit) && (workspace.getComponentList().size() > 0)) {
            int s = showHasChangedDialog();
            if (s == JOptionPane.OK_OPTION) {
                save();
                quit(true);
            } else if (s == JOptionPane.NO_OPTION) {
                quit(true);
            } else if (s == JOptionPane.CANCEL_OPTION) {
                return;
            }
        } else {
            workspace.removeAllComponents();
            System.exit(0);
        }
    }

    /**
     * Listener for mouse presses.
     */
    private final MouseListener mouseListener = new MouseAdapter() {
        /**
         * Responds to mouse events.
         *
         * @param mouseEvent Mouse Event
         */
        @Override
        public void mousePressed(final MouseEvent mouseEvent) {
            Point lastClickedPoint = mouseEvent.getPoint();
            // System.out.println("desktop-->" + lastClickedPoint); //TODO: Make
            // this visible somehow
            if (mouseEvent.isControlDown() || (mouseEvent.getButton() == MouseEvent.BUTTON3)) {
                contextMenu.show(frame, (int) lastClickedPoint.getX() + MENU_X_OFFSET, (int) lastClickedPoint.getY() + MENU_Y_OFFSET);
            }
        }
    };

    /**
     * listener for window closing events.
     */
    private final WindowListener windowListener = new WindowAdapter() {
        /**
         * Responds to window closing events.
         *
         * @param arg0 Window event
         */
        @Override
        public void windowClosing(final WindowEvent arg0) {
            quit(false);
        }
    };

    /**
     * listens to menu events for setting save enabled.
     */
    private final MenuListener menuListener = new MenuListener() {
        /**
         * Responds to menu selected events.
         *
         * @param arg0 Menu event
         */
        @Override
        public void menuSelected(final MenuEvent arg0) {
            if (workspace.changesExist()) {
                actionManager.getSaveWorkspaceAction().setEnabled(true);
            } else {
                actionManager.getSaveWorkspaceAction().setEnabled(false);
            }
        }

        /**
         * Responds to menu deslected events.
         *
         * @param arg0 Menu event
         */
        @Override
        public void menuDeselected(final MenuEvent arg0) {
            /* no implementation */
        }

        /**
         * Responds to menu canceled events.
         *
         * @param arg0 Menu event
         */
        @Override
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
            horizontalSplitter.setDividerLocation(getDividerLocation());
        }

    }

    /**
     * Update time label.
     */
    public void updateTimeLabel() {
        int timestep = workspace.getTime();
        long updateTimeMs = System.currentTimeMillis();
        if (updateTimeMs - lastUpdateTimeMs > 1000) {
            updateRate = timestep - lastTimestep;
            lastTimestep = timestep;
            lastUpdateTimeMs = updateTimeMs;
        }

        String text = String.format("Timestep: %s (%sHz)", timestep, updateRate);
        timeLabel.setText(text);
        if (workspace.getUpdater().isRunning()) {
            runningLabel.setVisible(true);
        } else {
            runningLabel.setVisible(false);
        }
    }

    /**
     * Helper method for determining where the bottom tab should be placed.
     *
     * @return the location
     */
    private int getDividerLocation() {
        return (int) (3 * (workspaceBounds.getHeight() / 4));
    }

    /**
     * Returns the width of the visible portion of the desktop.
     *
     * @return visible width.
     */
    public double getWidth() {
        return desktop.getVisibleRect().getWidth();
    }

    /**
     * Returns the height of the visible portion of the desktop.
     *
     * @return the visible height
     */
    public double getHeight() {
        return desktop.getVisibleRect().getHeight();
    }

    /**
     * Returns the internal desktop object. Sometimes useful in scripts.
     *
     * @return
     */
    public JDesktopPane getDesktop() {
        return desktop;
    }

    /**
     * Position a component given an index. Lays out components in a pattern moving diagonally and downward across the
     * desktop.
     * <p>
     * Note that this is overridden when individual components are opened.
     *
     * @param positionIndex
     * @param desktopComponent
     */
    public void positionComponent(int positionIndex,
                                  DesktopComponent<?> desktopComponent) {

        // TODO: Some better logic that detects whether some existing slot is
        // open would be nice, but this does well enough for now...

        if (positionIndex == 0) {
            // If this is the first window at it at a default position
            desktopComponent.getParentFrame().setBounds(DEFAULT_WINDOW_OFFSET,
                    DEFAULT_WINDOW_OFFSET,
                    (int) desktopComponent.getPreferredSize().getWidth(),
                    (int) desktopComponent.getPreferredSize().getHeight());
        } else {
            // Add window below the current window at a slight offent
            desktopComponent.getParentFrame().setBounds(
                    (int) (((positionIndex + 1) * DEFAULT_WINDOW_OFFSET)
                            % (desktop.getWidth() - desktopComponent
                            .getPreferredSize().getWidth())),
                    (int) (((positionIndex + 1) * DEFAULT_WINDOW_OFFSET)
                            % (desktop.getHeight() - desktopComponent
                            .getPreferredSize().getHeight())),
                    (int) desktopComponent.getPreferredSize().getWidth(),
                    (int) desktopComponent.getPreferredSize().getHeight());
            // Focus the last positioned frame to have the focus
            try {
                ((JInternalFrame) desktopComponent.getParentFrame())
                        .setSelected(true);
            } catch (PropertyVetoException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Reposition all the windows. Useful when windows get resized and can't be "recaptured".
     */
    public void repositionAllWindows() {
        // TODO: Do this for non-component internal frames as well?
        int i = 0;
        for (DesktopComponent<?> component : getDesktopComponents()) {
            positionComponent(i++, component);
        }
    }
}
