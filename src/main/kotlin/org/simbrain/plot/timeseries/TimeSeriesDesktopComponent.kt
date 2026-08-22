/**
 * Desktop frame for the time series plot. [TimeSeriesModel.recurrenceView] decides what it hosts:
 * just [TimeSeriesPlotPanel], just the per-series [RecurrencePanel] tabs, or both stacked with the
 * recurrence plots below the line chart. A toolbar and a View menu switch focus and the common
 * recurrence options without opening preferences; every control routes through the model plus a
 * propertyChanged fire, so the toolbar, menu, and preferences dialog stay in agreement.
 *
 * In the stacked view the two plots are horizontally aligned: both reserve the same fixed range-axis
 * width, the recurrence colorbar moves to the bottom edge, and the recurrence time axis follows the
 * line chart's domain axis, so time t sits at the same x pixel in both plots.
 *
 * This lives here rather than in the plot panel so trainer dialogs, which embed the panel directly,
 * are unaffected.
 */
package org.simbrain.plot.timeseries

import kotlinx.coroutines.Job
import org.jfree.chart.axis.AxisSpace
import org.jfree.data.general.DatasetChangeListener
import org.simbrain.plot.actions.PlotActionManager
import org.simbrain.util.genericframe.GenericFrame
import org.simbrain.util.swingDispatcher
import org.simbrain.util.widgets.ShowHelpAction
import org.simbrain.workspace.gui.DesktopComponent
import org.simbrain.workspace.gui.SimbrainDesktop
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.text.NumberFormat
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.ButtonGroup
import javax.swing.JComboBox
import javax.swing.JDesktopPane
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JRadioButtonMenuItem
import javax.swing.JSpinner
import javax.swing.JSplitPane
import javax.swing.JTabbedPane
import javax.swing.JToolBar
import javax.swing.SpinnerNumberModel
import javax.swing.SwingUtilities

