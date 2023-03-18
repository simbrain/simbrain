package org.simbrain.util.genericframe;

import javax.swing.*;
import java.awt.*;

/**
 * JInternalFrame which implements Generic Frame
 */
public class GenericJInternalFrame extends JInternalFrame implements GenericFrame {

    public GenericJInternalFrame() {
        addPropertyChangeListener(evt -> {
            if (JInternalFrame.IS_MAXIMUM_PROPERTY.equals(evt.getPropertyName())) {
                if (Boolean.TRUE.equals(evt.getNewValue())) {
                    // Frame is being maximized
                    setSize(getMaximumSize());
                    validate(); // Make sure the frame layout is updated
                }
            }
        });
    }

    @Override
    public void setLocationRelativeTo(Component c) {
    }

    @Override
    public void toFront() {
        super.toFront();
    }
}
