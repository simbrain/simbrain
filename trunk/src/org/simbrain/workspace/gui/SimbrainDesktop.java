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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.apache.log4j.Logger;
import org.simbrain.console.ConsoleComponent;
import org.simbrain.console.ConsoleDesktopComponent;
import org.simbrain.gauge.GaugeComponent;
import org.simbrain.gauge.GaugeDesktopComponent;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.NetworkDesktopComponent;
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
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.world.odorworld.OdorWorldDesktopComponent;
import org.simbrain.world.textworld.TextWorldComponent;
import org.simbrain.world.textworld.TextWorldDesktopComponent;
import org.simbrain.world.threedee.ThreeDeeComponent;
import org.simbrain.world.threedee.gui.MainConsole;
import org.simbrain.world.visionworld.VisionWorldComponent;
import org.simbrain.world.visionworld.VisionWorldDesktopComponent;

/**
 * Creates a Swing-based environment for working with a workspace.
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

    private static final Map<Workspace, SimbrainDesktop> INSTANCES = new HashMap<Workspace, SimbrainDesktop>();
    
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
    
    /** Current workspace file. */
    private File currentFile = new File(WorkspacePreferences.getDefaultFile());

    /** Last clicked point. */
    private Point lastClickedPoint = null;
    
    /** The workspace this desktop wraps. */
    private final Workspace workspace;
    
    /** The serializer used to persist the workspace. */
    private final WorkspaceSerializer workspaceSerializer;
    
    /** Workspace action manager. */
    private WorkspaceActionManager actionManager;

    /**
     * Mapping from workspace component types to integers which show how many have been added.
     * For naming.
     */
    private Hashtable<Class<?>, Integer> componentNameIndices = new Hashtable<Class<?>, Integer>();

    /** All the components in the workspace. */
    private Map<WorkspaceComponent<?>, DesktopComponent<?>> components
        = new LinkedHashMap<WorkspaceComponent<?>, DesktopComponent<?>>();
    
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
        this.workspaceSerializer = new WorkspaceSerializer(workspace);
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
        
        // TODO use a configuration file
        registerComponent(DataWorldComponent.class, DataWorldDesktopComponent.class);
        registerComponent(GameWorld2DComponent.class, GameWorld2DDesktopComponent.class);
        registerComponent(GaugeComponent.class, GaugeDesktopComponent.class);
        registerComponent(NetworkComponent.class, NetworkDesktopComponent.class);
        registerComponent(OdorWorldComponent.class, OdorWorldDesktopComponent.class);
        registerComponent(PlotComponent.class, PlotDesktopComponent.class);
        registerComponent(TextWorldComponent.class, TextWorldDesktopComponent.class);
        registerComponent(VisionWorldComponent.class, VisionWorldDesktopComponent.class);
        registerComponent(ThreeDeeComponent.class, MainConsole.class);
        registerComponent(ConsoleComponent.class, ConsoleDesktopComponent.class);
        
        //Make dragging a little faster but perhaps uglier.
        //desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
    }

    /**
     * Returns the workspace.
     * 
     * @return the workspace.
     */
    public Workspace getWorkspace() {
        return workspace;
    }
    
