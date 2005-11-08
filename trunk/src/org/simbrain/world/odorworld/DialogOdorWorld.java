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
import javax.swing.JTextField;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;


/**
 * <b>DialogOdorWorld</b> is used to set the enivronment's parameters,  in particular, the way stimuli are constructed
 * to be sent the network, and the way  outputs from the network are expressed in the world.
 */
public class DialogOdorWorld extends StandardDialog implements ActionListener {
    private final int initialWorldWidth = 5;
    private final int colorChooserIndent = 200;
    private OdorWorld theWorld;
    private LabelledItemPanel myContentPane = new LabelledItemPanel();
    private JTextField worldWidth = new JTextField();
    private JTextField worldHeight = new JTextField();
    private JCheckBox initiateMovement = new JCheckBox();
    private JCheckBox inhibitMovement = new JCheckBox();
    private JCheckBox useLocalBounds = new JCheckBox();
    private JCheckBox updateDrag = new JCheckBox();
    private JButton colorChoice = new JButton("Set");
    private Color theColor;

    public DialogOdorWorld(final OdorWorld wp) {
        theWorld = wp;
        init();
    }

    /**
     * This method initialises the components on the panel.
     */
    private void init() {
        setTitle("World Dialog");

        fillFieldValues();

        worldWidth.setColumns(initialWorldWidth);

        myContentPane.addItem("World Width", worldWidth);
        myContentPane.addItem("World Height", worldHeight);
        myContentPane.addItem("Moving objects initiates creature movement", initiateMovement);
        myContentPane.addItem("Objects block movement", inhibitMovement);
        myContentPane.addItem("Enable boundaries (if not, agents wrap around)", useLocalBounds);
        myContentPane.addItem("Update network while dragging objects", updateDrag);
        myContentPane.addItem("Set Background Color", colorChoice);

        setContentPane(myContentPane);

        colorChoice.addActionListener(this);
        updateDrag.addActionListener(this);
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        worldWidth.setText(Integer.toString(theWorld.getWorldWidth()));
        worldHeight.setText(Integer.toString(theWorld.getWorldHeight()));
        updateDrag.setSelected(theWorld.getUpdateWhileDragging());
        useLocalBounds.setSelected(theWorld.getUseLocalBounds());

        if (!updateDrag.isSelected()) {
            initiateMovement.setSelected(false);
            initiateMovement.setEnabled(false);
        } else {
            initiateMovement.setSelected((theWorld.getObjectDraggingInitiatesMovement()));
        }

        inhibitMovement.setSelected(theWorld.getObjectInhibitsMovement());
        theColor = new Color(theWorld.getBackgroundColor());
    }

    /**
     * Set projector values based on fields.
     */
    public void getValues() {
        theWorld.setWorldWidth(Integer.parseInt(worldWidth.getText()));
        theWorld.setWorldHeight(Integer.parseInt(worldHeight.getText()));
        theWorld.resize();
        theWorld.setUseLocalBounds(useLocalBounds.isSelected());
        theWorld.setUpdateWhileDragging(updateDrag.isSelected());
        theWorld.setObjectDraggingInitiatesMovement(initiateMovement.isSelected());
        theWorld.setObjectInhibitsMovement(inhibitMovement.isSelected());
        theWorld.setBackgroundColor(theColor.getRGB());
    }

    public void actionPerformed(final ActionEvent e) {
        if (e.getSource().equals(updateDrag)) {
            JCheckBox test = (JCheckBox) e.getSource();

            if (!test.isSelected()) {
                initiateMovement.setSelected(false);
                initiateMovement.setEnabled(false);
                repaint();
            } else if (test.isSelected()) {
                initiateMovement.setSelected((theWorld.getObjectDraggingInitiatesMovement()));
                initiateMovement.setEnabled(true);
                repaint();
            }
        } else if (e.getSource().equals(colorChoice)) {
            theColor = getColor();
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
}
