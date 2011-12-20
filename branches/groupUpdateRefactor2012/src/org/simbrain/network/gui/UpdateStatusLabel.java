package org.simbrain.network.gui;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;

import org.simbrain.network.interfaces.RootNetwork.UpdateMethod;

import edu.umd.cs.piccolo.event.PBasicInputEventHandler;

/**
 * The label which shows how the network is being updated.
 */
public class UpdateStatusLabel extends JLabel {

    /** Reference to parent NetworkPanel. */
    private NetworkPanel networkPanel;

    /** Standard update action. */
    private Action standardUpdateAction;

    /** Standard update action. */
    private Action priorityUpdateAction;

    /** Popup menu. */
    private JPopupMenu contextMenu;

    /**
     * Construct time label with reference to networkPanel.
     * 
     * @param netPanel reference to networkPanel
     */
    public UpdateStatusLabel(final NetworkPanel netPanel) {
        super();
        networkPanel = netPanel;
        // this.addInputEventListener(new UpdateStatusEventHandler());

        standardUpdateAction = new AbstractAction("Standard update") {
            public void actionPerformed(final ActionEvent event) {
                netPanel.getRootNetwork()
                        .setUpdateMethod(UpdateMethod.BUFFERED);
                update();
            }
        };
        priorityUpdateAction = new AbstractAction("Priority based update") {
            public void actionPerformed(final ActionEvent event) {
                netPanel.getRootNetwork().setUpdateMethod(
                        UpdateMethod.PRIORITYBASED);
                update();
            }
        };
        contextMenu = new JPopupMenu();
        contextMenu.add(standardUpdateAction);
        contextMenu.add(priorityUpdateAction);
        this.addMouseListener(new MouseAdapter() {
            public void mousePressed(final MouseEvent event) {
                if (event.isControlDown() || event.getButton() == 2) {
                    contextMenu.show(event.getComponent(), (int) event.getX(),
                            (int) event.getY());
                }
            }
        });

    }

    /**
     * Update the custom script label.
     */
    public void update() {
        setText("Update Method:"
                + networkPanel.getRootNetwork().getUpdateMethod());
    }

}
