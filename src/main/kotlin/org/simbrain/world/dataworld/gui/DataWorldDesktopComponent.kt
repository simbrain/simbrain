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
package org.simbrain.world.dataworld.gui

import org.simbrain.util.createAction
import org.simbrain.util.createEditorDialog
import org.simbrain.util.display
import org.simbrain.util.genericframe.GenericFrame
import org.simbrain.util.table.*
import org.simbrain.util.widgets.ShowHelpAction
import org.simbrain.workspace.gui.DesktopComponent
import org.simbrain.workspace.gui.SimbrainDesktop.actionManager
import org.simbrain.world.dataworld.DataWorld
import org.simbrain.world.dataworld.DataWorldComponent
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem

/**
 * **ReaderComponentDesktopGui** is the gui view for the reader world.
 */
class DataWorldDesktopComponent(frame: GenericFrame, val component: DataWorldComponent) :
    DesktopComponent<DataWorldComponent>(frame, component) {

    private val menuBar = JMenuBar()

    private val file = JMenu("File")

    private val edit = JMenu("Edit")

    private val preferences = JMenuItem("Preferences")

    private val help = JMenu("Help")

    private val helpItem = JMenuItem("Reader Help")

    private val tablePanel: SimbrainTablePanel = SimbrainTablePanel(
        component.dataWorld.dataModel, false
    )

    private val dataWorld: DataWorld = component.dataWorld

    init {

        this.preferredSize = Dimension(250, 400)

        // Menus
        addMenuBar()

        // Main panel
        tablePanel.addAction(tablePanel.table.importCsv)
        tablePanel.addAction(tablePanel.table.exportCsv(component.name))
        tablePanel.addAction(tablePanel.table.fillAction)
        tablePanel.addAction(tablePanel.table.randomizeAction)
        tablePanel.addAction(tablePanel.table.showBoxPlotAction)
        tablePanel.addAction(tablePanel.table.showHistogramAction)
        tablePanel.addAction(tablePanel.table.createShowMatrixPlotAction())
        // TODO: Add more actions
        add(tablePanel)
        frame.pack()

        // Force component to fill up parent panel
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                val component = e.component
                tablePanel.preferredSize = Dimension(component.width, component.height)
                tablePanel.revalidate()
            }
        })

        parentFrame.pack()
    }

    /**
     * Adds menu bar to the top of DataWorldComponent.
     */
    private fun addMenuBar() {

        // File Menu
        menuBar.add(file)
        file.add(actionManager.createImportAction(this))
        file.add(actionManager.createExportAction(this))
        file.addSeparator()
        file.add(createAction(name = "Preferences...") {
            component.dataWorld.createEditorDialog().display()
        })
        file.addSeparator()
        file.add(actionManager.createRenameAction(this))
        file.addSeparator()
        file.add(actionManager.createCloseAction(this))


        // Help Menu
        menuBar.add(help)
        val helpAction = ShowHelpAction("Pages/Worlds/SoundWorld/SoundWorld.html")
        helpItem.action = helpAction
        help.add(helpItem)

        // Add menu
        parentFrame.jMenuBar = menuBar
    }

}