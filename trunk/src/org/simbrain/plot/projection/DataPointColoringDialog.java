/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.plot.projection;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.projection.DataColoringManager;
import org.simbrain.util.widgets.ShowHelpAction;

/**
 * DataPointColoringDialog is a dialog box for making changes to the coloring
 * method in projection plot.
 *
 * @author Lam Nguyen
 *
 */

public class DataPointColoringDialog extends StandardDialog implements
        ActionListener {

    /** String of coloring methods. */
    private String[] projectionMethod = { "None", "DecayTrail", "Frequency" };

    /** Projection coloring manager. */
    private DataColoringManager colorManager;

    /** The projection model. */
    private ProjectionModel projectionModel;

    /** The base color */
    private Color baseColor;

    /** The hot color. */
    private Color hotColor;

    /** Button to edit base color. */
    private JButton baseColorButton = new JButton("Base color");

    /** Button to edit hot color. */
    private JButton hotColorButton = new JButton("Hot color");

    /** Base color indicator. */
    private JPanel baseColorIndicator = new JPanel();

    /** Hot color indicator. */
    private JPanel hotColorIndicator = new JPanel();

    /** Hot color on/off toggle. */
    private JCheckBox hotPointCheckBox = new JCheckBox();

    /** Text field to edit floor. */
    private JTextField floor = new JTextField("" + 1);

    /** Text field to edit ceiling. */
    private JTextField ceiling = new JTextField("" + 10);

    /** Text field to edit increment amount. */
    private JTextField incrementAmount = new JTextField("" + .5);

    /** Text field to edit decrement amount. */
    private JTextField decrementAmount = new JTextField("" + .2);

    /** Combo box to select projection type. */
    private JComboBox coloringMethod = new JComboBox(projectionMethod);

    /** Panel for changing parameters based on selected coloring method. */
    private LabelledItemPanel currentColoringPanel;

    /** Main dialog box. */
    private Box mainPanel = Box.createVerticalBox();

    /**
     * Dialog constructor.
     *
     * @param projectionModel the projection model
     */
    public DataPointColoringDialog(ProjectionModel projectionModel) {
        this.projectionModel = projectionModel;
        colorManager = projectionModel.getProjector().getColorManager();
        init();
    }

    /**
     * Initialize the dialog. Fill field values based on state of coloring
     * manager.
     */
    private void init() {
        setTitle("Data Point Coloring Settings");

        // Set up coloring method region of panel
        coloringMethod.setSelectedItem(colorManager.getColoringMethodString());
        coloringMethod.addActionListener(this);
        Box methodPanel = Box.createVerticalBox();
        Box cbPanel = Box.createHorizontalBox();
        cbPanel.add(new JLabel("Coloring method:"));
        cbPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        cbPanel.add(coloringMethod);
        methodPanel.add(cbPanel);
        methodPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        methodPanel.add(new JSeparator(JSeparator.HORIZONTAL));

        // Set up hot point check box
        Box hotPointBox = Box.createHorizontalBox();
        hotPointBox.add(new JLabel("Hot point mode"));
        hotPointBox.add(Box.createRigidArea(new Dimension(10, 0)));
        hotPointBox.add(hotPointCheckBox);
        hotPointCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (hotPointCheckBox.isSelected() == true) {
                    hotColorButton.setEnabled(true);
                    hotColorIndicator.setEnabled(true);
                } else {
                    hotColorButton.setEnabled(false);
                    hotColorIndicator.setEnabled(false);
                }
            }
        });

        // Set up color widgets
        baseColor = colorManager.getBaseColor();
        hotColor = colorManager.getHotColor();
        baseColorIndicator.setBackground(baseColor);
        hotColorIndicator.setBackground(hotColor);
        baseColorIndicator.setSize(50, 50);
        hotColorIndicator.setSize(50, 50);
        baseColorIndicator.setBorder(BorderFactory
                .createLineBorder(Color.black));
        hotColorIndicator
                .setBorder(BorderFactory.createLineBorder(Color.black));
        if (colorManager.getHotPointMode() == true) {
            hotPointCheckBox.setSelected(true);
            hotColorButton.setEnabled(true);
            hotColorIndicator.setEnabled(true);
        } else {
            hotPointCheckBox.setSelected(false);
            hotColorButton.setEnabled(false);
            hotColorIndicator.setEnabled(false);
        }
        floor.setText("" + Double.toString(colorManager.getFloor()));
        ceiling.setText("" + Double.toString(colorManager.getCeiling()));
        incrementAmount.setText(""
                + Double.toString(colorManager.getIncrementAmount()));
        decrementAmount.setText(""
                + Double.toString(colorManager.getDecrementAmount()));

        baseColorIndicator.setBackground(colorManager.getBaseColor());
        hotColorIndicator.setBackground(colorManager.getHotColor());
        baseColorButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                Color newColor = JColorChooser.showDialog(null,
                        "Choose base color", colorManager.getBaseColor());
                if (newColor == null) {
                    newColor = baseColorIndicator.getBackground();
                }
                baseColor = newColor;
                baseColorIndicator.setBackground(newColor);
            }
        });
        hotColorButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                Color newColor = JColorChooser.showDialog(null,
                        "Choose hot color", colorManager.getHotColor());
                if (newColor == null) {
                    newColor = hotColorIndicator.getBackground();
                }
                hotColor = newColor;
                hotColorIndicator.setBackground(newColor);
            }
        });

        // Initialize based on combo box
        initPanel();

        // Set up help button
        ShowHelpAction helpAction = new ShowHelpAction(
                "Pages/Plot/projection.html");
        addButton(new JButton(helpAction));

        // Assemble color panel
        JPanel baseColorPanel = new JPanel();
        JPanel hotColorPanel = new JPanel();
        baseColorPanel.add(baseColorButton);
        baseColorPanel.add(baseColorIndicator);
        hotColorPanel.add(hotColorButton);
        hotColorPanel.add(hotColorIndicator);

        mainPanel.add(methodPanel);
        mainPanel.add(hotPointBox);
        mainPanel.add(baseColorPanel);
        mainPanel.add(hotColorPanel);
        mainPanel.add(currentColoringPanel);
        setContentPane(mainPanel);
    }

    /**
     * Initialize the current coloring panel based on combo box selection.
     */
    private void initPanel() {
        if (coloringMethod.getSelectedItem() == "None") {
            clearColoringPanel();
            currentColoringPanel = getNonePanel();
            baseColorButton.setVisible(true);
            baseColorIndicator.setVisible(true);
            mainPanel.add(currentColoringPanel);
        } else if (coloringMethod.getSelectedItem() == "DecayTrail") {
            clearColoringPanel();
            currentColoringPanel = getDecayTrailPanel();
            baseColorButton.setVisible(true);
            baseColorIndicator.setVisible(true);
            mainPanel.add(currentColoringPanel);
        } else if (coloringMethod.getSelectedItem() == "Frequency") {
            clearColoringPanel();
            currentColoringPanel = getFrequencyPanel();
            baseColorButton.setVisible(true);
            baseColorIndicator.setVisible(true);
            mainPanel.add(currentColoringPanel);
        }
        pack();
        setLocationRelativeTo(null);

    }

    /**
     * Create panel for "None" coloring method.
     *
     * @return coloringPanel the coloringPanel
     */
    private LabelledItemPanel getNonePanel() {
        LabelledItemPanel coloringPanel = new LabelledItemPanel();
        return coloringPanel;
    }

    /**
     * Create panel for "DecayTrail" coloring method.
     *
     * @return coloringPanel the coloringPanel
     */
    private LabelledItemPanel getDecayTrailPanel() {
        LabelledItemPanel coloringPanel = new LabelledItemPanel();
        coloringPanel.addItem("Ceiling", ceiling);
        coloringPanel.addItem("Floor", floor);
        coloringPanel.addItem("Decrement Amount", decrementAmount);
        return coloringPanel;
    }

    /**
     * Create panel for "Frequency" coloring method.
     *
     * @return coloringPanel the coloringPanel
     */
    private LabelledItemPanel getFrequencyPanel() {
        LabelledItemPanel coloringPanel = new LabelledItemPanel();
        coloringPanel.addItem("Ceiling", ceiling);
        coloringPanel.addItem("Increment amount", incrementAmount);
        return coloringPanel;
    }

    /**
     * Clear the current coloring panel if null.
     */
    private void clearColoringPanel() {
        if (currentColoringPanel != null) {
            mainPanel.remove(currentColoringPanel);
        }
    }

    @Override
    protected void closeDialogOk() {
        commitChanges();
        projectionModel.getProjector().fireProjectorColorsChanged();
        super.closeDialogOk();
    }

    /**
     * Helper method to check that a value is between 0 and 1 and display a
     * warning if not.
     *
     * @param paramName parameter name to display in warning dialog
     * @param val value to check
     * @return true if the value is in range, false otherwise
     */
    private boolean betweenZeroAndOne(final String paramName, final double val) {
        if ((val < 0) || (val > 1)) {
            JOptionPane.showMessageDialog(null, paramName
                    + "should be between 0 and 1", "Warning!",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * Commit changes to coloringManager based on selected coloring method.
     */
    private void commitChanges() {
        try {
            double floorVal = Double.parseDouble(floor.getText());
            double ceilingVal = Double.parseDouble(ceiling.getText());
            double decAmount = Double.parseDouble(decrementAmount.getText());
            double incAmount = Double.parseDouble(incrementAmount.getText());

            if (!betweenZeroAndOne("Floor", floorVal)) {
                return;
            }
            if (!betweenZeroAndOne("Ceiling", ceilingVal)) {
                return;
            }
            if (!betweenZeroAndOne("Decrement Amount", decAmount)) {
                return;
            }
            if (!betweenZeroAndOne("Increment Amount", incAmount)) {
                return;
            }

            if (hotPointCheckBox.isSelected()) {
                colorManager.setHotPointMode(true);
                colorManager.setHotColor(hotColor);
            } else {
                colorManager.setHotPointMode(false);
            }
            if (coloringMethod.getSelectedItem() == "None") {
                colorManager.setColoringMethod("None");
                colorManager.setBaseColor(baseColor);
            }
            if (coloringMethod.getSelectedItem() == "DecayTrail") {
                colorManager.setColoringMethod("DecayTrail");
                colorManager.setBaseColor(baseColor);
                colorManager.setDecrementAmount(decAmount);
                colorManager.setCeiling(ceilingVal);
                colorManager.setFloor(floorVal);
            }
            if (coloringMethod.getSelectedItem() == "Frequency") {
                colorManager.setColoringMethod("Frequency");
                colorManager.setBaseColor(baseColor);
                colorManager.setIncrementAmount(incAmount);
                colorManager.setCeiling(ceilingVal);
            }
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(null, "Non-numeric field values:"
                    + "\nNetwork construction failed.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            nfe.printStackTrace();
        }

    }

    /**
     * Reinitialize current coloring panel.
     *
     * @param e the ActionEvent
     */
    public void actionPerformed(final ActionEvent e) {
        initPanel();
    }

}
