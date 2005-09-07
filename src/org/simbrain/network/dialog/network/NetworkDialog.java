 	/*
 * Part of HDV (High-Dimensional-Visualizer), a tool for visualizing high
 * dimensional datasets.
 * 
 * Copyright (C) 2004 Scott Hotton <http://www.math.smith.edu/~zeno/> and 
 * Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.network.dialog.network;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.simbrain.network.MouseEventHandler;
import org.simbrain.network.NetworkPanel;
import org.simbrain.network.NetworkPreferences;
import org.simbrain.network.SelectionHandle;
import org.simbrain.network.pnodes.PNodeLine;
import org.simbrain.network.pnodes.PNodeNeuron;
import org.simbrain.network.pnodes.PNodeWeight;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simnet.interfaces.Network;

/**
 * <b>DialogNetwork</b> is a dialog box for setting the properties of the 
 * Network GUI.  If the user presses ok, values become default values.  Restore
 * defaults restores to original values.  When canceling out the values prior to 
 * making any changes are restored.
 */
public class NetworkDialog extends StandardDialog implements ActionListener, ChangeListener {

        
    private NetworkPanel netPanel;
        
    private String[] list = {"Background", "Line", "Hot node", 
            "Cool node", "Excitatory weight", "Inhibitory weight",
            "Lasso", "Selection"};
    
    private JTabbedPane tabbedPane = new JTabbedPane();
    private JPanel colorPanel = new JPanel();
    private JPanel tabGraphics = new JPanel();
    private JPanel tabLogic = new JPanel();
    private JPanel tabMisc = new JPanel();
    private LabelledItemPanel graphicsPanel = new LabelledItemPanel();
    private LabelledItemPanel logicPanel = new LabelledItemPanel();
    private LabelledItemPanel miscPanel = new LabelledItemPanel();
    private JButton defaultButton = new JButton ("Restore defaults");
        
    private JComboBox cbChangeColor = new JComboBox(list);
    private JButton changeColorButton = new JButton("Set");
 	private JPanel colorIndicator = new JPanel();
    private JSlider weightSizeMaxSlider = new JSlider(JSlider.HORIZONTAL,5, 50, 10);
    private JSlider weightSizeMinSlider = new JSlider(JSlider.HORIZONTAL,5, 50, 10);
    private JTextField tfTimeStep = new JTextField();
    private JComboBox cbTimeUnits = new JComboBox(Network.getUnits());
    private JTextField precisionField = new JTextField();
    private JCheckBox showWeightValuesBox = new JCheckBox();
    private JCheckBox isRoundingBox= new JCheckBox();
    private JCheckBox indentNetworkFilesBox = new JCheckBox();
    private JTextField nudgeAmountField = new JTextField();
        
        /**
     * This method is the default constructor.
     */
    public NetworkDialog(NetworkPanel np) {
        netPanel = np;
        init();
    }

    /**
     * This method initialises the components on the panel.
     */
    private void init() {
        //Initialize Dialog
        setTitle("Network Dialog");
        fillFieldValues();
        checkRounding();
        graphicsPanel.setBorder(BorderFactory.createEtchedBorder());
        precisionField.setColumns(3);
        nudgeAmountField.setColumns(3);
        this.setLocation(500, 0); //Sets location of network dialog

        //Set up sliders
        weightSizeMaxSlider.setMajorTickSpacing(25);
        weightSizeMaxSlider.setPaintTicks(true);
        weightSizeMaxSlider.setPaintLabels(true);
        weightSizeMinSlider.setMajorTickSpacing(25);
        weightSizeMinSlider.setPaintTicks(true);
        weightSizeMinSlider.setPaintLabels(true);

        //Add Action Listeners
        defaultButton.addActionListener(this);
        changeColorButton.addActionListener(this);
        isRoundingBox.addActionListener(this);
        weightSizeMaxSlider.addChangeListener(this);
        weightSizeMinSlider.addChangeListener(this);
        showWeightValuesBox.addActionListener(this);
        cbChangeColor.addActionListener(this);
        cbChangeColor.setActionCommand("moveSelector");

        //Set up color pane
        colorPanel.add(cbChangeColor);
        colorIndicator.setSize(20, 20);
        colorPanel.add(colorIndicator);
        colorPanel.add(changeColorButton);
        setIndicatorColor();

        //Set up grapics panel
        graphicsPanel.addItem("Color:", colorPanel);
        graphicsPanel.addItem("Weight size max", weightSizeMaxSlider);
        graphicsPanel.addItem("Weight size min", weightSizeMinSlider);
        //graphicsPanel.addItem("Show weight values", showWeightValuesBox);

        //Set up logic panel
        logicPanel.addItem("Time step", tfTimeStep);
        logicPanel.addItem("Time units", cbTimeUnits);
        logicPanel.addItem("Round off neuron values", isRoundingBox);
        logicPanel.addItem("Precision of round-off", precisionField);

        //Set up Misc Panel
        miscPanel.addItem("Indent network files", indentNetworkFilesBox);
        miscPanel.addItem("Nudge Amount", nudgeAmountField);

        //Set up tab panels
        tabGraphics.add(graphicsPanel);
        tabLogic.add(logicPanel);
        tabMisc.add(miscPanel);
        tabbedPane.addTab("Graphics", tabGraphics);
        tabbedPane.addTab("Logic", tabLogic);
        tabbedPane.addTab("Misc.", tabMisc);
        addButton(defaultButton);
        setContentPane(tabbedPane);
    }

