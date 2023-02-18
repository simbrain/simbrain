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

import org.simbrain.console.ConsoleComponent
import org.simbrain.docviewer.DocViewerComponent
import org.simbrain.network.NetworkComponent
import org.simbrain.util.*
import org.simbrain.workspace.gui.couplingmanager.DesktopCouplingManager
import java.awt.event.KeyEvent
import javax.swing.Action

/**
 * Workspace action manager contains references to all the actions for a Workspace.
 */
class WorkspaceActionManager(desktop: SimbrainDesktop) {

    val workspace = desktop.workspace

    val newNetworkAction = desktop.desktopPane.createSuspendAction(
        iconPath = "menu_icons/Network.png",
        name = "New network",
        description = "Add a new network to the desktop",
        keyCombo = CmdOrCtrl + 'N',
        coroutineScope = workspace
    ) {
        workspace.addWorkspaceComponent(NetworkComponent(""))
    }

    val newConsoleAction = desktop.desktopPane.createAction(
        iconPath = "menu_icons/Terminal2.png",
        name = "New console",
        description = "Add a new console (terminal window) to the desktop"
    ) {
        val console = ConsoleComponent("")
        workspace.addWorkspaceComponent(console)
    }

    val newDocViewerAction = desktop.desktopPane.createAction(
        iconPath = "menu_icons/Copy.png",
        name = "New doc viewer",
        description = "Add a new document viewer window to the desktop"
    ) {
        val docViewer = DocViewerComponent()
        workspace.addWorkspaceComponent(docViewer)
    }

    val clearWorkspaceAction = desktop.desktopPane.createAction(
        name = "Clear desktop",
        description = "Remove all windows from the desktop",
        keyCombo = CmdOrCtrl + 'K'
    ) {
        desktop.clearDesktop()
    }

    val openWorkspaceAction = desktop.desktopPane.createAction(
        iconPath = "menu_icons/Open.png",
        name = "Open Workspace File (.zip) ...",
        description = "Open a workspace file from .zip"
    ) {
        desktop.openWorkspace()
    }

    val saveWorkspaceAction = desktop.desktopPane.createAction(
        iconPath = "menu_icons/Save.png",
        name = "Save workspace",
        description = "Save current workspace file",
        keyCombo = CmdOrCtrl + 'S'
    ) {
        desktop.save()
    }

    private val saveWorkspaceAsAction = desktop.desktopPane.createAction(
        iconPath = "menu_icons/Save.png",
        name = "Save workspace as...",
        description = "Save current workspace file as .zip"
    ) {
        desktop.saveAs()
    }

    val quitWorkspaceAction = desktop.desktopPane.createAction(
        name = "Quit Simbrain",
        description = "Quit Simbrain",
        keyCombo = CmdOrCtrl + 'Q'
    ) {
        desktop.quit(false)
    }

    val iterateAction = desktop.desktopPane.createAction(
        iconPath = "menu_icons/Step.png",
        name = "Iterate workspace",
        description = "Iterate workspace once",
        initBlock = {
            workspace.updater.events.runStarted.on { isEnabled = false }
            workspace.updater.events.runFinished.on { isEnabled = true }
        },
        keyCombo = KeyCombination(KeyEvent.VK_SPACE)
    ) {
        workspace.iterate()
    }

    val runAction = desktop.desktopPane.createAction(
        iconPath = "menu_icons/Play.png",
        name = "Run",
        description = "Run workspace"
    ) {
        workspace.run()
    }

    val stopAction = desktop.desktopPane.createAction(
        iconPath = "menu_icons/Stop.png",
        name = "Stop",
        description = "Stop workspace"
    ) {
        workspace.stop()
    }

    val openCouplingManagerAction = desktop.desktopPane.createAction(
        iconPath = "menu_icons/Coupling.png",
        name = "Open coupling manager...",
        description = "Open workspace coupling manager."
    ) {
        DesktopCouplingManager(desktop).displayInDialog {  }
    }

    val openCouplingListAction = desktop.desktopPane.createAction(
        iconPath = "menu_icons/CouplingList.png",
        name = "Open coupling list...",
        description = "Open list of workspace couplings."
    ) {
        CouplingListPanel(desktop, desktop.workspace.couplings).displayInDialog {  }
    }

    val propertyTabAction = desktop.desktopPane.createAction(
        iconPath = "menu_icons/systemMonitor.png",
        name = "Show / hide dock",
        description = "Toggle dock visibility."
    ) {
        desktop.toggleDock()
    }

    val showUpdaterDialog = desktop.desktopPane.createAction(
        iconPath = "menu_icons/Sequence.png",
        name = "Edit Update Sequence...",
        description = "Edit workspace update actions"
    ) {
        WorkspaceUpdateManagerPanel(workspace).displayInDialog {  }
    }

    val repositionAllWindowsAction = desktop.desktopPane.createAction(
        name = "Gather windows",
        description = "Repositions and resize all windows. Useful when windows get \"lost\" offscreen."
    ) {
        desktop.repositionAllWindows()
    }

    val resizeAllWindowsAction = desktop.desktopPane.createAction(
        name = "Resize windows",
        description = "Resize all windows on screen so they fit on the current desktop. Useful when windows get \"lost\" offscreen."
    ) {
        desktop.resizeAllWindows()
    }

    val runControlActions = listOf(runAction, stopAction)

    val openSaveWorkspaceActions = listOf(openWorkspaceAction, saveWorkspaceAction, saveWorkspaceAsAction)

    fun createComponentFactoryAction(
        name: String,
        iconPath: String
    ): Action {
        return createAction(
            name = name,
            iconPath = iconPath,
            description = "Create $name"
        ) {
            workspace.componentFactory.createWorkspaceComponent(name)
        }
    }

    val plotActions = listOf(
        createComponentFactoryAction("Bar Chart", "menu_icons/BarChart.png"),
        createComponentFactoryAction("Histogram", "menu_icons/BarChart.png"),
        createComponentFactoryAction("Pie Chart", "menu_icons/PieChart.png"),
        createComponentFactoryAction("Pixel Plot", "menu_icons/grid.png"),
        createComponentFactoryAction("Projection Plot", "menu_icons/ProjectionIcon.png"),
        createComponentFactoryAction("Raster Plot", "menu_icons/ScatterIcon.png"),
        createComponentFactoryAction("Time Series", "menu_icons/CurveChart.png")
    )
    val newWorldActions = listOf(
        createComponentFactoryAction("Data Table", "menu_icons/Table.png"),
        createComponentFactoryAction("Odor World", "menu_icons/SwissIcon.png"),
        createComponentFactoryAction("3D World", "menu_icons/World.png"),
        createComponentFactoryAction("Image World", "menu_icons/photo.png"),
        createComponentFactoryAction("Text World", "menu_icons/Text.png")
    )

}