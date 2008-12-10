package org.simbrain.workspace.gui;

import java.util.Collection;

import javax.swing.JMenu;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.ProducingAttribute;
import org.simbrain.workspace.SingleAttributeConsumer;
import org.simbrain.workspace.SingleAttributeProducer;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceListener;

/**
 * For coupling a given producing attribute to a menu-specified consuming attribute.
 */
public class ConsumingAttributeMenu extends JMenu implements WorkspaceListener {

	/** Reference to workspace. */
    Workspace workspace;
    
    /** The component to couple to. */
    ProducingAttribute<?> sourceProducingAttribute;
    
    /**
     * @param menuName the name of the menu
     * @param workspace the workspace
     * @param sourceComponent the target consuming attribute.
     */
    public ConsumingAttributeMenu(final String menuName, final Workspace workspace, final ProducingAttribute<?> sourceProducingAttribute) {

		super(menuName);
		this.workspace = workspace;
		this.sourceProducingAttribute = sourceProducingAttribute;
		workspace.addListener(this);
		updateMenu();
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
	public void componentAdded(WorkspaceComponent<?> component) {
		updateMenu();
	}

    /**
     * {@inheritDoc}
     */
	public void componentRemoved(WorkspaceComponent<?> component) {
		updateMenu();
	}

    /**
     * {@inheritDoc}
     */
	public void workspaceCleared() {
		updateMenu();
	}

	/**
	 * Update the menu when components are added.
	 */
	private void updateMenu() {
		this.removeAll();
        for (WorkspaceComponent<?> component : workspace.getComponentList()) {

            Collection<? extends Consumer> consumers = component.getConsumers();

            if (consumers.size() > 0) {
                JMenu componentMenu = new JMenu(component.getName());
                for (Consumer consumer : consumers) {
                    if (consumer instanceof SingleAttributeConsumer) {
                        SingleCouplingMenuItem item =
                            new SingleCouplingMenuItem(
                            		workspace, 
                            		consumer.getDescription(),
                            		sourceProducingAttribute, 
                            		consumer.getDefaultConsumingAttribute());
                        componentMenu.add(item);
                    } else {
                        JMenu consumerItem = new JMenu(consumer.getDescription());
                        for (ConsumingAttribute<?> target : consumer.getConsumingAttributes()) {
                            SingleCouplingMenuItem item = 
                                new SingleCouplingMenuItem(
                                    workspace,
                            		target.getAttributeDescription(),
                            		sourceProducingAttribute, 
                            		target);
                            consumerItem.add(item);
                            componentMenu.add(consumerItem);
                        }
                    }
                }
                this.add(componentMenu);
            }
        }
	}
}