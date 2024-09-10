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
package org.simbrain.network.desktop

import org.simbrain.network.NetworkComponent
import org.simbrain.network.gui.*
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.util.genericframe.GenericFrame
import org.simbrain.workspace.gui.DesktopComponent
import org.simbrain.workspace.gui.SimbrainDesktop.actionManager
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JMenu
import javax.swing.JMenuBar

/**
 * Network desktop component. An extension of the Gui component for this class
 * which is used in the Simbrain desktop.
 */
class NetworkDesktopComponent(frame: GenericFrame, component: NetworkComponent) :
    DesktopComponent<NetworkComponent>(frame, component) {

    val networkPanel = NetworkPanel(component)

    fun createFileMenu(): JMenu {
        val fileMenu = JMenu("File")
        fileMenu.add(actionManager.createImportAction(this))
        fileMenu.add(actionManager.createExportAction(this))
        fileMenu.add(actionManager.createRenameAction(this))
        fileMenu.addSeparator()
        fileMenu.add(networkPanel.networkActions.showNetworkUpdaterDialog);
        fileMenu.addSeparator()
        fileMenu.add(networkPanel.networkActions.showNetworkDefaultsAction);
        fileMenu.addSeparator()
        fileMenu.add(networkPanel.networkActions.showNetworkPropertiesAction);
        fileMenu.addSeparator()
        fileMenu.add(actionManager.createCloseAction(this))
        return fileMenu
    }

    override fun close() {
        super.close()
        NetworkPreferences.unregisterChangeListener(networkPanel.preferenceLoader)
    }

    companion object {
        /**
         * Default height.
         */
        private const val DEFAULT_HEIGHT = 450

        /**
         * Default width.
         */
        private const val DEFAULT_WIDTH = 450

        /**
         * If a synapse group has more than this many synapses and does not have
         * "compression" turned on, show user a warning.
         */
        private const val saveWarningThreshold = 200
    }

    /**
     * Create a new network frame.
     *
     * @param frame     frame of network
     * @param component network component
     */
    init {
        this.preferredSize = Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT)

        // component.setCurrentFile(currentFile);

        // Place networkPanel in a buffer so that toolbars don't get in the way
        // of canvas elements
        layout = BorderLayout()

        // Put it all together
        add("Center", networkPanel)

        JMenuBar().apply {
            parentFrame.jMenuBar = this
            add(createFileMenu())
            add(networkPanel.editMenu)
            add(networkPanel.insertMenu)
            add(networkPanel.actionMenu)
            add(networkPanel.viewMenu)
            add(networkPanel.helpMenu)
        }

        // Toggle the network panel's visiblity if the workspace component is
        // set to "gui off"
        component.events.guiToggled.on {
            networkPanel.guiOn = workspaceComponent.isGuiOn
        }
    }

}