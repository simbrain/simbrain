package org.simbrain.plot.projection

import org.simbrain.util.genericframe.GenericFrame
import org.simbrain.workspace.gui.DesktopComponent
import java.awt.Dimension
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem

class ProjectionDesktopComponent2(frame: GenericFrame, component: ProjectionComponent2): DesktopComponent<ProjectionComponent2>(frame, component) {


    val projectionPanel = ProjectionPanel(component.projector)

    init {
        preferredSize = Dimension(500, 400)
        add(projectionPanel)
        frame.jMenuBar = JMenuBar().apply {
            add(JMenu("Edit").apply {
                add(JMenuItem("Preferences...").apply {
                    addActionListener {
                        projectionPanel.showPrefDialog()
                    }
                })
            })
        }
    }

}