package org.simbrain.util.genericframe;

import java.awt.Component;

import javax.swing.JInternalFrame;

/**
 * JInternalFrame which implements Generic Frame
 */
public class GenericJInternalFrame extends JInternalFrame implements
        GenericFrame {

    @Override
    public void setLocationRelativeTo(Component c) {
    }
}
