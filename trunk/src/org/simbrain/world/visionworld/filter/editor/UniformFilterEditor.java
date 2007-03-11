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
import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.simbrain.world.visionworld.Filter;

import org.simbrain.world.visionworld.filter.UniformFilter;
import org.simbrain.world.visionworld.sensormatrix.editor.*;

/**
 * Uniform filter editor.
 */
public final class UniformFilterEditor
    extends JPanel
    implements FilterEditor {

    /** Value. */
    private JTextField value;

    /** Display name. */
    private static final String DISPLAY_NAME = "Uniform filter";

    /** Description. */
    private static final String DESCRIPTION = "Returns an uniform value for every image";

    /** Default value. */
    private static final double DEFAULT_VALUE = 1.0d;

    /** Empty insets. */
    private static final Insets EMPTY_INSETS = new Insets(0, 0, 0, 0);

    /** Field insets. */
    private static final Insets FIELD_INSETS = new Insets(0, 0, 6, 0);

    /** Label insets. */
    private static final Insets LABEL_INSETS = new Insets(0, 0, 6, 0);


    /**
     * Create a new uniform filter editor.
     */
    public UniformFilterEditor()
    {
        super();

        initComponents();
        layoutComponents();
    }


    /**
     * Initialize components.
     */
    private void initComponents() {
        value = new JTextField();
    }

    /**
     * Layout components.
     */
    private void layoutComponents() {
        setLayout(new GridBagLayout());
        setBorder(new CompoundBorder(new TitledBorder(DISPLAY_NAME), new EmptyBorder(6, 6, 6, 6)));
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
        add(new JLabel("Value"), c);

        c.insets = FIELD_INSETS;
        c.gridx = 1;
        c.weightx = 0.66f;
        add(value, c);

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
        value.setText(String.valueOf(DEFAULT_VALUE));
        return this;
    }

    /** {@inheritDoc} */
    public Filter createFilter() throws FilterEditorException {
        try {
            return new UniformFilter(Double.valueOf(value.getText()));
        }
        catch (NumberFormatException e) {
            throw new FilterEditorException(e);
        }
    }

    /** {@inheritDoc} */
    public String toString() {
        return DISPLAY_NAME;
    }
}
