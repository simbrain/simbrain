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
import org.simbrain.workspace.gui.CouplingListPanel;
import org.simbrain.workspace.gui.GenericFrame;
import org.simbrain.workspace.gui.SimbrainDesktop;

/**
 * GUI dialog for creating couplings.
 */
public class DesktopCouplingManager extends JPanel implements ActionListener, MouseListener, MouseMotionListener {

    /** Source workspace components. */
    private final JComboBox sourceComponents = new JComboBox();

    /** List of producing attributes. */
    private JList producingAttributes = new JList();

    /** Target workspace components. */
    private final JComboBox targetComponents = new JComboBox();

    /** List of consuming attributes. */
    private JList consumingAttributes = new JList();

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

        // SOURCE SIDE
        JPanel leftPanel = new JPanel(new BorderLayout());
        JScrollPane leftScrollPane = new JScrollPane(producingAttributes);
        Border leftBorder = BorderFactory.createTitledBorder("Source Producing Attributes");
        leftPanel.setBorder(leftBorder);
        producingAttributes.setDragEnabled(true);
        producingAttributes.setCellRenderer(new ProducingAttributeCellRenderer());
        producingAttributes.addMouseListener(this);
        producingAttributes.addMouseMotionListener(this);
        GenericListModel sourceComponentList = new GenericListModel(desktop.getWorkspace().getComponentList());
        sourceComponents.setModel(sourceComponentList);
        sourceComponents.addActionListener(this);
        if (sourceComponents.getModel().getSize() > 0) {
            sourceComponents.setSelectedIndex(0);
            this.refresh((WorkspaceComponent<?>)sourceComponentList.getElementAt(0), sourceComponents);
        }
        leftPanel.add("North", sourceComponents);
        leftPanel.add("Center", leftScrollPane);

        // TARGET SIDE
        JPanel rightPanel = new JPanel(new BorderLayout());
        JScrollPane rightScrollPane = new JScrollPane(consumingAttributes);
        Border rightBorder = BorderFactory.createTitledBorder("Target Consuming Attributes");
        rightPanel.setBorder(rightBorder);
        consumingAttributes.setDragEnabled(true);
        consumingAttributes.setCellRenderer(new ConsumingAttributeCellRenderer());
        consumingAttributes.addMouseListener(this);
        consumingAttributes.addMouseMotionListener(this);
        GenericListModel targetComponentList = new GenericListModel(desktop.getWorkspace().getComponentList());
        targetComponents.setModel(targetComponentList);
        targetComponents.addActionListener(this);
        if (targetComponents.getModel().getSize() > 0) {
            targetComponents.setSelectedIndex(0);
            refresh((WorkspaceComponent<?>)targetComponentList.getElementAt(0), targetComponents);
        }
        rightPanel.add("North", targetComponents);
        rightPanel.add("Center", rightScrollPane);
        
        // BOTTOM
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

        JComponent couplingList = new CouplingListPanel(desktop,
                new Vector(desktop.getWorkspace().getCouplingManager().getCouplings()));
        couplingList.setBorder(BorderFactory.createTitledBorder("Couplings"));

        // MAIN PANEL
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
     * Custom cell renderer.
     */
    private class ConsumingAttributeCellRenderer extends DefaultListCellRenderer {

        /**
         * Returns the list of cell renderer components.
         * @param list Graphical object to be rendered.
         * @param object to be rendered.
         * @param index of object.
         * @param isSelected boolean value.
         * @param cellHasFocus boolean value.
         * @return Component to be rendered.
         * @overrides java.awt.Component
         */
        public java.awt.Component getListCellRendererComponent(final JList list,
                final Object object, final int index, final boolean isSelected, final boolean cellHasFocus) {
            DefaultListCellRenderer renderer = (DefaultListCellRenderer)
                    super.getListCellRendererComponent(list, object, index, isSelected, cellHasFocus);
            ConsumingAttribute consumingAttribute = (ConsumingAttribute) object;
            renderer.setText(consumingAttribute.getAttributeDescription());
            return renderer;
       }
    }

    /**
     * Custom producer cell renderer.
     */
    private class ProducingAttributeCellRenderer extends DefaultListCellRenderer {

