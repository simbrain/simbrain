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

import org.simbrain.plot.barchart.BarChartComponent
import org.simbrain.plot.barchart.BarChartModel
import org.simbrain.plot.histogram.HistogramComponent
import org.simbrain.plot.histogram.HistogramModel
import org.simbrain.plot.piechart.PieChartComponent
import org.simbrain.plot.piechart.PieChartModel
import org.simbrain.plot.pixelplot.EmitterMatrix
import org.simbrain.plot.pixelplot.PixelPlotComponent
import org.simbrain.plot.projection.ProjectionComponent
import org.simbrain.plot.rasterchart.RasterModel
import org.simbrain.plot.rasterchart.RasterPlotComponent
import org.simbrain.plot.timeseries.TimeSeriesModel
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent
import org.simbrain.util.CmdOrCtrl
import org.simbrain.util.KeyCombination
import org.simbrain.util.createAction
import org.simbrain.util.displayInDialog
import org.simbrain.workspace.Consumer
import org.simbrain.workspace.Producer
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.workspace.couplings.CouplingManager
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.workspace.gui.couplingmanager.DesktopCouplingManager
import org.simbrain.world.dataworld.DataWorld
import org.simbrain.world.dataworld.DataWorldComponent
import org.simbrain.world.imageworld.ImageWorldComponent
import org.simbrain.world.imageworld.filters.Filter
import java.lang.Math.ceil
import java.lang.Math.sqrt
import javax.swing.Action
import javax.swing.JMenu
import javax.swing.JOptionPane

/**
 * Workspace action manager contains references to all the actions for a Workspace.
 */
class WorkspaceActions {

    val workspace = SimbrainDesktop.workspace

    val newNetworkAction = createComponentFactoryAction("Network", "menu_icons/Network.png", CmdOrCtrl + 'N')
    val newConsoleAction = createComponentFactoryAction("Console", "menu_icons/Terminal2.png")
    val newDocViewerAction = createComponentFactoryAction("Document Viewer", "menu_icons/Copy.png")

    val clearWorkspaceAction = SimbrainDesktop.desktopPane.createAction(
        name = "Clear desktop",
        description = "Remove all windows from the desktop",
        keyboardShortcut = CmdOrCtrl + 'K',
        coroutineScope = workspace
    ) {
        SimbrainDesktop.clearDesktop()
    }

    val openWorkspaceAction = SimbrainDesktop.desktopPane.createAction(
        iconPath = "menu_icons/Open.png",
        name = "Open Workspace File (.zip) ...",
        description = "Open a workspace file from .zip",
        coroutineScope = workspace
    ) {
        SimbrainDesktop.openWorkspace()
    }

    val saveWorkspaceAction = SimbrainDesktop.desktopPane.createAction(
        iconPath = "menu_icons/Save.png",
        name = "Save workspace",
        description = "Save current workspace file",
        keyboardShortcut = CmdOrCtrl + 'S',
        coroutineScope = workspace
    ) {
        SimbrainDesktop.save()
    }

    private val saveWorkspaceAsAction = SimbrainDesktop.desktopPane.createAction(
        iconPath = "menu_icons/Save.png",
        name = "Save workspace as...",
        description = "Save current workspace file as .zip",
        coroutineScope = workspace
    ) {
        SimbrainDesktop.saveAs()
    }

    val quitWorkspaceAction = SimbrainDesktop.desktopPane.createAction(
        name = "Quit Simbrain",
        description = "Quit Simbrain",
        keyboardShortcut = CmdOrCtrl + 'Q',
        coroutineScope = workspace
    ) {
        SimbrainDesktop.quit(false)
    }

    val iterateAction = SimbrainDesktop.desktopPane.createAction(
        iconPath = "menu_icons/Step.png",
        name = "Iterate workspace",
        description = "Iterate workspace once",
        initBlock = {
            workspace.updater.events.runStarted.on { isEnabled = false }
            workspace.updater.events.runFinished.on { isEnabled = true }
        },
        coroutineScope = workspace
    ) {
        workspace.iterate()
    }

    val runAction = SimbrainDesktop.desktopPane.createAction(
        iconPath = "menu_icons/Play.png",
        name = "Run",
        description = "Run workspace",
        coroutineScope = workspace
    ) {
        workspace.run()
    }

    val stopAction = SimbrainDesktop.desktopPane.createAction(
        iconPath = "menu_icons/Stop.png",
        name = "Stop",
        description = "Stop workspace",
        coroutineScope = workspace
    ) {
        workspace.stop()
    }

    val openCouplingManagerAction = SimbrainDesktop.desktopPane.createAction(
        iconPath = "menu_icons/Coupling.png",
        name = "Open coupling manager...",
        description = "Open workspace coupling manager.",
        coroutineScope = workspace
    ) {
        DesktopCouplingManager(SimbrainDesktop).displayInDialog {  }
    }

