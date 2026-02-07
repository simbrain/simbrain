package org.simbrain.workspace.gui

import bsh.Interpreter
import bsh.util.JConsole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import org.pmw.tinylog.Logger
import org.simbrain.console.ConsoleDesktopComponent
import org.simbrain.custom_sims.NewSimulation
import org.simbrain.custom_sims.Simulation
import org.simbrain.custom_sims.simulations
import org.simbrain.docviewer.DocViewerViewPanel
import org.simbrain.util.*
import org.simbrain.util.genericframe.GenericFrame
import org.simbrain.util.genericframe.GenericJFrame
import org.simbrain.util.genericframe.GenericJInternalFrame
import org.simbrain.util.widgets.ProgressWindow
import org.simbrain.util.widgets.ShowHelpAction
import org.simbrain.util.widgets.ToggleButton
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.workspace.WorkspacePreferences
import org.simbrain.workspace.updater.PerformanceMonitor
import java.awt.*
import java.awt.event.*
import java.beans.PropertyVetoException
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import javax.swing.*
import javax.swing.JSplitPane.DIVIDER_LOCATION_PROPERTY
import javax.swing.event.*

/**
 * Creates a singleton Swing-based environment for working with a workspace.
 *
 * Also provides wrappers for GUI elements called from a terminal.
 *
 * @author Matt Watson
 * @author Jeff Yoshimi
 */
object SimbrainDesktop {

    val workspace = Workspace()

    val desktopPane: JDesktopPane = JDesktopPane()

    /**
     * Name to display in Simbrain desktop window.
     */
    private val FRAME_TITLE = BuildInfo.applicationTitle

    /**
     * The frame that will hold the workspace.
     */
    val frame: JFrame = JFrame(FRAME_TITLE)

    /**
     * Manager for onboarding popups
     */
    val onboardingManager = OnboardingPopupManager(frame)

    /**
     * Associates workspace components with their corresponding desktop components.
     */
    private val workspaceComponentDesktopComponentMap = CompletableDeferredHashMap<WorkspaceComponent, DesktopComponent<*>>()

    /**
     * Reference to the last internal frames that were focused, so that they can get the focus when the next one is
     * closed.
     */
    private val lastFocusedStack = Stack<DesktopComponent<*>>()

    /**
     * The x offset for popup menus.
     */
    private const val MENU_X_OFFSET = 5

    /**
     * The y offset for popup menus.
     */
    private const val MENU_Y_OFFSET = 53

    /**
     * The default serial version ID.
     */
    private const val serialVersionUID = 1L

    /**
     * Initial indent of entire workspace.
     */
    private const val WORKSPACE_INSET = 80

    /**
     * After placing one simbrain window how far away to put the next one.
     */
    private const val DEFAULT_WINDOW_OFFSET = 30

    /**
     * Cached context menu.
     */
    private var contextMenu: JPopupMenu? = null

    var wsToolBar = JToolBar()

    lateinit var infoDockButton: AbstractButton

    val screenSize = Toolkit.getDefaultToolkit().screenSize

    val sideDockSplitter = SimbrainDesktopDock(
        mainComponent = desktopPane,
        dockComponent = DocViewerViewPanel().apply {
            workspace.infoDoc.events.renderedTextChanged.on(Dispatchers.Swing) {
                text = it
                renderedTextPanel.caretPosition = 0
            }
        },
        orientation = JSplitPane.HORIZONTAL_SPLIT,
        defaultSize = (screenSize.width * .2).toInt()
    ).apply {
        // Add listener for when the info dock becomes visible to show onboarding popup
        dockComponent.addComponentListener(object : ComponentAdapter() {
            override fun componentShown(e: ComponentEvent?) {
                swingInvokeLater {
                    onboardingManager.showPopup(
                        PopupConfig(
                            title = "Info Panel Toggle",
                            message = "Use this button to toggle the visibility of this info screen. The info panel shows documentation and help content for various features.",
                            targetComponent = infoDockButton,
                            placement = PopupPlacement.BOTTOM_CENTER,
                            suppressionKey = "info_dock_help",
                            style = PopupStyle.INFO
                        )
                    )

                }

            }
        })
    }

    val bottomDockSplitter = SimbrainDesktopDock(
        mainComponent = sideDockSplitter,
        dockComponent = JTabbedPane().apply {
            addTab("Components", null, ComponentPanel(this@SimbrainDesktop), "Show workspace components")
            addTab("Terminal", null, terminalPanel, "Simbrain terminal")
            addTab("Performance", null, PerformanceMonitorPanel(this@SimbrainDesktop.workspace), "Performance monitoring")
            addChangeListener {
                PerformanceMonitor.enabled = selectedIndex == 2
            }
        },
        orientation = JSplitPane.VERTICAL_SPLIT,
        defaultSize = WorkspacePreferences.bottomDockSize
    ).apply {
        addPropertyChangeListener(DIVIDER_LOCATION_PROPERTY) {
            if (dockComponent.isVisible) {
                WorkspacePreferences.bottomDockSize = dividerLocation
            }
        }
        // Show dock based on preference
        if (WorkspacePreferences.showBottomDockByDefault) {
            showDock()
        }
    }

    private val workspaceBounds: Rectangle

    val actionManager = WorkspaceActions()

    /**
     * Interpreter for terminal.
     */
    private var interpreter: Interpreter? = null

