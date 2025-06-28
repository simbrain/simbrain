package org.simbrain.util.widgets;

import javax.swing.*;
import java.awt.*;

/**
 * Simple utility for making rows with a label and a component.
 */
public class LabelledItem extends JPanel {

    /**
     * Construct the labelled item.
     *
     * @param labelText the text for the label
     * @param component the labelled component
     */
    public LabelledItem(String labelText, JComponent component) {
        Box itemBox = Box.createHorizontalBox();
        itemBox.setAlignmentX(Box.LEFT_ALIGNMENT);
        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(100, 10));
        itemBox.add(label);
        itemBox.add(Box.createHorizontalStrut(10));
        itemBox.add(Box.createHorizontalGlue());
        itemBox.add(component);
        add(itemBox);
    }

}
