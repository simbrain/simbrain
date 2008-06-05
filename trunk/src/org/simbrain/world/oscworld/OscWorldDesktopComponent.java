package org.simbrain.world.oscworld;

import java.awt.BorderLayout;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import javax.swing.border.EmptyBorder;

import ca.odell.glazedlists.swing.EventListModel;

import org.simbrain.workspace.gui.DesktopComponent;

/**
 * OSC world desktop component.
 */
public final class OscWorldDesktopComponent
    extends DesktopComponent<OscWorldComponent> {

    /** Create OSC message action. */
    private final Action createOscMessageAction;

    /** List of OSC message consumers. */
    private final JList consumers;


    /**
     * Create a new OSC world desktop component with the specified OSC world component.
     *
     * @param oscWorldComponent OSC world component
     */
    public OscWorldDesktopComponent(final OscWorldComponent oscWorldComponent) {
        super(oscWorldComponent);

        createOscMessageAction = new CreateOscMessageAction();
        consumers = new JList(new EventListModel(oscWorldComponent.getConsumersEventList()));

        JMenuBar menuBar = new JMenuBar();
        JToolBar toolBar = new JToolBar();

        JMenu file = new JMenu("File");
        file.add(createOscMessageAction);
        toolBar.add(createOscMessageAction);

        menuBar.add(file);
        setJMenuBar(menuBar);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(11, 4, 4, 4));
        mainPanel.add("North", new JLabel("OSC messages:"));
        mainPanel.add("Center", new JScrollPane(consumers));

        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add("North", toolBar);
        contentPane.add("Center", mainPanel);
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