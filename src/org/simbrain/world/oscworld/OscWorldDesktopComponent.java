package org.simbrain.world.oscworld;

import java.awt.BorderLayout;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import org.simbrain.workspace.gui.DesktopComponent;

/**
 * OSC world desktop component.
 */
public final class OscWorldDesktopComponent
    extends DesktopComponent<OscWorldComponent> {

    /** Create OSC message action. */
    private final Action createOscMessageAction;


    /**
     * Create a new OSC world desktop component with the specified OSC world component.
     *
     * @param oscWorldComponent OSC world component
     */
    public OscWorldDesktopComponent(final OscWorldComponent oscWorldComponent) {
        super(oscWorldComponent);

        createOscMessageAction = new CreateOscMessageAction();

        JMenuBar menuBar = new JMenuBar();
        JToolBar toolBar = new JToolBar();

        JMenu file = new JMenu("File");
        file.add(createOscMessageAction);
        toolBar.add(createOscMessageAction);

        menuBar.add(file);
        setJMenuBar(menuBar);

        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add("North", toolBar);
        contentPane.add("Center", new OscWorld());        
    }


    /** {@inheritDoc} */
    public void close() {
        // empty
    }

    /**
     * Create OSC message action.
     */
    private final class CreateOscMessageAction
        extends AbstractAction {

        /**
         * Create a new create OSC message action.
         */
        CreateOscMessageAction() {
            super("Create OSC message");
            putValue(Action.LONG_DESCRIPTION, "Create a new OSC message");
        }


        /** {@inheritDoc} */
        public void actionPerformed(final ActionEvent event) {
            SwingUtilities.invokeLater(new Runnable() {
                    /** {@inheritDoc} */
                    public void run() {
                        String address = JOptionPane.showInputDialog(null, "Create a new OSC message with the specified address.\nThe address must begin with a '/' character.\n\n\nOSC message address:", "Create a new OSC message", JOptionPane.QUESTION_MESSAGE);
                        getWorkspaceComponent().addMessage(address);
                    }
                });
        }
    }
}