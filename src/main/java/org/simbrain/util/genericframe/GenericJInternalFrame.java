package org.simbrain.util.genericframe;

import javax.swing.*;
import java.awt.*;

/**
 * JInternalFrame which implements Generic Frame
 */
public class GenericJInternalFrame extends JInternalFrame implements GenericFrame {

    @Override
    public void setLocationRelativeTo(Component c) {
    }

    @Override
    public void toFront() {
        super.toFront();
    }
}
