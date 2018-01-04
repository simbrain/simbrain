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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.simbrain.workspace.Attribute2;
import org.simbrain.workspace.AttributeListener;
import org.simbrain.workspace.AttributeType;
import org.simbrain.workspace.Consumer2;
import org.simbrain.workspace.Producer2;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceListener;

/**
 * Displays a panel with a JComboBox, which the user uses to select a component,
 * and a JList of attributes for that component.
 *
 * TODO: Rename to AttributePanel?
 */
public class PotentialAttributePanel extends JPanel
        implements ActionListener, MouseListener {

    /** Parent frame. */
    private JFrame parentFrame = new JFrame();

    /** Drop down box for workspace components. */
    private ComponentDropDownBox componentList;

    /** List of Attributes in a specified Component. */
    private JList attributeList;

    /** List model. */
    private DefaultListModel model;

    // TODO: Renames
    public enum ProducerOrConsumer {
        Producing, Consuming
    };

    private ProducerOrConsumer producerOrConsumer;

    /** Panel for setting visibility of attribute types. */
    // private AttributeTypePanel attributeTypePanel;

    /** List of network couplings. */
    private JList attributeTypes = new JList();

    /**
     * Creates a new attribute list panel.
     *
     * @param workspace reference to workspace
     * @param attributeType
     */
    public PotentialAttributePanel(final Workspace workspace,
            ProducerOrConsumer attributeType) {
        super(new BorderLayout());
        this.producerOrConsumer = attributeType;

        // Set up attribute lists
        model = new DefaultListModel();
        attributeList = new JList(model);
        attributeList.setCellRenderer(new AttributeCellRenderer());
        attributeList.addMouseListener(this);

        // Component list box
        componentList = new ComponentDropDownBox(workspace);
        componentList.addActionListener(this);
        JPanel componentPanel = new JPanel();
        componentPanel.setLayout(new BorderLayout());
        componentPanel.add(componentList, BorderLayout.WEST);
        add(componentPanel, BorderLayout.NORTH);

        // Scroll pane
        JScrollPane listScroll = new JScrollPane(attributeList);
        listScroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        listScroll.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(listScroll, BorderLayout.CENTER);

        // Bottom panel
        JPanel bottomPanel = new JPanel();
        JButton button = new JButton("Set attribute visibility");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {

                // if (attributeTypePanel != null) {
                // JDialog dialog = new JDialog(parentFrame);
                // dialog.setContentPane(attributeTypePanel);
                // dialog.pack();
                // dialog.setLocationRelativeTo(null);
                // dialog.setVisible(true);
                // }
            }
        });
        bottomPanel.add(button);
        add(bottomPanel, BorderLayout.SOUTH);

        // Listen for attribute changes
        for (WorkspaceComponent component : workspace.getComponentList()) {
            addAttributeListener(component);
        }
        // Initialize frame
        // parentFrame.setContentPane(this);
    }

    /**
     * Add a listener to the specified workspace component.
     *
     * @param component component to listen to
     */
    private void addAttributeListener(final WorkspaceComponent component) {

        component.addAttributeListener(new AttributeListener() {

            public void attributeTypeVisibilityChanged(AttributeType type) {
                if (isSelectedComponent(component)) {
                    // if (component == type.getParentComponent()) { // Not sure
                    // if
                    // // this is
                    // // needed
                    refresh(component);
                    // }
                }
            }

            public void attributeObjectRemoved(Object object) {
                // No implementation
            }

            public void potentialAttributesChanged() {
                if (isSelectedComponent(component)) {
                    refresh(component);
                }
            }
        });

    }

    /**
     * Returns true if the component is current, false otherwise.
     *
     * @param component the component to check
     * @return whether the component is current
     */
    private boolean isSelectedComponent(final WorkspaceComponent component) {
        if (component == componentList.getSelectedItem()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @see ActionListener
     * @param event
     */
    public void actionPerformed(final ActionEvent event) {

        // Refresh component list
        if (event.getSource() instanceof JComboBox) {
            WorkspaceComponent component = (WorkspaceComponent) ((JComboBox) event
                    .getSource()).getSelectedItem();
            refresh(component);
        }
    }

    /**
     * Refresh attribute list.
     */
    private void refresh(final WorkspaceComponent component) {

        if (component != null) {
            model.clear();
            if (producerOrConsumer == ProducerOrConsumer.Producing) {
                for (Producer2<?> producer : component.getProducers()) {
                    model.addElement(producer);
                }
            } else {
                for (Consumer2<?> consumer : component.getConsumers()) {
                    model.addElement(consumer);
                }
            }
        }

        // attributeTypes.setListData(new
        // Vector(component.getAttributeTypes()));
        // attributeTypePanel = new AttributeTypePanel(component,
        // producerOrConsumer);

        // Set Attribute list
        // if (component != null) {
        // model.clear();
        // if (producerOrConsumer == ProducerOrConsumer.Producing) {
        // for (PotentialAttribute potentialProducer : component
        // .getAnnotatedProducers()) {
        // model.addElement(potentialProducer);
        // }
        // } else if (producerOrConsumer == ProducerOrConsumer.Consuming) {
        // for (PotentialAttribute potentialConsumer : component
        // .getAnnotatedConsumers()) {
        // model.addElement(potentialConsumer);
        // }
        // }
        // }
    }

    /**
     * Clear attribute list.
     */
    private void clearList() {
        model.clear();
    }

    /**
     * Returns selected attributes.
     *
     * @return list of selected attributes.
     */
    public List getSelectedAttributes() {
        if (producerOrConsumer == ProducerOrConsumer.Producing) {
            List<Producer2<?>> ret = new ArrayList<>();
            for (Object object : attributeList.getSelectedValuesList()) {
                ret.add((Producer2<?>) object);
            }
            return ret;
        } else if (producerOrConsumer == ProducerOrConsumer.Consuming) {
            List<Consumer2<?>> ret = new ArrayList<>();
            for (Object object : attributeList.getSelectedValuesList()) {
                ret.add((Consumer2<?>) object);
            }
            return ret;
        }
        return null;
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    /**
     * Custom attribute renderer for JList.
     */
    private class AttributeCellRenderer extends DefaultListCellRenderer {

        /**
         * @overrides java.awt.Component
         * @param list
         * @param object
         * @param index
         * @param isSelected
         * @param cellHasFocus
         * @return
         */
        public java.awt.Component getListCellRendererComponent(final JList list,
                final Object object, final int index, final boolean isSelected,
                final boolean cellHasFocus) {
            DefaultListCellRenderer renderer = (DefaultListCellRenderer) super.getListCellRendererComponent(
                    list, object, index, isSelected, cellHasFocus);
            // TODO
            // PotentialAttribute id = (PotentialAttribute) object;
            // Set text color based on data type
            Attribute2 atttribute = (Attribute2) object;
            renderer.setForeground(
                    DesktopCouplingManager.getColor(atttribute.getType()));
            //
            // renderer.setText(id.getDescription() + "<"
            // + CouplingManager.getTypeDescriptor(id.getDataType()) + ">");
            return renderer;
        }

    }

    /**
     * A JComboBox which listens to the workspace and updates accordingly.
     */
    private class ComponentDropDownBox extends JComboBox
            implements WorkspaceListener {

        /** Reference to workspace. */
        private Workspace workspace;

        /**
         * @param workspace the workspace
         */
        public ComponentDropDownBox(final Workspace workspace) {
            this.workspace = workspace;
            for (WorkspaceComponent component : workspace.getComponentList()) {
                this.addItem(component);
            }
            if (this.getModel().getSize() > 0) {
                this.setSelectedIndex(0);
                PotentialAttributePanel.this
                        .refresh((WorkspaceComponent) this.getItemAt(0));
            }
            workspace.addListener(this);
        }

        /**
         * {@inheritDoc}
         *
         * @return
         */
        public boolean clearWorkspace() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public void componentAdded(final WorkspaceComponent component) {
            this.addItem(component);
            addAttributeListener(component);
        }

        /**
         * {@inheritDoc}
         */
        public void componentRemoved(final WorkspaceComponent component) {
            this.removeItem(component);
            if (this.getItemCount() == 0) {
                PotentialAttributePanel.this.clearList();
            }
        }

        /**
         * {@inheritDoc}
         */
        public void workspaceCleared() {
            this.removeAllItems();
            PotentialAttributePanel.this.clearList();
        }

        /**
         * {@inheritDoc}
         */
        public void newWorkspaceOpened() {
        }
    }

}
