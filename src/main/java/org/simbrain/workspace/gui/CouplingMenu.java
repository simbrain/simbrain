package org.simbrain.workspace.gui;

import org.simbrain.workspace.*;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A JMenu that appears relative to some object (an {@link AttributeContainer}) in
 * a workspace component. The menu allows you to create a coupling from that object
 * to any other attribute container of the same data type in Simbrain.
 */
public class CouplingMenu extends JMenu {

    /**
     * The workspace component where this menu will be shown.
     */
    private WorkspaceComponent sourceComponent;

    /**
     * The source object that will be the producer in whatever coupling is created
     * using this menu.
     */
    private AttributeContainer source;

    /**
     * Construct the menu.
     *
     * @param sourceComponent parent workspace component
     * @param source the source object
     */
    public CouplingMenu(WorkspaceComponent sourceComponent, AttributeContainer source) {
        this.sourceComponent = sourceComponent;
        this.source = source;
        setText("Create " + source.getClass().getSimpleName() + " Coupling");
        removeAll();
        List<Producer<?>> producers =
            sourceComponent.getVisibleProducers().stream()
                .filter(p -> p.getBaseObject().equals(source))
                .collect(Collectors.toList());
        for (Producer<?> producer : producers) {
            createProducerSubmenu(producer);
        }
    }

    /**
     * Create a custom name for this menu besides the default "Create X coupling".
     *
     * @param name the custom name.
     */
    public void setCustomName(String name) {
        this.setText(name);
    }

    /**
     * Create a submenu for a specific producer, that will "send" to a consumer
     * to create a coupling.
     *
     * @param producer the producer to make a menu for
     */
    private void createProducerSubmenu(Producer<?> producer) {
        JMenu producerSubmenu = new JMenu(producer.getSimpleDescription() + " send to");
        boolean hasItems = false;
        for (WorkspaceComponent targetComponent : sourceComponent.getWorkspace().getComponentList()) {
            List<CouplingMenuItem> couplings = new ArrayList<>();
            List<Consumer<?>> consumers = targetComponent.getVisibleConsumers();
            for (Consumer<?> consumer : consumers) {
                if (producer.getType() == consumer.getType()) {
                    couplings.add(
                            new CouplingMenuItem(
                                    sourceComponent.getWorkspace(),
                                    targetComponent.getName() + "/" + consumer.getSimpleDescription(),
                                    producer,
                                    consumer
                            )
                    );
                }
            }
            if (!couplings.isEmpty()) {
                for (CouplingMenuItem item : couplings) {
                    producerSubmenu.add(item);
                    hasItems = true;
                }
            }
        }
        if (hasItems) {
            add(producerSubmenu);
        }
    }

    // Note: this is not currently used.  It is enough to build all couplings using a producer / "send" menu.
    // However, if it is reinvoked it should be updated to be similar to the createProducerMenu
    private void createConsumerSubmenu(Consumer<?> consumer) {
        JMenu consumerSubmenu = new JMenu(consumer.getDescription() + " receive " + consumer.getTypeName() + " from");
        boolean hasItems = false;
        for (WorkspaceComponent targetComponent : sourceComponent.getWorkspace().getComponentList()) {
            List<CouplingMenuItem> couplings = new ArrayList<>();
            List<Producer<?>> producers = targetComponent.getVisibleProducers();
            for (Producer<?> producer : producers) {
                if (consumer.getType() == producer.getType()) {
                    couplings.add(
                            new CouplingMenuItem(
                                    sourceComponent.getWorkspace(),
                                    targetComponent.getName() + "/" + producer.getDescription(),
                                    producer,
                                    consumer
                            )
                    );
                }
            }
            if (!couplings.isEmpty()) {
                for (CouplingMenuItem item : couplings) {
                    consumerSubmenu.add(item);
                    hasItems = true;
                }
            }
        }
        if (hasItems) {
            add(consumerSubmenu);
        }
    }
}
