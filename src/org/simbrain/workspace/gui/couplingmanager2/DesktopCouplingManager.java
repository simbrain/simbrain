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
package org.simbrain.workspace.gui.couplingmanager2;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;

import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.ProducingAttribute;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.gui.AttributePanel;
import org.simbrain.workspace.gui.CouplingListPanel;
import org.simbrain.workspace.gui.GenericFrame;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.workspace.gui.AttributePanel.AttributeType;

/**
 * GUI dialog for creating couplings.
 */
public class DesktopCouplingManager extends JPanel implements ActionListener {

    /** List of producing attributes. */
    private AttributePanel producingAttributes;

    /** List of consuming attributes. */
    private AttributePanel consumingAttributes;

    /** Methods for making couplings. */
    String[] tempStrings = { "One to one", "One to many" };

    /** Methods for making couplings. */
    private JComboBox couplingMethodComboBox = new JComboBox(tempStrings);

    /** Reference to desktop. */
    private SimbrainDesktop desktop;
    
    /** Reference of parent frame. */
    private final GenericFrame frame;

    /**
     * Creates and displays the coupling manager.
     *
     * @param desktop reference to parent desktop
     * @param reference to parent frame
     */
    public DesktopCouplingManager(final SimbrainDesktop desktop, final GenericFrame frame) {
        super();
        this.desktop = desktop;
        this.frame = frame;

        // Left Panel
        JPanel leftPanel = new JPanel();
        Border leftBorder = BorderFactory.createTitledBorder("Source Producing Attributes");
        leftPanel.setBorder(leftBorder);
        producingAttributes = new AttributePanel(desktop.getWorkspace(), AttributeType.Producing);
        leftPanel.add(producingAttributes);

        // Right Panel
        JPanel rightPanel = new JPanel();
        Border rightBorder = BorderFactory.createTitledBorder("Target Consuming Attributes");
        rightPanel.setBorder(rightBorder);
        consumingAttributes = new AttributePanel(desktop.getWorkspace(),  AttributeType.Consuming);
        rightPanel.add(consumingAttributes);
        
        // Bottom Panel
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
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

        JComponent couplingList = new CouplingListPanel(desktop,new Vector(desktop.getWorkspace().getCouplingManager().getCouplings()));
        couplingList.setBorder(BorderFactory.createTitledBorder("Couplings"));

        // Main Panel
        JPanel centerPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        centerPanel.add(leftPanel);
        centerPanel.add(couplingList);
        centerPanel.add(rightPanel);
        centerPanel.setPreferredSize(new Dimension(800, 400));
        this.setLayout(new BorderLayout());
        this.add("Center", centerPanel);
        this.add("South", bottomPanel);
        desktop.getFrame().getRootPane().setDefaultButton(okButton);
        
    }

    /**
     * @see ActionListener.
     * @param event to listen.
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

    	ArrayList<ProducingAttribute<?>> producingAttributeList = (ArrayList<ProducingAttribute<?>>) producingAttributes.getSelectedAttributes();
        ArrayList<ConsumingAttribute<?>> consumingAttributeList = (ArrayList<ConsumingAttribute<?>>) consumingAttributes.getSelectedAttributes();
        
        if ((producingAttributeList.size() == 0) || (consumingAttributeList.size() == 0)) {
          JOptionPane.showMessageDialog(null,
                  "You must select at least one consuming and producing attribute \n in order to create couplings!",
                  "No Attributes Selected Warning", JOptionPane.WARNING_MESSAGE);
                  return;
        }
        
        if (((String)couplingMethodComboBox.getSelectedItem()).equalsIgnoreCase("One to one")) {
            desktop.getWorkspace().coupleOneToOne(producingAttributeList, consumingAttributeList);
        } else if (((String)couplingMethodComboBox.getSelectedItem()).equalsIgnoreCase("One to many")) {
            desktop.getWorkspace().coupleOneToMany(producingAttributeList, consumingAttributeList);
        }
    }

}


