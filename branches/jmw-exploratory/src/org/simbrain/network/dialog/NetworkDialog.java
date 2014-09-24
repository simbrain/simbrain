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
package org.simbrain.network.dialog;

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

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.NetworkPreferences;
import org.simbrain.network.nodes.SelectionHandle;
import org.simbrain.network.nodes.SelectionMarquee;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;
import org.simnet.interfaces.RootNetwork.UpdateMethod;


/**
 * <b>NetworkDialog</b> is a dialog box for setting the properties of the  Network GUI.  If the user presses ok, values
 * become default values.  Restore defaults restores to original values.  When canceling out the values prior to
 * making any changes are restored.
 */
public class NetworkDialog extends StandardDialog implements ActionListener, ChangeListener {

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

    /** Signal. */
    private static final String SIGNAL = "Signal Synapse";

    /** Spike. */
    private static final String SPIKE = "Spike";

    /** Zero weight. */
    private static final String ZERO = "Zero weight";

    /** Network panel. */
    private NetworkPanel networkPanel;

    /** List of items for combo box. */
    private String[] objectColorList = {BACKGROUND, COOLNODE, EXCITATORY, HOTNODE,
            INHIBITORY, LASSO, LINE, SELECTION, SIGNAL, SPIKE, ZERO};

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Color panel displays current color of item selected in combo box. */
    private JPanel colorPanel = new JPanel();

    /** Graphics tab. */
    private JPanel tabGraphics = new JPanel();

    /** Logic tab. */
    private JPanel tabLogic = new JPanel();

    /** Miscellaneous tab. */
    private JPanel tabMisc = new JPanel();

    /** Grahpics panel. */
    private LabelledItemPanel graphicsPanel = new LabelledItemPanel();

    /** Logic panel. */
    private LabelledItemPanel logicPanel = new LabelledItemPanel();

    /** Miscellaneous panel. */
    private LabelledItemPanel miscPanel = new LabelledItemPanel();

    /** Restore defaults button. */
    private JButton defaultButton = new JButton("Restore defaults");

    /** Change color combo box. */
    private JComboBox cbChangeColor = new JComboBox(objectColorList);

    /** Change color of the item selected in combo box. */
    private JButton changeColorButton = new JButton("Set");

    /** Color indicator. */
    private JPanel colorIndicator = new JPanel();

    /** Maximum size of weight slider. */
    private JSlider weightSizeMaxSlider = new JSlider(JSlider.HORIZONTAL, 5, 50, 10);

    /** Minimum size of weight slider. */
    private JSlider weightSizeMinSlider = new JSlider(JSlider.HORIZONTAL, 5, 50, 10);

    /** Precision text field. */
    private JTextField precisionField = new JTextField();

    /** Rounding check box. */
    private JCheckBox isRoundingBox = new JCheckBox();

    /** Indent network files check box. */
    private JCheckBox indentNetworkFilesBox = new JCheckBox();

    /** Nudge amount text field. */
    private JTextField nudgeAmountField = new JTextField();

    /** Show subnet outline check box. */
    private JCheckBox showSubnetOutlineBox = new JCheckBox();

    /** Show time check box. */
    private JCheckBox showTimeBox = new JCheckBox();

    /** List of update methods. */
    private String[] updateMethodList = {"Standard", "Priority Based", "Script Based"};

    /** Root network update method combo box. */
    private JComboBox cbUpdateMethod = new JComboBox(updateMethodList);

    /** Set Script to be used for root network update method. */
    private JButton scriptButton = new JButton("Set");

