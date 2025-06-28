package org.simbrain.util;

import javax.swing.*;
import java.awt.*;

/**
 * <b>ComboBoxRenderer</b> formats and inserts images into combo boxes.
 */
public class ComboBoxRenderer extends JLabel implements ListCellRenderer {

    /**
     * Combo box renderer.
     */
    public ComboBoxRenderer() {
        setOpaque(true);
        setHorizontalAlignment(LEFT);
        setVerticalAlignment(CENTER);
    }

    /**
     * Puts images next to images in a combo box.
     *
     * @param list         List
     * @param value        Objects to insert
     * @param index        Where to insert image and text
     * @param isSelected   Is cell selected
     * @param cellHasFocus Cell has focus
     * @return Returns the componet to be put into combo box
     */
    public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        ImageIcon icon = (ImageIcon) value;
        setText(icon.getDescription());
        setIcon(icon);

        return this;
    }
}
