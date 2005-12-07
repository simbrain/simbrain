/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.world.odorworld;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;


/**
 * <b>DialogOdorWorldWall</b> is a dialog box for setting the properties of a wall.
 */
public class DialogOdorWorldWall extends StandardDialog implements ActionListener, ChangeListener {

    /** The initial offset of the dialog. */
    private final int initialDialogPlacement = 500;

    /** The spacing of major ticks on the sliders (25%). */
    private final int majorTickSpacing = 25;

    /** The initial indent of the color chooser. */
    private final int colorChooserIndent = 200;

    /** The world in which the wall is. */
    private OdorWorld world = null;

    /** The wall for which this dialog is called. */
    private Wall wall = null;

    /** The panel containing untabbed settings. */
    private LabelledItemPanel topPanel = new LabelledItemPanel();

    /** The button for opening the color chooser. */
    private JButton colorButton = new JButton("Set");

    /** The slider that adjust the width of the wall. */
    private JSlider width = new JSlider();

    /** The slider that adjusts the height of the wall. */
    private JSlider height = new JSlider();

    /** The text field that contains the probability of resurrection per turn. */
    private JTextField resurrectionProb = new JTextField();

    /** The panel containing stimulus information. */
    private PanelStimulus stimPanel;

    /** The panel containing items not pertaining to other panels. */
    private LabelledItemPanel miscPanel = new LabelledItemPanel();

    /** The text field containing the number of bites until the wall dies (absolute, not remaining). */
    private JTextField bitesToDie = new JTextField();

    /** The checkbox representing whether or not the wall is edible. */
    private JCheckBox edible = new JCheckBox();

    /**
     * This method is the default constructor.
     * @param dworld the called from
     * @param selectedWall the wall called for
     */
    public DialogOdorWorldWall(final OdorWorld dworld, final Wall selectedWall) {
        wall = selectedWall;
        world = dworld;
        init();
    }

    /**
     * This method initialises the components on the topPanel.
     */
    private void init() {
        //Initialize Dialog
        setTitle("Wall Dialog");
        fillFieldValues();
        this.setLocation(initialDialogPlacement, 0); //Sets location of dialog

        //Set up sliders
        width.setMajorTickSpacing(majorTickSpacing);
        width.setPaintTicks(true);
        width.setPaintLabels(true);
        height.setMajorTickSpacing(majorTickSpacing);
        height.setPaintTicks(true);
        height.setPaintLabels(true);

        bitesToDie.setColumns(2);

        //Add Action Listeners
        colorButton.addActionListener(this);
        width.addChangeListener(this);
        height.addChangeListener(this);

        edible.addActionListener(this);

        //Set up topPanel
        topPanel.addItem("Set wall color (all Walls)", colorButton);
        topPanel.addItem("Width", width);
        topPanel.addItem("Height", height);

        miscPanel.addItem("Edible", edible);
        miscPanel.addItem("Bites to die", bitesToDie);
        miscPanel.addItem("Resurrection Probability", resurrectionProb);

        setStimPanel(new PanelStimulus(wall));
        getStimPanel().getTabbedPane().insertTab("Wall", null, topPanel, null, 0);
        getStimPanel().getTabbedPane().addTab("Miscellaneous", miscPanel);
        getStimPanel().getTabbedPane().setSelectedIndex(0);
        setContentPane(getStimPanel());
    }

    /**
     * Respond to button pressing events.
     * @param e the ActionEvent triggering this method
     */
    public void actionPerformed(final ActionEvent e) {
        Object o = e.getSource();

        if (o == colorButton) {
            Color theColor = getColor();

            if (theColor != null) {
                world.setWallColor(theColor.getRGB());
            }
        }

        if (o == edible) {
            bitesToDie.setEnabled(edible.isSelected());
        }
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        width.setValue(wall.getWidth());
        height.setValue(wall.getHeight());
        resurrectionProb.setText("" + wall.getResurrectionProb());
        edible.setSelected(wall.getEdible());
        bitesToDie.setText((new Integer(wall.getBitesToDie())).toString());
        bitesToDie.setEnabled(wall.getEdible());
    }

    /**
     * (non-Javadoc).
     *
     * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
     */
    public void stateChanged(final ChangeEvent e) {
        JSlider j = (JSlider) e.getSource();

        if (j == width) {
            wall.setWidth(j.getValue());
            world.repaint();
        } else if (j == height) {
            wall.setHeight(j.getValue());
            world.repaint();
        }
    }

    /**
     * Show the color pallette and get a color.
     *
     * @return selected color
     */
    public Color getColor() {
        JColorChooser colorChooser = new JColorChooser();
        Color theColor = JColorChooser.showDialog(this, "Choose Color", Color.BLACK);
        colorChooser.setLocation(colorChooserIndent, colorChooserIndent); //Set location of color chooser

        return theColor;
    }

    /**
     * Commits the changes edited here.
     */
    public void commitChanges() {
        wall.setEdible(edible.isSelected());

        if (!edible.isSelected()) {
            wall.setBites(0);
        }

        wall.setBitesToDie(Integer.parseInt(bitesToDie.getText()));
        wall.setResurrectionProb(Double.parseDouble(resurrectionProb.getText()));
    }

    /**
     * @param stimPanel The stimPanel to set.
     */
    void setStimPanel(final PanelStimulus stimPanel) {
        this.stimPanel = stimPanel;
    }

    /**
     * @return Returns the stimPanel.
     */
    PanelStimulus getStimPanel() {
        return stimPanel;
    }
}
