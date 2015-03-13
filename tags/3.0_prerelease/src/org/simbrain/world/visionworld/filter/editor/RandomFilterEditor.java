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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.simbrain.world.visionworld.Filter;
import org.simbrain.world.visionworld.filter.RandomFilter;

/**
 * Random filter editor.
 */
public final class RandomFilterEditor extends JPanel implements FilterEditor {

    /** Minimum value. */
    private JTextField minimumValue;

    /** Maximum value. */
    private JTextField maximumValue;

    /** Display name. */
    private static final String DISPLAY_NAME = "Random filter";

    /** Description. */
    private static final String DESCRIPTION = "Returns a random value between minimum value and maximum value for every image";

    /** Default minimum value. */
    private static final double DEFAULT_MINIMUM_VALUE = -1.0d;

    /** Default maximum value. */
    private static final double DEFAULT_MAXIMUM_VALUE = 1.0d;

    /** Empty insets. */
    private static final Insets EMPTY_INSETS = new Insets(0, 0, 0, 0);

    /** Field insets. */
    private static final Insets FIELD_INSETS = new Insets(0, 0, 6, 0);

    /** Label insets. */
    private static final Insets LABEL_INSETS = new Insets(0, 0, 6, 0);

    /**
     * Create a new random filter editor.
     */
    public RandomFilterEditor() {
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
        setLayout(new GridBagLayout());
        setBorder(new CompoundBorder(new TitledBorder(DISPLAY_NAME),
                new EmptyBorder(6, 6, 6, 6)));
        setToolTipText(DESCRIPTION);
        GridBagConstraints c = new GridBagConstraints();

        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridheight = 1;
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.insets = LABEL_INSETS;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.33f;
        c.weighty = 0;
        add(new JLabel("Minimum value"), c);

        c.insets = FIELD_INSETS;
        c.gridx = 1;
        c.weightx = 0.66f;
        add(minimumValue, c);

        c.insets = LABEL_INSETS;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0.33f;
        add(new JLabel("Maximum value"), c);

        c.insets = FIELD_INSETS;
        c.gridx = 1;
        c.weightx = 0.66f;
        add(maximumValue, c);

        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = EMPTY_INSETS;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 1.0f;
        c.weighty = 1.0f;
        add(Box.createGlue(), c);
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
        } catch (NumberFormatException e) {
            throw new FilterEditorException(e);
        } catch (IllegalArgumentException e) {
            throw new FilterEditorException(e);
        }
    }

    /** {@inheritDoc} */
    public String toString() {
        return DISPLAY_NAME;
    }
}