        /**
         * Producer cell renderer component.
         * @param list to be rendered.
         * @param object to be added.
         * @param index of producer.
         * @param isSelected boolean value.
         * @param cellHasFocus boolean value.
         * @return rendered producers.
         * @overrides java.awt.Component
         */
        public java.awt.Component getListCellRendererComponent(final JList list, final Object object,
                final int index, final boolean isSelected, final boolean cellHasFocus) {
            DefaultListCellRenderer renderer = (DefaultListCellRenderer)
            super.getListCellRendererComponent(list, object, index, isSelected, cellHasFocus);
            ProducingAttribute producingAttribute = (ProducingAttribute) object;
           renderer.setText(producingAttribute.getAttributeDescription());
            return renderer;
       }
    }

    /**
     * Refresh combo boxes.
     *
     * @param component the workspace component being checked
     * @param comboBox the combo box upon which the refresh is based
     */
    private void refresh(final WorkspaceComponent component, final JComboBox comboBox) {

        if (component != null) {
            // Populate attribute Lists
            if (comboBox == targetComponents) {
                if (component.getConsumers() != null)  {
                    consumingAttributes.setModel(new GenericListModel<ConsumingAttribute>(component.getConsumingAttributes()));
                }
          } else if (comboBox == sourceComponents) {
              if (component.getProducers() != null) {
                  producingAttributes.setModel(new GenericListModel<ProducingAttribute>(component.getProducingAttributes()));
              }
          }
        }
    }

    /**
     * @see ActionListener.
     * @param event to listen.
     */
    public void actionPerformed(final ActionEvent event) {

        // Refresh component lists
        if (event.getSource() instanceof JComboBox) {
            WorkspaceComponent component = (WorkspaceComponent) ((JComboBox) event.getSource()).getSelectedItem();
            refresh(component, (JComboBox) event.getSource());
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
        ArrayList<ProducingAttribute<?>> producingAttributes = getSelectedProducingAttributes();
        ArrayList<ConsumingAttribute<?>> consumingAttributes = getSelectedConsumingAttributes();
        
        if ((producingAttributes.size() == 0) || (consumingAttributes.size() == 0)) {
          JOptionPane.showMessageDialog(null,
                  "You must select at least one consuming and producing attribute \n in order to create couplings!",
                  "No Attributes Selected Warning", JOptionPane.WARNING_MESSAGE);
                  return;
        }
        
        if (((String)couplingMethodComboBox.getSelectedItem()).equalsIgnoreCase("One to one")) {
            desktop.getWorkspace().coupleOneToOne(producingAttributes, consumingAttributes);
        } else if (((String)couplingMethodComboBox.getSelectedItem()).equalsIgnoreCase("One to many")) {
            desktop.getWorkspace().coupleOneToMany(producingAttributes, consumingAttributes);
        }
    }
    
    /**
     * Returns producers selected in producer list.
     * @return selected producers.
     */
    public ArrayList<ProducingAttribute<?>> getSelectedProducingAttributes() {
        ArrayList<ProducingAttribute<?>> ret = new ArrayList<ProducingAttribute<?>>();
        for (Object object : producingAttributes.getSelectedValues()) {
            ret.add((ProducingAttribute<?>) object);
        }
        return ret;
    }

    /**
     * Returns consumers selected in consumer list.
     * @return selected consumers.
     */
    private ArrayList<ConsumingAttribute<?>> getSelectedConsumingAttributes() {
        ArrayList<ConsumingAttribute<?>> ret = new ArrayList<ConsumingAttribute<?>>();
        for (Object object : consumingAttributes.getSelectedValues()) {
            ret.add((ConsumingAttribute<?>) object);
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    public void mouseClicked(final MouseEvent event) {
        System.out.println("MouseClicked");
    }

    /**
     * {@inheritDoc}
     */
    public void mouseEntered(final MouseEvent arg0) {
    }

    /**
     * {@inheritDoc}
     */
    public void mouseExited(final MouseEvent arg0) {
    }

    /**
     * {@inheritDoc}
     */
    public void mousePressed(final MouseEvent event) {
        System.out.println("MousePressed");

    }

    /**
     * {@inheritDoc}
     */
    public void mouseDragged(final MouseEvent event) {
       System.out.println("MouseDragged");
    }

    /**
     * {@inheritDoc}
     */
    public void mouseReleased(final MouseEvent arg0) {
    }

    /**
     * {@inheritDoc}
     */
    public void mouseMoved(final MouseEvent arg0) {
    }

    /**
     * @return the consumerJList.
     */
    public JList getConsumerJList() {
        return consumingAttributes;
    }

    /**
     * @return the producerJList.
     */
    public JList getProducerJList() {
        return producingAttributes;
    }
}


