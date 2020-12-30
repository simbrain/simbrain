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
import org.simbrain.util.genericframe.GenericFrame
import org.simbrain.workspace.component_actions.CloseAction
import org.simbrain.workspace.component_actions.OpenAction
import org.simbrain.workspace.component_actions.SaveAction
import org.simbrain.workspace.component_actions.SaveAsAction
import org.simbrain.workspace.gui.DesktopComponent
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JOptionPane

/**
 * Network desktop component. An extension of the Gui component for this class
 * which is used in the Simbrain desktop.
 */
class NetworkDesktopComponent(frame: GenericFrame?, component: NetworkComponent) : DesktopComponent<NetworkComponent?>(frame, component) {

    val networkPanel = NetworkPanel(component)

    /**
     * Create and return a new File menu for this Network panel.
     *
     * @return a new File menu for this Network panel
     */
    fun createFileMenu(): JMenu {
        val fileMenu = JMenu("File")
        fileMenu.add(OpenAction(this))
        fileMenu.add(SaveAction(this))
        fileMenu.add(SaveAsAction(this))
        fileMenu.addSeparator()
        fileMenu.add(networkPanel.networkActions.showNetworkUpdaterDialog);
        fileMenu.add(networkPanel.networkActions.showNetworkPreferencesAction);
        fileMenu.addSeparator()
        fileMenu.add(CloseAction(workspaceComponent))
        return fileMenu
    }

    override fun postAddInit() {
        if (parentFrame.jMenuBar == null) {
//            createAndAttachMenus()
            // TODO: watch out! menus are created with constructor now
        }

        // TODO
        //networkPanel.getNetwork().setName(this.getName());
        //
        //// TODO: Below only needs to happen when opening; but currently it
        //// happens also when creating a new network
        //networkPanel.clearPanel();
        //if (networkPanel.getNetwork() != this.getWorkspaceComponent().getNetwork()) {
        //    networkPanel.setNetwork(this.getWorkspaceComponent().getNetwork());
        //}
        //networkPanel.initScreenElements();
        //networkPanel.initGui();
    }

    public override fun closing() {}

    override fun showSaveFileDialog() {
        if (showUncompressedSynapseGroupWarning()) {
            super.showSaveFileDialog()
        }
    }

    override fun save() {
        if (showUncompressedSynapseGroupWarning()) {
            super.save()
        }
    }

    /**
     * If at least one synapse group has a large number of synapses that are not
     * going to be saved using compression, show the user a warning.
     *
     * @return true if the save operation should proceed, false if the save
     * operation should be cancelled.
     */
    private fun showUncompressedSynapseGroupWarning(): Boolean {
        val showPanel = networkPanel.network.synapseGroups.any {
            it.allSynapses.size > saveWarningThreshold && !it.isUseFullRepOnSave
        }
        if (showPanel) {
            val n = JOptionPane.showConfirmDialog(null, "<html><body><p style='width: 200px;'>You are saving at least one large synapse group without compression. " + "It is reccomended that you enable 'optimize as group' in all large synapse groups so that " + "their weight matrices are compressed.   Otherwise the save will take a " + "long time and the saved file will be large. Click Cancel to go ahead with the save, " + "and OK to return to the network and change settings.</body></html>", "Save Warning", JOptionPane.OK_CANCEL_OPTION)
            if (n == JOptionPane.OK_OPTION) {
                return false
            }
        }
        return true
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

        with(networkPanel) {
            JMenuBar().apply {
                add(createFileMenu())
                add(createEditMenu())
                add(insertMenu)
                add(createViewMenu())
                // TODO: todo copied from old code
                // TODO
                //menuBar.add(NetworkScriptMenu.getNetworkScriptMenu(this.getNetworkPanel()));
                // menuBar.add(createAttributeMenu());
                add(this@with.helpMenu)
                parentFrame.jMenuBar = this
            }
        }

        // Toggle the network panel's visiblity if the workspace component is
        // set to "gui off"
        component.events.onGUIToggled(Runnable { networkPanel.guiOn = workspaceComponent!!.isGuiOn })
    }
}