    /**
     * Respond to button pressing events
     */
    public void actionPerformed(ActionEvent e) {

        Object o = e.getSource();

        if (o == isRoundingBox) {
            checkRounding();
            netPanel.getNetwork().setRoundingOff(isRoundingBox.isSelected());
        } else if (o == precisionField) {
            netPanel.getNetwork().setPrecision(Integer.valueOf(precisionField.getText()).intValue());
        } else if (o == changeColorButton) {
            Color theColor = getColor();
            switch (cbChangeColor.getSelectedIndex()) {
	            case 0:
					if (theColor != null) {
						netPanel.setBackgroundColor(theColor);
						netPanel.renderObjects();
					}
					break;
				case 1:
					if (theColor != null) {
						PNodeLine.setLineColor(theColor);
						netPanel.resetLineColors();
						netPanel.renderObjects();
					}
					break;
				case 2:
					if (theColor != null) {
				        PNodeNeuron.setCoolColor(theColor.getRGB());
						netPanel.renderObjects();
					}
					break;
				case 3:
					if (theColor != null) {
				        PNodeNeuron.setHotColor(theColor.getRGB());
						netPanel.renderObjects();
					}
					break;
				case 4:
					if (theColor != null) {
					       PNodeWeight.setExcitatoryColor(theColor);
					       netPanel.renderObjects();
					}
					break;
				case 5:
					if (theColor != null) {
				        PNodeWeight.setInhibitoryColor(theColor);
				        netPanel.renderObjects();
					}
					break;
				case 6:
					if (theColor != null) {
						MouseEventHandler.setMarquisColor(theColor);
					}
					break;
				case 7:
					if (theColor != null) {
						SelectionHandle.setSelectionColor(theColor);
					}
					break;
			};
			netPanel.renderObjects();
            	setIndicatorColor();
        } else if (o == defaultButton) {
            NetworkPreferences.restoreDefaults();
            this.returnToCurrentPrefs();
        } else if (e.getActionCommand().equals("moveSelector")) {
            setIndicatorColor();
        }
    }

    /**
	 * Populate fields with current data
	 */
    public void fillFieldValues() {
        tfTimeStep.setText(Double.toString(netPanel.getNetwork().getTimeStep()));
        cbTimeUnits.setSelectedIndex(netPanel.getNetwork().getTimeUnits());
        precisionField.setText(Integer.toString(netPanel.getNetwork().getPrecision()));
        nudgeAmountField.setText(Double.toString(netPanel.getNudgeAmount()));
        isRoundingBox.setSelected(netPanel.getNetwork().isRoundingOff());
        weightSizeMaxSlider.setValue(PNodeWeight.getMaxRadius());
        weightSizeMinSlider.setValue(PNodeWeight.getMinRadius());
        indentNetworkFilesBox.setSelected(netPanel.getSerializer().isUsingTabs());
    }

    /** (non-Javadoc)
     * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
     */
    public void stateChanged(ChangeEvent e) {
        JSlider j = (JSlider) e.getSource();
        if (j == weightSizeMaxSlider) {
        		PNodeWeight.setMaxRadius(j.getValue());
        } else if (j == weightSizeMinSlider) {
    			PNodeWeight.setMinRadius(j.getValue());
        }
        netPanel.renderObjects();
    }

    /**
     * Show the color pallette and get a color
     * 
     * @return selected color
     */
    public Color getColor() {
        //Color findColor = colorFinder();
        JColorChooser colorChooser = new JColorChooser();
        Color theColor = JColorChooser.showDialog(this, "Choose Color",
                colorIndicator.getBackground());
        colorChooser.setLocation(200, 200); //Set location of color chooser
        return theColor;
    }

    /**
     * Enable or disable the precision field depending on state of rounding button
     *
     */
    private void checkRounding() {
        if (isRoundingBox.isSelected() == false) {
            precisionField.setEnabled(false);
        } else {
            precisionField.setEnabled(true);
        }
    }

