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
 * Provides JMenus which can be used to create couplings.
 */
public class CouplingMenus  {

    /**
     * For coupling a specific source component to a selected target component,
     * using one of the built in coupling methods (one to one, all to all, etc.).
     * 
     * @param sourceComponent the specified source component
     * @return the menu of target components
     */
    public static JMenu getMenuOfTargetComponents(final WorkspaceComponent<?> sourceComponent) {
        JMenu componentMenu = new JMenu("Components");
        for (final WorkspaceComponent<?> targetComponent : sourceComponent.getWorkspace().getComponentList()) {
            JMenuItem componentMenuItem = new JMenuItem(targetComponent.getName());
            componentMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    sourceComponent.getWorkspace().coupleOneToOne(sourceComponent.getProducingAttributes(), targetComponent.getConsumingAttributes());
                }
            });
            componentMenu.add(componentMenuItem);
        }
        return componentMenu;
    }
    
    /**
     * For coupling a selected producing attribute to a specified target consuming attribute.
     * 
     * @param workspace parent workspace 
     * @param target the specified consuming attribute.
     * @return the menu of producing attributes
     */
    public static JMenu getMenuOfProducingAttributes(final Workspace workspace, final ConsumingAttribute<?> target) {
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
     * For coupling a specified producing attribute to a selected target consuming attribute.
     * 
     * @param workspace parent workspace 
     * @param target the specified producing attribute.
     * @return the menu of consuming attributes
     */
    public static JMenu getMenuOfConsumingAttributes(final Workspace workspace, final ProducingAttribute<?> source) {
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

}
