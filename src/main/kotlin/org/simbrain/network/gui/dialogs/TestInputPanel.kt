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
package org.simbrain.network.gui.dialogs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.groups.NeuronGroup
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.trainer.DataPanel
import org.simbrain.util.createAction
import org.simbrain.util.math.NumericMatrix
import org.simbrain.util.table.NumericTable
import org.simbrain.util.table.SimbrainJTable
import java.beans.PropertyChangeEvent
import javax.swing.*

/**
 * Panel for sending inputs from a table to a network. The action that calls
 * this class provides the input neurons and network panel from which the action
 * gets the network to be updated.
 *
 * @author Jeff Yoshimi
 * @author Lam Nguyen
 */
class TestInputPanel private constructor(
    networkPanel: NetworkPanel?,
    inputNeurons: List<Neuron>,
    dataHolder: NumericMatrix
) : DataPanel(inputNeurons, dataHolder, 5, "Test Inputs"), CoroutineScope {

    @Transient
    private var job = SupervisorJob()

    @Transient
    override var coroutineContext = Dispatchers.Default + job

    /**
     * Network panel.
     */
    private val networkPanel: NetworkPanel

    /**
     * True when iteration mode is on.
     */
    private var iterationMode = true

    /**
     * Button used to advance row. Disabled when iteration mode is on.
     */
    private var advance: JButton? = null

    /**
     * This is the network that should be updated whenever the input neurons are
     * updated. If null, update the whole network
     */
    private var network: Network? = null

    /**
     * Reference to neuron group for cases when that is what's being edited.
     */
    private var neuronGroup: NeuronGroup? = null
    // TODO
    // LMSNetwork lms;
    // public TestInputPanel(NetworkPanel networkPanel, LMSNetwork lms) {
    //     super(lms.getInputData());
    //     this.lms = lms;
    //     if (networkPanel == null) {
    //         throw new IllegalArgumentException("networkPanel must not be null");
    //     }
    //     this.networkPanel = networkPanel;
    //     initTestInputPanel();
    // }
    /**
     * Initiate the test network panel using the network panel.
     */
    private fun initTestInputPanel() {
        network = networkPanel.network
        (table.data as NumericTable).isIterationMode = iterationMode
        val test = JButton(testRowAction)
        advance = JButton(advanceRowAction)
        val testTable = JButton(testTableAction)
        val iterationCheckBox = JCheckBox(iterationModeAction)
        iterationCheckBox.isSelected = iterationMode
        toolbars.add(table.toolbarEditRows)
        val testToolBar = JToolBar()
        testToolBar.add(test)
        testToolBar.add(advance)
        testToolBar.add(testTable)
        testToolBar.add(iterationCheckBox)
        toolbars.add(testToolBar)
    }

    /**
     * Action for advancing a row to be tested.
     */
    private val advanceRowAction = createAction(
        iconPath = "menu_icons/plus.png",
        description = "Advance row"
    ) {
        advanceRow()
    }

    private val testRowAction = createAction(
        iconPath = "menu_icons/Step.png",
        description = "Test row"
    ) {
        testRow()
    }

    private val iterationModeAction = createAction(
        name = "Iteration mode"
    ) {
        if (iterationMode) {
            iterationMode = false
            advance!!.isEnabled = true
        } else {
            iterationMode = true
            advance!!.isEnabled = false
        }
    }

    private val testTableAction = createAction(
        iconPath = "menu_icons/Play.png",
        description = "Test table"
    ) {
        testTable()
    }

    /**
     * Construct the panel using a reference to a class that has a double array.
     * Changes to the table will change the data in that class.
     *
     * @param networkPanel networkPanel, must not be null
     * @param inputNeurons input neurons of the network to be tested
     * @param dataHolder   the class whose data should be edited.
     */
    init {
        requireNotNull(networkPanel) { "networkPanel must not be null" }
        this.dataHolder = dataHolder
        this.networkPanel = networkPanel
        initTestInputPanel()
    }

    /**
     * Advances the row to test.
     */
    private fun advanceRow() {
        (table.data as NumericTable).updateCurrentRow()
        table.updateRowSelection()
        table.scrollRectToVisible(table.getCellRect((table.data as NumericTable).currentRow, table.columnCount, true))
    }

    /**
     * Test the selected row.
     */
    private fun testRow() {
        var testRow = (table.data as NumericTable).currentRow
        if (testRow >= (table.data as NumericTable).rowCount) {
            testRow = 0
        }
        table.updateRowSelection()

        // TODO: Replace with explicit boolean
        if (inputNeurons == null) {
            // lms.getNAList().get(0).setValues(((NumericTable) table.getData()).getVectorCurrentRow());
        } else {
            for (j in inputNeurons.indices) {
                inputNeurons[j].forceSetActivation((table.data as NumericTable).getLogicalValueAt(testRow, j))
            }
        }
        if (network != null) {
            network!!.update()
        } else {
            inputNeurons[0].network.update()
        }
        if (iterationMode) {
            advanceRow()
        }
    }

    /**
     * Advance through the entire table and test each row.
     */
    private fun testTable() {
        for (j in 0 until (table.data as NumericTable).rowCount) {
            (table.data as NumericTable).currentRow = j
            table.scrollRectToVisible(
                table.getCellRect(
                    (table.data as NumericTable).currentRow,
                    table.columnCount,
                    true
                )
            )
            testRow()
        }
    }

    override fun getTable(): SimbrainJTable {
        return table
    }

    /**
     * Resest the data in this panel.
     *
     * @param data the data to set
     */
    fun setData(data: Array<DoubleArray?>?) {
        if (data != null) {
            (table.data as NumericTable).setData(data)
        }
        if (neuronGroup != null) {
            neuronGroup!!.inputManager.data = data
        }
    }

    companion object {
        @JvmStatic
        fun createTestInputPanel(networkPanel: NetworkPanel, neuronGroup: NeuronGroup): TestInputPanel {
            val tip = createTestInputPanel(networkPanel, neuronGroup.neuronList)
            tip.neuronGroup = neuronGroup
            return tip
        }

        /**
         * Create panel using a network panel and a list of selected neurons for
         * case where no data holder is provided (currently, applying test inputs to
         * an arbitrary set of loose neurons).
         *
         * @param networkPanel networkPanel, must not be null
         * @param inputNeurons input neurons of the network to be tested
         * @return the constructed panel
         */
        @JvmStatic
        fun createTestInputPanel(networkPanel: NetworkPanel, inputNeurons: List<Neuron>): TestInputPanel {
            val dataHolder: NumericMatrix = object : NumericMatrix {
                var dataMatrix = Array(5) { DoubleArray(inputNeurons.size) }
                override fun setData(data: Array<DoubleArray>) {
                    dataMatrix = data
                }

                override fun getData(): Array<DoubleArray> {
                    return dataMatrix
                }
            }
            val panel = TestInputPanel(networkPanel, inputNeurons, dataHolder)
            panel.addPropertyChangeListener { evt: PropertyChangeEvent? ->
                panel.commitChanges() // TODO: More efficient way?
            }
            return panel
        }

        /**
         * Create the test input panel.
         *
         * @param networkPanel networkPanel, must not be null.
         * @param inputNeurons input neurons of the network to be tested.
         * @param dataHolder   the class whose data should be edited.
         * @return the constructed panel.
         */
        @JvmStatic
        fun createTestInputPanel(
            networkPanel: NetworkPanel?,
            inputNeurons: List<Neuron>,
            dataHolder: NumericMatrix
        ): TestInputPanel {
            val panel = TestInputPanel(networkPanel, inputNeurons, dataHolder)
            panel.addPropertyChangeListener { evt: PropertyChangeEvent? ->
                panel.commitChanges() // TODO: More efficient way?
            }
            return panel
        }
    }

}
