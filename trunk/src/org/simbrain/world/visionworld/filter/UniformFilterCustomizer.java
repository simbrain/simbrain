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
package org.simbrain.world.visionworld.filter;

import java.beans.Customizer;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.dishevelled.layout.LabelFieldLayout;

/**
 * Customizer for UniformFilter.
 */
public class UniformFilterCustomizer
    extends JPanel
    implements Customizer {

    /** Uniform filter. */
    private UniformFilter uniformFilter;

    /** Value editor. */
    private JTextField value;

    /** Document listener. */
    private DocumentListener documentListener;

    /** Property change listener. */
    private PropertyChangeListener propertyChangeListener;

    /** Property change support. */
    private PropertyChangeSupport propertyChangeSupport;


    /**
     * Create a new customizer for UniformFilter.
     */
    public UniformFilterCustomizer() {
        super();
        initComponents();
        layoutComponents();

        documentListener = new DocumentListener() {
                /** {@inheritDoc} */
                public void changedUpdate(final DocumentEvent event) {
                    updateValue();
                }

                /** {@inheritDoc} */
                public void insertUpdate(final DocumentEvent event) {
                    updateValue();
                }

                /** {@inheritDoc} */
                public void removeUpdate(final DocumentEvent event) {
                    updateValue();
                }
            };

        propertyChangeListener = new PropertyChangeListener() {
                /** {@inheritDoc} */
                public void propertyChange(final PropertyChangeEvent event) {
                    value.setText(event.getNewValue().toString());
                }
            };

        propertyChangeSupport = new PropertyChangeSupport(this);
    }


    /**
     * Initialize components.
     */
    private void initComponents() {
        value = new JTextField();
        value.getDocument().addDocumentListener(documentListener);
    }

    /**
     * Layout components.
     */
    private void layoutComponents() {
        Border titledBorder = new TitledBorder("Uniform filter");
        Border emptyBorder = new EmptyBorder(6, 6, 6, 6);
        setBorder(new CompoundBorder(titledBorder, emptyBorder));
        LabelFieldLayout l = new LabelFieldLayout();
        setLayout(l);
        add(new JLabel("Value:"), l.label());
        add(value, l.field());
    }

    /** {@inheritDoc} */
    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /** {@inheritDoc} */
    public void removePropertyChangeListener(final PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /** {@inheritDoc} */
    public void setObject(final Object object) {
        if (!(object instanceof UniformFilter)) {
            throw new IllegalArgumentException("object not instance of UniformFilter");
        }
        uniformFilter = (UniformFilter) object;
        uniformFilter.addPropertyChangeListener("value", propertyChangeListener);
        value.setText(String.valueOf(uniformFilter.getValue()));
    }

    /**
     * Update value if necessary.
     */
    private void updateValue() {
        double oldValue = uniformFilter.getValue();
        try {
            double newValue = Double.valueOf(value.getText());
            if (oldValue != newValue) {
                uniformFilter.setValue(newValue);
                propertyChangeSupport.firePropertyChange("value", oldValue, uniformFilter.getValue());
            }
        }
        catch (NumberFormatException e) {
            // ignore
        }
    }

    /** {@inheritDoc} */
    protected void finalize() throws Throwable {
        uniformFilter.removePropertyChangeListener("value", propertyChangeListener);
    }
}
