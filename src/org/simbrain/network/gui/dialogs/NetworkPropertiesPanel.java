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
package org.simbrain.network.gui.dialogs;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.simbrain.network.core.Network;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.EditMode;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.gui.nodes.SelectionHandle;
import org.simbrain.network.gui.nodes.SelectionMarquee;
import org.simbrain.network.gui.nodes.SynapseNode;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.Utils;

/**
 * <b>NetworkPropertiesPanel</b> is a panel for setting the properties of the
 * neural network, mainly the GUI. If the user presses ok, values become default
 * values. Restore defaults restores to original values. When canceling out the
 * values prior to making any changes are restored.
 *
 */
public class NetworkPropertiesPanel extends JPanel {

    /** Network panel. */
    protected NetworkPanel networkPanel;

    /** Background. */
    private static final String BACKGROUND = "Background";

    /** Line. */
    private static final String LINE = "Line";

    /** Hot node. */
    private static final String HOTNODE = "Hot node";

    /** Cool node. */
    private static final String COOLNODE = "Cool node";

    /** Excitatory weight. */
    private static final String EXCITATORY = "Excitatory weight";

    /** Inhibitory weight. */
    private static final String INHIBITORY = "Inhibitory weight";

    /** Lasso. */
    private static final String LASSO = "Lasso";

    /** Selection. */
    private static final String SELECTION = "Selection";

    /** Spike. */
    private static final String SPIKE = "Spike";

    /** Zero weight. */
    private static final String ZERO = "Zero weight";

    /** List of items for combo box. */
    private String[] objectColorList = { BACKGROUND, HOTNODE, COOLNODE,
            EXCITATORY, INHIBITORY, SPIKE, ZERO };

    /** Color panel displays current color of item selected in combo box. */
    private JPanel colorPanel = new JPanel();

    /** Change color combo box. */
    private JComboBox cbChangeColor = new JComboBox(objectColorList);

    /** Change color of the item selected in combo box. */
    private JButton changeColorButton = new JButton("Set");

    /** Color indicator. */
    private JPanel colorIndicator = new JPanel();

    /** Maximum size of weight slider. */
    private JSlider weightSizeMaxSlider = new JSlider(JSlider.HORIZONTAL, 5,
            50, 10);

    /** Minimum size of weight slider. */
    private JSlider weightSizeMinSlider = new JSlider(JSlider.HORIZONTAL, 5,
            50, 10);

    /**
     * Threshold above which subnetworks or groups with that many synapses stop
     * displaying them.
     */
    private JTextField tfSynapseVisibilityThreshold = new JTextField();

    /** Nudge amount text field. */
    private JTextField nudgeAmountField = new JTextField();

    /** Network time step text field. */
    private JTextField timeStepField = new JTextField();

    /** Wand radius. */
    private JTextField wandRadiusField = new JTextField();

    /** Show time check box. */
    private JCheckBox showTimeBox = new JCheckBox();

    /** Check box for whether to use subsampling. */
    private JCheckBox cbUseSubSampling = new JCheckBox();

    /** Text field for number of subsamples to use. */
    private JTextField tfNumSubSamples = new JTextField();

    /**
     * This method is the default constructor.
     *
     * @param np reference to <code>NetworkPanel</code>.
     */
    public NetworkPropertiesPanel(final NetworkPanel np) {
        networkPanel = np;
        init();
    }

