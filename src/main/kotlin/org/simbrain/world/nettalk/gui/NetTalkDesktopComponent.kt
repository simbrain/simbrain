package org.simbrain.world.nettalk.gui

import org.simbrain.util.genericframe.GenericFrame
import org.simbrain.util.widgets.ShowHelpAction
import org.simbrain.workspace.gui.DesktopComponent
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.world.nettalk.NetTalkComponent
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem

class NetTalkDesktopComponent(frame: GenericFrame, component: NetTalkComponent) :
    DesktopComponent<NetTalkComponent>(frame, component) {

    val panel = NetTalkPanel(component.nettalk)

    init {
        preferredSize = Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT)
        addMenuBar()
        add(panel)
        frame.pack()

        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                val c = e.component
                panel.preferredSize = Dimension(c.width, c.height)
                panel.revalidate()
            }
        })
        parentFrame.pack()
    }

    private fun addMenuBar() {
        val menuBar = JMenuBar()
        val file = JMenu("File")
        file.add(SimbrainDesktop.actionManager.createImportAction(this))
        file.add(SimbrainDesktop.actionManager.createExportAction(this))
        menuBar.add(file)

        val help = JMenu("Help")
        help.add(JMenuItem(ShowHelpAction("https://docs.simbrain.net/docs/worlds/nettalk.html")))
        menuBar.add(help)

        parentFrame.jMenuBar = menuBar
    }

    companion object {
        private const val DEFAULT_WIDTH = 600
        private const val DEFAULT_HEIGHT = 500
    }
}
