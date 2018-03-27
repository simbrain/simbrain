package org.simbrain.workspace.gui;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CouplingMenu creates a JMenu containing an entry for every available consumer and producer from
 * a given model object.
 * TODO: Limit the total number of entries in the coupling menus.
 */
public class CouplingMenu extends JMenu {

    private Workspace workspace;
    private Object source;

    public CouplingMenu(Workspace workspace) {
        this.workspace = workspace;
    }

    public void setSourceModel(Object source) {
        this.source = source;
        setText("Create " + source.getClass().getSimpleName() + " Coupling");
        updateItems();
    }

    public void setCustomName(String name) {
        this.setText(name);
    }

    private void updateItems() {
        removeAll();
        List<Producer<?>> producers = workspace.getCouplingFactory().getProducersFromModel(source);
        for (Producer<?> producer : producers) {
            createProducerSubmenu(producer);
        }
        List<Consumer<?>> consumers = workspace.getCouplingFactory().getConsumersFromModel(source);
        for (Consumer<?> consumer : consumers) {
            createConsumerSubmenu(consumer);
        }
    }

    private void createProducerSubmenu(Producer<?> producer) {
        JMenu producerSubmenu = new JMenu("Send " + producer.getTypeName() + " To");
        boolean hasItems = false;
        for (WorkspaceComponent targetComponent : workspace.getComponentList()) {
            List<CouplingMenuItem> couplings = new ArrayList<CouplingMenuItem>();
            List<Consumer<?>> consumers = workspace.getCouplingFactory().getAllConsumers(targetComponent);
            for (Consumer<?> consumer : consumers) {
                if (producer.getType() == consumer.getType()) {
                    couplings.add(new CouplingMenuItem(workspace, consumer.getId() + ":" + consumer.getDescription(), producer, consumer));
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
        JMenu consumerSubmenu = new JMenu("Receive " + consumer.getTypeName() + " From");
        boolean hasItems = false;
        for (WorkspaceComponent targetComponent : workspace.getComponentList()) {
            List<CouplingMenuItem> couplings = new ArrayList<CouplingMenuItem>();
            List<Producer<?>> producers = workspace.getCouplingFactory().getAllProducers(targetComponent);
            for (Producer<?> producer : producers) {
                if (consumer.getType() == producer.getType()) {
                    couplings.add(new CouplingMenuItem(workspace, producer.getId() + " " + producer.getDescription(), producer, consumer));
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
