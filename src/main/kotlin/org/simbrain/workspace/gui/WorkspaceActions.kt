package org.simbrain.workspace.gui

import org.simbrain.network.core.TensorLayer
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.plot.barchart.BarChartComponent
import org.simbrain.plot.barchart.BarChartModel
import org.simbrain.plot.heatmap.HeatMapComponent
import org.simbrain.plot.heatmap.HeatMapModel
import org.simbrain.plot.histogram.HistogramComponent
import org.simbrain.plot.histogram.HistogramModel
import org.simbrain.plot.piechart.PieChartComponent
import org.simbrain.plot.piechart.PieChartModel
import org.simbrain.plot.pixelplot.PixelPlot
import org.simbrain.plot.pixelplot.PixelPlotComponent
import org.simbrain.plot.projection.ProjectionComponent
import org.simbrain.plot.raster.RasterModel
import org.simbrain.plot.rasterchart.RasterPlotComponent
import org.simbrain.plot.timeseries.TimeSeriesModel
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent
import org.simbrain.util.*
import org.simbrain.workspace.Consumer
import org.simbrain.workspace.Producer
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.workspace.WorkspacePreferences
import org.simbrain.workspace.couplings.CouplingManager
import org.simbrain.workspace.couplings.getConsumer
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.workspace.gui.SimbrainDesktop.desktopPane
import org.simbrain.workspace.gui.couplingmanager.DesktopCouplingManager
import org.simbrain.world.dataworld.DataWorld
import org.simbrain.world.dataworld.DataWorldComponent
import org.simbrain.world.imageworld.ImageWorldComponent
import org.simbrain.world.imageworld.filters.ImageProcessingPipeline
import org.simbrain.world.odorworld.OdorWorldPreferences
import java.lang.Math.ceil
import java.lang.Math.sqrt
import javax.swing.Action
import javax.swing.JMenu

/**
 * Workspace action manager contains references to all the actions for a Workspace.
 */
class WorkspaceActions {

    val workspace = SimbrainDesktop.workspace

    val newNetworkAction = createComponentFactoryAction("Network", "menu_icons/network_icon_black.png", CmdOrCtrl + 'N')
    val newConsoleAction = createComponentFactoryAction("Console", "menu_icons/Terminal.png")
    val newDocViewerAction = createComponentFactoryAction("Document viewer", "menu_icons/Copy.png")

    val toggleBottomDock = createAction(
        name = "Bottom dock",
        description = "Show/hide bottom panels",
        iconPath = "menu_icons/Tools.png"
    ) {
        SimbrainDesktop.bottomDockSplitter.toggleDock(WorkspacePreferences.bottomDockSize)
    }

    val toggleInfoDock = desktopPane.createAction(
        name = "Show/hide info panel",
        description = "Toggle visibility of info panel",
        iconPath = "menu_icons/Info.png",
        initBlock = {
            isEnabled = false
            SimbrainDesktop.workspace.infoDoc.events.textChanged.on {
                isEnabled = it.isNotEmpty()
                if (it.isEmpty()) {
                    SimbrainDesktop.sideDockSplitter.hideDock()
                }
            }
        },
    ) {
        SimbrainDesktop.sideDockSplitter.toggleDock()
    }

    val clearWorkspaceAction = desktopPane.createAction(
        name = "Clear desktop",
        description = "Remove all windows from the desktop",
        keyboardShortcut = CmdOrCtrl + 'K',
        coroutineScope = workspace
    ) {
        SimbrainDesktop.clearDesktop()
    }

    val openWorkspaceAction = desktopPane.createAction(
        iconPath = "menu_icons/Open.png",
        name = "Open workspace file (.zip)...",
        description = "Open a workspace file from .zip (Cmd/Ctrl-O)",
        coroutineScope = workspace
    ) {
        SimbrainDesktop.openWorkspace()
    }