    /**
     * Time indicator.
     */
    private val timeLabel = JLabel()

    /**
     * "Throbber" to indicate a simulation is running.
     */
    private val runningLabel = JLabel()

    /**
     * Update rate for display.
     */
    private var updateRate = 0

    /**
     * Timer to calculate update rate.
     */
    private var lastUpdateTimeMs: Long = 0

    /**
     * Timestep at the last update rate calculation.
     */
    private var lastTimestep = 0

    private val mouseListener: MouseListener = object : MouseAdapter() {
        override fun mousePressed(mouseEvent: MouseEvent) {
            val lastClickedPoint = mouseEvent.point
            if (mouseEvent.isControlDown || mouseEvent.button == MouseEvent.BUTTON3) {
                contextMenu!!.show(
                    frame,
                    lastClickedPoint.getX().toInt() + MENU_X_OFFSET,
                    lastClickedPoint.getY().toInt() + MENU_Y_OFFSET
                )
            }
        }
    }

    private val windowListener: WindowListener = object : WindowAdapter() {
        override fun windowClosing(arg0: WindowEvent) {
            quit(false)
        }
    }

    /**
     * Listens to menu events for setting save enabled.
     */
    private val menuListener: MenuListener = object : MenuListener {
        override fun menuSelected(arg0: MenuEvent) {
            actionManager.saveWorkspaceAction.isEnabled = workspace.changesExist()
        }

        override fun menuDeselected(arg0: MenuEvent) {
        }

        override fun menuCanceled(arg0: MenuEvent) {
        }
    }

    init {
        frame.iconImages = listOf(
            ResourceManager.getImage("simbrain_iconset/icon_20x20.png"),
            ResourceManager.getImage("simbrain_iconset/icon_32x32.png"),
            ResourceManager.getImage("simbrain_iconset/icon_40x40.png"),
            ResourceManager.getImage("simbrain_iconset/icon_64x64.png"),
            ResourceManager.getImage("simbrain_iconset/icon_128x128.png"),
            ResourceManager.getImage("simbrain_iconset/icon_512x512.png")
        )
        createAndAttachMenus()
        
        // Add macOS About menu handler
        if (Utils.isMacOSX()) {
            Desktop.getDesktop().setAboutHandler {
                showAboutDialog()
            }
        }
        
        wsToolBar = createToolBar()
        createContextMenu()
        val events = workspace.events
        events.workspaceCleared.on {
            workspaceComponentDesktopComponentMap.clear()
            desktopPane.removeAll()
            desktopPane.repaint()
            frame.title = FRAME_TITLE
            lastTimestep = 0
            updateTimeLabel()
        }
        events.componentAdded.on(Dispatchers.Swing, wait = true) { addDesktopComponent(it) }
        events.componentRemoved.on(Dispatchers.Swing) { wc  ->
            val component = workspaceComponentDesktopComponentMap.getImmediately(wc) as? DesktopComponent<*> ?: return@on
            workspaceComponentDesktopComponentMap.remove(wc)
            component.parentFrame.dispose()
            if (!lastFocusedStack.isEmpty()) {
                lastFocusedStack.remove(component)
            }
            moveLastFocusedComponentToFront()
        }
        events.workspaceOpened.on(Dispatchers.Swing) {
            frame.title = workspace.currentFile!!.name
            lastTimestep = 0
            updateTimeLabel()
        }
        workspace.updater.events.workspaceUpdated.on { updateTimeLabel() }
        workspace.updater.events.runStarted.on { StandardDialog.setSimulationRunning(true) }
        workspace.updater.events.runFinished.on { StandardDialog.setSimulationRunning(false) }
        workspaceBounds = Rectangle(
            WORKSPACE_INSET,
            WORKSPACE_INSET,
            screenSize.width - WORKSPACE_INSET * 2,
            screenSize.height - WORKSPACE_INSET * 2
        )

        // Set up Desktop
        if (System.getProperty("os.name").lowercase(Locale.getDefault()).contains("windows")) {
            desktopPane.background = Color.WHITE
            desktopPane.border = BorderFactory.createLoweredBevelBorder()
        }
        desktopPane.addMouseListener(mouseListener)
        desktopPane.preferredSize =
            Dimension(screenSize.width - WORKSPACE_INSET * 2, screenSize.height - WORKSPACE_INSET * 3)

        // Main panel
        val mainPanel = JPanel(BorderLayout()).apply {
            add(wsToolBar, BorderLayout.NORTH)
            add(bottomDockSplitter, BorderLayout.CENTER)
        }
        frame.contentPane = mainPanel

        // Configure frame
        frame.bounds = workspaceBounds
        frame.pack()
        frame.addWindowListener(windowListener)
        frame.isVisible = true

        // Set initial desktopPane bounds
        desktopPane.bounds = Rectangle(200, 0, frame.width - 200, frame.height)
        frame.isVisible = true

        // Set up Frame
        frame.bounds = workspaceBounds
        frame.contentPane = mainPanel
        frame.pack()
        frame.addWindowListener(windowListener)

        // Set the "dock" image.
        if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
            Taskbar.getTaskbar().iconImage = ResourceManager.getImage("simbrain_iconset/icon_128x128.png")
        }

        // Start terminal
        Thread(interpreter).start()

