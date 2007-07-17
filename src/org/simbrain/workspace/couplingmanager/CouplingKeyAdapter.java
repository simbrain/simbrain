package org.simbrain.workspace.couplingmanager;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JList;

public class CouplingKeyAdapter extends KeyAdapter {

    private JList manager;
    public CouplingKeyAdapter(final JList manager) {
        this.manager = manager;
    }

    public void keyPressed(final KeyEvent event) {
        int keyCode = event.getKeyCode();
        switch(keyCode) {
        case KeyEvent.VK_A:

                manager.addSelectionInterval(0, manager.getSize().height);

            break;
        }
    }
}
