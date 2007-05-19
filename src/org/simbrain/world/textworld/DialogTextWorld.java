/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/Documentation/docs/SimbrainDocs.html#Credits
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
package org.simbrain.world.textworld;

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
 * <b>DialogTextWorld</b> displays a dialog for setting textworld preferences such as how to
 * parse text fields.
 *
 */
public class DialogTextWorld extends StandardDialog implements ActionListener {

    /** Checkbox for how to parse text. */
    private JCheckBox ckParse = new JCheckBox();
    /** Does enter send current line of text to be read. */
    private JCheckBox ckEnter = new JCheckBox();
    /** Button for setting highlight color. */
    private JButton bnColor = new JButton("Set");
    /** Text field for setting pause time. */
    private JTextField tfPauseTime = new JTextField();
    /** Layout panel for dialog. */
    private LabelledItemPanel panel = new LabelledItemPanel();
    /** Instance of TextWorld. */
    private TextWorld world;

    /**
     * Dialog Constructor.
     * @param wd Current TextWorld
     */
    public DialogTextWorld(final TextWorld wd) {
        world = wd;
        init();
        this.pack();
        this.setLocationRelativeTo(null);
    }

    /**
     * Initializes dialog.
     */
    private void init() {
        this.fillFieldValues();
        setTitle("TextWorld Dialog");
        bnColor.addActionListener(this);
        bnColor.setActionCommand("color");
        panel.addItem("Parse by character", ckParse);
        panel.addItem("Enter sends current line", ckEnter);
        panel.addItem("Highlight color", bnColor);
        panel.addItem("Pause time", tfPauseTime);
        setContentPane(panel);
    }

    /**
     * Show the color pallette and get a color.
     *
     * @return selected color
     */
    public Color getColor() {
        JColorChooser colorChooser = new JColorChooser();
        Color theColor = JColorChooser.showDialog(this, "Choose Color", world.getHilightColor());
        colorChooser.setLocation(200, 200); //Set location of color chooser

        return theColor;
    }

    /**
     * Fills the fields with the current values.
     */
    private void fillFieldValues() {
        ckParse.setSelected(world.getParseChar());
        ckEnter.setSelected(world.getSendEnter());
        tfPauseTime.setText(Integer.toString(world.getPauseTime()));
    }

    /**
     * Commits any changes made in dialog.
     */
    public void commitChanges() {
        world.setParseChar(ckParse.isSelected());
        world.setSendEnter(ckEnter.isSelected());
        world.setPauseTime(Integer.parseInt(tfPauseTime.getText()));
    }

    /**
     * @param e ActionEvent for which to respond.
     */
    public void actionPerformed(final ActionEvent e) {
        Object o = e.getActionCommand();

        if (o == "color") {
            world.setHilightColor(getColor());
        }
    }
}