    /**
     * This method is the default constructor.
     *
     * @param np reference to <code>NetworkPanel</code>.
     */
    public NetworkDialog(final NetworkPanel np) {
        networkPanel = np;
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
        checkScript();
        graphicsPanel.setBorder(BorderFactory.createEtchedBorder());
        precisionField.setColumns(3);
        nudgeAmountField.setColumns(3);

        //Set up sliders
        weightSizeMaxSlider.setMajorTickSpacing(25);
        weightSizeMaxSlider.setPaintTicks(true);
        weightSizeMaxSlider.setPaintLabels(true);
        weightSizeMinSlider.setMajorTickSpacing(25);
        weightSizeMinSlider.setPaintTicks(true);
        weightSizeMinSlider.setPaintLabels(true);

        //Add Action Listeners
        defaultButton.addActionListener(this);
        showTimeBox.addActionListener(this);
        showSubnetOutlineBox.addActionListener(this);
        changeColorButton.addActionListener(this);
        isRoundingBox.addActionListener(this);
        weightSizeMaxSlider.addChangeListener(this);
        weightSizeMinSlider.addChangeListener(this);
        cbChangeColor.addActionListener(this);
        cbChangeColor.setActionCommand("moveSelector");
        cbUpdateMethod.addActionListener(this);
        scriptButton.addActionListener(this);

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
        graphicsPanel.addItem("Show subnet outline", showSubnetOutlineBox);
        graphicsPanel.addItem("Show time", showTimeBox);

        //Set up logic panel
        logicPanel.addItem("Round off neuron values", isRoundingBox);
        logicPanel.addItem("Precision of round-off", precisionField);
        logicPanel.addItem("Network Update Method", cbUpdateMethod);
        logicPanel.addItem("Set Script", scriptButton);

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

    /** @see StandardDialog */
    protected void closeDialogOk() {
        super.closeDialogOk();
        this.commitChanges();
        this.setAsDefault();
    }

    /** @see StandardDialog */
    protected void closeDialogCancel() {
        super.closeDialogCancel();
        this.returnToCurrentPrefs();
    }

    /**
     * Respond to button pressing events with immediate changes to network panel, where relevant.
     *
     * @param e action event
     */
    public void actionPerformed(final ActionEvent e) {
        Object o = e.getSource();

        if (o == isRoundingBox) {
            checkRounding();
            networkPanel.getRootNetwork().setRoundingOff(isRoundingBox.isSelected());
        } else if (o == changeColorButton) {
            Color theColor = getColor();
            if (cbChangeColor.getSelectedItem().toString().equals(BACKGROUND)) {

                if (theColor != null) {
                    networkPanel.setBackgroundColor(theColor);
                }

            } else if (cbChangeColor.getSelectedItem().toString().equals(LINE)) {

                if (theColor != null) {
                    networkPanel.setLineColor(theColor);
               }

            } else if (cbChangeColor.getSelectedItem().toString().equals(HOTNODE)) {

                if (theColor != null) {
                    networkPanel.setHotColor(Utils.colorToFloat(theColor));
                }

            } else if (cbChangeColor.getSelectedItem().toString().equals(COOLNODE)) {

                if (theColor != null) {
                    networkPanel.setCoolColor(Utils.colorToFloat(theColor));
                }

            } else if (cbChangeColor.getSelectedItem().toString().equals(EXCITATORY)) {

                if (theColor != null) {
                    networkPanel.setExcitatoryColor(theColor);
                }

            } else if (cbChangeColor.getSelectedItem().toString().equals(INHIBITORY)) {

                if (theColor != null) {
                    networkPanel.setInhibitoryColor(theColor);
                }

            } else if (cbChangeColor.getSelectedItem().toString().equals(LASSO)) {

                if (theColor != null) {
                    SelectionMarquee.setMarqueeColor(theColor);
                }

            } else if (cbChangeColor.getSelectedItem().toString().equals(SELECTION)) {

                if (theColor != null) {
                    SelectionHandle.setSelectionColor(theColor);
                }

            } else if (cbChangeColor.getSelectedItem().toString().equals(SIGNAL)) {

                if (theColor != null) {
                    networkPanel.setSignalColor(theColor);
                }

            } else if (cbChangeColor.getSelectedItem().toString().equals(SPIKE)) {

                if (theColor != null) {
                    networkPanel.setSpikingColor(theColor);
                }

            } else if (cbChangeColor.getSelectedItem().toString().equals(ZERO)) {

                if (theColor != null) {
                    networkPanel.setZeroWeightColor(theColor);
                }

            }
            networkPanel.resetColors();
            setIndicatorColor();
        } else if (o == defaultButton) {
            NetworkPreferences.restoreDefaults();
            this.returnToCurrentPrefs();
        } else if (e.getActionCommand().equals("moveSelector")) {
            setIndicatorColor();
        } else if (o == showTimeBox) {
            networkPanel.setShowTime(showTimeBox.isSelected());
            networkPanel.repaint();
        } else if (o == showSubnetOutlineBox) {
            networkPanel.setShowSubnetOutline(showSubnetOutlineBox.isSelected());
        } else if (o == cbUpdateMethod) {
            checkScript();
        } else if (o == scriptButton) {
            networkPanel.getUpdateStatusLabel().loadUpdateScript();
        }
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        showTimeBox.setSelected(networkPanel.getShowTime());
        showSubnetOutlineBox.setSelected(networkPanel.getShowSubnetOutline());
        precisionField.setText(Integer.toString(networkPanel.getRootNetwork().getPrecision()));
        nudgeAmountField.setText(Double.toString(networkPanel.getNudgeAmount()));
        isRoundingBox.setSelected(networkPanel.getRootNetwork().getRoundingOff());
        weightSizeMaxSlider.setValue(networkPanel.getMaxDiameter());
        weightSizeMinSlider.setValue(networkPanel.getMinDiameter());
        indentNetworkFilesBox.setSelected(networkPanel.getRootNetwork().getUsingTabs());
        if (networkPanel.getRootNetwork().getUpdateMethod().equals(UpdateMethod.DEFAULT)) {
            cbUpdateMethod.setSelectedIndex(0);
        } else if (networkPanel.getRootNetwork().getUpdateMethod().equals(UpdateMethod.PRIORITYBASED)) {
            cbUpdateMethod.setSelectedIndex(1);
        } else if (networkPanel.getRootNetwork().getUpdateMethod().equals(UpdateMethod.SCRIPTBASED)) {
            cbUpdateMethod.setSelectedIndex(2);
        }
    }

    /**
     * Commits changes not handled in action performed.
     */
    private void commitChanges() {
        networkPanel.setNudgeAmount(Double.parseDouble(nudgeAmountField.getText()));
        networkPanel.getRootNetwork().setUsingTabs(indentNetworkFilesBox.isSelected());
        networkPanel.getRootNetwork().setPrecision(Integer.parseInt(precisionField.getText()));
        switch (cbUpdateMethod.getSelectedIndex()) {
            case 0:
                networkPanel.getRootNetwork().setUpdateMethod(UpdateMethod.DEFAULT);
                break;
            case 1:
                networkPanel.getRootNetwork().setUpdateMethod(UpdateMethod.PRIORITYBASED);
                break;
            case 2:
                networkPanel.getRootNetwork().setUpdateMethod(UpdateMethod.SCRIPTBASED);
                break;
            default:
                break;
        }
        networkPanel.getUpdateStatusLabel().update();
    }

    /**
     * Listens and responds to slider state changes.
     *
     * @param e change event
     */
    public void stateChanged(final ChangeEvent e) {
        JSlider j = (JSlider) e.getSource();

        if (j == weightSizeMaxSlider) {
            networkPanel.setMaxDiameter(j.getValue());
        } else if (j == weightSizeMinSlider) {
            networkPanel.setMinDiameter(j.getValue());
        }

        networkPanel.resetSynapseDiameters();
    }

    /**
     * Show the color pallette and get a color.
     *
     * @return selected color
     */
    public Color getColor() {
        JColorChooser colorChooser = new JColorChooser();
        Color theColor = JColorChooser.showDialog(this, "Choose Color", colorIndicator.getBackground());
        colorChooser.setLocation(200, 200); //Set location of color chooser
        return theColor;
    }

    /**
     * Enable or disable the precision field depending on state of rounding button.
     */
    private void checkRounding() {
        if (!isRoundingBox.isSelected()) {
            precisionField.setEnabled(false);
        } else {
            precisionField.setEnabled(true);
        }
    }

    /**
     * Enable or disable the set button depending on the update method selection.
     */
    private void checkScript() {
        if (cbUpdateMethod.getSelectedItem().toString().equalsIgnoreCase("Script Based")) {
            scriptButton.setEnabled(true);
        } else {
            scriptButton.setEnabled(false);
        }
    }

    /**
     * Restores the changed fields to their previous values used when user cancels out of the dialog.
     */
    public void returnToCurrentPrefs() {
        networkPanel.setBackgroundColor(new Color(NetworkPreferences.getBackgroundColor()));
        networkPanel.setLineColor(new Color(NetworkPreferences.getLineColor()));
        networkPanel.setHotColor(NetworkPreferences.getHotColor());
        networkPanel.setCoolColor(NetworkPreferences.getCoolColor());
        networkPanel.setExcitatoryColor(new Color(NetworkPreferences.getExcitatoryColor()));
        networkPanel.setInhibitoryColor(new Color(NetworkPreferences.getInhibitoryColor()));
        SelectionMarquee.setMarqueeColor(new Color(NetworkPreferences.getLassoColor()));
        SelectionHandle.setSelectionColor(new Color(NetworkPreferences.getSelectionColor()));
        networkPanel.setSignalColor(new Color(NetworkPreferences.getSignalColor()));
        networkPanel.setSpikingColor(new Color(NetworkPreferences.getSpikingColor()));
        networkPanel.setZeroWeightColor(new Color(NetworkPreferences.getZeroWeightColor()));
        networkPanel.setMaxDiameter(NetworkPreferences.getMaxDiameter());
        networkPanel.setMinDiameter(NetworkPreferences.getMinDiameter());
        networkPanel.getRootNetwork().setTimeStep(NetworkPreferences.getTimeStep());
        networkPanel.getRootNetwork().setPrecision(NetworkPreferences.getPrecision());
        networkPanel.setNudgeAmount(NetworkPreferences.getNudgeAmount());
        networkPanel.getRootNetwork().setUsingTabs(NetworkPreferences.getUsingIndent());
        networkPanel.resetColors();
        setIndicatorColor();
        networkPanel.resetSynapseDiameters();
        fillFieldValues();
    }

    /**
     * Sets selected preferences as user defaults to be used each time program is launched.
     * Called when "ok" is pressed.
     */
    public void setAsDefault() {
        NetworkPreferences.setBackgroundColor(networkPanel.getBackground().getRGB());
        NetworkPreferences.setLineColor(networkPanel.getLineColor().getRGB());
        NetworkPreferences.setHotColor(networkPanel.getHotColor());
        NetworkPreferences.setCoolColor(networkPanel.getCoolColor());
        NetworkPreferences.setExcitatoryColor(networkPanel.getExcitatoryColor().getRGB());
        NetworkPreferences.setInhibitoryColor(networkPanel.getInhibitoryColor().getRGB());
        NetworkPreferences.setLassoColor(SelectionMarquee.getMarqueeColor().getRGB());
        NetworkPreferences.setSelectionColor(SelectionHandle.getSelectionColor().getRGB());
        NetworkPreferences.setSignalColor(networkPanel.getSignalColor().getRGB());
        NetworkPreferences.setSpikingColor(networkPanel.getSpikingColor().getRGB());
        NetworkPreferences.setZeroWeightColor(networkPanel.getZeroWeightColor().getRGB());
        NetworkPreferences.setMaxDiameter(networkPanel.getMaxDiameter());
        NetworkPreferences.setMinDiameter(networkPanel.getMinDiameter());
        NetworkPreferences.setTimeStep(networkPanel.getRootNetwork().getTimeStep());
        NetworkPreferences.setPrecision(networkPanel.getRootNetwork().getPrecision());
        NetworkPreferences.setUsingIndent(networkPanel.getRootNetwork().getUsingTabs());
        NetworkPreferences.setNudgeAmount(networkPanel.getNudgeAmount());
    }

    /**
     * Set the color indicator based on the current selection  in the combo box.
     */
    private void setIndicatorColor() {
        if (cbChangeColor.getSelectedItem().toString().equals(BACKGROUND)) {

            colorIndicator.setBackground(networkPanel.getBackground());

        } else if (cbChangeColor.getSelectedItem().toString().equals(LINE)) {

            colorIndicator.setBackground(networkPanel.getLineColor());

        } else if (cbChangeColor.getSelectedItem().toString().equals(HOTNODE)) {

            colorIndicator.setBackground(Utils.floatToHue(networkPanel.getHotColor()));

        } else if (cbChangeColor.getSelectedItem().toString().equals(COOLNODE)) {

            colorIndicator.setBackground(Utils.floatToHue(networkPanel.getCoolColor()));

        } else if (cbChangeColor.getSelectedItem().toString().equals(EXCITATORY)) {

            colorIndicator.setBackground(networkPanel.getExcitatoryColor());

        } else if (cbChangeColor.getSelectedItem().toString().equals(INHIBITORY)) {

            colorIndicator.setBackground(networkPanel.getInhibitoryColor());

        } else if (cbChangeColor.getSelectedItem().toString().equals(LASSO)) {

            colorIndicator.setBackground(SelectionMarquee.getMarqueeColor());

        } else if (cbChangeColor.getSelectedItem().toString().equals(SELECTION)) {

            colorIndicator.setBackground(SelectionHandle.getSelectionColor());

        } else if (cbChangeColor.getSelectedItem().toString().equals(SIGNAL)) {

            colorIndicator.setBackground(networkPanel.getSignalColor());

        } else if (cbChangeColor.getSelectedItem().toString().equals(SPIKE)) {

            colorIndicator.setBackground(networkPanel.getSpikingColor());

        } else if (cbChangeColor.getSelectedItem().toString().equals(ZERO)) {

            colorIndicator.setBackground(networkPanel.getZeroWeightColor());

        }
    }

}