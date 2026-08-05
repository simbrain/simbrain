package org.simbrain.plot.heatmap

import org.simbrain.plot.actions.PlotActionManager
import org.simbrain.util.genericframe.GenericFrame
import org.simbrain.workspace.gui.DesktopComponent
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.util.widgets.ShowHelpAction
import java.awt.BorderLayout
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem

class HeatMapDesktopComponent(frame: GenericFrame, component: HeatMapComponent) :
    DesktopComponent<HeatMapComponent>(frame, component) {

    private val actionManager = PlotActionManager(this)

    private val heatMapPanel = HeatMapPanel(component.model)

    init {
        layout = BorderLayout()
        add("Center", heatMapPanel)
        createAttachMenuBar()
    }

    private fun createAttachMenuBar() {
        val bar = JMenuBar()
        val fileMenu = JMenu("File").apply {
            actionManager.openSavePlotActions.forEach { add(it) }
            addSeparator()
            add(SimbrainDesktop.actionManager.createRenameAction(this@HeatMapDesktopComponent))
            addSeparator()
            add(SimbrainDesktop.actionManager.createCloseAction(this@HeatMapDesktopComponent))
        }
        val editMenu = JMenu("Edit").apply {
            add(JMenuItem("Preferences...").apply { addActionListener { heatMapPanel.showPropertiesDialog() } })
        }
        val helpMenu = JMenu("Help").apply {
            add(JMenuItem(ShowHelpAction("https://docs.simbrain.net/docs/plots/heatMap.html")))
        }
        bar.add(fileMenu)
        bar.add(editMenu)
        bar.add(helpMenu)
        parentFrame.jMenuBar = bar
    }
}