class TimeSeriesDesktopComponent(frame: GenericFrame, component: TimeSeriesPlotComponent) :
    DesktopComponent<TimeSeriesPlotComponent>(frame, component) {

    private val actionManager = PlotActionManager(this)

    private val plotModel = component.model

    private val timeSeriesPanel = TimeSeriesPlotPanel(plotModel)

    /** Holds whichever layout the current view calls for, so rebuilding never touches the toolbar. */
    private val centerPanel = JPanel(BorderLayout())

    private val recurrenceTabs = JTabbedPane()

    private val recurrencePanels = mutableMapOf<TimeSeriesModel.TimeSeries, RecurrencePanel>()

    private var shownView: RecurrenceView? = null

    private var splitPane: JSplitPane? = null

    private val viewMenuItems = mutableMapOf<RecurrenceView, JRadioButtonMenuItem>()

    private lateinit var viewCombo: JComboBox<RecurrenceView>

    private lateinit var modeCombo: JComboBox<RecurrenceMode>

    private lateinit var thresholdSpinner: JSpinner

    /** True while controls are being written from the model, so their listeners don't echo back. */
    private var syncingControls = false

    /** Whether a tab title pass is already queued, so bursts of data coalesce into one. */
    private val titleSyncQueued = AtomicBoolean(false)

    private val lineChartDomainAxis get() = timeSeriesPanel.chartPanel.chart.xyPlot.domainAxis

    /** Model-side registrations, undone in [close] because the model can outlive this window. */
    private val modelSubscriptions = mutableListOf<Job>()

    private val titleSyncListener = DatasetChangeListener { scheduleTitleSync() }

    init {
        layout = BorderLayout()
        createAttachMenuBar()
        add(createToolbar(), BorderLayout.NORTH)
        add(centerPanel, BorderLayout.CENTER)
        rebuildLayout()
        syncControls()

        modelSubscriptions += plotModel.events.propertyChanged.on(swingDispatcher) {
            rebuildLayout()
            syncControls()
        }
        modelSubscriptions += plotModel.events.timeSeriesAdded.on(swingDispatcher) { if (recurrenceVisible()) rebuildTabs() }
        modelSubscriptions += plotModel.events.timeSeriesRemoved.on(swingDispatcher) { if (recurrenceVisible()) rebuildTabs() }
        // Renames happen in place without an add/remove event; catch them as data flows
        plotModel.dataset.addChangeListener(titleSyncListener)
        // Aligned panels pull the line chart's range on their own refreshes, which auto-range keeps
        // silent; this listener covers the loud changes between refreshes, like a mouse zoom
        lineChartDomainAxis.addChangeListener {
            if (shownView == RecurrenceView.BOTH) recurrencePanels.values.forEach { it.applyAlignedDomain() }
        }
    }

    override fun close() {
        super.close()
        modelSubscriptions.forEach { it.cancel() }
        modelSubscriptions.clear()
        plotModel.dataset.removeChangeListener(titleSyncListener)
        recurrencePanels.values.forEach { it.dispose() }
        recurrencePanels.clear()
        timeSeriesPanel.dispose()
    }

    private fun recurrenceVisible() = plotModel.recurrenceView != RecurrenceView.TIME_SERIES

    private fun rebuildLayout() {
        if (plotModel.recurrenceView == shownView) return
        val previousView = shownView
        // The height the disappearing region actually occupies, read before the layout is torn down
        val removedRegionHeight = when {
            previousView != RecurrenceView.BOTH -> 0
            plotModel.recurrenceView == RecurrenceView.TIME_SERIES -> recurrenceTabs.height + dividerAllowance()
            else -> timeSeriesPanel.height + dividerAllowance()
        }
        shownView = plotModel.recurrenceView
        centerPanel.removeAll()
        when (plotModel.recurrenceView) {
            RecurrenceView.TIME_SERIES -> {
                timeSeriesPanel.chartPanel.chart.xyPlot.fixedRangeAxisSpace = null
                clearSplitMinimums()
                recurrencePanels.values.forEach { it.dispose() }
                recurrencePanels.clear()
                recurrenceTabs.removeAll()
                splitPane = null
                centerPanel.add(timeSeriesPanel, BorderLayout.CENTER)
            }
            RecurrenceView.BOTH -> {
                rebuildTabs()
                val split = JSplitPane(JSplitPane.VERTICAL_SPLIT, timeSeriesPanel, recurrenceTabs).apply {
                    resizeWeight = 0.5
                    isContinuousLayout = true
                    isOneTouchExpandable = true
                }
                // Small minimums so the divider can be dragged freely; preferred sizes are not floors
                timeSeriesPanel.minimumSize = Dimension(100, 100)
                recurrenceTabs.minimumSize = Dimension(100, 100)
                split.addComponentListener(object : ComponentAdapter() {
                    override fun componentResized(e: ComponentEvent) {
                        split.removeComponentListener(this)
                        SwingUtilities.invokeLater { applyFairDivider(split) }
                    }
                })
                splitPane = split
                centerPanel.add(split, BorderLayout.CENTER)
            }
            RecurrenceView.RECURRENCE -> {
                timeSeriesPanel.chartPanel.chart.xyPlot.fixedRangeAxisSpace = null
                clearSplitMinimums()
                rebuildTabs()
                splitPane = null
                centerPanel.add(recurrenceTabs, BorderLayout.CENTER)
            }
        }
        // Entering the stacked view grows the frame toward a comfortable two-plot height, but never
        // past the space the desktop actually has; leaving it gives back the removed region's space.
        // Initial construction (previousView == null) leaves the frame alone so saved bounds win.
        val bounds = parentFrame.bounds
        val targetHeight = when {
            previousView == null -> bounds.height
            plotModel.recurrenceView == RecurrenceView.BOTH ->
                maxOf(bounds.height, minOf(availableFrameHeight() - bounds.y - FRAME_MARGIN, BOTH_VIEW_TARGET_HEIGHT))
            previousView == RecurrenceView.BOTH ->
                (bounds.height - removedRegionHeight).coerceAtLeast(MIN_FRAME_HEIGHT)
            else -> bounds.height
        }
        if (targetHeight != bounds.height) {
            parentFrame.setBounds(bounds.x, bounds.y, bounds.width, targetHeight)
        }
        revalidate()
        repaint()
    }

    private fun dividerAllowance() = splitPane?.dividerSize ?: JSplitPane().dividerSize

    /** Height the frame can occupy: its desktop pane when docked, otherwise the usable screen. */
    private fun availableFrameHeight(): Int {
        val desktopPane = SwingUtilities.getAncestorOfClass(JDesktopPane::class.java, this) as? JDesktopPane
        return desktopPane?.height?.takeIf { it > 0 }
            ?: GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds.height
    }

    /**
     * Place the divider so the two chart areas come out equal, compensating for the legend and
     * button rows above and the tab strip below; preferred sizes deliberately play no part, so
     * neither plot's 400px default acts as a floor that squeezes the other.
     */
    private fun applyFairDivider(split: JSplitPane) {
        val height = split.height
        if (height <= 0) return
        val topChrome = (timeSeriesPanel.height - timeSeriesPanel.chartPanel.height).coerceAtLeast(0)
        val bottomChrome = (recurrenceTabs.selectedComponent as? RecurrencePanel)
            ?.let { (recurrenceTabs.height - it.height).coerceAtLeast(0) } ?: 0
        split.dividerLocation = ((height - split.dividerSize + topChrome - bottomChrome) / 2)
            .coerceIn((height * 0.3).toInt(), (height * 0.7).toInt())
    }

    /**
     * Make the tabs match the model's series one for one, following series identity so a surviving
     * series keeps its panel across reorders and removals.
     */
    private fun rebuildTabs() {
        val selected = recurrenceTabs.selectedComponent
        recurrenceTabs.removeAll()
        val current = plotModel.timeSeriesList.toList()
        recurrencePanels.keys.filter { ts -> current.none { it === ts } }
            .forEach { recurrencePanels.remove(it)?.dispose() }
        current.forEach { ts ->
            val panel = recurrencePanels.getOrPut(ts) { RecurrencePanel(plotModel, ts) }
            recurrenceTabs.addTab(ts.series.key.toString(), panel)
        }
        if (selected != null && recurrenceTabs.indexOfComponent(selected) >= 0) {
            recurrenceTabs.selectedComponent = selected
        }
        val provider: (() -> DomainAlignment)? =
            if (shownView == RecurrenceView.BOTH) ({ currentDomainAlignment() }) else null
        recurrencePanels.values.forEach { it.domainAlignmentProvider = provider }
        revalidate()
        repaint()
    }

    /**
     * The alignment state pulled by aligned recurrence panels: the line chart's current time range
     * and a range-axis space sized to the widest tick label either chart currently needs, kept in
     * step on the line chart's plot as a side effect so both reserve the same width.
     */
    private fun currentDomainAlignment(): DomainAlignment {
        val linePlot = timeSeriesPanel.chartPanel.chart.xyPlot
        val space = sharedRangeAxisSpace()
        if (linePlot.fixedRangeAxisSpace != space) linePlot.fixedRangeAxisSpace = space
        return DomainAlignment(lineChartDomainAxis.range, space)
    }

    /**
     * Range-axis area both aligned charts reserve, measured from the extreme tick labels of the line
     * chart's two axes (its range axis carries values, its domain the times the recurrence axis
     * shows). Fixed axis space bypasses JFreeChart's own label measurement, so wide labels would
     * otherwise be clipped.
     */
    private fun sharedRangeAxisSpace(): AxisSpace {
        val linePlot = timeSeriesPanel.chartPanel.chart.xyPlot
        val metrics = timeSeriesPanel.getFontMetrics(linePlot.rangeAxis.tickLabelFont)
        val format = NumberFormat.getNumberInstance()
        val labelWidth = listOf(
            linePlot.rangeAxis.range.lowerBound, linePlot.rangeAxis.range.upperBound,
            linePlot.domainAxis.range.lowerBound, linePlot.domainAxis.range.upperBound
        ).maxOf { metrics.stringWidth(format.format(it)) }
        return AxisSpace().apply {
            left = maxOf(MIN_ALIGNED_AXIS_WIDTH, labelWidth + AXIS_CHROME_WIDTH).toDouble()
            right = ALIGNED_AXIS_RIGHT_INSET
        }
    }

    private fun clearSplitMinimums() {
        timeSeriesPanel.minimumSize = null
        recurrenceTabs.minimumSize = null
    }

    private fun scheduleTitleSync() {
        if (!recurrenceVisible()) return
        if (titleSyncQueued.compareAndSet(false, true)) {
            SwingUtilities.invokeLater {
                titleSyncQueued.set(false)
                if (!recurrenceVisible()) return@invokeLater
                for (i in 0 until recurrenceTabs.tabCount) {
                    val title = (recurrenceTabs.getComponentAt(i) as? RecurrencePanel)
                        ?.timeSeries?.series?.key?.toString() ?: continue
                    if (recurrenceTabs.getTitleAt(i) != title) recurrenceTabs.setTitleAt(i, title)
                }
            }
        }
    }

    private fun changeSettings(mutate: () -> Unit) {
        if (syncingControls) return
        mutate()
        plotModel.events.propertyChanged.fire()
    }

    private fun createToolbar() = JToolBar().apply {
        isFloatable = false
        viewCombo = JComboBox(RecurrenceView.entries.toTypedArray()).apply {
            selectedItem = plotModel.recurrenceView
            toolTipText = "Which plots the window shows"
            maximumSize = preferredSize
            addActionListener { changeSettings { plotModel.recurrenceView = selectedItem as RecurrenceView } }
        }
        add(viewCombo)
        addSeparator()
        modeCombo = JComboBox(RecurrenceMode.entries.toTypedArray()).apply {
            toolTipText = "How the recurrence plots render distances"
            maximumSize = preferredSize
            addActionListener { changeSettings { plotModel.recurrenceMode = selectedItem as RecurrenceMode } }
        }
        add(modeCombo)
        thresholdSpinner = JSpinner(SpinnerNumberModel(plotModel.recurrenceThreshold, 0.0, 1.0, 0.01)).apply {
            toolTipText = "Recurrence threshold, as a fraction of the largest distance in the window"
            val size = Dimension(70, preferredSize.height)
            preferredSize = size
            maximumSize = size
            addChangeListener { changeSettings { plotModel.recurrenceThreshold = value as Double } }
        }
        add(thresholdSpinner)
    }

    /** Write the model's current settings into the toolbar and menu without echoing back. */
    private fun syncControls() {
        syncingControls = true
        try {
            viewCombo.selectedItem = plotModel.recurrenceView
            viewMenuItems[plotModel.recurrenceView]?.isSelected = true
            modeCombo.selectedItem = plotModel.recurrenceMode
            thresholdSpinner.value = plotModel.recurrenceThreshold
            modeCombo.isEnabled = recurrenceVisible()
            thresholdSpinner.isEnabled = recurrenceVisible() && plotModel.recurrenceMode == RecurrenceMode.THRESHOLD
        } finally {
            syncingControls = false
        }
    }

    companion object {
        private const val MIN_FRAME_HEIGHT = 200

        /** Comfortable total frame height for the stacked view; a taller frame is left as is. */
        private const val BOTH_VIEW_TARGET_HEIGHT = 720

        /** Breathing room kept below the frame when growth is clamped to the desktop. */
        private const val FRAME_MARGIN = 8

        /** Floor for the aligned range-axis area, matching the unaligned charts' typical reservation. */
        private const val MIN_ALIGNED_AXIS_WIDTH = 60

        /** Tick marks, tick-label insets, and the rotated axis title beside the measured label. */
        private const val AXIS_CHROME_WIDTH = 30

        private const val ALIGNED_AXIS_RIGHT_INSET = 8.0
    }

    private fun createAttachMenuBar() {
        val bar = JMenuBar()
        val fileMenu = JMenu("File").apply {
            actionManager.openSavePlotActions.forEach { add(it) }
            addSeparator()
            add(SimbrainDesktop.actionManager.createRenameAction(this@TimeSeriesDesktopComponent))
            addSeparator()
            add(SimbrainDesktop.actionManager.createCloseAction(this@TimeSeriesDesktopComponent))
        }
        val editMenu = JMenu("Edit").apply {
            add(JMenuItem(TimeSeriesPlotActions.getPropertiesDialogAction(timeSeriesPanel)))
        }
        val viewMenu = JMenu("View").apply {
            val group = ButtonGroup()
            RecurrenceView.entries.forEach { view ->
                val item = JRadioButtonMenuItem(view.toString(), view == plotModel.recurrenceView)
                item.addActionListener { changeSettings { plotModel.recurrenceView = view } }
                group.add(item)
                viewMenuItems[view] = item
                add(item)
            }
        }
        val helpMenu = JMenu("Help").apply {
            add(JMenuItem(ShowHelpAction("https://docs.simbrain.net/docs/plots/timeSeries.html")))
        }
        bar.add(fileMenu)
        bar.add(editMenu)
        bar.add(viewMenu)
        bar.add(helpMenu)
        parentFrame.jMenuBar = bar
    }
}
