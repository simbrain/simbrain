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

import org.simbrain.network.connections.ConnectionStrategy
import org.simbrain.network.connections.RadialProbabilistic
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.util.SimbrainConstants.Polarity
import org.simbrain.util.displayInDialog
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.ProbabilityDistribution.Randomizer
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.util.widgets.DropDownTriangle
import org.simbrain.util.widgets.DropDownTriangle.UpDirection
import java.awt.*
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*

/**
 * Panel for randomizing inhibitory and excitatory synapses.
 */
class InhibExcRandomizerPanel(var synapses: List<Synapse>) : JPanel() {


    enum class RandBehavior {
        FORCE_ON, DEFAULT, FORCE_OFF
    }

    private val randomizerState: RandBehavior? = null

    private var excitatoryRandomizerPanel: EditableRandomizerPanel? = null

    private var inhibitoryRandomizerPanel: EditableRandomizerPanel? = null

    /**
     * The randomizer for excitatory synapses.
     */
    var exRandomizer: ProbabilityDistribution = NormalDistribution();

    /**
     * The randomizer for inhibitory synapses.
     */
    var inRandomizer: ProbabilityDistribution = NormalDistribution();

    init {

        excitatoryRandomizerPanel = EditableRandomizerPanel(null, exRandomizer, false)
        inhibitoryRandomizerPanel = EditableRandomizerPanel(null, inRandomizer, false)

        excitatoryRandomizerPanel?.initListeners()
        inhibitoryRandomizerPanel?.initListeners()


        // if (SynapsePolarityAndRandomizerPanel.RandBehavior.FORCE_OFF != randomizerState) {
        // buffer
        this.add(Box.createVerticalStrut(10), BorderLayout.CENTER)
        val dualRandomizerPanel = JPanel()
        dualRandomizerPanel.layout = BoxLayout(dualRandomizerPanel, BoxLayout.X_AXIS)
        val inBox = Box.createVerticalBox()
        val exBox = Box.createVerticalBox()
        inBox.alignmentY = TOP_ALIGNMENT
        inBox.add(inhibitoryRandomizerPanel)
        inBox.add(Box.createVerticalGlue())
        inBox.add(JPanel())
        exBox.alignmentY = TOP_ALIGNMENT
        exBox.add(excitatoryRandomizerPanel)
        exBox.add(Box.createVerticalGlue())
        exBox.add(JPanel())
        dualRandomizerPanel.add(inBox)
        dualRandomizerPanel.add(Box.createHorizontalStrut(5))
        dualRandomizerPanel.add(exBox)
        this.add(dualRandomizerPanel, BorderLayout.SOUTH)
    }

}


/**
 * @author Zoë Tosi
 */
class EditableRandomizerPanel : JPanel {
    /**
     * The Polarity Associated with the panel (inhibitory -&#62; only
     * negative values allowed; exciatory -&#62; only positive values
     * allowed).
     */
    private val polarity: Polarity? = null

    /**
     * The PolaraizedRandomizer this panel will either use to fill field
     * values and edit, or which this panel will create and then edit.
     */
    private val randomizer = Randomizer()

    /**
     * The randomizer panel used as a basis for this panel.
     */
    private var randomizerPanel: AnnotatedPropertyEditor? = null

    /**
     * A DropDownTriangle used to show or hide [.randomizerPanel]. The
     * state of the triangle is used on creation to decide whether or not
     * any randomization at all occurs. Only visible on creation.
     */
    val enableStatusTriangle: DropDownTriangle

    /**
     * The apply button used to apply changes after editing.
     */
    private val applyButton = JButton("Apply")
    /**
     * Construct the synapse randomizer panel.
     *
     * @param parent     for resizing
     * @param dist  initial probabiliity distrubtion
     * @param enabled    start out enabled (w/ [.randomizerPanel]
     * visible)
     */
    /**
     * Construct the synapse randomizer panel.
     *
     * @param parent     for resizing
     * @param dist  initial probabiliity distrubtion
     */
    @JvmOverloads
    constructor(parent: Window?, dist: ProbabilityDistribution, enabled: Boolean = true) {
        randomizer.probabilityDistribution = dist
        // Below used to set color of boundary, which we are ok without
        // polarity = randomizer.getProbabilityDistribution().getPolarity();
        enableStatusTriangle = DropDownTriangle(UpDirection.LEFT, enabled, "Disabled", "Enabled", parent)
        enableStatusTriangle.setUpLabelColor(Color(200, 0, 0))
        enableStatusTriangle.setDownLabelColor(Color(0, 160, 0))
        init()
    }

    // /**
    //  * Initialize with a polarity.
    //  *
    //  * @param parent parent window
    //  * @param polarity initial polarity
    //  */
    // constructor(parent: Window?, polarity: Polarity?) {
    //     randomizer.probabilityDistribution = UniformRealDistribution()
    //     enableStatusTriangle = DropDownTriangle(UpDirection.LEFT, false, "Disabled", "Enabled", parent)
    //     enableStatusTriangle.setUpLabelColor(Color(200, 0, 0))
    //     enableStatusTriangle.setDownLabelColor(Color(0, 160, 0))
    //     init()
    // }

