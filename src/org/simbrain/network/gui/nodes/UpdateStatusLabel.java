package org.simbrain.network.gui.nodes;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPopupMenu;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.interfaces.RootNetwork.UpdateMethod;
import org.simbrain.util.SFileChooser;

import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.nodes.PText;

/**
 * The label which shows how the network is being updated.
 */
public class UpdateStatusLabel extends PText {

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
        this.addInputEventListener(new UpdateStatusEventHandler());

        standardUpdateAction = new AbstractAction("Standard update") {
            public void actionPerformed(final ActionEvent event) {
                netPanel.getRootNetwork().setUpdateMethod(UpdateMethod.BUFFERED);
                update();
            }
        };
        priorityUpdateAction = new AbstractAction("Priority based update") {
            public void actionPerformed(final ActionEvent event) {
                netPanel.getRootNetwork().setUpdateMethod(UpdateMethod.PRIORITYBASED);
                update();
            }
        };
        createContextMenu();
    }

    /**
     * Update the custom script label.
     */
    public void update() {
        setText("Update Method:" + networkPanel.getRootNetwork().getUpdateMethod());
    }

    /**
     * Create the popup menu.
     */
    private void createContextMenu() {
        contextMenu = new JPopupMenu();

        contextMenu.add(standardUpdateAction);
        contextMenu.add(priorityUpdateAction);

    }
    /**
     * Open dialog on double clicks.
     */
    private class UpdateStatusEventHandler
        extends PBasicInputEventHandler {

        /** @see PBasicInputEventHandler */
        public void mousePressed(final PInputEvent event) {

            if (event.isControlDown() || event.getButton() == 2) {
                contextMenu.show(networkPanel, (int) getX(), (int) getY());
            }
            event.setHandled(true);
        }

    }
}
