package org.simbrain.console

import org.simbrain.util.genericframe.GenericFrame
import org.simbrain.workspace.gui.DesktopComponent
import org.simbrain.workspace.gui.SimbrainDesktop
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JMenu
import javax.swing.JMenuBar

/**
 * A desktop window containing a [CommandConsolePanel].
 */
class ConsoleDesktopComponent(frame: GenericFrame, component: ConsoleComponent) :
    DesktopComponent<ConsoleComponent>(frame, component) {

    private val consolePanel = CommandConsolePanel(component.workspace, SimbrainDesktop, inWorkspaceWindow = true)

    override fun onClosed() {
        consolePanel.close()
    }

    init {
        preferredSize = Dimension(500, 400)
        layout = BorderLayout()
        add(consolePanel, BorderLayout.CENTER)

        val actions = SimbrainDesktop.actionManager
        parentFrame.setJMenuBar(JMenuBar().apply {
            add(JMenu("File").apply {
                add(actions.createRenameAction(this@ConsoleDesktopComponent))
                addSeparator()
                add(actions.createCloseAction(this@ConsoleDesktopComponent))
            })
        })
    }
}
