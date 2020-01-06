package org.simbrain.workspace.gui;

import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponent;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

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
     * @param source          the source object
     */
    public CouplingMenu(WorkspaceComponent sourceComponent, AttributeContainer source) {
        this.sourceComponent = sourceComponent;
        this.source = source;
        setText("Create " + source.getClass().getSimpleName() + " Coupling");
        removeAll();
        sourceComponent.getWorkspace().getCouplingManager().getVisibleProducers(source)
                .forEach(this::createProducerSubmenu);
        sourceComponent.getWorkspace().getCouplingManager().getVisibleConsumers(source)
                .forEach(this::createConsumerSubmenu);
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
    private void createProducerSubmenu(Producer producer) {
        JMenu producerSubmenu = new JMenu(producer.getSimpleDescription() + " send to");
        boolean hasItems = false;
        for (WorkspaceComponent targetComponent : sourceComponent.getWorkspace().getComponentList()) {
            List<CouplingMenuItem> couplings = new ArrayList<>();
            // TODO: show "..." when there are more than 20 items
            // TODO: get compatible consumers by component
            targetComponent.getCouplingManager().getCompatibleConsumers(producer).stream().limit(20).forEach(c -> {
                couplings.add(
                        new CouplingMenuItem(
                                sourceComponent.getWorkspace(),
                                targetComponent.getName() + "/" + c.getSimpleDescription(),
                                producer,
                                c
                        )
                );
            });
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
    private void createConsumerSubmenu(Consumer consumer) {
        JMenu consumerSubmenu = new JMenu(consumer.getSimpleDescription() + " receive " + consumer.getTypeName() + " from");
        boolean hasItems = false;
        for (WorkspaceComponent targetComponent : sourceComponent.getWorkspace().getComponentList()) {
            List<CouplingMenuItem> couplings = new ArrayList<>();
            targetComponent.getCouplingManager().getCompatibleProducers(consumer).stream().limit(20).forEach(p -> {
                couplings.add(
                        new CouplingMenuItem(
                                sourceComponent.getWorkspace(),
                                targetComponent.getName() + "/" + p.getSimpleDescription(),
                                p,
                                consumer
                        )
                );
            });

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
