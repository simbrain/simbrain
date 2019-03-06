package org.simbrain.workspace.gui;

import org.simbrain.workspace.*;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CouplingMenu creates a JMenu containing an entry for every available consumer and producer from
 * a given model object.
 * TODO: Limit the total number of entries in the coupling menus.
 */
public class CouplingMenu extends JMenu {

    private WorkspaceComponent sourceComponent;
    private AttributeContainer source;

    public CouplingMenu(WorkspaceComponent sourceComponent) {
        this.sourceComponent = sourceComponent;
    }

    public void setSourceModel(AttributeContainer source) {
        this.source = source;
        setText("Create " + source.getClass().getSimpleName() + " Coupling");
        updateItems();
    }

    public void setCustomName(String name) {
        this.setText(name);
    }

    private void updateItems() {
        removeAll();
        List<Producer<?>> producers =
                sourceComponent.getVisibleProducers().stream()
                .filter(p -> p.getBaseObject().equals(source))
                .collect(Collectors.toList());
        for (Producer<?> producer : producers) {
            createProducerSubmenu(producer);
        }
        List<Consumer<?>> consumers =
                sourceComponent.getVisibleConsumers().stream()
                .filter(c -> c.getBaseObject().equals(source))
                .collect(Collectors.toList());
        for (Consumer<?> consumer : consumers) {
            createConsumerSubmenu(consumer);
        }
    }

    private void createProducerSubmenu(Producer<?> producer) {
        // TODO: Verbose description but maybe good enough for now...
        JMenu producerSubmenu = new JMenu(producer.getDescription() + " send " + producer.getTypeName() + " to");
        boolean hasItems = false;
        for (WorkspaceComponent targetComponent : sourceComponent.getWorkspace().getComponentList()) {
            List<CouplingMenuItem> couplings = new ArrayList<>();
            List<Consumer<?>> consumers = targetComponent.getVisibleConsumers();
            for (Consumer<?> consumer : consumers) {
                if (producer.getType() == consumer.getType()) {
                    couplings.add(
                            new CouplingMenuItem(
                                    sourceComponent.getWorkspace(),
                                    targetComponent.getName() + "/" + consumer.getDescription(),
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

    private void createConsumerSubmenu(Consumer<?> consumer) {
        // TODO: Verbose but maybe good enough for now...
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