//    public DesktopComponent getDesktopComponent(WorkspaceComponent component) {
//        return components.get(component);
//    }
    
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
        for (Action action : actionManager.getImportExportActions()) {
            fileMenu.add(action);
        }
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
    private final Map<Class<? extends WorkspaceComponent<?>>,
        Class<? extends DesktopComponent<?>>> wrappers
        = new HashMap<Class<? extends WorkspaceComponent<?>>,
        Class<? extends DesktopComponent<?>>>();
    
    /**
     * Registers a gui wrapper class association with a component class.
     * 
     * @param component The component class.
     * @param gui The gui class.
     */
    public void registerComponent(final Class<? extends WorkspaceComponent<?>> component,
            final Class<? extends DesktopComponent<?>> gui) {
        wrappers.put(component, gui);
    }
    
    /**
     * Creates an instance of the proper wrapper class around the provided instance.
     * 
     * @param component The component to wrap.
     * @return A new desktop component wrapping the provided component.
     */
    @SuppressWarnings("unchecked")
    private DesktopComponent<?> createDesktopComponent(final WorkspaceComponent<?> component) {        
        Class<? extends WorkspaceComponent<?>> componentClass
            = (Class<? extends WorkspaceComponent<?>>) component.getClass();
        Class<? extends DesktopComponent<?>> guiClass = wrappers.get(componentClass);
        
        if (guiClass == null) {
            throw new IllegalArgumentException(
                "no desktop component registered for " + component.getClass());
        }
        
        try {
            Constructor<? extends DesktopComponent<?>> constructor
                = guiClass.getConstructor(componentClass);
            return constructor.newInstance(component);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public DesktopComponent<?> getDesktopComponent(final WorkspaceComponent<?> component) {
        return components.get(component);
    }
    
    /** Listener on the workspace. */
    private final WorkspaceListener listener = new WorkspaceListener() {
        public boolean clearWorkspace() {
            if (changesExist()) {
                WorkspaceChangedDialog dialog = new WorkspaceChangedDialog(SimbrainDesktop.this);

                return !dialog.hasUserCancelled();
            }
            
            return true;
        }
        
        public void workspaceCleared() {
            if (changesExist()) {
                WorkspaceChangedDialog dialog = new WorkspaceChangedDialog(SimbrainDesktop.this);

                if (dialog.hasUserCancelled()) {
                    return;
                }
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
            LOGGER.trace("Adding workspace component: " + workspaceComponent);

            DesktopComponent<?> component = createDesktopComponent(workspaceComponent);

            /* set this as the parent desktop for the desktop component */
            component.setDesktop(SimbrainDesktop.this);

            /* HANDLE COMPONENT BOUNDS */

            if (lastClickedPoint != null) {
                component.setBounds((int) lastClickedPoint.getX(),
                    (int) lastClickedPoint.getY(),
                    (int) component.getPreferredSize().getWidth(),
                    (int) component.getPreferredSize().getHeight());
                guiChanged = true;
            } else if (components.size() == 0) {
                component.setBounds(DEFAULT_WINDOW_OFFSET, DEFAULT_WINDOW_OFFSET,
                    (int) component.getPreferredSize().getWidth(),
                    (int) component.getPreferredSize().getHeight());
                guiChanged = true;
            } else {
                DesktopComponent<?> dc = null;

                for (DesktopComponent<?> next : components.values()) {
                    dc = next;
                }

                int lastX = dc.getX();
                int lastY = dc.getY();
                component.setBounds(lastX + DEFAULT_WINDOW_OFFSET, lastY + DEFAULT_WINDOW_OFFSET,
                    (int) component.getPreferredSize().getWidth(),
                    (int) component.getPreferredSize().getHeight());
                guiChanged = true;
            }

            /* HANDLE COMPONENT NAMING */

            /*
             * Names take the form (ClassName - "Component") + index, where index
             * iterates as new components are added. e.g. Network 1, Network 2, etc.
             */
            if (componentNameIndices.get(component.getClass()) == null) {
                componentNameIndices.put(component.getClass(), 1);
            } else {
                int index = componentNameIndices.get(component.getClass());
                componentNameIndices.put(component.getClass(), index + 1);
            }

            component.setName("" + component.getSimpleName()
                + componentNameIndices.get(component.getClass()));
            component.setTitle("" + component.getSimpleName() + " "
                + componentNameIndices.get(component.getClass()));

            /* FINISH ADDING COMPONENT */

            components.put(workspaceComponent, component);
            desktop.add(component);
            component.setVisible(true);

            try {
                component.setSelected(true);
            } catch (java.beans.PropertyVetoException e) {
                System.out.print(e.getStackTrace());
            }

            component.addComponentListener(componentListener);
            lastClickedPoint = null;
            component.postAddInit();
        }

        @SuppressWarnings("unchecked")
        public void componentRemoved(final WorkspaceComponent workspaceComponent) {
            DesktopComponent<?> component = components.get(workspaceComponent);
            
            component.dispose();
            components.remove(component);
        }
    };
    
    /**
     * Add a new <c>SimbrainComponent</c>.
     *
     * @param component
     */
    @SuppressWarnings("unchecked")
    public void addComponent(final WorkspaceComponent workspaceComponent,
            final DesktopComponent component) {
        /* set this as the parent desktop for the desktop component */
        component.setDesktop(SimbrainDesktop.this);

        /* HANDLE COMPONENT NAMING */
        
        // TODO component name comes from component

        /* FINISH ADDING COMPONENT */

        components.put(workspaceComponent, component);
        desktop.add(component);
        component.setVisible(true);

        component.addComponentListener(componentListener);
//        lastClickedPoint = null;
//        component.postAddInit();
    }
    
    /**
     * Shows the dialog for opening a workspace file.
     */
    public void openWorkspace() {

//        if (changesExist()) {
//            WorkspaceChangedDialog theDialog = new WorkspaceChangedDialog(this);
//
//            if (theDialog.hasUserCancelled()) {
//                return;
//            }
//        }
//        // TODO ?
////        workspaceChanged = false;
//
//        String currentDirectory = WorkspacePreferences.getCurrentDirectory();
//        
//        SFileChooser simulationChooser = new SFileChooser(currentDirectory, "sim");
//        File simFile = simulationChooser.showOpenDialog();
//
//        if (simFile != null) {
//            workspaceSerializer.readWorkspace(simFile, false);
//            currentFile = simFile;
//            currentDirectory = simulationChooser.getCurrentLocation();
//            WorkspacePreferences.setCurrentDirectory(currentDirectory);
//            WorkspacePreferences.setDefaultFile(currentFile.toString());
//        }

        
        WorkspaceSerializer serializer = new WorkspaceSerializer(workspace);
        
        try {
            serializer.deserialize(new FileInputStream("workspace.zip"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Shows the dialog for saving a workspace file.
     */
    public void saveWorkspace() {
        
        String currentDirectory = WorkspacePreferences.getCurrentDirectory();
        SFileChooser simulationChooser = new SFileChooser(currentDirectory, "sim");
        // TODO ?
//        workspaceChanged = false;

        if (changesExist()) {
            WorkspaceChangedDialog theDialog = new WorkspaceChangedDialog(this);

            if (theDialog.hasUserCancelled()) {
                return;
            }
        }

        File simFile = simulationChooser.showSaveDialog();

        if (simFile != null) {
            workspaceSerializer.writeWorkspace(simFile);
            currentFile = simFile;
            currentDirectory = simulationChooser.getCurrentLocation();
            WorkspacePreferences.setCurrentDirectory(currentDirectory);
            WorkspacePreferences.setDefaultFile(simFile.toString());
        }
    }

    /**
     * Show the save dialog.
     */
    public void save() {
        if (changesExist()) {
            WorkspaceChangedDialog theDialog = new WorkspaceChangedDialog(this);

            if (theDialog.hasUserCancelled()) {
                return;
            }
        }

        if (currentFile != null) {
            workspaceSerializer.writeWorkspace(currentFile);
            frame.setTitle(currentFile.getName());
        }
    }

    /**
     * Create the GUI and show it. For thread safety, this method
     * should be invoked from the event-dispatching thread.
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

        //workspaceSerializer.readWorkspace(new File(WorkspacePreferences.getDefaultFile()), false);
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
     * Open a specific workspace component (network, world, etc).
     *
     * @param type the type of the component to open.
     */
//    public void openWorkspaceComponent(final Class<?> type) {
//        try {
//            String currentDirectory = WorkspacePreferences.getCurrentDirectory();
//            WorkspaceComponent component = (WorkspaceComponent) type.newInstance();
//            SFileChooser chooser = new SFileChooser(component.getCurrentDirectory(),
//                component.getFileExtension());
//            File theFile = chooser.showOpenDialog();
//
//            if (theFile != null) {
//                workspace.addWorkspaceComponent(component);
//                component.open(theFile);
//                component.setCurrentDirectory(chooser.getCurrentLocation());
//                WorkspacePreferences.setCurrentDirectory(currentDirectory.toString());
//            }
//        } catch (InstantiationException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        }
//    }
    
    /**
     * Returns all windows which have changed.
     *
     * @return all windows which have changed.
     */
//    public ArrayList<WorkspaceComponent> getChangedWindows() {
//        ArrayList<WorkspaceComponent> ret = new ArrayList<WorkspaceComponent>();
//        for (WorkspaceComponent window : workspace.getComponentList()) {
//            if (window.isChangedSinceLastSave()) {
//                ret.add(window);
//            }
//        }
//        return ret;
//    }

    /**
     * Quit application.
     */
    public void quit() {
        //ensures that frameClosing events are called
        workspace.removeAllComponents();

        System.exit(0);
    }

    /**
     * Creates a producer oriented menu for coupling to the given target.
     * 
     * @param target the consuming attribute.
     * @return A new menu instance.
     */
    public JMenu getProducerMenu(final ConsumingAttribute<?> target) {
        System.out.println("getProducerMenu");
        JMenu producerMenu = new JMenu("Producers");
        for (WorkspaceComponent<?> component : workspace.getComponentList()) {

            Collection<? extends Producer> producers = component.getProducers();

            if (producers.size() > 0) {
                JMenu componentMenu = new JMenu(component.getName());
                for (Producer producer : component.getProducers()) {
                    if (producer instanceof SingleAttributeProducer) {
                        SingleCouplingMenuItem item = 
                                new SingleCouplingMenuItem(workspace, producer.getDescription(), 
                        producer.getDefaultProducingAttribute(), target);
                        componentMenu.add(item);
                    } else {
                        JMenu producerItem = new JMenu(producer.getDescription());
                        for (ProducingAttribute<?> source : producer.getProducingAttributes()) {
                            SingleCouplingMenuItem item = 
                                new SingleCouplingMenuItem(workspace,source.getAttributeDescription(), source, target);
                            producerItem.add(item);
                            componentMenu.add(producerItem);
                        }
                    }
                }
                producerMenu.add(componentMenu);
            }
        }
        return producerMenu;
    }
    
    /**
     * Creates a consumer oriented menu for coupling to the given source.
     * 
     * @param source the producing attribute.
     * @return A new menu instance.
     */
    public JMenu getConsumerMenu(final ProducingAttribute<?> source) {
        JMenu consumerMenu = new JMenu("Consumers");
        for (WorkspaceComponent<?> component : workspace.getComponentList()) {
            Collection<? extends Consumer> consumers = component.getConsumers();
            
            if (consumers.size() > 0) {
                JMenu componentMenu = new JMenu(component.getName());
                for (Consumer consumer : component.getConsumers()) {
                        if (consumer instanceof SingleAttributeConsumer) {
                            SingleCouplingMenuItem item = new SingleCouplingMenuItem(workspace, consumer.getDescription(), 
                                    source, consumer.getDefaultConsumingAttribute());
                            componentMenu.add(item);
                        } else {
                            JMenu consumerItem = new JMenu(consumer.getDescription());
                            for (ConsumingAttribute<?> target : consumer.getConsumingAttributes()) {
                                SingleCouplingMenuItem item = new SingleCouplingMenuItem(workspace,
                                        target.getAttributeDescription(), source, target);
                                consumerItem.add(item);
                            }
                            componentMenu.add(consumerItem);
                    }
                }
                consumerMenu.add(componentMenu);
            }
        }
        return consumerMenu;
    }
    
    /**
     * Get a menu representing all components which have lists of producers,
     * which returns such a list.
     *
     * @param listener the component which will listens to the menu items in this menu
     * @return the menu containing all available components with nonempty producer lists
     */
    public JMenu getProducerListMenu(final ActionListener listener) {
        JMenu producerListMenu = new JMenu("Producer lists");
        for (WorkspaceComponent<?> component : workspace.getComponentList()) {
                CouplingMenuItem producerListItem = new CouplingMenuItem(component,
                    CouplingMenuItem.EventType.PRODUCER_LIST);
                producerListItem.setText(component.getName());
                producerListItem.addActionListener(listener);
                producerListMenu.add(producerListItem);
        }
        return producerListMenu;
    }

    /**
     * Get a menu representing all components which have lists of consumers,
     * which returns such a list.
     *
     * @param listener the component which will listens to the menu items in this menu
     * @return the menu containing all available components with nonempty consumer lists
     */
    public JMenu getConsumerListMenu(final ActionListener listener) {
        JMenu consumerListMenu = new JMenu("Consumer lists");
        for (WorkspaceComponent<?> component : workspace.getComponentList()) {
                CouplingMenuItem consumerListItem = new CouplingMenuItem(component,
                    CouplingMenuItem.EventType.CONSUMER_LIST);
                consumerListItem.setText(component.getName());
                consumerListItem.addActionListener(listener);
                consumerListMenu.add(consumerListItem);
        }
        return consumerListMenu;
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
            if (changesExist()) {
                WorkspaceChangedDialog dialog = new WorkspaceChangedDialog(SimbrainDesktop.this);

                if (dialog.hasUserCancelled()) {
                    return;
                }
            }

            quit();
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
