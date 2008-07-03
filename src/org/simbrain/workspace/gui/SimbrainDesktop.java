package org.simbrain.workspace.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
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
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.apache.log4j.Logger;
import org.simbrain.console.ConsoleComponent;
import org.simbrain.console.ConsoleDesktopComponent;
import org.simbrain.gauge.GaugeComponent;
import org.simbrain.gauge.GaugeDesktopComponent;
import org.simbrain.network.gui.NetworkComponent;
import org.simbrain.network.gui.NetworkDesktopComponent;
import org.simbrain.plot.PlotComponent;
import org.simbrain.plot.PlotDesktopComponent;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.ToggleButton;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.ProducingAttribute;
import org.simbrain.workspace.SingleAttributeConsumer;
import org.simbrain.workspace.SingleAttributeProducer;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceListener;
import org.simbrain.workspace.WorkspacePreferences;
import org.simbrain.workspace.WorkspaceSerializer;
import org.simbrain.world.dataworld.DataWorldComponent;
import org.simbrain.world.dataworld.DataWorldDesktopComponent;
import org.simbrain.world.gameworld2d.GameWorld2DComponent;
import org.simbrain.world.gameworld2d.GameWorld2DDesktopComponent;
import org.simbrain.world.midiworld.MidiWorldComponent;
import org.simbrain.world.midiworld.MidiWorldDesktopComponent;
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

