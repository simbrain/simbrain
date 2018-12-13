package org.simbrain.util.widgets;

import javax.swing.*;
import java.awt.*;

public class ColorSelector extends JPanel {

    /**
     * The current color. Default to black
     */
    private Color color = Color.BLACK;

    /**
     * The button to open JColorChooser
     */
    private JButton colorButton = new JButton("Set Color");

    /**
     * An indicator showing the current color
     */
    private JPanel colorIndicator = new JPanel();

    /**
     * Default constructor
     */
    public ColorSelector() {
        super();
        add(colorButton);
        colorIndicator.setSize(20, 20);
        colorIndicator.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        colorIndicator.setBackground(color);
        add(colorIndicator);
        colorButton.addActionListener(arg0 -> {
            color = JColorChooser.showDialog(this, "Choose Color", color);
            colorIndicator.setBackground(color);
        });
    }
    
    public void setValue(Color color) {
        this.color = color;
        this.colorIndicator.setBackground(color);
    }

    public Color getValue() {
        return this.color;
    }
}