    /**
     * This method initializes the components on the panel.
     */
    private void init() {

        // Initial setup
        Box mainVertical = Box.createVerticalBox();
        fillFieldValues();

        // Set up sliders
        weightSizeMaxSlider.setMajorTickSpacing(25);
        weightSizeMaxSlider.setPaintTicks(true);
        weightSizeMaxSlider.setPaintLabels(true);
        weightSizeMinSlider.setMajorTickSpacing(25);
        weightSizeMinSlider.setPaintTicks(true);
        weightSizeMinSlider.setPaintLabels(true);

        // Add Action Listeners
        addActionListeners();

        // Set up color pane
        colorPanel.add(cbChangeColor);
        colorIndicator.setSize(20, 20);
        colorPanel.add(colorIndicator);
        colorPanel.add(changeColorButton);
        setIndicatorColor();

        // Set up graphics panel
        LabelledItemPanel guiPanel = new LabelledItemPanel();
        guiPanel.addItem("Color:", colorPanel);
        guiPanel.addItem("Weight size max", weightSizeMaxSlider);
        guiPanel.addItem("Weight size min", weightSizeMinSlider);
        mainVertical.add(guiPanel);

        // Separator
        mainVertical.add(new JSeparator(JSeparator.HORIZONTAL));

        // Other properties
        LabelledItemPanel miscPanel = new LabelledItemPanel();
        miscPanel.addItem("Network time step", timeStepField);
        miscPanel.addItem("Synapse visibility threshold",
                tfSynapseVisibilityThreshold);
        nudgeAmountField.setColumns(3);
        miscPanel.addItem("Nudge Amount", nudgeAmountField);
        miscPanel.addItem("Wand radius", wandRadiusField);

        // Subsampling Stuff
        miscPanel.add(new JSeparator(JSeparator.HORIZONTAL));
        miscPanel.addItem("Use Subsampling for large neuron groups",
                cbUseSubSampling);
        miscPanel.addItem("Number of Subsamples / Subsampling Threshold",
                tfNumSubSamples);
        updateSubSamplingStuff();

        // TODO: tooltips for all this
        mainVertical.add(miscPanel);

        // Add the main panel
        add(mainVertical);

    }

