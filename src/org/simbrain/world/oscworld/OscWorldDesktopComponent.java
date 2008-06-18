package org.simbrain.world.oscworld;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.Collections;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import javax.swing.border.EmptyBorder;

import ca.odell.glazedlists.GlazedLists;

import ca.odell.glazedlists.swing.EventListModel;

import org.dishevelled.layout.LabelFieldPanel;

import org.simbrain.workspace.gui.CouplingMenus;
import org.simbrain.workspace.gui.GuiComponent;
import org.simbrain.workspace.gui.GenericFrame;

/**
 * OSC world desktop component.
 */
public final class OscWorldDesktopComponent
    extends GuiComponent<OscWorldComponent> {

    /** Create OSC in message action. */
    private final Action createOscInMessageAction;

    /** Create OSC out message action. */
    private final Action createOscOutMessageAction;

    /** List of OSC out message consumers. */
    private final JList consumers;

    /** List of OSC in message producers. */
    private final JList producers;


    /**
     * Create a new OSC world desktop component with the specified OSC world component.
     *
     * @param oscWorldComponent OSC world component
     */
    public OscWorldDesktopComponent(GenericFrame frame, final OscWorldComponent oscWorldComponent) {
        super(frame, oscWorldComponent);

        createOscInMessageAction = new CreateOscInMessageAction();
        createOscOutMessageAction = new CreateOscOutMessageAction();
        consumers = new JList(new EventListModel(oscWorldComponent.getConsumersEventList()));
        producers = new JList(new EventListModel(GlazedLists.eventList(Collections.emptyList())));

        consumers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        consumers.addMouseListener(new MouseAdapter()
            {
                /** {@inheritDoc} */
                public void mousePressed(final MouseEvent event)
                {
                    if (event.isPopupTrigger())
                    {
                        showContextMenu(event);
                    }
                }

                /** {@inheritDoc} */
                public void mouseClicked(final MouseEvent event)
                {
                    if (event.isPopupTrigger())
                    {
                        showContextMenu(event);
                    }
                }

                /**
                 * Show the consumer context menu if a consumer is selected.
                 *
                 * @param event mouse event
                 */
                private void showContextMenu(final MouseEvent event)
                {
                    if (consumers.getSelectedIndex() > -1)
                    {
                        JPopupMenu contextMenu = new JPopupMenu();
                        OscMessageConsumer consumer = (OscMessageConsumer) consumers.getSelectedValue();
                        JMenu producerMenu = CouplingMenus.getProducerMenu(oscWorldComponent.getWorkspace(), consumer.getDefaultConsumingAttribute());
                        producerMenu.setText("Set input source");
                        contextMenu.add(producerMenu);
                        contextMenu.show(consumers, event.getX(), event.getY());
                    }
                }
            });

        JMenuBar menuBar = new JMenuBar();
        JToolBar toolBar = new JToolBar();

        JMenu file = new JMenu("File");
        file.add(createOscInMessageAction);
        file.add(createOscOutMessageAction);
        toolBar.add(createOscInMessageAction);
        toolBar.add(createOscOutMessageAction);

        menuBar.add(file);
        getParentFrame().setJMenuBar(menuBar);

        LabelFieldPanel inPanel = new LabelFieldPanel();
        inPanel.addField("OSC in host:", new JLabel(oscWorldComponent.getOscInHost()));
        inPanel.addField("OSC in port:", new JLabel(String.valueOf(oscWorldComponent.getOscInPort())));
        inPanel.addSpacing(11);
        inPanel.addLabel("OSC in messages:");
        inPanel.addFinalField(new JScrollPane(producers));

        LabelFieldPanel outPanel = new LabelFieldPanel();
        outPanel.addField("OSC out host:", new JLabel(oscWorldComponent.getOscOutHost()));
        outPanel.addField("OSC out port:", new JLabel(String.valueOf(oscWorldComponent.getOscOutPort())));
        outPanel.addSpacing(11);
        outPanel.addLabel("OSC out messages:");
        outPanel.addFinalField(new JScrollPane(consumers));

        JPanel mainPanel = new JPanel();
        mainPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        mainPanel.setLayout(new GridLayout(1, 2, 12, 12));
        mainPanel.add(inPanel);
        mainPanel.add(outPanel);

        setLayout(new BorderLayout());
        add("North", toolBar);
        add("Center", mainPanel);
    }

    /** {@inheritDoc} */
    public void close() {
        // empty
    }

    /**
     * Create OSC in message action.
     */
    private final class CreateOscInMessageAction
        extends AbstractAction {

        /**
         * Create a new create OSC in message action.
         */
        CreateOscInMessageAction() {
            super("Create OSC in message");
            putValue(Action.LONG_DESCRIPTION, "Create a new OSC in message");
            setEnabled(false);
        }


        /** {@inheritDoc} */
        public void actionPerformed(final ActionEvent event) {
            // empty
        }
    }

    /**
     * Create OSC out message action.
     */
    private final class CreateOscOutMessageAction
        extends AbstractAction {

        /**
         * Create a new create OSC out message action.
         */
        CreateOscOutMessageAction() {
            super("Create OSC out message");
            putValue(Action.LONG_DESCRIPTION, "Create a new OSC out message");
        }


        /** {@inheritDoc} */
        public void actionPerformed(final ActionEvent event) {
            SwingUtilities.invokeLater(new Runnable() {
                    /** {@inheritDoc} */
                    public void run() {
                        String address = JOptionPane.showInputDialog(null, "Create a new OSC out message with the specified address.\nThe address must begin with a '/' character.\n\n\nOSC out message address:", "Create a new OSC out message", JOptionPane.QUESTION_MESSAGE);
                        getWorkspaceComponent().addOutMessage(address);
                    }
                });
        }
    }
}