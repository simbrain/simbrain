package org.simbrain.util.genericframe;

import java.beans.PropertyVetoException;

import javax.swing.JFrame;

/**
 * JFrame which implements Generic Frame
 */
public class GenericJFrame extends JFrame implements GenericFrame {

    public void setIcon(boolean b) throws PropertyVetoException {
        this.setIcon(b);
    }        
}