    /**
     * Initializes the layout of the panel
     */
    private fun init() {
        randomizerPanel = AnnotatedPropertyEditor(randomizer)
        layout = GridBagLayout()
        // val colorBorder = BorderFactory.createLineBorder(if (Polarity.EXCITATORY == polarity) Color.red else Color.blue)
        // this.setBorder(BorderFactory.createTitledBorder(colorBorder, polarity?.title()));
        this.setBorder(BorderFactory.createLineBorder(Color.gray));
        val gbc = GridBagConstraints()
        gbc.anchor = GridBagConstraints.NORTHWEST
        gbc.fill = GridBagConstraints.BOTH
        gbc.weighty = 1.0
        gbc.weightx = 1.0
        gbc.gridx = 0
        gbc.gridy = 0
        val topPanel = Box.createHorizontalBox()
        // if (SynapsePolarityAndRandomizerPanel.RandBehavior.FORCE_ON != randomizerState && creationPanel) {
        topPanel.add(JLabel("Weight Randomizer"))
        topPanel.add(Box.createHorizontalStrut(15))
        topPanel.add(Box.createHorizontalGlue())
        topPanel.add(enableStatusTriangle)
        topPanel.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        this.add(topPanel, gbc)
        gbc.gridy += 1
        // }
        // randomizerPanel!!.isVisible =
        //     enableStatusTriangle.isDown || SynapsePolarityAndRandomizerPanel.RandBehavior.FORCE_ON == randomizerState
        this.add(randomizerPanel, gbc)
        gbc.gridy += 1
        gbc.fill = GridBagConstraints.VERTICAL
        gbc.weighty = 1.0
        this.add(JPanel(), gbc)
        gbc.anchor = GridBagConstraints.SOUTHEAST
        gbc.fill = GridBagConstraints.NONE
        gbc.weighty = 0.0
        gbc.gridy += 1
        // if (!creationPanel) {
        val bottomPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        bottomPanel.add(applyButton)
        bottomPanel.preferredSize =
            Dimension(randomizerPanel!!.preferredSize.width, bottomPanel.preferredSize.height)
        this.add(bottomPanel, gbc)
        // }
    }

    /**
     * Initializes the listener on the apply button, allowing the values in
     * the randomizer to be committed, and if editing, immediately causing
     * the synapses in question to undergo randomization.
     */
    fun initListeners() {
        applyButton.addActionListener {
            if (enableStatusTriangle.isDown) {
                randomizerPanel!!.commitChanges()
                // if (Polarity.EXCITATORY == polarity) {
                //     if (synapseGroup != null) {
                //         synapseGroup.setExcitatoryRandomizer(randomizer.probDist)
                //         if (!creationPanel) {
                //             synapseGroup.randomizeExcitatoryConnections()
                //         }
                //     }
                // } else {
                // if (synapseGroup != null) {
                //     synapseGroup.setInhibitoryRandomizer(randomizer.probDist)
                //     if (!creationPanel) {
                //         synapseGroup.randomizeInhibitoryConnections()
                //     }
                // }
                // }
            }
        }
        enableStatusTriangle.addMouseListener(object : MouseListener {
            override fun mouseClicked(arg0: MouseEvent) {
                randomizerPanel!!.isVisible = enableStatusTriangle.isDown
                repaint()
                // parent.pack()
            }

            override fun mouseEntered(arg0: MouseEvent) {}
            override fun mouseExited(arg0: MouseEvent) {}
            override fun mousePressed(arg0: MouseEvent) {}
            override fun mouseReleased(arg0: MouseEvent) {}
        })
    }

    /**
     * Adds an additional listener to the apply button so that other panels
     * can perform other actions if the button is pressed.
     */
    fun addApplyActionListener(al: ActionListener?) {
        applyButton.addActionListener(al)
    }

    /**
     * Applies the settings in the randomizer panel to the randomizer given
     * to or created by this panel if the display triangle displays
     * "Enabled".
     */
    fun commitChanges() {
        // if (enableStatusTriangle.isDown || randomizerState == SynapsePolarityAndRandomizerPanel.RandBehavior.FORCE_ON) {
        //     randomizerPanel!!.commitChanges()
        // }
    }

    fun commitChanges(connection: ConnectionStrategy) {
        // excitatoryRandomizerPanel.commitChanges()
        // inhibitoryRandomizerPanel.commitChanges()
        // connection.excitatoryRatio = ratioSlider.getValue().toDouble() / 100
        // connection.isUseExcitatoryRandomization = this.exRandomizerEnabled()
        // connection.isUseInhibitoryRandomization = this.inRandomizerEnabled()
        // connection.exRandomizer = getExRandomizer()
        // connection.inRandomizer = getInRandomizer()
    }

}

fun createInhibExcPanel(synapses: List<Synapse>): InhibExcRandomizerPanel? {
    val panel = InhibExcRandomizerPanel(synapses)
    if (synapses.isEmpty()) {
        JOptionPane.showMessageDialog(
            null, "No synapses to display", "Warning",
            JOptionPane.WARNING_MESSAGE
        );
        return null
    }
    return panel
}

fun main() {
    val net = Network()
    val neurons = List(20) { Neuron(net) }
    // val neurons = mutableListOf<Neuron>() // To test empty list case
    val conn = RadialProbabilistic()
    val syns = conn.connectNeurons(net, neurons, neurons)
    createInhibExcPanel(syns)?.displayInDialog()
}