    /**
     * Restores the changed fields to their previous values
     * Used when user cancels out of the dialog.
     *
     */
    public void returnToCurrentPrefs() {
        netPanel.setBackgroundColor(new Color(NetworkPreferences.getBackgroundColor()));
        PNodeLine.setLineColor(new Color(NetworkPreferences.getLineColor()));
        PNodeNeuron.setHotColor(NetworkPreferences.getHotColor());
        PNodeNeuron.setCoolColor(NetworkPreferences.getCoolColor());
        PNodeWeight.setExcitatoryColor(new Color(NetworkPreferences.getExcitatoryColor()));
        PNodeWeight.setInhibitoryColor(new Color(NetworkPreferences.getInhibitoryColor()));
        PNodeWeight.setMaxRadius(NetworkPreferences.getMaxRadius());
        PNodeWeight.setMinRadius(NetworkPreferences.getMinRadius());
        netPanel.getNetwork().setTimeStep(NetworkPreferences.getTimeStep());
        netPanel.getNetwork().setTimeUnits(NetworkPreferences.getTimeUnits());
        netPanel.getNetwork().setPrecision(NetworkPreferences.getPrecision());
        netPanel.setNudgeAmount(NetworkPreferences.getNudgeAmount());
        netPanel.getSerializer().setUsingTabs(NetworkPreferences.getUsingIndent());
        netPanel.resetLineColors();
        netPanel.renderObjects();
        setIndicatorColor();
    }

    /**
     * Sets selected preferences as user defaults to be used each time program is launched
     * Called when "ok" is pressed
     *
     */
    public void setAsDefault() {
        NetworkPreferences.setBackgroundColor(netPanel.getBackground().getRGB());
        NetworkPreferences.setLineColor(PNodeLine.getLineColor().getRGB());
        NetworkPreferences.setHotColor(PNodeNeuron.getHotColor());
        NetworkPreferences.setCoolColor(PNodeNeuron.getCoolColor());
        NetworkPreferences.setExcitatoryColor(PNodeWeight.getExcitatoryColor().getRGB());
        NetworkPreferences.setInhibitoryColor(PNodeWeight.getInhibitoryColor().getRGB());
        NetworkPreferences.setLassoColor(MouseEventHandler.getMarquisColor().hashCode());
        NetworkPreferences.setSelectionColor(SelectionHandle.getSelectionColor().hashCode());
        NetworkPreferences.setMaxRadius(PNodeWeight.getMaxRadius());
        NetworkPreferences.setMinRadius(PNodeWeight.getMinRadius());
        NetworkPreferences.setTimeStep(netPanel.getNetwork().getTimeStep());
        NetworkPreferences.setTimeUnits(netPanel.getNetwork().getTimeUnits());
        NetworkPreferences.setPrecision(netPanel.getNetwork().getPrecision());
        NetworkPreferences.setUsingIndent(netPanel.getSerializer().isUsingTabs());
        NetworkPreferences.setNudgeAmount(netPanel.getNudgeAmount());
    }

    public boolean isUsingIndent() {
        return indentNetworkFilesBox.isSelected();
    }

    /**
     * Gets the value for nudge
     * 
     * @return 
     */
    public double getNudgeAmountField() {
        return Double.valueOf(nudgeAmountField.getText()).doubleValue();
    }

    /**
     * Set the color indicator based on the current selection 
     * in the combo box
     */
    private void setIndicatorColor() {
        Color clr;

        switch (cbChangeColor.getSelectedIndex()) {
        case 0:
            colorIndicator.setBackground(netPanel.getBackground());
            break;
        case 1:
            colorIndicator.setBackground(PNodeLine.getLineColor());
            break;
        case 2:
            clr = Color.getHSBColor((float) PNodeNeuron.getHotColor(), 1,
                    (float) 1);
            colorIndicator.setBackground(clr);
            break;
        case 3:
            clr = Color.getHSBColor((float) PNodeNeuron.getCoolColor(), 1,
                    (float) 1);
            colorIndicator.setBackground(clr);
            break;
        case 4:
            colorIndicator.setBackground(PNodeWeight.getExcitatoryColor());
            break;
        case 5:
            colorIndicator.setBackground(PNodeWeight.getInhibitoryColor());
            break;
        case 6:
            colorIndicator.setBackground((Color) MouseEventHandler
                    .getMarquisColor());
            break;
        case 7:
            colorIndicator.setBackground((Color) SelectionHandle
                    .getSelectionColor());
            break;
        }
    }

    /**
     * @return Returns the cbTimeUnits.
     */
    public JComboBox getCbTimeUnits() {
        return cbTimeUnits;
    }
}