    /**
     * Set up relevant buttons to respond to actions. These actions are
     * immediately changed in the network panel (to make it easier to fine tune
     * them), and so "canceling" out of the dialog will not change these.
     */
    private void addActionListeners() {
        changeColorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateColors();
            }
        });
        weightSizeMaxSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider j = (JSlider) e.getSource();
                SynapseNode.setMaxDiameter(j.getValue());
                networkPanel.resetSynapseDiameters();
            }
        });
        weightSizeMinSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider j = (JSlider) e.getSource();
                SynapseNode.setMinDiameter(j.getValue());
                networkPanel.resetSynapseDiameters();
            }
        });
        cbChangeColor.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setIndicatorColor();
            }
        });
        cbUseSubSampling.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateSubSamplingStuff();
            }
        });
    }

    /**
     * Update the num subsamples text field (enabled only if subsampling is selected).
     */
    protected void updateSubSamplingStuff() {
        tfNumSubSamples.setEnabled(cbUseSubSampling.isSelected());
   }

    /**
     * Update relevant colors based on combo box. As noted in
     * {@link #addActionListeners}, these changes are immediate and canceling
     * from the dialog will not revert them.
     */
    private void updateColors() {
        Color theColor = showColorChooser();
        if (cbChangeColor.getSelectedItem().toString().equals(BACKGROUND)) {
            if (theColor != null) {
                NetworkPanel.setBackgroundColor(theColor);
            }
        } else if (cbChangeColor.getSelectedItem().toString().equals(LINE)) {
            if (theColor != null) {
                SynapseNode.setLineColor(theColor);
            }
        } else if (cbChangeColor.getSelectedItem().toString().equals(HOTNODE)) {
            if (theColor != null) {
                NeuronNode.setHotColor(Utils.colorToFloat(theColor));
            }
        } else if (cbChangeColor.getSelectedItem().toString().equals(COOLNODE)) {
            if (theColor != null) {
                NeuronNode.setCoolColor(Utils.colorToFloat(theColor));
            }
        } else if (cbChangeColor.getSelectedItem().toString()
                .equals(EXCITATORY)) {
            if (theColor != null) {
                SynapseNode.setExcitatoryColor(theColor);
            }
        } else if (cbChangeColor.getSelectedItem().toString()
                .equals(INHIBITORY)) {
            if (theColor != null) {
                SynapseNode.setInhibitoryColor(theColor);
            }
        } else if (cbChangeColor.getSelectedItem().toString().equals(SPIKE)) {
            if (theColor != null) {
                NeuronNode.setSpikingColor(theColor);
            }
        } else if (cbChangeColor.getSelectedItem().toString().equals(ZERO)) {
            if (theColor != null) {
                SynapseNode.setZeroWeightColor(theColor);
            }
        }
        networkPanel.resetColors();
        setIndicatorColor();
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        weightSizeMaxSlider.setValue(SynapseNode.getMaxDiameter());
        weightSizeMinSlider.setValue(SynapseNode.getMinDiameter());
        showTimeBox.setSelected(networkPanel.getShowTime());
        wandRadiusField.setText(Integer.toString(EditMode.getWandRadius()));
        timeStepField.setText(Double.toString(networkPanel.getNetwork()
                .getTimeStep()));
        nudgeAmountField
                .setText(Double.toString(NetworkPanel.getNudgeAmount()));
        tfSynapseVisibilityThreshold.setText(Integer.toString(Network
                .getSynapseVisibilityThreshold()));
        cbUseSubSampling.setSelected(NeuronGroup.isUseSubSampling());
        tfNumSubSamples.setText(Integer.toString(NeuronGroup.getNumSubSamples()));
    }

    /**
     * Commits changes when ok is pressed in parent dialog. Canceling will leave
     * these unchanged.
     */
    public void commitChanges() {
        networkPanel.getNetwork().setTimeStep(
                Double.parseDouble(timeStepField.getText()));
        NetworkPanel.setNudgeAmount(Double.parseDouble(nudgeAmountField
                .getText()));
        Network.setSynapseVisibilityThreshold(Integer
                .parseInt(tfSynapseVisibilityThreshold.getText()));
        EditMode.setWandRadius(Integer.parseInt(wandRadiusField.getText()));
        if (networkPanel.getEditMode().isWand()) {
            networkPanel.getEditMode().resetWandCursor();
            networkPanel.updateCursor();
        }
        networkPanel.setShowTime(showTimeBox.isSelected());
        networkPanel.repaint();
        
        NeuronGroup.setUseSubSampling(cbUseSubSampling.isSelected());
        NeuronGroup.setNumSubSamples(Integer.parseInt(tfNumSubSamples
                .getText()));
    }

    /**
     * Show the color palette and get a color.
     *
     * @return selected color
     */
    public Color showColorChooser() {
        JColorChooser colorChooser = new JColorChooser();
        Color theColor = JColorChooser.showDialog(this, "Choose Color",
                colorIndicator.getBackground());
        colorChooser.setLocation(200, 200); // Set location of color chooser
        return theColor;
    }

    /**
     * Set the color indicator based on the current selection in the combo box.
     */
    public void setIndicatorColor() {
        if (cbChangeColor.getSelectedItem().toString().equals(BACKGROUND)) {
            colorIndicator.setBackground(NetworkPanel.getBackgroundColor());
        } else if (cbChangeColor.getSelectedItem().toString().equals(LINE)) {
            colorIndicator.setBackground(SynapseNode.getLineColor());
        } else if (cbChangeColor.getSelectedItem().toString().equals(HOTNODE)) {
            colorIndicator.setBackground(Utils.floatToHue(NeuronNode
                    .getHotColor()));
        } else if (cbChangeColor.getSelectedItem().toString().equals(COOLNODE)) {
            colorIndicator.setBackground(Utils.floatToHue(NeuronNode
                    .getCoolColor()));
        } else if (cbChangeColor.getSelectedItem().toString()
                .equals(EXCITATORY)) {
            colorIndicator.setBackground(SynapseNode.getExcitatoryColor());
        } else if (cbChangeColor.getSelectedItem().toString()
                .equals(INHIBITORY)) {
            colorIndicator.setBackground(SynapseNode.getInhibitoryColor());
        } else if (cbChangeColor.getSelectedItem().toString().equals(LASSO)) {
            colorIndicator.setBackground(SelectionMarquee.getMarqueeColor());
        } else if (cbChangeColor.getSelectedItem().toString().equals(SELECTION)) {
            colorIndicator.setBackground(SelectionHandle.getSelectionColor());
        } else if (cbChangeColor.getSelectedItem().toString().equals(SPIKE)) {
            colorIndicator.setBackground(NeuronNode.getSpikingColor());
        } else if (cbChangeColor.getSelectedItem().toString().equals(ZERO)) {
            colorIndicator.setBackground(SynapseNode.getZeroWeightColor());
        }
    }

}