    val saveWorkspaceAction = desktopPane.createAction(
        iconPath = "menu_icons/Save.png",
        name = "Save workspace",
        description = "Save current workspace file",
        keyboardShortcut = CmdOrCtrl + 'S',
        coroutineScope = workspace
    ) {
        SimbrainDesktop.save()
    }

    private val saveWorkspaceAsAction = desktopPane.createAction(
        iconPath = "menu_icons/Save.png",
        name = "Save workspace as...",
        description = "Save current workspace file as .zip (Cmd/Ctrl-Shift-S)",
        coroutineScope = workspace
    ) {
        SimbrainDesktop.saveAs()
    }

    val quitWorkspaceAction = desktopPane.createAction(
        name = "Quit Simbrain",
        description = "Quit Simbrain (Cmd/Ctrl-Q)",
        keyboardShortcut = CmdOrCtrl + 'Q',
        coroutineScope = workspace
    ) {
        SimbrainDesktop.quit(false)
    }

    val iterateAction = desktopPane.createAction(
        iconPath = "menu_icons/Step.png",
        name = "Iterate workspace",
        description = "Iterate workspace once (spacebar)",
        initBlock = {
            workspace.updater.events.runStarted.on { isEnabled = false }
            workspace.updater.events.runFinished.on { isEnabled = true }
        },
        coroutineScope = workspace
    ) {
        workspace.iterateSuspend()
    }

    val runAction = desktopPane.createAction(
        iconPath = "menu_icons/Play.png",
        name = "Run",
        description = "Run workspace (F6)",
        coroutineScope = workspace
    ) {
        workspace.run()
    }

    val stopAction = desktopPane.createAction(
        iconPath = "menu_icons/Stop.png",
        name = "Stop",
        description = "Stop workspace (Esc)",
        coroutineScope = workspace
    ) {
        // A repeated press with the iteration still in flight means it is stuck in a suspend
        // attribute; escalate to cancelling it
        if (!workspace.updater.isRunning && workspace.updater.hasActiveIteration) {
            workspace.stopNow()
        } else {
            workspace.stop()
        }
    }

    val openCouplingManagerAction = desktopPane.createAction(
        iconPath = "menu_icons/Link.png",
        name = "Open coupling manager...",
        description = "Open workspace coupling manager.",
        coroutineScope = workspace
    ) {
        DesktopCouplingManager(SimbrainDesktop).displayInDialog(title = "Couplings") {  }
    }

    val openCouplingListAction = desktopPane.createAction(
        iconPath = "menu_icons/Table.png",
        name = "Open coupling list...",
        description = "Open list of workspace couplings.",
        coroutineScope = workspace
    ) {
        CouplingListPanel(SimbrainDesktop, SimbrainDesktop.workspace.couplings).displayInDialog {  }
    }

    val resetOnboardingWindows = desktopPane.createAction(
        name = "Reset onboarding windows",
        description = "All onboarding windows will be reset to appear again.",
    ) {
        WorkspacePreferences.clearAllPopupSuppressions()
    }


    val showNetworkPreferencesAction = desktopPane.createAction(
        name = "Network preferences...",
        description = "Set default properties that apply to all networks in the Simbrain workspace.",
    ) {
        getPreferenceDialog(NetworkPreferences).display()
    }

    val showOdorWorldPreferencesAction = desktopPane.createAction(
        name = "Odor world preferences...",
        description = "Set default properties that apply to all odor worlds in the Simbrain workspace.",
    ) {
        getPreferenceDialog(OdorWorldPreferences).display()
    }

    val showWorkspacePreferencesAction = createAction(
        name = "Workspace preferences...",
        description = "Set default properties that apply to all networks in the Simbrain workspace.",
    ) {
        getPreferenceDialog(WorkspacePreferences).display()
    }

    val showUpdaterDialog = desktopPane.createAction(
        iconPath = "menu_icons/Sequence.png",
        name = "Edit update sequence...",
        description = "Edit workspace update actions",
        coroutineScope = workspace
    ) {
        WorkspaceUpdateManagerPanel(workspace).displayInDialog {  }
    }

