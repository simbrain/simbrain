/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2006 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.world.visionworld.filter.editor;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.util.LabelledItemPanel;

import org.simbrain.world.visionworld.Filter;

import org.simbrain.world.visionworld.filter.RandomFilter;

/**
 * Random filter editor.
 */
public final class RandomFilterEditor
    extends JPanel
    implements FilterEditor {

    /** Minimum value. */
    private JTextField minimumValue;

    /** Maximum value. */
    private JTextField maximumValue;

    /** Default minimum value. */
    private static final double DEFAULT_MINIMUM_VALUE = -1.0d;

    /** Default maximum value. */
    private static final double DEFAULT_MAXIMUM_VALUE = 1.0d;


    /**
     * Create a new random filter editor.
     */
    public RandomFilterEditor()
    {
        super();

        initComponents();
        layoutComponents();
    }


    /**
     * Initialize components.
     */
    private void initComponents() {
        minimumValue = new JTextField();
        maximumValue = new JTextField();
    }

    /**
     * Layout components.
     */
    private void layoutComponents() {
        setLayout(new BorderLayout());
        LabelledItemPanel valuesPanel = new LabelledItemPanel();
        valuesPanel.addItem("Minimum value", minimumValue);
        valuesPanel.addItem("Maximum value", maximumValue);
        add("Center", valuesPanel);
    }

    /** {@inheritDoc} */
    public Component getEditorComponent() {
        minimumValue.setText(String.valueOf(DEFAULT_MINIMUM_VALUE));
        maximumValue.setText(String.valueOf(DEFAULT_MAXIMUM_VALUE));
        return this;
    }

    /** {@inheritDoc} */
    public Filter createFilter() throws FilterEditorException {
        try {
            double min = Double.valueOf(minimumValue.getText());
            double max = Double.valueOf(maximumValue.getText());
            return new RandomFilter(min, max);
        }
        catch (NumberFormatException e) {
            throw new FilterEditorException(e);
        }
        catch (IllegalArgumentException e) {
            throw new FilterEditorException(e);
        }
    }
}