/**
 * Creates a Swing-based environment for working with a workspace.
 * 
 * Also provides wrappers for GUI elements called from a terminal.
 * 
 * @author Matt Watson
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
    private static final int WORKSPACE_INSET = 50;

    /** After placing one simbrain window how far away to put the next one. */
    private static final int DEFAULT_WINDOW_OFFSET = 30;

    private static final Map<Workspace, SimbrainDesktop> INSTANCES
        = new HashMap<Workspace, SimbrainDesktop>();
    
    /** Desktop pane. */
    JDesktopPane desktop;

    /** Cached context menu. */
    private JPopupMenu contextMenu;

    /** Workspace tool bar. */
    private JToolBar wsToolBar = new JToolBar();

    /** the frame that will hold the workspace. */
    private JFrame frame;
    
    /** Used to track whether the gui has been modified since the last save. */
    private boolean guiChanged = false;
    
    /** Last clicked point. */
    private Point lastClickedPoint = null;
    
    /** The workspace this desktop wraps. */
    private final Workspace workspace;
    
    /** Workspace action manager. */
    private WorkspaceActionManager actionManager;

    /**
     * Mapping from workspace component types to integers which show how many have been added.
     * For naming.
     */
    private Hashtable<Class<?>, Integer> componentNameIndices = new Hashtable<Class<?>, Integer>();

    /** All the components in the workspace. */
    private Map<WorkspaceComponent<?>, GuiComponent<?>> components
        = new LinkedHashMap<WorkspaceComponent<?>, GuiComponent<?>>();
    
    // TODO this should be addressed at a higher level
    public static SimbrainDesktop getDesktop(Workspace workspace) {
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
        frame = new JFrame("Simbrain");

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setBounds(WORKSPACE_INSET, WORKSPACE_INSET, screenSize.width - (WORKSPACE_INSET * 2),
                screenSize.height - (WORKSPACE_INSET * 2));

        //Set up the GUI.
        desktop = new JDesktopPane(); //a specialized layered pane

        actionManager = new WorkspaceActionManager(this);
        
        createAndAttachMenus();

        wsToolBar = createToolBar();

        JPanel mainPanel = new JPanel(new BorderLayout());
        JScrollPane workspaceScroller = new JScrollPane();
        mainPanel.add("North", wsToolBar);
        mainPanel.add("Center", workspaceScroller);
        frame.setContentPane(mainPanel);
        workspaceScroller.setViewportView(desktop);

        frame.addWindowListener(windowListener);
        desktop.addMouseListener(mouseListener);
        frame.addKeyListener(new WorkspaceKeyAdapter(workspace));
        desktop.addKeyListener(new WorkspaceKeyAdapter(workspace));
        createContextMenu();

        workspace.addListener(listener);
        
        SimbrainDesktop.registerComponents();

        //Make dragging a little faster but perhaps uglier.
        //desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
    }

    /**
     * Creat mappings from components to their GUI wrappers
     */
    private static void registerComponents() {
        // TODO use a configuration file
        registerComponent(DataWorldComponent.class, DataWorldDesktopComponent.class);
        registerComponent(GameWorld2DComponent.class, GameWorld2DDesktopComponent.class);
        registerComponent(GaugeComponent.class, GaugeDesktopComponent.class);
        registerComponent(MidiWorldComponent.class, MidiWorldDesktopComponent.class);
        registerComponent(NetworkComponent.class, NetworkDesktopComponent.class);
        registerComponent(OdorWorldComponent.class, OdorWorldDesktopComponent.class);
        registerComponent(OscWorldComponent.class, OscWorldDesktopComponent.class);
        registerComponent(PlotComponent.class, PlotDesktopComponent.class);
        registerComponent(TextWorldComponent.class, TextWorldDesktopComponent.class);
        registerComponent(VisionWorldComponent.class, VisionWorldDesktopComponent.class);
        registerComponent(ThreeDeeComponent.class, MainConsole.class);
        registerComponent(ConsoleComponent.class, ConsoleDesktopComponent.class);
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

        /* Gauge menu button. */
        button = new JButton();
        button.setIcon(ResourceManager.getImageIcon("Gauge.png"));
        final JPopupMenu gaugeMenu = new JPopupMenu();
        for (Action action : actionManager.getGaugeActions()) {
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
        menuBar.add(createInsertMenu());
        menuBar.add(createCoupleMenu());
        menuBar.add(createHelpMenu());
        frame.setJMenuBar(menuBar);
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
        fileMenu.addSeparator();
        fileMenu.add(actionManager.getClearWorkspaceAction());
        fileMenu.addSeparator();
        fileMenu.add(actionManager.getOpenNetworkAction());
        fileMenu.add(actionManager.getOpenGaugeAction());

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
     * Create the workspace insert menu.
     *
     * @return insert menu
     */
    private JMenu createInsertMenu() {
        JMenu insertMenu = new JMenu("Insert");
        insertMenu.add(actionManager.getNewNetworkAction());
        JMenu newGaugeSubMenu = new JMenu("New Gauge");
        for (Action action : actionManager.getGaugeActions()) {
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
        JMenu newGaugeSubMenu = new JMenu("New Gauge");
        for (Action action : actionManager.getGaugeActions()) {
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
     * This nasty declaration creates a map of the workspace components to their associated
     * wrapper class.
     */
    private static final Map<Class<? extends WorkspaceComponent<?>>,Class<? extends GuiComponent<?>>> wrappers = new HashMap<Class<? extends WorkspaceComponent<?>>,
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
     * Creates an instance of the proper wrapper class around the provided instance.
     * 
     * @param component The component to wrap.
     * @param parentFrame The frame of this component
     * @return A new desktop component wrapping the provided component.
     */
    @SuppressWarnings("unchecked")
    static GuiComponent<?> createDesktopComponent(final GenericFrame parentFrame, final WorkspaceComponent<?> component) {
        Class<? extends WorkspaceComponent<?>> componentClass = (Class<? extends WorkspaceComponent<?>>) component.getClass();
        Class<? extends GuiComponent<?>> guiClass = wrappers.get(componentClass);
        
        if (guiClass == null) {
            throw new IllegalArgumentException(
                "no desktop component registered for " + component.getClass());
        }
        
        try {
            Constructor<? extends GuiComponent<?>> constructor
                = guiClass.getConstructor(GenericFrame.class, componentClass);
            return constructor.newInstance(parentFrame, component);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Return a workspace component.
     * @param component component to return
     * @return component.
     */
    public GuiComponent<?> getDesktopComponent(final WorkspaceComponent<?> component) {
        return components.get(component);
    }
    
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
            GuiComponent<?> component = components.get(workspaceComponent);            
            components.remove(component);
            component.getParentFrame().dispose();
        }
    };
    
    
    /**
     * Wraps GUI Component in a JInternalFrame for Desktop
     * 
     * @author jyoshimi
     *
     */
    private static class DesktopInternalFrame extends GenericJInternalFrame {
        
        WorkspaceComponent workspaceComponent;
        GuiComponent guiComponent;

        /**
         * 
         * @param workspaceComponent
         */
        public DesktopInternalFrame(WorkspaceComponent workspaceComponent) {
            setResizable(true);
            setMaximizable(true);
            setIconifiable(true);
            setClosable(true);
            addInternalFrameListener(new WindowFrameListener());
            this.workspaceComponent = workspaceComponent;
        }
        
        /**
         * 
         * @param guiComponent
         */
        public void setGuiComponent(GuiComponent guiComponent) {
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
    }

    /**
     * Add a new <c>SimbrainComponent</c>.
     *
     * @param desktopComponent
     */
    @SuppressWarnings("unchecked")
    public void addComponent(final WorkspaceComponent workspaceComponent, GuiComponent guiComponent) {

        LOGGER.trace("Adding workspace component: " + workspaceComponent);

        final DesktopInternalFrame componentFrame = new DesktopInternalFrame(workspaceComponent);
        
        // When deserializing it is provided
        if (guiComponent == null) {
            guiComponent = createDesktopComponent(componentFrame, workspaceComponent);
        }
        componentFrame.setGuiComponent(guiComponent);
        
        /* HANDLE COMPONENT BOUNDS */
        if (lastClickedPoint != null) {
            componentFrame.setBounds((int) lastClickedPoint.getX(),
                (int) lastClickedPoint.getY(),
                (int) guiComponent.getPreferredSize().getWidth(),
                (int) guiComponent.getPreferredSize().getHeight());
            guiChanged = true;
        } else if (components.size() == 0) {
            componentFrame.setBounds(DEFAULT_WINDOW_OFFSET, DEFAULT_WINDOW_OFFSET,
                (int) guiComponent.getPreferredSize().getWidth(),
                (int) guiComponent.getPreferredSize().getHeight());
            guiChanged = true;
        } else {            
            GuiComponent<?> lastComponentAdded = null;
            for (GuiComponent<?> next : components.values()) {
                lastComponentAdded = next;
            }
            int lastX = lastComponentAdded.getParentFrame().getX();
            int lastY = lastComponentAdded.getParentFrame().getY();
            componentFrame.setBounds(lastX + DEFAULT_WINDOW_OFFSET, lastY + DEFAULT_WINDOW_OFFSET,
                (int) guiComponent.getPreferredSize().getWidth(),
                (int) guiComponent.getPreferredSize().getHeight());
            guiChanged = true;
        }
        /* HANDLE COMPONENT NAMING */
        /*
         * Names take the form (ClassName - "Component") + index, where index
         * iterates as new components are added. e.g. Network 1, Network 2, etc.
         */
        if (componentNameIndices.get(guiComponent.getClass()) == null) {
            componentNameIndices.put(guiComponent.getClass(), 1);
        } else {
            int index = componentNameIndices.get(guiComponent.getClass());
            componentNameIndices.put(guiComponent.getClass(), index + 1);
        }
        guiComponent.getParentFrame().setTitle("" + guiComponent.getSimpleName()
            + componentNameIndices.get(guiComponent.getClass()));
        guiComponent.getParentFrame().setTitle("" + guiComponent.getSimpleName() + " "
            + componentNameIndices.get(guiComponent.getClass()));

        /* FINISH ADDING COMPONENT */
        guiComponent.addComponentListener(componentListener);
        lastClickedPoint = null;
        components.put(workspaceComponent, guiComponent);
        componentFrame.setContentPane(guiComponent);
        componentFrame.setVisible(true);
        desktop.add(componentFrame);
        guiComponent.postAddInit();
   
        // Forces last component of the desktop to the front
        
        try {
            ((JInternalFrame)componentFrame).setSelected(true);
        } catch (PropertyVetoException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Shows the dialog for opening a workspace file.
     */
    public void openWorkspace() {
        SFileChooser simulationChooser = new SFileChooser(workspace.getCurrentDirectory(), "zip");
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
    private void openWorkspace(File file) {
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
    public static void showJFrame(WorkspaceComponent component) {
        
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
                        WorkspaceSerializer serializer = new WorkspaceSerializer(this.getWorkspace());
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
        SFileChooser chooser = new SFileChooser(workspace.getCurrentDirectory(), "zip");
        if (workspace.getCurrentFile() != null) {
            chooser.setSelectedFile(workspace.getCurrentFile());
        } else {
            chooser.setSelectedFile(new File("workspace"));
        }
        File theFile = chooser.showSaveDialog();
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

        /* Display the window. */
        frame.setVisible(true);
        
        openWorkspace(workspace.getCurrentFile());

    }


    /**
     * Simbrain main method.  Creates a single instance of the Simulation class
     *
     * @param args currently not used
     */
    public static void main(final String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new SimbrainDesktop(new Workspace()).createAndShowGUI();
            }
        });
    }

    /**
     * Checks to see if anything has changed and then offers to save if true.
     */
    private void showHasChangedDialog() {
        Object[] options = {"Save", "Don't Save", "Cancel" };
        int s = JOptionPane.showOptionDialog(frame,
                 "The workspace has changed since last save,\nWould you like to save these changes?",
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
     */
    public void quit(boolean forceQuit) {
                
        if (changesExist() && (forceQuit == false)) {
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
            lastClickedPoint = mouseEvent.getPoint();
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
}
