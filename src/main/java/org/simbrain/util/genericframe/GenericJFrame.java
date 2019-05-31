package org.simbrain.util.genericframe;

import javax.swing.*;
import java.beans.PropertyVetoException;

/**
 * JFrame which implements Generic Frame.
 */
public class GenericJFrame extends JFrame implements GenericFrame {

    public void setIcon(boolean b) throws PropertyVetoException {
        this.setIcon(b);
    }

    @Override
    public void setMaximizable(boolean b) {
    }
}