    val openCouplingListAction = SimbrainDesktop.desktopPane.createAction(
        iconPath = "menu_icons/CouplingList.png",
        name = "Open coupling list...",
        description = "Open list of workspace couplings.",
        coroutineScope = workspace
    ) {
        CouplingListPanel(SimbrainDesktop, SimbrainDesktop.workspace.couplings).displayInDialog {  }
    }

    val propertyTabAction = SimbrainDesktop.desktopPane.createAction(
        iconPath = "menu_icons/systemMonitor.png",
        name = "Show / hide dock",
        description = "Toggle dock visibility.",
        coroutineScope = workspace
    ) {
        SimbrainDesktop.toggleDock()
    }

    val showUpdaterDialog = SimbrainDesktop.desktopPane.createAction(
        iconPath = "menu_icons/Sequence.png",
        name = "Edit Update Sequence...",
        description = "Edit workspace update actions",
        coroutineScope = workspace
    ) {
        WorkspaceUpdateManagerPanel(workspace).displayInDialog {  }
    }

    val repositionAllWindowsAction = SimbrainDesktop.desktopPane.createAction(
        name = "Gather windows",
        description = "Repositions and resize all windows. Useful when windows get \"lost\" offscreen.",
        coroutineScope = workspace
    ) {
        SimbrainDesktop.repositionAllWindows()
    }

    val resizeAllWindowsAction = SimbrainDesktop.desktopPane.createAction(
        name = "Resize windows",
        description = "Resize all windows on screen so they fit on the current SimbrainDesktop. Useful when windows get \"lost\" offscreen.",
        coroutineScope = workspace
    ) {
        SimbrainDesktop.resizeAllWindows()
    }

    val runControlActions = listOf(runAction, stopAction)

    val openSaveWorkspaceActions = listOf(openWorkspaceAction, saveWorkspaceAction, saveWorkspaceAsAction)

