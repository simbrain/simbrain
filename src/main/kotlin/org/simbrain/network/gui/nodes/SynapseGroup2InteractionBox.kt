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
package org.simbrain.network.gui.nodes

import org.simbrain.network.core.Neuron
import org.simbrain.network.core.SynapseGroup2
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.WeightMatrixViewer
import org.simbrain.network.gui.createCouplingMenu
import org.simbrain.util.ResourceManager
import java.awt.event.ActionEvent
import java.util.function.Consumer
import javax.swing.*


class SynapseGroup2InteractionBox(
    networkPanel: NetworkPanel,
    val synapseGroup: SynapseGroup2,
    val synapseGroupNode: SynapseGroup2Node
) : InteractionBox(networkPanel) {


    override fun getPropertyDialog(): JDialog? {
        return null
    // TODO
        // return networkPanel.createSynapseGroupDialog(synapseGroup)
    }

    override fun getModel(): SynapseGroup2? {
        return synapseGroup
    }

    override fun isDraggable(): Boolean {
        return false
    }

    override fun getContextMenu(): JPopupMenu? {
        return getDefaultContextMenu()
    }

    /**
     * Returns default actions for a context menu.
     *
     * @return the default context menu
     */
    protected fun getDefaultContextMenu(): JPopupMenu? {
        val menu = JPopupMenu()

        // Edit
        val editGroup: Action = object : AbstractAction("Edit Synapse Group...") {
            override fun actionPerformed(event: ActionEvent) {
                // TODO
                // val dialog: StandardDialog = networkPanel.createSynapseGroupDialog(synapseGroup!!)
                // dialog.setLocationRelativeTo(null)
                // dialog.pack()
                // dialog.isVisible = true
            }
        }
        menu.add(editGroup)
        menu.add(removeAction)

        // Selection stuff
        menu.addSeparator()
        val selectSynapses: Action = object : AbstractAction("Select Synapses") {
            override fun actionPerformed(event: ActionEvent) {
                selectSynapses()
            }
        }
        menu.add(selectSynapses)
        val selectIncomingNodes: Action = object : AbstractAction("Select Incoming Neurons") {
            override fun actionPerformed(event: ActionEvent) {
                synapseGroup.source.neuronList.forEach(Consumer { obj: Neuron -> obj.select() })
            }
        }
        menu.add(selectIncomingNodes)
        val selectOutgoingNodes: Action = object : AbstractAction("Select Outgoing Neurons") {
            override fun actionPerformed(event: ActionEvent) {
                synapseGroup.target.neuronList.forEach(Consumer { obj: Neuron -> obj.select() })
            }
        }
        menu.add(selectOutgoingNodes)

        // Weight adjustment stuff
        menu.addSeparator()
        //Action adjustSynapses = new AbstractAction("Adjust Synapses...") {
        //    public void actionPerformed(final ActionEvent event) {
        //        selectSynapses();
        // TODO: Check after synapse group code stabilizes for more
        // efficient way.
        // final SynapseAdjustmentPanel synapsePanel =
        // SynapseAdjustmentPanel
        // .createSynapseAdjustmentPanel(getNetworkPanel(),
        // synapseGroup.getAllSynapses());
        // JDialog dialog = new JDialog();
        // dialog.setTitle("Adjust selected synapses");
        // dialog.setContentPane(synapsePanel);
        // dialog.pack();
        // dialog.setLocationRelativeTo(null);
        // dialog.setVisible(true);
        // dialog.addWindowListener(new WindowAdapter() {
        // public void windowClosing(WindowEvent e) {
        // synapsePanel.removeListeners();
        // }
        // });
        //}
        //};
        //menu.add(adjustSynapses);
        menu.add(JMenuItem(showWeightMatrixAction))

        // Freezing actions
        menu.addSeparator()
        setFreezeActionsEnabled()
        menu.add(freezeSynapsesAction)
        menu.add(unfreezeSynapsesAction)

        // Synapse Enabling actions
        menu.addSeparator()
        setSynapseEnablingActionsEnabled()
        menu.add(enableSynapsesAction)
        menu.add(disableSynapsesAction)

        // Synapse Visibility
        menu.addSeparator()
        val tsvCheckBox = JCheckBoxMenuItem()
        val toggleSynapseVisibility: Action = object : AbstractAction("Toggle Synapse Visibility") {
            override fun actionPerformed(event: ActionEvent) {
                synapseGroup.displaySynapses = !synapseGroup.displaySynapses
                tsvCheckBox.isSelected = synapseGroup.displaySynapses
            }
        }
        tsvCheckBox.action = toggleSynapseVisibility
        tsvCheckBox.isSelected = synapseGroup.displaySynapses
        menu.add(tsvCheckBox)

        // Coupling menu
        val couplingMenu: JMenu = networkPanel.networkComponent.createCouplingMenu(synapseGroup)
        menu.addSeparator()
        menu.add(couplingMenu)
        return menu
    }

    /**
     * Select the synapses in this group.
     */
    private fun selectSynapses() {
        // TODO: fix getObjectNodeMap
        // List<SynapseNode> nodes = new ArrayList<SynapseNode>();
        // for (Synapse synapse : synapseGroup.getExcitatorySynapses()) {
        //     nodes.add((SynapseNode) getNetworkPanel().getObjectNodeMap().get(synapse));
        //
        // }
        // for (Synapse synapse : synapseGroup.getInhibitorySynapses()) {
        //     nodes.add((SynapseNode) getNetworkPanel().getObjectNodeMap().get(synapse));
        //
        // }
        // getNetworkPanel().clearSelection();
        // getNetworkPanel().setSelection(nodes);
    }

    /**
     * Action for showing the weight matrix for this neuron group.
     */
    var showWeightMatrixAction: Action = object : AbstractAction() {
        // Initialize
        init {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/grid.png"))
            putValue(NAME, "Show Weight Matrix")
            putValue(SHORT_DESCRIPTION, "Show Weight Matrix")
        }

        override fun actionPerformed(event: ActionEvent) {
            val sourceNeurons = synapseGroup.source.neuronList
            val targetNeurons = synapseGroup.target.neuronList
            val panel = WeightMatrixViewer.getWeightMatrixPanel(
                WeightMatrixViewer(
                    sourceNeurons, targetNeurons,
                    networkPanel
                )
            )
            networkPanel.displayPanel(panel, "Edit weights")
        }
    }

    /**
     * Action for editing the group name.
     */
    protected var renameAction: Action = object : AbstractAction("Rename Group...") {
        override fun actionPerformed(event: ActionEvent) {
            val newName = JOptionPane.showInputDialog("Name:", synapseGroup!!.label)
            synapseGroup.label = newName
        }
    }

    /**
     * Action for removing this group
     */
    protected var removeAction: Action = object : AbstractAction() {
        init {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/RedX_small.png"))
            putValue(NAME, "Remove Group...")
            putValue(SHORT_DESCRIPTION, "Remove synapse group...")
        }

        override fun actionPerformed(arg0: ActionEvent) {
            synapseGroup!!.delete()
        }
    }

    /**
     * Sets whether the freezing actions are enabled based on whether the
     * synapses are all frozen or not.
     *
     *
     * If all synapses are frozen already, then "freeze synapses" is disabled.
     *
     *
     * If all synapses are unfrozen already, then "unfreeze synapses" is
     * disabled.
     */
    private fun setFreezeActionsEnabled() {
        try {
            // TODO
            // freezeSynapsesAction.setEnabled(!synapseGroup.isFrozen(Polarity.BOTH));
            // unfreezeSynapsesAction.setEnabled(synapseGroup.isFrozen(Polarity.BOTH));
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

    /**
     * Action for freezing synapses
     */
    protected var freezeSynapsesAction: Action = object : AbstractAction() {
        init {
            // putValue(SMALL_ICON, ResourceManager.getImageIcon("Clamp.png"));
            putValue(NAME, "Freeze Synapses")
            putValue(SHORT_DESCRIPTION, "Freeze all synapses in this group (prevent learning)")
        }

        override fun actionPerformed(arg0: ActionEvent) {
            // synapseGroup.setFrozen(true, Polarity.BOTH);
        }
    }

    /**
     * Action for unfreezing synapses
     */
    protected var unfreezeSynapsesAction: Action = object : AbstractAction() {
        init {
            // putValue(SMALL_ICON, ResourceManager.getImageIcon("Clamp.png"));
            putValue(NAME, "Unfreeze Synapses")
            putValue(SHORT_DESCRIPTION, "Unfreeze all synapses in this group (allow learning)")
        }

        override fun actionPerformed(arg0: ActionEvent) {
            // synapseGroup.setFrozen(false, Polarity.BOTH);
        }
    }

    /**
     * Sets whether the synapse-enabling actions are enabled based on whether
     * the synapses themselves are all enabled or not. Of course, "enable" means
     * two things here, (1) a property of synapses whereby the let current pass
     * or not and (2) a property of swing actions where being disabled means
     * being grayed out and unusable.
     *
     * If all synapses are enabled already, then the "enable synapses" action is
     * disabled.
     *
     * If all synapses are disabled already, then the "disable synapses" actions
     * is disabled.
     */
    private fun setSynapseEnablingActionsEnabled() {
        // enableSynapsesAction.setEnabled(!synapseGroup.isEnabled(Polarity.BOTH));
        // disableSynapsesAction.setEnabled(synapseGroup.isEnabled(Polarity.BOTH));
    }

    /**
     * Action for enabling synapses
     */
    protected var enableSynapsesAction: Action = object : AbstractAction() {
        init {
            // putValue(SMALL_ICON, ResourceManager.getImageIcon("Clamp.png"));
            putValue(NAME, "Enable Synapses")
            putValue(
                SHORT_DESCRIPTION,
                "Enable all synapses in this group (allow activation " + "to pass through synapses)"
            )
        }

        override fun actionPerformed(arg0: ActionEvent) {
            // synapseGroup.setEnabled(true, Polarity.BOTH);
        }
    }

    /**
     * Action for disabling synapses
     */
    protected var disableSynapsesAction: Action = object : AbstractAction() {
        init {
            // putValue(SMALL_ICON, ResourceManager.getImageIcon("Clamp.png"));
            putValue(NAME, "Disable Synapses")
            putValue(
                SHORT_DESCRIPTION,
                "Disable all synapses in this group (don't allow " + "activation to pass through synapses)"
            )
        }

        override fun actionPerformed(arg0: ActionEvent) {
            // synapseGroup.setEnabled(false, Polarity.BOTH);
        }
    }

    override fun getToolTipText(): String {
        return "Synapses: " + synapseGroup.size() + " Density: " + synapseGroup.size()
            .toDouble() / (synapseGroup.source.size() * synapseGroup.target.size())
    }

}