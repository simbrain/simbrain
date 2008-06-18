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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.simbrain.workspace.*;

/**
 * Provides menus which can be used in setting couplings
 */
public class CouplingMenus  {

    /**
     * Creates a producer oriented menu for coupling to the given target.
     * 
     * @param workspace reference to workspace 
     * @param target the consuming attribute.
     * @return A new menu instance.
     */
    public static JMenu getProducerMenu(final Workspace workspace, final ConsumingAttribute<?> target) {
        System.out.println("getProducerMenu");
        JMenu producerMenu = new JMenu("Producers");
        for (WorkspaceComponent<?> component : workspace.getComponentList()) {

            Collection<? extends Producer> producers = component.getProducers();

            if (producers.size() > 0) {
                JMenu componentMenu = new JMenu(component.getName());
                for (Producer producer : component.getProducers()) {
                    if (producer instanceof SingleAttributeProducer) {
                        SingleCouplingMenuItem item =
                            new SingleCouplingMenuItem(workspace, producer.getDescription(),
                        producer.getDefaultProducingAttribute(), target);
                        componentMenu.add(item);
                    } else {
                        JMenu producerItem = new JMenu(producer.getDescription());
                        for (ProducingAttribute<?> source : producer.getProducingAttributes()) {
                            SingleCouplingMenuItem item = 
                                new SingleCouplingMenuItem(
                                    workspace,source.getAttributeDescription(), source, target);
                            producerItem.add(item);
                            componentMenu.add(producerItem);
                        }
                    }
                }
                producerMenu.add(componentMenu);
            }
        }
        return producerMenu;
    }
    
    /**
     * Creates a consumer oriented menu for coupling to the given source.
     * 
     * @param workspace reference to workspace
     * @param source the producing attribute.
     * @return A new menu instance.
     */
    public static JMenu getComponentMenu(final ActionListener listener, final WorkspaceComponent targetComponent) {
        JMenu componentMenu = new JMenu("Components");
        for (final WorkspaceComponent<?> sourceComponent : targetComponent.getWorkspace().getComponentList()) {
            JMenuItem componentMenuItem = new JMenuItem(sourceComponent.getName());
            componentMenuItem.addActionListener(listener);
            componentMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    targetComponent.getWorkspace().couple(targetComponent, sourceComponent);
                }
            });
            componentMenu.add(componentMenuItem);
        }
        return componentMenu;
    }

    
    /**
     * Creates a consumer oriented menu for coupling to the given source.
     * 
     * @param workspace reference to workspace
     * @param source the producing attribute.
     * @return A new menu instance.
     */
    public static JMenu getConsumerMenu(final Workspace workspace, final ProducingAttribute<?> source) {
        JMenu consumerMenu = new JMenu("Consumers");
        for (WorkspaceComponent<?> component : workspace.getComponentList()) {
            Collection<? extends Consumer> consumers = component.getConsumers();
            
            if (consumers.size() > 0) {
                JMenu componentMenu = new JMenu(component.getName());
                for (Consumer consumer : component.getConsumers()) {
                        if (consumer instanceof SingleAttributeConsumer) {
                            SingleCouplingMenuItem item = new SingleCouplingMenuItem(workspace, consumer.getDescription(),
                                    source, consumer.getDefaultConsumingAttribute());
                            componentMenu.add(item);
                        } else {
                            JMenu consumerItem = new JMenu(consumer.getDescription());
                            for (ConsumingAttribute<?> target : consumer.getConsumingAttributes()) {
                                SingleCouplingMenuItem item = new SingleCouplingMenuItem(workspace,
                                        target.getAttributeDescription(), source, target);
                                consumerItem.add(item);
                            }
                            componentMenu.add(consumerItem);
                    }
                }
                consumerMenu.add(componentMenu);
            }
        }
        return consumerMenu;
    }
    
    /**
     * Get a menu representing all components which have lists of producers,
     * which returns such a list.
     *
     * @param workspace reference to workspace
     * @param listener the component which will listens to the menu items in this menu
     * @return the menu containing all available components with nonempty producer lists
     */
    public static JMenu getProducerListMenu(final Workspace workspace, final ActionListener listener) {
        JMenu producerListMenu = new JMenu("Producer lists");
        for (WorkspaceComponent<?> component : workspace.getComponentList()) {
                CouplingMenuItem producerListItem = new CouplingMenuItem(component,
                    CouplingMenuItem.EventType.PRODUCER_LIST);
                producerListItem.setText(component.getName());
                producerListItem.addActionListener(listener);
                producerListMenu.add(producerListItem);
        }
        return producerListMenu;
    }

    /**
     * Get a menu representing all components which have lists of consumers,
     * which returns such a list.
     *
     * @param workspace reference to workspace
     * @param listener the component which will listens to the menu items in this menu
     * @return the menu containing all available components with nonempty consumer lists
     */
    public static JMenu getConsumerListMenu(final Workspace workspace, final ActionListener listener) {
        JMenu consumerListMenu = new JMenu("Consumer lists");
        for (WorkspaceComponent<?> component : workspace.getComponentList()) {
                CouplingMenuItem consumerListItem = new CouplingMenuItem(component,
                    CouplingMenuItem.EventType.CONSUMER_LIST);
                consumerListItem.setText(component.getName());
                consumerListItem.addActionListener(listener);
                consumerListMenu.add(consumerListItem);
        }
        return consumerListMenu;
    }
}