    val repositionAllWindowsAction = desktopPane.createAction(
        name = "Gather windows",
        description = "Repositions and resize all windows. Useful when windows get \"lost\" offscreen.",
        coroutineScope = workspace
    ) {
        SimbrainDesktop.repositionAllWindows()
    }

    val resizeAllWindowsAction = desktopPane.createAction(
        name = "Resize windows",
        description = "Resize all windows on screen so they fit on the current SimbrainDesktop. Useful when windows get \"lost\" offscreen.",
        coroutineScope = workspace
    ) {
        SimbrainDesktop.resizeAllWindows()
    }

    val showComponentBoundsAction = desktopPane.createAction(
        name = "Show component bounds...",
        description = "Show locations and sizes of all workspace components"
    ) {
        showMessageDialog(SimbrainDesktop.getComponentBoundsString(), "Component Bounds")
    }

    val runControlActions = listOf(runAction, stopAction)

    val openSaveWorkspaceActions = listOf(openWorkspaceAction, saveWorkspaceAction, saveWorkspaceAsAction)

    fun createComponentFactoryAction(
        name: String,
        iconPath: String,
        keyboardShortcut: KeyCombination? = null
    ): Action {
        return desktopPane.createAction(
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
        createComponentFactoryAction("Bar chart", "menu_icons/BarChart.png"),
        createComponentFactoryAction("Heat map", "menu_icons/HeatMap.png"),
        createComponentFactoryAction("Histogram", "menu_icons/histogram.png"),
        createComponentFactoryAction("Pie chart", "menu_icons/PieChart.png"),
        createComponentFactoryAction("Pixel plot", "menu_icons/PixelPlot.png"),
        createComponentFactoryAction("Projection plot", "menu_icons/CubeShadow.png"),
        createComponentFactoryAction("Raster plot", "menu_icons/RasterPlot.png"),
        createComponentFactoryAction("Time series", "menu_icons/TimeSeries.png")
    )
    val newWorldActions = listOf(
        createComponentFactoryAction("Data world", "menu_icons/TableBold.png"),
        createComponentFactoryAction("Odor world", "menu_icons/mouse_icon.png"),
        createComponentFactoryAction("Image world", "menu_icons/camera.png"),
        createComponentFactoryAction("Text world", "menu_icons/Text.png"),
        //createComponentFactoryAction("Sound world", "menu_icons/speaker.png")
    )

    fun <T: WorkspaceComponent> createImportAction(desktopComponent: DesktopComponent<T>) = desktopComponent.createAction(
        name = "Import from xml...",
        iconPath = "menu_icons/import.png",
        description = "Import from xml",
        keyboardShortcut = CmdOrCtrl + 'O',
        coroutineScope = workspace
    ) {
        desktopComponent.showImportDialog()
    }

    fun <T: WorkspaceComponent> createExportAction(desktopComponent: DesktopComponent<T>) = desktopComponent.createAction(
        name = "Export to xml...",
        iconPath = "menu_icons/export.png",
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
            val newTitle = showInputDialog("Rename component", desktopComponent.title)
            if (newTitle != null) {
                desktopComponent.title = newTitle
                workspaceComponent.name = newTitle
            }
        }

    private fun <T: WorkspaceComponent> createCoupledPlotAction(
        producer: Producer,
        plotType: String,
        objectName: String,
        iconPath: String,
        description: String = "Create coupled ${plotType.lowercase()}",
        componentCreator: (componentName: String) -> T,
        consumerProvider: CouplingManager.(T) -> Consumer
    ) = desktopPane.createAction(
        name = "${plotType} of $objectName".lowercase().replaceFirstChar { it.uppercase() },
        iconPath = iconPath,
        description = description,
        coroutineScope = workspace
    ) {
        val component = componentCreator("${plotType} of $objectName".lowercase().replaceFirstChar { it.uppercase() })
        workspace.addWorkspaceComponent(component)
        with(workspace.couplingManager) {
            producer couple consumerProvider(component)
        }
    }

    @JvmOverloads
    fun createCoupledProjectionPlotAction(producer: Producer, objectName: String, plotType: String = "Projection plot") = createCoupledPlotAction(
        producer = producer,
        plotType = plotType,
        objectName = objectName,
        iconPath = "menu_icons/CubeShadow.png",
        componentCreator = { name -> ProjectionComponent(name) },
        consumerProvider = {
            it.getConsumer("addPoint")
        }
    )

    @JvmOverloads
    fun createCoupledTimeSeriesPlotAction(producer: Producer, objectName: String, plotType: String = "Time series plot") = createCoupledPlotAction(
        producer = producer,
        plotType = plotType,
        objectName = objectName,
        iconPath = "menu_icons/TimeSeries.png",
        componentCreator = { name -> TimeSeriesPlotComponent(name) },
        consumerProvider = {
            it.model.getConsumer(TimeSeriesModel::setValues)
        }
    )

    fun createCoupledTimeSeriesPlotAction(producers: List<Producer>) = desktopPane.createAction(
        name = "Time series plot of ${producers.size} ${producers.map { it.baseObject::class.simpleName }.toSet().joinToString(", ")}${if (producers.size > 1) "s" else ""}",
        iconPath = "menu_icons/TimeSeries.png",
        description = "Create coupled time series plot",
        coroutineScope = workspace
    ) {
        val component = TimeSeriesPlotComponent("Time series plot of ${producers.size} ${producers.map { it.baseObject::class.simpleName }.toSet().joinToString(", ")}${if (producers.size > 1) "s" else ""}")
        workspace.addWorkspaceComponent(component)
        producers.forEach { producer ->
            with(workspace.couplingManager) {
                // The component names the series from the producing side once coupled, and keeps them current
                val timeSeries = component.addTimeSeries(producer.simpleDescription)
                producer couple timeSeries.getConsumer(TimeSeriesModel.TimeSeries::setValue)
            }
        }
    }

    @JvmOverloads
    fun createCoupledHistogramPlotAction(producer: Producer, objectName: String, plotType: String = "Histogram plot") = createCoupledPlotAction(
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
    fun createCoupledPieChartAction(producer: Producer, objectName: String, plotType: String = "Pie chart") = createCoupledPlotAction(
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
    fun createCoupledBarChartAction(producer: Producer, objectName: String, plotType: String = "Bar chart") = createCoupledPlotAction(
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
    fun createCoupledRasterPlotAction(producer: Producer, objectName: String, plotType: String = "Raster plot") = createCoupledPlotAction(
        producer = producer,
        plotType = plotType,
        objectName = objectName,
        iconPath = "menu_icons/RasterPlot.png",
        componentCreator = { name -> RasterPlotComponent(name) },
        consumerProvider = {
            it.model.rasterConsumerList.first().getConsumer(RasterModel.RasterConsumer::setValues)
        }
    )

    @JvmOverloads
    fun createCoupledHeatMapAction(producer: Producer, objectName: String, plotType: String = "Heat map") = createCoupledPlotAction(
        producer = producer,
        plotType = plotType,
        objectName = objectName,
        iconPath = "menu_icons/HeatMap.png",
        componentCreator = { name -> HeatMapComponent(name) },
        consumerProvider = {
            it.model.getConsumer(HeatMapModel::setValues)
        }
    )

    @JvmOverloads
    fun createCoupledPixelPlotAction(producer: Producer, objectName: String, plotType: String = "Pixel plot") = createCoupledPlotAction(
        producer = producer,
        plotType = plotType,
        objectName = objectName,
        iconPath = "menu_icons/grid.png",
        componentCreator = { name -> PixelPlotComponent(name) },
        consumerProvider = {
            it.pixelPlot.getConsumer(PixelPlot::setBrightness)
        }
    )

    fun createCoupledDataWorldAction(name: String = "Record data", producer: Producer, sourceName: String, numCols: Int) = desktopPane.createAction(
        name = name,
        iconPath = "menu_icons/Table.png",
        coroutineScope = workspace
    ) {
        val component = DataWorldComponent(sourceName, DataWorld(cols = numCols))
        workspace.addWorkspaceComponent(component)
        with(workspace.couplingManager) {
            producer couple component.dataWorld.getConsumer(DataWorld::setCurrentNumericRow)
        }
    }

    @JvmOverloads
    fun createCoupledPlotMenu(producer: Producer, objectName: String, menuTitle: String = "Couple plots"): JMenu {
        val menu = JMenu(menuTitle).apply {
            icon = Icons.small("menu_icons/TimeSeries.png")
        }
        menu.add(createCoupledBarChartAction(producer, objectName))
        menu.add(createCoupledHeatMapAction(producer, objectName))
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
    fun createImageInput(consumer: Consumer, numUnits: Int, menuTitle: String = "Add image world", postActionBlock:
        suspend (ImageWorldComponent) -> Unit = {}) = desktopPane.createAction(
        name = menuTitle,
        iconPath = "menu_icons/photo.png",
        description = "Create image input",
        coroutineScope = workspace
    ) {
        val component = ImageWorldComponent("Image input for ${consumer.baseObject.id}")
        val length = ceil(sqrt(numUnits.toDouble())).toInt()
        workspace.addWorkspaceComponent(component)
        component.world.resetImageAlbum(length, length)
        val producer = component.world.imagePipelineCollection.currentPipeline.getProducer(ImageProcessingPipeline::brightness)
        with(workspace.couplingManager) {
            producer couple consumer
        }
        postActionBlock(component)
    }

    /**
     * Create an image world coupled to a single channel of a [TensorLayer].
     * The image world is created at the tensor's exact width × height.
     * Couples pipeline brightness → channel consumer.
     */
    fun createTensorChannelImageInput(
        tensorLayer: TensorLayer,
        channelIndex: Int,
        menuTitle: String = "Channel $channelIndex"
    ) = desktopPane.createAction(
        name = menuTitle,
        iconPath = "menu_icons/photo.png",
        description = "Create image input for channel $channelIndex",
        coroutineScope = workspace
    ) {
        val component = ImageWorldComponent("Image input for ${tensorLayer.id} Ch$channelIndex")
        workspace.addWorkspaceComponent(component)
        component.world.resetImageAlbum(tensorLayer.shape.width, tensorLayer.shape.height)
        val producer = component.world.imagePipelineCollection.currentPipeline
            .getProducer(ImageProcessingPipeline::brightness)
        val consumer = tensorLayer.channelContainers[channelIndex]
            .getConsumer(TensorLayer.ChannelContainer::setValues)
        with(workspace.couplingManager) {
            producer couple consumer
        }
        tensorLayer.isClamped = true
    }

    /**
     * Create an image world coupled to all 3 channels of a [TensorLayer] via RGB.
     * The image world is created at the tensor's exact width × height.
     * Couples pipeline rgbActivations → tensor setActivations.
     */
    fun createTensorRgbImageInput(
        tensorLayer: TensorLayer,
        menuTitle: String = "RGB (all 3 channels)"
    ) = desktopPane.createAction(
        name = menuTitle,
        iconPath = "menu_icons/photo.png",
        description = "Create RGB image input",
        coroutineScope = workspace
    ) {
        val component = ImageWorldComponent("RGB image input for ${tensorLayer.id}")
        workspace.addWorkspaceComponent(component)
        component.world.resetImageAlbum(tensorLayer.shape.width, tensorLayer.shape.height)
        val producer = component.world.imagePipelineCollection.currentPipeline
            .getProducer(ImageProcessingPipeline::rgbActivations)
        with(workspace.couplingManager) {
            val consumer = tensorLayer.getConsumer(tensorLayer::activations)
            producer couple consumer
        }
        tensorLayer.isClamped = true
        tensorLayer.rgbComposite = true
    }

}
