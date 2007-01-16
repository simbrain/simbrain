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

import org.simbrain.world.visionworld.filter.UniformFilter;

/**
 * Uniform filter editor.
 */
public final class UniformFilterEditor
    extends JPanel
    implements FilterEditor {

    /** Value. */
    private JTextField value;

    /** Default value. */
    private static final double DEFAULT_VALUE = 1.0d;


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
        setLayout(new BorderLayout());
        LabelledItemPanel valuePanel = new LabelledItemPanel();
        valuePanel.addItem("Value", value);
        add("Center", valuePanel);
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
}
