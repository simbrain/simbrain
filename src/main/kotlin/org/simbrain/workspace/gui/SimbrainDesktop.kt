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
package org.simbrain.workspace.gui

import bsh.Interpreter
import bsh.util.JConsole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import org.pmw.tinylog.Logger
import org.simbrain.console.ConsoleDesktopComponent
import org.simbrain.custom_sims.NewSimulation
import org.simbrain.custom_sims.Simulation
import org.simbrain.custom_sims.simulations
import org.simbrain.util.*
import org.simbrain.util.genericframe.GenericFrame
import org.simbrain.util.genericframe.GenericJFrame
import org.simbrain.util.genericframe.GenericJInternalFrame
import org.simbrain.util.widgets.ShowHelpAction
import org.simbrain.util.widgets.ToggleButton
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.workspace.WorkspacePreferences
import org.simbrain.workspace.updater.PerformanceMonitor.enabled
import java.awt.*
import java.awt.event.*
import java.beans.PropertyVetoException
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import javax.swing.*
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

    @JvmField
    val desktopPane: JDesktopPane = JDesktopPane()

    /**
     * Name to display in Simbrain desktop window.
     */
    private const val FRAME_TITLE = "Simbrain 4 Beta"

    /**
     * Associates workspace components with their corresponding desktop components.
     */
    private val workspaceComponentDesktopComponentMap: MutableMap<WorkspaceComponent, DesktopComponent<*>> = LinkedHashMap()

    val desktopComponents: Collection<DesktopComponent<*>>
        get() = workspaceComponentDesktopComponentMap.values


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

    /**
     * Workspace tool bar.
     */
    private var wsToolBar = JToolBar()

    /**
     * Whether the bottom dock is visible.
     */
    private var dockVisible = true

    /**
     * The frame that will hold the workspace.
     */
    val frame: JFrame = JFrame(FRAME_TITLE)

    /**
     * The bottom dock.
     */
    private val bottomDock: JTabbedPane

    /**
     * Pane splitter for bottom dock.
     */
    private val horizontalSplitter: JSplitPane

    /**
     * Boundary of workspace.
     */
    private val workspaceBounds: Rectangle

    /**
     * Workspace action manager.
     */
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
        /**
         * @return Terminal panel.
         */
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
     * Print text to terminal.
     *
     * @param toPrint text to print
     */
    fun printToTerminal(toPrint: String?) {
        interpreter!!.println(toPrint)
    }

    /**
     * Creates the workspace tool bar.
     *
     * @return JToolBar tool bar created
     */
    private fun createToolBar(): JToolBar {
        val bar = JToolBar()
        bar.add(actionManager.openWorkspaceAction)
        bar.add(actionManager.saveWorkspaceAction)
        bar.addSeparator()
        bar.add(actionManager.iterateAction)
        bar.add(ToggleButton(actionManager.runControlActions).apply {
            setAction("Run")
            workspace.updater.events.runStarted.on{
                setAction("Stop")
            }
            workspace.updater.events.runFinished.on{
                setAction("Run")
            }
        })
        bar.addSeparator()
        bar.add(actionManager.openCouplingManagerAction)
        bar.addSeparator()
        bar.add(actionManager.newNetworkAction)

        /* World menu button. */
        var button = JButton()
        button.icon = ResourceManager.getImageIcon("menu_icons/World.png")
        val worldMenu = JPopupMenu()
        for (action in actionManager.newWorldActions) {
            worldMenu.add(action)
        }
        button.addActionListener { e ->
            val button = e.source as JButton
            worldMenu.show(button, 0, button.height)
        }
        button.componentPopupMenu = worldMenu
        bar.add(button)

        /* Chart menu button. */button = JButton()
        button.icon = ResourceManager.getImageIcon("menu_icons/BarChart.png")
        val gaugeMenu = JPopupMenu()
        for (action in actionManager.plotActions) {
            gaugeMenu.add(action)
        }
        button.addActionListener { e ->
            val button = e.source as JButton
            gaugeMenu.show(button, 0, button.height)
        }
        button.componentPopupMenu = gaugeMenu
        bar.add(button)
        bar.add(actionManager.newConsoleAction)

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
        runningLabel.icon = ResourceManager.getImageIcon("menu_icons/Throbber.gif")
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

    /**
     * Create script menu.
     *
     * @return script JMenu
     */
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

    /**
     * Create the workspace file menu.
     *
     * @return file menu
     */
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
        fileMenu.add(actionManager.quitWorkspaceAction)
        return fileMenu
    }

    /**
     * Create the workspace view menu.
     *
     * @return view menu
     */
    private fun createViewMenu(): JMenu {
        val viewMenu = JMenu("View")
        viewMenu.add(actionManager.propertyTabAction)
        viewMenu.addSeparator()
        viewMenu.add(JMenuItem(actionManager.resizeAllWindowsAction))
        viewMenu.add(JMenuItem(actionManager.repositionAllWindowsAction))
        return viewMenu
    }

    /**
     * Create the workspace insert menu.
     *
     * @return insert menu
     */
    private fun createInsertMenu(): JMenu {
        val insertMenu = JMenu("Insert")
        insertMenu.add(actionManager.newNetworkAction)
        // insertMenu.add(new OpenEditorAction(this)); //TODO: Move this action
        // manager
        val newGaugeSubMenu = JMenu("New Plot")
        for (action in actionManager.plotActions) {
            newGaugeSubMenu.add(action)
        }
        insertMenu.add(newGaugeSubMenu)
        val newWorldSubMenu = JMenu("New World")
        for (action in actionManager.newWorldActions) {
            newWorldSubMenu.add(action)
        }
        insertMenu.add(newWorldSubMenu)
        insertMenu.addSeparator()
        insertMenu.add(actionManager.newDocViewerAction)
        insertMenu.add(actionManager.newConsoleAction)
        return insertMenu
    }

    /**
     * Create the workspace couplings menu.
     *
     * @return couplings menu
     */
    private fun createCoupleMenu(): JMenu {
        val coupleMenu = JMenu("Couplings")
        coupleMenu.add(actionManager.openCouplingManagerAction)
        coupleMenu.add(actionManager.openCouplingListAction)
        return coupleMenu
    }

    /**
     * Create the workspace help menu.
     *
     * @return help menu
     */
    private fun createHelpMenu(): JMenu {
        val helpMenu = JMenu("Help")
        helpMenu.add(ShowHelpAction("Main Help", "SimbrainDocs.html"))
        helpMenu.addSeparator()
        helpMenu.add(ShowHelpAction("Quick start", "Pages/QuickStart.html"))
        helpMenu.add(ShowHelpAction("Keyboard Shortcuts", "KeyboardShortcuts.html"))
        helpMenu.add(ShowHelpAction("Credits", "SimbrainCredits.html"))
        return helpMenu
    }

    /**
     * Create a new context menu for this network panel.
     */
    private fun createContextMenu() {
        contextMenu = JPopupMenu()
        contextMenu!!.add(actionManager.newNetworkAction)
        val newGaugeSubMenu = JMenu("New Plot")
        for (action in actionManager.plotActions) {
            newGaugeSubMenu.add(action)
        }
        contextMenu!!.add(newGaugeSubMenu)
        val newWorldSubMenu = JMenu("New World")
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
     *
     * @param component component to check with
     * @return component guicomponent
     */
    fun getDesktopComponent(component: WorkspaceComponent): DesktopComponent<*> {
        return workspaceComponentDesktopComponentMap[component]!!
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
            init()
            this.workspaceComponent = workspaceComponent
        }

        /**
         * Initialize the frame.
         */
        private fun init() {
            isResizable = true
            isMaximizable = true
            isIconifiable = true
            isClosable = true
            defaultCloseOperation = DO_NOTHING_ON_CLOSE
            addInternalFrameListener(WindowFrameListener())
        }

        /**
         * Set the Gui Component.
         *
         * @param desktopComponent the component to set.
         */
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
    } // End DesktopInternalFrame

    /**
     * Add internal frame.
     *
     * @param internalFrame the frame to add.
     */
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

    /**
     * Registers instance of guiComponents.
     *
     * @param workspaceComponent Workspace component
     * @param desktopComponent       GUI component
     */
    fun registerComponentInstance(
        workspaceComponent: WorkspaceComponent,
        desktopComponent: DesktopComponent<*>
    ) {
        desktopComponent.desktop = this
        workspaceComponentDesktopComponentMap[workspaceComponent] = desktopComponent
    }

    /**
     * Add a new SimbrainComponent.
     *
     * @param workspaceComponent Workspace Component
     */
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
    fun openWorkspace() {
        workspace.stop()
        val simulationChooser = SFileChooser(WorkspacePreferences.simulationDirectory, "Zip Archive", "zip")
        val simFile = simulationChooser.showOpenDialog()
        if (simFile != null) {
            workspace.openWorkspace(simFile)
            workspace.currentDirectory = simulationChooser.currentLocation
            workspace.currentFile = simFile
        }
    }

    /**
     * Show a save-as dialog.
     */
    fun saveAs() {

        // Create the file chooser
        val chooser = SFileChooser(workspace.currentDirectory, "Zip Archive", "zip")

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
            workspace.currentDirectory = chooser.currentLocation
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
            if (workspace.currentFile != null) {
                save(workspace.currentFile)
            } else {
                saveAs() // Show save-as if there is no current file.
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
        // any time an exception occurs, present a dialog box with the error to the user
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
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

        /* Display the window. */frame.isVisible = true
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
     * Listener for mouse presses.
     */
    private val mouseListener: MouseListener = object : MouseAdapter() {
        /**
         * Responds to mouse events.
         *
         * @param mouseEvent Mouse Event
         */
        override fun mousePressed(mouseEvent: MouseEvent) {
            val lastClickedPoint = mouseEvent.point
            // System.out.println("desktop-->" + lastClickedPoint); //TODO: Make
            // this visible somehow
            if (mouseEvent.isControlDown || mouseEvent.button == MouseEvent.BUTTON3) {
                contextMenu!!.show(
                    frame,
                    lastClickedPoint.getX().toInt() + MENU_X_OFFSET,
                    lastClickedPoint.getY().toInt() + MENU_Y_OFFSET
                )
            }
        }
    }

    /**
     * listener for window closing events.
     */
    private val windowListener: WindowListener = object : WindowAdapter() {
        /**
         * Responds to window closing events.
         *
         * @param arg0 Window event
         */
        override fun windowClosing(arg0: WindowEvent) {
            quit(false)
        }
    }

    /**
     * listens to menu events for setting save enabled.
     */
    private val menuListener: MenuListener = object : MenuListener {
        /**
         * Responds to menu selected events.
         *
         * @param arg0 Menu event
         */
        override fun menuSelected(arg0: MenuEvent) {
            if (workspace.changesExist()) {
                actionManager.saveWorkspaceAction.isEnabled = true
            } else {
                actionManager.saveWorkspaceAction.isEnabled = false
            }
        }

        /**
         * Responds to menu deslected events.
         *
         * @param arg0 Menu event
         */
        override fun menuDeselected(arg0: MenuEvent) {
            /* no implementation */
        }

        /**
         * Responds to menu canceled events.
         *
         * @param arg0 Menu event
         */
        override fun menuCanceled(arg0: MenuEvent) {
            /* no implementation */
        }
    }

    /**
     * Default constructor.
     *
     * @param workspace The workspace for this desktop.
     */
    init {
        frame.iconImages = Arrays.asList(
            ResourceManager.getImage("simbrain_iconset/icon_20x20.png"),
            ResourceManager.getImage("simbrain_iconset/icon_32x32.png"),
            ResourceManager.getImage("simbrain_iconset/icon_40x40.png"),
            ResourceManager.getImage("simbrain_iconset/icon_64x64.png"),
            ResourceManager.getImage("simbrain_iconset/icon_128x128.png"),
            ResourceManager.getImage("simbrain_iconset/icon_512x512.png")
        )
        createAndAttachMenus()
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
            val component = workspaceComponentDesktopComponentMap[wc] ?: return@on
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
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        workspaceBounds = Rectangle(
            WORKSPACE_INSET,
            WORKSPACE_INSET,
            screenSize.width - WORKSPACE_INSET * 2,
            screenSize.height - WORKSPACE_INSET * 2
        )

        dockVisible = false

        // Set up Desktop
        if (System.getProperty("os.name").lowercase(Locale.getDefault()).contains("windows")) {
            desktopPane.background = Color.WHITE
            desktopPane.border = BorderFactory.createLoweredBevelBorder()
        }
        desktopPane.addMouseListener(mouseListener)
        desktopPane.addKeyListener(WorkspaceKeyAdapter(workspace))
        desktopPane.preferredSize =
            Dimension(screenSize.width - WORKSPACE_INSET * 2, screenSize.height - WORKSPACE_INSET * 3)

        // Create the Tabbed Pane for bottom of the desktop
        bottomDock = JTabbedPane()
        bottomDock.addTab("Components", null, ComponentPanel(this), "Show workspace components")
        bottomDock.addTab("Terminal", null, terminalPanel, "Simbrain terminal")
        bottomDock.addTab(
            "Performance",
            null,
            PerformanceMonitorPanel(this.workspace),
            "Performance and thread monitoring"
        )

        // Set up the main panel
        horizontalSplitter = JSplitPane(JSplitPane.VERTICAL_SPLIT)
        horizontalSplitter.dividerLocation = dividerLocation
        horizontalSplitter.topComponent = desktopPane
        horizontalSplitter.bottomComponent = bottomDock
        val mainPanel = JPanel(BorderLayout())
        mainPanel.add(wsToolBar, "North")
        mainPanel.add(horizontalSplitter, "Center")
        if (!dockVisible) {
            horizontalSplitter.bottomComponent.isVisible = false
        }

        // Set up Frame
        frame.bounds = workspaceBounds
        frame.contentPane = mainPanel
        frame.pack()
        frame.addWindowListener(windowListener)
        frame.addKeyListener(WorkspaceKeyAdapter(workspace))

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
     * Provisional Code for toggling tab dock's visibility.
     */
    fun toggleDock() {
        if (dockVisible) {
            dockVisible = false
            horizontalSplitter.bottomComponent.isVisible = false
            enabled = false
        } else {
            dockVisible = true
            horizontalSplitter.bottomComponent.isVisible = true
            enabled = true
            horizontalSplitter.dividerLocation = dividerLocation
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

    private val dividerLocation: Int
        /**
         * Helper method for determining where the bottom tab should be placed.
         *
         * @return the location
         */
        private get() = (3 * (workspaceBounds.getHeight() / 4)).toInt()
    val width: Double
        /**
         * Returns the width of the visible portion of the desktop.
         *
         * @return visible width.
         */
        get() = desktopPane.visibleRect.getWidth()
    val height: Double
        /**
         * Returns the height of the visible portion of the desktop.
         *
         * @return the visible height
         */
        get() = desktopPane.visibleRect.getHeight()

    /**
     * Position a component given an index. Lays out components in a pattern moving diagonally and downward across the
     * desktop.
     *
     *
     * Note that this is overridden when individual components are opened.
     *
     * @param positionIndex
     * @param desktopComponent
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
    fun repositionAllWindows() {
        // TODO: Do this for non-component internal frames as well?
        var i = 0
        for (component in desktopComponents) {
            positionComponent(i++, component)
        }
    }

    fun resizeAllWindows() {
        var maxX = 0
        var maxY = 0
        val desktopHeight: Double = desktopPane.getSize().getHeight()
        val desktopWidth: Double = desktopPane.getSize().getWidth()

        for (c in desktopPane.getComponents()) {
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
            for (c in desktopPane.getComponents()) {
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
     *
     * @param component   The component to wrap.
     * @param parentFrame The frame of this component
     * @return A new desktop component wrapping the provided component.
     */
    @JvmStatic
    fun createDesktopComponent(parentFrame: GenericFrame?, component: WorkspaceComponent): DesktopComponent<*> {
        val genericFrame = parentFrame ?: DesktopInternalFrame(component)
        return component.workspace.componentFactory.createGuiComponent(genericFrame, component)
    }

    /**
     * Show Gui View of a workspace component. Used from terminal.
     *
     * @param component component to view
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
            // Line below for Ubuntu so that icons don't turn on by default
            // See https://stackoverflow.com/questions/10356725/jdesktoppane-has-a-toolbar-at-bottom-of-window-on-linux
            if (Utils.isLinux()) {
                UIManager.put("DesktopPaneUI", "javax.swing.plaf.basic.BasicDesktopPaneUI")
            }
            UIManager.setLookAndFeel(
                UIManager.getSystemLookAndFeelClassName()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        SwingUtilities.invokeLater { createAndShowGUI() }
    }
}