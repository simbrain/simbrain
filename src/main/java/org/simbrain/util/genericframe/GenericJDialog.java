package org.simbrain.util.genericframe;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyVetoException;

/**
 * JDialog which implements Generic Frame.
 */
public class GenericJDialog extends JDialog implements GenericFrame {

    public GenericJDialog(Frame parent, String title) {
        super(parent, title);
    }

    public GenericJDialog() {
    }

    public void setIcon(boolean b) throws PropertyVetoException {
    }

    @Override
    public void setMaximizable(boolean b) {
    }

}
