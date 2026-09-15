package org.simbrain.util.widgets;

import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.Theme;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A button that opens a color chooser next to a swatch showing the current color. Supports a null state, shown as
 * "...", for when several edited objects have different colors; choosing a color clears it.
 */
public class ColorSelector extends JPanel {

    /**
     * The current color. Default to black
     */
    private Color color = Color.BLACK;

    /**
     * True when no single color is being shown.
     */
    private boolean isNull = false;

    /**
     * The button to open JColorChooser
     */
    private final JButton colorButton = new JButton("Set Color");

    /**
     * An indicator showing the current color
     */
    private final JPanel colorIndicator = new JPanel();

    /**
     * Shown in place of the indicator while in the null state.
     */
    private final JLabel nullLabel = new JLabel(SimbrainConstants.NULL_STRING);

    private final List<Consumer<Color>> changeListeners = new ArrayList<>();

    /**
     * Default constructor
     */
    public ColorSelector() {
        super();
        add(colorButton);
        colorIndicator.setPreferredSize(new Dimension(20, 20));
        colorIndicator.setBorder(BorderFactory.createLineBorder(Theme.getDivider()));
        colorIndicator.setBackground(color);
        add(colorIndicator);
        nullLabel.setVisible(false);
        add(nullLabel);
        colorButton.addActionListener(arg0 -> {
            var selectedColor = JColorChooser.showDialog(this, "Choose Color", color);
            if (selectedColor != null) {
                setValue(selectedColor);
            }
        });
    }

    /**
     * Sets the color, leaves the null state, and notifies change listeners.
     */
    public void setValue(Color color) {
        this.color = color;
        this.isNull = false;
        this.colorIndicator.setBackground(color);
        this.colorIndicator.setVisible(true);
        this.nullLabel.setVisible(false);
        changeListeners.forEach(listener -> listener.accept(color));
    }

    public Color getValue() {
        return this.color;
    }

    /**
     * Show "..." instead of a color, until a color is chosen.
     */
    public void setNull() {
        isNull = true;
        colorIndicator.setVisible(false);
        nullLabel.setVisible(true);
    }

    public boolean isNull() {
        return isNull;
    }

    /**
     * Called whenever a color is set, whether by the chooser or by {@link #setValue(Color)}.
     */
    public void addChangeListener(Consumer<Color> listener) {
        changeListeners.add(listener);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        colorButton.setEnabled(enabled);
    }
}
