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
package org.simbrain.workspace.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.simbrain.workspace.Attribute;
import org.simbrain.workspace.AttributeHolderListener;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.ProducingAttribute;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceListener;

/**
 * Displays a panel with a JComboBox, which the user uses to select a component,
 * and a JList of attributes for that component.
 */
public class AttributePanel extends JPanel implements ActionListener,
        MouseListener {

    /** Parent frame. */
    private JFrame parentFrame = new JFrame();

    /** Drop down box for workspace components. */
    private ComponentDropDownBox componentList;

    /** Drop down box for attributes. */
    private AttributeDropDownBox attributeDropDownBox = new AttributeDropDownBox(
            "Extra Attributes");

    /** List of Attributes in a specified Component. */
    private JList attributeList;

    /** List model. */
    private DefaultListModel model;

    // TODO: Replace with generic parameter if possible.
    public enum AttributeType {
        Producing, Consuming
    };
    private AttributeType attributeType;

    /**
     * Creates a new attribute list panel.
     *
     * @param workspace
     *            reference to workspace
     */
    public AttributePanel(final Workspace workspace, AttributeType attributeType) {
        super(new BorderLayout());
        this.attributeType = attributeType;

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
        listScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        listScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(listScroll, BorderLayout.CENTER);

        // Bottom panel
        JPanel bottomPanel = new JPanel();
        JMenuBar bar = new JMenuBar();
        bar.add(attributeDropDownBox);
        bottomPanel.add(bar);
        add(bottomPanel, BorderLayout.SOUTH);

        // Listen for attribute changes
        for (WorkspaceComponent component : workspace.getComponentList()) {
            addAttributeListener(component);
        }
        // Initialize frame
        parentFrame.setContentPane(this);
        parentFrame.pack();
    }

    /**
     * Add a listener to the specified workspace component.
     *
     * @param component component to listen to
     */
    private void addAttributeListener(final WorkspaceComponent component) {
        component.addAttributeListener(new AttributeHolderListener() {

            public void consumerAdded(Consumer consumer) {
                refresh(consumer.getParentComponent());
            }

            public void consumerRemoved(Consumer consumer) {
                refresh(consumer.getParentComponent());
            }

            public void producerAdded(Producer producer) {
                refresh(producer.getParentComponent());
            }

            public void producerRemoved(Producer producer) {
                refresh(producer.getParentComponent());
            }

        });

    }

    /**
     * @see ActionListener
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

        // Set Attribute list
        if (component != null) {
            model.clear();
            attributeDropDownBox.initializeDropDownBox(component); // TODO: Disaggregate into producer / consumer
            if (attributeType == AttributeType.Producing) {
                for (ProducingAttribute<?> attribute : component
                        .getProducingAttributes()) {
                    //System.out.println("adding producing attribute: " + attribute);
                    model.addElement(attribute);
                }
            } else if (attributeType == AttributeType.Consuming) {
                for (ConsumingAttribute<?> attribute : component
                        .getConsumingAttributes()) {
                    model.addElement(attribute);
                }
            }
        }
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
    public ArrayList<?> getSelectedAttributes() {
        if (attributeType == AttributeType.Producing) {
            ArrayList<ProducingAttribute<?>> ret = new ArrayList<ProducingAttribute<?>>();
            for (Object object : attributeList.getSelectedValues()) {
                ret.add((ProducingAttribute<?>) object);
            }
            return ret;
        } else if (attributeType == AttributeType.Consuming) {
            ArrayList<ConsumingAttribute<?>> ret = new ArrayList<ConsumingAttribute<?>>();
            for (Object object : attributeList.getSelectedValues()) {
                ret.add((ConsumingAttribute<?>) object);
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
         */
        public java.awt.Component getListCellRendererComponent(
                final JList list, final Object object, final int index,
                final boolean isSelected, final boolean cellHasFocus) {
            DefaultListCellRenderer renderer = (DefaultListCellRenderer) super
                    .getListCellRendererComponent(list, object, index,
                            isSelected, cellHasFocus);
            Attribute attribute = (Attribute) object;

            renderer.setText(attribute.getAttributeDescription());
            return renderer;
        }

    }

    /**
     * A JMenu whose items can be used to add or remove custom attribute types
     * to particular components.
     *
     * NOTE: This is currently experimental and not fully implemented!
     *
     * The idea is that components can provide a list of methods that can be
     * called which are used to add custom attributes to the method. These can
     * be called in the attribute panel since it's where the attributes are
     * viewed and edited.
     *
     * TODO: 
     *  - Only display if a component has some methods 
     *  - Change lists from lists of strings to lists of objects containing a description 
     *     and a method name.
     */
    private class AttributeDropDownBox extends JMenu  {

        /**
         * Constructor
         */
        public AttributeDropDownBox(final String string) {
            super(string);
        }

        /**
         * Initialize dropdown box with attribute-related methods that can be
         * called on workspace components.
         *
         * @param component
         */
        public void initializeDropDownBox(final WorkspaceComponent component) {
            this.removeAll();
            for(final String string : component.getAttributeTypes()) {
                JCheckBoxMenuItem theItem = new JCheckBoxMenuItem(string);
                theItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent arg0) {
                        Method theMethod = null;
                        try {
                            theMethod = component.getClass().getMethod(string,
                                    new Class[] { boolean.class });
                        } catch (SecurityException e1) {
                            e1.printStackTrace();
                        } catch (NoSuchMethodException e1) {
                            e1.printStackTrace();
                        }

                        try {
                            if (theMethod != null) {
                                theMethod.invoke(component,
                                        new Object[] { false });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                this.add(theItem);
            }
        }
    }

    /**
     * A JComboBox which listens to the workspace and updates accordingly.
     */
    private class ComponentDropDownBox extends JComboBox implements
            WorkspaceListener {

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
                AttributePanel.this.refresh((WorkspaceComponent) this.getItemAt(0));
            }
            workspace.addListener(this);
        }

        /**
         * {@inheritDoc}
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
                AttributePanel.this.clearList();
            }
        }

        /**
         * {@inheritDoc}
         */
        public void workspaceCleared() {
            this.removeAllItems();
            AttributePanel.this.clearList();
        }
    }

}
