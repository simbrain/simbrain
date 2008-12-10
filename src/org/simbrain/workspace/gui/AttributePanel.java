/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005-2006 Jeff Yoshimi <www.jeffyoshimi.net>
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
import java.awt.event.MouseMotionListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.simbrain.workspace.ProducingAttribute;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.gui.couplingmanager2.GenericListModel;

/**
 * Displays a list of the current couplings in the network.
 *
 */
public class AttributePanel extends JPanel implements ActionListener, MouseListener, MouseMotionListener {

    /** List of network couplings. */
    private JList attributes = new JList();

    /** Instance of parent frame. */
    private JFrame attributeFrame = new JFrame();

    /** Source workspace components. */
    private JComboBox sourceComponents = new JComboBox();

    /** Simbrain desktop reference. */
    private final SimbrainDesktop desktop;

    /**
     * Creates a new coupling list panel using the applicable desktop and coupling lists.
     * @param desktop Reference to simbrain desktop
     * @param couplingList list of couplings to be shown in window
     */
    public AttributePanel(final SimbrainDesktop desktop) {

        super(new BorderLayout());

        // Reference to the simbrain desktop
        this.desktop = desktop;

        attributes.setDragEnabled(true);
        attributes.setCellRenderer(new AttributeCellRenderer());
        attributes.addMouseListener(this);
        attributes.addMouseMotionListener(this);
        GenericListModel sourceComponentList = new GenericListModel(desktop.getWorkspace().getComponentList());
        sourceComponents.setModel(sourceComponentList);
        sourceComponents.addActionListener(this);
        if (sourceComponents.getModel().getSize() > 0) {
            sourceComponents.setSelectedIndex(0);
            this.refresh((WorkspaceComponent<?>)sourceComponentList.getElementAt(0), sourceComponents);
        }

        //Scroll pane for showing lists larger than viewing window and setting maximum size
        JScrollPane listScroll = new JScrollPane(attributes);
        listScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        listScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Add scroll pane to JPanel
        add(sourceComponents, BorderLayout.NORTH);
        add(listScroll, BorderLayout.CENTER);
//        add(buttonPanel, BorderLayout.SOUTH);
        attributeFrame.setContentPane(this);
        attributeFrame.pack();
    }


    /**
     * @see ActionListener
     * @param event Action event.
     */
    public void actionPerformed(final ActionEvent event) {

        // Refresh component lists
        if (event.getSource() instanceof JComboBox) {
            WorkspaceComponent component = (WorkspaceComponent) ((JComboBox) event.getSource()).getSelectedItem();
            refresh(component, (JComboBox) event.getSource());
        }        
    }

    /**
     * Custom producer cell renderer.
     */
    private class AttributeCellRenderer extends DefaultListCellRenderer {

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
            ProducingAttribute attribute = (ProducingAttribute) object;
           renderer.setText(attribute.getAttributeDescription());
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
            if (component.getProducers() != null) {
                attributes.setModel(new GenericListModel<ProducingAttribute>(
                                component.getProducingAttributes()));
            }
        }
    }


    public void mouseClicked(MouseEvent e) {
        // TODO Auto-generated method stub
        System.out.println("Clicked");
    }


    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub
        System.out.println("Entered");
    }


    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub
        System.out.println("Exited");
    }


    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub
        System.out.println("Pressed");
    }


    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub
        System.out.println("Released");
    }


    public void mouseDragged(MouseEvent e) {
        // TODO Auto-generated method stub
        System.out.println("Dragged");
    }


    public void mouseMoved(MouseEvent e) {
        // TODO Auto-generated method stub
        System.out.println("Moved");
    }


}
