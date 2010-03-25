/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.world.oscworld;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.workspace.gui.ConsumingAttributeMenu;
import org.simbrain.workspace.gui.GenericFrame;
import org.simbrain.workspace.gui.GuiComponent;
import org.simbrain.workspace.gui.ProducingAttributeMenu;

/**
 * OSC world desktop component.
 */
public final class OscWorldDesktopComponent
    extends GuiComponent<OscWorldComponent> {

    /** List of OSC out message consumers. */
    private final JList consumers;

    /** List of OSC in message producers. */
    private final JList producers;


    /**
     * Create a new OSC world desktop component with the specified OSC world component.
     *
     * @param frame parent frame
     * @param oscWorldComponent OSC world component
     */
    public OscWorldDesktopComponent(final GenericFrame frame,
                                    final OscWorldComponent oscWorldComponent) {
        super(frame, oscWorldComponent);

        Action closeAction = new CloseAction();
        Action createOscInMessageAction = new CreateOscInMessageAction();
        Action createOscOutMessageAction = new CreateOscOutMessageAction();
//        consumers = new JList(new EventListModel(oscWorldComponent.getConsumersEventList()));
//        producers = new JList(new EventListModel(oscWorldComponent.getProducersEventList()));

        // TODO: Unbreak below!
        consumers = new JList();
        producers = new JList();

        
        consumers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        consumers.setVisibleRowCount(16);
        consumers.addMouseListener(new MouseAdapter() {
                /** {@inheritDoc} */
                public void mousePressed(final MouseEvent event) {
                    if (event.isPopupTrigger()) {
                        showContextMenu(event);
                    }
                }

                /** {@inheritDoc} */
                public void mouseReleased(final MouseEvent event) {
                    if (event.isPopupTrigger()) {
                        showContextMenu(event);
                    }
                }

                /**
                 * Show the consumer context menu if a consumer is selected.
                 *
                 * @param event mouse event
                 */
                private void showContextMenu(final MouseEvent event) {
                    if (consumers.getSelectedIndex() > -1) {
                        JPopupMenu contextMenu = new JPopupMenu();
                        OscMessageConsumer consumer = (OscMessageConsumer) consumers.getSelectedValue();
                        ProducingAttributeMenu producerMenu = new ProducingAttributeMenu("Receive coupling from", oscWorldComponent.getWorkspace(),
                                                                           consumer.getConsumingAttributes().get(0));
                        producerMenu.setText("Set input source");
                        contextMenu.add(producerMenu);
                        contextMenu.show(consumers, event.getX(), event.getY());
                    }
                }
            });

        producers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        producers.setVisibleRowCount(16);
        producers.addMouseListener(new MouseAdapter() {
                /** {@inheritDoc} */
                public void mousePressed(final MouseEvent event) {
                    if (event.isPopupTrigger()) {
                        showContextMenu(event);
                    }
                }

                /** {@inheritDoc} */
                public void mouseReleased(final MouseEvent event) {
                    if (event.isPopupTrigger()) {
                        showContextMenu(event);
                    }
                }

                /**
                 * Show the consumer context menu if a consumer is selected.
                 *
                 * @param event mouse event
                 */
                private void showContextMenu(final MouseEvent event) {
                    if (producers.getSelectedIndex() > -1) {
                        JPopupMenu contextMenu = new JPopupMenu();
                        OscMessageProducer producer = (OscMessageProducer) producers.getSelectedValue();
                        ConsumingAttributeMenu consumerMenu = new ConsumingAttributeMenu("Send coupling to", oscWorldComponent.getWorkspace(),
                                                                           producer.getProducingAttributes().get(0));
                        consumerMenu.setText("Set output target");
                        contextMenu.add(consumerMenu);
                        contextMenu.show(producers, event.getX(), event.getY());
                    }
                }
            });

        JMenuBar menuBar = new JMenuBar();
        JToolBar toolBar = new JToolBar();

        JMenu file = new JMenu("File");
        file.add(createOscInMessageAction);
        file.add(createOscOutMessageAction);
        file.addSeparator();
        file.add(closeAction);
        toolBar.add(createOscInMessageAction);
        toolBar.add(createOscOutMessageAction);

        menuBar.add(file);
        getParentFrame().setJMenuBar(menuBar);

        LabelledItemPanel inPanel = new LabelledItemPanel();
        inPanel.addItem("OSC in host:", new JLabel(oscWorldComponent.getOscInHost()));
        inPanel.addItem("OSC in port:", new JLabel(String.valueOf(oscWorldComponent.getOscInPort())));
        //inPanel.addSpacing(11);
        inPanel.addItem("OSC in messages:", new JScrollPane(producers));

        LabelledItemPanel outPanel = new LabelledItemPanel();
        outPanel.addItem("OSC out host:", new JLabel(oscWorldComponent.getOscOutHost()));
        outPanel.addItem("OSC out port:", new JLabel(String.valueOf(oscWorldComponent.getOscOutPort())));
        //outPanel.addSpacing(11);
        outPanel.addItem("OSC out messages:", new JScrollPane(consumers));

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
    public void closing() {
        // empty
    }


    /**
     * Close action.
     */
    private final class CloseAction
        extends AbstractAction {

        /**
         * Create a new close action.
         */
        CloseAction() {
            // TODO:  would be nice to have internal frame title here, e.g. "OscWorldDesktop 1"
            super("Close");
            putValue(Action.LONG_DESCRIPTION, "Close");
            KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_W,
                                                         Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
            putValue(Action.ACCELERATOR_KEY, keyStroke);
        }


        /** {@inheritDoc} */
        public void actionPerformed(final ActionEvent event) {
            close();
        }
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
        }


        /** {@inheritDoc} */
        public void actionPerformed(final ActionEvent event) {
            SwingUtilities.invokeLater(new Runnable() {
                    /** {@inheritDoc} */
                    public void run() {
                        String address = JOptionPane.showInputDialog(null, "Create a new OSC in message with the specified address.\nThe address must begin with a '/' character.\n\n\nOSC in message address:", "Create a new OSC in message", JOptionPane.QUESTION_MESSAGE);
                        getWorkspaceComponent().addInMessage(address);
                    }
                });
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