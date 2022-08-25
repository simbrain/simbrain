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

import org.simbrain.util.SwitchableChangeListener
import org.simbrain.util.SwitchablePropertyChangeListener
import org.simbrain.util.Utils
import org.simbrain.util.displayInDialog
import java.awt.*
import java.beans.PropertyChangeEvent
import java.util.*
import javax.swing.*
import javax.swing.border.Border
import javax.swing.event.ChangeEvent

/**
 * Panel for setting inhibitory-excitatory ratio.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
class PercentExcitatoryPanel(percentExcitatory: Double = 50.0) : JPanel() {

    /**
     * Max ratio of excitatory/inhibitory connections.
     */
    private val RATIO_MAX = 100

    /**
     * Min ratio of excitatory/inhibitory connections.
     */
    private val RATIO_MIN = 0

    /**
     * A slider for setting the ratio of inhibitory to excitatory connections.
     */
    private val ratioSlider = JSlider(JSlider.HORIZONTAL, RATIO_MIN, RATIO_MAX, percentExcitatory.toInt())

    /**
     * A text field for setting the ratio of excitatory to inhibitory
     * connections.
     */
    private val eRatio = JFormattedTextField(percentExcitatory)

    /**
     * A text field for setting the ratio of inhibitory to excitatory
     * connections.
     */
    private val iRatio = JFormattedTextField(100 - percentExcitatory)

    /**
     * A switchable listener.
     *
     * @see SwitchablePropertyChangeListener listenting to changes to the
     * excitatory ratio text field.
     */
    private var exTfListener: SwitchablePropertyChangeListener? = null

    /**
     * A switchable listener.
     *
     * @see SwitchablePropertyChangeListener listenting to changes to the
     * inhibitory ratio text field.
     */
    private var inTfListener: SwitchablePropertyChangeListener? = null

    /**
     * A switchable listener.
     *
     * @see SwitchableChangeListener listenting to changes to the
     * excitatory/inhibitory ratio slider.
     */
    private var sliderListener: SwitchableChangeListener? = null

    /**
     * The apply button associated with the polarity slider for editing.
     */
    private val sliderApply = JButton("Apply")

    init {
        ratioSlider.majorTickSpacing = 10
        ratioSlider.minorTickSpacing = 2
        ratioSlider.paintTicks = true

        val labelTable = Hashtable<Int, JLabel>()
        labelTable[0] = JLabel("0/100")
        labelTable[25] = JLabel("25/75")
        labelTable[50] = JLabel("50/50")
        labelTable[75] = JLabel("75/25")
        labelTable[100] = JLabel("100/0")
        ratioSlider.labelTable = labelTable
        ratioSlider.paintLabels = true

        val sliderPanel = JPanel(GridBagLayout())

        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.gridwidth = 5
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.CENTER
        gbc.insets = Insets(5, 5, 5, 5)
        sliderPanel.add(ratioSlider, gbc)

        gbc.insets = Insets(5, 5, 0, 5)
        gbc.gridx = 1
        gbc.gridy = 1
        gbc.gridwidth = 1
        gbc.fill = GridBagConstraints.NONE
        val inTfPanel = JPanel(FlowLayout())
        val iRatioSize = iRatio.preferredSize
        iRatioSize.width = 40
        iRatio.preferredSize = iRatioSize
        inTfPanel.add(JLabel("% Inhibitory"))
        inTfPanel.add(iRatio)
        sliderPanel.add(inTfPanel, gbc)

        gbc.gridx = 2
        gbc.gridwidth = 1
        gbc.weightx = 0.0
        val blank = JPanel()
        blank.preferredSize = Dimension(60, 10)
        blank.minimumSize = Dimension(60, 10)
        sliderPanel.add(blank, gbc)

        gbc.gridx = 3
        gbc.weightx = 0.0
        gbc.gridwidth = 1
        val exTfPanel = JPanel(FlowLayout())
        val eRatioSize = eRatio.preferredSize
        eRatioSize.width = 40
        eRatio.preferredSize = eRatioSize
        exTfPanel.add(JLabel("% Excitatory"))
        exTfPanel.add(eRatio)
        sliderPanel.add(exTfPanel, gbc)

        gbc.gridx = 4
        gbc.gridwidth = 1
        gbc.weightx = 0.0
        gbc.fill = GridBagConstraints.NONE
        // sliderPanel.add(warning, gbc)
        // warning.setVisible(false)

        // if (!creationPanel) {
        //     gbc.gridwidth = 1
        //     gbc.fill = GridBagConstraints.NONE
        //     gbc.anchor = GridBagConstraints.EAST
        //     gbc.gridx = 4
        //     gbc.gridy = 2
        //     gbc.insets = Insets(10, 5, 5, 10)
        //     sliderPanel.add(sliderApply, gbc)
        // }

        val sliderBorder: Border = BorderFactory.createTitledBorder("Inhibitory/Excitatory Ratio")
        sliderPanel.border = sliderBorder
        layout = BorderLayout()
        add(sliderPanel, BorderLayout.NORTH)

        ratioSlider.setValue(percentExcitatory.toInt())

        sliderListener = object : SwitchableChangeListener() {
            override fun stateChanged(e: ChangeEvent) {
                val source = e.source as JSlider
                if (source === ratioSlider && isEnabled) {
                    exTfListener!!.disable()
                    inTfListener!!.disable()
                    eRatio.value = ratioSlider.value
                    iRatio.value = RATIO_MAX - ratioSlider.value
                    exTfListener!!.enable()
                    inTfListener!!.enable()
                }
            }
        }

        exTfListener = object : SwitchablePropertyChangeListener() {
            override fun propertyChange(evt: PropertyChangeEvent) {
                if (evt.source === eRatio && isEnabled) {
                    sliderListener!!.disable()
                    inTfListener!!.disable()
                    ratioSlider.value = (eRatio.value as Number).toInt()
                    // iRatio.value = SynapsePolarityAndRandomizerPanel.RATIO_MAX - (eRatio.value as Number).toInt()
                    sliderListener!!.enable()
                    inTfListener!!.enable()
                }
            }
        }

        inTfListener = object : SwitchablePropertyChangeListener() {
            override fun propertyChange(evt: PropertyChangeEvent) {
                if (evt.source === iRatio && isEnabled) {
                    sliderListener!!.disable()
                    exTfListener!!.disable()
                    ratioSlider.value = RATIO_MAX - (iRatio.value as Number).toInt()
                    eRatio.value = RATIO_MAX - (iRatio.value as Number).toInt()
                    sliderListener!!.enable()
                    exTfListener!!.enable()
                }
            }
        }

        sliderApply.addActionListener {
            val percentExcitatory = Utils.doubleParsable(eRatio) / 100
            if (!java.lang.Double.isNaN(percentExcitatory)) {
                ratioSlider.value = (100 * getPercentAsProbability()).toInt() // TODO
            }
        }

        ratioSlider.addChangeListener(sliderListener)

        eRatio.addPropertyChangeListener(exTfListener)

        iRatio.addPropertyChangeListener(inTfListener)

    }

    /**
     * Returns the percent excitatory as a double between 0 and 1.
     */
    fun getPercentAsProbability(): Double {
        return Utils.doubleParsable(eRatio) / 100
    }

}

fun main() {
    PercentExcitatoryPanel().displayInDialog()
}