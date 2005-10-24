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

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * <b>DialogOdorWorldWall</b> is a dialog box for setting the properties of a wall.
 */
public class DialogOdorWorldWall extends StandardDialog implements ActionListener, ChangeListener {
    private OdorWorld world = null;
    private Wall wall = null;
    private LabelledItemPanel topPanel = new LabelledItemPanel();
    private JButton colorButton = new JButton("Set");
    private JSlider width = new JSlider();
    private JSlider height = new JSlider();
    private JTextField resurrectionProb = new JTextField();
    PanelStimulus stimPanel;
    private LabelledItemPanel miscPanel = new LabelledItemPanel();
    private JTextField bitesToDie = new JTextField();
    private JCheckBox edible = new JCheckBox();

    /**
     * This method is the default constructor.
     */
    public DialogOdorWorldWall(OdorWorld dworld, Wall selectedWall) {
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
        this.setLocation(500, 0); //Sets location of network dialog

        //Set up sliders
        width.setMajorTickSpacing(25);
        width.setPaintTicks(true);
        width.setPaintLabels(true);
        height.setMajorTickSpacing(25);
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

        stimPanel = new PanelStimulus(wall);
        stimPanel.getTabbedPane().insertTab("Wall", null, topPanel, null, 0);
        stimPanel.getTabbedPane().addTab("Miscellaneous", miscPanel);
        stimPanel.getTabbedPane().setSelectedIndex(0);
        setContentPane(stimPanel);
    }

    /**
     * Respond to button pressing events
     */
    public void actionPerformed(ActionEvent e) {
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
     * Populate fields with current data
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
     * (non-Javadoc)
     *
     * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
     */
    public void stateChanged(ChangeEvent e) {
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
     * Show the color pallette and get a color
     *
     * @return selected color
     */
    public Color getColor() {
        JColorChooser colorChooser = new JColorChooser();
        Color theColor = JColorChooser.showDialog(this, "Choose Color", Color.BLACK);
        colorChooser.setLocation(200, 200); //Set location of color chooser

        return theColor;
    }

    public void commitChanges() {
        wall.setEdible(edible.isSelected());

        if (!edible.isSelected()) {
            wall.setBites(0);
        }

        wall.setBitesToDie(Integer.parseInt(bitesToDie.getText()));
        wall.setResurrectionProb(Double.parseDouble(resurrectionProb.getText()));
    }
}
