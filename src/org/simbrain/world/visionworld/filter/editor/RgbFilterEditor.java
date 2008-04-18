package org.simbrain.world.visionworld.filter.editor;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.simbrain.world.visionworld.Filter;
import org.simbrain.world.visionworld.filter.RgbFilter;

/**
 * FilterEditor that creates RgbFilters.
 * 
 * @author Matt Watson
 */
public class RgbFilterEditor implements FilterEditor {
    /** The spacing of labels and text fields. */
    private static final int SPACING = 8;
    /** The width of text fields. */
    private static final int TEXT_FIELD_WIDTH = 72;
    /** The spacing in the borders. */
    private static final int BORDER_SPACING = 6;
    
    /** The panel containing the components. */
    JPanel panel = new JPanel();
    /** The layout. */
    SpringLayout layout = new SpringLayout();
    /** The red threshold. */
    JTextField red = new JTextField("0");
    /** The green threshold. */
    JTextField green = new JTextField("0");
    /** The blue threshold. */
    JTextField blue = new JTextField("0");
    /** The output lower bound. */
    JTextField lower = new JTextField("0");
    /** The output upper bound. */
    JTextField upper = new JTextField("1");
    
    /**
     * Creates a new instance.
     */
    public RgbFilterEditor() {
        panel.setLayout(layout);
        
        panel.setBorder(new CompoundBorder(new TitledBorder("RGB Filter Editor"),
            new EmptyBorder(BORDER_SPACING, BORDER_SPACING, BORDER_SPACING, BORDER_SPACING)));
        
        JLabel redLabel = new JLabel("Red");
        JLabel greenLabel = new JLabel("Green");
        JLabel blueLabel = new JLabel("Blue");
        JLabel lowerLabel = new JLabel("Output Lower Bound");
        JLabel upperLabel = new JLabel("Output Upper Bound");
        
        add(redLabel, red);
        add(greenLabel, green);
        add(blueLabel, blue);
        add(lowerLabel, lower);
        add(upperLabel, upper);
    }
    
    /** The previous label added. */
    private JLabel previous;
    
    /**
     * Adds a label and a field combination.
     * 
     * @param label The label to add.
     * @param field The field to add.
     */
    private void add(final JLabel label, final JTextField field) {
        panel.add(label);
        panel.add(field);
        
        if (previous == null) {
            layout.putConstraint(SpringLayout.NORTH, panel, -SPACING, SpringLayout.NORTH, label);
        } else {
            layout.putConstraint(SpringLayout.NORTH, label, SPACING, SpringLayout.SOUTH, previous);
        }
        
        layout.putConstraint(SpringLayout.WEST, panel, -SPACING, SpringLayout.WEST, label);
        layout.putConstraint(SpringLayout.SOUTH, panel, SPACING, SpringLayout.SOUTH, label);
        layout.putConstraint(SpringLayout.EAST, field, -SPACING, SpringLayout.EAST, panel);
        layout.putConstraint(SpringLayout.WEST, field, -(TEXT_FIELD_WIDTH + SPACING),
            SpringLayout.EAST, panel);
        layout.putConstraint(SpringLayout.SOUTH, field, SPACING, SpringLayout.SOUTH, label);
        
        previous = label;
    }
    
    /**
     * {@inheritDoc}
     */
    public Filter createFilter() throws FilterEditorException {
        int red = getIntValue(this.red);
        int green = getIntValue(this.green);
        int blue = getIntValue(this.blue);
        int lower = getIntValue(this.lower);
        int upper = getIntValue(this.upper);
        
        return new RgbFilter(red, green, blue, lower, upper);
    }
    
    /**
     * Returns the int value of the provided text field.
     * 
     * @param panel The panel to get value from.
     * @return The int value.
     */
    int getIntValue(final JTextField panel) {
        try {
            return Integer.parseInt(panel.getText());
        } catch (NumberFormatException e) {
            panel.setBackground(Color.RED);
            throw e;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public Component getEditorComponent() {
        return panel;
    }
    
    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "RGB Filter";
    }
}
