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
package org.simbrain.workspace.gui.couplingmanager;

import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.genericframe.GenericJInternalFrame;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.MismatchedAttributesException;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.gui.CouplingListPanel;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.workspace.gui.couplingmanager.AttributePanel.ProducerOrConsumer;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Type;
import java.util.List;

/**
 * GUI dialog for creating couplings.
 */
public class DesktopCouplingManager extends JPanel implements ActionListener {

    /**
     * Flag to ensure that only one dialog is opened at a time.
     */
    public static boolean isVisible;

    /**
     * List of producers.
     */
    private AttributePanel producerPanel;

    /**
     * List of consumers.
     */
    private AttributePanel consumerPanel;

    /**
     * Methods for making couplings.
     */
    private String[] tempStrings = {"One to One", "One to Many"};

    /**
     * Methods for making couplings.
     */
    private JComboBox<String> couplingMethodComboBox = new JComboBox<String>(tempStrings);

    /**
     * Reference to desktop.
     */
    private SimbrainDesktop desktop;

    /**
     * Reference of parent frame.
     */
    private GenericFrame frame;

    /**
     * Creates and displays the coupling manager.
     *
     * @param desktop reference to parent desktop
     * @param frame   reference to parent frame
     */
    public DesktopCouplingManager(final SimbrainDesktop desktop, final GenericJInternalFrame frame) {
        super(new BorderLayout());
        this.desktop = desktop;
        this.frame = frame;
        isVisible = true;

        // Left Panel
        Border leftBorder = BorderFactory.createTitledBorder("Producers");
        producerPanel = new AttributePanel(desktop.getWorkspace(), ProducerOrConsumer.Producing);
        producerPanel.setBorder(leftBorder);

        // Right Panel
        Border rightBorder = BorderFactory.createTitledBorder("Consumers");
        consumerPanel = new AttributePanel(desktop.getWorkspace(), ProducerOrConsumer.Consuming);
        consumerPanel.setBorder(rightBorder);

        // Bottom Panel
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(new JButton(new ShowHelpAction("Pages/Workspace/Couplings.html")));
        bottomPanel.add(couplingMethodComboBox);

        JButton addCouplingsButton = new JButton("Add Coupling(s)");
        addCouplingsButton.setActionCommand("addCouplings");
        addCouplingsButton.addActionListener(this);
        bottomPanel.add(addCouplingsButton);

        JButton okButton = new JButton("OK");
        okButton.setActionCommand("ok");
        okButton.addActionListener(this);
        bottomPanel.add(okButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand("cancel");
        cancelButton.addActionListener(this);
        bottomPanel.add(cancelButton);

        JComponent couplingList = new CouplingListPanel(desktop, desktop.getWorkspace().getCouplings());
        couplingList.setBorder(BorderFactory.createTitledBorder("Couplings"));

        // Main Panel
        JPanel centerPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        centerPanel.add(producerPanel);
        centerPanel.add(couplingList);
        centerPanel.add(consumerPanel);
        centerPanel.setPreferredSize(new Dimension(800, 400));
        this.add("Center", centerPanel);
        this.add("South", bottomPanel);

        frame.getRootPane().setDefaultButton(okButton);
        frame.pack();
    }

    /**
     * @param event to listen.
     * @see ActionListener
     */
    public void actionPerformed(final ActionEvent event) {

        // Refresh component lists
        if (event.getSource() instanceof JComboBox) {
            WorkspaceComponent component = (WorkspaceComponent) ((JComboBox) event.getSource()).getSelectedItem();
        }

        // Handle Button Presses
        if (event.getSource() instanceof JButton) {
            JButton button = (JButton) event.getSource();
            if (button.getActionCommand().equalsIgnoreCase("addCouplings")) {
                addCouplings();
            } else if (button.getActionCommand().equalsIgnoreCase("ok")) {
                frame.dispose();
            } else if (button.getActionCommand().equalsIgnoreCase("cancel")) {
                frame.dispose();
            }
        }
    }

    /**
     * Add couplings using the selected method.
     */
    private void addCouplings() {
        List<Producer<?>> producers = (List<Producer<?>>) producerPanel.getSelectedAttributes();
        List<Consumer<?>> consumers = (List<Consumer<?>>) consumerPanel.getSelectedAttributes();

        if ((producers.size() == 0) || (consumers.size() == 0)) {
            JOptionPane.showMessageDialog(null, "You must select at least one consuming and producing attribute\nto create couplings.", "No Attributes Selected Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String couplingMethod = (String) couplingMethodComboBox.getSelectedItem();
            if (couplingMethod.equalsIgnoreCase("One to One")) {
                desktop.getWorkspace().getCouplingManager().createOneToOneCouplings(producers, consumers);
            } else if (couplingMethod.equalsIgnoreCase("One to Many")) {
                desktop.getWorkspace().getCouplingManager().createOneToManyCouplings(producers, consumers);
            }
        } catch (MismatchedAttributesException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Unmatched Attributes", JOptionPane.WARNING_MESSAGE, null);
        }
    }

    /**
     * Associates attribute and coupling data types (classes) with colors used
     * in displaying attributes and couplings.
     *
     * @param dataType the data type to associate with a color
     * @return the color associated with a data type
     */
    public static Color getColor(Type dataType) {
        if (dataType == double.class) {
            return Color.black;
        } else if (dataType == double[].class) {
            return Color.green.darker().darker();
        } else if (dataType == String.class) {
            return Color.blue.brighter();
        }
        return Color.black;
    }

}
