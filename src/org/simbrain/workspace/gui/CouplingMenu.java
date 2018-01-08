package org.simbrain.workspace.gui;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;

import javax.swing.JMenu;
import java.util.ArrayList;
import java.util.List;

public class CouplingMenu extends JMenu {
    private Workspace workspace;
    private Object source;

    public CouplingMenu(Workspace workspace) {
        this.workspace = workspace;
    }

    public void setSourceModel(Object source) {
        this.source = source;
        setText("Couple " + source.toString());
        updateItems();
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
        JMenu producerSubmenu = new JMenu(producer.getDescription());
        boolean hasItems = false;
        for (WorkspaceComponent targetComponent : workspace.getComponentList()) {
            List<CouplingMenuItem> couplings = new ArrayList<CouplingMenuItem>();
            List<Consumer<?>> consumers = workspace.getCouplingFactory().getAllConsumers(targetComponent);
            for (Consumer<?> consumer : consumers) {
                if (producer.getType() == consumer.getType()) {
                    couplings.add(new CouplingMenuItem(workspace, consumer.getDescription(), producer, consumer));
                }
            }
            if (couplings.size() > 1) {
                JMenu consumerSubmenu = new JMenu(targetComponent.getSimpleName());
                for (CouplingMenuItem item : couplings) {
                    consumerSubmenu.add(item);
                    hasItems = true;
                }
                producerSubmenu.add(consumerSubmenu);
            } else if (couplings.size() == 1) {
                producerSubmenu.add(couplings.get(0));
                hasItems = true;
            }
        }
        if (hasItems) {
            add(producerSubmenu);
        }
    }

    private void createConsumerSubmenu(Consumer<?> consumer) {
        JMenu consumerSubmenu = new JMenu(consumer.getDescription());
        boolean hasItems = false;
        for (WorkspaceComponent targetComponent : workspace.getComponentList()) {
            List<CouplingMenuItem> couplings = new ArrayList<CouplingMenuItem>();
            List<Producer<?>> producers = workspace.getCouplingFactory().getAllProducers(targetComponent);
            for (Producer<?> producer : producers) {
                if (consumer.getType() == producer.getType()) {
                    couplings.add(new CouplingMenuItem(workspace, producer.getDescription(), producer, consumer));
                }
            }
            if (couplings.size() > 1) {
                JMenu producerSubmenu = new JMenu(targetComponent.getSimpleName());
                for (CouplingMenuItem item : couplings) {
                    producerSubmenu.add(item);
                    hasItems = true;
                }
                consumerSubmenu.add(producerSubmenu);
            } else if (couplings.size() == 1) {
                consumerSubmenu.add(couplings.get(0));
                hasItems = true;
            }
        }
        if (hasItems) {
            add(consumerSubmenu);
        }
    }
}