        // Make dragging a little faster but perhaps uglier.
        // desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
    }

    /**
     * Listener for swing component changes.
     */
    private val componentListener: ComponentListener = object : ComponentAdapter() {

        override fun componentMoved(event: ComponentEvent) {

            // Prevent window from being moved outside of visible area
            val x = event.component.bounds.getX().toInt()
            val y = event.component.bounds.getY().toInt()
            val width = event.component.bounds.getWidth().toInt()
            val height = event.component.bounds.getHeight().toInt()
            if (x < desktopPane.visibleRect.getX()) {
                event.component.setBounds(0, y, width, height)
            }
            if (y < desktopPane.visibleRect.getY()) {
                event.component.setBounds(x, 0, width, height)
            }

            // Workspace has changed
            workspace.setWorkspaceChanged(true)
        }

        override fun componentResized(arg0: ComponentEvent) {
            // System.out.println("Component resized");
            workspace.setWorkspaceChanged(true)
        }
    }

    /**
     * Takes the last gui component opened and moves it to the front of the simbrain desktop, place it in focus.
     */
    private fun moveLastFocusedComponentToFront() {
        if (!lastFocusedStack.isEmpty()) {
            val lastFocused = lastFocusedStack.peek()
            if (lastFocused != null) {
                try {
                    (lastFocused.parentFrame as JInternalFrame).isSelected = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private val terminalPanel: JConsole
        get() {
            val console = JConsole()
            interpreter = ConsoleDesktopComponent.getSimbrainInterpreter(console, workspace).also {
                try {
                    it.set("desktop", this)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            console.preferredSize = Dimension(400, 300)
            return console
        }

    /**
     * Print text to Simbrain terminal.
     */
    fun printToTerminal(toPrint: String?) {
        interpreter!!.println(toPrint)
    }

    private fun createToolBar(): JToolBar {
        val bar = JToolBar()
        bar.add(actionManager.openWorkspaceAction)
        bar.add(actionManager.saveWorkspaceAction)
        bar.addSeparator()
        bar.add(actionManager.iterateAction)
        bar.add(ToggleButton(actionManager.runControlActions).apply {
            setAction("Run")
            workspace.updater.events.runStarted.on {
                setAction("Stop")
            }
            workspace.updater.events.runFinished.on {
                setAction("Run")
            }
        })
        bar.addSeparator()
        bar.add(actionManager.openCouplingManagerAction)
        bar.addSeparator()
        bar.add(actionManager.newNetworkAction)

        bar.add(createDropdownButton(
            ResourceManager.getSmallIcon("menu_icons/World.png"),
            actionManager.newWorldActions,
            "Create a new world"
        ))

        bar.add(createDropdownButton(
            ResourceManager.getSmallIcon("menu_icons/BarChart.png"),
            actionManager.plotActions,
            "Create a new plot"
        ))

        bar.add(actionManager.newConsoleAction)

        // Toggle docks
        bar.addSeparator()
        bar.add(actionManager.toggleBottomDock)
        infoDockButton = bar.add(actionManager.toggleInfoDock)

        // Initialize time label
        timeLabel.border = BorderFactory.createEmptyBorder(0, 10, 0, 10)
        timeLabel.addMouseListener(object : MouseAdapter() {
            // Reset time if user double clicks on label.
            override fun mousePressed(event: MouseEvent) {
                if (event.clickCount == 2) {
                    workspace.updater.resetTime()
                    lastTimestep = 0
                    updateTimeLabel()
                }
            }
        })
        runningLabel.icon = ResourceManager.getSmallIcon("menu_icons/Throbber.gif")
        runningLabel.isVisible = false
        updateTimeLabel()
        bar.add(timeLabel)
        bar.add(runningLabel)
        return bar
    }

    /**
     * Create and attach workspace menus.
     */
    private fun createAndAttachMenus() {
        val menuBar = JMenuBar()
        menuBar.add(createFileMenu())
        menuBar.add(createViewMenu())
        menuBar.add(createInsertMenu())
        menuBar.add(createScriptMenu())
        menuBar.add(createCoupleMenu())
        menuBar.add(createHelpMenu())
        frame.jMenuBar = menuBar
    }

    private fun createScriptMenu(): JMenu {
        val scriptMenu = JMenu("Simulations")
        simulations.addToMenu(scriptMenu) { newSimulation: Any? ->
            if (newSimulation is NewSimulation) {
                workspace.launch {
                    newSimulation.run(this@SimbrainDesktop)
                }
            } else if (newSimulation is Simulation) {
                workspace.launch {
                    newSimulation.instantiate(this@SimbrainDesktop).run()
                }
            }
        }
        return scriptMenu
    }

    private fun createFileMenu(): JMenu {
        val fileMenu = JMenu("File")
        fileMenu.addMenuListener(menuListener)
        for (action in actionManager.openSaveWorkspaceActions) {
            fileMenu.add(action)
        }
        fileMenu.addSeparator()
        fileMenu.add(actionManager.clearWorkspaceAction)
        fileMenu.addSeparator()
        fileMenu.add(actionManager.showUpdaterDialog)
        fileMenu.addSeparator()
        fileMenu.add(actionManager.showWorkspacePreferencesAction)
        fileMenu.add(actionManager.showNetworkPreferencesAction)
        fileMenu.add(actionManager.showOdorWorldPreferencesAction)
        fileMenu.addSeparator()
        fileMenu.add(actionManager.resetOnboardingWindows)
        fileMenu.addSeparator()
        fileMenu.add(actionManager.quitWorkspaceAction)
        return fileMenu
    }

    private fun createViewMenu(): JMenu {
        val viewMenu = JMenu("View")
        viewMenu.add(JMenuItem(actionManager.toggleBottomDock))
        viewMenu.add(JMenuItem(actionManager.toggleInfoDock))
        viewMenu.addSeparator()
        viewMenu.add(JMenuItem(actionManager.resizeAllWindowsAction))
        viewMenu.add(JMenuItem(actionManager.repositionAllWindowsAction))
        return viewMenu
    }

    private fun createInsertMenu(): JMenu {
        val insertMenu = JMenu("Insert")
        insertMenu.add(actionManager.newNetworkAction)
        // insertMenu.add(new OpenEditorAction(this)); //TODO: Move this action
        // manager
        val newGaugeSubMenu = JMenu("New plot")
        for (action in actionManager.plotActions) {
            newGaugeSubMenu.add(action)
        }
        insertMenu.add(newGaugeSubMenu)
        val newWorldSubMenu = JMenu("New world")
        for (action in actionManager.newWorldActions) {
            newWorldSubMenu.add(action)
        }
        insertMenu.add(newWorldSubMenu)
        insertMenu.addSeparator()
        insertMenu.add(actionManager.newDocViewerAction)
        insertMenu.add(actionManager.newConsoleAction)
        return insertMenu
    }

    private fun createCoupleMenu(): JMenu {
        val coupleMenu = JMenu("Couplings")
        coupleMenu.add(actionManager.openCouplingManagerAction)
        coupleMenu.add(actionManager.openCouplingListAction)
        return coupleMenu
    }

    private fun createHelpMenu(): JMenu {
        val helpMenu = JMenu("Help")
        helpMenu.add(ShowHelpAction("Documentation", "https://docs.simbrain.net/"))
        helpMenu.add(ShowHelpAction("Quick start", "https://docs.simbrain.net/docs/quickstart.html"))
        helpMenu.add(ShowHelpAction("Keyboard shortcuts", "https://docs.simbrain.net/docs/shortcuts.html"))
        helpMenu.addSeparator()
        helpMenu.add(actionManager.toggleInfoDock)
        helpMenu.addSeparator()
        helpMenu.add(ShowHelpAction("Credits", "https://simbrain.net/SimbrainCredits.html"))
        
        // Add About menu item for non-macOS platforms (macOS has native About menu)
        if (!Utils.isMacOSX()) {
            helpMenu.addSeparator()
            val aboutAction = object : AbstractAction("About Simbrain") {
                override fun actionPerformed(e: ActionEvent) {
                    showAboutDialog()
                }
            }
            helpMenu.add(aboutAction)
        }
        
        return helpMenu
    }
    
    /**
     * Show the About dialog with version and build information
     */
    private fun showAboutDialog() {
        val aboutDialog = JDialog(frame, "About Simbrain", true)
        aboutDialog.layout = BorderLayout()
        
        // Logo at the top
        val logoPanel = JPanel(FlowLayout(FlowLayout.CENTER))
        val logoLabel = JLabel()
        val logoImage = ResourceManager.getImage("simbrain_iconset/icon_128x128.png")
        logoLabel.icon = ImageIcon(logoImage)
        logoPanel.add(logoLabel)
        
        // Middle section with info
        val infoPanel = JPanel()
        infoPanel.layout = BoxLayout(infoPanel, BoxLayout.Y_AXIS)
        infoPanel.border = BorderFactory.createEmptyBorder(5, 15, 5, 15)

        val titleLabel = JLabel("Simbrain ${BuildInfo.versionName}")
        titleLabel.font = Font("SansSerif", Font.BOLD, 18)
        titleLabel.alignmentX = Component.CENTER_ALIGNMENT

        val versionLabel = JLabel(BuildInfo.fullVersionString)
        versionLabel.font = Font("SansSerif", Font.PLAIN, 14)
        versionLabel.alignmentX = Component.CENTER_ALIGNMENT
        
        // Add build info if available
        val buildInfoLabel = if (BuildInfo.buildNumber != "dev" && BuildInfo.commitSha != "unknown") {
            JLabel("Commit: ${BuildInfo.commitSha}").apply {
                font = Font("SansSerif", Font.PLAIN, 12)
                alignmentX = Component.CENTER_ALIGNMENT
                foreground = Color.GRAY
            }
        } else null

        val descriptionLabel = JLabel("A framework for neural network simulation")
        descriptionLabel.alignmentX = Component.CENTER_ALIGNMENT
        
        infoPanel.add(Box.createVerticalStrut(10))
        infoPanel.add(titleLabel)
        infoPanel.add(Box.createVerticalStrut(5))
        infoPanel.add(versionLabel)
        buildInfoLabel?.let {
            infoPanel.add(Box.createVerticalStrut(3))
            infoPanel.add(it)
        }
        infoPanel.add(Box.createVerticalStrut(10))
        infoPanel.add(descriptionLabel)
        infoPanel.add(Box.createVerticalStrut(10))

        // Links
        val linkPanel = JPanel()
        linkPanel.layout = BoxLayout(linkPanel, BoxLayout.Y_AXIS)
        linkPanel.border = BorderFactory.createEmptyBorder(0, 0, 10, 0)
        
        val websiteButton = JButton("Visit Simbrain Website")
        websiteButton.addActionListener { 
            Utils.displayURLInBrowser("https://simbrain.net")
        }
        websiteButton.alignmentX = Component.CENTER_ALIGNMENT
        
        val creditsButton = JButton("View Credits")
        creditsButton.addActionListener { 
            Utils.displayURLInBrowser("https://simbrain.net/SimbrainCredits.html")
        }
        creditsButton.alignmentX = Component.CENTER_ALIGNMENT
        
        linkPanel.add(websiteButton)
        linkPanel.add(Box.createVerticalStrut(5))
        linkPanel.add(creditsButton)

        // Add all components to the dialog
        val centerPanel = JPanel(BorderLayout())
        centerPanel.add(logoPanel, BorderLayout.NORTH)
        centerPanel.add(infoPanel, BorderLayout.CENTER)
        
        aboutDialog.add(centerPanel, BorderLayout.CENTER)
        aboutDialog.add(linkPanel, BorderLayout.SOUTH)

        // Configure dialog
        aboutDialog.size = Dimension(375, 380)
        aboutDialog.isResizable = false
        aboutDialog.setLocationRelativeTo(frame)
        aboutDialog.isVisible = true
    }

    private fun createContextMenu() {
        contextMenu = JPopupMenu()
        contextMenu!!.add(actionManager.newNetworkAction)
        val newGaugeSubMenu = JMenu("New plot")
        for (action in actionManager.plotActions) {
            newGaugeSubMenu.add(action)
        }
        contextMenu!!.add(newGaugeSubMenu)
        val newWorldSubMenu = JMenu("New world")
        for (action in actionManager.newWorldActions) {
            newWorldSubMenu.add(action)
        }
        contextMenu!!.add(newWorldSubMenu)
        contextMenu!!.addSeparator()
        contextMenu!!.add(actionManager.newDocViewerAction)
        contextMenu!!.add(actionManager.newConsoleAction)
    }


    /**
     * Returns the desktop component corresponding to a workspace component.
     */
    fun getDesktopComponentBlocking(component: WorkspaceComponent): DesktopComponent<*> {
        return runBlocking {
            workspaceComponentDesktopComponentMap.getImmediately(component)
                ?: throw IllegalStateException("Cannot find component ${component.name} in ${workspaceComponentDesktopComponentMap.keys.map { it.name }}")
        }
    }

    suspend fun getDesktopComponent(component: WorkspaceComponent): DesktopComponent<*> {
        return workspaceComponentDesktopComponentMap.get(component)
    }

    /**
     * Utility class for adding internal frames, which are not wrappers for WorkspaceComponents. Wraps GUI Component in
     * a JInternalFrame for Desktop.
     */
    private class DesktopInternalFrame(workspaceComponent: WorkspaceComponent) : GenericJInternalFrame() {
        /**
         * Reference to workspace component.
         */
        private val workspaceComponent: WorkspaceComponent

        /**
         * Gui Component.
         */
        private var desktopComponent: DesktopComponent<*>? = null

        /**
         * Construct an internal frame.
         *
         * @param workspaceComponent workspace component.
         */
        init {
            isResizable = true
            isMaximizable = true
            isIconifiable = true
            isClosable = true
            defaultCloseOperation = DO_NOTHING_ON_CLOSE
            addInternalFrameListener(WindowFrameListener())
            this.workspaceComponent = workspaceComponent
        }

        fun setGuiComponent(desktopComponent: DesktopComponent<*>?) {
            this.desktopComponent = desktopComponent
        }

        /**
         * Manage cleanup when a component is closed.
         */
        private inner class WindowFrameListener : InternalFrameAdapter() {
            override fun internalFrameActivated(e: InternalFrameEvent) {
                // TODO: Does not work properly. Should be used so that
                // the last focused stack tracks changes in focus and not just
                // open / close events.
                // lastFocusedStack.remove(guiComponent);
                // lastFocusedStack.push(guiComponent);
            }

            override fun internalFrameOpened(e: InternalFrameEvent) {
                super.internalFrameOpened(e)
            }

            override fun internalFrameClosing(e: InternalFrameEvent) {
                desktopComponent!!.close()
            }

            override fun internalFrameClosed(e: InternalFrameEvent) {
                super.internalFrameClosed(e)
            }
        }
    }

    fun addInternalFrame(internalFrame: JInternalFrame) {
        internalFrame.addInternalFrameListener(object : InternalFrameListener {
            override fun internalFrameActivated(arg0: InternalFrameEvent) {}
            override fun internalFrameClosed(arg0: InternalFrameEvent) {}
            override fun internalFrameClosing(arg0: InternalFrameEvent) {
                moveLastFocusedComponentToFront()
            }

            override fun internalFrameDeactivated(arg0: InternalFrameEvent) {}
            override fun internalFrameDeiconified(arg0: InternalFrameEvent) {
            }
            override fun internalFrameIconified(arg0: InternalFrameEvent) {
            }
            override fun internalFrameOpened(arg0: InternalFrameEvent) {
            }
        })
        desktopPane.add(internalFrame)
    }

    fun registerComponentInstance(
        workspaceComponent: WorkspaceComponent,
        desktopComponent: DesktopComponent<*>
    ) {
        workspaceComponentDesktopComponentMap[workspaceComponent] = desktopComponent
    }

    fun addDesktopComponent(workspaceComponent: WorkspaceComponent) {
        Logger.trace("Adding workspace component: $workspaceComponent")
        val componentFrame = DesktopInternalFrame(workspaceComponent)
        // componentFrame.setFrameIcon(new ImageIcon(ResourceManager.getImage("icons/20.png")));
        val desktopComponent = createDesktopComponent(componentFrame, workspaceComponent)
        componentFrame.setGuiComponent(desktopComponent)

        // Either add the window at a default location, or relative to the last
        // added window. Note that this is overridden when individual
        // components are opened
        if (workspaceComponentDesktopComponentMap.size == 0) {
            componentFrame.setBounds(
                DEFAULT_WINDOW_OFFSET,
                DEFAULT_WINDOW_OFFSET,
                desktopComponent.preferredSize.getWidth().toInt(),
                desktopComponent.preferredSize.getHeight().toInt()
            )
        } else {
            // This should be coordinated with the logic in
            // RepositionAllWindowsSction
            val highestComponentNumber = workspaceComponentDesktopComponentMap.size + 1
            val xMax = desktopPane.width - desktopComponent.preferredSize.getWidth()
            val yMax = desktopPane.height - desktopComponent.preferredSize.getHeight()
            componentFrame.setBounds(
                (highestComponentNumber * DEFAULT_WINDOW_OFFSET % xMax).toInt(),
                (highestComponentNumber * DEFAULT_WINDOW_OFFSET % yMax).toInt(),
                desktopComponent.preferredSize.getWidth().toInt(),
                desktopComponent.preferredSize.getHeight().toInt()
            )
        }

        // Other initialization
        componentFrame.addComponentListener(componentListener)
        componentFrame.contentPane = desktopComponent
        registerComponentInstance(workspaceComponent, desktopComponent)
        componentFrame.isVisible = true
        componentFrame.title = workspaceComponent.name
        desktopPane.add(componentFrame)
        lastFocusedStack.push(desktopComponent)
        desktopComponent.parentFrame.pack()
        // System.out.println(lastOpened.getName());

        // Forces last component of the desktop to the front
        try {
            (componentFrame as JInternalFrame).isSelected = true
        } catch (e: PropertyVetoException) {
            e.printStackTrace()
        }
    }

    /**
     * Shows the dialog for opening a workspace file.
     */
    suspend fun openWorkspace() {
        workspace.stop()
        val simulationChooser = SFileChooser(WorkspacePreferences.simulationDirectory, "Zip Archive", "zip")
        val simFile = simulationChooser.showOpenDialog()
        if (simFile != null) {
            val progressWindow = ProgressWindow(100, "Loading workspace...")
            progressWindow.minimumSize = Dimension(300, 100)
            progressWindow.setLocationRelativeTo(null)
            
            try {
                workspace.openWorkspace(simFile, useDesktop = true) { current, total ->
                    swingInvokeLater {
                        progressWindow.progressBar.maximum = total
                        progressWindow.value = current
                        progressWindow.text = "Loading components: $current / $total"
                    }
                }
                WorkspacePreferences.simulationDirectory = simulationChooser.currentLocation!!
                workspace.currentFile = simFile
            } finally {
                // Close progress window when done
                progressWindow.close()
            }
        }
    }

    /**
     * Show a save-as dialog.
     */
    fun saveAs() {
        saveAs(checkForProblems = true)
    }

    /**
     * Show a save-as dialog.
     * @param checkForProblems whether to check for problematic save scenarios and show warning
     */
    private fun saveAs(checkForProblems: Boolean) {

        // Check if save would be problematic and show warning before file chooser
        if (checkForProblems && workspace.isSaveProblematic(this)) {
            if (!showSaveWarningDialog()) {
                return // User cancelled
            }
        }

        // Create the file chooser
        val chooser = SFileChooser(WorkspacePreferences.simulationDirectory, "Zip Archive", "zip")

        // Set the file
        val theFile: File?
        theFile = if (workspace.currentFile != null) {
            chooser.showSaveDialog(workspace.currentFile)
        } else {
            // Default workspace
            chooser.showSaveDialog("workspace")
        }

        // Save the file by setting the current file
        if (theFile != null) {
            workspace.currentFile = theFile
            WorkspacePreferences.simulationDirectory = chooser.currentLocation!!
            save(theFile)
        }
    }

    /**
     * If changes exist, show a change dialog, otherwise just save the current file.
     */
    fun save() {

        workspace.stop()

        // Ignore the save command if there are no changes
        if (workspace.changesExist()) {
            // Check if save would be problematic and show warning
            if (workspace.isSaveProblematic(this)) {
                if (!showSaveWarningDialog()) {
                    return // User cancelled
                }
            }
            
            if (workspace.currentFile != null) {
                save(workspace.currentFile)
            } else {
                saveAs(checkForProblems = false) // Warning already shown above
            }
        }
    }

    /**
     * Save a specified file.
     *
     * @param file file to save.
     */
    private fun save(file: File?) {
        if (file != null) {
            frame.title = file.name
            workspace.save(file)
        }
    }

    /**
     * Show a warning dialog when saving a workspace with custom update actions or control panels
     * that won't be restored on reopen.
     *
     * @return true if user wants to proceed with save, false if cancelled
     */
    private fun showSaveWarningDialog(): Boolean {
        // Get the default dialog font
        val font = UIManager.getFont("Label.font")
        val fontFamily = font.family
        val fontSize = font.size
        
        val message = """
            <html>
            <body style='width: 400px; padding: 10px; font-family: $fontFamily; font-size: ${fontSize}pt;'>
            <p>This workspace contains custom update actions or control panels
            that will <b>NOT</b> be restored when reopening.</p>
            
            <p>To make this workspace reopenable, the simulation must:</p>
            <ul>
            <li>Have a unique simulationId (e.g., newSim("my_sim_id"))</li>
            <li>Register a reopen function with .registerReopenFunction()</li>
            </ul>
            
            <p>For more information, see:<br>
            <a href="https://docs.simbrain.net/docs/simulations/#saving-and-reopening-simulations">
            https://docs.simbrain.net/docs/simulations/#saving-and-reopening-simulations</a></p>
            
            <p>Do you want to save anyway?</p>
            </body>
            </html>
        """.trimIndent()
        
        // Create a JEditorPane to display HTML with clickable links
        val editorPane = JEditorPane("text/html", message).apply {
            isEditable = false
            isOpaque = false
            addHyperlinkListener { e ->
                if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        Desktop.getDesktop().browse(e.url.toURI())
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        }
        
        val options = arrayOf("Yes", "No")
        val result = JOptionPane.showOptionDialog(
            frame,
            editorPane,
            "Warning: Workspace May Not Reopen Correctly",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null, // icon
            options, // button labels
            options[1] // initial value - "No" is the default
        )
        return result == 0 // 0 = Yes, 1 = No
    }

    /**
     * Clear desktop of all components. Show a save-as dialog if there have been changes.
     */
    fun clearDesktop() {

        workspace.stop()

        // If there have been changes, show a save-as dialog
        if (workspace.changesExist()) {
            val s = showHasChangedDialog()
            if (s == JOptionPane.OK_OPTION) {
                save()
                clearComponents()
            } else if (s == JOptionPane.NO_OPTION) {
                clearComponents()
            } else if (s == JOptionPane.CANCEL_OPTION) {
                return
            }
        } else {
            // If there have been no changes, just clear away!
            clearComponents()
        }
    }

    /**
     * Helper method to clear all components from the desktop.
     */
    private fun clearComponents() {
        workspaceComponentDesktopComponentMap.clear()
        workspace.clearWorkspace()
    }

    /**
     * Create the GUI and show it. For thread safety, this method should be invoked from the event-dispatching thread.
     */
    private fun createAndShowGUI() {

        // Any time an exception occurs, present a dialog box with the error to the user
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            e.printStackTrace()
            val stackTrace = sw.toString()
            val textArea = JTextArea("An error occurred: ${e.message}\n\n$stackTrace").apply {
                isEditable = false
                rows = 10
                columns = 50
            }
            val scrollPane = JScrollPane(textArea)
            SwingUtilities.invokeLater {
                JOptionPane.showMessageDialog(null, scrollPane, "Uncaught Exception", JOptionPane.ERROR_MESSAGE)
            }
        }

        /*
         * Make sure we have nice window decorations.
         * JFrame.setDefaultLookAndFeelDecorated(true); Create and set up the
         * window.
         */
        frame.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE

        /** Open a default workspace  */
        // openWorkspace(workspace.getCurrentFile());

        frame.isVisible = true
    }

    /**
     * Checks to see if anything has changed and then offers to save if true.
     *
     * @return the JOptionPane pane result
     */
    private fun showHasChangedDialog(): Int {
        val options = arrayOf<Any>("Save", "Don't Save", "Cancel")
        return JOptionPane.showOptionDialog(
            frame,
            """
     The workspace has changed since last save,
     Would you like to save these changes?
     """.trimIndent(),
            "Workspace Has Changed",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            options,
            options[0]
        )
    }

    /**
     * Quit application.
     *
     * @param forceQuit should quit be forced.
     */
    fun quit(forceQuit: Boolean) {
        if (workspace.changesExist() && !forceQuit && workspace.componentList.size > 0) {
            val s = showHasChangedDialog()
            if (s == JOptionPane.OK_OPTION) {
                save()
                quit(true)
            } else if (s == JOptionPane.NO_OPTION) {
                quit(true)
            } else if (s == JOptionPane.CANCEL_OPTION) {
                return
            }
        } else {
            workspace.removeAllComponents()
            System.exit(0)
        }
    }

    /**
     * Update time label.
     */
    fun updateTimeLabel() {
        val timestep = workspace.time
        val updateTimeMs = System.currentTimeMillis()
        if (updateTimeMs - lastUpdateTimeMs > 1000) {
            updateRate = timestep - lastTimestep
            lastTimestep = timestep
            lastUpdateTimeMs = updateTimeMs
        }
        val text = String.format("Timestep: %s (%sHz)", timestep, updateRate)
        timeLabel.text = text
        runningLabel.isVisible = workspace.updater.isRunning
    }

    /**
     * Returns the width of the visible portion of the desktop.
     */
    val width: Double
        get() = desktopPane.visibleRect.getWidth()

    /**
     * Returns the height of the visible portion of the desktop.
     */
    val height: Double
        get() = desktopPane.visibleRect.getHeight()

    /**
     * Position a component given an index. Lays out components in a pattern moving diagonally and downward across the
     * desktop.
     *
     * Note that this is overridden when individual components are opened.
     */
    fun positionComponent(
        positionIndex: Int,
        desktopComponent: DesktopComponent<*>
    ) {

        // TODO: Some better logic that detects whether some existing slot is
        // open would be nice, but this does well enough for now...
        if (positionIndex == 0) {
            // If this is the first window at it at a default position
            desktopComponent.parentFrame.setBounds(
                DEFAULT_WINDOW_OFFSET,
                DEFAULT_WINDOW_OFFSET,
                desktopComponent.preferredSize.getWidth().toInt(),
                desktopComponent.preferredSize.getHeight().toInt()
            )
        } else {
            // Add window below the current window at a slight offent
            desktopComponent.parentFrame.setBounds(
                ((positionIndex + 1) * DEFAULT_WINDOW_OFFSET
                        % (desktopPane.width - desktopComponent
                    .preferredSize.getWidth())).toInt(),
                ((positionIndex + 1) * DEFAULT_WINDOW_OFFSET
                        % (desktopPane.height - desktopComponent
                    .preferredSize.getHeight())).toInt(),
                desktopComponent.preferredSize.getWidth().toInt(),
                desktopComponent.preferredSize.getHeight().toInt()
            )
            // Focus the last positioned frame to have the focus
            try {
                (desktopComponent.parentFrame as JInternalFrame).isSelected = true
            } catch (e: PropertyVetoException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Reposition all the windows. Useful when windows get resized and can't be "recaptured".
     */
    suspend fun repositionAllWindows() {
        var i = 0
        for (frame in desktopPane.allFrames) {
            if (frame is JInternalFrame) {
                positionFrame(i++, frame)
            }
        }
    }

    private fun positionFrame(positionIndex: Int, frame: JInternalFrame) {
        if (positionIndex == 0) {
            frame.setBounds(
                DEFAULT_WINDOW_OFFSET,
                DEFAULT_WINDOW_OFFSET,
                frame.width,
                frame.height
            )
        } else {
            val xMax = desktopPane.width - frame.width
            val yMax = desktopPane.height - frame.height
            frame.setBounds(
                ((positionIndex + 1) * DEFAULT_WINDOW_OFFSET % xMax).toInt(),
                ((positionIndex + 1) * DEFAULT_WINDOW_OFFSET % yMax).toInt(),
                frame.width,
                frame.height
            )
            try {
                frame.isSelected = true
            } catch (e: PropertyVetoException) {
                e.printStackTrace()
            }
        }
    }

    fun resizeAllWindows() {
        var maxX = 0
        var maxY = 0
        val desktopHeight: Double = desktopPane.size.getHeight()
        val desktopWidth: Double = desktopPane.size.getWidth()

        for (c in desktopPane.components) {
            val bottomRightX = (c.width + c.x)
            val bottomRightY = (c.height + c.y)
            if (maxX < bottomRightX) {
                maxX = bottomRightX
            }
            if (maxY < bottomRightY) {
                maxY = bottomRightY
            }
        }

        val xScalingRatio = maxX / desktopWidth
        val yScalingRatio = maxY / desktopHeight

        val finalScalingRatio = if (xScalingRatio > yScalingRatio) 1 / xScalingRatio else 1 / yScalingRatio

        if (finalScalingRatio < 1) {
            for (c in desktopPane.components) {
                val orignalTopLeftX = c.x.toDouble()
                val orignalTopLeftY = c.y.toDouble()
                val originalWidth = c.width
                val originalHeight = c.height
                c.setBounds(
                    (orignalTopLeftX * finalScalingRatio).toInt(),
                    (orignalTopLeftY * finalScalingRatio).toInt(),
                    (originalWidth * finalScalingRatio).toInt(),
                    (originalHeight * finalScalingRatio).toInt()
                )
            }
        }
    }

    /**
     * Called by componentBounds.bsh
     */
    fun getComponentBoundsString() = desktopPane.allFrames.joinToString("\n") {
        "${it.title} (${it.x}, ${it.y}, ${it.width}, ${it.height})"
    }

    /**
     * Creates an instance of the proper wrapper class around the provided instance.
     */
    @JvmStatic
    fun createDesktopComponent(parentFrame: GenericFrame?, component: WorkspaceComponent): DesktopComponent<*> {
        val genericFrame = parentFrame ?: DesktopInternalFrame(component)
        return component.workspace.componentFactory.createGuiComponent(genericFrame, component)
    }

    /**
     * Show Gui View of a workspace component. Used from terminal.
     */
    fun showJFrame(component: WorkspaceComponent) {
        val theFrame = GenericJFrame()
        val desktopComponent = createDesktopComponent(theFrame, component)
        theFrame.isResizable = true
        theFrame.isVisible = true
        theFrame.setBounds(100, 100, 200, 200)
        theFrame.contentPane = desktopComponent
    }

    /**
     * Simbrain main method. Creates a single instance of the Simulation class
     *
     * @param args currently not used
     */
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            // Set macOS-specific properties for menu bar
            if (Utils.isMacOSX()) {
                System.setProperty("apple.laf.useScreenMenuBar", "true")
                System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Simbrain")
                System.setProperty("apple.awt.application.name", "Simbrain")
            }
            
            // Line below for Ubuntu so that icons don't turn on by default
            // See https://stackoverflow.com/questions/10356725/jdesktoppane-has-a-toolbar-at-bottom-of-window-on-linux
            if (Utils.isLinux()) {
                UIManager.put("DesktopPaneUI", "javax.swing.plaf.basic.BasicDesktopPaneUI")
            }
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        SwingUtilities.invokeLater { createAndShowGUI() }
    }
}