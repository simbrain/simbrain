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
 * The label which shows whether an update script is being used.
 */
public class UpdateStatusLabel extends PText {

    /** Reference to parent NetworkPanel. */
    private NetworkPanel networkPanel;

    /** Standard update action. */
    private Action standardUpdateAction;

    /** Standard update action. */
    private Action priorityUpdateAction;

    /** Load update action. */
    private Action loadAction;

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
                netPanel.getRootNetwork().setCustomUpdateScript(null);
                update();
            }
        };
        priorityUpdateAction = new AbstractAction("Priority based update") {
            public void actionPerformed(final ActionEvent event) {
                netPanel.getRootNetwork().setCustomUpdateScript(null);
                netPanel.getRootNetwork().setUpdateMethod(UpdateMethod.PRIORITYBASED);
                update();
            }
        };
        loadAction = new AbstractAction("Load custom update script") {
            public void actionPerformed(final ActionEvent event) {
                loadCustomUpdateScript();
                netPanel.getRootNetwork().setUpdateMethod(UpdateMethod.SCRIPTBASED);
            }
        };
        createContextMenu();
    }

    /**
     * Calls loadCustomUpdateScript() to load an update script.
     */
    public void loadUpdateScript() {
        loadCustomUpdateScript();
    }
    /**
     * Load custom update script.
     */
    private void loadCustomUpdateScript() {
        SFileChooser chooser = new SFileChooser(".", "Beanshell Script", "bsh");
        File theFile = chooser.showOpenDialog();
        if (theFile != null) {
            networkPanel.getRootNetwork().setCustomUpdateScript(theFile);
            update();
        }
    }

    /**
     * Update the custom script label.
     */
    public void update() {
        if (networkPanel.getRootNetwork().getCustomUpdateScript() == null) {
            if (networkPanel.getRootNetwork().getUpdateMethod() == UpdateMethod.PRIORITYBASED) {
                setText("Update: Priority Based Update");
            } else {
                this.setText("Update: Standard Update");
            }
        } else {
            this.setText("Update: " + networkPanel.getRootNetwork().getCustomUpdateScript().getName());
        }
    }

    /**
     * Create the popup menu.
     */
    private void createContextMenu() {
        contextMenu = new JPopupMenu();

        contextMenu.add(standardUpdateAction);
        contextMenu.add(priorityUpdateAction);
        contextMenu.add(loadAction);

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
            if (event.getClickCount() == 2) {
                loadCustomUpdateScript();
            }
            event.setHandled(true);
        }

    }
}