    fun createComponentFactoryAction(
        name: String,
        iconPath: String,
        keyboardShortcut: KeyCombination? = null
    ): Action {
        return SimbrainDesktop.desktopPane.createAction(
            name = name,
            iconPath = iconPath,
            description = "Create $name",
            keyboardShortcut = keyboardShortcut,
            coroutineScope = workspace
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
        createComponentFactoryAction("Text World", "menu_icons/Text.png"),
        createComponentFactoryAction("Sound World", "menu_icons/speaker.png")
    )

    fun <T: WorkspaceComponent> createImportAction(desktopComponent: DesktopComponent<T>) = desktopComponent.createAction(
        name = "Import from xml...",
        iconPath = "menu_icons/Open.png",
        description = "Import from xml",
        keyboardShortcut = CmdOrCtrl + 'O',
        coroutineScope = workspace
    ) {
        desktopComponent.showImportDialog()
    }

    fun <T: WorkspaceComponent> createExportAction(desktopComponent: DesktopComponent<T>) = desktopComponent.createAction(
        name = "Export to xml...",
        iconPath = "menu_icons/Save.png",
        description = "Export to xml",
        keyboardShortcut = CmdOrCtrl + 'S',
        coroutineScope = workspace
    ) {
        desktopComponent.showExportDialog()
    }

    fun <T: WorkspaceComponent> createCloseAction(desktopComponent: DesktopComponent<T>) = desktopComponent
        .createAction(
        name = "Close",
        description = "Close component",
        keyboardShortcut = CmdOrCtrl + 'W',
        coroutineScope = workspace
    ) {
        desktopComponent.workspaceComponent.tryClosing()
    }

    fun <T: WorkspaceComponent> createRenameAction(desktopComponent: DesktopComponent<T>) = desktopComponent
        .createAction(
            name = "Rename...",
            description = "Rename this component",
            coroutineScope = workspace
        ) {
            val newTitle: String = JOptionPane.showInputDialog(
                null,
                "Rename component",
                desktopComponent.title
            )
            desktopComponent.title = newTitle
            workspaceComponent.name = newTitle
        }

    private fun <T: WorkspaceComponent> createCoupledPlotAction(
        producer: Producer,
        plotType: String,
        objectName: String,
        iconPath: String,
        description: String = "Create Coupled $plotType",
        componentCreator: (componentName: String) -> T,
        consumerProvider: CouplingManager.(T) -> Consumer
    ) = SimbrainDesktop.desktopPane.createAction(
        name = plotType,
        iconPath = iconPath,
        description = description,
        coroutineScope = workspace
    ) {
        val component = componentCreator("$plotType of $objectName")
        workspace.addWorkspaceComponent(component)
        with(workspace.couplingManager) {
            producer couple consumerProvider(component)
        }
    }

    @JvmOverloads
    fun createCoupledProjectionPlotAction(producer: Producer, objectName: String, plotType: String = "Projection Plot") = createCoupledPlotAction(
        producer = producer,
        plotType = plotType,
        objectName = objectName,
        iconPath = "menu_icons/ProjectionIcon.png",
        componentCreator = { name -> ProjectionComponent(name) },
        consumerProvider = {
            it.getConsumer("addPoint")
        }
    )

    @JvmOverloads
    fun createCoupledTimeSeriesPlotAction(producer: Producer, objectName: String, plotType: String = "Time Series Plot") = createCoupledPlotAction(
        producer = producer,
        plotType = plotType,
        objectName = objectName,
        iconPath = "menu_icons/CurveChart.png",
        componentCreator = { name -> TimeSeriesPlotComponent(name) },
        consumerProvider = {
            it.model.getConsumer(TimeSeriesModel::setValues)
        }
    )

    @JvmOverloads
    fun createCoupledHistogramPlotAction(producer: Producer, objectName: String, plotType: String = "Histogram Plot") = createCoupledPlotAction(
        producer = producer,
        plotType = plotType,
        objectName = objectName,
        iconPath = "menu_icons/BarChart.png",
        componentCreator = { name -> HistogramComponent(name) },
        consumerProvider = {
            it.model.getConsumer(HistogramModel::addData.name)
        }
    )

    @JvmOverloads
    fun createCoupledPieChartAction(producer: Producer, objectName: String, plotType: String = "Pie Chart") = createCoupledPlotAction(
        producer = producer,
        plotType = plotType,
        objectName = objectName,
        iconPath = "menu_icons/PieChart.png",
        componentCreator = { name -> PieChartComponent(name) },
        consumerProvider = {
            it.model.getConsumer(PieChartModel::setValues)
        }
    )

    @JvmOverloads
    fun createCoupledBarChartAction(producer: Producer, objectName: String, plotType: String = "Bar Chart") = createCoupledPlotAction(
        producer = producer,
        plotType = plotType,
        objectName = objectName,
        iconPath = "menu_icons/BarChart.png",
        componentCreator = { name -> BarChartComponent(name) },
        consumerProvider = {
            it.model.getConsumer(BarChartModel::setBarValues)
        }
    )

    @JvmOverloads
    fun createCoupledRasterPlotAction(producer: Producer, objectName: String, plotType: String = "Raster Plot") = createCoupledPlotAction(
        producer = producer,
        plotType = plotType,
        objectName = objectName,
        iconPath = "menu_icons/ScatterIcon.png",
        componentCreator = { name -> RasterPlotComponent(name) },
        consumerProvider = {
            it.model.rasterConsumerList.first().getConsumer(RasterModel.RasterConsumer::setValues)
        }
    )

    @JvmOverloads
    fun createCoupledPixelPlotAction(producer: Producer, objectName: String, plotType: String = "Pixel Plot") = createCoupledPlotAction(
        producer = producer,
        plotType = plotType,
        objectName = objectName,
        iconPath = "menu_icons/grid.png",
        componentCreator = { name -> PixelPlotComponent(name) },
        consumerProvider = {
            it.emitter.getConsumer(EmitterMatrix::setBrightness)
        }
    )

    fun createCoupledDataWorldAction(name: String = "Record Data", producer: Producer, sourceName: String, numCols: Int) = SimbrainDesktop.desktopPane.createAction(
        name = name,
        iconPath = "menu_icons/Table.png",
        coroutineScope = workspace
    ) {
        val component = DataWorldComponent(sourceName, DataWorld(cols = numCols)).apply {
            dataWorld.appendMode = DataWorld.DataEntryMode.APPEND
        }
        workspace.addWorkspaceComponent(component)
        with(workspace.couplingManager) {
            producer couple component.dataWorld.getConsumer(DataWorld::setCurrentNumericRow)
        }
    }

    @JvmOverloads
    fun createCoupledPlotMenu(producer: Producer, objectName: String, menuTitle: String = "Couple Plots"): JMenu {
        val menu = JMenu(menuTitle)
        menu.add(createCoupledBarChartAction(producer, objectName))
        menu.add(createCoupledPieChartAction(producer, objectName))
        menu.add(createCoupledPixelPlotAction(producer, objectName))
        menu.add(createCoupledProjectionPlotAction(producer, objectName))
        menu.add(createCoupledHistogramPlotAction(producer, objectName))
        menu.add(createCoupledRasterPlotAction(producer, objectName))
        menu.add(createCoupledTimeSeriesPlotAction(producer, objectName))
        return menu
    }

    /**
     * Create an action that couples an image world to a consumer (e.g. neuron group or array) with the specified
     * number of units.
     */
    @JvmOverloads
    fun createImageInput(consumer: Consumer, numUnits: Int, menuTitle: String = "Create Image Input", postActionBlock:
        () -> Unit = {}) = SimbrainDesktop.desktopPane.createAction(
        name = menuTitle,
        iconPath = "menu_icons/photo.png",
        description = "Create Image Input",
        coroutineScope = workspace
    ) {
        val component = ImageWorldComponent("Image Input for ${consumer.baseObject.id}")
        val length = ceil(sqrt(numUnits.toDouble())).toInt()
        component.world.resetImageAlbum(length, length)
        workspace.addWorkspaceComponent(component)
        val producer = component.world.currentFilter.getProducer(Filter::getBrightness)
        with(workspace.couplingManager) {
            producer couple consumer
        }
        postActionBlock()
    }

}