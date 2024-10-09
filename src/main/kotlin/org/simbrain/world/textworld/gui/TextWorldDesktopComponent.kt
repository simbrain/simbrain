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
package org.simbrain.world.textworld.gui

import org.simbrain.util.genericframe.GenericFrame
import org.simbrain.util.widgets.ShowHelpAction
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.workspace.gui.CouplingMenu
import org.simbrain.workspace.gui.DesktopComponent
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.world.textworld.*
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem

/**
 * **ReaderComponentDesktopGui** is the gui view for the reader world.
 */
class TextWorldDesktopComponent(frame: GenericFrame, component: TextWorldComponent) :
    DesktopComponent<TextWorldComponent>(frame, component) {
    /**
     * Menu Bar.
     */
    private val menuBar = JMenuBar()

    /**
     * File menu for saving and opening world files.
     */
    private val file = JMenu("File")

    /**
     * Edit menu Item.
     */
    private val edit = JMenu("Edit")

    /**
     * Opens user preferences dialog.
     */
    private val preferences = JMenuItem("Preferences")

    /**
     * Opens the help dialog for TextWorld.
     */
    private val help = JMenu("Help")

    /**
     * Help menu item.
     */
    private val helpItem = JMenuItem("Reader Help")

    /**
     * The pane representing the text world.
     */
    val panel: TextWorldPanel

    /**
     * The text world.
     */
    private val world: TextWorld

    /**
     * Creates a new frame of type TextWorld.
     *
     * @param frame
     * @param component
     */
    init {
        world = component.world
        panel = TextWorldPanel(world)
        this.preferredSize = Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT)
        addMenuBar()
        add(panel)
        frame.pack()

        // Force component to fill up parent panel
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                val component = e.component
                panel.preferredSize = Dimension(component.width, component.height)
                panel.revalidate()
            }
        })
        parentFrame.pack()
    }

    /**
     * Adds menu bar to the top of TextWorldComponent.
     */
    private fun addMenuBar() {

        // File Menu
        menuBar.add(file)
        file.add(SimbrainDesktop.actionManager.createImportAction(this))
        file.add(SimbrainDesktop.actionManager.createExportAction(this))
        file.addSeparator()
        file.add(loadTextAction)
        file.addSeparator()
        file.add(SimbrainDesktop.actionManager.createRenameAction(this))
        file.addSeparator()
        file.add(SimbrainDesktop.actionManager.createCloseAction(this))

        // Edit menu
        fun createEditMenu() {
            edit.removeAll()
            preferences.action = world.textWorldPrefs
            edit.add(createShowFindAndReplaceAction())
            edit.addSeparator()
            edit.add(
                SimbrainDesktop.actionManager.createCoupledDataWorldAction(
                    name = "Record Word Embeddings",
                    world.getProducer(TextWorld::currentVector),
                    sourceName = "${world.id} Word Embeddings",
                    world.currentVector.size
                )
            )
            edit.add(
                SimbrainDesktop.actionManager.createCoupledPlotMenu(
                    world.getProducer(TextWorld::currentVector),
                    "${world.id} Word Embeddings",
                )
            )
            edit.addSeparator()
            edit.add(CouplingMenu(workspaceComponent, world))
            edit.addSeparator()
            edit.add(preferences)
        }
        createEditMenu()
        onCouplingAttributesChanged { createEditMenu() }
        menuBar.add(edit)

        // View Menu
        val viewMenu = JMenu("View")
        viewMenu.add(world.viewTokenEmbedding)
        menuBar.add(viewMenu)

        // Help Menu
        menuBar.add(help)
        val helpAction = ShowHelpAction("https://docs.simbrain.net/docs/worlds/textworld.html")
        helpItem.action = helpAction
        help.add(helpItem)

        // Add menu
        parentFrame.jMenuBar = menuBar
    }

    companion object {
        /**
         * Default height.
         */
        private const val DEFAULT_HEIGHT = 250

        /**
         * Default width.
         */
        private const val DEFAULT_WIDTH = 400
    }
}